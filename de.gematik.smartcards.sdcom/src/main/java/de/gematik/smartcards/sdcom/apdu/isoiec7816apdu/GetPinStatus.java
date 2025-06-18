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
 * This class covers the GET PIN STATUS command from [gemSpec_COS#14.6.4], see also ISO/IEC
 * 7816-4:2020 clause 11.6.6.
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
public final class GetPinStatus extends AbstractAuthenticate {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -7801801486225359234L; // */

  /** INS code according to [gemSpec_COS#(N077.900)]. */
  public static final int INS = Verify.INS; // */

  /**
   * Comfort constructor.
   *
   * @param loc indicates whether this PIN is global or DF-specific
   * @param id is the PIN identifier
   * @throws IllegalArgumentException if {@code 0 &gt; id &gt; '1f' = 31}
   */
  public GetPinStatus(final EafiLocation loc, final int id) {
    super(
        0x80, // CLA
        INS, // INS
        0x00, // P1=00 according to ISO/IEC 7816-4 tab.101
        loc, id); // P2 is derived from these parameters

    // Note: here input parameters are not checked, because
    //       that is sufficiently done in superclass.
  } // end constructor */

  /**
   * Returns explanation for given trailer.
   *
   * @param trailer to be explained
   * @return explanation for trailers
   * @see CommandApdu#explainTrailer(int)
   */
  @Override
  public String explainTrailer(final int trailer) {
    // --- [gemSpec_COS]
    if (0x63c0 == (trailer & 0xfff0)) { // NOPMD literal in if statement
      if (0x63cf == trailer) { // NOPMD literal in if statement
        return "RetryCounter: at least 15 retries";
      } else {
        return "RetryCounter: " + (trailer & 0xf) + " retries";
      } // end else
    } // end fi

    return switch (trailer) {
      case 0x62c1 -> "transportStatus = Transport-PIN";
      case 0x62c7 -> "transportStatus = Empty-PIN";
      case 0x62d0 -> "PasswordDisabled, verification not required";
      case 0x9000 -> "PasswortEnabled, security status set";
      default -> super.explainTrailer(trailer);
    }; // end Switch
  } // end method */
} // end class
