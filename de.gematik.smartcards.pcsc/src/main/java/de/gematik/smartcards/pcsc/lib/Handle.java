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

import com.sun.jna.IntegerType;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import java.io.Serial;

/**
 * Base class for handles used in PC/SC.
 *
 * <p>On Windows, it is a handle (ULONG_PTR which cannot be dereferenced). On PCSC, it is an integer
 * (int32_t on OS X, long on Linux).
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
class Handle extends IntegerType {

  /** Automatically generated constant. */
  @Serial private static final long serialVersionUID = 632127127669579760L;

  /** Platform specific number of octet used for internal representation. */
  /* package */ static final int SIZE =
      Platform.isWindows() ? Native.POINTER_SIZE : Dword.SIZE; // */

  /**
   * Create a zero-valued signed IntegerType.
   *
   * @param value of handle
   * @throws IllegalArgumentException if value has too many bits for a Dword on this platform, e.g.
   *     {@link #SIZE} is four but bit-length of value is greater than 32.
   */
  /* package */ Handle(final long value) {
    super(SIZE, value);
  } // end constructor */

  /**
   * Return string representation.
   *
   * @return simple name of this class concatenated with the hex-value enclosed in curly brackets,
   *     e.g. "Handle{1a2b3c4d}"
   * @see IntegerType#toString()
   */
  @Override
  public final String toString() {
    final String formatter = getClass().getSimpleName() + "{%x}";

    return String.format(formatter, longValue());
  } // end method */
} // end class
