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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box test on {@link DerPrintableString}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "ES_COMPARING_PARAMETER_STRING_WITH_EQ", i.e.
//         Comparison of String parameter using == or !=
//         That finding is correct, but intentionally assertSame(...) is used.
// Note 2: Spotbugs claims: "ES_COMPARING_STRINGS_WITH_EQ"
//         Explanation: This code compares java.lang.String objects for reference
//                      equality using the == or != operators. Unless both strings
//                      are either constants in a source file, or have been interned
//                      using the String.intern() method, the same string value may
//                      be represented by two different String objects.
//                      Consider using the equals(Object) method instead.
//         The finding is correct. Intentionally, the code is NOT changed.
// Note 3: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.,
//         Non-null field is not initialized
//         This finding is for a class attribute which is initialized by JUnit.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ES_COMPARING_PARAMETER_STRING_WITH_EQ", // see note 1
  "ES_COMPARING_STRINGS_WITH_EQ", // see note 2
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 3
}) // */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerPrintableString {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Temporary Directory. */
  @TempDir
  /* package */ static Path claTempDir; // NOPMD, NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR */

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

  /** Test method for {@link DerPrintableString#DerPrintableString(String)}. */
  @Test
  void test_DerPrintableString__String() {
    // Assertions:
    // ... a. Constructors from superclasses work as expected.

    // Note: Simple constructor does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. bunch of test with random input
    // --- c. ERROR: invalid characters

    // --- a. smoke test with manually chosen input
    for (final var input :
        Set.of(
            "", // empty input
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ", // upper case
            "abcdefghijklmnopqrstuvwxyz", // lower case
            "0123456789", // digits
            " '()+,-./:=?", // special characters
            RNG.nextPrintable(200))) {
      final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
      final String expected =
          "13" + BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);

      final DerPrintableString dut = new DerPrintableString(input); // NOPMD new in loop

      assertEquals(expected, dut.toString());
      assertSame(input, dut.insDecoded); // ES_COMPARING_PARAMETER_STRING_WITH_EQ
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (input...)

    // --- b. bunch of test with random input
    RNG.intsClosed(0, 1000, 200)
        .forEach(
            size -> {
              final String input = RNG.nextPrintable(size);
              final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
              final String expected =
                  "13" + BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
              final DerPrintableString dut = new DerPrintableString(input);
              assertEquals(expected, dut.toString());
              assertSame(input, dut.insDecoded); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
              assertTrue(dut.isValid());
              assertTrue(dut.insFindings.isEmpty());
            }); // end forEach(size -> ...)

    // --- c. ERROR: invalid characters
    Set.of(
            "@",
            "a!",
            "#0",
            "%*", // < A
            "[]", // Z < character < a
            "{}" // > z
            )
        .forEach(
            decoded -> {
              final Throwable throwable =
                  assertThrows(
                      IllegalArgumentException.class, () -> new DerPrintableString(decoded));
              assertEquals(DerPrintableString.MESSAGE, throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(decoded -> ...)
  } // end method */

  /** Test method for {@link DerPrintableString#DerPrintableString(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerPrintableString__ByteBuffer() {
    // Assertions:
    // ... a. constructors from superclasses work as expected

    // Test strategy:
    // --- a. smoke test with a bunch of input
    // --- b. FINDING: wrong format
    // --- c: ERROR: ArithmeticException
    // --- d. ERROR: BufferUnderflowException

    // --- a. smoke test with a bunch of input
    for (final var size : RNG.intsClosed(0, 1000, 20).toArray()) {
      final String decoded = RNG.nextPrintable(size);
      final byte[] valueField = decoded.getBytes(StandardCharsets.US_ASCII);
      final String input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = new DerPrintableString(buffer);

      assertEquals("13" + input, dut.toString());
      assertEquals(decoded, dut.insDecoded);
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (size...)
    // end --- a.

    // --- b. FINDING: invalid characters
    for (final var decoded : Set.of("@", "a!", "#0")) {
      final byte[] valueField = decoded.getBytes(StandardCharsets.US_ASCII);
      final String input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = new DerPrintableString(buffer);

      assertEquals("13" + input, dut.toString());
      assertEquals(decoded, dut.insDecoded);
      assertFalse(dut.isValid());
      assertEquals(1, dut.insFindings.size());
      assertEquals(List.of(DerPrintableString.MESSAGE), dut.insFindings);
    } // end For (decoded...)
    // end --- b.

    // --- c: ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerPrintableString(buffer));

      assertNull(e.getCause());
    } // end --- c.

    // --- d. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerPrintableString(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerPrintableString(buffer));
    } // end For (input...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerPrintableString#DerPrintableString(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerPrintableString__InputStream() {
    // Assertions:
    // ... a. constructors from superclasses work as expected

    // Test strategy:
    // --- a. smoke test with a bunch of input
    // --- b. FINDING: wrong format
    // --- c: ERROR: ArithmeticException
    // --- d. ERROR: IOException

    try {
      // --- a. smoke test with a bunch of input
      for (final var size : RNG.intsClosed(0, 1000, 20).toArray()) {
        final String decoded = RNG.nextPrintable(size);
        final byte[] valueField = decoded.getBytes(StandardCharsets.US_ASCII);
        final String input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerPrintableString(inputStream);

        assertEquals("13" + input, dut.toString());
        assertEquals(decoded, dut.insDecoded);
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
      } // end For (size...)
      // end --- a.

      // --- b. FINDING: invalid characters
      for (final var decoded : Set.of("@", "a!", "#0")) {
        final byte[] valueField = decoded.getBytes(StandardCharsets.US_ASCII);
        final String input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerPrintableString(inputStream);

        assertEquals("13" + input, dut.toString());
        assertEquals(decoded, dut.insDecoded);
        assertFalse(dut.isValid());
        assertEquals(1, dut.insFindings.size());
        assertEquals(List.of(DerPrintableString.MESSAGE), dut.insFindings);
      } // end For (decoded...)
      // end --- b.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- c: ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> new DerPrintableString(inputStream));

      assertNull(thrown.getCause());
    } // end --- c.

    // --- d. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerPrintableString(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerPrintableString#getComment()}. */
  @Test
  void test_getComment() {
    // Test strategy:
    // --- a. smoke test without findings
    // --- b. smoke test with findings

    // --- a. smoke test without findings
    // --- b. smoke test with findings
    for (final var entry :
        Map.ofEntries(
                Map.entry(
                    "Max Mustermann", // no findings
                    " # PrintableString := \"Max Mustermann\""),
                Map.entry(
                    "foo@gmail.com", // invalid character, here @
                    " # PrintableString := \"foo@gmail.com\", findings: "
                        + DerPrintableString.MESSAGE))
            .entrySet()) {
      final var input = entry.getKey();
      final var expected = entry.getValue();
      final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
      final String octets =
          String.format("%02x", DerPrintableString.TAG)
              + BerTlv.getLengthField(valueField.length)
              + Hex.toHexDigits(valueField);
      final BerTlv dutGen = BerTlv.getInstance(octets);
      assertEquals(DerPrintableString.class, dutGen.getClass());
      final DerPrintableString dut = (DerPrintableString) dutGen;
      assertEquals(octets, dut.toString());

      final var actual = dut.getComment();

      assertEquals(expected, actual);
    } // end For (entry...)
  } // end method */

  /** Test method for {@link DerPrintableString#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with value constructor
    // --- b. smoke test with InputStream constructor

    // --- a. smoke test with value constructor
    for (final var decoded : Set.of("Foo Bar")) {
      final DerPrintableString dut = new DerPrintableString(decoded); // NOPMD new in loop
      assertSame(decoded, dut.getDecoded()); // ES_COMPARING_PARAMETER_STRING_WITH_EQ
    } // end For (decoded ...)

    // --- b. smoke test with InputStream constructor
    // Note: Invalid characters causes a finding during construction rather than an exception.
    final String character = "abcDEF0123#&@~!_"; // valid and invalid characters
    final int supIndex = character.length() - 1;
    RNG.intsClosed(0, 1000, 20)
        .forEach(
            size -> {
              final char[] chars = new char[size];

              for (int i = size; i-- > 0; ) { // NOPMD assignment in operands
                chars[i] = character.charAt(RNG.nextIntClosed(0, supIndex));
              } // end For (i...)

              final String expected = new String(chars); // NOPMD String instantiation
              final byte[] valueField = expected.getBytes(StandardCharsets.US_ASCII);
              final String input =
                  "13" + BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
              final BerTlv dutGen =
                  BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));
              assertEquals(DerPrintableString.class, dutGen.getClass());
              final DerPrintableString dut = (DerPrintableString) dutGen;
              assertEquals(input, dut.toString());

              final var present = dut.getDecoded();

              assertEquals(expected, present);
            }); // end forEach(size -> ...)
  } // end method */
} // end class
