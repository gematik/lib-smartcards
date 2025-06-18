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
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactorySpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class providing an implementation of {@link TerminalFactorySpi}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public class IfdFactory extends TerminalFactorySpi {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(IfdFactory.class); // */

  /**
   * Constructor with (unused) parameter.
   *
   * @param parameter ignored by this method
   */
  public IfdFactory(final @Nullable Object parameter) {
    // Note 1: The signature of this constructor is proposed in the documentation of
    //         class javax.smartcardio.TerminalFactorySpi in module java.smartcardio.

    super();
    LOGGER.atTrace().log("IfdFactory constructed: {}", LOGGER.getName());

    if (null != parameter) {
      LOGGER
          .atWarn()
          .log("instead of \"null\" parameter({}) = {}", parameter.getClass(), parameter);
    } // end fi
  } // end constructor */

  /**
   * Returns the {@link CardTerminals} created by this factory.
   *
   * @return the CardTerminals created by this factory.
   */
  @Override
  protected CardTerminals engineTerminals() {
    LOGGER.atTrace().log("start engineTerminals()-method");

    return new IfdCollection();
  } // end method */
} // end class
