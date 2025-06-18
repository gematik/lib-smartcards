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
package de.gematik.smartcards.sdcom.apdu;

import de.gematik.smartcards.sdcom.Message;

/**
 * This interface is the base of all command messages exchanged.
 *
 * <p>Known implementing classes: {@link de.gematik.smartcards.sdcom.apdu.CommandApdu}.
 *
 * <p>This interface assumes (see comment in {@link Message} that a command
 *
 * <ol>
 *   <li>possibly is associated with a (logical) channel and therefore has a channel number.
 *   <li>possibly is protected by cryptographic means.
 * </ol>
 *
 * @param <T> type of command implementing this interface
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public interface Command<T> extends Message {

  /**
   * Returns number of (logical) channel this command is associated with.
   *
   * @return number of (logical) channel this command is associated with
   */
  int getChannelNumber(); // */

  /**
   * Indicates if a command is protected by cryptographic means.
   *
   * <p><i><b>Note:</b> According to the class comment in {@link Message} such a method is only
   * necessary for command.</i>
   *
   * @return {@code TRUE} if command is cryptographically protected, {@code FALSE} otherwise
   */
  boolean isSecureMessagingIndicated(); // */

  /**
   * Modifies command such that an association with a particular (logical) channel is removed.
   *
   * <p>This is the inverse operation to {@link #setChannelNumber(int)}.
   *
   * @return T, equal to the original command but for the channel number
   */
  T removeChannelNumber(); // */

  /**
   * Modifies command such that it is associated with the given channel.
   *
   * <p>This is the inverse operation to {@link #removeChannelNumber()}.
   *
   * @param channelNumber number of (logical) channel this command is associated with
   * @return T, equal to the original command but for the channel number
   */
  T setChannelNumber(int channelNumber); // */
} // end interface
