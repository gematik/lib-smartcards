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

import static de.gematik.smartcards.utils.AfiUtils.MEBI;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static de.gematik.smartcards.utils.AfiUtils.VALUES_BOOLEAN;
import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
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
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.signers.ISO9796d2PSSSigner;
import org.bouncycastle.crypto.signers.ISO9796d2Signer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link RsaPublicKeyImpl}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.CyclomaticComplexity",
  "PMD.ExcessiveImports",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname",
})
final class TestRsaPublicKeyImpl extends RsaKeyRepository {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestRsaPublicKeyImpl.class); // */

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

  /** Test method for {@link RsaPublicKeyImpl#RsaPublicKeyImpl(BigInteger, BigInteger)}. */
  @Test
  void test_RsaPublicKeyImpl__BigInteger_BigInteger() {
    // Assertions:
    // ... a. getModulus()-method works as expected
    // ... b. getPublicExponent()-method works as expected

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    // --- b. some random input

    // --- a. smoke test with a fixed input
    {
      final BigInteger n = BigInteger.valueOf(15);
      final BigInteger e = BigInteger.valueOf(7);

      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

      assertSame(n, dut.getModulus());
      assertSame(e, dut.getPublicExponent());
    } // end --- a.

    // --- b. some random input
    IntStream.rangeClosed(0, 20)
        .forEach(
            i -> {
              final BigInteger n = new BigInteger(2048, RNG);
              final BigInteger e = BigInteger.valueOf(RNG.nextLong());

              final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

              assertSame(n, dut.getModulus());
              assertSame(e, dut.getPublicExponent());
            }); // end forEach(i -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#RsaPublicKeyImpl(RSAPublicKey)}. */
  @Test
  void test_RsaPublicKeyImpl__RsaPublicKey() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a randomly generated key

    final var puk = getPuk(MIN_LENGTH);

    // --- a. smoke test with a randomly generated key
    {
      final var dut = new RsaPublicKeyImpl(puk);

      assertSame(puk.getModulus(), dut.getModulus());
      assertSame(puk.getPublicExponent(), dut.getPublicExponent());
    } // end --- a.
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#RsaPublicKeyImpl(ConstructedBerTlv, EafiRsaPukFormat)}.
   */
  @Test
  void test_RsaPublicKeyImpl__ConstructedBerTlv_EafiRsaPukFormat() {
    // Assertions:
    // ... a. extract(ConstructedBerTlv, EafiRsaPukFormat)-method works as expected
    // ... b. getter work as expected

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final ConstructedBerTlv tlv =
          (ConstructedBerTlv)
              BerTlv.getInstance(
                  """
                      30-1a
                         30-0d
                            06-09-2a864886f70d010101
                            05-00
                         03-09-00 {30-06  [(02-01-1b) (02-01-11)]}
                      """);
      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(tlv, EafiRsaPukFormat.RFC5280);

      assertEquals(BigInteger.valueOf(27), dut.getModulus());
      assertEquals(BigInteger.valueOf(17), dut.getPublicExponent());
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#extract(ConstructedBerTlv, EafiRsaPukFormat)}. */
  @Test
  void test_extract__ConstructedBerTlv_EafiRsaPukFormat() {
    // Assetions:
    // - none -

    // Test strategy:
    // --- a. check for ISO7816
    // --- b. check for PKCS#1
    // --- c. check for RFC5280

    // --- a. check for ISO7816
    assertTrue(zzTestExtractConstructedBerTlvIso7816());

    // --- b. check for PKCS#1
    assertTrue(zzTestExtractConstructedBerTlvPkcs1());

    // --- c. check for RFC5280
    assertTrue(zzTestExtractConstructedBerTlvRfc5280());
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#equals(Object)}. */
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
    // --- f. different objects, but same content

    final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(BigInteger.TEN, BigInteger.TWO);

    for (final Object obj :
        new Object[] {
          dut, // --- a. same reference
          null, // --- b. null input
          "afi" // --- c. difference in type
        }) {
      assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals to compare object reference
    } // end For (obj...)

    // --- d. difference in modulus
    // --- e. difference in public exponent
    // --- f. different objects, but same content
    // spotless:off
    Set.of(dut.getModulus(), ONE).forEach(modulus ->
        Set.of(dut.getPublicExponent(), ZERO).forEach(publicExponent -> {
          final Object obj = new RsaPublicKeyImpl(modulus, publicExponent);

          assertNotSame(obj, dut);
          assertEquals(
              modulus.equals(dut.getModulus())
                  && publicExponent.equals(dut.getPublicExponent()),
              dut.equals(obj));
        }) // end forEach(publicExponent -> ...)
    ); // end forEach(modulus -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#getAlgorithm()}. */
  @Test
  void test_getAlgorithm() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input

    final var expected = getPuk(MIN_LENGTH).getAlgorithm();

    // --- a. smoke test with a fixed input
    {
      final BigInteger n = BigInteger.valueOf(64);
      final BigInteger e = BigInteger.valueOf(12);
      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

      final var actual = dut.getAlgorithm();

      assertEquals("RSA", actual);
      assertEquals(expected, actual);
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#getEncoded()}. */
  @Test
  void test_getEncoded() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected
    // ... b. getEncoded(EafiRsaPukFormat)-method works as expected for RFC5280

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final BigInteger n = BigInteger.valueOf(255);
      final BigInteger e = BigInteger.valueOf(7);

      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

      assertEquals(
          Hex.extractHexDigits(
              """
                  30 1b
                  |  30 0d
                  |  |  06 09 2a864886f70d010101
                  |  |  05 00
                  |  03 0a 00-30-07-[(02-02-00ff)(02-01-07)]
                  """),
          Hex.toHexDigits(dut.getEncoded()));
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#getEncoded(EafiRsaPukFormat)}. */
  @Test
  void test_getEncoded__EafiRsaPukFormat() {
    // Assetions:
    // - none -

    // Test strategy:
    // --- a. check for ISO7816
    // --- b. check for PKCS#1
    // --- c. check for RFC5280

    // --- a. check for ISO7816
    assertTrue(zzTestGetEncodedIso7816());

    // --- b. check for PKCS#1
    assertTrue(zzTestGetEncodedPkcs1());

    // --- c. check for RFC5280
    assertTrue(zzTestGetEncodedRfc5280());
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#getFormat()}. */
  @Test
  void test_getFormat() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    KEY_PAIR_GENERATOR.initialize(MIN_LENGTH); // small key-size for fast operations
    final var expected = getPuk(MIN_LENGTH).getFormat();

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final var n = BigInteger.valueOf(64);
      final var e = BigInteger.valueOf(12);
      final var dut = new RsaPublicKeyImpl(n, e);

      final var actual = dut.getFormat();

      assertEquals("X.509", actual);
      assertEquals(expected, actual);
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#getTrailer(BigInteger, EafiHashAlgorithm)}. */
  @Test
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  void test_getTrailer__BigInteger_EafiHashAlgorithm() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. one octet trailer
    // --- c. all valid two-octet trailers
    // --- d. ERROR: invalid hash-identifier
    // --- e. ERROR: mismatch between hash and trailer
    // --- f. ERROR: invalid trailer

    // --- a. smoke test
    {
      final var hash = EafiHashAlgorithm.SHA_256;
      final var expected = (hash.getHashId() << 8) + 0xcc;
      final var messRep = BigInteger.valueOf(((RNG.nextLong() & 0x7fffffffffffL) << 16) + expected);

      final var actual = RsaPublicKeyImpl.getTrailer(messRep, hash);

      assertEquals(expected, actual);
    } // end --- a.

    final var map =
        Arrays.stream(EafiHashAlgorithm.values())
            .collect(Collectors.toMap(EafiHashAlgorithm::getHashId, Function.identity()));

    for (int i = 0; i <= 0xff; i++) {
      for (int j = 0; j <= 0xff; j++) {
        final var expected = (i << 8) + j;
        final var messRep =
            BigInteger.valueOf(((RNG.nextLong() & 0x7fffffffffffL) << 16) + expected);

        if (0xbc == j) { // NOPMD literal in if statement
          // --- b. one-octet trailer
          for (final var hash : EafiHashAlgorithm.values()) {
            final var actual = RsaPublicKeyImpl.getTrailer(messRep, hash);

            assertEquals(j, actual);
          } // end For (hash...)
          // end --- b.
        } else if (0xcc == j) { // NOPMD literal in if statement
          final var hash = map.get(i);

          if (null == hash) {
            // --- d. ERROR: invalid hash-identifier
            Arrays.stream(EafiHashAlgorithm.values())
                .forEach(
                    hashIn ->
                        assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                RsaPublicKeyImpl.getTrailer(
                                    messRep, hashIn))); // end forEach(hashIn -> ...)
          } else {
            Arrays.stream(EafiHashAlgorithm.values())
                .forEach(
                    hashIn -> {
                      if (hashIn == hash) {
                        // --- c. all valid two-octet trailers
                        final var actual = RsaPublicKeyImpl.getTrailer(messRep, hash);

                        assertEquals(expected, actual);
                      } else {
                        // --- e. ERROR: mismatch between hash and trailer
                        assertThrows(
                            IllegalArgumentException.class,
                            () -> RsaPublicKeyImpl.getTrailer(messRep, hashIn));
                      } // end else
                    }); // end forEach(hashIn -> ...)
          } // end else
        } else {
          // --- f. ERROR: invalid trailer
          Arrays.stream(EafiHashAlgorithm.values())
              .forEach(
                  hashIn ->
                      assertThrows(
                          IllegalArgumentException.class,
                          () -> RsaPublicKeyImpl.getTrailer(messRep, hashIn)));
        } // end else
      } // end For (j...)
    } // end For (i...)
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#hashCode()}. */
  @Test
  void test_hashCode() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. call hashCode()-method again

    final BigInteger n = new BigInteger(1024, RNG);
    final BigInteger e = new BigInteger(128, RNG);
    final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

    final int hashCode = dut.hashCode();

    // --- a. smoke test
    {
      assertEquals(n.hashCode() * 31 + e.hashCode(), hashCode);
    } // end --- a.

    // --- b. call hashCode()-method again
    {
      assertEquals(hashCode, dut.hashCode());
    } // end --- b.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#pkcs1RsaEp(BigInteger)}. */
  @Test
  void test__pkcs1_RsaEp___BigInteger() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter(s) work as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. border tests
    // --- c. ERROR: m out of range

    final var dut = new RsaPublicKeyImpl(getPuk(MIN_LENGTH));
    final var n = dut.getModulus();
    final var e = dut.getPublicExponent();
    final var infimum = ZERO;
    final var supremum = n.subtract(ONE);

    // --- a. smoke test
    {
      final var m = new BigInteger(dut.getModulusLengthBit() - 1, RNG);
      final var expected = m.modPow(dut.getPublicExponent(), n);

      final var actual = dut.pkcs1RsaEp(m);

      assertEquals(expected, actual);
    } // end method */

    // --- b. border tests
    Set.of(infimum, supremum)
        .forEach(
            m -> {
              final var expected = m.modPow(e, n);

              final var actual = dut.pkcs1RsaEp(m);

              assertEquals(expected, actual);
            }); // end forEach(m -> ...)

    // --- c. ERROR: m out of range
    Set.of(infimum.subtract(ONE), supremum.add(ONE))
        .forEach(
            m ->
                assertThrows(
                    IllegalArgumentException.class,
                    () -> dut.pkcs1RsaEp(m))); // end forEach(m -> ...)
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#pkcs1RsaEsOaepEncrypt(byte[], EafiHashAlgorithm)}. */
  @Test
  void test__pkcs1_RsaEs_Oaep_Encrypt__byteA_EafiHashAlgorithm() {
    // Assertions:
    // ... a. pkcs1_RsaEs_Oaep_Encrypt(byte[], byte[], EafiHashAlgorithm)-method
    //        works as expected

    // Test strategy:
    // --- a. smoke test

    final var keyLength = MAX_LENGTH;
    final var prk = getPrk(keyLength);
    final var dut = new RsaPublicKeyImpl(getPuk(keyLength));

    // --- a. smoke test
    try {
      final var hash = EafiHashAlgorithm.SHA_256;
      final var message = RNG.nextBytes(16, 64);

      final var ciphertext = dut.pkcs1RsaEsOaepEncrypt(message, hash);

      final var cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");
      final var oaepParams =
          new OAEPParameterSpec(
              hash.getAlgorithm(),
              "MGF1",
              new MGF1ParameterSpec(hash.getAlgorithm()),
              PSource.PSpecified.DEFAULT);
      cipher.init(Cipher.DECRYPT_MODE, prk, oaepParams);
      final var plaintext = cipher.doFinal(ciphertext);
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
    // end --- a.
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#pkcs1RsaEsOaepEncrypt(byte[], byte[],
   * EafiHashAlgorithm)}.
   */
  @Test
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  void test__pkcs1_RsaEs_Oaep_Encrypt__byteA_byteA_EafiHashAlgorithm() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with manually chosen parameter
    // --- b. loop over relevant range of input parameter
    // --- c. ERROR: message too long

    try {
      final var cipher = Cipher.getInstance("RSA/ECB/OAEPPadding");

      // --- a. smoke test with manually chosen parameter
      {
        final var keyLength = MAX_LENGTH;
        final var prk = getPrk(keyLength);
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        final var hash = EafiHashAlgorithm.SHA_256;
        final var message = RNG.nextBytes(16, 64);
        final var label = RNG.nextBytes(16, 64);

        final var ciphertext = dut.pkcs1RsaEsOaepEncrypt(message, label, hash);

        final var oaepParams =
            new OAEPParameterSpec(
                hash.getAlgorithm(),
                "MGF1",
                new MGF1ParameterSpec(hash.getAlgorithm()),
                new PSource.PSpecified(label));
        cipher.init(Cipher.DECRYPT_MODE, prk, oaepParams);
        final var plaintext = cipher.doFinal(ciphertext);
        final var expected = Hex.toHexDigits(message);
        final var actual = Hex.toHexDigits(plaintext);
        assertEquals(expected, actual);
      } // end --- a.

      // --- b. loop over relevant range of input parameter
      for (final var keyLength : IntStream.rangeClosed(MAX_LENGTH - 12, MAX_LENGTH).toArray()) {
        final var prk = getPrk(keyLength);
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        for (final var hash : EafiHashAlgorithm.values()) {
          final var supremumMessageLength =
              dut.getModulusLengthOctet() - 2 * hash.getDigestLength() - 2;
          for (final var mLength : RNG.intsClosed(1, supremumMessageLength, 5).toArray()) {
            final var message = RNG.nextBytes(mLength);
            final var label = RNG.nextBytes(1, mLength);

            final var ciphertext = dut.pkcs1RsaEsOaepEncrypt(message, label, hash);

            final var oaepParams =
                new OAEPParameterSpec(
                    hash.getAlgorithm(),
                    "MGF1",
                    new MGF1ParameterSpec(hash.getAlgorithm()),
                    new PSource.PSpecified(label));
            cipher.init(Cipher.DECRYPT_MODE, prk, oaepParams);
            final var plaintext = cipher.doFinal(ciphertext);
            final var expected = Hex.toHexDigits(message);
            final var actual = Hex.toHexDigits(plaintext);
            assertEquals(
                expected,
                actual,
                () -> String.format("ln=%d, %s, mL=%d", keyLength, hash.getAlgorithm(), mLength));
          } // end For (mLength...)

          // --- c. ERROR: message too long
          {
            final var message = RNG.nextBytes(supremumMessageLength + 1);
            final var label = AfiUtils.EMPTY_OS;

            assertThrows(
                IllegalArgumentException.class,
                () -> dut.pkcs1RsaEsOaepEncrypt(message, label, hash));
          } // end --- c.
        } // end For (hash...)
      } // end For (keyLength...)
      // end --- b, c.
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#pkcs1RsaEsPkcs1V15Encrypt(byte[])}. */
  @Test
  void test__pkcs1_RsaEs_Pkcs1_V1_5_Encrypt__byteA() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter work as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. increment key-length and loop over all valid message.lengths
    // --- c. ERROR: message too long

    // --- a. smoke test
    try {
      KEY_PAIR_GENERATOR.initialize(MIN_LENGTH);
      final Cipher decrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      final KeyPair keyPair = KEY_PAIR_GENERATOR.generateKeyPair();
      final RSAPrivateCrtKey prk = (RSAPrivateCrtKey) keyPair.getPrivate();
      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl((RSAPublicKey) keyPair.getPublic());
      final int mLen = 5;
      final byte[] message = RNG.nextBytes(mLen);
      final byte[] cipher = dut.pkcs1RsaEsPkcs1V15Encrypt(message);

      decrypt.init(Cipher.DECRYPT_MODE, prk);
      final byte[] plainText = decrypt.doFinal(cipher);

      assertArrayEquals(message, plainText);
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.

    // --- b. increment key-length and loop over all valid message.lengths
    try {
      final Cipher decrypt = Cipher.getInstance("RSA/ECB/PKCS1Padding");

      IntStream.rangeClosed(MIN_LENGTH, MIN_LENGTH + 16)
          .forEach(
              keyLength -> {
                try {
                  final RSAPrivateCrtKey prk = getPrk(keyLength);
                  final RsaPublicKeyImpl dut =
                      new RsaPublicKeyImpl(prk.getModulus(), prk.getPublicExponent());
                  final int supremumLength = dut.getModulusLengthOctet() - 11;

                  for (final int mLen : RNG.intsClosed(0, supremumLength, 5).toArray()) {
                    final byte[] message = RNG.nextBytes(mLen);
                    final byte[] cipher = dut.pkcs1RsaEsPkcs1V15Encrypt(message);

                    decrypt.init(Cipher.DECRYPT_MODE, prk);
                    final byte[] plainText = decrypt.doFinal(cipher);

                    assertArrayEquals(message, plainText);
                  } // end For (mLen...)

                  // --- c. ERROR: message too long
                  {
                    final int mLen = supremumLength + 1;
                    final byte[] message = RNG.nextBytes(mLen);

                    assertThrows(
                        IllegalArgumentException.class,
                        () -> dut.pkcs1RsaEsPkcs1V15Encrypt(message));
                  } // end --- c.
                } catch (BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
                  fail(UNEXPECTED, e);
                } // end Catch (...)
              }); // end forEach(keyLength -> ...)
    } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- b.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#pkcs1RsaSsaPkcs1V15Verify(byte[], byte[])}. */
  @Test
  void test__pkcs1_RsaSsa_Pkcs1_v1_5_Verify___byteA_byteA() {
    // Assertions:
    // ... a. pkcs1_RsaVp1(BigInteger)-method works as expected

    // Test strategy:
    // --- a. smoke test with examples from ftp://ftp.rsa.com/pub/pkcs/ascii/examples.asc
    // --- b. loop over relevant range of input parameter
    // --- c. ERROR: invalid signature content
    // --- d. ERROR: wrong DSI
    // --- e. ERROR: signature has wrong length

    // --- a. smoke test with examples from ftp://ftp.rsa.com/pub/pkcs/ascii/examples.asc
    {
      final RsaPrivateCrtKeyImpl prk =
          new RsaPrivateCrtKeyImpl(
              new BigInteger(
                  "33d48445c859e52340de704bcdda065fbb4058d740bd1d67d29e9c146c11cf61", 16),
              new BigInteger(
                  "335e8408866b0fd38dc7002d3f972c67389a65d5d8306566d5c4f2a5aa52628b", 16),
              new BigInteger("010001", 16));
      final RsaPublicKeyImpl dut = prk.getPublicKey();

      // example 1:
      {
        final var message =
            Hex.toByteArray(
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
                    """);
        final byte[] signature =
            Hex.toByteArray(
                """
                    06db36cb18d3475b9c01db3c789528080279bbaeff2b7d558ed6615987c85186
                    3f8a6c2cffbc89c3f75a18d96b127c717d54d0d8048da8a0544626d17a2a8fbe
                    """);

        assertEquals(EafiHashAlgorithm.MD2, dut.pkcs1RsaSsaPkcs1V15Verify(message, signature));
      } // end example 1

      // example 2:
      {
        final byte[] message =
            Hex.toByteArray("45766572796f6e65206765747320467269646179206f66662e");
        final byte[] signature =
            Hex.toByteArray(
                """
                    05fa6a812fc7df8bf4f2542509e03e846e11b9c620be2009efb440efbcc66921
                    6994ac04f341b57d05202d428fb2a27b5c77dfd9b15bfc3d559353503410c1e1
                    """);

        assertEquals(EafiHashAlgorithm.MD2, dut.pkcs1RsaSsaPkcs1V15Verify(message, signature));
      } // end example 2
    } // end --- a.

    // --- b. loop over the relevant range of input parameter
    // Note: We need at least a 744 bit key for SHA512WithRSA signatures.
    // spotless:off
    IntStream.rangeClosed(768, 768 + 16).forEach(keyLength -> {
      final RSAPrivateCrtKey prk = getPrk(keyLength);
      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(getPuk(keyLength));

      Arrays.stream(EafiHashAlgorithm.values()).forEach(hash -> {
        try {
          final var algoName = switch (hash) {
            case SHA_1 -> "SHA1";
            case SHA_224 -> "SHA224";
            case SHA_256 -> "SHA256";
            case SHA_384 -> "SHA384";
            case SHA_512 -> "SHA512";
            default -> hash.getAlgorithm();
          } + "WithRSA";
          final var signer = Signature.getInstance(algoName, BC);
          signer.initSign(prk);

          for (final var messageLength : RNG.intsClosed(0, 1024, 5).toArray()) {
            final var message = RNG.nextBytes(messageLength);
            signer.update(message);
            final byte[] signature = signer.sign();

            assertEquals(hash, dut.pkcs1RsaSsaPkcs1V15Verify(message, signature));

            // --- c. ERROR: invalid signature content
            {
              final var sigWrong = signature.clone();
              sigWrong[0]++;
              assertThrows(
                  IllegalArgumentException.class,
                  () -> dut.pkcs1RsaSsaPkcs1V15Verify(message, sigWrong));
            } // end --- c.

            // --- d. ERROR: wrong DSI
            {
              final var messageWrong = Arrays.copyOfRange(message, 0, messageLength + 1);
              assertThrows(
                  IllegalArgumentException.class,
                  () -> dut.pkcs1RsaSsaPkcs1V15Verify(messageWrong, signature));
            } // end --- d.
          } // end For (messageLength...)

          // --- e. ERROR: signature has wrong length
          {
            // Note: Hereafter we loop until a leading '00' octet occurs,
            //       which is then snipped away. Seen as a BigInteger, that
            //       does not change the signature, but SHALL lead to a
            //       verification error.
            int counter = 0;
            for (; true; counter++) {
              final byte[] message = RNG.nextBytes(1, 1024);
              signer.update(message);
              final byte[] signature = signer.sign();

              // signature ok
              assertDoesNotThrow(() -> dut.pkcs1RsaSsaPkcs1V15Verify(message, signature));

              // signature is too long
              final var signatureTooLong = AfiUtils.concatenate(signature, RNG.nextBytes(1, 10));
              assertThrows(
                  IllegalArgumentException.class,
                  () -> dut.pkcs1RsaSsaPkcs1V15Verify(message, signatureTooLong));

              if (0 == signature[0]) {
                // ... leading zero
                // signature is too short (although okay as a BigInteger)
                final var signatureTooShort = Arrays.copyOfRange(signature, 1, signature.length);
                assertThrows(
                    IllegalArgumentException.class,
                    () -> dut.pkcs1RsaSsaPkcs1V15Verify(message, signatureTooShort));

                break;
              } // end fi
            } // end For (counter...)
            LOGGER.atDebug().log("{} bit: counter = {}", keyLength, counter);
          } // end --- e.
        } catch (InvalidKeyException
                 | NoSuchAlgorithmException
                 | SignatureException e) {
          fail(UNEXPECTED, e);
        } // end Catch (...)
      }); // end forEach(hash -> ...)
    }); // end forEach(keyLength -> ...)
    // spotless:on
    // end --- b, c, d, e
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#pkcs1RsaSsaPssVerify(byte[], byte[],
   * EafiHashAlgorithm)}.
   */
  @Test
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CyclomaticComplexity",
  })
  void test__pkcs1_RsaSsa_Pss_Verify___byteA_byteA_EafiHashAlgorithm() {
    // Assertions:
    // ... a. pkcs1_RsaSsa_Pss_Verify(byte[], byte[], int, EafiHashAlgorithm)-method
    //        works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all hash-algorithms

    final var prk = getPrk(MAX_LENGTH);
    final var dut = new RsaPublicKeyImpl(getPuk(MAX_LENGTH));
    final var message = RNG.nextBytes(1024, 2048);

    try {
      // --- a. smoke test
      {
        final var hash = EafiHashAlgorithm.SHA_256;
        final var algoName = "SHA256WithRSA/PSS";
        final var signer = Signature.getInstance(algoName);
        final var algorithmParameterSpec = MGF1ParameterSpec.SHA256;
        signer.setParameter(
            new PSSParameterSpec(
                hash.getAlgorithm(),
                "MGF1",
                algorithmParameterSpec,
                hash.getDigestLength(), // salt-length
                1));
        signer.initSign(prk);
        signer.update(message);
        final var signature = signer.sign();

        assertDoesNotThrow(() -> dut.pkcs1RsaSsaPssVerify(message, signature, hash));
      } // end --- a.

      // --- b. loop over all hash-algorithms
      {
        for (final var hash : EafiHashAlgorithm.values()) {
          // spotless:off
          final String algoName = switch (hash) {
            case SHA_1 -> "SHA1";
            case SHA_224 -> "SHA224";
            case SHA_256 -> "SHA256";
            case SHA_384 -> "SHA384";
            case SHA_512 -> "SHA512";
            default -> "ignore";
          };
          // spotless:on
          if ("ignore".equals(algoName)) { // NOPMD literal in if statement
            // ... hash-function for which no MGF1ParameterSpec exists
            continue;
          } // end fi
          final var signer = Signature.getInstance(algoName + "WithRSA/PSS");
          signer.setParameter(
              new PSSParameterSpec(
                  hash.getAlgorithm(),
                  "MGF1",
                  switch (hash) {
                    case SHA_1 -> MGF1ParameterSpec.SHA1;
                    case SHA_224 -> MGF1ParameterSpec.SHA224;
                    case SHA_256 -> MGF1ParameterSpec.SHA256;
                    case SHA_384 -> MGF1ParameterSpec.SHA384;
                    default -> MGF1ParameterSpec.SHA512;
                  },
                  hash.getDigestLength(), // salt-length
                  1));
          signer.initSign(prk);
          signer.update(message);
          final var signature = signer.sign();

          assertDoesNotThrow(() -> dut.pkcs1RsaSsaPssVerify(message, signature, hash), algoName);
        } // end For (hash...)
      } // end --- b.
    } catch (InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | SignatureException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#pkcs1RsaSsaPssVerify(byte[], byte[], int,
   * EafiHashAlgorithm)}.
   *
   * <p>Test vectors are taken from <a
   * href="https://github.com/twosigma/OpenJDK/blob/master/test/jdk/sun/security/rsa/pss/SigGenPSS_186-3.txt">SigGenPSS_186-3.txt</a>.
   */
  @Test
  void test__pkcs1_RsaSsa_Pss_Verify___byteA_byteA_int_EafiHashAlgorithm() {
    // Assertions:
    // ... a. verify_IsoIec9796_2_ds2(byte[], byte[], int, EafiHashAlgorithm)-method
    //        works as expected

    // Note: Because of the assertion "a", this simple method does not need extensive.
    //       Thus, we just use the test vectors.

    // Test strategy:
    // --- a. apply test vectors
    // --- b. ERROR: recoverable part present

    // --- a. apply test vectors
    try {
      final Path path = Path.of("src", "test", "resources", "SigGenPSS_186-3.txt");

      assertTrue(Files.isRegularFile(path), () -> path.toAbsolutePath().normalize().toString());

      final List<String> lines =
          Files.readAllLines(path, StandardCharsets.US_ASCII).stream()
              .filter(i -> !i.startsWith("#")) // let non-comment-line pass
              .filter(
                  line -> { // let lines pass which contain non-whitespace characters
                    for (int index = line.length(); index-- > 0; ) { // NOPMD assignment in operand
                      if (!Character.isWhitespace(line.charAt(index))) {
                        return true;
                      } // end fi
                    } // end For (index...)

                    return false;
                  })
              .toList();

      // loop through lines from file SignGenPSS_186-3.txt
      var puk = new RsaPublicKeyImpl(ONE, ZERO); // initialize
      final var splitter = " = ";
      for (var index = 0; index < lines.size(); ) {
        final var line = lines.get(index++); // NOPMD reassignment of loop variable

        if (line.startsWith("n = ")) {
          final var n = new BigInteger(line.split(splitter)[1], 16); // NOPMD new in loop
          final var e = new BigInteger(lines.get(index++).split(splitter)[1], 16); // NOPMD
          puk = new RsaPublicKeyImpl(n, e); // NOPMD new in loop
        } else if (line.startsWith("SHAAlg = ")) {
          final var hashAlg = line.split(splitter)[1];
          // spotless:off
          final var hash = switch (hashAlg) {
            case "SHA-224" -> EafiHashAlgorithm.SHA_224;
            case "SHA-256" -> EafiHashAlgorithm.SHA_256;
            case "SHA-384" -> EafiHashAlgorithm.SHA_384;
            default -> EafiHashAlgorithm.SHA_512;
          };
          // spotless:on
          final var message =
              Hex.toByteArray(
                  lines.get(index++).split(splitter)[1]); // NOPMD reassignment of loop variable
          final var signature =
              Hex.toByteArray(
                  lines.get(index++).split(splitter)[1]); // NOPMD reassignment of loop variable
          final var salt =
              Hex.toByteArray(
                  lines.get(index++).split(splitter)[1]); // NOPMD reassignment of loop variable
          final var dut = puk;

          assertDoesNotThrow(() -> dut.pkcs1RsaSsaPssVerify(message, signature, salt.length, hash));
        } // end else if
      } // end For (index...)
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.

    // --- b. ERROR: recoverable part present
    // Note 1: Hereafter, intentionally we verify a ISO/IEC 9796-2 DS2 signature.
    // Note 2: Test  vector from ISO/IEC 9796-2:2010 E.1.3.2
    {
      final var dut = PRK_9796_2_E1.getPublicKey();
      final var hash = EafiHashAlgorithm.SHA_1;
      final var saltLength = 20;
      final var expected = // m1
          Hex.toByteArray(
              """
                  61626364 62636465 63646566 64656667 65666768 66676869 6768696A 68696A6B
                  696A6B6C 6A6B6C6D 6B6C6D6E 6C6D6E6F 6D6E6F70 6E6F7071 6F707172 70717273
                  71727374 72737475 73747576 74757677 75767778 7677
                  """);
      final var m2 =
          Hex.toByteArray(
              """
                  7879 7778797A 78797A61 797A6162 7A616263 61626364 62636465
                  """);
      final var signature =
          Hex.toByteArray(
              """
                  67FC4BB5 C6AF6CC1 B44E01A3 2E5910CA 3423F21B CE635C71 DB6E8FD3 4E012E16
                  8F8342A6 21C0DEB6 DD0FEE98 3F523E51 67A2DCDD 13FAE8B8 2C66322F 8953293C
                  0EB9CFD1 9EC8E3AA DFB97ECB C23D3EC8 A0E32377 E3F5754D E6B8839B E0C9F07E
                  37E61950 9DAF2E6E 0548754C FB5E3F19 92BE1220 E7ED9899 37261BF4 417434C3
                  """);
      final var actual = dut.verifyIsoIec9796p2ds2(signature, m2, saltLength, hash);
      assertArrayEquals(expected, actual);

      assertThrows(
          IllegalArgumentException.class,
          () -> dut.pkcs1RsaSsaPssVerify(m2, signature, saltLength, hash));
    } // end --- b.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#pkcs1RsaVp1(BigInteger)}. */
  @Test
  void test__pkcs1_RsaVp1___BigInteger() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getter work as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. border tests
    // --- c. ERROR: s out of range

    final var dut = new RsaPublicKeyImpl(getPuk(MIN_LENGTH));
    final var n = dut.getModulus();
    final var e = dut.getPublicExponent();
    final var infimum = ZERO;
    final var supremum = n.subtract(ONE);

    // --- a. smoke test
    {
      final var s = new BigInteger(dut.getModulusLengthBit() - 1, RNG);
      final var expected = s.modPow(e, n);

      final var actual = dut.pkcs1RsaVp1(s);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. border tests
    Set.of(infimum, supremum)
        .forEach(
            s -> {
              final var expected = s.modPow(e, n);

              final var actual = dut.pkcs1RsaVp1(s);

              assertEquals(expected, actual);
            }); // end forEach(s -> ...)
    // end --- b.

    // --- c. ERROR: s out of range
    Set.of(infimum.subtract(ONE), supremum.add(ONE))
        .forEach(
            s ->
                assertThrows(
                    IllegalArgumentException.class,
                    () -> dut.pkcs1RsaEp(s))); // end forEach(s -> ...)
    // end --- c.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#signatureOpeningIsoIec9796p2(BigInteger)}. */
  @Test
  void test__signatureOpening_IsoIec9796_2__BigInteger() {
    // Assertions:
    // ...a pkcs1_RsaEp(BigInteger)-method works as expected

    // Test strategy:
    // --- a. smoke test

    final var prk = getPrk(MIN_LENGTH); // small key-size for fast operations
    final var n = prk.getModulus();
    final var d = prk.getPrivateExponent();
    final var e = prk.getPublicExponent();
    final var dut = new RsaPublicKeyImpl(n, e);

    // --- a. smoke test
    {
      final var expected =
          new BigInteger(dut.getModulusLengthBit() - 1, RNG).clearBit(0); // make expected even
      final var s1 = expected.modPow(d, n);
      final var s2 = n.subtract(s1);

      final var actual1 = dut.signatureOpeningIsoIec9796p2(s1);
      final var actual2 = dut.signatureOpeningIsoIec9796p2(s2);

      assertEquals(expected, actual1);
      assertEquals(expected, actual2);
    } // end method */
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#verifyIsoIec9796p2ds1(byte[], byte[],
   * EafiHashAlgorithm)}.
   */
  @Test
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.CyclomaticComplexity",
    "PMD.NPathComplexity",
    "PMD.NcssCount",
  })
  void test__verify_IsoIec9796_2_ds1__byteA_byteA_EafiHashAlgorithm() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with full recovery
    // --- b. smoke test with partial recovery
    // --- c. loop over various key-lengths
    // --- d. loop over various hash-algorithms
    // --- e. loop over all trailer lengths
    // --- f. loop over various message lengths
    // --- g. ERROR: wrong signature
    // --- h. ERROR: partial recovery with too many padding bits

    final var engine = new RSAEngine();

    try {
      // --- a. smoke test with full recovery
      // a.1 Test vector from ISO/IEC 9796-2:2010 E.1.2.1
      // a.2 signature generated by bouncy castle

      // a.1 Test vector from ISO/IEC 9796-2:2010 E.1.2.1
      {
        final var dut = PRK_9796_2_E1.getPublicKey();
        final var hash = EafiHashAlgorithm.SHA_1;
        final var m = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopqopqrpqrs";
        final var expected = m.getBytes(StandardCharsets.UTF_8);
        assertEquals(
            Hex.extractHexDigits(
                """
                    61626364 62636465 63646566 64656667 65666768 66676869 6768696A 68696A6B
                    696A6B6C 6A6B6C6D 6B6C6D6E 6C6D6E6F 6D6E6F70 6E6F7071 6F707172 70717273
                    """),
            Hex.toHexDigits(expected));
        final var signature =
            Hex.toByteArray(
                """
                    24725B14 80D1ED93 54A210F5 08BBB528 0B718CCE AC8E1549 F1039362 6D59C8FE
                    CEF57483 D501C31D 8E5F8E81 6430C55C B263CF29 AA7D1C81 012DF0C8 431963E2
                    5A8789DB A6C8E211 00BD3E1C 7A769056 AC2A8530 8469FD1E ACDD68D4 997935C7
                    A543274E 2C025392 9D916618 E0DBCD30 0665CAEE 97CE6217 B00469BE 7ABE43C9
                    """);

        final var actual = dut.verifyIsoIec9796p2ds1(signature, AfiUtils.EMPTY_OS, hash);

        assertArrayEquals(expected, actual);
      } // end a.1.

      // a.2 signature generated by bouncy castle
      {
        final var keyLength = MAX_LENGTH;
        final var prk = getPrk(keyLength);
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        final var kpPrk =
            new RSAPrivateCrtKeyParameters(
                prk.getModulus(),
                prk.getPublicExponent(),
                prk.getPrivateExponent(),
                prk.getPrimeP(),
                prk.getPrimeQ(),
                prk.getPrimeExponentP(),
                prk.getPrimeExponentQ(),
                prk.getCrtCoefficient());
        final var message = RNG.nextBytes(16, 32);
        final var hash = EafiHashAlgorithm.SHA_1;
        final var digest = new SHA1Digest();
        final var implicit = true;
        final var signer = new ISO9796d2Signer(engine, digest, implicit);
        signer.init(true, kpPrk);
        signer.update(message, 0, message.length);
        final var signature = signer.generateSignature();
        final var m1 = signer.getRecoveredMessage();
        final var m2 = Arrays.copyOfRange(message, m1.length, message.length);

        final var recovered = dut.verifyIsoIec9796p2ds1(signature, m2, hash);

        assertArrayEquals(m1, recovered);
        assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
      } // end a.2
      // end --- a.

      // --- b. smoke test with partial recovery
      // b.1 Test vector from ISO/IEC 9796-2:2010 E.1.3.1
      // b.2 signature generated by bouncy castle

      // b.1 Test vector from ISO/IEC 9796-2:2010 E.1.3.1
      {
        final var dut = PRK_9796_2_E1.getPublicKey();
        final var hash = EafiHashAlgorithm.RIPEMD_160;
        final var expected = // m1
            Hex.toByteArray(
                """
                    FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                    FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                    FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                    FEDCBA98 76543210 FEDC
                    """);
        final var m2 =
            Hex.toByteArray(
                """
                    BA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98
                    """);
        final var signature =
            Hex.toByteArray(
                """
                    30CA91BB 8721F57A 8230CB13 FBC511F1 24345CA3 AD45E9AF FC0C848F 0DCD4F44
                    35D88226 B4D2A88B 70CAE7C9 371D7B1E 6588A454 467F8010 FB21C6DC 66FE6954
                    63B97DEE 36041B23 FFA24809 678C67DF DCAF8DC0 F5F75CF0 E677C528 EA80EF15
                    6E68953B 8892FB4E 0AD5EEBB 0632B9A7 B40DDD41 6E16A09C 842E6B9F 688D96F8
                    """);

        final var actual = dut.verifyIsoIec9796p2ds1(signature, m2, hash);

        assertArrayEquals(expected, actual);
      } // end b.1.

      // b.2 signature generated by bouncy castle
      {
        final var keyLength = MAX_LENGTH;
        final var prk = getPrk(keyLength);
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        final var kpPrk =
            new RSAPrivateCrtKeyParameters(
                prk.getModulus(),
                prk.getPublicExponent(),
                prk.getPrivateExponent(),
                prk.getPrimeP(),
                prk.getPrimeQ(),
                prk.getPrimeExponentP(),
                prk.getPrimeExponentQ(),
                prk.getCrtCoefficient());
        final var message = RNG.nextBytes(1024, 2048);
        final var hash = EafiHashAlgorithm.SHA_1;
        final var digest = new SHA1Digest();
        final var implicit = false;
        final var signer = new ISO9796d2Signer(engine, digest, implicit);
        signer.init(true, kpPrk);
        signer.update(message, 0, message.length);
        final var signature = signer.generateSignature();
        final var m1 = signer.getRecoveredMessage();
        final var m2 = Arrays.copyOfRange(message, m1.length, message.length);

        final var recovered = dut.verifyIsoIec9796p2ds1(signature, m2, hash);

        assertArrayEquals(m1, recovered);
        assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
      } // end b.2
      // end --- b.

      // --- c. loop over various key-lengths
      for (final var keyLength : IntStream.rangeClosed(MAX_LENGTH - 8, MAX_LENGTH).toArray()) {
        final var prk = getPrk(keyLength);
        final var myPrk = new RsaPrivateCrtKeyImpl(prk);
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        final var kpPrk =
            new RSAPrivateCrtKeyParameters(
                prk.getModulus(),
                prk.getPublicExponent(),
                prk.getPrivateExponent(),
                prk.getPrimeP(),
                prk.getPrimeQ(),
                prk.getPrimeExponentP(),
                prk.getPrimeExponentQ(),
                prk.getCrtCoefficient());
        final var k = dut.getModulusLengthBit();

        // --- d. loop over various hash-algorithms
        for (final var hash : EafiHashAlgorithm.values()) {
          final Digest digest = HASH_MAP.get(hash);
          final var lh = hash.getDigestLength() << 3; // length in bits

          // --- e. loop over all trailer lengths
          for (final var implicit : VALUES_BOOLEAN) {
            if (!implicit && NO_VALID_TRAILER.contains(hash)) {
              // ... no valid trailer for digest
              assertThrows(
                  IllegalArgumentException.class, () -> new ISO9796d2Signer(engine, digest, false));
            } else {
              // ... valid trailer for digest
              final var signer = new ISO9796d2Signer(engine, digest, implicit);
              signer.init(true, kpPrk);
              final var c = k - lh - 8 * (implicit ? 1 : 2) - 4; // capacity in bit
              final var delta = c % 8;
              final var cStar = c - delta;
              final var cOctet = cStar >> 3; // capacity in octet

              // --- f. loop over various message lengths
              final var mLengthValues =
                  new LinkedHashSet<>(List.of(0, 1, cOctet - 1, cOctet, cOctet + 1, MEBI));
              for (final var mLength : mLengthValues) {
                final var message = RNG.nextBytes(mLength);
                final byte[] signature;
                final byte[] m2;
                if (0 == (k % 8)) {
                  // ... bouncy castle supports only multiple 8 modulus lengths
                  signer.update(message, 0, message.length);
                  signature = signer.generateSignature();
                  final var m1 = signer.getRecoveredMessage();
                  m2 = Arrays.copyOfRange(message, m1.length, message.length);
                } else {
                  // ... modulus length which is not supported by bouncy castle
                  final var result = myPrk.signIsoIec9796p2ds1(message, hash, implicit);
                  signature = result[0];
                  m2 = result[1];
                } // end else

                final var m1 = dut.verifyIsoIec9796p2ds1(signature, m2, hash);

                assertArrayEquals(message, AfiUtils.concatenate(m1, m2));

                // --- g. ERROR: wrong signature
                {
                  final var m2False = Arrays.copyOfRange(m2, 0, m2.length + 1);
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> dut.verifyIsoIec9796p2ds1(signature, m2False, hash));
                } // end --- g.
              } // end For (mLength...)
            } // end else
          } // end For (implicit...)
        } // end For (hash...)
      } // end For (keyLength...)
      // end --- c, d, e, f, g.

      // --- h. ERROR: partial recovery with too many padding bits
      {
        final var keyLength = MAX_LENGTH;
        final var prk = new RsaPrivateCrtKeyImpl(getPrk(keyLength));
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        final var message = RNG.nextBytes(keyLength, keyLength << 1);
        final var hash = EafiHashAlgorithm.SHA_256;

        // Note: The code hereafter is a simplified copy of
        //       RsaPrivateKeyImpl.sign_IsoIec9796_2_ds1(byte[], byte[], EafiHashAlgorithm, int)
        //       with the assumptions:
        //       1. modulus length is a multiple of 8. Thus, delta = 0.
        //       2. trailer = 'bc'
        final int k = prk.getModulusLengthBit();
        final int lh = hash.getDigestLength() << 3; // length in bits
        final int c = k - lh - 8 - 4;
        final var t = BigInteger.valueOf(0xbc);

        for (final var delta :
            new int[] {
              4, // correct value
              12 // too many padding bits
            }) {
          final var cStar = c - delta;
          final var mSplit = RsaPrivateKeyImpl.messageAllocation(message, cStar);
          final var m1 = mSplit[0];
          final var m2 = mSplit[1];
          final var h = hash.digest(message);

          BigInteger bitString = ONE;
          bitString = bitString.shiftLeft(1).add((m2.length > 0) ? ONE : ZERO);
          bitString = bitString.shiftLeft(c - cStar);
          bitString = bitString.shiftLeft(1).add(ONE);
          bitString = bitString.shiftLeft(cStar).add(new BigInteger(1, m1));
          bitString = bitString.shiftLeft(lh).add(new BigInteger(1, h));
          bitString = bitString.shiftLeft(8).add(t);
          final var f2 = bitString.toByteArray();

          if (0 == (f2[0] & 0x10)) {
            for (int i = 0; true; i++) {
              // --- check high nibble
              if (i > 0) {
                if (0 == (f2[i] & 0xf0)) {
                  f2[i] |= (byte) 0xb0;
                } else {
                  f2[i] ^= (byte) 0xb0;
                  break;
                } // end fi
              } // end fi (skip first high nibble)

              // --- check low nibble
              if (0 == (f2[i] & 0x0f)) {
                f2[i] |= 0x0b;
              } else {
                f2[i] ^= 0x0b;
                break;
              } // end fi
            } // end For (i...)
          } // end fi
          final BigInteger F2 = new BigInteger(f2); // NOPMD "F2" doesn't match [a-z][a-zA-Z0-9]*
          final byte[] signature =
              AfiBigInteger.i2os(prk.signIsoIec9796p2A4(F2), prk.getModulusLengthOctet());

          if (4 == delta) { // NOPMD literal in if statement
            // ... correct signature
            final var recovered = dut.verifyIsoIec9796p2ds1(signature, m2, hash);

            assertArrayEquals(m1, recovered);
          } else {
            // ... too many padding bits
            assertThrows(
                IllegalArgumentException.class,
                () -> dut.verifyIsoIec9796p2ds1(signature, m2, hash));
          } // end else
        } // end For (delta...)
      } // end --- h.
    } catch (CryptoException | IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#verifyIsoIec9796p2ds2(byte[], byte[],
   * EafiHashAlgorithm)}.
   */
  @Test
  void test__verify_IsoIec9796_2_ds2__byteA_byteA_EafiHashAlgorithm() {
    // Assertions:
    // ... a. verify_IsoIec9796_2_ds2(byte[], byte[], int, EafiHashAlgorithm)-method
    //        works as expected

    // Note: Because of assertions "a" this simple method needs little testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var engine = new RSAEngine();
    final var keyLength = MAX_LENGTH;
    final var prk = getPrk(keyLength);
    final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
    final var kpPrk =
        new RSAPrivateCrtKeyParameters(
            prk.getModulus(),
            prk.getPublicExponent(),
            prk.getPrivateExponent(),
            prk.getPrimeP(),
            prk.getPrimeQ(),
            prk.getPrimeExponentP(),
            prk.getPrimeExponentQ(),
            prk.getCrtCoefficient());
    final var message = RNG.nextBytes(MAX_LENGTH, MAX_LENGTH << 1);
    final var hash = EafiHashAlgorithm.SHA_256;
    final var digest = new SHA256Digest();

    // --- a. smoke test
    try {
      final var signer = new ISO9796d2PSSSigner(engine, digest, hash.getDigestLength());
      signer.init(true, kpPrk);
      signer.update(message, 0, message.length);
      final var signature = signer.generateSignature();
      final var m1 = signer.getRecoveredMessage();
      final var m2 = Arrays.copyOfRange(message, m1.length, message.length);

      final var recovered = dut.verifyIsoIec9796p2ds2(signature, m2, hash);

      assertArrayEquals(m1, recovered);
      assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
    } catch (CryptoException | IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#verifyIsoIec9796p2ds2(byte[], byte[], int,
   * EafiHashAlgorithm)}.
   */
  @SuppressWarnings({
    "PMD.AvoidInstantiatingObjectsInLoops",
    "PMD.CognitiveComplexity",
    "PMD.CyclomaticComplexity",
    "PMD.NcssCount",
  })
  @Test
  void test__verify_IsoIec9796_2_ds2__byteA_byteA_int_EafiHashAlgorithm() {
    // Assertions:
    // ... a. signatureOpening_IsoIec9796_2(BigInteger)-method works as intended

    // Test strategy:
    // --- a. smoke test with full recovery
    // --- b. smoke test with partial recovery
    // --- c. loop over various key-lengths
    // --- d. loop over various hash-algorithms
    // --- e. loop over all trailer lengths
    // --- f. loop over various salt lengths
    // --- g. loop over various message lengths
    // --- h. ERROR: wrong signature
    // --- i. ERROR: no first '1' bit

    final var engine = new RSAEngine();

    try {
      // --- a. smoke test with full recovery
      // a.1 Test vector from ISO/IEC 9796-2:2010 E.1.2.2
      // a.2 signature generated by bouncy castle

      // a.1 Test vector from ISO/IEC 9796-2:2010 E.1.2.2
      {
        final var dut = PRK_9796_2_E1.getPublicKey();
        final var hash = EafiHashAlgorithm.RIPEMD_160;
        final var message =
            Hex.toByteArray(
                """
                    FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                    FEDCBA98 76543210 FEDCBA98 76543210
                    """);
        final var signature =
            Hex.toByteArray(
                """
                    56136187 14871D42 EAA2CFFB DB9639BA 04ED8532 B4A20C4E 79CB2BA3 0ED58BB5
                    93E38E2C 9CBF7563 32CC26C4 B115F73C E1BEF204 252DAF73 708569E6 E3304E1F
                    F194E877 69800E73 10D31E4E 4AE53E2D 73FE0EDC C1B74AAB 6A64A808 CA3EDA35
                    2B0F86C0 A4ECC996 B8301BD9 8293B7BF 4063CD94 66091B74 39E3682A 53A58505
                    """);

        final var recovered =
            dut.verifyIsoIec9796p2ds2(signature, AfiUtils.EMPTY_OS, hash.getDigestLength(), hash);

        assertArrayEquals(message, recovered);
      } // end a.1

      // a.2 signature generated by bouncy castle
      {
        final var keyLength = MAX_LENGTH;
        final var prk = getPrk(keyLength);
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        final var kpPrk =
            new RSAPrivateCrtKeyParameters(
                prk.getModulus(),
                prk.getPublicExponent(),
                prk.getPrivateExponent(),
                prk.getPrimeP(),
                prk.getPrimeQ(),
                prk.getPrimeExponentP(),
                prk.getPrimeExponentQ(),
                prk.getCrtCoefficient());
        final var hash = EafiHashAlgorithm.SHA_1;
        final var digest = new SHA1Digest();
        final var implicit = true;
        final var message = RNG.nextBytes(16, 32);
        final var saltLength = hash.getDigestLength();
        final var signer = new ISO9796d2PSSSigner(engine, digest, saltLength, implicit);
        signer.init(true, kpPrk);
        signer.update(message, 0, message.length);
        final var signature = signer.generateSignature();
        final var m1 = signer.getRecoveredMessage();
        final var m2 = Arrays.copyOfRange(message, m1.length, message.length);

        final var recovered = dut.verifyIsoIec9796p2ds2(signature, m2, hash);

        assertArrayEquals(m1, recovered);
        assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
      } // end a.2
      // end --- a.

      // --- b. smoke test with partial recovery
      // b.1 Test vector from ISO/IEC 9796-2:2010 E.1.3.2
      // b.2 signature generated by bouncy castle

      // b.1 Test vector from ISO/IEC 9796-2:2010 E.1.3.2
      {
        final var dut = PRK_9796_2_E1.getPublicKey();
        final var hash = EafiHashAlgorithm.SHA_1;
        final var saltLength = 20;
        final var expected = // m1
            Hex.toByteArray(
                """
                    61626364 62636465 63646566 64656667 65666768 66676869 6768696A 68696A6B
                    696A6B6C 6A6B6C6D 6B6C6D6E 6C6D6E6F 6D6E6F70 6E6F7071 6F707172 70717273
                    71727374 72737475 73747576 74757677 75767778 7677
                    """);
        final var m2 =
            Hex.toByteArray(
                """
                    7879 7778797A 78797A61 797A6162 7A616263 61626364 62636465
                    """);
        final var signature =
            Hex.toByteArray(
                """
                    67FC4BB5 C6AF6CC1 B44E01A3 2E5910CA 3423F21B CE635C71 DB6E8FD3 4E012E16
                    8F8342A6 21C0DEB6 DD0FEE98 3F523E51 67A2DCDD 13FAE8B8 2C66322F 8953293C
                    0EB9CFD1 9EC8E3AA DFB97ECB C23D3EC8 A0E32377 E3F5754D E6B8839B E0C9F07E
                    37E61950 9DAF2E6E 0548754C FB5E3F19 92BE1220 E7ED9899 37261BF4 417434C3
                    """);

        final var actual = dut.verifyIsoIec9796p2ds2(signature, m2, saltLength, hash);

        assertArrayEquals(expected, actual);
      } // end b.1

      // b.2 signature generated by bouncy castle
      {
        final var keyLength = MAX_LENGTH;
        final var prk = getPrk(keyLength);
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        final var kpPrk =
            new RSAPrivateCrtKeyParameters(
                prk.getModulus(),
                prk.getPublicExponent(),
                prk.getPrivateExponent(),
                prk.getPrimeP(),
                prk.getPrimeQ(),
                prk.getPrimeExponentP(),
                prk.getPrimeExponentQ(),
                prk.getCrtCoefficient());
        final var hash = EafiHashAlgorithm.SHA_1;
        final var digest = new SHA1Digest();
        final var implicit = false;
        final var message = RNG.nextBytes(1024, 2048);
        final var saltLength = hash.getDigestLength();
        final var signer = new ISO9796d2PSSSigner(engine, digest, saltLength, implicit);
        signer.init(true, kpPrk);
        signer.update(message, 0, message.length);
        final var signature = signer.generateSignature();
        final var m1 = signer.getRecoveredMessage();
        final var m2 = Arrays.copyOfRange(message, m1.length, message.length);

        final var recovered = dut.verifyIsoIec9796p2ds2(signature, m2, saltLength, hash);

        assertArrayEquals(m1, recovered);
        assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
      } // end b.2
      // end --- b.

      // --- c. loop over various key-lengths
      for (final var keyLength : IntStream.rangeClosed(MAX_LENGTH - 8, MAX_LENGTH).toArray()) {
        final var prk = getPrk(keyLength);
        final var myPrk = new RsaPrivateCrtKeyImpl(prk);
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        final var kpPrk =
            new RSAPrivateCrtKeyParameters(
                prk.getModulus(),
                prk.getPublicExponent(),
                prk.getPrivateExponent(),
                prk.getPrimeP(),
                prk.getPrimeQ(),
                prk.getPrimeExponentP(),
                prk.getPrimeExponentQ(),
                prk.getCrtCoefficient());
        final var k = dut.getModulusLengthBit();

        // --- d. loop over various hash-algorithms
        for (final var hash : EafiHashAlgorithm.values()) {
          final Digest digest = HASH_MAP.get(hash);
          final var lh = hash.getDigestLength() << 3; // length in bits

          // --- e. loop over all trailer lengths
          for (final var implicit : VALUES_BOOLEAN) {
            if (!implicit && NO_VALID_TRAILER.contains(hash)) {
              // ... no valid trailer for digest
              assertThrows(
                  IllegalArgumentException.class, () -> new ISO9796d2Signer(engine, digest, false));
            } else {
              // ... valid trailer for digest
              final var t = implicit ? 1 : 2;

              // --- f. loop over various salt lengths
              // Note 1: According to 9.2.4 it is:
              //         c = k - Lh - Ls - 8t - 2
              // Note 2: According to 7.2.2 the capacity shall be
              //         c >= 7
              // Note 3: It follows:
              //         k - Lh - Ls - 8t - 2 >= 7
              //     <=> k - Lh - Ls - 8t >= 9
              //     <=> k - Lh      - 8t - 9 >= Ls
              final var saltLengthMaxBit = k - lh - 8 * t - 9;
              final var saltLengthMax = saltLengthMaxBit >> 3; // length in octet
              for (final var LsOctet : RNG.intsClosed(1, saltLengthMax, 5).toArray()) {
                final var c = k - lh - 8 * (LsOctet + t) - 2; // capacity, 9.2.4
                final var co = c >> 3; // capacity in octet

                // --- g. loop over various message lengths
                final var mLengthValues =
                    new LinkedHashSet<>(List.of(0, 1, co - 1, co, co + 1, MEBI));
                for (final var mLength : mLengthValues) {
                  if (mLength < 0) {
                    // ... cOctet==0, thus, (cOctet - 1) is negative
                    continue;
                  } // end fi
                  // ... mLength is non-negative

                  LOGGER
                      .atTrace()
                      .log(
                          "n={} bit, {}, t={}, Ls={}, |M|={}",
                          k,
                          hash.getAlgorithm(),
                          t,
                          LsOctet,
                          mLength);
                  final var message = RNG.nextBytes(mLength);
                  final byte[] signature;
                  final byte[] m2;
                  if (0 == (k % 8)) {
                    // ... bouncy castle supports only multiple 8 modulus lengths
                    final var signer = new ISO9796d2PSSSigner(engine, digest, LsOctet, implicit);
                    signer.init(true, kpPrk);
                    signer.update(message, 0, message.length);
                    signature = signer.generateSignature();
                    final var m1 = signer.getRecoveredMessage();
                    m2 = Arrays.copyOfRange(message, m1.length, message.length);
                  } else {
                    final var result =
                        myPrk.signIsoIec9796p2ds2(
                            message, LsOctet, hash, implicit, RNG.nextBoolean());
                    signature = result[0];
                    m2 = result[1];
                  } // end else

                  final var m1 = dut.verifyIsoIec9796p2ds2(signature, m2, LsOctet, hash);

                  assertArrayEquals(message, AfiUtils.concatenate(m1, m2));

                  // --- h. ERROR: wrong signature
                  {
                    final var m2False = Arrays.copyOfRange(m2, 0, m2.length + 1);
                    assertThrows(
                        IllegalArgumentException.class,
                        () -> dut.verifyIsoIec9796p2ds2(signature, m2False, LsOctet, hash));
                  } // end --- h.
                } // end For (mLength...)
              } // end For (saltLength...)
            } // end else
          } // end For (implicit...)
        } // end For (hash...)
      } // end For (keyLength...)
      // end --- c, d, e, f, g, h.

      // --- i. ERROR: no first '1' bit
      {
        // Note 1: The idea is to generate the message representative F
        //         such that during message recovery after XOR with
        //         mask-generation-function D equals zero.
        final var keyLength = MIN_LENGTH;
        final var prk = new RsaPrivateCrtKeyImpl(getPrk(keyLength));
        final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
        final var nOctet = prk.getModulusLengthOctet();
        final var trailer = new byte[] {(byte) 0xbc};
        final var numberOfPaddingBytes = trailer.length;
        final var hash = EafiHashAlgorithm.SHA_1;
        final var saltLength = -1; // value doesn't matter
        final var m2 = AfiUtils.EMPTY_OS; // value doesn't matter

        final var modulus = prk.getModulus();
        BigInteger messageRepresentative;
        do {
          final var hashValue = RNG.nextBytes(hash.getDigestLength());
          final var n =
              hash.maskGenerationFunction(
                  hashValue, nOctet - hash.getDigestLength() - numberOfPaddingBytes, 0);
          messageRepresentative = new BigInteger(1, AfiUtils.concatenate(n, hashValue, trailer));
        } while (modulus.compareTo(messageRepresentative) < 0);
        // ... modulus > messageRepresentative
        //    => no "c too big" exception in pkcs1_RsaSp1(BigInteger)-method
        final var signature = AfiBigInteger.i2os(prk.pkcs1RsaSp1(messageRepresentative), nOctet);

        assertThrows(
            IllegalArgumentException.class,
            () -> dut.verifyIsoIec9796p2ds2(signature, m2, saltLength, hash));
      } // end --- i.
    } catch (CryptoException | IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#verifyIsoIec9796p2ds3(byte[], byte[], int,
   * EafiHashAlgorithm)}.
   */
  @Test
  void test__verify_IsoIec9796_2_ds3__byteA_byteA_int_EafiHashAlgorithm() {
    // Assertions:
    // ... a. verify_IsoIec9796_2_ds2(byte[], byte[], int, EafiHashAlgorithm)-method
    //        works as intended

    // Test strategy:
    // --- a. smoke test without salt
    // --- b. smoke test without salt but message present
    // --- c. smoke test with salt

    final var engine = new RSAEngine();
    final var keyLength = MAX_LENGTH;
    final var prk = getPrk(keyLength);
    final var dut = new RsaPublicKeyImpl(getPuk(keyLength));
    final var kpPrk =
        new RSAPrivateCrtKeyParameters(
            prk.getModulus(),
            prk.getPublicExponent(),
            prk.getPrivateExponent(),
            prk.getPrimeP(),
            prk.getPrimeQ(),
            prk.getPrimeExponentP(),
            prk.getPrimeExponentQ(),
            prk.getCrtCoefficient());
    final var message = RNG.nextBytes(MAX_LENGTH, MAX_LENGTH << 1);
    final var hash = EafiHashAlgorithm.SHA_256;
    final var digest = new SHA256Digest();

    try {
      // --- a. smoke test without salt
      // a.1 Test vector from ISO/IEC 9796-2:2010 E.1.2.3
      // a.2 Test vector from ISO/IEC 9796-2:2010 E.1.3.3

      // a.1 Test vector from ISO/IEC 9796-2:2010 E.1.2.3
      {
        final var dut3 = PRK_9796_2_E1.getPublicKey();
        final var hash3 = EafiHashAlgorithm.SHA_1;
        final var saltLength = 0;
        final var expected = AfiUtils.EMPTY_OS;
        final var signature =
            Hex.toByteArray(
                """
                    00CB4DC1 F43D1E3B E55D0F7E 29258A26 4AF5F62B 3891429B EEDD0B46 E7F6B44C
                    3D60DC7C D984C57F 30257FCD 1489C17D B45EF390 3B5BC372 93242771 80395A2B
                    24B0470D AADAD8F2 69C2109B E547F928 66574C22 4E921274 6119C0D1 2725C73B
                    15CDCE63 BF8E3389 96AF4C06 D45ACC14 77A2317F 4F6B1310 55F64C3D CB88765E
                    """);

        final var actual = dut3.verifyIsoIec9796p2ds3(signature, expected, saltLength, hash3);

        assertArrayEquals(expected, actual);
      } // end a.1

      // a.2 Test vector from ISO/IEC 9796-2:2010 E.1.3.3
      {
        final var dut3 = PRK_9796_2_E1.getPublicKey();
        final var hash3 = EafiHashAlgorithm.SHA_1;
        final var saltLength = 0;
        final var expected = // m1
            Hex.toByteArray(
                """
                    FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                    FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                    FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210
                    FEDCBA98 76543210 FE
                    """);
        final var m2 =
            Hex.toByteArray(
                """
                    DCBA98 76543210 FEDCBA98 76543210 FEDCBA98 76543210 FEDCBA98
                    """);
        final var signature =
            Hex.toByteArray(
                """
                    30147ECB 074705DD F33EF765 D0EE1017 D5535AB3 9A7727C4 D8D4DC42 42C693BD
                    1FB544EC AE2323D1 185BED05 C8AA5F69 9D3AAED4 1FC3ECF9 DF297A61 56D6BC86
                    5196A619 806E3FDF F8A8416D 2984EF9E 33940013 4A6D1712 2FCF0946 783AEBD4
                    6F11397E 66863E74 28F4542D E2AE8A30 7355633F 380F937B 308C149F 14194487
                    """);

        final var actual = dut3.verifyIsoIec9796p2ds3(signature, m2, saltLength, hash3);

        assertArrayEquals(expected, actual);
      } // end a.2
      // end --- a.

      // --- b. smoke test without salt but message present
      {
        final var saltLength = 0;
        final var signer = new ISO9796d2PSSSigner(engine, digest, saltLength);
        signer.init(true, kpPrk);
        signer.update(message, 0, message.length);
        final var signature = signer.generateSignature();
        final var m1 = signer.getRecoveredMessage();
        final var m2 = Arrays.copyOfRange(message, m1.length, message.length);

        final var recovered = dut.verifyIsoIec9796p2ds3(signature, m2, saltLength, hash);

        assertArrayEquals(m1, recovered);
        assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
      } // end --- b.

      // --- c. smoke test with salt
      {
        final var saltLength = 7;
        final var signer = new ISO9796d2PSSSigner(engine, digest, saltLength);
        signer.init(true, kpPrk);
        signer.update(message, 0, message.length);
        final var signature = signer.generateSignature();
        final var m1 = signer.getRecoveredMessage();
        final var m2 = Arrays.copyOfRange(message, m1.length, message.length);

        final var recovered = dut.verifyIsoIec9796p2ds3(signature, m2, saltLength, hash);

        assertArrayEquals(m1, recovered);
        assertArrayEquals(message, AfiUtils.concatenate(m1, m2));
      } // end --- c.
    } catch (CryptoException | IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected
    // ... b. getModulus()-method works as expected
    // ... c. getPublicExponent()-method works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with fixed input, n and e with similar length
    // --- b. smoke test with fixed input, n much longer than e

    final int nLength = 248;

    // --- a. smoke test with fixed input, n and e with similar length
    {
      final BigInteger n = BigInteger.probablePrime(nLength, RNG);
      final BigInteger e = BigInteger.valueOf(255);
      final String nHex = AfiBigInteger.toHex(n);

      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

      assertEquals(nLength / 4, nHex.length());
      assertEquals(
          String.format(
              "n  = %s%ne  =                                                             ff",
              AfiBigInteger.toHex(n)),
          dut.toString());
    } // end --- a.

    // --- b. smoke test with fixed input, n much longer than e
    {
      final BigInteger n = BigInteger.probablePrime(nLength + 1, RNG);
      final BigInteger e = BigInteger.valueOf(255);
      final String nHex = AfiBigInteger.toHex(n);

      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

      assertEquals(nLength / 4 + 2, nHex.length());
      assertEquals(String.format("n  = %s%ne  = ff", AfiBigInteger.toHex(n)), dut.toString());
    } // end --- b.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#toString(int, int)}. */
  @Test
  void test_toString__int_int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with even number of characters
    // --- b. smoke test with odd number of characters

    // --- a. smoke test with even number of characters
    {
      final var n = 0x2341;
      final var e = 0x12;
      final var dut = new RsaPublicKeyImpl(BigInteger.valueOf(n), BigInteger.valueOf(e));

      assertEquals(String.format("n  = %4x%ne  = %4x", n, e), dut.toString(2, 2));
    } // end --- a.

    // --- b. smoke test with odd number of characters
    {
      final var n = 323;
      final var e = 13;
      final var dut = new RsaPublicKeyImpl(BigInteger.valueOf(n), BigInteger.valueOf(e));

      assertEquals(String.format("n  = %x%ne  = %x", n, e), dut.toString(0, 0));
      assertEquals(String.format("n  = %6x%ne  = %4x", n, e), dut.toString(3, 2));
      assertEquals(String.format("n  = %8x%ne  = %8x", n, e), dut.toString(4, 4));
    } // end --- b.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#getModulus()}. */
  @Test
  void test_getModulus() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final BigInteger n = BigInteger.valueOf(64);
      final BigInteger e = BigInteger.valueOf(12);

      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

      assertSame(n, dut.getModulus());
    } // end --- a.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#getModulusLengthBit()}. */
  @Test
  void test_getModulusLengthBit() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected
    // ... b. getModulus()-method works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with fixed input
    // --- b. some random input

    // --- a. smoke test with a fixed input
    {
      final BigInteger n = BigInteger.valueOf(15);
      final BigInteger e = BigInteger.valueOf(7);

      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

      assertEquals(4, dut.getModulusLengthBit());
    } // end --- a.

    // --- b. some random input
    RNG.intsClosed(1024, 2048, 20)
        .forEach(
            modulusLength -> {
              final BigInteger n = new BigInteger(modulusLength, RNG);
              final BigInteger e = BigInteger.valueOf(RNG.nextLong());

              final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

              assertEquals(n.bitLength(), dut.getModulusLengthBit());
            }); // end forEach(modulusLength -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#getModulusLengthOctet()}. */
  @Test
  void test_getModulusLengthOctet() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected
    // ... b. getModulusLengthBit()-method works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with fixed input
    // --- b. some random input

    // --- a. smoke test with a fixed input
    {
      final BigInteger n = BigInteger.valueOf(256);
      final BigInteger e = BigInteger.valueOf(7);

      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

      assertEquals(2, dut.getModulusLengthOctet());
    } // end --- a.

    // --- b. some random input
    RNG.intsClosed(1024, 2048, 20)
        .forEach(
            modulusLength -> {
              final BigInteger n = new BigInteger(modulusLength, RNG);
              final BigInteger e = BigInteger.valueOf(RNG.nextLong());

              final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

              assertEquals(AfiBigInteger.i2os(n).length, dut.getModulusLengthOctet());
            }); // end forEach(modulusLength -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link RsaPublicKeyImpl#getPublicExponent()}. */
  @Test
  void test_getPublicExponent() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with a fixed input
    {
      final BigInteger n = BigInteger.valueOf(64);
      final BigInteger e = BigInteger.valueOf(12);

      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(n, e);

      assertSame(e, dut.getPublicExponent());
    } // end --- a.
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#extract(ConstructedBerTlv, EafiRsaPukFormat)} where
   * format equals {@link EafiRsaPukFormat#ISO7816}.
   */
  private boolean zzTestExtractConstructedBerTlvIso7816() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. happy case with manually chosen values
    // --- b. ERROR: wrong tags, missing values, ...

    final EafiRsaPukFormat format = EafiRsaPukFormat.ISO7816;

    // --- a. happy case with manually chosen values
    Map.ofEntries(
            Map.entry(
                "7f49-06  [(81-01-1b) (82-01-11)]", // structure as expected
                new BigInteger[] {BigInteger.valueOf(27), BigInteger.valueOf(17)}),
            Map.entry(
                "7f49-06  [(82-01-0b) (81-01-23)]", // e before n
                new BigInteger[] {BigInteger.valueOf(35), BigInteger.valueOf(11)}),
            Map.entry(
                "7f49-06  [(81-02-b341) (82-02-0081)]", // MSBit set, leading '00'
                new BigInteger[] {BigInteger.valueOf(0xb341), BigInteger.valueOf(0x81)}),
            Map.entry(
                "7f49-09  [(81-02-b341) (81-01-11) (82-02-0081)]", // only the first modulus count
                new BigInteger[] {BigInteger.valueOf(0xb341), BigInteger.valueOf(0x81)}))
        .forEach(
            (input, expected) -> {
              final ConstructedBerTlv tlv = (ConstructedBerTlv) BerTlv.getInstance(input);
              final BigInteger[] present = RsaPublicKeyImpl.extract(tlv, format);

              assertArrayEquals(expected, present);
            }); // end forEach((input, expected) -> ...)
    // end --- a.

    // --- b. ERROR: wrong tags, missing values, ...
    Set.of(
            "7f48-03 [(81-01-1b) (82-01-03)]", // outer tag wrong
            "7f49-03 [(80-01-1b) (82-01-03)]", // n missing
            "7f49-03 [(81-01-1b) (81-01-03)]" // e missing
            )
        .forEach(
            input -> {
              final ConstructedBerTlv tlv = (ConstructedBerTlv) BerTlv.getInstance(input);

              assertThrows(
                  IllegalArgumentException.class, () -> RsaPublicKeyImpl.extract(tlv, format));
            }); // end forEach(input -> ...)
    // end --- b.

    return true;
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#extract(ConstructedBerTlv, EafiRsaPukFormat)} where
   * format equals {@link EafiRsaPukFormat#PKCS1}.
   */
  private boolean zzTestExtractConstructedBerTlvPkcs1() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. happy case with manually chosen values
    // --- b. ERROR: wrong tags, missing values, ...

    final EafiRsaPukFormat format = EafiRsaPukFormat.PKCS1;

    // --- a. happy case with manually chosen values
    Map.ofEntries(
            Map.entry(
                "30-06  [(02-01-1b) (02-01-11)]", // structure as expected
                new BigInteger[] {BigInteger.valueOf(27), BigInteger.valueOf(17)}),
            Map.entry(
                "30-06  [(02-02-b341) (02-02-0081)]", // MSBit set, leading '00'
                new BigInteger[] {
                  BigInteger.valueOf(0xffffffffffffb341L), BigInteger.valueOf(0x81)
                }),
            Map.entry(
                "30-0a  [(02-03-00b341) (02-02-0081) 02-01-11)]", // only the first two numbers
                // count
                new BigInteger[] {BigInteger.valueOf(0xb341), BigInteger.valueOf(0x81)}))
        .forEach(
            (input, expected) -> {
              final var tlv = (ConstructedBerTlv) BerTlv.getInstance(input);
              final var actual = RsaPublicKeyImpl.extract(tlv, format);

              assertArrayEquals(expected, actual, input);
            }); // end forEach((input, expected) -> ...)
    // end --- a.

    // --- b. ERROR: wrong tags, missing values, ...
    Set.of(
            "31-03 [(02-01-1b) (02-01-03)]", // outer tag wrong
            "30-03 [(82-01-1b) (02-01-03)]", // n missing
            "30-03 [(02-01-1b) (92-01-03)]" // e missing
            )
        .forEach(
            input -> {
              final ConstructedBerTlv tlv = (ConstructedBerTlv) BerTlv.getInstance(input);

              assertThrows(
                  IllegalArgumentException.class, () -> RsaPublicKeyImpl.extract(tlv, format));
            }); // end forEach(input -> ...)
    // end --- b.

    return true;
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#extract(ConstructedBerTlv, EafiRsaPukFormat)} where
   * format equals {@link EafiRsaPukFormat#RFC5280}.
   */
  private boolean zzTestExtractConstructedBerTlvRfc5280() {
    // Assertions:
    // ... a. extract(ConstructedBerTlv, PKCS#1) works as expected

    // Test strategy:
    // --- a. happy case with manually chosen values
    // --- b. ERROR: wrong tags, missing values, ...

    final EafiRsaPukFormat format = EafiRsaPukFormat.RFC5280;

    // --- a. happy case with manually chosen values
    Map.ofEntries(
            // structure as expected
            Map.entry(
                """
                    30-1a
                       30-0d
                          06-09-2a864886f70d010101
                          05-00
                       03-09-00 {30-06  [(02-01-1b) (02-01-11)]}
                    """,
                new BigInteger[] {BigInteger.valueOf(27), BigInteger.valueOf(17)}),
            // structure as expected, but different order
            Map.entry(
                """
                    30-1a
                       03-09-00 {30-06  [(02-01-1c) (02-01-11)]}
                       30-0d
                          05-00
                          06-09-2a864886f70d010101
                    """,
                new BigInteger[] {BigInteger.valueOf(28), BigInteger.valueOf(17)}))
        .forEach(
            (input, expected) -> {
              final ConstructedBerTlv tlv = (ConstructedBerTlv) BerTlv.getInstance(input);
              final BigInteger[] present = RsaPublicKeyImpl.extract(tlv, format);

              assertArrayEquals(expected, present, input);
            }); // end forEach((input, expected) -> ...)
    // end --- a.

    // --- b. ERROR: wrong tags, missing values, ...
    Set.of(
            """
                b0-1a
                   30-0d
                      06-09-2a864886f70d010101
                      05-00
                   03-09-00 {30-06  [(02-01-1b) (02-01-11)]}
                """, // outer tag
            """
                30-1a
                   b0-0d
                      06-09-2a864886f70d010101
                      05-00
                   03-09-00 {30-06  [(02-01-1b) (02-01-11)]}
                """, // sequence with algorithm is missing
            """
                30-1a
                   30-0d
                      06-09-2a864886f70d010102
                      05-00
                   03-09-00 {30-06  [(02-01-1b) (02-01-11)]}
                """, // OID wrong
            """
                30-1a
                   30-0d
                      86-09-2a864886f70d010101
                      05-00
                   03-09-00 {30-06  [(02-01-1b) (02-01-11)]}
                """, // OID missing
            """
                30-1a
                   30-0d
                      06-09-2a864886f70d010101
                      85-00
                   03-09-00 {30-06  [(02-01-1b) (02-01-11)]}
                """, // NULL missing
            """
                30-1b
                   30-0e
                      06-09-2a864886f70d010101
                      85-01-72
                   03-09-00 {30-06  [(02-01-1b) (02-01-11)]}
                """, // NULL not empty
            """
                30-1a
                   30-0d
                      06-09-2a864886f70d010101
                      05-00
                   83-09-00 {30-06  [(02-01-1b) (02-01-11)]}
                """) // BitString missing
        .forEach(
            input -> {
              final ConstructedBerTlv tlv = (ConstructedBerTlv) BerTlv.getInstance(input);

              assertThrows(
                  IllegalArgumentException.class, () -> RsaPublicKeyImpl.extract(tlv, format));
            }); // end forEach(input -> ...)
    // end --- b.

    return true;
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#getEncoded(EafiRsaPukFormat)} with {@link
   * EafiRsaPukFormat#ISO7816}.
   */
  private boolean zzTestGetEncodedIso7816() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. more manually chosen values

    final EafiRsaPukFormat format = EafiRsaPukFormat.ISO7816;

    // --- a. smoke test
    {
      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(BigInteger.TEN, BigInteger.TWO);

      assertEquals("7f49 06  81 01 0a  82 01 02", dut.getEncoded(format).toString(" "));
    } // end --- a.

    // --- b. more manually chosen values
    Map.ofEntries(
            // MSBit not set
            Map.entry(
                new RsaPublicKeyImpl(BigInteger.valueOf(0x1b), BigInteger.valueOf(0x11)),
                "7f49 06  81 01 1b  82 01 11"),
            // MSBit set
            Map.entry(
                new RsaPublicKeyImpl(BigInteger.valueOf(0xf3), BigInteger.valueOf(0x81)),
                "7f49 06  81 01 f3  82 01 81"))
        .forEach(
            (puk, expected) ->
                assertEquals(
                    expected,
                    puk.getEncoded(format).toString(" "))); // end forEach((puk, expected) -> ...)
    // end --- b.

    return true;
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#getEncoded(EafiRsaPukFormat)} with {@link
   * EafiRsaPukFormat#PKCS1}.
   */
  private boolean zzTestGetEncodedPkcs1() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. more manually chosen values

    final EafiRsaPukFormat format = EafiRsaPukFormat.PKCS1;

    // --- a. smoke test
    {
      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(BigInteger.TEN, BigInteger.TWO);

      assertEquals("30 06  02 01 0a  02 01 02", dut.getEncoded(format).toString(" "));
    } // end --- a.

    // --- b. more manually chosen values
    Map.ofEntries(
            // MSBit not set
            Map.entry(
                new RsaPublicKeyImpl(BigInteger.valueOf(0x1b), BigInteger.valueOf(0x11)),
                "30 06  02 01 1b  02 01 11"),
            // MSBit set
            Map.entry(
                new RsaPublicKeyImpl(BigInteger.valueOf(0xf3), BigInteger.valueOf(0x81)),
                "30 08  02 02 00f3  02 02 0081"))
        .forEach(
            (puk, expected) ->
                assertEquals(
                    expected,
                    puk.getEncoded(format).toString(" "))); // end forEach((puk, expected) -> ...)
    // end --- b.

    return true;
  } // end method */

  /**
   * Test method for {@link RsaPublicKeyImpl#getEncoded(EafiRsaPukFormat)} with {@link
   * EafiRsaPukFormat#RFC5280}.
   */
  private boolean zzTestGetEncodedRfc5280() {
    // Assertions:
    // ... a. RsaPublicKeyImpl(BigInteger, BigInteger)-constructor works as expected
    // ... b. getEncoded(PKCS1)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. randomly generated keys

    final EafiRsaPukFormat format = EafiRsaPukFormat.RFC5280;

    // --- a. smoke test
    {
      final RsaPublicKeyImpl dut = new RsaPublicKeyImpl(BigInteger.TEN, BigInteger.TWO);

      assertEquals(
          "30 1a  30 0d  06 09 2a864886f70d010101  05 00  03 09 00300602010a020102",
          dut.getEncoded(format).toString(" "));
    } // end --- a.

    // --- b. randomly generated keys
    {
      KEY_PAIR_GENERATOR.initialize(MIN_LENGTH); // small key-size for fast operations

      IntStream.range(0, 10)
          .forEach(
              i -> {
                final var puk = (RSAPublicKey) KEY_PAIR_GENERATOR.generateKeyPair().getPublic();
                final var dut = new RsaPublicKeyImpl(puk);
                final var expected = BerTlv.getInstance(puk.getEncoded()).toStringTree();

                final var actual = dut.getEncoded(format).toStringTree();

                assertEquals(expected, actual);
              }); // end forEach(i -> ...)
    } // end --- b.

    return true;
  } // end method */
} // end method */
