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
package de.gematik.smartcards.tlv;

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests on {@link BerTlv}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestPrimitiveSpecific {

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

  /** Test method for {@link PrimitiveSpecific#PrimitiveSpecific(long, byte[])}. */
  @Test
  void test_PrimitiveSpecific__long_byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    {
      final var expTag = RNG.nextIntClosed(0, 30);
      final var expValue = RNG.nextBytes(1, 4);

      final var dut = new MyPrimitiveSpecific(expTag, expValue);

      assertEquals(expTag, dut.getTag());
      assertArrayEquals(expValue, dut.getValueField());
    } // end --- a.
  } // end method */

  /** Test method for {@link PrimitiveSpecific#PrimitiveSpecific(byte[], InputStream)}. */
  @Test
  void test_PrimitiveSpecific__byteA_InputStream() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test

    try {
      // --- a. smoke test
      {
        final var expTag = RNG.nextIntClosed(0, 30);
        final var expValue = RNG.nextBytes(1, 4);
        final var is =
            new ByteArrayInputStream(
                Hex.toByteArray(
                    String.format("%02x%s", expValue.length, Hex.toHexDigits(expValue))));

        final var dut = new MyPrimitiveSpecific(Hex.toByteArray(String.format("%02x", expTag)), is);

        assertEquals(expTag, dut.getTag());
        assertArrayEquals(expValue, dut.getValueField());
      } // end --- a.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link PrimitiveSpecific#commentDecoded(int, byte[])}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_commentDecoded__int_byteA")
  @Test
  void test_commentDecoded__int_byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_commentDecoded__int_byteA");
  } // end method */

  /** Test method for {@link PrimitiveSpecific#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    {
      final var expTag = RNG.nextIntClosed(0, 30);
      final var expValue = RNG.nextInt();
      final var input = Hex.toByteArray(String.format("%08x", expValue));

      final var dut = new MyPrimitiveSpecific(expTag, input);

      assertEquals(expValue, dut.getDecoded());
    } // end --- a.
  } // end method */

  /** Test method for {@link PrimitiveSpecific#getFindings()}. */
  @Test
  void test_getFindings() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with no findings
    // --- b. smoke test with finding

    // --- a. smoke test with no findings
    {
      final var expTag = RNG.nextIntClosed(0, 30);
      final var expValue = RNG.nextInt();
      final var input = Hex.toByteArray(String.format("%08x", expValue));
      final var dut = new MyPrimitiveSpecific(expTag, input);

      final var present = dut.getFindings();

      assertTrue(present.isEmpty());
    } // end --- a.

    // --- b. smoke test with finding
    {
      final var expTag = RNG.nextIntClosed(0, 30);
      final var expValue = RNG.nextInt();
      final var expected = ", findings: out-of-range";
      final var input = Hex.toByteArray(String.format("01 %08x", expValue));
      final var dut = new MyPrimitiveSpecific(expTag, input);

      final var present = dut.getFindings();

      assertEquals(expected, present);
    } // end --- b.
  } // end method */

  /** Test method for {@link PrimitiveSpecific#isValid()}. */
  @Test
  void test_isValid() {
    // Assertions:
    // .. a. "getFindings()"-method works as expected

    // Test strategy:
    // --- a. smoke test with  valid  object
    // --- b. smoke test with invalid object

    // --- a. smoke test with  valid  object
    {
      final var expTag = RNG.nextIntClosed(0, 30);
      final var expValue = RNG.nextInt();
      final var input = Hex.toByteArray(String.format("%08x", expValue));
      final var dut = new MyPrimitiveSpecific(expTag, input);

      final var present = dut.isValid();

      assertTrue(present);
    } // end --- a.

    // --- b. smoke test with invalid object
    {
      final var expTag = RNG.nextIntClosed(0, 30);
      final var expValue = RNG.nextInt();
      final var input = Hex.toByteArray(String.format("01 %08x", expValue));
      final var dut = new MyPrimitiveSpecific(expTag, input);

      final var present = dut.isValid();

      assertFalse(present);
    } // end --- b.
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /**
   * Class supporting tests with the abstract class under test.
   *
   * <p>The value field is interpreted as a signed {@code int} value. A finding is added in case
   * {@link BigInteger#intValueExact()} indicates "out-of-range".
   */
  private static final class MyPrimitiveSpecific extends PrimitiveSpecific<Integer> {

    /** Specific instance attribute. */
    private final int insAttribute; // */

    /**
     * Constructor.
     *
     * @param tag the tag-field
     * @param value the value-field
     * @throws ArithmeticException if the value-field (seen as {@link BigInteger#intValueExact()}
     *     throws such an exception
     */
    private MyPrimitiveSpecific(final long tag, final byte[] value) {
      super(tag, value);

      final var bi = new BigInteger(getValueField());
      int attribute;
      try {
        attribute = bi.intValueExact();
      } catch (ArithmeticException e) {
        attribute = bi.intValue();
        insFindings.add("out-of-range");
      } // end Catch (...)

      insAttribute = attribute;
    } // end constructor */

    /**
     * Constructor.
     *
     * <p><i><b>Notes:</b></i>
     *
     * <ol>
     *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
     *       the input parameter(s) while this constructor is running.</i>
     *   <li><i>Object sharing is not a problem here, because object is immutable.</i>
     * </ol>
     *
     * @param tag the tag-field
     * @param inputStream form which the TLV object is constructed
     * @throws ArithmeticException if the length-field indicates a length greater than {@link
     *     Integer#MAX_VALUE}
     * @throws IOException if underlying methods do so
     * @throws IllegalArgumentException if underlying constructors do so
     */
    private MyPrimitiveSpecific(final byte[] tag, final InputStream inputStream)
        throws IOException {
      // CT_CONSTRUCTOR_THROW
      super(tag, inputStream);

      final var bi = new BigInteger(getValueField());
      int attribute;
      try {
        attribute = bi.intValueExact();
      } catch (ArithmeticException e) {
        attribute = bi.intValue();
        insFindings.add("out-of-range");
      } // end Catch (...)

      insAttribute = attribute;
    } // end constructor */

    /**
     * Getter.
     *
     * @return decoded value
     */
    @Override
    public Integer getDecoded() {
      return insAttribute;
    } // end method */
  } // end inner class
} // end class
