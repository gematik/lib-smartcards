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

import de.gematik.smartcards.utils.Hex;
import java.io.Serial;

/**
 * This class covers the PUT DATA command from [gemSpec_COS#14.5.2], see also ISO/IEC 7816-4:2020
 * clause 11.5.6.
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
public class PutData extends AbstractDataObject {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 3432549248815995895L;

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0xda;

  /**
   * Comfort-Constructor for writing a data object.
   *
   * @param tag of data object to be written
   * @param data to be written to data object
   */
  public PutData(final int tag, final byte[] data) {
    super(
        0x00, // CLA
        INS, // INS
        tag, // influences P1 and P2
        data);
  } // end constructor */

  /**
   * Comfort-Constructor for writing a data object.
   *
   * @param tag of data object to be written
   * @param data to be written to data object, only hexadecimal digits are taken into account
   */
  public PutData(final int tag, final String data) {
    this(tag, Hex.toByteArray(data));
  } // end constructor */
} // end class
