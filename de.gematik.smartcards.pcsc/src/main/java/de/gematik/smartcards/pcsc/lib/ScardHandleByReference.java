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

/**
 * Pointer to an {@link ScardHandle}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
public class ScardHandleByReference extends HandleByReference {

  /**
   * Constructs a new {@link ScardHandle} from value pointed to by this instance.
   *
   * @return {@link ScardHandle} referenced by this instance
   */
  public ScardHandle getValue() {
    return new ScardHandle(getLong());
  } // end method */

  /**
   * Sets pointer to value contained in given context.
   *
   * @param context - the new value to be referenced
   */
  /* package */ void setValue(final ScardHandle context) {
    setLong(context.longValue());
  } // end method */
} // end class
