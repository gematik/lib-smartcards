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
import com.sun.jna.ptr.ByReference;

/**
 * Pointer to a DWORD (LPDWORD) type used by WinSCard.h.
 *
 * <p><i><b>Note:</b> According to the needs of {@link NativeMappedConverter#defaultValue()} the
 * visibility of this class has to be "public".</i>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
public final class DwordByReference extends ByReference {

  /**
   * Default constructor results in a "pointer" to a {@link Dword} with value 0.
   *
   * <p><i><b>Note:</b> According to requirements of {@link NativeMappedConverter#defaultValue()} a
   * public no-arg constructor is needed in this class.</i>
   */
  public DwordByReference() {
    this(new Dword());
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param value to be referenced
   */
  public DwordByReference(final Dword value) {
    super(Dword.SIZE);
    setValue(value);
  } // end constructor */

  /**
   * Returns value pointed to by this pointer.
   *
   * <p>For the return value a new {@link Dword} object is created.
   *
   * @return referenced value as {@link Dword}
   */
  /* package */
  public Dword getValue() {
    return new Dword(
        (4 == Dword.SIZE) ? getPointer().getInt(0) & 0xffffffffL : getPointer().getLong(0));
  } // end method */

  /**
   * Copies the value contained in given {@link Dword} to the position this pointer points to.
   *
   * @param value new value to be referenced
   */
  /* package */ void setValue(final Dword value) {
    if (4 == Dword.SIZE) { // NOPMD literal in if statement
      getPointer().setInt(0, value.intValue());
    } else {
      getPointer().setLong(0, value.longValue());
    } // end else
  } // end method */
} // end class
