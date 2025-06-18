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
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class representing a TLV object of universal class with tag-number 31, i.e., DATE-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause 38.4.1.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "EI_EXPOSE_REP",
//         Short message: May expose internal representation by returning reference
//                        to mutable object
//         That finding is suppressed because insDecoded is of type LocaleDate,
//         which is immutable according to JavaDoc.
// Note 2: Spotbugs claims "EI_EXPOSE_REP2",
//         Short message: May expose internal representation by incorporating
//                        reference to mutable object
//         That finding is suppressed because insDecoded is of type LocaleDate,
//         which is immutable according to JavaDoc.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP", // see note 1
  "EI_EXPOSE_REP2" // see note 2
}) // */
public final class DerDate extends PrimitiveSpecific<LocalDate> {

  /**
   * Integer representation of tag for DATE.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-X.693-201508-I/en">ISO/IEC 8824-1:2015</a>
   *       clause 38.4.1
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   *
   * <p><i><b>Note:</b> The octet string representation of a universal class tag with tag-number 31
   * is '1f1f'.</i>
   */
  public static final int TAG = 0x1f1f; // */

  /** Tag-field. */
  @VisibleForTesting
  /* package */ static final byte[] TAG_FIELD = Hex.toByteArray(String.format("%x", TAG)); // */

  /**
   * Format used for converting to and from value-field.
   *
   * <p>The format is specified in <a
   * href="https://www.itu.int/rec/T-REC-X.680-X.693-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   * 8.26.2.2, i.e. {@code YYYYMMDD}. This is identical to {@link DateTimeFormatter#BASIC_ISO_DATE}.
   */
  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.BASIC_ISO_DATE; // */

  /**
   * Character set used for converting to and from value-field.
   *
   * <p>The format is specified in <a
   * href="https://www.itu.int/rec/T-REC-X.680-X.693-201508-I/en">ISO/IEC 8825-1:2015</a> clause
   * 8.26.2.2, i.e. {@link StandardCharsets#UTF_8}.
   */
  /* package */ static final Charset CHARSET = StandardCharsets.UTF_8; // */

  /**
   * De-coded value of value-field.
   *
   * <p>Similar classes use lazy initialization for {@link DerDate#DerDate(InputStream)}.
   * Intentionally no lazy initialization occurs here, because the value extracted from an {@link
   * InputStream} is immediately checked.
   */
  /* package */ final LocalDate insDecoded; // */

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
  public DerDate(final LocalDate value) {
    // see ISO/IEC 8824-1:2015 clause 38.4.1
    // and ISO/IEC 8825-1:2015 clause 8.26.2.2
    super(TAG, value.format(FORMATTER).getBytes(CHARSET));

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
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws BufferUnderflowException if the length-field or the value-field ends early
   */
  /* package */ DerDate(final ByteBuffer buffer) {
    super(TAG_FIELD, buffer);

    // --- set instance attribute
    LocalDate date; // NOPMD could be final (false positive)
    try {
      date = LocalDate.parse(new String(insValueField, CHARSET), FORMATTER);
    } catch (DateTimeParseException e) {
      // ... invalid format
      insFindings.add("wrong format");
      date = LocalDate.now(); // will never be used, because of findings
    } // end Catch (...)

    insDecoded = date;
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
  /* package */ DerDate(final InputStream inputStream) throws IOException {
    super(TAG_FIELD, inputStream);

    // --- set instance attribute
    LocalDate date; // NOPMD could be final (false positive)
    try {
      date = LocalDate.parse(new String(insValueField, CHARSET), FORMATTER);
    } catch (DateTimeParseException e) {
      // ... invalid format
      insFindings.add("wrong format");
      date = LocalDate.now(); // will never be used, because of findings
    } // end Catch (...)

    insDecoded = date;
  } // end constructor */

  /**
   * Returns a comment describing the content of the object.
   *
   * <p>Findings are added to the comment if
   *
   * <ol>
   *   <li>value-field is not a {@link StandardCharsets#UTF_8} encoded {@link String} formatted
   *       according to {@link #FORMATTER}
   * </ol>
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@code
   *     DATE := } and the {@link String} representation of {@link #getDecoded()} value.
   */
  @Override
  public String getComment() {
    return DELIMITER
        + "DATE"
        + (isValid()
            ? (" := " + getDecoded())
            : (getFindings() + ", value-field as UTF-8: " + new String(insValueField, CHARSET)));
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
   * @return value represented by this universal-class primitive TLV object of type DATE, i.e.
   *     {@link LocalDate}
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public LocalDate getDecoded() {
    return insDecoded; // EI_EXPOSE_REP
  } // end method */
} // end class
