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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link AfiUtils}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "DLS_DEAD_LOCAL_STORE".
//         Spotbugs short message: This instruction assigns a value to a local
//             variable, but the value is not read or used in any subsequent
//             instruction.
//         Rational: The finding is a "false positive".
// Note 2: Spotbugs claims: "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT".
//         Spotugs short message: This code calls a method and ignores the
//             return value.
//         Rational: Return values cannot be observed if the method produces an
//             exception.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "DLS_DEAD_LOCAL_STORE", // see note 1
  "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", // see note 2
}) // */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestAfiBigInteger {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Test strategy:
    // --- a. use constants defined in class
    assertEquals(3, AfiBigInteger.THREE.intValueExact());
    assertEquals(4, AfiBigInteger.FOUR.intValueExact());
    assertEquals(5, AfiBigInteger.FIVE.intValueExact());
    assertEquals(6, AfiBigInteger.SIX.intValueExact());
    assertEquals(7, AfiBigInteger.SEVEN.intValueExact());
    assertEquals(8, AfiBigInteger.EIGHT.intValueExact());
    assertEquals(9, AfiBigInteger.NINE.intValueExact());
  } // end method */

  /** Method executed after other tests. */
  @AfterAll
  static void tearDownAfterClass() {
    // intentionally empty
  } // end method */

  /** Method executed before each test. */
  @BeforeEach
  void setUp() {
    // intentionally empty
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /** Test method for {@link AfiBigInteger#epsilon(BigInteger, BigInteger)}. */
  @Test
  void test_epsilon__BigInteger_BigInteger() {
    // Test strategy:
    // --- a. smoke test with small numbers and integer epsilon
    // --- b. smoke test with small numbers and rational epsilon
    // --- c. high precision, even for huge numbers
    // --- d. random input
    // --- e. IllegalArgumentException

    // --- a. smoke test with small numbers and integer epsilon
    for (final var i :
        List.of(
            //            a, b, epsilon_expected
            new double[] {0, 0, 0},
            new double[] {1, 2, -1},
            new double[] {16, 4, 2},
            new double[] {-16, -4, 2},
            new double[] {1e12, 1e12 * Math.pow(2.0, 1.0 / 8), -1.0 / 8},
            new double[] {(1L << 62), 1, 62})) {
      final BigInteger a = BigInteger.valueOf((long) i[0]); // NOPMD short variable name
      final BigInteger b = BigInteger.valueOf((long) i[1]); // NOPMD short variable name
      final double e = i[2]; // NOPMD short variable name

      // check identity
      assertEquals(0.0, AfiBigInteger.epsilon(a, a));
      assertEquals(0.0, AfiBigInteger.epsilon(b, b));

      // check epsilon of a and b
      assertEquals(e, AfiBigInteger.epsilon(a, b), 1e-12);
    } // end For (i...)
    // end --- a.

    // --- b. smoke test with small numbers and rational epsilon
    List.of(new Object[] {65_537, 17, 11.912_559_172_361_02}, new Object[] {2, 4, -1.0})
        .forEach(
            i -> {
              final BigInteger a = BigInteger.valueOf((int) i[0]); // NOPMD short variable name
              final BigInteger b = BigInteger.valueOf((int) i[1]); // NOPMD short variable name
              final double e = (double) (i[2]); // NOPMD short variable name

              // check identity
              assertEquals(0.0, AfiBigInteger.epsilon(a, a));
              assertEquals(0.0, AfiBigInteger.epsilon(b, b));

              // check epsilon of a and b
              assertEquals(e, AfiBigInteger.epsilon(a, b), 1e-12);
            }); // end forEach(i -> ...)

    // --- c. high precision, even for huge numbers
    final BigInteger x200001 = BigInteger.valueOf(0x200001);
    final BigInteger x1 = ONE; // NOPMD short variable name
    List.of(
            new BigInteger[] {x200001.shiftLeft(107), x1.shiftLeft(0), BigInteger.valueOf(128)},
            new BigInteger[] {x200001.shiftLeft(208), x1.shiftLeft(100), BigInteger.valueOf(129)},
            new BigInteger[] {x200001.shiftLeft(1002), x1.shiftLeft(0), BigInteger.valueOf(1023)})
        .forEach(
            i -> {
              final BigInteger a = i[0]; // NOPMD short variable name
              final BigInteger b = i[1]; // NOPMD short variable name
              final double e = i[2].doubleValue(); // NOPMD short variable name
              final double d = e / AfiBigInteger.epsilon(a, b) - 1; // NOPMD , relative difference

              // check identity
              assertEquals(0.0, AfiBigInteger.epsilon(a, a));
              assertEquals(0.0, AfiBigInteger.epsilon(b, b));

              // check epsilon of a and b
              assertEquals(0, d, 1e-8);
            }); // end forEach(i -> ...)

    // --- d. random input
    IntStream.range(0, 10)
        .forEach(
            i -> {
              final int a = RNG.nextIntClosed(1, Integer.MAX_VALUE); // NOPMD short variable name
              final int b = RNG.nextIntClosed(1, Integer.MAX_VALUE); // NOPMD short variable name
              final double expected =
                  AfiBigInteger.epsilon(BigInteger.valueOf(a), BigInteger.valueOf(b));
              if (a == b) {
                assertEquals(0.0, expected);
              } else {
                // ... a != b
                final var actual = Math.log(((double) a) / b) * AfiUtils.INVERSE_LN_2;
                assertEquals(expected, actual, 1e-15, () -> "delta = %e" + (expected - actual));
              } // end fi
            }); // end forEach(i -> ...)

    // --- e. IllegalArgumentException
    List.of(
            new int[] {-1, 1}, // a < 0 < b
            new int[] {1, -1}, // b < 0 < a
            new int[] {0, 1},
            new int[] {0, -1},
            new int[] {1, 0},
            new int[] {-1, 0})
        .forEach(
            i -> {
              final BigInteger a = BigInteger.valueOf(i[0]); // NOPMD short variable name
              final BigInteger b = BigInteger.valueOf(i[1]); // NOPMD short variable name
              final Throwable thrown =
                  assertThrows(IllegalArgumentException.class, () -> AfiBigInteger.epsilon(a, b));
              assertEquals("signum(a * b) <= 0", thrown.getMessage());
              assertNull(thrown.getCause());
            }); // end forEach(i -> ...)
  } // end method */

  /** Test method for {@link AfiBigInteger#epsilonInfimum(java.math.BigInteger, double)}. */
  @Test
  void test_epsilonInfimum__BigInteger_double() {
    // Assertions:
    // ... a. epsilon(a, b)-method works as expected

    // Test strategy:
    // --- a. smoke test with manually checked result
    // --- b. random BigInteger and various epsilon
    // --- c. checks for small epsilon

    // --- a. smoke test with manually checked result
    List.of(
            new String[] { // e = -1,0000000000000000e-04
              "d72f76ded24d78ecf80e8146a1c37d963921d39ff837a8930814203a1e0d00ad878"
                  + "8f7096e476f86c2b24e1ee7981c0f32dabe1b4cad6e2c8b9d4035ae7dd70e",
              "bf1a36e2eb1c432d",
              "d72ba566a9a843c6d7d4624b865bd2789b0ad0bb3d760fb1b75ae6951440df14520"
                  + "5d3af82b0f7a2ecb8ae794704459c3d13fe92175d627b0f89ccb934d7e969",
            },
            new String[] { // e = 3,0000000000000000e-01
              "8c0d5f4bcd52f503a1b07ccfeaa657ee",
              "3fd3333333333333",
              "ac6cadcf61dd194f309ffb1cef0d4e5d",
            })
        .forEach(
            i -> {
              final BigInteger a = new BigInteger(i[0], 16); // NOPMD short variable name
              final double e = // NOPMD short variable name
                  Double.longBitsToDouble(new BigInteger(Hex.toByteArray(i[1])).longValueExact());
              final BigInteger b = new BigInteger(i[2], 16); // NOPMD short variable name
              final BigInteger p = AfiBigInteger.epsilonInfimum(a, e); // NOPMD short variable name

              // check present result
              assertEquals(b, p);
            }); // end forEach(i ->)

    // --- b. random BigInteger and various epsilon
    List.of(
            1.0e-10,
            1.0e-8,
            1.0e-6,
            1.0e-5,
            2.2e-5,
            4.7e-5, // E3 sequence
            1.0e-4,
            2.2e-4,
            4.7e-4, // E3 sequence
            0.001,
            0.015,
            0.022,
            0.033,
            0.047,
            0.068, // E6 sequence
            .100,
            .121,
            .147,
            .178,
            .215,
            .261,
            .315,
            .333,
            .464,
            .562,
            .681,
            .826, // E12 sequence
            1.00,
            1.21,
            1.47,
            1.78,
            2.15,
            2.61,
            3.15,
            3.33,
            4.64,
            5.62,
            6.81,
            8.26, // E12 sequence
            10.0,
            15.0,
            22.0,
            33.0,
            47.0,
            68.0 // E6 sequence
            )
        .forEach(
            epsilon -> {
              List.of(256, 512, 4096)
                  .forEach(
                      noBits -> {
                        final BigInteger a =
                            new BigInteger(noBits, RNG).setBit(noBits - 1); // NOPMD short name
                        assertEquals(noBits, a.bitLength());

                        // b.1 check positive epsilon, i.e. b > a
                        final BigInteger b =
                            AfiBigInteger.epsilonInfimum(a, epsilon); // NOPMD short name

                        // b.1.i  check that b is the closest number, i.e. epsilon(b - 1, a) <
                        // epsilon
                        assertTrue(AfiBigInteger.epsilon(b.subtract(ONE), a) < epsilon);

                        // b.1.ii check that epsilon(b, a) >= epsilon
                        assertTrue(AfiBigInteger.epsilon(b, a) >= epsilon);

                        // b.2 check negative epsilon, i.e. c < a
                        final BigInteger c =
                            AfiBigInteger.epsilonInfimum(a, -epsilon); // NOPMD short name

                        // b.2.i  check that c is closest number, i.e. epsilon(c + 1, a) > epsilon
                        assertTrue(AfiBigInteger.epsilon(c.add(ONE), a) > -epsilon);

                        // b.2.ii check that epsilon(c, a) <= epsilon
                        assertTrue(AfiBigInteger.epsilon(c, a) <= -epsilon);
                      }); // end forEach(numbits -> ...)
            }); // end forEach(epsilon -> ...)

    // --- c. checks for small epsilon
    // c.1 epsilon is zero
    double epsilon = 0;
    final BigInteger dutC1 = new BigInteger(100, RNG);
    assertSame(dutC1, AfiBigInteger.epsilonInfimum(dutC1, epsilon));

    // c.2 epsilon is smallest possible value
    epsilon = Double.MIN_VALUE; // 2^-1074
    final BigInteger dutC2 = ONE.shiftLeft(2048);
    final BigInteger preC2 = AfiBigInteger.epsilonInfimum(dutC2, epsilon);
    assertTrue(dutC2.compareTo(preC2) < 0);
  } // end method */

  /** Test method for {@link AfiBigInteger#epsilonSupremum(java.math.BigInteger, double)}. */
  @Test
  void test_epsilonSupremum__BigInteger_double() {
    // Assertions:
    // ... a. epsilon(a, b)-method works as expected

    // Test strategy:
    // --- a. smoke test with manually checked result
    // --- b. random BigInteger and various epsilon
    // --- c. checks for small epsilon

    // --- a. smoke test with manually checked result
    List.of(
            new String[] { // e = 2,1345000000000000e+01
              "58d14557ffa5b973baa79ce00febf0c3",
              "40355851eb851eb8",
              "e19f88ac79185d3d24234134219d3c56b722c",
            },
            new String[] { // e = -1,0000000000000000e-10
              "568b43f358ed37273e734288b6b23971",
              "bddb7cdfd9d7bdbb",
              "568b43f33f297e90af3887e4d00f01d7",
            })
        .forEach(
            i -> {
              final BigInteger a = new BigInteger(i[0], 16); // NOPMD short variable name
              final double e = // NOPMD short variable name
                  Double.longBitsToDouble(new BigInteger(Hex.toByteArray(i[1])).longValueExact());
              final BigInteger b = new BigInteger(i[2], 16); // NOPMD short variable name
              final BigInteger p = AfiBigInteger.epsilonSupremum(a, e); // NOPMD short variable name

              // check present result
              assertEquals(b, p);
            }); // end forEach(i ->)

    // --- b. random BigInteger and various epsilon
    List.of(
            1.0e-10,
            1.0e-8,
            1.0e-6,
            1.0e-5,
            2.2e-5,
            4.7e-5, // E3 sequence
            1.0e-4,
            2.2e-4,
            4.7e-4, // E3 sequence
            0.001,
            0.015,
            0.022,
            0.033,
            0.047,
            0.068, // E6 sequence
            .100,
            .121,
            .147,
            .178,
            .215,
            .261,
            .315,
            .333,
            .464,
            .562,
            .681,
            .826, // E12 sequence
            1.00,
            1.21,
            1.47,
            1.78,
            2.15,
            2.61,
            3.15,
            3.33,
            4.64,
            5.62,
            6.81,
            8.26, // E12 sequence
            10.0,
            15.0,
            22.0,
            33.0,
            47.0,
            68.0 // E6 sequence
            )
        .forEach(
            epsilon -> {
              List.of(256, 512, 4096)
                  .forEach(
                      noBits -> {
                        final BigInteger a =
                            new BigInteger(noBits, RNG).setBit(noBits - 1); // NOPMD short name
                        assertEquals(noBits, a.bitLength());

                        // b.1 check positive epsilon, i.e. b > a
                        final BigInteger b =
                            AfiBigInteger.epsilonSupremum(a, epsilon); // NOPMD short name

                        // b.1.i  check that epsilon(b,a) <= epsilon
                        assertTrue(AfiBigInteger.epsilon(b, a) <= epsilon);

                        // b.1.ii check that b is farest number, i.e. epsilon(b + 1, a) > epsilon
                        assertTrue(AfiBigInteger.epsilon(b.add(ONE), a) > epsilon);

                        // b.2 check negative epsilon, i.e. c < a
                        final BigInteger c =
                            AfiBigInteger.epsilonSupremum(a, -epsilon); // NOPMD short name

                        // b.2.i  check that epsilon(c, a) >= epsilon
                        assertTrue(AfiBigInteger.epsilon(c, a) >= -epsilon);

                        // b.2.ii check that c is farest number, i.e. epsilon(c - 1, a) < epsilon
                        assertTrue(AfiBigInteger.epsilon(c.subtract(ONE), a) < -epsilon);
                      }); // end forEach(numbits -> ...)
            }); // end forEach(epsilon -> ...)

    // --- c. checks for small epsilon
    // c.1 epsilon is zero
    double epsilon = 0;
    final BigInteger dutC1 = new BigInteger(100, RNG);
    assertSame(dutC1, AfiBigInteger.epsilonSupremum(dutC1, epsilon));

    // c.2 epsilon is smallest possible value
    epsilon = Double.MIN_VALUE; // 2^-1074
    final BigInteger dutC2 = ONE.shiftLeft(2048);
    final BigInteger preC2 = AfiBigInteger.epsilonSupremum(dutC2, epsilon);
    assertTrue(dutC2.compareTo(preC2) < 0);
  } // end method */

  /** Test method for {@link AfiBigInteger#i2os(java.math.BigInteger)}. */
  @Test
  void test_i2os__BigInteger() {
    // Test strategy:
    // --- a. check {0, +128, 128, -1, 255}
    // --- b. check all two byte values
    // --- c. check lot of random input
    // --- d. check special integers

    // --- a. check 0 as input
    assertArrayEquals(new byte[1], AfiBigInteger.i2os(BigInteger.ZERO), "00");
    Map.ofEntries(Map.entry("80", List.of(-128, 128)), Map.entry("ff", List.of(-1, 255)))
        .forEach(
            (output, input) ->
                input.stream()
                    .map(BigInteger::valueOf)
                    .forEach(
                        i ->
                            assertEquals(
                                output,
                                Hex.toHexDigits(
                                    AfiBigInteger.i2os(
                                        i))))); // end forEach((output, input) -> ...)

    // --- b. check all two byte values
    IntStream.rangeClosed(-32_768, 32_767)
        .forEach(
            i -> {
              final String exp =
                  ((-128 <= i) && (i <= 255))
                      ? String.format("%02x", i & 0xff)
                      : String.format("%04x", i & 0xffff);

              assertEquals(exp, Hex.toHexDigits(AfiBigInteger.i2os(BigInteger.valueOf(i))));
            }); // end forEach(i -> ...)

    // --- c. check lot of random input
    IntStream.range(1, 512)
        .forEach(
            i -> {
              final BigInteger dut = new BigInteger(1, RNG.nextBytes(i));
              final byte[] a = AfiBigInteger.i2os(dut); // NOPMD short variable name
              final byte[] b = dut.toByteArray(); // NOPMD short variable name
              if (a.length == b.length) {
                // ... a and b have same length
                assertArrayEquals(a, b, () -> "same length: " + dut.toString(16));
              } else {
                // ... a and b differ in length, leading '00' expected in b
                assertEquals(b.length - 1, a.length);
                assertEquals(0, b[0]);
                assertEquals(Hex.toHexDigits(b), "00" + Hex.toHexDigits(a));
              } // end else
            }); // end forEach(i -> ...)

    // --- d. check special integers
    List.of(
            "8000000000000000",
            "8000000000000001",
            "fffffffffffffffe",
            "ffffffffffffffff",
            "0800000000000000",
            "0800000000000001",
            "0ffffffffffffffe",
            "0fffffffffffffff",
            "ff",
            "00",
            "01",
            "23",
            "0345",
            "4567",
            "056789",
            "7ffffffe",
            "7fffffff",
            "80000000",
            "80000001",
            "07fffffe",
            "07ffffff",
            "08000000",
            "08000001",
            "07fffffffffffffe",
            "07ffffffffffffff",
            "7ffffffffffffffe",
            "7fffffffffffffff")
        .forEach(
            i ->
                assertEquals(
                    i,
                    Hex.toHexDigits(
                        AfiBigInteger.i2os(new BigInteger(i, 16))))); // end forEach(i ->)
  } // end method */

  /** Test method for {@link AfiBigInteger#i2os(java.math.BigInteger, int)}. */
  @Test
  void test_i2os__BigInteger_int() {
    // Test strategy:
    // --- a. Convert 0 to various lengths
    // --- b. Convert a negative number to various lengths
    // --- c. Convert a positive number to various lengths
    // --- d. Check negative length

    // --- a. Convert 0 to various lengths
    {
      final var dut = BigInteger.ZERO;
      IntStream.range(
              0, // infimum of valid length
              10 // manually chosen value for upper boundary of range
              )
          .forEach(
              length -> {
                final byte[] exp = new byte[length];
                assertEquals(
                    Hex.toHexDigits(exp), Hex.toHexDigits(AfiBigInteger.i2os(dut, length)));
              }); // end forEach(length -> ...)
    } // end --- a.

    // --- b. Convert a negative number to various lengths
    final BigInteger negative = BigInteger.valueOf(0xfedcba9876543210L);
    assertTrue(negative.compareTo(BigInteger.ZERO) < 0);
    Map.ofEntries(
            Map.entry(0, ""),
            Map.entry(1, "10"),
            Map.entry(2, "3210"),
            Map.entry(3, "543210"),
            Map.entry(4, "76543210"),
            Map.entry(5, "9876543210"),
            Map.entry(6, "ba9876543210"),
            Map.entry(7, "dcba9876543210"),
            Map.entry(8, "fedcba9876543210"),
            Map.entry(9, "00fedcba9876543210"),
            Map.entry(10, "0000fedcba9876543210"),
            Map.entry(11, "000000fedcba9876543210"))
        .forEach(
            (length, exp) ->
                assertEquals(
                    exp,
                    Hex.toHexDigits(
                        AfiBigInteger.i2os(
                            negative, length)))); // end forEach((length, exp) -> ...)

    // --- c. Convert a positive number to various lengths
    final BigInteger positive = BigInteger.valueOf(0x0123456789abcdefL);
    assertTrue(positive.compareTo(BigInteger.ZERO) > 0);
    Map.ofEntries(
            Map.entry(0, ""),
            Map.entry(1, "ef"),
            Map.entry(2, "cdef"),
            Map.entry(3, "abcdef"),
            Map.entry(4, "89abcdef"),
            Map.entry(5, "6789abcdef"),
            Map.entry(6, "456789abcdef"),
            Map.entry(7, "23456789abcdef"),
            Map.entry(8, "0123456789abcdef"),
            Map.entry(9, "000123456789abcdef"),
            Map.entry(10, "00000123456789abcdef"),
            Map.entry(11, "0000000123456789abcdef"))
        .forEach(
            (length, exp) ->
                assertEquals(
                    exp,
                    Hex.toHexDigits(
                        AfiBigInteger.i2os(
                            positive, length)))); // end forEach((length, exp) -> ...)

    // --- d. Check negative length
    List.of(
            -1, // supremum of invalid values
            Integer.MIN_VALUE // infimum  of invalid values
            )
        .forEach(
            l -> {
              final Throwable thrown =
                  assertThrows(
                      NegativeArraySizeException.class,
                      () -> AfiBigInteger.i2os(ONE, l)); // RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT
              assertEquals(Integer.toString(l), thrown.getMessage());
              assertNull(thrown.getCause());
            }); // end forEach(length -> ...)
  } // end method */

  /** Test method for {@link AfiBigInteger#ld(java.math.BigInteger)}. */
  @Test
  void test_ld__BigInteger() {
    // Test strategy:
    // --- a. check zero value
    // --- b. check negative numbers
    // --- c. small smoke test values
    // --- d. random small values
    // --- e. a lot of big numbers

    // --- a. check zero value
    assertEquals(Double.NEGATIVE_INFINITY, AfiBigInteger.ld(BigInteger.ZERO));

    // --- b. check negative numbers
    Stream.of(
            -1, // supremum for negative numbers
            Integer.MIN_VALUE // deep down in invalid range
            )
        .forEach(
            val ->
                assertEquals(
                    Double.NaN,
                    AfiBigInteger.ld(BigInteger.valueOf(val)))); // end forEach(val -> ...)

    // --- c. small smoke test values
    Stream.of(
            new int[] {1, 0}, // infimum of positive number
            new int[] {2, 1},
            new int[] {4, 2})
        .forEach(
            entry -> {
              final BigInteger val = BigInteger.valueOf(entry[0]);
              final double exp = entry[1];
              assertEquals(exp, AfiBigInteger.ld(val));
            }); // end forEach(entry -> ...)

    // --- d. random small values
    RNG.intsClosed(1, 0x1_0000, 1024)
        .forEach(
            val ->
                assertEquals(
                    Math.log(val) * AfiUtils.INVERSE_LN_2,
                    AfiBigInteger.ld(BigInteger.valueOf(val)))); // end forEach(val -> ...)

    // --- e. a lot of big numbers
    IntStream.range(0, 64)
        .forEach(
            i -> {
              // calculate start-value, which is small enough to be exactly converted to long
              BigInteger val = new BigInteger(63, RNG);
              double exp = Math.log(val.longValueExact()) * AfiUtils.INVERSE_LN_2;

              assertEquals(exp, AfiBigInteger.ld(val));

              // loop for bigger numbers
              for (int j = 0; j < 33_000; j++) {
                val = val.shiftLeft(1);
                exp += 1;
                assertEquals(exp, AfiBigInteger.ld(val), exp * 1e-14);
              } // end For (j...)
            }); // end forEach(i -> ...)
  } // end method */

  /** Test method for {@link AfiBigInteger#pow2(double)}. */
  @Test
  void test_pow2__double() {
    // Test strategy:
    // --- a. all small results up to 2^20
    // --- b. a lot of random results in long range
    // --- c. a lot of random results in double range

    // --- a. all small results up to 2^20
    for (int val = 1 << 20; val-- > 0; ) { // NOPMD assignment in operand
      // border between val and (val + 1)
      final double border = val + 0.5;

      // ld(border), i.e. exponent such that 2^ldBorder == border (neglecting rounding
      // errors)
      final double ldBorder = Math.log(border) * AfiUtils.INVERSE_LN_2;

      // calculate supremum exponent leading to val and infimum exponent leading to (val +
      // 1)
      // Note: Because of (possible) rounding errors here, a double tick up and down is used.
      final double expSupre = Math.nextDown(Math.nextDown(ldBorder));
      final double expInfim = Math.nextUp(Math.nextUp(ldBorder));

      assertEquals(BigInteger.valueOf(val), AfiBigInteger.pow2(expSupre));
      assertEquals(BigInteger.valueOf(val + 1), AfiBigInteger.pow2(expInfim));
    } // end For (val...)
    // end --- a.

    // --- b. a lot of random results in long range
    IntStream.range(0, 64) // loop over exponents in range [0, 63]
        .forEach(
            i -> {
              final double exponent = i * RNG.nextDouble(); // make exponent a bit fuzzy
              assertEquals(
                  BigInteger.valueOf(Math.round(Math.pow(2, exponent))),
                  AfiBigInteger.pow2(exponent));
            }); // end forEach(i -> ...)

    // --- c. a lot of random results in double range
    final var acceptableDelta = 1e-15; // DLS_DEAD_LOCAL_STORE
    // c.1 manually chosen input
    for (final var input : Set.of(0x408d78c472ec674eL)) {
      final var exponent = Double.longBitsToDouble(input);
      final var expected = Math.pow(2, exponent);

      final var actual = AfiBigInteger.pow2(exponent).doubleValue();

      final var delta = Math.abs(expected / actual - 1);
      assertTrue(
          delta < acceptableDelta,
          () -> String.format("%e = 0x%x", exponent, Double.doubleToLongBits(exponent)));
    } // end For (input...)

    // c.2 random chosen input
    IntStream.range(63, 1024) // loop over exponents in range [63, 1023]
        .forEach(
            i -> {
              final var exponent = i + RNG.nextDouble(); // make exponent a bit fuzzy
              final var expected = Math.pow(2, exponent);

              final var actual = AfiBigInteger.pow2(exponent).doubleValue();

              final var delta = Math.abs(expected / actual - 1);
              assertTrue(
                  delta < acceptableDelta,
                  () -> String.format("%e = 0x%x", exponent, Double.doubleToLongBits(exponent)));
            }); // end forEach(i -> ...)
  } // end method */

  /** Test method for {@link AfiBigInteger#pow2(int, int)}. */
  @Test
  void test_pow2__int_int() {
    // Test strategy:
    // --- a. Invalid values for numerator a
    // --- b. Invalid values for denominator b
    // --- c. a == 0
    // --- d. b == 1
    // --- e. a lot of random values with result in double range

    // --- a. Invalid values for numerator a
    for (final var a :
        List.of(
            Integer.MIN_VALUE, // infimum  of invalid values
            -1 // supremum of invalid values
            )) {
      for (final var b :
          List.of(
              Integer.MIN_VALUE, // infimum  of invalid values
              0, // supremum of invalid values
              1 // infimum  of  valid  values
              )) {
        final var thrown =
            assertThrows(IllegalArgumentException.class, () -> AfiBigInteger.pow2(a, b));
        assertEquals("parameter(s) too small", thrown.getMessage());
        assertNull(thrown.getCause());
      } // end For (b...)
    } // end For (a...)
    // end --- a.

    // --- b. Invalid values for denominator b
    // spotless:off
    Stream.of(
            Integer.MIN_VALUE, // infimum  of invalid values
            -1, // supremum of invalid values
            0 // infimum  of  valid  values
        )
        .forEach(
            a ->
                List.of(
                        Integer.MIN_VALUE, // infimum  of invalid values
                        0 // supremum of invalid values
                    )
                    .forEach(
                        b -> {
                          final Throwable thrown =
                              assertThrows(
                                  IllegalArgumentException.class, () -> AfiBigInteger.pow2(a, b));
                          assertEquals("parameter(s) too small", thrown.getMessage());
                          assertNull(thrown.getCause());
                        }) // end forEach(b -> ...)
        ); // end forEach(a -> ...)
    // spotless:on

    // --- c. a == 0
    RNG.intsClosed(1, 1024, 20)
        .forEach(b -> assertSame(ONE, AfiBigInteger.pow2(0, b))); // end forEach(b -> ...)

    // --- d. b == 1
    RNG.intsClosed(0, 1024, 20)
        .forEach(
            a ->
                assertEquals(
                    BigInteger.TWO.pow(a), AfiBigInteger.pow2(a, 1))); // end forEach(a -> ...)

    // --- e. a lot of random values with result in double range
    // spotless:off
    RNG.intsClosed(0, 768, 20)
        .forEach(
            a ->
                RNG.intsClosed(1, 768, 20)
                    .forEach(
                        b -> {
                          final BigInteger dut = AfiBigInteger.pow2(a, b);
                          final double exp = Math.pow(2.0, ((double) a) / b);

                          if (exp < 1e12) { // NOPMD literal in if statement
                            // ... expected value rather small
                            assertEquals(exp, dut.doubleValue() + 0.5, 0.5);
                          } else {
                            assertEquals(exp, dut.doubleValue(), exp * 1e-12);
                            // ... expected value rather large
                          } // end fi
                        }) // end forEach(b -> ...)
        ); // end forEach(a -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link AfiBigInteger#previousProbablePrime(java.math.BigInteger)}. */
  @Test
  void test_previousProbablePrime() {
    // Test strategy:
    // --- a. check method in case of twin primes
    // --- b. check method for arbitrary start values
    // --- c. small and negative start values

    // --- a. check method in case of twin primes
    // search for twin primes
    // Note: In order to speed up the process the bitLength used here is rather small.
    final int twinLength = 128;
    BigInteger lowerTwin;
    BigInteger upperTwin;
    do {
      lowerTwin = BigInteger.probablePrime(twinLength, RNG);
      upperTwin = lowerTwin.add(BigInteger.TWO);
    } while (!upperTwin.isProbablePrime(100));
    // ... we have twin primes
    // a.1 start value is upper twin
    assertEquals(lowerTwin, AfiBigInteger.previousProbablePrime(upperTwin));

    // a.2 start value is upperTwin - 1, i.e. an even number
    upperTwin = upperTwin.subtract(ONE);
    assertEquals(lowerTwin, AfiBigInteger.previousProbablePrime(upperTwin));

    // --- b. check method for arbitrary start values
    RNG.intsClosed(128, 768, 20)
        .forEach(
            bitLength -> {
              final BigInteger upper = new BigInteger(bitLength, RNG).setBit(bitLength - 1);
              final BigInteger lower = AfiBigInteger.previousProbablePrime(upper);
              assertEquals(bitLength, upper.bitLength()); // check bitLength of upper
              assertTrue(lower.compareTo(upper) < 0); // check for lower < upper
              assertTrue(lower.isProbablePrime(100)); // check that lower is prime
              assertTrue(
                  lower.nextProbablePrime().compareTo(upper) >= 0); // no prime in ]lower, upper]
            }); // end forEach(bitLength -> ...)

    // --- c. small and negative start values
    Stream.of(
            new int[] {11, 7},
            new int[] {7, 5},
            new int[] {5, 3},
            new int[] {3, -3},
            new int[] {0, -3},
            new int[] {-23, -29})
        .forEach(
            entry -> {
              final int upper = entry[0];
              final int lower = entry[1];
              assertEquals(
                  lower, AfiBigInteger.previousProbablePrime(BigInteger.valueOf(upper)).intValue());
            }); // end forEach(entry -> ...)
  } // end method */

  /** Test method for {@link AfiBigInteger#root(java.math.BigInteger, int)}. */
  @Test
  void test_root__int() {
    // Test strategy:
    // --- a. invalid number a
    // --- b. invalid root r
    // --- c. small, exact, smoke test values
    // --- d. arbitrary, random values for number a and root r
    // --- e. performance test

    // --- a. invalid number a
    for (final var a :
        Set.of(
            Integer.MIN_VALUE, // deep into range of invalid values
            -1, // close to supremum of invalid values
            0 // supremum of invalid values
            )) {
      for (final var r :
          List.of(
              Integer.MIN_VALUE, // infimum of invalid values
              -1, // close to supremum of invalid values
              0, // supremum of invalid values
              1 // infimum  of  valid  values
              )) {
        final var input = BigInteger.valueOf(a);
        final Throwable thrown =
            assertThrows(IllegalArgumentException.class, () -> AfiBigInteger.root(input, r));
        assertEquals("number a below 1 is not allowed", thrown.getMessage());
        assertNull(thrown.getCause());
      } // end For (r...)
    } // end For (a...)
    // end --- a.

    // --- b. invalid root r
    Stream.of(
            1 // infimum of valid values
            )
        .forEach(
            a -> {
              List.of(
                      Integer.MIN_VALUE, // infimum of invalid values
                      -1, // close to supremum of invalid values
                      0 // supremum of invalid values
                      )
                  .forEach(
                      r -> {
                        final var input = BigInteger.valueOf(a);
                        final Throwable thrown =
                            assertThrows(
                                IllegalArgumentException.class, () -> AfiBigInteger.root(input, r));
                        assertEquals("r = " + r + " is not allowed", thrown.getMessage());
                        assertNull(thrown.getCause());
                      }); // end forEach(r -> ...)
            }); // end forEach(a -> ...)

    // --- c. small, exact, smoke test values
    List.of(
            new int[] {1, 1, 1},
            new int[] {2, 2, 1},
            new int[] {3, 3, 1},
            new int[] {4, 4, 1}, // 1st
            new int[] {1, 1, 2},
            new int[] {4, 2, 2},
            new int[] {9, 3, 2},
            new int[] {16, 4, 2}, // 2nd
            new int[] {1, 1, 3},
            new int[] {8, 2, 3},
            new int[] {27, 3, 3},
            new int[] {64, 4, 3} // 3rd
            )
        .forEach(
            entry -> {
              final BigInteger val = BigInteger.valueOf(entry[0]);
              final int root = entry[2];
              final BigInteger exp = BigInteger.valueOf(entry[1]);

              assertEquals(exp, AfiBigInteger.root(val, root));
            }); // end forEach(i ->)

    // --- d. arbitrary, random values for number a and root r
    RNG.intsClosed(128, 4096, 10)
        .forEach(
            bitLength -> {
              final BigInteger val = new BigInteger(bitLength, RNG).setBit(bitLength - 1);
              IntStream.range(1, 20)
                  .forEach(
                      root -> {
                        final BigInteger result = AfiBigInteger.root(val, root);

                        // check that result isn't too big
                        assertTrue(result.pow(root).compareTo(val) <= 0);

                        // check that result is the smallest possible value
                        assertTrue(result.add(ONE).pow(root).compareTo(val) > 0);
                      }); // end forEach(root -> ...)
            }); // end forEach(bitLength -> ...)

    // --- e. performance test
    // Note 1: The tests above last approximately 0.3 seconds (which isn't that long).
    // Note 2: The tests hereafter are designed such that they (should) give a reliable indication
    //         for the performance of the method-under-test.
    // Note 3: Usually the runtime of the following test is large, but is not intended to find any
    //         implementation errors. Thus, (usually) the following tests are commented out.
    // Note 4: "maxBitLength" can be set to 513 for low test executions time.
    // Note 5: "maxBitLength" can be set to 16_384 for good indication of the performance for the
    //         method under test.
    final int minBitLength = 512;
    final int maxBitLength = 513;
    IntStream.range(minBitLength, maxBitLength)
        .forEach(
            bitLength -> {
              final BigInteger val = new BigInteger(bitLength, RNG).setBit(bitLength - 1);
              IntStream.range(1, 40)
                  .forEach(
                      root -> {
                        final BigInteger result = AfiBigInteger.root(val, root);

                        // check that result isn't too big
                        assertTrue(result.pow(root).compareTo(val) <= 0);

                        // check that result is the smallest possible value
                        assertTrue(result.add(ONE).pow(root).compareTo(val) > 0);
                      }); // end forEach(root -> ...)
            }); // end forEach(bitLength -> ...)
  } // end method */

  /** Test method for {@link AfiBigInteger#toHex(java.math.BigInteger)}. */
  @Test
  void test_toHex__BigInteger() {
    // Note: Underlying method is tested elsewhere, so we can be
    //       lazy here and concentrate on good test-coverage.

    // Test strategy:
    // --- a. some manually chosen smoke tests
    assertEquals("00", AfiBigInteger.toHex(BigInteger.ZERO));
  } // end method */

  /** Test method for {@link AfiBigInteger#toHex(java.math.BigInteger, int)}. */
  @Test
  void test_toHex__BigInteger_int() {
    // Note: Underlying method is tested elsewhere, so we can be
    //       lazy here and concentrate on good test-coverage.

    // Test strategy:
    // --- a. some manually chosen smoke tests
    assertEquals("0080", AfiBigInteger.toHex(BigInteger.valueOf(0x80), 2));
  } // end method */
} // end class
