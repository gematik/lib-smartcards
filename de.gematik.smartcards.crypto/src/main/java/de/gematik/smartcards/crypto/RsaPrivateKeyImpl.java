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

import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerNull;
import de.gematik.smartcards.tlv.DerOctetString;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.Serial;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class implements{@link RSAPrivateKey}.
 *
 * <p>From the perspective of this class
 *
 * <ol>
 *   <li>Instances are immutable value-types. Thus, {@link Object#equals(Object) equals()}, {@link
 *       Object#hashCode() hashCode()} are overwritten, but {@link Object#clone() clone()} isn't
 *       overwritten.
 *   <li>where data is passed in or out, defensive cloning is performed.
 *   <li>methods are thread-safe.
 * </ol>
 *
 * <p>It follows that from the perspective of this class object sharing is possible without side
 * effects.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.CyclomaticComplexity",
  "PMD.GodClass",
  "PMD.LocalVariableNamingConventions",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
})
// Note: An RSA-private-key has enough information to support all cryptographic
//       methods of an RSA-public-key. From that point of view, an RSA-private-key
//       could extend RSA-public-key. This is not implemented here, because
//       methods "getFormat()" and "getEncoded()" differ completely for public
//       and private RSA-keys.
public class RsaPrivateKeyImpl implements RSAPrivateKey {

  /** Serial number randomly generated on 2022-07-25. */
  @Serial private static final long serialVersionUID = 7201148518736068403L;

  /**
   * Infimum for public exponent.
   *
   * <p>According to <a
   * href="https://www.sogis.eu/documents/cc/crypto/SOGIS-Agreed-Cryptographic-Mechanisms-1.2.pdf">SOG-IS
   * clause 4.1</a> and <a href="https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf">FIPS
   * 186-4 B.3.3 step 2</a> the public exponent <b>SHALL</b> be in range {@code 2^16 &lt;
   * publicExponent &lt; 2^256}.
   */
  public static final BigInteger INFIMUM_E = BigInteger.valueOf(65_537); // */

  /**
   * Minimum modulus length considered secure (in bit).
   *
   * <p>Currently, the modulus length shall be equal or greater than 1900 bits (this will change to
   * 3000 bits on January 1st, 2026), see <a
   * href="https://www.sogis.eu/documents/cc/crypto/SOGIS-Agreed-Cryptographic-Mechanisms-1.2.pdf">SOG-IS
   * clause 4.1</a>.
   */
  public static final int INFIMUM_SECURE_MODULUS_LENGTH = 1_900; // NOPMD long name */

  /** Supremum bit-length for public exponent. */
  public static final int SUPREMUM_LD_E = 255; // */

  /** Random number generator. */
  /* package */ static final SecureRandom RNG = new SecureRandom(); // */

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Because only immutable instance attributes of this class are taken into account for
   *       this instance attribute lazy initialization is possible.</i>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  /* package */ volatile int insHashCode; // NOPMD volatile not recommended */

  /** Modulus of the RSA key. */
  private final BigInteger insModulus; // */

  /** Public exponent of RSA key. */
  private final BigInteger insPublicExponent; // */

  /** Private exponent of RSA key. */
  private final BigInteger insPrivateExponent; // */

  /**
   * Constructor using given parameter.
   *
   * @param modulus of RSA key
   * @param publicExponent public exponent of RSA key
   * @param privateExponent private exponent of RSA key
   */
  public RsaPrivateKeyImpl(
      final BigInteger modulus, final BigInteger publicExponent, final BigInteger privateExponent) {
    insModulus = modulus;
    insPublicExponent = publicExponent;
    insPrivateExponent = privateExponent;
  } // end constructor */

  /**
   * Function splits a message into M1 and M2 with {@code message = M1 || M2} according to ISO/IEC
   * 9796-2:2010 clause 7.2.2.
   *
   * @param message message to be split into two parts
   * @param numberOfBitsM1 C*, i.e., number of bits in first part, <b>SHALL</b>> be a multiple of 8
   * @return {M1, M2} with {@code C* &gt; (bit-length of M1)} and {@code message == M1 || M2}.
   * @throws IllegalArgumentException if {@code numberOfBitsM1} is not a multiple of 8
   * @throws NegativeArraySizeException if {@code numberOfBitsM1} is negative
   */
  @VisibleForTesting // otherwise: private
  /* package */ static byte[][] messageAllocation(final byte[] message, final int numberOfBitsM1) {
    if (0 != (numberOfBitsM1 % 8)) {
      throw new IllegalArgumentException("not a multiple of 8: " + numberOfBitsM1);
    } // end fi

    final byte[] m1 = Arrays.copyOf(message, Math.min(message.length, numberOfBitsM1 >> 3));
    final byte[] m2 = Arrays.copyOfRange(message, m1.length, message.length);

    return new byte[][] {m1, m2};
  } // end method */

