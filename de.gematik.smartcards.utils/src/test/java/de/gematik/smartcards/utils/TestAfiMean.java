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

import static de.gematik.smartcards.utils.AfiMean.BUFFER_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link AfiMean}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "DLS_DEAD_LOCAL_STORE"
//         Spotbugs message: This instruction assigns a value to a local variable,
//             but the value is not read or used in any subsequent instruction.
//         Rational: False positive.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "DLS_DEAD_LOCAL_STORE", // see note 1
}) // */
@SuppressWarnings({"PMD.MethodNamingConventions", "PMD.TooManyMethods", "checkstyle:methodname"})
final class TestAfiMean {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

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

  /** Test method for {@link AfiMean#AfiMean(double[])}. */
  @Test
  void test_AfiMean__doubleA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. empty array
    // --- b. array.length <= BUFFER_SIZE
    // --- c. array.length > BUFFER_SIZE
    // --- d. check that small values count

    // --- a. empty array
    {
      final AfiMean dut = new AfiMean();

      assertEquals(0.0, dut.getBias(), Double.MIN_VALUE);
      assertEquals(0, dut.getCount());
      assertThrows(IllegalStateException.class, dut::getMinimum);
      assertThrows(IllegalStateException.class, dut::getMean);
      assertThrows(IllegalStateException.class, dut::getMaximum);
      assertThrows(IllegalStateException.class, dut::getSum);
      assertThrows(IllegalStateException.class, dut::getSquareSum);
      assertThrows(IllegalStateException.class, dut::getStandardDeviation);
      assertThrows(IllegalStateException.class, dut::getVariance);
    } // end --- a.

    // --- b. array.length <= BUFFER_SIZE
    RNG.intsClosed(0, BUFFER_SIZE, 5).forEach(this::zzzAfiMeanDouble);

    // --- c. array.length > BUFFER_SIZE
    RNG.intsClosed(BUFFER_SIZE + 1, 2 * BUFFER_SIZE, 5).forEach(this::zzzAfiMeanDouble);

