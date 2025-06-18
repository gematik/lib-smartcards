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

import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;

import de.gematik.smartcards.crypto.AfiElcParameterSpec;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.g2icc.cos.EafiCosAlgId;
import de.gematik.smartcards.g2icc.cvc.Cvc;
import de.gematik.smartcards.sdcom.MessageLayer;
import de.gematik.smartcards.sdcom.apdu.ApduLayer;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ReadBinary;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this abstract class is to provide a proxy for G2 cards.
 *
 * <p>Directly known subclasses: {@link IccUser}, {@link IccSmc}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "EI_EXPOSE_REP",
//          Short message: Returning a reference to a mutable object value stored in one
//              of the object's fields exposes the internal representation of the object.
//          Rational: That finding is suppressed because class EcPrivateKeyImpl is immutable.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP" // see note 1
}) // */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize", "PMD.GodClass"})
public abstract class IccProxy implements ApduLayer {

  /** name says it all. */
  public static final String AID_CIA_PREFIX = "E828 BD08 0F";

  /** name says it all. */
  public static final String AID_DF_ESIGN = "A000 0001 6745 5349 474E";

  /** name says it all. */
  public static final String AID_DF_CIA_ESIGN = AID_CIA_PREFIX + AID_DF_ESIGN;

  /** Logger. */
  protected static final Logger LOGGER = LoggerFactory.getLogger(IccProxy.class); // */

  /** Key suffix. */
  /* package */ static final String KEY_SUFFIX = "10111213 14151617 1819111b 1c1d1e1f"; // */

  /** {@link ApduLayer} used for real communication. */
  private final ApduLayer insApduLayer; // */

  /** Values from EF.ATR indicating the maximum length of APDUs. */
  private final int[] insApduLength; // */

  /**
   * Key reference of a private key used for authentication.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM
   * ELC_SESSIONKEY_4_SM}.
   */
  private final int insPrk4Sm; // */

  /** Card individual PrK.RCA.AdminCMS.CS.E256. */
  private final EcPrivateKeyImpl insRcaAdminCmsCsE256; // */

  /** Card individual SK.CMS.AES128.enc. */
  private final byte[] insCms128enc; // */

  /** Card individual SK.CMS.AES128.mac. */
  private final byte[] insCms128mac; // */

  /** Card individual SK.CMS.AES256.enc. */
  private final byte[] insCms256enc; // */

  /** Card individual SK.CMS.AES256.mac. */
  private final byte[] insCms256mac; // */

  /** Card individual SK.CUP.AES128.enc. */
  private final byte[] insCup128enc; // */

  /** Card individual SK.CUP.AES128.mac. */
  private final byte[] insCup128mac; // */

  /** Card individual SK.CUP.AES256.enc. */
  private final byte[] insCup256enc; // */

  /** Card individual SK.CUP.AES256.mac. */
  private final byte[] insCup256mac; // */

  /** content of / MF / EF.ATR. */
  private final String insContentEfAtr; // */

  /** Content of / MF / EF.Version2. */
  private final String insContentEfVersion2; // */

  /** content of / MF / EF.C.CA...CS.E256. */
  private final Cvc insCvcCaE256; // */

  /** complete ICCSN of ICC connected to this proxy. */
  private final String insIccsn; // */

  /** the eight LSByte of ICCSN. */
  private final String insIccsn8; // */

  /** Product Type Version COS. */
  private final String insPtCos; // */

  /** Product indicator Chip. */
  private final String insPiChip; // */

  /** Product indicator COS. */
  private final String insPiCos; // */

  /** Product indicator initialized object-system. */
  private final String insPiInitializedObjectSystem; // NOPMD long name */

