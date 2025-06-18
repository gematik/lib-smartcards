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
package de.gematik.smartcards.g2icc.cos;

import de.gematik.smartcards.sdcom.MessageLayer;
import de.gematik.smartcards.sdcom.apdu.ApduLayer;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.utils.AfiUtils;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure Messaging layer for sending APDU.
 *
 * <p>The purpose of this class is to insert an additional layer between an application and an ICC
 * performing secure messaging.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public abstract class SecureMessagingLayer extends SecureMessagingConverter
    implements ApduLayer { // NOPMD possible God class

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(SecureMessagingLayer.class); // */

  /** {@link ApduLayer} where protected command APDU are sent to. */
  private final ApduLayer insApduLayer; // */

  /** Execution time of the previous command-response pair in seconds. */
  private double insTime; // */

  /**
   * Comfort-constructor.
   *
   * @param apduLayer where secured commands are sent to
   */
  protected SecureMessagingLayer(final ApduLayer apduLayer) {
    super();

    insApduLayer = apduLayer;
  } // end constructor */

  /**
   * Sends given message.
   *
   * @param message to be sent, typically a command
   * @return corresponding response message
   */
  @Override
  public final byte[] send(final byte[] message) {
    return insApduLayer.send(message);
  } // end method */

  /**
   * Sends given command APDU.
   *
   * @param cmdApdu command APDU to be sent
   * @return corresponding {@link ResponseApdu}
   * @see ApduLayer#send(CommandApdu)
   */
  @Override
  public final ResponseApdu send(final CommandApdu cmdApdu) {
    final long startTime = System.nanoTime();
    final CommandApdu proCmd = secureCommand(cmdApdu);

    final ResponseApdu proRsp = insApduLayer.send(proCmd);

    final ResponseApdu result = unsecureResponse(proRsp);
    final long endTime = System.nanoTime();
    setTime(startTime, endTime);
    LOGGER.atDebug().log("execution time: {}", AfiUtils.nanoSeconds2Time(endTime - startTime));

    return result;
  } // end method */

  /**
   * Return execution time of the previous command-response pair.
   *
   * @return execution time in seconds of the previous command-response pair
   * @see MessageLayer#getTime()
   */
  @Override
  public final double getTime() {
    return insTime;
  } // end method */

  /**
   * Set execution time based on start- and end time.
   *
   * @param startTime start time in nanoseconds
   * @param endTime end time in nanoseconds
   */
  /* package */ void setTime(final long startTime, final long endTime) {
    insTime = (endTime - startTime) * 1e-9; // convert from nanosecond to second
  } // end method */

  /**
   * Performs an ISO padding on the given message, assuming a block size of 16 octets.
   *
   * @param message to be padded
   * @return {@code message || 80 (00 ... 00)}
   * @see SecureMessagingConverter#padIso(byte[])
   */
  @Override
  protected final byte[] padIso(final byte[] message) {
    final int newLength = ((message.length >> 4) + 1) << 4;
    final byte[] result = Arrays.copyOf(message, newLength);

    // --- set delimiter according to ISO/IEC 7816-4
    result[message.length] = (byte) 0x80;

    return result;
  } // end method */
} // end class
