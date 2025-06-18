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
package de.gematik.smartcards.utils;

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Collection of popular OID.
 *
 * <p>According to <a href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a>
 * clause 8.19 an object identifier, is a list of object identifier components. This implementation
 * supports the following representations for object identifier components:
 *
 * <ol>
 *   <li>Each object identifier component is an unsigned integer.
 *   <li>Object identifier components are encoded in subidentifier. Each subidentifier is an
 *       unsigned binary number of (theoretically) unlimited length. Citation from clause 8.19.2:
 *       <br>
 *       <i>Each subidentifier is represented as a series of (one or more) octets. Bit 8 of each
 *       octet indicates whether it is the last in the series: bit 8 of the last octet is zero; bit
 *       8 of each preceding octet is one. Bits 7 to 1 of the octets in the series collectively
 *       encode the subidentifier. Conceptually, these groups of bits are concatenated to form an
 *       unsigned binary number whose most significant bit is bit 7 of the first octet and whose
 *       least significant bit is bit 1 of the last octet. The subidentifier shall be encoded in the
 *       fewest possible octets, that is, the leading octet of the subidentifier shall not have the
 *       value {@code 128 = 0x80 = ´80´}.</i>
 * </ol>
 *
 * <p>The current implementation uses the Java primitive {@code int} for the internal representation
 * of object identifier components. It follows that values for object identifier components are
 * limited to the range [0, {@link Integer#MAX_VALUE}]. Considering object identifiers from real
 * world applications, this seems not to be too restrictive.
 *
 * <p>This class provides static methods for conversion between the following representations of an
 * object identifier:
 *
 * <ol>
 *   <li>ANS.1 notation of OID, e.g. "{1 2 840 10045 3 1 7}"
 *   <li>Octet string representation of OID, e.g. "2b240305030107"
 *   <li>Point-notation of OID, e.g. "1.2.840.10045.3.1.7".
 * </ol>
 *
 * <p>Instances of this class are immutable.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES"
//         Short message: It is a good practice to avoid reusing public identifiers
//             from the Java Standard Library as field names in your code.
//         Explanation: That finding is suppressed because the name is chosen
//             intentionally.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES" // see note 1
}) // */
@SuppressWarnings({"PMD.FieldNamingConventions", "PMD.LongVariable", "PMD.TooManyMethods"})
public final class AfiOid implements Serializable {

  /** Serial number randomly generated on 2022-02-17 10:08. */
  @Serial private static final long serialVersionUID = 8234458620732855005L; // */

  /** Constant indicating invalid encoding. */
  public static final AfiOid INVALID = new AfiOid("INVALID", 0, 0); // */

  /**
   * Variant of PACE protocol.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/0.4.0.127.0.7.2.2.4.2.2">0.4.0.127.0.7.2.2.4.2.2</a> and <a
   * href="https://www.bsi.bund.de/EN/Publications/TechnicalGuidelines/TR03110/BSITR03110.html">BSI-TR-03110
   * part 3</a>.
   */
  public static final AfiOid id_PACE_ECDH_GM_AES_CBC_CMAC_128 =
      new AfiOid("id-PACE-ECDH-GM-AES-CBC-CMAC-128", 0, 4, 0, 127, 0, 7, 2, 2, 4, 2, 2); // */

  /**
   * Variant of PACE protocol.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/0.4.0.127.0.7.2.2.4.2.3">0.4.0.127.0.7.2.2.4.2.3</a> and <a
   * href="https://www.bsi.bund.de/EN/Publications/TechnicalGuidelines/TR03110/BSITR03110.html">BSI-TR-03110
   * part 3</a>.
   */
  public static final AfiOid id_PACE_ECDH_GM_AES_CBC_CMAC_192 =
      new AfiOid("id-PACE-ECDH-GM-AES-CBC-CMAC-192", 0, 4, 0, 127, 0, 7, 2, 2, 4, 2, 3); // */

  /**
   * Variant of PACE protocol.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/0.4.0.127.0.7.2.2.4.2.4">0.4.0.127.0.7.2.2.4.2.4</a> and <a
   * href="https://www.bsi.bund.de/EN/Publications/TechnicalGuidelines/TR03110/BSITR03110.html">BSI-TR-03110
   * part 3</a>.
   */
  public static final AfiOid id_PACE_ECDH_GM_AES_CBC_CMAC_256 =
      new AfiOid("id-PACE-ECDH-GM-AES-CBC-CMAC-256", 0, 4, 0, 127, 0, 7, 2, 2, 4, 2, 4); // */

  /**
   * Professional category "physician".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.30">1.2.276.0.76.4.30</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.30">dimdi</a>.
   */
  public static final AfiOid arzt_ghcs = new AfiOid("arzt_ghcs", 1, 2, 276, 0, 76, 4, 30); // */

  /**
   * Institution type "medical practice".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.50">1.2.276.0.76.4.50</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.50">dimdi</a>.
   */
  public static final AfiOid medical_practice =
      new AfiOid("medical-practice", 1, 2, 276, 0, 76, 4, 50); // */

  /**
   * Institution type "dental practice".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.51">1.2.276.0.76.4.51</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.51">dimdi</a>.
   */
  public static final AfiOid dental_practice =
      new AfiOid("dental-practice", 1, 2, 276, 0, 76, 4, 51); // */

  /**
   * Institution type "practice of a psychological psychotherapist".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.52">1.2.276.0.76.4.52</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.52">dimdi</a>.
   */
  public static final AfiOid psychotherapist_practice =
      new AfiOid("medicalpsychotherapist-practice", 1, 2, 276, 0, 76, 4, 52); // */

  /**
   * Institution type "hospital".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.53">1.2.276.0.76.4.53</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.53">dimdi</a>.
   */
  public static final AfiOid krankenhaus_ghcs =
      new AfiOid("krankenhaus_ghcs", 1, 2, 276, 0, 76, 4, 53); // */

  /**
   * Institution type "public pharmacy".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.54">1.2.276.0.76.4.54</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.54">dimdi</a>.
   */
  public static final AfiOid oeffentliche_apotheke_ghcs =
      new AfiOid("oeffentliche-apotheke-ghcs", 1, 2, 276, 0, 76, 4, 54); // */

  /**
   * Institution type "hospital pharmacy".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.55">1.2.276.0.76.4.55</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.55">dimdi</a>.
   */
  public static final AfiOid krankenhausapotheke_ghcs =
      new AfiOid("krankenhausapotheke-ghcs", 1, 2, 276, 0, 76, 4, 55); // */

  /**
   * Institution type "army pharmacy".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.56">1.2.276.0.76.4.56</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.56">dimdi</a>.
   */
  public static final AfiOid bundeswehrapotheke_ghcs =
      new AfiOid("bundeswehrapotheke-ghcs", 1, 2, 276, 0, 76, 4, 56); // */

  /**
   * Institution type "mobile unit Emergency Medical Service".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.57">1.2.276.0.76.4.57</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.57">dimdi</a>.
   */
  public static final AfiOid mobile_einrichtung_rettungsdienst_ghcs =
      new AfiOid("mobile-einrichtung-rettungsdienst-ghcs", 1, 2, 276, 0, 76, 4, 57); // */

  /**
   * Institution type "operating side of gematik".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.58">1.2.276.0.76.4.58</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.58">dimdi</a>.
   */
  public static final AfiOid bs_gematik_ghcs =
      new AfiOid("bs-gematik-ghcs", 1, 2, 276, 0, 76, 4, 58); // */

  /**
   * Institution type "health insurance company".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.59">1.2.276.0.76.4.59</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.59">dimdi</a>.
   */
  public static final AfiOid kostentraeger_ghcs =
      new AfiOid("kostentraeger-ghcs", 1, 2, 276, 0, 76, 4, 59); // */

  /**
   * Certificate of the qualified electronic signature for the electronic Health Card used in the
   * German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.66">1.2.276.0.76.4.66</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.66">dimdi</a>.
   */
  public static final AfiOid egk_qes = new AfiOid("egk-qes", 1, 2, 276, 0, 76, 4, 66); // */

  /**
   * Certificate of the advanced electronic signature for the electronic Health Card used in the
   * German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.67">1.2.276.0.76.4.67</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.67">dimdi</a>.
   */
  public static final AfiOid egk_sig = new AfiOid("egk-sig", 1, 2, 276, 0, 76, 4, 67); // */

  /**
   * Certificate for encryption for the electronic Health Card used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.68">1.2.276.0.76.4.68</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.68">dimdi</a>.
   */
  public static final AfiOid egk_enc = new AfiOid("egk-enc", 1, 2, 276, 0, 76, 4, 68); // */

  /**
   * Certificate for encryption of e-prescriptions for the electronic Health Card used in the German
   * eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.69">1.2.276.0.76.4.69</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.69">dimdi</a>.
   */
  public static final AfiOid egk_encv = new AfiOid("egk-encv", 1, 2, 276, 0, 76, 4, 69); // */

  /**
   * Certificate for authentication for the electronic Health Card used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.70">1.2.276.0.76.4.70</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.70">dimdi</a>.
   */
  public static final AfiOid egk_aut = new AfiOid("egk-aut", 1, 2, 276, 0, 76, 4, 70); // */

  /**
   * Certificate for message signature for the electronic Health Card used in the German
   * eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.71">1.2.276.0.76.4.71</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.71">dimdi</a>.
   */
  public static final AfiOid egk_autn = new AfiOid("egk-autn", 1, 2, 276, 0, 76, 4, 71); // */

  /**
   * Certificate of the qualified electronic signature for the HPC used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.72">1.2.276.0.76.4.72</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.72">dimdi</a>.
   */
  public static final AfiOid hba_qes = new AfiOid("hba-qes", 1, 2, 276, 0, 76, 4, 72); // */

  /**
   * Certificate of the advanced electronic signature for the HPC used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.73">1.2.276.0.76.4.73</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.73">dimdi</a>.
   */
  public static final AfiOid hba_sig = new AfiOid("hba-sig", 1, 2, 276, 0, 76, 4, 73); // */

  /**
   * Certificate for encryption for the HPC used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.74">1.2.276.0.76.4.74</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.74">dimdi</a>.
   */
  public static final AfiOid hba_enc = new AfiOid("hba-enc", 1, 2, 276, 0, 76, 4, 74); // */

  /**
   * Certificate for authentication for the HPC used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.75">1.2.276.0.76.4.75</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.75">dimdi</a>.
   */
  public static final AfiOid hba_aut = new AfiOid("hba-aut", 1, 2, 276, 0, 76, 4, 75); // */

  /**
   * Certificate for encryption for the SMC-B used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.76">1.2.276.0.76.4.76</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.76">dimdi</a>.
   */
  public static final AfiOid smc_b_enc = new AfiOid("smc-b-enc", 1, 2, 276, 0, 76, 4, 76); // */

  /**
   * Certificate for authentication for the SMC-B used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.77">1.2.276.0.76.4.77</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.77">dimdi</a>.
   */
  public static final AfiOid smc_b_aut = new AfiOid("smc-b-aut", 1, 2, 276, 0, 76, 4, 77); // */

  /**
   * Certificate of the organizational electronic signature for the Security Module Card type B.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.78">1.2.276.0.76.4.78</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.78">dimdi</a>.
   */
  public static final AfiOid smc_b_osig = new AfiOid("smc-b-osig", 1, 2, 276, 0, 76, 4, 78); // */

  /**
   * Certificate for authentication for the application connector used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.79">1.2.276.0.76.4.79</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.79">dimdi</a>.
   */
  public static final AfiOid ak_aut = new AfiOid("ak-aut", 1, 2, 276, 0, 76, 4, 79); // */

  /**
   * Certificate for authentication for the network connector used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.80">1.2.276.0.76.4.80</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.80">dimdi</a>.
   */
  public static final AfiOid nk_vpn = new AfiOid("nk-vpn", 1, 2, 276, 0, 76, 4, 80); // */

  /**
   * Certificate for authentication for the VPN gateway used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.81">1.2.276.0.76.4.81</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.81">dimdi</a>.
   */
  public static final AfiOid vpnk_vpn = new AfiOid("vpnk-vpn", 1, 2, 276, 0, 76, 4, 81); // */

  /**
   * Certificate for authentication for the card terminal used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.82">1.2.276.0.76.4.82</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.82">dimdi</a>.
   */
  public static final AfiOid smkt_aut = new AfiOid("smkt-aut", 1, 2, 276, 0, 76, 4, 82); // */

  /**
   * Certificate for SSL-authentication for servers and services used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.83">1.2.276.0.76.4.83</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.83">dimdi</a>.
   */
  public static final AfiOid sf_ssl = new AfiOid("sf-ssl", 1, 2, 276, 0, 76, 4, 83); // */

  /**
   * Certificate for SSL-authentication as a client for servers and services used in the German
   * eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.84">1.2.276.0.76.4.84</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.84">dimdi</a>.
   */
  public static final AfiOid sf_ssl_c = new AfiOid("sf-ssl-c", 1, 2, 276, 0, 76, 4, 84); // */

