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
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class representing a TLV object of universal class with tag-number 4, i.e. OCTETSTRING-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause 8.7.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class DerOctetString extends PrimitiveSpecific<byte[]> {

  /** Because this class has no values, the comment is fixed. */
  private static final String COMMENT = DELIMITER + "OCTETSTRING"; // */

  /**
   * Integer representation of tag for OCTETSTRING.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause
   *       8, table 1
   *   <li><a href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause
   *       8.7.3.2 note 2
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 4; // */

  /**
   * Comfort constructor using value.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       defensively cloned</i>
   * </ol>
   *
   * @param value encoded in this primitive TLV object
   */
  public DerOctetString(final byte[] value) {
    super(TAG, value.clone());
  } // end constructor */

  /**
   * Constructor reading length- and value-field from {@link ByteBuffer}.
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
  /* package */ DerOctetString(final ByteBuffer buffer) {
    super(new byte[] {(byte) TAG}, buffer);
  } // end constructor */

  /**
   * Constructor reading length- and value-field from {@link InputStream}.
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
  /* package */ DerOctetString(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);
  } // end constructor */

  /**
   * Returns a comment describing the content of the object.
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@code
   *     OCTETSTRING}
   */
  @Override
  public String getComment() {
    return COMMENT;
  } // end method */

  /**
   * Returns value this universal, primitive TLV object encodes.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is defensively cloned and
   *       never used again by this method.</i>
   * </ol>
   *
   * @return value represented by this universal-class primitive TLV object of type OCTETSTRING,
   *     i.e. {@code byte[]}.
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public byte[] getDecoded() {
    return getValueField();
  } // end method */

  /**
   * Converts object to a printable hexadecimal string with given delimiters.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable or
   *       primitive and return value is immutable.</i>
   * </ol>
   *
   * @param delimiter to be used between
   *     <ul>
   *       <li>tag-field and length-field
   *       <li>length-field and value-field (if and only if present)
   *     </ul>
   *
   * @param delo is ignored for {@link PrimitiveBerTlv}
   * @param noIndentation indicates how often the indent parameter {@code delimiter} is used before
   *     printing the tag
   * @param addComment flag indicates whether a comment is added to the output ({@code TRUE}) or not
   *     ({@code FALSE})
   * @return string with characters (0..9, a..f) except for the delimiters
   * @see PrimitiveBerTlv#toString(String, String, int, boolean)
   */
  @Override
  @VisibleForTesting
  /* package */ String toString(
      final String delimiter,
      final String delo,
      final int noIndentation,
      final boolean addComment) {
    final StringBuilder result =
        new StringBuilder(super.toString(delimiter, delo, noIndentation, addComment));

    if (addComment) {
      // ... comment requested (and already added by superclass)
      result.append(commentDecoded(noIndentation, getDecoded()));
    } // end fi

    return result.toString();
  } // end method */
} // end class
