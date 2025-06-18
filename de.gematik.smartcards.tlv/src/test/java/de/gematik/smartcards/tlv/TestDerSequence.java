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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
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
 * Class performing white-box tests on {@link DerSequence}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.,
//         Non-null field is not initialized
//         This finding is for a class attribute which is initialized by JUnit.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 1
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerSequence {

  /** Prefix for class names in case of immutable lists. */
  private static final String CLASS_IMMUTABLE_LIST = "java.util.ImmutableCollections$List"; // */

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

  /** Test method for {@link DerSequence#DerSequence(Collection)}. */
  @Test
  void test_DerSequence__Collection() {
    // Assertions:
    // ... a. super-constructor works as expected
    // ... b. toString()-method works as expected

    // Note: Because of the assertions a and b we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    for (final var entry :
        Map.ofEntries(
                Map.entry("30 00", new ArrayList<>()), // 0 entries
                Map.entry("30 02  05 00", List.of(DerNull.NULL)), // 1 entry
                // 2 entries
                Map.entry(
                    "30 06  05 00  04 02 4711",
                    List.of(DerNull.NULL, new DerOctetString(Hex.toByteArray("4711")))))
            .entrySet()) {
      final var key = entry.getKey();
      final var value = entry.getValue();
      final var list =
          value.stream().filter(i -> i instanceof BerTlv).map(i -> (BerTlv) i).toList();

      final DerSequence dut = new DerSequence(list); // NOPMD new in loop

      assertEquals(key, dut.toString(" "));
      assertTrue(dut.isValid());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    } // end For (entry...)
  } // end method */

  /** Test method for {@link DerSequence#DerSequence(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerSequence__ByteBuffer() {
    // Assertions:
    // ... a. super-constructor works as expected

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. ERROR: ArithmeticException
    // --- c. ERROR: IOException

    // --- a. smoke test with manually chosen input
    final var set = Set.of("00", "02  45 00", "06  05 00  04 02 4712", "06  81 00  81 00  82 00");
    for (final var input : set) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = new DerSequence(buffer);

      assertEquals("30 " + input, dut.toString(" "));
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    } // end For (input...)
    // end --- a.

    // --- b. ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerSequence(buffer));

      assertNull(e.getCause());
    } // end --- b.

    // --- c. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "-02-(81-00)", "-8102-(82-00)")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerSequence(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerSequence(buffer));
    } // end For (input...)
    // end --- c.
  } // end method */

  /** Test method for {@link DerSequence#DerSequence(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerSequence__InputStream() {
    // Assertions:
    // ... a. super-constructor works as expected

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. ERROR: ArithmeticException
    // --- c. ERROR: IOException

    // --- a. smoke test with manually chosen input
    try {
      final var set = Set.of("00", "02  45 00", "06  05 00  04 02 4712", "06  81 00  81 00  82 00");
      for (final var input : set) {
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerSequence(inputStream);

        assertEquals("30 " + input, dut.toString(" "));
        assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
      } // end For (input...)
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.

    // --- b. ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final Throwable thrown =
          assertThrows(ArithmeticException.class, () -> new DerSequence(inputStream));

      assertNull(thrown.getCause());
    } // end --- b.

    // --- c. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerSequence(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- c.
  } // end method */

  /** Test method for {@link DerSequence#add(BerTlv)}. */
  @Test
  void test_add_BerTlv() {
    // Assertions:
    // ... a. toString(String)-method works as expected.

    // Test strategy:
    // --- a. add to   empty   value-field
    // --- b. add to non-empty value-field
    // --- c. add same object again

    // --- initialize device-under-test
    final DerSequence dut = new DerSequence(new ArrayList<>());
    final List<BerTlv> vaf = dut.getTemplate();
    assertTrue(vaf.isEmpty());
    assertEquals("30 00", dut.toString(" ")); // NOPMD "30 00" appears often
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- a. add to   empty   value-field
    final PrimitiveBerTlv tlvA = (PrimitiveBerTlv) BerTlv.getInstance("81 01 a1");
    final ConstructedBerTlv dutA = dut.add(tlvA);
    assertEquals(DerSequence.class, dutA.getClass());
    final List<BerTlv> vafA = dutA.getTemplate();
    assertNotSame(dut, dutA);
    assertEquals(1, vafA.size());
    assertSame(tlvA, vafA.getFirst());
    assertEquals("30 00", dut.toString(" "));
    assertEquals("30 03  81 01 a1", dutA.toString(" "));
    assertTrue(dutA.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- b. add to non-empty value-field
    final ConstructedBerTlv tlvB = (ConstructedBerTlv) BerTlv.getInstance("a2 00");
    final ConstructedBerTlv dutB = dutA.add(tlvB);
    assertEquals(DerSequence.class, dutB.getClass());
    final List<BerTlv> vafB = dutB.getTemplate();
    assertNotSame(dutA, dutB);
    assertEquals(vafA.size() + 1, vafB.size());
    assertSame(tlvA, vafB.get(0));
    assertSame(tlvB, vafB.get(1));
    assertEquals("30 00", dut.toString(" "));
    assertEquals("30 03  81 01 a1", dutA.toString(" "));
    assertEquals("30 05  81 01 a1  a2 00", dutB.toString(" "));
    assertTrue(dutB.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- c. add same object again
    final ConstructedBerTlv dutC = dutB.add(tlvB);
    assertEquals(DerSequence.class, dutC.getClass());
    final List<BerTlv> vafC = dutC.getTemplate();
    assertNotSame(dutB, dutC);
    assertEquals(vafB.size() + 1, vafC.size());
    assertSame(tlvA, vafC.get(0));
    assertSame(tlvB, vafC.get(1));
    assertSame(tlvB, vafC.get(2));
    assertEquals("30 00", dut.toString(" "));
    assertEquals("30 03  81 01 a1", dutA.toString(" "));
    assertEquals("30 05  81 01 a1  a2 00", dutB.toString(" "));
    assertEquals("30 07  81 01 a1  a2 00  a2 00", dutC.toString(" "));
    assertTrue(dutC.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
  } // end method */

  /** Test method for {@link DerSequence#getComment()}. */
  @Test
  void test_getComment() {
    // Assertions:
    // ... a. super-constructor works as expected

    // Note: Because of the assertion a and because the method-under-test is rather
    //       simple, we can be lazy here and concentrate on good code-coverage.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    for (final var entry :
        Map.ofEntries(
                // 0 entries
                Map.entry("30 00", "30 00 # SEQUENCE with 0 elements"),
                // 1 entry
                Map.entry(
                    "30 02  05 00",
                    String.format("30 02 # SEQUENCE with 1 element%n|  05 00 # NULL")),
                // 2 entries
                Map.entry(
                    "30 06  05 00  04 02 4713",
                    String.format(
                        "30 06 # SEQUENCE with 2 elements%n"
                            + "|  05 00 # NULL%n"
                            + "|  04 02 4713 # OCTETSTRING")))
            .entrySet()) {
      final var key = entry.getKey();
      final var expected = entry.getValue();
      final DerSequence dut = (DerSequence) BerTlv.getInstance(key);

      assertEquals(expected, dut.toStringTree());
      assertEquals(key, dut.toString(" "));
    } // end For (entry...)
  } // end method */

  /** Test method for {@link DerSequence#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Assertions:
    // ... a. super-constructor works as expected
    // ... b. super.getValue()-method works as expected

    // Note: Because of the assertion a and b we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    for (final var entry :
        Map.ofEntries(
                Map.entry("30 00", new ArrayList<>()), // 0 entries
                Map.entry("30 02  05 00", List.of(DerNull.NULL)), // 1 entry
                // 2 entries
                Map.entry(
                    "30 06  05 00  04 02 4714",
                    List.of(DerNull.NULL, new DerOctetString(Hex.toByteArray("4714")))))
            .entrySet()) {
      final var key = entry.getKey();
      final var expected = entry.getValue();
      final var dut = (DerSequence) BerTlv.getInstance(key);

      final var actual = dut.getDecoded();

      assertEquals(expected, actual);
    } // end For (entry...)
  } // end method */

  /** Test method for {@link DerSequence#isValid()}. */
  @Test
  void test_isValid() {
    // Assertions:
    // ... a. constructor works as expected

    // Test strategy:
    // --- a. smoke test
    assertTrue(new DerSequence(new ArrayList<>()).isValid());
  } // end method */
} // end class
