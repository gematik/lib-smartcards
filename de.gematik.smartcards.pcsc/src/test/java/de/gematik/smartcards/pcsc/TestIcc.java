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

import static de.gematik.smartcards.pcsc.constants.PcscStatus.OS_WINDOWS;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_S_SUCCESS;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_CHANGED;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_EXCLUSIVE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_INUSE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_PRESENT;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_UNPOWERED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.pcsc.lib.Dword;
import de.gematik.smartcards.pcsc.lib.ScardReaderState;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ReadBinary;
import de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset;
import de.gematik.smartcards.sdcom.isoiec7816objects.EafiIccProtocol;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;
import javax.smartcardio.ATR;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
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
 *   <li>At least one reader with a smart card is available which has the following features:
 *       <ol>
 *         <li>supports T=1 protocol
 *         <li>supports logical channels
 *         <li>has an EF.GDO, i.e. a transparent EF with shortFileIdentifier = 2.
 *         <li>responses with NoError = '9000' to all commands from {@link
 *             InvestigateIfds#COMMAND_APDUS}
 *       </ol>
 *   <li>No IFD is disconnected during test.
 *   <li>No ICC is inserted or removed during test.
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "ES_COMPARING_STRINGS_WITH_EQ", i.e.
//         Short message: Comparison of String parameter using == or !=
//         Rational: That finding is correct, but intentionally assertSame(...) is used.
// Note 2: Spotbugs claims "NP_NULL_PARAM_DEREF"
//         Short message: Null passed for non-null parameter.
//         Rational: The finding is correct, "ScardReaderState.szReader" is
//             defined as "@Nullable" but parameter "name" in the constructor
//             is defined a "@NonNullable".
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ES_COMPARING_STRINGS_WITH_EQ", // see note 1
  "NP_NULL_PARAM_DEREF" // see note 2
}) // */
@EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isSmartCardResourceManagerRunning")
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestIcc {

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
   * Test method for constructor and getters.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link Icc#Icc(Ifd, String)}
   *   <li>{@link Icc#getATR()}
   *   <li>{@link Icc#getAnswerToReset()}
   *   <li>{@link Icc#getBasicChannel()}
   *   <li>{@link Icc#getIfd()}
   *   <li>{@link Icc#getLogger()}
   *   <li>{@link Icc#getProtocol()}
   * </ol>
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_Icc__Ifd_String() {
    LOGGER.atTrace().log("test_Icc__Ifd_String");
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. loop over all IFD with an ICC supporting T=1
    // --- b. connect with T=1, control instance attributes
    // --- c. ERROR: connect with other protocols

    try (IfdCollection ifdCollection = new IfdCollection()) {
      final ScardReaderState[] readers = ScardReaderState.createArray(IFD_T1, false);
      assertEquals(
          SCARD_S_SUCCESS,
          ifdCollection.scardGetStatusChange(
              0, // timeout
              readers));

      // --- a. loop over all IFD with an ICC supporting T=1
      for (final ScardReaderState srs : readers) {
        final AnswerToReset expectedAtr =
            new AnswerToReset(Arrays.copyOfRange(srs.rgbAtr, 0, srs.cbAtr.intValue()));
        final Ifd ifd = new Ifd(ifdCollection, srs.szReader); // NP_NULL_PARAM_DEREF

        // --- b. connect with T=1, control instance attributes
        {
          final Icc dut = new Icc(ifd, T_1); // NOPMD new in loop
          final CardChannel cc = dut.getBasicChannel();
          final Logger logger = dut.getLogger();

          assertSame(ifd, dut.getIfd()); // spotbugs: ES_COMPARING_STRINGS_WITH_EQ
          assertSame(T_1, dut.getProtocol());
          assertEquals(expectedAtr, dut.getAnswerToReset());
          assertEquals(new ATR(expectedAtr.getBytes()), dut.getATR());
          assertNotNull(logger);
          assertTrue(logger.getName().startsWith(dut.getClass().getName() + "."), logger::getName);
          assertNotNull(cc);
          assertEquals(0, cc.getChannelNumber());
          // FIXME assertDoesNotThrow(dut::checkExclusive);
          assertDoesNotThrow(dut::checkState);

          dut.disconnect(true);
        } // end --- b. */

        // --- c. ERROR: connect with other protocols
        List.of(
                "", // empty string
                "T=0", // likely value
                "*", // likely value
                "foo")
            .forEach(
                protocol ->
                    assertThrows(
                        IllegalArgumentException.class,
                        () -> new Icc(ifd, protocol))); // end forEach(protocol -> ...)
        // end --- c.
      } // end For (srs...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Icc#checkState()}. */
  @Test
  void test_checkState() {
    LOGGER.atTrace().log("test_checkState");
    // Assertions:
    // ... a. no card is removed while this test-class is running
    // ... b. constructor(s) work as expected
    // ... d. disconnect(boolean)-method works as expected

    // Note 1: For good code coverage ICC insertion and removal is necessary.
    //         According to assertion a (see above) this is not the case here.
    //         Thus, code coverage is poor :-(

    // Test strategy:
    // --- a. connect to all ICC supporting T=1 and checkState
    // --- b. disconnect and checkState

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1 and checkState
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        assertDoesNotThrow(dut::checkState);

        // --- b. disconnect and checkState
        dut.disconnect(true);

        assertThrows(IllegalStateException.class, dut::checkState);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Icc#openLogicalChannel()}. */
  @Test
  void test_openLogicalChannel() {
    LOGGER.atTrace().log("openLogicalChannel");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. open all available logical channels
    // --- c. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        // --- b. open all available logical channels
        // TODO check the amount of available channels against information from ATR
        try {
          for (; ; ) {
            final CardChannel cc = dut.openLogicalChannel();
            final int ccNumber = cc.getChannelNumber();

            assertFalse(dut.isClosed(ccNumber));
          } // end For (;;)
        } catch (TransmitException e) {
          fail(AfiUtils.UNEXPECTED, e);
        } catch (CardException e) {
          // NoMoreChannelsAvailable
          assertEquals(
              "openLogicalChannel() failed, card response: SW1SW2='6981'  Data absent",
              e.getMessage());
        } // end Catch (...)

        // --- c. disconnect
        dut.disconnect(true);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** test method for {@link Icc#reset()}. */
  @Test
  void test_reset() {
    LOGGER.atTrace().log("test_reset");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. open all available logical channels
    // --- c. reset and check channel status
    // --- d. disconnect

    final List<Integer> channelNumbers = new ArrayList<>();

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        // --- b. open all available logical channels
        channelNumbers.clear();
        channelNumbers.add(0); // add basic logical channel
        // TODO check the amount of available channels against information from ATR
        try {
          for (; ; ) {
            final CardChannel cc = dut.openLogicalChannel();
            final int ccNumber = cc.getChannelNumber();
            channelNumbers.add(ccNumber);
          } // end For (;;)
        } catch (TransmitException e) {
          fail(AfiUtils.UNEXPECTED, e);
        } catch (CardException e) {
          // NoMoreChannelsAvailable
          assertEquals(
              "openLogicalChannel() failed, card response: SW1SW2='6981'  Data absent",
              e.getMessage());
        } // end Catch (...)

        // --- c. reset and check channel status
        channelNumbers.forEach(ccNo -> assertFalse(dut.isClosed(ccNo)));

        dut.reset();

        channelNumbers.forEach(ccNo -> assertEquals(0 != ccNo, dut.isClosed(ccNo)));

        // --- d. disconnect
        dut.disconnect(true);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {}@link {@link Icc#isClosed(int)}. */
  @Test
  void test_isClosed() {
    LOGGER.atTrace().log("test_isClosed");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note 1: Here we test with just the basic logical channel open.
    //         More tests should be done witch class IccChannel.

    // Test strategy:
    // --- a. connect to an ICC supporting T=1
    // --- b. call method-under-test with various channel numbers
    // --- c. disconnect and call method-under-test

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1 and checkState
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        // --- b. call method-under-test with various channel numbers
        assertFalse(dut.isClosed(0)); // basic logical channel is always open
        assertTrue(dut.isClosed(1)); // no other channel is open

        // --- c. disconnect and call method-under-test
        dut.disconnect(true);

        IntStream.range(0, 20)
            .forEach(
                channelNumber ->
                    assertThrows(IllegalStateException.class, () -> dut.isClosed(channelNumber)));
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for various methods.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>{@link Icc#beginExclusive()}
   *   <li>{@code Icc#checkExclusive()}
   *   <li>{@link Icc#endExclusive()}
   * </ol>
   */
  @Test
  void test_exclusive() {
    LOGGER.atTrace().log("test_exclusive");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: Neither beginExclusive nor endExclusive is currently implemented.
    //       Thus, code coverage is poor.

    // Test strategy:
    // --- a. smoke test with all IFD where an ICC supporting T=1 is present
    try (IfdCollection ifdCollection = new IfdCollection()) {
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        assertThrows(PcscException.class, dut::beginExclusive);
        // FIXME assertDoesNotThrow(dut::checkExclusive);
        assertThrows(PcscException.class, dut::endExclusive);

        dut.disconnect(true);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Icc#transmitControlCommand(int, byte[])}. */
  @Test
  void test_transmitControlCommand__int_byteA() {
    LOGGER.atTrace().log("test_transmitControlCommand__int_byteA");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. smoke test with all IFD where an ICC supporting T=1 is present
    try (IfdCollection ifdCollection = new IfdCollection()) {
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        assertThrows(PcscException.class, () -> dut.transmitControlCommand(0, AfiUtils.EMPTY_OS));
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Icc#disconnect(boolean)}. */
  @Test
  void test_disconnect__boolean() {
    LOGGER.atTrace().log("test_disconnect__boolean");
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. send(CommandApdu)-method works as expected

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. disconnect(false) => card remains powered
    // --- c. disconnect(true) => card is unpowered
    // --- d. disconnect again

    final ReadBinary rbWithSfi = new ReadBinary(2, 0, CommandApdu.NE_SHORT_WILDCARD);
    final ReadBinary rbWoSfi = new ReadBinary(0, 0, CommandApdu.NE_SHORT_WILDCARD);

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final ScardReaderState[] readers = ScardReaderState.createArray(List.of(name), false);
        final ScardReaderState srs = readers[0];

        final Ifd ifd = new Ifd(ifdCollection, name); // NOPMD new in loop
        final Icc dutA = new Icc(ifd, T_1); // NOPMD new in loop
        srs.dwEventState = new Dword(); // NOPMD new in loop
        assertEquals(SCARD_S_SUCCESS, ifdCollection.scardGetStatusChange(0, readers));
        final int expectedState = SCARD_STATE_CHANGED | SCARD_STATE_PRESENT | SCARD_STATE_EXCLUSIVE;
        // check state
        assertEquals(
            expectedState + (OS_WINDOWS ? SCARD_STATE_INUSE : 0),
            srs.dwEventState.intValue() & 0xffff);
        // READ BINARY with SFI => currentEF is set
        assertEquals(0x9000, dutA.send(rbWithSfi).getTrailer());

        // --- b. disconnect(false) => card remains powered
        dutA.disconnect(false);
        srs.dwEventState = new Dword(); // NOPMD new in loop
        assertEquals(SCARD_S_SUCCESS, ifdCollection.scardGetStatusChange(0, readers));
        // check state
        assertEquals(
            SCARD_STATE_CHANGED | SCARD_STATE_PRESENT, srs.dwEventState.intValue() & 0xffff);

        // --- c. disconnect(true) => card is unpowered
        final Icc dutB = new Icc(ifd, T_1); // NOPMD new in loop
        srs.dwEventState = new Dword(); // NOPMD new in loop
        assertEquals(SCARD_S_SUCCESS, ifdCollection.scardGetStatusChange(0, readers));
        // check state
        assertEquals(
            expectedState + (OS_WINDOWS ? SCARD_STATE_INUSE : 0),
            srs.dwEventState.intValue() & 0xffff);
        // READ BINARY without SFI => currentEF is used
        assertEquals(0x9000, dutB.send(rbWoSfi).getTrailer());

        dutB.disconnect(true);
        srs.dwCurrentState = srs.dwEventState;
        srs.dwEventState = new Dword(); // NOPMD new in loop
        assertEquals(SCARD_S_SUCCESS, ifdCollection.scardGetStatusChange(0, readers));

        // check state
        assertEquals(
            OS_WINDOWS
                ? SCARD_STATE_CHANGED | SCARD_STATE_PRESENT | SCARD_STATE_UNPOWERED
                : SCARD_STATE_CHANGED | SCARD_STATE_PRESENT,
            srs.dwEventState.intValue() & 0xffff);

        // --- d. disconnect again
        assertDoesNotThrow(() -> dutB.disconnect(true));
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Icc#send(byte[], double...)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_send__byteA_doubleA() {
    LOGGER.atTrace().log("test_send__byteA_doubleA");
    // Assertions:
    // ... a. no card is removed while this test-class is running
    // ... b. constructor(s) work as expected

    // Note 1: The behaviour of the underlying methods should be tested elsewhere.
    //         Thus, here we do no more than smoke tests.
    // Note 2: For good code coverage ICC insertion and removal is necessary.
    //         According to assertion a (see above) this is not the case here.
    //         Thus, code coverage is poor :-(

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. send commands covering all ISO/IEC 7816 cases
    // --- c. disconnect
    // --- d. ERROR: send another command

    final double[] executionTime = new double[1];

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1);

        for (final CommandApdu cmd : InvestigateIfds.COMMAND_APDUS) {
          final ResponseApdu rsp = new ResponseApdu(dut.send(cmd.getBytes(), executionTime));

          assertEquals(0x9000, rsp.getTrailer());
        } // end For (cmd...)

        // --- c. disconnect
        dut.disconnect(true);

        // --- d. ERROR: send another command
        assertThrows(
            IllegalStateException.class,
            () -> dut.send(InvestigateIfds.COMMAND_APDUS.getFirst().getBytes(), executionTime));
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Icc#send(byte[])}. */
  @Test
  void test_send__byte() {
    LOGGER.atTrace().log("test_send__byte");
    // Assertions:
    // ... a. the assertions of the underlying method send(byte[], double...) are met
    // ... b. send(byte[], double...)-method works as expected

    // Note: Because of the assertions we can be lazy here and concentrate
    //       on code coverage.

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. send commands covering all ISO/IEC 7816 cases
    // --- c. check instance attribute "insTime"
    // --- d. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        for (final CommandApdu cmd : InvestigateIfds.COMMAND_APDUS) {
          final long startTime = System.nanoTime();
          final ResponseApdu rsp = new ResponseApdu(dut.send(cmd.getBytes())); // NOPMD new in loop
          final long runTime = System.nanoTime() - startTime;
          final double execTime = runTime * 1e-9;

          assertEquals(0x9000, rsp.getTrailer());
          // Note 1, lower border: Measurements at lower levels are more accurate.
          // Note 2, upper border: Differenz is less than 0.05 seconds.
          assertTrue(
              (dut.getTime() <= execTime) && (dut.getTime() + 0.05 > execTime),
              () -> String.format("libTime = %f, testTime 0 %f", dut.getTime(), execTime));
        } // end For (cmd...)

        // --- d. disconnect
        dut.disconnect(true);
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Icc#send(CommandApdu)}. */
  @Test
  void test_send__CommandApdu() {
    LOGGER.atTrace().log("test_send__CommandApdu");
    // Assertions:
    // ... a. the assertions of the underlying method send(byte[]) are met
    // ... b. send(byte[])-method works as expected

    // Note: Because of the assertions we can be lazy here and concentrate
    //       on code coverage.

    // Test strategy:
    // --- a. connect to all ICC supporting T=1
    // --- b. send commands covering all ISO/IEC 7816 cases
    // --- c. check instance attribute "insTime"
    // --- d. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        for (final CommandApdu cmd : InvestigateIfds.COMMAND_APDUS) {
          final ResponseApdu rsp = dut.send(cmd);

          assertEquals(0x9000, rsp.getTrailer());
        } // end For (cmd...)

        // --- d. disconnect
        dut.disconnect(true);
      } // end For (name...)
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

    // Note: These simple methods do not need extensive testing,
    //       So we can be lazy here.

    // Test strategy:
    // --- a. connect to an ICC supporting T=1
    // --- b. set and get time
    // --- c. disconnect

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to an ICC supporting T=1
      final String name = IFD_T1.get(0);
      final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1);

      // --- b. set and get time
      {
        final double time = RNG.nextDouble();
        dut.setTime(time);

        assertEquals(time, dut.getTime(), 1e-6);
      } // end --- b.

      // --- c. disconnect
      dut.disconnect(true);
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Icc#isValid()}. */
  @Test
  void test_isValid() {
    LOGGER.atTrace().log("test_isValid");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note 1: For good code coverage ICC insertion and removal is necessary.
    //         According to assertion a (see above) this is not the case here.
    //         Thus, code coverage is poor :-(

    // Test strategy:
    // --- a. connect to all ICC supporting T=1 and call method-under-test
    // --- b. disconnect and call method-under-test

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1 and call method-under-test
      for (final String name : IFD_T1) {
        final Icc dut = new Icc(new Ifd(ifdCollection, name), T_1); // NOPMD new in loop

        assertTrue(dut.isValid());

        // --- b. disconnect and checkState
        dut.disconnect(true);

        assertFalse(dut.isValid());
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Icc#toString()}. */
  @Test
  void test_toString() {
    LOGGER.atTrace().log("test_toString");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: This simple method does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. connect to all ICC supporting T=1 and call method-under-test
    // --- b. disconnect and call method-under-test

    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. connect to all ICC supporting T=1 and call method-under-test
      for (final String name : IFD_T1) {
        final Ifd ifd = new Ifd(ifdCollection, name); // NOPMD new in loop
        final Icc dut = new Icc(ifd, T_1); // NOPMD new in loop

        assertEquals(
            "PC/SC ICC in " + ifd.getName() + ", protocol " + T_1 + ", state OK", dut.toString());

        // --- b. disconnect and checkState
        dut.disconnect(true);
        assertEquals(
            "PC/SC ICC in " + ifd.getName() + ", protocol " + T_1 + ", state DISCONNECTED",
            dut.toString());

        assertFalse(dut.isValid());
      } // end For (name...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
