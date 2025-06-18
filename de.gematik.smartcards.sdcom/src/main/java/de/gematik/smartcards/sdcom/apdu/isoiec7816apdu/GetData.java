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

import java.io.Serial;

/**
 * This class covers the GET DATA command from [gemSpec_COS#14.5.1], see also ISO/IEC 7816-4:2020
 * clause 11.5.3.
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
public final class GetData extends AbstractDataObject {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 5939690953619007149L;

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0xca;

  /**
   * Comfort-Constructor for reading a value field for a given tag.
   *
   * @param tag of requested DO
   * @param ne number of expected bytes in response APDU
   */
  public GetData(final int tag, final int ne) {
    super(
        0x00, // CLA
        INS, // INS
        tag, // influences P1 and P2
        ne);
  } // end constructor */
} // end class
