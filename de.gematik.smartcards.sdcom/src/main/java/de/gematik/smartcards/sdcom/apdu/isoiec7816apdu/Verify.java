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
import java.io.Serial;

/**
 * This class covers the VERIFY command from [gemSpec_COS#14.6.6], see also ISO/IEC 7816-4:2020
 * clause 11.6.6.
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
public final class Verify extends AbstractAuthenticate {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -7365257928927288238L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0x20; // */

  /** Representation of command data field in {@link #toString()} method. */
  private final String insBlindedDataField; // */

  /**
   * Comfort constructor.
   *
   * @param loc indicates whether this password-object is global or DF-specific
   * @param id password identifier
   * @param format indicates the transport format for the password
   * @param secret the secret itself
   */
  public Verify(
      final EafiLocation loc, final int id, final EafiPasswordFormat format, final String secret) {
    super(
        0x00, // CLA
        INS, // INS
        0x00, // P1=00 according to ISO/IEC 7816-4:2020 tab.109
        loc,
        id, // P2 is derived from these parameters
        format.octets(secret)); // command data field

    // Note: here input parameters are not checked, because
    //       that is sufficiently done in superclass.

    insBlindedDataField = format.blind(secret);
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
      return "PasswordNotUsable, change password";
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
