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

import static de.gematik.smartcards.tlv.DerBitString.EM_7;
import static de.gematik.smartcards.tlv.DerBitString.EM_GT0;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box tests on {@link DerBitString}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "ES_COMPARING_PARAMETER_STRING_WITH_EQ", i.e.
//         Comparison of String parameter using == or !=
//         That finding is correct, but intentionally assertSame(...) is used.
// Note 2: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.,
//         Non-null field is not initialized
//         This finding is for a class attribute which is initialized by JUnit.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ES_COMPARING_STRINGS_WITH_EQ", // see note 1
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" // see note 2
}) // */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerBitString {

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

  /** Test method for {@link DerBitString#DerBitString(byte[])}. */
  @Test
  void test_DerBitString__byteA() {
    // Assertions:
    // ... a. underlying DerBitString(int, byte[])-constructor works as expected
    // ... b. toString()-method works as expected

    // Note: Because of the assertions a and be we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. some manually chosen input
    // --- c. check for defensive cloning

    // --- a. smoke test
    {
      final var octets = Hex.toByteArray("af ed");

      final var dut = new DerBitString(octets);

      assertEquals(DerBitString.TAG, dut.getTag());
      assertEquals(0, dut.getNumberOfUnusedBits());
      assertTrue(dut.isValid());
      assertArrayEquals(octets, dut.getDecoded());
    } // end --- a.

    // --- b. some manually chosen input
    Set.of(
            "", // empty bit-string
            "42", // one octet
            "affa" // two octets
            )
        .forEach(
            input -> {
              final byte[] octets = Hex.toByteArray(input);
              final String exp =
                  "03" // tag-field
                      + BerTlv.getLengthField(1 + octets.length)
                      + "00"
                      + input;
              final DerBitString dut = new DerBitString(octets);

              // --- c. check for defensive cloning
              assertNotNull(dut.insDecoded);
              assertNotSame(octets, dut.insDecoded);
              assertArrayEquals(octets, dut.insDecoded);

              assertNull(dut.insBitString);
              assertEquals(0, dut.getNumberOfUnusedBits());
              assertEquals(input, Hex.toHexDigits(dut.getDecoded()));
              assertEquals(exp, dut.toString());
              assertTrue(dut.isValid());
              assertTrue(dut.insFindings.isEmpty());
            }); // end forEach(octets -> ...)
  } // end method */

  /** Test method for {@link DerBitString#DerBitString(int, byte[])}. */
  @Test
  void test_DerBitString__int_byteA() {
    // Assertions:
    // ... a. super-constructor works as expected
    // ... b. toString()-method works as expected

    // Note: Because of the assertion a and be we can be lazy here.

    // Test strategy:
    // --- a. value-field with minimal length
    // --- b. all valid values for numberOfUnusedBits
    // --- c. bunch of random bit-strings.
    // --- d. check for defensive cloning
    // --- e. ERROR: numberOfUnusedBits > 0 but empty bit-string
    // --- f. ERROR: numberOfUnusedBits out of range

    // --- a. value-field with minimal length
    {
      final byte[] octets = AfiUtils.EMPTY_OS;
      final DerBitString dut = new DerBitString(0, octets);
      assertNotNull(dut.insDecoded);
      assertNotSame(octets, dut.insDecoded);
      assertArrayEquals(octets, dut.insDecoded);
      assertNull(dut.insBitString);
      assertEquals(0, dut.getNumberOfUnusedBits());
      assertEquals("030100", dut.toString());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    }

    // --- b. all valid values for numberOfUnusedBits
    // --- c. bunch of random bit-strings.
    IntStream.rangeClosed(0, 7)
        .forEach(
            noUnusedBits -> {
              final String msByte =
                  String.format("%02x", noUnusedBits); // NOPMD "%02x" appears often

              // Note: Intentionally the following loop starts with 1 rather than 0.
              //       Reason 1: Empty bit-string is handled under a.
              //       Reason 2: An exception is thrown for length = 0, but noUnusedBits > 0.
              RNG.intsClosed(1, 1024, 10)
                  .forEach(
                      length -> {
                        final byte[] octets = RNG.nextBytes(length);
                        final DerBitString dut = new DerBitString(noUnusedBits, octets);

                        // --- d. check for defensive cloning
                        assertNotNull(dut.insDecoded);
                        assertNotSame(octets, dut.insDecoded);
                        assertArrayEquals(octets, dut.insDecoded);

                        assertNull(dut.insBitString);
                        assertEquals(noUnusedBits, dut.getNumberOfUnusedBits());
                        assertEquals(
                            "03"
                                + BerTlv.getLengthField(1 + length)
                                + msByte
                                + Hex.toHexDigits(octets),
                            dut.toString());
                        assertTrue(dut.isValid());
                        assertTrue(dut.insFindings.isEmpty());
                      }); // end forEach(length -> ...)
            }); // end forEach(noUnusedBits -> ...)

    // --- e. ERROR: numberOfUnusedBits > 0 but empty bit-string
    IntStream.rangeClosed(1, 7)
        .forEach(
            noUnusedBits -> {
              final Throwable thrown =
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> new DerBitString(noUnusedBits, AfiUtils.EMPTY_OS));
              assertEquals(EM_GT0, thrown.getMessage());
              assertNull(thrown.getCause());
            }); // end forEach(noUnusedBits -> ...)

    // --- f. ERROR: numberOfUnusedBits out of range
    // f.1 numberOfUnusedBits < 0
    // f.2 numberOfUnusedBits > 7
    RNG.intsClosed(1, 1024, 10)
        .forEach(
            length -> {
              final byte[] octets = RNG.nextBytes(length);

              // f.1 numberOfUnusedBits < 0
              RNG.intsClosed(-300, -1, 200)
                  .forEach(
                      noUnusedBits -> {
                        final Throwable thrown =
                            assertThrows(
                                IllegalArgumentException.class,
                                () -> new DerBitString(noUnusedBits, octets));
                        assertEquals("numberOfUnusedBits out of range [0, 7]", thrown.getMessage());
                        assertNull(thrown.getCause());
                      }); // end forEach(noUnusedBits -> ...)

              // f.2 numberOfUnusedBits > 7
              RNG.intsClosed(8, 300, 200)
                  .forEach(
                      noUnusedBits -> {
                        final Throwable thrown =
                            assertThrows(
                                IllegalArgumentException.class,
                                () -> new DerBitString(noUnusedBits, octets));
                        assertEquals("numberOfUnusedBits out of range [0, 7]", thrown.getMessage());
                        assertNull(thrown.getCause());
                      }); // end forEach(noUnusedBits -> ...)
            }); // end forEach(length -> ...)
  } // end method */

  /** Test method for {@link DerBitString#DerBitString(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerBitString__ByteBuffer() {
    // Assertions:
    // ... a. super-constructor works as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. value-field with minimal length
    // --- b. all valid values for numberOfUnusedBits
    // --- c. a bunch of random bit-strings
    // --- d. FINDING: numberOfUnusedBits > 0 but empty bit-string
    // --- e. FINDING: numberOfUnusedBits out of range
    // --- f. FINDING: value-field absent
    // --- g. ERROR: ArithmeticException
    // --- h. ERROR: BufferUnderflowException

    // Loop over all possible values for MSByte
    // --- b. all valid values for numberOfUnusedBits
    for (final var msByte : IntStream.rangeClosed(0x00, 0xff).toArray()) {
      final String unusedBits = String.format("%02x", msByte);

      // --- a. value-field with minimal length
      // --- c. a bunch of random bit-strings
      for (final var length : RNG.intsClosed(0, 20, 10).toArray()) {
        final byte[] octets = RNG.nextBytes(length);
        final String input =
            BerTlv.getLengthField(1 + length) // length-field
                + unusedBits
                + Hex.toHexDigits(octets); // value-field
        final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

        final var dut = new DerBitString(buffer);

        assertEquals(msByte, dut.getNumberOfUnusedBits());
        assertNull(dut.insDecoded);
        assertNull(dut.insBitString);

        if (msByte > 7) { // NOPMD literal in if statement
          // --- e. FINDING: numberOfUnusedBits out of range
          assertFalse(dut.isValid());
          assertEquals(List.of(EM_7), dut.insFindings);
        } else if ((msByte > 0) && (0 == length)) {
          // --- d. FINDING: numberOfUnusedBits > 0 but empty bit-string
          assertFalse(dut.isValid());
          assertEquals(List.of(EM_GT0), dut.insFindings);
        } else {
          // ... input okay
          assertTrue(dut.isValid());
          assertTrue(dut.insFindings.isEmpty());
        } // end else
      } // end For (length ...)
    } // end For (msByte ...)

    // --- f. FINDING: value-field absent
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("-00"));

      final var dut = new DerBitString(buffer);

      assertEquals(0, dut.getNumberOfUnusedBits());
      assertNull(dut.insDecoded);
      assertNull(dut.insBitString);
      assertFalse(dut.isValid());
      assertEquals(List.of("value-field absent"), dut.insFindings);
    } // end --- f.

    // --- g. ERROR: ArithmeticException
    {
      final var buffer =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerBitString(buffer));

      assertNull(e.getCause());
    } // end --- g.

    // --- h. ERROR: BufferUnderflowException
    for (final var input : Set.of("--00", "-8100", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerBitString(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerBitString(buffer));
    } // end For (input...)
    // end --- h.
  } // end method */

  /** Test method for {@link DerBitString#DerBitString(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerBitString__InputStream() {
    // Assertions:
    // ... a. super-constructor works as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. value-field with minimal length
    // --- b. all valid values for numberOfUnusedBits
    // --- c. a bunch of random bit-strings
    // --- d. FINDING: numberOfUnusedBits > 0 but empty bit-string
    // --- e. FINDING: numberOfUnusedBits out of range
    // --- f. FINDING: value-field absent
    // --- g. ERROR: ArithmeticException
    // --- h. ERROR: IOException

    try {
      // Loop over all possible values for MSByte
      // --- b. all valid values for numberOfUnusedBits
      for (final var msByte : IntStream.rangeClosed(0x00, 0xff).toArray()) {
        final String unusedBits = String.format("%02x", msByte);

        // --- a. value-field with minimal length
        // --- c. a bunch of random bit-strings
        for (final var length : RNG.intsClosed(0, 20, 10).toArray()) {
          final byte[] octets = RNG.nextBytes(length);
          final String input =
              BerTlv.getLengthField(1 + length) // length-field
                  + unusedBits
                  + Hex.toHexDigits(octets); // value-field
          final ByteArrayInputStream bais = new ByteArrayInputStream(Hex.toByteArray(input));

          final var dut = new DerBitString(bais);

          assertEquals(msByte, dut.getNumberOfUnusedBits());
          assertNull(dut.insDecoded);
          assertNull(dut.insBitString);

          if (msByte > 7) { // NOPMD literal in if statement
            // --- e. FINDING: numberOfUnusedBits out of range
            assertFalse(dut.isValid());
            assertEquals(List.of(EM_7), dut.insFindings);
          } else if ((msByte > 0) && (0 == length)) {
            // --- d. FINDING: numberOfUnusedBits > 0 but empty bit-string
            assertFalse(dut.isValid());
            assertEquals(List.of(EM_GT0), dut.insFindings);
          } else {
            // ... input okay
            assertTrue(dut.isValid());
            assertTrue(dut.insFindings.isEmpty());
          } // end else
        } // end For (length ...)
      } // end For (msByte ...)

      // --- f. FINDING: value-field absent
      {
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray("---00"));

        final var dut = new DerBitString(inputStream);

        assertEquals(0, dut.getNumberOfUnusedBits());
        assertNull(dut.insDecoded);
        assertNull(dut.insBitString);
        assertFalse(dut.isValid());
        assertEquals(List.of("value-field absent"), dut.insFindings);
      } // end --- f.

      // --- g. ERROR: ArithmeticException
      {
        final var inputStream =
            new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

        final Throwable thrownD =
            assertThrows(ArithmeticException.class, () -> new DerBitString(inputStream));

        assertNull(thrownD.getCause());
      } // end --- g.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- h. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-h.bin");
      Files.write(path, Hex.toByteArray("-02-0023"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerBitString(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- h.
  } // end method */

  /** Test method for {@link DerBitString#getBitString()}. */
  @Test
  void test_getBitString() {
    // Assertions:
    // ... a. constructors work as expected
    // ... b. toBitString(...)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. read instance attribute again

    // --- a. smoke test
    final DerBitString dut = new DerBitString(1, Hex.toByteArray("f3a7"));
    assertNull(dut.insBitString);

    final String bitString = dut.getBitString();
    assertNotNull(dut.insBitString);
    assertEquals("11110011 1010011", bitString);

    // --- b. read instance attribute again
    assertSame(bitString, dut.getBitString()); // ES_COMPARING_STRINGS_WITH_EQ
  } // end method */

  /** Test method for {@link DerBitString#getComment()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity"})
  @Test
  void test_getComment() throws IOException {
    // Assertions:
    // ... a. constructors work as expected
    // ... b. getBitString()-method works as expected

    // Note: This simple method does not need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with value-constructor
    // --- b. smoke test with stream-constructor
    // --- c. FINDING: value-field absent
    // --- d. FINDING: numberOfUnusedBits out of range, value-constructor
    // --- e. FINDING: numberOfUnusedBits > 0 but empty bit-string, value-constructor
    // --- f. FINDING: numberOfUnusedBits out of range, stream-constructor
    // --- g. FINDING: numberOfUnusedBits > 0 but empty bit-string, stream-constructor

    // --- a. smoke test with value-constructor
    assertEquals(
        " # BITSTRING: 0 unused bits: ''", new DerBitString(AfiUtils.EMPTY_OS).getComment());
    Map.ofEntries(
            Map.entry(0, "7f"),
            Map.entry(1, "7e"),
            Map.entry(2, "427c"),
            Map.entry(3, "4278"),
            Map.entry(4, "4270"),
            Map.entry(5, "4260"),
            Map.entry(6, "4240"),
            Map.entry(7, "4200"))
        .forEach(
            (noUnusedBits, input) -> {
              final var dut = new DerBitString(noUnusedBits, Hex.toByteArray(input));

              assertEquals(
                  String.format(
                      " # BITSTRING: %d unused bit%s: '%s'",
                      noUnusedBits, (1 == noUnusedBits) ? "" : "s", dut.getBitString()),
                  dut.getComment());
            }); // end forEach((noUnusedBits, input) -> ...)

    // --- b. smoke test with stream-constructor
    assertEquals(
        " # BITSTRING: 6 unused bits: '11'",
        new DerBitString(new ByteArrayInputStream(Hex.toByteArray("-02-06c0"))).getComment());

    // --- c. FINDING: value-field absent
    {
      final var inputStream = new ByteArrayInputStream(Hex.toByteArray(".00"));

      final var dut = new DerBitString(inputStream);

      assertEquals(
          " # BITSTRING: 0 unused bits: '', findings: value-field absent", dut.getComment());
    } // end --- c.

    // --- d. FINDING: numberOfUnusedBits out of range, value-constructor
    // --- e. FINDING: numberOfUnusedBits > 0 but empty bit-string, value-constructor
    for (final var msByte : IntStream.rangeClosed(0x00, 0xff).toArray()) {
      final String unusedBits = String.format("%02x", msByte & 0xff);

      for (final var length : RNG.intsClosed(0, 20, 10).toArray()) {
        final var octet = RNG.nextBytes(length);
        final var input =
            BerTlv.getLengthField(1 + length) // length-field
                + unusedBits
                + Hex.toHexDigits(octet); // value-field
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerBitString(inputStream);

        if (msByte > 7) { // NOPMD literal in if statement
          // --- f. FINDING: numberOfUnusedBits out of range, stream-constructor
          assertEquals(
              String.format(
                  " # BITSTRING: %d unused bits: '%s', findings: %s",
                  msByte, dut.getBitString(), EM_7),
              dut.getComment());
        } else if ((msByte > 0) && (0 == length)) {
          // --- g. FINDING: numberOfUnusedBits > 0 but empty bit-string,
          // stream-constructor
          assertEquals(
              String.format(
                  " # BITSTRING: %d unused bit%s: '%s', findings: %s",
                  msByte, (1 == msByte) ? "" : "s", dut.getBitString(), EM_GT0),
              dut.getComment());
        } else {
          // ... input okay
          assertEquals(
              String.format(
                  " # BITSTRING: %d unused bit%s: '%s'",
                  msByte, (1 == msByte) ? "" : "s", dut.getBitString()),
              dut.getComment());
        } // end else
      } // end For (length...)
    } // end For (msByte...)
  } // end method */

  /** Test method for {@link DerBitString#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Assertions:
    // ... a. constructors work as expected
    // ... b. toByteArray()-method works as expected

    // Test strategy:
    // --- a. getDecoded after DerBitString(byte[])-constructor
    // --- b. getDecoded after DerBitString(InputStream)-constructor
    // --- c. getDecoded with empty value-field

    // --- a. getDecoded after DerBitString(byte[])-constructor
    IntStream.rangeClosed(0, 10)
        .forEach(
            length -> {
              final byte[] octets = RNG.nextBytes(length);
              final DerBitString dut = new DerBitString(octets);
              assertNotNull(dut.insDecoded);
              assertNotSame(octets, dut.insDecoded);
              final byte[] decoded = dut.getDecoded();
              assertArrayEquals(octets, decoded);
              assertNotSame(decoded, dut.insDecoded);
            }); // end forEach(length -> ...)

    // --- b. getDecoded after DerBitString(InputStream)-constructor
    IntStream.rangeClosed(0, 10)
        .forEach(
            length -> {
              final byte[] octets = RNG.nextBytes(length);
              final byte[] input = new DerBitString(octets).getEncoded();
              final ByteArrayInputStream bais = new ByteArrayInputStream(input);
              final BerTlv dutGen = BerTlv.getInstance(bais);
              assertEquals(DerBitString.class, dutGen.getClass());
              final DerBitString dut = (DerBitString) dutGen;
              assertNull(dut.insDecoded);
              final byte[] deco1 = dut.getDecoded();
              assertNotNull(dut.insDecoded);
              assertNotSame(octets, dut.insDecoded);
              final byte[] deco2 = dut.getDecoded();
              assertNotSame(dut.insDecoded, deco1);
              assertNotSame(dut.insDecoded, deco2);
              assertNotSame(deco1, deco2);
              assertArrayEquals(octets, dut.insDecoded);
              assertArrayEquals(octets, deco1);
              assertArrayEquals(octets, deco2);
            }); // end forEach(length -> ...)

    // --- c. getDecoded with empty value-field
    {
      final BerTlv dutGen = BerTlv.getInstance("0300");
      assertEquals(DerBitString.class, dutGen.getClass());
      final DerBitString dut = (DerBitString) dutGen;
      assertNull(dut.insDecoded);
      assertEquals(0, dut.getDecoded().length);
    }
  } // end method */

  /** Test method for {@link DerBitString#getNumberOfUnusedBits()}. */
  @Test
  void test_getNumberOfUnusedBits() {
    // Assertions:
    // ... a. constructors work as expected
    // ... b. toByteArray()-method works as expected

    // Test strategy:
    // --- a. value-field with minimal length
    // --- b. all valid values for numberOfUnusedBits
    // --- c. bunch of random bit-strings.
    // --- d. getNumberOfUnusedBits after DerBitString(InputStream)-constructor
    // --- e. empty value-field

    // --- a. value-field with minimal length
    assertEquals(0, new DerBitString(0, AfiUtils.EMPTY_OS).getNumberOfUnusedBits());

    // --- b. all valid values for numberOfUnusedBits
    // --- c. bunch of random bit-strings.
    IntStream.rangeClosed(0, 7)
        .forEach(
            noUnusedBits -> {
              RNG.intsClosed(1, 1024, 10)
                  .forEach(
                      length -> {
                        final byte[] octets = RNG.nextBytes(length);
                        final DerBitString dut = new DerBitString(noUnusedBits, octets);
                        assertEquals(noUnusedBits, dut.getNumberOfUnusedBits());
                      }); // end forEach(length -> ...)
            }); // end forEach(noUnusedBits -> ...)

    // --- d. getNumberOfUnusedBits after DerBitString(InputStream)-constructor
    IntStream.rangeClosed(0, 7)
        .forEach(
            msByte -> {
              final String unusedBits = String.format("%02x", msByte);

              // bunch of random bit-strings.
              RNG.intsClosed(1, 20, 10)
                  .forEach(
                      length -> {
                        final byte[] octet = RNG.nextBytes(length);
                        final String input =
                            "03" // tag-field
                                + BerTlv.getLengthField(1 + length) // length-field
                                + unusedBits
                                + Hex.toHexDigits(octet); // value-field
                        final ByteArrayInputStream bais =
                            new ByteArrayInputStream(Hex.toByteArray(input));

                        final BerTlv dutGen = BerTlv.getInstance(bais);
                        assertEquals(DerBitString.class, dutGen.getClass());
                        final DerBitString dut = (DerBitString) dutGen;
                        assertEquals(msByte, dut.getNumberOfUnusedBits());
                      }); // end forEach(length -> ...)
            }); // end forEach(msByte -> ...)

    // --- e. empty value-field
    final BerTlv dutGen = BerTlv.getInstance("0300");
    assertEquals(DerBitString.class, dutGen.getClass());
    final DerBitString dut = (DerBitString) dutGen;
    assertEquals(0, dut.getNumberOfUnusedBits());
  } // end method */

  /** Test method for {@link DerBitString#toBitString(int, byte[])}. */
  @Test
  void test_toBitString__int_byteA() {
    // Test strategy:
    // --- a. smoke test
    // --- b. some with manually chosen values
    // --- c. random input with no unused bits
    // --- d. random input with unused bits
    // --- e. empty bit-string because of too many unused bits

    // --- a. smoke test
    {
      final var expected = "10100101 11";
      final var noUnusedBits = 6;
      final var octets = Hex.toByteArray("a5 ff");

      final var present = DerBitString.toBitString(noUnusedBits, octets);

      assertEquals(expected, present);
    } // end --- a.

    // --- b. some manually chosen values
    Map.ofEntries(
            // no unused bits
            Map.entry(new Object[] {0, "00"}, "00000000"),
            Map.entry(new Object[] {0, "01"}, "00000001"),
            Map.entry(new Object[] {0, "02"}, "00000010"),
            Map.entry(new Object[] {0, "04"}, "00000100"),
            Map.entry(new Object[] {0, "08"}, "00001000"),
            Map.entry(new Object[] {0, "10"}, "00010000"),
            Map.entry(new Object[] {0, "20"}, "00100000"),
            Map.entry(new Object[] {0, "40"}, "01000000"),
            Map.entry(new Object[] {0, "80"}, "10000000"),
            Map.entry(new Object[] {0, "01 38"}, "00000001 00111000"),
            Map.entry(new Object[] {0, "02 54"}, "00000010 01010100"),
            Map.entry(new Object[] {0, "04 73"}, "00000100 01110011"),
            Map.entry(new Object[] {0, "08 02"}, "00001000 00000010"),
            Map.entry(new Object[] {0, "10 ad"}, "00010000 10101101"),
            Map.entry(new Object[] {0, "20 89"}, "00100000 10001001"),
            Map.entry(new Object[] {0, "40 cf"}, "01000000 11001111"),
            Map.entry(new Object[] {0, "80 b1"}, "10000000 10110001"),

            // some unused bits
            Map.entry(new Object[] {1, "a3"}, "1010001"),
            Map.entry(new Object[] {2, "a3"}, "101000"),
            Map.entry(new Object[] {3, "a3"}, "10100"),
            Map.entry(new Object[] {4, "a3"}, "1010"),
            Map.entry(new Object[] {5, "a3"}, "101"),
            Map.entry(new Object[] {6, "a3"}, "10"),
            Map.entry(new Object[] {7, "a3"}, "1"),
            Map.entry(new Object[] {7, "6ea3"}, "01101110 1"))
        .forEach(
            (input, expected) -> {
              final int noUnusedBits = (int) input[0];
              final byte[] octets = Hex.toByteArray((String) input[1]);

              assertEquals(expected, DerBitString.toBitString(noUnusedBits, octets), expected);
            }); // end forEach((input, expected) -> ...)

    // --- c. random input with no unused bits
    RNG.intsClosed(1, 20, 5)
        .forEach(
            size -> {
              final StringBuilder octets = new StringBuilder();
              final List<String> expected = new ArrayList<>();

              for (int i = 0; i < size; i++) {
                final int octet = RNG.nextIntClosed(0, 255);
                octets.append(String.format("%02x", octet));
                final String bits = "00000000" + Integer.toBinaryString(octet);

                expected.add(bits.substring(bits.length() - 8));
              } // end For (i...)

              assertEquals(
                  String.join(" ", expected),
                  DerBitString.toBitString(0, Hex.toByteArray(octets.toString())));
            }); // end forEach(size -> ...)

    // --- d. random input with unused bits
    // Note: Intentionally no checks here, because manually chosen tests seems sufficient.

    // --- d. empty bit-string because of too many unused bits
    Map.ofEntries(
            Map.entry(8, "00"),
            Map.entry(15, "00"),
            Map.entry(16, "1122"),
            Map.entry(248, "1123456789abcdef 2123456789abcdef 3123456789abcdef 4123456789abcd"),
            Map.entry(255, "a123456789abcdef b123456789abcdef c123456789abcdef d123456789abcd"))
        .forEach(
            (noUnusedBits, octets) ->
                assertEquals(
                    "",
                    DerBitString.toBitString(
                        noUnusedBits, Hex.toByteArray(octets)))); // end forEach(input -> ...)
  } // end method */

  /** Test method for {@link DerBitString#toString(String, String, int, boolean)}. */
  @Test
  void test_toString__String_String_int_boolean() {
    // Assertions:
    // ... a. toString(String, String, int, int, boolean)-method from superclass
    //        works as expected.

    // Test strategy:
    // --- a. smoke test for addComment=true   AND  0 == noUnusedBits
    // --- b. smoke test for addComment=true   AND  0 != noUnusedBits
    // --- c. smoke test for addComment=false  AND  0 == noUnusedBits
    // --- d. smoke test for addComment=false  AND  0 != noUnusedBits

    final String delimiter = " ";
    final String delo = "|  ";
    final int noIndentation = 1;
    final byte[] valueField = new DerInteger(BigInteger.TEN).getEncoded();

    // --- a. smoke test for addComment=true   AND  0 == noUnusedBits
    {
      final boolean addComment = true;
      final int noUnusedBits = 0;
      final DerBitString dut = new DerBitString(noUnusedBits, valueField);

      final String present = dut.toString(delimiter, delo, noIndentation, addComment);

      assertEquals(
          String.format(
              "|  03 04 0002010a # BITSTRING: 0 unused bits: '00000010 00000001 00001010'%n"
                  + "|     ##########%n"
                  + "|     # 02 01 0a # INTEGER := 10%n"
                  + "|     ##########"),
          present);
    } // end --- a.

    // --- b. smoke test for addComment=true   AND  0 != noUnusedBits
    {
      final boolean addComment = true;
      final int noUnusedBits = 2;
      final DerBitString dut = new DerBitString(noUnusedBits, valueField);

      final String present = dut.toString(delimiter, delo, noIndentation, addComment);

      assertEquals(
          "|  03 04 0202010a # BITSTRING: 2 unused bits: '00000010 00000001 000010'", present);
    } // end --- b.

    // --- c. smoke test for addComment=false  AND  0 == noUnusedBits
    {
      final boolean addComment = false;
      final int noUnusedBits = 0;
      final DerBitString dut = new DerBitString(noUnusedBits, valueField);

      final String present = dut.toString(delimiter, delo, noIndentation, addComment);

      assertEquals("|  03 04 0002010a", present);
    } // end --- c.

    // --- d. smoke test for addComment=false  AND  0 != noUnusedBits
    {
      final boolean addComment = false;
      final int noUnusedBits = 1;
      final DerBitString dut = new DerBitString(noUnusedBits, valueField);

      final String present = dut.toString(delimiter, delo, noIndentation, addComment);

      assertEquals("|  03 04 0102010a", present);
    } // end --- d.
  } // end method */

  /** Test method for {@link DerBitString#toStringTree()}. */
  @Test
  void test_toStringTree() {
    // Test strategy:
    // --- a. smoke test
    // --- b. some manual tests

    // --- a. smoke test
    {
      final var expected = "03 02 0340 # BITSTRING: 3 unused bits: '01000'";
      final var dut = new DerBitString(3, Hex.toByteArray("40"));

      final var present = dut.toStringTree();

      assertEquals(expected, present);
    } // end --- a.

    // --- b. some manual tests
    Map.ofEntries(
            // empty bit-string
            Map.entry("03 01 00", "03 01 00 # BITSTRING: 0 unused bits: ''"),
            // non-empty bit-string, valid TLV-object, but unused bits
            Map.entry(
                "03 04 01820143",
                "03 04 01820143 # BITSTRING: 1 unused bit: '10000010 00000001 0100001'"),
            // non-empty bit-string, valid TLV-object
            Map.entry(
                "03 04 00820143",
                String.join(
                    LINE_SEPARATOR,
                    "03 04 00820143 # BITSTRING: 0 unused bits: '10000010 00000001 01000011'",
                    "   ##########",
                    "   # 82 01 43",
                    "   ##########")))
        .forEach(
            (input, expected) ->
                assertEquals(
                    expected,
                    BerTlv.getInstance(input)
                        .toStringTree())); // end forEach((input, output) -> ...)
  } // end method */
} // end class
