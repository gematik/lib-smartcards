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

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerOctetString;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerSequence;
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
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.KeySpec;
import java.util.List;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class implements {@link ECPrivateKey} and {@link KeySpec}.
 *
 * <p>From the perspective of this class
 *
 * <ol>
 *   <li>instances are immutable value-types. Thus, {@link Object#equals(Object) equals()}, {@link
 *       Object#hashCode() hashCode()} are overwritten. {@link Object#clone() clone()} is not
 *       overwritten, because instances are immutable.
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
//         Rational: That finding is suppressed because insParams and
//             insPublicPart are immutable, see JavaDoc comment of that classes.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP" // see note 1
}) // */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.GodClass", "PMD.TooManyMethods"})
public final class EcPrivateKeyImpl implements ECPrivateKey, KeySpec {

  /** Serial number randomly generated on 2021-05-07 12:15. */
  @Serial private static final long serialVersionUID = -5907461608410604348L; // */

  /** Message in exception in case of invalid tag. */
  @VisibleForTesting // otherwise = private
  /* package */ static final String INVALID_TAG = "invalid tag: '"; // */

  /** Message in exceptions if the version is unknown. */
  @VisibleForTesting // otherwise = private
  /* package */ static final String UNKNOWN_VERSION = "unknown version: "; // */

  /** Random number generator. */
  private static final SecureRandom RNG = new SecureRandom(); // NOPMD same name as method */

  /** Domain parameter. */
  private final AfiElcParameterSpec insParams; // */

  /** Public part corresponding to this private key. */
  private final EcPublicKeyImpl insPublicPart; // */

