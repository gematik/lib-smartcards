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
package de.gematik.smartcards.g2icc.proxy;

import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_S_SUCCESS;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SCOPE_SYSTEM;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;

import de.gematik.smartcards.pcsc.lib.Dword;
import de.gematik.smartcards.pcsc.lib.ScardContextByReference;
import de.gematik.smartcards.pcsc.lib.WinscardLibraryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class checking if PC/SC functionality is available.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class PcscStatus {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(PcscStatus.class); // */

  /**
   * Constant indicating if the Smart card resource manager is running.
   *
   * <p>{@code TRUE} indicates that the Smart card resource manager is running. In that case "real"
   * PC/SC readers could be used for testing.
   *
   * <p>{@code FALSE} indicated that the Smart card resource manager is not running. In that case no
   * "real" PC/SC readers could be used for testing.
   */
  private static final boolean SMART_CARD_RESOURCE_MANAGER_RUNNING; // NOPMD long name */

  /*
   * static
   */
  static {
    boolean scmrRunning = false;
    try {
      // --- check if Smart card resource manager is running
      final WinscardLibraryImpl library = WinscardLibraryImpl.openLib();
      final ScardContextByReference phContext = new ScardContextByReference();
      final int code =
          library.scardEstablishContext(
              new Dword(SCARD_SCOPE_SYSTEM), // scope
              null, // phReserved1
              null, // phReserved2
              phContext); // phContext
      scmrRunning = SCARD_S_SUCCESS == code;
    } catch (UnsatisfiedLinkError e) {
      LOGGER.atWarn().log(UNEXPECTED, e);
    } // end Catch (...)

    SMART_CARD_RESOURCE_MANAGER_RUNNING = scmrRunning;
  } // end static */

  /** Default constructor. */
  private PcscStatus() {
    // intentionally empty
  } // end constructor */

  /**
   * Indicates if a Smart card resource manager is running.
   *
   * @return {@code TRUE} if the Smart card resource manager is running. In that case "real" PC/SC
   *     readers could be used for testing, {@code FALSE} otherwise
   */
  public static boolean isSmartCardResourceManagerRunning() {
    return SMART_CARD_RESOURCE_MANAGER_RUNNING;
  } // end method */
} // end class
