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

import static de.gematik.smartcards.crypto.AfiElcParameterSpec.brainpoolP256r1;
import static de.gematik.smartcards.crypto.AfiElcParameterSpec.brainpoolP384r1;
import static de.gematik.smartcards.crypto.AfiElcParameterSpec.brainpoolP512r1;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CAR;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CARDHOLDER_CERTIFICATE_TEMPLATE;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CED;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CERTIFICATE_CONTENT_TEMPLATE;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CHAT;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CHR;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CPI;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CXD;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_FLAG_LIST;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_PUK_TEMPLATE;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_SIGNATURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.crypto.AfiElcParameterSpec;
import de.gematik.smartcards.crypto.AfiElcUtils;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link Cvc}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.
//         Spotbugs message: Non-null field insTempDirectory is not initialized
//         This finding is for an attribute which is initialized by JUnit.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" // see note 1
}) // */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestCvc {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestCvc.class); // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Key for CPI. */
  /* package */ static final String KEY_CPI = "CPI"; // */

  /** Key for CAR. */
  /* package */ static final String KEY_CAR = "CAR"; // */

  /** Key for constructed DO with CHAT. */
  /* package */ static final String KEY_CHAT = "CHAT"; // */

  /** Key for CHR. */
  /* package */ static final String KEY_CHR = "CHR"; // */

  /** Key for CED. */
  /* package */ static final String KEY_CED = "CED"; // */

  /** Key for CXD. */
  /* package */ static final String KEY_CXD = "CXD"; // */

  /** Key for flag list. */
  /* package */ static final String KEY_FLAG_LIST = "flagList"; // */

  /** Key for constructed DO with message to be signed. */
  /* package */ static final String KEY_MESSAGE = "message"; // */

  /** Key for OID flag list interpretation. */
  /* package */ static final String KEY_OID_FLAG_LIST = "OidFlagList"; // */

  /** Key for OID pukUsage. */
  /* package */ static final String KEY_OID_PUK_USAGE = "OidPukUsage"; // */

  /** Key for public key. */
  /* package */ static final String KEY_PUK = "PuK"; // */

  /** Key for constructed DO with public key. */
  /* package */ static final String KEY_PUK_DO = "PuK-DO"; // */

  /** Key for signature. */
  /* package */ static final String KEY_SIGNATURE = "signature"; // */

  /** Set with keys for primitive DO in CVC. */
  private static final Set<String> KEYS_PRIMITIVE =
      Set.of(
          KEY_CPI,
          KEY_CAR,
          KEY_OID_PUK_USAGE,
          KEY_PUK,
          KEY_CHR,
          KEY_OID_FLAG_LIST,
          KEY_FLAG_LIST,
          KEY_CED,
          KEY_CXD,
          KEY_SIGNATURE); // */

  /** Set with all keys. */
  private static final Set<String> KEYS_ALL =
      Stream.of(KEYS_PRIMITIVE, Set.of(KEY_MESSAGE, KEY_PUK_DO, KEY_CHAT))
          .flatMap(Set::stream)
          .collect(Collectors.toSet()); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // intentionally empty
  } // end method */

  /** Method executed after other tests. */
  @AfterAll
  static void tearDownAfterClass() {
    // intentionally empty
  } // end method */

  /** Method executed before each test. */
  @BeforeEach
  void setUp() {
    // --- clear cache
    TrustCenter.clearCache();
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /*
   * Test method for {@link Cvc#Cvc(byte[])}.
   *
  @Test
  void test_Cvc__byteA() {
    // Note: This is a rather simple constructor. So we can be lazy here.

    // Test strategy:
    // --- a. constructed CVC, not added because signature verification key is missing
    // --- b. constructed CVC, added because signature verification key is present

    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.insCacheCvc.isEmpty());

    // --- a. constructed CVC, not added because signature verification key is missing
    { // DEGXX_8-6-02-20 / DEGXX_1-1-02-20 / 0009_80-276-88311-0000129008
      final ConstructedBerTlv input = (ConstructedBerTlv) BerTlv.getInstance(
          """
              7f21 81da
                7f4e 8193
                  5f29 01 70
                  42 08 4445475858110220
                  7f49 4b
                    06 06 2b2403050301
                    86 41 041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9
                            a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00
                  5f20 0c 000980276883110000129008
                  7f4c 13
                    06 08 2a8214004c048118
                    53 07 0000000000000c
                  5f25 06 020000050006
                  5f24 06 020500050005
                5f37 40 92564533191b57fffeea94520700c5b727f686ac4c223713ae6c4ea4aa0ef146
                        890df9165746fa7fc3172494af42c3ecc4ecf14bd40947929d7a63bd352bfb5d
              """
      );
      final Cvc dut = new Cvc(input.getEncoded());

      // Note: The sequence of checks hereafter use the alphabetic order of
      //       instance attributes.
      assertEquals("4445475858110220", dut.getCar());
      assertEquals("DEGXX_1-1-02-20", dut.getCarHumanReadable());
      assertEquals("020000050006", dut.getCed());
      assertEquals("06. Mai 2020", dut.getCedHumanReadable());
      assertEquals("000980276883110000129008", dut.getChr());
      assertEquals("0009_80-276-88311-0000129008", dut.getChrHumanReadable());
      assertEquals("70", Hex.toHexDigits(dut.insCpi.getValue()));
      assertFalse(dut.hasCriticalFindings());
      assertNotSame(input, dut.getCvc());
      assertEquals(input, dut.getCvc());
      assertEquals("020500050005", dut.getCxd());
      assertEquals("05. Mai 2025", dut.getCxdHumanReadable());
      assertTrue(dut.insDomainParameter.isPresent());
      assertEquals(AfiElcParameterSpec.brainpoolP256r1, dut.insDomainParameter.orElseThrow());
      assertEquals(
          String.format(
              """
                  Certificate Profile Indicator%s
                  Certificate Holder Reference      CHR = 000980276883110000129008
                         Discretionary data  = 0009
                         ICCSN               = 80276883110000129008
                           Major Industry ID = 80
                           Country Code      = 276
                           Issuer Identifier = 88311
                           Serial Number     = 0000129008
                  Certification Authority Reference CAR = 4445475858110220
                         CA-Identifier       = DEGXX
                         service indicator   = 1
                         discretionary data  = 1
                         algorithm reference = 02
                         generation year     = 20
                  Certificate Effective  Date       CED = 020000050006     => 06. Mai 2020
                  Certificate Expiration Date       CXD = 020500050005     => 05. Mai 2025
                  Usage of enclosed public key      OID%s
                  Public point as octet string      P   =%s
                         Domain parameter of public key = brainpoolP256r1
                         P = (xp, yp) = ('%s', '%s')
                  Interpretation of Flag-List       OID%s
                  List of access rights,      Flag-List = 0000000000000c
                         b0b1:  Endnutzer Zertifikat für Authentisierungszwecke
                         b52 => Sichere Signaturerstellungseinheit (SSEE)
                         b53 => Remote PIN Empfänger
                  """,
              "     CPI = 70 => self descriptive Card Verifiable Certificate", // CPI
              " = 2b2403050301     = {1 3 36 3 5 3 1} = authS_gemSpec-COS-G2_ecc-with-sha256",
              " 041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9a3"
                  + "6167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
              "1801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9",
              "a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
              " = 2a8214004c048118 = {1 2 276 0 76 4 152} = cvc_FlagList_TI"
          ).replaceAll("\n", LINE_SEPARATOR),
          dut.insExplanation
      );
      assertEquals("0000000000000c", Hex.toHexDigits(dut.insFlagList));
      // hashCode() intentionally NOT checked here
      assertEquals(
          input.getConstructed(TAG_CERTIFICATE_CONTENT_TEMPLATE).orElseThrow(),
          dut.insMessage
      );
      assertEquals("2a8214004c048118", Hex.toHexDigits(dut.insOidFlagList));
      assertTrue(dut.insOidFlagListInterpretation.isPresent());
      assertEquals(AfiOid.CVC_FlagList_TI, dut.insOidFlagListInterpretation.orElseThrow());
      assertEquals("2b2403050301", Hex.toHexDigits(dut.insOidPuk));
      assertTrue(dut.insOidPukUsage.isPresent());
      assertEquals(AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256, dut.insOidPukUsage.orElseThrow());
      assertEquals(
          new EcPublicKeyImpl(
              new ECPoint(
                  new BigInteger(
                      "1801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9",
                      16
                  ),
                  new BigInteger(
                      "a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
                      16
                  )
              ),
              AfiElcParameterSpec.brainpoolP256r1
          ),
          dut.getPublicKey()
      );
      assertEquals(
          "041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab"
              + "9a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
          Hex.toHexDigits(dut.insPuK)
      );
      assertEquals(
          List.of(
              "verification key was not found => signature could not be checked"
          ),
          dut.getReport()
      );
      assertEquals(
          "92564533191b57fffeea94520700c5b727f686ac4c223713ae6c4ea4aa0ef146"
              + "890df9165746fa7fc3172494af42c3ecc4ecf14bd40947929d7a63bd352bfb5d",
          Hex.toHexDigits(dut.insSignature)
      );
      assertEquals(Cvc.SIGNATURE_NO_PUBLIC_KEY, dut.getSignatureStatus());

      // Note: Hereafter is some derived information
      assertEquals(
          Hex.extractHexDigits(
              """
              7f4e 8193
                   5f29 01 70
                   42 08 4445475858110220
                   7f49 4b
                        06 06 2b2403050301
                        86 41 041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9
                                a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00
                   5f20 0c 000980276883110000129008
                   7f4c 13
                        06 08 2a8214004c048118
                        53 07 0000000000000c
                   5f25 06 020000050006
                   5f24 06 020500050005
              5f37 40 92564533191b57fffeea94520700c5b727f686ac4c223713ae6c4ea4aa0ef146
                      890df9165746fa7fc3172494af42c3ecc4ecf14bd40947929d7a63bd352bfb5d
              """
          ),
          Hex.toHexDigits(dut.getValueField())
      );
      assertTrue(dut.isEndEntity());
      assertFalse(dut.isSubCa());
      assertFalse(dut.isRootCa());
      // toString() intentionally not tested here

      // expectation: neither key nor CVC added to cache
      assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
      assertTrue(TrustCenter.CACHE_CVC.insCacheCvc.isEmpty());
    } // end --- a.

    // --- b. constructed CVC, added because signature verification key is present
    {
      // add public key: / DEGXX_8-2-02-14 / DEARX_1-1-02-14
      final String car = "4445415258110214";
      final EcPublicKeyImpl puk = new EcPublicKeyImpl(
          BerTlv.getInstance(
              """
                  7f49 4e
                     06 09 2b2403030208010107
                     86 41 045617061e38bb9e161bc6a5a291a391049c16a781e50d34fd71dd5469feeae03e
                             2d5886c6dd90bc4a1d2caad1054b3da52cc5674e95edc76ff29fced69b5720d4
                  """
          ),
          EafiElcPukFormat.ISOIEC7816
      );
      TrustCenter.CACHE_PUK.add(car, puk);
      assertEquals(1, TrustCenter.CACHE_PUK.insCache.size());

      // add CVC: / DEGXX_8-2-02-14 / DEARX_1-1-02-14 / 000a_80-276-88311-0000003345
      final ConstructedBerTlv input = (ConstructedBerTlv) BerTlv.getInstance(
          """
              7f21 81da
              |  7f4e 8193
              |  |  5f29 01 70
              |  |  42 08 4445415258110214
              |  |  7f49 4b
              |  |  |  06 06 2b2403050301
              |  |  |  86 41 040eb7abc450f819eaf84bde147f247aa80bb7009920f457e4f2cb6ead6a6ae68d
                               2695823a47ee2244b426c1fdb8b1b0005fed04214eb70cfcb6bef13fd0b694e9
              |  |  5f20 0c 000a80276883110000003345
              |  |  7f4c 13
              |  |  |  06 08 2a8214004c048118
              |  |  |  53 07 00000000000001
              |  |  5f25 06 010401020100
              |  |  5f24 06 010901020007
              |  5f37 40 014c9805488b04174ce7a8d99430d97a56fd8fda7d7ac353de915605351cd188
                         7f5d2b2939007cafd78a64304dc4bb4f9945a27dd75c569d2f603a05d273e6c0
              """
      );
      final Cvc dut = new Cvc(input.getEncoded());

      // Note: The sequence of checks hereafter use the alphabetic order of
      //       instance attributes.
      assertEquals("4445415258110214", dut.getCar());
      assertEquals("DEARX_1-1-02-14", dut.getCarHumanReadable());
      assertEquals("010401020100", dut.getCed());
      assertEquals("10. Dezember 2014", dut.getCedHumanReadable());
      assertEquals("000a80276883110000003345", dut.getChr());
      assertEquals("000a_80-276-88311-0000003345", dut.getChrHumanReadable());
      assertEquals("70", Hex.toHexDigits(dut.insCpi.getValue()));
      assertFalse(dut.hasCriticalFindings());
      assertNotSame(input, dut.getCvc());
      assertEquals(input, dut.getCvc());
      assertEquals("010901020007", dut.getCxd());
      assertEquals("07. Dezember 2019", dut.getCxdHumanReadable());
      assertTrue(dut.insDomainParameter.isPresent());
      assertEquals(AfiElcParameterSpec.brainpoolP256r1, dut.insDomainParameter.orElseThrow());
      assertEquals(
          String.format(
              """
                  Certificate Profile Indicator%s
                  Certificate Holder Reference      CHR = 000a80276883110000003345
                         Discretionary data  = 000a
                         ICCSN               = 80276883110000003345
                           Major Industry ID = 80
                           Country Code      = 276
                           Issuer Identifier = 88311
                           Serial Number     = 0000003345
                  Certification Authority Reference CAR = 4445415258110214
                         CA-Identifier       = DEARX
                         service indicator   = 1
                         discretionary data  = 1
                         algorithm reference = 02
                         generation year     = 14
                  Certificate Effective  Date       CED = 010401020100     => 10. Dezember 2014
                  Certificate Expiration Date       CXD = 010901020007     => 07. Dezember 2019
                  Usage of enclosed public key      OID%s
                  Public point as octet string      P   =%s
                         Domain parameter of public key = brainpoolP256r1
                         P = (xp, yp) = ('%s', '%s')
                  Interpretation of Flag-List       OID%s
                  List of access rights,      Flag-List = 00000000000001
                         b0b1:  Endnutzer Zertifikat für Authentisierungszwecke
                         b55 => SAK für Stapel- oder Komfortsignatur
                  """,
              "     CPI = 70 => self descriptive Card Verifiable Certificate", // CPI
              " = 2b2403050301     = {1 3 36 3 5 3 1} = authS_gemSpec-COS-G2_ecc-with-sha256",
              " 040eb7abc450f819eaf84bde147f247aa80bb7009920f457e4f2cb6ead6a6ae68d26"
                  + "95823a47ee2244b426c1fdb8b1b0005fed04214eb70cfcb6bef13fd0b694e9",
              "0eb7abc450f819eaf84bde147f247aa80bb7009920f457e4f2cb6ead6a6ae68d", // xp
              "2695823a47ee2244b426c1fdb8b1b0005fed04214eb70cfcb6bef13fd0b694e9", // yp
              " = 2a8214004c048118 = {1 2 276 0 76 4 152} = cvc_FlagList_TI"
          ).replaceAll("\n", LINE_SEPARATOR),
          dut.insExplanation
      );
      assertEquals("00000000000001", Hex.toHexDigits(dut.insFlagList));
      // hashCode() intentionally NOT checked here
      assertEquals(
          input.getConstructed(TAG_CERTIFICATE_CONTENT_TEMPLATE).orElseThrow(),
          dut.insMessage
      );
      assertEquals("2a8214004c048118", Hex.toHexDigits(dut.insOidFlagList));
      assertTrue(dut.insOidFlagListInterpretation.isPresent());
      assertEquals(AfiOid.CVC_FlagList_TI, dut.insOidFlagListInterpretation.orElseThrow());
      assertEquals("2b2403050301", Hex.toHexDigits(dut.insOidPuk));
      assertTrue(dut.insOidPukUsage.isPresent());
      assertEquals(AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256, dut.insOidPukUsage.orElseThrow());
      assertEquals(
          new EcPublicKeyImpl(
              new ECPoint(
                  new BigInteger(
                      "0eb7abc450f819eaf84bde147f247aa80bb7009920f457e4f2cb6ead6a6ae68d",
                      16
                  ),
                  new BigInteger(
                      "2695823a47ee2244b426c1fdb8b1b0005fed04214eb70cfcb6bef13fd0b694e9",
                      16
                  )
              ),
              AfiElcParameterSpec.brainpoolP256r1
          ),
          dut.getPublicKey()
      );
      assertEquals(
          "040eb7abc450f819eaf84bde147f247aa80bb7009920f457e4f2cb6ead6a6ae68d"
              + "2695823a47ee2244b426c1fdb8b1b0005fed04214eb70cfcb6bef13fd0b694e9",
          Hex.toHexDigits(dut.insPuK)
      );
      assertTrue(dut.getReport().isEmpty());
      assertEquals(
          "014c9805488b04174ce7a8d99430d97a56fd8fda7d7ac353de915605351cd1887f5"
              + "d2b2939007cafd78a64304dc4bb4f9945a27dd75c569d2f603a05d273e6c0",
          Hex.toHexDigits(dut.insSignature)
      );
      assertEquals(Cvc.SIGNATURE_VALID, dut.getSignatureStatus());

      // Note: Hereafter is some derived information
      assertEquals(
          Hex.extractHexDigits(
              """
                  7f4e 8193
                  |  5f29 01 70
                  |  42 08 4445415258110214
                  |  7f49 4b
                  |  |  06 06 2b2403050301
                  |  |  86 41 040eb7abc450f819eaf84bde147f247aa80bb7009920f457e4f2cb6ead6a6ae68d
                                2695823a47ee2244b426c1fdb8b1b0005fed04214eb70cfcb6bef13fd0b694e9
                  |  5f20 0c 000a80276883110000003345
                  |  7f4c 13
                  |  |  06 08 2a8214004c048118
                  |  |  53 07 00000000000001
                  |  5f25 06 010401020100
                  |  5f24 06 010901020007
                  5f37 40 014c9805488b04174ce7a8d99430d97a56fd8fda7d7ac353de915605351cd188
                          7f5d2b2939007cafd78a64304dc4bb4f9945a27dd75c569d2f603a05d273e6c0
                  """
          ),
          Hex.toHexDigits(dut.getValueField())
      );
      assertTrue(dut.isEndEntity());
      assertFalse(dut.isSubCa());
      assertFalse(dut.isRootCa());
      // toString() intentionally not tested here

      // check content of caches in TrustCenter
      assertEquals(2, TrustCenter.CACHE_PUK.insCache.size());
      assertEquals(
          Map.ofEntries(
              Map.entry(car, puk),
              Map.entry(dut.getChr(), dut.getPublicKey())
          ),
          TrustCenter.CACHE_PUK.insCache
      );
      assertEquals(1, TrustCenter.CACHE_CVC.insCacheCvc.size());
      assertEquals(
          Set.of(dut),
          TrustCenter.CACHE_CVC.insCacheCvc
      );
    } // end --- b.
  } // end method */

  /*
   * Test method for {@link Cvc#Cvc(ConstructedBerTlv)}.
   *
  @Test
  void test_Cvc__ConstructedBerTlv() {
    // Note 1: The constructor is a bit complex. Thus, here we concentrate on
    //         some happy cases. Furthermore, it is checked if extra DO are
    //         ignored. The sequence within a constructed DO SHALL not matter.
    // Note 2: Detailed testing of DO and corner cases are subject to getter
    //         test methods. There it is also checked if missing DO is detected.

    // Test strategy:
    // --- a. smoke test with end entity CV-certificate
    // --- b. smoke test with CVC-Sub-CA certificate
    // --- c. smoke test with self-signed CVC-Root-CA
    // --- d. smoke test with link certificate
    // --- e. check if extra DO are ignored and sequence does not matter

    // --- a. smoke test with end entity CV-certificate
    { // DEGXX_8-6-02-20 / DEGXX_1-1-02-20 / 0009_80-276-88311-0000129008
      final ConstructedBerTlv input = (ConstructedBerTlv) BerTlv.getInstance(
          """
              7f21 81da
                7f4e 8193
                  5f29 01 70
                  42 08 4445475858110220
                  7f49 4b
                    06 06 2b2403050301
                    86 41 041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9
                            a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00
                  5f20 0c 000980276883110000129008
                  7f4c 13
                    06 08 2a8214004c048118
                    53 07 0000000000000c
                  5f25 06 020000050006
                  5f24 06 020500050005
                5f37 40 92564533191b57fffeea94520700c5b727f686ac4c223713ae6c4ea4aa0ef146
                        890df9165746fa7fc3172494af42c3ecc4ecf14bd40947929d7a63bd352bfb5d
              """
      );
      final Cvc dut = new Cvc(input);

      // Note: The sequence of checks hereafter use the alphabetic order of
      //       instance attributes.
      assertEquals("4445475858110220", dut.getCar());
      assertEquals("DEGXX_1-1-02-20", dut.getCarHumanReadable());
      assertEquals("020000050006", dut.getCed());
      assertEquals("06. Mai 2020", dut.getCedHumanReadable());
      assertEquals("000980276883110000129008", dut.getChr());
      assertEquals("0009_80-276-88311-0000129008", dut.getChrHumanReadable());
      assertEquals("70", Hex.toHexDigits(dut.insCpi.getValue()));
      assertFalse(dut.hasCriticalFindings());
      assertSame(input, dut.getCvc());
      assertEquals("020500050005", dut.getCxd());
      assertEquals("05. Mai 2025", dut.getCxdHumanReadable());
      assertTrue(dut.insDomainParameter.isPresent());
      assertEquals(AfiElcParameterSpec.brainpoolP256r1, dut.insDomainParameter.orElseThrow());
      assertEquals(
          String.format(
          """
              Certificate Profile Indicator%s
              Certificate Holder Reference      CHR = 000980276883110000129008
                     Discretionary data  = 0009
                     ICCSN               = 80276883110000129008
                       Major Industry ID = 80
                       Country Code      = 276
                       Issuer Identifier = 88311
                       Serial Number     = 0000129008
              Certification Authority Reference CAR = 4445475858110220
                     CA-Identifier       = DEGXX
                     service indicator   = 1
                     discretionary data  = 1
                     algorithm reference = 02
                     generation year     = 20
              Certificate Effective  Date       CED = 020000050006     => 06. Mai 2020
              Certificate Expiration Date       CXD = 020500050005     => 05. Mai 2025
              Usage of enclosed public key      OID%s
              Public point as octet string      P   =%s
                     Domain parameter of public key = brainpoolP256r1
                     P = (xp, yp) = ('%s', '%s')
              Interpretation of Flag-List       OID%s
              List of access rights,      Flag-List = 0000000000000c
                     b0b1:  Endnutzer Zertifikat für Authentisierungszwecke
                     b52 => Sichere Signaturerstellungseinheit (SSEE)
                     b53 => Remote PIN Empfänger
              """,
              "     CPI = 70 => self descriptive Card Verifiable Certificate", // CPI
              " = 2b2403050301     = {1 3 36 3 5 3 1} = authS_gemSpec-COS-G2_ecc-with-sha256",
              " 041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9a3"
                  + "6167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
              "1801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9",
              "a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
              " = 2a8214004c048118 = {1 2 276 0 76 4 152} = cvc_FlagList_TI"
          ).replaceAll("\n", LINE_SEPARATOR),
          dut.insExplanation
      );
      assertEquals("0000000000000c", Hex.toHexDigits(dut.insFlagList));
      // hashCode() intentionally NOT checked here
      assertEquals(
          input.getConstructed(TAG_CERTIFICATE_CONTENT_TEMPLATE).orElseThrow(),
          dut.insMessage
      );
      assertEquals("2a8214004c048118", Hex.toHexDigits(dut.insOidFlagList));
      assertTrue(dut.insOidFlagListInterpretation.isPresent());
      assertEquals(AfiOid.CVC_FlagList_TI, dut.insOidFlagListInterpretation.orElseThrow());
      assertEquals("2b2403050301", Hex.toHexDigits(dut.insOidPuk));
      assertTrue(dut.insOidPukUsage.isPresent());
      assertEquals(AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256, dut.insOidPukUsage.orElseThrow());
      assertEquals(
          new EcPublicKeyImpl(
              new ECPoint(
                  new BigInteger(
                      "1801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9",
                      16
                  ),
                  new BigInteger(
                      "a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
                      16
                  )
              ),
              AfiElcParameterSpec.brainpoolP256r1
          ),
          dut.getPublicKey()
      );
      assertEquals(
          "041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab"
              + "9a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
          Hex.toHexDigits(dut.insPuK)
      );
      assertEquals(
          List.of(
              "verification key was not found => signature could not be checked"
          ),
          dut.getReport()
      );
      assertEquals(
          "92564533191b57fffeea94520700c5b727f686ac4c223713ae6c4ea4aa0ef146"
              + "890df9165746fa7fc3172494af42c3ecc4ecf14bd40947929d7a63bd352bfb5d",
          Hex.toHexDigits(dut.insSignature)
      );
      assertEquals(Cvc.SIGNATURE_NO_PUBLIC_KEY, dut.getSignatureStatus());

      // Note: Hereafter is some derived information
      assertEquals(
          Hex.extractHexDigits(
              """
              7f4e 8193
                   5f29 01 70
                   42 08 4445475858110220
                   7f49 4b
                        06 06 2b2403050301
                        86 41 041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9
                                a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00
                   5f20 0c 000980276883110000129008
                   7f4c 13
                        06 08 2a8214004c048118
                        53 07 0000000000000c
                   5f25 06 020000050006
                   5f24 06 020500050005
              5f37 40 92564533191b57fffeea94520700c5b727f686ac4c223713ae6c4ea4aa0ef146
                      890df9165746fa7fc3172494af42c3ecc4ecf14bd40947929d7a63bd352bfb5d
              """
          ),
          Hex.toHexDigits(dut.getValueField())
      );
      assertTrue(dut.isEndEntity());
      assertFalse(dut.isSubCa());
      assertFalse(dut.isRootCa());
      // toString() intentionally not tested here
    } // end --- a.

    // --- b. smoke test with CVC-Sub-CA certificate
    { // DEZGW_8-2-02-16 / DEATO_1-0-02-16
      final ConstructedBerTlv input = (ConstructedBerTlv) BerTlv.getInstance(
          """
             7f21 81d8
                7f4e 8191
                   5f29 01 70
                   42 08 44455a4757820216
                   7f49 4d
                      06 08 2a8648ce3d040302
                      86 41 04714f876bc50843da884ea840335e4be5d1d7736174d101a1d6b9c08d019d07a0
                              1ab1688a6ce0267299424046811bb263206c18f7eb923d274af789b75c14e205
                   5f20 08 444541544f100216
                   7f4c 13
                      06 08 2a8214004c048118
                      53 07 80000000000000
                   5f25 06 010600070108
                   5f24 06 020400070107
                5f37 40 61aea1b93eee7432db0c339ac0fadb9f8a5434549330bba896220fd695c9a208
                        1aec06245406d2682883c999378dea616584b3b2dd894c9b14dd79882a65d4bc
             """
      );
      final Cvc dut = new Cvc(input);

      // Note: The sequence of checks hereafter use the alphabetic order of
      //       instance attributes.
      assertEquals("44455a4757820216", dut.getCar());
      assertEquals("DEZGW_8-2-02-16", dut.getCarHumanReadable());
      assertEquals("010600070108", dut.getCed());
      assertEquals("18. Juli 2016", dut.getCedHumanReadable());
      assertEquals("444541544f100216", dut.getChr());
      assertEquals("DEATO_1-0-02-16", dut.getChrHumanReadable());
      assertEquals("70", Hex.toHexDigits(dut.insCpi.getValue()));
      assertFalse(dut.hasCriticalFindings());
      assertSame(input, dut.getCvc());
      assertEquals("020400070107", dut.getCxd());
      assertEquals("17. Juli 2024", dut.getCxdHumanReadable());
      assertTrue(dut.insDomainParameter.isPresent());
      assertEquals(AfiElcParameterSpec.brainpoolP256r1, dut.insDomainParameter.orElseThrow());
      assertEquals(
          String.format(
              """
                  Certificate Profile Indicator%s
                  Certificate Holder Reference      CHR = 444541544f100216
                         CA-Identifier       = DEATO
                         service indicator   = 1
                         discretionary data  = 0
                         algorithm reference = 02
                         generation year     = 16
                  Certification Authority Reference CAR = 44455a4757820216
                         CA-Identifier       = DEZGW
                         service indicator   = 8
                         discretionary data  = 2
                         algorithm reference = 02
                         generation year     = 16
                  Certificate Effective  Date       CED = 010600070108     => 18. Juli 2016
                  Certificate Expiration Date       CXD = 020400070107     => 17. Juli 2024
                  Usage of enclosed public key      OID%s
                  Public point as octet string      P   =%s
                         Domain parameter of public key = brainpoolP256r1
                         P = (xp, yp) = ('%s', '%s')
                  Interpretation of Flag-List       OID%s
                  List of access rights,      Flag-List = 80000000000000
                         b0b1:  Zertifikat einer Sub-CA
                  """,
              "     CPI = 70 => self descriptive Card Verifiable Certificate", // CPI
              " = 2a8648ce3d040302 = {1 2 840 10045 4 3 2} = ecdsa-with-SHA256",
              " 04714f876bc50843da884ea840335e4be5d1d7736174d101a1d6b9c08d019d07a01a"
                  + "b1688a6ce0267299424046811bb263206c18f7eb923d274af789b75c14e205",
              "714f876bc50843da884ea840335e4be5d1d7736174d101a1d6b9c08d019d07a0",
              "1ab1688a6ce0267299424046811bb263206c18f7eb923d274af789b75c14e205",
              " = 2a8214004c048118 = {1 2 276 0 76 4 152} = cvc_FlagList_TI"
          ).replaceAll("\n", LINE_SEPARATOR),
          dut.insExplanation
      );
      assertEquals("80000000000000", Hex.toHexDigits(dut.insFlagList));
      // hashCode() intentionally NOT checked here
      assertEquals(
          input.getConstructed(TAG_CERTIFICATE_CONTENT_TEMPLATE).orElseThrow(),
          dut.insMessage
      );
      assertEquals("2a8214004c048118", Hex.toHexDigits(dut.insOidFlagList));
      assertTrue(dut.insOidFlagListInterpretation.isPresent());
      assertEquals(AfiOid.CVC_FlagList_TI, dut.insOidFlagListInterpretation.orElseThrow());
      assertEquals("2a8648ce3d040302", Hex.toHexDigits(dut.insOidPuk));
      assertTrue(dut.insOidPukUsage.isPresent());
      assertEquals(AfiOid.ECDSA_with_SHA256, dut.insOidPukUsage.orElseThrow());
      assertEquals(
          new EcPublicKeyImpl(
              new ECPoint(
                  new BigInteger(
                      "714f876bc50843da884ea840335e4be5d1d7736174d101a1d6b9c08d019d07a0",
                      16
                  ),
                  new BigInteger(
                      "1ab1688a6ce0267299424046811bb263206c18f7eb923d274af789b75c14e205",
                      16
                  )
              ),
              AfiElcParameterSpec.brainpoolP256r1
          ),
          dut.getPublicKey()
      );
      assertEquals(
          "04714f876bc50843da884ea840335e4be5d1d7736174d101a1d6b9c08d019d07a01a"
              + "b1688a6ce0267299424046811bb263206c18f7eb923d274af789b75c14e205",
          Hex.toHexDigits(dut.insPuK)
      );
      assertEquals(
          List.of(
              "verification key was not found => signature could not be checked"
          ),
          dut.getReport()
      );
      assertEquals(
          "61aea1b93eee7432db0c339ac0fadb9f8a5434549330bba896220fd695c9a208"
              + "1aec06245406d2682883c999378dea616584b3b2dd894c9b14dd79882a65d4bc",
          Hex.toHexDigits(dut.insSignature)
      );
      assertEquals(Cvc.SIGNATURE_NO_PUBLIC_KEY, dut.getSignatureStatus());

      // Note: Hereafter is some derived information
      assertEquals(
          Hex.extractHexDigits(
              """
                 7f4e 8191
                    5f29 01 70
                    42 08 44455a4757820216
                    7f49 4d
                       06 08 2a8648ce3d040302
                       86 41 04714f876bc50843da884ea840335e4be5d1d7736174d101a1d6b9c08d019d07a0
                               1ab1688a6ce0267299424046811bb263206c18f7eb923d274af789b75c14e205
                    5f20 08 444541544f100216
                    7f4c 13
                       06 08 2a8214004c048118
                       53 07 80000000000000
                    5f25 06 010600070108
                    5f24 06 020400070107
                 5f37 40 61aea1b93eee7432db0c339ac0fadb9f8a5434549330bba896220fd695c9a208
                         1aec06245406d2682883c999378dea616584b3b2dd894c9b14dd79882a65d4bc
                  """
          ),
          Hex.toHexDigits(dut.getValueField())
      );
      assertFalse(dut.isEndEntity());
      assertTrue(dut.isSubCa());
      assertFalse(dut.isRootCa());
      // toString() intentionally not tested here
    } // end --- b.

    // --- c. smoke test with self-signed CVC-Root-CA
    { // DEZGW_8-4-02-20
      final ConstructedBerTlv input = (ConstructedBerTlv) BerTlv.getInstance(
          """
              7f21 81d8
              |  7f4e 8191
              |  |  5f29 01 70
              |  |  42 08 44455a4757840220
              |  |  7f49 4d
              |  |  |  06 08 2a8648ce3d040302
              |  |  |  86 41 0400c6ebb5b22db0b11bbc632e173b94f42d81e90d8a1b0d6d73bda38974065164
                               9a694ade782bd577b8bec6dffad37e14503599896bf4e7a739a34d9d8d783a3e
              |  |  5f20 08 44455a4757840220
              |  |  7f4c 13
              |  |  |  06 08 2a8214004c048118
              |  |  |  53 07 ffffffffffffff
              |  |  5f25 06 020000050103
              |  |  5f24 06 030000050102
              |  5f37 40 7a2f28238295dfdb70c3f999d31f2de733b7df3e83cd3f69b662ec17e5a6df9a
                         53a2cfe7fc9b8aa08f25c21ac63783154ba82199043284ab66774fdf975322ca
              """
      );
      final Cvc dut = new Cvc(input);

      // Note: The sequence of checks hereafter use the alphabetic order of
      //       instance attributes.
      assertEquals("44455a4757840220", dut.getCar());
      assertEquals("DEZGW_8-4-02-20", dut.getCarHumanReadable());
      assertEquals("020000050103", dut.getCed());
      assertEquals("13. Mai 2020", dut.getCedHumanReadable());
      assertEquals("44455a4757840220", dut.getChr());
      assertEquals("DEZGW_8-4-02-20", dut.getChrHumanReadable());
      assertEquals("70", Hex.toHexDigits(dut.insCpi.getValue()));
      assertFalse(dut.hasCriticalFindings());
      assertSame(input, dut.getCvc());
      assertEquals("030000050102", dut.getCxd());
      assertEquals("12. Mai 2030", dut.getCxdHumanReadable());
      assertTrue(dut.insDomainParameter.isPresent());
      assertEquals(AfiElcParameterSpec.brainpoolP256r1, dut.insDomainParameter.orElseThrow());
      assertEquals(
          String.format(
              """
                  Certificate Profile Indicator%s
                  Certificate Holder Reference      CHR = 44455a4757840220
                         CA-Identifier       = DEZGW
                         service indicator   = 8
                         discretionary data  = 4
                         algorithm reference = 02
                         generation year     = 20
                  Certification Authority Reference CAR = 44455a4757840220
                         CA-Identifier       = DEZGW
                         service indicator   = 8
                         discretionary data  = 4
                         algorithm reference = 02
                         generation year     = 20
                  Certificate Effective  Date       CED = 020000050103     => 13. Mai 2020
                  Certificate Expiration Date       CXD = 030000050102     => 12. Mai 2030
                  Usage of enclosed public key      OID%s
                  Public point as octet string      P   =%s
                         Domain parameter of public key = brainpoolP256r1
                         P = (xp, yp) = ('%s', '%s')
                  Interpretation of Flag-List       OID%s
                  List of access rights,      Flag-List = ffffffffffffff
                         b0b1:  Link-Zertifikat einer Root-CA
                         b02 => RFU
                         b03 => RFU
                         b04 => RFU
                         b05 => RFU
                         b06 => RFU
                         b07 => RFU
                         b08 => eGK: Verwendung der ESIGN-AUTN Funktionalität mit PIN.CH
                         b09 => eGK: Verwendung der ESIGN-AUTN Funktionalität ohne PIN
                         b10 => eGK: Verwendung der ESIGN-ENCV Funktionalität mit PIN.CH
                         b11 => eGK: Verwendung der ESIGN-ENCV Funktionalität ohne PIN
                         b12 => eGK: Verwendung der ESIGN-AUT Funktionalität
                         b13 => eGK: Verwendung der ESIGN-ENC Funktionalität
                         b14 => eGK: Notfalldatensatz verbergen und sichtbar machen
                         b15%s
                         b16 => RFU
                         b17 => eGK: Notfalldatensatz lesen mit MRPIN.NFD
                         b18 => eGK: Notfalldatensatz lesen ohne PIN
                         b19 => eGK: Persönliche Erklärungen (DPE) verbergen und sichtbar machen
                         b20 => eGK: DPE schreiben, löschen (hier 'erase', nicht 'delete')
                         b21 => RFU
                         b22 => eGK: DPE lesen mit MRPIN.DPE_Read
                         b23 => eGK: DPE lesen ohne PIN
                         b24%s
                         b25%s
                         b26 => RFU
                         b27 => eGK: Einwilligungen im DF.HCA schreiben
                         b28 => eGK: Verweise im DF.HCA lesen und schreiben
                         b29 => eGK: Geschützte Versichertendaten lesen mit PIN.CH
                         b30 => eGK: Geschützte Versichertendaten lesen ohne PIN
                         b31 => eGK: Loggingdaten schreiben mit PIN.CH
                         b32 => eGK: Loggingdaten schreiben ohne PIN
                         b33 => eGK: Zugriff in den AdV-Umgebungen
                         b34 => eGK: Prüfungsnachweis lesen und schreiben
                         b35 => RFU
                         b36 => RFU
                         b37 => RFU
                         b38 => RFU
                         b39 => eGK: Gesundheitsdatendienste verbergen und sichtbar machen
                         b40%s
                         b41 => eGK: Organspendedatensatz lesen mit MRPIN.OSE
                         b42 => eGK: Organspendedatensatz lesen ohne PIN
                         b43%s
                         b44 => eGK: Organspendedatensatz verbergen und sichtbar machen
                         b45 => eGK: AMTS-Datensatz verbergen und sichtbar machen
                         b46 => eGK: AMTS-Datensatz lesen
                         b47%s
                         b48 => RFU
                         b49 => RFU
                         b50 => RFU
                         b51 => Auslöser Komfortsignatur
                         b52 => Sichere Signaturerstellungseinheit (SSEE)
                         b53 => Remote PIN Empfänger
                         b54 => Remote PIN Sender
                         b55 => SAK für Stapel- oder Komfortsignatur
                  """,
              "     CPI = 70 => self descriptive Card Verifiable Certificate", // CPI
              " = 2a8648ce3d040302 = {1 2 840 10045 4 3 2} = ecdsa-with-SHA256",
              " 0400c6ebb5b22db0b11bbc632e173b94f42d81e90d8a1b0d6d73bda389740651649a"
                  + "694ade782bd577b8bec6dffad37e14503599896bf4e7a739a34d9d8d783a3e",
              "00c6ebb5b22db0b11bbc632e173b94f42d81e90d8a1b0d6d73bda38974065164", // xp
              "9a694ade782bd577b8bec6dffad37e14503599896bf4e7a739a34d9d8d783a3e", // yp
              " = 2a8214004c048118 = {1 2 276 0 76 4 152} = cvc_FlagList_TI",
              " => eGK: Notfalldatensatz schreiben, löschen (hier 'erase', nicht 'delete')",  // b15
              " => eGK: Einwilligungen und Verweise im DF.HCA verbergen und sichtbar machen", // b24
              " => eGK: Einwilligungen im DF.HCA lesen und löschen (hier 'erase', nicht 'delete')",
              " => eGK: Gesundheitsdatendienste lesen, schreiben und löschen (hier 'erase')", // b40
              " => eGK: Organspendedatensatz schreiben, löschen (hier 'erase', nicht 'delete')",
              " => eGK: AMTS-Datensatz schreiben, löschen (hier „erase“, nicht „delete“)"     // b47
          ).replaceAll("\n", LINE_SEPARATOR),
          dut.insExplanation
      );
      assertEquals("ffffffffffffff", Hex.toHexDigits(dut.insFlagList));
      // hashCode() intentionally NOT checked here
      assertEquals(
          input.getConstructed(TAG_CERTIFICATE_CONTENT_TEMPLATE).orElseThrow(),
          dut.insMessage
      );
      assertEquals("2a8214004c048118", Hex.toHexDigits(dut.insOidFlagList));
      assertTrue(dut.insOidFlagListInterpretation.isPresent());
      assertEquals(AfiOid.CVC_FlagList_TI, dut.insOidFlagListInterpretation.orElseThrow());
      assertEquals("2a8648ce3d040302", Hex.toHexDigits(dut.insOidPuk));
      assertTrue(dut.insOidPukUsage.isPresent());
      assertEquals(AfiOid.ECDSA_with_SHA256, dut.insOidPukUsage.orElseThrow());
      assertEquals(
          new EcPublicKeyImpl(
              new ECPoint(
                  new BigInteger(
                      "00c6ebb5b22db0b11bbc632e173b94f42d81e90d8a1b0d6d73bda38974065164",
                      16
                  ),
                  new BigInteger(
                      "9a694ade782bd577b8bec6dffad37e14503599896bf4e7a739a34d9d8d783a3e",
                      16
                  )
              ),
              AfiElcParameterSpec.brainpoolP256r1
          ),
          dut.getPublicKey()
      );
      assertEquals(
          "0400c6ebb5b22db0b11bbc632e173b94f42d81e90d8a1b0d6d73bda389740651649a"
              + "694ade782bd577b8bec6dffad37e14503599896bf4e7a739a34d9d8d783a3e",
          Hex.toHexDigits(dut.insPuK)
      );
      assertEquals(
          List.of(
              "verification key was not found => signature could not be checked"
          ),
          dut.getReport()
      );
      assertEquals(
          "7a2f28238295dfdb70c3f999d31f2de733b7df3e83cd3f69b662ec17e5a6df9a53a"
              + "2cfe7fc9b8aa08f25c21ac63783154ba82199043284ab66774fdf975322ca",
          Hex.toHexDigits(dut.insSignature)
      );
      assertEquals(Cvc.SIGNATURE_NO_PUBLIC_KEY, dut.getSignatureStatus());

      // Note: Hereafter is some derived information
      assertEquals(
          Hex.extractHexDigits(
              """
                  7f4e 8191
                  |  5f29 01 70
                  |  42 08 44455a4757840220
                  |  7f49 4d
                  |  |  06 08 2a8648ce3d040302
                  |  |  86 41 0400c6ebb5b22db0b11bbc632e173b94f42d81e90d8a1b0d6d73bda38974065164
                                9a694ade782bd577b8bec6dffad37e14503599896bf4e7a739a34d9d8d783a3e
                  |  5f20 08 44455a4757840220
                  |  7f4c 13
                  |  |  06 08 2a8214004c048118
                  |  |  53 07 ffffffffffffff
                  |  5f25 06 020000050103
                  |  5f24 06 030000050102
                  5f37 40 7a2f28238295dfdb70c3f999d31f2de733b7df3e83cd3f69b662ec17e5a6df9a
                          53a2cfe7fc9b8aa08f25c21ac63783154ba82199043284ab66774fdf975322ca
                  """
          ),
          Hex.toHexDigits(dut.getValueField())
      );
      assertFalse(dut.isEndEntity());
      assertFalse(dut.isSubCa());
      assertTrue(dut.isRootCa());
      // toString() intentionally not tested here
    } // end --- c.

    // --- d. smoke test with link certificate
    { // DEZGW_8-1-02-14 / DEZGW_8-2-02-16
      final ConstructedBerTlv input = (ConstructedBerTlv) BerTlv.getInstance(
          """
              7f21 81d8
              |  7f4e 8191
              |  |  5f29 01 70
              |  |  42 08 44455a4757810214
              |  |  7f49 4d
              |  |  |  06 08 2a8648ce3d040302
              |  |  |  86 41 04a42ee03e1e077b5db4dc347d3e22ce02ac3f44f0ad583ecb2f57e69ec96089da
                               78b619056e17932fe64b1b41e21c05ee546d2909dc357e35612e1a2479c10d55
              |  |  5f20 08 44455a4757820216
              |  |  7f4c 13
              |  |  |  06 08 2a8214004c048118
              |  |  |  53 07 ffffffffffffff
              |  |  5f25 06 010600060007
              |  |  5f24 06 020600060006
              |  5f37 40 3660c95a1de68819966d08f17039f31f6152ea0b518d4b0f55c4b83c9b20ebb9
                         2ba7a7a4009bafd2bb01160a1b24dff2923e55df64e036d84e62f553e150fbf6
              """
      );
      final Cvc dut = new Cvc(input);

      // Note: The sequence of checks hereafter use the alphabetic order of
      //       instance attributes.
      assertEquals("44455a4757810214", dut.getCar());
      assertEquals("DEZGW_8-1-02-14", dut.getCarHumanReadable());
      assertEquals("010600060007", dut.getCed());
      assertEquals("07. Juni 2016", dut.getCedHumanReadable());
      assertEquals("44455a4757820216", dut.getChr());
      assertEquals("DEZGW_8-2-02-16", dut.getChrHumanReadable());
      assertEquals("70", Hex.toHexDigits(dut.insCpi.getValue()));
      assertFalse(dut.hasCriticalFindings());
      assertSame(input, dut.getCvc());
      assertEquals("020600060006", dut.getCxd());
      assertEquals("06. Juni 2026", dut.getCxdHumanReadable());
      assertTrue(dut.insDomainParameter.isPresent());
      assertEquals(AfiElcParameterSpec.brainpoolP256r1, dut.insDomainParameter.orElseThrow());
      assertEquals(
          String.format(
              """
                  Certificate Profile Indicator%s
                  Certificate Holder Reference      CHR = 44455a4757820216
                         CA-Identifier       = DEZGW
                         service indicator   = 8
                         discretionary data  = 2
                         algorithm reference = 02
                         generation year     = 16
                  Certification Authority Reference CAR = 44455a4757810214
                         CA-Identifier       = DEZGW
                         service indicator   = 8
                         discretionary data  = 1
                         algorithm reference = 02
                         generation year     = 14
                  Certificate Effective  Date       CED = 010600060007     => 07. Juni 2016
                  Certificate Expiration Date       CXD = 020600060006     => 06. Juni 2026
                  Usage of enclosed public key      OID%s
                  Public point as octet string      P   =%s
                         Domain parameter of public key = brainpoolP256r1
                         P = (xp, yp) = ('%s', '%s')
                  Interpretation of Flag-List       OID%s
                  List of access rights,      Flag-List = ffffffffffffff
                         b0b1:  Link-Zertifikat einer Root-CA
                         b02 => RFU
                         b03 => RFU
                         b04 => RFU
                         b05 => RFU
                         b06 => RFU
                         b07 => RFU
                         b08 => eGK: Verwendung der ESIGN-AUTN Funktionalität mit PIN.CH
                         b09 => eGK: Verwendung der ESIGN-AUTN Funktionalität ohne PIN
                         b10 => eGK: Verwendung der ESIGN-ENCV Funktionalität mit PIN.CH
                         b11 => eGK: Verwendung der ESIGN-ENCV Funktionalität ohne PIN
                         b12 => eGK: Verwendung der ESIGN-AUT Funktionalität
                         b13 => eGK: Verwendung der ESIGN-ENC Funktionalität
                         b14 => eGK: Notfalldatensatz verbergen und sichtbar machen
                         b15%s
                         b16 => RFU
                         b17 => eGK: Notfalldatensatz lesen mit MRPIN.NFD
                         b18 => eGK: Notfalldatensatz lesen ohne PIN
                         b19 => eGK: Persönliche Erklärungen (DPE) verbergen und sichtbar machen
                         b20 => eGK: DPE schreiben, löschen (hier 'erase', nicht 'delete')
                         b21 => RFU
                         b22 => eGK: DPE lesen mit MRPIN.DPE_Read
                         b23 => eGK: DPE lesen ohne PIN
                         b24%s
                         b25%s
                         b26 => RFU
                         b27 => eGK: Einwilligungen im DF.HCA schreiben
                         b28 => eGK: Verweise im DF.HCA lesen und schreiben
                         b29 => eGK: Geschützte Versichertendaten lesen mit PIN.CH
                         b30 => eGK: Geschützte Versichertendaten lesen ohne PIN
                         b31 => eGK: Loggingdaten schreiben mit PIN.CH
                         b32 => eGK: Loggingdaten schreiben ohne PIN
                         b33 => eGK: Zugriff in den AdV-Umgebungen
                         b34 => eGK: Prüfungsnachweis lesen und schreiben
                         b35 => RFU
                         b36 => RFU
                         b37 => RFU
                         b38 => RFU
                         b39 => eGK: Gesundheitsdatendienste verbergen und sichtbar machen
                         b40%s
                         b41 => eGK: Organspendedatensatz lesen mit MRPIN.OSE
                         b42 => eGK: Organspendedatensatz lesen ohne PIN
                         b43%s
                         b44 => eGK: Organspendedatensatz verbergen und sichtbar machen
                         b45 => eGK: AMTS-Datensatz verbergen und sichtbar machen
                         b46 => eGK: AMTS-Datensatz lesen
                         b47%s
                         b48 => RFU
                         b49 => RFU
                         b50 => RFU
                         b51 => Auslöser Komfortsignatur
                         b52 => Sichere Signaturerstellungseinheit (SSEE)
                         b53 => Remote PIN Empfänger
                         b54 => Remote PIN Sender
                         b55 => SAK für Stapel- oder Komfortsignatur
                  """,
              "     CPI = 70 => self descriptive Card Verifiable Certificate", // CPI
              " = 2a8648ce3d040302 = {1 2 840 10045 4 3 2} = ecdsa-with-SHA256",
              " 04a42ee03e1e077b5db4dc347d3e22ce02ac3f44f0ad583ecb2f57e69ec96089da78"
                  + "b619056e17932fe64b1b41e21c05ee546d2909dc357e35612e1a2479c10d55",
              "a42ee03e1e077b5db4dc347d3e22ce02ac3f44f0ad583ecb2f57e69ec96089da", // xp
              "78b619056e17932fe64b1b41e21c05ee546d2909dc357e35612e1a2479c10d55", // yp
              " = 2a8214004c048118 = {1 2 276 0 76 4 152} = cvc_FlagList_TI",
              " => eGK: Notfalldatensatz schreiben, löschen (hier 'erase', nicht 'delete')",  // b15
              " => eGK: Einwilligungen und Verweise im DF.HCA verbergen und sichtbar machen", // b24
              " => eGK: Einwilligungen im DF.HCA lesen und löschen (hier 'erase', nicht 'delete')",
              " => eGK: Gesundheitsdatendienste lesen, schreiben und löschen (hier 'erase')", // b40
              " => eGK: Organspendedatensatz schreiben, löschen (hier 'erase', nicht 'delete')",
              " => eGK: AMTS-Datensatz schreiben, löschen (hier „erase“, nicht „delete“)"     // b47
          ).replaceAll("\n", LINE_SEPARATOR),
          dut.insExplanation
      );
      assertEquals("ffffffffffffff", Hex.toHexDigits(dut.insFlagList));
      // hashCode() intentionally NOT checked here
      assertEquals(
          input.getConstructed(TAG_CERTIFICATE_CONTENT_TEMPLATE).orElseThrow(),
          dut.insMessage
      );
      assertEquals("2a8214004c048118", Hex.toHexDigits(dut.insOidFlagList));
      assertTrue(dut.insOidFlagListInterpretation.isPresent());
      assertEquals(AfiOid.CVC_FlagList_TI, dut.insOidFlagListInterpretation.orElseThrow());
      assertEquals("2a8648ce3d040302", Hex.toHexDigits(dut.insOidPuk));
      assertTrue(dut.insOidPukUsage.isPresent());
      assertEquals(AfiOid.ECDSA_with_SHA256, dut.insOidPukUsage.orElseThrow());
      assertEquals(
          new EcPublicKeyImpl(
              new ECPoint(
                  new BigInteger(
                      "a42ee03e1e077b5db4dc347d3e22ce02ac3f44f0ad583ecb2f57e69ec96089da",
                      16
                  ),
                  new BigInteger(
                      "78b619056e17932fe64b1b41e21c05ee546d2909dc357e35612e1a2479c10d55",
                      16
                  )
              ),
              AfiElcParameterSpec.brainpoolP256r1
          ),
          dut.getPublicKey()
      );
      assertEquals(
          "04a42ee03e1e077b5db4dc347d3e22ce02ac3f44f0ad583ecb2f57e69ec96089da78"
              + "b619056e17932fe64b1b41e21c05ee546d2909dc357e35612e1a2479c10d55",
          Hex.toHexDigits(dut.insPuK)
      );
      assertEquals(
          List.of(
              "verification key was not found => signature could not be checked"
          ),
          dut.getReport()
      );
      assertEquals(
          "3660c95a1de68819966d08f17039f31f6152ea0b518d4b0f55c4b83c9b20ebb92ba"
              + "7a7a4009bafd2bb01160a1b24dff2923e55df64e036d84e62f553e150fbf6",
          Hex.toHexDigits(dut.insSignature)
      );
      assertEquals(Cvc.SIGNATURE_NO_PUBLIC_KEY, dut.getSignatureStatus());

      // Note: Hereafter is some derived information
      assertEquals(
          Hex.extractHexDigits(
              """
                  7f4e 8191
                  |  5f29 01 70
                  |  42 08 44455a4757810214
                  |  7f49 4d
                  |  |  06 08 2a8648ce3d040302
                  |  |  86 41 04a42ee03e1e077b5db4dc347d3e22ce02ac3f44f0ad583ecb2f57e69ec96089da
                                78b619056e17932fe64b1b41e21c05ee546d2909dc357e35612e1a2479c10d55
                  |  5f20 08 44455a4757820216
                  |  7f4c 13
                  |  |  06 08 2a8214004c048118
                  |  |  53 07 ffffffffffffff
                  |  5f25 06 010600060007
                  |  5f24 06 020600060006
                  5f37 40 3660c95a1de68819966d08f17039f31f6152ea0b518d4b0f55c4b83c9b20ebb9
                          2ba7a7a4009bafd2bb01160a1b24dff2923e55df64e036d84e62f553e150fbf6
                  """
          ),
          Hex.toHexDigits(dut.getValueField())
      );
      assertFalse(dut.isEndEntity());
      assertFalse(dut.isSubCa());
      assertTrue(dut.isRootCa());
      // toString() intentionally not tested here
    } // end --- d.

    // --- e. check if extra DO are ignored and sequence does not matter
    { // derived from: DEGXX_8-6-02-20 / DEGXX_1-1-02-20 / 0009_80-276-88311-0000129008
      final ConstructedBerTlv input = (ConstructedBerTlv) BerTlv.getInstance(
          """
              7f21 81f6
              |  5f37 40 92564533191b57fffeea94520700c5b727f686ac4c223713ae6c4ea4aa0ef146
                         890df9165746fa7fc3172494af42c3ecc4ecf14bd40947929d7a63bd352bfb5d
              |  80 00
              |  7f4e 81ad
              |  |  81 01 11
              |  |  7f4c 18
              |  |  |  53 07 0000000000000c
              |  |  |  82 03 112233
              |  |  |  06 08 2a8214004c048118
              |  |  42 08 4445475858110220
              |  |  83 00
              |  |  5f24 06 020500050005
              |  |  7f49 4d
              |  |  |  84 00
              |  |  |  86 41 041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9
                               a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00
              |  |  |  06 06 2b2403050301
              |  |  5f29 01 70
              |  |  5f29 01 82
              |  |  5f20 0c 000980276883110000129008
              |  |  06 08 2a8214004c048119
              |  |  5f25 06 020000050006
              """
      );
      final Cvc dut = new Cvc(input);

      // Note: The sequence of checks hereafter use the alphabetic order of
      //       instance attributes.
      assertEquals("4445475858110220", dut.getCar());
      assertEquals("DEGXX_1-1-02-20", dut.getCarHumanReadable());
      assertEquals("020000050006", dut.getCed());
      assertEquals("06. Mai 2020", dut.getCedHumanReadable());
      assertEquals("000980276883110000129008", dut.getChr());
      assertEquals("0009_80-276-88311-0000129008", dut.getChrHumanReadable());
      assertEquals("70", Hex.toHexDigits(dut.insCpi.getValue()));
      assertFalse(dut.hasCriticalFindings());
      assertSame(input, dut.getCvc());
      assertEquals("020500050005", dut.getCxd());
      assertEquals("05. Mai 2025", dut.getCxdHumanReadable());
      assertTrue(dut.insDomainParameter.isPresent());
      assertEquals(AfiElcParameterSpec.brainpoolP256r1, dut.insDomainParameter.orElseThrow());
      assertEquals(
          String.format(
              """
                  Certificate Profile Indicator%s
                  Certificate Holder Reference      CHR = 000980276883110000129008
                         Discretionary data  = 0009
                         ICCSN               = 80276883110000129008
                           Major Industry ID = 80
                           Country Code      = 276
                           Issuer Identifier = 88311
                           Serial Number     = 0000129008
                  Certification Authority Reference CAR = 4445475858110220
                         CA-Identifier       = DEGXX
                         service indicator   = 1
                         discretionary data  = 1
                         algorithm reference = 02
                         generation year     = 20
                  Certificate Effective  Date       CED = 020000050006     => 06. Mai 2020
                  Certificate Expiration Date       CXD = 020500050005     => 05. Mai 2025
                  Usage of enclosed public key      OID%s
                  Public point as octet string      P   =%s
                         Domain parameter of public key = brainpoolP256r1
                         P = (xp, yp) = ('%s', '%s')
                  Interpretation of Flag-List       OID%s
                  List of access rights,      Flag-List = 0000000000000c
                         b0b1:  Endnutzer Zertifikat für Authentisierungszwecke
                         b52 => Sichere Signaturerstellungseinheit (SSEE)
                         b53 => Remote PIN Empfänger
                  """,
              "     CPI = 70 => self descriptive Card Verifiable Certificate", // CPI
              " = 2b2403050301     = {1 3 36 3 5 3 1} = authS_gemSpec-COS-G2_ecc-with-sha256",
              " 041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9a3"
                  + "6167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
              "1801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9",
              "a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
              " = 2a8214004c048118 = {1 2 276 0 76 4 152} = cvc_FlagList_TI"
          ).replaceAll("\n", LINE_SEPARATOR),
          dut.insExplanation
      );
      assertEquals("0000000000000c", Hex.toHexDigits(dut.insFlagList));
      // hashCode() intentionally NOT checked here
      assertEquals(
          input.getConstructed(TAG_CERTIFICATE_CONTENT_TEMPLATE).orElseThrow(),
          dut.insMessage
      );
      assertEquals("2a8214004c048118", Hex.toHexDigits(dut.insOidFlagList));
      assertTrue(dut.insOidFlagListInterpretation.isPresent());
      assertEquals(AfiOid.CVC_FlagList_TI, dut.insOidFlagListInterpretation.orElseThrow());
      assertEquals("2b2403050301", Hex.toHexDigits(dut.insOidPuk));
      assertTrue(dut.insOidPukUsage.isPresent());
      assertEquals(AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256, dut.insOidPukUsage.orElseThrow());
      assertEquals(
          new EcPublicKeyImpl(
              new ECPoint(
                  new BigInteger(
                      "1801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9",
                      16
                  ),
                  new BigInteger(
                      "a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
                      16
                  )
              ),
              AfiElcParameterSpec.brainpoolP256r1
          ),
          dut.getPublicKey()
      );
      assertEquals(
          "041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab"
              + "9a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00",
          Hex.toHexDigits(dut.insPuK)
      );
      assertEquals(
          List.of(
              "verification key was not found => signature could not be checked"
          ),
          dut.getReport()
      );
      assertEquals(
          "92564533191b57fffeea94520700c5b727f686ac4c223713ae6c4ea4aa0ef146"
              + "890df9165746fa7fc3172494af42c3ecc4ecf14bd40947929d7a63bd352bfb5d",
          Hex.toHexDigits(dut.insSignature)
      );
      assertEquals(Cvc.SIGNATURE_NO_PUBLIC_KEY, dut.getSignatureStatus());

      // Note: Hereafter is some derived information
      assertEquals(
          Hex.extractHexDigits(
              """
                  5f37 40 92564533191b57fffeea94520700c5b727f686ac4c223713ae6c4ea4aa0ef146
                          890df9165746fa7fc3172494af42c3ecc4ecf14bd40947929d7a63bd352bfb5d
                  -------------------
                  80 00
                  -------------------
                  7f4e 81ad
                  |  81 01 11
                  |  7f4c 18
                  |  |  53 07 0000000000000c
                  |  |  82 03 112233
                  |  |  06 08 2a8214004c048118
                  |  42 08 4445475858110220
                  |  83 00
                  |  5f24 06 020500050005
                  |  7f49 4d
                  |  |  84 00
                  |  |  86 41 041801129424912f3dbe36cd5eaef7a2e033098f968bea2cc4ef839450cc6c4ab9
                                a36167afd04f081003ad6211bbb927f82b8d0d887527c879e0376c08bf6b9d00
                  |  |  06 06 2b2403050301
                  |  5f29 01 70
                  |  5f29 01 82
                  |  5f20 0c 000980276883110000129008
                  |  06 08 2a8214004c048119
                  |  5f25 06 020000050006"""
          ),
          Hex.toHexDigits(dut.getValueField())
      );
      assertTrue(dut.isEndEntity());
      assertFalse(dut.isSubCa());
      assertFalse(dut.isRootCa());
      // toString() intentionally not tested here
    } // end --- e.
  } // end method */

  /*
   * Test method for {@link Cvc#checkCar(List, List)}.
   *
  @Test
  void test_checkCar__List_List() {
    // Test strategy:
    // --- a. loop over relevant domain parameter
    // --- b. check with various lengths

    // --- a. loop over relevant domain parameter
    Stream.of(
            AfiElcParameterSpec.brainpoolP256r1,
            AfiElcParameterSpec.brainpoolP384r1,
            AfiElcParameterSpec.brainpoolP512r1
        ).forEach(domainParameter -> {
          // --- create private key for signature generation
          final EcPrivateKeyImpl prk = new EcPrivateKeyImpl(domainParameter);

          // --- b. check with various lengths
          IntStream.range(1, 20).forEach(length -> {
            final List<Object> carInfo = chrRandom(TAG_CAR, length, length);

            final PrimitiveBerTlv doCar = (PrimitiveBerTlv) carInfo.get(0);
            final String car = Hex.toHexDigits(doCar.getValue());
            TrustCenter.CACHE_PUK.insCache.put(car, prk.getPublicKey());
            final Map<String, BerTlv> input = new ConcurrentHashMap<>();
            input.put(KEY_CAR, doCar);

            final ConstructedBerTlv doCvc = cvcRandom(input, prk);
            final Cvc dut = new Cvc(doCvc);

            final List<String> report = new ArrayList<>();
            final List<String> explanation = new ArrayList<>();
            final String carHumanReadable = dut.checkCar(report, explanation);

            assertEquals(car, dut.getCar());

            if (8 == length) { // NOPMD literal in if statement
              // ... correct length
              assertEquals(
                  String.format(
                      "%s_%d-%d-%s-%s",
                      carInfo.get(1),
                      (Integer) carInfo.get(2),
                      (Integer) carInfo.get(3),
                      String.format("%02x", (Integer) carInfo.get(4)),
                      String.format("%02d", ((Integer) carInfo.get(5)) - 2000)
                  ),
                  carHumanReadable
              );
              assertEquals(
                  String.format(
                      "Certification Authority Reference CAR = %s%n"
                          + "       CA-Identifier       = %s%n"
                          + "       service indicator   = %d%n"
                          + "       discretionary data  = %d%n"
                          + "       algorithm reference = %02x%n"
                          + "       generation year     = %02d%n",
                      car,
                      carInfo.get(1),
                      (Integer) carInfo.get(2),
                      (Integer) carInfo.get(3),
                      (Integer) carInfo.get(4),
                      ((Integer) carInfo.get(5)) - 2000
                  ),
                  explanation.toString()
              );
              assertTrue(report.isEmpty(), report::toString);
            } else {
              // ... incorrect length
              assertEquals(car, carHumanReadable);
              assertEquals(
                  String.format(
                      "Certification Authority Reference CAR = %s%n",
                      car
                  ),
                  explanation.toString()
              );
              assertEquals(
                  List.of(
                      "CAR has an invalid length"
                  ),
                  report
              );
            } // end else (length == 8)

          }); // end forEach(length -> ...)
        }); // end forEach(domainParameter -> ...)
  } // end method */

  /*
   * Test method for {@link Cvc#checkCed(List, List)}.
   *
  @Test
  void test_checkCed__List_List() {
    // Assertions:
    // ... a. explainDate(...)-method works as expected

    // Note: Because of the assertion the method-under-test is rather simple.
    //       Thus, we can be lazy here and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    final String prefix = "Certificate Effective  Date       CED";
    final List<String> report = new ArrayList<>();
    List.of(
        AfiElcParameterSpec.brainpoolP256r1,
        AfiElcParameterSpec.brainpoolP384r1,
        AfiElcParameterSpec.brainpoolP512r1
    ).forEach(domainParameter -> {
      final EcPrivateKeyImpl prk = new EcPrivateKeyImpl(domainParameter);

      final PrimitiveBerTlv doCar = (PrimitiveBerTlv) chrRandom(TAG_CAR, 8, 8).get(0);
      TrustCenter.CACHE_PUK.insCache.put(
          Hex.toHexDigits(doCar.getValue()),
          prk.getPublicKey()
      );

      final List<Object> randomDate = dateRandom(TAG_CED, 2000, 2014);
      final PrimitiveBerTlv doDate = (PrimitiveBerTlv) randomDate.get(0);
      final LocalDate localDate = (LocalDate) randomDate.get(1);

      final Map<String, BerTlv> input = new ConcurrentHashMap<>();
      input.put(KEY_CAR, doCar);
      input.put(KEY_CED, doDate);

      final ConstructedBerTlv doCvc = cvcRandom(input, prk);
      final Cvc dut = new Cvc(doCvc);

      final List<String> explanation = new ArrayList<>();
      final String humanReadable = dut.checkCed(
          report,
          explanation
      );

      assertEquals(
          localDate.format(Cvc.DATE_FORMATTER),
          humanReadable
      );
      assertTrue(report.isEmpty());
      assertEquals(
          String.format(
              "%s = %-16s => %s%n",
              prefix,
              Hex.toHexDigits(doDate.getValue()),
              humanReadable
          ),
          explanation.toString()
      );
    }); // end forEach(domainParameter -> ...)
  } // end method */

  /*
   * Test method for {@link Cvc#checkCxd(List, List)}.
   *
  @Test
  void test_checkCxd__List_List() {
    // Assertions:
    // ... a. explainDate(...)-method works as expected

    // Note: Because of the assertion the method-under-test is rather simple.
    //       Thus, we can be lazy here and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    final String prefix = "Certificate Expiration Date       CXD";
    final List<String> report = new ArrayList<>();
    List.of(
        AfiElcParameterSpec.brainpoolP256r1,
        AfiElcParameterSpec.brainpoolP384r1,
        AfiElcParameterSpec.brainpoolP512r1
    ).forEach(domainParameter -> {
      final EcPrivateKeyImpl prk = new EcPrivateKeyImpl(domainParameter);

      final PrimitiveBerTlv doCar = (PrimitiveBerTlv) chrRandom(TAG_CAR, 8, 8).get(0);
      TrustCenter.CACHE_PUK.insCache.put(
          Hex.toHexDigits(doCar.getValue()),
          prk.getPublicKey()
      );

      final List<Object> randomDate = dateRandom(TAG_CXD, 2015, 2099);
      final PrimitiveBerTlv doDate = (PrimitiveBerTlv) randomDate.get(0);
      final LocalDate localDate = (LocalDate) randomDate.get(1);

      final Map<String, BerTlv> input = new ConcurrentHashMap<>();
      input.put(KEY_CAR, doCar);
      input.put(KEY_CXD, doDate);

      final ConstructedBerTlv doCvc = cvcRandom(input, prk);
      final Cvc dut = new Cvc(doCvc);

      final List<String> explanation = new ArrayList<>();
      final String humanReadable = dut.checkCxd(
          report,
          explanation
      );

      assertEquals(
          localDate.format(Cvc.DATE_FORMATTER),
          humanReadable
      );
      assertTrue(report.isEmpty());
      assertEquals(
          String.format(
              "%s = %-16s => %s%n",
              prefix,
              Hex.toHexDigits(doDate.getValue()),
              humanReadable
          ),
          explanation.toString()
      );
    }); // end forEach(domainParameter -> ...)
  } // end method */

  /*
   *  Test method for {@link Cvc#checkCpi(List, List)}.
   *
  @Test @org.junit.jupiter.api.Disabled
  void test_checkCpi__List_List() {
    LOGGER.atInfo().log("valid CPI: {}", cpiValid().toString(" "));
    LOGGER.atInfo().log("randomCPI: {}", cpiRandom(1, 10).toString(" "));
    LOGGER.atInfo().log("randomCPI: {}", cpiRandom(0, 12).toString(" "));

    fail("not (yet) implemented");
  } // end method */

  /** Test method for {@link Cvc#checkOidFlagList(List, List)}. */
  @Test
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_checkOidFlagList__List_List")
  void test_checkOidFlagList__List_List() {
    final byte msByte = (byte) 0xc0;
    IntStream.range(0, 20)
        .forEach(
            i ->
                LOGGER.atInfo().log("Flag-List: {}", randomFlaglist(1, 10, msByte).toStringTree()));

    fail("not (yet) implemented: test_checkOidFlagList__List_List");
  } // end method */

  /** Test method for {@link Cvc#checkOidPuk(List, List)}. */
  @Test
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_checkOidPuk__List_List")
  void test_checkOidPuk__List_List() {
    LOGGER.atInfo().log("OID: {}", randomOid().toStringTree());

    fail("not (yet) implemented: test_checkOidPuk__List_List");
  } // end method */

  /** Test method for {@link Cvc#checkPublicKey(List, List)}. */
  @Test
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_checkPublicKey__List_List")
  void test_checkPublicKey__List_List() {
    LOGGER.atInfo().log("PuK.256: {}", randomPuk(brainpoolP256r1));
    LOGGER.atInfo().log("PuK.384: {}", randomPuk(brainpoolP384r1));
    LOGGER.atInfo().log("PuK.512: {}", randomPuk(brainpoolP512r1));
    LOGGER.atInfo().log("PuK.rnd: {}", randomPuk());

    fail("not (yet) implemented: test_checkPublicKey__List_List");
  } // end method */

  /** Test method for {@link Cvc#getCvc()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_getCvc() {
    // Test strategy:
    // --- a. loop over all relevant domain parameter
    // --- b. generate a bunch of CVC with random content
    // --- c. check for findings

    // Note 1:    10 rounds => execution time approx. 2.7 s, smoke test
    // Note 2: 30000 rounds => execution time approx. 1 h, rather thoroughly
    final var roundsPerDomainParameter = 10;

    // --- a. loop over all relevant domain parameter
    // --- b. generate a bunch of CVC with random content
    // --- c. check for findings
    for (final var domainParameter : List.of(brainpoolP256r1, brainpoolP384r1, brainpoolP512r1)) {
      final EcPrivateKeyImpl prk = new EcPrivateKeyImpl(domainParameter);

      final PrimitiveBerTlv doCar = (PrimitiveBerTlv) randomChr(TAG_CAR, 8, 8).getFirst();
      TrustCenter.CACHE_PUK.insCache.put(
          Hex.toHexDigits(doCar.getValueField()), prk.getPublicKey());

      for (int i = roundsPerDomainParameter; i-- > 0; ) { // NOPMD assignment in operand
        if (0 == (i % 1_000)) {
          LOGGER.atInfo().log("############## start round {}", i);
        } else {
          LOGGER.atTrace().log("############## start round {}", i);
        } // end else

        final Map<String, BerTlv> input = new ConcurrentHashMap<>();
        input.put(KEY_CAR, doCar);

        final ConstructedBerTlv doCvc = randomCvc(input, prk);
        final Cvc dut = new Cvc(doCvc);
        assertSame(doCvc, dut.getCvc());

        final List<String> report = dut.getReport();
        final boolean strangeReport = !report.isEmpty();

        if (strangeReport) {
          LOGGER.atInfo().log("CVC: {}", dut);
          LOGGER.atInfo().log("report: {}", dut.getReport());
        } // end fi

        assertFalse(dut.hasCriticalFindings());
        assertFalse(strangeReport);
      } // end For (i...)
    } // end For (domainParameter...)
  } // end method */

  /*
   * Test method for {@link Cvc#explainDate(String, byte[], List, List)}.
   *
  @Test
  void test_explainDate__String_byteA_List_List() {
    // Test strategy:
    // --- a. loop over various prefixes
    // --- b. ERROR: wrong length
    // --- c. ERROR: correct length == 6, but invalid octets
    // --- d. ERROR: correct length == 6, valid octets, not normalized
    // --- e. correct length == 6, valid octets, normalized, all possible values

    // --- a. loop over various prefixes
    RNG.intsClosed(0, 20, 5).forEach(prefixLength -> {
      final String prefix = RNG.nextUtf8(prefixLength);

      // --- b. random lengths from range [0, 10] (but always excluding correct length 6)
      RNG.intsClosed(0, 10, 6)
          .filter(dateLength -> 6 != dateLength)
          .forEach(dateLength -> {
            final byte[] date = RNG.nextBytes(dateLength);

            final List<String> report = new ArrayList<>();
            final List<String> explanation = new ArrayList<>();
            final String humanReadable = Cvc.explainDate(prefix, date, report, explanation);

            assertEquals(
                Hex.toHexDigits(date),
                humanReadable
            );
            assertEquals(
                List.of(prefix + " has an invalid length"),
                report
            );
            assertEquals(
                String.format("%s = %-16s%n", prefix, humanReadable),
                explanation.toString()
            );
          }); // end forEach(dateLength -> ...)
      // end --- a.

      // --- c. correct length == 6, but invalid octets
      {
        final List<String> report = new ArrayList<>();
        final List<String> explanation = new ArrayList<>();
        for (int index = 6; index-- > 0;) { // NOPMD assignment in operand
          // get a valid random date
          final byte[] date = ((PrimitiveBerTlv) dateRandom(TAG_CED, 2000, 2099).get(0)).getValue();

          for (int i = 255; i-- > 10;) { // NOPMD assignment in operand
            date[index] = (byte) i;

            report.clear();
            explanation.clear();
            final String humanReadable = Cvc.explainDate(prefix, date, report, explanation);

            assertEquals(
                Hex.toHexDigits(date),
                humanReadable
            );
            assertEquals(
                List.of(prefix + " has invalid digits"),
                report
            );
            assertEquals(
                String.format("%s = %-16s%n", prefix, humanReadable),
                explanation.toString()
            );
          } // end For (i...)
        } // end For (index...)
      } // end --- c.

      // --- d. correct length == 6, valid octets, not normalized
      List.of(
          "0207 0002 0209", // 2027-02-29, not a leap year, but 29th February
          "0203 0004 0301", // 2023-04-31, there never is a 31st day in April
          "0500 0000 0206", // 2050-00-26, there never is a 0th month
          "0302 0103 0109"  // 2032-13-19, there never is a 13th month
      ).forEach(input -> {
        final byte[] date = Hex.toByteArray(input);
        final List<String> report = new ArrayList<>();
        final List<String> explanation = new ArrayList<>();
        final String humanReadable = Cvc.explainDate(prefix, date, report, explanation);

        assertEquals(
            Hex.extractHexDigits(input),
            humanReadable
        );
        assertEquals(
            List.of(prefix + " is not normalized"),
            report
        );
        assertEquals(
            String.format("%s = %-16s%n", prefix, humanReadable),
            explanation.toString()
        );
      }); // end forEach(input -> ...)
      // end --- d.

      // --- e. correct length == 6, valid octets, normalized, all possible values
      {
        final List<String> report = new ArrayList<>();
        final List<String> explanation = new ArrayList<>();
        final LocalDate infimum = LocalDate.of(2000, 1, 1);    // smallest supported date
        final LocalDate supremum = LocalDate.of(2099, 12, 31); // greatest supported date
        for (LocalDate loDate = infimum; !loDate.isEqual(supremum); loDate = loDate.plusDays(1)) {
          final int day = loDate.getDayOfMonth();
          final int month = loDate.getMonthValue();
          final int year = loDate.getYear() - 2000;
          final String date = String.format(
              "%02d%02d%02d%02d%02d%02d",
              year / 10, year % 10,
              month / 10, month % 10,
              day / 10, day % 10
          );
          explanation.clear();
          final String humanReadable = Cvc.explainDate(
              prefix,
              Hex.toByteArray(date),
              report,
              explanation
          );

          assertEquals(
              loDate.format(Cvc.DATE_FORMATTER),
              humanReadable
          );
          assertTrue(report.isEmpty());
          assertEquals(
              String.format("%s = %-16s => %s%n", prefix, date, humanReadable),
              explanation.toString()
          );
        } // end For (localDate...)
      } // end --- e.
    }); // end forEach(prefixLength -> ...)
  } // end method */

  /**
   * Randomly created CHR.
   *
   * <p>If length is
   *
   * <ol>
   *   <li>eight, then the first five octet are ASCII printable characters and the remaining 6
   *       nibbles are binary coded decimal.
   *   <li>twelve, then the first two octet have arbitrary content and the remaining 20 nibble are
   *       binary coded decimal.
   *   <li>neither 8 nor 12, then an arbitrary octet string is generated as CHR
   * </ol>
   *
   * <p><b>Assertions:</b>
   *
   * <ol>
   *   <li>{@code 0 &le; minLength &le; maxLength}
   *   <li>{@code tag} is in set {{@link Cvc#TAG_CAR CAR}, {@link Cvc#TAG_CHR CHR}}
   * </ol>
   *
   * @param tag used for TLV object
   * @param minLength minimal number of octet in CHR
   * @param maxLength maximal number of octet in CHR
   * @return list with various things, if number of octet in value-field is
   *     <ul>
   *       <li>8, then the following items are returned:
   *           <ol>
   *             <li>{@link PrimitiveBerTlv} with tag {@link Cvc#TAG_CAR}
   *             <li>{@link String} with CA-identifier (five characters)
   *             <li>{@link Integer} with service indicator, range [0, 9]
   *             <li>{@link Integer} with discretionary data, range [0, 9]
   *             <li>{@link Integer} with algorithm reference in range [0, 255]
   *             <li>{@link Integer} with year, range [2000, 2099]
   *           </ol>
   *       <li>12, then the following items are returned:
   *           <ol>
   *             <li>{@link PrimitiveBerTlv} with tag {@link Cvc#TAG_CAR}
   *             <li>{@link String} with discretionary data (four hex-digits)
   *             <li>{@link String} with major industry identifier (two decimal digits)
   *             <li>{@link String} with country code (three decimal digits)
   *             <li>{@link String} with issuer identifier (five decimal digits)
   *             <li>{@link String} with serial number (ten decimal digits)
   *           </ol>
   *       <li>other, then the following items are returned:
   *           <ol>
   *             <li>{@link PrimitiveBerTlv} with tag {@link Cvc#TAG_CAR}
   *             <li>{@link String} with value-field of TLV-object, hex-digits only
   *           </ol>
   *     </ul>
   */
  /* package */
  static List<Object> randomChr(final int tag, final int minLength, final int maxLength) {
    assertTrue(0 <= minLength);
    assertTrue(minLength <= maxLength);
    assertTrue(Set.of(TAG_CAR, TAG_CHR).contains(tag));

    final int length = RNG.nextIntClosed(minLength, maxLength);

    switch (length) { // NOPMD no default (that is a false positive)
      case 8 -> {
        int index;
        final byte[] valueField = new byte[length];
        final StringBuilder caIdentifier = new StringBuilder();

        // add CA-identifier
        index = 0;
        for (; index < 5; index++) {
          final double x = RNG.nextDouble();

          final int character;
          if (x < 0.5) { // NOPMD literal in if statement
            // ... with 50% probability add upper case letter, i.e. ['A', 'Z']
            character = RNG.nextIntClosed('A', 'Z');
          } else {
            // ... with 50% probability add lower case letter, i.e. ['a', 'z']
            character = RNG.nextIntClosed('a', 'z');
          } // end fi
          valueField[index] = (byte) character;
          caIdentifier.append((char) character);
        } // end For (index...)

        final int serviceIndicator = RNG.nextBoolean() ? 1 : 8;
        final int discretionaryData = RNG.nextIntClosed(0, 9);
        valueField[index++] = (byte) ((serviceIndicator << 4) + discretionaryData);

        final int algorithmReference = 2; // RNG.nextIntClosed(0, 0xff);
        valueField[index++] = (byte) algorithmReference;

        final int year = RNG.nextIntClosed(2000, 2099);
        valueField[index] = Hex.toByteArray(String.format("%4d", year))[1];

        return List.of(
            BerTlv.getInstance(tag, valueField),
            caIdentifier.toString(),
            serviceIndicator,
            discretionaryData,
            algorithmReference,
            year);
      } // end length == 8

      case 12 -> {
        final String discretionaryData = Hex.toHexDigits(RNG.nextBytes(2));
        final String majorIndustryId = "80"; // indicating health-care
        final String countryCode = String.format("%03d", RNG.nextIntClosed(0, 999));
        final String issuerIdentifier = String.format("%05d", RNG.nextIntClosed(0, 99_999));
        final String serialNumber = String.format("%010d", RNG.nextIntClosed(0, Integer.MAX_VALUE));

        return List.of(
            BerTlv.getInstance(
                tag,
                discretionaryData
                    + majorIndustryId
                    + countryCode
                    + issuerIdentifier
                    + serialNumber),
            discretionaryData,
            majorIndustryId,
            countryCode,
            issuerIdentifier,
            serialNumber);
      } // end length == 12

      default -> {
        final String chr = Hex.toHexDigits(RNG.nextBytes(length));

        return List.of(BerTlv.getInstance(tag, chr), chr);
      } // end default
    } // end Switch
  } // end method */

  /**
   * Randomly generated CPI.
   *
   * <p><b>Assertions:</b>
   *
   * <ol>
   *   <li>{@code 0 &le; minLength &le; maxLength}
   * </ol>
   *
   * @param minLength minimal number of octet in CPI
   * @param maxLength maximal number of octet in CPI
   * @return {@link PrimitiveBerTlv} with tag {@link Cvc#TAG_CPI}
   */
  /* package */
  static BerTlv randomCpi(final int minLength, final int maxLength) {
    assertTrue(0 <= minLength);
    assertTrue(minLength <= maxLength);

    return cpiTlv(RNG.nextBytes(minLength, maxLength));
  } // end method */

  /**
   * TLV-object with given CPI.
   *
   * @param cpi value-field of TLV-object
   * @return {@link PrimitiveBerTlv} with tag {@link Cvc#TAG_CPI}
   */
  /* package */
  static BerTlv cpiTlv(final byte[] cpi) {
    return BerTlv.getInstance(TAG_CPI, cpi);
  } // end method */

  /**
   * TLV-object with given CPI.
   *
   * @return {@link PrimitiveBerTlv} with tag {@link Cvc#TAG_CPI}
   */
  /* package */
  static BerTlv cpiValid() {
    return BerTlv.getInstance(TAG_CPI, new byte[] {0x70});
  } // end method */

  /**
   * Generates a random date.
   *
   * <p><b>Assertions:</b>
   *
   * <ol>
   *   <li>{@code tag} is in set {{@link Cvc#TAG_CED CED}, {@link Cvc#TAG_CXD CXD}}.
   *   <li>{@code 2000 &le; minYear &le; maxYear &le; 2099}
   * </ol>
   *
   * @param tag to be used
   * @param minYear infimum of year
   * @param maxYear supremum of year
   * @return list with the following elements:
   *     <ol>
   *       <li>{@link PrimitiveBerTlv} with given tag
   *       <li>{@link LocalDate} contained in the value-field
   *     </ol>
   */
  /* package */
  static List<Object> randomDate(final int tag, final int minYear, final int maxYear) {
    assertTrue(Set.of(TAG_CED, TAG_CXD).contains(tag));
    assertTrue(2000 <= minYear);
    assertTrue(minYear <= maxYear);
    assertTrue((maxYear <= 2099));

    final int yearOffset = 2000;

    final int year = RNG.nextIntClosed(minYear, maxYear) - yearOffset;
    final int month = RNG.nextIntClosed(1, 12);
    final int day = RNG.nextIntClosed(1, 28); // just to be sure :)

    return List.of(
        BerTlv.getInstance(
            tag,
            String.format(
                "%02d%02d%02d%02d%02d%02d",
                year / 10, year % 10, month / 10, month % 10, day / 10, day % 10)),
        LocalDate.of(year + yearOffset, month, day));
  } // end method */

  /**
   * Randomly generated flag list.
   *
   * <p><b>Assertions:</b>
   *
   * <ol>
   *   <li>{@code 0 &lt; minLength &le; maxLength}
   *   <li>bits b6 to b1 are clear in {@code msByte}
   * </ol>
   *
   * @param minLength minimal number of octet in CHR
   * @param maxLength maximal number of octet in CHR
   * @param msByte bits b8 and b7 (the most significant bits in that byte) define the first two bits
   *     in the flag list
   * @return {@link PrimitiveBerTlv} with tag {@link Cvc#TAG_FLAG_LIST}
   */
  /* package */
  static BerTlv randomFlaglist(final int minLength, final int maxLength, final byte msByte) {
    assertTrue(0 < minLength);
    assertTrue(minLength <= maxLength);
    assertEquals(0, msByte & 0x3f);

    final byte[] flagList;
    if (0xc0 == (msByte & 0xff)) { // NOPMD literal in if statement
      // ... link certificate
      //     => set all bits
      flagList = Hex.toByteArray("ff ffff ffff ffff");
    } else {
      // ... not a link certificate
      //     => generate Flag-List randomly
      flagList = RNG.nextBytes(minLength, maxLength);

      if (7 == flagList.length) { // NOPMD literal in if statement
        // --- clear all RFU bits
        final byte[] maskRfu = Hex.toByteArray("00 ff 7b df e1 ff 1f");

        for (int i = maskRfu.length; i-- > 0; ) { // NOPMD assignment in operand
          flagList[i] &= maskRfu[i];
        } // end For (i...)
      } // end fi

      // --- set bits b0 and b1 in flag list as indicated by the parameter msByte
      flagList[0] &= 0x3f; // clear the two MSBit
      flagList[0] |= (byte) (msByte & 0xc0); // set the two MSBit according to msByte
    } // end else

    return BerTlv.getInstance(TAG_FLAG_LIST, flagList);
  } // end method */

  /**
   * Randomly generated OID.
   *
   * @return {@link DerOid}
   */
  /* package */
  static BerTlv randomOid() {
    final List<Integer> oid = new ArrayList<>();

    final int firstComponent = RNG.nextIntClosed(0, 2);
    oid.add(firstComponent); // first component
    oid.add(RNG.nextIntClosed(0, (2 == firstComponent) ? 0x1_0000 : 39));

    for (int i = RNG.nextIntClosed(10, 10); i-- > 0; ) { // NOPMD assignment in operand
      oid.add((int) (Math.pow(RNG.nextDouble(), 10) * 10_000));
    } // end For (i...)

    return new DerOid(new AfiOid(oid));
  } // end method */

  /**
   * Randomly generated point on randomly chosen elliptic curve.
   *
   * <p>One of the brainpool curves will be randomly chosen.
   *
   * @return list with the following elements:
   *     <ol>
   *       <li>{@link PrimitiveBerTlv} with tag '86', its value-field contains the point in
   *           uncompressed format
   *       <li>{@link EcPrivateKeyImpl} corresponding private key
   *     </ol>
   */
  /* package */
  static List<Object> randomPuk() {
    final double rnd = RNG.nextDouble();

    if (rnd < 0.6) { // NOPMD literal in if statement
      // ... 60% chance for 256 bit curve
      return randomPuk(brainpoolP256r1);
    } else if (rnd < 0.85) { // NOPMD literal in if statement
      // ... 25% chance for 384 bit curve
      return randomPuk(brainpoolP384r1);
    } else {
      // ... 15% chance for 512 bit curve
      return randomPuk(brainpoolP512r1);
    } // end else
  } // end method */

  /**
   * Randomly generated valid point on given elliptic curve.
   *
   * <p><b>Assertion(s):</b>
   *
   * <ol>
   *   <li>{@code domainparameter} are one of the brainpool curves
   * </ol>
   *
   * @param domainparameter for point on elliptic curve
   * @return list with the following elements:
   *     <ol>
   *       <li>{@link PrimitiveBerTlv} with tag '86', its value-field contains the point in
   *           uncompressed format
   *       <li>{@link EcPrivateKeyImpl} corresponding private key
   *     </ol>
   */
  /* package */
  static List<Object> randomPuk(final AfiElcParameterSpec domainparameter) {
    assertTrue(Set.of(brainpoolP256r1, brainpoolP384r1, brainpoolP512r1).contains(domainparameter));

    final EcPrivateKeyImpl prk = new EcPrivateKeyImpl(domainparameter);

    return List.of(
        BerTlv.getInstance(
            0x86, AfiElcUtils.p2osUncompressed(prk.getPublicKey().getW(), domainparameter)),
        prk);
  } // end method */

  /**
   * Randomly generated signature.
   *
   * <p><b>Assertions:</b>
   *
   * <ol>
   *   <li>{@code 0 &le; minLength &le; maxLength}
   * </ol>
   *
   * @param minLength minimal number of octet in signature
   * @param maxLength maximal number of octet in signature
   * @return {@link PrimitiveBerTlv} with tag {@link Cvc#TAG_SIGNATURE}
   */
  /* package */
  static BerTlv randomSignature(final int minLength, final int maxLength) {
    assertTrue(0 <= minLength);
    assertTrue(minLength <= maxLength);

    return signatureTlv(RNG.nextBytes(minLength, maxLength));
  } // end method */

  /**
   * TLV-object with given signature.
   *
   * @param signature value-field of TLV-object
   * @return {@link PrimitiveBerTlv} with tag {@link Cvc#TAG_SIGNATURE}
   */
  /* package */
  static BerTlv signatureTlv(final byte[] signature) {
    return BerTlv.getInstance(TAG_SIGNATURE, signature);
  } // end method */

  /**
   * Create a card-verifiable certificate DO with random content.
   *
   * @param input predefined TLV-objects
   * @param prk private key used for signing the certificate content
   * @return appropriate TLV structure for a CV-certificate
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
  /* package */ static ConstructedBerTlv randomCvc(
      final Map<String, BerTlv> input, final EcPrivateKeyImpl prk) {
    // --- fill map "input" with missing data objects
    KEYS_PRIMITIVE.forEach(
        key -> {
          if (!input.containsKey(key)) {
            // ... currently no TLV-object for key
            //     => generate one
            LOGGER.atTrace().log("key = {}", key);

            switch (key) { // NOPMD no default (that is a false positive)
              case KEY_CPI -> input.put(key, cpiValid());

              case KEY_CAR -> input.put(key, (BerTlv) randomChr(TAG_CAR, 8, 8).getFirst());

              case KEY_OID_PUK_USAGE -> {
                // --- distinguish between End-Entity- and CA-certificate
                final boolean isEndEntity;
                if (input.containsKey(KEY_CHR)) {
                  // ... CHR available
                  //     => derive from CHR
                  isEndEntity = 8 != input.get(KEY_CHR).getValueField().length;
                } else if (input.containsKey(KEY_FLAG_LIST)) {
                  // ... Flag-List available
                  //     => investigate bits b0 b1
                  final byte msByte = input.get(KEY_FLAG_LIST).getValueField()[0];
                  isEndEntity = (0 == (msByte & 0xc0));
                } else {
                  // ... neither Flag-List nor CHR available
                  //     => decide randomly
                  isEndEntity = RNG.nextBoolean();
                } // end else (end-entity?)

                // --- distinguish between key length
                final int lengthPuk;
                if (input.containsKey(KEY_PUK)) {
                  // ... public key available
                  //     => use its length do define OID
                  lengthPuk = input.get(KEY_PUK).getValueField().length;
                } else {
                  // ... no public key available
                  //     => chose bit length randomly

                  final double rnd = RNG.nextDouble();
                  if (rnd < 0.33) { // NOPMD literal in if statement
                    // ... 33%: 512 bit
                    lengthPuk = 0x81;
                  } else if (rnd < 0.66) { // NOPMD literal in if statement
                    // ... 33%: 384 bit
                    lengthPuk = 0x61;
                  } else {
                    // ... 34%: 256 bit
                    lengthPuk = 0x41;
                  } // end else (...)
                } // end else (public key available?)

                // ... isEndEntity is well defined
                // ... lengthPuk   is well defined

                final AfiOid oid;
                if (0x81 == lengthPuk) { // NOPMD literal in if statement
                  // ... 512 bit
                  oid =
                      isEndEntity
                          ? AfiOid.authS_gemSpec_COS_G2_ecc_with_sha512
                          : AfiOid.ECDSA_with_SHA512;
                } else if (0x61 == lengthPuk) { // NOPMD literal in if statement
                  // ... 384 bit
                  oid =
                      isEndEntity
                          ? AfiOid.authS_gemSpec_COS_G2_ecc_with_sha384
                          : AfiOid.ECDSA_with_SHA384;
                } else {
                  // ... all other lengths
                  oid =
                      isEndEntity
                          ? AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256
                          : AfiOid.ECDSA_with_SHA256;
                } // end else (number of octet in public point)

                input.put(key, new DerOid(oid));
              } // end KEY_OID_PUK_USAGE

              case KEY_PUK -> {
                if (input.containsKey(KEY_OID_PUK_USAGE)) {
                  // ... OID available
                  //     => derive domain parameter from that OID
                  final AfiOid oid = ((DerOid) input.get(KEY_OID_PUK_USAGE)).getDecoded();

                  final AfiElcParameterSpec domainParameter;
                  if (Set.of(AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256, AfiOid.ECDSA_with_SHA256)
                      .contains(oid)) {
                    domainParameter = brainpoolP256r1;
                  } else if (Set.of(
                          AfiOid.authS_gemSpec_COS_G2_ecc_with_sha384, AfiOid.ECDSA_with_SHA384)
                      .contains(oid)) {
                    domainParameter = brainpoolP384r1;
                  } else {
                    domainParameter = brainpoolP512r1;
                  } // end else

                  input.put(key, (BerTlv) randomPuk(domainParameter).getFirst());
                } else {
                  // ... no OID for public key usage available
                  //     => chose domain parameter randomly
                  input.put(key, (BerTlv) randomPuk().getFirst());
                } // end else (OID available?)
              } // end KEY_PUK

              case KEY_CHR -> {
                // --- distinguish between End-Entity- and CA-certificate
                final boolean isEndEntity;
                if (input.containsKey(KEY_OID_PUK_USAGE)) {
                  // ... OID for public key usage available
                  //     => derive length of CHR from there
                  final AfiOid oid = ((DerOid) input.get(KEY_OID_PUK_USAGE)).getDecoded();
                  isEndEntity = !oid.getName().startsWith("ecdsa");
                } else if (input.containsKey(KEY_FLAG_LIST)) {
                  // ... Flag-List available
                  //     => investigate bits b0 b1
                  final byte msByte = input.get(KEY_FLAG_LIST).getValueField()[0];
                  isEndEntity = (0 == (msByte & 0xc0));
                } else {
                  // ... neither OID for public key usage  NOR Flag-List available
                  //     => chose type randomly
                  isEndEntity = RNG.nextBoolean();
                } // end else (OID for PuK usage available?)

                final int lengthChr = isEndEntity ? 12 : 8;
                final BerTlv doChr = (BerTlv) randomChr(TAG_CHR, lengthChr, lengthChr).getFirst();
                input.put(key, doChr);
              } // end KEY_CHR

              case KEY_OID_FLAG_LIST -> input.put(key, new DerOid(AfiOid.CVC_FlagList_TI));

              case KEY_FLAG_LIST -> {
                final boolean isEndEntity;

                if (input.containsKey(KEY_CHR)) {
                  // ... CHR available
                  //     => derive from CHR
                  isEndEntity = 8 != input.get(KEY_CHR).getValueField().length;
                } else if (input.containsKey(KEY_OID_PUK_USAGE)) {
                  // ... OID for public key usage available
                  //     => derive length of CHR from there
                  final AfiOid oid = ((DerOid) input.get(KEY_OID_PUK_USAGE)).getDecoded();
                  isEndEntity = !oid.getName().startsWith("ecdsa");
                } else {
                  // ... neither CHR  NOR  OID for public key usage available
                  //     => chose type randomly
                  isEndEntity = RNG.nextBoolean();
                } // end else

                final byte msByte = (byte) (isEndEntity ? 0x00 : (RNG.nextBoolean() ? 0x80 : 0xc0));

                input.put(key, randomFlaglist(7, 7, msByte));
              } // end KEY_FLAG_LIST

              case KEY_CED -> input.put(key, (BerTlv) randomDate(TAG_CED, 2010, 2014).getFirst());

              case KEY_CXD -> input.put(key, (BerTlv) randomDate(TAG_CXD, 2015, 2020).getFirst());

              case KEY_SIGNATURE -> {
                // intentionally empty
              } // end KEY_SIGNATURE

              default -> LOGGER.atWarn().log("no generator for key = {}", key);
            } // end Switch (key)
          } // end fi (input does not contain "key")

          LOGGER.atTrace().log("map: {}", input);
        }); // end forEach(key -> ...)
    // ... map "input" properly filled

    // --- define random order for TLV-object
    final var order = new TreeMap<Integer, String>();

    // Note 1: Loop until all keys from "KEYS_ALL" are values in map "order".
    // Note 2: With very high probability, the keys in map "order" are unique,
    //         because just 13 items are added to the map. It rarely happens
    //         that within 13 randomly picked integer values two have the same
    //         value. But better safe than sorry.
    do {
      order.clear();

      KEYS_ALL.forEach(key -> order.put(RNG.nextInt(), key));
    } while (order.size() < KEYS_ALL.size());
    // ... we have a random order for keys

    // --- compile DO for public key
    final BerTlv doPublicKey =
        BerTlv.getInstance(
            TAG_PUK_TEMPLATE,
            order.values().stream()
                .filter(s -> Set.of(KEY_OID_PUK_USAGE, KEY_PUK).contains(s))
                .map(input::get)
                .collect(Collectors.toList()));
    input.put(KEY_PUK_DO, doPublicKey);

    // --- compile DO for CHAT
    final BerTlv doChat =
        BerTlv.getInstance(
            TAG_CHAT,
            order.values().stream()
                .filter(s -> Set.of(KEY_OID_FLAG_LIST, KEY_FLAG_LIST).contains(s))
                .map(input::get)
                .collect(Collectors.toList()));
    input.put(KEY_CHAT, doChat);

    // --- compile DO for message
    final BerTlv doMessage =
        BerTlv.getInstance(
            TAG_CERTIFICATE_CONTENT_TEMPLATE,
            order.values().stream()
                .filter(
                    s ->
                        Set.of(KEY_CPI, KEY_CAR, KEY_PUK_DO, KEY_CHR, KEY_CHAT, KEY_CED, KEY_CXD)
                            .contains(s))
                .map(input::get)
                .collect(Collectors.toList()));
    input.put(KEY_MESSAGE, doMessage);

    // --- sign (if signature is not in predefined input)
    if (!input.containsKey(KEY_SIGNATURE)) {
      final byte[] messageToBeSigned = doMessage.getEncoded();
      final byte[] signature = prk.signEcdsa(messageToBeSigned);

      input.put(KEY_SIGNATURE, signatureTlv(signature));
    } // end fi

    // --- compile certificate content
    return (ConstructedBerTlv)
        BerTlv.getInstance(
            TAG_CARDHOLDER_CERTIFICATE_TEMPLATE,
            order.values().stream()
                .filter(s -> Set.of(KEY_MESSAGE, KEY_SIGNATURE).contains(s))
                .map(input::get)
                .collect(Collectors.toList()));
  } // end method */
} // end class
