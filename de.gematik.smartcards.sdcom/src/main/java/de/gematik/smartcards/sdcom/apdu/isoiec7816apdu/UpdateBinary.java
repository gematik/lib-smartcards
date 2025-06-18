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

import de.gematik.smartcards.utils.Hex;
import java.io.Serial;

/**
 * This class covers the UPDATE BINARY command from [gemSpec_COS#14.3.5], see also ISO/IEC
 * 7816-4:2020 clause 11.3.5.
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li><i>Instances are immutable value-types.</i>>
 *   <li><i>No constructor is thread-safe, because content possibly changes after calling the
 *       constructor but before defensive cloning is performed.</i>
 *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable or
 *       defensively cloned and return values are immutable.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class UpdateBinary extends AbstractBinary {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 4815733325764821426L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0xd6;

  /**
   * Comfort-Constructor for updating the content of a transparent EF.
   *
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param offset offset of first octet in body of transparent EF affected by this command
   * @param data to be written
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; sfi &gt; 30}
   *       <li>{@code 0 &gt; offset}
   *       <li>short file identifier is absent (i.e. {@code sfi = 0}) and {@code offset &gt; '7fff'}
   *       <li>short file identifier is present (i.e. {@code sfi &gt; 0}) and {@code offset > 'ff'}
   *       <li>{@code 0 &ge; Nc &gt; 65535}
   *     </ol>
   */
  public UpdateBinary(final int sfi, final int offset, final byte[] data) {
    super(0, INS, sfi, offset, data);
  } // end constructor */

  /**
   * Comfort-Constructor for updating the content of a transparent EF.
   *
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param offset offset of first octet in body of transparent EF affected by this command
   * @param data to be written, only hexadecimal digits are taken into account
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; sfi &gt; 30}
   *       <li>{@code 0 &gt; offset}
   *       <li>short file identifier is absent (i.e. {@code sfi = 0}) and {@code offset &gt; '7fff'}
   *       <li>short file identifier is present (i.e. {@code sfi &gt; 0}) and {@code offset > 'ff'}
   *       <li>{@code 0 &ge; Nc &gt; 65535}
   *     </ol>
   */
  public UpdateBinary(final int sfi, final int offset, final String data) {
    this(sfi, offset, Hex.toByteArray(data));
  } // end constructor */
} // end class
