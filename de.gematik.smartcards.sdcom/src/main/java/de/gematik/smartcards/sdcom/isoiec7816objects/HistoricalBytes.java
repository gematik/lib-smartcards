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
package de.gematik.smartcards.sdcom.isoiec7816objects;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles historical bytes according to ISO/IEC 7816-3 and ISO/IEC 7816-4.
 *
 * <p><i><b>Note:</b> Unless otherwise stated references are valid for ISO/IEC 7816-3:2006 and
 * ISO/IEC 7816-4:2020.</i>
 *
 * <p>From the perspective of this class
 *
 * <ol>
 *   <li>Instances are immutable value-types. Thus, {@link Object#equals(Object) equals()} and
 *       {@link Object#hashCode() hashCode()} are overwritten {@link Object#clone() clone()} is not
 *       overwritten.
 *   <li>Where data is passed in or out, defensive cloning is performed.
 *   <li>Methods are thread-safe.
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class HistoricalBytes implements Serializable {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -3846532983174702151L; // */

  /**
   * Historical bytes.
   *
   * <p>If historical bytes are absent then the array is empty.
   */
  private final byte[] insHistoricalBytes; // */

  /**
   * Comfort constructor.
   *
   * @param historicalBytes octet string with historical bytes
   */
  public HistoricalBytes(final byte[] historicalBytes) {
    insHistoricalBytes = historicalBytes.clone();
  } // end constructor */

  /**
   * Returns historical bytes.
   *
   * @return historical bytes
   */
  public byte[] getHistoricalBytes() {
    return insHistoricalBytes.clone();
  } // end method */

  /**
   * Checks this object.
   *
   * @return list with findings, empty list indicates no findings
   */
  public List<String> check() {
    return new ArrayList<>();
  } // end method */
} // end class
