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

/**
 * PSCARDCONTEXT used for SCardEstablishContext.
 *
 * <p><i><b>Note:</b> According to the needs of {@link NativeMappedConverter#defaultValue()} the
 * visibility of this class has to be "public".</i>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
public class ScardContextByReference extends HandleByReference {

  /**
   * Constructs a new {@link ScardContext} from value pointed to by this instance.
   *
   * @return referenced {@link ScardContext}
   */
  public ScardContext getValue() {
    return new ScardContext(getLong());
  } // end method */

  /**
   * Copies the value contained in given {@link ScardContext} to the position this instance points
   * to.
   *
   * @param context the new value to be referenced
   */
  /* package */ void setValue(final ScardContext context) {
    setLong(context.longValue());
  } // end method */
} // end class
