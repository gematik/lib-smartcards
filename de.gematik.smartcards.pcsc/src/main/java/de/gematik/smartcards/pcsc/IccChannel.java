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

import de.gematik.smartcards.sdcom.MessageLayer;
import de.gematik.smartcards.sdcom.apdu.ApduLayer;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ManageChannel;
import de.gematik.smartcards.utils.Hex;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * A logical channel connection to a Smart Card.
 *
 * <p>It is used to exchange APDUs with a Smart Card. A CardChannel object can be obtained by
 * calling the method {@link Card#getBasicChannel} or {@link Card#openLogicalChannel}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see CardChannel
 */
// Note 1: Spotbugs claims "EI_EXPOSE_REP".
//         Spotbugs message: Returning a reference to a mutable object value stored
//         in one of the object's fields exposes the internal representation of the object.
//         Rational: That finding is suppressed because intentionally that
//         reference is returned.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EI_EXPOSE_REP" // see note 1
}) // */
public final class IccChannel extends CardChannel implements ApduLayer {

  /** Number of logical this logical channel. */
  private final int insChannelNumber; // */

  /** Card, to which this channel communicates. */
  private final Icc insCard; // */

  /** Execution time of the previous command-response pair in seconds. */
  private volatile double insTime; // NOPMD volatile not recommended */

  /**
   * Comfort constructor.
   *
   * @param icc this logical channel communicates with
   * @param channel number of this logical channel
   */
  /* package */ IccChannel(final Icc icc, final int channel) {
    super();

    insCard = icc;
    insChannelNumber = channel;
  } // end constructor */

  /**
   * Sends given message and returns the corresponding response.
   *
   * <p>The CLA byte of the command APDU is automatically adjusted to match the channel number of
   * this card channel.
   *
   * <p>Note that this method cannot be used to transmit {@code MANAGE CHANNEL} APDUs. Logical
   * channels should be managed by using
   *
   * <ol>
   *   <li>{@link Card#openLogicalChannel()},
   *   <li>{@link #reset()},
   *   <li>{@link Icc#reset()},
   *   <li>{@link #close()}.
   * </ol>
   *
   * <p>This method is used by the implementation of {@link #send(CommandApdu)}. The purpose of this
   * method is to provide a transparent channel. E.g., if a {@link CommandApdu} is constructed from
   * octet string {@code '00 B0 8102 00 0003'} (i.e., READ BINARY with ShortFileIdentifier=1 and
   * offset=2 and Ne=3, ISO-case 2E (extended)), then {@link #send(CommandApdu)} converts that
   * {@link CommandApdu} to {@code '00 B0 8102 03'} (i.e., ISO-case 2S (short)).
   *
   * @param command command APDU
   * @return corresponding response APDU
   * @throws IllegalArgumentException if {@code apdu} encodes a {@code MANAGE CHANNEL} command
   * @throws IllegalStateException if
   *     <ol>
   *       <li>this channel has been {@link #close() closed}
   *       <li>the corresponding {@link Card} has been {@link Card#disconnect disconnected}
   *     </ol>
   *
   * @see MessageLayer#send(byte[])
   */
  @Override
  public byte[] send(final byte[] command) {
    // --- block MANAGE CHANNEL commands
    if (command[1] == ManageChannel.INS) {
      throw new IllegalArgumentException(
          "MANAGE CHANNEL command not allowed, use other methods: " + Hex.toHexDigits(command));
    } // end fi

    return send(command, true);
  } // end method */

