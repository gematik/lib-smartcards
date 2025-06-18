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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Class performing white-box test on {@link DerRestrictedCharacterStringTypes}. */
// Note 1: Spotbugs claims: "ES_COMPARING_STRINGS_WITH_EQ"
//         Explanation: This code compares java.lang.String objects for reference
//             equality using the == or != operators. Unless both strings
//             are either constants in a source file, or have been interned
//             using the String.intern() method, the same string value may
//             be represented by two different String objects.
//             Consider using the equals(Object) method instead.
//         The finding is correct. Intentionally, the code is NOT changed.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ES_COMPARING_STRINGS_WITH_EQ" // see note 1
}) // */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerRestrictedCharacterStringTypes {

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

  /**
   * Tests {@link DerRestrictedCharacterStringTypes#DerRestrictedCharacterStringTypes(int, String)}.
   */
  @Test
  void test_DerRestrictedCharacterStringTypes__int_String() {
    // Assertions:
    // ... a. toBytes()-method works as expected

    // Test strategy:
    // --- a. only valid characters
    // --- b. some "invalid" characters

    // --- a. only valid characters
    {
      final String input = "Hello";
      final var dut = new DerMyString(DerPrintableString.TAG, input);
      assertEquals(DerPrintableString.TAG, dut.getTag());
      assertSame(input, dut.insDecoded); // ES_COMPARING_STRINGS_WITH_EQ
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end --- a.

    // --- b. some "invalid" characters
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> new DerMyString(DerPrintableString.TAG, "12345678901"));

      assertEquals(DerRestrictedCharacterStringTypes.MESSAGE, throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- b.
  } // end method */

  /**
   * Tests {@link DerRestrictedCharacterStringTypes#DerRestrictedCharacterStringTypes(byte[],
   * ByteBuffer)}.
   */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerRestrictedCharacterStringTypes__byteA_ByteBuffer() {
    // Assertions:
    // ... a. constructors from superclasses work as expected

    // Test strategy:
    // --- a. valid DER-object
    // --- b. invalid DER-object

    final int tag = 0;
    final var tagField = new byte[] {(byte) tag};
    final var inputs =
        Set.of(
            // --- a. valid DER-object
            "",
            "a",
            "abcD 1",

            // --- b. invalid DER-object
            "a 3456789a",
            "asldkfjasldkf239485",
            "aslkdfj2309487539845");

    for (final var string : inputs) {
      final byte[] octets = string.getBytes(StandardCharsets.US_ASCII);
      final String input = BerTlv.getLengthField(octets.length) + Hex.toHexDigits(octets);

      final var dut = new DerMyString(tagField, ByteBuffer.wrap(Hex.toByteArray(input)));

      assertEquals(tag, dut.getTag());
      assertEquals(octets.length, dut.getLengthOfValueField());
      assertEquals(string, dut.insDecoded);
      assertEquals(!dut.invalidCharacters(), dut.isValid());

      if (dut.isValid()) {
        assertTrue(dut.insFindings.isEmpty());
      } else {
        assertEquals(1, dut.insFindings.size());
        assertEquals(DerRestrictedCharacterStringTypes.MESSAGE, dut.insFindings.getFirst());
      } // end else
    } // end For (string...)
  } // end method */

  /**
   * Tests {@link DerRestrictedCharacterStringTypes#DerRestrictedCharacterStringTypes(byte[],
   * InputStream)}.
   */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerRestrictedCharacterStringTypes__byteA_InputStream() {
    // Assertions:
    // ... a. constructors from superclasses work as expected

    // Test strategy:
    // --- a. valid DER-object
    // --- b. invalid DER-object

    final int tag = 0;
    final var tagField = new byte[] {(byte) tag};
    final var inputs =
        Set.of(
            // --- a. valid DER-object
            "",
            "a",
            "abcD 1",

            // --- b. invalid DER-object
            "a 3456789a",
            "asldkfjasldkf239485",
            "aslkdfj2309487539845");

    for (final var string : inputs) {
      try {
        final byte[] octets = string.getBytes(StandardCharsets.US_ASCII);
        final String input = BerTlv.getLengthField(octets.length) + Hex.toHexDigits(octets);

        final var dut = new DerMyString(tagField, new ByteArrayInputStream(Hex.toByteArray(input)));

        assertEquals(tag, dut.getTag());
        assertEquals(octets.length, dut.getLengthOfValueField());
        assertEquals(string, dut.insDecoded);
        assertEquals(!dut.invalidCharacters(), dut.isValid());

        if (dut.isValid()) {
          assertTrue(dut.insFindings.isEmpty());
        } else {
          assertEquals(1, dut.insFindings.size());
          assertEquals(DerRestrictedCharacterStringTypes.MESSAGE, dut.insFindings.getFirst());
        } // end else
      } catch (IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (string...)
  } // end method */

  /** Test method for {@link DerRestrictedCharacterStringTypes#getComment(String)}. */
  @Test
  void test_getComment__String() {
    // Test strategy:
    // --- a. loop over a bunch of "types" and "value-fields"
    for (final var type : Set.of("a", "asdf")) {
      for (final var string :
          Set.of(
              // --- valid
              "",
              "asd",
              "123456789",

              // --- invalid
              "123456789a")) {
        final var dut = new DerMyString(DerPrintableString.TAG, string); // NOPMD new in loop

        assertEquals(
            DerSpecific.DELIMITER + type + " := \"" + string + '"' + dut.getFindings(),
            dut.getComment(type));
      } // end For (string...)
    } // end For (types...)
  } // end method */

  @Test
  void test_getDecoded() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    for (final var string : Set.of("", "asdf")) {
      assertEquals(string, new DerMyString(DerPrintableString.TAG, string).getDecoded()); // NOPMD
    } // end For (string...)
  } // end method */

  @Test
  void test_toBytes() {
    // Assertions:
    // ... a. toBytes()-method works as expected for subclasses

    // Note: From the assertions it follows, that here we can concentrate on
    //       corner cases.

    // Test strategy:
    // --- a. smoke test with DerPrintableString
    // --- b. smoke test with DerTeletexString
    // --- c. smoke test with unknown tag

    // --- a. smoke test with DerPrintableString
    {
      final String input = "23049";
      assertArrayEquals(
          DerPrintableString.toBytes(input),
          DerRestrictedCharacterStringTypes.toBytes(DerPrintableString.TAG, input));
    } // end --- a.

    // --- b. smoke test with DerTeletexString
    {
      final String input = "23049";
      assertArrayEquals(
          DerTeletexString.toBytes(input),
          DerRestrictedCharacterStringTypes.toBytes(DerTeletexString.TAG, input));
    } // end --- b.

    // --- c. smoke test with unknown tag
    {
      final int tag = DerBitString.TAG;
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> DerRestrictedCharacterStringTypes.toBytes(tag, "asdf"));
      assertEquals(String.format("tag = '%x' not (yet) implemented", tag), throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- c.
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /** Class used for testing abstract superclass. */
  // Note 1: Spotbugs claims "CT_CONSTRUCTOR_THROW"
  //         Short message: Classes that throw exceptions in their constructors
  //             are vulnerable to Finalizer attacks.
  //         That finding is not correct, because an empty finalize() declared
  //             "final" is present in superclass.
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "CT_CONSTRUCTOR_THROW" // see note 1
  }) // */
  /* package */ static class DerMyString extends DerRestrictedCharacterStringTypes {

    /**
     * Comfort constructor using value.
     *
     * @param tag used for tag-field
     * @param value encoded in this primitive TLV object
     * @throws IllegalArgumentException if parameter {@code value} contains invalid characters, see
     *     {@link #invalidCharacters()}
     */
    /* package */ DerMyString(final int tag, final String value) {
      super(tag, value);
    } // end inner constructor */

    /** From {@link ByteBuffer} constructor. */
    /* package */ DerMyString(final byte[] tag, final ByteBuffer buffer) {
      // CT_CONSTRUCTOR_THROW
      super(tag, buffer);
    } // end inner constructor */

    /** From {@link InputStream} constructor. */
    /* package */ DerMyString(final byte[] tag, final InputStream inputStream) throws IOException {
      // CT_CONSTRUCTOR_THROW
      super(tag, inputStream);
    } // end inner constructor */

    /**
     * Converts given {@code byte[]} into corresponding {@link String}.
     *
     * <p>This is the inverse function to {@link #toBytes(int, String)}.
     *
     * @param octets to be converted
     * @return corresponding {@link String}
     */
    /* package */
    @Override
    String fromBytes(final byte[] octets) {
      // Mimics DerPrintableString
      return new String(octets, StandardCharsets.US_ASCII);
    } // end inner method */

    /**
     * Checks for invalid characters.
     *
     * @return {@code TRUE} if value-field contains invalid characters, {@code FALSE} otherwise
     */
    /* package */
    @Override
    boolean invalidCharacters() {
      return insDecoded.length() > 10;
    } // end inner method */

    /**
     * Returns a comment describing the content of the object.
     *
     * @return comment about the TLV-object content
     */
    @Override
    public String getComment() {
      return "";
    } // end inner method */
  } // end inner class
} // end class
