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

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerNull;
import de.gematik.smartcards.tlv.DerOctetString;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiOid;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.Serial;
import java.math.BigInteger;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class extends {@link RsaPrivateKeyImpl} and implements {@link RsaPrivateCrtKeyImpl}.
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
// Note 1: Spotbugs claims "CT_CONSTRUCTOR_THROW"
//         Short message: Classes that throw exceptions in their constructors
//             are vulnerable to Finalizer attacks.
//         That finding is not correct, because an empty finalize() declared
//             "final" is present in superclass.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "CT_CONSTRUCTOR_THROW" // see note 1
}) // */
@SuppressWarnings({
  "PMD.GodClass",
  "PMD.LocalVariableNamingConventions",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods"
})
public class RsaPrivateCrtKeyImpl extends RsaPrivateKeyImpl implements RSAPrivateCrtKey {

  /** Serial number randomly generated on 2022-07-25. */
  @Serial private static final long serialVersionUID = 7201148518736068403L;

  /**
   * Infimum of modulus length for which this class supports secure key generation.
   *
   * <p><i><b>Note:</b> This does not mean that the generated keys are strong. It only means that
   * for key generation the same algorithm as for strong keys is used.</i>
   */
  public static final int INFIMUM_SECURE_MODULUS = 200; // */

  /**
   * Certainty that a number is probably prime.
   *
   * <p>A measure of the uncertainty that a user is willing to tolerate: The probability that a
   * number is prime exceeds (1 - 1/2^certainty).
   *
   * <p>This constant is used in method {@link BigInteger#isProbablePrime(int)}.
   */
  public static final int CERTAINTY = 120;

  /** Message in case version is unknown in {@link EafiRsaPrkFormat} structures. */
  /* package */ static final String UNKNOWN_VERSION = "unknown version"; // */

  /** Prime number p. */
  private final BigInteger insPrimeP; // */

  /** Prime number q. */
  private final BigInteger insPrimeQ; // */

  /** Prime exponent p. */
  private final BigInteger insPrimeExponentP; // */

  /** Prime exponent q. */
  private final BigInteger insPrimeExponentQ; // */

  /** CRT coefficient. */
  private final BigInteger insCrtCoefficient; // */

  /**
   * Constructor uses given primes p and q.
   *
   * <p>The public exponent e is estimated as follows: If the modulus length is
   *
   * <ol>
   *   <li>64 bit or smaller then {@code eMin = 3}
   *   <li>else {code eMin = F4 = 65537}
   * </ol>
   *
   * <p>The smallest number greater than or equal to {@code eMin} fulfilling {@code gcd(e,
   * (p-1)(q-1)) == 1} is taken as public exponent e.
   *
   * @param primeP first prime number
   * @param primeQ second prime number
   */
  public RsaPrivateCrtKeyImpl(final BigInteger primeP, final BigInteger primeQ) {
    this(calculateParameters(primeP, primeQ));
  } // end constructor */

