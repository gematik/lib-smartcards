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

import static de.gematik.smartcards.utils.AfiUtils.KIBI;
import static de.gematik.smartcards.utils.AfiUtils.MEBI;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box tests on {@link DerUtf8String}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "ES_COMPARING_STRINGS_WITH_EQ", i.e.
//         Comparison of String objects using == or !=
//         That finding is correct, but the code intentionally uses "assertSame(...)".
//         Thus, this bug is suppressed.
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
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerUtf8String {

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

  /** Test method for {@link DerUtf8String#checkEncoding(byte[])}. */
  @Test
  void test_checkEncoding__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. all valid 1 byte encodings
    // --- c. all valid 2 byte encodings
    // --- d. all valid 3 byte encodings
    // --- e. all valid 4 byte encodings
    // --- f. invalid 1 byte encodings
    // --- g. overlong 2 byte encodings
    // --- h. overlong 3 byte encodings
    // --- i. overlong 4 byte encodings
    // --- j. invalid range [0xd800, 0xdfff]
    // --- k. out of range, i.e. > 0x10ffff
    // --- l. codePoint with 2 byte, invalid follow byte
    // --- m. codePoint with 3 byte, invalid follow bytes
    // --- n. codePoint with 4 byte, invalid follow bytes
    // --- o. codePoint with 5 byte (all are invalid according to RFC 3729)
    // --- p. codePoint with 6 byte (all are invalid according to RFC 3729)
    // --- q. invalid: manually chosen corner cases

    // --- a. smoke test
    {
      assertTrue(DerUtf8String.checkEncoding(Hex.toByteArray("41 30")));
      assertFalse(DerUtf8String.checkEncoding(Hex.toByteArray("61 80")));
    } // end --- a.

    // --- b. all valid 1 byte encodings
    IntStream.rangeClosed(0x00, 0x7f)
        .forEach(
            b1 -> {
              final byte[] octets = new byte[] {DerUtf8String.TAG, 0x01, (byte) b1};

              assertTrue(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- b.

    // --- c. all valid 2 byte encodings
    IntStream.rangeClosed(0x80, 0x7ff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xc0 | (codePoint >> 6));
              final byte b2 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 0x02, b1, b2};

              assertTrue(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- c.

    // --- d. all valid 3 byte encodings
    IntStream.rangeClosed(0x800, 0xffff)
        .filter(codePoint -> (0xd800 > codePoint) || (codePoint > 0xdfff))
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xe0 | (codePoint >> 12));
              final byte b2 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b3 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 0x03, b1, b2, b3};

              assertTrue(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- d.

    // --- e. all valid 4 byte encodings
    IntStream.rangeClosed(0x10000, 0x10ffff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xf0 | (codePoint >> 18));
              final byte b2 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b4 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 0x04, b1, b2, b3, b4};

              assertTrue(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- e.

    // --- f. invalid 1 byte encodings
    IntStream.rangeClosed(0x80, 0xff)
        .forEach(
            b1 -> {
              // Note: Intentionally here after the invalid codePoint no octet follow,
              //       because invalid follow byte are handled elsewhere.
              final byte[] octets = new byte[] {DerUtf8String.TAG, 2, 0x66, (byte) b1};

              assertFalse(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- f.

    // --- g. overlong 2 byte encodings
    IntStream.rangeClosed(0x00, 0x7f)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xc0 | (codePoint >> 6));
              final byte b2 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 4, 0x67, b1, b2, 0x2};

              assertFalse(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- g.

    // --- h. overlong 3 byte encodings
    IntStream.rangeClosed(0x80, 0x7ff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xe0 | (codePoint >> 12));
              final byte b2 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b3 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 5, 0x68, b1, b2, b3, 0x3};

              assertFalse(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- h.

    // --- i. overlong 4 byte encodings
    IntStream.rangeClosed(0x800, 0xffff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xf0 | (codePoint >> 18));
              final byte b2 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b4 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 6, 0x69, b1, b2, b3, b4, 0x4};

              assertFalse(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- i.

    // --- j. invalid range [0xd800, 0xdfff]
    IntStream.rangeClosed(0xd800, 0xdfff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xe0 | (codePoint >> 12));
              final byte b2 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b3 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 5, 0x6a, b1, b2, b3, 0x5};

              assertFalse(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- j.

    // --- k. out of range, i.e. > 0x10ffff
    RNG.intsClosed(0x110000, 0x1f_ffff, KIBI)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xf0 | (codePoint >> 18));
              final byte b2 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b4 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 6, 0x6b, b1, b2, b3, b4, 0x6};

              assertFalse(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)

    // set with all combination of bits b8, b7 for follow-bytes
    final Set<Integer> msBits =
        Set.of(
            0x00, 0x40, 0x80, // Note: This is a valid value for bits b8, b7 in a follow byte.
            0xc0);

    // --- l. codePoint with 2 byte, invalid follow byte
    msBits.stream()
        .filter(msBits2 -> 0x80 != msBits2)
        .forEach(
            msBits2 -> {
              IntStream.rangeClosed(0x80, 0x7ff)
                  .forEach(
                      codePoint -> {
                        final byte b1 = (byte) (0xc0 | (codePoint >> 6));
                        final byte b2 = (byte) (msBits2 | (codePoint & 0x3f));
                        final byte[] octets = new byte[] {DerUtf8String.TAG, 4, 0x6c, b1, b2, 0x7};

                        assertFalse(DerUtf8String.checkEncoding(octets));
                      }); // end forEach(codePoint -> ...)
            }); // end forEach(msBits2 -> ...)
    // end --- l.

    // --- m. codePoint with 3 byte, invalid follow bytes
    msBits.forEach(
        msBits2 -> {
          msBits.stream()
              .filter(msBits3 -> (0x80 != msBits2) || (0x80 != msBits3))
              // Note: At least in one follow byte the combination for bits b8, b7 is invalid
              .forEach(
                  msBits3 -> {
                    IntStream.rangeClosed(0x800, 0xffff)
                        .filter(codePoint -> (0xd800 > codePoint) || (codePoint > 0xdfff))
                        .forEach(
                            codePoint -> {
                              final byte b1 = (byte) (0xe0 | (codePoint >> 12));
                              final byte b2 = (byte) (msBits2 | ((codePoint >> 6) & 0x3f));
                              final byte b3 = (byte) (msBits3 | (codePoint & 0x3f));
                              final byte[] octets =
                                  new byte[] {DerUtf8String.TAG, 5, 0x6d, b1, b2, b3, 0x8};

                              assertFalse(DerUtf8String.checkEncoding(octets));
                            }); // end forEach(codePoint -> ...)
                  }); // end forEach(msBits3 -> ...)
        }); // end forEach(msBits2 -> ...)
    // end --- m.

    // --- n. codePoint with 4 byte, invalid follow bytes
    msBits.forEach(
        msBits2 -> {
          msBits.forEach(
              msBits3 -> {
                msBits.stream()
                    .filter(msBits4 -> (0x80 != msBits2) || (0x80 != msBits3) || (0x80 != msBits4))
                    // Note: At least in one follow byte the combination for bits b8, b7 is invalid
                    .forEach(
                        msBits4 -> {
                          IntStream.rangeClosed(0x10000, 0x10ffff)
                              .forEach(
                                  codePoint -> {
                                    final byte b1 = (byte) (0xf0 | (codePoint >> 18));
                                    final byte b2 = (byte) (msBits2 | ((codePoint >> 12) & 0x3f));
                                    final byte b3 = (byte) (msBits3 | ((codePoint >> 6) & 0x3f));
                                    final byte b4 = (byte) (msBits4 | (codePoint & 0x3f));
                                    final byte[] octets =
                                        new byte[] {
                                          DerUtf8String.TAG, 6, 0x6e, b1, b2, b3, b4, 0x9
                                        };

                                    assertFalse(DerUtf8String.checkEncoding(octets));
                                  }); // end forEach(codePoint -> ...)
                        }); // end forEach(msBits4 -> ...)
              }); // end forEach(msBits3 -> ...)
        }); // end forEach(msBits2 -> ...)
    // end --- n.

    // --- o. codePoint with 5 byte (all are invalid according to RFC 3729)
    // Note: There are 2 + 4 * 6 = 26 bit available
    RNG.intsClosed(0x00, 0x3ff_ffff, MEBI)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xf8 | (codePoint >> 24));
              final byte b2 = (byte) (0x80 | ((codePoint >> 18) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b4 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b5 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 5, b1, b2, b3, b4, b5};

              assertFalse(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- o.

    // --- p. codePoint with 6 byte (all are invalid according to RFC 3729)
    // Note: There are 1 + 5 * 6 = 31 bit available
    RNG.intsClosed(0x00, Integer.MAX_VALUE, MEBI)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xfc | (codePoint >> 30));
              final byte b2 = (byte) (0x80 | ((codePoint >> 24) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 18) & 0x3f));
              final byte b4 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b5 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b6 = (byte) (0x80 | (codePoint & 0x3f));
              final byte[] octets = new byte[] {DerUtf8String.TAG, 6, b1, b2, b3, b4, b5, b6};

              assertFalse(DerUtf8String.checkEncoding(octets));
            }); // end forEach(codePoint -> ...)
    // end --- p.

    // --- q. invalid: manually chosen corner cases
    Stream.of(
            "7a c3" // multibyte codePoint ends early
            )
        .forEach(
            input ->
                assertFalse(
                    DerUtf8String.checkEncoding(
                        Hex.toByteArray(input)))); // end forEach(input -> ...)
    // end --- q.
  } // end method */

  /** Test method for {@link DerUtf8String#DerUtf8String(String)}. */
  @Test
  void test_DerUtf8String__String() {
    //  Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with empty  value
    // --- b. smoke test with manual values

    // --- a. smoke test with empty  value
    {
      final DerUtf8String dut = new DerUtf8String("");
      assertEquals("0c00", dut.toString());
      assertEquals(0, dut.getLengthOfValueField());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    }

    // --- b. smoke test with manual values
    Set.of("Alfred")
        .forEach(
            input -> {
              final byte[] value = input.getBytes(StandardCharsets.UTF_8);
              final DerUtf8String dut = new DerUtf8String(input);
              assertEquals(DerUtf8String.TAG, dut.getTag());
              assertEquals(value.length, dut.getLengthOfValueField());
              assertArrayEquals(value, dut.getValueField());
            }); // end forEach(input -> ...)
  } // end method */

  /** Test method for {@link DerUtf8String#DerUtf8String(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerUtf8String__ByteBuffer() {
    //  Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with manual chosen input
    // --- b. ERROR: ArithmeticException
    // --- c. ERROR: BufferUnderflowException

    // --- a. smoke test with manual chosen input
    for (final var value : Set.of("Mustermann")) {
      final byte[] valueField = value.getBytes(StandardCharsets.UTF_8);
      final String octets = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(octets));

      final var dut = new DerUtf8String(buffer);

      assertNull(dut.insDecoded);
      assertEquals(value, dut.getDecoded());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (value...)
    // end --- a.

    // --- b: ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerUtf8String(buffer));

      assertNull(e.getCause());
    } // end --- b.

    // --- c. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerUtf8String(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerUtf8String(buffer));
    } // end For (input...)
    // end --- c.
  } // end method */

  /** Test method for {@link DerUtf8String#DerUtf8String(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerUtf8String__InputStream() {
    //  Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with manual chosen input
    // --- b. ERROR: ArithmeticException
    // --- c. ERROR: IOException

    try {
      // --- a. smoke test with manual chosen input
      for (final var value : Set.of("Mustermann")) {
        final byte[] valueField = value.getBytes(StandardCharsets.UTF_8);
        final String octets =
            BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(octets));

        final var dut = new DerUtf8String(inputStream);

        assertNull(dut.insDecoded);
        assertEquals(value, dut.getDecoded());
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
      } // end For (value...)
      // end --- a.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- b: ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> new DerUtf8String(inputStream));

      assertNull(thrown.getCause());
    } // end --- b.

    // --- c. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerUtf8String(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- c.
  } // end method */

  /** Test method for {@link DerUtf8String#getComment()}. */
  @Test
  void test_getComment() {
    // Note: This simple method does not need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    // --- a. smoke test
    for (final var value : Set.of("FooBar", "ÄÖÜäöüßèê")) {
      final var expected = String.format(" # UTF8String := \"%s\"", value);

      final var actual = new DerUtf8String(value).getComment(); // NOPMD new in loop

      assertEquals(expected, actual);
    } // end For (value...)
  } // end method */

  /** Test method for {@link DerUtf8String#getDecoded()}. */
  @Test
  void test_getDecoded() {
    //  Assertions:
    // ... a. Constructor DerUtf8String(String) works as expected
    // ... b. Constructor DerUtf8String(InputStream) works as expected

    // Test strategy:
    // --- a. decode after DerUtf8String(String)-constructor
    // --- b. decode after DerUtf8String(InputStream)-constructor
    // --- c. all valid code points
    // --- d. invalid encoding

    // --- a. decode after DerUtf8String(String)-constructor
    {
      final String input = "A 9";
      final DerUtf8String dut = new DerUtf8String(input);
      assertNotNull(dut.insDecoded);
      assertEquals("0c 03 412039", dut.toString(" "));
      assertSame(input, dut.getDecoded()); // Spotbugs: ES_COMPARING_STRINGS_WITH_EQ
      assertTrue(dut.insFindings.isEmpty());
    } // end --- a.

    // --- b. decode after DerUtf8String(InputStream)-constructor
    {
      final DerUtf8String dut =
          (DerUtf8String)
              BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray("0c 03 422031")));
      assertNull(dut.insDecoded);
      assertEquals("B 1", dut.getDecoded());
      assertTrue(dut.insFindings.isEmpty());
    } // end --- b.

    // --- c. all valid code points
    // c.1 valid one byte code point encodings
    IntStream.rangeClosed(0x00, 0x7f)
        .forEach(
            b1 -> {
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {DerUtf8String.TAG, 3, (byte) c1, (byte) b1, (byte) c3}));
              final List<Integer> expected = List.of(c1, b1, c3);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertTrue(dut.isValid());
              assertTrue(dut.insFindings.isEmpty());
            }); // end forEach(codePoint -> ...)

    // c.2 valid two byte code point encodings
    IntStream.rangeClosed(0x80, 0x7ff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xc0 | (codePoint >> 6));
              final byte b2 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {DerUtf8String.TAG, 4, (byte) c1, b1, b2, (byte) c3}));
              final List<Integer> expected = List.of(c1, codePoint, c3);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertTrue(dut.isValid());
              assertTrue(dut.insFindings.isEmpty());
            }); // end forEach(codePoint -> ...)

    // c.3 valid three byte code point encodings
    IntStream.rangeClosed(0x800, 0xffff)
        .filter(codePoint -> (0xd800 > codePoint) || (codePoint > 0xdfff))
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xe0 | (codePoint >> 12));
              final byte b2 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b3 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {DerUtf8String.TAG, 5, (byte) c1, b1, b2, b3, (byte) c3}));
              final List<Integer> expected = List.of(c1, codePoint, c3);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertTrue(dut.isValid());
              assertTrue(dut.insFindings.isEmpty());
            }); // end forEach(codePoint -> ...)

    // c.4 valid four byte code point encodings
    IntStream.rangeClosed(0x10000, 0x10ffff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xf0 | (codePoint >> 18));
              final byte b2 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b4 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {
                                DerUtf8String.TAG, 6, (byte) c1, b1, b2, b3, b4, (byte) c3
                              }));
              final List<Integer> expected = List.of(c1, codePoint, c3);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertTrue(dut.isValid());
              assertTrue(dut.insFindings.isEmpty());
            }); // end forEach(codePoint -> ...)

    // Note: Here we define the replacement character, see
    //       https://en.wikipedia.org/wiki/Specials_(Unicode_block)#Replacement_character
    final int rci = 0xfffd;
    final String rcs = new String(new int[] {rci}, 0, 1);
    final List<String> expectedFindings = List.of("invalid encoding");

    // --- d. invalid encoding
    // d.1 invalid one byte code point encodings
    // Note: Intentionally no byte follows the invalid encoding.
    IntStream.rangeClosed(0x80, 0xff)
        .forEach(
            b1 -> {
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {DerUtf8String.TAG, 2, (byte) c1, (byte) b1}));
              final List<Integer> expected = List.of(c1, rci);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertFalse(dut.isValid());
              assertEquals(expectedFindings, dut.insFindings);
              assertTrue(dut.getDecoded().contains(rcs));
            }); // end forEach(codePoint -> ...)

    // d.2. overlong 2 byte encodings
    IntStream.rangeClosed(0x00, 0x7f)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xc0 | (codePoint >> 6));
              final byte b2 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {DerUtf8String.TAG, 4, (byte) c1, b1, b2, (byte) c3}));
              final List<Integer> expected = List.of(c1, rci, rci, c3);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertFalse(dut.isValid());
              assertEquals(expectedFindings, dut.insFindings);
              assertTrue(dut.getDecoded().contains(rcs));
            }); // end forEach(codePoint -> ...)

    // d.3. overlong 3 byte encodings
    IntStream.rangeClosed(0x80, 0x7ff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xe0 | (codePoint >> 12));
              final byte b2 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b3 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {DerUtf8String.TAG, 5, (byte) c1, b1, b2, b3, (byte) c3}));
              final List<Integer> expected = List.of(c1, rci, rci, rci, c3);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertFalse(dut.isValid());
              assertEquals(expectedFindings, dut.insFindings);
              assertTrue(dut.getDecoded().contains(rcs));
            }); // end forEach(codePoint -> ...)

    // d.4 overlong 4 byte encodings
    IntStream.rangeClosed(0x800, 0xffff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xf0 | (codePoint >> 18));
              final byte b2 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b4 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {
                                DerUtf8String.TAG, 6, (byte) c1, b1, b2, b3, b4, (byte) c3
                              }));
              final List<Integer> expected = List.of(c1, rci, rci, rci, rci, c3);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertFalse(dut.isValid());
              assertEquals(expectedFindings, dut.insFindings);
              assertTrue(dut.getDecoded().contains(rcs));
            }); // end forEach(codePoint -> ...)

    // d.5 invalid range [0xd800, 0xdfff]
    IntStream.rangeClosed(0xd800, 0xdfff)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xe0 | (codePoint >> 12));
              final byte b2 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b3 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {DerUtf8String.TAG, 5, (byte) c1, b1, b2, b3, (byte) c3}));
              final List<Integer> expected = List.of(c1, rci, c3);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertFalse(dut.isValid());
              assertEquals(expectedFindings, dut.insFindings);
              assertTrue(dut.getDecoded().contains(rcs));
            }); // end forEach(codePoint -> ...)

    // d.6 out of range, i.e. > 0x10ffff
    RNG.intsClosed(0x110000, 0x1f_ffff, KIBI)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xf0 | (codePoint >> 18));
              final byte b2 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b4 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {
                                DerUtf8String.TAG, 6, (byte) c1, b1, b2, b3, b4, (byte) c3
                              }));
              final List<Integer> expected = List.of(c1, rci, rci, rci, rci, c3);
              assertNull(dut.insDecoded);

              final List<Integer> present = dut.getDecoded().codePoints().boxed().toList();

              assertEquals(expected, present);
              assertNotNull(dut.insDecoded);
              assertFalse(dut.isValid());
              assertEquals(expectedFindings, dut.insFindings);
              assertTrue(dut.getDecoded().contains(rcs));
            }); // end forEach(codePoint -> ...)

    // set with all combination of bits b8, b7 for follow-bytes
    final Set<Integer> msBits =
        Set.of(
            0x00, 0x40, 0x80, // Note: This is a valid value for bits b8, b7 in a follow byte.
            0xc0);

    // d.7 codePoint with 2 byte, invalid follow byte
    msBits.stream()
        .filter(msBits2 -> 0x80 != msBits2)
        .forEach(
            msBits2 -> {
              IntStream.rangeClosed(0x80, 0x7ff)
                  .forEach(
                      codePoint -> {
                        final byte b1 = (byte) (0xc0 | (codePoint >> 6));
                        final byte b2 = (byte) (msBits2 | (codePoint & 0x3f));
                        final int rd = RNG.nextInt();
                        final int c1 = rd & 0x7f;
                        final int c3 = (rd >> 8) & 0x7f;
                        final DerUtf8String dut =
                            (DerUtf8String)
                                BerTlv.getInstance(
                                    new ByteArrayInputStream(
                                        new byte[] {
                                          DerUtf8String.TAG, 4, (byte) c1, b1, b2, (byte) c3
                                        }));
                        assertNull(dut.insDecoded);

                        final List<Integer> present =
                            dut.getDecoded().codePoints().boxed().toList();

                        assertTrue(present.contains(rci));
                        assertNotNull(dut.insDecoded);
                        assertFalse(dut.isValid());
                        assertEquals(expectedFindings, dut.insFindings);
                        assertTrue(dut.getDecoded().contains(rcs));
                      }); // end forEach(codePoint -> ...)
            }); // end forEach(msBits2 -> ...)

    // d.8 codePoint with 3 byte, invalid follow bytes
    msBits.forEach(
        msBits2 -> {
          msBits.stream()
              .filter(msBits3 -> (0x80 != msBits2) || (0x80 != msBits3))
              // Note: At least in one follow byte the combination for bits b8, b7 is invalid
              .forEach(
                  msBits3 -> {
                    IntStream.rangeClosed(0x800, 0xffff)
                        .filter(codePoint -> (0xd800 > codePoint) || (codePoint > 0xdfff))
                        .forEach(
                            codePoint -> {
                              final byte b1 = (byte) (0xe0 | (codePoint >> 12));
                              final byte b2 = (byte) (msBits2 | ((codePoint >> 6) & 0x3f));
                              final byte b3 = (byte) (msBits3 | (codePoint & 0x3f));
                              final int rd = RNG.nextInt();
                              final int c1 = rd & 0x7f;
                              final int c3 = (rd >> 8) & 0x7f;
                              final DerUtf8String dut =
                                  (DerUtf8String)
                                      BerTlv.getInstance(
                                          new ByteArrayInputStream(
                                              new byte[] {
                                                DerUtf8String.TAG,
                                                5,
                                                (byte) c1,
                                                b1,
                                                b2,
                                                b3,
                                                (byte) c3
                                              }));
                              assertNull(dut.insDecoded);

                              final String decoded = dut.getDecoded();

                              assertTrue(decoded.codePoints().boxed().toList().contains(rci));
                              assertNotNull(dut.insDecoded);
                              assertFalse(dut.isValid());
                              assertEquals(expectedFindings, dut.insFindings);
                              assertTrue(dut.getDecoded().contains(rcs));
                            }); // end forEach(codePoint -> ...)
                  }); // end forEach(msBits3 -> ...)
        }); // end forEach(msBits2 -> ...)

    // d.9 codePoint with 4 byte, invalid follow bytes
    msBits.forEach(
        msBits2 -> {
          msBits.forEach(
              msBits3 -> {
                msBits.stream()
                    .filter(msBits4 -> (0x80 != msBits2) || (0x80 != msBits3) || (0x80 != msBits4))
                    // Note: At least in one follow byte the combination for bits b8, b7 is invalid
                    .forEach(
                        msBits4 -> {
                          RNG.intsClosed(0x10000, 0x10ffff, KIBI)
                              .forEach(
                                  codePoint -> {
                                    final byte b1 = (byte) (0xf0 | (codePoint >> 18));
                                    final byte b2 = (byte) (msBits2 | ((codePoint >> 12) & 0x3f));
                                    final byte b3 = (byte) (msBits3 | ((codePoint >> 6) & 0x3f));
                                    final byte b4 = (byte) (msBits4 | (codePoint & 0x3f));
                                    final int rd = RNG.nextInt();
                                    final int c1 = rd & 0x7f;
                                    final int c3 = (rd >> 8) & 0x7f;
                                    final DerUtf8String dut =
                                        (DerUtf8String)
                                            BerTlv.getInstance(
                                                new ByteArrayInputStream(
                                                    new byte[] {
                                                      DerUtf8String.TAG,
                                                      6,
                                                      (byte) c1,
                                                      b1,
                                                      b2,
                                                      b3,
                                                      b4,
                                                      (byte) c3
                                                    }));
                                    assertNull(dut.insDecoded);

                                    final String decoded = dut.getDecoded();

                                    assertTrue(decoded.codePoints().boxed().toList().contains(rci));
                                    assertNotNull(dut.insDecoded);
                                    assertFalse(dut.isValid());
                                    assertEquals(expectedFindings, dut.insFindings);
                                    assertTrue(dut.getDecoded().contains(rcs));
                                  }); // end forEach(codePoint -> ...)
                        }); // end forEach(msBits4 -> ...)
              }); // end forEach(msBits3 -> ...)
        }); // end forEach(msBits2 -> ...)

    // d.10 codePoint with 5 byte (all are invalid according to RFC 3729)
    // Note: There are 2 + 4 * 6 = 26 bit available
    RNG.intsClosed(0x00, 0x3ff_ffff, KIBI)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xf8 | (codePoint >> 24));
              final byte b2 = (byte) (0x80 | ((codePoint >> 18) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b4 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b5 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {
                                DerUtf8String.TAG, 7, (byte) c1, b1, b2, b3, b4, b5, (byte) c3
                              }));
              assertNull(dut.insDecoded);

              final String decoded = dut.getDecoded();

              assertTrue(decoded.codePoints().boxed().toList().contains(rci));
              assertNotNull(dut.insDecoded);
              assertFalse(dut.isValid());
              assertEquals(expectedFindings, dut.insFindings);
              assertTrue(dut.getDecoded().contains(rcs));
            }); // end forEach(codePoint -> ...)

    // d.11 codePoint with 6 byte (all are invalid according to RFC 3729)
    // Note: There are 1 + 5 * 6 = 31 bit available
    RNG.intsClosed(0x00, Integer.MAX_VALUE, KIBI)
        .forEach(
            codePoint -> {
              final byte b1 = (byte) (0xfc | (codePoint >> 30));
              final byte b2 = (byte) (0x80 | ((codePoint >> 24) & 0x3f));
              final byte b3 = (byte) (0x80 | ((codePoint >> 18) & 0x3f));
              final byte b4 = (byte) (0x80 | ((codePoint >> 12) & 0x3f));
              final byte b5 = (byte) (0x80 | ((codePoint >> 6) & 0x3f));
              final byte b6 = (byte) (0x80 | (codePoint & 0x3f));
              final int rd = RNG.nextInt();
              final int c1 = rd & 0x7f;
              final int c3 = (rd >> 8) & 0x7f;
              final DerUtf8String dut =
                  (DerUtf8String)
                      BerTlv.getInstance(
                          new ByteArrayInputStream(
                              new byte[] {
                                DerUtf8String.TAG, 8, (byte) c1, b1, b2, b3, b4, b5, b6, (byte) c3
                              }));
              assertNull(dut.insDecoded);

              final String decoded = dut.getDecoded();

              assertTrue(decoded.codePoints().boxed().toList().contains(rci));
              assertNotNull(dut.insDecoded);
              assertFalse(dut.isValid());
              assertEquals(expectedFindings, dut.insFindings);
              assertTrue(dut.getDecoded().contains(rcs));
            }); // end forEach(codePoint -> ...)
  } // end method */
} // end class
