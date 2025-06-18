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

import static de.gematik.smartcards.crypto.RsaPrivateCrtKeyImpl.CERTAINTY;
import static de.gematik.smartcards.crypto.RsaPrivateCrtKeyImpl.INFIMUM_E;
import static de.gematik.smartcards.crypto.RsaPrivateCrtKeyImpl.INFIMUM_SECURE_MODULUS;
import static de.gematik.smartcards.crypto.RsaPrivateCrtKeyImpl.SUPREMUM_LD_E;
import static de.gematik.smartcards.crypto.RsaPrivateKeyImpl.INFIMUM_SECURE_MODULUS_LENGTH;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerNull;
import de.gematik.smartcards.tlv.DerOctetString;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link RsaPrivateKeyImpl}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "DLS_DEAD_LOCAL_STORE"
//         Spotbugs message: this instruction assigns a value to a local variable,
//             but the value is not read or used in any subsequent instruction.
//         Rational: That finding is a false positive because the value is used
//             in the next two lines of code.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "DLS_DEAD_LOCAL_STORE", // see note 1
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestRsaPrivateCrtKeyImpl extends RsaKeyRepository {

  /** Logger. */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(TestRsaPrivateCrtKeyImpl.class); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check parameters of PRK_9796_2_E1

    // --- a. check parameters of PRK_9796_2_E1
    {
      // check modulus
      assertEquals(
          Hex.extractHexDigits(
              """
                  FAA8ED34 EEF1CE38 D29814B6 EEAA154D C060BB37 EB1A51E8 AB0398DD ADDFD334
                  CB9BE20C 087B1DDF 1F78A397 62B5F20A 7A730086 30913CD2 EE60183D E249DD16
                  9CA4EB3A E0420E51 13D73050 4A73A926 BEFBFF32 C89858DE 5E5B3899 FEC52521
                  04933163 625F2963 5AB8FAA7 AA14C4F3 C0DD2470 DEFCEB39 2429110A 0149A771
                  """),
          AfiBigInteger.toHex(PRK_9796_2_E1.getModulus()));

      // check private exponent
      assertEquals(
          Hex.extractHexDigits(
              """
                  0A71B48C DF4A1342 5E1BAB87 9F471638 92AEB277 A9CBC369 B1CAD109 3C93FE22
                  33267EC0 805A7693 F6A506D0 F9723F6B 1A6F755A ECB0B7DE 1F440102 94186936
                  316AAC4B F39B37BF 6105DFA0 AEA60B82 C17306F2 179F2ED4 704D5A6F BCB141C0
                  C9380F5A 500823CE 67E8ED81 7F8A5100 59E9541B 498C91F4 1ABE8C10 6220E72B
                  """),
          AfiBigInteger.toHex(PRK_9796_2_E1.getPrivateExponent()));
    } // end --- a.
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

  /** Test method for {@link RsaPrivateCrtKeyImpl#RsaPrivateCrtKeyImpl(BigInteger, BigInteger)}. */
  @Test
  void test_RsaPrivateCrtKey__BigInteger2() {
    //  Assertions:
    // ... a. calculateParameters(p, q)-method works as expected
    // ... b. calculateParameters(p, q, e)-method works as expected

    // Test strategy:
    // --- a. Create an RSA key with the smallest possible modulus length
    // --- b. Create a small  RSA keys, i.e., eMin=F0
    // --- c. Create a larger RSA keys, i.e., eMin=F4

    // --- a. Create an RSA key with the smallest possible modulus length
    BigInteger primeP = BigInteger.valueOf(3);
    BigInteger primeQ = BigInteger.valueOf(5);

    RsaPrivateCrtKeyImpl dut = new RsaPrivateCrtKeyImpl(primeP, primeQ);

    assertEquals(0xf, dut.getModulus().intValueExact());
    assertEquals(0x3, dut.getPublicExponent().intValueExact());
    assertEquals(0x3, dut.getPrivateExponent().intValueExact());
    assertEquals(0x3, dut.getPrimeP().intValueExact());
    assertEquals(0x5, dut.getPrimeQ().intValueExact());
    assertEquals(0x1, dut.getPrimeExponentP().intValueExact());
    assertEquals(0x3, dut.getPrimeExponentQ().intValueExact());
    assertEquals(0x2, dut.getCrtCoefficient().intValueExact());

    // --- b. Create a small  RSA keys, i.e., eMin=F0
    // b.1 e = eMin = F0
    // b.2 e > eMin = F0, because of p
    // b.3 e > eMin = F0, because of q
    // b.4 e < F4

    // b.1 e = eMin = F0
    primeP = BigInteger.valueOf(11); // gcd(F0, p - 1) = gcd(3, 10) = 1 == 1
    primeQ = BigInteger.valueOf(17); // gcd(F0, q - 1) = gcd(3, 16) = 1 == 1

    dut = new RsaPrivateCrtKeyImpl(primeP, primeQ);

    assertEquals(0xbb, dut.getModulus().intValueExact());
    assertEquals(0x03, dut.getPublicExponent().intValueExact());
    assertEquals(0x1b, dut.getPrivateExponent().intValueExact());
    assertEquals(0x0b, dut.getPrimeP().intValueExact());
    assertEquals(0x11, dut.getPrimeQ().intValueExact());
    assertEquals(0x07, dut.getPrimeExponentP().intValueExact());
    assertEquals(0x0b, dut.getPrimeExponentQ().intValueExact());
    assertEquals(0x02, dut.getCrtCoefficient().intValueExact());

    // b.2 e > eMin = F0, because of p
    primeP = BigInteger.valueOf(7); // gcd(F0, p - 1) = gcd(3, 6) = 3 != 1
    primeQ = BigInteger.valueOf(11); // gcd(F0, q - 1) = gcd(3, 10) = 1 == 1

    dut = new RsaPrivateCrtKeyImpl(primeP, primeQ);

    assertEquals(0x4d, dut.getModulus().intValueExact());
    assertEquals(0x07, dut.getPublicExponent().intValueExact());
    assertEquals(0x0d, dut.getPrivateExponent().intValueExact());
    assertEquals(0x07, dut.getPrimeP().intValueExact());
    assertEquals(0x0b, dut.getPrimeQ().intValueExact());
    assertEquals(0x01, dut.getPrimeExponentP().intValueExact());
    assertEquals(0x03, dut.getPrimeExponentQ().intValueExact());
    assertEquals(0x02, dut.getCrtCoefficient().intValueExact());

    // b.3 e > eMin = F0, because of q
    primeP = BigInteger.valueOf(11); // gcd(F0, p - 1) = gcd(3, 10) = 1 == 1
    primeQ = BigInteger.valueOf(13); // gcd(F0, q - 1) = gcd(3, 12) = 3 != 1

    dut = new RsaPrivateCrtKeyImpl(primeP, primeQ);

    assertEquals(0x8f, dut.getModulus().intValueExact());
    assertEquals(0x07, dut.getPublicExponent().intValueExact());
    assertEquals(0x2b, dut.getPrivateExponent().intValueExact());
    assertEquals(0x0b, dut.getPrimeP().intValueExact());
    assertEquals(0x0d, dut.getPrimeQ().intValueExact());
    assertEquals(0x03, dut.getPrimeExponentP().intValueExact());
    assertEquals(0x07, dut.getPrimeExponentQ().intValueExact());
    assertEquals(0x06, dut.getCrtCoefficient().intValueExact());

    // b.4 e < F4
    final int border = 64;
    final BigInteger sqrt = ONE.shiftLeft(64).sqrt();
    primeQ = AfiBigInteger.previousProbablePrime(sqrt);
    primeP = AfiBigInteger.previousProbablePrime(primeQ);

    dut = new RsaPrivateCrtKeyImpl(primeP, primeQ);

    assertEquals(border, dut.getModulusLengthBit());
    assertTrue(INFIMUM_E.compareTo(dut.getPublicExponent()) > 0);

    // --- c. Create a larger RSA keys, i.e., eMin=F4
    // c.0 e >= F4
    primeP = sqrt.nextProbablePrime();
    primeQ = primeP.nextProbablePrime();

    dut = new RsaPrivateCrtKeyImpl(primeP, primeQ);

    assertEquals(border + 1, dut.getModulusLengthBit());
    assertTrue(INFIMUM_E.compareTo(dut.getPublicExponent()) <= 0);
  } // end method */

  /**
   * Test method for {@link RsaPrivateCrtKeyImpl#RsaPrivateCrtKeyImpl(BigInteger, BigInteger,
   * BigInteger)}.
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount"})
  @Test
  void test_RsaPrivateCrtKey__BigInteger_BigInteger_BigInteger() {
    //  Assertions:
    // ... a. calculateParameters(p, q, e)-method works as expected

    // Test strategy:
    // --- a. Smoke test with valid parameters
    // --- b. Smoke test with invalid publicExponent due to primeP
    // --- c. Smoke test with invalid publicExponent due to primeQ

    // --- a. Smoke test with valid parameters
    BigInteger primeP = BigInteger.valueOf(3);
    BigInteger primeQ = BigInteger.valueOf(5);
    BigInteger pubExp = BigInteger.valueOf(3);

    RsaPrivateCrtKeyImpl dut = new RsaPrivateCrtKeyImpl(primeP, primeQ, pubExp);

    assertEquals(0xf, dut.getModulus().intValueExact());
    assertEquals(0x3, dut.getPublicExponent().intValueExact());
    assertEquals(0x3, dut.getPrivateExponent().intValueExact());
    assertEquals(0x3, dut.getPrimeP().intValueExact());
    assertEquals(0x5, dut.getPrimeQ().intValueExact());
    assertEquals(0x1, dut.getPrimeExponentP().intValueExact());
    assertEquals(0x3, dut.getPrimeExponentQ().intValueExact());
    assertEquals(0x2, dut.getCrtCoefficient().intValueExact());

    primeP = BigInteger.valueOf(11); // gcd(F0, p - 1) = gcd(3, 10) = 1 == 1
    primeQ = BigInteger.valueOf(17); // gcd(F0, q - 1) = gcd(3, 16) = 1 == 1
    pubExp = BigInteger.valueOf(3);

    dut = new RsaPrivateCrtKeyImpl(primeP, primeQ, pubExp);

    assertEquals(0xbb, dut.getModulus().intValueExact());
    assertEquals(0x03, dut.getPublicExponent().intValueExact());
    assertEquals(0x1b, dut.getPrivateExponent().intValueExact());
    assertEquals(0x0b, dut.getPrimeP().intValueExact());
    assertEquals(0x11, dut.getPrimeQ().intValueExact());
    assertEquals(0x07, dut.getPrimeExponentP().intValueExact());
    assertEquals(0x0b, dut.getPrimeExponentQ().intValueExact());
    assertEquals(0x02, dut.getCrtCoefficient().intValueExact());

    primeP = BigInteger.valueOf(7); // gcd(F0, p - 1) = gcd(3, 6) = 3 != 1
    primeQ = BigInteger.valueOf(11); // gcd(F0, q - 1) = gcd(3, 10) = 1 == 1
    pubExp = BigInteger.valueOf(7);

    dut = new RsaPrivateCrtKeyImpl(primeP, primeQ, pubExp);

    assertEquals(0x4d, dut.getModulus().intValueExact());
    assertEquals(0x07, dut.getPublicExponent().intValueExact());
    assertEquals(0x0d, dut.getPrivateExponent().intValueExact());
    assertEquals(0x07, dut.getPrimeP().intValueExact());
    assertEquals(0x0b, dut.getPrimeQ().intValueExact());
    assertEquals(0x01, dut.getPrimeExponentP().intValueExact());
    assertEquals(0x03, dut.getPrimeExponentQ().intValueExact());
    assertEquals(0x02, dut.getCrtCoefficient().intValueExact());

    primeP = BigInteger.valueOf(11); // gcd(F0, p - 1) = gcd(3, 10) = 1 == 1
    primeQ = BigInteger.valueOf(13); // gcd(F0, q - 1) = gcd(3, 12) = 3 != 1
    pubExp = BigInteger.valueOf(7);

    dut = new RsaPrivateCrtKeyImpl(primeP, primeQ, pubExp);

    assertEquals(0x8f, dut.getModulus().intValueExact());
    assertEquals(0x07, dut.getPublicExponent().intValueExact());
    assertEquals(0x2b, dut.getPrivateExponent().intValueExact());
    assertEquals(0x0b, dut.getPrimeP().intValueExact());
    assertEquals(0x0d, dut.getPrimeQ().intValueExact());
    assertEquals(0x03, dut.getPrimeExponentP().intValueExact());
    assertEquals(0x07, dut.getPrimeExponentQ().intValueExact());
    assertEquals(0x06, dut.getCrtCoefficient().intValueExact());

    // RSA key from ftp://ftp.rsa.com/pub/pkcs/ascii/examples.asc
    primeP = new BigInteger("33d48445c859e52340de704bcdda065fbb4058d740bd1d67d29e9c146c11cf61", 16);
    primeQ = new BigInteger("335e8408866b0fd38dc7002d3f972c67389a65d5d8306566d5c4f2a5aa52628b", 16);
    pubExp = new BigInteger("010001", 16);

    dut = new RsaPrivateCrtKeyImpl(primeP, primeQ, pubExp);

    assertEquals(
        new BigInteger(
            "0a66791dc6988168de7ab77419bb7fb0c001c62710270075142942e19a8d8c51d053"
                + "b3e3782a1de5dc5af4ebe99468170114a1dfe67cdc9a9af55d655620bbab",
            16),
        dut.getModulus());
    assertEquals(new BigInteger("010001", 16), dut.getPublicExponent());
    assertEquals(
        new BigInteger(
            "0123c5b61ba36edb1d3679904199a89ea80c09b9122e1400c09adcf7784676d01d23"
                + "356a7d44d6bd8bd50e94bfc723fa87d8862b75177691c11d757692df8881",
            16),
        dut.getPrivateExponent());
    assertEquals(
        new BigInteger("33d48445c859e52340de704bcdda065fbb4058d740bd1d67d29e9c146c11cf61", 16),
        dut.getPrimeP());
    assertEquals(
        new BigInteger("335e8408866b0fd38dc7002d3f972c67389a65d5d8306566d5c4f2a5aa52628b", 16),
        dut.getPrimeQ());
    assertEquals(
        new BigInteger("045ec90071525325d3d46db79695e9afacc4523964360e02b119baa366316241", 16),
        dut.getPrimeExponentP());
    assertEquals(
        new BigInteger("15eb327360c7b60d12e5e2d16bdcd97981d17fba6b70db13b20b436e24eada59", 16),
        dut.getPrimeExponentQ());
    assertEquals(
        new BigInteger("2ca6366d72781dfa24d34a9a24cbc2ae927a9958af426563ff63fb11658a461d", 16),
        dut.getCrtCoefficient());

    // --- b. Smoke test with invalid publicExponent due to primeP
    // --- c. Smoke test with invalid publicExponent due to primeQ
    {
      final var input =
          Set.of(
              List.of(7, 11, 3), // --- b.
              List.of(11, 13, 3) // --- c.
              );
      for (final var pqe : input) {
        final var p = BigInteger.valueOf(pqe.getFirst());
        final var q = BigInteger.valueOf(pqe.get(1));
        final var e = BigInteger.valueOf(pqe.getLast());

        assertThrows(IllegalArgumentException.class, () -> new RsaPrivateCrtKeyImpl(p, q, e));
      } // end For (pqe...)
    } // end --- b, c.
  } // end method */

  /**
   * Test method for {@link RsaPrivateCrtKeyImpl#RsaPrivateKeyImpl}. where all parameters are input
   * values
   */
  @Test
  void test_RsaPrivateCrtKey__BigInteger7() {
    // Assertions:
    // ... a. superclasses work as expected
    // ... b. getter work as expected

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    // --- b. some random input

    // --- a. smoke test with a fixed input
    {
      final BigInteger n = BigInteger.valueOf(15);
      final BigInteger e = BigInteger.valueOf(7);
      final BigInteger d = BigInteger.valueOf(9);
      final BigInteger p = ZERO;
      final BigInteger q = ONE;
      final BigInteger dp = TWO;
      final BigInteger dq = TEN;
      final BigInteger c = BigInteger.valueOf(12);

      final RsaPrivateCrtKeyImpl dut = new RsaPrivateCrtKeyImpl(n, e, d, p, q, dp, dq, c);

      assertSame(n, dut.getModulus());
      assertSame(e, dut.getPublicExponent());
      assertSame(d, dut.getPrivateExponent());
      assertSame(p, dut.getPrimeP());
      assertSame(q, dut.getPrimeQ());
      assertSame(dp, dut.getPrimeExponentP());
      assertSame(dq, dut.getPrimeExponentQ());
      assertSame(c, dut.getCrtCoefficient());
    } // end --- a.

    // --- b. some random input
    IntStream.rangeClosed(0, 20)
        .forEach(
            i -> {
              final BigInteger n = new BigInteger(2048, RNG);
              final BigInteger e = BigInteger.valueOf(RNG.nextLong());
              final BigInteger d = new BigInteger(2040, RNG);
              final BigInteger p = BigInteger.valueOf(RNG.nextLong());
              final BigInteger q = BigInteger.valueOf(RNG.nextLong());
              final BigInteger dp = BigInteger.valueOf(RNG.nextLong());
              final BigInteger dq = BigInteger.valueOf(RNG.nextLong());
              final BigInteger c = BigInteger.valueOf(RNG.nextLong());

              final RsaPrivateCrtKeyImpl dut = new RsaPrivateCrtKeyImpl(n, e, d, p, q, dp, dq, c);

              assertSame(n, dut.getModulus());
              assertSame(e, dut.getPublicExponent());
              assertSame(d, dut.getPrivateExponent());
              assertSame(p, dut.getPrimeP());
              assertSame(q, dut.getPrimeQ());
              assertSame(dp, dut.getPrimeExponentP());
              assertSame(dq, dut.getPrimeExponentQ());
              assertSame(c, dut.getCrtCoefficient());
            }); // end forEach(i -> ...)
    // end --- b.
  } // end method */

  /**
   * Test method for {@link RsaPrivateCrtKeyImpl#RsaPrivateCrtKeyImpl(DerSequence,
   * EafiRsaPrkFormat)}.
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount"})
  @Test
  void test_RsaPrivateCrtKey__DerSequence_EafiRsaPrkFormat() {
    // Assertions:
    // ... a. RsaPrivateCrtKeyImpl(BigInteger ... crtValues)-constructor works as expected
    // ... b. package tlv works as expected
    // ... c. when testing PKCS#1 it is assumed that PKCS#1 works as expected

    // Test strategy:
    // --- a. smoke test, PKCS#1 format
    // --- b. smoke test, PKCS#8 format
    // --- c. ERROR, PKCS#1: version != 0
    // --- d. ERROR, PKCS#1: children missing
    // --- e. ERROR, PKCS#1: correct tag, but instance of PrimitiveBerTlv rather than DerInteger
    // --- f. ERROR, PKCS#8: version != 0
    // --- g. ERROR, PKCS#8: children missing
    // --- h. ERROR, PKCS#8: correct tag, but not a specific TLV-object
    // --- i. ERROR, PKCS#8: wrong OID
    // --- j. ERROR, PKCS#8: invalid parameters
    // --- k. ERROR, PKCS#8: invalid privateKey

    for (int i = 20; i-- > 0; ) { // NOPMD assignment in operand
      // --- a. smoke test, PKCS#1 format
      final BigInteger version = ZERO;
      final BigInteger modulus = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
      final BigInteger pubExpo = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
      final BigInteger priExpo = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
      final BigInteger primeP = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
      final BigInteger primeQ = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
      final BigInteger priExP = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
      final BigInteger priExQ = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
      final BigInteger crtCoe = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
      final DerSequence privateKey =
          new DerSequence(
              List.of(
                  new DerInteger(version),
                  new DerInteger(modulus),
                  new DerInteger(pubExpo),
                  new DerInteger(priExpo),
                  new DerInteger(primeP),
                  new DerInteger(primeQ),
                  new DerInteger(priExP),
                  new DerInteger(priExQ),
                  new DerInteger(crtCoe)));

      RsaPrivateCrtKeyImpl dut = new RsaPrivateCrtKeyImpl(privateKey, EafiRsaPrkFormat.PKCS1);

      assertEquals(modulus, dut.getModulus());
      assertEquals(pubExpo, dut.getPublicExponent());
      assertEquals(priExpo, dut.getPrivateExponent());
      assertEquals(primeP, dut.getPrimeP());
      assertEquals(primeQ, dut.getPrimeQ());
      assertEquals(priExP, dut.getPrimeExponentP());
      assertEquals(priExQ, dut.getPrimeExponentQ());
      assertEquals(crtCoe, dut.getCrtCoefficient());

      // --- b. smoke test, PKCS#8 format
      final DerSequence pkcs8 =
          new DerSequence(
              List.of(
                  new DerInteger(version),
                  new DerSequence(List.of(new DerOid(AfiOid.rsaEncryption), DerNull.NULL)),
                  new DerOctetString(privateKey.getEncoded())));

      dut = new RsaPrivateCrtKeyImpl(pkcs8, EafiRsaPrkFormat.PKCS8);

      assertEquals(modulus, dut.getModulus());
      assertEquals(pubExpo, dut.getPublicExponent());
      assertEquals(priExpo, dut.getPrivateExponent());
      assertEquals(primeP, dut.getPrimeP());
      assertEquals(primeQ, dut.getPrimeQ());
      assertEquals(priExP, dut.getPrimeExponentP());
      assertEquals(priExQ, dut.getPrimeExponentQ());
      assertEquals(crtCoe, dut.getCrtCoefficient());
    } // end For (i...)
    // end --- a, b.

    // --- c. ERROR, PKCS#1: version != 0
    {
      final var versions =
          Set.of(
              BigInteger.valueOf(Integer.MIN_VALUE).subtract(ONE), // just below Integer range
              BigInteger.valueOf(-1), // supremum of values too small
              ONE, // infimum of values too big
              BigInteger.valueOf(Integer.MAX_VALUE).add(ONE) // just above Integer range
              );
      for (final var version : versions) {
        final DerSequence keyMaterial = new DerSequence(List.of(new DerInteger(version)));

        assertThrows(
            IllegalArgumentException.class,
            () -> new RsaPrivateCrtKeyImpl(keyMaterial, EafiRsaPrkFormat.PKCS1));
      } // end For (version...)
    } // end --- c.

    // --- d. ERROR, PKCS#1: children missing
    // d.0 version and the rest missing
    // d.1 modulus and the rest missing
    // d.2 pubExpo and the rest missing
    // d.3 priExpo and the rest missing
    // d.4 primeP  and the rest missing
    // d.5 primeQ  and the rest missing
    // d.6 priExP  and the rest missing
    // d.7 priExQ  and the rest missing
    // d.8 just crtCoe missing
    // d.9 everything there
    {
      // d.0 version and the rest missing
      final DerSequence keyMaterial0 = new DerSequence(new ArrayList<>());
      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(keyMaterial0, EafiRsaPrkFormat.PKCS1));

      // d.1 modulus and the rest missing
      final DerSequence keyMaterial1 = keyMaterial0.add(new DerInteger(ZERO)); // add version
      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(keyMaterial1, EafiRsaPrkFormat.PKCS1));
      final BigInteger modulus = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);

      // d.2 pubExpo and the rest missing
      final DerSequence keyMaterial2 = keyMaterial1.add(new DerInteger(modulus)); // add modulus
      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(keyMaterial2, EafiRsaPrkFormat.PKCS1));
      final BigInteger pubExpo = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);

      // d.3 priExpo and the rest missing
      final DerSequence keyMaterial3 = keyMaterial2.add(new DerInteger(pubExpo)); // add publicExpo
      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(keyMaterial3, EafiRsaPrkFormat.PKCS1));
      final BigInteger priExpo = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);

      // d.4 primeP  and the rest missing
      final DerSequence keyMaterial4 = keyMaterial3.add(new DerInteger(priExpo)); // add privateExpo
      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(keyMaterial4, EafiRsaPrkFormat.PKCS1));
      final BigInteger primeP = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);

      // d.5 primeQ  and the rest missing
      final DerSequence keyMaterial5 = keyMaterial4.add(new DerInteger(primeP)); // add primeP
      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(keyMaterial5, EafiRsaPrkFormat.PKCS1));
      final BigInteger primeQ = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);

      // d.6 priExP  and the rest missing
      final DerSequence keyMaterial6 = keyMaterial5.add(new DerInteger(primeQ)); // add primeQ
      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(keyMaterial6, EafiRsaPrkFormat.PKCS1));
      final BigInteger priExP = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);

      // d.7 priExQ  and the rest missing
      final DerSequence keyMaterial7 = keyMaterial6.add(new DerInteger(priExP)); // add primeExpoP
      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(keyMaterial7, EafiRsaPrkFormat.PKCS1));
      final BigInteger priExQ = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);

      // d.8 just crtCoe missing
      final DerSequence keyMaterial8 = keyMaterial7.add(new DerInteger(priExQ)); // add primeExpoQ
      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(keyMaterial8, EafiRsaPrkFormat.PKCS1));
      final BigInteger crtCoe = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);

      // d.9 everything there
      final DerSequence keyMaterial9 = keyMaterial8.add(new DerInteger(crtCoe)); // add crtCoeff.
      final RsaPrivateCrtKeyImpl dut =
          new RsaPrivateCrtKeyImpl(keyMaterial9, EafiRsaPrkFormat.PKCS1);
      assertEquals(modulus, dut.getModulus());
      assertEquals(pubExpo, dut.getPublicExponent());
      assertEquals(priExpo, dut.getPrivateExponent());
      assertEquals(primeP, dut.getPrimeP());
      assertEquals(primeQ, dut.getPrimeQ());
      assertEquals(priExP, dut.getPrimeExponentP());
      assertEquals(priExQ, dut.getPrimeExponentQ());
      assertEquals(crtCoe, dut.getCrtCoefficient());
    }

    // --- e. ERROR, PKCS#1: correct tag, but instance of PrimitiveBerTlv rather than DerInteger
    // impossible with package de.gematik.smartcards.tlv version 0.7.x

    // --- f. ERROR, PKCS#8: version != 0
    {
      final var versions =
          Set.of(
              BigInteger.valueOf(Integer.MIN_VALUE).subtract(ONE), // just below Integer range
              BigInteger.valueOf(-1), // supremum of values too small
              ONE, // infimum of values too big
              BigInteger.valueOf(Integer.MAX_VALUE).add(ONE) // just above Integer range
              );
      for (final var version : versions) {
        final DerSequence keyMaterial = new DerSequence(List.of(new DerInteger(version)));

        assertThrows(
            IllegalArgumentException.class,
            () -> new RsaPrivateCrtKeyImpl(keyMaterial, EafiRsaPrkFormat.PKCS8));
      } // end For (version...)
    } // end --- f.

    // --- g. ERROR, PKCS#8: children missing
    // g.0 happy case (from which other test-cases are deduced)
    // g.1 just version missing
    // g.2 just privatekeyAlgorithm missing
    // g.3 just privatekeyAlgorithm.OID missing
    // g.4 just privatekeyAlgorithm.NULL missing
    // g.5 just privateKey missing

    // g.0 happy case (from which other test-cases are deduced)
    final BigInteger version = ZERO;
    final BigInteger modulus = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
    final BigInteger pubExpo = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
    final BigInteger priExpo = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
    final BigInteger primeP = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
    final BigInteger primeQ = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
    final BigInteger priExP = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
    final BigInteger priExQ = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
    final BigInteger crtCoe = BigInteger.valueOf(RNG.nextLong() & 0x7fffffffffffffffL);
    final DerSequence privateKey =
        new DerSequence(
            List.of(
                new DerInteger(version),
                new DerInteger(modulus),
                new DerInteger(pubExpo),
                new DerInteger(priExpo),
                new DerInteger(primeP),
                new DerInteger(primeQ),
                new DerInteger(priExP),
                new DerInteger(priExQ),
                new DerInteger(crtCoe)));
    {
      final RsaPrivateCrtKeyImpl dut =
          new RsaPrivateCrtKeyImpl(
              new DerSequence(
                  // PKCS#8 structure
                  List.of(
                      new DerInteger(version), // version
                      new DerSequence(
                          // privateKeyAlgorithm
                          List.of(
                              new DerOid(AfiOid.rsaEncryption), // OID
                              DerNull.NULL // parameters
                              )),
                      new DerOctetString(privateKey.getEncoded()) // privateKey
                      )),
              EafiRsaPrkFormat.PKCS8);
      assertEquals(modulus, dut.getModulus());
      assertEquals(pubExpo, dut.getPublicExponent());
      assertEquals(priExpo, dut.getPrivateExponent());
      assertEquals(primeP, dut.getPrimeP());
      assertEquals(primeQ, dut.getPrimeQ());
      assertEquals(priExP, dut.getPrimeExponentP());
      assertEquals(priExQ, dut.getPrimeExponentQ());
      assertEquals(crtCoe, dut.getCrtCoefficient());
    }

    // g.1 just version missing
    {
      final var input =
          new DerSequence(
              // PKCS#8 structure
              List.of(
                  new DerSequence(
                      // privateKeyAlgorithm
                      List.of(
                          new DerOid(AfiOid.rsaEncryption), // OID
                          DerNull.NULL // parameters
                          )),
                  new DerOctetString(privateKey.getEncoded()) // privateKey
                  ));

      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(input, EafiRsaPrkFormat.PKCS8));
    } // end g.1

    // g.2 just privatekeyAlgorithm missing
    {
      final var input =
          new DerSequence(
              // PKCS#8 structure
              List.of(
                  new DerInteger(version), // version
                  new DerOctetString(privateKey.getEncoded()) // privateKey
                  ));

      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(input, EafiRsaPrkFormat.PKCS8));
    } // end g.2

    // g.3 just privatekeyAlgorithm.OID missing
    {
      final var input =
          new DerSequence(
              // PKCS#8 structure
              List.of(
                  new DerInteger(version), // version
                  new DerSequence(
                      // privateKeyAlgorithm
                      List.of(
                          DerNull.NULL // parameters
                          )),
                  new DerOctetString(privateKey.getEncoded()) // privateKey
                  ));

      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(input, EafiRsaPrkFormat.PKCS8));
    } // end g.3

    // g.4 just privatekeyAlgorithm.NULL missing
    {
      final var input =
          new DerSequence(
              // PKCS#8 structure
              List.of(
                  new DerInteger(version), // version
                  new DerSequence(
                      // privateKeyAlgorithm
                      List.of(
                          new DerOid(AfiOid.rsaEncryption) // OID
                          )),
                  new DerOctetString(privateKey.getEncoded()) // privateKey
                  ));

      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(input, EafiRsaPrkFormat.PKCS8));
    } // end g.4

    // g.5 just privateKey missing
    {
      final var input =
          new DerSequence(
              // PKCS#8 structure
              List.of(
                  new DerInteger(version), // version
                  new DerSequence(
                      // privateKeyAlgorithm
                      List.of(
                          new DerOid(AfiOid.rsaEncryption), // OID
                          DerNull.NULL // parameters
                          ))));

      assertThrows(
          IllegalArgumentException.class,
          () -> new RsaPrivateCrtKeyImpl(input, EafiRsaPrkFormat.PKCS8));
    } // end g.5

    // --- h. ERROR, PKCS#8: correct tag, but not a specific TLV-object
    // impossible with package de.gematik.smartcards.tlv version 2.0.0-alpha6

    // --- i. ERROR, PKCS#8: wrong OID
    {
      final var oids =
          AfiOid.PREDEFINED.stream().filter(oid -> !AfiOid.rsaEncryption.equals(oid)).toList();
      for (final var oid : oids) {
        final var input =
            new DerSequence(
                // PKCS#8 structure
                List.of(
                    new DerInteger(version), // version
                    new DerSequence(
                        // privateKeyAlgorithm
                        List.of(
                            new DerOid(oid), // OID
                            DerNull.NULL // parameters
                            )),
                    new DerOctetString(privateKey.getEncoded()) // privateKey
                    ));

        final var e =
            assertThrows(
                IllegalArgumentException.class,
                () -> new RsaPrivateCrtKeyImpl(input, EafiRsaPrkFormat.PKCS8));
        assertEquals("OID = " + oid.getName() + " instead of rsaEncryption", e.getMessage());
        assertNull(e.getCause());
      } // end For (oid...)
    } // end --- i.

    // --- j. ERROR, PKCS#8: invalid parameters
    {
      final var input =
          new DerSequence(
              // PKCS#8 structure
              List.of(
                  new DerInteger(version), // version
                  new DerSequence(
                      // privateKeyAlgorithm
                      List.of(
                          new DerOid(AfiOid.rsaEncryption), // OID
                          BerTlv.getInstance("05 01 00") // parameters
                          )),
                  new DerOctetString(privateKey.getEncoded()) // privateKey
                  ));

      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> new RsaPrivateCrtKeyImpl(input, EafiRsaPrkFormat.PKCS8));
      assertEquals("invalid parameters in privateKeyAlgorithm", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- j.

    // --- k. ERROR, PKCS#8: invalid privateKey
    // Note: Invalid PKCS#1 format is tested in this method above, see c, d and e.
    //       So we can be lazy here and invoke with an invalid TLV-structure.
    {
      final var input =
          new DerSequence(
              // PKCS#8 structure
              List.of(
                  new DerInteger(version), // version
                  new DerSequence(
                      // privateKeyAlgorithm
                      List.of(
                          new DerOid(AfiOid.rsaEncryption), // OID
                          DerNull.NULL // parameters
                          )),
                  new DerOctetString(Hex.toByteArray("010203")) // privateKey
                  ));

      final var e =
          assertThrows(
              IllegalArgumentException.class,
              () -> new RsaPrivateCrtKeyImpl(input, EafiRsaPrkFormat.PKCS8));

      assertEquals("unexpected IOException", e.getMessage());
    } // end --- k.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#RsaPrivateCrtKeyImpl(RSAPrivateCrtKey)}. */
  @Test
  void test_RsaPrivateCrtKeyImpl_RsaPrivateCrtKey() {
    // Assertions:
    // ... a. RsaPrivateCrtKeyImpl(BigInteger, ...)-constructor works as expected

    // Test strategy:
    // --- a. smoke test
    {
      final RSAPrivateCrtKey prk = getPrk(MIN_LENGTH);

      final var dut = new RsaPrivateCrtKeyImpl(prk);

      assertSame(prk.getModulus(), dut.getModulus());
      assertSame(prk.getPublicExponent(), dut.getPublicExponent());
      assertSame(prk.getPrivateExponent(), dut.getPrivateExponent());
      assertSame(prk.getPrimeP(), dut.getPrimeP());
      assertSame(prk.getPrimeQ(), dut.getPrimeQ());
      assertSame(prk.getPrimeExponentP(), dut.getPrimeExponentP());
      assertSame(prk.getPrimeExponentQ(), dut.getPrimeExponentQ());
      assertSame(prk.getCrtCoefficient(), dut.getCrtCoefficient());
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#gakpLog2(int)}. */
  @Test
  void test_gakpLog2__int() {
    // Assertions:
    // ... a. gakpLog2(int, BigInteger)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: modulus length too small

    // --- a. smoke test
    {
      final var nLength = INFIMUM_SECURE_MODULUS;
      final var dut = RsaPrivateCrtKeyImpl.gakpLog2(nLength);

      assertEquals(nLength, dut.getModulusLengthBit());
      assertEquals(INFIMUM_E, dut.getPublicExponent());
    } // end --- a.

    // --- b. ERROR: modulus length too small
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#gakpLog2(int, BigInteger)}. */
  @Test
  void test_gakpLog2__int_BigInteger() {
    // Assertions:
    // ... a. createPrime(double, BigInteger)-method works as expected
    // ... b. gakpLog2(double, double, BigInteger, boolean)-method works as expected
    // ... b. check()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of valid input parameters
    // --- c. ERROR: modulus-length out of range
    // --- d. ERROR: publicExponent out of range
    // --- e. ERROR: publicExponent even

    final BigInteger eInfimum = INFIMUM_E;
    final BigInteger eAwkward = zzzAwkwardE();
    final BigInteger eSupremum = ONE.shiftLeft(256).subtract(ONE);

    // --- a. smoke test
    {
      final int modulusLength = 512;

      final var dut = RsaPrivateCrtKeyImpl.gakpLog2(modulusLength, eAwkward);

      assertEquals(modulusLength, dut.getModulusLengthBit());
      assertEquals(eAwkward, dut.getPublicExponent());
      assertEquals("", dut.check());
    } // end --- a.

    // --- b. loop over a bunch of valid input parameters
    // spotless:off
    Set.of(eInfimum, eAwkward, eSupremum)
        .forEach(
            publicExponent ->
                RNG.intsClosed(INFIMUM_SECURE_MODULUS, 4096, 5)
                    .parallel() // for perfromance boost
                    .forEach(
                        modulusLength -> {
                          final var dut =
                              RsaPrivateCrtKeyImpl.gakpLog2(modulusLength, publicExponent);

                          assertEquals(modulusLength, dut.getModulusLengthBit());
                          assertEquals(publicExponent, dut.getPublicExponent());
                          assertEquals("", dut.check());
                        }) // end forEach(modulusLength -> ...)
        ); // end forEach(publicExponent -> ...)
    // spotless:on
    // end --- b.

    // --- c. ERROR: modulus-length out of range
    Set.of(INFIMUM_SECURE_MODULUS - 1)
        .forEach(
            modulusLength ->
                assertThrows(
                    IllegalArgumentException.class,
                    () ->
                        RsaPrivateCrtKeyImpl.gakpLog2(
                            modulusLength, eInfimum))); // end forEach(modulusLength -> ...)
    // end --- c.

    // --- d. ERROR: publicExponent out of range
    Set.of(ONE)
        .forEach(
            publicExponent ->
                assertThrows(
                    IllegalArgumentException.class,
                    () -> RsaPrivateCrtKeyImpl.gakpLog2(INFIMUM_SECURE_MODULUS, publicExponent),
                    () ->
                        publicExponent.toString(16)
                            + ", bitLength = "
                            + publicExponent.bitLength()));
    // end --- d.

    // --- e. ERROR: publicExponent even
    {
      final var eInput = eInfimum.add(ONE);

      assertThrows(
          IllegalArgumentException.class,
          () -> RsaPrivateCrtKeyImpl.gakpLog2(INFIMUM_SECURE_MODULUS, eInput));
    } // end --- e.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#gakpLog2(double, double, BigInteger, boolean)}. */
  // @org.junit.jupiter.api.Disabled
  @Test
  void test_gakpLog2__double_double_BigInteger_boolean() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with modulus at upper border
    // --- b. smoke test with modulus at lower border
    // --- c. some random values

    final var publicExponent = INFIMUM_E;
    final var d2min = -5;
    final var d2max = 35;
    final var y2mean = 0.5;
    final var y2span = 1;

    // --- a. smoke test with modulus at upper border
    {
      final int ldnExpected =
          RNG.nextIntClosed(INFIMUM_SECURE_MODULUS, INFIMUM_SECURE_MODULUS + 100);
      final var epsilon = 2.0;

      final var dut = RsaPrivateCrtKeyImpl.gakpLog2(ldnExpected, epsilon, publicExponent, true);

      final var d2 = d2(dut.getPrimeP(), dut.getPrimeQ(), epsilon);
      final var y2 = y2(dut.getModulus(), ldnExpected);

      assertTrue(
          (d2min < d2) && (d2 < d2max),
          () ->
              String.format(
                  "ldnExpected=%d, epsilon=%f: d2 = %f out of range", ldnExpected, epsilon, d2));
      assertTrue(
          (-y2mean - y2span < y2) && (y2 < y2span - y2mean),
          () -> String.format("y2 = %f out of range", y2));
    } // end --- a.

    // --- b. smoke test with modulus at lower border
    {
      final int ldnExpected =
          RNG.nextIntClosed(INFIMUM_SECURE_MODULUS, INFIMUM_SECURE_MODULUS + 100);
      final var epsilon = 1.0;

      final var dut = RsaPrivateCrtKeyImpl.gakpLog2(ldnExpected, epsilon, publicExponent, false);

      final var d2 = d2(dut.getPrimeP(), dut.getPrimeQ(), epsilon);
      final var y2 = y2(dut.getModulus(), ldnExpected);

      assertTrue(
          (d2min < d2) && (d2 < d2max),
          () ->
              String.format(
                  "ldnExpected=%d, epsilon=%f: d2 = %f out of range", ldnExpected, epsilon, d2));
      assertTrue(
          (y2mean - y2span < y2) && (y2 < y2mean + y2span),
          () -> String.format("y2 = %f out of range", y2));
    } // end --- b.

    // Note: Test "c" is commented out, because for large modulus the tests
    //       checking d2 fail. It seems that more investigations are necessary
    //       to find an acceptable range for d2.
    /*/ --- c. some random values
    RNG.intsClosed(INFIMUM_SECURE_MODULUS, 4096, 5).forEach(modulusLength -> {
      final var rndModulus = RNG.nextDouble();
      final var epsilon = 0.1 + 29.9 * RNG.nextDouble();
      final var ldnExpected = modulusLength - rndModulus;
      final var bigModulus = rndModulus < 0.5;

      final var dut = RsaPrivateCrtKeyImpl.gakpLog2(
          ldnExpected, epsilon, publicExponent,
          bigModulus
      );

      final double d2 = d2(dut.getPrimeP(), dut.getPrimeQ(), epsilon);
      final double ldnPresent = AfiBigInteger.ld(dut.getModulus());
      assertTrue(
          (d2min < d2) && (d2 < d2max),
          () -> String.format(
              "ldnExpected=%f, epsilon=%f: d2 = %f out of range",
              ldnExpected, epsilon, d2
          )
      );
      assertEquals(ldnExpected, ldnPresent, 1e-9);
    }); // end forEach(modulusLength -> ...)
    // end --- c. */
  } // end method */

  @SuppressWarnings("PMD.ShortMethodName")
  private static double d2(final BigInteger p, final BigInteger q, final double epsilonExpected) {
    // It is:
    // d1 = epsilonExpected - epsilonPresent
    // It is expected, that d1 is in range [-1e-15, 1e-15].
    // Thus, for better readability d2 = 1e15 * d1 is used.

    final double factorE = 1e15;
    final double epsilonPresent = AfiBigInteger.epsilon(q, p);

    return factorE * (epsilonExpected - epsilonPresent);
  } // end method */

  @SuppressWarnings("PMD.ShortMethodName")
  private static double y2(final BigInteger n, final int ldnExpected) {
    // It is:
    // n1 = ld (modulus / modulusExact) = ld ((modulusExact + delta) / modulusExact)
    //    = ld (1 + delta / modulusExact)
    //    = ln (1 + delta / modulusExact) / ln 2                                (1)
    // Assume 0 <= delta << modulusExact, thus, it follows:
    // n1 = delta / modulusExact / ln 2                                         (2)
    // It is expected, that n1 is in the range -2^-45 < +2^-45.
    // Thus, instead of n1 the value n2 = 2^45 * n1 is used for better readability.

    final int shifter = 128;
    final double invLn2 = 1 / Math.log(2);
    final double factorY = invLn2 / ONE.shiftLeft(shifter - 45).doubleValue();
    final BigInteger nExact = ONE.shiftLeft(ldnExpected);

    final BigInteger delta = n.subtract(nExact);
    final double deltaShift = delta.shiftRight(ldnExpected - shifter).doubleValue();

    return deltaShift * factorY;
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#createPrime(double, BigInteger)}. */
  @Test
  void test_createPrime__double_BigInteger() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of valid input parameters

    // --- a. smoke test
    {
      final var publicExponent = INFIMUM_E;
      final var bitLength = 512;
      final var ldPrime = bitLength - RNG.nextDouble();

      final var dut = RsaPrivateCrtKeyImpl.createPrime(ldPrime, publicExponent);

      assertEquals(bitLength, dut.bitLength());
      assertEquals(ldPrime, AfiBigInteger.ld(dut), Double.MIN_VALUE);
      assertEquals(0, ONE.compareTo(publicExponent.gcd(dut.subtract(ONE))));
      assertTrue(dut.isProbablePrime(CERTAINTY));
    } // end --- a.

    // --- b. loop over a bunch of valid input parameters
    final int noPublicExponents = 2;
    final int noBitLength = 5;
    IntStream.rangeClosed(0, noPublicExponents)
        .forEach(
            i -> {
              final BigInteger publicExponent = BigInteger.probablePrime(32, RNG);

              RNG.intsClosed(256, 2048, noBitLength)
                  .parallel() // for performance boost
                  .forEach(
                      bitLength -> {
                        final double ldPrime = bitLength - RNG.nextDouble();

                        final BigInteger dut =
                            RsaPrivateCrtKeyImpl.createPrime(ldPrime, publicExponent);

                        assertEquals(bitLength, dut.bitLength());
                        assertEquals(ldPrime, AfiBigInteger.ld(dut), Double.MIN_VALUE);
                        assertEquals(0, ONE.compareTo(publicExponent.gcd(dut.subtract(ONE))));
                        assertTrue(dut.isProbablePrime(CERTAINTY));
                      }); // end forEach(bitLength -> ...)
            }); // end forEach(i -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#check()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.NcssCount"})
  @Test
  void test_check() {
    // Assertions:
    // ... a. getter work as expected

    // Test strategy:
    // --- a. smoke test without findings
    // --- b. FINDING: p not prime
    // --- c. FINDING: q not prime
    // --- d. FINDING: primes not compatible with e
    // --- e. FINDING: e not invertible
    // --- f. FINDING: d wrong
    // --- g. FINDING: n wrong
    // --- h. FINDING: dp wrong
    // --- i. FINDING: dq wrong
    // --- j. FINDING: c wrong
    // --- k. FINDING: more than one finding, here: p and q not prime
    // --- l. FINDING: d is calculated the naive way

    final var mapCounter = new TreeMap<Integer, Integer>();
    final var mapKeyGene = new TreeMap<Integer, Integer>();
    final var noRounds = 61; // Note: Host "arthur" performs 6100 (ish) rounds per minute.
    for (int i = noRounds; i-- > 0; ) { // NOPMD reassignment
      KEY_PAIR_REPOSITORY.remove(MIN_LENGTH);
      final var prkOk = new RsaPrivateCrtKeyImpl(getPrk(MIN_LENGTH));

      // --- a. smoke test without findings
      {
        final var dut = new RsaPrivateCrtKeyImpl(prkOk);

        final String actual = dut.check();

        assertEquals("", actual);
      } // end --- a.

      // --- b. FINDING: p not prime
      {
        final var p1 = BigInteger.probablePrime(75, RNG);
        final var p2 = BigInteger.probablePrime(75, RNG);
        final var p = p1.multiply(p2);
        final var q = BigInteger.probablePrime(151, RNG);
        final var dut = new RsaPrivateCrtKeyImpl(p, q);
        final var expected = String.format("p= 0x%s is not prime", p.toString(16));

        final String actual = dut.check();

        assertEquals(expected, actual);
      } // end --- b.

      // --- c. FINDING: q not prime
      {
        final var p = BigInteger.probablePrime(150, RNG);
        final var q1 = BigInteger.probablePrime(75, RNG);
        final var q2 = BigInteger.probablePrime(76, RNG);
        final var q = q1.multiply(q2);
        final var dut = new RsaPrivateCrtKeyImpl(p, q);
        final var expected = String.format("q= 0x%s is not prime", q.toString(16));

        final String actual = dut.check();

        assertEquals(expected, actual);
      } // end --- c.

      // --- d. FINDING: primes not compatible with e
      // --- e. FINDING: e not invertible
      // d.1 p not compatible with e
      // d.2 q not compatible with e
      {
        var primeOk = BigInteger.probablePrime(100, RNG);
        var primeNok = BigInteger.probablePrime(100, RNG);

        var primeOk1 = primeOk.subtract(ONE);
        var primeNok1 = primeNok.subtract(ONE);
        BigInteger pubExpoStrange = BigInteger.valueOf(65_541);

        int counter = 0;
        while ((0 != ONE.compareTo(primeOk1.gcd(pubExpoStrange)))
            || (0 == ONE.compareTo(primeNok1.gcd(pubExpoStrange)))) {
          pubExpoStrange = pubExpoStrange.add(TWO);
          counter++;

          if (0 == (counter % 1_000)) {
            // ... long and unsuccessful search
            //     => try a different key
            primeOk = BigInteger.probablePrime(100, RNG);
            primeNok = BigInteger.probablePrime(100, RNG);

            primeOk1 = primeOk.subtract(ONE);
            primeNok1 = primeNok.subtract(ONE);
            pubExpoStrange = BigInteger.valueOf(65_541);
          } // end fi
        } // end While (...)
        mapCounter.putIfAbsent(counter, 0);
        mapCounter.put(counter, mapCounter.get(counter) + 1);

        assertEquals(0, ONE.compareTo(primeOk1.gcd(pubExpoStrange)));
        assertNotEquals(0, ONE.compareTo(primeNok1.gcd(pubExpoStrange)));

        // d.1 p not compatible with e
        {
          final var prkD = new RsaPrivateCrtKeyImpl(primeNok, primeOk);
          final var dut =
              new RsaPrivateCrtKeyImpl(
                  prkD.getModulus(),
                  pubExpoStrange,
                  prkD.getPrivateExponent(),
                  prkD.getPrimeP(),
                  prkD.getPrimeQ(),
                  prkD.getPrimeExponentP(),
                  prkD.getPrimeExponentQ(),
                  prkD.getCrtCoefficient());
          final var expected =
              String.format(
                  "1 != gcd(e=0x%s, (p-1)(q-1)%n%s",
                  pubExpoStrange.toString(16), "e not relatively prime to (p-1)(q-1)");

          final String actual = dut.check();

          assertEquals(expected, actual);
        } // end --- d.1

        // d.2 q not compatible with e
        {
          final var prkD = new RsaPrivateCrtKeyImpl(primeOk, primeNok);
          final var dut =
              new RsaPrivateCrtKeyImpl(
                  prkD.getModulus(),
                  pubExpoStrange,
                  prkD.getPrivateExponent(),
                  prkD.getPrimeP(),
                  prkD.getPrimeQ(),
                  prkD.getPrimeExponentP(),
                  prkD.getPrimeExponentQ(),
                  prkD.getCrtCoefficient());
          final var expected =
              String.format(
                  "1 != gcd(e=0x%s, (p-1)(q-1)%n%s",
                  pubExpoStrange.toString(16), "e not relatively prime to (p-1)(q-1)");

          final String actual = dut.check();

          assertEquals(expected, actual);
        } // end --- d.2
      } // end --- d.

      // --- f. FINDING: d wrong
      {
        final var dNew = prkOk.getPrivateExponent().add(ONE);

        final var dut =
            new RsaPrivateCrtKeyImpl(
                prkOk.getModulus(),
                prkOk.getPublicExponent(),
                dNew,
                prkOk.getPrimeP(),
                prkOk.getPrimeQ(),
                dNew.mod(prkOk.getPrimeP().subtract(ONE)),
                dNew.mod(prkOk.getPrimeQ().subtract(ONE)),
                prkOk.getCrtCoefficient());
        final var expected = "d is wrong";

        final String actual = dut.check();

        assertEquals(expected, actual);
      } // end --- f.

      // --- g. FINDING: n wrong
      {
        final var dut =
            new RsaPrivateCrtKeyImpl(
                prkOk.getModulus().add(TWO),
                prkOk.getPublicExponent(),
                prkOk.getPrivateExponent(),
                prkOk.getPrimeP(),
                prkOk.getPrimeQ(),
                prkOk.getPrimeExponentP(),
                prkOk.getPrimeExponentQ(),
                prkOk.getCrtCoefficient());
        final var expected = "n is wrong";

        final String actual = dut.check();

        assertEquals(expected, actual);
      } // end --- g.

      // --- h. FINDING: dp wrong
      {
        final var dut =
            new RsaPrivateCrtKeyImpl(
                prkOk.getModulus(),
                prkOk.getPublicExponent(),
                prkOk.getPrivateExponent(),
                prkOk.getPrimeP(),
                prkOk.getPrimeQ(),
                prkOk.getPrimeExponentP().add(ONE),
                prkOk.getPrimeExponentQ(),
                prkOk.getCrtCoefficient());
        final var expected = "dp is wrong";

        final String actual = dut.check();

        assertEquals(expected, actual);
      } // end --- h.

      // --- i. FINDING: dq wrong
      {
        final var dut =
            new RsaPrivateCrtKeyImpl(
                prkOk.getModulus(),
                prkOk.getPublicExponent(),
                prkOk.getPrivateExponent(),
                prkOk.getPrimeP(),
                prkOk.getPrimeQ(),
                prkOk.getPrimeExponentP(),
                prkOk.getPrimeExponentQ().add(ONE),
                prkOk.getCrtCoefficient());
        final var expected = "dq is wrong";

        final String actual = dut.check();

        assertEquals(expected, actual);
      } // end --- i.

      // --- j. FINDING: c wrong
      {
        final var dut =
            new RsaPrivateCrtKeyImpl(
                prkOk.getModulus(),
                prkOk.getPublicExponent(),
                prkOk.getPrivateExponent(),
                prkOk.getPrimeP(),
                prkOk.getPrimeQ(),
                prkOk.getPrimeExponentP(),
                prkOk.getPrimeExponentQ(),
                prkOk.getCrtCoefficient().add(ONE));
        final var expected = "c is wrong";

        final String actual = dut.check();

        assertEquals(expected, actual);
      } // end --- j.

      // --- k. FINDING: more than one finding, here: p and q not prime
      {
        final var p1 = BigInteger.probablePrime(75, RNG);
        final var p2 = BigInteger.probablePrime(75, RNG);
        final var p = p1.multiply(p2);
        final var q1 = BigInteger.probablePrime(75, RNG);
        final var q2 = BigInteger.probablePrime(76, RNG);
        final var q = q1.multiply(q2);
        final var dut = new RsaPrivateCrtKeyImpl(p, q);
        final var expected =
            String.format(
                "p= 0x%s is not prime%nq= 0x%s is not prime", p.toString(16), q.toString(16));

        final String actual = dut.check();

        assertEquals(expected, actual);
      } // end --- k.

      // --- l. FINDING: d is calculated the naive way
      {
        // assure that dFips and dNaive differ (which is not always the case if
        // GCD is small)
        RsaPrivateCrtKeyImpl prk;
        int keyGenCounter = 0;
        for (; true; keyGenCounter++) {
          final var p = BigInteger.probablePrime(100, RNG);
          final var q = BigInteger.probablePrime(100, RNG);
          prk = new RsaPrivateCrtKeyImpl(p, q);
          final var e = prk.getPublicExponent();
          final var p1 = p.subtract(ONE);
          final var q1 = q.subtract(ONE);
          final var pq1 = p1.multiply(q1);
          final var dNaive = e.modInverse(pq1);
          final var dFips = e.modInverse(pq1.divide(p1.gcd(q1)));

          if (dNaive.compareTo(dFips) != 0) {
            break;
          } // end fi
        } // end forever-loop
        // ... dFips and dNaive differ
        //     => we get an appropriate finding
        mapKeyGene.putIfAbsent(keyGenCounter, 0);
        mapKeyGene.put(keyGenCounter, mapKeyGene.get(keyGenCounter) + 1);

        final var e = prk.getPublicExponent();
        final var p = prk.getPrimeP();
        final var q = prk.getPrimeQ();
        final var dNaive = e.modInverse(p.subtract(ONE).multiply(q.subtract(ONE)));
        final var dut =
            new RsaPrivateCrtKeyImpl(
                prk.getModulus(),
                e,
                dNaive,
                p,
                q,
                prk.getPrimeExponentP(),
                prk.getPrimeExponentQ(),
                prk.getCrtCoefficient());
        final var expected = "d is not in accordance to FIPS 186-4";

        final var actual = dut.check();

        assertEquals(
            expected, actual, () -> dut.getEncoded(EafiRsaPrkFormat.PKCS1).toString(" ", "   "));
      } // end --- l.
    } // end For (i...)
    LOGGER.atInfo().log(
        "mapCounter: {}{}",
        LINE_SEPARATOR,
        mapCounter.entrySet().stream()
            .map(entry -> String.format("counter =%7d,%9d", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(LINE_SEPARATOR)));
    LOGGER.atInfo().log(
        "mapKeyGeneration: {}{}",
        LINE_SEPARATOR,
        mapKeyGene.entrySet().stream()
            .map(entry -> String.format("keyGene =%7d,%9d", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(LINE_SEPARATOR)));
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#checkSecurity()}. */
  @Test
  void test_checkSecurity() {
    // Assertions:
    // ... a. getModulusLengthBit()-method works as expected
    // ... b. getPublicExponent()-method works as expected
    // ... c. deltaMin(int)-method works as expected
    // ... d. getPrimeP()-method works as expected
    // ... e. getPrimeQ()-method works as expected

    // Test strategy:
    // --- a. smoke test without findings
    // --- b. FINDING: modulus too small
    // --- c. FINDING: public exponent too small
    // --- d. FINDING: public exponent too big
    // --- e. FINDING: p and q too close together
    // --- f. FINDING: p and q too far apart
    // --- g. FINDING: modulus and publicExponent too small

    // --- a. smoke test without findings
    {
      final var dut = RsaPrivateCrtKeyImpl.gakpLog2(INFIMUM_SECURE_MODULUS_LENGTH);

      final String actual = dut.checkSecurity();

      assertTrue(actual.isEmpty(), () -> actual + LINE_SEPARATOR + dut);
    } // end --- a.

    // --- b. FINDING: modulus too small
    {
      final var prk = getPrk(INFIMUM_SECURE_MODULUS_LENGTH - 1);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(),
              ONE.shiftLeft(SUPREMUM_LD_E).subtract(ONE),
              prk.getPrivateExponent());

      final var actual = dut.checkSecurity();

      assertEquals("modulus length too small: 1899", actual);
    } // end --- b.

    // --- c. FINDING: public exponent too small
    {
      final var prk = getPrk(MAX_LENGTH);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), INFIMUM_E.subtract(ONE), prk.getPrivateExponent());

      final var actual = dut.checkSecurity();

      assertEquals("public exponent to small: 65536", actual);
    } // end --- c.

    // --- d. FINDING: public exponent too big
    {
      final var prk = getPrk(MAX_LENGTH);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), ONE.shiftLeft(SUPREMUM_LD_E), prk.getPrivateExponent());

      final var actual = dut.checkSecurity();

      assertEquals("bit length of e out of range: " + (SUPREMUM_LD_E + 1), actual);
    } // end --- d.

    // --- e. FINDING: p and q too close together
    {
      final var publicExponent = INFIMUM_E;
      final var ldq = INFIMUM_SECURE_MODULUS_LENGTH / 2.0 - 0.1;
      final var q = RsaPrivateCrtKeyImpl.createPrime(ldq, publicExponent);
      var p =
          q.subtract(RsaPrivateCrtKeyImpl.deltaMin(INFIMUM_SECURE_MODULUS_LENGTH))
              .nextProbablePrime();
      while (0 != ONE.compareTo(publicExponent.gcd(p.subtract(ONE)))) {
        p = p.nextProbablePrime();
      } // end While (p not compatible with publicExponent)
      final var dut = new RsaPrivateCrtKeyImpl(p, q, publicExponent);

      final var actual = dut.checkSecurity();

      assertEquals("p and q too close together", actual);
    } // end --- e.

    // --- f. FINDING: p and q too far apart
    {
      final var publicExponent = INFIMUM_E;
      final var ldn = INFIMUM_SECURE_MODULUS_LENGTH - 0.5;
      final var epsilon = 30.0; // DLS_DEAD_LOCAL_STORE
      final var ldp = Math.nextDown((ldn - epsilon) / 2);
      final var ldq = Math.nextUp((ldn + epsilon) / 2);
      final var p = RsaPrivateCrtKeyImpl.createPrime(ldp, publicExponent);
      final var q = RsaPrivateCrtKeyImpl.createPrime(ldq, publicExponent);
      final var dut = new RsaPrivateCrtKeyImpl(p, q, publicExponent);

      final var actual = dut.checkSecurity();

      assertEquals("epsilon out of range", actual);
    } // end --- e.

    // --- g. FINDING: modulus and publicExponent too small
    {
      final var dut =
          RsaPrivateCrtKeyImpl.gakpLog2(
              INFIMUM_SECURE_MODULUS_LENGTH - 1, BigInteger.valueOf(65_535));

      final var actual = dut.checkSecurity();

      assertEquals(
          String.format(
              "%s%n%s", "modulus length too small: 1899", "public exponent to small: 65535"),
          actual);
    } // end --- g.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#deltaMin(int)}. */
  @Test
  void test_deltaMin__int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of valid input parameters
    // --- c. loop over a bunch of small modulus lengths

    // --- a. smoke test
    {
      final var modulusLength = 2048;

      final var actual = RsaPrivateCrtKeyImpl.deltaMin(modulusLength);

      assertEquals(modulusLength / 2.0 - 100, AfiBigInteger.ld(actual), 1e-8);
    } // end --- a.

    // --- b. loop over a bunch of valid input parameters
    // Note: Implementation of "AfiRng.intsClosed(...)"- method assures
    //       that also odd values are created.
    RNG.intsClosed(INFIMUM_SECURE_MODULUS_LENGTH, 4096, 5)
        .forEach(
            modulusLength -> {
              final var actual = RsaPrivateCrtKeyImpl.deltaMin(modulusLength);

              // Note: For odd modulus-length <= 250 the method-under-test is not very
              //       accurate (because of rounding effects in sqrt()-method).
              assertEquals(
                  modulusLength / 2.0 - 100,
                  AfiBigInteger.ld(actual),
                  1e-8,
                  () -> Integer.toString(modulusLength));
            }); // end forEach(modulusLength -> ...)
    // end --- b.

    // --- c. loop over a bunch of small modulus lengths
    RNG.intsClosed(-1, INFIMUM_SECURE_MODULUS_LENGTH - 1, 5)
        .forEach(
            modulusLength -> {
              final var actual = RsaPrivateCrtKeyImpl.deltaMin(modulusLength);

              assertSame(ONE, actual, () -> Integer.toString(modulusLength));
            }); // end forEach(modulusLength -> ...)
    // end --- c.
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. difference in modulus
    // --- e. difference in public exponent
    // --- f. difference in private exponent
    // --- g. difference in prime p
    // --- h. difference in prime q
    // --- i. difference in dp
    // --- j. difference in dq
    // --- k. difference in c
    // --- l. different objects, but same content

    final var dut = new RsaPrivateCrtKeyImpl(getPrk(MIN_LENGTH));

    for (final Object obj :
        new Object[] {
          dut, // --- a. same reference
          null, // --- b. null input
          "afi" // --- c. difference in type
        }) {
      assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals to compare object refer.
    } // end For (obj...)

    // --- d. difference in modulus
    // --- e. difference in public exponent
    // --- f. difference in private exponent
    // --- g. difference in prime p
    // --- h. difference in prime q
    // --- i. difference in dp
    // --- j. difference in dq
    // --- k. difference in c
    // --- l. different objects, but same content
    // spotless:off
    Set.of(dut.getModulus(), ZERO).forEach(n -> {
      Set.of(dut.getPublicExponent(), ONE).forEach(e -> {
        Set.of(dut.getPrivateExponent(), TWO).forEach(d -> {
          Set.of(dut.getPrimeP(), ZERO).forEach(p -> {
            Set.of(dut.getPrimeQ(), ONE).forEach(q -> {
              Set.of(dut.getPrimeExponentP(), TWO).forEach(dp -> {
                Set.of(dut.getPrimeExponentQ(), ZERO).forEach(dq -> {
                  Set.of(dut.getCrtCoefficient(), ONE).forEach(c -> {
                    final Object obj = new RsaPrivateCrtKeyImpl(n, e, d, p, q, dp, dq, c);

                    assertNotSame(obj, dut);
                    assertEquals(
                        n.equals(dut.getModulus())
                            && e.equals(dut.getPublicExponent())
                            && d.equals(dut.getPrivateExponent())
                            && p.equals(dut.getPrimeP())
                            && q.equals(dut.getPrimeQ())
                            && dp.equals(dut.getPrimeExponentP())
                            && dq.equals(dut.getPrimeExponentQ())
                            && c.equals(dut.getCrtCoefficient()),
                        dut.equals(obj)
                    );
                  }); // end forEach(c -> ...)
                }); // end forEach(dq -> ...)
              }); // end forEach(dp -> ...)
            }); // end forEach(q -> ...)
          }); // end forEach(p -> ...)
        }); // end forEach(d -> ...)
      }); // end forEach(e -> ...)
    }); // end forEach(n -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#getEncoded()}. */
  @Test
  void test_getEncoded() {
    // Assertions:
    // ... a. RsaPrivateKeyImpl(BigInteger, BigInteger, BigInteger)-constructor works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final var n = BigInteger.valueOf(255);
      final var e = BigInteger.valueOf(7);
      final var d = BigInteger.valueOf(42);
      final var p = BigInteger.valueOf(11);
      final var q = BigInteger.valueOf(17);
      final var dp = BigInteger.valueOf(10);
      final var dq = BigInteger.valueOf(13);
      final var c = BigInteger.valueOf(65);
      final var dut = new RsaPrivateCrtKeyImpl(n, e, d, p, q, dp, dq, c);
      final var expected =
          BerTlv.getInstance(
                  """
                      30 32 #
                      |  02 01 00 #
                      |  30 0d #
                      |  |  06 09 2a864886f70d010101 #
                      |  |  05 00 #
                      |  04 1e #
                      |     ##########
                      |     # 30 1c #
                      |     # |  02 01 00 #
                      |     # |  02 02 00ff #
                      |     # |  02 01 07 #
                      |     # |  02 01 2a #
                      |     # |  02 01 0b #
                      |     # |  02 01 11 #
                      |     # |  02 01 0a #
                      |     # |  02 01 0d #
                      |     # |  02 01 41 #
                      |     ##########
                      """)
              .toStringTree();

      final var actual = BerTlv.getInstance(dut.getEncoded()).toStringTree();

      assertEquals(expected, actual);
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#getEncoded(EafiRsaPrkFormat)}. */
  @Test
  void test_getEncoded__EafiRsaPrkFormat() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test for PKCS#1
    // --- b. smoke test for PKCS#8

    final var n = BigInteger.valueOf(255);
    final var e = BigInteger.valueOf(7);
    final var d = BigInteger.valueOf(42);
    final var p = BigInteger.valueOf(11);
    final var q = BigInteger.valueOf(17);
    final var dp = BigInteger.valueOf(10);
    final var dq = BigInteger.valueOf(13);
    final var c = BigInteger.valueOf(65);
    final var dut = new RsaPrivateCrtKeyImpl(n, e, d, p, q, dp, dq, c);

    // --- a. smoke test for PKCS#1
    {
      final var expected =
          BerTlv.getInstance(
                  """
                      30 1c
                      |  02 01 00
                      |  02 02 00ff
                      |  02 01 07
                      |  02 01 2a
                      |  02 01 0b
                      |  02 01 11
                      |  02 01 0a
                      |  02 01 0d
                      |  02 01 41
                      """)
              .toStringTree();

      final var actual = dut.getEncoded(EafiRsaPrkFormat.PKCS1).toStringTree();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. smoke test for PKCS#8
    {
      final var expected =
          BerTlv.getInstance(
                  """
                      30 32
                      |  02 01 00
                      |  30 0d
                      |  |  06 09 2a864886f70d010101
                      |  |  05 00
                      |  04 1e
                      |     ##########
                      |     # 30 1c #
                      |     # |  02 01 00
                      |     # |  02 02 00ff
                      |     # |  02 01 07
                      |     # |  02 01 2a
                      |     # |  02 01 0b
                      |     # |  02 01 11
                      |     # |  02 01 0a
                      |     # |  02 01 0d
                      |     # |  02 01 41
                      |     ##########
                      """)
              .toStringTree();

      final var actual = dut.getEncoded(EafiRsaPrkFormat.PKCS8).toStringTree();

      assertEquals(expected, actual);
    } // end --- b.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#getEpsilon()}. */
  @Test
  void test_getEpsilon() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with p > q
    // --- b. smoke test with p < q

    final var publicExponent = INFIMUM_E;

    // --- a. smoke test with p > q
    {
      final var p = RsaPrivateCrtKeyImpl.createPrime(201, publicExponent);
      final var q = RsaPrivateCrtKeyImpl.createPrime(200, publicExponent);
      final var dut = new RsaPrivateCrtKeyImpl(p, q, publicExponent);
      final var expected = AfiBigInteger.epsilon(p, q);

      final var actual = dut.getEpsilon();

      assertEquals(expected, actual, 1e-10);
    } // end --- a.

    // --- b. smoke test with p < q
    {
      final var p = RsaPrivateCrtKeyImpl.createPrime(200, publicExponent);
      final var q = RsaPrivateCrtKeyImpl.createPrime(201, publicExponent);
      final var dut = new RsaPrivateCrtKeyImpl(p, q, publicExponent);
      final var expected = AfiBigInteger.epsilon(q, p);

      final var actual = dut.getEpsilon();

      assertEquals(expected, actual, 1e-10);
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#hashCode()}. */
  @Test
  void test_hashCode() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. call hashCode()-method again

    final var dut = new RsaPrivateCrtKeyImpl(getPrk(MAX_LENGTH));
    final var n = dut.getModulus();
    final var e = dut.getPublicExponent();
    final var d = dut.getPrivateExponent();
    final var p = dut.getPrimeP();
    final var q = dut.getPrimeQ();
    final var dp = dut.getPrimeExponentP();
    final var dq = dut.getPrimeExponentQ();
    final var c = dut.getCrtCoefficient();

    final var actual = dut.hashCode();

    // --- a. smoke test
    {
      var expected = (n.hashCode() * 31 + e.hashCode()) * 31 + d.hashCode();
      expected = (expected * 31 + p.hashCode()) * 31 + q.hashCode();
      expected = (expected * 31 + dp.hashCode()) * 31 + dq.hashCode();
      expected = expected * 31 + c.hashCode();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. call hashCode()-method again
    {
      assertEquals(actual, dut.hashCode());
    } // end --- b.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#isEpsilonOkay(double, double)}. */
  @Test
  void test_isEpsilonOkay__double_double() {
    // Assertions:
    // ... a. getEpsilon()-method works as expected

    // Test strategy:
    // --- a. smoke test, okay
    // --- b. FINDING: epsilon too small
    // --- c. FINDING: epsilon too big

    final var dut = new RsaPrivateCrtKeyImpl(getPrk(MIN_LENGTH));

    // --- a. smoke test, okay
    {
      final var infimum = 0.0;
      final var supremum = 40.0;

      assertTrue(dut.isEpsilonOkay(infimum, supremum));
    } // end --- a.

    // --- b. FINDING: epsilon too small
    {
      final var infimum = 40.0;
      final var supremum = 50.0;

      assertFalse(dut.isEpsilonOkay(infimum, supremum));
    } // end --- b.

    // --- c. FINDING: epsilon too big
    {
      final var infimum = -1.0;
      final var supremum = 0.0;

      assertFalse(dut.isEpsilonOkay(infimum, supremum));
    } // end --- c.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#pkcs1RsaDp(BigInteger)}. */
  @Test
  void test__pkcs1_RsaDp__BigInteger() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. border tests
    // --- c. ERROR: m out of range

    final var keyLength = MIN_LENGTH;
    final var prk = getPrk(keyLength); // small key-size for fast operations
    final var n = prk.getModulus();
    final var d = prk.getPrivateExponent();
    final var dut = new RsaPrivateCrtKeyImpl(prk);
    final var infimum = ZERO;
    final var supremum = n.subtract(ONE);

    // --- a. smoke test
    {
      final var c = new BigInteger(keyLength - 1, RNG);
      final var expected = c.modPow(d, n);

      final var actual = dut.pkcs1RsaDp(c);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. border tests
    Set.of(infimum, supremum)
        .forEach(
            c -> {
              final var expected = c.modPow(d, n);

              final var actual = dut.pkcs1RsaDp(c);

              assertEquals(expected, actual);
            }); // end forEach(m -> ...)
    // end --- b.

    // --- c. ERROR: m out of range
    Set.of(infimum.subtract(ONE), supremum.add(ONE))
        .forEach(
            m ->
                assertThrows(
                    IllegalArgumentException.class,
                    () -> dut.pkcs1RsaDp(m))); // end forEach(m -> ...)
    // end --- c.
  } // end method */

  /** Test method for {@link RsaPrivateCrtKeyImpl#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. superclasses work as expected
    // ... b. RsaPrivateCrtKeyImpl(...)-constructor(s) work as expected
    // ... b. get...()-methods works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with fixed values
    {
      final BigInteger n = BigInteger.valueOf(0x123456789L);
      final BigInteger e = BigInteger.valueOf(7);
      final BigInteger d = BigInteger.valueOf(0x54321);
      final BigInteger p = BigInteger.valueOf(0x12345);
      final BigInteger q = BigInteger.valueOf(0x12);
      final BigInteger dp = BigInteger.valueOf(0x332);
      final BigInteger dq = BigInteger.valueOf(0x35);
      final BigInteger c = BigInteger.valueOf(0x4711);

      final RsaPrivateCrtKeyImpl dut = new RsaPrivateCrtKeyImpl(n, e, d, p, q, dp, dq, c);

      assertEquals(
          String.format(
              "n  = 0123456789%n"
                  + "e  =         07%n"
                  + "d  =     054321%n"
                  + "p  = 012345%n"
                  + "q  =     12%n"
                  + "dp =   0332%n"
                  + "dq =     35%n"
                  + "c  =   4711"),
          dut.toString());
    } // end --- a.
  } // end method */

  /**
   * Test method for getter.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link RsaPrivateCrtKeyImpl#getPrimeP()}
   *   <li>{@link RsaPrivateCrtKeyImpl#getPrimeQ()}
   *   <li>{@link RsaPrivateCrtKeyImpl#getPrimeExponentP()}
   *   <li>{@link RsaPrivateCrtKeyImpl#getPrimeExponentQ()}
   *   <li>{@link RsaPrivateCrtKeyImpl#getCrtCoefficient()}
   * </ol>
   */
  @Test
  void test_getter() {
    // Assertions:
    // ... a. superclasses work as expected
    // ... b. RsaPrivateCrtKeyImpl(...)-constructor(s) work as expected

    // Note: These simple methods do not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final BigInteger n = BigInteger.valueOf(15);
      final BigInteger e = BigInteger.valueOf(7);
      final BigInteger d = BigInteger.valueOf(9);
      final BigInteger p = ZERO;
      final BigInteger q = ONE;
      final BigInteger dp = TWO;
      final BigInteger dq = TEN;
      final BigInteger c = BigInteger.valueOf(12);

      final RsaPrivateCrtKeyImpl dut = new RsaPrivateCrtKeyImpl(n, e, d, p, q, dp, dq, c);

      assertSame(n, dut.getModulus());
      assertSame(e, dut.getPublicExponent());
      assertSame(d, dut.getPrivateExponent());
      assertSame(p, dut.getPrimeP());
      assertSame(q, dut.getPrimeQ());
      assertSame(dp, dut.getPrimeExponentP());
      assertSame(dq, dut.getPrimeExponentQ());
      assertSame(c, dut.getCrtCoefficient());
    } // end --- a.
  } // end method */

  /**
   * Test special key, here from RU/TU environment.
   *
   * <p>In particular: 80276883110000145872-C_CH_AUTN_R2048. Idemia claims that c in CRT is short.
   *
   * <p>Findings:
   *
   * <ol>
   *   <li>Yes, c = q^(-1) mod p is short, but okay.
   *   <li>private exponent d is calculated d = e^(-1) mod (p-1)(q-1), instead of d = e^(-1) mod
   *       lcm((p-1)(q-1))
   * </ol>
   */
  @Test
  void test_80276883110000145872_C_Ch_Autn_R2048() {
    final String input =
        """
            308204bc020100300d06092a864886f70d0101010500048204a6308204a2020100
            0282010100b72350e7899eb8d447fbc001e390bd505eafe72eae4b4812d6ab1b0e
            b03997f93b1edc98e5ede08453f399367b7521b6c7ac9a6cba2e357ef27380416c
            7270a092e7adb538c275527d47c062acd59710632c425dbced83bdb0a7b9b9683e
            0b729468fae7bf96f5392f5b95658e771e49e7d7df20c19be2dc1c320e994080ce
            f66175b12e1bb83e5d5c9e6a37c9b5ccd5c406f94d218a083a504b6528f68dfafe
            109c782b5645a8b90ce448ed0717dd49680645f4e022970fcb0a76866dd13aeb9e
            e0a23f5acac84fe58af8666609ac7c7069042a4d23b99c99b30ad6bede570189f2
            737d117863116312a1f75b3f0edeea365a598f93cf52deb55a0cd628d293020301
            000102820101009fa0e4d02ca070b277dbf3ccb1b263913e374ce6df7d36e154c3
            e7ddb1b541c637c5faba3b1050f686c679f18742dd94180f56bb16bd585bc2b0f2
            461464dbcedabd9e27abc5b3c5a6f577b78ab6d1e9b2c7cd412f8047a87be67678
            29688b49e01a7138b742b99492b6dd4d9c14300e734b326db6d44db555ce3bf350
            c9f59a464feeb6b5d39415b1e92aa12eeed6f24e8743835b025dfe210a289c8178
            ef1a40ed5c01b4169cde43e0231edf37309ec4249ffcdbb422c5aad2031d923cd4
            50a4e41f011051df6569c763bb42bc31c5be19d8998efacc4cfe4aeb9f49865146
            91279614d32d1dba40a2a3e215b4cc16c1f30c0b5d845abc8594a5b6f673c76102
            818100baaac90cff5dbc47f0e81362570123cf9d452dcabf3172ce70e35b6844cf
            520aa7c8fb387fd6da90b2cccd694ca04583b194d0da52e74d981c2b8c89a899d9
            2d46515d6ca87f902eaf630d39ecdb3306498d25c5fd09896ee865859322df5f47
            48ae94ba855fada192910b4f3f5af73a3ab076a4c5ee524258732bfbb4e323d702
            818100fb28f59d9f2aa7c21c2e198d6ec30bdcc592d67c751225f4b006e3e198d2
            401b145339561337dbab7a0662ba3b0a5ef96857bbb077ae209206ec9e60b387d6
            fd8785a93884c8e1962c41d9c611c98c9d9b78025f301ef1191eb47f11dc1c1a48
            e60336fd4d8229b06d43ab898095fb29388138451594f5af21abc449af7befa502
            818100879613d1280ffaf9ba67a7e4cf5399a26d16d9ab21f315f413956148bc6a
            66aa2cb3549ef664a67f621056c7bc8bd1d25583c5ac3f799927fa963b4fa63291
            c84c023fbcf42b2c1c6cf2a2a1c784746c85b24b94a3367128290fccd5520833d9
            c05666db90932db865908b4975b86a495665e4d7cc994e10b8d477472e5daa2302
            818035420d3b3c4a2642a209207ffe31bbed37b4186951c5e4688b7f1a89770776
            72878fd243af5593ee3bb4e8f0869e7f5f31c210d9adb72dcee94af99695db433a
            85a75127dbf860e5a042e273fec7975ac061e556dbf463b9a8ea2b5bd529dc6502
            e9419adaf9b3ef666c080ee3466a78e633788425bc6d90927a42e8411eedc5027f
            00881a87c7e265b844bf92653a30d7759f60c11dec3976824547ea808181c97fda
            158f26bf91bc1de9b1e5a07752b8b608af2e8110ea02f4b553444bb7d505971806
            ac97491ed485547fcc1a1b54eafb7899216cf1d8b964f26c00f9607c4da5cfa295
            9ab8c762482bb5a4dfb4582ff4786c5d5140a182f892c7a6e5870e45
            """;
    final DerSequence sequence = (DerSequence) BerTlv.getInstance(input);
    final RsaPrivateCrtKeyImpl dut = new RsaPrivateCrtKeyImpl(sequence, EafiRsaPrkFormat.PKCS8);

    assertEquals(
        String.join(LINE_SEPARATOR, List.of("d is not in accordance to FIPS 186-4")), dut.check());
  } // end method */

  /**
   * Calculates a public exponent with lots of small factors.
   *
   * @return awkward public exponent
   */
  private BigInteger zzzAwkwardE() {
    // Note: Hereafter we calculate a public exponent with lots of small factors.
    //       Thus, with a high probability, a prime-candidate does not
    //       fulfill gcd(e, p-1) == 1 and another prime-candidate has to be
    //       calculated.
    BigInteger prime = AfiBigInteger.THREE; // smallest odd prime
    BigInteger result = prime;

    for (; ; ) {
      prime = prime.nextProbablePrime();
      final BigInteger newResult = result.multiply(prime);

      if (newResult.bitLength() > SUPREMUM_LD_E) {
        return result;
      } // end fi

      result = newResult;
    } // end forever-loop
  } // end method */
} // end class
