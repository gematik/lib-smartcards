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
 * This is a generic superclass for ISO/IEC 7816-4 commands dealing with transparent EF.
 *
 * <p>Known subclasses: {@link EraseBinary}, {@link ReadBinary}, {@link UpdateBinary}, {@link
 * WriteBinary}.
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
public abstract class AbstractBinary extends CommandApdu {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = -5284750950279497725L; // */

  /**
   * Comfort constructor for a ISO-case 1 APDU, i.e. command data field absent and Le-field absent.
   *
   * <p>Estimates parameters P1 and P2 from {@code sfi} and {@code offset} and passes all other
   * input parameter to constructor from superclass.
   *
   * @param cla class byte
   * @param ins instruction byte
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
  protected AbstractBinary(final int cla, final int ins, final int sfi, final int offset) {
    super(
        cla, // CLA
        ins, // INS
        (SFI_ABSENT == sfi) ? (offset >> 8) : (0x80 | sfi), // P1
        offset); // P2

    // Note 1: checks are intentionally performed after super-constructor is called,
    //         because calling super-constructor has to be the first action.

    // --- check sfi and offset
    checkP1P2(sfi, offset);
  } // end constructor */

  /**
   * Comfort constructor for a case 2 APDU, i.e. command
   *
   * <p>Estimates parameters P1 and P2 from {@code sfi} and {@code offset} and passes all other
   * input parameters to constructor from superclass.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param offset offset of first octet in body of transparent EF affected by this command
   * @param ne maximum number of expected data bytes in a {@link ResponseApdu}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code sfi} is not in range [0, 30]
   *       <li>{@code 0 &gt; offset}
   *       <li>short file identifier is absent (i.e. {@code sfi = 0}) and {@code offset &gt; '7fff'}
   *       <li>short file identifier is present (i.e. {@code sfi &gt; 0}) and {@code offset > 'ff'}
   *       <li>{@code ne} not from set {1, 2, ..., 65535, {@link CommandApdu#NE_SHORT_WILDCARD
   *           NE_SHORT_WILDCARD}, {@link CommandApdu#NE_EXTENDED_WILDCARD NE_EXTENDED_WILDCARD}}.
   *     </ol>
   */
  protected AbstractBinary(
      final int cla, final int ins, final int sfi, final int offset, final int ne) {
    super(
        cla, // CLA
        ins, // INS
        (SFI_ABSENT == sfi) ? (offset >> 8) : (0x80 | sfi), // P1
        offset, // P2
        ne); // Ne

    // Note 1: Checks are intentionally performed after super-constructor is called,
    //         because calling super-constructor has to be the first action.

    // --- check sfi and offset
    checkP1P2(sfi, offset);

    // --- check Ne
    // Note 2: Intentionally invalid values for Ne are NOT checked here,
    //         because such checks are best done in superclass.
  } // end constructor */

  /**
   * Comfort constructor for a case 3 APDU, i.e. command data present and response data absent.
   *
   * <p>Estimates parameters P1 and P2 from {@code sfi} and {@code offset} and passes all other
   * input parameter to constructor from superclass.
   *
   * @param cla class byte
   * @param ins instruction byte
   * @param sfi short file identifier from range [0, 30], if 0, then current EF is affected
   * @param offset offset of first octet in body of transparent EF affected by this command
   * @param cmdData command data field, arbitrary octet string with at least one octet
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; sfi &gt; 30}
   *       <li>{@code 0 &gt; offset}
   *       <li>short file identifier is absent (i.e. {@code sfi = 0}) and {@code offset &gt; '7fff'}
   *       <li>short file identifier is present (i.e. {@code sfi &gt; 0}) and {@code offset > 'ff'}
   *       <li>{@code 0 &ge; Nc &gt; 65535}
   *     </ol>
   */
  protected AbstractBinary(
      final int cla, final int ins, final int sfi, final int offset, final byte[] cmdData) {
    super(
        cla, // CLA
        ins, // INS
        (SFI_ABSENT == sfi) ? (offset >> 8) : (0x80 | sfi), // P1
        offset, // P2
        cmdData); // command data field

    // Note 1: checks are intentionally performed after super-constructor is called,
    //         because calling super-constructor has to be the first action.

    // --- check short file identifier and offset
    checkP1P2(sfi, offset);

    // --- check command data field
    // Note 2: intentionally Nc is not checked here, because
    //         this is sufficiently checked in superclass.
  } // end constructor */

  /**
   * Checks Short File Identifier (aka {@code sfi} and {@code offset}.
   *
   * @param sfi short file identifier from, <b>SHALL</b> be in range [0, 30], if zero, then current
   *     EF is affected
   * @param offset offset of first octet in body of transparent EF affected by this command
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; sfi &gt; 30}
   *       <li>{@code 0 &gt; offset}
   *       <li>short file identifier is absent (i.e. {@code sfi = 0}) and {@code offset &gt; '7fff'}
   *       <li>short file identifier is present (i.e. {@code sfi &gt; 0}) and {@code offset > 'ff'}
   *     </ol>
   */
  @VisibleForTesting // otherwise = private
  @SuppressWarnings("PMD.CyclomaticComplexity")
  /* package */ static void checkP1P2(final int sfi, final int offset) {
    // --- check short file identifier
    if ((SFI_ABSENT > sfi) || (sfi > SFI_SUPREMUM)) {
      throw new IllegalArgumentException("invalid SFI: " + sfi);
    } // end fi
    // ... 0 <= sfi <= 30

    // --- check offset
    if (0 > offset) {
      throw new IllegalArgumentException("invalid offset: " + offset);
    } // end fi
    // ... offset >= 0

    if (SFI_ABSENT == sfi) {
      // ... no sfi
      //     => offset in two octet

      if (offset > 0x7fff) { // NOPMD literal in if statement
        throw new IllegalArgumentException("invalid offset: " + offset);
      } // end fi
    } else {
      // ... sfi present
      // => offset in one octet

      if (offset > 0xff) { // NOPMD literal in if statement
        throw new IllegalArgumentException("invalid offset for SFI present: " + offset);
      } // end fi
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
      case 0x6a84 -> "DataTooBig";
      case 0x6b00 -> "OffsetTooBig";
      default -> super.explainTrailer(trailer);
    }; // end Switch
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
    final int p1 = getP1();
    return (p1 < 0x80) ? 0 : p1 & 0x7f;
  } // end method */

  /**
   * Return offset encoded in the {@link CommandApdu}.
   *
   * @return offset associated with command parameters from range [0, 'ffff']
   */
  public final int getOffset() {
    final int p1 = getP1();
    final int p2 = getP2();

    return (p1 < 0x80) ? (p1 << 8) + p2 : p2;
  } // end method */
} // end class
