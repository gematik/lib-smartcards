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
package de.gematik.smartcards.g2icc.cos;

import de.gematik.smartcards.sdcom.apdu.ApduLayer;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ReadBinary;
import de.gematik.smartcards.utils.AfiUtils;
import javax.smartcardio.CardException;

/**
 * This class provides functions and methods used for easier smart card access.
 *
 * <p>TODO the functionality of this class should be moved to {@code G2Proxy}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class IccUtils {

  /** Private default constructor. */
  private IccUtils() {
    // intentionally empty
  } // end constructor */

  /**
   * Reads complete body of transparent EF.
   *
   * @param ifd for transmitting APDU
   * @param maxNe is the maximum value used for Ne
   * @return (part of) body of indicated EF which shall be transparent
   * @throws CardException if underlying methods do so
   */
  public static byte[] readTransparentBody(final ApduLayer ifd, final int maxNe)
      throws CardException {
    return readTransparentBody(ifd, 0, 0, 0xffff, maxNe);
  } // end method */

  /**
   * Reads (part of) body of transparent EF.
   *
   * @param ifd for transmitting APDU
   * @param sfi of EF from which to read, if 0 currentEF is used
   * @param offset is the offset of the first byte to be read
   * @param numberOfBytes is the maximum number of bytes to be read
   * @param maxNe is the maximum value used for Ne
   * @return (part of) body of indicated EF which shall be transparent
   * @throws CardException if underlying methods do so
   */
  public static byte[] readTransparentBody(
      final ApduLayer ifd, final int sfi, int offset, final int numberOfBytes, final int maxNe)
      throws CardException {
    // Note: The idea in the following implementation is to adjust Ne in the
    //       first command such that for subsequent commands (if any) Ne = maxNe.
    //       Thus, offset in the last command is the smallest possible value.
    //       This is good, because the offset cannot be larger than 32768.

    byte[] result = new byte[0];
    int firstNe = numberOfBytes % maxNe;
    if (0 == firstNe) {
      // ... numberOfBytes is a multiple of maxNe
      firstNe = maxNe;
    } // end fi
    ReadBinary cmd = new ReadBinary(sfi, offset, firstNe);

    for (; ; ) {
      // --- send command
      final ResponseApdu rsp = ifd.send(cmd);
      final int trailer = rsp.getTrailer();

      // --- evaluate trailer
      // spotless:off
      result = switch (trailer) {
        case 0x6282, 0x9000 -> AfiUtils.concatenate(result, rsp.getData()); // EndOfFileWarning
        default -> throw new CardException(String.format(
            "unexpected trailer: %04X, %s",
            trailer,
            cmd.explainTrailer(trailer)
        ));
      }; // end Switch
      // spotless:on

      if ((result.length == numberOfBytes) || (0x6282 == trailer)) {
        // ... nothing more to read
        return result;
      } // end fi
      // ... not_AllBytesRead  AND  not_EndOfFileWarning => something more to read

      // --- prepare the next ReadBinary command
      offset += rsp.getNr(); // NOPMD reassigning parameter
      cmd = new ReadBinary(0, offset, maxNe); // NOPMD new in loop
    } // end For (...)
  } // end method */
} // end class
