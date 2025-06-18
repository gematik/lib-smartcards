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
import de.gematik.smartcards.utils.Hex;
import java.io.Serial;

/**
 * This class covers the UPDATE RECORD command from [gemSpec_COS#14.4.8], see also ISO/IEC
 * 7816-4:2013 clause 11.4.5.
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li><i>Instances are immutable value-types.</i>>
 *   <li><i>No constructor is thread-safe, because content possibly changes after calling the
 *       constructor but before defensive cloning is performed.</i>
 *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable or
 *       defensively cloned and return values are immutable.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class UpdateRecord extends AbstractRecord {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 8701322324201030006L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0xdc; // */

  /**
   * Comfort-Constructor for replacing a record with record number in P1.
   *
   * @param recordNumber number of records to replace
   * @param sfi short file identifier, if zero then current EF is affected
   * @param newRecordContent new record content
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code recordNumber} is not in range [{@link #RECORD_NUMBER_ABSENT}, {@link
   *           #RECORD_NUMBER_SUPREMUM}]
   *       <li>{@code sfi} is not in range [{@link CommandApdu#SFI_ABSENT}, {@link
   *           CommandApdu#SFI_SUPREMUM}]
   *       <li>{@code Nc} is not in range [0, 65535]
   *     </ol>
   */
  public UpdateRecord(final int recordNumber, final int sfi, final byte[] newRecordContent) {
    super(
        0x00, // CLA
        INS, // INS
        recordNumber, // P1 = recordNumber
        sfi, // short file identifier
        0x04, // addressing is record number in P1
        newRecordContent); // command data field

    // Note 1: Checks are intentionally performed after super-constructor is called,
    //         because calling super-constructor has to be the first action.

    // Note 2: Here is just one additional check for input parameters, because
    //         other checks are sufficiently done in superclass.
    if (RECORD_NUMBER_ABSENT == recordNumber) {
      throw new IllegalArgumentException("Invalid recordNumber: " + recordNumber);
    } // end fi
  } // end constructor */

  /**
   * Comfort-Constructor for replacing a record with record number in P1.
   *
   * @param recordNumber number of records to replace
   * @param sfi short file identifier, if zero then current EF is affected
   * @param newRecordContent new record content
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code recordNumber} is not in range [{@link #RECORD_NUMBER_ABSENT}, {@link
   *           #RECORD_NUMBER_SUPREMUM}]
   *       <li>{@code sfi} is not in range [{@link CommandApdu#SFI_ABSENT}, {@link
   *           CommandApdu#SFI_SUPREMUM}]
   *       <li>{@code Nc} is not in range [0, 65535]
   *     </ol>
   */
  public UpdateRecord(final int recordNumber, final int sfi, final String newRecordContent) {
    this(recordNumber, sfi, Hex.toByteArray(newRecordContent));
  } // end constructor */
} // end class
