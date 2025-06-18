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

import static de.gematik.smartcards.tlv.DerUtcTime.UtcTimeFormat.HH_MM_SS_Z;
import static de.gematik.smartcards.tlv.DerUtcTime.UtcTimeFormat.HH_MM_Z;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Class representing a TLV object of universal class with tag-number 23, i.e. UTCTime-type.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause 47.3.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "EI_EXPOSE_REP",
//         Short message: May expose internal representation by returning reference
//                        to mutable object
//         That finding is suppressed because insDecoded is of type ZonedDateTime,
//         which is immutable according to JavaDoc.
// Note 2: Spotbugs claims "SF_SWITCH_NO_DEFAULT", i.e.
//         Switch statement found where default case is missing
//         This is a false positive.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP", // see note 1
  "SF_SWITCH_NO_DEFAULT" // see note 2
}) // */
public final class DerUtcTime extends PrimitiveSpecific<ZonedDateTime> {

  /**
   * Integer representation of tag for UTCTime.
   *
   * <p>This tag-value is defined in e.g.:
   *
   * <ol>
   *   <li><a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause
   *       47.3
   *   <li><a href="https://en.wikipedia.org/wiki/X.690">Wikipedia on X.690</a>
   * </ol>
   */
  public static final int TAG = 23; // */

  /** UTC time zone. */
  /* package */ static final ZoneId UTC_TIME_ZONE = ZoneId.of("Z"); // */

