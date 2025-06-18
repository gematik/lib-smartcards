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
 * Context used by most SCard functions.
 *
 * <p>The SCARDCONTEXT type defined in <a
 * href="https://github.com/LudovicRousseau/PCSC">pcsclite.h.in</a>.
 *
 * <p><i><b>Note:</b> According to the needs of {@link NativeMappedConverter#defaultValue()} the
 * visibility of this class has to be "public".</i>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
public final class ScardContext extends Handle {

  /** Automatically generated constant. */
  @Serial private static final long serialVersionUID = -3523834124322054204L;

  /**
   * Default constructor.
   *
   * <p>Constructs a handle with value 0.
   *
   * <p><i><b>Note:</b> According to the needs of {@link NativeMappedConverter#defaultValue()} the
   * visibility of this class has to be "public".</i>
   */
  public ScardContext() {
    this(0L);
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param value used by this context, i.e. a {@link Handle}
   * @throws IllegalArgumentException if value has too many bits for a {@link Handle} on this
   *     platform, e.g. {@link Handle#SIZE} is four but bit-length of value is greater than 32.
   */
  public ScardContext(final long value) {
    super(value);
  } // end constructor */
} // end class
