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

import de.gematik.smartcards.utils.AfiOid;
import java.security.interfaces.ECPrivateKey;

/**
 * Enumeration of formats for {@link EcPrivateKeyImpl}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public enum EafiElcPrkFormat {
  /**
   * ASN.1 TLV structure. It is unknown where this structure is defined, but it is used by TSPs.
   *
   * <pre>
   *   30 - L                                 # SEQUENCE
   *      02 01 00                            #   probably version number
   *      30 - L                              #   DO domain parameter
   *      |    06 07 2a8648ce3d0201           #     {@link AfiOid#ecPublicKey}
   *      |    30 - L                         #     DO elliptic curve
   *      |    |    02 01 01                  #       probably version number
   *      |    |    30 - L -                  #       DO field
   *      |    |    |    06 07 2a8648ce3d0101 #         {@link AfiOid#fieldType}
   *      |    |    |    02 - L - p           #         prime P defining the field Fp
   *      |    |    30 - L -                  #       DO-coefficients
   *      |    |    |    04 - L - a           #         coefficient a
   *      |    |    |    04 - L - b           #         coefficient b
   *      |    |    04 - L - G                #       generator G uncompressed
   *      |    |    02 - L - n                #       order of generator G
   *      |    |    02 - L - h                #       cofactor h
   *      04 - L - {@link #PKCS1}             #   private key d
   * </pre>
   */
  ASN1, // */

  /**
   * TLV Structure similar to BSI-TR03110-3 v2.10 clause D.3.3.
   *
   * <pre>
   *   7F48 - L          # tag according to ISO/IEC 7816-6
   *        06 - L - OID #   identifying domain parameter, see e.g. {@link AfiElcParameterSpec}
   *        02 - L - d   #   private key d
   * </pre>
   */
  ISOIEC7816, // */

  /**
   * TLV structure similar to PKCS#1.
   *
   * <p><i><b>Note:</b> Encoding a private key into this format is easy and unambiguous, but because
   * no domain parameters are given decoding from this format would be ambiguous.</i>
   *
   * <pre>
   *   30 - L
   *      02 01 01   # version = 1
   *      04 - L - d # private key d
   * </pre>
   */
  PKCS1, // */

  /**
   * TLV structure being in accordance to {@link ECPrivateKey#getEncoded()}.
   *
   * <pre>
   *   30 - L                     # SEQUENCE
   *      02 01 00                #   version
   *      30 - L                  #   SEQUENCE with OID
   *         06 07 2a8648ce3d0201 #    {@link AfiOid#ecPublicKey}
   *         06 - L - OID         #    identifying domain parameter, see {@link AfiElcParameterSpec}
   *      04 - L - {@link #PKCS1} #   private key d
   * </pre>
   *
   * <p><i><b>Note:</b> This structure is identical to the output of {@link
   * ECPrivateKey#getEncoded()}.</i>
   */
  PKCS8, // */
} // end Enum_ElcPrK_Format
