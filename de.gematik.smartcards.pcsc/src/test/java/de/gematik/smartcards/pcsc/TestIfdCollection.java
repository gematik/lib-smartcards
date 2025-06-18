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

import static de.gematik.smartcards.pcsc.constants.PcscStatus.ERROR_INVALID_HANDLE;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.OS_WINDOWS;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_INVALID_HANDLE;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_NO_SMARTCARD;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_PROTO_MISMATCH;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_TIMEOUT;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_UNKNOWN_READER;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_S_SUCCESS;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_W_REMOVED_CARD;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_PROTOCOL_T0;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_PROTOCOL_T1;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SCOPE_SYSTEM;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SCOPE_TERMINAL;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SCOPE_USER;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SHARE_EXCLUSIVE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SHARE_SHARED;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_EMPTY;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_PRESENT;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_UNPOWER_CARD;
import static javax.smartcardio.CardTerminals.State.ALL;
import static javax.smartcardio.CardTerminals.State.CARD_ABSENT;
import static javax.smartcardio.CardTerminals.State.CARD_INSERTION;
import static javax.smartcardio.CardTerminals.State.CARD_PRESENT;
import static javax.smartcardio.CardTerminals.State.CARD_REMOVAL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.pcsc.constants.PcscStatus;
import de.gematik.smartcards.pcsc.lib.DwordByReference;
import de.gematik.smartcards.pcsc.lib.ScardHandleByReference;
import de.gematik.smartcards.pcsc.lib.ScardReaderState;
import de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset;
import de.gematik.smartcards.sdcom.isoiec7816objects.EafiIccProtocol;
import de.gematik.smartcards.utils.AfiUtils;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link IfdCollection}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isSmartCardResourceManagerRunning")
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestIfdCollection {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestIfdCollection.class); // */

  /*
   * Random number generator.
   *
  private static final AfiRng RNG = new AfiRng(); // */

  /*
   * Default ATR array.
   *
  private static final byte[] DEFAULT_ATR_ARRAY = new byte[AnswerToReset.MAX_ATR_SIZE]; // */

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

  /*
   * Test method for {@link IfdCollection#createScardReaderStates(List, ScardReaderState[])}.
   *
  @Test
  void test_createScardReaderStates__List_ScardReaderStateA() {
    LOGGER.atTrace().log("test_createScardReaderStates__List_ScardReaderStateA");
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test with empty list and empty array
    // --- b. readerNames empty, oldReaders with random elements
    // --- c. disjunctive reader-names in readerNames and oldReaders
    // --- d. equal reader-names in readerNames and oldReaders

    // --- a. smoke test with empty list and empty array
    {
      final List<String> readerNames = Collections.emptyList();
      final ScardReaderState[] oldReaders = new ScardReaderState[0];

      final ScardReaderState[] dut = IfdCollection.createScardReaderStates(readerNames, oldReaders);

      assertEquals(1, dut.length);
      final ScardReaderState pnp = dut[0];
      assertEquals(
          PNP_READER_ID,
          pnp.szReader
      );
      // intentionally instance attribute pvUserData not checked (I don't know how)
      assertEquals(0L, pnp.dwCurrentState.longValue());
      assertEquals(0L, pnp.dwEventState.longValue());
      assertEquals(0L, pnp.cbAtr.longValue());
      assertArrayEquals(DEFAULT_ATR_ARRAY, pnp.rgbAtr);
    } // end --- a.

    // --- b. readerNames empty, oldReaders with random elements
    {
      final List<String> readerNames = Collections.emptyList();
      RNG.intsClosed(1, 20, 5).forEach(lengthOldReader -> {
        final ScardReaderState[] oldReaders = new ScardReaderState[lengthOldReader];
        for (int i = oldReaders.length; i-- > 0;) { // NOPMD assignment in operand
          oldReaders[i] = rndScardReaderState(RNG.nextInt(1, 128));
        } // end For (i...)

        final ScardReaderState[] dut = IfdCollection.createScardReaderStates(
            readerNames,
            oldReaders
        );

        assertEquals(1, dut.length);
        final ScardReaderState pnp = dut[0];
        assertEquals(PNP_READER_ID, pnp.szReader);
        // intentionally instance attribute pvUserData not checked (I don't know how)
        assertEquals(0L, pnp.dwCurrentState.longValue());
        assertEquals(0L, pnp.dwEventState.longValue());
        assertEquals(0L, pnp.cbAtr.longValue());
        assertArrayEquals(DEFAULT_ATR_ARRAY, pnp.rgbAtr);
      }); // end forEach(lengthOldReader -> ...)
    } // end --- b.

    // --- c. disjunctive reader-names in readerNames and oldReaders
    RNG.intsClosed(1, 20, 10).forEach(noReaderNames -> {
      final List<String> readerNames = IntStream.rangeClosed(1, noReaderNames)
          .mapToObj(RNG::nextUtf8) // readerName.length() == i
          .toList(); // list with strings, ith-element has length of i
      RNG.intsClosed(1, 20, 5).forEach(lengthOldReader -> {
        final ScardReaderState[] oldReaders = new ScardReaderState[lengthOldReader];
        for (int i = oldReaders.length; i-- > 0;) { // NOPMD assignment in operand
          // random ScardReaderState with a readerName longer than any name in readerName
          oldReaders[i] = rndScardReaderState(noReaderNames + RNG.nextInt(1, 128));
        } // end For (i...)

        final ScardReaderState[] dut = IfdCollection.createScardReaderStates(
            readerNames,
            oldReaders
        );

        assertEquals(1 + readerNames.size(), dut.length);

        // check first element
        final ScardReaderState pnp = dut[0];
        assertEquals(PNP_READER_ID, pnp.szReader);
        // intentionally instance attribute pvUserData not checked (I don't know how)
        assertEquals(0L, pnp.dwCurrentState.longValue());
        assertEquals(0L, pnp.dwEventState.longValue());
        assertEquals(0L, pnp.cbAtr.longValue());
        assertArrayEquals(DEFAULT_ATR_ARRAY, pnp.rgbAtr);

        //  check all other elements
        for (int i = dut.length; i-- > 1;) { // NOPMD assignment in operand
          final ScardReaderState srs = dut[i];

          assertEquals(readerNames.get(i - 1), srs.szReader);
          // intentionally instance attribute pvUserData not checked (I don't know how)
          assertEquals(0L, srs.dwCurrentState.longValue());
          assertEquals(0L, srs.dwEventState.longValue());
          assertEquals(0L, srs.cbAtr.longValue());
          assertArrayEquals(DEFAULT_ATR_ARRAY, srs.rgbAtr);
        } // end For (i...)
      }); // end forEach(lengthOldReader -> ...)
    }); // end forEach(noReaderNames -> ...)
    // end --- c.

    // --- d. equal reader-names in readerNames and oldReaders
    RNG.intsClosed(1, 60, 30).forEach(noReaderNames -> {
      // set readerNames
      final List<String> readerNames = new ArrayList<>();
      for (int i = noReaderNames; i-- > 0;) { // NOPMD assignment in operand
        String readerName;
        do {
          readerName = Hex.toHexDigits(RNG.nextBytes(2));
        } while (readerNames.contains(readerName));
        readerNames.add(readerName);
      } // end For (i...)

      RNG.intsClosed(1, 60, 30).forEach(lengthOldReader -> {
        // get a list with unique oldReaderNames
        final List<String> oldReaderNames = new ArrayList<>();
        for (int i = lengthOldReader; i-- > 0;) { // NOPMD assignment in operand
          String readerName;
          do {
            readerName = Hex.toHexDigits(RNG.nextBytes(2));
          } while (readerNames.contains(readerName) || oldReaderNames.contains(readerName));
          oldReaderNames.add(readerName);
        } // end For (i...)

        // check that all reader-names are unique (better safe than sorry)
        {
          final Set<String> union = new HashSet<>();
          union.addAll(readerNames);
          union.addAll(oldReaderNames);
          assertEquals(noReaderNames + lengthOldReader, union.size());
        }

        // get a random subset of indices from readerNames
        final int noNamesToCopy = RNG.nextIntClosed(1, readerNames.size());
        final Set<Integer> copyIndices = new HashSet<>();
        while (copyIndices.size() < noNamesToCopy) {
          copyIndices.add(RNG.nextIntClosed(0, noReaderNames - 1));
        } // end While (...)

        // insert reader-names from readerNames into random positions in oldReaderNames
        copyIndices.forEach(index -> {
          final int position = RNG.nextIntClosed(0, oldReaderNames.size());
          oldReaderNames.add(position, readerNames.get(index));
        }); // end forEach(index -> ...)
        assertEquals(
            lengthOldReader + copyIndices.size(),
            new HashSet<>(oldReaderNames).size()
        );

        // create for each reader-name in oldReaderNames an ScardReaderState
        final Map<String, ScardReaderState> mapping = oldReaderNames.stream()
            .map(name -> Map.entry(name, rndScardReaderState(name)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // create array with oldReaders
        final ScardReaderState[] oldReaders = new ScardReaderState[oldReaderNames.size()];
        for (int i = oldReaderNames.size(); i-- > 0;) { // NOPMD assignment in operand
          final ScardReaderState srs = mapping.get(oldReaderNames.get(i));
          assertNotNull(srs);
          oldReaders[i] = srs;
        } // end For (i...)

        // perform the test
        final ScardReaderState[] dut = IfdCollection.createScardReaderStates(
            readerNames,
            oldReaders
        );

        assertEquals(1 + readerNames.size(), dut.length);

        // check first element
        final ScardReaderState pnp = dut[0];
        assertEquals(PNP_READER_ID, pnp.szReader);
        // intentionally instance attribute pvUserData not checked (I don't know how)
        assertEquals(0L, pnp.dwCurrentState.longValue());
        assertEquals(0L, pnp.dwEventState.longValue());
        assertEquals(0L, pnp.cbAtr.longValue());
        assertArrayEquals(DEFAULT_ATR_ARRAY, pnp.rgbAtr);

        //  check all other elements
        for (int i = dut.length; i-- > 1;) { // NOPMD assignment in operand
          final ScardReaderState srs = dut[i];
          final String readerName = readerNames.get(i - 1);

          assertEquals(readerName, srs.szReader);
          // intentionally instance attribute pvUserData not checked (I don't know how)

          if (mapping.containsKey(readerName)) {
            // ... we hava an old ScardReaderState
            final ScardReaderState old = mapping.get(readerName);
            assertEquals(old.szReader, srs.szReader);
            assertSame(old.pvUserData, srs.pvUserData);
            assertSame(old.dwCurrentState, srs.dwCurrentState);
            assertSame(old.dwEventState, srs.dwEventState);
            assertSame(old.cbAtr, srs.cbAtr);
            assertNotSame(old.rgbAtr, srs.rgbAtr);
            assertEquals(AnswerToReset.MAX_ATR_SIZE, srs.rgbAtr.length);
            assertEquals(
                Hex.toHexDigits(old.rgbAtr, 0, old.cbAtr.intValue()),
                Hex.toHexDigits(srs.rgbAtr, 0, srs.cbAtr.intValue())
            );
            final byte[] remaining = Arrays.copyOfRange(
                srs.rgbAtr,
                srs.cbAtr.intValue(),
                AnswerToReset.MAX_ATR_SIZE
            );
            assertArrayEquals(
                new byte[remaining.length], // NOPMD new in loop
                remaining
            );
          } else {
            // ... no old ScardReaderState
            assertEquals(0L, srs.dwCurrentState.longValue());
            assertEquals(0L, srs.dwEventState.longValue());
            assertEquals(0L, srs.cbAtr.longValue());
            assertArrayEquals(DEFAULT_ATR_ARRAY, srs.rgbAtr);
          } // end else
        } // end For (i...)
        // FIXME
      }); // end forEach(lengthOldReader -> ...)
    }); // end forEach(noReaderNames -> ...)
    // end --- d.
  } // end method */

  /**
   * Test method for {@link IfdCollection#IfdCollection()}.
   *
   * <p>Here the method-under-test (i.e. the default-constructor) is called as proposed by module
   * {@code java.smartcardio}:
   *
   * <ol>
   *   <li>use {@link AfiPcsc} provider
   *   <li>use {@link TerminalFactory#terminals()}
   * </ol>
   */
  @Test
  void test_IfdCollection__Provider() {
    LOGGER.atTrace().log("test_IfdCollection__Provider");
    // Assertions:
    // ... a. createScardReaderStates(List, ScardReaderState[])-method works as expected

    // Test strategy:
    // --- a. smoke test
    try {
      final TerminalFactory terminalFactory =
          TerminalFactory.getInstance(AfiPcsc.TYPE, null, new AfiPcsc());

      try (IfdCollection dut = (IfdCollection) terminalFactory.terminals()) {
        // check insLibrary: intentionally no tests

        // check insScardContext: intentionally no tests

        /*/ check insKnownReaders
        {
          final ScardReaderState[] tmp = dut.getKnownReaders();

          assertEquals(1, tmp.length);
          final ScardReaderState pnp = tmp[0];
          assertEquals(
              PNP_READER_ID,
              pnp.szReader
          );
          // intentionally instance attribute pvUserData not checked (I don't know how)
          assertEquals(0L, pnp.dwCurrentState.longValue());
          assertEquals(0L, pnp.dwEventState.longValue());
          assertEquals(0L, pnp.cbAtr.longValue());
          assertArrayEquals(DEFAULT_ATR_ARRAY, pnp.rgbAtr);
        } // end check insKnownReaders */

        // check insIsClosed
        assertFalse(dut.isClosed());
      } catch (PcscException e) {
        // ... exception from dut.close()-method thrown
        fail(AfiUtils.UNEXPECTED, e);
      } // end Catch (...)
    } catch (NoSuchAlgorithmException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.
  } // end method */

  /**
   * Test method for {@link IfdCollection#IfdCollection()}.
   *
   * <p>Here the public default constructor is used.
   */
  @Test
  void test_IfdCollection() {
    LOGGER.atTrace().log("test_IfdCollection");
    // Assertions:
    // ... a. underlying constructor(int) works as expected

    // Test strategy:
    // --- a. smoke test
    assertDoesNotThrow(
        () -> {
          try (IfdCollection dut = new IfdCollection()) {
            assertFalse(dut.isClosed());
          } // end try-with-resources
        });
  } // end method */

  /** Test method for {@link IfdCollection#IfdCollection(int)}. */
  @Test
  void test_IfdCollection__int() {
    LOGGER.atTrace().log("test_IfdCollection__int");
    // Assertions:
    // ... a. createScardReaderStates(List, ScardReaderState[])-method works as expected

    // Test strategy:
    // --- a. smoke test with  valid  scope-values
    // --- b. smoke test with invalid scope-values

    final int[] validScopes =
        OS_WINDOWS
            ? new int[] {SCARD_SCOPE_USER, SCARD_SCOPE_SYSTEM}
            : new int[] {SCARD_SCOPE_USER, SCARD_SCOPE_TERMINAL, SCARD_SCOPE_SYSTEM};

    final int[] invalidScopes = OS_WINDOWS ? new int[] {SCARD_SCOPE_TERMINAL} : new int[] {4};

    // --- a. smoke test with  valid  scope-values
    for (final int scope : validScopes) {
      try (IfdCollection dut = new IfdCollection(scope)) { // NOPMD new in loop
        // check insLibrary: intentionally no tests

        // check insScardContext: intentionally no tests

        /*/ check insKnownReaders
        {
          final ScardReaderState[] tmp = dut.getKnownReaders();

          assertEquals(1, tmp.length);
          final ScardReaderState pnp = tmp[0];
          assertEquals(
              PNP_READER_ID,
              pnp.szReader
          );
          // intentionally instance attribute pvUserData not checked (I don't know how)
          assertEquals(0L, pnp.dwCurrentState.longValue());
          assertEquals(0L, pnp.dwEventState.longValue());
          assertEquals(0L, pnp.cbAtr.longValue());
          assertArrayEquals(DEFAULT_ATR_ARRAY, pnp.rgbAtr);
        } // end check insKnownReaders */

        // check insIsClosed
        assertFalse(dut.isClosed());
      } catch (PcscException e) {
        // ... exception from dut.close()-method thrown
        fail(AfiUtils.UNEXPECTED, e);
      } // end Catch (...)
    } // end For (scope...)
    // end --- a.

    // --- b. smoke test with invalid scope-values
    for (final int scope : invalidScopes) {
      assertThrows(
          EstablishContextException.class,
          () -> {
            try (IfdCollection dut = new IfdCollection(scope)) { // NOPMD new in loop
              assertFalse(dut.isClosed());
            } // end try-with-resources
          });
    } // end For (scope...)
    // end --- b.
  } // end method */

  /**
   * Test method for {@link IfdCollection#list(CardTerminals.State)}.
   *
   * <p>This method assumes that at least one smart card reader is available.
   */
  @Test
  void test_list__State() {
    LOGGER.atTrace().log("test_list__State");
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. scardListReaders()-method works as expected (TODO test that method)
    // ... c. scardGetStatusChange(long, ScardReaderState[])-method works as expected

    // Test strategy:
    // --- a. ERROR: state == null
    // --- b. state == ALL
    // --- c. state == CARD_INSERTION
    // --- d. state == CARD_PRESENT
    // --- e. state == CARD_EMPTY
    // --- f. state == CARD_REMOVAL
    // FIXME implement more tests
    // especially with dynamic changes to
    // - list of reader,
    // - card insertion,
    // - card removal

    try (IfdCollection dut = new IfdCollection()) {
      final List<String> allReaders = dut.scardListReaders();
      assertFalse(
          allReaders.isEmpty(),
          "List with readerNames is empty, but at least one reader SHALL be available.");
      final ScardReaderState[] readers = ScardReaderState.createArray(allReaders, false);
      assertEquals(
          SCARD_S_SUCCESS,
          dut.scardGetStatusChange(
              0, // timeout
              readers));

      // --- a. ERROR: state == null
      {
        assertThrows(NullPointerException.class, () -> dut.list(null));
      } // end --- a.

      // --- b. state == ALL
      final List<CardTerminal> ifdsAll = dut.list(ALL);
      {
        assertEquals(allReaders, ifdsAll.stream().map(CardTerminal::getName).toList());

        ifdsAll.forEach(
            ifd -> assertSame(ifd, dut.getTerminal(ifd.getName()))); // end forEach(ifd -> ...)
      } // end --- b.

      // --- c. state == CARD_INSERTION
      assertThrows(IllegalArgumentException.class, () -> dut.list(CARD_INSERTION));

      // --- d. state == CARD_PRESENT
      {
        final List<CardTerminal> ifds = dut.list(CARD_PRESENT);

        for (final ScardReaderState srs : readers) {
          final String name = (null == srs.szReader) ? "" : srs.szReader;
          final Optional<CardTerminal> opIfd =
              ifds.stream().filter(ifd -> name.equals(ifd.getName())).findAny();

          if (opIfd.isPresent()) {
            // ... IFD present in "readers" and in "ifds"
            //     => srs.dwEventState expected to indicate SCARD_STATE_PRESENT
            assertEquals(
                SCARD_STATE_PRESENT,
                srs.dwEventState.intValue() & (SCARD_STATE_PRESENT | SCARD_STATE_EMPTY));
            // expect the same IFD everytime list(...)-method is called
            assertSame(
                ifdsAll.stream().filter(ifd -> name.equals(ifd.getName())).findAny().orElseThrow(),
                opIfd.get());
          } else {
            // ... IFD present in "readers" but absent in "ifds"
            //     => srs.dwEventState expected to indicate SCARD_STATE_EMPTY
            assertEquals(
                SCARD_STATE_EMPTY,
                srs.dwEventState.intValue() & (SCARD_STATE_PRESENT | SCARD_STATE_EMPTY));
          } // end else
        } // end For (srs...)
      } // end --- d.

      // --- e. state == CARD_EMPTY
      {
        final List<CardTerminal> ifds = dut.list(CARD_ABSENT);

        for (final ScardReaderState srs : readers) {
          final String name = (null == srs.szReader) ? "" : srs.szReader;
          final Optional<CardTerminal> opIfd =
              ifds.stream().filter(ifd -> name.equals(ifd.getName())).findAny();

          if (opIfd.isPresent()) {
            // ... IFD present in "readers" and in "ifds"
            //     => srs.dwEventState expected to indicate SCARD_STATE_EMPTY
            assertEquals(
                SCARD_STATE_EMPTY,
                srs.dwEventState.intValue() & (SCARD_STATE_PRESENT | SCARD_STATE_EMPTY));
            // expect the same IFD everytime list(...)-method is called
            assertSame(
                ifdsAll.stream().filter(ifd -> name.equals(ifd.getName())).findAny().orElseThrow(),
                opIfd.get());
          } else {
            // ... IFD present in "readers" but absent in "ifds"
            //     => srs.dwEventState expected to indicate SCARD_STATE_PRESENT
            assertEquals(
                SCARD_STATE_PRESENT,
                srs.dwEventState.intValue() & (SCARD_STATE_PRESENT | SCARD_STATE_EMPTY));
          } // end else
        } // end For (srs...)
      } // end --- d.

      // --- f. state == CARD_REMOVAL
      assertThrows(IllegalArgumentException.class, () -> dut.list(CARD_REMOVAL));
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IfdCollection#waitForChange(long)}. */
  @Test
  void test_waitForChange__long() {
    LOGGER.atTrace().log("test_waitForChange__long");
    // Assertions:
    // ... a. IfdCollection()-constructor works as expected

    // Note: Because the default-constructor is the only constructor publicly
    //       available, there is no need to test with other scoped for
    //       SCardEstablishContext.

    // Test strategy:
    // --- a. smoke test
    try (IfdCollection dut = new IfdCollection()) {
      assertThrows(PcscException.class, () -> dut.waitForChange(7));
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IfdCollection#scardListReaders()}. */
  @Test
  void test_scardListReaders() {
    // Assertions:
    // ... a. list of connected readers does not change during test
    // ... b. constructor(s) work as expected

    // Note 1: The behaviour of the underlying methods should be tested elsewhere.
    //         Thus, here we do no more than smoke tests.
    // Note 2: For good code coverage dynamic change of connected readers-list
    //         during tests is necessary. According to assertion a (see above)
    //         this is not the case here. Thus, code coverage is poor :-(

    // Test strategy:
    // --- a. smoke test
    try (IfdCollection dut = new IfdCollection()) {
      // get list of attached readers from default library
      final Set<String> expected =
          TerminalFactory.getDefault().terminals().list().stream()
              .map(CardTerminal::getName)
              .collect(Collectors.toSet());

      // get list of attached readers from method-under-test
      final Set<String> present = Set.copyOf(dut.scardListReaders());

      assertEquals(expected, present);
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IfdCollection#scardGetStatusChange(long, ScardReaderState...)}. */
  @Test
  void test_scardGetStatusChange__long_ScardReaderState() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. scardListReaders()-method works as expected (TODO test that method)

    // Note: The behaviour of the underlying method should be tested elsewhere.
    //       Thus, here we do no more than smoke tests.

    // Test strategy:
    // --- a. smoke test with SCARD_STATE_UNAWARE
    // --- b. smoke test without changes (should cause a timeout)
    try (IfdCollection dut = new IfdCollection()) {
      final List<String> allReaders = dut.scardListReaders();
      assertFalse(
          allReaders.isEmpty(),
          "List with readerNames is empty, but at least one reader SHALL be available.");
      final ScardReaderState[] readers = ScardReaderState.createArray(allReaders, false);
      final int timeout;

      // --- a. smoke test with SCARD_STATE_UNAWARE
      timeout = 10_000; // 10 s = 10_000 ms
      long startTime = System.nanoTime();
      assertEquals(
          SCARD_S_SUCCESS,
          dut.scardGetStatusChange(
              timeout, // [ms]
              readers));
      double runTime = (System.nanoTime() - startTime) * 1e-6; // runTime in milliseconds

      assertTrue(runTime < 500, String.format("runtime = %f ms not below 500 ms", runTime));

      // --- b. smoke test without changes (should cause a timeout)
      for (final ScardReaderState srs : readers) {
        srs.dwCurrentState = srs.dwEventState;
      } // end For (srs...)

      startTime = System.nanoTime();
      assertEquals(
          SCARD_E_TIMEOUT,
          dut.scardGetStatusChange(
              timeout, // [ms]
              readers));
      runTime = (System.nanoTime() - startTime) * 1e-6; // runTime in milliseconds

      assertTrue(
          runTime >= 0.99 * timeout, // allow 1% measuring discrepancy
          String.format("runtime = %f ms not >= %d ms", runTime, timeout));
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test method for {@link IfdCollection#scardConnect(String, int, int, ScardHandleByReference,
   * DwordByReference)}.
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  @Test
  void test_scardConnect__String_int_int_ScardHandleByReference_DwordByReference() {
    LOGGER
        .atTrace()
        .log("test_scardConnect__String_int_int_ScardHandleByReference_DwordByReference");
    // Assertions:
    // ... a. IfdCollection()-constructor works as expected

    // Note: Because the default-constructor is the only constructor publicly
    //       available, there is no need to test with other scoped for
    //       SCardEstablishContext.

    // Test strategy:
    // --- a. loop over all connected readers
    // --- b. loop over all useful values for SHARE_MODE
    // --- c. loop over all useful values for preferredProtocols
    // --- d. ERROR: reader -> SCARD_STATE_EMPTY
    // --- e. reader -> SCARD_STATE_PRESENT, observe code
    // --- f. power down the smart card
    // --- g. ERROR: unknown reader

    final int[] shareModes =
        new int[] {
          SCARD_SHARE_EXCLUSIVE, SCARD_SHARE_SHARED,
          // SCARD_SHARE_DIRECT,
        };
    final int[] protocols =
        new int[] {
          // SCARD_PROTOCOL_UNDEFINED,
          SCARD_PROTOCOL_T0, SCARD_PROTOCOL_T1, SCARD_PROTOCOL_T0 + SCARD_PROTOCOL_T1,
          // SCARD_PROTOCOL_RAW
        };
    final ScardHandleByReference phCard = new ScardHandleByReference();
    final DwordByReference pdwActiveProtocol = new DwordByReference();

    try (IfdCollection dut = new IfdCollection()) {
      final List<String> allReaders = dut.scardListReaders();
      assertFalse(
          allReaders.isEmpty(),
          "List with readerNames is empty, but at least one reader SHALL be available.");
      final ScardReaderState[] readers = ScardReaderState.createArray(allReaders, false);
      assertEquals(
          SCARD_S_SUCCESS,
          dut.scardGetStatusChange(
              0, // timeout
              readers));

      // --- a. loop over all connected readers
      for (final ScardReaderState reader : readers) {
        final String name = reader.szReader;
        final boolean isEmpty =
            SCARD_STATE_EMPTY == (reader.dwEventState.intValue() & SCARD_STATE_EMPTY);

        // --- b. loop over all useful values for SHARE_MODE
        for (final int shareMode : shareModes) {
          // --- c. loop over all useful values for preferredProtocols
          for (final int preferredProtocol : protocols) {
            // spotless:off
            final var preferred = switch (preferredProtocol) {
              case SCARD_PROTOCOL_T0 -> EnumSet.of(EafiIccProtocol.T0);
              case SCARD_PROTOCOL_T1 -> EnumSet.of(EafiIccProtocol.T1);
              case 3 -> EnumSet.of(EafiIccProtocol.T0, EafiIccProtocol.T1);
              default -> EnumSet.noneOf(EafiIccProtocol.class);
            };
            // spotless:on
            assertFalse(preferred.isEmpty());

            final int codeConnect =
                dut.scardConnect(name, shareMode, preferredProtocol, phCard, pdwActiveProtocol);

            if (isEmpty) {
              // ... no smart card in reader
              // --- d. ERROR: reader -> SCARD_STATE_EMPTY
              assertEquals(
                  OS_WINDOWS ? SCARD_W_REMOVED_CARD : SCARD_E_NO_SMARTCARD,
                  codeConnect,
                  PcscStatus.getExplanation(codeConnect));
              assertEquals(
                  OS_WINDOWS ? ERROR_INVALID_HANDLE : SCARD_E_INVALID_HANDLE,
                  dut.getLibrary().scardDisconnect(phCard.getValue(), SCARD_UNPOWER_CARD));
            } else {
              // ... smart card present in reader
              // --- e. reader -> SCARD_STATE_PRESENT, observe code
              final var osAtr = Arrays.copyOfRange(reader.rgbAtr, 0, reader.cbAtr.intValue());
              final var atr = new AnswerToReset(osAtr); // NOPMD new in loop
              final var intersection = EnumSet.copyOf(preferred);
              intersection.retainAll(atr.getSupportedProtocols());

              if (intersection.isEmpty()) {
                // ... no common protocol
                assertEquals(
                    SCARD_E_PROTO_MISMATCH, codeConnect, PcscStatus.getExplanation(codeConnect));
                assertEquals(
                    OS_WINDOWS ? ERROR_INVALID_HANDLE : SCARD_E_INVALID_HANDLE,
                    dut.getLibrary().scardDisconnect(phCard.getValue(), SCARD_UNPOWER_CARD));
              } else {
                // ... common protocol
                assertEquals(SCARD_S_SUCCESS, codeConnect, PcscStatus.getExplanation(codeConnect));

                // --- f. power down the smart card
                assertEquals(
                    SCARD_S_SUCCESS,
                    dut.getLibrary().scardDisconnect(phCard.getValue(), SCARD_UNPOWER_CARD));
              } // end else (common protocol?)
            } // end else (ICC present?)
          } // end For (preferredProtocol...)
        } // end For (shareMode...)
      } // end For (reader...)

      // --- g. ERROR: unknown reader
      {
        final String name = "fooBar42";

        // g.1 loop over all useful values for SHARE_MODE
        for (final int shareMode : shareModes) {
          // g.2 loop over all useful values for preferredProtocols
          for (final int preferredProtocol : protocols) {
            final int codeConnect =
                dut.scardConnect(name, shareMode, preferredProtocol, phCard, pdwActiveProtocol);

            assertEquals(
                SCARD_E_UNKNOWN_READER, codeConnect, PcscStatus.getExplanation(codeConnect));
            assertEquals(
                OS_WINDOWS ? ERROR_INVALID_HANDLE : SCARD_E_INVALID_HANDLE,
                dut.getLibrary().scardDisconnect(phCard.getValue(), SCARD_UNPOWER_CARD));
          } // end For (preferredProtocol...)
        } // end For (shareMode...)
      } // end --- g.
    } catch (CardException e) {
      // ... exception from dut.close()-method thrown
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link IfdCollection#close()}. */
  @Test
  void test_close() {
    LOGGER.atTrace().log("test_close");
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. smoke test
    try (IfdCollection dut = new IfdCollection()) {
      // check insIsClosed
      assertFalse(dut.isClosed());

      dut.close();

      assertTrue(dut.isClosed());
    } catch (PcscException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.
  } // end method */

  /** Test method for {@link IfdCollection#isClosed()}. */
  @Test
  void test_isClosed() {
    LOGGER.atTrace().log("test_close");
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. close()-method works as expected

    // Test strategy:
    // --- a. smoke test
    try (IfdCollection dut = new IfdCollection()) {
      assertFalse(dut.isClosed());

      dut.close();

      assertTrue(dut.isClosed());
    } catch (PcscException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.
  } // end method */

  /*
   * Creates an {@link ScardReaderState} with random content.
   *
   * <p>The attributes are set as follows:
   * <ol>
   *   <li>{@link ScardReaderState#szReader}:
   *       Random string with given length.
   *   <li>{@link ScardReaderState#pvUserData}:
   *       Random pointer.
   *   <li>{@link ScardReaderState#dwCurrentState}:
   *       Random {@link Dword} with positive {@code int} value
   *   <li>{@link ScardReaderState#dwEventState}:
   *       Random {@link Dword} with positive {@code int} value
   *   <li>{@link ScardReaderState#rgbAtr}:
   *       Random octet string with length from range [1, 128]
   *   <li>{@link ScardReaderState#cbAtr}:
   *       Random {@link Dword} in range [1, ATR.length]
   * </ol>
   *
   * @param lengthSzReaderName
   *     number of characters in {@link ScardReaderState#szReader}
   *
   * @return new {@link ScardReaderState} with random content
   *
  private static ScardReaderState rndScardReaderState(
      final int lengthSzReaderName
  ) {
    final ScardReaderState result = new ScardReaderState();

    result.szReader = RNG.nextUtf8(lengthSzReaderName);
    result.pvUserData = Pointer.createConstant(RNG.nextInt() & 0x7fffffff);
    result.dwCurrentState = new Dword(RNG.nextInt() & 0x7fffffff);
    result.dwEventState = new Dword(RNG.nextInt() & 0x7fffffff);
    result.rgbAtr = RNG.nextBytes(1, AnswerToReset.MAX_ATR_SIZE);
    result.cbAtr = new Dword(RNG.nextIntClosed(1, result.rgbAtr.length));

    return result;
  } // end method */

  /*
   * Creates an {@link ScardReaderState} with random content.
   *
   * <p>The attributes are set as follows:
   * <ol>
   *   <li>{@link ScardReaderState#szReader}:
   *       set to given parameter.
   *   <li>{@link ScardReaderState#pvUserData}:
   *       Random pointer.
   *   <li>{@link ScardReaderState#dwCurrentState}:
   *       Random {@link Dword} with positive {@code int} value
   *   <li>{@link ScardReaderState#dwEventState}:
   *       Random {@link Dword} with positive {@code int} value
   *   <li>{@link ScardReaderState#rgbAtr}:
   *       Random octet string with length from range [1, 128]
   *   <li>{@link ScardReaderState#cbAtr}:
   *       Random {@link Dword} in range [1, ATR.length]
   * </ol>
   *
   * @param szReaderName
   *     name of reader
   *
   * @return new {@link ScardReaderState} with random content
   *
  private static ScardReaderState rndScardReaderState(
      final String szReaderName
  ) {
    final ScardReaderState result = new ScardReaderState();

    result.szReader = szReaderName;
    result.pvUserData = Pointer.createConstant(RNG.nextInt() & 0x7fffffff);
    result.dwCurrentState = new Dword(RNG.nextInt() & 0x7fffffff);
    result.dwEventState = new Dword(RNG.nextInt() & 0x7fffffff);
    result.rgbAtr = RNG.nextBytes(1, AnswerToReset.MAX_ATR_SIZE);
    result.cbAtr = new Dword(RNG.nextIntClosed(1, result.rgbAtr.length));

    return result;
  } // end method */
} // end class
