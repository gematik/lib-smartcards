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
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box test on {@link DerTeletexString}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "ES_COMPARING_PARAMETER_STRING_WITH_EQ", i.e.
//         Comparison of String parameter using == or !=
//         That finding is correct, but intentionally assertSame(...) is used.
// Note 2: Spotbugs claims "ES_COMPARING_STRINGS_WITH_EQ", i.e.
//         Comparison of String objects using == or !=
//         That finding is correct, but intentionally assertSame(...) is used.
// Note 3: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.,
//         Non-null field is not initialized
//         This finding is for a class attribute which is initialized by JUnit.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ES_COMPARING_PARAMETER_STRING_WITH_EQ", // see note 1
  "ES_COMPARING_STRINGS_WITH_EQ", // see note 2
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 3
}) // */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerTeletexString {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Supremum of valid index in {@link DerTeletexString#UNICODE}. */
  private static final int SUPREMUM_INDEX = DerTeletexString.UNICODE.size() - 1; // */

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

  /** Test method for {@link DerTeletexString#DerTeletexString(String)}. */
  @Test
  void test_DerTeletexString__String() {
    // Assertions:
    // ... a. Constructors from superclasses work as expected.
    // ... b. toBytes()-method works as expected.

    // Note: Simple constructor does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. a bunch of tests with random input
    // --- c. ERROR: invalid characters

    // --- a. smoke test with manually chosen input
    // Note: Hereafter only such characters are used which have
    //       identical code points in US_ASCII and T.61.
    for (final var input : Set.of("", "Alfred 65 !@")) {
      final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
      final String expected =
          "14" + BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
      final DerTeletexString dut = new DerTeletexString(input); // NOPMD new in loop
      assertEquals(expected, dut.toString());
      assertSame(input, dut.insDecoded); // ES_COMPARING_PARAMETER_STRING_WITH_EQ
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (input...)

    // --- b. bunch of test with random input
    RNG.intsClosed(0, 1000, 200)
        .forEach(
            size -> {
              final int[] codepoints = new int[size];

              for (int i = size; i-- > 0; ) { // NOPMD assignment in operands
                codepoints[i] = DerTeletexString.UNICODE.get(RNG.nextIntClosed(0, SUPREMUM_INDEX));
              } // end For (i...)

              final String input = new String(codepoints, 0, size);
              final byte[] valueField = DerTeletexString.toBytes(input);
              final String expected =
                  "14" + BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
              final DerTeletexString dut = new DerTeletexString(input);
              assertEquals(expected, dut.toString());
              assertSame(input, dut.insDecoded); // ES_COMPARING_STRINGS_WITH_EQ
              assertTrue(dut.isValid());
              assertTrue(dut.insFindings.isEmpty());
            }); // end forEach(size -> ...)

    // --- c. ERROR: invalid characters
    Set.of("^")
        .forEach(
            decoded -> {
              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> new DerTeletexString(decoded));
              assertEquals(DerTeletexString.MESSAGE, throwable.getMessage());
            }); // end forEach(decoded -> ...)
  } // end method */

  /** Test method for {@link DerTeletexString#DerTeletexString(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerTeletexString__ByteBuffer() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toBytes()-method works as expected

    // Test strategy:
    // --- a. smoke test with a bunch of input
    // --- b. FINDING: invalid characters
    // --- c: ERROR: ArithmeticException
    // --- d. ERROR: BufferUnderflowException

    // --- a. smoke test with a bunch of input
    for (final var size : RNG.intsClosed(0, 1000, 20).toArray()) {
      final int[] codepoints = new int[size];
      for (int i = size; i-- > 0; ) { // NOPMD assignment in operands
        codepoints[i] = DerTeletexString.UNICODE.get(RNG.nextIntClosed(0, SUPREMUM_INDEX));
      } // end For (i...)
      final String decoded = new String(codepoints, 0, size);
      final byte[] valueField = DerTeletexString.toBytes(decoded);
      final String input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = new DerTeletexString(buffer);

      assertEquals("14" + input, dut.toString());
      assertEquals(decoded, dut.insDecoded);
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (size...)
    // end --- a.

    // --- b. FINDING: invalid characters
    {
      final var map = Map.ofEntries(Map.entry("^", "•"));

      for (final var entry : map.entrySet()) {
        final var given = entry.getKey();
        final var decoded = entry.getValue();
        final byte[] valueField = given.getBytes(StandardCharsets.UTF_8);
        final String input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

        final var dut = new DerTeletexString(buffer);

        assertEquals("14" + input, dut.toString());
        assertEquals(decoded, dut.insDecoded);
        assertFalse(dut.isValid());
        assertEquals(1, dut.insFindings.size());
        assertEquals(List.of(DerTeletexString.MESSAGE), dut.insFindings);
      } // end For (entry...)
    } // end --- b.

    // --- c: ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerTeletexString(buffer));

      assertNull(e.getCause());
    } // end --- c.

    // --- d. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerTeletexString(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerTeletexString(buffer));
    } // end For (input...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerTeletexString#DerTeletexString(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerTeletexString__InputStream() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toBytes()-method works as expected

    // Test strategy:
    // --- a. smoke test with a bunch of input
    // --- b. FINDING: invalid characters
    // --- c: ERROR: ArithmeticException
    // --- d. ERROR: IOException

    try {
      // --- a. smoke test with a bunch of input
      for (final var size : RNG.intsClosed(0, 1000, 20).toArray()) {
        final int[] codepoints = new int[size];
        for (int i = size; i-- > 0; ) { // NOPMD assignment in operands
          codepoints[i] = DerTeletexString.UNICODE.get(RNG.nextIntClosed(0, SUPREMUM_INDEX));
        } // end For (i...)
        final String decoded = new String(codepoints, 0, size);
        final byte[] valueField = DerTeletexString.toBytes(decoded);
        final String input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerTeletexString(inputStream);

        assertEquals("14" + input, dut.toString());
        assertEquals(decoded, dut.insDecoded);
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
      } // end For (size...)
      // end --- a.

      // --- b. FINDING: invalid characters
      {
        final var map = Map.ofEntries(Map.entry("^", "•"));

        for (final var entry : map.entrySet()) {
          final var given = entry.getKey();
          final var decoded = entry.getValue();
          final byte[] valueField = given.getBytes(StandardCharsets.UTF_8);
          final String input =
              BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
          final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

          final var dut = new DerTeletexString(inputStream);

          assertEquals("14" + input, dut.toString());
          assertEquals(decoded, dut.insDecoded);
          assertFalse(dut.isValid());
          assertEquals(1, dut.insFindings.size());
          assertEquals(List.of(DerTeletexString.MESSAGE), dut.insFindings);
        } // end For (entry...)
      } // end --- b.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- c: ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> new DerTeletexString(inputStream));

      assertNull(thrown.getCause());
    } // end --- c.

    // --- d. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerTeletexString(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerTeletexString#fromBytes(byte[])}. */
  @Test
  void test_fromBytes() {
    // Assertions:
    // ... a. toBytes()-method works as expected

    // Test strategy:
    // --- a. smoke test with valid characters
    // --- b. random input with valid characters
    // --- c. invalid characters
    // --- d. too few octets

    final DerTeletexString dut = new DerTeletexString("");

    // --- a. smoke test with valid characters
    for (final var expected : Set.of("Alfred", "$ is high", "#")) {
      final byte[] octets = DerTeletexString.toBytes(expected);

      final var actual = dut.fromBytes(octets);

      assertEquals(expected, actual);
    } // end For (input...)

    // --- b. random input with valid characters
    // FIXME

    // --- c. invalid characters
    Map.ofEntries(
            Map.entry("416c66726564", "Alfred"),
            Map.entry("61 20 7b 20 62", "a • b"),
            Map.entry("42 c241", "BÁ"))
        .forEach(
            (input, output) -> {
              final byte[] octets = Hex.toByteArray(input);
              assertEquals(output, dut.fromBytes(octets), input);
            }); // end forEach((input, output) -> ...)

    // --- d. too few octets
    Stream.of("42 c2")
        .forEach(
            input -> {
              final Throwable throwable =
                  assertThrows(
                      IllegalArgumentException.class, () -> dut.fromBytes(Hex.toByteArray(input)));
              assertEquals(DerTeletexString.MESSAGE, throwable.getMessage());
              assertNotNull(throwable.getCause());
              final Throwable innerException = throwable.getCause();
              assertEquals(BufferUnderflowException.class, innerException.getClass());
            }); // end forEach (input -> ...)
  } // end method */

  /** Test method for {@link DerTeletexString#getComment()}. */
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
                    " # TeletexString := \"Max Mustermann\""),
                Map.entry(
                    "foo^gmail.com", // invalid character, here ^
                    " # TeletexString := \"foo•gmail.com\", findings: " + DerTeletexString.MESSAGE))
            .entrySet()) {
      final var input = entry.getKey();
      final var expected = entry.getValue();
      final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
      final String octets =
          String.format("%02x", DerTeletexString.TAG)
              + BerTlv.getLengthField(valueField.length)
              + Hex.toHexDigits(valueField);
      final BerTlv dutGen = BerTlv.getInstance(octets);
      assertEquals(DerTeletexString.class, dutGen.getClass());
      final DerTeletexString dut = (DerTeletexString) dutGen;
      assertEquals(octets, dut.toString());

      final var actual = dut.getComment();

      assertEquals(expected, actual);
    } // end For (entry...)
  } // end method */

  /** Test method for {@link DerTeletexString#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with value constructor
    // --- b. smoke test with InputStream constructor

    // --- a. smoke test with value constructor
    for (final var decoded : Set.of("Foo Bar")) {
      final DerTeletexString dut = new DerTeletexString(decoded); // NOPMD new in loop

      assertSame(decoded, dut.getDecoded()); // ES_COMPARING_PARAMETER_STRING_WITH_EQ
    } // end For (decoded...)
    // end --- a.

    // --- b. smoke test with InputStream constructor
    // Note 1: Hereafter only such characters are used which have
    //         identical code points in US_ASCII and T.61.
    // Note 2: Invalid characters cause a finding during construction rather than an exception.
    // Note 3: Invalid characters are substituted by • = bullet point.
    final String character = "abcDEF0123&@!_"; // valid characters
    final int supIndex = character.length() - 1;
    RNG.intsClosed(0, 1000, 20)
        .forEach(
            size -> {
              final char[] chars = new char[size];

              for (int i = size; i-- > 0; ) { // NOPMD assignment in operands
                chars[i] = character.charAt(RNG.nextIntClosed(0, supIndex));
              } // end For (i...)

              final String decoded = new String(chars); // NOPMD String instantiation
              final byte[] valueField = decoded.getBytes(StandardCharsets.US_ASCII);
              final String input =
                  "14" + BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
              final BerTlv dutGen =
                  BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));
              assertEquals(DerTeletexString.class, dutGen.getClass());
              final DerTeletexString dut = (DerTeletexString) dutGen;
              assertEquals(input, dut.toString());
              assertEquals(decoded, dut.getDecoded());
            }); // end forEach(size -> ...)
  } // end method */

  /** Test method for {@link DerIa5String#invalidCharacters()}. */
  @Test
  void test_invalidCharacters() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    final var dut = new DerTeletexString("Foo Bar");

    assertFalse(dut.invalidCharacters());
  } // end method */

  /** Test method for {@link DerTeletexString#toBytes(String)}. */
  @Test
  void test_toBytes__String() {
    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. invalid input

    // --- a. smoke test with manually chosen input
    for (final var entry :
        Map.ofEntries(
                Map.entry("Alfred", "416c66726564"),
                Map.entry("$", "a4"),
                Map.entry("%", "25"),
                Map.entry("#", "a6"),
                Map.entry("¥", "a5"), // yen sign
                Map.entry("À", "c141"), // Grave
                Map.entry("é", "c265"), // Acute
                Map.entry("Ĝ", "c347"), // Circumflex
                Map.entry("ĩ", "c469"), // Tilde
                Map.entry("Ā", "c541"), // Macron
                Map.entry("Ŭ", "c655"), // Breve
                Map.entry("Ċ", "c743"), // Dot
                Map.entry("Ä", "c841"), // Umlaut
                Map.entry("Å", "ca41"), // Ring
                Map.entry("Ç", "cb43"), // Cedilla
                Map.entry("Ő", "cd4f"), // Double Acute
                Map.entry("Ę", "ce45"), // Ogonek
                Map.entry("ď", "cf64"), // Caron
                // FIXME: add more input
                Map.entry("", ""))
            .entrySet()) {
      final var input = entry.getKey();
      final var expected = entry.getValue();

      final var actual = Hex.toHexDigits(DerTeletexString.toBytes(input));

      assertEquals(expected, actual, input);
    } // end For (entry...)
    // end --- a.

    // --- b. invalid input
    // FIXME
  } // end method */
} // end class
