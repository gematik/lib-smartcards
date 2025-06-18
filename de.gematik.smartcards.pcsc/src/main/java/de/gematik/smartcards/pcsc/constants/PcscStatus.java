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
package de.gematik.smartcards.pcsc.constants;

import com.sun.jna.Platform;
import de.gematik.smartcards.pcsc.PcscException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class contains error codes from <a
 * href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>.
 *
 * <p>Per error code three things are relevant:
 *
 * <ol>
 *   <li>name of constant, e.g. "SCARD_F_INTERNAL_ERROR"
 *   <li>corresponding error code, e.g. 0x80100001
 *   <li>error description, e.g. "An internal consistency check failed."
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
 * @see <a href="https://msdn.microsoft.com/en-us/library/aa924526.aspx">MSDN Smart Card Error
 *     Values</a>
 */
@SuppressWarnings({"PMD.LongVariable"})
public final class PcscStatus {

  /** Flag indicating a Windows platform. */
  public static final boolean OS_WINDOWS = Platform.isWindows(); // */

  /** Flag indicating a Linux platform. */
  public static final boolean OS_LINUX = Platform.isLinux(); // */

  /** Flag indicating a Mac platform. */
  public static final boolean OS_MAC = Platform.isMac(); // */

  // Note 1: The sequence hereafter is identical to pscslite.h.in.
  // Note 2: Unless otherwise stated definitions are identical in
  //         pcsclite.h.in and MSDN, see also
  //         https://pcsclite.apdu.fr/

  /**
   * SCARD_S_SUCCESS.
   *
   * <p><i><b>Note:</b> This {@code code} is defined in {@code pcsdllite.h.in} but not defined in <a
   * href="https://msdn.microsoft.com/en-us/library/aa924526.aspx">MSDN Smart Card Error
   * Values</a></i>
   */
  public static final int SCARD_S_SUCCESS = 0x00000000; // */

  /** SCARD_F_INTERNAL_ERROR. */
  public static final int SCARD_F_INTERNAL_ERROR = 0x80100001; // */

  /** SCARD_E_CANCELLED. */
  public static final int SCARD_E_CANCELLED = 0x80100002; // */

  /** SCARD_E_INVALID_HANDLE. */
  public static final int SCARD_E_INVALID_HANDLE = 0x80100003; // */

  /** SCARD_E_INVALID_PARAMETER. */
  public static final int SCARD_E_INVALID_PARAMETER = 0x80100004; // */

  /** SCARD_E_INVALID_TARGET. */
  public static final int SCARD_E_INVALID_TARGET = 0x80100005; // */

  /** SCARD_E_NO_MEMORY. */
  public static final int SCARD_E_NO_MEMORY = 0x80100006; // */

  /** SCARD_F_WAITED_TOO_LONG. */
  public static final int SCARD_F_WAITED_TOO_LONG = 0x80100007; // */

  /** SCARD_E_INSUFFICIENT_BUFFER. */
  public static final int SCARD_E_INSUFFICIENT_BUFFER = 0x80100008; // */

  /** SCARD_E_UNKNOWN_READER. */
  public static final int SCARD_E_UNKNOWN_READER = 0x80100009; // */

  /** SCARD_E_TIMEOUT. */
  public static final int SCARD_E_TIMEOUT = 0x8010000A; // */

  /** SCARD_E_SHARING_VIOLATION. */
  public static final int SCARD_E_SHARING_VIOLATION = 0x8010000B; // */

  /** SCARD_E_NO_SMARTCARD. */
  public static final int SCARD_E_NO_SMARTCARD = 0x8010000C; // */

  /** SCARD_E_UNKNOWN_CARD. */
  public static final int SCARD_E_UNKNOWN_CARD = 0x8010000D; // */

  /** SCARD_E_CANT_DISPOSE. */
  public static final int SCARD_E_CANT_DISPOSE = 0x8010000E; // */

  /** SCARD_E_PROTO_MISMATCH. */
  public static final int SCARD_E_PROTO_MISMATCH = 0x8010000F; // */

  /** SCARD_E_NOT_READY. */
  public static final int SCARD_E_NOT_READY = 0x80100010; // */

