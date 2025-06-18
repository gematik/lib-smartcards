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

import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CAR;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.Hex;
import java.util.List;

/**
 * Certification authority reference (CAR).
 *
 * <p>From the perspective of this class instances are immutable value-types. It follows that from
 * the perspective of this class object sharing is possible without side effects.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class CertificationAuthorityReference extends CardHolderReference {

  /** Offset for calculating year from just two digits. */
  /* package */ static final int OFFSET_YEAR = 2000; // */

  /** Flag indicating critical findings. */
  private final boolean insCriticalFindings; // NOPMD no getter */

  /** Generation year, see {@link #getGenerationYear()}. */
  private final int insGenerationYear; // */

  /** Findings. */
  private final List<String> insReport; // */

  /** Service indicator, see {@link #getServiceIndicator()}. */
  private final int insServiceIndicator; // */

  /**
   * Constructor using certification authority reference (CAR).
   *
   * @param car certification authority reference
   */
  public CertificationAuthorityReference(final String car) {
    // Note 1: SonarQube claims the following finding (bug):
    //         "Add a way to break out of this recursive method."
    // Note 2: This is a false positive. Here no recursion occurs, because this constructor calls
    //         another constructor in this class and that constructor calls a constructor in
    //         superclass.
    this((PrimitiveBerTlv) BerTlv.getInstance(TAG_CAR, car));
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param tlv primitive data object containing CPI
   */
  /* package */ CertificationAuthorityReference(final PrimitiveBerTlv tlv) {
    super(tlv, "Certification Authority Reference CAR = ");

    final byte[] value = getValue();
    final int length = value.length;

    if (8 == length) { // NOPMD literal in if statement
      // ... expected length
      insCriticalFindings = super.hasCriticalFindings();
      insReport = super.getReport();

      if (insCriticalFindings) {
        // ... critical findings
        //     => better safe than sorry and use default values
        insGenerationYear = -1;
        insServiceIndicator = -1;
      } else {
        // ... no critical findings
        //     => extract other instance attributes
        insGenerationYear = OFFSET_YEAR + Integer.parseInt(Hex.toHexDigits(value, 7, 1));
        insServiceIndicator = (value[5] & 0xff) >> 4;
      } // end else
    } else {
      // ... unexpected length
      //     => better safe than sorry and use default values
      insGenerationYear = -1;
      insServiceIndicator = -1;
      insCriticalFindings = true;
      insReport = List.of("CAR has an invalid length");
    } // end else
  } // end constructor */

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

  /**
   * Return year of generation extracted from CAR.
   *
   * <p>If {@link #hasCriticalFindings()} then {@code -1}, otherwise in range [2000, 2099].
   *
   * @return year of generation
   */
  /* package */ int getGenerationYear() {
    return insGenerationYear;
  } // end method */

  /**
   * Return service indicator extracted from CAR.
   *
   * <p>If {@link #hasCriticalFindings()} then {@code -1}, otherwise
   *
   * <ul>
   *   <li>{@code 1} indicating a CVC-Sub-CA generating End-Entity certificates,
   *   <li>{@code 8} indicating a CVC-CA generating CA-certificates.
   * </ul>
   *
   * @return service indicator
   */
  /* package */ int getServiceIndicator() {
    return insServiceIndicator;
  } // end method */
} // end class
