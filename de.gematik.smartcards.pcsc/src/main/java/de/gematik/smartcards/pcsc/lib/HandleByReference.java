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

import com.sun.jna.ptr.ByReference;

/**
 * Pointer to a handle.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
class HandleByReference extends ByReference {

  /** Default constructor. */
  /* package */ HandleByReference() {
    super(Handle.SIZE);
  } // end constructor */

  /**
   * Returns value stored in the handle referenced by this instance.
   *
   * @return value stored in the handle referenced by this instance
   */
  /* package */ long getLong() {
    return (4 == Handle.SIZE) ? getPointer().getInt(0) : getPointer().getLong(0);
  } // end method */

  /**
   * Sets value stored in handle referenced by this instance.
   *
   * @param value new value
   */
  /* package */ void setLong(final long value) {
    if (4 == Handle.SIZE) { // NOPMD literal in if statement
      getPointer().setInt(0, (int) value);
    } else {
      getPointer().setLong(0, value);
    } // end fi
  } // end method */
} // end class
