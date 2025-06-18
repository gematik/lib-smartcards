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

import static de.gematik.smartcards.utils.AfiUtils.KIBI;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.DerBitString;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.IntStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests on {@link EcPublicKeyImpl}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestEcPublicKeyImpl {

  /** String constant for algorithm "SHA256withECDSA". */
  private static final String ALG_SHA256_ECDSA = "SHA256withECDSA"; // */

  /** String constant for algorithm "SHA384withECDSA". */
  private static final String ALG_SHA384_ECDSA = "SHA384withECDSA"; // */

  /** String constant for algorithm "SHA512withECDSA". */
  private static final String ALG_SHA512_ECDSA = "SHA512withECDSA"; // */

  /**
   * {@link BouncyCastleProvider} for cryptographic functions.
   *
   * <p>According to <a
   * href="https://docs.oracle.com/en/java/javase/13/security/oracle-providers.html#GUID-091BF58C-82AB-4C9C-850F-1660824D5254">SUN</a>
   * The support for {@code brainpool}-curves has been deleted, see also <a
   * href="https://bugs.openjdk.java.net/browse/JDK-8251547">OpenJDK</a>.
   *
   * <p>Because some tests use the underlying Java Runtime Environment for cryptography with
   * elliptic curves here we use {@link BouncyCastleProvider}.
   */
  private static final BouncyCastleProvider BC = new BouncyCastleProvider(); // */

  /** Error message. */
  private static final String ERROR = "ERROR, invalid structure"; // */

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

  /** Test method for {@link EcPublicKeyImpl#EcPublicKeyImpl(BerTlv, EafiElcPukFormat)}. */
  @Test
  void test_EcPublicKeyImpl__BerTlv_EafiElcPukFormat() {
    // Assertions:
    // ... a. EcPublicKeyImpl(Object[])-constructor works as expected

    // Test strategy:
    // --- a. happy case for ISO7816-format, compressed and uncompressed
    // --- b. happy case for X509-format, compressed and uncompressed
    // --- c. ERROR: wrong format ISO7816-format
    // --- d. ERROR: wrong format X509

    try {
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);

      AfiElcParameterSpec.PREDEFINED.forEach(
          dp -> {
            try {
              keyPairGenerator.initialize(dp);
              final var keyPair = keyPairGenerator.generateKeyPair();
              final var point = ((ECPublicKey) keyPair.getPublic()).getW();
              final var uncompressed = AfiElcUtils.p2osUncompressed(point, dp);
              final var compressed = AfiElcUtils.p2osCompressed(point, dp);

              // --- a. happy case for ISO7816-format, compressed and uncompressed
              var dut = // uncompressed format
                  new EcPublicKeyImpl(
                      BerTlv.getInstance(
                          0x7f49,
                          List.of(new DerOid(dp.getOid()), BerTlv.getInstance(0x86, uncompressed))),
                      EafiElcPukFormat.ISOIEC7816);
              assertEquals(point, dut.getW());
              assertEquals(dp, dut.getParams());

              dut = // compressed format
                  new EcPublicKeyImpl(
                      BerTlv.getInstance(
                          0x7f49,
                          List.of(new DerOid(dp.getOid()), BerTlv.getInstance(0x86, compressed))),
                      EafiElcPukFormat.ISOIEC7816);
              assertEquals(point, dut.getW());
              assertEquals(dp, dut.getParams());

              // --- b. happy case for X509-format, compressed and uncompressed
              dut = // uncompressed
                  new EcPublicKeyImpl(
                      new DerSequence(
                          List.of(
                              new DerSequence(
                                  // DO-OID
                                  List.of(new DerOid(AfiOid.ecPublicKey), new DerOid(dp.getOid()))),
                              new DerBitString(uncompressed))),
                      EafiElcPukFormat.X509);
              assertEquals(point, dut.getW());
              assertEquals(dp, dut.getParams());

              dut = // compressed
                  new EcPublicKeyImpl(
                      new DerSequence(
                          List.of(
                              new DerSequence(
                                  // DO-OID
                                  List.of(new DerOid(AfiOid.ecPublicKey), new DerOid(dp.getOid()))),
                              new DerBitString(compressed))),
                      EafiElcPukFormat.X509);
              assertEquals(point, dut.getW());
              assertEquals(dp, dut.getParams());
            } catch (InvalidAlgorithmParameterException e) {
              fail(UNEXPECTED, e);
            } // end Catch (...)
          }); // end forEach(dp -> ...)
    } catch (NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a, b.

    // --- c. ERROR: wrong format ISO7816-format
    // c.1 ERROR: not tag '7F49'
    // c.2 ERROR: missing OID tag '06'
    // c.3 ERROR: missing point tag '86'
    // c.4 ERROR: wrong OID content
    // c.5 ERROR: wrong point content
    {
      Throwable throwable;

      // c.1 ERROR: not tag '7F49'
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(BerTlv.getInstance("80 01 02"), EafiElcPukFormat.ISOIEC7816));
      assertEquals("invalid tag: '80'", throwable.getMessage());
      assertNull(throwable.getCause());

      // c.2 ERROR: missing OID tag '06'
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      BerTlv.getInstance("7f49 03 (86 01 02)"), EafiElcPukFormat.ISOIEC7816));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());

      // c.3 ERROR: missing point tag '86'
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      BerTlv.getInstance(
                          0x7f49,
                          List.of(new DerOid(AfiOid.ansix9p256r1), BerTlv.getInstance("80-01-04"))),
                      EafiElcPukFormat.ISOIEC7816));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());

      // c.4 ERROR: wrong OID content
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      BerTlv.getInstance(
                          0x7f49,
                          List.of(
                              new DerOid(AfiOid.rsaEncryption),
                              BerTlv.getInstance("86-01-040102"))),
                      EafiElcPukFormat.ISOIEC7816));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());

      // c.5 ERROR: wrong point content
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      BerTlv.getInstance(
                          0x7f49,
                          List.of(
                              new DerOid(AfiOid.brainpoolP384r1),
                              BerTlv.getInstance("86-01-00") // Point of Infinity
                              )),
                      EafiElcPukFormat.ISOIEC7816));
      assertEquals("w is ECPoint.POINT_INFINITY", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- c.

    // --- d. ERROR: wrong format X509
    // d.1 ERROR: outer tag not '30'
    // d.2 ERROR: missing SEQUENCE
    // d.3 ERROR: missing BITSTRING
    // d.4 ERROR: missing OID-1
    // d.5 ERROR: missing OID-2
    // d.6 ERROR: wrong OID-1
    // d.7 ERROR: wrong OID-2
    // d.8 ERROR: wrong BITSTRING content
    {
      Throwable throwable;

      // d.1 ERROR: outer tag not '30'
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> new EcPublicKeyImpl(BerTlv.getInstance("80 01 02"), EafiElcPukFormat.X509));
      assertEquals("invalid tag: '80'", throwable.getMessage());
      assertNull(throwable.getCause());

      // d.2 ERROR: missing SEQUENCE
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      new DerSequence(List.of(new DerBitString(AfiUtils.EMPTY_OS))),
                      EafiElcPukFormat.X509));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());

      // d.3 ERROR: missing BITSTRING
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      new DerSequence(
                          List.of(
                              new DerSequence(
                                  // DO-OID
                                  List.of(
                                      new DerOid(AfiOid.ecPublicKey),
                                      new DerOid(AfiOid.ansix9p256r1))),
                              BerTlv.getInstance("81-02-1122"))),
                      EafiElcPukFormat.X509));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());

      // d.4 ERROR: missing OID-1
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      new DerSequence(
                          List.of(
                              new DerSequence(new ArrayList<>()),
                              new DerBitString(AfiUtils.EMPTY_OS))),
                      EafiElcPukFormat.X509));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());

      // d.5 ERROR: missing OID-2
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      new DerSequence(
                          List.of(
                              new DerSequence(
                                  // DO-OID
                                  List.of(new DerOid(AfiOid.ecPublicKey))),
                              new DerBitString(AfiUtils.EMPTY_OS))),
                      EafiElcPukFormat.X509));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());

      // d.6 ERROR: wrong OID-1
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      new DerSequence(
                          List.of(
                              new DerSequence(
                                  // DO-OID
                                  List.of(
                                      new DerOid(AfiOid.rsaEncryption),
                                      new DerOid(AfiOid.brainpoolP384r1))),
                              new DerBitString(AfiUtils.EMPTY_OS))),
                      EafiElcPukFormat.X509));
      assertEquals("wrong OID", throwable.getMessage());
      assertNull(throwable.getCause());

      // d.7 ERROR: wrong OID-2
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      new DerSequence(
                          List.of(
                              new DerSequence(
                                  // DO-OID
                                  List.of(
                                      new DerOid(AfiOid.ecPublicKey),
                                      new DerOid(AfiOid.rsaEncryption))),
                              new DerBitString(AfiUtils.EMPTY_OS))),
                      EafiElcPukFormat.X509));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());

      // d.8 ERROR: wrong BITSTRING content
      throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  new EcPublicKeyImpl(
                      new DerSequence(
                          List.of(
                              new DerSequence(
                                  // DO-OID
                                  List.of(
                                      new DerOid(AfiOid.ecPublicKey),
                                      new DerOid(AfiOid.brainpoolP384r1))),
                              new DerBitString(new byte[1]) // Point of Infinity
                              )),
                      EafiElcPukFormat.X509));
      assertEquals("w is ECPoint.POINT_INFINITY", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- d.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#EcPublicKeyImpl(ECPublicKey)}. */
  @Test
  void test_EcPublicKeyImpl__EcPublicKey() {
    // Assertions:
    // ... a. EcPublicKeyImpl(ECPoint, ECParameterSpec)-constructor works as expected

    // Test strategy:
    // --- a. happy case for all predefined domain parameters

    try {
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);

      // --- a. happy case for all predefined domain parameters
      AfiElcParameterSpec.PREDEFINED.forEach(
          dp -> {
            try {
              keyPairGenerator.initialize(dp);
              final var keyPair = keyPairGenerator.generateKeyPair();
              final var publicKey = (ECPublicKey) keyPair.getPublic();

              final var dut = new EcPublicKeyImpl(publicKey);
              assertEquals(publicKey.getW(), dut.getW());
              assertSame(publicKey.getParams(), dut.getParams());
            } catch (InvalidAlgorithmParameterException e) {
              fail(UNEXPECTED, e);
            } // end Catch (...)
          }); // end forEach(dp -> ...)
      // end --- a.
    } catch (NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#EcPublicKeyImpl(ECPoint, ECParameterSpec)}. */
  @Test
  void test_EcPublicKeyImpl__EcPoint_EcParameterSpec() {
    // Assertions:
    // ... a. EcPublicKeyImpl(ECPoint, ECParameterSpec)-constructor works as expected

    // Test strategy:
    // --- a. happy case for all predefined domain parameters
    // --- b. ERROR: point not on an elliptic curve
    // --- c. ERROR: point of infinity
    // --- d. happy case with arbitrary domain parameter, small bit-length

    // --- a. happy case for all predefined domain parameters
    try {
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);

      AfiElcParameterSpec.PREDEFINED.forEach(
          dp -> {
            try {
              keyPairGenerator.initialize(dp);
              final var keyPair = keyPairGenerator.generateKeyPair();
              final var point = ((ECPublicKey) keyPair.getPublic()).getW();

              final var dut = new EcPublicKeyImpl(point, dp);
              assertEquals(point, dut.getW());
              assertSame(dp, dut.getParams());

              // --- b. ERROR: point not on an elliptic curve
              var throwable =
                  assertThrows(
                      IllegalArgumentException.class,
                      () ->
                          new EcPublicKeyImpl(
                              new ECPoint(
                                  point.getAffineX(), point.getAffineY().add(BigInteger.ONE)),
                              dp));
              assertEquals("point not on curve", throwable.getMessage());
              assertNull(throwable.getCause());

              // --- c. ERROR: point of infinity
              throwable =
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> new EcPublicKeyImpl(ECPoint.POINT_INFINITY, dp));
              assertEquals("w is ECPoint.POINT_INFINITY", throwable.getMessage());
              assertNull(throwable.getCause());
            } catch (InvalidAlgorithmParameterException e) {
              fail(UNEXPECTED, e);
            } // end Catch (...)
          }); // end forEach(dp -> ...)
    } catch (NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a, b, c.

    // --- d. happy case with arbitrary domain parameter, small bit-length
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
      final EcPublicKeyImpl dut = new EcPublicKeyImpl(dp.getGenerator(), dp);
      assertSame(dp, dut.getParams());
    } // end --- d.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#fromSequence(DerSequence, int)}. */
  @Test
  void test_fromSequence__DerSequence_int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: tau is negative
    // --- c. ERROR: the sequence contains less than two integers

    // --- a. smoke test
    {
      final var r = BigInteger.valueOf(0xff223344556677L);
      final var s = BigInteger.ONE;
      final var expected = Hex.extractHexDigits("44556677 00000001");
      final var sequence = new DerSequence(List.of(new DerInteger(r), new DerInteger(s)));
      final var tau = 4;

      final var actual = Hex.toHexDigits(EcPublicKeyImpl.fromSequence(sequence, tau));

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: tau is negative
    {
      final var sequence =
          new DerSequence(List.of(new DerInteger(BigInteger.ONE), new DerInteger(BigInteger.TWO)));
      final var tau = -1;

      assertThrows(
          NegativeArraySizeException.class, () -> EcPublicKeyImpl.fromSequence(sequence, tau));
    } // end --- b.

    // --- c. ERROR: the sequence contains less than two integers
    {
      final var sequence = new DerSequence(List.of(new DerInteger(BigInteger.ONE)));
      final var tau = 1;

      assertThrows(NoSuchElementException.class, () -> EcPublicKeyImpl.fromSequence(sequence, tau));
    } // end --- c.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#toSequence(byte[])}. */
  @Test
  void test_toSequence__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. empty octet string
    // --- c. ERROR: odd length

    // --- a. smoke test
    {
      final var input = Hex.toByteArray("0102");
      final var expected =
          new DerSequence(List.of(new DerInteger(BigInteger.ONE), new DerInteger(BigInteger.TWO)));

      final var actual = EcPublicKeyImpl.toSequence(input);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. empty octet string
    {
      final var input = AfiUtils.EMPTY_OS;
      final var expected =
          new DerSequence(
              List.of(new DerInteger(BigInteger.ZERO), new DerInteger(BigInteger.ZERO)));

      final var actual = EcPublicKeyImpl.toSequence(input);

      assertEquals(expected, actual);
    } // end --- b.

    // --- c. ERROR: odd length
    Set.of("01", "010203")
        .forEach(
            input ->
                assertThrows(
                    IllegalArgumentException.class,
                    () ->
                        EcPublicKeyImpl.toSequence(
                            Hex.toByteArray(input)))); // end forEach(input -> ...)
    // end --- c.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#encipher(byte[])}. */
  @Test
  void test_encipher__byteA() {
    // Assertions:
    // ... a. EcPrivateKeyImpl#decipher(ConstructedBerTlv)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. ERROR: invalid elliptic curve

    // --- a. smoke test
    {
      final var dp = AfiElcParameterSpec.brainpoolP256r1;
      final var prk = new EcPrivateKeyImpl(dp);
      final var dut = prk.getPublicKey();
      final var expected = RNG.nextBytes(8, 32);

      final var cryptoDo = dut.encipher(expected);

      final var actual = prk.decipher(cryptoDo);
      assertArrayEquals(expected, actual);
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final var prk = new EcPrivateKeyImpl(dp);
          final var dut = prk.getPublicKey();

          RNG.intsClosed(0, KIBI, 10)
              .forEach(
                  mlen -> {
                    final var expected = RNG.nextBytes(mlen);

                    final var cryptoDo = dut.encipher(expected);

                    final var actual = prk.decipher(cryptoDo);
                    assertArrayEquals(expected, actual);
                  }); // end For (mLen -> ...)
        }); // end forEach(dp -> ...)
    // end --- b.

    // --- c. ERROR: invalid elliptic curve
    {
      final var dp = AfiElcParameterSpec.brainpoolP256r1;
      final var myDp =
          AfiElcParameterSpec.getInstance(
              new ECParameterSpec(
                  dp.getCurve(), dp.getGenerator(), dp.getOrder(), dp.getCofactor() + 1));
      assertThrows(NoSuchElementException.class, myDp::getOid);
      final var prk = new EcPrivateKeyImpl(myDp);
      final var dut = prk.getPublicKey();
      final var message = RNG.nextBytes(8, 32);

      assertThrows(NoSuchElementException.class, () -> dut.encipher(message));
    } // end --- c.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#equals(Object)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_equals__Object() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. loop over predefined curves
    // a.1 same reference
    // a.2 null input
    // a.3 difference in type
    // a.4 difference in domain parameter
    // a.5 difference in x-coordinate
    // a.6 difference in y-coordinate
    // a.7 different objects same content

    // --- b. arbitrary domain parameter
    // b.1 same reference
    // b.2 null input
    // b.3 difference in type
    // b.4 difference in domain parameter
    // b.5 difference in x-coordinate
    // b.6 difference in y-coordinate

    // --- a. loop over predefined curves
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
            keyPairGenerator.initialize(dp);
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            final ECPoint point = ((ECPublicKey) keyPair.getPublic()).getW();
            final EcPublicKeyImpl dut = new EcPublicKeyImpl(point, dp);

            for (final Object obj :
                new Object[] {
                  dut, // a.1 same reference
                  null, // a.2 null input
                  "afi" // a.3 difference in type
                }) {
              assertEquals(
                  obj == dut, dut.equals(obj)); // NOPMD use equals to compare object refer.
            } // end For (obj...)

            AfiElcParameterSpec.PREDEFINED.forEach(
                dpInner -> {
                  boolean actual;

                  try {
                    keyPairGenerator.initialize(dpInner);
                    ECPoint pointInner;
                    do {
                      final KeyPair keyPairInner = keyPairGenerator.generateKeyPair();
                      pointInner = ((ECPublicKey) keyPairInner.getPublic()).getW();
                    } while (point.getAffineX().equals(pointInner.getAffineX()));
                    // ... x-coordinates differ

                    if (dp.equals(dpInner)) {
                      // ... same domain parameter
                      // a.5 difference in x-coordinate
                      actual = dut.equals(new EcPublicKeyImpl(pointInner, dp));
                      assertFalse(actual);

                      // a.6 difference in y-coordinate
                      final ECPoint pointOther =
                          new ECPoint(point.getAffineX(), dp.getP().subtract(point.getAffineY()));
                      assertEquals(point.getAffineX(), pointOther.getAffineX());
                      assertTrue(AfiElcUtils.isPointOnEllipticCurve(pointOther, dp));
                      actual = dut.equals(new EcPublicKeyImpl(pointOther, dp));
                      assertFalse(actual);
                    } else {
                      // ... domain parameter differ
                      // a.4 difference in domain parameter
                      actual = dut.equals(new EcPublicKeyImpl(pointInner, dpInner));
                      assertFalse(actual);
                    } // end else
                  } catch (InvalidAlgorithmParameterException e) {
                    fail(UNEXPECTED, e);
                  } // end Catch (...)
                }); // end forEach(dp -> ...)

            // a.7 different objects same content
            final Object obj = new EcPublicKeyImpl(dut.getW(), dut.getParams());
            assertNotSame(dut, obj);
            final var actual = dut.equals(obj);
            assertTrue(actual);
          } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            fail(UNEXPECTED, e);
          } // end Catch (...)
        }); // end forEach(dp -> ...)

    // --- b. arbitrary domain parameter
    // p=7, a=2, b=6, G=(1,3), n=11, h=1
    // G={(1, 3), (1, 4), (2, 2), (2, 5), (3, 2), (3, 5), (4, 1), (4, 6), (5, 1), (5, 6), O}
    {
      final ECParameterSpec dp =
          new ECParameterSpec(
              new EllipticCurve(
                  new ECFieldFp(AfiBigInteger.SEVEN), BigInteger.TWO, AfiBigInteger.SIX),
              new ECPoint(BigInteger.ONE, AfiBigInteger.THREE),
              BigInteger.valueOf(11),
              1);
      final EcPublicKeyImpl dut =
          new EcPublicKeyImpl(new ECPoint(BigInteger.ONE, AfiBigInteger.FOUR), dp);

      for (final Object obj :
          new Object[] {
            dut, // b.1 same reference
            null, // b.2 null input
            "afi", // b.3 difference in type
            // b.4 difference in domain parameter
            new EcPublicKeyImpl(
                dut.getW(),
                new ECParameterSpec(
                    new EllipticCurve(
                        new ECFieldFp(AfiBigInteger.SEVEN), BigInteger.TWO, AfiBigInteger.SIX),
                    new ECPoint(BigInteger.TWO, BigInteger.TWO),
                    BigInteger.valueOf(11),
                    1)),
            // b.5 difference in x-coordinate
            // Note: y-coordinate also differs to avoid "Point not on curve"
            new EcPublicKeyImpl(new ECPoint(BigInteger.TWO, AfiBigInteger.FIVE), dp),
            // b.6 difference in y-coordinate
            new EcPublicKeyImpl(new ECPoint(BigInteger.ONE, AfiBigInteger.THREE), dp)
          }) {
        assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals to compare object refer.
      } // end For (obj...)
    } // end --- b.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#hashCode()}. */
  @Test
  void test_hashCode() {
    // Assertions:
    // ... a. hashCode()-method of class AfiElcParameterSpec works as expected
    // ... b. hashCode()-method of class ECPoint works as expected

    // Test strategy:
    // --- a. loop over predefined curves
    // --- b. call hashCode()-method again

    // --- a. loop over predefined curves
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
            keyPairGenerator.initialize(dp);
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            final ECPoint point = ((ECPublicKey) keyPair.getPublic()).getW();

            // --- b. check that the same result occurs than for JRE keys
            final EcPublicKeyImpl dut = new EcPublicKeyImpl(point, dp);
            assertEquals(31 * dp.hashCode() + point.hashCode(), dut.hashCode());
          } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            fail(UNEXPECTED, e);
          } // end Catch (...)
        }); // end forEach(dp -> ...)

    // --- b. call hashCode()-method again
    // Note: The main reason for this check is to get full code-coverage.
    //        a. The first time this method is called on a newly constructed object
    //           insHashCode is zero.
    //        b. The second time this method is called the insHashCode isn't zero
    //           (with a high probability).
    try {
      final AfiElcParameterSpec dp = AfiElcParameterSpec.brainpoolP256r1;
      final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
      keyPairGenerator.initialize(dp);
      final KeyPair keyPair = keyPairGenerator.generateKeyPair();
      final ECPoint point = ((ECPublicKey) keyPair.getPublic()).getW();

      final EcPublicKeyImpl dut = new EcPublicKeyImpl(point, dp);
      final int hash = dut.hashCode();
      assertEquals(hash, dut.hashCode());
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#getAlgorithm()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_getAlgorithm() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. loop over all predefined domain parameters
    // --- b. check that the same result occurs than for JRE keys

    // --- a. loop over all predefined domain parameters
    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      try {
        final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
        keyPairGenerator.initialize(dp);
        final var keyPair = keyPairGenerator.generateKeyPair();
        final var puk = (ECPublicKey) keyPair.getPublic();

        // --- b. check that the same result occurs than for JRE keys
        final var dut = new EcPublicKeyImpl(puk.getW(), dp);
        assertEquals(puk.getAlgorithm(), dut.getAlgorithm());
      } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (dp...)
    // end --- a, b.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#getEncoded()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_getEncoded() {
    // Assertions:
    // ... a. getEncoded(EafiElcPukFormat)-method works as expected

    // Test strategy:
    // --- a. loop over all predefined domain parameters
    // --- b. check that the same result occurs than for JRE keys

    // --- a. loop over all predefined domain parameter
    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      try {
        // Note: Intentionally here NOT the bouncy-castle provider is used,
        //       because that provider uses a different encoding format.
        final var keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(dp);
        final var keyPair = keyPairGenerator.generateKeyPair();
        final var puk = (ECPublicKey) keyPair.getPublic();

        // --- b. check that the same result occurs than for JRE keys
        final EcPublicKeyImpl dut = new EcPublicKeyImpl(puk.getW(), dp);
        assertEquals(
            BerTlv.getInstance(puk.getEncoded()).toString(" "),
            BerTlv.getInstance(dut.getEncoded()).toString(" "));
      } catch (InvalidAlgorithmParameterException e) {
        // ... domain parameter not supported by JRE
        //     => check for brainpool curve

        if (!dp.getOid().getName().startsWith("brainpool")) {
          // ... not a brainpool curve, that is unexpected
          fail(UNEXPECTED, e);
        } // end fi
      } catch (NoSuchAlgorithmException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (dp...)
    // end --- a, b.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#getEncoded(EafiElcPukFormat)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_getEncoded__EafiElcPukFormat() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. loop over all predefined domain parameter
    // --- b. convert for each format
    // --- c. ERROR: domain parameter without OID

    final DerOid oidEcPublicKey = new DerOid(AfiOid.ecPublicKey);

    // --- a. loop over all predefined domain parameter
    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      final DerOid oidDomainParameter = new DerOid(dp.getOid());

      try {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
        keyPairGenerator.initialize(dp);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final ECPoint point = ((ECPublicKey) keyPair.getPublic()).getW();
        final byte[] uncompressed = AfiElcUtils.p2osUncompressed(point, dp);

        final EcPublicKeyImpl dut = new EcPublicKeyImpl(point, dp);

        // --- b. convert for each format
        // b.1 ISO7816
        // b.2 X509

        // b.1 ISO7816
        assertEquals(
            BerTlv.getInstance(
                0x7f49, List.of(oidDomainParameter, BerTlv.getInstance(0x86, uncompressed))),
            dut.getEncoded(EafiElcPukFormat.ISOIEC7816));

        // b.2 X509
        assertEquals(
            new DerSequence(
                List.of(
                    new DerSequence(List.of(oidEcPublicKey, oidDomainParameter)),
                    new DerBitString(uncompressed))),
            dut.getEncoded(EafiElcPukFormat.X509));
        // end --- b.
      } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (dp...)
    // end --- a, b.

    // --- c. ERROR: domain parameter without OID
    {
      final AfiElcParameterSpec dp = AfiElcParameterSpec.brainpoolP512r1;
      final AfiElcParameterSpec dpDut =
          new AfiElcParameterSpec(
              dp.getP(),
              dp.getCurve().getA(),
              dp.getCurve().getB(),
              dp.getGenerator().getAffineX(),
              dp.getP().subtract(dp.getGenerator().getAffineY()),
              dp.getOrder(),
              dp.getCofactor());

      Arrays.stream(EafiElcPukFormat.values())
          .forEach(
              format -> {
                final EcPublicKeyImpl dut = new EcPublicKeyImpl(dp.getGenerator(), dpDut);

                assertThrows(NoSuchElementException.class, () -> dut.getEncoded(format));
              }); // end forEach(format -> ...)
    } // end --- c.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#getFormat()}. */
  @Test
  void test_getFormat() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all predefined domain parameters
    // --- c. check that the same result occurs than for JRE keys

    // --- a. smoke test
    {
      final var dp = AfiElcParameterSpec.ansix9p256r1;
      try {
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
        keyPairGenerator.initialize(dp);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final ECPublicKey puk = (ECPublicKey) keyPair.getPublic();

        // --- c. check that the same result occurs than for JRE keys
        final EcPublicKeyImpl dut = new EcPublicKeyImpl(puk.getW(), dp);
        assertEquals(puk.getFormat(), dut.getFormat());
      } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end --- a.

    // --- b. loop over all predefined domain parameters
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
            keyPairGenerator.initialize(dp);
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            final ECPublicKey puk = (ECPublicKey) keyPair.getPublic();

            // --- c. check that the same result occurs than for JRE keys
            final EcPublicKeyImpl dut = new EcPublicKeyImpl(puk.getW(), dp);
            assertEquals(puk.getFormat(), dut.getFormat());
          } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            fail(UNEXPECTED, e);
          } // end Catch (...)
        }); // end forEach(dp -> ...)
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#getParams()}. */
  @Test
  void test_getParams() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. loop over all predefined domain parameters
    // --- b. arbitrary domain parameter

    // --- a. loop over all predefined domain parameters
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final ECParameterSpec parameterSpec =
              new ECParameterSpec(
                  dp.getCurve(), dp.getGenerator(), dp.getOrder(), dp.getCofactor());
          final EcPublicKeyImpl dut = new EcPublicKeyImpl(dp.getGenerator(), parameterSpec);
          assertSame(dp, dut.getParams());
        }); // end forEach(dp -> ...)
    // end --- a.

    // --- b. arbitrary domain parameter
    {
      final ECParameterSpec parameterSpec =
          new ECParameterSpec(
              new EllipticCurve(
                  new ECFieldFp(AfiBigInteger.SEVEN), AfiBigInteger.FIVE, BigInteger.ONE),
              new ECPoint(AfiBigInteger.SIX, AfiBigInteger.THREE),
              BigInteger.valueOf(12),
              1);
      final EcPublicKeyImpl dut =
          new EcPublicKeyImpl(
              new ECPoint(AfiBigInteger.SIX, AfiBigInteger.FOUR), // -G
              parameterSpec);
      final ECParameterSpec dpDut = dut.getParams();
      assertEquals(AfiElcParameterSpec.class, dpDut.getClass());
      assertEquals(parameterSpec.getCurve(), dpDut.getCurve());
      assertEquals(parameterSpec.getGenerator(), dpDut.getGenerator());
      assertEquals(parameterSpec.getOrder(), dpDut.getOrder());
      assertEquals(parameterSpec.getCofactor(), dpDut.getCofactor());
    } // end --- b.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // - non -

    // Test strategy:
    // --- a. loop over all predefined domain parameter
    // --- b. arbitrary domain parameter, i.e. no OID available

    // --- a. loop over all predefined domain parameter
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
            keyPairGenerator.initialize(dp);
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            final ECPoint point = ((ECPublicKey) keyPair.getPublic()).getW();

            final EcPublicKeyImpl dut = new EcPublicKeyImpl(point, dp);

            assertEquals(
                String.format(
                    "Domain parameter OID = %s, P = (xp, yp) = ('%s', '%s')",
                    dp.getOid(),
                    Hex.toHexDigits(AfiElcUtils.fe2os(point.getAffineX(), dp)),
                    Hex.toHexDigits(AfiElcUtils.fe2os(point.getAffineY(), dp))),
                dut.toString());
          } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            fail(UNEXPECTED, e);
          } // end Catch (...)
        }); // end forEach(dp -> ...)
    // end --- a.

    // --- b. arbitrary domain parameter, i.e. no OID available
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
      final ECPoint point =
          new ECPoint(
              dp.getGenerator().getAffineX(), dp.getP().subtract(dp.getGenerator().getAffineY()));
      final EcPublicKeyImpl dut = new EcPublicKeyImpl(point, dp);

      assertEquals(
          String.format(
              "Domain parameter: %s%nP = (xp, yp) = ('%s', '%s')",
              dp,
              Hex.toHexDigits(AfiElcUtils.fe2os(point.getAffineX(), dp)),
              "04" // Hex.toHexDigits(AfiElcUtils.fe2os(point.getAffineY(), dp))
              ),
          dut.toString());
    } // end --- b.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#verifyEcdsa(byte[], byte[])}. */
  @Test
  void test_verifyEcdsa__byteA_byteA() {
    // Assertions:
    // ... a. verifyEcdsa(byte[], DerSequence)-method works as expected
    // ... b. fromSequence(DerSequence)-method works as expected
    // ... b. toSequence(byte[])-method works as expected

    // Note: Because of the assertions, this simple method does not need
    //       intensive testing. Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    try {
      final var dp = AfiElcParameterSpec.brainpoolP256r1;
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
      keyPairGenerator.initialize(dp);
      final var keyPair = keyPairGenerator.generateKeyPair();
      final var prk = (ECPrivateKey) keyPair.getPrivate();
      final var dut = new EcPublicKeyImpl(((ECPublicKey) keyPair.getPublic()).getW(), dp);
      final var message = RNG.nextBytes(16, 32);
      final var signer = Signature.getInstance(ALG_SHA256_ECDSA, BC);
      signer.initSign(prk);
      signer.update(message);
      final var sigOctets = signer.sign();
      final var signature = (DerSequence) BerTlv.getInstance(sigOctets);

      // signature verification successful
      assertTrue(dut.verifyEcdsa(message, EcPublicKeyImpl.fromSequence(signature, dp.getTau())));
    } catch (InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | SignatureException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#verifyEcdsa(byte[], DerSequence)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_verifyEcdsa__byteA_DerSequence() {
    // Assertions:
    // ... a. verifyEcdsa(BigInteger, DerSequence)-method works as expected

    // Note 1: The test strategy here is to use JRE for creating signatures and
    //         check those signatures with the method-under-test.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all predefined domain parameters
    // --- b. create a bunch of key-pairs
    // --- d. create a bunch of messages

    // --- a. smoke test
    try {
      final var dp = AfiElcParameterSpec.brainpoolP256r1;
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
      keyPairGenerator.initialize(dp);
      final var keyPair = keyPairGenerator.generateKeyPair();
      final var prk = (ECPrivateKey) keyPair.getPrivate();
      final var dut = new EcPublicKeyImpl(((ECPublicKey) keyPair.getPublic()).getW(), dp);

      final int size = dp.getP().bitLength();
      final String algorithm;
      if (size <= 256) { // NOPMD literal in if statement
        algorithm = ALG_SHA256_ECDSA;
      } else if (size <= 384) { // NOPMD literal in if statement
        algorithm = ALG_SHA384_ECDSA;
      } else {
        algorithm = ALG_SHA512_ECDSA;
      } // end fi

      final var signer = Signature.getInstance(algorithm, BC);
      signer.initSign(prk);
      final var message = RNG.nextBytes(1024, 2048);
      signer.update(message);
      final var sigOctets = signer.sign();
      final var signature = ((DerSequence) BerTlv.getInstance(sigOctets));

      assertTrue(dut.verifyEcdsa(message, signature));
    } catch (InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | SignatureException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.

    // --- b. loop over all predefined domain parameter
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          try {
            final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
            keyPairGenerator.initialize(dp);

            // --- c. create a bunch of key-pairs
            IntStream.range(0, 5)
                .forEach(
                    i -> {
                      final var keyPair = keyPairGenerator.generateKeyPair();
                      final var prk = (ECPrivateKey) keyPair.getPrivate();
                      final var dut =
                          new EcPublicKeyImpl(((ECPublicKey) keyPair.getPublic()).getW(), dp);

                      final int size = dp.getP().bitLength();
                      final String algorithm;
                      if (size <= 256) { // NOPMD literal in if statement
                        algorithm = ALG_SHA256_ECDSA;
                      } else if (size <= 384) { // NOPMD literal in if statement
                        algorithm = ALG_SHA384_ECDSA;
                      } else {
                        algorithm = ALG_SHA512_ECDSA;
                      } // end fi

                      // --- d. create a bunch of messages
                      try {
                        final var signer = Signature.getInstance(algorithm, BC);
                        signer.initSign(prk);
                        RNG.intsClosed(0, 1000, 10)
                            .forEach(
                                length -> {
                                  final var message = RNG.nextBytes(length);

                                  try {
                                    signer.update(message);
                                    final var sigOctets = signer.sign();
                                    final var signature =
                                        ((DerSequence) BerTlv.getInstance(sigOctets));
                                    final var r = signature.getInteger(0).getDecoded();
                                    final var s = signature.getInteger(1).getDecoded();

                                    // signature verification successful
                                    assertTrue(dut.verifyEcdsa(message, signature));

                                    // signature verification fails (for one reason or the other)
                                    final double rnd = RNG.nextDouble();
                                    if (rnd < 0.5) { // NOPMD literal in if statement
                                      // ... 50% chance to manipulate signature
                                      assertFalse(
                                          dut.verifyEcdsa(
                                              message,
                                              new DerSequence(
                                                  List.of(
                                                      new DerInteger(r.add(BigInteger.ONE)),
                                                      new DerInteger(s)))));
                                    } else if ((0 == length) || (rnd < 0.8)) {
                                      // ... 30% chance to append something to message
                                      final byte[] newMessage =
                                          AfiUtils.concatenate(message, RNG.nextBytes(1, 10));
                                      assertFalse(dut.verifyEcdsa(newMessage, signature));
                                    } else {
                                      // ... 20% chance to manipulate a byte in the message
                                      final int index = RNG.nextIntClosed(0, message.length - 1);
                                      message[index]++;
                                      assertFalse(dut.verifyEcdsa(message, signature));
                                    } // end fi
                                  } catch (SignatureException e) {
                                    fail(UNEXPECTED, e);
                                  } // end Catch (...)
                                }); // end forEach(size -> ...)
                      } catch (InvalidKeyException | NoSuchAlgorithmException e) {
                        fail(UNEXPECTED, e);
                      } // end Catch (...)
                    }); // end forEach(i -> ...)
          } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            fail(UNEXPECTED, e);
          } // end Catch (...)
        }); // end forEach(dp -> ...)
    // end --- b, c, d.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#verifyEcdsa(BigInteger, byte[])}. */
  @Test
  void test_verifyEcdsa__BigInteger_byteA() {
    // Assertion:
    // ... a. verifyEcdsa(byte[], DerSequence)-method works as expected
    // ... b. toSequence(byte[])-method works as expected

    // Note: Because of the assertions, this simple method does not need
    //       intensive testing. Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    try {
      final var dp = AfiElcParameterSpec.ansix9p256r1;
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
      keyPairGenerator.initialize(dp);
      final var keyPair = keyPairGenerator.generateKeyPair();
      final var prk = (ECPrivateKey) keyPair.getPrivate();
      final var dut = new EcPublicKeyImpl(((ECPublicKey) keyPair.getPublic()).getW(), dp);
      final var message = RNG.nextBytes(16, 32);
      final var signer = Signature.getInstance(ALG_SHA256_ECDSA, BC);
      signer.initSign(prk);
      signer.update(message);
      final var sigOctets = signer.sign();
      final var rs = ((DerSequence) BerTlv.getInstance(sigOctets)).getDecoded();

      // extract r and s from signature
      final BigInteger biR = ((DerInteger) rs.get(0)).getDecoded();
      final BigInteger biS = ((DerInteger) rs.get(1)).getDecoded();

      final byte[] signature =
          AfiUtils.concatenate(
              AfiBigInteger.i2os(biR, dp.getTau()), AfiBigInteger.i2os(biS, dp.getTau()));

      // signature verification successful
      assertTrue(
          dut.verifyEcdsa(new BigInteger(1, EafiHashAlgorithm.SHA_256.digest(message)), signature));
    } catch (InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | SignatureException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.
  } // end method */

  /** Test method for {@link EcPublicKeyImpl#verifyEcdsa(BigInteger, DerSequence)}. */
  @Test
  void test_verifyEcdsa__BigInteger_DerSequence() {
    // Assertion:
    // ... a. verifyEcdsa(byte[], DerSequence)-method works as expected

    // Note 1: Assertion a. implies that the happy-cases of method-under-test
    //         work as expected. Thus, here we can concentrate on corner cases
    //         and code coverage.

    // Test strategy:
    // --- a. R == 0
    // --- b. S == 0
    // --- c. R == n
    // --- d. S == n
    // --- e. R, S in range [1, n-1]
    // --- f. Q == PointOfInfinity

    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final EcPublicKeyImpl dut = new EcPublicKeyImpl(dp.getGenerator(), dp);
          // --- a. R == 0
          assertFalse(
              dut.verifyEcdsa(
                  BigInteger.ONE,
                  new DerSequence(
                      List.of(new DerInteger(BigInteger.ZERO), new DerInteger(BigInteger.ONE)))));

          // --- b. S == 0
          assertFalse(
              dut.verifyEcdsa(
                  BigInteger.ONE,
                  new DerSequence(
                      List.of(new DerInteger(BigInteger.TEN), new DerInteger(BigInteger.ZERO)))));

          // --- c. R == n
          assertFalse(
              dut.verifyEcdsa(
                  BigInteger.ONE,
                  new DerSequence(
                      List.of(new DerInteger(dp.getOrder()), new DerInteger(BigInteger.TEN)))));

          // --- d. S == n
          assertFalse(
              dut.verifyEcdsa(
                  BigInteger.ONE,
                  new DerSequence(
                      List.of(new DerInteger(BigInteger.TEN), new DerInteger(dp.getOrder())))));

          // --- e. R, S in range [1, n-1]
          assertFalse(
              dut.verifyEcdsa(
                  BigInteger.ONE,
                  new DerSequence(
                      List.of(new DerInteger(BigInteger.TWO), new DerInteger(BigInteger.ONE)))));
        }); // end forEach(dp -> ...)
    // end --- a, b, c, d, e.

    // --- f. Q == PointOfInfinity
    // p=7, a=6, b=1, Gx=1, Gy=6, n=12, h=1, no0=1
    // G = {(1,6), (6,6), (0,1), (3,5), (5,3), (2,0), (5,4), (3,2), (0,6), (6,1), (1,1), O}
    {
      final AfiElcParameterSpec dp =
          AfiElcParameterSpec.getInstance(
              new ECParameterSpec(
                  new EllipticCurve(
                      new ECFieldFp(AfiBigInteger.SEVEN), AfiBigInteger.SIX, BigInteger.ONE),
                  new ECPoint(BigInteger.valueOf(1), BigInteger.valueOf(6)), // Generator
                  BigInteger.valueOf(12),
                  1));

      // --- set device-under-test with a point from G
      final EcPublicKeyImpl dut =
          new EcPublicKeyImpl(new ECPoint(BigInteger.valueOf(3), BigInteger.valueOf(5)), dp);

      assertFalse(
          dut.verifyEcdsa(
              AfiBigInteger.FOUR,
              new DerSequence(
                  List.of(
                      new DerInteger(BigInteger.valueOf(11)), new DerInteger(BigInteger.ONE)))));
    } // end --- f.
  } // end method */

  /** Test method for serialization and deserialization. */
  @SuppressWarnings({"PMD.NPathComplexity"})
  @Test
  void test_serialize() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. predefined domain parameter
    // --- b. arbitrary curve (i.e., not a named curve), here use -G as generator
    // --- c. small size of domain parameter

    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          // --- a. predefined domain parameter
          {
            final ECPublicKey dut = new EcPrivateKeyImpl(dp).getPublicKey();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
              oos.writeObject(dut);
            } catch (IOException e) {
              fail(UNEXPECTED, e);
            } // end Catch (...)

            final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
              final Object obj = ois.readObject();
              assertEquals(dut, obj);
            } catch (ClassNotFoundException | IOException e) {
              fail(UNEXPECTED, e);
            } // end Catch (...)
          } // end --- a.

          // --- b. arbitrary curve (i.e., not a named curve), here use -G as generator
          {
            final AfiElcParameterSpec dp2 =
                AfiElcParameterSpec.getInstance(
                    new ECParameterSpec(
                        dp.getCurve(),
                        // with G as generator form predefined domain parameter
                        new ECPoint(
                            // here -G is used
                            dp.getGenerator().getAffineX(),
                            dp.getP().subtract(dp.getGenerator().getAffineY())),
                        dp.getOrder(),
                        dp.getCofactor()));
            assertTrue(AfiElcUtils.isPointOnEllipticCurve(dp2.getGenerator(), dp));

            final ECPublicKey dut = new EcPrivateKeyImpl(dp2).getPublicKey();
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
              oos.writeObject(dut);
            } catch (IOException e) {
              fail(UNEXPECTED, e);
            } // end Catch (...)

            final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
              final Object obj = ois.readObject();
              assertEquals(dut, obj);
            } catch (ClassNotFoundException | IOException e) {
              fail(UNEXPECTED, e);
            } // end Catch (...)
          } // end --- b.
        }); // end forEach(dp -> ...)

    // --- c. small size of domain parameter
    {
      // p=5, a=2, b=4, G=(4,1), n=7, h=1, (G) = {(4,1), (2,4), (0,3), (0,2), (2,1), (4,4), O}
      final AfiElcParameterSpec dp =
          AfiElcParameterSpec.getInstance(
              new ECParameterSpec(
                  new EllipticCurve(
                      new ECFieldFp(BigInteger.valueOf(5)), // p
                      BigInteger.valueOf(2), // a
                      BigInteger.valueOf(4) // b
                      ),
                  new ECPoint(
                      BigInteger.valueOf(4), // Gx
                      BigInteger.valueOf(1) // Gy
                      ),
                  BigInteger.valueOf(7), // order n
                  1 // cofactor h
                  ));
      final ECPublicKey dut = new EcPrivateKeyImpl(dp).getPublicKey();
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(dut);
      } catch (IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)

      final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      try (ObjectInputStream ois = new ObjectInputStream(bais)) {
        final Object obj = ois.readObject();
        assertEquals(dut, obj);
      } catch (ClassNotFoundException | IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end --- c.
  } // end method */
} // end class
