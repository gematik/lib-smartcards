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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class representing a TLV object of universal class with tag-number 1, i.e. BOOLEAN-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause 8.2.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class DerBoolean extends PrimitiveSpecific<Boolean> {

  /**
   * Integer representation of tag for BOOLEAN.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-X.693-201508-I/en">ISO/IEC 8824-1:2015</a>
   *       table 1
   *   <li><a href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *       8.2.2, example
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 1; // */

  /** The one and only instantiation of this class representing {@code TRUE}. */
  public static final DerBoolean TRUE = create(true); // */

  /** The one and only instantiation of this class representing {@code FALSE}. */
  public static final DerBoolean FALSE = create(false); // */

  /** De-coded value of value-field. */
  private final boolean insDecoded; // */

  /**
   * Comfort constructor.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because object is immutable.</i>
   * </ol>
   *
   * @param buffer form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Integer#MAX_VALUE}
   * @throws BufferUnderflowException if the length-field or the value-field ends early
   */
  private DerBoolean(final ByteBuffer buffer) {
    super(new byte[] {(byte) TAG}, buffer);

    insDecoded = (insValueField.length > 0) && (0 != insValueField[0]);

    check();
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because object is immutable.</i>
   * </ol>
   *
   * @param inputStream form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Integer#MAX_VALUE}
   * @throws IOException if underlying methods do so
   */
  private DerBoolean(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);

    insDecoded = (insValueField.length > 0) && (0 != insValueField[0]);

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
    if (1 != getLengthOfValueField()) { // NOPMD literal in if statement
      // ... length of value-field != 1
      insFindings.add("length of value-field unequal to 1");
    } else if (1 != insLengthOfLengthFieldFromStream) { // NOPMD literal in if statement
      insFindings.add("original length-field unequal to '01'");
    } // end fi
  } // end method */

  /**
   * Checks for predefined value.
   *
   * @param result being checked
   */
  private static DerBoolean checkInstance(final DerBoolean result) {
    if (result.isValid()) {
      // ... TLV-object according to specification
      //     => return pre-defined objects

      if (result.getDecoded()) {
        // ... TRUE
        return (-1 == result.insValueField[0]) ? TRUE : result;
      } // end fi
      // ... FALSE
      return FALSE;
    } // end fi
    // ... TLV-object not in accordance to specification

    return result;
  } // end method */

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
  /* package */
  static DerBoolean readInstance(final ByteBuffer buffer) {
    return checkInstance(new DerBoolean(buffer));
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
  /* package */
  static DerBoolean readInstance(final InputStream inputStream) throws IOException {
    return checkInstance(new DerBoolean(inputStream));
  } // end constructor */

  /**
   * Pseudo constructor.
   *
   * @param value encoded in this primitive TLV object
   * @return corresponding instance of TLV-object
   */
  private static DerBoolean create(final boolean value) {
    // see ISO/IEC 8825-1:2015 clause 8.2.2.
    return create(String.format("-01-%02x", value ? 0xff : 0x00));
  } // end method */

  /**
   * Pseudo constructor.
   *
   * <p><i><b>Note 1:</b> It is important to use a way for constructing the two definite values such
   * that instance attributes {@link #insLengthOfLengthFieldFromStream} and {@link
   * #insLengthOfValueFieldFromStream} are properly set. That is necessary to substitute instances
   * read from {@link InputStream}s by the fixed values defined in this class.</i>
   *
   * <p><i><b>Note 2:</b> The only reason for separating {@link #create(boolean)} and {@code
   * create(String)} into two methods is: Test methods now can provoke an exception. Doing so
   * improves code coverage.</i>
   *
   * @param octets with length- and value-field from which the boolean ASN.1 value is constructed
   * @return corresponding instance of TLV-object
   * @throws IllegalStateException if construction fails
   */
  @VisibleForTesting // otherwise = private
  /* package */ static DerBoolean create(final String octets) {
    try {
      return new DerBoolean(new ByteArrayInputStream(Hex.toByteArray(octets)));
    } catch (IOException | IllegalArgumentException e) {
      throw new IllegalStateException("initialization failed", e);
    } // end Catch (...)
  } // end method */

  /**
   * Returns a comment describing the content of the object.
   *
   * <p>Findings are added to the comment if length of value-field differs from 1.
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@code
   *     BOOLEAN := } and {@link #getDecoded()}
   */
  @Override
  public String getComment() {
    return DELIMITER + "BOOLEAN := " + getDecoded() + getFindings();
  } // end method */

  /**
   * Returns value this universal, primitive TLV object encodes.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return value represented by this universal-class primitive TLV object of type BOOLEAN
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public Boolean getDecoded() {
    return insDecoded;
  } // end method */
} // end class
