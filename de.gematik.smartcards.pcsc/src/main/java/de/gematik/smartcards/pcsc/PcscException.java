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

import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.Serial;
import javax.smartcardio.CardException;

/**
 * Class providing a more specific exception in case something is wrong.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public class PcscException extends CardException {

  /** Automatically generated constant. */
  @Serial private static final long serialVersionUID = 9138292445040201026L; // */

  /** Status code. */
  private final int insCode; // */

  /**
   * Comfort constructor.
   *
   * @param code status code
   * @param message with information about error situation
   */
  public PcscException(final int code, final @Nullable String message) {
    this(code, message, null);
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param code status code
   * @param message with information about error situation
   * @param cause with information about location causing the error
   */
  public PcscException(
      final int code, final @Nullable String message, final @Nullable Throwable cause) {
    super(message, cause);

    insCode = code;
  } // end constructor */

  /**
   * Getter.
   *
   * @return status code
   */
  /* package */ int getCode() {
    return insCode;
  } // end method */

  /**
   * Return {@link String} representation.
   *
   * @return {@link String} representation
   * @see Object#toString()
   */
  @Override
  public String toString() {
    final int code = getCode();
    final String result = super.toString();

    return String.format("StatusCode = %d = 0x%x, %s", code, code, result);
  } // end method */
} // end class
