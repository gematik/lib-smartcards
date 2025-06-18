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
package de.gematik.smartcards.g2icc.cvc;

import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CED;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CXD;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.Hex;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Date information inside a card-verifiable certificate.
 *
 * <p>The date information is in the form 'yymmdd', i.e. year, month and day of month are encoded in
 * two decimal digits. For the encoded form each digit is encoded as uncompressed BCD. E.g. 30
 * December 2045 = '451230' = '040501020300'.
 *
 * <p>From the perspective of this class instances are immutable value-types. It follows that from
 * the perspective of this class object sharing is possible without side effects.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class CertificateDate extends CvcComponent {

  /** Default date in case of invalid input. */
  /* package */ static final LocalDate DEFAULT_DATE = LocalDate.of(2000, 1, 1); // */

  /**
   * Format for date.
   *
   * <p>Day with two digits, non-abbreviated month, year with four digits, German style.
   */
  /* package */ static final DateTimeFormatter FORMAT_DE =
      DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMANY); // */

  /** Flag indicating critical findings. */
  private final boolean insCriticalFindings; // NOPMD no getter */

  /** Explanation. */
  private final List<String> insExplanation; // */

  /** Findings. */
  private final List<String> insReport; // */

  /** Date. */
  private final LocalDate insDate; // */

  /**
   * Constructor using a {@link LocalDate}.
   *
   * @param isCed type of date, if {@code TRUE} then {@code CertificateEffectiveDate}, otherwise
   *     {@code CertificateExpirationDate}
   * @param date year, month and day of month
   */
  public CertificateDate(final boolean isCed, final LocalDate date) {
    this(
        (PrimitiveBerTlv)
            BerTlv.getInstance(
                isCed ? TAG_CED : TAG_CXD,
                String.format(
                    "%02d%02d%02d%02d%02d%02d",
                    (date.getYear() % 100) / 10,
                    date.getYear() % 10,
                    date.getMonthValue() / 10,
                    date.getMonthValue() % 10,
                    date.getDayOfMonth() / 10,
                    date.getDayOfMonth() % 10)));
  } // end constructor */

  /** Comfort constructor. */
  /* package */ CertificateDate(final PrimitiveBerTlv tlv) {
    super(tlv);

    final String prefix =
        (tlv.getTag() == TAG_CED)
            ? "Certificate Effective  Date       CED"
            : "Certificate Expiration Date       CXD";
    boolean critical = true;
    final byte[] value = getValue();
    final String hexDigits = Hex.toHexDigits(value);
    final StringBuilder stringBuilder =
        new StringBuilder(String.format("%s = %-16s", prefix, hexDigits));

    final List<String> report = new ArrayList<>();
    LocalDate localDate = DEFAULT_DATE;
    if (6 == value.length) { // NOPMD literal in if statement
      // ... date has correct length

      // --- roughly check digits in date
      boolean digitCheck = true;

      // define first two digits of year (i.e. century)
      int digits = CertificationAuthorityReference.OFFSET_YEAR / 100;

      for (final byte b : value) {
        final int digit = b & 0xff;
        digitCheck &= (digit <= 9);
        digits = 10 * digits + digit;
      } // end For (i...)

      if (digitCheck) {
        // ... digits in value-field are all in range [0, 9]

        try {
          localDate = LocalDate.of(digits / 10_000, (digits / 100) % 100, digits % 100);
          critical = false;

          stringBuilder.append(" => ").append(localDate.format(FORMAT_DE));
        } catch (DateTimeException e) {
          // ... date is not normalized
          report.add(prefix + " is not normalized");
        } // end Catch (...)
      } else {
        // ... at least one digit in date is not in range [0, 9]
        // Note: Intentionally this is not an ERROR situation.
        report.add(prefix + " has invalid digits");
      } // end else (digits in range [0, 9]?)
    } else {
      // ... date has wrong length
      report.add(prefix + " has an invalid length");
    } // end else (correct length?)

    insCriticalFindings = critical;
    insExplanation = List.of(stringBuilder.toString());
    insReport = Collections.unmodifiableList(report);
    insDate = localDate;
  } // end constructor */

  /**
   * Return date.
   *
   * @return date
   */
  public LocalDate getDate() {
    return insDate;
  } // end method */

  /**
   * Explanation of the content of this component.
   *
   * @return {@link List} of {@link String} with explanation
   */
  /* package */
  @Override
  List<String> getExplanation() {
    return insExplanation;
  } // end method */

  /**
   * Return a human-readable form of cardholder reference.
   *
   * @return human-readable form of cardholder reference
   */
  /* package */ String getHumanReadable() {
    return hasCriticalFindings() ? Hex.toHexDigits(getValue()) : getDate().format(FORMAT_DE);
  } // end method */

  /**
   * Report of findings in this component.
   *
   * <p>A finding which is reported is something unexpected, possibly in or not in accordance to
   * gematik specifications.
   *
   * @return {@link List} of {@link String} with findings
   */
  /* package */
  @Override
  List<String> getReport() {
    return insReport;
  } // end default method */

  /**
   * Signals critical findings.
   *
   * @return {@code TRUE} if the component is not in conformance to gemSpec_PKI, {@code FALSE}
   *     otherwise
   */
  /* package */
  @Override
  boolean hasCriticalFindings() {
    return insCriticalFindings;
  } // end method */
} // end class
