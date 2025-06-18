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

import de.gematik.smartcards.utils.AfiUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Class representing a TLV object of universal class with tag-number 0, i.e., End-of-Content-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause 8.1.5.
 *
 * <p>Because this type of TLV-object is always primitive, with tag-number 0 and the value-field is
 * always absent, there is just one possible instantiation. Thus, this class has no public
 * constructor and just one instantiation:
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class DerEndOfContent extends PrimitiveSpecific<String> {

  /**
   * Integer representation of End-of-Content tag.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-X.693-201508-I/en">ISO/IEC 8824-1:2015</a>
   *       table 1
   *   <li><a href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *       8.1.5, note
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 0; // */

  /** The one and only instantiation of this class. */
  public static final DerEndOfContent EOC = new DerEndOfContent(); // */

  /** Because this class has no values, the comment is fixed. */
  private static final String COMMENT = DELIMITER + "EndOfContent"; // */

  /**
   * Default constructor.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because object is immutable.</i>
   * </ol>
   */
  private DerEndOfContent() {
    // see ISO/IEC 8825-1:2025 clause 8.1.5.
    super(TAG, AfiUtils.EMPTY_OS);
  } // end constructor */

  /**
   * Constructor reading length- and value-field from the given {@link ByteBuffer}.
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
  private DerEndOfContent(final ByteBuffer buffer) {
    super(new byte[] {(byte) TAG}, buffer);

    check();
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
  private DerEndOfContent(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);

    check();
  } // end constructor */

  /**
   * Check instance attributes.
   *
   * <p>Here a finding is added in case
   *
   * <ol>
   *   <li>the value-field is present or
   *   <li>the length-field contains more than one octet
   * </ol>
   */
  private void check() {
    if (0 != getLengthOfValueField()) { // NOPMD literal in if statement
      // ... length of value-field not 0
      insFindings.add("value-field present");
    } else if (1 != insLengthOfLengthFieldFromStream) { // NOPMD literal in conditional stat.
      // ... value-field absent  AND  length of length-field not 1
      insFindings.add("original length-field unequal to '00'");
    } // end else
  } // end method */

  /**
   * Checks for predefined value.
   *
   * @param result being checked
   */
  private static DerEndOfContent checkInstance(final DerEndOfContent result) {
    if (result.insFindings.isEmpty()) {
      return EOC;
    } // end fi
    // ... findings present

    return result;
  } // end method */

  /**
   * Pseudo-constructor reading length- and value-field from an {@link ByteBuffer}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param buffer form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Integer#MAX_VALUE}
   * @throws BufferUnderflowException if the length-field or the value-field ends early
   */
  /* package */
  static DerEndOfContent readInstance(final ByteBuffer buffer) {
    return checkInstance(new DerEndOfContent(buffer));
  } // end method */

  /**
   * Pseudo-constructor reading length- and value-field from an {@link InputStream}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param inputStream form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Integer#MAX_VALUE}
   * @throws IOException if underlying methods do so
   */
  /* package */
  static DerEndOfContent readInstance(final InputStream inputStream) throws IOException {
    return checkInstance(new DerEndOfContent(inputStream));
  } // end method */

  /**
   * Returns a comment describing the content of the object.
   *
   * <p>Findings are added to the comment if
   *
   * <ol>
   *   <li>a value-field is present, or
   *   <li>length-field unequal to '00'
   * </ol>
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@code
   *     EndOfContent}
   */
  @Override
  public String getComment() {
    return COMMENT + getFindings();
  } // end method */

  /**
   * Getter.
   *
   * @return an empty {@link String}, because this type has no useful decoded value
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public String getDecoded() {
    return "";
  } // end method */
} // end class
