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

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.bouncycastle.crypto.signers.ISOTrailers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box tests for {@link AfiUtils}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "Non-null field is not initialized".
//         for class-attribute "claTempDir".
//         The finding is correct, but suppressed hereafter, because JUnit initializes it.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 1
}) // */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestEafiHashAlgorithm {

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

  /** Temporary Directory. */
  @TempDir
  /* package */ static Path claTempDir; // NOPMD use of non-final, non-private static field */

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

  /** Test method for {@link EafiHashAlgorithm#getInstance(AfiOid)}. */
  @Test
  void test_getInstance__AfiOid() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with all hash algorithms
    // --- b. ERROR: invalid OID

    // --- a. smoke test with all hash algorithms
    {
      assertSame(EafiHashAlgorithm.MD2, EafiHashAlgorithm.getInstance(AfiOid.MD2));
      assertSame(EafiHashAlgorithm.MD5, EafiHashAlgorithm.getInstance(AfiOid.MD5));
      assertSame(EafiHashAlgorithm.RIPEMD_160, EafiHashAlgorithm.getInstance(AfiOid.RIPEMD160));
      assertSame(EafiHashAlgorithm.SHA_1, EafiHashAlgorithm.getInstance(AfiOid.SHA1));
      assertSame(EafiHashAlgorithm.SHA_224, EafiHashAlgorithm.getInstance(AfiOid.SHA224));
      assertSame(EafiHashAlgorithm.SHA_256, EafiHashAlgorithm.getInstance(AfiOid.SHA256));
      assertSame(EafiHashAlgorithm.SHA_384, EafiHashAlgorithm.getInstance(AfiOid.SHA384));
      assertSame(EafiHashAlgorithm.SHA_512, EafiHashAlgorithm.getInstance(AfiOid.SHA512));
    } // end --- a.

    // --- b. ERROR: invalid OID
    {
      assertThrows(
          IllegalArgumentException.class,
          () -> EafiHashAlgorithm.getInstance(AfiOid.brainpoolP256r1));
    } // end --- b.
  } // end method */

  /** Test method for {@link EafiHashAlgorithm#digest(byte[])}. */
  @Test
  void test_digest__byteA() {
    // Assertions:
    // ... a. getMessageDigest()-method works as expected

    // Note 1: The simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. loop over all hash-algorithms and check that correct hash-value is calculated
    for (final EafiHashAlgorithm entry : EafiHashAlgorithm.values()) {
      try {
        final byte[] message = RNG.nextBytes(0, 128);

        // estimate expected hash-value
        final MessageDigest digest = MessageDigest.getInstance(entry.getAlgorithm());
        final byte[] exp = digest.digest(message);
        assertEquals(Hex.toHexDigits(exp), Hex.toHexDigits(entry.digest(message)));
      } catch (NoSuchAlgorithmException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (entry...)
  } // end method */

  /** Test method for {@link EafiHashAlgorithm#digest(java.nio.file.Path)}. */
  @Test
  void test_digest__Path() {
    // Assertions:
    // ... a. getMessageDigest()-method works as expected

    // Test strategy:
    // --- a. loop over all hash-algorithms and check that correct hash-value is calculated
    for (final EafiHashAlgorithm entry : EafiHashAlgorithm.values()) {
      try {
        final byte[] message = RNG.nextBytes(0, 128);

        // store message in a file
        // Note: To make this test thread-safe, the message
        //       has to be stored in a hash-specific file.
        final Path path = claTempDir.resolve("message." + entry.getAlgorithm());
        Files.write(path, message);

        // estimate expected hash-value
        final MessageDigest digest = MessageDigest.getInstance(entry.getAlgorithm());
        final byte[] exp = digest.digest(message);
        assertEquals(Hex.toHexDigits(exp), Hex.toHexDigits(entry.digest(path)));
      } catch (IOException | NoSuchAlgorithmException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (entry...)
  } // end method */

  /** Test method for {@link EafiHashAlgorithm#getAlgorithm()}. */
  @Test
  void test_getAlgorithm() {
    // Note: This simple method needs little testing, so we can be lazy here.

    // Test strategy:
    // --- a. loop over all entries of enumeration
    for (final var entry : EafiHashAlgorithm.values()) {
      try {
        final MessageDigest digest = MessageDigest.getInstance(entry.getAlgorithm());
        assertEquals(digest.getAlgorithm(), entry.getAlgorithm());
      } catch (NoSuchAlgorithmException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (entry...)
  } // end method */

  /** Test method for {@link EafiHashAlgorithm#getDigestLength()}. */
  @Test
  void test_getBlockLength() {
    // Assertions:
    // - none -

    // Note: This simple method doesn't need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. loop over all entries of enumeration

    // spotless:off

    // --- a. loop over all entries of enumeration
    for (final EafiHashAlgorithm entry : EafiHashAlgorithm.values()) {
      final int expected = switch (entry) {
        case MD2 -> 48;
        case SHA_384, SHA_512 -> 128;
        default -> 64;
      }; // end Switch (entry)

      final var present = entry.getBlockLength();

      assertEquals(expected, present, entry::getAlgorithm);
    } // end For (entry...)
    // end --- a.
    // spotless:on
  } // end method */

  /** Test method for {@link EafiHashAlgorithm#getDigestLength()}. */
  @Test
  void test_getDigestLength() {
    // Note: This simple method doesn't need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. loop over all entries of enumeration
    for (final EafiHashAlgorithm entry : EafiHashAlgorithm.values()) {
      try {
        final MessageDigest digest = MessageDigest.getInstance(entry.getAlgorithm());
        assertEquals(digest.getDigestLength(), entry.getDigestLength());
      } catch (NoSuchAlgorithmException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (entry...)
  } // end method */

  /** Test method for {@link EafiHashAlgorithm#getMessageDigest()}. */
  @Test
  void test_getMessageDigest() {
    // Note 1: The simple method doesn't need extensive testing, so we can be lazy here.

    // Note 2: BouncyCastle Provider is removed here in order to cause an exception.
    //         Thus, code coverage is improved.
    Security.removeProvider(BC.getName());

    // Test strategy:
    // --- a. loop over all entries of enumeration
    // --- b. check RIPEMD_160 separately

    // --- a. loop over all entries of enumeration
    for (final EafiHashAlgorithm dut : EafiHashAlgorithm.values()) {
      if (EafiHashAlgorithm.RIPEMD_160.equals(dut)) {
        // ... expect exception
        assertThrows(AssertionError.class, dut::getMessageDigest);
      } else {
        // ... no exception expected
        try {
          final MessageDigest digest = MessageDigest.getInstance(dut.getAlgorithm());
          assertEquals(digest.getAlgorithm(), dut.getMessageDigest().getAlgorithm());
        } catch (NoSuchAlgorithmException e) {
          fail(UNEXPECTED, e);
        } // end Catch (...)
      } // end else
    } // end For (entry...)

    // --- b. check RIPEMD_160 separately
    // Note 3: Re-add BouncyCastle Provider.
    Security.addProvider(BC);

    try {
      final EafiHashAlgorithm dut = EafiHashAlgorithm.RIPEMD_160;
      final MessageDigest digest = MessageDigest.getInstance(dut.getAlgorithm());
      assertEquals(digest.getAlgorithm(), dut.getMessageDigest().getAlgorithm());
    } catch (NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link EafiHashAlgorithm#getHashId()}. */
  @Test
  void test_getHashId() {
    // Note 1: The simple method needs little testing, so we can be lazy here.

    // Test strategy:
    // --- a. check the return value for each entry from enumeration
    final var map =
        Map.ofEntries(
            Map.entry(EafiHashAlgorithm.MD2, 0x80),
            Map.entry(EafiHashAlgorithm.MD5, 0x81),
            Map.entry(EafiHashAlgorithm.RIPEMD_160, ISOTrailers.TRAILER_RIPEMD160 >> 8),
            Map.entry(EafiHashAlgorithm.SHA_1, ISOTrailers.TRAILER_SHA1 >> 8),
            Map.entry(EafiHashAlgorithm.SHA_224, ISOTrailers.TRAILER_SHA224 >> 8),
            Map.entry(EafiHashAlgorithm.SHA_256, ISOTrailers.TRAILER_SHA256 >> 8),
            Map.entry(EafiHashAlgorithm.SHA_384, ISOTrailers.TRAILER_SHA384 >> 8),
            Map.entry(EafiHashAlgorithm.SHA_512, ISOTrailers.TRAILER_SHA512 >> 8));
    for (final var entry : EafiHashAlgorithm.values()) {
      final int expected = map.get(entry);

      final var actual = entry.getHashId();

      assertEquals(expected, actual, entry::toString);
    } // end For (entry...)
  } // end method */

  /** Test method for {@link EafiHashAlgorithm#getOid()}. */
  @Test
  void test_getOid() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with all values
    final var map =
        Map.ofEntries(
            Map.entry(EafiHashAlgorithm.MD2, AfiOid.MD2),
            Map.entry(EafiHashAlgorithm.MD5, AfiOid.MD5),
            Map.entry(EafiHashAlgorithm.RIPEMD_160, AfiOid.RIPEMD160),
            Map.entry(EafiHashAlgorithm.SHA_1, AfiOid.SHA1),
            Map.entry(EafiHashAlgorithm.SHA_224, AfiOid.SHA224),
            Map.entry(EafiHashAlgorithm.SHA_256, AfiOid.SHA256),
            Map.entry(EafiHashAlgorithm.SHA_384, AfiOid.SHA384),
            Map.entry(EafiHashAlgorithm.SHA_512, AfiOid.SHA512));
    for (final var i : EafiHashAlgorithm.values()) {
      final var expected = map.get(i);
      final var actual = i.getOid();

      assertEquals(expected, actual, i::toString);
    } // end For (i...)
  } // end method */

  /** Test method for {@link EafiHashAlgorithm#maskGenerationFunction(byte[], int, int)}. */
  @Test
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  void test_maskGenerationFunction__byteA_int_int() {
    // Assertion:
    // ... a. getAlgorithm()-method works as expected

    // Test strategy:
    // --- a. loop over all entries of enumeration
    // --- b. loop over several seeds
    // --- c. loop over several startValues
    // --- d. loop over several outputLength
    // --- e. invalid input values

    final StringBuilder output = new StringBuilder();
    final int maxHashRounds = 20;

    // --- a. loop over all entries of enumeration
    for (final var entry : EafiHashAlgorithm.values()) {
      // --- b. loop over several seeds
      for (final var seedLength : RNG.intsClosed(0, 128, 10).boxed().toList()) {
        try {
          final byte[] seed = RNG.nextBytes(seedLength);
          final String format = Hex.toHexDigits(seed) + "%08x";
          final MessageDigest meDi = MessageDigest.getInstance(entry.getAlgorithm());
          // --- c. loop over several startValues
          for (final int startValue :
              List.of(
                  -maxHashRounds - 2, // so negative, that no overflow occurs when incrementing
                  0, // value from PKCS#1 Annex B.2.1 or ISO/IEC 9796-2 Annex B
                  1, // value from ANSI X9.63 clause 5.6.3
                  RNG.nextIntClosed(2, 512) // random value
                  )) {
            // calculate output
            output.setLength(0);
            for (int i = 0; i < maxHashRounds; i++) {
              final String hashInput = String.format(format, startValue + i);
              meDi.reset();
              output.append(Hex.toHexDigits(meDi.digest(Hex.toByteArray(hashInput))));
            } // end For (i...)
            final byte[] totalOutput = Hex.toByteArray(output.toString());

            // --- d. loop over several outputLength
            for (int i = totalOutput.length; i-- > 0; ) { // NOPMD assignment in operand
              final byte[] exp = Arrays.copyOf(totalOutput, i);
              final byte[] pre = entry.maskGenerationFunction(seed, i, startValue);
              assertEquals(Hex.toHexDigits(exp), Hex.toHexDigits(pre));
            } // end For (i...)

            // --- e. invalid input values
            // e.1 negative outputLength
            List.of(
                    Integer.MIN_VALUE, // infimum  of invalid values
                    -1 // supremum of invalid values
                    )
                .forEach(
                    outputLength -> {
                      final Throwable thrown =
                          assertThrows(
                              NegativeArraySizeException.class,
                              () -> entry.maskGenerationFunction(seed, outputLength, startValue));
                      assertEquals(Integer.toString(outputLength), thrown.getMessage());
                      assertNull(thrown.getCause());
                    }); // end forEach(outputLength -> ...)
          } // end For startValue...)
        } catch (NoSuchAlgorithmException e) {
          fail(UNEXPECTED, e);
        } // end Catch (...)
      } // end For (seedLength...)
    } // end For (entry...)
  } // end method */
} // end class
