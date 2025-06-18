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

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;

import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Base class for BER-TLV objects according to <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a>.
 *
 * <p>Known subclasses:<br>
 * {@link ConstructedBerTlv}, {@link PrimitiveBerTlv}.
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
//             "final" is present in this class.
// Note 2: Spotbugs claims "DCN_NULLPOINTER_EXCEPTION"
//         Short message: Do not catch NullPointerException.
//         Rational: Here a NullPointerException is caught to improve code coverage.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "CT_CONSTRUCTOR_THROW", // see note 1
  "DCN_NULLPOINTER_EXCEPTION", // see note 2
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.CyclomaticComplexity",
  "PMD.GodClass",
  "PMD.SingleMethodSingleton",
  "PMD.TooManyMethods",
})
public abstract class BerTlv {

  /** {@link Base64} decoder. */
  /* package */ static final Base64.Decoder BASE64_DECODER = Base64.getDecoder(); // */

  /** {@link Base64} encoder. */
  /* package */ static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder(); // */

  /**
   * Supremum number of octet in tag-field supported by this implementation.
   *
   * @see #insTag
   */
  @VisibleForTesting /* package */ static final int NO_TAG_FIELD = 8; // */

  /**
   * Flag indicating the form of the length-field.
   *
   * <p>The following applies:
   *
   * <ul>
   *   <li>If {@code TRUE} then the length-field uses the indefinite form.
   *   <li>If {@code FALSE} then the length-field uses the definite form.
   * </ul>
   */
  /* package */ final boolean insIndefiniteForm; // NOPMD no accessor */

  /**
   * Number of octets in length-field read from stream.
   *
   * <p>It is possible that this value differs from the length of a length-field if a TLV-object is
   * converted {@link #getEncoded()} because the {@code inputStream} possibly contains a
   * length-field which has NOT the minimum octet, e.g. {@code '820003'} as length-field from {@code
   * inputStream} leads to a length-field of {@code '03'} with just one octet.
   */
  /* package */ final int insLengthOfLengthFieldFromStream; // NOPMD long name */

  /** Number of octets in tag-field. */
  private final int insLengthOfTagField; // */

  /**
   * Integer representation for tag of a BER-TLV object.
   *
   * <p><i><b>Note:</b> According to <a
   * href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause 8.1.2, a
   * tag-field can contain an arbitrary number of octets. Intentionally, the number of octets for
   * the tag-field is limited within this implementation to eight octets. That seems sufficient for
   * all relevant applications.</i>
   */
  private final long insTag; // */

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally the visibility of this instance attribute is "default" (i.e., package
   *       private). Thus, subclasses in this package are able to get and set it.</i>
   *   <li><i>Because only immutable instance attributes of this class and all subclasses are taken
   *       into account for this instance attribute lazy initialization is possible.</i>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  /* package */ volatile int insHashCode; // NOPMD volatile */

  /**
   * Number of octets in value-field.
   *
   * <p>Intentionally, this instance attribute is not final and initialized to an invalid value
   * (here -1). The reason for that is as follows: Consider constructing an TLV object from a stream
   * with the following content: {@code 'a1-07-(82-820003-112233)'}. A constructor will read the
   * tag-field (i.e. 'a7') and the length-field (i.e. '07') from the stream, and afterward that
   * constructor will read the value-field (i.e., the one and only primitive TLV-object in this
   * case).
   *
   * <p>If that TLV-object is converted by {@link #getEncoded()} the result is: {@code
   * 'a1-05-(82-03-112233'}. This means the exact value for {@code insLengthOfValueField} is not
   * available before all following length- and value-fields are investigated.
   *
   * <p>Constructors of subclasses <b>SHALL</b> set this instance attribute properly, i.e., to a
   * value which is valid under the assumption that all length-fields are encoded using the minimum
   * possible number of octets.
   */
  /* package */ long insLengthOfValueField = -1; // NOPMD no accessor */

  /**
   * Number of octet in value-field read from stream.
   *
   * <p>It is possible that this value differs from the length of a value-field if a constructed
   * TLV-object is converted {@link #getEncoded()} because the {@code inputStream} possibly contains
   * a length-field which has NOT the minimum octet, e.g. if the {@code inputStream} contains {@code
   * 'a0-07-(80-820003-112233)'} then {@link #getEncoded()} returns {@code 'a0-05-(80-03-112233)'}.
   */
  /* package */ long insLengthOfValueFieldFromStream; // NOPMD long name */

  /** Concatenation of tag-field and length-field. */
  /* package */ byte[] insTagLengthField; // */

  /**
   * Constructor reading a TLV-object from a buffer.
   *
   * <p>This constructor reads tag-field and length-field from {@code byteBuffer}.
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
   * @param buffer from which just the tag is read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>tag is not in accordance to ISO/IEC 8825-1:2021
   *       <li>tag contains more than eight octets
   *     </ol>
   */
  /* package */ BerTlv(final byte[] tag, final ByteBuffer buffer) {
    super();

    checkTag(tag);
    final long[] lengthInfo = readLength(buffer);
    final long lenVfStream = lengthInfo[0];
    if (lenVfStream < 0) {
      // ... indefinite form of length-field
      insIndefiniteForm = true;
      insLengthOfValueFieldFromStream = 0;
    } else {
      // ... definite form of length-field
      insIndefiniteForm = false;
      insLengthOfValueFieldFromStream = lenVfStream;
    } // end fi
    final byte[] lengthField = Hex.toByteArray(getLengthField(insLengthOfValueFieldFromStream));

    insLengthOfLengthFieldFromStream = (int) lengthInfo[1];
    insLengthOfTagField = tag.length;
    insTag = convertTag(tag);
    insTagLengthField = AfiUtils.concatenate(tag, lengthField);
  } // end constructor */

  /**
   * Constructor reading a TLV-object from an input stream.
   *
   * <p>This constructor takes the given tag-field and reads the length-field from {@code
   * inputStream}.
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
   * @param inputStream from which just the tag is read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>tag is not in accordance to ISO/IEC 8825-1:2021
   *       <li>tag contains more than eight octets
   *     </ol>
   *
   * @throws IOException if underlying methods do so
   */
  /* package */ BerTlv(final byte[] tag, final InputStream inputStream) throws IOException {
    // CT_CONSTRUCTOR_THROW
    super();

    checkTag(tag);
    final long[] lengthInfo = readLength(inputStream);
    final long lenVfStream = lengthInfo[0];
    if (lenVfStream < 0) {
      // ... indefinite form of length-field
      insIndefiniteForm = true;
      insLengthOfValueFieldFromStream = 0;
    } else {
      // ... definite form of length-field
      insIndefiniteForm = false;
      insLengthOfValueFieldFromStream = lenVfStream;
    } // end fi
    final byte[] lengthField = Hex.toByteArray(getLengthField(insLengthOfValueFieldFromStream));

    insLengthOfLengthFieldFromStream = (int) lengthInfo[1];
    insLengthOfTagField = tag.length;
    insTag = convertTag(tag);
    insTagLengthField = AfiUtils.concatenate(tag, lengthField);
  } // end constructor */

