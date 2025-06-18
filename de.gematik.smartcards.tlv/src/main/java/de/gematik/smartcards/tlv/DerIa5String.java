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

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Class representing a TLV object of universal class with tag-number 22, i.e. IA5String-type.
 *
 * <p>For more information about the encoding, see
 *
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/IA5STRING">Wikipedia</a>,
 *   <li><a href="https://en.wikipedia.org/wiki/ISO/IEC_646">Wikipedia ISO/IEC 646</a>, and
 *   <li>the Germany's National Replacement Character Set <a
 *       href="https://en.wikipedia.org/wiki/DIN_66003">Wikipedia DIN 66003</a>
 * </ul>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class DerIa5String extends DerRestrictedCharacterStringTypes {

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
  public static final int TAG = 22; // */

  /** List with T.61 code points. */
  /* package */ static final List<Integer> IA5 = new ArrayList<>();

  /** List with Unicode code points. */
  /* package */ static final List<Integer> UNICODE = new ArrayList<>();

  /*
   * Fill arrays with code points.
   */
  static {
    // --- fill lists according to DIN 66003
    IntStream.rangeClosed(0x00, 0xff)
        .forEach(
            codePoint -> {
              IA5.add(codePoint);

              switch (codePoint) { // NOPMD missing default, false positive
                case 0x40 -> // ASCII = @, DIN 66003 = §
                    UNICODE.add(0xa7);
                case 0x5b -> // ASCII = [, DIN 66003 = Ä
                    UNICODE.add(0xc4);
                case 0x5c -> // ASCII = \, DIN 66003 = Ö
                    UNICODE.add(0xd6);
                case 0x5d -> // ASCII = ], DIN 66003 = Ü
                    UNICODE.add(0xdc);
                case 0x7b -> // ASCII = {, DIN 66003 = ä
                    UNICODE.add(0xe4);
                case 0x7c -> // ASCII = |, DIN 66003 = ö
                    UNICODE.add(0xf6);
                case 0x7d -> // ASCII = }, DIN 66003 = ü
                    UNICODE.add(0xfc);
                case 0x7e -> // ASCII = ~, DIN 66003 = ß
                    UNICODE.add(0xdf);
                default -> // ASCII == DIN 66003
                    UNICODE.add(codePoint);
              } // end Switch (codePoint)
            }); // end forEach(codePoint -> ...)
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
  public DerIa5String(final String value) {
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
  /* package */ DerIa5String(final ByteBuffer buffer) {
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
  /* package */ DerIa5String(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);
  } // end constructor */

  /**
   * Converts given {@code byte[]} into corresponding {@link String}.
   *
   * @param octets to be converted
   * @return corresponding {@link String}
   */
  /* package */
  @Override
  String fromBytes(final byte[] octets) {
    final StringBuilder result = new StringBuilder();
    final ByteBuffer buffer = ByteBuffer.wrap(octets);

    while (buffer.hasRemaining()) {
      final int ia5 = buffer.get() & 0xff;
      final int index = IA5.indexOf(ia5);
      final int unicode = UNICODE.get(index);

      result.appendCodePoint(unicode);
    } // end While (not all bytes read)

    return result.toString();
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
    return getComment("IA5String");
  } // end method */

  /**
   * Checks for invalid characters.
   *
   * <p>Valid characters are specified in <a
   * href="https://en.wikipedia.org/wiki/DIN_66003">Wikipedia DIN 66003</a>.
   *
   * <p>This method returns always {@code FALSE} for the following reasons:
   *
   * <ol>
   *   <li>If this method is called in {@link
   *       DerRestrictedCharacterStringTypes#DerRestrictedCharacterStringTypes(int, String)}
   *       constructor, then the conversion {@link DerIa5String#toBytes(String)} was successful,
   *       i.e. no invalid characters occurred.
   *   <li>If this method is called in {@link
   *       DerRestrictedCharacterStringTypes#DerRestrictedCharacterStringTypes(byte[], InputStream)}
   *       then no invalid characters can occur, because for each byte-value a valid character
   *       exists.
   * </ol>
   *
   * @return {@code false}
   */
  /* package */
  @Override
  boolean invalidCharacters() {
    return false;
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
    final byte[] result = new byte[value.length()];

    try {
      for (int i = result.length; i-- > 0; ) { // NOPMD assignment in operand
        final int codePoint = value.codePointAt(i);
        final int index = UNICODE.indexOf(codePoint);
        final int ia5 = IA5.get(index);
        result[i] = (byte) ia5;
      } // end For (i...)
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException(MESSAGE, e);
    } // end Catch (...)

    return result;
  } // end method */
} // end class
