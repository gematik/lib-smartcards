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
package de.gematik.smartcards.sdcom;

import de.gematik.smartcards.sdcom.apdu.Command;
import de.gematik.smartcards.sdcom.apdu.Response;

/**
 * This is the top-level interface for all messages sent to and from the ICC.
 *
 * <p>Direct known sub-interfaces: {@link Command}, {@link Response}.
 *
 * <p>The interfaces and the implementations in this package and sub-packages assume that
 *
 * <ol>
 *   <li>the communication model is half-duplex, i.e. if a message has been sent in one direction,
 *       then the next message is sent in the opposite direction.
 *   <li>for each command message there is a corresponding response message. Thus,
 *       <ul>
 *         <li>if a communication channel supports logical channels the channel number is
 *             (optionally) given only in the command message. I.e. the sender of a command message
 *             may see different logical channels, but the sender of a response message sees only
 *             one (logical) channel.
 *         <li>if a command message is cryptographically protected (e.g. by secure messaging), then
 *             the corresponding response message is assumed to also be cryptographically protected.
 *       </ul>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public interface Message {

  /**
   * Returns an octet string representation of the message.
   *
   * @return octet string representation of a message
   */
  byte[] getBytes(); // */
} // end interface