  /**
   * Sends given message.
   *
   * <p>The CLA byte of the command APDU is automatically adjusted to match the channel number of
   * this card channel.
   *
   * <p>This method allows {@code MANAGE CHANNEL} commands.
   *
   * <p>This method is used by the implementation of {@link #send(CommandApdu)}. The purpose of this
   * method is to provide a transparent channel. E.g., if a {@link CommandApdu} is constructed from
   * octet string {@code '00 B0 8102 00 0003'} (i.e., READ BINARY with ShortFileIdentifier=1 and
   * offset=2 and Ne=3, ISO-case 2E (extended)), then {@link #send(CommandApdu)} converts that
   * {@link CommandApdu} to {@code '00 B0 8102 03'} (i.e., ISO-case 2S (short)).
   *
   * @param command command APDU
   * @param adjustChannelNumber flag indicating whether the channel-number has to be integrated into
   *     the CLA byte:
   *     <ul>
   *       <li>if {@code TRUE} then CLA byte is changed
   *       <li>if {@code FALSE} then CLA byte is not changed
   *     </ul>
   *
   * @return corresponding response APDU
   * @throws IllegalStateException if
   *     <ol>
   *       <li>this channel has been {@link #close() closed}
   *       <li>the corresponding {@link Card} has been {@link Card#disconnect disconnected}
   *     </ol>
   *
   * @see MessageLayer#send(byte[])
   */
  /* package */ byte[] send(final byte[] command, final boolean adjustChannelNumber) {
    final double[] executionTime = new double[1];

    try {
      final int channelNo = insChannelNumber;

      if (getCard().isClosed(channelNo)) {
        // ... channel is closed
        throw new IllegalStateException("Logical channel has been closed");
      } // end fi

      // --- adjust CLA byte according to channel-number
      if (adjustChannelNumber) {
        // ... CLA byte needs adjustment

        final CommandApdu cmdA = new CommandApdu(command);
        final CommandApdu cmdB = cmdA.setChannelNumber(channelNo);

        command[0] = (byte) cmdB.getCla();
      } // end fi

      // --- send command and receive response
      return getCard().send(command, executionTime);
    } finally {
      setTime(executionTime[0]);
    } // end finally
  } // end method */

  /**
   * Sends given command APDU.
   *
   * <p>The CLA byte of the command APDU is automatically adjusted to match the channel number of
   * this card channel.
   *
   * <p>Note that this method cannot be used to transmit {@code MANAGE CHANNEL} APDUs. Logical
   * channels should be managed by using
   *
   * <ol>
   *   <li>{@link Card#openLogicalChannel()},
   *   <li>{@link #reset()},
   *   <li>{@link Icc#reset()},
   *   <li>{@link #close()}.
   * </ol>
   *
   * <p>E.g. if a {@link CommandApdu} is constructed from octet string {@code '00 B0 8102 00 0003'}
   * (i.e READ BINARY with ShortFileIdentifier=1 and offset=2 and Ne=3, ISO-case 2E (extended)),
   * then this method converts that {@link CommandApdu} to {@code '00 B0 8102 03'} (i.e. ISO-case 2S
   * (short)). Thus, if the intention is to send an ISO-case 2E method {@link #send(byte[])} has to
   * be used.
   *
   * @param apdu command APDU to be sent
   * @return corresponding {@link ResponseApdu}
   * @throws IllegalArgumentException if {@code apdu} encodes a {@code MANAGE CHANNEL} command
   * @throws IllegalStateException if
   *     <ol>
   *       <li>this channel has been {@link #close() closed}
   *       <li>the corresponding {@link Card} has been {@link Card#disconnect disconnected}
   *     </ol>
   *
   * @see ApduLayer#send(CommandApdu)
   */
  @Override
  public ResponseApdu send(final CommandApdu apdu) {
    // --- block MANAGE CHANNEL commands
    if (apdu.getIns() == ManageChannel.INS) {
      throw new IllegalArgumentException(
          "MANAGE CHANNEL command not allowed, use other methods: " + apdu);
    } // end fi

    return sendCmd(apdu);
  } // end method */

