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
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class representing a TLV object of universal class with tag-number 2, i.e., INTEGER-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause 8.3.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.AvoidUsingVolatile"})
public final class DerInteger extends PrimitiveSpecific<BigInteger> {

  /**
   * Integer representation of tag for INTEGER.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-X.693-201508-I/en">ISO/IEC 8824-1:2015</a>
   *       table 1
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 2; // */

  /** Error message in case the nine MSBit are all equal. */
  @VisibleForTesting /* package */ static final String EM_9 = "9 MSBit all equal"; // */

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
  @VisibleForTesting /* package */ volatile @Nullable BigInteger insDecoded; // */

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
  public DerInteger(final BigInteger value) {
    // see ISO/IEC 8825-1:2015 clause 8.2.2.
    super(TAG, value.toByteArray());

    insDecoded = value;
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
  /* package */ DerInteger(final ByteBuffer buffer) {
    super(new byte[] {(byte) TAG}, buffer);

    check();
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
  /* package */ DerInteger(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);

    check();
  } // end constructor */

  private void check() {
    if (0 == insValueField.length) {
      insFindings.add("value-field absent");
    } // end fi

    if ((insValueField.length > 1)
        && (((-1 == insValueField[0]) && (0x80 == (insValueField[1] & 0x80))) // 9x '1'
            || ((0 == insValueField[0]) && (0x00 == (insValueField[1] & 0x80))) // 9x '0'
        )) {
      // ... more than one octet in value-field   AND  9 MSBit are all equal
      insFindings.add(EM_9);
    } // end fi
    // ... correct value-field
  } // end method */

  /**
   * Returns a comment describing the content of the object.
   *
   * <p>Findings are added to the comment if
   *
   * <ol>
   *   <li>value-field is absent
   *   <li>the 9 MSBit in the original value-field are all equal
   * </ol>
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@code
   *     INTEGER := } and the decimal representation of the {@link #getDecoded()} value
   */
  @Override
  public String getComment() {
    return DELIMITER + "INTEGER := " + getDecoded() + getFindings();
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
   * @return value represented by this universal-class primitive TLV object of type INTEGER, i.e.
   *     {@link BigInteger}
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public BigInteger getDecoded() {
    // Note 1: Because object content is immutable, it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    BigInteger result = insDecoded; // read from the main memory into thread local memory
    if (null == result) {
      // ... obviously, attribute insDecoded has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      // Note 2: "new BigInteger(byte[])" throws an exception if the byte-array
      //         is empty. In such a case, "BigInteger.ZERO" is assumed.
      result = (0 == insValueField.length) ? BigInteger.ZERO : new BigInteger(insValueField);

      insDecoded = result; // store insDecoded into thread local memory
    } // end fi

    return result;
  } // end method */
} // end class
