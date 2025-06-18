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
package de.gematik.smartcards.g2icc.cvc;

import static de.gematik.smartcards.g2icc.cvc.Cvc.SignatureStatus.SIGNATURE_INVALID;
import static de.gematik.smartcards.g2icc.cvc.Cvc.SignatureStatus.SIGNATURE_NO_PUBLIC_KEY;
import static de.gematik.smartcards.g2icc.cvc.Cvc.SignatureStatus.SIGNATURE_UNKNOWN;
import static de.gematik.smartcards.g2icc.cvc.Cvc.SignatureStatus.SIGNATURE_VALID;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;

import de.gematik.smartcards.crypto.AfiElcParameterSpec;
import de.gematik.smartcards.crypto.AfiElcUtils;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.math.BigInteger;
import java.security.spec.ECPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class provides information stored in a card-verifiable certificate G2.
 *
 * <p>According to the document gemSpec_PKI from <a href="https://fachportal.gematik.de">gematik</a>
 * card-verifiable certificates (CV-certificates, CVC) used in the so-called generation 2 Smart
 * Cards have the following structure (tags defined in ISO/IEC 7816-6:2022):
 *
 * <pre>
 *   7f21 - L                      # cardholder certificate template
 *    7f4e - L                       # certificate content template
 *       5f29 01 70                    # interchange profil
 *       42 08 4445475858860220        # CAR, issuer identification number
 *       7f49 - L                      # cardholder public key template
 *          06 - L 2a8648ce3d040302      # OID with public key usage
 *          86 - L 04316d35a...29d       # public point in encoded format
 *       5f20 - L 4445475858110220     # CHR, card holder name
 *       7f4c 13                       # CHAT, certificate holder authorization template
 *          06 08 2a8214004c048118       # OID of flag list
 *          53 07 80ff7bdfe1ff0c         # flag list
 *       5f25 06 020000040009          # CED, certificate effective date
 *       5f24 06 020800040008          # CXD, certificate expiration date
 *    5f37 - L 71ea6544...477f33c2   # signature R || S
 * </pre>
 *
 * <p>From the perspective of this class
 *
 * <ol>
 *   <li>instances are immutable value-types. Thus, {@link #equals(Object)}, {@link #hashCode()} are
 *       overwritten. Method {@link #clone()} is not overwritten, because instances are immutable.
 *   <li>where data is passed in or out, defensive cloning is performed.
 *   <li>methods are thread-safe.
 * </ol>
 *
 * <p>It follows that from the perspective of this class object sharing is possible without side
 * effects.
 *
 * <p>FIXME Cyclomatic complexity:
 *
 * <ol>
 *   <li>Before refactoring : WMC=119
 *   <li>extra class for CPI : WMC=117, ATFD=46, TCC=7.225%
 *   <li>extra classes for CAR, CHR: WMC=112, ATFD=52, TCC=9.206%
 *   <li>extra classes for CED, CXD: WMC=108, ATFD=57, TCC=11,492
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "EI_EXPOSE_REP",
//         Short message: Returning a reference to a mutable object value stored
//             in one of the object's fields exposes the internal representation
//             of the object.
//         Rational: That finding is suppressed because EcPublicKeyImpl
//             is immutable.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP", // see note 1
}) // */
@SuppressWarnings({
  "PMD.AvoidCatchingGenericException",
  "PMD.AvoidLiteralsInIfCondition",
  "PMD.AvoidUsingVolatile",
  "PMD.BeanMembersShouldSerialize",
  "PMD.CollapsibleIfStatements",
  "PMD.ConstructorCallsOverridableMethod",
  "PMD.CyclomaticComplexity",
  "PMD.ExcessiveMethodLength",
  "PMD.ExcessiveParameterList",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.NcssCount",
  "PMD.PrematureDeclaration",
  "PMD.ShortClassName",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports"
})
public final class Cvc {

  /**
   * Flag list CMS.
   *
   * <p>Information is taken from [gemSpec_PKI#Tab_PKI_911].
   */
  /* package */ static final String[] CVC_FLAG_LIST_CMS =
      new String[] {
        "b00", // b00 and b01 together indicate whether this is an RCA certificate
        "b01", // an Sub-CA certificate or an end-user certificate, see below
        "b02 => RFU",
        "b03 => RFU",
        "b04 => RFU",
        "b05 => RFU",
        "b06 => RFU",
        "b07 => RFU",
        "b08 => Administrative Tätigkeiten CMS",
        "b09 => Administrative Tätigkeiten VSD",
        "b10 => Administrative Tätigkeiten zum Schreiben von CV-Zertifikaten",
        "b11 => Administrative Tätigkeiten eines ZDA zur Laufzeitverlängerung der QES-Anwendung",
        "b12 => RFU",
        "b13 => RFU",
        "b14 => RFU",
        "b15 => RFU",
        "b16 => RFU",
        "b17 => RFU",
        "b18 => RFU",
        "b19 => RFU",
        "b20 => RFU",
        "b21 => RFU",
        "b22 => RFU",
        "b23 => RFU",
        "b24 => RFU",
        "b25 => RFU",
        "b26 => RFU",
        "b27 => RFU",
        "b28 => RFU",
        "b29 => RFU",
        "b30 => RFU",
        "b31 => RFU",
        "b32 => RFU",
        "b33 => RFU",
        "b34 => RFU",
        "b35 => RFU",
        "b36 => RFU",
        "b37 => RFU",
        "b38 => RFU",
        "b39 => RFU",
        "b40 => RFU",
        "b41 => RFU",
        "b42 => RFU",
        "b43 => RFU",
        "b44 => RFU",
        "b45 => RFU",
        "b46 => RFU",
        "b47 => RFU",
        "b48 => RFU",
        "b49 => RFU",
        "b50 => RFU",
        "b51 => RFU",
        "b52 => RFU",
        "b53 => RFU",
        "b54 => RFU",
        "b55 => RFU",
      }; // end cvc_FlagList_CMS */

