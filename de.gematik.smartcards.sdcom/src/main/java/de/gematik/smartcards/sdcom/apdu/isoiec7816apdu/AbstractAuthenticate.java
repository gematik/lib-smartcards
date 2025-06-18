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

/**
 * This is a generic superclass for ISO/IEC 7816-4 commands dealing with user or component
 * authentication.
 *
 * <p>Directly known subclasses:<br>
 * {@link ChangeReferenceData},<br>
 * {@link GetPinStatus},<br>
 * {@link ResetRetryCounter},<br>
 * {@link Verify}
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li>Instances are immutable value-types.
 *   <li><i>All constructor(s) and methods are thread-safe, because input parameter(s) are immutable
 *       or primitive.</i>
 *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
 *       return values are immutable.</i>
 *   <li><i>It makes no sense to instantiate objects of this type from outside this package. Thus,
 *       an abstract class would do. On the other hand, testing abstract classes requires a
 *       non-abstract (test-)class. Instead here constructors have a "protected" visibility.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public class AbstractAuthenticate extends CommandApdu {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -2515769059680777845L; // */

  /** Infimum of identifier. */
  public static final int ID_INFIMUM = 0; // */

  /** Supremum of identifier. */
  public static final int ID_SUPREMUM = 31; // */

  /**
   * Comfort constructor for a case 1 command APDU.
   *
   * <p>Estimates parameter P2 from location and identifier and passes all other input parameter to
   * constructor from superclass.
   *
   * @param cla class byte
   * @param ins instruction code
   * @param p1 parameter P1
   * @param loc location of password or key
   * @param id identifier of password or key from range [0, 31]
   * @throws IllegalArgumentException if {@code 0 &gt; id &gt; '1f' = 31}
   */
  protected AbstractAuthenticate(
      final int cla, final int ins, final int p1, final EafiLocation loc, final int id) {
    super(cla, ins, p1, loc.reference(id));

    // Note: Here input parameters are not checked, because
    //       that is sufficiently done in superclass.
  } // end constructor */

  /**
   * Comfort constructor for a case 3 command APDU.
   *
   * <p>Estimates parameter P2 from location and identifier and passes all other input parameter to
   * constructor from superclass.
   *
   * @param cla class byte
   * @param ins instruction code
   * @param p1 parameter P1
   * @param loc location of password or key
   * @param id identifier of password or key from range [0, 31]
   * @param cmdData command data field, arbitrary octet string with at least one octet
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; id &gt; '1f' = 31}
   *       <li>{@code 0 &gt; Nc &gt; 65535}
   *     </ol>
   */
  protected AbstractAuthenticate(
      final int cla,
      final int ins,
      final int p1,
      final EafiLocation loc,
      final int id,
      final byte[] cmdData) {
    super(
        cla,
        ins,
        p1,
        loc.reference(id), // P2, possibly throws IllegalArgumentException
        cmdData);

    // Note: here input parameters are not checked, because
    //       that is sufficiently done in superclass.
  } // end constructor */

  /**
   * Returns explanation for given trailer.
   *
   * @param trailer to be explained
   * @return explanation for certain trailers
   * @see CommandApdu#explainTrailer(int)
   */
  @Override
  public String explainTrailer(final int trailer) {
    // --- [gemSpec_COS]
    if (0x63c0 == (trailer & 0xfff0)) { // NOPMD literal in if statement
      if (0x63cf == trailer) { // NOPMD literal in if statement
        return "WrongSecretWarning: at least 15 retries";
      } else {
        return "WrongSecretWarning: " + (trailer & 0xf) + " retries";
      } // end else
    } // end fi

    return switch (trailer) {
      case 0x6983 -> "ObjectBlocked";
      case 0x6a88 -> "ObjectNotFound";
      default -> super.explainTrailer(trailer);
    }; // end Switch
  } // end method */

  /**
   * Describes the location of a secret.
   *
   * @author <a href="mailto:software-development@gematik.de">gematik</a>
   */
  public enum EafiLocation {
    /** Global secret. */
    GLOBAL(0x00), // */

    /** DF-specific secret. */
    DF_SPECIFIC(0x80); // */

    /** Stores coding of location. */
    private final int insLocation;

    /**
     * Comfort constructor.
     *
     * @param location the coding
     */
    EafiLocation(final int location) {
      insLocation = location;
    } // end constructor */

    /**
     * Returns reference of secret.
     *
     * @param id identifier of secret
     * @return reference of secret, i.e. a combination of location and identifier
     * @throws IllegalArgumentException if {@code 0 &gt; id &gt; '1f' = 31}
     */
    /* package */ int reference(final int id) {
      if ((ID_INFIMUM > id) || (id > ID_SUPREMUM)) {
        throw new IllegalArgumentException("Invalid identifier: " + id);
      } // end fi
      // ... valid identifier

      return insLocation | id;
    } // end method */
  } // end LOCATION */
} // end class
