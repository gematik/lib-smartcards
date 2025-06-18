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
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import java.io.Serial;

/**
 * This class covers the READ RECORD command from [gemSpec_COS#14.4.6], see also ISO/IEC 7816-4:2013
 * clause 11.4.3.
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
public final class ReadRecord extends AbstractRecord {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 2777745395153329355L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0xb2; // */

  /**
   * Comfort Constructor for reading a record with record number in P1.
   *
   * @param recordNumber number of record to read
   * @param sfi short file identifier, if zero then current EF is affected
   * @param ne maximum number of expected data bytes in a {@link ResponseApdu}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code recordNumber} is not in range [{@link #RECORD_NUMBER_INFIMUM}, {@link
   *           #RECORD_NUMBER_SUPREMUM}]
   *       <li>{@code sfi} is not in range [{@link CommandApdu#SFI_ABSENT}, {@link
   *           CommandApdu#SFI_SUPREMUM}]
   *       <li>{@code ne} not from set {1, 2, ..., 65535, {@link CommandApdu#NE_SHORT_WILDCARD
   *           NE_SHORT_WILDCARD}, {@link CommandApdu#NE_EXTENDED_WILDCARD NE_EXTENDED_WILDCARD}}.
   *     </ol>
   */
  public ReadRecord(final int recordNumber, final int sfi, final int ne) {
    super(
        0x00, // CLA-byte
        INS, // INS-code
        recordNumber, // P1 = recordNumber
        sfi, // short file identifier
        0x04, // addressing is record number in P1
        ne); // Ne

    // Note: Here input parameters are not checked, because
    //       that is sufficiently done in superclass.
  } // end constructor */
} // end class
