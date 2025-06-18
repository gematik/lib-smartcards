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
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class representing a TLV object of universal class with tag-number 3, i.e. BITSTRING-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause 8.6.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.AvoidUsingVolatile"})
public final class DerBitString extends PrimitiveSpecific<byte[]> {

  /**
   * Integer representation of tag for BITSTRING.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-X.693-201508-I/en">ISO/IEC 8824-1:2015</a>
   *       table 1
   *   <li><a href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *       8.6.4.2
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 3; // */

  /** Error message in case of more than seven unused bits. */
  @VisibleForTesting
  /* package */ static final String EM_7 = "numberOfUnusedBits greater than 7"; // */

  /** Error message in case the bit-string is empty, but one or more unused bits. */
  @VisibleForTesting
  /* package */ static final String EM_GT0 =
      "numberOfUnusedBits greater than zero but empty bit-string"; // */

  /**
   * Cash the number of unused bits.
   *
   * <p>See <a href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   * 8.6.2.2.
   */
  /* package */ final int insNumberOfUnusedBits; // */

  /**
   * Bit-string representation containing just '0' and '1' characters grouped together.
   *
   * <p>Examples:
   *
   * <ol>
   *   <li>Empty bit-string: ''.
   *   <li>Two bits: '10'.
   *   <li>Nine bits: '1 00110011'.
   * </ol>
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  /* package */ volatile @Nullable String insBitString; // */

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
  /* package */
  @Nullable byte[] insDecoded; // */

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
   * @param value encoded in this primitive TLV object, the number of unused bits is assumed to be
   *     zero
   */
  public DerBitString(final byte[] value) {
    this(0, value);
  } // end constructor */

  /**
   * Comfort constructor using value.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read,
   *       defensively cloned or primitive</i>
   * </ol>
   *
   * @param numberOfUnusedBits number of unused bits in last octet, <b>SHALL</b> be in range [0, 7]
   * @param value encoded in this primitive TLV object
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@link #insNumberOfUnusedBits} is not in range [0, 7]
   *       <li>{@link #insNumberOfUnusedBits} is not zero AND parameter {@code value} is empty
   *     </ol>
   */
  public DerBitString(final int numberOfUnusedBits, final byte[] value) {
    // see ISO/IEC 8825-1:2015 clause 8.6.
    super(TAG, AfiUtils.concatenate(new byte[] {(byte) numberOfUnusedBits}, value));

    insNumberOfUnusedBits = numberOfUnusedBits;
    insDecoded = value.clone();

    if ((0 > insNumberOfUnusedBits) || (insNumberOfUnusedBits > 7)) {
      // ... invalid number of unused bits, see ISO/IEC 8825-1:2015 clause 8.6.2.2
      throw new IllegalArgumentException("numberOfUnusedBits out of range [0, 7]");
    } // end fi
    // ... 0 <= numberOfUnusedBits <= 7

    if ((0 != insNumberOfUnusedBits) && (1 == getLengthOfValueField())) {
      throw new IllegalArgumentException(EM_GT0);
    } // end fi
  } // end constructor */

  /**
   * Constructor reading length- and value-field from an input stream.
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
  /* package */ DerBitString(final ByteBuffer buffer) {
    super(new byte[] {(byte) TAG}, buffer);

    insNumberOfUnusedBits = (0 == insValueField.length) ? 0 : (insValueField[0] & 0xff);

    check();
  } // end constructor */

  /**
   * Constructor reading length- and value-field from an input stream.
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
  /* package */ DerBitString(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);

    insNumberOfUnusedBits = (0 == insValueField.length) ? 0 : (insValueField[0] & 0xff);

