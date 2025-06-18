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

import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * This class extends {@link Random} and provides additional methods.
 *
 * <p>The main purpose of the additional methods is to support JUnit tests with random test vectors.
 *
 * <p>The following Java classes provide random values:
 *
 * <ol>
 *   <li>{@link Math#random()}: That method returns a {@code double} from range {@code [0, 1[}. That
 *       is equivalent to {@link Random#nextDouble()}. Thus, there seem to be no benefit to use
 *       {@link Math#random()} as the base for additional methods here.
 *   <li>{@link Random}: That class has lots of methods. This class adds some useful methods. The
 *       main advantage (from the perspective of testing) is that it is possible to {@link
 *       Random#setSeed(long)} and use the same sequence of random numbers again.
 *   <li>{@link SecureRandom}: That is a subclass of {@link Random} providing cryptographically
 *       secure random numbers. But those are not really necessary for random test vectors in JUnit
 *       test.
 * </ol>
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li><i>This is a final class and superclasses have instance new AfiRattributes, but these are
 *       not directly available to users.</i>
 *   <li><i>This class has no instance attributes.</i>
 *   <li><i>Thus, this class is an entity-type.</i>
 *   <li><i>It follows that this class intentionally does not implement the following methods:
 *       {@link Object#clone() clone()}, {@link Object#equals(Object) equals()}, {@link
 *       Object#hashCode() hashCode()}.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class AfiRng extends Random {

  /** Automatically generated ID. */
  @Serial private static final long serialVersionUID = 658178239725367962L; // */

  /** String constant. */
  private static final String MIN_GT_MAX = "min > max"; // */

  /**
   * Function returning a set of integers.
   *
   * <p>The entries of the set are (mainly) calculated by {@link #nextIntClosed(int, int)}, see
   * there for more information about the range of returned values.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
   *       return value is never used again by this method.</i>
   * </ol>
   *
   * @param min is the infimum of all return-values
   * @param max is the supremum of all return-values
   * @param streamSize number of elements, if zero or negative the size of stream is zero
   * @return unsorted stream of randomly chosen integers
   *     <ul>
   *       <li>If {@code min == max} then the set just contains {{code {min}}}
   *       <li>If {@code min < max} then it is for element {@code i} in the set: {@code min <= i <=
   *           max}.
   *       <li>If {code min > max} then it is for element {@code i} in the set: {@code i <= min OR
   *           max <= i}.
   *       <li>If {@code numberOfEntries == 1} then the set contains just {{@code min}}.
   *       <li>If {@code numberOfEntries == 2} then the set contains {{@code min, max}}.
   *       <li>If {@code numberOfEntries == 3} then the set contains {{@code min, min++, max}}.
   *       <li>If {@code numberOfEntries == 4} then the set contains {{@code min, min++, max--,
   *           max}}.
   *       <li>If {@code numberOfEntries > 4} then the set contains {{@code min, min++, r1, r2, ...,
   *           rn, max--, max}} with {@code r?} being randomly chosen.
   *       <li>If {@code numberOfEntries > (max - min)} then all numbers from range {@code [min,
   *           max]} are requested and returned.
   *     </ul>
   */
  public IntStream intsClosed(final int min, final int max, final int streamSize) {
    final Collection<Integer> set = // initial capacity in range [0, 32_768]
        new LinkedHashSet<>(Math.min(32_768, Math.max(0, streamSize)), 1.0f);

    // --- check input parameter
    // Note: Intentionally input parameters are not checked. All
    //       values and all of their combinations are accepted.

    // --- check whether entries in set are expected
    if (streamSize > 0) {
      // ... at least one entry requested

      // --- calculate total span, i.e., total amount of possible values
      // Note: to avoid an overflow in the following calculations,
      //       these are performed in the long-range.
      final boolean minLeMax = (min <= max); // flag: min \le max
      final long span;
      if (minLeMax) {
        // ... min <= max: span = max - min + 1
        // calculate total span, i.e. total amount of possible values
        span = ((long) max) - min + 1;
      } else {
        // ... min > max: span = (MAX_VALUE - min + 1) + (max - MIN_VALUE + 1)
        span = ((long) Integer.MAX_VALUE) - min + 1 + max - Integer.MIN_VALUE + 1;
      } // end fi

      // --- fill result
      if (streamSize > (span >> 1)) {
        // ... more than half of available values are requested
        set.addAll(intsClosedMany(min, max, streamSize, minLeMax));
      } else {
        // ... less than half of possible values are requested
        //     => return "{min, max, min+1, max-1, random, ...}"
        intsClodesFew(min, max, streamSize, set);
      } // end else (streamSize > 50%)
    } // end fi (streamSize > 0)

    final IntStream.Builder result = IntStream.builder();
    set.forEach(result::accept);

    return result.build();
  } // end method */

  private void intsClodesFew(
      final int min, final int max, final int streamSize, final Collection<Integer> set) {
    switch (streamSize) {
      case 1 -> set.add(min);
      case 2 -> {
        set.add(min);
        set.add(max);
      }
      case 3 -> {
        set.add(min);
        set.add(max);
        set.add(min + 1);
      }
      case 4 -> {
        set.add(min);
        set.add(max);
        set.add(min + 1);
        set.add(max - 1);
      }
      default -> {
        // ... more than 4 entries requested

        // fill with "border" elements
        set.add(min);
        set.add(max);
        set.add(min + 1);
        set.add(max - 1);
        final int inf = min + 2;
        final int sup = max - 2;
        while (set.size() < streamSize) {
          set.add(nextIntClosed(inf, sup));
        } // end While (not enough entries generated)
      } // end default
    } // end Switch (streamSize)
  } // end method */

  private List<Integer> intsClosedMany(
      final int min, final int max, final int streamSize, final boolean minLeMax) {
    // Strategy is: Fill a list with all possible values and remove some elements from
    // that list. This gives better performance, because no value is generated twice.
    final List<Integer> res = new ArrayList<>();

    // --- fill result with all requested values
    if (minLeMax) {
      // ... min < max
      IntStream.rangeClosed(min, max).forEach(res::add);
    } else {
      // ... min > max
      // add all values from [min, Integer.MAX_VALUE]
      IntStream.rangeClosed(min, Integer.MAX_VALUE).forEach(res::add);

      // add all values from [Integer.MIN_VALUE, max]
      IntStream.rangeClosed(Integer.MIN_VALUE, max).forEach(res::add);
    } // end fi

    // --- remove extra values
    int supremum = res.size() - 3; // don't touch penultimate and last element in result
    while (res.size() > streamSize) {
      res.remove(nextIntClosed(2, supremum--));
    } // end While (too many elements in result)
    return res;
  } // end method */

  /**
   * Returns a byte-array of given length and randomly chosen contents.
   *
   * <p>Internally this method uses {@link #nextBytes(byte[])}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is never used again by this method.</i>
   * </ol>
   *
   * @param length of the {@code byte[]} to be returned
   * @return an array of random octet
   * @throws NegativeArraySizeException if {@code length} is negative
   */
  public byte[] nextBytes(final int length) {
    final byte[] result = new byte[length];
    nextBytes(result);

    return result;
  } // end method */

  /**
   * Returns a byte-array of randomly chosen length and randomly chosen contents.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally the length of the returned array is NOT evenly distributed over the
   *       range {@code [min, max]} but biased to {@code min}.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is never used again by this method.</i>
   * </ol>
   *
   * @param min infimum length of byte-array (inclusive)
   * @param max supremum length of byte-array (inclusive)
   * @return an array of random octet with {@code min <= length <= max}
   * @throws IllegalArgumentException if {@code min > max}
   * @throws NegativeArraySizeException if {code numberOfBytes} is negative
   */
  public byte[] nextBytes(final int min, final int max) {
    if (min > max) {
      throw new IllegalArgumentException(MIN_GT_MAX);
    } // end fi
    // ... min <= max

    final double rnd = nextDouble();
    final int len = (int) (rnd * rnd * (max - min + 1)) + min;

    return nextBytes(len);
  } // end method */

  /**
   * Function returns a random integer.
   *
   * <p>The returned value is from the following range:
   *
   * <ol>
   *   <li>If {@code min == max} then that integer is returned.
   *   <li>If {@code min < max} then an integer from range {@code [min, max]} is returned.
   *   <li>If {@code min > max} then an integer NOT in range {@code ]max, min[} is returned, i.e. an
   *       integer either from range {@code [Integer.MIN_VALUE , max[} or from range {@code ]min,
   *       Integer.MAX_VALUE]}
   * </ol>
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>There are two main differences to the behavior of similar functions from class {@link
   *       Random} and alike:</i>
   *       <ul>
   *         <li><i>Intentionally the case {@code min >= max} does NOT throw an {@link
   *             IllegalArgumentException}.</i>
   *         <li><i>Intentionally {@code max} is inclusive.</i>
   *       </ul>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
   *       return value is primitive.</i>
   * </ol>
   *
   * @param min left side border of range
   * @param max right side border of range
   * @return random integer from appropriate range
   */
  public int nextIntClosed(final int min, final int max) {
    final int result;
    final int delta = max - min + 1;
    if (0 == delta) {
      // ... min == max + 1
      //     => full 32 bit entropy
      // Note: The situation [(min==Integer.MIN_VALUE) AND (max==Integer.MAX_VAlUE)]
      //       is just a special case of min == max + 1.
      result = nextInt();
    } else if (delta > 0) {
      // ... delta > 0, i.e. 31 bit entropy or less
      // Note: In case min > max it is possible (and intended) that an overflow might occur.
      result = min + nextInt(delta);
    } else {
      // ... delta < 0, i.e. 32 bit entropy
      // Note: It follows that with a probability of 50% or more the next random number is okay.
      final boolean minLeMax = (min <= max);
      final long span;
      if (minLeMax) {
        // ... min <= max: span = max - min + 1
        // calculate total span, i.e. total amount of possible values
        span = ((long) max) - min + 1;
      } else {
        // ... min > max: span = (MAX_VALUE - min + 1) + (max - MIN_VALUE + 1)
        span = ((long) Integer.MAX_VALUE) - min + 1 + max - Integer.MIN_VALUE + 1;
      } // end fi
      long rnd;
      do {
        rnd = nextInt() & 0xffffffffL; // kind of conversion to unsigned int32
      } while (rnd >= span);
      result = (int) (min + rnd);
    } // end fi (delta?)

    return result;
  } // end method */

  /**
   * Creates a printable character.
   *
   * <p>Valid characters are specified in <a
   * href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause 41.4,
   * table 10. Thus, the following characters are considered:
   *
   * <pre>
   * ABCDEFGHIJKLMNOPQRSTUVWXYZ  Latin capital letters
   * abcdefghijklmnopqrstuvwxyz  Latin small letters
   * 0123456789                  dezimal digits
   * ' '  SPACE
   * '''  APOSTROPHE
   * '('  LEFT PARENTHESIS
   * ')'  RIGHT PARENTHESIS
   * '+'  PLUS SIGN
   * ','  COMMA
   * '-'  HYPHEN-MINUS
   * '.'  FULL-STOP
   * '/'  SOLIDUS
   * ':'  COLON
   * '='  EQUALS SIGN
   * '?'  QUESTION MARK
   * </pre>
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return printable character
   */
  public char nextPrintable() {
    final String validCharacters =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" // Latin capital letters
            + "abcdefghijklmnopqrstuvwxyz" // Latin small letters
            + "0123456789" // dezimal digits
            + " '()+,-./:=?"; // other
    final int supremumIndex = validCharacters.length() - 1;

    return validCharacters.charAt(nextIntClosed(0, supremumIndex));
  } // end method */

  /**
   * Returns a {@link String} of given length and randomly chosen contents.
   *
   * <p>The sequence of characters in the returned {@link String} are printable characters.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param length of the {@code String} to be returned
   * @return {@link String} with printable characters
   * @throws NegativeArraySizeException if chosen {@code length} is negative
   */
  public String nextPrintable(final int length) {
    final char[] result = new char[length];

    for (int i = length; i-- > 0; ) { // NOPMD assignment in operand
      result[i] = nextPrintable();
    } // end For (i...)

    return new String(result); // NOPMD String instantiation
  } // end method */

  /**
   * Returns a {@link String} of randomly chosen length and randomly chosen contents.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally the length of the returned array is NOT evenly distributed over the
   *       range {@code [min, max]} but biased to {@code min}.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param min infimum length of {@link String} (inclusive)
   * @param max supremum length of {@link String} (inclusive)
   * @return {@link String} with printable characters
   * @throws IllegalArgumentException if {@code min > max}
   * @throws NegativeArraySizeException if chosen {@code length} is negative
   */
  public String nextPrintable(final int min, final int max) {
    if (min > max) {
      throw new IllegalArgumentException(MIN_GT_MAX);
    } // end fi
    // ... min <= max

    final double rnd = nextDouble();
    final int length = (int) (rnd * rnd * (max - min + 1)) + min;

    return nextPrintable(length);
  } // end method */

  /**
   * Creates <a href="https://tools.ietf.org/html/rfc3629">UTF-8</a> code points.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return UTF-8 {@code codePoint}
   */
  public int nextCodePoint() {
    // Note 1: According to RFC 3629 code points for UTF-8 are in range
    //         [U+0000, U+10FFFF] except the range [U+D800, U+DFFF], see
    //         https://datatracker.ietf.org/doc/html/rfc3629#section-3.
    final int delta = 0xdfff - 0xd800 + 1;
    final int maxValue = 0x10_ffff - delta;

    // evenly distributed on range [0, 1[
    final double rnd = nextDouble();

    // stretch rnd to maxValue, but favor low values due to rnd^4
    final int intermediate = (int) Math.round(Math.pow(rnd, 4) * maxValue);

    return intermediate + ((intermediate < 0xd800) ? 0 : delta);
  } // end method */

  /**
   * Returns a {@link String} of given length and randomly chosen contents.
   *
   * <p>The sequence of characters in the returned {@link String} are <a
   * href="https://tools.ietf.org/html/rfc3629">UTF-8</a> code points.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param length of the {@code String} to be returned
   * @return {@link String} with characters from {@link StandardCharsets#UTF_8}
   * @throws NegativeArraySizeException if chosen {@code length} is negative
   */
  public String nextUtf8(final int length) {
    final int[] result = new int[length];

    for (int i = length; i-- > 0; ) { // NOPMD assignment in operand
      result[i] = nextCodePoint();
    } // end For (i...)

    return new String(result, 0, length);
  } // end method */

  /**
   * Returns a {@link String} of randomly chosen length and randomly chosen contents.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally the length of the returned array is NOT evenly distributed over the
   *       range {@code [min, max]} but biased to {@code min}.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param min infimum length of {@link String} (inclusive)
   * @param max supremum length of {@link String} (inclusive)
   * @return {@link String} with characters from {@link StandardCharsets#UTF_8}
   * @throws IllegalArgumentException if {@code min > max}
   * @throws NegativeArraySizeException if chosen {@code length} is negative
   */
  public String nextUtf8(final int min, final int max) {
    if (min > max) {
      throw new IllegalArgumentException(MIN_GT_MAX);
    } // end fi
    // ... min <= max

    final double rnd = nextDouble();
    final int len = (int) (rnd * rnd * (max - min + 1)) + min;

    return nextUtf8(len);
  } // end method */
} // end class
