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

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.MD2Digest;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA224Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA384Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Class providing RSA keys for tests.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.UseUtilityClass"})
class RsaKeyRepository {

  /**
   * {@link BouncyCastleProvider} for cryptographic functions.
   *
   * <p>The JDK does not support {@link EafiHashAlgorithm#RIPEMD_160}.
   *
   * <p>Because some tests use the underlying Java Runtime Environment here we use {@link
   * BouncyCastleProvider}.
   */
  /* package */
  static final BouncyCastleProvider BC = new BouncyCastleProvider(); // */

  /** Key pair generator. */
  /* package */
  static final KeyPairGenerator KEY_PAIR_GENERATOR; // */

  /** Repository for key pairs. */
  /* package */
  static final Map<Integer, KeyPair> KEY_PAIR_REPOSITORY = new ConcurrentHashMap<>(); // */

  /**
   * Minimum modulus length.
   *
   * <p>This is the minimum length supported by JDK.
   */
  /* package */
  static final int MIN_LENGTH = 512; // */

  /**
   * Maximum modulus length.
   *
   * <p>A reasonable key length with not too much generating time.
   */
  /* package */
  static final int MAX_LENGTH = 2048; // */

  /** RSA-key from ISO/IEC 9796-2:2010 E.1 and E.1.1, public exponent = 3. */
  /* package */
  static final RsaPrivateCrtKeyImpl PRK_9796_2_E1 =
      new RsaPrivateCrtKeyImpl(
          // prime p
          new BigInteger(
              Hex.extractHexDigits(
                  """
                      FB961451 995C82F9 527CAAAF B3FB4254 6D00A01D 8B2BDE3D 2E7B8F7D 0C9E781E
                      B7FABFC8 E86E9F6D ACE3435A 9D043A99 93F3E473 D93FA888 D3577906 77A94931
                      """),
              16),
          // prime q
          new BigInteger(
              Hex.extractHexDigits(
                  """
                      FF0EAFCA 70585166 A8CD8E90 36E75290 2F32B863 068016B6 A89F2EA3 418882EF
                      6F570122 F92D2E9B EFFF7329 1818F251 BF095D6E 208F93CD CEF4767A 568AB241
                      """),
              16),
          // public exponent
          AfiBigInteger.THREE);

  /** Random Number Generator. */
  /* package */
  static final AfiRng RNG = new AfiRng(); // */

  /** Set with hash algorithms for which no ISO/IEC 9796-2 trailer exists. */
  /* package */
  static final Set<EafiHashAlgorithm> NO_VALID_TRAILER =
      Set.of(EafiHashAlgorithm.MD2, EafiHashAlgorithm.MD5); // */

  /** Mapping from hash algorithm to a digest object. */
  /* package */
  static final Map<EafiHashAlgorithm, Digest> HASH_MAP =
      Map.ofEntries(
          Map.entry(EafiHashAlgorithm.MD2, new MD2Digest()),
          Map.entry(EafiHashAlgorithm.MD5, new MD5Digest()),
          Map.entry(EafiHashAlgorithm.RIPEMD_160, new RIPEMD160Digest()),
          Map.entry(EafiHashAlgorithm.SHA_1, new SHA1Digest()),
          Map.entry(EafiHashAlgorithm.SHA_224, new SHA224Digest()),
          Map.entry(EafiHashAlgorithm.SHA_256, new SHA256Digest()),
          Map.entry(EafiHashAlgorithm.SHA_384, new SHA384Digest()),
          Map.entry(EafiHashAlgorithm.SHA_512, new SHA512Digest())); // */

  static {
    Security.addProvider(BC);

    try {
      KEY_PAIR_GENERATOR = KeyPairGenerator.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
      throw new IllegalArgumentException(e); // NOPMD raw exception
    } // end Catch (...)
  } // end static */

  /**
   * Retrieves keypair of desired length from {@link #KEY_PAIR_REPOSITORY}.
   *
   * <p>If no key with the desired length is in the repository, an appropriate key pair is
   * generated.
   *
   * @param keyLength modulus length in bit
   * @return {@link KeyPair} of given modulus length
   */
  /* package */
  static KeyPair getKeyPair(final int keyLength) {
    final int effectiveKeyLength = Math.min(Math.max(keyLength, MIN_LENGTH), MAX_LENGTH);

    return KEY_PAIR_REPOSITORY.computeIfAbsent(
        effectiveKeyLength,
        k -> {
          if (0 == (k % 8)) {
            // ... multiple of 8
            KEY_PAIR_GENERATOR.initialize(k);
            return KEY_PAIR_GENERATOR.generateKeyPair();
          } else {
            final var prk = RsaPrivateCrtKeyImpl.gakpLog2(k);
            return new KeyPair(prk.getPublicKey(), prk);
          } // end else
        });
  } // end method */

  /**
   * Retrieves the private key of desired length from {@link #KEY_PAIR_REPOSITORY}.
   *
   * @param keyLength modulus length in bit
   * @return {@link RSAPrivateCrtKey} of given modulus length
   */
  /* package */
  static RSAPrivateCrtKey getPrk(final int keyLength) {
    final var keyPair = getKeyPair(keyLength);

    return (RSAPrivateCrtKey) keyPair.getPrivate();
  } // end method */

  /**
   * Retrieves the public key of desired length from {@link #KEY_PAIR_REPOSITORY}.
   *
   * @param keyLength modulus length in bit
   * @return {@link RSAPublicKey} of given modulus length
   */
  /* package */
  static RSAPublicKey getPuk(final int keyLength) {
    final var keyPair = getKeyPair(keyLength);

    return (RSAPublicKey) keyPair.getPublic();
  } // end method */
} // end class
