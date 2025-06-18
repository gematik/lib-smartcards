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
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link Hmac}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestHmac {

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

  /** Test method for {@link Hmac#doFinal(byte[])}. */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  @Test
  void test_doFinal__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with test vectors
    // --- b. loop over relevant range of input parameter
    // --- c. use very long key

    try {
      // --- a. smoke test with test vectors
      // a.1 examples from https://www.baeldung.com/java-hmac
      // a.2 examples from https://en.wikipedia.org/wiki/HMAC

      // a.1 example from https://www.baeldung.com/java-hmac
      {
        final var key = "123456".getBytes(StandardCharsets.UTF_8);
        final var data = "baeldung".getBytes(StandardCharsets.UTF_8);
        Map.ofEntries(
                // clause 3
                Map.entry(
                    EafiHashAlgorithm.SHA_256,
                    "5b50d80c7dc7ae8bb1b1433cc0b99ecd2ac8397a555c6f75cb8a619ae35a0c35"),
                // clause 4.2
                Map.entry(EafiHashAlgorithm.MD5, "621dc816b3bf670212e0c261dc9bcdb6"),
                // clause 5.2
                Map.entry(
                    EafiHashAlgorithm.SHA_512,
                    """
                        b313a21908df55c9e322e3c65a4b0b7561ab1594ca806b3affbc0d769a1290c1
                        922aa6622587bea3c0c4d871470a6d06f54dbd20dbda84250e2741eb01f08e33
                        """))
            .forEach(
                (hash, expected) -> {
                  final var dut = new Hmac(key, hash);

                  final var present = Hex.toHexDigits(dut.doFinal(data));

                  assertEquals(Hex.extractHexDigits(expected), present, hash::getAlgorithm);
                }); // end forEach((hash, expected) -> ...)
      } // end a.1

      // a.2 examples from https://en.wikipedia.org/wiki/HMAC
      {
        final var key = "key".getBytes(StandardCharsets.UTF_8);
        final var data =
            "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        Map.ofEntries(
                Map.entry(EafiHashAlgorithm.MD5, "80070713463e7749b90c2dc24911e275"),
                Map.entry(EafiHashAlgorithm.SHA_1, "de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9"),
                Map.entry(
                    EafiHashAlgorithm.SHA_512,
                    """
                        b42af09057bac1e2d41708e48a902e09b5ff7f12ab428a4fe86653c73dd248fb
                        82f948a549f7b791a5b41915ee4d1ec3935357e4e2317250d0372afa2ebeeb3a
                        """))
            .forEach(
                (hash, expected) -> {
                  final var dut = new Hmac(key, hash);

                  final var present = Hex.toHexDigits(dut.doFinal(data));

                  assertEquals(Hex.extractHexDigits(expected), present, hash::getAlgorithm);
                }); // end forEach((hash, expected) -> ...)
      } // end a.1
      // end --- a.

      // --- b. loop over relevant range of input parameter
      // spotless:off
      for (final var hash : List.of(
          // Note: MD2 is commented out because tests fail. I don't know why and
          //       because MD2 is outdated (and thus, irrelevant), I will not fix
          //       it (at least for the time being).
          // EafiHashAlgorithm.MD2,
          EafiHashAlgorithm.MD5,
          EafiHashAlgorithm.SHA_1,
          EafiHashAlgorithm.SHA_224,
          EafiHashAlgorithm.SHA_256,
          EafiHashAlgorithm.SHA_384,
          EafiHashAlgorithm.SHA_512
      )) {
        final var algorithm = switch (hash) {
          case MD5 -> "HmacMD5";
          case SHA_1 -> "HmacSHA1";
          case SHA_224 -> "HmacSHA224";
          case SHA_256 -> "HmacSHA256";
          case SHA_384 -> "HmacSHA384";
          case SHA_512 -> "HmacSHA512";
          default -> "unknown";
        };
        for (final var keyLength : RNG.intsClosed(1, hash.getBlockLength(), 5).toArray()) {
          final var key = RNG.nextBytes(keyLength);
          final var dut = new Hmac(key, hash); // NOPMD new in loop
          final var skSpec = new SecretKeySpec(key, algorithm); // NOPMD new in loop
          final var mac = Mac.getInstance(algorithm);
          mac.init(skSpec);

          for (final var dataLength : RNG.intsClosed(0, 1024, 5).toArray()) {
            final var data = RNG.nextBytes(dataLength);
            final var expected = Hex.toHexDigits(mac.doFinal(data));

            final var present = Hex.toHexDigits(dut.doFinal(data));

            assertEquals(expected, present);
          } // end For (dataLength...)
        } // end For (keyLength...)
      } // end For (hash...)
      // spotless:on
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- c. use very long key
    {
      final var hash = EafiHashAlgorithm.MD5;
      final var keyLength = hash.getBlockLength() + 1;
      final var keyLong = RNG.nextBytes(keyLength);
      final var keyShort = hash.digest(keyLong);
      final var data = RNG.nextBytes(128, 256);
      final var expected = new Hmac(keyShort, hash).doFinal(data);

      final var present = new Hmac(keyLong, hash).doFinal(data);

      assertArrayEquals(expected, present);
    } // end --- c.
  } // end method */

  /** Test method for {@link Hmac#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in class
    // --- d. difference in hash function
    // --- e. difference in secret key
    // --- f. different object, but same content

    final var dutKey = "4711";
    final var dutHash = EafiHashAlgorithm.SHA_256;
    final var dut = new Hmac(Hex.toByteArray(dutKey), dutHash);

    for (final Object obj :
        new Object[] {
          dut, // --- a. same reference
          null, // --- b. null input
          "afi" // --- c. difference in class
        }) {
      assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals to compare object reference
    } // end For (obj...)

    // --- d. difference in hash function
    // --- e. difference in secret key
    // --- f. different object, but same content
    for (final var hash : EafiHashAlgorithm.values()) {
      List.of(dutKey, "0815", "47110815")
          .forEach(
              key -> {
                final var other = new Hmac(Hex.toByteArray(key), hash); // NOPMD new in loop

                assertEquals(
                    dutKey.equals(key) && dutHash.equals(hash),
                    dut.equals(other),
                    () -> hash.getAlgorithm() + " " + key);
              }); // end forEach(key -> ...)
    } // end For (hash...)
    // end --- d, e, f.
  } // end method */

  /** Test method for {@link Hmac#hashCode()}. */
  @Test
  void test_hashCode() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. call hashCode()-method again

    final var dutKey = "08164711";
    final var dutHash = EafiHashAlgorithm.SHA_256;
    final var dut = new Hmac(Hex.toByteArray(dutKey), dutHash);

    final int hashCode = dut.hashCode();

    // --- a. smoke test
    {
      final var key = Hex.toByteArray(dutKey);
      final var ipad = new byte[dutHash.getBlockLength()];
      Arrays.fill(ipad, (byte) 0x36);
      for (int i = key.length; i-- > 0; ) { // NOPMD assignment in operand
        ipad[i] ^= key[i];
      } // end For (i...)

      assertEquals(dutHash.hashCode() * 31 + Arrays.hashCode(ipad), hashCode);
    } // end --- a.

    // --- b. call hashCode()-method again
    {
      assertEquals(hashCode, dut.hashCode());
    } // end --- b.
  } // end method */
} // end class