  /**
   * Certificate for SSL-authentication as a server for servers and services used in the German
   * eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.85">1.2.276.0.76.4.85</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.85">dimdi</a>.
   */
  public static final AfiOid sf_ssl_s = new AfiOid("sf-ssl-s", 1, 2, 276, 0, 76, 4, 85); // */

  /**
   * Certificate for authentication for servers and services used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.86">1.2.276.0.76.4.86</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.86">dimdi</a>.
   */
  public static final AfiOid sf_aut = new AfiOid("af-aut", 1, 2, 276, 0, 76, 4, 86); // */

  /**
   * Certificate for authentication for servers and services used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.87">1.2.276.0.76.4.87</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.87">dimdi</a>.
   */
  public static final AfiOid sf_enc = new AfiOid("af-enc", 1, 2, 276, 0, 76, 4, 87); // */

  /**
   * Service-type broker used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.88">1.2.276.0.76.4.88</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.88">dimdi</a>.
   */
  public static final AfiOid broker_ghcs = new AfiOid("broker-ghcs", 1, 2, 276, 0, 76, 4, 88); // */

  /**
   * Service-type broker anonymizing service used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.89">1.2.276.0.76.4.89</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.89">dimdi</a>.
   */
  public static final AfiOid broker_anonym_ghcs =
      new AfiOid("broker-anonym-ghcs", 1, 2, 276, 0, 76, 4, 89); // */

  /**
   * Service-type VPN-gateway used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.90">1.2.276.0.76.4.90</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.90">dimdi</a>.
   */
  public static final AfiOid vpnk_ghcs = new AfiOid("vpnk-ghcs", 1, 2, 276, 0, 76, 4, 90); // */

  /**
   * Service-type VPN-gateway for the telematic infrastructure used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.91">1.2.276.0.76.4.91</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.91">dimdi</a>.
   */
  public static final AfiOid vpnk_ti_ghcs =
      new AfiOid("vpnk-ti-ghcs", 1, 2, 276, 0, 76, 4, 91); // */

  /**
   * Service-type VPN-gateway for value-added services used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.92">1.2.276.0.76.4.92</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.92">dimdi</a>.
   */
  public static final AfiOid vpnk_mwdn_ghcs =
      new AfiOid("vpnk-mwdn-ghcs", 1, 2, 276, 0, 76, 4, 92); // */

  /**
   * Service-type trusted service used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.93">1.2.276.0.76.4.93</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.93">dimdi</a>.
   */
  public static final AfiOid trusted_ghcs =
      new AfiOid("trusted-ghcs", 1, 2, 276, 0, 76, 4, 93); // */

  /**
   * Service-type audit service used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.94">1.2.276.0.76.4.94</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.94">dimdi</a>.
   */
  public static final AfiOid audit_ghcs = new AfiOid("audit-ghcs", 1, 2, 276, 0, 76, 4, 94); // */

  /**
   * Service-type service Directory Service used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.95">1.2.276.0.76.4.95</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.95">dimdi</a>.
   */
  public static final AfiOid sds_ghcs = new AfiOid("sds-ghcs", 1, 2, 276, 0, 76, 4, 95); // */

  /**
   * Service-type service for e-prescriptions used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.96">1.2.276.0.76.4.96</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.96">dimdi</a>.
   */
  public static final AfiOid vodd_ghcs = new AfiOid("vodd-ghcs", 1, 2, 276, 0, 76, 4, 96); // */

  /**
   * Service-type service for personal data of the insured persons used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.97">1.2.276.0.76.4.97</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.97">dimdi</a>.
   */
  public static final AfiOid vsdd_ghcs = new AfiOid("vsdd-ghcs", 1, 2, 276, 0, 76, 4, 97); // */

  /**
   * Service-type card management system used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.100">1.2.276.0.76.4.100</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.100">dimdi</a>.
   */
  public static final AfiOid cms_ghcs = new AfiOid("cms-ghcs", 1, 2, 276, 0, 76, 4, 100); // */

  /**
   * Service-type update flag service used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.101">1.2.276.0.76.4.101</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.101">dimdi</a>.
   */
  public static final AfiOid ufs_ghcs = new AfiOid("ufs-ghcs", 1, 2, 276, 0, 76, 4, 101); // */

  /**
   * Component-type application connector used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.103">1.2.276.0.76.4.103</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.103">dimdi</a>.
   */
  public static final AfiOid ak_ghcs = new AfiOid("ak-ghcs", 1, 2, 276, 0, 76, 4, 103); // */

  /**
   * Component-type network connector used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.104">1.2.276.0.76.4.104</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.104">dimdi</a>.
   */
  public static final AfiOid nk_ghcs = new AfiOid("nk-ghcs", 1, 2, 276, 0, 76, 4, 104); // */

  /**
   * Component-type card terminal used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.105">1.2.276.0.76.4.105</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.105">dimdi</a>.
   */
  public static final AfiOid kt_ghcs = new AfiOid("kt-ghcs", 1, 2, 276, 0, 76, 4, 105); // */

  /**
   * Certificate for authentication of the signature application component
   * (Signaturanwendungskomponente SAK) used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.113">1.2.276.0.76.4.113</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.113">dimdi</a>.
   */
  public static final AfiOid sak_aut = new AfiOid("sak-aut", 1, 2, 276, 0, 76, 4, 113); // */

  /**
   * Component-type signature application component (Signaturanwendungskomponente SAK) used in the
   * German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.119">1.2.276.0.76.4.119</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.119">dimdi</a>.
   */
  public static final AfiOid sak_ghcs = new AfiOid("sak-ghcs", 1, 2, 276, 0, 76, 4, 119); // */

  /**
   * Version 1.0.5 of the document "Gemeinsame Policy für die Ausgabe der HPC" for the QES, SIG, AUT
   * and ENC certificates of the Health Professional Card used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.145">1.2.276.0.76.4.145</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.145">dimdi</a>.
   */
  public static final AfiOid policy_hba_010005_cp =
      new AfiOid("policy-hba-010005-cp", 1, 2, 276, 0, 76, 4, 145); // */

  /**
   * Indication on how to interpret a flag-list within a CV-certificate, here:
   * Telematik-Infrastruktur.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.152">1.2.276.0.76.4.152</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.152">dimdi</a>.
   */
  public static final AfiOid CVC_FlagList_TI =
      new AfiOid("cvc_FlagList_TI", 1, 2, 276, 0, 76, 4, 152); // */

  /**
   * Indication on how to interpret a flag-list within a CV-certificate, here: Card Management
   * System.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.153">1.2.276.0.76.4.153</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.153">dimdi</a>.
   */
  public static final AfiOid CVC_FlagList_CMS =
      new AfiOid("cvc_FlagList_CMS", 1, 2, 276, 0, 76, 4, 153); // */

  /**
   * Service-type intermediary VSDM used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.159">1.2.276.0.76.4.159</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.159">dimdi</a>.
   */
  public static final AfiOid int_vsdm_ghcs =
      new AfiOid("int-vsdm-ghcs", 1, 2, 276, 0, 76, 4, 159); // */

  /**
   * Service-type configuration service used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.160">1.2.276.0.76.4.160</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.160">dimdi</a>.
   */
  public static final AfiOid konfigdienst_ghcs =
      new AfiOid("konfigdienst-ghcs", 1, 2, 276, 0, 76, 4, 160); // */

  /**
   * Service-type vpn-access-service for the telematic infrastructure used in the German
   * eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.161">1.2.276.0.76.4.161</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.161">dimdi</a>.
   */
  public static final AfiOid vpnz_ti_ghcs =
      new AfiOid("vpnz-ti-ghcs", 1, 2, 276, 0, 76, 4, 161); // */

  /**
   * Service-type tsl-signature used in the German eHealth-Card system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.162">1.2.276.0.76.4.162</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.162">dimdi</a>.
   */
  public static final AfiOid tsl_ghcs = new AfiOid("tls-ghcs", 1, 2, 276, 0, 76, 4, 162); // */

  /**
   * Version 1.2.0 of the document "Certificate Policy - Gemeinsame Zertifizierungs-Richtlinie für
   * Teilnehmer der gematik-TSL" for the X.509-nonQES certificates of the electronic Health card,
   * "SMC-B" and technical components used in the German eHealth-Card system, including
   * test-certificates.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.163">1.2.276.0.76.4.163</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.163">dimdi</a>.
   */
  public static final AfiOid policy_gem_010200_or_cp =
      new AfiOid("policy-gem-010200-or-cp", 1, 2, 276, 0, 76, 4, 163); // */

  /**
   * Service-type vpn-access-service for secure internet service used in the German eHealth-Card
   * system.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.166">1.2.276.0.76.4.166</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.166">dimdi</a>.
   */
  public static final AfiOid vpnz_sis_ghcs =
      new AfiOid("vpnz-sis-ghcs", 1, 2, 276, 0, 76, 4, 166); // */

  /**
   * Service-type provider registry (Verzeichnisdienst TI) in the German healthcare telematics
   * infrastructure.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.171">1.2.276.0.76.4.171</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.171">dimdi</a>.
   */
  public static final AfiOid vzd_ti = new AfiOid("vzd-ti", 1, 2, 276, 0, 76, 4, 171); // */

  /**
   * Service-type KOM-LE (healthcare provider communication) used in the German healthcare
   * telematics infrastructure.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.172">1.2.276.0.76.4.172</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.172">dimdi</a>.
   */
  public static final AfiOid komle = new AfiOid("komle", 1, 2, 276, 0, 76, 4, 172); // */

  /**
   * KOM-LE SMIME attribute "recipient-emails" to relate recipient Email-adresses to encryption
   * certificates.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.173">1.2.276.0.76.4.173</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.173">dimdi</a>.
   */
  public static final AfiOid komle_recipient_emails =
      new AfiOid("komle-recipient-emails", 1, 2, 276, 0, 76, 4, 173); // */

  /**
   * Technical role "client module" in the German healthcare telematics infrastructure.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.174">1.2.276.0.76.4.174</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.174">dimdi</a>.
   */
  public static final AfiOid cmfd = new AfiOid("cmfd", 1, 2, 276, 0, 76, 4, 174); // */

  /**
   * Version 1.3.0 of the document "Spezifikation TSL-Dienst", specifying the policy requirements
   * for issuing TSL-signer certificates.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.176">1.2.276.0.76.4.176</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.176">dimdi</a>.
   */
  public static final AfiOid policy_gem_tsl_signer =
      new AfiOid("policy-gem-tsl-signer", 1, 2, 276, 0, 76, 4, 176); // */

  /**
   * Business premises of an association of statutory health insurance dentists.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.187">1.2.276.0.76.4.187</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.187">dimdi</a>.
   */
  public static final AfiOid leo_zahnaerzte =
      new AfiOid("leo-zahnaerzte", 1, 2, 276, 0, 76, 4, 187); // */

  /**
   * AdV environment on the authority of health care insurance providers used in the German
   * healthcare telematics infrastructure.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.190">1.2.276.0.76.4.190</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.190">dimdi</a>.
   */
  public static final AfiOid adv_ktr = new AfiOid("adv-ktr", 1, 2, 276, 0, 76, 4, 190); // */

  /**
   * Business premises of an association of statutory health insurance physicians.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.210">1.2.276.0.76.4.210</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.210">dimdi</a>.
   */
  public static final AfiOid leo_kassenaerztliche_vereinigung =
      new AfiOid("leo-kassenaerztliche-vereinigung", 1, 2, 276, 0, 76, 4, 210); // */

  /**
   * Business premises of the National Association of Statutory Health Insurance Funds.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.223">1.2.276.0.76.4.223</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.223">dimdi</a>.
   */
  public static final AfiOid bs_gkv_spitzenverband =
      new AfiOid("bs-gkv-spitzenverband", 1, 2, 276, 0, 76, 4, 223); // */

  /**
   * Business premises of a State Pharmacists' Association (LAV).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.224">1.2.276.0.76.4.224</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.224">dimdi</a>.
   */
  public static final AfiOid leo_apothekerverband =
      new AfiOid("leo-apothekerverband", 1, 2, 276, 0, 76, 4, 224); // */

  /**
   * Business premises of the German Pharmacists Association (DAV).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.225">1.2.276.0.76.4.225</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.225">dimdi</a>.
   */
  public static final AfiOid leo_dav = new AfiOid("leo-dav", 1, 2, 276, 0, 76, 4, 225); // */

  /**
   * Business premises of a hospital association.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.226">1.2.276.0.76.4.226</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.226">dimdi</a>.
   */
  public static final AfiOid leo_krankenhausverband =
      new AfiOid("leo-krankenhausverband", 1, 2, 276, 0, 76, 4, 226); // */

