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

import de.gematik.smartcards.utils.Hex;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Class representing a TLV object of universal class with tag-number 20, i.e. TeletextString-type.
 *
 * <p>For more information about the encoding, see
 *
 * <ul>
 *   <li><a href="https://www.itu.int/rec/T-REC-T.61">T.61:1988</a>,
 *   <li><a href="https://en.wikipedia.org/wiki/ITU_T.61">Wikipedia ITU-T.61</a>
 * </ul>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class DerTeletexString extends DerRestrictedCharacterStringTypes {

  /**
   * Integer representation of tag for PrintableString.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause
   *       41, table 8
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690, T61String</a>
   * </ol>
   */
  public static final int TAG = 20; // */

  /**
   * Translation from T.61 code points to Unicode code points.
   *
   * <p>The following array contains other code points.
   *
   * <p>The first element is the T.61 code point. The second element (if present) gives the
   * corresponding Unicode code point. If the second element is absent then the Unicode code point
   * is identical to the T.61 code point.
   *
   * <p>For more information, see
   *
   * <ul>
   *   <li><a href="https://en.wikipedia.org/wiki/ITU_T.61">Wikipedia ITU-T.61</a>
   * </ul>
   */
  /* package */ static final int[][] TRANSLATION = {
    {0x08}, // BS = BackSpace
    {0x0a}, // LF = LineFeed
    {0x0c}, // FF = FormeFeed, page break
    {0x0d}, // CR = CarriageReturn
    {0x0e}, // SO = ShiftOut, see https://en.wikipedia.org/wiki/Shift_Out_and_Shift_In_characters
    {0x0f}, // SI = ShiftIn,  see https://en.wikipedia.org/wiki/Shift_Out_and_Shift_In_characters

    // 0x19, // SS2, intentionally commented out
    {0x1a}, // SUB = substitute character
    // 0x1b, // ESC = escape, intentionally commented out
    // 0x1d, // SS3, intentionally commented out

    {0x20}, // SP = space
    {0x21}, // ! = exclamation mark
    {0x22}, // " = quotation mark

    // Note 1: Code points for 0x3? and 0x4? are added by static block

    // Note 2: Other 0x5? code points are added by static block
    {0x5d}, // ] = right square bracket
    {0x5f}, // _ = low line

    // Note 3: Code points for 0x6? are added by static block

    // Note 4: Other 0x7? code points are added by static block
    {0x7c}, // | = vertical line
    // 0x7f, // DEL, intentionally commented out

    // 0x8b, // PLD, intentionally commented out
    // 0x8c, // PLU, intentionally commented out

    // 0x9b, // CSI, intentionally commented out

    {0xa0}, // NBSP = non breakable space
    {0xa1}, // inverted exclamation mark
    {0xa2}, // cent sign
    {0xa3}, // pound sign
    {0xa4, 0x0024}, // $ = dollar sign
    {0xa5}, // yen sign
    {0xa6, 0x0023}, // # = number sign
    {0xa7}, // § = section sign
    {0xa8, 0xc2a4}, // currency sign
    {0xab}, // left pointed double angle quotation mark
    {0xb0}, // ° = degree sign
    {0xb1}, // plus-minus sign
    {0xb2}, // ² = superscript 2
    {0xb3}, // ³ = superscript 3
    {0xb4, 0xc2d7}, // multiplication sign
    {0xb5}, // micro sign
    {0xb6}, // pilcrow sign
    {0xb7}, // middle dot
    {0xb8, 0xc2f7}, // division sign
    {0xbb}, // right-pointed double angle quotation mark
    {0xbc}, // vulgar fraction 1/4
    {0xbd}, // vulgar fraction 1/2
    {0xbe}, // vulgar fraction 3/4
    {0xbf}, // inverted question mark

    // Note 5: Code points 0xc? are added by static block

    {0xe0, 0x2126}, // greek Omega
    {0xe1, 0x00c6}, // Æ = ligature AE
    {0xe2, 0x00d0}, // Ð = stroked D
    {0xe3, 0x00aa}, // ª = superscript a
    {0xe4, 0x0126}, // stroked H
    // 0xe5, // not defined
    {0xe6, 0x0132}, // ligature IJ
    {0xe7, 0x013f}, // doted L
    {0xe8, 0x0141}, // stroked L
    {0xe9, 0x00d8}, // Ø = struck O
    {0xea, 0x0152}, // ligature CE
    {0xeb, 0x00ba}, // º = superscript circle
    {0xec, 0x00de}, // Þ
    {0xed, 0x0166}, // stroked T
    {0xee, 0x014a}, // kind of greek N
    {0xef, 0x0149}, // kind of greek Eta
    {0xf0, 0x0138}, // greek k
    {0xf1, 0x00e6}, // æ = ligature ae
    {0xf2, 0x0111}, // struck d
    {0xf3, 0x00f0}, // ð
    {0xf4, 0x0127}, // struck h
    {0xf5, 0x0131}, // i without dot
    {0xf6, 0x0133}, // ligature ij
    {0xf7, 0x0140}, // l with dot
    {0xf8, 0x0142}, // struck l
    {0xf9, 0x00f8}, // ø = struck o
    {0xfa, 0x0153}, // ligature ce
    {0xfb, 0x00df}, // ß = greek beta
    {0xfc, 0x00fe}, // þ
    {0xfd, 0x0167}, // struck t
    {0xfe, 0x0148}, // kind of greek eta
    // 0xff, // not defined
  }; // */

  /** List with T.61 code points. */
  /* package */ static final List<Integer> T61 = new ArrayList<>();

  /** List with Unicode code points. */
  /* package */ static final List<Integer> UNICODE = new ArrayList<>();

  /*
   * Fill arrays with code points.
   */
  static {
    // --- add block from 0x25 to 0x5b with
    //     - digits,
    //     - latin capital letters and
    //     - some other characters
    IntStream.rangeClosed(0x25, 0x5b)
        .forEach(
            character -> {
              T61.add(character);
              UNICODE.add(character);
            }); // end forEach(character -> ...)

    // --- add block from 0x61 to 0x7a with
    //     - latin small letters
    IntStream.rangeClosed(0x61, 0x7a)
        .forEach(
            character -> {
              T61.add(character);
              UNICODE.add(character);
            }); // end forEach(character -> ...)

    // --- add characters from TRANSLATION
    for (final int[] character : TRANSLATION) {
      final int t61 = character[0];
      final int uni = (character.length < 2) ? t61 : character[1];

      T61.add(t61);
      UNICODE.add(uni);
    } // end For (character...)

    // --- add T.61 multi-byte code points, i.e. characters with diacritical marks
    //     see https://en.wikipedia.org/wiki/T.51/ISO/IEC_6937
    Map.ofEntries(
            // Grave
            Map.entry(0xc1, List.of("AEIOUaeiou", "ÀÈÌÒÙàèìòù")),

            // Acute
            Map.entry(0xc2, List.of("ACEILNORSUYZacegilnorsuyz", "ÁĆÉÍĹŃÓŔŚÚÝŹáćéģíĺńóŕśúýź")),

            // Circumflex
            Map.entry(0xc3, List.of("ACEGHIJOSUWYaceghijosuwy", "ÂĈÊĜĤÎĴÔŜÛŴŶâĉêĝĥîĵôŝûŵŷ")),

            // Tilde
            Map.entry(0xc4, List.of("AINOUainou", "ÃĨÑÕŨãĩñõũ")),

            // Macron
            Map.entry(0xc5, List.of("AEIOUaeiou", "ĀĒĪŌŪāēīōū")),

            // Breve
            Map.entry(0xc6, List.of("AGUagu", "ĂĞŬăğŭ")),

            // Dot
            Map.entry(0xc7, List.of("CEGIZcegz", "ĊĖĠİŻċėġż")),

            // Umlaut
            Map.entry(0xc8, List.of("AEIOUYaeiouy", "ÄËÏÖÜŸäëïöüÿ")),

            // Ring
            Map.entry(0xca, List.of("AUau", "ÅŮåů")),

            // Cedilla
            Map.entry(0xcb, List.of("CGKLNRSTcklnrst", "ÇĢĶĻŅŖŞŢçķļņŗşţ")),

            // Double Acute
            Map.entry(0xcd, List.of("OUou", "ŐŰőű")),

            // Ogonek
            Map.entry(0xce, List.of("AEIUaeiu", "ĄĘĮŲąęįų")),

            // Caron
            Map.entry(0xcf, List.of("CDELNRSTZcdelnrstz", "ČĎĚĽŇŘŠŤŽčďěľňřšťž")))
        .forEach(
            (msByte, list) -> {
              final int[] secondCharacter = list.get(0).codePoints().toArray();
              final int[] unicode = list.get(1).codePoints().toArray();

              for (int i = 0; i < secondCharacter.length; i++) {
                T61.add((msByte << 8) + secondCharacter[i]);
                UNICODE.add(unicode[i]);
              } // end For (i...)
            });
  } // end static */

  /**
   * Comfort constructor using value.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe, because input parameter(s) are immutable.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable.</i>
   * </ol>
   *
   * @param value encoded in this primitive TLV object
   */
  public DerTeletexString(final String value) {
    super(TAG, value);
  } // end constructor */

  /**
   * Constructor reading length- and value-field from an {@link ByteBuffer}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param buffer form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws BufferUnderflowException if the length-field or the value-field ends early
   */
  /* package */ DerTeletexString(final ByteBuffer buffer) {
    super(new byte[] {(byte) TAG}, buffer);
  } // end constructor */

  /**
   * Constructor reading length- and value-field from an {@link InputStream}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param inputStream form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws IOException if underlying methods do so
   */
  /* package */ DerTeletexString(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);
  } // end constructor */

  /**
   * Converts given {@code byte[]} into corresponding {@link String}.
   *
   * @param octets to be converted
   * @return corresponding {@link String}
   * @throws IllegalArgumentException if last octet in {@code octets} has a high nibble with value
   *     'c'.
   */
  /* package */
  @Override
  String fromBytes(final byte[] octets) {
    try {
      final StringBuilder result = new StringBuilder();
      final ByteBuffer buffer = ByteBuffer.wrap(octets);

      while (buffer.hasRemaining()) {
        int t61 = buffer.get() & 0xff;

        if ((0xc0 <= t61) && (t61 <= 0xcf)) {
          t61 = (t61 << 8) + (buffer.get() & 0xff);
        } // end fi

        final int index = T61.indexOf(t61);

        // --- convert to Unicode code point
        // Note: Invalid characters are substituted by • = bullet point.
        try {
          final int unicode = UNICODE.get(index);

          result.appendCodePoint(unicode);
        } catch (IndexOutOfBoundsException ei) {
          // Note 1: The following "placeHolder" = '•' = 0x2022 is used for
          //         characters not allowed for a TeletexString. '•' works at
          //         least in the following environments:
          //         - IntelliJ-Run window (but IntelliJ-Terminal shows a different glyph)
          //         - bash console
          //         - text-files opened with Notepad
          result.append('•');
        } // end Catch (...)
      } // end While (not all bytes read)

      return result.toString();
    } catch (BufferUnderflowException e) {
      throw new IllegalArgumentException(MESSAGE, e);
    } // end Catch (...)
  } // end method */

  /**
   * Returns a comment describing the content of the object.
   *
   * <p>Findings are added to the comment if present.
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@link
   *     #getDecoded()} value
   */
  @Override
  public String getComment() {
    return getComment("TeletexString");
  } // end method */

  /**
   * Checks for invalid characters.
   *
   * <p>Valid characters are specified in <a
   * href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause 41.4,
   * table 10.
   *
   * @return {@code TRUE} if value-field contains invalid characters, {@code FALSE} otherwise
   */
  /* package */
  @Override
  boolean invalidCharacters() {
    return insDecoded.codePoints().anyMatch(codePoint -> !UNICODE.contains(codePoint));
  } // end method */

  /**
   * Converts given {@link String} to appropriate {@code byte[]}.
   *
   * <p>This is the inverse function to {@link #fromBytes(byte[])}.
   *
   * @param value {@link String} to be converted
   * @return appropriate octet-string
   * @throws IllegalArgumentException if {@code value} contains invalid characters
   */
  /* package */
  static byte[] toBytes(final String value) {
    final StringBuilder result = new StringBuilder();

    value
        .codePoints()
        .forEach(
            codePoint -> {
              try {
                final int index = UNICODE.indexOf(codePoint);
                final int t61 = T61.get(index);

                result.append(String.format((t61 > 0xff) ? "%04x" : "%02x", t61));
              } catch (IndexOutOfBoundsException e) {
                throw new IllegalArgumentException(MESSAGE, e);
              } // end Catch (...)
            }); // end forEach(codePoint -> ...)

    return Hex.toByteArray(result.toString());
  } // end method */
} // end class