  /**
   * Integer defining the private key.
   *
   * <p>Intentionally this is a final instance attribute, because instances SHALL be immutable. It
   * follows that destroying is not possible, see {@link #destroy()}.
   */
  private final BigInteger insS; // */

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
   * Comfort constructor, counterpart to {@link #getEncoded(EafiElcPrkFormat)}.
   *
   * @param keyMaterial containing the private key and an OID referencing domain parameters
   * @param format identifying the coding
   * @throws ArithmeticException if cofactor is not in range of {@link Integer}
   * @throws ArrayIndexOutOfBoundsException if {@link EafiElcPrkFormat#PKCS1} format is used
   * @throws IllegalArgumentException if TLV structure is not in accordance to {@link
   *     EafiElcPrkFormat}
   */
  public EcPrivateKeyImpl(final BerTlv keyMaterial, final EafiElcPrkFormat format) {
    this(tlv2ObjectArray(keyMaterial, format));
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param d private key
   * @param dp the associated elliptic curve domain parameters
   * @throws IllegalArgumentException if {@code d} is not in range {@code [1, n - 1]} where {@code
   *     n} is the generator order of the elliptic curve domain parameter, see {@link
   *     ECParameterSpec#getOrder()}
   */
  public EcPrivateKeyImpl(final BigInteger d, final ECParameterSpec dp) {
    insS = d;
    insParams = AfiElcParameterSpec.getInstance(dp);
    insPublicPart =
        new EcPublicKeyImpl(AfiElcUtils.multiply(d, dp.getGenerator(), dp), getParams());
  } // end constructor */

  /**
   * Constructs a randomly generated key pair for the elliptic curve given by domain parameters.
   *
   * @param dp the associated elliptic curve domain parameters
   */
  public EcPrivateKeyImpl(final ECParameterSpec dp) {
    this(rng(dp.getOrder()), dp);
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param prk private key
   */
  public EcPrivateKeyImpl(final ECPrivateKey prk) {
    this(prk.getS(), prk.getParams());
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param objects contains two objects:
   *     <ol>
   *       <li>{@link BigInteger} representing the private key d
   *       <li>{@link AfiElcParameterSpec} representing domain parameters
   *     </ol>
   *
   * @throws ArithmeticException if cofactor is not in range of {@link Integer}
   * @throws ArrayIndexOutOfBoundsException if {@code objects} contain less than two elements, this
   *     happens if {@link #tlv2ObjectArray(BerTlv, EafiElcPrkFormat)} is called with {@link
   *     EafiElcPrkFormat#PKCS1}
   * @throws IllegalArgumentException if {@link #tlv2ObjectArray(BerTlv, EafiElcPrkFormat)} does so,
   *     see there
   */
  private EcPrivateKeyImpl(final Object... objects) {
    // Note 1: SonarQube claims the following major code smell on this constructor:
    //         "Remove this unused private "EcPrivateKeyImpl" constructor."
    // Note 2: This is a false positive, because this constructor is used.
    this((BigInteger) objects[0], (AfiElcParameterSpec) objects[1]);
  } // end constructor */

  /**
   * Pseudo-Random Number Generator.
   *
   * <p>This method implements Algorithm 2 from BSI-TR-03111 v2.10 clause 4.1.1.
   *
   * @param n upper boarder of the interval
   * @return RNG({ 1, 2, , ..., n - 1 })
   */
  @VisibleForTesting // otherwise = private
  /* package */ static BigInteger rng(final BigInteger n) {
    return new BigInteger(n.bitLength(), RNG).mod(n.subtract(BigInteger.ONE)).add(BigInteger.ONE);
  } // end method */

  /**
   * Extract key attributes from the given data object.
   *
   * <p>The returned array contains at least one element. The second element is absent for formats,
   * which do not contain domain parameters, e.g. {@link EafiElcPrkFormat#PKCS1}.
   *
   * @param keyMaterial containing the private key and an OID referencing domain parameters
   * @param format identifying the coding
   * @return array of objects representing the key, i.e.
   *     <ol>
   *       <li>{@link BigInteger} representing the private key d
   *       <li>{@link AfiElcParameterSpec} representing domain parameters
   *     </ol>
   *
   * @throws ArithmeticException if cofactor is not in range of {@link Integer}
   * @throws IllegalArgumentException if TLV structure is not in accordance to {@link
   *     EafiElcPrkFormat}
   */
  @VisibleForTesting // otherwise = private
  /* package */ static Object[] tlv2ObjectArray(
      final BerTlv keyMaterial, final EafiElcPrkFormat format) {
    try {
      return switch (format) {
        case ASN1 -> zzzFromAsn1(keyMaterial);
        case ISOIEC7816 -> zzzFromIsoIec7816(keyMaterial);
        case PKCS1 -> zzzFromPkcs1(keyMaterial);
        case PKCS8 -> zzzFromPkcs8(keyMaterial);
      }; // end Switch (format)
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException("ERROR, invalid structure", e);
    } // end Catch (...)
  } // end method */

  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  private static Object[] zzzFromAsn1(final BerTlv keyMaterial) {
    if (DerSequence.TAG != keyMaterial.getTag()) {
      // ... wrong tag
      throw new IllegalArgumentException(INVALID_TAG + keyMaterial.getTagField() + '\'');
    } // end fi
    // ... tag is as expected
    final var keyDo = (DerSequence) keyMaterial;

    final var versionA =
        ((DerInteger) keyDo.get(DerInteger.TAG).orElseThrow()).getDecoded().intValueExact();

    if (0 == versionA) {
      // ... versionA == 0

      final var dodp = (DerSequence) keyDo.get(DerSequence.TAG).orElseThrow();
      final var oidA = ((DerOid) dodp.get(DerOid.TAG).orElseThrow()).getDecoded();

      if (!AfiOid.ecPublicKey.equals(oidA)) {
        // ... unexpected OID
        //     => throw exception
        throw new IllegalArgumentException("expected OID = ecPublicKey, instead got " + oidA);
      } // end fi
      // ... expected OID == ecPublicKey present

      final var doElc = (DerSequence) dodp.get(DerSequence.TAG).orElseThrow();
      final var versionB =
          ((DerInteger) doElc.get(DerInteger.TAG, 0).orElseThrow()).getDecoded().intValueExact();

      if (1 == versionB) { // NOPMD literal in a conditional statement
        // ... versionB == 1

        final var doField = (DerSequence) doElc.get(DerSequence.TAG, 0).orElseThrow();

        final var oidB = ((DerOid) doField.get(DerOid.TAG).orElseThrow()).getDecoded();

        if (!AfiOid.fieldType.equals(oidB)) {
          // ... unexpected OID
          //     => throw exception
          throw new IllegalArgumentException("expected OID = fieldType, instead got " + oidB);
        } // end fi
        // ... expected OID == fieldType present

        final var doCoeff = (DerSequence) doElc.get(DerSequence.TAG, 1).orElseThrow();
        final var doGe = (DerOctetString) doElc.get(DerOctetString.TAG).orElseThrow();
        final var doOrderN = (DerInteger) doElc.get(DerInteger.TAG, 1).orElseThrow();
        final var doCofact = (DerInteger) doElc.get(DerInteger.TAG, 2).orElseThrow();

        final var primeP = ((DerInteger) doField.get(DerInteger.TAG).orElseThrow()).getDecoded();
        final var coeffA =
            new BigInteger(
                1,
                ((DerOctetString) doCoeff.get(DerOctetString.TAG, 0).orElseThrow()).getDecoded());
        final var coeffB =
            new BigInteger(
                1,
                ((DerOctetString) doCoeff.get(DerOctetString.TAG, 1).orElseThrow()).getDecoded());
        final var generator = AfiElcUtils.os2p(doGe.getDecoded(), primeP);
        final var orderN = doOrderN.getDecoded();
        final var cofactor = doCofact.getDecoded().intValueExact();
        final var dp =
            new AfiElcParameterSpec(
                primeP,
                coeffA,
                coeffB,
                generator.getAffineX(),
                generator.getAffineY(),
                orderN,
                cofactor);

        final var octets = (DerOctetString) keyDo.get(DerOctetString.TAG).orElseThrow();

        return new Object[] {
          tlv2ObjectArray(BerTlv.getInstance(octets.getDecoded()), EafiElcPrkFormat.PKCS1)[0], dp
        };
      } else {
        // ... unknown version
        //     => throw exception
        throw new IllegalArgumentException(UNKNOWN_VERSION + versionB);
      } // end else
    } else {
      // ... unknown version
      //     => throw exception
      throw new IllegalArgumentException(UNKNOWN_VERSION + versionA);
    } // end else
  } // end method */

  private static Object[] zzzFromIsoIec7816(final BerTlv keyMaterial) {
    if (0x7f48 != keyMaterial.getTag()) { // NOPMD literal in a conditional statement
      // ... wrong tag
      throw new IllegalArgumentException(INVALID_TAG + keyMaterial.getTagField() + '\'');
    } // end fi
    // ... tag is as expected
    final var keyDo = (ConstructedBerTlv) keyMaterial;
    final var d = ((DerInteger) keyDo.get(DerInteger.TAG).orElseThrow()).getDecoded();
    final var oid = ((DerOid) keyDo.get(DerOid.TAG).orElseThrow()).getDecoded();

    return new Object[] {d, AfiElcParameterSpec.getInstance(oid)};
  } // end method */

  private static Object[] zzzFromPkcs1(final BerTlv keyMaterial) {
    if (0x30 != keyMaterial.getTag()) { // NOPMD literal in a conditional statement
      // ... wrong tag
      throw new IllegalArgumentException(INVALID_TAG + keyMaterial.getTagField() + '\'');
    } // end fi
    // ... tag is as expected
    final var keyDo = (DerSequence) keyMaterial;
    final var version =
        ((DerInteger) keyDo.get(DerInteger.TAG).orElseThrow()).getDecoded().intValueExact();
    final var octets = (DerOctetString) keyDo.get(DerOctetString.TAG).orElseThrow();

    if (1 == version) { // NOPMD literal in a conditional statement
      // ... version == 1
      //     => return just private key d
      return new Object[] {new BigInteger(1, octets.getDecoded())};
    } else {
      // ... unknown version
      //     => throw exception
      throw new IllegalArgumentException(UNKNOWN_VERSION + version);
    } // end else
  } // end method */

  private static Object[] zzzFromPkcs8(final BerTlv keyMaterial) {
    if (0x30 != keyMaterial.getTag()) { // NOPMD literal in a conditional statement
      // ... wrong tag
      throw new IllegalArgumentException(INVALID_TAG + keyMaterial.getTagField() + '\'');
    } // end fi
    // ... tag is as expected
    final var keyDo = (DerSequence) keyMaterial;
    final var version =
        ((DerInteger) keyDo.get(DerInteger.TAG).orElseThrow()).getDecoded().intValueExact();

    if (0 == version) {
      // ... version == 0
      final var oids = (DerSequence) keyDo.get(DerSequence.TAG).orElseThrow();
      final var oidA = ((DerOid) oids.get(DerOid.TAG, 0).orElseThrow()).getDecoded();

      if (!AfiOid.ecPublicKey.equals(oidA)) {
        // ... unexpected OID
        //     => throw exception
        throw new IllegalArgumentException("expected OID = ecPublicKey, instead got " + oidA);
      } // end fi
      // ... expected OID == ecPublicKey present

      final var oidB = ((DerOid) oids.get(DerOid.TAG, 1).orElseThrow()).getDecoded();
      final var octets = (DerOctetString) keyDo.get(DerOctetString.TAG).orElseThrow();

      return new Object[] {
        tlv2ObjectArray(BerTlv.getInstance(octets.getDecoded()), EafiElcPrkFormat.PKCS1)[0],
        AfiElcParameterSpec.getInstance(oidB)
      };
    } else {
      // ... unknown version
      //     => throw exception
      throw new IllegalArgumentException(UNKNOWN_VERSION + version);
    } // end else
  } // end method */

  /**
   * Deciphers given cipher according to [gemSpec_COS#(N090.300)c].
   *
   * <p>This function is a counterpart of {@link EcPublicKeyImpl#encipher(byte[])}.
   *
   * @param cipher being in accordance to [gemSpec_COS#(N090.300)c].
   * @return plain text from cipher
   * @throws IllegalArgumentException to indicate "decipher-error"
   */
  public byte[] decipher(final ConstructedBerTlv cipher) {
    try {
      final var dp = getParams();

      final var oidDo = cipher.getPrimitive(0x06).orElseThrow(); // (N090.300)c.2
      final var dpPuk = AfiElcParameterSpec.getInstance(new AfiOid(oidDo.getValueField()));
      final var keyDo = cipher.getConstructed(0x7F49).orElseThrow(); // (N090.300)c.3
      final var cipherDo = cipher.getPrimitive(0x86).orElseThrow(); // (N090.300)c.4
      final var macDo = cipher.getPrimitive(0x8E).orElseThrow(); // (N090.300)c.5

      final var poA = AfiElcUtils.os2p(keyDo.getPrimitive(0x86).orElseThrow().getValueField(), dp);
      final byte[] piC = cipherDo.getValueField();
      final byte[] c = new byte[piC.length - 1];
      final byte[] t = macDo.getValueField();
      System.arraycopy(piC, 1, c, 0, c.length);

      // Note: AfiElcUtils.sharedSecret(...) checks for mismatch of domain
      //       parameter. Thus, we do not check explicitly.
      final var peA = new EcPublicKeyImpl(poA, dpPuk); // (N004.800)a
      final var kab = AfiElcUtils.sharedSecret(this, peA); // (N004.800)b
      final var kdEnc = AfiUtils.concatenate(kab, Hex.toByteArray("0000 0001"));
      final var kdMac = AfiUtils.concatenate(kab, Hex.toByteArray("0000 0002"));
      final var kEnc = new AesKey(EafiHashAlgorithm.SHA_256.digest(kdEnc), 0, 32); // (N001.520)a
      final var kMac = new AesKey(EafiHashAlgorithm.SHA_256.digest(kdMac), 0, 32); // (N001.520)b
      final var t1 = kEnc.encipherCbc(new byte[16]); // (N004.800)c.2
      final var out = kMac.verifyCmac(kMac.padIso(c), t); // (N004.800)d

      if (out && (0x02 == piC[0])) {
        // ... MAC is okay
        return kEnc.truncateIso(kEnc.decipherCbc(c, t1)); // (N004.800)e
      } // end fi
      // ... wrong MAC
    } catch (IllegalArgumentException | NoSuchElementException e) { // NOPMD empty catch block
      // intentionally empty
    } // end Catch (...)

    throw new IllegalArgumentException("decipher error");
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

    final var other = (EcPrivateKeyImpl) obj;

    // --- compare primitive instance attributes (currently none)
    // -/-

    // --- compare reference types
    // ... assertion: instance attributes are never null
    return this.getParams().equals(other.getParams()) && this.getS().equals(other.getS());
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
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      result = getParams().hashCode(); // start value
      final var hashCodeMultiplier = 31; // small prime number

      // --- take into account primitive instance attributes (currently none)
      // -/-

      // --- take into account reference types (currently only public point)
      result = result * hashCodeMultiplier + getS().hashCode();

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
   * @return octet string representation of format {@link EafiElcPrkFormat#PKCS8}
   * @see java.security.Key#getEncoded()
   */
  @Override
  public byte[] getEncoded() {
    // same result as sun.security.ec.ECPrivateKeyImpl.getEncoded()
    return getEncoded(EafiElcPrkFormat.PKCS8).getEncoded();
  } // end method */

  /**
   * Converts this object to {@link BerTlv}.
   *
   * <p><i>Note: this is the counterpart to {@link EcPrivateKeyImpl#EcPrivateKeyImpl(BerTlv,
   * EafiElcPrkFormat)}.</i>
   *
   * @param format indicates the structure of the TLV-object.
   * @return returns this object as {@link BerTlv}
   * @throws NoSuchElementException if there is no OID defined for the domain parameters in {@link
   *     AfiOid}
   */
  public ConstructedBerTlv getEncoded(final EafiElcPrkFormat format) {
    final var dp = getParams();

    if (EafiElcPrkFormat.ASN1.equals(format)) {
      // ... ASN.1 format
      // spotless:off
      return new DerSequence(List.of(
          new DerInteger(BigInteger.ZERO), // probably version number
          // DO-ecPublicKey
          new DerSequence(List.of(
              new DerOid(AfiOid.ecPublicKey),
              // DO-domain parameter
              new DerSequence(List.of(
                  new DerInteger(BigInteger.ONE),
                  // DO-primeP
                  new DerSequence(List.of(
                      new DerOid(AfiOid.fieldType), new DerInteger(dp.getP())
                  )),
                  // DO-coefficient
                  new DerSequence(List.of(
                      new DerOctetString(dp.getCurve().getA().toByteArray()),
                      new DerOctetString(dp.getCurve().getB().toByteArray())
                  )),
                  // generator
                  new DerOctetString(
                      AfiElcUtils.p2osUncompressed(dp.getGenerator(), dp)
                  ),
                  new DerInteger(dp.getOrder()), // order
                  new DerInteger(BigInteger.valueOf(dp.getCofactor())) // cofactor
              ))
          )),
          new DerOctetString(getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
      ));
      // spotless:on
    } else if (EafiElcPrkFormat.ISOIEC7816.equals(format)) {
      // ... ISO/IEC 7816 format
      return (ConstructedBerTlv)
          BerTlv.getInstance(0x7f48, List.of(new DerOid(dp.getOid()), new DerInteger(getS())));
    } else if (EafiElcPrkFormat.PKCS1.equals(format)) {
      // ... PKCS#1 format
      return new DerSequence(
          List.of(
              new DerInteger(BigInteger.ONE),
              new DerOctetString(AfiBigInteger.i2os(getS(), dp.getTau()))));
    } // end fi
    // ... assuming PKCS#8 format

    return new DerSequence(
        List.of(
            new DerInteger(BigInteger.ZERO),
            new DerSequence(List.of(new DerOid(AfiOid.ecPublicKey), new DerOid(dp.getOid()))),
            new DerOctetString(getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())));
  } // end method */

  /**
   * Returns the name of the primary encoding format of this key.
   *
   * @return "PKCS#8"
   * @see java.security.Key#getFormat()
   */
  @Override
  public String getFormat() {
    // see .../jdk-doc/docs/api/java/security/Key.html#getFormat%28%29
    // same result as sun.security.ec.ECPrivateKeyImpl.getFormat()
    return "PKCS#8";
  } // end method */

  /**
   * Returns the associated elliptic curve domain parameters.
   *
   * @return the EC domain parameters
   * @see java.security.spec.ECPrivateKeySpec#getParams()
   */
  @Override
  public AfiElcParameterSpec getParams() {
    return insParams; // EI_EXPOSE_REP
  } // end method */

  /**
   * Returns corresponding public part of key pair.
   *
   * @return corresponding public key
   */
  public EcPublicKeyImpl getPublicKey() {
    return insPublicPart; // EI_EXPOSE_REP
  } // end method */

  /**
   * Returns the private value S.
   *
   * @return the private value S
   * @see ECPrivateKey#getS()
   */
  @Override
  public BigInteger getS() {
    return insS;
  } // end method */

  /**
   * Signs a message according to BSI-TR03111 v2.10 clause 4.2.1.1.
   *
   * <p><i><b>Note:</b> The hash function is implicitly chosen from domain parameter, i.e.:</i>
   *
   * <ol>
   *   <li><i>key length up to 256 bit: SHA-256</i>
   *   <li><i>key length up to 384 bit: SHA-384</i>
   *   <li><i>otherwise: SHA-512</i>
   * </ol>
   *
   * @param message to be signed
   * @return signature R || S
   */
  public byte[] signEcdsa(final byte[] message) {
    return signEcdsa(calculateHashValue(message));
  } // end method */

  /**
   * Signs a hash value according to BSI-TR03111 v2.10 clause 4.2.1.1.
   *
   * @param hashTau to be signed
   * @return signature R || S
   */
  public byte[] signEcdsa(final BigInteger hashTau) {
    return EcPublicKeyImpl.fromSequence(signEcdsaDer(hashTau), getParams().getTau());
  } // end method */

  /**
   * This method is for test purposes only when a certain k is necessary to run examples.
   *
   * <p><i><b>Note:</b> The hash function is implicitly chosen from domain parameter:</i>
   *
   * <ol>
   *   <li><i>key length up to 256 bit: SHA-256</i>
   *   <li><i>key length up to 384 bit: SHA-384</i>
   *   <li><i>otherwise: SHA-512</i>
   * </ol>
   *
   * @param message to be signed
   * @param k random number from range {@code [1, n-1]} with {@code n} as the order from domain
   *     parameters
   * @return a {@link DerSequence} with two elements:
   *     <ol>
   *       <li>element {@link DerInteger} with R
   *       <li>element {@link DerInteger} with S
   *     </ol>
   *
   * @throws ArithmeticException if random number {@code k} is not invertible {@code mod n}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>random number {@code k} is not in range {@code [1, n-1}
   *       <li>x-coordinate of Q = [k]G is zero
   *       <li>s = k-1 (r d + OS2I(hash(message))) mod n is zero
   *     </ol>
   */
  @VisibleForTesting
  // otherwise private
  /* package */ DerSequence signEcdsa(final byte[] message, final BigInteger k) {
    return signEcdsaDer(calculateHashValue(message), k);
  } // end method */

  /**
   * Signs a message according to BSI-TR03111 v2.10 clause 4.2.1.1.
   *
   * <p><i><b>Note:</b> The hash function is implicitly chosen from domain parameter, i.e.:</i>
   *
   * <ol>
   *   <li><i>key length up to 256 bit: SHA-256</i>
   *   <li><i>key length up to 384 bit: SHA-384</i>
   *   <li><i>otherwise: SHA-512</i>
   * </ol>
   *
   * @param message to be signed
   * @return a {@link DerSequence} with two elements:
   *     <ol>
   *       <li>element {@link DerInteger} with R
   *       <li>element {@link DerInteger} with S
   *     </ol>
   */
  public DerSequence signEcdsaDer(final byte[] message) {
    return signEcdsaDer(calculateHashValue(message));
  } // end method */

  /**
   * Signs a hash value according to BSI-TR03111 v2.10 clause 4.2.1.1.
   *
   * @param hashTau to be signed
   * @return a {@link DerSequence} with two elements:
   *     <ol>
   *       <li>element {@link DerInteger} with R
   *       <li>element {@link DerInteger} with S
   *     </ol>
   */
  public DerSequence signEcdsaDer(final BigInteger hashTau) {
    final BigInteger n = getParams().getOrder();

    // --- loop until underlying method does not throw an '0 == r' exception
    for (; ; ) {
      try {
        return signEcdsaDer(hashTau, rng(n));
      } catch (ArithmeticException | IllegalArgumentException e) { // NOPMD empty catch block
        // ... ArithmeticException: probably "k not invertible"
        // ... IllegalArgumentException: probably (r==0) or (s==0)
        //     => try again
      } // end Catch (...)
    } // end For (...)
  } // end method */

  /**
   * This method is for test purposes only when a certain k is necessary to run examples.
   *
   * <p><i><b>Note:</b> The hash function is implicitly chosen from domain parameter:</i>
   *
   * <ol>
   *   <li><i>key length up to 256 bit: SHA-256</i>
   *   <li><i>key length up to 384 bit: SHA-384</i>
   *   <li><i>otherwise: SHA-512</i>
   * </ol>
   *
   * @param hashTau hash value of message to be signed
   * @param k random number from range {@code [1, n-1]} with {@code n} as the order from domain
   *     parameters
   * @return a {@link DerSequence} with two elements:
   *     <ol>
   *       <li>element {@link DerInteger} with R
   *       <li>element {@link DerInteger} with S
   *     </ol>
   *
   * @throws ArithmeticException if random number {@code k} is not invertible {@code mod n}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>random number {@code k} is not in range {@code [1, n-1}
   *       <li>x-coordinate of Q = [k]G is zero
   *       <li>s = k-1 (r d + OS2I(hash(message))) mod n is zero
   *     </ol>
   */
  @VisibleForTesting
  // otherwise private
  /* package */ DerSequence signEcdsaDer(final BigInteger hashTau, final BigInteger k) {
    final var dp = getParams();
    final var n = dp.getOrder();

    // --- check range k
    // Note 1: According to BST-TR-03111 v2.10 clause 4.2.1.1, action step 1
    //         integer k has to be in range [1, n-1] = [1, n[.
    // Note 2: The following statement checks the upper boundary.
    // Note 3: The lower boundary is checked by AfiElcUtils.multiply(...).
    if (k.compareTo(n) >= 0) {
      // ... k >= n, i.e. k not in range [1, n[
      //     => throw exception
      throw new IllegalArgumentException("factor k greater or equal to order n");
    } // end fi

    // --- step 1: k = RNG({1, 2, ..., n-1})
    // no random number is generated here, because k is given as an input parameter

    // --- step 2: Q = [k]G
    final var q = AfiElcUtils.multiply(k, dp.getGenerator(), dp);

    // --- step 3: r = OS2I(FE2OS(xq)) mod n
    final var r = q.getAffineX().mod(n);
    if (0 == r.signum()) {
      throw new IllegalArgumentException("0 == r");
    } // end fi
    // ... r != 0

    // --- step 4: kinv = k^-1 mod n
    final var kinv = k.modInverse(n);

    // --- step 5: s = kinv (r d + Htau) mod n
    final var s = kinv.multiply(r.multiply(getS()).add(hashTau)).mod(n);
    if (0 == s.signum()) {
      throw new IllegalArgumentException("0 == s");
    } // end fi
    // ... s != 0

    return new DerSequence(List.of(new DerInteger(r), new DerInteger(s)));
  } // end method */

  /**
   * Calculate the hash value.
   *
   * <p><i><b>Note:</b> The hash function is implicitly chosen from domain parameter:</i>
   *
   * <ol>
   *   <li><i>key length up to 256 bit: SHA-256</i>
   *   <li><i>key length up to 384 bit: SHA-384</i>
   *   <li><i>otherwise: SHA-512</i>
   * </ol>
   *
   * @param message to be signed
   * @return hash value as {@link BigInteger}
   */
  @VisibleForTesting
  /* package */ BigInteger calculateHashValue(final byte[] message) {
    // --- set hash function
    final var tau = getParams().getTau();
    final EafiHashAlgorithm hashFunction;
    if (tau <= 32) { // NOPMD literal in a conditional statement
      // ... key length <= 256 bit
      hashFunction = EafiHashAlgorithm.SHA_256;
    } else if (tau <= 48) { // NOPMD literal in a conditional statement
      // ... key length <= 384 bit
      hashFunction = EafiHashAlgorithm.SHA_384;
    } else {
      // ... key length > 384 bit
      hashFunction = EafiHashAlgorithm.SHA_512;
    } // end fi

    // --- calculate hash value, here it is converted to an integer, because that
    //     format is needed in BST-TR-03111 v2.10 clause 4.2.1.1, action step 5.
    return new BigInteger(1, hashFunction.digest(message));
  } // end method */

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
    final var dp = getParams();
    final var d = getS().toString(16);

    if (dp.hasOid()) {
      final var oid = dp.getOid();

      return String.format("Domain parameter OID = %s, d = '%s'", oid, d);
    } // end fi

    return String.format("Domain parameter: %s%nd = '%s'", dp, d);
  } // end method */

  /**
   * Performs serialization and deserialization.
   *
   * @return object replacing instance of {@link EcPrivateKeyImpl} used for serialization
   * @throws ObjectStreamException if underlying methods do so
   */
  @Serial
  private Object writeReplace() throws ObjectStreamException {
    return new Serializable() {
      /** Serial number randomly generated on 2021-05-07 16:32. */
      @Serial private static final long serialVersionUID = 1822456871686807908L;

      /** Proxy for point defining the private key. */
      private transient BigInteger insD = getS(); // */

      /** Proxy for domain parameters. */
      private transient AfiElcParameterSpec insAfiElcParameterSpec = getParams(); // */

      /**
       * Method used during serialization.
       *
       * @param oos output stream to which objects are written
       * @throws IOException if underlying methods do so
       */
      @Serial
      private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.writeObject(insD);
        oos.writeObject(insAfiElcParameterSpec);
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
        insD = (BigInteger) ois.readObject();
        insAfiElcParameterSpec = (AfiElcParameterSpec) ois.readObject();
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
        return new EcPrivateKeyImpl(insD, insAfiElcParameterSpec);
      } // end inner method
    }; // end inner class
  } // end method */
} // end class
