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
package de.gematik.smartcards.pcsc;

import static de.gematik.smartcards.pcsc.constants.PcscStatus.SCARD_S_SUCCESS;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_SCOPE_SYSTEM;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;

import de.gematik.smartcards.pcsc.lib.Dword;
import de.gematik.smartcards.pcsc.lib.ScardContextByReference;
import de.gematik.smartcards.pcsc.lib.WinscardLibraryImpl;
import javax.smartcardio.TerminalFactory;
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

  /**
   * Constant indicating if a PC/SC provider is available.
   *
   * <p>{@code TRUE} indicates that a PC/SC provider is available and can be used to establish
   * connections to a smart card.
   *
   * <p>{@code FALSE} indicates that no PC/SC provider is available.
   */
  private static final boolean PCSC_PROVIDER_AVAILABLE; // */

  /*
   * static
   */
  static {
    boolean scmrRunning = false;
    boolean pcscAvailable = false;
    try {
      // --- check if PC/SC provider is available
      final var providerName = TerminalFactory.getDefault().getProvider().getName();
      pcscAvailable = !"None".equals(providerName);

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

    PCSC_PROVIDER_AVAILABLE = pcscAvailable;
    SMART_CARD_RESOURCE_MANAGER_RUNNING = scmrRunning;
  } // end static */

  /** Default constructor. */
  private PcscStatus() {
    // intentionally empty
  } // end constructor */

  /**
   * Indicates if everything is available.
   *
   * @return {@link #isSmartCardResourceManagerRunning()} AND {@link #isPcscProviderAvailable()}
   */
  public static boolean isEverythingAvailable() {
    return isPcscProviderAvailable() && isSmartCardResourceManagerRunning();
  } // end method */

  /**
   * Indicates if a default PC/SC provider is available.
   *
   * @return {@code TRUE} if a PC/SC provider is available and can be used to establish connections
   *     to a smart card, {@code FALSE} otherwise
   */
  public static boolean isPcscProviderAvailable() {
    return PCSC_PROVIDER_AVAILABLE;
  } // end method */

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
