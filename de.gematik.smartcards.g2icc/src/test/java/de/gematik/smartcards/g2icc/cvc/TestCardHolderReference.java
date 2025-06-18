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

import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CHR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link CardHolderReference}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestCardHolderReference {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

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

  /** Test method for {@link CardHolderReference#CardHolderReference(String)}. */
  @Test
  void test_CardHolderReference__String() {
    // Assertions:
    // ... a. underlying constructor CardHolderReference(PrimitiveBerTlv)
    //        works as expected

    // Note: Simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final String input = "2000_80-274-54321-987654320a";
    final CardHolderReference dut = new CardHolderReference(input);
    assertEquals(TAG_CHR, dut.getDataObject().getTag());
    assertEquals(input, dut.getHumanReadable());
  } // end method */

  /** Test method for {@link CardHolderReference#CardHolderReference(PrimitiveBerTlv)}. */
  @Test
  void test_CardHolderReference__PrimitiveBerTlv() {
    // Assertions:
    // ... a. underlying constructor CardHolderReference(PrimitiveBerTlv, String)
    //        works as expected

    // Note: Simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final CardHolderReference dut =
        new CardHolderReference(
            (PrimitiveBerTlv) BerTlv.getInstance("5f20 0c 1000_80-274-54321-987654320a"));
    assertFalse(dut.hasCriticalFindings());
    assertEquals(
        "Certificate Holder Reference      CHR = " + Hex.toHexDigits(dut.getValue()),
        dut.getExplanation().getFirst());
  } // end method */

  /** Test method for {@link CardHolderReference#CardHolderReference(PrimitiveBerTlv, String)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_CardHolderReference__PrimitiveBerTlv_String() {
    // Assertions:
    // ... a. explainCar(...)-method works as expected
    // ... b. explainChr(...)-method works as expected

    // Note: Because of the assertions the constructor-under-test is not that
    //       complicated. Thus, we can a bit lazy here.

    // Test strategy:
    // --- a. loop over several types
    // --- b. loop over several lengths, including 8 and 12

    // --- a. loop over several types
    final var types =
        List.of(
            "", // empty prefix
            "fooBar");
    for (final var type : types) {
      // --- b. loop over several lengths, including 8 and 12
      for (int length = 20; length-- > 0; ) { // NOPMD assignment in operand
        switch (length) { // NOPMD no default (false positive)
          case 8 -> {
            final String chr = "415a61787a_1-0-02-26";
            final CardHolderReference dut =
                new CardHolderReference((PrimitiveBerTlv) BerTlv.getInstance(TAG_CHR, chr), type);

            assertFalse(dut.hasCriticalFindings());
            assertEquals(
                List.of(
                    String.format("%s%s", type, Hex.extractHexDigits(chr)),
                    "       CA-Identifier       = AZaxz",
                    "       service indicator   = 1",
                    "       discretionary data  = 0",
                    "       algorithm reference = 02",
                    "       generation year     = 26"),
                dut.getExplanation());
            assertTrue(dut.getReport().isEmpty());
          } // end case length == 8

          case 12 -> {
            final String chr = "000f_80-274-54321-9876543210";
            final CardHolderReference dut =
                new CardHolderReference((PrimitiveBerTlv) BerTlv.getInstance(TAG_CHR, chr), type);

            assertFalse(dut.hasCriticalFindings());
            assertEquals(
                List.of(
                    String.format("%s%s", type, Hex.extractHexDigits(chr)),
                    "       Discretionary data  = 000f",
                    "       ICCSN               = 80274543219876543210",
                    "         Major Industry ID = 80",
                    "         Country Code      = 274",
                    "         Issuer Identifier = 54321",
                    "         Serial Number     = 9876543210"),
                dut.getExplanation());
            assertTrue(dut.getReport().isEmpty());
          } // end case length == 12

          default -> {
            final byte[] chr = RNG.nextBytes(length);
            final CardHolderReference dut =
                new CardHolderReference((PrimitiveBerTlv) BerTlv.getInstance(TAG_CHR, chr), type);

            assertTrue(dut.hasCriticalFindings());
            assertEquals(
                List.of(String.format("%s%s", type, Hex.toHexDigits(chr))), dut.getExplanation());
            assertEquals(List.of("CHR has an invalid length"), dut.getReport());
          } // end default
        } // end Switch (length)
      } // end For (length...)
    } // end For (type...=
  } // end method */

  /** Test method for {@link CardHolderReference#explainCar(byte[], List, List, List)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
  @Test
  void test_explainCar__byteA_List_List_List() {
    // Test strategy:
    // --- a. smoke test
    // --- b. smoke test with valid values
    // --- c. invalid characters in CA-name
    // --- d. service indicator not in set {1, 8}
    // --- e. discretionary data not BCD encoded
    // --- f. algorithm reference does not indicate ELC
    // --- g. year not BCD encoded

    final List<Boolean> critical = new ArrayList<>();
    final List<String> explanation = new ArrayList<>();
    final List<String> report = new ArrayList<>();

    // --- a. smoke test
    {
      final var car = "425a61787a_1-0-02-24";
      final var expected =
          List.of(
              "BZaxz_1-0-02-24",
              List.of(
                  "       CA-Identifier       = BZaxz",
                  "       service indicator   = 1",
                  "       discretionary data  = 0",
                  "       algorithm reference = 02",
                  "       generation year     = 24"));
      final var exp0 = expected.getFirst();
      final var exp1 = expected.getLast();

      final var present =
          CardHolderReference.explainCar(Hex.toByteArray(car), critical, explanation, report);

      assertEquals(exp0, present);
      assertTrue(critical.isEmpty());
      assertEquals(exp1, explanation);
      assertTrue(report.isEmpty());
    } // end --- a.

    // --- b. smoke test with valid values
    Map.ofEntries(
            // infimum supported length == regular length
            Map.entry(
                "415a61787a_1-0-02-24",
                List.of(
                    "AZaxz_1-0-02-24",
                    List.of(
                        "       CA-Identifier       = AZaxz",
                        "       service indicator   = 1",
                        "       discretionary data  = 0",
                        "       algorithm reference = 02",
                        "       generation year     = 24"))),
            // longer than regular
            Map.entry(
                "4445544158_8-9-02-2837",
                List.of(
                    "DETAX_8-9-02-28",
                    List.of(
                        "       CA-Identifier       = DETAX",
                        "       service indicator   = 8",
                        "       discretionary data  = 9",
                        "       algorithm reference = 02",
                        "       generation year     = 28"))))
        .forEach(
            (car, expected) -> {
              explanation.clear();
              assertEquals(
                  expected.get(0),
                  CardHolderReference.explainCar(
                      Hex.toByteArray(car), critical, explanation, report));
              assertTrue(critical.isEmpty());
              assertEquals(expected.get(1), explanation);
              assertTrue(report.isEmpty());
            }); // end forEach(chr -> ...)
    // end --- b.

    // --- c. invalid characters in CA-name
    Map.ofEntries(
            // 1st character just below infimum='A'
            Map.entry(
                "405a61787a_1-0-02-24",
                List.of(
                    "@Zaxz_1-0-02-24",
                    List.of(
                        "       CA-Identifier       = @Zaxz",
                        "       service indicator   = 1",
                        "       discretionary data  = 0",
                        "       algorithm reference = 02",
                        "       generation year     = 24"))),
            // 2nd character just above 'Z'
            Map.entry(
                "415b61787a_1-0-02-14",
                List.of(
                    "A[axz_1-0-02-14",
                    List.of(
                        "       CA-Identifier       = A[axz",
                        "       service indicator   = 1",
                        "       discretionary data  = 0",
                        "       algorithm reference = 02",
                        "       generation year     = 14"))),
            // 3rd character just below 'a'
            Map.entry(
                "415a60787a_1-0-02-16",
                List.of(
                    "AZ`xz_1-0-02-16",
                    List.of(
                        "       CA-Identifier       = AZ`xz",
                        "       service indicator   = 1",
                        "       discretionary data  = 0",
                        "       algorithm reference = 02",
                        "       generation year     = 16"))),
            // 4th character just above supremum = 'z'
            Map.entry(
                "415a617b7a_1-0-02-18",
                List.of(
                    "AZa{z_1-0-02-18",
                    List.of(
                        "       CA-Identifier       = AZa{z",
                        "       service indicator   = 1",
                        "       discretionary data  = 0",
                        "       algorithm reference = 02",
                        "       generation year     = 18"))),
            // 5th character is a decimal digit
            Map.entry(
                "415a617831_1-0-02-20",
                List.of(
                    "AZax1_1-0-02-20",
                    List.of(
                        "       CA-Identifier       = AZax1",
                        "       service indicator   = 1",
                        "       discretionary data  = 0",
                        "       algorithm reference = 02",
                        "       generation year     = 20"))))
        .forEach(
            (car, expected) -> {
              critical.clear();
              explanation.clear();
              report.clear();

              assertEquals(
                  expected.get(0),
                  CardHolderReference.explainCar(
                      Hex.toByteArray(car), critical, explanation, report));
              assertEquals(List.of(true), critical);
              assertEquals(expected.get(1), explanation);
              assertEquals(List.of("CA name contains non-alphabetic characters"), report);
            }); // end forEach(chr -> ...)
    // end --- c.

    // --- d. service indicator not in set {1, 8}
    IntStream.rangeClosed(0, 0xf)
        .forEach(
            serviceIndicator -> {
              final String car = String.format("4445544158_%x-9-02-28", serviceIndicator);

              critical.clear();
              explanation.clear();
              report.clear();

              assertEquals(
                  String.format("DETAX_%x-9-02-28", serviceIndicator),
                  CardHolderReference.explainCar(
                      Hex.toByteArray(car), critical, explanation, report));

              assertEquals(
                  ((1 == serviceIndicator) || (8 == serviceIndicator))
                      ? Collections.emptyList()
                      : List.of(true),
                  critical);

              assertEquals(
                  List.of(
                      "       CA-Identifier       = DETAX",
                      String.format("       service indicator   = %x", serviceIndicator),
                      "       discretionary data  = 9",
                      "       algorithm reference = 02",
                      "       generation year     = 28"),
                  explanation);

              assertEquals(
                  ((1 == serviceIndicator) || (8 == serviceIndicator))
                      ? Collections.emptyList()
                      : List.of("service indicator not in set {1, 8}"),
                  report);
            }); // end forEach(serviceIndicator -> ...)
    // end --- d.

    // --- e. discretionary data not BCD encoded
    IntStream.rangeClosed(0, 0xf)
        .forEach(
            discretionaryData -> {
              final String car = String.format("4445544158_1-%x-02-28", discretionaryData);

              critical.clear();
              explanation.clear();
              report.clear();

              assertEquals(
                  String.format("DETAX_1-%x-02-28", discretionaryData),
                  CardHolderReference.explainCar(
                      Hex.toByteArray(car), critical, explanation, report));

              assertEquals(
                  (discretionaryData <= 9) ? Collections.emptyList() : List.of(true), critical);

              assertEquals(
                  List.of(
                      "       CA-Identifier       = DETAX",
                      "       service indicator   = 1",
                      String.format("       discretionary data  = %x", discretionaryData),
                      "       algorithm reference = 02",
                      "       generation year     = 28"),
                  explanation);

              assertEquals(
                  (discretionaryData <= 9)
                      ? Collections.emptyList()
                      : List.of("discretionary data not BCD encoded"),
                  report);
            }); // end forEach(serviceIndicator -> ...)
    // end --- e.

    // --- f. algorithm reference does not indicate ELC
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            algRef -> {
              final String car = String.format("4445544158_1-8-%02x-28", algRef);

              critical.clear();
              explanation.clear();
              report.clear();

              assertEquals(
                  String.format("DETAX_1-8-%02x-28", algRef),
                  CardHolderReference.explainCar(
                      Hex.toByteArray(car), critical, explanation, report));

              assertEquals((algRef == 2) ? Collections.emptyList() : List.of(true), critical);

              assertEquals(
                  List.of(
                      "       CA-Identifier       = DETAX",
                      "       service indicator   = 1",
                      "       discretionary data  = 8",
                      String.format("       algorithm reference = %02x", algRef),
                      "       generation year     = 28"),
                  explanation);

              assertEquals(
                  (algRef == 2)
                      ? Collections.emptyList()
                      : List.of("algorithm reference does not indicate elliptic curve"),
                  report);
            }); // end forEach(serviceIndicator -> ...)
    // end --- f.

    // --- g. year not BCD encoded
    IntStream.rangeClosed(0, 0xf)
        .forEach(
            tensYear -> {
              IntStream.rangeClosed(0, 0xf)
                  .forEach(
                      onesYear -> {
                        final String car =
                            String.format("4445544158_1-8-02-%x%x", tensYear, onesYear);

                        critical.clear();
                        explanation.clear();
                        report.clear();

                        assertEquals(
                            String.format("DETAX_1-8-02-%x%x", tensYear, onesYear),
                            CardHolderReference.explainCar(
                                Hex.toByteArray(car), critical, explanation, report));

                        assertEquals(
                            ((tensYear <= 9) && (onesYear <= 9))
                                ? Collections.emptyList()
                                : List.of(true),
                            critical);

                        assertEquals(
                            List.of(
                                "       CA-Identifier       = DETAX",
                                "       service indicator   = 1",
                                "       discretionary data  = 8",
                                "       algorithm reference = 02",
                                String.format(
                                    "       generation year     = %x%x", tensYear, onesYear)),
                            explanation);

                        assertEquals(
                            ((tensYear <= 9) && (onesYear <= 9))
                                ? Collections.emptyList()
                                : List.of("generation year not BCD encoded"),
                            report);
                      }); // end forEach(oYear -> ...)
            }); // end forEach(tYear -> ...)
    // end --- g.
  } // end method */

  /** Test method for {@link CardHolderReference#explainChr(byte[], List, List)}. */
  @Test
  void test_explainChr__byteA_List_List_List() {
    // Test strategy:
    // --- a. smoke test
    // --- b. smoke test with valid values
    // --- c. ICCSN with non-decimal digits
    // --- d. Major Industry Identifier not 'health-care'

    final List<String> explanation = new ArrayList<>();
    final List<String> report = new ArrayList<>();

    // --- a. smoke test
    {
      final var chr = "10ab_80-276-12345-98";
      final var expected =
          List.of(
              "       Discretionary data  = 10ab",
              "       ICCSN               = 802761234598",
              "         Major Industry ID = 80",
              "         Country Code      = 276",
              "         Issuer Identifier = 12345",
              "         Serial Number     = 98");

      final var present = CardHolderReference.explainChr(Hex.toByteArray(chr), explanation, report);

      assertEquals(chr, present);
      assertEquals(expected, explanation);
      assertTrue(report.isEmpty());
    } // end --- a.

    // --- b. smoke test with valid values
    Map.ofEntries(
            // infimum supported length
            Map.entry(
                "12ab_80-276-12345-98",
                List.of(
                    "       Discretionary data  = 12ab",
                    "       ICCSN               = 802761234598",
                    "         Major Industry ID = 80",
                    "         Country Code      = 276",
                    "         Issuer Identifier = 12345",
                    "         Serial Number     = 98")),
            // regular length
            Map.entry(
                "0000_80-274-54321-9876543210",
                List.of(
                    "       Discretionary data  = 0000",
                    "       ICCSN               = 80274543219876543210",
                    "         Major Industry ID = 80",
                    "         Country Code      = 274",
                    "         Issuer Identifier = 54321",
                    "         Serial Number     = 9876543210")),
            // longer than regular
            Map.entry(
                "ffff_80-472-13524-987654321042",
                List.of(
                    "       Discretionary data  = ffff",
                    "       ICCSN               = 8047213524987654321042",
                    "         Major Industry ID = 80",
                    "         Country Code      = 472",
                    "         Issuer Identifier = 13524",
                    "         Serial Number     = 987654321042")))
        .forEach(
            (chr, explan) -> {
              explanation.clear();
              assertEquals(
                  chr, CardHolderReference.explainChr(Hex.toByteArray(chr), explanation, report));
              assertEquals(explan, explanation);
              assertTrue(report.isEmpty());
            }); // end forEach(chr -> ...)
    // end --- b.

    // --- c. ICCSN with non-decimal digits
    Map.ofEntries(
            // infimum supported length
            Map.entry(
                "12ab_80-a76-12345-98",
                List.of(
                    "       Discretionary data  = 12ab",
                    "       ICCSN               = 80a761234598",
                    "         Major Industry ID = 80",
                    "         Country Code      = a76",
                    "         Issuer Identifier = 12345",
                    "         Serial Number     = 98")),
            // longer than regular
            Map.entry(
                "ffff_80-472-13524-98765432104f",
                List.of(
                    "       Discretionary data  = ffff",
                    "       ICCSN               = 804721352498765432104f",
                    "         Major Industry ID = 80",
                    "         Country Code      = 472",
                    "         Issuer Identifier = 13524",
                    "         Serial Number     = 98765432104f")))
        .forEach(
            (chr, explan) -> {
              explanation.clear();
              report.clear();
              assertEquals(
                  chr, CardHolderReference.explainChr(Hex.toByteArray(chr), explanation, report));
              assertEquals(explan, explanation);
              assertEquals(List.of("ICCSN contains non-decimal digits"), report);
            }); // end forEach(chr -> ...)
    // end --- c.

    // --- d. Major Industry Identifier not 'health-care'
    Map.ofEntries(
            // MII != 80
            Map.entry(
                "ffff_79-472-13524-987654321043",
                List.of(
                    "       Discretionary data  = ffff",
                    "       ICCSN               = 7947213524987654321043",
                    "         Major Industry ID = 79",
                    "         Country Code      = 472",
                    "         Issuer Identifier = 13524",
                    "         Serial Number     = 987654321043")))
        .forEach(
            (chr, explan) -> {
              explanation.clear();
              report.clear();
              assertEquals(
                  chr, CardHolderReference.explainChr(Hex.toByteArray(chr), explanation, report));
              assertEquals(explan, explanation);
              assertEquals(
                  List.of("Major Industry Identifier does not indicate \"health-care\""), report);
            }); // end forEach(chr -> ...)
    // end --- d.
  } // end method */

  /** Test method for {@link CardHolderReference#getExplanation()}. */
  @Test
  void test_getExplanation() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. check for unmodifiable list
    final CardHolderReference dut =
        new CardHolderReference(
            (PrimitiveBerTlv) BerTlv.getInstance("5f20 0c 1000_80-274-54321-9876543210"));
    final List<String> list = dut.getExplanation();
    assertEquals(7, list.size());

    final Throwable throwable =
        assertThrows(UnsupportedOperationException.class, () -> list.add("test_getExplanation"));
    assertNull(throwable.getMessage());
    assertNull(throwable.getCause());
  } // end method */

  /** Test method for {@link CardHolderReference#getHumanReadable()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_getHumanReadable() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke tests
    final var map =
        Map.ofEntries(
            Map.entry("12ab_80-276-12345-9786543210", "12ab_80-276-12345-9786543210"),
            Map.entry("415a61787a_1-0-02-24", "AZaxz_1-0-02-24"));
    for (final var entry : map.entrySet()) {
      final var chr = entry.getKey();
      final var expected = entry.getValue();
      final CardHolderReference dut =
          new CardHolderReference((PrimitiveBerTlv) BerTlv.getInstance(TAG_CHR, chr));

      final var present = dut.getHumanReadable();

      assertEquals(expected, present);
    } // end For (entry...)
  } // end method */

  /** Test method for {@link CardHolderReference#getReport()}. */
  @Test
  void test_getReport() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. check for unmodifiable list
    final CardHolderReference dut =
        new CardHolderReference(
            (PrimitiveBerTlv) BerTlv.getInstance("5f20 0c 1000_80-274-54321-987654320a"));
    final List<String> list = dut.getReport();
    assertEquals(1, list.size());

    final Throwable throwable =
        assertThrows(UnsupportedOperationException.class, () -> list.add("test_getReport"));
    assertNull(throwable.getMessage());
    assertNull(throwable.getCause());
  } // end method */

  /** Test method for {@link CardHolderReference#hasCriticalFindings()}. */
  @Test
  void hasCriticalFindings() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test without critical findings
    // --- b. smoke test with critical finding

    // --- a. smoke test without critical findings
    assertFalse(
        new CardHolderReference(
                (PrimitiveBerTlv) BerTlv.getInstance("5f20 0c 1000_80-274-54321-987654320a"))
            .hasCriticalFindings());

    // --- b. smoke test with critical finding
    assertTrue(
        new CardHolderReference(
                (PrimitiveBerTlv) BerTlv.getInstance("5f20 08 405a61787a_1-0-02-24"))
            .hasCriticalFindings());
  } // end method */
} // end class
