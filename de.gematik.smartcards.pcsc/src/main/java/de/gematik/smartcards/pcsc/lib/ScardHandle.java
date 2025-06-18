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
package de.gematik.smartcards.pcsc.lib;

import com.sun.jna.NativeMappedConverter;
import java.io.Serial;

/**
 * The {@code SCARDHANDLE} type defined in {@code pcsclite.h.in}.
 *
 * <p>It represents a connection to a card.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
public class ScardHandle extends Handle {

  /** Automatically generated constant. */
  @Serial private static final long serialVersionUID = -8716055377388435391L;

  /**
   * Default constructor.
   *
   * <p>Constructs a handle with value 0. Default-constructor.
   *
   * <p><i><b>Note:</b> According to the needs of {@link NativeMappedConverter#defaultValue()} the
   * visibility of this constructor has to be "public".</i>
   */
  public ScardHandle() {
    this(0);
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param value the handle
   */
  /* package */ ScardHandle(final long value) {
    super(value);
  } // end constructor */
} // end class
