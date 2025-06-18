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
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import java.io.Serial;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This is a generic superclass for ISO/IEC 7816-4 commands dealing with records.
 *
 * <p>Known subclasses: {@link AppendRecord}, {@link ReadRecord}, {@link UpdateRecord}
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
public abstract class AbstractRecord extends CommandApdu {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 3217719013309029162L; // */

  /** Value used in case record number is absent. */
  public static final int RECORD_NUMBER_ABSENT = 0; // */

  /** Infimum of record numbers. */
  public static final int RECORD_NUMBER_INFIMUM = 1; // */

  /** Supremum of record numbers. */
  public static final int RECORD_NUMBER_SUPREMUM = 254; // */

  /**
   * Comfort constructor for a case 2 APDU, i.e. command data field absent and Le-field present.
   *
   * <p>Estimates parameter P2 from Short File Identifier and bits b3, b2, b1 and passes all other
   * input parameter to constructor from superclass.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param recordNumber number of record
   * @param sfi short file identifier, if 0, then current EF is affected
   * @param b3b2b1 content of bits b3, b2, b1 being command specific
   * @param ne maximum number of expected data bytes in a {@link ResponseApdu}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code recordNumber} is not in range [{@link #RECORD_NUMBER_INFIMUM}, {@link
   *           #RECORD_NUMBER_SUPREMUM}]
   *       <li>{@code sfi} is not in range [{@link CommandApdu#SFI_ABSENT}, {@link
   *           CommandApdu#SFI_SUPREMUM}]
   *       <li>{@code b3b2b1} is not in range [0, 7]
   *       <li>{@code ne} not from set {1, 2, ..., 65535, {@link CommandApdu#NE_SHORT_WILDCARD
   *           NE_SHORT_WILDCARD}, {@link CommandApdu#NE_EXTENDED_WILDCARD NE_EXTENDED_WILDCARD}}.
   *     </ol>
   */
  protected AbstractRecord(
      final int cla,
      final int ins,
      final int recordNumber,
      final int sfi,
      final int b3b2b1,
      final int ne) {
    super(cla, ins, recordNumber, (sfi << 3) | b3b2b1, ne);

    // Note 1: Checks are intentionally performed after super-constructor is called,
    //         because calling super-constructor has to be the first action.

    // --- check recordNumber, sfi, b3b2b1
    checkP1P2(recordNumber, sfi, b3b2b1);

    // Note: For case 2 commands recordNumber=0 is invalid
    if (RECORD_NUMBER_ABSENT == recordNumber) {
      throw new IllegalArgumentException("Invalid recordNumber: " + recordNumber);
    } // end fi

    // --- check Ne
    // Note 2: Intentionally invalid values for Ne are NOT checked here,
    //         because such checks are done in superclass.
  } // end constructor */

  /**
   * Comfort constructor for a case 3 APDU, i.e. command data present and response data absent.
   *
   * @param cla class byte
   * @param ins instruction code
   * @param recordNumber number of record
   * @param sfi short file identifier, if 0, then current EF is affected
   * @param b3b2b1 content of bits b3, b2, b1 being command specific
   * @param cmdData command data field, arbitrary octet string with at least one octet
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code recordNumber} is not in range [{@link #RECORD_NUMBER_ABSENT}, {@link
   *           #RECORD_NUMBER_SUPREMUM}]
   *       <li>{@code sfi} is not in range [{@link CommandApdu#SFI_ABSENT}, {@link
   *           CommandApdu#SFI_SUPREMUM}]
   *       <li>{@code b3b2b1} is not in range [0, 7]
   *       <li>{@code Nc} is not in range [0, 65535]
   *     </ol>
   */
  protected AbstractRecord(
      final int cla,
      final int ins,
      final int recordNumber,
      final int sfi,
      final int b3b2b1,
      final byte[] cmdData) {
    super(cla, ins, recordNumber, (sfi << 3) | b3b2b1, cmdData);

    // Note 1: Checks are intentionally performed after super-constructor is called,
    //         because calling super-constructor has to be the first action.

    // --- check recordNumber, sfi, b3b2b1
    checkP1P2(recordNumber, sfi, b3b2b1);

    // --- check Nc
    // Note 2: Intentionally Nc is not checked here, because
    //         this is sufficiently checked in superclass.
  } // end constructor */

  /**
   * Check {@code recordNumber}, Short File Identifier and bits b3, b2 and b1.
   *
   * @param recordNumber number of record
   * @param sfi short file identifier
   * @param b3b2b1 content of bits b3, b2, b1 being command specific
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code recordNumber} is not in range [{@link #RECORD_NUMBER_ABSENT}, {@link
   *           #RECORD_NUMBER_SUPREMUM}]
   *       <li>{@code sfi} is not in range [{@link CommandApdu#SFI_ABSENT}, {@link
   *           CommandApdu#SFI_SUPREMUM}]
   *       <li>{@code b3b2b1} is not in range [0, 7]
   *     </ol>
   */
  @VisibleForTesting // otherwise = private
  @SuppressWarnings("PMD.CyclomaticComplexity")
  /* package */ static void checkP1P2(final int recordNumber, final int sfi, final int b3b2b1) {
    // --- check record number
    // Note: Append Record doesn't have a record number, thus, we accept 0 here
    if ((RECORD_NUMBER_ABSENT > recordNumber) || (recordNumber > RECORD_NUMBER_SUPREMUM)) {
      throw new IllegalArgumentException("Invalid recordNumber: " + recordNumber);
    } // end fi

    // --- check short file identifier
    if ((SFI_ABSENT > sfi) || (sfi > SFI_SUPREMUM)) {
      throw new IllegalArgumentException("Invalid SFI: " + sfi);
    } // end fi

    // --- check b3b2b1
    if ((0 > b3b2b1) || (b3b2b1 > 7)) {
      throw new IllegalArgumentException("Invalid b3b2b1: " + b3b2b1);
    } // end fi
  } // end method */

  /**
   * Converts given trailer into an explanation {@link String}.
   *
   * @return explanation for certain trailers
   * @see CommandApdu#explainTrailer(int)
   */
  @Override
  public String explainTrailer(final int trailer) {
    // --- [gemSpec_COS]
    if (0x63c0 == (trailer & 0xfff0)) { // NOPMD literal in if statement
      // ... UpdateRetryWarning
      if (0x63cf == trailer) { // NOPMD literal in if statement
        return "UpdateRetryWarning: at least 15 retries";
      } else {
        return "UpdateRetryWarning: " + (trailer & 0xf) + " retries";
      } // end else
    } // end fi

    return switch (trailer) {
      case 0x6282 -> "EndOfRecordWarning";
      case 0x6287 -> "RecordDeactivated";
      case 0x6700 -> "WrongRecordLength";
      default -> super.explainTrailer(trailer);
    }; // end Switch
  } // end method */

  /**
   * Return record number encoded in the {@link CommandApdu}.
   *
   * @return record number associated with command parameters
   */
  public final int getRecordNumber() {
    return getP1();
  } // end method */

  /**
   * Return short file identifier (aka {@code SFI} encoded in the {@link CommandApdu}.
   *
   * @return short file identifier, i.e.
   *     <ol>
   *       <li>if {@code SFI} is absent returns zero,
   *       <li>otherwise {@code SFI} from range [1, 30]
   *     </ol>
   */
  public final int getSfi() {
    return getP2() >> 3;
  } // end method */
} // end class
