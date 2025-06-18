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

import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CPI;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.Hex;
import java.util.Collections;
import java.util.List;

/**
 * Certificate profile indicator, CPI.
 *
 * <p>From the perspective of this class instances are immutable value-types. It follows that from
 * the perspective of this class object sharing is possible without side effects.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class CertificateProfileIndicator extends CvcComponent {

  /** Flag indicating critical findings. */
  private final boolean insCriticalFindings; // NOPMD no getter */

  /** Explanation. */
  private final List<String> insExplanation; // */

  /** Findings. */
  private final List<String> insReport; // */

  /**
   * Default constructor.
   *
   * <p>This constructor uses '70' as CPI.
   */
  public CertificateProfileIndicator() {
    this((PrimitiveBerTlv) BerTlv.getInstance(TAG_CPI, "70"));
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param tlv primitive data object containing CPI
   */
  /* package */ CertificateProfileIndicator(final PrimitiveBerTlv tlv) {
    super(tlv);

    final byte[] value = getValue();
    final StringBuilder explanation =
        new StringBuilder(256)
            .append(
                String.format(
                    "Certificate Profile Indicator     CPI = %s", Hex.toHexDigits(value)));

    if ((1 == value.length) && (0x70 == value[0])) {
      // ... CPI is according to [gemSpec_PKI#GS-A_4987]

      explanation.append(" => self descriptive Card Verifiable Certificate");
      insCriticalFindings = false;
      insReport = Collections.emptyList();
    } else {
      // ... CPI is NOT in accordance to [gemSpec_PKI#GS-A_4987]

      insCriticalFindings = true;
      insReport = List.of("CPI value is unknown");
    } // end else

    insExplanation = List.of(explanation.toString());
  } // end constructor */

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
