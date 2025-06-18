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

import static de.gematik.smartcards.crypto.AesKey.AES_CBC_NO_PADDING;
import static de.gematik.smartcards.crypto.AesKey.BLOCK_LENGTH;
import static de.gematik.smartcards.crypto.AesKey.INFIMUM_CMAC_LENGTH;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link AesKey}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NONNULL_PARAM_VIOLATION" i.e.,
//         Spotbugs message: This method passes a null value as the parameter of
//             a method which must be non-null.
//         Rational: The finding is correct, but here, intentionally the
//             parameter is null.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_PARAM_VIOLATION" // see note 1
}) // */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestAesKey {

  /**
   * {@link BouncyCastleProvider} for cryptographic functions.
   *
   * <p>The JDK does not support {@link EafiHashAlgorithm#RIPEMD_160}.
   *
   * <p>Because some tests use the underlying Java Runtime Environment here we use {@link
   * BouncyCastleProvider}.
   */
  private static final BouncyCastleProvider BC = new BouncyCastleProvider(); // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Key type. */
  private static final String KEY_TYPE = "AES"; // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    Security.addProvider(BC);
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

  /** Test method for {@link AesKey#AesKey(byte[], int, int)}. */
  @Test
  void test_AesKey__byteA_int_int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with valid key-length
    // --- b. ERROR: key is null
    // --- c. ERROR: "offset + len" too big
    // --- d. ERROR: "offset" too small
    // --- e. ERROR: invalid key-length

    final var keyMaterial = RNG.nextBytes(64);

    // --- a. smoke test with valid key-length
    for (final var len : Set.of(16, 24, 32)) {
      final var maxOffset = keyMaterial.length - len;
      for (final var offset : RNG.intsClosed(0, maxOffset, 5).toArray()) {
        assertDoesNotThrow(() -> new AesKey(keyMaterial, offset, len)); // NOPMD new in loop
      } // end For (offset...)
    } // end For (len...)
    // end --- a.

    // --- b. ERROR: key is null
    // NP_NONNULL_PARAM_VIOLATION
    assertThrows(IllegalArgumentException.class, () -> new AesKey(null, 0, 16));
    // end --- b.

    // --- c. ERROR: "offset + len" too big
    for (final var len : Set.of(16, 24, 32)) {
      final var offset = keyMaterial.length - len + 1;

      assertThrows(
          IllegalArgumentException.class,
          () -> new AesKey(keyMaterial, offset, len), // NOPMD new in loop
          () -> Integer.toString(offset));
    } // end For (len...)

    // --- d. ERROR: "offset" too small
    assertThrows(ArrayIndexOutOfBoundsException.class, () -> new AesKey(keyMaterial, -1, 16));
    // end --- d.

    // --- e. ERROR: invalid key-length
    Set.of(15, 17, 23, 25, 31, 33)
        .forEach(
            len ->
                assertThrows(
                    IllegalArgumentException.class, () -> new AesKey(keyMaterial, 0, len)));
    // end --- e.
  } // end method */

  /** Test method for {@link AesKey#calculateCmac(byte[])}. */
  @Test
  void test_calculateCmac__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter

    try {
      final var mac = Mac.getInstance("AESCMAC", BC);
      final var keyMaterial = RNG.nextBytes(64);

      // --- a. smoke test
      {
        final var len = 16;
        final var offset = 3;
        final var dut = new AesKey(keyMaterial, offset, len);
        final var message = RNG.nextBytes(16, 64);

        final var actual = dut.calculateCmac(message);

        mac.init(new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE));
        final var expected = mac.doFinal(message);
        assertArrayEquals(expected, actual);
      }
      // end --- a.

      // --- b. loop over relevant range of input parameter
      for (final var len : Set.of(16, 24, 32)) {
        final var maxOffset = keyMaterial.length - len;
        for (final var offset : RNG.intsClosed(0, maxOffset, 5).toArray()) {
          final var dut = new AesKey(keyMaterial, offset, len); // NOPMD new in loop
          mac.init(new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE)); // NOPMD new in loop
          for (final var mLen : RNG.intsClosed(0, 1024, 5).toArray()) {
            final var message = RNG.nextBytes(mLen);

            final var actual = dut.calculateCmac(message);

            final var expected = mac.doFinal(message);
            assertArrayEquals(expected, actual);
          } // for (mLen...)
        } // end For (offset...)
      } // end For (len...)
      // end --- b.
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link AesKey#calculateCmac(byte[], int)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_calculateCmac__byteA_int() {
    // Assertions:
    // ... a. calculateCmac(byte[])-method works as expected

    // Note: Because of assertion "a" we can be lazy here and
    //       concentrate on corner cases.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter

    try {
      final var mac = Mac.getInstance("AESCMAC", BC);
      final var keyMaterial = RNG.nextBytes(64);

      // --- a. smoke test
      {
        final var len = 16;
        final var offset = 3;
        final var dut = new AesKey(keyMaterial, offset, len);
        final var message = RNG.nextBytes(16, 64);
        final var macLen = 8;

        final var actual = dut.calculateCmac(message, macLen);

        mac.init(new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE));
        final var expected = Arrays.copyOfRange(mac.doFinal(message), 0, macLen);
        assertArrayEquals(expected, actual);
      }
      // end --- a.

      // --- b. loop over relevant range of input parameter
      for (final var len : Set.of(16, 24, 32)) {
        final var maxOffset = keyMaterial.length - len;
        for (final var offset : RNG.intsClosed(0, maxOffset, 5).toArray()) {
          final var dut = new AesKey(keyMaterial, offset, len); // NOPMD new in loop
          mac.init(new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE)); // NOPMD new in loop
          for (final var mLen : RNG.intsClosed(0, 1024, 5).toArray()) {
            final var message = RNG.nextBytes(mLen);

            for (var macLen : RNG.intsClosed(7, BLOCK_LENGTH + 1, 5).toArray()) {
              if (macLen < INFIMUM_CMAC_LENGTH) {
                macLen = INFIMUM_CMAC_LENGTH; // NOPMD reassignment of loop control variable
              } // end fi
              if (macLen > BLOCK_LENGTH) {
                macLen = BLOCK_LENGTH; // NOPMD reassignment of loop control variable
              } // end fi

              final var actual = dut.calculateCmac(message, macLen);

              final var expected = Arrays.copyOfRange(mac.doFinal(message), 0, macLen);
              assertArrayEquals(expected, actual);
            } // end For (macLen...)
          } // for (mLen...)
        } // end For (offset...)
      } // end For (len...)
      // end --- b.
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link AesKey#verifyCmac(byte[], byte[])}. */
  @Test
  void test_verifyCmac__byteA_byteA() {
    // Assertions:
    // ... a. calculateCmac(byte[], int)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. CMAC too short
    // --- d. CMAC too long
    // --- e. CMAC wrong

    final var keyMaterial = RNG.nextBytes(64);

    // --- a. smoke test
    {
      final var len = 16;
      final var offset = 3;
      final var dut = new AesKey(keyMaterial, offset, len);
      final var message = RNG.nextBytes(16, 64);
      final var macLen = 10;
      final var cmac = dut.calculateCmac(message, macLen);

      assertTrue(dut.verifyCmac(message, cmac));
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    for (final var len : Set.of(16, 24, 32)) {
      final var maxOffset = keyMaterial.length - len;
      for (final var offset : RNG.intsClosed(0, maxOffset, 5).toArray()) {
        final var dut = new AesKey(keyMaterial, offset, len); // NOPMD new in loop

        for (final var mLen : RNG.intsClosed(0, 1024, 5).toArray()) {
          final var message = RNG.nextBytes(mLen);
          final var cmac = dut.calculateCmac(message);

          for (final var macLen : RNG.intsClosed(INFIMUM_CMAC_LENGTH, BLOCK_LENGTH, 5).toArray()) {
            final var actual = Arrays.copyOfRange(cmac, 0, macLen);

            assertTrue(dut.verifyCmac(message, actual));
          } // end For (macLen...)

          // --- c. CMAC too short
          assertFalse(
              dut.verifyCmac(message, Arrays.copyOfRange(cmac, 0, INFIMUM_CMAC_LENGTH - 1)));

          // --- d. CMAC too long
          assertFalse(dut.verifyCmac(message, Arrays.copyOfRange(cmac, 0, BLOCK_LENGTH + 1)));

          // --- e. CMAC wrong
          cmac[0]++;
          assertFalse(dut.verifyCmac(message, cmac));
          // end --- e.
        } // for (mLen...)
      } // end For (offset...)
    } // end For (len...)
    // end --- b, c, d, e.
  } // end method */

  /** Test method for {@link AesKey#decipherCbc(byte[])}. */
  @Test
  void test_decipherCbc__byteA() {
    // Assertion:
    // ... a. decipherCbc(byte[], byte[])-method works as expected

    // Note: Because of assertion "a" we can be lazy here and
    //       concentrate on code coverage during JUnit tests.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: length of ciphertext not a multiple of 16

    try {
      final var cipher = Cipher.getInstance(AES_CBC_NO_PADDING);
      final var keyMaterial = RNG.nextBytes(64);
      final var len = 16;
      final var offset = 5;
      final var dut = new AesKey(keyMaterial, offset, len);
      final var iv = new byte[BLOCK_LENGTH];
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE),
          new IvParameterSpec(iv));

      // --- a. smoke test
      {
        final var mLen = 2 * BLOCK_LENGTH;
        final var expected = RNG.nextBytes(mLen);
        final var ciphertext = cipher.doFinal(expected);

        final var actual = dut.decipherCbc(ciphertext);

        assertArrayEquals(expected, actual);
      } // end --- a.

      // --- b. ERROR: length of ciphertext not a multiple of 16
      {
        final var mLen = 2 * BLOCK_LENGTH - 1;
        final var ciphertext = RNG.nextBytes(mLen);

        final var throwable =
            assertThrows(IllegalArgumentException.class, () -> dut.decipherCbc(ciphertext, iv));
        assertEquals("invalid ciphertext.length", throwable.getMessage());
      } // end --- b.
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link AesKey#decipherCbc(byte[], byte[])}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_decipherCbc__byteA_byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. ERROR: length of ciphertext not a multiple of 16
    // --- d. ERROR: length of IV not 16

    try {
      final var cipher = Cipher.getInstance(AES_CBC_NO_PADDING);
      final var keyMaterial = RNG.nextBytes(64);

      // --- a. smoke test
      {
        final var len = 16;
        final var offset = 5;
        final var dut = new AesKey(keyMaterial, offset, len);
        final var iv = RNG.nextBytes(BLOCK_LENGTH);
        cipher.init(
            Cipher.ENCRYPT_MODE,
            new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE),
            new IvParameterSpec(iv));
        final var expected = RNG.nextBytes(RNG.nextIntClosed(1, 10) * BLOCK_LENGTH);
        final var ciphertext = cipher.doFinal(expected);

        final var actual = dut.decipherCbc(ciphertext, iv);

        assertArrayEquals(expected, actual);
      } // end --- a.

      // --- b. loop over relevant range of input parameter
      for (final var len : Set.of(16, 24, 32)) {
        final var maxOffset = keyMaterial.length - len;
        for (final var offset : RNG.intsClosed(0, maxOffset, 5).toArray()) {
          final var dut = new AesKey(keyMaterial, offset, len); // NOPMD new in loop
          for (final var mLen : RNG.intsClosed(0, 1024, 5).toArray()) {
            final var iv = RNG.nextBytes(BLOCK_LENGTH);
            cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE), // NOPMD new in loop
                new IvParameterSpec(iv)); // NOPMD new in loop
            final var expected = RNG.nextBytes(mLen * BLOCK_LENGTH);
            final var ciphertext = cipher.doFinal(expected);

            final var actual = dut.decipherCbc(ciphertext, iv);

            assertArrayEquals(expected, actual);
          } // for (mLen...)

          // --- c. ERROR: length of ciphertext not a multiple of 16
          {
            final var iv = RNG.nextBytes(BLOCK_LENGTH);

            for (int mlen = 64; mlen > 0; mlen--) {
              if (0 == (mlen % BLOCK_LENGTH)) {
                continue;
              } // end fi

              final var ciphertext = RNG.nextBytes(mlen);

              final var throwable =
                  assertThrows(
                      IllegalArgumentException.class, () -> dut.decipherCbc(ciphertext, iv));
              assertEquals("invalid ciphertext.length", throwable.getMessage());
            } // end For (mLen...)
          } // end --- c.

          // --- d. ERROR: length of IV not 16
          {
            final var ciphertext = RNG.nextBytes(5 * BLOCK_LENGTH);

            for (int ivLen = 32; ivLen > 0; ivLen--) {
              if (BLOCK_LENGTH == ivLen) {
                continue;
              } // end fi

              final var iv = RNG.nextBytes(ivLen);

              final var throwable =
                  assertThrows(
                      IllegalArgumentException.class, () -> dut.decipherCbc(ciphertext, iv));
              assertEquals("invalid ICV.length", throwable.getMessage());
            } // end For (mLen...)
          } // end --- d.
        } // end For (offset...)
      } // end For (len...)
      // end --- b, c, d.
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link AesKey#encipherCbc(byte[])}. */
  @Test
  void test_encipherCbc__byteA() {
    // Assertion:
    // ... a. encipherCbc(byte[], byte[])-method works as expected

    // Note: Because of assertion "a" we can be lazy here and
    //       concentrate on code coverage during JUnit tests.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: length of plaintext not a multiple of 16

    try {
      final var cipher = Cipher.getInstance(AES_CBC_NO_PADDING);
      final var keyMaterial = RNG.nextBytes(64);
      final var len = 16;
      final var offset = 5;
      final var dut = new AesKey(keyMaterial, offset, len);
      final var iv = new byte[BLOCK_LENGTH];
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE),
          new IvParameterSpec(iv));

      // --- a. smoke test
      {
        final var mLen = 2 * BLOCK_LENGTH;
        final var message = RNG.nextBytes(mLen);
        final var expected = cipher.doFinal(message);

        final var actual = dut.encipherCbc(message);

        assertArrayEquals(expected, actual);
      } // end --- a.

      // --- b. ERROR: length of plaintext not a multiple of 16
      {
        final var mLen = 2 * BLOCK_LENGTH - 1;
        final var message = RNG.nextBytes(mLen);

        final var throwable =
            assertThrows(IllegalArgumentException.class, () -> dut.encipherCbc(message, iv));
        assertEquals("invalid plaintext.length", throwable.getMessage());
      } // end --- b.
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link AesKey#encipherCbc(byte[], byte[])}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_encipherCbc__byteA_byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. ERROR: length of ciphertext not a multiple of 16
    // --- d. ERROR: length of IV not 16

    try {
      final var cipher = Cipher.getInstance(AES_CBC_NO_PADDING);
      final var keyMaterial = RNG.nextBytes(64);

      // --- a. smoke test
      {
        final var len = 16;
        final var offset = 5;
        final var dut = new AesKey(keyMaterial, offset, len);
        final var iv = RNG.nextBytes(BLOCK_LENGTH);
        cipher.init(
            Cipher.ENCRYPT_MODE,
            new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE),
            new IvParameterSpec(iv));
        final var message = RNG.nextBytes(RNG.nextIntClosed(1, 10) * BLOCK_LENGTH);
        final var expected = cipher.doFinal(message);

        final var actual = dut.encipherCbc(message, iv);

        assertArrayEquals(expected, actual);
      } // end --- a.

      // --- b. loop over relevant range of input parameter
      for (final var len : Set.of(16, 24, 32)) {
        final var maxOffset = keyMaterial.length - len;
        for (final var offset : RNG.intsClosed(0, maxOffset, 5).toArray()) {
          final var dut = new AesKey(keyMaterial, offset, len); // NOPMD new in loop
          for (final var mLen : RNG.intsClosed(0, 1024, 5).toArray()) {
            final var iv = RNG.nextBytes(BLOCK_LENGTH);
            cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyMaterial, offset, len, KEY_TYPE), // NOPMD new in loop
                new IvParameterSpec(iv)); // NOPMD new in loop
            final var message = RNG.nextBytes(mLen * BLOCK_LENGTH);
            final var expected = cipher.doFinal(message);

            final var actual = dut.encipherCbc(message, iv);

            assertArrayEquals(expected, actual);
          } // for (mLen...)

          // --- c. ERROR: length of ciphertext not a multiple of 16
          {
            final var iv = RNG.nextBytes(BLOCK_LENGTH);

            for (int mlen = 64; mlen > 0; mlen--) {
              if (0 == (mlen % BLOCK_LENGTH)) {
                continue;
              } // end fi

              final var ciphertext = RNG.nextBytes(mlen);

              final var throwable =
                  assertThrows(
                      IllegalArgumentException.class, () -> dut.encipherCbc(ciphertext, iv));
              assertEquals("invalid plaintext.length", throwable.getMessage());
            } // end For (mLen...)
          } // end --- c.

          // --- d. ERROR: length of IV not 16
          {
            final var ciphertext = RNG.nextBytes(5 * BLOCK_LENGTH);

            for (int ivLen = 32; ivLen > 0; ivLen--) {
              if (BLOCK_LENGTH == ivLen) {
                continue;
              } // end fi

              final var iv = RNG.nextBytes(ivLen);

              final var throwable =
                  assertThrows(
                      IllegalArgumentException.class, () -> dut.encipherCbc(ciphertext, iv));
              assertEquals("invalid ICV.length", throwable.getMessage());
            } // end For (mLen...)
          } // end --- d.
        } // end For (offset...)
      } // end For (len...)
      // end --- b, c, d.
    } catch (BadPaddingException
        | IllegalBlockSizeException
        | InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | NoSuchPaddingException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Smoke test method for GCM encipher and decipher. */
  @Test
  void test_enc_dec_Gcm() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    {
      final var keyLength = 16;
      final var dut = new AesKey(RNG.nextBytes(keyLength), 0, keyLength);
      final var nonce = RNG.nextBytes(12);
      final var plaintext = RNG.nextBytes(64, 128);
      final var associatedData = RNG.nextBytes(4, 32);
      final var tagLength = 120; // bit

      final var ciphertext = dut.encipherGcm(plaintext, nonce, associatedData, tagLength);

      final var pt2 = dut.decipherGcm(ciphertext, nonce, associatedData, tagLength);

      assertEquals(Hex.toHexDigits(plaintext), Hex.toHexDigits(pt2));
    } // end --- a.
  } // end method */

  /** Test method for {@link AesKey#decipherGcm(byte[], byte[], byte[], int)}. */
  @Test
  void test_decipherGcm__byteA3_int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test from file "gcmDecrypt128.rsp", line 15_150
    // --- b. loop over test vectors from directory "gcmtestvectors"
    // --- c. ERROR: invalid tagLength

    // --- a. smoke test from file "gcmDecrypt128.rsp", line 15_150
    {
      final var key = Hex.toByteArray("c5bd2929d96d7e247fa3ad4a80593569");
      final var iv = Hex.toByteArray("1f71abed1ec330a593df5406");
      final var ct = "737156a3cfca8609b88097b18697b6d2b787b187691027ff4c42be891647ba55";
      final var aad = Hex.toByteArray("83eeaf36f905e770a3c7a6a507dcc1aaaba7a3b8");
      final var tag = "8266e858d68edfe3ed7c4f228fb6cf";
      final var pt = "d4a28243297b564e01985dc1b6cd3ebfb99aba0debf9716fcecf40863d817f56";
      final var dut = new AesKey(key, 0, key.length);

      final var present =
          Hex.toHexDigits(dut.decipherGcm(Hex.toByteArray(ct + tag), iv, aad, tag.length() << 2));

      assertEquals(pt, present);
    } // end --- a.

    final var root = Paths.get(".").toAbsolutePath().normalize();
    try (Stream<Path> walker = Files.walk(root)) {
      final var inputDir =
          walker
              .filter(Files::isDirectory)
              .filter(p -> "/gcmtestvectors".equals("/" + p.getFileName()))
              .findAny()
              .orElseThrow();

      // --- b. loop over test vectors from directory "gcmtestvectors"
      for (final int keyLength : new int[] {128, 192, 256}) {
        final var inputPath = inputDir.resolve("gcmDecrypt" + keyLength + ".rsp");
        assertTrue(Files.isRegularFile(inputPath));
        final var iterator = Files.readAllLines(inputPath, StandardCharsets.UTF_8).iterator();

        while (iterator.hasNext()) {
          final var line = iterator.next().trim();

          if (line.startsWith("Key = ")) {
            final var key = Hex.toByteArray(line.substring(5));
            final var iv = Hex.toByteArray(iterator.next().substring(5));
            final var ct = iterator.next().substring(5).trim();
            final var aad = Hex.toByteArray(iterator.next().substring(5));
            final var tag = iterator.next().substring(5).trim();
            final var result = iterator.next().trim();
            final var dut = new AesKey(key, 0, key.length); // NOPMD new in loop

            if ("FAIL".equals(result)) { // NOPMD literal in if statement
              final var throwable =
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> dut.decipherGcm(Hex.toByteArray(ct + tag), iv, aad, tag.length() << 2));

              assertEquals("tag mismatch", throwable.getMessage());
              assertNull(throwable.getCause());
            } else {
              final var pt = result.substring(4).trim();

              final var present =
                  Hex.toHexDigits(
                      dut.decipherGcm(Hex.toByteArray(ct + tag), iv, aad, tag.length() << 2));

              assertEquals(pt, present);
            } // end else
          } // end fi
        } // end While (has next)
      } // end For (keyLength...)
      // end --- b.

      // --- c. ERROR: invalid tagLength
      {
        final var key = RNG.nextBytes(16);
        final var dut = new AesKey(key, 0, key.length);

        assertThrows(
            IllegalArgumentException.class,
            () -> dut.decipherGcm(RNG.nextBytes(64), RNG.nextBytes(12), AfiUtils.EMPTY_OS, 0));
      } // end --- c.
    } catch (IOException | NoSuchElementException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- c.
  } // end method */

  /** Test method for {@link AesKey#encipherGcm(byte[], byte[], byte[], int)}. */
  @Test
  void test_encipherGcm__byteA3_int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test from file "gcmEncryptExtIV128.rsp", line 15_638
    // --- b. loop over test vectors from directory "gcmtestvectors"
    // --- c. ERROR: invalid tagLength

    // --- a. smoke test from file "gcmEncryptExtIV128.rsp", line 15_638
    {
      final var key = Hex.toByteArray("653a6208c95313be1c279379eeeb9a37");
      final var iv = Hex.toByteArray("eaf78603d2dcc894e61c20f6");
      final var pt =
          Hex.toByteArray("b51b7d7b43c6f3f5b6a5005e23d76fde51e466af52ee7d50172bc8325f242c97");
      final var aad = Hex.toByteArray("b494bdab6bba3c4286a6d8924b40910c562d9e99");
      final var ct = "7996fe1a4881fb2616d82e93b1e4370d374537c6b14325b9fe90fd60df393c77";
      final var tag = "76b30c4df67057d2";
      final var dut = new AesKey(key, 0, key.length);

      final var present = Hex.toHexDigits(dut.encipherGcm(pt, iv, aad, tag.length() << 2));

      assertEquals(ct + tag, present);
    } // end --- a.

    final var root = Paths.get(".").toAbsolutePath().normalize();
    try (Stream<Path> walker = Files.walk(root)) {
      final var inputDir =
          walker
              .filter(Files::isDirectory)
              .filter(p -> "/gcmtestvectors".equals("/" + p.getFileName()))
              .findAny()
              .orElseThrow();

      // --- b. loop over test vectors from directory "gcmtestvectors"
      for (final int keyLength : new int[] {128, 192, 256}) {
        final var inputPath = inputDir.resolve("gcmEncryptExtIV" + keyLength + ".rsp");
        assertTrue(Files.isRegularFile(inputPath));
        final var iterator = Files.readAllLines(inputPath, StandardCharsets.UTF_8).iterator();

        while (iterator.hasNext()) {
          final var line = iterator.next().trim();

          if (line.startsWith("Key = ")) {
            final var key = Hex.toByteArray(line.substring(5));
            final var iv = Hex.toByteArray(iterator.next().substring(5));
            final var pt = Hex.toByteArray(iterator.next().substring(5));
            final var aad = Hex.toByteArray(iterator.next().substring(5));
            final var ct = iterator.next().substring(5).trim();
            final var tag = iterator.next().substring(5).trim();
            final var dut = new AesKey(key, 0, key.length); // NOPMD new in loop

            final var present = Hex.toHexDigits(dut.encipherGcm(pt, iv, aad, tag.length() << 2));

            assertEquals(ct + tag, present, () -> "Key = " + Hex.toHexDigits(key));
          } // end fi
        } // end While (has next)
      } // end For (keyLength...)
      // end --- b.

      // --- c. ERROR: invalid tagLength
      {
        final var key = RNG.nextBytes(16);
        final var dut = new AesKey(key, 0, key.length);

        assertThrows(
            IllegalArgumentException.class,
            () -> dut.encipherGcm(AfiUtils.EMPTY_OS, RNG.nextBytes(12), AfiUtils.EMPTY_OS, 0));
      } // end --- c.
    } catch (IOException | NoSuchElementException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- c.
  } // end method */

  /**
   * Test method for various methods.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link AesKey#padIso(byte[])}
   *   <li>{@link AesKey#truncateIso(byte[])}
   * </ol>
   */
  @Test
  void test_padIso__truncate__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant of input parameter
    // --- c. strange length for padded octet string
    // --- d. ERROR: invalid separator
    // --- e. ERROR: invalid padding-length
    // --- f. ERROR: short message with invalid padding

    final var dut = new AesKey(RNG.nextBytes(BLOCK_LENGTH), 0, BLOCK_LENGTH);
    final var separator = new byte[] {-128};
    final var zero = new byte[1];

    // --- a. smoke test
    {
      final var input = Hex.toByteArray("0102 0304 0506 0708 09");
      final var expected = Hex.toByteArray("0102 0304 0506 0708 09  80 0000 0000 0000");

      final var actual = dut.padIso(input);

      assertArrayEquals(expected, actual);
    } // end --- a.

    // --- b. loop over relevant of input parameter
    {
      Set.of(
              AfiUtils.EMPTY_OS,
              Hex.toByteArray("80"),
              Hex.toByteArray("af8000"),
              RNG.nextBytes(15),
              RNG.nextBytes(16),
              RNG.nextBytes(17),
              RNG.nextBytes(31),
              RNG.nextBytes(32),
              RNG.nextBytes(33))
          .forEach(
              input -> {
                var expected = AfiUtils.concatenate(input, separator);
                while (0 != (expected.length % BLOCK_LENGTH)) {
                  expected = AfiUtils.concatenate(expected, zero);
                } // end While (...)

                final var actualPad = dut.padIso(input);

                assertArrayEquals(expected, actualPad);

                final var actualTruncate = dut.truncateIso(expected);

                assertArrayEquals(input, actualTruncate);
              }); // end forEach(input -> ...)
    } // end --- b.

    // --- c. strange length for padded octet string
    {
      final var expected = Hex.toByteArray("afbc80");
      final var input = AfiUtils.concatenate(expected, Hex.toByteArray("8000"));

      final var actual = dut.truncateIso(input);

      assertArrayEquals(expected, actual);
    } // end --- c.

    // --- d. ERROR: invalid separator
    IntStream.rangeClosed(0x00, 0xff)
        .forEach(
            sep -> {
              final var message = RNG.nextBytes(0, 32);
              final var input =
                  AfiUtils.concatenate(
                      message,
                      new byte[] {(byte) sep},
                      new byte[RNG.nextIntClosed(1, BLOCK_LENGTH - 1)]);

              if (0x80 == sep) { // NOPMD literal
                // ... correct separator
                final var actual = dut.truncateIso(input);

                assertArrayEquals(message, actual);
              } else {
                // ... wrong separator
                final var throwable =
                    assertThrows(IllegalArgumentException.class, () -> dut.truncateIso(input));
                assertEquals("padding not in accordance to ISO/IEC 7816-4", throwable.getMessage());
              } // end else
            }); // end forEach(sep -> ...)
    // end --- d.

    // --- e. ERROR: invalid padding-length
    {
      final var input =
          AfiUtils.concatenate(Hex.toByteArray("4711"), separator, new byte[BLOCK_LENGTH]);

      final var throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.truncateIso(input));
      assertEquals("padding not in accordance to ISO/IEC 7816-4", throwable.getMessage());
    } // end --- e.

    // --- f. ERROR: short message with invalid padding
    for (int i = BLOCK_LENGTH; i-- > 0; ) { // NOPMD assignment in operand
      final var input = new byte[i]; // NOPMD new in loop

      final var throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.truncateIso(input));
      assertEquals("padding not in accordance to ISO/IEC 7816-4", throwable.getMessage());
    } // end For (i...)
    // end --- f.
  } // end method */
} // end class