  /**
   * Flag list TI.
   *
   * <p>The following information is in accordance to gemSpec_PKI, v.2.11.1 from 28th January 2022,
   * Tab_PKI_910.
   */
  /* package */ static final String[] CVC_FLAG_LIST_TI =
      new String[] {
        "b00", // b00 and b01 together indicate whether this is an RCA certificate
        "b01", // a Sub-CA certificate or an end-user certificate, see below
        "b02 => RFU",
        "b03 => RFU",
        "b04 => RFU",
        "b05 => RFU",
        "b06 => RFU",
        "b07 => RFU",
        "b08 => eGK: Verwendung der ESIGN-AUTN Funktionalität mit PIN.CH",
        "b09 => eGK: Verwendung der ESIGN-AUTN Funktionalität ohne PIN",
        "b10 => eGK: Verwendung der ESIGN-ENCV Funktionalität mit PIN.CH",
        "b11 => eGK: Verwendung der ESIGN-ENCV Funktionalität ohne PIN",
        "b12 => eGK: Verwendung der ESIGN-AUT Funktionalität",
        "b13 => eGK: Verwendung der ESIGN-ENC Funktionalität",
        "b14 => eGK: Notfalldatensatz verbergen und sichtbar machen",
        "b15 => eGK: Notfalldatensatz schreiben, löschen (hier 'erase', nicht 'delete')",
        "b16 => RFU",
        "b17 => eGK: Notfalldatensatz lesen mit MRPIN.NFD",
        "b18 => eGK: Notfalldatensatz lesen ohne PIN",
        "b19 => eGK: Persönliche Erklärungen (DPE) verbergen und sichtbar machen",
        "b20 => eGK: DPE schreiben, löschen (hier 'erase', nicht 'delete')",
        "b21 => RFU",
        "b22 => eGK: DPE lesen mit MRPIN.DPE_Read",
        "b23 => eGK: DPE lesen ohne PIN",
        "b24 => eGK: Einwilligungen und Verweise im DF.HCA verbergen und sichtbar machen",
        "b25 => eGK: Einwilligungen im DF.HCA lesen und löschen (hier 'erase', nicht 'delete')",
        "b26 => RFU",
        "b27 => eGK: Einwilligungen im DF.HCA schreiben",
        "b28 => eGK: Verweise im DF.HCA lesen und schreiben",
        "b29 => eGK: Geschützte Versichertendaten lesen mit PIN.CH",
        "b30 => eGK: Geschützte Versichertendaten lesen ohne PIN",
        "b31 => eGK: Loggingdaten schreiben mit PIN.CH",
        "b32 => eGK: Loggingdaten schreiben ohne PIN",
        "b33 => eGK: Zugriff in den AdV-Umgebungen", // previous action: read logging data
        "b34 => eGK: Prüfungsnachweis lesen und schreiben",
        "b35 => RFU", // previous action: eGK: Testkennzeichen lesen mit PIN.CH
        "b36 => RFU", // previous action: eGK: Testkennzeichen lesen ohne PIN
        "b37 => RFU",
        "b38 => RFU",
        "b39 => eGK: Gesundheitsdatendienste verbergen und sichtbar machen",
        "b40 => eGK: Gesundheitsdatendienste lesen, schreiben und löschen (hier 'erase')",
        "b41 => eGK: Organspendedatensatz lesen mit MRPIN.OSE",
        "b42 => eGK: Organspendedatensatz lesen ohne PIN",
        "b43 => eGK: Organspendedatensatz schreiben, löschen (hier 'erase', nicht 'delete')",
        "b44 => eGK: Organspendedatensatz verbergen und sichtbar machen",
        "b45 => eGK: AMTS-Datensatz verbergen und sichtbar machen",
        "b46 => eGK: AMTS-Datensatz lesen",
        "b47 => eGK: AMTS-Datensatz schreiben, löschen (hier „erase“, nicht „delete“)",
        "b48 => RFU",
        "b49 => RFU", // previous action: Fingerprint des COS erstellen
        "b50 => RFU",
        "b51 => Auslöser Komfortsignatur",
        "b52 => Sichere Signaturerstellungseinheit (SSEE)",
        "b53 => Remote PIN Empfänger",
        "b54 => Remote PIN Sender",
        "b55 => SAK für Stapel- oder Komfortsignatur",
      }; // end FlagList_TI */

  /**
   * Tag for certification authority reference, CAR.
   *
   * <p>Defined in ISO/IEC 7816-6:2022 as "issuer identification number".
   */
  public static final int TAG_CAR = 0x42; // */

  /**
   * Tag for cardholder certificate template.
   *
   * <p>Defined in ISO/IEC 7816-6:2022.
   */
  public static final int TAG_CARDHOLDER_CERTIFICATE_TEMPLATE = 0x7f21; // NOPMD long name */

  /**
   * Tag for certificate effective date, CED.
   *
   * <p>Defined in ISO/IEC 7816-6:2022.
   */
  public static final int TAG_CED = 0x5f25; // */

  /**
   * Tag for certificate content template.
   *
   * <p>Defined in ISO/IEC 7816-6:2022.
   */
  public static final int TAG_CERTIFICATE_CONTENT_TEMPLATE = 0x7f4e; // NOPMD long name */

  /**
   * Tag for certificate holder authorization template, CHAT.
   *
   * <p>Defined in ISO/IEC 7816-6:2022.
   */
  public static final int TAG_CHAT = 0x7f4c; // */

  /**
   * Tag for cardholder reference, CHR.
   *
   * <p>Defined in ISO/IEC 7816-6:2022 as "cardholder name".
   */
  public static final int TAG_CHR = 0x5f20; // */

  /**
   * Tag for certificate profile indicator, CPI.
   *
   * <p>Defined in ISO/IEC 7816-6:2022 as "interchange profile".
   */
  public static final int TAG_CPI = 0x5f29; // */

  /**
   * Tag for certificate expiration date, CXD.
   *
   * <p>Defined in ISO/IEC 7816-6:2022.
   */
  public static final int TAG_CXD = 0x5f24; // */

  /**
   * Tag for the flag list within a DO CHAT.
   *
   * <p>Defined in ISO/IEC 7816-6:2022 as "discretionary data".
   */
  public static final int TAG_FLAG_LIST = 0x53; // */

  /**
   * Tag for public point within a DO for public key.
   *
   * <p>Defined in ISO/IEC 7816-8:2021.
   */
  public static final int TAG_PUBLIC_POINT = 0x86; // */

  /**
   * Tag for cardholder public key template.
   *
   * <p>Defined in ISO/IEC 7816-6:2022.
   */
  public static final int TAG_PUK_TEMPLATE = 0x7f49; // */

  /**
   * Tag for signature.
   *
   * <p>Defined in ISO/IEC 7816-6:2022.
   */
  public static final int TAG_SIGNATURE = 0x5f37; // */

  /** Certification authority reference (CAR). */
  private final CertificationAuthorityReference insCar; // */

  /** Cardholder reference (CHR). */
  private final CardHolderReference insChr; // */

  /** Certificate effective date, CED. */
  private final CertificateDate insCed; // */

  /** Certificate expiration date, CXD. */
  private final CertificateDate insCxd; // */

  /** Certificate profile indicator, CPI. */
  @VisibleForTesting // otherwise = private
  /* package */ final CertificateProfileIndicator insCpi; // */

  /** Content of CVC as given in a constructor. */
  private final ConstructedBerTlv insCvcContent; // */

  /**
   * Flag indicating if the card-verifiable certificate contains critical errors.
   *
   * <p>Intentionally this attribute is final and set within the constructor, although not all
   * checks are provided there. Additional checks require information from the trust center, which
   * may not be present at construction time. Thus, the {@link #hasCriticalFindings()}-result may
   * vary.
   */
  private final boolean insCriticalFindings; // */

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Because only immutable instance attributes of this class are taken into account for
   *       this instance attribute lazy initialization is possible.</i>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  private volatile int insHashCode; // */

  /**
   * Explanation of card-verifiable certificate content.
   *
   * <p>Intentionally this attribute is final and set within the constructor, although not all
   * checks are provided there. Additional checks require information from the trust center, which
   * may not be present at construction time. Thus, the {@link #getExplanation()}-result may vary.
   */
  @VisibleForTesting // otherwise = private
  /* package */ final List<String> insExplanation; // */

  /** List of flags indicating access rights. */
  @VisibleForTesting // otherwise = private
  /* package */ final byte[] insFlagList; // */

  /** Message containing certified information. */
  @VisibleForTesting // otherwise = private
  /* package */ final ConstructedBerTlv insMessage; // */

