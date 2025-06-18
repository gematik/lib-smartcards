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
package de.gematik.smartcards.sdcom.apdu;

import de.gematik.smartcards.sdcom.Message;
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;

/**
 * A response APDU following the structure defined in ISO/IEC 7816-3:2006, clause 12.1.
 *
 * <p>The functionality provided here is a superset of {@code javax.smartcardio.ResponseAPDU}. Thus,
 * there is no need to use the final class {@code javax.smartcardio.ResponseAPDU}.
 *
 * <p>A {@link ResponseApdu} consists of body (possibly empty) and a two byte trailer. This class
 * does not attempt to verify that the APDU encodes a semantically valid response.
 *
 * <p>From the perspective of this class
 *
 * <ol>
 *   <li>Instances are immutable value-types. Thus, {@link Object#equals(Object) equals()} and
 *       {@link Object#hashCode() hashCode()} are overwritten. {@link Object#clone() clone()} is not
 *       overwritten.
 *   <li>Where data is passed in or out, defensive cloning is performed.
 *   <li>Methods are thread-safe.
 * </ol>
 *
 * <p>It follows that from the perspective of this class object sharing is possible without side
 * effects.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public class ResponseApdu implements Apdu, Response, Serializable {

  /** Automatically generated UID. */
  @java.io.Serial private static final long serialVersionUID = 6537870055900805797L; // */

  /** Special value indicating that during comparison any data matches. */
  protected static final byte[] WILDCARD_DATA = new byte[0]; // */

  /** Special value indicating that during comparison any trailer matches. */
  // Note: This value is chosen such that it is next to the supremum of valid
  //       values and all 16 least significant bit are '0'. This way if
  //       WILDCARD_TRAILER is used for masking differences trailers do not
  //       count during comparison.
  public static final int WILDCARD_TRAILER = 0x1_0000; // */

  /** Optional response data field. */
  private final byte[] insData; // */

  /**
   * Flag indicating if data field is representing {@code WILDCARD}.
   *
   * <p>This flag enables serialization and deserialization of instances without losing information
   * about wildcard status of data field.
   */
  private final boolean insWildcardData; // */

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally the visibility of this instance attribute is "protected". Thus,
   *       subclasses are able to get and set it.</i>
   *   <li><i>Because only immutable instance attributes of this class and all subclasses are taken
   *       into account for this instance attribute lazy initialization is possible.</i>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  protected volatile int insHashCode; // NOPMD volatile not recommended */

  /** Trailer = SW1-SW2 of {@link ResponseApdu}, range [0, 65,535] = ['0000', 'ffff']. */
  private final int insTrailer; // */

  /**
   * Constructs a {@link ResponseApdu} from a byte array containing the complete APDU contents
   * (optional body and trailer).
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
   * @param apdu the complete response APDU
   * @throws IllegalArgumentException if {@code length} is &le; 2
   */
  public ResponseApdu(final byte[] apdu) {
    this(apdu, 0, apdu.length);
  } // end constructor */

  /**
   * Constructs a {@link ResponseApdu} from hexadecimal digits extracted from string.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe, because input parameter(s) are immutable.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable.</i>
   * </ol>
   *
   * @param apdu the complete response APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>number of hex-digits in apdu is odd
   *       <li>apdu contains less than two octet, i.e. four hex-digits
   *     </ol>
   */
  public ResponseApdu(final String apdu) {
    this(Hex.toByteArray(apdu));
  } // end constructor */

  /**
   * Constructs a {@link ResponseApdu} from a byte array containing the complete APDU contents
   * (optional body and trailer).
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
   * @param apdu the complete response APDU
   * @param offset offset to first byte of response APDU
   * @param length number of octet in response APDU, i.e. number of octet in response data field
   *     plus two octet for trailer
   * @throws IllegalArgumentException if {@code length} is &le; 2
   * @throws IndexOutOfBoundsException if sub-range indicated by {@code offset} and {@code length}
   *     is out of bounds
   */
  public ResponseApdu(final byte[] apdu, final int offset, final int length) {
    Objects.checkFromIndexSize(offset, length, apdu.length);
    insData = Arrays.copyOfRange(apdu, offset, offset + length - 2);
    insWildcardData = false;

    final int sw1 = (apdu[offset + length - 2]) & 0xff;
    final int sw2 = (apdu[offset + length - 1]) & 0xff;

    insTrailer = (sw1 << 8) + sw2;
  } // end constructor */

  /**
   * Constructors a {@link ResponseApdu} from data field and trailer.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because the byte-array for the response data field
   *       might change after this constructor is called.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
   *       defensive cloning is used.</i>
   * </ol>
   *
   * @param data response data field with arbitrary length and content, if {@link #WILDCARD_DATA}
   *     then during comparison the response data field is treated as wildcard, i.e. any length and
   *     content is accepted during comparison
   * @param trailer trailer, only the 16 least significant bit are taken into account
   */
  public ResponseApdu(final byte[] data, final int trailer) {
    insWildcardData = WILDCARD_DATA == data;
    insData = insWildcardData ? data : data.clone();

    insTrailer = (WILDCARD_TRAILER == trailer) ? trailer : trailer & 0xffff;
  } // end constructor */

  /**
   * Estimates difference between this {@link ResponseApdu} and given one.
   *
   * <p>The result contains element
   *
   * <ol>
   *   <li>{@link EafiResponseApduDifference#CONTENT} if and only if the response data field of
   *       {@code expected} is NOT {@link #WILDCARD_DATA} and the content of the response data
   *       fields differ. More formal: There is at least one position on range [0, min(this.getNr(),
   *       expected.getNr()] where octet values differ. In other words: The presence of {@link
   *       EafiResponseApduDifference#CONTENT} does not indicate if the length of response data
   *       fields differ.
   *   <li>{@link EafiResponseApduDifference#LENGTH} if and only if the response data field of
   *       {@code expected} is not {@link #WILDCARD_DATA} and the length of the response data fields
   *       differ.
   *   <li>{@link EafiResponseApduDifference#TRAILER} if the trailer differ.
   * </ol>
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is never used again in this class.</i>
   * </ol>
   *
   * @param expected response APDU
   * @return set of {@link EafiResponseApduDifference} describing the difference between this
   *     response APDU and the expected response APDU.
   */
  @SuppressWarnings("PMD.LooseCoupling")
  public final EnumSet<EafiResponseApduDifference> difference(final ResponseApdu expected) {
    final var result = EnumSet.noneOf(EafiResponseApduDifference.class);

    // --- check trailer
    final int exTra = expected.getTrailer();
    if ((this.getTrailer() != exTra) && (exTra != WILDCARD_TRAILER)) {
      // ... difference in trailer  AND  expectedTrailer != WILDCARD_TRAILER
      result.add(EafiResponseApduDifference.TRAILER);
    } // end fi

    // --- check data
    // Note: Intentionally getData() is NOT used here because defensive cloning
    //       used in that method would increase runtime and memory footprint.
    final byte[] is = this.insData;
    final byte[] ex = expected.insData;

    if (!expected.isWildcardData()) {
      // ... expected response data field is NOT wildcard
      //     => compare response data fields

      // --- compare length of response data fields
      if (ex.length != is.length) {
        // ... response data field differ in length
        result.add(EafiResponseApduDifference.LENGTH);
      } // end fi

      // --- compare content of response data fields
      for (int i = Math.min(ex.length, is.length); i-- > 0; ) { // NOPMD assignment in operand
        if (ex[i] != is[i]) {
          // ... octet at index i differ
          result.add(EafiResponseApduDifference.CONTENT);
          break; // stop after the first difference is found
        } // end fi
      } // end For (...)
    } // end fi (ex not wildcard)

    return result;
  } // end method */

  /**
   * Estimates difference between this {@link ResponseApdu} and given one after applying a mask.
   *
   * <p>This method differs from {@link #difference(ResponseApdu)} such that only bits set to '1' in
   * mask are taken into account for comparison. In particular:
   *
   * <ol>
   *   <li>If data field of {@code expected} is {@link #WILDCARD_DATA}, then differences in data
   *       fields are ignored.
   *   <li>If trailer of {@code expected} is {@link #WILDCARD_TRAILER}, then differences in trailer
   *       are ignored.
   *   <li>If the data field of {@code mask} is shorter than the data field of {@code this} ({@code
   *       expected}), then only {@code mask.data.length} most significant octet from {@code this}
   *       ({@code expected}) are taken into account.
   * </ol>
   *
   * <p>Examples:
   *
   * <ol>
   *   <li>{@code this} = '112233 9000', {@code expected} = '11ff 9000' and {@code mask} = 'ff
   *       9000': No difference, because length of data field of {@code mask} is one octet and the
   *       first octet in {@code this} and {@code expected} are equal and trailers are also equal.
   *   <li>{@code this} = '102233 9000', {@code expected} = '11ff 9000' and {@code mask} = 'fe
   *       9000': No difference, because length of data field of {@code mask} is one octet and
   *       difference in bit b1 is masked out and trailers are also equal.
   *   <li>{@code this} and {@code expected} are arbitrary APDU and {@code mask} = '{@link
   *       #WILDCARD_DATA} {@link #WILDCARD_TRAILER}': No difference, because all possible
   *       differences are masked out.
   * </ol>
   *
   * @param expected response APDU
   * @param mask used to mask certain areas in {@code this} and {@code expected}
   * @return thisMasked.difference(expectedMasked)
   * @see ResponseApdu#difference(ResponseApdu)
   */
  @SuppressWarnings("PMD.LooseCoupling")
  public final EnumSet<EafiResponseApduDifference> difference(
      final ResponseApdu expected, final ResponseApdu mask) {
    // Note 1: Intentionally getData() is NOT used hereafter, because that
    //         method uses defensive cloning, which is NOT necessary here.
    final byte[] maData = mask.insData;
    final int maTrailer = mask.insTrailer;

    // --- calculate attributes of thisMasked
    // create data field with the appropriate length
    final byte[] thData = this.insData;
    final byte[] thisData =
        Arrays.copyOfRange(
            thData,
            0, // from
            Math.min(thData.length, maData.length) // to
            );

    // mask bits in data field
    for (int i = thisData.length; i-- > 0; ) { // NOPMD assignment in operand
      thisData[i] &= maData[i];
    } // end For (i...)

    final int thisTrailer = this.insTrailer & maTrailer & 0xffff;
    final ResponseApdu thisMasked = new ResponseApdu(thisData, thisTrailer);

    // --- calculate attributes of expectedMasked
    final byte[] exData = expected.insData;
    final byte[] expectedData;
    if (expected.isWildcardData()) {
      // ... Wildcard for expected data
      expectedData = WILDCARD_DATA;
    } else {
      // ... not Wildcard for expected data
      // create data field with the appropriate length

      expectedData =
          Arrays.copyOfRange(
              exData,
              0, // from
              Math.min(exData.length, maData.length) // to
              );

      // mask bits in data field
      for (int i = expectedData.length; i-- > 0; ) { // NOPMD assignment in operand
        expectedData[i] &= maData[i];
      } // end For (i...)
    } // end else

    final int exTrailer = expected.insTrailer;
    final int expectedTrailer =
        (WILDCARD_TRAILER == exTrailer) ? WILDCARD_TRAILER : exTrailer & maTrailer & 0xffff;
    final ResponseApdu expectedMasked = new ResponseApdu(expectedData, expectedTrailer);

    return thisMasked.difference(expectedMasked);
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) of correct class are
   *       immutable and return value is primitive.</i>
   *   <li><i>Two {@link ResponseApdu} are not equal if only one of them have a data field
   *       representing {@link #WILDCARD_DATA}.</i>
   *   <li><i>Two {@link ResponseApdu} are not equal if only one of them have a trailer representing
   *       {@link #WILDCARD_TRAILER}.</i>
   *   <li><i>Otherwise two {@link ResponseApdu} are equal if there data field and trailer are
   *       equal.</i>
   * </ol>
   *
   * @param obj object used for comparison, can be null
   * @return true if objects are equal, false otherwise
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public final boolean equals(final @CheckForNull Object obj) {
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

    // Note 2: Although this is possibly a superclass we check the classes of
    //         this and obj. Thus, this check isn't necessary in subclasses.
    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end fi
    // ... obj is instance of ResponseApdu

    final ResponseApdu other = (ResponseApdu) obj;

    // --- compare instance attributes
    // ... assertion: instance attributes are never null
    // Note 3: Intentionally here we start with attributes which more likely differ.
    return (this.isWildcardData() == other.isWildcardData())
        && Arrays.equals(this.insData, other.insData)
        && (this.insTrailer == other.insTrailer);
  } // end method */

  /**
   * Return octet string representation of {@link ResponseApdu}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is never used again by this
   *       class.</i>
   *   <li><i>If data field represents {@link #WILDCARD_DATA}, then this information gets lost by
   *       this transformation.</i>
   *   <li><i>If trailer represents {@link #WILDCARD_TRAILER}, then this information gets lost by
   *       this transformation.</i>
   * </ol>
   *
   * @return octet string representation of {@link ResponseApdu} according to ISO/IEC 7816-3.
   * @see Message#getBytes()
   */
  @Override
  public final byte[] getBytes() {
    final byte[] result = Arrays.copyOf(insData, insData.length + 2); // copy response data field

    result[result.length - 2] = (byte) getSw1(); // copy SW1
    result[result.length - 1] = (byte) getSw2(); // copy SW2

    return result;
  } // end method */

  /**
   * Return the response data field of this response APDU.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe, because the byte-array is immutable.</i>
   *   <li><i>Object sharing is not a problem here, defensive cloning is used with the exception of
   *       {@link #WILDCARD_DATA}.</i>
   *   <li><i>If the data field represents {@link #WILDCARD_DATA}, then the method also returns
   *       {@link #WILDCARD_DATA} without cloning that octet string.</i>
   * </ol>
   *
   * @return response data field
   * @see Response#getData()
   */
  @Override
  public final byte[] getData() {
    return isWildcardData() ? WILDCARD_DATA : insData.clone();
  } // end method */

  /**
   * Return number of octet in response data field.
   *
   * <p>If the response data field is absent (or empty) then 0 is returned.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return the number of octet in the data field of the response or 0 if this response has no data
   * @see Response#getNr()
   */
  @Override
  public final int getNr() {
    return insData.length;
  } // end method */

  /**
   * Returns the value of the status bytes SW1 and SW2 as a single status word SW.
   *
   * <p>Status word {@code SW} is defined as {@code (getSW1() &lt;&lt; 8) | getSW2()}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return the value of the status word SW in range ['0000', 'ffff']
   */
  public final int getSw() {
    return insTrailer & 0xffff;
  } // end method */

  /**
   * Return status byte SW1.
   *
   * <p>The status byte SW1 is the most significant octet of the status word SW, see {@link
   * #getSw()}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return the value of the status byte SW1 in range ['00', 'ff']
   */
  public final int getSw1() {
    return (insTrailer >> 8) & 0xff;
  } // end method */

  /**
   * Return status byte SW2.
   *
   * <p>The status byte SW2 is the least significant octet of the status word SW, see {@link
   * #getSw()}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return the value of the status byte SW2 in range ['00', 'ff']
   */
  public final int getSw2() {
    return insTrailer & 0xff;
  } // end method */

  /**
   * Return trailer of this {@link ResponseApdu}.
   *
   * <p>If trailer has value {@link #WILDCARD_TRAILER}, then that value is returned.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return trailer, i.e. status of command, in range ['0000', 'ffff'] or {@link
   *     #WILDCARD_TRAILER}.
   * @see Response#getTrailer()
   */
  @Override
  public final int getTrailer() {
    return insTrailer;
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
   * @see java.lang.Object#hashCode()
   * @see java.lang.Object#hashCode()
   */
  @Override
  public final int hashCode() {
    // Note 1: Because this class is a direct subclass of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because equals() takes into account CLA, INS, P1, P2, data and
    //         Le-field, here we can do the same.
    // Note 3: Instead of simple adding the hash-codes of instance attributes
    //         here some calculation is done.
    // Note 4: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from the main memory into thread local memory
    if (0 == result) {
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion 1: CLA, INS, P1 and P2 are one byte octet
      // ... assertion 2: instance attributes are never null

      final int hashCodeMultiplier = 31; // small prime number

      // --- take into account primitive instance attributes
      // take into account command header
      result = insTrailer;

      // --- take into account reference types (currently only insData)
      // Note 5: Intentionally insData is taken into account, because often
      //         that is the only attribute that differs.
      result = result * hashCodeMultiplier + Arrays.hashCode(insData);

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Returns flag indicating if data field represents {@code Wildcard}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return {@code TRUE} if data field represents {@code Wildcard}, {@code FALSE} otherwise
   */
  protected boolean isWildcardData() {
    return insWildcardData;
  } // end method */

  /**
   * Returns {@link String} representation of this {@link ResponseApdu}.
   *
   * <p>In general the format of the returned {@link String} is
   *
   * <pre>SW1SW2='9000'  Nr='0010'  Data='000102030405060708090a0b0c0d0e0f'</pre>
   *
   * <p>If the trailer represents {@link #WILDCARD_TRAILER}, then the status word is encoded as
   * {@code SW1SW2='****'}.
   *
   * <p>If the data field represents {@link #WILDCARD_DATA}, then the number of octet in the data
   * field (i.e. Nr) and the data field itself are encoded as {@code Data='*'}
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return string representation in human-readable form, e.g. "SW1SW2='9000' Nr='0003'
   *     Data='112233'"
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    // Note: Intentionally, this method does not use getData(), because
    //       there cloning is performed which is not necessary here.

    final var trailer = getTrailer();
    final var trailerString =
        (WILDCARD_TRAILER == trailer) ? "SW1SW2='****'" : String.format("SW1SW2='%04x'", trailer);
    final var nr = getNr();
    final String dataString;
    if (0 == nr) {
      if (isWildcardData()) {
        dataString = "  Data='*'";
      } else {
        dataString = "  Data absent";
      } // end else
    } else {
      dataString = String.format("  Nr='%04x'  Data='%s'", nr, Hex.toHexDigits(insData));
    } // end else

    return trailerString + dataString;
  } // end method */
} // end class
