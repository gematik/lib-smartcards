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
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class for primitive BER-TLV objects according to <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a>.
 *
 * <p>Known subclasses:<br>
 * {@link PrimitiveSpecific}.
 *
 * <p>From the perspective of this class
 *
 * <ol>
 *   <li>instances are immutable value-types. Thus, {@link Object#equals(Object) equals()}, {@link
 *       Object#hashCode() hashCode()} are overwritten. {@link Object#clone() clone()} method is not
 *       overwritten, because instances are immutable.
 *   <li>where data is passed in or out, defensive cloning is performed.
 *   <li>methods are thread-safe.
 * </ol>
 *
 * <p>It follows that from the perspective of this class object sharing is possible without side
 * effects.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "CT_CONSTRUCTOR_THROW"
//         Short message: Classes that throw exceptions in their constructors
//             are vulnerable to Finalizer attacks.
//         That finding is not correct, because an empty finalize() declared
//             "final" is present in superclass.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "CT_CONSTRUCTOR_THROW" // see note 1
}) // */
public class PrimitiveBerTlv extends BerTlv {

  /** Error message in case of constructed tag. */
  @VisibleForTesting
  /* package */ static final String EMC = "tag-field indicates constructed encoding"; // */

  /** Error message in case of End-Of-Stream. */
  @VisibleForTesting
  /* package */ static final String EM_EOS = "unexpected end of stream while reading a value-field";

  /** Error message in case of indefinite form. */
  @VisibleForTesting
  /* package */ static final String EM_INDEFINITE = "indefinite form for length-field not allowed";

  /** Error message in case value-field is too long. */
  @VisibleForTesting /* package */ static final String EM_TOO_LONG = "length too big"; // */

  /** Value-field. */
  /* package */ final byte[] insValueField; // */

