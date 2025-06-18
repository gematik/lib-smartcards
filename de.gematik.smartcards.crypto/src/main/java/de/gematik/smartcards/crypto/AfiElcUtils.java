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
import de.gematik.smartcards.utils.AfiUtils;
import java.math.BigInteger;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;

/**
 * This class provides functions for dealing with elliptic curve stuff.
 *
 * <p>The functionality provided by this class is base on <a
 * href="https://www.bsi.bund.de/SharedDocs/Downloads/EN/BSI/Publications/TechGuidelines/TR03111/BSI-TR-03111_V-2-1_pdf">BSI
 * TR-03111</a> version 2.10 from 2018-06-01. If not stated otherwise, this version is referenced
 * throughout this class.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public final class AfiElcUtils {

  /** Private default-constructor. */
  private AfiElcUtils() {
    // intentionally empty
  } // end constructor */

  /**
   * Calculates P + Q according to BSI-TR03111 v2.10 clause 2.3.1.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>One or both points could be the "point of infinity".</i>
   *   <li><i>Assumption: Both points are element of E(Fp), i.e. they are part of the elliptic
   *       curve.</i>
   * </ol>
   *
   * @param p point P on the elliptic curve
   * @param q point Q on the elliptic curve
   * @param dp containing prime which defines the field Fp
   * @return the sum {@code P + Q}
   */
  public static ECPoint add(final ECPoint p, final ECPoint q, final ECParameterSpec dp) {
    if (ECPoint.POINT_INFINITY.equals(p)) {
      // ... p == "point of infinity"
      //     => BSI-TR03111 v2.10 clause 2.3.1 item 1: P + Q = O + Q = Q
      return q;
    } // end fi

    if (ECPoint.POINT_INFINITY.equals(q)) {
      // ... q == "point of infinity"
      //     => BSI-TR03111 v2.10 clause 2.3.1 item 1: P + Q = P + O = P
      return p;
    } // end fi
    // ... neither P nor Q is "point of infinity"

    final var m = ((ECFieldFp) dp.getCurve().getField()).getP();
    final var xp = p.getAffineX();
    final var yp = p.getAffineY();
    final var xq = q.getAffineX();
    final var yq = q.getAffineY();

    if (xp.equals(xq)) {
      // ... P and Q have the same x-coordinate

      if (BigInteger.ZERO.equals(yp.add(yq).mod(m))) {
        // ... P = -Q
        //     => BSI-TR03111 v2.10 clause 2.3.1 item 2: P + Q = P + (-P) = O (point of infinity)
        return ECPoint.POINT_INFINITY;
      } // end fi
      // ... xp == xq  AND  P != -Q
      // ... assumption P and Q are element of E(Fp) => P == Q
      //     => BSI-TR03111 v2.10 clause 2.3.1 item 4: P + Q = P + P = [2]P

      final var a = dp.getCurve().getA();
      final var lambda =
          AfiBigInteger.THREE
              .multiply(xp)
              .multiply(xp)
              .add(a)
              .multiply(yp.shiftLeft(1).modInverse(m))
              .mod(m);
      final var xr = lambda.multiply(lambda).subtract(xp.shiftLeft(1)).mod(m);
      final var yr = lambda.multiply(xp.subtract(xr)).subtract(yp).mod(m);

      return new ECPoint(xr, yr);
    } // end fi
    // ... neither P nor Q is "point of infinity"
    // ... P and Q differ in x-coordinate
    //     => BSI-TR03111 v2.10 clause 2.3.1 item 3

    final var lambda = yq.subtract(yp).multiply(xq.subtract(xp).modInverse(m)).mod(m);
    final var xr = lambda.modPow(BigInteger.TWO, m).subtract(xp).subtract(xq).mod(m);
    final var yr = lambda.multiply(xp.subtract(xr)).subtract(yp).mod(m);

    return new ECPoint(xr, yr);
  } // end method */

  /**
   * Elliptic Curve Key Agreement Algorithm - ECKA according to BSI-TR-03111 v2.10 clause 4.3.1.
   *
   * @param prk private key
   * @param puk public key
   * @return shared point Sab
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>domain parameter of private and public key differ
   *       <li>point representing public key is not on elliptic curve
   *       <li>result of multiplication is "point to infinity"
   *     </ol>
   */
  public static ECPoint ecka(final ECPrivateKey prk, final ECPublicKey puk) {
    return ecka(
        new ECPrivateKeySpec(prk.getS(), prk.getParams()),
        new ECPublicKeySpec(puk.getW(), puk.getParams()));
  } // end method */

  /**
   * Elliptic Curve Key Agreement Algorithm - ECKA according to BSI-TR-03111 v2.10 clause 4.3.1.
   *
   * @param prk private key
   * @param puk public key
   * @return shared point Sab
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>domain parameter of private and public key differ
   *       <li>point representing public key is not on elliptic curve
   *       <li>result of multiplication is "point to infinity"
   *     </ol>
   */
  public static ECPoint ecka(final ECPrivateKeySpec prk, final ECPublicKeySpec puk) {
    final var dpPrk = AfiElcParameterSpec.getInstance(prk.getParams());
    final var dpPuk = AfiElcParameterSpec.getInstance(puk.getParams());

    if (!dpPrk.equals(dpPuk)) {
      // ... mismatch of domain parameter
      throw new IllegalArgumentException("different domain parameter");
    } // end fi
    // ... keys use the same domain parameter

    // ... Assertion: Public key puk is not "point of infinity". Such a value for
    //                public key is prohibited by the constructor of that key.
    final var pukW = puk.getW();
    if (!isPointOnEllipticCurve(pukW, dpPrk)) {
      // ... point is not on curve
      //     => invalid public key
      throw new IllegalArgumentException("invalid public key");
    } // end fi
    // ... public key uses point which is an element of elliptic curve but not "point of infinity"

    final var prkD = prk.getS();
    final var n = dpPrk.getOrder();
    final var h = BigInteger.valueOf(dpPrk.getCofactor());

    final var l = h.modInverse(n); // step 1
    final var q = multiply(h, pukW, dpPrk); // step 2
    final var sab = multiply(prkD.multiply(l).mod(n), q, dpPrk); // step 3

    if (ECPoint.POINT_INFINITY.equals(sab)) {
      throw new IllegalArgumentException("ERROR");
    } // end fi
    // ... Sab is not PointOfInfinity

    // step 4 is optional and omitted here

    return sab; // step 5
  } // end method */

  /**
   * Elliptic Curve Key Agreement Algorithm - ECKA according to BSI-TR-03111 v2.10 clause 4.3.1.
   *
   * @param prk private key
   * @param puk public key
   * @return shared secret Zab
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>domain parameter of private and public key differ
   *       <li>point representing public key is not on elliptic curve
   *       <li>result of multiplication is "point to infinity"
   *     </ol>
   */
  public static byte[] sharedSecret(final ECPrivateKey prk, final ECPublicKey puk) {
    final var sab = ecka(prk, puk);
    // ... no exception so far
    //     => prk and puk use the same domain parameters

    return fe2os(sab.getAffineX(), puk.getParams()); // step 4, step 5
  } // end method */

  /**
   * Converts a field element to an octet string according to BSI-TR03111 v2.0 clause 3.1.3.
   *
   * @param fieldElement to be converted to an octet string
   * @param dp domain parameter containing prime p which defines the field Fp
   * @return octet string representation of the given field element
   * @throws IllegalArgumentException if {@code fieldElement}
   *     <ol>
   *       <li>is negative
   *       <li>greater than prime p in domain parameters {@code dp}
   *     </ol>
   */
  public static byte[] fe2os(final BigInteger fieldElement, final ECParameterSpec dp) {
    final var p = ((ECFieldFp) dp.getCurve().getField()).getP();
    if ((BigInteger.ZERO.compareTo(fieldElement) > 0) || (fieldElement.compareTo(p) >= 0)) {
      throw new IllegalArgumentException("0 > fieldElement >= p");
    } // end fi
    // ... 0 <= fieldElement < p

    final var l = (int) Math.round(Math.ceil(p.bitLength() / 8.0));

    return AfiBigInteger.i2os(fieldElement, l);
  } // end method */

  /**
   * Checks if {@code P} is a point on the elliptic curve E according to BSI-TR03111 v2.10 equation
   * 2.6.
   *
   * @param point for which the check is applied
   * @param dp domain parameter defining the elliptic curve E
   * @return {@code TRUE} if point {@code P= (x, y)} is either "point of infinity" or {@code y² ==
   *     x³ + ax + b}, {@code FALSE} otherwise
   */
  public static boolean isPointOnEllipticCurve(final ECPoint point, final ECParameterSpec dp) {
    if (ECPoint.POINT_INFINITY.equals(point)) {
      // ... point is PointOfInfinity
      return true;
    } // end fi
    // ... point is not PointOfInfinity

    final var x = point.getAffineX();
    final var y = point.getAffineY();
    final var a = dp.getCurve().getA();
    final var b = dp.getCurve().getB();
    final var p = ((ECFieldFp) dp.getCurve().getField()).getP();

    return (y.modPow(BigInteger.TWO, p)
        .equals(x.modPow(AfiBigInteger.THREE, p).add(a.multiply(x)).add(b).mod(p)));
  } // end method */

  /**
   * Calculates [k]P according to BSI-TR03111 v2.10 clause 2.3.1.
   *
   * @param k factor by which P is multiplied, shall be in range {@code [1, n]}
   * @param p point multiplied
   * @param dp domain parameter containing prime P which defines the field Fp
   * @return product [k]P
   * @throws IllegalArgumentException if k is not in range {@code [1, n]}
   */
  public static ECPoint multiply(final BigInteger k, final ECPoint p, final ECParameterSpec dp) {
    if (ECPoint.POINT_INFINITY.equals(p)) {
      return ECPoint.POINT_INFINITY;
    } // end

    // --- check input
    final var n = dp.getOrder();
    if ((1 != k.signum()) || (k.compareTo(n) > 0)) {
      throw new IllegalArgumentException("factor k not in range [1, n]");
    } // end fi
    // ... k in range [1, n]

    // Note 1: The following algorithm is based on "double and add" mechanism.
    //         I.e., each bit in factor k is tested whether it is set (then
    //         something is added to the result) or not set (then nothing is
    //         added to the result). Between rounds the summand is doubled. This
    //         algorithm is based on a binary representation of factor k and the
    //         equation: [k]P = [a0*2^0 + a1*2^1 + ... + ai*2î + ... + am*2^m]P
    //         with ai form set {0, 1}.
    //         E.g.: 18 P = 0*1*P + 1*2*P + 0*4*P + 0*8*P + 1*16*P
    // Note 2: Hereafter xs and ys are the coordinates of the summand S

    // --- initialize "result" depending on the least-significant bit with p or "zero"
    ECPoint result = k.testBit(0) ? p : ECPoint.POINT_INFINITY;

    // --- initialize S = P
    ECPoint s = p;

    // --- loop until all set bits in factor k have been addressed
    for (BigInteger fac = k.shiftRight(1); !BigInteger.ZERO.equals(fac); fac = fac.shiftRight(1)) {
      // double summand S
      s = add(s, s, dp);

      if (fac.testBit(0)) {
        // ... least significant bit b0 is set
        //     => add current summand to result
        result = add(result, s, dp);
      } // end fi
    } // end For (fac...)

    return result;
  } // end method */

  /**
   * Converts an octet string to a field element according to BSI-TR03111 v2.10 clause 3.1.3.
   *
   * @param os octet string to be converted
   * @param dp domain parameter containing prime p which defines the field Fp
   * @return field element
   */
  public static BigInteger os2fe(final byte[] os, final ECParameterSpec dp) {
    final var p = ((ECFieldFp) dp.getCurve().getField()).getP();
    return os2fe(os, p);
  } // end method */

  /**
   * Converts an octet string to a field element according to BSI-TR03111 v2.10 clause 3.1.3.
   *
   * <p>This is the inverse function to {@link #fe2os(BigInteger, ECParameterSpec)}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i> Method {@link #fe2os(BigInteger, ECParameterSpec)} throws an {@link
   *       IllegalArgumentException} if a field element is not in range {@code [0, p-1]}. According
   *       to BSI-TR03111 v2.10 clause 3.1.3 the intermediate result integer {@code X} is taken
   *       {@code mod p}. Thus, any input value for {@code os} is accepted here and the result is
   *       always in range {@code [0, p-1]}</i>
   *   <li><i> According to BSI-TR03111 v2.10 clause 3.1.3 a prime number {@code p} is used to
   *       define the field {@code Fp}. Intentionally this method does not check whether parameter
   *       {@code p} is prime or not.</i>
   * </ol>
   *
   * @param os octet string to be converted
   * @param p prime defining the field Fp
   * @return field element
   * @throws IllegalArgumentException if prime {@code p} is not positive
   */
  public static BigInteger os2fe(final byte[] os, final BigInteger p) {
    if (p.signum() < 1) { // NOPMD literal in if statement
      throw new IllegalArgumentException("p not positive");
    } // end fi
    // ... p is positive, i.e. p > 0

    return new BigInteger(1, os).mod(p);
  } // end method */

  /**
   * Converts given octet string to a point according to BSI-TR03111 v2.0 clause 3.2.
   *
   * @param os octet string to be converted
   * @param dp domain parameter containing prime p which defines the field Fp
   * @return point according to given octet string
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>point is not on elliptic curve
   *       <li>compressed format but p not congruent 3 mod 4
   *       <li>compressed format but {@code x³ + ax + b} not square in Fp
   *     </ol>
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  public static ECPoint os2p(final byte[] os, final AfiElcParameterSpec dp) {
    final var p = dp.getP();

    // --- distinguish encodings by first octet
    // spotless:off
    switch (os[0] & 0xff) {
      case 0x00: // "point of infinity" expected
        return os2p(os, p);

      case 0x04: // uncompressed encoding
        return zzzOs2p04(os, dp, p);

      case 0x02, 0x03: // compressed encoding
      {
        final var a = dp.getCurve().getA();
        final var b = dp.getCurve().getB();
        final var x = os2fe(Arrays.copyOfRange(os, 1, os.length), dp);

        // --- step 1: alpha = x³ + ax + b
        final var alpha = x.modPow(AfiBigInteger.THREE, p).add(a.multiply(x)).add(b).mod(p);
        // ... 0 <= alpha < p

        // --- step 2: check whether alpha is a square in Fp
        // Note 1: For calculating the Legendre-Symbol L(alpha, p) see
        //         https://en.wikipedia.org/wiki/Legendre_symbol
        // Note 2: The result of the Legendre-Symbol is defined to be form set {-1, 0, 1}.
        //         According to the calculation above, the result is from set {p-1, 0, 1}.
        try {
          final var bi1 = BigInteger.ONE;
          final var legendreSymbol =
              alpha.modPow(p.subtract(bi1).shiftRight(1), p).intValueExact();

          // Note 3: Typically it is (p > Integer.MAX_VALUE). Thus, during the
          //         calculation of the Legendre-Symbol typically an
          //         ArithmeticException is thrown. If such an exception is NOT
          //         thrown, that means either Legendre-Symbol is in set "{0, 1}"
          //         or (p <= Integer.MAX_VALUE) in which case the variable
          //         legendreSymbol above calculates to (p - 1).
          // Note 4: The following if-then-else structure intentionally checks
          //         only values from set {0, 1} but not (p - 1).
          //         If legendreSymbol == (p - 1) then the following code
          //         falls through to the end of the try-catch structure and
          //         throws an appropriate exception.

          if (0 == legendreSymbol) {
            // ... alpha congruent 0 mod p
            //     => alpha == 0, because from step 1 it is 0 <= alpha < p

            // --- step 3: if alpha = 0 => y = 0
            return new ECPoint(x, BigInteger.ZERO);
          } else if (1 == legendreSymbol) { // NOPMD literal in if statement
            // ... alpha is a square in Fp

            // --- step 4: compute square root beta of alpha
            if (dp.isPcongruent3mod4()) {
              // ... p = 3 mod 4 (i.e. p congruent 3 mod 4, see BSI_TR-03111, clause  3.2.2)
              //     => beta = alpha^[(p+1)/4] mod p
              final var beta = alpha.modPow(p.add(bi1).shiftRight(2), p);

              // --- step 5: compute y
              final var ypDash = os[0] & 0x01;
              final var y = (ypDash == (beta.byteValue() & 0x01)) ? beta : p.subtract(beta);
              return new ECPoint(x, y);
            } else {
              // ... p not congruent 3 mod 4, don't know how to compute square root
              throw new IllegalArgumentException(
                  "cannot calculate square root, use uncompressed encoding");
            } // end else
          } // end else if
          // ... Legendre-Symbol neither 0 nor 1
          //     => alpha is not a square in Fp
        } catch (ArithmeticException e) { // NOPMD empty catch-block
          // ... Legendre-Symbol calculates to (p-1) i.e, neither 0 nor 1
          //     => alpha is not square in Fp
          // intentionally empty
        } // end Catch (...)

        throw new IllegalArgumentException("alpha is not a square in Fp");
      } // end compressed encoding
      // fall-through

      default:
        throw new IllegalArgumentException("unimplemented encoding");
    } // end Switch
    // spotless:on
  } // end method */

  /**
   * Converts given octet string to a point according to BSI-TR03111 v2.10 clause 3.2.
   *
   * <p>This is the inverse function to {@link #p2osUncompressed(ECPoint, AfiElcParameterSpec)}.
   *
   * <p>Because from the domain parameters just prime {@code p} is given here this function cannot
   * decode points in compressed format (i.e. {@link #p2osCompressed(ECPoint, ECParameterSpec)}).
   *
   * @param os octet string to be converted
   * @param p prime defining the field Fp
   * @return point according to given octet string
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@link #os2fe(byte[], BigInteger)}- method does so
   *       <li>the first octet in {@code os} is '00' but the length of that octet string is greater
   *           than one
   *       <li>the octet string {@code os} is neither {@code '00'} nor does it start with {@code
   *           '04'}
   *       <li>the length of octet string {@code os} is even
   *     </ol>
   */
  public static ECPoint os2p(final byte[] os, final BigInteger p) {
    // --- distinguish encodings by first octet
    // spotless:off
    switch (os[0] & 0xff) {
      case 0x04: {
        if (0 == (os.length & 1)) {
          // ... length of octet string is even
          throw new IllegalArgumentException("length of octet string is even");
        } // end fi

        // --- extract coordinates
        final var len = os.length >> 1;
        final var x = new byte[len];
        final var y = new byte[len];
        System.arraycopy(os, 1, x, 0, len);
        System.arraycopy(os, 1 + len, y, 0, len);

        return new ECPoint(os2fe(x, p), os2fe(y, p));
      } // end uncompressed encoding

      case 0x00: // "point of infinity" expected
        if (1 == os.length) { // NOPMD literal in if statement
          return ECPoint.POINT_INFINITY;
        } // end fi
        // ... "point of infinity" not coded in one octet => intentionally fall through

      case 0x02, 0x03: // intentionally fall through
      default:
        throw new IllegalArgumentException("unimplemented encoding");
    } // end Switch
    // spotless:on
  } // end method */

  /**
   * Converts given octet string to a point according to BSI-TR03111 v2.0 clause 3.2.
   *
   * @param os octet string to be converted
   * @param dp domain parameter containing prime p which defines the field Fp
   * @return point according to given octet string
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>point is not on elliptic curve
   *       <li>compressed format but p not congruent 3 mod 4
   *       <li>compressed format but {@code x³ + ax + b} not square in Fp
   *     </ol>
   */
  private static ECPoint zzzOs2p04(
      final byte[] os, final AfiElcParameterSpec dp, final BigInteger p) {
    final var result = os2p(os, p);

    // --- check if "result" is indeed a point on the elliptic curve
    if (!isPointOnEllipticCurve(result, dp)) {
      throw new IllegalArgumentException("P is not a point on the elliptic curve");
    } // end fi

    return result;
  } // end method */

  /**
   * Converts given point to an octet string according to BSI-TR03111 v2.10 clause 3.2.2 compressed
   * encoding.
   *
   * @param point to be converted
   * @param dp domain parameter containing prime p which defines the field Fp
   * @return compressed encoding of given point
   */
  public static byte[] p2osCompressed(final ECPoint point, final ECParameterSpec dp) {
    if (ECPoint.POINT_INFINITY.equals(point)) {
      return new byte[1];
    } // end fi

    return AfiUtils.concatenate(
        new byte[] {(byte) (point.getAffineY().testBit(0) ? 0x03 : 0x02)},
        fe2os(point.getAffineX(), dp));
  } // end method */

  /**
   * Converts given point to an octet string according to BSI-TR03111 v2.10 clause 3.2.1
   * uncompressed encoding.
   *
   * @param point to be converted
   * @param dp domain parameter containing prime p which defines the field Fp
   * @return uncompressed encoding of given point
   * @throws IllegalArgumentException if the underlying method {@link #fe2os(BigInteger,
   *     ECParameterSpec)} does so
   */
  public static byte[] p2osUncompressed(final ECPoint point, final AfiElcParameterSpec dp) {
    if (ECPoint.POINT_INFINITY.equals(point)) {
      return new byte[1];
    } // end fi

    return AfiUtils.concatenate(
        new byte[] {4}, fe2os(point.getAffineX(), dp), fe2os(point.getAffineY(), dp));
  } // end method */
} // end class
