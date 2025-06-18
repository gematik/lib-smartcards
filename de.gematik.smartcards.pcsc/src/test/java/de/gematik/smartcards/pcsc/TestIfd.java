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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.pcsc.lib.ScardReaderState;
import de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset;
import de.gematik.smartcards.sdcom.isoiec7816objects.EafiIccProtocol;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link Ifd}.
 *
 * <p><b>Assertions:</b>
 *
 * <ol>
 *   <li>At least two readers with a smart card are available.
 *   <li>At least one reader without a smart card is available.
 *   <li>At least one ICC which supports "T=1" only.
 *   <li>At least one ICC which does not support "T=1".
 *   <li>No IFD is disconnected during test.
 *   <li>No ICC is inserted or removed during test.
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "DLS_DEAD_LOCAL_STORE"
//         Short message: This instruction assigns a value to a local variable,
//             but the value is not read or used in any subsequent instruction.
//         Rational: This is a "false positive".
// Note 2: Spotbugs claims "ES_COMPARING_STRINGS_WITH_EQ" i.e.,
//         Short message: Comparison of String parameter using == or !=
//         Rational: That finding is correct, but intentionally assertSame(...) is used.
// Note 3: Spotbugs claims "NP_NULL_PARAM_DEREF"
//         Short message: Null passed for non-null parameter.
//         Rational: The finding is correct, "ScardReaderState.szReader" is
//             defined as "@Nullable" but parameter "name" in the constructor
//             is defined a "@NonNullable".
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "DLS_DEAD_LOCAL_STORE", // see note 1
  "ES_COMPARING_STRINGS_WITH_EQ", // see note 2
  "NP_NULL_PARAM_DEREF" // note 3
}) // */
@EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isSmartCardResourceManagerRunning")
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.AvoidDuplicateLiterals",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestIfd {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestIfd.class); // */

  /** Random number generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Execution times for immediate return. */
  private static final List<Long> RUNTIME_IMMEDIATE = new ArrayList<>();

  /** Execution times for timeout-return. */
  private static final List<Long> RUNTIME_TIMEOUT = new ArrayList<>();

  /** Waiting time in milliseconds in case an immediate return is expected. */
  private static final int IMMEDIATE = 10_000; // 10 seconds */

  /**
   * Waiting time in milliseconds in case no return is expected.
   *
   * <p>This value should not be too low, so the runtime can be distinguished from an immediate
   * return.
   *
   * <p>This value should not be too high to avoid long test times.
   */
  private static final int TIMEOUT = 1_000; // 1 second */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. minimum amount of IFD present
    try (IfdCollection ifdCollection = new IfdCollection()) {
      final List<String> readerNames = ifdCollection.scardListReaders();
      final ScardReaderState[] readers = ScardReaderState.createArray(readerNames, false);
      final long startTime = System.nanoTime();
      assertEquals(
          SCARD_S_SUCCESS,
          ifdCollection.scardGetStatusChange(
              IMMEDIATE, // timeout
              readers));
      RUNTIME_IMMEDIATE.add(System.nanoTime() - startTime);
      int noIccT0 = 0;
      int noIccT1 = 0;
      int noIccAbsent = 0;
      for (final ScardReaderState srs : readers) {
        final int state = srs.dwEventState.intValue();

        if (SCARD_STATE_PRESENT == (state & SCARD_STATE_PRESENT)) {
          // ... ICC present
          final var osAtr = Arrays.copyOfRange(srs.rgbAtr, 0, srs.cbAtr.intValue());
          final var atr = new AnswerToReset(osAtr); // NOPMD new in loop
          final EnumSet<EafiIccProtocol> protocols = atr.getSupportedProtocols(); // NOPMD use Set

          if (protocols.contains(EafiIccProtocol.T0)) {
            noIccT0++;
          } else if (protocols.contains(EafiIccProtocol.T1)) {
            noIccT1++;
          } // end elif
        } else {
          // ... ICC absent
          noIccAbsent++;
        } // end else
      } // end For (srs...)

      assertTrue(noIccT0 > 0, String.format("Number of ICC with T=0: %d.", noIccT0));
      assertTrue(noIccT1 > 0, String.format("Number of ICC with T=1: %d.", noIccT1));
      assertTrue(noIccAbsent > 0, String.format("Number of IFD w/o ICC: %d.", noIccAbsent));
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Method executed after other tests. */
  @AfterAll
  static void tearDownAfterClass() {
    // Test strategy:
    // --- a. observe runtime if immediate return is expected
    // --- b. observe runtime if waiting is expected

    // --- a. observe runtime if immediate return is expected
    {
      if (RUNTIME_IMMEDIATE.isEmpty()) {
        return;
      } // end fi
      // ... at least one value in RUNTIME_IMMEDIATE
      //     => show statistics

      final long min = RUNTIME_IMMEDIATE.stream().mapToLong(t -> t).min().orElseThrow();

      final long max = RUNTIME_IMMEDIATE.stream().mapToLong(t -> t).max().orElseThrow();
      final double noValues = RUNTIME_IMMEDIATE.size();
      final long mean = Math.round(RUNTIME_IMMEDIATE.stream().mapToLong(t -> t).sum() / noValues);
      LOGGER.atInfo().log(
          "RuntimeImmediate: #={}, min={}, mean={}, max={}",
          RUNTIME_IMMEDIATE.size(),
          AfiUtils.nanoSeconds2Time(min),
          AfiUtils.nanoSeconds2Time(mean),
          AfiUtils.nanoSeconds2Time(max));

      // Observed times:
      // - gnbe1058:
      //   RuntimeImmediate: #=8, min=2,332600 ms, mean= 5,254975 ms, max= 16,665800 ms
      //   RuntimeImmediate: #=8, min=5,665700 ms, mean=38,058413 ms, max=206,425100 ms
      //   RuntimeImmediate: #=8, min=4,086400 ms, mean=41,018613 ms, max=250,988200 ms
      // - sabine@kubuntu:
      // - sabine@Windows:
      // 50,000,000 ns = 50,000 us = 50 ms
      final long meanRuntime = 50_000_000; // spotbugs: DLS_DEAD_LOCAL_STORE
      assertTrue(min >= 0, "min >= 0");
      assertTrue(
          mean < meanRuntime,
          () ->
              String.format(
                  "mean = %s >= %s",
                  AfiUtils.nanoSeconds2Time(mean), AfiUtils.nanoSeconds2Time(meanRuntime)));
    } // end --- a.

    // --- b. observe runtime if waiting is expected
    {
      if (RUNTIME_TIMEOUT.isEmpty()) {
        return;
      } // end fi
      // ... at least one value in RUNTIME_IMMEDIATE
      //     => show statistics

      final long min = RUNTIME_TIMEOUT.stream().mapToLong(t -> t).min().orElseThrow();

      final long max = RUNTIME_TIMEOUT.stream().mapToLong(t -> t).max().orElseThrow();
      final double noValues = RUNTIME_TIMEOUT.size();
      final long mean = Math.round(RUNTIME_TIMEOUT.stream().mapToLong(t -> t).sum() / noValues);
      LOGGER.atInfo().log(
          "RuntimeTimeout: #={}, min={}, mean={}, max={}",
          RUNTIME_TIMEOUT.size(),
          AfiUtils.nanoSeconds2Time(min),
          AfiUtils.nanoSeconds2Time(mean),
          AfiUtils.nanoSeconds2Time(max));

      // Observed times:
      // - gnbe1058
      //   RuntimeTimeout: #=3, min= 1,011", mean= 1,012", max= 1,013"
      // - sabine@kubuntu:
      // - sabine@Windows:
      // 1,100,000,000 ns = 1,100,000 us = 1,100 ms = 1.1 s
      final long meanRuntime = 1_100_000_000; // spotbugs: DLS_DEAD_LOCAL_STORE
      assertTrue(min >= TIMEOUT * 1e-6, "min >= TIMEOUT");
      assertTrue(
          mean < meanRuntime,
          () ->
              String.format(
                  "mean = %s >= %s",
                  AfiUtils.nanoSeconds2Time(mean), AfiUtils.nanoSeconds2Time(meanRuntime)));
    } // end --- b.
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

  /** Test method for {@link Ifd#Ifd(IfdCollection, String)}. */
  @Test
  void test_Ifd__IfdCollection_String() {
    LOGGER.atTrace().log("test_Ifd__IfdCollection_String");
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over randomly generated strings
    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. smoke test
      {
        final String name = "fooBar42";
        final Ifd dut = new Ifd(ifdCollection, name);

        assertSame(ifdCollection, dut.getIfdCollection());
        assertSame(name, dut.getName());
      } // end --- a.

      // --- b. loop over randomly generated strings
      IntStream.range(0, 10)
          .forEach(
              i -> {
                final String name = RNG.nextUtf8(1, 50);
                final Ifd dut = new Ifd(ifdCollection, name);

                assertSame(ifdCollection, dut.getIfdCollection());
                assertSame(name, dut.getName());
              }); // end forEach(i...)
      // end --- b.
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Ifd#getIfdCollection()}. */
  @Test
  void test_getIfdCollection() {
    LOGGER.atTrace().log("test_getIfdCollection");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: This simple getter does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. smoke test
      {
        final String name = "fooBar42.getIfdCollection";
        final Ifd dut = new Ifd(ifdCollection, name);

        assertSame(ifdCollection, dut.getIfdCollection());
      } // end --- a.
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Ifd#getName()}. */
  @Test
  void test_getName() {
    LOGGER.atTrace().log("test_Ifd__IfdCollection_String");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    try (IfdCollection ifdCollection = new IfdCollection()) {
      // --- a. smoke test
      {
        final String name = "fooBar42";
        final Ifd dut = new Ifd(ifdCollection, name);

        assertSame(name, dut.getName());
      } // end --- a.
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Ifd#connect(String)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_connect() {
    LOGGER.atTrace().log("test_connect");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. loop over all available readers
    // --- b. ICC present: connect 1st time
    // --- c. ICC present: connect 2nd time with same protocol
    // --- d. ICC present: connect 3rd time with "*" protocol
    // --- e. ICC present: ERROR: connect 4th time with different protocol
    // --- f. ICC present: connect 5th time after ICC disconnected (i.e. ICC is invalid)
    // --- g. ICC present: ERROR: connect with unsupported protocol
    // --- h. ERROR: readers with ICC absent

    final String protocolA = "T=1";
    final String protocolB = "T=2";

    try (IfdCollection ifdCollection = new IfdCollection()) {
      final List<String> readerNames = ifdCollection.scardListReaders();
      final ScardReaderState[] readers = ScardReaderState.createArray(readerNames, false);
      final long startTime = System.nanoTime();
      assertEquals(
          SCARD_S_SUCCESS,
          ifdCollection.scardGetStatusChange(
              IMMEDIATE, // timeout
              readers));
      RUNTIME_IMMEDIATE.add(System.nanoTime() - startTime);

      // --- a. loop over all available readers
      for (final ScardReaderState srs : readers) {
        final int state = srs.dwEventState.intValue();
        final CardTerminal dut = new Ifd(ifdCollection, srs.szReader); // NP_NULL_PARAM_DEREF

        if (SCARD_STATE_PRESENT == (state & SCARD_STATE_PRESENT)) {
          // ... ICC present
          final var osAtr = Arrays.copyOfRange(srs.rgbAtr, 0, srs.cbAtr.intValue());
          final var atr = new AnswerToReset(osAtr);
          final EnumSet<EafiIccProtocol> protocols = atr.getSupportedProtocols(); // NOPMD use Set

          if (protocols.contains(EafiIccProtocol.T0)) {
            // ... unsupported protocol
            // --- g. ICC present: ERROR: connect with unsupported protocol
            assertThrows(CardException.class, () -> dut.connect(protocolA));
          } else if (protocols.contains(EafiIccProtocol.T1)) {
            // ... supported protocol
            // --- b. ICC present: connect 1st time
            final Card card1 = dut.connect(protocolA);

            assertNotNull(card1);

            // --- c. ICC present: connect 2nd time with same protocol
            final Card card2 = dut.connect(protocolA);

            assertSame(card1, card2);

            // --- d. ICC present: connect 3rd time with "*" protocol
            final Card card3 = dut.connect("*");

            assertSame(card1, card3);
            assertSame(card2, card3);

            // --- e. ICC present: ERROR: connect 4th time with different protocol
            assertThrows(CardException.class, () -> dut.connect(protocolB));

            // --- f. ICC present: connect 5th time after ICC disconnected (i.e. ICC is invalid)
            card3.disconnect(true);
            card2.disconnect(true);
            card1.disconnect(true);
            final Card card5 = dut.connect(protocolA);

            assertSame(card1, card3);
            assertSame(card2, card3);
            assertNotSame(card1, card5);
            card5.disconnect(true);
          } // end elif
        } else {
          // ... ICC absent
          // --- h. ERROR: readers with ICC absent
          assertThrows(CardException.class, () -> dut.connect(protocolA));
        } // end else
      } // end For (srs...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Ifd#isCardPresent()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_isCardPresent() {
    LOGGER.atTrace().log("test_isCardPresent");
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getName()-method works as expected

    // Test strategy:
    // --- a. loop over all available readers
    try (IfdCollection ifdCollection = new IfdCollection()) {
      final List<String> readerNames = ifdCollection.scardListReaders();
      final ScardReaderState[] readers = ScardReaderState.createArray(readerNames, false);
      final long startTime = System.nanoTime();
      assertEquals(
          SCARD_S_SUCCESS,
          ifdCollection.scardGetStatusChange(
              IMMEDIATE, // timeout
              readers));
      RUNTIME_IMMEDIATE.add(System.nanoTime() - startTime);

      // --- a. loop over all available readers
      for (final ScardReaderState srs : readers) {
        final int state = srs.dwEventState.intValue();
        final CardTerminal dut = new Ifd(ifdCollection, srs.szReader); // NP_NULL_PARAM_DEREF

        if (SCARD_STATE_PRESENT == (state & SCARD_STATE_PRESENT)) {
          // ... ICC present
          assertTrue(dut.isCardPresent());
        } else {
          // ... ICC absent
          assertFalse(dut.isCardPresent());
        } // end else
      } // end For (srs...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Ifd#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: This simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- a. smoke test
    try (IfdCollection ifdCollection = new IfdCollection()) {
      final String name = "fooBar.toString";
      final CardTerminal dut = new Ifd(ifdCollection, name);

      assertEquals("PC/SC IFD " + name, dut.toString());
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Ifd#waitForCardPresent(long)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_waitForCardPresent__long() {
    LOGGER.atTrace().log("waitForCardPresent");
    // Assertions:
    // ... a. the list of connected readers does not change during test
    // ... b. constructor(s) work as expected

    // Note 1: The behavior of the underlying methods should be tested elsewhere.
    //         Thus, here we do no more than smoke tests.
    // Note 2: For good code coverage dynamic change of connected readers-list
    //         during tests is necessary. According to assertion a (see above)
    //         this is not the case here. Thus, code coverage is poor :-(

    // Test strategy:
    // --- a. loop over all available readers and wait for change
    // --- b. waiting although card is already present
    // --- c. waiting for a card that never comes
    // --- d. ERROR: negative timeout

    // --- a. loop over all available readers and wait for change
    try (IfdCollection ifdCollection = new IfdCollection()) {
      final List<String> readerNames = ifdCollection.scardListReaders();
      final ScardReaderState[] readers = ScardReaderState.createArray(readerNames, false);
      long startTime = System.nanoTime();
      assertEquals(
          SCARD_S_SUCCESS,
          ifdCollection.scardGetStatusChange(
              IMMEDIATE, // timeout
              readers));
      RUNTIME_IMMEDIATE.add(System.nanoTime() - startTime);

      // --- a. loop over all available readers
      for (final ScardReaderState srs : readers) {
        final int state = srs.dwEventState.intValue();
        final CardTerminal dut = new Ifd(ifdCollection, srs.szReader); // NP_NULL_PARAM_DEREF

        // --- d. ERROR: negative timeout
        assertThrows(IllegalArgumentException.class, () -> dut.waitForCardPresent(-1));

        if (SCARD_STATE_PRESENT == (state & SCARD_STATE_PRESENT)) {
          // ... ICC present
          // --- b. waiting although card is present
          startTime = System.nanoTime();
          final boolean present = dut.waitForCardPresent(IMMEDIATE);
          RUNTIME_IMMEDIATE.add(System.nanoTime() - startTime);

          assertTrue(present, () -> dut + " waitForCardPresent");
        } else {
          // ... ICC absent
          // --- c. waiting for a card that never comes
          startTime = System.nanoTime();
          final boolean present = dut.waitForCardPresent(TIMEOUT);
          RUNTIME_TIMEOUT.add(System.nanoTime() - startTime);

          assertFalse(present, () -> dut + " waitForCardPresent");
        } // end else
      } // end For (srs...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link Ifd#waitForCardAbsent(long)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_waitForCardAbsent__long() {
    LOGGER.atTrace().log("waitForCardAbsent");
    // Assertions:
    // ... a. list of connected readers does not change during test
    // ... b. constructor(s) work as expected

    // Note 1: The behaviour of the underlying methods should be tested elsewhere.
    //         Thus, here we do no more than smoke tests.
    // Note 2: For good code coverage dynamic change of connected readers-list
    //         during tests is necessary. According to assertion a (see above)
    //         this is not the case here. Thus, code coverage is poor :-(

    // Test strategy:
    // --- a. loop over all available readers and wait for change
    // --- b. waiting for card removal that never occurs is present
    // --- c. waiting although already absent
    // --- d. ERROR: negative timeout

    // --- a. loop over all available readers and wait for change
    try (IfdCollection ifdCollection = new IfdCollection()) {
      final List<String> readerNames = ifdCollection.scardListReaders();
      final ScardReaderState[] readers = ScardReaderState.createArray(readerNames, false);
      long startTime = System.nanoTime();
      assertEquals(
          SCARD_S_SUCCESS,
          ifdCollection.scardGetStatusChange(
              IMMEDIATE, // timeout
              readers));
      RUNTIME_IMMEDIATE.add(System.nanoTime() - startTime);

      // --- a. loop over all available readers
      for (final ScardReaderState srs : readers) {
        final int state = srs.dwEventState.intValue();
        final CardTerminal dut = new Ifd(ifdCollection, srs.szReader); // NP_NULL_PARAM_DEREF

        // --- d. ERROR: negative timeout
        assertThrows(IllegalArgumentException.class, () -> dut.waitForCardAbsent(-1));

        if (SCARD_STATE_PRESENT == (state & SCARD_STATE_PRESENT)) {
          // ... ICC present
          // --- b. waiting for card removal that never occurs is present
          startTime = System.nanoTime();
          final boolean present = dut.waitForCardAbsent(TIMEOUT);
          RUNTIME_TIMEOUT.add(System.nanoTime() - startTime);

          assertFalse(present, () -> dut + " waitForCardAbsent");
        } else {
          // ... ICC absent
          // --- c. waiting although already absent
          startTime = System.nanoTime();
          final boolean present = dut.waitForCardAbsent(TIMEOUT);
          RUNTIME_IMMEDIATE.add(System.nanoTime() - startTime);

          assertTrue(present, () -> dut + " waitForCardAbsent");
        } // end else
      } // end For (srs...)
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
