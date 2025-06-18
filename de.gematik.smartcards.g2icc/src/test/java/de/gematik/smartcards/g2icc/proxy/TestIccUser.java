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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.g2icc.GsmcKt80276883110000107637;
import de.gematik.smartcards.g2icc.cos.EafiCosAlgId;
import de.gematik.smartcards.g2icc.cos.SecureMessagingConverterSoftware;
import de.gematik.smartcards.g2icc.cos.SecureMessagingLayer;
import de.gematik.smartcards.pcsc.Icc;
import de.gematik.smartcards.pcsc.Ifd;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.EafiPasswordFormat;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.GetPinStatus;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.Verify;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import javax.smartcardio.CardException;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Class performing white-box tests for {@link TestIccUser}.
 *
 * <p>The tests in this class expect the following types of cards:
 *
 * <ol>
 *   <li>At least one {@link IccProxyEgk}.
 *   <li>At least one {@link IccProxyHba} or one {@link IccProxySmcB}.
 *   <li>At least one {@link IccProxyGsmcKt}.
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@EnabledIf("de.gematik.smartcards.g2icc.proxy.PcscStatus#isSmartCardResourceManagerRunning")
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.AvoidDuplicateLiterals",
  "PMD.MethodNamingConventions",
  "checkstyle.methodname"
})
@TestMethodOrder(MethodOrderer.MethodName.class)
final class TestIccUser extends TestIccProxy {

  /** ISO case 1: Select per AID, no command data field, no response data field. */
  public static final CommandApdu ISO_CASE_1 = new CommandApdu("00 a4  040c"); // */

  /** ISO case 2: Read EF.GDO per shortFileIdentifier. */
  public static final CommandApdu ISO_CASE_2 = new CommandApdu("00 b0 8200   00"); // */

  /** ISO case 3: Select EF.ATR, no response data field. */
  public static final CommandApdu ISO_CASE_3 = new CommandApdu("00 a4 020c   02   2f01"); // */

  /** ISO case 4: Select EF.DIR, request FCP, use the extended APDU format. */
  public static final CommandApdu ISO_CASE_4 = new CommandApdu("00a4 0204 00 0002 2f00 0000"); // */

  /**
   * List of APDU covering all ISO cases.
   *
   * <p><i><b>Note:</b> This list of commands is supported by all G2.x cards in any order if the MF
   * is the current DF.</i>
   */
  private static final List<CommandApdu> COMMAND_APDUS =
      List.of(ISO_CASE_1, ISO_CASE_2, ISO_CASE_3, ISO_CASE_4); // */

  /** Communication protocol. */
  private static final String T_1 = "T=1"; // */

  /** Get Pin Status command for PIN.CH. */
  private static final CommandApdu GET_PIN_STATUS =
      new GetPinStatus(
          AbstractAuthenticate.EafiLocation.GLOBAL, 0x01 // pwdIdentifier of PIN.CH
          );

  /** Verify command for PIN.CH. */
  private static final CommandApdu VERIFY =
      new Verify(
          AbstractAuthenticate.EafiLocation.GLOBAL,
          0x01, // pwdIdentifier of PIN.CH
          EafiPasswordFormat.FORMAT_2_PIN_BLOCK,
          "123456");