  /** Object identifier identifying a flag list interpretation. */
  @VisibleForTesting // otherwise = private
  /* package */ final AfiOid insOidFlagList; // */

  /** Object identifier stating the purpose of the enclosed public key. */
  @VisibleForTesting // otherwise = private
  /* package */ final AfiOid insOidPuk; // */

  /**
   * Public key enclosed in this certificate, here in encoded format.
   *
   * <p>Intentionally the type of this attribute is such that it can stare arbitrary content of
   * arbitrary length. This way, the constructor does not throw exceptions in case the octet string
   * is not a valid public key encoding.
   */
  @VisibleForTesting // otherwise = private
  /* package */ final byte[] insPuK; // */

  /**
   * Public key enclosed in this certificate.
   *
   * <p>If {@code NULL} then no valid public key could be extracted from certificate content.
   */
  private final @Nullable EcPublicKeyImpl insPublicKey; // */

  /**
   * Report.
   *
   * <p>Intentionally this attribute is final and set within the constructor, although not all
   * checks are provided there. Additional checks require information from the trust center, which
   * may not be present at construction time. Thus, the {@link #getReport()}-result may vary.
   */
  private final List<String> insReport; // */

  /**
   * Signature.
   *
   * <p>Intentionally the type of this attribute is such that it can stare arbitrary content of
   * arbitrary length. This way the constructor does not throw exceptions in case the octet string
   * is not a valid representation of a signature generated with elliptic curve cryptography.
   */
  @VisibleForTesting // otherwise = private
  /* package */ final byte[] insSignature; // */

  /**
   * Status of signature.
   *
   * <p>Intentionally, this instance attribute is not final, because it may change over time, e.g.,
   * a public key necessary for verifying the signature is added to the trust center after
   * instantiation of this object.
   */
  private volatile SignatureStatus insSignatureStatus = SIGNATURE_UNKNOWN; // */

  /**
   * Content of CV-certificate, see {@link #getValueField()}.
   *
   * <p>Intentionally, this instance attribute is not final, because lazy initialization is used.
   */
  @VisibleForTesting // otherwise = private
  /* package */ byte[] insValueField = AfiUtils.EMPTY_OS; // */

  /**
   * Comfort constructor.
   *
   * @param cpi certificate profile indicator
   * @param car certification authority reference
   * @param oidPukUsage object identifier (OID) indicating the intended usage of the public key
   * @param puk public key contained in the card-verifiable certificate
   * @param chr card-holder reference
   * @param oidFlagList object identifier (OID) indicating the scheme used to interpret the flag
   *     list
   * @param flagList list of flags with access rights of the identity to which the public key
   *     belongs
   * @param ced certificate effective date (kind of "valid from")
   * @param cxd certificate expiration date (kind of "valid to")
   * @param signingKey private key used to sign the certificate content
   */
  public Cvc(
      final CertificateProfileIndicator cpi,
      final CertificationAuthorityReference car,
      final AfiOid oidPukUsage,
      final EcPublicKeyImpl puk,
      final CardHolderReference chr,
      final AfiOid oidFlagList,
      final byte[] flagList,
      final CertificateDate ced,
      final CertificateDate cxd,
      final EcPrivateKeyImpl signingKey) {
    // Note 1: SonarQube claims the following major finding:
    //         "Constructor has 10 parameters, which is greater than 7 authorized."
    // Note 2: This will NOT be fixed.
    //         A CVC needs ten parameters when constructed from scratch.
    //         I will not combine parameters to just satisfy SonarQube.
    this(
        (ConstructedBerTlv)
            BerTlv.getInstance(
                TAG_CERTIFICATE_CONTENT_TEMPLATE,
                List.of(
                    cpi.getDataObject(),
                    car.getDataObject(),
                    BerTlv.getInstance(
                        TAG_PUK_TEMPLATE,
                        List.of(
                            new DerOid(oidPukUsage),
                            BerTlv.getInstance(
                                0x86,
                                AfiElcUtils.p2osUncompressed(
                                    puk.getW(), puk.getParams())))), // end DO-PublicKey
                    chr.getDataObject(),
                    BerTlv.getInstance(
                        TAG_CHAT,
                        List.of(
                            new DerOid(oidFlagList),
                            BerTlv.getInstance(TAG_FLAG_LIST, flagList))), // end DO-CHAT
                    ced.getDataObject(),
                    cxd.getDataObject())),
        signingKey);
  } // end method */

  /**
   * Comfort constructor.
   *
   * @param message to be signed, i.e. the certificate content
   * @param signingKey private key used to sign the certificate content
   */
  private Cvc(final ConstructedBerTlv message, final EcPrivateKeyImpl signingKey) {
    this(
        (ConstructedBerTlv)
            BerTlv.getInstance(
                TAG_CARDHOLDER_CERTIFICATE_TEMPLATE,
                List.of(
                    message,
                    BerTlv.getInstance(
                        TAG_SIGNATURE, signingKey.signEcdsa(message.getEncoded())))));
  } // end method */

  /**
   * Constructor using an octet string representation of a card-verifiable certificate (CVC).
   *
   * <p>According to the document gemSpec_PKI from <a
   * href="https://fachportal.gematik.de">gematik</a> card-verifiable certificates (CV-certificates,
   * CVC) used in the so-called generation 2 Smart Cards have the following structure (tags defined
   * in ISO/IEC 7816-6:2022):
   *
   * <pre>
   *   7f21 81d8                     # cardholder certificate template
   *    7f4e 8191                      # certificate content template
   *       5f29 01 70                    # interchange profil
   *       42 08 4445475858860220        # CAR, issuer identification number
   *       7f49 4d                       # cardholder public key temp
   *          06 08 2a8648ce3d040302       # OID with public key usage
   *          86 41 04316d35a...29d        # public point in uncompressed format
   *       5f20 08 4445475858110220      # CHR, cardholder name
   *       7f4c 13                       # CHAT, certificate holder authorization template
   *          06 08 2a8214004c048118       # OID of flag list
   *          53 07 80ff7bdfe1ff0c         # flag list
   *       5f25 06 020000040009          # CED, certificate effective date
   *       5f24 06 020800040008          # CXD, certificate expiration date
   *    5f37 40 71ea6544...477f33c2    # signature R || S
   * </pre>
   *
   * <p>Intentionally this constructor is VERY lazy, i.e. it accepts almost any certificate content.
   * Conformity to a specification is checked by {@link #check()}. Findings could be read by {@link
   * #getReport()}.
   *
   * <p>CV-certificates without critical findings (see {@link #hasCriticalFindings()}) are added to
   * the cache of {@link TrustCenter}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are defensively
   *       cloned.</i>
   * </ol>
   *
   * @param octets content of card-verifiable certificate (CV-certificate, CVC)
   * @throws NoSuchElementException if
   *     <ol>
   *       <li>{@code cvc} contains no DO'7f4e', i.e. certificate content template
   *       <li>{@code cvc} contains no DO'5f37', i.e. signature R || S
   *       <li>DO'7f4e' contains no DO'5f29', i.e. interchange profile
   *       <li>DO'7f4e' contains no DO'42', i.e. CAR, issuer identification number
   *       <li>DO'7f4e' contains no DO'7f49', i.e. cardholder public key template
   *       <li>DO'7f4e' contains no DO'5f20', i.e. CHR, cardholder name
   *       <li>DO'7f4e' contains no DO'7f4c', i.e. CHAT, certificate holder authorization template
   *       <li>DO'7f4e' contains no DO'5f25', i.e. CED, certificate effective date
   *       <li>DO'7f4e' contains no DO'5f24', i.e. CXD, certificate expiration date
   *       <li>DO'7f49' contains no DO'06', i.e. OID with public key usage
   *       <li>DO'7f49' contains no DO'86', i.e. public point in encoded format
   *       <li>DO'7f4c' contains no DO'06', i.e. OID of flag list
   *       <li>DO'7f4c' contains no DO'53', i.e. flag list
   *     </ol>
   *
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>octet string is not in accordance to ISO/IEC 8825:2008
   *       <li>tag-field contains more than eight octet
   *     </ol>
   */
  public Cvc(final byte[] octets) {
    this((ConstructedBerTlv) BerTlv.getInstance(octets));

    TrustCenter.add(this);
  } // end constructor */

