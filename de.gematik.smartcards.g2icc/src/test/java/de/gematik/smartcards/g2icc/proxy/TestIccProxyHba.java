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
package de.gematik.smartcards.g2icc.proxy;

import static de.gematik.smartcards.g2icc.proxy.IccProxyGsmcK.AID_DF_SAK;
import static de.gematik.smartcards.g2icc.proxy.IccProxyHba.AID_DF_QES;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.EafiLocation.DF_SPECIFIC;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.EafiPasswordFormat.FORMAT_2_PIN_BLOCK;
import static de.gematik.smartcards.utils.AfiUtils.KIBI;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.crypto.X509Utils;
import de.gematik.smartcards.g2icc.cos.EafiCosAlgId;
import de.gematik.smartcards.g2icc.cvc.TrustCenter;
import de.gematik.smartcards.pcsc.Icc;
import de.gematik.smartcards.pcsc.IccChannel;
import de.gematik.smartcards.pcsc.Ifd;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.GetPinStatus;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ReadBinary;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.Verify;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import java.security.interfaces.ECPublicKey;
import java.util.Set;
import javax.smartcardio.CardException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link IccProxyHba}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@EnabledIf("de.gematik.smartcards.g2icc.proxy.PcscStatus#isSmartCardResourceManagerRunning")
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
@TestMethodOrder(MethodOrderer.MethodName.class)
final class TestIccProxyHba extends TestIccProxy {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestIccProxyHba.class);

  /** {@link Ifd} with gSMC-K. */
  private static final Ifd IFD_SMC_K; // */

  /** {@link Icc} used for communication. */
  private static final Icc ICC_SMC_K; // */

  /** Basic logical channel of {@link #ICC_SMC_K}. */
  private static final IccChannel BLC_SMC_K; // */

  /** {@link Ifd} with gSMC-KT. */
  private static final Ifd IFD_SMC_KT; // */

  /** {@link Icc} used for communication. */
  private static final Icc ICC_SMC_KT; // */

  /** Basic logical channel of {@link #ICC_SMC_KT}. */
  private static final IccChannel BLC_SMC_KT; // */

  /** {@link Ifd} with device-under-test. */
  private static final Ifd IFD_USER; // */

  /** {@link Icc} used for communication. */
  private static final Icc ICC_USER; // */

  /** Basic logical channel of {@link #ICC_USER}. */
  private static final IccChannel BLC_USER; // */

  /*
   * static
   */
  static {
    // --- set path of trust-center to something useful
    assertDoesNotThrow(TrustCenter::initialize);

    IFD_SMC_K = getCardTerminal(IccProxyGsmcK.class);
    IFD_SMC_KT = getCardTerminal(IccProxyGsmcKt.class);
    IFD_USER = getCardTerminal(IccProxyHba.class);

    try {
      ICC_SMC_K = (Icc) IFD_SMC_K.connect(T_1);
      BLC_SMC_K = (IccChannel) ICC_SMC_K.getBasicChannel();

      ICC_SMC_KT = (Icc) IFD_SMC_KT.connect(T_1);
      BLC_SMC_KT = (IccChannel) ICC_SMC_KT.getBasicChannel();

      ICC_USER = (Icc) IFD_USER.connect(T_1);
      BLC_USER = (IccChannel) ICC_USER.getBasicChannel();
    } catch (CardException e) {
      fail(UNEXPECTED, e);
      throw new AssertionError(UNEXPECTED, e); // NOPMD throwing raw exception
    } // end Catch (...)
  } // end static */

  /** Method executed after other tests. */
  @AfterAll
  static void tearDownAfterClass() {
    Set.of(ICC_USER, ICC_SMC_KT, ICC_SMC_K)
        .forEach(
            icc -> {
              try {
                icc.disconnect(true);
              } catch (CardException e) {
                LOGGER.atWarn().log(UNEXPECTED, e);
              } // end Catch (...)
            }); // end forEach(icc -> ...)
  } // end method */

  /** Method executed before each test. */
  @BeforeEach
  void setUp() {
    // --- reset the application layer
    ICC_SMC_KT.reset();
    ICC_USER.reset();
  } // end method */

  /** Test method for {@link IccProxyHba#toString()}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented test_toString")
  @Test
  void test_100_toString() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented test_toString");
  } // end method */

  /** Test method for {@link IccProxyHba#getHpcAutdSukCvcE256()}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented getHpcAutdSukCvcE256")
  @Test
  void test_110_getHpcAutdSukCvcE256() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented getHpcAutdSukCvcE256");
  } // end method */

  /** Test method for {@link IccProxyHba#getHpcAutrCvcE256()}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented getHpcAutrCvcE256")
  @Test
  void test_120_getHpcAutrCvcE256() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented getHpcAutrCvcE256");
  } // end method */

  /** Test method for {@link IccProxyHba#getCvcRoleAuthentication()}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented getCvcRoleAuthentication()")
  @Test
  void test_130_getCvcRoleAuthentication() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented getCvcRoleAuthentication()");
  } // end method */

  /** Test method for {@link IccProxyHba#getCvc4Sm()}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented getCvc4Sm()")
  @Test
  void test_140_getCvc4Sm() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented getCvc4Sm()");
  } // end method */

  /**
   * Performs stapel signature.
   *
   * <p>In this method first DF.QES is selected and SE# there is set to "2" and afterward the
   * trusted channels are established.
   *
   * <p>The following steps are performed:
   *
   * <ol>
   *   <li>select DF.QES
   *   <li>MSE Restore SE#2
   *   <li>establish trusted channel
   *   <li>read X.509
   *   <li>select PrK...E256
   *   <li>verify PIN.QES
   *   <li>generate several signatures
   * </ol>
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_200_stapelSignature_a() {
    LOGGER.atDebug().log("start test_stapelSignature");
    // Assertions:
    // ... a. at least one HBA is present (for stapel signature)
    // ... b. at least one gSMC-K is present (providing TC for stapel signature)
    // ... c. at least one gSMC-KT is present (providing TC for remote PIN)

    final var idPinQes = 1;
    final var idPrkQes = 6;
    final var valuePinQes = "123456";
    ResponseApdu rsp;

    try {
      // create Device-Under-Test
      final var dutHba = (IccProxyHba) IccProxy.getInstance(BLC_USER);
      final var dutSmK = (IccProxyGsmcK) IccProxy.getInstance(BLC_SMC_K);
      final var dutSmT = (IccProxyGsmcKt) IccProxy.getInstance(BLC_SMC_KT);
      LOGGER.atDebug().log("HBA:{}{}", LINE_SEPARATOR, dutHba);
      LOGGER.atDebug().log("gSMC-K:{}{}", LINE_SEPARATOR, dutSmK);
      LOGGER.atDebug().log("gSMC-KT:{}{}", LINE_SEPARATOR, dutSmT);

      // --- check the status of PIN.QES
      LOGGER.atDebug().log("select DF.QES");
      dutHba.send(new CommandApdu(0x00, 0xa4, 0x04, 0x0c, AID_DF_QES), 0x9000);
      LOGGER.atDebug().log("GET PIN STATUS (PIN.QES)");
      dutHba.send(new GetPinStatus(DF_SPECIFIC, idPinQes), 0x63c3);
      ICC_USER.reset();

      // --- select the appropriate folder in gSMC-K
      LOGGER.atDebug().log("select DF.SAK");
      dutSmK.send(new CommandApdu(0x00, 0xa4, 0x04, 0x0c, AID_DF_SAK), 0x9000);

      // --- run real test
      LOGGER.atDebug().log("select DF.QES");
      dutHba.send(new CommandApdu(0x00, 0xa4, 0x04, 0x0c, AID_DF_QES), 0x9000);

      LOGGER.atDebug().log("MSE Restore SE#2");
      dutHba.send(new CommandApdu(0x00, 0x22, 0xf3, 0x02), 0x9000);

      LOGGER.atDebug().log("establish Trusted Channel to gSMC-KT");
      var tc = dutHba.establishTrustedChannel(dutSmT);

      LOGGER.atDebug().log("verify PIN.QES");
      tc.send(new Verify(DF_SPECIFIC, idPinQes, FORMAT_2_PIN_BLOCK, valuePinQes));

      LOGGER.atDebug().log("establish Trusted Channel to gSMC-K");
      tc = dutHba.establishTrustedChannel(dutSmK);

      LOGGER.atDebug().log("read X.509 from EF.C.HP.QES.E256, part 1");
      rsp = tc.send(new ReadBinary(6, 0, 0x300), 0x9000);
      final var raw1 = rsp.getData();
      LOGGER.atDebug().log("read X.509 from EF.C.HP.QES.E256, part 2");
      rsp = tc.send(new ReadBinary(0, 0x300, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000);
      final var raw2 = rsp.getData();
      final var x509 = X509Utils.generateCertificate(AfiUtils.concatenate(raw1, raw2));
      final var puk = new EcPublicKeyImpl((ECPublicKey) x509.getPublicKey());

      LOGGER.atDebug().log("select PrK.CH.QES.E256");
      tc.send(
          new CommandApdu(
              0x00,
              0x22,
              0x41,
              0xb6,
              AfiUtils.concatenate(
                  BerTlv.getInstance(0x84, String.format("%02x", 0x80 + idPrkQes)).getEncoded(),
                  BerTlv.getInstance(0x80, EafiCosAlgId.SIGN_ECDSA.getAlgId()).getEncoded())),
          0x9000);

      LOGGER.atDebug().log("generate several signatures");
      for (final var mLen : RNG.intsClosed(0, 64 * KIBI, 10).boxed().toList()) {
        final var message = RNG.nextBytes(mLen);

        LOGGER.atDebug().log("sign message with {} octet", mLen);
        final var signature =
            tc.send(
                    new CommandApdu(
                        0x00,
                        0x2a,
                        0x9e,
                        0x9a,
                        EafiHashAlgorithm.SHA_256.digest(message),
                        CommandApdu.NE_EXTENDED_WILDCARD),
                    0x9000)
                .getData();

        assertTrue(puk.verifyEcdsa(message, signature));
      } // end For (mLen...)
    } catch (IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
