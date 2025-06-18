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
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_SUPREMUM;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for testing abstract class {@link AbstractBinary}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestAbstractBinary {

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

  /** Test method for {@link AbstractBinary#AbstractBinary(int, int, int, int)}. */
  @Test
  void test_AbstractBinary__int_int_int_int() {
    // Assertions:
    // ... a. constructor from superclasses work as expected
    // ... b. checkP1P2()-method works as expected

    // Note: Because of assertions we can be lazy here
    //       and concentrate on code-coverage.

    // Test strategy:
    // --- a. loop over bunch of valid input values
    // --- b. ERROR: smoke test

    // --- a. loop over bunch of valid input values
    RNG.intsClosed(0, 0xfe, 5)
        .forEach(
            cla -> {
              RNG.intsClosed(0, 0xff, 5)
                  .forEach(
                      ins -> {
                        RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5)
                            .forEach(
                                sfi -> {
                                  RNG.intsClosed(0, (SFI_ABSENT == sfi) ? 0x7fff : 0xff, 5)
                                      .forEach(
                                          offset -> {
                                            final AbstractBinary dut =
                                                new MyBinary(cla, ins, sfi, offset);

                                            assertEquals(cla, dut.getCla());
                                            assertEquals(ins, dut.getIns());
                                            assertEquals(sfi, dut.getSfi());
                                            assertEquals(offset, dut.getOffset());
                                            assertEquals(
                                                (SFI_ABSENT == sfi) ? offset >> 8 : 0x80 | sfi,
                                                dut.getP1());
                                            assertEquals(offset & 0xff, dut.getP2());
                                            assertEquals(1, dut.getCase());
                                          }); // end forEach(offset -> ...)
                                }); // end forEach(sfi -> ...)
                      }); // end forEach(ins -> ...)
            }); // end forEach(cla -> ...)

    // --- b. ERROR: smoke test
    assertThrows(IllegalArgumentException.class, () -> new MyBinary(1, 2, 3, 256));
  } // end method */

  /** Test method for {@link AbstractBinary#AbstractBinary(int, int, int, int, int)}. */
  @Test
  void test_AbstractBinary__int_int_int_int_int() {
    // Assertions:
    // ... a. constructor from superclasses work as expected
    // ... b. checkP1P2()-method works as expected

    // Note: Because of assertions we can be lazy here
    //       and concentrate on code-coverage.

    // Test strategy:
    // --- a. loop over bunch of valid input values
    // --- b. ERROR: smoke test

    // --- a. loop over bunch of valid input values
    RNG.intsClosed(0, 0xfe, 5)
        .forEach(
            cla -> {
              RNG.intsClosed(0, 0xfe, 5)
                  .forEach(
                      ins -> {
                        RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5)
                            .forEach(
                                sfi -> {
                                  RNG.intsClosed(0, (SFI_ABSENT == sfi) ? 0x7fff : 0xff, 5)
                                      .forEach(
                                          offset -> {
                                            RNG.intsClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD, 5)
                                                .forEach(
                                                    ne -> {
                                                      final AbstractBinary dut =
                                                          new MyBinary(cla, ins, sfi, offset, ne);

                                                      assertEquals(cla, dut.getCla());
                                                      assertEquals(ins, dut.getIns());
                                                      assertEquals(sfi, dut.getSfi());
                                                      assertEquals(offset, dut.getOffset());
                                                      assertEquals(
                                                          (SFI_ABSENT == sfi)
                                                              ? offset >> 8
                                                              : 0x80 | sfi,
                                                          dut.getP1());
                                                      assertEquals(offset & 0xff, dut.getP2());
                                                      assertEquals(2, dut.getCase());
                                                      assertEquals(ne, dut.getNe());
                                                    }); // end forEach(ne -> ...)
                                          }); // end forEach(offset -> ...)
                                }); // end forEach(sfi -> ...)
                      }); // end forEach(ins -> ...)
            }); // end forEach(cla -> ...)

    // --- b. ERROR: smoke test
    assertThrows(IllegalArgumentException.class, () -> new MyBinary(1, 2, -1, 256, 0));
  } // end method */

  /** Test method for {@link AbstractBinary#AbstractBinary(int, int, int, int, byte[])}. */
  @Test
  void test_Binary__int_int_int_int_byteA() {
    // Assertions:
    // ... a. constructor from superclasses work as expected
    // ... b. checkP1P2()-method works as expected

    // Note: Because of assertions we can be lazy here
    //       and concentrate on code-coverage.

    // Test strategy:
    // --- a. loop over bunch of valid input values
    // --- b. ERROR: smoke test

    // --- a. loop over bunch of valid input values
    RNG.intsClosed(NC_INFIMUM, NC_SUPREMUM, 5)
        .forEach(
            nc -> {
              final byte[] data = RNG.nextBytes(nc);

              RNG.intsClosed(0, 0xfe, 5)
                  .forEach(
                      cla -> {
                        RNG.intsClosed(0, 0xfe, 5)
                            .forEach(
                                ins -> {
                                  RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5)
                                      .forEach(
                                          sfi -> {
                                            RNG.intsClosed(
                                                    0, (SFI_ABSENT == sfi) ? 0x7fff : 0xff, 5)
                                                .forEach(
                                                    offset -> {
                                                      final AbstractBinary dut =
                                                          new MyBinary(cla, ins, sfi, offset, data);

                                                      assertEquals(cla, dut.getCla());
                                                      assertEquals(ins, dut.getIns());
                                                      assertEquals(sfi, dut.getSfi());
                                                      assertEquals(offset, dut.getOffset());
                                                      assertEquals(
                                                          (SFI_ABSENT == sfi)
                                                              ? offset >> 8
                                                              : 0x80 | sfi,
                                                          dut.getP1());
                                                      assertEquals(offset & 0xff, dut.getP2());
                                                      assertEquals(3, dut.getCase());
                                                      assertEquals(nc, dut.getNc());
                                                      assertArrayEquals(data, dut.getData());
                                                    }); // end forEach(offset -> ...)
                                          }); // end forEach(sfi -> ...)
                                }); // end forEach(ins -> ...)
                      }); // end forEach(cla -> ...)
            }); // end forEach(nc -> ...)

    // --- b. ERROR: smoke test
    assertThrows(
        IllegalArgumentException.class, () -> new MyBinary(1, 2, 3, 256, RNG.nextBytes(1, 10)));
  } // end method */

  /** Test method for {@link AbstractBinary#checkP1P2(int, int)}. */
  @Test
  void test_checkP1P2__int_int() {
    // Test strategy:
    // --- a. border tests for SFI present
    // --- b. border tests for SFI absent
    // --- c. invalid SFI
    // --- d. offset < 0
    // --- e. offset >= 0, but invalid for SFI present
    // --- f. offset >= 0, but invalid for SFI absent

    final int offsetInfimum = 0;
    final int offsetSfiSupremum = 255;
    final int offsetSupremum = 0x7fff;

    // --- a. border tests for SFI present
    RNG.intsClosed(SFI_INFIMUM, SFI_SUPREMUM, 5)
        .forEach(
            sfi -> {
              RNG.intsClosed(offsetInfimum, offsetSfiSupremum, 5)
                  .forEach(
                      offset ->
                          assertDoesNotThrow(
                              () ->
                                  AbstractBinary.checkP1P2(
                                      sfi, offset))); // end forEach(offset -> ...)
            }); // end forEach(sfi -> ...)
    // end --- a.

    // --- b. border tests for SFI absent
    RNG.intsClosed(offsetInfimum, offsetSupremum, 5)
        .forEach(
            offset ->
                assertDoesNotThrow(
                    () ->
                        AbstractBinary.checkP1P2(
                            SFI_ABSENT, offset))); // end forEach(offset -> ...)
    // end --- b.

    // --- c. invalid SFI
    List.of(SFI_ABSENT - 1, SFI_SUPREMUM + 1)
        .forEach(
            sfi -> {
              RNG.intsClosed(offsetInfimum, offsetSupremum, 5)
                  .forEach(
                      offset -> {
                        final Throwable throwable =
                            assertThrows(
                                IllegalArgumentException.class,
                                () -> AbstractBinary.checkP1P2(sfi, offset));
                        assertEquals("invalid SFI: " + sfi, throwable.getMessage());
                        assertNull(throwable.getCause());
                      }); // end forEach(offset -> ...)
            }); // end forEach(sfi -> ...)
    // end --- c.

    // --- d. offset < 0
    RNG.intsClosed(SFI_INFIMUM, SFI_SUPREMUM, 5)
        .forEach(
            sfi -> {
              final Throwable throwable =
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> AbstractBinary.checkP1P2(sfi, offsetInfimum - 1));
              assertEquals("invalid offset: -1", throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(sfi -> ...)
    // end --- d.

    // --- e. offset >= 0, but invalid for SFI present
    RNG.intsClosed(SFI_INFIMUM, SFI_SUPREMUM, 5)
        .forEach(
            sfi -> {
              final Throwable throwable =
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> AbstractBinary.checkP1P2(sfi, offsetSfiSupremum + 1));
              assertEquals("invalid offset for SFI present: " + 256, throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(sfi -> ...)
    // end --- e.

    // --- f. offset >= 0, but invalid for SFI absent
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> AbstractBinary.checkP1P2(SFI_ABSENT, offsetSupremum + 1));
      assertEquals("invalid offset: " + 32_768, throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- f.
  } // end method */

  /** Test method for {@link CommandApdu#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. smoke test
    // --- b. smoke test with    same   class,    same   content
    // --- c. smoke test with    same   class, different content
    // --- d. smoke test with different class,    same   content

    final CommandApdu dut = new MyBinary(1, 2, 3, 4);

    // --- a. smoke test
    {
      final CommandApdu other = new MyBinary(1, 2, 3, 4);

      assertNotSame(dut, other);
      assertTrue(dut.equals(other)); // NOPMD simplify assertion
    } // end --- a.

    Map.ofEntries(
            // --- b. smoke test with    same   class,    same   content
            Map.entry(new MyBinary(1, 2, 3, 4), List.of(true, true)),
            // --- c. smoke test with    same   class, different content
            Map.entry(new MyBinary(1, 2, 3, 5), List.of(false, false)),
            // --- d. smoke test with different class,    same   content
            Map.entry(new CommandApdu(1, 2, 0x83, 4), List.of(false, true)))
        .forEach(
            (obj, result) -> {
              assertNotSame(dut, obj);
              assertEquals(result.get(0), dut.equals(obj));
              if (result.get(1)) {
                assertEquals(Hex.toHexDigits(dut.getBytes()), Hex.toHexDigits(obj.getBytes()));
              } // end fi
            }); // end forEach((obj, result) -> ...)
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

    final CommandApdu dut = new MyBinary(0, 1, 2, 3);

    // --- a. explanation not empty
    List.of(0x6a84, 0x6b00).forEach(trailer -> assertFalse(dut.explainTrailer(trailer).isEmpty()));

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
    // --- b. loop over all possible values with offset <= 255
    // --- c. loop over relevant values for SFI absent

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);

    // --- a. smoke test
    {
      final int sfi = RNG.nextIntClosed(SFI_ABSENT, SFI_SUPREMUM);
      final int offset = RNG.nextIntClosed(0, 0xff);
      final AbstractBinary dut = new MyBinary(cla, ins, sfi, offset);

      assertEquals(sfi, dut.getSfi());
      assertEquals(offset, dut.getOffset());
    } // end --- a.

    // --- b. loop over all possible values with offset <= 255
    IntStream.rangeClosed(SFI_ABSENT, SFI_SUPREMUM)
        .forEach(
            sfi -> {
              IntStream.rangeClosed(0, 255)
                  .forEach(
                      offset -> {
                        final AbstractBinary dut = new MyBinary(cla, ins, sfi, offset);
                        assertEquals(sfi, dut.getSfi());
                        assertEquals(offset, dut.getOffset());
                      }); // end forEach(offset -> ...)
            }); // end forEach(sfi -> ...)
    // end --- b.

    // --- c. loop over relevant values for SFI absent
    RNG.intsClosed(0, 0x7fff, 512)
        .forEach(
            offset -> assertEquals(offset, new MyBinary(cla, ins, SFI_ABSENT, offset).getOffset()));
    // end --- c.
  } // end method */
} // end class
