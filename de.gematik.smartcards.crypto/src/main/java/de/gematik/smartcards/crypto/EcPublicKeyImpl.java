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

import static de.gematik.smartcards.crypto.AesKey.INFIMUM_CMAC_LENGTH;
import static de.gematik.smartcards.utils.EafiHashAlgorithm.SHA_256;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerBitString;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.KeySpec;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class implements {@link ECPublicKey} and {@link KeySpec}.
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
// Note 1: Spotbugs claims "EI_EXPOSE_REP",
//         Short message: Returning a reference to a mutable object value stored
//             in one of the object's fields exposes the internal representation
//             of the object.
//         Rational: False positive, insParams is immutable, see JavaDoc comment
//             in class AfiElcParameterSpec.
// Note 2: Spotbugs claims "SE_BAD_FIELD", i.e.,
//         Short message: This Serializable class defines a non-primitive instance
//             field which is neither transient, Serializable, or java.lang.Object.
//         Rational: It is correct that class ECPoint is not serializable.
//             The finding is suppressed, because this class implements proper
//             serialization and deserialization.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP", // see note 1
  "SE_BAD_FIELD" // see note 2
}) // */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public final class EcPublicKeyImpl implements ECPublicKey, KeySpec {

  /** Serial number randomly generated on 2021-05-07 15:03. */
  @Serial private static final long serialVersionUID = 6370806359797890188L; // */

  /** Domain parameter. */
  private final AfiElcParameterSpec insParams; // */

  // Note 1: SonarQube claims the following critical code smell on instance attribute "insW":
  //         "Make "insW" transient or serializable."
  // Note 2: This instance attribute is serializable, for an explanation see the note to the
  //         spotbugs finding "SE_BAD_FIELD".
  /** Point on an elliptic curve defining the public key. */
  private final ECPoint insW; // SE_BAD_FIELD */

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
  private volatile int insHashCode; // NOPMD volatile */

  /**
   * Comfort constructor, counterpart to {@link #getEncoded(EafiElcPukFormat)}.
   *
   * @param keyMaterial containing the private key and an OID referencing domain parameters
   * @param format identifying the coding
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>TlV structure is not in accordance to {@link EafiElcPukFormat}
   *       <li>underlying method {@link AfiElcUtils#os2p(byte[], AfiElcParameterSpec)} does so
   *       <li>underlying constructor {@link EcPublicKeyImpl#EcPublicKeyImpl(ECPoint,
   *           ECParameterSpec)} does so
   *     </ol>
   */
  public EcPublicKeyImpl(final BerTlv keyMaterial, final EafiElcPukFormat format) {
    this(tlv2ObjectArray(keyMaterial, format));
  } // end constructor */

  /**
   * Kind of copy constructor.
   *
   * @param publicKey public key from which attributes are taken
   */
  public EcPublicKeyImpl(final ECPublicKey publicKey) {
    this(publicKey.getW(), publicKey.getParams());
  } // end constructor */

  /**
   * Constructor with the specified parameter values.
   *
   * @param w the public point
   * @param dp the associated elliptic curve domain parameters
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>public point is not on curve
   *       <li>public point is {@link ECPoint#POINT_INFINITY}
   *     </ol>
   */
  public EcPublicKeyImpl(final ECPoint w, final ECParameterSpec dp) {
    insW = w;
    insParams = AfiElcParameterSpec.getInstance(dp);

    if (ECPoint.POINT_INFINITY.equals(w)) {
      throw new IllegalArgumentException("w is ECPoint.POINT_INFINITY");
    } // end fi

    if (!AfiElcUtils.isPointOnEllipticCurve(w, dp)) {
      throw new IllegalArgumentException("point not on curve");
    } // end fi
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param objects contains two objects:
   *     <ol>
   *       <li>{@link ECPoint} representing the public point P
   *       <li>{@link ECParameterSpec} representing domain parameters
   *     </ol>
   */
  private EcPublicKeyImpl(final Object... objects) {
    // Note 1: SonarQube claims the following major code smell on this constructor:
    //         "Remove this unused private "EcPrivateKeyImpl" constructor."
    // Note 2: This is a false positive, because this constructor is used.
    this((ECPoint) objects[0], (ECParameterSpec) objects[1]);
  } // end constructor */

  /**
   * Converts a signature given as a {@link DerSequence} into an octet string "R || S".
   *
   * @param sequence with two integers R and S
   * @param tau number of octets used to encode R and S
   * @return octet string representation of "R || S"
   * @throws NegativeArraySizeException if {@code tau} is negative
   * @throws NoSuchElementException if parameter {@code sequence} does not contain two {@link
   *     DerInteger} elements
   */
  public static byte[] fromSequence(final DerSequence sequence, final int tau) {
    final BigInteger r = sequence.getInteger(0).getDecoded();
    final BigInteger s = sequence.getInteger(1).getDecoded();

    return AfiUtils.concatenate(AfiBigInteger.i2os(r, tau), AfiBigInteger.i2os(s, tau));
  } // end method */

  /**
   * Converts an octet string with signature "R || S" into a {@link DerSequence}.
   *
   * <p>This function splits the given parameter {@code rs} evenly and converts the resulting two
   * halfs into {@link BigInteger}.
   *
   * @param rs octet string with signature "R || S"
   * @return a {@link DerSequence} with two elements:
   *     <ol>
   *       <li>element {@link DerInteger} with R
   *       <li>element {@link DerInteger} with S
   *     </ol>
   *
   * @throws IllegalArgumentException if number of octets in parameter {@code rs} is odd
   */
  public static DerSequence toSequence(final byte[] rs) {
    final int length = rs.length;

    if (1 == (length & 1)) { // NOPMD literal in if statement
      throw new IllegalArgumentException("odd number of octets");
    } // end fi
    // ... number of octets is even

    final int noOctets = length >> 1;
    final BigInteger r = new BigInteger(1, rs, 0, noOctets);
    final BigInteger s = new BigInteger(1, rs, noOctets, noOctets);

    return new DerSequence(List.of(new DerInteger(r), new DerInteger(s)));
  } // end method */

  /**
   * Enciphers given plain text according to [gemSpec_COS#(N091.650)c.3].
   *
   * <p>This function is a counterpart of {@link EcPrivateKeyImpl#decipher(ConstructedBerTlv)}.
   *
   * @param m plain text for enciphering
   * @return TLV object with cryptogram, see [gemSpec_COS#(N091.650)c.3.vi]
   * @throws NoSuchElementException if the curve is not listed in {@link AfiOid}
   */
  public ConstructedBerTlv encipher(final byte[] m) {
    final var dp = this.getParams();
    final var oid = dp.getOid(); // possibly throws NoSuchElementException
    // ... public key belongs to a supported curve

    final var prkEphemeral = new EcPrivateKeyImpl(dp); // (N004.500)b
    final var peA = prkEphemeral.getPublicKey().getW(); // (N004.500)c
    final byte[] kab = AfiElcUtils.sharedSecret(prkEphemeral, this); // (N004.500)d
    final byte[] kdEnc = AfiUtils.concatenate(kab, Hex.toByteArray("0000 0001"));
    final byte[] kdMac = AfiUtils.concatenate(kab, Hex.toByteArray("0000 0002"));
    final var kEnc = new AesKey(SHA_256.digest(kdEnc), 0, 32); // (N001.520)a
    final var kMac = new AesKey(SHA_256.digest(kdMac), 0, 32); // (N001.520)b
    final var t1 = kEnc.encipherCbc(new byte[16]); // (N004.500)e.2
    final var poA = AfiElcUtils.p2osUncompressed(peA, dp); // (N004.500)f
    final var c = kEnc.encipherCbc(kEnc.padIso(m), t1); // (N004.500)g
    final var t = kMac.calculateCmac(kMac.padIso(c), INFIMUM_CMAC_LENGTH); // (N004.500)h
    final var oidDo = new DerOid(oid); // (N091.650)c.3.ii
    final var keyDo =
        BerTlv.getInstance(0x7f49, List.of(BerTlv.getInstance(0x86, poA))); // (N091.650)c.3.iii
    final var cipherDo =
        BerTlv.getInstance(0x86, AfiUtils.concatenate(new byte[] {2}, c)); // (N091.650)c.3.iv
    final var macDo = BerTlv.getInstance(0x8e, t); // (N091.650)c.3.v

    return (ConstructedBerTlv)
        BerTlv.getInstance(0xa6, List.of(oidDo, keyDo, cipherDo, macDo)); // (N091.500)c.3.vi
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
    // Note 1: Because this class is a direct subclass of Object calling super.equals(...)
    //         would be wrong. Instead, special checks are performed.

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
    // ... obj is an instance of ElcPublicKey

    final EcPublicKeyImpl other = (EcPublicKeyImpl) obj;

    // --- compare primitive instance attributes (currently none)
    // -/-

    // --- compare reference types
    // ... assertion: instance attributes are never null
    return this.getParams().equals(other.getParams()) && this.getW().equals(other.getW());
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

    int result = insHashCode; // read attribute from main memory into thread local memory
    if (0 == result) {
      // ... obviously attribute hashCode has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      result = getParams().hashCode(); // start value
      final int hashCodeMultiplier = 31; // small prime number

      // --- take into account primitive instance attributes (currently none)
      // -/-

      // --- take into account reference types (currently only public point)
      result = result * hashCodeMultiplier + getW().hashCode();

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Returns the standard algorithm name for this key.
   *
   * @return "EC"
   * @see java.security.Key#getAlgorithm()
   */
  @Override
  public String getAlgorithm() {
    // see .../jdk-doc/docs/technotes/guides/security/StandardNames.html#KeyFactory
    return "EC";
  } // end method */

  /**
   * Returns the key in its primary encoding format.
   *
   * @return octet string representation of format {@link EafiElcPukFormat#X509}
   * @see java.security.Key#getEncoded()
   */
  @Override
  public byte[] getEncoded() {
    // same result as sun.security.ec.ECPublicKeyImpl.getEncoded()
    return getEncoded(EafiElcPukFormat.X509).getEncoded();
  } // end method */

  /**
   * Converts this object to {@link ConstructedBerTlv}.
   *
   * <p><i><b>Note:</b> This is the counterpart to {@link EcPublicKeyImpl#EcPublicKeyImpl(BerTlv,
   * EafiElcPukFormat)}.</i>
   *
   * @param format indicates the structure of the TLV-object.
   * @return returns this object as {@link ConstructedBerTlv}
   * @throws NoSuchElementException if there is no OID for domain parameters used by this key
   *     instance
   */
  public ConstructedBerTlv getEncoded(final EafiElcPukFormat format) {
    final AfiElcParameterSpec dp = getParams();
    final AfiOid oid = dp.getOid();
    final byte[] point = AfiElcUtils.p2osUncompressed(getW(), dp);

    if (EafiElcPukFormat.ISOIEC7816.equals(format)) {
      // ... ISO7816
      return (ConstructedBerTlv)
          BerTlv.getInstance(0x7f49, List.of(new DerOid(oid), BerTlv.getInstance(0x86, point)));
    } // end fi
    // ... not ISO7816
    //     => assume X509

    return new DerSequence(
        List.of(
            new DerSequence(List.of(new DerOid(AfiOid.ecPublicKey), new DerOid(oid))),
            new DerBitString(point)));
  } // end method */

  /**
   * Returns the name of the primary encoding format of this key.
   *
   * @return "X.509"
   * @see java.security.Key#getFormat()
   */
  @Override
  public String getFormat() {
    // see .../jdk-doc/docs/api/java/security/Key.html#getFormat%28%29
    // same result as sun.security.ec.ECPublicKeyImpl.getFormat()
    return "X.509";
  } // end method */

  /**
   * Returns the associated elliptic curve domain parameters.
   *
   * @return the EC domain parameters
   * @see java.security.interfaces.ECPublicKey#getParams()
   */
  @Override
  public AfiElcParameterSpec getParams() {
    return insParams; // EI_EXPOSE_REP
  } // end method */

  /**
   * Returns the public point W.
   *
   * @return the public point W
   * @see java.security.interfaces.ECPublicKey#getW()
   */
  @Override
  public ECPoint getW() {
    return insW;
  } // end method +/

  /**
   * Converts instance to {@link String}.
   *
   * <p>The result contains domain parameters (if possible just as OID) and the coordinates of the
   * point in hexadecimal representation.
   *
   * @return relevant instance attributes as {@link String}
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final AfiElcParameterSpec dp = getParams();
    final ECPoint point = getW();
    final String xp = Hex.toHexDigits(AfiElcUtils.fe2os(point.getAffineX(), dp));
    final String yp = Hex.toHexDigits(AfiElcUtils.fe2os(point.getAffineY(), dp));

    if (dp.hasOid()) {
      final AfiOid oid = dp.getOid();

      return String.format("Domain parameter OID = %s, P = (xp, yp) = ('%s', '%s')", oid, xp, yp);
    } // end fi

    return String.format("Domain parameter: %s%nP = (xp, yp) = ('%s', '%s')", dp, xp, yp);
  } // end method */

  /**
   * Verifies a signature according to BSI-TR-03111 v2.10 clause 4.2.1.2.
   *
   * <p><i><b>Note:</b> The hash function is implicitly chosen from domain parameter, i.e.:</i>
   *
   * <ol>
   *   <li><i>key length up to 256 bit: SHA-256</i>
   *   <li><i>key length up to 384 bit: SHA-384</i>
   *   <li><i>otherwise: SHA-512</i>
   * </ol>
   *
   * @param message for which signature is verified
   * @param signature R || S
   * @return true if signature is valid, false otherwise
   * @throws IllegalArgumentException if the number of octets in parameter {@code signature} is odd
   */
  public boolean verifyEcdsa(final byte[] message, final byte[] signature) {
    return verifyEcdsa(message, toSequence(signature));
  } // end method */

  /**
   * Verifies a signature according to BSI-TR-03111 v2.10 clause 4.2.1.2.
   *
   * <p><i><b>Note:</b> The hash function is implicitly chosen from domain parameter, i.e.:</i>
   *
   * <ol>
   *   <li><i>key length up to 256 bit: SHA-256</i>
   *   <li><i>key length up to 384 bit: SHA-384</i>
   *   <li><i>otherwise: SHA-512</i>
   * </ol>
   *
   * @param message for which signature is verified
   * @param signature as a {@link DerSequence} with two {@link DerInteger} R and S
   * @return true if signature is valid, false otherwise
   * @throws NoSuchElementException if parameter {@code signature} does not contain two {@link
   *     DerInteger} elements
   */
  public boolean verifyEcdsa(final byte[] message, final DerSequence signature) {
    final AfiElcParameterSpec dp = getParams();
    final int tau = dp.getTau();

    // --- step 0: calculate hash value,
    //     here it is converted to an integer, because that format is necessary in step 5
    final EafiHashAlgorithm hashFunction;
    if (tau <= 32) { // NOPMD literal in if statement
      // ... key length <= 256 bit
      hashFunction = SHA_256;
    } else if (tau <= 48) { // NOPMD literal in if statement
      // ... key length <= 384 bit
      hashFunction = EafiHashAlgorithm.SHA_384;
    } else {
      // ... key length > 384 bit
      hashFunction = EafiHashAlgorithm.SHA_512;
    } // end fi

    return verifyEcdsa(new BigInteger(1, hashFunction.digest(message)), signature);
  } // end method */

  /**
   * Verifies a signature according to BSI-TR-03111 v2.10 clause 4.2.1.2.
   *
   * @param htau hash value used for signature verification
   * @param signature R || S
   * @return true if signature is valid, false otherwise
   * @throws IllegalArgumentException if the number of octets in parameter {@code signature} is odd
   */
  public boolean verifyEcdsa(final BigInteger htau, final byte[] signature) {
    return verifyEcdsa(htau, toSequence(signature));
  } // end method */

  /**
   * Verifies a signature according to BSI-TR-03111 v2.10 clause 4.2.1.2.
   *
   * @param htau hash value used for signature verification
   * @param signature as a {@link DerSequence} with two {@link DerInteger} R and S
   * @return true if signature is valid, false otherwise
   * @throws NoSuchElementException if parameter {@code signature} does not contain two {@link
   *     DerInteger} elements
   */
  public boolean verifyEcdsa(final BigInteger htau, final DerSequence signature) {
    final BigInteger r = signature.getInteger(0).getDecoded();
    final BigInteger s = signature.getInteger(1).getDecoded();

    final var dp = getParams();
    final var n = dp.getOrder();

    // --- step 1: verify that r, s are in range [1, n-1]
    if ((1 != r.min(s).signum()) || (r.max(s).compareTo(n) >= 0)) {
      return false;
    } // end fi
    // ... r and s are in range [1, n-1]

    // --- step 2: compute s^-1
    final var sinv = s.modInverse(n);

    // --- step 3: compute u1 and u2
    final var u1 = sinv.multiply(htau).mod(n);
    final var u2 = sinv.multiply(r).mod(n);

    // --- step 4: compute Q = [u1]G + [u2]P
    final var pointQ =
        AfiElcUtils.add(
            AfiElcUtils.multiply(u1, dp.getGenerator(), dp),
            AfiElcUtils.multiply(u2, getW(), dp),
            dp);

    if (pointQ.equals(ECPoint.POINT_INFINITY)) {
      return false;
    } // end fi

    // --- step 5: compute v
    final var v = pointQ.getAffineX();

    return v.equals(r);
  } // end method */

  /**
   * Performs serialization and deserialization.
   *
   * @return object replacing instance of {@link EcPublicKeyImpl} used for serialization
   * @throws ObjectStreamException if underlying methods do so
   */
  @Serial
  private Object writeReplace() throws ObjectStreamException {
    return new Serializable() {
      /** Serial number randomly generated on 2021-05-07 16:28. */
      @Serial private static final long serialVersionUID = -1216097659709427313L; // */

      /** Proxy for point defining the public key. */
      private transient ECPoint insInnerW = getW(); // */

      /** Proxy for domain parameters. */
      private transient AfiElcParameterSpec insInnerParams = getParams(); // */

      /**
       * Method used during serialization.
       *
       * @param oos output stream to which objects are written
       * @throws IOException if underlying methods do so
       */
      @Serial
      private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.writeObject(insInnerW.getAffineX());
        oos.writeObject(insInnerW.getAffineY());
        oos.writeObject(insInnerParams);
      } // end inner method

      /**
       * Method used during deserialization.
       *
       * @param ois inputs stream from which objects are read
       * @throws IOException if underlying methods do so
       */
      @Serial
      private void readObject(final ObjectInputStream ois)
          throws IOException, ClassNotFoundException {
        final var x = (BigInteger) ois.readObject();
        final var y = (BigInteger) ois.readObject();
        insInnerW = new ECPoint(x, y);
        insInnerParams = (AfiElcParameterSpec) ois.readObject();
      } // end inner method

      /**
       * Returns object read from stream.
       *
       * @return deserialized object
       */
      @Serial
      private Object readResolve() {
        // Note 1: SonarQube claims the following critical code smell on this method:
        //         "Make this class "private" or elevate the visibility of "readResolve"."
        //         "Non-final classes which implement readResolve(), should not set its visibility
        //         to private since it will then be unavailable to child classes."
        // Note 2: This is a false positive, because the class is final.
        return new EcPublicKeyImpl(insInnerW, insInnerParams);
      } // end inner method
    }; // end inner class
  } // end method */

  /**
   * Extract key attributes from given data object.
   *
   * @param keyMaterial containing the public key and an OID referencing domain parameters
   * @param format identifying the coding
   * @return array of objects with key attributes, i.e.
   *     <ol>
   *       <li>{@link ECPoint} representing the public point
   *       <li>{@link AfiElcParameterSpec} representing domain parameters
   *     </ol>
   *
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>TlV structure is not in accordance to {@link EafiElcPukFormat}
   *       <li>underlying method {@link AfiElcUtils#os2p(byte[], AfiElcParameterSpec)} does so
   *     </ol>
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  private static Object[] tlv2ObjectArray(final BerTlv keyMaterial, final EafiElcPukFormat format) {
    try {
      if (EafiElcPukFormat.ISOIEC7816.equals(format)) {
        // ... ISO7816 format
        if (0x7f49 != keyMaterial.getTag()) { // NOPMD literal in if statement
          // ... wrong tag
          throw new IllegalArgumentException("invalid tag: '" + keyMaterial.getTagField() + '\'');
        } // end fi
        // ... tag is as expected
        final ConstructedBerTlv keyDo = (ConstructedBerTlv) keyMaterial;

        final DerOid oid = (DerOid) keyDo.get(DerOid.TAG).orElseThrow();
        final PrimitiveBerTlv p = keyDo.getPrimitive(0x86).orElseThrow();
        final AfiElcParameterSpec dp = AfiElcParameterSpec.getInstance(oid.getDecoded());

        return new Object[] {
          AfiElcUtils.os2p(p.getValueField(), dp), dp,
        };
      } // end fi
      // ... not ISO7816 format
      //     => assume X509 format

      if (DerSequence.TAG != keyMaterial.getTag()) {
        // ... wrong tag
        throw new IllegalArgumentException("invalid tag: '" + keyMaterial.getTagField() + '\'');
      } // end fi
      // ... tag is as expected
      final DerSequence keyDo = (DerSequence) keyMaterial;

      final DerSequence doOid = (DerSequence) keyDo.get(DerSequence.TAG).orElseThrow();
      final DerBitString bits = (DerBitString) keyDo.get(DerBitString.TAG).orElseThrow();
      final AfiOid oid1 = ((DerOid) doOid.get(DerOid.TAG, 0).orElseThrow()).getDecoded();
      final AfiOid oid2 = ((DerOid) doOid.get(DerOid.TAG, 1).orElseThrow()).getDecoded();
      final AfiElcParameterSpec dp = AfiElcParameterSpec.getInstance(oid2);

      if (AfiOid.ecPublicKey.equals(oid1)) {
        // ... correct first OID
        // --- snip leading octet

        return new Object[] {AfiElcUtils.os2p(bits.getDecoded(), dp), dp};
      } // end fi (correct OID?)

      throw new IllegalArgumentException("wrong OID");
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException("ERROR, invalid structure", e);
    } // end Catch (...)
  } // end method */
} // end class
