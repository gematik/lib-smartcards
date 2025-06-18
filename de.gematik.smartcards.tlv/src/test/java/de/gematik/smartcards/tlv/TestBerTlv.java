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

import static de.gematik.smartcards.tlv.ClassOfTag.APPLICATION;
import static de.gematik.smartcards.tlv.ClassOfTag.CONTEXT_SPECIFIC;
import static de.gematik.smartcards.tlv.ClassOfTag.PRIVATE;
import static de.gematik.smartcards.tlv.ClassOfTag.UNIVERSAL;
import static de.gematik.smartcards.utils.AfiUtils.EMPTY_OS;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests on {@link BerTlv}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "DLS_DEAD_LOCAL_STORE", i.e.
//         Dead store to local variable
//         These findings corresponds to code with pre-increment (++x) and pre-decrement (--x)
//         and seems to be a false positive.
// Note 2: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.
//         Non-null field is not initialized
//         This finding is for a class attribute which is initialized by JUnit.
// Note 3: Spotbugs claims "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", i.e.
//         Redundant nullcheck of value known to be non-null
//         This happens at the end of a try-with-resources structure.
//         This seems to be a false positive, because the code does not contain a null-check.
//         Probably the container adds something here and a null-check appears in the byte-code.
// Note 4: Spotbugs claims: "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT".
//         Spotugs short message: This code calls a method and ignores the
//             return value.
//         Rational: Return values cannot be observed if the method produces an
//             exception.
// Note 5: Spotbugs claims "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"
//         Spotbugs message: This instance method writes to a static field.
//         Rational: This is intentional to disable manual tests.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "DLS_DEAD_LOCAL_STORE", // see note 1
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 2
  "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", // see note 3
  "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", // see note 4
  "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", // see note 5
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.CyclomaticComplexity",
  "PMD.ExcessiveImports",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
@TestMethodOrder(MethodOrderer.MethodName.class)
final class TestBerTlv {

  /** Set with valid tags of various lengths. */
  /* package */ static final Set<byte[]> VALID_TAGS = new HashSet<>(); // */

