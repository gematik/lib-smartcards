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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiOid;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Class performing white-box tests on {@link DerSet}. */
// Note 1: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.,
//         Non-null field is not initialized
//         This finding is for a class attribute which is initialized by JUnit.
// Note 2: Spotbugs claims "Method ignores return value, is this OK?" when assertThrows(...) is
//         used in test_isoIec23465_createSdApplication__Parameter()-method. But spotbugs only
//         spots these places where newly created objects are ignored, but not similar checks in
//         other test methods. Strange, but true.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 1
  "RV_RETURN_VALUE_IGNORED_INFERRED", // see note 2
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerSet {

  /** Prefix for class names in case of immutable lists. */
  private static final String CLASS_IMMUTABLE_LIST = "java.util.ImmutableCollections$List"; // */

  /** Text for finding with non-mutual exclusive tags. */
  private static final String F_DOUBLES = "tags not mutual exclusive"; // */

  /** Text for finding with non-mutual exclusive tags. */
  private static final String F_ORDER = "tags not correctly sorted"; // */

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

  /** Test method for {@link DerSet#DerSet(Collection)}. */
  @Test
  void test_DerSet__Collection() {
    // Assertions:
    // ... a. "sort(Collection)"-method works as expected
    // ... b. "toString()"-method works as expected

    // Note: Because of the assertions a and b we can be lazy here.

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b. ERROR: tags not mutual exclusive

    // --- a. smoke test with manually chosen input
    for (final var entry :
        Map.ofEntries(
                Map.entry("31 00", Collections.emptySet()), // 0 entries
                Map.entry("31 02  05 00", Set.of(DerNull.NULL)), // 1 entry
                // 2 entries
                Map.entry(
                    "31 06  04 02 4711  05 00",
                    List.of(DerNull.NULL, new DerOctetString(Hex.toByteArray("4711")))))
            .entrySet()) {
      final var expected = entry.getKey();
      final var value = entry.getValue();
      final var collection =
          value.stream().filter(i -> i instanceof BerTlv).map(i -> (BerTlv) i).toList();

      final DerSet dut = new DerSet(collection); // NOPMD new in loop

      assertEquals(expected, dut.toString(" "));
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    } // end For (entry...)

    // --- b. ERROR: tags not mutual exclusive
    Set.of(
            List.of(DerNull.NULL, DerNull.NULL), // equal
            List.of(BerTlv.getInstance("85 00"), BerTlv.getInstance("85 02 1122")) // not equal
            )
        .forEach(
            value -> {
              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> new DerSet(value));
              assertEquals("some tags occur more than once", throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(value -> ...)
  } // end method */

  /** Test method for {@link DerSet#DerSet(ByteBuffer)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount"})
  @Test
  void test_DerSet__ByteBuffer() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b: ERROR: ArithmeticException
    // --- c. duplicate TLV-objects in InputStream
    // --- d. not correctly sorted
    // --- e. ERROR: BufferUnderflowException

    // --- a. smoke test with manually chosen input
    final var set = Set.of("00", "02  45 00", "06  04 02 4712  05 00");
    for (final var input : set) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      final var dut = new DerSet(buffer);

      assertEquals("31 " + input, dut.toString(" "));
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    } // end For (input...)
    // end --- a.

    // --- b. ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerSet(buffer));

      assertNull(e.getCause());
    } // end --- b.

    // --- c. duplicate tag in buffer
    // c.0 no duplicates
    // c.1 duplicate tag at level 0
    // c.2 duplicate tag at level 1
    // c.3 duplicate tag at level 2
    // c.4 duplicate tag at level 0 and level 2
    {
      // c.0 no duplicates
      String input = "31 80  31 0a  31 04  80 00  81 00  80 00  81 00  40 01 ab  41 01 12  00 00";

      BerTlv dutGen = BerTlv.getFromBuffer(ByteBuffer.wrap(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      DerSet dut0 = (DerSet) dutGen;
      assertTrue(dut0.isValid(), dut0.insFindings::toString);
      assertTrue(dut0.insFindings.isEmpty());
      DerSet dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertTrue(dut1.isValid(), dut1.insFindings::toString);
      assertTrue(dut1.insFindings.isEmpty());
      DerSet dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertTrue(dut2.isValid());
      assertTrue(dut2.insFindings.isEmpty());

      // c.1 duplicate tag at level 0
      input = "31 80  31 0a  31 04  80 00  81 00  80 00  81 00  40 01 ab  40 01 12  00 00";

      dutGen = BerTlv.getFromBuffer(ByteBuffer.wrap(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      dut0 = (DerSet) dutGen;
      assertFalse(dut0.isValid());
      assertEquals(1, dut0.insFindings.size());
      assertEquals(F_DOUBLES, dut0.insFindings.getFirst());
      dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertTrue(dut1.isValid(), dut1.insFindings::toString);
      assertTrue(dut1.insFindings.isEmpty());
      dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertTrue(dut2.isValid());
      assertTrue(dut2.insFindings.isEmpty());

      // c.2 duplicate tag at level 1
      input = "31 80  31 0a  31 04  80 00  81 00  80 00  80 00  40 01 ab  41 01 12  00 00";

      dutGen = BerTlv.getFromBuffer(ByteBuffer.wrap(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      dut0 = (DerSet) dutGen;
      assertTrue(dut0.isValid(), dut0.insFindings::toString);
      assertTrue(dut0.insFindings.isEmpty());
      dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertFalse(dut1.isValid(), dut1.insFindings::toString);
      assertEquals(1, dut1.insFindings.size());
      assertEquals(F_DOUBLES, dut1.insFindings.getFirst());
      dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertTrue(dut2.isValid());
      assertTrue(dut2.insFindings.isEmpty());

      // c.3 duplicate tag at level 2
      input = "31 80  31 0a  31 04  80 00  80 00  80 00  81 00  40 01 ab  41 01 12  00 00";

      dutGen = BerTlv.getFromBuffer(ByteBuffer.wrap(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      dut0 = (DerSet) dutGen;
      assertTrue(dut0.isValid(), dut0.insFindings::toString);
      assertTrue(dut0.insFindings.isEmpty());
      dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertTrue(dut1.isValid(), dut1.insFindings::toString);
      assertTrue(dut1.insFindings.isEmpty());
      dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertFalse(dut2.isValid(), dut2.insFindings::toString);
      assertEquals(1, dut2.insFindings.size());
      assertEquals(F_DOUBLES, dut2.insFindings.getFirst());

      // c.4 duplicate tag at level 0 and level 2
      input = "31 80  31 0a  31 04  80 00  80 00  80 00  81 00  41 01 ab  41 01 12  00 00";

      dutGen = BerTlv.getFromBuffer(ByteBuffer.wrap(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      dut0 = (DerSet) dutGen;
      assertFalse(dut0.isValid());
      assertEquals(1, dut0.insFindings.size());
      assertEquals(F_DOUBLES, dut0.insFindings.getFirst());
      dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertTrue(dut1.isValid());
      assertTrue(dut1.insFindings.isEmpty());
      dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertFalse(dut2.isValid());
      assertEquals(1, dut2.insFindings.size());
      assertEquals(F_DOUBLES, dut2.insFindings.getFirst());
    } // end --- c.

    // --- d. not correctly sorted
    {
      final var input = "31 04 (42-01-47)(02-01-47)";

      final var dut = BerTlv.getFromBuffer(ByteBuffer.wrap(Hex.toByteArray(input)));

      final var dut0 = (DerSet) dut;
      assertFalse(dut0.isValid(), dut0.insFindings::toString);
      assertEquals(1, dut0.insFindings.size());
      assertEquals(F_ORDER, dut0.insFindings.getFirst());
    } // end --- d.

    // --- e. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "-02-(81-00)", "-8102-(82-00)")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerSet(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerSet(buffer));
    } // end For (input...)
    // end --- e.
  } // end method */

  /** Test method for {@link DerSet#DerSet(InputStream)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount"})
  @Test
  void test_DerSet__InputStream() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with manually chosen input
    // --- b: ERROR: ArithmeticException
    // --- c. duplicate TLV-objects in InputStream
    // --- d. not correctly sorted
    // --- e. ERROR: IOException

    // --- a. smoke test with manually chosen input
    try {
      final var set = Set.of("00", "02  45 00", "06  04 02 4712  05 00");
      for (final var input : set) {
        final var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        final var dut = new DerSet(inputStream);

        assertEquals("31 " + input, dut.toString(" "));
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
          assertThrows(ArithmeticException.class, () -> new DerSet(inputStream));

      assertNull(thrown.getCause());
    } // end --- b.

    // --- c. duplicate tag in InputStream
    // c.0 no duplicates
    // c.1 duplicate tag at level 0
    // c.2 duplicate tag at level 1
    // c.3 duplicate tag at level 2
    // c.4 duplicate tag at level 0 and level 2
    {
      // c.0 no duplicates
      String input = "31 80  31 0a  31 04  80 00  81 00  80 00  81 00  40 01 ab  41 01 12  00 00";

      BerTlv dutGen = BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      DerSet dut0 = (DerSet) dutGen;
      assertTrue(dut0.isValid(), dut0.insFindings::toString);
      assertTrue(dut0.insFindings.isEmpty());
      DerSet dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertTrue(dut1.isValid(), dut1.insFindings::toString);
      assertTrue(dut1.insFindings.isEmpty());
      DerSet dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertTrue(dut2.isValid());
      assertTrue(dut2.insFindings.isEmpty());

      // c.1 duplicate tag at level 0
      input = "31 80  31 0a  31 04  80 00  81 00  80 00  81 00  40 01 ab  40 01 12  00 00";

      dutGen = BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      dut0 = (DerSet) dutGen;
      assertFalse(dut0.isValid());
      assertEquals(1, dut0.insFindings.size());
      assertEquals(F_DOUBLES, dut0.insFindings.getFirst());
      dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertTrue(dut1.isValid(), dut1.insFindings::toString);
      assertTrue(dut1.insFindings.isEmpty());
      dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertTrue(dut2.isValid());
      assertTrue(dut2.insFindings.isEmpty());

      // c.2 duplicate tag at level 1
      input = "31 80  31 0a  31 04  80 00  81 00  80 00  80 00  40 01 ab  41 01 12  00 00";

      dutGen = BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      dut0 = (DerSet) dutGen;
      assertTrue(dut0.isValid(), dut0.insFindings::toString);
      assertTrue(dut0.insFindings.isEmpty());
      dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertFalse(dut1.isValid(), dut1.insFindings::toString);
      assertEquals(1, dut1.insFindings.size());
      assertEquals(F_DOUBLES, dut1.insFindings.getFirst());
      dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertTrue(dut2.isValid());
      assertTrue(dut2.insFindings.isEmpty());

      // c.3 duplicate tag at level 2
      input = "31 80  31 0a  31 04  80 00  80 00  80 00  81 00  40 01 ab  41 01 12  00 00";

      dutGen = BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      dut0 = (DerSet) dutGen;
      assertTrue(dut0.isValid(), dut0.insFindings::toString);
      assertTrue(dut0.insFindings.isEmpty());
      dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertTrue(dut1.isValid(), dut1.insFindings::toString);
      assertTrue(dut1.insFindings.isEmpty());
      dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertFalse(dut2.isValid(), dut2.insFindings::toString);
      assertEquals(1, dut2.insFindings.size());
      assertEquals(F_DOUBLES, dut2.insFindings.getFirst());

      // c.4 duplicate tag at level 0 and level 2
      input = "31 80  31 0a  31 04  80 00  80 00  80 00  81 00  41 01 ab  41 01 12  00 00";

      dutGen = BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));

      assertEquals(DerSet.class, dutGen.getClass());
      dut0 = (DerSet) dutGen;
      assertFalse(dut0.isValid());
      assertEquals(1, dut0.insFindings.size());
      assertEquals(F_DOUBLES, dut0.insFindings.getFirst());
      dut1 = (DerSet) dut0.get(DerSet.TAG).orElseThrow();
      assertTrue(dut1.isValid());
      assertTrue(dut1.insFindings.isEmpty());
      dut2 = (DerSet) dut1.get(DerSet.TAG).orElseThrow();
      assertFalse(dut2.isValid());
      assertEquals(1, dut2.insFindings.size());
      assertEquals(F_DOUBLES, dut2.insFindings.getFirst());
    } // end --- c.

    // --- d. not correctly sorted
    {
      final var input = "31 04 (42-01-47)(02-01-47)";

      final var dut = BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(input)));

      final var dut0 = (DerSet) dut;
      assertFalse(dut0.isValid(), dut0.insFindings::toString);
      assertEquals(1, dut0.insFindings.size());
      assertEquals(F_ORDER, dut0.insFindings.getFirst());
    } // end --- d.

    // --- e. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerSet(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- e.
  } // end method */

  /** Test method for {@link DerSet#sort(Collection)}. */
  @Test
  void test_sort__Collection() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. random collections, mutual exclusive tags
    // --- c. random collections, double tags

    final var clazzes = ClassOfTag.values();
    final var maxIndexClazzes = clazzes.length - 1;

    // --- a. smoke test
    {
      final var uni = DerBoolean.TRUE;
      final var app = BerTlv.getInstance("41-00");
      final var con = BerTlv.getInstance("a2-00");
      final var pri = BerTlv.getInstance("c1-01-ab"); // tag-number=1, primitive
      final var prc = BerTlv.getInstance("d1-02-(04-00)"); // tag-number=1, constructed

      // mapping from input to expected
      final var map =
          Map.ofEntries(
              Map.entry(Set.of(uni), List.of(uni)), // one UNIVERSAL
              Map.entry(Set.of(app), List.of(app)), // one APPLICATION
              Map.entry(Set.of(con), List.of(con)), // one CONTEXT_SPECIFIC
              Map.entry(List.of(pri), List.of(pri)), // one PRIVATE
              Map.entry(List.of(pri, con, app, uni), List.of(uni, app, con, pri)), // wrong order
              Map.entry(List.of(pri, uni, BerTlv.getInstance("c1-01-00")), List.of(uni, pri)),
              Map.entry(List.of(pri, prc), List.of(pri, prc)),
              Map.entry(List.of(prc, pri), List.of(pri, prc)),
              Map.entry(new ArrayList<BerTlv>(), Collections.emptyList())); // empty collection

      for (final var entry : map.entrySet()) {
        final var input = entry.getKey();
        final var expected = entry.getValue();

        final var present = DerSet.sort(input);

        assertEquals(expected, present);
      } // end For (entry...)
    } // end --- a.

    { // --- b, c.
      final var maxTagNumber = 100;
      final var tags = new LinkedHashSet<Long>();
      for (final var noTags : RNG.intsClosed(2, 8 * maxTagNumber, 10).boxed().toList()) {
        // create collection of tags
        while (tags.size() < noTags) {
          final var clazz = clazzes[RNG.nextIntClosed(0, maxIndexClazzes)];
          final var isCon = RNG.nextBoolean();
          final var number = RNG.nextIntClosed(0, maxTagNumber);

          tags.add(BerTlv.convertTag(BerTlv.createTag(clazz, isCon, number)));
        } // end While (...)

        final var input = tags.stream().map(tag -> createTlv(BerTlv.convertTag(tag))).toList();

        final var present = DerSet.sort(input);

        // --- b. random collections, mutual exclusive tags
        {
          // check result
          var previous = present.getFirst();
          var preClazz = previous.getClassOfTag().getEncoding();
          for (int index = 1; index < noTags; index++) {
            final var next = present.get(index);
            final var nexClazz = next.getClassOfTag().getEncoding();
            final var clazzSorting = (preClazz <= nexClazz);

            assertTrue(clazzSorting, previous.toString(" ") + " : " + next.toString(" "));

            if (preClazz == nexClazz) {
              assertTrue(previous.getTag() < next.getTag());
            } // end fi

            previous = next;
            preClazz = nexClazz;
          } // end For (index...)
        } // end --- b.

        // --- c. random collections, double tags
        {
          final var doubles = tags.stream().map(tag -> createTlv(BerTlv.convertTag(tag))).toList();

          final var inputC = new ArrayList<>(input); // NOPMD new in loop
          inputC.addAll(doubles);

          final var preC = DerSet.sort(inputC);

          assertEquals(present, preC);
        } // end --- c.

        tags.clear();
      } // end For (noTags...)
    } // end --- b, c.
  } // end method */

  /** Test method for {@link DerSet#add(BerTlv)}. */
  @Test
  void test_add_BerTlv() {
    // Assertions:
    // ... a. toString(String)-method works as expected.

    // Test strategy:
    // --- a. add to   empty   value-field
    // --- b. add to non-empty value-field
    // --- c. ERROR: duplicate tag

    // --- initialize device-under-test
    final DerSet dut = new DerSet(new ArrayList<>());
    final List<BerTlv> vaf = dut.getTemplate();
    assertTrue(vaf.isEmpty());
    assertEquals("31 00", dut.toString(" "));
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- a. add to   empty   value-field
    final PrimitiveBerTlv tlvA = (PrimitiveBerTlv) BerTlv.getInstance("81 01 a1");
    final ConstructedBerTlv dutA = dut.add(tlvA);
    assertEquals(DerSet.class, dutA.getClass());
    final List<BerTlv> vafA = dutA.getTemplate();
    assertNotSame(dut, dutA);
    assertEquals(1, vafA.size());
    assertSame(tlvA, vafA.getFirst());
    assertEquals("31 00", dut.toString(" "));
    assertEquals("31 03  81 01 a1", dutA.toString(" "));
    assertTrue(dutA.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- b. add to non-empty value-field
    final ConstructedBerTlv tlvB = (ConstructedBerTlv) BerTlv.getInstance("a2 00");
    final ConstructedBerTlv dutB = dutA.add(tlvB);
    assertEquals(DerSet.class, dutB.getClass());
    final List<BerTlv> vafB = dutB.getTemplate();
    assertNotSame(dutA, dutB);
    assertEquals(vafA.size() + 1, vafB.size());
    assertSame(tlvA, vafB.get(0));
    assertSame(tlvB, vafB.get(1));
    assertEquals("31 00", dut.toString(" "));
    assertEquals("31 03  81 01 a1", dutA.toString(" "));
    assertEquals("31 05  81 01 a1  a2 00", dutB.toString(" "));
    assertTrue(dutB.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- c. ERROR: duplicate tag
    final Throwable throwable =
        assertThrows(
            IllegalArgumentException.class,
            () -> dutB.add(tlvB) // Spotbugs: RV_RETURN_VALUE_IGNORED_INFERRED
            );
    assertEquals("tag already present", throwable.getMessage());
    assertNull(throwable.getCause());
  } // end method */

  /**
   * Create an arbitrary TLV-object.
   *
   * @return TLV-object with given tag
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  private BerTlv createTlv(final byte[] tagField) {
    final long tag = BerTlv.convertTag(tagField);

    switch ((int) tag) {
      case DerEndOfContent.TAG: // tag-number =  0
        return DerEndOfContent.EOC;

      case DerBoolean.TAG: // tag-number =  1
        return RNG.nextBoolean() ? DerBoolean.TRUE : DerBoolean.FALSE;

      case DerInteger.TAG: // tag-number =  2
        return new DerInteger(new BigInteger(128, RNG));

      case DerBitString.TAG: // tag-number =  3
        return new DerBitString(RNG.nextIntClosed(0, 7), RNG.nextBytes(1, 10));

      case DerOctetString.TAG: // tag-number =  4
        return new DerOctetString(RNG.nextBytes(0, 10));

      case DerNull.TAG: // tag-number =  5
        return DerNull.NULL;

      case DerOid.TAG: // tag-number =  6
        return new DerOid(
            AfiOid.PREDEFINED.get(RNG.nextIntClosed(0, AfiOid.PREDEFINED.size() - 1)));

      case DerUtf8String.TAG: // tag-number = 12
        return new DerUtf8String(Hex.toHexDigits(RNG.nextBytes(0, 10)));

      case DerSequence.TAG: // tag-number = 16
        return new DerSequence(new ArrayList<>());

      case DerSet.TAG: // tag-number = 17
        return new DerSet(new ArrayList<>());

      default:
        // ... present value of tag has no specific subclass
        //     => create generic subclass
        if (0x00 == (tagField[0] & 0x20)) { // NOPMD literal in if statement
          // ... primitive
          return new PrimitiveBerTlv(tag, RNG.nextBytes(0, 10));
        } else {
          // ... constructed
          // make tag primitive
          return new ConstructedBerTlv(
              tag,
              List.of(
                  new PrimitiveBerTlv(
                      BerTlv.convertTag(
                          BerTlv.createTag(
                              ClassOfTag.APPLICATION, false, RNG.nextIntClosed(0, 128))),
                      RNG.nextBytes(0, 10))));
        } // end else
    } // end Switch (tag)
  } // end method */

  /** Test method for {@link DerSet#getComment()}. */
  @Test
  void test_getComment() throws IOException {
    // Assertions:
    // ... a. super-constructor works as expected

    // Note: Because of the assertion a and because the method-under-test is rather
    //       simple, we can be lazy here and concentrate on good code-coverage.

    // Test strategy:
    // --- a. smoke test with manually chosen input, no findings
    // --- b. smoke test with findings

    // --- a. smoke test with manually chosen input, no findings
    Map.ofEntries(
            // 0 entries
            Map.entry("31 00", "31 00 # SET with 0 elements"),
            // 1 entry
            Map.entry("31 02  05 00", String.format("31 02 # SET with 1 element%n|  05 00 # NULL")),
            // 2 entries
            Map.entry(
                "31 06  04 02 4713  05 00",
                String.format(
                    "31 06 # SET with 2 elements%n"
                        + "|  04 02 4713 # OCTETSTRING%n"
                        + "|  05 00 # NULL")))
        .forEach(
            (key, value) -> {
              final DerSet dut =
                  (DerSet) BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray(key)));
              assertEquals(value, dut.toStringTree());
              assertEquals(key, dut.toString(" "));
            }); // end forEach((key, value) -> ...)

    // --- b. smoke test with findings
    assertEquals(
        " # SET with 2 elements, findings: tags not mutual exclusive",
        BerTlv.getInstance(new ByteArrayInputStream(Hex.toByteArray("31-05  81-01-11  81-00")))
            .getComment());
  } // end method */

  /** Test method for {@link DerSet#getDecoded()}. */
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
                Map.entry("31 00", new ArrayList<>()), // 0 entries
                Map.entry("31 02  05 00", List.of(DerNull.NULL)), // 1 entry
                // 2 entries
                Map.entry(
                    "31 06  05 00  04 02 4714",
                    List.of(DerNull.NULL, new DerOctetString(Hex.toByteArray("4714")))))
            .entrySet()) {
      final var key = entry.getKey();
      final var expected = entry.getValue();
      final DerSet dut = (DerSet) BerTlv.getInstance(key);

      final var actual = dut.getDecoded();

      assertEquals(expected, actual);
    } // end For (entry...)
  } // end method */

  /** Test method for {@link DerSet#isValid()}. */
  @Test
  void test_isValid() {
    // Assertions:
    // ... a. constructor works as expected

    // Test strategy:
    // --- a. smoke test
    assertTrue(new DerSet(new ArrayList<>()).isValid());
  } // end method */
} // end class
