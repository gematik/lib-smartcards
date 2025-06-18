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
 * Class which supports testing of abstract class {@link AbstractRecord}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
final class MyRecord extends AbstractRecord {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 7522468462160930003L;

  /**
   * Comfort constructor for a case 2 APDU, i.e. command data field absent and Le-field present.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param recordNumber number of record from range [0, 254]
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param b3b2b1 content of bits b3, b2, b1 being command specific, from range [0, 7]
   * @param ne maximum number of expected data bytes in a {@link ResponseApdu}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code recordNumber} is not in range [0, 254]
   *       <li>{@code sfi} is not in range [0, 30]
   *       <li>{@code b3b2b1} is not in range [0, 7]
   *       <li>{@code ne} not from set {1, 2, ..., 65535, {@link CommandApdu#NE_SHORT_WILDCARD
   *           NE_SHORT_WILDCARD}, {@link CommandApdu#NE_EXTENDED_WILDCARD NE_EXTENDED_WILDCARD}}.
   *     </ol>
   */
  /* package */ MyRecord(
      final int cla,
      final int ins,
      final int recordNumber,
      final int sfi,
      final int b3b2b1,
      final int ne) {
    super(cla, ins, recordNumber, sfi, b3b2b1, ne);
  } // end constructor */

  /**
   * Comfort constructor for a case 3 APDU, i.e. command data present and response data absent.
   *
   * @param cla class byte
   * @param ins instruction code
   * @param recordNumber number of record from range [0,254]
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param b3b2b1 content of bits b3, b2, b1 being command specific, from range [0, 7]
   * @param cmdData command data field, arbitrary octet string with at least one octet
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code recordNumber} is not in range [0, 254]
   *       <li>{@code sfi} is not in range [0, 30]
   *       <li>{@code b3b2b1} is not in range [0, 7]
   *       <li>{@code Nc} is not in range [0, 65535]
   *     </ol>
   */
  /* package */ MyRecord(
      final int cla,
      final int ins,
      final int recordNumber,
      final int sfi,
      final int b3b2b1,
      final byte[] cmdData) {
    super(cla, ins, recordNumber, sfi, b3b2b1, cmdData);
  } // end constructor */
} // end class
