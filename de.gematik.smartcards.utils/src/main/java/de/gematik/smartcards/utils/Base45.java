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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class provides functionality to encode and decode octet strings according to <a
 * href="https://github.com/patrikhson/base45">base 45</a>.
 */
public final class Base45 {

  /** Alphabet used for encoding and decoding. */
  public static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:"; // */

  /**
   * Characters used for encoding octet strings.
   *
   * <p>The characters here and their order is identical to <a
   * href="https://en.wikipedia.org/wiki/QR_code#Storage">QR-code alphanumeric mode</a>. for more
   * information, see also <a
   * href="https://www.thonky.com/qr-code-tutorial/alphanumeric-mode-encoding">tutorial</a>.
   *
   * <p>The following table shows the {@link #ALPHABET} and its mapping to the corresponding integer
   * values:
   *
   * <pre>
   * 0 0
   * 1 1
   * 2 2
   * 3 3
   * 4 4
   * 5 5
   * 6 6
   * 7 7
   * 8 8
   * 9 9
   * A 10
   * B 11
   * C 12
   * D 13
   * E 14
   * F 15
   * G 16
   * H 17
   * I 18
   * J 19
   * K 20
   * L 21
   * M 22
   * N 23
   * O 24
   * P 25
   * Q 26
   * R 27
   * S 28
   * T 29
   * U 30
   * V 31
   * W 32
   * X 33
   * Y 34
   * Z 35
   *   36 (space)
   * $ 37
   * % 38
   * * 39
   * + 40
   * - 41
   * . 42
   * / 43
   * : 44
   * </pre>
   */
  @VisibleForTesting /* package */ static final char[] CHARS = ALPHABET.toCharArray(); // */

  /** Number of elements in the character set. */
  @VisibleForTesting /* package */ static final char MODUL = (char) CHARS.length; // */

  /** Default constructor. */
  private Base45() {
    super();
    // intentionally empty
  } // end method */

  /**
   * Decode given characters according to <a href="https://github.com/patrikhson/base45">base
   * 45</a>.
   *
   * <p>This is the inverse function of {@link #encode(byte[])}.
   *
   * @param chars characters to be decoded
   * @return octet string
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>number of characters in input {@code chars} is neither {@code 3*i} nor {@code 3*i +
   *           2} with {@code i} as a non negative integer
   *       <li>any of the input characters are not in {@link #ALPHABET}
   *       <li>a decoded value of a character-triple is not in range {@code [0, 65535] = ['0000',
   *           'FFFF']}
   *     </ol>
   */
  public static byte[] decode(final CharSequence chars) {
    final ByteArrayOutputStream result = new ByteArrayOutputStream();

    char charC;
    char charD;
    char charE;
    final int endIndex = chars.length();
    for (int index = 0; index < endIndex; index += 3) {
      // --- try to read another character, which is the first of a new triple
      charC = chars.charAt(index);

      // --- try to read another character, which would be the second of a new triple
      // Note 1: This will fail in case the number of characters is wrong.
      try {
        charD = chars.charAt(index + 1);
      } catch (final IndexOutOfBoundsException e) {
        // ... number of characters in input is neither 3*i nor (3*i + 2)
        //     => number of characters in input is wrong, throw appropriate exception
        throw new IllegalArgumentException("invalid number of input character"); // NOPMD stack lost
      } // end Catch (...)

      // --- try to read another character, which would be the third of a new triple
      // Note 2: This will fail in case the number of octets in output is odd.
      try {
        charE = chars.charAt(index + 2);
      } catch (final IndexOutOfBoundsException e) {
        // ... number of characters in input is (3*i + 2)
        //     => odd number of octets in output
        final int decoded = decode(charC, charD, '0');
        result.write(decoded);

        break;
      } // end Catch (...)
      // ... three additional characters read from input char-sequence
      //     => convert those characters and add them to the output
      final int decoded = decode(charC, charD, charE);
      result.write(decoded >> 8);
      result.write(decoded);
    } // end For (index...)

    return result.toByteArray();
  } // end method */

  /**
   * Decode given characters into octets.
   *
   * <p>This is kind of the inverse function of {@link #encode(int[], char)}.
   *
   * @param charC first character
   * @param charD second character
   * @param charE third character
   * @return corresponding decoded value
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>any of the input characters are not in {@link #ALPHABET}
   *       <li>the decoded value is not in range {@code [0, 65535] = ['0000', 'FFFF']}
   *     </ol>
   */
  /* package */
  static int decode(final char charC, final char charD, final char charE) {
    final int intC = ALPHABET.indexOf(charC);
    final int intD = ALPHABET.indexOf(charD);
    final int intE = ALPHABET.indexOf(charE);

    if ((-1 == intC) || (-1 == intD) || (-1 == intE)) { // NOPMD literal in if statement
      throw new IllegalArgumentException("invalid character(s)");
    } // end fi

    final int result = intC + MODUL * (intD + MODUL * intE);

    if (result > 0xffff) { // NOPMD literal in if statement
      throw new IllegalArgumentException("invalid code");
    } // end fi

    return result;
  } // end method */

  /**
   * Encode given octet string according to <a href="https://github.com/patrikhson/base45">base
   * 45</a>.
   *
   * <p>This is the inverse function of {@link #decode(CharSequence)}.
   *
   * @param octets to be encoded
   * @return <a href="https://github.com/patrikhson/base45">base 45</a> encoded octets
   */
  public static String encode(final byte[] octets) {
    // Note 1: This implementation converts the input to a buffer and reads from that buffer.
    // Note 2: Typically the input "octets" contain many bytes. Thus, the underlying method
    //         encode(char, ...) will be called many times. Instead of creating a new int[]
    //         each time that method is called, such an array is allocated once and then
    //         passed into that method as an additional parameter.
    // Note 3: Intentionally an "endless-loop" is used hereafter. Thus, this implementation
    //         relies on the JRE and its ability to check for BufferUnderflow. Such a check
    //         is there anyway, so there seems to be no need to do an extra check here.
    final StringBuilder result = new StringBuilder();
    final int[] cde = new int[3];
    final ByteBuffer byteBuffer = ByteBuffer.wrap(octets);

    while (byteBuffer.remaining() > 1) {
      encode(cde, byteBuffer.getChar());
      result.append(CHARS[cde[0]]).append(CHARS[cde[1]]).append(CHARS[cde[2]]);
    } // end While (enough data in byteBuffer)

    if (byteBuffer.hasRemaining()) {
      // ... number of octet is odd
      //     => take care of the last octet
      encode(cde, (char) (octets[octets.length - 1] & 0xff));
      result.append(CHARS[cde[0]]).append(CHARS[cde[1]]);
    } // end fi

    return result.toString();
  } // end method */

  /**
   * Encode two bytes.
   *
   * <p>This is the inverse function of {@link #decode(char, char, char)}.
   *
   * @param word containing two bytes to be encoded
   * @param result base45 encoded {@link String} of parameter {@code word}
   */
  @VisibleForTesting
  /* package */ static void encode(final int[] result, char word) {
    result[0] = word % MODUL;
    word /= MODUL; // NOPMD reassigned parameter

    result[1] = word % MODUL;
    word /= MODUL;

    result[2] = word;
  } // end method */
} // end class
