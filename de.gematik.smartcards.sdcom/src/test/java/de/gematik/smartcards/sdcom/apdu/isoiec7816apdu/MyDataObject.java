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
 * Class which supports testing of abstract class {@link AbstractDataObject}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
final class MyDataObject extends AbstractDataObject {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -1037140612138418512L;

  /**
   * Comfort constructor for a case 2 APDU, i.e. command data field absent and Le-field present.
   *
   * <p>Estimates parameters P1 and P2 from given tag and passes all other input parameter to
   * constructor from superclass.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param tag tag of TLV-data-object
   * @param ne maximum number of expected data bytes in a response APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code tag} is not in range [{@link #TAG_INFIMUM}, {@link #TAG_SUPREMUM}]
   *       <li>{@code ne} not from set {1, 2, ..., 65535, {@link CommandApdu#NE_SHORT_WILDCARD
   *           NE_SHORT_WILDCARD}, {@link CommandApdu#NE_EXTENDED_WILDCARD NE_EXTENDED_WILDCARD}}.
   *     </ol>
   */
  /* package */ MyDataObject(final int cla, final int ins, final int tag, final int ne) {
    super(cla, ins, tag, ne);
  } // end constructor */

  /**
   * Comfort constructor for a case 3 APDU, i.e. command data present and response data absent.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param tag tag of TLV-data-object
   * @param cmdData command data field, arbitrary octet string with at least one octet
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code tag} is not in range [{@link #TAG_INFIMUM}, {@link #TAG_SUPREMUM}]
   *       <li>{@code Nc} is not in range [0, 65535]
   *     </ol>
   */
  /* package */ MyDataObject(final int cla, final int ins, final int tag, final byte[] cmdData) {
    super(cla, ins, tag, cmdData);
  } // end constructor */
} // end class