  /**
   * Constructor using {@link ConstructedBerTlv} representation of CVC.
   *
   * <p>This constructor is called by {@link Cvc#Cvc(byte[])}. The only difference is, that this
   * constructor does not add CV-certificates to caches of {@link TrustCenter}.
   *
   * @param cvc content of card-verifiable certificate (CV-certificate, CVC)
   * @throws NoSuchElementException if
   *     <ol>
   *       <li>{@code cvc} contains no DO'7f4e', i.e. certificate content template
   *       <li>{@code cvc} contains no DO'5f37', i.e. signature R || S
   *       <li>DO'7f4e' contains no DO'5f29', i.e. interchange profile
   *       <li>DO'7f4e' contains no DO'42', i.e. CAR, issuer identification number
   *       <li>DO'7f4e' contains no DO'7f49', i.e. cardholder public key template
   *       <li>DO'7f4e' contains no DO'5f20', i.e. CHR, cardholder name
   *       <li>DO'7f4e' contains no DO'7f4c', i.e. CHAT, certificate holder authorization template
   *       <li>DO'7f4e' contains no DO'5f25', i.e. CED, certificate effective date
   *       <li>DO'7f4e' contains no DO'5f24', i.e. CXD, certificate expiration date
   *       <li>DO'7f49' contains no DO'06', i.e. OID with public key usage
   *       <li>DO'7f49' contains no DO'86', i.e. public point in encoded format
   *       <li>DO'7f4c' contains no DO'06', i.e. OID of flag list
   *       <li>DO'7f4c' contains no DO'53', i.e. flag list
   *     </ol>
   */
  /* package */ Cvc(final ConstructedBerTlv cvc) {
    // --- store given input parameter for later use
    insCvcContent = cvc;

    // --- extract relevant information from given DO, i.e. cvc
    insMessage = cvc.getConstructed(TAG_CERTIFICATE_CONTENT_TEMPLATE).orElseThrow();
    insSignature = cvc.getPrimitive(TAG_SIGNATURE).orElseThrow().getValueField();

    // --- extract relevant information from DO'7f4e', i.e. message
    insCpi = new CertificateProfileIndicator(insMessage.getPrimitive(TAG_CPI).orElseThrow());
    insCar = new CertificationAuthorityReference(insMessage.getPrimitive(TAG_CAR).orElseThrow());
    final ConstructedBerTlv doPuk = insMessage.getConstructed(TAG_PUK_TEMPLATE).orElseThrow();
    insChr = new CardHolderReference(insMessage.getPrimitive(TAG_CHR).orElseThrow());
    final ConstructedBerTlv chat = insMessage.getConstructed(TAG_CHAT).orElseThrow();
    insCed = new CertificateDate(insMessage.getPrimitive(TAG_CED).orElseThrow());
    insCxd = new CertificateDate(insMessage.getPrimitive(TAG_CXD).orElseThrow());

    // --- extract relevant information from DO'7f49', i.e. cardholder public key template
    insOidPuk = ((DerOid) doPuk.getPrimitive(DerOid.TAG).orElseThrow()).getDecoded();
    insPuK = doPuk.getPrimitive(TAG_PUBLIC_POINT).orElseThrow().getValueField();
    final byte msByte = insPuK[0];
    final List<String> report = new ArrayList<>(); // list with findings
    boolean criticalFindings = false; // flag indicating critical findings
    final Optional<AfiElcParameterSpec> domainParameter;
    // spotless:off
    switch (msByte) {
      case 0x02, 0x03 -> { // compressed encoding
        criticalFindings = true;
        report.add("public key with compressed encoding");
        domainParameter = switch (insPuK.length) {
          case 0x21 -> Optional.of(AfiElcParameterSpec.brainpoolP256r1);
          case 0x31 -> Optional.of(AfiElcParameterSpec.brainpoolP384r1);
          case 0x41 -> Optional.of(AfiElcParameterSpec.brainpoolP512r1);
          default -> Optional.empty();
        }; // end Switch (PuK length)
      } // end case '02', '03'

      case 0x04 -> domainParameter = switch (insPuK.length) {
        case 0x41 -> Optional.of(AfiElcParameterSpec.brainpoolP256r1);
        case 0x61 -> Optional.of(AfiElcParameterSpec.brainpoolP384r1);
        case 0x81 -> Optional.of(AfiElcParameterSpec.brainpoolP512r1);
        default -> Optional.empty();
      }; // end Switch (PuK length)

      default -> throw new IllegalArgumentException(
          String.format("invalid coding for public key: '%02x'", msByte)
      );
    } // end Switch (compression indicator)
    // spotless:on

    EcPublicKeyImpl tmpPuk; // NOPMD could be final (false positive)
    try {
      final AfiElcParameterSpec dp = domainParameter.orElseThrow();
      tmpPuk = new EcPublicKeyImpl(AfiElcUtils.os2p(insPuK, dp), dp);
    } catch (RuntimeException e) {
      tmpPuk = null; // NOPMD assigning to null is a code smell
    } // end Catch (...)
    insPublicKey = tmpPuk;

    // --- extract relevant information from DO'7f4c'=CHAT=certificate holder authorization template
    insOidFlagList = ((DerOid) chat.getPrimitive(DerOid.TAG).orElseThrow()).getDecoded();
    insFlagList = chat.getPrimitive(TAG_FLAG_LIST).orElseThrow().getValueField();

    // --- report and explain attributes
    criticalFindings |= checkTag(report);

    criticalFindings |= insCpi.hasCriticalFindings(); // CPI
    final List<String> explanation = new ArrayList<>(insCpi.getExplanation());
    report.addAll(insCpi.getReport());

    criticalFindings |= insChr.hasCriticalFindings(); // CHR
    explanation.addAll(insChr.getExplanation());
    report.addAll(insChr.getReport());

    criticalFindings |= insCar.hasCriticalFindings(); // CAR
    explanation.addAll(insCar.getExplanation());
    report.addAll(insCar.getReport());

    criticalFindings |= insCed.hasCriticalFindings(); // CED
    explanation.addAll(insCed.getExplanation());
    report.addAll(insCed.getReport());

    criticalFindings |= insCxd.hasCriticalFindings(); // CXD
    explanation.addAll(insCxd.getExplanation());
    report.addAll(insCxd.getReport());

    // cardholder public key template
    criticalFindings |= checkOidPuk(report, explanation); // OID of public key usage
    criticalFindings |= checkPublicKey(report, explanation); // public key

    // CHAT
    criticalFindings |= checkOidFlagList(report, explanation); // OID of flag list description
    // TODO check Flag-List

    // signature
    // TODO

    // --- cross-checks with attributes of this instance
    // Note: Cross-checks with attributes of other CVC are performed in check()-method.
    // crossCheck OidPuk and domain parameter
    // crossCheck certificate-type, OidPuk, CHR.length, Flag-List
    crossCheckFlaglistChr(report);
    crossCheckFlaglistOidPuk(report);
    crossCheckCedCxd(report);

    insCriticalFindings = criticalFindings;
    insExplanation = Collections.unmodifiableList(explanation);
    insReport = Collections.unmodifiableList(report);
  } // end constructor */

