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
import java.io.Serial;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This is a generic superclass for ISO/IEC 7816-4 commands dealing data objects.
 *
 * <p>Known subclasses: {@link GetData}, {@link ListPublicKey}, {@link PutData}
 *
 * <p>Intentionally method {@code clone()} from superclass is NOT overwritten, because the
 * superclass provides a shallow copy which is also sufficient for this class.
 *
 * <p>Intentionally method {@code equals(Object)} from superclass is NOT overwritten, because the
 * superclass provides a sufficient implementation for that method which also fulfills the
 * equals-contract for this class.
 *
 * <p>Intentionally method {@code hashCode()} from superclass is NOT overwritten, because the
 * superclass provides a sufficient implementation for that method.
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li>Instances are immutable value-types.
 *   <li><i>All constructor(s) and methods are thread-safe, because input parameter(s) are immutable
 *       or primitive.</i>
 *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
 *       return values are immutable.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "CT_CONSTRUCTOR_THROW"
//         Short message: Classes that throw exceptions in their constructors
//             are vulnerable to Finalizer attacks.
//         That finding is not correct, because an empty finalize() declared
//             "final" is present in superclass.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "CT_CONSTRUCTOR_THROW" // see note 1
}) // */
public abstract class AbstractDataObject extends CommandApdu {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -4208287676223997483L; // */

  /** Infimum value used as tag, see ISO/IEC 7816-4:2020 table 93. */
  public static final int TAG_INFIMUM = 0; // */

  /** Supremum value used as tag, see ISO/IEC 7816-4:2020 table 93. */
  public static final int TAG_SUPREMUM = 0xffff; // */

  /**
   * Comfort constructor for a case 2 APDU, i.e. command data field absent and Le-field present.
   *
   * <p>Estimates parameters P1 and P2 from given tag and passes all other input parameter to
   * constructor from superclass.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param tag tag of TLV-data-object
   * @param ne maximum number of expected data bytes in a response APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code tag} is not in range [{@link #TAG_INFIMUM}, {@link #TAG_SUPREMUM}]
   *       <li>{@code ne} not from set {1, 2, ..., 65535, {@link CommandApdu#NE_SHORT_WILDCARD
   *           NE_SHORT_WILDCARD}, {@link CommandApdu#NE_EXTENDED_WILDCARD NE_EXTENDED_WILDCARD}}.
   *     </ol>
   */
  protected AbstractDataObject(final int cla, final int ins, final int tag, final int ne) {
    super(
        cla, // CLA
        ins, // INS
        tag >> 8, // P1
        tag & 0xff, // P2
        ne); // Ne

    // Note 1: Checks are intentionally performed after super-constructor is called,
    //         because calling super-constructor has to be the first action.

    // --- check tag
    checkTag(tag);

    // --- check Ne
    // Note 2: Intentionally invalid values for CLA, INS and Ne are NOT checked
    //         here, because such checks are best done in superclass.
  } // end constructor */

  /**
   * Comfort constructor for a case 3 APDU, i.e. command data present and response data absent.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param tag tag of TLV-data-object
   * @param cmdData command data field, arbitrary octet string with at least one octet
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code tag} is not in range [{@link #TAG_INFIMUM}, {@link #TAG_SUPREMUM}]
   *       <li>{@code Nc} is not in range [0, 65535]
   *     </ol>
   */
  protected AbstractDataObject(final int cla, final int ins, final int tag, final byte[] cmdData) {
    super(
        cla, // CLA
        ins, // INS
        tag >> 8, // P1
        tag & 0xff, // P2
        cmdData); // command data field

    // Note 1: Checks are intentionally performed after super-constructor is called,
    //         because calling super-constructor has to be the first action.

    // --- check tag
    checkTag(tag);

    // --- check CLA, INS, command data field
    // Note 2: Intentionally invalid values for CLA, INS and Nc are NOT checked
    //         here, because such checks are best done in superclass.

    // Note 3: If tag indicates 'constructed DO' then the command data field has to
    //         contain a concatenation of one or more BER-TLV data objects.
    //         Intentionally this is not checked here, but may be checked by an ICC.
  } // end constructor */

  /**
   * Check for a valid tag.
   *
   * @param tag tag of TLV-data-object
   * @throws IllegalArgumentException if {@code tag} is not in range [{@link #TAG_INFIMUM}, {@link
   *     #TAG_SUPREMUM}]
   */
  @VisibleForTesting // otherwise = private
  /* package */ static void checkTag(final int tag) {
    if ((TAG_INFIMUM > tag) || (tag > TAG_SUPREMUM)) {
      throw new IllegalArgumentException(String.format("Invalid Tag: '%x'", tag));
    } // end fi
  } // end method */

  /**
   * Return tag encoded in parameters P1 and P2.
   *
   * @return tag
   */
  public int getTag() {
    return (getP1() << 8) | getP2();
  } // end method */
} // end class