  /**
   * Test method for{@link IccUser#establishTrustedChannel(IccSmc)}.
   *
   * <p>Assertions:
   *
   * <ol>
   *   <li>an IFD with a gSMC-KT is present
   *   <li>an IFD with an HPC or SMC-B is present
   * </ol>
   */
  @Test
  void test_10_establishTrustedChannel__IccSmc() {
    LOGGER.atTrace().log("start test_establishTrustedChannel__IccSmc");

    try {
      // --- retrieve appropriate card terminals
      final Set<Ifd> ifdSmcList = getIfds(IccProxyGsmcKt.class);
      final Set<Ifd> ifdUserList = // Note: eGK has no CVC with flag RemotePinReceiver.
          getIfds(IccProxyHba.class, IccProxySmcB.class);

      for (final Ifd ifdSmc : ifdSmcList) {
        LOGGER.atTrace().log("connect to SMC");
        final Icc iccSmc = (Icc) ifdSmc.connect(T_1);
        final IccSmc smc = (IccSmc) IccProxy.getInstance(iccSmc);
        LOGGER.atDebug().log("SMC-type = {}", smc.getClass().getSimpleName());

        for (final Ifd ifdUser : ifdUserList) {
          LOGGER.atTrace().log("connect to HBA or SMC-B");
          final Icc iccUser = (Icc) ifdUser.connect(T_1);
          final IccUser user = (IccUser) IccProxy.getInstance(iccUser);
          LOGGER.atDebug().log("User-type = {}", user.getClass().getSimpleName());
          LOGGER.atTrace().log("card information: {}", user.toString());

          LOGGER.atTrace().log("establish Trusted-Channel");
          final SecureMessagingLayer smLayer = user.establishTrustedChannel(smc);
          smLayer.setSmCmdEnc(true);

          LOGGER.atTrace().log("use Trusted-Channel");
          IntStream.rangeClosed(0, 3)
              .forEach(
                  i ->
                      COMMAND_APDUS.forEach(
                          cmd ->
                              assertEquals(
                                  0x9000,
                                  smLayer.send(cmd).getTrailer()))); // end forEach(i -> ...)

          // --- disconnect
          iccUser.disconnect(true);
        } // end For (ifdUser...)

        iccSmc.disconnect(true);
      } // for (ifdSmc...)
    } catch (CardException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link IccUser#establishTrustedChannel(IccSmc)}.
   *
   * <p>Assertions:
   *
   * <ol>
   *   <li>an IFD with an eGK, HPC or SMC-B is present
   * </ol>
   */
  @Test
  void test_20_establishTrustedChannel__IccSmcSimulator() {
    LOGGER.atTrace().log("start test_establishTrustedChannel__IccSmcSimulator");

    final IccSmc dut = new IccProxyGsmcKt(new SmcSimulator());

    try {
      // --- retrieve appropriate card terminals
      for (final Ifd ifdUser : getIfds(IccUser.class)) {
        LOGGER.atTrace().log("connect to eGK, HBA or SMC-B");
        final Icc iccUser = (Icc) ifdUser.connect(T_1);
        final IccUser user = (IccUser) IccProxy.getInstance(iccUser);
        LOGGER.atDebug().log("User-type = {}", user.getClass().getSimpleName());
        LOGGER.atTrace().log("card information: {}", user.toString());

        LOGGER.atTrace().log("establish Trusted-Channel");
        final SecureMessagingLayer smLayer = user.establishTrustedChannel(dut);
        smLayer.setSmCmdEnc(false);

        LOGGER.atTrace().log("use Trusted-Channel");
        IntStream.rangeClosed(0, 3)
            .forEach(
                i ->
                    COMMAND_APDUS.forEach(
                        cmd ->
                            assertEquals(
                                "9000",
                                String.format(
                                    "%04x",
                                    smLayer.send(cmd).getTrailer())))); // end forEach(i -> ...)

        // --- disconnect
        iccUser.disconnect(true);
      } // end For (ifd...)
    } catch (CardException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link IccUser#establishTrustedChannel(SecureMessagingConverterSoftware)}.
   *
   * <p>Assertions:
   *
   * <ol>
   *   <li>an IFD with any type of {@link IccUser} is present
   * </ol>
   */
  @Test
  void test_22_establishTrustedChannel__SmTransformerSoftware() {
    LOGGER.atTrace().log("start test_establishTrustedChannel__IccSmcSimulator");

    // Assertions:
    // ... a. at least one IFD with an appropriate ICC is available

    // Test strategy:
    // --- a. establish a trusted channel
    // --- b. send a bunch of APDU
    // --- c. send a bunch of prepared APDU

    final Set<Ifd> setIfd = getIfds(IccUser.class);
    final var transformer =
        new SecureMessagingConverterSoftware(
            GsmcKt80276883110000107637.CVC_CA_E256,
            GsmcKt80276883110000107637.CVC_SMC_AUTD_E256,
            GsmcKt80276883110000107637.PRK_SMC_AUTD_RPS_CVC_E256);

    try {
      final List<CommandApdu> commands = new ArrayList<>();
      for (final var ifd : setIfd) {
        LOGGER.atTrace().log("connect to ICC");
        final var icc = (Icc) ifd.connect(T_1);
        final var user = (IccUser) IccProxy.getInstance(icc);
        LOGGER.atDebug().log("User-type = {}", user.getClass().getSimpleName());
        LOGGER.atTrace().log("card information: {}", user.toString());

        // --- a. establish a trusted channel
        {
          assertDoesNotThrow(() -> user.establishTrustedChannel(transformer));
        } // end --- a.

        // --- b. send a bunch of APDU one by one
        LOGGER.atTrace().log("send a bunch of APDU one by one");
        for (final var cmd : COMMAND_APDUS) {
          final var cmdSm = transformer.secureCommand(cmd);
          final var rspSm = user.send(cmdSm, 0x9000);
          final var rsp = transformer.unsecureResponse(rspSm);
          assertEquals(0x9000, rsp.getSw());
        } // end For (cmd...)
        // end --- b.

        // --- c. send a bunch of prepared APDU
        {
          final var maxIndex = COMMAND_APDUS.size() - 1;
          commands.clear();
          while (commands.size() < 100) {
            final var cmd = COMMAND_APDUS.get(RNG.nextIntClosed(0, maxIndex));
            commands.add(transformer.secureCommand(cmd));
          } // end While (not enough commands)
          LOGGER.atTrace().log("send a bunch of prepared APDU");
          final var responses = commands.stream().map(user::send).toList();
          final var unexpected =
              responses.stream()
                  .map(transformer::unsecureResponse)
                  .filter(rsp -> 0x9000 != rsp.getSw())
                  .toList();
          assertTrue(unexpected.isEmpty(), unexpected::toString);
        } // end --- c.

        icc.disconnect(true);
      } // end For (ifd...)
    } catch (CardException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link IccUser#externalAuthenticate(IccUser)}.
   *
   * <p>Assertions:
   *
   * <ol>
   *   <li>an IFD with a HPC or SMC-B is present
   *   <li>an IFD with an eGK is present
   * </ol>
   */
  @Test
  void test_30_externalAuthenticate() {
    LOGGER.atTrace().log("start test_externalAuthenticate");

    try {
      // --- retrieve appropriate card terminals
      final Set<Ifd> ifdLeoList = getIfds(IccProxyHba.class, IccProxySmcB.class);
      final Set<Ifd> ifdEgkList = getIfds(IccProxyEgk.class);
      for (final Ifd ifdLeo : ifdLeoList) {
        LOGGER.atTrace().log("connect to LEO");
        final Icc iccLeo = (Icc) ifdLeo.connect(T_1);
        final IccUser leo = (IccUser) IccProxy.getInstance(iccLeo);
        LOGGER.atDebug().log("LEO-type = {}", leo.getClass().getSimpleName());
        LOGGER.atTrace().log("card information: {}", leo.toString());
        leo.send(GET_PIN_STATUS, 0x9000, 0x63c3);
        leo.send(VERIFY, 0x9000);

        for (final Ifd ifdEgk : ifdEgkList) {
          LOGGER.atTrace().log("connect to eGK");
          final Icc iccEgk = (Icc) ifdEgk.connect(T_1);
          final IccUser egk = (IccUser) IccProxy.getInstance(iccEgk);
          LOGGER.atTrace().log("card information: {}", egk.toString());

          LOGGER.atTrace().log("perform test");
          final boolean success = egk.externalAuthenticate(leo);

          assertTrue(success);

          // --- disconnect
          iccEgk.disconnect(true);
        } // end For (ifdEgk...)

        iccLeo.disconnect(true);
      } // end For (ifdLeo...)
    } catch (CardException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccUser#internalAuthenticate(byte[])}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_40_internalAuthenticate__byteA() {
    LOGGER.atDebug().log("start test_internalAuthenticate__byteA");

    try {
      for (final Ifd ifd : getIfds(IccUser.class)) {
        final Icc icc = (Icc) ifd.connect(T_1);

        // --- a. get a Proxy
        final IccUser proxy = (IccUser) IccProxy.getInstance(icc);
        LOGGER.atDebug().log("User-type = {}", proxy.getClass().getSimpleName());
        LOGGER.atTrace().log("card information: {}", proxy.toString());

        // --- b. user verification
        if ((proxy instanceof IccProxyHba) || (proxy instanceof IccProxySmcB)) {
          proxy.send(VERIFY, 0x9000);
        } // end fi

        final byte[] token = RNG.nextBytes(24);
        final byte[] response = proxy.internalAuthenticate(token);
        final EcPublicKeyImpl puk = proxy.getCvcRoleAuthentication().getPublicKey();
        final BigInteger htau =
            new BigInteger(
                1,
                // message, see (N086.900)a
                AfiUtils.concatenate(
                    token, Hex.toByteArray(EafiCosAlgId.ELC_ROLE_AUTHENTICATION.getAlgId())));
        LOGGER.atTrace().log("htau = {}", htau.toString(16));

        final boolean isSignatureValid = puk.verifyEcdsa(htau, response);

        assertTrue(isSignatureValid);

        // --- disconnect
        icc.disconnect(true);
      } // end For (ifd...)
    } catch (CardException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
