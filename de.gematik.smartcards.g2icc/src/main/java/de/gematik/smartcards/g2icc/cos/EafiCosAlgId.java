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

/**
 * Algorithm identifier as defined by {@code gemSpec_COS}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.FieldNamingConventions"})
public enum EafiCosAlgId {
  // Note: The list is sorted according to [gemSpec_COS#16.1].

  // Note: Algorithm identifier for authentication, see [gemSpec_COS#Tab.268].

  /** Symmetric session key negotiation with AES, session keys used for secure messaging. */
  AES_SESSIONKEY_4_SM("54"), // */

  /** Symmetric session key negotiation with AES, session keys used for {@code PSO}-commands. */
  AES_SESSIONKEY_4_TC("74"), // */

  /** Asymmetric asynchronous administration, used with {@code GENERAL AUTHENTICATE}. */
  ELC_ASYNCHRON_ADMIN("F4"), // */

  /** Asymmetric, {@code INTERNAL AUTHENTICATE}. */
  ELC_ROLE_AUTHENTICATION("00"), // */

  /** Asymmetric, {@code EXTERNAL AUTHENTICATE}. */
  ELC_ROLE_CHECK("00"), // */

  /**
   * Asymmetric session key negotiation with elliptic curve cryptography, session keys used for
   * secure messaging.
   */
  ELC_SESSIONKEY_4_SM("54"), // */

  /**
   * Asymmetric session key negotiation with elliptic curve cryptography, session keys used for
   * {@code PSO}-commands.
   */
  ELC_SESSIONKEY_4_TC("D4"), // */

  /** Client authentication with RSA keys, {@code INTERNAL AUTHENTICATE}. */
  RSA_CLIENT_AUTHENTICATION("05"), // */

  // Note: Algorithm identifier for encryption, decryption, see [gemSpec_COS#Tab.269].

  /** {@code PSO Decipher} with RSA key. */
  RSA_DECIPHER_OAEP("85"), // */

  /** {@code PSO Encipher} with RSA key. */
  RSA_ENCIPHER_OAEP("05"), // */

  /** {@code PSO Decipher}, {@code PSO Encipher} with elliptic curve cryptography. */
  ELC_SHARED_SECRET_CALCULATION("0B"), // NOPMD long name */

  // Note: Algorithm identifier for integrity and authenticity, see [gemSpec_COS#Tab.270].

  /** {@code PSO Compute Digital Signature}. */
  SIGN_9796_2_DS_2("07"), // */

  /** {@code PSO Compute Digital Signature}. */
  SIGN_PKCS_1_V_1_5("02"), // */

  /** {@code PSO Compute Digital Signature}. */
  SIGN_PSS("05"), // */

  /** {@code PSO Compute Digital Signature}. */
  SIGN_ECDSA("00"), // */
  ;

  /** Stores algorithm identifier used at the {@code IFD &lt;-&gt; ICC} interface. */
  private final String insAlgId;

  /**
   * Comfort constructor.
   *
   * @param algId algorithm identifier used at the {@code IFD &lt;-&gt; ICC} interface
   */
  EafiCosAlgId(final String algId) {
    insAlgId = algId;
  } // end constructor */

  /**
   * Return algorithm identifier used at {@code IFD &lt;-&gt; ICC} interface.
   *
   * @return algorithm identifier
   */
  public String getAlgId() {
    return insAlgId;
  } // end method */
} // end enum
