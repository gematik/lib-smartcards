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

import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_LEAVE_CARD;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_PROTOCOL_T1;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SHARE_EXCLUSIVE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_UNPOWER_CARD;
import static de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset.MAX_ATR_SIZE;

import de.gematik.smartcards.pcsc.constants.PcscStatus;
import de.gematik.smartcards.pcsc.lib.Dword;
import de.gematik.smartcards.pcsc.lib.DwordByReference;
import de.gematik.smartcards.pcsc.lib.ScardHandle;
import de.gematik.smartcards.pcsc.lib.ScardHandleByReference;
import de.gematik.smartcards.sdcom.MessageLayer;
import de.gematik.smartcards.sdcom.apdu.ApduLayer;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ManageChannel;
import de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardPermission;
import javax.smartcardio.CardTerminal;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class extending {@link Card} and implementing {@link ApduLayer}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see CardTerminal
 */
// Note 1: Spotbugs claims "EI_EXPOSE_REP".
//         Spotbugs message: Returning a reference to a mutable object value stored
//         in one of the object's fields exposes the internal representation of the object.
//         Rational: That finding is suppressed because intentionally that
//         reference is returned.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP" // see note 1
}) // */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.ShortClassName",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports"
})
public final class Icc extends Card implements ApduLayer {

  /**
   * States of a smart card.
   *
   * @author <a href="mailto:software-development@gematik.de">gematik</a>
   */
  private enum EafiState {
    /** ICC is usable. */
    OK,

    /** ICC is removed. */
    REMOVED,

    /** ICC is disconnected. */
    DISCONNECTED,
  } // end enum EafiState

  /**
   * Substitution for long interface-device names assigned by the PC/SC layer.
   *
   * <p>The substitution is used in the name of the {@link Logger}.
   *
   * <p>The substitution name is used as key. The names assigned by the PC/SC layer are used as
   * entries in the value.
   */
  private static final Map<String, List<String>> NAME_SUBSTITUTION =
      Map.ofEntries(
          Map.entry(
              "Alcor AU9560",
              List.of(
                  "Alcor Micro AU9560", // ubuntu
                  "Generic EMV Smartcard Reader" // Windows
                  )),
          Map.entry(
              "Cloud-2700",
              List.of(
                  "Identiv uTrust 2700 R Smart Card Reader [CCID Interface]"
                      + " (53691324212792)", // Linux
                  "Identive CLOUD 2700 R Smart Card Reader [CCID Interface]"
                      + " (53691324212792)", // Linux
                  "Identive CLOUD 2700 R Smart Card Reader" // Windows: gnbe1058
                  )),
          Map.entry(
              "Cloud4700-CB",
              List.of(
                  "Identive Identive CLOUD 4500 F Dual Interface Reader " // ubuntu
                      + "[CLOUD 4700 F Contact Reader] (53201326201956)")),
          Map.entry(
              "Cloud4700-CL",
              List.of(
                  "Identive Identive CLOUD 4500 F Dual Interface Reader " // ubuntu
                      + "[CLOUD 4700 F Contactless Reader] (53201326201956)")),
          Map.entry(
              "Omnikey-6121",
              List.of(
                  "HID Global OMNIKEY 6121 Smart Card Reader"
                      + " [OMNIKEY 6121 Smart Card Reader]", // Linux
                  "HID Global OMNIKEY 6121 Smart Card Reader" // Windows: gnbe1058
                  )),
          Map.entry(
              "SCR-3310",
              List.of(
                  "SCM Microsystems Inc. SCR3310 USB Smart Card Reader", // Windows: gnbe1058
                  "SCM Microsystems Inc. SCR 3310 [CCID Interface] (00000000000000)" // ubuntu
                  ))); // */

  /** Answer-To-Reset. */
  private final AnswerToReset insAnswerToReset; // */

  /** Basic logical channel. */
  private final IccChannel insBasicChannel; // */

  /** {@link CardTerminal} this connection is associated with. */
  private final Ifd insIfd; // */

  /** Instance logger. */
  private final Logger insLogger; // */

