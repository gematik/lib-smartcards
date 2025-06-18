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

import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.Hex;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Class representing a TLV object of universal class with tag-number 6, i.e. OBJECT
 * IDENTIFIER-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause 8.19.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "EI_EXPOSE_REP",
//         Short message: May expose internal representation by returning reference
//                        to mutable object
//         That finding is suppressed because insDecoded is of type AfiOid,
//         which is immutable according to JavaDoc.
// Note 2: Spotbugs claims "EI_EXPOSE_REP2",
//         Short message: May expose internal representation by incorporating
//                        reference to mutable object
//         That finding is suppressed because insDecoded is of type AfiOid,
//         which is immutable according to JavaDoc.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP", // see note 1
  "EI_EXPOSE_REP2" // see note 2
}) // */
public final class DerOid extends PrimitiveSpecific<AfiOid> {

  /**
   * Integer representation of tag for INTEGER.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> table 1
   *   <li><a href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause
   *       8.19.5, example
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 6; // */

  /** De-coded value of value-field. */
  /* package */ final AfiOid insDecoded; // */

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
  public DerOid(final AfiOid value) {
    // see ISO/IEC 8825-1:2015 clause 8.19
    super(TAG, Hex.toByteArray(value.getOctetString()));

    insDecoded = value; // EI_EXPOSE_REP2
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
   * @throws ArithmeticException if decoded value is not supported by this implementation, see
   *     {@link AfiOid#AfiOid(byte[])}
   * @throws BufferUnderflowException if the length-field or the value-field ends early
   */
  /* package */ DerOid(final ByteBuffer buffer) {
    super(new byte[] {(byte) TAG}, buffer);

    AfiOid decoded;
    try {
      decoded = new AfiOid(insValueField);
    } catch (ArithmeticException | IllegalArgumentException e) {
      decoded = AfiOid.INVALID;
      insFindings.add("invalid OID");
    } // end Catch (...)

    insDecoded = decoded;
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
  /* package */ DerOid(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);

    AfiOid decoded;
    try {
      decoded = new AfiOid(insValueField);
    } catch (ArithmeticException | IllegalArgumentException e) {
      decoded = AfiOid.INVALID;
      insFindings.add("invalid OID");
    } // end Catch (...)

    insDecoded = decoded;
  } // end constructor */

  /**
   * Returns a comment describing the content of the object.
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@code
   *     OBJECT IDENTIFIER := } and the {@link #getDecoded()} value
   */
  @Override
  public String getComment() {
    final String point = insDecoded.getPoint();
    final String name = insDecoded.getName(); // equal to point if OID is pre-defined in AfiOid

    return DELIMITER
        + "OBJECT IDENTIFIER := "
        + name // show user-friendly name first
        + (name.equals(point) ? "" : " = " + point); // do not show point-notation twice
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
   * @return value represented by this universal-class primitive TLV object of type OBJECT
   *     IDENTIFIER, i.e. {@link AfiOid}
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public AfiOid getDecoded() {
    return insDecoded; // EI_EXPOSE_REP
  } // end method */
} // end class
