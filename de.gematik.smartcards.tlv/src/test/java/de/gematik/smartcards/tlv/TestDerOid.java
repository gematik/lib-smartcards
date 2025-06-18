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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box tests on {@link DerOid}.
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
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerOid {

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

  /** Test method for {@link DerOid#DerOid(AfiOid)}. */
  @Test
  void test_DerOid__AfiOid() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test pre-defined values
    for (final var oid : AfiOid.PREDEFINED) {
      final DerOid dut = new DerOid(oid); // NOPMD new in loop

      final var oidOctetString = oid.getOctetString();
      assertSame(oid, dut.insDecoded);
      assertEquals(
          "06" // tag-field
              + BerTlv.getLengthField(Hex.toByteArray(oidOctetString).length) // length-field
              + oidOctetString, // value-field
          dut.toString());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (oid...)
  } // end method */

  /** Test method for {@link DerOid#DerOid(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerOid__ByteBuffer() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. invalid input
    // --- c. ERROR: ArithmeticException
    // --- d. ERROR: BufferUnderflowException

    // --- a. smoke test
    for (final var oid : AfiOid.PREDEFINED) {
      final var oidOctetString = oid.getOctetString();
      final String input =
          BerTlv.getLengthField(Hex.toByteArray(oidOctetString).length) // length-field
              + oidOctetString; // value-field
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = new DerOid(buffer);

      assertNotNull(dut.insDecoded);
      assertEquals(oid, dut.insDecoded);
      assertEquals("06" + input, dut.toString());
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
    } // end For (oid...)
    // end --- a.

    // --- b. invalid input
    {
      final var oidOctetString = "80";
      final String input =
          BerTlv.getLengthField(Hex.toByteArray(oidOctetString).length) // length-field
              + oidOctetString; // value-field
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = new DerOid(buffer);

      assertNotNull(dut.insDecoded);
      assertEquals(AfiOid.INVALID, dut.insDecoded);
      assertEquals("06" + input, dut.toString());
      assertFalse(dut.isValid());
      assertEquals(List.of("invalid OID"), dut.insFindings);
    } // end --- b.

    // --- c. ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerOid(buffer));

      assertNull(e.getCause());
    } // end --- c.

    // --- d. ERROR: BufferUnderflowException
    for (final var input : Set.of("-03-813403", "-8103-813403", "-820003-813403")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerOid(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerOid(buffer));
    } // end For (input...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerOid#DerOid(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerOid__InputStream() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. invalid input
    // --- c. ERROR: ArithmeticException
    // --- d. ERROR: IOException

    // --- a. smoke test
    try {
      for (final var oid : AfiOid.PREDEFINED) {
        final var oidOctetString = oid.getOctetString();
        final String input =
            BerTlv.getLengthField(Hex.toByteArray(oidOctetString).length) // length-field
                + oidOctetString; // value-field
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerOid(inputStream);

        assertNotNull(dut.insDecoded);
        assertEquals(oid, dut.insDecoded);
        assertEquals("06" + input, dut.toString());
        assertTrue(dut.isValid());
        assertTrue(dut.insFindings.isEmpty());
      } // end For (oid...)
      // end --- a.

      // --- b. invalid input
      {
        final var oidOctetString = "80";
        final String input =
            BerTlv.getLengthField(Hex.toByteArray(oidOctetString).length) // length-field
                + oidOctetString; // value-field
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerOid(inputStream);

        assertNotNull(dut.insDecoded);
        assertEquals(AfiOid.INVALID, dut.insDecoded);
        assertEquals("06" + input, dut.toString());
        assertFalse(dut.isValid());
        assertEquals(List.of("invalid OID"), dut.insFindings);
      } // end --- b.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- c. ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> new DerOid(inputStream));

      assertNull(thrown.getCause());
    } // end --- c.

    // --- d. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerOid(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerBoolean#getComment()}. */
  @Test
  void test_getComment() {
    // Note: This simple method does not need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with pre-defined OID from class AfiOid
    // --- b. smoke test with OID not pre-defined in class AfiOid

    // --- a. smoke test with pre-defined OID from class AfiOid
    AfiOid.PREDEFINED.forEach(
        oid -> {
          final DerOid dut = new DerOid(oid);
          assertSame(oid, dut.insDecoded);
          assertEquals(
              DerSpecific.DELIMITER
                  + "OBJECT IDENTIFIER := "
                  + oid.getName()
                  + " = "
                  + oid.getPoint(),
              dut.getComment());
        }); // end forEach(oid -> ...)

    // --- b. smoke test with OID not pre-defined in class AfiOid
    final DerOid dut = new DerOid(new AfiOid(1, 3, 64, 3, 1));
    assertEquals(" # OBJECT IDENTIFIER := 1.3.64.3.1", dut.getComment());
  } // end method */

  /** Test method for {@link DerOid#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Assertions:
    // ... a. Constructor DerOid(AfiOid) works as expected
    // ... b. Constructor DerOid(InputStream) works as expected

    // Test strategy:
    // --- a. decode after DerOid(AfiOid)-constructor
    // --- b. decode after DerOid(InputStream)-constructor

    AfiOid input;
    DerOid dut;

    // --- a. decode after DerInteger(BigInteger)-constructor
    input = AfiOid.brainpoolP384r1;
    dut = new DerOid(input);
    assertNotNull(dut.insDecoded);
    assertSame(input, dut.insDecoded);
    assertSame(input, dut.getDecoded());

    // --- b. decode after DerOid(InputStream)-constructor
    input = AfiOid.rsaEncryption;
    dut = (DerOid) BerTlv.getInstance(new ByteArrayInputStream(new DerOid(input).getEncoded()));
    assertNotNull(dut.insDecoded);
    assertNotSame(input, dut.insDecoded);
    assertEquals(input, dut.insDecoded);
    assertSame(dut.insDecoded, dut.getDecoded());
  } // end method */
} // end class
