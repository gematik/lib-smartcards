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

import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CPI;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link CertificateProfileIndicator}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestCertificateProfileIndicator {

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

  /** Test method for {@link CertificateProfileIndicator#CertificateProfileIndicator()}. */
  @Test
  void test_CertificateProfileIndicator() {
    // Assertions:
    // ... a. underlying constructor test_CertificateProfileIndicator(PrimitiveBerTlv)
    //        works as expected

    // Note: Simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final CertificateProfileIndicator dut = new CertificateProfileIndicator();
    assertEquals(TAG_CPI, dut.getDataObject().getTag());
    assertEquals("70", Hex.toHexDigits(dut.getValue()));
  } // end method */

  /** Tests {@link CertificateProfileIndicator#CertificateProfileIndicator(PrimitiveBerTlv)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_CertificateProfileIndicator__PrimitiveBerTlv() {
    // Test strategy:
    // --- a. loop over a bunch of tags
    // --- b. lengthValueField == 0
    // --- c. lengthValueField == 1
    // --- d. lengthValueField > 1

    // --- a. loop over a bunch of tags
    for (final var tag : List.of(TAG_CPI, 0x80)) {
      // --- b. lengthValueField == 0
      {
        final byte[] value = AfiUtils.EMPTY_OS;
        final PrimitiveBerTlv input = (PrimitiveBerTlv) BerTlv.getInstance(tag, value);
        final CertificateProfileIndicator dut = new CertificateProfileIndicator(input);
        assertEquals(List.of("Certificate Profile Indicator     CPI = "), dut.getExplanation());
        assertEquals(List.of("CPI value is unknown"), dut.getReport());
        assertTrue(dut.hasCriticalFindings());
      } // end --- b.

      // --- c. lengthValueField == 1
      for (int cpi = 256; cpi-- > 0; ) { // NOPMD assignment in operand
        final byte[] value = new byte[] {(byte) (cpi & 0xff)};
        final PrimitiveBerTlv input = (PrimitiveBerTlv) BerTlv.getInstance(tag, value);
        final CertificateProfileIndicator dut = new CertificateProfileIndicator(input);

        if (0x70 == cpi) { // NOPMD literal in if statement
          // ... CPI is according to [gemSpec_PKI#GS-A_4987]
          assertEquals(
              List.of(
                  String.format(
                      "Certificate Profile Indicator     CPI = %s => "
                          + "self descriptive Card Verifiable Certificate",
                      Hex.toHexDigits(value))),
              dut.getExplanation());
          assertTrue(dut.getReport().isEmpty());
          assertFalse(dut.hasCriticalFindings());
        } else {
          // ... CPI is NOT in accordance to [gemSpec_PKI#GS-A_4987]
          assertEquals(
              List.of(
                  String.format(
                      "Certificate Profile Indicator     CPI = %s", Hex.toHexDigits(value))),
              dut.getExplanation());
          assertEquals(List.of("CPI value is unknown"), dut.getReport());
          assertTrue(dut.hasCriticalFindings());
        } // end else (cpi == '70')
      } // end For (cpi...)
      // end --- c.

      // --- d. lengthValueField > 1
      RNG.intsClosed(2, 10, 5)
          .forEach(
              length -> {
                final byte[] value = RNG.nextBytes(length);
                final PrimitiveBerTlv input = (PrimitiveBerTlv) BerTlv.getInstance(tag, value);
                final CertificateProfileIndicator dut = new CertificateProfileIndicator(input);
                assertEquals(
                    List.of(
                        String.format(
                            "Certificate Profile Indicator     CPI = %s", Hex.toHexDigits(value))),
                    dut.getExplanation());
                assertEquals(List.of("CPI value is unknown"), dut.getReport());
                assertTrue(dut.hasCriticalFindings());
              }); // end forEach(length -> ...)
    } // end For (tag...)
  } // end method */

  /** Test method for {@link CertificateProfileIndicator#getExplanation()}. */
  @Test
  void test_getExplanation() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. check for unmodifiable list
    final CertificateProfileIndicator dut =
        new CertificateProfileIndicator((PrimitiveBerTlv) BerTlv.getInstance("5f29 01 70"));
    final List<String> list = dut.getExplanation();
    assertEquals(1, list.size());

    final Throwable throwable =
        assertThrows(UnsupportedOperationException.class, () -> list.add("test_getExplanation"));
    assertNull(throwable.getMessage());
    assertNull(throwable.getCause());
  } // end method */

  /** Test method for {@link CertificateProfileIndicator#getReport()}. */
  @Test
  void test_getReport() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. check for unmodifiable list
    final CertificateProfileIndicator dut =
        new CertificateProfileIndicator((PrimitiveBerTlv) BerTlv.getInstance("5f29 01 71"));
    final List<String> list = dut.getReport();
    assertEquals(1, list.size());

    final Throwable throwable =
        assertThrows(UnsupportedOperationException.class, () -> list.add("test_getReport"));
    assertNull(throwable.getMessage());
    assertNull(throwable.getCause());
  } // end method */

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
        new CertificateProfileIndicator((PrimitiveBerTlv) BerTlv.getInstance("5f29-01-70"))
            .hasCriticalFindings());

    // --- b. smoke test with critical finding
    assertTrue(
        new CertificateProfileIndicator((PrimitiveBerTlv) BerTlv.getInstance("5f29-01-71"))
            .hasCriticalFindings());
  } // end method */
} // end class
