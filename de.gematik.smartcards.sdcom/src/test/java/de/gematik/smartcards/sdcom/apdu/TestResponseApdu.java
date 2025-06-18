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
package de.gematik.smartcards.sdcom.apdu;

import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_EXTENDED_WILDCARD;
import static de.gematik.smartcards.sdcom.apdu.ResponseApdu.WILDCARD_DATA;
import static de.gematik.smartcards.sdcom.apdu.ResponseApdu.WILDCARD_TRAILER;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for white-box testing of {@link ResponseApdu}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
/*/ Note 1: Spotbugs claims "NP_LOAD_OF_KNOWN_NULL_VALUE".
//         Spotbugs message: The variable referenced at this point is known to be
//             null due to an earlier check against null. Although this is valid,
//             it might be a mistake (perhaps you intended to refer to a different
//             variable, or perhaps the earlier check to see if the variable is
//             null should have been a check to see if it was non-null).
//         Rational: That finding is suppressed because intentionally a null-value is used.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_LOAD_OF_KNOWN_NULL_VALUE" // see note 1
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestResponseApdu {

  /** Formatter for showing this, expected and mask. */
  private static final String FORMATTER = "this = %s%nexpe = %s%nmask = %s"; // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Set of trailer values used for border tests. */
  private static final Set<Integer> TRAILER_VALUES =
      RNG.intsClosed(0, WILDCARD_TRAILER, 5).boxed().collect(Collectors.toSet()); // */

  /** Set indicating that no difference is found. */
  private static final Set<EafiResponseApduDifference> SET_NONE =
      EnumSet.noneOf(EafiResponseApduDifference.class); // */

  /** Set indicating that APDU differ in content of data field only. */
  private static final Set<EafiResponseApduDifference> SET_CONTENT =
      Set.of(EafiResponseApduDifference.CONTENT); // */

  /** Set indicating that APDU differ in length of data field only. */
  private static final Set<EafiResponseApduDifference> SET_LENGTH =
      Set.of(EafiResponseApduDifference.LENGTH); // */

  /** Set indicating that APDU differ in trailer only. */
  private static final Set<EafiResponseApduDifference> SET_TRAILER =
      Set.of(EafiResponseApduDifference.TRAILER); // */

  /** Set indicating that APDU differ in length of data fields and trailer. */
  private static final Set<EafiResponseApduDifference> SET_LEN_TRA =
      Set.of(EafiResponseApduDifference.LENGTH, EafiResponseApduDifference.TRAILER); // */

  /** Set indicating that APDU differ in content and length. */
  private static final Set<EafiResponseApduDifference> SET_CON_LEN =
      Set.of(EafiResponseApduDifference.CONTENT, EafiResponseApduDifference.LENGTH); // */

  /** Set indicating that APDU differ in content, length and trailer. */
  private static final Set<EafiResponseApduDifference> SET_CO_LE_TR =
      Set.of(
          EafiResponseApduDifference.CONTENT,
          EafiResponseApduDifference.LENGTH,
          EafiResponseApduDifference.TRAILER); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Note: A lot of border test in this test-class depend on the fact that
    //       WILDCARD_TRAILER is just above the supremum of valid values.
    assertEquals(0x1_0000, WILDCARD_TRAILER);
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

  /** Test method for {@link ResponseApdu#ResponseApdu(byte[])}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_ResponseApdu__byteA() {
    // Assertions:
    // ... a. underlying ResponseApdu(byte[], int, int)-constructor works as expected
    // ... c. getters work as expected

    // Note: Because of the assertions the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy.
    // --- a. intentionally empty
    // --- b. check with length <= 1 => invalid
    for (final var length : List.of(0, 1)) {
      final var input = RNG.nextBytes(length);

      final var t = assertThrows(IllegalArgumentException.class, () -> new ResponseApdu(input));

      assertEquals("0 > " + (length - 2), t.getMessage());
      assertNull(t.getCause());
    } // end For (length...)

    // --- c. check with length >= 2 => valid
    RNG.intsClosed(0, NE_EXTENDED_WILDCARD, 50)
        .forEach(
            nr -> {
              final byte[] data = RNG.nextBytes(nr);

              TRAILER_VALUES.forEach(
                  trailer -> {
                    final byte[] apdu =
                        AfiUtils.concatenate(
                            data, new byte[] {(byte) (trailer >> 8), trailer.byteValue()});
                    final ResponseApdu dut = new ResponseApdu(apdu);

                    assertArrayEquals(data, dut.getData());
                    assertEquals(trailer & 0xffff, dut.getTrailer());
                  }); // end forEach(trailer -> ...)
            }); // end forEach(nr -> ...)
  } // end method */

  /** Test method for {@link ResponseApdu#ResponseApdu(String)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_ResponseApdu__String() {
    // Assertions:
    // ... a. ResponseApdu(byte[])-constructor works as expected

    // Note: Because of the assertions and the simplicity of the
    //       constructor-under-test, we can be lazy here.

    // Test strategy:
    // --- a. intentionally empty
    // --- b. check with length <= 1 => invalid
    // --- c. check with length >= 2 => valid
    // --- d. odd number of hex digits

    // --- a. intentionally empty
    // --- b. check with length <= 1 => invalid
    for (final var length : List.of(0, 1)) {
      final var input = Hex.toHexDigits(RNG.nextBytes(length));

      final var t = assertThrows(IllegalArgumentException.class, () -> new ResponseApdu(input));

      assertEquals("0 > " + (length - 2), t.getMessage());
      assertNull(t.getCause());
    } // end For (length...)

    // --- c. check with length >= 2 => valid
    RNG.intsClosed(0, NE_EXTENDED_WILDCARD, 50)
        .forEach(
            nr -> {
              final byte[] data = RNG.nextBytes(nr);

              TRAILER_VALUES.forEach(
                  trailer -> {
                    final byte[] apdu =
                        AfiUtils.concatenate(
                            data, new byte[] {(byte) (trailer >> 8), trailer.byteValue()});
                    final ResponseApdu dut = new ResponseApdu(Hex.toHexDigits(apdu));

                    assertArrayEquals(data, dut.getData());
                    assertEquals(trailer & 0xffff, dut.getTrailer());
                  }); // end forEach(trailer -> ...)
            }); // end forEach(nr -> ...)

    // --- d. odd number of hex digits
    List.of("5", "12345")
        .forEach(
            apdu -> {
              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> new ResponseApdu(apdu));
              assertEquals("Number of hex-digits in <" + apdu + "> is odd", throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(apdu -> ...)
  } // end method */

  /** Test method for {@link ResponseApdu#ResponseApdu(byte[], int, int)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_ResponseApdu__byteA_int_int() {
    // Assertions:
    // ... a. getters works as expected

    // Test strategy.
    // --- b. loop over various lengths for prefix- and postfix-octet strings
    // --- c. check with length <= 1 => invalid
    // --- d. check with length >= 2 => valid
    // --- e. check that invalid offset and invalid length are detected

    // --- b. loop over various lengths for prefix- and postfix-octet strings
    // spotless:off
    RNG.intsClosed(0, 20, 5).forEach(offset -> {
      final byte[] prefix = RNG.nextBytes(offset);

      RNG.intsClosed(0, 20, 5).forEach(postfixLength -> {
        final byte[] postfix = RNG.nextBytes(postfixLength);

        // --- c. check with length <= 1 => invalid
        IntStream.rangeClosed(0, 1).forEach(length -> {
          final byte[] input = RNG.nextBytes(length);
          final byte[] apdu = AfiUtils.concatenate(prefix, input, postfix);
          final Throwable throwable = assertThrows(
              IllegalArgumentException.class,
              () -> new ResponseApdu(apdu, offset, length));
          assertEquals(offset + " > " + (offset + length - 2), throwable.getMessage());
          assertNull(throwable.getCause());
        }); // end forEach(i -> ...)

        // --- d. check with length >= 2 => valid
        RNG.intsClosed(0, NE_EXTENDED_WILDCARD, 50).forEach(nr -> {
          final byte[] data = RNG.nextBytes(nr);

          TRAILER_VALUES.forEach(
              trailer -> {
                final byte[] apdu = AfiUtils.concatenate(
                    prefix,
                    data,
                    new byte[]{(byte) (trailer >> 8), trailer.byteValue()},
                    postfix);
                final ResponseApdu dut = new ResponseApdu(apdu, offset, nr + 2);

                assertArrayEquals(data, dut.getData());
                assertEquals(trailer & 0xffff, dut.getTrailer());
              }); // end forEach(trailer -> ...)
        }); // end forEach(nr -> ...)
      }); // end forEach(postfixLength -> ...)
    }); // end forEach(offset -> ...)
    // spotless:on

    // --- e. check that invalid offset and invalid length are detected
    // Note: Keys in the following map are lists with the following elements:
    //       - index = 0: apdu.length
    //       - index = 1: offset
    //       - index = 2: length
    final var inputs =
        List.of(
            List.of(8, -1, 2), // offset < 0
            List.of(7, 1, -3), // length < 0
            List.of(5, 2, 4) // offset + length > apduLength
            );
    for (final var input : inputs) {
      final int apduLength = input.get(0);
      final int offset = input.get(1);
      final int length = input.get(2);
      final var octets = RNG.nextBytes(apduLength);
      final Throwable throwable =
          assertThrows(
              IndexOutOfBoundsException.class, () -> new ResponseApdu(octets, offset, length));
      assertEquals(
          String.format(
              "Range [%d, %d + %d) out of bounds for length %d",
              offset, offset, length, apduLength),
          throwable.getMessage());
      assertNull(throwable.getCause());
    } // end For (input...)
  } // end method */

  /** Test method for {@link ResponseApdu#ResponseApdu(byte[], int)}. */
  @Test
  void test_ResponseApdu__byteA_int() {
    // Assertions:
    // ... a. getters works as expected

    // Note: The constructor-under-test is rather simple. Thus, extensive
    //       testing seems not to be necessary, and we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. arbitrary trailer
    // --- c. data == WildCard
    // --- d. arbitrary data
    // --- e. check for defensive cloning upon construction

    // --- a. smoke test
    {
      final var expData = RNG.nextBytes(16, 32);
      final var expSw12 = RNG.nextIntClosed(0, 0xffff);

      final var dut = new ResponseApdu(expData, expSw12);

      assertArrayEquals(expData, dut.getData());
      assertEquals(expSw12, dut.getSw());
    } // end --- a.

    // --- b. arbitrary trailer
    AfiUtils.SPECIAL_INT.forEach(
        trailer -> {
          // --- c. data == WildCard
          {
            final ResponseApdu dut = new ResponseApdu(WILDCARD_DATA, trailer);

            assertSame(WILDCARD_DATA, dut.getData());
            assertEquals(
                (WILDCARD_TRAILER == trailer) ? trailer : trailer & 0xffff, dut.getTrailer());
          } // end --- c.

          // --- d. arbitrary data
          RNG.intsClosed(0, 20, 5)
              .forEach(
                  nr -> {
                    final byte[] data = RNG.nextBytes(nr);

                    final ResponseApdu dutA = new ResponseApdu(data, trailer);
                    final byte[] out = dutA.getData();

                    assertNotSame(data, out);
                    assertArrayEquals(data, out);
                    assertEquals(
                        (WILDCARD_TRAILER == trailer) ? trailer : trailer & 0xffff,
                        dutA.getTrailer());

                    if (nr > 0) {
                      // ... at least one octet in response data field
                      // --- e. check for defensive cloning upon construction
                      final byte msb = data[0];
                      data[0]++;
                      final ResponseApdu dutB = new ResponseApdu(data, trailer);
                      assertEquals(msb, dutA.getData()[0]);
                      assertEquals((byte) (msb + 1), dutB.getData()[0]);
                    } // end fi
                  }); // end forEach(nr -> ...)
        }); // end forEach(trailer -> ...)
  } // end method */

  /** Test method for {@link ResponseApdu#difference(ResponseApdu)}. */
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.NPathComplexity"
  })
  @Test
  void test_difference__ResponseApdu() {
    // Test strategy:
    // --- a. loop over bunch of trailer
    // --- b. this has WILDCARD_DATA
    // --- c. expected has WILDCARD_DATA
    // --- d. "same" content but different lengths
    // --- e. "different" content and different lengths

    // --- a. loop over bunch of trailer
    for (final var trailerThis : TRAILER_VALUES) {
      for (final var trailerExpected : TRAILER_VALUES) {
        final boolean sameTrailer =
            (trailerThis.equals(trailerExpected) || (trailerExpected == WILDCARD_TRAILER));

        // --- b. this has WILDCARD_DATA
        // b.1 expected with WILDCARD_DATA
        // b.2 expected with empty data field
        // b.3 expected with non-empty data field
        {
          final ResponseApdu dut = new ResponseApdu(WILDCARD_DATA, trailerThis);

          // b.1 expected with WILDCARD_DATA
          assertEquals(
              sameTrailer ? SET_NONE : SET_TRAILER,
              dut.difference(new ResponseApdu(WILDCARD_DATA, trailerExpected)));

          // b.2 expected with empty data field
          assertEquals(
              sameTrailer ? SET_NONE : SET_TRAILER,
              dut.difference(new ResponseApdu(AfiUtils.EMPTY_OS, trailerExpected)));

          // b.3 expected with non-empty data field
          RNG.intsClosed(1, 256, 5)
              .forEach(
                  nr ->
                      assertEquals(
                          sameTrailer ? SET_LENGTH : SET_LEN_TRA,
                          dut.difference(new ResponseApdu(RNG.nextBytes(nr), trailerExpected))));
        } // end --- b.

        // --- c. expected has WILDCARD_DATA
        // c.1 this with WILDCARD_DATA
        // c.2 this with response data field of various lengths
        {
          final ResponseApdu expected = new ResponseApdu(WILDCARD_DATA, trailerExpected);

          // c.1 this with WILDCARD_DATA
          assertEquals(
              sameTrailer ? SET_NONE : SET_TRAILER,
              new ResponseApdu(WILDCARD_DATA, trailerThis).difference(expected));

          // c.2 this with response data field of various lengths
          RNG.intsClosed(0, 1024, 5)
              .forEach(
                  nr ->
                      assertEquals(
                          sameTrailer ? SET_NONE : SET_TRAILER,
                          new ResponseApdu(RNG.nextBytes(nr), trailerThis)
                              .difference(expected))); // end forEach(nr -> ...)
        } // end --- c.

        // --- d. "same" content but different lengths
        RNG.intsClosed(0, 10, 5)
            .forEach(
                postfixLength -> {
                  final byte[] postfix = RNG.nextBytes(postfixLength);

                  RNG.intsClosed(0, 10, 5)
                      .forEach(
                          nr -> {
                            final byte[] dataA = RNG.nextBytes(nr);
                            final byte[] dataB = AfiUtils.concatenate(dataA, postfix);
                            final ResponseApdu rspA = new ResponseApdu(dataA, trailerThis);
                            final ResponseApdu rspB = new ResponseApdu(dataB, trailerExpected);
                            final Set<EafiResponseApduDifference> exp =
                                EnumSet.noneOf(EafiResponseApduDifference.class);

                            if (!sameTrailer) {
                              exp.add(EafiResponseApduDifference.TRAILER);
                            } // end fi

                            if (0 != postfixLength) {
                              exp.add(EafiResponseApduDifference.LENGTH);
                            } // end fi

                            assertEquals(exp, rspA.difference(rspB));

                            // --- e. "different" content and different lengths
                            for (int index = nr; index-- > 0; ) { // NOPMD assignment in operands
                              exp.add(EafiResponseApduDifference.CONTENT);
                              final byte[] dataC = dataB.clone();
                              dataC[index]++; // modify data
                              final ResponseApdu rspC =
                                  new ResponseApdu(dataC, trailerExpected); // NOPMD loop, new

                              assertEquals(exp, rspA.difference(rspC));
                            } // end For (index...)
                          }); // end forEach(nr -> ...)
                }); // end forEach(postfixLength -> ...)
      } // end For (trailerExpected...)
    } // end For (trailerThis...)
  } // end method */

  /** Test method for {@link ResponseApdu#difference(ResponseApdu, ResponseApdu)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NcssCount"})
  @Test
  void test_difference__ResponseApdu_ResponseApdu() {
    // Assertions:
    // ... a. difference(ResponseApdu)-method works as expected

    // Test strategy:
    // --- a. expected is 'WildcardData WildcardTrailer'
    // --- b. expected is 'WildcardData     xxyy       '
    // --- c. expected is 'xx  ...   yy WildcardTrailer'
    // --- d. mask is 'WildcardData WildcardTrailer'
    // --- e. mask is 'WildcardData     xxyy       '
    // --- f. mask is 'xx  ...   yy WildcardTrailer'
    // --- g. this is 'WildcardData WildcardTrailer'
    // --- h. this is 'WildcardData     xxyy       '
    // --- i. this is 'xx  ...   yy WildcardTrailer'
    // --- j. length: t == e == m, all data fields have equal lengths
    // --- k. length: t == e,   this   and expected have data fields with equal length
    // --- l. length: e == m, expected and   mask   have data fields with equal length
    // --- m. length: t == m,   this   and   mask   have data fields with equal length
    // --- n. length: t < e, m, data field of   this   is shortest
    // --- o. length: e < t, m, data field of expected is shortest
    // --- p. length: m < t, e, data field of   mask   is shortest
    // --- q. just one bit difference in trailer
    // --- r. just one bit difference in data field

    final ResponseApdu mask0 = new ResponseApdu(WILDCARD_DATA, WILDCARD_TRAILER);
    final ResponseApdu mask1 = new ResponseApdu(WILDCARD_DATA, 0xffff);
    final ResponseApdu mask2 = new ResponseApdu(Hex.toByteArray("deaf"), WILDCARD_TRAILER);
    final ResponseApdu mask3 = new ResponseApdu(Hex.toByteArray("defe"), 0xffff);

    // --- a. expected is 'WildcardData WildcardTrailer'
    {
      final ResponseApdu expected = new ResponseApdu(WILDCARD_DATA, WILDCARD_TRAILER);

      // Note: Loop over bunch of data fields and trailer for this and mask
      List.of(
              WILDCARD_DATA, // special value
              new byte[0], // data field absent
              RNG.nextBytes(1, 5) // arbitrary data field
              )
          .forEach(
              thisData -> {
                List.of(
                        WILDCARD_DATA, // special value
                        new byte[0], // data field absent
                        RNG.nextBytes(1, 5) // arbitrary data field
                        )
                    .forEach(
                        maskData -> {
                          TRAILER_VALUES.forEach(
                              thisTrailer -> {
                                final ResponseApdu dut = new ResponseApdu(thisData, thisTrailer);

                                TRAILER_VALUES.forEach(
                                    maskTrailer -> {
                                      final ResponseApdu mask =
                                          new ResponseApdu(maskData, maskTrailer);

                                      assertEquals(SET_NONE, dut.difference(expected, mask));
                                    }); // end forEach(maskTrailer -> ...)
                              }); // end forEach(thisTrailer -> ...)
                        }); // end forEach(maskData -> ...)
              }); // end forEach(thisData -> ...)
    } // end --- a.

    // --- b. expected is 'WildcardData     xxyy       '
    TRAILER_VALUES.forEach(
        expectedTrailer -> {
          final ResponseApdu expected = new ResponseApdu(WILDCARD_DATA, expectedTrailer);

          // Note: Loop over bunch of data fields and trailer for this and mask
          List.of(
                  WILDCARD_DATA, // special value
                  new byte[0], // data field absent
                  RNG.nextBytes(1, 5) // arbitrary data field
                  )
              .forEach(
                  thisData -> {
                    List.of(
                            WILDCARD_DATA, // special value
                            new byte[0], // data field absent
                            RNG.nextBytes(1, 5) // arbitrary data field
                            )
                        .forEach(
                            maskData -> {
                              TRAILER_VALUES.forEach(
                                  thisTrailer -> {
                                    final ResponseApdu dut =
                                        new ResponseApdu(thisData, thisTrailer);

                                    TRAILER_VALUES.forEach(
                                        maskTrailer -> {
                                          final ResponseApdu mask =
                                              new ResponseApdu(maskData, maskTrailer);

                                          assertEquals(
                                              (WILDCARD_TRAILER == expectedTrailer)
                                                      || (WILDCARD_TRAILER == maskTrailer)
                                                      || ((thisTrailer & maskTrailer)
                                                          == (expectedTrailer
                                                              & maskTrailer)) // NOPMD
                                                  ? SET_NONE
                                                  : SET_TRAILER,
                                              dut.difference(expected, mask),
                                              () -> String.format(FORMATTER, dut, expected, mask));
                                        }); // end forEach(maskTrailer -> ...)
                                  }); // end forEach(thisTrailer -> ...)
                            }); // end forEach(maskData -> ...)
                  }); // end forEach(thisData -> ...)
        }); // end forEach(expectedTrailer -> ...)
    // end --- b.

    // --- c. expected is 'xx  ...   yy WildcardTrailer'
    // Note: Intentionally no difference in data field.
    {
      final byte[] data = RNG.nextBytes(1, 10);
      final ResponseApdu expected = new ResponseApdu(data, WILDCARD_TRAILER);

      TRAILER_VALUES.forEach(
          maskTrailer -> {
            final ResponseApdu mask = new ResponseApdu(data, maskTrailer);

            IntStream.rangeClosed(0, WILDCARD_TRAILER)
                .forEach(
                    thisTrailer -> {
                      final ResponseApdu dut = new ResponseApdu(data, thisTrailer);

                      assertEquals(SET_NONE, dut.difference(expected, mask));
                    }); // end forEach(thisTrailer -> ...)
          }); // end forEach(maskTrailer -> ...)
    } // end --- c.

    // --- d. mask is 'WildcardData WildcardTrailer'
    {
      final ResponseApdu mask = new ResponseApdu(WILDCARD_DATA, WILDCARD_TRAILER);

      // Note: Loop over bunch of data fields and trailer for this and expected
      List.of(
              WILDCARD_DATA, // special value
              new byte[0], // data field absent
              RNG.nextBytes(1, 5) // arbitrary data field
              )
          .forEach(
              thisData -> {
                List.of(
                        WILDCARD_DATA, // special value
                        new byte[0], // data field absent
                        RNG.nextBytes(1, 5) // arbitrary data field
                        )
                    .forEach(
                        expectedData -> {
                          TRAILER_VALUES.forEach(
                              thisTrailer -> {
                                final ResponseApdu dut = new ResponseApdu(thisData, thisTrailer);

                                TRAILER_VALUES.forEach(
                                    expectedTrailer -> {
                                      final ResponseApdu expected =
                                          new ResponseApdu(expectedData, expectedTrailer);

                                      assertEquals(SET_NONE, dut.difference(expected, mask));
                                    }); // end forEach(expectedTrailer -> ...)
                              }); // end forEach(thisTrailer -> ...)
                        }); // end forEach(expectedData -> ...)
              }); // end forEach(thisData -> ...)
    } // end --- d.

    // --- e. mask is 'WildcardData     xxyy       '
    TRAILER_VALUES.forEach(
        maskTrailer -> {
          final ResponseApdu mask = new ResponseApdu(WILDCARD_DATA, maskTrailer);

          // Note: Loop over a bunch of data fields and trailer for this and expected
          List.of(
                  WILDCARD_DATA, // special value
                  new byte[0], // data field absent
                  RNG.nextBytes(1, 5) // arbitrary data field
                  )
              .forEach(
                  thisData -> {
                    List.of(
                            WILDCARD_DATA, // special value
                            new byte[0], // data field absent
                            RNG.nextBytes(1, 5) // arbitrary data field
                            )
                        .forEach(
                            expectedData -> {
                              TRAILER_VALUES.forEach(
                                  thisTrailer -> {
                                    final ResponseApdu dut =
                                        new ResponseApdu(thisData, thisTrailer);

                                    TRAILER_VALUES.forEach(
                                        expectedTrailer -> {
                                          final ResponseApdu expected =
                                              new ResponseApdu(expectedData, expectedTrailer);

                                          assertEquals(
                                              (WILDCARD_TRAILER == expectedTrailer)
                                                      || (WILDCARD_TRAILER == maskTrailer)
                                                      || ((thisTrailer & maskTrailer)
                                                          == (expectedTrailer
                                                              & maskTrailer)) // NOPMD
                                                  ? SET_NONE
                                                  : SET_TRAILER,
                                              dut.difference(expected, mask),
                                              () -> String.format(FORMATTER, dut, expected, mask));
                                        }); // end forEach(expectedTrailer -> ...)
                                  }); // end forEach(thisTrailer -> ...)
                            }); // end forEach(expectedData -> ...)
                  }); // end forEach(thisData -> ...)
        }); // end forEach(maskTrailer -> ...)
    // end --- e.

    // --- f. mask is 'xx  ...   yy WildcardTrailer'
    // Note: Intentionally no difference in data field.
    {
      final byte[] data = RNG.nextBytes(1, 10);
      final ResponseApdu mask = new ResponseApdu(data, WILDCARD_TRAILER);

      TRAILER_VALUES.forEach(
          expectedTrailer -> {
            final ResponseApdu expected = new ResponseApdu(data, expectedTrailer);

            IntStream.rangeClosed(0, WILDCARD_TRAILER)
                .forEach(
                    thisTrailer -> {
                      final ResponseApdu dut = new ResponseApdu(data, thisTrailer);

                      assertEquals(SET_NONE, dut.difference(expected, mask));
                    }); // end forEach(thisTrailer -> ...)
          }); // end forEach(expectedTrailer -> ...)
    } // end --- f.

    // --- g. this is 'WildcardData WildcardTrailer'
    // g.0 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
    // g.1 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData      xx yy     '
    // g.2 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
    // g.3 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
    // g.4 expected = 'WildcardData      xx yy     '   mask = 'WildcardData WildcardTrailer'
    // g.5 expected = 'WildcardData      xx yy     '   mask = 'WildcardData      xx yy     '
    // g.6 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
    // g.7 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy       xx yy     '
    // g.8 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
    // g.9 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData      xx yy     '
    // g.a expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
    // g.b expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
    // g.c expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData WildcardTrailer'
    // g.d expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData      xx yy     '
    // g.e expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
    // g.f expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy       xx yy     '
    {
      final ResponseApdu dut = new ResponseApdu(WILDCARD_DATA, WILDCARD_TRAILER);

      ResponseApdu expected;

      // g.0 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
      // g.1 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData      xx yy     '
      // g.2 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
      // g.3 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
      expected = new ResponseApdu(WILDCARD_DATA, WILDCARD_TRAILER);
      for (final ResponseApdu mask : new ResponseApdu[] {mask0, mask1, mask2, mask3}) {
        assertEquals(SET_NONE, dut.difference(expected, mask));
      } // end For (mask...)

      // g.4 expected = 'WildcardData      xx yy     '   mask = 'WildcardData WildcardTrailer'
      expected = new ResponseApdu(WILDCARD_DATA, 0x6982);
      assertEquals(SET_NONE, dut.difference(expected, mask0));

      // g.5 expected = 'WildcardData      xx yy     '   mask = 'WildcardData      xx yy     '
      assertEquals(SET_TRAILER, dut.difference(expected, mask1));

      // g.6 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
      assertEquals(SET_NONE, dut.difference(expected, mask2));

      // g.7 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy       xx yy     '
      assertEquals(SET_TRAILER, dut.difference(expected, mask3));

      // g.8 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
      expected = new ResponseApdu(Hex.toByteArray("12"), WILDCARD_TRAILER);
      assertEquals(SET_NONE, dut.difference(expected, mask0));

      // g.9 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData      xx yy     '
      assertEquals(SET_NONE, dut.difference(expected, mask1));

      // g.a expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
      assertEquals(SET_LENGTH, dut.difference(expected, mask2));

      // g.b expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
      assertEquals(SET_LENGTH, dut.difference(expected, mask3));

      // g.c expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData WildcardTrailer'
      expected = new ResponseApdu(Hex.toByteArray("34"), 0x6a81);
      assertEquals(SET_NONE, dut.difference(expected, mask0));

      // g.d expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData      xx yy     '
      assertEquals(SET_TRAILER, dut.difference(expected, mask1));

      // g.e expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
      assertEquals(SET_LENGTH, dut.difference(expected, mask2));

      // g.f expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy       xx yy     '
      assertEquals(SET_LEN_TRA, dut.difference(expected, mask3));
    } // end --- g.

    // --- h. this is 'WildcardData     xxyy       '
    // h.0 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
    // h.1 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData      xx yy     '
    // h.2 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
    // h.3 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
    // h.4 expected = 'WildcardData      xx yy     '   mask = 'WildcardData WildcardTrailer'
    // h.5 expected = 'WildcardData      xx yy     '   mask = 'WildcardData      xx yy     '
    // h.6 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
    // h.7 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy       xx yy     '
    // h.8 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
    // h.9 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData      xx yy     '
    // h.a expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
    // h.b expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
    // h.c expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData WildcardTrailer'
    // h.d expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData      xx yy     '
    // h.e expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
    // h.f expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy       xx yy     '
    {
      final ResponseApdu dut = new ResponseApdu(WILDCARD_DATA, 0x9347);

      ResponseApdu expected;

      // h.0 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
      // h.1 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData      xx yy     '
      // h.2 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
      // h.3 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
      expected = new ResponseApdu(WILDCARD_DATA, WILDCARD_TRAILER);
      for (final ResponseApdu mask : new ResponseApdu[] {mask0, mask1, mask2, mask3}) {
        assertEquals(SET_NONE, dut.difference(expected, mask));
      } // end For (mask...)

      // h.4 expected = 'WildcardData      xx yy     '   mask = 'WildcardData WildcardTrailer'
      expected = new ResponseApdu(WILDCARD_DATA, 0x6982);
      assertEquals(SET_NONE, dut.difference(expected, mask0));

      // h.5 expected = 'WildcardData      xx yy     '   mask = 'WildcardData      xx yy     '
      assertEquals(SET_TRAILER, dut.difference(expected, mask1));

      // h.6 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
      assertEquals(SET_NONE, dut.difference(expected, mask2));

      // h.7 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy       xx yy     '
      assertEquals(SET_TRAILER, dut.difference(expected, mask3));

      // h.8 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
      expected = new ResponseApdu(Hex.toByteArray("12"), WILDCARD_TRAILER);
      assertEquals(SET_NONE, dut.difference(expected, mask0));

      // h.9 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData      xx yy     '
      assertEquals(SET_NONE, dut.difference(expected, mask1));

      // h.a expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
      assertEquals(SET_LENGTH, dut.difference(expected, mask2));

      // h.b expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
      assertEquals(SET_LENGTH, dut.difference(expected, mask3));

      // h.c expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData WildcardTrailer'
      expected = new ResponseApdu(Hex.toByteArray("34"), 0x6a81);
      assertEquals(SET_NONE, dut.difference(expected, mask0));

      // h.d expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData      xx yy     '
      assertEquals(SET_TRAILER, dut.difference(expected, mask1));

      // h.e expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
      assertEquals(SET_LENGTH, dut.difference(expected, mask2));

      // h.f expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy       xx yy     '
      assertEquals(SET_LEN_TRA, dut.difference(expected, mask3));
    } // end --- h.

    // --- i. this is 'xx  ...   yy WildcardTrailer'
    // i.0 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
    // i.1 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData      xx yy     '
    // i.2 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
    // i.3 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
    // i.4 expected = 'WildcardData      xx yy     '   mask = 'WildcardData WildcardTrailer'
    // i.5 expected = 'WildcardData      xx yy     '   mask = 'WildcardData      xx yy     '
    // i.6 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
    // i.7 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy       xx yy     '
    // i.8 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
    // i.9 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData      xx yy     '
    // i.a expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
    // i.b expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
    // i.c expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData WildcardTrailer'
    // i.d expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData      xx yy     '
    // i.e expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
    // i.f expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy       xx yy     '
    {
      final ResponseApdu dut = new ResponseApdu(Hex.toByteArray("012345"), WILDCARD_TRAILER);

      ResponseApdu expected;

      // i.0 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
      // i.1 expected = 'WildcardData WildcardTrailer'   mask = 'WildcardData      xx yy     '
      // i.2 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
      // i.3 expected = 'WildcardData WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
      expected = new ResponseApdu(WILDCARD_DATA, WILDCARD_TRAILER);
      for (final ResponseApdu mask : new ResponseApdu[] {mask0, mask1, mask2, mask3}) {
        assertEquals(SET_NONE, dut.difference(expected, mask));
      } // end For (mask...)

      // i.4 expected = 'WildcardData      xx yy     '   mask = 'WildcardData WildcardTrailer'
      expected = new ResponseApdu(WILDCARD_DATA, 0x6982);
      assertEquals(SET_NONE, dut.difference(expected, mask0));

      // i.5 expected = 'WildcardData      xx yy     '   mask = 'WildcardData      xx yy     '
      assertEquals(SET_TRAILER, dut.difference(expected, mask1));

      // i.6 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
      assertEquals(SET_NONE, dut.difference(expected, mask2));

      // i.7 expected = 'WildcardData      xx yy     '   mask = ' xx  ... yy       xx yy     '
      assertEquals(SET_TRAILER, dut.difference(expected, mask3));

      // i.8 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData WildcardTrailer'
      expected = new ResponseApdu(Hex.toByteArray("12"), WILDCARD_TRAILER);
      assertEquals(SET_NONE, dut.difference(expected, mask0));

      // i.9 expected = ' xx  ... yy  WildcardTrailer'   mask = 'WildcardData      xx yy     '
      assertEquals(SET_NONE, dut.difference(expected, mask1));

      // i.a expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy  WildcardTrailer'
      assertEquals(SET_CON_LEN, dut.difference(expected, mask2));

      // i.b expected = ' xx  ... yy  WildcardTrailer'   mask = ' xx  ... yy       xx yy     '
      assertEquals(SET_CON_LEN, dut.difference(expected, mask3));

      // i.c expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData WildcardTrailer'
      expected = new ResponseApdu(Hex.toByteArray("34"), 0x6a81);
      assertEquals(SET_NONE, dut.difference(expected, mask0));

      // i.d expected = ' xx  ... yy       xx yy     '   mask = 'WildcardData      xx yy     '
      assertEquals(SET_TRAILER, dut.difference(expected, mask1));

      // i.e expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy  WildcardTrailer'
      assertEquals(SET_CON_LEN, dut.difference(expected, mask2));

      // i.f expected = ' xx  ... yy       xx yy     '   mask = ' xx  ... yy       xx yy     '
      assertEquals(SET_CO_LE_TR, dut.difference(expected, mask3));
    } // end --- i.

    // --- j. length: t == e == m, all data fields have equal lengths
    // Note: Intentionally here no difference in trailer.
    // j.1 length = 0
    // j.2 length > 0
    {
      TRAILER_VALUES.forEach(
          trailer -> {
            // j.1 length = 0
            final List<byte[]> dataFields = List.of(WILDCARD_DATA, AfiUtils.EMPTY_OS, new byte[0]);
            dataFields.forEach(
                thisData -> {
                  final ResponseApdu dut = new ResponseApdu(thisData, trailer);

                  dataFields.forEach(
                      expectedData -> {
                        final ResponseApdu expected = new ResponseApdu(expectedData, trailer);

                        dataFields.forEach(
                            maskData ->
                                assertEquals(
                                    SET_NONE,
                                    dut.difference(
                                        expected,
                                        new ResponseApdu(
                                            maskData, trailer)))); // end forEach(maskData -> ...)
                      }); // end forEach(expectedData -> ...)
                }); // end forEach(thisData -> ...)

            // j.2 length > 0
            Map.ofEntries(
                    Map.entry(List.of("55", "55", "ff"), SET_NONE), // equal content
                    Map.entry(List.of("ff", "f7", "f7"), SET_NONE), // difference masked out
                    Map.entry(List.of("08", "f7", "08"), SET_CONTENT) // difference not masked out
                    )
                .forEach(
                    (strings, set) -> {
                      final ResponseApdu dut =
                          new ResponseApdu(Hex.toByteArray(strings.get(0)), trailer);
                      final ResponseApdu exp =
                          new ResponseApdu(Hex.toByteArray(strings.get(1)), trailer);
                      final ResponseApdu mas =
                          new ResponseApdu(Hex.toByteArray(strings.get(2)), trailer);

                      assertEquals(set, dut.difference(exp, mas));
                    }); // end forEach((strings, set) -> ...)
          }); // end forEach(trailer -> ...)
    } // end --- j.

    // --- k. length: t == e,   this   and expected have data fields with equal length
    // Note: Intentionally here no difference in trailer.
    // k.1 length(t, e) = 0
    // k.2 length(t, e) > 0
    {
      TRAILER_VALUES.forEach(
          trailer -> {
            // k.1 length(t, e) = 0
            final List<byte[]> dataFields = List.of(WILDCARD_DATA, AfiUtils.EMPTY_OS, new byte[0]);
            dataFields.forEach(
                thisData -> {
                  final ResponseApdu dut = new ResponseApdu(thisData, trailer);

                  dataFields.forEach(
                      expectedData -> {
                        final ResponseApdu expected = new ResponseApdu(expectedData, trailer);

                        RNG.intsClosed(1, 20, 5)
                            .forEach(
                                nrMask ->
                                    assertEquals(
                                        SET_NONE,
                                        dut.difference(
                                            expected,
                                            new ResponseApdu(
                                                RNG.nextBytes(nrMask),
                                                trailer)))); // end forEach(maskData -> ...)
                      }); // end forEach(expectedData -> ...)
                }); // end forEach(thisData -> ...)

            // k.2 length(t, e) > 0
            Map.ofEntries(
                    Map.entry(List.of("2345", "23ff", "ff"), SET_NONE), // no diff, short mask
                    Map.entry(List.of("3456", "2672", "eddbff"), SET_NONE), // difference masked out
                    Map.entry(
                        List.of("749c", "349c", "4000ff"), SET_CONTENT) // difference not masked out
                    )
                .forEach(
                    (strings, set) -> {
                      final ResponseApdu dut =
                          new ResponseApdu(Hex.toByteArray(strings.get(0)), trailer);
                      final ResponseApdu exp =
                          new ResponseApdu(Hex.toByteArray(strings.get(1)), trailer);
                      final ResponseApdu mas =
                          new ResponseApdu(Hex.toByteArray(strings.get(2)), trailer);

                      assertEquals(set, dut.difference(exp, mas));
                    }); // end forEach((strings, set) -> ...)
          }); // end forEach(trailer -> ...)
    } // end for --- k.

    // --- l. length: e == m, expected and   mask   have data fields with equal length
    // Note: Intentionally here no difference in trailer.
    // l.1 length(e, m) = 0
    // l.2 length(e, m) > 0
    {
      TRAILER_VALUES.forEach(
          trailer -> {
            // l.1 length(e, m) = 0
            final List<byte[]> dataFields = List.of(WILDCARD_DATA, AfiUtils.EMPTY_OS, new byte[0]);
            dataFields.forEach(
                maskData -> {
                  final ResponseApdu mask = new ResponseApdu(maskData, trailer);

                  dataFields.forEach(
                      expectedData -> {
                        final ResponseApdu expected = new ResponseApdu(expectedData, trailer);

                        RNG.intsClosed(1, 20, 5)
                            .forEach(
                                nr ->
                                    assertEquals(
                                        SET_NONE,
                                        new ResponseApdu(RNG.nextBytes(nr), trailer)
                                            .difference(expected, mask))); // end forEach(nr -> ...)
                      }); // end forEach(expectedData -> ...)
                }); // end forEach(maskData -> ...)

            // l.2 length(e, m) > 0
            Map.ofEntries(
                    Map.entry(List.of("23", "23ff", "ffff"), SET_LENGTH), // no diff in content
                    Map.entry(List.of("34", "2672", "eddb"), SET_LENGTH), // difference masked out
                    Map.entry(
                        List.of("74", "349c", "4000"), SET_CON_LEN) // difference not masked out
                    )
                .forEach(
                    (strings, set) -> {
                      final ResponseApdu dut =
                          new ResponseApdu(Hex.toByteArray(strings.get(0)), trailer);
                      final ResponseApdu exp =
                          new ResponseApdu(Hex.toByteArray(strings.get(1)), trailer);
                      final ResponseApdu mas =
                          new ResponseApdu(Hex.toByteArray(strings.get(2)), trailer);

                      assertEquals(
                          set,
                          dut.difference(exp, mas),
                          () -> String.format(FORMATTER, dut, exp, mas));
                    }); // end forEach((strings, set) -> ...)
          }); // end forEach(trailer -> ...)
    } // end for --- l.

    // --- m. length: t == m,   this   and   mask   have data fields with equal length
    // Note: Intentionally here no difference in trailer.
    // m.1 length(t, m) = 0
    // m.2 length(t, m) > 0
    {
      TRAILER_VALUES.forEach(
          trailer -> {
            // m.1 length(t, m) = 0
            final List<byte[]> dataFields = List.of(WILDCARD_DATA, AfiUtils.EMPTY_OS, new byte[0]);
            dataFields.forEach(
                thisData -> {
                  final ResponseApdu dut = new ResponseApdu(thisData, trailer);

                  dataFields.forEach(
                      maskedData -> {
                        final ResponseApdu mask = new ResponseApdu(maskedData, trailer);

                        RNG.intsClosed(1, 20, 5)
                            .forEach(
                                nr ->
                                    assertEquals(
                                        SET_NONE,
                                        dut.difference(
                                            new ResponseApdu(RNG.nextBytes(nr), trailer),
                                            mask))); // end forEach(nr -> ...)
                      }); // end forEach(maskedData -> ...)
                }); // end forEach(thisData -> ...)

            // m.2 length(t, m) > 0
            Map.ofEntries(
                    Map.entry(List.of("45", "45ff", "ff"), SET_NONE), // no diff, short mask
                    Map.entry(List.of("3456", "26", "edff"), SET_LENGTH), // difference masked out
                    Map.entry(
                        List.of("749c", "34", "4000"), SET_CON_LEN) // difference not masked out
                    )
                .forEach(
                    (strings, set) -> {
                      final ResponseApdu dut =
                          new ResponseApdu(Hex.toByteArray(strings.get(0)), trailer);
                      final ResponseApdu exp =
                          new ResponseApdu(Hex.toByteArray(strings.get(1)), trailer);
                      final ResponseApdu mas =
                          new ResponseApdu(Hex.toByteArray(strings.get(2)), trailer);

                      assertEquals(
                          set,
                          dut.difference(exp, mas),
                          () -> String.format(FORMATTER, dut, exp, mas));
                    }); // end forEach((strings, set) -> ...)
          }); // end forEach(trailer -> ...)
    } // end for --- m.

    // --- n. length: t < e, m, data field of   this   is shortest
    TRAILER_VALUES.forEach(
        trailer -> {
          Map.ofEntries(
                  Map.entry(List.of("", "78", "ffff"), SET_LENGTH), // 0 = t < e < m
                  Map.entry(List.of("34", "2673", "ed00ff"), SET_LENGTH), // 0 < t < e < m
                  Map.entry(List.of("74", "34ffff", "4000"), SET_CON_LEN) // 0 < t < m < e
                  )
              .forEach(
                  (strings, set) -> {
                    final ResponseApdu dut =
                        new ResponseApdu(Hex.toByteArray(strings.get(0)), trailer);
                    final ResponseApdu exp =
                        new ResponseApdu(Hex.toByteArray(strings.get(1)), trailer);
                    final ResponseApdu mas =
                        new ResponseApdu(Hex.toByteArray(strings.get(2)), trailer);

                    assertEquals(
                        set,
                        dut.difference(exp, mas),
                        () -> String.format(FORMATTER, dut, exp, mas));
                  }); // end forEach((strings, set) -> ...)
        }); // end forEach(trailer -> ...)
    // end --- n.

    // --- o. length: e < t, m, data field of expected is shortest
    TRAILER_VALUES.forEach(
        trailer -> {
          Map.ofEntries(
                  Map.entry(List.of("89", "", "ffff"), SET_LENGTH), // 0 = e < t < m
                  Map.entry(List.of("593d", "58", "fe00ff"), SET_LENGTH), // 0 < e < t < m
                  Map.entry(List.of("593f", "d9", "8000ff"), SET_CON_LEN) // 0 < e < m < t
                  )
              .forEach(
                  (strings, set) -> {
                    final ResponseApdu dut =
                        new ResponseApdu(Hex.toByteArray(strings.get(0)), trailer);
                    final ResponseApdu exp =
                        new ResponseApdu(Hex.toByteArray(strings.get(1)), trailer);
                    final ResponseApdu mas =
                        new ResponseApdu(Hex.toByteArray(strings.get(2)), trailer);

                    assertEquals(
                        set,
                        dut.difference(exp, mas),
                        () -> String.format(FORMATTER, dut, exp, mas));
                  }); // end forEach((strings, set) -> ...)
        }); // end forEach(trailer -> ...)
    // end --- o.

    // --- p. length: m < t, e, data field of   mask   is shortest
    TRAILER_VALUES.forEach(
        trailer -> {
          Map.ofEntries(
                  Map.entry(List.of("9a", "bcde", ""), SET_NONE), // 0 = m < t < e
                  Map.entry(List.of("513d", "59ffff", "f7"), SET_NONE), // 0 < m < t < e
                  Map.entry(List.of("593f30", "49ff", "10"), SET_CONTENT) // 0 < m < e < t
                  )
              .forEach(
                  (strings, set) -> {
                    final ResponseApdu dut =
                        new ResponseApdu(Hex.toByteArray(strings.get(0)), trailer);
                    final ResponseApdu exp =
                        new ResponseApdu(Hex.toByteArray(strings.get(1)), trailer);
                    final ResponseApdu mas =
                        new ResponseApdu(Hex.toByteArray(strings.get(2)), trailer);

                    assertEquals(
                        set,
                        dut.difference(exp, mas),
                        () -> String.format(FORMATTER, dut, exp, mas));
                  }); // end forEach((strings, set) -> ...)
        }); // end forEach(trailer -> ...)
    // end --- p.

    // --- q. just one bit difference in trailer
    // --- r. just one bit difference in data field
    Map.ofEntries(
            // trailer differ, masked out
            Map.entry(List.of("000102 6900", "000102 9620", "ffffff 00df"), SET_NONE),
            // trailer differ, not masked out
            Map.entry(List.of("102030 1234", "102030 127f", "ffffff ff7f"), SET_TRAILER),
            // content differ in 1st octet, masked out
            Map.entry(List.of("54def7 9000", "56def7 9000", "fdffff ffff"), SET_NONE),
            // content differ in 1st octet,  not masked out
            Map.entry(List.of("54def7 6100", "56def7 6100", "ffffff ffff"), SET_CONTENT),
            // content differ in 2nd octet, masked out
            Map.entry(List.of("54def7 9001", "54dff7 9001", "fffeff ffff"), SET_NONE),
            // content differ in 2nd octet,  not masked out
            Map.entry(List.of("54def7 6101", "54dff7 6101", "ffffff ffff"), SET_CONTENT),
            // content differ in 3rd octet, masked out
            Map.entry(List.of("54def7 9002", "54def6 9002", "fffffe ffff"), SET_NONE),
            // content differ in 3rd octet,  not masked out
            Map.entry(List.of("54def7 6102", "54def6 6102", "ffffff ffff"), SET_CONTENT))
        .forEach(
            (strings, set) -> {
              final ResponseApdu dut = new ResponseApdu(strings.get(0));
              final ResponseApdu exp = new ResponseApdu(strings.get(1));
              final ResponseApdu mas = new ResponseApdu(strings.get(2));

              assertEquals(
                  set, dut.difference(exp, mas), () -> String.format(FORMATTER, dut, exp, mas));
            }); // end forEach((strings, set) -> ...)
  } // end method */

  /** Test method for {@link ResponseApdu#equals(Object)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. difference in data field
    // --- e. difference in trailer
    // --- f. different object, but equal content

    // Loop over bunch of data fields
    final var dataFields =
        List.of(WILDCARD_DATA, AfiUtils.EMPTY_OS, new byte[0], RNG.nextBytes(1, 10));
    for (final var data : dataFields) {
      for (final var trailer : RNG.intsClosed(0, WILDCARD_TRAILER, 1024).boxed().toList()) {
        final ResponseApdu dut = new ResponseApdu(data, trailer);

        for (final Object[] obj :
            new Object[][] {
              new Object[] {dut, true}, // --- a. same reference
              new Object[] {null, false}, // --- b. null input
              new Object[] {"afi", false}, // --- c. difference in type
            }) {
          assertEquals(obj[1], dut.equals(obj[0]));
        } // end For (obj...)

        Object obj;
        boolean result;

        // --- d. difference in data field
        // d.1 obj differ in data field Wildcard
        // d.2 obj with shorter data field
        // d.3 obj with longer data field
        {
          if (0 == data.length) {
            // ... data field absent
            //     => either Wildcard or just absent

            // d.1 obj differ in data field Wildcard
            obj =
                new ResponseApdu(dut.isWildcardData() ? AfiUtils.EMPTY_OS : WILDCARD_DATA, trailer);
            result = dut.equals(obj);
            assertFalse(result);
          } else {
            // ... at least one octet in data field

            // d.2 obj with shorter data field
            result =
                dut.equals(new ResponseApdu(Arrays.copyOfRange(data, 0, data.length - 1), trailer));
            assertFalse(result);
          } // end fi

          // d.2 obj with longer data field
          result =
              dut.equals(new ResponseApdu(AfiUtils.concatenate(data, RNG.nextBytes(1)), trailer));
          assertFalse(result);
        } // end --- d.

        // --- e. difference in trailer
        RNG.intsClosed(0, WILDCARD_TRAILER, 1024)
            .forEach(
                trailer2 ->
                    assertEquals(
                        trailer == trailer2, dut.equals(new ResponseApdu(data, trailer2))));

        // --- f. different object, but equal content
        obj = new ResponseApdu(data, trailer);

        assertNotSame(dut, obj);
        result = dut.equals(obj);
        assertTrue(result);
        result = obj.equals(dut);
        assertTrue(result);
      } // end For (trailer...)
    } // end For (data...)
  } // end method */

  /** Test method for {@link ResponseApdu#getBytes()}. */
  @Test
  void test_getBytes() {
    // Assertions:
    // ... a. getSW1()-method works as expected
    // ... b. getSW2()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of data fields and all relevant trailer

    // --- a. smoke test
    {
      final var expected = Hex.extractHexDigits("afde 6234");
      final var dut = new ResponseApdu(expected);

      final var present = Hex.toHexDigits(dut.getBytes());

      assertEquals(expected, present);
    } // end --- a.

    // --- b. loop over a bunch of data fields and all relevant trailer
    List.of(WILDCARD_DATA, AfiUtils.EMPTY_OS, new byte[0], RNG.nextBytes(1, 10))
        .forEach(
            data -> {
              IntStream.rangeClosed(0, WILDCARD_TRAILER)
                  .forEach(
                      trailer -> {
                        final ResponseApdu dut = new ResponseApdu(data, trailer);

                        assertEquals(
                            Hex.toHexDigits(data) + String.format("%04x", trailer & 0xffff),
                            Hex.toHexDigits(dut.getBytes()));
                      }); // end forEach(trailer -> ...)
            }); // end forEach(data -> ...)
  } // end method */

  /** Test method for {@link ResponseApdu#getData()}. */
  @Test
  void test_getData() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of trailer
    // --- c. data field represents WildcardData
    // --- d. check for expected content for arbitrary data fields
    // --- e. check for defensive cloning

    // --- a. smoke test
    {
      final var expected = "fa";
      final var dut = new ResponseApdu(expected + "9000");

      final var present = Hex.toHexDigits(dut.getData());

      assertEquals(expected, present);
    } // end --- a.

    // --- b. loop over a bunch of trailer
    TRAILER_VALUES.forEach(
        trailer -> {
          // --- c. data field represents WildcardData
          assertSame(WILDCARD_DATA, new ResponseApdu(WILDCARD_DATA, trailer).getData());

          // --- d. check for expected content for arbitrary data fields
          // --- e. check for defensive cloning
          RNG.intsClosed(0, 20, 5)
              .forEach(
                  nr -> {
                    final byte[] data = RNG.nextBytes(nr);
                    final ResponseApdu dut = new ResponseApdu(data, trailer);

                    final byte[] data1 = dut.getData();
                    final byte[] data2 = dut.getData();

                    assertArrayEquals(data, data1);
                    assertArrayEquals(data, data2);
                    assertNotSame(data, data1);
                    assertNotSame(data, data2);
                    assertNotSame(data1, data2);
                  }); // end forEach(data -> ...)
        }); // end forEach(trailer -> ...)
  } // end method */

  /** Test method for {@link ResponseApdu#getNr()}. */
  @Test
  void test_getNr() {
    // Assertions:
    // ... a. constructor work as expected

    // Note: Simple method does not need extensive testing. Together with the
    //        assertions, it follows that we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of data fields

    // --- a. smoke test
    {
      final var dut = new ResponseApdu("af 9000");

      assertEquals(1, dut.getNr());
    } // end --- a.

    // --- b. loop over a bunch of data fields
    TRAILER_VALUES.forEach(
        trailer -> {
          assertEquals(0, new ResponseApdu(WILDCARD_DATA, trailer).getNr());

          RNG.intsClosed(0, 20, 5)
              .forEach(
                  nr ->
                      assertEquals(
                          nr,
                          new ResponseApdu(RNG.nextBytes(nr), trailer)
                              .getNr())); // end forEach(data -> ...)
        }); // end forEach(trailer -> ...)
  } // end method */

  /**
   * Test method for get status.
   *
   * <p>In particular: Test method for
   *
   * <ol>
   *   <li>{@link ResponseApdu#getTrailer()},
   *   <li>{@link ResponseApdu#getSw()},
   *   <li>{@link ResponseApdu#getSw1()} and
   *   <li>{@link ResponseApdu#getSw2()}.
   * </ol>
   */
  @Test
  void test_getStatus() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of data fields and all trailer

    // --- a. smoke test
    {
      final var dut = new ResponseApdu("9000");

      assertEquals(0x9000, dut.getSw());
      assertEquals(0x9000, dut.getTrailer());
      assertEquals(0x90, dut.getSw1());
      assertEquals(0x00, dut.getSw2());
    } // end --- a.

    // --- b. loop over a bunch of data fields and all trailer
    List.of(WILDCARD_DATA, AfiUtils.EMPTY_OS, RNG.nextBytes(1, 20))
        .forEach(
            data -> {
              IntStream.rangeClosed(0, WILDCARD_TRAILER)
                  .forEach(
                      trailer -> {
                        final ResponseApdu dut = new ResponseApdu(data, trailer);
                        final int sw = trailer & 0xffff;

                        assertEquals(trailer, dut.getTrailer());
                        assertEquals(sw, dut.getSw());
                        assertEquals(sw >> 8, dut.getSw1());
                        assertEquals(sw & 0xff, dut.getSw2());
                      }); // end forEach(trailer -> ...)
            }); // end forEach(data -> ...)
  } // end method */

  /** Test method for {@link ResponseApdu#hashCode()}. */
  @Test
  void test_hashCode() {
    // Test strategy:
    // --- a. loop over bunch of data fields and trailer
    // --- b. call hashCode()-method again

    // --- a. loop over bunch of data fields and trailer
    List.of(WILDCARD_DATA, AfiUtils.EMPTY_OS, RNG.nextBytes(1, 20))
        .forEach(
            data -> {
              TRAILER_VALUES.forEach(
                  trailer -> {
                    final ResponseApdu dut = new ResponseApdu(data, trailer);

                    assertEquals(trailer * 31 + Arrays.hashCode(data), dut.hashCode());
                  }); // end forEach(trailer -> ...)
            }); // end forEach(data -> ...)

    // --- b. call hashCode()-method again
    final ResponseApdu dut = new ResponseApdu(RNG.nextBytes(2, 10));
    final int hash = dut.hashCode();
    assertEquals(hash, dut.hashCode());
  } // end method */

  /** Test method for {@link ResponseApdu#isWildcardData()}. */
  @Test
  void test_isWildcardData() {
    // Assertions:
    // ... a. constructor work as expected

    // Test strategy:
    // --- a. Wildcard
    // --- b. not Wildcard

    // --- a. Wildcard
    assertTrue(new ResponseApdu(WILDCARD_DATA, 0x9000).isWildcardData());

    // --- b. not Wildcard
    assertFalse(new ResponseApdu(AfiUtils.EMPTY_OS, 0x9000).isWildcardData());
  } // end method */

  /** Test method for serialisation. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_serialize() {
    // Assertions:
    // ... a. equals()-method works as expected

    // Test strategy:
    // --- a. create a bunch of response APDU
    // --- b. serialize each response APDU
    // --- c. deserialize each response APDU
    // --- e. check if deserialized APDU equals original APDU

    // --- a. create a bunch of response APDU
    // a.1 loop over all relevant data field representations
    // a.2 loop over all relevant trailer
    final List<ResponseApdu> originals = new ArrayList<>();
    List.of(
            WILDCARD_DATA,
            AfiUtils.EMPTY_OS,
            new byte[0],
            RNG.nextBytes(1, 20),
            RNG.nextBytes(NE_EXTENDED_WILDCARD))
        .forEach(
            data -> {
              TRAILER_VALUES.forEach(
                  trailer ->
                      originals.add(
                          new ResponseApdu(data, trailer))); // end forEach(trailer -> ...)
            }); // end forEach(data -> ...)

    for (final var rspApdu : originals) {
      byte[] transfer = AfiUtils.EMPTY_OS;
      final String string = rspApdu.toString();
      final int length = string.length();

      // --- b. serialize each response APDU
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(string);
        oos.writeObject(rspApdu);
        oos.writeInt(length);
        oos.flush();
        transfer = baos.toByteArray();
      } catch (IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)

      try (ByteArrayInputStream bais = new ByteArrayInputStream(transfer);
          ObjectInputStream ois = new ObjectInputStream(bais)) {
        // --- c. deserialize each response APDU
        final Object outA = ois.readObject();
        final Object outB = ois.readObject();
        final int outC = ois.readInt();

        // --- e. check if deserialized APDU equals original APDU
        assertEquals(string, outA);
        assertEquals(outB, rspApdu);
        assertEquals(outC, length);
      } catch (ClassNotFoundException | IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (rspApdu...)
  } // end method */

  /** Test method for {@link ResponseApdu#toString()}. */
  @Test
  void test_toString() {
    // Test strategy:
    // --- a. check 'WildcardData WildcardTrailer'
    // --- b. check 'WildcardData      xx yy     '
    // --- c. check ' xx  ...  yy WildcardTrailer'
    // --- d. check ' xx  ...  yy      xx yy     '

    // --- a. check 'WildcardData WildcardTrailer'
    assertEquals(
        "SW1SW2='****'  Data='*'", new ResponseApdu(WILDCARD_DATA, WILDCARD_TRAILER).toString());

    // --- b. check 'WildcardData      xx yy     '
    assertEquals("SW1SW2='6983'  Data='*'", new ResponseApdu(WILDCARD_DATA, 0x6983).toString());

    // --- c. check ' xx  ...  yy WildcardTrailer'
    assertEquals(
        "SW1SW2='****'  Data absent",
        new ResponseApdu(AfiUtils.EMPTY_OS, WILDCARD_TRAILER).toString());
    assertEquals(
        "SW1SW2='****'  Nr='0001'  Data='af'",
        new ResponseApdu(new byte[] {(byte) 0xaf}, WILDCARD_TRAILER).toString());

    // --- d. check ' xx  ...  yy      xx yy     '
    assertEquals(
        "SW1SW2='6481'  Data absent", new ResponseApdu(AfiUtils.EMPTY_OS, 0x6481).toString());
    RNG.intsClosed(1, NE_EXTENDED_WILDCARD, 6)
        .forEach(
            nr -> {
              final byte[] data = RNG.nextBytes(nr);
              final String datS =
                  String.format("  Nr='%04x'  Data='%s'", nr, Hex.toHexDigits(data));

              RNG.intsClosed(0, 0xffff, 20)
                  .forEach(
                      trailer -> {
                        final String sw = String.format("SW1SW2='%04x'", trailer);
                        final ResponseApdu dut = new ResponseApdu(data, trailer);

                        assertEquals(sw + datS, dut.toString());
                      }); // end forEach(trailer -> ...)
            }); // end forEach(nr -> ...)
  } // end method */
} // end class
