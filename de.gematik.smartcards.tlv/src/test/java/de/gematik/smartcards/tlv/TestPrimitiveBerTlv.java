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

import static de.gematik.smartcards.tlv.PrimitiveBerTlv.EMC;
import static de.gematik.smartcards.tlv.PrimitiveBerTlv.EM_EOS;
import static de.gematik.smartcards.tlv.PrimitiveBerTlv.EM_INDEFINITE;
import static de.gematik.smartcards.tlv.PrimitiveBerTlv.EM_TOO_LONG;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static de.gematik.smartcards.utils.AfiUtils.VALUES_BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
 * Class performing white-box tests on {@link BerTlv}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.
//         Non-null field is not initialized
//         This finding is for a class attribute which is initialized by JUnit.
// Note 2: Spotbugs claims "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", i.e.
//         Redundant nullcheck of value known to be non-null
//         This happens at the end of a try-with-resources structure.
//         This seems to be a false positive, because the code dose not contain a null-check.
//         Probably the container adds something here and a null-check appears in the bytey-code.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 1
  "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE" // see note 2
}) // */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestPrimitiveBerTlv {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

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

  /** Test method for {@link PrimitiveBerTlv#PrimitiveBerTlv(byte[], ByteBuffer)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity"})
  @Test
  void test_PrimitiveBerTlv__byteA_ByteBuffer() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. Loop over the relevant set of tags
    // --- b. Loop over various values for lengthOfValueField
    // --- c. Loop over various encodings for lengthField
    // --- d. Various amounts of extra byte after a valid primitive TLV object
    // --- e. ERROR: indefinite form of length-field
    // --- f. ERROR: lengthOfValueField too big
    // --- g. ERROR: not all bytes of value-field available

    // --- a. Loop over the relevant set of tags
    for (final var tag : TestBerTlv.VALID_TAGS) {
      for (final var len : RNG.intsClosed(0, 512, 10).toArray()) {
        // --- b. Loop over various values for lengthOfValueField
        final byte[] lengthField = Hex.toByteArray(BerTlv.getLengthField(len));
        final byte[] valueField = RNG.nextBytes(len);
        final String length = String.format("%04x", len);

        // --- c. Loop over various encodings for lengthField
        final var lenFields =
            List.of(
                lengthField, // optimal encoding, i.e., as short as possible
                Hex.toByteArray("83-00" + length), // length-field on 4 bytes
                Hex.toByteArray("85-000000" + length), // length-field on 6 bytes
                Hex.toByteArray("8c-00000000000000000000" + length) // length-field on 13 bytes
                );
        for (final var lenField : lenFields) {
          // --- d. Various amounts of extra byte after a valid primitive TLV object
          for (final var extra : RNG.intsClosed(0, 100, 10).toArray()) {
            final byte[] suffix = RNG.nextBytes(extra);
            final byte[] input = AfiUtils.concatenate(lenField, valueField, suffix);
            final var buffer = ByteBuffer.wrap(input);

            if (tag.length > BerTlv.NO_TAG_FIELD) {
              // ... tag too long
              final var e =
                  assertThrows(
                      IllegalArgumentException.class, () -> new PrimitiveBerTlv(tag, buffer));

              assertEquals("tag too long for this implementation", e.getMessage());
              assertNull(e.getCause());
            } else if (0x20 == (tag[0] & 0x20)) { // NOPMD literal
              // ... constructed encoding
              final var e =
                  assertThrows(
                      IllegalArgumentException.class, () -> new PrimitiveBerTlv(tag, buffer));

              assertEquals(EMC, e.getMessage());
              assertNull(e.getCause());
            } else {
              // ... valid primitive encoding
              final PrimitiveBerTlv dut = new PrimitiveBerTlv(tag, buffer);
              assertEquals(Hex.toHexDigits(tag), dut.getTagField());
              assertEquals(len, dut.getLengthOfValueField());
              assertArrayEquals(valueField, dut.getValueField());
              assertEquals(
                  dut.getTagField() + dut.getLengthField(), Hex.toHexDigits(dut.insTagLengthField));
            } // end else
          } // end For (extra...)
        } // end For (lenField...)
      } // end For (len...)

      if ((tag.length <= BerTlv.NO_TAG_FIELD) && (0 == (tag[0] & 0x20))) {
        // ... valid primitive encoding

        // --- e. ERROR: indefinite form of length-field
        {
          final var b = ByteBuffer.wrap(Hex.toByteArray("-80-12345678"));

          final var e =
              assertThrows(IllegalArgumentException.class, () -> new PrimitiveBerTlv(tag, b));

          assertEquals(EM_INDEFINITE, e.getMessage());
          assertNull(e.getCause());
        } // end --- e.

        // --- f. ERROR: lengthOfValueField too big
        {
          final var b = ByteBuffer.wrap(Hex.toByteArray("-8480000000-0102.."));

          final var e = assertThrows(ArithmeticException.class, () -> new PrimitiveBerTlv(tag, b));

          assertEquals(EM_TOO_LONG, e.getMessage());
          assertNull(e.getCause());
        } // end --- f.

        // --- g. ERROR: not all bytes of value-field available
        {
          final var b = ByteBuffer.wrap(Hex.toByteArray("-02-01"));

          assertThrows(BufferUnderflowException.class, () -> new PrimitiveBerTlv(tag, b));
        } // end --- g.
      } // end fi
    } // end For (tag...)
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#PrimitiveBerTlv(byte[], InputStream)}. */
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.CyclomaticComplexity"
  })
  @Test
  void test_PrimitiveBerTlv__byteA_InputStream() {
    // Assertions:
    // ... a. Super-constructor BerTlv(byte[], InputStream) works as expected
    // ... b. BerTlv.getLengthField(long) method works as expected

    // Test strategy:
    // --- a. Loop over the relevant set of tags
    // --- b. Loop over various values for lengthOfValueField
    // --- c. Loop over various encodings for lengthField
    // --- d. Various amounts of extra byte after a valid primitive TLV object
    // --- e. ERROR: indefinite form of length-field
    // --- f. ERROR: lengthOfValueField too big
    // --- g. ERROR: read from ByteArrayInputStream, not all bytes of value-field available
    // --- h. ERROR: read from file,                 not all bytes of value-field available

    // --- a. Loop over the relevant set of tags
    for (final var tag : TestBerTlv.VALID_TAGS) {
      for (final var len : RNG.intsClosed(0, 512, 10).toArray()) {
        // --- b. Loop over various values for lengthOfValueField
        final byte[] lengthField = Hex.toByteArray(BerTlv.getLengthField(len));
        final byte[] valueField = RNG.nextBytes(len);
        final String length = String.format("%04x", len);

        // --- c. Loop over various encodings for lengthField
        final var lenFields =
            List.of(
                lengthField, // optimal encoding, i.e., as short as possible
                Hex.toByteArray("83-00" + length), // length-field on 4 bytes
                Hex.toByteArray("85-000000" + length), // length-field on 6 bytes
                Hex.toByteArray("8c-00000000000000000000" + length) // length-field on 13 bytes
                );
        for (final var lenField : lenFields) {
          // --- d. Various amounts of extra byte after valid primitive TLV object
          for (final var extra : RNG.intsClosed(0, 100, 10).toArray()) {
            final byte[] suffix = RNG.nextBytes(extra);
            final byte[] input = AfiUtils.concatenate(lenField, valueField, suffix);
            final InputStream inputStream = new ByteArrayInputStream(input);

            if (tag.length > BerTlv.NO_TAG_FIELD) {
              // ... tag too long
              final Throwable thrown =
                  assertThrows(
                      IllegalArgumentException.class, () -> new PrimitiveBerTlv(tag, inputStream));
              assertEquals("tag too long for this implementation", thrown.getMessage());
              assertNull(thrown.getCause());
            } else if (0x20 == (tag[0] & 0x20)) { // NOPMD literal
              // ... constructed encoding
              final Throwable thrown =
                  assertThrows(
                      IllegalArgumentException.class, () -> new PrimitiveBerTlv(tag, inputStream));
              assertEquals(EMC, thrown.getMessage());
              assertNull(thrown.getCause());
            } else {
              // ... valid primitive encoding
              try {
                final PrimitiveBerTlv dut = new PrimitiveBerTlv(tag, inputStream);
                assertEquals(Hex.toHexDigits(tag), dut.getTagField());
                assertEquals(len, dut.getLengthOfValueField());
                assertArrayEquals(valueField, dut.getValueField());
                assertEquals(
                    dut.getTagField() + dut.getLengthField(),
                    Hex.toHexDigits(dut.insTagLengthField));
              } catch (IOException e) {
                fail(UNEXPECTED, e);
              } // end Catch (...)
            } // end else
          } // end For (extra...)
        } // end For (lenField...)
      } // end For (len...)

      if ((tag.length <= BerTlv.NO_TAG_FIELD) && (0 == (tag[0] & 0x20))) {
        // ... valid primitive encoding

        // --- e. ERROR: indefinite form of length-field
        {
          final var input = new ByteArrayInputStream(Hex.toByteArray("-80-12345678"));

          final Throwable thrownE =
              assertThrows(IllegalArgumentException.class, () -> new PrimitiveBerTlv(tag, input));

          assertEquals("indefinite form for length-field not allowed", thrownE.getMessage());
          assertNull(thrownE.getCause());
        } // end --- e.

        // --- f. ERROR: lengthOfValueField too big
        {
          final var is = new ByteArrayInputStream(Hex.toByteArray("-8480000000-0102.."));

          final var e = assertThrows(ArithmeticException.class, () -> new PrimitiveBerTlv(tag, is));

          assertEquals("length too big", e.getMessage());
          assertNull(e.getCause());
        } // end --- f.

        // --- g. ERROR: read from ByteArrayInputStream, not all bytes of value-field available
        {
          final var is = new ByteArrayInputStream(Hex.toByteArray("-02-01"));

          final var e = assertThrows(EOFException.class, () -> new PrimitiveBerTlv(tag, is));

          assertEquals(EM_EOS, e.getMessage());
          assertNull(e.getCause());
        } // end --- g.

        // --- h. ERROR: read from file,                 not all bytes of value-field available
        try {
          final Path path = claTempDir.resolve("input.bin");
          Files.write(path, Hex.toByteArray("-03- 0102"));
          try (var is = Files.newInputStream(path)) {
            final var e = assertThrows(EOFException.class, () -> new PrimitiveBerTlv(tag, is));

            assertEquals(EM_EOS, e.getMessage());
            assertNull(e.getCause());
          } // end try-with-resources, RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE
        } catch (IOException e) {
          fail(UNEXPECTED, e);
        } // end Catch (...)
      } // end fi
    } // end For (tag...)
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#PrimitiveBerTlv(long, byte[])}. */
  @Test
  void test_PrimitiveBerTlv__long_byteA() {
    // Assertions:
    // ... a. Super-constructor BerTlv(long, long) works as expected.
    // ... b. checkTag()-method works as expected.

    // Simple constructor doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. Manually chosen values for smoke test.
    // --- b. Invalid tag

    // --- a. Manually chosen values for smoke test.
    // a.1 Empty value-field
    long tag = 0xdf21;
    byte[] valueField = AfiUtils.EMPTY_OS;
    PrimitiveBerTlv dut = new PrimitiveBerTlv(tag, valueField);
    assertEquals(tag, dut.getTag());
    assertNotSame(valueField, dut.getValueField());
    assertArrayEquals(valueField, dut.getValueField());
    assertNotSame(dut.getValueField(), dut.getValueField());
    assertEquals("df2100", Hex.toHexDigits(dut.getEncoded()));

    // a.2 non-empty value-field
    tag = 0xc2;
    valueField = RNG.nextBytes(1, 16);
    dut = new PrimitiveBerTlv(tag, valueField);
    assertEquals(tag, dut.getTag());
    assertNotSame(valueField, dut.getValueField());
    assertArrayEquals(valueField, dut.getValueField());
    assertNotSame(dut.getValueField(), dut.getValueField());
    assertEquals(
        "c2" + String.format("%02x", valueField.length) + Hex.toHexDigits(valueField),
        Hex.toHexDigits(dut.getEncoded()));

    // --- b. Invalid tag
    final Throwable thrown =
        assertThrows(
            IllegalArgumentException.class, () -> new PrimitiveBerTlv(0x20, AfiUtils.EMPTY_OS));
    assertEquals(EMC, thrown.getMessage());
    assertNull(thrown.getCause());
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#createTag(ClassOfTag, long)}. */
  @Test
  void test_createTag__ClassOfTag_long() {
    // Assertions:
    // ... a. Underlying method BerTlv.createTag(ClassOfTag, boolean, long) works as expected.

    // Simple method  doesn't need extensive testing, so we can be lazy here.
    // Test strategy:
    // --- a. loop over all class of tag
    // --- b. loop over all numbers for one byte tag

    // --- a. loop over all class of tag
    for (final var clazz : ClassOfTag.values()) {
      long number = 0;
      // --- b. loop over all numbers for one byte tag
      for (; number < 0x1f; number++) {
        assertEquals(
            String.format("%02x", clazz.getEncoding() + number),
            Hex.toHexDigits(PrimitiveBerTlv.createTag(clazz, number)));
      } // end For (number...)
    } // end For (clazz...)
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#equals(java.lang.Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. difference in tag
    // --- e. difference in value field length
    // --- f. difference in value field content
    // --- g. different object but same tag and value-field

    // --- create device under test (DUT)
    final PrimitiveBerTlv dut = (PrimitiveBerTlv) BerTlv.getInstance("02 03 123456");

    for (final Object obj :
        new Object[] {
          dut, // --- a. same reference
          null, // --- b. null input
          "afi" // --- c. difference in type
        }) {
      assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals to compare object references
    } // end For (obj...)

    Map.ofEntries(
            // --- d. difference in tag
            Map.entry(BerTlv.getInstance("42 03 123456"), false),
            // --- e. difference in value field length
            Map.entry(BerTlv.getInstance("02 04 12345678"), false),
            // --- f. difference in value field content
            Map.entry(BerTlv.getInstance("02 03 123457"), false),
            // --- g. different object but same tag and value-field
            Map.entry(BerTlv.getInstance("02 03 123456"), true))
        .forEach(
            (tlv, expected) -> {
              assertInstanceOf(PrimitiveBerTlv.class, tlv);
              assertNotSame(dut, tlv);
              assertEquals(expected, dut.equals(tlv));
            }); // end forEach((tlv, expected) -> ...)
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#getLengthOfValueField()}. */
  @Test
  void test_getLengthOfValueField() {
    // Assertions:
    // ... a. Constructors work as expected.

    // Simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. Test with some random values for length of value-field.
    for (final var len : RNG.intsClosed(0, 1024, 10).boxed().toList()) {
      final byte[] value = RNG.nextBytes(len);
      final PrimitiveBerTlv dut = new PrimitiveBerTlv(0x04, value); // NOPMD new in loop
      assertEquals((long) len, dut.getLengthOfValueField());
    } // end For (len...)
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#getValueField()}. */
  @Test
  void test_getValueField() {
    // Assertions:
    // ... a. Constructors work as expected.

    // Simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. Test with some random values for length of value-field.
    for (final var len : RNG.intsClosed(0, 1024, 10).boxed().toList()) {
      final byte[] valueIn = RNG.nextBytes(len);
      final PrimitiveBerTlv dut = new PrimitiveBerTlv(0x44, valueIn); // NOPMD new in loop
      final byte[] valueOut = dut.getValueField();
      assertArrayEquals(valueIn, valueOut);
      assertNotSame(valueIn, dut.insValueField);
      assertNotSame(valueOut, dut.insValueField);
    } // end For (len...)
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#hashCode()}. */
  @Test
  void test_hashCode() {
    // Test strategy:
    // --- a. loop over relevant subset of available tags
    // --- b. Loop over various values for lengthOfValueField
    // --- c. call hashCode()-method again

    // --- a. loop over relevant subset of available tags
    TestBerTlv.VALID_TAGS.stream()
        .filter(tagField -> (tagField.length <= BerTlv.NO_TAG_FIELD)) // tag short enough
        .filter(tagField -> (0x00 == (tagField[0] & 0x20))) // primitive encoding
        .filter(tagField -> (0x00 != (tagField[0] & 0xc0))) // not universal class
        .forEach(
            tagField -> {
              // --- b. Loop over various values for lengthOfValueField
              RNG.intsClosed(0, 500, 10)
                  .forEach(
                      len -> {
                        final byte[] lengthField = Hex.toByteArray(BerTlv.getLengthField(len));
                        final byte[] valueField = RNG.nextBytes(len);
                        final byte[] input =
                            AfiUtils.concatenate(tagField, lengthField, valueField);
                        final PrimitiveBerTlv dut = (PrimitiveBerTlv) BerTlv.getInstance(input);
                        final long tag = dut.getTag();
                        final int msInt = (int) (tag >> 32);
                        final int lsInt = (int) tag;

                        final int expectedHash =
                            (msInt * 31 + lsInt) * 31 + Arrays.hashCode(valueField);
                        assertEquals(expectedHash, dut.hashCode());
                      }); // end forEach(len -> ...)
            }); // end forEach(tagField -> ...)

    // --- c. call hashCode()-method again
    // Note: The main reason for this check is to get full code-coverage.
    //        a. The first time this method is called on a newly constructed BerTlv object
    //           insHashCode is zero.
    //        b. The second time this method is called the insHashCode isn't zero (with a
    //           high probability).
    final PrimitiveBerTlv dut = (PrimitiveBerTlv) BerTlv.getInstance("80 01 af");
    final int hash = dut.hashCode();
    assertEquals(hash, dut.hashCode());
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#getEncoded()}. */
  @Test
  void test_getEncoded() {
    // Assertions:
    // ... a. PrimitiveBerTlv(byte[]) constructor works as expected.

    // Test strategy:
    // --- a. loop over relevant subset of available tags
    // --- b. loop over various values for lengthOfValueField

    // --- a. loop over relevant subset of available tags
    for (final var tagField : TestBerTlv.VALID_TAGS) {
      if ((tagField.length <= BerTlv.NO_TAG_FIELD) // tag short enough
          && (0x00 == (tagField[0] & 0x20)) // primitive encoding
          && (0x00 != (tagField[0] & 0xc0)) // not universal class
      ) {
        // --- b. Loop over various values for lengthOfValueField
        for (final var len : RNG.intsClosed(0, 500, 10).boxed().toList()) {
          final byte[] lengthField = Hex.toByteArray(BerTlv.getLengthField(len));
          final byte[] valueField = RNG.nextBytes(len);
          final byte[] input = AfiUtils.concatenate(tagField, lengthField, valueField);
          final byte[] present = BerTlv.getInstance(input).getEncoded();
          assertNotSame(input, present);
          assertArrayEquals(input, present);
        } // end For (len...)
      } // end fi
    } // end For (tagField...)
    // end --- a, b.
  } // end method */

  /** Test method for {@link PrimitiveBerTlv#getEncoded(java.io.ByteArrayOutputStream)}. */
  @Test
  void test_getEncoded__OutputStream() {
    // Assertions:
    // ... a. PrimitiveBerTlv(byte[])-constructor    works as expected.
    // ... b. super.toByteArray(OutputStream)-method works as expected.

    // Test strategy:
    // --- a. loop over relevant subset of available tags
    // --- b. loop over various values for lengthOfValueField
    // --- c. use partially filled stream

    // --- a. loop over relevant subset of available tags
    final int maxLengthValueField = 500;
    final int maxLengthPrefix = 128;
    final int sizeOutputBuffer = maxLengthPrefix + 8 + 3 + maxLengthValueField;
    final ByteArrayOutputStream baos = new ByteArrayOutputStream(sizeOutputBuffer);
    for (final var tagField : TestBerTlv.VALID_TAGS) {
      if ((tagField.length <= BerTlv.NO_TAG_FIELD) // tag short enough
          && (0x00 == (tagField[0] & 0x20)) // primitive encoding
          && (0x00 != (tagField[0] & 0xc0)) // not universal class
      ) {
        // --- b. Loop over various values for lengthOfValueField
        for (final var len : RNG.intsClosed(0, maxLengthValueField, 10).boxed().toList()) {
          final byte[] lengthField = Hex.toByteArray(BerTlv.getLengthField(len));
          final byte[] valueField = RNG.nextBytes(len);
          final byte[] input = AfiUtils.concatenate(tagField, lengthField, valueField);
          final PrimitiveBerTlv dut = (PrimitiveBerTlv) BerTlv.getInstance(input);
          byte[] present = dut.getEncoded(baos).toByteArray();
          assertNotSame(input, present);
          assertArrayEquals(input, present);
          baos.reset();

          // --- c. use partially filled stream
          final byte[] prefix = RNG.nextBytes(1, maxLengthPrefix);
          baos.writeBytes(prefix);
          present = dut.getEncoded(baos).toByteArray();
          assertArrayEquals(AfiUtils.concatenate(prefix, input), present);
          baos.reset();
        } // end For (len...)
      } // end fi
    } // end For (tagField...)
    // end --- a, b, c.
  } // end method */

  /**
   * Test method for {@link PrimitiveBerTlv#toString(String, String, int, boolean)}.
   *
   * <p><i><b>Note:</b> This method tests cases where the value of {@code addComment} does not
   * matter, because none of the TLV-objects under test implement {@link DerSpecific}.</i>
   */
  @Test
  void test_toString__String_String_int_false() {
    // Assertions:
    // ... a. Method tagLength2String(String, String, int) works as expected

    // Note 1: The method-under-test is used only for toString()-conversions which is mostly to
    //         inform a user about the content of TLV-objects. Thus, we can be (a bit) lazy here
    //         and do some smoke-tests for good test-coverage.

    // Test strategy:
    // --- a. delimiter = "",  value-field absent
    // --- b. delimiter = "x", value-field absent
    // --- c. delimiter = "",  value-field present
    // --- d. delimiter = "y", value-field present

    for (final var comment : VALUES_BOOLEAN) {
      final String delo = "";
      PrimitiveBerTlv dut;

      // --- a. delimiter = "",  value-field absent
      dut = new PrimitiveBerTlv(0xdf45, AfiUtils.EMPTY_OS); // NOPMD new in loop
      assertEquals("df4500", dut.toString("", delo, 0, comment));

      // --- b. delimiter = "x", value-field absent
      assertEquals("df45x00", dut.toString("x", delo, 0, comment));

      // --- c. delimiter = "",  value-field present
      final byte[] value = RNG.nextBytes(1, 20);
      final String lengthField = BerTlv.getLengthField(value.length);
      final String valueField = Hex.toHexDigits(value);
      dut = new PrimitiveBerTlv(0x9f23, value); // NOPMD new in loop
      assertEquals("9f23" + lengthField + valueField, dut.toString("", delo, 0, comment));

      // --- d. delimiter = "y", value-field present
      assertEquals("9f23y" + lengthField + "y" + valueField, dut.toString("y", delo, 0, comment));
    } // end For (addComment...)
    Set.of(true, false)
        .forEach(
            addComment -> {
              final String delo = "";
              PrimitiveBerTlv dut;

              // --- a. delimiter = "",  value-field absent
              dut = new PrimitiveBerTlv(0xdf45, new byte[0]);
              assertEquals("df4500", dut.toString("", delo, 0, addComment));

              // --- b. delimiter = "x", value-field absent
              assertEquals("df45x00", dut.toString("x", delo, 0, addComment));

              // --- c. delimiter = "",  value-field present
              final byte[] value = RNG.nextBytes(1, 20);
              final String lengthField = BerTlv.getLengthField(value.length);
              final String valueField = Hex.toHexDigits(value);
              dut = new PrimitiveBerTlv(0x9f23, value);
              assertEquals(
                  "9f23" + lengthField + valueField, dut.toString("", delo, 0, addComment));

              // --- d. delimiter = "y", value-field present
              assertEquals(
                  "9f23y" + lengthField + "y" + valueField, dut.toString("y", delo, 0, addComment));
            }); // end forEach(addComment -> ...)
  } // end method */

  /**
   * Test method for {@link ConstructedBerTlv#toString(String, String, int, boolean)}.
   *
   * <p><i><b>Note:</b> This method tests cases where the value of {@code addComment} is important,
   * because at least some of the TLV-objects under test implement {@link DerSpecific}.</i>
   */
  @Test
  void test_toString__String_String_int_true() {
    // Note 1: The correct handling of comments is tested in subclasses implementing
    //         interface "DerComment". So we can be lazy here and concentrate on
    //         smoke test and code-coverage.

    // Test strategy:
    // --- a. smoke test for PrimitiveBerTlv, i.e. not implementing DerComment-interface
    // --- b. smoke test for class implementing DerComment-interface

    // --- a. smoke test for PrimitiveBerTlv, i.e. not implementing DerComment-interface
    Set.of("84 03 720815")
        .forEach(
            input ->
                Set.of(true, false)
                    .forEach(
                        addComment -> {
                          final BerTlv dut = BerTlv.getInstance(input);
                          assertEquals(PrimitiveBerTlv.class, dut.getClass());
                          assertFalse(dut instanceof DerSpecific);
                          assertEquals(input, dut.toStringTree());
                        })); // end forEach(input -> ...)

    // --- b. smoke test for class implementing DerComment-interface
    final BerTlv dut = DerEndOfContent.EOC;
    assertEquals("00 00", dut.toString(" "));
    assertEquals("00 00 # EndOfContent", dut.toStringTree());
  } // end method */

  /**
   * Creates a {@link PrimitiveBerTlv} with random tag and random value-field.
   *
   * @param tagLengthValue output parameter, filled by this method, <b>SHALL</b> have a length of
   *     three; the method sets
   *     <ol>
   *       <li>tag-field in element 0,
   *       <li>length-field in element 1
   *       <li>value-field in element 2
   *     </ol>
   */
  /* package */
  static PrimitiveBerTlv createRandom(
      final boolean isIndefiniteForm, final int maxLengthPrimitive, final byte[][] tagLengthValue) {
    final ClassOfTag childClazz = TestBerTlv.randomClassOfTag();
    final long childTagNumber = TestBerTlv.randomTagNumber(childClazz, isIndefiniteForm, false);
    final var child =
        createRandom(
            BerTlv.convertTag(BerTlv.createTag(childClazz, false, childTagNumber)),
            RNG.nextIntClosed(0, maxLengthPrimitive));
    tagLengthValue[0] = Hex.toByteArray(child.getTagField());
    tagLengthValue[1] = TestBerTlv.randomLengthField(child.getLengthOfValueField());
    tagLengthValue[2] = child.getValueField();

    return child;
  } // end method */

  /**
   * Creates a {@link PrimitiveBerTlv} with given tag and given length of value-field.
   *
   * @param tag of TLV-object
   * @param len number of octets in value-field
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  /* package */ static PrimitiveBerTlv createRandom(final long tag, final int len) {
    final var tagField = BerTlv.convertTag(tag);
    final var lf = Hex.toByteArray(BerTlv.getLengthField(len));

    // spotless:off
    switch ((int) tag) {
      case DerEndOfContent.TAG:
        return DerEndOfContent.readInstance(
            ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len)))
        ); // tag-number =  0

      case DerBoolean.TAG:
        return DerBoolean.readInstance(
            ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len)))
        ); // tag-number =  1

      case DerInteger.TAG:
        return new DerInteger(
            ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len)))
        ); // tag-number =  2

      case DerBitString.TAG:
        return new DerBitString(
            ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len)))
        ); // tag-number =  3

      case DerOctetString.TAG:
        return new DerOctetString(
            ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len)))
        ); // tag-number =  4

      case DerNull.TAG:
        return DerNull.readInstance(
            ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len)))
        ); // tag-number =  5

      case DerOid.TAG:
        return new DerOid(
            ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len)))
        ); // tag-number =  6

      case DerUtf8String.TAG:
        return new DerUtf8String(RNG.nextPrintable(len)); // tag-number = 12

      case DerPrintableString.TAG:
        return new DerPrintableString(RNG.nextPrintable(len)); // tn = 19

      case DerTeletexString.TAG:
        return new DerTeletexString(RNG.nextPrintable(len)); // tm = 20

      case DerIa5String.TAG:
        return new DerIa5String(RNG.nextPrintable(len)); // tag-number = 22

      case DerUtcTime.TAG:
        return new DerUtcTime(
            ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len)))
        ); // tag-number = 23

      case DerDate.TAG:
        return new DerDate(
            ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len)))
        ); // tag-number = 31

      default:
        // ... tag-value has no specific subclass

        if ((0 == (tagField[0] & 0x20))) {
          return new PrimitiveBerTlv(
              tagField,
              ByteBuffer.wrap(AfiUtils.concatenate(lf, RNG.nextBytes(len))));
        } else {
          throw new IllegalArgumentException("constructed tag");
        } // end else
    } // end Switch (tag)
    // spotless:on
  } // end method */
} // end class
