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

import static de.gematik.smartcards.crypto.RsaPrivateKeyImpl.INFIMUM_E;
import static de.gematik.smartcards.crypto.RsaPrivateKeyImpl.INFIMUM_SECURE_MODULUS_LENGTH;
import static de.gematik.smartcards.crypto.RsaPrivateKeyImpl.SUPREMUM_LD_E;
import static de.gematik.smartcards.utils.AfiUtils.MEBI;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static de.gematik.smartcards.utils.AfiUtils.VALUES_BOOLEAN;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TWO;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.signers.ISO9796d2PSSSigner;
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
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestRsaPrivateKeyImpl extends RsaKeyRepository {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestRsaPrivateKeyImpl.class); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    RSAPrivateCrtKey prk = getPrk(Integer.MIN_VALUE);
    assertEquals(MIN_LENGTH, prk.getModulus().bitLength());

    prk = getPrk(Integer.MAX_VALUE);
    assertEquals(MAX_LENGTH, prk.getModulus().bitLength());
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

  /**
   * Test method for {@link RsaPrivateKeyImpl#RsaPrivateKeyImpl(BigInteger, BigInteger,
   * BigInteger)}.
   */
  @Test
  void test_RsaPrivateKey__BigInteger_BigInteger_BigInteger() {
    // Assertions:
    // ... a. getter work as expected

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    // --- b. some random input

    // --- a. smoke test with a fixed input
    {
      final var n = BigInteger.valueOf(15);
      final var e = BigInteger.valueOf(7);
      final var d = BigInteger.valueOf(9);

      final var dut = new RsaPrivateKeyImpl(n, e, d);

      assertSame(n, dut.getModulus());
      assertSame(e, dut.getPublicExponent());
      assertSame(d, dut.getPrivateExponent());
    } // end --- a.

    // --- b. some random input
    IntStream.rangeClosed(0, 20)
        .forEach(
            i -> {
              final var n = new BigInteger(2048, RNG);
              final var e = BigInteger.valueOf(RNG.nextLong());
              final var d = new BigInteger(2040, RNG);

              final var dut = new RsaPrivateKeyImpl(n, e, d);

              assertSame(n, dut.getModulus());
              assertSame(e, dut.getPublicExponent());
              assertSame(d, dut.getPrivateExponent());
            }); // end forEach(i -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#messageAllocation(byte[], int)}. */
  @Test
  void test_messageAllocation__byteA_int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of random input
    // --- c. ERROR: numberOfBitsM1 not a multiple of 8
    // --- d. ERROR: numberOfBitsM1 negative

    // --- a. smoke test
    {
      final byte[] message = Hex.toByteArray("000102 03");
      final int numberOfBitsM1 = 24;

      final byte[][] m = RsaPrivateKeyImpl.messageAllocation(message, numberOfBitsM1);

      assertEquals("000102", Hex.toHexDigits(m[0]));
      assertEquals("03", Hex.toHexDigits(m[1]));
    } // end --- a.

    // --- b. bunch of random input
    RNG.intsClosed(0, 256, 5)
        .forEach(
            m1Length -> {
              final int numberOfBitsM1 = m1Length << 3;
              final byte[] m1 = RNG.nextBytes(m1Length);

              RNG.intsClosed(0, 256, 5)
                  .forEach(
                      m2Length -> {
                        final byte[] m2 = RNG.nextBytes(m2Length);
                        final byte[] message = AfiUtils.concatenate(m1, m2);

                        final byte[][] m =
                            RsaPrivateKeyImpl.messageAllocation(message, numberOfBitsM1);

                        assertArrayEquals(m1, m[0]);
                        assertArrayEquals(m2, m[1]);
                      }); // end forEach(m2Length -> ...)
            }); // end forEach(m1Length -> ...)

    // --- c. ERROR: numberOfBitsM1 not a multiple of 8
    Set.of(
            -12,
            -11,
            -10,
            -9,
            -7,
            -6,
            -5,
            -4,
            -3,
            -2,
            -1, // negative values
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            9,
            10,
            11,
            12,
            13,
            14,
            15 // positive values
            )
        .forEach(
            numberOfBitsM1 ->
                assertThrows(
                    IllegalArgumentException.class,
                    () ->
                        RsaPrivateKeyImpl.messageAllocation(
                            AfiUtils.EMPTY_OS,
                            numberOfBitsM1))); // end forEach(numberOfBitsM1 -> ...)

    // --- d. ERROR: numberOfBitsM1 negative
    Set.of(
            Integer.MIN_VALUE, // infimum of possible negative values  AND  multiple of 8
            -8 // supremum of possible negative values  AND  multiple of 8
            )
        .forEach(
            numberOfBitsM1 ->
                assertThrows(
                    NegativeArraySizeException.class,
                    () ->
                        RsaPrivateKeyImpl.messageAllocation(
                            AfiUtils.EMPTY_OS,
                            numberOfBitsM1))); // end forEach(numberOfBitsM1 -> ...)
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#checkSecurity()}. */
  @Test
  void test_checkSecurity() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with an ok-key
    // --- b. FINDING: modulus too small
    // --- c. FINDING: the public exponent is too small
    // --- d. FINDING: the public exponent is too large

    // --- a. smoke test with an ok-key
    {
      final var prk = getPrk(INFIMUM_SECURE_MODULUS_LENGTH);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());

      final var actual = dut.checkSecurity();

      assertTrue(actual.isEmpty());
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

    // --- c. FINDING: the public exponent is too small
    {
      final var prk = getPrk(MAX_LENGTH);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), INFIMUM_E.subtract(ONE), prk.getPrivateExponent());

      final var actual = dut.checkSecurity();

      assertEquals("public exponent to small: 65536", actual);
    } // end --- c.

    // --- d. FINDING: the public exponent is too large
    {
      final var prk = getPrk(MAX_LENGTH);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), ONE.shiftLeft(SUPREMUM_LD_E), prk.getPrivateExponent());

      final var actual = dut.checkSecurity();

      assertEquals("bit length of e out of range: " + (SUPREMUM_LD_E + 1), actual);
    } // end --- d.
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#getAlgorithm()}. */
  @Test
  void test_getAlgorithm() {
    // Assertions:
    // ... a. RsaPrivateKeyImpl(BigInteger, BigInteger, BigInteger)-constructor works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final var n = BigInteger.valueOf(64);
      final var e = BigInteger.valueOf(12);
      final var d = BigInteger.valueOf(42);

      final var dut = new RsaPrivateKeyImpl(n, e, d);

      assertEquals("RSA", dut.getAlgorithm());
      assertEquals(getPrk(MIN_LENGTH).getAlgorithm(), dut.getAlgorithm());
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#getEncoded()}. */
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
      final var dut = new RsaPrivateKeyImpl(n, e, d);
      final var expected =
          BerTlv.getInstance(
                  """
                      30 23
                      |  02 01 00
                      |  30 0d
                      |  |  06 09 2a864886f70d010101
                      |  |  05 00
                      |  04 0f
                      |     ##########
                      |     # 30 0d
                      |     # |  02 01 00
                      |     # |  02 02 00ff
                      |     # |  02 01 07
                      |     # |  02 01 2a
                      |     ##########
                      """)
              .toStringTree();

      final var actual = BerTlv.getInstance(dut.getEncoded()).toStringTree();

      assertEquals(expected, actual);
    } // end --- a.
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

    final var prk = getPrk(MIN_LENGTH);
    final var dut =
        new RsaPrivateKeyImpl(prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());

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
    Set.of(dut.getModulus(), ZERO)
        .forEach(
            n -> {
              Set.of(dut.getPublicExponent(), ONE)
                  .forEach(
                      e -> {
                        Set.of(dut.getPrivateExponent(), TWO)
                            .forEach(
                                d -> {
                                  final Object obj = new RsaPrivateKeyImpl(n, e, d);

                                  assertNotSame(obj, dut);
                                  assertEquals(
                                      n.equals(dut.getModulus())
                                          && e.equals(dut.getPublicExponent())
                                          && d.equals(dut.getPrivateExponent()),
                                      dut.equals(obj));
                                }); // end forEach(d -> ...)
                      }); // end forEach(e -> ...)
            }); // end forEach(n -> ...)
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#getFormat()}. */
  @Test
  void test_getFormat() {
    // Assertions:
    // ... a. RsaPrivateKeyImpl(BigInteger, BigInteger, BigInteger)-constructor works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final var n = BigInteger.valueOf(64);
      final var e = BigInteger.valueOf(12);
      final var d = BigInteger.valueOf(42);

      final var dut = new RsaPrivateKeyImpl(n, e, d);

      assertEquals("PKCS#8", dut.getFormat());
      assertEquals(getPrk(MIN_LENGTH).getFormat(), dut.getFormat());
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#hashCode()}. */
  @Test
  void test_hashCode() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. call hashCode()-method again

    final var n = new BigInteger(1024, RNG);
    final var e = new BigInteger(128, RNG);
    final var d = new BigInteger(512, RNG);
    final var dut = new RsaPrivateKeyImpl(n, e, d);

    final int actual = dut.hashCode();

    // --- a. smoke test
    {
      final var expected = (n.hashCode() * 31 + e.hashCode()) * 31 + d.hashCode();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. call hashCode()-method again
    {
      assertEquals(actual, dut.hashCode());
    } // end --- b.
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#pkcs1RsaDp(BigInteger)}. */
  @Test
  void test__pkcs1_RsaDp___BigInteger() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter work as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. border tests
    // --- c. ERROR: m out of range

    final var keyLength = MIN_LENGTH;
    final var prk = getPrk(keyLength); // small key-size for fast operations
    final var n = prk.getModulus();
    final var e = prk.getPublicExponent();
    final var d = prk.getPrivateExponent();
    final var dut = new RsaPrivateKeyImpl(n, e, d);
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

  /** Test method for {@link RsaPrivateKeyImpl#pkcs1RsaEsOaepDecrypt(byte[], EafiHashAlgorithm)}. */
  @Test
  void test__pkcs1_RsaEs_Oaep_Decrypt__byteA_EafiHashAlgorithm() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter work as expected
    // ... c. pkcs1_RsaEs_Oaep_Decrypt(byte[], byte[], EafiHashAlgorithm)-method
    //        works as expected

    // Test strategy:
    // --- a. smoke test

    final var keyLength = MAX_LENGTH;
    final var puk = getPuk(keyLength);
    final var prk = getPrk(keyLength);
    final var dut =
        new RsaPrivateKeyImpl(prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());

    // --- a. smoke test
    try {
      final var cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
      final var hash = EafiHashAlgorithm.SHA_256;
      final var message = RNG.nextBytes(16, 64);
      final var oaepParams =
          new OAEPParameterSpec(
              hash.getAlgorithm(),
              "MGF1",
              new MGF1ParameterSpec(hash.getAlgorithm()),
              PSource.PSpecified.DEFAULT);
      cipher.init(Cipher.ENCRYPT_MODE, puk, oaepParams);
      final var ciphertext = cipher.doFinal(message);

      final var plaintext = dut.pkcs1RsaEsOaepDecrypt(ciphertext, hash);

      final var expected = Hex.toHexDigits(message);
      final var actual = Hex.toHexDigits(plaintext);
      assertEquals(expected, actual);
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link RsaPrivateKeyImpl#pkcs1RsaEsOaepDecrypt(byte[], byte[],
   * EafiHashAlgorithm)}.
   */
  @Test
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.CyclomaticComplexity",
    "PMD.NcssCount",
  })
  void test__pkcs1_RsaEs_Oaep_Decrypt___byteA_byteA_EafiHashAlgorithm() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter work as expected
    // ... c. pkcs1_RsaDp(BigInteger)-method works as expected

    // Test strategy:
    // --- a. smoke test with manually chosen parameter
    // --- b. loop over relevant range of input parameter
    // --- c. ERROR: capacity too low
    // --- d. ERROR: ciphertext too big
    // --- e. ERROR: no separator end
    // --- f. ERROR: separator octet not ´01´
    // --- g. ERROR: wrong label
    // --- h. ERROR: MSByte not zero

    try {
      final var cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");

      // --- a. smoke test with manually chosen parameter
      {
        final var keyLength = MAX_LENGTH;
        final var puk = getPuk(keyLength);
        final var prk = getPrk(keyLength);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        final var hash = EafiHashAlgorithm.SHA_256;
        final var message = RNG.nextBytes(16, 64);
        final var oaepParams =
            new OAEPParameterSpec(
                hash.getAlgorithm(),
                "MGF1",
                new MGF1ParameterSpec(hash.getAlgorithm()),
                PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.ENCRYPT_MODE, puk, oaepParams);
        final var ciphertext = cipher.doFinal(message);

        final var plaintext = dut.pkcs1RsaEsOaepDecrypt(ciphertext, AfiUtils.EMPTY_OS, hash);

        final var expected = Hex.toHexDigits(message);
        final var actual = Hex.toHexDigits(plaintext);
        assertEquals(expected, actual);
      } // end --- a.

      // --- b. loop over relevant range of input parameter
      for (final var keyLength : IntStream.rangeClosed(MAX_LENGTH - 12, MAX_LENGTH).toArray()) {
        final var puk = getPuk(keyLength);
        final var prk = getPrk(keyLength);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        for (final var hash : EafiHashAlgorithm.values()) {
          final var supremumMessageLength =
              dut.getModulusLengthOctet() - 2 * hash.getDigestLength() - 2;
          for (final var mLength : RNG.intsClosed(1, supremumMessageLength, 5).toArray()) {
            final var message = RNG.nextBytes(mLength);
            final var label = RNG.nextBytes(1, mLength);
            final var oaepParams =
                new OAEPParameterSpec(
                    hash.getAlgorithm(),
                    "MGF1",
                    new MGF1ParameterSpec(hash.getAlgorithm()),
                    new PSource.PSpecified(label));
            cipher.init(Cipher.ENCRYPT_MODE, puk, oaepParams);
            final var ciphertext = cipher.doFinal(message);

            final var plaintext = dut.pkcs1RsaEsOaepDecrypt(ciphertext, label, hash);

            final var expected = Hex.toHexDigits(message);
            final var actual = Hex.toHexDigits(plaintext);
            assertEquals(
                expected,
                actual,
                () -> String.format("ln=%d, %s, mL=%d", keyLength, hash.getAlgorithm(), mLength));
          } // end For (mLength...)
        } // end For (hash...)
      } // end For (keyLength...)
      // end --- b.

      // --- c. ERROR: capacity too low
      // c.1 modulus-length sufficient
      // c.2 modulus-length too small
      {
        final var message = AfiUtils.EMPTY_OS;
        final var hash = EafiHashAlgorithm.SHA_256;
        var keyLength = 8 * (2 * hash.getDigestLength() + 2);

        // c.1 modulus-length sufficient
        {
          final var puk = new RsaPublicKeyImpl(getPuk(keyLength));
          final var prk = getPrk(keyLength);
          final var dut =
              new RsaPrivateKeyImpl(
                  prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
          final var ciphertext = puk.pkcs1RsaEsOaepEncrypt(message, hash);

          final var plaintext = dut.pkcs1RsaEsOaepDecrypt(ciphertext, AfiUtils.EMPTY_OS, hash);

          final var expected = Hex.toHexDigits(message);
          final var actual = Hex.toHexDigits(plaintext);
          assertEquals(expected, actual);
        } // end c.1

        // c.2 modulus-length too small
        keyLength -= 8;
        {
          final var prk = getPrk(keyLength);
          final var dut =
              new RsaPrivateKeyImpl(
                  prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
          final var ciphertext = RNG.nextBytes(dut.getModulusLengthOctet());
          final var label = AfiUtils.EMPTY_OS;

          assertThrows(
              IllegalArgumentException.class,
              () -> dut.pkcs1RsaEsOaepDecrypt(ciphertext, label, hash));
        } // end c.1
      } // end --- c.

      // --- d. ERROR: ciphertext too big
      {
        final var hash = EafiHashAlgorithm.SHA_256;
        final var prk = getPrk(MAX_LENGTH);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        final var ciphertext = AfiBigInteger.i2os(dut.getModulus());
        final var label = AfiUtils.EMPTY_OS;

        assertThrows(
            IllegalArgumentException.class,
            () -> dut.pkcs1RsaEsOaepDecrypt(ciphertext, label, hash));
      } // end --- d.

      // --- e. ERROR: no separator end
      {
        final var keyLength = MAX_LENGTH;
        final var puk = new RsaPublicKeyImpl(getPuk(keyLength));
        final var prk = getPrk(keyLength);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        final var hash = EafiHashAlgorithm.SHA_256;
        final var label = RNG.nextBytes(16, 32);

        // Note: Hereafter we encipher with code similar to
        //       RsaPublicKeyImpl.pkcs1_RsaEs_Oaep_Encrypt(byte[], byte[], EafiHashAlgorithm)
        //       but with separator ´00' and a message containing only ´00' octet.
        final byte[] ciphertext;
        {
          final int k = dut.getModulusLengthOctet();
          final int hLen = hash.getDigestLength();
          final byte[] lHash = hash.digest(label); // step 2.a
          final byte[] db = new byte[k - hLen - 1]; // step 2.c
          System.arraycopy(lHash, 0, db, 0, hLen);

          // Note: Other octets of "db" have to be ´00' for this test here. The
          //       implementation here is such that "db" needs no further
          //       modification.
          // db[db.length - message.length - 1] = (byte) 0x00;
          // System.arraycopy(message, 0, db, db.length - message.length, message.length);

          final byte[] seed = RNG.nextBytes(hLen); // step 2.d
          final byte[] dbMask = hash.maskGenerationFunction(seed, db.length, 0); // step 2.e
          final byte[] maskedDb = new byte[db.length];
          for (int i = maskedDb.length; i-- > 0; ) { // NOPMD assignment in operand
            maskedDb[i] = (byte) (db[i] ^ dbMask[i]); // step 2.f
          } // end For (i...)
          final byte[] seedMask = hash.maskGenerationFunction(maskedDb, hLen, 0); // step 2.g
          final byte[] maskedSeed = new byte[hLen];
          for (int i = hLen; i-- > 0; ) { // NOPMD assignment in operand
            maskedSeed[i] = (byte) (seed[i] ^ seedMask[i]); // step 2.h
          } // end For (i...)

          // step 2.i
          // Note: Prefix '00' is here not necessary, because em is converted
          //       to a positive BigInteger.
          final byte[] em = AfiUtils.concatenate(maskedSeed, maskedDb);

          // --- PKCS#1 clause 7.1.1 step 3: RSA encryption
          final BigInteger p = new BigInteger(1, em); // step 3.a
          final BigInteger c = puk.pkcs1RsaEp(p); // step 3.b

          ciphertext = AfiBigInteger.i2os(c, k); // steps 3.c and 4
        }

        assertThrows(
            IllegalArgumentException.class,
            () -> dut.pkcs1RsaEsOaepDecrypt(ciphertext, label, hash));
      } // end --- g.

      // --- f. ERROR: separator octet not ´01´
      {
        final var keyLength = MAX_LENGTH;
        final var puk = new RsaPublicKeyImpl(getPuk(keyLength));
        final var prk = getPrk(keyLength);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        final var hash = EafiHashAlgorithm.SHA_256;
        final var message = RNG.nextBytes(16, 64);
        final var label = RNG.nextBytes(16, 32);

        // Note: Hereafter we encipher with code similar to
        //       RsaPublicKeyImpl.pkcs1_RsaEs_Oaep_Encrypt(byte[], byte[], EafiHashAlgorithm)
        //       but with different separator.
        final int k = dut.getModulusLengthOctet();
        final int hLen = hash.getDigestLength();
        final byte[] lHash = hash.digest(label); // step 2.a
        final byte[] db = new byte[k - hLen - 1]; // step 2.c
        final byte[] maskedSeed = new byte[hLen];
        final byte[] maskedDb = new byte[db.length];
        for (int separator = 256; separator-- > 0; ) { // NOPMD assignment in operan
          System.arraycopy(lHash, 0, db, 0, hLen);
          db[db.length - message.length - 1] = (byte) separator; // insert separator
          System.arraycopy(message, 0, db, db.length - message.length, message.length);
          final byte[] seed = RNG.nextBytes(hLen); // step 2.d
          final byte[] dbMask = hash.maskGenerationFunction(seed, db.length, 0); // step 2.e
          for (int i = maskedDb.length; i-- > 0; ) { // NOPMD assignment in operand
            maskedDb[i] = (byte) (db[i] ^ dbMask[i]); // step 2.f
          } // end For (i...)
          final byte[] seedMask = hash.maskGenerationFunction(maskedDb, hLen, 0); // step 2.g
          for (int i = hLen; i-- > 0; ) { // NOPMD assignment in operand
            maskedSeed[i] = (byte) (seed[i] ^ seedMask[i]); // step 2.h
          } // end For (i...)

          // step 2.i
          // Note: Prefix '00' is here not necessary, because em is converted
          //       to a positive BigInteger.
          final byte[] em = AfiUtils.concatenate(maskedSeed, maskedDb);

          // --- PKCS#1 clause 7.1.1 step 3: RSA encryption
          final BigInteger p = new BigInteger(1, em); // step 3.a
          final BigInteger c = puk.pkcs1RsaEp(p); // step 3.b
          final var ciphertext = AfiBigInteger.i2os(c, k); // steps 3.c and 4

          if (0x01 == separator) { // NOPMD literal
            final var plaintext = dut.pkcs1RsaEsOaepDecrypt(ciphertext, label, hash);

            final var expected = Hex.toHexDigits(message);
            final var actual = Hex.toHexDigits(plaintext);
            assertEquals(expected, actual);
          } else {
            assertThrows(
                IllegalArgumentException.class,
                () -> dut.pkcs1RsaEsOaepDecrypt(ciphertext, label, hash));
          } // end fi
        } // end For (separator...)
      } // end --- f.

      // --- g. ERROR: wrong label
      // g.1 correct label
      // g.2 wrong label
      {
        final var keyLength = MAX_LENGTH;
        final var puk = new RsaPublicKeyImpl(getPuk(keyLength));
        final var prk = getPrk(keyLength);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        final var hash = EafiHashAlgorithm.SHA_256;
        final var message = RNG.nextBytes(16, 64);
        final var label = Hex.toByteArray("01");
        final var oaepParams =
            new OAEPParameterSpec(
                hash.getAlgorithm(),
                "MGF1",
                new MGF1ParameterSpec(hash.getAlgorithm()),
                new PSource.PSpecified(label));
        cipher.init(Cipher.ENCRYPT_MODE, puk, oaepParams);
        final var ciphertext = cipher.doFinal(message);

        // g.1 correct label
        final var plaintext = dut.pkcs1RsaEsOaepDecrypt(ciphertext, label, hash);

        final var expected = Hex.toHexDigits(message);
        final var actual = Hex.toHexDigits(plaintext);
        assertEquals(expected, actual);

        // g.2 wrong label
        label[0]++;

        assertThrows(
            IllegalArgumentException.class,
            () -> dut.pkcs1RsaEsOaepDecrypt(ciphertext, label, hash));
      } // end --- g.

      // --- h. ERROR: MSByte not zero
      {
        final var keyLength = MAX_LENGTH;
        final var puk = new RsaPublicKeyImpl(getPuk(keyLength));
        final var prk = getPrk(keyLength);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        final var hash = EafiHashAlgorithm.SHA_256;
        final var message = RNG.nextBytes(16, 64);
        final var oaepParams =
            new OAEPParameterSpec(
                hash.getAlgorithm(),
                "MGF1",
                new MGF1ParameterSpec(hash.getAlgorithm()),
                PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.ENCRYPT_MODE, puk, oaepParams);
        final var ciphertext = cipher.doFinal(message);

        // modify DSI (digital signature input)
        final var dsi =
            AfiBigInteger.i2os(
                dut.pkcs1RsaDp(new BigInteger(1, ciphertext)), dut.getModulusLengthOctet());
        assertEquals(0, dsi[0] & 0xff);
        dsi[0]++;
        final var cipherFalse = AfiBigInteger.i2os(puk.pkcs1RsaEp(new BigInteger(1, dsi)));

        assertThrows(
            IllegalArgumentException.class,
            () -> dut.pkcs1RsaEsOaepDecrypt(cipherFalse, AfiUtils.EMPTY_OS, hash));
      } // end --- h.
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#pkcs1RsaEsPkcs1V15Decrypt(byte[])}. */
  @Test
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  void test__pkcs1_RsaEsPkcs1_v1_5_Decrypt__byteA() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter work as expected
    // ... c. pkcs1_RsaDp(BigInteger)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. ERROR: MSByte not ´00´
    // --- d. ERROR: 2nd octet not ´02´
    // --- e. ERROR: less than 8 padding octet
    // --- f. ERROR: no separator octet with value ´00´

    try {
      final Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

      // --- a. smoke test
      {
        final var prk = getPrk(MIN_LENGTH);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        final var puk = dut.getPublicKey();
        final byte[] message = RNG.nextBytes(20);
        cipher.init(Cipher.ENCRYPT_MODE, puk);
        final byte[] cipherText = cipher.doFinal(message);

        final byte[] plaintext = dut.pkcs1RsaEsPkcs1V15Decrypt(cipherText);

        final var expected = Hex.toHexDigits(message);
        final var actual = Hex.toHexDigits(plaintext);
        assertEquals(expected, actual);
      } // end --- a.

      // --- b. loop over relevant range of input parameter
      for (final var keyLength : IntStream.rangeClosed(MAX_LENGTH - 12, MAX_LENGTH).toArray()) {
        final var puk = getPuk(keyLength);
        final var prk = getPrk(keyLength);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        final var supremumMessageLength = dut.getModulusLengthOctet() - 11;
        for (final var mLength : RNG.intsClosed(1, supremumMessageLength, 5).toArray()) {
          final var message = RNG.nextBytes(mLength);
          cipher.init(Cipher.ENCRYPT_MODE, puk);
          final var ciphertext = cipher.doFinal(message);

          final var plaintext = dut.pkcs1RsaEsPkcs1V15Decrypt(ciphertext);

          final var expected = Hex.toHexDigits(message);
          final var actual = Hex.toHexDigits(plaintext);
          assertEquals(expected, actual);
        } // end For (mLength...)
      } // end For (keyLength...)
      // end --- b.

      {
        final var prk = getPrk(MIN_LENGTH);
        final var dut =
            new RsaPrivateKeyImpl(
                prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
        final var puk = dut.getPublicKey();
        final var dsi = RNG.nextBytes(puk.getModulusLengthOctet());

        Map.ofEntries(
                Map.entry("0102 1112131415161718 00", false), // --- c. ERROR: MSByte not ´00´
                Map.entry("0003 1112131415161718 00", false), // --- d. ERROR: 2nd octet not ´02´
                Map.entry("0002 00", false), // --- e. ERROR: less than 8 padding octet
                Map.entry(
                    "0002 11121314151617 00", false), // --- e. ERROR: less than 8 padding octet
                Map.entry("0002 1112131415161718 00", true) // 8 padding octet
                )
            .forEach(
                (msbytes, success) -> {
                  final var msoctets = Hex.toByteArray(msbytes);
                  System.arraycopy(msoctets, 0, dsi, 0, msoctets.length);
                  final var ciphertext =
                      AfiBigInteger.i2os(
                          puk.pkcs1RsaEp(new BigInteger(1, dsi)), puk.getModulusLengthOctet());

                  if (success) {
                    final var plaintext = dut.pkcs1RsaEsPkcs1V15Decrypt(ciphertext);

                    final var expected = Hex.toHexDigits(dsi, 11, puk.getModulusLengthOctet() - 11);
                    final var actual = Hex.toHexDigits(plaintext);
                    assertEquals(expected, actual);
                  } else {
                    assertThrows(
                        IllegalArgumentException.class,
                        () -> dut.pkcs1RsaEsPkcs1V15Decrypt(ciphertext));
                  } // end else
                }); // end forEach((msbytes, success) -> ...)

        // --- f. ERROR: no separator octet with value ´00´
        {
          Arrays.fill(dsi, (byte) 1);
          dsi[0] = 0;
          dsi[1] = 2;
          final var ciphertext =
              AfiBigInteger.i2os(
                  puk.pkcs1RsaEp(new BigInteger(1, dsi)), puk.getModulusLengthOctet());

          assertThrows(
              IllegalArgumentException.class, () -> dut.pkcs1RsaEsPkcs1V15Decrypt(ciphertext));
        } // end --- f.
      } // end --- c, d, e, f.
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#pkcs1RsaSp1(BigInteger)}. */
  @Test
  void test__pkcs1_RsaSp1___BigInteger() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter work as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. border tests
    // --- c. ERROR: m out of range

    final var keyLength = MIN_LENGTH;
    final var prk = getPrk(keyLength); // small key-size for fast operations
    final var n = prk.getModulus();
    final var e = prk.getPublicExponent();
    final var d = prk.getPrivateExponent();
    final var dut = new RsaPrivateKeyImpl(n, e, d);
    final var infimum = ZERO;
    final var supremum = n.subtract(ONE);

    // --- a. smoke test
    {
      final var c = new BigInteger(keyLength - 1, RNG);
      final var expected = c.modPow(d, n);

      final var actual = dut.pkcs1RsaSp1(c);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. border tests
    Set.of(infimum, supremum)
        .forEach(
            c -> {
              final var expected = c.modPow(d, n);

              final var actual = dut.pkcs1RsaSp1(c);

              assertEquals(expected, actual);
            }); // end forEach(m -> ...)

    // --- c. ERROR: m out of range
    Set.of(infimum.subtract(ONE), supremum.add(ONE))
        .forEach(
            m ->
                assertThrows(
                    IllegalArgumentException.class,
                    () -> dut.pkcs1RsaDp(m))); // end forEach(m -> ...)
  } // end method */

  /**
   * Test method for {@link RsaPrivateKeyImpl#pkcs1RsaSsaPkcs1V15Sign(byte[], EafiHashAlgorithm)}.
   */
  @Test
  void test__pkcs1_RsaSsaPkcs1_v1_5_Sign___byteA_EafiHashAlgorithm() {
    // Assertions:
    // ... a. pkcs1_RsaSp1(BigInteger)-method works as expected

    // Test strategy:
    // --- a. smoke test with examples from ftp://ftp.rsa.com/pub/pkcs/ascii/examples.asc
    // --- b. loop over relevant range of input parameter

    // --- a. smoke test with examples from ftp://ftp.rsa.com/pub/pkcs/ascii/examples.asc
    {
      final RsaPrivateCrtKeyImpl dut =
          new RsaPrivateCrtKeyImpl(
              new BigInteger(
                  "33d48445c859e52340de704bcdda065fbb4058d740bd1d67d29e9c146c11cf61", 16),
              new BigInteger(
                  "335e8408866b0fd38dc7002d3f972c67389a65d5d8306566d5c4f2a5aa52628b", 16),
              new BigInteger("010001", 16));
      final EafiHashAlgorithm hash = EafiHashAlgorithm.MD2;

      // example 1:
      {
        final byte[] message =
            BerTlv.getInstance(
                    """
                        30 81a4
                         02 01 00
                         30 42
                          31 0b
                           30 09
                            06 03 550406
                            13 02 5553
                          31 1d
                           30 1b
                            06 03 55040a
                            13 14 4578616d706c65204f7267616e697a6174696f6e
                          31 14
                           30 12
                            06 03 550403
                            13 0b 5465737420557365722031
                         30 5b
                          30 0d
                           06 09 2a864886f70d010101
                           05 00
                          03 4a 00304702400a66791dc6988168de7ab77419bb7fb0c001c62710270075142942e19a8d8c51
                                d053b3e3782a1de5dc5af4ebe99468170114a1dfe67cdc9a9af55d655620bbab0203010001
                        """)
                .getEncoded();
        final byte[] expected =
            Hex.toByteArray(
                """
                    06db36cb18d3475b9c01db3c789528080279bbaeff2b7d558ed6615987c85186
                    3f8a6c2cffbc89c3f75a18d96b127c717d54d0d8048da8a0544626d17a2a8fbe
                    """);

        final byte[] present = dut.pkcs1RsaSsaPkcs1V15Sign(message, hash);

        assertArrayEquals(expected, present);
      } // end example 1

      // example 2:
      {
        final byte[] message =
            Hex.toByteArray("45766572796f6e65206765747320467269646179206f66662e");
        final byte[] expected =
            Hex.toByteArray(
                """
                    05fa6a812fc7df8bf4f2542509e03e846e11b9c620be2009efb440efbcc66921
                    6994ac04f341b57d05202d428fb2a27b5c77dfd9b15bfc3d559353503410c1e1
                    """);

        final byte[] present = dut.pkcs1RsaSsaPkcs1V15Sign(message, hash);

        assertArrayEquals(expected, present);
      } // end example 2
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    // Note: We need at least a 744 bit key for SHA512WithRSA signatures.
    // spotless:off
    IntStream.rangeClosed(768, 768 + 16).forEach(keyLength -> {
      final RSAPrivateCrtKey prk = getPrk(keyLength);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
      final RsaPublicKeyImpl puk =
          new RsaPublicKeyImpl(prk.getModulus(), prk.getPublicExponent());

      Arrays.stream(EafiHashAlgorithm.values()).forEach(hash -> {
        try {
          final String algoName = switch (hash) {
            case SHA_1 -> "SHA1";
            case SHA_224 -> "SHA224";
            case SHA_256 -> "SHA256";
            case SHA_384 -> "SHA384";
            case SHA_512 -> "SHA512";
            default -> hash.getAlgorithm();
          } + "WithRSA";
          final Signature verifier = Signature.getInstance(algoName, BC);
          verifier.initVerify(puk);

          for (final int messageLength : RNG.intsClosed(0, 1024, 5).toArray()) {
            final byte[] message = RNG.nextBytes(messageLength);
            verifier.update(message);

            final byte[] signature = dut.pkcs1RsaSsaPkcs1V15Sign(message, hash);

            assertTrue(verifier.verify(signature));
          } // end For (messageLength...)
        } catch (InvalidKeyException
                 | NoSuchAlgorithmException
                 | SignatureException e) {
          fail(UNEXPECTED, e);
        } // end Catch (...)
      }); // end forEach(hash -> ...)
    }); // end forEach(keyLength -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#pkcs1RsaSsaPssSign(byte[], EafiHashAlgorithm)}. */
  @Test
  void test__pkcs1_RsaSsaPssSign__byteA_EafiHashAlgorithm() {
    // Assertions:
    // ... a. pkcs1_RsaSsa_Pss_Sign(byte[], EafiHashAlgorithm, int)-method works as expected

    // Test strategy:
    // --- a. smoke test

    final var prk = getPrk(MAX_LENGTH);
    final var dut =
        new RsaPrivateKeyImpl(prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
    final var puk = dut.getPublicKey();
    final var message = RNG.nextBytes(1024, 2048);

    try {
      // --- a. smoke test
      {
        final var hash = EafiHashAlgorithm.SHA_256;

        final var signature = dut.pkcs1RsaSsaPssSign(message, hash);

        final var algoName = "SHA256WithRSA/PSS";
        final var verifier = Signature.getInstance(algoName);
        final var algorithmParameterSpec = MGF1ParameterSpec.SHA256;
        verifier.setParameter(
            new PSSParameterSpec(
                hash.getAlgorithm(),
                "MGF1",
                algorithmParameterSpec,
                hash.getDigestLength(), // salt-length
                1));
        verifier.initVerify(puk);
        verifier.update(message);
        final var actual = verifier.verify(signature);

        assertTrue(actual);
      } // end --- a.
    } catch (InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | SignatureException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link RsaPrivateKeyImpl#pkcs1RsaSsaPssSign(byte[], EafiHashAlgorithm, int)}.
   */
  @Test
  void test__pkcs1_RsaSsaPssSign__byteA_EafiHashAlgorithm_int() {
    // Assertions:
    // ... a. pkcs1_RsaSsa_Pss_Sign(byte[], EafiHashAlgorithm, byte[])-method works as expected

    // Test strategy:
    // --- a. smoke test

    final var prk = getPrk(MAX_LENGTH);
    final var dut =
        new RsaPrivateKeyImpl(prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
    final var puk = dut.getPublicKey();
    final var message = RNG.nextBytes(1024, 2048);
    final var saltLength = 20;

    try {
      // --- a. smoke test
      {
        final var hash = EafiHashAlgorithm.SHA_256;

        final var signature = dut.pkcs1RsaSsaPssSign(message, hash, saltLength);

        final var algoName = "SHA256WithRSA/PSS";
        final var verifier = Signature.getInstance(algoName);
        final var algorithmParameterSpec = MGF1ParameterSpec.SHA256;
        verifier.setParameter(
            new PSSParameterSpec(
                hash.getAlgorithm(), "MGF1", algorithmParameterSpec, saltLength, 1));
        verifier.initVerify(puk);
        verifier.update(message);
        final var actual = verifier.verify(signature);

        assertTrue(actual);
      } // end --- a.
    } catch (InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | SignatureException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link RsaPrivateKeyImpl#pkcs1RsaSsaPssSign(byte[], EafiHashAlgorithm,
   * byte[])}.
   */
  @Test
  void test__pkcs1_RsaSsa_PssSign___byteA_EafiHashAlgorithm_byteA() {
    // Assertions:
    // ... a. sign_IsoIec9796_2_ds3(byte[], byte[], EafiHashAlgorithm, int, int, boolean)-method
    //        works as expected

    // Note: Because of assertion "a" this simple method does not need
    //       extensive testing. Thus, we just use the test vectors.

    // Test strategy:
    // --- a. apply test vectors
    {
      try {
        final Path path = Path.of("src", "test", "resources", "SigGenPSS_186-3.txt");

        assertTrue(Files.isRegularFile(path), () -> path.toAbsolutePath().normalize().toString());

        final List<String> lines =
            Files.readAllLines(path, StandardCharsets.US_ASCII).stream()
                .filter(i -> !i.startsWith("#")) // let non-comment-line pass
                .filter(
                    line -> { // let lines pass which contain non-whitespace characters
                      for (int index = line.length();
                          index-- > 0; ) { // NOPMD assignment in operand
                        if (!Character.isWhitespace(line.charAt(index))) {
                          return true;
                        } // end fi
                      } // end For (index...)

                      return false;
                    })
                .toList();

        // loop through lines from file SignGenPSS_186-3.txt
        RsaPrivateKeyImpl dut = new RsaPrivateKeyImpl(ONE, ZERO, TWO); // initialize DUT
        final String splitter = " = ";
        for (int index = 0; index < lines.size(); ) {
          final String line = lines.get(index++); // NOPMD reassignment of loop variable

          if (line.startsWith("n = ")) {
            final var n = new BigInteger(line.split(splitter)[1], 16); // NOPMD new in loop
            final var e = new BigInteger(lines.get(index++).split(splitter)[1], 16); // NOPMD
            final var d = new BigInteger(lines.get(index++).split(splitter)[1], 16); // NOPMD
            dut = new RsaPrivateKeyImpl(n, e, d); // NOPMD new in loop
          } else if (line.startsWith("SHAAlg = ")) {
            final String hashAlg = line.split(splitter)[1];
            // spotless:off
            final EafiHashAlgorithm hash = switch (hashAlg) {
              case "SHA-224" -> EafiHashAlgorithm.SHA_224;
              case "SHA-256" -> EafiHashAlgorithm.SHA_256;
              case "SHA-384" -> EafiHashAlgorithm.SHA_384;
              case "SHA-512" -> EafiHashAlgorithm.SHA_512;
              default -> fail(UNEXPECTED + ": " + hashAlg);
            }; // end Switch (hashAlg)
            // spotless:on
            final byte[] message =
                Hex.toByteArray(
                    lines.get(index++).split(splitter)[1]); // NOPMD reassignment of loop variable
            final byte[] sigExpexted =
                Hex.toByteArray(
                    lines.get(index++).split(splitter)[1]); // NOPMD reassignment of loop variable
            final byte[] salt =
                Hex.toByteArray(
                    lines.get(index++).split(splitter)[1]); // NOPMD reassignment of loop variable

            final byte[] sigPresent = dut.pkcs1RsaSsaPssSign(message, hash, salt);

            assertArrayEquals(sigExpexted, sigPresent);
          } // end else if
        } // end For (index...)
      } catch (IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#signIsoIec9796p2A4(BigInteger)}. */
  @Test
  void test__sign_IsoIec9796_2_A4__BigInteger() {
    // Assertions:
    // ... a. sign_IsoIec9796_2_A6(BigInteger)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: invalid trailer
    // --- c. check for minimum calculation

    final RSAPrivateCrtKey prk = getPrk(MIN_LENGTH); // small key-size for fast operations
    final var n = prk.getModulus();
    final var e = prk.getPublicExponent();
    final var d = prk.getPrivateExponent();
    final var dut = new RsaPrivateKeyImpl(n, e, d);

    // --- a. smoke test
    // --- b. ERROR: invalid trailer
    {
      final var a = new BigInteger(dut.getModulusLengthBit() - 5, RNG).shiftLeft(4);
      for (int i = 0; i < 16; i++) {
        final var dsi = a.add(BigInteger.valueOf(i));

        if (12 == i) { // NOPMD literal
          // ... low nibble equals ´1100´
          final var signature = dut.signIsoIec9796p2A4(dsi);
          final var other = n.subtract(signature);

          assertTrue(signature.compareTo(other) < 0);
        } else {
          // ... low nibble not ´1100´
          assertThrows(IllegalArgumentException.class, () -> dut.signIsoIec9796p2A6(dsi));
        } // end else
      } // end For (i...)
    } // end --- a, b.

    // --- c. check for minimum calculation
    {
      final var twelve = BigInteger.valueOf(12);
      var countOriginal = 10;
      var countOther = 10;

      do {
        final var dsi =
            new BigInteger(dut.getModulusLengthBit() - 5, RNG) // NOPMD new in loop
                .shiftLeft(4)
                .add(twelve);
        final var a4 = dut.signIsoIec9796p2A4(dsi);
        final var a6 = dut.signIsoIec9796p2A6(dsi);

        if (a4.equals(a6)) {
          countOriginal--;
        } else {
          countOther--;
        } // end else
      } while ((countOriginal > 0) || (countOther > 0));
    } // end --- c.
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#signIsoIec9796p2A6(BigInteger)}. */
  @Test
  void test__sign_IsoIec9796_2_A6__BigInteger() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: invalid trailer

    final RSAPrivateCrtKey prk = getPrk(MIN_LENGTH); // small key-size for fast operations
    final var n = prk.getModulus();
    final var e = prk.getPublicExponent();
    final var d = prk.getPrivateExponent();
    final var dut = new RsaPrivateKeyImpl(n, e, d);

    // --- a. smoke test
    // --- b. ERROR: invalid trailer
    {
      final var a = new BigInteger(dut.getModulusLengthBit() - 5, RNG).shiftLeft(4);
      for (int i = 0; i < 16; i++) {
        final var dsi = a.add(BigInteger.valueOf(i));

        if (12 == i) { // NOPMD literal
          // ... low nibble equals ´1100´
          final var actual = dut.signIsoIec9796p2A6(dsi);
          final var expected = dut.pkcs1RsaSp1(dsi);

          assertEquals(expected, actual);
        } else {
          // ... low nibble not ´1100´
          assertThrows(IllegalArgumentException.class, () -> dut.signIsoIec9796p2A6(dsi));
        } // end else
      } // end For (i...)
    } // end --- a, b.
  } // end method */

  /**
   * Test method for {@link RsaPrivateKeyImpl#signIsoIec9796p2ds1(byte[], EafiHashAlgorithm,
   * boolean)}.
   */
  @Test
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.NcssCount",
  })
  void test__sign_IsoIec9796_2_ds1__byteA_EafiHashAlgorithm_boolean() {
    // Assertions:
    // - none -

    // Note: Intentionally BouncyCastle is NOT used to verify signatures for the
    //       following reasons:
    //       1. BouncyCastle signature verification fails if the minimum is taken
    //          into account.
    //       2. BouncyCastle only handles keys with a multiple of 8 modulus lengths.

    // Test strategy:
    // --- a. smoke test with full recovery
    // --- b. smoke test with partial recovery
    // --- c. loop over various key-lengths
    // --- d. loop over various hash-algorithms
    // --- e. loop over all trailer lengths
    // --- f. loop over various message lengths
    // --- g. ERROR: capacity too small

    // --- a. smoke test with full recovery
    // a.1 Test vector from ISO/IEC 9796-2:2010 E.1.2.1
    // a.2 signature verified by RsaPublicKeyImpl

    // a.1 Test vector from ISO/IEC 9796-2:2010 E.1.2.1
    {
      final var hash = EafiHashAlgorithm.SHA_1;
      final var m = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopqopqrpqrs";
      final var message = m.getBytes(StandardCharsets.UTF_8);
      assertEquals(
          Hex.extractHexDigits(
              """
                  61626364 62636465 63646566 64656667 65666768 66676869 6768696A 68696A6B
                  696A6B6C 6A6B6C6D 6B6C6D6E 6C6D6E6F 6D6E6F70 6E6F7071 6F707172 70717273
                  """),
          Hex.toHexDigits(message));
      final var expected =
          Hex.extractHexDigits(
              """
                  24725B14 80D1ED93 54A210F5 08BBB528 0B718CCE AC8E1549 F1039362 6D59C8FE
                  CEF57483 D501C31D 8E5F8E81 6430C55C B263CF29 AA7D1C81 012DF0C8 431963E2
                  5A8789DB A6C8E211 00BD3E1C 7A769056 AC2A8530 8469FD1E ACDD68D4 997935C7
                  A543274E 2C025392 9D916618 E0DBCD30 0665CAEE 97CE6217 B00469BE 7ABE43C9
                  """);

      final var signature = PRK_9796_2_E1.signIsoIec9796p2ds1(message, hash, false);

      final var actual = Hex.toHexDigits(signature[0]);
      assertEquals(expected, actual);
      assertEquals(0, signature[1].length);
    } // end a.1

    // a.2 signature verified by RsaPublicKeyImpl
    {
      final var prk = getPrk(MIN_LENGTH);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
      final var puk = dut.getPublicKey();
      final var hash = EafiHashAlgorithm.SHA_1;
      final var message = RNG.nextBytes(16, 32);

      final var result = dut.signIsoIec9796p2ds1(message, hash, true);
      final var signature = result[0];
      final var m2 = result[1];
      assertEquals(0, m2.length);

      final var m1 = puk.verifyIsoIec9796p2ds1(signature, m2, hash);
      assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
    } // end a.2
    // end --- a.

    // --- b. smoke test with partial recovery
    // b.1 Test vector from ISO/IEC 9796-2:2010 E.1.3.1
    // b.2 signature verified by RsaPublicKeyImpl

    // b.1 Test vector from ISO/IEC 9796-2:2010 E.1.3.1
    {
      final var hash = EafiHashAlgorithm.RIPEMD_160;
      final var m1 = // m1
          Hex.toByteArray(
              """
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FEDC
                  """);
      final var expM2 =
          Hex.toByteArray(
              """
                  BA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98
                  """);
      final var expSignature =
          Hex.extractHexDigits(
              """
                  30CA91BB 8721F57A 8230CB13 FBC511F1 24345CA3 AD45E9AF FC0C848F 0DCD4F44
                  35D88226 B4D2A88B 70CAE7C9 371D7B1E 6588A454 467F8010 FB21C6DC 66FE6954
                  63B97DEE 36041B23 FFA24809 678C67DF DCAF8DC0 F5F75CF0 E677C528 EA80EF15
                  6E68953B 8892FB4E 0AD5EEBB 0632B9A7 B40DDD41 6E16A09C 842E6B9F 688D96F8
                  """);

      final var result =
          PRK_9796_2_E1.signIsoIec9796p2ds1(AfiUtils.concatenate(m1, expM2), hash, true);
      final var actSignature = result[0];
      final var actM2 = result[1];

      assertEquals(expSignature, Hex.toHexDigits(actSignature));
      assertArrayEquals(expM2, actM2);
    } // end b.1

    // b.2 signature verified by RsaPublicKeyImpl
    {
      final var prk = getPrk(MIN_LENGTH);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
      final var puk = dut.getPublicKey();
      final var hash = EafiHashAlgorithm.SHA_1;
      final var message = RNG.nextBytes(1024, 2048);

      final var result = dut.signIsoIec9796p2ds1(message, hash, true);
      final var signature = result[0];
      final var m2 = result[1];
      assertTrue(m2.length > 0);

      final var m1 = puk.verifyIsoIec9796p2ds1(signature, m2, hash);
      assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
    } // end b.2
    // end --- b.

    // --- c. loop over various key-lengths
    for (final var keyLength : IntStream.rangeClosed(MAX_LENGTH - 8, MAX_LENGTH).toArray()) {
      final var prk = getPrk(keyLength);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
      final var puk = dut.getPublicKey();
      final var k = dut.getModulusLengthBit();

      // --- d. loop over various hash-algorithms
      for (final var hash : EafiHashAlgorithm.values()) {
        final var lh = hash.getDigestLength() << 3; // length in bits

        // --- e. loop over all trailer lengths
        for (final var implicit : VALUES_BOOLEAN) {
          final var noPaddingBytes = implicit ? 1 : 2;
          if ((2 == noPaddingBytes) && NO_VALID_TRAILER.contains(hash)) {
            // ... no valid trailer for digest
            continue;
          } // end fi
          // ... valid trailer for digest

          final var c = k - lh - 8 * noPaddingBytes - 4; // capacity in bit
          final var delta = c % 8;
          final var cStar = c - delta;
          final var cOctet = cStar >> 3; // capacity in octet

          // --- f. loop over various message lengths
          final var mLengthValues =
              new LinkedHashSet<>(List.of(0, 1, cOctet - 1, cOctet, cOctet + 1, MEBI));
          for (final var mLength : mLengthValues) {
            final var message = RNG.nextBytes(mLength);

            final var result = dut.signIsoIec9796p2ds1(message, hash, implicit);
            final byte[] signature = result[0];
            final byte[] m2 = result[1];

            final var m1 = puk.verifyIsoIec9796p2ds1(signature, m2, hash);
            assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
          } // end For (mLength...)
        } // end For (noPaddingBytes...)
      } // end For (hash...)
    } // end For (keyLength...)
    // end --- c, d, e, f.

    // --- g. ERROR: capacity too small
    // g.1 capacity just sufficient
    // g.2 capacity is not sufficient
    {
      final var hash = EafiHashAlgorithm.SHA_512;
      final var keyLength = 11 + 8 * hash.getDigestLength() + 8;
      final var prk = getPrk(keyLength);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
      final var message = RNG.nextBytes(16, 32);

      // g.1 capacity just sufficient
      {
        final var result = dut.signIsoIec9796p2ds1(message, hash, true);
        final var signature = result[0];
        final var m2 = result[1];
        assertTrue(m2.length > 0);

        final var m1 = dut.getPublicKey().verifyIsoIec9796p2ds1(signature, m2, hash);
        assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
      } // end g.1

      // g.2 capacity is not sufficient
      {
        assertThrows(
            IllegalArgumentException.class, () -> dut.signIsoIec9796p2ds1(message, hash, false));
      } // end g.2
    } // end --- g.
  } // end method */

  /**
   * Test method for {@link RsaPrivateKeyImpl#signIsoIec9796p2ds2(byte[], int, EafiHashAlgorithm,
   * boolean, boolean )}.
   */
  @Test
  void test__sign_IsoIec9796_2_ds2__byteA_int_EafiHashAlgorithm_boolean_boolean() {
    // Assertions:
    // ... a. sign_IsoIec9796_2_ds2(byte[], byte[], EafiHashAlgorithm, boolean, boolean)-method
    //        works as expected

    // Test strategy:
    // --- a. smoke test with minimum building
    // --- b. smoke test without minimum building

    final var prk = getPrk(MAX_LENGTH);
    final var dut =
        new RsaPrivateKeyImpl(prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
    final var puk = dut.getPublicKey();
    final var hash = EafiHashAlgorithm.SHA_256;
    final var lengthSalt = hash.getDigestLength();

    // --- a. smoke test with minimum building
    {
      final var message = RNG.nextBytes(1024, 2048);

      final var result = dut.signIsoIec9796p2ds2(message, lengthSalt, hash, true, true);
      final var signature = result[0];
      final var m2 = result[1];

      final var m1 = puk.verifyIsoIec9796p2ds2(signature, m2, hash);
      assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
    } // end --- a.

    // --- b. smoke test without minimum building
    {
      final var engine = new RSAEngine();
      final var digest = new SHA256Digest();
      final var kpPuk = new RSAKeyParameters(false, puk.getModulus(), puk.getPublicExponent());
      final var verifier =
          new ISO9796d2PSSSigner(
              engine, digest, lengthSalt, true // implicit
              );
      verifier.init(false, kpPuk);

      for (int counter = 100; counter-- > 0; ) { // NOPMD assignment in operand
        final var message = RNG.nextBytes(1024, 2048);

        final var result = dut.signIsoIec9796p2ds2(message, lengthSalt, hash, true, false);
        final var signature = result[0];
        final var m2 = result[1];

        verifier.update(message, 0, message.length);
        final var flag = verifier.verifySignature(signature);
        final var m1 = verifier.getRecoveredMessage();
        assertTrue(flag);
        assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
      } // end For (counter...)
    } // end --- b.
  } // end method */

  /**
   * Test method for {@link RsaPrivateKeyImpl#signIsoIec9796p2ds2(byte[], byte[], EafiHashAlgorithm,
   * boolean, boolean)}.
   */
  @Test
  void test__sign_IsoIec9796_2_ds2__byteA_byteA_EafiHashAlgorithm_boolean_boolean() {
    // Assertions:
    // ... a. sign_IsoIec9796_2_ds3(byte[], byte[], EafiHashAlgorithm, int, boolean)-method
    //        works as expected

    // Test strategy:
    // --- a. smoke test with full recovery, test vector from ISO/IEC 9796-2:2010 E.1.2.2
    // --- b. smoke test with partial recovery, test vector from ISO/IEC 9796-2:2010 E.1.3.2
    // --- c. smoke test with minimum building
    // --- d. smoke test without minimum building
    // --- e. ERROR: salt too short

    // --- a. smoke test with full recovery, test vector from ISO/IEC 9796-2:2010 E.1.2.2
    {
      final var hash = EafiHashAlgorithm.RIPEMD_160;
      final var message =
          Hex.toByteArray(
              """
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FEDCBA98 76543210
                  """);
      final var salt = Hex.toByteArray("436BCA99 54EC376C 96B79C95 D4B82686 F3494AD3");
      final var expSignature =
          Hex.toByteArray(
              """
                  56136187 14871D42 EAA2CFFB DB9639BA 04ED8532 B4A20C4E 79CB2BA3 0ED58BB5
                  93E38E2C 9CBF7563 32CC26C4 B115F73C E1BEF204 252DAF73 708569E6 E3304E1F
                  F194E877 69800E73 10D31E4E 4AE53E2D 73FE0EDC C1B74AAB 6A64A808 CA3EDA35
                  2B0F86C0 A4ECC996 B8301BD9 8293B7BF 4063CD94 66091B74 39E3682A 53A58505
                  """);

      final var result = PRK_9796_2_E1.signIsoIec9796p2ds2(message, salt, hash, false, true);
      final var actSignature = result[0];
      final var actM2 = result[1];

      assertArrayEquals(expSignature, actSignature);
      assertEquals(0, actM2.length);
    } // end --- a.

    // --- b. smoke test with partial recovery, test vector from ISO/IEC 9796-2:2010 E.1.3.2
    {
      final var hash = EafiHashAlgorithm.SHA_1;
      final var m1 = // M1
          Hex.toByteArray(
              """
                  61626364 62636465 63646566 64656667 65666768 66676869 6768696A 68696A6B
                  696A6B6C 6A6B6C6D 6B6C6D6E 6C6D6E6F 6D6E6F70 6E6F7071 6F707172 70717273
                  71727374 72737475 73747576 74757677 75767778 7677
                  """);
      final var expM2 =
          Hex.toByteArray(
              """
                  7879 7778797A 78797A61 797A6162 7A616263 61626364 62636465
                  """);
      final var message = AfiUtils.concatenate(m1, expM2);
      final var salt = Hex.toByteArray("4C95C1B8 7A1DE8AC C193C14C F3147FE9 C6636078");
      final var expSignature =
          Hex.toByteArray(
              """
                  67FC4BB5 C6AF6CC1 B44E01A3 2E5910CA 3423F21B CE635C71 DB6E8FD3 4E012E16
                  8F8342A6 21C0DEB6 DD0FEE98 3F523E51 67A2DCDD 13FAE8B8 2C66322F 8953293C
                  0EB9CFD1 9EC8E3AA DFB97ECB C23D3EC8 A0E32377 E3F5754D E6B8839B E0C9F07E
                  37E61950 9DAF2E6E 0548754C FB5E3F19 92BE1220 E7ED9899 37261BF4 417434C3
                  """);

      final var result = PRK_9796_2_E1.signIsoIec9796p2ds2(message, salt, hash, true, true);
      final var actSignature = result[0];
      final var actM2 = result[1];

      assertArrayEquals(expSignature, actSignature);
      assertArrayEquals(expM2, actM2);
    } // end --- b.

    final var prk = getPrk(MAX_LENGTH);
    final var dut =
        new RsaPrivateKeyImpl(prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
    final var puk = dut.getPublicKey();
    final var hash = EafiHashAlgorithm.SHA_256;
    final var ls = hash.getDigestLength();

    // --- c. smoke test with minimum building
    {
      final var message = RNG.nextBytes(1024, 2048);

      final var result = dut.signIsoIec9796p2ds2(message, RNG.nextBytes(ls), hash, true, true);
      final var signature = result[0];
      final var m2 = result[1];

      final var m1 = puk.verifyIsoIec9796p2ds2(signature, m2, hash);
      assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
    } // end --- c.

    // --- d. smoke test without minimum building
    {
      final var engine = new RSAEngine();
      final var digest = new SHA256Digest();
      final var kpPuk = new RSAKeyParameters(false, puk.getModulus(), puk.getPublicExponent());
      final var verifier =
          new ISO9796d2PSSSigner(
              engine, digest, ls, true // implicit
              );
      verifier.init(false, kpPuk);

      for (int counter = 100; counter-- > 0; ) { // NOPMD assignment in operand
        final var message = RNG.nextBytes(1024, 2048);

        final var result = dut.signIsoIec9796p2ds2(message, RNG.nextBytes(ls), hash, true, false);
        final var signature = result[0];
        final var m2 = result[1];

        verifier.update(message, 0, message.length);
        final var flag = verifier.verifySignature(signature);
        final var m1 = verifier.getRecoveredMessage();
        assertTrue(flag);
        assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
      } // end For (counter...)
    } // end --- d.

    // --- e. ERROR: salt too short
    {
      final var message = RNG.nextBytes(1024, 2048);

      for (final var hashAlgo : EafiHashAlgorithm.values()) {
        for (final var impl : VALUES_BOOLEAN) {
          for (final var bool : VALUES_BOOLEAN) {
            assertThrows(
                IllegalArgumentException.class,
                () -> dut.signIsoIec9796p2ds2(message, AfiUtils.EMPTY_OS, hashAlgo, impl, bool));
          } // end For (bool...)
        } // end For (noPad...)
      } // end For (hashAlgo...)
    } // end --- e.
  } // end method */

  /**
   * Test method for {@link RsaPrivateKeyImpl#signIsoIec9796p2ds3(byte[], byte[], EafiHashAlgorithm,
   * boolean, boolean)}.
   */
  @Test
  void test__sign_IsoIec9796_2_ds3__byteA_byteA_EafiHashAlgorithm_boolean_boolean() {
    // Assertions:
    // ... a. sign_IsoIec9796_2_ds3(byte[], byte[], EafiHashAlgorithm, int, boolean, boolean)-method
    //        works as expected

    // Test strategy:
    // --- a. smoke test no salt, no  message, test vector from ISO/IEC 9796-2:2010 E.1.3.3
    // --- b. smoke test no salt, but message, zest vector from ISO/IEC 9796-2:2010 E.1.3.3
    // --- c. smoke test with salt

    // --- a. smoke test no salt, no  message, test vector from ISO/IEC 9796-2:2010 E.1.3.3
    {
      final var hash = EafiHashAlgorithm.SHA_1;
      final var salt = AfiUtils.EMPTY_OS;
      final var message = AfiUtils.EMPTY_OS;
      final var expSignature =
          Hex.toByteArray(
              """
                  00CB4DC1 F43D1E3B E55D0F7E 29258A26 4AF5F62B 3891429B EEDD0B46 E7F6B44C
                  3D60DC7C D984C57F 30257FCD 1489C17D B45EF390 3B5BC372 93242771 80395A2B
                  24B0470D AADAD8F2 69C2109B E547F928 66574C22 4E921274 6119C0D1 2725C73B
                  15CDCE63 BF8E3389 96AF4C06 D45ACC14 77A2317F 4F6B1310 55F64C3D CB88765E
                  """);

      final var result = PRK_9796_2_E1.signIsoIec9796p2ds3(message, salt, hash, true, true);
      final var actSignature = result[0];
      final var actM2 = result[1];

      assertArrayEquals(expSignature, actSignature);
      assertEquals(0, actM2.length);
    } // end --- a.

    // --- b. smoke test no salt, but message, test vector from ISO/IEC 9796-2:2010 E.1.3.3
    {
      final var hash = EafiHashAlgorithm.SHA_1;
      final var salt = AfiUtils.EMPTY_OS;
      final var m1 = // m1
          Hex.toByteArray(
              """
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FE
                  """);
      final var expM2 =
          Hex.toByteArray(
              """
                  DCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98
                  """);
      final var message = AfiUtils.concatenate(m1, expM2);
      final var expSignature =
          Hex.toByteArray(
              """
                  30147ECB 074705DD F33EF765 D0EE1017 D5535AB3 9A7727C4 D8D4DC42 42C693BD
                  1FB544EC AE2323D1 185BED05 C8AA5F69 9D3AAED4 1FC3ECF9 DF297A61 56D6BC86
                  5196A619 806E3FDF F8A8416D 2984EF9E 33940013 4A6D1712 2FCF0946 783AEBD4
                  6F11397E 66863E74 28F4542D E2AE8A30 7355633F 380F937B 308C149F 14194487
                  """);

      final var result = PRK_9796_2_E1.signIsoIec9796p2ds3(message, salt, hash, false, true);
      final var actSignature = result[0];
      final var actM2 = result[1];

      assertArrayEquals(expSignature, actSignature);
      assertArrayEquals(expM2, actM2);
    } // end --- b.

    // --- c. smoke test with salt
    {
      final var prk = getPrk(MAX_LENGTH);
      final var dut =
          new RsaPrivateKeyImpl(
              prk.getModulus(), prk.getPublicExponent(), prk.getPrivateExponent());
      final var puk = dut.getPublicKey();
      final var hash = EafiHashAlgorithm.SHA_256;
      final var salt = RNG.nextBytes(1, hash.getDigestLength());
      final var message = RNG.nextBytes(1024, 2048);

      final var result = dut.signIsoIec9796p2ds3(message, salt, hash, true, true);
      final var signature = result[0];
      final var m2 = result[1];

      final var m1 = puk.verifyIsoIec9796p2ds3(signature, m2, salt.length, hash);
      assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
    } // end --- c.
  } // end method */

  /**
   * Test method for {@link RsaPrivateKeyImpl#signIsoIec9796p2ds3(byte[], byte[], EafiHashAlgorithm,
   * int, boolean, boolean)}.
   */
  @Test
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  void test__sign_IsoIec9796_2_ds3__byteA_byteA_EafiHashAlgorithm_int_boolean_boolean() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test, test vector from ISO/IEC 9796-2:2010 E.1.3.3
    // --- b. loop over relevant range of input parameter
    // --- c. ERROR: m1 too long
    // --- d. ERROR: capacity too small

    // --- a. smoke test, test vector from ISO/IEC 9796-2:2010 E.1.3.3
    {
      final var hash = EafiHashAlgorithm.SHA_1;
      final var salt = AfiUtils.EMPTY_OS;
      final var m1 = // m1
          Hex.toByteArray(
              """
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                  FEDCBA98 76543210 FE
                  """);
      final var expM2 =
          Hex.toByteArray(
              """
                  DCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98
                  """);
      final var mes = AfiUtils.concatenate(m1, expM2);
      final var expSignature =
          Hex.toByteArray(
              """
                  30147ECB 074705DD F33EF765 D0EE1017 D5535AB3 9A7727C4 D8D4DC42 42C693BD
                  1FB544EC AE2323D1 185BED05 C8AA5F69 9D3AAED4 1FC3ECF9 DF297A61 56D6BC86
                  5196A619 806E3FDF F8A8416D 2984EF9E 33940013 4A6D1712 2FCF0946 783AEBD4
                  6F11397E 66863E74 28F4542D E2AE8A30 7355633F 380F937B 308C149F 14194487
                  """);

      final var result =
          PRK_9796_2_E1.signIsoIec9796p2ds3(mes, salt, hash, m1.length << 3, false, true);
      final var actSignature = result[0];
      final var actM2 = result[1];

      assertArrayEquals(expSignature, actSignature);
      assertArrayEquals(expM2, actM2);
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    // b.1 keyLength
    // b.2 hash-algorithm
    // b.3 implicit
    // b.4 salt-length
    // b.5 message-length
    // b.6 length M1
    // b.7 useMinimum = true
    // b.8 useMinimum = false
    IntStream.rangeClosed(MAX_LENGTH - 8, MAX_LENGTH)
        .forEach(
            keyLength -> {
              int countMinEven =
                  0; // no# of signatures with useMinimum = true where    sig    is used
              int countMinOdd =
                  0; // no# of signatures with useMinimum = true where (n - sig) is used
              int countSigBig = 0; // no# of signatures with useMinimum = false where sig > n - sig
              int countSigSmall =
                  0; // no# of signatures with useMinimum = false where sig < n - sig
              final var prk = getPrk(keyLength);
              final var n = prk.getModulus();
              final var dut =
                  new RsaPrivateKeyImpl(n, prk.getPublicExponent(), prk.getPrivateExponent());
              final var puk = dut.getPublicKey();
              final var k = dut.getModulusLengthBit();

              for (final var h : EafiHashAlgorithm.values()) { // b.2
                final var lh = h.getDigestLength() << 3; // length in bits

                for (final var imp : VALUES_BOOLEAN) { // b.3
                  final var noPadBytes = imp ? 1 : 2;
                  final var lsSupremum = (k - lh - 8 * noPadBytes - 2 - 7) >> 3;

                  for (final var ls : RNG.intsClosed(0, lsSupremum, 5).toArray()) { // b.4
                    final var m1S = (k - lh - 8 * ls - 8 * noPadBytes - 2) >> 3; // m1 supremum

                    for (final var l :
                        List.of(0, 1, Math.max(0, m1S - 1), m1S, m1S + 1, MEBI)) { // b.5

                      for (final var lenM1 : RNG.intsClosed(0, m1S, 5).toArray()) { // b.6
                        // b.7 useMinimum = true
                        {
                          final var s = RNG.nextBytes(ls);
                          final var m = RNG.nextBytes(l);

                          final var result =
                              dut.signIsoIec9796p2ds3(m, s, h, lenM1 << 3, imp, true);
                          final var sig = result[0];
                          final var m2 = result[1];

                          final var m1 = puk.verifyIsoIec9796p2ds3(sig, m2, ls, h);
                          assertArrayEquals(m, AfiUtils.concatenate(m1, m2));
                          final var iSig = new BigInteger(1, sig); // NOPMD new in loop
                          final var nSig = n.subtract(iSig);
                          assertTrue(iSig.compareTo(nSig) < 0);
                          final var dsi = puk.pkcs1RsaVp1(iSig);
                          if (dsi.testBit(0)) {
                            countMinOdd++;
                          } else {
                            countMinEven++;
                          } // end else
                        } // end b.7

                        // b.8 useMinimum = false
                        {
                          final var s = RNG.nextBytes(ls);
                          final var m = RNG.nextBytes(l);

                          final var result =
                              dut.signIsoIec9796p2ds3(m, s, h, lenM1 << 3, imp, false);
                          final var sig = result[0];
                          final var m2 = result[1];

                          final var m1 = puk.verifyIsoIec9796p2ds3(sig, m2, ls, h);
                          assertArrayEquals(m, AfiUtils.concatenate(m1, m2));
                          final var iSig = new BigInteger(1, sig); // NOPMD new in loop
                          final var nSig = n.subtract(iSig);
                          final var dsi = puk.pkcs1RsaVp1(iSig);
                          assertFalse(dsi.testBit(0));
                          if (iSig.compareTo(nSig) < 0) {
                            countSigSmall++;
                          } else {
                            countSigBig++;
                          } // end else
                        } // end b.8
                      } // end For (lenM1...)

                      // --- c. ERROR: m1 too long
                      {
                        final var s = RNG.nextBytes(ls);
                        final var m = RNG.nextBytes(1024);

                        final var throwable =
                            assertThrows(
                                IllegalArgumentException.class,
                                () -> dut.signIsoIec9796p2ds3(m, s, h, (m1S + 1) << 3, imp, true));
                        assertEquals("M1 too long", throwable.getMessage());
                      } // end --- c.
                    } // end For (mLen...)
                  } // end For (ls...)

                  // --- d. ERROR: capacity too small
                  {
                    final var s = RNG.nextBytes(lsSupremum + 1);
                    final var m = RNG.nextBytes(1024);

                    final var throwable =
                        assertThrows(
                            IllegalArgumentException.class,
                            () -> dut.signIsoIec9796p2ds3(m, s, h, 0, imp, true));
                    assertEquals("capacity too small", throwable.getMessage());
                  } // end --- d.
                } // end For (imp...)
              } // end For (hash...)

              LOGGER.atInfo().log(
                  "k = {}, cme = {}, cmo = {}, css = {}, csb = {}",
                  k,
                  countMinEven,
                  countMinOdd,
                  countSigSmall,
                  countSigBig);

              assertTrue(countMinEven > 0, "countMinEven  = " + countMinEven);
              assertTrue(countMinOdd > 0, "countMinOdd   = " + countMinOdd);
              assertTrue(countSigSmall > 0, "countSigSmall = " + countSigSmall);
              assertTrue(countSigBig > 0, "countSigBig   = " + countSigBig);
            }); // end forEach(keyLength -> ...)
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. superclass works as expected
    // ... b. RsaPrivateBigInteger, BigInteger, BigInteger)-constructor works as expected
    // ... c. getPrivateExponent()-method works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed, small input
    // --- b. smoke test with a fixed, huge input

    // --- a. smoke test with a fixed, small input
    {
      final var n = BigInteger.valueOf(0x12345678);
      final var e = BigInteger.valueOf(0x42);
      final var d = BigInteger.valueOf(0x34567);
      final var dut = new RsaPrivateKeyImpl(n, e, d);
      final var expected = String.format("n  = 12345678%ne  =       42%nd  =   034567");

      final var actual = dut.toString();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. smoke test with a fixed, huge input
    {
      final var n = ONE.shiftLeft(259);
      final var e = BigInteger.valueOf(0x11);
      final var d = ONE.shiftLeft(240).subtract(ONE);
      final var dut = new RsaPrivateKeyImpl(n, e, d);
      final var expected =
          String.format(
              "n  = %s%ne  = %s%nd  =       %s",
              AfiBigInteger.toHex(n), AfiBigInteger.toHex(e), AfiBigInteger.toHex(d));

      final var actual = dut.toString();

      assertEquals(expected, actual);
    } // end --- b.
  } // end method */

  /**
   * Test method for various methods.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link RsaPrivateKeyImpl#getModulus()}
   *   <li>{@link RsaPrivateKeyImpl#getModulusLengthBit()}
   *   <li>{@link RsaPrivateKeyImpl#getModulusLengthOctet()}
   *   <li>{@link RsaPrivateKeyImpl#getPublicExponent()}
   *   <li>{@link RsaPrivateKeyImpl#getPrivateExponent()}
   *   <li>{@link RsaPrivateKeyImpl#getPublicKey()}
   * </ol>
   */
  @Test
  @SuppressWarnings({"PMD.NcssCount"})
  void test_getter() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter

    // --- a. smoke test
    {
      final var n = BigInteger.valueOf(0x1234);
      final var e = BigInteger.valueOf(3);
      final var d = BigInteger.valueOf(0x0325);
      final var dut = new RsaPrivateKeyImpl(n, e, d);

      assertSame(n, dut.getModulus());
      assertEquals(13, dut.getModulusLengthBit());
      assertEquals(2, dut.getModulusLengthOctet());
      assertSame(e, dut.getPublicExponent());
      assertSame(d, dut.getPrivateExponent());

      final var puk = dut.getPublicKey();

      assertSame(n, puk.getModulus());
      assertSame(e, puk.getPublicExponent());
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    {
      final var e = BigInteger.valueOf(3);
      final var d = BigInteger.valueOf(0x0325);
      var n = BigInteger.valueOf(0x1234);
      var dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(13, dut.getModulusLengthBit());
      assertEquals(2, dut.getModulusLengthOctet());

      // shift 1
      n = n.shiftLeft(1);
      dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(14, dut.getModulusLengthBit());
      assertEquals(2, dut.getModulusLengthOctet());

      // shift 2
      n = n.shiftLeft(1);
      dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(15, dut.getModulusLengthBit());
      assertEquals(2, dut.getModulusLengthOctet());

      // shift 3
      n = n.shiftLeft(1);
      dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(16, dut.getModulusLengthBit());
      assertEquals(2, dut.getModulusLengthOctet());

      // shift 4
      n = n.shiftLeft(1);
      dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(17, dut.getModulusLengthBit());
      assertEquals(3, dut.getModulusLengthOctet());

      // shift 5
      n = n.shiftLeft(1);
      dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(18, dut.getModulusLengthBit());
      assertEquals(3, dut.getModulusLengthOctet());

      // shift 6
      n = n.shiftLeft(1);
      dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(19, dut.getModulusLengthBit());
      assertEquals(3, dut.getModulusLengthOctet());

      // shift 7
      n = n.shiftLeft(1);
      dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(20, dut.getModulusLengthBit());
      assertEquals(3, dut.getModulusLengthOctet());

      // shift 8
      n = n.shiftLeft(1);
      dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(21, dut.getModulusLengthBit());
      assertEquals(3, dut.getModulusLengthOctet());

      // shift 9
      n = n.shiftLeft(1);
      dut = new RsaPrivateKeyImpl(n, e, d);
      assertSame(n, dut.getModulus());
      assertEquals(22, dut.getModulusLengthBit());
      assertEquals(3, dut.getModulusLengthOctet());
    } // end --- b.
  } // end method */

  /** Test method for {@link RsaPrivateKeyImpl#getPrivateExponent()}. */
  @Test
  void test_getPrivateExponent() {
    // Assertions:
    // ... a. superclass works as expected
    // ... b. RsaPrivateBigInteger, BigInteger, BigInteger)-constructor works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final var n = BigInteger.valueOf(47);
      final var e = BigInteger.valueOf(11);
      final var d = BigInteger.valueOf(18);

      final var dut = new RsaPrivateKeyImpl(n, e, d);

      assertSame(d, dut.getPrivateExponent());
    } // end --- a.
  } // end method */
} // end class