  /**
   * De-coded value of value-field.
   *
   * <p>Similar classes use lazy initialization for {@link DerUtcTime#DerUtcTime(InputStream)}.
   * Intentionally no lazy initialization occurs here, because the value extracted from an {@link
   * InputStream} is immediately checked.
   */
  /* package */ final ZonedDateTime insDecoded; // */

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
   * @param formatter indicating how to convert {@code value} into value-field
   */
  public DerUtcTime(final ZonedDateTime value, final UtcTimeFormat formatter) {
    // see ISO/IEC 8824-1:2021 clause 47.3.
    super(TAG, fromDateTime(value, formatter).getBytes(StandardCharsets.US_ASCII));

    // --- set instance attribute
    insDecoded = (ZonedDateTime) fromValueField().orElseThrow();
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
  /* package */ DerUtcTime(final ByteBuffer buffer) {
    super(new byte[] {(byte) TAG}, buffer);

    // --- set instance attribute
    final Optional<?> dateTime = fromValueField();

    if (dateTime.isEmpty()) {
      // ... unknown format in value-field
      insFindings.add("wrong format");
      insDecoded = ZonedDateTime.now(); // will never be used, because of findings
    } else {
      // ... known format
      insDecoded = (ZonedDateTime) dateTime.get();
    } // end fi
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
  /* package */ DerUtcTime(final InputStream inputStream) throws IOException {
    super(new byte[] {(byte) TAG}, inputStream);

    // --- set instance attribute
    final Optional<?> dateTime = fromValueField();

    if (dateTime.isEmpty()) {
      // ... unknown format in value-field
      insFindings.add("wrong format");
      insDecoded = ZonedDateTime.now(); // will never be used, because of findings
    } else {
      // ... known format
      insDecoded = (ZonedDateTime) dateTime.get();
    } // end fi
  } // end constructor */

  /**
   * Convert date and time to value-field.
   *
   * @param dateTime date and time to be converted
   * @param format to be used for conversion
   */
  /* package */
  static String fromDateTime(final ZonedDateTime dateTime, final UtcTimeFormat format) {
    ZonedDateTime result = dateTime;

    if (HH_MM_Z.equals(format) || HH_MM_SS_Z.equals(format)) {
      // ... UTC time zone requested
      //     => calculate instant in UTC time zone
      result = result.withZoneSameInstant(UTC_TIME_ZONE);
    } // end fi

    return result.format(format.getFormatter());
  } // end method */

  /**
   * Returns a comment describing the content of the object.
   *
   * <p>Findings are added to the comment if
   *
   * <ol>
   *   <li>value-field is not properly formatted
   * </ol>
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@code
   *     UTCTime := } and the {@link String} representation of {@link #getDecoded()} value.
   */
  @Override
  public String getComment() {
    return DELIMITER
        + "UTCTime"
        + (isValid()
            ? (" := " + getDecoded())
            : (getFindings()
                + ", value-field as UTF-8: "
                + new String(insValueField, StandardCharsets.UTF_8)));
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
   * @return value represented by this universal-class primitive TLV object of type UTCTime, i.e.
   *     {@link ZonedDateTime}
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public ZonedDateTime getDecoded() {
    return insDecoded; // EI_EXPOSE_REP
  } // end method */

  /**
   * Convert value-field of {@link DerUtcTime} to {@link ZonedDateTime}.
   *
   * @return {@link Optional#empty()} in case the value-field is not in accordance to any format
   *     from {@link UtcTimeFormat}, otherwise an {@link Optional} with appropriate {@link
   *     ZonedDateTime} object
   */
  private Optional<?> fromValueField() {
    final String input = new String(insValueField, StandardCharsets.US_ASCII);

    return Arrays.stream(UtcTimeFormat.values())
        .map(
            format -> {
              try {
                return Optional.of(ZonedDateTime.parse(input, format.getFormatter()));
              } catch (DateTimeParseException e) {
                return Optional.empty();
              } // end Catch (...)
            })
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findAny();
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /**
   * Enumeration of formats defined in <a
   * href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause 47.3 §1.
   */
  public enum UtcTimeFormat {
    /**
     * Formatter according to <a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC
     * 8824-1:2021</a> clause 47.3 §1 a, b.1, c.1.
     *
     * <p>In particular, <a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC
     * 8824-1:2021</a> clause 47.3 §1 item a, concatenated with b.1 and c.1, i.e.:
     *
     * <ol>
     *   <li>§1 a: the six-digit YYMMDD where YY is the two low-order digits of the Christian year,
     *       MM is the month (counting January as 01), and DD is the day of the month (01 to 31).
     *       <br>
     *       <i><b>Note:</b> In a {@link DateTimeFormatter} this is encoded as "yyMMdd".</i>
     *   <li>§1 b.1: the four-digit hhmm where hh is hour (00 to 23) and mm is minutes (00 to 59).
     *       <br>
     *       <i><b>Note:</b> In a {@link DateTimeFormatter} this is encoded as "HHmm".</i>
     *   <li>§1 c.1: the character Z.<br>
     *       <i><b>Note:</b> In {@link DateTimeFormatter} the {@link ZoneId} for UTC is encoded as
     *       "VV" (yes two characters).</i>
     * </ol>
     */
    HH_MM_Z("yyMMddHHmmVV"), // */

    /**
     * Formatter according to <a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC
     * 8824-1:2021</a> clause 47.3 §1 a, b.2, c.1.
     *
     * <p>In particular, <a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC
     * 8824-1:2021</a> clause 47.3 §1 item a, concatenated with b.2 and c.1, i.e.:
     *
     * <ol>
     *   <li>§1 a: the six-digit YYMMDD where YY is the two low-order digits of the Christian year,
     *       MM is the month (counting January as 01), and DD is the day of the month (01 to 31).
     *       <br>
     *       <i><b>Note:</b> In a {@link DateTimeFormatter} this is encoded as "yyMMdd".</i>
     *   <li>§1 b.2: the four-digits hhmmss where hh is hour (00 to 23) and mm is minutes (00 to 59)
     *       and ss is seconds (00 to 59).<br>
     *       <i><b>Note:</b> In a {@link DateTimeFormatter} this is encoded as "HHmmss".</i>
     *   <li>§1 c.1: the character Z.<br>
     *       <i><b>Note:</b> In {@link DateTimeFormatter} the {@link ZoneId} for UTC is encoded as
     *       "VV" (yes two characters).</i>
     * </ol>
     */
    HH_MM_SS_Z("yyMMddHHmmssVV"), // */

    /**
     * Formatter according to <a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC
     * 8824-1:2021</a> clause 47.3 §1 a, b.1, c.2.
     *
     * <p>In particular, <a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC
     * 8824-1:2021</a> clause 47.3 §1 item a, concatenated with b.1 and c.2, i.e.:
     *
     * <ol>
     *   <li>§1 a: the six-digits YYMMDD where YY is the two low-order digits of the Christian year,
     *       MM is the month (counting January as 01), and DD is the day of the month (01 to 31).
     *       <br>
     *       <i><b>Note:</b> In a {@link DateTimeFormatter} this is encoded as "yyMMdd".</i>
     *   <li>§1 b.1: the four-digits hhmm where hh is hour (00 to 23) and mm is minutes (00 to 59).
     *       <br>
     *       <i><b>Note:</b> In a {@link DateTimeFormatter} this is encoded as "HHmm".</i>
     *   <li>§1 c.2: one of the characters + or -, followed by hhmm, where hh is hour and mm is
     *       minutes.<br>
     *       <i><b>Note:</b> In {@link DateTimeFormatter} the differential time is encoded as "xx"
     *       (yes two characters).</i>
     * </ol>
     */
    HH_MM_DIFF("yyMMddHHmmxx"), // */

    /**
     * Formatter according to <a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC
     * 8824-1:2021</a> clause 47.3 §1 a, b.2, c.2.
     *
     * <p>In particular, <a href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC
     * 8824-1:2021</a> clause 47.3 §1 item a, concatenated with b.1 and c.2, i.e.:
     *
     * <ol>
     *   <li>§1 a: the six-digits YYMMDD where YY is the two low-order digits of the Christian year,
     *       MM is the month (counting January as 01), and DD is the day of the month (01 to 31).
     *       <br>
     *       <i><b>Note:</b> In a {@link DateTimeFormatter} this is encoded as "yyMMdd".</i>
     *   <li>§1 b.2: the four-digits hhmmss where hh is hour (00 to 23) and mm is minutes (00 to 59)
     *       and ss is seconds (00 to 59).<br>
     *       <i><b>Note:</b> In a {@link DateTimeFormatter} this is encoded as "HHmmss".</i>
     *   <li>§1 c.2: one of the characters + or -, followed by hhmm, where hh is hour and mm is
     *       minutes.<br>
     *       <i><b>Note:</b> In {@link DateTimeFormatter} the differential time is encoded as "xx"
     *       (yes two characters).</i>
     * </ol>
     */
    HH_MM_SS_DIFF("yyMMddHHmmssxx"); // */

    /**
     * Formatter.
     *
     * <p><i><b>Note:</b> An enum is {@link Serializable}. but a {@link DateTimeFormatter} is not.
     * If we define instance "insFormatter" as a {@link DateTimeFormatter} then intelligent IDE etc.
     * claim a "non-serializable instance attribute in a serializable class". Here that would work,
     * because enum is handled differently, see <a
     * href="http://docs.oracle.com/javase/1.5.0/docs/guide/serialization/spec/serial-arch.html#enum">JavaDoc</a>.
     * to avoid such (false positive) findings we define the instance attribute "insFormatter" as a
     * {@link String}.</i>
     */
    private final String insFormatter;

    /**
     * Comfort constructor.
     *
     * @param format specifying the format
     */
    UtcTimeFormat(final String format) {
      insFormatter = format;
    } // end constructor */

    /**
     * Returns appropriate formatter.
     *
     * @return {@link DateTimeFormatter}
     */
    public DateTimeFormatter getFormatter() {
      return DateTimeFormatter.ofPattern(insFormatter);
    } // end method */
  } // end enumeration
} // end class