  /** SCARD_E_INVALID_VALUE. */
  public static final int SCARD_E_INVALID_VALUE = 0x80100011; // */

  /** SCARD_E_SYSTEM_CANCELLED. */
  public static final int SCARD_E_SYSTEM_CANCELLED = 0x80100012; // */

  /** SCARD_F_COMM_ERROR. */
  public static final int SCARD_F_COMM_ERROR = 0x80100013; // */

  /** SCARD_F_UNKNOWN_ERROR. */
  public static final int SCARD_F_UNKNOWN_ERROR = 0x80100014; // */

  /** SCARD_E_INVALID_ATR. */
  public static final int SCARD_E_INVALID_ATR = 0x80100015; // */

  /** SCARD_E_NOT_TRANSACTED. */
  public static final int SCARD_E_NOT_TRANSACTED = 0x80100016; // */

  /** SCARD_E_READER_UNAVAILABLE. */
  public static final int SCARD_E_READER_UNAVAILABLE = 0x80100017; // */

  /** SCARD_P_SHUTDOWN. */
  public static final int SCARD_P_SHUTDOWN = 0x80100018; // */

  /** SCARD_E_PCI_TOO_SMALL. */
  public static final int SCARD_E_PCI_TOO_SMALL = 0x80100019; // */

  /** SCARD_E_READER_UNSUPPORTED. */
  public static final int SCARD_E_READER_UNSUPPORTED = 0x8010001A; // */

  /** SCARD_E_DUPLICATE_READER. */
  public static final int SCARD_E_DUPLICATE_READER = 0x8010001B; // */

  /** SCARD_E_CARD_UNSUPPORTED. */
  public static final int SCARD_E_CARD_UNSUPPORTED = 0x8010001C; // */

  /** SCARD_E_NO_SERVICE. */
  public static final int SCARD_E_NO_SERVICE = 0x8010001D; // */

  /** SCARD_E_SERVICE_STOPPED. */
  public static final int SCARD_E_SERVICE_STOPPED = 0x8010001E; // */

  /**
   * SCARD_E_UNEXPECTED.
   *
   * <p>The following passage is a cite from <a
   * href="https://sourceforge.net/p/rdesktop/bugs/319/">sourceforge</a>:
   *
   * <p><b>begin cite:</b><br>
   * {@link #SCARD_E_UNSUPPORTED_FEATURE} has not the same value on Windows winscard and pcsc-lite.
   *
   * <p><b>Windows has:</b>
   *
   * <pre>
   * #define SCARD_E_UNSUPPORTED_FEATURE 0x80100022
   * #define SCARD_E_UNEXPECTED 0x8010001F
   * </pre>
   *
   * <p><b>pcsc-lite has:</b>
   *
   * <pre>
   * #define SCARD_E_UNSUPPORTED_FEATURE 0x8010001F
   * #define SCARD_E_UNEXPECTED 0x8010001F
   * </pre>
   *
   * <p>Yes, the same value is used for two different names. This is an historic issue.
   *
   * <p>pcsc-lite never returns <i>SCARD_E_UNEXPECTED</i> so when {@code 0x8010001F} is returned it
   * is in fact {@link #SCARD_E_UNSUPPORTED_FEATURE}. And this {@link #SCARD_E_UNSUPPORTED_FEATURE}
   * {@code (0x8010001F)} should be converted to a Windows {@link #SCARD_E_UNSUPPORTED_FEATURE}
   * {@code (0x80100022)} by rdekstop. Otherwise, the Windows application will get a
   * <i>SCARD_E_UNEXPECTED</i> {@code (0x8010001F)} when a {@link #SCARD_E_UNSUPPORTED_FEATURE} is
   * expected {@code (0x80100022)}.<br>
   * <b>end cite.</b>
   */
  public static final int SCARD_E_UNEXPECTED = OS_WINDOWS ? 0x8010001F : SCARD_S_SUCCESS; // */

