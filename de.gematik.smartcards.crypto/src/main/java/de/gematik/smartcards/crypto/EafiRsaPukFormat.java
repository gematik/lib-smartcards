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

/**
 * Enumeration of formats for {@code RsaPublicKeyImpl}.
 *
 * <p>These formats are used in {@link RsaPublicKeyImpl#getEncoded(EafiRsaPukFormat)} to convert a
 * public RSA key into a TLV-structure. During such a transformation, the structures shown below are
 * created.
 *
 * <p>These formats are used in {@link
 * RsaPublicKeyImpl#RsaPublicKeyImpl(de.gematik.smartcards.tlv.ConstructedBerTlv, EafiRsaPukFormat)}
 * when an instance is constructed from a TLV-structure. During such a transformation, the
 * structures shown below are expected. Extra TLV-objects within such a structure do not harm, but
 * missing elements cause an {@link IllegalArgumentException}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public enum EafiRsaPukFormat {
  /**
   * TLV structure being in accordance to ISO/IEC 7816-8:2019 table 3.
   *
   * <pre>
   * 7F49 - L -
   *      81 - L - modulus
   *      82 - L - publicExponent
   * </pre>
   */
  ISO7816,

  /**
   * TLV structure being in accordance to <a
   * href="https://tools.ietf.org/html/rfc8017#appendix-A.1.1">PKCS #1</a> {@code RSAPublicKey}.
   *
   * <pre>
   * 30 - L
   *    02 - L - modulus
   *    02 - L - publicExponent
   * </pre>
   */
  PKCS1,

  /*
   * RFC4880, i.e. OpenPGP. Possibly it is a good idea to activate
   *          this item in case package rfc with classes:
   *          - RFC4880.java
   *          - PgpPublicKeyBlock.java
   *          is available.
   */

  /**
   * TLV structure in accordance to <a href="https://tools.ietf.org/html/rfc5280">RFC 5280</a>.
   *
   * <p>The TLV structure is in conformance to ASN.1 definition of {@code SubjectPublicKeyInfo} as
   * defined in <a href="https://tools.ietf.org/html/rfc5280#section-4.1">RFC 5280 clause 4.1</a>:
   *
   * <pre>
   * SubjectPublicKeyInfo ::= SEQUENCE {
   *     algorithm        AlgorithmIdentifier,
   *     subjectPublicKey BIT STRING
   * }
   * </pre>
   *
   * <p>The algorithm in that structure is defined in <a
   * href="https://tools.ietf.org/html/rfc5280#section-4.1.1.2">RFC 5280 clause 4.1.1.2</a>:
   *
   * <pre>
   * AlgorithmIdentifier ::= SEQUENCE {
   *     algorithm  OBJECT IDENTIFIER,
   *     parameters ANY DEFINED BY algorithm OPTIONAL
   * }
   * </pre>
   *
   * <pre>
   * 30 - L -
   *    30 - L -
   *       06 - L - 2a864886f70d010101 # i.e. OID = 1.2.840.113549.1.1.1 = rsaEncryption
   *       05 - L = parameters = null
   *    03 - L - {@link #PKCS1}
   * </pre>
   */
  RFC5280,
} // end enumeration