  /**
   * Certificate content is checked.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here.</i>
   *   <li><i>This method does not throw exceptions. In case something odd is detected, a finding is
   *       added to a report.</i>
   *   <li><i>The only critical finding detected in this method is "invalid signature"</i>
   * </ol>
   *
   * @return a list with the following elements:
   *     <ol>
   *       <li>{@link List} of {@link String}s with report
   *       <li>{@link String} with explanation of CV-certificate content
   *       <li>{@link Boolean}: {@code TRUE} if CVC contains critical findings, {@code FALSE}
   *           otherwise
   *     </ol>
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ List<Object> check() {
    final List<String> report = new ArrayList<>(insReport); // list with findings
    final List<String> explanation = new ArrayList<>(insExplanation); // CVC explained
    boolean criticalFindings = insCriticalFindings; // flag indicating critical findings

    criticalFindings |= checkSignature(report, explanation); // signature

    // FIXME cross check this and parent CVC
    // a. time information, i.e. this.CED >= parent.CED
    // b. time information, i.e. this.CXD <= parent.CXD (except for link certificates)
    // c. this.CAR == parent.CHR
    // d. this.sigVerifyKey == parent.publicKey
    // e. this.flagList SHALL have no bits set, which are clear in parent

    return List.of(
        Collections.unmodifiableList(report),
        explanation.stream().collect(Collectors.joining(LINE_SEPARATOR, "", LINE_SEPARATOR)),
        criticalFindings);
  } // end method */

  /**
   * Check and explain OID of the flag list.
   *
   * <p>The content of OID for the flag list is explained.
   *
   * <p>It is a critical finding, if
   *
   * <ol>
   *   <li>Octet string for OID public key usage is an invalid OID.
   *   <li>OID is not from set { {@link AfiOid#CVC_FlagList_CMS CVC_FlagList_CMS}, {@link
   *       AfiOid#CVC_FlagList_TI CVC_FlagList_TI} }
   *   <li>FlagList does not contain 56 bits = 7 octet
   * </ol>
   *
   * @param report report where findings are written to
   * @param explanation CV-certificate explanation
   * @return {@code TRUE} if critical findings occur, {@code FALSE} otherwise
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ boolean checkOidFlagList(
      final List<String> report, final List<String> explanation) {
    explanation.add(
        String.format(
            "Interpretation of Flag-List       OID = %-16s = %s = %s",
            insOidFlagList.getOctetString(), insOidFlagList.getAsn1(), insOidFlagList));
    explanation.add(
        String.format("List of access rights,      Flag-List = %s", Hex.toHexDigits(insFlagList)));

    boolean result =
        !Set.of(AfiOid.CVC_FlagList_CMS, AfiOid.CVC_FlagList_TI).contains(insOidFlagList);

    if (result) {
      // ... invalid OID
      report.add("wrong OID for for Flag-List description");
    } else {
      // ... OID is from the set of expected OID
      if (7 == insFlagList.length) {
        // .. flagList has appropriate length
        explainFlagList(
            AfiOid.CVC_FlagList_TI.equals(insOidFlagList) ? CVC_FLAG_LIST_TI : CVC_FLAG_LIST_CMS,
            report,
            explanation);
      } else {
        // ... flagList has wrong length
        result = true;
        report.add("Flag-List has wrong length");
      } // end fi
    } // end fi

    return result;
  } // end method */

  /**
   * Check and explain OID of public key usage.
   *
   * <p>The content of OID public key usage is explained.
   *
   * <p>It is a critical finding, if
   *
   * <ol>
   *   <li>Octet string for OID public key usage is an invalid OID.
   *   <li>OID is not from set { {@link AfiOid#authS_gemSpec_COS_G2_ecc_with_sha256
   *       authS_gemSpec_COS_G2_ecc_with_sha256}, {@link AfiOid#authS_gemSpec_COS_G2_ecc_with_sha384
   *       authS_gemSpec_COS_G2_ecc_with_sha384}, {@link AfiOid#authS_gemSpec_COS_G2_ecc_with_sha512
   *       authS_gemSpec_COS_G2_ecc_with_sha512}, {@link AfiOid#ECDSA_with_SHA256
   *       ecdsa-with-SHA256}, {@link AfiOid#ECDSA_with_SHA384 ecdsa-with-SHA384}, {@link
   *       AfiOid#ECDSA_with_SHA512 ecdsa-with-SHA512} }
   * </ol>
   *
   * @param report report where findings are written to
   * @param explanation CV-certificate explanation
   * @return {@code TRUE} if critical findings occur, {@code FALSE} otherwise
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ boolean checkOidPuk(final List<String> report, final List<String> explanation) {
    explanation.add(
        String.format(
            "Usage of enclosed public key      OID = %-16s = %s =  %s",
            insOidPuk.getOctetString(), insOidPuk.getAsn1(), insOidPuk));

    final boolean result =
        !Set.of(
                AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256,
                AfiOid.authS_gemSpec_COS_G2_ecc_with_sha384,
                AfiOid.authS_gemSpec_COS_G2_ecc_with_sha512,
                AfiOid.ECDSA_with_SHA256,
                AfiOid.ECDSA_with_SHA384,
                AfiOid.ECDSA_with_SHA512)
            .contains(insOidPuk);

    if (result) {
      // ... invalid OID
      report.add("wrong OID for public key usage");
    } // end fi

    return result;
  } // end method */

  /**
   * Check and explain public key.
   *
   * <p>The content of public key is explained.
   *
   * <p>It is a critical finding, if
   *
   * <ol>
   *   <li>domain parameter of public key do not match with the OID for public key usage
   *   <li>decoding of the point for public key throws an exception
   * </ol>
   *
   * @param report report where findings are written to
   * @param explanation CV-certificate explanation
   * @return {@code TRUE} if critical findings occur, {@code FALSE} otherwise
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ boolean checkPublicKey(final List<String> report, final List<String> explanation) {
    explanation.add("Public point as octet string      P   = " + Hex.toHexDigits(insPuK));

    boolean result = false;
    try {
      final AfiElcParameterSpec domainParameter = getPublicKey().getParams();
      final ECPoint p = getPublicKey().getW();
      // ... point is on elliptic curve

      // --- explain P
      final String pX = Hex.toHexDigits(AfiElcUtils.fe2os(p.getAffineX(), domainParameter));
      final String pY = Hex.toHexDigits(AfiElcUtils.fe2os(p.getAffineY(), domainParameter));

      explanation.add(
          String.format("       Domain parameter of public key = %s", domainParameter.getOid()));
      explanation.add(String.format("       P = (xp, yp) = ('%s', '%s')", pX, pY));

      // --- cross check of domain parameter, CHR.length and OID for public key usage
      // ... OID for public key usage present
      final AfiOid oidPukUsage = insOidPuk;
      final AfiOid oidDomainParameter = domainParameter.getOid();
      final int chrLength = insChr.getValue().length;

      if (8 == chrLength) {
        // ... CHR of a CA, Sub-CA or Root-CA
        //     => expect ecdsa-with-SHA???... OID
        result = zzzCrossCheckOidCvcCa(report, oidDomainParameter, oidPukUsage);
      } else if (12 == chrLength) {
        // ... CHR of an end-entity certificate
        //     => expect authS_gemSpec_COS_G2_ecc_with_sha??? OID
        result = zzzCrossCheckOidCvcEe(report, oidDomainParameter, oidPukUsage);
      } //  end else if
    } catch (Exception e) {
      result = true;
      report.add("exception while decoding P: " + e.getMessage());
    } // end Catch (...)

    return result;
  } // end method */