  /**
   * SCARD_E_UNSUPPORTED_FEATURE.
   *
   * <p>This error code has values which depend on the machine, see {@link #SCARD_E_UNEXPECTED}.
   */
  public static final int SCARD_E_UNSUPPORTED_FEATURE = OS_WINDOWS ? 0x80100022 : 0x8010001F; // */

  /** SCARD_E_ICC_INSTALLATION. */
  public static final int SCARD_E_ICC_INSTALLATION = 0x80100020; // */

  /** SCARD_E_ICC_CREATEORDER. */
  public static final int SCARD_E_ICC_CREATEORDER = 0x80100021; // */

  /** SCARD_E_DIR_NOT_FOUND. */
  public static final int SCARD_E_DIR_NOT_FOUND = 0x80100023; // */

  /** SCARD_E_FILE_NOT_FOUND. */
  public static final int SCARD_E_FILE_NOT_FOUND = 0x80100024; // */

  /** SCARD_E_NO_DIR. */
  public static final int SCARD_E_NO_DIR = 0x80100025; // */

  /** SCARD_E_NO_FILE. */
  public static final int SCARD_E_NO_FILE = 0x80100026; // */

  /** SCARD_E_NO_ACCESS. */
  public static final int SCARD_E_NO_ACCESS = 0x80100027; // */

  /** SCARD_E_WRITE_TOO_MANY. */
  public static final int SCARD_E_WRITE_TOO_MANY = 0x80100028; // */

  /** SCARD_E_BAD_SEEK. */
  public static final int SCARD_E_BAD_SEEK = 0x80100029; // */

  /** SCARD_E_INVALID_CHV. */
  public static final int SCARD_E_INVALID_CHV = 0x8010002A; // */

  /** SCARD_E_UNKNOWN_RES_MNG. */
  public static final int SCARD_E_UNKNOWN_RES_MNG = 0x8010002B; // */

  /** SCARD_E_NO_SUCH_CERTIFICATE. */
  public static final int SCARD_E_NO_SUCH_CERTIFICATE = 0x8010002C; // */

  /** SCARD_E_CERTIFICATE_UNAVAILABLE. */
  public static final int SCARD_E_CERTIFICATE_UNAVAILABLE = 0x8010002D;

  /** SCARD_E_NO_READERS_AVAILABLE. */
  public static final int SCARD_E_NO_READERS_AVAILABLE = 0x8010002E; // */

  /** SCARD_E_COMM_DATA_LOST. */
  public static final int SCARD_E_COMM_DATA_LOST = 0x8010002F; // */

  /** SCARD_E_NO_KEY_CONTAINER. */
  public static final int SCARD_E_NO_KEY_CONTAINER = 0x80100030; // */

  /** SCARD_E_SERVER_TOO_BUSY. */
  public static final int SCARD_E_SERVER_TOO_BUSY = 0x80100031; // */

  /** SCARD_W_UNSUPPORTED_CARD. */
  public static final int SCARD_W_UNSUPPORTED_CARD = 0x80100065; // */

  /** SCARD_W_UNRESPONSIVE_CARD. */
  public static final int SCARD_W_UNRESPONSIVE_CARD = 0x80100066; // */

  /** SCARD_W_UNPOWERED_CARD. */
  public static final int SCARD_W_UNPOWERED_CARD = 0x80100067; // */

  /** SCARD_W_RESET_CARD. */
  public static final int SCARD_W_RESET_CARD = 0x80100068; // */

  /** SCARD_W_REMOVED_CARD. */
  public static final int SCARD_W_REMOVED_CARD = 0x80100069; // */

  /** SCARD_W_SECURITY_VIOLATION. */
  public static final int SCARD_W_SECURITY_VIOLATION = 0x8010006A; // */

  /** SCARD_W_WRONG_CHV. */
  public static final int SCARD_W_WRONG_CHV = 0x8010006B; // */

  /** SCARD_W_CHV_BLOCKED. */
  public static final int SCARD_W_CHV_BLOCKED = 0x8010006C; // */

  /** SCARD_W_EOF. */
  public static final int SCARD_W_EOF = 0x8010006D; // */

  /** SCARD_W_CANCELLED_BY_USER. */
  public static final int SCARD_W_CANCELLED_BY_USER = 0x8010006E; // */

