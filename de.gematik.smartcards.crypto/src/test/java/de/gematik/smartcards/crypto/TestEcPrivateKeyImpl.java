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

import static de.gematik.smartcards.crypto.AfiElcParameterSpec.brainpoolP256r1;
import static de.gematik.smartcards.utils.AfiUtils.KIBI;
import static de.gematik.smartcards.utils.AfiUtils.VALUES_BOOLEAN;
import static de.gematik.smartcards.utils.EafiHashAlgorithm.SHA_256;
import static de.gematik.smartcards.utils.EafiHashAlgorithm.SHA_384;
import static de.gematik.smartcards.utils.EafiHashAlgorithm.SHA_512;
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
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerOctetString;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
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
 * Class performing white-box tests on {@link EcPrivateKeyImpl}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.AvoidInstantiatingObjectsInLoops",
  "PMD.CyclomaticComplexity",
  "PMD.ExcessiveImports",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname",
})
final class TestEcPrivateKeyImpl {

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

  /** Test method for {@link EcPrivateKeyImpl#EcPrivateKeyImpl(BerTlv, EafiElcPrkFormat)}. */
  @Test
  void test_ElcPrivateKeyimpl__BerTlv_EafiElcPrkFormat() {
    // Assertions:
    // ... a. tlv2ObjectArray(BerTlv, EafiElcPrkFormat)-method works as expected
    // ... b. toTlv(EafiElcPrkFormat)-method works as expected
    // ... c. equals(Object)-method works as expected

    // Note: Because constructor-under-test is rather simple and
    //       because of assertions, we can be lazy here and concentrate
    //       on code-coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: the PKCS#1 format gives not enough information

    // --- a. smoke test
    {
      final var format = EafiElcPrkFormat.ISOIEC7816;
      final var dut = new EcPrivateKeyImpl(brainpoolP256r1);
      assertEquals(dut, new EcPrivateKeyImpl(dut.getEncoded(format), format));
    } // end --- a.

    // --- b. ERROR: the PKCS#1 format gives not enough information
    {
      final var format = EafiElcPrkFormat.PKCS1;
      final var dut = new EcPrivateKeyImpl(brainpoolP256r1);
      final BerTlv tlv = dut.getEncoded(format);
      final Throwable throwable =
          assertThrows(
              ArrayIndexOutOfBoundsException.class, () -> new EcPrivateKeyImpl(tlv, format));
      assertEquals("Index 1 out of bounds for length 1", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- b.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#EcPrivateKeyImpl(BigInteger, ECParameterSpec)}. */
  @Test
  void test_ElcPrivateKeyImpl__BigInteger_EcParameterSpec() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. happy cases for all predefined domain parameters
    // --- b. ERROR: d out of range [1, n-1]
    // --- c. happy case with arbitrary domain parameter, small bit-length

    // --- a. happy cases for all predefined domain parameters
    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      try {
        final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
        keyPairGenerator.initialize(dp);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final var d = ((ECPrivateKey) keyPair.getPrivate()).getS();
        final ECPublicKey puk = (ECPublicKey) keyPair.getPublic();

        final var dut = new EcPrivateKeyImpl(d, dp);

        assertEquals(d, dut.getS());
        assertSame(dp, dut.getParams());
        assertEquals(puk.getW(), dut.getPublicKey().getW());
        assertSame(dp, dut.getPublicKey().getParams());
      } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
        fail(AfiUtils.UNEXPECTED, e);
      } // end Catch (...)

      // --- b. ERROR: d out of range [1, n-1]
      final var nokD =
          List.of(
              BigInteger.ZERO, // supremum of "too small" values
              BigInteger.valueOf(-1), // next to supremum of "too small" values
              dp.getOrder().add(BigInteger.ONE)); // next to infimum of "too large" values
      for (final var d : nokD) {
        final var throwable =
            assertThrows(IllegalArgumentException.class, () -> new EcPrivateKeyImpl(d, dp));
        assertEquals("factor k not in range [1, n]", throwable.getMessage());
        assertNull(throwable.getCause());
      } // end For (d...)

      // infimum of "too large" values
      final var d = dp.getOrder();
      final var throwable =
          assertThrows(IllegalArgumentException.class, () -> new EcPrivateKeyImpl(d, dp));
      assertEquals("w is ECPoint.POINT_INFINITY", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end For (dp...)
    // end --- a, b.

    // --- c. happy case with arbitrary domain parameter, small bit-length
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
      final var dut = new EcPrivateKeyImpl(BigInteger.TEN, dp);
      assertSame(dp, dut.getParams());
    } // end --- c.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#EcPrivateKeyImpl(ECParameterSpec)}. */
  @Test
  void test_ElcPrivateKeyImpl__EcParameterSpec() {
    // Assertions:
    // ... a. rng(BigInteger)-method works as expected
    // ... b. EcPrivateKey(BigInteger, ECParameter)-constructor works as expected

    // Note: Because of the assertions, we can be lazy here and concentrate on
    //       code coverage.

    // Test strategy:
    // --- a. create a bunch of keys
    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      final var dut = new EcPrivateKeyImpl(dp); // NOPMD new in loop

      assertEquals(1, dut.getS().signum());
    } // end For (dp...)
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#EcPrivateKeyImpl(ECPrivateKey)}. */
  @Test
  void test_EcPrivateKeyImpl__EcPrivateKey() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test for over all predefined curves

    // --- a. smoke test for over all predefined curves
    try {
      for (final var dp : AfiElcParameterSpec.PREDEFINED) {
        final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
        keyPairGenerator.initialize(dp);
        final var keyPair = keyPairGenerator.generateKeyPair();
        final var prk = (ECPrivateKey) keyPair.getPrivate();
        final var puk = (ECPublicKey) keyPair.getPublic();

        final var dut = new EcPrivateKeyImpl(prk); // NOPMD new in loop

        assertEquals(prk.getS(), dut.getS());
        assertSame(prk.getParams(), dut.getParams());
        assertEquals(puk.getW(), dut.getPublicKey().getW());
        assertSame(dp, dut.getPublicKey().getParams());
      } // end For (dp...)
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#rng(BigInteger)}. */
  @Test
  void test_rng__BigInteger() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. create random numbers for real world domain parameter
    // --- b. check for small numbers n that all possible values occur

    // --- a. create random numbers for real world domain parameter
    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      final var n = dp.getOrder();

      final var rnd = EcPrivateKeyImpl.rng(n);

      assertEquals(1, rnd.signum()); // 0 < rnd
      assertTrue(rnd.compareTo(n) < 0); // rnd < n
    } // end For (dp...)

    // --- b. check for small numbers n that all possible values occur
    IntStream.range(2, 64)
        .forEach(
            n -> {
              // Note: Random numbers will be generated from the set {1, 2, ..., n-1}.
              //       The following bitPattern has bits b(n-1), ..., b2, b1 with
              //       bit b1 as the least significant bit. At the beginning, all
              //       bits b1, b2, ..., b(n-1) are set.
              BigInteger bitPattern = BigInteger.valueOf((1L << (n - 1)) - 1);

              // Note: Create a bunch of random numbers i and clear bit bi in
              //       bitPattern.
              final var order = BigInteger.valueOf(n);
              for (int counter = 100 * n; counter-- > 0; ) { // NOPMD assignment in operands
                final var rnd = EcPrivateKeyImpl.rng(order).intValueExact();
                assertTrue(0 < rnd);
                assertTrue(rnd < n);

                bitPattern = bitPattern.clearBit(rnd - 1);

                if (0 == bitPattern.signum()) {
                  // ... all possible values observed
                  //     => everything is okay, test passed
                  return;
                } // end fi
              } // end For (counter...)
              // ... loop ended without bitPattern being zero, report

              fail("n = " + n + ", bitPattern = " + bitPattern.toString(16));
            }); // end forEach(n -> ...)
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#tlv2ObjectArray(BerTlv, EafiElcPrkFormat)}. */
  @Test
  void test_tlv2ObjectArray__BerTlv_EafiElcPrkFormat() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. convert from each format
    for (final var format : EafiElcPrkFormat.values()) {
      switch (format) { // NOPMD missing default
        case ASN1 -> checkFromAsn1();
        case ISOIEC7816 -> checkFromIsoIec7816();
        case PKCS1 -> checkFromPkcs1();
        case PKCS8 -> checkFromPkcs8();
        default -> fail("ERROR, implementation missing: " + format);
      } // end Switch (...)
    } // end For (format...)
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#decipher(ConstructedBerTlv)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
  @Test
  void test_decipher__ConstructedBerTlv() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with all domain parameters
    // --- b. ERROR: mismatch in domain parameters
    // --- c. ERROR: point PO_A not on the elliptic curve
    // --- d. ERROR: correct MAC but wrong padding
    // --- e. ERROR: wrong MAC
    // --- f. ERROR: invalid OID
    // --- g. ERROR: invalid Padding Indicator
    // --- h. ERROR: missing DO

    final var invalidPoint = new ECPoint(BigInteger.ZERO, BigInteger.TEN);

    boolean flag; // true => no error expected, false = error expected

    // --- a. smoke test with all domain parameters
    for (final var dpPrk : AfiElcParameterSpec.PREDEFINED) {
      final var dut = new EcPrivateKeyImpl(dpPrk); // NOPMD new in loop

      for (final var mlen : RNG.intsClosed(0, KIBI, 5).toArray()) {
        final var expected = RNG.nextBytes(mlen);

        // --- b. ERROR: mismatch in domain parameters
        for (final var dpPuk : AfiElcParameterSpec.PREDEFINED) {
          flag = dpPrk.equals(dpPuk);
          final var prkReceiver = flag ? dut : new EcPrivateKeyImpl(dpPuk); // NOPMD new in loop
          final var pukReceiver = prkReceiver.getPublicKey();

          // encipher according to                                               // (N091.650)c.3
          final var prkEphemeral = new EcPrivateKeyImpl(dpPuk); // NOPMD new     // (N004.500)b
          final var pukEphemeral = prkEphemeral.getPublicKey();

          // --- c. ERROR: point PO_A not on the elliptic curve
          for (final var flagPoa : VALUES_BOOLEAN) {
            flag &= flagPoa;
            final var pea = flagPoa ? pukEphemeral.getW() : invalidPoint; // (N004.500)c
            final var kab = AfiElcUtils.sharedSecret(prkEphemeral, pukReceiver); // (N004.500)d
            final var kdEnc = AfiUtils.concatenate(kab, Hex.toByteArray("0000 0001"));
            final var kdMac = AfiUtils.concatenate(kab, Hex.toByteArray("0000 0002"));
            final var kEnc = new AesKey(SHA_256.digest(kdEnc), 0, 32); // NOPMD  // (N001.520)a
            final var kMac = new AesKey(SHA_256.digest(kdMac), 0, 32); // NOPMD  // (N001.520)b
            final var t2 = BigInteger.ZERO; // (N001.520)c
            final var t1 = kEnc.encipherCbc(AfiBigInteger.i2os(t2, 16)); // (N004.500)e.2
            final var poa = AfiElcUtils.p2osUncompressed(pea, dpPuk); // (N004.500)f
            final var messagePadded = kEnc.padIso(expected);

            // --- d. ERROR: correct MAC but wrong padding
            for (final var flagPadding : VALUES_BOOLEAN) {
              flag &= flagPadding;
              if (!flagPadding) {
                messagePadded[messagePadded.length - 1]++;
              } // end fi
              final var c = kEnc.encipherCbc(messagePadded, t1); // (N004.500)g
              final var t = kMac.calculateCmac(kMac.padIso(c), 8); // (N004.500)h

              // --- e. ERROR: wrong MAC
              for (final var flagMac : VALUES_BOOLEAN) {
                flag &= flagMac;
                if (!flagMac) {
                  t[0]++;
                } // end fi

                // --- f. ERROR: invalid OID
                for (final var flagOid : VALUES_BOOLEAN) {
                  flag &= flagOid;
                  final var oid = flagOid ? dpPuk.getOid() : AfiOid.id_RSAES_OAEP;

                  final var oidDo = new DerOid(oid); // NOPMD new in loop       // (N091.650)c.3.ii
                  final var keyDo =
                      BerTlv.getInstance(
                          0x7f49, List.of(BerTlv.getInstance(0x86, poa))); // (N091.650)c.3.iii

                  // --- g. ERROR: invalid Padding Indicator
                  for (final var flagPi : VALUES_BOOLEAN) {
                    flag &= flagPi;
                    final var cipherDo =
                        BerTlv.getInstance(
                            0x86,
                            AfiUtils.concatenate(
                                new byte[] {(byte) (flagPi ? 2 : 1)}, c) // NOPMD new
                            ); // (N091.650)c.3.iv
                    final var macDo = BerTlv.getInstance(0x8e, t); // (N091.650)c.3.v

                    // --- h. ERROR: missing DO
                    for (final var template :
                        Set.of(
                            List.of(keyDo, cipherDo, macDo), // OID missing
                            List.of(oidDo, cipherDo, macDo), // key missing
                            List.of(oidDo, keyDo, macDo), // cipherDo missing
                            List.of(oidDo, keyDo, cipherDo), // macDo missing
                            List.of(oidDo, keyDo, cipherDo, macDo) // everything ok
                            )) {
                      final var success = flag & (4 == template.size());
                      final var cipher =
                          (ConstructedBerTlv)
                              BerTlv.getInstance(0xa6, template); // (N091.500)c.3.vi

                      if (success) {
                        final var actual = dut.decipher(cipher);

                        assertArrayEquals(expected, actual);
                      } else {
                        // spotless:off
                        final var throwable = assertThrows(
                            IllegalArgumentException.class,
                            () -> dut.decipher(cipher),
                            () -> String.format(
                                "cur=%s, POA=%s, pad=%s, MAC=%s, OID=%s, PI=%s, size=%d%n%s",
                                dpPrk.equals(dpPuk),
                                flagPoa,
                                flagPadding,
                                flagMac,
                                flagOid,
                                flagPi,
                                template.size(),
                                cipher.toStringTree()));
                        // spotless:on
                        assertEquals("decipher error", throwable.getMessage());
                      } // end else
                    } // end For (template...)
                  } // end For (flagPi...)
                } // end For (flagOid...)
              } // end For (flagMac...)
            } // end For (flagPadding...)
          } // end For (flagPoa...)
        } // end For (dpPuk...)
      } // end For (mlen...)
    } // end For (dpPrk...)
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. loop over predefined curves
    // a.1 same reference
    // a.2 null input
    // a.3 difference in type
    // a.4 difference in domain parameter
    // a.5 difference in private key d
    // a.6 different objects same content

    // --- b. arbitrary domain parameter
    // b.1 same reference
    // b.2 null input
    // b.3 difference in type
    // b.4 difference in domain parameter
    // b.5 difference in private key d

    // --- a. loop over predefined curves
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final var dut = new EcPrivateKeyImpl(dp);

          for (final Object obj :
              new Object[] {
                dut, // a.1 same reference
                null, // a.2 null input
                "afi" // a.3 difference in type
              }) {
            assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals to compare object refer.
          } // end For (obj...)

          // a.4 difference in domain parameter
          // a.5 difference in private key d
          AfiElcParameterSpec.PREDEFINED.forEach(
              dpInner -> {
                EcPrivateKeyImpl inner;
                do {
                  inner = new EcPrivateKeyImpl(dpInner); // NOPMD new in loop
                } while (dut.getS().equals(inner.getS()));
                // ... private key d differ

                final var actual = dut.equals(inner);
                assertFalse(actual);
              }); // end forEach(dp -> ...)

          // a.6 different objects same content
          final Object obj = new EcPrivateKeyImpl(dut.getS(), dut.getParams());
          assertNotSame(dut, obj);
          final var actual = dut.equals(obj);
          assertTrue(actual);
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
      final var dut = new EcPrivateKeyImpl(BigInteger.TWO, dp);

      for (final Object obj :
          new Object[] {
            dut, // b.1 same reference
            null, // b.2 null input
            "afi", // b.3 difference in type
            // b.4 difference in domain parameter
            new EcPrivateKeyImpl(
                dut.getS(),
                new ECParameterSpec(
                    new EllipticCurve(
                        new ECFieldFp(AfiBigInteger.SEVEN), BigInteger.TWO, AfiBigInteger.SIX),
                    new ECPoint(BigInteger.TWO, BigInteger.TWO),
                    BigInteger.valueOf(11),
                    1)),
            new EcPrivateKeyImpl(BigInteger.TEN, dp) // b.5 difference in private key d
          }) {
        assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals to compare object refer.
      } // end For (obj...)
    } // end --- b.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#hashCode()}. */
  @Test
  void test_hashCode() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. loop over predefined curves
    // --- b. call hashCode()-method again

    // --- a. loop over predefined curves
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final var dut = new EcPrivateKeyImpl(dp);
          assertEquals(31 * dp.hashCode() + dut.getS().hashCode(), dut.hashCode());
        }); // end forEach(dp -> ...)
    // end --- a.

    // --- b. call hashCode()-method again
    // Note: The main reason for this check is to get full code-coverage.
    //        a. The first time this method is called on a newly constructed BerTlv object
    //           insHashCode is zero.
    //        b. The second time this method is called the insHashCode isn't zero
    //           (with a high probability).
    {
      final ECParameterSpec dp = AfiElcParameterSpec.brainpoolP384r1;
      final var dut = new EcPrivateKeyImpl(dp);
      assertEquals(31 * dp.hashCode() + dut.getS().hashCode(), dut.hashCode());
      final var hash = dut.hashCode();
      assertEquals(hash, dut.hashCode());
    } // end --- b.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#getAlgorithm()}. */
  @Test
  void test_getAlgorithm() {
    // Test strategy:
    // --- a. loop over all predefined domain parameters
    // --- b. check that the same result occurs than for JRE keys

    try {
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);

      // --- a. loop over all predefined domain parameters
      for (final var dp : AfiElcParameterSpec.PREDEFINED) {
        keyPairGenerator.initialize(dp);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final ECPrivateKey prk = (ECPrivateKey) keyPair.getPrivate();
        final var expected = prk.getAlgorithm();

        // --- b. check that the same result occurs than for JRE keys
        final var dut = new EcPrivateKeyImpl(dp); // NOPMD new in loop

        final var actual = dut.getAlgorithm();

        assertEquals(expected, actual);
      } // end For (dp...)
      // end --- a, b.
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#getEncoded()}. */
  @Test
  void test_getEncoded() {
    // Assertions.
    // ... a. getEncoded(EafiElcPrkFormat)-method works as expected

    // Test strategy:
    // --- a. loop over all predefined domain parameters
    // --- b. check that the same result occurs than for JRE keys

    try {
      // Note: Intentionally here NOT the bouncy-castle provider is used,
      //       because that provider uses a different encoding format.
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC");

      // --- a. loop over all predefined domain parameters
      for (final var dp : AfiElcParameterSpec.PREDEFINED) {
        try {
          keyPairGenerator.initialize(dp);
          for (int i = 10; i-- > 0; ) { // NOPMD assignment in operands
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            final ECPrivateKey prk = (ECPrivateKey) keyPair.getPrivate();

            // --- b. check that the same result occurs than for JRE keys
            final var dut = new EcPrivateKeyImpl(prk.getS(), dp); // NOPMD new in loop

            assertEquals(
                BerTlv.getInstance(prk.getEncoded()).toString(" "),
                BerTlv.getInstance(dut.getEncoded()).toString(" "));
          } // end For (i...)
        } catch (InvalidAlgorithmParameterException e) {
          // ... domain parameter not supported by JRE
          //     => check for brainpool curve

          if (!dp.getOid().getName().startsWith("brainpool")) {
            // ... not a brainpool curve, that is unexpected
            fail(AfiUtils.UNEXPECTED, e);
          } // end fi
        } // end Catch (...)
      } // end For (dp...)
    } catch (NoSuchAlgorithmException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#getEncoded(EafiElcPrkFormat)}. */
  @Test
  void test_getEncoded__ElcPrkFormat() {
    // Test strategy:
    // --- a. convert for each format
    for (final var format : EafiElcPrkFormat.values()) {
      switch (format) { // NOPMD missing default
        case ASN1 -> zzTestGetEncodedElcPrkFormatAns1();
        case ISOIEC7816 -> zzTestGetEncodedElcPrkFormatIsoIec7816();
        case PKCS1 -> zzTestGetEncodedElcPrkFormatPkcs1();
        case PKCS8 -> zztestGetEncodedElcPrkFormatPkcs8();
        default -> fail("ERROR, implementation missing: " + format);
      } // end Switch (...)
    } // end For (format)...)
  } // end method */

  private void zzTestGetEncodedElcPrkFormatAns1() {
    // Assertions:
    // ... a. getInstance(PKCS1)-method works as expected

    // Test strategy:
    // --- a. smoke test with arbitrary domain parameter
    // --- b. loop over all predefined curves

    // --- a. smoke test with arbitrary domain parameter
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
                  1)); // cofactor h
      final var dut = new EcPrivateKeyImpl(BigInteger.TWO, dp);
      final var expected =
          String.format(
              "30 3e%n"
                  + "|  02 01 00%n"
                  + "|  30 2f%n"
                  + "|  |  06 07 2a8648ce3d0201%n"
                  + "|  |  30 24%n"
                  + "|  |  |  02 01 01%n"
                  + "|  |  |  30 0c%n"
                  + "|  |  |  |  06 07 2a8648ce3d0101%n"
                  + "|  |  |  |  02 01 05%n"
                  + "|  |  |  30 06%n"
                  + "|  |  |  |  04 01 02%n"
                  + "|  |  |  |  04 01 04%n"
                  + "|  |  |  04 03 040401%n"
                  + "|  |  |  02 01 07%n"
                  + "|  |  |  02 01 01%n"
                  + "|  04 08 3006020101040102");

      assertEquals(expected, dut.getEncoded(EafiElcPrkFormat.ASN1).toString(" ", "|  "));
    } // end --- a.

    // --- b. loop over all predefined curves
    {
      final var doEcPublicKey = new DerOid(AfiOid.ecPublicKey);
      final var doFieldType = new DerOid(AfiOid.fieldType);

      AfiElcParameterSpec.PREDEFINED.forEach(
          dp -> {
            final var p = dp.getP();
            final var a = dp.getCurve().getA();
            final var b = dp.getCurve().getB();
            final var g = dp.getGenerator();
            final var n = dp.getOrder();
            final var h = dp.getCofactor();
            final var dut = new EcPrivateKeyImpl(dp);
            final BerTlv expected =
                new DerSequence(
                    List.of(
                        new DerInteger(BigInteger.ZERO),
                        new DerSequence(
                            List.of(
                                doEcPublicKey,
                                new DerSequence(
                                    List.of(
                                        new DerInteger(BigInteger.ONE),
                                        new DerSequence(List.of(doFieldType, new DerInteger(p))),
                                        new DerSequence(
                                            List.of(
                                                new DerOctetString(a.toByteArray()),
                                                new DerOctetString(b.toByteArray()))),
                                        new DerOctetString(AfiElcUtils.p2osUncompressed(g, dp)),
                                        new DerInteger(n),
                                        new DerInteger(BigInteger.valueOf(h)))))),
                        new DerOctetString(dut.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())));

            assertEquals(
                expected.toStringTree(), dut.getEncoded(EafiElcPrkFormat.ASN1).toStringTree());
          }); // end forEach(dp -> ...)
    } // end --- b.
  } // end method */

  private void zzTestGetEncodedElcPrkFormatIsoIec7816() {
    // Test strategy:
    // --- a. loop over all predefined curves
    // --- b. ERROR: no OID for domain parameter defined

    // --- a. loop over all predefined curves
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final var dut = new EcPrivateKeyImpl(dp);
          final BerTlv expected =
              BerTlv.getInstance(
                  0x7f48, List.of(new DerOid(dp.getOid()), new DerInteger(dut.getS())));

          assertEquals(
              expected.toStringTree(), dut.getEncoded(EafiElcPrkFormat.ISOIEC7816).toStringTree());
        }); // end forEach(dp -> ...)
    // end --- a.

    // --- b. ERROR: no OID for domain parameter defined
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
      final var dut = new EcPrivateKeyImpl(dpDut);

      assertThrows(NoSuchElementException.class, () -> dut.getEncoded(EafiElcPrkFormat.ISOIEC7816));
    } // end --- b.
  } // end method */

  private void zzTestGetEncodedElcPrkFormatPkcs1() {
    // Test strategy:
    // --- a. smoke test with arbitrary domain parameter
    // --- b. loop over all predefined curves

    // --- a. smoke test with arbitrary domain parameter
    {
      // p=5, a=2, b=4, G=(4,1), n=7, h=1, (G) = {(4,1), (2,4), (0,3), (0,2), (2,1), (4,4), O}
      final var dp =
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
      final var dut = new EcPrivateKeyImpl(BigInteger.TWO, dp);

      assertEquals(
          "30 06  02 01 01  04 01 02", dut.getEncoded(EafiElcPrkFormat.PKCS1).toString(" "));
    } // end --- a.

    // --- b. loop over all predefined curves
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final var dut = new EcPrivateKeyImpl(dp);
          final var expected =
              new DerSequence(
                  List.of(
                      new DerInteger(BigInteger.ONE),
                      new DerOctetString(AfiBigInteger.i2os(dut.getS(), dp.getTau()))));

          assertEquals(
              expected.toString(" ", "|  "),
              dut.getEncoded(EafiElcPrkFormat.PKCS1).toString(" ", "|  "));
        }); // end forEach(dp -> ...)
  } // end method */

  private void zztestGetEncodedElcPrkFormatPkcs8() {
    // Assertions:
    // ... a. toTlv(PKCS1)-method works as expected

    // Test strategy:
    // --- a. loop over all predefined curves
    // --- b. ERROR: no OID for domain parameter defined

    // --- a. loop over all predefined curves
    {
      final var doEcPublicKey = new DerOid(AfiOid.ecPublicKey);
      AfiElcParameterSpec.PREDEFINED.forEach(
          dp -> {
            final var dut = new EcPrivateKeyImpl(dp);
            final BerTlv expected =
                new DerSequence(
                    List.of(
                        new DerInteger(BigInteger.ZERO),
                        new DerSequence(List.of(doEcPublicKey, new DerOid(dp.getOid()))),
                        new DerOctetString(dut.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())));

            assertEquals(
                expected.toStringTree(), dut.getEncoded(EafiElcPrkFormat.PKCS8).toStringTree());
          }); // end forEach(dp -> ...)
    } // end --- a.

    // --- b. ERROR: no OID for domain parameter defined
    {
      final var dp = AfiElcParameterSpec.brainpoolP512r1;
      final var dpDut =
          new AfiElcParameterSpec(
              dp.getP(),
              dp.getCurve().getA(),
              dp.getCurve().getB(),
              dp.getGenerator().getAffineX(),
              dp.getP().subtract(dp.getGenerator().getAffineY()),
              dp.getOrder(),
              dp.getCofactor());
      final var dut = new EcPrivateKeyImpl(dpDut);

      assertThrows(NoSuchElementException.class, () -> dut.getEncoded(EafiElcPrkFormat.PKCS8));
    }
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#getFormat()}. */
  @Test
  void test_getFormat() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. loop over all predefined domain parameters
    // --- b. check that the same result occurs than for JRE keys

    try {
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);

      // --- a. loop over all predefined domain parameters
      for (final var dp : AfiElcParameterSpec.PREDEFINED) {
        keyPairGenerator.initialize(dp);
        final var keyPair = keyPairGenerator.generateKeyPair();
        final var prk = (ECPrivateKey) keyPair.getPrivate();
        final var expected = prk.getFormat();

        // --- b. check that the same result occurs than for JRE keys
        final var dut = new EcPrivateKeyImpl(dp); // NOPMD new in loop
        final var actual = dut.getFormat();

        assertEquals(expected, actual);
      } // end For (dp...)
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#getParams()}. */
  @Test
  void test_getParams() {
    // Test strategy:
    // --- a. loop over all predefined domain parameters
    // --- b. arbitrary domain parameter

    // --- a. loop over all predefined domain parameters
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final ECParameterSpec parameterSpec =
              new ECParameterSpec(
                  dp.getCurve(), dp.getGenerator(), dp.getOrder(), dp.getCofactor());
          final var dut = new EcPrivateKeyImpl(parameterSpec);
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
      final var dut = new EcPrivateKeyImpl(parameterSpec);
      final ECParameterSpec dpDut = dut.getParams();
      assertEquals(AfiElcParameterSpec.class, dpDut.getClass());
      assertEquals(parameterSpec.getCurve(), dpDut.getCurve());
      assertEquals(parameterSpec.getGenerator(), dpDut.getGenerator());
      assertEquals(parameterSpec.getOrder(), dpDut.getOrder());
      assertEquals(parameterSpec.getCofactor(), dpDut.getCofactor());
    } // end --- b.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#getPublicKey()}. */
  @Test
  void test_getPublicKey() {
    // Assertions.
    // - none -

    // Test strategy:
    // --- a. loop over all predefined domain parameters
    // --- b. check that the same result occurs than for JRE keys

    try {
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);

      // --- a. loop over all predefined domain parameters
      for (final var dp : AfiElcParameterSpec.PREDEFINED) {
        keyPairGenerator.initialize(dp);
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        final ECPrivateKey prk = (ECPrivateKey) keyPair.getPrivate();
        final ECPublicKey puk = (ECPublicKey) keyPair.getPublic();
        final var expected = new EcPublicKeyImpl(puk.getW(), dp); // NOPMD new in loop

        // --- b. check that the same result occurs than for JRE keys
        final var dut = new EcPrivateKeyImpl(prk.getS(), dp); // NOPMD new in loop

        final var actual = dut.getPublicKey();

        assertEquals(expected, actual);
      } // end For (dp...)
    } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#signEcdsa(byte[])}. */
  @Test
  void test_signEcdsa__byte() {
    // Assertions:
    // ... a. signEcdsa(BigInteger)-method works as expected
    // ... b. calculateHashValue(message)-method words as expected
    // ... c. fromSequence(DerSequence, int)-method works as expected

    // Test strategy:
    // --- a. smoke test
    try {
      final var dp = AfiElcParameterSpec.ansix9p521r1;
      final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
      keyPairGenerator.initialize(dp);

      final KeyPair keyPair = keyPairGenerator.generateKeyPair();
      final var dut = new EcPrivateKeyImpl(((ECPrivateKey) keyPair.getPrivate()).getS(), dp);
      final ECPublicKey puk = (ECPublicKey) keyPair.getPublic();
      final String algorithm = "SHA512withECDSA";
      final Signature verifier = Signature.getInstance(algorithm, BC);
      verifier.initVerify(puk);
      final var message = RNG.nextBytes(64, 128);

      final var signature = dut.signEcdsa(message);

      verifier.update(message);
      assertTrue(verifier.verify(EcPublicKeyImpl.toSequence(signature).getEncoded()));
    } catch (InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | SignatureException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#signEcdsa(byte[], BigInteger)}. */
  @Test
  void test_signEcdsa__byteA_BigInteger() {
    // Assertion:
    // ... a. signEcdsaDer(BigInteger, BigInteger)-method works as expected
    // ... b. calculateHashValue(message)-method words as expected

    // Test strategy:
    // --- a. Test vectors from ANSI X9.62
    // spotless:off
    final var list = List.of(
        List.of(// example from ANSI X9.62 v2005, clause L.4.2
            /* d */ "20186677036482506117540275567393538695075300175221296989956723148347484984008",
            AfiOid.ansix9p256r1.getOctetString(),
            /* m */ "Example of ECDSA with ansip256r1 and SHA-256",
            /* k */ "72546832179840998877302529996971396893172522460793442785601695562409154906335",
            /* r */ "97354732615802252173078420023658453040116611318111190383344590814578738210384",
            /* s */ "98506158880355671805367324764306888225238061309262649376965428126566081727535"
        ),
        List.of(// example from ANSI X9.62 v2005, clause L.4.3
            /* d */ "617573726813476282316253885608633222275541026607493641741273231656161177732180"
                + "358888434629562647985511298272498852936680947729040673640492310550142822667389",
            AfiOid.ansix9p521r1.getOctetString(),
            /* m */ "Example of ECDSA with ansip521r1 and SHA-512",
            /* k */ "680653287821550352084510981843217484761695867533539777370032409758497463972872"
                + "5689481598054743894544060040710846048585856076812050552869216017728862957612913",
            /* r */ "136892619581212740795614074472225740353586416818253432118855346036565286568604"
                + "0549247096155740756318290773648848859639978618869784291633651685766829574104630",
            /* s */ "162475472034888371560812215121400303239868541500393573448544599906560997930481"
                + "1509538477657407457976246218976767156629169821116579317401249024208611945405790"
        )
    );
    // spotless:on
    for (final var input : list) {
      int index = -1;
      final var d = new BigInteger(input.get(++index));
      final var dp =
          AfiElcParameterSpec.getInstance(
              new AfiOid(AfiOid.os2Components(Hex.toByteArray(input.get(++index)))));
      final var message = input.get(++index).getBytes(StandardCharsets.UTF_8);
      final var k = new BigInteger(input.get(++index));
      final var r = new BigInteger(input.get(++index));
      final var s = new BigInteger(input.get(++index));
      final var expected = new DerSequence(List.of(new DerInteger(r), new DerInteger(s)));
      final var dut = new EcPrivateKeyImpl(d, dp);

      final var actual = dut.signEcdsa(message, k);

      assertEquals(expected, actual);
    } // end For(input...)
    // end --- a.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#signEcdsaDer(byte[])}. */
  @Test
  void test_signEcdsaDer__byteA() {
    // Assertions:
    // ... a. signEcdsaDer(BigInteger)-method works as expected
    // ... b. calculateHashValue(message)-method words as expected

    // Note 1: The test strategy here is to create signatures with
    //         method-under-test and check those signatures by JRE.

    // Test strategy:
    // --- a. loop over all predefined domain parameters
    // --- b. create a bunch of key-pairs
    // --- c. create a bunch of messages

    // --- a. loop over all predefined domain parameter
    try {
      for (final var dp : AfiElcParameterSpec.PREDEFINED) {
        final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
        keyPairGenerator.initialize(dp);

        // --- b. create a bunch of key-pairs
        for (int i = 10; i-- > 0; ) { // NOPMD assignment in operand
          final KeyPair keyPair = keyPairGenerator.generateKeyPair();
          final var dut = new EcPrivateKeyImpl(((ECPrivateKey) keyPair.getPrivate()).getS(), dp);
          final ECPublicKey puk = (ECPublicKey) keyPair.getPublic();

          final var size = dp.getP().bitLength();
          final String algorithm;
          if (size <= 256) { // NOPMD literal in if statement
            algorithm = "SHA256withECDSA";
          } else if (size <= 384) { // NOPMD literal in if statement
            algorithm = "SHA384withECDSA";
          } else {
            algorithm = "SHA512withECDSA";
          } // end fi

          // --- c. create a bunch of messages
          final Signature verifier = Signature.getInstance(algorithm, BC);
          verifier.initVerify(puk);
          for (final var length : RNG.intsClosed(0, 1000, 20).toArray()) {
            final var message = RNG.nextBytes(length);

            final var signature = dut.signEcdsaDer(message);

            verifier.update(message);
            assertTrue(verifier.verify(signature.getEncoded()));
          } // end For (length...)
        } // end For (i...)
      } // end For (dp...)
    } catch (InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | SignatureException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
    // end --- a, b, c.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#signEcdsaDer(BigInteger)}. */
  @Test
  void test_signEcdsaDer__BigInteger() {
    // Assertions:
    // ... a. signEcdsaDer(BigInteger, BigInteger)-method works as expected

    // Note 1: The basic functionality is tested in "test_signEcdsaDer__byteA".
    //         Thus, here we concentrate on corner cases.

    // Test strategy:
    // --- a. r or s equals zero
    // --- b. k not invertible

    // --- a. r or s equals zero
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
                  1)); // cofactor h
      final var dut = new EcPrivateKeyImpl(BigInteger.TWO, dp);

      for (final var m : List.of("ce2315", "ca")) {
        final var message = Hex.toByteArray(m);

        // create a bunch of signatures and hope that at least once
        // (r==0) and (s==0) occurs (for good code coverage)
        for (int i = 100; i-- > 0; ) { // NOPMD assignment in operand
          final var signature = dut.signEcdsaDer(message);

          assertTrue(dut.getPublicKey().verifyEcdsa(message, signature));
        } // end For (i...)
      } // end For (m...)
    } // end --- a.

    // --- b. ERROR: k not invertible mod n
    {
      // p=11 a=1, b=7, G=(3,2), n=15, h=1
      final AfiElcParameterSpec dp =
          AfiElcParameterSpec.getInstance(
              new ECParameterSpec(
                  new EllipticCurve(
                      new ECFieldFp(BigInteger.valueOf(11)), // p
                      BigInteger.valueOf(1), // a
                      BigInteger.valueOf(7) // b
                      ),
                  new ECPoint(
                      BigInteger.valueOf(3), // Gx
                      BigInteger.valueOf(2) // Gy
                      ),
                  BigInteger.valueOf(15), // order n
                  1 // cofactor h
                  ));
      final var dut = new EcPrivateKeyImpl(BigInteger.TWO, dp);
      final var message = Hex.toByteArray("ca");

      // create a bunch of signatures and hope that at least once
      // k is not invertible
      IntStream.range(0, 100)
          .forEach(
              i -> {
                final var signature = dut.signEcdsaDer(message);

                try {
                  assertTrue(dut.getPublicKey().verifyEcdsa(message, signature));
                } catch (ArithmeticException e) {
                  assertEquals("BigInteger not invertible.", e.getMessage());
                } // end Catch (...)
              }); // end forEach(i -> ...)
    } // end --- b.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#signEcdsaDer(BigInteger, BigInteger)}. */
  @Test
  void test_signEcdsaDer__BigInteger_BigInteger() {
    // Assertion:
    // - none -

    // Note 1: The basic functionality is tested in "test_signEcdsaDer__byteA".
    //         Thus, here we concentrate on corner cases.

    // Test strategy:
    // --- a. intentionally empty
    // --- b. loop over all predefined domain parameters
    // --- c. ERROR: k too small
    // --- d. ERROR: k too large
    // --- e. ERROR: k not invertible mod n
    // --- f. ERROR: r is zero
    // --- g. ERROR: s is zero

    // --- b. loop over all predefined domain parameters
    try {
      for (final var dp : AfiElcParameterSpec.PREDEFINED) {
        final var keyPairGenerator = KeyPairGenerator.getInstance("EC", BC);
        keyPairGenerator.initialize(dp);

        // create a bunch of key-pairs
        for (int i = 2; i-- > 0; ) { // NOPMD assignment in operand
          final KeyPair keyPair = keyPairGenerator.generateKeyPair();
          final var dut = new EcPrivateKeyImpl(((ECPrivateKey) keyPair.getPrivate()).getS(), dp);
          final ECPublicKey puk = (ECPublicKey) keyPair.getPublic();

          final var size = dp.getP().bitLength();
          final String algorithm;
          if (size <= 256) { // NOPMD literal in if statement
            algorithm = "SHA256withECDSA";
          } else if (size <= 384) { // NOPMD literal in if statement
            algorithm = "SHA384withECDSA";
          } else {
            algorithm = "SHA512withECDSA";
          } // end fi

          final Signature verifier = Signature.getInstance(algorithm, BC);
          verifier.initVerify(puk);

          // create a bunch of messages
          for (final var length : RNG.intsClosed(0, 1000, 5).toArray()) {
            final var message = RNG.nextBytes(length);
            final var k = new BigInteger(255, RNG);

            final var signature = dut.signEcdsa(message, k);

            verifier.update(message);

            assertTrue(verifier.verify(signature.getEncoded()));
          } // end For (length...)

          // --- c. ERROR: k too small
          List.of(
                  BigInteger.valueOf(-1), // next to supremum of "too small"
                  BigInteger.ZERO // supremum of "too small"
                  )
              .forEach(
                  k -> {
                    final Throwable throwable =
                        assertThrows(
                            IllegalArgumentException.class,
                            () -> dut.signEcdsa(AfiUtils.EMPTY_OS, k));
                    assertEquals("factor k not in range [1, n]", throwable.getMessage());
                    assertNull(throwable.getCause());
                  }); // end forEach(k -> ...)

          // --- d. ERROR: k too large
          List.of(
                  dp.getOrder(), // infimum of "too large"
                  dp.getOrder().add(BigInteger.ONE) // next to infimum of "too large"
                  )
              .forEach(
                  k -> {
                    final Throwable throwable =
                        assertThrows(
                            IllegalArgumentException.class,
                            () -> dut.signEcdsa(AfiUtils.EMPTY_OS, k));
                    assertEquals("factor k greater or equal to order n", throwable.getMessage());
                    assertNull(throwable.getCause());
                  }); // end forEach(k -> ...)
        } // end For (i...)
      } // end For (dp...)
    } catch (InvalidAlgorithmParameterException
        | InvalidKeyException
        | NoSuchAlgorithmException
        | SignatureException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)

    // --- e. ERROR: k not invertible mod n
    {
      // p=11 a=1, b=7, G=(3,2), n=15, h=1
      final AfiElcParameterSpec dp =
          AfiElcParameterSpec.getInstance(
              new ECParameterSpec(
                  new EllipticCurve(
                      new ECFieldFp(BigInteger.valueOf(11)), // p
                      BigInteger.valueOf(1), // a
                      BigInteger.valueOf(7) // b
                      ),
                  new ECPoint(
                      BigInteger.valueOf(3), // Gx
                      BigInteger.valueOf(2) // Gy
                      ),
                  BigInteger.valueOf(15), // order n
                  1 // cofactor h
                  ));
      final var dut = new EcPrivateKeyImpl(BigInteger.TWO, dp);
      final var message = Hex.toByteArray("ca");
      final var k = BigInteger.valueOf(3);
      final var throwable =
          assertThrows(ArithmeticException.class, () -> dut.signEcdsa(message, k));
      assertEquals("BigInteger not invertible.", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- a.

    // --- f. ERROR: r is zero
    {
      // p=5, a=2, b=4, G=(4,1), n=7, h=1, (G) = {(4,1), (2,4), (0,3), (0,2), (2,1), (4,4), O}
      final AfiElcParameterSpec dp =
          AfiElcParameterSpec.getInstance(
              new ECParameterSpec(
                  new EllipticCurve(
                      new ECFieldFp(BigInteger.valueOf(5)), // p = 5
                      BigInteger.valueOf(2), // a = 2
                      BigInteger.valueOf(4) // b = 4
                      ),
                  new ECPoint(
                      BigInteger.valueOf(4), // Gx
                      BigInteger.valueOf(1) // Gy
                      ),
                  BigInteger.valueOf(7), // n
                  1 // cofactor
                  ));
      final var dut = new EcPrivateKeyImpl(BigInteger.TWO, dp);
      final var message = Hex.toByteArray("ca");
      final var k = BigInteger.valueOf(3);
      final var throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.signEcdsa(message, k));
      assertEquals("0 == r", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- f.

    // --- g. ERROR: s is zero
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
      final var dut = new EcPrivateKeyImpl(BigInteger.TWO, dp);
      final var message = Hex.toByteArray("ce2315");
      final var k = BigInteger.TWO;

      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.signEcdsa(message, k));
      assertEquals("0 == s", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- g.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#toString()}. */
  @Test
  void test_toString() {
    // Test strategy:
    // --- a. loop over all predefined domain parameter
    // --- b. arbitrary domain parameter, i.e. no OID available

    // --- a. loop over all predefined domain parameter
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final var dut = new EcPrivateKeyImpl(dp);

          assertEquals(
              String.format(
                  "Domain parameter OID = %s, d = '%s'", dp.getOid(), dut.getS().toString(16)),
              dut.toString());
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
              1); // cofactor
      final var dut = new EcPrivateKeyImpl(dp);

      assertEquals(
          String.format("Domain parameter: %s%nd = '%s'", dp, dut.getS().toString(16)),
          dut.toString());
    } // end --- b.
  } // end method */

  /** Test method for {@link EcPrivateKeyImpl#calculateHashValue(byte[])}. */
  @Test
  void test_calculateHashValue__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with predefined curves
    for (final var dp : AfiElcParameterSpec.PREDEFINED) {
      // spotless:off
      final var hashFunction = switch (dp.getTau()) {
        case 32 -> SHA_256;
        case 48 -> SHA_384;
        default -> SHA_512;
      }; // end Switch (...)
      // spotless:on
      final var dut = new EcPrivateKeyImpl(dp);
      final var input = RNG.nextBytes(50, 100);
      final var expected = new BigInteger(1, hashFunction.digest(input));

      final var present = dut.calculateHashValue(input);

      assertEquals(expected, present);
    } // end For (dp...)
  } // end method */

  /** Test method for serialization and deserialization. */
  @SuppressWarnings({"PMD.NPathComplexity"})
  @Test
  void test_serialize() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. predefined domain parameter
    // --- b. arbitrary curve (i.e., not a named curve)
    // --- c. small size of domain parameter

    // --- a. predefined domain parameter
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          // --- a. predefined domain parameter
          {
            final var dut = new EcPrivateKeyImpl(dp);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
              oos.writeObject(dut);
            } catch (IOException e) {
              fail(AfiUtils.UNEXPECTED, e);
            } // end Catch (...)

            final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
              final Object obj = ois.readObject();
              assertEquals(dut, obj);
              assertEquals(dut.getPublicKey(), ((EcPrivateKeyImpl) obj).getPublicKey());
            } catch (ClassNotFoundException | IOException e) {
              fail(AfiUtils.UNEXPECTED, e);
            } // end Catch (...)
          }
          // end --- a.

          // --- b. arbitrary curve (i.e., not a named curve), here use -G as generator
          {
            final AfiElcParameterSpec dp2 =
                AfiElcParameterSpec.getInstance(
                    new ECParameterSpec(
                        dp.getCurve(),
                        // with G as generator form predefined domain parameter here -G is used
                        new ECPoint(
                            dp.getGenerator().getAffineX(),
                            dp.getP().subtract(dp.getGenerator().getAffineY())),
                        dp.getOrder(),
                        dp.getCofactor()));
            assertTrue(AfiElcUtils.isPointOnEllipticCurve(dp2.getGenerator(), dp));

            final var dut = new EcPrivateKeyImpl(dp2);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
              oos.writeObject(dut);
            } catch (IOException e) {
              fail(AfiUtils.UNEXPECTED, e);
            } // end Catch (...)

            final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
              final Object obj = ois.readObject();
              assertEquals(dut, obj);
              assertEquals(dut.getPublicKey(), ((EcPrivateKeyImpl) obj).getPublicKey());
            } catch (ClassNotFoundException | IOException e) {
              fail(AfiUtils.UNEXPECTED, e);
            } // end Catch (...)
          } // end --- b.
        }); // end forEach(dp -> ...)
    // end --- a, b.

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
                  1)); // cofactor h
      final var dut = new EcPrivateKeyImpl(dp);
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(dut);
      } catch (IOException e) {
        fail(AfiUtils.UNEXPECTED, e);
      } // end Catch (...)

      final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      try (ObjectInputStream ois = new ObjectInputStream(bais)) {
        final Object obj = ois.readObject();
        assertEquals(dut, obj);
        assertEquals(dut.getPublicKey(), ((EcPrivateKeyImpl) obj).getPublicKey());
      } catch (ClassNotFoundException | IOException e) {
        fail(AfiUtils.UNEXPECTED, e);
      } // end Catch (...)
    } // end --- c.
  } // end method */

  @SuppressWarnings({"PMD.NcssCount"})
  private void checkFromAsn1() {
    // Assertions:
    // ... a. tlv2ObjectArray(BerTlv, PKCS1)-method works as expected
    // ... b. toTlv(ASN1)-method workd as expected

    // Test strategy:
    // --- a. happy cases
    // --- b. ERROR: not tag '30'
    // --- c. ERROR: missing version '02'
    // --- d. ERROR: unexpected version
    // --- e. ERROR: missing DomainParameter-Sequence '30'
    // --- f. ERROR in DomainParameter-Sequence
    // --- g. ERROR: missing octet string '04'

    final var format = EafiElcPrkFormat.ASN1;
    final var doVersion = new DerInteger(BigInteger.ZERO);
    final var doEcPublicKey = new DerOid(AfiOid.ecPublicKey);
    final var doFieldType = new DerOid(AfiOid.fieldType);

    // --- a. happy cases
    // a.1 predefined domain parameter
    // a.2 arbitrary domain parameter
    {
      // a.1 predefined domain parameter
      AfiElcParameterSpec.PREDEFINED.forEach(
          dp -> {
            final var dut = new EcPrivateKeyImpl(dp);
            final Object[] result =
                EcPrivateKeyImpl.tlv2ObjectArray(dut.getEncoded(format), format);
            assertEquals(2, result.length);
            assertEquals(dut.getS(), result[0]);
            assertEquals(dp, result[1]);
          }); // end forEach(dp -> ...)
    }

    // a.2 arbitrary domain parameter
    // p=5, a=2, b=4, G=(4,1), n=7, h=1, (G) = {(4,1), (2,4), (0,3), (0,2), (2,1), (4,4), O}
    // spotless:off
    final AfiElcParameterSpec dA = AfiElcParameterSpec.getInstance(new ECParameterSpec(
        new EllipticCurve(
            new ECFieldFp(BigInteger.valueOf(5)), // p
            BigInteger.valueOf(2),                // a
            BigInteger.valueOf(4)                 // b
        ),
        new ECPoint(
            BigInteger.valueOf(4), // Gx
            BigInteger.valueOf(1)  // Gy
        ),
        BigInteger.valueOf(7), // order n
        1 // cofactor h
    ));
    final var dutA2 = new EcPrivateKeyImpl(BigInteger.TWO, dA);
    final Object[] result = EcPrivateKeyImpl.tlv2ObjectArray(
        new DerSequence(List.of(
            doVersion,
            new DerSequence(List.of(// DO domain parameter
                doEcPublicKey,
                new DerSequence(List.of(// DO elliptic curve
                    new DerInteger(BigInteger.ONE),
                    new DerSequence(List.of(// DO-fieldType
                        doFieldType,
                        new DerInteger(dA.getP())
                    )),
                    new DerSequence(List.of(// DO coefficients
                        new DerOctetString(dA.getCurve().getA().toByteArray()),
                        new DerOctetString(dA.getCurve().getB().toByteArray())
                    )),
                    new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                    new DerInteger(dA.getOrder()),
                    new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                ))
            )),
            new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
        )),
        format
    );
    // spotless:on
    assertEquals(2, result.length);
    assertEquals(dutA2.getS(), result[0]);
    assertEquals(dA, dutA2.getParams());

    // --- b. ERROR: not tag '30'
    {
      final var input = BerTlv.getInstance("a4 00");
      final var throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EcPrivateKeyImpl.tlv2ObjectArray(input, format));
      assertEquals(EcPrivateKeyImpl.INVALID_TAG + "a4'", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- b.

    // --- c. ERROR: missing version '02'
    {
      // spotless:off
      final var input = new DerSequence(List.of(
          // doVersion,
          new DerSequence(List.of(// DO domain parameter
              doEcPublicKey,
              new DerSequence(List.of(// DO elliptic curve
                  new DerInteger(BigInteger.ONE),
                  new DerSequence(List.of(// DO-fieldType
                      doFieldType,
                      new DerInteger(dA.getP())
                  )),
                  new DerSequence(List.of(// DO coefficients
                      new DerOctetString(dA.getCurve().getA().toByteArray()),
                      new DerOctetString(dA.getCurve().getB().toByteArray())
                  )),
                  new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                  new DerInteger(dA.getOrder()),
                  new DerInteger(BigInteger.valueOf(dA.getCofactor()))
              ))
          )),
          new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
      ));
      // spotless:on
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EcPrivateKeyImpl.tlv2ObjectArray(input, format));
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    } // end --- c.

    // --- d. ERROR: unexpected version
    {
      // spotless:off
      final var input = new DerSequence(List.of(
          new DerInteger(BigInteger.TWO), // doVersion,
          new DerSequence(List.of(// DO domain parameter
              doEcPublicKey,
              new DerSequence(List.of(// DO elliptic curve
                  new DerInteger(BigInteger.ONE),
                  new DerSequence(List.of(// DO-fieldType
                      doFieldType,
                      new DerInteger(dA.getP())
                  )),
                  new DerSequence(List.of(// DO coefficients
                      new DerOctetString(dA.getCurve().getA().toByteArray()),
                      new DerOctetString(dA.getCurve().getB().toByteArray())
                  )),
                  new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                  new DerInteger(dA.getOrder()),
                  new DerInteger(BigInteger.valueOf(dA.getCofactor()))
              ))
          )),
          new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
      ));
      // spotless:on

      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EcPrivateKeyImpl.tlv2ObjectArray(input, format));
      assertEquals(EcPrivateKeyImpl.UNKNOWN_VERSION + 2, throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- d.

    // --- e. ERROR: missing DomainParameter-Sequence '30'
    {
      final var input =
          new DerSequence(
              List.of(
                  doVersion,
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())));

      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EcPrivateKeyImpl.tlv2ObjectArray(input, format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    } // end --- e.

    // --- f. ERROR in DomainParameter-Sequence
    // f.1 ERROR: OID ecPublicKey missing
    // f.2 ERROR: wrong OID for ecPublicKey
    // f.3 ERROR: DO-elliptic curve missing
    // f.4 ERROR in DO-elliptic curve

    // f.1 ERROR: OID ecPublicKey missing
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      // doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.ONE),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType,
                              new DerInteger(dA.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // f.2 ERROR: wrong OID for ecPublicKey
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      new DerOid(AfiOid.rsaEncryption), // doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.ONE),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType,
                              new DerInteger(dA.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals("expected OID = ecPublicKey, instead got rsaEncryption", throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // f.3 ERROR: DO-elliptic curve missing
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // f.4 ERROR in DO-elliptic curve
    // f.4.a ERROR: version missing, i.e. order is taken as version
    // f.4.b ERROR: unexpected version
    // f.4.c ERROR: DO-fieldType missing, i.e. DO-coefficients is taken as DO-fieldType
    // f.4.d ERROR in DO-fieldType
    // f.4.e ERROR: DO-coefficients missing
    // f.4.f ERROR in DO-coefficients
    // f.4.g ERROR: generator missing
    // f.4.h ERROR: order and cofactor expected but only one integer present
    // f.4.i ERROR: cofactor to large for integer representation

    // f.4.a ERROR: version missing, i.e. order is taken as version
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          // new DerInteger(BigInteger.ONE),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType, new DerInteger(dA.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(
          EcPrivateKeyImpl.UNKNOWN_VERSION + dA.getOrder().intValueExact(), throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // f.4.b ERROR: unexpected version
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(4711)),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType, new DerInteger(dA.getP()))),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(EcPrivateKeyImpl.UNKNOWN_VERSION + 4711, throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // f.4.c ERROR: DO-fieldType missing, i.e., DO-coefficients are taken as DO-fieldType
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(1)),
                          /*
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType,
                              new DerInteger(dpA2.getP())
                          )), // */
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // f.4.d ERROR in DO-fieldType
    // f.4.d.1 ERROR: OID missing
    // f.4.d.2 ERROR: unexpected OID
    // f.4.d.3 ERROR: integer missing

    // f.4.d.1 ERROR: OID missing
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(1)),
                          new DerSequence(List.of(// DO-fieldType
                              // doFieldType,
                              new DerInteger(dA.getP()))),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // f.4.d.2 ERROR: unexpected OID
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(1)),
                          new DerSequence(List.of(// DO-fieldType
                              new DerOid(AfiOid.ecPublicKey), // doFieldType,
                              new DerInteger(dA.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals("expected OID = fieldType, instead got ecPublicKey", throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // f.4.d.3 ERROR: integer missing
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(1)),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType
                              // new DerInteger(dpA2.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // f.4.e ERROR: DO-coefficients missing
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(1)),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType, new DerInteger(dA.getP())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }
    // f.4.f ERROR in DO-coefficients, too few coefficients
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(1)),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType, new DerInteger(dA.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              // new DerOctetString(dpA2.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }
    // f.4.g ERROR: generator missing
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(1)),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType, new DerInteger(dA.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          // new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(),dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // f.4.h ERROR: order and cofactor expected but only one integer present
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(1)),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType, new DerInteger(dA.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          // new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // f.4.i ERROR: cofactor to large for integer representation
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          ArithmeticException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.valueOf(1)),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType, new DerInteger(dA.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(Integer.MAX_VALUE + 1L))
                      ))
                  )),
                  new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded())
              )),
              format
          )
      );
      // spotless:on
      assertEquals("BigInteger out of int range", throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // --- g. ERROR: missing octet string '04'
    {
      // spotless:off
      final Throwable throwable = assertThrows(
          IllegalArgumentException.class,
          () -> EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(
                  doVersion,
                  new DerSequence(List.of(// DO domain parameter
                      doEcPublicKey,
                      new DerSequence(List.of(// DO elliptic curve
                          new DerInteger(BigInteger.ONE),
                          new DerSequence(List.of(// DO-fieldType
                              doFieldType, new DerInteger(dA.getP())
                          )),
                          new DerSequence(List.of(// DO coefficients
                              new DerOctetString(dA.getCurve().getA().toByteArray()),
                              new DerOctetString(dA.getCurve().getB().toByteArray())
                          )),
                          new DerOctetString(AfiElcUtils.p2osUncompressed(dA.getGenerator(), dA)),
                          new DerInteger(dA.getOrder()),
                          new DerInteger(BigInteger.valueOf(dA.getCofactor()))
                      ))
                  ))
              )),
              format
          )
      );
      // spotless:on
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    } // end --- g.
  } // end method */

  private void checkFromIsoIec7816() {
    final var format = EafiElcPrkFormat.ISOIEC7816;

    // Test strategy:
    // --- a. happy cases
    // --- b. ERROR: not tag '7f48'
    // --- c. ERROR: missing OID '06'
    // --- d. ERROR: OID does not point to domain parameters
    // --- e. ERROR: missing integer '02'

    // --- a. happy cases
    AfiElcParameterSpec.PREDEFINED.forEach(
        dp -> {
          final var d = BigInteger.valueOf(RNG.nextInt());
          final Object[] result =
              EcPrivateKeyImpl.tlv2ObjectArray(
                  BerTlv.getInstance(0x7f48, List.of(new DerOid(dp.getOid()), new DerInteger(d))),
                  format);

          assertEquals(2, result.length);
          assertEquals(d, result[0]);
          assertEquals(dp, result[1]);
        }); // end forEach(dp -> ...)

    // --- b. ERROR: not tag '7f48'
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EcPrivateKeyImpl.tlv2ObjectArray(BerTlv.getInstance("a1 00"), format));
      assertEquals(EcPrivateKeyImpl.INVALID_TAG + "a1'", throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // --- c. ERROR: missing OID '06'
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      BerTlv.getInstance(0x7f48, List.of(new DerInteger(BigInteger.ONE))), format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }
    // --- d. ERROR: OID does not point to domain parameters
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      BerTlv.getInstance(
                          0x7f48,
                          List.of(new DerOid(AfiOid.ecPublicKey), new DerInteger(BigInteger.ONE))),
                      format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
      assertEquals("No value present", throwable.getCause().getMessage());
    }

    // --- e. ERROR: missing integer '02'
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      BerTlv.getInstance(0x7f48, List.of(new DerOid(AfiOid.brainpoolP384r1))),
                      format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
      assertEquals("No value present", throwable.getCause().getMessage());
    }
  } // end method */

  private void checkFromPkcs1() {
    final var format = EafiElcPrkFormat.PKCS1;

    // Test strategy:
    // --- a. happy cases
    // --- b. ERROR: not tag '30'
    // --- c. ERROR: missing version '02'
    // --- d. ERROR: unexpected version
    // --- e. ERROR: missing octet string '04'

    final var version = new DerInteger(BigInteger.ONE);

    // --- a. happy cases
    {
      // Note: Here all one byte values with and without leading '00' bytes are checked
      IntStream.rangeClosed(1, 3)
          .forEach(
              length -> {
                IntStream.range(0, 256)
                    .forEach(
                        value -> {
                          final var d = BigInteger.valueOf(value);

                          final Object[] result =
                              EcPrivateKeyImpl.tlv2ObjectArray(
                                  new DerSequence(
                                      List.of(
                                          version,
                                          new DerOctetString(AfiBigInteger.i2os(d, length)))),
                                  format);
                          assertEquals(1, result.length);
                          assertEquals(BigInteger.class, result[0].getClass());
                          assertEquals(d, result[0]);
                        }); // end forEach(value -> ...)
              }); // end forEach(length -> ...)

      assertEquals(
          new BigInteger("01a1", 16),
          EcPrivateKeyImpl.tlv2ObjectArray(
              new DerSequence(List.of(version, new DerOctetString(Hex.toByteArray("01a1")))),
              format)[0]);
    }

    // --- b. ERROR: not tag '30'
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EcPrivateKeyImpl.tlv2ObjectArray(BerTlv.getInstance("a0 00"), format));
      assertEquals(EcPrivateKeyImpl.INVALID_TAG + "a0'", throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // --- c. ERROR: missing version '02'
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      new DerSequence(List.of(new DerOctetString(Hex.toByteArray("01c1")))),
                      format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // --- d. ERROR: unexpected version
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      new DerSequence(
                          List.of(
                              new DerInteger(BigInteger.ZERO),
                              new DerOctetString(Hex.toByteArray("01d1")))),
                      format));
      assertEquals(EcPrivateKeyImpl.UNKNOWN_VERSION + 0, throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // --- e. ERROR: missing octet string '04'
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EcPrivateKeyImpl.tlv2ObjectArray(new DerSequence(List.of(version)), format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }
  } // end method */

  private void checkFromPkcs8() {
    // Assertions:
    // ... a. tlv2ObjectArray(BerTlv, PKCS1)-method works as expected
    // ... b. getEncoded(PKCS8)-method works as expected

    // Test strategy:
    // --- a. happy cases
    // --- b. ERROR: not tag '30'
    // --- c. ERROR: missing version '02'
    // --- d. ERROR: unexpected version
    // --- e. ERROR: missing OID-sequence
    // --- f. ERROR: first OID != ecPublicKey
    // --- g. ERROR: second OID missing
    // --- h. ERROR: second OID does not reference known domain parameter
    // --- e. ERROR: missing octet string '04'

    final var format = EafiElcPrkFormat.PKCS8;
    final var doVersion = new DerInteger(BigInteger.ZERO);
    final var doEcPublicKey = new DerOid(AfiOid.ecPublicKey);

    // --- a. happy cases
    // a.1 predefined domain parameter
    // a.2 smoke test
    {
      // a.1 predefined domain parameter
      AfiElcParameterSpec.PREDEFINED.forEach(
          dp -> {
            final var dut = new EcPrivateKeyImpl(dp);
            final Object[] result =
                EcPrivateKeyImpl.tlv2ObjectArray(dut.getEncoded(format), format);
            assertEquals(2, result.length);
            assertEquals(dut.getS(), result[0]);
            assertEquals(dp, result[1]);
          }); // end forEach(dp -> ...)
    }

    // a.2 smoke test
    final var dutA2 =
        new EcPrivateKeyImpl(BigInteger.valueOf(4711), AfiElcParameterSpec.ansix9p521r1);
    final Object[] result =
        EcPrivateKeyImpl.tlv2ObjectArray(
            new DerSequence(
                List.of(
                    doVersion,
                    new DerSequence(List.of(doEcPublicKey, new DerOid(dutA2.getParams().getOid()))),
                    new DerOctetString(dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded()))),
            format);
    assertEquals(2, result.length);
    assertEquals(dutA2.getS(), result[0]);
    assertEquals(AfiElcParameterSpec.ansix9p521r1, dutA2.getParams());

    // --- b. ERROR: not tag '30'
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> EcPrivateKeyImpl.tlv2ObjectArray(BerTlv.getInstance("a2 00"), format));
      assertEquals(EcPrivateKeyImpl.INVALID_TAG + "a2'", throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // --- c. ERROR: missing version '02'
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      new DerSequence(
                          List.of(
                              new DerSequence(
                                  List.of(doEcPublicKey, new DerOid(dutA2.getParams().getOid()))),
                              new DerOctetString(
                                  dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded()))),
                      format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // --- d. ERROR: unexpected version
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      new DerSequence(
                          List.of(
                              new DerInteger(BigInteger.ONE),
                              new DerSequence(
                                  List.of(doEcPublicKey, new DerOid(dutA2.getParams().getOid()))),
                              new DerOctetString(
                                  dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded()))),
                      format));
      assertEquals(EcPrivateKeyImpl.UNKNOWN_VERSION + 1, throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // --- e. ERROR: missing OID-sequence
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      new DerSequence(
                          List.of(
                              doVersion,
                              new DerOctetString(
                                  dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded()))),
                      format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // --- f. ERROR: first OID != ecPublicKey
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      new DerSequence(
                          List.of(
                              doVersion,
                              new DerSequence(
                                  List.of(
                                      new DerOid(AfiOid.ansix9p521r1),
                                      new DerOid(dutA2.getParams().getOid()))),
                              new DerOctetString(
                                  dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded()))),
                      format));
      assertEquals("expected OID = ecPublicKey, instead got ansix9p521r1", throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // --- g. ERROR: second OID missing
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      new DerSequence(
                          List.of(
                              doVersion,
                              new DerSequence(List.of(doEcPublicKey)),
                              new DerOctetString(
                                  dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded()))),
                      format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // --- h. ERROR: second OID does not reference known domain parameter
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      new DerSequence(
                          List.of(
                              doVersion,
                              new DerSequence(
                                  List.of(doEcPublicKey, new DerOid(AfiOid.rsaEncryption))),
                              new DerOctetString(
                                  dutA2.getEncoded(EafiElcPrkFormat.PKCS1).getEncoded()))),
                      format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }

    // --- e. ERROR: missing octet string '04'
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class,
              () ->
                  EcPrivateKeyImpl.tlv2ObjectArray(
                      new DerSequence(
                          List.of(
                              doVersion,
                              new DerSequence(
                                  List.of(doEcPublicKey, new DerOid(dutA2.getParams().getOid()))))),
                      format));
      assertEquals(ERROR, throwable.getMessage());
      assertEquals(NoSuchElementException.class, throwable.getCause().getClass());
    }
  } // end method */
} // end class
