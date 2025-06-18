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
import de.gematik.smartcards.utils.AfiUtils;
import java.io.Serial;

/**
 * This class covers the RESET RETRY COUNTER command from [gemSpec_COS#14.6.5], see also ISO/IEC
 * 7816-4:2020 clause 11.6.10.
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
// Note 1: Spotbugs claims "EQ_DOESNT_OVERRIDE_EQUALS".
//         Spotbugs message: his class extends a class that defines an equals
//             method and adds fields, but doesn't define an equals method itself.
//         Rational: That finding is suppressed because the implementation of
//             the equals()-method in the superclass is sufficient.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "EQ_DOESNT_OVERRIDE_EQUALS" // see note 1
}) // */
public final class ResetRetryCounter extends AbstractAuthenticate {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 4783671832610069572L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0x2c;

  /** Representation of PIN in {@link #toString()} method. */
  private final String insBlindedDataField; // */

  /**
   * Command APDU for resetting the retry counter without PUK and without changing the PIN-value.
   *
   * <p><i><b>Note:</b> The case "command data field absent", i.e. neither PUK nor PIN cannot be
   * managed by the other constructor.</i>
   *
   * @param loc indicates whether this PIN is global or DF-specific
   * @param id is the PIN identifier
   * @throws IllegalArgumentException if {@code 0 &gt; id &gt; '1f' = 31}
   */
  public ResetRetryCounter(final EafiLocation loc, final int id) {
    super(
        0x00, // CLA
        INS, // INS
        3, // P1
        loc, id); // P2 is derived from these parameters

    // Note: here input parameters are not checked, because
    //       that is sufficiently done in superclass.

    insBlindedDataField = "";
  } // end constructor */

  /**
   * Command APDU for resetting the retry counter with PUK and/or new PIN.
   *
   * <p>If
   *
   * <ol>
   *   <li>PUK present and PIN present => P1=0: resetting with PUK and setting a new PIN-value
   *   <li>PUK present and PIN empty => P1=1: resetting with PUK, PIN-value remains unchanged
   *   <li>PUK empty and PIN present => P1=2: resetting without PUK and setting a new PIN-value
   *   <li>PUK empty and PIN empty => P1=3: causes {@link IllegalArgumentException} in this case use
   *       {@link #ResetRetryCounter(EafiLocation, int)} instead.
   * </ol>
   *
   * @param loc indicates whether this PIN is global or DF-specific
   * @param id is the PIN identifier
   * @param format indicates the transport format for PUK and PIN
   * @param puk unblocking code, possibly be empty
   * @param newPin the new secret, possibly be empty
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code 0 &gt; id &gt; '1f' = 31}
   *       <li>{@code 0 &gt; Nc &gt; 65535}
   *       <li>puk and newPIN are both empty
   *     </ol>
   */
  public ResetRetryCounter(
      final EafiLocation loc,
      final int id,
      final EafiPasswordFormat format,
      final String puk,
      final String newPin) {
    // Note 1: Before calling another constructor or a super-constructor it is not
    //         possible to do calculations. Thus, these calculations are delegated
    //         to a private class method called by a private constructor.
    this(estimateParameter(loc, id, format, puk, newPin));

    // Note 2: Here input parameters are not checked, because
    //         that is sufficiently done in superclass.

    // Note 3: If both PUK and newPIN are absent, then Nc=0 which causes
    //         an IllegalArgumentException in superclass constructor.
  } // end constructor */

  /**
   * Comfort constructor.
   *
   * @param obj with output from {@link #estimateParameter(EafiLocation, int, EafiPasswordFormat,
   *     String, String)}
   */
  private ResetRetryCounter(final Object... obj) {
    super(
        0x00, // CLA
        INS, // INS
        (int) obj[0], // P1
        (EafiLocation) obj[1], // location
        (int) obj[2], // id
        (byte[]) obj[3]); // command data field

    insBlindedDataField = (String) obj[4];
  } // end constructor */

  /**
   * Estimates parameter P1 from input parameters puk and newPin.
   *
   * <ol>
   *   <li>PUK present and PIN present => P1=0: resetting with PUK and setting a new PIN-value
   *   <li>PUK present and PIN empty => P1=1: resetting with PUK, PIN-value remains unchanged
   *   <li>PUK empty and PIN present => P1=2: resetting without PUK and setting a new PIN-value
   *   <li>PUK empty and PIN empty => P1=3: causes {@link IllegalArgumentException} in constructor
   *       of superclass because command data field is absent but a Case 3 APDU is expected.
   * </ol>
   *
   * @param loc - indicates whether this PIN is global or DF-specific
   * @param id - is the PIN identifier
   * @param format - indicates the transport format for PUK and PIN
   * @param puk - unblocking code, possibly be empty
   * @param newPin - the new secret, possibly be empty
   * @return array with appropriate information for {@link
   *     ResetRetryCounter#ResetRetryCounter(Object[])}, i.e.
   *     <ol>
   *       <li>index = 0: parameter P1 derived from input parameters puk and newPin
   *       <li>index = 1: location
   *       <li>index = 2: identifier
   *       <li>index = 3: command data field, here possibly empty byte[]
   *       <li>index = 4: blinded command data field
   *     </ol>
   */
  private static Object[] estimateParameter(
      final EafiLocation loc,
      final int id,
      final EafiPasswordFormat format,
      final String puk,
      final String newPin) {
    // --- convert PUK and newPIN to octet string
    byte[] osPuk = format.octets(puk);
    byte[] osPin = format.octets(newPin);

    // --- estimate if PUK or newPIN is empty (i.e., absent)
    final boolean isPukEmpty =
        format.equals(EafiPasswordFormat.FORMAT_2_PIN_BLOCK)
            ? (0x20 == osPuk[0])
            : (0 == osPuk.length);
    final boolean isPinEmpty =
        format.equals(EafiPasswordFormat.FORMAT_2_PIN_BLOCK)
            ? (0x20 == osPin[0])
            : (0 == osPin.length);

    final String pukBlinding;
    if (isPukEmpty) {
      osPuk = AfiUtils.EMPTY_OS;
      pukBlinding = "";
    } else {
      pukBlinding = format.blind(puk);
    } // end else

    final String pinBlinding;
    if (isPinEmpty) {
      osPin = AfiUtils.EMPTY_OS;
      pinBlinding = "";
    } else {
      pinBlinding = format.blind(newPin);
    } // end else

    return new Object[] {
      (isPukEmpty ? 2 : 0) + (isPinEmpty ? 1 : 0), // index = 0: P1
      loc, // index = 1: location
      id, // index = 2: identifier
      AfiUtils.concatenate(osPuk, osPin), // index = 3: command data field
      pukBlinding + pinBlinding
    };
  } // end method */

  /**
   * Converts given trailer into an explanation {@link String}.
   *
   * @param trailer to be explained
   * @return explanation for trailers
   * @see CommandApdu#explainTrailer(int)
   */
  @Override
  public String explainTrailer(final int trailer) {
    // --- [gemSpec_COS]
    return switch (trailer) {
      case 0x6983 -> "CommandBlocked";
      case 0x6985 -> "LongPassword/ShortPassword";
      default -> super.explainTrailer(trailer);
    }; // end Switch
  } // end method */

  /**
   * Returns blinded command data field.
   *
   * @return blinded command data field
   */
  private String getBlindedDataField() {
    return insBlindedDataField;
  } // end method */

  /**
   * Returns a {@link String} representation of this instance.
   *
   * <p>The command data field is blinded to not disclose secrets.
   *
   * @return {@link String} representation of this instance
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return toString(getBlindedDataField());
  } // end method */
} // end class
