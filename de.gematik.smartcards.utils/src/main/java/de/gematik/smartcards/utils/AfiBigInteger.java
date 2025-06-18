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
package de.gematik.smartcards.utils;

import static java.math.BigInteger.ONE;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Arrays;

/**
 * This class provides additional methods for {@link BigInteger}.
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li><i>This is a final class and {@link Object} is the direct superclass.</i>
 *   <li><i>This class has no instance attributes and no usable constructor.</i>
 *   <li><i>Thus, this class is an entity-type.</i>
 *   <li><i>It follows that this class intentionally does not implement the following methods:
 *       {@link java.lang.Object#clone() clone()}, {@link java.lang.Object#equals(Object) equals()},
 *       {@link java.lang.Object#hashCode() hashCode()}.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "FL_FLOATS_AS_LOOP_COUNTERS".
//         Spotbugs message: Using floating-point loop counters can lead to unexpected behavior.
//         Rational: That finding is suppressed because usage of double is intentional.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "FL_FLOATS_AS_LOOP_COUNTERS" // see note 1
}) // */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public final class AfiBigInteger {

  /** Number three. */
  public static final BigInteger THREE = BigInteger.valueOf(3); // */

  /** Number three. */
  public static final BigInteger FOUR = BigInteger.valueOf(4); // */

  /** Number three. */
  public static final BigInteger FIVE = BigInteger.valueOf(5); // */

  /** Number three. */
  public static final BigInteger SIX = BigInteger.valueOf(6); // */

  /** Number three. */
  public static final BigInteger SEVEN = BigInteger.valueOf(7); // */

  /** Number three. */
  public static final BigInteger EIGHT = BigInteger.valueOf(8); // */

  /** Number three. */
  public static final BigInteger NINE = BigInteger.valueOf(9); // */

  /** Private default constructor prevents creating instances of this class. */
  private AfiBigInteger() {
    super();
    // intentionally empty
  } // end default constructor */

  /**
   * Calculates {@code ld(a/b) = ld(a) - ld(b)}.
   *
   * <p>The result is better known as epsilon in case a and b are the primes of an RSA key.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameters are immutable and the
   *       return value is primitive.</i>
   * </ol>
   *
   * @param a the first number
   * @param b the second number
   * @return {@code ld(a) - ld(b)}
   * @throws IllegalArgumentException if {@code signum(a) != signum(b)}
   */
  public static double epsilon(final BigInteger a, final BigInteger b) {
    if (a.compareTo(b) == 0) {
      // ... a == b
      //     => epsilon = 0
      return 0;
    } // end fi
    // ... a != b

    if ((a.signum() != b.signum())) {
      throw new IllegalArgumentException("signum(a * b) <= 0");
    } // end fi
    // ... a and b have same sign
    //     => a/b is positive

    final BigInteger p = a.abs(); // NOPMD short variable name
    final BigInteger q = b.abs(); // NOPMD short variable name
    // ... p > 0   and   q > 0

    final BigDecimal x; // NOPMD short variable name
    final BigDecimal y; // NOPMD short variable name
    final double factor;
    // Note: For the following line pitest complains
    //       "changed conditional boundary -> SURVIVED".
    //       That is a false positive, because here p always differs from q.
    //       Thus, it doesn't matter whether the condition is "greater than"
    //       or "greater than or equal".
    if (p.compareTo(q) > 0) { // afiNoPiTest
      x = new BigDecimal(p);
      y = new BigDecimal(q);
      factor = AfiUtils.INVERSE_LN_2;
    } else {
      x = new BigDecimal(q);
      y = new BigDecimal(p);
      factor = -AfiUtils.INVERSE_LN_2;
    } // end fi
    // ... 0 < y < x  =>  x/y > 1

    final double quotient = x.divide(y, MathContext.DECIMAL128).doubleValue();

    final double result;
    // Note: For the following line
    //       1. NOPMD complains "avoid literals in conditional statements".
    //       2. pitest complains "changed conditional boundary -> SURVIVED".
    //          That is a false positive, because here for the precision of the result it doesn't
    //          matter (much) whether the condition is "less than" or "less than or equal to".
    if (quotient < 2) { // NOPMD afiNoPiTest
      // ... x and y are close to each other
      // Note 1: to avoid rounding errors during subtraction here it is:
      //         epsilon = ld(x) - ld(y)
      //                 = ld(x/y)
      //                 = ld(1 + (x-y)/y)
      //                 = log1p((x-y)/y) / ln(2)
      result = Math.log1p(x.subtract(y).divide(y, MathContext.DECIMAL128).doubleValue());
    } else {
      // ... x and y differ so much that rounding errors are negligible
      result = Math.log(quotient);
    } // end fi

    return factor * result;
  } // end method */

  /**
   * Calculates lower boundaries.
   *
   * @param a number
   * @param epsilon to be taken into account
   * @return array with {@code lowerBoundary} and {code upperBoundary}
   */
  private static BigInteger[] initializeBoundaries(final BigInteger a, final double epsilon) {
    final double ldb = ld(a) + epsilon;
    BigInteger lowerBoundary;
    for (double i = ldb; true; i = Math.nextDown(i)) {
      lowerBoundary = pow2(i); // FL_FLOATS_AS_LOOP_COUNTERS
      if (epsilon(lowerBoundary, a) < epsilon) { // changed conditional boundary -> SURVIVED
        // ... epsilon too small
        //     => lowerBoundary is appropriate
        break;
      } // end fi
    } // end For (i...)

    BigInteger upperBoundary;
    for (double i = ldb; true; i = Math.nextUp(i)) {
      upperBoundary = pow2(i); // FL_FLOATS_AS_LOOP_COUNTERS
      if (epsilon(upperBoundary, a) > epsilon) {
        // ... epsilon too large
        //     => upperBoundary is appropriate
        break;
      } // end fi
    } // end For (i...)

    return new BigInteger[] {lowerBoundary, upperBoundary};
  } // end method */

  /**
   * Returns number <i>b</i> as close to number <i>a</i> as possible such that {@code |ld(b/a)| >=
   * |epsilon|}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>For numbers with more than 53 bits there is more than one number b such that {@link
   *       #ld(BigInteger)} for {@code b/a} is constant. Because in the quotient {@code ld(b/a) =
   *       ln(b/a) / ln(2)} precision is lost, it is possible that b increments by one but the
   *       double value {@code ld(b/a)} changes more than one tick.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameters are immutable or
   *       primitive and the return value is immutable.</i>
   * </ol>
   *
   * @param a number
   * @param epsilon to be taken into account
   * @return closest number <i>b</i> such that {@code |ld(b/a)| >= |epsilon|}
   */
  public static BigInteger epsilonInfimum(final BigInteger a, final double epsilon) {
    if (Math.abs(epsilon) < Double.MIN_VALUE) {
      // ... very small epsilon
      //     => just return a
      return a;
    } // end fi
    // ... epsilon not zero

    // --- initialize
    final BigInteger[] boundaries = initializeBoundaries(a, epsilon);
    BigInteger lowerBoundary = boundaries[0];
    BigInteger upperBoundary = boundaries[1];

    // --- find appropriate result
    // Note: TODO Check whether Newton's method gives better performance than bisection.
    //       See also root(BigInteger, int).
    boolean flag = true;
    while (flag) {
      final BigInteger mid = upperBoundary.add(lowerBoundary).shiftRight(1); // (upper + lower) / 2
      final double eps = epsilon(mid, a);
      flag = upperBoundary.subtract(lowerBoundary).compareTo(ONE) > 0;

      // Note: For the following line pitest complains "changed conditional boundary -> SURVIVED".
      //       That is a false positive, because here epsilon always differs from 0. Thus, it
      //       doesn't matter whether the condition is "greater than" or "greater than or equal".
      if (epsilon > 0) { // afiNoPiTest
        // ... epsilon > 0
        //     => a < lowerBoundary < b < upperBoundary
        if (eps >= epsilon) {
          // ... result ok or too big
          upperBoundary = mid;
        } else {
          // ... result too small
          lowerBoundary = mid;
        } // end else
      } else {
        // ... epsilon < 0
        //     => lowerBoundary < b < upperBoundary < a
        if (eps <= epsilon) {
          // ... result ok or too small
          lowerBoundary = mid;
        } else {
          // ... result too big
          upperBoundary = mid;
        } // end else
      } // end else
    } // end While (flag)

    // Note: For the following line pitest complains "changed conditional boundary -> SURVIVED".
    //       That is a false positive, because here epsilon always differs from 0. Thus, it doesn't
    //       matter whether the condition is "greater than" or "greater than or equal".
    return (epsilon > 0) ? upperBoundary : lowerBoundary; // afiNoPiTest
  } // end method */

  /**
   * Returns number <i>b</i> as far away from number <i>a</i> such that {@code |ld(b/a)| <=
   * |epsilon|}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>For numbers with more than 53 bits there is more than one number b such that {@link
   *       #ld(BigInteger)} for {@code b/a} is constant. Because in the quotient {@code ld(b/a) =
   *       ln(b/a) / ln(2)} precision is lost, it is possible that b increments by one but the
   *       double value {@code ld(b/a)} changes more than one tick.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameters are immutable or
   *       primitive and the return value is immutable.</i>
   * </ol>
   *
   * @param a number
   * @param epsilon to be taken into account
   * @return number b as far away as possible such that {@code |ld(b/a)| <= |epsilon|}
   */
  public static BigInteger epsilonSupremum(final BigInteger a, final double epsilon) {
    if (Math.abs(epsilon) < Double.MIN_VALUE) {
      // ... very small epsilon
      //     => just return a
      return a;
    } // end fi
    // ... epsilon not zero

    // --- initialize
    final BigInteger[] boundaries = initializeBoundaries(a, epsilon);
    BigInteger lowerBoundary = boundaries[0];
    BigInteger upperBoundary = boundaries[1];

    // --- find appropriate result
    // Note: TODO Check whether Newton's method gives better performance than bisection.
    //       See also root(BigInteger, int).
    boolean flag = true;
    while (flag) {
      final BigInteger mid = upperBoundary.add(lowerBoundary).shiftRight(1); // (upper + lower) / 2
      final double eps = epsilon(mid, a);
      flag = upperBoundary.subtract(lowerBoundary).compareTo(ONE) > 0;

      // Note: For the following line pitest complains "changed conditional boundary -> SURVIVED".
      //       That is a false positive, because here epsilon always differs from 0. Thus, it
      //       doesn't matter whether the condition is "greater than" or "greater than or equal".
      if (epsilon > 0) { // afiNoPiTest
        // ... epsilon > 0 => a < lowerBoundary < b < upperBoundary
        if (eps > epsilon) {
          // ... result too big
          upperBoundary = mid;
        } else {
          // ... result ok or too small
          lowerBoundary = mid;
        } // end fi
      } else {
        // ... epsilon < 0 => lowerBoundary < b < upperBoundary < a
        if (eps < epsilon) {
          // ... result too small
          lowerBoundary = mid;
        } else {
          // ... result ok or too big
          upperBoundary = mid;
        } // end fi
      } // end fi (epsilon > 0)
    } // end While (...)

    // Note: For the following line pitest complains "changed conditional boundary -> SURVIVED".
    //       That is a false positive, because here epsilon always differs from 0. Thus, it doesn't
    //       matter whether the condition is "greater than" or "greater than or equal".
    return (epsilon < 0) ? upperBoundary : lowerBoundary; // afiNoPiTest
  } // end method */

  /**
   * Converts given number to an octet string of minimal length.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>The result for {@link BigInteger#ZERO} is ´00´.</i>
   *   <li><i>The result never has a leading ´00´ octet, e.g.: +128 = 0x80 is converted to '80'.
   *       -128 is also converted to '80'. 'ff' is the result for -1 and +255.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter is immutable and the
   *       return value is never used again within this class.</i>
   * </ol>
   *
   * @param val number to be converted
   * @return octet string representation of i without leading ´00´ octet
   */
  public static byte[] i2os(final BigInteger val) {
    if (val.compareTo(BigInteger.ZERO) == 0) {
      // ... val == 0 => don't return an empty array
      return new byte[1];
    } // end fi
    // ... val != 0

    final byte[] twosComplement = val.toByteArray();

    if (0 == twosComplement[0]) {
      // ... leading ´00´ => snip it of
      return Arrays.copyOfRange(twosComplement, 1, twosComplement.length);
    } else {
      // ... no leading ´00´ => twosComplement is okay
      return twosComplement;
    } // end fi
  } // end method */

  /**
   * Converts given number to an octet string of given length.
   *
   * <p>If the given {@code length} indicates
   *
   * <ul>
   *   <li>more octet than are available in {@code val}, then an appropriate number of {@code ´00}
   *       octet are inserted at the left.
   *   <li>less octet than are available in {@code val}, then an appropriate number of
   *       least-significant octet are returned.
   * </ul>
   *
   * <p>Examples:
   *
   * <ol>
   *   <li>{@code i2os('1234', 3) = '001234'}, i.e. left padding with ´00´
   *   <li>{@code i2os('1234', 1) = '34'}, i.e. snipping most significant octets
   * </ol>
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If {@code length} is zero then an empty array is returned.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameters are immutable or
   *       primitive and the return value is immutable.</i>
   * </ol>
   *
   * @param val number to be converted
   * @param length number of octets to be returned, if 0 then an empty {@code byte[]} is returned
   * @return octet string representation of given number
   * @throws NegativeArraySizeException if {@code length} is negative
   */
  public static byte[] i2os(final BigInteger val, final int length) {
    if (0 == length) {
      return AfiUtils.EMPTY_OS;
    } // end fi

    final byte[] bytes = val.toByteArray();
    if (bytes.length == length) {
      return bytes;
    } // end fi
    // ... bytes.length != length
    //     =>  array copy necessary

    final byte[] result = new byte[length];

    // Note: For the following line pitest complains "changed conditional boundary -> SURVIVED".
    //       That is a false positive, because here bytes.length != length. Thus, it doesn't
    //       matter whether the condition is "less than" or "less than or equal".
    if (bytes.length < length) { // afiNoPiTest
      // ... val too short
      //     => leading ´00´ necessary
      System.arraycopy(bytes, 0, result, length - bytes.length, bytes.length);
    } else {
      // ... val too long
      //     => snip most significant octet
      System.arraycopy(bytes, bytes.length - length, result, 0, result.length);
    } // end fi

    return result;
  } // end method */

  /**
   * Calculates logarithm (base 2) of given number.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter is immutable and the
   *       return value is primitive.</i>
   * </ol>
   *
   * @param val number whose logarithm is requested
   * @return logarithm (base 2) of {@code val}
   */
  @SuppressWarnings({"PMD.ShortMethodName"})
  public static double ld(final BigInteger val) {
    // --- shift val such that only the 64 MSBit are taken into account
    // Note: According to the Java specification, a double value has a mantissa of 53 bits.
    //       Thus, taking into account the 64 MSBit only is NOT a loss of information when
    //       using Math.log(double).
    final int shift = Math.max(0, val.bitLength() - 64);

    return shift + Math.log(val.shiftRight(shift).doubleValue()) * AfiUtils.INVERSE_LN_2;
  } // end fi

  /**
   * Calculates 2^x.
   *
   * <p>For exponents smaller than 63 an exact value can be calculated. For exponents greater or
   * equal to 63 only the 63 most significant bit might differ from zero, all other bit are set to
   * zero.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter is primitive and return
   *       value is immutable.</i>
   * </ol>
   *
   * @param exp exponent
   * @return number close to 2^x
   */
  public static BigInteger pow2(final double exp) {
    // Note: For the following line pitest complains "changed conditional boundary -> SURVIVED".
    //       That is a false positive, because here it is a matter of precision. Thus, it doesn't
    //       matter whether the condition is "greater than" or "greater than or equal".
    if (exp < 63) { // NOPMD afiNoPiTest avoid literals in conditional statements
      // ... result is so small that we can calculate the exact value here,
      //     although the mantissa of double contains 53 bits only.
      return BigInteger.valueOf(Math.round(Math.pow(2, exp)));
    } // end fi
    // ... exponent too big for a long result

    final int shifter = (int) exp - 62;
    final double exponent = exp - shifter;

    return pow2(exponent).shiftLeft(shifter);
  } // end method */

  /**
   * Calculates 2^(a/b).
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem, because input parameters are primitive and return
   *       value is immutable.</i>
   * </ol>
   *
   * @param a numerator of exponent
   * @param b denominator of exponent
   * @return number close to 2^(a/b)
   * @throws IllegalArgumentException if {@code (a < 0) or (b < 1)}
   */
  public static BigInteger pow2(final int a, final int b) {
    if ((a < 0) || (b < 1)) {
      throw new IllegalArgumentException("parameter(s) too small");
    } // end fi
    // ... a >= 0  AND  b >= 1

    if (0 == a) {
      // ... 2^0 is always one
      return ONE;
    } // end fi

    if (1 == b) { // NOPMD avoid literals in conditional statements
      // ... 2^(a/1) = 2^a
      return BigInteger.ZERO.setBit(a);
    } // end fi
    // ... a >= 1  OR  b >= 2

    // Note: Use "greatest common divisor" to avoid large intermediate numbers.
    final int gcd = BigInteger.valueOf(a).gcd(BigInteger.valueOf(b)).intValueExact();

    return root(BigInteger.ZERO.setBit(a / gcd), b / gcd);
  } // end method */

  /**
   * Returns the first integer smaller than this BigInteger that is probably prime.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter and return value are
   *       immutable.</i>
   * </ol>
   *
   * @param val given number
   * @return the first integer less than {@code val} that is probably prime.
   */
  public static BigInteger previousProbablePrime(final BigInteger val) {
    BigInteger result = val;

    // --- assure that val is odd
    if (!result.testBit(0)) {
      // ... val is even => increment val
      result = result.setBit(0);
    } // end fi

    // --- loop until the previous prime is found
    do {
      result = result.subtract(BigInteger.TWO);
    } while (!result.isProbablePrime(100));

    return result;
  } // end method */

  /**
   * Estimates r-th root of given number.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameters are immutable or
   *       primitive and return value is immutable.</i>
   * </ol>
   *
   * @param a given number
   * @param r indicates which root to estimate
   * @return the biggest number such that its r-th power is less or equal to given number
   */
  public static BigInteger root(final BigInteger a, final int r) {
    if (a.compareTo(ONE) < 0) {
      throw new IllegalArgumentException("number a below 1 is not allowed");
    } // end fi
    // ... a >= 1

    if (r < 1) { // NOPMD avoid literals in conditional statements
      throw new IllegalArgumentException("r = " + r + " is not allowed");
    } else if (1 == r) { // NOPMD avoid literals in conditional statements
      return a;
    } else if (2 == r) { // NOPMD avoid literals in conditional statements
      return a.sqrt();
    } // end fi

    // --- implementation according to Newton's method
    // Note 0: runtime (as measured by corresponding test method with test "e. performance test"
    //         active): more than six times faster than implementation using bisection.
    // Note 1: This implementation is based on Newton's method, see
    //         https://en.wikipedia.org/wiki/Newton%27s_method
    // Note 2: The function is f(x) = x^r - a and x is searched such that f(x) is zero.
    //         f(x) = x^r - a, => f'(x) = r x^(r-1).
    // Note 3: According to Newton's method iterate as follows:
    //         x_(n+1) = x - f(x) / f'(x) = x - (x^r - a) / (r x^(r-1))
    //                 = x + (a - x^r) / (r x^(r-1))
    //                 = x + 1/r [a/x^(r-1) - x]

    // calculate start value
    // Note 4: For numbers with unlimited precision it is:
    //         x = a^(1/r) = 2^ld(x) = 2^ld(a^(1/r)) = 2^(ld(a)/r)
    BigInteger x; // NOPMD short variable name

    // Note 5: This implementation has the best start value.
    // Note 6: 2^ld(a) is incremented hereafter, because of rounding errors 2^ld(a)
    //         usually is too small. If that is the case and the start value is
    //         below 10 then the iteration might end up with a rather large
    //         result, and the following loop is rather time-consuming.
    x = pow2(ld(a) / r).add(ONE);

    final int rDec = r - 1;
    final BigInteger biR = BigInteger.valueOf(r);
    BigInteger delta;
    do {
      // calculate delta
      delta = a.divide(x.pow(rDec)).subtract(x).divide(biR);
      // end implementation 2.b.ii */

      x = x.add(delta);
    } while (delta.compareTo(BigInteger.ZERO) != 0);
    while (x.pow(r).compareTo(a) > 0) {
      x = x.subtract(ONE);
    } // end fi

    return x;
  } // end method */

  /**
   * Converts BigInteger to hex-digits of minimal length.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>The result for {@link BigInteger#ZERO} is ´00´.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter and return value are
   *       immutable.</i>
   * </ol>
   *
   * @param i the number to be converted
   * @return hex-digit representation of input number without leading ´00' octet
   */
  public static String toHex(final BigInteger i) {
    return Hex.toHexDigits(i2os(i));
  } // end method */

  /**
   * Converts BigInteger to hex-digits of given length.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If k is zero an empty array is returned.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameters are immutable or
   *       primitive and return value is immutable.</i>
   * </ol>
   *
   * @param i is the number to be converted
   * @param k number of octet to be returned, if 0 then an empty {@link String} is returned
   * @return octet string representation of i
   * @throws NegativeArraySizeException if k is negative
   */
  public static String toHex(final BigInteger i, final int k) {
    return Hex.toHexDigits(i2os(i, k));
  } // end method */
} // end class
