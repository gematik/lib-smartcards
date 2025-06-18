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
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class representing a TLV object of universal class with tag-number 17 = '11' i.e., SET-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause 27.
 * According to that clause, the tags in a SET-type are mutual exclusive.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class DerSet extends ConstructedBerTlv implements DerSpecific {

  /**
   * Integer representation of tag for SET.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> table 1
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 0x31; // */

  /** Findings in case TLV-object is not in accordance to its specification. */
  /* package */ final List<String> insFindings = new ArrayList<>(); // */

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
   * @throws IllegalArgumentException if tags in {@code value} are not mutual exclusive
   */
  public DerSet(final Collection<? extends BerTlv> value) {
    super(TAG, sort(value));

    // --- check if tags occur more than once, see ISO/IEC 8824-1:2021 clause 27.3.
    if (value.size() != getTemplate().size()) {
      throw new IllegalArgumentException("some tags occur more than once");
    } // end fi
  } // end constructor */

  /**
   * Constructor reading length- and value-field from {@link ByteBuffer}.
   *
   * <p>If a certain value for a tag occurs more than once in a SET-type, then only the first
   * TLV-object with that tag is taken into account, and all other TLV-objects with that tag are
   * ignored.
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
  /* package */ DerSet(final ByteBuffer buffer) {
    super(new byte[] {(byte) TAG}, buffer);

    check();
  } // end constructor */

  /**
   * Constructor reading length- and value-field from {@link InputStream}.
   *
   * <p>If a certain value for a tag occurs more than once in a SET-type, then only the first
   * TLV-object with that tag is taken into account, and all other TLV-objects with that tag are
   * ignored.
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
  /* package */ DerSet(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);

    check();
  } // end constructor */

  private void check() {
    final var sorted = sort(getTemplate());

    if (sorted.size() == getTemplate().size()) {
      if (!sorted.equals(getTemplate())) {
        insFindings.add("tags not correctly sorted");
      } // end fi
    } else {
      // ... tags not mutual exclusive
      insFindings.add("tags not mutual exclusive");
    } // end else
  } // end method */

  /**
   * Sorts the elements in given {@link Collection} in accordance to <a
   * href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC 8825-1:2021</a> clause 10.3.
   *
   * <p><i><b>Note:</b> If tags are not mutual exclusive, then only the first element with a certain
   * tag is taken into account.</i>
   *
   * @param collection of {@link BerTlv} objects
   */
  @VisibleForTesting
  /* package */ static List<BerTlv> sort(final Collection<? extends BerTlv> collection) {
    final var result = new ArrayList<BerTlv>();

    for (final var clazz : ClassOfTag.values()) {
      collection.stream()
          .filter(tlv -> clazz.equals(tlv.getClassOfTag()))
          .map(BerTlv::getTag)
          .distinct()
          .sorted()
          .forEach(
              tag ->
                  result.add(
                      collection.stream()
                          .filter(tlv -> tag == tlv.getTag())
                          .findFirst()
                          .orElseThrow())); // end forEach(tag -> ...)
    } // end For (clazz...)

    return result;
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
   * @throws IllegalArgumentException if tag of given {@code tlv}-object is already present in the
   *     SET. More precisely: If the return value of {@link #get(long)} is used in {@link
   *     java.util.Optional#isPresent()} and that operation returns {@code TRUE} then an exception
   *     is thrown.
   */
  @Override
  public DerSet add(final BerTlv tlv) {
    if (get(tlv.getTag()).isPresent()) {
      // ... TLV-object with tag already present in SET
      throw new IllegalArgumentException("tag already present");
    } // end fi

    final List<BerTlv> valueField = new ArrayList<>(insValueField);
    valueField.add(tlv);

    return new DerSet(valueField);
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

    String result =
        DELIMITER
            + "SET with "
            + noElements
            + ((1 == noElements) ? " element" : " elements"); // singular vs. plural

    if (!insFindings.isEmpty()) {
      // ... findings present

      result +=
          insFindings.stream()
              .collect(
                  Collectors.joining(
                      ", ", // delimiter
                      ", findings: ", // prefix
                      "")); // suffix
    } // end fi

    return result;
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

  /**
   * Returns whether the TLV-object is in accordance to its specification.
   *
   * @return {@code TRUE} if the TLV-object is in accordance to its specification, {@code FALSE}
   *     otherwise
   * @see DerSpecific#isValid()
   */
  @Override
  public boolean isValid() {
    return insFindings.isEmpty();
  } // end method */
} // end class
