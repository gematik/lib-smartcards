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
 * This class covers the GET RANDOM command from [gemSpec_COS#14.9.5], there is no corresponding
 * command in ISO/IEC 7816-4.
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
public final class GetRandom extends CommandApdu {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 8158170345309555322L; // */

  /** INS code from Get Challenge according to ISO/IEC 7816-4. */
  public static final int INS = GetChallenge.INS;

  /**
   * Comfort constructor for retrieving a random octet string.
   *
   * @param ne indicates the number of expected bytes
   * @throws IllegalArgumentException if {@code ne} not from set {1, 2, ..., 65535, {@link
   *     CommandApdu#NE_SHORT_WILDCARD NE_SHORT_WILDCARD}, {@link CommandApdu#NE_EXTENDED_WILDCARD
   *     NE_EXTENDED_WILDCARD}}.
   */
  public GetRandom(final int ne) {
    super(
        0x80, // CLA
        INS, // INS
        0x00, // P1, fixed value
        0x00, // P2, fixed value
        ne); // Le-field

    // Note: here input parameters are not checked, because
    //       that is sufficiently done in superclass.
  } // end constructor */
} // end class