  /** SCARD_W_CARD_NOT_AUTHENTICATED. */
  public static final int SCARD_W_CARD_NOT_AUTHENTICATED = 0x8010006F;

  /**
   * ERROR_INVALID_HANDLE.
   *
   * <p><i><b>Note:</b> This {@code code} is defined in <a
   * href=https://github.com/jnasmartcardio/jnasmartcardio.git>JnaSmartcardIO</a>. This {@code code}
   * is neither defined in {@code pcsdllite.h.in} nor in <a
   * href=https://msdn.microsoft.com/en-us/library/aa924526.aspx>MSDN Smart Card Error Values</a>.
   * </i>
   */
  public static final int ERROR_INVALID_HANDLE = 0x06; // */

  /**
   * ERROR_BAD_COMMAND.
   *
   * <p><i><b>Note:</b> This {@code code} is defined in <a
   * href=https://github.com/jnasmartcardio/jnasmartcardio.git>JnaSmartcardIO</a>. This {@code code}
   * is neither defined in {@code pcsdllite.h.in} nor in <a
   * href=https://msdn.microsoft.com/en-us/library/aa924526.aspx>MSDN Smart Card Error Values</a>.
   * </i>
   */
  public static final int ERROR_BAD_COMMAND = 0x16; // */

  /** Mapping from status code to status-name and status-description. */
  private static final List<Map.Entry<Integer, List<String>>> MAPPING =
      List.<Map.Entry<Integer, List<String>>>of(
          Map.entry(SCARD_S_SUCCESS, List.of("SCARD_S_SUCCESS", "No error was encountered.")),
          Map.entry(
              SCARD_F_INTERNAL_ERROR,
              List.of("SCARD_F_INTERNAL_ERROR", "An internal consistency check failed.")),
          Map.entry(
              SCARD_E_CANCELLED,
              List.of("SCARD_E_CANCELLED", "The action was cancelled by an SCardCancel request.")),
          Map.entry(
              SCARD_E_INVALID_HANDLE,
              List.of("SCARD_E_INVALID_HANDLE", "The supplied handle was invalid.")),
          Map.entry(
              SCARD_E_INVALID_PARAMETER,
              List.of(
                  "SCARD_E_INVALID_PARAMETER",
                  "One or more of the supplied parameters could not be properly interpreted.")),
          Map.entry(
              SCARD_E_INVALID_TARGET,
              List.of(
                  "SCARD_E_INVALID_TARGET", "Registry startup information is missing or invalid.")),
          Map.entry(
              SCARD_E_NO_MEMORY,
              List.of(
                  "SCARD_E_NO_MEMORY", "Not enough memory available to complete this command.")),
          Map.entry(
              SCARD_F_WAITED_TOO_LONG,
              List.of("SCARD_F_WAITED_TOO_LONG", "An internal consistency timer has expired.")),
          Map.entry(
              SCARD_E_INSUFFICIENT_BUFFER,
              List.of(
                  "SCARD_E_INSUFFICIENT_BUFFER",
                  "The data buffer to receive returned data is too small for the returned data.")),
          Map.entry(
              SCARD_E_UNKNOWN_READER,
              List.of("SCARD_E_UNKNOWN_READER", "The specified reader name is not recognized.")),
          Map.entry(
              SCARD_E_TIMEOUT,
              List.of("SCARD_E_TIMEOUT", "The user-specified timeout value has expired.")),
          Map.entry(
              SCARD_E_SHARING_VIOLATION,
              List.of(
                  "SCARD_E_SHARING_VIOLATION",
                  "The smart card cannot be accessed because of other connections outstanding.")),
          Map.entry(
              SCARD_E_NO_SMARTCARD,
              List.of(
                  "SCARD_E_NO_SMARTCARD",
                  "The operation requires a Smart Card,"
                      + " but no Smart Card is currently in the device.")),
          Map.entry(
              SCARD_E_UNKNOWN_CARD,
              List.of("SCARD_E_UNKNOWN_CARD", "The specified smart card name is not recognized.")),
          Map.entry(
              SCARD_E_CANT_DISPOSE,
              List.of(
                  "SCARD_E_CANT_DISPOSE",
                  "The system could not dispose of the media in the requested manner.")),
          Map.entry(
              SCARD_E_PROTO_MISMATCH,
              List.of(
                  "SCARD_E_PROTO_MISMATCH",
                  "The requested protocols are incompatible with the protocol currently"
                      + " in use with the smart card.")),
          Map.entry(
              SCARD_E_NOT_READY,
              List.of(
                  "SCARD_E_NOT_READY",
                  "The reader or smart card is not ready to accept commands.")),
          Map.entry(
              SCARD_E_INVALID_VALUE,
              List.of(
                  "SCARD_E_INVALID_VALUE",
                  "One or more of the supplied parameters values could not be"
                      + " properly interpreted.")),
          Map.entry(
              SCARD_E_SYSTEM_CANCELLED,
              List.of(
                  "SCARD_E_SYSTEM_CANCELLED",
                  "The action was cancelled by the system, presumably to log off or shut down.")),
          Map.entry(
              SCARD_F_COMM_ERROR,
              List.of("SCARD_F_COMM_ERROR", "An internal communications error has been detected.")),
          Map.entry(
              SCARD_F_UNKNOWN_ERROR,
              List.of(
                  "SCARD_F_UNKNOWN_ERROR",
                  "An internal error has been detected, but the source is unknown.")),
          Map.entry(
              SCARD_E_INVALID_ATR,
              List.of(
                  "SCARD_E_INVALID_ATR",
                  "An ATR obtained from the registry is not a valid ATR string.")),
          Map.entry(
              SCARD_E_NOT_TRANSACTED,
              List.of(
                  "SCARD_E_NOT_TRANSACTED",
                  "An attempt was made to end a non-existent transaction.")),
          Map.entry(
              SCARD_E_READER_UNAVAILABLE,
              List.of(
                  "SCARD_E_READER_UNAVAILABLE",
                  "The specified reader is not currently available for use.")),
          Map.entry(
              SCARD_P_SHUTDOWN,
              List.of(
                  "SCARD_P_SHUTDOWN",
                  "The operation has been aborted to allow the server application to exit.")),
          Map.entry(
              SCARD_E_PCI_TOO_SMALL,
              List.of("SCARD_E_PCI_TOO_SMALL", "The PCI Receive buffer was too small.")),
          Map.entry(
              SCARD_E_READER_UNSUPPORTED,
              List.of(
                  "SCARD_E_READER_UNSUPPORTED",
                  "The reader driver does not meet minimal requirements for support.")),
          Map.entry(
              SCARD_E_DUPLICATE_READER,
              List.of(
                  "SCARD_E_DUPLICATE_READER",
                  "The reader driver did not produce a unique reader name.")),
          Map.entry(
              SCARD_E_CARD_UNSUPPORTED,
              List.of(
                  "SCARD_E_CARD_UNSUPPORTED",
                  "The smart card does not meet minimal requirements for support.")),
          Map.entry(
              SCARD_E_NO_SERVICE,
              List.of("SCARD_E_NO_SERVICE", "The Smart card resource manager is not running.")),
          Map.entry(
              SCARD_E_SERVICE_STOPPED,
              List.of("SCARD_E_SERVICE_STOPPED", "The Smart card resource manager has shut down.")),
          Map.entry(
              SCARD_E_UNEXPECTED,
              List.of("SCARD_E_UNEXPECTED", "An unexpected card error has occurred.")),
          Map.entry(
              SCARD_E_UNSUPPORTED_FEATURE,
              List.of(
                  "SCARD_E_UNSUPPORTED_FEATURE",
                  "This smart card does not support the requested feature.")),
          Map.entry(
              SCARD_E_ICC_INSTALLATION,
              List.of(
                  "SCARD_E_ICC_INSTALLATION",
                  "No primary provider can be found for the smart card.")),
          Map.entry(
              SCARD_E_ICC_CREATEORDER,
              List.of(
                  "SCARD_E_ICC_CREATEORDER",
                  "The requested order of object creation is not supported.")),
          Map.entry(
              SCARD_E_DIR_NOT_FOUND,
              List.of(
                  "SCARD_E_DIR_NOT_FOUND",
                  "The identified directory does not exist in the smart card.")),
          Map.entry(
              SCARD_E_FILE_NOT_FOUND,
              List.of(
                  "SCARD_E_FILE_NOT_FOUND",
                  "The identified file does not exist in the smart card.")),
          Map.entry(
              SCARD_E_NO_DIR,
              List.of(
                  "SCARD_E_NO_DIR",
                  "The supplied path does not represent a smart card directory.")),
          Map.entry(
              SCARD_E_NO_FILE,
              List.of(
                  "SCARD_E_NO_FILE", "The supplied path does not represent a smart card file.")),
          Map.entry(
              SCARD_E_NO_ACCESS, List.of("SCARD_E_NO_ACCESS", "Access is denied to this file.")),
          Map.entry(
              SCARD_E_WRITE_TOO_MANY,
              List.of(
                  "SCARD_E_WRITE_TOO_MANY",
                  "The smart card does not have enough memory to store the information.")),
          Map.entry(
              SCARD_E_BAD_SEEK,
              List.of(
                  "SCARD_E_BAD_SEEK",
                  "There was an error trying to set the smart card file object pointer.")),
          Map.entry(
              SCARD_E_INVALID_CHV,
              List.of("SCARD_E_INVALID_CHV", "The supplied PIN is incorrect.")),
          Map.entry(
              SCARD_E_UNKNOWN_RES_MNG,
              List.of(
                  "SCARD_E_UNKNOWN_RES_MNG",
                  "An unrecognized error code was returned from a layered component.")),
          Map.entry(
              SCARD_E_NO_SUCH_CERTIFICATE,
              List.of("SCARD_E_NO_SUCH_CERTIFICATE", "The requested certificate does not exist.")),
          Map.entry(
              SCARD_E_CERTIFICATE_UNAVAILABLE,
              List.of(
                  "SCARD_E_CERTIFICATE_UNAVAILABLE",
                  "The requested certificate could not be obtained.")),
          Map.entry(
              SCARD_E_NO_READERS_AVAILABLE,
              List.of("SCARD_E_NO_READERS_AVAILABLE", "Cannot find a smart card reader.")),
          Map.entry(
              SCARD_E_COMM_DATA_LOST,
              List.of(
                  "SCARD_E_COMM_DATA_LOST",
                  "A communications error with the smart card has been detected."
                      + " Retry the operation.")),
          Map.entry(
              SCARD_E_NO_KEY_CONTAINER,
              List.of(
                  "SCARD_E_NO_KEY_CONTAINER",
                  "The requested key container does not exist on the smart card.")),
          Map.entry(
              SCARD_E_SERVER_TOO_BUSY,
              List.of(
                  "SCARD_E_SERVER_TOO_BUSY",
                  "The Smart Card Resource Manager is too busy to complete this operation.")),
          Map.entry(
              SCARD_W_UNSUPPORTED_CARD,
              List.of(
                  "SCARD_W_UNSUPPORTED_CARD",
                  "The reader cannot communicate with the card,"
                      + " due to ATR string configuration conflicts.")),
          Map.entry(
              SCARD_W_UNRESPONSIVE_CARD,
              List.of("SCARD_W_UNRESPONSIVE_CARD", "The smart card is not responding to a reset.")),
          Map.entry(
              SCARD_W_UNPOWERED_CARD,
              List.of(
                  "SCARD_W_UNPOWERED_CARD",
                  "Power has been removed from the smart card,"
                      + " so that further communication is not possible.")),
          Map.entry(
              SCARD_W_RESET_CARD,
              List.of(
                  "SCARD_W_RESET_CARD",
                  "The smart card has been reset, so any shared state information is invalid.")),
          Map.entry(
              SCARD_W_REMOVED_CARD,
              List.of(
                  "SCARD_W_REMOVED_CARD",
                  "The smart card has been removed, so further communication is not possible.")),
          Map.entry(
              SCARD_W_SECURITY_VIOLATION,
              List.of(
                  "SCARD_W_SECURITY_VIOLATION",
                  "Access was denied because of a security violation.")),
          Map.entry(
              SCARD_W_WRONG_CHV,
              List.of(
                  "SCARD_W_WRONG_CHV",
                  "The card cannot be accessed because the wrong PIN was presented.")),
          Map.entry(
              SCARD_W_CHV_BLOCKED,
              List.of(
                  "SCARD_W_CHV_BLOCKED",
                  "The card cannot be accessed because the maximum number of"
                      + " PIN entry attempts has been reached.")),
          Map.entry(
              SCARD_W_EOF,
              List.of("SCARD_W_EOF", "The end of the smart card file has been reached.")),

          // Note: The description differs between "pcsclite.h.in" and Windows:
          //       pcsclite: The user pressed "Cancel" on a Smart Card Selection Dialog.
          //       Windows:  The action was cancelled by the user.
          Map.entry(
              SCARD_W_CANCELLED_BY_USER,
              List.of(
                  "SCARD_W_CANCELLED_BY_USER",
                  "The user pressed \"Cancel\" on a Smart Card Selection Dialog.")),
          Map.entry(
              SCARD_W_CARD_NOT_AUTHENTICATED,
              List.of("SCARD_W_CARD_NOT_AUTHENTICATED", "No PIN was presented to the smart card.")),
          Map.entry(
              ERROR_INVALID_HANDLE, List.of("ERROR_INVALID_HANDLE", "The handle is invalid.")),
          Map.entry(
              ERROR_BAD_COMMAND,
              List.of("ERROR_BAD_COMMAND", "The device does not recognize the command."))); // */