  /**
   * Constructor reading length- and value-field from a {@link ByteBuffer}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If the {@code buffer} {@link ByteBuffer#hasRemaining()} then at least one octet is
   *       read which changes the {@code position} of {@code buffer}.</i>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param tag the tag-field
   * @param buffer form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Integer#MAX_VALUE}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>tag is not in accordance to <a
   *           href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause
   *           8.1.2
   *       <li>bit b6 of the leading octet in the tag-field does not indicate "primitive" encoding
   *       <li>the length-field indicates the indefinite form, see <a
   *           href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a>
   *           8.1.3.6
   *     </ol>
   */
  /* package */ PrimitiveBerTlv(final byte[] tag, final ByteBuffer buffer) {
    // CT_CONSTRUCTOR_THROW
    // --- read length-field from inputStream (there defensive cloning is used)
    super(tag, buffer);
    checkTag();
    // ... value of tag is okay

    // --- check the length of value-field
    if (insIndefiniteForm) {
      // ... indefinite form, which is invalid for primitive BER-TLV objects
      throw new IllegalArgumentException(EM_INDEFINITE);
    } else if (insLengthOfValueFieldFromStream > Integer.MAX_VALUE) {
      // ... length too big
      // Note 1: According to the current java specification, a byte[]-array
      //         can hold as much as Integer.MAX_VALUE elements.
      //         Thus, an int is sufficient hereafter.
      throw new ArithmeticException(EM_TOO_LONG);
    } // end fi
    final int length = (int) insLengthOfValueFieldFromStream;
    // ... 0 <= length <= Integer.MAX_VALUE

    // --- read (i.e. copy) value-field from inputStream
    insValueField = new byte[length]; // allocate memory for value-field
    buffer.get(insValueField);

    // --- set instance attributes
    // set insLengthOfValueField
    insLengthOfValueField = insLengthOfValueFieldFromStream;
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
   * @param tag the tag-field
   * @param inputStream form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Integer#MAX_VALUE}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>tag is not in accordance to <a
   *           href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause
   *           8.1.2
   *       <li>bit b6 of the leading octet in the tag-field does not indicate "primitive" encoding
   *       <li>the length-field indicates the indefinite form, see <a
   *           href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a>
   *           8.1.3.6
   *     </ol>
   *
   * @throws IOException if underlying methods do so
   */
  /* package */ PrimitiveBerTlv(final byte[] tag, final InputStream inputStream)
      throws IOException {
    // CT_CONSTRUCTOR_THROW
    // --- read length-field from inputStream (there defensive cloning is used)
    super(tag, inputStream);
    checkTag();
    // ... value of tag is okay

    // --- check the length of value-field
    if (insIndefiniteForm) {
      // ... indefinite form, which is invalid for primitive BER-TLV objects
      throw new IllegalArgumentException(EM_INDEFINITE);
    } else if (insLengthOfValueFieldFromStream > Integer.MAX_VALUE) {
      // ... length too big
      // Note 1: According to the current java specification, a byte[]-array
      //         can hold as much as Integer.MAX_VALUE elements.
      //         Thus, an int is sufficient hereafter.
      throw new ArithmeticException(EM_TOO_LONG);
    } // end fi
    final int length = (int) insLengthOfValueFieldFromStream;
    // ... 0 <= length <= Integer.MAX_VALUE

    // --- read (i.e. copy) value-field from inputStream
    insValueField = inputStream.readNBytes(length);
    if (length > insValueField.length) {
      // ... end of stream
      throw new EOFException(EM_EOS);
    } // end fi

    // --- set instance attributes
    insLengthOfValueField = insLengthOfValueFieldFromStream;
  } // end constructor */

  /**
   * Tag-Value-Constructor.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive or
   *       defensively cloned.</i>
   * </ol>
   *
   * @param tag contains the tag of the new object
   * @param valueField contains the value-field, defensive-cloning is used
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>the tag is not in accordance to <a
   *           href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause
   *           8.1.2
   *       <li>bit b6 of the leading octet in the tag-field does not indicate "primitive" encoding
   *     </ol>
   */
  /* package */ PrimitiveBerTlv(final long tag, final byte[] valueField) {
    // CT_CONSTRUCTOR_THROW
    super(tag, valueField.length);
    checkTag();
    // ... value of tag is okay

    // TODO check if cloning here is really necessary
    // Note: As long as the visibility of this constructor is not "default" cloning is necessary.
    // Note: Class PrimitiveSpecific has a constructor with visibility "protected".
    //       That might be another reason why cloning here is necessary.
    insValueField = valueField.clone();
    insLengthOfValueField = insValueField.length;
  } // end method */

  /**
   * Checks whether bit b6 of leading tag-octet indicates "primitive" encoding.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here.</i>
   * </ol>
   *
   * @throws IllegalArgumentException if bit b6 of the leading octet in the tag-field does not
   *     indicate "primitive" encoding
   */
  private void checkTag() {
    if (0x00 != (insTagLengthField[0] & 0x20)) { // NOPMD avoid literals in conditional statements
      // ... bit b6 of leading octet in tag-field indicates "constructed" encoding
      throw new IllegalArgumentException(EMC);
    } // end fi
  } // end method */

  /**
   * Creates a tag from given class and number.
   *
   * <p>The implementation of this method supports only non-negative values for number. Thus, the
   * number contains at most 63 bit which are divided in no more than nine groups of seven bit.
   * Thus, the returned octet string representation of the tag-field contains at most ten octet.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable or
   *       primitive and return value is never used again by this method.</i>
   * </ol>
   *
   * @param classOfTag class of tag
   * @param number of tag
   * @return octet string representation of tag
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>value of parameter "number" is negative
   *     </ol>
   *
   * @throws ArithmeticException if the result is longer than eight octets
   */
  public static byte[] createTag(final ClassOfTag classOfTag, final long number) {
    return createTag(classOfTag, false, number);
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is primitive.</i>
   * </ol>
   *
   * @param obj object used for comparison, can be null
   * @return true if objects are equal, false otherwise
   * @see BerTlv#equals(java.lang.Object)
   */
  @Override
  public final boolean equals(final @CheckForNull Object obj) {
    // Note 1: Because this class is not a direct subclass of Object we call super.equals(...).
    //         That method already checks for reflexive, null and class.
    if (!super.equals(obj)) {
      return false;
    } // end fi
    // ... obj not same as this
    // ... obj not null
    // ... obj has same class as this
    // ... obj has same tag as this

    final PrimitiveBerTlv other = (PrimitiveBerTlv) obj;

    // --- compare primitive instance attributes (currently none)
    // -/-

    // --- compare reference types
    // ... assertion: instance attributes are never null
    return (insValueField.length == other.insValueField.length) // compare length (fast check)
        && Arrays.equals(insValueField, other.insValueField); // compare content
  } // end method */

  /**
   * Returns octet string representation of value-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is defensively cloned and
   *       never used again by this method.</i>
   * </ol>
   *
   * @return value-field as octet-string
   * @see BerTlv#getValueField()
   */
  @Override
  public final byte[] getValueField() {
    return insValueField.clone();
  } // end method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return hash-code of object
   * @see BerTlv#hashCode()
   */
  @Override
  public final int hashCode() {
    // Note 1: Because this class is not a direct subclass of object we call super.hashCode(...).
    // Note 2: Because equals() takes into account just insValue we can do here the same.
    // Note 3: Instead of simple adding the hash-codes of instance attributes here some
    //         calculation is done.
    // Note 4: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from main memory into thread local memory
    if (0 == result) {
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      result = super.hashCode(); // start value from superclass
      final int hashCodeMultiplier = 31; // small prime number

      // --- take into account primitive instance attributes (currently none)
      // -/-

      // --- take into account reference types (currently only insValue)
      result = result * hashCodeMultiplier + Arrays.hashCode(insValueField);

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Returns this BER-TLV object as an octet string.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because defensive cloning is used and return
   *       value is never used again by this method.</i>
   * </ol>
   *
   * @return octet string representation of BER-TLV object
   * @see BerTlv#getEncoded()
   */
  @Override
  public final byte[] getEncoded() {
    return AfiUtils.concatenate(insTagLengthField, insValueField);
  } // end method */

  /**
   * Appends content of this BER-TLV object to given stream.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       defensive cloning is used and return value is never used again by this method.</i>
   * </ol>
   *
   * @param out is the stream where this primitive TLV object is written to
   * @see BerTlv#getEncoded(java.io.ByteArrayOutputStream)
   */
  @Override
  @VisibleForTesting
  /* package */ final ByteArrayOutputStream getEncoded(final ByteArrayOutputStream out) {
    super.getEncoded(out) // write tag-field and length-field
        .writeBytes(insValueField); // write value-field

    return out;
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
   * @return string with characters (0-9, a-f) except for the delimiters
   * @see BerTlv#toString(String, String, int, boolean)
   */
  @Override
  /* package */ String toString(
      final String delimiter,
      final String delo,
      final int noIndentation,
      final boolean addComment) {
    final String comment;
    if (addComment) {
      // ... comment requested  AND  this provides a comment
      comment = getComment();
    } else {
      // ... comment not requested  OR  no comment available
      comment = "";
    } // end else

    return tagLength2String(delimiter, delo, noIndentation) // tag-field and length-field
        .append((0 == getLengthOfValueField()) ? "" : delimiter) // delimiter if value-field present
        .append(Hex.toHexDigits(insValueField)) // value-field
        .append(comment) // handle comment
        .toString(); // StringBuilder.toString()
  } // end method */
} // end class
