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

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.utils.Hex;
import java.io.Serial;

/**
 * This class covers the WRITE BINARY command from [gemSpec_COS#14.3.6], see also ISO/IEC
 * 7816-4:2020 clause 11.3.4.
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
public final class WriteBinary extends AbstractBinary {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 1493389092778806207L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0xd0; // */

  /**
   * Comfort-Constructor for appending data to the content of a transparent EF.
   *
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param data to be written
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; sfi &gt; 30}
   *       <li>{@code 0 &ge; Nc &gt; 65535}
   *     </ol>
   */
  public WriteBinary(final int sfi, final byte[] data) {
    super(0, INS, sfi, 0, data);
  } // end constructor */

  /**
   * Comfort-Constructor for appending data to the content of a transparent EF.
   *
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param data to be written, only hexadecimal digits are taken into account
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; sfi &gt; 30}
   *       <li>{@code 0 &ge; Nc &gt; 65535}
   *     </ol>
   */
  public WriteBinary(final int sfi, final String data) {
    this(sfi, Hex.toByteArray(data));
  } // end constructor */

  /**
   * Converts given trailer into an explanation {@link String}.
   *
   * @return explanation for certain trailers
   * @see CommandApdu#explainTrailer(int)
   */
  @Override
  public String explainTrailer(final int trailer) {
    // --- [gemSpec_COS]
    if (trailer == 0x6a84) { // NOPMD literal in if statement
      return "DataTooBig";
    } // end fi

    return super.explainTrailer(trailer);
  } // end method */
} // end class