  private static boolean zzzCrossCheckOidCvcCa(
      final List<String> report, final AfiOid oidDomainParameter, final AfiOid oidPukUsage) {
    boolean result = false;
    if (!((AfiOid.brainpoolP256r1.equals(oidDomainParameter)
            && AfiOid.ECDSA_with_SHA256.equals(oidPukUsage))
        || (AfiOid.brainpoolP384r1.equals(oidDomainParameter)
            && AfiOid.ECDSA_with_SHA384.equals(oidPukUsage))
        || (AfiOid.brainpoolP512r1.equals(oidDomainParameter)
            && AfiOid.ECDSA_with_SHA512.equals(oidPukUsage)))) {
      result = true;
      report.add("mismatch of domain parameter and OID for public key usage");
    } // end fi (invalid combination of domain parameter and public key usage

    return result;
  } // end method */

  private static boolean zzzCrossCheckOidCvcEe(
      final List<String> report, final AfiOid oidDomainParameter, final AfiOid oidPukUsage) {
    boolean result = false;
    if (!((AfiOid.brainpoolP256r1.equals(oidDomainParameter)
            && AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256.equals(oidPukUsage))
        || (AfiOid.brainpoolP384r1.equals(oidDomainParameter)
            && AfiOid.authS_gemSpec_COS_G2_ecc_with_sha384.equals(oidPukUsage))
        || (AfiOid.brainpoolP512r1.equals(oidDomainParameter)
            && AfiOid.authS_gemSpec_COS_G2_ecc_with_sha512.equals(oidPukUsage)))) {
      result = true;
      report.add("mismatch of domain parameter and OID for public key usage");
    } // end fi (invalid combination of domain parameter and public key usage

    return result;
  } // end method */

  /**
   * Check and explain signature.
   *
   * <p>The content of the signature is explained.
   *
   * <p>It is a critical finding, if
   *
   * <ol>
   *   <li>a public key for verifying the signature is in trust center cache AND the signature is
   *       invalid.
   * </ol>
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is TODO thread-safe.</i>
   *   <li><i>Object sharing TODO</i>
   *   <li><i>Intentionally, it is <b>NOT</b> a critical finding if the cache of the trust center
   *       does not contain a public key for signature verification.</i>
   * </ol>
   *
   * @param report report where findings are written to
   * @param explanation CV-certificate explanation
   * @return {@code TRUE} if critical findings occur, {@code FALSE} otherwise
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ boolean checkSignature(final List<String> report, final List<String> explanation) {
    final StringBuilder stringBuilder = new StringBuilder("Signature is ");

    final SignatureStatus signatureStatus = getSignatureStatus();
    final boolean result = SIGNATURE_INVALID == signatureStatus;

    switch (signatureStatus) { // NOPMD no default (that is a false positive)
      case SIGNATURE_INVALID -> {
        report.add("signature is INVALID");
        stringBuilder.append("INVALID   ");
      }

      case SIGNATURE_VALID -> stringBuilder.append("valid     ");

      default -> {
        // ... verification key was not found
        //     => signature could not be checked
        // Note: Intentionally this is not a critical finding.
        report.add("verification key was not found => signature could not be checked");
        stringBuilder.append("not checked  ");
      }
    } // end Switch (signature status)

    final String sigString = Hex.toHexDigits(insSignature);
    final int rLength = insSignature.length;

    // Note 1: The following implementation works also for odd amount of octet in
    //         signature. If so, then R and S have different lengths.
    // Note 2: If R and S have different lengths, then the signature is always
    //         invalid.

    explanation.add(String.format("%ssig = 'R || S' = %s", stringBuilder, sigString));
    explanation.add(String.format("       R = '%s'", sigString.substring(0, rLength)));
    explanation.add(String.format("       S = '%s'", sigString.substring(rLength)));

    return result;
  } // end method */

  /**
   * Checks the tag of the CV-certificate.
   *
   * <p>It is a critical finding, if
   *
   * <ol>
   *   <li>tag differs from {@link #TAG_CARDHOLDER_CERTIFICATE_TEMPLATE}
   * </ol>
   *
   * @param report report where findings are written to
   * @return {@code TRUE} if critical findings occur, {@code FALSE} otherwise
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ boolean checkTag(final List<String> report) {
    final long tag = insCvcContent.getTag();

    final boolean result = TAG_CARDHOLDER_CERTIFICATE_TEMPLATE != tag;

    if (result) {
      // ... wrong tag
      //     => report that
      report.add(
          String.format(
              "expected tag '%x', but present tag is '%x'",
              TAG_CARDHOLDER_CERTIFICATE_TEMPLATE, tag));
    } // end fi

    return result;
  } // end method */

  /**
   * Cross-check for content of CED and CED.
   *
   * <p>CED <b>SHALL</b> be smaller than CXD.
   *
   * @param report report where findings are written to
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void crossCheckCedCxd(final List<String> report) {
    if (insCed.getDate().isAfter(insCxd.getDate())) {
      // ... CED > CXD
      report.add("CED is greater than CXD");
    } // end fi
  } // end method */

  /**
   * Cross-check for content of flag list and length of CHR.
   *
   * <p>Bits b0 contain information about the type of CV-certificate.
   *
   * <ol>
   *   <li>If bit b0 is set, then CHR <b>SHALL</b> contain eight octet.
   *   <li>If bit b0 is not set, then CHR <b>SHALL</b> contain twelve octet.
   * </ol>
   *
   * @param report report where findings are written to
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void crossCheckFlaglistChr(final List<String> report) {
    final int length = insChr.getValue().length;
    if (isEndEntity()) {
      // ... FlagList indicates end entity CV-certificate

      if (12 != length) {
        // ... End-Entity-CVC but length of CHR is not 12
        report.add("FlagList indicates EE-certificate, but CHR has not twelve octet");
      } // end fi
    } else {
      // ... FlagList indicates NOT end entity CV-certificate
      if (8 != length) {
        // ... CVC-Root-CA  OR  CVC-Sub-CA but length of CHR is not 8
        report.add("FlagList indicates CA-certificate, but CHR has not eight octet");
      } // end fi
    } // end else
  } // end method */

