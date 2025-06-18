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

import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_E_NO_SMARTCARD;
import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_W_REMOVED_CARD;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.INFINITE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_EMPTY;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_PRESENT;

import de.gematik.smartcards.pcsc.constants.PcscStatus;
import de.gematik.smartcards.pcsc.lib.Dword;
import de.gematik.smartcards.pcsc.lib.ScardReaderState;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.List;
import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardNotPresentException;
import javax.smartcardio.CardPermission;
import javax.smartcardio.CardTerminal;

/**
 * Class extending {@link CardTerminal}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see CardTerminal
 */
// Note 1: Spotbugs claims "EI_EXPOSE_REP".
//         Spotbugs message: Returning a reference to a mutable object value
//             stored in one of the object's fields exposes the internal
//             representation of the object.
//         Rational: That finding is suppressed because intentionally that
//             reference is returned.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP" // see note 1
}) // */
@SuppressWarnings({"PMD.ShortClassName", "PMD.TooManyStaticImports"})
public final class Ifd extends CardTerminal {

  /**
   * Smart card.
   *
   * <p>If {@code NULL} this indicates that a smart card is not connected.
   */
  private @Nullable Icc insIcc; // NOPMD no accessor */

  /** Library used for PC/SC activities. */
  private final IfdCollection insIfdCollection; // */

  /** Name of the interface device. */
  private final String insName; // */

  /**
   * Comfort constructor.
   *
   * @param ifdCollection class with access to the library for PC/SC activities
   * @param name of interface device
   */
  /* package */ Ifd(final IfdCollection ifdCollection, final String name) {
    super();

    insIfdCollection = ifdCollection;
    insName = name;
  } // end constructor */

  /**
   * Getter.
   *
   * @return Library used for PC/SC activities
   */
  /* package */ IfdCollection getIfdCollection() {
    return insIfdCollection;
  } // end method */

  /**
   * Returns the unique name of this terminal.
   *
   * @return the unique name of this terminal.
   * @see CardTerminal#getName()
   */
  @Override
  public String getName() {
    return insName;
  } // end method */

  /**
   * Establishes a connection to the card.
   *
   * <p>If a connection has previously established using the specified protocol, this method returns
   * the same {@link Card} object as the previous call.
   *
   * <p><i><b>Notes on implementation:</b></i>
   *
   * <ol>
   *   <li>Currently only protocol "T=1" is supported by this implementation. Other values for
   *       {@code protocol} will cause an {@link IllegalArgumentException}.
   *   <li>
   * </ol>
   *
   * @param protocol the protocol to use ("T=0", "T=1", or "T=CL"), or "*" to connect using any
   *     available protocol
   * @return the card the connection has been established with
   * @throws IllegalArgumentException if protocol is an invalid protocol specification
   * @throws CardNotPresentException if no card is present in this terminal
   * @throws CardException if
   *     <ol>
   *       <li>a connection could not be established using the specified protocol
   *       <li>connection has previously been established using a different protocol
   *     </ol>
   *
   * @throws SecurityException if a SecurityManager exists and the caller does not have the required
   *     {@link CardPermission permission}
   * @see CardTerminal#connect(String)
   */
  @Override
  @SuppressWarnings("PMD.CyclomaticComplexity")
  public synchronized Card connect(final String protocol) throws CardException {
    if (null != insIcc) {
      // ... IFD is already aware of an available ICC
      if (insIcc.isValid()) {
        // ... ICC still usable
        final String iccProtocol = insIcc.getProtocol();

        if (protocol.equals(iccProtocol) || "*".equals(protocol)) { // NOPMD literal cond. statement
          // ... same protocol requested again
          //     => just return the ICC
          return insIcc;
        } else {
          // ... different protocol requested
          //     => it is impossible to connect to the same card with again with a different
          //        protocol, mimic Suns's implementation
          throw new CardException(
              "Cannot connect using "
                  + protocol
                  + ", connection already established using "
                  + iccProtocol);
        } // end else
      } else {
        // ... ICC no longer usable
        insIcc = null; // NOPMD assigning to "null" is a code smell
      } // end else
    } // end fi (null != insIcc)
    // ... null == insIcc
    //     => connect

    try {
      insIcc = new Icc(this, protocol);

      return insIcc; // spotbugs: EI_EXPOSE_REP
    } catch (PcscException e) {
      switch (e.getCode()) { // NOPMD less than three branches
        case SCARD_W_REMOVED_CARD, // Windows
                SCARD_E_NO_SMARTCARD -> // pcsc.lite
            throw new CardNotPresentException("No card present", e);
        default -> throw new CardException("connect() failed", e);
      } // end Switch (code)
    } // end Catch (...)
  } // end method */

  /**
   * Returns whether a card is present in this terminal.
   *
   * @return whether a card is present in this terminal.
   * @throws CardException if the status could not be determined
   * @see CardTerminal#isCardPresent()
   */
  @Override
  public boolean isCardPresent() throws CardException {
    final ScardReaderState[] rgReaderStates =
        ScardReaderState.createArray(List.of(getName()), false);
    PcscStatus.check(
        "Ifd.isCardPresent()",
        getIfdCollection()
            .scardGetStatusChange(
                0, // timeout
                rgReaderStates));
    final int state = rgReaderStates[0].dwEventState.intValue();

    return SCARD_STATE_PRESENT == (state & SCARD_STATE_PRESENT);
  } // end method */

