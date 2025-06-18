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

import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link CertificationAuthorityReference}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestCertificationAuthorityReference {

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

  /** Test {@link CertificationAuthorityReference#CertificationAuthorityReference(String)}. */
  @Test
  void test_CertificationAuthorityReference__String() {
    // Assertions:
    // ... a. underlying constructor CertificationAuthorityReference(PrimitiveBerTlv)
    //        works as expected

    // Note: Simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final String input = "415a61787a_1-0-02-25";
    final CertificationAuthorityReference dut = new CertificationAuthorityReference(input);
    assertEquals(TAG_CAR, dut.getDataObject().getTag());
    assertEquals("AZaxz_1-0-02-25", dut.getHumanReadable());
  } // end method */

  /**
   * Test method for {@link
   * CertificationAuthorityReference#CertificationAuthorityReference(PrimitiveBerTlv)}.
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_CertificationAuthorityReference__PrimitiveBerTlv() {
    // Assertions:
    // ... a. constructors from superclass work as expected

    // Test strategy:
    // --- a. smoke tests with non-critical findings
    // --- b. appropriate length, but critical findings
    // --- c. inappropriate length

    // --- a. smoke tests with non-critical findings
    {
      // car mapped to List.of(carHumanReadable, explanation, serviceIndicator, year)
      final var map =
          Map.ofEntries(
              Map.entry(
                  "415a61787a_1-0-02-24",
                  List.of(
                      "AZaxz_1-0-02-24",
                      List.of(
                          "Certification Authority Reference CAR = 415a61787a100224",
                          "       CA-Identifier       = AZaxz",
                          "       service indicator   = 1",
                          "       discretionary data  = 0",
                          "       algorithm reference = 02",
                          "       generation year     = 24"),
                      1,
                      2024)));
      for (final var entry : map.entrySet()) {
        final var car = entry.getKey();
        final var expected = entry.getValue();
        final String humanReadable = (String) expected.get(0);
        final List<?> explanation = (List<?>) expected.get(1);
        final int serviceIndicator = (int) expected.get(2);
        final int year = (int) expected.get(3);

        final CertificationAuthorityReference dut =
            new CertificationAuthorityReference((PrimitiveBerTlv) BerTlv.getInstance(0x8a, car));

        assertEquals(Hex.extractHexDigits(car), Hex.toHexDigits(dut.getValue()));
        assertEquals(humanReadable, dut.getHumanReadable());
        assertEquals(explanation, dut.getExplanation());
        assertEquals(serviceIndicator, dut.getServiceIndicator());
        assertEquals(year, dut.getGenerationYear());
        assertFalse(dut.hasCriticalFindings());
        assertTrue(dut.getReport().isEmpty());
      } // end For (entry...)
    } // end --- a.
    // end --- a.

    // --- b. appropriate length, but critical findings
    // car mapped to List.of(carHumanReadable, explanation, report)
    Map.ofEntries(
            Map.entry(
                "405a61787a_1-0-02-26",
                List.of(
                    "@Zaxz_1-0-02-26",
                    List.of(
                        "Certification Authority Reference CAR = 405a61787a100226",
                        "       CA-Identifier       = @Zaxz",
                        "       service indicator   = 1",
                        "       discretionary data  = 0",
                        "       algorithm reference = 02",
                        "       generation year     = 26"),
                    List.of("CA name contains non-alphabetic characters"))))
        .forEach(
            (car, expected) -> {
              final String humanReadable = (String) expected.get(0);
              final List<?> explanation = (List<?>) expected.get(1);
              final List<?> report = (List<?>) expected.get(2);
              final int serviceIndicator = -1;
              final int year = -1;

              final CertificationAuthorityReference dut =
                  new CertificationAuthorityReference(
                      (PrimitiveBerTlv) BerTlv.getInstance(0x8b, car));
              assertEquals(Hex.extractHexDigits(car), Hex.toHexDigits(dut.getValue()));
              assertEquals(humanReadable, dut.getHumanReadable());
              assertEquals(explanation, dut.getExplanation());
              assertEquals(serviceIndicator, dut.getServiceIndicator());
              assertEquals(year, dut.getGenerationYear());
              assertTrue(dut.hasCriticalFindings());
              assertEquals(report, dut.getReport());
            }); // end forEach((car, expected) -> ...)
    // end --- b.

    // --- c. inappropriate length
    // car mapped to List.of(carHumanReadable, explanation, report)
    Map.ofEntries(
            Map.entry(
                "425a61787a_1-0-02-2642",
                List.of(
                    "425a61787a10022642",
                    List.of("Certification Authority Reference CAR = 425a61787a10022642"),
                    List.of("CAR has an invalid length"))))
        .forEach(
            (car, expected) -> {
              final String humanReadable = (String) expected.get(0);
              final List<?> explanation = (List<?>) expected.get(1);
              final List<?> report = (List<?>) expected.get(2);
              final int serviceIndicator = -1;
              final int year = -1;

              final CertificationAuthorityReference dut =
                  new CertificationAuthorityReference(
                      (PrimitiveBerTlv) BerTlv.getInstance(0x8b, car));
              assertEquals(Hex.extractHexDigits(car), Hex.toHexDigits(dut.getValue()));
              assertEquals(humanReadable, dut.getHumanReadable());
              assertEquals(explanation, dut.getExplanation());
              assertEquals(serviceIndicator, dut.getServiceIndicator());
              assertEquals(year, dut.getGenerationYear());
              assertTrue(dut.hasCriticalFindings());
              assertEquals(report, dut.getReport());
            }); // end forEach((car, expected) -> ...)
    // end --- c.
  } // end method */

  /** Test method for {@link CertificationAuthorityReference#getGenerationYear()}. */
  @Test
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  void test_getGenerationYear() {
    // Assertions:
    // ... a. constructor works as expected

    // Test strategy:
    // --- a. smoke test with some years
    for (final var year : RNG.intsClosed(0, 99, 20).boxed().toList()) {
      assertEquals(
          CertificationAuthorityReference.OFFSET_YEAR + year,
          new CertificationAuthorityReference(
                  (PrimitiveBerTlv)
                      BerTlv.getInstance(TAG_CAR, String.format("415a61787a_1-0-02-%02d", year)))
              .getGenerationYear());
    } // end For (year...)
  } // end method */

  /** Test method for {@link CertificationAuthorityReference#getReport()}. */
  @Test
  void test_getReport() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. check for unmodifiable list
    final CardHolderReference dut =
        new CertificationAuthorityReference(
            (PrimitiveBerTlv) BerTlv.getInstance("42 09 415a61787a_8-0-02-2442"));
    final List<String> list = dut.getReport();
    assertEquals(1, list.size());

    final Throwable throwable =
        assertThrows(UnsupportedOperationException.class, () -> list.add("test_getReport"));
    assertNull(throwable.getMessage());
    assertNull(throwable.getCause());
  } // end method */

  /** Test method for {@link CertificationAuthorityReference#getServiceIndicator()}. */
  @Test
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  void test_getServiceIndicator() {
    // Assertions:
    // ... a. constructor works as expected

    // Test strategy:
    // --- a. smoke test with all allowed values
    for (final int serviceIndicator : new int[] {1, 8}) {
      assertEquals(
          serviceIndicator,
          new CertificationAuthorityReference(
                  (PrimitiveBerTlv)
                      BerTlv.getInstance(
                          TAG_CAR, String.format("415a61787a_%d-0-02-17", serviceIndicator)))
              .getServiceIndicator());
    } // end For (serviceIndicator...)
  } // end method */

  /** Test method for {@link CertificationAuthorityReference#hasCriticalFindings()}. */
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
        new CertificationAuthorityReference(
                (PrimitiveBerTlv) BerTlv.getInstance("42 08 415a61787a_8-0-02-32"))
            .hasCriticalFindings());

    // --- b. smoke test with critical finding
    assertTrue(
        new CertificationAuthorityReference(
                (PrimitiveBerTlv) BerTlv.getInstance("42 08 415a61787a_0-0-02-32"))
            .hasCriticalFindings());
  } // end method */
} // end class