  /**
   * Cross-check for content of flag list and OID for public key usage.
   *
   * <p>Bit b0 contains information about the type of CV-certificate.
   *
   * <ol>
   *   <li>If bit b0 is set, then OID for public key usage <b>SHALL NOT</b> indicate authentication
   *       protocol.
   *   <li>If bit b0 is not set, then OID for public key usage <b>SHALL NOT</b> indicate signature
   *       verification.
   * </ol>
   *
   * @param report report where findings are written to
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void crossCheckFlaglistOidPuk(final List<String> report) {
    final String oidPukUsage = insOidPuk.getName();
    final byte msByte = insFlagList[0];

    if ((msByte < 0) // ... bit b0 is set in FlagList => CA-CVC
        && oidPukUsage.startsWith("autS")) {
      // ... CA-CVC but OID indicates authentication
      report.add(
          "FlagList indicates CA-certificate, but public key usage indicates EE-certificate");
    } // end fi

    if ((msByte >= 0) // ... bit b0 is clear in FlagList => end-entity certificate
        && oidPukUsage.startsWith("ecdsa")) {
      // ... End-Entity-CVC but OID indicates signature verification
      report.add(
          "FlagList indicates EE-certificate, but public key usage indicates CA-certificate");
    } // end fi
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * @param obj object used for comparison, can be null
   * @return true if objects are equal, false otherwise
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final @CheckForNull Object obj) {
    // Note 1: Because this class is a direct subclass of Object calling super.equals(...)
    //         would be wrong. Instead, special checks are performed.

    // --- reflexive
    if (this == obj) {
      return true;
    } // end fi
    // ... obj not same as this

    // --- null
    if (null == obj) {
      // ... this differs from null
      return false;
    } // end fi
    // ... obj not null

    // --- check type
    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end fi
    // ... obj is instance of ElcPublicKey

    final Cvc other = (Cvc) obj;

    // --- compare primitive instance attributes (currently none)
    // -/-

    // --- compare reference types
    // FIXME compare by attributes rather than by TLV. Thus, two CVC could be
    // considered equal even if their attributes hava a different sequence.
    // ... assertion: instance attributes are never null
    return this.insCvcContent.equals(other.insCvcContent);
  } // end method */

  /**
   * Explain flag list.
   *
   * <p>This method has no critical findings. Thus, it returns no result.
   *
   * @param flagListExplanation describing the flag list
   * @param report is a list of errors and warnings found during the check
   * @param explanation gives detailed information about the certificate content
   */
  /* package */ void explainFlagList(
      final String[] flagListExplanation,
      final List<String> report,
      final List<String> explanation) {
    final BigInteger flagList = new BigInteger(1, insFlagList);

    if (isRootCa()) {
      // ... link certificate or self-signed certificate of a CVC-Root-CA
      explanation.add("       b0b1:  Link-Zertifikat einer Root-CA");
    } else if (isSubCa()) {
      // ... CVC-Sub-CA certificate
      explanation.add("       b0b1:  Zertifikat einer Sub-CA");
    } else if (isEndEntity()) {
      // ... end entity CV-certificate
      explanation.add("       b0b1:  Endnutzer Zertifikat für Authentisierungszwecke");
    } else {
      // ... none of the above certificate types
      report.add("b0b1 is invalid");
      explanation.add("       b0b1:  1 0, which is invalid");
    } // end else

    if (isRootCa() && (0xffffffffffffffL != flagList.longValue())) { // NOPMD literal in statement
      // ... link-CVC or a self-signed CVC of a CVC-Root-CA  BUT not all flags set
      //     => all flags should be set
      report.add("not all flags are set in the certificate of a CVC-Root-CA");
    } // end fi

    final int offset = 55;
    for (int i = 2; i <= offset; i++) {
      if (flagList.testBit(offset - i)) {
        // ... bit is set
        final String accessRight = flagListExplanation[i];

        if (!isRootCa() && accessRight.endsWith("RFU")) {
          // ... an RFU bit is set in a non-Root-CVC
          report.add(String.format("bit b%02d is set, but RFU", i));
        } // end fi

        explanation.add("       " + accessRight);
      } // end fi
    } // end For (i...)
  } // end method */

  /**
   * Return certification authority reference object, CAR.
   *
   * @return CAR as {@link CertificationAuthorityReference}
   */
  public CertificationAuthorityReference getCarObject() {
    return insCar;
  } // end method */

  /**
   * Return certification authority reference (CAR).
   *
   * @return CAR in hex-digit representation
   */
  public String getCar() {
    return Hex.toHexDigits(getCarObject().getValue());
  } // end method */

  /**
   * Getter.
   *
   * @return {@link AfiOid} of CHAT
   */
  public AfiOid getChatOid() {
    return insOidFlagList;
  } // end method */

  /**
   * Getter.
   *
   * @return flaglist within CHAT
   */
  public byte[] getChatFlaglist() {
    return insFlagList.clone();
  } // end method

  /**
   * Returns cardholder reference (CHR).
   *
   * @return CHR as {@link CardHolderReference}
   */
  public CardHolderReference getChrObject() {
    return insChr;
  } // end method */

  /**
   * Returns cardholder reference (CHR).
   *
   * @return CHR in hex-digit representation
   */
  public String getChr() {
    return Hex.toHexDigits(getChrObject().getValue());
  } // end method */

  /**
   * Getter.
   *
   * @return hexadecimal representation of CED
   */
  public CertificateDate getCed() {
    return insCed;
  } // end method */

  /**
   * Getter.
   *
   * @return hexadecimal representation of CED
   */
  public String getCedHex() {
    return Hex.toHexDigits(getCed().getValue());
  } // end method */

  /**
   * Getter.
   *
   * @return hexadecimal representation of CXD
   */
  public CertificateDate getCxd() {
    return insCxd;
  } // end method */

  /**
   * Getter.
   *
   * @return hexadecimal representation of CXD
   */
  public String getCxdHex() {
    return Hex.toHexDigits(insCxd.getValue());
  } // end method */

  /**
   * Getter.
   *
   * @return certificate content as DER-TLV
   */
  public ConstructedBerTlv getCvc() {
    return insCvcContent;
  } // end method */

  /**
   * Return this CV-certificate explained.
   *
   * @return explanation of CV-certificate content
   */
  public String getExplanation() {
    return (String) check().get(1);
  } // end method */

  /**
   * Returns public key from CV-certificate.
   *
   * @return public key form CV-certificate
   * @throws NoSuchElementException if no value is present
   */
  public EcPublicKeyImpl getPublicKey() {
    if (null == insPublicKey) {
      throw new NoSuchElementException();
    } // end fi

    return insPublicKey; // EI_EXPOSE_REP
  } // end method */

  /**
   * Returns an unmodifiable list with findings.
   *
   * <p>Each item in the list contains a finding which states why this instance of a card-verifiable
   * certificate is not in conformance to gemSpec_PKI.
   *
   * @return unmodifiable {@link List} with findings, possibly empty
   */
  public List<String> getReport() {
    return ((List<?>) check().getFirst())
        .stream().filter(String.class::isInstance).map(i -> (String) i).toList();
  } // end method */

