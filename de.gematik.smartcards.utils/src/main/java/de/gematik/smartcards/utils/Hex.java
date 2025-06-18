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

import java.util.stream.IntStream;

/**
 * This class contains static methods useful when handling octet strings.
 *
 * <p><b>Notes:</b>
 *
 * <ol>
 *   <li>This is a final class and {@link java.lang.Object Object} is the direct superclass.
 *   <li>This class has no instance attributes and no usable constructor.
 *   <li>Thus, this class is an entity-type.
 *   <li>It follows that this class intentionally does not implement the following methods: {@link
 *       java.lang.Object#clone() clone()}, {@link java.lang.Object#equals(Object) equals()}, {@link
 *       java.lang.Object#hashCode() hashCode()}.
 *   <li>Some methods are thread-safe others are not.
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.ShortClassName"})
public final class Hex {

  /** Private default constructor prevents instantiation of this class. */
  private Hex() {
    super();
    // intentionally empty
  } // end constructor */

  /**
   * Hex-digits are extracted from given String.
   *
   * <p>Only characters from sets 0-9, a-f and A-F are taken into account.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter and return value are
   *       immutable.</i>
   * </ol>
   *
   * @param input from which hex-digits are extracted
   * @return {@link String} containing 0-9, a-f only (lower case), possibly empty
   */
  public static String extractHexDigits(final CharSequence input) {
    final int[] extractedCodePoints =
        input
            .codePoints()
            .parallel() // for performance boost
            .filter(
                (final int i) ->
                    (('0' <= i) && (i <= '9')) // decimal digits
                        || (('a' <= i) && (i <= 'f')) // lower case hex-digits
                        || (('A' <= i) && (i <= 'F')) // upper case hex-digits
                )
            .map((final int i) -> ('A' <= i) ? (i | 0x20) : i) // to lower case
            .toArray();

    return new String(extractedCodePoints, 0, extractedCodePoints.length);
  } // end method */

  /**
   * This function converts a string of hex-digits to a byte-array.
   *
   * <p>The following algorithm is used
   *
   * <ul>
   *   <li>First all non hex-digits are removed by using {@link #extractHexDigits}.
   *   <li>The first character will become the high-nibble of the first byte
   *   <li>The second character will become the low-nibble of the first byte
   *   <li>The third character will become the high-nibble of the second byte
   *   <li>...
   * </ul>
   *
   * <p><b>Notes:</b>
   *
   * <ol>
   *   <li>This method is thread-safe.
   *   <li>Object sharing is not a problem here, because the input parameter is immutable and the
   *       return value is never used again within this class.
   * </ol>
   *
   * @param input to be converted
   * @return corresponding byte-array
   * @throws IllegalArgumentException if number of hex-digits in {@code input} is odd
   */
  public static byte[] toByteArray(final CharSequence input) {
    final char[] hexDigits = extractHexDigits(input).toCharArray();
    final byte[] result = new byte[hexDigits.length >> 1];

    if (hexDigits.length != (result.length << 1)) {
      throw new IllegalArgumentException("Number of hex-digits in <" + input + "> is odd");
    } // end fi
    // ... number of hex-digits in hexDigits is even
    IntStream.range(0, result.length)
        .parallel() // for performance boost
        .forEach(
            i -> {
              int pos = i << 1;
              int highNibble = hexDigits[pos++];
              int lowNibble = hexDigits[pos];
              highNibble -= (highNibble > '9') ? 87 : 48;
              lowNibble -= (lowNibble > '9') ? 87 : 48;
              result[i] = (byte) ((highNibble << 4) | lowNibble);
            }); // end forEach(i -> ...)

    return result;
  } // end method */

  /**
   * This function takes a byte-array and converts it to a string of hex-digits.
   *
   * <p>Each byte in the array is converted to two characters containing the high- and low nibble of
   * the byte.
   *
   * <p><b>Notes:</b>
   *
   * <ol>
   *   <li>This method is NOT thread-safe, because the input array might change after calling this
   *       method.
   *   <li>Object sharing is not a problem here, because the input parameter is only read and
   *       otherwise not used and the return value is immutable.
   * </ol>
   *
   * @param input byte-array to be converted
   * @return {@link String} containing lower-case hex-digits (i.e.: 0..9, a..f)
   */
  public static String toHexDigits(final byte[] input) {
    return toHexDigits(input, 0, input.length);
  } // end method */

  /**
   * This function takes (part of) a byte-array and converts it to a string of hex-digits.
   *
   * <p>Each relevant byte in the array is converted to two characters containing the high- and low
   * nibble of the byte.
   *
   * <p><b>Notes:</b>
   *
   * <ol>
   *   <li>This method is NOT thread-safe, because the input array might change after calling this
   *       method.
   *   <li>Object sharing is not a problem here, because the input parameters are only read or
   *       primitive and otherwise not used and the return value is immutable.
   * </ol>
   *
   * @param input byte-array to be converted
   * @param offset index of first byte in array taken into account
   * @param length number of bytes from array to be taken into account
   * @return {@link String} containing lower-case hex-digits (i.e.: 0..9, a..f)
   * @throws ArrayIndexOutOfBoundsException if offset and/or length are such that elements outside
   *     input-array are addressed
   */
  public static String toHexDigits(final byte[] input, final int offset, final int length) {
    final char[] result = new char[length << 1];

    IntStream.range(offset, offset + length)
        .parallel() // for performance boost
        .forEach(
            i -> {
              final int byteValue = input[i];
              final int high = (byteValue >> 4) & 0xf; // high-nibble
              final int low = byteValue & 0xf; // low-nibble
              int pos = (i - offset) << 1;
              result[pos++] = (char) (high + ((high > 9) ? 87 : 48)); // NOPMD avoid reassigning
              result[pos] = (char) (low + ((low > 9) ? 87 : 48));
            }); // end forEach(i -> ...)

    return new String(result); // NOPMD String instantiation
  } // end method */
} // end class
