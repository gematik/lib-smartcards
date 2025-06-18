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
import java.util.Collection;
import java.util.List;

/**
 * Class representing a TLV object of universal class with tag-number 16 = '10', i.e.,
 * SEQUENCE-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause 8.9.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class DerSequence extends ConstructedBerTlv implements DerSpecific {

  /**
   * Integer representation of tag for SEQUENCE.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> table 1
   *   <li><a href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause
   *       8.9.3, example
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 0x30; // */

  /**
   * Comfort constructor using value.
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
   * @param value encoded in this primitive TLV object
   */
  public DerSequence(final Collection<? extends BerTlv> value) {
    super(TAG, value);
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
   *     Long#MAX_VALUE}
   * @throws BufferUnderflowException if the length-field or the value-field ends early
   */
  /* package */ DerSequence(final ByteBuffer buffer) {
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
   *     Long#MAX_VALUE}
   * @throws IOException if underlying methods do so
   */
  /* package */ DerSequence(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);
  } // end constructor */

  /**
   * Appends given TLV-object at the end of the value-field.
   *
   * <p>Because this (constructed) TLV object is immutable, this method returns another TLV object.
   * The only difference between this TLV object and the returned value is that the value field of
   * the returned TLV object has an additional entry in the value-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param tlv object to be appended at the end of the value-field
   * @return clone of this TLV object where the given TLV object is appended to its value-field
   */
  @Override
  public DerSequence add(final BerTlv tlv) {
    final List<BerTlv> valueField = new ArrayList<>(insValueField);
    valueField.add(tlv);

    return new DerSequence(valueField);
  } // end method */

  /**
   * Returns a comment describing the content of the object.
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@code
   *     SEQUENCE with ... elements}
   */
  @Override
  public String getComment() {
    final int noElements = insValueField.size();

    return DELIMITER
        + "SEQUENCE with "
        + noElements
        + ((1 == noElements) ? " element" : " elements"); // singular vs. plural
  } // end method */

  /**
   * Returns whether TLV-object is in accordance to its specification.
   *
   * <p>Because this class never has findings this method always returns {@code TRUE}.
   *
   * @return {@code TRUE}
   * @see DerSpecific#isValid()
   */
  @Override
  public boolean isValid() {
    return true;
  } // end method */

  /**
   * Returns value this universal, constructed TLV object encodes.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is defensively cloned and
   *       never used again by this method.</i>
   * </ol>
   *
   * @return value represented by this universal-class primitive TLV object of type SEQUENCE, i.e.
   *     {@link List}.
   */
  public List<BerTlv> getDecoded() {
    return getTemplate();
  } // end method */
} // end class
