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

import static de.gematik.smartcards.utils.AfiUtils.EXBI;
import static de.gematik.smartcards.utils.AfiUtils.GIBI;
import static de.gematik.smartcards.utils.AfiUtils.KIBI;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static de.gematik.smartcards.utils.AfiUtils.MEBI;
import static de.gematik.smartcards.utils.AfiUtils.PEBI;
import static de.gematik.smartcards.utils.AfiUtils.TEBI;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static de.gematik.smartcards.utils.AfiUtils.VALUES_BOOLEAN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link AfiUtils}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "ES_COMPARING_PARAMETER_STRING_WITH_EQ".
//         Short message: Comparison of String parameter using == or !=
//         Rationale: That finding is correct, but intentionally assertSame(...) is used.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ES_COMPARING_STRINGS_WITH_EQ", // see note 1
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestAfiUtils {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestAfiUtils.class); // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Standard format. */
  private static final String STAD_FORMAT = "%s%6.3f\""; // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // --- check constants
    assertEquals(KIBI, Math.round(Math.pow(2, 10)), KIBI);
    assertEquals(MEBI, Math.round(Math.pow(2, 20)), MEBI);
    assertEquals(GIBI, Math.round(Math.pow(2, 30)), GIBI);
    assertEquals(TEBI, Math.round(Math.pow(2, 40)), TEBI);
    assertEquals(PEBI, Math.round(Math.pow(2, 50)), PEBI);
    assertEquals(EXBI, Math.round(Math.pow(2, 60)), EXBI);
    assertEquals(0, AfiUtils.EMPTY_OS.length);
    assertEquals(AfiUtils.INVERSE_LN_2, 1.0 / Math.log(2), Double.MIN_VALUE, "1/ln(2)");
    assertSame(LINE_SEPARATOR, System.lineSeparator()); // ES_COMPARING_PARAMETER_STRING_WITH_EQ
    assertFalse(AfiUtils.SPECIAL_INT.isEmpty());
    assertFalse(AfiUtils.SPECIAL_LONG.isEmpty());
    assertTrue(VALUES_BOOLEAN.getFirst());
    assertFalse(VALUES_BOOLEAN.getLast());
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

  /** Test method for {@link AfiUtils#chill(long)}. */
  @Test
  void test_chill__long() {
    // Assumptions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. interrupt
    // --- c. negative input

    // --- a. smoke test
    {
      final int numItems = 10;
      final AfiMean mean =
          new AfiMean(
              IntStream.rangeClosed(1, numItems)
                  .mapToDouble(
                      i -> {
                        final int min = 100 * i;
                        final long sleepTime = RNG.nextIntClosed(min, min + 100);
                        final long startTime = System.nanoTime();

                        AfiUtils.chill(sleepTime);

                        final double runTime =
                            (System.nanoTime() - startTime) * 1e-6; // convert [ns] -> [ms]

                        return runTime - sleepTime; // delta
                      })
                  .toArray());

      assertEquals(
          "",
          // Note: The check-values are estimated on a fast Linux host.
          //       Especially for Windows hosts they are rather restrict.
          // mean.check(numItems, -10, 60, 0, 10), // Linux host
          mean.check(numItems, -10, 600, 0, 100), // GNBE1058
          mean::toString);
    } // end --- a.

    // --- b. interrupt
    {
      final Thread thread = new Thread(() -> AfiUtils.chill(10_000));
      thread.start();
      AfiUtils.chill(500); // give thread time to start

      final long startTime = System.nanoTime();
      assertDoesNotThrow(thread::interrupt);
      try {
        thread.join();
      } catch (InterruptedException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
      final long runTime = System.nanoTime() - startTime;

      assertEquals(
          10, // expected [ms]
          runTime * 1e-6, // present [ms]
          10.0, // acceptable delta [ms]
          () ->
              String.format(
                  "expected=%s, present=%s",
                  AfiUtils.nanoSeconds2Time(startTime), AfiUtils.nanoSeconds2Time(runTime)));
    } // end --- b.

    // --- c. negative input
    {
      final int numItems = 10;
      final AfiMean mean =
          new AfiMean(
              IntStream.rangeClosed(1, numItems)
                  .mapToDouble(
                      i -> {
                        final long sleepTime = -1;
                        final long startTime = System.nanoTime();

                        AfiUtils.chill(sleepTime);

                        return (System.nanoTime() - startTime) * 1e-6; // delta
                      })
                  .toArray());

      assertEquals("", mean.check(numItems, -10, 60, 0, 10), mean::toString);
    } // end --- c.
  } // end method */

  /** Test method for {@link AfiUtils#concatenate(byte[][])}. */
  @Test
  void test_concatenate__byteA2() {
    // Test strategy:
    // --- a. Input with length zero, i.e. empty input
    // --- b. Define some fixed input values and loop over all combinations
    // --- c. Loop over random byte-array of various length

    // --- a. Input with length zero, i.e. empty input
    {
      final var result = AfiUtils.concatenate();
      assertNotNull(result, "10");
      assertEquals(0, result.length, "12");
    } // end --- a.

    // --- b. Define some fixed input values and loop over all combinations
    // b.1 fix input, two arrays
    Set.of(
            "", // empty
            "07", // one octet
            "1234", // two octet
            "654321" // three octet
            )
        .forEach(
            i -> {
              final byte[] in1 = Hex.toByteArray(i);

              Set.of(
                      "", // empty
                      "89", // one octet
                      "7abc", // two octet
                      "fdecba" // three octet
                      )
                  .forEach(
                      j ->
                          assertArrayEquals(
                              Hex.toByteArray(i + j),
                              AfiUtils.concatenate(in1, Hex.toByteArray(j)),
                              () -> i + j)); // end forEach(j -> ...)
            }); // end forEach(i -> ...)

    // --- c. Loop over random byte-array of various length
    IntStream.range(0, 256)
        .forEach(
            i -> {
              final StringBuilder expected = new StringBuilder();
              final byte[][] input = new byte[i][];

              // create elements for input
              IntStream.range(0, i) // loop and create input
                  // Note: Don't do parallel() here, test will fail.
                  //       Best guess is that RNG.nextBytes is NOT thread-safe.
                  .forEach(
                      j -> {
                        final byte[] bytes = RNG.nextBytes(0, 16);
                        input[j] = bytes;
                        expected.append(Hex.toHexDigits(bytes));
                      }); // end forEach(j -> ...)

              // perform test
              assertEquals(
                  expected.toString(),
                  Hex.toHexDigits(AfiUtils.concatenate(input)),
                  () -> Integer.toString(i));
            }); // end forEach(i -> ...)
  } // end method */

  /** Test method for {@link AfiUtils#concatenate(Collection)}. */
  @Test
  void test_concatenate__Collection() {
    // Test strategy:
    // --- a. Input with length zero, i.e. empty input
    // --- b. Define some fixed input values and loop over all combinations
    // --- c. Loop over random byte-array of various length

    // --- a. Input with length zero, i.e. empty input
    {
      final var result = AfiUtils.concatenate(Collections.emptyList());
      assertNotNull(result, "10");
      assertEquals(0, result.length, "12");
    } // end --- a.

    // --- b. Define some fixed input values and loop over all combinations
    // b.1 fix input, two arrays
    Set.of(
            "", // empty
            "07", // one octet
            "1234", // two octet
            "654321" // three octet
            )
        .forEach(
            i -> {
              final byte[] in1 = Hex.toByteArray(i);

              Set.of(
                      "", // empty
                      "89", // one octet
                      "7abc", // two octet
                      "fdecba" // three octet
                      )
                  .forEach(
                      j ->
                          assertArrayEquals(
                              Hex.toByteArray(i + j),
                              AfiUtils.concatenate(List.of(in1, Hex.toByteArray(j))),
                              () -> i + j)); // end forEach(j -> ...)
            }); // end forEach(i -> ...)

    // --- c. Loop over random byte-array of various length
    {
      final List<byte[]> input = new ArrayList<>();
      IntStream.range(0, 256)
          .forEach(
              i -> {
                final StringBuilder expected = new StringBuilder();
                input.clear();

                // create elements for input
                IntStream.range(0, i) // loop and create input
                    // Note: Don't do parallel() here, test will fail.
                    //       Best guess is that RNG.nextBytes is NOT thread-safe.
                    .forEach(
                        j -> {
                          final byte[] bytes = RNG.nextBytes(0, 16);
                          input.add(bytes);
                          expected.append(Hex.toHexDigits(bytes));
                        }); // end forEach(j -> ...)

                // perform test
                assertEquals(
                    expected.toString(),
                    Hex.toHexDigits(AfiUtils.concatenate(input)),
                    () -> Integer.toString(i));
              }); // end forEach(i -> ...)
    } // end --- c.
  } // end method */

  /** Test method for {@link AfiUtils#entropy(int[])}. */
  @Test
  void test_entropy__intA() {
    // Test strategy:
    // --- a. manual tests with arrays.length <= 1
    // --- b. manual tests with exact known result
    // --- c. automatic tests with random input.

    // --- a. manual tests with arrays.length <= 1
    {
      final var expected = 0.0;
      final var delta = 1e-12;

      for (final var i :
          new int[][] {
            new int[0], // no element at all
            new int[] {-1}, // one element with various values
            new int[] {0},
            new int[] {1},
            new int[] {127},
            new int[] {Integer.MIN_VALUE},
            new int[] {Integer.MAX_VALUE},
          }) {
        final var actual = AfiUtils.entropy(i);

        assertEquals(
            expected, actual, delta, () -> ((0 == i.length) ? "{}" : Integer.toString(i[0])));
      } // end For (i...)
    } // end --- a.

    // --- b. manual tests with an exact known result
    Map.ofEntries(
            // elements all equal => entropy = 0
            Map.entry(List.of(0, 0), 0.0),
            Map.entry(List.of(0, 0, 0), 0.0),
            Map.entry(List.of(0, 0, 0, 0), 0.0),

            // elements all different => full entropy
            Map.entry(List.of(0, 1), 1.0),
            Map.entry(List.of(1, 0, 1, 0), 1.0),
            Map.entry(List.of(0, 0, 1, 1), 1.0),
            Map.entry(List.of(1, 2, 3, 4), 2.0),
            Map.entry(List.of(1, 2, 3, 4, 4, 3, 1, 2), 2.0),
            Map.entry(List.of(1, 2, 3, 4, 5, 8, 9, 0), 3.0))
        .forEach(
            (key, value) -> {
              final double expected = value;

              // original array
              final int[] inputO = key.stream().mapToInt(j -> j).toArray();
              assertEquals(
                  expected,
                  AfiUtils.entropy(inputO),
                  1e-12, // delta
                  key::toString);

              // original array + 1
              final int[] inputP1 = key.stream().mapToInt(j -> j + 1).toArray();
              assertEquals(
                  expected,
                  AfiUtils.entropy(inputP1),
                  1e-12, // delta
                  key::toString);

              // original array * 3
              final int[] inputM3 = key.stream().mapToInt(j -> 3 * j).toArray();
              assertEquals(
                  expected,
                  AfiUtils.entropy(inputM3),
                  1e-12, // delta
                  key::toString);
            }); // end forEach(i -> {}

    // --- c. automatic tests with random input.
    IntStream.range(0, 1024)
        .forEach(
            i -> {
              final int[] counter = new int[16];
              final int[] input = new int[i];
              for (int j = i; j-- > 0; ) { // NOPMD avoid assignment in operands
                final int rnd = RNG.nextInt(16);
                input[j] = rnd;
                counter[rnd]++; // adjust counter
              } // end For (j...)

              double expected = 0;
              for (final int j : counter) {
                if (0 == j) {
                  // ... number of occurrences is zero => entropy is also zero
                  continue;
                } // end fi
                final double probability = (double) j / i;
                final double entropy = probability * Math.log(probability) / Math.log(2);
                expected -= entropy;
              } // end For (j...)
              assertEquals(
                  expected,
                  AfiUtils.entropy(input),
                  1e-12, // delta
                  () -> Integer.toString(i));
            }); // end forEach(i -> {})
  } // end method */

  /** Test method for {@link AfiUtils#entropy(int[])}. */
  @Test
  void test_entropy__intA_performance() {
    // Test strategy:
    // a) For a huge amount of data the entropy is estimated and the runtime is observed.

    // --- create input
    final int[] counter = new int[16];
    final int[] input = new int[16 * MEBI]; // a multiple of 8
    for (int j = input.length; j > 0; ) {
      int rndNumber = RNG.nextInt();
      // System.out.printf("rndNumber = %08x ---------------%n", rndNumber);
      for (int k = 8; k-- > 0; rndNumber >>= 4) { // NOPMD assignment in operands
        final int rndNibble = rndNumber & 0xf;
        // System.out.printf("   nibble = %x%n", rndNibble);
        input[--j] = rndNibble; // NOPMD reassignment of loop variable
        counter[rndNibble]++;
      } // end For (k...)
    } // end For (j...)

    // --- estimate expected entropy
    double expected = 0;
    for (final int j : counter) {
      if (0 == j) {
        // ... number of occurrences is zero => entropy is also zero
        continue;
      } // end fi
      final double probability = (double) j / input.length;
      final double entropy = probability * Math.log(probability) / Math.log(2);
      expected -= entropy;
    } // end For (j...)

    // --- call method-under-test
    final var startTime = System.nanoTime();
    final var present = AfiUtils.entropy(input);
    final var runTime = System.nanoTime() - startTime;
    LOGGER.atInfo().log("test_entropy__intA_performance: {}", AfiUtils.nanoSeconds2Time(runTime));

    assertEquals(
        expected, present, 1e-12, // delta
        "10");
  } // end method */

  /** Test method for {@link AfiUtils#exec(String[])}. */
  @Test
  void test_exec__String() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with command "date".
    // --- b. empty string on IllegalArgumentException

    // --- a. smoke test with command "date".
    {
      final var present = AfiUtils.exec("date");

      assertFalse(present.isEmpty());
    } // end --- a.

    // --- b. empty string on IllegalArgumentException
    {
      final var present = AfiUtils.exec("");

      assertTrue(present.isEmpty());
    } // end --- b.
  } // end method */

  /** Test method for {@link AfiUtils#hostname()}. */
  @Test
  void test_hostname() {
    // Assertions:
    // ... a. exec(String)-method works as expected

    // Note: The value returned by the function-under-test has to be controlled
    //       manually. Thus, we log an appropriate message at INFO-level.

    // Test strategy:
    // --- a. smoke test
    final var present = AfiUtils.hostname();

    LOGGER.atInfo().log("hostname = \"{}\"", present);
    assertFalse(present.isEmpty());
  } // end method */

  /** Test method for {@link AfiUtils#incrementCounter(byte[])}. */
  @Test
  void test_incrementCounter__byteA() {
    // Test strategy
    // --- a. Loop over counter with length 1, 2 and 3 (should be enough)
    // --- b. For each counter-length loop over all possible values

    // --- a. Loop over counter with length 1, 2 and 3 (should be enough)
    for (int i = 1; i < 4; i++) {
      final byte[] dut = new byte[i]; // NOPMD new in loop
      final int supremum = (1 << (i << 3)); // = 2^(8i)

      // --- b. For each counter-length loop over all possible values
      for (int expected = 1; expected < (supremum + 5); expected++) {
        assertSame(dut, AfiUtils.incrementCounter(dut));
        assertEquals(expected & (supremum - 1), new BigInteger(1, dut).intValueExact()); // NOPMD
      } // end For (expected...)
    } // end For (i...)
  } // end method */

  /** Test method for {@link AfiUtils#seconds2Time(long)}. */
  @Test
  void test_seconds2Time__long() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. Invalid input throws an exception
    // --- b. Manual happy cases
    // --- c. Huge input values

    // --- a. Invalid input throws an exception
    for (final var i : AfiUtils.SPECIAL_LONG) {
      if (i < 0) {
        // ... check invalid input
        final var throwable =
            assertThrows(
                IllegalArgumentException.class,
                () -> AfiUtils.seconds2Time(i),
                () -> Long.toString(i, 16));
        assertEquals("input negative", throwable.getMessage(), () -> Long.toString(i, 16));
        assertNull(throwable.getCause(), () -> Long.toString(i, 16));
      } // end fi
    } // end For (i...)
    // end --- a.

    // --- b. Manual happy cases
    Map.ofEntries(
            Map.entry(0L, " 0\""), // infimum of valid input
            Map.entry(1L, " 1\""), // next to infimum
            Map.entry(59L, "59\""), // just below 1 minute
            Map.entry(60L, " 1'  0\""), // 1 minute
            Map.entry(3_599L, "59' 59\""), // just below 1 hour
            Map.entry(3_600L, " 1h  0'  0\""), // 1 hour
            Map.entry(86_399L, "23h 59' 59\""), // just below 1 day
            Map.entry(86_400L, "  1d  0h  0'  0\""), // 1 day
            Map.entry(31_535_999L, "364d 23h 59' 59\""), // just below 1 year
            Map.entry(31_536_000L, "1y   0d  0h  0'  0\"") // 1 year
            )
        .forEach(
            (input, expected) ->
                assertEquals(
                    expected,
                    AfiUtils.seconds2Time(input),
                    expected)); // end forEach((input, value) -> ...)

    // --- c. Huge input values
    Map.ofEntries(
            Map.entry(Long.MAX_VALUE, "292471208677y 195d 15h 30'  7\"") // supremum
            )
        .forEach(
            (input, expected) ->
                assertEquals(
                    expected,
                    AfiUtils.seconds2Time(input),
                    expected)); // end forEach((input, value) -> ...)
  } // end method */

  /** Test method for {@link AfiUtils#milliSeconds2Time(long)}. */
  @Test
  void test_milliSeconds2Time__long() {
    // Test strategy:
    // --- a. Invalid input throws an exception
    // --- b. Manual happy cases
    // --- c. Huge input values

    // --- a. Invalid input throws an exception
    for (final var i : AfiUtils.SPECIAL_LONG) {
      if (i < 0) {
        // ... check invalid input
        final var throwable =
            assertThrows(
                IllegalArgumentException.class,
                () -> AfiUtils.milliSeconds2Time(i),
                () -> Long.toString(i, 16));
        assertEquals("input negative", throwable.getMessage(), () -> Long.toString(i, 16));
        assertNull(throwable.getCause(), () -> Long.toString(i, 16));
      } // end fi
    } // end For (i...)
    // end --- a.

    // --- b. Manual happy cases
    Map.ofEntries(
            Map.entry(0L, List.of("0 ms")), // infimum of valid input
            Map.entry(1L, List.of("1 ms")), // next to infimum
            Map.entry(999L, List.of("999 ms")), // just below 1 second
            Map.entry(1_000L, List.of("", 1.0)), // 1 second
            Map.entry(59_999L, List.of("", 59.999)), // just below 1 minute
            Map.entry(60_000L, List.of(" 1' ", 0.0)), // 1 minute
            Map.entry(3_599_999L, List.of("59' ", 59.999)), // just below 1 hour
            Map.entry(3_600_000L, List.of(" 1h  0' ", 0.0)), // 1 hour
            Map.entry(86_399_999L, List.of("23h 59' ", 59.999)), // just below 1 day
            Map.entry(86_400_000L, List.of("  1d  0h  0' ", 0.0)), // 1 day
            Map.entry(31_535_999_999L, List.of("364d 23h 59' ", 59.999)), // just below 1 year
            Map.entry(31_536_000_000L, List.of("1y   0d  0h  0' ", 0.0)) // 1 year
            )
        .forEach(
            (input, value) -> {
              final String prefix = (String) value.get(0);

              final String expected;
              if (1 == value.size()) { // NOPMD literal in if statement
                // ... just one value in list
                //     => use that as expected value
                expected = prefix;
              } else {
                // ... more than one value in list
                final double seconds = (Double) value.get(1);
                expected = String.format(STAD_FORMAT, prefix, seconds);
              } // end else

              assertEquals(expected, AfiUtils.milliSeconds2Time(input), expected);
            }); // end forEach((input, value) -> ...)

    // --- c. Huge input values
    Map.ofEntries(
            Map.entry(Long.MAX_VALUE, List.of("292471208y 247d  7h 12' ", 55.807)) // supremum
            )
        .forEach(
            (input, value) -> {
              final String prefix = (String) value.get(0);
              final double seconds = (Double) value.get(1);
              final String expected = String.format(STAD_FORMAT, prefix, seconds);

              assertEquals(expected, AfiUtils.milliSeconds2Time(input), expected);
            }); // end forEach((input, value) -> ...)
  } // end method */

  /** Test method for {@link AfiUtils#nanoSeconds2Time(long)}. */
  @Test
  void test_nanoSeconds2Time__long() {
    // Test strategy:
    // --- a. Invalid input throws an exception
    // --- b. Manual happy cases
    // --- c. Huge for input

    // --- a. Invalid input throws an exception
    for (final var i : AfiUtils.SPECIAL_LONG) {
      if (i < 0) {
        // ... check invalid input
        final var throwable =
            assertThrows(
                IllegalArgumentException.class,
                () -> AfiUtils.nanoSeconds2Time(i),
                () -> Long.toString(i, 16));
        assertEquals("input negative", throwable.getMessage(), () -> Long.toString(i, 16));
        assertNull(throwable.getCause(), () -> Long.toString(i, 16));
      } // end fi
    } // end For (i...)
    // end --- a.

    // --- b. Manual happy cases
    Map.ofEntries(
            Map.entry(0L, List.of("0 ns")), // infimum
            Map.entry(1L, List.of("1 ns")), // next to infimum
            Map.entry(999L, List.of("999 ns")), // just below 1 us
            Map.entry(1_000L, List.of("", 1.0, "%s%.3f us")), // 1 us
            Map.entry(59_999L, List.of("", 59.999, "%s%.3f us")), // arbitrary value
            Map.entry(999_999L, List.of("", 999.999, "%s%.3f us")), // just below 1 ms
            Map.entry(1_000_000L, List.of("", 1.0, "%s%.6f ms")), // 1 ms
            Map.entry(999_999_999L, List.of("", 999.999_999, "%s%.6f ms")), // just below 1 s
            Map.entry(1_000_000_000L, List.of("", 1_000.0, "%s%.6f ms")), // 1 s
            Map.entry(1_000_000_001L, List.of("", 1.0, STAD_FORMAT)), // just above 1"
            Map.entry(3_600_000_000_000L, List.of(" 1h  0' ", 0.0, STAD_FORMAT)) // arbitrary value
            )
        .forEach(
            (input, value) -> {
              final String prefix = (String) value.get(0);

              final String expected;
              if (1 == value.size()) { // NOPMD literal in if statement
                // ... just one value in list
                //     => use that as expected value
                expected = prefix;
              } else {
                // ... more than one value in list
                final double number = (Double) value.get(1);
                final String format = (String) value.get(2);
                expected = String.format(format, prefix, number);
              } // end else

              assertEquals(expected, AfiUtils.nanoSeconds2Time(input), expected);
            }); // end forEach(entry -> ...)

    // --- c. Huge input values
    Map.ofEntries(
            Map.entry(Long.MAX_VALUE, List.of("292y 171d 23h 47' ", 16.855)) // supremum
            )
        .forEach(
            (input, value) -> {
              final String prefix = (String) value.get(0);
              final double seconds = (Double) value.get(1);
              final String expected = String.format(STAD_FORMAT, prefix, seconds);

              assertEquals(expected, AfiUtils.nanoSeconds2Time(input), expected);
            }); // end forEach((input, value) -> ...)
  } // end method */

  /** Test method for {@link AfiUtils#skOps(SelectionKey, boolean)}. */
  @Test
  void test_skOps__SelectionKey_boolean() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. OP_ACCEPT   and  attachment is null
    // --- b. OP_CONNECT  and  attachment is not a list
    // --- c. OP_READ     and  attachment is an empty list
    // --- d. OP_WRITE    and  attachment is a non-empty list
    // --- e. key invalid

    // --- a. OP_ACCEPT   and  attachment is null
    try (Selector selector = Selector.open();
        ServerSocketChannel socketChannel = ServerSocketChannel.open()) {
      socketChannel.configureBlocking(false);
      final SelectionKey key = socketChannel.register(selector, SelectionKey.OP_ACCEPT);
      assertEquals("null=v----", AfiUtils.skOps(key, true));
      assertEquals("null=vA---", AfiUtils.skOps(key, false));
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- b. OP_CONNECT  and  attachment is not a list
    try (Selector selector = Selector.open();
        SocketChannel socketChannel = SocketChannel.open()) {
      socketChannel.configureBlocking(false);
      final SelectionKey key = socketChannel.register(selector, SelectionKey.OP_CONNECT);
      assertEquals("null=v----", AfiUtils.skOps(key, true));
      assertEquals("null=v-C--", AfiUtils.skOps(key, false));
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    try {
      final Pipe pipe = Pipe.open();
      try (Selector selector = Selector.open();
          Pipe.SinkChannel sink = pipe.sink();
          Pipe.SourceChannel source = pipe.source()) {
        sink.configureBlocking(false);
        source.configureBlocking(false);

        final SelectionKey keySink = sink.register(selector, SelectionKey.OP_WRITE);
        final SelectionKey keySource = source.register(selector, SelectionKey.OP_READ);

        // --- c. OP_READ     and  attachment is an empty list
        keySource.attach(Collections.emptyList());
        assertEquals("[]=v----", AfiUtils.skOps(keySource, true));
        assertEquals("[]=v--R-", AfiUtils.skOps(keySource, false));

        // --- d. OP_WRITE    and  attachment is a non-empty list
        keySink.attach(List.of(15));
        assertEquals("15=v----", AfiUtils.skOps(keySink, true));
        assertEquals("15=v---W", AfiUtils.skOps(keySink, false));

        // --- e. key invalid
        source.close();
        IntStream.rangeClosed(0, 0x1f)
            .forEach(
                ops -> {
                  assertEquals("[]=i----", AfiUtils.skOps(keySource, true));
                  assertEquals("[]=i----", AfiUtils.skOps(keySource, false));
                }); // end forEach(ops -> ...)
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link AfiUtils#skInterestedOps(SelectionKey)}. */
  @Test
  void test_skInterestedOps__SelectionKey() {
    // Assertions:
    // ... a. AfiUtils.skOps(SelectionKey, boolean)-method works as expected

    // Note: Because of the assertion(s) we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. invalid key

    try {
      final Pipe pipe = Pipe.open();
      try (Selector selector = Selector.open();
          Pipe.SinkChannel sink = pipe.sink();
          Pipe.SourceChannel source = pipe.source()) {
        sink.configureBlocking(false);
        source.configureBlocking(false);

        final SelectionKey keySink = sink.register(selector, SelectionKey.OP_WRITE, "sink");
        final SelectionKey keySource = source.register(selector, SelectionKey.OP_READ, "source");

        // --- a. smoke test
        assertEquals("sink=v---W", AfiUtils.skInterestedOps(keySink));
        assertEquals("source=v--R-", AfiUtils.skInterestedOps(keySource));

        // --- b. invalid key
        sink.close();
        assertFalse(sink.isOpen());
        assertFalse(keySink.isValid());
        assertEquals("sink=i----", AfiUtils.skInterestedOps(keySink));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link AfiUtils#skInterestedOps(Collection)}. */
  @Test
  void test_skInterestedOps__Collection() {
    // Assertions:
    // ... a. AfiUtils.skInterestedOps(SelectionKey)-method works as expected

    // Note: Because of the assertion(s) we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    try {
      final Pipe pipe = Pipe.open();
      try (Selector selector = Selector.open();
          Pipe.SinkChannel sink = pipe.sink();
          Pipe.SourceChannel source = pipe.source()) {
        sink.configureBlocking(false);
        source.configureBlocking(false);

        final SelectionKey keySink = sink.register(selector, SelectionKey.OP_WRITE, "sink");
        final SelectionKey keySource = source.register(selector, SelectionKey.OP_READ, "source");

        // --- a. smoke test
        assertEquals(
            List.of("sink=v---W", "source=v--R-"),
            AfiUtils.skInterestedOps(Set.of(keySink, keySource)));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link AfiUtils#skReadyOps(SelectionKey)}. */
  @Test
  void test_skReadyOps__SelectionKey() {
    // Assertions:
    // ... a. AfiUtils.skOps(SelectionKey, boolean)-method works as expected

    // Note: Because of the assertion(s) we can be lazy here.

    // Test strategy:
    // --- a. smoke test before selector
    // --- b. selector.select(...)
    // --- c. smoke test after selector
    // --- d. invalid key

    try {
      final Pipe pipe = Pipe.open();
      try (Selector selector = Selector.open();
          Pipe.SinkChannel sink = pipe.sink();
          Pipe.SourceChannel source = pipe.source()) {
        sink.configureBlocking(false);
        source.configureBlocking(false);

        final SelectionKey keySink = sink.register(selector, SelectionKey.OP_WRITE, "sink");
        final SelectionKey keySource = source.register(selector, SelectionKey.OP_READ, "source");

        // --- a. smoke test before selector
        assertEquals("sink=v----", AfiUtils.skReadyOps(keySink));
        assertEquals("source=v----", AfiUtils.skReadyOps(keySource));

        // --- b. selector.select(...)
        // Note: It is expected, that the "selector.select(int)"-method returns
        //       immediately (whatever "immediately" is on the host where this
        //       tests run). On slow machines this may last several dozen
        //       milliseconds.
        final long expMaxRunTime = 50_000_000; // = 50_000 us = 50 ms = 0.05 s
        final long startTime = System.nanoTime();
        selector.select(1_000); // timeout = 1_000 ms = 1 s
        final long runTime = System.nanoTime() - startTime;
        assertTrue(runTime < expMaxRunTime, () -> AfiUtils.nanoSeconds2Time(runTime));

        // --- c. smoke test after selector
        assertEquals("sink=v---W", AfiUtils.skReadyOps(keySink));
        assertEquals("source=v----", AfiUtils.skReadyOps(keySource));

        // --- d. invalid key
        sink.close();
        assertFalse(sink.isOpen());
        assertFalse(keySink.isValid());
        assertEquals("sink=i----", AfiUtils.skReadyOps(keySink));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link AfiUtils#skReadyOps(Collection)}. */
  @Test
  void test_skReadyOps__Collection() {
    // Assertions:
    // ... a. AfiUtils.skReadyOps(SelectionKey)-method works as expected

    // Note: Because of the assertion(s) we can be lazy here.

    // Test strategy:
    // --- a. smoke test before selector
    // --- b. selector.select(...)
    // --- c. smoke test after selector
    // --- d. write a bit to the pipe

    try {
      final Pipe pipe = Pipe.open();
      try (Selector selector = Selector.open();
          Pipe.SinkChannel sink = pipe.sink();
          Pipe.SourceChannel source = pipe.source()) {
        sink.configureBlocking(false);
        source.configureBlocking(false);

        final SelectionKey keySink = sink.register(selector, SelectionKey.OP_WRITE, "sink");
        final SelectionKey keySource = source.register(selector, SelectionKey.OP_READ, "source");

        // --- a. smoke test before selector
        assertEquals(
            List.of("sink=v----", "source=v----"), AfiUtils.skReadyOps(Set.of(keySink, keySource)));

        // --- b. selector.select(...)
        long startTime = System.nanoTime();
        selector.select(1_000);
        long runTime = System.nanoTime() - startTime;
        assertTrue(runTime < 10_000_000, AfiUtils.nanoSeconds2Time(runTime));

        // --- c. smoke test after selector
        assertEquals(
            List.of("sink=v---W", "source=v----"), AfiUtils.skReadyOps(Set.of(keySink, keySource)));
        Set<SelectionKey> skSet = selector.selectedKeys();
        assertEquals(Set.of(keySink), skSet);
        skSet.clear();

        // --- d. write a bit to the pipe
        sink.write(ByteBuffer.wrap("Hello World!".getBytes(StandardCharsets.UTF_8)));
        startTime = System.nanoTime();
        selector.select(1_000);
        runTime = System.nanoTime() - startTime;
        assertTrue(runTime < 10_000_000, AfiUtils.nanoSeconds2Time(runTime));
        skSet = selector.selectedKeys();
        assertEquals(List.of("sink=v---W", "source=v--R-"), AfiUtils.skReadyOps(skSet));
        skSet.clear();
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
