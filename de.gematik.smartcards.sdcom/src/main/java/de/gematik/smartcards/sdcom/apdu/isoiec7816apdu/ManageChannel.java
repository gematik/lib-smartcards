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
 * This class covers the MANAGE CHANNEL command from [gemSpec_COS#14.9.8], see also ISO/IEC
 * 7816-4:2020 clause 11.2.3.
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
@SuppressWarnings({"PMD.DataClass"})
public final class ManageChannel extends CommandApdu {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 4961594506646411220L; // */

  /** INS code according to ISO/IEC 7816-4. */
  public static final int INS = 0x70;

  /**
   * MANAGE CHANNEL open.
   *
   * <p>Command APDU used to open a logical channel, i.e. {@code '00 70 0000 01'}
   */
  public static final ManageChannel OPEN = new ManageChannel(); // */

  /**
   * MANAGE CHANNEL close.
   *
   * <p>Command APDU used to close a logical channel, i.e. {@code '00 70 8000'}. The channel number
   * in CLA byte has to be adjusted to the number of the channel to be closed.
   */
  public static final ManageChannel CLOSE = new ManageChannel(0x80, 0x00); // */

  /**
   * MANAGE CHANNEL reset.
   *
   * <p>Command APDU used to reset a logical channel, i.e. {@code '00 70 4000'}. The channel number
   * in CLA byte has to be adjusted to the number of the channel to be reset.
   */
  public static final ManageChannel RESET_CHANNEL = new ManageChannel(0x40, 0x00); // */

  /**
   * MANAGE CHANNEL reset application.
   *
   * <p>Command APDU used to reset the application layer i.e. {@code '00 70 4001'}. All channels
   * will be closed and the basic logical channel will be reset.
   */
  public static final ManageChannel RESET_APPLICATION = new ManageChannel(0x40, 0x01); // */

  /**
   * Default constructor used for MANAGE CHANNEL "open".
   *
   * <p>This is case 1 in ISO/IEC 7816-3, command data field absent, Le-field absent.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe, because input parameters are primitive</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive.</i>
   * </ol>
   */
  private ManageChannel() {
    super(0x00, INS, 0x00, 0x00, 1);
  } // end constructor */

  /**
   * Comfort constructor for case 1.
   *
   * <p>This is case 1 in ISO/IEC 7816-3, command data field absent, Le-field absent.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe, because input parameters are primitive</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive.</i>
   * </ol>
   *
   * @param p1 the parameter byte P1, only the eight leas significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight leas significant bit are taken into account
   */
  private ManageChannel(final int p1, final int p2) {
    super(0x00, INS, p1, p2);
  } // end constructor */
} // end class
