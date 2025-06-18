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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box tests on {@link DerNull}.
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
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerNull {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Temporary Directory. */
  @TempDir
  /* package */ static Path claTempDir; // NOPMD, NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Test strategy:
    // --- a. check the one and only instance

    final DerNull dut = DerNull.NULL;

    // --- a. check the one and only instance
    assertNotNull(dut);
    assertEquals(DerNull.TAG, dut.getTag());
    assertEquals(0, dut.getLengthOfValueField());
    assertEquals("05 00", dut.toString(" "));
    assertEquals(DerSpecific.DELIMITER + "NULL", dut.getComment());
    assertTrue(dut.isValid());
    assertTrue(dut.insFindings.isEmpty());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(0L, dut.insLengthOfValueField);
    assertEquals(0L, dut.insLengthOfValueFieldFromStream);
    assertEquals("0500", Hex.toHexDigits(dut.insTagLengthField)); // NOPMD appears often
    assertEquals("0500", Hex.toHexDigits(dut.getEncoded()));
    assertTrue(dut.insFindings.isEmpty());
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

  /** Test method for {@link DerNull#readInstance(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_readInstance__ByteBuffer() {
    // Assertions:
    // ... a. methods from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. test with all possible input-values (here none)
    // --- b. FINDING: length-field not '01'
    // --- c. FINDING: value-field present
    // --- d. ERROR: ArithmeticException
    // --- e. ERROR: BufferUnderflowException

    // --- a. test with all possible input-values (here none)
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("-00"));

      final DerNull dut = DerNull.readInstance(buffer);

      assertSame(DerNull.NULL, dut);
      assertFalse(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(0L, dut.insLengthOfValueField);
      assertEquals(0L, dut.insLengthOfValueFieldFromStream);
      assertEquals("0500", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("0500", Hex.toHexDigits(dut.getEncoded()));
      assertTrue(dut.insFindings.isEmpty());
      assertTrue(dut.isValid());
    } // end --- a.

    // --- b. FINDING: length-field not '00'
    {
      final var inputSet =
          Set.of("---8100", "---8500 0000 0000", "---8f00 0000 0000 0000 0000 0000 0000 0000");
      for (final var input : inputSet) {
        final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

        final var dut = DerNull.readInstance(buffer);

        assertNotSame(DerNull.NULL, dut);
        assertFalse(dut.insIndefiniteForm);
        assertEquals(Hex.toByteArray(input).length, dut.insLengthOfLengthFieldFromStream);
        assertEquals(0L, dut.insLengthOfValueField);
        assertEquals(0L, dut.insLengthOfValueFieldFromStream);
        assertEquals(List.of("original length-field unequal to '00'"), dut.insFindings);
      } // end For (input...)
    } // end --- b.

    // --- c. FINDING: value-field present
    for (final var length : RNG.intsClosed(1, 20, 10).toArray()) {
      final byte[] valueField = RNG.nextBytes(length);
      final String input = BerTlv.getLengthField(length) + Hex.toHexDigits(valueField);
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = DerNull.readInstance(buffer);

      assertNotSame(DerNull.NULL, dut);
      assertFalse(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(length, dut.insLengthOfValueField);
      assertEquals(length, dut.insLengthOfValueFieldFromStream);
      assertEquals("05" + input, Hex.toHexDigits(dut.getEncoded()));
      assertEquals(List.of("value-field present"), dut.insFindings);
    } // end For (length...)
    // end --- c.

    // --- d. ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> DerNull.readInstance(buffer));

      assertNull(e.getCause());
    } // end --- d.

    // --- e. ERROR: BufferUnderflowException
    for (final var input : Set.of(".00", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> DerNull.readInstance(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> DerNull.readInstance(buffer));
    } // end For (input...)
    // end --- e.
  } // end method */

  /** Test method for {@link DerNull#readInstance(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_readInstance__InputStream() {
    // Assertions:
    // ... a. methods from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. test with all possible input-values (here none)
    // --- b. FINDING: length-field not '01'
    // --- c. FINDING: value-field present
    // --- d. ERROR: ArithmeticException
    // --- e. ERROR: IOException

    try {
      // --- a. test with all possible input-values (here none)
      {
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray("_00"));

        final var dut = DerNull.readInstance(inputStream);

        assertSame(DerNull.NULL, dut);
        assertFalse(dut.insIndefiniteForm);
        assertEquals(1, dut.insLengthOfLengthFieldFromStream);
        assertEquals(0L, dut.insLengthOfValueField);
        assertEquals(0L, dut.insLengthOfValueFieldFromStream);
        assertEquals("0500", Hex.toHexDigits(dut.insTagLengthField));
        assertEquals("0500", Hex.toHexDigits(dut.getEncoded()));
        assertTrue(dut.insFindings.isEmpty());
        assertTrue(dut.isValid());
      } // end --- a.

      // --- b. FINDING: length-field not '00'
      {
        final var inputSet =
            Set.of("---8100", "---8500 0000 0000", "---8f00 0000 0000 0000 0000 0000 0000 0000");
        for (final var input : inputSet) {
          final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

          final var dut = DerNull.readInstance(inputStream);

          assertNotSame(DerNull.NULL, dut);
          assertFalse(dut.insIndefiniteForm);
          assertEquals(Hex.toByteArray(input).length, dut.insLengthOfLengthFieldFromStream);
          assertEquals(0L, dut.insLengthOfValueField);
          assertEquals(0L, dut.insLengthOfValueFieldFromStream);
          assertEquals(List.of("original length-field unequal to '00'"), dut.insFindings);
        } // end For (input...)
      } // end --- b.

      // --- c. FINDING: value-field present
      for (final var length : RNG.intsClosed(1, 20, 10).toArray()) {
        final byte[] valueField = RNG.nextBytes(length);
        final String input = BerTlv.getLengthField(length) + Hex.toHexDigits(valueField);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = DerNull.readInstance(inputStream);

        assertNotSame(DerNull.NULL, dut);
        assertFalse(dut.insIndefiniteForm);
        assertEquals(1, dut.insLengthOfLengthFieldFromStream);
        assertEquals(length, dut.insLengthOfValueField);
        assertEquals(length, dut.insLengthOfValueFieldFromStream);
        assertEquals("05" + input, Hex.toHexDigits(dut.getEncoded()));
        assertEquals(List.of("value-field present"), dut.insFindings);
      } // end For (length...)
      // end --- c.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- d. ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> DerNull.readInstance(inputStream));

      assertNull(thrown.getCause());
    } // end --- d.

    // --- e. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-f.bin");
      Files.write(path, Hex.toByteArray(".00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> DerNull.readInstance(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- e.
  } // end method */

  /** Test method for {@link DerNull#getComment()}. */
  @Test
  void test_getComment() {
    // Note: This simple method does not need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. FINDING: value-field present
    // --- c. FINDING: original length-field unequal to `00'

    // --- a. smoke test
    assertEquals(" # NULL", DerNull.NULL.getComment());

    // --- b. FINDING: value-field present
    Set.of(
            "05 01 00", // just value-field present
            "05 8102 1122", // value-field present and non-optimal length-field
            "05 8f00 0000 0000 0000 0000 0000 0000 0003 aabbcc" // value-field, really long
            // length-field
            )
        .forEach(
            input -> {
              final BerTlv dutGen =
                  BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));
              assertEquals(DerNull.class, dutGen.getClass());
              final DerNull dut = (DerNull) dutGen;
              assertEquals(" # NULL, findings: value-field present", dut.getComment());
            }); // end forEach(input -> ...)

    // --- c. FINDING: original length-field unequal to `00'
    Set.of("05 8100", "05 8500 0000 0000", "05 8f00 0000 0000 0000 0000 0000 0000 0000")
        .forEach(
            input -> {
              final BerTlv dutGen =
                  BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));
              assertEquals(DerNull.class, dutGen.getClass());
              final DerNull dut = (DerNull) dutGen;
              assertEquals(
                  " # NULL, findings: original length-field unequal to '00'", dut.getComment());
            }); // end forEach(input -> ...)
  } // end method */

  /** Test method for {@link DerNull#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    assertTrue(DerNull.NULL.getDecoded().isEmpty());
  } // end method */
} // end class
