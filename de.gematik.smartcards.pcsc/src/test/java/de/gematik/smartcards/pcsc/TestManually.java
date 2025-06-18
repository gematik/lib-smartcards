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
package de.gematik.smartcards.pcsc;

import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ReadBinary;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.utils.Hex;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing manual tests.
 *
 * <p><i><b>Note:</b> It is intended that this class is disabled during build, because the methods
 * in this class are intended to be executed manually one-by-one.</i>
 *
 * <p><b>Observations:</b>
 *
 * <ol>
 *   <li><b>Alcor AU9560</b>
 *   <li><b>Cloud 2700:</b>
 *       <ol>
 *         <li>{@code sabine@Windows}: reader works like a charm (2022-07-13), it seems that a
 *             driver was installed on 2022-02-16
 *         <li>{@code arthur}: works under kubuntu 22.04 out of the box
 *       </ol>
 *   <li><b>Cloud 4700 Contact Reader:</b>
 *       <ol>
 *         <li>{@code arthur}: works under kubuntu 22.04 out of the box
 *       </ol>
 *   <li><b>Cloud 4700 Contactless Reader:</b>
 *       <ol>
 *         <li>{@code arthur}: works under kubuntu 22.04 out of the box
 *       </ol>
 *   <li><b>OMNIKEY 6121</b>
 *       <ol>
 *         <li>supports TA1=97 (Linux and Windows)
 *         <li>works on kubuntu 20.04 and 22.04 out of the box
 *       </ol>
 *   <li><b>SCR 3310:</b>
 *       <ol>
 *         <li>does not support TA1=97 and shows TA1=96 only (Linux and Windows)
 *         <li>works on kubuntu 20.04 and 22.04 out of the box
 *       </ol>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"
//         Spotbugs message: This instance method writes to a static field.
//         Rational: This is intentional to disable manual tests.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", // see note 1
}) // */
@EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isSmartCardResourceManagerRunning")
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.AvoidDuplicateLiterals",
  "PMD.UnitTestShouldIncludeAssert",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
