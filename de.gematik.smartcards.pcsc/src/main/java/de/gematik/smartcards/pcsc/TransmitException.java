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

import de.gematik.smartcards.sdcom.Message;
import java.io.Serial;
import javax.smartcardio.CardException;

/**
 * Class providing a specific exception in case transmission fails.
 *
 * <p>This is a wrapper for {@link PcscException} because method {@code send(byte[]} from interface
 * {@link Message} has to be overwritten, but is not allowed to throw checked exceptions.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public class TransmitException extends RuntimeException {

  /** Automatically generated constant. */
  @Serial private static final long serialVersionUID = 4516241077026964163L; // */

  /**
   * Constructor.
   *
   * @param cause name says it all
   */
  public TransmitException(final CardException cause) {
    super(cause);
  } // end constructor */
} // end class
