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
 * This interface is the base of all response messages exchanged.
 *
 * <p>Known implementing classes: {@link ResponseApdu}.
 *
 * <p>This interface assumes that a response
 *
 * <ol>
 *   <li>possibly has data (in form of an octet string of certain length).
 *   <li>possibly has a trailer (i.e. status or error code).
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public interface Response extends Message {

  /**
   * Return response data field.
   *
   * <p>In case a response has no data field an empty octet string is returned.
   *
   * @return data field of response
   */
  byte[] getData(); // */

  /**
   * Return number of octet in data.
   *
   * @return the number of octet in the data field of the response or 0 if this response has no data
   */
  int getNr(); // */

  /**
   * Returns trailer (status or error code) of a response.
   *
   * @return trailer, i.e. status of response or 0 if this response has no trailer
   */
  int getTrailer(); // */
} // end interface