  /**
   * Checks if this key is strong.
   *
   * <p>A finding is reported, if
   *
   * <ol>
   *   <li>modulus length is less than {@link #INFIMUM_SECURE_MODULUS_LENGTH}
   *   <li>public exponent is less than {@link #INFIMUM_E}
   *   <li>public exponent length is greater than {@link #SUPREMUM_LD_E}
   * </ol>
   *
   * @return {@link String} with check-result, if empty then no errors found
   */
  public String checkSecurity() {
    final List<String> result = new ArrayList<>();
    int a;

    // --- check modulus length
    if ((a = getModulusLengthBit()) < INFIMUM_SECURE_MODULUS_LENGTH) { // NOPMD assignment
      result.add("modulus length too small: " + a);
    } // end fi

    // --- check public exponent
    final var publicExponent = getPublicExponent();
    if (INFIMUM_E.compareTo(publicExponent) > 0) {
      result.add("public exponent to small: " + publicExponent);
    } // end fi

    a = publicExponent.bitLength();
    if (a > SUPREMUM_LD_E) {
      result.add("bit length of e out of range: " + a);
    } // end fi

    return String.join(LINE_SEPARATOR, result);
  } // end method */

  /**
   * Returns the standard algorithm name for this key, {@code RSA} here.
   *
   * <p>See <a
   * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/Key.html#getAlgorithm()">getAlgorithm()</a>
   * and <a
   * href="https://docs.oracle.com/en/java/javase/17/docs/specs/security/standard-names.html#cipher-algorithm-names">Algorithm
   * Names</a>
   *
   * @return {@link String} with content "RSA"
   * @see java.security.Key#getAlgorithm()
   */
  @Override
  public final String getAlgorithm() {
    return "RSA";
  } // end method */

  /**
   * Returns the primary encoding format of this key, here {@code "PKCS#8"}.
   *
   * <p>See <a
   * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/Key.html#getFormat()">getFormat()</a>.
   *
   * @return {@link String} with content "PKCS#8"
   * @see java.security.Key#getFormat()
   */
  @Override
  public final String getFormat() {
    return "PKCS#8";
  } // end method */