  /**
   * Set with number of opened channels.
   *
   * <p><i><b>Note:</b> Intentionally this set never contains {@code 0 = zero}, i.e. the number of
   * the basic logical channel, because that channel is always open.</i>
   */
  /* package */ final Set<Integer> insOpenChannels = new HashSet<>(); // NOPMD no accessor */

  /** {@link String} representation of protocol. */
  private final String insProtocol; // */

  /** Handle to smart card. */
  private final ScardHandle insScardHandle; // */

  /** EafiState of this card connection. */
  private volatile EafiState insState; // NOPMD no accessor */

  /*
   * Thread holding exclusive access to the card.
   *
   * <p>If {@code NULL} then no thread helds exclusive access.
   *
  private volatile @Nullable Thread insExclusiveThread; // NOPMD no accessor */

  /** Execution time of the previous command-response pair in seconds. */
  private volatile double insTime; // NOPMD volatile not recommended */

  /**
   * Constructor.
   *
   * @param ifd to which this smart card is connected
   * @param protocol used for communication
   * @throws IllegalArgumentException if {@code protocol} is not in set {"T=1"}
   */
  /* package */ Icc(final Ifd ifd, final String protocol) throws PcscException {
    super();

    // --- check input parameter
    if (!"T=1".equals(protocol)) { // NOPMD literal in if statement
      throw new IllegalArgumentException("unsupported protocol: " + protocol);
    } // end fi
    // ... requested protocol is in set of supported protocols

    insIfd = ifd;
    insProtocol = protocol;

    // --- connect to smart card
    final ScardHandleByReference phCard = new ScardHandleByReference();
    final DwordByReference pdwActiveProtocol = new DwordByReference();

    PcscStatus.check(
        "Icc: scardConnect",
        insIfd
            .getIfdCollection()
            .scardConnect(
                getIfd().getName(),
                SCARD_SHARE_EXCLUSIVE,
                SCARD_PROTOCOL_T1,
                phCard,
                pdwActiveProtocol));
    insScardHandle = phCard.getValue();

    // --- get ATR
    final DwordByReference pdwState = new DwordByReference();
    final DwordByReference pdwProtocol = new DwordByReference();
    final byte[] atrBuffer = new byte[MAX_ATR_SIZE];
    final ByteBuffer pbAtr = ByteBuffer.wrap(atrBuffer);
    final DwordByReference pcbAtrLen = new DwordByReference(new Dword(MAX_ATR_SIZE));

    PcscStatus.check(
        "Icc: scardStatus",
        ifd.getIfdCollection()
            .getLibrary()
            .scardStatus(
                getScardHandle(),
                null, // mszReaderName (here we are not interested in this value)
                new DwordByReference(), // pcchRederLen (initialized by referencing 0)
                pdwState,
                pdwProtocol,
                pbAtr,
                pcbAtrLen));
    insAnswerToReset =
        new AnswerToReset(Arrays.copyOfRange(atrBuffer, 0, pcbAtrLen.getValue().intValue()));

    // --- set logger
    final String name = ifd.getName();
    final String loggerName =
        NAME_SUBSTITUTION.entrySet().stream()
            .filter(entry -> entry.getValue().stream().anyMatch(name::startsWith))
            .map(
                entry ->
                    name.replace(
                        entry.getValue().stream().filter(name::startsWith).findAny().orElse(name),
                        entry.getKey()))
            .findAny()
            .orElse(name);
    insLogger = LoggerFactory.getLogger(Icc.class.getName() + "." + loggerName);

    // --- set other attributes
    insBasicChannel = new IccChannel(this, 0);
    insState = EafiState.OK;
  } // end constructor */

  /**
   * Check state of smart card.
   *
   * @throws IllegalStateException if
   *     <ol>
   *       <li>smart card is disconnected
   *       <li>smart card is removed
   *     </ol>
   */
  /* package */ void checkState() {
    switch (insState) {
      case DISCONNECTED -> throw new IllegalStateException("Card has been disconnected");
      case REMOVED -> throw new IllegalStateException("Card has been removed");
      default -> {
        // intentionally empty
      } // end default
    } // end Switch
  } // end method */

