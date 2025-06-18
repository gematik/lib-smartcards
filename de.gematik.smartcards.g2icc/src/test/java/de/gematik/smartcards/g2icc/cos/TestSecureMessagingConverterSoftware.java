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
package de.gematik.smartcards.g2icc.cos;

import static de.gematik.smartcards.utils.AfiUtils.KIBI;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.crypto.AesKey;
import de.gematik.smartcards.crypto.AfiElcUtils;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.g2icc.GsmcKt80276883110000107637;
import de.gematik.smartcards.g2icc.Hba80276883110000220885;
import de.gematik.smartcards.g2icc.PocPopp26;
import de.gematik.smartcards.g2icc.cvc.Cvc;
import de.gematik.smartcards.g2icc.cvc.TrustCenter;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Class performing white-box tests for {@link SecureMessagingConverterSoftware}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@EnabledIfSystemProperty(named = "user.name", matches = "alfred")
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
@TestMethodOrder(MethodOrderer.MethodName.class)
final class TestSecureMessagingConverterSoftware {

  /** Random Number Generator. */
  /* package */ static final AfiRng RNG = new AfiRng(); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. set the path of trust-center to something useful
    {
      assertDoesNotThrow(TrustCenter::initialize);
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

  /**
   * Test method for constructor and getter.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link SecureMessagingConverterSoftware#SecureMessagingConverterSoftware(Cvc, Cvc,
   *       java.security.interfaces.ECPrivateKey) }
   *   <li>{@link SecureMessagingConverterSoftware#getPrk()}
   *   <li>{@link SecureMessagingConverterSoftware#getEndEntityCvc()}
   *   <li>{@link SecureMessagingConverterSoftware#getSubCaCvc()}
   * </ol>
   */
  @SuppressWarnings({"PMD.NcssCount"})
  @Test
  void test_SmTransformerSoftware__Cvc_Cvc_EcPrivateKey() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: invalid signature for Sub-CA-CVC
    // --- c. ERROR: invalid signature for End-Entity-CVC
    // --- d. ERROR: not a Sub-CA-CVC
    // --- e. ERROR: not an End-Entity-CVC
    // --- f. ERROR: wrong Sub-CA-CVC in the chain
    // --- g. ERROR: wrong PrK in the chain

    // --- a. smoke test
    // a.1 identity from a gSMC-KT
    // a.2 identity from PoC PoPP-26

    // a.1 identity from a gSMC-KT
    {
      final var subCaCvc = GsmcKt80276883110000107637.CVC_CA_E256;
      final var eeCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
      final var prk = GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256;
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, subCaCvc.getSignatureStatus());
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, eeCvc.getSignatureStatus());
      assertEquals(eeCvc.getPublicKey(), prk.getPublicKey());