  /**
   * Sends given command APDU.
   *
   * <p>The CLA byte of the command APDU is automatically adjusted to match the channel number of
   * this card channel.
   *
   * <p>This method allows {@code MANAGE CHANNEL} commands.
   *
   * <p>E.g. if a {@link CommandApdu} is constructed from octet string {@code '00 B0 8102 00 0003'}
   * (i.e., READ BINARY with ShortFileIdentifier=1 and offset=2 and Ne=3, ISO-case 2E (extended)),
   * then this method converts that {@link CommandApdu} to {@code '00 B0 8102 03'} (i.e., ISO-case
   * 2S (short)). Thus, if the intention is to send an ISO-case 2E method {@link #send(byte[])} has
   * to be used.
   *
   * @param apdu command APDU to be sent
   * @return corresponding {@link ResponseApdu}
   * @throws IllegalStateException if
   *     <ol>
   *       <li>this channel has been {@link #close() closed}
   *       <li>the corresponding {@link Card} has been {@link Card#disconnect disconnected}
   *     </ol>
   */
  /* package */ ResponseApdu sendCmd(final CommandApdu apdu) {
    // Note: Intentionally, the very similar implementation from class Icc is
    //       NOT used here, because that would not set instance attribute insTime
    //       properly.

    // --- adjust channel number
    final CommandApdu cmd = apdu.setChannelNumber(insChannelNumber);

    getCard().getLogger().atDebug().log("cmd: {}", cmd.toString());

    final ResponseApdu result = new ResponseApdu(send(cmd.getBytes(), false));

    getCard().getLogger().atDebug().log("rsp: {}", result.toString());

    return result;
  } // end method */

  /**
   * Transmits the specified command APDU to the Smart Card and returns the response APDU.
   *
   * <p>The CLA byte of the command APDU is automatically adjusted to match the channel number of
   * this card channel.
   *
   * <p>Note that this method cannot be used to transmit {@code MANAGE CHANNEL} APDUs. Logical
   * channels should be managed by using
   *
   * <ol>
   *   <li>{@link Card#openLogicalChannel()},
   *   <li>{@link #reset()},
   *   <li>{@link Icc#reset()},
   *   <li>{@link #close()}.
   * </ol>
   *
   * <p>Implementations should transparently handle artifacts of the transmission protocol. For
   * example, when using the T=0 protocol, the following processing should occur as described in
   * ISO/IEC 7816-4:
   *
   * <ul>
   *   <li>if the response APDU has an SW1 of {@code 61}, the implementation should issue a {@code
   *       GET RESPONSE} command using {@code SW2} as the {@code Le} field. This process is repeated
   *       as long as an {@code SW1} of {@code 61} is received. The response body of these exchanges
   *       is concatenated to form the final response body.
   *   <li>if the response APDU is {@code 6C XX}, the implementation should reissue the command
   *       using {@code XX} as the {@code Le} field.
   * </ul>
   *
   * <p>The ResponseAPDU returned by this method is the result after this processing has been
   * performed.
   *
   * @param command the command APDU
   * @return the response APDU received from the card
   * @throws IllegalStateException if
   *     <ol>
   *       <li>this channel has been {@link #close() closed}
   *       <li>the corresponding {@link Card} has been {@link Card#disconnect disconnected}
   *     </ol>
   *
   * @throws IllegalArgumentException if the APDU encodes a {@code MANAGE CHANNEL} command
   * @throws NullPointerException if command is null
   */
  @Override
  public ResponseAPDU transmit(final CommandAPDU command) {
    return new ResponseAPDU(send(command.getBytes()));
  } // end method */

  /**
   * Transmits the command APDU stored in the command {@link ByteBuffer} and receives the response
   * APDU in the response {@link ByteBuffer}.
   *
   * <p>The command buffer must contain a valid command APDU data starting at {@code
   * command.position()} and the APDU must be {@code command.remaining()} bytes long. Upon return,
   * the command buffer's position will be equal to its limit; its limit will not have changed. The
   * output buffer will have received the response APDU bytes. Its position will have advanced by
   * the number of bytes received, which is also the return value of this method.
   *
   * <p>The CLA byte of the command APDU is automatically adjusted to match the channel number of
   * this CardChannel.
   *
   * <p>Note that this method cannot be used to transmit {@code MANAGE CHANNEL} APDUs. Logical
   * channels should be managed by using
   *
   * <ol>
   *   <li>{@link Card#openLogicalChannel()},
   *   <li>{@link #reset()},
   *   <li>{@link Icc#reset()},
   *   <li>{@link #close()}.
   * </ol>
   *
   * <p>See {@link #transmit(CommandAPDU)} for a discussion of the handling of response APDUs in
   * case of protocol "T=0".
   *
   * @param command the buffer containing the command APDU
   * @param response the buffer that shall receive the response APDU from the card
   * @return the length of the received response APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code command} and {@code response} are the same object
   *       <li>{@code response} may not have sufficient space to receive the response APDU
   *       <li>{@code command} encodes a {@code MANAGE CHANNEL} command
   *     </ol>
   *
   * @throws IllegalStateException if
   *     <ol>
   *       <li>this channel has been {@link #close() closed}
   *       <li>the corresponding {@link Card} has been {@link Card#disconnect disconnected}
   *     </ol>
   *
   * @throws NullPointerException if command or response is null
   * @throws ReadOnlyBufferException if the response buffer is read-only
   */
  @Override
  public int transmit(final ByteBuffer command, final ByteBuffer response) {
    if (command == response) { // NOPMD use equals()
      throw new IllegalArgumentException("command and response must not be the same object");
    } // end fi

    if (response.isReadOnly()) {
      throw new ReadOnlyBufferException();
    } // end fi

    final byte[] cmd = new byte[command.remaining()];
    command.get(cmd);
    final byte[] rsp = send(cmd);

    if (response.remaining() < rsp.length) {
      throw new IllegalArgumentException("Insufficient space in response buffer");
    } // end fi

    response.put(rsp);

    return rsp.length;
  } // end method */

