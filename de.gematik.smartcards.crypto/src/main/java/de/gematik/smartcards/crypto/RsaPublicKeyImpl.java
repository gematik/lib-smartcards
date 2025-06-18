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

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerBitString;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerNull;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.Serial;
import java.math.BigInteger;
import java.nio.BufferUnderflowException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class implements {@link RSAPublicKey} and {@link KeySpec}.
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
  "PMD.TooManyMethods",
})
public final class RsaPublicKeyImpl implements RSAPublicKey, KeySpec {

  /** Serial number randomly generated on 2022-07-25. */
  @Serial private static final long serialVersionUID = -7306119720068931503L; // */

  /** Random number generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Message in exceptions indicating that signature verification failed. */
  private static final String SIG_VER_FAILED = "signature verification failed"; // */

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
  private volatile int insHashCode; // NOPMD volatile not recommended */

  /** Modulus of the RSA key. */
  private final BigInteger insModulus; // */

  /** Public exponent of the RSA key. */
  private final BigInteger insPublicExponent; // */

  /**
   * Comfort constructor using key attributes.
   *
   * <p><i><b>Note:</b> Intentionally any value for {@code modulus} and {@code publicExponent} are
   * accepted by this constructor. Even negative numbers. Thus, it is the responsibility of the user
   * to assure that given values make sense.</i>
   *
   * @param modulus of RSA key (as in modulus n)
   * @param publicExponent public exponent (as in public exponent e)
   */
  public RsaPublicKeyImpl(final BigInteger modulus, final BigInteger publicExponent) {
    insModulus = modulus;
    insPublicExponent = publicExponent;
  } // end constructor */

  /**
   * Kind of copy constructor.
   *
   * @param publicKey public key from which attributes are taken
   */
  public RsaPublicKeyImpl(final RSAPublicKey publicKey) {
    this(publicKey.getModulus(), publicKey.getPublicExponent());
  } // end constructor */

  /**
   * Comfort constructor using an array of numbers.
   *
   * <p>The first number is used for modulus n. The second number is used for public exponent e. All
   * other elements in the array (if present) are ignored.
   *
   * @param values {n, e, ...}
   */
  private RsaPublicKeyImpl(final BigInteger... values) {
    // Note 1: SonarQube claims the following major code smell on this constructor:
    //         "Remove this unused private "EcPrivateKeyImpl" constructor."
    // Note 2: This is a false positive, because this constructor is used.
    this(values[0], values[1]);
  } // end constructor */

  /**
   * Comfort constructor using a TLV structure.
   *
   * @param keyMaterial is the given TLV-object
   * @param format indicates how to interpret the keyMaterial
   */
  public RsaPublicKeyImpl(final ConstructedBerTlv keyMaterial, final EafiRsaPukFormat format) {
    this(extract(keyMaterial, format));
  } // end constructor */