      final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);

      assertEquals(prk, dut.getPrk());
      assertSame(eeCvc, dut.getEndEntityCvc());
      assertSame(subCaCvc, dut.getSubCaCvc());
      assertThrows(NoSuchElementException.class, dut::getEndEntityOpponent);
      assertFalse(dut.isFlagSessionEnabled());
    } // end a.1

    // a.2 identity from PoC PoPP-26
    {
      final var subCaCvc = PocPopp26.CVC_CA_E256;
      final var eeCvc = PocPopp26.CVC_EE_E256;
      final var prk = PocPopp26.PRK_E256;
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, subCaCvc.getSignatureStatus());
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, eeCvc.getSignatureStatus());
      assertEquals(eeCvc.getPublicKey(), prk.getPublicKey());

      final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);

      assertEquals(prk, dut.getPrk());
      assertSame(eeCvc, dut.getEndEntityCvc());
      assertSame(subCaCvc, dut.getSubCaCvc());
      assertThrows(NoSuchElementException.class, dut::getEndEntityOpponent);
      assertFalse(dut.isFlagSessionEnabled());
    } // end a.1
    // end --- a.

    // --- b. ERROR: invalid signature for Sub-CA-CVC
    {
      final var subCaCvc = Hba80276883110000220885.CVC_CA_E256;
      final var eeCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
      final var prk = GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256;
      assertEquals(Cvc.SignatureStatus.SIGNATURE_NO_PUBLIC_KEY, subCaCvc.getSignatureStatus());
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, eeCvc.getSignatureStatus());
      assertEquals(eeCvc.getPublicKey(), prk.getPublicKey());

      final var throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk));

      assertEquals("invalid subCaCvc", throwable.getMessage());
    } // end --- b.

    // --- c. ERROR: invalid signature for End-Entity-CVC
    {
      final var subCaCvc = GsmcKt80276883110000107637.CVC_CA_E256;
      final var eeInput = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256.getCvc().getEncoded();
      eeInput[eeInput.length - 1]++; // invalidate signature
      final var eeCvc = new Cvc(eeInput);
      final var prk = GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256;
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, subCaCvc.getSignatureStatus());
      assertEquals(Cvc.SignatureStatus.SIGNATURE_INVALID, eeCvc.getSignatureStatus());
      assertEquals(eeCvc.getPublicKey(), prk.getPublicKey());

      final var throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk));

      assertEquals("invalid endEntityCvc", throwable.getMessage());
    } // end --- c.

    // --- d. ERROR: not a Sub-CA-CVC
    {
      final var eeCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
      final var prk = GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256;
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, eeCvc.getSignatureStatus());
      assertEquals(eeCvc.getPublicKey(), prk.getPublicKey());

      final var throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> new SecureMessagingConverterSoftware(eeCvc, eeCvc, prk));

      assertEquals("not a Sub-CA-CVC", throwable.getMessage());
    } // end --- d.

    // --- e. ERROR: not an End-Entity-CVC
    {
      final var subCaCvc = GsmcKt80276883110000107637.CVC_CA_E256;
      final var prk = GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256;
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, subCaCvc.getSignatureStatus());

      final var throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> new SecureMessagingConverterSoftware(subCaCvc, subCaCvc, prk));

      assertEquals("not an End-Entity-CVC", throwable.getMessage());
    } // end --- e.

    // --- f. ERROR: wrong Sub-CA-CVC in the chain
    {
      final var subCaCvc = PocPopp26.CVC_CA_E256;
      final var eeCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
      final var prk = GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256;
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, subCaCvc.getSignatureStatus());
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, eeCvc.getSignatureStatus());
      assertEquals(eeCvc.getPublicKey(), prk.getPublicKey());

      final var throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk));

      assertEquals("not a chain", throwable.getMessage());
    } // end --- f.

    // --- g. ERROR: wrong PrK in the chain
    {
      final var subCaCvc = GsmcKt80276883110000107637.CVC_CA_E256;
      final var eeCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
      final var prk = PocPopp26.PRK_E256;
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, subCaCvc.getSignatureStatus());
      assertEquals(Cvc.SignatureStatus.SIGNATURE_VALID, eeCvc.getSignatureStatus());

      final var throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk));

      assertEquals("not a chain", throwable.getMessage());
    } // end --- g.
  } // end method */

  /** Test method for {@link SecureMessagingConverterSoftware#getGeneralAuthenticateStep1()}. */
  @Test
  void test_getGeneralAuthenticateStep1() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    {
      final var subCaCvc = PocPopp26.CVC_CA_E256;
      final var eeCvc = PocPopp26.CVC_EE_E256;
      final var prk = PocPopp26.PRK_E256;
      final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);
      final var expected =
          new CommandApdu(
              String.format(
                  "10 86 0000   10   7c-0e-(c3-0c-%s)   00", dut.getEndEntityCvc().getChr()));

      final var actual = dut.getGeneralAuthenticateStep1();

      assertEquals(expected, actual);
    } // end --- a.
  } // end method */

  /**
   * Test method for {@link
   * SecureMessagingConverterSoftware#getGeneralAuthenticateStep2(ResponseApdu)}.
   */
  @Test
  void test_getGeneralAuthenticateStep2__ResponseApdu() {
    // Assertions:
    // ... a. importCvc(Cvc)-method works as expected

    // Test strategy:
    // --- a. ERROR: no CV-certificate imported
    // --- b. smoke test
    // --- c. ERROR: SW != '9000'
    // --- d. ERROR in BER-TLV structure
    // --- e. ERROR: point not on the elliptic curve
    // --- f. ERROR: unsupported elliptic curve

    final var subCaCvc = PocPopp26.CVC_CA_E256;
    final var eeCvc = PocPopp26.CVC_EE_E256;
    final var prk = PocPopp26.PRK_E256;
    final var dp = prk.getParams();
    final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);

    // --- a. ERROR: no CV-certificate imported
    {
      final var throwable =
          assertThrows(
              NoSuchElementException.class,
              () ->
                  dut.getGeneralAuthenticateStep2(
                      new ResponseApdu(
                          BerTlv.getInstance(
                                  0x7c,
                                  BerTlv.getInstance(
                                          0x85,
                                          AfiElcUtils.p2osUncompressed(
                                              eeCvc.getPublicKey().getW(), dp))
                                      .getEncoded())
                              .getEncoded(),
                          0x9000)));

      assertEquals("no opponent", throwable.getMessage());
    } // end --- a.

    // --- b. smoke test
    {
      final var opCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
      final var opPrk = GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256;
      assertDoesNotThrow(() -> dut.importCvc(opCvc));
      final var opEphemeralPrk = new EcPrivateKeyImpl(dp);
      final var opRsp =
          new ResponseApdu(
              BerTlv.getInstance(
                      0x7c,
                      BerTlv.getInstance(
                              0x85,
                              AfiElcUtils.p2osUncompressed(
                                  opEphemeralPrk.getPublicKey().getW(), dp))
                          .getEncoded())
                  .getEncoded(),
              0x9000);

      final var dutCmd = dut.getGeneralAuthenticateStep2(opRsp);

      assertTrue(dut.isFlagSessionEnabled());
      final var dutCmdDataFieldTlv = BerTlv.getInstance(dutCmd.getData());
      assertEquals(0x7c, dutCmdDataFieldTlv.getTag());
      final var dutEphemeralPuk =
          new EcPublicKeyImpl(
              AfiElcUtils.os2p(
                  ((ConstructedBerTlv) dutCmdDataFieldTlv)
                      .getPrimitive(0x85)
                      .orElseThrow()
                      .getValueField(),
                  dp),
              dp);
      final var k1 = AfiElcUtils.sharedSecret(opPrk, dutEphemeralPuk); // (N085.054)c.1
      final var k2 = // (N085.054)c.1
          AfiElcUtils.sharedSecret(opEphemeralPrk, dut.getEndEntityCvc().getPublicKey());
      final var kd = AfiUtils.concatenate(k1, k2); // (N085.054)c.3
      final var kdEnc = AfiUtils.concatenate(kd, Hex.toByteArray("0000 0001"));
      final var kEnc = new AesKey(EafiHashAlgorithm.SHA_1.digest(kdEnc), 0, 16);
      final var sscCmd = BigInteger.ONE;
      final var sscRsp = BigInteger.ZERO;
      assertEquals(AfiBigInteger.toHex(sscCmd, 16), Hex.toHexDigits(dut.getSscMacCmd()));
      assertEquals(AfiBigInteger.toHex(sscRsp, 16), Hex.toHexDigits(dut.getSscMacRsp()));

      // check Session Key material
      final var plain = RNG.nextBytes(33, 47);
      final var cipherIn = kEnc.padIso(plain);
      final var icv = kEnc.encipherCbc(AfiBigInteger.i2os(sscCmd, 16));
      final var expected = "01" + Hex.toHexDigits(kEnc.encipherCbc(cipherIn, icv));
      final var actual = Hex.toHexDigits(dut.encipher(plain));
      assertEquals(expected, actual);
    } // end --- b.

    // --- c. ERROR: SW != '9000'
    {
      final var throwable =
          assertThrows(
              IllegalArgumentException.class,
              () -> dut.getGeneralAuthenticateStep2(new ResponseApdu("6400")));

      assertEquals("not NoError", throwable.getMessage());
    } // end --- c.

    // --- d. ERROR in BER-TLV structure
    List.of(
            "7a-00", // wrong outer tag
            "7c-02  84-00", // wrong inner tag
            "abcdef" // not a BER-TLV structure
            )
        .forEach(
            input ->
                assertThrows(
                    IllegalArgumentException.class,
                    () ->
                        dut.getGeneralAuthenticateStep2(
                            new ResponseApdu(input + "9000")))); // end forEach(input -> ...)
    // end --- d.

    // --- e. ERROR: point not on the elliptic curve
    {
      final var opEphemeralPrk = new EcPrivateKeyImpl(dp);
      final var ephemeralPuk =
          AfiElcUtils.p2osUncompressed(opEphemeralPrk.getPublicKey().getW(), dp);
      ephemeralPuk[ephemeralPuk.length - 1]++;
      final var opRsp =
          new ResponseApdu(
              BerTlv.getInstance(0x7c, BerTlv.getInstance(0x85, ephemeralPuk).getEncoded())
                  .getEncoded(),
              0x9000);

      final var throwable =
          assertThrows(
              IllegalArgumentException.class, () -> dut.getGeneralAuthenticateStep2(opRsp));

      assertEquals("P is not a point on the elliptic curve", throwable.getMessage());
    } // end --- e.

    // --- f. ERROR: unsupported elliptic curve
    // TODO intentionally no test here for now
    //      as along as the constructors support brainpoolP256r1 only.
  } // end method */

  /** Test method for {@link SecureMessagingConverterSoftware#importCvc(Cvc)}. */
  @Test
  void test_importCvc__Cvc() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: invalid signature in CV-certificate
    // --- c. ERROR: not an End-Entity-certificate

    {
      final var subCaCvc = PocPopp26.CVC_CA_E256;
      final var eeCvc = PocPopp26.CVC_EE_E256;
      final var prk = PocPopp26.PRK_E256;
      final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);

      // --- a. smoke test
      {
        final var input = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
        final var parents = TrustCenter.getParent(input);
        assertFalse(parents.isEmpty());
        final var expRootCar = parents.iterator().next().getCar();

        final var chain = dut.importCvc(input);

        var previous = chain.getFirst();
        assertEquals(dut.getEndEntityCvc(), previous);
        for (int i = 1; i < chain.size(); i++) {
          final var actual = chain.get(i);
          assertEquals(previous.getCar(), actual.getChr());
          previous = actual;
        } // end For (i...)
        assertEquals(expRootCar, previous.getCar());
      } // end --- a.

      // --- b. ERROR: invalid signature in CV-certificate
      {
        final var throwable =
            assertThrows(
                IllegalArgumentException.class,
                () -> dut.importCvc(Hba80276883110000220885.CVC_HPC_AUTR_CVC_E256));
        assertEquals("signature not valid", throwable.getMessage());
      } // end --- b.

      // --- c. ERROR: not an End-Entity-certificate
      {
        final var throwable =
            assertThrows(
                IllegalArgumentException.class,
                () -> dut.importCvc(GsmcKt80276883110000107637.CVC_CA_E256));
        assertEquals("not an End-Entity-CVC", throwable.getMessage());
      } // end ---c.
    } // end --- a, b, c.
  } // end method */

  /**
   * Test method for {@link SecureMessagingConverterSoftware#computeCryptographicChecksum(byte[])}.
   */
  @Test
  void test_computeCryptographicChecksum__byteA() {
    // Assertions:
    // ... a. getGeneralAuthenticateStep2(ResponseApdu)-method works as expected

    // Test strategy:
    // --- a. ERROR: session not enabled
    // --- b. smoke test

    final var subCaCvc = PocPopp26.CVC_CA_E256;
    final var eeCvc = PocPopp26.CVC_EE_E256;
    final var prk = PocPopp26.PRK_E256;
    final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);
    final var opCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
    final var opPrk = GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256;
    assertDoesNotThrow(() -> dut.importCvc(opCvc));
    final var dp = prk.getParams();
    final var opEphemeralPrk = new EcPrivateKeyImpl(dp);
    final var opRsp =
        new ResponseApdu(
            BerTlv.getInstance(
                    0x7c,
                    BerTlv.getInstance(
                            0x85,
                            AfiElcUtils.p2osUncompressed(opEphemeralPrk.getPublicKey().getW(), dp))
                        .getEncoded())
                .getEncoded(),
            0x9000);

    // --- a. ERROR: session not enabled
    {
      assertFalse(dut.isFlagSessionEnabled());
      assertThrows(
          RejectedExecutionException.class,
          () -> dut.computeCryptographicChecksum(AfiUtils.EMPTY_OS));
    } // end --- a.

    final var dutCmd = dut.getGeneralAuthenticateStep2(opRsp);
    final var dutCmdDataFieldTlv = BerTlv.getInstance(dutCmd.getData());
    assertEquals(0x7c, dutCmdDataFieldTlv.getTag());
    final var dutEphemeralPuk =
        new EcPublicKeyImpl(
            AfiElcUtils.os2p(
                ((ConstructedBerTlv) dutCmdDataFieldTlv)
                    .getPrimitive(0x85)
                    .orElseThrow()
                    .getValueField(),
                dp),
            dp);
    final var k1 = AfiElcUtils.sharedSecret(opPrk, dutEphemeralPuk); // (N085.054)c.1
    final var k2 = // (N085.054)c.1
        AfiElcUtils.sharedSecret(opEphemeralPrk, dut.getEndEntityCvc().getPublicKey());
    final var kd = AfiUtils.concatenate(k1, k2); // (N085.054)c.3
    final var kdMac = AfiUtils.concatenate(kd, Hex.toByteArray("0000 0002"));
    final var kMac = new AesKey(EafiHashAlgorithm.SHA_1.digest(kdMac), 0, 16);
    var sscCmd = BigInteger.ONE;

    // --- b. smoke test
    for (final var length : RNG.intsClosed(0, KIBI, 10).toArray()) {
      assertArrayEquals(AfiBigInteger.i2os(sscCmd, 16), dut.getSscMacCmd());
      final var plain = RNG.nextBytes(length);
      final var expected =
          kMac.calculateCmac(
              AfiUtils.concatenate(AfiBigInteger.i2os(sscCmd, 16), kMac.padIso(plain)), 8);

      final var actual = dut.computeCryptographicChecksum(plain);

      assertArrayEquals(expected, actual);
      sscCmd = sscCmd.add(BigInteger.TWO);
    } // end For (length...)
    // end --- b.
  } // end method */

  /** Test method for {@link SecureMessagingConverterSoftware#decipher(byte[])}. */
  @Test
  void test_decipher__byteA() {
    // Assertions:
    // ... a. getGeneralAuthenticateStep2(ResponseApdu)-method works as expected
    // ... b. encipher(byte[])-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: wrong padding indicator
    // --- c. ERROR: wrong padding
    // --- d. ERROR: session not enabled

    final var subCaCvc = PocPopp26.CVC_CA_E256;
    final var eeCvc = PocPopp26.CVC_EE_E256;
    final var prk = PocPopp26.PRK_E256;
    final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);
    final var opCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
    assertDoesNotThrow(() -> dut.importCvc(opCvc));
    final var dp = prk.getParams();
    final var opEphemeralPrk = new EcPrivateKeyImpl(dp);
    final var opRsp =
        new ResponseApdu(
            BerTlv.getInstance(
                    0x7c,
                    BerTlv.getInstance(
                            0x85,
                            AfiElcUtils.p2osUncompressed(opEphemeralPrk.getPublicKey().getW(), dp))
                        .getEncoded())
                .getEncoded(),
            0x9000);

    // --- a. smoke test
    {
      assertDoesNotThrow(() -> dut.getGeneralAuthenticateStep2(opRsp));
      AfiUtils.incrementCounter(dut.getSscMacRsp());
      assertArrayEquals(dut.getSscMacCmd(), dut.getSscMacRsp());
      for (final var length : RNG.intsClosed(0, KIBI, 10).toArray()) {
        final var expected = RNG.nextBytes(length);
        final var cipher = dut.encipher(expected);

        final var actual = dut.decipher(cipher);

        assertArrayEquals(expected, actual);
      } // end For (length...)

      assertTrue(dut.isFlagSessionEnabled());
    } // end --- a.

    // --- b. ERROR: wrong padding indicator
    {
      assertDoesNotThrow(() -> dut.getGeneralAuthenticateStep2(opRsp));
      AfiUtils.incrementCounter(dut.getSscMacRsp());
      assertArrayEquals(dut.getSscMacCmd(), dut.getSscMacRsp());
      assertTrue(dut.isFlagSessionEnabled());
      final var plain = RNG.nextBytes(16, 64);
      final var cipher = dut.encipher(plain);
      cipher[0]++;

      assertThrows(IllegalArgumentException.class, () -> dut.decipher(cipher));
      assertFalse(dut.isFlagSessionEnabled());
    } // end --- b.

    // --- c. ERROR: wrong padding
    {
      assertDoesNotThrow(() -> dut.getGeneralAuthenticateStep2(opRsp));
      AfiUtils.incrementCounter(dut.getSscMacRsp());
      assertArrayEquals(dut.getSscMacCmd(), dut.getSscMacRsp());
      assertTrue(dut.isFlagSessionEnabled());
      final var plain = RNG.nextBytes(16, 64);
      final var cipher = dut.encipher(plain);
      cipher[cipher.length - 1]++;

      assertThrows(IllegalArgumentException.class, () -> dut.decipher(cipher));
    } // end --- c.

    // --- d. ERROR: session not enabled
    {
      assertFalse(dut.isFlagSessionEnabled());
      assertThrows(
          RejectedExecutionException.class,
          () -> dut.decipher(Hex.toByteArray("01000102030405060708090a0b0c0d0e0f")));
    } // end --- d.
  } // end method */

  /** Test method for {@link SecureMessagingConverterSoftware#encipher(byte[])}. */
  @Test
  void test_encipher__byteA() {
    // Assertions:
    // ... a. getGeneralAuthenticateStep2(ResponseApdu)-method works as expected

    // Test strategy:
    // --- a. ERROR: session not enabled
    // --- b. smoke test

    final var subCaCvc = PocPopp26.CVC_CA_E256;
    final var eeCvc = PocPopp26.CVC_EE_E256;
    final var prk = PocPopp26.PRK_E256;
    final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);
    final var opCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
    final var opPrk = GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256;
    assertDoesNotThrow(() -> dut.importCvc(opCvc));
    final var dp = prk.getParams();
    final var opEphemeralPrk = new EcPrivateKeyImpl(dp);
    final var opRsp =
        new ResponseApdu(
            BerTlv.getInstance(
                    0x7c,
                    BerTlv.getInstance(
                            0x85,
                            AfiElcUtils.p2osUncompressed(opEphemeralPrk.getPublicKey().getW(), dp))
                        .getEncoded())
                .getEncoded(),
            0x9000);

    // --- a. ERROR: session not enabled
    {
      assertFalse(dut.isFlagSessionEnabled());
      assertThrows(RejectedExecutionException.class, () -> dut.encipher(AfiUtils.EMPTY_OS));
    } // end --- a.

    final var dutCmd = dut.getGeneralAuthenticateStep2(opRsp);
    final var dutCmdDataFieldTlv = BerTlv.getInstance(dutCmd.getData());
    assertEquals(0x7c, dutCmdDataFieldTlv.getTag());
    final var dutEphemeralPuk =
        new EcPublicKeyImpl(
            AfiElcUtils.os2p(
                ((ConstructedBerTlv) dutCmdDataFieldTlv)
                    .getPrimitive(0x85)
                    .orElseThrow()
                    .getValueField(),
                dp),
            dp);
    final var k1 = AfiElcUtils.sharedSecret(opPrk, dutEphemeralPuk); // (N085.054)c.1
    final var k2 = // (N085.054)c.1
        AfiElcUtils.sharedSecret(opEphemeralPrk, dut.getEndEntityCvc().getPublicKey());
    final var kd = AfiUtils.concatenate(k1, k2); // (N085.054)c.3
    final var kdEnc = AfiUtils.concatenate(kd, Hex.toByteArray("0000 0001"));
    final var kEnc = new AesKey(EafiHashAlgorithm.SHA_1.digest(kdEnc), 0, 16);
    final var sscCmd = BigInteger.ONE;

    // --- b. smoke test
    for (final var length : RNG.intsClosed(0, KIBI, 10).toArray()) {
      final var plain = RNG.nextBytes(length);
      final var cipherIn = kEnc.padIso(plain);
      final var icv = kEnc.encipherCbc(AfiBigInteger.i2os(sscCmd, 16));
      final var expected = "01" + Hex.toHexDigits(kEnc.encipherCbc(cipherIn, icv));

      final var actual = Hex.toHexDigits(dut.encipher(plain));

      assertEquals(expected, actual);
    } // end For (length...)
    // end --- b.
  } // end method */

  /**
   * Test method for {@link SecureMessagingConverterSoftware#verifyCryptographicChecksum(byte[],
   * byte[])}.
   */
  @Test
  void test_verifyCryptographicChecksum__byteA_byteA() {
    // Assertions:
    // ... a. getGeneralAuthenticateStep2(ResponseApdu)-method works as expected
    // ... b. computeCryptographicChecksum(byte[])-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: wrong MAC
    // --- c. ERROR: session not enabled

    final var subCaCvc = PocPopp26.CVC_CA_E256;
    final var eeCvc = PocPopp26.CVC_EE_E256;
    final var prk = PocPopp26.PRK_E256;
    final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);
    final var opCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
    assertDoesNotThrow(() -> dut.importCvc(opCvc));
    final var dp = prk.getParams();
    final var opEphemeralPrk = new EcPrivateKeyImpl(dp);
    final var opRsp =
        new ResponseApdu(
            BerTlv.getInstance(
                    0x7c,
                    BerTlv.getInstance(
                            0x85,
                            AfiElcUtils.p2osUncompressed(opEphemeralPrk.getPublicKey().getW(), dp))
                        .getEncoded())
                .getEncoded(),
            0x9000);

    // --- a. smoke test
    {
      assertDoesNotThrow(() -> dut.getGeneralAuthenticateStep2(opRsp));
      AfiUtils.incrementCounter(dut.getSscMacCmd());
      for (final var length : RNG.intsClosed(0, KIBI, 10).toArray()) {
        final var plain = RNG.nextBytes(length);
        final var mac = dut.computeCryptographicChecksum(plain);

        final var actual = dut.verifyCryptographicChecksum(plain, mac);

        assertTrue(actual);
      } // end For (length...)

      assertTrue(dut.isFlagSessionEnabled());
    } // end --- a.

    // --- b. ERROR: wrong MAC
    {
      final var plain = RNG.nextBytes(16, 32);
      final var mac = dut.computeCryptographicChecksum(plain);
      mac[0]++;

      final var actual = dut.verifyCryptographicChecksum(plain, mac);

      assertFalse(actual);
    } // end --- b.

    // --- c. ERROR: session not enabled
    {
      assertFalse(dut.isFlagSessionEnabled());
      assertThrows(
          RejectedExecutionException.class,
          () -> dut.verifyCryptographicChecksum(AfiUtils.EMPTY_OS, AfiUtils.EMPTY_OS));
    } // end --- c.
  } // end method */

  /** Test method for {@link SecureMessagingConverterSoftware#padIso(byte[])}. */
  @Test
  void test_padIso__byteA() {
    // Assertions:
    // - none -

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. ERROR: session not enabled
    // --- b. establish a trusted channel
    // --- c. smoke test

    final var subCaCvc = PocPopp26.CVC_CA_E256;
    final var eeCvc = PocPopp26.CVC_EE_E256;
    final var prk = PocPopp26.PRK_E256;
    final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);
    final var opCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
    assertDoesNotThrow(() -> dut.importCvc(opCvc));
    final var dp = prk.getParams();
    final var opEphemeralPrk = new EcPrivateKeyImpl(dp);
    final var opRsp =
        new ResponseApdu(
            BerTlv.getInstance(
                    0x7c,
                    BerTlv.getInstance(
                            0x85,
                            AfiElcUtils.p2osUncompressed(opEphemeralPrk.getPublicKey().getW(), dp))
                        .getEncoded())
                .getEncoded(),
            0x9000);

    // --- a. ERROR: session not enabled
    {
      assertFalse(dut.isFlagSessionEnabled());
      assertThrows(
          RejectedExecutionException.class,
          () -> dut.computeCryptographicChecksum(AfiUtils.EMPTY_OS));
    } // end --- a.

    // --- b. establish a trusted channel
    {
      assertFalse(dut.isFlagSessionEnabled());

      assertDoesNotThrow(() -> dut.getGeneralAuthenticateStep2(opRsp));

      assertTrue(dut.isFlagSessionEnabled());
    } // end --- b.

    // --- c. smoke test
    {
      final var input = Hex.toHexDigits(RNG.nextBytes(15));
      final var expected = input + "80";

      final var actual = Hex.toHexDigits(dut.padIso(Hex.toByteArray(input)));

      assertEquals(expected, actual);
    } // end --- c.
  } // end method */

  /** Test method for {@link SecureMessagingConverterSoftware#getEndEntityOpponent()}. */
  @Test
  void test_getEndEntityOpponent() {
    // Assertions:
    // ... a. importCvc(Cvc)-method works as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut =
        new SecureMessagingConverterSoftware(
            GsmcKt80276883110000107637.CVC_CA_E256,
            GsmcKt80276883110000107637.CVC_SMC_AUTD_E256,
            GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256);
    assertThrows(NoSuchElementException.class, dut::getEndEntityOpponent);

    // --- a. smoke test
    {
      final var expected = PocPopp26.CVC_EE_E256;
      dut.importCvc(expected);

      final var actual = dut.getEndEntityOpponent();

      assertEquals(expected, actual);
    } // end --- a.
  } // end method */

  /** Test method for {@link SecureMessagingConverterSoftware#isFlagSessionEnabled()}. */
  @Test
  void test_isFlagSessionEnabled() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check after construction
    // --- b. check after GeneralAuthenticate step 2
    // --- c. check after deciphering error

    final var subCaCvc = PocPopp26.CVC_CA_E256;
    final var eeCvc = PocPopp26.CVC_EE_E256;
    final var prk = PocPopp26.PRK_E256;
    final var dut = new SecureMessagingConverterSoftware(subCaCvc, eeCvc, prk);

    // --- a. check after construction
    {
      assertFalse(dut.isFlagSessionEnabled());
    } // end --- a.

    // --- b. check after GeneralAuthenticate step 2
    {
      final var opCvc = GsmcKt80276883110000107637.CVC_SMC_AUTD_E256;
      assertDoesNotThrow(() -> dut.importCvc(opCvc));
      final var dp = prk.getParams();
      final var opEphemeralPrk = new EcPrivateKeyImpl(dp);
      final var opRsp =
          new ResponseApdu(
              BerTlv.getInstance(
                      0x7c,
                      BerTlv.getInstance(
                              0x85,
                              AfiElcUtils.p2osUncompressed(
                                  opEphemeralPrk.getPublicKey().getW(), dp))
                          .getEncoded())
                  .getEncoded(),
              0x9000);

      assertDoesNotThrow(() -> dut.getGeneralAuthenticateStep2(opRsp));

      assertTrue(dut.isFlagSessionEnabled());
    } // end --- b.

    // --- c. check after deciphering error
    {
      final var cipher = Hex.toByteArray("02 0304"); // wrong padding indicator
      assertThrows(IllegalArgumentException.class, () -> dut.decipher(cipher));

      assertFalse(dut.isFlagSessionEnabled());
    } // end --- c.
  } // end method */
} // end class
