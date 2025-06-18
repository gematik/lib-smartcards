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
 * This enumeration covers the life cycle status according to ISO/IEC 7816-4:2020 clause 7.4.10.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public enum LifeCycleStatus {
  /**
   * No information given.
   *
   * <p>This corresponds to value '00', see ISO/IEC 7816-4:2020 tab. 15.
   */
  NO_INFORMATION_GIVEN("no information given"),

  /**
   * Creation state.
   *
   * <p>This corresponds to value '01', see ISO/IEC 7816-4:2020 tab. 15.
   */
  CREATION("creation state"),

  /**
   * Initialisation state.
   *
   * <p>This corresponds to value '03', see ISO/IEC 7816-4:2020 tab. 15.
   */
  INITIALISATION("initialisation state"),

  /**
   * Operational state (activated).
   *
   * <p>This corresponds to values '05' and '07', see ISO/IEC 7816-4:2020 tab. 15.
   */
  ACTIVATED("operational state (activated)"),

  /**
   * Operational state (deactivated).
   *
   * <p>This corresponds to values '04' and '06', see ISO/IEC 7816-4:2020 tab. 15.
   */
  DEACTIVATED("operational state (deactivated)"),

  /**
   * Termination state.
   *
   * <p>This corresponds to values '0c', '0d', '0e' and '0f', see ISO/IEC 7816-4:2020 tab. 15.
   */
  TERMINATED("termination state"),

  /**
   * Proprietary state.
   *
   * <p>This corresponds to a value greater than '0f', see ISO/IEC 7816-4:2020 tab. 15.
   */
  PROPRIETARY("proprietary"),

  /**
   * Reserved for future use.
   *
   * <p>This corresponds to any value not yet defined in ISO/IEC 7816-4:2020 tab. 15.
   */
  RFU("RFU"),
  ;

  /** User friendly description. */
  private final String insDescription; // */

  /**
   * Constructor.
   *
   * @param description user friendly description
   */
  LifeCycleStatus(final String description) {
    insDescription = description;
  } // end constructor */

  /**
   * Pseudo constructor using a life cycle status byte.
   *
   * @param lcs indicating a life cycle status, only the 8 lSBit are taken into account
   * @return corresponding life cycle status
   */
  public static LifeCycleStatus getInstance(final int lcs) {
    final int lifeCycleStatusByte = lcs & 0xff;
    if (0 == lifeCycleStatusByte) {
      // ... b8..b1 = 0 0 0 0 0 0 0 0
      return NO_INFORMATION_GIVEN;
    } else if (1 == lifeCycleStatusByte) { // NOPMD literal in if statement
      // ... b8..b1 = 0 0 0 0 0 0 0 1
      return CREATION;
    } else if (3 == lifeCycleStatusByte) { // NOPMD literal in if statement
      return INITIALISATION;
    } else if (0x4 == (lifeCycleStatusByte & 0xfc)) { // NOPMD literal in if statement
      // ... b8..b1 = 0 0 0 0 0 1 - x
      return (0 == (lifeCycleStatusByte & 0x1)) ? DEACTIVATED : ACTIVATED;
    } else if (0xc == (lifeCycleStatusByte & 0xfc)) { // NOPMD literal in if statement
      // ... b8..b1 = 0 0 0 0 1 1 - -
      return TERMINATED;
    } else if (lifeCycleStatusByte >= 0x10) { // NOPMD literal in if statement
      // ... b8..b5 no all zero, b4..b1 don't care
      return PROPRIETARY;
    } else {
      // ... any other value
      return RFU;
    } // end else
  } // end method */

  /**
   * Return {@link String} representation of life cycle status.
   *
   * @return {@link String} representation
   * @see java.lang.Enum#toString()
   */
  @Override
  public String toString() {
    return insDescription;
  } // end method */
} // end enum