@TestMethodOrder(MethodOrderer.MethodName.class)
final class TestManually {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestManually.class); // */

  /** Separator. */
  private static final String SEPARATOR = LINE_SEPARATOR + "    "; // */

  /**
   * Flag indicating manual started test methods.
   *
   * <p>The idea behind is as follows:
   *
   * <ol>
   *   <li>If a certain test method is started manually, then this method runs without changing the
   *       code in this class.
   *   <li>If all tests in this class are started, e.g., during a test-suite, then the first
   *       test-method disables all other tests in this class.
   * </ol>
   */
  private static boolean claManualTest = true; // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. show available IFD
    try (IfdCollection dut = new IfdCollection()) {
      final List<String> allReaders = dut.scardListReaders();

      assertFalse(allReaders.isEmpty(), "at least one reader SHALL be available.");

      LOGGER.atInfo().log(
          "Available readers: {}",
          allReaders.stream()
              .collect(
                  Collectors.joining(
                      SEPARATOR, // delimiter
                      SEPARATOR, // prefix
                      ""))); // suffix
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(UNEXPECTED, e);
    } // end Catch (...)
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
   * Returns flag indicating if tests run manually.
   *
   * @return {@code TRUE} if tests are started manually, {@code FALSE} otherwise
   */
  private static boolean isManualTest() {
    return claManualTest;
  } // end method */

  /** Disable test methods when called automatically. */
  @Test
  void test_zzAfi_000_DisableWhenAutomatic() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check the value of the flag
    // --- b. set flag to false
    // --- c. check the value of the flag

    // --- a. check the value of the flag
    assertTrue(isManualTest());

    // --- b. disable manual tests
    claManualTest = false; // ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD

    // --- c. check the value of the flag
    assertFalse(isManualTest());
  } // end method */

  /**
   * Read EF.GDO from first IFD with an ICC present.
   *
   * <p>Sun's PC/SC implementation from package {@code javax.smartcardio} and Sun's Provider.
   */
  @EnabledIf("de.gematik.smartcards.pcsc.TestManually#isManualTest")
  @Test
  void test_zzAfi_100_ReadEfGdo_SunPcSc_SunProvider() {
    // Assertions:
    // - none -

    // Note: The intention here is to have a simple example with Sun's PC/SC implementation.

    // Test strategy:
    // --- a. use Sun's PC/SC implementation to read EF.GDO
    try {
      final TerminalFactory terminalFactory = TerminalFactory.getDefault();
      final CardTerminals cardTerminals = terminalFactory.terminals();
      final CardTerminal ifd = cardTerminals.list(CardTerminals.State.CARD_PRESENT).getFirst();
      final Card icc = ifd.connect("T=1");
      final CardChannel cardChannel = icc.getBasicChannel();
      final CommandAPDU cmdApdu = new CommandAPDU(Hex.toByteArray("00 b0 8200 00"));
      final ResponseAPDU rspApdu = cardChannel.transmit(cmdApdu);

      icc.disconnect(true);

      LOGGER.atInfo().log(
          "read EF.GDO, response APDU: {}", new ResponseApdu(rspApdu.getBytes()).toString());

      assertEquals("SunPCSC", terminalFactory.getProvider().getName());
      assertEquals("javax.smartcardio.TerminalFactory", terminalFactory.getClass().getName());
      assertEquals("sun.security.smartcardio.PCSCTerminals", cardTerminals.getClass().getName());
      assertEquals("sun.security.smartcardio.TerminalImpl", ifd.getClass().getName());
      assertEquals("sun.security.smartcardio.CardImpl", icc.getClass().getName());
      assertEquals("sun.security.smartcardio.ChannelImpl", cardChannel.getClass().getName());
    } catch (CardException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Read EF.GDO from first IFD with an ICC present.
   *
   * <p>Sun's PC/SC implementation from package {@code javax.smartcardio} but {@link AfiPcsc}
   * provider.
   */
  @EnabledIf("de.gematik.smartcards.pcsc.TestManually#isManualTest")
  @Test
  void test_zzAfi_110_ReadEfGdo_SunPcSc_AfiPcsc() {
    // Assertions:
    // - none -

    // Note: The intention here is to have a simple example with Sun's PC/SC implementation
    //       using the provider from class "AfiPcsc".

    // Test strategy:
    // --- a. use Sun's PC/SC implementation with "AfiPcsc"-provider to read EF.GDO
    try {
      final TerminalFactory terminalFactory =
          TerminalFactory.getInstance(AfiPcsc.TYPE, null, new AfiPcsc());
      final CardTerminals cardTerminals = terminalFactory.terminals();
      final CardTerminal ifd = cardTerminals.list(CardTerminals.State.CARD_PRESENT).getFirst();
      final Card icc = ifd.connect("T=1");
      final CardChannel cardChannel = icc.getBasicChannel();
      final CommandAPDU cmdApdu = new CommandAPDU(Hex.toByteArray("00 b0 8200 00"));
      final ResponseAPDU rspApdu = cardChannel.transmit(cmdApdu);

      icc.disconnect(true);

      LOGGER.atInfo().log(
          "read EF.GDO, response APDU: {}", new ResponseApdu(rspApdu.getBytes()).toString());

      assertEquals("de.gematik.smartcards.pcsc.AfiPcsc", terminalFactory.getProvider().getName());
      assertEquals("javax.smartcardio.TerminalFactory", terminalFactory.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.IfdCollection", cardTerminals.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.Ifd", ifd.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.Icc", icc.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.IccChannel", cardChannel.getClass().getName());
    } catch (CardException | NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Read EF.GDO from first IFD with an ICC present.
   *
   * <p>Here functions and classes from this library are fully used.
   */
  @EnabledIf("de.gematik.smartcards.pcsc.TestManually#isManualTest")
  @Test
  void test_zzAfi_120_ReadEfGdo_AfiPcSc() {
    // Assertions:
    // - none -

    // Note: The intention here is to have a simple example with Sun's PC/SC implementation
    //       using the provider from class "AfiPcsc".

    // Test strategy:
    // --- a. use this library to read EF.GDO
    try (var ifdCollection = new IfdCollection()) {
      final Ifd ifd = (Ifd) ifdCollection.list(CardTerminals.State.CARD_PRESENT).getFirst();
      final Icc icc = (Icc) ifd.connect("T=1");
      final ResponseApdu rspApdu = icc.send(new ReadBinary(2, 0, CommandApdu.NE_SHORT_WILDCARD));

      icc.disconnect(true);

      LOGGER.atInfo().log("read EF.GDO, response APDU: {}", rspApdu.toString());
      assertEquals("de.gematik.smartcards.pcsc.Ifd", ifd.getClass().getName());
      assertEquals("de.gematik.smartcards.pcsc.Icc", icc.getClass().getName());
    } catch (CardException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test "Identive CLOUD 2700 R Smart Card Reader [CCID Interface] (53691324212792)" with a T=1
   * card.
   */
  @EnabledIf("de.gematik.smartcards.pcsc.TestManually#isManualTest")
  @Test
  void test_zzAfi_200_Identive_2700_T1() {
    LOGGER.atInfo().log("test_200_Identive_2700_T1");

    // Assertions:
    // ... a. at least one "Identive CLOUD 2700" with an inserted card is present

    // Test strategy:
    // --- a. use "zzz_T1__String(...)"-method
    zzzT1String("2700");
  } // end method */

  /**
   * Test "Identive Identive CLOUD 4500 F Dual Interface Reader [CLOUD 4700 F Contactless Reader]
   * (...)" with a T=1 card.
   */
  @EnabledIf("de.gematik.smartcards.pcsc.TestManually#isManualTest")
  @Test
  void test_zzAfi_300_Identive_4700_contactless() {
    LOGGER.atInfo().log("test_300_Identive_4700_contactless");

    // Assertions:
    // ... a. at least one "Identive CLOUD 4700" with an inserted card is present

    // Test strategy:
    // --- a. use "zzz_T1__String(...)"-method
    zzzT1String("CLOUD 4700 F Contactless");
  } // end method */

  /**
   * Test "Identive Identive CLOUD 4500 F Dual Interface Reader [CLOUD 4700 F Contact Reader] (...)"
   * with a T=1 card.
   */
  @EnabledIf("de.gematik.smartcards.pcsc.TestManually#isManualTest")
  @Test
  void test_zzAfi_340_Identive_4700_T1() {
    LOGGER.atInfo().log("test_340_Identive_4700_T1");

    // Assertions:
    // ... a. at least one "Identive CLOUD 4700" with an inserted card is present

    // Test strategy:
    // --- a. use "zzz_T1__String(...)"-method
    zzzT1String("4700 F Contact");
  } // end method */

  /**
   * Test "HID Global OMNIKEY 6121 Smart Card Reader [OMNIKEY 6121 Smart Card Reader]" with a T=1
   * card.
   */
  @EnabledIf("de.gematik.smartcards.pcsc.TestManually#isManualTest")
  @Test
  void test_zzAfi_500_Omnikey_6121_T1() {
    LOGGER.atInfo().log("test_500_Omnikey_6121_T1");

    // Assertions:
    // ... a. at least one "OMNIKEY 6121" with an inserted card is present

    // Test strategy:
    // --- a. use "zzz_T1__String(...)"-method
    zzzT1String("6121");
  } // end method */

  /** Test "SCM Microsystems Inc. SCR 3310 [CCID Interface] (00000000000000)" with a T=1 card. */
  @EnabledIf("de.gematik.smartcards.pcsc.TestManually#isManualTest")
  @Test
  void test_zzAfi_800_Scr3310_T1() {
    LOGGER.atInfo().log("test_800_Scr3310_T1");

    // Assertions:
    // ... a. at least one "SRC 3310" with an inserted card is present

    // Test strategy:
    // --- a. use "zzz_T1__String(...)"-method
    zzzT1String("3310");
  } // end method */

  /** Send a bunch of commands to an IFD. */
  @EnabledIf("de.gematik.smartcards.pcsc.TestManually#isManualTest")
  @Test
  void test_zzAfi_900_SomeCommands() {
    LOGGER.atTrace().log("test_SomeCommands");

    // final String name = "Alcor";
    // final String name = "2700"; // Identive CLOUD 2700 R Smart Card Reader
    final String name = "3310"; // SCM Microsystems Inc. SCR 3310

    final String separator = LINE_SEPARATOR + "    ";
    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- connect to IFD
      final Ifd ifd =
          (Ifd)
              ifdCollection.list(CardTerminals.State.CARD_PRESENT).stream()
                  .filter(reader -> reader.getName().contains(name))
                  .findAny()
                  .orElseThrow();

      // --- connect to ICC
      final Icc icc = (Icc) ifd.connect("T=1");
      LOGGER.atInfo().log(
          "ATR = {}",
          icc.getAnswerToReset().explain().stream()
              .collect(
                  Collectors.joining(
                      separator, // delimiter
                      separator, // prefix
                      "")));

      // vvvvvvvvvvvvvvvvvvvvvv    issue command-APDU   vvvvvvvvvvvvvvvvvvvvvvvv

      /*/ try to select EF by fileIdentifier
      CommandApdu cmd;
      ResponseApdu rsp;
      final byte[] fid = new byte[2];
      for (int msb = 0; msb < 2; msb++) {
        fid[0] = (byte) msb;
        for (int lsb = 0; lsb < 256; lsb++) {
          fid[1] = (byte) lsb;
          cmd = new CommandApdu(
              0x00, 0xa4,
              0x02, 0x04,
              fid,
              CommandApdu.NE_SHORT_WILDCARD
          );
          rsp = icc.send(cmd);
          if (0x9000 == rsp.getTrailer()) {
            LOGGER.atTrace().log("success");
          } // end fi
        } // end For (lsb...)
      } // end For (msb...)
      // */

      // Read C.CH.AUT.* from an eHC G2.1
      ResponseApdu rsp;
      rsp = icc.send(new CommandApdu("00 b0 9100    00")); // read EF.Version2
      LOGGER.atInfo().log(BerTlv.getInstance(rsp.getData()).toStringTree());
      icc.send(new CommandApdu("00 a4 040c 0a a000000167455349474e"), 0x9000); // select DF.ESIGN
      rsp = icc.send(new ReadBinary(1, 0, CommandApdu.NE_EXTENDED_WILDCARD)); // EF.C.CH.AUT.R2048
      var tlv = (ConstructedBerTlv) BerTlv.getInstance(rsp.getData());
      LOGGER.atInfo().log(tlv.toString(" ", "| "));
      LOGGER.atInfo().log(tlv.toStringTree());
      rsp = icc.send(new ReadBinary(4, 0, CommandApdu.NE_EXTENDED_WILDCARD)); // EF.C.CH.AUT.E256
      tlv = (ConstructedBerTlv) BerTlv.getInstance(rsp.getData());
      LOGGER.atInfo().log(tlv.toString(" ", "| "));
      LOGGER.atInfo().log(tlv.toStringTree());
      // */
      // ^^^^^^^^^^^^^^^^^^^^^^    issue command-APDU   ^^^^^^^^^^^^^^^^^^^^^^^^

      // --- disconnect
      icc.disconnect(true);
    } catch (CardException e) {
      throw new TransmitException(e);
    } // end Catch (...)
  } // end method */

  /**
   * Test an IFD with a T=1 card.
   *
   * @param readerName (part of) IFD name
   */
  private void zzzT1String(final String readerName) {
    // Assertions:
    // ... a. at least one IFD with given name and an inserted card is present

    // Test strategy:
    // --- a. get an IFD with given name and an inserted card
    // --- b. connect to that reader with T=1
    // --- c. select MF without AID but with FCP

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. get an IFD with given name and an inserted card
      final Ifd ifd =
          (Ifd)
              ifdCollection.list(CardTerminals.State.CARD_PRESENT).stream()
                  .filter(reader -> reader.getName().contains(readerName))
                  .findAny()
                  .orElseThrow();

      // --- b. connect to that reader with T=1
      final Icc icc = (Icc) ifd.connect("T=1");
      LOGGER
          .atTrace()
          .log(
              "ATR = {}",
              icc.getAnswerToReset().explain().stream()
                  .collect(
                      Collectors.joining(
                          SEPARATOR, // delimiter
                          SEPARATOR, // prefix
                          "" // suffix
                          )));

      // --- c. select MF without AID but with FCP
      final ResponseApdu rsp =
          icc.send(
              new CommandApdu(
                  0x00,
                  0xa4, // SELECT
                  0x04,
                  0x04, // application, return FCP
                  CommandApdu.NE_EXTENDED_WILDCARD));

      assertEquals(0x9000, rsp.getTrailer());
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
