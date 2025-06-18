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
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractDataObject.TAG_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractDataObject.TAG_SUPREMUM;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.smartcards.utils.AfiRng;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for testing abstract class {@link AbstractDataObject}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestAbstractDataObject {

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

  /** Test method for {@link AbstractDataObject#AbstractDataObject(int, int, int, int)}. */
  @Test
  void test_AbstractDataObject__int_int_int_int() {
    // Assertions:
    // ... a. constructor from superclasses work as expected
    // ... b. checkTag()-method works as expected

    // Note: Because of assertions we can be lazy here
    //       and concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: invalid tag

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int tag = RNG.nextIntClosed(TAG_INFIMUM, TAG_SUPREMUM);
    final int ne = RNG.nextIntClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD);

    // --- a. smoke test
    {
      final AbstractDataObject dut = new MyDataObject(cla, ins, tag, ne);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(tag >> 8, dut.getP1());
      assertEquals(tag & 0xff, dut.getP2());
      assertEquals(tag, dut.getTag());
      assertEquals(2, dut.getCase());
      assertEquals(ne, dut.getNe());
    } // end --- a.

    // --- b. ERROR: invalid tag
    {
      assertThrows(
          IllegalArgumentException.class, () -> new MyDataObject(cla, ins, TAG_SUPREMUM + 1, ne));
    } // end --- b.
  } // end method */

  /** Test method for {@link AbstractDataObject#AbstractDataObject(int, int, int, byte[])}. */
  @Test
  void test_AbstractDataObject__int_int_int_byteA() {
    // Assertions:
    // ... a. constructor from superclasses work as expected
    // ... b. checkTag()-method works as expected

    // Note: Because of assertions we can be lazy here
    //       and concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int tag = RNG.nextIntClosed(TAG_INFIMUM, TAG_SUPREMUM);
    final byte[] data = RNG.nextBytes(NC_INFIMUM, NC_SUPREMUM);

    // --- a. smoke test
    {
      final AbstractDataObject dut = new MyDataObject(cla, ins, tag, data);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(tag >> 8, dut.getP1());
      assertEquals(tag & 0xff, dut.getP2());
      assertEquals(tag, dut.getTag());
      assertEquals(3, dut.getCase());
      assertEquals(data.length, dut.getNc());
      assertArrayEquals(data, dut.getData());
    } // end --- a.
  } // end method */

  /** Test method for {@link AbstractDataObject#checkTag(int)}. */
  @Test
  void test_checkTag__int() {
    // Test strategy:
    // --- a. smoke test
    // --- b. border test with valid tags
    // --- c. ERROR: invalid tag

    // --- a. smoke test
    {
      assertDoesNotThrow(() -> AbstractDataObject.checkTag(TAG_INFIMUM));
    } // end --- a.

    // --- b. border test with valid tags
    RNG.intsClosed(TAG_INFIMUM, TAG_SUPREMUM, 5)
        .forEach(
            tag ->
                assertDoesNotThrow(
                    () -> AbstractDataObject.checkTag(tag))); // end forEach(tag -> ...)

    // --- c. ERROR: invalid tag
    List.of(
            TAG_INFIMUM - 1, // just below infimum
            TAG_SUPREMUM + 1 // just above supremum
            )
        .forEach(
            tag ->
                assertThrows(
                    IllegalArgumentException.class, () -> AbstractDataObject.checkTag(tag)));
  } // end method */

  /** Test method for {@link AbstractDataObject#getTag()}. */
  @Test
  void test_getTag() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all relevant values for tag

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final byte[] data = RNG.nextBytes(NC_INFIMUM, NC_SUPREMUM);
    final int ne = RNG.nextIntClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD);

    // --- a. smoke test
    {
      final int tag = RNG.nextIntClosed(TAG_INFIMUM, TAG_SUPREMUM);

      assertEquals(tag, new MyDataObject(cla, ins, tag, ne).getTag());
      assertEquals(tag, new MyDataObject(cla, ins, tag, data).getTag());
    } // end --- a.

    // --- b. loop over all relevant values for tag
    IntStream.rangeClosed(TAG_INFIMUM, TAG_SUPREMUM)
        .forEach(
            tag -> {
              assertEquals(tag, new MyDataObject(cla, ins, tag, ne).getTag());
              assertEquals(tag, new MyDataObject(cla, ins, tag, data).getTag());
            }); // end forEach(tag -> ...)
  } // end method */
} // end class
