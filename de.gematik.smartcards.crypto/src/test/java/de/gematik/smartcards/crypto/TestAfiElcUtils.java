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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests on {@link AfiElcUtils}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestAfiElcUtils {

  /**
   * {@link BouncyCastleProvider} for cryptographic functions.
   *
   * <p>According to <a
   * href="https://docs.oracle.com/en/java/javase/13/security/oracle-providers.html#GUID-091BF58C-82AB-4C9C-850F-1660824D5254">SUN</a>
   * The support for {@code brainpool}-curves has been deleted, see also <a
   * href="https://bugs.openjdk.java.net/browse/JDK-8251547">OpenJDK</a>.
   *
   * <p>Because some tests use the underlying Java Runtime Environment for cryptography with
   * elliptic curves here we use the {@link BouncyCastleProvider}.
   */
  private static final BouncyCastleProvider BC = new BouncyCastleProvider(); // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Test strategy:
    // --- a. check predefined domain parameters
    // a.1 Prime p
    // a.2 p congruent 3 mod 4
    // a.3 check a
    // a.4 check b
    // a.5: 4a³ + 27b² != 0
    // a.6 check generator G

    // --- a. check predefined domain parameters
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final BigInteger p = dp.getP();
          final BigInteger a = dp.getCurve().getA();
          final BigInteger b = dp.getCurve().getB();
          final BigInteger x = dp.getGenerator().getAffineX();
          final BigInteger y = dp.getGenerator().getAffineY();

          // a.1 Prime p
          assertTrue(p.isProbablePrime(120));

          // a.2 p congruent 3 mod 4
          // Note: See BSI TR-03111, v2.10, clause 3.2.2, note telling how to
          //       calculate the square root.
          assertEquals(AfiBigInteger.THREE, p.mod(AfiBigInteger.FOUR));

          // a.3 check a
          // Note: See BSI TR-03111, v2.10, eq. 2.5: a element Fp.
          assertTrue(a.compareTo(BigInteger.ZERO) > 0);
          assertTrue(a.compareTo(p) < 0);

          // a.4 check b
          // Note: See BSI TR-03111, v2.10, eq. 2.5: a element Fp.
          assertTrue(b.compareTo(BigInteger.ZERO) >= 0);
          assertTrue(b.compareTo(p) < 0);

          // a.5: 4a³ + 27b² != 0
          // Note: See BSI TR-03111, v2.10, eq. 2.5: E not singular
          assertNotEquals(
              BigInteger.ZERO,
              AfiBigInteger.FOUR
                  .multiply(a.modPow(AfiBigInteger.THREE, AfiBigInteger.THREE))
                  .add(BigInteger.valueOf(27).multiply(b.modPow(BigInteger.TWO, p)))
                  .mod(p));

          // a.6 check generator G
          // Note: Generator G has to be a point on the curve, i.e. fulfill
          //       BSI TR-03111, v2.10, eq. 2.5: y² == x³ + ax + b
          assertEquals(
              y.modPow(BigInteger.TWO, p),
              x.modPow(AfiBigInteger.THREE, p).add(a.multiply(x)).add(b).mod(p));
        }); // end forEach(dp -> ...)
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

  /** Test method for {@link AfiElcUtils#add(ECPoint, ECPoint, ECParameterSpec)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity"})
  @Test
  void test_add__EcPoint_EcPoint_EcParameterSpec() {
    // Assertion:
    // ... a. multiply(BigInteger, ECPoint, AfiElcParameterSpec)-method works as expected

    // Note 1: Here it is assumed that multiply(...)-method works as expected, and
    //         when testing that method it is assumed that add(...)-method works
    //         as expected. At first glance, this seems to be a circular argument.
    //         However: When testing multiply(...) that method heavily relies on
    //         the basic functionality of this class and compares to functionality
    //         from JRE, especially key-pairs, sign and verify. So if testing
    //         multiply(...)-method successfully we know that the basic functionality
    //         of add(...)-method is okay.
    // Note 2: The test strategy here is based on the idea of complete induction.
    //         We start with a small list and verify that add and multiply works.
    //         Then another element is added to the list, and we verify that all
    //         add and multiply for that new element work as well. If so then
    //         add(...)-method (and multiply(...)-method) seem to be okay at
    //         least for those summands and factors, and so on.
    // Note 3: The indices within the list are 0, 1, 2, 3, 4, ...
    // Note 4: The elements in the list are:
    //         0 -> PointOfInfinity,
    //         1 -> [1]P,
    //         2 -> [2]P, ...
    //         i -> [i]P, ...

    // Test strategy:
    // --- a. loop over all predefined curves
    // --- b. Point of Infinity: O + O
    // --- c. loop over a bunch of points, including O + P and P + O

    final ECPoint infinity = ECPoint.POINT_INFINITY;

    // --- a. loop over all predefined curves
    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      // --- b. Point of Infinity: O + O
      assertEquals(infinity, AfiElcUtils.add(infinity, infinity, dp));

      // --- c. loop over a bunch of points, including O + P and P + O
      final List<ECPoint> points = new ArrayList<>();
      points.add(dp.getGenerator());
      IntStream.range(0, 5)
          .forEach(
              i -> {
                try {
                  final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
                  keyPairGenerator.initialize(dp);
                  final KeyPair keyPair = keyPairGenerator.generateKeyPair();
                  final ECPoint point = ((ECPublicKey) keyPair.getPublic()).getW();
                  points.add(point);
                } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
                  fail(UNEXPECTED, e);
                } // end Catch (...)
              }); // end forEach(i -> ...)

      points.forEach(
          point -> {
            // check P + (-P)
            List.of(
                    BigInteger.ZERO.subtract(point.getAffineY()), // negative value
                    BigInteger.ZERO.subtract(point.getAffineY()).mod(dp.getP()) // positive value
                    )
                .forEach(
                    yp ->
                        assertEquals(
                            ECPoint.POINT_INFINITY,
                            AfiElcUtils.add(
                                point,
                                new ECPoint(point.getAffineX(), yp),
                                dp))); // end forEach(yp -> ...)

            final List<ECPoint> list = new ArrayList<>();
            list.add(infinity);
            list.add(point);

            final List<Integer> hugeFactor = new ArrayList<>();
            for (; ; ) {
              final int size = list.size();
              final ECPoint expected = list.get(size - 1);

              // check add
              for (int i = size; i-- > 0; ) { // NOPMD assignment in operand
                final int j = size - i - 1;
                final ECPoint p = list.get(i);
                final ECPoint q = list.get(j);
                assertEquals(expected, AfiElcUtils.add(p, q, dp));
              } // end For (i...)

              // check multiply
              hugeFactor.clear();
              final int product = size - 1;
              final int sqrt = (int) Math.sqrt(product);

              // check small factors
              for (int i = 1; i <= sqrt; i++) {
                final int quotient = product / i;
                final int remainder = product % i;

                if (0 == remainder) {
                  // ... i is factor of size
                  assertEquals(
                      expected,
                      AfiElcUtils.multiply(BigInteger.valueOf(i), list.get(quotient), dp));

                  if (sqrt != quotient) {
                    hugeFactor.addFirst(quotient);
                  } // end fi
                } // end fi
              } // end For (i...)

              hugeFactor.forEach(
                  i -> {
                    final int quotient = product / i;
                    assertEquals(
                        expected,
                        AfiElcUtils.multiply(BigInteger.valueOf(i), list.get(quotient), dp));
                  }); // end forEach(i -> ...)

              if (size > 100) { // NOPMD literal in if statement
                // ... list is big enough
                break;
              } // end fi
              // ... list not big enough
              //     => add another element
              list.add(AfiElcUtils.multiply(BigInteger.valueOf(size), point, dp));
            } // end For (...)
          }); // end forEach(point -> ...)
    } // end For (dp...)
  } // end method */

  /** Test method for {@link AfiElcUtils#ecka(ECPrivateKey, ECPublicKey)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_ecka__EcPrivateKey_EcPublicKey() {
    // Test strategy:
    // --- a. bunch of random key-pairs
    // --- b. ERROR: not same domain parameter
    // --- c. ERROR: public point not on elliptic curve
    // --- d. ERROR: public point of infinity
    // --- e. ERROR: common secret represents point of infinity

    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      try {
        // --- a. bunch of random key-pairs
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
        keyPairGenerator.initialize(dp);

        final KeyPair keyPairA = keyPairGenerator.generateKeyPair();
        final ECPrivateKey prkA = (ECPrivateKey) keyPairA.getPrivate();
        final ECPublicKey pukA = (ECPublicKey) keyPairA.getPublic();

        final KeyPair keyPairB = keyPairGenerator.generateKeyPair();
        final ECPrivateKey prkB = (ECPrivateKey) keyPairB.getPrivate();
        final ECPublicKey pukB = (ECPublicKey) keyPairB.getPublic();

        // Note: Strategy is to verify that prkA * pukB == prkB * pukA
        assertEquals(AfiElcUtils.ecka(prkA, pukB), AfiElcUtils.ecka(prkB, pukA));

        // --- b. ERROR: not same domain parameter
        AfiElcParameterSpec.PREDEFINED.forEach(
            dpOther -> {
              if (!dp.equals(dpOther)) {
                // ... domain parameter differ
                try {
                  keyPairGenerator.initialize(dpOther);

                  final KeyPair keyPairC = keyPairGenerator.generateKeyPair();
                  final ECPrivateKey prkC = (ECPrivateKey) keyPairC.getPrivate();
                  final ECPublicKey pukC = (ECPublicKey) keyPairC.getPublic();

                  Throwable throwable =
                      assertThrows(
                          IllegalArgumentException.class, () -> AfiElcUtils.ecka(prkA, pukC));
                  assertEquals("different domain parameter", throwable.getMessage());
                  assertNull(throwable.getCause());

                  throwable =
                      assertThrows(
                          IllegalArgumentException.class, () -> AfiElcUtils.ecka(prkC, pukA));
                  assertEquals("different domain parameter", throwable.getMessage());
                  assertNull(throwable.getCause());
                } catch (InvalidAlgorithmParameterException e) {
                  fail(UNEXPECTED, e);
                } // end Catch (...)
              } // end fi
            }); // end forEach(dp -> ...)

        final KeyFactory keyFactory = KeyFactory.getInstance("EC");

        // --- c. ERROR: public point not on elliptic curve
        {
          final var puk =
              (ECPublicKey)
                  keyFactory.generatePublic(
                      new ECPublicKeySpec(new ECPoint(BigInteger.ZERO, BigInteger.ZERO), dp));
          final Throwable throwable =
              assertThrows(IllegalArgumentException.class, () -> AfiElcUtils.ecka(prkA, puk));
          assertEquals("invalid public key", throwable.getMessage());
          assertNull(throwable.getCause());
        } // end --- c.
      } catch (InvalidAlgorithmParameterException
          | InvalidKeySpecException
          | NoSuchAlgorithmException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (dp...)

    // --- e. ERROR: common secret represents point of infinity
    {
      final AfiElcParameterSpec dp =
          new AfiElcParameterSpec(
              BigInteger.valueOf(7), // p
              BigInteger.valueOf(5), // a
              BigInteger.valueOf(1), // b
              BigInteger.valueOf(6), // Gx
              BigInteger.valueOf(3), // Gy
              BigInteger.valueOf(12), // n
              1 // cofactor
              );

      Stream.of(2, 3, 4, 6)
          .map(BigInteger::valueOf)
          .forEach(
              d -> {
                final BigInteger dB = dp.getOrder().divide(d);
                assertEquals(dp.getOrder(), d.multiply(dB));

                final ECPrivateKeySpec prkA = new ECPrivateKeySpec(d, dp);
                final ECPublicKeySpec pukB =
                    new ECPublicKeySpec(AfiElcUtils.multiply(dB, dp.getGenerator(), dp), dp);

                final Throwable throwable =
                    assertThrows(
                        IllegalArgumentException.class, () -> AfiElcUtils.ecka(prkA, pukB));
                assertEquals("ERROR", throwable.getMessage());
                assertNull(throwable.getCause());
              });
    } // end --- e.
  } // end method */

  /** Test method for {@link AfiElcUtils#fe2os(BigInteger, ECParameterSpec)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_fe2os__BigInteger_EcParameterSpec() {
    // Test strategy:
    // --- a. check a bunch of BigInteger
    // --- b. ERROR: fieldElement out of range

    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      final int intL = dp.getL();
      final BigInteger primeP = dp.getP();
      final int lengthP = primeP.bitLength();

      // --- a. check a bunch of BigInteger
      // a.1 check border elements
      // a.2 check random integer

      // a.1 check border elements
      for (final var fieldElement :
          List.of(
              BigInteger.ZERO, // infimum of allowed values
              BigInteger.ONE, // nex to infimum
              primeP.subtract(BigInteger.TWO), // next to supremum
              primeP.subtract(BigInteger.ONE) // supremum of allowed values
              )) {
        assertEquals(
            Hex.toHexDigits(AfiBigInteger.i2os(fieldElement, intL)),
            Hex.toHexDigits(AfiElcUtils.fe2os(fieldElement, dp)));
      } // end For (fieldElement...)

      // a.2 check random integer
      IntStream.range(0, 100)
          .forEach(
              i -> {
                final BigInteger fieldElement = new BigInteger(lengthP - 1, RNG);
                assertEquals(
                    Hex.toHexDigits(AfiBigInteger.i2os(fieldElement, intL)),
                    Hex.toHexDigits(AfiElcUtils.fe2os(fieldElement, dp)));
              }); // end forEach(i -> ...)

      // --- b. ERROR: fieldElement out of range
      List.of(
              BigInteger.valueOf(-1), // supremum of values too small
              primeP // infimum  of values too large
              )
          .forEach(
              fieldElement -> {
                final Throwable throwable =
                    assertThrows(
                        IllegalArgumentException.class, () -> AfiElcUtils.fe2os(fieldElement, dp));
                assertEquals("0 > fieldElement >= p", throwable.getMessage());
                assertNull(throwable.getCause());
              }); // end forEach(fieldElement -> ...)
    } // end For (dp...)
  } // end method */

  /** Test method for {@link AfiElcUtils#isPointOnEllipticCurve(ECPoint, ECParameterSpec)}. */
  @Test
  void test_isPointOnEllipticCurve__EcPoint_EcParameterSpec() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. point of infinity
    // --- b. point on curve
    // --- c. point not on curve

    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      // --- a. point of infinity
      assertTrue(AfiElcUtils.isPointOnEllipticCurve(ECPoint.POINT_INFINITY, dp));

      // --- b. point on curve
      assertTrue(AfiElcUtils.isPointOnEllipticCurve(dp.getGenerator(), dp));
      final ECPoint generator = dp.getGenerator();
      final ECPoint opposite =
          new ECPoint(generator.getAffineX(), dp.getP().subtract(generator.getAffineY())); // NOPMD
      assertTrue(AfiElcUtils.isPointOnEllipticCurve(opposite, dp));

      // --- c. point not on curve
      assertFalse(
          AfiElcUtils.isPointOnEllipticCurve(
              new ECPoint(BigInteger.ZERO, BigInteger.ZERO), dp)); // NOPMD new in loop
    } // end For (dp ...)
  } // end method */

  /** Test method for {@link AfiElcUtils#multiply(BigInteger, ECPoint, ECParameterSpec)}. */
  @Test
  @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
  // false positive
  void test_multiply__BigInteger_EcPoint_EcParameterSpec() {
    // Assertions:
    // ... a. add(ECPoint, ECPoint, AfiElcDomainParameter)-method works as expected

    // Test strategy:
    // --- a. loop over all predefined curves
    // --- b. Point of Infinity
    // --- c. manual chosen corner cases with generator G
    // --- d. a bunch of random factors and generator G
    // --- e. ERROR: invalid factor

    // --- a. loop over all predefined curves
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final BigInteger n = dp.getOrder();

          // --- b. Point of Infinity
          List.of(
                  BigInteger.ZERO, // supremum of factor "too small"
                  BigInteger.ONE, // infimum of valid factors
                  BigInteger.TWO, // next to infimum
                  n, // supremum of valid factors
                  n.add(BigInteger.ONE), // infimum of factors "too large"
                  n.add(BigInteger.TWO) // next to infimum of factors "too large"
                  )
              .forEach(
                  factor ->
                      assertEquals(
                          ECPoint.POINT_INFINITY,
                          AfiElcUtils.multiply(
                              factor, ECPoint.POINT_INFINITY, dp))); // end forEach(factor -> ...)

          // --- c. manual chosen corner cases with generator G
          final ECPoint generator = dp.getGenerator();
          List.of(
                  BigInteger.ONE, // infimum of valid factors
                  BigInteger.TWO, // next to infimum
                  n.subtract(BigInteger.TWO), // next to supremum of relevant factors
                  n.subtract(BigInteger.ONE) // supremum of relevant factors
                  )
              .forEach(
                  factor -> {
                    try {
                      final KeyFactory keyFactory = KeyFactory.getInstance("EC");
                      final ECPoint w = AfiElcUtils.multiply(factor, generator, dp);
                      final ECPrivateKey prk =
                          (ECPrivateKey)
                              keyFactory.generatePrivate(new ECPrivateKeySpec(factor, dp));
                      final ECPublicKey puk =
                          (ECPublicKey) keyFactory.generatePublic(new ECPublicKeySpec(w, dp));

                      // Note: Method-under-test works as expected if PrK and PuK form a
                      //       valid key pair. This is tested by using that key-pair.

                      final String algorithm = "SHA256withECDSA";
                      final byte[] message = RNG.nextBytes(1, 100);

                      // --- create signature
                      final Signature signer = Signature.getInstance(algorithm, BC);
                      signer.initSign(prk);
                      signer.update(message);
                      final byte[] signature = signer.sign();

                      // --- verify signature
                      signer.initVerify(puk);
                      signer.update(message);
                      assertTrue(signer.verify(signature));
                    } catch (InvalidKeyException
                        | InvalidKeySpecException
                        | NoSuchAlgorithmException
                        | SignatureException e) {
                      fail(UNEXPECTED, e);
                    } // end Catch (...)
                  }); // end forEach(factor -> ...)
          assertEquals(
              ECPoint.POINT_INFINITY, AfiElcUtils.multiply(dp.getOrder(), dp.getGenerator(), dp));

          // --- d. a bunch of random factors and generator G
          IntStream.range(0, 20)
              .forEach(
                  i -> {
                    try {
                      final KeyPairGenerator keyPairGenerator =
                          KeyPairGenerator.getInstance("EC", BC);
                      keyPairGenerator.initialize(dp);
                      final KeyPair keyPair = keyPairGenerator.generateKeyPair();
                      final BigInteger factor = ((ECPrivateKey) keyPair.getPrivate()).getS();
                      final ECPoint point = ((ECPublicKey) keyPair.getPublic()).getW();
                      assertEquals(point, AfiElcUtils.multiply(factor, generator, dp));
                    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
                      fail(UNEXPECTED, e);
                    } // end Catch (...)
                  }); // end forEach(i -> ...)

          // --- e. ERROR: invalid factor
          List.of(
                  BigInteger.ZERO, // supremum of factor "too small"
                  n.add(BigInteger.ONE) // infimum of factors "too large"
                  )
              .forEach(
                  factor -> {
                    final Throwable throwable =
                        assertThrows(
                            IllegalArgumentException.class,
                            () -> AfiElcUtils.multiply(factor, generator, dp));
                    assertEquals("factor k not in range [1, n]", throwable.getMessage());
                    assertNull(throwable.getCause());
                  }); // end forEach(factor -> ...)
        }); // end forEach(dp -> ...)
  } // end method */

  /** Test method for {@link AfiElcUtils#os2fe(byte[], ECParameterSpec)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_os2fe__byteA_AfiElcParameterSpec() {
    // Test strategy:
    // --- a. empty octet string
    // --- b. bunch of random numbers

    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      // --- a. empty octet string
      assertEquals(BigInteger.ZERO, AfiElcUtils.os2fe(AfiUtils.EMPTY_OS, dp));

      // --- b. bunch of random numbers
      RNG.intsClosed(1, dp.getL() + 2, 100)
          .forEach(
              size -> {
                final byte[] os = RNG.nextBytes(size);
                assertEquals(new BigInteger(1, os).mod(dp.getP()), AfiElcUtils.os2fe(os, dp));
              }); // end forEach(size -> ...)
    } // end For (dp...)
  } // end method */

  /** Test method for {@link AfiElcUtils#os2fe(byte[], BigInteger)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_os2fe__byteA_BigInteger() {
    // Test strategy:
    // --- a. empty octet string
    // --- b. bunch of random numbers
    // --- c. ERROR: prime p too small

    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      final var primeP = dp.getP();

      // --- a. empty octet string
      assertEquals(BigInteger.ZERO, AfiElcUtils.os2fe(AfiUtils.EMPTY_OS, primeP));

      // --- b. bunch of random numbers
      RNG.intsClosed(1, primeP.bitLength() + 32, 100)
          .forEach(
              size -> {
                final byte[] os = RNG.nextBytes(size);
                assertEquals(new BigInteger(1, os).mod(primeP), AfiElcUtils.os2fe(os, primeP));
              }); // end forEach(size -> ...)
    } // end For (dp...)

    // --- c. ERROR: prime p too small
    {
      final var p = BigInteger.valueOf(-1);
      RNG.intsClosed(0, 10, 5)
          .forEach(
              size -> {
                final var os = RNG.nextBytes(size);
                final Throwable throwable =
                    assertThrows(IllegalArgumentException.class, () -> AfiElcUtils.os2fe(os, p));
                assertEquals("p not positive", throwable.getMessage());
                assertNull(throwable.getCause());
              }); // end forEach(size -> ...)
    } // end --- c.
  } // end method */

  /** Test method for {@link AfiElcUtils#os2p(byte[], AfiElcParameterSpec)}. */
  @Test
  void test_os2p__byteA_AfiElcParameterSpec() {
    // Assertions:
    // ... a. os2p(byte[], BigInteger)-method works as expected
    // ... b. isPointOnEllipticCurve(...)-method works as expected
    // ... c. p2osCompressed(ECPoint, AfiElcParameterSpec)-method works as expected
    // ... d. p2osUncompressed(ECPoint, AfiElcParameterSpec)-method works as expected

    // Test strategy:
    // --- a. manually chosen input
    // --- b. point of infinity
    // --- c. compressed encoding
    // --- d. uncompressed encoding
    // --- e. ERROR: point not on curve
    // --- f. ERROR: invalid compressed encoding
    // --- g. ERROR: invalid first octet
    // --- h. compressed encoding with yp=0
    // --- i. ERROR: compressed encoding for small p, alpha not square
    // --- j. ERROR: compressed encoding p not congruent 3 mod 4

    // --- a. manually chosen input
    // happy cases from ANSI X9.62
    Set.of(
            // base point G of ansix9p256r1
            List.of(
                "03", // msByte
                "6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296", // x
                "4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5", // y
                AfiElcParameterSpec.ansix9p256r1),
            // base point G of ansix9p384r1
            List.of(
                "03", // msByte
                "aa87ca22be8b05378eb1c71ef320ad746e1d3b628ba79b9859f741e082542a38550"
                    + "2f25dbf55296c3a545e3872760ab7", // x
                "3617de4a96262c6f5d9e98bf9292dc29f8f41dbd289a147ce9da3113b5f0b8c00a6"
                    + "0b1ce1d7e819d7a431d7c90ea0e5f", // y
                AfiElcParameterSpec.ansix9p384r1),
            // base point G of ansix9p512r1
            List.of(
                "02", // msByte
                "00c6858e06b70404e9cd9e3ecb662395b4429c648139053fb521f828af606b4d3db"
                    + "aa14b5e77efe75928fe1dc127a2ffa8de3348b3c1856a429bf97e7e31c2e5bd66", // x
                "011839296a789a3bc0045c8a5fb42c7d1bd998f54449579b446817afbd17273e662"
                    + "c97ee72995ef42640c550b9013fad0761353c7086a272c24088be94769fd16650", // y
                AfiElcParameterSpec.ansix9p521r1))
        .forEach(
            list -> {
              int index = -1;
              final String msByte = (String) list.get(++index);
              final String biX = (String) list.get(++index);
              final String biY = (String) list.get(++index);
              final AfiElcParameterSpec dp = (AfiElcParameterSpec) list.get(++index);

              assertEquals(
                  new ECPoint(new BigInteger(biX, 16), new BigInteger(biY, 16)),
                  AfiElcUtils.os2p(Hex.toByteArray(msByte + biX), dp));
            }); // end forEach(list -> ...)

    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          // --- b. point of infinity
          assertEquals(ECPoint.POINT_INFINITY, AfiElcUtils.os2p(new byte[1], dp));

          // create a bunch of points
          try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
            keyPairGenerator.initialize(dp);
            IntStream.range(0, 1000)
                .forEach(
                    i -> {
                      final KeyPair keyPair = keyPairGenerator.generateKeyPair();
                      final ECPoint point = ((ECPublicKey) keyPair.getPublic()).getW();

                      // --- c. compressed encoding
                      final byte[] compressed = AfiElcUtils.p2osCompressed(point, dp);
                      assertEquals(point, AfiElcUtils.os2p(compressed, dp));

                      // --- d. uncompressed encoding
                      final byte[] uncompressed = AfiElcUtils.p2osUncompressed(point, dp);
                      assertEquals(point, AfiElcUtils.os2p(uncompressed, dp));

                      // --- e. ERROR: point not on curve
                      uncompressed[3] =
                          (byte) (uncompressed[3] + 1); // modify uncompressed encoding
                      final Throwable throwable =
                          assertThrows(
                              IllegalArgumentException.class,
                              () -> AfiElcUtils.os2p(uncompressed, dp));
                      assertEquals(
                          "P is not a point on the elliptic curve", throwable.getMessage());
                      assertNull(throwable.getCause());
                    }); // end forEach(i -> ...)
          } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            fail(UNEXPECTED, e);
          } // end Catch (...)

          // --- f. ERROR: invalid compressed encoding
          {
            // prepare input
            final byte[][] input = new byte[100][];
            for (int i = input.length; i-- > 0; ) { // NOPMD assignment in operands
              input[i] = RNG.nextBytes(2, 100);
              input[i][0] = (byte) RNG.nextIntClosed(2, 3);
            } // end For (i...)

            // at least one input SHALL cause an exception (maybe not all of them)
            try {
              for (int i = input.length; i-- > 0; ) { // NOPMD assignment in operand
                final byte[] compressed = input[i];
                AfiElcUtils.os2p(compressed, dp);
              } // end For (i...)
              fail("exception expected");
            } catch (IllegalArgumentException e) {
              assertEquals("alpha is not a square in Fp", e.getMessage());
              assertNull(e.getCause());
            } // end Catch (...)
          } // end --- f.

          // --- g. ERROR: invalid first octet
          IntStream.range(0, 256)
              .forEach(
                  msByte -> {
                    if ((0 != msByte) // point of infinity
                        && (2 != msByte) // compressed encoding
                        && (3 != msByte) // compressed encoding
                        && (4 != msByte) // uncompressed encoding
                    ) {
                      // ... invalid first octet valid encoding
                      final byte[] os = RNG.nextBytes(1, 100);
                      os[0] = (byte) msByte;

                      final Throwable throwable =
                          assertThrows(
                              IllegalArgumentException.class,
                              () -> AfiElcUtils.os2p(os, dp),
                              () -> Hex.toHexDigits(os));
                      assertEquals("unimplemented encoding", throwable.getMessage());
                      assertNull(throwable.getCause());
                    } // end fi
                  }); // end forEach(msByte -> ...)
        }); // end forEach(dp -> ...)

    // --- h. compressed encoding with yp=0
    {
      final AfiElcParameterSpec dut =
          new AfiElcParameterSpec(
              BigInteger.valueOf(43), // prime p
              BigInteger.valueOf(18), // a
              BigInteger.valueOf(0), // b
              BigInteger.valueOf(2), // generator x-coordinate
              BigInteger.valueOf(1), // generator y-coordinate
              BigInteger.valueOf(11), // order n
              4 // co-factor h
              );
      final ECPoint expected = new ECPoint(AfiBigInteger.FIVE, BigInteger.ZERO);
      assertEquals(expected, AfiElcUtils.os2p(Hex.toByteArray("0205"), dut));
      assertEquals(expected, AfiElcUtils.os2p(Hex.toByteArray("0305"), dut));

      // --- i. ERROR: compressed encoding for small p, alpha not square
      final var inputI = Hex.toByteArray("0304");
      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> AfiElcUtils.os2p(inputI, dut));
      assertEquals("alpha is not a square in Fp", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- h, i.

    // --- j. compressed encoding p not congruent 3 mod 4
    {
      final AfiElcParameterSpec dut =
          new AfiElcParameterSpec(
              BigInteger.valueOf(41), // prime p
              BigInteger.valueOf(18), // a
              BigInteger.valueOf(0), // b
              BigInteger.valueOf(3), // generator x-coordinate
              BigInteger.valueOf(9), // generator y-coordinate
              BigInteger.valueOf(4), // order n
              8); // co-factor h
      assertFalse(dut.isPcongruent3mod4());
      final var input = Hex.toByteArray("0203");
      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> AfiElcUtils.os2p(input, dut));
      assertEquals(
          "cannot calculate square root, use uncompressed encoding", throwable.getMessage());
      assertNull(throwable.getCause());
    }
  } // end method */

  /** Test method for {@link AfiElcUtils#os2p(byte[], BigInteger)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_os2p__byteA_BigInteger() {
    // Assertions:
    // ... a. as2fe(...)-method works as expected

    // Test strategy:
    // --- a. point of infinity
    // --- b. uncompressed valid input
    // --- c. ERROR: uncompressed but even number of octets
    // --- d. ERROR: invalid point of infinity
    // --- e. ERROR: invalid first octet

    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      final var primeP = dp.getP();
      // --- a. point of infinity
      assertEquals(ECPoint.POINT_INFINITY, AfiElcUtils.os2p(new byte[1], primeP));

      // --- b. uncompressed valid input
      RNG.intsClosed(0, 100, 50)
          .forEach(
              size -> {
                final byte[] osA = RNG.nextBytes(size);
                final byte[] osB = RNG.nextBytes(size);
                assertEquals(
                    new ECPoint(AfiElcUtils.os2fe(osA, primeP), AfiElcUtils.os2fe(osB, primeP)),
                    AfiElcUtils.os2p(
                        Hex.toByteArray("04" + Hex.toHexDigits(osA) + Hex.toHexDigits(osB)),
                        primeP));
              }); // end forEach(size -> ...)

      // --- c. ERROR: uncompressed but even number of octets
      {
        final var msByte = new byte[] {4};
        RNG.intsClosed(0, 100, 50)
            .forEach(
                size -> {
                  final byte[] osA = RNG.nextBytes(size);
                  final byte[] osB = RNG.nextBytes(size + 1);
                  final byte[] input = AfiUtils.concatenate(msByte, osA, osB);
                  final Throwable throwable =
                      assertThrows(
                          IllegalArgumentException.class, () -> AfiElcUtils.os2p(input, primeP));
                  assertEquals("length of octet string is even", throwable.getMessage());
                  assertNull(throwable.getCause());
                }); // end forEach(size -> ...)
      } // end --- c.

      // --- d. ERROR: invalid point of infinity
      RNG.intsClosed(2, 100, 50)
          .forEach(
              size -> {
                final byte[] os = RNG.nextBytes(size);
                os[0] = 0;
                final Throwable throwable =
                    assertThrows(
                        IllegalArgumentException.class, () -> AfiElcUtils.os2p(os, primeP));
                assertEquals("unimplemented encoding", throwable.getMessage());
                assertNull(throwable.getCause());
              }); // end forEach(size -> ...)

      // --- e. ERROR: invalid first octet
      IntStream.range(0, 256)
          .forEach(
              msByte -> {
                if (4 != msByte) { // NOPMD literal in if statement
                  // ... invalid first octet valid encoding
                  final byte[] os = RNG.nextBytes(2, 100);
                  os[0] = (byte) msByte;

                  final Throwable throwable =
                      assertThrows(
                          IllegalArgumentException.class, () -> AfiElcUtils.os2p(os, primeP));
                  assertEquals("unimplemented encoding", throwable.getMessage());
                  assertNull(throwable.getCause());
                } // end fi
              }); // end forEach(msByte -> ...)
    } // end For (dp...)
  } // end method */

  /** Test method for {@link AfiElcUtils#p2osCompressed(ECPoint, ECParameterSpec)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_p2osCompressed__EcPoint_AfiElcParameterSpec() {
    // Assertion:
    // ... a. fe2os(...)-method works as expected

    // Test strategy:
    // --- a. point of infinity
    // --- b. corner cases
    // --- c. random points, not necessarily on elliptic curve
    // --- d. ERROR: fieldElements out of range

    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      final int intL = dp.getL();
      final BigInteger primeP = dp.getP();
      final int lengthP = primeP.bitLength();

      // --- a. point of infinity
      assertEquals("00", Hex.toHexDigits(AfiElcUtils.p2osCompressed(ECPoint.POINT_INFINITY, dp)));

      // --- b. corner cases
      // b.1 point (0, p-1)
      // b.2 point (p-1, 0)
      {
        // b.1 point (0, p-1)
        final BigInteger coA = BigInteger.ZERO;
        final BigInteger coB = primeP.subtract(BigInteger.ONE);
        assertEquals(
            "02" + Hex.toHexDigits(AfiBigInteger.i2os(coA, intL)),
            Hex.toHexDigits(AfiElcUtils.p2osCompressed(new ECPoint(coA, coB), dp)));

        // b.2 point (p-1, 0)
        assertEquals(
            "02" + Hex.toHexDigits(AfiBigInteger.i2os(coB, intL)),
            Hex.toHexDigits(AfiElcUtils.p2osCompressed(new ECPoint(coB, coA), dp)));
      }

      // --- c. random points, not necessarily on elliptic curve
      IntStream.range(0, 100)
          .forEach(
              i -> {
                final BigInteger coA = new BigInteger(lengthP - 1, RNG);
                final BigInteger coB = new BigInteger(lengthP - 1, RNG);
                assertEquals(
                    ((0 == (coB.intValue() & 1)) ? "02" : "03")
                        + Hex.toHexDigits(AfiBigInteger.i2os(coA, intL)),
                    Hex.toHexDigits(AfiElcUtils.p2osCompressed(new ECPoint(coA, coB), dp)));
              }); // end forEach(i -> ...)

      // --- d. ERROR: fieldElements out of range
      final List<BigInteger> fieldElements =
          List.of(
              BigInteger.valueOf(-1), // just too small
              BigInteger.ONE, // value ok
              primeP // just too large
              );
      fieldElements.forEach(
          x ->
              fieldElements.forEach(
                  (y -> {
                    final ECPoint point = new ECPoint(x, y);
                    if (BigInteger.ONE.equals(x)) {
                      // ... both values in range
                      assertTrue(
                          Hex.toHexDigits(AfiElcUtils.p2osCompressed(point, dp)).startsWith("03"));
                    } else {
                      // ... at least one coordinate out of range
                      final Throwable throwable =
                          assertThrows(
                              IllegalArgumentException.class,
                              () -> AfiElcUtils.p2osCompressed(point, dp));
                      assertEquals("0 > fieldElement >= p", throwable.getMessage());
                      assertNull(throwable.getCause());
                    } // end else
                  }))); // end forEach(x,y -> ...)
    } // end For (dp...)
  } // end method */

  /** Test method for {@link AfiElcUtils#p2osUncompressed(ECPoint, AfiElcParameterSpec)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_p2osUncompressed__EcPoint_AfiElcParameterSpec() {
    // Assertion:
    // ... a. fe2os(...)-method works as expected

    // Test strategy:
    // --- a. point of infinity
    // --- b. corner cases
    // --- c. random points, not necessarily on elliptic curve
    // --- d. ERROR: fieldElements out of range

    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      final int intL = dp.getL();
      final BigInteger primeP = dp.getP();
      final int lengthP = primeP.bitLength();

      // --- a. point of infinity
      assertEquals("00", Hex.toHexDigits(AfiElcUtils.p2osUncompressed(ECPoint.POINT_INFINITY, dp)));

      // --- b. corner cases
      // b.1 point (0, p-1)
      // b.2 point (p-1, 0)
      {
        // b.1 point (0, p-1)
        final BigInteger coA = BigInteger.ZERO;
        final BigInteger coB = primeP.subtract(BigInteger.ONE);
        assertEquals(
            "04"
                + Hex.toHexDigits(AfiBigInteger.i2os(coA, intL))
                + Hex.toHexDigits(AfiBigInteger.i2os(coB, intL)),
            Hex.toHexDigits(AfiElcUtils.p2osUncompressed(new ECPoint(coA, coB), dp)));

        // b.2 point (p-1, 0)
        assertEquals(
            "04"
                + Hex.toHexDigits(AfiBigInteger.i2os(coB, intL))
                + Hex.toHexDigits(AfiBigInteger.i2os(coA, intL)),
            Hex.toHexDigits(AfiElcUtils.p2osUncompressed(new ECPoint(coB, coA), dp)));
      }

      // --- c. random points, not necessarily on elliptic curve
      IntStream.range(0, 100)
          .forEach(
              i -> {
                final BigInteger coA = new BigInteger(lengthP - 1, RNG);
                final BigInteger coB = new BigInteger(lengthP - 1, RNG);
                assertEquals(
                    "04"
                        + Hex.toHexDigits(AfiBigInteger.i2os(coA, intL))
                        + Hex.toHexDigits(AfiBigInteger.i2os(coB, intL)),
                    Hex.toHexDigits(AfiElcUtils.p2osUncompressed(new ECPoint(coA, coB), dp)));
              }); // end forEach(i -> ...)

      // --- d. ERROR: fieldElements out of range
      final List<BigInteger> fieldElements =
          List.of(
              BigInteger.valueOf(-1), // just too small
              BigInteger.ONE, // value ok
              primeP // just too large
              );
      fieldElements.forEach(
          xp ->
              fieldElements.forEach(
                  (yp -> {
                    final ECPoint point = new ECPoint(xp, yp);
                    if (BigInteger.ONE.equals(xp) && BigInteger.ONE.equals(yp)) {
                      // ... both values in range
                      assertTrue(
                          Hex.toHexDigits(AfiElcUtils.p2osUncompressed(point, dp))
                              .startsWith("04"));
                    } else {
                      // ... at least one coordinate out of range
                      final Throwable throwable =
                          assertThrows(
                              IllegalArgumentException.class,
                              () -> AfiElcUtils.p2osUncompressed(point, dp));
                      assertEquals("0 > fieldElement >= p", throwable.getMessage());
                      assertNull(throwable.getCause());
                    } // end else
                  }))); // end forEach(x,y -> ...)
    } // end For (dp...)
  } // end method */

  /** Test method for {@link AfiElcUtils#sharedSecret(ECPrivateKey, ECPublicKey)}. */
  @Test
  @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
  // false positive
  void test_sharedSecret__EcPrivateKey_EcPublicKey() {
    // Assertions:
    // ... a. ecka(...)-method works as expected
    // ... b. fe2os(...)-method works as expected

    // Note: Simple method does not need extensive testing, so we can be lazy here

    // Test strategy:
    // --- a. bunch of random key-pairs
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
            keyPairGenerator.initialize(dp);

            final KeyPair keyPairA = keyPairGenerator.generateKeyPair();
            final ECPrivateKey prkA = (ECPrivateKey) keyPairA.getPrivate();

            final KeyPair keyPairB = keyPairGenerator.generateKeyPair();
            final ECPublicKey pukB = (ECPublicKey) keyPairB.getPublic();

            final ECPoint point = AfiElcUtils.ecka(prkA, pukB);
            assertArrayEquals(
                AfiElcUtils.fe2os(point.getAffineX(), dp), AfiElcUtils.sharedSecret(prkA, pukB));
          } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            fail(UNEXPECTED, e);
          } // end Catch (...)
        }); // end forEach(dp -> ...)
  } // end method */
} // end class
