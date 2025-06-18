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

import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CED;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CXD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link CertificateDate}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestCertificateDate {

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

  /** Test method for {@link CertificateDate#CertificateDate(boolean, LocalDate)}. */
  @Test
  void test_CertificateDate__boolean_LocalDate() {
    // Assertion:
    // ... a. underlying CertificateDate(PrimitiveBerTlv)-constructor works as
    //        expected

    // Note: Simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test for CED
    // --- b. smoke test for CXD

    // --- a. smoke test for CED
    {
      final CertificateDate dut = new CertificateDate(true, LocalDate.of(2045, 12, 30));
      assertEquals(
          List.of("Certificate Effective  Date       CED = 040501020300     => 30. Dezember 2045"),
          dut.getExplanation());
    } // end --- a.

    // --- b. smoke test for CXD
    {
      final CertificateDate dut = new CertificateDate(false, LocalDate.of(2054, 3, 12));
      assertEquals(
          List.of("Certificate Expiration Date       CXD = 050400030102     => 12. März 2054"),
          dut.getExplanation());
    } // end --- b.
  } // end method */

  /** Test method for {@link CertificateDate#CertificateDate(PrimitiveBerTlv)}. */
  @Test
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  void test_CertificateDate__PrimitiveBerTlv() {
    // Test strategy:
    // --- a. smoke test for CED and CXD
    // --- b. ERROR: wrong length
    // --- c. ERROR: correct length == 6, but invalid octets
    // --- d. ERROR: correct length == 6, valid octets, not normalized
    // --- e. correct length == 6, valid octets, normalized, all possible values

    // --- a. smoke test for CED and CXD
    // a.1 CED
    // a.2 CXD
    {
      // a.1 CED
      final CertificateDate ced =
          new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CED, "040501020300"));
      assertFalse(ced.hasCriticalFindings());
      assertEquals(
          List.of("Certificate Effective  Date       CED = 040501020300     => 30. Dezember 2045"),
          ced.getExplanation());
      assertTrue(ced.getReport().isEmpty());
      assertEquals(LocalDate.of(2045, 12, 30), ced.getDate());
      assertEquals("30. Dezember 2045", ced.getHumanReadable());

      // a.2 CXD
      final CertificateDate cxd =
          new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CXD, "050400030102"));
      assertFalse(cxd.hasCriticalFindings());
      assertEquals(
          List.of("Certificate Expiration Date       CXD = 050400030102     => 12. März 2054"),
          cxd.getExplanation());
      assertTrue(cxd.getReport().isEmpty());
      assertEquals(LocalDate.of(2054, 3, 12), cxd.getDate());
      assertEquals("12. März 2054", cxd.getHumanReadable());
    } // end --- a.

    // --- b. ERROR: wrong length
    RNG.intsClosed(0, 10, 6)
        .filter(dateLength -> 6 != dateLength)
        .forEach(
            dateLength -> {
              final byte[] date = RNG.nextBytes(dateLength);
              final CertificateDate dut =
                  new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CXD, date));
              assertTrue(dut.hasCriticalFindings());
              assertEquals(
                  List.of(
                      String.format(
                          "Certificate Expiration Date       CXD = %-16s", Hex.toHexDigits(date))),
                  dut.getExplanation());
              assertEquals(
                  List.of("Certificate Expiration Date       CXD has an invalid length"),
                  dut.getReport());
              assertEquals(CertificateDate.DEFAULT_DATE, dut.getDate());
              assertEquals(Hex.toHexDigits(date), dut.getHumanReadable());
            }); // end forEach(dateLength -> ...)
    // end --- b.

    // --- c. ERROR: correct length == 6, but invalid octets
    { // loop over all bytes
      for (int index = 6; index-- > 0; ) { // NOPMD assignment in operand
        // get a valid random date
        final int year = RNG.nextIntClosed(0, 99);
        final int month = RNG.nextIntClosed(0, 12);
        final int day = RNG.nextIntClosed(0, 28);
        final byte[] date =
            Hex.toByteArray(
                String.format(
                    "%02d%02d%02d%02d%02d%02d",
                    year / 10, year % 10, month / 10, month % 10, day / 10, day % 10));

        // loop over all possible values for a byte
        for (int i = 255; i-- > 10; ) { // NOPMD assignment in operand
          date[index] = (byte) i;

          final CertificateDate dut =
              new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CED, date));
          assertTrue(dut.hasCriticalFindings());
          assertEquals(
              List.of(
                  String.format(
                      "Certificate Effective  Date       CED = %-16s", Hex.toHexDigits(date))),
              dut.getExplanation());
          assertEquals(
              List.of("Certificate Effective  Date       CED has invalid digits"), dut.getReport());
          assertEquals(CertificateDate.DEFAULT_DATE, dut.getDate());
          assertEquals(Hex.toHexDigits(date), dut.getHumanReadable());
        } // end For (i...)
      } // end For (index...)
    } // end --- c.

    // --- d. ERROR: correct length == 6, valid octets, not normalized
    List.of(
            "0207 0002 0209", // 2027-02-29, not a leap year, but 29th February
            "0203 0004 0301", // 2023-04-31, there never is a 31st day in April
            "0500 0000 0206", // 2050-00-26, there never is a 0th month
            "0302 0103 0109" // 2032-13-19, there never is a 13th month
            )
        .forEach(
            input -> {
              final byte[] date = Hex.toByteArray(input);
              final CertificateDate dut =
                  new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CED, date));
              assertTrue(dut.hasCriticalFindings(), input);
              assertEquals(
                  List.of(
                      String.format(
                          "Certificate Effective  Date       CED = %-16s", Hex.toHexDigits(date))),
                  dut.getExplanation());
              assertEquals(
                  List.of("Certificate Effective  Date       CED is not normalized"),
                  dut.getReport());
              assertEquals(CertificateDate.DEFAULT_DATE, dut.getDate());
              assertEquals(Hex.toHexDigits(date), dut.getHumanReadable());
            }); // end forEach(input -> ...)
    // end --- d.

    // --- e. correct length == 6, valid octets, normalized, all possible values
    {
      final LocalDate infimum = LocalDate.of(2000, 1, 1); // smallest supported date
      final LocalDate supremum = LocalDate.of(2099, 12, 31); // greatest supported date
      for (LocalDate loDate = infimum; !loDate.isEqual(supremum); loDate = loDate.plusDays(1)) {
        final int day = loDate.getDayOfMonth();
        final int month = loDate.getMonthValue();
        final int year = loDate.getYear() - 2000;
        final String date =
            String.format(
                "%02d%02d%02d%02d%02d%02d",
                year / 10, year % 10, month / 10, month % 10, day / 10, day % 10);
        final CertificateDate ced =
            new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CED, date));
        assertEquals(loDate, ced.getDate());
        assertEquals(loDate.format(CertificateDate.FORMAT_DE), ced.getHumanReadable());
        assertFalse(ced.hasCriticalFindings());
        assertEquals(
            List.of(
                String.format(
                    "Certificate Effective  Date       CED = %s     => %s",
                    date, ced.getHumanReadable())),
            ced.getExplanation());
        assertTrue(ced.getReport().isEmpty());
      } // end For (localDate...)
    } // end --- e.
  } // end method */

  /** Test method for {@link CertificateDate#getExplanation()}. */
  @Test
  void test_getExplanation() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. check for unmodifiable list
    final CertificateDate dut =
        new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CED, "060701020300"));
    final List<String> list = dut.getExplanation();
    assertEquals(1, list.size());

    final Throwable throwable =
        assertThrows(UnsupportedOperationException.class, () -> list.add("test_getExplanation"));
    assertNull(throwable.getMessage());
    assertNull(throwable.getCause());
  } // end method */

  /** Test method for {@link CertificateDate#getHumanReadable()}. */
  @Test
  void getHumanReadable() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test, valid date
    // --- b. smoke test, invalid date

    // --- a. smoke test, valid date
    assertEquals(
        "03. Dezember 2045",
        new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CED, "040501020003"))
            .getHumanReadable());
    // --- b. smoke test, invalid date
    assertEquals(
        "040501020303",
        new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CED, "040501020303"))
            .getHumanReadable());
  } // end method */

  /** Test method for {@link CertificateDate#getReport()}. */
  @Test
  void test_getReport() {
    // Assertions:
    // ... a. constructor works as expected

    // Note: Simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. check for unmodifiable list
    final CertificateDate dut =
        new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CXD, "040501020009"));
    final List<String> list = dut.getReport();
    assertEquals(0, list.size());

    final Throwable throwable =
        assertThrows(UnsupportedOperationException.class, () -> list.add("test_getReport"));
    assertNull(throwable.getMessage());
    assertNull(throwable.getCause());
  } // end method */

  /** Test method for {@link CertificateDate#hasCriticalFindings()}. */
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
        new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CXD, "040501020008"))
            .hasCriticalFindings());

    // --- b. smoke test with critical finding
    assertTrue(
        new CertificateDate((PrimitiveBerTlv) BerTlv.getInstance(TAG_CXD, "040501030008"))
            .hasCriticalFindings());
  } // end method */
} // end class