  /**
   * Returns the ATR of this card.
   *
   * @return the ATR of this card.
   */
  @Override
  public ATR getATR() {
    return new ATR(insAnswerToReset.getBytes());
  } // end method */

  /**
   * Getter.
   *
   * @return {@link AnswerToReset} of connected smart card
   */
  public AnswerToReset getAnswerToReset() {
    return insAnswerToReset;
  } // end method */

  /**
   * Returns the protocol in use for this card.
   *
   * @return the protocol in use for this card, for example "T=0" or "T=1"
   */
  @Override
  public String getProtocol() {
    return insProtocol;
  } // end method */

  /**
   * Returns the CardChannel for the basic logical channel. The basic logical channel has a channel
   * number of 0.
   *
   * @return the CardChannel for the basic logical channel
   * @throws SecurityException if a SecurityManager exists and the caller does not have the required
   *     {@linkplain CardPermission permission}
   * @throws IllegalStateException if this card object has been disposed of via the {@link
   *     #disconnect} method
   */
  @Override
  public CardChannel getBasicChannel() {
    checkState();
    return insBasicChannel; // spotbugs: EI_EXPOSE_REP
  } // end method */

  /**
   * Opens a new logical channel to the card and returns it. The channel is opened by issuing a
   * <code>MANAGE CHANNEL</code> command that should use the format <code>[00 70 00 00 01]</code>.
   *
   * @return the logical channel which has been opened
   * @throws ArithmeticException if channel number in response data field encodes a number greater
   *     than {@link Integer#MAX_VALUE}
   * @throws CardException if the card operation fails
   */
  @Override
  public CardChannel openLogicalChannel() throws CardException {
    try {
      final ResponseApdu rsp = send(ManageChannel.OPEN);

      if (0x9000 != rsp.getTrailer()) { // NOPMD literal in if statement
        throw new CardException("openLogicalChannel() failed, card response: " + rsp);
      } // end fi

      final int channelNo = new BigInteger(rsp.getData()).intValueExact();

      insOpenChannels.add(channelNo);

      return new IccChannel(this, channelNo);
    } catch (TransmitException e) {
      throw new CardException("openLogicalChannel() failed", e);
    } // end Catch (...)
  } // end method */

  /**
   * Resets the application layer of the smart card.
   *
   * <p>All logical channels are closed and the basic logical channel is reset.
   *
   * @throws IllegalArgumentException if the card operation failed
   * @throws IllegalStateException if the corresponding {@link Card} has been {@link Card#disconnect
   *     disconnected}
   */
  public void reset() {
    send(ManageChannel.RESET_APPLICATION, 0x9000);

    insOpenChannels.clear();
  } // end method */

  /**
   * Returns {@code TRUE} if logical channel with given number is open.
   *
   * @param channelNumber number of the channel for which the status is requested
   * @return {@code TRUE} if that channel is open, {@code FALSE} otherwise
   * @throws IllegalStateException if
   *     <ol>
   *       <li>smart card is disconnected
   *       <li>smart card is removed
   *     </ol>
   */
  /* package */ boolean isClosed(final int channelNumber) {
    checkState();

    return (0 != channelNumber) && !insOpenChannels.contains(channelNumber);
  } // end method */

  /**
   * Requests exclusive access to this card.
   *
   * <p>Once a thread has invoked <code>beginExclusive</code>, only this thread is allowed to
   * communicate with this card until it calls <code>endExclusive</code>. Other threads attempting
   * communication will receive a CardException.
   *
   * <p>Applications have to ensure that exclusive access is correctly released. This can be
   * achieved by executing the <code>beginExclusive()</code> and <code>endExclusive</code> calls in
   * a <code>try ... finally</code> block.
   *
   * @throws SecurityException if a SecurityManager exists and the caller does not have the required
   *     {@linkplain CardPermission permission}
   * @throws CardException if exclusive access has already been set or if exclusive access could not
   *     be established
   * @throws IllegalStateException if this card object has been disposed of via the {@linkplain
   *     #disconnect} method
   */
  @Override
  public void beginExclusive() throws CardException {
    // FIXME
    throw new PcscException(0, "not (yet) implemented: beginExclusive()");
  } // end method */

