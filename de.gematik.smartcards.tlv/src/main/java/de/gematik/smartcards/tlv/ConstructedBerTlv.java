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
package de.gematik.smartcards.tlv; // NOPMD high amount of different objects

import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class for constructed BER-TLV objects according to <a
 * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a>.
 *
 * <p>Known subclasses:<br>
 * {@link DerSequence}, {@link DerSet}.
 *
 * <p>From the perspective of this class
 *
 * <ol>
 *   <li>Instances are immutable value-types. Thus, {@link Object#equals(Object) equals()}, {@link
 *       Object#hashCode() hashCode()} are overwritten, but {@link Object#clone() clone()} isn't
 *       overwritten.
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
// Note 2: Spotbugs claims "EI_EXPOSE_REP",
//         Short message: May expose internal representation by returning reference
//                        to mutable object
//         That finding is suppressed because insValue is always initialized
//         with unmodifiable lists. This is checked during JUnit-tests.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "CT_CONSTRUCTOR_THROW", // see note 1
  "EI_EXPOSE_REP" // see note 2
}) // */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class ConstructedBerTlv extends BerTlv {

  /** Error message. */
  @VisibleForTesting
  /* package */ static final String EMP = "tag-field indicates primitive encoding"; // */

  /** Value-field as list of {@link BerTlv}. */
  /* package */ final List<BerTlv> insValueField; // */

  /**
   * Constructor reading length- and value-field from a {@link ByteBuffer}.
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
   * @param buffer form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>the tag is not in accordance to <a
   *           href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *           8.1.2
   *       <li>bit b6 of the leading octet in the tag-field does not indicate "constructed" encoding
   *     </ol>
   */
  /* package */ ConstructedBerTlv(final byte[] tag, final ByteBuffer buffer) {
    // CT_CONSTRUCTOR_THROW
    // --- read length-field from inputStream (there defensive cloning is used)
    super(tag, buffer);
    checkTag();
    // ... value of tag is okay

    // --- read (i.e. copy) value-field from inputStream
    final List<BerTlv> value = new ArrayList<>();
    if (insIndefiniteForm) {
      // ... indefinite form, see ISO/IEC 8825-1:2015 clause 8.1.3.6
      //     => loop until end-of-contents octets

      // Note 1: According to ISO/IEC 8815-1:2008 clause 8.1.5 the end-of-contents, is encoded
      //         as a primitive TLV-object with tag-field '00' and length-field '00'.
      for (; ; ) {
        final BerTlv tlv = getFromBuffer(buffer);
        insLengthOfValueFieldFromStream +=
            tlv.getLengthOfTagField()
                + tlv.insLengthOfLengthFieldFromStream
                + tlv.insLengthOfValueFieldFromStream;
        if (tlv instanceof DerEndOfContent) {
          // ... end-of-contents reached
          //     => break loop
          break;
        } // end fi
        // ... end-of-contents (not yet) reached

        value.add(tlv);
      } // end For (...)
    } else {
      // ... definite form, see ISO/IEC 8825-1:2015
      //     => loop until the complete value-field is read from inputStream

      long length = insLengthOfValueFieldFromStream;
      while (length > 0) {
        final BerTlv tlv = getFromBuffer(buffer);
        value.add(tlv);

        length -= // length of TLV-object from stream
            tlv.getLengthOfTagField()
                + tlv.insLengthOfLengthFieldFromStream
                + tlv.insLengthOfValueFieldFromStream;
      } // end While (length > 0)
    } // end else

    // --- set instance attributes
    // set insValue such that it is unmodifiable
    insValueField = List.copyOf(value);

    // set insLengthOfValueField
    insLengthOfValueField = insValueField.stream().mapToLong(BerTlv::getLengthOfTlvObject).sum();

    // set insTagLengthField
    final var minLengthOfLf = calculateLengthOfLengthField(insLengthOfValueField);
    final var lengthOfTagField = getLengthOfTagField();
    final var tagLengthField = new byte[lengthOfTagField + minLengthOfLf];
    System.arraycopy(insTagLengthField, 0, tagLengthField, 0, lengthOfTagField); // copy tag-field
    final var lf = Hex.toByteArray(getLengthField(insLengthOfValueField));
    System.arraycopy(lf, 0, tagLengthField, lengthOfTagField, minLengthOfLf); // copy length-field
    insTagLengthField = tagLengthField;
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
   *     Long#MAX_VALUE}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>the tag is not in accordance to <a
   *           href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *           8.1.2
   *       <li>bit b6 of the leading octet in the tag-field does not indicate "constructed" encoding
   *     </ol>
   *
   * @throws IOException if underlying methods do so
   */
  /* package */ ConstructedBerTlv(final byte[] tag, final InputStream inputStream)
      throws IOException {
    // CT_CONSTRUCTOR_THROW
    // --- read length-field from inputStream (there defensive cloning is used)
    super(tag, inputStream);
    checkTag();
    // ... value of tag is okay

    // --- read (i.e. copy) value-field from inputStream
    final List<BerTlv> value = new ArrayList<>();
    if (insIndefiniteForm) {
      // ... indefinite form, see ISO/IEC 8825-1:2015 clause 8.1.3.6
      //     => loop until end-of-contents octets

      // Note 1: According to ISO/IEC 8815-1:2008 clause 8.1.5 the end-of-contents, is encoded
      //         as a primitive TLV-object with tag-field '00' and length-field '00'.
      for (; ; ) {
        final BerTlv tlv = getInstance(inputStream);
        insLengthOfValueFieldFromStream +=
            tlv.getLengthOfTagField()
                + tlv.insLengthOfLengthFieldFromStream
                + tlv.insLengthOfValueFieldFromStream;
        if (tlv instanceof DerEndOfContent) {
          // ... end-of-contents reached
          //     => break loop
          break;
        } // end fi
        // ... end-of-contents (not yet) reached

        value.add(tlv);
      } // end For (...)
    } else {
      // ... definite form, see ISO/IEC 8825-1:2015
      //     => loop until the complete value-field is read from inputStream

      long length = insLengthOfValueFieldFromStream;
      while (length > 0) {
        final BerTlv tlv = getInstance(inputStream);
        value.add(tlv);

        length -= // length of TLV-object from stream
            tlv.getLengthOfTagField()
                + tlv.insLengthOfLengthFieldFromStream
                + tlv.insLengthOfValueFieldFromStream;
      } // end While (length > 0)
    } // end fi

    // --- set instance attributes
    // set insValue such that it is unmodifiable
    insValueField = List.copyOf(value);

    // set insLengthOfValueField
    insLengthOfValueField = insValueField.stream().mapToLong(BerTlv::getLengthOfTlvObject).sum();

    // set insTagLengthField
    final var minLengthOfLf = calculateLengthOfLengthField(insLengthOfValueField);
    final var lengthOfTagField = getLengthOfTagField();
    final var tagLengthField = new byte[lengthOfTagField + minLengthOfLf];
    System.arraycopy(insTagLengthField, 0, tagLengthField, 0, lengthOfTagField); // copy tag-field
    final var lf = Hex.toByteArray(getLengthField(insLengthOfValueField));
    System.arraycopy(lf, 0, tagLengthField, lengthOfTagField, minLengthOfLf); // copy length-field
    insTagLengthField = tagLengthField;
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
   * @param value contains the value field which is either
   *     <ol>
   *       <li>empty, i.e. the value field has no TLV object, or
   *       <li>one TLV object, or
   *       <li>concatenation of two or more TLV objects
   *     </ol>
   *
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>the tag is not in accordance to <a
   *           href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *           8.1.1
   *       <li>bit b6 of the leading octet in the tag-field does not indicate "constructed" encoding
   *     </ol>
   */
  /* package */ ConstructedBerTlv(final long tag, final byte[] value) {
    // CT_CONSTRUCTOR_THROW
    super(tag, 0); // lengthOfValueField intentionally zero here, will be corrected later
    checkTag();
    // ... value of tag is okay

    final List<BerTlv> valueField = new ArrayList<>();
    final ByteArrayInputStream bais = new ByteArrayInputStream(value);
    while (bais.available() > 0) {
      valueField.add(getInstance(bais));
    } // end While (...)

    // --- set instance attributes
    insValueField = List.copyOf(valueField); // set insValue

    // set insLengthOfValueField
    insLengthOfValueField = insValueField.stream().mapToLong(BerTlv::getLengthOfTlvObject).sum();

    // set insTagLengthField
    final var minLengthOfLf = calculateLengthOfLengthField(insLengthOfValueField);
    final var lengthOfTagField = getLengthOfTagField();
    final var tagLengthField = new byte[lengthOfTagField + minLengthOfLf];
    System.arraycopy(insTagLengthField, 0, tagLengthField, 0, lengthOfTagField); // copy tag-field
    final var lf = Hex.toByteArray(getLengthField(insLengthOfValueField));
    System.arraycopy(lf, 0, tagLengthField, lengthOfTagField, minLengthOfLf); // copy lengh-field
    insTagLengthField = tagLengthField;
  } // end method */

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
   * @param value contains the value field
   * @throws ArithmeticException if value-field contains more than {@link Long#MAX_VALUE} octet
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>the tag is not in accordance to <a
   *           href="https://www.itu.int/rec/T-REC-X.690-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   *           8.1.2
   *       <li>bit b6 of the leading octet in the tag-field does not indicate "constructed" encoding
   *     </ol>
   */
  /* package */ ConstructedBerTlv(final long tag, final Collection<? extends BerTlv> value) {
    // CT_CONSTRUCTOR_THROW
    // spotless:off
    super(
        tag,
        value.stream()
            .map(tlv -> BigInteger.valueOf(tlv.getLengthOfTlvObject()))
            .reduce(BigInteger::add)
            .orElse(BigInteger.ZERO)
            .longValueExact() // throws ArithmeticException if sum is too large
    );
    checkTag();
    // spotless:on
    // ... value of tag is okay  AND  lengthOfValueField < Long.MAX_VALUE

    // --- set instance attributes
    insValueField = List.copyOf(value);
    insLengthOfValueField = insValueField.stream().mapToLong(BerTlv::getLengthOfTlvObject).sum();
  } // end method */

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
  public ConstructedBerTlv add(final BerTlv tlv) {
    final List<BerTlv> valueField = new ArrayList<>(insValueField);
    valueField.add(tlv);

    return new ConstructedBerTlv(getTag(), valueField);
  } // end method */

  /**
   * Checks whether bit b6 of leading tag-octet indicates "constructed" encoding.
   *
   * @throws IllegalArgumentException if bit b6 of the leading octet in the tag-field does not
   *     indicate "constructed" encoding
   */
  private void checkTag() {
    if (0x20 != (insTagLengthField[0] & 0x20)) { // NOPMD literals in conditional statement
      // ... bit b6 of leading octet in tag-field indicates "primitive" encoding
      throw new IllegalArgumentException(EMP);
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
   * @throws ArithmeticException if the result would be longer than eight octet
   */
  public static byte[] createTag(final ClassOfTag classOfTag, final long number) {
    return createTag(classOfTag, true, number);
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
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

    final ConstructedBerTlv other = (ConstructedBerTlv) obj;

    // --- compare primitive instance attributes (currently none)
    // -/-

    // --- compare reference types
    // ... assertion: instance attributes are never null
    return insValueField.equals(other.insValueField);
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
      for (final BerTlv i : insValueField) {
        result = result * hashCodeMultiplier + i.hashCode();
      } // end For ( i...)

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Searches in all elements of the value-field for a TLV-object with given tag.
   *
   * @param tag which is searched for in value-field
   * @return first TLV object with given tag if search is successful, otherwise {@link
   *     Optional#empty()}
   */
  public final Optional<BerTlv> get(final long tag) {
    return get(tag, 0);
  } // end method */

  /**
   * Searches in all elements of the value-field for a TLV-object with given tag.
   *
   * @param tag which is searched for in value-field
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th TLV object with given tag if search is successful, otherwise {@link
   *     Optional#empty()}
   */
  public final Optional<BerTlv> get(final long tag, int position) {
    for (final BerTlv i : insValueField) {
      // Note: The following if-statement uses "position-- <= 0".
      //       The usage of (similar) "--position < 0" has a different behavior
      //       when this method is called with position = Integer.MIN_VAlUE.
      if ((i.getTag() == tag) && (position-- <= 0)) { // NOPMD avoid assignment in operands
        return Optional.of(i);
      } // end fi
    } // end For (i...)

    return Optional.empty();
  } // end method */

  /**
   * Searches in all elements of the value-field for a TLV-object with given tag.
   *
   * @param tag which is searched for in value-field
   * @return first TLV object with given tag if search is successful, otherwise {@link
   *     Optional#empty()}
   * @throws ClassCastException if TLV-object with given {@code tag} exists, but isn't constructed
   */
  public final Optional<ConstructedBerTlv> getConstructed(final long tag) {
    return getConstructed(tag, 0);
  } // end method */

  /**
   * Searches in all elements of the value-field for a TLV-object with given tag.
   *
   * @param tag which is searched for in value-field
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th TLV object with given tag if search is successful, otherwise {@link
   *     Optional#empty()}
   * @throws ClassCastException if TLV-object, which is found, isn't constructed
   */
  public final Optional<ConstructedBerTlv> getConstructed(final long tag, final int position) {
    return get(tag, position).map(ConstructedBerTlv.class::cast);
  } // end method */

  /**
   * Searches in all elements of the value-field for a TLV-object with given tag.
   *
   * @param tag which is searched for in value-field
   * @return first TLV object with given tag
   * @throws ClassCastException if found TLV-object isn't primitive
   */
  public final Optional<PrimitiveBerTlv> getPrimitive(final long tag) {
    return getPrimitive(tag, 0);
  } // end method */

  /**
   * Searches in all elements of the value-field for a TLV-object with given tag.
   *
   * @param tag which is searched for in value-field
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th TLV object with given tag if search is successful, otherwise {@link
   *     Optional#empty()}
   * @throws ClassCastException if found TLV-object isn't primitive
   */
  public final Optional<PrimitiveBerTlv> getPrimitive(final long tag, final int position) {
    return get(tag, position).map(PrimitiveBerTlv.class::cast);
  } // end method */

  /**
   * Returns an unmodifiable {@link List} of TLV-objects from value-field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is an unmodifiable list of
   *       immutable entries.</i>
   * </ol>
   *
   * @return list of TLV-objects from value-field
   */
  public final List<BerTlv> getTemplate() {
    return insValueField; // EI_EXPOSE_REP
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
    final ByteArrayOutputStream baos = new ByteArrayOutputStream((int) getLengthOfValueField());

    for (final BerTlv tlv : insValueField) {
      tlv.getEncoded(baos);
    } // end For (tlv...)

    return baos.toByteArray();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerBitString}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerBitString} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerBitString getBitString() {
    return getBitString(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerBitString}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerBitString} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerBitString getBitString(final int position) {
    return (DerBitString) get(DerBitString.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerBoolean}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerBoolean} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerBoolean getBoolean() {
    return getBoolean(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerBoolean}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerBoolean} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerBoolean getBoolean(final int position) {
    return (DerBoolean) get(DerBoolean.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerDate}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerDate} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerDate getDate() {
    return getDate(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerDate}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerDate} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerDate getDate(final int position) {
    return (DerDate) get(DerDate.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerEndOfContent}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerEndOfContent} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerEndOfContent getEndOfContent() {
    return getEndOfContent(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerEndOfContent}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerEndOfContent} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerEndOfContent getEndOfContent(final int position) {
    return (DerEndOfContent) get(DerEndOfContent.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerIa5String}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerIa5String} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerIa5String getIa5String() {
    return getIa5String(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerIa5String}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerIa5String} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerIa5String getIa5String(final int position) {
    return (DerIa5String) get(DerIa5String.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerInteger}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerInteger} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerInteger getInteger() {
    return getInteger(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerInteger}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerInteger} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerInteger getInteger(final int position) {
    return (DerInteger) get(DerInteger.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerNull}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerNull} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerNull getNull() {
    return getNull(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerNull}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerNull} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerNull getNull(final int position) {
    return (DerNull) get(DerNull.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerOctetString}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerOctetString} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerOctetString getOctetString() {
    return getOctetString(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerOctetString}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerOctetString} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerOctetString getOctetString(final int position) {
    return (DerOctetString) get(DerOctetString.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerOid}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerOid} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerOid getOid() {
    return getOid(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerOid}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerOid} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerOid getOid(final int position) {
    return (DerOid) get(DerOid.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerPrintableString}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerPrintableString} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerPrintableString getPrintableString() {
    return getPrintableString(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerPrintableString}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerPrintableString} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerPrintableString getPrintableString(final int position) {
    return (DerPrintableString) get(DerPrintableString.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerSequence}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerSequence} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerSequence getSequence() {
    return getSequence(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerSequence}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerSequence} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerSequence getSequence(final int position) {
    return (DerSequence) get(DerSequence.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerSet}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerSet} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerSet getSet() {
    return getSet(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerSet}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerSet} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerSet getSet(final int position) {
    return (DerSet) get(DerSet.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerTeletexString}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerTeletexString} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerTeletexString getTeletexString() {
    return getTeletexString(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerTeletexString}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerTeletexString} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerTeletexString getTeletexString(final int position) {
    return (DerTeletexString) get(DerTeletexString.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerUtcTime}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerUtcTime} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerUtcTime getUtcTime() {
    return getUtcTime(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerUtcTime}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerUtcTime} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerUtcTime getUtcTime(final int position) {
    return (DerUtcTime) get(DerUtcTime.TAG, position).orElseThrow();
  } // end method */

  /**
   * Searches in the value-field for a {@link DerUtf8String}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @return first {@link DerUtf8String} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerUtf8String getUtf8String() {
    return getUtf8String(0);
  } // end method */

  /**
   * Searches in the value-field for a {@link DerUtf8String}.
   *
   * <p><i><b>Note:</b> This is a convenience method.</i>
   *
   * @param position any number,
   *     <ol>
   *       <li>if {@code position} is zero or negative the first TLV-object with given tag is
   *           returned
   *       <li>{@code position = 1} returns the second match
   *       <li>{@code position = 2} returns the third match
   *       <li>. . .
   *     </ol>
   *
   * @return position-th {@link DerUtf8String} in the value-field
   * @throws NoSuchElementException if the value-field does not contain an appropriate data-object
   */
  public final DerUtf8String getUtf8String(final int position) {
    return (DerUtf8String) get(DerUtf8String.TAG, position).orElseThrow();
  } // end method */

  /**
   * Converts this BER-TLV object into an octet string.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because defensive cloning is used and return
   *       value is never used again by this method.</i>
   * </ol>
   *
   * @return octet string representation of a BER-TLV object
   * @see BerTlv#getEncoded()
   */
  @Override
  public final byte[] getEncoded() {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream((int) getLengthOfTlvObject());
    getEncoded(baos);

    return baos.toByteArray();
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
   * @param out the stream where this primitive TLV object is written to
   * @see BerTlv#getEncoded(java.io.ByteArrayOutputStream)
   */
  @Override
  @VisibleForTesting
  /* package */ final ByteArrayOutputStream getEncoded(final ByteArrayOutputStream out) {
    // --- write tag-field and length-field
    super.getEncoded(out);

    // --- write value-field
    for (final BerTlv i : getTemplate()) {
      i.getEncoded(out);
    } // end For (i...)

    return out;
  } // end method */

  /**
   * Converts an object to a printable hexadecimal string with given delimiter.
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
   *       <li>tag- and length-Field
   *       <li>length- and value-field (if present)
   *     </ul>
   *
   * @param delo contains a delimiter to be used between subsequent TLV-objects in the value-field
   *     of {@link ConstructedBerTlv}. If this object is {@link PrimitiveBerTlv}, then this
   *     parameter is ignored.
   * @return string with characters (0-9, a-f) except for the parameters {@code delimiter} and
   *     {@code delo}
   */
  public final String toString(final String delimiter, final String delo) {
    return toString(delimiter, delo, 0, false);
  } // end method */

  /**
   * Converts object to a printable hexadecimal string with given delimiters.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe, because input parameter(s) are immutable or primitive.
   *       </i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable or
   *       primitive and return value is immutable.</i>
   * </ol>
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
   *           separate line. The ASCII character '\n'=0x0a is used as line separator. Furthermore
   *           the parameter {@code delo} is used for indentation.
   *     </ul>
   *
   * @param noIndentation indicates how often the indent parameter {@code delo} is used before
   *     printing the tag
   * @param addComment flag indicates whether a comment is added to the output ({@code TRUE}) or not
   *     ({@code FALSE})
   * @return string with characters (0-9, a-f) except for the delimiters
   * @see BerTlv#toString(String, String, int, boolean)
   */
  @Override
  /* package */ final String toString(
      final String delimiter,
      final String delo,
      final int noIndentation,
      final boolean addComment) {
    final String tagLength = tagLength2String(delimiter, delo, noIndentation).toString();

    if (delo.isEmpty()) {
      // ... complete TLV-object in a one-line string
      // Note: delo=="" happens only for addComment=false. Thus, it is
      //       not necessary to deal with DerComment.getComment() here.
      return tagLength
          + insValueField.parallelStream()
              .map(tlv -> String.format("%s%s%s", delimiter, delimiter, tlv.toString(delimiter)))
              .collect(Collectors.joining());
    } else {
      // ... TLV-object on several lines (if value-field is present)

      final String comment;
      if (addComment) {
        // ... comment requested  AND  this provides a comment
        comment = getComment();
      } else {
        // ... comment not requested  OR  no comment available
        comment = "";
      } // end else

      return tagLength
          + comment
          + insValueField.parallelStream()
              .map(
                  tlv ->
                      String.format(
                          "%n%s", tlv.toString(delimiter, delo, noIndentation + 1, addComment)))
              .collect(Collectors.joining());
    } // end else
  } // end method */
} // end class