  /**
   * Comfort constructor.
   *
   * @param apduLayer used for smart card communication
   * @param prk4Sm key reference of the private key supporting {@link
   *     EafiCosAlgId#ELC_SESSIONKEY_4_SM ELC_SESSIONKEY_4_SM}
   * @param cmsMasterkey128 masterkey used for deriving Card individual keys
   * @param cupMasterkey128 masterkey used for deriving Card individual keys
   * @param cmsMasterkey256 masterkey used for deriving Card individual keys
   * @param cupMasterkey256 masterkey used for deriving Card individual keys
   * @param adminCmsMasterkeyElc256 masterkey used for deriving Card individual keys
   */
  protected IccProxy(
      final ApduLayer apduLayer,
      final int prk4Sm,
      final String cmsMasterkey128,
      final String cupMasterkey128,
      final String cmsMasterkey256,
      final String cupMasterkey256,
      final String adminCmsMasterkeyElc256) {
    // --- set instance attribute ApduLayer first to enable card communication
    insApduLayer = apduLayer;
    insPrk4Sm = prk4Sm;

    // --- select MF intentionally without AID
    send(new CommandApdu(0x00, 0xA4, 0x04, 0x0C), 0x9000);

    // --- get information from EF.ATR
    final ConstructedBerTlv doEfAtr =
        (ConstructedBerTlv)
            BerTlv.getInstance(
                0xfa, // arbitrary tag, the only thing important is, that this is a constructed tag
                send(new ReadBinary(29, 0, CommandApdu.NE_SHORT_WILDCARD), 0x9000).getData());
    insContentEfAtr = Hex.toHexDigits(doEfAtr.getValueField());

    // APDU length limitations
    insApduLength = new int[4];

    // insApduLength
    final ConstructedBerTlv doBufferSize =
        doEfAtr
            .getConstructed(0xe0)
            .orElse(
                (ConstructedBerTlv)
                    BerTlv.getInstance(
                        0xe0,
                        List.of(
                            new DerInteger(BigInteger.valueOf(1033)), // infimum from (N029.890)a.1
                            new DerInteger(
                                BigInteger.valueOf(32_770)), // infimum from (N029.890)a.2
                            new DerInteger(BigInteger.valueOf(1033)), // infimum from (N029.890)a.3
                            new DerInteger(BigInteger.valueOf(1033)) // infimum from (N029.890)a.4
                            )));
    for (int i = insApduLength.length; i-- > 0; ) { // NOPMD assignment in operand
      insApduLength[i] =
          ((DerInteger) doBufferSize.getPrimitive(DerInteger.TAG, i).orElseThrow())
              .getDecoded()
              .intValueExact();
    } // end For (i...)

    insPtCos = byteToVersion(doEfAtr.getPrimitive(0xd0).orElseThrow().getValueField(), 0);
    insPiChip = productIndicator2string(doEfAtr.getPrimitive(0xd2).orElseThrow().getValueField());
    insPiCos = productIndicator2string(doEfAtr.getPrimitive(0xd3).orElseThrow().getValueField());
    insPiInitializedObjectSystem =
        productIndicator2string(doEfAtr.getPrimitive(0xd4).orElseThrow().getValueField());

    // --- get content of EF.Version2
    insContentEfVersion2 =
        BerTlv.getInstance(
                send(new ReadBinary(17, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000).getData())
            .toStringTree();

    // --- get ICCSN from EF.GDO
    insIccsn =
        Hex.toHexDigits(
            send(new ReadBinary(2, 2, CommandApdu.NE_SHORT_WILDCARD), 0x9000).getData());
    insIccsn8 = insIccsn.substring(insIccsn.length() - 16);

    // --- get CVC CA
    insCvcCaE256 =
        new Cvc(send(new ReadBinary(7, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000).getData());

    // --- derive Card individual keys according to
    //     D:\a\SPE-DK\IssueAfi\2007-07-16_Masterkey\MasterKeyDerivation_v4.doc
    final String suffix1 = "0000 0001";
    final String suffix2 = "0000 0002";
    insCms128enc =
        Hex.toByteArray(
            Hex.toHexDigits(
                EafiHashAlgorithm.SHA_256.digest(
                    Hex.toByteArray(cmsMasterkey128 + insIccsn + suffix1)),
                0, // offset
                16 // length
                ));
    insCms128mac =
        Hex.toByteArray(
            Hex.toHexDigits(
                EafiHashAlgorithm.SHA_256.digest(
                    Hex.toByteArray(cmsMasterkey128 + insIccsn + suffix2)),
                0, // offset
                16 // length
                ));
    insCms256enc =
        Hex.toByteArray(
            Hex.toHexDigits(
                EafiHashAlgorithm.SHA_256.digest(
                    Hex.toByteArray(cmsMasterkey256 + insIccsn + suffix1))));
    insCms256mac =
        Hex.toByteArray(
            Hex.toHexDigits(
                EafiHashAlgorithm.SHA_256.digest(
                    Hex.toByteArray(cmsMasterkey256 + insIccsn + suffix2))));
    insCup128enc =
        Hex.toByteArray(
            Hex.toHexDigits(
                EafiHashAlgorithm.SHA_256.digest(
                    Hex.toByteArray(cupMasterkey128 + insIccsn + suffix1)),
                0, // length
                16 // offset
                ));
    insCup128mac =
        Hex.toByteArray(
            Hex.toHexDigits(
                EafiHashAlgorithm.SHA_256.digest(
                    Hex.toByteArray(cupMasterkey128 + insIccsn + suffix2)),
                0, // offset
                16 // length
                ));
    insCup256enc =
        Hex.toByteArray(
            Hex.toHexDigits(
                EafiHashAlgorithm.SHA_256.digest(
                    Hex.toByteArray(cupMasterkey256 + insIccsn + suffix1))));
    insCup256mac =
        Hex.toByteArray(
            Hex.toHexDigits(
                EafiHashAlgorithm.SHA_256.digest(
                    Hex.toByteArray(cupMasterkey256 + insIccsn + suffix2))));
    final AfiElcParameterSpec domainParameter = AfiElcParameterSpec.brainpoolP256r1;
    insRcaAdminCmsCsE256 =
        new EcPrivateKeyImpl(
            new BigInteger(
                    1,
                    EafiHashAlgorithm.SHA_256.digest(
                        Hex.toByteArray(adminCmsMasterkeyElc256 + insIccsn + "0000 0001")))
                .mod(domainParameter.getOrder()),
            domainParameter);
  } // end default constructor */

  /**
   * Pseudo constructor using the AID of MF to decide the type of card.
   *
   * @param apduLayer used for card communication
   * @return an appropriate subclass
   * @throws IllegalArgumentException if TODO
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.SingletonClassReturningNewInstance"})
  public static IccProxy getInstance(final ApduLayer apduLayer) {
    final String aidMfEgk = Hex.extractHexDigits(IccProxyEgk.AID_MF);
    final String aidMfHba = Hex.extractHexDigits(IccProxyHba.AID_MF);
    final String aidMfSmcB = Hex.extractHexDigits(IccProxySmcB.AID_MF);
    final String aidMfSmcK = Hex.extractHexDigits(IccProxyGsmcK.AID_MF);
    final String aidMfSmcKt = Hex.extractHexDigits(IccProxyGsmcKt.AID_MF);

    // SELECT MF without AID, get FCP
    final ResponseApdu rsp =
        apduLayer.send(new CommandApdu(0x00, 0xa4, 0x04, 0x04, CommandApdu.NE_EXTENDED_WILDCARD));
    final ConstructedBerTlv fcp = (ConstructedBerTlv) BerTlv.getInstance(rsp.getData());

    // --- loop over all AIDs in FCP
    try {
      for (int i = 0; true; i++) {
        final String aid = Hex.toHexDigits(fcp.getPrimitive(0x84, i).orElseThrow().getValueField());

        if (aidMfEgk.equals(aid)) {
          return new IccProxyEgk(apduLayer);
        } else if (aidMfHba.equals(aid)) {
          return new IccProxyHba(apduLayer);
        } else if (aidMfSmcB.equals(aid)) {
          return new IccProxySmcB(apduLayer);
        } else if (aidMfSmcK.equals(aid)) {
          return new IccProxyGsmcK(apduLayer);
        } else if (aidMfSmcKt.equals(aid)) {
          return new IccProxyGsmcKt(apduLayer);
        } // end else if
      } // end For (i...)
    } catch (IllegalArgumentException e) {
      if ("DO not found".equals(e.getMessage())) { // NOPMD literal in if statement
        throw new NoSuchElementException("unknown card type", e);
      } else {
        throw e;
      } // end fi
    } // end Catch (...)
  } // end method */

  /**
   * Converts three octet to a version number.
   *
   * <p>E.g.: '031117' => 3.17.23
   *
   * @param input with version number
   * @param offset position of first octet of version number
   * @return version number
   */
  private String byteToVersion(final byte[] input, final int offset) {
    // Note: For interpretation see [gemSpec_Karten_Fach_TIP#Card-G2-A_3479]
    final int hauptVersion = input[offset] & 0xff;
    final int nebenVersion = input[offset + 1] & 0xff;
    final int revision = input[offset + 2] & 0xff;
    return hauptVersion + "." + nebenVersion + "." + revision;
  } // end method */

  /**
   * Converts product indicator value field to a human-readable string.
   *
   * @param pi product indicator, see gemSpec_eGK_Fach_TIP
   * @return product indicator in human-readable form
   */
  private String productIndicator2string(final byte[] pi) {
    // Note: for interpretation see [gemSpec_Karten_Fach_TIP#Card-G2-A_3480]
    final String herstellerId = new String(pi, 0, 5, StandardCharsets.UTF_8);
    final String produktKuerzel = new String(pi, 5, 8, StandardCharsets.UTF_8);
    final String version = byteToVersion(pi, 13);

    return "Hersteller-ID="
        + herstellerId
        + ", Produktkürzel="
        + produktKuerzel
        + ", Versionsnummer="
        + version;
  } // end method */

  /**
   * Getter.
   *
   * @return SK.CMS.AES128.enc
   */
  public byte[] getCms128Enc() {
    return insCms128enc.clone();
  } // end method */

  /**
   * Getter.
   *
   * @return SK.CMS.AES128.mac
   */
  public byte[] getCms128Mac() {
    return insCms128mac.clone();
  } // end method */

  /**
   * Getter.
   *
   * @return SK.CMS.AES256.enc
   */
  public byte[] getCms256Enc() {
    return insCms256enc.clone();
  } // end method */

  /**
   * Getter.
   *
   * @return SK.CMS.AES256.mac
   */
  public byte[] getCms256Mac() {
    return insCms256mac.clone();
  } // end method */

  /**
   * Getter.
   *
   * @return SK.CUP.AES128.enc
   */
  public byte[] getCup128Enc() {
    return insCup128enc.clone();
  } // end method */

  /**
   * Getter.
   *
   * @return SK.CUP.AES128.mac
   */
  public byte[] getCup128Mac() {
    return insCup128mac.clone();
  } // end method */

  /**
   * Getter.
   *
   * @return SK.CUP.AES256.enc
   */
  public byte[] getCup256Enc() {
    return insCup256enc.clone();
  } // end method */

  /**
   * Getter.
   *
   * @return SK.CUP.AES256.mac
   */
  public byte[] getCup256Mac() {
    return insCup256mac.clone();
  } // end method */

  /**
   * Getter.
   *
   * @return the content_EF_ATR
   */
  public final String getContentEfAtr() {
    return insContentEfAtr;
  } // end method */

  /**
   * Getter.
   *
   * @return ICCSN
   */
  public final String getIccsn() {
    return insIccsn;
  } // end method */

  /**
   * Getter.
   *
   * @return 8 LSByte from ICCSN
   */
  public final String getIccsn8() {
    return insIccsn8;
  } // end method */

  /**
   * Getter.
   *
   * @return amount of octet in an unsecured command APDU
   */
  public final int getMaxOctetCmdPlain() {
    return insApduLength[0];
  } // end method */

  /**
   * Getter.
   *
   * @return amount of octet in an unsecured response APDU
   */
  public final int getMaxOctetRspPlain() {
    return insApduLength[1];
  } // end method */

  /**
   * Getter.
   *
   * @return amount of octet in an unsecured command APDU
   */
  public final int getMaxOctetCmdSm() {
    return insApduLength[2];
  } // end method */

  /**
   * Getter.
   *
   * @return amount of octet in an unsecured response APDU
   */
  public final int getMaxOctetRspSm() {
    return insApduLength[3];
  } // end method */

  /**
   * Getter.
   *
   * @return the PI_COS
   */
  public final String getPiCos() {
    return insPiCos;
  } // end method */

  /**
   * Getter.
   *
   * @return the PI_Chip
   */
  public final String getPiChip() {
    return insPiChip;
  } // end method */

  /**
   * Getter.
   *
   * @return the PI_InitializedObjectSystem
   */
  public final String getPiInitializedObjectSystem() {
    return insPiInitializedObjectSystem;
  } // end method */

  /**
   * Returns key reference of a private key.
   *
   * <p>The private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM
   * ELC_SESSIONKEY_4_SM}.
   *
   * @return key reference of the private key used for CV-certificate authentication
   */
  /* package */ int getPrk4Sm() {
    return insPrk4Sm;
  } // end method */

  /**
   * Getter.
   *
   * @return PrK.RCA.AdminCMS.CS.E256
   */
  public EcPrivateKeyImpl getPrkAdminCmsE256() {
    return insRcaAdminCmsCsE256; // EI_EXPOSE_REP
  } // end method */

  /**
   * Getter.
   *
   * @return product type COS
   */
  public final String getPtCos() {
    return insPtCos;
  } // end method */

  /**
   * Converter to string.
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format("ICCSN               = %s%n", getIccsn())
        + String.format(
            "content EF.ATR      = %n    %s%n",
            ((ConstructedBerTlv) BerTlv.getInstance(0xfa, getContentEfAtr()))
                .getTemplate().stream()
                    .map(BerTlv::toStringTree)
                    .collect(Collectors.joining(LINE_SEPARATOR))
                    .replace(LINE_SEPARATOR, LINE_SEPARATOR + "    "))
        + String.format("    PT_COS          = %s%n", getPtCos())
        + String.format("    PI_Chip         = %s%n", getPiChip())
        + String.format("    PI_COS          = %s%n", getPiCos())
        + String.format("    PI_Init.ObjSys  = %s%n", getPiInitializedObjectSystem())
        + String.format(
            "content EF.Version2 = %n    %s%n",
            insContentEfVersion2.replace(LINE_SEPARATOR, LINE_SEPARATOR + "    "));
  } // end method */

  /**
   * Return execution time of the previous command-response pair.
   *
   * @return execution time in seconds of previous command-response pair
   * @see MessageLayer#getTime()
   */
  @Override
  public double getTime() {
    return insApduLayer.getTime();
  } // end method */

  /**
   * Sends given message.
   *
   * @param message to be sent, typically a command
   * @return corresponding response message
   */
  @Override
  public final byte[] send(final byte[] message) {
    return insApduLayer.send(message);
  } // end method */

  /**
   * Sends given message.
   *
   * @param commandApdu to be sent
   * @return corresponding response message
   */
  @Override
  public final ResponseApdu send(final CommandApdu commandApdu) {
    return insApduLayer.send(commandApdu);
  } // end method */

  /**
   * Sends given {@link CommandApdu} and compares trailer of corresponding {@link ResponseApdu} to
   * expected trailers.
   *
   * @param cmdApdu command APDU to be sent
   * @param expectedTrailer values of trailer (SW1 SW2) expected in {@link ResponseApdu}
   * @return corresponding {@link ResponseApdu}
   * @throws IllegalArgumentException if the trailer in the {@link ResponseApdu} does not match any
   *     of the expected trailers
   */
  @Override
  public final ResponseApdu send(final CommandApdu cmdApdu, final int... expectedTrailer) {
    return ApduLayer.super.send(cmdApdu, expectedTrailer);
  } // end method */

  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  /* package */ void importChain(final List<Cvc> chain) {
    final int[] acceptedTrailerUp = new int[] {0x6a88, 0x9000};
    final int[] acceptedTrailerDown = new int[] {0x9000};
    int[] acceptedTrailer = acceptedTrailerUp;
    int delta = 1;

    for (int index = 0; true; index += delta) {
      final Cvc cvcInFocus = chain.get(index);

      final String keyReference = cvcInFocus.getCar();
      final CommandApdu mseSet =
          new CommandApdu(0, 0x22, 0x81, 0xb6, BerTlv.getInstance(0x83, keyReference).getEncoded());

      LOGGER
          .atTrace()
          .log("i={}, d={}, a[0]={}", index, delta, Integer.toString(acceptedTrailer[0], 16));
      final ResponseApdu rsp = send(mseSet, acceptedTrailer);

      if (0x9000 == rsp.getTrailer()) { // NOPMD literal in if statement
        // ... signature verification key successfully selected
        //     => import that CV-certificate

        if (0x6a88 == acceptedTrailer[0]) { // NOPMD literal in if statement
          // ... first time ICC responded with NoError
          //     => switch direction
          delta *= -1;
          acceptedTrailer = acceptedTrailerDown;
        } // end fi

        final CommandApdu psoVerifyCertificate =
            new CommandApdu(0x00, 0x2a, 0x00, 0xbe, cvcInFocus.getValueField());
        send(psoVerifyCertificate, 0x9000);

        if ((-1 == delta) && (0 == index)) {
          // ... authentication key successfully imported
          //     => we are done
          break;
        } // end fi
      } // end fi
    } // end For (index...)
  } // end method */

  /**
   * Getter.
   *
   * @return CV-certificate from file / MF / EF.C.CA.CS.E256
   */
  public final Cvc getCvcCaE256() {
    return insCvcCaE256;
  } // end method */

  /**
   * Retrieves CV-certificate used for session key negotiation.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM
   * ELC_SESSIONKEY_4_SM}.
   *
   * @return CV-certificate used for session key negotiation
   * @throws NoSuchElementException if an appropriate {@link Cvc} is absent
   */
  public abstract Cvc getCvc4Sm(); // */
} // end class