  /**
   * Constructor with tag-field in integer representation and length of value-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive.</i>
   * </ol>
   *
   * @param tag of BER-TLV object, integer representation
   * @param lengthOfValueField number of octets in value-field
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>tag is not in accordance to ISO/IEC 8825-1:2021
   *       <li>{@code lengthOfValueField} is negative
   *     </ol>
   */
  /* package */ BerTlv(final long tag, final long lengthOfValueField) {
    // CT_CONSTRUCTOR_THROW
    super();
    final byte[] tagField = convertTag(tag);
    checkTag(tagField);
    final byte[] lengthField = Hex.toByteArray(getLengthField(lengthOfValueField));

    insLengthOfTagField = tagField.length;
    insTag = tag;
    insTagLengthField = AfiUtils.concatenate(tagField, lengthField);

    // Note 1: Instance attributes
    //         a. insDefiniteForm
    //         b. insLengthOfValueFieldFromStream
    //         are irrelevant for this constructor and thus set to zero.
    insIndefiniteForm = false;
    insLengthOfValueFieldFromStream = 0;

    // Note 2: Instance attribute insLengthOfLengthFieldFromStream
    //         is relevant for DerEndOfContent and DerNull, because one and only
    //         one instance of those classes exist, regardless whether those are
    //         created by the private default-constructor or constructed by
    //         reading octet from a stream. Thus, for those classes, it is
    //         relevant that the instance attribute insLengthOfLengthFieldFromStream
    //         is correct in cases where octets are read from an input stream.
    insLengthOfLengthFieldFromStream = 1;
  } // end constructor */

  /**
   * Pseudo constructor using {@link Base64} from {@link String} as octet string.
   *
   * <p>Converts given {@link String} by {@link Base64#getDecoder()} and then calls {@link
   * #getInstance(byte[])}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param base64 containing the data for the new object
   * @return an object of known subclass
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code base64} is not in valid Base64 scheme
   *       <li>an empty {@link String} is given
   *     </ol>
   */
  public static BerTlv base64(final String base64) {
    return getInstance(BASE64_DECODER.decode(base64));
  } // end method */

  /**
   * Calculates number of octets in the length-field for DER encoding.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
   *       return value is primitive.</i>
   * </ol>
   *
   * @param lengthOfValueField of value-field
   * @return length of length-field needed for given length of value-field
   */
  /* package */
  static int calculateLengthOfLengthField(long lengthOfValueField) {
    // Note 1: The implementation of this method supports value fields with a
    //         length of up to 2^64-1 octet = 16 EiByte. That is way more than
    //         any computer I have access to is able to store. Thus, this
    //         implementation seems ready for the future.

    if (lengthOfValueField < 0) {
      // ... lengthOfValueField is negative
      //     => treat the eight octets in lengthOfValueField as an unsigned long
      return 9;
    } else if (lengthOfValueField < 0x80L) { // NOPMD literals conditional statement
      // ... 0 <= lengthOfValueField < 0x80
      //     => lengthOfValueField contains one octet
      return 1;
    } // end fi
    // ... lengthOfValueField positive  AND  greater than or equal to 0x80 = 128

    int result = 2;
    while ((lengthOfValueField >>= 8) > 0) { // NOPMD reassigning parameters
      result++;
    } // end While (...)

    return result;
  } // end method */

  /**
   * Checks if {@code tag} conforms to <a
   * href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause 8.1.2.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read.</i>
   * </ol>
   *
   * @param tag of BER-TLV object, octet string representation
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>the given octet string is empty
   *       <li>tag is not in accordance to ISO/IEC 8825-1:2021
   *       <li>tag contains more than eight octet
   *     </ol>
   */
  @VisibleForTesting // otherwise = private
  /* package */ static void checkTag(final byte[] tag) {
    if (0 == tag.length) {
      // ... tag-field is empty
      throw new IllegalArgumentException("empty tag-field");
    } else if (1 == tag.length) { // NOPMD literals in a conditional statement
      // ... tag has only one byte, see ISO/IEC 8825-1:2021 clause 8.1.2.2
      checkTag1(tag);
    } else if (tag.length > NO_TAG_FIELD) {
      throw new IllegalArgumentException("tag too long for this implementation");
    } else {
      checkTag2345678(tag);
    } // end else
  } // end method */

  /**
   * Checks if {@code tag} consisting of one octet conforms to <a
   * href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause 8.1.2.
   *
   * <p><i><b>Notes:</b></i>
   *
   * @param tag of BER-TLV object, octet string representation
   * @throws IllegalArgumentException if tag is not in accordance to ISO/IEC 8825-1:2021
   */
  private static void checkTag1(final byte[] tag) {
    // ... tag has only one byte, see ISO/IEC 8825-1:2021 clause 8.1.2.2
    if (0x1f == (tag[0] & 0x1f)) { // NOPMD literals in a conditional statement
      throw new IllegalArgumentException(
          "No subsequent octet in tag = '" + Hex.toHexDigits(tag) + '\'');
    } // end fi
  } // end method */

