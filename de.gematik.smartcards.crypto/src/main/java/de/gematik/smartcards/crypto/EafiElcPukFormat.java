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

import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.utils.AfiOid;

/**
 * Enumeration of formats for {@link EcPublicKeyImpl}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public enum EafiElcPukFormat {
  /**
   * TLV structure according to BSI-TR-03110-3 v2.21 clause D.3.3.
   *
   * <pre>
   *   7F49 - L
   *        06 - L - OID # identifying domain parameter, see e.g. {@link AfiOid}
   *        86 - L - ... # public point in uncompressed format, see BST-TR-03111 v2.10 clause 3.2.1
   * </pre>
   */
  ISOIEC7816, // */

  /**
   * TLV structure being in accordance to PKCS#8, i.e.
   *
   * <pre>
   *   30 - L
   *      30 - L -                  # {@link DerSequence} with OID
   *           06 07 2a8648ce3d0201 # i.e. {@link AfiOid#ecPublicKey}
   *           06 - L - dP          # OID identifying domain parameter
   *      03 - L - ...              # public point in uncompressed format,
   *                             see BST-TR-03111 v2.10 clause 3.2.1
   * </pre>
   *
   * <p><i><b>Note:</b> This structure is identical to the output of {@link
   * java.security.interfaces.ECPublicKey#getEncoded()}.</i>
   */
  X509, // */
} // end enumeration
