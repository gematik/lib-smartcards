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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box tests on {@link DerBoolean}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.,
//         Non-null field is not initialized
//         This finding is for a class attribute which is initialized by JUnit.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" // see note 1
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerBoolean {

  /** Exception message. */
  private static final String MESSAGE_LENGTH = "length of value-field unequal to 1"; // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Temporary Directory. */
  @TempDir
  /* package */ static Path claTempDir; // NOPMD, NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Test strategy:
    // --- a. check pre-defined instances
    // a.1 check TRUE
    // a.2 check FALSE

    DerBoolean dut;

    // a.1 check TRUE
    dut = DerBoolean.TRUE;
    assertNotNull(dut);
    assertEquals(DerBoolean.TAG, dut.getTag());
    assertEquals(1L, dut.getLengthOfValueField());
    assertEquals("01 01 ff", dut.toString(" "));
    assertEquals(DerSpecific.DELIMITER + "BOOLEAN := true", dut.getComment());
    assertTrue(dut.isValid());
    assertTrue(dut.insFindings.isEmpty());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(1L, dut.insLengthOfValueFieldFromStream);
    assertEquals(1L, dut.insLengthOfValueField);
    assertEquals("0101", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("0101ff", Hex.toHexDigits(dut.getEncoded()));

    // a.2 check FALSE
    dut = DerBoolean.FALSE;
    assertNotNull(dut);
    assertEquals(DerBoolean.TAG, dut.getTag());
    assertEquals(1L, dut.getLengthOfValueField());
    assertEquals("01 01 00", dut.toString(" "));
    assertEquals(DerSpecific.DELIMITER + "BOOLEAN := false", dut.getComment());
    assertTrue(dut.isValid());
    assertTrue(dut.insFindings.isEmpty());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(1L, dut.insLengthOfValueFieldFromStream);
    assertEquals(1L, dut.insLengthOfValueField);
    assertEquals("0101", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("010100", Hex.toHexDigits(dut.getEncoded()));
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

  /** Test method for {@link DerBoolean#create(String)}. */
  @Test
  void test_create__String() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test for TRUE
    // --- b. smoke test for FALSE
    // --- c. ERROR: erroneous input

    // --- a. smoke test for TRUE
    {
      final var input = "-01-ff";
      final var expected = DerBoolean.TRUE;

      final var present = DerBoolean.create(input);

      assertNotSame(expected, present);
      assertEquals(expected, present);
    } // end --- a.

    // --- b. smoke test for FALSE
    {
      final var input = "-01-00";
      final var expected = DerBoolean.FALSE;

      final var present = DerBoolean.create(input);

      assertNotSame(expected, present);
      assertEquals(expected, present);
    } // end --- a.

    // --- c. ERROR: erroneous input
    {
      final var input = "01"; // value-field is missing

      assertThrows(IllegalStateException.class, () -> DerBoolean.create(input));
    } // end --- a.
  } // end method */

  /** Test method for {@link DerBoolean#readInstance(ByteBuffer)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount"})
  @Test
  void test_readInstance__ByteBuffer() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. test with all possible input-values
    // --- c. FINDING: wrong length
    // --- d: special input
    // --- e: ERROR: ArithmeticException
    // --- f: ERROR: BufferUnderflowException

    // --- a. smoke test
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("-01-03"));

      final var dut = DerBoolean.readInstance(buffer);

      assertEquals(DerBoolean.TAG, dut.getTag());
      assertEquals(1L, dut.getLengthOfValueField());
      assertEquals("01 01 03", dut.toString(" "));
      assertEquals(DerSpecific.DELIMITER + "BOOLEAN := true", dut.getComment());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
      assertFalse(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(1L, dut.insLengthOfValueField);
      assertEquals(1L, dut.insLengthOfValueFieldFromStream);
      assertEquals("0101", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("010103", Hex.toHexDigits(dut.getEncoded()));
      assertTrue(dut.insFindings.isEmpty());
      assertTrue(dut.isValid());
    } // end --- a.

    // --- b. test with all possible input-values
    for (final var value : IntStream.rangeClosed(0x00, 0xff).toArray()) {
      final var octets = String.format("-01-%02x", value);
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(octets));

      final DerBoolean dut = DerBoolean.readInstance(buffer);

      if (0 == value) { // NOPMD literal in if statement
        // ... FALSE
        assertFalse(dut.getDecoded());
        assertEquals(DerSpecific.DELIMITER + "BOOLEAN := false", dut.getComment());
        assertSame(DerBoolean.FALSE, dut);
      } else if (0xff == value) { // NOPMD literal in if statement
        // ... TRUE, special
        assertTrue(dut.getDecoded());
        assertEquals(DerSpecific.DELIMITER + "BOOLEAN := true", dut.getComment());
        assertSame(DerBoolean.TRUE, dut);
      } else {
        // ... TRUE
        assertTrue(dut.getDecoded());
        assertEquals(DerSpecific.DELIMITER + "BOOLEAN := true", dut.getComment());
        assertNotSame(DerBoolean.FALSE, dut);
        assertNotSame(DerBoolean.TRUE, dut);
      } // end fi
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (value...)

    // --- c. FINDING: wrong length
    for (final var length : IntStream.rangeClosed(0, 20).toArray()) {
      final var octets =
          BerTlv.getLengthField(length) // length-field
              + Hex.toHexDigits(RNG.nextBytes(length)); // value-field
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(octets));
      if (1 == length) { // NOPMD avoid literals in conditional statements
        // ... correct length

        final DerBoolean dut = DerBoolean.readInstance(buffer);

        assertEquals(1, dut.insLengthOfLengthFieldFromStream);
        assertEquals(length, dut.insLengthOfValueField);
        assertEquals(length, dut.insLengthOfValueFieldFromStream);
      } else {
        // ... wrong length

        final DerBoolean dut = DerBoolean.readInstance(buffer);

        assertNotSame(DerBoolean.TRUE, dut);
        assertNotSame(DerBoolean.FALSE, dut);
        assertFalse(dut.insIndefiniteForm);
        assertEquals(1, dut.insLengthOfLengthFieldFromStream);
        assertEquals(length, dut.insLengthOfValueField);
        assertEquals(length, dut.insLengthOfValueFieldFromStream);
        assertEquals("01" + octets, Hex.toHexDigits(dut.getEncoded()));
        assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
      } // end else
    } // end For (length...)
    // end --- c.

    // --- d: special input
    // d.0.i  optimal length-field, 0 octet in value-field
    // d.0.ii   long  length-field, 0 octet in value-field
    // d.1.i  optimal length-field, 1 octet in value-field
    // d.1.ii   long  length-field, 1 octet in value-field
    // d.2.i  optimal length-field, 2 octet in value-field
    // d.2.ii   long  length-field, 2 octet in value-field
    {
      DerBoolean dut;

      // d.0.i  optimal length-field, 0 octet in value-field
      dut = DerBoolean.readInstance(ByteBuffer.wrap(Hex.toByteArray("-00")));
      assertNotSame(DerBoolean.TRUE, dut);
      assertNotSame(DerBoolean.FALSE, dut);
      assertEquals(DerBoolean.TAG, dut.getTag());
      assertEquals("00", dut.getLengthField());
      assertEquals("01 00", dut.toString(" "));
      assertEquals(0L, dut.insLengthOfValueField);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(0L, dut.insLengthOfValueFieldFromStream);
      assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
      assertFalse(dut.insIndefiniteForm);
      assertEquals("0100", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("", Hex.toHexDigits(dut.insValueField));
      assertFalse(dut.getDecoded());
      assertFalse(dut.isValid());

      // d.0.ii   long  length-field, 0 octet in value-field
      dut = DerBoolean.readInstance(ByteBuffer.wrap(Hex.toByteArray("-8100")));
      assertNotSame(DerBoolean.TRUE, dut);
      assertNotSame(DerBoolean.FALSE, dut);
      assertEquals(DerBoolean.TAG, dut.getTag());
      assertEquals("00", dut.getLengthField());
      assertEquals("01 00", dut.toString(" "));
      assertEquals(0L, dut.insLengthOfValueField);
      assertEquals(2, dut.insLengthOfLengthFieldFromStream);
      assertEquals(0L, dut.insLengthOfValueFieldFromStream);
      assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
      assertFalse(dut.insIndefiniteForm);
      assertEquals("0100", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("", Hex.toHexDigits(dut.insValueField));
      assertFalse(dut.getDecoded());
      assertFalse(dut.isValid());

      // d.1.i  optimal length-field, 1 octet in value-field
      dut = DerBoolean.readInstance(ByteBuffer.wrap(Hex.toByteArray("-01-af")));
      assertNotSame(DerBoolean.TRUE, dut);
      assertNotSame(DerBoolean.FALSE, dut);
      assertEquals(DerBoolean.TAG, dut.getTag());
      assertEquals("01", dut.getLengthField());
      assertEquals("01 01 af", dut.toString(" "));
      assertEquals(1L, dut.insLengthOfValueField);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(1L, dut.insLengthOfValueFieldFromStream);
      assertTrue(dut.insFindings.isEmpty());
      assertFalse(dut.insIndefiniteForm);
      assertEquals("0101", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("af", Hex.toHexDigits(dut.insValueField));
      assertTrue(dut.getDecoded());
      assertTrue(dut.isValid());

      // d.1.ii   long  length-field, 1 octet in value-field
      dut = DerBoolean.readInstance(ByteBuffer.wrap(Hex.toByteArray("-8101-00")));
      assertNotSame(DerBoolean.TRUE, dut);
      assertNotSame(DerBoolean.FALSE, dut);
      assertEquals(DerBoolean.TAG, dut.getTag());
      assertEquals("01", dut.getLengthField());
      assertEquals("01 01 00", dut.toString(" "));
      assertEquals(1L, dut.insLengthOfValueField);
      assertEquals(2, dut.insLengthOfLengthFieldFromStream);
      assertEquals(1L, dut.insLengthOfValueFieldFromStream);
      assertEquals(List.of("original length-field unequal to '01'"), dut.insFindings);
      assertFalse(dut.insIndefiniteForm);
      assertEquals("0101", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("00", Hex.toHexDigits(dut.insValueField));
      assertFalse(dut.getDecoded());
      assertFalse(dut.isValid());

      // d.2.i  optimal length-field, 2 octet in value-field
      dut = DerBoolean.readInstance(ByteBuffer.wrap(Hex.toByteArray("-02-ff00")));
      assertNotSame(DerBoolean.TRUE, dut);
      assertNotSame(DerBoolean.FALSE, dut);
      assertEquals(DerBoolean.TAG, dut.getTag());
      assertEquals("02", dut.getLengthField());
      assertEquals("01 02 ff00", dut.toString(" "));
      assertEquals(2L, dut.insLengthOfValueField);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(2L, dut.insLengthOfValueFieldFromStream);
      assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
      assertFalse(dut.insIndefiniteForm);
      assertEquals("0102", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("ff00", Hex.toHexDigits(dut.insValueField));
      assertTrue(dut.getDecoded());
      assertFalse(dut.isValid());

      // d.2.ii   long  length-field, 2 octet in value-field
      dut = DerBoolean.readInstance(ByteBuffer.wrap(Hex.toByteArray("-820002-00ff")));
      assertNotSame(DerBoolean.TRUE, dut);
      assertNotSame(DerBoolean.FALSE, dut);
      assertEquals(DerBoolean.TAG, dut.getTag());
      assertEquals("02", dut.getLengthField());
      assertEquals("01 02 00ff", dut.toString(" "));
      assertEquals(2L, dut.insLengthOfValueField);
      assertEquals(3, dut.insLengthOfLengthFieldFromStream);
      assertEquals(2L, dut.insLengthOfValueFieldFromStream);
      assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
      assertFalse(dut.insIndefiniteForm);
      assertEquals("0102", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("00ff", Hex.toHexDigits(dut.insValueField));
      assertFalse(dut.getDecoded());
      assertFalse(dut.isValid());
    } // end --- d.

    // --- e: ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> DerBoolean.readInstance(buffer));

      assertNull(e.getCause());
    } // end --- e.

    // --- f. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> DerBoolean.readInstance(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> DerBoolean.readInstance(buffer));
    } // end For (input...)
    // end --- f.
  } // end method */

  /** Test method for {@link DerBoolean#readInstance(InputStream)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount"})
  @Test
  void test_readInstance__InputStream() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. test with all possible input-values
    // --- c. FINDING: wrong length
    // --- d: special input
    // --- e: ERROR: ArithmeticException
    // --- f: ERROR: IOException

    try {
      // --- a. smoke test
      {
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray("-01-03"));

        final var dut = DerBoolean.readInstance(inputStream);

        assertEquals(DerBoolean.TAG, dut.getTag());
        assertEquals(1L, dut.getLengthOfValueField());
        assertEquals("01 01 03", dut.toString(" "));
        assertEquals(DerSpecific.DELIMITER + "BOOLEAN := true", dut.getComment());
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
        assertFalse(dut.insIndefiniteForm);
        assertEquals(1, dut.insLengthOfLengthFieldFromStream);
        assertEquals(1L, dut.insLengthOfValueField);
        assertEquals(1L, dut.insLengthOfValueFieldFromStream);
        assertEquals("0101", Hex.toHexDigits(dut.insTagLengthField));
        assertEquals("010103", Hex.toHexDigits(dut.getEncoded()));
        assertTrue(dut.insFindings.isEmpty());
        assertTrue(dut.isValid());
      } // end --- a.

      // --- b. test with all possible input-values
      for (final var value : IntStream.rangeClosed(0x00, 0xff).toArray()) {
        final var octets = String.format("-01-%02x", value);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(octets));

        final DerBoolean dut = DerBoolean.readInstance(inputStream);

        if (0 == value) { // NOPMD literal in if statement
          // ... FALSE
          assertFalse(dut.getDecoded());
          assertEquals(DerSpecific.DELIMITER + "BOOLEAN := false", dut.getComment());
          assertSame(DerBoolean.FALSE, dut);
        } else if (0xff == value) { // NOPMD literal in if statement
          // ... TRUE, special
          assertTrue(dut.getDecoded());
          assertEquals(DerSpecific.DELIMITER + "BOOLEAN := true", dut.getComment());
          assertSame(DerBoolean.TRUE, dut);
        } else {
          // ... TRUE
          assertTrue(dut.getDecoded());
          assertEquals(DerSpecific.DELIMITER + "BOOLEAN := true", dut.getComment());
          assertNotSame(DerBoolean.FALSE, dut);
          assertNotSame(DerBoolean.TRUE, dut);
        } // end fi
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
      } // end For (value...)

      // --- c. FINDING: wrong length
      for (final var length : IntStream.rangeClosed(0, 20).toArray()) {
        final var octets =
            BerTlv.getLengthField(length) // length-field
                + Hex.toHexDigits(RNG.nextBytes(length)); // value-field
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(octets));
        if (1 == length) { // NOPMD avoid literals in conditional statements
          // ... correct length

          final DerBoolean dut = DerBoolean.readInstance(inputStream);

          assertEquals(1, dut.insLengthOfLengthFieldFromStream);
          assertEquals(length, dut.insLengthOfValueField);
          assertEquals(length, dut.insLengthOfValueFieldFromStream);
        } else {
          // ... wrong length

          final DerBoolean dut = DerBoolean.readInstance(inputStream);

          assertNotSame(DerBoolean.TRUE, dut);
          assertNotSame(DerBoolean.FALSE, dut);
          assertFalse(dut.insIndefiniteForm);
          assertEquals(1, dut.insLengthOfLengthFieldFromStream);
          assertEquals(length, dut.insLengthOfValueField);
          assertEquals(length, dut.insLengthOfValueFieldFromStream);
          assertEquals("01" + octets, Hex.toHexDigits(dut.getEncoded()));
          assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
        } // end else
      } // end For (length...)
      // end --- c.

      // --- d: special input
      // d.0.i  optimal length-field, 0 octet in value-field
      // d.0.ii   long  length-field, 0 octet in value-field
      // d.1.i  optimal length-field, 1 octet in value-field
      // d.1.ii   long  length-field, 1 octet in value-field
      // d.2.i  optimal length-field, 2 octet in value-field
      // d.2.ii   long  length-field, 2 octet in value-field
      {
        DerBoolean dut;

        // d.0.i  optimal length-field, 0 octet in value-field
        dut = DerBoolean.readInstance(new ByteArrayInputStream(Hex.toByteArray("-00")));
        assertNotSame(DerBoolean.TRUE, dut);
        assertNotSame(DerBoolean.FALSE, dut);
        assertEquals(DerBoolean.TAG, dut.getTag());
        assertEquals("00", dut.getLengthField());
        assertEquals("01 00", dut.toString(" "));
        assertEquals(0L, dut.insLengthOfValueField);
        assertEquals(1, dut.insLengthOfLengthFieldFromStream);
        assertEquals(0L, dut.insLengthOfValueFieldFromStream);
        assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
        assertFalse(dut.insIndefiniteForm);
        assertEquals("0100", Hex.toHexDigits(dut.insTagLengthField));
        assertEquals("", Hex.toHexDigits(dut.insValueField));
        assertFalse(dut.getDecoded());
        assertFalse(dut.isValid());

        // d.0.ii   long  length-field, 0 octet in value-field
        dut = DerBoolean.readInstance(new ByteArrayInputStream(Hex.toByteArray("-8100")));
        assertNotSame(DerBoolean.TRUE, dut);
        assertNotSame(DerBoolean.FALSE, dut);
        assertEquals(DerBoolean.TAG, dut.getTag());
        assertEquals("00", dut.getLengthField());
        assertEquals("01 00", dut.toString(" "));
        assertEquals(0L, dut.insLengthOfValueField);
        assertEquals(2, dut.insLengthOfLengthFieldFromStream);
        assertEquals(0L, dut.insLengthOfValueFieldFromStream);
        assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
        assertFalse(dut.insIndefiniteForm);
        assertEquals("0100", Hex.toHexDigits(dut.insTagLengthField));
        assertEquals("", Hex.toHexDigits(dut.insValueField));
        assertFalse(dut.getDecoded());
        assertFalse(dut.isValid());

        // d.1.i  optimal length-field, 1 octet in value-field
        dut = DerBoolean.readInstance(new ByteArrayInputStream(Hex.toByteArray("-01-af")));
        assertNotSame(DerBoolean.TRUE, dut);
        assertNotSame(DerBoolean.FALSE, dut);
        assertEquals(DerBoolean.TAG, dut.getTag());
        assertEquals("01", dut.getLengthField());
        assertEquals("01 01 af", dut.toString(" "));
        assertEquals(1L, dut.insLengthOfValueField);
        assertEquals(1, dut.insLengthOfLengthFieldFromStream);
        assertEquals(1L, dut.insLengthOfValueFieldFromStream);
        assertTrue(dut.insFindings.isEmpty());
        assertFalse(dut.insIndefiniteForm);
        assertEquals("0101", Hex.toHexDigits(dut.insTagLengthField));
        assertEquals("af", Hex.toHexDigits(dut.insValueField));
        assertTrue(dut.getDecoded());
        assertTrue(dut.isValid());

        // d.1.ii   long  length-field, 1 octet in value-field
        dut = DerBoolean.readInstance(new ByteArrayInputStream(Hex.toByteArray("-8101-00")));
        assertNotSame(DerBoolean.TRUE, dut);
        assertNotSame(DerBoolean.FALSE, dut);
        assertEquals(DerBoolean.TAG, dut.getTag());
        assertEquals("01", dut.getLengthField());
        assertEquals("01 01 00", dut.toString(" "));
        assertEquals(1L, dut.insLengthOfValueField);
        assertEquals(2, dut.insLengthOfLengthFieldFromStream);
        assertEquals(1L, dut.insLengthOfValueFieldFromStream);
        assertEquals(List.of("original length-field unequal to '01'"), dut.insFindings);
        assertFalse(dut.insIndefiniteForm);
        assertEquals("0101", Hex.toHexDigits(dut.insTagLengthField));
        assertEquals("00", Hex.toHexDigits(dut.insValueField));
        assertFalse(dut.getDecoded());
        assertFalse(dut.isValid());

        // d.2.i  optimal length-field, 2 octet in value-field
        dut = DerBoolean.readInstance(new ByteArrayInputStream(Hex.toByteArray("-02-ff00")));
        assertNotSame(DerBoolean.TRUE, dut);
        assertNotSame(DerBoolean.FALSE, dut);
        assertEquals(DerBoolean.TAG, dut.getTag());
        assertEquals("02", dut.getLengthField());
        assertEquals("01 02 ff00", dut.toString(" "));
        assertEquals(2L, dut.insLengthOfValueField);
        assertEquals(1, dut.insLengthOfLengthFieldFromStream);
        assertEquals(2L, dut.insLengthOfValueFieldFromStream);
        assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
        assertFalse(dut.insIndefiniteForm);
        assertEquals("0102", Hex.toHexDigits(dut.insTagLengthField));
        assertEquals("ff00", Hex.toHexDigits(dut.insValueField));
        assertTrue(dut.getDecoded());
        assertFalse(dut.isValid());

        // d.2.ii   long  length-field, 2 octet in value-field
        dut = DerBoolean.readInstance(new ByteArrayInputStream(Hex.toByteArray("-820002-00ff")));
        assertNotSame(DerBoolean.TRUE, dut);
        assertNotSame(DerBoolean.FALSE, dut);
        assertEquals(DerBoolean.TAG, dut.getTag());
        assertEquals("02", dut.getLengthField());
        assertEquals("01 02 00ff", dut.toString(" "));
        assertEquals(2L, dut.insLengthOfValueField);
        assertEquals(3, dut.insLengthOfLengthFieldFromStream);
        assertEquals(2L, dut.insLengthOfValueFieldFromStream);
        assertEquals(List.of(MESSAGE_LENGTH), dut.insFindings);
        assertFalse(dut.insIndefiniteForm);
        assertEquals("0102", Hex.toHexDigits(dut.insTagLengthField));
        assertEquals("00ff", Hex.toHexDigits(dut.insValueField));
        assertFalse(dut.getDecoded());
        assertFalse(dut.isValid());
      } // end --- d.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- e: ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> DerBoolean.readInstance(inputStream));

      assertNull(thrown.getCause());
    } // end --- e.

    // --- f. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-f.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> DerBoolean.readInstance(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- f.
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over complete range

    // --- a. smoke test
    assertNotEquals(DerBoolean.TRUE, DerBoolean.FALSE);

    // --- b. loop over complete range
    IntStream.rangeClosed(0x00, 0xff)
        .forEach(
            outer -> {
              final DerBoolean dutA =
                  (DerBoolean) BerTlv.getInstance(String.format("0101%02x", outer));

              IntStream.rangeClosed(0x00, 0xff)
                  .forEach(
                      inner -> {
                        final DerBoolean dutB =
                            (DerBoolean) BerTlv.getInstance(String.format("0101%02x", inner));
                        assertEquals(outer == inner, dutA.equals(dutB));
                      }); // end forEach(inner -> ...)
            }); // end forEach(outer -> ...)
  } // end method */

  /** Test method for {@link DerBoolean#getComment()}. */
  @Test
  void test_getComment() {
    // Note: This simple method does not need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. FINDING: length of value-field unequal to 1
    // --- c. FINDING: original length-field unequal to '01'

    // --- a. smoke test
    assertEquals(" # BOOLEAN := true", DerBoolean.TRUE.getComment());
    assertEquals(" # BOOLEAN := false", DerBoolean.FALSE.getComment());

    // --- b. FINDING: length of value-field unequal to 1
    Set.of(
            "01-00", // length == 0 != 1
            "01-02-0022", // length == 2 != 1
            "01---8f00 0000 0000 0000 0000 0000 0000 0003---00bbcc" // really long length-field
            )
        .forEach(
            input -> {
              final BerTlv dutGen =
                  BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));
              assertEquals(DerBoolean.class, dutGen.getClass());
              final DerBoolean dut = (DerBoolean) dutGen;
              assertEquals(
                  " # BOOLEAN := false, findings: length of value-field unequal to 1",
                  dut.getComment());
            }); // end forEach(input -> ...)

    // --- c. FINDING: original length-field unequal to '01'
    Set.of("01 8101 01", "01 8500 0000 0001 07", "01 8f00 0000 0000 0000 0000 0000 0000 0001 fa")
        .forEach(
            input -> {
              final BerTlv dutGen =
                  BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));
              assertEquals(DerBoolean.class, dutGen.getClass());
              final DerBoolean dut = (DerBoolean) dutGen;
              assertEquals(
                  " # BOOLEAN := true, findings: original length-field unequal to '01'",
                  dut.getComment());
            }); // end forEach(input -> ...)
  } // end method */

  /** Test method for {@link DerBoolean#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. test with all possible input-values
    assertTrue(DerBoolean.TRUE.getDecoded());
    assertFalse(DerBoolean.FALSE.getDecoded());
  } // end method */
} // end class