  /**
   * Checks if {@code tag} consisting of more than one octet conforms to <a
   * href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause 8.1.2.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read.</i>
   * </ol>
   *
   * @param tag of BER-TLV object, octet string representation
   * @throws IllegalArgumentException if tag is not in accordance to ISO/IEC 8825-1:2021
   */
  private static void checkTag2345678(final byte[] tag) {
    // ... tag has more than one byte, see ISO/IEC 8825-1:2021 clause 8.1.2.4

    // --- check Bits b7..b1 in first subsequent octet (i.e., 2nd octet)
    // Bits b7...b1 SHALL not all be zero
    if (0 == (tag[1] & 0x7f)) {
      // ... bits b7..b1 all zero in first subsequent octet,
      //     see ISO/IEC 8825-1:2021 clause 8.1.2.4.2 item c
      throw new IllegalArgumentException(
          "bits b7..b1 all zero in first subsequent octet of tag = '"
              + Hex.toHexDigits(tag)
              + '\'');
    } // end fi

    // Two byte tags SHAlL have a number greater than or equal to 31
    if ((2 == tag.length) && ((tag[1] & 0x7f) < 31)) {
      throw new IllegalArgumentException(
          "No need for two byte tag = '" + Hex.toHexDigits(tag) + '\'');
    } // end fi

    // --- check bits b5..b1 in most significant octet
    // Note: See ISO/IEC 8825-1:2021 clause 8.1.2.4.1 item c
    if (0x1f != (tag[0] & 0x1f)) { // NOPMD literals in a conditional statement
      throw new IllegalArgumentException(
          "Leading octet wrong in tag = '" + Hex.toHexDigits(tag) + '\'');
    } // end fi

    // --- check bit b8 in subsequent octets, but not the last subsequent octet
    // Note: See ISO/IEC 8825-1:2021 clause 8.1.3.4.3 item a
    for (int k = tag.length - 1; k-- > 1; ) { // NOPMD assignment in operand
      if (tag[k] >= 0) {
        throw new IllegalArgumentException(
            "Intermediate byte has MS-bit not set in tag = '" + Hex.toHexDigits(tag) + '\'');
      } // end fi
    } // end For (k...)

    // --- check bit b8 in last subsequent octet
    // Note: See ISO/IEC 8825-1:2021 clause 8.1.3.4.3 item a
    if (tag[tag.length - 1] < 0) {
      throw new IllegalArgumentException(
          "LS-byte has MS-bit set in tag = '" + Hex.toHexDigits(tag) + '\'');
    } // end fi
  } // end method */

  /**
   * Converts octet string representation of a tag into {@code long} representation.
   *
   * @param tag of BER-TLV object, integer representation
   * @return {@code long} representation of tag
   * @throws ArithmeticException if signed number representation does not fit into {@code long}
   */
  /* package */
  static long convertTag(final byte[] tag) {
    return ((NO_TAG_FIELD == tag.length) ? new BigInteger(tag) : new BigInteger(1, tag))
        .longValueExact();
  } // end method */

  /**
   * Converts {@code long} representation of a tag into octet string representation.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method accepts any value, even such values resulting in octet-string
   *       representations which are not in conformance to <a
   *       href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *       8.1.2, e.g. '8272'.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive, and
   *       return value is never used again by this method.</i>
   * </ol>
   *
   * @param tag of BER-TLV object, integer representation
   * @return octet string representation of tag
   */
  /* package */
  static byte[] convertTag(final long tag) {
    final String hexA = Long.toHexString(tag);
    final String hexB = ((1 == (hexA.length() % 2)) ? "0" : "") + hexA;

    return Hex.toByteArray(hexB);
  } // end method */

  /**
   * Creates a tag from the given class, constructed/primitive indication and number.
   *
   * <p>The implementation of this method supports only non-negative values for number. Thus, the
   * number contains at most 63 bits which are divided in no more than nine groups of seven bits.
   * Thus, the returned octet string representation of the tag-field contains at most ten octets.
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
   * @param isConstructed {@code TRUE} for indicating constructed TLV object {@code FALSE} for
   *     indicating a primitive TLV object,
   * @param number of tag
   * @return octet string representation of tag
   * @throws IllegalArgumentException if the value of parameter "number" is negative
   * @throws ArithmeticException if the result is longer than eight octets
   */
  // Note: Intentionally, this method has a visibility of "package-private".
  //       Subclasses provide publicly available methods for creating tags.
  /* package */
  static byte[] createTag(final ClassOfTag classOfTag, final boolean isConstructed, long number) {
    final int b8b7b6 = classOfTag.getEncoding() + (isConstructed ? 0x20 : 0x00);

    if (number < 0) {
      // ... number is negative
      throw new IllegalArgumentException("number = " + number + " < 0");
    } else if (number < 0x1f) { // NOPMD literals in a conditional statement
      // ... 0 <= number < '1f'
      //     => tag-field consists of one octet
      return new byte[] {(byte) (b8b7b6 + number)};
    } else {
      // ... number >= '1f'
      //     => tag-field has more than one octet

      // --- move bits from number to an octet string in groups of seven bits,
      //     start with the least significant bits
      final List<Byte> octet = new ArrayList<>();
      for (; number > 0; number >>= 7) { // NOPMD reassigning parameters
        octet.addFirst((byte) (number & 0x7f));
      } // end For (not all bits moved)

      // --- set most significant bit in all subsequent octets but the last subsequent octet
      for (int i = octet.size() - 1; i-- > 0; ) { // NOPMD assignment in operand
        octet.set(i, (byte) (octet.get(i) | 0x80));
      } // end For (i...)

      // --- add class information as leading octet
      octet.addFirst((byte) (b8b7b6 + 0x1f));

      // --- convert ArrayList<Byte> into byte[]
      final byte[] result = new byte[octet.size()];
      for (int i = result.length; i-- > 0; ) { // NOPMD assignment in operand
        result[i] = octet.get(i);
      } // end For (i...)

      return result;
    } // end else
  } // end method */

  /**
   * Returns number of tag.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and the
   *       output parameter is primitive.</i>
   * </ol>
   *
   * @return number of tag
   */
  @VisibleForTesting
  /* package */ static long numberOfTag(final byte[] octets) {
    // Assertions:
    // ... a. tag-field is in accordance to ISO/IEC 8825-1

    final var mask = 0x1f;
    final var msByte = octets[0] & mask;
    if (mask == msByte) {
      // ... tag-field consists of more than one octet
      long result = 0;

      for (int index = 1; true; index++) {
        final var subsequentOctet = octets[index];

        result += (subsequentOctet & 0x7f);

        if (subsequentOctet >= 0) {
          break;
        } // end fi

        result <<= 7;
      } // end For (index...)

      return result;
    } else {
      // ... tag-field consists of one octet
      return msByte;
    } // end else
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable, and
   *       return value is primitive.</i>
   * </ol>
   *
   * @param obj object used for comparison, can be null
   * @return true if objects are equal, false otherwise
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final @CheckForNull Object obj) {
    // Note 1: Because this class is a direct subclass of Object calling
    //         super.equals(...) would be wrong. Instead, special checks are
    //         performed.

    // --- reflexive
    if (this == obj) {
      return true;
    } // end fi
    // ... obj not same as this

    if (null == obj) {
      // ... this differs from null
      return false;
    } // end fi
    // ... obj not null

    // Note 2: Although this is an abstract class, we check the classes of this
    //         and obj. Thus, this check isn't necessary in subclasses.
    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end fi
    // ... obj is instance of BerTlv

    final BerTlv other = (BerTlv) obj;

    // --- compare primitive instance attributes
    // --- compare reference types
    // ... assertion: instance attributes are never null
    // Note 3: Because insTagField is just a copy of insTag hereafter just
    //         insTag is compared.
    return (insTag == other.insTag);
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
   * @return hash-code of the object
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    // Note 1: Because this class is a direct subclass of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because equals() takes into account just insTag we can do here the same.
    // Note 3: Instead of simple adding the hash-codes of instance attributes here some
    //         calculation is done.
    // Note 4: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.
    // Note 5: Intentionally instance attribute insHashCode is NOT set here.
    //         That instance attribute has to be set by subclasses.

    final int hashCodeMultiplier = 31; // small prime number

    // --- take into account primitive instance attributes
    // start value, take into account insTag
    int result = (int) (insTag >> 32); // MSBits of insTag
    result = result * hashCodeMultiplier + ((int) insTag); // LSBits of insTag

    // --- take into account reference types (currently none)
    // -/-

    return result;
  } // end method */

