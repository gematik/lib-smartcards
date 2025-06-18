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

import static de.gematik.smartcards.tlv.ConstructedBerTlv.EMP;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static de.gematik.smartcards.utils.AfiUtils.VALUES_BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box tests on {@link ConstructedBerTlv}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "Non-null field is not initialized".
//         for class-attribute "claTempDir".
//         The finding is correct, but suppressed hereafter, because JUnit initializes it.
// Note 2: Spotbugs claims "Redundant nullcheck of value known to be non-null"
//         at the end of "try-with-resources" structure.
//         That seems to be a false positive.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 1
  "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", // see note 2
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.CyclomaticComplexity",
  "PMD.ExcessiveImports",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.NcssCount",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestConstructedBerTlv {

  /** Temporary Directory. */
  @TempDir
  /* package */ static Path claTempDir; // NOPMD, NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR */

  /** Prefix for class names in case of immutable lists. */
  private static final String CLASS_IMMUTABLE_LIST = "java.util.ImmutableCollections$List"; // */

  /** Value-field of a constructed TLV object used for testing {@code get...(...)}-methods. */
  private static final List<BerTlv> VALUE_FIELD =
      List.of(
          new PrimitiveBerTlv(0x81, AfiUtils.EMPTY_OS), //   index =  0
          new ConstructedBerTlv(0xa1, new ArrayList<>()), // index =  1
          new PrimitiveBerTlv(0x82, AfiUtils.EMPTY_OS), //   index =  2
          new PrimitiveBerTlv(0x83, AfiUtils.EMPTY_OS), //   index =  3
          new ConstructedBerTlv(0xa2, new ArrayList<>()), // index =  4
          new ConstructedBerTlv(0xa3, new ArrayList<>()), // index =  5
          new PrimitiveBerTlv(0x81, AfiUtils.EMPTY_OS), //   index =  6
          new ConstructedBerTlv(0xa2, new ArrayList<>()), // index =  7
          new PrimitiveBerTlv(0x81, AfiUtils.EMPTY_OS), //   index =  8
          new PrimitiveBerTlv(0x83, AfiUtils.EMPTY_OS), //   index =  9
          new ConstructedBerTlv(0xa2, new ArrayList<>()), // index = 10
          new ConstructedBerTlv(0xa2, new ArrayList<>()), // index = 11
          new PrimitiveBerTlv(0x82, AfiUtils.EMPTY_OS), //   index = 12
          new PrimitiveBerTlv(0x81, AfiUtils.EMPTY_OS), //   index = 13
          new ConstructedBerTlv(0xa3, new ArrayList<>()), // index = 14
          new PrimitiveBerTlv(0x83, AfiUtils.EMPTY_OS), //   index = 15
          new ConstructedBerTlv(0xa1, new ArrayList<>()) //  index = 16
          ); // */

  /** Device-under-test for testing {@code get...(...)}-methods. */
  private static final ConstructedBerTlv DUT = new ConstructedBerTlv(0x60, VALUE_FIELD); // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

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

  /** Test method for {@link ConstructedBerTlv#ConstructedBerTlv(byte[], ByteBuffer)}. */
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.NcssCount"
  })
  @Test
  void test_ConstructedBerTlv__byteA_ByteBuffer() {
    // Assertions:
    // ... a. The corresponding constructor from superclass works as expected.
    // ... b. "getEncoded(ByteArrayOutputStream)"-method works as expected.
    // ... c. "toString(String, String, int, boolean)"-method works as expected.

    // Note 1: Because of assertion_a here we don't have to check for tag
    //         values which do not comply to ISO/IEC 8815-1:2008.
    // Note 2: Smoke tests hereafter are chosen such that good code-coverage is achieved.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over the relevant set of tags
    // --- c. loop over various values for lengthOfValueField
    // --- d. loop over various encodings for lengthField
    // --- e. Various amounts of extra byte after a valid constructed TLV object
    // --- f. ERROR: lengthOfValueField too big
    // --- g. ERROR: not all bytes of value-field available

    ConstructedBerTlv dut;
    List<BerTlv> value;

    // --- a. smoke test
    // a.1 empty DO
    // a.2 one primitive DO
    // a.3 two primitive DO, definite form
    // a.4 three primitive DO, indefinite form
    // a.5 tree high = 2, all definite form
    // a.6 tree high = 2, all indefinite form
    // a.7 tree high = 3, some definite, some indefinite form
    // a.8 extra long length field
    // a.9 arbitrary value

    // a.1 empty DO
    // a.1.i optimal length-field, definite form
    dut = new ConstructedBerTlv(Hex.toByteArray("60"), ByteBuffer.wrap(Hex.toByteArray("-00")));

    value = dut.getTemplate();
    assertEquals("60", dut.getTagField());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(0, dut.insLengthOfValueFieldFromStream);
    assertEquals(0L, dut.getLengthOfValueField());
    assertEquals("6000", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("6000", dut.toString());
    assertEquals(0, value.size());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.1.ii long length-field, definite form
    dut =
        new ConstructedBerTlv(Hex.toByteArray("ff21"), ByteBuffer.wrap(Hex.toByteArray("-820000")));

    assertEquals("ff21", dut.getTagField());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(3, dut.insLengthOfLengthFieldFromStream);
    assertEquals(0, dut.insLengthOfValueFieldFromStream);
    assertEquals(0L, dut.getLengthOfValueField());
    assertEquals("ff2100", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("ff2100", dut.toString());
    value = dut.getTemplate();
    assertEquals(0, value.size());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.1.iii optimal length-field, indefinite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("ff818203"), ByteBuffer.wrap(Hex.toByteArray("-80 (00~00)")));

    assertEquals("ff818203", dut.getTagField());
    assertTrue(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(2, dut.insLengthOfValueFieldFromStream);
    assertEquals(0, dut.getLengthOfValueField());
    assertEquals("ff81820300", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("ff81820300", dut.toString());
    value = dut.getTemplate();
    assertEquals(0, value.size());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.2 one primitive DO
    // a.2.i optimal length-field, definite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("21"), ByteBuffer.wrap(Hex.toByteArray("-03 (81-01-02)")));

    assertEquals("21", dut.getTagField());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(3, dut.insLengthOfValueFieldFromStream);
    assertEquals(3, dut.getLengthOfValueField());
    assertEquals("2103", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("2103810102", dut.toString());
    value = dut.getTemplate();
    assertEquals(1, value.size());
    assertEquals("810102", value.getFirst().toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.2.ii long length-field, definite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("22"), ByteBuffer.wrap(Hex.toByteArray("-8105 (81-820001-03)")));

    assertEquals("22", dut.getTagField());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(2, dut.insLengthOfLengthFieldFromStream);
    assertEquals(5, dut.insLengthOfValueFieldFromStream);
    assertEquals(3, dut.getLengthOfValueField());
    assertEquals("2203", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("2203810103", dut.toString());
    value = dut.getTemplate();
    assertEquals(1, value.size());
    assertEquals("810103", value.getFirst().toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.2.iii optimal length-field, indefinite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("23"),
            ByteBuffer.wrap(Hex.toByteArray("-80 (81-820002-0203 || 0000)")));

    assertEquals("23", dut.getTagField());
    assertTrue(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(8, dut.insLengthOfValueFieldFromStream);
    assertEquals(4, dut.getLengthOfValueField());
    assertEquals("2304", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("230481020203", dut.toString());
    value = dut.getTemplate();
    assertEquals(1, value.size());
    assertEquals("81020203", value.getFirst().toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.3 two primitive DO, definite form
    // a.3.i optimal length-field, definite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("31"),
            ByteBuffer.wrap(Hex.toByteArray("-07 {(81-01-02) (82-02-0304)}")));

    assertEquals("31", dut.getTagField());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(7, dut.insLengthOfValueFieldFromStream);
    assertEquals(7, dut.getLengthOfValueField());
    assertEquals("3107", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("310781010282020304", dut.toString());
    value = dut.getTemplate();
    assertEquals(2, value.size());
    assertEquals("810102", value.get(0).toString());
    assertEquals("82020304", value.get(1).toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.3.ii long length-field, definite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("32"),
            ByteBuffer.wrap(Hex.toByteArray("-810b {(81-820001-02) (82-8103-010203)}")));

    assertEquals("32", dut.getTagField());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(2, dut.insLengthOfLengthFieldFromStream);
    assertEquals(11, dut.insLengthOfValueFieldFromStream);
    assertEquals(8, dut.getLengthOfValueField());
    assertEquals("3208", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("32088101028203010203", dut.toString());
    value = dut.getTemplate();
    assertEquals(2, value.size());
    assertEquals("810102", value.get(0).toString());
    assertEquals("8203010203", value.get(1).toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.3.iii optimal length-field, indefinite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("33"),
            ByteBuffer.wrap(Hex.toByteArray("-80 {(81-820002-0203) (40-01-fe) || 00-00)")));

    assertEquals("33", dut.getTagField());
    assertTrue(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(11, dut.insLengthOfValueFieldFromStream);
    assertEquals(7, dut.getLengthOfValueField());
    assertEquals("3307", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("3307810202034001fe", dut.toString());
    value = dut.getTemplate();
    assertEquals(2, value.size());
    assertEquals("81020203", value.get(0).toString());
    assertEquals("4001fe", value.get(1).toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.4 three primitive DO, indefinite form
    // a.4.i optimal length-field, definite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("61"),
            ByteBuffer.wrap(Hex.toByteArray("-09 {(84-01-04) (85-02-0405) (86-00)}")));

    assertFalse(dut.insIndefiniteForm);
    assertEquals("61", dut.getTagField());
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(9, dut.insLengthOfValueFieldFromStream);
    assertEquals(9, dut.getLengthOfValueField());
    assertEquals("6109", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("6109840104850204058600", dut.toString());
    value = dut.getTemplate();
    assertEquals(3, value.size());
    assertEquals("840104", value.get(0).toString());
    assertEquals("85020405", value.get(1).toString());
    assertEquals("8600", value.get(2).toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // a.4.ii long length-field, definite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("62"),
            ByteBuffer.wrap(
                Hex.toByteArray("-810f {(c4-820001-04) (c5-8103-040506) (c6-820000)}")));

    assertEquals("62", dut.getTagField());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(2, dut.insLengthOfLengthFieldFromStream);
    assertEquals(15, dut.insLengthOfValueFieldFromStream);
    assertEquals(10, dut.getLengthOfValueField());
    assertEquals("620a", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("620ac40104c503040506c600", dut.toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    value = dut.getTemplate();
    assertEquals(3, value.size());
    assertEquals("c40104", value.get(0).toString());
    assertEquals("c503040506", value.get(1).toString());
    assertEquals("c600", value.get(2).toString());

    // a.4.iii optimal length-field, indefinite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("63"),
            ByteBuffer.wrap(
                Hex.toByteArray("-80 {(41-820002-0405) (42-01-fe) (43-03-040506) || 00-00)")));

    assertEquals("63", dut.getTagField());
    assertTrue(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(16, dut.insLengthOfValueFieldFromStream);
    assertEquals(12, dut.getLengthOfValueField());
    assertEquals("630c", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("630c410204054201fe4303040506", dut.toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    value = dut.getTemplate();
    assertEquals(3, value.size());
    assertEquals("41020405", value.get(0).toString());
    assertEquals("4201fe", value.get(1).toString());
    assertEquals("4303040506", value.get(2).toString());

    // a.5 tree high = 2, all definite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("e1"),
            ByteBuffer.wrap(
                Hex.toByteArray(
                    "-11"
                        + "c1-03-050607"
                        + "e2-06"
                        + "   c2-01-05"
                        + "   c3-01-50"
                        + "c4-02-0506")));

    assertEquals("e1", dut.getTagField());
    assertFalse(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(17, dut.insLengthOfValueFieldFromStream);
    assertEquals(17, dut.getLengthOfValueField());
    assertEquals("e111", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("e111c103050607e206c20105c30150c4020506", dut.toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    value = dut.getTemplate();
    assertEquals(3, value.size());
    assertEquals("c103050607", value.get(0).toString());
    assertEquals("e206c20105c30150", value.get(1).toString());
    assertEquals("c4020506", value.get(2).toString());
    value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate();
    assertEquals(2, value.size());
    assertEquals("c20105", value.get(0).toString());
    assertEquals("c30150", value.get(1).toString());

    // a.6 tree high = 2, all indefinite form
    // a.6.i primitive at end
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("f1"),
            ByteBuffer.wrap(
                Hex.toByteArray(
                    "-80"
                        + "81-03-050607"
                        + "f2-80"
                        + "   c2-01-05"
                        + "   c3-01-50"
                        + "   00-00"
                        + "44-02-0506"
                        + "00_00")));

    assertEquals("f1", dut.getTagField());
    assertTrue(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(21, dut.insLengthOfValueFieldFromStream);
    assertEquals(17, dut.getLengthOfValueField());
    assertEquals("f111", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("f1118103050607f206c20105c3015044020506", dut.toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    value = dut.getTemplate();
    assertEquals(3, value.size());
    assertEquals("8103050607", value.get(0).toString());
    assertEquals("f206c20105c30150", value.get(1).toString());
    assertEquals("44020506", value.get(2).toString());
    value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate();
    assertEquals(2, value.size());
    assertEquals("c20105", value.get(0).toString());
    assertEquals("c30150", value.get(1).toString());

    // a.6.ii constructed at end
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("f3"),
            ByteBuffer.wrap(
                Hex.toByteArray(
                    "-80"
                        + "4a-03-050607"
                        + "4b-02-0506"
                        + "f4-80"
                        + "   4c-01-05"
                        + "   4d-01-50"
                        + "   00-00"
                        + "00.00")));

    assertEquals("f3", dut.getTagField());
    assertTrue(dut.insIndefiniteForm);
    assertEquals(1, dut.insLengthOfLengthFieldFromStream);
    assertEquals(21, dut.insLengthOfValueFieldFromStream);
    assertEquals(17, dut.getLengthOfValueField());
    assertEquals("f311", Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("f3114a030506074b020506f4064c01054d0150", dut.toString());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    value = dut.getTemplate();
    assertEquals(3, value.size());
    assertEquals("4a03050607", value.get(0).toString());
    assertEquals("4b020506", value.get(1).toString());
    assertEquals("f4064c01054d0150", value.get(2).toString());
    value = ((ConstructedBerTlv) dut.getTemplate().get(2)).getTemplate();
    assertEquals(2, value.size());
    assertEquals("4c0105", value.get(0).toString());
    assertEquals("4d0150", value.get(1).toString());

    // a.7 tree high = 3, some definite, some indefinite form
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("ff71"),
            ByteBuffer.wrap(
                Hex.toByteArray(
                    "-80"
                        + "9f72-03-070809"
                        + "ff73-82001c"
                        + "   ff74-80"
                        + "        00.00"
                        + "   ff75-80"
                        + "        ff76-8300000a"
                        + "             9f77-8101-0a"
                        + "             9f78-02-0b0c"
                        + "        81-00"
                        + "        00_00"
                        + "c0-01-0d"
                        + "00~00")));

    // check root
    BerTlv focus = dut;
    assertEquals("ff71", focus.getTagField());
    assertTrue(focus.insIndefiniteForm);
    assertEquals(1, focus.insLengthOfLengthFieldFromStream);
    assertEquals(44, focus.insLengthOfValueFieldFromStream);
    assertEquals(32, focus.getLengthOfValueField());
    assertEquals("ff7120", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals(
        Hex.extractHexDigits(
            "ff71 20"
                + "|   9f72 03 070809"
                + "|   ff73 14"
                + "|   |   ff74 00"
                + "|   |   ff75 0e"
                + "|   |   |   ff76 09"
                + "|   |   |   |   9f77 01 0a"
                + "|   |   |   |   9f78 02 0b0c"
                + "|   |   |   81 00"
                + "|   c0 01 0d"),
        focus.toString());
    value = ((ConstructedBerTlv) focus).getTemplate();
    assertEquals(3, value.size());

    // check 1st child
    focus = value.getFirst();
    assertEquals("9f72", focus.getTagField());
    assertFalse(focus.insIndefiniteForm);
    assertEquals(1, focus.insLengthOfLengthFieldFromStream);
    assertEquals(3, focus.insLengthOfValueFieldFromStream);
    assertEquals(3, focus.getLengthOfValueField());
    assertEquals("9f7203", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals("9f7203070809", focus.toString());

    // check 2nd child
    focus = value.get(1);
    assertEquals("ff73", focus.getTagField());
    assertFalse(focus.insIndefiniteForm);
    assertEquals(3, focus.insLengthOfLengthFieldFromStream);
    assertEquals(28, focus.insLengthOfValueFieldFromStream);
    assertEquals(20, focus.getLengthOfValueField());
    assertEquals("ff7314", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals(
        Hex.extractHexDigits(
            "ff73 14"
                + "|   ff74 00"
                + "|   ff75 0e"
                + "|   |   ff76 09"
                + "|   |   |   9f77 01 0a"
                + "|   |   |   9f78 02 0b0c"
                + "|   |   81 00"),
        focus.toString());

    // check 3rd child
    focus = value.get(2);
    assertEquals("c0", focus.getTagField());
    assertFalse(focus.insIndefiniteForm);
    assertEquals(1, focus.insLengthOfLengthFieldFromStream);
    assertEquals(1, focus.insLengthOfValueFieldFromStream);
    assertEquals(1, focus.getLengthOfValueField());
    assertEquals("c001", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals("c0010d", focus.toString());

    // check 2nd child's children
    value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate();
    assertEquals(2, value.size());

    // check 2nd child's 1st child
    focus = value.getFirst();
    assertEquals("ff74", focus.getTagField());
    assertTrue(focus.insIndefiniteForm);
    assertEquals(1, focus.insLengthOfLengthFieldFromStream);
    assertEquals(2, focus.insLengthOfValueFieldFromStream);
    assertEquals(0, focus.getLengthOfValueField());
    assertEquals("ff7400", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals("ff7400", focus.toString());

    // check 2nd child's 2nd child
    focus = value.get(1);
    assertEquals("ff75", focus.getTagField());
    assertTrue(focus.insIndefiniteForm);
    assertEquals(1, focus.insLengthOfLengthFieldFromStream);
    assertEquals(20, focus.insLengthOfValueFieldFromStream);
    assertEquals(14, focus.getLengthOfValueField());
    assertEquals("ff750e", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals(
        Hex.extractHexDigits(
            "ff75 0e"
                + "|   ff76 09"
                + "|   |   9f77 01 0a"
                + "|   |   9f78 02 0b0c"
                + "|   81 00"),
        focus.toString());

    // check 2nd child's 1st child's children
    value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate(); // 2nd child's children
    value = ((ConstructedBerTlv) value.getFirst()).getTemplate(); // 1st child's children
    assertEquals(0, value.size());

    // check 2nd child's 2nd child's children
    value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate(); // 2nd child's children
    value = ((ConstructedBerTlv) value.get(1)).getTemplate(); // 2nd child's children
    assertEquals(2, value.size());

    // check 2nd child's 2nd child's 1st children
    focus = value.get(0);
    assertEquals("ff76", focus.getTagField());
    assertFalse(focus.insIndefiniteForm);
    assertEquals(4, focus.insLengthOfLengthFieldFromStream);
    assertEquals(10, focus.insLengthOfValueFieldFromStream);
    assertEquals(9, focus.getLengthOfValueField());
    assertEquals("ff7609", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals(
        Hex.extractHexDigits("ff76 09" + "|   9f77 01 0a" + "|   9f78 02 0b0c"), focus.toString());

    // check 2nd child's 2nd child's 2nd children
    focus = value.get(1);
    assertEquals("81", focus.getTagField());
    assertFalse(focus.insIndefiniteForm);
    assertEquals(1, focus.insLengthOfLengthFieldFromStream);
    assertEquals(0, focus.insLengthOfValueFieldFromStream);
    assertEquals(0, focus.getLengthOfValueField());
    assertEquals("8100", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals("8100", focus.toString());

    // check 2nd child's 2nd child's 1st child's children
    value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate(); // 2nd child's children
    value = ((ConstructedBerTlv) value.get(1)).getTemplate(); // 2nd child's children
    value = ((ConstructedBerTlv) value.get(0)).getTemplate(); // 1st child's children
    assertEquals(2, value.size());

    // check 2nd child's 2nd child's 1st child's 1st child
    focus = value.getFirst();
    assertEquals("9f77", focus.getTagField());
    assertFalse(focus.insIndefiniteForm);
    assertEquals(2, focus.insLengthOfLengthFieldFromStream);
    assertEquals(1, focus.insLengthOfValueFieldFromStream);
    assertEquals(1, focus.getLengthOfValueField());
    assertEquals("9f7701", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals("9f77010a", focus.toString());

    // check 2nd child's 2nd child's 1st child's 2nd child
    focus = value.get(1);
    assertEquals("9f78", focus.getTagField());
    assertFalse(focus.insIndefiniteForm);
    assertEquals(1, focus.insLengthOfLengthFieldFromStream);
    assertEquals(2, focus.insLengthOfValueFieldFromStream);
    assertEquals(2, focus.getLengthOfValueField());
    assertEquals("9f7802", Hex.toHexDigits(focus.insTagLengthField));
    assertEquals("9f78020b0c", focus.toString());

    // a.8 extra long length field
    dut =
        new ConstructedBerTlv(
            Hex.toByteArray("23"),
            ByteBuffer.wrap(Hex.toByteArray(" 8f000000000000000000000000000006  08 04 56879646")));

    assertEquals("23 06  08 04 56879646", dut.toString(" "));

    // a.9 arbitrary value
    final String inputA9 =
        Hex.extractHexDigits(
            " 20"
                + "|   9f72 03 070809"
                + "|   ff73 14"
                + "|   |   ff74 00"
                + "|   |   ff75 0e"
                + "|   |   |   ff76 09"
                + "|   |   |   |   9f77 01 0a"
                + "|   |   |   |   9f78 02 0b0c"
                + "|   |   |   81 00"
                + "|   41 01 0d");
    dut = new ConstructedBerTlv(Hex.toByteArray("ff71"), ByteBuffer.wrap(Hex.toByteArray(inputA9)));

    assertEquals("ff71" + inputA9, dut.toString());
    assertEquals(0x20, dut.getLengthOfValueField());

    // --- b. loop over relevant set of tags
    final byte[] endOfContents = DerEndOfContent.EOC.getEncoded();
    for (final var tag : TestBerTlv.VALID_TAGS) {
      // --- c. loop over various values for lengthOfValueField
      for (final var len : RNG.intsClosed(0, 512, 10).toArray()) {
        final byte[] valueField;
        if (0 == len) {
          valueField = AfiUtils.EMPTY_OS;
        } else if (1 == len) { // NOPMD literal in if statement
          // ... len==1 but the length of value-field is two hereafter, because the
          //     value-field in a constructed TLV object is never one octet long
          valueField = Hex.toByteArray("c1 00");
        } else {
          valueField = BerTlv.getInstance(0xc0, RNG.nextBytes(len)).getEncoded();
        } // end else
        final long correctedLen = valueField.length;
        final byte[] lengthField = Hex.toByteArray(BerTlv.getLengthField(correctedLen));
        final String length = String.format("%04x", correctedLen);

        // --- d. loop over various encodings for lengthField
        final var lenFields =
            List.of(
                lengthField, // optimal encoding, i.e., as short as possible
                Hex.toByteArray("83-00" + length), // length-field on  4 byte
                Hex.toByteArray("85-000000" + length), // length-field on  6 byte
                Hex.toByteArray("8c-00000000000000000000" + length), // length-field on 13 byte
                new byte[] {(byte) 0x80}); // indefinite form
        for (final var lenField : lenFields) {
          // --- e. Various amounts of extra byte after a valid constructed TLV object
          for (final var extra : RNG.intsClosed(0, 100, 10).toArray()) {
            final byte[] eof = (((byte) 0x80) == lenField[0]) ? endOfContents : AfiUtils.EMPTY_OS;
            final byte[] suffix = RNG.nextBytes(extra);
            final byte[] input = AfiUtils.concatenate(lenField, valueField, eof, suffix);
            final var buffer = ByteBuffer.wrap(input);

            if (tag.length > BerTlv.NO_TAG_FIELD) {
              // ... tag too long
              final Throwable thrown =
                  assertThrows(
                      IllegalArgumentException.class, () -> new ConstructedBerTlv(tag, buffer));
              assertEquals("tag too long for this implementation", thrown.getMessage());
              assertNull(thrown.getCause());
            } else if (0x20 == (tag[0] & 0x20)) { // NOPMD literal in statement
              // ... valid constructed encoding
              dut = new ConstructedBerTlv(tag, buffer);

              assertEquals(Hex.toHexDigits(tag), dut.getTagField());
              assertEquals(correctedLen, dut.getLengthOfValueField());
              assertEquals(
                  dut.getTagField() + dut.getLengthField(), Hex.toHexDigits(dut.insTagLengthField));
              assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
            } else {
              // ... primitive encoding
              final var e =
                  assertThrows(
                      IllegalArgumentException.class, () -> new ConstructedBerTlv(tag, buffer));
              assertEquals(EMP, e.getMessage());
              assertNull(e.getCause());
            } // end fi
          } // end For (extra...)
        } // end For (lenField...)
      } // end For (len...)
    } // end For (tag...)

    final var tag = Hex.toByteArray("60");

    // --- f. ERROR: lengthOfValueField too big
    // f.1 supremum good value for lengthOfValueField
    {
      final var buffer =
          ByteBuffer.wrap(
              Hex.toByteArray(
                  // Note: Intentionally, the value-field is incomplete.
                  //       Long.MAX_VALUE is equivalent to eight Exbi byte and no computer
                  //       I have access to has so much memory.
                  " 887fffffffffffffff 81-00 . . ."));

      assertThrows(BufferUnderflowException.class, () -> new ConstructedBerTlv(tag, buffer));
    } // end f.1

    // f.2 infimum  bad  value for lengthOfValueField
    {
      final var buf = ByteBuffer.wrap(Hex.toByteArray(" 888000000000000000 81-00 ..."));

      final var e = assertThrows(ArithmeticException.class, () -> new ConstructedBerTlv(tag, buf));

      assertEquals(
          "length of value-field too big for this implementation: '8000000000000000'",
          e.getMessage());
      assertNull(e.getCause());
    } // end f.2

    // --- g. ERROR: not all bytes of value-field available
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(" 06 81-00"));

      assertThrows(BufferUnderflowException.class, () -> new ConstructedBerTlv(tag, buffer));
    } // end --- g.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#ConstructedBerTlv(byte[], InputStream)}. */
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.NcssCount"
  })
  @Test
  void test_ConstructedBerTlv__byteA_InputStream() {
    // Assertions:
    // ... a. The corresponding constructor from superclass works as expected.
    // ... b. "getEncoded(ByteArrayOutputStream)"-method works as expected.
    // ... c. "toString(String, String, int, boolean)"-method works as expected.

    // Note 1: Because of assertion_a here we don't have to check for tag
    //         values which do not comply to ISO/IEC 8815-1:2008.
    // Note 2: Smoke tests hereafter are chosen such that good code-coverage is achieved.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over the relevant set of tags
    // --- c. loop over various values for lengthOfValueField
    // --- d. loop over various encodings for lengthField
    // --- e. Various amounts of extra byte after a valid constructed TLV object
    // --- f. ERROR: lengthOfValueField too big
    // --- g. ERROR: read from ByteArrayInputStream, not all bytes of value-field available
    // --- h. ERROR: read from file,                 not all bytes of value-field available

    ConstructedBerTlv dut;
    List<BerTlv> value;

    try {
      // --- a. smoke test
      // a.1 empty DO
      // a.2 one primitive DO
      // a.3 two primitive DO, definite form
      // a.4 three primitive DO, indefinite form
      // a.5 tree high = 2, all definite form
      // a.6 tree high = 2, all indefinite form
      // a.7 tree high = 3, some definite, some indefinite form
      // a.8 extra long length field
      // a.9 arbitrary value

      // a.1 empty DO
      // a.1.i optimal length-field, definite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("60"), new ByteArrayInputStream(Hex.toByteArray("-00")));

      value = dut.getTemplate();
      assertEquals("60", dut.getTagField());
      assertFalse(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(0, dut.insLengthOfValueFieldFromStream);
      assertEquals(0L, dut.getLengthOfValueField());
      assertEquals("6000", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("6000", dut.toString());
      assertEquals(0, value.size());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.1.ii long length-field, definite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("ff21"), new ByteArrayInputStream(Hex.toByteArray("-820000")));

      assertEquals("ff21", dut.getTagField());
      assertFalse(dut.insIndefiniteForm);
      assertEquals(3, dut.insLengthOfLengthFieldFromStream);
      assertEquals(0, dut.insLengthOfValueFieldFromStream);
      assertEquals(0L, dut.getLengthOfValueField());
      assertEquals("ff2100", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("ff2100", dut.toString());
      value = dut.getTemplate();
      assertEquals(0, value.size());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.1.iii optimal length-field, indefinite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("ff818203"),
              new ByteArrayInputStream(Hex.toByteArray("-80 (00~00)")));

      assertEquals("ff818203", dut.getTagField());
      assertTrue(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(2, dut.insLengthOfValueFieldFromStream);
      assertEquals(0, dut.getLengthOfValueField());
      assertEquals("ff81820300", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("ff81820300", dut.toString());
      value = dut.getTemplate();
      assertEquals(0, value.size());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.2 one primitive DO
      // a.2.i optimal length-field, definite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("21"), new ByteArrayInputStream(Hex.toByteArray("-03 (81-01-02)")));

      assertEquals("21", dut.getTagField());
      assertFalse(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(3, dut.insLengthOfValueFieldFromStream);
      assertEquals(3, dut.getLengthOfValueField());
      assertEquals("2103", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("2103810102", dut.toString());
      value = dut.getTemplate();
      assertEquals(1, value.size());
      assertEquals("810102", value.getFirst().toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.2.ii long length-field, definite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("22"),
              new ByteArrayInputStream(Hex.toByteArray("-8105 (81-820001-03)")));

      assertEquals("22", dut.getTagField());
      assertFalse(dut.insIndefiniteForm);
      assertEquals(2, dut.insLengthOfLengthFieldFromStream);
      assertEquals(5, dut.insLengthOfValueFieldFromStream);
      assertEquals(3, dut.getLengthOfValueField());
      assertEquals("2203", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("2203810103", dut.toString());
      value = dut.getTemplate();
      assertEquals(1, value.size());
      assertEquals("810103", value.getFirst().toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.2.iii optimal length-field, indefinite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("23"),
              new ByteArrayInputStream(Hex.toByteArray("-80 (81-820002-0203 || 0000)")));

      assertEquals("23", dut.getTagField());
      assertTrue(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(8, dut.insLengthOfValueFieldFromStream);
      assertEquals(4, dut.getLengthOfValueField());
      assertEquals("2304", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("230481020203", dut.toString());
      value = dut.getTemplate();
      assertEquals(1, value.size());
      assertEquals("81020203", value.getFirst().toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.3 two primitive DO, definite form
      // a.3.i optimal length-field, definite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("31"),
              new ByteArrayInputStream(Hex.toByteArray("-07 {(81-01-02) (82-02-0304)}")));

      assertEquals("31", dut.getTagField());
      assertFalse(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(7, dut.insLengthOfValueFieldFromStream);
      assertEquals(7, dut.getLengthOfValueField());
      assertEquals("3107", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("310781010282020304", dut.toString());
      value = dut.getTemplate();
      assertEquals(2, value.size());
      assertEquals("810102", value.get(0).toString());
      assertEquals("82020304", value.get(1).toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.3.ii long length-field, definite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("32"),
              new ByteArrayInputStream(Hex.toByteArray("-810b {(81-820001-02) (82-8103-010203)}")));

      assertEquals("32", dut.getTagField());
      assertFalse(dut.insIndefiniteForm);
      assertEquals(2, dut.insLengthOfLengthFieldFromStream);
      assertEquals(11, dut.insLengthOfValueFieldFromStream);
      assertEquals(8, dut.getLengthOfValueField());
      assertEquals("3208", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("32088101028203010203", dut.toString());
      value = dut.getTemplate();
      assertEquals(2, value.size());
      assertEquals("810102", value.get(0).toString());
      assertEquals("8203010203", value.get(1).toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.3.iii optimal length-field, indefinite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("33"),
              new ByteArrayInputStream(
                  Hex.toByteArray("-80 {(81-820002-0203) (40-01-fe) || 00-00)")));

      assertEquals("33", dut.getTagField());
      assertTrue(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(11, dut.insLengthOfValueFieldFromStream);
      assertEquals(7, dut.getLengthOfValueField());
      assertEquals("3307", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("3307810202034001fe", dut.toString());
      value = dut.getTemplate();
      assertEquals(2, value.size());
      assertEquals("81020203", value.get(0).toString());
      assertEquals("4001fe", value.get(1).toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.4 three primitive DO, indefinite form
      // a.4.i optimal length-field, definite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("61"),
              new ByteArrayInputStream(Hex.toByteArray("-09 {(84-01-04) (85-02-0405) (86-00)}")));

      assertFalse(dut.insIndefiniteForm);
      assertEquals("61", dut.getTagField());
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(9, dut.insLengthOfValueFieldFromStream);
      assertEquals(9, dut.getLengthOfValueField());
      assertEquals("6109", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("6109840104850204058600", dut.toString());
      value = dut.getTemplate();
      assertEquals(3, value.size());
      assertEquals("840104", value.get(0).toString());
      assertEquals("85020405", value.get(1).toString());
      assertEquals("8600", value.get(2).toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

      // a.4.ii long length-field, definite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("62"),
              new ByteArrayInputStream(
                  Hex.toByteArray("-810f {(c4-820001-04) (c5-8103-040506) (c6-820000)}")));

      assertEquals("62", dut.getTagField());
      assertFalse(dut.insIndefiniteForm);
      assertEquals(2, dut.insLengthOfLengthFieldFromStream);
      assertEquals(15, dut.insLengthOfValueFieldFromStream);
      assertEquals(10, dut.getLengthOfValueField());
      assertEquals("620a", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("620ac40104c503040506c600", dut.toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
      value = dut.getTemplate();
      assertEquals(3, value.size());
      assertEquals("c40104", value.get(0).toString());
      assertEquals("c503040506", value.get(1).toString());
      assertEquals("c600", value.get(2).toString());

      // a.4.iii optimal length-field, indefinite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("63"),
              new ByteArrayInputStream(
                  Hex.toByteArray("-80 {(41-820002-0405) (42-01-fe) (43-03-040506) || 00-00)")));

      assertEquals("63", dut.getTagField());
      assertTrue(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(16, dut.insLengthOfValueFieldFromStream);
      assertEquals(12, dut.getLengthOfValueField());
      assertEquals("630c", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("630c410204054201fe4303040506", dut.toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
      value = dut.getTemplate();
      assertEquals(3, value.size());
      assertEquals("41020405", value.get(0).toString());
      assertEquals("4201fe", value.get(1).toString());
      assertEquals("4303040506", value.get(2).toString());

      // a.5 tree high = 2, all definite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("e1"),
              new ByteArrayInputStream(
                  Hex.toByteArray(
                      "-11"
                          + "c1-03-050607"
                          + "e2-06"
                          + "   c2-01-05"
                          + "   c3-01-50"
                          + "c4-02-0506")));

      assertEquals("e1", dut.getTagField());
      assertFalse(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(17, dut.insLengthOfValueFieldFromStream);
      assertEquals(17, dut.getLengthOfValueField());
      assertEquals("e111", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("e111c103050607e206c20105c30150c4020506", dut.toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
      value = dut.getTemplate();
      assertEquals(3, value.size());
      assertEquals("c103050607", value.get(0).toString());
      assertEquals("e206c20105c30150", value.get(1).toString());
      assertEquals("c4020506", value.get(2).toString());
      value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate();
      assertEquals(2, value.size());
      assertEquals("c20105", value.get(0).toString());
      assertEquals("c30150", value.get(1).toString());

      // a.6 tree high = 2, all indefinite form
      // a.6.i primitive at end
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("f1"),
              new ByteArrayInputStream(
                  Hex.toByteArray(
                      "-80"
                          + "81-03-050607"
                          + "f2-80"
                          + "   c2-01-05"
                          + "   c3-01-50"
                          + "   00-00"
                          + "44-02-0506"
                          + "00_00")));

      assertEquals("f1", dut.getTagField());
      assertTrue(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(21, dut.insLengthOfValueFieldFromStream);
      assertEquals(17, dut.getLengthOfValueField());
      assertEquals("f111", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("f1118103050607f206c20105c3015044020506", dut.toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
      value = dut.getTemplate();
      assertEquals(3, value.size());
      assertEquals("8103050607", value.get(0).toString());
      assertEquals("f206c20105c30150", value.get(1).toString());
      assertEquals("44020506", value.get(2).toString());
      value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate();
      assertEquals(2, value.size());
      assertEquals("c20105", value.get(0).toString());
      assertEquals("c30150", value.get(1).toString());

      // a.6.ii constructed at end
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("f3"),
              new ByteArrayInputStream(
                  Hex.toByteArray(
                      "-80"
                          + "4a-03-050607"
                          + "4b-02-0506"
                          + "f4-80"
                          + "   4c-01-05"
                          + "   4d-01-50"
                          + "   00-00"
                          + "00.00")));

      assertEquals("f3", dut.getTagField());
      assertTrue(dut.insIndefiniteForm);
      assertEquals(1, dut.insLengthOfLengthFieldFromStream);
      assertEquals(21, dut.insLengthOfValueFieldFromStream);
      assertEquals(17, dut.getLengthOfValueField());
      assertEquals("f311", Hex.toHexDigits(dut.insTagLengthField));
      assertEquals("f3114a030506074b020506f4064c01054d0150", dut.toString());
      assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
      value = dut.getTemplate();
      assertEquals(3, value.size());
      assertEquals("4a03050607", value.get(0).toString());
      assertEquals("4b020506", value.get(1).toString());
      assertEquals("f4064c01054d0150", value.get(2).toString());
      value = ((ConstructedBerTlv) dut.getTemplate().get(2)).getTemplate();
      assertEquals(2, value.size());
      assertEquals("4c0105", value.get(0).toString());
      assertEquals("4d0150", value.get(1).toString());

      // a.7 tree high = 3, some definite, some indefinite form
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("ff71"),
              new ByteArrayInputStream(
                  Hex.toByteArray(
                      "-80"
                          + "9f72-03-070809"
                          + "ff73-82001c"
                          + "   ff74-80"
                          + "        00.00"
                          + "   ff75-80"
                          + "        ff76-8300000a"
                          + "             9f77-8101-0a"
                          + "             9f78-02-0b0c"
                          + "        81-00"
                          + "        00_00"
                          + "c0-01-0d"
                          + "00~00")));

      // check root
      BerTlv focus = dut;
      assertEquals("ff71", focus.getTagField());
      assertTrue(focus.insIndefiniteForm);
      assertEquals(1, focus.insLengthOfLengthFieldFromStream);
      assertEquals(44, focus.insLengthOfValueFieldFromStream);
      assertEquals(32, focus.getLengthOfValueField());
      assertEquals("ff7120", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals(
          Hex.extractHexDigits(
              "ff71 20"
                  + "|   9f72 03 070809"
                  + "|   ff73 14"
                  + "|   |   ff74 00"
                  + "|   |   ff75 0e"
                  + "|   |   |   ff76 09"
                  + "|   |   |   |   9f77 01 0a"
                  + "|   |   |   |   9f78 02 0b0c"
                  + "|   |   |   81 00"
                  + "|   c0 01 0d"),
          focus.toString());
      value = ((ConstructedBerTlv) focus).getTemplate();
      assertEquals(3, value.size());

      // check 1st child
      focus = value.getFirst();
      assertEquals("9f72", focus.getTagField());
      assertFalse(focus.insIndefiniteForm);
      assertEquals(1, focus.insLengthOfLengthFieldFromStream);
      assertEquals(3, focus.insLengthOfValueFieldFromStream);
      assertEquals(3, focus.getLengthOfValueField());
      assertEquals("9f7203", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals("9f7203070809", focus.toString());

      // check 2nd child
      focus = value.get(1);
      assertEquals("ff73", focus.getTagField());
      assertFalse(focus.insIndefiniteForm);
      assertEquals(3, focus.insLengthOfLengthFieldFromStream);
      assertEquals(28, focus.insLengthOfValueFieldFromStream);
      assertEquals(20, focus.getLengthOfValueField());
      assertEquals("ff7314", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals(
          Hex.extractHexDigits(
              "ff73 14"
                  + "|   ff74 00"
                  + "|   ff75 0e"
                  + "|   |   ff76 09"
                  + "|   |   |   9f77 01 0a"
                  + "|   |   |   9f78 02 0b0c"
                  + "|   |   81 00"),
          focus.toString());

      // check 3rd child
      focus = value.get(2);
      assertEquals("c0", focus.getTagField());
      assertFalse(focus.insIndefiniteForm);
      assertEquals(1, focus.insLengthOfLengthFieldFromStream);
      assertEquals(1, focus.insLengthOfValueFieldFromStream);
      assertEquals(1, focus.getLengthOfValueField());
      assertEquals("c001", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals("c0010d", focus.toString());

      // check 2nd child's children
      value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate();
      assertEquals(2, value.size());

      // check 2nd child's 1st child
      focus = value.getFirst();
      assertEquals("ff74", focus.getTagField());
      assertTrue(focus.insIndefiniteForm);
      assertEquals(1, focus.insLengthOfLengthFieldFromStream);
      assertEquals(2, focus.insLengthOfValueFieldFromStream);
      assertEquals(0, focus.getLengthOfValueField());
      assertEquals("ff7400", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals("ff7400", focus.toString());

      // check 2nd child's 2nd child
      focus = value.get(1);
      assertEquals("ff75", focus.getTagField());
      assertTrue(focus.insIndefiniteForm);
      assertEquals(1, focus.insLengthOfLengthFieldFromStream);
      assertEquals(20, focus.insLengthOfValueFieldFromStream);
      assertEquals(14, focus.getLengthOfValueField());
      assertEquals("ff750e", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals(
          Hex.extractHexDigits(
              "ff75 0e"
                  + "|   ff76 09"
                  + "|   |   9f77 01 0a"
                  + "|   |   9f78 02 0b0c"
                  + "|   81 00"),
          focus.toString());

      // check 2nd child's 1st child's children
      value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate(); // 2nd child's children
      value = ((ConstructedBerTlv) value.getFirst()).getTemplate(); // 1st child's children
      assertEquals(0, value.size());

      // check 2nd child's 2nd child's children
      value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate(); // 2nd child's children
      value = ((ConstructedBerTlv) value.get(1)).getTemplate(); // 2nd child's children
      assertEquals(2, value.size());

      // check 2nd child's 2nd child's 1st children
      focus = value.get(0);
      assertEquals("ff76", focus.getTagField());
      assertFalse(focus.insIndefiniteForm);
      assertEquals(4, focus.insLengthOfLengthFieldFromStream);
      assertEquals(10, focus.insLengthOfValueFieldFromStream);
      assertEquals(9, focus.getLengthOfValueField());
      assertEquals("ff7609", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals(
          Hex.extractHexDigits("ff76 09" + "|   9f77 01 0a" + "|   9f78 02 0b0c"),
          focus.toString());

      // check 2nd child's 2nd child's 2nd children
      focus = value.get(1);
      assertEquals("81", focus.getTagField());
      assertFalse(focus.insIndefiniteForm);
      assertEquals(1, focus.insLengthOfLengthFieldFromStream);
      assertEquals(0, focus.insLengthOfValueFieldFromStream);
      assertEquals(0, focus.getLengthOfValueField());
      assertEquals("8100", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals("8100", focus.toString());

      // check 2nd child's 2nd child's 1st child's children
      value = ((ConstructedBerTlv) dut.getTemplate().get(1)).getTemplate(); // 2nd child's children
      value = ((ConstructedBerTlv) value.get(1)).getTemplate(); // 2nd child's children
      value = ((ConstructedBerTlv) value.get(0)).getTemplate(); // 1st child's children
      assertEquals(2, value.size());

      // check 2nd child's 2nd child's 1st child's 1st child
      focus = value.getFirst();
      assertEquals("9f77", focus.getTagField());
      assertFalse(focus.insIndefiniteForm);
      assertEquals(2, focus.insLengthOfLengthFieldFromStream);
      assertEquals(1, focus.insLengthOfValueFieldFromStream);
      assertEquals(1, focus.getLengthOfValueField());
      assertEquals("9f7701", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals("9f77010a", focus.toString());

      // check 2nd child's 2nd child's 1st child's 2nd child
      focus = value.get(1);
      assertEquals("9f78", focus.getTagField());
      assertFalse(focus.insIndefiniteForm);
      assertEquals(1, focus.insLengthOfLengthFieldFromStream);
      assertEquals(2, focus.insLengthOfValueFieldFromStream);
      assertEquals(2, focus.getLengthOfValueField());
      assertEquals("9f7802", Hex.toHexDigits(focus.insTagLengthField));
      assertEquals("9f78020b0c", focus.toString());

      // a.8 extra long length field
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("23"),
              new ByteArrayInputStream(
                  Hex.toByteArray(" 8f000000000000000000000000000006  08 04 56879646")));

      assertEquals("23 06  08 04 56879646", dut.toString(" "));

      // a.9 arbitrary value
      final String inputA9 =
          Hex.extractHexDigits(
              " 20"
                  + "|   9f72 03 070809"
                  + "|   ff73 14"
                  + "|   |   ff74 00"
                  + "|   |   ff75 0e"
                  + "|   |   |   ff76 09"
                  + "|   |   |   |   9f77 01 0a"
                  + "|   |   |   |   9f78 02 0b0c"
                  + "|   |   |   81 00"
                  + "|   41 01 0d");
      dut =
          new ConstructedBerTlv(
              Hex.toByteArray("ff71"), new ByteArrayInputStream(Hex.toByteArray(inputA9)));

      assertEquals("ff71" + inputA9, dut.toString());
      assertEquals(0x20, dut.getLengthOfValueField());

      // --- b. loop over relevant set of tags
      final byte[] endOfContents = DerEndOfContent.EOC.getEncoded();
      for (final var tag : TestBerTlv.VALID_TAGS) {
        // --- c. loop over various values for lengthOfValueField
        for (final var len : RNG.intsClosed(0, 512, 10).toArray()) {
          final byte[] valueField;
          if (0 == len) {
            valueField = AfiUtils.EMPTY_OS;
          } else if (1 == len) { // NOPMD literal in if statement
            // ... len==1 but the length of value-field is two hereafter, because the
            //     value-field in a constructed TLV object is never one octet long
            valueField = Hex.toByteArray("c1 00");
          } else {
            valueField = BerTlv.getInstance(0xc0, RNG.nextBytes(len)).getEncoded();
          } // end else
          final long correctedLen = valueField.length;
          final byte[] lengthField = Hex.toByteArray(BerTlv.getLengthField(correctedLen));
          final String length = String.format("%04x", correctedLen);

          // --- d. loop over various encodings for lengthField
          final var lenFields =
              List.of(
                  lengthField, // optimal encoding, i.e., as short as possible
                  Hex.toByteArray("83-00" + length), // length-field on  4 byte
                  Hex.toByteArray("85-000000" + length), // length-field on  6 byte
                  Hex.toByteArray("8c-00000000000000000000" + length), // length-field on 13 byte
                  new byte[] {(byte) 0x80}); // indefinite form
          for (final var lenField : lenFields) {
            // --- e. Various amounts of extra byte after valid constructed TLV object
            for (final var extra : RNG.intsClosed(0, 100, 10).toArray()) {
              try {
                final byte[] eof =
                    (((byte) 0x80) == lenField[0]) ? endOfContents : AfiUtils.EMPTY_OS;
                final byte[] suffix = RNG.nextBytes(extra);
                final byte[] input = AfiUtils.concatenate(lenField, valueField, eof, suffix);
                final InputStream inputStream = new ByteArrayInputStream(input);

                if (tag.length > BerTlv.NO_TAG_FIELD) {
                  // ... tag too long
                  final Throwable thrown =
                      assertThrows(
                          IllegalArgumentException.class,
                          () -> new ConstructedBerTlv(tag, inputStream));
                  assertEquals("tag too long for this implementation", thrown.getMessage());
                  assertNull(thrown.getCause());
                } else if (0x20 == (tag[0] & 0x20)) { // NOPMD literal in statement
                  // ... valid constructed encoding
                  dut = new ConstructedBerTlv(tag, inputStream);

                  assertEquals(Hex.toHexDigits(tag), dut.getTagField());
                  assertEquals(correctedLen, dut.getLengthOfValueField());
                  assertEquals(
                      dut.getTagField() + dut.getLengthField(),
                      Hex.toHexDigits(dut.insTagLengthField));
                  assertTrue(
                      dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
                } else {
                  // ... primitive encoding
                  final Throwable thrown =
                      assertThrows(
                          IllegalArgumentException.class,
                          () -> new ConstructedBerTlv(tag, inputStream));
                  assertEquals(EMP, thrown.getMessage());
                  assertNull(thrown.getCause());
                } // end fi
              } catch (IOException e) {
                fail(UNEXPECTED, e);
              } // end Catch (...)
            } // end For (extra...)
          } // end For (lenField...)
        } // end For (len...)
      } // end For (tag...)
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    final var tag = Hex.toByteArray("60");

    // --- f. ERROR: lengthOfValueField too big
    // f.1 supremum good value for lengthOfValueField
    {
      final var is =
          new ByteArrayInputStream(
              Hex.toByteArray(
                  // Note: Intentionally the value-field is incomplete.
                  //       Long.MAX_VALUE is equivalent to eight Exbi byte and no computer
                  //       I have access to has so much memory.
                  " 887fffffffffffffff 81-00 . . ."));

      final var e =
          assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(tag, is));

      assertEquals("unexpected IOException", e.getMessage());
    } // end f.1

    // f.2 infimum  bad  value for lengthOfValueField
    {
      final var is = new ByteArrayInputStream(Hex.toByteArray(" 888000000000000000 81-00 ..."));

      final var e = assertThrows(ArithmeticException.class, () -> new ConstructedBerTlv(tag, is));

      assertEquals(
          "length of value-field too big for this implementation: '8000000000000000'",
          e.getMessage());
      assertNull(e.getCause());
    } // end f.2

    // --- g. ERROR: read from ByteArrayInputStream, not all bytes of value-field available
    {
      final var is = new ByteArrayInputStream(Hex.toByteArray(" 06 81-00"));

      final var e =
          assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(tag, is));

      assertEquals("unexpected IOException", e.getMessage());
    } // end --- g.

    // --- h. ERROR: read from file,                 not all bytes of value-field available
    try {
      final Path path = claTempDir.resolve("input.bin");
      Files.write(path, Hex.toByteArray(" 06 [(81-00)...]"));
      try (var is = Files.newInputStream(path)) {
        final var e =
            assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(tag, is));
        assertEquals("unexpected IOException", e.getMessage());
      } // end try-with-resources, RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link ConstructedBerTlv#ConstructedBerTlv(long, Collection)}. */
  @Test
  void test_ConstructedBerTlv__long_Collection() {
    // Assertions:
    // ... a. Super-constructor BerTlv(long, long) works as expected.
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test, empty list
    // --- b. smoke test, non-empty list
    // --- c. check for defensive cloning
    // --- d. ERROR: Primitive tag
    // --- e. ERROR: value-field too long

    ConstructedBerTlv dut;
    long tag;
    final List<BerTlv> value;

    // --- a. Smoke test, empty list
    tag = 0xa1;
    value = new ArrayList<>();
    dut = new ConstructedBerTlv(tag, value);
    assertEquals(tag, dut.getTag());
    assertEquals(value, dut.insValueField);
    assertEquals(String.format("%02x00", tag), Hex.toHexDigits(dut.insTagLengthField));
    assertEquals(String.format("%02x00", tag), dut.toString());
    assertEquals(0L, dut.getLengthOfValueField());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- b. Smoke test, non-empty list
    tag = 0xb0;
    value.add(BerTlv.getInstance("c7 02 a43c"));
    dut = new ConstructedBerTlv(tag, value);
    assertEquals(tag, dut.getTag());
    assertEquals(value, dut.insValueField);
    assertEquals(String.format("%02x04", tag), Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("b0 04  c7 02 a43c", dut.toString(" "));
    assertEquals(4L, dut.getLengthOfValueField());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- c. check for defensive cloning
    tag = 0xb1;
    dut = new ConstructedBerTlv(tag, value);
    assertEquals(tag, dut.getTag());
    assertEquals(value, dut.insValueField);
    assertNotSame(value, dut.insValueField);
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- d. ERROR: Primitive tag
    {
      final var input = new ArrayList<BerTlv>();

      final Throwable thrown =
          assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(0x40, input));
      assertEquals(EMP, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- d.

    // --- d. ERROR: value-field too long
    final String sumLengthTlvD =
        BigInteger.ONE.add(BigInteger.valueOf(Long.MAX_VALUE)).toString(16);
    List.of(
            // Note: size = 1 is not possible, because a single TLV-object with
            //       lengthOfTlvObject > Long.MAX_VALUE causes ArithmeticException in
            //       getLengthOfTlvObject()-method.
            // size = 2
            List.of(
                BerTlv.getInstance("52 03 112233"),
                new TestBerTlv.MyBerTlv(0xdf52, Long.MAX_VALUE - 15)),
            // size = 3
            List.of(
                BerTlv.getInstance("53 04 11223344"),
                new TestBerTlv.MyBerTlv(0xdf53, Long.MAX_VALUE - 18),
                BerTlv.getInstance("e5 00")))
        .forEach(
            list -> {
              // assure that sum of lengths of TLV-objects is beyond int-range
              assertEquals(
                  sumLengthTlvD,
                  list.stream()
                      .map(tlv -> BigInteger.valueOf(tlv.getLengthOfTlvObject()))
                      .reduce(BigInteger::add)
                      .orElse(BigInteger.ZERO)
                      .toString(16));
              final Throwable thrownD =
                  assertThrows(ArithmeticException.class, () -> BerTlv.getInstance(0x64, list));
              assertEquals("BigInteger out of long range", thrownD.getMessage());
              assertNull(thrownD.getCause());
            }); // forEach(list -> ...)
  } // end method */

  /*
   * Test method for {@link ConstructedBerTlv#ConstructedBerTlv(long)}.
   *
  @Test
  void test_ConstructedBerTlv__long() {
    // Assertions:
    // ... a. Super-constructor BerTlv(long, long) works as expected.

    // Simple constructor doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. Manually chosen values for smoke test
    // --- b. ERROR: Primitive tag

    // --- a. Manually chosen values for smoke test
    final long tag = 0xff21;
    final ConstructedBerTlv dut = new ConstructedBerTlv(tag);
    assertEquals(tag, dut.getTag());
    assertTrue(dut.getValue().isEmpty());
    assertEquals("ff2100", Hex.toHexDigits(dut.toByteArray()));
    assertEquals(0L, dut.getLengthOfValueField());
    assertTrue(dut.insValue.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- b. ERROR: Primitive tag
    final Throwable thrown = assertThrows(
        IllegalArgumentException.class,
        () -> new ConstructedBerTlv(0x00)
    );
    assertEquals(EMC, thrown.getMessage());
    assertNull(thrown.getCause());
  } // end method */

  /** Test method for {@link ConstructedBerTlv#ConstructedBerTlv(long, byte[])}. */
  @Test
  void test_ConstructedBerTlv__long_byteA() {
    // Assertions:
    // ... a. BerTlv(long, long)-constructor from superclass works as expected
    // ... b. BerTlv.getInstance(InputStream)-method         works as expected
    // ... c. toString(String)-method                        works as expected

    // Test strategy:
    // --- a. empty value-field
    // --- b. one TLV object in value-field
    // --- c. two TLV objects in value-field
    // --- d. ERROR: extra byte at end of octet string
    // --- e. ERROR: primitive tag

    ConstructedBerTlv dut;

    // --- a. empty value-field
    long tag = 0xe1;
    dut = new ConstructedBerTlv(tag, AfiUtils.EMPTY_OS);
    assertEquals(tag, dut.getTag());
    assertTrue(dut.getTemplate().isEmpty());
    assertEquals(String.format("%02x00", tag), Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("e1 00", dut.toString(" "));
    assertEquals(0L, dut.getLengthOfValueField());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- b. one TLV object in value-field
    tag = 0xe2;
    List<BerTlv> value = List.of(BerTlv.getInstance("41-8102-1112"));
    dut =
        new ConstructedBerTlv(
            tag,
            Hex.toByteArray(value.stream().map(BerTlv::toString).collect(Collectors.joining())));
    assertEquals("e2 04  41 02 1112", dut.toString(" "));
    assertEquals(4L, dut.getLengthOfValueField());
    assertEquals(tag, dut.getTag());
    assertEquals(1, dut.getTemplate().size());
    assertEquals(String.format("%02x04", tag), Hex.toHexDigits(dut.insTagLengthField));
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- c. two TLV objects in value-field
    tag = 0xe3;
    value =
        List.of(
            BerTlv.getInstance("43-820003-313233"),
            BerTlv.getInstance("23-80-(44-01-44  45-02-5152 00-00)"));
    dut =
        new ConstructedBerTlv(
            tag,
            Hex.toByteArray(value.stream().map(BerTlv::toString).collect(Collectors.joining())));
    assertEquals(tag, dut.getTag());
    assertEquals(2, dut.getTemplate().size());
    assertEquals(String.format("%02x0e", tag), Hex.toHexDigits(dut.insTagLengthField));
    assertEquals("e3 0e  43 03 313233  23 07  44 01 44  45 02 5152", dut.toString(" "));
    assertEquals(14L, dut.getLengthOfValueField());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- d. ERROR: extra byte at end of octet string
    final StringBuilder builder = new StringBuilder("82-01-11 || 5f");
    // d.1 ERROR: incomplete tag
    {
      final var input = Hex.toByteArray(builder.toString());

      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(0x20, input));

      assertEquals("unexpected IOException", thrown.getMessage());
    } // end d.1

    // d.2 ERROR: length-field missing
    {
      final var input = Hex.toByteArray(builder.append("20").toString());

      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(0x20, input));

      assertEquals("unexpected IOException", thrown.getMessage());
    } // end d.2

    // d.3 ERROR: incomplete length-field
    {
      final var input = Hex.toByteArray(builder.append("81").toString());

      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(0x20, input));

      assertEquals("unexpected IOException", thrown.getMessage());
    } // end d.3

    // d.4 ERROR: value-field missing
    {
      final var input = Hex.toByteArray(builder.append("02").toString());

      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(0x20, input));

      assertEquals("unexpected IOException", thrown.getMessage());
    } // end d.4

    // d.5 ERROR: incomplete value-field
    {
      final var input = Hex.toByteArray(builder.append("11").toString());

      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(0x20, input));

      assertEquals("unexpected IOException", thrown.getMessage());
    } // end d.5

    // d.6 NoError: complete second TLV object
    dut = new ConstructedBerTlv(0x20, Hex.toByteArray(builder.append("23").toString()));
    assertEquals("20 08  82 01 11  5f20 02 1123", dut.toString(" "));
    assertEquals(8L, dut.getLengthOfValueField());

    // --- e. ERROR: primitive tag
    {
      final var input = Hex.toByteArray("09-01-42");

      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new ConstructedBerTlv(0x40, input));

      assertEquals(EMP, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- e.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#add(BerTlv)}. */
  @Test
  void test_add_BerTlv() {
    // Assertions:
    // ... a. toString(String)-method works as expected.

    // Test strategy:
    // --- a. add to   empty   value-field
    // --- b. add to non-empty value-field

    // --- initialize device-under-test
    final ConstructedBerTlv dut = new ConstructedBerTlv(0xa0, new ArrayList<>());
    final List<BerTlv> vaf = dut.getTemplate();
    assertTrue(vaf.isEmpty());
    assertEquals("a0 00", dut.toString(" "));
    assertEquals(ConstructedBerTlv.class, dut.getClass());
    assertTrue(dut.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- a. add to   empty   value-field
    final BerTlv tlvA = BerTlv.getInstance("81 01 a1");
    final ConstructedBerTlv dutA = dut.add(tlvA);
    final List<BerTlv> vafA = dutA.getTemplate();
    assertNotSame(dut, dutA);
    assertEquals(1, vafA.size());
    assertSame(tlvA, vafA.getFirst());
    assertEquals("a0 00", dut.toString(" "));
    assertEquals("a0 03  81 01 a1", dutA.toString(" "));
    assertEquals(ConstructedBerTlv.class, dut.getClass());
    assertTrue(dutA.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));

    // --- b. add to non-empty value-field
    final BerTlv tlvB = new ConstructedBerTlv(0xa2, new ArrayList<>());
    final ConstructedBerTlv dutB = dutA.add(tlvB);
    final List<BerTlv> vafB = dutB.getTemplate();
    assertNotSame(dutA, dutB);
    assertEquals(vafA.size() + 1, vafB.size());
    assertSame(tlvA, vafB.get(0));
    assertSame(tlvB, vafB.get(1));
    assertEquals("a0 00", dut.toString(" "));
    assertEquals("a0 03  81 01 a1", dutA.toString(" "));
    assertEquals("a0 05  81 01 a1  a2 00", dutB.toString(" "));
    assertEquals(ConstructedBerTlv.class, dut.getClass());
    assertTrue(dutB.insValueField.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
  } // end method */

  /** Test method for {@link ConstructedBerTlv#createTag(ClassOfTag, long)}. */
  @Test
  void test_createTag__ClassOfTag_long() {
    // Assertions:
    // ... a. Underlying method BerTlv.createTag(ClassOfTag, boolean, long) works as expected.

    // Note: This simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all class of tag
    // --- c. loop over all numbers for one byte tag

    // --- a. smoke test
    {
      final var classOfTag = ClassOfTag.APPLICATION;
      final var number = 260;
      final var expected = "7f8204";

      final var present = Hex.toHexDigits(ConstructedBerTlv.createTag(classOfTag, number));

      assertEquals(expected, present);
    } // end --- a.

    // --- b. loop over all class of tag
    Stream.of(ClassOfTag.values())
        .forEach(
            clazz -> {
              long number = 0;
              // --- c. loop over all numbers for one byte tag
              for (; number < 0x1f; number++) {
                assertEquals(
                    String.format("%02x", clazz.getEncoding() + 0x20 + number),
                    Hex.toHexDigits(ConstructedBerTlv.createTag(clazz, number)));
              } // end For (number...)
            }); // end forEach(class -> ...)
  } // end method */

  /** Test method for {@link ConstructedBerTlv#equals(java.lang.Object)}. */
  @Test
  void test_equals__Object() {
    // Assertions:
    // ... a. equals()-method of superclass works as expected
    // ... b. equals()-method from PrimitiveBerTlv works as expected

    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. difference in tag
    // --- e. difference in value field length
    // --- f. difference in value field content
    // --- g. different object but same tag and value-field

    // --- create device under test (DUT)
    final String lengthValue = "-08-[81-01-01  22-03-(82-01-10)]";
    final ConstructedBerTlv dut = (ConstructedBerTlv) BerTlv.getInstance("21" + lengthValue);

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
            Map.entry(BerTlv.getInstance("22" + lengthValue), false),
            // --- e. difference in value field length
            Map.entry(BerTlv.getInstance("21-09-[81-02-0102  22-03-(82-01-10)]"), false),

            // --- f. difference in value field content
            // f.1 difference in tag of primitive child
            Map.entry(BerTlv.getInstance("21-08-[82-01-01  22-03-(82-01-10)]"), false),
            // f.2 difference in tag of constructed child
            Map.entry(BerTlv.getInstance("21-08-[81-01-01  23-03-(82-01-10)]"), false),
            // f.3 difference in value field of primitive child
            Map.entry(BerTlv.getInstance("21-08-[81-01-02  22-03-(82-01-10)]"), false),

            // f.4 difference in value field of constructed child
            // f.4.i   difference in tag
            Map.entry(BerTlv.getInstance("21-08-[81-01-01  22-03-(83-01-10)]"), false),
            // f.4.ii  difference in length-field
            Map.entry(BerTlv.getInstance("21-09-[81-01-01  22-04-(82-02-1011)]"), false),
            // f.4.iii difference in value-field
            Map.entry(BerTlv.getInstance("21-08-[81-01-01  22-03-(82-01-11)]"), false),
            // --- g. different object but same tag and value-field
            Map.entry(BerTlv.getInstance("21" + lengthValue), true))
        .forEach(
            (tlv, expected) -> {
              assertEquals(ConstructedBerTlv.class, tlv.getClass());
              assertNotSame(dut, tlv);
              assertEquals(expected, dut.equals(tlv));
            }); // end forEach((tlv, expected) -> ...)
  } // end method */

  /** Test method for {@link ConstructedBerTlv#hashCode()}. */
  @Test
  void test_hashCode() {
    // Test strategy:
    // --- a. loop over relevant subset of available tags, empty value-field
    // --- b. loop over various amount of entries in value-field
    // --- c. call hashCode()-method again

    // --- a. loop over relevant subset of available tags, empty value-field
    final byte[] lengthField = new byte[1];
    TestBerTlv.VALID_TAGS.stream()
        .filter(tagField -> (tagField.length <= BerTlv.NO_TAG_FIELD)) // tag short enough
        .filter(tagField -> (0x00 != (tagField[0] & 0x20))) // primitive encoding
        .forEach(
            tagField -> {
              final byte[] input = AfiUtils.concatenate(tagField, lengthField);
              final ConstructedBerTlv dut = (ConstructedBerTlv) BerTlv.getInstance(input);
              final long tag = dut.getTag();
              final int msInt = (int) (tag >> 32);
              final int lsInt = (int) tag;

              final int expectedHash = msInt * 31 + lsInt;
              assertEquals(expectedHash, dut.hashCode());
            }); // end forEach(tagField -> ...)

    // --- b. loop over various amount of entries in value-field
    // Note: Hereafter a constructed TLV object is created bottom up. It is assumed, that
    //       hashCode for empty value-fields are correct (that is tested above).

    // b.1 constructed with (some) primitive children.
    final PrimitiveBerTlv priA00 = new PrimitiveBerTlv(0x40, RNG.nextBytes(0, 16));
    final PrimitiveBerTlv priA01 = new PrimitiveBerTlv(0x41, RNG.nextBytes(0, 16));
    ConstructedBerTlv conA0 = new ConstructedBerTlv(0x60, new ArrayList<>());
    int hashCode = conA0.hashCode();
    hashCode = hashCode * 31 + priA00.hashCode();
    hashCode = hashCode * 31 + priA01.hashCode();
    conA0 = conA0.add(priA00).add(priA01);
    final int hashCodeA0 = conA0.hashCode();
    assertEquals(hashCode, hashCodeA0);

    // b.2 constructed with (some) primitive children
    final PrimitiveBerTlv priB00 = new PrimitiveBerTlv(0x50, RNG.nextBytes(0, 16));
    final PrimitiveBerTlv priB01 = new PrimitiveBerTlv(0x51, RNG.nextBytes(0, 16));
    final PrimitiveBerTlv priB02 = new PrimitiveBerTlv(0x52, RNG.nextBytes(0, 16));
    ConstructedBerTlv conB0 = new ConstructedBerTlv(0x70, new ArrayList<>());
    hashCode = conB0.hashCode();
    hashCode = hashCode * 31 + priB00.hashCode();
    hashCode = hashCode * 31 + priB01.hashCode();
    hashCode = hashCode * 31 + priB02.hashCode();
    conB0 = conB0.add(priB00).add(priB01).add(priB02);
    final int hashCodeB0 = conB0.hashCode();
    assertEquals(hashCode, hashCodeB0);

    // b.3 constructed with primitive and constructed children
    final PrimitiveBerTlv priC00 = new PrimitiveBerTlv(0x80, RNG.nextBytes(0, 16));
    ConstructedBerTlv conC0 = new ConstructedBerTlv(0xa0, new ArrayList<>());
    hashCode = conC0.hashCode();
    hashCode = hashCode * 31 + hashCodeA0;
    hashCode = hashCode * 31 + priC00.hashCode();
    hashCode = hashCode * 31 + hashCodeB0;

    // Note: Intentionally, all TLV objects hereafter are freshly generated.
    conC0 =
        conC0
            // conA0, freshly generated
            .add(
                new ConstructedBerTlv(conA0.getTag(), new ArrayList<>())
                    .add(new PrimitiveBerTlv(priA00.getTag(), priA00.getValueField()))
                    .add(new PrimitiveBerTlv(priA01.getTag(), priA01.getValueField())))
            // priC00, freshly generated
            .add(new PrimitiveBerTlv(priC00.getTag(), priC00.getValueField()))
            // conB0, freshly generated
            .add(
                new ConstructedBerTlv(conB0.getTag(), new ArrayList<>())
                    .add(new PrimitiveBerTlv(priB00.getTag(), priB00.getValueField()))
                    .add(new PrimitiveBerTlv(priB01.getTag(), priB01.getValueField()))
                    .add(new PrimitiveBerTlv(priB02.getTag(), priB02.getValueField())));
    assertEquals(hashCode, conC0.hashCode());

    // --- c. call hashCode()-method again
    final ConstructedBerTlv conC = new ConstructedBerTlv(0xf0, new ArrayList<>());
    hashCode = conC.hashCode();
    assertEquals(hashCode, conC.hashCode());
  } // end method */

  /** Test method for {@link ConstructedBerTlv#get(long)}. */
  @Test
  void test_get__long() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note 1: Because of assertion_a we can be lazy here and do some smoke tests.
    // Note 2: Here the static object DUT is used.
    // Note 3: Tests on randomly generated TLV objects are performed TODO

    // Test strategy:
    // --- a.   existing    primitive  tag
    // --- b.   existing   constructed tag
    // --- c. non-existing  primitive  tag
    // --- d. non-existing constructed tag

    Optional<BerTlv> result;

    // --- a.   existing    primitive  tag
    result = DUT.get(0x82);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(2), result.get());

    // --- b.   existing   constructed tag
    result = DUT.get(0xa3);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(5), result.get());

    // --- c. non-existing  primitive  tag
    result = DUT.get(0x80);
    assertTrue(result.isEmpty());

    // --- d. non-existing constructed tag
    result = DUT.get(0xa0);
    assertTrue(result.isEmpty());
  } // end method */

  /** Test method for {@link ConstructedBerTlv#get(long, int)}. */
  @SuppressWarnings({"PMD.NcssCount"})
  @Test
  void test_get__long_int() {
    // Note 1: This test method does some smoke test.
    // Note 2: Here the static object DUT is used.
    // Note 3: Tests on randomly generated TLV objects are performed in method
    //         TODO

    // Test strategy:
    // --- a. all existing  primitive  tag
    // --- b. all existing constructed tag
    // --- c. non-existing  primitive  tag
    // --- d. non-existing constructed tag

    long tag;
    int pos;

    // --- a. all existing  primitive  tag
    // a.1 DO'81'
    // a.1.i   1st DO'81'
    tag = 0x81;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<BerTlv> resA = DUT.get(0x81, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.getFirst(), resA.get());
            }); // end forEach(position -> ...)

    // a.1.ii  2nd DO'81'
    Optional<BerTlv> result;
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(6), result.get());

    // a.1.iii 3rd DO'81'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(8), result.get());

    // a.1.iv  4th DO'81'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(13), result.get());

    // a.1.v   5th DO'81'
    result = DUT.get(tag, pos);
    assertTrue(result.isEmpty());

    // a.2 DO'82'
    // a.2.i   1st DO'82'
    tag = 0x82;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<BerTlv> resA = DUT.get(0x82, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(2), resA.get());
            }); // end forEach(position -> ...)

    // a.2.ii  2nd DO'82'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(12), result.get());

    // a.2.iii 3rd DO'82'
    result = DUT.get(tag, pos);
    assertTrue(result.isEmpty());

    // a.3 DO'83'
    // a.3.i   1st DO'83'
    tag = 0x83;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<BerTlv> resA = DUT.get(0x83, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(3), resA.get());
            }); // end forEach(position -> ...)

    // a.3.ii  2nd DO'83'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(9), result.get());

    // a.3.iii 3rd DO'83'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(15), result.get());

    // a.3.iv  4th DO'83'
    result = DUT.get(tag, pos);
    assertTrue(result.isEmpty());

    // --- b. all existing constructed tag
    // b.1 DO'a1'
    // b.1.i   1st DO'a1'
    tag = 0xa1;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<BerTlv> resA = DUT.get(0xa1, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(1), resA.get());
            }); // end forEach(position -> ...)

    // b.1.ii  2nd DO'a1'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(16), result.get());

    // b.1.iii 3rd DO'a1'
    result = DUT.get(tag, pos);
    assertTrue(result.isEmpty());

    // b.2 DO'a2'
    // b.2.i   1st DO'a2'
    tag = 0xa2;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<BerTlv> resA = DUT.get(0xa2, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(4), resA.get());
            }); // end forEach(position -> ...)

    // b.2.ii  2nd DO'a2'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(7), result.get());

    // b.2.iii  3rd DO'a2'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(10), result.get());

    // b.2.iv  4th DO'a2'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(11), result.get());

    // b.2.v   5th DO'a2'
    result = DUT.get(tag, pos);
    assertTrue(result.isEmpty());

    // b.3 DO'a3'
    // b.3.i   1st DO'a3'
    tag = 0xa3;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<BerTlv> resA = DUT.get(0xa3, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(5), resA.get());
            }); // end forEach(position -> ...)

    // b.3.ii  2nd DO'a3'
    result = DUT.get(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(14), result.get());

    // b.3.iii 3rd DO'a3'
    result = DUT.get(tag, pos);
    assertTrue(result.isEmpty());

    // --- c. non-existing  primitive  tag
    List.of(Integer.MIN_VALUE, -1, 0, 1, 2)
        .forEach(
            position -> {
              final Optional<BerTlv> resA = DUT.get(0x80, position);
              assertTrue(resA.isEmpty());
            }); // end forEach(position -> ...)

    // --- d. non-existing constructed tag
    List.of(Integer.MIN_VALUE, -1, 0, 1, 2)
        .forEach(
            position -> {
              final Optional<BerTlv> resA = DUT.get(0xa0, position);
              assertTrue(resA.isEmpty());
            }); // end forEach(position -> ...)
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getConstructed(long)}. */
  @Test
  void test_getConstructed__long() {
    // Assertions:
    // ... a. getConstructed(long, int)-method works as expected

    // Note 1: Because of assertion_a we can be lazy here and do some smoke tests.
    // Note 2: Here the static object DUT is used.
    // Note 3: Tests on randomly generated TLV objects are performed in method
    //         TODO

    // Test strategy:
    // --- a.   existing    primitive  tag
    // --- b.   existing   constructed tag
    // --- c. non-existing  primitive  tag
    // --- d. non-existing constructed tag

    Optional<ConstructedBerTlv> result;

    // --- a.   existing    primitive  tag
    assertThrows(ClassCastException.class, () -> DUT.getConstructed(0x82));

    // --- b.   existing   constructed tag
    result = DUT.getConstructed(0xa3);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(5), result.get());

    // --- c. non-existing  primitive  tag
    result = DUT.getConstructed(0x80);
    assertTrue(result.isEmpty());

    // --- d. non-existing constructed tag
    result = DUT.getConstructed(0xa0);
    assertTrue(result.isEmpty());
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getConstructed(long, int)}. */
  @Test
  void test_getConstructed__long_int() {
    // Note 1: This test method does some smoke test.
    // Note 2: Here the static object DUT is used.
    // Note 3: Tests on randomly generated TLV objects are performed in method
    //         TODO

    // Test strategy:
    // --- a. all existing  primitive  tag
    // --- b. all existing constructed tag
    // --- c. non-existing  primitive  tag
    // --- d. non-existing constructed tag

    long tag;

    // --- a. all existing  primitive  tag
    // a.1 DO'81'
    // a.1.i   1st DO'81'
    tag = 0x81;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position ->
                assertThrows(
                    ClassCastException.class,
                    () -> DUT.getConstructed(0x81, position))); // end forEach(position -> ...)

    // a.1.ii  2nd DO'81'
    assertThrows(ClassCastException.class, () -> DUT.getConstructed(0x81, 1));

    // a.1.iii 3rd DO'81'
    assertThrows(ClassCastException.class, () -> DUT.getConstructed(0x81, 2));

    // a.1.iv  4th DO'81'
    assertThrows(ClassCastException.class, () -> DUT.getConstructed(0x81, 3));

    // a.1.v   5th DO'81'
    Optional<ConstructedBerTlv> result;
    result = DUT.getConstructed(tag, 4);
    assertTrue(result.isEmpty());

    // a.2 DO'82'
    // a.2.i   1st DO'82'
    tag = 0x82;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position ->
                assertThrows(
                    ClassCastException.class,
                    () -> DUT.getConstructed(0x82, position))); // end forEach(position -> ...)

    // a.2.ii  2nd DO'82'
    assertThrows(ClassCastException.class, () -> DUT.getConstructed(0x82, 1));

    // a.2.iii 3rd DO'82'
    result = DUT.getConstructed(tag, 2);
    assertTrue(result.isEmpty());

    // a.3 DO'83'
    // a.3.i   1st DO'83'
    tag = 0x83;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position ->
                assertThrows(
                    ClassCastException.class,
                    () -> DUT.getConstructed(0x83, position))); // end forEach(position -> ...)

    // a.3.ii  2nd DO'83'
    assertThrows(ClassCastException.class, () -> DUT.getConstructed(0x83, 1));

    // a.3.iii 3rd DO'83'
    assertThrows(ClassCastException.class, () -> DUT.getConstructed(0x83, 2));

    // a.3.iv  4th DO'83'
    result = DUT.getConstructed(tag, 3);
    assertTrue(result.isEmpty());

    // --- b. all existing constructed tag
    // b.1 DO'a1'
    // b.1.i   1st DO'a1'
    tag = 0xa1;
    int pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<ConstructedBerTlv> resA = DUT.getConstructed(0xa1, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(1), resA.get());
            }); // end forEach(position -> ...)

    // b.1.ii  2nd DO'a1'
    result = DUT.getConstructed(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(16), result.get());

    // b.1.iii 3rd DO'a1'
    result = DUT.getConstructed(tag, pos);
    assertTrue(result.isEmpty());

    // b.2 DO'a2'
    // b.2.i   1st DO'a2'
    tag = 0xa2;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<ConstructedBerTlv> resA = DUT.getConstructed(0xa2, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(4), resA.get());
            }); // end forEach(position -> ...)

    // b.2.ii  2nd DO'a2'
    result = DUT.getConstructed(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(7), result.get());

    // b.2.iii  3rd DO'a2'
    result = DUT.getConstructed(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(10), result.get());

    // b.2.iv  4th DO'a2'
    result = DUT.getConstructed(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(11), result.get());

    // b.2.v   5th DO'a2'
    result = DUT.getConstructed(tag, pos);
    assertTrue(result.isEmpty());

    // b.3 DO'a3'
    // b.3.i   1st DO'a3'
    tag = 0xa3;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<ConstructedBerTlv> resA = DUT.getConstructed(0xa3, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(5), resA.get());
            }); // end forEach(position -> ...)

    // b.3.ii  2nd DO'a3'
    result = DUT.getConstructed(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(14), result.get());

    // b.3.iii 3rd DO'a3'
    result = DUT.getConstructed(tag, pos);
    assertTrue(result.isEmpty());

    // --- c. non-existing  primitive  tag
    List.of(Integer.MIN_VALUE, -1, 0, 1, 2)
        .forEach(
            position -> {
              final Optional<ConstructedBerTlv> resA = DUT.getConstructed(0x80, position);
              assertTrue(resA.isEmpty());
            }); // end forEach(position -> ...)

    // --- d. non-existing constructed tag
    List.of(Integer.MIN_VALUE, -1, 0, 1, 2)
        .forEach(
            position -> {
              final Optional<ConstructedBerTlv> resA = DUT.getConstructed(0xa0, position);
              assertTrue(resA.isEmpty());
            }); // end forEach(position -> ...)
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getPrimitive(long)}. */
  @Test
  void test_getPrimitive__long() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note 1: Because of assertion_a we can be lazy here and do some smoke tests.
    // Note 2: Here the static object DUT is used.
    // Note 3: Tests on randomly generated TLV objects are performed in method
    //         TODO

    // Test strategy:
    // --- a.   existing    primitive  tag
    // --- b.   existing   constructed tag
    // --- c. non-existing  primitive  tag
    // --- d. non-existing constructed tag

    Optional<PrimitiveBerTlv> result;

    // --- a.   existing    primitive  tag
    result = DUT.getPrimitive(0x82);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(2), result.get());

    // --- b.   existing   constructed tag
    assertThrows(ClassCastException.class, () -> DUT.getPrimitive(0xa3));

    // --- c. non-existing  primitive  tag
    result = DUT.getPrimitive(0x80);
    assertTrue(result.isEmpty());

    // --- d. non-existing constructed tag
    result = DUT.getPrimitive(0xa0);
    assertTrue(result.isEmpty());
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getPrimitive(long, int)}. */
  @Test
  void test_getPrimitive__long_int() {
    // Note 1: This test method does some smoke test.
    // Note 2: Here the static object DUT is used.
    // Note 3: Tests on randomly generated TLV objects are performed in method
    //         TODO

    // Test strategy:
    // --- a. all existing  primitive  tag
    // --- b. all existing constructed tag
    // --- c. non-existing  primitive  tag
    // --- d. non-existing constructed tag

    long tag;
    int pos;

    // --- a. all existing  primitive  tag
    // a.1 DO'81'
    // a.1.i   1st DO'81'
    tag = 0x81;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<PrimitiveBerTlv> resA = DUT.getPrimitive(0x81, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.getFirst(), resA.get());
            }); // end forEach(position -> ...)

    // a.1.ii  2nd DO'81'
    Optional<PrimitiveBerTlv> result;
    result = DUT.getPrimitive(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(6), result.get());

    // a.1.iii 3rd DO'81'
    result = DUT.getPrimitive(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(8), result.get());

    // a.1.iv  4th DO'81'
    result = DUT.getPrimitive(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(13), result.get());

    // a.1.v   5th DO'81'
    result = DUT.getPrimitive(tag, pos);
    assertTrue(result.isEmpty());

    // a.2 DO'82'
    // a.2.i   1st DO'82'
    tag = 0x82;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<PrimitiveBerTlv> resA = DUT.getPrimitive(0x82, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(2), resA.get());
            }); // end forEach(position -> ...)

    // a.2.ii  2nd DO'82'
    result = DUT.getPrimitive(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(12), result.get());

    // a.2.iii 3rd DO'82'
    result = DUT.getPrimitive(tag, pos);
    assertTrue(result.isEmpty());

    // a.3 DO'83'
    // a.3.i   1st DO'83'
    tag = 0x83;
    pos = 1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position -> {
              final Optional<PrimitiveBerTlv> resA = DUT.getPrimitive(0x83, position);
              assertTrue(resA.isPresent());
              assertSame(VALUE_FIELD.get(3), resA.get());
            }); // end forEach(position -> ...)

    // a.3.ii  2nd DO'83'
    result = DUT.getPrimitive(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(9), result.get());

    // a.3.iii 3rd DO'83'
    result = DUT.getPrimitive(tag, pos++);
    assertTrue(result.isPresent());
    assertSame(VALUE_FIELD.get(15), result.get());

    // a.3.iv  4th DO'83'
    result = DUT.getPrimitive(tag, pos);
    assertTrue(result.isEmpty());

    // --- b. all existing constructed tag
    // b.1 DO'a1'
    // b.1.i   1st DO'a1'
    tag = 0xa1;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position ->
                assertThrows(
                    ClassCastException.class,
                    () -> DUT.getPrimitive(0xa1, position))); // end forEach(position -> ...)

    // b.1.ii  2nd DO'a1'
    assertThrows(ClassCastException.class, () -> DUT.getPrimitive(0xa1, 1));

    // b.1.iii 3rd DO'a1'
    result = DUT.getPrimitive(tag, 2);
    assertTrue(result.isEmpty());

    // b.2 DO'a2'
    // b.2.i   1st DO'a2'
    tag = 0xa2;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position ->
                assertThrows(
                    ClassCastException.class,
                    () -> DUT.getPrimitive(0xa2, position))); // end forEach(position -> ...)

    // b.2.ii  2nd DO'a2'
    assertThrows(ClassCastException.class, () -> DUT.getPrimitive(0xa2, 1));

    // b.2.iii  3rd DO'a2'
    assertThrows(ClassCastException.class, () -> DUT.getPrimitive(0xa2, 2));

    // b.2.iv  4th DO'a2'
    assertThrows(ClassCastException.class, () -> DUT.getPrimitive(0xa2, 3));

    // b.2.v   5th DO'a2'
    result = DUT.getPrimitive(tag, 4);
    assertTrue(result.isEmpty());

    // b.3 DO'a3'
    // b.3.i   1st DO'a3'
    tag = 0xa3;
    List.of(Integer.MIN_VALUE, -1, 0)
        .forEach(
            position ->
                assertThrows(
                    ClassCastException.class,
                    () -> DUT.getPrimitive(0xa3, position))); // end forEach(position -> ...)

    // b.3.ii  2nd DO'a3'
    assertThrows(ClassCastException.class, () -> DUT.getPrimitive(0xa3, 1));

    // b.3.iii 3rd DO'a3'
    result = DUT.getPrimitive(tag, 2);
    assertTrue(result.isEmpty());

    // --- c. non-existing  primitive  tag
    List.of(Integer.MIN_VALUE, -1, 0, 1, 2)
        .forEach(
            position -> {
              final Optional<PrimitiveBerTlv> resA = DUT.getPrimitive(0x80, position);
              assertTrue(resA.isEmpty());
            }); // end forEach(position -> ...)

    // --- d. non-existing constructed tag
    List.of(Integer.MIN_VALUE, -1, 0, 1, 2)
        .forEach(
            position -> {
              final Optional<PrimitiveBerTlv> resA = DUT.getPrimitive(0xa0, position);
              assertTrue(resA.isEmpty());
            }); // end forEach(position -> ...)
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getValueField()}. */
  @Test
  void test_getValueField() {
    // Assertions:
    // ... a. ConstructedBerTlv(String)-constructor works as expected
    // ... b. toByteArray(OutputStream)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. some manual chosen input

    // --- a. smoke test
    {
      final var expected = Hex.toByteArray("81-01-af");
      final var dut = new ConstructedBerTlv(0x20, expected);

      final var present = dut.getValueField();

      assertArrayEquals(expected, present);
    } // end --- a.

    // --- b. some manual chosen input
    Map.ofEntries(
            Map.entry(
                "fa 00", // empty value-field
                ""),
            Map.entry(
                "fa-02-(d1-00)", // one primitive DO
                "d1-00"),
            Map.entry(
                "fb-0d-[d2-01-04   fc-03-(80-01-11)  d3-03-123456]",
                "[d2-01-04   fc-03-(80-01-11)  d3-03-123456]"))
        .forEach(
            (tlv, valueField) -> {
              final ConstructedBerTlv dut = (ConstructedBerTlv) BerTlv.getInstance(tlv);

              assertEquals(
                  Hex.extractHexDigits(valueField), Hex.toHexDigits(dut.getValueField()), tlv);
            });
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getTemplate()}. */
  @Test
  void test_getTemplate() {
    // Assertions:
    // ... a. ConstructedBerTlv(List, long)-constructor works as expected

    // Test strategy:
    // --- a. smoke test with empty list
    // --- b. smoke test with non-empty list

    ConstructedBerTlv dut;
    final List<BerTlv> exp;

    // --- a. smoke test with empty list
    exp = new ArrayList<>();
    dut = new ConstructedBerTlv(0x2a, exp);
    final List<BerTlv> preA = dut.getTemplate();
    // intentionally assertNotSame is not tested here, because list is empty
    assertEquals(exp, preA);
    assertTrue(preA.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
    final var input = BerTlv.getInstance("cb 00");
    assertThrows(UnsupportedOperationException.class, () -> preA.add(input));

    // --- b. smoke test with non-empty list
    exp.add(BerTlv.getInstance("41 00"));
    assertEquals(1, exp.size());
    dut = new ConstructedBerTlv(0x2a, exp);
    final List<BerTlv> preB = dut.getTemplate();
    assertNotSame(exp, dut.insValueField);
    assertEquals(exp, preB);
    assertTrue(preB.getClass().getName().startsWith(CLASS_IMMUTABLE_LIST));
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getBitString()}. */
  @Test
  void test_getBitString() {
    // Assertions:
    // ... a. getBitString(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerBitString(RNG.nextBytes(16, 32));
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getBitString();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(83-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getBitString);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getBitString(int)}. */
  @Test
  void test_getBitString__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerBitString(RNG.nextBytes(16, 32));

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(
              List.of(
                  new DerBitString(RNG.nextBytes(16, 32)),
                  expected,
                  new DerBitString(RNG.nextBytes(16, 32))));

      final var actual = dut.getBitString(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getBitString(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getBoolean()}. */
  @Test
  void test_getBoolean() {
    // Assertions:
    // ... a. getBoolean(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = DerBoolean.TRUE;
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getBoolean();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(81-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getBoolean);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getBoolean(int)}. */
  @Test
  void test_getBoolean__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = DerBoolean.FALSE;

    // --- a. smoke test
    {
      final var dut = new DerSequence(List.of(DerBoolean.FALSE, expected));

      final var actual = dut.getBoolean(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getBoolean(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getDate()}. */
  @Test
  void test_getDate() {
    // Assertions:
    // ... a. getDate(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerDate(LocalDate.now());
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getDate();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-04-(9f1f-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getDate);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getDate(int)}. */
  @Test
  void test_getDate__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerDate(LocalDate.EPOCH);

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(
              List.of(
                  new DerDate(LocalDate.now()),
                  expected,
                  new DerDate(LocalDate.now().plusDays(1))));

      final var actual = dut.getDate(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getDate(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getEndOfContent()}. */
  @Test
  void test_getEndOfContent() {
    // Assertions:
    // ... a. getEndOfContent(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = DerEndOfContent.EOC;
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getEndOfContent();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(80-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getEndOfContent);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getEndOfContent(int)}. */
  @Test
  void test_getEndOfContent__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = DerEndOfContent.EOC;

    // --- a. smoke test
    {
      final var dut = new DerSequence(List.of(expected));

      final var actual = dut.getEndOfContent(0);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getEndOfContent(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getIa5String()}. */
  @Test
  void test_getIa5String() {
    // Assertions:
    // ... a. getIa5String(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerIa5String("Foo Bar");
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getIa5String();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(96-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getIa5String);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getIa5String(int)}. */
  @Test
  void test_getIa5String__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerIa5String("4711 0815");

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(List.of(new DerIa5String("0815"), expected, new DerIa5String("4711")));

      final var actual = dut.getIa5String(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getIa5String(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getInteger()}. */
  @Test
  void test_getInteger() {
    // Assertions:
    // ... a. getInteger(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerInteger(BigInteger.TEN);
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getInteger();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(82-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getInteger);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getInteger(int)}. */
  @Test
  void test_getInteger__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerInteger(BigInteger.ONE);

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(
              List.of(new DerInteger(BigInteger.TWO), expected, new DerInteger(BigInteger.ZERO)));

      final var actual = dut.getInteger(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getInteger(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getNull()}. */
  @Test
  void test_getNull() {
    // Assertions:
    // ... a. getNull(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = DerNull.NULL;
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getNull();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(85-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getNull);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getNull(int)}. */
  @Test
  void test_getNull__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = DerNull.NULL;

    // --- a. smoke test
    {
      final var dut = new DerSequence(List.of(expected));

      final var actual = dut.getNull(0);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getNull(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getOctetString()}. */
  @Test
  void test_getOctetString() {
    // Assertions:
    // ... a. getOctetString(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerOctetString(RNG.nextBytes(16, 32));
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getOctetString();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(84-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getOctetString);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getOctetString(int)}. */
  @Test
  void test_getOctetString__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerOctetString(RNG.nextBytes(16, 32));

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(
              List.of(
                  new DerOctetString(RNG.nextBytes(16, 32)),
                  expected,
                  new DerOctetString(RNG.nextBytes(16, 32))));

      final var actual = dut.getOctetString(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getOctetString(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getOid()}. */
  @Test
  void test_getOid() {
    // Assertions:
    // ... a. getOid(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerOid(AfiOid.ansix9p256r1);
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getOid();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(86-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getOid);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getOid(int)}. */
  @Test
  void test_getOid__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerOid(AfiOid.ansix9p521r1);

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(List.of(new DerOid(AfiOid.ak_aut), expected, new DerOid(AfiOid.ak_ghcs)));

      final var actual = dut.getOid(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getOid(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getPrintableString()}. */
  @Test
  void test_getPrintableString() {
    // Assertions:
    // ... a. getPrintableString(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerPrintableString("Bar Foo");
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getPrintableString();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(93-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getPrintableString);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getPrintableString(int)}. */
  @Test
  void test_getPrintableString__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerPrintableString("4712 0815");

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(
              List.of(new DerPrintableString("0816"), expected, new DerPrintableString("4710")));

      final var actual = dut.getPrintableString(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getPrintableString(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getSequence()}. */
  @Test
  void test_getSequence() {
    // Assertions:
    // ... a. getSequence(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerSequence(List.of(DerNull.NULL));
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getSequence();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-04-(b0-02-8000)");

      assertThrows(NoSuchElementException.class, dut::getSequence);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getSequence(int)}. */
  @Test
  void test_getSequence__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerSequence(List.of(DerBoolean.FALSE));

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(
              List.of(
                  new DerSequence(List.of(DerBoolean.TRUE)),
                  expected,
                  new DerSequence(List.of(DerNull.NULL))));

      final var actual = dut.getSequence(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getSequence(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getSet()}. */
  @Test
  void test_getSet() {
    // Assertions:
    // ... a. getSet(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerSet(List.of(DerNull.NULL));
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getSet();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-04-(b1-02-8000)");

      assertThrows(NoSuchElementException.class, dut::getSet);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getSet(int)}. */
  @Test
  void test_getSet__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerSet(List.of(DerBoolean.FALSE));

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(
              List.of(
                  new DerSet(List.of(DerBoolean.TRUE)),
                  expected,
                  new DerSet(List.of(DerNull.NULL))));

      final var actual = dut.getSet(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getSet(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getTeletexString()}. */
  @Test
  void test_getTeletexString() {
    // Assertions:
    // ... a. getTeletexString(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerTeletexString("Foo bar.");
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getTeletexString();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(94-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getTeletexString);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getTeletexString(int)}. */
  @Test
  void test_getTeletexString__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerTeletexString("4717 0815");

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(
              List.of(new DerTeletexString("0812"), expected, new DerTeletexString("4701")));

      final var actual = dut.getTeletexString(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getTeletexString(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getUtcTime()}. */
  @Test
  void test_getUtcTime() {
    // Assertions:
    // ... a. getUtcTime(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerUtcTime(ZonedDateTime.now(), DerUtcTime.UtcTimeFormat.HH_MM_SS_Z);
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getUtcTime();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(97-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getUtcTime);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getUtcTime(int)}. */
  @Test
  void test_getUtcTime__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected =
        new DerUtcTime(ZonedDateTime.now(), DerUtcTime.UtcTimeFormat.HH_MM_SS_DIFF);

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(
              List.of(
                  new DerUtcTime(
                      ZonedDateTime.now().plusDays(-1), DerUtcTime.UtcTimeFormat.HH_MM_SS_Z),
                  expected,
                  new DerUtcTime(
                      ZonedDateTime.now().plusDays(1), DerUtcTime.UtcTimeFormat.HH_MM_SS_Z)));

      final var actual = dut.getUtcTime(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getUtcTime(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getUtf8String()}. */
  @Test
  void test_getUtf8String() {
    // Assertions:
    // ... a. getUtf8String(int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    // --- a. smoke test
    {
      final var expected = new DerUtf8String("Foo Bar.");
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(0x20, expected.getEncoded());

      final var actual = dut.getUtf8String();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = (ConstructedBerTlv) BerTlv.getInstance("20-03-(8c-01-0b)");

      assertThrows(NoSuchElementException.class, dut::getUtf8String);
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getUtf8String(int)}. */
  @Test
  void test_getUtf8String__int() {
    // Assertions:
    // ... a. get(long, int)-method works as expected

    // Note: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NoSuchElementException

    final var expected = new DerUtf8String("4811 0815");

    // --- a. smoke test
    {
      final var dut =
          new DerSequence(List.of(new DerUtf8String("1815"), expected, new DerUtf8String("3711")));

      final var actual = dut.getUtf8String(1);

      assertSame(expected, actual);
    } // end --- a.

    // --- b. ERROR: NoSuchElementException
    {
      final var dut = new DerSequence(List.of(expected));

      assertThrows(NoSuchElementException.class, () -> dut.getUtf8String(1));
    } // end --- b.
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getEncoded()}. */
  @Test
  void test_getEncoded() {
    // Assertions:
    // ... a. ConstructedBerTlv(String)-constructor works as expected
    // ... b. toByteArray(OutputStream)-method      works as expected

    // Note 1: Because of assertion_b we can be lazy here and
    //         concentrate on test-coverage.
    // Note 2: It seems impossible to cause an "IOException".
    //         Thus, code-coverage is less than 100% :(

    // Test strategy:
    // --- a. smoke test
    final String input = Hex.extractHexDigits("20-03 (81-01-32)");
    final ConstructedBerTlv dut = (ConstructedBerTlv) BerTlv.getInstance(input);
    assertEquals(input, Hex.toHexDigits(dut.getEncoded()));
  } // end method */

  /** Test method for {@link ConstructedBerTlv#getEncoded(java.io.ByteArrayOutputStream)}. */
  @Test
  void test_getEncodedOutputStream() {
    // Assertions:
    // ... a. instance attribute insTagLengthField is properly set.
    // ... b. PrimitiveBerTlv.toString()-method works as expected.

    // Test strategy:
    // --- a. manually chosen smoke test with prefix
    // --- b. loop over all relevant constructed tag
    // --- c. empty value-field
    // --- d. value-field with one primitive child
    // --- e. value-field with one constructed child
    // --- f. value-field with one constructed and one primitive child

    // --- a. manually chosen smoke test with prefix
    final String prefix = "abcdef";
    final String dutAos = Hex.extractHexDigits("f1-08  [81-01-54  20-03 (82-01-44)]");
    final ByteArrayOutputStream baosA = new ByteArrayOutputStream(20);
    baosA.writeBytes(Hex.toByteArray(prefix));
    final ConstructedBerTlv dutA = (ConstructedBerTlv) BerTlv.getInstance(dutAos);
    dutA.getEncoded(baosA);
    assertEquals(prefix + dutAos, Hex.toHexDigits(baosA.toByteArray()));

    // --- b. loop over all relevant constructed tag
    TestBerTlv.VALID_TAGS.stream()
        .filter(tagField -> (tagField.length <= BerTlv.NO_TAG_FIELD)) // tag short enough
        .filter(tagField -> (0x20 == (tagField[0] & 0x20))) // constructed encoding
        .forEach(
            tagField -> {
              final long tag =
                  ((8 == tagField.length) ? new BigInteger(tagField) : new BigInteger(1, tagField))
                      .longValueExact();
              final ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
              ConstructedBerTlv dut = new ConstructedBerTlv(tag, new ArrayList<>());

              // --- c. empty value-field
              dut.getEncoded(baos);
              assertEquals(
                  Hex.toHexDigits(dut.insTagLengthField), Hex.toHexDigits(baos.toByteArray()));
              baos.reset();

              // --- c. value-field with one primitive child
              final PrimitiveBerTlv priA = new PrimitiveBerTlv(0x80, RNG.nextBytes(0, 16));
              dut = dut.add(priA);
              dut.getEncoded(baos);
              assertEquals(
                  Hex.toHexDigits(dut.insTagLengthField) + priA,
                  Hex.toHexDigits(baos.toByteArray()));
              final String dutOs = dut.toString();
              assertEquals(Hex.toHexDigits(baos.toByteArray()), dutOs);
              baos.reset();

              // --- e. value-field with one constructed child
              ConstructedBerTlv dutD = new ConstructedBerTlv(0x20, new ArrayList<>()).add(dut);
              dutD.getEncoded(baos);
              assertEquals(
                  Hex.toHexDigits(dutD.insTagLengthField) + dutOs,
                  Hex.toHexDigits(baos.toByteArray()));
              baos.reset();

              // --- f. value-field with one constructed and one primitive child
              final PrimitiveBerTlv priE = new PrimitiveBerTlv(0x8e, RNG.nextBytes(0, 16));
              dutD = dutD.add(priE);
              dutD.getEncoded(baos);
              assertEquals(
                  Hex.toHexDigits(dutD.insTagLengthField) + dutOs + priE,
                  Hex.toHexDigits(baos.toByteArray()));
              baos.reset();
            }); // end forEach(tagField -> ...)
  } // end method */

  /** Test method for {@link ConstructedBerTlv#toString(String, String)}. */
  @Test
  void test_toString__String_String() {
    // Assertions:
    // ... a. toString(String, String, int, boolean)-method works as expected

    // Note 1: Because of assertion_a we can be lazy here and do some smoke tests.
    // Note 2: Tests on randomly generated TLV objects are performed in test method
    //         TODO

    // Test strategy:
    // --- a. smoke tests
    {
      final var expected = "a1-03" + LINE_SEPARATOR + "|.-87-01-99";
      final var dut = (ConstructedBerTlv) BerTlv.getInstance(expected);

      final var actual = dut.toString("-", "|.-");

      assertEquals(expected, actual);
    } // end --- a.
  } // end method */

  /**
   * Test method for {@link ConstructedBerTlv#toString(String, String, int, boolean)}.
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
    // --- a. no delimiter and no indentation
    // --- b. delimiter but no indentation
    // --- c. no delimiter but indentation
    // --- d. delimiter and indentation

    // --- define constructed DO
    final String deli = "-";
    final String in = "|."; // NOPMD short name, indentation
    final String newL = String.format("%n");
    final String tag1C = "61";
    final String len1C = "14";
    final String tag11P = "de";
    final String len11P = "03";
    final String val11P = "010204";
    final String tag12C = "6f";
    final String len12C = "07";
    final String tag121P = "46";
    final String len121P = "05";
    final String val121P = "1f2e3d4c5b";
    final String tag13P = "1f7f";
    final String len13P = "00";
    final String tag14C = "3f40";
    final String len14C = "00";

    final ConstructedBerTlv dut =
        (ConstructedBerTlv)
            BerTlv.getInstance(
                tag1C + len1C + tag11P + len11P + val11P + tag12C + len12C + tag121P + len121P
                    + val121P + tag13P + len13P + tag14C + len14C);

    for (final var addComment : VALUES_BOOLEAN) {
      // --- a. no delimiter and no indentation
      assertEquals(
          tag1C + len1C + tag11P + len11P + val11P + tag12C + len12C + tag121P + len121P + val121P
              + tag13P + len13P + tag14C + len14C,
          dut.toString("", "", 0, addComment));

      // --- b. delimiter but no indentation
      assertEquals(
          tag1C + deli + len1C + deli + deli + tag11P + deli + len11P + deli + val11P + deli + deli
              + tag12C + deli + len12C + deli + deli + tag121P + deli + len121P + deli + val121P
              + deli + deli + tag13P + deli + len13P + deli + deli + tag14C + deli + len14C,
          dut.toString(deli, "", 0, addComment));

      // --- c. no delimiter but indentation
      // c.1 indentation = 0
      assertEquals(
          tag1C + len1C + newL + in + tag11P + len11P + val11P + newL + in + tag12C + len12C + newL
              + in + in + tag121P + len121P + val121P + newL + in + tag13P + len13P + newL + in
              + tag14C + len14C,
          dut.toString("", in, 0, addComment));

      // c.1 indentation = 1
      assertEquals(
          in + tag1C + len1C + newL + in + in + tag11P + len11P + val11P + newL + in + in + tag12C
              + len12C + newL + in + in + in + tag121P + len121P + val121P + newL + in + in + tag13P
              + len13P + newL + in + in + tag14C + len14C,
          dut.toString("", in, 1, addComment));

      // c.1 indentation = 2
      assertEquals(
          in + in + tag1C + len1C + newL + in + in + in + tag11P + len11P + val11P + newL + in + in
              + in + tag12C + len12C + newL + in + in + in + in + tag121P + len121P + val121P + newL
              + in + in + in + tag13P + len13P + newL + in + in + in + tag14C + len14C,
          dut.toString("", in, 2, addComment));

      // --- d. delimiter and indentation
      assertEquals(
          in + in + in + tag1C + deli + len1C + newL + in + in + in + in + tag11P + deli + len11P
              + deli + val11P + newL + in + in + in + in + tag12C + deli + len12C + newL + in + in
              + in + in + in + tag121P + deli + len121P + deli + val121P + newL + in + in + in + in
              + tag13P + deli + len13P + newL + in + in + in + in + tag14C + deli + len14C,
          dut.toString(deli, in, 3, addComment));
    } // end For (addComment...)
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
    // --- a. smoke test for BerTlv not implementing DerComment-interface
    // --- b. smoke test where at least one TLV-object implements DerComment-interface

    // --- a. smoke test for BerTlv not implementing DerComment-interface
    for (final var expected : Set.of(String.format("60 05%n|  84 03 720815"))) {
      for (final var addComment : VALUES_BOOLEAN) {
        final BerTlv dut = BerTlv.getInstance(expected);
        assertEquals(ConstructedBerTlv.class, dut.getClass());
        assertFalse(dut instanceof DerSpecific);

        final var actual = dut.toString(" ", "|  ", 0, addComment);

        assertEquals(expected, actual);
      } // end For (addComment...)
    } // end For (input...)
    // end --- a.

    // --- b. smoke test where at least one TLV-object implements DerComment-interface
    // b.0 parent and child comment absent => see case a. above
    // b.1 parent   comment present    child   comment absent
    // b.2 parent   comment absent     child   comment present
    // b.3 parent   comment present    child   comment present

    // b.1 parent   comment present    child   comment absent
    Map.ofEntries(
            Map.entry("30 02    80 00", String.format("30 02 # SEQUENCE with 1 element%n|  80 00")),
            Map.entry(
                "30 0a    80 00    41 01 00    82 00    41 01 72",
                String.format(
                    "30 0a # SEQUENCE with 4 elements%n"
                        + "|  80 00%n"
                        + "|  41 01 00%n"
                        + "|  82 00%n"
                        + "|  41 01 72")))
        .forEach(
            (input, exp) -> {
              final BerTlv dut = BerTlv.getInstance(input);
              assertEquals(DerSequence.class, dut.getClass());
              assertEquals(exp, dut.toStringTree());
            }); // end forEach((input, exp) -> ...)

    // b.2 parent   comment absent     child   comment present
    Map.ofEntries(
            Map.entry("60 02    00 00", String.format("60 02%n|  00 00 # EndOfContent")),
            Map.entry(
                "60 0a    80 00    01 01 00    82 00    01 01 72",
                String.format(
                    "60 0a%n"
                        + "|  80 00%n"
                        + "|  01 01 00 # BOOLEAN := false%n"
                        + "|  82 00%n"
                        + "|  01 01 72 # BOOLEAN := true")))
        .forEach(
            (input, exp) -> {
              final BerTlv dut = BerTlv.getInstance(input);
              assertEquals(ConstructedBerTlv.class, dut.getClass());
              assertEquals(exp, dut.toStringTree());
            }); // end forEach((input, exp) -> ...)

    // b.3 parent   comment present    child   comment present
    Map.ofEntries(
            Map.entry(
                "30 02    00 00",
                String.format("30 02 # SEQUENCE with 1 element%n|  00 00 # EndOfContent")),
            Map.entry(
                "30 0a    80 00    01 01 00    82 00    01 01 72",
                String.format(
                    "30 0a # SEQUENCE with 4 elements%n"
                        + "|  80 00%n"
                        + "|  01 01 00 # BOOLEAN := false%n"
                        + "|  82 00%n"
                        + "|  01 01 72 # BOOLEAN := true")))
        .forEach(
            (input, exp) -> {
              final BerTlv dut = BerTlv.getInstance(input);
              assertEquals(DerSequence.class, dut.getClass());
              assertEquals(exp, dut.toStringTree());
            }); // end forEach((input, exp) -> ...)
  } // end method */

  /**
   * Creates a {@link ConstructedBerTlv} with a random tag and a random template.
   *
   * <p>Implementation notes:
   *
   * <ol>
   *   <li>The number of children is evenly chosen in range [0, maxNoChildren].
   *   <li>With {@code probabilityIndefiniteForm} it is chosen if the returned length-field
   *       indicates the {@code indefinite form}
   *   <li>{@code countdown} controls the probability of a child to be constructed: {@code
   *       P(constructed) = countdown / 20}.
   * </ol>
   *
   * @param maxNoChildren maximum number of children
   * @param probabilityIndefiniteForm name says it all
   * @param countdown counting down
   * @param tagLengthValue output parameter, filled by this method, <b>SHALL</b> have a length of
   *     three; the method sets
   *     <ol>
   *       <li>tag-field in element 0,
   *       <li>length-field in element 1
   *       <li>value-field in element 2
   *     </ol>
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  /* package */ static ConstructedBerTlv createRandom(
      final int maxNoChildren,
      final double probabilityIndefiniteForm,
      final int countdown,
      final byte[][] tagLengthValue) {
    final var maxLengthPrimitive = 1024;
    final var factorProbability = 1.0 / 20.0;

    final var clazz = TestBerTlv.randomClassOfTag();
    final var tagNumber = TestBerTlv.randomTagNumber(clazz, false, true);
    final var tagField = BerTlv.createTag(clazz, true, tagNumber);
    final var tag = BerTlv.convertTag(tagField);
    final var isIndefiniteForm = RNG.nextDouble() < probabilityIndefiniteForm;
    final var probabilityConstructed = countdown * factorProbability;
    final var noChildren = RNG.nextIntClosed(0, maxNoChildren);
    final var children = new ArrayList<BerTlv>();
    final var childOctets = new byte[noChildren + (isIndefiniteForm ? 1 : 0)][];

    // --- create children
    final var tlv = new byte[3][];
    int index = 0;
    for (; index < noChildren; index++) {
      final var isConstructed = RNG.nextDouble() < probabilityConstructed;

      final BerTlv child;
      if (isConstructed) {
        child = createRandom(maxNoChildren, probabilityIndefiniteForm, countdown - 1, tlv);
      } else {
        child = TestPrimitiveBerTlv.createRandom(isIndefiniteForm, maxLengthPrimitive, tlv);
      } // end else

      children.add(child);
      childOctets[index] = AfiUtils.concatenate(tlv);
    } // end For (i...)

    // --- possibly adjust valueField with "END OF CONTENT" indication
    if (isIndefiniteForm) {
      childOctets[index] = DerEndOfContent.EOC.getEncoded();
    } // end fi

    // --- set output parameter "tagLengthValue"
    tagLengthValue[0] = tagField;
    tagLengthValue[2] = AfiUtils.concatenate(childOctets);
    if (isIndefiniteForm) {
      tagLengthValue[1] = new byte[] {-128};
    } else {
      tagLengthValue[1] = TestBerTlv.randomLengthField(tagLengthValue[2].length);
    } // end else

    return (DerSequence.TAG == tag)
        ? new DerSequence(children)
        : new ConstructedBerTlv(tag, children);
  } // end method */
} // end class