    // --- d. check that small values count
    {
      final double[] data = new double[2 * BUFFER_SIZE];
      Arrays.fill(data, 1e-17);
      data[0] = 1;
      double sum = 0;
      for (final double datum : data) {
        sum += datum;
      } // end For (i...)

      final AfiMean dut = new AfiMean(data);
      assertEquals(1.0, sum);
      assertEquals(1.0 / data.length + 1e-17, dut.getMean());
    } // end --- d.
  } // end method */

  /**
   * Check {@link AfiMean#AfiMean(double...)} for given number of data elements.
   *
   * @param noData amount of data
   */
  private void zzzAfiMeanDouble(final int noData) {
    final double[] data = new double[noData];
    double min = Double.POSITIVE_INFINITY;
    double max = Double.NEGATIVE_INFINITY;
    double sum = 0;

    for (int i = noData; i-- > 0; ) { // NOPMD assignment in operand
      final double item = RNG.nextDouble();
      data[i] = item;

      min = Math.min(min, item);
      max = Math.max(max, item);
      sum += item;
    } // end For (i...)

    final double mean = sum / noData;
    final double bias;
    final double squareSum;
    if (noData <= BUFFER_SIZE) {
      bias = 0;

      double sqSum = 0;
      for (int i = noData; i-- > 0; ) { // NOPMD assignment in operand
        final double d = data[i];
        sqSum += d * d;
      } // end For (i...)
      squareSum = sqSum;
    } else {
      bias = sum / noData;
      sum = 0;

      double sqSum = 0;
      for (int i = noData; i-- > 0; ) { // NOPMD assignment in operand
        final double d = data[i] - bias;
        sqSum += d * d;
      } // end For (i...)
      squareSum = sqSum;
    } // end fi
    final double variance = (squareSum - sum * sum / noData) / noData;
    final double deviation = Math.sqrt(variance);

    final AfiMean dut = new AfiMean(data);

    if (0 == noData) {
      assertEquals(bias, dut.getBias(), Double.MIN_VALUE);
      assertEquals(noData, dut.getCount());
      assertThrows(IllegalStateException.class, dut::getMinimum);
      assertThrows(IllegalStateException.class, dut::getMean);
      assertThrows(IllegalStateException.class, dut::getMaximum);
      assertThrows(IllegalStateException.class, dut::getSum);
      assertThrows(IllegalStateException.class, dut::getSquareSum);
      assertThrows(IllegalStateException.class, dut::getVariance);
      assertThrows(IllegalStateException.class, dut::getStandardDeviation);
    } else {
      // ... at least one data item
      final var delta = 2e-14;
      assertEquals(bias, dut.getBias(), delta);
      assertEquals(noData, dut.getCount());
      assertEquals(min, dut.getMinimum(), delta);
      assertEquals(mean, dut.getMean(), delta);
      assertEquals(max, dut.getMaximum(), delta);
      assertEquals(sum, dut.getSum(), 1e-10);
      assertEquals(0.0, 1.0 - squareSum / dut.getSquareSum(), delta);
      assertEquals(variance, dut.getVariance(), delta);
      assertEquals(deviation, dut.getStandardDeviation(), delta);
    } // end else
  } // end method */

  /** Test method for {@link AfiMean#check(int, double, double, double, double)}. */
  @Test
  void test_check__int_double4() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter work as expected

    // Test strategy:
    // --- a. smoke test with empty data
    // --- b. smoke test with some data in the data set

    // --- a. smoke test with empty data
    {
      final AfiMean dut = new AfiMean();

      assertEquals(0, dut.getCount());
      assertThrows(IllegalStateException.class, () -> dut.check(0, 1, 2, 3, 4));
    } // end --- a.

    // --- b. smoke test with some data in the data set
    // b.1 everything is ok
    // b.2 complaining count
    // b.3 complaining min
    // b.4 complaining max
    // b.5 complaining minMean
    // b.6 complaining maxMean
    // b.7 several errors
    {
      final AfiMean dut = new AfiMean(1, 2, 3);

      // b.1 everything is ok
      assertEquals("", dut.check(3, 0.9, 3.1, 1.9, 2.1));

      // b.2 complaining count
      assertEquals("noEntries = 3 < 4", dut.check(4, 0.9, 3.1, 1.9, 2.1));

      // b.3 complaining min
      assertEquals("minimum =  1.00e+00 <  1.10e+00", dut.check(3, 1.1, 3.1, 1.9, 2.1));

      // b.4 complaining max
      assertEquals("maximum =  3.00e+00 >  2.90e+00", dut.check(3, 0.9, 2.9, 1.9, 2.1));

      // b.5 complaining minMean
      assertEquals("mean =  2.00e+00 <  2.10e+00", dut.check(3, 0.9, 3.1, 2.1, 2.2));

      // b.6 complaining maxMean
      assertEquals("mean =  2.00e+00 >  1.90e+00", dut.check(3, 0.9, 3.1, 1.8, 1.9));

      // b.7 several errors
      assertEquals(
          String.format(
              "noEntries = 3 < 4%n"
                  + "minimum =  1.00e+00 <  1.10e+00%n"
                  + "maximum =  3.00e+00 >  2.90e+00%n"
                  + "mean =  2.00e+00 <  2.10e+00%n"
                  + "mean =  2.00e+00 >  1.90e+00"),
          dut.check(4, 1.1, 2.9, 2.1, 1.9));
    } // end --- b.
  } // end method */

  /** Test method for {@link AfiMean#enter(double...)}. */
  @Test
  void test_enter__doubleA() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter work as expected

    // Test strategy:
    // --- a. no data to empty data set
    // --- b. no data to non-empty data set
    // --- c. add data such that BUFFER_SIZE is not exceeded
    // --- d. add data such that BUFFER_SIZE is exceeded
    // --- e. add data items one after the other
    // --- f. add different chunks of data

    // --- a. no data to empty data set
    {
      final AfiMean dut = new AfiMean();

      dut.enter();

      assertEquals(0, dut.getCount());
    } // end --- a.

    // --- b. no data to non-empty data set
    {
      final AfiMean dut = new AfiMean(1, 2, 3);
      final int count = dut.getCount();

      dut.enter();

      assertEquals(count, dut.getCount());
    } // end --- b.

    // --- c. add data such that BUFFER_SIZE is not exceeded
    zzzEnterDoubleArrayC();

    // --- d. add data such that BUFFER_SIZE is exceeded
    zzzEnterDoubleArrayd();

    // --- e. add data items one after the other
    zzzEnterDoubleArrayE();

    // --- f. add different chunks of data
  } // end method */

  /**
   * Test method for {@link AfiMean#enter(double...)}.
   *
   * <p>Here the following aspects are tested:
   *
   * <pre>
   *   --- c. add data such that BUFFER_SIZE is not exceeded
   * </pre>
   */
  private void zzzEnterDoubleArrayC() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. loop over various amount of data already in data set
    // --- b. loop over various amount of data entered to data set

    RNG.intsClosed(1, BUFFER_SIZE, 10)
        .forEach(
            totalSize -> {
              final double bias = 2000 * (RNG.nextDouble() - 0.5);
              final double amplitude = 1000 * RNG.nextDouble();
              final double[] totalData = rnd(totalSize, bias, amplitude);
              final AfiMean reference = new AfiMean(totalData);

              RNG.intsClosed(0, totalSize, 5)
                  .forEach(
                      oldSize -> {
                        // --- a. loop over various amount of data already in data set
                        final double[] oldData = Arrays.copyOfRange(totalData, 0, oldSize);
                        // --- b. loop over various amount of data entered to data set
                        final double[] newData = Arrays.copyOfRange(totalData, oldSize, totalSize);
                        final AfiMean dut = new AfiMean(oldData);

                        dut.enter(newData);

                        assertEquals(0, dut.getBias());
                        assertEquals(reference.getCount(), dut.getCount());
                        assertEquals(reference.getMinimum(), dut.getMinimum());
                        assertEquals(reference.getMean(), dut.getMean());
                        assertEquals(reference.getMaximum(), dut.getMaximum());
                        assertEquals(reference.getSum(), dut.getSum());
                        assertEquals(reference.getSquareSum(), dut.getSquareSum());
                      }); // end forEach(oldSize -> ...)
            }); // end forEach(totalSize -> ...)
  } // end method */

  /**
   * Test method for {@link AfiMean#enter(double...)}.
   *
   * <p>Here the following aspects are tested:
   *
   * <pre>
   *   --- d. add data such that BUFFER_SIZE is exceeded
   * </pre>
   */
  private void zzzEnterDoubleArrayd() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. loop over various amount of data already in data set
    // --- b. loop over various amount of data entered to data set

    RNG.intsClosed(BUFFER_SIZE + 1, 2 * BUFFER_SIZE, 10)
        .forEach(
            totalSize -> {
              final double bias = 2000 * (RNG.nextDouble() - 0.5);
              final double amplitude = 1000 * RNG.nextDouble();
              final double[] totalData = rnd(totalSize, bias, amplitude);
              final AfiMean reference = new AfiMean(totalData);

              RNG.intsClosed(0, BUFFER_SIZE, 5)
                  .forEach(
                      oldSize -> {
                        // --- a. loop over various amount of data already in data set
                        final double[] oldData = Arrays.copyOfRange(totalData, 0, oldSize);
                        // --- b. loop over various amount of data entered to data set
                        final double[] newData = Arrays.copyOfRange(totalData, oldSize, totalSize);
                        final AfiMean dut = new AfiMean(oldData);

                        dut.enter(newData);

                        assertEquals(reference.getBias(), dut.getBias());
                        assertEquals(reference.getCount(), dut.getCount());
                        assertEquals(reference.getMinimum(), dut.getMinimum());
                        assertEquals(reference.getMean(), dut.getMean());
                        assertEquals(reference.getMaximum(), dut.getMaximum());
                        assertEquals(reference.getSum(), dut.getSum());
                        assertEquals(reference.getSquareSum(), dut.getSquareSum());
                      }); // end forEach(oldSize -> ...)
            }); // end forEach(totalSize -> ...)
  } // end method */

  /**
   * Test method for {@link AfiMean#enter(double...)}.
   *
   * <p>Here the following aspects are tested:
   *
   * <pre>
   *   --- e. add data items one after the other
   * </pre>
   */
  private void zzzEnterDoubleArrayE() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. observe instance attributes
    RNG.intsClosed(1, BUFFER_SIZE + 10, 10)
        .forEach(
            totalSize -> {
              final double offset = 2000 * (RNG.nextDouble() - 0.5);
              final double amplitude = 1000 * RNG.nextDouble();
              final double[] data = rnd(totalSize, offset, amplitude);

              final AfiMean dut = new AfiMean();
              assertEquals(0, dut.getCount());

              for (int i = 0; i < totalSize; ) {
                final double item = data[i++]; // NOPMD reassignment of loop control variable
                dut.enter(item);
                final double[] ref = Arrays.copyOfRange(data, 0, i);
                final AfiMean reference = new AfiMean(ref); // NOPMD new in loop

                assertEquals(reference.getCount(), dut.getCount());
                assertEquals(reference.getMinimum(), dut.getMinimum());
                assertEquals(reference.getMaximum(), dut.getMaximum());
                assertEquals(
                    reference.getMean(), dut.getMean(), Math.abs(reference.getMean() * 1e-9));
                assertEquals(
                    reference.getVariance(),
                    dut.getVariance(),
                    Math.abs(reference.getVariance() * 1e-9));

                if (i <= BUFFER_SIZE) {
                  // ... reference and dut calculate the following attributes based on the same data
                  assertEquals(reference.getBias(), dut.getBias());
                  assertEquals(reference.getSum(), dut.getSum());
                  assertEquals(reference.getSquareSum(), dut.getSquareSum());
                } // end fi
              } // end For (i...)
            }); // end forEach(totalSize -> ...)
  } // end method */

  /** Test method for {@link AfiMean#getBias()}. */
  @Test
  void test_getBias() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: More tests in test_enter__doubleA()-method.

    // Test strategy:
    // --- a. fewer data than BUFFER_SIZE
    // --- b. at least BUFFER_SIZE data

    final double fixedValue = 1.0;

    // --- a. fewer data than BUFFER_SIZE
    for (final var size : RNG.intsClosed(0, BUFFER_SIZE, 5).boxed().toList()) {
      final double[] data = new double[size]; // NOPMD new in loop
      Arrays.fill(data, fixedValue);
      final AfiMean dut = new AfiMean(data); // NOPMD new in loop

      final double present = dut.getBias();

      assertEquals(0, present);
    } // end For (size...)
    // end --- a.

    // --- b. at least BUFFER_SIZE data
    RNG.intsClosed(BUFFER_SIZE + 1, 2 * BUFFER_SIZE, 5)
        .forEach(
            size -> {
              final double[] data = new double[size];
              Arrays.fill(data, fixedValue);
              final AfiMean dut = new AfiMean(data);

              final double present = dut.getBias();

              assertEquals(fixedValue, present, () -> String.valueOf(size));
            }); // end forEach(size -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AfiMean#getCount()}. */
  @Test
  void test_getCount() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: More tests in test_enter__doubleA()-method.

    // Test strategy:
    // --- a. smoke test with various values for size
    for (final var size : RNG.intsClosed(0, 2 * BUFFER_SIZE, 5).boxed().toList()) {
      final double[] data = new double[size]; // NOPMD new in loop
      final AfiMean dut = new AfiMean(data); // NOPMD new in loop

      final int present = dut.getCount();

      assertEquals(size, present);
    } // end For (size...)
    // end --- a.
  } // end method */

  /** Test method for {@link AfiMean#getMean()}. */
  @Test
  void test_getMean() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. enter(double[])-method works as expected

    // Note: More tests in test_enter__doubleA()-method.

    // Test strategy:
    // --- a. smoke test with empty data
    // --- b. smoke test with some data item from constructor

    // --- a. smoke test with empty data
    {
      final AfiMean dut = new AfiMean();

      assertEquals(0, dut.getCount());
      assertThrows(IllegalStateException.class, dut::getMean);
    } // end --- a.

    // --- b. smoke test with some data item from constructor
    Map.ofEntries(
            Map.entry(1.0, new double[] {1}), // just one element
            Map.entry(2.0, new double[] {1, 3}),
            Map.entry(3.0, new double[] {1, 5, 3}),
            Map.entry(4.0, new double[] {1, 4, 3, 8}))
        .forEach(
            (expected, input) -> {
              final AfiMean dut = new AfiMean(input);

              final double present = dut.getMean();

              assertEquals(expected, present, 1e-14);
            }); // end forEach((expected, input) -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AfiMean#getMinimum()}. */
  @Test
  void test_getMaximum() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. enter(double[])-method works as expected

    // Note: More tests in test_enter__doubleA()-method.

    // Test strategy:
    // --- a. smoke test with empty data
    // --- b. smoke test with some data item from constructor
    // --- c. more values than fit in buffer

    // --- a. smoke test with empty data
    {
      final AfiMean dut = new AfiMean();

      assertEquals(0, dut.getCount());
      assertThrows(IllegalStateException.class, dut::getMaximum);
    } // end --- a.

    // --- b. smoke test with some data item from constructor
    Map.ofEntries(
            Map.entry(1.0, new double[] {1}), // just one element
            Map.entry(2.0, new double[] {2, 1}), // max at beginning
            Map.entry(3.0, new double[] {1, 2, 3}), // max at end
            Map.entry(4.0, new double[] {1, 4, 3, 2}) // max in between
            )
        .forEach(
            (expected, input) -> {
              final AfiMean dut = new AfiMean(input);

              final double present = dut.getMaximum();

              assertEquals(expected, present, 1e-14);
            }); // end forEach((expected, input) -> ...)
    // end --- b.

    // --- c. more values than fit in buffer
    {
      final double expected = 1.5; // DLS_DEAD_LOCAL_STORE
      final double[] data = rnd(2 * BUFFER_SIZE, 0, 1);
      data[RNG.nextIntClosed(0, data.length - 1)] = expected;
      final AfiMean dut = new AfiMean(data);

      final double present = dut.getMaximum();

      assertEquals(expected, present);
    } // end --- c.
  } // end method */

  /** Test method for {@link AfiMean#getMinimum()}. */
  @Test
  void test_getMinimum() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. enter(double[])-method works as expected

    // Note: More tests in test_enter__doubleA()-method.

    // Test strategy:
    // --- a. smoke test with empty data
    // --- b. smoke test with some data item from constructor
    // --- c. more values than fit in buffer

    // --- a. smoke test with empty data
    {
      final AfiMean dut = new AfiMean();

      assertEquals(0, dut.getCount());
      assertThrows(IllegalStateException.class, dut::getMinimum);
    } // end --- a.

    // --- b. smoke test with some data item from constructor
    Map.ofEntries(
            Map.entry(1.0, new double[] {1}), // just one element
            Map.entry(2.0, new double[] {2, 3}), // min at beginning
            Map.entry(3.0, new double[] {4, 5, 3}), // min at end
            Map.entry(4.0, new double[] {6, 5, 4, 8}) // min in between
            )
        .forEach(
            (expected, input) -> {
              final AfiMean dut = new AfiMean(input);

              final double present = dut.getMinimum();

              assertEquals(expected, present, 1e-14);
            }); // end forEach((expected, input) -> ...)
    // end --- b.

    // --- c. more values than fit in buffer
    {
      final double expected = -0.5; // DLS_DEAD_LOCAL_STORE
      final double[] data = rnd(2 * BUFFER_SIZE, 1, 1);
      data[RNG.nextIntClosed(0, data.length - 1)] = expected;
      final AfiMean dut = new AfiMean(data);

      final double present = dut.getMinimum();

      assertEquals(expected, present);
    } // end --- c.
  } // end method */

  /** Test method for {@link AfiMean#getSum()}. */
  @Test
  void test_getSum() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. enter(double[])-method works as expected
    // ... c. getCount()-method works as expected

    // Note: More tests in test_enter__doubleA()-method.

    // Test strategy:
    // --- a. smoke test with empty data
    // --- b. smoke test with some data item from constructor
    // --- c. more values than fit in buffer

    // --- a. smoke test with empty data
    {
      final AfiMean dut = new AfiMean();

      assertEquals(0, dut.getCount());
      assertThrows(IllegalStateException.class, dut::getSum);
    } // end --- a.

    // --- b. smoke test with some data item from constructor
    Map.ofEntries(
            Map.entry(1.0, new double[] {1}), // just one element
            Map.entry(3.0, new double[] {2, 1}),
            Map.entry(6.0, new double[] {1, 2, 3}),
            Map.entry(10.0, new double[] {1, 4, 3, 2}))
        .forEach(
            (expected, input) -> {
              final AfiMean dut = new AfiMean(input);

              final double present = dut.getSum();

              assertEquals(expected, present, 1e-14);
            }); // end forEach((expected, input) -> ...)
    // end --- b.

    // --- c. more values than fit in buffer
    {
      final double[] data = rnd(2 * BUFFER_SIZE, -1, 2);
      final AfiMean dut = new AfiMean(data);

      final double present = dut.getSum();

      assertEquals(0.0, present);
    } // end --- c.
  } // end method */

  /** Test method for {@link AfiMean#getSquareSum()}. */
  @Test
  void test_getSquareSum() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. enter(double[])-method works as expected
    // ... c. getCount()-method works as expected
    // ... d. getBias()-method works as expected

    // Note: More tests in test_enter__doubleA()-method.

    // Test strategy:
    // --- a. smoke test with empty data
    // --- b. smoke test with some data item from constructor
    // --- c. more values than fit in buffer

    // --- a. smoke test with empty data
    {
      final AfiMean dut = new AfiMean();

      assertEquals(0, dut.getCount());
      assertThrows(IllegalStateException.class, dut::getSquareSum);
    } // end --- a.

    // --- b. smoke test with some data item from constructor
    Map.ofEntries(
            Map.entry(1.0, new double[] {1}), // just one element
            Map.entry(5.0, new double[] {2, 1}),
            Map.entry(14.0, new double[] {1, 2, 3}),
            Map.entry(30.0, new double[] {1, 4, 3, 2}))
        .forEach(
            (expected, input) -> {
              final AfiMean dut = new AfiMean(input);

              final double present = dut.getSquareSum();

              assertEquals(expected, present, 1e-14);
            }); // end forEach((expected, input) -> ...)
    // end --- b.

    // --- c. more values than fit in buffer
    {
      final double[] data = rnd(2 * BUFFER_SIZE, 1, 2);
      final AfiMean dut = new AfiMean(data);
      final double bias = dut.getBias();
      double expected = 0;
      for (int i = data.length; i-- > 0; ) { // NOPMD assignment in operand
        final double d = data[i] - bias;
        expected += d * d;
      } // end For (i...)

      final double present = dut.getSquareSum();

      assertEquals(expected, present, 1e-9);
    } // end --- c.
  } // end method */

  /** Test method for {@link AfiMean#getStandardDeviation()}. */
  @Test
  void test_getStandardDeviation() {
    // Assertions:
    // ... a. getVariance()-method works as expected

    // Note: Because of the assertion a. this simple method does not need
    //       extensive testing. Thus, be we can be lazy here.

    // Test strategy:
    // --- a. smoke test with empty data
    // --- b. smoke test with some data item from constructor

    // --- a. smoke test with empty data
    {
      final AfiMean dut = new AfiMean();

      assertEquals(0, dut.getCount());
      assertThrows(IllegalStateException.class, dut::getStandardDeviation);
    } // end --- a.

    // --- b. smoke test with some data item from constructor
    Set.of(
            new double[] {1}, // just one element
            new double[] {1, 3},
            new double[] {1, 5, 3},
            new double[] {1, 4, 3, 8})
        .forEach(
            input -> {
              final AfiMean dut = new AfiMean(input);
              final double expected = Math.sqrt(dut.getVariance());

              final double present = dut.getStandardDeviation();

              assertEquals(expected, present, 1e-14);
            }); // end forEach((expected, input) -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AfiMean#getVariance()}. */
  @Test
  void test_getVariance() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getCount()-method works as expected
    // ... c. getSum()-method works as expected
    // ... d. getSquareSum()-method works as expected

    // Note: More tests in test_enter__doubleA()-method.

    // Test strategy:
    // --- a. smoke test with empty data
    // --- b. smoke test with some data item from constructor
    // --- c. one data item causing variance being lower than Double.MIN_VALUE

    // --- a. smoke test with empty data
    {
      final AfiMean dut = new AfiMean();

      assertEquals(0, dut.getCount());
      assertThrows(IllegalStateException.class, dut::getVariance);
    } // end --- a.

    // --- b. smoke test with some data item from constructor
    RNG.intsClosed(1, 10, 5)
        .forEach(
            size -> {
              final double[] data = new double[size];
              for (int i = size; i-- > 0; ) { // NOPMD assignment in operand
                data[i] = 3 * RNG.nextDouble() - 1;
              } // end For (i...)
              final AfiMean dut = new AfiMean(data);
              final double sum = dut.getSum();
              final double square = dut.getSquareSum();
              final double expected = (square - sum * sum / size) / size;

              final double present = dut.getVariance();

              assertEquals(expected, present, 1e-10);
            }); // end forEach(size -> ...)
    // end --- b.

    // --- c. one data item causing variance being lower than Double.MIN_VALUE
    {
      final double input = Double.longBitsToDouble(0x3ff99da841d0a9b8L);
      final AfiMean dut = new AfiMean(input);
      final double expected = 0;

      final double present = dut.getVariance();

      assertEquals(expected, present, Double.MIN_VALUE);
    } // end --- c.
  } // end method */

  /** Test method for {@link AfiMean#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. getters work as expected

    // Test strategy:
    // --- a. smoke test with empty data set
    // --- b. smoke test with some data item from constructor

    // --- a. smoke test with empty data set
    {
      final AfiMean dut = new AfiMean();

      assertEquals(
          "noEntries=    0  min= -           mean= -           max= -           s= -   ",
          dut.toString());
    } // end --- a.

    // --- b. smoke test with some data item from constructor
    {
      final AfiMean dut = new AfiMean(3, 2, 0, 4, 6);

      final String present = dut.toString();

      assertEquals(
          "noEntries=    5  min= 0.000e+00   mean= 3.000e+00   max= 6.000e+00   s=2.00e+00",
          present);
    } // end --- b.
  } // end method */

  /**
   * Creates an array of {@code double} values in range [bias - amplitude, bias + amplitude].
   *
   * @param size number of items in returned array
   * @param bias of generated random numbers
   * @param amplitude of generated random numbers
   * @return appropriate array of random numbers
   */
  private double[] rnd(final int size, final double bias, final double amplitude) {
    return IntStream.range(0, size)
        .mapToDouble(i -> bias + 2 * amplitude * (RNG.nextDouble() - 0.5))
        .toArray();
  } // end method */
} // end class
