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

import static de.gematik.smartcards.tlv.DerInteger.EM_9;
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
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Class performing white-box tests on {@link DerInteger}.
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
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerInteger {

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

  /** Test method for {@link DerInteger#DerInteger(BigInteger)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerInteger__BigInteger() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with small  values
    // --- b. smoke test with random values

    // --- a. smoke test with small  values
    for (int value = -32_768; value <= 32_767; value++) {
      final DerInteger dut = new DerInteger(BigInteger.valueOf(value)); // NOPMD new in loop

      assertNotNull(dut.insDecoded);
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());

      if ((-128 <= value) && (value <= 127)) {
        // ... one byte value-field
        assertEquals(String.format("0201%02x", value & 0xff), dut.toString());
      } else {
        // ... two byte value-field
        assertEquals(String.format("0202%04x", value & 0xffff), dut.toString());
      } // end else (1 or 2 byte in value-field)
    } // end For (value...)

    // --- b. smoke test with random values
    for (final var bitLength : RNG.intsClosed(1, 20_000, 10_000).toArray()) {
      final byte[] rnd = RNG.nextBytes(bitLength >> 3);

      for (final var signum : Set.of(-1, +1)) {
        final BigInteger input = new BigInteger(signum, rnd);
        final byte[] octets = input.toByteArray();
        final String exp = "02" + BerTlv.getLengthField(octets.length) + Hex.toHexDigits(octets);

        final DerInteger dut = new DerInteger(input);

        assertEquals(exp, dut.toString());
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
      } // end For (signum...)
    } // end For (bitLength...)
  } // end method */

  /** Test method for {@link DerInteger#DerInteger(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerInteger__ByteBuffer() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with all one and two byte encodings (and a bit more)
    // --- b. FINDING: wrong length
    // --- c: ERROR: ArithmeticException
    // --- d: ERROR: BufferUnderflowException

    // --- a. smoke test with all one and two byte encodings (and a bit more)
    for (final var value : IntStream.rangeClosed(-70_000, 70_000).toArray()) {
      final var valueField = BigInteger.valueOf(value).toByteArray();
      final var octets = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(octets));

      final var dut = new DerInteger(buffer);

      assertNull(dut.insDecoded);
      assertEquals(value, dut.getDecoded().intValueExact());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (value...)

    // --- b. FINDING: wrong length
    // b.1 value-field absent
    // b.2 value-field present, but 9 MSBit are all equal
    {
      DerInteger dut;

      // b.1 value-field absent
      dut = new DerInteger(ByteBuffer.wrap(Hex.toByteArray("-00")));
      assertEquals(BigInteger.ZERO, dut.getDecoded());
      assertFalse(dut.isValid());
      assertEquals(List.of("value-field absent"), dut.insFindings);

      // b.2 value-field present, but 9 MSBit are all equal
      dut = new DerInteger(ByteBuffer.wrap(Hex.toByteArray("-02-007f"))); // 9 MSBit are '0'

      assertEquals(BigInteger.valueOf(0x7f), dut.getDecoded());
      assertFalse(dut.isValid());
      assertEquals(List.of(EM_9), dut.insFindings);

      dut = new DerInteger(ByteBuffer.wrap(Hex.toByteArray("-02-ff80"))); // 9 MSBit are '1'

      assertEquals(BigInteger.valueOf(-128), dut.getDecoded());
      assertFalse(dut.isValid());
      assertEquals(List.of(EM_9), dut.insFindings);
    }
    // end --- b.

    // --- c: ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerInteger(buffer));

      assertNull(e.getCause());
    } // end --- c.

    // --- d. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerInteger(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerInteger(buffer));
    } // end For (input...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerInteger#DerInteger(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerInteger__InputStream() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with all one and two byte encodings (and a bit more)
    // --- b. FINDING: wrong length
    // --- c: ERROR: ArithmeticException
    // --- d: ERROR: IOException

    try {
      // --- a. smoke test with all one and two byte encodings (and a bit more)
      for (final var value : IntStream.rangeClosed(-70_000, 70_000).toArray()) {
        final var valueField = BigInteger.valueOf(value).toByteArray();
        final var octets = BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(octets));

        final var dut = new DerInteger(inputStream);

        assertNull(dut.insDecoded);
        assertEquals(value, dut.getDecoded().intValueExact());
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
      } // end For (value...)

      // --- b. FINDING: wrong length
      // b.1 value-field absent
      // b.2 value-field present, but 9 MSBit are all equal
      {
        DerInteger dut;

        // b.1 value-field absent
        dut = new DerInteger(new ByteArrayInputStream(Hex.toByteArray("-00")));
        assertEquals(BigInteger.ZERO, dut.getDecoded());
        assertFalse(dut.isValid());
        assertEquals(List.of("value-field absent"), dut.insFindings);

        // b.2 value-field present, but 9 MSBit are all equal
        dut =
            new DerInteger(
                new ByteArrayInputStream(Hex.toByteArray("-02-007f"))); // 9 MSBit are '0'

        assertEquals(BigInteger.valueOf(0x7f), dut.getDecoded());
        assertFalse(dut.isValid());
        assertEquals(List.of(EM_9), dut.insFindings);

        dut =
            new DerInteger(
                new ByteArrayInputStream(Hex.toByteArray("-02-ff80"))); // 9 MSBit are '1'

        assertEquals(BigInteger.valueOf(-128), dut.getDecoded());
        assertFalse(dut.isValid());
        assertEquals(List.of(EM_9), dut.insFindings);
      }
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- b.

    // --- c: ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> new DerInteger(inputStream));

      assertNull(thrown.getCause());
    } // end --- c.

    // --- d. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-d.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerInteger(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerInteger#getComment()}. */
  @Test
  void test_getComment() {
    // Note: This simple method does not need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. FINDING: value-field absent
    // --- c. FINDING: 9 MSBit all equal

    // --- a. smoke test
    for (final var value : IntStream.rangeClosed(-1000, 1000).toArray()) {
      final var dut = new DerInteger(BigInteger.valueOf(value)); // NOPMD new in loop
      final var expected = " # INTEGER := " + value;

      final var actual = dut.getComment();

      assertEquals(expected, actual);
    } // end For (value...)
    // end --- a.

    // --- b. FINDING: value-field absent
    Set.of(
            "02 00", // optimal length-field
            "02 8100", // long length-field
            "02 8f00 0000 0000 0000 0000 0000 0000 0000" // really long length-field
            )
        .forEach(
            input -> {
              final BerTlv dutGen =
                  BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));
              assertEquals(DerInteger.class, dutGen.getClass());
              final DerInteger dut = (DerInteger) dutGen;
              assertEquals(" # INTEGER := 0, findings: value-field absent", dut.getComment());
            }); // end forEach(input -> ...)

    // --- c. FINDING: 9 MSBit all equal
    Map.ofEntries(
            Map.entry(127, "02 02 007f"),
            Map.entry(126, "02 8102 007e"),
            Map.entry(125, "02 820003 00007d"),
            Map.entry(-128, "02 02 ff80"),
            Map.entry(-127, "02 8102 ff81"),
            Map.entry(-126, "02 820003 ffff82"))
        .forEach(
            (key, value) -> {
              final BerTlv dutGen =
                  BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(value)));
              assertEquals(DerInteger.class, dutGen.getClass());
              final DerInteger dut = (DerInteger) dutGen;
              assertEquals(
                  " # INTEGER := " + key + ", findings: 9 MSBit all equal", dut.getComment());
            }); // end forEach(input -> ...)
  } // end method */

  /** Test method for {@link DerInteger#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Assertions:
    // ... a. Constructor DerInteger(BigInteger) works as expected
    // ... b. Constructor DerInteger(InputStream) works as expected

    // Test strategy:
    // --- a. decode after DerInteger(BigInteger)-constructor
    // --- b. decode after DerInteger(InputStream)-constructor

    final BigInteger input;
    DerInteger dut;

    // --- a. decode after DerInteger(BigInteger)-constructor
    input = BigInteger.ZERO;
    dut = new DerInteger(input);
    assertNotNull(dut.insDecoded);
    assertEquals("02 01 00", dut.toString(" "));
    assertSame(input, dut.getDecoded());

    // --- b. decode after DerInteger(InputStream)-constructor
    dut = (DerInteger) BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray("02 02 00ff")));
    assertNull(dut.insDecoded);
    assertEquals(255, dut.getDecoded().intValueExact());
  } // end method */
} // end class
