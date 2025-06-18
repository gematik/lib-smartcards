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
package de.gematik.smartcards.sdcom.apdu.isoiec7816apdu;

import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NC_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NC_SUPREMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_EXTENDED_WILDCARD;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_ABSENT;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_SUPREMUM;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractRecord.RECORD_NUMBER_ABSENT;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractRecord.RECORD_NUMBER_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractRecord.RECORD_NUMBER_SUPREMUM;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.utils.AfiRng;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for testing abstract class {@link AbstractRecord}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestAbstractRecord {

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
    // intentionally empty
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /** Test method for {@link AbstractRecord#AbstractRecord(int, int, int, int, int, int)}. */
  @Test
  void test_AbstractRecord__int_int_int_int_int_int() {
    // Assertions:
    // ... a. constructor from superclasses work as expected
    // ... b. checkP1P2()-method works as expected

    // Note: Because of assertions we can be lazy here
    //       and concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: invalid recordNumber

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int recNo = RNG.nextIntClosed(RECORD_NUMBER_INFIMUM, RECORD_NUMBER_SUPREMUM);
    final int sfi = RNG.nextIntClosed(SFI_ABSENT, SFI_SUPREMUM);
    final int b3b2b1 = RNG.nextIntClosed(0, 7);
    final int ne = RNG.nextIntClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD);

    // --- a. smoke test
    {
      final AbstractRecord dut = new MyRecord(cla, ins, recNo, sfi, b3b2b1, ne);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(recNo, dut.getP1());
      assertEquals((sfi << 3) | b3b2b1, dut.getP2());
      assertEquals(recNo, dut.getRecordNumber());
      assertEquals(sfi, dut.getSfi());
      assertEquals(2, dut.getCase());
      assertEquals(ne, dut.getNe());
    } // end --- a.

    // --- b. ERROR: invalid recordNumber
    {
      assertThrows(
          IllegalArgumentException.class,
          () -> new MyRecord(cla, ins, RECORD_NUMBER_ABSENT, sfi, b3b2b1, ne));
    } // end --- b.
  } // end method */

  /** Test method for {@link AbstractRecord#AbstractRecord(int, int, int, int, int, byte[])}. */
  @Test
  void test_AbstractRecord__int_int_int_int_int_byteA() {
    // Assertions:
    // ... a. constructor from superclasses work as expected
    // ... b. checkP1P2()-method works as expected

    // Note: Because of assertions we can be lazy here
    //       and concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int recNo = RNG.nextIntClosed(RECORD_NUMBER_ABSENT, RECORD_NUMBER_SUPREMUM);
    final int sfi = RNG.nextIntClosed(SFI_ABSENT, SFI_SUPREMUM);
    final int b3b2b1 = RNG.nextIntClosed(0, 7);
    final byte[] data = RNG.nextBytes(NC_INFIMUM, NC_SUPREMUM);

    // --- a. smoke test
    {
      final AbstractRecord dut = new MyRecord(cla, ins, recNo, sfi, b3b2b1, data);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(recNo, dut.getP1());
      assertEquals((sfi << 3) | b3b2b1, dut.getP2());
      assertEquals(recNo, dut.getRecordNumber());
      assertEquals(sfi, dut.getSfi());
      assertEquals(3, dut.getCase());
      assertEquals(data.length, dut.getNc());
      assertArrayEquals(data, dut.getData());
    } // end --- a.
  } // end method */

  /** Test method for {@link AbstractRecord#checkP1P2(int, int, int)}. */
  @Test
  void test_checkP1P2__int_int_int() {
    // Test strategy:
    // --- a. smoke test
    // --- b. border test recordNumber
    // --- c. border test SFI
    // --- d. border test bits b3 b2 b1
    // --- e. ERROR: invalid recordNumber
    // --- f. ERROR: invalid SFI
    // --- g. ERROR: invalid bits b3 b2 b1

    // --- a. smoke test
    {
      assertDoesNotThrow(() -> AbstractRecord.checkP1P2(1, 2, 3));
    } // end --- a.

    // --- b. border test recordNumber
    // --- c. border test SFI
    // --- d. border test bits b3 b2 b1
    RNG.intsClosed(RECORD_NUMBER_ABSENT, RECORD_NUMBER_SUPREMUM, 5)
        .forEach(
            recordNumber -> {
              RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5)
                  .forEach(
                      sfi -> {
                        RNG.intsClosed(0, 7, 5)
                            .forEach(
                                b3b2b1 ->
                                    assertDoesNotThrow(
                                        () ->
                                            AbstractRecord.checkP1P2(
                                                recordNumber,
                                                sfi,
                                                b3b2b1))); // end forEach(b3b2b1 -> ...)
                      }); // end forEach(sfi -> ...)
            }); // end forEach(recordNumber -> ...)
    // end --- b, c, d

    // --- e. ERROR: invalid recordNumber
    List.of(
            RECORD_NUMBER_ABSENT - 1, // just below infimum
            RECORD_NUMBER_SUPREMUM + 1 // just above supremum
            )
        .forEach(
            recordNumber -> {
              RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5)
                  .forEach(
                      sfi -> {
                        RNG.intsClosed(0, 7, 5)
                            .forEach(
                                b3b2b1 ->
                                    assertThrows(
                                        IllegalArgumentException.class,
                                        () -> AbstractRecord.checkP1P2(recordNumber, sfi, b3b2b1),
                                        () ->
                                            Integer.toString(
                                                recordNumber))); // end forEach(b3b2b1 -> ...)
                      }); // end forEach(sfi -> ...)
            }); // end forEach(recordNumber -> ...)
    // end --- e.

    // --- f. ERROR: invalid SFI
    RNG.intsClosed(RECORD_NUMBER_ABSENT, RECORD_NUMBER_SUPREMUM, 5)
        .forEach(
            recordNumber -> {
              List.of(
                      SFI_ABSENT - 1, // just below infimum
                      SFI_SUPREMUM + 1 // just above supremum
                      )
                  .forEach(
                      sfi -> {
                        RNG.intsClosed(0, 7, 5)
                            .forEach(
                                b3b2b1 ->
                                    assertThrows(
                                        IllegalArgumentException.class,
                                        () -> AbstractRecord.checkP1P2(recordNumber, sfi, b3b2b1),
                                        () ->
                                            String.format(
                                                "SFI = %d", sfi))); // end forEach(b3b2b1 -> ...)
                      }); // end forEach(sfi -> ...)
            }); // end forEach(recordNumber -> ...)
    // end --- f.

    // --- g. ERROR: invalid bits b3 b2 b1
    RNG.intsClosed(RECORD_NUMBER_ABSENT, RECORD_NUMBER_SUPREMUM, 5)
        .forEach(
            recordNumber -> {
              RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5)
                  .forEach(
                      sfi -> {
                        List.of(
                                -1, // just below infimum
                                8 // just above supremum
                                )
                            .forEach(
                                b3b2b1 ->
                                    assertThrows(
                                        IllegalArgumentException.class,
                                        () ->
                                            AbstractRecord.checkP1P2(
                                                recordNumber,
                                                sfi,
                                                b3b2b1))); // end forEach(b3b2b1 -> ...)
                      }); // end forEach(sfi -> ...)
            }); // end forEach(recordNumber -> ...)
    // end --- g.
  } // end method */

  /** Test method for {@link AbstractBinary#explainTrailer(int)}. */
  @Test
  void test_explainTrailer() {
    // Note 1: The meaning of a certain trailer-value is typically standardised
    //         in an ISO-standard or specified in a specification.
    //         Thus, the correct explanation has to be ensured by manual reviews
    //         rather than unit test. It follows that the JUnit-test here
    //         concentrates on code-coverage.
    // Note 2: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. explanation not empty
    // --- b. check UpdateRetryWarning
    // --- c. check that some trailers are explained by superclass

    final CommandApdu dut = new MyRecord(0, 1, 2, 3, 4, 5);

    // --- a. explanation not empty
    List.of(0x6282, 0x6287, 0x6700)
        .forEach(trailer -> assertFalse(dut.explainTrailer(trailer).isEmpty()));

    // --- b. check UpdateRetryWarning
    assertEquals("UpdateRetryWarning: at least 15 retries", dut.explainTrailer(0x63cf));
    IntStream.rangeClosed(0, 14)
        .forEach(
            retry ->
                assertEquals(
                    "UpdateRetryWarning: " + retry + " retries",
                    dut.explainTrailer(0x63c0 + retry)));

    // --- c. check that some trailers are explained by superclass
    assertEquals("NoError", dut.explainTrailer(0x9000));
  } // end method */

  /**
   * Test method for getter.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link AbstractBinary#getSfi()},
   *   <li>{@link AbstractBinary#getOffset()}.
   * </ol>
   */
  @Test
  void test_getter() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all possible values for recordNumber
    // --- c. loop over relevant values for SFI

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int recNo = RNG.nextIntClosed(0, 0xfe);
    final int b3b2b1 = RNG.nextIntClosed(0, 7);
    final byte[] data = RNG.nextBytes(NC_INFIMUM, NC_SUPREMUM);

    // --- a. smoke test
    {
      final int sfi = RNG.nextIntClosed(SFI_ABSENT, SFI_SUPREMUM);
      final AbstractRecord dut = new MyRecord(cla, ins, recNo, sfi, b3b2b1, data);

      assertEquals(recNo, dut.getRecordNumber());
      assertEquals(sfi, dut.getSfi());
    } // end --- a.

    // --- b. loop over all possible values for recordNumber
    // --- c. loop over relevant values for SFI
    RNG.intsClosed(RECORD_NUMBER_ABSENT, RECORD_NUMBER_SUPREMUM, 5)
        .forEach(
            recordNumber -> {
              RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5)
                  .forEach(
                      sfi -> {
                        final AbstractRecord dut =
                            new MyRecord(cla, ins, recordNumber, sfi, b3b2b1, data);

                        assertEquals(recordNumber, dut.getRecordNumber());
                        assertEquals(sfi, dut.getSfi());
                      }); // end forEach(sfi -> ...)
            }); // end forEach(recordNumber -> ...)
    // end --- b, c
  } // end method */
} // end class