  /** Mapping from status-code to status-name. */
  @VisibleForTesting // otherwise = private
  /* package */ static final Map<Integer, String> NAME = getMappingName(); // */

  /** Mapping from status-code to status-description. */
  @VisibleForTesting // otherwise = private
  /* package */ static final Map<Integer, String> DESCRIPTION = getMappingDescription(); // */

  /** Default constructor. */
  private PcscStatus() {
    // intentionally empty
  } // end constructor */

  /** Initialize {@link #NAME}. */
  private static Map<Integer, String> getMappingName() {
    final Map<Integer, String> result = new ConcurrentHashMap<>();

    MAPPING.forEach(
        entry -> {
          final int code = entry.getKey();

          if (!result.containsKey(code)) {
            result.put(code, entry.getValue().getFirst());
          } // end fi
        }); // end forEach(entry -> ...)

    return Collections.unmodifiableMap(result);
  } // end method */

  /** Initialize {@link #DESCRIPTION}. */
  private static Map<Integer, String> getMappingDescription() {
    final Map<Integer, String> result = new ConcurrentHashMap<>();

    MAPPING.forEach(
        entry -> {
          final int code = entry.getKey();

          if (!result.containsKey(code)) {
            result.put(code, entry.getValue().get(1));
          } // end fi
        }); // end forEach(entry -> ...)

    return Collections.unmodifiableMap(result);
  } // end method */

  /**
   * Checks code.
   *
   * @param message included in exception if code does not represent {@link #SCARD_S_SUCCESS}
   * @param code to be checked and converted into a corresponding exception message
   * @throws PcscException if {@code code} does not represent {@link #SCARD_S_SUCCESS}, method
   *     {@link Exception#getMessage() getMessage()} will provide an appropriate description of the
   *     error situation
   */
  public static void check(final String message, final int code) throws PcscException {
    if (SCARD_S_SUCCESS != code) {
      // ... no success
      //     => throw appropriate exception
      throw new PcscException(code, message + ": " + getExplanation(code));
    } // end fi
  } // end method */

  /**
   * Retrieve an explanation from a numerical error code.
   *
   * @param code numerical error code
   * @return corresponding description
   */
  public static String getExplanation(final int code) {
    final String name = NAME.get(code);
    final String description = DESCRIPTION.get(code);

    if (null == name) {
      return String.format("no explanation for code = 0x%x = %d", code, code);
    } else {
      return name + " -> " + description;
    } // end else
  } // end method */
} // end class