  /**
   * Returns status of signature.
   *
   * @return status of signature, see {@link #insSignatureStatus}
   */
  public SignatureStatus getSignatureStatus() {
    // Note 1: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract), it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    // read attribute from the main memory into thread local memory
    // Note 2: SonarQube claims a major fining here:
    //         "Merge the previous cases into this one using comma-separated label."
    // Note 3: Here it seems better to keep separated case-statements.
    SignatureStatus signatureStatus = insSignatureStatus;
    switch (signatureStatus) {
      case SIGNATURE_INVALID:
      case SIGNATURE_VALID:
        // ... result will never change
        return signatureStatus;

      default:
        // ... signature status may change if in the meantime additional
        //     keys have been added to the cache of the trust center
        //     => check signature

        try {
          final EcPublicKeyImpl caPuK = TrustCenter.getPublicKey(getCar());
          final boolean valid = caPuK.verifyEcdsa(insMessage.getEncoded(), insSignature);

          signatureStatus = valid ? SIGNATURE_VALID : SIGNATURE_INVALID;
        } catch (NoSuchElementException e) {
          // ... verification key was not found
          //     => signature could not be checked
          // Note: Intentionally this is not a critical finding.
          signatureStatus = SIGNATURE_NO_PUBLIC_KEY;
        } // end Catch (...)

        insSignatureStatus = signatureStatus; // store attribute into thread local memory

        return signatureStatus; // end default
    } // end Switch (signature status)
  } // end method */

  /**
   * Returns the value-field of the CV-certificate as an octet string.
   *
   * <p>The command data field of a PSO Verify Certificate command contains the value-field of a
   * CV-certificate.
   *
   * @return octet string representation of the value-field
   */
  public byte[] getValueField() {
    // Note 1: Because this is an immutable value type, it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    byte[] result = insValueField; // read attribute from the main memory into thread local memory
    if (0 == result.length) {
      // ... obviously instance attribute has never been calculated (lazy initialization)
      //     => do so now
      result = insCvcContent.getValueField();
      insValueField = result; // store attribute into thread local memory
    } // end fi

    return result.clone();
  } // end method */

  /**
   * Checks if the content of this CV-certificate has critical findings.
   *
   * @return {@code TRUE} if {@link #getReport()} contains critical findings, {@code FALSE}
   *     otherwise
   */
  public boolean hasCriticalFindings() {
    return (Boolean) check().get(2);
  } // end method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * @return hash-code of object
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    // Note 1: Because this class is a direct subclass of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from main memory into thread local memory
    if (0 == result) {
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      // Note 3: All attributes are derived from CVC content. Thus, it is
      //         sufficient to calculate the hashCode over that attribute.

      // --- take into account primitive instance attributes (currently none)
      // -/-

      // --- take into account reference types (currently only CVC content)
      result = insCvcContent.hashCode();

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Check if this is an end entity CV-certificate.
   *
   * <p>A CV-certificate is considered to be end entity, if the two most significant bits in the
   * flag list equals b0b1 = '00'.
   *
   * @return true if flag list indicates CV-certificate of end entity.
   */
  public boolean isEndEntity() {
    return 0x00 == (insFlagList[0] & 0xc0);
  } // end method */

  /**
   * Check if this is a CV-certificate of a CVC-Root-CA.
   *
   * <p>A CV-certificate is considered to be CVC-Root-CA, if the two most significant bits in the
   * flag list equals b0b1 = '11'.
   *
   * @return true if flag list indicates CVC-Root-CA
   */
  public boolean isRootCa() {
    return 0xc0 == (insFlagList[0] & 0xc0);
  } // end method */

  /**
   * Check if this is a CV-certificate of a CVC-Sub-CA.
   *
   * <p>A CV-certificate is considered to be CVC-Sub-CA, if the two most significant bits in the
   * flag list equals b0b1 = '10'.
   *
   * @return true if flag list indicates CVC-Sub-CA, i.e. bits b0b1 = '10'
   */
  public boolean isSubCa() {
    return 0x80 == (insFlagList[0] & 0xc0);
  } // end method */

  /**
   * Returns a string representation of the object.
   *
   * @return string representation of the object
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder(0x1_0000); // 64 kibi Byte buffer
    final String line =
        String.format(
            "--------------------------------------------------------------------------------%n");

    result
        .append(String.format("CVC-Content '%s'%n", insCvcContent))
        .append(String.format("CVC.DER:%n%s%n", getCvc().toString(" ", "|    ")))
        .append(line);

    // --- get report, explanation and error situation
    final List<Object> checkResult = check();
    final List<String> report =
        ((List<?>) checkResult.getFirst())
            .stream().filter(String.class::isInstance).map(i -> (String) i).toList();
    final String explanation = (String) checkResult.get(1);
    final boolean hasCriticalFindings = (Boolean) checkResult.get(2);

    if (hasCriticalFindings) {
      // ... CVC contains critical findings
      result.append(String.format("ERRORS were found in the CV-Certificate:%n"));
    } else if (report.isEmpty()) {
      // ... nothing to report
      result.append(String.format("CV-Certificate seems to be in conformance to [gemSpec_PKI]%n"));
    } else {
      // ... CVC contains minor issues
      result
          .append(
              String.format(
                  "WARNING: The CV-Certificate may not be fully in conformance to"
                      + " [gemSpec_PKI].%n"))
          .append(
              String.format(
                  "         Still a Smart Card might accept this CV-Certificate. Good Luck!%n"));
    } // end fi

    int counter = 0;
    for (final String i : report) {
      result.append(String.format("%5d. %s%n", ++counter, i));
    } // end For (i...)

    result
        .append(line)

        // --- attributes as is
        .append(String.format("Certificate content as is:%n"))
        .append(
            String.format(
                "Certificate Profile Indicator     CPI = %s%n", Hex.toHexDigits(insCpi.getValue())))
        .append(String.format("Certification Authority Reference CAR = %s%n", getCar()))
        .append(
            String.format(
                "Usage of enclosed public key      OID = %s%n", insOidPuk.getOctetString()))
        .append(
            String.format("Public point as octet string      P   = %s%n", Hex.toHexDigits(insPuK)))
        .append(String.format("Certificate Holder Reference      CHR = %s%n", getChr()))
        .append(
            String.format(
                "Interpretation of Flag-List       OID = %s%n", insOidFlagList.getOctetString()))
        .append(
            String.format(
                "List of access rights,      Flag-List = %s%n", Hex.toHexDigits(insFlagList)))
        .append(String.format("Certificate Effective  Date       CED = %s%n", getCedHex()))
        .append(String.format("Certificate Expiration Date       CXD = %s%n", getCxdHex()))
        .append(
            String.format(
                "Signature as provided by CA       sig = %s%n", Hex.toHexDigits(insSignature)))
        .append(line)

        // --- attributes with explanation
        .append(String.format("Explanation of certificate content:%n%s", explanation));

    return result.toString();
  } // end method */

  /**
   * Enumeration for status of signature verification in a {@link Cvc}.
   *
   * @author <a href="mailto:software-development@gematik.de">gematik</a>
   */
  public enum SignatureStatus {
    /** Status of signature in CVC unknown. */
    SIGNATURE_UNKNOWN,

    /** Signature checked and signature is valid. */
    SIGNATURE_VALID,

    /** Signature checked and signature is invalid. */
    SIGNATURE_INVALID,

    /** Signature isn't checked, because signature-verification-key is unknown. */
    SIGNATURE_NO_PUBLIC_KEY,
  } // end enum
} // end class
