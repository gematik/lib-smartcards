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
package de.gematik.smartcards.crypto;

import de.gematik.smartcards.tlv.DerNull;
import de.gematik.smartcards.tlv.DerOctetString;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains routines from standard <a
 * href="https://en.wikipedia.org/wiki/PKCS_1">PKCS#1</a>.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class Pkcs1Utils {

  /** Private default-constructor preventing instantiation. */
  private Pkcs1Utils() {
    // intentionally empty
  } // end constructor */

  /**
   * Calculates a digestInfo according to PKCS#1 section 9.2 note 1.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change any of the entries in
   *       the input array after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and
   *       otherwise not used and the return value is never used again within this class.</i>
   * </ol>
   *
   * @param message for which the digest info is calculated
   * @param hashAlgorithm used
   * @return digest info for the given message
   */
  public static DerSequence pkcs1DigestInfo(
      final byte[] message, final EafiHashAlgorithm hashAlgorithm) {
    final var hash = hashAlgorithm.digest(message);

    return new DerSequence(
        List.of(
            new DerSequence(List.of(new DerOid(hashAlgorithm.getOid()), DerNull.NULL)),
            new DerOctetString(hash)));
  } // end method */

  /**
   * Performs EMSA-PKCS1-v1_5 operation according to PKCS#1, v2.2, section 9.2.
   *
   * @param message message to be encoded
   * @param emLen intended length in octets of the encoded message
   * @param hashAlgorithm used for calculating the digest info
   * @return encoded message
   * @throws IllegalArgumentException if emLen is too small
   */
  public static byte[] pkcs1EmsaV15(
      final byte[] message, final int emLen, final EafiHashAlgorithm hashAlgorithm) {
    final var digestInfo = pkcs1DigestInfo(message, hashAlgorithm).getEncoded();
    final int tLen = digestInfo.length;
    final int psLen = emLen - tLen - 3;

    // PKCS#1 v2.2, clause 9.2, step 3
    if (psLen < 8) { // NOPMD literal in if statement
      throw new IllegalArgumentException("intended encoded message length too short");
    } // end fi

    // PKCS#1 v2.2, clause 9.2, steps 4 and 5
    final byte[] em = new byte[emLen];
    Arrays.fill(em, (byte) 0xff);
    em[0] = 0;
    em[1] = 1;
    em[2 + psLen] = 0;
    System.arraycopy(digestInfo, 0, em, 3 + psLen, digestInfo.length);

    return em;
  } // end method */
} // end class
