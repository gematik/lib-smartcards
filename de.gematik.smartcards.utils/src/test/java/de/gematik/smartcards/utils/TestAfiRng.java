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
package de.gematik.smartcards.utils;

import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link AfiRng}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "Dead store to local variable".
//         This finding is caused by final local constants maxAmount and supremumAmount.
//         That seems to be a false positive.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "DLS_DEAD_LOCAL_STORE", // see note 1
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestAfiRng {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestAfiRng.class); // */

  /** Source of entropy in case test-methods() need random numbers. */
  private static final SecureRandom RNG = new SecureRandom(); // */

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

  /** Test method for {@link AfiRng#intsClosed(int, int, int)}. */
  @Test
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
  void test_intsClosed() {
    // Test strategy:
    // --- a. check result for numberOfEntries not positive
    // --- b. check min == max <=> span == 1
    // --- c. check min < max AND numberOfEntries >= span, i.e. complete range [min, max]
    // --- d. check min < max, numberOfEntries < span, i.e. not all numbers from range [min, max]
    // --- e. check min > max AND numberOfEntries >= span, i.e. complete range
    // --- f. check min > max, numberOfEntries < span, i.e. not all numbers from range

    // --- create device-under-test
    final AfiRng dut = new AfiRng();

    // --- a. check result for numberOfEntries not positive
    Set.of(Integer.MIN_VALUE, -128, -1, 0)
        .forEach(
            numberOfEntries ->
                assertEquals(
                    0,
                    dut.intsClosed(1, 2, numberOfEntries)
                        .count())); // end forEach(numberOfEntries -> ...)

    // --- b. check min == max <=> span == 1
    Set.of(
            Integer.MIN_VALUE, // infimum of possible input
            -128,
            -1,
            0,
            1,
            127, // some arbitrary input
            Integer.MAX_VALUE // supremum of possible input
            )
        .forEach(
            min ->
                assertEquals(
                    "[" + min + "]",
                    dut.intsClosed(min, min, 20)
                        .boxed()
                        .toList()
                        .toString())); // end forEach(min -> ...)

    // --- c. check min < max AND numberOfEntries >= span, i.e. complete range [min, max]
    final String expectedC = "[1, 2, 3, 4, 5, 6]";
    Set.of(6, 7, 16, 128, Integer.MAX_VALUE)
        .forEach(
            numberOfEntries ->
                assertEquals(
                    expectedC,
                    dut.intsClosed(1, 6, numberOfEntries)
                        .boxed()
                        .toList()
                        .toString())); // end forEach(numberOfEntries -> ...)

    // --- d. check min < max, numberOfEntries < span, i.e. not all numbers from range [min, max]
    // d.1: numberOfEntries <= 4
    assertEquals(
        "[1]", // numberOfEntries == 1
        dut.intsClosed(1, 60, 1).boxed().toList().toString());
    assertEquals(
        "[1, 60]", // numberOfEntries == 2
        dut.intsClosed(1, 60, 2).boxed().toList().toString());
    assertEquals(
        "[1, 60, 2]", // numberOfEntries == 3
        dut.intsClosed(1, 60, 3).boxed().toList().toString());
    assertEquals(
        "[1, 60, 2, 59]", // numberOfEntries == 4
        dut.intsClosed(1, 60, 4).boxed().toList().toString());

    // d.2: numberOfEntries "huge"
    final int minNumberOfEntriesD2 = 0;
    final int maxNumberOfEntriesD2 = 1000;
    final int minD2 = -800;
    final int maxD2 = 800;
    IntStream.range(minNumberOfEntriesD2, maxNumberOfEntriesD2)
        .forEach(
            numberOfEntries -> {
              final Set<Integer> result =
                  dut.intsClosed(minD2, maxD2, numberOfEntries).boxed().collect(Collectors.toSet());
              assertEquals(numberOfEntries, result.size());

              if (numberOfEntries >= 1) { // NOPMD avoid literals in conditional statement
                assertTrue(result.contains(minD2), "infimum");
                assertEquals(minD2, result.stream().mapToInt(Integer::intValue).min().getAsInt());
              } // end fi

              if (numberOfEntries >= 2) { // NOPMD avoid literals in conditional statement
                assertTrue(result.contains(maxD2), "supremum");
                assertEquals(maxD2, result.stream().mapToInt(Integer::intValue).max().getAsInt());
              } // end fi

              if (numberOfEntries >= 3) { // NOPMD avoid literals in conditional statement
                assertTrue(result.contains(minD2 + 1), "infimum+1");
              } // end fi

              if (numberOfEntries >= 4) { // NOPMD avoid literals in conditional statement
                assertTrue(result.contains(maxD2 - 1), "supremum-1");
              } // end fi
            }); // end forEach(numberOfElements -> ...)

    // --- e. check with min > max AND numberOfEntries >= span, i.e. complete range
    final int minE = Integer.MAX_VALUE - 1; // two negative values
    final int maxE = Integer.MIN_VALUE + 3; // four positive values
    final String expectedE =
        String.format(
            "[%d, %d, %d, %d, %d, %d]", minE, minE + 1, maxE - 3, maxE - 2, maxE - 1, maxE);
    Set.of(6, 7, 16, 128, Integer.MAX_VALUE)
        .forEach(
            numberOfEntries ->
                assertEquals(
                    expectedE,
                    dut.intsClosed(minE, maxE, numberOfEntries)
                        .boxed()
                        .toList()
                        .toString())); // end forEach(numberOfEntries -> ...)

    // --- f. check min > max, numberOfEntries < span, i.e. not all numbers from range
    // f.1: numberOfEntries <= 4
    assertEquals(
        "[9]", // numberOfEntries == 1
        dut.intsClosed(9, 3, 1).boxed().toList().toString());
    assertEquals(
        "[9, 3]", // numberOfEntries == 2
        dut.intsClosed(9, 3, 2).boxed().toList().toString());
    assertEquals(
        "[9, 3, 10]", // numberOfEntries == 3
        dut.intsClosed(9, 3, 3).boxed().toList().toString());
    assertEquals(
        "[9, 3, 10, 2]", // numberOfEntries == 4
        dut.intsClosed(9, 3, 4).boxed().toList().toString());

    // f.2: numberOfEntries "huge"
    final int minNumberOfEntriesF2 = 0;
    final int maxNumberOfEntriesF2 = 1000;
    final int minF2 = -100;
    final int maxF2 = -300;
    IntStream.range(minNumberOfEntriesF2, maxNumberOfEntriesF2)
        .forEach(
            numberOfEntries -> {
              final Set<Integer> result =
                  dut.intsClosed(minF2, maxF2, numberOfEntries).boxed().collect(Collectors.toSet());
              assertEquals(numberOfEntries, result.size());

              if (numberOfEntries >= 1) { // NOPMD avoid literals in conditional statement
                assertTrue(result.contains(minF2), "infimum");
                assertEquals(
                    minF2,
                    result.stream()
                        .filter(i -> minF2 <= i)
                        .mapToInt(Integer::intValue)
                        .min()
                        .orElseThrow());
              } // end fi

              if (numberOfEntries >= 2) { // NOPMD avoid literals in conditional statement
                assertTrue(result.contains(maxF2), "supremum");
                assertEquals(
                    maxF2,
                    result.stream()
                        .filter(i -> i <= maxF2)
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElseThrow());
              } // end fi

              if (numberOfEntries >= 3) { // NOPMD avoid literals in conditional statement
                assertTrue(result.contains(minF2 + 1), "infimum+1");
              } // end fi

              if (numberOfEntries >= 4) { // NOPMD avoid literals in conditional statement
                assertTrue(result.contains(maxF2 - 1), "supremum-1");
              } // end fi
            }); // end forEach(numberOfElements -> ...)
  } // end method */

  /** Test method for {@link AfiRng#nextBytes(int)}. */
  @Test
  void test_nextBytes_int() {
    // Assertions:
    // ... a. nextBytes(byte[])-method from superclass works as expected

    // Note: Because of the assertions, we can be lazy here.

    // Test strategy:
    // --- a. some manually chosen happy cases
    // --- b. invalid value

    // --- define device-under-test
    final AfiRng dut = new AfiRng();
    final int infimumA = 0; // infimum of happy cases

    // --- a. some manually chosen happy cases
    for (final var length :
        List.of(
            infimumA,
            infimumA + 1, // next to infimum
            42)) {
      assertEquals(length, dut.nextBytes(length).length);
    } // end For (length...)
    // end --- a.

    // --- b. invalid value
    List.of(
            infimumA - 1, // supremum of invalid values
            infimumA - 2, // next to supremum
            Integer.MIN_VALUE // infimum of invalid values
            )
        .forEach(
            length ->
                assertThrows(
                    NegativeArraySizeException.class,
                    () -> dut.nextBytes(length))); // end forEach(length -> ...)
  } // end method */

  /** Test method for {@link AfiRng#nextBytes(int, int)}. */
  @Test
  void test_nextBytes__int_int() {
    // Assertions:
    // ... a. nextDouble()-method from superclass works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. some manually chosen happy cases
    // --- b. min == max
    // --- c. invalid value

    // --- define device-under-test
    final AfiRng dut = new AfiRng();

    // --- a. some manually chosen happy cases
    final int infimumA = 0; // infimum of happy cases
    final int span = 9;
    List.of(
            infimumA,
            infimumA + 1, // next to infimum
            42)
        .forEach(
            length -> {
              final int min = length;
              final int max = length + span;
              final Set<Integer> setValues =
                  IntStream.rangeClosed(min, max).boxed().collect(Collectors.toSet());

              // --- define a value for the maximum number of tries to generate all possible values.
              // Note: This value could be very high, but then it takes some time to fail a test. If
              //       this value is too low a false negative is possible.
              //       Here 64 times the amount of expected values.
              final int maxCounter = Math.multiplyExact(64, setValues.size());

              // --- run until all expected values have been observed (but don't run forever)
              for (int counter = 0; counter < maxCounter; counter++) {
                final int actual = dut.nextBytes(min, max).length;

                // remove actual from setValues
                setValues.remove(actual);

                if (setValues.isEmpty()) {
                  // ... all possible values observed
                  //     => return from test
                  return;
                } // end fi
                // ... set not (yet) empty
                //     => check generated value and check counter

                // check if value is in expected range
                // Note: Here it is min < max. Thus, the actual value has to be from the range
                //       [min, max].
                assertTrue(
                    (min <= actual) || (actual <= max),
                    () -> String.format("]%x, %x[: %x", min, max, actual));
              } // end For (counter...)

              final var message =
                  "not all values observed: "
                      + "min = "
                      + min
                      + ", max = "
                      + max
                      + ", missing values: "
                      + setValues;
              fail(message);
            }); // end forEach(length -> ...)

    // --- b. min == max
    IntStream.range(0, 20)
        .forEach(
            min -> assertEquals(min, dut.nextBytes(min, min).length)); // end forEach(min -> ...)

    // --- c. invalid value
    // c.1 min and max less than zero
    assertThrows(NegativeArraySizeException.class, () -> dut.nextBytes(-2, -1));

    // c.2 min > max
    assertThrows(IllegalArgumentException.class, () -> dut.nextBytes(2, 1));
  } // end method */

  /** Test method for {@link AfiRng#nextIntClosed(int, int)}. */
  @Test
  @SuppressWarnings({"PMD.UnitTestShouldIncludeAssert"})
  void test_nextIntClosed__int_int() {
    // Assertions:
    // ... a. SecureRandom.nextInt() works as expected and generates (nearly) uniformly distributed
    //        random numbers from range [Integer.MIN_VALUE, Integer.MAX_VALUE].
    // ... b. SecureRandom.nextInt(bound) works as expected and generates (nearly) uniformly
    //        distributed random numbers from range [0, bound[.
    // Note: From the assertions it follows that there is no need to test the distribution here.

    // Test strategy:
    // --- a. checks with min == max
    // --- b. checks with small range (all possible values SHAlL occur)
    // --- c. checks for good test-coverage
    // --- d. checks with huge range (edges of range SHALL occur, values in between SHOULD)

    // --- a. checks with min == max
    LOGGER.atTrace().log("start nextIntClosed__min_min");
    nextIntClosedMinMin();

    // --- b. checks with small range (all possible values SHAlL occur)
    LOGGER.atTrace().log("start nextIntClosed__smallRange");
    nextIntClosedSmallRange();

    // --- c. checks for good test-coverage
    LOGGER.atTrace().log("start nextIntClosed__testCoverage");
    nextIntClosedTestCoverage();

    // --- d. checks with huge range (edges of range SHALL occur, values in between SHOULD)
    // see test_nextIntClosed__hugeRange();
  } // end method */

  /**
   * Test method for {@link AfiRng#nextIntClosed(int, int)}.
   *
   * <p>This method checks parameter combination min == max.
   */
  private void nextIntClosedMinMin() {
    // Test strategy:
    // --- a. manually chosen corner cases
    // --- b. all values from a range around zero
    // --- c. random values

    // --- definitions and create device-under-test
    final AfiRng dut = new AfiRng();

    // --- a. manually chosen corner cases
    Set.of(
            Integer.MIN_VALUE, // infimum of possible values
            Integer.MIN_VALUE + 1, // close to infimum
            Integer.MAX_VALUE - 1, // close to supremum
            Integer.MAX_VALUE // supremum of possible values
            )
        .forEach(min -> assertEquals(min, dut.nextIntClosed(min, min)));

    // --- b. all values from a range around zero
    // Note 1: The following range is chosen such that it (hopefully) covers (most of)
    //         real world cases. For test-performance reasons the range isn't too large.
    final int infimum = -32_768;
    final int supremum = 32_767;
    IntStream.rangeClosed(infimum, supremum)
        .forEach(
            min -> {
              assertEquals(min, dut.nextIntClosed(min, min));

              // --- c. random values
              // Note 2: For each value also a random value is used as test-input.
              final int rnd = RNG.nextInt();
              assertEquals(rnd, dut.nextIntClosed(rnd, rnd));
            }); // end forEach(min -> ...)
  } // end method */

  /**
   * Test method for {@link AfiRng#nextIntClosed(int, int)}.
   *
   * <p>This method checks parameter combinations where the amount of possible values is so small
   * that with high probability in reasonable time all values SHALL occur.
   */
  private void nextIntClosedSmallRange() {
    // Test strategy:
    // A (relatively) small range of possible values is build. Then all possible values from that
    // range are added to a set. Afterwards the method-under-test is called and it is observed
    // that (within reasonable time) all possible values are observed and that all generated
    // values are in the expected range.
    // --- a. min < max
    // --- b. min > max
    // Note: The case min == max is tested in nextIntClosed__min_min()-method, see there

    // --- definition
    // Note: The higher the following value, the longer it will take the test to run
    final int span = 1050;

    // --- a. min < max
    // a.1: min == Integer.MIN_VALUE
    getRandomSmallRangeMinLtMax(Integer.MIN_VALUE, Integer.MIN_VALUE + span);

    // a.2: max == Integer.MAX_VALUE
    getRandomSmallRangeMinLtMax(Integer.MAX_VALUE - span, Integer.MAX_VALUE);

    // a.3: min == 0
    getRandomSmallRangeMinLtMax(0, span);

    // a.4: max == 0
    getRandomSmallRangeMinLtMax(-span, 0);

    // a.5: min with manually chosen values around zero
    getRandomSmallRangeMinLtMax(-span / 2, span / 2);

    // a.6:. random values for border
    // Note: Do a parallel test for two reasons:
    //       1. performance boost
    //       2. check if method is thread-safe
    IntStream.range(0, 256)
        .forEach(
            i -> {
              final int border = RNG.nextInt();
              if (border > 0) {
                // ... border > 0, possibly close to Integer.MAX_VALUE
                getRandomSmallRangeMinLtMax(border - span, border);
              } else {
                // ... border <= 0, possibly close to Integer.MIN_VALUE
                getRandomSmallRangeMinLtMax(border, border + span);
              } // end fi
            }); // end forEach(i -> ...)

    // --- b. min > max
    // b.1: min == Integer.MAX_VALUE, min can't be bigger
    getRandomSmallRangeMinGtMax(Integer.MAX_VALUE, Integer.MAX_VALUE + span);

    // b.2: max == Integer.MIN_VALUE, max can't be smaller
    getRandomSmallRangeMinGtMax(Integer.MIN_VALUE - span, Integer.MAX_VALUE);

    // b.3: random values for border
    IntStream.range(0, 256)
        .forEach(
            i -> {
              final int min = Integer.MAX_VALUE - RNG.nextInt(span);
              getRandomSmallRangeMinGtMax(min, min + span);
            }); // end forEach(i -> ...)
  } // end method */

  /**
   * Test method for {@link AfiRng#nextIntClosed(int, int)}.
   *
   * <p>This method checks parameter combinations {@code min < max} and amount of possible values is
   * so small that with high probability in reasonable time all values SHALL occur.
   *
   * @param min infimum of expected values
   * @param max supremum of expected values
   */
  private void getRandomSmallRangeMinLtMax(final int min, final int max) {
    // Test strategy:
    // A (relatively) small range of possible values is build. Then all possible values from that
    // range are added to a set. Afterwards the method-under-test is called and it is observed
    // that (within reasonable time) all possible values are observed and that for all generated
    // values actual it is min <= actual <= max.

    // --- create device-under-test and set of expected values
    final AfiRng dut = new AfiRng();

    final Set<Integer> setValues = new HashSet<>();
    for (int actual = min; true; actual++) {
      setValues.add(actual);
      if (max == actual) {
        // ... end of range reached => break this for loop
        // Note: max is possibly Integer.MAX_VALUE therefore actual is always <= max
        break;
      } // end fi
    } // end For (actual...)

    // --- define a value for the maximum number of tries to generate all possible values.
    // Note: This value could be very high, but then it takes some time to fail a test. If
    //       this value is too low a false negative is possible.
    //       Here 64 times the amount of expected values.
    final int maxCounter = Math.multiplyExact(64, setValues.size());

    // --- run until all expected values have been observed (but don't run forever)
    for (int counter = 0; counter < maxCounter; counter++) {
      final int actual = dut.nextIntClosed(min, max);

      // remove actual from setValues
      setValues.remove(actual);

      if (setValues.isEmpty()) {
        // ... all possible values observed
        //     => return from test
        return;
      } // end fi
      // ... set not (yet) empty
      //     => check generated value and check counter

      // check if value is in expected range
      // Note: Here it is min < max. Thus, the actual value has to be from range [min, max].
      assertTrue(
          (min <= actual) && (actual <= max),
          () -> String.format("[%x, %x]: %x", min, max, actual));
    } // end For (counter...)

    fail(
        "not all values observed:"
            + " min = "
            + min
            + ", max = "
            + max
            + ", missing values: "
            + setValues);
  } // end method */

  /**
   * Test method for {@link AfiRng#nextIntClosed(int, int)}.
   *
   * <p>This method checks parameter combinations {@code min > max} and amount of possible values is
   * so small that with high probability in reasonable time all values SHALL occur.
   *
   * @param min infimum of expected values
   * @param max supremum of expected values
   */
  private void getRandomSmallRangeMinGtMax(final int min, final int max) {
    // Test strategy:
    // A (relatively) small range of possible values is build. Then all possible values from that
    // range are added to a set. Afterwards the method-under-test is called and it is observed
    // that (within reasonable time) all possible values are observed and that for all generated
    // values actual it is (min <= actual) OR (actual <= max).

    // --- create device-under-test and set of expected values
    final AfiRng dut = new AfiRng();

    final Set<Integer> setValues = new HashSet<>();
    for (int actual = min; true; actual++) {
      setValues.add(actual);
      if (max == actual) {
        // ... end of range reached => break this for loop
        break;
      } // end fi
    } // end For (actual...)

    // --- define a value for the maximum number of tries to generate all possible values.
    // Note: This value could be very high, but then it takes some time to fail a test. If
    //       this value is too low a false negative is possible.
    //       Here 64 times the amount of expected values.
    final int maxCounter = Math.multiplyExact(64, setValues.size());

    // --- run until all expected values have been observed (but don't run forever)
    for (int counter = 0; counter < maxCounter; counter++) {
      final int actual = dut.nextIntClosed(min, max);

      // remove actual from setValues
      setValues.remove(actual);

      if (setValues.isEmpty()) {
        // ... all possible values observed
        //     => return from test
        return;
      } // end fi
      // ... set not (yet) empty
      //     => check generated value and check counter

      // check if value is in expected range
      // Note: Here it is min < max. Thus, the actual value has to be from range [min, max].
      assertTrue(
          (min <= actual) || (actual <= max),
          () -> String.format("]%x, %x[: %x", min, max, actual));
    } // end For (counter...)

    fail(
        "not all values observed:"
            + " min="
            + min
            + ", max="
            + max
            + ", missing values: "
            + setValues);
  } // end method */

  /**
   * Test method for {@link AfiRng#nextIntClosed(int, int)}.
   *
   * <p>This method checks some corner-cases for good test-coverage. More sophisticated test are
   * performed in {@link #test_nextIntClosed__hugeRange()}. But that method runs REALLY long.
   */
  private void nextIntClosedTestCoverage() {
    // Test strategy:
    // --- a. checks with full span
    // --- b. checks with almost full span

    // --- definitions and create device-under-test
    final AfiRng dut = new AfiRng();

    // --- a. checks with full span
    // Note: Because here we test the full span no checks are performed on the returned value.
    dut.nextIntClosed(1, 0);

    // --- b. checks with almost full span
    // Note: Here we loop several times for good test-coverage.
    // b.1: min < max
    final int minB1 = 0xc000_0000;
    final int maxB1 = 0x5000_0000;
    IntStream.range(0, 1000)
        .forEach(
            i -> {
              final int rnd = dut.nextIntClosed(minB1, maxB1);
              assertTrue((minB1 <= rnd) && (rnd <= maxB1));
            }); // end forEach(i -> ...)

    // b.2: min > max
    final int minB2 = 0x3000_0000;
    final int maxB2 = 0x0c00_0000;
    IntStream.range(0, 1000)
        .forEach(
            i -> {
              final int rnd = dut.nextIntClosed(minB2, maxB2);
              assertTrue((minB2 <= rnd) || (rnd <= maxB2));
            }); // end forEach(i -> ...)
  } // end method */

  /**
   * Test method for {@link AfiRng#nextIntClosed(int, int)}.
   *
   * <p>This method checks parameter combinations where the number of possible values is so large
   * that it seems not possible to observe all allowed values in reasonable time.
   */
  @Test
  @EnabledIfSystemProperty(named = "afiTestLevel", matches = "high")
  @SuppressWarnings({"PMD.UnitTestShouldIncludeAssert"})
  void test_nextIntClosed__hugeRange() {
    // Test strategy:
    // --- a. manually chosen span
    // --- b. full span
    // --- c. consider all possible combinations for MostSignificantByte of min and max

    // --- a. manually chosen span
    for (final int[] span :
        new int[][] {
          // min < max  AND  min * max > 0, i.e. min and max have the same sign
          // {0x0000_0000, 0x0000_ffff}, //  64 ki
          // {0x7ffe_ffff, 0x7fff_ffff}, //  64 ki + 1
          // {0x8000_0000, 0x8001_ffff}, // 128 ki
          {0xfffd_ffff, 0xffff_ffff}, // 128 ki + 1
          // min < max  AND  min < 0 < max, i.e. min and max have different sign
          // {0xfffd_0005, 0x0001_0004}, // 256 ki
          {0xfffd_fff0, 0x0001_fff0}, // 256 ki + 1
          {0xff00_0000, 0x7fff_ffff}, // more than 2^31, min is representative, max isn't
          {0xfd00_0001, 0x7e00_0000}, // more than 2^31, max is representative, min isn't

          // min > max
          // {0x7fff_fff0, 0x8000_000f}, // 32 values
          {0x7ff8_0000, 0x8011_0000}, //
          // {0x7fef_0000, 0x8008_0000}, //
          // {0x7ff8_0000, 0xffe8_0000}, //
          {0x7ff8_0000, 0xfff8_0000}, // more than 2^31 , note: max is a lonely representative
          {0x7ff8_0000, 0x0007_ffff}, // more than 2^32
        }) {
      final int min = span[0];
      final int max = span[1];
      nextIntClosedHhugeRange(min, max);
    } // end For (span...)

    // --- b. full span
    nextIntClosedHhugeRange(Integer.MIN_VALUE, Integer.MAX_VALUE); // min < max
    nextIntClosedHhugeRange(5, 4); // min > max
    // */

    /* TODO / --- c. consider all possible combinations for MostSignificantByte of min and max
    // Note 1: By considering all possible combinations the expected range
    //         is from somewhat small to (almost) full span.
    // Note 2: This covers both cases: (min < max) as well as (min > max).
    // Note 3: Do a parallel test for two reasons:
    //         1. performance boost
    //         2. check if method is thread-safe
    final int infimumMSByte  = Byte.MIN_VALUE;
    final int supremumMSByte = Byte.MAX_VALUE;
    IntStream.rangeClosed(infimumMSByte, supremumMSByte).forEach(minMSByte -> {
          final int min = (minMSByte << 24) | (RNG.nextInt() & 0x00ff_ffff);
          IntStream.rangeClosed(infimumMSByte, supremumMSByte)
              .forEach(maxMSByte -> {
                final int max = (maxMSByte << 24) | (RNG.nextInt() & 0x00ff_ffff);
                nextIntClosed__hugeRange(min, max);
              }); // end forEach(maxMSByte -> ...)
        }); // end forEach(minMSByte -> ...) */
  } // end method */

  /**
   * Test method for {@link AfiRng#nextIntClosed(int, int)}.
   *
   * <p>This method checks parameter combinations where the amount of possible values is so large
   * that it seems not possible to observe all allowed values in reasonable time.
   *
   * @param min infimum of expected values
   * @param max supremum of expected values
   */
  @SuppressWarnings({"PMD.NPathComplexity", "PMD.CognitiveComplexity"})
  private void nextIntClosedHhugeRange(final int min, final int max) {
    final long startTime = System.nanoTime();
    // Test strategy:
    // a. generate a list of representatives (i.e. a subset
    //    of the expected values but including min and max)
    // b. generate a bunch of random numbers (serial operation).
    // c. check that all generated numbers are an element of the expected range
    // d. convert these numbers to representatives
    // e. remove from the set of expected representatives all observed representatives
    // f. method-under-test passes if all expected representatives are observed

    // --- definitions
    final AfiRng dut = new AfiRng();
    final boolean minLtMax = (min < max);
    LOGGER.atTrace().log("--------------------");
    if (minLtMax) {
      // ... min < max
      LOGGER.atTrace().log("{}", String.format("[ '%08x', '%08x']", min, max));
      LOGGER.atTrace().log("{}", String.format("[%11d,%11d]", min, max));
    } else {
      // ... min >= max
      LOGGER.atTrace().log("{}", String.format("] '%08x', '%08x'[", max, min));
      LOGGER.atTrace().log("{}", String.format("]%11d,%11d[", max, min));
    } // end fi

    // --- calculate total span, i.e. total amount of possible values
    // Note: to avoid an overflow in the following calculations these
    //       are performed in the long-range.
    final long span;
    if (minLtMax) {
      // ... min < max: span = max - min + 1
      // calculate total span, i.e. total amount of possible values
      span = ((long) max) - min + 1;
    } else {
      // ... min > max: span = (MAX_VALUE - min + 1) + (max - MIN_VALUE + 1)
      span = ((long) Integer.MAX_VALUE) - min + 1 + max - Integer.MIN_VALUE + 1;
    } // end fi
    LOGGER.atTrace().log(String.format("span='%08x'=%12d", span, span));

    final int shifter = Math.max(0, (int) (Math.log(span - 1) / Math.log(2)) - 15);
    final int delta = 1 << shifter;
    final int filter = -1 << shifter;
    LOGGER
        .atTrace()
        .log(String.format("shifter=%2d, delta=%08x, filter=%08x", shifter, delta, filter));

    // --- a. Generate a list of representatives
    final Set<Integer> expectedRepresentative = new LinkedHashSet<>(100_000);
    expectedRepresentative.add(min);

    final long end = min + span;
    for (long rep = (min & filter) + delta; rep < end; rep += delta) {
      expectedRepresentative.add((int) rep);
    } // end For (rep...)
    expectedRepresentative.add(max);

    // show representatives
    LOGGER
        .atTrace()
        .log(
            expectedRepresentative.stream()
                .map(i -> String.format("'%08x =%13d", i, i))
                .collect(
                    Collectors.joining(
                        LINE_SEPARATOR,
                        " reps (" + expectedRepresentative.size() + ")= [" + LINE_SEPARATOR,
                        LINE_SEPARATOR + "]")));

    // --- define some constant values
    // Maximum amount of random integer generated. The worst case is full span, i.e. 2^32 possible
    // values. Because here random numbers are generated, maxAmount is larger than that.
    // The higher maxAmount the lower the probability of false rejection.
    final long maxAmount = 0x10_0000_0000L; // 16 times 2^32
    final int noBytesPerRound = 0x1_0000; // 64 kiByte (i.e. MaximumNumberOfBytesPerRequest)
    final int noIntPerRound = noBytesPerRound / 4; // because 4 byte per int
    final long maxRounds = maxAmount / noIntPerRound;
    final int[] numbers = new int[noIntPerRound];
    for (long counter = 0; counter++ < maxRounds; ) { // NOPMD assignment in operands
      // --- b. Generate a bunch of random numbers (serial operation)
      for (int i = noIntPerRound; i-- > 0; ) { // NOPMD assignment in operands
        numbers[i] = dut.nextIntClosed(min, max);
      } // end For (i...)

      // c. --- check that all generated numbers are an element of the expected range
      if (minLtMax) {
        // ... min < max, i.e. random numbers in range [min, max]
        final Set<Integer> outOfRange =
            Arrays.stream(numbers)
                .filter(i -> !((min <= i) && (i <= max)))
                .boxed()
                .collect(Collectors.toSet());
        assertTrue(outOfRange.isEmpty(), () -> String.format("[%d, %d]: %s", min, max, outOfRange));
      } else {
        // ... min > max, i.e. random numbers NOT in range ]max, min[
        final Set<Integer> outOfRange =
            Arrays.stream(numbers)
                .filter(i -> !((min <= i) || (i <= max)))
                .boxed()
                .collect(Collectors.toSet());
        assertTrue(outOfRange.isEmpty(), () -> String.format("]%d, %d[: %s", max, min, outOfRange));
      } // end fi (min < max)

      // --- d. convert these numbers to representatives
      final Set<Integer> observedRepresentatives =
          Arrays.stream(numbers)
              .map(
                  i -> {
                    if (min == i) {
                      return min;
                    } else if (max == i) {
                      return max;
                    } else {
                      return i & filter;
                    } // end fi
                  })
              .boxed()
              .collect(Collectors.toSet());

      // e. remove from the set of expected representatives all observed representatives
      expectedRepresentative.removeAll(observedRepresentatives);
      if (expectedRepresentative.isEmpty()) {
        // ... all possible values observed
        //     => abort this loop
        LOGGER.atInfo().log(
            String.format(
                "test_getRandom__hugeRange(%08x, %08x): counter =%7d, runtime = %s",
                min, max, counter, AfiUtils.nanoSeconds2Time(System.nanoTime() - startTime)));
        return;
      } // end fi (all expected values observed)

      LOGGER
          .atTrace()
          .log(
              String.format(
                  "counter =%6d, size =%6d, set = %s",
                  counter,
                  expectedRepresentative.size(),
                  (expectedRepresentative.size() < 3) ? expectedRepresentative : "(too huge)"));
    } // end For (counter...)

    fail(
        "not all values observed: min="
            + min
            + ", max="
            + max
            + ", size="
            + expectedRepresentative.size()
            + ": "
            + expectedRepresentative);
  } // end method */

  /*
   * Test method for {@link AfiRng#nextIntClosed(int, int)}.
   *
   * <p>############################################################################################
   * ###############################################################################################
   * NOTE 1: The following method is commented out. The method tries to check whether all possible
   * values could be observed and whether the observed values are equally distributed. The ideas
   * behind are not bad, but the implementation isn't good. The runtime is poor and lots of false
   * negative occur. Thus, it is commented out. TODO
   * ###############################################################################################
   * ###############################################################################################
   *
   * <p>This method checks parameter combinations where the amount of possible values is so large
   * that it seems not possible to observe all allowed values in reasonable time.
   *
   * @param min infimum of expected values
   * @param max supremum of expected values
   *
  /* package * void nextIntClosedHugeRangeDistribution(final int min, final int max) {
    // Test strategy:
    // Generate a bunch of random numbers and count (in a histogram fashion) how often the
    // leftmost 12 bit occur. Convert the amount value into a probability and check the
    // observed probability with the expected one.

    // --- estimate expected probability
    // In order to simplify the analysis the following assumption are made:
    // a. random numbers are unsigned 16 bit values.
    // b. histogram takes into account most significant byte.
    // Examples:
    // 1. min = 0x0000, max = 0x01ff, thus, it follows:
    //    - MSBytes 02..ff are never observed.
    //    - MSBytes 00 and 01 are observed with equal probability.
    // 2. min = 0x0000, max = 0x017f, thus, it follows:
    //    - MSBytes 02..ff are never observed.
    //    - MSBytes 00 and 01 are observed. 00 occurs twice as often as 01.
    //    - span is max - min + 1 = 0x17f + 1 = 384
    //    - MSByte 00: span00 = 0x0100 - 0x0000     = 256, probability = span00 / span = 66,6%
    //    - MSByte 01: span01 = 0x017f - 0x0100 + 1 = 128, probability = span01 / span = 33,3%
    // 3. min = 0x0080, max = 0x01ff, thus, it follows:
    //    - MSBytes 02..ff are never observed.
    //    - MSBytes 00 and 01 are observed. 01 occurs twice as often as 00.
    //    - span is max - min + 1 = 0x17f + 1 = 384
    //    - MSByte 00: span00 = 0x0100 - 0x0080     = 128, probability = span00 / span = 33,3%
    //    - MSByte 01: span01 = 0x01ff - 0x0100 + 1 = 256, probability = span01 / span = 66,6%
    // 4. min = 0xff00, max = 0x007f
    //    - MSBytes 01..fe are never observed
    //    - MSBytes 00 and ff are observed. ff occurs twice as often as 00.
    //    - span is (MAX_VALUE - min + 1) + (max - MIN_VALUE + 1) = 256 + 128 = 384
    //    - MSByte 00: span00 = 0x0_007f - 0x0000 + 1 = 128
    //    - MSByte ff: spanff = 0x1_0000 - 0xff00     = 256

    // --- define device-under-test and histogram
    final boolean minLtMax = (min < max);
    final int histogramSize = 4096; // NOPMD declared before use and exit point, 12 bit
    final int shifter = 20; // 20 bit, i.e. 32 bit - (bits in histogramSize)

    final int ms12bitMin = (min >> shifter);
    final int ms12bitMax = (max >> shifter);
    LOGGER.atTrace().log("--------------------");
    if (minLtMax) {
      // ... min < max
      LOGGER.atTrace().log("{}", String.format("[ '%08x', '%08x']", min, max));
      LOGGER.atTrace().log("{}", String.format("[%11d,%11d]", min, max));
    } else {
      // ... min >= max
      LOGGER.atTrace().log("{}", String.format("] '%08x', '%08x'[", max, min));
      LOGGER.atTrace().log("{}", String.format("]%11d,%11d[", max, min));
    } // end fi
    LOGGER.atTrace().log("{}", String.format("miM='%8x'=%6d", ms12bitMin, ms12bitMin));
    LOGGER.atTrace().log("{}", String.format("maM='%8x'=%6d", ms12bitMax, ms12bitMax));

    if (ms12bitMin == ms12bitMax) {
      // ... min and max are too close for the following implementation
      //     => don'r run this test-code
      LOGGER.atTrace().log("min and max are close together: min={}, max={}", min, max);
      return;
    } // end fi
    // ... min != max

    final long[] histogram = new long[histogramSize]; // 16 x 256, i.e. 12 bit
    final double[] expectedProbability = new double[histogramSize];

    // --- calculate total span, i.e. total amount of possible values
    final long span;
    if (minLtMax) {
      // ... min < max: span = max - min + 1
      // calculate total span, i.e. total amount of possible values
      span = ((long) max) - min + 1;
    } else {
      // ... min > max: span = (MAX_VALUE - min + 1) + (max - MIN_VALUE + 1)
      // Note: Obviously the following expression is calculated left to right. It is important
      //       that no overflow occurs. To ensure this the expression starts with a long.
      span = 1L + Integer.MAX_VALUE - min + max - Integer.MIN_VALUE + 1;
    } // end fi
    LOGGER.atTrace().log("{}", String.format("span='%08x'=%12d", span, span));

    // --- calculate spanXy, i.e. total amount of possible values for each histogram category
    int indexMaxProbability = -1; // points to span which is most probable
    int indexMinProbability = -1; // points to span which is least probable
    double maxProbability = 0; // collects highest probability for a span
    double minProbability = 1; // collects least probability for a span
    for (int index = histogramSize; index-- > 0; ) { // NOPMD assignment in operands
      final int ms12bitActual = index - 2048;
      final long spanXy;
      if (ms12bitMin == ms12bitActual) {
        // ... lower end of range
        spanXy = ((ms12bitActual + 1) << shifter) - min;
      } else if (ms12bitMax == ms12bitActual) {
        // ... upper end of range
        spanXy = max - (ms12bitActual << shifter) + 1;
      } else if (minLtMax && (ms12bitMin < ms12bitActual) && (ms12bitActual < ms12bitMax)) {
        // ... min < max and in between lower and upper end of range
        //     => all values with this MostSignificant16Bit possible
        spanXy = 1 << shifter;
      } else if (!minLtMax && ((ms12bitMin < ms12bitActual) || (ms12bitActual < ms12bitMax))) {
        // ... min > max
        //     => all values with this MostSignificant16Bit possible
        spanXy = 1 << shifter;
      } else {
        // ... out of range
        //     => no values with this MostSignificatn16Bit expected
        spanXy = 0;
      } // end fi
      final double probability = ((double) spanXy) / span; // convert to probability
      expectedProbability[index] = probability;

      // adjust variables
      if (probability > maxProbability) {
        indexMaxProbability = index;
        maxProbability = probability;
      } // end fi
      if ((probability > 0) && (probability < minProbability)) {
        indexMinProbability = index;
        minProbability = probability;
      } // end fi

      // debug output
      if (probability > 0) {
        LOGGER
            .atTrace()
            .log(
                String.format(
                    "ms16Bit='%8x'=%5d, spanXY=%7d, p=%e",
                    ms12bitActual, index, spanXy, probability));
      } // end fi
    } // end For (ms16bit...)

    LOGGER
        .atTrace()
        .log(
            String.format("minProbability: index =%5d, p=%e", indexMinProbability, minProbability));
    LOGGER
        .atTrace()
        .log(
            String.format("maxProbability: index =%5d, p=%e", indexMaxProbability, maxProbability));

    // --- estimate optimal amount of random numbers to be generated
    final int infimumAmount = histogramSize * 256; // low value for good performance
    final long supremumAmount = 0x1_0000_0000L; // 1 * 2^32, not too big for good performance
    final double minAmount = 1.0 / minProbability; // minExpected == 1
    final double maxAmount = Long.MAX_VALUE / maxProbability; // no overflow in histogram
    final double pFalseNegative = 1.0 / 10_000; // 100 ppm false rejection rate
    // Note: minProbability contains the least (but positive) value for the probability of a
    //       certain span. The probability for the event "a value from that span is NOT observed
    //       within n rounds is: pFalseNegative = (1-minProbability)^n.
    //       It follows: n = ln(pFalseNegative) / ln(1 - minProbability).
    final double optimalAmount = Math.log(pFalseNegative) / Math.log1p(-minProbability);
    long rounds =
        Math.round(
            Math.max(
                infimumAmount, // at least infimumAmount rounds
                Math.min(supremumAmount, optimalAmount) // no more than supremumAmount rounds
                ));
    LOGGER
        .atTrace()
        .log(
            String.format(
                "min=%e, max=%e, opt=%e, rnd=%d", minAmount, maxAmount, optimalAmount, rounds));

    // --- call method-under-test
    final AfiRng dut = new AfiRng();
    int totalAmountOfRounds = 0;
    for (boolean runFlag = true; runFlag; ) {
      for (long counter = rounds; counter-- > 0; ) { // NOPMD assignment in operands
        final int actual = dut.nextIntClosed(min, max);

        // Note: Observed runtime for min=Integer.MIN_VALUE, max=0x7ff0_0000
        //       runtime histogram  range-check
        //       501 s     int         yes
        //       498 s     int         no
        //       495 s     long        no
        // Conclusion: Runtime doesn't depend much on the implementation here. Thus, the
        //       implementation here concentrates on reliable test results. I.e.:
        //       - in histogram use long => for reasonable executions times no overflow
        //       - perform range check for each generated value
        if (minLtMax) {
          // ... min <= max
          assertTrue(
              (min <= actual) && (actual <= max),
              () -> String.format("[%d, %d]: %d", min, max, actual));
        } else {
          // ... min > max
          assertTrue(
              (min <= actual) || (actual <= max),
              () -> String.format("]%d, %d[: %d", max, min, actual));
        } // end fi

        final int index = (actual >> shifter) + 2048;
        histogram[index]++;
      } // end For (counter...)
      totalAmountOfRounds += rounds;

      if (histogram[indexMinProbability] > 0) {
        // ... a value from the least probable span was observed
        //     => abort
        runFlag = false; // NOPMD assignment of loop control variable
      } else {
        // ... a value from least probable span wasn't yet observed
        //     => adjust the number of rounds and try again
        rounds /= 2;
        runFlag = (rounds > 0); // NOPMD assignment of loop control variable
        LOGGER.atTrace().log("new rounds = " + rounds);
      } // end fi
    } // end For (runFlag...)

    final long totalRounds = totalAmountOfRounds;
    final double inverseAmount = 1.0 / totalRounds;
    // --- calculate divergence
    final String divergenceInfo =
        IntStream.range(0, histogramSize)
            .boxed()
            .map(
                index -> {
                  final double exPro = expectedProbability[index];
                  final long isCount = histogram[index];
                  final double isPro = isCount * inverseAmount;
                  final long exCount = Math.round((exPro * totalRounds));

                  if (exPro > 0) {
                    final int actualDivergence =
                        (int)
                            Math.round(
                                (isPro / exPro - 1.0) * 1e6 // convert to ppm
                                );

                    return String.format(
                        "i=%4d: c=%6d (%6d), is=%.6f, ex=%.6f, div=%6.2f %%",
                        index, isCount, exCount, isPro, exPro, actualDivergence / 10_000.0);
                  } // end fi
                  // ... expectedProbability == 0

                  return String.format(
                      "i=%4d: c=%5d (%5d), is=%.6f, ex=%.6f",
                      index, isCount, exCount, exPro, isPro);
                })
            .collect(Collectors.joining(LINE_SEPARATOR));
    LOGGER.atTrace().log(String.format("observed values:%n%s", divergenceInfo));

    // --- check observed probability against expected probability
    for (int i = histogramSize; i-- > 0; ) { // NOPMD assignment in operands
      final int index = i; // has to be final for lambda expressions
      final double probabilityExpected = expectedProbability[index];
      final long exCount = Math.round((probabilityExpected * totalRounds));
      final long isCount = histogram[index];
      final double probabilityObserved = histogram[index] * inverseAmount;

      if (probabilityExpected > 0) {
        // ... expected probability > 0

        final double divergence = probabilityObserved / probabilityExpected - 1.0;
        if (exCount < 10) { // NOPMD avoid literals in conditional statement
          // ... not much occurrences expected, be tolerant here
          assertTrue(
              (isCount > 0) && (isCount < 2 * exCount),
              () -> "i=" + index + ", is=" + isCount + ", ex=" + exCount);
        } else if (exCount < 100) { // NOPMD avoid literals in conditional statement
          // ... some occurrences expected, accept -50% .. +75%
          assertTrue(
              (divergence > -0.50) && (divergence < 0.75),
              () -> String.format("i=%d, div=%e", index, divergence));
        } else {
          // ... lot of occurrences expected, accept -40% .. +30%
          assertTrue(
              (divergence > -0.40) && (divergence < 0.30),
              () -> String.format("i=%d, div=%e", index, divergence));
        } // end fi
      } else {
        // ... expected probability is zero
        assertEquals(0, histogram[index]);
      } // end fi
    } // end For (index...)
  } // end method */

  /** Test method for {@link AfiRng#nextPrintable()}. */
  @Test
  void test_nextPrintable() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. all results are "printable characters"
    // --- b. all "printable characters" are taken into account

    final AfiRng dut = new AfiRng();
    final String validCharacters =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" // Latin capital letters
            + "abcdefghijklmnopqrstuvwxyz" // Latin small letters
            + "0123456789" // digits
            + " '()+,-./:=?"; // other

    // --- a. all results are "printable characters"
    IntStream.range(0, 10_000)
        .forEach(
            i -> {
              final char character = dut.nextPrintable();
              final int index = validCharacters.indexOf(character);

              assertTrue(index >= 0, () -> String.format("%d: %s", index, character));
            }); // end forEach(i -> ...)
    // end --- a.

    // --- b. all "printable characters" are taken into account
    {
      final List<Integer> list = new ArrayList<>(validCharacters.chars().boxed().toList());

      int counter = 10_000;
      while (!list.isEmpty() && (counter-- > 0)) {
        final int character = dut.nextPrintable();

        list.remove(Integer.valueOf(character));
      } // end While

      assertTrue(list.isEmpty(), list::toString);
    } // end --- b.
  } // end method */

  /** Test method for {@link AfiRng#nextPrintable(int)}. */
  @Test
  void test_nextPrintable__int() {
    // Assertions:
    // ... a. nextPrintable()-method

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NegativeArraySizeException

    final AfiRng dut = new AfiRng();

    // --- a. smoke test
    for (final var length : dut.intsClosed(0, 20, 10).boxed().toList()) {
      final String printable = dut.nextPrintable(length);
      assertEquals(length, printable.length());
    } // end For (length...)
    // end --- a.

    // --- b. ERROR: NegativeArraySizeException
    List.of(
            Integer.MIN_VALUE, // infimum  of invalid values
            -1 // supremum of invalid values
            )
        .forEach(
            length ->
                assertThrows(
                    NegativeArraySizeException.class,
                    () -> dut.nextPrintable(-1))); // end forEach(length -> ...)
  } // end method */

  /** Test method for {@link AfiRng#nextPrintable(int, int)}. */
  @Test
  void test_nextPrintable__int_int() {
    // Assertions:
    // ... a. nextPrintable(int)-method works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. some manually chosen happy cases
    // --- b. min == max
    // --- c. ERROR: invalid value

    // --- define device-under-test
    final AfiRng dut = new AfiRng();

    // --- a. some manually chosen happy cases
    final int infimumA = 0; // infimum of happy cases
    final int span = 9;
    List.of(
            infimumA,
            infimumA + 1, // next to infimum
            42)
        .forEach(
            length -> {
              final int min = length;
              final int max = length + span;
              final Set<Integer> setValues =
                  IntStream.rangeClosed(min, max).boxed().collect(Collectors.toSet());

              // --- define a value for the maximum number of tries to generate all possible values.
              // Note: This value could be very high, but then it takes some time to fail a test. If
              //       this value is too low a false negative is possible.
              //       Here 64 times the amount of expected values.
              final int maxCounter = Math.multiplyExact(64, setValues.size());

              // --- run until all expected values have been observed (but don't run forever)
              for (int counter = 0; counter < maxCounter; counter++) {
                final int actual = dut.nextPrintable(min, max).length();

                // remove actual from setValues
                setValues.remove(actual);

                if (setValues.isEmpty()) {
                  // ... all possible values observed
                  //     => return from test
                  return;
                } // end fi
                // ... set not (yet) empty
                //     => check generated value and check counter

                // check if value is in expected range
                // Note: Here it is min < max. Thus, the actual value has to be from range [min,
                // max].
                assertTrue(
                    (min <= actual) || (actual <= max),
                    () -> String.format("]%x, %x[: %x", min, max, actual));
              } // end For (counter...)

              fail(
                  "not all values observed:"
                      + " min="
                      + min
                      + ", max="
                      + max // NOPMD string appears often
                      + ", missing values: "
                      + setValues);
            }); // end forEach(length -> ...)

    // --- b. min == max
    IntStream.range(0, 20)
        .forEach(
            min ->
                assertEquals(
                    min,
                    dut.nextPrintable(min, min).codePoints().count())); // end forEach(min -> ...)

    // --- c. ERROR: invalid value
    // c.1 ERROR: min and max less than zero
    assertThrows(NegativeArraySizeException.class, () -> dut.nextPrintable(-2, -1));

    // c.2 ERROR: min > max
    assertThrows(IllegalArgumentException.class, () -> dut.nextPrintable(2, 1));
  } // end method */

  /** Test method for {@link AfiRng#nextCodePoint()}. */
  @Test
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  void test_nextCodePoint() {
    // Assertions:
    // ... a. nextDouble()-method from superclass works as expected

    // Test strategy:
    // --- a. generate a bunch of codePoints, check for invalid values and border values

    // --- create device-under-test
    final AfiRng dut = new AfiRng();

    // --- a. generate a bunch of codePoints, check for invalid values and border values
    // Note 1: According to RFC 3629 code points for UTF-8 are in range
    //         [U+0000, U+10FFFF] except the range [U+D800, U+DFFF].
    // Note 2: Intentionally the following loop has NO parallel execution,
    //         because that seems to be faster than a parallel execution.

    // define flagList to control border elements, i.e. elements from set:
    // {U+0000, U+D7FF, U+E000, U+10FFFF}:
    final boolean[] flags = new boolean[4];
    // outer loop, so flags are not checked too often
    for (int counter = 0x800; counter-- > 0; ) { // NOPMD avoid assignment in operands
      IntStream.rangeClosed(0, 0x8000)
          .forEach(
              j -> { // inner loop 32768
                final int rng = dut.nextCodePoint();
                assertTrue(rng >= 0, () -> "negative: " + rng);
                assertTrue(rng <= 0x10_ffff, () -> String.format("too large: %x", rng));
                assertFalse(
                    (0xd800 <= rng) && (rng <= 0xdfff), () -> String.format("invalid:%x", rng));

                if (0 == rng) { // NOPMD literal in if statement
                  // ... border element associated with flag.0
                  flags[0] = true;
                } // end fi

                if (0xd7ff == rng) { // NOPMD literal in if statement
                  // ... border element associated with flag.1
                  flags[1] = true;
                } // end fi

                if (0xe000 == rng) { // NOPMD literal in if statement
                  // ... border element associated with flag.2
                  flags[2] = true;
                } // end fi

                if (0x10_ffff == rng) { // NOPMD literal in if statement
                  // ... border element associated with flag.3
                  flags[3] = true;
                } // end fi
              }); // end forEach(j -> ...)

      if (flags[0] && flags[1] && flags[2] && flags[3]) {
        // ... all flags are true => all border elements observed
        //     => stop looping
        return;
      } // end fi
    } // end For (counter...)

    fail("some border elements absent");
  } // end method */

  /** Test method for {@link AfiRng#nextUtf8(int)}. */
  @Test
  void test_nextUtf8__int() {
    // Assertions:
    // ... a. nextDouble()-method from superclass works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: NegativeArraySizeException

    final AfiRng dut = new AfiRng();

    // --- a. smoke test
    for (final var length : dut.intsClosed(0, 20, 10).boxed().toList()) {
      final String utf8 = dut.nextUtf8(length);
      assertEquals((long) length, utf8.codePoints().count());
    } // end For (length...)
    // end --- a.

    // --- b. ERROR: NegativeArraySizeException
    List.of(
            Integer.MIN_VALUE, // infimum  of invalid values
            -1 // supremum of invalid values
            )
        .forEach(
            length ->
                assertThrows(
                    NegativeArraySizeException.class,
                    () -> dut.nextUtf8(-1))); // end forEach(length -> ...)
  } // end method */

  /** Test method for {@link AfiRng#nextUtf8(int, int)}. */
  @Test
  void test_nextUtf8__int_int() {
    // Assertions:
    // ... a. nextDouble()-method from superclass works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. some manually chosen happy cases
    // --- b. min == max
    // --- c. ERROR: invalid value

    // --- define device-under-test
    final AfiRng dut = new AfiRng();

    // --- a. some manually chosen happy cases
    final int infimumA = 0; // infimum of happy cases
    final int span = 9;
    List.of(
            infimumA,
            infimumA + 1, // next to infimum
            42)
        .forEach(
            length -> {
              final int min = length;
              final int max = length + span;
              final Set<Integer> setValues =
                  IntStream.rangeClosed(min, max).boxed().collect(Collectors.toSet());

              // --- define a value for the maximum number of tries to generate all possible values.
              // Note: This value could be very high, but then it takes some time to fail a test. If
              //       this value is too low a false negative is possible.
              //       Here 64 times the amount of expected values.
              final int maxCounter = Math.multiplyExact(64, setValues.size());

              // --- run until all expected values have been observed (but don't run forever)
              for (int counter = 0; counter < maxCounter; counter++) {
                final int actual = (int) dut.nextUtf8(min, max).codePoints().count();

                // remove actual from setValues
                setValues.remove(actual);

                if (setValues.isEmpty()) {
                  // ... all possible values observed
                  //     => return from test
                  return;
                } // end fi
                // ... set not (yet) empty
                //     => check generated value and check counter

                // check if value is in expected range
                // Note: Here it is min < max. Thus, the actual value has to be from range [min,
                // max].
                assertTrue(
                    (min <= actual) || (actual <= max),
                    () -> String.format("]%x, %x[: %x", min, max, actual));
              } // end For (counter...)

              fail(
                  "not all values observed:"
                      + " min="
                      + min
                      + ", max="
                      + max // NOPMD string appears often
                      + ", missing values: "
                      + setValues);
            }); // end forEach(length -> ...)

    // --- b. min == max
    IntStream.range(0, 20)
        .forEach(
            min ->
                assertEquals(
                    min, dut.nextUtf8(min, min).codePoints().count())); // end forEach(min -> ...)

    // --- c. ERROR: invalid value
    // c.1 ERROR: min and max less than zero
    assertThrows(NegativeArraySizeException.class, () -> dut.nextUtf8(-2, -1));

    // c.2 ERROR: min > max
    assertThrows(IllegalArgumentException.class, () -> dut.nextUtf8(2, 1));
  } // end method */
} // end class