    check();
  } // end constructor */

  private void check() {
    if (0 == insValueField.length) {
      insFindings.add("value-field absent");
    } else {
      // ... at least one octet in value-field

      if (insNumberOfUnusedBits > 7) { // NOPMD literal in if statement
        // ... invalid number of unused bits, see ISO/IEC 8825-1:2015 clause 8.6.2.2
        insFindings.add(EM_7);
      } else if ((0 != insNumberOfUnusedBits) && (1 == getLengthOfValueField())) {
        // ... number of unused bits in range [1, 7], but empty bit-string
        insFindings.add(EM_GT0);
      } // end else if
    } // end fi
  } // end method */

  /**
   * Returns bit-string, performs lazy-initialization, if necessary.
   *
   * @return bit-string
   */
  /* package */ String getBitString() {
    // Note 1: Because object content is immutable, it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    String result = insBitString; // read from the main memory into thread local memory
    if (null == result) {
      // ... obviously attribute insBitString has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      result = toBitString(getNumberOfUnusedBits(), getDecoded());

      insBitString = result; // store insBitString into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Returns a comment describing the content of the object.
   *
   * <p>Findings are added to the comment if
   *
   * <ol>
   *   <li>number of unused bits is not in range [0, 7]
   *   <li>number of unused bits is greater than zero but bit-string is empty
   * </ol>
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus the
   *     number of unused bits and the bit-string
   */
  @Override
  public String getComment() {
    final int noUnusedBits = getNumberOfUnusedBits();

    return String.format(
        DELIMITER + "BITSTRING: %d unused bit%s: '%s'%s",
        noUnusedBits,
        (1 == noUnusedBits) ? "" : "s", // singular vs. plural
        getBitString(),
        getFindings());
  } // end method */

  /**
   * Returns value this universal, primitive TLV object encodes.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is never used again by this
   *       method.</i>
   * </ol>
   *
   * @return value represented by this universal-class primitive TLV object of type BITSTRING, i.e.
   *     {@code byte[]}. It is possible that the last octet contains unused bits, see {@link
   *     #getNumberOfUnusedBits()}.
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public byte[] getDecoded() {
    // Note 1: Because object content is immutable, it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    @Nullable byte[] result = insDecoded; // read from the main memory into thread local memory
    if (null == result) {
      // ... obviously, attribute insDecoded has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      if (0 == insValueField.length) {
        // ... value-field absent, TLV-object not in accordance to specification
        //     => play it safe and assume empty bit-string
        result = AfiUtils.EMPTY_OS;
      } else {
        // ... at least one byte in value-field coding number of unused bits
        //     => extract bit-string, ignore octet with number of unused bits

        // Note: The fact that the number of unused bits is probably > 7
        //       is intentionally ignored by this implementation.

        result = Arrays.copyOfRange(insValueField, 1, (int) (getLengthOfValueField()));
      } // end else

      insDecoded = result; // store insDecoded into thread local memory
    } // end fi

    return result.clone();
  } // end method */

  /**
   * Return number of unused bits in last octet of BITSTRING.
   *
   * <p>See <a href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   * 8.6.2.2.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return number of unused bits in the last octet, an integer from range [0, 7]
   */
  public int getNumberOfUnusedBits() {
    return insNumberOfUnusedBits;
  } // end method */

  /**
   * Converts given {@code octets} to bit-string.
   *
   * <p>Every bit in the bit-string is converted to either '0' or '1'. Only bits from the bit-string
   * are taken into account. In particular: No unused bits are taken into account.
   *
   * <p>For easy orientation a single space is inserted between eight bits.
   *
   * @param noUnusedBits number of unused bits
   * @param octets with bit-string
   * @return {@link String} with '0', '1' and spaces representing the bit-string
   */
  /* package */
  static String toBitString(final int noUnusedBits, final byte[] octets) {
    // --- calculate number of used bits
    int noUsedBits = (octets.length << 3) - noUnusedBits;
    if (noUsedBits <= 0) {
      // ... no used bits
      //     => empty bit-string
      return "";
    } // end fi
    // ... at least one bit is used

    // --- convert octets to BigInteger and remove unused bits from the end of the bit-string
    final BigInteger number = new BigInteger(1, octets).shiftRight(noUnusedBits);

    // --- extract bits
    final StringBuilder result = new StringBuilder();

    int counter = 0;
    while (noUsedBits > 0) {
      if (0 == (counter++ % 8)) { // NOPMD assignment in operand
        result.append(' ');
      } // end fi
      result.append(number.testBit(--noUsedBits) ? '1' : '0');
    } // end While (...)

    return result.toString().trim();
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
   * @param noIndentation indicates how often the indent parameter {@code delo} is used before
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

    if (addComment && (0 == getNumberOfUnusedBits())) {
      // ... comment requested  AND  no-unused-bits
      // Note 1: The requested comment for "this" instance is already added by
      //         the superclass. Here, we try to add a comment for the bit-string,
      //         in case that bit-string forms a TLV-object.
      // Note 2: If numberOfUnusedBits is zero, then here we try to decode that
      //         bits-string as a constructed TLV-object.
      result.append(commentDecoded(noIndentation, getDecoded()));
    } // end fi

    return result.toString();
  } // end method */
} // end class