  /**
   * Business premises of the German Hospital TrustCentre and Informationprocessing.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.227">1.2.276.0.76.4.227</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.227">dimdi</a>.
   */
  public static final AfiOid leo_dktig = new AfiOid("leo-dktik", 1, 2, 276, 0, 76, 4, 227); // */

  /**
   * Business premises of the German Hospital Federation.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.228">1.2.276.0.76.4.228</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.228">dimdi</a>.
   */
  public static final AfiOid leo_dkg = new AfiOid("leo-dkg", 1, 2, 276, 0, 76, 4, 228); // */

  /**
   * Institution type "Business premises of the German Medical Association" of the "SMC-B".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.229">1.2.276.0.76.4.229</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.229">dimdi</a>.
   */
  public static final AfiOid leo_baek = new AfiOid("leo-baek", 1, 2, 276, 0, 76, 4, 229); // */

  /**
   * Institution type "Business premises of a chamber of physicians" of the "SMC-B".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.230">1.2.276.0.76.4.230</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.230">dimdi</a>.
   */
  public static final AfiOid leo_aerztekammer =
      new AfiOid("leo-aerztekammer", 1, 2, 276, 0, 76, 4, 230); // */

  /**
   * Institution type "Business premises of a chamber of dentists" of "SMC-B".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.231">1.2.276.0.76.4.231</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.231">dimdi</a>.
   */
  public static final AfiOid leo_zahnaerztekammer =
      new AfiOid("leo-zahnaerztekammer", 1, 2, 276, 0, 76, 4, 231); // */

  /**
   * Business premises of the national association of statutory health insurance physicians.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.242">1.2.276.0.76.4.242</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.242">dimdi</a>.
   */
  public static final AfiOid leo_kbv = new AfiOid("leo-kbv", 1, 2, 276, 0, 76, 4, 242); // */

  /**
   * Business premises of the German dental association.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.243">1.2.276.0.76.4.243</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.243">dimdi</a>.
   */
  public static final AfiOid leo_bzaek = new AfiOid("leo-bzaek", 1, 2, 276, 0, 76, 4, 243); // */

  /**
   * Business premises of the National Association of Statutory Health Insurance Dentists.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.244">1.2.276.0.76.4.244</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.244">dimdi</a>.
   */
  public static final AfiOid leo_kzbv = new AfiOid("leo-kzbv", 1, 2, 276, 0, 76, 4, 244); // */

  /**
   * Nursing and elderly care facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.245">1.2.276.0.76.4.245</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.245">dimdi</a>.
   */
  public static final AfiOid institution_pflege =
      new AfiOid("institution-pflege", 1, 2, 276, 0, 76, 4, 245); // */

  /**
   * Obstetrics facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.246">1.2.276.0.76.4.246</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.246">dimdi</a>.
   */
  public static final AfiOid institution_geburtshilfe =
      new AfiOid("institution-geburtshilfe", 1, 2, 276, 0, 76, 4, 246); // */

  /**
   * Physical therapy facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.247">1.2.276.0.76.4.247</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.247">dimdi</a>.
   */
  public static final AfiOid praxis_physiotherapeut =
      new AfiOid("praxis-physiotherapeut", 1, 2, 276, 0, 76, 4, 247); // */

  /**
   * Optician facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.248">1.2.276.0.76.4.248</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.248">dimdi</a>.
   */
  public static final AfiOid institution_augenoptiker =
      new AfiOid("institution-augenoptiker", 1, 2, 276, 0, 76, 4, 248); // */

  /**
   * Hearing aid acoustician facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.249">1.2.276.0.76.4.249</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.249">dimdi</a>.
   */
  public static final AfiOid institution_hoerakustiker =
      new AfiOid("institution-hoerakustiker", 1, 2, 276, 0, 76, 4, 249); // */

  /**
   * Orthopaedic shoemaker facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.250">1.2.276.0.76.4.250</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.250">dimdi</a>.
   */
  public static final AfiOid institution_orthopaedieschuhmacher =
      new AfiOid("institution-orthopaedieschuhmacher", 1, 2, 276, 0, 76, 4, 250); // */

  /**
   * Orthopedic technician facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.251">1.2.276.0.76.4.251</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.251">dimdi</a>.
   */
  public static final AfiOid institution_orthopaedietechniker =
      new AfiOid("institution-orthopaedietechniker", 1, 2, 276, 0, 76, 4, 251); // */

  /**
   * Dental technician facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.252">1.2.276.0.76.4.252</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.252">dimdi</a>.
   */
  public static final AfiOid institution_zahntechniker =
      new AfiOid("institution-zahntechniker", 1, 2, 276, 0, 76, 4, 252); // */

  /**
   * Rescue control center.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.253">1.2.276.0.76.4.253</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.253">dimdi</a>.
   */
  public static final AfiOid institution_rettungsleitstelle =
      new AfiOid("institution-rettungsleitstelle", 1, 2, 276, 0, 76, 4, 253); // */

  /**
   * German military medical service.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.254">1.2.276.0.76.4.254</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.254">dimdi</a>.
   */
  public static final AfiOid sanitaetsdienst_bundeswehr =
      new AfiOid("sanitaetsdienst-bundeswehr", 1, 2, 276, 0, 76, 4, 254); // */

  /**
   * Public health service facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.255">1.2.276.0.76.4.255</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.255">dimdi</a>.
   */
  public static final AfiOid institution_oegd =
      new AfiOid("institution-oegd", 1, 2, 276, 0, 76, 4, 255); // */

  /**
   * Occupational medicine facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.256">1.2.276.0.76.4.256</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.256">dimdi</a>.
   */
  public static final AfiOid institution_arbeitsmedizin =
      new AfiOid("institution-arbeitsmedizin", 1, 2, 276, 0, 76, 4, 256); // */

  /**
   * Prevention and rehabilitation facility type.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.276.0.76.4.257">1.2.276.0.76.4.257</a> and <a
   * href="http://portal.dimdi.de/websearch/servlet/Gate?accessid=showOidDoc&query=oid=1.2.276.0.76.4.257">dimdi</a>.
   */
  public static final AfiOid institution_vorsorge_reha =
      new AfiOid("institution-vorsorge-reha", 1, 2, 276, 0, 76, 4, 257); // */

  /**
   * APDU-Packet-Signer withing PoPP-Service.
   *
   * <p>For more explanation see TODO.
   */
  public static final AfiOid OID_POPP_APDU_PACKET_SIGNER =
      new AfiOid("oid-popp-apdu-packet-signer", 1, 2, 276, 0, 76, 4, 293); // */

  /**
   * PoPP-Token-Signer withing PoPP-Service.
   *
   * <p>For more explanation see TODO.
   */
  public static final AfiOid OID_POPP_TOKEN_SIGNER =
      new AfiOid("oid-popp-token-signer", 1, 2, 276, 0, 76, 4, 320); // */

  /**
   * ANSI X9.62 field type prime-field.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.10045.1.1">1.2.840.10045.1.1</a>.
   */
  public static final AfiOid fieldType = new AfiOid("fieldType", 1, 2, 840, 10_045, 1, 1); // */

  /**
   * Elliptic curve public cryptography.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.10045.2.1">1.2.840.10045.2.1</a>.
   */
  public static final AfiOid ecPublicKey = new AfiOid("ecPublicKey", 1, 2, 840, 10_045, 2, 1); // */

  /**
   * Domain parameter of an elliptic curve according to clause L.6.4.3 from <a
   * href="https://standards.globalspec.com/std/1955141/ANSI%20X9.62">ANSI X9.62-2005</a>.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.10045.3.1.7">1.2.840.10045.3.1.7</a>.
   */
  public static final AfiOid ansix9p256r1 =
      new AfiOid("ansix9p256r1", 1, 2, 840, 10_045, 3, 1, 7); // */

  /**
   * Elliptic Curve Digital Signature Algorithm (DSA) coupled with the Secure Hash Algorithm 256
   * (SHA256) algorithm.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.10045.4.3.2">1.2.840.10045.4.3.2</a>.
   */
  public static final AfiOid ECDSA_with_SHA256 =
      new AfiOid("ecdsa-with-SHA256", 1, 2, 840, 10_045, 4, 3, 2); // */

  /**
   * Elliptic Curve Digital Signature Algorithm (DSA) coupled with the Secure Hash Algorithm 384
   * (SHA384) algorithm.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.10045.4.3.3">1.2.840.10045.4.3.3</a>.
   */
  public static final AfiOid ECDSA_with_SHA384 =
      new AfiOid("ecdsa-with-SHA384", 1, 2, 840, 10_045, 4, 3, 3); // */

  /**
   * Elliptic Curve Digital Signature Algorithm (DSA) coupled with the Secure Hash Algorithm 384
   * (SHA512) algorithm.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.10045.4.3.4">1.2.840.10045.4.3.4</a>.
   */
  public static final AfiOid ECDSA_with_SHA512 =
      new AfiOid("ecdsa-with-SHA512", 1, 2, 840, 10_045, 4, 3, 4); // */

  /**
   * RSAES-PKCS1-v1_5 encryption scheme.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.1">1.2.840.113549.1.1.1</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.1">RFC 8017 A.1</a>.
   */
  public static final AfiOid rsaEncryption =
      new AfiOid("rsaEncryption", 1, 2, 840, 113_549, 1, 1, 1); // */