  /**
   * Releases the exclusive access previously established using <code>beginExclusive</code>.
   *
   * @throws SecurityException if a SecurityManager exists and the caller does not have the required
   *     {@linkplain CardPermission permission}
   * @throws IllegalStateException if
   *     <ol>
   *       <li>the active Thread does not currently have exclusive access to this card
   *       <li>this card object has been disposed of via the {@link #disconnect} method
   *     </ol>
   *
   * @throws CardException if the operation failed
   */
  @Override
  public void endExclusive() throws CardException {
    // FIXME
    throw new PcscException(0, "not (yet) implemented: endExclusive");
  } // end method */

  /**
   * Transmits a control command to the terminal device.
   *
   * <p>This can be used to, for example, control terminal functions like a built-in PIN pad or
   * biometrics.
   *
   * @param controlCode the control code of the command
   * @param command the command data
   * @return the response from the terminal device
   * @throws SecurityException if a SecurityManager exists and the caller does not have the required
   *     {@linkplain CardPermission permission}
   * @throws NullPointerException if command is null
   * @throws CardException if the card operation failed
   * @throws IllegalStateException if this card object has been disposed of via the {@linkplain
   *     #disconnect disconnect()} method
   */
  @Override
  public byte[] transmitControlCommand(final int controlCode, final byte[] command)
      throws CardException {
    // FIXME
    throw new PcscException(0, "not (yet) implemented: transimitControlCommand(int, byte[]");
  } // end method */

  /**
   * Disconnects the connection with this card.
   *
   * <p>After this method returns, calling methods on this object or in {{@link CardChannel}s
   * associated with this object that require interaction with the card will raise an {@link
   * IllegalStateException}.
   *
   * @param reset whether to reset the card after disconnecting
   * @throws CardException if the card operation failed
   */
  @Override
  public synchronized void disconnect(final boolean reset) throws CardException {
    if (EafiState.OK != insState) {
      return;
    } // end fi

    // FIXME checkExclusive();

    insState = EafiState.DISCONNECTED;
    // FIXME insExclusiveThread = null; // NOPMD assigning to "null" is a code smell

    PcscStatus.check(
        "Icc.disconnect",
        getIfd()
            .getIfdCollection()
            .getLibrary()
            .scardDisconnect(getScardHandle(), reset ? SCARD_UNPOWER_CARD : SCARD_LEAVE_CARD));
  } // end method */

  /**
   * Return logger of this connection.
   *
   * @return {@link Logger} used by this connection.
   */
  /* package */ Logger getLogger() {
    return insLogger;
  } // end method */

  /**
   * Returns execution time of previous command-response pair.
   *
   * @return execution time in seconds of previous command-response pair
   */
  @Override
  public double getTime() {
    return insTime;
  } // end method */

  /**
   * Sends given message.
   *
   * <p>This method is used by the implementation of {@link #send(CommandApdu)}. The purpose of this
   * method is to provide a transparent channel. E.g., if a {@link CommandApdu} is constructed from
   * octet string {@code '00 B0 8102 00 0003'} (i.e., READ BINARY with ShortFileIdentifier=1 and
   * offset=2 and Ne=3, ISO-case 2E (extended)), then {@link #send(CommandApdu)} converts that
   * {@link CommandApdu} to {@code '00 B0 8102 03'} (i.e., ISO-case 2S (short)).
   *
   * @param command command APDU
   * @return corresponding response APDU
   * @see MessageLayer#send(byte[])
   */
  @Override
  public byte[] send(final byte[] command) {
    final double[] executionTime = new double[1];

    try {
      return send(command, executionTime);
    } finally {
      setTime(executionTime[0]);
    } // end finally
  } // end method */

