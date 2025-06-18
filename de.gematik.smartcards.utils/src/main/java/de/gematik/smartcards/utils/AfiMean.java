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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Instances of this class can be used to compute several simple statistics for a set of numbers.
 *
 * <p>Numbers are entered into the data set using the {@link #enter(double[])} method. Methods are
 * provided to return the following statistics for the set of numbers that have been entered:
 *
 * <ol>
 *   <li>number of items entered so far via {@link #getCount()}
 *   <li>average via {@link #getMean()}
 *   <li>variance via {@link #getVariance()}
 *   <li>standard deviation via {@link #getStandardDeviation()}
 *   <li>smallest value ever entered into the set via {@link #getMinimum()}
 *   <li>biggest value ever entered into the set via {@link #getMaximum()}
 * </ol>
 *
 * <p>It is possible to enter any amount of numbers without causing {@link OutOfMemoryError},
 * because attributes (e.g. minimum, maximum, average) are updated each time another value is
 * entered.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public class AfiMean {

  /** Size of buffer. */
  @VisibleForTesting // otherwise: private
  /* package */ static final int BUFFER_SIZE = 8192; // */

  /** Message in exceptions for empty data set. */
  private static final String EMPTY_DATA_SET = "empty data set";

  /**
   * Number used to bias the entered value to gain better numerical results.
   *
   * <p>For more information see <a
   * href="https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance">Wikipedia</a>.
   */
  private double insBias; // NOPMD no accessors */

  /**
   * Buffer storing the first numbers entered.
   *
   * <p>The purpose of this buffer is to get a better value for {@link #insBias} once "some" numbers
   * have been entered.
   *
   * <p>As long as fewer numbers have been entered than the capacity of this buffer all calculations
   * are done on this buffer. When the amount of number exceeds the capacity of this buffer, it is
   * no longer used.
   */
  private double[] insBuffer; // NOPMD no accessors */

  /** Number of numbers that have been entered. */
  private int insCount; // NOPMD no accessors */

  /** The maximum of all numbers. */
  private double insMaximum = Double.NEGATIVE_INFINITY; // NOPMD no accessors */

  /** The minimum of all numbers. */
  private double insMinimum = Double.POSITIVE_INFINITY; // NOPMD no accessors */

  /**
   * The sum of the squares, all the items taken into account.
   *
   * <p>Used in {@link #getVariance()}.
   */
  private double insSquareSum; // NOPMD no accessors */

  /** The sum of all the items that have been entered. */
  private double insSum; // NOPMD no accessors */

  /**
   * Comfort Constructor for an array of numbers.
   *
   * @param data array with numbers to be entered
   */
  public AfiMean(final double... data) {
    super();
    insCount = data.length;

    if (insCount <= BUFFER_SIZE) {
      // ... all numbers fit into the buffer
      //     => fill the buffer
      insBuffer = Arrays.copyOfRange(data, 0, BUFFER_SIZE);
    } else {
      // ... numbers exceed buffer capacity
      //     => do not use the buffer and calculate instance attributes properly
      insBias = Arrays.stream(data).sum() / insCount;
      insMaximum = Arrays.stream(data).max().orElseThrow();
      insMinimum = Arrays.stream(data).min().orElseThrow();
      insSquareSum =
          Arrays.stream(data)
              .map(
                  d -> {
                    final double s = d - getBias();
                    return s * s;
                  })
              .sum();
    } // end else
  } // end method */

  /**
   * Checks attributes against given values and reports anomalies.
   *
   * @param count reports if {@link #getCount()} is less than this parameter
   * @param minimum reports if {@link #getMinimum()} is less than this parameter
   * @param maximum reports if {@link #getMaximum()} is greater than this parameter
   * @param minMean reports if {@link #getMean()} is less than this parameter
   * @param maxMean reports if {@link #getMean()} is greater than this parameter
   * @return check result, empty if nothing unusual is found
   * @throws IllegalStateException if number of items is zero
   */
  public String check(
      final int count,
      final double minimum,
      final double maximum,
      final double minMean,
      final double maxMean) {
    final int counter;
    final double min;
    final double max;
    final double mean;

    synchronized (this) {
      counter = getCount();
      min = getMinimum();
      max = getMaximum();
      mean = getMean();
    } // end synchronized

    final Collection<String> result = new ArrayList<>();
    if (counter < count) {
      result.add(String.format(Locale.US, "noEntries = %d < %d", counter, count));
    } // end fi

    if (min < minimum) {
      result.add(String.format(Locale.US, "minimum = %9.2e < %9.2e", min, minimum));
    } // end fi

    if (max > maximum) {
      result.add(String.format(Locale.US, "maximum = %9.2e > %9.2e", max, maximum));
    } // end fi

    if (mean < minMean) {
      result.add(String.format(Locale.US, "mean = %9.2e < %9.2e", mean, minMean));
    } // end fi

    if (mean > maxMean) {
      result.add(String.format(Locale.US, "mean = %9.2e > %9.2e", mean, maxMean));
    } // end fi

    return String.join(LINE_SEPARATOR, result);
  } // end method */

  /**
   * Adds number(s) to the data set.
   *
   * @param data number(s) being added to the data set
   */
  public final void enter(final double... data) {
    if (0 == data.length) {
      return;
    } // end fi

    final int count;
    final int length;

    synchronized (this) {
      count = getCount();
      length = data.length;

      if (count + data.length <= BUFFER_SIZE) {
        // ... after entering all data will fit into buffer
        //     => add new data to buffer
        System.arraycopy(data, 0, insBuffer, count, length);

        insCount += length;

        return;
      } // end fi

      if (count <= BUFFER_SIZE) {
        // ... after entering there is too much data for buffer
        //     => get rid of buffer and calculate instance attribute
        final double[] concatenation = Arrays.copyOfRange(insBuffer, 0, count + length);

        // Note: Buffer is no longer needed, free memory.
        insBuffer = new double[0];
        System.arraycopy(data, 0, concatenation, count, data.length);

        insCount += data.length;
        insBias = Arrays.stream(concatenation).sum() / insCount;
        insMaximum = Arrays.stream(concatenation).max().orElseThrow();
        insMinimum = Arrays.stream(concatenation).min().orElseThrow();
        insSquareSum =
            Arrays.stream(concatenation)
                .map(
                    d -> {
                      final double s = d - insBias;
                      return s * s;
                    })
                .sum();

        return;
      } // end fi
    } // end synchronized
    // ... before entering there too much data for buffer
    //     => update instance attributes

    final double max = Arrays.stream(data).max().orElseThrow();
    final double min = Arrays.stream(data).min().orElseThrow();
    final double sum = Arrays.stream(data).map(d -> d - insBias).sum();
    final double squareSum =
        Arrays.stream(data)
            .map(
                n -> {
                  final double d = n - insBias;
                  return d * d;
                })
            .sum();

    // --- synchronized update of instance attributes
    synchronized (this) {
      insCount += length;
      insMaximum = Math.max(insMaximum, max);
      insMinimum = Math.min(insMinimum, min);
      insSum += sum;
      insSquareSum += squareSum;
    } // end synchronized
  } // end method */

  /**
   * Getter.
   *
   * <p><b>WARNING:</b> If {@link #getCount()} is less or equal to {@link #BUFFER_SIZE} then zero is
   * returned.
   *
   * @return bias value
   */
  @VisibleForTesting // otherwise: private
  /* package */ final double getBias() {
    return insBias;
  } // end method */

  /**
   * Getter.
   *
   * <p><i><b>Note:</b> This value is always valid.</i>
   *
   * @return number of items that have been entered
   */
  public final synchronized int getCount() {
    return insCount;
  } // end method */

  /**
   * Return average of all the items that have been entered.
   *
   * <p><i><b>Note:</b> This value is always valid.</i>
   *
   * @return average
   * @throws IllegalStateException if number of items is zero
   */
  public final double getMean() {
    final double bias;
    final int count;
    final double sum;

    synchronized (this) {
      count = getCount();

      if (0 == count) {
        throw new IllegalStateException(EMPTY_DATA_SET);
      } else {
        bias = getBias();
        sum = getSum();
      } // end else
    } // end synchronized

    return sum / count + bias;
  } // end method */

  /**
   * Returns maximum of all items that have been entered.
   *
   * @return maximum
   * @throws IllegalStateException if number of items is zero
   */
  public final double getMaximum() {
    double max;

    synchronized (this) {
      final int count = getCount();

      if (0 == count) {
        throw new IllegalStateException(EMPTY_DATA_SET);
      } else if (count <= BUFFER_SIZE) {
        max = Double.NEGATIVE_INFINITY;
        for (int i = count; i-- > 0; ) { // NOPMD assignment in operand
          max = Math.max(max, insBuffer[i]);
        } // end For (...)
      } else {
        max = insMaximum;
      } // end else
    } // end synchronized

    return max;
  } // end method */

  /**
   * Returns minimum of all items that have been entered.
   *
   * @return minimum
   * @throws IllegalStateException if number of items is zero
   */
  public final double getMinimum() {
    double min;

    synchronized (this) {
      final int count = getCount();

      if (0 == count) {
        throw new IllegalStateException(EMPTY_DATA_SET);
      } else if (count <= BUFFER_SIZE) {
        min = Double.POSITIVE_INFINITY;
        for (int i = count; i-- > 0; ) { // NOPMD assignment in operand
          min = Math.min(min, insBuffer[i]);
        } // end For (...)
      } else {
        min = insMinimum;
      } // end else
    } // end synchronized

    return min;
  } // end method */

  /**
   * Getter.
   *
   * <p><i><b>Note:</b> This value is always valid.</i>
   *
   * @return biased sum
   * @throws IllegalStateException if number of items is zero
   */
  @VisibleForTesting // otherwise: private
  /* package */ final double getSum() {
    final int count;
    final double[] buffer;

    synchronized (this) {
      count = getCount();

      if (0 == count) {
        throw new IllegalStateException(EMPTY_DATA_SET);
      } else if (count <= BUFFER_SIZE) {
        buffer = insBuffer;
      } else {
        return insSum;
      } // end else
    } // end synchronized

    return Arrays.stream(Arrays.copyOfRange(buffer, 0, count)).sum();
  } // end method */

  /**
   * Getter.
   *
   * <p><i><b>Note:</b> This value is always valid.</i>
   *
   * @return something
   * @throws IllegalStateException if number of items is zero
   */
  @VisibleForTesting // otherwise: private
  /* package */ final double getSquareSum() {
    final int count;
    final double[] buffer;

    synchronized (this) {
      count = getCount();

      if (0 == count) {
        throw new IllegalStateException(EMPTY_DATA_SET);
      } else if (count <= BUFFER_SIZE) {
        buffer = insBuffer;
      } else {
        return insSquareSum;
      } // end else
    } // end synchronized

    return Arrays.stream(Arrays.copyOfRange(buffer, 0, count)).map(d -> d * d).sum();
  } // end method */

  /**
   * Return standard deviation of all the items that have been entered.
   *
   * @return square root of {@link #getVariance()}
   * @throws IllegalStateException if number of items is zero
   */
  public final double getStandardDeviation() {
    return Math.sqrt(getVariance());
  } // end method */

  /**
   * Return variance of all the items that have been entered.
   *
   * <p>Variance is defined as follows, see <a
   * href="https://en.wikipedia.org/wiki/Variance">Wikipedia</a>:<br>
   * Var(x) = E[(X-u)^2], with u = E[X] = mean value and X = list of values.
   *
   * @return variance
   * @throws IllegalStateException if number of items is zero
   */
  public final double getVariance() {
    // Note: according to Wikipedia it is:
    //       Var(X) = E[(X-u)^2] = E[X^2] - E[X]^2 = 1/n * sum(X^2) - mean^2

    final double sum;
    final double squareSum;
    final int count;

    synchronized (this) {
      count = getCount();
      sum = getSum();
      squareSum = getSquareSum();
    } // end synchronized

    // Note 1: The following calculation possible produces negative values
    //         or -0.0 because of rounding errors.
    final double result = (squareSum - sum * sum / count) / count;

    return (result < Double.MIN_VALUE) ? 0 : result; // avoid negative values
  } // end method */

  /**
   * Returns {@link String} representation.
   *
   * <p>Included in the returned result is:
   *
   * <ol>
   *   <li>number of entries entered so far
   *   <li>min value
   *   <li>mean value
   *   <li>max value
   *   <li>standard deviation
   * </ol>
   *
   * @return {@link String} representation
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (0 == getCount()) {
      // ... no numbers entered
      return "noEntries=    0  min= -           mean= -           max= -           s= -   ";
    } // end fi
    // ... count > 0

    final int count;
    final double min;
    final double mean;
    final double max;
    final double deviation;

    synchronized (this) {
      count = getCount();
      min = getMinimum();
      mean = getMean();
      max = getMaximum();
      deviation = getStandardDeviation();
    } // end synchronized

    return String.format(
        Locale.US,
        "noEntries=%5d  min=%10.3e   mean=%10.3e   max=%10.3e   s=%5.2e",
        count,
        min,
        mean,
        max,
        deviation);
  } // end method */
} // end class
