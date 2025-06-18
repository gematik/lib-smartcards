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

import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CHR;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Cardholder reference (CHR).
 *
 * <p>This class handles key references with lengths of eight (key reference of a certification
 * authority (CA)) and twelve (key reference of end-entity).
 *
 * <p>From the perspective of this class instances are immutable value-types. It follows that from
 * the perspective of this class object sharing is possible without side effects.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public class CardHolderReference extends CvcComponent {

  /** Flag indicating critical findings. */
  private final boolean insCriticalFindings; // NOPMD no getter */

  /** Explanation. */
  private final List<String> insExplanation; // */

  /** Findings. */
  private final List<String> insReport; // */

  /** Human-readable form of cardholder reference. */
  private final String insHumanReadable; // */

  /**
   * Constructor using cardholder reference (CHR).
   *
   * @param chr cardholder reference
   */
  public CardHolderReference(final String chr) {
    // Note 1: SonarQube claims the following finding (bug):
    //         "Add a way to break out of this recursive method."
    // Note 2: This is a false positive. Here no recursion occurs, because this constructor calls
    //         another constructor in this class and that constructor calls a constructor in
    //         superclass.
    this((PrimitiveBerTlv) BerTlv.getInstance(TAG_CHR, chr));
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param tlv primitive data object containing CPI
   */
  /* package */ CardHolderReference(final PrimitiveBerTlv tlv) {
    this(tlv, "Certificate Holder Reference      CHR = ");
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param tlv primitive data object containing CPI
   * @param prefix of explanation
   */
  /* package */ CardHolderReference(final PrimitiveBerTlv tlv, final String prefix) {
    super(tlv);

    final List<Boolean> critical = new ArrayList<>();
    final List<String> explanation = new ArrayList<>();
    final List<String> report = new ArrayList<>();
    final byte[] value = getValue();
    final String hexDigits = Hex.toHexDigits(value);

    explanation.add(String.format("%s%s", prefix, hexDigits));

    switch (value.length) { // NOPMD no default (false positive)
      case 8 -> // ... CHR of a CA, i.e. Sub-CA or Root-CA
          insHumanReadable = explainCar(value, critical, explanation, report);

      case 12 -> // ... CHR of an end-entity certificate
          insHumanReadable = explainChr(value, explanation, report);

      default -> { // ... invalid length
        critical.add(true);
        report.add("CHR has an invalid length");
        insHumanReadable = hexDigits;
      } // end default
    } // end Switch (length of value-field)

    insCriticalFindings = !critical.isEmpty();
    insExplanation = Collections.unmodifiableList(explanation);
    insReport = Collections.unmodifiableList(report);
  } // end constructor */

  /**
   * Explain content of CAR, certificate authorization reference, issuer identification number.
   *
   * <p>Assertions:
   *
   * <ol>
   *   <li>{@link #getValue()} contains at least 8 octet
   * </ol>
   *
   * @param critical list to be enlarged in case of critical findings
   * @param explanation CV-certificate explanation
   * @param report where findings are reported
   * @return CAR in human-readable form
   */
  @VisibleForTesting // otherwise = private
  /* package */ static String explainCar(
      final byte[] value,
      final List<Boolean> critical,
      final List<String> explanation,
      final List<String> report) {
    // Note: The encoding is specified in [gemSpec_PKI#GS-A_4990]
    final byte[] caId = Arrays.copyOfRange(value, 0, 5);

    // Note: The first five octets contain an ASCII encoded string.
    boolean characterCheck = true;
    for (final byte octet : caId) {
      characterCheck &=
          (('A' <= octet) && (octet <= 'Z')) // upper case letter
              || (('a' <= octet) && (octet <= 'z')); // lower case letter
    } // end For (octet...)
    final String caIdentifier = new String(caId, StandardCharsets.US_ASCII);
    if (!characterCheck) {
      critical.add(true);
      report.add("CA name contains non-alphabetic characters");
    } // end fi
    explanation.add("       CA-Identifier       = " + caIdentifier);

    // Note: The high nibble of octet 6 contains a decimal digit.
    final int serviceIndicator = (value[5] & 0xff) >> 4;
    if ((1 != serviceIndicator) && (8 != serviceIndicator)) {
      critical.add(true);
      report.add("service indicator not in set {1, 8}");
    } // end fi
    explanation.add(String.format("       service indicator   = %x", serviceIndicator));

    // Note: The low nibble of octet 6 contains a decimal digit.
    final int discretionaryDat = value[5] & 0xf;
    if (discretionaryDat > 9) { // NOPMD literal in if statement
      critical.add(true);
      report.add("discretionary data not BCD encoded");
    } // end fi
    explanation.add(String.format("       discretionary data  = %x", discretionaryDat));

    // Note: Octet 7 contains an algorithm reference, BCD encoded.
    final int algorithmReference = value[6] & 0xff;
    if (2 != algorithmReference) { // NOPMD literal in if statement
      critical.add(true);
      report.add("algorithm reference does not indicate elliptic curve");
    } // end fi
    explanation.add(String.format("       algorithm reference = %02x", algorithmReference));

    // Note: Octet 8 contains the last two digits of generation year, BCD encoded.
    final int generationYear = value[7] & 0xff;
    final int tYear = generationYear >> 4;
    final int oYear = generationYear & 0xf;
    if ((tYear > 9) || (oYear > 9)) {
      critical.add(true);
      report.add("generation year not BCD encoded");
    } // end fi
    explanation.add(String.format("       generation year     = %02x", generationYear));

    return String.format(
        "%s_%x-%x-%02x-%02x",
        caIdentifier, serviceIndicator, discretionaryDat, algorithmReference, generationYear);
  } // end method */

  /**
   * Explain content of CHR, cardholder reference.
   *
   * <p>Assertions:
   *
   * <ol>
   *   <li>{@link #getValue()} contains at least 8 octet
   * </ol>
   *
   * @param explanation CV-certificate explanation
   * @param report where findings are reported
   * @return CAR in human-readable form
   */
  @VisibleForTesting // otherwise = private
  /* package */ static String explainChr(
      final byte[] value, final List<String> explanation, final List<String> report) {
    // Note: The encoding is specified in [gemSpec_PKI#GS-A_4630]
    final String hexDigits = Hex.toHexDigits(value);
    final String discretData = Hex.toHexDigits(value, 0, 2); // arbitrary content
    final String iccsn = hexDigits.substring(4); // BCD encode

    try {
      new BigInteger(iccsn);
    } catch (NumberFormatException e) {
      // ... non-decimal digits
      // Note: Intentionally this is not a critical finding.
      report.add("ICCSN contains non-decimal digits");
    } // end Catch (...)

    final String majorInduId = iccsn.substring(0, 2);
    if (!"80".equals(majorInduId)) { // NOPMD literal in if statement
      // ... Major Industry Identifier not '80'
      // Note: Intentionally this is not a critical finding.
      report.add("Major Industry Identifier does not indicate \"health-care\"");
    } // end fi

    final String countryCode = iccsn.substring(2, 5);
    final String issuerIdent = iccsn.substring(5, 10);
    final String serialNumbe = iccsn.substring(10);

    explanation.add("       Discretionary data  = " + discretData);
    explanation.add("       ICCSN               = " + iccsn);
    explanation.add("         Major Industry ID = " + majorInduId);
    explanation.add("         Country Code      = " + countryCode);
    explanation.add("         Issuer Identifier = " + issuerIdent);
    explanation.add("         Serial Number     = " + serialNumbe);

    return String.format(
        "%s_%s-%s-%s-%s", discretData, majorInduId, countryCode, issuerIdent, serialNumbe);
  } // end method */

  /**
   * Explanation of the content of this component.
   *
   * @return {@link List} of {@link String} with explanation
   */
  /* package */
  @Override
  /* package */ final List<String> getExplanation() {
    return insExplanation;
  } // end method */

  /**
   * Return a human-readable form of cardholder reference.
   *
   * @return human-readable form of cardholder reference
   */
  /* package */
  final String getHumanReadable() {
    return insHumanReadable;
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