  /**
   * Sends given message.
   *
   * <p>This method is used by the implementation of {@link #send(CommandApdu)}. The purpose of this
   * method is to provide a transparent channel. E.g., if a {@link CommandApdu} is constructed from
   * octet string {@code '00 B0 8102 00 0003'} (i.e., READ BINARY with ShortFileIdentifier=1 and
   * offset=2 and Ne=3, ISO-case 2E (extended)), then {@link #send(CommandApdu)} converts that
   * {@link CommandApdu} to {@code '00 B0 8102 03'} (i.e., ISO-case 2S (short)).
   *
   * @param command command APDU
   * @param executionTime <b>[OUT]</b> parameter with at least one element which receives the
   *     execution time of the underlying library call in seconds
   * @return corresponding response APDU
   */
  /* package */ byte[] send(final byte[] command, final double... executionTime) {
    try {
      checkState();
      // FIXME checkExclusive();

      getLogger().atTrace().log("cmd: '{}'", Hex.toHexDigits(command));

      final byte[] result =
          getIfd()
              .getIfdCollection()
              .getLibrary()
              .scardTransmit(getScardHandle(), command, executionTime);
      final double runTime = executionTime[0];

      getLogger()
          .atTrace()
          .log("rsp: {},  '{}'", String.format("time=%7.3f s", runTime), Hex.toHexDigits(result));

      return result;
    } catch (PcscException e) {
      if (e.getCode() == PcscStatus.SCARD_W_REMOVED_CARD) {
        insState = EafiState.REMOVED;
      } // end fi

      throw new TransmitException(e);
    } // end Catch (...)
  } // end method */

  /**
   * Sends given command APDU.
   *
   * <p>E.g. if a {@link CommandApdu} is constructed from octet string {@code '00 B0 8102 00 0003'}
   * (i.e., READ BINARY with ShortFileIdentifier=1 and offset=2 and Ne=3, ISO-case 2E (extended)),
   * then this method converts that {@link CommandApdu} to {@code '00 B0 8102 03'} (i.e., ISO-case
   * 2S (short)). Thus, if the intention is to send an ISO-case 2E method {@link #send(byte[])} has
   * to be used.
   *
   * @param apdu command APDU to be sent
   * @return corresponding {@link ResponseApdu}
   */
  @Override
  public ResponseApdu send(final CommandApdu apdu) {
    getLogger().atDebug().log("cmd: {}", apdu.toString());

    final ResponseApdu result = new ResponseApdu(send(apdu.getBytes()));

    getLogger().atDebug().log("rsp: {}", result.toString());

    return result;
  } // end method */

  /**
   * Set execution time based on start- and end time.
   *
   * @param executionTime in seconds
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void setTime(final double executionTime) {
    insTime = executionTime;
  } // end method */

  /**
   * Getter.
   *
   * @return interface device to which this smart card is connected
   */
  /* package */ Ifd getIfd() {
    return insIfd;
  } // end method */

  /**
   * Getter.
   *
   * @return handle to smart card
   */
  private ScardHandle getScardHandle() {
    return insScardHandle;
  } // end method */

  /**
   * Checks if smart card is valid.
   *
   * @return {@code TRUE} is smart card could still be used, {@code FALSE} otherwise
   */
  /* package */ boolean isValid() {
    if (EafiState.OK != insState) {
      return false;
    } // end fi

    // ping card via ScardStatus
    final int code =
        getIfd()
            .getIfdCollection()
            .getLibrary()
            .scardStatus(
                getScardHandle(),
                null, // mszReaderName (here we are not interested in this value)
                new DwordByReference(), // pcchRederLen (initialized by referencing 0)
                new DwordByReference(), // pdwState
                new DwordByReference(), // pdwProtocol
                ByteBuffer.allocate(MAX_ATR_SIZE), // ATR buffer
                new DwordByReference(new Dword(MAX_ATR_SIZE)));

    if (PcscStatus.SCARD_S_SUCCESS == code) {
      return true;
    } // end fi

    insState = EafiState.REMOVED;

    return false;
  } // end method */

  /**
   * Returns {@link String} representation.
   *
   * @return "PC/SC ICC in " + IfdName + protocol + state
   */
  @Override
  public String toString() {
    return "PC/SC ICC in "
        + getIfd().getName()
        + ", protocol "
        + getProtocol()
        + ", state "
        + insState;
  } // end method
} // end class
