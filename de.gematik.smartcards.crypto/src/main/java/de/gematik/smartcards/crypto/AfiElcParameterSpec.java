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

import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class extends the functionality and attributes of {@link ECParameterSpec}.
 *
 * <p>According to JavaDoc superclass {@link ECParameterSpec} is immutable. Because this class has
 * not setters, it follows, that this class also is immutable.
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
 * <p>TODO: Implement all recommended curves from <a
 * href="https://docs.oracle.com/en/java/javase/13/security/oracle-providers.html#GUID-091BF58C-82AB-4C9C-850F-1660824D5254">Oracle</a>.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "SE_NO_SUITABLE_CONSTRUCTOR", i.e.
//         Spotbugs message: This class implements the Serializable interface and
//             its superclass does not. When such an object is deserialized, the
//             fields of the superclass need to be initialized by invoking the
//             void constructor of the superclass. Since the superclass does not
//             have one, serialization and deserialization will fail at runtime.
//         Rational: It is correct that this class implements the Serializable
//             interface and its superclass does not. The finding does not
//             matter here, because this class implements proper serialization
//             and deserialization, see the corresponding JUnit test.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "SE_NO_SUITABLE_CONSTRUCTOR" // see note 1
}) // */
@SuppressWarnings({"PMD.FieldNamingConventions", "PMD.SingleMethodSingleton", "PMD.TooManyMethods"})
public final class AfiElcParameterSpec extends ECParameterSpec implements Serializable {

  /** Serial number randomly generated on 2021-05-07 15:51. */
  @Serial private static final long serialVersionUID = -8788341234956219740L; // */

