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
package de.gematik.smartcards.pcsc.lib;

import static de.gematik.smartcards.pcsc.constants.PcscStatus.ERROR_INVALID_HANDLE;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.OS_LINUX;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.OS_WINDOWS;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_INSUFFICIENT_BUFFER;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_INVALID_HANDLE;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_INVALID_VALUE;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_NO_SMARTCARD;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_PROTO_MISMATCH;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_UNKNOWN_READER;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_S_SUCCESS;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_W_REMOVED_CARD;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_ALL_READERS;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_DEFAULT_READERS;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_EJECT_CARD;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_LEAVE_CARD;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_PROTOCOL_RAW;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_PROTOCOL_T0;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_PROTOCOL_T1;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_PROTOCOL_UNDEFINED;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_RESET_CARD;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SCOPE_SYSTEM;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SCOPE_TERMINAL;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SCOPE_USER;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SHARE_DIRECT;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SHARE_EXCLUSIVE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SHARE_SHARED;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_EMPTY;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_UNPOWER_CARD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.pcsc.constants.PcscStatus;
import de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset;
import de.gematik.smartcards.sdcom.isoiec7816objects.EafiIccProtocol;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link WinscardLibrary}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isSmartCardResourceManagerRunning")
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestWinscardLibraryImpl {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestWinscardLibraryImpl.class); // */

  /** Execution times for immediate return. */
  private static final List<Long> RUNTIME_IMMEDIATE = new ArrayList<>();

  /** Random number generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Library used for function invocation. */
  private static final WinscardLibraryImpl IMPLEMENTATION = WinscardLibraryImpl.openLib(); // */

  /**
   * List of possible values for "disconnect action".
   *
   * <p>The main reason for this array is to have all relevant constants available in this class.
   */
  private static final int[] DISCONNECT_ACTION =
      new int[] {
        SCARD_LEAVE_CARD, // index = 0
        SCARD_RESET_CARD, // index = 1
        SCARD_UNPOWER_CARD, // index = 2
        SCARD_EJECT_CARD, // index = 3
      }; // */

  /**
   * List of possible values for "preferred protocol".
   *
   * <p>The main reason for this array is to have all relevant constants available in this class.
   */
  private static final int[] PREFERRED_PROTOCOL =
      new int[] {
        SCARD_PROTOCOL_UNDEFINED, // index = 0
        SCARD_PROTOCOL_T0, // index = 1
        SCARD_PROTOCOL_T1, // index = 2
        SCARD_PROTOCOL_T0 + SCARD_PROTOCOL_T1, // index = 3
        SCARD_PROTOCOL_RAW // index = 4
      }; // */

  /**
   * List of (partial) reader names to choose from,
   *
   * <p>The main reason for this array is to have all relevant constants available in this class.
   */
  private static final String[] READER_NAMES =
      new String[] {
        "OMNIKEY 6121", // index = 0
        "CLOUD 2700 R", // index = 1
        "3310", // index = 2, i.e. SCR 3310
      }; // */

  /**
   * List of possible values for "scope".
   *
   * <p>The main reason for this array is to have all relevant constants available in this class.
   */
  private static final int[] SCOPE =
      new int[] {
        SCARD_SCOPE_USER, // index = 0
        SCARD_SCOPE_TERMINAL, // index = 1
        SCARD_SCOPE_SYSTEM // index = 2
      }; // */

  /**
   * List of possible values for "share mode".
   *
   * <p>The main reason for this array is to have all relevant constants available in this class.
   */
  private static final int[] SHARE_MODE =
      new int[] {
        SCARD_SHARE_EXCLUSIVE, // index = 0
        SCARD_SHARE_SHARED, // index = 1
        SCARD_SHARE_DIRECT, // index = 2
      }; // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    assertNotNull(IMPLEMENTATION);

    assertEquals(4, DISCONNECT_ACTION.length);
    assertEquals(SCARD_LEAVE_CARD, DISCONNECT_ACTION[0]);
    assertEquals(5, PREFERRED_PROTOCOL.length);
    assertEquals(SCARD_PROTOCOL_T1, PREFERRED_PROTOCOL[2]);
    assertEquals(3, SCOPE.length);
    assertEquals(SCARD_SCOPE_SYSTEM, SCOPE[2]);
    assertEquals(3, SHARE_MODE.length);
    assertEquals(SCARD_SHARE_SHARED, SHARE_MODE[1]);
    assertEquals(3, IMPLEMENTATION.getScardPciList().size());
  } // end method */

  /** Method executed after other tests. */
  @AfterAll
  static void tearDownAfterClass() {
    // Test strategy:
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
      // - sabine@kubuntu:
      //   RuntimeImmediate: #=62, min=1,592433 ms, mean=2,804403 ms, max=11,310371 ms
      final long meanRuntime = 20_000_000; // 20,000,000 ns = 20,000 us = 20 ms
      assertTrue(min >= 0, "min >= 0");
      assertTrue(
          mean < meanRuntime,
          () ->
              String.format(
                  "mean = %s >= %s",
                  AfiUtils.nanoSeconds2Time(mean), AfiUtils.nanoSeconds2Time(meanRuntime)));
    } // end --- a.
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

  /** Test method for {@link WinscardLibraryImpl#multiString(byte[])}. */
  @Test
  void test_multiString__byteA() {
    LOGGER.atTrace().log("test_multiString__byteA");
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over an increasing amount of random strings
    // --- c. ERROR: no termination

    final Charset utf8 = StandardCharsets.UTF_8;
    final byte[] terminator = new byte[1];

    // --- a. smoke test
    {
      // terminator at the beginning
      assertTrue(WinscardLibraryImpl.multiString(Hex.toByteArray("00")).isEmpty());

      // two strings
      assertEquals(
          List.of("AB", "cde"),
          WinscardLibraryImpl.multiString(Hex.toByteArray("4142006364650000")));
    } // end --- a.

    // --- b. loop over an increasing amount of random strings
    RNG.intsClosed(0, 40, 10)
        .forEach(
            suffixLength -> {
              final byte[] suffix = RNG.nextBytes(suffixLength);

              IntStream.rangeClosed(1, 128)
                  .forEach(
                      noStrings -> {
                        final List<String> expected = new ArrayList<>();
                        for (int i = 0; i < noStrings; i++) {
                          String tmp;
                          do {
                            tmp = RNG.nextUtf8(1, 128);
                            // do not allow terminator in expected string
                          } while (tmp.contains("\0"));
                          expected.add(tmp);
                        } // end For (i...)
                        final byte[][] array = new byte[expected.size() + 2][];
                        int i = array.length;
                        array[--i] = suffix;
                        array[--i] = terminator;
                        while (i-- > 0) {
                          array[i] =
                              AfiUtils.concatenate(expected.get(i).getBytes(utf8), terminator);
                        } // end While (i...)
                        final byte[] input = AfiUtils.concatenate(array);

                        final List<String> present = WinscardLibraryImpl.multiString(input);

                        assertEquals(
                            expected,
                            present,
                            () ->
                                String.format(
                                    "i=%d: input='%s', exp=%s",
                                    noStrings,
                                    Hex.toHexDigits(input),
                                    expected.stream()
                                        .map(
                                            s ->
                                                s.codePoints()
                                                    .mapToObj(cp -> Integer.toString(cp, 16))
                                                    .collect(Collectors.joining("-")))
                                        .toList()));
                      }); // end forEach(noStrings -> ...)
            }); // end forEach(suffixLength -> ...)
    // end --- b.

    // --- c. ERROR: no termination
    List.of(
            "", // empty string
            "4100 42", // no termination at all
            "6100 6200" // no empty string for termination
            )
        .forEach(
            input -> {
              final var octets = Hex.toByteArray(input);
              assertThrows(
                  IllegalArgumentException.class, () -> WinscardLibraryImpl.multiString(octets));
            });
    // end --- c.
  } // end method */

  /** Test method for {@link WinscardLibraryImpl#scardEstablishContext}. */
  @Test
  void test_scardEstablishContext() {
    LOGGER.atTrace().log("test_scardEstablishContext");
    // Assertions:
    // ... a. scardReleaseContext(...)-method works as expected

    final ScardContextByReference phContext = new ScardContextByReference();

    // Test strategy:
    // --- a. establish with all scope values from pcsc-lite.h.in
    for (final int scope : new int[] {SCARD_SCOPE_USER, SCARD_SCOPE_TERMINAL, SCARD_SCOPE_SYSTEM}) {
      // establish context
      final int code =
          IMPLEMENTATION.scardEstablishContext(
              new Dword(scope), // scope NOPMD new in loop
              null, // phReserved1
              null, // phReserved2
              phContext // phContext
              );
      final ScardContext context = phContext.getValue();

      // check return code
      assertEquals(
          OS_WINDOWS && (SCARD_SCOPE_TERMINAL == scope) ? SCARD_E_INVALID_VALUE : SCARD_S_SUCCESS,
          code);

      // release context
      assertEquals(
          OS_WINDOWS && (SCARD_SCOPE_TERMINAL == scope) ? ERROR_INVALID_HANDLE : SCARD_S_SUCCESS,
          IMPLEMENTATION.scardReleaseContext(context));
    } // end For (scope...)
    // end --- a.
  } // end method */

  /**
   * Test method for {@link WinscardLibraryImpl#scardListReaders(ScardContext, String, byte[],
   * DwordByReference)}.
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_scardListReaders() {
    LOGGER.atTrace().log("test_scardListReaders");
    // Assertions:
    // ... a. the list of readers does not change while this test-method is running

    // Test strategy:
    // --- a. get a list with all readers ever attached to the system
    // Note: This also shows the necessary bufferSize.
    //       Allocate a buffer with appropriate size.
    // --- b. loop over all possibilities to establish a scope
    // --- c. loop over all possibilities for hContext
    // --- d. loop over all non-deprecated possibilities for mszGroups
    // --- e. SCardListReaders with pcchReaders indicating a sufficiently large buffer
    // --- f. SCardListReaders with pcchReaders indicating a "too small buffer"
    // --- g. ERROR: non-Windows needs a real ScardContext

    final Set<Integer> scopes = Set.of(SCARD_SCOPE_USER, SCARD_SCOPE_TERMINAL, SCARD_SCOPE_SYSTEM);
    final String[] readerGroups =
        new String[] {
          null, SCARD_ALL_READERS, SCARD_DEFAULT_READERS, SCARD_ALL_READERS + SCARD_DEFAULT_READERS
        };
    final ScardContextByReference phContext = new ScardContextByReference();
    final DwordByReference pcchReaders = new DwordByReference();

    // --- a. get a list with all readers ever attached to the system
    final List<String> allReaders = getListOfReaders(true);
    final int bufferSize =
        allReaders.stream()
                .mapToInt(String::length)
                .map(length -> length + 1) // null-terminator at end of each string
                .sum()
            + 1; // empty string at the end
    assertTrue(bufferSize > 0);
    final byte[] mszReaders = new byte[2 * bufferSize]; // double the memory-space

    // --- b. loop over all possibilities to establish a scope
    // spotless:off
    scopes.stream()
        .filter(scope -> OS_LINUX || (SCARD_SCOPE_TERMINAL != scope))
        .forEach(scope -> {
          LOGGER.atTrace().log("scope = {}", scope);

          // --- establish context
          assertEquals(
              SCARD_S_SUCCESS,
              IMPLEMENTATION.scardEstablishContext(
                  new Dword(scope), // scope
                  null, // phReserved1
                  null, // phReserved2
                  phContext // phContext
              ));

          // --- c. loop over all possibilities for hContext
          for (final ScardContext hContext : new ScardContext[]{null, phContext.getValue()}) {
            LOGGER.atTrace().log("hContext = {}", hContext);

            // --- d. loop over all non-deprecated possibilities for mszGroups
            for (final String mszGroups : readerGroups) {
              LOGGER.atTrace().log("mszGroups = {}", mszGroups);

              if (OS_WINDOWS || (null != hContext)) {
                // ... on Windows "hContext = null" is no problem
                // --- e. SCardListReaders with pcchReaders indicating a sufficiently large
                // buffer
                RNG.intsClosed(bufferSize, mszReaders.length, 10).forEach(bufSize -> {
                  pcchReaders.setValue(new Dword(bufSize)); // NOPMD new in loop
                  assertEquals(
                      SCARD_S_SUCCESS,
                      IMPLEMENTATION.scardListReaders(hContext, mszGroups, mszReaders, pcchReaders)
                  );
                  final List<String> ifds = WinscardLibraryImpl.multiString(mszReaders);

                  ifds.forEach(ifd -> assertTrue(allReaders.contains(ifd)));

                  // --- f. SCardListReaders with pcchReaders indicating a "too small
                  // buffer"
                  assertEquals(
                      SCARD_E_INSUFFICIENT_BUFFER,
                      IMPLEMENTATION.scardListReaders(
                          hContext,
                          mszGroups,
                          mszReaders,
                          new DwordByReference(new Dword(pcchReaders.getValue().longValue() - 1))
                      )
                  );
                }); // end forEach(bufsize -> ...)
              } else {
                // ... on non-Windows "hContext = null" is a problem
                // --- g. ERROR: non-Windows needs a real ScardContext
                pcchReaders.setValue(new Dword(mszReaders.length)); // NOPMD new in loop
                assertEquals(
                    SCARD_E_INVALID_HANDLE,
                    IMPLEMENTATION.scardListReaders(
                        null, // here hContext is always null
                        mszGroups,
                        mszReaders,
                        pcchReaders
                    )
                );
              } // end else
            } // end For (mszGroups...)
          } // end For (hContext...)

          // --- release context
          assertEquals(SCARD_S_SUCCESS, IMPLEMENTATION.scardReleaseContext(phContext.getValue()));
        }); // end forEach(scope -> ...)
    // spotless:on
  } // end method */

  /**
   * Test method for {@link WinscardLibraryImpl#scardGetStatusChange}.
   *
   * <p>This method assumes that at least one smart card reader is available.
   */
  @Test
  void test_scardGetStatusChange() {
    LOGGER.atTrace().log("test_scardGetStatusChange");
    // Assertions:
    // ... a. scardEstablishContext(...)-method works as expected
    // ... b. scardListReaders(...)-method works as expected
    // ... c. scardReleaseContext(...)-method works as expected
    // ... d. the list of readers does not change while this test-method is running

    // Observations:
    // - hContext = null => ERROR_INVALID_HANDLE
    // - readers.length = 0 => IllegalArgumentException in JNA
    // - If bit SCARD_STATE_IGNORE is set in dwCurrentState then that IFD is ignored.
    // - If all IFDs are ignored then status = SCARD_E_NO_READERS_AVAILABLE.

    // Test strategy:
    // --- a. get a list of currently attached readers
    // --- b. establish with all scope values from pcsc-lite.h.in
    // --- c. hContext == null
    // --- d. hContext valid, dwCurrent=SCARD_STATE_UNAWARE
    // TODO more tests
    // --- ?. loop over several possibilities for dwTimeout
    // --- ?. use random dwCurrentState and random dwEventState
    // --- ?. release context

    final ScardContextByReference phContext = new ScardContextByReference();

    // --- a. get a list of currently attached readers
    final List<String> readerNames = getListOfReaders(false);
    assertFalse(
        readerNames.isEmpty(),
        "List with readerNames is empty, but at least one reader SHALL be available.");

    // --- b. establish with all scope values from pcsc-lite.h.in
    for (final int scope : new int[] {SCARD_SCOPE_USER, SCARD_SCOPE_TERMINAL, SCARD_SCOPE_SYSTEM}) {
      LOGGER.atTrace().log("scope = {}", scope);
      if (OS_WINDOWS && (SCARD_SCOPE_TERMINAL == scope)) {
        // ... Windows does not support SCARD_SCOPE_TERMINAL
        //     => continue
        continue;
      } // end fi

      // establish context
      assertEquals(
          SCARD_S_SUCCESS,
          IMPLEMENTATION.scardEstablishContext(
              new Dword(scope), // scope NOPMD new in loop
              null, // phReserved1
              null, // phReserved2
              phContext // phContext
              ));
      final ScardContext context = phContext.getValue();

      // --- c. hContext == null
      // no checks, because hContext is intentionally NOT annotated with @Nullable

      // --- d. hContext valid, dwCurrent=SCARD_STATE_UNAWARE
      testScardGetStatusChangeContextValidDwCurrentStateScardStateUnaware(context, readerNames);

      // release context
      assertEquals(SCARD_S_SUCCESS, IMPLEMENTATION.scardReleaseContext(context));
    } // end For (scope...)
  } // end method */

  /**
   * Test method for {@link WinscardLibraryImpl#scardGetStatusChange}.
   *
   * <p>Here:
   *
   * <ol>
   *   <li>{@code hContext} is always valid
   *   <li>the state of all readers is requested
   *   <li>{@link ScardReaderState#dwCurrentState dwCurrentState} is always {@link
   *       WinscardLibrary#SCARD_STATE_UNAWARE SCARD_STATE_UNAWARE}
   * </ol>
   *
   * @param context see {@link WinscardLibrary#SCardGetStatusChange}
   * @param readerNames names of readers under test
   */
  private void testScardGetStatusChangeContextValidDwCurrentStateScardStateUnaware(
      final ScardContext context, final List<String> readerNames) {
    LOGGER
        .atTrace()
        .log("test_scardGetStatusChange__hContextValid_dwCurrentStateScardStateUnaware");
    // Assertions:
    // ... a. method-under-test returns immediately

    // Test strategy:
    // --- a. loop over various values for dwTimeout
    // --- b. dwEventState = SCARD_STATE_UNAWARE
    // --- c. dwEventState = random-value

    // --- a. loop over various values for dwTimeout
    RNG.intsClosed(0, 10_000, 20)
        .forEach(
            timeout -> {
              // --- b. dwEventState = SCARD_STATE_UNAWARE
              final ScardReaderState[] readers = ScardReaderState.createArray(readerNames, false);
              // fill ScardReaderState elements with random content
              for (int i = readers.length; i-- > 0; ) { // NOPMD assignment in operand
                // readers[i].dwCurrentState = new Dword(SCARD_STATE_UNAWARE);
                // readers[i].dwEventState = new Dword(SCARD_STATE_UNAWARE);
                RNG.nextBytes(readers[i].rgbAtr);
                readers[i].cbAtr =
                    new Dword(RNG.nextIntClosed(0, AnswerToReset.MAX_ATR_SIZE)); // NOPMD new
              } // end For (i...)

              final long startTime = System.nanoTime();
              final int code = IMPLEMENTATION.scardGetStatusChange(context, timeout, readers);
              RUNTIME_IMMEDIATE.add(System.nanoTime() - startTime);

              // --- checks
              assertEquals(SCARD_S_SUCCESS, code);

              // check that ScardReaderStates do not change
              // FIXME
            }); // end forEach(timeout -> ...)
  } // end method */

  /**
   * Test method for {@link WinscardLibraryImpl#scardConnect(ScardContext, String, int, int,
   * ScardHandleByReference, DwordByReference)}.
   *
   * <p>This method assumes that at least one smart card reader is available.
   */
  @Test
  void test_scardConnect__ScardContext_String_int_int_ScardHandleByReference_DwordByReference() {
    LOGGER.atTrace().log("test_scardConnect");
    // Assertions:
    // ... a. scardEstablishContext(...)-method works as expected
    // ... b. scardListReaders(...)-method works as expected
    // ... c. scardReleaseContext(...)-method works as expected
    // ... d. the list of readers does not change while this test-method is running
    // ... e. no card-insertion happens while test-method is running
    // ... e. no card-removal   happens while test-method is running

    // Observations:
    // - none -

    // Test strategy:
    // --- a. get a list of currently attached readers and their status
    // --- b. establish with all scope values from pcsc-lite.h.in
    // --- c. hContext == null
    // --- d. hContext valid
    // --- e. release context

    final ScardContextByReference phContext = new ScardContextByReference();

    // --- a. get a list of currently attached readers and their status
    final ScardReaderState[] readers = getReaderStatus();

    // --- b. establish with all scope values from pcsc-lite.h.in
    for (final int scope : new int[] {SCARD_SCOPE_USER, SCARD_SCOPE_TERMINAL, SCARD_SCOPE_SYSTEM}) {
      LOGGER.atTrace().log("scope = {}", scope);
      if (OS_WINDOWS && (SCARD_SCOPE_TERMINAL == scope)) {
        // ... Windows does not support SCARD_SCOPE_TERMINAL
        //     => continue
        continue;
      } // end fi

      // establish context
      assertEquals(
          SCARD_S_SUCCESS,
          IMPLEMENTATION.scardEstablishContext(
              new Dword(scope), // scope NOPMD new in loop
              null, // phReserved1
              null, // phReserved2
              phContext)); // phContext
      final ScardContext context = phContext.getValue();

      // --- c. hContext == null
      // no checks, because hContext is intentionally NOT annotated with @Nullable

      // --- d. hContext valid
      ztestScardConnectContextValid(context, readers);

      // --- e. release context
      assertEquals(SCARD_S_SUCCESS, IMPLEMENTATION.scardReleaseContext(context));
    } // end For (scope...)
  } // end method */

  /**
   * Test method for {@link WinscardLibraryImpl#scardConnect(ScardContext, String, int, int,
   * ScardHandleByReference, DwordByReference)}.
   *
   * <p>Here:
   *
   * <ol>
   *   <li>{@code hContext} is always valid
   * </ol>
   *
   * @param context see {@link WinscardLibraryImpl#scardConnect}
   * @param readers array with status for all connected readers
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  private void ztestScardConnectContextValid(
      final ScardContext context, final ScardReaderState... readers) {
    LOGGER.atTrace().log("test_scardConnect__hContextValid");
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. loop over all readers
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

    // --- a. loop over all readers
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
            case 1 -> EnumSet.of(EafiIccProtocol.T0);
            case 2 -> EnumSet.of(EafiIccProtocol.T1);
            case 3 -> EnumSet.of(EafiIccProtocol.T0, EafiIccProtocol.T1);
            default -> EnumSet.noneOf(EafiIccProtocol.class);
          };
          // spotless:on
          assertFalse(preferred.isEmpty());

          final int codeConnect =
              IMPLEMENTATION.scardConnect(
                  context, name, shareMode, preferredProtocol, phCard, pdwActiveProtocol);

          if (isEmpty) {
            // ... no smart card in reader
            // --- d. ERROR: reader -> SCARD_STATE_EMPTY
            assertEquals(
                OS_WINDOWS ? SCARD_W_REMOVED_CARD : SCARD_E_NO_SMARTCARD,
                codeConnect,
                PcscStatus.getExplanation(codeConnect));
            assertEquals(
                OS_WINDOWS ? ERROR_INVALID_HANDLE : SCARD_E_INVALID_HANDLE,
                IMPLEMENTATION.scardDisconnect(phCard.getValue(), SCARD_UNPOWER_CARD));
          } else {
            // ... a smart card is present in reader
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
                  IMPLEMENTATION.scardDisconnect(phCard.getValue(), SCARD_UNPOWER_CARD));
            } else {
              // ... common protocol
              assertEquals(SCARD_S_SUCCESS, codeConnect, PcscStatus.getExplanation(codeConnect));

              // --- f. power down the smart card
              assertEquals(
                  SCARD_S_SUCCESS,
                  IMPLEMENTATION.scardDisconnect(phCard.getValue(), SCARD_UNPOWER_CARD));
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
              IMPLEMENTATION.scardConnect(
                  context, name, shareMode, preferredProtocol, phCard, pdwActiveProtocol);

          assertEquals(SCARD_E_UNKNOWN_READER, codeConnect, PcscStatus.getExplanation(codeConnect));
          assertEquals(
              OS_WINDOWS ? ERROR_INVALID_HANDLE : SCARD_E_INVALID_HANDLE,
              IMPLEMENTATION.scardDisconnect(phCard.getValue(), SCARD_UNPOWER_CARD));
        } // end For (preferredProtocol...)
      } // end For (shareMode...)
    } // end --- g.
  } // end method */

  /**
   * Test method for {@link WinscardLibraryImpl#scardDisconnect(ScardHandle, int)}.
   *
   * <p>This method assumes that at least one smart card reader with an ICC is available.
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
  @Test
  void test_scardDisconnect__ScardHandle_int() {
    LOGGER.atTrace().log("test_scardDisconnect__ScardHandle_int");
    // Assertions:
    // ... a. scardEstablishContext(...)-method works as expected
    // ... b. scardListReaders(...)-method works as expected
    // ... c. scardReleaseContext(...)-method works as expected
    // ... d. the list of readers does not change while this test-method is running
    // ... e. no card-insertion happens while test-method is running
    // ... e. no card-removal   happens while test-method is running

    // Observations:
    // - none -

    // Test strategy:
    // --- a. get a list of currently attached readers and their status
    // --- b. establish with all scope values from pcsc-lite.h.in
    // --- c. loop over all readers with CARD_INSERTET
    // --- d. scardConnect(...) with all useful parameter combinations
    // --- e. loop over all possibilities for dwDisposition
    // --- f. scardDisconnect(null, int)
    // --- g. scardDisconnect(cardHandle, int)
    // --- h. release context

    final ScardContextByReference phContext = new ScardContextByReference();
    final ScardHandleByReference phCard = new ScardHandleByReference();
    final DwordByReference pdwActiveProtocol = new DwordByReference();
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
    final int[] disconnectModes =
        new int[] {
          SCARD_LEAVE_CARD, SCARD_RESET_CARD, SCARD_UNPOWER_CARD, SCARD_EJECT_CARD,
        };

    // --- a. get a list of currently attached readers and their status
    final ScardReaderState[] readers = getReaderStatus();

    // --- b. establish with all scope values from pcsc-lite.h.in
    for (final int scope : new int[] {SCARD_SCOPE_USER, SCARD_SCOPE_TERMINAL, SCARD_SCOPE_SYSTEM}) {
      LOGGER.atTrace().log("scope = {}", scope);
      if (OS_WINDOWS && (SCARD_SCOPE_TERMINAL == scope)) {
        // ... Windows does not support SCARD_SCOPE_TERMINAL
        //     => continue
        continue;
      } // end fi

      // establish context
      assertEquals(
          SCARD_S_SUCCESS,
          IMPLEMENTATION.scardEstablishContext(
              new Dword(scope), // scope NOPMD new in loop
              null, // phReserved1
              null, // phReserved2
              phContext // phContext
              ));
      final ScardContext context = phContext.getValue();

      // --- c. loop over all readers with CARD_INSERTED
      for (final ScardReaderState reader : readers) {
        final String name = reader.szReader;
        if (SCARD_STATE_EMPTY == (reader.dwEventState.intValue() & SCARD_STATE_EMPTY)) {
          // ... empty card reader
          continue;
        } // end fi
        // ... card inserted

        final var osAtr = Arrays.copyOfRange(reader.rgbAtr, 0, reader.cbAtr.intValue());
        final var atr = new AnswerToReset(osAtr); // NOPMD new in loop

        // --- d. scardConnect(...) with all useful parameter combinations
        // d.1 loop over all useful values for preferredProtocol
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

          final var intersection = EnumSet.copyOf(preferred);
          intersection.retainAll(atr.getSupportedProtocols());

          if (intersection.isEmpty()) {
            // ... no common protocol
            continue;
          } // end fi
          // ... at least one common protocol

          // d.2 loop over all useful values for SHARE_MODE
          for (final int shareMode : shareModes) {
            // --- e. loop over all possibilities for dwDisposition
            for (final int disconnect : disconnectModes) {
              int code =
                  IMPLEMENTATION.scardConnect(
                      context, name, shareMode, preferredProtocol, phCard, pdwActiveProtocol);
              assertEquals(SCARD_S_SUCCESS, code, PcscStatus.getExplanation(code));

              // --- f. scardDisconnect(null, int)
              // no checks, because card is intentionally NOT annotated with @Nullable

              // --- g. scardDisconnect(cardHandle, int)
              code = IMPLEMENTATION.scardDisconnect(phCard.getValue(), disconnect);
              assertEquals(SCARD_S_SUCCESS, code, PcscStatus.getExplanation(code));
            } // end For (disconnect...)
          } // end For (shareMode...)
        } // end For (preferredProtocol...)
      } // end For (reader...)

      // --- h. release context
      assertEquals(SCARD_S_SUCCESS, IMPLEMENTATION.scardReleaseContext(context));
    } // end For (scope...)
  } // end method */

  /** Test method for {@link WinscardLibraryImpl#scardReleaseContext(ScardContext)}. */
  @Test
  void test_scardReleaseContext() {
    LOGGER.atTrace().log("test_scardReleaseContext");
    // Assertions:
    // ... a. scardEstablishContext(...)-method works as expected

    final ScardContextByReference phContext = new ScardContextByReference();

    // Test strategy:
    // --- a. establish with all scope values from pcsc-lite.h.in
    for (final int scope : new int[] {SCARD_SCOPE_USER, SCARD_SCOPE_TERMINAL, SCARD_SCOPE_SYSTEM}) {
      LOGGER.atTrace().log("SCardEstablishContext: {}", scope);
      // establish context
      assertEquals(
          OS_WINDOWS && (SCARD_SCOPE_TERMINAL == scope) ? SCARD_E_INVALID_VALUE : SCARD_S_SUCCESS,
          IMPLEMENTATION.scardEstablishContext(
              new Dword(scope), // scope NOPMD new in loop
              null, // phReserved1
              null, // phReserved2
              phContext // phContext
              ));

      // release context
      List.of(
              OS_WINDOWS && (SCARD_SCOPE_TERMINAL == scope)
                  ? ERROR_INVALID_HANDLE
                  : SCARD_S_SUCCESS, // first release is successful
              // further releasing cause "ERROR_INVALID_HANDLE"
              OS_WINDOWS ? ERROR_INVALID_HANDLE : SCARD_E_INVALID_HANDLE,
              OS_WINDOWS ? ERROR_INVALID_HANDLE : SCARD_E_INVALID_HANDLE)
          .forEach(
              expectedStatus -> {
                final int codeRelease = IMPLEMENTATION.scardReleaseContext(phContext.getValue());

                assertEquals(
                    expectedStatus, codeRelease, () -> PcscStatus.getExplanation(codeRelease));
              }); // end forEach(expectedStatus -> ...)
    } // end For (scope...)
    // end --- a.
  } // end method */

  /**
   * Get list of all readers.
   *
   * @param allReaders {@code TRUE} indicating if all readers ever attached are requested (Windows
   *     only) or just currently attached readers ({@code FALSE}).
   * @return list of reader names
   */
  private static List<String> getListOfReaders(final boolean allReaders) {
    // Assertions:
    // ... a. scardEstablishContext(...)-method works as expected
    // ... b. scardListReaders(...)-method works as expected
    // ... c. scardReleaseContext(...)-method works as expected

    // --- establish context in recommended way
    final ScardContextByReference phContext = new ScardContextByReference();
    assertEquals(
        SCARD_S_SUCCESS,
        IMPLEMENTATION.scardEstablishContext(
            new Dword(SCARD_SCOPE_SYSTEM), // scope
            null, // phReserved1
            null, // phReserved2
            phContext // phContext
            ));
    final ScardContext context = phContext.getValue();

    // --- get list of readers in recommended way
    final DwordByReference pcchReaders = new DwordByReference();
    int code =
        IMPLEMENTATION.scardListReaders(OS_WINDOWS ? null : context, null, null, pcchReaders);
    assertEquals(SCARD_S_SUCCESS, code);
    final long bufferSize = pcchReaders.getValue().longValue();
    assertTrue(bufferSize > 0);
    final byte[] mszReaders = new byte[(int) bufferSize];

    // call second time (pcchReaders indicate perfect size)
    code =
        IMPLEMENTATION.scardListReaders(
            (allReaders && OS_WINDOWS) ? null : context, // NOPMD assignment to "null"
            null,
            mszReaders,
            pcchReaders);
    assertEquals(SCARD_S_SUCCESS, code);
    final List<String> result = WinscardLibraryImpl.multiString(mszReaders);

    // --- release context
    assertEquals(SCARD_S_SUCCESS, IMPLEMENTATION.scardReleaseContext(context));

    return result;
  } // end method */

  /**
   * Return mapping of reader names and their current status.
   *
   * @return readers and their status
   */
  private static ScardReaderState[] getReaderStatus() {
    // Assertions:
    // ... a. scardEstablishContext(...)-method works as expected
    // ... b. scardListReaders(...)-method works as expected
    // ... c. scardReleaseContext(...)-method works as expected

    // --- establish context in recommended way
    final ScardContextByReference phContext = new ScardContextByReference();
    assertEquals(
        SCARD_S_SUCCESS,
        IMPLEMENTATION.scardEstablishContext(
            new Dword(SCARD_SCOPE_SYSTEM), // scope
            null, // phReserved1
            null, // phReserved2
            phContext // phContext
            ));
    final ScardContext context = phContext.getValue();

    // --- get list of readers in recommended way
    final DwordByReference pcchReaders = new DwordByReference();
    int code =
        IMPLEMENTATION.scardListReaders(OS_WINDOWS ? null : context, null, null, pcchReaders);
    assertEquals(SCARD_S_SUCCESS, code);
    final long bufferSize = pcchReaders.getValue().longValue();
    assertTrue(bufferSize > 0);
    final byte[] mszReaders = new byte[(int) bufferSize];

    // call second time (pcchReaders indicate perfect size)
    code = IMPLEMENTATION.scardListReaders(context, null, mszReaders, pcchReaders);
    assertEquals(SCARD_S_SUCCESS, code);
    final List<String> readerNames = WinscardLibraryImpl.multiString(mszReaders);

    // --- estimate current status of readers
    final ScardReaderState[] result = ScardReaderState.createArray(readerNames, false);
    final long startTime = System.nanoTime();
    code =
        IMPLEMENTATION.scardGetStatusChange(
            context, 0, // timeout
            result);
    RUNTIME_IMMEDIATE.add(System.nanoTime() - startTime);
    assertEquals(SCARD_S_SUCCESS, code);

    // --- release context
    assertEquals(SCARD_S_SUCCESS, IMPLEMENTATION.scardReleaseContext(context));

    return result;
  } // end method */
} // end class
