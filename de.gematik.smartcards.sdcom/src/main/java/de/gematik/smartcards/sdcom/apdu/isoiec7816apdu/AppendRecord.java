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
 * This class covers the APPEND RECORD command from [gemSpec_COS#14.4.2}, see also ISO/IEC
 * 7816-4:2013 clause 11.4.6.
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
public final class AppendRecord extends AbstractRecord {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 4986925147059422231L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0xe2; // */

  /**
   * Comfort-Constructor for appending a record to a structured EF.
   *
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param recordContent content of record to be appended
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code sfi} is not in range [0, 30]
   *       <li>{@code Nc} is not in range [0, 65535]
   *     </ol>
   */
  public AppendRecord(final int sfi, final byte[] recordContent) {
    super(
        0x00, // CLA
        INS, // INS
        0, // P1=0 according to ISO/IEC 7816-4:2020 table 77
        sfi, // short file identifier
        0, // b3b2b1=0 according to ISO/IEC 7816-4:2020 table 77
        recordContent); // command data field

    // Note: Here input parameters are not checked, because
    //       that is sufficiently done in superclass.
  } // end constructor */

  /**
   * Comfort-Constructor for appending a record to a structured EF.
   *
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param recordContent content of record to be appended
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code sfi} is not in range [0, 30]
   *       <li>{@code Nc} is not in range [0, 65535]
   *     </ol>
   */
  public AppendRecord(final int sfi, final String recordContent) {
    this(sfi, Hex.toByteArray(recordContent));
  } // end constructor */

  /**
   * Converts given trailer into an explanation {@link String}.
   *
   * @return explanation for certain trailers
   * @see CommandApdu#explainTrailer(int)
   */
  @Override
  public String explainTrailer(final int trailer) {
    // --- [gemSpec_COS]
    if (trailer == 0x6a84) { // NOPMD literal in if statement
      return "FullRecordList/OutOfMemory";
    } // end fi

    return super.explainTrailer(trailer);
  } // end method */
} // end class
