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

import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_INSUFFICIENT_BUFFER;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_NO_READERS_AVAILABLE;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_READER_UNAVAILABLE;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_S_SUCCESS;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SCOPE_SYSTEM;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_EMPTY;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_PRESENT;

import de.gematik.smartcards.pcsc.constants.PcscStatus;
import de.gematik.smartcards.pcsc.lib.Dword;
import de.gematik.smartcards.pcsc.lib.DwordByReference;
import de.gematik.smartcards.pcsc.lib.ScardContext;
import de.gematik.smartcards.pcsc.lib.ScardContextByReference;
import de.gematik.smartcards.pcsc.lib.ScardHandleByReference;
import de.gematik.smartcards.pcsc.lib.ScardReaderState;
import de.gematik.smartcards.pcsc.lib.WinscardLibrary;
import de.gematik.smartcards.pcsc.lib.WinscardLibraryImpl;
import de.gematik.smartcards.utils.AfiUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class providing an implementation of {@link CardTerminals}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see CardTerminals
 */
@SuppressWarnings({"PMD.TooManyStaticImports"})
public final class IfdCollection extends CardTerminals implements AutoCloseable {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(IfdCollection.class); // */

  /**
   * Collection of {@link Ifd} instances.
   *
   * <p>The mapping assures that for each name only one {@link Ifd} instance exists (within this
   * runtime environment). Thus, the name of an {@link Ifd} also represents an {@link Icc} inserted
   * in that {@link Ifd}.
   */
  private final Map<String, Ifd> insIfdCollection = new ConcurrentHashMap<>(); // NOPMD !accessor */

  /**
   * Flag indicating whether an instance has been closed.
   *
   * <p>{@code FALSE} (the default value upon construction) indicates that the instance is not
   * closed. Calling {@link #close()} changes the value to {@code TRUE}.
   */
  private boolean insIsClosed; // NOPMD no accessor */

  /** Library used for PC/SC activities. */
  private final WinscardLibraryImpl insLibrary; // */

  /** {@link ScardContext} used for library calls. */
  private final ScardContext insScardContext; // */

  /** Default constructor. */
  public IfdCollection() {
    this(SCARD_SCOPE_SYSTEM);
  } // end constructor */

  /**
   * Constructor using the given scope.
   *
   * @param scope used for establishing a context
   */
  /* package */
  @VisibleForTesting
  IfdCollection(final int scope) {
    super();

    try {
      LOGGER.atDebug().log("IfdCollection constructed: {}", LOGGER.getName());

      final WinscardLibraryImpl library = WinscardLibraryImpl.openLib();
      final ScardContextByReference phContext = new ScardContextByReference();
      final int code =
          library.scardEstablishContext(
              new Dword(scope), // scope
              null, // phReserved1
              null, // phReserved2
              phContext); // phContext
      PcscStatus.check("SCardEstablishContext", code);
      // ... no error
      //     => set instance attributes

      insLibrary = library;
      insScardContext = phContext.getValue();
    } catch (PcscException e) {
      // ... exception occurred
      //     => wrap into an unchecked exception, because checked exceptions
      //        are not allowed according to the signature of the method
      LOGGER.atError().log("SCardEstablishContext failed", e);
      throw new EstablishContextException(e);
    } // end Catch (...)
  } // end constructor */

