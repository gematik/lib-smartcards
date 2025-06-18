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

/**
 * Enumeration of formats for {@code RsaPrivateKeyImpl}.
 *
 * <p>These formats are used in {@code RsaPrivateKeyImpl#getEncoded(EafiRsaPrkFormat)} to convert a
 * private RSA key into a TLV-structure. During such a transformation, the structures shown below
 * are created.
 *
 * <p>Furthermore, these formats are also used in {@code
 * RsaPrivateKeyImpl#RsaPrivateKeyImpl(DerSequence, EafiRsaPrkFormat)} when an instance is
 * constructed from a TLV-structure. During such a transformation, the structures shown below are
 * expected. Extra TLV-objects within such a structure do not harm, but missing elements cause an
 * {@link IllegalArgumentException}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public enum EafiRsaPrkFormat {
  /**
   * TLV structure being in accordance to <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.1.2">PKCS #1</a> {@code RSAPrivateKey}.
   *
   * <pre>
   *   30 - L
   *      02 01 00                 # version, here zero
   *      02 - L - modulus         # n = p * q
   *      02 - L - publicExponent  # e
   *      02 - L - privateExponent # d
   *      02 - L - prime1          # p
   *      02 - L - prime2          # q
   *      02 - L - exponent1       # dp = d mod (p - 1)
   *      02 - L - exponent2       # dq = d mod (q - 1)
   *      02 - L - coefficient     # c  = q^(-1) mod p
   * </pre>
   */
  PKCS1, // */

  /**
   * TLV structure being in accordance to <a href="https://tools.ietf.org/html/rfc5958">PKCS#8</a>
   * {@code OneAsymmetricKey}.
   *
   * <pre>
   *   30 - L
   *      02 01 00                      # version, here zero
   *      30 - L                        # privateKeyAlgorithm
   *           06 09 2a864886f70d010101 # {@link AfiOid#rsaEncryption}
   *           05 00                    # NULL = parameters
   *      04 - L - bitString            # privateKey = {@link #PKCS1}
   * </pre>
   */
  PKCS8, // */
} // end enumeration
