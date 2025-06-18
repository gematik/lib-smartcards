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

import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import java.io.Serial;

/**
 * Class which supports testing of abstract class {@link AbstractBinary}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
final class MyBinary extends AbstractBinary {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -884892511337423446L;

  /**
   * Comfort constructor for a ISO-case 1 APDU, i.e. command data field absent and Le-field absent.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param sfi short file identifier from range [0, 30]
   * @param offset offset of first octet in body of transparent EF affected by this command
   * @throws IllegalArgumentException if super-constructor does so
   */
  /* package */ MyBinary(final int cla, final int ins, final int sfi, final int offset) {
    super(cla, ins, sfi, offset);
  } // end constructor */

  /**
   * Comfort constructor for a case 2 APDU, i.e. command data field absent and Le-field present.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param sfi short file identifier from range [0, 30]
   * @param offset offset of first octet in body of transparent EF affected by this command
   * @param ne maximum number of expected data bytes in a {@link ResponseApdu}
   * @throws IllegalArgumentException if super-constructor does so
   */
  /* package */ MyBinary(
      final int cla, final int ins, final int sfi, final int offset, final int ne) {
    super(cla, ins, sfi, offset, ne);
  } // end constructor */

  /**
   * Comfort constructor for a case 3 APDU, i.e. command data present and response data absent.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param sfi short file identifier from range [0, 30]
   * @param offset offset of first octet in body of transparent EF affected by this command
   * @param cmdData command data field, arbitrary octet string with at least one octet
   * @throws IllegalArgumentException if super-constructor does so
   */
  /* package */ MyBinary(
      final int cla, final int ins, final int sfi, final int offset, final byte[] cmdData) {
    super(cla, ins, sfi, offset, cmdData);
  } // end constructor */
} // end inner class