  /** Temporary Directory. */
  @TempDir
  /* package */ static Path claTempDir; // NOPMD, NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR */

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestBerTlv.class); // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /**
   * Flag indicating manual started test methods.
   *
   * <p>The idea behind is as follows:
   *
   * <ol>
   *   <li>If a certain test method is started manually, then this method runs without changing the
   *       code in this class.
   *   <li>If all tests in this class are started, e.g., during a test-suite, then the first
   *       test-method disables all other tests in this class.
   * </ol>
   */
  private static boolean claManualTest = true; // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Assertions:
    // ... a. BerTlv.createTag(...) works as expected

    // --- fill the set of valid tags with appropriate values
    // loop over all class of tag
    Stream.of(ClassOfTag.values())
        .forEach(
            clazz -> {
              // loop over all values {primitive, constructed}
              Stream.of(true, false)
                  .forEach(
                      isConstructed -> {
                        long number = 0;
                        // loop over all numbers for one byte tags
                        for (; number < 0x1f; number++) {
                          VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));
                        } // end For (number...)

                        // loop over all numbers for two byte tags
                        for (; number <= 0x7f; number++) {
                          VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));
                        } // end For (number...)

                        // relevant numbers for 3-byte tags
                        // the lowest number for 3-byte tag
                        number = 0x80;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // the highest number for 3-byte tag
                        number <<= 7;
                        number--;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // relevant numbers for 4-byte tags
                        // the lowest number for 4-byte tag
                        number++;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // the highest number for 4-byte tag
                        number <<= 7;
                        number--;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // relevant numbers for 5-byte tags
                        // the lowest number for 5-byte tag
                        number++;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // the highest number for 5-byte tags
                        number <<= 7;
                        number--;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // relevant numbers for 6-byte tags
                        // the lowest number for 6-byte tag
                        number++;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // the highest number for 6-byte tags
                        number <<= 7;
                        number--;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // relevant numbers for 7-byte tags
                        // the lowest number for a 7-byte tag
                        number++;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // the highest number for 7-byte tags
                        number <<= 7;
                        number--;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // relevant numbers for 8 byte tags
                        // the lowest number for an 8-byte tag
                        number++;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // the highest number for 8 byte tag
                        number <<= 7;
                        number--;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // relevant numbers for 9-byte tags
                        // the lowest number for a 9-byte tag
                        number++;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // the highest number for a 9-byte tag
                        number <<= 7;
                        number--;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // possible numbers for 10 byte tags
                        // the lowest number for 10 byte tag
                        number++;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));

                        // the highest number for 10 byte tag
                        number = Long.MAX_VALUE;
                        VALID_TAGS.add(BerTlv.createTag(clazz, isConstructed, number));
                      }); // end forEach(isConstructed -> ...)
            }); // end forEach(class -> ...)
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
   * Returns flag indicating if tests run manually.
   *
   * @return {@code TRUE} if tests are started manually, {@code FALSE} otherwise
   */
  private static boolean isManualTest() {
    return claManualTest;
  } // end method */

  /** Disable test methods when called automatically. */
  @Test
  void test_000_DisableWhenAutomatic() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check the value of the flag
    // --- b. set flag to false
    // --- c. check the value of the flag

    // --- a. check the value of the flag
    assertTrue(isManualTest());

    // --- b. disable manual tests
    claManualTest = false; // ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD

    // --- c. check the value of the flag
    assertFalse(isManualTest());
  } // end method */

  /** Test method for {@link BerTlv#BerTlv(byte[], ByteBuffer)}. */
  @Test
  void test_100_BerTlv__byteA_ByteBuffer() {
    // Assertions:
    // ... a. method BerTlv.readTag(ByteBuffer)    works as expected
    // ... b. method BerTlv.checkTag()             works as expected
    // ... c. method BerTlv.readlength(ByteBuffer) works as expected
    // ... c. method BerTlv.getLengthField(long)   works as expected

    // Note: This simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. read valid tags from ByteBuffer
    // --- b. ERROR: read invalid tags from ByteBuffer
    // --- c. indefinite form
    // --- d. ERROR: ArithmeticException

    // --- a. read valid tags from ByteBuffer
    VALID_TAGS.stream()
        .filter(octets -> (octets.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            tagField -> {
              final int len = RNG.nextIntClosed(0, 1024);
              final byte[] lengthField = Hex.toByteArray(BerTlv.getLengthField(len));
              final var buffer = ByteBuffer.wrap(lengthField);

              final var dut = new MyBerTlv(tagField, buffer);

              assertFalse(buffer.hasRemaining());
              assertEquals(len, dut.insLengthOfValueFieldFromStream);
              assertEquals(lengthField.length, dut.insLengthOfLengthFieldFromStream);
              assertEquals(tagField.length, dut.getLengthOfTagField());
              assertEquals(Hex.toHexDigits(tagField), dut.getTagField());
              assertEquals(
                  Hex.toHexDigits(AfiUtils.concatenate(tagField, lengthField)),
                  Hex.toHexDigits(dut.insTagLengthField));
              assertEquals(-1, dut.insLengthOfValueField);
            }); // end forEach(tag -> ...)
    // end --- a.

    final var tagField = Hex.toByteArray("20");

    // --- c. indefinite form
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("-80"));

      final var dut = new MyBerTlv(tagField, buffer);

      assertTrue(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(0, dut.insLengthOfValueFieldFromStream);
      assertFalse(buffer.hasRemaining());
    } // end --- c.

    // --- d. ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new MyBerTlv(tagField, buffer));

      assertNull(e.getCause());
      assertNotEquals(0, buffer.position());
    } // end --- d.
  } // end method */

  /** Test method for {@link BerTlv#BerTlv(long, long)}. */
  @Test
  void test_120_BerTlv__long_long() {
    // Assertions:
    // ... a. method BerTlv.convertTag(long)     works as expected
    // ... b. method BerTlv.checkTag()           works as expected
    // ... c. method BerTlv.getLengthField(long) works as expected

    // Note: This simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. smoke test with manually chosen  valid  input for tag
    // --- c. smoke test with manually chosen invalid input for tag
    // --- d. invalid value for lengthOfValueField
    // --- e. reasonable combination of tag and length values

    // --- a. smoke test
    {
      final var tag = 0xbf43;
      final var len = 15;

      final var dut = new MyBerTlv(tag, len);

      final byte[] tagField = BerTlv.convertTag(tag);
      assertEquals(tagField.length, dut.getLengthOfTagField());
      assertEquals(tag, dut.getTag());
      assertEquals(
          Hex.toHexDigits(tagField) + BerTlv.getLengthField(len),
          Hex.toHexDigits(dut.insTagLengthField));
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(0, dut.insLengthOfValueFieldFromStream);
      assertEquals(len, dut.getLengthOfValueField());
    } // end --- a.

    // --- b. smoke test with manually chosen  valid  input for tag
    Stream.of(0L, 0x20L, 0x40L, 0x60L, 0x80L, 0xa0L, 0xc0L, 0xe0L, 0x1f8173L, 0xffffffffffffff7fL)
        .forEach(
            tag -> {
              final int len = RNG.nextIntClosed(0, 20);

              final var dut = new MyBerTlv(tag, len);

              final byte[] tagField = BerTlv.convertTag(tag);
              assertEquals(tagField.length, dut.getLengthOfTagField());
              assertEquals(tag, dut.getTag());
              assertEquals(
                  Hex.toHexDigits(tagField) + BerTlv.getLengthField(len),
                  Hex.toHexDigits(dut.insTagLengthField));
              assertEquals(1, dut.insLengthOfLengthFieldFromStream);
              assertEquals(0, dut.insLengthOfValueFieldFromStream);
              assertEquals(len, dut.getLengthOfValueField());
            }); // end forEach(entry -> ...)

    // --- c. smoke test with manually chosen invalid input for tag
    Stream.of(0x1f, 0x1f00, 0x1f80, 0x1f1e, 0x1e1f, 0x3f1f00, 0x5f8481)
        .forEach(
            tag -> {
              final Throwable throwed =
                  assertThrows(IllegalArgumentException.class, () -> new MyBerTlv(tag, 0));
              assertNull(throwed.getCause());
            }); // end forEach(tag -> ...)

    // --- d. invalid value for lengthOfValueField
    Stream.of(
            -1L, // supremum of invalid length
            Long.MIN_VALUE // infimum  of invalid length
            )
        .forEach(
            len ->
                assertThrows(
                    IllegalArgumentException.class,
                    () -> new MyBerTlv(0x00, len))); // end forEach(len -> ...)

    // --- e. reasonable combination of tag and length values
    final Set<Long> lengths = new HashSet<>();
    RNG.intsClosed(0, 1024, 16) // reasonable length values
        .forEach(i -> lengths.add((long) i));
    IntStream.range(0, 16) // length values from int-range
        .forEach(i -> lengths.add((long) RNG.nextInt() & Integer.MAX_VALUE)); // NOPMD unnecessary
    IntStream.range(0, 16) // length values from long-range
        .forEach(i -> lengths.add(RNG.nextLong() & Long.MAX_VALUE));

    VALID_TAGS.stream()
        .filter(octets -> (octets.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            tagField -> {
              final long tag = BerTlv.convertTag(tagField);
              final String osTag = Hex.toHexDigits(tagField);

              lengths.forEach(
                  len -> {
                    final var dut = new MyBerTlv(tag, len);
                    assertEquals(
                        osTag + dut.getLengthField(), Hex.toHexDigits(dut.insTagLengthField));
                  }); // end forEach(len -> ...)
            }); // end forEach(tag -> ...)
  } // end method */

  /** Test method for {@link BerTlv#base64(String)}. */
  @Test
  void test_200_base64__String() {
    // Assertions:
    // ... a. underlying getInstance(byte[])-method works as expected

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. some manually chosen input
    // --- c. ERROR: exception for invalid input

    // --- a. smoke test with manually chosen input
    {
      final var input = "gQKquw==";
      final var expected = BerTlv.getInstance("81-02-aabb");

      final var dut = BerTlv.base64(input);

      assertEquals(expected, dut);
    } // end --- a.

    // --- b. some manually chosen input
    Set.of(
            "80 00", // empty primitive
            "a0 00", // empty constructed
            "81 02 abcd", // non-empty primitive
            "a1 03  82 01 42") // non-empty constructed
        .forEach(
            hex -> {
              final String input = BerTlv.BASE64_ENCODER.encodeToString(Hex.toByteArray(hex));
              final var expected = BerTlv.getInstance(hex);

              final var actual = BerTlv.base64(input);

              assertEquals(expected, actual, hex);
            }); // end forEach(input -> ...)

    // --- c. ERROR: exception for invalid input
    Map.ofEntries(
            Map.entry("", "unexpected IOException"), // empty input
            Map.entry("af<>#", "Illegal base64 character 3c") // invalid characters in input
            )
        .forEach(
            (input, message) -> {
              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> BerTlv.base64(input));
              assertEquals(message, throwable.getMessage());
            }); // end forEach(input -> ...)
  } // end method */

  /** Test method for {@link BerTlv#calculateLengthOfLengthField(long)}. */
  @Test
  void test_210_calculateLengthOfLengthField__long() {
    // Test strategy:
    // --- a. manual tests for various lengths

    // --- a. manual tests for various lengths
    long lengthOfValueField;
    // a.1 length-field 1 octet long
    assertEquals(1, BerTlv.calculateLengthOfLengthField(0x00L));
    assertEquals(1, BerTlv.calculateLengthOfLengthField(lengthOfValueField = 0x7f));

    // a.2 length-field 2 octet long
    assertEquals(2, BerTlv.calculateLengthOfLengthField(++lengthOfValueField));
    assertEquals(2, BerTlv.calculateLengthOfLengthField(lengthOfValueField = 0xff));

    // a.3 length-field 3 octet long
    assertEquals(3, BerTlv.calculateLengthOfLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals(3, BerTlv.calculateLengthOfLengthField(--lengthOfValueField));

    // a.4 length-field 4 octet long
    assertEquals(4, BerTlv.calculateLengthOfLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals(4, BerTlv.calculateLengthOfLengthField(--lengthOfValueField));

    // a.5 length-field 5 octet long
    assertEquals(5, BerTlv.calculateLengthOfLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals(5, BerTlv.calculateLengthOfLengthField(--lengthOfValueField));

    // a.6 length-field 6 octet long
    assertEquals(6, BerTlv.calculateLengthOfLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals(6, BerTlv.calculateLengthOfLengthField(--lengthOfValueField));

    // a.7 length-field 7 octet long
    assertEquals(7, BerTlv.calculateLengthOfLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals(7, BerTlv.calculateLengthOfLengthField(--lengthOfValueField));

    // a.8 length-field 8 octet long
    assertEquals(8, BerTlv.calculateLengthOfLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals(8, BerTlv.calculateLengthOfLengthField(--lengthOfValueField));

    // a.9 length-field 9 octet long
    // Spotbugs: DLS_DEAD_LOCAL_STORE for next line of code
    assertEquals(9, BerTlv.calculateLengthOfLengthField(++lengthOfValueField));
    assertEquals(9, BerTlv.calculateLengthOfLengthField(Long.MAX_VALUE));

    Set.of(-1L, Long.MIN_VALUE)
        .forEach(
            lengthValueField ->
                assertEquals(
                    9,
                    BerTlv.calculateLengthOfLengthField(
                        lengthValueField))); // end forEach(lengthOfValueField -> ...)
  } // end method */

  /** Test method for {@link BerTlv#checkTag(byte[])}. */
  @Test
  void test_220_checkTag__byteA() {
    // Test strategy:
    // --- a. loop over valid tags
    // --- b. empty tag-field
    // --- c. illegal argument

    // --- a. loop over valid tags
    VALID_TAGS.stream()
        .filter(octets -> (octets.length <= BerTlv.NO_TAG_FIELD))
        .forEach(BerTlv::checkTag);

    // --- b. empty tag-field
    {
      final Throwable thrown =
          assertThrows(IllegalArgumentException.class, () -> BerTlv.checkTag(new byte[0]));
      assertEquals("empty tag-field", thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- b.

    // --- c. illegal argument
    final String interMessage = "Intermediate byte has MS-bit not set in tag = '";
    final String msBitMessage = "LS-byte has MS-bit set in tag = '";
    // loop over all class of tag
    Stream.of(ClassOfTag.values())
        .forEach(
            clazz -> {
              // loop over all values {primitive, constructed}
              Stream.of(true, false)
                  .forEach(
                      isConstructed -> {
                        // c.1 wrong values in tag
                        Map.ofEntries(
                                Map.entry("1f", "No subsequent octet in tag = '"),
                                Map.entry(
                                    "1f00",
                                    "bits b7..b1 all zero in first subsequent octet of tag = '"),
                                Map.entry(
                                    "1f80",
                                    "bits b7..b1 all zero in first subsequent octet of tag = '"),
                                Map.entry("1f1e", "No need for two byte tag = '"),
                                Map.entry("001f", "Leading octet wrong in tag = '"),
                                Map.entry("1e1f", "Leading octet wrong in tag = '"),
                                Map.entry("1f1f00", interMessage),
                                Map.entry("1f7f00", interMessage),
                                Map.entry("1f208000", interMessage),
                                Map.entry("1f827f00", interMessage),
                                Map.entry("1f21808000", interMessage),
                                Map.entry("1fff7f8000", interMessage),
                                Map.entry("1f84807f00", interMessage),
                                Map.entry("1f8580", msBitMessage),
                                Map.entry("1fffff", msBitMessage),
                                Map.entry("1f868080", msBitMessage),
                                Map.entry("1fffffff", msBitMessage))
                            .forEach(
                                (key, messagePrefix) -> {
                                  final byte[] input = Hex.toByteArray(key);

                                  // adjust bits b8..b6 in leading octet
                                  input[0] +=
                                      (byte) (clazz.getEncoding() + (isConstructed ? 0x20 : 0x00));
                                  final Throwable thrown =
                                      assertThrows(
                                          IllegalArgumentException.class,
                                          () -> BerTlv.checkTag(input));
                                  assertEquals(
                                      messagePrefix + Hex.toHexDigits(input) + "'",
                                      thrown.getMessage());
                                  assertNull(thrown.getCause());
                                }); // end forEach(entry -> ...)

                        // c.2 tag too long for current implementation
                        final byte[] input = Hex.toByteArray("1f8283848586878809");
                        input[0] += (byte) (clazz.getEncoding() + (isConstructed ? 0x20 : 0x00));
                        final Throwable thrown =
                            assertThrows(
                                IllegalArgumentException.class, () -> BerTlv.checkTag(input));
                        assertEquals("tag too long for this implementation", thrown.getMessage());
                        assertNull(thrown.getCause());
                      }); // end forEach(isConstructed -> ...)
            }); // end forEach(class -> ...)
  } // end method */

  /** Test method for {@link BerTlv#convertTag(byte[])}. */
  @Test
  void test_230_convertTag__byteA() {
    // Note: This simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over valid tags
    // --- c. manually chosen values
    // --- d. too many octets

    // --- a. smoke test
    {
      final var input = "df7f";
      final var expected = Long.valueOf(input, 16);

      final var present = BerTlv.convertTag(Hex.toByteArray(input));

      assertEquals(expected, present);
    } // end --- a.

    // --- b. loop over valid tags
    VALID_TAGS.stream()
        .filter(octets -> (octets.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            octets -> {
              final long expected =
                  ((8 == octets.length) ? new BigInteger(octets) : new BigInteger(1, octets))
                      .longValueExact();

              assertEquals(expected, BerTlv.convertTag(octets));
            }); // end forEach(octets -> ...)

    // --- c. manually chosen values
    Map.ofEntries(
            Map.entry(Long.MIN_VALUE, "8000000000000000"), // MIN_VALUE
            Map.entry(0xffffffffffffffffL, "ffffffffffffffff"), // -1
            Map.entry(0x00L, "00"), // zero value
            Map.entry(0x7fL, "7f"), // zero value
            Map.entry(0x80L, "80"), // zero value
            Map.entry(0xffL, "ff"), // zero value
            Map.entry(0x0123456789abcdefL, "0123456789abcdef"), // arbitrary value
            Map.entry(Long.MAX_VALUE, "7fffffffffffffff")) // MAX_VALUE
        .forEach(
            (expected, input) ->
                assertEquals(
                    expected,
                    BerTlv.convertTag(Hex.toByteArray(input)))); // end forEach(entry -> ...)

    // --- d. too many octets
    for (final var input : Set.of("01 0000 0000 0000 0000", "ff ffff ffff ffff ffff")) {
      final var octets = Hex.toByteArray(input);

      // RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
      assertThrows(ArithmeticException.class, () -> BerTlv.convertTag(octets));
    } // end For (input...)
  } // end method */

  /** Test method for {@link BerTlv#convertTag(long)}. */
  @Test
  void test_240_convertTag__long() {
    //  Assertions:
    // ... a. method BerTlv.createTag(...) works as expected.

    // Simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over valid tags
    // --- c. manually chosen values

    // --- a. smoke test
    {
      final var input = 0x9f8307;
      final var expected = Long.toHexString(input);

      final var present = Hex.toHexDigits(BerTlv.convertTag(input));

      assertEquals(expected, present);
    } // end --- a.

    // --- b. loop over valid tags
    VALID_TAGS.stream()
        .filter(octets -> (octets.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            octets -> {
              final long tag =
                  ((8 == octets.length) ? new BigInteger(octets) : new BigInteger(1, octets))
                      .longValueExact();

              assertEquals(Hex.toHexDigits(octets), Hex.toHexDigits(BerTlv.convertTag(tag)));
            }); // end forEach(octets -> ...)

    // --- c. manually chosen values
    Map.ofEntries(
            Map.entry(Long.MIN_VALUE, "8000000000000000"), // MIN_VALUE
            Map.entry(0xffffffffffffffffL, "ffffffffffffffff"), // -1
            Map.entry(0x00L, "00"), // zero value
            Map.entry(0x7fL, "7f"), // zero value
            Map.entry(0x80L, "80"), // zero value
            Map.entry(0xffL, "ff"), // zero value
            Map.entry(0x0123456789abcdefL, "0123456789abcdef"), // arbitrary value
            Map.entry(Long.MAX_VALUE, "7fffffffffffffff")) // MAX_VALUE
        .forEach(
            (key, expected) -> {
              final long input = key;

              assertEquals(expected, Hex.toHexDigits(BerTlv.convertTag(input)));
            }); // end forEach(entry -> ...)
  } // end method */

  /**
   * Test method for various methods.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link BerTlv#createTag(ClassOfTag, boolean, long)}
   *   <li>{@link BerTlv#numberOfTag(byte[])}
   * </ol>
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity", "PMD.NcssCount"})
  @Test
  void test_250_createTag__TagClass_boolean_long() {
    // Test strategy:
    // --- 0. smoke test
    // --- a. loop over all class of tag
    // --- b. loop over all values {primitive, constructed}
    // --- c. loop over all numbers for one byte tag
    // --- d. loop over all numbers for two byte tag
    // --- e. relevant numbers for  3 byte tags
    // --- f. relevant numbers for  4 byte tags
    // --- g. relevant numbers for  5 byte tags
    // --- h. relevant numbers for  6 byte tags
    // --- i. relevant numbers for  7 byte tags
    // --- j. relevant numbers for  8 byte tags
    // --- k. relevant numbers for  9 byte tags
    // --- l. possible numbers for 10 byte tags
    // --- m. relevant values for invalid numbers

    // --- 0. smoke test
    {
      final var expNumber = 64;
      final var expTag = "9f40";

      final var preTag = BerTlv.createTag(CONTEXT_SPECIFIC, false, expNumber);

      assertEquals(expTag, Hex.toHexDigits(preTag));
      assertEquals(expNumber, BerTlv.numberOfTag(preTag));
    } // end --- 0.

    // --- a. loop over all class of tag
    for (final var clazz : ClassOfTag.values()) {
      // --- b. loop over all values {primitive, constructed}
      for (final var isConstructed : AfiUtils.VALUES_BOOLEAN) {
        final var tmp1 = clazz.getEncoding() + (isConstructed ? 0x20 : 0x00);
        final var tmp2 = tmp1 + 0x1f;
        long expNumber = 0;
        // --- c. loop over all numbers for one byte tag
        for (; expNumber < 0x1f; expNumber++) {
          final var expTag = String.format("%02x", tmp1 + expNumber);

          final var preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

          assertEquals(expTag, Hex.toHexDigits(preTag));
          assertEquals(expNumber, BerTlv.numberOfTag(preTag));
        } // end For (number...)

        // --- d. loop over all numbers for two byte tag
        for (; expNumber <= 0x7f; expNumber++) {
          final var expTag = String.format("%02x%02x", tmp2, expNumber);

          final var preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

          assertEquals(expTag, Hex.toHexDigits(preTag));
          assertEquals(expNumber, BerTlv.numberOfTag(preTag));
        } // end For (number...)

        // --- e. relevant numbers for 3 byte tags
        // e.1 lowest number for 3 byte tag
        expNumber = 0x80;
        var expTag = String.format("%02x8100", tmp2);

        var preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // e.2 highest number for 3 byte tag
        expNumber <<= 7;
        expNumber--;
        expTag = String.format("%02xff7f", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // --- f. relevant numbers for 4 byte tags
        // f.1 lowest number for 4 byte tag
        expNumber++;
        expTag = String.format("%02x818000", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // f.2 highest number for 4 byte tag
        expNumber <<= 7;
        expNumber--;
        expTag = String.format("%02xffff7f", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // --- g. relevant numbers for 5 byte tags
        // g.1 lowest number for 5 byte tag
        expNumber++;
        expTag = String.format("%02x81808000", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // g.2 highest number for 5 byte tag
        expNumber <<= 7;
        expNumber--;
        expTag = String.format("%02xffffff7f", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // --- h. relevant numbers for 6 byte tags
        // h.1 lowest number for 6 byte tag
        expNumber++;
        expTag = String.format("%02x8180808000", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // h.2 highest number for 6 byte tag
        expNumber <<= 7;
        expNumber--;
        expTag = String.format("%02xffffffff7f", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // --- i. relevant numbers for 7 byte tags
        // i.1 lowest number for 7 byte tag
        expNumber++;
        expTag = String.format("%02x818080808000", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // i.2 highest number for 7 byte tag
        expNumber <<= 7;
        expNumber--;
        expTag = String.format("%02xffffffffff7f", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // --- j. relevant numbers for 8 byte tags
        // j.1 lowest number for 8 byte tag
        expNumber++;
        expTag = String.format("%02x81808080808000", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // j.2 highest number for 8 byte tag
        expNumber <<= 7;
        expNumber--;
        expTag = String.format("%02xffffffffffff7f", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // --- k. relevant numbers for  9 byte tags
        // k.1 lowest number for 9 byte tag
        expNumber++;
        expTag = String.format("%02x8180808080808000", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // k.2 highest number for 9 byte tag
        expNumber <<= 7;
        expNumber--;
        expTag = String.format("%02xffffffffffffff7f", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // --- l. possible numbers for 10 byte tags
        // l.1 lowest number for 10 byte tag
        expNumber++;
        expTag = String.format("%02x818080808080808000", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // l.2 highest number for 10 byte tag
        expNumber = Long.MAX_VALUE;
        expTag = String.format("%02xffffffffffffffff7f", tmp2);

        preTag = BerTlv.createTag(clazz, isConstructed, expNumber);

        assertEquals(expTag, Hex.toHexDigits(preTag));
        assertEquals(expNumber, BerTlv.numberOfTag(preTag));

        // --- l. relevant values for invalid numbers
        for (final var invalidNumber : Set.of(-1L, Long.MIN_VALUE)) {
          final var thrown =
              assertThrows(
                  IllegalArgumentException.class,
                  () -> BerTlv.createTag(clazz, isConstructed, invalidNumber));
          assertEquals("number = " + invalidNumber + " < 0", thrown.getMessage());
          assertNull(thrown.getCause());
        } // end For (invalidNumber...)
      } // end For (isConstructed...)
    } // end For (clazz...)
  } // end method */

  /** Test method for {@link BerTlv#equals(Object)}. */
  @Test
  void test_260_equals__Object() {
    // Test strategy:
    // --- 0. smoke test
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. difference in tag
    // --- e. different object, but same tag

    // --- 0. smoke test
    {
      final var dut = new MyBerTlv(0x11, 17);
      final var obj = new MyBerTlv(0x12, 17);

      // Note: IDE et.all. propose to simplify the following line of code.
      //       If so, then the method under test (here "equals(Object)")
      //       is NOT executed. Thus, we keep this line.
      final var present = dut.equals(obj);

      assertFalse(present); // NOPMD simplify
    } // end --- 0.
  } // end method */

  /** Test method for {@link BerTlv#getClassOfTag()}. */
  @Test
  void test_270_getClassOfTag() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over valid tags

    // --- a. smoke test
    {
      final var dut = new MyBerTlv(Hex.toByteArray("03"));

      final var present = dut.getClassOfTag();

      assertEquals(UNIVERSAL, present);
    } // end --- a.

    // --- b. loop over valid tags
    VALID_TAGS.stream()
        .filter(tag -> (tag.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            tag -> {
              final var dut = new MyBerTlv(tag);

              switch (tag[0] & 0xc0) {
                case 0x00 -> assertEquals(UNIVERSAL, dut.getClassOfTag());
                case 0x40 -> assertEquals(APPLICATION, dut.getClassOfTag());
                case 0x80 -> assertEquals(CONTEXT_SPECIFIC, dut.getClassOfTag());
                case 0xc0 -> assertEquals(PRIVATE, dut.getClassOfTag());
                default -> fail("unexpected case");
              } // end Switch
            }); // end forEach(octets -> ...)
  } // end method */

  /** Test method for {@link BerTlv#getNumberOfTag()}. */
  @Test
  void test_280_getNumberOfTag() {
    // Assertions:
    // ... a. "numberOfTag(byte[])"-method works as expected

    // Test strategy:
    // --- a. smoke test
    {
      final var expected = 7;
      final var dut = BerTlv.getInstance("47 00");

      final var present = dut.getNumberOfTag();

      assertEquals(expected, present);
    } // end --- a.
  } // end method */

  /** Test method for {@link BerTlv#getBase64()}. */
  @Test
  void test_290_getBase64() {
    // Assertions:
    // ... a. getEncoded()-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    {
      final var input = Hex.toByteArray("84 03 112233");
      final var dut = BerTlv.getInstance(input);
      final var expected = BerTlv.BASE64_ENCODER.encodeToString(input);

      final var actual = dut.getBase64();

      assertEquals(expected, actual);
    } // end --- a.
  } // end method */

  /** Test method for {@link BerTlv#getEncoded()}. */
  @Test
  void test_300_getEncoded() {
    // Assertions:
    // - none -

    // Note: This abstract method is sufficiently tested in subclasses.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with subclass PrimitiveBerTlv
    // --- b. smoke test with subclass ConstructedBerTlv

    for (final var input :
        List.of(
            // --- a. smoke test with subclass PrimitiveBerTlv
            "83-04-01020304",
            // --- b. smoke test with subclass ConstructedBerTlv
            "a1-07-[(81-01-03)(82-02-abcd)]")) {
      final var dut = BerTlv.getInstance(input);
      final var expected = Hex.extractHexDigits(input);

      final var actual = Hex.toHexDigits(dut.getEncoded());

      assertEquals(expected, actual);
    } // end For (input...)
    // end --- a, b.
  } // end method */

  /** Test method for {@link BerTlv#getEncoded(ByteArrayOutputStream)}. */
  @Test
  void test_310_getEncoded__OutputStream() {
    // Assertions:
    // ... a. instance attribute insTagLengthField is properly set

    // Note: Simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. write to empty stream
    // --- c. write to partially filled stream

    // --- define some constants
    final Set<Long> lengths = new HashSet<>();
    RNG.intsClosed(0, 1024, 16) // reasonable length values
        .forEach(i -> lengths.add((long) i));
    IntStream.range(0, 16) // length values from int-range
        .forEach(i -> lengths.add((long) (RNG.nextInt() & Integer.MAX_VALUE)));
    IntStream.range(0, 16) // length values from long-range
        .forEach(i -> lengths.add(RNG.nextLong() & Long.MAX_VALUE));

    final int maxLengthPrefix = 128;
    final int bufferSize = maxLengthPrefix + 8 + 9;
    final ByteArrayOutputStream baos = new ByteArrayOutputStream(bufferSize);

    // --- a. smoke test
    {
      final var expected = Hex.extractHexDigits("81-820105");
      final var dut = new MyBerTlv(0x81, 261);

      dut.getEncoded(baos);

      final var present = Hex.toHexDigits(baos.toByteArray());
      baos.reset();
      assertEquals(expected, present);
    } // end --- a.

    // --- loop over reasonable combinations of tag and length values
    VALID_TAGS.stream() // Note: Don't use parallel here because we have only one stream.
        .filter(octets -> (octets.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            tagField -> {
              final long tag =
                  ((8 == tagField.length) ? new BigInteger(tagField) : new BigInteger(1, tagField))
                      .longValueExact();
              final String osTag = Hex.toHexDigits(tagField);

              lengths.forEach(
                  len -> {
                    final var dut = new MyBerTlv(tag, len);

                    // --- a. write to empty stream
                    dut.getEncoded(baos);
                    assertEquals(osTag + dut.getLengthField(), Hex.toHexDigits(baos.toByteArray()));
                    baos.reset();

                    // --- b. write to partially filled stream
                    final byte[] prefix = RNG.nextBytes(1, 16);
                    baos.writeBytes(prefix);
                    dut.getEncoded(baos);
                    assertEquals(
                        Hex.toHexDigits(prefix) + osTag + dut.getLengthField(),
                        Hex.toHexDigits(baos.toByteArray()));
                    baos.reset();
                  }); // end forEach(len -> ...)
            }); // end forEach(tag -> ...)
  } // end method */

  /** Test method for {@link BerTlv#getInstance(byte[])}. */
  @Test
  void test_320_getInstance__byteA() {
    // Assertions:
    // ... a. underlying getInstance(InputStream)-method works as expected
    // ... b. toString(String)-method works as expected

    // Note 1: Because of assertion_a we can be lazy here and do some smoke tests.
    // Note 2: Tests on randomly generated TLV objects are performed by test method
    //         "test_900_ConstructedBerTlv_getInstance__InputStream()".

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: IllegalArgumentException, not in accordance to ISO/IEC 8825-1:2021
    // --- c. ERROR: ArithmeticException, tag-field too long
    // --- d. ERROR: ArithmeticException, value from length-field exceeds Long#MAX_VALUE

    // --- a. smoke test
    {
      final String expected = "a1 03  87 01 99";

      final var present = BerTlv.getInstance(Hex.toByteArray(expected));

      assertEquals(expected, present.toString(" "));
    } // end --- a.

    // --- b. ERROR: IllegalArgumentException, not in accordance to ISO/IEC 8825-1:2021
    {
      final var input =
          Set.of(
              "c0-80-4711", // primitive with indefinite form
              "a0-80-81-01-23" // constructed with indefinite form without EndOfContent
              );
      for (final var i : input) {
        final var octets = Hex.toByteArray(i);

        assertThrows(IllegalArgumentException.class, () -> BerTlv.getInstance(octets));
      } // end For (i...)
    } // end --- b.

    // --- c. ERROR: ArithmeticException, tag-field too long
    {
      final var input =
          Set.of(
              "9f8283848586878809-01-12", // primitive tag
              "bf8283848586878809-01-12" // constructed tag
              );
      for (final var i : input) {
        final var octets = Hex.toByteArray(i);

        assertThrows(ArithmeticException.class, () -> BerTlv.getInstance(octets));
      } // end For (i...)
    } // end --- c.

    // --- d. ERROR: ArithmeticException, value from length-field exceeds Long#MAX_VALUE
    {
      final var input =
          Set.of(
              "c1-888000000000000000-810123", // primitive tag
              "e1-888000000000000000-(81-02-23)" // constructed tag
              );
      for (final var i : input) {
        final var octets = Hex.toByteArray(i);

        assertThrows(ArithmeticException.class, () -> BerTlv.getInstance(octets));
      } // end For (i...)
    } // end --- d.
  } // end method */

  /** Test method for {@link BerTlv#getInstance(ByteBuffer)}. */
  @SuppressWarnings({"PMD.NcssCount"})
  @Test
  void test_330_getInstance__ByteBuffer() {
    // Assertions:
    // ... a. toString(String)-method works as expected

    // Note 1: Tests on randomly generated TLV objects are performed by test method
    //         "test_900_ConstructedBerTlv_getInstance__InputStream()".

    // Test strategy:
    // --- a. smoke test with one TLV-object
    // --- b. smoke test with two TLV-objects
    // --- c. ERROR: BufferUnderflowException, position == 0
    // --- d. ERROR: BufferUnderflowException, position > 0

    final var capacity = 0x10;
    final var buffer = ByteBuffer.allocate(capacity);

    // --- a. smoke test with one TLV-object
    {
      final var expected = "81 04 12345678";
      final var octets = Hex.toByteArray(expected);
      buffer.clear().put(octets).flip();
      assertEquals(0, buffer.position());
      assertEquals(octets.length, buffer.limit());

      final var dut = BerTlv.getInstance(buffer);

      assertEquals(expected, dut.toString(" "));
      assertEquals(octets.length, buffer.position());
      assertEquals(octets.length, buffer.limit());
    } // end --- a.

    // --- b. smoke test with two TLV-objects
    {
      final var expected1 = "81 04 01234567";
      final var expected2 = "ab 04  ac 02  83 00";
      final var octets1 = Hex.toByteArray(expected1);
      final var octets2 = Hex.toByteArray(expected2);
      buffer.clear().put(octets1).put(octets2).flip();
      assertEquals(0, buffer.position());
      assertEquals(octets1.length + octets2.length, buffer.limit());

      final var dut1 = BerTlv.getInstance(buffer);

      assertEquals(expected1, dut1.toString(" "));
      assertEquals(octets1.length, buffer.position());
      assertEquals(octets1.length + octets2.length, buffer.limit());

      final var dut2 = BerTlv.getInstance(buffer);

      assertEquals(expected2, dut2.toString(" "));
      assertEquals(octets1.length + octets2.length, buffer.position());
      assertEquals(octets1.length + octets2.length, buffer.limit());
    } // end --- b.

    // --- c. ERROR: BufferUnderflowException, position == 0
    {
      final var expected = "ac 06  ae 04  8c 02 1234";
      final var octets = Hex.toByteArray(expected);
      buffer.clear(); // buffer ready for write-operations
      for (int index = 0; index < octets.length; ) {
        buffer.put(octets[index++]); // NOPMD reassigning loop control variable; write-operation
        assertEquals(index, buffer.position());
        assertEquals(capacity, buffer.limit());
        buffer.flip(); // prepare buffer for read-operation
        assertEquals(0, buffer.position());
        assertEquals(index, buffer.limit());

        if (index < octets.length) {
          // ... TLV-object not completely written to the buffer
          assertThrows(BufferUnderflowException.class, () -> BerTlv.getInstance(buffer));

          assertEquals(0, buffer.position());
          assertEquals(index, buffer.limit());
          buffer.position(buffer.limit()).limit(capacity); // prepare the next write-operation
        } else {
          // ... TLV-object completely written to the buffer
          assertEquals(0, buffer.position());
          assertEquals(octets.length, buffer.limit());

          final var present = BerTlv.getInstance(buffer);

          assertEquals(expected, present.toString(" "));
          assertEquals(octets.length, buffer.position());
          assertFalse(buffer.hasRemaining());
        } // end else
      } // end For (index...)
    } // end --- c.

    // --- d. ERROR: BufferUnderflowException, position > 0
    {
      final var expectedInt = RNG.nextInt();
      final var expectedTlv = "ac 06  ae 04  8c 02 1234";
      final var octets = Hex.toByteArray(expectedTlv);
      buffer.clear(); // buffer ready for write-operations
      buffer.putInt(expectedInt);
      final var offset = buffer.position();
      buffer.flip(); // prepare buffer for read-operation
      assertEquals(expectedInt, buffer.getInt());
      assertEquals(offset, buffer.position());
      assertFalse(buffer.hasRemaining());
      buffer.limit(capacity); // prepare buffer for write-operation
      for (int index = 0; index < octets.length; ) {
        buffer.put(octets[index++]); // NOPMD reassigning loop control variable; write-operation
        assertEquals(offset + index, buffer.position());
        assertEquals(capacity, buffer.limit());
        buffer.flip().position(offset); // prepare buffer for read-operation
        assertEquals(offset, buffer.position());
        assertEquals(offset + index, buffer.limit());

        if (index < octets.length) {
          // ... TLV-object not completely written to the buffer
          assertThrows(BufferUnderflowException.class, () -> BerTlv.getInstance(buffer));

          assertEquals(offset, buffer.position());
          assertEquals(offset + index, buffer.limit());
          buffer
              .position(buffer.limit())
              .limit(capacity); // prepare buffer for next write-operation
        } else {
          // ... TLV-object completely written to the buffer
          assertEquals(offset, buffer.position());
          assertEquals(offset + octets.length, buffer.limit());

          final var present = BerTlv.getInstance(buffer);

          assertEquals(expectedTlv, present.toString(" "));
          assertEquals(offset + octets.length, buffer.position());
          assertFalse(buffer.hasRemaining());
        } // end else
      } // end For (index...)
    } // end --- d.
  } // end method */

  /** Test method for {@link BerTlv#getInstance(InputStream)}. */
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.CyclomaticComplexity",
    "PMD.NcssCount",
    "PMD.NPathComplexity"
  })
  @Test
  void test_340_getInstance__InputStream() {
    // Assertions:
    // ... a. toString(String)-method works as expected

    // Note 1: Tests on randomly generated TLV objects are performed by test method
    //         "test_900_ConstructedBerTlv_getInstance__InputStream()".

    // Test strategy:
    // --- a. smoke test
    // --- b. specific subclasses
    // --- c. check for defensive cloning
    // --- d. ERROR: IllegalArgumentException, not in accordance to ISO/IEC 8825-1:2021
    // --- e. ERROR: ArithmeticException, tag-field too long
    // --- f. ERROR: ArithmeticException, value from length-field exceeds Long#MAX_VALUE
    // --- g. ERROR: IOException

    final var path = claTempDir.resolve("test_getInstance__InputStream.bin");

    // --- a. smoke test
    // a.1 from and toString (ByteArrayInputStream)
    // a.2 change created objects
    // a.3 from and toString (FileInputStream)
    {
      final var setInput =
          Set.of(
              "80 02 1122", //       primitive
              "a1 00", //            constructed,   empty
              "a2 03  87 01 99"); // constructed, non-empty

      // a.1 from and toString
      for (final var expected : setInput) {
        final var is = new ByteArrayInputStream(Hex.toByteArray(expected)); // NOPMD new in loop

        final var dut = BerTlv.getInstance(is);

        final var present = dut.toString(" ");
        assertEquals(expected, present);
      } // end For (input...)

      // a.2 change created objects
      // a.2.i  change primitive objects: intentionally empty, because immutable
      // a.2.ii change constructed objects
      BerTlv dutA = BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray("60 00")));
      assertEquals(ConstructedBerTlv.class, dutA.getClass());
      final ConstructedBerTlv dutA2ii = (ConstructedBerTlv) dutA;
      dutA =
          dutA2ii.add(
              BerTlv.getInstance(
                  new ByteArrayInputStream(Hex.toByteArray("80 01 07")))); // add primitive
      assertNotSame(dutA2ii, dutA);
      assertEquals(ConstructedBerTlv.class, dutA.getClass());
      assertEquals("60 03  80 01 07", dutA.toString(" "));

      // a.3 from and toString (FileInputStream)
      try {
        for (final var expected : setInput) {
          Files.write(path, Hex.toByteArray(expected));
          try (var is = Files.newInputStream(path)) {
            final var dut = BerTlv.getInstance(is);

            final var present = dut.toString(" ");
            assertEquals(expected, present);
          } // end try-with-resources
        } // end For (input...)
      } catch (IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end --- a.

    // --- b. Specific subclasses
    final var tagFields =
        VALID_TAGS.stream()
            .filter(tagField -> tagField.length <= BerTlv.NO_TAG_FIELD)
            .map(Hex::toHexDigits)
            .toList();
    for (final var tf : tagFields) {
      final var tagField = Hex.toByteArray(tf);
      final long tag = BerTlv.convertTag(tagField);

      switch ((int) tag) {
        case DerEndOfContent.TAG -> {
          for (final var input : Set.of("%s 00")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerEndOfContent.class, dut.getClass());
          } // end For (input...)
        } // EndOfContent,                                       tag-number =  0

        case DerBoolean.TAG -> {
          for (final var input :
              Set.of(
                  "%s 01 00", // FALSE
                  "%s 01 ff" // TRUE
                  )) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerBoolean.class, dut.getClass());
          } // end For (input...)
        } // BOOLEAN,                                            tag-number =  1

        case DerInteger.TAG -> {
          for (final var input :
              Set.of(
                  "%s 01 ff", //  -1
                  "%s 01 00", //   0
                  "%s 02 0080" // 127
                  )) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerInteger.class, dut.getClass());
          } // end For (input...)
        } // INTEGER,                                            tag-number =  2

        case DerBitString.TAG -> {
          for (final var input :
              Set.of(
                  "%s 01 00", // empty
                  "%s 02 0780", // 1 bit
                  "%s 02 00a5" // 8 bit
                  )) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerBitString.class, dut.getClass());
          } // end For (input...)
        } // BITSTRING,                                          tag-number =  3

        case DerOctetString.TAG -> {
          for (final var input :
              Set.of(
                  "%s 00", // empty
                  "%s 01 47", // 1 octet
                  "%s 02 0080" // 2 octets
                  )) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerOctetString.class, dut.getClass());
          } // end For (input...)
        } // OCTETSTRING,                                        tag-number =  4

        case DerNull.TAG -> {
          for (final var input : Set.of("%s 00")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerNull.class, dut.getClass());
          } // end For (input...)
        } // NULL,                                               tag-number =  5

        case DerOid.TAG -> {
          for (final var input : Set.of("%s 03 813403", "%s 06 2b2403050301")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerOid.class, dut.getClass());
          } // end For (input...)
        } // OBJECT IDENTIFIER,                                  tag-number =  6

        case DerUtf8String.TAG -> {
          for (final var input : Set.of("%s 03 414242", "%s 06 313233343536")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerUtf8String.class, dut.getClass());
          } // end For (input...)
        } // UTF8String,                                         tag-number = 12

        case DerSequence.TAG -> {
          for (final var input : Set.of("%s 03  81-01-03", "%s 06  1b-04-24030503")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerSequence.class, dut.getClass());
          } // end For (input...)
        } // SEQUENCE,                                           tag-number = 16

        case DerSet.TAG -> {
          for (final var input : Set.of("%s 03  81-01-03", "%s 06  1b-04-24030503")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerSet.class, dut.getClass());
          } // end For (input...)
        } // SET,                                                tag-number = 17

        case DerPrintableString.TAG -> {
          for (final var input : Set.of("%s 02 3132", "%s 02 4162")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerPrintableString.class, dut.getClass());
          } // end For (input...)
        } // PrintableString,                                    tag-number = 19

        case DerTeletexString.TAG -> {
          for (final var input : Set.of("%s 02 3132", "%s 02 4162")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerTeletexString.class, dut.getClass());
          } // end For (input...)
        } // DerTeletexString,                                   tag-number = 20

        case DerIa5String.TAG -> {
          for (final var input : Set.of("%s 02 3132", "%s 02 4162")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerIa5String.class, dut.getClass());
          } // end For (input...)
        } // DerIa5String,                                       tag-number = 22

        case DerUtcTime.TAG -> {
          for (final var input :
              Set.of("%s 0b 323130323139313834345a", "%s 11 3231303231393138343530392d31303235")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerUtcTime.class, dut.getClass());
          } // end For (input...)
        } // UTCTime,                                            tag-number = 23

        case DerDate.TAG -> {
          for (final var input : Set.of("%s 08 3139383730363234")) {
            final var is = new ByteArrayInputStream(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getInstance(is);

            assertEquals(DerDate.class, dut.getClass());
          } // end For (input...)
        } // DATE,                                               tag-number = 31

        default -> {
          // ... tag has no specific subclass

          if (0x00 == (tagField[0] & 0x20)) { // NOPMD literal in if statement
            // ... generic PrimitiveBerTlv
            final byte[] valueField = RNG.nextBytes(0, 32);
            final String input =
                tf // tag-field
                    + BerTlv.getLengthField(valueField.length) // length-field
                    + Hex.toHexDigits(valueField); // value-field
            final var is = new ByteArrayInputStream(Hex.toByteArray(input));

            final var dut = BerTlv.getInstance(is);

            assertEquals(PrimitiveBerTlv.class, dut.getClass());
          } else {
            // ... generic ConstructedBerTlv
            final String input = tf + "00"; // tag-field || length-field, no value-field
            final var is = new ByteArrayInputStream(Hex.toByteArray(input));

            final var dut = BerTlv.getInstance(is);

            assertEquals(ConstructedBerTlv.class, dut.getClass());
          } // end fi
        } // end default
      } // end Switch tag
    } // end For (tagField...)

    // --- c. check for defensive cloning
    List.of(
            "c1 00", // primitive,   value-field absent
            "c2 07 0123456789abcd", // primitive,   value-field present
            "e3 00", // constructed, value-field absent
            "e4 07  c5 01 10  c6 02 abcd" // constructed, value-field present
            )
        .forEach(
            input -> {
              final byte[] octetString = Hex.toByteArray(input);
              final ByteArrayInputStream bais = new ByteArrayInputStream(octetString);

              final var dutC1 = MyBerTlv.getInstance(bais);
              final BerTlv dutC2;
              if (dutC1 instanceof PrimitiveBerTlv) {
                // ... PrimitiveBerTlv, mark-position never changes
                //     => it is possible to reuse the InputStream
                bais.reset();
                dutC2 = MyBerTlv.getInstance(bais);
              } else {
                // ... not PrimitiveBerTlv, i.e. ConstructedBerTlv, there mark-position changes
                //     => reuse underlying byte-array rather than the InputStream
                dutC2 = MyBerTlv.getInstance(new ByteArrayInputStream(octetString));
              } // end else
              Arrays.fill(octetString, (byte) 0x00);
              assertEquals(input, dutC1.toString(" "));
              assertEquals(input, dutC2.toString(" "));
              assertEquals(dutC1, dutC2);
              zzzNotSame(dutC1, dutC2, true);
            }); // end forEach(input -> ...)

    // --- d. ERROR: IllegalArgumentException, not in accordance to ISO/IEC 8825-1:2021
    {
      final var input =
          Set.of(
              "c0-80-4712", // primitive with indefinite form
              "a0-80-81-01-24" // constructed with indefinite form without EndOfContent
              );
      for (final var i : input) {
        final var is = new ByteArrayInputStream(Hex.toByteArray(i));

        assertThrows(IllegalArgumentException.class, () -> BerTlv.getInstance(is));
      } // end For (i...)
    } // end --- d.

    // --- e. ERROR: ArithmeticException, tag-field too long
    {
      final var input =
          Set.of(
              "9f8283848586878809-01-13", // primitive tag
              "bf8283848586878809-01-13" // constructed tag
              );
      for (final var i : input) {
        final var is = new ByteArrayInputStream(Hex.toByteArray(i));

        assertThrows(ArithmeticException.class, () -> BerTlv.getInstance(is));
      } // end For (i...)
    } // end --- e.

    // --- f. ERROR: ArithmeticException, value from length-field exceeds Long#MAX_VALUE
    {
      final var input =
          Set.of(
              "c1-888000000000000000-810124", // primitive tag
              "e1-888000000000000000-(81-02-24)" // constructed tag
              );
      for (final var i : input) {
        final var is = new ByteArrayInputStream(Hex.toByteArray(i));

        assertThrows(ArithmeticException.class, () -> BerTlv.getInstance(is));
      } // end For (i...)
    } // end --- f.

    // --- g. ERROR: IOException
    try {
      Files.write(path, Hex.toByteArray("80-01-23"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        final var e = assertThrows(IllegalArgumentException.class, () -> BerTlv.getInstance(is));

        assertEquals(IllegalArgumentException.class, e.getClass());
        assertEquals("unexpected IOException", e.getMessage());
        assertEquals(ClosedChannelException.class, e.getCause().getClass());
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- g.
  } // end method */

  /** Test method for {@link BerTlv#getInstance(long, byte[])}. */
  @Test
  void test_350_getInstance__long_byteA() {
    // Assertions:
    // ... a. "getInstance(InputStrem)"-method works as expected
    // ... b. toString(String)-method works as expected

    // Test strategy:
    // --- a. smoke test for a primitive TLV object
    // --- b. smoke test for a constructed TLV object
    // --- c. check for defensive cloning
    // --- d. ERROR: IllegalArgumentException, not in accordance to ISO/IEC 8825-1:2021
    // --- e. ERROR: ArithmeticException, tag-field too long
    // --- f. ERROR: ArithmeticException, value from length-field exceeds Long#MAX_VALUE
    // --- g. ERROR: NullPointerException inside the thread

    BerTlv dut;

    // --- a. smoke test for a primitive TLV object
    {
      dut = BerTlv.getInstance(0x40, Hex.toByteArray("112233"));
      assertEquals(PrimitiveBerTlv.class, dut.getClass());
      assertEquals("40 03 112233", dut.toString(" "));
    } // end --- a.

    // --- b. smoke test for a constructed TLV object
    {
      dut = BerTlv.getInstance(0x61, Hex.toByteArray("81 01 11   82 02 1234"));
      assertEquals(ConstructedBerTlv.class, dut.getClass());
      assertEquals("61 07  81 01 11  82 02 1234", dut.toString(" "));
    } // end --- b.

    // --- c. check for defensive cloning
    {
      final var tags =
          List.of(
              0xc1, 0xdf25, // primitive
              0xe3, 0xff27 // constructed
              );
      final var valueFields =
          List.of(
              "c1 00", // primitive,   value-field absent
              "c2 07 0123456789abcd", // primitive,   value-field present
              "e3 00", // constructed, value-field absent
              "e4 07  c5 01 10  c6 02 abcd" // constructed, value-field present
              );
      for (final var tag : tags) {
        for (final var valueField : valueFields) {
          final var octetString = Hex.toByteArray(valueField);
          final var dutC1 = BerTlv.getInstance(tag, octetString);
          final var dutC2 = BerTlv.getInstance(tag, octetString);

          Arrays.fill(octetString, (byte) 0x00);
          assertEquals(dutC1, dutC2);
          zzzNotSame(dutC1, dutC2, true);
        } // end For (octetString...)
      } // end For (tag...)
    } // end --- c.

    // --- d. ERROR: IllegalArgumentException, not in accordance to ISO/IEC 8825-1:2021
    {
      final var input =
          Set.of(
              "c0-80-4711", // primitive with indefinite form
              "a0-80-81-01-23" // constructed with indefinite form without EndOfContent
              );
      for (final var i : input) {
        final var octets = Hex.toByteArray(i);

        assertThrows(IllegalArgumentException.class, () -> BerTlv.getInstance(0x20, octets));
      } // end For (i...)
    } // end --- d.

    // --- e. ERROR: ArithmeticException, tag-field too long
    {
      final var input =
          Set.of(
              "9f8283848586878809-01-12", // primitive tag
              "bf8283848586878809-01-12" // constructed tag
              );
      for (final var i : input) {
        final var octets = Hex.toByteArray(i);

        assertThrows(ArithmeticException.class, () -> BerTlv.getInstance(0x21, octets));
      } // end For (i...)
    } // end --- e.

    // --- f. ERROR: ArithmeticException, value from length-field exceeds Long#MAX_VALUE
    {
      final var input =
          Set.of(
              "c1-888000000000000000-810123", // primitive tag
              "e1-888000000000000000-(81-02-23)" // constructed tag
              );
      for (final var i : input) {
        final var octets = Hex.toByteArray(i);

        assertThrows(ArithmeticException.class, () -> BerTlv.getInstance(0x22, octets));
      } // end For (i...)
    } // end --- f.

    // --- g. ERROR: NullPointerException inside the thread
    {
      assertThrows(IllegalArgumentException.class, () -> BerTlv.getInstance(0x23, (byte[]) null));
    } // end --- g.
  } // end method */

  /** Test method for {@link BerTlv#getInstance(long, Collection)}. */
  @Test
  void test_360_getInstance__long_Collection() {
    // Assertions:
    // ... a. underlying constructor PrimitiveBerTlv  (long, byte[]) works as expected
    // ... b. underlying constructor ConstructedBerTlv(List, long) works as expected
    // ... c. toString(String)-method works as expected

    // Test strategy:
    // --- a. smoke test for a primitive TLV object
    // --- b. smoke test for a constructed TLV object
    // --- c. special constructed TLV-objects
    // --- d. ERROR: primitive tag and value-field too long
    // --- e. ERROR: constructed tag and value-field too long

    BerTlv dut;
    Collection<BerTlv> valueField;

    // --- a. smoke test for a primitive TLV object
    // a.1 empty List
    // a.2 List with one element
    // a.3 List with two elements
    {
      // a.1 empty List
      valueField = Collections.emptyList();

      dut = BerTlv.getInstance(0x40, valueField);

      assertEquals(PrimitiveBerTlv.class, dut.getClass());
      assertEquals("40 00", dut.toString(" "));

      // a.2 List with one element
      valueField = List.of(BerTlv.getInstance(0x80, Hex.toByteArray("a9b5")));

      dut = BerTlv.getInstance(0x41, valueField);

      assertEquals(PrimitiveBerTlv.class, dut.getClass());
      assertEquals("41 04 8002a9b5", dut.toString(" "));

      // a.3 List with two elements
      valueField =
          List.of(
              BerTlv.getInstance(0x80, Hex.toByteArray("a9b8")),
              BerTlv.getInstance(0xa1, Hex.toByteArray("91 02 4321")));

      dut = BerTlv.getInstance(0x42, valueField);

      assertEquals(PrimitiveBerTlv.class, dut.getClass());
      assertEquals("42 0a 8002a9b8a10491024321", dut.toString(" "));
    } // end --- a.

    // --- b. smoke test for a constructed TLV object
    // b.1 empty Collection
    // b.2 List with one element
    // b.3 List with two elements
    {
      // b.1 empty Collection
      valueField = Collections.emptySet();

      dut = BerTlv.getInstance(0x60, valueField);

      assertEquals(ConstructedBerTlv.class, dut.getClass());
      assertEquals("60 00", dut.toString(" "));
      final var dutB =
          ((ConstructedBerTlv) dut).add(BerTlv.getInstance(0x8f, Hex.toByteArray("fedc")));
      assertNotSame(dut, dutB);
      assertEquals("60 04  8f 02 fedc", dutB.toString(" "));

      // b.2 List with one element
      valueField = List.of(BerTlv.getInstance(0x80, "a9b4"));

      dut = BerTlv.getInstance(0x61, valueField);

      assertEquals(ConstructedBerTlv.class, dut.getClass());
      assertEquals("61 04  80 02 a9b4", dut.toString(" "));

      // b.3 List with two elements
      valueField =
          List.of(
              BerTlv.getInstance(0x80, Hex.toByteArray("a9b3")),
              BerTlv.getInstance(0xa1, Hex.toByteArray("91 02 4321")));

      dut = BerTlv.getInstance(0x62, valueField);

      assertEquals(ConstructedBerTlv.class, dut.getClass());
      assertEquals("62 0a  80 02 a9b3  a1 04  91 02 4321", dut.toString(" "));
    } // end --- b.

    // --- c. special constructed TLV-objects
    {
      valueField =
          List.of(
              BerTlv.getInstance(0x80, Hex.toByteArray("a9b4")),
              BerTlv.getInstance(0xa1, Hex.toByteArray("91 02 4320")));
      final var expected = "%02x 0a  80 02 a9b4  a1 04  91 02 4320";
      for (final var tag : Set.of(DerSequence.TAG, DerSet.TAG)) {
        dut = BerTlv.getInstance(tag, valueField);

        assertEquals(String.format(expected, tag), dut.toString(" "));
      } // end For (tag...)
    } // end --- c.

    // --- d. ERROR: primitive tag and value-field too long
    {
      final long tlvLength = Integer.MAX_VALUE + 1L;
      final var lists =
          List.of(
              // size = 1
              List.of((BerTlv) new MyBerTlv(0xdf41, Integer.MAX_VALUE - 6)),
              // size = 2
              List.of(
                  BerTlv.getInstance(0x42, Hex.toByteArray("112233")),
                  new MyBerTlv(0xdf42, Integer.MAX_VALUE - 11)),
              // size = 3
              List.of(
                  BerTlv.getInstance(0x43, Hex.toByteArray("11223344")),
                  new MyBerTlv(0xdf43, Integer.MAX_VALUE - 14),
                  BerTlv.getInstance(0xe4, EMPTY_OS)));
      for (final var list : lists) {
        // assure that sum of lengths of TLV-objects is beyond int-range
        assertEquals(
            tlvLength,
            list.stream()
                .map(tlv -> BigInteger.valueOf(tlv.getLengthOfTlvObject()))
                .reduce(BigInteger::add)
                .orElse(BigInteger.ZERO)
                .longValueExact());

        final var e = assertThrows(ArithmeticException.class, () -> BerTlv.getInstance(0x44, list));

        assertEquals("BigInteger out of int range", e.getMessage());
        assertNull(e.getCause());
      } // end For (list...)
    } // end --- d.

    // --- e. ERROR: constructed tag and value-field too long
    {
      final String tlvLength = BigInteger.ONE.add(BigInteger.valueOf(Long.MAX_VALUE)).toString(16);
      final var lists =
          List.of(
              // Note: size = 1 is not possible, because a single TLV-object with
              //       lengthOfTlvObject > Long.MAX_VALUE causes ArithmeticException in
              //       getLengthOfTlvObject()-method.
              // size = 2
              List.of(
                  BerTlv.getInstance(0x52, Hex.toByteArray("112233")),
                  new MyBerTlv(0xdf52, Long.MAX_VALUE - 15)),
              // size = 3
              List.of(
                  BerTlv.getInstance(0x53, Hex.toByteArray("11223344")),
                  new MyBerTlv(0xdf53, Long.MAX_VALUE - 18),
                  BerTlv.getInstance(0xe5, EMPTY_OS)));
      for (final var list : lists) {
        // assure that sum of lengths of TLV-objects is beyond int-range
        assertEquals(
            tlvLength,
            list.stream()
                .map(tlv -> BigInteger.valueOf(tlv.getLengthOfTlvObject()))
                .reduce(BigInteger::add)
                .orElse(BigInteger.ZERO)
                .toString(16));

        final var e = assertThrows(ArithmeticException.class, () -> BerTlv.getInstance(0x64, list));

        assertEquals("BigInteger out of long range", e.getMessage());
        assertNull(e.getCause());
      } // end For (list...)
    } // end --- e.
  } // end method */

  /** Test method for {@link BerTlv#getInstance(long, String)}. */
  @Test
  void test_370_getInstance__long_String() {
    // Assertions:
    // ... a. "getInstance(long, byte[])"-method works as expected

    // Test strategy:
    // --- a. smoke test for a primitive TLV object
    // --- b. smoke test for a constructed TLV object

    BerTlv dut;

    // --- a. smoke test for a primitive TLV object
    dut = BerTlv.getInstance(0x40, "112233");

    assertEquals(PrimitiveBerTlv.class, dut.getClass());
    assertEquals("40 03 112233", dut.toString(" "));

    // --- b. smoke test for a constructed TLV object
    dut = BerTlv.getInstance(0x61, "81 01 11   82 02 1234");

    assertEquals(ConstructedBerTlv.class, dut.getClass());
    assertEquals("61 07  81 01 11  82 02 1234", dut.toString(" "));
  } // end method */

  /** Test method for {@link BerTlv#getInstance(java.lang.String)}. */
  @Test
  void test_380_getInstance__String() {
    // Assertions:
    // ... a. "getInstance(byte[])"-method works as expected
    // ... b. toString(String)-method works as expected

    // Note 1: Because of assertion_a we can be lazy here and do some smoke tests.

    // Test strategy:
    // --- a. smoke test
    final String input = "a3 03  87 01 99";
    assertEquals(input, BerTlv.getInstance(input).toString(" "));
  } // end method */

  /** Test method for {@link BerTlv#getFromBuffer(ByteBuffer)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NcssCount"})
  @Test
  void test_390_getFromBuffer__ByteBuffer() {
    // Assertions:
    // ... a. toString(String)-method works as expected

    // Note 1: Tests on randomly generated TLV objects are performed by test method
    //         "test_900_ConstructedBerTlv_getInstance__InputStream()".

    // Test strategy:
    // --- a. smoke test
    // --- b. specific subclasses
    // --- c. check for defensive cloning
    // --- d. ERROR: tag-field too long

    // --- a. smoke test
    // a.1 from and toString (ByteArrayInputStream)
    // a.2 change created objects
    {
      final var setInput =
          Set.of(
              "80 02 1122", //       primitive
              "a1 00", //            constructed,   empty
              "a2 03  87 01 99"); // constructed, non-empty

      // a.1 from and toString
      for (final var expected : setInput) {
        final var buffer = ByteBuffer.wrap(Hex.toByteArray(expected));

        final var dut = BerTlv.getFromBuffer(buffer);

        final var present = dut.toString(" ");
        assertEquals(expected, present);
      } // end For (input...)

      // a.2 change created objects
      // a.2.i  change primitive objects: intentionally empty, because immutable
      // a.2.ii change constructed objects
      BerTlv dutA = BerTlv.getFromBuffer(ByteBuffer.wrap(Hex.toByteArray("60 00")));
      assertEquals(ConstructedBerTlv.class, dutA.getClass());
      final ConstructedBerTlv dutA2ii = (ConstructedBerTlv) dutA;
      dutA = dutA2ii.add(new PrimitiveBerTlv(0x80, Hex.toByteArray("07"))); // add primitive
      assertNotSame(dutA2ii, dutA);
      assertEquals(ConstructedBerTlv.class, dutA.getClass());
      assertEquals("60 03  80 01 07", dutA.toString(" "));
    } // end --- a.

    // --- b. Specific subclasses
    final var tagFields =
        VALID_TAGS.stream()
            .filter(tagField -> tagField.length <= BerTlv.NO_TAG_FIELD)
            .map(Hex::toHexDigits)
            .toList();
    for (final var tf : tagFields) {
      final var tagField = Hex.toByteArray(tf);
      final long tag = BerTlv.convertTag(tagField);

      switch ((int) tag) {
        case DerEndOfContent.TAG -> {
          for (final var input : Set.of("%s 00")) {
            final var buffer = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(buffer);

            assertEquals(DerEndOfContent.class, dut.getClass());
          } // end For (input...)
        } // EndOfContent,                                       tag-number =  0

        case DerBoolean.TAG -> {
          for (final var input :
              Set.of(
                  "%s 01 00", // FALSE
                  "%s 01 ff" // TRUE
                  )) {
            final var buffer = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(buffer);

            assertEquals(DerBoolean.class, dut.getClass());
          } // end For (input...)
        } // BOOLEAN,                                            tag-number =  1

        case DerInteger.TAG -> {
          for (final var input :
              Set.of(
                  "%s 01 ff", //  -1
                  "%s 01 00", //   0
                  "%s 02 0080" // 127
                  )) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerInteger.class, dut.getClass());
          } // end For (input...)
        } // INTEGER,                                            tag-number =  2

        case DerBitString.TAG -> {
          for (final var input :
              Set.of(
                  "%s 01 00", // empty
                  "%s 02 0780", // 1 bit
                  "%s 02 00a5" // 8 bit
                  )) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerBitString.class, dut.getClass());
          } // end For (input...)
        } // BITSTRING,                                          tag-number =  3

        case DerOctetString.TAG -> {
          for (final var input :
              Set.of(
                  "%s 00", // empty
                  "%s 01 47", // 1 octet
                  "%s 02 0080" // 2 octets
                  )) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerOctetString.class, dut.getClass());
          } // end For (input...)
        } // OCTETSTRING,                                        tag-number =  4

        case DerNull.TAG -> {
          for (final var input : Set.of("%s 00")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerNull.class, dut.getClass());
          } // end For (input...)
        } // NULL,                                               tag-number =  5

        case DerOid.TAG -> {
          for (final var input : Set.of("%s 03 813403", "%s 06 2b2403050301")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerOid.class, dut.getClass());
          } // end For (input...)
        } // OBJECT IDENTIFIER,                                  tag-number =  6

        case DerUtf8String.TAG -> {
          for (final var input : Set.of("%s 03 414242", "%s 06 313233343536")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerUtf8String.class, dut.getClass());
          } // end For (input...)
        } // UTF8String,                                         tag-number = 12

        case DerSequence.TAG -> {
          for (final var input : Set.of("%s 03  81-01-03", "%s 06  1b-04-24030503")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerSequence.class, dut.getClass());
          } // end For (input...)
        } // SEQUENCE,                                           tag-number = 16

        case DerSet.TAG -> {
          for (final var input : Set.of("%s 03  81-01-03", "%s 06  1b-04-24030503")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerSet.class, dut.getClass());
          } // end For (input...)
        } // SET,                                                tag-number = 17

        case DerPrintableString.TAG -> {
          for (final var input : Set.of("%s 02 3132", "%s 02 4162")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerPrintableString.class, dut.getClass());
          } // end For (input...)
        } // PrintableString,                                    tag-number = 19

        case DerTeletexString.TAG -> {
          for (final var input : Set.of("%s 02 3132", "%s 02 4162")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerTeletexString.class, dut.getClass());
          } // end For (input...)
        } // DerTeletexString,                                   tag-number = 20

        case DerIa5String.TAG -> {
          for (final var input : Set.of("%s 02 3132", "%s 02 4162")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerIa5String.class, dut.getClass());
          } // end For (input...)
        } // DerIa5String,                                       tag-number = 22

        case DerUtcTime.TAG -> {
          for (final var input :
              Set.of("%s 0b 323130323139313834345a", "%s 11 3231303231393138343530392d31303235")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerUtcTime.class, dut.getClass());
          } // end For (input...)
        } // UTCTime,                                            tag-number = 23

        case DerDate.TAG -> {
          for (final var input : Set.of("%s 08 3139383730363234")) {
            final var is = ByteBuffer.wrap(Hex.toByteArray(String.format(input, tf)));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(DerDate.class, dut.getClass());
          } // end For (input...)
        } // DATE,                                               tag-number = 31

        default -> {
          // ... tag has no specific subclass

          if (0x00 == (tagField[0] & 0x20)) { // NOPMD literal in if statement
            // ... generic PrimitiveBerTlv
            final byte[] valueField = RNG.nextBytes(0, 32);
            final String input =
                tf // tag-field
                    + BerTlv.getLengthField(valueField.length) // length-field
                    + Hex.toHexDigits(valueField); // value-field
            final var is = ByteBuffer.wrap(Hex.toByteArray(input));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(PrimitiveBerTlv.class, dut.getClass());
            assertEquals(Hex.toHexDigits(tagField), dut.getTagField());
            assertArrayEquals(valueField, dut.getValueField());
          } else {
            // ... generic ConstructedBerTlv
            final String input = tf + "00"; // tag-field || length-field, no value-field
            final var is = ByteBuffer.wrap(Hex.toByteArray(input));

            final var dut = BerTlv.getFromBuffer(is);

            assertEquals(ConstructedBerTlv.class, dut.getClass());
            assertEquals(Hex.toHexDigits(tagField), dut.getTagField());
          } // end fi
        } // end default
      } // end Switch tag
    } // end For (tagField...)

    // --- c. check for defensive cloning
    List.of(
            "c1 00", // primitive,   value-field absent
            "c2 07 0123456789abcd", // primitive,   value-field present
            "e3 00", // constructed, value-field absent
            "e4 07  c5 01 10  c6 02 abcd" // constructed, value-field present
            )
        .forEach(
            input -> {
              final var octetString = Hex.toByteArray(input);
              final var buf = ByteBuffer.wrap(octetString);

              final var dutC1 = BerTlv.getFromBuffer(buf);
              final var dutC2 = BerTlv.getFromBuffer(buf.clear());

              Arrays.fill(octetString, (byte) 0x00);
              assertEquals(input, dutC1.toString(" "));
              assertEquals(input, dutC2.toString(" "));
              assertEquals(dutC1, dutC2);
              zzzNotSame(dutC1, dutC2, true);
            }); // end forEach(input -> ...)

    // --- d. ERROR: tag-field too long
    Set.of(
            "df82 8384 8586 8788 09   00", // primitive,   length value-field = 00
            "bf92 9394 9596 9798 19   00" // constructed, length value-field = 00
            )
        .forEach(
            input -> {
              final var b = ByteBuffer.wrap(Hex.toByteArray(input));

              final var e = assertThrows(ArithmeticException.class, () -> BerTlv.getFromBuffer(b));

              assertNull(e.getCause());
            }); // end forEach(input -> ...)
    // */
  } // end method */

  /** Test method for {@link BerTlv#getLengthField()}. */
  @Test
  void test_400_getLengthField() {
    // Assertions:
    // ... a. method getLengthField(long)    works as expected
    // ... b. method getLengthOfValueField() works as expected

    // Note: Underlying method is sufficiently tested elsewhere, so we can be lazy here.

    // Test strategy:
    // --- a. manual test for various lengths
    final long tag = 0x00;
    assertEquals("00", new MyBerTlv(tag, 0x00).getLengthField());
    assertEquals("7f", new MyBerTlv(tag, 0x7f).getLengthField());
    assertEquals("81e2", new MyBerTlv(tag, 0xe2).getLengthField());
  } // end method */

  /** Test method for {@link BerTlv#getLengthField(long)}. */
  @Test
  void test_410_getLengthField__long() {
    // Test strategy:
    // --- a. manual tests for various lengths
    // --- b. negative lengths are invalid

    long lengthOfValueField;

    // --- a. manual tests for various lengths
    // a.1 length-field 1 octet long
    assertEquals("00", BerTlv.getLengthField(0x00L)); // infimum of valid values
    assertEquals("7f", BerTlv.getLengthField(lengthOfValueField = 0x7f));

    // a.2 length-field 2 octet long
    assertEquals("8180", BerTlv.getLengthField(++lengthOfValueField));
    assertEquals("81ff", BerTlv.getLengthField(lengthOfValueField = 0xff));

    // a.3 length-field 3 octet long
    assertEquals("820100", BerTlv.getLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals("82ffff", BerTlv.getLengthField(--lengthOfValueField));

    // a.4 length-field 4 octet long
    assertEquals("83010000", BerTlv.getLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals("83ffffff", BerTlv.getLengthField(--lengthOfValueField));

    // a.5 length-field 5 octet long
    assertEquals("8401000000", BerTlv.getLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals("84ffffffff", BerTlv.getLengthField(--lengthOfValueField));

    // a.6 length-field 6 octet long
    assertEquals("850100000000", BerTlv.getLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals("85ffffffffff", BerTlv.getLengthField(--lengthOfValueField));

    // a.7 length-field 7 octet long
    assertEquals("86010000000000", BerTlv.getLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals("86ffffffffffff", BerTlv.getLengthField(--lengthOfValueField));

    // a.8 length-field 8 octet long
    assertEquals("8701000000000000", BerTlv.getLengthField(++lengthOfValueField));
    lengthOfValueField <<= 8;
    assertEquals("87ffffffffffffff", BerTlv.getLengthField(--lengthOfValueField));

    // a.9 length-field 9 octet long
    assertEquals("880100000000000000", BerTlv.getLengthField(++lengthOfValueField));
    lengthOfValueField = Long.MAX_VALUE; // supremum of valid values
    assertEquals("887fffffffffffffff", BerTlv.getLengthField(lengthOfValueField));

    // --- b. negative lengths are invalid
    Stream.of(
            -1L, // supremum of invalid values
            Long.MIN_VALUE // infimum of invalid values
            )
        .forEach(
            len ->
                assertThrows(
                    IllegalArgumentException.class,
                    () -> BerTlv.getLengthField(len))); // end forEach(lengthOfValueField -> ...)
  } // end method */

  /** Test method for {@link BerTlv#getLengthOfTagField()}. */
  @Test
  void test_420_getLengthOfTagField() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over valid tags

    // --- a. smoke test
    {
      final var dut = BerTlv.getInstance("d6-01-fa");

      final var present = dut.getLengthOfTagField();

      assertEquals(1, present);
    } // end --- a.

    // --- b. loop over valid tags
    VALID_TAGS.stream()
        .filter(tag -> (tag.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            tag -> {
              final var dut = new MyBerTlv(tag);

              assertEquals(tag.length, dut.getLengthOfTagField());
            }); // end forEach(octets -> ...)
  } // end method */

  /** Test method for {@link BerTlv#getLengthOfTlvObject()}. */
  @Test
  void test_430_getLengthOfTlvObject() {
    // Assertions:
    // ... a. Constructor work as expected.

    // Simple method doesn't need extensive testing. Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test, just for good test-coverage.
    // --- b. ERROR: ArithmeticException

    // --- a. smoke test, just for good test-coverage.
    assertEquals(1 + 2 + 128, new MyBerTlv(0x00, 128).getLengthOfTlvObject());

    // --- b. ERROR: ArithmeticException
    long lengthValueField = Long.MAX_VALUE - 10;
    final var dutB1 = new MyBerTlv(0x41, lengthValueField);
    assertEquals(Long.MAX_VALUE, dutB1.getLengthOfTlvObject());
    final var dutB2 = new MyBerTlv(0x42, ++lengthValueField); // Spotbugs: DLS_DEAD_LOCAL_STORE
    final Throwable thrown = assertThrows(ArithmeticException.class, dutB2::getLengthOfTlvObject);
    assertEquals("long overflow", thrown.getMessage());
    assertNull(thrown.getCause());
  } // end method */

  /** Test method for {@link BerTlv#getLengthOfValueField()}. */
  @Test
  void test_440_getLengthOfValueField() {
    // Note 1: As described in the Java-Doc comment to instance attribute insLengthOfValueField
    //         that instance attribute is properly set by subclasses.
    //         Thus, this method has to be tested by subclasses.
    // Note 2: Here we do just a smoke test.

    // Test strategy:
    // --- a. Smoke test where instance attribute is properly set.
    // --- b. Smoke test where instance attribute is not properly set.

    // --- a. Smoke test where instance attribute is properly set.
    assertEquals(2, new MyBerTlv(0x00, 2).getLengthOfValueField());

    // --- b. Smoke test where instance attribute is not properly set.
    try {
      final var dut =
          new MyBerTlv(new byte[] {-2}, new ByteArrayInputStream(Hex.toByteArray("00")));
      final Throwable thrown = assertThrows(RuntimeException.class, dut::getLengthOfValueField);
      assertEquals(
          "instance attribute LengthOfValueField not (yet) properly initialized",
          thrown.getMessage());
      assertNull(thrown.getCause());
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- b.
  } // end method */

  /** Test method for {@link BerTlv#getTag()}. */
  @Test
  void test_450_getTag() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over valid tags

    // --- a. smoke test
    {
      final var expected = 0xd8;
      final var dut = BerTlv.getInstance(String.format("%02x-01-ba", expected));

      final var present = dut.getTag();

      assertEquals(expected, present);
    } // end --- a.

    // --- b. loop over valid tags
    VALID_TAGS.stream()
        .filter(octets -> (octets.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            octets -> {
              final long tag =
                  ((8 == octets.length) ? new BigInteger(octets) : new BigInteger(1, octets))
                      .longValueExact();
              final var dut = new MyBerTlv(octets);

              assertEquals(tag, dut.getTag());
            }); // end forEach(octets -> ...)
  } // end method */

  /** Test method for {@link BerTlv#getTagField()}. */
  @Test
  void test_460_getTagField() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over valid tags

    // --- a. smoke test
    {
      final var expected = "d7";
      final var dut = BerTlv.getInstance(expected + "01-fa");

      final var present = dut.getTagField();

      assertEquals(expected, present);
    } // end --- a.

    // --- b. loop over valid tags
    VALID_TAGS.stream()
        .filter(tag -> (tag.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            tag -> {
              final var dut = new MyBerTlv(tag);

              assertEquals(Hex.toHexDigits(tag), dut.getTagField());
            }); // end forEach(octets -> ...)
  } // end method */

  /** Test method for {@link BerTlv#hashCode()}. */
  @Test
  void test_470_hashCode() {
    // Test strategy:
    // --- a. loop over relevant subset of available tags
    // --- b. call hashCode()-method again

    // --- a. loop over relevant subset of available tags
    VALID_TAGS.stream()
        .filter(tagField -> (tagField.length <= BerTlv.NO_TAG_FIELD))
        .forEach(
            tagField -> {
              final var dut = new MyBerTlv(tagField);
              final long tag = dut.getTag();
              final int msInt = (int) (tag >> 32);
              final int lsInt = (int) tag;

              assertEquals(msInt * 31 + lsInt, dut.hashCode());
            }); // end forEach(tagField -> ...)

    // --- b. call hashCode()-method again
    final var dut = new MyBerTlv(0x20, 0);
    final int hash = dut.hashCode();
    assertEquals(hash, dut.hashCode());
  } // end method */

  /** Test method for {@link BerTlv#readLength(ByteBuffer)}. */
  @Test
  void test_480_readLength__ByteBuffer() {
    // Assertions:
    // ... a. BerTlv.getLengthField(long)-method works as expected

    // Test strategy:
    // --- a. read valid length-fields from ByteBuffer
    // --- b. ERROR: not all bytes of length-field available) from ByteBuffer
    // --- c. indefinite form
    // --- d. check for ArithmeticException

    long lengthOfValueField;

    // --- a. read valid length-fields from ByteBuffer
    {
      final Set<Long> relevantTestValues = new HashSet<>();
      // a.1 length-field 1 octet long
      relevantTestValues.add(0x00L);
      relevantTestValues.add(lengthOfValueField = 0x7f);

      // a.2 length-field 2 octet long
      relevantTestValues.add(++lengthOfValueField);
      relevantTestValues.add(lengthOfValueField = 0xff);

      // a.3 length-field 3 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // a.4 length-field 4 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // a.5 length-field 5 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // a.6 length-field 6 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // a.7 length-field 7 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // a.8 length-field 8 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // a.9 length-field 9 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField = Long.MAX_VALUE;
      relevantTestValues.add(lengthOfValueField);
      relevantTestValues.add(--lengthOfValueField); // Spotbugs: DLS_DEAD_LOCAL_STORE

      // a.10 loop over all values
      relevantTestValues.forEach(
          loValueField -> {
            Set.of(
                    BerTlv.getLengthField(loValueField),
                    String.format("8900%016x", loValueField),
                    String.format("8a0000%016x", loValueField))
                .forEach(
                    input -> {
                      final byte[] octets = Hex.toByteArray(input);
                      final ByteBuffer buffer = ByteBuffer.wrap(octets);
                      assertEquals(0, buffer.position());
                      assertEquals(buffer.capacity(), buffer.limit());

                      final long[] actual = BerTlv.readLength(buffer);

                      assertEquals(buffer.capacity(), buffer.position());
                      assertFalse(buffer.hasRemaining());
                      assertEquals(loValueField, actual[0]);
                      assertEquals(octets.length, actual[1]);

                      // --- b. ERROR: not all bytes of length-field available from ByteBuffer
                      buffer.clear().limit(buffer.capacity() - 1);
                      assertEquals(0, buffer.position());
                      assertThrows(BufferUnderflowException.class, () -> BerTlv.readLength(buffer));
                      assertEquals((2 == input.length()) ? 0 : 1, buffer.position());
                    }); // end forEach(input -> ...)
          }); // end forEach(loValueField -> ...)
    } // end --- a, b.

    // --- c. indefinite form
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("80"));
      assertEquals(0, buffer.position());
      assertEquals(buffer.capacity(), buffer.limit());

      final long[] actual = BerTlv.readLength(buffer);

      assertEquals(-1, actual[0]);
      assertEquals(1, actual[1]);
      assertFalse(buffer.hasRemaining());
    } // end --- c.

    // --- d. check for ArithmeticException
    {
      final var supremumGood = ByteBuffer.wrap(Hex.toByteArray("887fffffffffffffff"));
      final var infimumBad = ByteBuffer.wrap(Hex.toByteArray("888000000000000000"));

      // d.1 supremum happy case
      final var actual = BerTlv.readLength(supremumGood);
      assertEquals(0x7fffffffffffffffL, actual[0]);
      assertEquals(9, actual[1]);
      assertFalse(supremumGood.hasRemaining());

      // d.2 infimum length too big
      assertThrows(ArithmeticException.class, () -> BerTlv.readLength(infimumBad));
      assertFalse(infimumBad.hasRemaining());
    } // end --- d.
  } // end method */

  /** Test method for {@link BerTlv#readLength(InputStream)}. */
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.NcssCount",
  })
  @Test
  void test_490_readLength__InputStream() throws IOException {
    // Assertions:
    // ... a. BerTlv.getLengthField(long)-method works as expected

    // Test strategy:
    // --- a. read valid length-fields from ByteArrayInputStream
    // --- b. ERROR: not all bytes of length-field available, from ByteArrayInputStream
    // --- c. ERROR: not all bytes of length-field available, from File
    // --- d. read from PipedInputStream
    // --- e. indefinite form
    // --- f. check for ArithmeticException
    // --- g. ERROR: IOException

    final Path path = claTempDir.resolve("test_490_readLength__InputStream.bin");
    try (var po = new PipedOutputStream();
        var pi = new PipedInputStream(po, 1)) {
      final var queue = new LinkedBlockingQueue<byte[]>();

      final var thread =
          new Thread(
              () -> {
                try {
                  for (; ; ) {
                    final var input = queue.take();

                    if (0 == input.length) {
                      break;
                    } // end fi

                    AfiUtils.chill(10);

                    for (final var b : input) {
                      po.write(b);
                      po.flush();
                      AfiUtils.chill(1);
                    } // end For (b...)
                  } // end forever-loop
                } catch (IOException | InterruptedException e) {
                  LOGGER.atError().log(UNEXPECTED, e);
                } // end Catch (...)
              });
      thread.start();

      // --- define length-values
      long lengthOfValueField;
      final Set<Long> relevantTestValues = new TreeSet<>();
      // length-field 1 octet long
      relevantTestValues.add(0x00L);
      relevantTestValues.add(lengthOfValueField = 0x7f);

      // length-field 2 octet long
      relevantTestValues.add(++lengthOfValueField);
      relevantTestValues.add(lengthOfValueField = 0xff);

      // length-field 3 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // length-field 4 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // length-field 5 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // length-field 6 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // length-field 7 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // length-field 8 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField <<= 8;
      relevantTestValues.add(--lengthOfValueField);

      // length-field 9 octet long
      relevantTestValues.add(++lengthOfValueField);
      lengthOfValueField = Long.MAX_VALUE;
      relevantTestValues.add(lengthOfValueField);
      relevantTestValues.add(--lengthOfValueField); // Spotbugs: DLS_DEAD_LOCAL_STORE

      // loop over all values
      for (final var expected : relevantTestValues) {
        final var inputs =
            List.of(
                BerTlv.getLengthField(expected),
                String.format("8900%016x", expected),
                String.format("8a0000%016x", expected));
        for (final var input : inputs) {
          final var octets = Hex.toByteArray(input);

          // --- a. read valid length-fields from ByteArrayInputStream
          {
            final var is = new ByteArrayInputStream(octets);

            final var present = BerTlv.readLength(is);

            assertEquals(expected, present[0]);
            assertEquals(octets.length, present[1]);
          } // end --- a.

          final var prefix = Arrays.copyOf(octets, octets.length - 1);

          // --- b. ERROR: not all bytes of length-field available, from ByteArrayInputStream
          {
            final var is = new ByteArrayInputStream(prefix);

            assertThrows(EOFException.class, () -> BerTlv.readLength(is));
          } // end --- b.

          // --- c. ERROR: not all bytes of length-field available, from File
          {
            Files.write(path, prefix);
            try (var is = Files.newInputStream(path)) {
              assertThrows(EOFException.class, () -> BerTlv.readLength(is));
            } // end try-with-resources
          } // end --- c.

          // --- d. read from PipedInputStream
          {
            queue.put(octets);

            final var present = BerTlv.readLength(pi);

            assertEquals(expected, present[0]);
            assertEquals(octets.length, present[1]);
          } // end --- d.
        } // end For (input...)
      } // end For (loValueField...)

      queue.put(EMPTY_OS);
      thread.join();
    } catch (IOException | InterruptedException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- e. indefinite form
    {
      final var lengthInfo = BerTlv.readLength(new ByteArrayInputStream(new byte[] {(byte) 0x80}));
      assertEquals(-1, lengthInfo[0]);
      assertEquals(1, lengthInfo[1]);
    } // end --- e.

    // --- f. check for ArithmeticException
    // f.1 supremum happy case
    // f.2 infimum length too big
    {
      final String supGood = "887fffffffffffffff";
      final String infBad = "888000000000000000";

      // f.1 supremum happy case
      final var lengthInfo = BerTlv.readLength(new ByteArrayInputStream(Hex.toByteArray(supGood)));
      assertEquals(0x7fffffffffffffffL, lengthInfo[0]);
      assertEquals(9, lengthInfo[1]);

      // f.2 infimum length too big
      final var is = new ByteArrayInputStream(Hex.toByteArray(infBad));
      final var e = assertThrows(ArithmeticException.class, () -> BerTlv.readLength(is));
      assertEquals(
          "length of value-field too big for this implementation: '8000000000000000'",
          e.getMessage());
      assertNull(e.getCause());
    } // end --- f.

    // --- g. ERROR: IOException
    try {
      Files.write(path, Hex.toByteArray("89 000000000000778899"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        final var e = assertThrows(IOException.class, () -> BerTlv.readLength(is));

        assertEquals(ClosedChannelException.class, e.getClass());
        assertNull(e.getMessage());
        assertNull(e.getCause());
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- g.
  } // end method */

  /** Test method for {@link BerTlv#readTag(ByteBuffer)}. */
  @Test
  void test_500_readTag__ByteBuffer() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. read valid tags from ByteBuffer
    // --- c. ERROR: not all bytes of tag-field available

    // --- a. smoke test
    {
      final var expected = "3f8182838485868788898a8b8c7e"; // tag with 14 octets
      final var expOctets = Hex.toByteArray(expected);
      final var input = ByteBuffer.wrap(expOctets);
      assertEquals(0, input.position());
      assertEquals(expOctets.length, input.limit());

      final var present = Hex.toHexDigits(BerTlv.readTag(input));

      assertEquals(expected, present);
      assertEquals(expOctets.length, input.position());
      assertFalse(input.hasRemaining());
    } // end --- a.

    // --- b. read valid tags from ByteBuffer
    VALID_TAGS.forEach(
        expected -> {
          final var input = ByteBuffer.wrap(expected);
          assertEquals(0, input.position());
          assertEquals(expected.length, input.limit());

          final var actual = BerTlv.readTag(input);

          assertArrayEquals(expected, actual);
          assertFalse(input.hasRemaining());

          // --- c. ERROR: not all bytes of tag-field available
          input.clear().limit(input.capacity() - 1);
          assertEquals(0, input.position());

          assertThrows(BufferUnderflowException.class, () -> BerTlv.readTag(input));

          assertFalse(input.hasRemaining());
        }); // end forEach(expected -> ...)
    // end --- b, c.
  } // end method */

  /** Test method for {@link BerTlv#readTag(InputStream)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_510_readTag__InputStream() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. read valid tags from ByteArrayInputStream
    // --- c. ERROR: not all bytes of tag-field available from ByteArrayInputStream
    // --- d. ERROR: not all bytes of tag-field available from File
    // --- e. read from PipedInputStream

    final Path path = claTempDir.resolve("test_510_readTag__InputStream.bin");

    try (var po = new PipedOutputStream();
        var pi = new PipedInputStream(po, 1)) {
      final var queue = new LinkedBlockingQueue<byte[]>();

      final var thread =
          new Thread(
              () -> {
                try {
                  for (; ; ) {
                    final var input = queue.take();

                    if (0 == input.length) {
                      break;
                    } // end fi

                    AfiUtils.chill(10);

                    for (final var b : input) {
                      po.write(b);
                      po.flush();
                      AfiUtils.chill(1);
                    } // end For (b...)
                  } // end forever-loop
                } catch (IOException | InterruptedException e) {
                  LOGGER.atError().log(UNEXPECTED, e);
                } // end Catch (...)
              });
      thread.start();

      // --- a. smoke test
      {
        final var expected = "3f8182838485868788898a8b8c7f"; // tag with 14 octets
        final var input = new ByteArrayInputStream(Hex.toByteArray(expected));

        final var present = Hex.toHexDigits(BerTlv.readTag(input));

        assertEquals(expected, present);
      } // end --- a.

      for (final var expected : VALID_TAGS) {
        // --- b. read valid tags from ByteArrayInputStream
        {
          final var is = new ByteArrayInputStream(expected);

          final byte[] present = BerTlv.readTag(is);

          assertNotSame(expected, present);
          assertArrayEquals(expected, present);
        } // end --- b.

        final var prefix = Arrays.copyOf(expected, expected.length - 1);

        // --- c. ERROR: not all bytes of tag-field available from ByteArrayInputStream
        {
          final var is = new ByteArrayInputStream(prefix);

          assertThrows(EOFException.class, () -> BerTlv.readTag(is));
        } // end --- c.

        // --- d. ERROR: not all bytes of tag-field available from File
        if (RNG.nextDouble() < 0.1) { // NOPMD literal in if statement, test only 10% of values
          Files.write(path, prefix);
          try (InputStream inputStream = Files.newInputStream(path)) {
            assertThrows(EOFException.class, () -> BerTlv.readTag(inputStream));
          } // end try-with-resources, RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE
        } // end fi
        // end --- d.

        // --- e. read from PipedInputStream
        {
          queue.put(expected);

          final byte[] present = BerTlv.readTag(pi);

          assertNotSame(expected, present);
          assertArrayEquals(expected, present);
        } // end --- e.
      } // end For (expected...)

      queue.put(EMPTY_OS);
      thread.join();
    } catch (IOException | InterruptedException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link BerTlv#tagLength2String(String, String, int)}. */
  @Test
  void test_520_tagLength2String__String_String_int() {
    // Note 1: The method-under-test is used only for toString()-conversions which is mostly to
    //         inform a user about the content of TLV-objects. Thus, we can be (a bit) lazy here
    //         and do some smoke-tests for good test-coverage.

    // Test strategy:
    // All combinations of
    // 1. delimiter = {"",  "y"},
    // 2. delo      = {"", "\n", "y"}
    // 3. noI       = [ 0,   1,   2}
    // --- a. delimiter = "",  delo = "",   noI=0
    // --- b. delimiter = "",  delo = "",   noI=1
    // --- c. delimiter = "",  delo = "",   noI=2
    // --- d. delimiter = "",  delo = "\n", noI=0
    // --- e. delimiter = "",  delo = "\n", noI=1
    // --- f. delimiter = "",  delo = "\n", noI=2
    // --- g. delimiter = "",  delo = "y",  noI=0
    // --- h. delimiter = "",  delo = "y",  noI=1
    // --- i. delimiter = "",  delo = "y",  noI=2
    // --- j. delimiter = "x", delo = "",   noI=0
    // --- k. delimiter = "x", delo = "",   noI=1
    // --- l. delimiter = "x", delo = "",   noI=2
    // --- m. delimiter = "x", delo = "\n", noI=0
    // --- n. delimiter = "x", delo = "\n", noI=1
    // --- o. delimiter = "x", delo = "\n", noI=2
    // --- p. delimiter = "x", delo = "y",  noI=0
    // --- q. delimiter = "x", delo = "y",  noI=1
    // --- r. delimiter = "x", delo = "y",  noI=2

    // --- define Device-Under-Test
    final var dut = new MyBerTlv(0x41, 5);
    String delimiter;
    String delo;
    String exp;

    // --- a. delimiter = "",  delo = "",   noI=0
    delimiter = "";
    delo = "";
    exp = "4105";
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 0).toString());

    // --- b. delimiter = "",  delo = "",   noI=1
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 1).toString());

    // --- c. delimiter = "",  delo = "",   noI=2
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 2).toString());

    // --- d. delimiter = "",  delo = "\n", noI=0
    delo = "\n";
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 0).toString());

    // --- e. delimiter = "",  delo = "\n", noI=1
    assertEquals("g  " + exp, dut.tagLength2String(delimiter, delo, 1).toString());

    // --- f. delimiter = "",  delo = "\n", noI=2
    assertEquals("h  h  " + exp, dut.tagLength2String(delimiter, delo, 2).toString());

    // --- g. delimiter = "",  delo = "y",  noI=0
    delo = "y";
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 0).toString());

    // --- h. delimiter = "",  delo = "y",  noI=1
    assertEquals("y" + exp, dut.tagLength2String(delimiter, delo, 1).toString());

    // --- i. delimiter = "",  delo = "y",  noI=2
    assertEquals("yy" + exp, dut.tagLength2String(delimiter, delo, 2).toString());

    // --- j. delimiter = "x", delo = "",   noI=0
    delimiter = "x";
    delo = "";
    exp = "41x05";
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 0).toString());

    // --- k. delimiter = "x", delo = "",   noI=1
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 1).toString());

    // --- l. delimiter = "x", delo = "",   noI=2
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 2).toString());

    // --- m. delimiter = "x", delo = "\n", noI=0
    delo = "\n";
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 0).toString());

    // --- n. delimiter = "x", delo = "\n", noI=1
    assertEquals("g  " + exp, dut.tagLength2String(delimiter, delo, 1).toString());

    // --- o. delimiter = "x", delo = "\n", noI=2
    assertEquals("h  h  " + exp, dut.tagLength2String(delimiter, delo, 2).toString());

    // --- p. delimiter = "x", delo = "y",  noI=0
    delo = "y";
    assertEquals(exp, dut.tagLength2String(delimiter, delo, 0).toString());

    // --- q. delimiter = "x", delo = "y",  noI=1
    assertEquals("y" + exp, dut.tagLength2String(delimiter, delo, 1).toString());

    // --- r. delimiter = "x", delo = "y",  noI=2
    assertEquals("yy" + exp, dut.tagLength2String(delimiter, delo, 2).toString());
  } // end method */

  /** Test method for {@link BerTlv#toString()}. */
  @Test
  void test_530_toString() {
    // Assertions:
    // ... a. getEncoded()-method works as expected

    // Note 1: Because of assertion_a we can be lazy here and do some smoke tests.
    // Note 2: Tests on randomly generated TLV objects are performed TODO

    // Test strategy:
    // --- a. smoke test
    // --- b. some manual tests

    // --- a. smoke test
    {
      final var expected = Hex.extractHexDigits("83-03-afa5de");
      final var dut = BerTlv.getInstance(expected);

      final var present = dut.toString();

      assertEquals(expected, present);
    } // end --- a.

    // --- b. some manual tests
    Stream.of(
            "88 04 81828384", // primitive DO
            "a1 03  87 01 99" // constructed DO
            )
        .forEach(
            input ->
                assertEquals(
                    Hex.extractHexDigits(input),
                    BerTlv.getInstance(Hex.toByteArray(input))
                        .toString())); // end forEach(input -> ...)
  } // end method */

  /** Test method for {@link BerTlv#toString(String)}. */
  @Test
  void test_540_toString__String() {
    // Assertions:
    // ... a. toString(String, String, int)-method works as expected

    // Note 1: Because of assertion_a we can be lazy here and do some smoke tests.
    // Note 2: Tests on randomly generated TLV objects are performed in test method
    //         TODO

    // Test strategy:
    // --- a. smoke test
    // --- b. some manual tests

    // --- a. smoke test
    {
      final var expected = "83-03-afa5de";
      final var dut = BerTlv.getInstance(expected);

      final var present = dut.toString("-");

      assertEquals(expected, present);
    } // end --- a.

    // --- b. some manual tests
    Stream.of(
            "88.04.81828384", // primitive DO
            "a1.03..87.01.99" // constructed DO
            )
        .forEach(
            input ->
                assertEquals(
                    input,
                    BerTlv.getInstance(Hex.toByteArray(input))
                        .toString("."))); // end forEach(input -> ...)
  } // end method */

  /** Test method for {@link BerTlv#toString(String, String, int, boolean)}. */
  @Test
  void test_550_toString__String_String_int_boolean() {
    // Assertions:
    // - none -

    // Note: This abstract method is sufficiently tested in subclasses.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with subclass PrimitiveBerTlv
    // --- b. smoke test with subclass ConstructedBerTlv

    final var delo = " ";
    final var delimiter = "|  ";
    final var noIndentation = 2;

    // --- a. smoke test with subclass PrimitiveBerTlv
    {
      final var dut = BerTlv.getInstance("02 01 03");
      final var expected = String.format("%s02%s01%s03", delo + delo, delimiter, delimiter);

      final var actual = dut.toString(delimiter, delo, noIndentation, false);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. smoke test with subclass ConstructedBerTlv
    {
      final var dut = BerTlv.getInstance("30 06 [(02 01 12) (04 01 ab)]");
      final var expected =
          String.format(
              "%s30%s06%n%s02%s01%s12%n%s04%s01%sab",
              delo + delo,
              delimiter,
              delo + delo + delo,
              delimiter,
              delimiter,
              delo + delo + delo,
              delimiter,
              delimiter);

      final var actual = dut.toString(delimiter, delo, noIndentation, false);

      assertEquals(expected, actual);
    } // end --- b.
  } // end method */

  /**
   * Method checks whether given objects are not "same".
   *
   * @param dutA first object
   * @param dutB second object
   * @param doRecursive true => for {@link ConstructedBerTlv} check children recursively
   */
  private void zzzNotSame(final BerTlv dutA, final BerTlv dutB, final boolean doRecursive) {
    // Assertions:
    // ... a. dutA and dutB are "equal"

    assertNotSame(dutA, dutB, dutA::toStringTree); // objects not same
    assertNotSame(dutA.insTagLengthField, dutB.insTagLengthField); // tag- and length-field not same

    if (dutA instanceof PrimitiveBerTlv) {
      // ... PrimitiveBerTlv
      assertNotSame(((PrimitiveBerTlv) dutA).insValueField, ((PrimitiveBerTlv) dutB).insValueField);
    } else {
      // ... not PrimitiveBerTlv
      //     => ConstructedBerTlv

      final var listA = ((ConstructedBerTlv) dutA).insValueField;
      final var listB = ((ConstructedBerTlv) dutB).insValueField;

      if (doRecursive && !listA.isEmpty()) {
        // ... recursively check children for non-empty lists
        assertNotSame(listA, listB); // value-field not same
        for (int i = listA.size(); i-- > 0; ) { // NOPMD assignment
          zzzNotSame(listA.get(i), listB.get(i), true);
        } // end For (i...)
      } // end fi (doRecursive)

      // Note 1: If doRecursive is false, i.e. no recursion occurs to check for "NotSame", then
      //         no further checks are possible. Reasons:
      //         Objects dutA and dutB are equal, thus, all children are equal. It is possible
      //         that children are same, but that is not guaranteed. For further information,
      //         see javaDoc on java.util.List, section Unmodifiable Lists-

    } // end else (constructed?)
  } // end method */

  /** Test method for {@link BerTlv#toStringTree()}. */
  @Test
  void test_560_toStringTree() {
    // Note 1: Here no real test happens, because the method-under-test is based
    //         on an abstract method. Thus, this method is tested in subclasses.
    // Note 2: Here we concentrate on good code-coverage.

    // Test strategy:
    // --- a. smoke test with helper class
    assertEquals(
        "method not implemented: "
            + "delimiter= , "
            + "delo=|  , "
            + "noIndentation=0, "
            + "addComment=true",
        new MyBerTlv(0x80, 0).toStringTree());
  } // end method */

  // ###########################################################################
  // ###############                manual tests                 ###############
  // ###########################################################################

  /**
   * Test method for {@link BerTlv#getInstance(InputStream)}.
   *
   * <p>Here only {@link PrimitiveBerTlv} objects are tested.
   */
  @EnabledIf("de.gematik.smartcards.tlv.TestBerTlv#isManualTest")
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.NcssCount",
    "PMD.NPathComplexity"
  })
  @Test
  void test_700_PrimitiveBerTlv_getInstance__InputStream() {
    // Assertions:
    // ... a. all automatic tests pass

    // Test strategy:
    // --- a. check all one byte, primitive UNIVERSAL tags
    // --- b. check non-universal class promitive tags

    final var len1 = RNG.intsClosed(0x0000, 0x007f, 5).boxed().toList();
    final var len2 = RNG.intsClosed(0x007f, 0x00ff, 5).boxed().toList();
    final var len3 = RNG.intsClosed(0x0100, 0xffff, 5).boxed().toList();
    final var len4 = RNG.intsClosed(0x01_0000, 0xff_ffff, 5).boxed().toList();
    final var lengths =
        Stream.of(len1, len2, len3, len4).flatMap(Collection::stream).distinct().sorted().toList();

    try (var po = new PipedOutputStream();
        var pi = new PipedInputStream(po, 1)) {
      final var queue = new LinkedBlockingQueue<byte[]>();

      final var thread =
          new Thread(
              () -> {
                try {
                  for (; ; ) {
                    final var input = queue.take();

                    if (0 == input.length) {
                      break;
                    } // end fi

                    AfiUtils.chill(10);

                    for (final var b : input) {
                      po.write(b);
                      po.flush();
                      AfiUtils.chill(1);
                    } // end For (b...)
                  } // end forever-loop
                } catch (IOException | InterruptedException e) {
                  LOGGER.atError().log("thread 800", e);
                } // end Catch (...)
              });
      thread.start();

      // --- a. check all one byte UNIVERSAL tags
      for (final var num : IntStream.range(0x00, 0x1f).toArray()) {
        LOGGER.atInfo().log("tag='{}'", String.format("%02x", num));
        final var tagField = BerTlv.createTag(UNIVERSAL, false, num);
        final var tag = BerTlv.convertTag(tagField);

        for (final var length : lengths) {
          final var expected = TestPrimitiveBerTlv.createRandom(tag, length);
          assertEquals(length.longValue(), expected.getLengthOfValueField());

          final var biLength = BigInteger.valueOf(length);
          final var valueField = expected.getValueField();
          final var lengthFields = new HashSet<String>();

          final var lengthField0 = BerTlv.getLengthField(length);
          lengthFields.add(lengthField0);

          final var lengthField1 = AfiBigInteger.toHex(biLength, lengthField0.length() / 2);
          lengthFields.add(String.format("%02x%s", 0x80 + lengthField1.length() / 2, lengthField1));

          final var lengthField2 = AfiBigInteger.toHex(biLength, lengthField0.length() / 2 + 1);
          lengthFields.add(String.format("%02x%s", 0x80 + lengthField2.length() / 2, lengthField2));

          final var lengthFieldMax = AfiBigInteger.toHex(biLength, 127);
          lengthFields.add(String.format("ff%s", lengthFieldMax));

          for (final var lf : lengthFields) {
            final var encoded = AfiUtils.concatenate(tagField, Hex.toByteArray(lf), valueField);
            final var inputStream = new ByteArrayInputStream(encoded);

            final var present = BerTlv.getInstance(inputStream);

            assertEquals(expected, present);
            if (encoded.length < 1024) { // NOPMD literals in if statement
              queue.put(encoded);

              final var present2 = BerTlv.getInstance(pi);

              assertEquals(expected, present2);
            } // end fi
          } // end For (lf...)
        } // end For (length...)
      } // end For (num...)
      // end --- a.

      // --- b. check non-universal class tags
      for (final var length : lengths) {
        final var biLength = BigInteger.valueOf(length);
        final var valueField = RNG.nextBytes(length);
        final var lengthFields = new TreeSet<String>();

        final var lengthField0 = BerTlv.getLengthField(length);
        lengthFields.add(lengthField0);

        final var lengthField1 = AfiBigInteger.toHex(biLength, lengthField0.length() / 2);
        lengthFields.add(String.format("%02x%s", 0x80 + lengthField1.length() / 2, lengthField1));

        final var lengthField2 = AfiBigInteger.toHex(biLength, lengthField0.length() / 2 + 1);
        lengthFields.add(String.format("%02x%s", 0x80 + lengthField2.length() / 2, lengthField2));

        final var lengthFieldMax = AfiBigInteger.toHex(biLength, 127);
        lengthFields.add(String.format("ff%s", lengthFieldMax));

        for (final var lf : lengthFields) {
          LOGGER.atInfo().log("lengthField='{}'", lf);
          final var lv = AfiUtils.concatenate(Hex.toByteArray(lf), valueField);

          for (final var clazz : List.of(APPLICATION, CONTEXT_SPECIFIC, PRIVATE)) {
            for (final var num : RNG.intsClosed(0, 0x100, 10).toArray()) {
              final var tagField = BerTlv.createTag(clazz, false, num);
              final var expected = new PrimitiveBerTlv(tagField, ByteBuffer.wrap(lv));
              final var encoded = AfiUtils.concatenate(tagField, lv);
              final var inputStream = new ByteArrayInputStream(encoded);
              LOGGER.atTrace().log("expected: {}", expected.toString(" "));

              final var present = BerTlv.getInstance(inputStream);

              assertEquals(expected, present);
              if (encoded.length < 1024) { // NOPMD literals in if statement
                queue.put(encoded);

                final var present2 = BerTlv.getInstance(pi);

                assertEquals(expected, present2);
              } // end fi
            } // end For (num...)
          } // end For (clazz...)
        } // end For (lf...)
      } // end For (length...)
      // end --- b.

      queue.put(EMPTY_OS);
      thread.join();
    } catch (IOException | InterruptedException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link BerTlv#getInstance(InputStream)}.
   *
   * <p>Here only {@link ConstructedBerTlv} objects are tested.
   */
  @EnabledIf("de.gematik.smartcards.tlv.TestBerTlv#isManualTest")
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_800_ConstructedBerTlv_getInstance__InputStream() {
    // Assertions:
    // ... a. all automatic tests pass
    // ... b. manual test "test_800_PrimitiveBerTlv_getInstance__InputStream()" passes

    // Test strategy:
    // --- a. create various constructed TLV objects randomly

    final var path = Paths.get("build").resolve("log").toAbsolutePath().normalize();
    assertTrue(Files.isDirectory(path), path::toString);

    final var maxCounter = 1_000;
    final var maxNoChildren = 10;
    final var countDown = 8;

    final var tlv = new byte[3][];

    boolean success = true;
    try (var poSmall = new PipedOutputStream();
        var piSmall = new PipedInputStream(poSmall, 1);
        var poBig = new PipedOutputStream();
        var piBig = new PipedInputStream(poBig, 0x1_0000)) {
      final var queue = new LinkedBlockingQueue<byte[]>();

      final var thread =
          new Thread(
              () -> {
                try {
                  for (; ; ) {
                    final var input = queue.take();

                    if (0 == input.length) {
                      break;
                    } // end fi

                    AfiUtils.chill(100);
                    for (final var b : input) {
                      poSmall.write(b);
                      poSmall.flush();
                    } // end For (b...)

                    poBig.write(input);
                    poBig.flush();
                  } // end forever-loop
                } catch (IOException | InterruptedException e) {
                  LOGGER.atError().log("thread 800", e);
                } // end Catch (...)
              });
      thread.start();

      int fileNamePrefix = -1;
      for (int counter = maxCounter; counter-- > 0; ) { // NOPMD assignment in operand
        if (0 == (counter % 100)) {
          LOGGER.atInfo().log("counter = {}", counter);
        } // end fi
        final var probabilityIndefiniteForm = 1.0 - (1.0 * counter) / maxCounter;
        final var expected =
            TestConstructedBerTlv.createRandom(
                maxNoChildren, probabilityIndefiniteForm, countDown, tlv);
        final var encoded = AfiUtils.concatenate(tlv);

        // --- test with small size of the pipe
        queue.put(encoded);

        final var presentSmall = BerTlv.getInstance(piSmall);

        if (!expected.equals(presentSmall)) {
          final var prefix = String.format("%04d.", ++fileNamePrefix);
          Files.writeString(path.resolve(prefix + "1"), expected.toStringTree(), UTF_8);
          Files.writeString(path.resolve(prefix + "2"), presentSmall.toStringTree(), UTF_8);
          Files.writeString(path.resolve(prefix + "o"), Hex.toHexDigits(encoded), UTF_8);
          LOGGER.atWarn().log("mismatch small: {}", fileNamePrefix);

          success = false;
        } // end fi

        // --- test with big size of the pipe
        final var presentBig = BerTlv.getInstance(piBig);

        if (!expected.equals(presentBig)) {
          final var prefix = String.format("%04d.", ++fileNamePrefix);
          Files.writeString(path.resolve(prefix + "1"), expected.toStringTree(), UTF_8);
          Files.writeString(path.resolve(prefix + "2"), presentBig.toStringTree(), UTF_8);
          Files.writeString(path.resolve(prefix + "o"), Hex.toHexDigits(encoded), UTF_8);
          LOGGER.atWarn().log("mismatch big: {}", fileNamePrefix);

          success = false;
        } // end fi
      } // end forever-loop

      queue.put(EMPTY_OS);
      thread.join();
    } catch (IOException | InterruptedException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    assertTrue(success, "success");
  } // end method */

  /**
   * Test method for random {@link BerTlv} objects.
   *
   * <p>Here, the following methods are tested:
   *
   * <ol>
   *   <li>{@link BerTlv#getInstance(ByteBuffer)}
   *   <li>{@link BerTlv#getInstance(InputStream)}
   * </ol>
   */
  @EnabledIf("de.gematik.smartcards.tlv.TestBerTlv#isManualTest")
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
  @Test
  void test_900_randomTlv() {
    // Assertions:
    // ... a. all automatic tests pass

    // Test strategy:
    // --- a. create a couple of random TLV-objects
    // --- b. create from byte[]
    // --- c. write and read through a pipe
    // --- d. write and read with ByteBuffer

    final var infimumTotalLength = 1_000_000;
    final var maximumLengthPrimitive = 512;
    final var expected = new ArrayList<BerTlv>();
    final var encoded = new ArrayList<byte[]>();
    int maxLength = Integer.MIN_VALUE;
    int totalLength = 0;

    // --- a. create a couple of random TLV-objects
    {
      final var tagLengthValue = new byte[3][];
      for (; ; ) {
        final BerTlv tlv;
        if (RNG.nextDouble() < 0.5) { // NOPMD literal in if statement
          // ... 50% chance for a primitive TLV object
          tlv = TestPrimitiveBerTlv.createRandom(false, maximumLengthPrimitive, tagLengthValue);
        } else {
          // ... 50% chance for a constructed TLV object
          tlv = TestConstructedBerTlv.createRandom(10, 0.5, 8, tagLengthValue);
        } // end else
        final var octets = AfiUtils.concatenate(tagLengthValue);
        expected.add(tlv);
        encoded.add(octets);
        maxLength = Math.max(maxLength, octets.length);
        totalLength += octets.length;

        if ((expected.size() > 20) && (totalLength > infimumTotalLength)) {
          LOGGER.atInfo().log(
              "size = {}, maxLength = {}, totalLength = {}",
              expected.size(),
              maxLength,
              totalLength);

          break;
        } // end fi
      } // end forever-loop
    } // end --- a.

    final var input = AfiUtils.concatenate(encoded);

    // --- b. create from byte[]
    for (int i = expected.size(); i-- > 0; ) { // NOPMD assignment in operand
      final var present = BerTlv.getInstance(encoded.get(i));

      assertEquals(expected.get(i), present);
    } // end --- b.

    // --- c. write and read through a pipe
    try (var po = new PipedOutputStream();
        var pi = new PipedInputStream(po, 1024)) {

      new Thread(
              () -> {
                try {
                  po.write(input);
                  po.flush();
                } catch (IOException e) {
                  LOGGER.atError().log("thread 900", e);
                } // end Catch (...)
              })
          .start();

      for (final var exp : expected) {
        final var present = BerTlv.getInstance(pi);

        assertEquals(exp, present);
      } // end forever-loop
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- c.

    // --- d. write and read with ByteBuffer
    {
      final var chunkSize = 1_000;
      final var capacity = maxLength + chunkSize;
      final var buffer = ByteBuffer.allocate(capacity);
      final var present = new ArrayList<BerTlv>();

      for (int offset = 0; offset < input.length; ) {
        final var length = Math.min(chunkSize, input.length - offset);
        // Assertions: buffer is prepared for a write-operation
        LOGGER.atTrace().log("+: p={}, l={}, c={}", buffer.position(), length, buffer.capacity());
        buffer.put(input, offset, length);
        offset += length; // NOPMD reassignment of loop control variable

        try {
          for (; ; ) {
            final var pre = BerTlv.getInstance(buffer.flip()); // prepare and read

            present.add(pre);
            LOGGER.atInfo().log("-: {}", expected.size() - present.size());
            buffer.compact(); // prepare buffer for the next write-operation
          } // end forever-loop
        } catch (BufferUnderflowException e) {
          buffer.position(buffer.limit()).limit(capacity); // prepare the next write-operation
        } // end Catch (...)
      } // end For (i...)

      assertEquals(expected, present);
    } // end --- d.
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /** Class which could be instantiated and used for test purposes. */
  // Note 1: Spotbugs claims "CT_CONSTRUCTOR_THROW"
  //         Short message: Classes that throw exceptions in their constructors
  //             are vulnerable to Finalizer attacks.
  //         That finding is not correct, because an empty finalize() declared
  //             "final" is present in superclass.
  @edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
    "CT_CONSTRUCTOR_THROW" // see note 1
  }) // */
  /* package */ static class MyBerTlv extends BerTlv {

    /**
     * Tag constructor, length of value-field set to zero.
     *
     * @param tag of BER-TLV object, octet string representation
     */
    /* package */ MyBerTlv(final byte[] tag) {
      this(convertTag(tag), 0);
    } // end constructor */

    /**
     * Tag-Length constructor.
     *
     * @param tag of BER-TLV object, integer representation
     * @param len length of value-field
     */
    /* package */ MyBerTlv(final long tag, final long len) {
      super(tag, len);
      insLengthOfValueField = len;
    } // end constructor */

    /**
     * Comfort constructor.
     *
     * @param tag the tag-field
     * @param buffer input stream form which just the tag is read
     */
    /* package */ MyBerTlv(final byte[] tag, final ByteBuffer buffer) {
      super(tag, buffer);
      // Note: Intentionally here instance attribute insLengthOfValueField is NOT set.
    } // end constructor */

    /**
     * Comfort constructor.
     *
     * @param tag the tag-field
     * @param bais input stream form which just the tag is read
     */
    /* package */ MyBerTlv(final byte[] tag, final ByteArrayInputStream bais) throws IOException {
      // CT_CONSTRUCTOR_THROW
      super(tag, bais);
      // Note: Intentionally here instance attribute insLengthOfValueField is NOT set.
    } // end constructor */

    @Override
    public byte[] getValueField() {
      throw new IllegalCallerException("method not implemented");
    } // end inner method */

    @Override
    public byte[] getEncoded() {
      return insTagLengthField.clone(); // defensive cloning, better safe than sorry :-)
    } // end inner method */

    /* package */
    @Override
    String toString(
        final String delimiter,
        final String delo,
        final int noIndentation,
        final boolean addComment) {
      return String.format(
          "method not implemented: delimiter=%s, delo=%s, noIndentation=%d, addComment=%s",
          delimiter, delo, noIndentation, addComment);
    } // end inner method */
  } // end inner class

  /**
   * Returns a random {@link ClassOfTag}.
   *
   * @return random {@link ClassOfTag}
   */
  /* package */
  static ClassOfTag randomClassOfTag() {
    final var rndClass = RNG.nextDouble();
    if (rndClass < 0.5) { // NOPMD literals in if statement
      // ... 50% UNIVERSAL
      return UNIVERSAL;
    } else if (rndClass < 0.7) { // NOPMD literals in if statement
      // ... 20% APPLICATION
      return APPLICATION;
    } else if (rndClass < 0.9) { // NOPMD literals in if statement
      // ... 20% CONTEXT SPECIFIC
      return CONTEXT_SPECIFIC;
    } else {
      // ... 10% PRIVATE
      return PRIVATE;
    } // end else
  } // end method */

  /**
   * Create a random tag-number
   *
   * <p>If {@code clazz} equals {@link ClassOfTag#UNIVERSAL} and {@code isConstructed = TRUE} then
   * tag-number is 0x10 = '10' = 16.
   *
   * <p>If {@code clazz} equals {@link ClassOfTag#UNIVERSAL} and {@code isConstructed = FALSE} and
   * {@code isIndefiniteForm = TRUE} then tag-number is randomly chosen from range [1, 31].
   *
   * <p>If {@code clazz} equals {@link ClassOfTag#UNIVERSAL} and {@code isConstructed = FALSE} and
   * {@code isIndefiniteForm = FALSE} then tag-number is randomly chosen from range [0, 31].
   *
   * <p>In all other cases, the tag-number is randomly chosen from range [0, 65636] favoring small
   * numbers.
   *
   * @param clazz class of the returned tag
   * @param isIndefiniteForm {@code TRUE} if the tag is used in a template where the enclosed {@link
   *     ConstructedBerTlv} uses the indefinite form, {@code FALSE} otherwise
   * @param isConstructed {@code TRUE} if the to be created tag is constructed
   * @return tag-number
   */
  /* package */
  static long randomTagNumber(
      final ClassOfTag clazz, final boolean isIndefiniteForm, final boolean isConstructed) {
    if (UNIVERSAL.equals(clazz)) {
      // Note: In case the indefinite form is used, EndOfContent
      //       cannot be used as a random child.
      final var minTagNumber = isIndefiniteForm ? 1 : 0;
      return isConstructed ? 0x10 : RNG.nextIntClosed(minTagNumber, 31);
    } else {
      return Math.round(0x1_0000 * Math.pow(RNG.nextDouble(), 4));
    } // end else
  } // end method */

  /**
   * Manipulates the length of the length-field.
   *
   * @param length of value-field
   * @return corresponding length-field, but not necessarily the shortest possible length-field
   */
  /* package */
  static byte[] randomLengthField(final long length) {
    final var supremum = 128;
    final var lf0 = Hex.toByteArray(BerTlv.getLengthField(length));

    if (RNG.nextDouble() < 0.25) { // NOPMD literals in if statement
      // ... 25% chance to use the shortest possible length-field
      return lf0;
    } else {
      // ... use a length-field which is longer than necessary
      final var lf1 = AfiBigInteger.i2os(BigInteger.valueOf(length));
      final var lf2 = new byte[Math.max(lf0.length, RNG.nextIntClosed(2, supremum))];
      lf2[0] = (byte) (0x80 + lf2.length - 1);
      final var offset = lf2.length - lf1.length;
      System.arraycopy(lf1, 0, lf2, offset, lf1.length);

      return lf2;
    } // end else
  } // end method */

  @Test
  void test_randomLengthField__long() {
    // Assertions:
    // ... a. "readLength(ByteBuffer)"-method works as expected

    // Test strategy:
    // --- a. all one-octet length-fields
    // --- b. all two-octet length-fields
    // --- c. random integer values
    // --- d. random long values

    // --- a. all one-octet length-fields
    // --- b. all two-octet length-fields
    for (final var expected : IntStream.rangeClosed(0, 0xff).toArray()) {
      final var randomLengthField = randomLengthField(expected);
      final var buffer = ByteBuffer.wrap(randomLengthField);

      final var present = BerTlv.readLength(buffer);

      assertEquals(expected, present[0]);
    } // end For (expected...)
    // end --- a, b.

    // --- c. random integer values
    for (final var expected : RNG.intsClosed(0x100, Integer.MAX_VALUE, 1024).toArray()) {
      final var randomLengthField = randomLengthField(expected);
      final var buffer = ByteBuffer.wrap(randomLengthField);

      final var present = BerTlv.readLength(buffer);

      assertEquals(expected, present[0]);
    } // end For (expected...)
    // end --- c.

    // --- d. random long values
    for (int i = 1024; i-- > 0; ) { // NOPMD assignment in operands
      long expected;
      do {
        expected = RNG.nextLong();
      } while (expected < 0);
      final var randomLengthField = randomLengthField(expected);
      final var buffer = ByteBuffer.wrap(randomLengthField);

      final var present = BerTlv.readLength(buffer);

      assertEquals(expected, present[0]);
    } // end For (expected...)
  } // end method */
} // end class