  /**
   * Returns the key in its primary encoding format.
   *
   * <p>Here a subset of {@link EafiRsaPrkFormat#PKCS8} is returned containing all available
   * attributes, i.e.
   *
   * <ol>
   *   <li>version (set to zero)
   *   <li>modulus
   *   <li>public exponent
   *   <li>private exponent
   * </ol>
   *
   * @return key in encoded format
   * @see java.security.Key#getEncoded()
   */
  @Override
  public byte[] getEncoded() {
    return new DerSequence(
            List.of(
                new DerInteger(ZERO), // version
                new DerSequence(List.of(new DerOid(AfiOid.rsaEncryption), DerNull.NULL)),
                new DerOctetString(
                    new DerSequence(
                            List.of(
                                new DerInteger(ZERO), // version
                                new DerInteger(getModulus()),
                                new DerInteger(getPublicExponent()),
                                new DerInteger(getPrivateExponent())))
                        .getEncoded())))
        .getEncoded();
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * @param obj object used for comparison, can be null
   * @return true if objects are equal, false otherwise
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final @CheckForNull Object obj) {
    // Note 1: Because this class is a direct subclass of Object calling
    //         super.equals(...) would be wrong.
    //         Instead, special checks are performed.

    // --- reflexive
    if (this == obj) {
      return true;
    } // end fi
    // ... obj not the same as this

    // --- null
    if (null == obj) {
      // ... this differs from null
      return false;
    } // end fi
    // ... obj not null

    // --- check type
    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end fi
    // ... obj is an instance of RsaPrivateKeyImpl

    final RsaPrivateKeyImpl other = (RsaPrivateKeyImpl) obj;

    // --- compare instance attributes
    // ... assertion: instance attributes are never null
    return getModulus().equals(other.getModulus())
        && getPublicExponent().equals(other.getPublicExponent())
        && getPrivateExponent().equals(other.getPrivateExponent());
  } // end method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * @return hash-code of object
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    // Note 1: Because this class is a direct subclass of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from the main memory into thread local memory
    if (0 == result) {
      // ... obviously attribute hashCode has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      result = getModulus().hashCode(); // start value calculated from instance attribute modulus
      final int hashCodeMultiplier = 31; // small prime number

      result = result * hashCodeMultiplier + getPublicExponent().hashCode(); // update with e
      result = result * hashCodeMultiplier + getPrivateExponent().hashCode(); // update with d

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Implements RSA decryption Primitive (RSADP) from PKCS #1 v2.2 subsection 5.1.2.
   *
   * <p>Because Chinese Remainder Theorem (CRT) parameters are not available here, "straightforward"
   * implementation is used.
   *
   * @param cipherText being deciphered
   * @return the deciphered plain text
   * @throws IllegalArgumentException if {@code 0 &le; cipherText &lt; modulus} is not fulfilled
   */
  public BigInteger pkcs1RsaDp(final BigInteger cipherText) {
    if (cipherText.signum() < 0) {
      throw new IllegalArgumentException("c is too small");
    } // end fi

    final BigInteger n = getModulus();

    if (cipherText.compareTo(n) >= 0) {
      throw new IllegalArgumentException("c is too big");
    } // end fi
    // ... 0 <= c < n

    // --- use "straight forward"
    return cipherText.modPow(getPrivateExponent(), n);
  } // end method */

  /**
   * Deciphers given cryptogram according to RSAES-OAEP-DECRYPT from PKCS#1 v2.2 subsection 7.1.2.
   *
   * <p>This method uses an empty label.
   *
   * @param cryptogram cryptogram
   * @param hashAlgorithm to be used for MGF
   * @return plain text
   * @throws IllegalArgumentException to indicate "decipher-error" as defined in PKCS#1
   */
  public final byte[] pkcs1RsaEsOaepDecrypt(
      final byte[] cryptogram, final EafiHashAlgorithm hashAlgorithm) {
    return pkcs1RsaEsOaepDecrypt(cryptogram, AfiUtils.EMPTY_OS, hashAlgorithm);
  } // end method */

  /**
   * Deciphers given cryptogram according to RSAES-OAEP-DECRYPT from PKCS#1 v2.2 subsection 7.1.2.
   *
   * @param cryptogram cryptogram
   * @param label possible empty
   * @param hashAlgorithm to be used for MGF
   * @return plain text
   * @throws IllegalArgumentException to indicate "decipher-error" as defined in PKCS#1
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  public final byte[] pkcs1RsaEsOaepDecrypt(
      final byte[] cryptogram, final byte[] label, final EafiHashAlgorithm hashAlgorithm) {
    try {
      final int k = getModulusLengthOctet(); // NOPMD
      final int hLen = hashAlgorithm.getDigestLength(); // NOPMD

      // --- PKCS#1 v2.2, clause 7.1.2, step 1.a: check length of label L
      // intentionally L is not checked whether for exceeding the input limitation of
      // hash algorithm, because this limitation is beyond the size of a java byte-array

      // --- PKCS#1 v2.2, clause 7.1.2, step 1.b: check length of cryptogram
      // intentionally the ciphertext "cryptogram" is not checked here, if it is
      // "too long", then pkcs1_Dp(c)-method will throw an exception

      // --- PKCS#1 v2.2, clause 7.1.2, step 1.c: capacity of the private key
      if (k >= (2 * hLen + 2)) {
        // ... modulus has enough capacity

        // --- PKCS#1 v2.2, clause 7.1.2, step 2: RSA decryption
        final BigInteger c = new BigInteger(1, cryptogram); // step 2.a
        final BigInteger m = pkcs1RsaDp(c); // step 2.b

        // --- PKCS#1 v2.2, clause 7.1.2, step 3: EME-OAEP decoding
        // step 3.a
        final byte[] lHash = hashAlgorithm.digest(label); // NOPMD

        // step 3.b
        final byte[] encodedMessage = AfiBigInteger.i2os(m, k);
        final int Y = encodedMessage[0] & 0xff; // NOPMD declaration before possible exit point
        final byte[] maskedSeed = new byte[hLen];
        System.arraycopy(encodedMessage, 1, maskedSeed, 0, hLen);
        final byte[] maskedDb = new byte[k - hLen - 1];
        System.arraycopy(encodedMessage, 1 + hLen, maskedDb, 0, maskedDb.length);
        // step 3.c
        final byte[] seedMask = hashAlgorithm.maskGenerationFunction(maskedDb, hLen, 0);
        final byte[] seed = new byte[hLen]; // step 3.d
        for (int i = hLen; i-- > 0; ) { // NOPMD assignment in operand
          seed[i] = (byte) (maskedSeed[i] ^ seedMask[i]);
        } // end For (i...)
        // step 3.e
        final byte[] dbMask = hashAlgorithm.maskGenerationFunction(seed, maskedDb.length, 0);
        final byte[] db = new byte[dbMask.length]; // step 3.f
        for (int i = db.length; i-- > 0; ) { // NOPMD assignment in operand
          db[i] = (byte) (maskedDb[i] ^ dbMask[i]);
        } // end For (i...)

        // step 3.g
        final byte[] lhashDash = new byte[hLen];
        System.arraycopy(db, 0, lhashDash, 0, hLen);
        int separatorIndex = hLen;
        while (0 == db[separatorIndex]) { // possibly throws ArrayIndexOutOfBoundsException
          separatorIndex++;
        } // end While (...)

        if ((1 == db[separatorIndex]) && Arrays.equals(lHash, lhashDash) && (0 == Y)) {
          final byte[] result = new byte[db.length - separatorIndex - 1];
          System.arraycopy(db, separatorIndex + 1, result, 0, result.length);

          return result;
        } // end fi (everything right?)
      } // end fi (enough capacity?)
    } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) { // NOPMD empty catch
      // intentionally empty
    } // end Catch (...)

    throw new IllegalArgumentException("decryption error");
  } // end method */

  /**
   * Deciphers the given cipher text according to RSAES-PKCS1-V1_5-DECRYPT from PKCS#1 v2.2
   * subsection 7.2.2.
   *
   * @param ciphertext to be deciphered
   * @return message included in cipher text
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code cipherText} (seen as a {@link BigInteger} is not in range [0, n - 1]
   *       <li>plain text is not padded according to RSAES-PKCS1-v1_5
   *     </ol>
   */
  public final byte[] pkcs1RsaEsPkcs1V15Decrypt(final byte[] ciphertext) {
    final byte[] plainText =
        AfiBigInteger.i2os(pkcs1RsaDp(new BigInteger(1, ciphertext)), getModulusLengthOctet());

    // --- find separator byte
    int separator = 2;
    try {
      // Note 1: In case no separator is available, then ArrayIndexOutOfBoundsException is thrown.
      while (0 != plainText[separator]) {
        separator++;
      } // end For (b...)
      separator++;

      // --- check plain text
      if ((0 == plainText[0]) && (2 == plainText[1]) && (separator >= 11)) {
        return Arrays.copyOfRange(plainText, separator, plainText.length);
      } // end fi
    } catch (ArrayIndexOutOfBoundsException e) { // NOPMD empty block
      // intentionally empty
    } // end Catch (...)

    // Note 1: All exceptions within this function are thrown from this line. Thus, the
    //         caller gets no information from the exception stack what went wrong.
    throw new IllegalArgumentException("decryption error");
  } // end method */

  /**
   * Signs a message m according to RSASP1 from PKCS#1 v2.2 subsection 5.2.1.
   *
   * <p><i><b>Note:</b> This function is identical to {@link #pkcs1RsaDp(BigInteger)}.</i>
   *
   * @param m message to be signed
   * @return signature representative
   */
  public final BigInteger pkcs1RsaSp1(final BigInteger m) {
    return pkcs1RsaDp(m);
  } // end method */

  /**
   * Signs the given message according to RSASSA-PKCS1-v1_5-SIGN from PKCS#1 v2.2 subsection 8.2.1.
   *
   * @param message message to be signed
   * @param hash indicates hash algorithm to be used during message encoding
   * @return byte array containing the signature
   */
  public byte[] pkcs1RsaSsaPkcs1V15Sign(final byte[] message, final EafiHashAlgorithm hash) {
    final int emLen = getModulusLengthOctet();

    final BigInteger dsi = new BigInteger(1, Pkcs1Utils.pkcs1EmsaV15(message, emLen, hash));
    final BigInteger sig = pkcs1RsaSp1(dsi);

    return AfiBigInteger.i2os(sig, emLen);
  } // end method */

  /**
   * Signs the given message according to RSASSA-PSS from PKCS#1 v2.2 subsection 8.1.1.
   *
   * <p><i><b>Note:</b> This function uses a salt with a length equal to the length of the hash
   * value. For different salt lengths use {@link #pkcs1RsaSsaPssSign(byte[], EafiHashAlgorithm,
   * int)} instead.</i>
   *
   * @param message message to be signed
   * @param hash indicates hash algorithm to be used during message encoding
   * @return byte array containing the signature
   */
  public byte[] pkcs1RsaSsaPssSign(final byte[] message, final EafiHashAlgorithm hash) {
    return pkcs1RsaSsaPssSign(message, hash, hash.getDigestLength());
  } // end method */

  /**
   * Signs the given message according to RSASSA-PSS from PKCS#1 v2.2 subsection 8.1.1.
   *
   * @param message message to be signed
   * @param hash indicates hash algorithm to be used during message encoding
   * @param lengthSalt length of salt in byte
   * @return byte array containing the signature
   */
  public final byte[] pkcs1RsaSsaPssSign(
      final byte[] message, final EafiHashAlgorithm hash, final int lengthSalt) {
    final var salt = new byte[lengthSalt];
    RNG.nextBytes(salt);

    // --- call subroutine with appropriate parameters
    return pkcs1RsaSsaPssSign(
        message, // message
        hash, // hash-algorithm
        salt // salt, 9.2.1 step 2, (N003.300)a.2
        );
  } // end method */

  /**
   * Signs the given message according to RSASSA-PSS from PKCS#1 v2.2 subsection 8.1.1.
   *
   * <p><i><b>Note:</b> Intentionally this method is intended to be used with known test vectors for
   * test-purposes only, because specifying the salt is insecure.</i>
   *
   * @param message message to be signed
   * @param hash indicates hash algorithm to be used during message encoding
   * @param salt to be used during signature generation
   * @return byte array containing the signature
   */
  @VisibleForTesting // otherwise: private
  /* package */ final byte[] pkcs1RsaSsaPssSign(
      final byte[] message, final EafiHashAlgorithm hash, final byte[] salt) {
    // --- EMSA-PSS encoding PKCS#1 v2.2 clause 8.1.1 step 1
    // --- RSA signature PKCS#1 v2.2 clause 8.1.1 step 2

    // --- call subroutine with appropriate parameters
    return signIsoIec9796p2ds3(
        message,
        salt,
        hash,
        0, // cStar => M1 is empty
        true, // see PKCS#1 v2.2 clause 9.1.1, step 12 => always implicit
        false // do not use minimum
        )[0]; // do not return M2, because M2 is always equal to m, because of cStar = 0
  } // end method */

  /**
   * Signs a message representative according to ISO/IEC 9796-2:2010 annex B.4.
   *
   * <p><i><b>Note:</b> Defining s = I^d mod n, this function returns minimum{s, n-s}.</i>
   *
   * @param messageRepresentative message representative I as positive integer
   * @return signature
   * @throws IllegalArgumentException in case the rightmost nibble of I is not '1100'
   */
  public final BigInteger signIsoIec9796p2A4(final BigInteger messageRepresentative) {
    final BigInteger signature = signIsoIec9796p2A6(messageRepresentative);

    return signature.min(getModulus().subtract(signature));
  } // end method */

  /**
   * Signs a message representative according to ISO/IEC 9796-2:2010 annex B.6.
   *
   * <p><i><b>Note:</b> The only difference to {@link #signIsoIec9796p2A4(BigInteger)} is that here
   * always s = I^d mod n is returned. Thus, no minimum calculation is involved.</i>
   *
   * @param messageRepresentative message representative I as positive integer
   * @return signature
   * @throws IllegalArgumentException in case the rightmost nibble of I is not '1100'
   */
  public final BigInteger signIsoIec9796p2A6(final BigInteger messageRepresentative) {
    if (0xc != (messageRepresentative.intValue() & 0xf)) { // NOPMD literal in if statement
      throw new IllegalArgumentException("rightmost nibble != '1100'");
    } // end fi

    return pkcs1RsaSp1(messageRepresentative);
  } // end method */

  /**
   * Signs a message according to ISO/IEC 9796-2:2010 DS1 from clause 8.3 together with signature
   * production function from ISO/IEC 9796-2:2010 appendix B.4 (minimum).
   *
   * @param message message to be signed
   * @param hash hash algorithm to be used
   * @param implicit if {@code TRUE} then the trailer field consists of one octet ´BC´, otherwise
   *     the trailer consists of two octets, see ISO/IEC 9796-2 clause 9.2.3
   * @return array of byte-arrays with {signature, M2}
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  public final byte[][] signIsoIec9796p2ds1(
      final byte[] message, final EafiHashAlgorithm hash, final boolean implicit) {
    final int k = getModulusLengthBit();
    final int Lh = hash.getDigestLength() << 3; // length in bits
    final int numberOfPaddingBytes = implicit ? 1 : 2;
    final int c = k - Lh - 8 * numberOfPaddingBytes - 4; // capacity according to 8.2.3

    if (c < 7) { // NOPMD literal in if statement
      // ... capacity too small, see clause 7.2.2 §1
      throw new IllegalArgumentException("capacity of signature too small: " + c);
    } // end fi

    final int mLength = message.length << 3; // length in bits

    // Note: For negative x it is (x mod 8) == (((x % 8) + 8) % 8) != (x % 8)
    final var delta = (((c - mLength) % 8) + 8) % 8; // 7.2.2, delta = (c - mLength) mod 8
    final var t = trailerField(hash, 1 == numberOfPaddingBytes); // 8.2.2, trailer field
    final var cStar = Math.min(c - delta, mLength); // 7.2.2 §2
    final var mSplit = messageAllocation(message, cStar); // 7.2.2
    final var m1 = mSplit[0];
    final var m2 = mSplit[1];
    final var h = hash.digest(message); // 8.3.1

    BigInteger bitString = ONE; // 8.3.2, §1 dash 1
    bitString = bitString.shiftLeft(1).add((m2.length > 0) ? ONE : ZERO); // 8.3.2, §1 dash 2
    bitString = bitString.shiftLeft(c - cStar); // 8.3.2, §1 dash 3
    bitString = bitString.shiftLeft(1).add(ONE); // 8.3.2, §1 dash 4
    bitString = bitString.shiftLeft(cStar).add(new BigInteger(1, m1)); // 8.3.2, §1 dash 5
    bitString = bitString.shiftLeft(Lh).add(new BigInteger(1, h)); // 8.3.2, §1 dash 6
    bitString = bitString.shiftLeft(8 * numberOfPaddingBytes).add(t); // 8.3.2, §1 dash 7

    // 8.3.2, §2
    final var shift = (8 - (k % 8)) % 8;
    final var f2 = bitString.shiftLeft(shift).toByteArray();

    if (0 == (f2[0] & 0x10)) {
      zzzAdjustF2(f2);
    } // end fi

    final BigInteger uF2 = (new BigInteger(f2)).shiftRight(shift);
    final byte[] signature = AfiBigInteger.i2os(signIsoIec9796p2A4(uF2), getModulusLengthOctet());

    return new byte[][] {signature, m2};
  } // end method */

  private static void zzzAdjustF2(final byte[] f2) {
    for (int i = 0; true; i++) {
      // --- check high nibble
      if (i > 0) {
        if (0 == (f2[i] & 0xf0)) {
          f2[i] |= (byte) 0xb0;
        } else {
          f2[i] ^= (byte) 0xb0;
          return;
        } // end fi
      } // end fi (skip first high nibble)

      // --- check low nibble
      if (0 == (f2[i] & 0x0f)) {
        f2[i] |= 0x0b;
      } else {
        f2[i] ^= 0x0b;
        return;
      } // end fi
    } // end For (i...)
  } // end method */

  /**
   * Signs a message according to ISO/IEC 9796-2:2010 DS2 from clause 9.
   *
   * @param message message to be signed
   * @param lengthSalt length of salt in byte
   * @param hash hash algorithm to be used
   * @param implicit if {@code TRUE} then the trailer field consists of one octet ´BC´, otherwise
   *     the trailer consists of two octets, see ISO/IEC 9796-2 clause 9.2.3
   * @param useMinimum true/false then ISO/IEC 9796-2 B.4/B.6 (with/without minimum) during
   *     signature calculation
   * @return array of byte-arrays with {signature, M2}
   * @throws IllegalArgumentException if {@code sLen < 1}, see ISO/IEC 9796-2 clause 9.2.2
   */
  public final byte[][] signIsoIec9796p2ds2(
      final byte[] message,
      final int lengthSalt,
      final EafiHashAlgorithm hash,
      final boolean implicit,
      final boolean useMinimum) {
    final var salt = new byte[lengthSalt];
    RNG.nextBytes(salt);

    // --- call subroutine with appropriate parameters
    return signIsoIec9796p2ds2(
        message,
        salt, // 9.3.1 §3, (N003.300)a.2
        hash,
        implicit,
        useMinimum);
  } // end method */

  /**
   * Signs a message according to ISO/IEC 9796-2:2010 DS2 from clause 9.
   *
   * <p><i><b>Note:</b> Intentionally this method is intended to be used with known test vectors for
   * test-purposes only, because specifying the salt is insecure.</i>
   *
   * @param message message to be signed
   * @param salt to be used during the signature generation
   * @param hash hash algorithm to be used
   * @param implicit if {@code TRUE} then the trailer field consists of one octet ´BC´, otherwise
   *     the trailer consists of two octets, see ISO/IEC 9796-2 clause 9.2.3
   * @param useMinimum true/false => ISO/IEC 9796-2 B.4/B.6 (with/without minimum) during signature
   *     calculation
   * @return array of byte-arrays with {signature, M2}
   * @throws IllegalArgumentException if sLen < 1, see ISO/IEC 9796-2 clause 9.2.2
   */
  @VisibleForTesting // otherwise: private
  /* package */ final byte[][] signIsoIec9796p2ds2(
      final byte[] message,
      final byte[] salt,
      final EafiHashAlgorithm hash,
      final boolean implicit,
      final boolean useMinimum) {
    if (salt.length < 1) { // NOPMD literal in if statement
      // ... see 9.2.2, Ls > 0 required
      throw new IllegalArgumentException("salt too short");
    } // end fi

    // --- call subroutine with appropriate parameters
    return signIsoIec9796p2ds3(message, salt, hash, implicit, useMinimum);
  } // end method */

  /**
   * Signs a message according to ISO/IEC 9796-2:2010 DS3 from clause 10.
   *
   * @param message message to be signed
   * @param salt to be used
   * @param hash hash algorithm to be used
   * @param implicit if {@code TRUE} then the trailer field consists of one octet ´BC´, otherwise
   *     the trailer consists of two octets, see ISO/IEC 9796-2 clause 9.2.3
   * @param useMinimum true/false then ISO/IEC 9796-2 B.4/B.6 (with/without minimum) during
   *     signature calculation
   * @return array of byte-arrays with {signature, M2}
   */
  public final byte[][] signIsoIec9796p2ds3(
      final byte[] message,
      final byte[] salt,
      final EafiHashAlgorithm hash,
      final boolean implicit,
      final boolean useMinimum) {
    // Note: according to clause 10 §2 Ls >= 0, i.e., Ls==0 is allowed for DS3 (but not for DS2)
    final var conLs = salt.length << 3; // length of salt in bit
    final var k = getModulusLengthBit();
    final var conLh = hash.getDigestLength() << 3; // length in bits
    final var mLength = message.length << 3; // length in bits
    final var c = k - conLh - conLs - 8 * (implicit ? 1 : 2) - 2; // capacity, 9.2.4
    final var delta = (((c - mLength) % 8) + 8) % 8; // 7.2.2, delta = (c - mLength) mod 8
    // note: for negative x it is (x mod 8) == (((x % 8) + 8) % 8) != (x % 8)
    final var cStar = Math.min(c - delta, mLength);

    // --- call subroutine with appropriate parameters
    return signIsoIec9796p2ds3(message, salt, hash, cStar, implicit, useMinimum);
  } // end method */

  /**
   * Signs a given message according to ISO/IEC 9796-2:2010 DS3.
   *
   * <p><i><b>Note:</b> Intentionally this method is intended to be used with known test vectors for
   * test-purposes only, because specifying the salt is insecure.</i>
   *
   * @param message message to be signed
   * @param salt salt
   * @param lenM1 C*, i.e., length of the recoverable part M1 in bits, should be a multiple of 8
   * @param hash hash algorithm to be used
   * @param implicit if {@code TRUE} then the trailer field consists of one octet ´BC´, otherwise
   *     the trailer consists of two octets, see ISO/IEC 9796-2 clause 9.2.3
   * @param useMinimum true/false => ISO/IEC 9796-2 B.4/B.6 (with/without minimum) during signature
   *     calculation
   * @return array of byte-arrays with {signature, M2}
   */
  @VisibleForTesting // otherwise: private
  /* package */ final byte[][] signIsoIec9796p2ds3(
      final byte[] message,
      final byte[] salt,
      final EafiHashAlgorithm hash,
      final int lenM1,
      final boolean implicit,
      final boolean useMinimum) {
    // Note: symbols and references are taken either from ISO/IEC 9796-2 or from
    //       [gemSpec_COS].

    final var numberOfPaddingBytes = implicit ? 1 : 2;
    final var conLs = salt.length << 3; // length of salt in bit
    final var k = getModulusLengthBit(); // modulus length, 9.2.1
    final var conLh = hash.getDigestLength() << 3; // length of hash in bits, clause 4
    final var c = k - conLh - conLs - 8 * numberOfPaddingBytes - 2; // capacity, 9.2.4

    if (c < 7) { // NOPMD literal
      // see clause 7.2.2
      throw new IllegalArgumentException("capacity too small");
    } // end fi

    // --- message allocation
    final var mSplit = messageAllocation(message, lenM1);
    final var m1 = mSplit[0];

    if ((m1.length << 3) > c) { // cStar > c?
      // see clause 7.2.2
      throw new IllegalArgumentException("M1 too long");
    } // end fi

    final var m2 = mSplit[1];
    final var hashM2 = hash.digest(m2);

    // --- calculations according to ISO/IEC 9797-2 resp. [gemSpec_COS]
    // 9.3.1 §2, (N003.300)a.1
    final var uC = AfiBigInteger.i2os(BigInteger.valueOf((long) m1.length << 3), 8);
    final var h =
        hash.digest(AfiUtils.concatenate(uC, m1, hashM2, salt)); // 9.3.1 §4, (N003.300)a.3

    // Note: P from 9.3.2 step 1 is intentionally not calculated because leading '0'
    //       bits are not necessary for D being a BigInteger
    // --- calculate D according to 9.3.2 step 2
    final var d =
        ONE.shiftLeft(m1.length << 3)
            .add(new BigInteger(1, m1))
            .shiftLeft(conLs)
            .add(new BigInteger(1, salt));

    // Note: Length of N in 9.3.2 step 3 is
    //       k + delta - Lh - 8t - 1 = (k + delta - 1) - Lh - 8t
    //                               = 8*modulusLength_Byte - Lh - 8t
    final var nOctet = getModulusLengthOctet();
    final var n =
        new BigInteger(
            1,
            hash.maskGenerationFunction(
                h, nOctet - hash.getDigestLength() - numberOfPaddingBytes, 0)); // 9.3.2 step 3
    final var dDash =
        d.xor(n)
            // 9.3.2 step 4
            .and(ONE.shiftLeft(k - 1 - conLh - (numberOfPaddingBytes << 3)).subtract(ONE));
    final var t = trailerField(hash, implicit); // 9.2.3, trailer field
    final var f =
        dDash
            .shiftLeft(conLh)
            .add(new BigInteger(1, h))
            .shiftLeft(numberOfPaddingBytes << 3)
            .add(t); // 9.3.2 step 5

    // 7.2.4, (N003.300)b.2
    final var s = useMinimum ? signIsoIec9796p2A4(f) : signIsoIec9796p2A6(f);
    final var signature = AfiBigInteger.i2os(s, nOctet);

    return new byte[][] {signature, m2};
  } // end method */

  /**
   * Calculates trailer field according to ISO/IEC 9796-2 clause 8.1.2 respectively 9.1.3.
   *
   * @param hash algorithm used
   * @param implicit if {@code TRUE} then the trailer field consists of one octet ´BC´, otherwise
   *     the trailer consists of two octets, see ISO/IEC 9796-2 clause 9.2.3
   * @return trailer field according to ISO/IEC 9796-2
   */
  private BigInteger trailerField(final EafiHashAlgorithm hash, final boolean implicit) {
    return implicit
        ? BigInteger.valueOf(0xbc)
        : BigInteger.valueOf(((long) hash.getHashId() << 8) + 0xcc);
  } // end method */

  /**
   * Returns a {@link String} representation of this instance.
   *
   * @return {@link String} representation of instance attributes
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final var result = new StringBuilder();
    final var n = AfiBigInteger.toHex(getModulus());
    final var e = AfiBigInteger.toHex(getPublicExponent());

    if (n.length() > (e.length() + 60)) {
      // ... n is way longer than e
      //     => left align n and e
      result.append(String.format("n  = %s%ne  = %s", n, e));
    } else {
      // ... n and e have "similar" length
      //     => right align n and e
      final var formatter = "n  = %s%ne  = %" + n.length() + 's';
      result.append(String.format(formatter, n, e));
    } // end else

    final var formatterD = "%nd  = %" + n.length() + 's';
    result.append(String.format(formatterD, AfiBigInteger.toHex(getPrivateExponent())));

    return result.toString();
  } // end method */

  /**
   * Returns the modulus.
   *
   * @return the modulus
   */
  @Override
  public final BigInteger getModulus() {
    return insModulus;
  } // end method */

  /**
   * Returns length of modulus in bit.
   *
   * @return length of modulus in bit
   */
  public final int getModulusLengthBit() {
    return getModulus().bitLength();
  } // end method */

  /**
   * Returns minimum number of bytes necessary for modulus.
   *
   * @return length of modulus in byte
   */
  public final int getModulusLengthOctet() {
    return (getModulusLengthBit() + 7) >> 3;
  } // end method */

  /**
   * Returns the public exponent.
   *
   * @return the public exponent
   */
  public BigInteger getPublicExponent() {
    return insPublicExponent;
  } // end method */

  /**
   * Returns the private exponent.
   *
   * @return the private exponent
   */
  @Override
  public BigInteger getPrivateExponent() {
    return insPrivateExponent;
  } // end method */

  /**
   * Returns public part of this private RSA key.
   *
   * @return public part
   */
  public final RsaPublicKeyImpl getPublicKey() {
    return new RsaPublicKeyImpl(getModulus(), getPublicExponent());
  } // end method */
} // end class
