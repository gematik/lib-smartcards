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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.util.Arrays;
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
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestPkcs1Utils {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // intentionally empty
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

  /** Test method for {@link Pkcs1Utils#pkcs1DigestInfo(byte[], EafiHashAlgorithm)}. */
  @Test
  void test_pkcs1DigestInfo__byteA_EafiHashAlgorithm() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with examples from ftp://ftp.rsa.com/pub/pkcs/ascii/examples.asc

    // --- a. smoke test with examples from ftp://ftp.rsa.com/pub/pkcs/ascii/examples.asc
    {
      final RsaPrivateCrtKeyImpl prk =
          new RsaPrivateCrtKeyImpl(
              new BigInteger(
                  "33d48445c859e52340de704bcdda065fbb4058d740bd1d67d29e9c146c11cf61", 16),
              new BigInteger(
                  "335e8408866b0fd38dc7002d3f972c67389a65d5d8306566d5c4f2a5aa52628b", 16),
              new BigInteger("010001", 16));
      final RsaPublicKeyImpl puk = prk.getPublicKey();

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
        final var dsi = puk.pkcs1RsaVp1(new BigInteger(1, signature));
        final var dsiOctet = AfiBigInteger.i2os(dsi);
        int index = 0;
        while (dsiOctet[index] != 0) {
          index++;
        } // end While (separator not reached)
        final var tlvOctet = Arrays.copyOfRange(dsiOctet, index + 1, dsiOctet.length);
        final var expected = BerTlv.getInstance(tlvOctet);

        final var present = Pkcs1Utils.pkcs1DigestInfo(message, EafiHashAlgorithm.MD2);

        assertEquals(expected, present);
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
        final var dsi = puk.pkcs1RsaVp1(new BigInteger(1, signature));
        final var dsiOctet = AfiBigInteger.i2os(dsi);
        int index = 0;
        while (dsiOctet[index] != 0) {
          index++;
        } // end While (separator not reached)
        final var tlvOctet = Arrays.copyOfRange(dsiOctet, index + 1, dsiOctet.length);
        final var expected = BerTlv.getInstance(tlvOctet);

        final var present = Pkcs1Utils.pkcs1DigestInfo(message, EafiHashAlgorithm.MD2);

        assertEquals(expected, present);
      } // end example 2
    } // end --- a.
  } // end method */

  /** Test method for {@link Pkcs1Utils#pkcs1EmsaV15(byte[], int, EafiHashAlgorithm)}. */
  @Test
  void test_pkcs1_emsa_v1_5__byteA_int_EafiHashAlgorithm() {
    // Assertions:
    // ... a. pkcs1DigestInfo(...)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over several messages
    // --- c. loop over all hash-algorithms
    // --- d. loop over various lengths for encodedMessage
    // --- e. ERROR: emLen too short

    // --- a. smoke test
    {
      final var message = Hex.toByteArray("Mac Mustermann");
      final var hashAlgorithm = EafiHashAlgorithm.SHA_256;
      final var digestInfo = Pkcs1Utils.pkcs1DigestInfo(message, hashAlgorithm).getEncoded();
      final int emLen = digestInfo.length + 11;

      final var present = Pkcs1Utils.pkcs1EmsaV15(message, emLen, hashAlgorithm);

      assertEquals(
          "0001ffffffffffffffff00" + Hex.toHexDigits(digestInfo), Hex.toHexDigits(present));
    } // end --- a.

    // --- b. loop over several messages
    RNG.intsClosed(0, 100, 20)
        .forEach(
            messageLength -> {
              final var message = RNG.nextBytes(messageLength);

              // --- c. loop over all hash-algorithms
              Arrays.stream(EafiHashAlgorithm.values())
                  .filter(i -> !EafiHashAlgorithm.RIPEMD_160.equals(i)) // filter RIPEMD_160
                  .forEach(
                      hashAlgorithm -> {
                        final var digestInfo =
                            Pkcs1Utils.pkcs1DigestInfo(message, hashAlgorithm).getEncoded();
                        final int emLenMin = digestInfo.length + 11;

                        // --- d. loop over various lengths for encodedMessage
                        RNG.intsClosed(emLenMin, emLenMin + 200, 10)
                            .forEach(
                                emLen -> {
                                  final var present =
                                      Pkcs1Utils.pkcs1EmsaV15(message, emLen, hashAlgorithm);

                                  assertEquals(emLen, present.length);
                                  final var ps = new byte[emLen - digestInfo.length - 3];
                                  Arrays.fill(ps, (byte) 0xff);
                                  assertEquals(
                                      "0001"
                                          + Hex.toHexDigits(ps)
                                          + "00"
                                          + Hex.toHexDigits(digestInfo),
                                      Hex.toHexDigits(present));
                                }); // end forEach(emLen -> ...)

                        // --- e. ERROR: emLen too short
                        assertThrows(
                            IllegalArgumentException.class,
                            () -> Pkcs1Utils.pkcs1EmsaV15(message, emLenMin - 1, hashAlgorithm));
                      }); // end forEach(hashAlgorithm -> ...)
            }); // end forEach(messageLength -> ...)
  } // end method */
} // end class