  /**
   * Message Digest 2 (MD2) checksum with Rivest, Shamir and Adleman (RSA) encryption.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.2">1.2.840.113549.1.1.2</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.2.4">RFC 8017 A.2.4</a>.
   */
  public static final AfiOid md2WithRSAEncryption =
      new AfiOid("md2WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 2); // */

  /**
   * Message Digest 4 (MD4) checksum with Rivest, Shamir and Adleman (RSA) encryption.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.3">1.2.840.113549.1.1.3</a>.
   */
  public static final AfiOid md4withRSAEncryption =
      new AfiOid("md4withRSAEncryption", 1, 2, 840, 113_549, 1, 1, 3); // */

  /**
   * Rivest, Shamir and Adleman (RSA) encryption with Message Digest 5 (MD5) signature.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.4">1.2.840.113549.1.1.4</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.2.4">RFC 8017 A.2.4</a>.
   */
  public static final AfiOid md5WithRSAEncryption =
      new AfiOid("md5WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 4); // */

  /**
   * Rivest, Shamir and Adleman (RSA) with Secure Hash Algorithm (SHA-1) signature.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.5">1.2.840.113549.1.1.5</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.2.4">RFC 8017 A.2.4</a>.
   */
  public static final AfiOid sha1WithRSAEncryption =
      new AfiOid("sha1WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 5); // */

  /**
   * Rivest, Shamir and Adleman (RSA) Optimal Asymmetric Encryption Padding (OAEP) encryption set.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.6">1.2.840.113549.1.1.6</a>.
   */
  public static final AfiOid ripemd160WithRSAEncryption =
      new AfiOid("ripemd160WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 6); // */

  /**
   * RSAES-OAEP.
   *
   * <p>Public-key encryption scheme combining Optimal Asymmetric Encryption Padding (OAEP) with the
   * Rivest, Shamir and Adleman Encryption Scheme (RSAES).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.7">1.2.840.113549.1.1.7</a> and <a
   * href="https://datatracker.ietf.org/doc/html/rfc8017#appendix-C">RFC 8017 C</a>.
   */
  public static final AfiOid id_RSAES_OAEP =
      new AfiOid("id-RSAES-OAEP", 1, 2, 840, 113_549, 1, 1, 7); // */

  /**
   * Rivest, Shamir and Adleman (RSA) algorithm that uses the Mask Generator Function 1 (MGF1).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.8">1.2.840.113549.1.1.8</a> and <a
   * href="https://datatracker.ietf.org/doc/html/rfc8017#appendix-B.2.1">RFC 8017 B.2.1</a>.
   */
  public static final AfiOid id_mgf1 = new AfiOid("id-mgf1", 1, 2, 840, 113_549, 1, 1, 8); // */

  /**
   * Signature algorithm {@code RSASSA-PSS}.
   *
   * <p>Rivest, Shamir, Adleman (RSA) Signature Scheme with Appendix - Probabilistic Signature
   * Scheme (RSASSA-PSS).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.10">1.2.840.113549.1.1.10</a> and <a
   * href="https://datatracker.ietf.org/doc/html/rfc8017#appendix-C">RFC 8017 C</a>.
   */
  public static final AfiOid id_RSASSA_PSS =
      new AfiOid("id-RSASSA-PSS", 1, 2, 840, 113_549, 1, 1, 10); // */

  /**
   * Signature algorithm {@code sha256WithRSAEncryption}.
   *
   * <p>Public-Key Cryptography Standards (PKCS) #1 version 1.5 signature algorithm with Secure Hash
   * Algorithm 256 (SHA256) and Rivest, Shamir and Adleman (RSA) encryption.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.11">1.2.840.113549.1.1.11</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.2.5">RFC 8017 A.2.5</a>.
   */
  public static final AfiOid sha256WithRSAEncryption =
      new AfiOid("sha256WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 11); // */

  /**
   * Signature algorithm {@code sha384WithRSAEncryption}.
   *
   * <p>Secure Hash Algorithm 384 (SHA384) with Rivest, Shamir and Adleman (RSA) Encryption.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.12">1.2.840.113549.1.1.12</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.2.5">RFC 8017 A.2.5</a>.
   */
  public static final AfiOid sha384WithRSAEncryption =
      new AfiOid("sha384WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 12); // */

  /**
   * Signature algorithm {@code sha512WithRSAEncryption}.
   *
   * <p>Secure Hash Algorithm SHA-512 with Rivest, Shamir and Adleman (RSA) encryption.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.13">1.2.840.113549.1.1.13</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.2.5">RFC 8017 A.2.5</a>.
   */
  public static final AfiOid sha512WithRSAEncryption =
      new AfiOid("sha512WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 13); // */

  /**
   * Signature algorithm {@code sha224WithRSAEncryption}.
   *
   * <p>Secure Hash Algorithm SHA-224 with Rivest, Shamir and Adleman (RSA) encryption (PKCS#1
   * version 1.5).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.14">1.2.840.113549.1.1.14</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.2.5">RFC 8017 A.2.5</a>.
   */
  public static final AfiOid sha224WithRSAEncryption =
      new AfiOid("sha224WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 14); // */

  /**
   * Signature algorithm {@code sha512-224WithRSAEncryption}.
   *
   * <p>Secure Hash Algorithm SHA-512/224 with Rivest, Shamir and Adleman (RSA) encryption.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.15">1.2.840.113549.1.1.15</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.2.5">RFC 8017 A.2.5</a>.
   */
  public static final AfiOid sha512_224WithRSAEncryption =
      new AfiOid("sha512-224WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 15); // */

  /**
   * Signature algorithm {@code sha512-256WithRSAEncryption}.
   *
   * <p>Secure Hash Algorithm SHA-512/256 with Rivest, Shamir and Adleman (RSA) encryption.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.1.16">1.2.840.113549.1.1.16</a> and <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.2.5">RFC 8017 A.2.5</a>.
   */
  public static final AfiOid sha512_256WithRSAEncryption =
      new AfiOid("sha512-256WithRSAEncryption", 1, 2, 840, 113_549, 1, 1, 16); // */

  /**
   * Password-Based Encryption {@code pbeWithMD2AndDES-CBC}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5)
   * pbeWithMD2AndDES-CBC(1)}.
   *
   * <p>Password-Based Encryption (PBE) algorithm with Message Digest 2 (MD2) and Data Encryption
   * Standard in Cipher Block Chaining mode (DES-CBC).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.1">1.2.840.113549.1.5.1</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-A.3">RFC 8018 A.3</a>.
   */
  public static final AfiOid pbeWithMD2AndDES_CBC =
      new AfiOid("pbeWithMD2AndDES-CBC", 1, 2, 840, 113_549, 1, 5, 1); // */

  /**
   * Password-Based Encryption {@code pbeWithMD5AndDES-CBC}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5)
   * pbeWithMD5AndDES-CBC(3)}.
   *
   * <p>Password-based encryption algorithm using Data Encryption Standard in Cipher Block Chaining
   * mode (DES-CBC) and Message Digest 5 (MD5).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.3">1.2.840.113549.1.5.3</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-A.3">RFC 8018 A.3</a>.
   */
  public static final AfiOid pbeWithMD5AndDES_CBC =
      new AfiOid("pbeWithMD5AndDES-CBC", 1, 2, 840, 113_549, 1, 5, 3); // */

  /**
   * Password-Based Encryption {@code pbeWithMD2AndRC2-CBC}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5)
   * pbeWithMD2AndRC2-CBC(4)}.
   *
   * <p>Password-based encryption algorithm using Rivest Cipher 2 in Cipher Block Chaining mode
   * (RC2-CBC) and Message Digest 2 (MD2).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.4">1.2.840.113549.1.5.4</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-A.3">RFC 8018 A.3</a>.
   */
  public static final AfiOid pbeWithMD2AndRC2_CBC =
      new AfiOid("pbeWithMD2AndRC2-CBC", 1, 2, 840, 113_549, 1, 5, 4); // */

  /**
   * Password-Based Encryption {@code pbeWithMD5AndRC2-CBC}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5)
   * pbeWithMD5AndRC2-CBC(6)}.
   *
   * <p>Password-based encryption algorithm using Rivest Cipher 2 in Cipher Block Chaining mode
   * (RC2-CBC) and Message Digest 5 (MD5).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.6">1.2.840.113549.1.5.6</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-A.3">RFC 8018 A.3</a>.
   */
  public static final AfiOid pbeWithMD5AndRC2_CBC =
      new AfiOid("pbeWithMD5AndRC2-CBC", 1, 2, 840, 113_549, 1, 5, 6); // */

  /**
   * Password-Based Encryption {@code pbeWithMD5AndXOR}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5)
   * pbeWithMD5AndXOR(9)}.
   *
   * <p>Password-based encryption algorithm using "XOR" and Message Digest 5 (MD5).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.9">1.2.840.113549.1.5.9</a>.
   */
  public static final AfiOid pbeWithMD5AndXOR =
      new AfiOid("pbeWithMD5AndXOR", 1, 2, 840, 113_549, 1, 5, 9); // */

  /**
   * Password-Based Encryption {@code pbeWithSHA1AndDES-CBC}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5)
   * pbeWithSHA1AndDES-CBC(10)}.
   *
   * <p>Password-based encryption algorithm using Data Encryption Standard in Cipher Block Chaining
   * mode (DES-CBC) and Secure Hash Algorithm 1 (SHA1).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.10">1.2.840.113549.1.5.10</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-A.3">RFC 8018 A.3</a>.
   */
  public static final AfiOid pbeWithSHA1AndDES_CBC =
      new AfiOid("pbeWithSHA1AndDES-CBC", 1, 2, 840, 113_549, 1, 5, 10); // */

  /**
   * Password-Based Encryption {@code pbeWithSHA1AndRC2-CBC}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5)
   * pbeWithSHA1AndRC2-CBC(11)}.
   *
   * <p>Password-based encryption algorithm using Rivest Cipher 2 in Cipher Block Chaining mode
   * (RC2-CBC) and Secure Hash Algorithm 1 (SHA1).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.11">1.2.840.113549.1.5.11</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-A.3">RFC 8018 A.3</a>.
   */
  public static final AfiOid pbeWithSHA1AndRC2_CBC =
      new AfiOid("pbeWithSHA1AndRC2-CBC", 1, 2, 840, 113_549, 1, 5, 11); // */

  /**
   * Password-Based Encryption {@code id-PBKDF2}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5) pBKDF2(12)}.
   *
   * <p>Password-Based Key Derivation Function 2 (PBKDF2).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.12">1.2.840.113549.1.5.12</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-A.2">RFC 8018 A.2</a>.
   */
  public static final AfiOid id_PBKDF2 =
      new AfiOid("id-PBKDF2", 1, 2, 840, 113_549, 1, 5, 12); // */

  /**
   * Password-Based Encryption {@code pbes2}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5) pbes2(13)}.
   *
   * <p>Password-Based Encryption Scheme 2 (PBES2).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.13">1.2.840.113549.1.5.13</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-A.4">RFC 8018 A.4</a>.
   */
  public static final AfiOid pbes2 = new AfiOid("pbes2", 1, 2, 840, 113_549, 1, 5, 13); // */

  /**
   * Password-Based Encryption {@code id-PBMAC1}.
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-5(5) PBMAC1(14)}.
   *
   * <p>Password-Based Encryption Scheme 2 (PBES2).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.2.840.113549.1.5.14">1.2.840.113549.1.5.14</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-A.5">RFC 8018 A.5</a>.
   */
  public static final AfiOid id_PBMAC1 =
      new AfiOid("id-PBMAC1", 1, 2, 840, 113_549, 1, 5, 14); // */

  /**
   * Content type "data".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-7(7) id-data(1)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.7.1">id-data</a>.
   */
  public static final AfiOid id_data = new AfiOid("id-data", 1, 2, 840, 113_549, 1, 7, 1); // */

  /**
   * Content type "signed data".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-7(7)
   * signedData(2)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.7.2">id-signedData</a>.
   */
  public static final AfiOid id_signedData =
      new AfiOid("id-signedData", 1, 2, 840, 113_549, 1, 7, 2); // */

  /**
   * Content type "enveloped data".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-7(7)
   * envelopedData(3)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.7.3">id-envelopedData</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc5652.html#section-6.1">RFC 5652 clause 6.1</a>.
   */
  public static final AfiOid id_envelopedData =
      new AfiOid("id-envelopedData", 1, 2, 840, 113_549, 1, 7, 3); // */

  /**
   * Content type "signed and enveloped data".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-7(7)
   * signedAndEnvelopedData(4)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.7.4">signedAndEnvelopedData</a>.
   */
  public static final AfiOid signedAndEnvelopedData =
      new AfiOid("signedAndEnvelopedData", 1, 2, 840, 113_549, 1, 7, 4); // */

  /**
   * Content type "id-digestedData".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-7(7)
   * digestedData(5)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.7.5">id-digestedData</a>.
   */
  public static final AfiOid id_digestedData =
      new AfiOid("id-digestedData", 1, 2, 840, 113_549, 1, 7, 5); // */

  /**
   * Content type "id-encryptedData".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-7(7)
   * encryptedData(6)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.7.6">id-encryptedData</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc5652.html#section-8">RFC 5652 clause 8</a>.
   */
  public static final AfiOid id_encryptedData =
      new AfiOid("id-encryptedData", 1, 2, 840, 113_549, 1, 7, 6); // */

  /**
   * Content type "dataWithAttributes".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-7(7)
   * dataWithAttributes(7)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.7.7">dataWithAttributes</a>.
   */
  public static final AfiOid dataWithAttributes =
      new AfiOid("dataWithAttributes", 1, 2, 840, 113_549, 1, 7, 7); // */

  /**
   * Content type "encryptedPrivateKeyInfo".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-7(7)
   * encryptedPrivateKeyInfo(8)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.7.8">dataWithAttributes</a>.
   */
  public static final AfiOid encryptedPrivateKeyInfo =
      new AfiOid("encryptedPrivateKeyInfo", 1, 2, 840, 113_549, 1, 7, 8); // */

  /**
   * Content type "e-mailAddress".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * emailAddress(1)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.1">e-mailAddress</a>.
   */
  public static final AfiOid e_mailAddress =
      new AfiOid("e-mailAddress", 1, 2, 840, 113_549, 1, 9, 1); // */

  /**
   * Content type "unstructuredName".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * unstructuredName(2)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.2">unstructuredName</a>.
   */
  public static final AfiOid unstructuredName =
      new AfiOid("unstructuredName", 1, 2, 840, 113_549, 1, 9, 2); // */

  /**
   * Content type "contentType".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * contentType(3)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.3">contentType</a>.
   */
  public static final AfiOid contentType =
      new AfiOid("contentType", 1, 2, 840, 113_549, 1, 9, 3); // */

  /**
   * Content type "id-messageDigest".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * id-messageDigest(4)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.4">id-messageDigest</a>.
   */
  public static final AfiOid id_messageDigest =
      new AfiOid("id-messageDigest", 1, 2, 840, 113_549, 1, 9, 4); // */

  /**
   * Content type "id-signingTime".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * signing-time(5)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.5">id-signingTime</a>.
   */
  public static final AfiOid id_signingTime =
      new AfiOid("id-signingTime", 1, 2, 840, 113_549, 1, 9, 5); // */

  /**
   * Content type "id-countersignature".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * countersignature(6)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.6">id-countersignature</a>.
   */
  public static final AfiOid id_coountersignature =
      new AfiOid("id-countersignature", 1, 2, 840, 113_549, 1, 9, 6); // */

  /**
   * Content type "challengePassword".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * challengePassword(7)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.7">challengePassword</a>.
   */
  public static final AfiOid challengePassword =
      new AfiOid("challengePassword", 1, 2, 840, 113_549, 1, 9, 7); // */

  /**
   * Content type "unstructuredAddress".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * unstructuredAddress(8)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.8">unstructuredAddress</a>.
   */
  public static final AfiOid unstructuredAddress =
      new AfiOid("unstructuredAddress", 1, 2, 840, 113_549, 1, 9, 8); // */

  /**
   * Content type "friendlyName".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * friendlyName(20)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.20">friendlyName</a>.
   */
  public static final AfiOid friendlyName =
      new AfiOid("friendlyName", 1, 2, 840, 113_549, 1, 9, 20); // */

  /**
   * Content type "localKeyID".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * localKeyID(21)}.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.1.9.21">friendlyName</a>.
   */
  public static final AfiOid localKeyID =
      new AfiOid("localKeyID", 1, 2, 840, 113_549, 1, 9, 21); // */

  /**
   * Content type "id-aa-CMSAlgorithmProtection".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-12(12)
   * pkcs-12Version1(10) pkcs-12BagIds(1) pkcs-8ShroudedKeyBag(2)}.
   *
   * <p>For more information see <a
   * href="http://oidref.com/1.2.840.113549.1.9.52">id_aa_CMSAlgorithmProtection</a>.
   */
  public static final AfiOid id_aa_CMSAlgorithmProtection =
      new AfiOid("id-aa-CMSAlgorithmProtection", 1, 2, 840, 113_549, 1, 9, 52); // */

  /**
   * Content type "pkcs-8ShroudedKeyBag".
   *
   * <p>ASN.1 notation: {iso(1) member-body(2) us(840) rsadsi(113549) pkcs(1) pkcs-9(9)
   * id-aa-CMSAlgorithmProtection(52)}.
   *
   * <p>For more information see <a
   * href="http://oidref.com/1.2.840.113549.1.12.10.1.2">pkcs-8ShroudedKeyBag</a>.
   */
  public static final AfiOid pkcs_8ShroudedKeyBag =
      new AfiOid("pkcs-8ShroudedKeyBag", 1, 2, 840, 113_549, 1, 12, 10, 1, 2); // */

  /**
   * Hash algorithm that uses a 128-bit key (MD2).
   *
   * <p>For more information see <a href="http://www.oid-info.com/get/1.2.840.113549.2.2">id-MD2</a>
   * and <a href="https://www.rfc-editor.org/rfc/rfc8017#appendix-B.1">PKCS#1 annex B.1</a>.
   */
  public static final AfiOid MD2 = new AfiOid("MD2", 1, 2, 840, 113_549, 2, 2); // */

  /**
   * Hash algorithm MD4.
   *
   * <p>For more information see <a href="http://www.oid-info.com/get/1.2.840.113549.2.4">md4</a>.
   */
  public static final AfiOid MD4 = new AfiOid("MD4", 1, 2, 840, 113_549, 2, 4); // */

  /**
   * Hash algorithm that uses a 128-bit key (MD5).
   *
   * <p>For more information see <a href="http://www.oid-info.com/get/1.2.840.113549.2.5">id-MD5</a>
   * and <a href="https://www.rfc-editor.org/rfc/rfc8017#appendix-B.1">PKCS#1 annex B.1</a>.
   */
  public static final AfiOid MD5 = new AfiOid("MD5", 1, 2, 840, 113_549, 2, 5); // */

  /**
   * HMAC with SHA-1.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.2.7">hmacWithSHA1</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-B.1.1">RFC 8018 B.1.1</a>.
   */
  public static final AfiOid hmacWithSHA1 =
      new AfiOid("hmacWithSHA1", 1, 2, 840, 113_549, 2, 7); // */

  /**
   * HMAC with SHA-224.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.2.8">hmacWithSHA224</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-B.1.2">RFC 8018 B.1.2</a>.
   */
  public static final AfiOid hmacWithSHA224 =
      new AfiOid("hmacWithSHA224", 1, 2, 840, 113_549, 2, 8); // */

  /**
   * HMAC with SHA-256.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.2.9">hmacWithSHA256</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-B.1.2">RFC 8018 B.1.2</a>.
   */
  public static final AfiOid hmacWithSHA256 =
      new AfiOid("hmacWithSHA256", 1, 2, 840, 113_549, 2, 9); // */

  /**
   * HMAC with SHA-384.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.2.10">hmacWithSHA384</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-B.1.2">RFC 8018 B.1.2</a>.
   */
  public static final AfiOid hmacWithSHA384 =
      new AfiOid("hmacWithSHA384", 1, 2, 840, 113_549, 2, 10); // */

  /**
   * HMAC with SHA-512.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.2.11">hmacWithSHA512</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-B.1.2">RFC 8018 B.1.2</a>.
   */
  public static final AfiOid hmacWithSHA512 =
      new AfiOid("hmacWithSHA512", 1, 2, 840, 113_549, 2, 11); // */

  /**
   * HMAC with SHA-512/224.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.2.12">hmacWithSHA512-224</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-B.1.2">RFC 8018 B.1.2</a>.
   */
  public static final AfiOid hmacWithSHA512_224 =
      new AfiOid("hmacWithSHA512-224", 1, 2, 840, 113_549, 2, 12); // */

  /**
   * HMAC with SHA-512/256.
   *
   * <p>For more information see <a
   * href="http://www.oid-info.com/get/1.2.840.113549.2.13">hmacWithSHA512-256</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-B.1.2">RFC 8018 B.1.2</a>.
   */
  public static final AfiOid hmacWithSHA512_256 =
      new AfiOid("hmacWithSHA512-256", 1, 2, 840, 113_549, 2, 13); // */

  /**
   * Rec. ITU-T X.509 certificate extensions for validity models.
   *
   * <p>Subsequent OIDs identify validity models for certificate chain verification as a certificate
   * extension.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.4.1.8301.3.5">1.3.6.1.4.1.8301.3.5</a>.
   */
  public static final AfiOid validityModels =
      new AfiOid("validityModels", 1, 3, 6, 1, 4, 1, 8301, 3, 5); // */

  /**
   * Rec. ITU-T X.509 certificate extensions for validity models, here "validityModelChain".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.4.1.8301.3.5.1">1.3.6.1.4.1.8301.3.5.1</a>.
   */
  public static final AfiOid validityModelChain =
      new AfiOid("validityModelChain", 1, 3, 6, 1, 4, 1, 8301, 3, 5, 1); // */

  /**
   * Rec. ITU-T X.509 certificate extensions for validity models, here "validityModelShell".
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.4.1.8301.3.5.2">1.3.6.1.4.1.8301.3.5.2</a>.
   */
  public static final AfiOid validityModelShell =
      new AfiOid("validityModelShell", 1, 3, 6, 1, 4, 1, 8301, 3, 5, 2); // */

  /**
   * Certificate authority information access.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.5.5.7.1.1">1.3.6.1.5.5.7.1.1</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.2.1">RFC 5280 4.2.2.1</a>.
   */
  public static final AfiOid authorityInfoAccess =
      new AfiOid("authorityInfoAccess", 1, 3, 6, 1, 5, 5, 7, 1, 1); // */

  /**
   * "qcStatements" extension.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.5.5.7.1.3">1.3.6.1.5.5.7.1.3</a> and <a
   * href="https://datatracker.ietf.org/doc/html/rfc3739#section-3.2.6">RFC 3739 3.2.6</a>.
   */
  public static final AfiOid qcStatements =
      new AfiOid("qcStatements", 1, 3, 6, 1, 5, 5, 7, 1, 3); // */

  /**
   * Public-Key Infrastructure using X.509 (PKIX) Certificate Practice Statement (CPS) pointer
   * qualifier.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.5.5.7.2.1">1.3.6.1.5.5.7.2.1</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.2.1">RFC 5280 4.2.2.1</a>.
   */
  public static final AfiOid certificatePracticeStatement =
      new AfiOid("certificatePracticeStatement", 1, 3, 6, 1, 5, 5, 7, 2, 1); // */

  /**
   * Transport Layer Security (TLS) World Wide Web (WWW) server authentication.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.5.5.7.3.1">1.3.6.1.5.5.7.3.1</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.12">RFC 5280 4.2.1.12</a>.
   */
  public static final AfiOid tlsServerAuthentication =
      new AfiOid("tlsServerAuthentication", 1, 3, 6, 1, 5, 5, 7, 3, 1); // */

  /**
   * Transport Layer Security (TLS) World Wide Web (WWW) client authentication.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.5.5.7.3.2">1.3.6.1.5.5.7.3.2</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.12">RFC 5280 4.2.1.12</a>.
   */
  public static final AfiOid tlsClientAuthentication =
      new AfiOid("tlsClientAuthentication", 1, 3, 6, 1, 5, 5, 7, 3, 2); // */

  /**
   * Signing of downloadable executable code.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.5.5.7.3.3">1.3.6.1.5.5.7.3.3</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.12">RFC 5280 4.2.1.12</a>.
   */
  public static final AfiOid signingOfDownloadableExecutableCode =
      new AfiOid("signingOfDownloadableExecutableCode", 1, 3, 6, 1, 5, 5, 7, 3, 3); // */

  /**
   * Email protection.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.5.5.7.3.4">1.3.6.1.5.5.7.3.4</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.12">RFC 5280 4.2.1.12</a>.
   */
  public static final AfiOid eMailProtection =
      new AfiOid("eMailProtection", 1, 3, 6, 1, 5, 5, 7, 3, 4); // */

  /**
   * Online Certificate Status Protocol (OCSP).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.5.5.7.48.1">1.3.6.1.5.5.7.48.1</a> and <a
   * href="https://datatracker.ietf.org/doc/html/rfc5280#section-4.2.1.4">RFC 5280 4.2.1.4</a>.
   */
  public static final AfiOid id_ad_ocsp =
      new AfiOid("id-ad-ocsp", 1, 3, 6, 1, 5, 5, 7, 48, 1); // */

  /**
   * Certificate authority issuers.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.6.1.5.5.7.48.2">1.3.6.1.5.5.7.48.2</a> and <a
   * href="https://datatracker.ietf.org/doc/html/rfc5280#section-4.2.2.1">RFC 5280 4.2.2.1</a>.
   */
  public static final AfiOid caIssuers = new AfiOid("caIssuers", 1, 3, 6, 1, 5, 5, 7, 48, 2); // */

  /**
   * Secure Hash Algorithm that uses a 160 bit key (SHA1).
   *
   * <p>For more information see <a href="http://www.oid-info.com/get/1.3.14.3.2.26">id-sha1</a> and
   * <a href="https://www.rfc-editor.org/rfc/rfc8017#appendix-B.1">PKCS#1 annex B.1</a>.
   */
  public static final AfiOid SHA1 = new AfiOid("SHA1", 1, 3, 14, 3, 2, 26); // */

  /**
   * Hash algorithm <a href="https://en.wikipedia.org/wiki/RIPEMD">RIPEMD 160</a> algorithm.
   *
   * <p>For more information see <a href="http://www.oid-info.com/get/1.3.36.3.2.1">ripemd160</a>.
   */
  public static final AfiOid RIPEMD160 = new AfiOid("ripemd160", 1, 3, 36, 3, 2, 1); // */

  /**
   * Domain parameter of an elliptic curve according to <a
   * href="https://tools.ietf.org/html/rfc5639#section-3.4">RFC-5639 clause 3.4</a>.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.36.3.3.2.8.1.1.7">1.3.36.3.3.2.8.1.1.7</a>.
   */
  public static final AfiOid brainpoolP256r1 =
      new AfiOid("brainpoolP256r1", 1, 3, 36, 3, 3, 2, 8, 1, 1, 7); // */

  /**
   * Domain parameter of an elliptic curve according to <a
   * href="https://tools.ietf.org/html/rfc5639#section-3.6">RFC-5639 clause 3.6</a>.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.36.3.3.2.8.1.1.11">1.3.36.3.3.2.8.1.1.11</a>.
   */
  public static final AfiOid brainpoolP384r1 =
      new AfiOid("brainpoolP384r1", 1, 3, 36, 3, 3, 2, 8, 1, 1, 11); // */

  /**
   * Domain parameter of an elliptic curve according to <a
   * href="https://tools.ietf.org/html/rfc5639#section-3.7">RFC-5639 clause 3.7</a>.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/1.3.36.3.3.2.8.1.1.13">1.3.36.3.3.2.8.1.1.13</a>.
   */
  public static final AfiOid brainpoolP512r1 =
      new AfiOid("brainpoolP512r1", 1, 3, 36, 3, 3, 2, 8, 1, 1, 13); // */

  /**
   * Key usage in CV-certificate, here: authentication with SHA-256.
   *
   * <p>For more explanation see <a href="http://www.gematik.de">gematik</a> gemSpec_COS v3.12.0.
   */
  public static final AfiOid authS_gemSpec_COS_G2_ecc_with_sha256 =
      new AfiOid("authS_gemSpec-COS-G2_ecc-with-sha256", 1, 3, 36, 3, 5, 3, 1); // */

  /**
   * Key usage in CV-certificate, here: authentication with SHA-384.
   *
   * <p>For more explanation see <a href="http://www.gematik.de">gematik</a> gemSpec_COS v3.12.0.
   */
  public static final AfiOid authS_gemSpec_COS_G2_ecc_with_sha384 =
      new AfiOid("authS_gemSpec-COS-G2_ecc-with-sha384", 1, 3, 36, 3, 5, 3, 2); // */

  /**
   * Key usage in CV-certificate, here: authentication with SHA-512.
   *
   * <p>For more explanation see <a href="http://www.gematik.de">gematik</a> gemSpec_COS v3.12.0.
   */
  public static final AfiOid authS_gemSpec_COS_G2_ecc_with_sha512 =
      new AfiOid("authS_gemSpec-COS-G2_ecc-with-sha512", 1, 3, 36, 3, 5, 3, 3); // */

  /**
   * Attribute admission.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/1.3.36.8.3.3">1.3.36.8.3.3</a>.
   */
  public static final AfiOid attributeAdmission =
      new AfiOid("attributeAdmission", 1, 3, 36, 8, 3, 3); // */

  /**
   * Domain parameter of an elliptic curve according to <a
   * href="https://www.rfc-editor.org/rfc/rfc7748.html#section-4.1">RFC 7748, 4.1</a>.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/1.3.101.110">1.3.101.110</a> and
   * <a href="https://www.rfc-editor.org/rfc/rfc8410.html#section-3">RFC 8410, clause 3</a>.
   */
  public static final AfiOid Curve25519 = new AfiOid("Curve25519", 1, 3, 101, 110); // */

  /**
   * Domain parameter of an elliptic curve according to <a
   * href="https://www.rfc-editor.org/rfc/rfc7748.html#section-4.2">RFC 7748, 4.2</a>.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/1.3.101.111">1.3.101.111</a> and
   * <a href="https://www.rfc-editor.org/rfc/rfc8410.html#section-3">RFC 8410, clause 3</a>.
   */
  public static final AfiOid Curve448 = new AfiOid("Curve448", 1, 3, 101, 111); // */

  /**
   * Domain parameter of an elliptic curve according to clause L.6.5.2 from <a
   * href="https://standards.globalspec.com/std/1955141/ANSI%20X9.62">ANSI X9.62-2005</a>.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/1.3.132.0.34">1.3.132.0.34</a>
   */
  public static final AfiOid ansix9p384r1 = new AfiOid("ansix9p384r1", 1, 3, 132, 0, 34); // */

  /**
   * Domain parameter of an elliptic curve according to clause L.6.6.2 from <a
   * href="https://standards.globalspec.com/std/1955141/ANSI%20X9.62">ANSI X9.62-2005</a>.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/1.3.132.0.35">1.3.132.0.35</a>
   */
  public static final AfiOid ansix9p521r1 = new AfiOid("ansix9p521r1", 1, 3, 132, 0, 35); // */

  /**
   * Attribute type {@code commonName}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.3">2.5.4.3</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.3">RFC 4519 2.3</a>.
   */
  public static final AfiOid id_at_commonName = new AfiOid("commonName", 2, 5, 4, 3); // */

  /**
   * Attribute type surname.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.4">2.5.4.4</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.32">RFC 4519 2.32</a>.
   */
  public static final AfiOid id_at_surname = new AfiOid("surname", 2, 5, 4, 4); // */

  /**
   * Attribute type serial number.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.5">2.5.4.5</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.31">RFC 4519 2.31</a>.
   */
  public static final AfiOid id_at_serialNumber = new AfiOid("serialNumber", 2, 5, 4, 5); // */

  /**
   * Attribute type country name.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.6">2.5.4.6</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.2">RFC 2.2</a>.
   */
  public static final AfiOid id_at_countryName = new AfiOid("countryName", 2, 5, 4, 6); // */

  /**
   * Attribute type locality name.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.7">2.5.4.7</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.16">RFC 2.16</a>.
   */
  public static final AfiOid id_at_localityName = new AfiOid("localityName", 2, 5, 4, 7); // */

  /**
   * Attribute type state or province name.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.8">2.5.4.8</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#appendix-A.1">RFC 5280 A.1</a>.
   */
  public static final AfiOid id_at_stateOrProvinceName =
      new AfiOid("stateOrProvinceName", 2, 5, 4, 8); // */

  /**
   * Attribute type street address.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.9">2.5.4.9</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.34">RFC 4519 2.34</a>.
   */
  public static final AfiOid id_at_streetAddress = new AfiOid("streetAddress", 2, 5, 4, 9); // */

  /**
   * Attribute type organization name.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.10">2.5.4.10</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.19">RFC 4519 2.19</a>.
   */
  public static final AfiOid id_at_organizationName =
      new AfiOid("organizationName", 2, 5, 4, 10); // */

  /**
   * Attribute type organization unit name.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.11">2.5.4.11</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.20">RFC 4519 2.20</a>.
   */
  public static final AfiOid id_at_organizationUnitName =
      new AfiOid("organizationUnitName", 2, 5, 4, 11); // */

  /**
   * Attribute type title.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.12">2.5.4.12</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.38">RFC 4519 2.38</a>.
   */
  public static final AfiOid id_at_title = new AfiOid("title", 2, 5, 4, 12); // */

  /**
   * Attribute type postal code.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.17">2.5.4.17</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.24">RFC 4519 2.24</a>.
   */
  public static final AfiOid id_at_postalCode = new AfiOid("postalCode", 2, 5, 4, 17); // */

  /**
   * Attribute type given name.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.42">2.5.4.42</a> and <a
   * href="https://tools.ietf.org/html/rfc4519#section-2.12">RFC 4519 2.12</a>.
   */
  public static final AfiOid id_at_givenName = new AfiOid("givenName", 2, 5, 4, 42); // */

  /**
   * Attribute type organization identifier.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.4.97">2.5.4.97</a>.
   */
  public static final AfiOid id_at_organizationIdentifier =
      new AfiOid("organizationIdentifier", 2, 5, 4, 97); // */

  /**
   * Certificate extension {@code subjectKeyIdentifier}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.14">2.5.29.14</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.2">RFC 5280 4.2.1.2</a>.
   */
  public static final AfiOid subjectKeyIdentifier =
      new AfiOid("subjectKeyIdentifier", 2, 5, 29, 14); // */

  /**
   * Certificate extension {@code keyUsage}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.15">2.5.29.15</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.3">RFC 5280 4.2.1.3</a>.
   */
  public static final AfiOid keyUsage = new AfiOid("keyUsage", 2, 5, 29, 15); // */

  /**
   * Certificate extension {@code privateKeyUsagePeriod}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.16">2.5.29.16</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc5280.html#appendix-A.2">RFC 5280 A.2</a>.
   */
  public static final AfiOid privateKeyUsagePeriod =
      new AfiOid("privateKeyUsagePeriod", 2, 5, 29, 16); // */

  /**
   * Certificate extension {@code subjectAltName}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.17">2.5.29.17</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc5280.html#section-4.2.1.6">RFC 5280 4.2.1.6</a>.
   */
  public static final AfiOid subjectAltName = new AfiOid("subjectAltName", 2, 5, 29, 17); // */

  /**
   * Certificate extension {@code issuerAltName}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.18">2.5.29.18</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc5280.html#section-4.2.1.7">RFC 5280 4.2.1.7</a>.
   */
  public static final AfiOid issuerAltName = new AfiOid("issuerAltName", 2, 5, 29, 18); // */

  /**
   * Certificate extension {@code basicConstraints}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.19">2.5.29.19</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.9">RFC 5280 4.2.1.9</a>.
   */
  public static final AfiOid basicConstraints = new AfiOid("basicConstraints", 2, 5, 29, 19); // */

  /**
   * Certificate extension {@code crlDistributionPoint}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.31">2.5.29.31</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.13">RFC 5280 4.2.1.13</a>.
   */
  public static final AfiOid crlDistributionPoint =
      new AfiOid("crlDistributionPoint", 2, 5, 29, 31); // */

  /**
   * Certificate extension {@code certificatePolicies}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.32">2.5.29.32</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.4">RFC 5280 4.2.1.4</a>.
   */
  public static final AfiOid certificatePolicies =
      new AfiOid("certificatePolicies", 2, 5, 29, 32); // */

  /**
   * Certificate extension {@code authorityKeyIdentifier}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.35">2.5.29.35</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.1">RFC 5280 4.2.1.1</a>.
   */
  public static final AfiOid authorityKeyIdentifier =
      new AfiOid("authorityKeyIdentifier", 2, 5, 29, 35); // */

  /**
   * Certificate extension {@code extKeyUsage}.
   *
   * <p>For more explanation see <a href="http://oid-info.com/get/2.5.29.37">2.5.29.37</a> and <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.2.1.12">RFC 5280 4.2.1.12</a>.
   */
  public static final AfiOid extKeyUsage = new AfiOid("extKeyUsage", 2, 5, 29, 37); // */

  /**
   * aes128-ECB.
   *
   * <p>ASN.1 notation: {joint-iso-itu-t(2) country(16) us(840) organization(1) gov(101) csor(3)
   * nistAlgorithms(4) aes(1) aes128-ECB(1)}.
   *
   * <p>Electronic CodeBook (ECB) with 128-bit Advanced Encryption Standard (AES) information.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.101.3.4.1.1">2.16.840.1.101.3.4.1.1</a>.
   */
  public static final AfiOid aes128_ECB =
      new AfiOid("aes128-ECB", 2, 16, 840, 1, 101, 3, 4, 1, 1); // */

  /**
   * aes128-CBC-PAD.
   *
   * <p>ASN.1 notation: {joint-iso-itu-t(2) country(16) us(840) organization(1) gov(101) csor(3)
   * nistAlgorithms(4) aes(1) aes128-CBC-PAD(2)}.
   *
   * <p>Voice encryption 128-bit Advanced Encryption Standard (AES) algorithm with Cipher-Block
   * Chaining (CBC) mode of operation.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.101.3.4.1.2">2.16.840.1.101.3.4.1.2</a>.
   */
  public static final AfiOid aes128_CBC_PAD =
      new AfiOid("aes128-CBC-PAD", 2, 16, 840, 1, 101, 3, 4, 1, 2); // */

  /**
   * aes128-OFB.
   *
   * <p>ASN.1 notation: {joint-iso-itu-t(2) country(16) us(840) organization(1) gov(101) csor(3)
   * nistAlgorithms(4) aes(1) aes128-OFB(3)}.
   *
   * <p>OFB (128 bit AES information).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.101.3.4.1.3">2.16.840.1.101.3.4.1.3</a>.
   */
  public static final AfiOid aes128_OFB =
      new AfiOid("aes128-OFB", 2, 16, 840, 1, 101, 3, 4, 1, 3); // */

  /**
   * aes192-CBC-PAD.
   *
   * <p>ASN.1 notation: {joint-iso-itu-t(2) country(16) us(840) organization(1) gov(101) csor(3)
   * nistAlgorithms(4) aes(1) aes192-CBC-PAD(22)}.
   *
   * <p>256-bit Advanced Encryption Standard (AES) algorithm with Cipher-Block Chaining (CBC) mode
   * of operation.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.101.3.4.1.22">2.16.840.1.101.3.4.1.22</a>. and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-C">RFC 8018 C</a>.
   */
  public static final AfiOid aes192_CBC_PAD =
      new AfiOid("aes192-CBC-PAD", 2, 16, 840, 1, 101, 3, 4, 1, 22); // */

  /**
   * aes256-CBC-PAD.
   *
   * <p>ASN.1 notation: {joint-iso-itu-t(2) country(16) us(840) organization(1) gov(101) csor(3)
   * nistAlgorithms(4) aes(1) aes256-CBC-PAD(42)}.
   *
   * <p>256-bit Advanced Encryption Standard (AES) algorithm with Cipher-Block Chaining (CBC) mode
   * of operation.
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.101.3.4.1.42">2.16.840.1.101.3.4.1.42</a>. and <a
   * href="https://www.rfc-editor.org/rfc/rfc8018.html#appendix-C">RFC 8018 C</a>.
   */
  public static final AfiOid aes256_CBC_PAD =
      new AfiOid("aes256-CBC-PAD", 2, 16, 840, 1, 101, 3, 4, 1, 42); // */

  /**
   * Secure Hash Algorithm that uses a 256-bit key (SHA256).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.101.3.4.2.1">2.16.840.1.101.3.4.2.1</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8017#appendix-B.1">PKCS#1 annex B.1</a>.
   */
  public static final AfiOid SHA256 = // PI_DO_NOT_REUSE_PUBLIC_IDENTIFIERS_FIELD_NAMES
      new AfiOid("SHA256", 2, 16, 840, 1, 101, 3, 4, 2, 1); // */

  /**
   * Secure Hash Algorithm that uses a 384-bit key (SHA384).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.101.3.4.2.2">2.16.840.1.101.3.4.2.2</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8017#appendix-B.1">PKCS#1 annex B.1</a>.
   */
  public static final AfiOid SHA384 = new AfiOid("SHA384", 2, 16, 840, 1, 101, 3, 4, 2, 2); // */

  /**
   * Secure Hash Algorithm that uses a 512-bit key (SHA512).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.101.3.4.2.3">2.16.840.1.101.3.4.2.3</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8017#appendix-B.1">PKCS#1 annex B.1</a>.
   */
  public static final AfiOid SHA512 = new AfiOid("SHA512", 2, 16, 840, 1, 101, 3, 4, 2, 3); // */

  /**
   * Secure Hash Algorithm that uses a 224-bit key (SHA224).
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.101.3.4.2.4">2.16.840.1.101.3.4.2.4</a> and <a
   * href="https://www.rfc-editor.org/rfc/rfc8017#appendix-B.1">PKCS#1 annex B.1</a>.
   */
  public static final AfiOid SHA224 = new AfiOid("SHA224", 2, 16, 840, 1, 101, 3, 4, 2, 4); // */

  /**
   * Certificate type.
   *
   * <p>Netscape certificate type (a Rec. ITU-T X.509 v3 certificate extension used to identify
   * whether the certificate subject is a Secure Sockets Layer (SSL) client, an SSL server or a
   * Certificate Authority (CA))
   *
   * <p>For more explanation see <a
   * href="http://oid-info.com/get/2.16.840.1.113730.1.1">2.16.840.1.113730.1.1</a>.
   */
  public static final AfiOid cert_type =
      new AfiOid("cert-type", 2, 16, 840, 1, 113_730, 1, 1); // */

  /** Set of pre-defined object identifier. */
  public static final List<AfiOid> PREDEFINED =
      List.of(
          // Note 1: Intentionally a list is used here rather than a Set.
          // Note 2: In case a new OID is defined but not listed here, IntelliJ will
          //         mark that OID as "unused".
          // Note 3: Test-method setUpBeforeClass() will check that all elements in
          //         this list are mutual exclusive.
          INVALID,
          CVC_FlagList_CMS,
          CVC_FlagList_TI,
          ECDSA_with_SHA256,
          ECDSA_with_SHA384,
          ECDSA_with_SHA512,
          Curve25519,
          Curve448,
          MD2,
          MD4,
          MD5,
          OID_POPP_APDU_PACKET_SIGNER,
          OID_POPP_TOKEN_SIGNER,
          RIPEMD160,
          SHA1,
          SHA224,
          SHA256,
          SHA384,
          SHA512,
          adv_ktr,
          aes128_CBC_PAD,
          aes128_ECB,
          aes128_OFB,
          aes192_CBC_PAD,
          aes256_CBC_PAD,
          ak_aut,
          ak_ghcs,
          arzt_ghcs,
          ansix9p256r1,
          ansix9p384r1,
          ansix9p521r1,
          attributeAdmission,
          audit_ghcs,
          authorityInfoAccess,
          authorityKeyIdentifier,
          authS_gemSpec_COS_G2_ecc_with_sha256,
          authS_gemSpec_COS_G2_ecc_with_sha384,
          authS_gemSpec_COS_G2_ecc_with_sha512,
          basicConstraints,
          brainpoolP256r1,
          brainpoolP384r1,
          brainpoolP512r1,
          broker_anonym_ghcs,
          broker_ghcs,
          bs_gematik_ghcs,
          bs_gkv_spitzenverband,
          bundeswehrapotheke_ghcs,
          caIssuers,
          cert_type,
          certificatePolicies,
          certificatePracticeStatement,
          challengePassword,
          cmfd,
          cms_ghcs,
          contentType,
          crlDistributionPoint,
          dataWithAttributes,
          dental_practice,
          e_mailAddress,
          eMailProtection,
          ecPublicKey,
          egk_aut,
          egk_autn,
          egk_enc,
          egk_encv,
          egk_qes,
          egk_sig,
          encryptedPrivateKeyInfo,
          extKeyUsage,
          fieldType,
          friendlyName,
          hba_aut,
          hba_enc,
          hba_qes,
          hba_sig,
          hmacWithSHA1,
          hmacWithSHA224,
          hmacWithSHA256,
          hmacWithSHA384,
          hmacWithSHA512,
          hmacWithSHA512_224,
          hmacWithSHA512_256,
          id_PACE_ECDH_GM_AES_CBC_CMAC_128,
          id_PACE_ECDH_GM_AES_CBC_CMAC_192,
          id_PACE_ECDH_GM_AES_CBC_CMAC_256,
          id_PBKDF2,
          id_PBMAC1,
          id_RSAES_OAEP,
          id_RSASSA_PSS,
          id_aa_CMSAlgorithmProtection,
          id_ad_ocsp,
          id_at_commonName,
          id_at_countryName,
          id_at_givenName,
          id_at_localityName,
          id_at_organizationIdentifier,
          id_at_organizationName,
          id_at_organizationUnitName,
          id_at_postalCode,
          id_at_serialNumber,
          id_at_stateOrProvinceName,
          id_at_streetAddress,
          id_at_surname,
          id_at_title,
          id_coountersignature,
          id_data,
          id_digestedData,
          id_encryptedData,
          id_envelopedData,
          id_messageDigest,
          id_mgf1,
          id_signedData,
          id_signingTime,
          institution_arbeitsmedizin,
          institution_augenoptiker,
          institution_geburtshilfe,
          institution_hoerakustiker,
          institution_oegd,
          institution_orthopaedieschuhmacher,
          institution_orthopaedietechniker,
          institution_pflege,
          institution_rettungsleitstelle,
          institution_vorsorge_reha,
          institution_zahntechniker,
          int_vsdm_ghcs,
          issuerAltName,
          keyUsage,
          komle,
          komle_recipient_emails,
          konfigdienst_ghcs,
          kostentraeger_ghcs,
          krankenhaus_ghcs,
          krankenhausapotheke_ghcs,
          kt_ghcs,
          leo_aerztekammer,
          leo_apothekerverband,
          leo_baek,
          leo_bzaek,
          leo_dav,
          leo_dkg,
          leo_dktig,
          leo_kassenaerztliche_vereinigung,
          leo_kbv,
          leo_krankenhausverband,
          leo_kzbv,
          leo_zahnaerzte,
          leo_zahnaerztekammer,
          localKeyID,
          md2WithRSAEncryption,
          md4withRSAEncryption,
          md5WithRSAEncryption,
          medical_practice,
          mobile_einrichtung_rettungsdienst_ghcs,
          nk_ghcs,
          nk_vpn,
          oeffentliche_apotheke_ghcs,
          pbeWithMD2AndDES_CBC,
          pbeWithMD2AndRC2_CBC,
          pbeWithMD5AndDES_CBC,
          pbeWithMD5AndRC2_CBC,
          pbeWithMD5AndXOR,
          pbeWithSHA1AndDES_CBC,
          pbeWithSHA1AndRC2_CBC,
          pbes2,
          pkcs_8ShroudedKeyBag,
          policy_gem_010200_or_cp,
          policy_gem_tsl_signer,
          policy_hba_010005_cp,
          praxis_physiotherapeut,
          privateKeyUsagePeriod,
          psychotherapist_practice,
          qcStatements,
          ripemd160WithRSAEncryption,
          rsaEncryption,
          sak_aut,
          sak_ghcs,
          sanitaetsdienst_bundeswehr,
          sds_ghcs,
          sha256WithRSAEncryption,
          sha384WithRSAEncryption,
          sha512WithRSAEncryption,
          sf_aut,
          sf_enc,
          sf_ssl,
          sf_ssl_c,
          sf_ssl_s,
          sha1WithRSAEncryption,
          sha224WithRSAEncryption,
          sha512_224WithRSAEncryption,
          sha512_256WithRSAEncryption,
          signedAndEnvelopedData,
          signingOfDownloadableExecutableCode,
          smc_b_aut,
          smc_b_enc,
          smc_b_osig,
          smkt_aut,
          subjectAltName,
          subjectKeyIdentifier,
          tlsClientAuthentication,
          tlsServerAuthentication,
          trusted_ghcs,
          tsl_ghcs,
          ufs_ghcs,
          unstructuredAddress,
          unstructuredName,
          validityModelChain,
          validityModelShell,
          validityModels,
          vodd_ghcs,
          vpnk_ghcs,
          vpnk_mwdn_ghcs,
          vpnk_ti_ghcs,
          vpnk_vpn,
          vpnz_sis_ghcs,
          vpnz_ti_ghcs,
          vsdd_ghcs,
          vzd_ti); // */

  /** Human-readable name of OID, e.g. "ansix9p256r1". */
  private final String insName; // */

  /** ANS.1 notation of OID, e.g. "{1 2 840 10045 3 1 7}". */
  private final String insAsn1; // */

  /** Octet string representation of OID, e.g. "2b240305030107". */
  private final String insOctetString; // */

  /** Point-notation of OID, e.g. "1.2.840.10045.3.1.7". */
  private final String insPoint; // */

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Because only immutable instance attributes of this class are taken into account for
   *       this instance attribute, lazy initialization is possible.</i>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  private volatile int insHashCode; // NOPMD volatile */

  /**
   * Comfort constructor using an octet string, e.g. '2b2403050301'.
   *
   * @param oid octet string representation of OID
   * @throws ArithmeticException if any component isn't in range [0, {@link Long#MAX_VALUE}]
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>any component isn't in range [0, {@link Integer#MAX_VALUE}]
   *       <li>{@code oid} ends early
   *     </ol>
   */
  public AfiOid(final byte[] oid) {
    this(os2Components(oid));
  } // end method */

  /**
   * Comfort constructor using an array of components, e.g. [1, 2, 840, 10045, 3, 1, 7].
   *
   * @param components of object identifier
   */
  public AfiOid(final int... components) {
    this(Arrays.stream(components).boxed().toList());
  } // end constructor */

  /**
   * Comfort constructor using a {@link List} of components, e.g. {@code List.of(1, 2, 840, 10045,
   * 3, 1, 7)}.
   *
   * @param components of object identifier
   */
  public AfiOid(final Collection<Integer> components) {
    super();
    insAsn1 = components2Asn1(components);
    insOctetString = components2Os(components);
    insPoint = components2Point(components);

    // intentionally, this statement is the last, so estimateName()-method
    // is able to us equals(...)-method
    insName = estimateName();
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param name human-readable name
   * @param components of object identifier
   */
  private AfiOid(final String name, final int... components) {
    super();
    final List<Integer> list = Arrays.stream(components).boxed().toList();

    insName = name;
    insAsn1 = components2Asn1(list);
    insOctetString = components2Os(list);
    insPoint = components2Point(list);
  } // end constructor */

  /**
   * Converts the given list of object identifier components into ASN.1 notation.
   *
   * <p>Counterpart to method {@link #asn2Components(String)}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change any of the entries in
   *       the input list after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and the
   *       return value is immutable.</i>
   * </ol>
   *
   * @param components of object identifier, e.g. {@code List.of(1, 2, 840, 10045, 3, 1, 7)}
   * @return ASN.1 representation, e.g. "{1 2 840 10045 3 1 7}"
   */
  public static String components2Asn1(final Collection<Integer> components) {
    return components.stream()
        .map(i -> Integer.toString(i)) // NOPMD use lambda expression (false positive)
        .collect(
            Collectors.joining(
                " ", // delimiter
                "{", // prefix
                "}" // suffix
                ));
  } // end method */

  /**
   * Converts the given array of object identifier components into octet string representation.
   *
   * <p>This method first converts given array to {@link List} and then uses {@link
   * #components2Os(Collection)}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change any of the entries in
   *       the input array after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and the
   *       return value is immutable.</i>
   * </ol>
   *
   * @param components of object identifier
   * @return octet string representation, e.g. [2, 100, 3] is converted to "813403"
   * @throws IllegalArgumentException if {@link #components2Os(Collection)} does so, see there
   */
  public static String components2Os(final int... components) {
    return components2Os(Arrays.stream(components).boxed().toList());
  } // end method */

  /**
   * Converts the given list of object identifier components into octet string representation.
   *
   * <p>Counterpart to method {@link #os2Components(byte[])}.
   *
   * <p>For more information, refer to ISO/IEC 8825-1:2021 clause 8.19.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change any of the entries in
   *       the input list after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and the
   *       return value is immutable.</i>
   * </ol>
   *
   * @param components of object identifier
   * @return octet string representation, e.g. {@code List.of(2, 100, 3)} is converted to "813403"
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>less than two components are given
   *       <li>any component is negative
   *       <li>the first component is greater than two
   *       <li>the first component is in range [0, 1], and the second component is greater than 39
   *     </ol>
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  public static String components2Os(final Collection<Integer> components) {
    // --- check amount of components
    if (components.size() < 2) { // NOPMD literal in if statement
      throw new IllegalArgumentException("less than two object identifier components");
    } // end fi
    // ... at least two components

    // --- check for non-negative components
    if (components.stream().anyMatch(i -> i < 0)) {
      throw new IllegalArgumentException("at least one component is negative");
    } // end fi
    // ... all components are non-negative

    // --- check range of first component
    final Iterator<Integer> ite = components.iterator();
    final int firstComponent = ite.next();
    if (firstComponent > 2) { // NOPMD literal in if statement
      // ... first component greater than 2
      throw new IllegalArgumentException("first component greater than 2");
    } // end fi
    // ... first component in range [0, 2]

    // --- check range of second component
    final int secondComponent = ite.next();
    if ((firstComponent < 2) && (secondComponent > 39)) {
      // ... firstComponent from set {0, 1} but secondComponent greater than 39
      throw new IllegalArgumentException(
          "first component in range [0, 1], but second component greater than 39");
    } // end fi
    // ... all components okay for this implementation

    // --- conversion
    // allocate buffer
    // Note: Because subidentifier are int at most five octet are needed to encode each.
    final ByteBuffer buffer = ByteBuffer.allocate((components.size() - 1) * 5);

    // write first subidentifier to buffer
    final long firstSubidentifier = 40L * firstComponent + secondComponent;
    writeSubId(buffer, firstSubidentifier);

    // write (all) other subidentifier
    while (ite.hasNext()) {
      writeSubId(buffer, ite.next());
    } // end While (...)

    return Hex.toHexDigits(buffer.array(), 0, buffer.position());
  } // end method */

  /**
   * Converts the given list of object identifier components into point notation.
   *
   * <p>Counterpart to method {@link #point2Components(String)}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change any of the entries in
   *       the input list after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and the
   *       return value is immutable.</i>
   * </ol>
   *
   * @param components of object identifier, e.g. {@code List.of(1, 2, 840, 10045, 3, 1, 7)}
   * @return ASN.1 representation, e.g. "1.2.840.10045.3.1.7"
   */
  public static String components2Point(final Collection<Integer> components) {
    return components.stream()
        .map(i -> Integer.toString(i)) // NOPMD use lambda expression (false positive)
        .collect(Collectors.joining("."));
  } // end method */

  /**
   * Converts ASN.1-notation of OID into list of object identifier components.
   *
   * <p>Counterpart to method {@link #components2Asn1(Collection)}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is immutable and the
   *       return value is never used again within this class.</i>
   * </ol>
   *
   * @param oid given in point-notation, e.g. "{1 2 840 10045 3 1 7}"
   * @return list of object identifier components, e.g. {@code List.of(1, 2, 840, 10045, 3, 1, 7)}
   * @throws ArithmeticException if any component isn't in range [{@link Integer#MIN_VALUE}, {@link
   *     Integer#MAX_VALUE}]
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>given {@code oid} doesn't start with '{' character
   *       <li>given {@code oid} doesn't end with '}' character
   *       <li>given {@code oid} contains characters other than decimal digits or space (i.e. ' ')
   *           (apart from prefix '{' and suffix '}')
   *       <li>any component is negative
   *     </ol>
   */
  public static List<Integer> asn2Components(final String oid) {
    // --- check prefix
    if ('{' != oid.charAt(0)) { // NOPMD literal in if statement
      throw new IllegalArgumentException("input doesn't start with '{'-character");
    } // end fi

    // --- check suffix
    final int endIndex = oid.length() - 1;
    if ('}' != oid.charAt(endIndex)) { // NOPMD literal in if statement
      throw new IllegalArgumentException("input doesn't end with '}'-character");
    } // end fi
    // ... correct prefix and suffix

    return string2Components(oid.substring(1, endIndex), ' ');
  } // end method */

  /**
   * Converts octet string representation of OID into a list of object identifier components.
   *
   * <p>Counterpart to method {@link #components2Os(Collection)}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change entries in the input
   *       array after calling this method by another thread.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and
   *       otherwise not used and the return value is never used again within this class.</i>
   * </ol>
   *
   * @param oid given as octet string, e.g. "2b240305030107"
   * @return list of object identifier components, e.g. {@code List.of(1, 2, 840, 10045, 3, 1, 7)}
   * @throws ArithmeticException if any component isn't in range [0, {@link Long#MAX_VALUE}]
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>any component isn't in range [0, {@link Integer#MAX_VALUE}]
   *       <li>{@code oid} ends early
   *     </ol>
   */
  public static List<Integer> os2Components(final byte[] oid) {
    try {
      final List<Integer> result = new ArrayList<>();
      final ByteBuffer buffer = ByteBuffer.wrap(oid);

      // --- read object identifier components from buffer
      // read first subidentifier
      final long firstSubidentifier = readSubId(buffer);
      final int firstComponent = (firstSubidentifier >= 80) ? 2 : (int) (firstSubidentifier / 40);
      result.add(firstComponent);

      // check range of secondComponent
      final long secondComponent = firstSubidentifier - 40L * firstComponent;
      if (secondComponent > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("secondComponent too big for this implementation");
      } // end fi
      result.add((int) secondComponent);

      while (buffer.remaining() > 0) {
        final long component = readSubId(buffer);
        if (component > Integer.MAX_VALUE) {
          throw new IllegalArgumentException("component too big for this implementation");
        } // end fi
        result.add((int) component);
      } // end While (...)

      return result;
    } catch (BufferUnderflowException e) {
      throw new IllegalArgumentException(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Converts point-notation of OID into list of object identifier components.
   *
   * <p>Counterpart to method {@link #components2Point(Collection)}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is immutable and the
   *       return value is never used again within this class.</i>
   * </ol>
   *
   * @param oid given in point-notation, e.g. "1.2.840.10045.3.1.7"
   * @return list of object identifier components, e.g. {@code List.of(1, 2, 840, 10045, 3, 1, 7)}
   * @throws ArithmeticException if any component isn't in range [{@link Integer#MIN_VALUE}, {@link
   *     Integer#MAX_VALUE}]
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>given {@code oid} contains characters other than decimal digits or point (i.e. '.')
   *       <li>any component in list is negative
   *     </ol>
   */
  public static List<Integer> point2Components(final String oid) {
    return string2Components(oid, '.');
  } // end method */

  /**
   * Converts list of object identifier components from {@link String} into {@link List}.
   *
   * <p>It is assumed that object identifier components are separated by given {@code delimiter}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameters are immutable or
   *       primitive and the return value is never used again within this class.</i>
   * </ol>
   *
   * @param oid to be converted
   * @param delimiter separating adjacent object identifier components
   * @return list of object identifier components
   * @throws ArithmeticException if any component isn't in range [{@link Integer#MIN_VALUE}, {@link
   *     Integer#MAX_VALUE}]
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>any component in list is negative
   *     </ol>
   *
   * @throws NumberFormatException if
   *     <ol>
   *       <li>given {@code oid} is empty
   *       <li>a string to be converted to an integer is empty
   *       <li>a string to be converted to an integer contains non-decimal digits
   *     </ol>
   */
  /* package */
  static List<Integer> string2Components(final String oid, final char delimiter) {
    final List<Integer> result = new ArrayList<>();

    int start = 0;
    int end;

    for (; ; ) {
      end = oid.indexOf(delimiter, start);

      if (-1 == end) {
        // ... no more delimiter-characters
        //     => treat rest of string as integer
        result.add(new BigInteger(oid.substring(start)).intValueExact()); // NOPMD new in loop
        // ... at least one component present in result
        //     => the OptionalInt returned by ".min()" is always present.
        //        Thus, "isPresent()" is not necessary

        if (result.stream().mapToInt(i -> i).min().getAsInt() < 0) {
          // ... at least one component is negative
          throw new IllegalArgumentException("at least one component is negative");
        } // end fi

        return result;
      } // end fi
      // ... found delimiter-character

      result.add(new BigInteger(oid.substring(start, end)).intValueExact()); // NOPMD new in loop

      start = end + 1;
    } // end For (;;)
  } // end method */

  /**
   * Reads a subidentifier from given buffer.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is a problem here, because buffer is modified by this method.</i>
   * </ol>
   *
   * @param buffer from which a subidentifier is read
   * @return subidentifier read from buffer
   * @throws ArithmeticException if value of subidentifier of greater than {@link Long#MAX_VALUE}
   * @throws BufferUnderflowException if buffer ends before the end of the subidentifier
   */
  /* package */
  static long readSubId(final ByteBuffer buffer) {
    // loop until a byte with bit 8 not set is read, concatenate bits b7 to b1 of bytes read
    BigInteger result = BigInteger.ZERO; // initialize result
    byte nextByte; // define loop-variable
    do {
      nextByte = buffer.get(); // -128 <= nextByte <= 127
      result = result.shiftLeft(7); // shift
      result = result.add(BigInteger.valueOf(nextByte & 0x7f)); // add bits from nextByte
    } while (nextByte < 0); // bit 8 of nextByte not set => end of subidentifier

    // Note 1: The current implementation uses int for coding of subidentifier.
    return result.longValueExact();
  } // end method */

  /**
   * Writes subidentifier to buffer.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is a problem here, because buffer is modified by this method.</i>
   * </ol>
   *
   * @param buffer to which {@code subidentifier} is written to
   * @param subidentifier to be written to {@code buffer}
   * @throws BufferOverflowException if there is insufficient space in {@code buffer}
   */
  /* package */
  static void writeSubId(final ByteBuffer buffer, long subidentifier) {
    // ... Assertions: 0 <= subidentifier <= 2*2 + Integer.MAX_VALUE

    // Note 1: The current implementation uses int for coding of object
    //         identifier components.
    // Note 2: It follows that the biggest value for subidentifier occurs if the
    //         first subidentifier is calculated for firstComponent = X = 2 and
    //         secondComponent = Y = Integer.MAX_VALUE:
    //         firstSubidentifier = (40 * X) + Y, see ISO/IEC 8825-1:2021, clause 8.19.4.
    // Note 3: If follows that each subidentifier has at most 32 bit. Thus,
    //         5 octet (each containing at most 7 bits from input-parameter
    //         subidentifier) are sufficient.

    // allocate buffer for subidentifier
    int index = 5; // see note 3, why five octet are sufficient
    final byte[] octets = new byte[index]; // buffer for octet string encoding of subidentifier.

    // store least significant octet in buffer, here bit b8 is always 0
    octets[--index] = (byte) (subidentifier & 0x7f);

    // store other octet in buffer, here bit b8 is always 1
    while ((subidentifier >>= 7) > 0) { // NOPMD reassigned parameter
      octets[--index] = (byte) (0x80 | (subidentifier & 0x7f));
    } // end While (...)

    // write octets of subidentifier to buffer
    buffer.put(octets, index, octets.length - index);
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
    // ... obj is instance of AfiOid

    final AfiOid other = (AfiOid) obj;

    // --- compare instance attributes
    // ... assertion: instance attributes are never null
    // Note 2: Because all instance attributes (except insHashCode) could be converted
    //         into each other, it is sufficient to just take into account insOctetString.
    return insOctetString.equals(other.insOctetString);
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
    // Note 2: Because all other attributes could be calculated from octet-string
    //         representation, here just that attribute is taken into account.

    int result = insHashCode; // read attribute from main memory into thread local memory
    if (0 == result) {
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      result = insOctetString.hashCode(); // start value

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Estimate object-identifier of curve parameter.
   *
   * @return name corresponding to OID for pre-defined OID in {@link #PREDEFINED} or {@link
   *     #getPoint()} otherwise
   */
  private String estimateName() {
    return PREDEFINED.parallelStream()
        .filter(this::equals)
        .findAny()
        .map(AfiOid::getName)
        .orElse(getPoint());
  } // end method */

  /**
   * Return human-readable name of object identifier, e.g. "Ansix9p256r1".
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the return value is immutable.</i>
   * </ol>
   *
   * @return human-readable name of OID or {@link #getPoint()} if the OID is not pre-defined in this
   *     class
   */
  public String getName() {
    return insName;
  } // end method */

  /**
   * Returns list of object identifier components.
   *
   * <p>Compared to {@link #getPoint()} here the components are separated by space (i.e. ' ')
   * character and enclosed in curly brackets.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the return value is immutable.</i>
   * </ol>
   *
   * @return list of object identifier components, e.g. "{1 2 840 10045 3 1 7}"
   */
  public String getAsn1() {
    return insAsn1;
  } // end method */

  /**
   * Returns octet string representation of this object identifier.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the return value is immutable.</i>
   * </ol>
   *
   * @return octet string representation of OID, e.g. "2b240305030107"
   */
  public String getOctetString() {
    return insOctetString;
  } // end method */

  /**
   * Returns list of object identifier components.
   *
   * <p>Compared to {@link #getAsn1()} here the components are separated by point (i.e. '.')
   * character and not enclosed in any bracket.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the return value is immutable.</i>
   * </ol>
   *
   * @return list of object identifier components, e.g. "1.2.840.10045.3.1.7"
   */
  public String getPoint() {
    return insPoint;
  } // end method */

  /**
   * Converts an instance into {@link String}.
   *
   * <p>For object identifier pre-defined in this class its {@code name} is returned. For other
   * object identifier instead of "UNKNOWN" the {@link #getPoint()}}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the return value is immutable.</i>
   * </ol>
   *
   * @return {@link #getName()}
   */
  @Override
  public String toString() {
    return insName;
  } // end method */
} // end class
