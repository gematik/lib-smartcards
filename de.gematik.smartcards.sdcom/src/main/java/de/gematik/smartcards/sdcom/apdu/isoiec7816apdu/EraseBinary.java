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
package de.gematik.smartcards.sdcom.apdu.isoiec7816apdu;

import java.io.Serial;

/**
 * This class covers the ERASE BINARY command from [gemSpec_COS#14.3.1], see also ISO/IEC
 * 7816-4:2020 clause 11.3.7.
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li><i>Instances are immutable value-types.</i>>
 *   <li><i>All constructor(s) and methods are thread-safe, because input parameter(s) are immutable
 *       or primitive.</i>
 *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
 *       return values are immutable.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class EraseBinary extends AbstractBinary {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -2850252862378173594L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0x0e;

  /**
   * Comfort-Constructor for erasing the content of a transparent EF.
   *
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param offset offset of first octet in body of transparent EF affected by this command
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; sfi &gt; 30}
   *       <li>{@code 0 &gt; offset}
   *       <li>short file identifier is absent (i.e. {@code sfi = 0}) and {@code offset &gt; '7fff'}
   *       <li>short file identifier is present (i.e. {@code sfi &gt; 0}) and {@code offset > 'ff'}
   *     </ol>
   */
  public EraseBinary(final int sfi, final int offset) {
    super(
        0x00, // CLA
        INS, // INS
        sfi, // influences P1
        offset); // influences P2 and possibly P1

    // Note: here input parameters are not checked, because
    //       that is sufficiently done in superclass.
  } // end constructor */
} // end class