  /**
   * Extracts numbers from a TLV-structure, used by various constructors.
   *
   * @param keyMaterial is the given TLV-object
   * @param format indicates how to interpret parameter {@code keyMaterial}
   * @return array of numbers, i.e. {@code [n, e]}, the first element represents modulus n, the
   *     second element represents public exponent e
   * @throws IllegalArgumentException if TLV structure is not in conformance to {@link
   *     EafiRsaPukFormat}
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  /* package */ static BigInteger[] extract(
      final ConstructedBerTlv keyMaterial, final EafiRsaPukFormat format) {
    try {
      if (EafiRsaPukFormat.ISO7816.equals(format)) {
        // ... ISO7816 format
        if (0x7f49 != keyMaterial.getTag()) { // NOPMD literal in if statement
          // ... wrong tag
          throw new IllegalArgumentException(
              "invalid tag: '7f49' != '" + keyMaterial.getTagField() + '\'');
        } // end fi
        // ... tag is as expected

        final PrimitiveBerTlv n = keyMaterial.getPrimitive(0x81).orElseThrow();
        final PrimitiveBerTlv e = keyMaterial.getPrimitive(0x82).orElseThrow();

        return new BigInteger[] {
          new BigInteger(1, n.getValueField()), new BigInteger(1, e.getValueField())
        };

      } else if (EafiRsaPukFormat.PKCS1.equals(format)) {
        // ... PKCS1 format
        if (0x30 != keyMaterial.getTag()) { // NOPMD literal in if statement
          // ... wrong tag
          throw new IllegalArgumentException(
              "invalid tag: '30' != '" + keyMaterial.getTagField() + '\'');
        } // end fi
        // ... tag is as expected

        final DerInteger n = (DerInteger) keyMaterial.getPrimitive(DerInteger.TAG, 0).orElseThrow();
        final DerInteger e = (DerInteger) keyMaterial.getPrimitive(DerInteger.TAG, 1).orElseThrow();

        return new BigInteger[] {n.getDecoded(), e.getDecoded()};

      } else {
        // ... none of the other formats
        //     => assume RFC5280 format
        if (0x30 != keyMaterial.getTag()) { // NOPMD literal in if statement
          // ... wrong tag
          throw new IllegalArgumentException(
              "invalid tag: '30' != '" + keyMaterial.getTagField() + '\'');
        } // end fi
        // ... tag is as expected

        final DerSequence algId =
            (DerSequence) keyMaterial.getConstructed(DerSequence.TAG).orElseThrow();
        final DerOid algorithm = (DerOid) algId.getPrimitive(DerOid.TAG).orElseThrow();
        final AfiOid oid = algorithm.getDecoded();

        if (!AfiOid.rsaEncryption.equals(algorithm.getDecoded())) {
          throw new IllegalArgumentException("unexpected OID = " + oid);
        } // end fi (rsaEncryption?)
        // ... correct OID

        final var doNull = algId.getPrimitive(DerNull.TAG);
        if (doNull.isEmpty()) {
          throw new IllegalArgumentException("DO NULL is missing");
        } // end fi
        final DerBitString pkcs1 =
            (DerBitString) keyMaterial.getPrimitive(DerBitString.TAG).orElseThrow();

        return extract(
            (ConstructedBerTlv) BerTlv.getInstance(pkcs1.getDecoded()), EafiRsaPukFormat.PKCS1);
      } // end else
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException("ERROR, invalid structure", e);
    } // end Catch (...)
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
    // ... obj not same as this

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
    // ... obj is instance of RsaPublicKeyImpl

    final RsaPublicKeyImpl other = (RsaPublicKeyImpl) obj;

    // --- compare instance attributes
    // ... assertion: instance attributes are never null
    return this.getModulus().equals(other.getModulus())
        && this.getPublicExponent().equals(other.getPublicExponent());
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
  public String getAlgorithm() {
    return "RSA";
  } // end method */

  /**
   * Returns the key in its primary encoding format, here {@link EafiRsaPukFormat#RFC5280}.
   *
   * @return key in encoded format
   * @see java.security.Key#getEncoded()
   */
  @Override
  public byte[] getEncoded() {
    return getEncoded(EafiRsaPukFormat.RFC5280).getEncoded();
  } // end method */

  /**
   * Converts this object to {@link ConstructedBerTlv}.
   *
   * @param format indicates the structure of the TLV-object.
   * @return returns this object as {@link ConstructedBerTlv}
   */
  public ConstructedBerTlv getEncoded(final EafiRsaPukFormat format) {
    if (EafiRsaPukFormat.ISO7816.equals(format)) {
      // ... ISO7816 format
      return (ConstructedBerTlv)
          BerTlv.getInstance(
              0x7f49,
              List.of(
                  BerTlv.getInstance(0x81, AfiBigInteger.i2os(getModulus())),
                  BerTlv.getInstance(0x82, AfiBigInteger.i2os(getPublicExponent()))));
    } else if (EafiRsaPukFormat.PKCS1.equals(format)) {
      // ... PKCS1 format
      return new DerSequence(
          List.of(new DerInteger(getModulus()), new DerInteger(getPublicExponent())));
    } else {
      // ... none of the other formats
      //     => assuming RFC5280 format
      return new DerSequence(
          // sequence, SubjectPublicKeyInfo
          List.of(
              new DerSequence(
                  // sequence, AlgorithmIdentifier
                  List.of(new DerOid(AfiOid.rsaEncryption), DerNull.NULL)),
              // subjectPublicKey
              new DerBitString(getEncoded(EafiRsaPukFormat.PKCS1).getEncoded())));
    } // end else
  } // end method */

  /**
   * Returns the name of the primary encoding format of this key, here {@code "X.509"}.
   *
   * <p>See <a
   * href="https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/security/Key.html#getFormat()">getFormat()</a>.
   *
   * @return {@link String} with content "X.509"
   * @see java.security.Key#getFormat()
   */
  @Override
  public String getFormat() {
    return "X.509";
  } // end method */

  /**
   * Extracts trailer field from message representative, see ISO/IEC 9796-2:2010, 8.2.2, 9.2.3.
   *
   * @param messRep message representative F* during signature verification
   * @param hash function used for signature production
   * @return trailer field of F*
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>LSByte in F* is not in set {'bc', 'cc'}
   *       <li>LSByte in F* is 'cc' but hash-algorithm indicated in penultimate byte of F* does not
   *           indicate the same hash-function as parameter {@code hash}
   *     </ol>
   */
  @VisibleForTesting // otherwise: private
  /* package */ static int getTrailer(final BigInteger messRep, final EafiHashAlgorithm hash) {
    final int trailer = messRep.intValue() & 0xffff;
    // spotless:off
    switch (trailer & 0xff) {
      case 0xbc:
        return 0xbc;

      case 0xcc: {
        if (hash.getHashId() == (trailer >> 8)) {
          return trailer;
        } // end fi
        // ... trailer does not match hashId
      } // end 0xcc, intentionally fall through

      default:
        throw new IllegalArgumentException(SIG_VER_FAILED);
    } // end Switch
    // spotless:on
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

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Implements RSA Encryption Primitive (RSAEP) from PKCS#1 v2.2 section 5.1.1.
   *
   * @param m message representative, an integer between 0 and {@code n - 1}
   * @return cipher text representative, an integer between 0 and {@code n - 1}
   * @throws IllegalArgumentException if {@code 0 &le; m &lt; n} is not fulfilled
   */
  public BigInteger pkcs1RsaEp(final BigInteger m) {
    // PKCS#1 v2.2, clause 5.1.1, step 1
    if ((m.signum() < 0) || (m.compareTo(getModulus()) >= 0)) {
      throw new IllegalArgumentException("signature/message representative out of range");
    } // end fi
    // ... 0 <= m < n

    return m.modPow(getPublicExponent(), getModulus());
  } // end method */

  /**
   * Enciphers the given message according to RSAES-OAEP-ENCRYPT from PKCS#1 v2.2 subsection 7.1.1.
   *
   * <p>Contrary to {@link #pkcs1RsaEsOaepEncrypt(byte[], byte[], EafiHashAlgorithm)} this method
   * uses an empty {@code label}.
   *
   * @param m message to be encrypted, an octet string of length {@code mLen}, with {@code mLen < k
   *     - 2 hLen - 2}
   * @param hashAlgorithm hash function to be used
   * @return cipher text, an octet string of length {@link #getModulusLengthOctet()}
   * @throws IllegalArgumentException if message <i>m</i> is too long
   */
  public byte[] pkcs1RsaEsOaepEncrypt(final byte[] m, final EafiHashAlgorithm hashAlgorithm) {
    return pkcs1RsaEsOaepEncrypt(m, AfiUtils.EMPTY_OS, hashAlgorithm);
  } // end method */

  /**
   * Enciphers the given message according to RSAES-OAEP-ENCRYPT from PKCS#1 v2.2 subsection 7.1.1.
   *
   * @param m message to be encrypted, an octet string of length {@code mLen}, with {@code mLen < k
   *     - 2 hLen - 1}
   * @param l label to be associated with the message
   * @param hashAlgorithm hash function to be used
   * @return cipher text an octet string of length <i>k</i>
   * @throws IllegalArgumentException if the message is too long
   */
  public byte[] pkcs1RsaEsOaepEncrypt(
      final byte[] m, final byte[] l, final EafiHashAlgorithm hashAlgorithm) {
    final int k = getModulusLengthOctet();
    final int hLen = hashAlgorithm.getDigestLength();

    // --- PKCS#1 clause 7.1.1 step 1.a
    // Note 1: Intentionally label l is not checked whether it exceeds the input
    //         limitation of hash algorithm, because this limitation is beyond
    //         the size of a java byte-array.

    // --- PKCS#1 clause 7.1.1 step 1.b
    if (m.length > k - 2 * hLen - 2) {
      throw new IllegalArgumentException("message too long");
    } // end fi

    // --- PKCS#1 clause 7.1.1 step 2
    final byte[] lHash = hashAlgorithm.digest(l); // step 2.a
    // step 2.b, intentionally nothing, the implementation here implicitly contains PS
    final byte[] db = new byte[k - hLen - 1]; // step 2.c
    System.arraycopy(lHash, 0, db, 0, hLen);
    db[db.length - m.length - 1] = (byte) 0x01;
    System.arraycopy(m, 0, db, db.length - m.length, m.length);
    final byte[] seed = RNG.nextBytes(hLen); // step 2.d
    final byte[] dbMask = hashAlgorithm.maskGenerationFunction(seed, db.length, 0); // step 2.e
    final byte[] maskedDb = new byte[db.length];
    for (int i = maskedDb.length; i-- > 0; ) { // NOPMD assignment in operand
      maskedDb[i] = (byte) (db[i] ^ dbMask[i]); // step 2.f
    } // end For (i...)
    final byte[] seedMask = hashAlgorithm.maskGenerationFunction(maskedDb, hLen, 0); // step 2.g
    final byte[] maskedSeed = new byte[hLen];
    for (int i = hLen; i-- > 0; ) { // NOPMD assignment in operand
      maskedSeed[i] = (byte) (seed[i] ^ seedMask[i]); // step 2.h
    } // end For (i...)

    // step 2.i
    // Note: Prefix '00' is here not necessary, because em is converted to a positive BigInteger.
    final byte[] em = AfiUtils.concatenate(maskedSeed, maskedDb);

    // --- PKCS#1 clause 7.1.1 step 3: RSA encryption
    final BigInteger p = new BigInteger(1, em); // step 3.a
    final BigInteger c = pkcs1RsaEp(p); // step 3.b

    return AfiBigInteger.i2os(c, k); // steps 3.c and 4
  } // end method */

  /**
   * Enciphers the given message according to RSAES-PKCS1-V1_5-ENCRYPT from PKCS#1 v2.2 subsection
   * 7.2.1.
   *
   * <p><i><b>Note:</b></i>k <i>denotes the length in octets of the modulus.</i>
   *
   * @param m message to be encrypted, an octet string ol length <i>mLen</i>, where <i>mLen</i>
   *     {@code &lt; k - 11}
   * @return cipher text, an octet string of length <i>k</i>
   * @throws IllegalArgumentException if the message is too long
   */
  public byte[] pkcs1RsaEsPkcs1V15Encrypt(final byte[] m) {
    final int k = getModulusLengthOctet();

    // --- PKCS#1, clause 7.2.1, step 1
    final int psLength = k - m.length - 3;
    if (psLength < 8) { // NOPMD literal in if statement
      throw new IllegalArgumentException("message too long");
    } // end fi

    // --- PKCS#1, clause 7.2.1, step 2.a
    final byte[] ps = RNG.nextBytes(psLength);
    for (int i = ps.length; i-- > 0; ) { // NOPMD assignment in operands
      while (0 == ps[i]) {
        ps[i] = (byte) RNG.nextIntClosed(0x01, 0xff);
      } // end While
    } // end For (i...)

    final BigInteger em =
        new BigInteger("0002" + Hex.toHexDigits(ps) + "00" + Hex.toHexDigits(m), 16);
    final BigInteger ciphertext = pkcs1RsaEp(em);

    return AfiBigInteger.i2os(ciphertext, k);
  } // end method */

  /**
   * Verifies a signature according to RSASSA-PKCS1-V1_5-VERIFY from PKCS#1 v2.2 subsection 8.2.2.
   *
   * @param message message protected by given signature
   * @param signature signature to be verified
   * @return hash algorithm used during signature creation
   * @throws IllegalArgumentException if signature verification fails
   */
  @SuppressWarnings("PMD.EmptyCatchBlock")
  public EafiHashAlgorithm pkcs1RsaSsaPkcs1V15Verify(final byte[] message, final byte[] signature) {
    try {
      final var emLen = getModulusLengthOctet();

      // --- PKCS#1, clause 8.2.2, step 1
      if (signature.length == emLen) {
        // ... signature has the correct length

        // Note 1: The following line possibly throws IllegalArgumentException if
        //         signature (as a BigInteger) is not in range [0, n - 1]
        final var dsi = pkcs1RsaVp1(new BigInteger(1, signature));
        final var dsiOctet = AfiBigInteger.i2os(dsi);
        int index = 0;
        while (dsiOctet[index] != 0) {
          index++;
        } // end While (separator not reached)
        final var tlvOctet = Arrays.copyOfRange(dsiOctet, index + 1, dsiOctet.length);
        final var sequence = (ConstructedBerTlv) BerTlv.getInstance(tlvOctet);
        final var algoId = sequence.getConstructed(DerSequence.TAG).orElseThrow();
        final var oidDo = (DerOid) algoId.getPrimitive(DerOid.TAG).orElseThrow();
        final var hash = EafiHashAlgorithm.getInstance(oidDo.getDecoded());
        final var em = new BigInteger(1, Pkcs1Utils.pkcs1EmsaV15(message, emLen, hash));

        if (0 == dsi.compareTo(em)) {
          return hash;
        } // end fi
        // ... DSI != expected value
      } // end fi
    } catch (ArithmeticException // strange TLV structure
        | ArrayIndexOutOfBoundsException // no '00' separator octet
        | BufferUnderflowException // strange TLV structure
        | ClassCastException // strange TLV structure
        | IllegalArgumentException // signature out of range
        | NoSuchElementException e // strange TLV structure
    ) {
      // intentionally empty
    } // end Catch (...)

    throw new IllegalArgumentException(SIG_VER_FAILED);
  } // end method */

  /**
   * Verifies a signature according to RSASSA-PSS-VERIFY from PKCS#1 v2.2 subsection 8.1.2.
   *
   * <p>This method just calls {@link #pkcs1RsaSsaPssVerify(byte[], byte[], int, EafiHashAlgorithm)}
   * with a salt-length equal to digest-length.
   *
   * @param m the message
   * @param s the signature to be verified
   * @param hash indicates the hash algorithm used by the signer
   * @throws IllegalArgumentException if signature verification fails
   */
  public void pkcs1RsaSsaPssVerify(final byte[] m, final byte[] s, final EafiHashAlgorithm hash) {
    pkcs1RsaSsaPssVerify(m, s, hash.getDigestLength(), hash);
  } // end method */

  /**
   * Verifies a signature according to RSASSA-PSS-VERIFY from PKCS#1 v2.2 subsection 8.1.2.
   *
   * @param m the message
   * @param s the signature to be verified
   * @param lengthSalt length of salt in byte
   * @param hash indicates the hash algorithm used by the signer
   * @throws IllegalArgumentException if signature verification fails
   */
  public void pkcs1RsaSsaPssVerify(
      final byte[] m, final byte[] s, final int lengthSalt, final EafiHashAlgorithm hash) {
    final byte[] m1 = verifyIsoIec9796p2ds2(s, m, lengthSalt, hash);

    if (m1.length > 0) {
      throw new IllegalArgumentException(SIG_VER_FAILED);
    } // end fi
  } // end method */

  /**
   * Verification primitive according to RSAVP1 from PKCS#1 v2.2 subsection 5.2.2.
   *
   * <p><i><b>Note:</b> This function is identical to {@link #pkcs1RsaEp(BigInteger)}.</i>
   *
   * @param s signature representative, an integer between 0 and {@code n - 1}
   * @return message representative, an integer between 0 and {@code n - 1}
   * @throws IllegalArgumentException if {@code 0 &le; s &lt; n} is not fulfilled
   */
  public BigInteger pkcs1RsaVp1(final BigInteger s) {
    return pkcs1RsaEp(s);
  } // end method */

  /**
   * Signature opening function in accordance to ISO/IEC 9796-2:2010.
   *
   * <p><i><b>Note:</b> this function combines B.5 and B.7</i>
   *
   * @param signature signature to be opened
   * @return message representative F*
   */
  @VisibleForTesting
  // otherwise: private
  /* package */ BigInteger signatureOpeningIsoIec9796p2(final BigInteger signature) {
    // Note 1: Unless otherwise noted, the references given in this
    //         function are valid within ISO/IEC 9796-2:2010.
    // Note 2: Intentionally the check from B.5 §1 is not performed,
    //         i.e. S may have the same bit-length as modulus.

    final var jStar = pkcs1RsaEp(signature); // see B.5 §1, B.7 §3

    // Note 3: instead of testing the rightmost nibble in J* here only the
    //         LSBit is tested. See B.5 §3 case public exponent is odd.
    return jStar.testBit(0) ? getModulus().subtract(jStar) : jStar;
  } // end method */

  /**
   * Verifies given signature and returns recovered part of message if signature is okay.
   *
   * <p>Message recovery is in accordance to ISO/IEC 9796-2:2010 DS1 from clause 8.3 together with
   * signature opening function from ISO/IEC 9796-2:2010 B.5 (if necessary 'minimum' is taken into
   * account).
   *
   * @param signature signature giving message recovery
   * @param message2 unrecoverable part of the message (possibly empty)
   * @param hash hash algorithm to be used
   * @return recoverable part of the message
   * @throws IllegalArgumentException if signature verification fails
   */
  public byte[] verifyIsoIec9796p2ds1(
      final byte[] signature, final byte[] message2, final EafiHashAlgorithm hash) {
    final var fStar = signatureOpeningIsoIec9796p2(new BigInteger(1, signature));
    // ... rightmost nibble of F* is '1100'=0xC
    // Note: signatureOpening_ISO9796_2(BigInteger)-method does not test the
    //       least significant nibble but the least significant bit only.

    final var tnoOfPaddingBytes = (0xbc == getTrailer(fStar, hash)) ? 1 : 2;
    // ... hash-algorithm possibly indicated in trailer matches hash-algorithm from parameter "hash"

    final var k = getModulusLengthBit();
    final var shift = (8 - (k % 8)) % 8;
    final var f2 = fStar.shiftLeft(shift).toByteArray();
    final boolean isTotalRecovery = (0 == (f2[0] & 0x20));

    // --- number of padding bits
    var numPaddingBits = 1;
    if (0 == (f2[0] & 0x10)) {
      for (var i = 0; numPaddingBits > 0; i++) {
        numPaddingBits = zzzCheckHighNibble(f2, i, numPaddingBits);

        if (numPaddingBits > 0) {
          numPaddingBits = zzzCheckLowNibble(f2, i, numPaddingBits);
        } // end fi
      } // end For (i...)

      numPaddingBits *= -1;
    } // end fi

    // --- clear high nibble
    f2[0] &= 0x0f;

    // --- continue if total recovery or less than 9 padding bits, see 8.4, §6
    if (isTotalRecovery || (numPaddingBits < 9)) {
      final var f1 = (new BigInteger(1, f2)).shiftRight(shift + 8 * tnoOfPaddingBytes);
      final var conLh = hash.getDigestLength() << 3; // length in bits
      final var cStar = k - 3 - 8 * tnoOfPaddingBytes - conLh - numPaddingBits;
      final var m1 = new byte[cStar >> 3];
      final var h = new byte[conLh >> 3];
      final var f3 = AfiBigInteger.i2os(f1, m1.length + h.length);
      System.arraycopy(f3, 0, m1, 0, m1.length);
      System.arraycopy(f3, m1.length, h, 0, h.length);
      final var hDash = hash.digest(AfiUtils.concatenate(m1, message2));

      if (Arrays.equals(h, hDash)) {
        return m1;
      } // end fi
    } // end fi

    throw new IllegalArgumentException(SIG_VER_FAILED);
  } // end method */

  /**
   * Adjust high nibble of octet in {@code f2} indexed by parameter {@code i}.
   *
   * <p>The only purpose of this method is to reduce the complexity of the one and only method
   * calling this method.
   *
   * @param f2 octet string representation of F2
   * @param i index to current octet in focus
   * @param numPaddingBits discovered so far
   * @return new amount of padding bits, if negative, then no more padding bits
   */
  private static int zzzCheckHighNibble(final byte[] f2, final int i, int numPaddingBits) {
    // --- check high nibble
    if (i > 0) {
      if (0xb0 == (f2[i] & 0xf0)) { // NOPMD literal in if statement
        f2[i] &= 0x0f;
        numPaddingBits += 4; // NOPMD reassignment of parameter
      } else {
        f2[i] ^= (byte) 0xb0;
        if ((0x80 & f2[i]) == 0x80) { // NOPMD literal in if statement
          numPaddingBits += 1;
          f2[i] &= 0x7f;
        } else if ((0x40 & f2[i]) == 0x40) { // NOPMD literal in if statement
          numPaddingBits += 2;
          f2[i] &= 0x3f;
        } else if ((0x20 & f2[i]) == 0x20) { // NOPMD literal in if statement
          numPaddingBits += 3;
          f2[i] &= 0x1f;
        } else {
          numPaddingBits += 4;
          f2[i] &= 0x0f;
        } // end fi
        numPaddingBits *= -1;
      } // end fi
    } // end fi (skip first high nibble)

    return numPaddingBits;
  } // end method */

  /**
   * Adjust low nibble of octet in {@code f2} indexed by parameter {@code i}.
   *
   * <p>The only purpose of this method is to reduce the complexity of the one and only method
   * calling this method.
   *
   * @param f2 octet string representation of F2
   * @param i index to current octet in focus
   * @param numPaddingBits discovered so far
   * @return new amount of padding bits, if negative, then no more padding bits
   */
  private static int zzzCheckLowNibble(final byte[] f2, final int i, int numPaddingBits) {
    // --- check low nibble
    if (0xb == (f2[i] & 0x0f)) { // NOPMD literal in if statement
      f2[i] &= (byte) 0xf0;
      numPaddingBits += 4; // NOPMD reassigning parameter
    } else {
      f2[i] ^= 0x0b;
      if ((0x08 & f2[i]) == 0x08) { // NOPMD literal in if statement
        numPaddingBits += 1;
        f2[i] &= (byte) 0xf7;
      } else if ((0x04 & f2[i]) == 0x04) { // NOPMD literal in if statement
        numPaddingBits += 2;
        f2[i] &= (byte) 0xf3;
      } else if ((0x02 & f2[i]) == 0x02) { // NOPMD literal in if statement
        numPaddingBits += 3;
        f2[i] &= (byte) 0xf1;
      } else {
        numPaddingBits += 4;
        f2[i] &= (byte) 0xf0;
      } // end fi
      numPaddingBits *= -1;
    } // end fi

    return numPaddingBits;
  } // end method */

  /**
   * Verifies given signature and returns recovered part of message in case signature is okay.
   *
   * <p>This method just calls {@link #verifyIsoIec9796p2ds2(byte[], byte[], int,
   * EafiHashAlgorithm)} with a salt-length equal to digest-length.
   *
   * @param signature signature giving message recovery
   * @param message2 unrecoverable part of a message (possibly empty)
   * @param hash hash algorithm to be used
   * @return recoverable part of the message
   * @throws IllegalArgumentException if signature verification fails
   */
  public byte[] verifyIsoIec9796p2ds2(
      final byte[] signature, final byte[] message2, final EafiHashAlgorithm hash) {
    return verifyIsoIec9796p2ds2(signature, message2, hash.getDigestLength(), hash);
  } // end method */

  /**
   * Verifies given signature and returns recovered part of message in case signature is okay.
   *
   * <p>Message recovery is in accordance to ISO/IEC 9796-2:2010 DS2 from clause 9.4 together with
   * signature opening function from ISO/IEC 9796-2:2010 B.5 (if necessary 'minimum' is taken into
   * account).
   *
   * @param signature signature giving message recovery
   * @param message2 unrecoverable part of a message (possibly empty)
   * @param lengthSalt length of salt in octet
   * @param hash hash algorithm to be used
   * @return recoverable part of a message
   * @throws IllegalArgumentException if signature verification fails
   */
  public byte[] verifyIsoIec9796p2ds2(
      final byte[] signature,
      final byte[] message2,
      final int lengthSalt,
      final EafiHashAlgorithm hash) {
    // Note: If not stated otherwise, references are valid in ISO/IEC 9796-2:2010

    final var k = getModulusLengthBit(); // see 9.2.1
    final var conLh = hash.getDigestLength() << 3; // see 9.3.1, §4
    final var fStar = signatureOpeningIsoIec9796p2(new BigInteger(1, signature)); // see 7.3.2
    // ... rightmost nibble of F* is '1100'=0xC
    // Note: signatureOpening_ISO9796_2(BigInteger) method do not test the
    //       least significant nibble but the least significant bit only.

    final var tnoOfPaddingBytes = (0xbc == getTrailer(fStar, hash)) ? 1 : 2; // 9.4, §1

    // --- 9.4, §3 step 1: ignored, because adding '0' bits to the left of F* is not necessary

    // --- 9.4, §3 step 2: split F* into D'* and H*
    // remove trailer and convert to octet string
    final var dH = AfiBigInteger.i2os(fStar.shiftRight(tnoOfPaddingBytes << 3));
    final var hStar = new byte[hash.getDigestLength()];
    final var dDashStar = new byte[dH.length - hStar.length];
    System.arraycopy(dH, 0, dDashStar, 0, dDashStar.length);
    System.arraycopy(dH, dDashStar.length, hStar, 0, hStar.length);

    // Note: Length of N* in 9.4, §3 step 3 is
    //          (k + delta - Lh - 8t - 1)
    //       = ((k + delta - 1) - Lh - 8t)
    //       = 8*modulusLength_Byte - Lh - 8t
    // 9.4, §3 step 3
    final var nStar =
        new BigInteger(
            1,
            hash.maskGenerationFunction(
                hStar, getModulusLengthOctet() - hash.getDigestLength() - tnoOfPaddingBytes, 0));

    // --- 9.4, §3 step 4 and step 5
    final var dStar =
        new BigInteger(1, dDashStar)
            .xor(nStar)
            // 9.2.2 §3 step 4
            .and(ONE.shiftLeft(k - 1 - conLh - (tnoOfPaddingBytes << 3)).subtract(ONE));

    // --- return indication that verification has failed due to no first '1' bit
    if (dStar.compareTo(ZERO) == 0) {
      throw new IllegalArgumentException(SIG_VER_FAILED); // 9.4, §3 step 6
    } // end fi

    final var sStar = AfiBigInteger.i2os(dStar, lengthSalt);
    final var m1 = dStar.shiftRight(lengthSalt << 3); // remove S* from D*,
    // Note: First '1' bit still present

    // --- 9.4, §3 step 6 including removal of first '1' bit
    final var m1Star = AfiBigInteger.i2os(m1, m1.bitLength() >> 3);

    // 9.4, §3 step 7
    final var c = AfiBigInteger.i2os(BigInteger.valueOf((long) m1Star.length << 3), 8);

    // 9.4, step 8
    final var h = hash.digest(AfiUtils.concatenate(c, m1Star, hash.digest(message2), sStar));

    if (Arrays.equals(hStar, h)) {
      return m1Star;
    } // end fi

    throw new IllegalArgumentException(SIG_VER_FAILED);
  } // end method */

  /**
   * Verifies given signature and returns recovered part of message in case signature is okay.
   *
   * <p>Message recovery is in according to ISO/IEC 9796-2 DS3 from clause 10 together with
   * signature opening function from ISO/IEC 9796-2 (if necessary 'minimum' is taken into account).
   *
   * @param signature signature giving message recovery
   * @param message2 unrecoverable part of message, may be empty
   * @param lengthSalt length of salt in byte
   * @param hash hash algorithm to be used
   * @return recoverable part of the message
   * @throws IllegalArgumentException if signature verification fails
   */
  public byte[] verifyIsoIec9796p2ds3(
      final byte[] signature,
      final byte[] message2,
      final int lengthSalt,
      final EafiHashAlgorithm hash) {
    return verifyIsoIec9796p2ds2(signature, message2, lengthSalt, hash);
  } // end method */

  /**
   * Converts this object to a printable string.
   *
   * @return human-readable {@link String} representation of this RSA key
   * @see Object#toString()
   */
  @Override
  public String toString() {
    final String n = AfiBigInteger.toHex(getModulus());
    final String e = AfiBigInteger.toHex(getPublicExponent());

    if (n.length() > (e.length() + 60)) {
      // ... n is way longer than e
      //     => left align n and e
      return String.format("n  = %s%ne  = %s", n, e);
    } else {
      // ... n and e have "similar" length
      //     => right align n and e
      final var format = String.format("n  = %%s%%ne  = %%%ds", n.length());
      return String.format(format, n, e);
    } // end else
  } // end method */

  /**
   * Converts this object to a printable string.
   *
   * <p>Examples for a public key with {@code {n, e} = {323, 13} = {'143', 'd'}}:
   *
   * <ol>
   *   <li>
   *       <pre>toString(0, 0) encodes:
   *       n = 143
   *       e = d
   *   </pre>
   *   <li>
   *       <pre>toString(3, 2) encodes:
   *       n =    143
   *       e =    d
   *   </pre>
   *   <li>
   *       <pre>toString(4, 4) encodes:
   *       n =      143
   *       e =        d
   *   </pre>
   * </ol>
   *
   * @param noOctetModulus number of octet in output string for modulus n
   * @param noOctetPublicExponent number of octets in output string for public exponent e
   * @return human-readable {@link String} representation of this RSA key
   */
  public String toString(final int noOctetModulus, final int noOctetPublicExponent) {
    // --- convert parameter to hex-digits
    final String nHex1 = getModulus().toString(16);
    final String eHex1 = getPublicExponent().toString(16);

    // --- create the format string
    //     e.g., for {noOctetModulus, noOctetPublicExponent} = {4, 3} the format string is
    //     "n  = %8s%ne  = %6s"
    final String format =
        String.format(
            "n  = %s%%ne  = %s",
            noOctetModulus > 0 ? String.format("%%%ds", noOctetModulus << 1) : "%s",
            noOctetPublicExponent > 0 ? String.format("%%%ds", noOctetPublicExponent << 1) : "%s");

    return String.format(format, nHex1, eHex1);
  } // end method */

  /**
   * Returns the modulus.
   *
   * @return the modulus
   */
  @Override
  public BigInteger getModulus() {
    return insModulus;
  } // end method */

  /**
   * Returns length of modulus in bit.
   *
   * @return length of modulus in bit
   */
  public int getModulusLengthBit() {
    return getModulus().bitLength();
  } // end method */

  /**
   * Returns minimum number of bytes necessary for modulus.
   *
   * @return length of modulus in byte
   */
  public int getModulusLengthOctet() {
    return (getModulusLengthBit() + 7) >> 3;
  } // end method */

  /**
   * Returns the public exponent.
   *
   * @return the public exponent
   */
  @Override
  public BigInteger getPublicExponent() {
    return insPublicExponent;
  } // end method */
} // end class
