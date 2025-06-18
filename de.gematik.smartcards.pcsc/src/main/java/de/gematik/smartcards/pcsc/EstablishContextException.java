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

import java.io.Serial;
import javax.smartcardio.TerminalFactorySpi;

/**
 * Class providing a specific exception in case establishing a context fails.
 *
 * <p>This is a wrapper for {@link PcscException} because method {@code engineTerminals()} from
 * class {@link TerminalFactorySpi} has to be overwritten, but is not allowed to throw checked
 * exceptions.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public class EstablishContextException extends RuntimeException {

  /** Automatically generated constant. */
  @Serial private static final long serialVersionUID = -3991988242185445940L;

  /**
   * Constructor.
   *
   * @param cause name says it all
   */
  public EstablishContextException(final PcscException cause) {
    super(cause);
  } // end constructor */
} // end class
