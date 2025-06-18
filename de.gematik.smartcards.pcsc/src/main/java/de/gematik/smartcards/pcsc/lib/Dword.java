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
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import java.io.Serial;

/**
 * The DWORD type used by WinSCard.h, used wherever an integer is needed in SCard functions.
 *
 * <p>On Windows and OS X, this is always typedef'd to an uint32_t. In the pcsclite library on
 * Linux, it is a long instead, which is 64 bits on 64-bit Linux.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
public class Dword extends IntegerType {

  /** Automatically generated constant. */
  @Serial private static final long serialVersionUID = -8116458501858666194L; // */

  /** Platform specific number of octets used for internal representation. */
  /* package */ static final int SIZE =
      (Platform.isWindows() || Platform.isMac()) ? 4 : NativeLong.SIZE; // */

  /**
   * Default constructor results in a {@link Dword} with value 0.
   *
   * <p><i><b>Note:</b> According to the requirements from {@link ScardIoRequest#ScardIoRequest()}
   * the visibility of this constructor has to be "public".</i>
   */
  public Dword() {
    this(0);
  } // end constructor */

  /**
   * Comfort constructor, results in a {@link Dword} with given value.
   *
   * @param value of {@link Dword}
   * @throws IllegalArgumentException if value has too many bits for a Dword on this platform, e.g.
   *     {@link #SIZE} is four but bit-length of value is greater than 32.
   */
  public Dword(final long value) {
    super(SIZE, value);
  } // end constructor */

  /**
   * Returns signed decimal {@link String}.
   *
   * @return {@link String} representation of this instance
   * @see IntegerType#toString()
   */
  @Override
  public String toString() {
    return Long.toString(longValue());
  } // end method */
} // end class