  /**
   * Domain parameter according to ANSI X9.62 clause L.6.4.3.
   *
   * <p>See e.g. ANSI X9.62-2015 clause L.6.4.3 or <a
   * href="https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf">FIPS 186-4 D.1.2.3</a>
   */
  public static final AfiElcParameterSpec ansix9p256r1 =
      new AfiElcParameterSpec(
          /* prime p */ new BigInteger(
              "ffffffff00000001000000000000000000000000ffffffffffffffffffffffff", 16),
          /* coefficient a */ new BigInteger(
              "ffffffff00000001000000000000000000000000fffffffffffffffffffffffc", 16),
          /* coefficient b */ new BigInteger(
              "5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16),
          new ECPoint(
              /* Gx */ new BigInteger(
                  "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296", 16),
              /* Gy */ new BigInteger(
                  "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5", 16)),
          /* n, order of base point */ new BigInteger(
              "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551", 16),
          /* cofactor h */ 1,
          AfiOid.ansix9p256r1); // */

  /**
   * Domain parameter according to ANSI X9.62 clause L.6.5.2.
   *
   * <p>See e.g. ANSI X9.62-2015 clause L.6.5.2 or <a
   * href="https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf">FIPS 186-4 D.1.2.4</a>
   */
  public static final AfiElcParameterSpec ansix9p384r1 =
      new AfiElcParameterSpec(
          /* prime p */ new BigInteger(
              "000000ffffffffffffffffffffffffffffffffffffffffffffffffff"
                  + "fffffffffffffeffffffff0000000000000000ffffffff",
              16),
          /* coefficient a */ new BigInteger(
              "000000ffffffffffffffffffffffffffffffffffffffffffffffffff"
                  + "fffffffffffffeffffffff0000000000000000fffffffc",
              16),
          /* coefficient b */ new BigInteger(
              "000000b3312fa7e23ee7e4988e056be3f82d19181d9c6efe814112"
                  + "0314088f5013875ac656398d8a2ed19d2a85c8edd3ec2aef",
              16),
          new ECPoint(
              /* Gx */ new BigInteger(
                  "000000aa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b98"
                      + "59f741e082542a385502f25dbf55296c3a545e3872760ab7",
                  16),
              /* Gy */ new BigInteger(
                  "0000003617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147c"
                      + "e9da3113b5f0b8c00a60b1ce1d7e819d7a431d7c90ea0e5f",
                  16)),
          /* n, order of base point */ new BigInteger(
              "000000ffffffffffffffffffffffffffffffffffffffffffffffffc7"
                  + "634d81f4372ddf581a0db248b0a77aecec196accc52973",
              16),
          /* cofactor h */ 1,
          AfiOid.ansix9p384r1); // */

  /**
   * Domain parameter according to ANSI X9.62 clause L.6.6.2.
   *
   * <p>See e.g. ANSI X9.62-2015 clause L.6.6.2 or <a
   * href="https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf">FIPS 186-4 D.1.2.5</a>
   */
  public static final AfiElcParameterSpec ansix9p521r1 =
      new AfiElcParameterSpec(
          /* prime p */ new BigInteger(
              "00000001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                  + "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
              16),
          /* coefficient a */ new BigInteger(
              "00000001ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
                  + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffc",
              16),
          /* coefficient b */ new BigInteger(
              "0000000051953eb9618e1c9a1f929a21a0b68540eea2da725b99b315f3b8b489918ef109"
                  + "e156193951ec7e937b1652c0bd3bb1bf073573df883d2c34f1ef451fd46b503f00",
              16),
          new ECPoint(
              /* Gx */ new BigInteger(
                  "00000000c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3d"
                      + "baa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66",
                  16),
              /* Gy */ new BigInteger(
                  "000000011839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e66"
                      + "2c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650",
                  16)),
          /* n, order of base point */ new BigInteger(
              "00000001fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffa"
                  + "51868783bf2f966b7fcc0148f709a5d03bb5c9b8899c47aebb6fb71e91386409",
              16),
          /* cofactor h */ 1,
          AfiOid.ansix9p521r1); // */

  /**
   * Domain parameter according to RFC-5639 clause 3.4.
   *
   * <p>See <a href="https://tools.ietf.org/html/rfc5639#section-3.4">RFC-5639 clause 3.4</a>.
   */
  public static final AfiElcParameterSpec brainpoolP256r1 =
      new AfiElcParameterSpec(
          /* prime p */ new BigInteger(
              "a9fb57dba1eea9bc3e660a909d838d726e3bf623d52620282013481d1f6e5377", 16),
          /* coefficient a */ new BigInteger(
              "7d5a0975fc2c3057eef67530417affe7fb8055c126dc5c6ce94a4b44f330b5d9", 16),
          /* coefficient b */ new BigInteger(
              "26dc5c6ce94a4b44f330b5d9bbd77cbf958416295cf7e1ce6bccdc18ff8c07b6", 16),
          new ECPoint(
              /* Gx */ new BigInteger(
                  "8bd2aeb9cb7e57cb2c4b482ffc81b7afb9de27e1e3bd23c23a4453bd9ace3262", 16),
              /* Gy */ new BigInteger(
                  "547ef835c3dac4fd97f8461a14611dc9c27745132ded8e545c1d54c72f046997", 16)),
          /* n, order of base point */ new BigInteger(
              "a9fb57dba1eea9bc3e660a909d838d718c397aa3b561a6f7901e0e82974856a7", 16),
          /* cofactor h */ 1,
          AfiOid.brainpoolP256r1); // */

  /**
   * Domain parameter according to RFC-5639 clause 3.6.
   *
   * <p>See <a href="https://tools.ietf.org/html/rfc5639#section-3.6">RFC-5639 clause 3.6</a>.
   */
  public static final AfiElcParameterSpec brainpoolP384r1 =
      new AfiElcParameterSpec(
          /* prime p */ new BigInteger(
              "0000008cb91e82a3386d280f5d6f7e50e641df152f7109ed5456b4"
                  + "12b1da197fb71123acd3a729901d1a71874700133107ec53",
              16),
          /* coefficient a */ new BigInteger(
              "0000007bc382c63d8c150c3c72080ace05afa0c2bea28e4fb22787"
                  + "139165efba91f90f8aa5814a503ad4eb04a8c7dd22ce2826",
              16),
          /* coefficient b */ new BigInteger(
              "00000004a8c7dd22ce28268b39b55416f0447c2fb77de107dcd2a6"
                  + "2e880ea53eeb62d57cb4390295dbc9943ab78696fa504c11",
              16),
          new ECPoint(
              /* Gx */ new BigInteger(
                  "0000001d1c64f068cf45ffa2a63a81b7c13f6b8847a3e77ef14fe3"
                      + "db7fcafe0cbd10e8e826e03436d646aaef87b2e247d4af1e",
                  16),
              /* Gy */ new BigInteger(
                  "0000008abe1d7520f9c2a45cb1eb8e95cfd55262b70b29feec5864"
                      + "e19c054ff99129280e4646217791811142820341263c5315",
                  16)),
          // n, order of base point
          new BigInteger(
              "0000008cb91e82a3386d280f5d6f7e50e641df152f7109ed5456b3"
                  + "1f166e6cac0425a7cf3ab6af6b7fc3103b883202e9046565",
              16),
          1, // cofactor h
          AfiOid.brainpoolP384r1); // */

  /**
   * Domain parameter according to RFC-5639 clause 3.7.
   *
   * <p>See <a href="https://tools.ietf.org/html/rfc5639#section-3.7">RFC-5639 clause 3.7</a>.
   */
  public static final AfiElcParameterSpec brainpoolP512r1 =
      new AfiElcParameterSpec(
          /* prime p */ new BigInteger(
              "000000aadd9db8dbe9c48b3fd4e6ae33c9fc07cb308db3b3c9d20ed6639cca70330871"
                  + "7d4d9b009bc66842aecda12ae6a380e62881ff2f2d82c68528aa6056583a48f3",
              16),
          /* coefficient a */ new BigInteger(
              "0000007830a3318b603b89e2327145ac234cc594cbdd8d3df91610a83441caea9863bc"
                  + "2ded5d5aa8253aa10a2ef1c98b9ac8b57f1117a72bf2c7b9e7c1ac4d77fc94ca",
              16),
          /* coefficient b */ new BigInteger(
              "0000003df91610a83441caea9863bc2ded5d5aa8253aa10a2ef1c98b9ac8b57f1117a7"
                  + "2bf2c7b9e7c1ac4d77fc94cadc083e67984050b75ebae5dd2809bd638016f723",
              16),
          new ECPoint(
              /* Gx */ new BigInteger(
                  "00000081aee4bdd82ed9645a21322e9c4c6a9385ed9f70b5d916c1b43b62eef4d0098e"
                      + "ff3b1f78e2d0d48d50d1687b93b97d5f7c6d5047406a5e688b352209bcb9f822",
                  16),
              /* Gy */ new BigInteger(
                  "0000007dde385d566332ecc0eabfa9cf7822fdf209f70024a57b1aa000c55b881f8111"
                      + "b2dcde494a5f485e5bca4bd88a2763aed1ca2b2fa8f0540678cd1e0f3ad80892",
                  16)),
          /* n, order of base point */ new BigInteger(
              "000000aadd9db8dbe9c48b3fd4e6ae33c9fc07cb308db3b3c9d20ed6639cca70330870"
                  + "553e5c414ca92619418661197fac10471db1d381085ddaddb58796829ca90069",
              16),
          /* cofactor h */ 1,
          AfiOid.brainpoolP512r1); // */

  /** Set of pre-defined object identifier. */
  public static final List<AfiElcParameterSpec> PREDEFINED =
      List.of(
          ansix9p256r1,
          ansix9p384r1,
          ansix9p521r1,
          brainpoolP256r1,
          brainpoolP384r1,
          brainpoolP512r1); // */

  /*
   * Domain parameter according to RFC-7748 clause 4.1.
   *
   * <p>See <a href="https://www.rfc-editor.org/rfc/rfc7748.html#section-4.1">RFC-7748, 3.7</a>.
   *
   * <p>According to
   * <a href="https://en.wikipedia.org/wiki/Curve25519">Wikipedia</a>
   * {@code Curve25519} is a
   * <a href="https://en.wikipedia.org/wiki/Montgomery_curve">Montgommery</a>
   * curve. But {@link AfiElcUtils} currently supports
   * <a href="https://en.wikipedia.org/wiki/Weierstrass_elliptic_function">Weierstrass</a>
   * curves only.
   * Thus, currently this library cannot support
   * <a href="https://en.wikipedia.org/wiki/Curve25519">Curve25519</a>.
   */

  /** Contains the number of octets necessary to encode prime p, i.e., ceiling(log256(p)). */
  private final int insL; // */

  /**
   * OID referencing domain parameters.
   *
   * <p>This attribute is set to {@code NULL} in case the domain parameter belong to an elliptic
   * curve for which no OID is defined in {@link AfiOid}.
   */
  private final @Nullable AfiOid insOid; // spotbugs: SE_TRANSIENT_FIELD_NOT_RESTORED */

  /** Contains the number of octets necessary to encode order n, i.e., ceiling(log256(n)). */
  private final int insTau; // */

  /**
   * Flag telling if prime {@code p} defining the field {@code Fp} is congruent {@code 3 mod 4}.
   *
   * <p>According to BSI TR-03111, v2.10, clause 3.2.2, note only then the square root of beta could
   * be calculated when it comes to uncompress a point using {@link AfiElcUtils#os2p(byte[],
   * AfiElcParameterSpec)}.
   *
   * <p><i><b>Note:</b> For {@link #PREDEFINED} domain parameter it is checked by Junit that this is
   * {@code TRUE}. For other domain parameter this flag is calculated within the appropriate
   * constructor.</i>
   */
  private final boolean insIsPcongruent3mod4; // NOPMD provide accessor */

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
   * Constructs an arbitrary object from given parameters.
   *
   * <p>This constructor is only used in {@link #getInstance(ECParameterSpec)} (apart from test
   * methods). Because {@link #getInstance(ECParameterSpec)} checks first if given parameters belong
   * to a predefined elliptic curve this constructor is only used for elliptic curves which are NOT
   * predefined. Thus, the instance attribute object identifier here is always set to {@code NULL}.
   *
   * @param p prime defining the field Fp
   * @param a coefficient {@code a} of an elliptic curve
   * @param b coefficient {@code b} of an elliptic curve
   * @param gx x-coordinate of base point
   * @param gy y-coordinate of base point
   * @param n order of base point
   * @param h cofactor
   */
  /* package */ AfiElcParameterSpec(
      final BigInteger p,
      final BigInteger a,
      final BigInteger b,
      final BigInteger gx,
      final BigInteger gy,
      final BigInteger n,
      final int h) {
    super(new EllipticCurve(new ECFieldFp(p), a, b), new ECPoint(gx, gy), n, h);

    insL = estimateL();
    insOid = estimateOid();
    insTau = estimateTau();
    insIsPcongruent3mod4 = AfiBigInteger.THREE.equals(p.mod(AfiBigInteger.FOUR));
  } // end constructor */

  /**
   * Constructs an object from given parameters.
   *
   * @param p prime defining the field Fp
   * @param a coefficient {@code a} of an elliptic curve
   * @param b coefficient {@code b} of an elliptic curve
   * @param g base point
   * @param n order of base point
   * @param h cofactor
   * @param oid object-identifier referencing the domain parameter of an elliptic curve
   */
  private AfiElcParameterSpec(
      final BigInteger p,
      final BigInteger a,
      final BigInteger b,
      final ECPoint g,
      final BigInteger n,
      final int h,
      @Nullable final AfiOid oid) {
    super(new EllipticCurve(new ECFieldFp(p), a, b), g, n, h);

    insL = estimateL();
    insOid = oid;
    insTau = estimateTau();
    insIsPcongruent3mod4 = true;
  } // end constructor */

  /**
   * Pseudo constructor returning domain parameters referenced by given OID.
   *
   * @param oid referencing domain parameters
   * @return domain parameters referenced by OID
   * @throws NoSuchElementException if given {@code oid} does not refer to pre-defined domain
   *     parameters
   */
  public static AfiElcParameterSpec getInstance(final AfiOid oid) {
    return PREDEFINED.stream().filter(i -> oid.equals(i.insOid)).findAny().orElseThrow();
  } // end method */

  /**
   * Pseudo constructor.
   *
   * <p>If given {@link ECParameterSpec} equals one of the {@link #PREDEFINED} domain parameters,
   * then that is returned. Otherwise, a new instance of this class is returned.
   *
   * @param parameterSpec from which an instance of this class is constructed
   * @return domain parameters
   */
  @SuppressWarnings({"PMD.SingletonClassReturningNewInstance"})
  public static AfiElcParameterSpec getInstance(final ECParameterSpec parameterSpec) {
    if (parameterSpec instanceof final AfiElcParameterSpec result) {
      return result;
    } // end fi
    // ... parameterSpec NOT of type AfiElcParameterSpec

    final EllipticCurve curve = parameterSpec.getCurve();
    final ECPoint g = parameterSpec.getGenerator(); // NOPMD short name
    final BigInteger n = parameterSpec.getOrder(); // NOPMD short name
    final int h = parameterSpec.getCofactor(); // NOPMD short name

    return PREDEFINED.parallelStream()
        .filter(i -> i.getCurve().equals(curve))
        .filter(i -> i.getGenerator().equals(g))
        .filter(i -> i.getOrder().equals(n))
        .filter(i -> i.getCofactor() == h)
        .findAny()
        .orElseGet(
            () ->
                new AfiElcParameterSpec(
                    ((ECFieldFp) curve.getField()).getP(),
                    curve.getA(),
                    curve.getB(),
                    g.getAffineX(),
                    g.getAffineY(),
                    n,
                    h));
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
    // Note 1: Although this class is not a direct subclass of Object we don't call
    //         super.equals(obj) here, because the superclass doesn't override the
    //         equals-method. Thus, reflexive and null is checked here.

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
    // ... obj is an instance of AfiElcParameterSpec

    final AfiElcParameterSpec other = (AfiElcParameterSpec) obj;

    // --- compare instance attributes
    // ... assertion: instance attributes are never null
    // Note 2: Sequence of checks is such that the check likely fails fast
    //         for non-equal objects.
    if (null == this.insOid) {
      // ... this has no OID

      if (null == other.insOid) { // NOPMD statement can be simplified
        // ... both OID are absent, i.e., unknown
        //     => compare all other curve parameter
        return equals(other);
      } else {
        // ... this has no OID, but other has
        //     => they cannot be equal
        return false;
      } // end fi
    } else {
      // ... at least this has an OID
      //     => compare OID
      return this.insOid.equals(other.insOid);
    } // end fi
  } // end method */

  /**
   * Compares all curve parameter.
   *
   * @return true if all curve parameter are equal, false otherwise
   */
  @SuppressWarnings({"PMD.SuspiciousEqualsMethodName"})
  private boolean equals(final AfiElcParameterSpec other) {
    return this.getP().equals(other.getP()) // compare prime P
        && (0 == this.getOrder().compareTo(other.getOrder())) // compare order
        && this.getGenerator().equals(other.getGenerator()) // compare generator
        && this.getCurve().equals(other.getCurve()) // compare curve
        && (this.getCofactor() == other.getCofactor()); // compare co-factor
  } // end method */

  /**
   * Calculate the number of octets necessary to encode prime p.
   *
   * @return number of octets necessary to encode prime p
   */
  private int estimateL() {
    return (int) Math.round(Math.ceil(getP().bitLength() / 8.0));
  } // end method */

  /**
   * Estimate object-identifier of curve parameter.
   *
   * @return OID corresponding to domain parameter or {@code NULL} for unknown elliptic curve
   */
  private @Nullable AfiOid estimateOid() {
    return PREDEFINED.stream()
        .filter(this::equals)
        .findAny()
        .map(AfiElcParameterSpec::getOid)
        .orElse(null);
  } // end method */

  /**
   * Calculate the number of octets necessary to encode order n.
   *
   * @return minimum number of octets necessary to encode order n
   */
  private int estimateTau() {
    return (int) Math.round(Math.ceil(getOrder().bitLength() / 8.0));
  } // end method */

  /**
   * Return number of octet necessary to encode prime p defining field Fp.
   *
   * @return length of prime p in octet
   */
  public int getL() {
    return insL;
  } // end method */

  /**
   * Return object-identifier (OID) referencing the domain parameter defining this elliptic curve.
   *
   * @return OID referencing domain parameters
   * @throws NoSuchElementException if the curve is not in {@link #PREDEFINED}
   */
  public AfiOid getOid() {
    if (null == insOid) {
      throw new NoSuchElementException("not a predefined curve");
    } // end fi

    return insOid;
  } // end method */

  /**
   * Return prime p defining the field Fp.
   *
   * @return prime p
   */
  public BigInteger getP() {
    return ((ECFieldFp) getCurve().getField()).getP();
  } // end method */

  /**
   * Return number of octet necessary to encode order n of base point.
   *
   * @return length of order n in octet
   */
  public int getTau() {
    return insTau;
  } // end method */

  /**
   * Returns whether the domain parameters have an OID.
   *
   * <p>More specifically: This method returns true if and only if for the domain parameter of this
   * instance an OID is present in {@link AfiOid}.
   *
   * <p><i><b>Note:</b> Currently for all domain parameter in {@link #PREDEFINED} a corresponding
   * OID is present in {@link AfiOid}.</i>
   *
   * @return {@code TRUE} if an OID is available, {@code FALSE} otherwise
   */
  public boolean hasOid() {
    return null != insOid;
  } // end method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * @return hash-code of object
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    // Note 1: Although this class is not a direct subclass of Object, we do not
    //         call super.hashCode() here, because the superclass does not
    //         override the hashCode-method.
    // Note 2: For performance reasons, this method takes into account prime P
    //         only. Currently, (as of 2021-01-08) for pre-defined curves, this
    //         value is unique. If the same curve with a different generator is
    //          used, that would lead to non-equal objects with the same hash-code.
    //         That seems okay.
    // Note 3: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from the main memory into thread local memory
    if (0 == result) {
      // ... obviously attribute attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      result = getP().hashCode(); // start value

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Returns whether the prime {@code p} defining the field {@code Fp} is congruent {@code 3 mod 4}.
   *
   * <p><i><b>Note:</b> For compressed encodings of points on an elliptic curve decoding in {@link
   * AfiElcUtils#os2p(byte[], AfiElcParameterSpec)} is only possible if this is {@code TRUE}.</i>
   *
   * @return {@code TRUE} if prime {@code p} is congruent {@code 3 mod 4}, {@code FALSE} otherwise
   */
  public boolean isPcongruent3mod4() {
    return insIsPcongruent3mod4;
  } // end method */

  /**
   * Converts elliptic curve parameters to a printable string.
   *
   * @see Object#toString()
   */
  @Override
  public String toString() {
    final int noOctet = getL();

    return String.format(
        "p  = '%s'%na  = '%s'%nb  = '%s'%nGx = '%s'%nGy = '%s'%nn  = '%s'%nh  = %d",
        Hex.toHexDigits(AfiBigInteger.i2os(getP(), noOctet)),
        Hex.toHexDigits(AfiBigInteger.i2os(getCurve().getA(), noOctet)),
        Hex.toHexDigits(AfiBigInteger.i2os(getCurve().getB(), noOctet)),
        Hex.toHexDigits(AfiBigInteger.i2os(getGenerator().getAffineX(), noOctet)),
        Hex.toHexDigits(AfiBigInteger.i2os(getGenerator().getAffineY(), noOctet)),
        Hex.toHexDigits(AfiBigInteger.i2os(getOrder(), getTau())),
        getCofactor());
  } // end method */

  /**
   * Performs serialization and deserialization.
   *
   * @return object replacing instance of {@link AfiElcParameterSpec} used for serialization
   * @throws ObjectStreamException if underlying methods do so
   */
  @Serial
  private Object writeReplace() throws ObjectStreamException {
    return new Serializable() {
      /** Serial number randomly generated on 2024-06-30. */
      @Serial private static final long serialVersionUID = 8567143407868168167L;

      /** Proxy for prime P defining finite field Fp. */
      private transient BigInteger insP = getP(); // */

      /** Proxy for the coefficient A of the elliptic curve. */
      private transient BigInteger insA = getCurve().getA(); // */

      /** Proxy for the coefficient B of the elliptic curve. */
      private transient BigInteger insB = getCurve().getB(); // */

      /** Proxy for x-coordinate of generator. */
      private transient BigInteger insX = getGenerator().getAffineX(); // */

      /** Proxy for y-coordinate of generator. */
      private transient BigInteger insY = getGenerator().getAffineY(); // */

      /** Proxy for order off base point. */
      private transient BigInteger insN = getOrder(); // */

      /** Proxy for cofactor. */
      private transient BigInteger insH = BigInteger.valueOf(getCofactor()); // */

      /**
       * Method used during serialization.
       *
       * @param oos output stream to which objects are written
       * @throws IOException if underlying methods do so
       */
      @Serial
      private void writeObject(final ObjectOutputStream oos) throws IOException {
        oos.writeObject(insP);
        oos.writeObject(insA);
        oos.writeObject(insB);
        oos.writeObject(insX);
        oos.writeObject(insY);
        oos.writeObject(insN);
        oos.writeObject(insH);
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
        insP = (BigInteger) ois.readObject();
        insA = (BigInteger) ois.readObject();
        insB = (BigInteger) ois.readObject();
        insX = (BigInteger) ois.readObject();
        insY = (BigInteger) ois.readObject();
        insN = (BigInteger) ois.readObject();
        insH = (BigInteger) ois.readObject();
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
        return getInstance(
            new ECParameterSpec(
                new EllipticCurve(new ECFieldFp(insP), insA, insB), // curve
                new ECPoint(insX, insY), // generator
                insN, // order
                insH.intValue() // co-factor
                ));
      } // end inner method
    }; // end inner class
  } // end method */
} // end class
