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

import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_S_SUCCESS;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_PRESENT;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.pcsc.lib.ScardReaderState;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ManageChannel;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ReadBinary;
import de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset;
import de.gematik.smartcards.sdcom.isoiec7816objects.EafiIccProtocol;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link Icc}.
 *
 * <p><b>Assertions:</b>
 *
 * <ol>
 *   <li>At least one reader with a smart card available with the following features:
 *       <ol>
 *         <li>supports T=1 protocol.
 *         <li>supports logical channels
 *         <li>responses with NoError = '9000' to all commands from {@link
 *             InvestigateIfds#COMMAND_APDUS}
 *       </ol>
 *   <li>No IFD is disconnected during test.
 *   <li>No ICC is inserted or removed during test.
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isSmartCardResourceManagerRunning")
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.AvoidDuplicateLiterals",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestIccChannel {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestIfdCollection.class); // */

  /** Random number generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** List with reader names with smart cards supporting T=1. */
  private static final List<String> IFD_T1 = new ArrayList<>();

  /** Protocol supported by class-under-test. */
  private static final String T_1 = "T=1"; // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. minimum amount of IFD present
    try (IfdCollection ifdCollection = new IfdCollection()) {
      final List<String> readerNames = ifdCollection.scardListReaders();
      ScardReaderState[] readers = ScardReaderState.createArray(readerNames, false);
      assertEquals(
          SCARD_S_SUCCESS,
          ifdCollection.scardGetStatusChange(
              0, // timeout
              readers));

      for (final ScardReaderState srs : readers) {
        final int state = srs.dwEventState.intValue();

        if (SCARD_STATE_PRESENT == (state & SCARD_STATE_PRESENT)) {
          // ... ICC present
          final var osAtr = Arrays.copyOfRange(srs.rgbAtr, 0, srs.cbAtr.intValue());
          final var atr = new AnswerToReset(osAtr); // NOPMD new in loop
          final EnumSet<EafiIccProtocol> protocols = atr.getSupportedProtocols(); // NOPMD use Set

          if (protocols.contains(EafiIccProtocol.T1)) {
            IFD_T1.add(srs.szReader);

            assert srs.szReader != null;
            if (srs.szReader.contains("OMNIKEY")) { // NOPMD deeply nested if-statements
              // Note 1: OMNIKEY 6121 on gnbe1058 seems to cache an ATR. Sometimes
              //         that reader caches a warm-ATR, but after connecting
              //         SCardStatus gives the cold-ATR (which might be different).
              //         Thus, here we connect so that the driver caches the cold-ATR.
              final Icc icc = new Icc(new Ifd(ifdCollection, srs.szReader), T_1); // NOPMD new loop
              icc.disconnect(true);
            } // end fi
          } // end elif
        } // end fi
      } // end For (srs...)

      // Note 2: OMNIKEY 6121 on gnbe1058 seems to cache an ATR.
      //         So, lets get the status after (possible) connection.
      readers = ScardReaderState.createArray(readerNames, false);
      assertEquals(
          SCARD_S_SUCCESS,
          ifdCollection.scardGetStatusChange(
              0, // timeout
              readers));

      assertFalse(IFD_T1.isEmpty(), () -> "ICC with T=1: " + IFD_T1);
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
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
   * Test method for constructor.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link IccChannel#IccChannel(Icc, int)}
   *   <li>{@link IccChannel#getCard()}
   *   <li>{@link IccChannel#getChannelNumber()}
   * </ol>
   */
  @Test
  void test_Constructor() {
    // Assertions:
    // ... a. close()-method works as expected

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. check basic logical channel
    // --- c. open all available logical channels
    // --- d. close all additional logical channels
    // --- e. ERROR: getChannelNumber() on closed channel
    // --- f. disconnect

    final List<IccChannel> openChannels = new ArrayList<>();

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        // --- b. check basic logical channel
        IccChannel dut = (IccChannel) icc.getBasicChannel();

        assertSame(icc, dut.getCard());
        assertEquals(0, dut.getChannelNumber());

        // --- c. open all available logical channels
        // TODO check the amount of available channels against information from ATR
        openChannels.clear();
        try {
          for (int ccNumber = 1; true; ccNumber++) {
            dut = (IccChannel) icc.openLogicalChannel();
            openChannels.add(dut);

            assertSame(icc, dut.getCard());
            assertEquals(ccNumber, dut.getChannelNumber());
          } // end For (;;)
        } catch (TransmitException e) {
          fail(AfiUtils.UNEXPECTED, e);
        } catch (CardException e) {
          // NoMoreChannelsAvailable
          assertEquals(
              "openLogicalChannel() failed, card response: SW1SW2='6981'  Data absent",
              e.getMessage());
        } // end Catch (...)

        openChannels.forEach(
            cc -> {
              try {
                // --- d. close all additional logical channels
                cc.close();

                // --- e. ERROR: getChannelNumber() on closed channel
                assertThrows(IllegalStateException.class, cc::getChannelNumber);
              } catch (CardException e) {
                fail(AfiUtils.UNEXPECTED, e);
              } // end Catch (...)
            }); // end forEach(cc -> ...)

        // --- f. disconnect
        icc.disconnect(true);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccChannel#send(byte[])}. */
  @Test
  void test_send__byteA() {
    // Assertions:
    // ... a. send(byte[], boolean)-method works as expected

    // Test strategy:
    // --- a. connect to an ICC supporting T=1
    // --- b. send valid command
    // --- c. ERROR: send MANAGE CHANNEL command
    // --- d. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to an ICC supporting T=1
      final String name = IFD_T1.get(0);
      final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1);
      final IccChannel dut = (IccChannel) icc.getBasicChannel();

      // --- b. send valid command
      final ResponseApdu rsp = new ResponseApdu(dut.send(Hex.toByteArray("00 b0 8202 00")));
      assertEquals(0x9000, rsp.getTrailer());

      // --- c. ERROR: send MANAGE CHANNEL command
      assertThrows(IllegalArgumentException.class, () -> dut.send(ManageChannel.OPEN.getBytes()));

      // --- d. disconnect
      icc.disconnect(true);
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccChannel#send(byte[], boolean)}. */
  @Test
  void test_send__byte_boolean() {
    LOGGER.atTrace().log("test_send__byte_boolean");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. open one additional logical channel
    // --- c. send command with channel-number-adjustment
    // --- d. send command without channel-number-adjustment
    // --- e. observe execution time
    // --- f. ERROR: send on closed logical channel
    // --- g. ERROR: send on never opened logical channel
    // --- h. check that MANAGE CHANNEL is possible
    // --- i. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop
        final IccChannel bc = (IccChannel) icc.getBasicChannel();

        try {
          // --- b. open one additional logical channel
          final IccChannel ac = (IccChannel) icc.openLogicalChannel();

          // --- c. send command with channel-number-adjustment
          bc.send(
              Hex.toByteArray("03 b0 8200 00"),
              true); // FIXME check log-file for CLA-byte changes automatically
          ac.send(
              Hex.toByteArray("02 b0 8200 00"),
              true); // FIXME check log-file for CLA-byte changes automatically

          // --- d. send command without channel-number-adjustment
          bc.send(
              Hex.toByteArray("01 b0 8200 00"),
              false); // FIXME check log-file for CLA-byte changes automatically
          ac.send(
              Hex.toByteArray("00 b0 8200 00"),
              false); // FIXME check log-file for CLA-byte changes automatically

          // --- e. observe execution time
          final long startTime = System.nanoTime();
          bc.send(Hex.toByteArray("00 b0 8200 00"), false);
          final long runTime = System.nanoTime() - startTime;
          final double execTime = runTime * 1e-9;

          // Note 1, lower border: Measurements at lower levels are more accurate.
          // Note 2, upper border: Differenz is less than 0.05 seconds.
          assertTrue(
              (bc.getTime() <= execTime) && (bc.getTime() + 0.05 > execTime),
              () -> String.format("libTime = %f, testTime 0 %f", bc.getTime(), execTime));

          // --- f. ERROR: send on closed logical channel
          ac.close();
          assertThrows(
              IllegalStateException.class, () -> ac.send(Hex.toByteArray("01 b0 8201 00"), false));

          // --- g. ERROR: send on never opened logical channel
          assertThrows(
              IllegalStateException.class,
              () ->
                  new IccChannel(icc, 4) // NOPMD new in loop
                      .send(Hex.toByteArray("00 b0 8201 00"), true));

          // --- h. check that MANAGE CHANNEL is possible
          assertDoesNotThrow(() -> bc.send(ManageChannel.OPEN.getBytes(), true));
        } catch (TransmitException e) {
          fail(AfiUtils.UNEXPECTED, e);
        } catch (CardException e) {
          // NoMoreChannelsAvailable
          assertEquals(
              "openLogicalChannel() failed, card response: SW1SW2='6981'  Data absent",
              e.getMessage());
        } // end Catch (...)

        // --- i. disconnect
        icc.disconnect(true);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccChannel#send(CommandApdu)}. */
  @Test
  void test_send__CommandApdu() {
    LOGGER.atTrace().log("test_send__CommandApdu");
    // Assertions:
    // ... a. sendCmd(CommandApdu)-method works as expected

    // Test strategy:
    // --- a. connect to an ICC supporting T=1
    // --- b. send valid command
    // --- c. ERROR: send MANAGE CHANNEL command
    // --- d. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to an ICC supporting T=1
      final String name = IFD_T1.get(0);
      final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1);
      final IccChannel dut = (IccChannel) icc.getBasicChannel();

      // --- b. send valid command
      final ResponseApdu rsp = dut.send(new ReadBinary(2, 0, CommandApdu.NE_SHORT_WILDCARD));
      assertEquals(0x9000, rsp.getTrailer());

      // --- c. ERROR: send MANAGE CHANNEL command
      assertThrows(IllegalArgumentException.class, () -> dut.send(ManageChannel.OPEN));

      // --- d. disconnect
      icc.disconnect(true);
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccChannel#sendCmd(CommandApdu)}. */
  @Test
  void test_sendCmd__CommandApdu() {
    LOGGER.atTrace().log("test_sendCmd__CommandApdu");
    // Assertions:
    // ... a. send(byte[], boolean)-method works as expected

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. open one additional logical channel
    // --- c. send command with channel-number-adjustment
    // --- d. disconnect

    final CommandApdu cmdA = new CommandApdu("03 b0 8200 00");
    final CommandApdu cmdB = new CommandApdu("02 b0 8200 00");

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop
        final IccChannel bc = (IccChannel) icc.getBasicChannel();

        try {
          // --- b. open one additional logical channel
          final IccChannel ac = (IccChannel) icc.openLogicalChannel();

          // --- c. send command with channel-number-adjustment
          bc.sendCmd(cmdA); // FIXME check log-file for CLA-byte changes automatically
          ac.sendCmd(cmdB); // FIXME check log-file for CLA-byte changes automatically
        } catch (TransmitException e) {
          fail(AfiUtils.UNEXPECTED, e);
        } catch (CardException e) {
          // NoMoreChannelsAvailable
          assertEquals(
              "openLogicalChannel() failed, card response: SW1SW2='6981'  Data absent",
              e.getMessage());
        } // end Catch (...)

        // --- d. disconnect
        icc.disconnect(true);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccChannel#transmit(CommandAPDU)}. */
  @Test
  void test_transmit__CommandApdu() {
    LOGGER.atTrace().log("test_transmit__CommandAPDU");
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. send(byte[])-method works as expected

    // Test strategy:
    // --- a. connect to an ICC supporting T=1
    // --- b. call method-under-test
    // --- c. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to an ICC supporting T=1
      final String name = IFD_T1.get(0);
      final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1);
      final IccChannel dut = (IccChannel) icc.getBasicChannel();

      // --- b. call method-under-test
      dut.transmit(new CommandAPDU(Hex.toByteArray("00 b0 8203 00")));
      // FIXME check log-file for CLA-byte changes automatically

      // --- c. disconnect
      icc.disconnect(true);
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccChannel#transmit(ByteBuffer, ByteBuffer)}. */
  @Test
  void test_transmit__ByteBuffer_ByteBuffer() {
    LOGGER.atTrace().log("test_transmit__ByteBuffer_ByteBuffer");
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. send(byte[])-method works as expected

    // Test strategy:
    // --- a. connect to an ICC supporting T=1
    // --- b. send with appropriate buffers
    // --- c. ERROR: command equals response
    // --- d. ERROR: insufficient space in response
    // --- e. ERROR: response is read-only
    // --- f. disconnect

    final ByteBuffer command;
    final ByteBuffer response;

    final CommandApdu cmdApdu = new ReadBinary(2, 0, CommandApdu.NE_SHORT_WILDCARD);

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to an ICC supporting T=1
      final String name = IFD_T1.getFirst();
      final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1);
      final IccChannel dut = (IccChannel) icc.getBasicChannel();
      final ResponseApdu expected = dut.send(cmdApdu, 0x9000);

      // --- b. send with appropriate buffers
      command = ByteBuffer.allocate(200);
      response = ByteBuffer.allocate(expected.getBytes().length);
      command.put(cmdApdu.getBytes()).flip();
      final int length = dut.transmit(command, response);
      final byte[] present = new byte[length];
      response.flip().get(present);

      assertEquals(expected.getBytes().length, length);
      assertEquals(Hex.toHexDigits(expected.getBytes()), Hex.toHexDigits(present));

      // --- c. ERROR: command equals response
      assertThrows(IllegalArgumentException.class, () -> dut.transmit(command, command));
      assertThrows(IllegalArgumentException.class, () -> dut.transmit(response, response));

      // --- d. ERROR: insufficient space in response
      command.clear().put(cmdApdu.getBytes()).flip();
      response.clear().put((byte) 0xaf);
      assertThrows(IllegalArgumentException.class, () -> dut.transmit(command, response));

      // --- e. ERROR: response is read-only
      assertThrows(
          ReadOnlyBufferException.class, () -> dut.transmit(command, response.asReadOnlyBuffer()));

      // --- f. disconnect
      icc.disconnect(true);
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccChannel#close()}. */
  @Test
  void test_close() {
    LOGGER.atTrace().log("test_close");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. open all available logical channels
    // --- c. close all additional logical channels
    // --- d. ERROR: close basic logical channel
    // --- f. disconnect

    final List<IccChannel> openChannels = new ArrayList<>();

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        // --- b. open all available logical channels
        // TODO check the amount of available channels against information from ATR
        openChannels.clear();
        try {
          for (; ; ) {
            openChannels.add((IccChannel) icc.openLogicalChannel());
          } // end For (;;)
        } catch (TransmitException e) {
          fail(AfiUtils.UNEXPECTED, e);
        } catch (CardException e) {
          // NoMoreChannelsAvailable
          assertEquals(
              "openLogicalChannel() failed, card response: SW1SW2='6981'  Data absent",
              e.getMessage());
        } // end Catch (...)

        openChannels.forEach(
            cc -> {
              try {
                // --- c. close all additional logical channels
                cc.close();
                cc.close(); // close again for good code coverage
              } catch (CardException e) {
                fail(AfiUtils.UNEXPECTED, e);
              } // end Catch (...)
            }); // end forEach(cc -> ...)

        // --- d. ERROR: close basic logical channel
        assertThrows(IllegalStateException.class, icc.getBasicChannel()::close);

        // --- f. disconnect
        icc.disconnect(true);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccChannel#reset()}. */
  @Test
  void test_reset() {
    LOGGER.atTrace().log("test_reset");
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. send(CommandApdu)-method works as expected

    // Test strategy:
    // --- a. connect to an ICC supporting T=1
    // --- b. call method-under-test
    // --- c. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to an ICC supporting T=1
      final String name = IFD_T1.get(0);
      final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1);
      final IccChannel dut = (IccChannel) icc.getBasicChannel();

      // --- b. call method-under-test
      dut.reset(); // FIXME check log-file for CLA-byte changes automatically

      // --- c. disconnect
      icc.disconnect(true);
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for execution time.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link Icc#setTime(double)}
   *   <li>{@link Icc#getTime()}
   * </ol>
   */
  @Test
  void test_time() {
    LOGGER.atTrace().log("test_time");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: These simple methods do not need extensive testing.
    //       So we can be lazy here.

    // Test strategy:
    // --- a. connect to an ICC supporting T=1
    // --- b. set and get time
    // --- c. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to an ICC supporting T=1
      final String name = IFD_T1.get(0);
      final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1);
      final IccChannel dut = (IccChannel) icc.getBasicChannel();

      // --- b. set and get time
      {
        final double time = RNG.nextDouble();
        dut.setTime(time);

        assertEquals(time, dut.getTime(), 1e-6);
      } // end --- b.

      // --- c. disconnect
      icc.disconnect(true);
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IccChannel#toString()}. */
  @Test
  void test_toString() {
    LOGGER.atTrace().log("test_toString");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: These simple methods do not need extensive testing.
    //       So we can be lazy here.

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. open all available logical channels
    // --- c. call method under test
    // --- c. disconnect

    final List<IccChannel> openChannels = new ArrayList<>();

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc icc = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        // --- b. open all available logical channels
        // b.1 basic logical channel
        assertEquals(icc + ", channelNumber 0", icc.getBasicChannel().toString());

        // TODO check the amount of available channels against information from ATR
        openChannels.clear();
        try {
          for (; ; ) {
            openChannels.add((IccChannel) icc.openLogicalChannel());
          } // end For (;;)
        } catch (TransmitException e) {
          fail(AfiUtils.UNEXPECTED, e);
        } catch (CardException e) {
          // NoMoreChannelsAvailable
          assertEquals(
              "openLogicalChannel() failed, card response: SW1SW2='6981'  Data absent",
              e.getMessage());
        } // end Catch (...)

        // b.2 all other logical channels
        openChannels.forEach(
            cc -> {
              assertEquals(icc + ", channelNumber " + cc.getChannelNumber(), cc.toString());
            }); // end forEach(cc -> ...)

        // --- c. disconnect
        icc.disconnect(true);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