  /**
   * Returns {@link String} representation.
   *
   * @return "PC/SC IFD " + {@link #getName()}
   */
  @Override
  public String toString() {
    return "PC/SC IFD " + getName();
  } // end method */

  /**
   * Waits until a card is present in this terminal or the timeout expires.
   *
   * <p>If the method returns due to an expired timeout, it returns false. Otherwise, it returns
   * true.
   *
   * <p>If a card is present in this terminal when this method is called, it returns immediately.
   *
   * @param timeout if positive, block for up to {@code timeout} milliseconds; if zero, block
   *     indefinitely; must not be negative
   * @return false if the method returns due to an expired timeout, true otherwise.
   * @throws IllegalArgumentException if timeout is negative
   * @throws CardException if the operation failed
   * @see CardTerminal#waitForCardAbsent(long)
   */
  @Override
  public boolean waitForCardPresent(final long timeout) throws CardException {
    return waitForCard(true, timeout);
  } // end method */

  /**
   * Waits until a card is absent in this terminal or the timeout expires.
   *
   * <p>If the method returns due to an expired timeout, it returns false. Otherwise, it returns
   * true.
   *
   * <p>If no card is present in this terminal when this method is called, it returns immediately.
   *
   * @param timeout if positive, block for up to {@code timeout} milliseconds; if zero, block
   *     indefinitely; must not be negative
   * @return false if the method returns due to an expired timeout, true otherwise.
   * @throws IllegalArgumentException if timeout is negative
   * @throws CardException if the operation failed
   * @see CardTerminal#waitForCardAbsent(long)
   */
  @Override
  public boolean waitForCardAbsent(final long timeout) throws CardException {
    return waitForCard(false, timeout);
  } // end method */

  /**
   * Waits until a card is in indicated state in this terminal or the timeout expires.
   *
   * <p>If the method returns due to an expired timeout, it returns false. Otherwise, it returns
   * true.
   *
   * <p>If no card is in the indicated state in this terminal when this method is called, it returns
   * immediately.
   *
   * @param timeout if positive, block for up to {@code timeout} milliseconds; if zero, block
   *     indefinitely; must not be negative
   * @return false if the method returns due to an expired timeout, true otherwise.
   * @throws IllegalArgumentException if timeout is negative
   * @throws CardException if the operation failed
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  private boolean waitForCard(final boolean isPresent, final long timeout) throws CardException {
    // Note 1: SonarQube claims the following finding:
    //         "Refactor this method to not always return the same value."
    // Note 2: This finding is a false positive, because the method sometimes returns TRUE and
    //         otherwise FALSE.
    if (timeout < 0) {
      throw new IllegalArgumentException("timeout must not be negative");
    } // end fi

    // --- initialize startTime
    final long startTime = System.currentTimeMillis();

    // --- initialize ifd
    final ScardReaderState[] rgReaderStates =
        ScardReaderState.createArray(List.of(getName()), false);
    final ScardReaderState ifd = rgReaderStates[0];
    final int wantedState = isPresent ? SCARD_STATE_PRESENT : SCARD_STATE_EMPTY;

    try {
      // --- get current state
      PcscStatus.check(
          "ScardGetStatusChange.1",
          getIfdCollection()
              .scardGetStatusChange(
                  0, // timeout
                  ifd));

      // --- loop
      for (; ; ) {
        final int state = ifd.dwEventState.intValue();

        if (wantedState == (state & wantedState)) {
          // ... reached wanted state
          return true;
        } // end fi
        // ... not yet the wanted state

        ifd.dwCurrentState = ifd.dwEventState;
        ifd.dwEventState = new Dword(); // NOPMD new in loop

        // Note 1: If the current runTime is greater than "timeout" the
        //         difference "timeout - runTime" would be negative.
        //         Math.max(...) avoids negative values.
        // Note 2: For very big "timeout" - values "remaining" would be larger
        //         than Integer.MAX_VALUE. Some platforms do not support such
        //         large positive values for a Dword. Math.min(...) prohibits
        //         exceptions raised by to large Dword-values.
        // Note 3: Integer.MAX_VALUE ms = 24d 20h 31' 23,647"
        final long remaining =
            (0 == timeout)
                ? INFINITE
                : Math.min(
                    Integer.MAX_VALUE,
                    Math.max(0L, timeout - (System.currentTimeMillis() - startTime)));
        PcscStatus.check(
            "ScardGetStatusChange.2",
            getIfdCollection()
                .scardGetStatusChange(
                    remaining, // timeout
                    ifd));
      } // end For (;;)
    } catch (PcscException e) {
      if (e.getCode() == PcscStatus.SCARD_E_TIMEOUT) {
        return false;
      } // end fi

      throw new CardException("waitForCard() failed", e);
    } // end Catch (PcscException
  } // end method */
} // end class
