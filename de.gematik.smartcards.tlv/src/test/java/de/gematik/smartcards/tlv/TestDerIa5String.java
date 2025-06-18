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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box test on {@link DerIa5String}.
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
  "PMD.AvoidDuplicateLiterals",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerIa5String {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Supremum of valid index in {@link DerIa5String#UNICODE}. */
  private static final int SUPREMUM_INDEX = DerIa5String.UNICODE.size() - 1; // */

  /** Temporary Directory. */
  @TempDir
  /* package */ static Path claTempDir; // NOPMD, NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    if (TestBerTlv.VALID_TAGS.isEmpty()) {
      TestBerTlv.setUpBeforeClass();
    } // end fi
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

  /** Test method for {@link DerIa5String#DerIa5String(String)}. */
  @Test
  void test_DerIa5String__String() {
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
    //       identical code points in US_ASCII and IA5String.
    for (final var input :
        Set.of(
            "", // empty input
            "Alfred 65 !$")) {
      final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
      final String expected =
          "16" + BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
      final DerIa5String dut = new DerIa5String(input); // NOPMD new in loop
      assertEquals(expected, dut.toString());
      assertSame(input, dut.insDecoded); // ES_COMPARING_PARAMETER_STRING_WITH_EQ
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (input...)
    // end --- a.

    // --- b. bunch of test with random input
    RNG.intsClosed(0, 1000, 200)
        .forEach(
            size -> {
              final int[] codepoints = new int[size];

              for (int i = size; i-- > 0; ) { // NOPMD assignment in operands
                codepoints[i] = DerIa5String.UNICODE.get(RNG.nextIntClosed(0, SUPREMUM_INDEX));
              } // end For (i...)

              final String input = new String(codepoints, 0, size);
              final byte[] valueField = DerIa5String.toBytes(input);
              final String expected =
                  "16" + BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
              final DerIa5String dut = new DerIa5String(input);
              assertEquals(expected, dut.toString());
              assertSame(input, dut.insDecoded); // ES_COMPARING_STRINGS_WITH_EQ
              assertTrue(dut.isValid());
              assertTrue(dut.insFindings.isEmpty());
            }); // end forEach(size -> ...)

    // --- c. ERROR: invalid characters
    Set.of("[")
        .forEach(
            decoded -> {
              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> new DerIa5String(decoded));
              assertEquals(DerIa5String.MESSAGE, throwable.getMessage());
            }); // end forEach(decoded -> ...)
  } // end method */

  /** Test method for {@link DerIa5String#DerIa5String(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerIa5String__ByteBuffer() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toBytes()-method works as expected

    // Test strategy:
    // --- a. smoke test with a bunch of input
    // --- b. FINDING: invalid characters
    // --- c: ERROR: ArithmeticException
    // --- d. ERROR: BufferUnderflowException

    // --- a. smoke test with a bunch of input
    for (final var size : RNG.intsClosed(0, 1000, 200).toArray()) {
      final int[] codepoints = new int[size];

      for (int i = size; i-- > 0; ) { // NOPMD assignment in operands
        codepoints[i] = DerIa5String.UNICODE.get(RNG.nextIntClosed(0, SUPREMUM_INDEX));
      } // end For (i...)

      final var decoded = new String(codepoints, 0, size);
      final var valueField = DerIa5String.toBytes(decoded);
      final var input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = new DerIa5String(buffer);

      assertEquals("16" + input, dut.toString());
      assertEquals(decoded, dut.insDecoded);
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (size...)
    // end --- a.

    // --- b. FINDING: invalid characters
    // Note: Intentionally not tests here. It is impossible to inject invalid
    //       characters from a ByteBuffer, because for each byte in the
    //       ByteBuffer a valid character exists.

    // --- c: ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerIa5String(buffer));

      assertNull(e.getCause());
    } // end --- c.

    // --- d. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerIa5String(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerIa5String(buffer));
    } // end For (input...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerIa5String#DerIa5String(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerIa5String__InputStream() {
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
      for (final var size : RNG.intsClosed(0, 1000, 200).toArray()) {
        final int[] codepoints = new int[size];

        for (int i = size; i-- > 0; ) { // NOPMD assignment in operands
          codepoints[i] = DerIa5String.UNICODE.get(RNG.nextIntClosed(0, SUPREMUM_INDEX));
        } // end For (i...)

        final var decoded = new String(codepoints, 0, size);
        final var valueField = DerIa5String.toBytes(decoded);
        final var input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerIa5String(inputStream);

        assertEquals("16" + input, dut.toString());
        assertEquals(decoded, dut.insDecoded);
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
      } // end For (size...)
      // end --- a.

      // --- b. FINDING: invalid characters
      // Note: Intentionally not tests here. It is impossible to inject invalid
      //       characters from an InputStream, because for each byte in the
      //       InputStream a valid character exists.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- c: ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> new DerIa5String(inputStream));

      assertNull(thrown.getCause());
    } // end --- c.

    // --- d. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerIa5String(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerIa5String#fromBytes(byte[])}. */
  @Test
  void test_fromBytes() {
    // Assertions:
    // ... a. toBytes()-method works as expected

    // Test strategy:
    // --- a. smoke test with valid characters
    // --- b. random input with valid characters
    // --- c. invalid characters

    final DerIa5String dut = new DerIa5String("");

    // --- a. smoke test with valid characters
    for (final var entry :
        Map.ofEntries(
                Map.entry("Alfred", "416c66726564"),
                Map.entry("$", "24"),
                Map.entry("%", "25"),
                Map.entry("#", "23"),

                // characters which differ from ASCII
                Map.entry("§", "40"),
                Map.entry("Ä", "5b"),
                Map.entry("Ö", "5c"),
                Map.entry("Ü", "5d"),
                Map.entry("ä", "7b"),
                Map.entry("ö", "7c"),
                Map.entry("ü", "7d"),
                Map.entry("ß", "7e"),
                Map.entry("", ""))
            .entrySet()) {
      final var expected = entry.getKey();
      final var bytes = entry.getValue();
      final byte[] octets = Hex.toByteArray(bytes);

      final var actual = dut.fromBytes(octets);

      assertEquals(expected, actual);
    } // endForEach (octets ...)

    // --- b. random input with valid characters
    // Note: Intentionally no tests here, because it is hard to estimate
    //       the expected value without re-implementing or using the
    //       method-under-test.

    // --- c. invalid characters
    // Note: No tests for invalid characters, because for each byte-value
    //       a valid character exists.
  } // end method */

  /** Test method for {@link DerIa5String#getComment()}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_getComment() {
    // Test strategy:
    // --- a. smoke test without findings
    // --- b. smoke test with findings

    try {
      // --- a. smoke test without findings
      for (final var entry :
          Map.ofEntries(
                  Map.entry(
                      "Max Mustermann", // no findings
                      " # IA5String := \"Max Mustermann\""))
              .entrySet()) {
        final var input = entry.getKey();
        final var comment = entry.getValue();
        final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
        final String octets =
            BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(octets));

        final var dut = new DerIa5String(inputStream);

        assertEquals("16" + octets, dut.toString());
        assertEquals(comment, dut.getComment());
      } // end For (entry...)
      // end --- a.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- b. smoke test with findings
    // Note: Intentionally no tests here. For reason see comment in
    //       test_invalidCharacters()-method.
  } // end method */

  /** Test method for {@link DerIa5String#getDecoded()}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_getDecoded() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with value constructor
    // --- b. smoke test with InputStream constructor

    // --- a. smoke test with value constructor
    for (final var decoded : Set.of("Foo Bar")) {
      final DerIa5String dut = new DerIa5String(decoded); // NOPMD new in loop

      assertSame(decoded, dut.getDecoded()); // ES_COMPARING_PARAMETER_STRING_WITH_EQ
    } // end For (decoded ...)

    // --- b. smoke test with InputStream constructor
    // Note 1: Hereafter only such characters are used which have
    //         identical code points in US_ASCII and T.61.
    // Note 2: Invalid characters causes a finding during construction rather than an exception.
    // Note 3: Invalid characters are substituted by • = bullet point.
    try {
      final String character = "abcDEF0123&§!_"; // valid characters
      final int supIndex = character.length() - 1;
      for (final var size : RNG.intsClosed(0, 1000, 20).toArray()) {
        final char[] chars = new char[size];

        for (int i = size; i-- > 0; ) { // NOPMD assignment in operands
          chars[i] = character.charAt(RNG.nextIntClosed(0, supIndex));
        } // end For (i...)

        final String decoded = new String(chars); // NOPMD String instantiation
        final byte[] valueField = DerIa5String.toBytes(decoded);
        final String input = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerIa5String(inputStream);

        assertEquals("16" + input, dut.toString());
        assertEquals(decoded, dut.getDecoded());
      } // end For (size...)
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link DerIa5String#invalidCharacters()}. */
  @Test
  void test_invalidCharacters() {
    // Assertions:
    // - none -

    // Note: Intentionally this test-method does not contain many tests,
    //       because the method-under-test is used in two places:
    //       a. DerIA5String(String), where an exception is thrown if an invalid
    //          character occurs during toBytes()-method.
    //       b. DerIA5String(InputStream), where it is impossible to inject
    //          invalid characters, because for each byte in the InputStream
    //          a valid character exists.
    //       Conclusion: It is not possible to construct a device-under-test
    //       with invalid characters by the current constructors in the
    //       class-under-test.

    // Test strategy:
    // --- a. smoke test
    {
      final var dut = new DerIa5String("Foo Bar invalid");

      assertFalse(dut.invalidCharacters());
    } // end --- a.
  } // end method */

  /** Test method for {@link DerIa5String#toBytes(String)}. */
  @Test
  void test_toBytes__String() {
    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. invalid input

    // --- a. smoke test with manually chosen input
    Map.ofEntries(
            Map.entry("Alfred", "416c66726564"),
            Map.entry("$", "24"),
            Map.entry("%", "25"),
            Map.entry("#", "23"),

            // characters which differ from ASCII
            Map.entry("§", "40"),
            Map.entry("Ä", "5b"),
            Map.entry("Ö", "5c"),
            Map.entry("Ü", "5d"),
            Map.entry("ä", "7b"),
            Map.entry("ö", "7c"),
            Map.entry("ü", "7d"),
            Map.entry("ß", "7e"),
            Map.entry("", ""))
        .forEach(
            (input, encoding) ->
                assertEquals(
                    encoding,
                    Hex.toHexDigits(DerIa5String.toBytes(input)),
                    input)); // end forEach(input -> ...)

    // --- b. invalid input
    for (final var input : Set.of("@", "[", "\\", "]", "{", "|", "}", "~")) {
      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> DerIa5String.toBytes(input));
      assertEquals(DerIa5String.MESSAGE, throwable.getMessage());
      final Throwable innerException = throwable.getCause();
      assertNotNull(innerException);
      assertEquals(IndexOutOfBoundsException.class, innerException.getClass());
    } // end For (input...)
  } // end method */
} // end class