  /**
   * Constructor uses given primes p and q and given public exponent.
   *
   * @param primeP first prime number
   * @param primeQ second prime number
   * @param publicExponent public exponent
   * @throws IllegalArgumentException if gcd(e, (p-1)(q-1)) != 1
   */
  public RsaPrivateCrtKeyImpl(
      final BigInteger primeP, final BigInteger primeQ, final BigInteger publicExponent) {
    // CT_CONSTRUCTOR_THROW
    this(calculateParameters(primeP, primeQ, publicExponent));
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param modulus modulus n
   * @param publicExponent public exponent e
   * @param privateExponent private exponent d
   * @param primeP prime p
   * @param primeQ prime q
   * @param primeExponentP prime exponent p, {@code dP = d mod (p - 1)}
   * @param primeExponentQ prime exponent q, {@code dQ = d mod (q - 1)}
   * @param crtCoefficient CRT coefficient, {@code c = q^(-1) mod p}
   */
  public RsaPrivateCrtKeyImpl(
      final BigInteger modulus,
      final BigInteger publicExponent,
      final BigInteger privateExponent,
      final BigInteger primeP,
      final BigInteger primeQ,
      final BigInteger primeExponentP,
      final BigInteger primeExponentQ,
      final BigInteger crtCoefficient) {
    // Note 1: SonarQube claims the following major finding:
    //         "Constructor has 8 parameters, which is greater than 7 authorized."
    // Note 2: This will NOT be fixed.
    //         Private RSA keys with CRT need eight parameters.
    //         I will not combine parameters to just satisfy SonarQube.
    super(modulus, publicExponent, privateExponent);

    insPrimeP = primeP;
    insPrimeQ = primeQ;
    insPrimeExponentP = primeExponentP;
    insPrimeExponentQ = primeExponentQ;
    insCrtCoefficient = crtCoefficient;
  } // end constructor */

  /**
   * Constructor using given parameters.
   *
   * @param crtValues {n, e, d, p, q, dP, dQ, c}
   */
  private RsaPrivateCrtKeyImpl(final BigInteger... crtValues) {
    this(
        crtValues[0],
        crtValues[1],
        crtValues[2],
        crtValues[3],
        crtValues[4],
        crtValues[5],
        crtValues[6],
        crtValues[7]);
  } // end constructor */

  /**
   * From TLV constructor, interprets a TLV-structure according to given format.
   *
   * <p>This is the counterpart to {@link #getEncoded(EafiRsaPrkFormat)}.
   *
   * @param keyMaterial as TLV-object
   * @param format indicates how to interpret the {@code keyMaterial}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>format is not in set {@link EafiRsaPrkFormat}
   *       <li>structure of {@code keyMaterial} differs from specification, see {@link
   *           EafiRsaPrkFormat}
   *     </ol>
   */
  public RsaPrivateCrtKeyImpl(final DerSequence keyMaterial, final EafiRsaPrkFormat format) {
    this(tlv2BigIntegerArray(keyMaterial, format));
  } // end constructor */

  /**
   * Kind of copy constructor.
   *
   * @param privateKey private key from which attributes are taken
   */
  public RsaPrivateCrtKeyImpl(final RSAPrivateCrtKey privateKey) {
    this(
        privateKey.getModulus(),
        privateKey.getPublicExponent(),
        privateKey.getPrivateExponent(),
        privateKey.getPrimeP(),
        privateKey.getPrimeQ(),
        privateKey.getPrimeExponentP(),
        privateKey.getPrimeExponentQ(),
        privateKey.getCrtCoefficient());
  } // end constructor

  /**
   * Takes primes p and q, estimates public exponent e and calculates all other parameters of an RSA
   * key pair.
   *
   * <p>The public exponent e is estimated as follows: If the modulus length is
   *
   * <ul>
   *   <li>64 bit or smaller then {@code eMin = F0 = 2^2^0 + 1 = 3}
   *   <li>else {code eMin = F4 = 2^2^4+1 = 65537}
   * </ul>
   *
   * <p>The smallest number greater than or equal to {@code eMin} fulfilling {@code gcd(e,
   * (p-1)(q-1)) == 1} is taken as public exponent e.
   *
   * @return {n, e, d, p, q, dP, dQ, c}
   */
  private static BigInteger[] calculateParameters(
      final BigInteger primeP, final BigInteger primeQ) {
    final var pMinus1 = primeP.subtract(ONE);
    final var qMinus1 = primeQ.subtract(ONE);
    final var modulus = primeP.multiply(primeQ);
    var pubExpoCandidate = BigInteger.valueOf((modulus.bitLength() <= 64) ? 3 : 65_537);
    while ((0 != ONE.compareTo(pubExpoCandidate.gcd(pMinus1)))
        || (0 != ONE.compareTo(pubExpoCandidate.gcd(qMinus1)))) {
      // ... invalid candidate for publicExponent
      pubExpoCandidate = pubExpoCandidate.add(BigInteger.TWO);
    } // end While
    // ... gcd(e, (p-1)(q-1)) = 1

    return calculateParameters(primeP, primeQ, pubExpoCandidate);
  } // end method */

  /**
   * Takes primes p and q and public exponent e and calculates all other parameters of RSA key pair.
   *
   * @param primeP first prime number
   * @param primeQ second prime number
   * @param publicExponent public exponent
   * @return {n, e, d, p, q, dP, dQ, c}
   * @throws IllegalArgumentException if gcd(e, (p-1)(q-1)) != 1
   */
  private static BigInteger[] calculateParameters(
      final BigInteger primeP, final BigInteger primeQ, final BigInteger publicExponent) {
    final BigInteger pMinus1 = primeP.subtract(ONE);
    final BigInteger qMinus1 = primeQ.subtract(ONE);
    final BigInteger pq1 = pMinus1.multiply(qMinus1);

    if (ONE.compareTo(publicExponent.gcd(pq1)) != 0) {
      throw new IllegalArgumentException("gcd(e, (p-1)(q-1)) != 1");
    } // end fi
    // ... gcd(e, (p-1)(q-1)) = 1

    // --- calculate d
    // Note 1: For the following implementation see FIPS 186-4 annex B.3.1, step 3.b or
    //         SOG-IS Crypto Working Group v1.2, January 2020, annex B.3, step 4.
    // Note 2: Make use of: lcm((p-1, q-1) = (p-1)(q-1)/gcd(p-1, q-1)
    final BigInteger privateExponent = publicExponent.modInverse(pq1.divide(pMinus1.gcd(qMinus1)));

    return new BigInteger[] {
      primeP.multiply(primeQ), // modulus
      publicExponent,
      privateExponent,
      primeP,
      primeQ,
      privateExponent.mod(pMinus1), // primeExponentP
      privateExponent.mod(qMinus1), // primeExponentQ
      primeQ.modInverse(primeP) // crtCoefficient
    };
  } // end method */

  /**
   * Key generation based on log2 with public exponent 65537.
   *
   * @param modulusLength in bit
   * @return appropriate private RSA key
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code modulusLength &lt;} {@link #INFIMUM_SECURE_MODULUS}
   *     </ol>
   */
  public static RsaPrivateCrtKeyImpl gakpLog2(final int modulusLength) {
    return gakpLog2(
        modulusLength, INFIMUM_E // F4 = 2^(2^4) + 1 = 65537
        );
  } // end method */

  /**
   * Key generation based on log2.
   *
   * @param modulusLength in bit
   * @param publicExponent positive odd number
   * @return appropriate private RSA key
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code modulusLength &lt;} {@link #INFIMUM_SECURE_MODULUS}
   *       <li>public exponent is less than 3
   *       <li>public exponent is even
   *     </ol>
   */
  public static RsaPrivateCrtKeyImpl gakpLog2(
      final int modulusLength, final BigInteger publicExponent) {
    // Note 1: The algorithm used hereafter is based on the following observations:
    //             n = p * q  => ld(n) = ld(p) + ld(q)                       (1)
    //             epsilon = ld(q) - ld(p)                                   (2)
    //         From equations (1) and (2) it follows:
    //             ld(p) = (ld(n) - epsilon) / 2                             (3)
    //             ld(q) = (ld(n) + epsilon) / 2                             (4)
    // Note 2: Here ld(n) and epsilon are randomly chosen. Thus, based on
    //         equations (3) and (4), candidates for p and q are also random
    //         and independent. On a logarithmic scale, p and q are evenly
    //         distributed.
    // Note 3: References used hereafter are valid in AlgKat version 2016-12-07, i.e.:
    //         Bekanntmachung zur elektronischen Signatur nach dem Signaturgesetz
    //         und der Signaturverordnung (Übersicht über geeignete Algorithmen)
    //         vom 7. Dezember 2016

    // --- check modulusLength
    // Note 4: Intentionally all bit-length above INFIMUM_SECURE_MODULUS are
    //         supported hereafter, rather than the bit-length from table 1.
    // Note 5: The algorithm used hereafter needs a certain bit-length to work
    //         properly.
    if (INFIMUM_SECURE_MODULUS > modulusLength) {
      throw new IllegalArgumentException("invalid modulus length");
    } // end fi
    // ... modulusLength >= INFIMUM_SECURE_MODULUS

    // --- check public exponent
    // Note 6: See clause 3.1 §1
    if (AfiBigInteger.THREE.compareTo(publicExponent) > 0) {
      // ... publicExponent < 3
      throw new IllegalArgumentException("given public exponent too small");
    } // end fi
    // ... e is in range

    if (!publicExponent.testBit(0)) {
      throw new IllegalArgumentException("given public exponent is even");
    } // end fi
    // ... e is odd

    // --- random values
    final double rndModulus = RNG.nextDouble();
    final double rndEpsilon = RNG.nextDouble();

    // --- choose ld(n) and epsilon
    final double ldn = modulusLength - rndModulus;
    final double epsilonMin = 0.1; // see clause 3.1 §4
    final double epsilonSpan = 29.9; // 30 - 0.1, see clause 3.1 §4
    final double epsilon = epsilonMin + rndEpsilon * epsilonSpan;

    return gakpLog2(ldn, epsilon, publicExponent, rndModulus < 0.5);
  } // end method */

  /**
   * Key generation based on given logarithm dualis.
   *
   * @param ldn logarithm dualis for modulus n
   * @param epsilon logarithm dualis for quotient of primes q and p
   * @param publicExponent positive odd number
   * @param bigModulus if {@code TRUE} then is closer to the upper boundary
   * @return appropriate private RSA key
   */
  @VisibleForTesting // otherwise: private
  /* package */ static RsaPrivateCrtKeyImpl gakpLog2(
      final double ldn,
      final double epsilon,
      final BigInteger publicExponent,
      final boolean bigModulus) {
    double ldp = (ldn - epsilon) / 2;
    double ldq = (ldn + epsilon) / 2;

    // Note 1: Assume:
    //         ldp is a randomly chosen value > 100.0                        (1)
    //         ldu  = Math.nextUp(ldp), i.e. the next double value           (2)
    //         p    = 2^ldp = AfiBigInteger.pow2(ldp)                        (3)
    //         u    = 2^ldu = AfiBigInteger.pow2(ldu)                        (4)
    //         dUlp = u - p                                                  (5)
    // Note 2: During prime generation (see method createPrime(double)) prime
    //         candidate p is randomly adjusted. The maximum value of adjustment is
    //         dRnd = BigInteger.ONE.shiftLeft(((int) ldp) - 53)             (6)
    //         quot = dUlp / dRnd                                            (7)
    // Note 3: Experiments show that for ldp around 100 the quotient quot is
    //         above 80. For bigger values of ldp the quotient quot increases.
    //         It follows that the influence of Math.nextUp(double) or
    //         Math.nextDown(double) on the result of prime generation is much
    //         higher than randomly choosing the lower bits of the prime number.
    //         If rndModulus is zero (one) there is a risk that the modulus length
    //         is too small (big) because of rounding errors and alike. To
    //         avoid this, the values ldq (ldp) are manipulated such that
    //         the modulus length stays in range.
    if (bigModulus) {
      // ... modulus possibly close to upper boundary
      //     => make q smaller, so modulus is not too big
      ldq = Math.nextDown(ldq);
    } else {
      // ... modulus possibly close to lower boundary
      //     => make p bigger so modulus is not too small
      ldp = Math.nextUp(ldp);
    } // end else
    final BigInteger p = createPrime(ldp, publicExponent);
    final BigInteger q = createPrime(ldq, publicExponent);

    return new RsaPrivateCrtKeyImpl(calculateParameters(p, q, publicExponent));
  } // end method */

  /**
   * Creates randomly a prime number with (approximately) the given logarithm dualis, which is
   * compatible to the given public exponent.
   *
   * @param ldPrime approximate value of logarithm to base 2 of result
   * @param publicExponent to which the prime number has to be compatible to
   * @return appropriate prime number
   */
  @VisibleForTesting // otherwise = private
  /* package */ static BigInteger createPrime(
      final double ldPrime, final BigInteger publicExponent) {
    // --- calculate most significant bits of prime from parameter ldPrime
    // Note 1: This sets approximately the first 52 bits which is the mantissa of
    //         ldPrime, all other bits in pMSBits are zero.
    final var pMsBits = AfiBigInteger.pow2(ldPrime);

    // --- calculate a random number for the least significant bits in pMSBits.
    // Note 2: Because of this random number ld(prime) differs from ldPrime,
    //         but the difference is smaller than the precision of a double.
    final var primeRandom = new BigInteger(((int) ldPrime) - 53, RNG);

    // --- calculate a candidate for prime number
    // Note 3: The least significant bit of primeRandom is not used in the
    //         candidate because the candidate has to be odd. Therefore, the
    //         least significant bit in primeRandom is used as a sign bit
    //         => pCandidate (hopefully) is equally distributed around pMSBits.
    final var primeCandidate =
        (primeRandom.testBit(0) ? pMsBits.add(primeRandom) : pMsBits.subtract(primeRandom))
            .setBit(0);

    BigInteger result = primeCandidate;
    for (; ; ) {
      if ((0 == ONE.compareTo(publicExponent.gcd(result.subtract(ONE)))) // ... GCD(e, p-1) == 1
          && result.isProbablePrime(CERTAINTY) // ... result is probable prime
      ) {
        // ... result is compatible with public exponent  AND  prime
        //     => return result
        return result;
      } // end fi (compatible with public exponent  AND  prime?)
      // ... result not appropriate
      //     => calculate another candidate

      // Note 4: According to https://en.wikipedia.org/wiki/Prime-counting_function the number
      //         of primes below a number x is
      //               pi(x) = x / ln(x)                                     (1)
      //         If follows that the number of primes between two numbers a and b is:
      //               pi(b) - pi(a) = b / ln(b) - a / ln(a)                 (2)
      // Note 5: In the following formula we define
      //               d   = b - a and                                       (3)
      //               lna = ln(a) and                                       (4)
      //               lnb = ln(b)                                           (5)
      //         It follows:
      //               pi(b) - pi(a) = b / lnb - a / lna
      //                             = (b lna - a lnb) / (lnb lna)           (6)
      //         It is: lnb = ln(a + d) = ln(a(1 + d/a)) = lna + ln(1 + d/a) (7)
      //         Because d <<< a it is ln(1 + d/a) = d/a                     (8)
      //         If follows approximately:
      //             pi(b) - pi(a) = (b lna - a (lna + d/a)) / ((lna + d/a) lna)
      //                           = (b lna - a lna - d) / (lna^2 + lna d/a)
      //                           = (lna (b - a) - d) / (lna^2 + lna d/a)
      //                           = d (lna - 1) / (lna (lna + d/a))
      //         Because of (8) it is lna >>> d/a, thus, it follows:
      //             pi(b) - pi(a) = d (1 - 1/lna) / lna = (approx.) d / lna (9)
      // Note 6: For ln(a) = 16384 (i.e. a prime number with 16384 bit if follows
      //         that in the range of d = Integer.MAX_Value approximately
      //         131072 prime numbers occur. That number is sufficiently large.
      //         It follows that manipulating the four least significant octet
      //         in primeCandidate is sufficient to find a prime number sooner
      //         or later.

      result = primeCandidate.xor(BigInteger.valueOf(RNG.nextInt() & 0x7ffffffe));
    } // end forever-loop
  } // end method */

  /**
   * Extracts BigIntegers from a TLV-structure.
   *
   * @param keyMaterial as TLV-object
   * @param format indicates how to interpret the {@code keyMaterial}
   * @return key parameters {n, e, d, p, q, dP, dQ, c}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>format is not in set {@link EafiRsaPrkFormat}
   *       <li>structure of {@code keyMaterial} differs from specification, see {@link
   *           EafiRsaPrkFormat}
   *     </ol>
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  private static BigInteger[] tlv2BigIntegerArray(
      final DerSequence keyMaterial, final EafiRsaPrkFormat format) {
    try {
      int index = 0;

      // extract version information
      final int version =
          ((DerInteger) keyMaterial.get(DerInteger.TAG, index++).orElseThrow())
              .getDecoded()
              .intValueExact(); // throws ArithmeticException if version is out of int-range

      if (0 != version) {
        throw new IllegalArgumentException(UNKNOWN_VERSION);
      } // end fi
      // ... version == 0

      if (EafiRsaPrkFormat.PKCS1 == format) {
        // extract modulus
        final BigInteger modulus =
            ((DerInteger) keyMaterial.get(DerInteger.TAG, index++).orElseThrow()).getDecoded();

        // extract publicExponent
        final BigInteger publicExponent =
            ((DerInteger) keyMaterial.get(DerInteger.TAG, index++).orElseThrow()).getDecoded();

        // extract privateExponent
        final BigInteger privateExponent =
            ((DerInteger) keyMaterial.get(DerInteger.TAG, index++).orElseThrow()).getDecoded();

        // extract primeP
        final BigInteger primeP =
            ((DerInteger) keyMaterial.get(DerInteger.TAG, index++).orElseThrow()).getDecoded();

        // extract primeQ
        final BigInteger primeQ =
            ((DerInteger) keyMaterial.get(DerInteger.TAG, index++).orElseThrow()).getDecoded();

        // extract primeExponentP
        final BigInteger primeExponentP =
            ((DerInteger) keyMaterial.get(DerInteger.TAG, index++).orElseThrow()).getDecoded();

        // extract primeExponentQ
        final BigInteger primeExponentQ =
            ((DerInteger) keyMaterial.get(DerInteger.TAG, index++).orElseThrow()).getDecoded();

        // extract crtCoefficient
        final BigInteger crtCoefficient =
            ((DerInteger) keyMaterial.get(DerInteger.TAG, index).orElseThrow()).getDecoded();

        return new BigInteger[] {
          modulus,
          publicExponent,
          privateExponent,
          primeP,
          primeQ,
          primeExponentP,
          primeExponentQ,
          crtCoefficient
        }; // end PKCS#1
      } else {
        // ... assuming PKCS#8
        // extract privateKeyAlgorithm
        final DerSequence privateKeyAlgorithm =
            (DerSequence) keyMaterial.get(DerSequence.TAG).orElseThrow();

        // extract oid
        final AfiOid oid =
            ((DerOid) privateKeyAlgorithm.get(DerOid.TAG).orElseThrow()).getDecoded();
        if (!AfiOid.rsaEncryption.equals(oid)) {
          throw new IllegalArgumentException(
              "OID = " + oid.getName() + " instead of rsaEncryption");
        } // end fi
        // ... correct OID

        // extract parameters
        final DerNull parameters = (DerNull) privateKeyAlgorithm.get(DerNull.TAG).orElseThrow();
        if (!parameters.isValid()) {
          throw new IllegalArgumentException("invalid parameters in privateKeyAlgorithm");
        } // end fi
        // ... privateKeyAlgorithm is okay

        final byte[] privateKey =
            ((DerOctetString) keyMaterial.get(DerOctetString.TAG).orElseThrow()).getDecoded();

        return tlv2BigIntegerArray(
            (DerSequence) BerTlv.getInstance(privateKey), EafiRsaPrkFormat.PKCS1); // end PKCS#8
      } // end else
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException(UNKNOWN_VERSION, e);
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException("missing values", e);
    } // end Catch
  } // end method */

  /**
   * Checks whether the parameters of this object form a valid RSA key.
   *
   * <p><i><b>Note:</b> This method does not check for security (see {@link #checkSecurity()}),
   * e.g.:</i>
   *
   * <ol>
   *   <li><i>No modulus length is reported as insecure.</i>
   *   <li><i>If primes p and q are close to each other (small epsilon) or differ much (big epsilon)
   *       this is not reported.</i>
   * </ol>
   *
   * @return {@link String} with check-result, if empty then no errors found
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
  public String check() {
    final List<String> result = new ArrayList<>();

    // --- check if p is really prime
    final BigInteger primeP = getPrimeP();
    if (!primeP.isProbablePrime(CERTAINTY)) {
      result.add(String.format("p= 0x%s is not prime", primeP.toString(16)));
    } // end fi

    // --- check if q is really prime
    final BigInteger primeQ = getPrimeQ();
    if (!primeQ.isProbablePrime(CERTAINTY)) {
      result.add(String.format("q= 0x%s is not prime", primeQ.toString(16)));
    } // end fi

    // calculate (p-1)(q-1)
    final BigInteger pMinus1 = primeP.subtract(ONE);
    final BigInteger qMinus1 = primeQ.subtract(ONE);
    final BigInteger pqMinus1 = pMinus1.multiply(qMinus1);

    // --- check public exponent
    final BigInteger publicExponent = getPublicExponent();
    if (0 != ONE.compareTo(publicExponent.gcd(pqMinus1))) {
      result.add(String.format("1 != gcd(e=0x%s, (p-1)(q-1)", publicExponent.toString(16)));
    } // end fi

    // --- check private exponent
    final BigInteger privateExponent = getPrivateExponent();
    try {
      final BigInteger dShouldNaive = publicExponent.modInverse(pqMinus1);
      final BigInteger dShould = publicExponent.modInverse(pqMinus1.divide(pMinus1.gcd(qMinus1)));
      if (List.of(dShouldNaive, dShould).contains(privateExponent)) {
        // ... d is acceptable
        if (privateExponent.compareTo(dShould) != 0) {
          result.add("d is not in accordance to FIPS 186-4");
        } // end fi
      } else {
        // ... d is not acceptable
        result.add("d is wrong");
      } // end fi
    } catch (ArithmeticException e) {
      // publicExponent is not invertible
      result.add("e not relatively prime to (p-1)(q-1)");
    } // end Catch (...)

    // --- check modulus
    if (getModulus().compareTo(primeP.multiply(primeQ)) != 0) {
      result.add("n is wrong");
    } // end fi

    // --- check dp
    final BigInteger primeExponentP = getPrimeExponentP();
    if (primeExponentP.compareTo(privateExponent.mod(pMinus1)) != 0) {
      result.add("dp is wrong");
    } // end fi

    // --- check dq
    final BigInteger primeExponentQ = getPrimeExponentQ();
    if (primeExponentQ.compareTo(privateExponent.mod(qMinus1)) != 0) {
      result.add("dq is wrong");
    } // end fi

    // --- check c
    final BigInteger crtCoefficient = getCrtCoefficient();
    if (crtCoefficient.compareTo(primeQ.modInverse(primeP)) != 0) {
      result.add("c is wrong");
    } // end fi

    return String.join(LINE_SEPARATOR, result);
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
   *   <li>{@code |q - p| &ge; 2^(n/2 - 100)}, see FIPS 186-4 B.3.3 step 6.4
   *   <li>{@code epsilon = | ld(q/p) | &gt; 30}, see AlgKat clause 3.1 §4.
   * </ol>
   *
   * @return {@link String} with check-result, if empty then no errors found
   */
  @Override
  public String checkSecurity() {
    final List<String> result = new ArrayList<>();

    // --- check modulus length and range of public exponent
    final var findingsSuperclass = super.checkSecurity();
    if (!findingsSuperclass.isEmpty()) {
      result.add(findingsSuperclass);
    } // end fi

    // --- check the difference of p and q
    final var deltaMin = deltaMin(getModulusLengthBit());
    final var delta = getPrimeQ().subtract(getPrimeP()).abs();
    if (deltaMin.compareTo(delta) > 0) {
      result.add("p and q too close together");
    } // end fi

    // --- check epsilon, values from German algorithm catalog AlgKat
    if (!isEpsilonOkay(0.0, 30.0)) {
      result.add("epsilon out of range");
    } // end fi

    return String.join(LINE_SEPARATOR, result);
  } // end method */

  /**
   * Calculates the minimum difference between primes p and q.
   *
   * <p>According to <a
   * href="https://www.sogis.eu/documents/cc/crypto/SOGIS-Agreed-Cryptographic-Mechanisms-1.2.pdf">SOG-IS
   * clause B.3 step 3</a> and <a
   * href="https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.186-4.pdf">FIPS 186-4 B.3.3 step 5.4</a>
   * the primes p and q <b>SHALL</b> not be too close together.
   *
   * @param modulusLength number of bits in modulus
   * @return 2^(n/2 - 100) for modulus length greater than {@link #INFIMUM_SECURE_MODULUS_LENGTH},
   *     otherwise {@link BigInteger#ONE}
   */
  /* package */
  static BigInteger deltaMin(final int modulusLength) {
    if (modulusLength < INFIMUM_SECURE_MODULUS_LENGTH) {
      return ONE;
    } // end fi

    return 0 == (modulusLength % 2)
        ? ONE.shiftLeft((modulusLength >> 1) - 100) // even modulusLength
        : ONE.shiftLeft(modulusLength - 200).sqrt(); // odd  modulusLength
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
    // Note 1: Because this class is not a direct subclass of Object we call super.equals(...).
    //         That method already checks for reflexive, null and class.
    if (!super.equals(obj)) {
      return false;
    } // end fi
    // ... obj not the same as this
    // ... obj not null
    // ... obj has the same class as this

    final RsaPrivateCrtKeyImpl other = (RsaPrivateCrtKeyImpl) obj;

    // --- compare primitive instance attributes (currently none)
    // -/-

    // --- compare reference types
    // ... assertion: instance attributes are never null
    return this.getPrimeP().equals(other.getPrimeP())
        && this.getPrimeQ().equals(other.getPrimeQ())
        && this.getPrimeExponentP().equals(other.getPrimeExponentP())
        && this.getPrimeExponentQ().equals(other.getPrimeExponentQ())
        && this.getCrtCoefficient().equals(other.getCrtCoefficient());
  } // end method */

  @Override
  public byte[] getEncoded() {
    return getEncoded(EafiRsaPrkFormat.PKCS8).getEncoded();
  } // end method

  /**
   * Converts this object to {@link DerSequence}.
   *
   * <p>This is the counterpart to {@link RsaPrivateCrtKeyImpl#RsaPrivateCrtKeyImpl(DerSequence,
   * EafiRsaPrkFormat)}.
   *
   * @param format indicates the structure of the TLV-object.
   * @return returns instance attributes according to parameter {@code format}
   * @throws IllegalArgumentException if the format is not in set {@link EafiRsaPrkFormat}
   */
  public DerSequence getEncoded(final EafiRsaPrkFormat format) {
    if (EafiRsaPrkFormat.PKCS1.equals(format)) {
      // ... PKCS#1
      return new DerSequence(
          List.of(
              new DerInteger(BigInteger.ZERO), // version
              new DerInteger(getModulus()),
              new DerInteger(getPublicExponent()),
              new DerInteger(getPrivateExponent()),
              new DerInteger(getPrimeP()),
              new DerInteger(getPrimeQ()),
              new DerInteger(getPrimeExponentP()),
              new DerInteger(getPrimeExponentQ()),
              new DerInteger(getCrtCoefficient())));
    } else {
      // ... PKCS#8
      return new DerSequence(
          List.of(
              new DerInteger(BigInteger.ZERO), // version
              new DerSequence(List.of(new DerOid(AfiOid.rsaEncryption), DerNull.NULL)),
              new DerOctetString(getEncoded(EafiRsaPrkFormat.PKCS1).getEncoded())));
    } // end else
  } // end method */

  /**
   * Calculates epsilon = |ld(p) - ld(q)|.
   *
   * @return epsilon
   */
  public double getEpsilon() {
    return Math.abs(AfiBigInteger.epsilon(getPrimeP(), getPrimeQ()));
  } // end method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * @return hash-code of object
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    // Note 1: Because this class is not a direct subclass of "Object" we call super.hashCode(...).
    // Note 2: Because equals() takes into account just insValue we can do here the same.
    // Note 3: Instead of simple adding the hash-codes of instance attributes
    //         here, some calculation is done.
    // Note 4: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from the main memory into thread local memory
    if (0 == result) {
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      result = super.hashCode(); // start value from superclass
      final int hashCodeMultiplier = 31; // small prime number

      // --- take into account primitive instance attributes (currently none)
      // -/-

      // --- take into account reference types (currently only insValue)
      result = result * hashCodeMultiplier + getPrimeP().hashCode();
      result = result * hashCodeMultiplier + getPrimeQ().hashCode();
      result = result * hashCodeMultiplier + getPrimeExponentP().hashCode();
      result = result * hashCodeMultiplier + getPrimeExponentQ().hashCode();
      result = result * hashCodeMultiplier + getCrtCoefficient().hashCode();

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Checks whether difference of primes p and q are okay.
   *
   * @param epsilonInfimum lower boundary of range
   * @param epsilonSupremum upper boundary of range
   * @return true if {@code epsilonInfimum <= |ld(p/q)| <= epsilonSupremum}
   */
  public boolean isEpsilonOkay(final double epsilonInfimum, final double epsilonSupremum) {
    final double epsilon = getEpsilon();

    return ((epsilonInfimum <= epsilon) && (epsilon <= epsilonSupremum));
  } // end method */

  /**
   * Implements RSA decryption Primitive (RSADP) from PKCS #1 v2.1 subsection 5.1.2.
   *
   * <p>Implementation uses Chinese Remainder Theorem (CRT parameters).
   *
   * @param cipherText is the cipher text being deciphered
   * @return m is the deciphered plain text
   * @throws IllegalArgumentException if {@code 0 &le; cipherText &lt; modulus} is not fulfilled
   */
  @Override
  public BigInteger pkcs1RsaDp(final BigInteger cipherText) {
    if (cipherText.signum() < 0) {
      throw new IllegalArgumentException("c is too small");
    } // end fi

    final var n = getModulus();

    if (cipherText.compareTo(n) >= 0) {
      throw new IllegalArgumentException("c is too big");
    } // end fi
    // ... 0 <= c < n
    // ... CRT parameters available

    // --- Chinese Remainder Theorem, approx. 3 to 4 times faster than straight forward
    final var p = getPrimeP();
    final var q = getPrimeQ();
    final var dP = getPrimeExponentP();
    final var dQ = getPrimeExponentQ();

    // Note 1: Don't use the following approach, because the sequence of
    //         map-entries is randomly chosen. Thus, the sequence in the
    //         resulting list is not predictable => the result of this
    //         computation depending on m1 and m2 might be wrong.
    // final List<@Nullable BigInteger> list = Map.of(dP, p, dQ, q).entrySet().parallelStream()

    final List<BigInteger> list =
        List.of(List.of(dP, p), List.of(dQ, q)).parallelStream()
            .map(i -> cipherText.modPow(i.getFirst(), i.get(1)))
            .toList();
    final var m1 = list.get(0);
    final var m2 = list.get(1);
    final var crtCoefficient = getCrtCoefficient();

    return m2.add(q.multiply(m1.subtract(m2).multiply(crtCoefficient).mod(p)));
  } // end method */

  /**
   * Returns a {@link String} representation of this instance.
   *
   * @return {@link String} representation of instance attributes
   * @see Object#toString()
   */
  @Override
  public String toString() {
    final var p = AfiBigInteger.toHex(getPrimeP());
    final var q = AfiBigInteger.toHex(getPrimeQ());
    final var dp = AfiBigInteger.toHex(getPrimeExponentP());
    final var dq = AfiBigInteger.toHex(getPrimeExponentQ());
    final var c = AfiBigInteger.toHex(getCrtCoefficient());

    final int maxLength = Stream.of(p, q, dp, dq, c).mapToInt(String::length).max().orElseThrow();

    final String format =
        "%s%n"
            + "p  = %"
            + maxLength
            + "s%n" // NOPMD "s%n" appears often
            + "q  = %"
            + maxLength
            + "s%n"
            + "dp = %"
            + maxLength
            + "s%n"
            + "dq = %"
            + maxLength
            + "s%n"
            + "c  = %"
            + maxLength
            + "s";

    return String.format(format, super.toString(), p, q, dp, dq, c);
  } // end method */

  /**
   * Returns the primeP.
   *
   * @return the primeP
   */
  @Override
  public BigInteger getPrimeP() {
    return insPrimeP;
  } // end method */

  /**
   * Returns the primeQ.
   *
   * @return the primeQ
   */
  @Override
  public BigInteger getPrimeQ() {
    return insPrimeQ;
  } // end method */

  /**
   * Returns the primeExponentP.
   *
   * @return the primeExponentP
   */
  @Override
  public BigInteger getPrimeExponentP() {
    return insPrimeExponentP;
  } // end method */

  /**
   * Returns the primeExponentQ.
   *
   * @return the primeExponentQ
   */
  @Override
  public BigInteger getPrimeExponentQ() {
    return insPrimeExponentQ;
  } // end method */

  /**
   * Returns the crtCoefficient.
   *
   * @return the crtCoefficient
   */
  @Override
  public BigInteger getCrtCoefficient() {
    return insCrtCoefficient;
  } // end method */
} // end class
