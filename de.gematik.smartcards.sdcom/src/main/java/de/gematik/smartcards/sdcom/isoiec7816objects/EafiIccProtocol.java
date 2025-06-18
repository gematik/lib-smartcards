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

/**
 * This class enumerates transmission protocols relevant for PC/SC.
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li><i>All methods are thread-safe, because input parameter(s) are primitive.</i>
 *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
 *       result is immutable.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public enum EafiIccProtocol {
  /** Direct mode, used for test purposes. */
  DIRECT(0, "direct"), // */

  /** T=0 according to ISO/IEC 7816-3. */
  T0(1, "T=0"), // */

  /** T=1 according to ISO/IEC 7816-3. */
  T1(2, "T=1"), // */

  /** Wildcard used for {@link #T0} of {@link #T1}. */
  T0_OR_T1(3, "*"), // */

  /** RAW protocol. */
  RAW(4, "RAW"), // */

  /** T=2, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T2(-1, "T=2"), // */

  /** T=3, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T3(-1, "T=3"), // */

  /** T=4, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T4(-1, "T=4"), // */

  /** T=5, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T5(-1, "T=5"), // */

  /** T=6, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T6(-1, "T=6"), // */

  /** T=7, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T7(-1, "T=7"), // */

  /** T=8, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T8(-1, "T=8"), // */

  /** T=9, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T9(-1, "T=9"), // */

  /** T=10, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T10(-1, "T=10"), // */

  /** T=11, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T11(-1, "T=11"), // */

  /** T=12, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T12(-1, "T=12"), // */

  /** T=13, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T13(-1, "T=13"), // */

  /** T=14, not (yet) standardised by ISO/IEC JTC 1/SC 17. */
  T14(-1, "T=14"), // */

  /** Pseudo protocol optionally indicated in an Answer-To-Reset. */
  T15(-1, "T=15"), // */

  /** Memory card according to TODO. */
  MEMORY_CARD(-1, "MemoryCard"), // */

  /** Universal Serial Bus according to ISO/IEC 7816-12. */
  USB(-1, "USB"), // */

  /** Contactless according to ISO/IEC 14443 series. */
  CL(-1, "T=CL");

  /** User friendly description. */
  private final String insDescription; // */

  /** PC/SC code used by corresponding protocol. */
  private final int insPcscCode; // */

  /**
   * Constructor.
   *
   * @param pcscCode PC/SC code to be used by this protocol or {@code -1} if no such code exists
   * @param description user friendly description
   */
  EafiIccProtocol(final int pcscCode, final String description) {
    insPcscCode = pcscCode;
    insDescription = description;
  } // end constructor */

  /**
   * Pseudo constructor using given protocol number.
   *
   * <p>Because only the four least significant bit of parameter {@code protocolNumber} are taken
   * into account there are no invalid values.
   *
   * @param protocolNumber coding from ISO/IEC 7816-3, only the four least significant bits are
   *     taken into account
   * @return {@link EafiIccProtocol} indicated by given {@code protocolNumber}
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  public static EafiIccProtocol getInstance(final byte protocolNumber) {
    final int masked = protocolNumber & 0xf;

    return switch (masked) {
      case 0 -> T0;
      case 1 -> T1;
      case 2 -> T2;
      case 3 -> T3;
      case 4 -> T4;
      case 5 -> T5;
      case 6 -> T6;
      case 7 -> T7;
      case 8 -> T8;
      case 9 -> T9;
      case 10 -> T10;
      case 11 -> T11;
      case 12 -> T12;
      case 13 -> T13;
      case 14 -> T14;
      default -> T15;
    };
  } // end pseudo constructor */

  /**
   * Pseudo constructor using given PC/SC constant.
   *
   * @param pcscCode coding from PC/SC
   * @return {@link EafiIccProtocol} indicated by given coding
   * @throws IllegalArgumentException if there is no matching protocol
   */
  @SuppressWarnings({"PMD.SingletonClassReturningNewInstance"})
  public static EafiIccProtocol getInstance(final int pcscCode) {
    for (final EafiIccProtocol i : values()) {
      try {
        // Note: getCode() throws an IllegalArgumentException
        //       in case a protocol has no valid PC/SC code
        if (i.getCode() == pcscCode) {
          // ... match
          return i;
        } // end fi
      } catch (IllegalArgumentException e) { // NOPMD avoid empty catch blocks
        // intentionally empty
      } // end Catch (...)
    } // end For (i...)

    throw new IllegalArgumentException("unknown value: " + pcscCode);
  } // end pseudo constructor */

  /**
   * Pseudo constructor given description.
   *
   * <p>If a name or a description equals the given {@code description}, then that protocol is
   * returned. Furthermore, for reason of backward compatibility the {@link String} "DONT_CARE" also
   * translates to {@link EafiIccProtocol#T0_OR_T1 T0_OR_T1}.
   *
   * @param description string representation of description used to construct the result
   * @return {@link EafiIccProtocol} indicated by given {@link String}
   * @throws IllegalArgumentException if there is no matching protocol
   */
  @SuppressWarnings({"PMD.SingletonClassReturningNewInstance"})
  public static EafiIccProtocol getInstance(final String description) {
    // backward compatibility to MicE etc.
    if ("DONT_CARE".equals(description)) { // NOPMD literal in if statement
      return T0_OR_T1;
    } // end fi

    for (final EafiIccProtocol i : values()) {
      if ((i.name().equals(description)) || (i.insDescription.equals(description))) {
        // ... match
        return i;
      } // end fi
    } // end For (i...)

    throw new IllegalArgumentException("unsupported description: " + description);
  } // end pseudo constructor */

  /**
   * Return the PC/SC code of protocol.
   *
   * @return PC/SC code
   * @throws IllegalArgumentException if there is PC/SC code assigned for the protocol
   */
  public int getCode() {
    if (insPcscCode < 0) {
      throw new IllegalArgumentException("no valid PC/SC code for protocol " + this);
    } // end fi

    return insPcscCode;
  } // end method */

  /**
   * Return {@link String} representation of protocol.
   *
   * @return {@link String} representation
   * @see java.lang.Enum#toString()
   */
  @Override
  public String toString() {
    return insDescription;
  } // end method */
} // end class
