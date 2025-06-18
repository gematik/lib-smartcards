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

import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests on {@link DerOctetString}.
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
final class TestDerOctetString {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestDerOctetString.class);

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

  /** Test method for {@link DerOctetString#DerOctetString(byte[])}. */
  @Test
  void test_DerOctetString__byteA() {
    // Assertions:
    // ... a. super-constructor works as expected
    // ... b. toString()-method works as expected

    // Note: Because of the assertions a and be we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    for (final var octets :
        Set.of(
            "", // empty octet-string
            "42" //  one octet
            )) {
      final byte[] octetString = Hex.toByteArray(octets);
      final String expected =
          "04" // tag-field
              + BerTlv.getLengthField(octetString.length)
              + octets;

      final var dut = new DerOctetString(octetString); // NOPMD new in loop

      assertNotNull(dut.insValueField); // instance attribute properly set?
      assertNotSame(octetString, dut.insValueField); // defensive cloning?
      assertEquals(expected, dut.toString());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (octets ...)
  } // end method */

  /** Test method for {@link DerOctetString#DerOctetString(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerOctetString__ByteBuffer() {
    // Assertions:
    // ... a. super-constructor works as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. bunch of random octet-strings.
    // --- b. ERROR: ArithmeticException
    // --- c. ERROR: BufferUnderflowException

    // --- a. bunch of random octet-strings.
    for (final var length : RNG.intsClosed(0, 20, 10).toArray()) {
      final var octet = RNG.nextBytes(length);
      final var input =
          BerTlv.getLengthField(length) // length-field
              + Hex.toHexDigits(octet); // value-field
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = new DerOctetString(buffer);

      assertEquals("04" + input, dut.toString());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
      final byte[] deco1 = dut.getDecoded();
      assertNotNull(dut.insValueField);
      assertNotSame(octet, dut.insValueField);
      final byte[] deco2 = dut.getDecoded();
      assertNotSame(dut.insValueField, deco1);
      assertNotSame(dut.insValueField, deco2);
      assertNotSame(deco1, deco2);
      assertArrayEquals(octet, dut.insValueField);
      assertArrayEquals(octet, deco1);
      assertArrayEquals(octet, deco2);
    } // end For (length...)
    // end --- a.

    // --- b: ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerOctetString(buffer));

      assertNull(e.getCause());
    } // end --- b.

    // --- c. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerOctetString(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerOctetString(buffer));
    } // end For (input...)
    // end --- c.
  } // end method */

  /** Test method for {@link DerOctetString#DerOctetString(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerOctetString__InputStream() {
    // Assertions:
    // ... a. super-constructor works as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. bunch of random octet-strings.
    // --- b. ERROR: ArithmeticException
    // --- c. ERROR: IOException

    try {
      // --- a. bunch of random octet-strings.
      for (final var length : RNG.intsClosed(0, 20, 10).toArray()) {
        final var octet = RNG.nextBytes(length);
        final var input =
            BerTlv.getLengthField(length) // length-field
                + Hex.toHexDigits(octet); // value-field
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerOctetString(inputStream);

        assertEquals("04" + input, dut.toString());
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
        final byte[] deco1 = dut.getDecoded();
        assertNotNull(dut.insValueField);
        assertNotSame(octet, dut.insValueField);
        final byte[] deco2 = dut.getDecoded();
        assertNotSame(dut.insValueField, deco1);
        assertNotSame(dut.insValueField, deco2);
        assertNotSame(deco1, deco2);
        assertArrayEquals(octet, dut.insValueField);
        assertArrayEquals(octet, deco1);
        assertArrayEquals(octet, deco2);
      } // end For (length...)
      // end --- a.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- b: ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> new DerOctetString(inputStream));

      assertNull(thrown.getCause());
    } // end --- b.

    // --- c. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerOctetString(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- c.
  } // end method */

  /** Test method for {@link DerBoolean#getComment()}. */
  @Test
  void test_getComment() {
    // Note: This simple method does not need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    {
      final var expected = " # OCTETSTRING";
      for (final var length : RNG.intsClosed(0, 1024, 20).toArray()) {
        final var dut = new DerOctetString(RNG.nextBytes(length)); // NOPMD new in loop

        final var actual = dut.getComment();

        assertEquals(expected, actual);
      } // end For (length...)
    } // end --- a.
  } // end method */

  /** Test method for {@link DerOctetString#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Assertions:
    // ... a. constructors work as expected
    // ... b. super.getValue()-method works as expected

    // Note: Because of the assertions (especially assertion_b) this simple
    //       method does not need intensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final byte[] input = RNG.nextBytes(0, 20);
    final var dut = new DerOctetString(input);
    assertArrayEquals(input, dut.getDecoded());
  } // end method */

  /** Test method for {@link DerOctetString#toStringTree()}. */
  @Test
  void test_toStringTree() {
    // Test strategy:
    // --- a. smoke test
    for (final var entry :
        Map.ofEntries(
                Map.entry("04 00", "04 00 # OCTETSTRING"), // empty octet-string
                Map.entry(
                    "04 01 42", "04 01 42 # OCTETSTRING"), // non-empty, content not a TLV-object
                // non-empty, primitive TLV-object
                Map.entry(
                    "04 02  (81 00)",
                    String.join(
                        LINE_SEPARATOR,
                        "04 02 8100 # OCTETSTRING",
                        "   ##########",
                        "   # 81 00",
                        "   ##########")),
                // non-empty, constructed TLV-object
                Map.entry(
                    "04 05 [a4 03 (82 01 42)]",
                    String.join(
                        LINE_SEPARATOR,
                        "04 05 a403820142 # OCTETSTRING",
                        "   ##########",
                        "   # a4 03",
                        "   # |  82 01 42",
                        "   ##########")),
                // non-empty, primitive ASN.1 content
                Map.entry(
                    "04 03 (01 01 00)",
                    String.join(
                        LINE_SEPARATOR,
                        "04 03 010100 # OCTETSTRING",
                        "   ##########",
                        "   # 01 01 00 # BOOLEAN := false",
                        "   ##########")),
                // non-empty, constructed ASN.1 content
                Map.entry(
                    "04 05 [30 03 (01 01 ff)]",
                    String.join(
                        LINE_SEPARATOR,
                        "04 05 30030101ff # OCTETSTRING",
                        "   ##########",
                        "   # 30 03 # SEQUENCE with 1 element",
                        "   # |  01 01 ff # BOOLEAN := true",
                        "   ##########")),
                // non-empty, another octet-string, not a TLV-object
                Map.entry(
                    "04 05 (04 03 112233)",
                    String.join(
                        LINE_SEPARATOR,
                        "04 05 0403112233 # OCTETSTRING",
                        "   ##########",
                        "   # 04 03 112233 # OCTETSTRING",
                        "   ##########")),
                // non-empty, another octet-string, primitive TLV-object
                Map.entry(
                    "04 05 [04 03 (83 01 74)]",
                    String.join(
                        LINE_SEPARATOR,
                        "04 05 0403830174 # OCTETSTRING",
                        "   ##########",
                        "   # 04 03 830174 # OCTETSTRING",
                        "   #    ##########",
                        "   #    # 83 01 74",
                        "   #    ##########",
                        "   ##########")),
                // non-empty, another octet-string, primitive ASN.1-object
                Map.entry(
                    "04 05 [04 03 (01 01 00)]",
                    String.join(
                        LINE_SEPARATOR,
                        "04 05 0403010100 # OCTETSTRING",
                        "   ##########",
                        "   # 04 03 010100 # OCTETSTRING",
                        "   #    ##########",
                        "   #    # 01 01 00 # BOOLEAN := false",
                        "   #    ##########",
                        "   ##########")),
                // non-empty, two primitive TlV-objects
                Map.entry(
                    "04 06 [(81 01 27) (82 01 28)]",
                    String.join(
                        LINE_SEPARATOR,
                        "04 06 810127820128 # OCTETSTRING",
                        "   ##########",
                        "   # 81 01 27",
                        "   # 82 01 28",
                        "   ##########")))
            .entrySet()) {
      final var input = entry.getKey();
      final var output = entry.getValue();
      LOGGER.atTrace().log("input: {}", input);

      assertEquals(output, BerTlv.getInstance(input).toStringTree());
    } // end For (entry...)
  } // end method */
} // end class