  /**
   * Returns default comment.
   *
   * @return empty {@link String}
   */
  @SuppressWarnings({"PMD.EmptyMethodInAbstractClassShouldBeAbstract"})
  public String getComment() {
    return "";
  } // end method */

  /**
   * Returns octet string representation of value-field.
   *
   * @return value-field as octet-string
   */
  public abstract byte[] getValueField();

  /**
   * Returns class of tag.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return class of tag
   */
  public final ClassOfTag getClassOfTag() {
    final byte msByte = insTagLengthField[0];

    return ClassOfTag.getInstance(msByte);
  } // end method */

  /**
   * Returns number of tag.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return number of the tag
   */
  public long getNumberOfTag() {
    return numberOfTag(insTagLengthField);
  } // end method */

  /**
   * Converts the result of {@link #getEncoded()} into a {@link Base64} {@link String}.
   *
   * <p>Encodes the result of {@link #getEncoded()} by {@link Base64#getEncoder()}.
   *
   * <p>This is kind of the inverse operation to {@link #base64(String)}.
   *
   * @return {@link Base64} encoding of octet string representation
   */
  public final String getBase64() {
    return BASE64_ENCODER.encodeToString(getEncoded());
  } // end method */

  /**
   * Converts this BER-TLV object into an octet string.
   *
   * <p>This is kind of the inverse operation to {@link #getInstance(byte[])}.
   *
   * <p>Implementations of this method <b>SHALL</b> be thread-safe.
   *
   * <p>Implementations of this method <b>SHALL</b> be such that object sharing is not a problem.
   *
   * @return octet string representation of a BER-TLV object
   */
  public abstract byte[] getEncoded(); // */

  /**
   * Appends content of this BER-TLV object to given stream.
   *
   * <p>This method just appends the tag- and length-field to the output stream. The value-field is
   * appended by subclasses.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       return value is never used again by this method.</i>
   * </ol>
   *
   * @param out is the stream where DOs are written to
   * @return the same stream used for output
   */
  /* package */ ByteArrayOutputStream getEncoded(final ByteArrayOutputStream out) {
    out.writeBytes(insTagLengthField); // write tag-field and value-field

    return out;
  } // end method */

  /**
   * Pseudo constructor using an octet string.
   *
   * <p>The value of bit b6 in the first octet decides whether the given octets are treated as
   * {@link PrimitiveBerTlv} or {@link ConstructedBerTlv}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param octets containing the data for the new object
   * @return an object of known subclass
   * @throws IllegalArgumentException if octet string is not in accordance to ISO/IEC 8825-1:2021
   * @throws ArithmeticException if
   *     <ol>
   *       <li>a tag-field contains more than eight octets
   *       <li>a length-field indicates a length greater than {@link Long#MAX_VALUE}
   *     </ol>
   */
  @SuppressWarnings({"PMD.SingletonClassReturningNewInstance"})
  public static BerTlv getInstance(final byte[] octets) {
    // Note: To minimize memory consumption, we don't clone here.
    return getInstance(new ByteArrayInputStream(octets));
  } // end method */

  /**
   * Pseudo constructor from {@link ByteBuffer}.
   *
   * <p>The value of bit b6 in the first octet decides whether the given octets are treated as
   * {@link PrimitiveBerTlv} or {@link ConstructedBerTlv}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is not thread-safe, care must be taken to ensure that the buffer is not
   *       accessed until the operation completes.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   *   <li><i>This method does not change the buffer's {@code limit} attribute.</i>
   *   <li><i>This method does not set or change the buffer's {@code mark} attribute.</i>
   *   <li><i>In case a {@link BufferUnderflowException} occurs then the buffer's {@code position}
   *       attribute does not change.</i>
   *   <li><i>In case this method returns a {@link BerTlv} object (i.e., a {@link BerTlv} object is
   *       read from the buffer) the buffer's {@code position} attribute increases accordingly.</i>
   * </ol>
   *
   * <p>Assume the {@code buffer} is filled octet by octet and after each octet this method is
   * called. Then unless a complete {@link BerTlv} is in the {@code buffer} a {@link
   * BufferUnderflowException} is thrown. As soon as a complete {@link BerTlv} is in the {@code
   * buffer} then that {@link BerTlv} will be returned.
   *
   * @param buffer containing the data for the new object
   * @return an object of known subclass
   * @throws BufferUnderflowException if too few octets are available, in that case as soon as
   *     enough octets are added then a valid {@link BerTlv} can be retrieved from the {@code
   *     buffer}
   * @throws IllegalArgumentException if octet string is not in accordance to ISO/IEC 8825-1:2021
   * @throws ArithmeticException if
   *     <ol>
   *       <li>tag-field contains more than eight octets
   *       <li>the length-field indicates a length greater than {@link Long#MAX_VALUE}
   *     </ol>
   */
  public static BerTlv getInstance(final ByteBuffer buffer) {
    final var position = buffer.position();

    try {
      return getFromBuffer(buffer);
    } catch (BufferUnderflowException e) {
      // ... not enough octets in buffer
      //     => restore "position"
      buffer.position(position);

      throw e;
    } // end Catch (...)
  } // end method */

  /**
   * Pseudo constructor from {@link InputStream}.
   *
   * <p>The value of bit b6 in the first octet decides whether the given octets are treated as
   * {@link PrimitiveBerTlv} or {@link ConstructedBerTlv}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param inputStream containing the data for the new object
   * @return an object of known subclass
   * @throws IllegalArgumentException if octet string is not in accordance to ISO/IEC 8825-1:2021
   * @throws ArithmeticException if
   *     <ol>
   *       <li>tag-field contains more than eight octets
   *       <li>the length-field indicates a length greater than {@link Long#MAX_VALUE}
   *     </ol>
   */
  public static BerTlv getInstance(final InputStream inputStream) {
    try {
      final byte[] tagField = readTag(inputStream); // read tag-field

      return getFromInputStream(tagField, inputStream);
    } catch (IOException e) {
      throw new IllegalArgumentException("unexpected IOException", e);
    } // end Catch (...)
  } // end method */

