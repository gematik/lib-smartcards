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

import static de.gematik.smartcards.tlv.DerDate.CHARSET;
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
import java.time.LocalDate;
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
 * Class performing white-box test on {@link DerDate}.
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
final class TestDerDate {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Tag-field. */
  private static final String TAG_FIELD = Hex.toHexDigits(DerDate.TAG_FIELD); // */

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

  /** Test method for {@link DerDate#DerDate(LocalDate)}. */
  @Test
  void test_DerDate__LocalDate() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString(String)-method works as expected

    // Note: Because of the assertion a and the simplicity of the
    //       constructor under test, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. check that defensive cloning is used
    final LocalDate input = LocalDate.of(1965, 3, 24);

    // --- a. smoke test
    final DerDate dut = new DerDate(input);
    assertEquals("1f1f 08 3139363530333234", dut.toString(" "));
    assertTrue(dut.isValid());
    assertTrue(dut.insFindings.isEmpty());
    assertSame(input, dut.insDecoded);
    assertEquals("1965-03-24", dut.insDecoded.toString());
  } // end method */

  /** Test method for {@link DerDate#DerDate(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerDate__ByteBuffer() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with a bunch of random values
    // --- b. FINDING: wrong format
    // --- c: ERROR: ArithmeticException
    // --- d. ERROR: IOException

    // --- a. smoke test with a bunch of random values
    for (final var i : IntStream.range(0, 1000).toArray()) {
      final int year = RNG.nextIntClosed(1584, 3000);
      final int month = RNG.nextIntClosed(1, 12);
      final int day = RNG.nextIntClosed(1, 28);
      final String octets =
          "08" // length-field
              + Hex.toHexDigits(String.format("%04d%02d%02d", year, month, day).getBytes(CHARSET));
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(octets));

      final var dut = new DerDate(buffer);

      assertEquals(TAG_FIELD + octets, dut.toString());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
      assertEquals(LocalDate.of(year, month, day), dut.insDecoded);
      assertEquals(
          String.format("%04d-%02d-%02d", year, month, day),
          dut.insDecoded.toString(),
          () -> Integer.toString(i));
    } // end For (i...)
    // end --- a.

    // --- b. FINDING: wrong format
    {
      final var inputSet =
          Set.of(
              "196503241234", // yyyymmddHHMM time appended
              "2021-02-13" // delimiter
              );
      for (final var input : inputSet) {
        final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
        final String octets =
            BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var buffer = ByteBuffer.wrap(Hex.toByteArray(octets));

        final var dut = new DerDate(buffer);

        assertEquals(TAG_FIELD + octets, dut.toString());
        assertFalse(dut.isValid());
        assertEquals(1, dut.insFindings.size());
        assertEquals(List.of("wrong format"), dut.insFindings);
      } // end For (input...)
    } // end --- b.

    // --- c: ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerDate(buffer));

      assertNull(e.getCause());
    } // end --- c.

    // --- d. ERROR: BufferUnderflowException
    for (final var input : Set.of("-08-(33343536.3132.3038)", "-8108-(33343536.3132.3039)")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerDate(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerDate(buffer));
    } // end For (input...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerDate#DerDate(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerDate__InputStream() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with a bunch of random values
    // --- b. FINDING: wrong format
    // --- c: ERROR: ArithmeticException
    // --- d. ERROR: IOException

    try {
      // --- a. smoke test with a bunch of random values
      for (final var i : IntStream.range(0, 1000).toArray()) {
        final int year = RNG.nextIntClosed(1584, 3000);
        final int month = RNG.nextIntClosed(1, 12);
        final int day = RNG.nextIntClosed(1, 28);
        final String octets =
            "08" // length-field
                + Hex.toHexDigits(
                    String.format("%04d%02d%02d", year, month, day).getBytes(CHARSET));
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(octets));

        final var dut = new DerDate(inputStream);

        assertEquals(TAG_FIELD + octets, dut.toString());
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
        assertEquals(LocalDate.of(year, month, day), dut.insDecoded);
        assertEquals(
            String.format("%04d-%02d-%02d", year, month, day),
            dut.insDecoded.toString(),
            () -> Integer.toString(i));
      } // end For (i...)
      // end --- a.

      // --- b. FINDING: wrong format
      {
        final var inputSet =
            Set.of(
                "196503241234", // yyyymmddHHMM time appended
                "2021-02-13" // delimiter
                );
        for (final var input : inputSet) {
          final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
          final String octets =
              BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
          final var inputStream = new ByteArrayInputStream(Hex.toByteArray(octets));

          final var dut = new DerDate(inputStream);

          assertEquals(TAG_FIELD + octets, dut.toString());
          assertFalse(dut.isValid());
          assertEquals(1, dut.insFindings.size());
          assertEquals(List.of("wrong format"), dut.insFindings);
        } // end For (input...)
      } // end --- b.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- c: ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrownD =
          assertThrows(ArithmeticException.class, () -> new DerDate(inputStream));

      assertNull(thrownD.getCause());
    } // end --- c.

    // --- d. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerDate(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerDate#getComment()}. */
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
                    "19870623", // no findings
                    " # DATE := 1987-06-23"),
                Map.entry(
                    "870623", // with findings, year with only two digits
                    " # DATE, findings: wrong format, value-field as UTF-8: 870623"),
                Map.entry(
                    "1987-06-23", // with findings, delimiter
                    " # DATE, findings: wrong format, value-field as UTF-8: 1987-06-23"),
                Map.entry(
                    "198706231432", // with findings, time appended
                    " # DATE, findings: wrong format, value-field as UTF-8: 198706231432"))
            .entrySet()) {
      final var input = entry.getKey();
      final var comment = entry.getValue();
      final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
      final String octets =
          String.format("%04x", DerDate.TAG)
              + BerTlv.getLengthField(valueField.length)
              + Hex.toHexDigits(valueField);
      final BerTlv dutGen = BerTlv.getInstance(octets);
      assertEquals(DerDate.class, dutGen.getClass());
      final DerDate dut = (DerDate) dutGen;
      assertEquals(octets, dut.toString());
      assertEquals(comment, dut.getComment());
    } // end For (entry...)
  } // end method */

  /** Test method for {@link DerDate#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with value constructor
    // --- b. smoke test with InputStream constructor

    // --- a. smoke test with value constructor
    {
      final LocalDate input = LocalDate.of(1965, 3, 27);
      final DerDate dut = new DerDate(input);
      assertEquals("1f1f 08 3139363530333237", dut.toString(" "));
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
      assertSame(input, dut.insDecoded);
      assertEquals("1965-03-27", dut.getDecoded().toString());
    }

    // --- b. smoke test with InputStream constructor
    {
      final BerTlv dutGen =
          BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray("1f1f 08 3139363530333238")));
      assertEquals(DerDate.class, dutGen.getClass());
      final DerDate dut = (DerDate) dutGen;
      assertEquals(LocalDate.of(1965, 3, 28), dut.getDecoded());
    } // end --- b.
  } // end method */
} // end class