  /**
   * Returns an unmodifiable list of all terminals matching the specified state.
   *
   * <p>If state is {@link State#ALL State.ALL}, this method returns all CardTerminals encapsulated
   * by this object. If state is {@link State#CARD_PRESENT State.CARD_PRESENT} or {@link
   * State#CARD_ABSENT State.CARD_ABSENT}, it returns all CardTerminals where a card is currently
   * present or absent, respectively.
   *
   * <p>If state is {@link State#CARD_INSERTION State.CARD_INSERTION} or {@link State#CARD_REMOVAL
   * State.CARD_REMOVAL}, it returns all CardTerminals for which an insertion (or removal,
   * respectively) was detected during the last call to {@link #waitForChange}. If {@code
   * waitForChange()} has not been called on this object, {@code CARD_INSERTION} is equivalent to
   * {@code CARD_PRESENT} and {@code CARD_REMOVAL} is equivalent to {@code CARD_ABSENT}. For an
   * example of the use of {@code CARD_INSERTION}, see {@link #waitForChange}.
   *
   * @param state the {@link State State} of each element in the returned list
   * @return an unmodifiable list of all terminals matching the specified {@code state}.
   * @throws NullPointerException if state is null
   * @throws CardException if the card operation failed
   * @see CardTerminals#list(State)
   */
  @Override
  public List<CardTerminal> list(final State state) throws CardException {
    // FIXME implement
    // - CARD_INSERTION
    // - CARD_REMOVAL
    LOGGER.atDebug().log("list({})", state);

    final List<String> readerNames;
    switch (state) { // NOPMD less than three branches
      case ALL, CARD_PRESENT, CARD_ABSENT -> {
        final List<String> allReaders = scardListReaders();

        // --- remove all recently disconnected IFD from IFD-collection
        final List<String> recentlyRemoved =
            insIfdCollection.keySet().stream().filter(name -> !allReaders.contains(name)).toList();
        recentlyRemoved.forEach(insIfdCollection::remove);

        if (State.ALL.equals(state)) {
          // ... ALL readers requested
          readerNames = allReaders;
        } else {
          // ... readers requested with a specific state
          //     => filter
          final int wantedState =
              State.CARD_PRESENT.equals(state) ? SCARD_STATE_PRESENT : SCARD_STATE_EMPTY;
          final ScardReaderState[] rgReaderStates = ScardReaderState.createArray(allReaders, false);
          PcscStatus.check(
              "IfdCollection.list(" + state + ")",
              getLibrary()
                  .scardGetStatusChange(
                      getScardContext(),
                      0, // timeout
                      rgReaderStates));
          readerNames =
              Arrays.stream(rgReaderStates)
                  .filter(readerState -> 0 != (readerState.dwEventState.intValue() & wantedState))
                  // Note: szReader is never null, but code-checkers think it might
                  //       be. To silence them here a conversion->String is used
                  //       which tolerates "null".
                  .map(readerState -> String.format("%s", readerState.szReader))
                  .toList();
        } // end else
      } // end ALL

      default ->
          throw new IllegalArgumentException(
              "state = " + state + " not (yet) supported by this implementation");
    } // end Switch (state)

    LOGGER.atDebug().log("readerNames = {}", readerNames);

    return readerNames.stream()
        .map(n -> insIfdCollection.computeIfAbsent(n, i -> new Ifd(this, n)))
        .map(n -> (CardTerminal) n) // NOPMD unnecessary cast (false positive)
        .toList();
  } // end method */

  /**
   * Waits for card insertion or removal in any terminal of this object or until the timeout
   * expires.
   *
   * <p>This method examines each CardTerminal of this object. If a card was inserted into or
   * removed from a CardTerminal since the previous call to {@code waitForChange()}, it returns
   * immediately. Otherwise, or if this is the first call to {@code waitForChange()} on this object,
   * it blocks until a card is inserted into or removed from a CardTerminal.
   *
   * <p>If {@code timeout} is greater than 0, the method returns after {@code timeout} milliseconds
   * even if there is no change in state. In that case, this method returns {@code false}; otherwise
   * it returns {@code true}.
   *
   * <p>This method is often used in a loop in combination with {@link #list(State)
   * list(State.CARD_INSERTION)}, for example:
   *
   * <pre>
   *  TerminalFactory factory = ...;
   *  CardTerminals terminals = factory.terminals();
   *  while (true) {
   *      for (CardTerminal terminal : terminals.list(CARD_INSERTION)) {
   *          // examine Card in terminal, return if it matches
   *      }
   *      terminals.waitForChange();
   *  }</pre>
   *
   * @param timeout if positive, block for up to {@code timeout} milliseconds; if zero, block
   *     indefinitely; must not be negative
   * @return false if the method returns due to an expired timeout, true otherwise.
   * @throws IllegalStateException if this {@code CardTerminals} object does not contain any
   *     terminals
   * @throws IllegalArgumentException if timeout is negative
   * @throws CardException if the card operation failed
   * @see CardTerminals#waitForChange(long)
   */
  @Override
  public boolean waitForChange(final long timeout) throws CardException {
    // FIXME
    throw new PcscException(0, "not (yet) implemented"); // NOPMD
  } // end method */

  /**
   * Closes this resource, relinquishing any underlying resources. This method is invoked
   * automatically on objects managed by the {@code try}-with-resources statement.
   *
   * @throws PcscException if this resource cannot be closed
   * @see AutoCloseable#close()
   */
  @Override
  public void close() throws PcscException {
    synchronized (this) {
      if (isClosed()) {
        // ... already closed
        //     => no further action required
        LOGGER.atDebug().log("IfdCollection already closed");
        return;
      } // end fi
      // ... not (yet) closed
      //     => do so now

      // Note: Set flag inside synchronization block in order to inform
      //       other threads about the state change immediately.
      LOGGER.atDebug().log("IfdCollection closing");
      insIsClosed = true;
    } // end synchronized

    PcscStatus.check("SCardReleaseContext", getLibrary().scardReleaseContext(getScardContext()));
  } // end method */

  /**
   * Getter.
   *
   * @return {@code TRUE} if {@link #close()} has previously been called, {@code FALSE} otherwise
   */
  public boolean isClosed() {
    return insIsClosed;
  } // end method */

  /**
   * Getter.
   *
   * @return Library used for PC/SC activities
   */
  /* package */ WinscardLibraryImpl getLibrary() {
    return insLibrary;
  } // end method */

