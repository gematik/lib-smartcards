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
import java.nio.charset.StandardCharsets;

/**
 * Class representing a TLV object of universal class with tag-number 19, i.e. PrintableString-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause 41.4.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class DerPrintableString extends DerRestrictedCharacterStringTypes {

  /**
   * Integer representation of tag for PrintableString.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause
   *       8, table 1
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 19; // */

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
  public DerPrintableString(final String value) {
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
  /* package */ DerPrintableString(final ByteBuffer buffer) {
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
  /* package */ DerPrintableString(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);
  } // end constructor */

  /**
   * Converts given {@code byte[]} into corresponding {@link String}.
   *
   * <p>This is the inverse function to {@link #toBytes(int, String)}.
   *
   * @param octets to be converted
   * @return corresponding {@link String}
   */
  /* package */
  @Override
  String fromBytes(final byte[] octets) {
    // see ISO/IEC 8824-1:2021 clause 41.4
    return new String(insValueField, StandardCharsets.US_ASCII);
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
    return getComment("PrintableString");
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
  @Override
  /* package */ boolean invalidCharacters() {
    // spotless:off
    return insDecoded
        .chars()
        .anyMatch(
            character -> {
              final var upperCase = ('A' <= character) && (character <= 'Z');
              final var lowerCase = upperCase || ('a' <= character) && (character <= 'z');
              final var digits = lowerCase || ('0' <= character) && (character <= '9');
              final var special = digits || switch (character) {
                case ' ',   // SPACE
                     '\'',  // APOSTROPHE
                     '(',   // LEFT PARENTHESIS
                     ')',   // RIGHT PARENTHESIS
                     '+',   // PLUS SIGN
                     ',',   // COMMA
                     '-',   // HYPHEN-MINUS
                     '.',   // FULL-STOP
                     '/',   // SOLIDUS
                     ':',   // COLON
                     '=',   // EQUALS SIGN
                     '?' -> // QUESTION MARK
                    true;
                default -> false;
              };

              return !special;
            });
    // spotless:on
  } // end method */

  /**
   * Converts given {@link String} to appropriate {@code byte[]}.
   *
   * <p>This is the inverse function to {@link #fromBytes(byte[])}.
   *
   * @param value {@link String} to be converted
   * @return appropriate octet-string
   */
  /* package */
  static byte[] toBytes(final String value) {
    // see ISO/IEC 8824-1:2021 clause 41.4
    return value.getBytes(StandardCharsets.US_ASCII);
  } // end method */
} // end class
