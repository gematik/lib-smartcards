/*
 * Copyright (Date see Readme), gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */
package de.gematik.smartcards.g2icc.proxy;

import de.gematik.smartcards.g2icc.cos.EafiCosAlgId;
import de.gematik.smartcards.g2icc.cos.SecureMessagingConverter;
import de.gematik.smartcards.g2icc.cos.SecureMessagingConverterSoftware;
import de.gematik.smartcards.g2icc.cos.SecureMessagingLayer;
import de.gematik.smartcards.g2icc.cos.SecureMessagingLayerSmc;
import de.gematik.smartcards.g2icc.cvc.Cvc;
import de.gematik.smartcards.g2icc.cvc.TrustCenter;
import de.gematik.smartcards.sdcom.apdu.ApduLayer;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.GetChallenge;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Subset of {@link IccProxy} for G2 cards used by human beings.
 *
 * <p>Directly known subclasses: {@link IccProxyEgk}, {@link IccProxyHba}, {@link IccProxySmcB}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public abstract class IccUser extends IccProxy {

  /** Formatter for command data field of an MSE Set command. */
  private static final String MSE_SET_DATA_FIELD = "84-01-%02x   80-01-%s"; // */

  /**
   * Key reference of a private key used for authentication.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_ROLE_AUTHENTICATION
   * elcRoleAuthentication} and {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM ELC_SESSIONKEY_4_SM}.
   */
  private final int insPrkRoleAuthentication; // */

  /**
   * Comfort constructor.
   *
   * @param apduLayer used for smart card communication
   * @param prkRoleAuthentication key reference of the private key supporting at least {@link
   *     EafiCosAlgId#ELC_ROLE_AUTHENTICATION elcRoleAuthentication}
   * @param prk4Sm key reference of the private key supporting {@link
   *     EafiCosAlgId#ELC_SESSIONKEY_4_SM ELC_SESSIONKEY_4_SM}
   * @param cmsMasterkey128 masterkey used for deriving card individual keys
   * @param cupMasterkey128 masterkey used for deriving card individual keys
   * @param cmsMasterkey256 masterkey used for deriving card individual keys
   * @param cupMasterkey256 masterkey used for deriving card individual keys
   * @param adminCmsMasterkeyElc256 masterkey used for deriving card individual keys
   */
  protected IccUser(
      final ApduLayer apduLayer,
      final int prkRoleAuthentication,
      final int prk4Sm,
      final String cmsMasterkey128,
      final String cupMasterkey128,
      final String cmsMasterkey256,
      final String cupMasterkey256,
      final String adminCmsMasterkeyElc256) {
    // Note 1: SonarQube claims the following major finding:
    //         "Constructor has 8 parameters, which is greater than 7 authorized."
    // Note 2: This will NOT be fixed.
    //         This class needs eight parameters.
    //         I will not combine parameters to just satisfy SonarQube.
    super(
        apduLayer,
        prk4Sm,
        cmsMasterkey128,
        cupMasterkey128,
        cmsMasterkey256,
        cupMasterkey256,
        adminCmsMasterkeyElc256);

    insPrkRoleAuthentication = prkRoleAuthentication;
  } // end constructor */

  /**
   * Perform Internal Authenticate command.
   *
   * <p>This method performs an internal authentication with algorithm-identifier {@link
   * EafiCosAlgId#ELC_ROLE_AUTHENTICATION elcRoleAuthentication}.
   *
   * @param token to be signed, octet string with 24 octets of arbitrary content, see (N086.200)a.
   * @return response with signature over given {@code token}
   */
  protected byte[] internalAuthenticate(final byte[] token) {
    // --- select private key, see (N100.900)
    send(
        new CommandApdu(
            0x00,
            0x22, // CLA INS of MSE Set
            0x41,
            0xa4, // P1 P2 for Internal Authenticate command
            String.format(
                MSE_SET_DATA_FIELD,
                getPrkRoleAuthentication(),
                EafiCosAlgId.ELC_ROLE_AUTHENTICATION.getAlgId())),
        0x9000);

    // --- INTERNAL AUTHENTICATE, see (N086.400)
    return send(
            new CommandApdu(
                0x00,
                0x88, // CLA INS of INTERNAL AUTHENTICATE
                0x00,
                0x00, // P1 P2
                token,
                CommandApdu.NE_SHORT_WILDCARD),
            0x9000)
        .getData();
  } // end method */

  /**
   * Perform External Authenticate command.
   *
   * <p>This method performs an external authentication with algorithm-identifier {@link
   * EafiCosAlgId#ELC_ROLE_CHECK}.
   *
   * @param proxy the other {@link IccUser} used for {@link #internalAuthenticate(byte[])}
   * @return {@code TRUE} if external authentication was successful, {@code FALSE} otherwise
   */
  public boolean externalAuthenticate(final IccUser proxy) {
    final Cvc authCvc = proxy.getCvcRoleAuthentication();

    // --- select public key, see (N101.900)
    final String chr = authCvc.getChr();
    final CommandApdu mseSet =
        new CommandApdu(
            0x00,
            0x22,
            0x81,
            0xa4,
            AfiUtils.concatenate(
                BerTlv.getInstance(0x83, chr).getEncoded(),
                BerTlv.getInstance(0x80, EafiCosAlgId.ELC_ROLE_CHECK.getAlgId()).getEncoded()));
    final ResponseApdu rsp =
        send(
            mseSet,
            0x9000, // NoError
            0x6a88 // KeyNotFound
            );
    if (rsp.getTrailer() == 0x6a88) { // NOPMD literal in if statement
      // ... public key not (yet) available
      //     => import it

      // get CVC-chain
      final String car = getCvcCaE256().getCar();
      final List<Cvc> chain = TrustCenter.getChain(authCvc, car);

      // import CVC-chain
      importChain(chain);

      send(mseSet, 0x9000); // NoError
    } // end fi
    // ... public key selected

    // --- GET CHALLENGE
    final byte[] rndIcc = send(new GetChallenge(16), 0x9000).getData();
    final byte[] token = AfiUtils.concatenate(rndIcc, Hex.toByteArray(getIccsn8()));

    // --- INTERNAL AUTHENTICATE
    final byte[] response = proxy.internalAuthenticate(token);

    // --- EXTERNAL AUTHENTICATE
    final int trailer = send(new CommandApdu(0x00, 0x82, 0x00, 0x00, response)).getTrailer();

    return 0x9000 == trailer;
  } // end method */

  /**
   * Establishes a trusted channel between this instance and given {@link SecureMessagingConverter}.
   *
   * <p>In case of success the given {@link SecureMessagingConverter} can be used to {@link
   * SecureMessagingConverter#secureCommand(CommandApdu) secure} and {@link
   * SecureMessagingConverter#unsecureResponse(ResponseApdu) unsecure} APDU.
   *
   * @param smTransformer the other end point of the trusted channel
   */
  public void establishTrustedChannel(final SecureMessagingConverterSoftware smTransformer) {
    // --- select keys in ICC user
    LOGGER.atTrace().log("prepare ICC user");
    // select private key
    this.send(
        // see (N100.900)
        new CommandApdu(
            0x00,
            0x22, // CLA INS of MSE Set
            0x41,
            0xa4, // P1 P2 for Internal Authenticate command
            String.format(
                MSE_SET_DATA_FIELD, getPrk4Sm(), EafiCosAlgId.ELC_SESSIONKEY_4_SM.getAlgId())),
        0x9000);
    final var userGenAut1 = smTransformer.getGeneralAuthenticateStep1();
    ResponseApdu rspUser =
        send(
            userGenAut1,
            0x9000, // NoError
            0x6a88 // KeyNotFound
            );
    final var chain = smTransformer.importCvc(this.getCvc4Sm()); // get CVC-chain
    if (rspUser.getTrailer() == 0x6a88) { // NOPMD literal in if statement
      // ... public key not (yet) available
      //     => import it

      // import CVC-chain
      this.importChain(chain);

      rspUser =
          this.send(
              userGenAut1, 0x9000 // NoError
              );
    } // end fi
    // ... public key selected
    LOGGER.atTrace().log("GENERAL AUTHENTICATE step 1 completed");

    LOGGER.atTrace().log("GENERAL AUTHENTICATE, ICC user step 2");
    this.send(smTransformer.getGeneralAuthenticateStep2(rspUser), 0x9000);
  } // end method */

  /**
   * Establishes a trusted channel.
   *
   * @param smc Secure Module Card used to establish a trusted channel to the ICC of this user
   * @return appropriate secure messaging layer
   */
  public SecureMessagingLayer establishTrustedChannel(final IccSmc smc) {
    final Cvc userCvc = this.getCvc4Sm();
    final Cvc smcCvc = smc.getCvc4Tc();

    // --- select keys in ICC user
    LOGGER.atTrace().log("prepare ICC user");
    // select private key
    this.send(
        // see (N100.900)
        new CommandApdu(
            0x00,
            0x22, // CLA INS of MSE Set
            0x41,
            0xa4, // P1 P2 for Internal Authenticate command
            String.format(
                MSE_SET_DATA_FIELD, getPrk4Sm(), EafiCosAlgId.ELC_SESSIONKEY_4_SM.getAlgId())),
        0x9000);
    final CommandApdu userGenAut1 =
        new CommandApdu(
            0x10,
            0x86,
            0x00,
            0x00,
            BerTlv.getInstance(0x7c, List.of(BerTlv.getInstance(0xc3, smcCvc.getChr())))
                .getEncoded(),
            CommandApdu.NE_SHORT_WILDCARD);
    ResponseApdu rspUser =
        send(
            userGenAut1,
            0x9000, // NoError
            0x6a88); // KeyNotFound
    if (rspUser.getTrailer() == 0x6a88) { // NOPMD literal in if statement
      // ... public key not (yet) available
      //     => import it

      // get CVC-chain
      final String car = this.getCvcCaE256().getCar();
      final List<Cvc> chain = TrustCenter.getChain(smcCvc, car);

      // import CVC-chain
      this.importChain(chain);

      rspUser = this.send(userGenAut1, 0x9000); // NoError
    } // end fi
    // ... public key selected

    // --- select keys in SMC
    LOGGER.atTrace().log("prepare SMC");
    // select private key
    smc.send(
        // see (N100.900)
        new CommandApdu(
            0x00,
            0x22, // CLA INS of MSE Set
            0x41,
            0xa4, // P1 P2 for Internal Authenticate command
            String.format(
                MSE_SET_DATA_FIELD, smc.getPrk4Tc(), EafiCosAlgId.ELC_SESSIONKEY_4_TC.getAlgId())),
        0x9000);
    final CommandApdu smcGenAut1 =
        new CommandApdu(
            0x10,
            0x86,
            0x00,
            0x00,
            BerTlv.getInstance(0x7c, List.of(BerTlv.getInstance(0xc3, userCvc.getChr())))
                .getEncoded(),
            CommandApdu.NE_SHORT_WILDCARD);
    ResponseApdu rspSmc =
        smc.send(
            smcGenAut1,
            0x9000, // NoError
            0x6a88); // KeyNotFound
    if (rspSmc.getTrailer() == 0x6a88) { // NOPMD literal in if statement
      // ... public key not (yet) available
      //     => import it

      // get CVC-chain
      final String car = smc.getCvcCaE256().getCar();
      final List<Cvc> chain = TrustCenter.getChain(userCvc, car);

      // import CVC-chain
      smc.importChain(chain);

      rspSmc = smc.send(smcGenAut1, 0x9000); // NoError
    } // end fi
    // ... public key selected
    LOGGER.atTrace().log("GENERAL AUTHENTICATE step 1 completed");

    LOGGER.atTrace().log("GENERAL AUTHENTICATE, ICC user step 2");
    this.send(
        new CommandApdu(
            0x00,
            0x86,
            0x00,
            0x00, // command header of General Authenticate
            rspSmc.getData()),
        0x9000);

    LOGGER.atTrace().log("GENERAL AUTHENTICATE, SMC step 2");
    smc.send(
        new CommandApdu(
            0x00,
            0x86,
            0x00,
            0x00, // command header of General Authenticate
            rspUser.getData()),
        0x9000);

    return new SecureMessagingLayerSmc(this, smc);
  } // end method */

  /**
   * Retrieves CV-certificate used for role authentication.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_ROLE_AUTHENTICATION
   * elcRoleAuthentication}.
   *
   * @return CV-certificate used for role-authentication
   * @throws NoSuchElementException if an appropriate {@link Cvc} is absent
   */
  public abstract Cvc getCvcRoleAuthentication(); // */

  /**
   * Returns key reference of a private key.
   *
   * <p>The private key supports at least {@link EafiCosAlgId#ELC_ROLE_AUTHENTICATION
   * elcRoleAuthentication}.
   *
   * @return key reference of the private key used for CV-certificate authentication
   */
  /* package */ int getPrkRoleAuthentication() {
    return insPrkRoleAuthentication;
  } // end method */
} // end class