  /**
   * Getter.
   *
   * @return context used for PC/SC activities
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ ScardContext getScardContext() {
    return insScardContext;
  } // end method */

  /**
   * Simple wrapper around {@link WinscardLibrary#SCardListReaders(ScardContext,
   * java.nio.ByteBuffer, java.nio.ByteBuffer, DwordByReference)}.
   *
   * @return list of reader-names
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ List<String> scardListReaders() throws PcscException {
    final String mszReaderGroups = WinscardLibrary.SCARD_ALL_READERS + "\0";
    final DwordByReference pcchReaders = new DwordByReference();
    byte[] mszReaders = AfiUtils.EMPTY_OS;

    int status;
    do {
      status = getLibrary().scardListReaders(getScardContext(), mszReaderGroups, null, pcchReaders);

      if (SCARD_S_SUCCESS == status) {
        mszReaders = new byte[pcchReaders.getValue().intValue()]; // NOPMD new in loop
        // Note: The next call to SCardListReaders might return SCARD_E_INSUFFICIENT_BUFFER
        //       in case a terminal (i.e. IFD, smart card reader) was attached since the
        //       last call to SCardListReaders determining the length of mszReaders.
        status =
            getLibrary()
                .scardListReaders(getScardContext(), mszReaderGroups, mszReaders, pcchReaders);
      } // end fi (success)
    } while (SCARD_E_INSUFFICIENT_BUFFER == status);
    // ... err contains the code from the last call to SCardListReaders

    return switch (status) {
      case SCARD_S_SUCCESS -> WinscardLibraryImpl.multiString(mszReaders);
      case SCARD_E_NO_READERS_AVAILABLE, SCARD_E_READER_UNAVAILABLE -> Collections.emptyList();
      default -> {
        PcscStatus.check("SCardListReaders", status);
        // Note 1: the next line is unreachable, because the ErrorCode.check(...)-method
        //         throws an exception if 'err' is not 0.
        //         If 'err' is 0 then no exception is thrown but then 'err' is
        //         SCARD_S_SUCCESS and the "else-branch" never applies.
        // Note 2: the following line is kept for two reasons:
        //         a. in case the ErrorCode.check(...)-method does not work as intended,
        //         b. to avoid compiler-errors, because the compiler is not smart enough
        //            to see that the following line is unreachable.
        throw new IllegalStateException(
            "Reached line of code which SHALL be unreachable."
                + " Something is wrong with the PcscStatus.check(...)-method");
      } // end default
    }; // end Switch (status)
  } // end method */

  /**
   * Simple wrapper around {@link WinscardLibraryImpl#scardGetStatusChange(ScardContext, long,
   * ScardReaderState...)}.
   *
   * <p><i><b>Note:</b> Compared to {@link WinscardLibraryImpl#scardGetStatusChange(ScardContext,
   * long, ScardReaderState...)} this method has no parameter {@code context}, because this method
   * uses {@code context} from instance attribute</i>
   *
   * @param dwTimeout see {@link WinscardLibraryImpl#scardGetStatusChange(ScardContext, long,
   *     ScardReaderState...)}
   * @param rgReaderStates see {@link WinscardLibraryImpl#scardGetStatusChange(ScardContext, long,
   *     ScardReaderState...)}
   * @return see {@link WinscardLibraryImpl#scardGetStatusChange(ScardContext, long,
   *     ScardReaderState...)}
   * @throws IllegalArgumentException if {@code readerStates} has zero length
   */
  /* package */ int scardGetStatusChange(
      final long dwTimeout, final ScardReaderState... rgReaderStates) {
    return getLibrary().scardGetStatusChange(getScardContext(), dwTimeout, rgReaderStates);
  } // end method */

  /**
   * Simple wrapper around {@link WinscardLibraryImpl#scardConnect}.
   *
   * <p><i><b>Note:</b> Compared to {@link WinscardLibraryImpl#scardConnect} this method has no
   * parameter {@code context}, because this method uses {@code context} from an instance attribute.
   * </i>
   *
   * @param szReader see {@link WinscardLibrary#SCardConnect}}
   * @param dwShareMode see {@link WinscardLibrary#SCardConnect}}
   * @param dwPreferredProtocols see {@link WinscardLibrary#SCardConnect}}
   * @param phCard see {@link WinscardLibrary#SCardConnect}}
   * @param pdwActiveProtocol see {@link WinscardLibrary#SCardConnect}}
   * @return see {@link WinscardLibrary#SCardConnect}}
   */
  /* package */ int scardConnect(
      final String szReader,
      final int dwShareMode,
      final int dwPreferredProtocols,
      final ScardHandleByReference phCard,
      final DwordByReference pdwActiveProtocol) {
    return getLibrary()
        .scardConnect(
            getScardContext(),
            szReader,
            dwShareMode,
            dwPreferredProtocols,
            phCard,
            pdwActiveProtocol);
  } // end method */
} // end class
