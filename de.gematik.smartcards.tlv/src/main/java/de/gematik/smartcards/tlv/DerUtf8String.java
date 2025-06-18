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

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class representing a TLV object of universal class with tag-number 12, i.e. UTF8String-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause 8.23.10.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class DerUtf8String extends PrimitiveSpecific<String> {

  /**
   * Integer representation of tag for UTF8String.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> table 1
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 12; // */

  /**
   * De-coded value of value-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  /* package */ volatile @Nullable String insDecoded; // NOPMD volatile not recommended */

  /**
   * Comfort constructor using value.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe, because input parameter is immutable.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter is immutable</i>
   * </ol>
   *
   * @param value encoded in this primitive TLV object
   */
  public DerUtf8String(final String value) {
    super(TAG, value.getBytes(StandardCharsets.UTF_8));

    insDecoded = value;
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
   *     Integer#MAX_VALUE}
   * @throws BufferUnderflowException if the length-field or the value-field ends early
   */
  /* package */ DerUtf8String(final ByteBuffer buffer) {
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
   *     Integer#MAX_VALUE}
   * @throws IOException if underlying methods do so
   */
  /* package */ DerUtf8String(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);
  } // end constructor */

  /**
   * Returns a comment describing the content of the object.
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@code
   *     OCTET-STRING}
   */
  @Override
  public String getComment() {
    return DELIMITER + "UTF8String := \"" + getDecoded() + '"';
  } // end method */

  /**
   * Returns value this universal, primitive TLV object encodes.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return value represented by this universal-class primitive TLV object of type UTF8String, i.e.
   *     {@link String}
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public String getDecoded() {
    // Note 1: Because object content is immutable it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    String result = insDecoded; // read from main memory into thread local memory
    if (null == result) {
      // ... obviously attribute insDecoded has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      if (!checkEncoding(insValueField)) {
        insFindings.add("invalid encoding");
      } // end fi

      result = new String(insValueField, StandardCharsets.UTF_8);

      insDecoded = result; // store insDecoded into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Checks if octet string is in conformance to UFT-8.
   *
   * <p>For more details see <a href="https://en.wikipedia.org/wiki/UTF-8">Wikipedia-en</a> and <a
   * href="https://de.wikipedia.org/wiki/UTF-8">Wikipedia-de</a>.
   *
   * <p><i><b>Note:</b> When an octet string is converted to a string via {@link
   * String#String(byte[], java.nio.charset.Charset)} with {@link StandardCharsets#UTF_8} and the
   * encoding is not in conformance to RFC 3629, then the resulting {@link String} contains the <a
   * href="https://en.wikipedia.org/wiki/Specials_(Unicode_block)#Replacement_character">ReplacementCharacter</a>.
   * On the other hand, that <a
   * href="https://en.wikipedia.org/wiki/Specials_(Unicode_block)#Replacement_character">ReplacementCharacter</a>
   * possibly is intentionally contained in a {@link String} resulting from an octet string with a
   * valid encoding. Thus, it is not possible to check for valid / invalid encoded octet string by
   * checking the presence of the <a
   * href="https://en.wikipedia.org/wiki/Specials_(Unicode_block)#Replacement_character">ReplacementCharacter</a>
   * in the corresponding {@link String}.</i>
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @param octet octet string checked for conformance
   * @return {@code TRUE} if no findings are detected, {@code FALSE} otherwise
   */
  @VisibleForTesting // otherwise = private
  @SuppressWarnings({
    "PMD.AvoidReassigningLoopVariables", // "index" is reassigned in the for-loop
    "PMD.AvoidLiteralsInIfCondition", // lots of literals in if-statements
  })
  /* package */ static boolean checkEncoding(final byte[] octet) {
    try {
      final var buffer = ByteBuffer.wrap(octet);
      while (buffer.hasRemaining()) {
        final var b1 = buffer.get();
        final boolean finding;
        if (0xc0 == (b1 & 0xe0)) {
          // ... code point on two bytes
          final var b2 = buffer.get();
          finding = checkEncoding2(b1, b2);
        } else if (0xe0 == (b1 & 0xf0)) {
          // ... code point on three bytes
          final var b2 = buffer.get();
          final var b3 = buffer.get();
          finding = checkEncoding3(b1, b2, b3);
        } else if (0xf0 == (b1 & 0xf8)) {
          // ... code point on four bytes
          final var b2 = buffer.get();
          final var b3 = buffer.get();
          final var b4 = buffer.get();
          finding = checkEncoding4(b1, b2, b3, b4);
        } else {
          // ... code point on more than four bytes
          //     => invalid encoding
          finding = b1 < 0; // code point on more than four bytes?
        } // end else

        if (finding) {
          return false;
        } // end fi
      } // end While (buffer.hasRemaining)
    } catch (BufferUnderflowException e) {
      return false;
    } // end Catch (...)

    return true;
  } // end method */

  private static boolean checkEncoding2(final byte b1, final byte b2) {
    if (0x80 != (b2 & 0xc0)) { // NOPMD literal in if statement
      // ... invalid 2nd byte
      return true;
    } // end fi (valid 2nd byte)

    final int codePoint = ((b1 & 0x1f) << 6) | (b2 & 0x3f);
    // Note: If "codePoint < 80" then the encoding is longer than necessary (overlong encoding).
    return 0x80 > codePoint;
  } // end method */

  private static boolean checkEncoding3(final byte b1, final byte b2, final byte b3) {
    if ((0x80 != (b2 & 0xc0)) || (0x80 != (b3 & 0xc0))) {
      // ... invalid 2nd, 3rd byte
      return true;
    } // end fi (invalid 2nd, 3rd byte)

    final int codePoint = ((((b1 & 0x0f) << 6) | (b2 & 0x3f)) << 6) | (b3 & 0x3f);

    //      overlong encoding   OR                 invalid range
    return (0x0800 > codePoint) || ((0xd800 <= codePoint) && (codePoint <= 0xdfff));
  } // end method */

  private static boolean checkEncoding4(
      final byte b1, final byte b2, final byte b3, final byte b4) {
    if ((0x80 != (b2 & 0xc0)) || (0x80 != (b3 & 0xc0)) || (0x80 != (b4 & 0xc0))) {
      // ... invalid 2nd, 3rd, 4th byte
      return true;
    } // end fi (invalid 2nd, 3rd byte)

    final int codePoint =
        ((((((b1 & 0x07) << 6) | (b2 & 0x3f)) << 6) | (b3 & 0x3f)) << 6) | (b4 & 0x3f);

    //       overlong encoding   OR     invalid range
    return (0x10000 > codePoint) || (0x10ffff < codePoint);
  } // end method */
} // end class