  /**
   * Pseudo constructor using tag and octet string representation of value-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param tag contains the tag of the new object
   * @param valueField contains the value-field
   * @return an object of known subclass
   * @throws IllegalArgumentException if octet string is not in accordance to ISO/IEC 8825-1:2021
   * @throws ArithmeticException if
   *     <ol>
   *       <li>a tag-field contains more than eight octets
   *       <li>a length-field indicates a length greater than {@link Long#MAX_VALUE}
   *     </ol>
   */
  @SuppressWarnings({"PMD.SingletonClassReturningNewInstance"})
  public static BerTlv getInstance(final long tag, final byte[] valueField) {
    // Note: During JUnit tests the parameter "valueField" is set to "null".
    //       The only reason for that is to achieve a better code coverage.
    //       Setting "valueField" to "null" is a way to cause an "IOException"
    //       here and cover the corresponding catch-code.
    //       For the same reason, the following code considers "null" for
    //       that parameter.
    final var tagField = convertTag(tag);
    final var lengthField =
        Hex.toByteArray((null == valueField) ? "00" : getLengthField(valueField.length));
    final var list = new byte[][] {lengthField, valueField};
    try (var po = new PipedOutputStream();
        var pi = new PipedInputStream(po, 0x1_0000)) {

      new Thread(
              () -> {
                try {
                  for (final var octets : list) {
                    po.write(octets);
                    po.flush();
                  } // end forever-loop
                } catch (IOException | NullPointerException e) { // NOPMD empty catch block
                  // DCN_NULLPOINTER_EXCEPTION
                  // intentionally empty
                } // end Catch (...)
              })
          .start();

      if (null == valueField) {
        // Note 1: SonarQube claims the following minor code smell on the following line:
        //         "Remove this "close" call; closing the resource is handled
        //         automatically by the try-with-resources."
        // Note 2: The following line intentionally closes the stream to improve
        //         the code coverage during JUnit tests.
        pi.close();
      } // end fi

      return getFromInputStream(tagField, pi);
    } catch (IOException e) {
      throw new IllegalArgumentException(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Pseudo constructor using tag and {@code List} of TLV-objects.
   *
   * <p>If the tag indicates a primitive encoding then the value-field of that {@link
   * PrimitiveBerTlv} is the concatenation of all TLV-objects in parameter {@code valueField}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param tag contains the tag of the new object
   * @param valueField contains the value-field
   * @return an object of known subclass
   * @throws ArithmeticException if
   *     <ol>
   *       <li>tag is primitive and value-field contains more than {@link Integer#MAX_VALUE} octet
   *       <li>tag is constructed and value-field contains more than {@link Long#MAX_VALUE} octet
   *     </ol>
   */
  @SuppressWarnings({"PMD.SingletonClassReturningNewInstance"})
  public static BerTlv getInstance(final long tag, final Collection<BerTlv> valueField) {
    final byte[] tagField = convertTag(tag);
    if (0 == (tagField[0] & 0x20)) {
      // ... primitive TLV object

      // estimate length of value-field
      final int length =
          valueField.stream()
              .map(tlv -> BigInteger.valueOf(tlv.getLengthOfTlvObject()))
              .reduce(BigInteger::add)
              .orElse(BigInteger.ZERO)
              .intValueExact(); // throws ArithmeticException if sum is too large

      // allocate space for value-field
      final var baos = new ByteArrayOutputStream(length);

      // copy TLV objects from parameter valueField to value-field
      for (final var tlv : valueField) {
        tlv.getEncoded(baos);
      } // end For (tlv...)

      return getInstance(tag, baos.toByteArray());
    } // end fi
    // ... not primitive TLV object

    return switch ((int) tag) {
      case DerSequence.TAG -> new DerSequence(valueField);
      case DerSet.TAG -> new DerSet(valueField);
      default -> new ConstructedBerTlv(tag, valueField);
    }; // end Switch (tag)
  } // end method */

  /**
   * Pseudo constructor using tag and octet string representation of value-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param tag contains the tag of the new object
   * @param valueField contains the value-field
   * @return an object of known subclass
   * @throws ArithmeticException if
   *     <ol>
   *       <li>a tag-field contains more than eight octets
   *       <li>a length-field indicates a length greater than {@link Long#MAX_VALUE}
   *     </ol>
   *
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>number of hex-digits in {@code valueField} is odd
   *       <li>octet string is not in accordance to ISO/IEC 8825-1:2021
   *     </ol>
   */
  public static BerTlv getInstance(final long tag, final String valueField) {
    return getInstance(tag, Hex.toByteArray(valueField));
  } // end method */

  /**
   * Pseudo constructor using hex-digits from {@link String} as octet string.
   *
   * <p>Converts given {@link String} by {@link Hex#toByteArray(CharSequence)} and then calls {@link
   * #getInstance(byte[])}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param octets containing the data for the new object
   * @return an object of known subclass
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>number of hex-digits in {@code valueField} is odd
   *       <li>octet string is not in accordance to ISO/IEC 8825-1:2021
   *     </ol>
   *
   * @throws ArithmeticException if
   *     <ol>
   *       <li>a tag-field contains more than eight octets
   *       <li>a length-field indicates a length greater than {@link Long#MAX_VALUE}
   *     </ol>
   */
  public static BerTlv getInstance(final String octets) {
    return getInstance(Hex.toByteArray(octets));
  } // end method */

  /**
   * Pseudo constructor from {@link InputStream}.
   *
   * <p>The value of bit b6 in the first octet decides whether the given octets are treated as
   * {@link PrimitiveBerTlv} or {@link ConstructedBerTlv}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param buffer containing the data for the new object
   * @return an object of known subclass
   * @throws IllegalArgumentException if octet string is not in accordance to ISO/IEC 8825-1:2021
   * @throws ArithmeticException if
   *     <ol>
   *       <li>a tag-field contains more than eight octets
   *       <li>a length-field indicates a length greater than {@link Long#MAX_VALUE}
   *     </ol>
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.SingletonClassReturningNewInstance"})
  /* package */ static BerTlv getFromBuffer(final ByteBuffer buffer) {
    final byte[] tagField = readTag(buffer); // read tag-field

    // Note 1: Intentionally here the tag is cast into an int as a preparation
    //         for the following switch-statement. As long as that switch
    //         statement contains only cases for tags no longer than four
    //         octets that is okay.
    // Note 2: The "default" branch takes care of all other tags, even those
    //         with more than four octets.
    final int tag = (int) convertTag(tagField);
    // ... no exception thrown, i.e., tag-field short enough for this implementation

    // switch on tag to create instances of special subclasses
    return switch (tag) {
      case DerEndOfContent.TAG -> DerEndOfContent.readInstance(buffer); //           tag-number =  0
      case DerBoolean.TAG -> DerBoolean.readInstance(buffer); //                     tag-number =  1
      case DerInteger.TAG -> new DerInteger(buffer); //                              tag-number =  2
      case DerBitString.TAG -> new DerBitString(buffer); //                          tag-number =  3
      case DerOctetString.TAG -> new DerOctetString(buffer); //                      tag-number =  4
      case DerNull.TAG -> DerNull.readInstance(buffer); //                           tag-number =  5
      case DerOid.TAG -> new DerOid(buffer); //                                      tag-number =  6
      case DerUtf8String.TAG -> new DerUtf8String(buffer); //                        tag-number = 12
      case DerSequence.TAG -> new DerSequence(buffer); //                            tag-number = 16
      case DerSet.TAG -> new DerSet(buffer); //                                      tag-number = 17
      case DerPrintableString.TAG -> new DerPrintableString(buffer); //              tag-number = 19
      case DerTeletexString.TAG -> new DerTeletexString(buffer); //                  tag-number = 20
      case DerIa5String.TAG -> new DerIa5String(buffer); //                          tag-number = 22
      case DerUtcTime.TAG -> new DerUtcTime(buffer); //                              tag-number = 23
      case DerDate.TAG -> new DerDate(buffer); //                                    tag-number = 31

      default ->
          (0 == (tagField[0] & 0x20)) // ... tag-value has no specific subclass
              ? new PrimitiveBerTlv(tagField, buffer) // => create generic subclass
              : new ConstructedBerTlv(tagField, buffer);
    }; // end Switch (tag)
  } // end method */

  private static BerTlv getFromInputStream(final byte[] tagField, final InputStream inputStream)
      throws IOException {
    // Note 1: Intentionally here the tag is cast into an int as a preparation
    //         for the following switch-statement. As long as that switch
    //         statement contains only cases for tags no longer than four
    //         octets that is okay.
    // Note 2: The "default" branch takes care of all other tags, even those
    //         with more than four octets.
    final int tag = (int) convertTag(tagField);
    // ... no exception thrown, i.e., tag-field short enough for this implementation

    // switch on tag to create instances of special subclasses
    return switch (tag) {
      case DerEndOfContent.TAG -> DerEndOfContent.readInstance(inputStream); //      tag-number =  0
      case DerBoolean.TAG -> DerBoolean.readInstance(inputStream); //                tag-number =  1
      case DerInteger.TAG -> new DerInteger(inputStream); //                         tag-number =  2
      case DerBitString.TAG -> new DerBitString(inputStream); //                     tag-number =  3
      case DerOctetString.TAG -> new DerOctetString(inputStream); //                 tag-number =  4
      case DerNull.TAG -> DerNull.readInstance(inputStream); //                      tag-number =  5
      case DerOid.TAG -> new DerOid(inputStream); //                                 tag-number =  6
      case DerUtf8String.TAG -> new DerUtf8String(inputStream); //                   tag-number = 12
      case DerSequence.TAG -> new DerSequence(inputStream); //                       tag-number = 16
      case DerSet.TAG -> new DerSet(inputStream); //                                 tag-number = 17
      case DerPrintableString.TAG -> new DerPrintableString(inputStream); //         tag-number = 19
      case DerTeletexString.TAG -> new DerTeletexString(inputStream); //             tag-number = 20
      case DerIa5String.TAG -> new DerIa5String(inputStream); //                     tag-number = 22
      case DerUtcTime.TAG -> new DerUtcTime(inputStream); //                         tag-number = 23
      case DerDate.TAG -> new DerDate(inputStream); //                               tag-number = 31
      default ->
          (0 == (tagField[0] & 0x20)) // ... tag-value has no specific subclass
              ? new PrimitiveBerTlv(tagField, inputStream) // => create generic subclass
              : new ConstructedBerTlv(tagField, inputStream);
    };
  } // end method */

  /**
   * Returns the contents of the length-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return length-field as octet string
   */
  public final String getLengthField() {
    return getLengthField(getLengthOfValueField());
  } // end method */

  /**
   * Calculates the contents of the length-field from given length of value-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param lengthOfValueField of value-field in octet
   * @return length-field as octet string
   * @throws IllegalArgumentException if {@code lengthOfValueField} is negative
   */
  /* package */
  static String getLengthField(final long lengthOfValueField) {
    // Note 1: The implementation of this method supports value fields with a
    //         length of up to 2^63-1 octet = 8 EiByte. That is way more than
    //         any computer I have access to is able to store.
    //         Thus, this implementation seems ready for the future.

    if (lengthOfValueField < 0) {
      // ... lengthOfValueField is negative
      //     => throw exception
      throw new IllegalArgumentException("length of value-field SHALL NOT be negative");
    } else if (lengthOfValueField < 0x80) { // NOPMD literals in conditional statement
      // ... 0 <= lengthOfValueField < 0x80
      //     => length field consists of one octet
      return String.format("%02x", lengthOfValueField);
    } // end else if
    // ... lengthOfValueField positive  AND  greater than or equal to 0x80 = 128

    final int length = calculateLengthOfLengthField(lengthOfValueField) - 1;
    final String formatter = String.format("8%d%%0%dx", length, length << 1);

    return String.format(formatter, lengthOfValueField);
  } // end method */

  /**
   * Returns number of octets in tag-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return the number of bytes in the tag-field
   */
  public final int getLengthOfTagField() {
    return insLengthOfTagField;
  } // end method */

  /**
   * Returns number of octets in the TLV object.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return number of octet in the TLV object including tag-, length- and value-field
   */
  public final long getLengthOfTlvObject() {
    return Math.addExact(insTagLengthField.length, getLengthOfValueField());
  } // end method */

  /**
   * Return number of octet in value-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return length of the value-field
   */
  public final long getLengthOfValueField() {
    if (insLengthOfValueField < 0) {
      // ... negative values are not allowed for length of value-field
      throw new IllegalStateException(
          "instance attribute LengthOfValueField not (yet) properly initialized");
    } // end fi
    // ... instance attribute insLengthOfValueField is properly initialized

    return insLengthOfValueField;
  } // end method */

  /**
   * Returns integer representation of tag-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return integer representation of tag-field
   */
  public final long getTag() {
    return insTag;
  } // end method */

  /**
   * Returns octet string representation of tag-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return octet string representation of tag-field
   */
  public final String getTagField() {
    return Hex.toHexDigits(insTagLengthField, 0, insLengthOfTagField);
  } // end method */

  /**
   * Reads a length-field from given {@link ByteBuffer}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If the {@code buffer} {@link ByteBuffer#hasRemaining()} then at least one octet is
   *       read which changes the {@code position} of {@code buffer}.</i>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       return value is never used again by this method.</i>
   * </ol>
   *
   * @param buffer the buffer from which the length field is read
   * @return length information,
   *     <ol>
   *       <li>the first entry in {@code long[]} contains the number of octets in the value-field or
   *           -1 in case of indefinite form (see <a
   *           href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *           8.1.3.6)
   *       <li>the second entry in {@code long[]} contains the number of octets in the length-field
   *           read from the {@code inputStream}
   *     </ol>
   *
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws BufferUnderflowException if the end of the stream is reached before the whole
   *     length-field is read
   */
  /* package */
  static long[] readLength(final ByteBuffer buffer) {
    final int len = buffer.get() & 0xff; // read next octet

    if (len < 0x80) { // NOPMD literal in if statement
      // ... definite short form according to ISO/IEC 8825-1:2015 clause 8.1.3.4
      return new long[] {len, 1};
    } else if (0x80 == len) { // NOPMD literals in conditional statement
      // ... indefinite form according to ISO/IEC 8825-1:2015 clause 8.1.3.6
      return new long[] {-1, 1};
    } else {
      // ... definite long form according to ISO/IEC 8825-1:2015 clause 8.1.3.5
      //     => length-field consists of more than one octet

      // --- allocate memory space for remaining octets of length-field
      final byte[] length = new byte[len & 0x7f]; // number of subsequent octets in length-field

      // --- read subsequent octets of length field
      buffer.get(length);

      return investigateLengthField(length);
    } // end fi
  } // end method */

  /**
   * Reads a length-field from given {@link InputStream}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       return value is never used again by this method.</i>
   * </ol>
   *
   * @param inputStream the stream from which the length field is read
   * @return length information,
   *     <ol>
   *       <li>the first entry in {@code long[]} contains the number of octets in the value-field or
   *           -1 in case of indefinite form (see <a
   *           href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *           8.1.3.6)
   *       <li>the second entry in {@code long[]} contains the number of octets in the length-field
   *           read from the {@code inputStream}
   *     </ol>
   *
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws IOException if underlying methods do so
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  /* package */ static long[] readLength(final InputStream inputStream) throws IOException {
    final String message = "unexpected end of stream while reading a tag";

    final int len = inputStream.read(); // read next octet

    if (len < 0) {
      // ... reached EndOfStream
      throw new EOFException(message);
    } else if (len < 0x80) { // NOPMD literal in if statement
      // ... definite short form according to ISO/IEC 8825-1:2015 clause 8.1.3.4
      return new long[] {len, 1};
    } else if (0x80 == len) { // NOPMD literals in conditional statement
      // ... indefinite form according to ISO/IEC 8825-1:2015 clause 8.1.3.6
      return new long[] {-1, 1};
    } else {
      // ... definite long form according to ISO/IEC 8825-1:2015 clause 8.1.3.5
      //     => length-field consists of more than one octet

      // --- allocate memory space for remaining octets of length-field
      final byte[] length = new byte[len & 0x7f]; // number of subsequent octets in length-field

      // --- read subsequent octets of length field
      final var noRead = inputStream.readNBytes(length, 0, length.length);
      if (noRead < length.length) {
        throw new EOFException(message);
      } // end fi

      return investigateLengthField(length);
    } // end fi
  } // end method */

  private static long[] investigateLengthField(final byte[] length) {
    // --- convert octet string representation of length-field into long
    final BigInteger result = new BigInteger(1, length);

    // --- check the length of the value-field
    if (result.bitLength() > 63) { // NOPMD literal in if statement
      throw new ArithmeticException(
          "length of value-field too big for this implementation: '" + result.toString(16) + "'");
    } // end fi

    return new long[] {result.longValueExact(), 1 + length.length};
  } // end method */

  /**
   * Reads a tag-field from given stream.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If the {@code buffer} {@link ByteBuffer#hasRemaining()} then at least one octet is
   *       read which changes the {@code position} of {@code buffer}.</i>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       return value is never used again by this method.</i>
   * </ol>
   *
   * @param buffer from which the tag is read
   * @return tag-field read from stream
   * @throws BufferUnderflowException if the buffer limit is reached before reading the whole tag
   */
  /* package */
  static byte[] readTag(final ByteBuffer buffer) {
    byte octet;

    // --- read first (and possibly only) byte of tag
    octet = buffer.get();

    final List<Byte> tag = new ArrayList<>();
    tag.add(octet);

    if (0x1f == (octet & 0x1f)) { // NOPMD literals in conditional statement
      // ... tag has more than one octet

      // --- read remaining octets of tag-field
      do {
        octet = buffer.get();
        tag.add(octet);
      } while ((octet & 0xff) >= 0x80);
    } // end fi (more than one byte in tag)

    // --- convert ArrayList<Byte> into byte[]
    final byte[] result = new byte[tag.size()];
    for (int i = result.length; i-- > 0; ) { // NOPMD assignment in operand
      result[i] = tag.get(i);
    } // end For (i...)

    return result;
  } // end method */

  /**
   * Reads a tag-field from given stream.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of the
   *       input parameter(s) while this method is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       return value is never used again by this method.</i>
   * </ol>
   *
   * @param inputStream from which the tag is read
   * @return tag-field read from stream
   * @throws IOException if underlying methods do so
   */
  /* package */
  static byte[] readTag(final InputStream inputStream) throws IOException {
    final String message = "unexpected end of stream while reading a tag";

    int octet;

    // --- read first (and possibly only) byte of tag
    octet = inputStream.read();
    if (octet < 0) {
      // ... reached EndOfStream
      throw new EOFException(message);
    } // end fi

    final List<Integer> tag = new ArrayList<>();
    tag.add(octet);

    if (0x1f == (octet & 0x1f)) { // NOPMD literals in a conditional statement
      // ... tag has more than one octet

      // --- read remaining octets of tag-field
      do {
        octet = inputStream.read();
        if (octet < 0) {
          throw new EOFException(message);
        } // end fi
        tag.add(octet);
      } while (octet >= 0x80);
    } // end fi (more than one byte in tag)

    // --- convert List<Integer> into byte[]
    final byte[] result = new byte[tag.size()];
    int index = 0;
    for (final var element : tag) {
      result[index++] = element.byteValue();
    } // end For (element...)

    return result;
  } // end method */

  /**
   * Returns {@link StringBuilder} containing tag-field and length-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable or
   *       primitive and return value is never used again by this method.</i>
   * </ol>
   *
   * @param delimiter used between tag- and value-field
   * @param delo contains a delimiter to be used between subsequent TLV-objects in the value-field.
   *     <ul>
   *       <li>if {@code delo} is an empty string no indentation is used
   *       <li>if {@code delo} equals "\n" then the indentations are numbered with a non-hexadecimal
   *           digit plus " ".
   *       <li>if {@code delo} is not an empty string it is used noI-times.
   *     </ul>
   *
   * @param noI indicates how often the indent parameter n is used before printing the tag
   * @return d || d || ... || d || tag-field || d || length-field
   */
  /* package */
  final StringBuilder tagLength2String(final String delimiter, final String delo, final int noI) {
    final StringBuilder result = indentation(delo, noI);

    // --- add tag-field, delimiter and length-field
    return result
        .append(getTagField()) // tag-field
        .append(delimiter) // delimiter
        .append(getLengthField()); // length-field
  } // end method */

  /**
   * Returns {@link StringBuilder} containing appropriate indentation.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable or
   *       primitive and return value is never used again by this method.</i>
   * </ol>
   *
   * @param delo contains a delimiter to be used between subsequent TLV-objects in the value-field.
   *     <ul>
   *       <li>if {@code delo} is an empty string no indentation is used
   *       <li>if {@code delo} equals "\n" then the indentations are numbered with a non-hexadecimal
   *           digit plus " ".
   *       <li>if {@code delo} is not an empty string it is used noI-times.
   *     </ul>
   *
   * @param noI indicates how often the indent parameter n is used before printing the tag
   * @return appropriate indentation
   */
  /* package */ StringBuilder indentation(final String delo, final int noI) {
    final StringBuilder result = new StringBuilder();

    // --- indentation
    if ("\n".equals(delo)) { // NOPMD literals in conditional statement
      final char numerator = (char) ('f' + noI);
      for (int j = noI; j-- > 0; ) { // NOPMD assignment in operand
        result.append(numerator).append("  ");
      } // end For (j...)
    } else if (!"".equals(delo)) { // NOPMD literals in conditional statement
      for (int j = noI; j-- > 0; ) { // NOPMD assignment in operand
        result.append(delo);
      } // end For (j...)
    } // end fi

    return result;
  } // end method */

  /**
   * Converts an object to an octet string representation.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return octet string with characters (0..9, a..f)
   * @see Object#toString()
   */
  @Override
  public final String toString() {
    return Hex.toHexDigits(getEncoded());
  } // end method */

  /**
   * Converts an object to a printable hexadecimal string.
   *
   * <p>Call {@code toString(d,"",0)}, see {@link #toString(String, String, int, boolean)}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param delimiter to be used between
   *     <ul>
   *       <li>tag-field and length-field
   *       <li>length-field and value-field (if present)
   *       <li>twice used to separate different TLV-objects in the value-field in case of {@link
   *           ConstructedBerTlv}
   *     </ul>
   *
   * @return string with characters (0..9, a..f) except for the parameter d
   */
  public final String toString(final String delimiter) {
    return toString(delimiter, "", 0, false);
  } // end method */

  /**
   * Converts object to a printable hexadecimal string with given delimiters.
   *
   * <p>Implementations of this method <b>SHALL</b> be thread-safe.
   *
   * <p>Implementations of this method <b>SHALL</b> be such that object sharing is not a problem.
   *
   * @param delimiter to be used between
   *     <ul>
   *       <li>tag- and length-Field
   *       <li>length- and value-field (if present)
   *     </ul>
   *
   * @param delo contains a delimiter to be used between subsequent TLV-objects in the value-field
   *     of {@link ConstructedBerTlv}.
   *     <ul>
   *       <li>if {@code delo} is an empty string the TLV-objects of the value-field are separated
   *           by twice the parameter {@code delimiter}
   *       <li>if {@code delo} equals "\n" then the indentations are numbered with a non-hexadecimal
   *           digit plus " ".
   *       <li>if {@code delo} differs from "\n" each TLV-object of the value-field starts on a
   *           separate line. The ASCII character '\n'=0x0a is used as line separator. Furthermore,
   *           the parameter {@code delo} is used for indentation.
   *     </ul>
   *
   * @param noIndentation indicates how often the indent parameter {@code delo} is used before
   *     printing the tag
   * @param addComment flag indicates whether a comment is added to the output ({@code TRUE}) or not
   *     ({@code FALSE})
   * @return string with characters (0-9, a-f) except for the delimiters
   */
  /* package */
  abstract String toString(
      String delimiter, String delo, int noIndentation, boolean addComment); // */

  /**
   * Converts an object to a printable hexadecimal string.
   *
   * <p>In principle, the output is identical to
   *
   * <ol>
   *   <li>{@code toString(" ")} for {@link PrimitiveBerTlv} and
   *   <li>{@code toString(" ", "| ")} for {@link ConstructedBerTlv}
   * </ol>
   *
   * <p>The difference to such a call is as follows: If an instance implements {@link DerSpecific},
   * then {@link DerSpecific#getComment()} is added to the output.
   *
   * <p>EXAMPLES
   *
   * <ol>
   *   <li>{@link DerEndOfContent}<br>
   *       {@code "00 00"} versus {@code "00 00 # EndOfContent"}
   *   <li>{@link DerBoolean}<br>
   *       {@code "01 00 00"} versus {@code "01 00 00 # BOOLEAN: FALSE"}
   * </ol>
   *
   * @return string with characters (0-9, a-f) except for the delimiters and comments
   */
  public final String toStringTree() {
    return toString(" ", "|  ", 0, true);
  } // end method */

  /**
   * Empty finalizer method prevents finalizer attacks.
   *
   * <p>For more information, see <a
   * href="https://wiki.sei.cmu.edu/confluence/display/java/OBJ11-J.+Be+wary+of+letting+constructors+throw+exceptions">SEI
   * Cert Rule Obj-11</a>.
   *
   * @deprecated in {@link java.lang.Object}
   */
  @Deprecated(forRemoval = true)
  @Override
  @SuppressWarnings({
    "PMD.EmptyFinalizer",
    "PMD.EmptyMethodInAbstractClassShouldBeAbstract",
    "checkstyle:nofinalizer"
  })
  protected final void finalize() {
    // Note 1: SonarQube claims the following critical code smell on this method:
    //         "Do not override the Object.finalize() method."
    // Note 2: As stated in the Java-Doc comment to this method is overwritten
    //         to prevent finalizer attacks.
    // intentionally empty
  } // end method */
} // end class