  /**
   * Closes this card channel.
   *
   * <p>The logical channel is closed by issuing a {@code MANAGE CHANNEL} command that use the
   * format {@code [xx 70 80 00]} {@code xx} is the {@code CLA} byte that encodes this logical
   * channel. After this method returns, calling other methods in this class will raise an {@link
   * IllegalStateException}.
   *
   * <p>Note that the basic logical channel cannot be closed using this method. It can be closed by
   * calling {@link Card#disconnect}.
   *
   * @throws CardException if the card operation failed
   * @throws IllegalStateException if this CardChannel represents a connection the basic logical
   *     channel
   */
  @Override
  public void close() throws CardException {
    try {
      if (0 == insChannelNumber) {
        throw new IllegalStateException("Cannot close basic logical channel");
      } // end fi

      if (getCard().isClosed(insChannelNumber)) {
        // ... already closed
        return;
      } // end fi

      final ResponseApdu rsp = sendCmd(ManageChannel.CLOSE);

      if (0x9000 != rsp.getTrailer()) { // NOPMD literal in if statement
        throw new CardException("close() failed: " + rsp);
      } // end fi
    } finally {
      getCard().insOpenChannels.remove(insChannelNumber);
    } // end finally
  } // end method */

  /**
   * Resets this card channel.
   *
   * @throws CardException if the card operation failed
   * @throws IllegalStateException if
   *     <ol>
   *       <li>this channel has been {@link #close() closed}
   *       <li>the corresponding {@link Card} has been {@link Card#disconnect disconnected}
   *     </ol>
   */
  public void reset() throws CardException {
    final ResponseApdu rsp = sendCmd(ManageChannel.RESET_CHANNEL);

    if (0x9000 != rsp.getTrailer()) { // NOPMD literal in if statement
      throw new CardException("reset() failed: " + rsp);
    } // end fi
  } // end method */

  /**
   * Returns the Card this channel is associated with.
   *
   * @return the Card this channel is associated with
   */
  @Override
  public Icc getCard() {
    return insCard; // spotbugs: EI_EXPOSE_REP
  } // end method */

  /**
   * Returns the channel number of this CardChannel.
   *
   * <p>A channel number of 0 indicates the basic logical channel.
   *
   * @return the channel number of this CardChannel
   * @throws IllegalStateException if
   *     <ol>
   *       <li>this channel has been {@link #close() closed}
   *       <li>if the corresponding Card has been {@link Card#disconnect disconnected}
   *     </ol>
   */
  @Override
  public int getChannelNumber() {
    getCard().checkState();

    if (getCard().isClosed(insChannelNumber)) {
      throw new IllegalStateException("Logical channel has been closed");
    } // end fi

    return insChannelNumber;
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
   * Returns {@link String} representation.
   *
   * @return string representation of {@link Icc} + channelNumber
   */
  @Override
  public String toString() {
    return getCard() + ", channelNumber " + insChannelNumber;
  } // end method
} // end class
