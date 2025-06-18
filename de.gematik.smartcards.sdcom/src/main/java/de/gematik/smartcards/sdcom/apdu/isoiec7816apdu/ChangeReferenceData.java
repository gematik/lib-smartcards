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
package de.gematik.smartcards.sdcom.apdu.isoiec7816apdu;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.utils.AfiUtils;
import java.io.Serial;

/**
 * This class covers the CHANGE REFERENCE DATA command from [gemSpec_COS#14.6.1], see also ISO/IEC
 * 7816-4:2020 clause 11.6.7.
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li><i>Instances are immutable value-types.</i>>
 *   <li><i>All constructor(s) and methods are thread-safe, because input parameter(s) are immutable
 *       or primitive.</i>
 *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
 *       return values are immutable.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "EQ_DOESNT_OVERRIDE_EQUALS".
//         Spotbugs message: his class extends a class that defines an equals
//             method and adds fields, but doesn't define an equals method itself.
//         Rational: That finding is suppressed because the implementation of
//             the equals()-method in the superclass is sufficient.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EQ_DOESNT_OVERRIDE_EQUALS" // see note 1
}) // */
public final class ChangeReferenceData extends AbstractAuthenticate {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -6645721145568248144L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0x24; // */

  /** Representation of old and new secret in {@link #toString()} method. */
  private final String insBlindedDataField; // */

  /**
   * Command APDU for setting a PIN to a new value.
   *
   * <p>Estimates parameter P2 from location and identifier. Estimates command data field from
   * format and new PIN value.
   *
   * @param loc indicates whether this PIN is global or DF-specific
   * @param id is the PIN identifier
   * @param format indicates the transport format for the PIN
   * @param newSecret the new secret itself
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; id &gt; '1f' = 31}
   *       <li>{@code 0 &gt; Nc &gt; 65535}
   *     </ol>
   */
  public ChangeReferenceData(
      final EafiLocation loc,
      final int id,
      final EafiPasswordFormat format,
      final String newSecret) {
    super(
        0x00, // CLA
        INS, // INS
        1, // P1, here set PIN
        loc,
        id, // P2 is derived from these parameters
        format.octets(newSecret)); // command data field

    // Note: here input parameters are not checked, because
    //       that is sufficiently done in superclass.
    insBlindedDataField = format.blind(newSecret);
  } // end constructor */

  /**
   * Command APDU for changing a PIN from old to new value.
   *
   * <p>Estimates parameter P2 from location and identifier. Estimates command data field from
   * format and old/new PIN value.
   *
   * @param loc indicates whether this PIN is global or DF-specific
   * @param id is the PIN identifier
   * @param format indicates the transport format for the PIN
   * @param oldSecret the present secret itself
   * @param newSecret the new secret itself
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; id &gt; '1f' = 31}
   *       <li>{@code 0 &gt; Nc &gt; 65535}
   *     </ol>
   */
  public ChangeReferenceData(
      final EafiLocation loc,
      final int id,
      final EafiPasswordFormat format,
      final String oldSecret,
      final String newSecret) {
    super(
        0x00, // CLA
        INS, // INS
        0, // P1, here change PIN
        loc,
        id, // P2 is derived from these parameters
        AfiUtils.concatenate(format.octets(oldSecret), format.octets(newSecret))); // cmdData

    insBlindedDataField = format.blind(oldSecret) + format.blind(newSecret);
  } // end constructor */

  /**
   * Converts given trailer into an explanation {@link String}.
   *
   * @param trailer to be explained
   * @return explanation for trailers
   * @see CommandApdu#explainTrailer(int)
   */
  @Override
  public String explainTrailer(final int trailer) {
    // --- [gemSpec_COS]
    if (0x6985 == trailer) { // NOPMD literal in if statement
      return "NewPasswordWrongLength, choose new password with appropriate length";
    } // end fi

    return super.explainTrailer(trailer);
  } // end method */

  /**
   * Returns blinded command data field.
   *
   * @return blinded command data field
   */
  private String getBlindedDataField() {
    return insBlindedDataField;
  } // end method */

  /**
   * Returns a {@link String} representation of this instance.
   *
   * <p>The command data field is blinded to not disclose secrets.
   *
   * @return {@link String} representation of this instance
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return toString(getBlindedDataField());
  } // end method */
} // end class
