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
package de.gematik.smartcards.g2icc.cos;

import de.gematik.smartcards.crypto.AesKey;
import de.gematik.smartcards.crypto.AfiElcUtils;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.g2icc.cvc.Cvc;
import de.gematik.smartcards.g2icc.cvc.TrustCenter;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.security.interfaces.ECPrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.RejectedExecutionException;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure messaging transformer in software.
 *
 * <p>Private authentication keys and symmetric session keys are stored as instance attributes in
 * this class. From a performance point of view, this is faster than a smart card, but less secure.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings("PMD.GodClass")
public final class SecureMessagingConverterSoftware extends SecureMessagingConverter {

  /** Logger. */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SecureMessagingConverterSoftware.class); // */

  /** Message for no session keys available. */
  private static final String NO_SESSIONKEYS = "no session keys available"; // */

  /** Padding indicator. */
  private static final byte[] PADDING_INDICATOR = new byte[] {1}; // */

  /** Suffix used for key-derivation "enc". */
  private static final byte[] SUFFIX_ENC = Hex.toByteArray("0000 0001"); // */

  /** Suffix used for key-derivation "enc". */
  private static final byte[] SUFFIX_MAC = Hex.toByteArray("0000 0002"); // */

  /** Private key used for authentication. */
  private final EcPrivateKeyImpl insPrk; // */

  /** End-entity certificate for {@link #insPrk}. */
  private final Cvc insEndEntityCvc; // */

  /** Sub-CA-CVC for {@link #insEndEntityCvc}. */
  private final Cvc insSubCaCvc; // */

  /**
   * Content of the End-entity certificate of opponent.
   *
   * <p>An empty {@code byte[]} indicates that the value is not (yet) set.
   */
  private byte[] insEndEntityOpponent = AfiUtils.EMPTY_OS; // */

  /** Session key for enciphering, see gemSpec_COS#(N029.900)d.2. */
  private AesKey insKenc = new AesKey(new byte[16], 0, 16); // */

  /** Session key for MAC computation, see gemSpec_COS#(N029.900)d.4. */
  private AesKey insKmac = new AesKey(new byte[16], 0, 16); // */

  /**
   * Send sequence, see gemSpec_COS#(N029.900)d.5
   *
   * <p>This counter is only used for securing {@link CommandApdu} and is always set such that it
   * has the correct value <b>before</b> the transformation begins, i.e., at the end of the former
   * transformation.
   */
  private final byte[] insSscMacCmd = new byte[16]; // */

  /**
   * Send sequence, see gemSpec_COS#(N029.900)d.5
   *
   * <p>This counter is only used for unsecuring {@link ResponseApdu} and is always set to the
   * correct value at the beginning of the transformation.
   */
  private final byte[] insSscMacRsp = new byte[16]; // */

  /**
   * Flag, see gemSpec_COS#(N029.900)d.1.
   *
   * <p><i><b>Note:</b> Contrary to gemSpec_COS this is not an {@code enum} but a boolean value.
   * {@code FALSE} means "no session keys", {@code TRUE} means "SK4TC".</i>
   */
  private boolean insFlagSessionEnabled; // */

  /**
   * Comfort constructor.
   *
   * @param prk private key used for authentication
   * @param subCaCvc CV-certificate of Sub-CA which issued {@code endEntityCvc}
   * @param endEntityCvc end-entity-CV-certificate for the private authentication key
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code subCaCvc} is not trustworthy or is not a CV-certificate of a Sub-CA
   *       <li>{@code endEntityCvc} is not trustworthy or is not an end-entity-CVC
   *       <li>{@code subCaCvc}, {@code endEntityCvc} and {@code prk} do not form a chain
   *     </ol>
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  public SecureMessagingConverterSoftware(
      final Cvc subCaCvc, final Cvc endEntityCvc, final ECPrivateKey prk) {
    super();

    if (!Cvc.SignatureStatus.SIGNATURE_VALID.equals(subCaCvc.getSignatureStatus())) {
      throw new IllegalArgumentException("invalid subCaCvc");
    } // end fi
    if (!Cvc.SignatureStatus.SIGNATURE_VALID.equals(endEntityCvc.getSignatureStatus())) {
      throw new IllegalArgumentException("invalid endEntityCvc");
    } // end fi
    if (!subCaCvc.isSubCa()) {
      throw new IllegalArgumentException("not a Sub-CA-CVC");
    } // end fi
    if (!endEntityCvc.isEndEntity()) {
      throw new IllegalArgumentException("not an End-Entity-CVC");
    } // end fi

    insSubCaCvc = subCaCvc;
    insEndEntityCvc = endEntityCvc;
    insPrk = new EcPrivateKeyImpl(prk);

    if (!(subCaCvc.getChr().equals(endEntityCvc.getCar())
        && endEntityCvc.getPublicKey().equals(insPrk.getPublicKey()))) {
      throw new IllegalArgumentException("not a chain");
    } // end fi
  } // end constructor */

  /**
   * Get a GENERAL AUTHENTICATE command APDU for step 1 of mutual authentication.
   *
   * <p>For details see gemSpec_COS#(N085.012).
   *
   * @return an appropriate GENERAL AUTHENTICATE command APDU
   */
  public CommandApdu getGeneralAuthenticateStep1() {
    return new CommandApdu(
        0x10,
        0x86, // CLA, INS
        0x00,
        0x00, // P1,  P2
        BerTlv.getInstance(0x7c, BerTlv.getInstance(0xc3, getEndEntityCvc().getChr()).getEncoded())
            .getEncoded(),
        CommandApdu.NE_SHORT_WILDCARD);
  } // end method */

  /**
   * Get a GENERAL AUTHENTICATE command APDU for step 2 of mutual authentication.
   *
   * <p>For details see gemSpec_COS#(N085.016).
   *
   * @param rsp a {@link ResponseApdu} according to gemSpec_COS#(N085.052)h
   * @return a GENERAL AUTHENTICATE command APDU according to (N085.016)
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>status word in {@code rsp} differs from '9000'
   *       <li>response data-field is not a BER-TLV structure according to
   *           <pre>́7C - L7C - (85 - L85 - ephemeralPK_opponent) ́</pre>
   *       <li>{@code ephemeralPK_opponent} is not a point on the same elliptic curve as {@link
   *           #getEndEntityCvc()}
   *       <li>domain parameter of private key is not in set {brainpoolP256r1, brainpoolP384r1,
   *           brainpoolP512r1}
   *     </ol>
   *
   * @throws NoSuchElementException if no {@link Cvc} imported by {@link #importCvc(Cvc)}
   */
  public CommandApdu getGeneralAuthenticateStep2(final ResponseApdu rsp) {
    if (0x9000 != rsp.getSw()) { // NOPMD literal in if statement
      throw new IllegalArgumentException("not NoError");
    } // end fi
    // ... NoError

    final var rspDataField = BerTlv.getInstance(rsp.getData());
    if (0x7c != rspDataField.getTag()) { // NOPMD literal in if statement
      throw new IllegalArgumentException("outer tag not '7c'");
    } // end fi
    final var pkDo = ((ConstructedBerTlv) rspDataField).getPrimitive(0x85);
    if (pkDo.isEmpty()) {
      throw new IllegalArgumentException("DO with ephemeral PuK absent");
    } // end fi

    // --- (N085.052)f: create ephemeralSelf
    final var dp = getPrk().getParams();
    final var ephemeralSelf = new EcPrivateKeyImpl(dp);

    // --- (N085.056)a: extract public key from response APDU
    final var ephemeralPukOpponent =
        new EcPublicKeyImpl(AfiElcUtils.os2p(pkDo.get().getValueField(), dp), dp);
    final var k1 = // (N085.056)c.1
        AfiElcUtils.sharedSecret(ephemeralSelf, getEndEntityOpponent().getPublicKey());
    final var k2 = AfiElcUtils.sharedSecret(getPrk(), ephemeralPukOpponent); // (N085.056)c.1
    final var kd = AfiUtils.concatenate(k1, k2); // (N085.056)c.3

    // --- calculate session keys
    final byte[] kdEnc = AfiUtils.concatenate(kd, SUFFIX_ENC);
    final byte[] kdMac = AfiUtils.concatenate(kd, SUFFIX_MAC);

    // TODO for now we assume brainpoolP256r1, other CVC not yet available
    insKenc = new AesKey(EafiHashAlgorithm.SHA_1.digest(kdEnc), 0, 16);
    insKmac = new AesKey(EafiHashAlgorithm.SHA_1.digest(kdMac), 0, 16);
    // */

    // Set the SendSequenceCounter, see (N001.500)c, (N001.510)c, (N001.520)c
    // Note 1: A SendSequenceCounter is initialized with zero and incremented
    //         before the first command APDU is secured. It is incremented again
    //         before the first response APDU is unsecured.
    // Note 2: Here we use two SendSequenceCounter, one for command and the
    //         other for response APDU.
    Arrays.fill(insSscMacCmd, (byte) 0);
    insSscMacCmd[insSscMacCmd.length - 1]++; // set SscMacCmd = 1
    Arrays.fill(insSscMacRsp, (byte) 0); // set SscMacRsp = 0
    insFlagSessionEnabled = true;

    // --- compile response APDU, see (N085.016)
    return new CommandApdu(
        0x00,
        0x86, // CLA, INS: GENERAL AUTHENTICATE
        0x00,
        0x00, // P1,  P2 : no information given
        BerTlv.getInstance(
                0x7c,
                BerTlv.getInstance(
                        0x85, AfiElcUtils.p2osUncompressed(ephemeralSelf.getPublicKey().getW(), dp))
                    .getEncoded())
            .getEncoded());
  } // end method */

  /**
   * Import CV-certificates.
   *
   * <p>This method introduces the opponent to this instance.
   *
   * @param endEntityCvc CV-certificate of the opponent
   * @return a chain of CV-certificates calculated by {@link TrustCenter#getChain(Cvc, String)} with
   *     the End-Entity-CVC of this instance as the first element in the list
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code endEntityCvc} is not trustworthy or is not an end-entity-CVC
   *     </ol>
   */
  public List<Cvc> importCvc(final Cvc endEntityCvc) {
    if (!Cvc.SignatureStatus.SIGNATURE_VALID.equals(endEntityCvc.getSignatureStatus())) {
      throw new IllegalArgumentException("signature not valid");
    } // end fi
    if (!endEntityCvc.isEndEntity()) {
      throw new IllegalArgumentException("not an End-Entity-CVC");
    } // end fi

    insEndEntityOpponent = endEntityCvc.getCvc().getEncoded();

    return TrustCenter.getChain(
        getEndEntityCvc(),
        TrustCenter.getParent(getEndEntityOpponent()).iterator().next().getCar());
  } // end method */

  /**
   * Computes a MAC for the given message.
   *
   * @param message of arbitrary length (i.e., without padding)
   * @return MAC
   * @throws RejectedExecutionException if session keys are not enabled
   */
  @Override
  protected byte[] computeCryptographicChecksum(final byte[] message) {
    // Note 1: It is assumed that the SendSequenceCounter already has the
    //         correct value when a MAC is calculated for a CommandApdu.
    // Note 2: Transforming a CommandApdu optionally starts with enciphering.
    final var macInput = getKmac().padIso(message);
    LOGGER.atTrace().log("SSCmac  : {}", Hex.toHexDigits(getSscMacCmd()));
    LOGGER.atTrace().log("MACinput: {}", Hex.toHexDigits(macInput));
    final byte[] result =
        getKmac()
            .calculateCmac(
                AfiUtils.concatenate(getSscMacCmd(), macInput), 8 // see (N002.810)h
                );
    LOGGER.atTrace().log("MAC     : {}", Hex.toHexDigits(result));

    // --- prepare the SendSequenceCounter for transforming the next CommandApdu
    AfiUtils.incrementCounter(getSscMacCmd());
    AfiUtils.incrementCounter(getSscMacCmd());

    return result;
  } // end method */

  /**
   * Deciphers given cryptogram, inverse operation to {@link #encipher(byte[])}.
   *
   * @param data one octet padding indicator (<b>SHALL</b> be equal to '01') concatenated with a
   *     cryptogram
   * @return plain text without any padding
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>Padding indicator is not '01'
   *       <li>an error occurred during deciphering
   *       <li>{@code plainText} is not correctly padded
   *     </ol>
   */
  @Override
  protected byte[] decipher(final byte[] data) {
    // Note: It is assumed that the SendSequenceCounter already has
    //       the correct value when the optional step of deciphering
    //       is performed.
    try {
      final var pi = data[0] & 0xff;
      if (1 == pi) { // NOPMD literal in if statement
        // ... padding indicator == 1

        final var cipherText = Arrays.copyOfRange(data, 1, data.length);
        final byte[] plainText =
            getKenc().decipherCbc(cipherText, getKenc().encipherCbc(getSscMacRsp()));

        return getKenc().truncateIso(plainText);
      } // end fi
      // ... wrong padding indicator, intentionally no further action here
    } catch (IllegalArgumentException e) { // NOPMD empty catch block
      // ... padding error in deciphered data, intentionally no further action here
    } // end Catch (...)
    // ... wrong padding indicator or wrong padding

    insFlagSessionEnabled = false; // end SM session

    // Note: A wrong MAC only ends an SM session. Here intentionally an
    //       exception is thrown, because deciphering should only happen
    //       after MAC verification. So here we have correct MAC but
    //       wrong cryptogram. Thus, something is really strange here.
    throw new IllegalArgumentException("decipher error");
  } // end method */

  /**
   * Enciphers given message, inverse operation to {@link #decipher(byte[])}.
   *
   * @param message of arbitrary length (i.e., without any padding)
   * @return padding indicator '01' concatenated with corresponding cryptogram
   * @throws RejectedExecutionException if session keys are not enabled
   */
  @Override
  protected byte[] encipher(final byte[] message) {
    // Note 1: It is assumed that the SendSequenceCounter already has the
    //         correct value at the beginning of the transformation.
    // Note 2: Transforming a CommandApdu optionally starts with enciphering.
    final byte[] cipher =
        getKenc().encipherCbc(getKenc().padIso(message), getKenc().encipherCbc(getSscMacCmd()));

    return AfiUtils.concatenate(
        PADDING_INDICATOR, // see gemSpec_COS#(N032.200)d
        cipher);
  } // end method */

  /**
   * Checks if data correspond to MAC.
   *
   * @param data protected by MAC, note: there is no ISO padding at the end of data
   * @param mac protecting data
   * @return {@code TRUE} if MAC matches expectation, {@code FALSE} otherwise
   * @throws RejectedExecutionException if session keys are not enabled
   */
  @Override
  protected boolean verifyCryptographicChecksum(final byte[] data, final byte[] mac) {
    // --- increment SendSequenceCounter at the beginning of the transformation
    //     Note: Transforming a ResponseApdu always begins with checking the MAC.
    AfiUtils.incrementCounter(getSscMacRsp());
    AfiUtils.incrementCounter(getSscMacRsp());

    final var macInput = getKmac().padIso(data);
    LOGGER.atTrace().log("SSCmac  : {}", Hex.toHexDigits(getSscMacRsp()));
    LOGGER.atTrace().log("MACinput: {}", Hex.toHexDigits(macInput));
    LOGGER.atTrace().log("MACexpec: {}", Hex.toHexDigits(mac));
    final var macIs =
        getKmac()
            .calculateCmac(
                AfiUtils.concatenate(getSscMacRsp(), macInput), 8 // see (N002.810)h
                );
    LOGGER.atTrace().log("MACis   : {}", Hex.toHexDigits(macIs));

    insFlagSessionEnabled = Arrays.equals(macIs, mac);
    LOGGER.atTrace().log("MACequal: {}", insFlagSessionEnabled);

    return insFlagSessionEnabled;
  } // end method */

  /**
   * Performs an ISO padding on given message.
   *
   * <p><i><b>Note:</b> This method is never used. Instead {@link AesKey#padIso(byte[])} is
   * used.</i>
   *
   * @param message to be padded
   * @return nothing
   * @throws RejectedExecutionException all the time
   */
  @Override
  protected byte[] padIso(final byte[] message) {
    return getKenc().padIso(message);
  } // end method */

  /**
   * Getter.
   *
   * @return private authentication key
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ EcPrivateKeyImpl getPrk() {
    return insPrk;
  } // end method */

  /**
   * Getter.
   *
   * @return end-entity CV-certificate
   */
  public Cvc getEndEntityCvc() {
    return insEndEntityCvc;
  } // end method */

  /**
   * Getter.
   *
   * @return Sub-CA-CV-certificate
   */
  public Cvc getSubCaCvc() {
    return insSubCaCvc;
  } // end method */

  /**
   * Getter.
   *
   * @return end-entity CV-certificate of opponent
   * @throws NoSuchElementException if this instance attribute was never set
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ Cvc getEndEntityOpponent() {
    if (0 == insEndEntityOpponent.length) {
      throw new NoSuchElementException("no opponent");
    } // end fi

    return new Cvc(insEndEntityOpponent);
  } // end method */

  /**
   * Getter.
   *
   * @return session key for enciphering
   * @throws RejectedExecutionException if session keys are not enabled
   */
  private AesKey getKenc() {
    if (!insFlagSessionEnabled) {
      throw new RejectedExecutionException(NO_SESSIONKEYS);
    } // end fi
    // ... session keys available

    return insKenc;
  } // end method */

  /**
   * Getter.
   *
   * @return session key MAC calculation
   * @throws RejectedExecutionException if session keys are not enabled
   */
  private AesKey getKmac() {
    if (!insFlagSessionEnabled) {
      throw new RejectedExecutionException(NO_SESSIONKEYS);
    } // end fi
    // ... session keys available

    return insKmac;
  } // end method */

  /**
   * Getter.
   *
   * @return send-sequence-counter for {@link CommandApdu}.
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ byte[] getSscMacCmd() {
    return insSscMacCmd; // NOPMD expose internal array
  } // end method */

  /**
   * Getter.
   *
   * @return send-sequence-counter for {@link ResponseApdu}.
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ byte[] getSscMacRsp() {
    return insSscMacRsp; // NOPMD expose internal array
  } // end method */

  /**
   * Return flag.
   *
   * @return unconditionally {@link #insFlagSessionEnabled}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ boolean isFlagSessionEnabled() {
    return insFlagSessionEnabled;
  } // end method */
} // end class
