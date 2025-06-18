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
package de.gematik.smartcards.pcsc.lib;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import de.gematik.smartcards.pcsc.constants.PcscStatus;
import de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.ByteBuffer;

/**
 * The winscard API, also known as PC/SC.
 *
 * <p>Implementations of this API exist on Windows, OS X, and Linux, although the symbol names and
 * sizeof parameters differs on different platforms.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
// Note: The name of methods in this interface SHALL exactly match those names
//        in the library. Otherwise, those methods are not found.
//        Because these method names do not match checkstyle and pmd requirements,
//        the corresponding findings are suppressed here.
@SuppressWarnings({
  "PMD.MethodNamingConventions", // method name
  "checkstyle:abbreviationaswordinname", // more than one uppercase letter
  "checkstyle:methodname", // method name starts with an uppercase letter
  "checkstyle:parametername", // diverge from pattern '^[a-z]([a-z0-9][a-zA-Z0-9]*)?$'
})
public interface WinscardLibrary extends Library {

  /**
   * Infinite timeout.
   *
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int INFINITE = -1; // */

  /**
   * Special value used to get notified of he arrival of another smart card reader.
   *
   * <p>This special value is used in {@link WinscardLibrary#SCardGetStatusChange(ScardContext,
   * Dword, ScardReaderState[], Dword)} for being notified of the arrival of a new smart card
   * reader.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardgetstatuschangea">MSDN
   *     SCardGetStatusChange</a>
   */
  String PNP_READER_ID = "\\\\?PnP?\\Notification"; // */

  /**
   * Special value used when indicating the length of an output buffer.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Mac OS X does not (yet) support memory (auto) allocation in the PC/SC layer. So you
   *       have to use a double call mechanism. One first call to get the size to allocate and one
   *       second call with a buffer allocated at the correct size, see <a
   *       href="https://ludovicrousseau.blogspot.de/2010/04/pcsc-sample-in-c.html">Ludovic
   *       Rousseau's blog</a></i>
   *   <li>Auto-allocation seems not to work with JAVA.
   * </ol>
   *
   * @see <a
   *     href="https://docs.rs/smartcard/0.1.0/smartcard/scard/winscard/constant.SCARD_AUTOALLOCATE.html">MSDN
   *     definition</a>
   */
  Dword SCARD_AUTOALLOCATE = new Dword(-1); // */

  // ===============================================================================================
  // begin section with names of reader groups

  /**
   * Group used when no group name is provided when listing readers.
   *
   * <p>Returns a list of all readers, regardless of what group or groups the readers are in.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardlistreadersa">MSDN
   *     SCardListReaders function</a>
   */
  String SCARD_ALL_READERS = "SCard$AllReaders\000"; // */

  /**
   * Default group to which all readers are added when introduced into the system.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardlistreadersa">MSDN
   *     SCardListReaders function</a>
   */
  String SCARD_DEFAULT_READERS = "SCard$DefaultReaders\000"; // */

  /**
   * Unused legacy value.
   *
   * <p>This is an internally managed group that cannot be modified by using any reader group APIs.
   * It is intended to be used for enumeration only.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardlistreadersa">MSDN
   *     SCardListReaders function</a>
   * @deprecated i.e. legacy value
   */
  // Note 1: SonarQube claims the following major finding:
  //         "Do not forget to remove this deprecated code someday."
  // Note 2: This constant remains until it is deleted by Microsoft.
  @Deprecated(since = "legacy value")
  String SCARD_LOCAL_READERS = "SCard$LocalReaders\000"; // */

  /**
   * Unused legacy value.
   *
   * <p>This is an internally managed group that cannot be modified by using any reader group APIs.
   * It is intended to be used for enumeration only.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardlistreadersa">MSDN
   *     SCardListReaders function</a>
   * @deprecated i.e. legacy value
   */
  // Note 1: SonarQube claims the following major finding:
  //         "Do not forget to remove this deprecated code someday."
  // Note 2: This constant remains until it is deleted by Microsoft.
  @Deprecated(since = "legacy value")
  String SCARD_SYSTEM_READERS = "SCard$SystemReaders\000"; // */

  // end   section with names of reader groups
  // ===============================================================================================
  // begin section with smart card protocols

  /**
   * SCARD_PROTOCOL_UNDEFINED.
   *
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardconnectw">MSDN
   *     SCardConnect function</a>
   */
  int SCARD_PROTOCOL_UNDEFINED = 0; // */

  /**
   * SCARD_PROTOCOL_T0.
   *
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardconnectw">MSDN
   *     SCardConnect function</a>
   */
  int SCARD_PROTOCOL_T0 = 1; // */

  /**
   * SCARD_PROTOCOL_T1.
   *
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardconnectw">MSDN
   *     SCardConnect function</a>
   */
  int SCARD_PROTOCOL_T1 = 2; // */

  /**
   * SCARD_PROTOCOL_RAW.
   *
   * <p>Raw active protocol.
   *
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_PROTOCOL_RAW = 4; // */

  // end   section with smart card protocols
  // ===============================================================================================
  // begin section with disconnect options

  /**
   * SCARD_LEAVE_CARD.
   *
   * <p>Do not do anything special.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scarddisconnect">MSDN
   *     SCardDisconnect function</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_LEAVE_CARD = 0; // */

  /**
   * SCARD_RESET_CARD.
   *
   * <p>Reset the card.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scarddisconnect">MSDN
   *     SCardDisconnect function</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_RESET_CARD = 1; // */

  /**
   * SCARD_UNPOWER_CARD.
   *
   * <p>Power down the card.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scarddisconnect">MSDN
   *     SCardDisconnect function</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_UNPOWER_CARD = 2; // */

  /**
   * SCARD_EJECT_CARD.
   *
   * <p>Eject the card.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scarddisconnect">MSDN
   *     SCardDisconnect function</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_EJECT_CARD = 3; // */

  // end   section with disconnect options
  // ===============================================================================================
  // begin section with scopes for establishing a context

  /**
   * Scope user.
   *
   * <p>Database operations are performed within the domain of the user, see {@link
   * WinscardLibrary#SCardEstablishContext(Dword, Pointer, Pointer, ScardContextByReference)}
   *
   * @see <a
   *     href="https://docs.microsoft.com/de-de/windows/win32/api/winscard/nf-winscard-scardestablishcontext?redirectedfrom=MSDN">MSDN
   *     SCardEstablishContext function</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_SCOPE_USER = 0; // */

  /**
   * Scope terminal.
   *
   * <p><i><b>Note:</b> This constants is defined in {@code pcsclite.h.in} but not mentioned in <a
   * href="https://docs.microsoft.com/de-de/windows/win32/api/winscard/nf-winscard-scardestablishcontext?redirectedfrom=MSDN">MSDN
   * SCardEstablishContext function</a></i>
   *
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_SCOPE_TERMINAL = 1; // */

  /**
   * Scope system.
   *
   * <p>Database operations are performed within the domain of the system. The calling application
   * must have appropriate access permissions for any database actions, see {@link
   * WinscardLibrary#SCardEstablishContext(Dword, Pointer, Pointer, ScardContextByReference)}
   *
   * @see <a
   *     href="https://docs.microsoft.com/de-de/windows/win32/api/winscard/nf-winscard-scardestablishcontext?redirectedfrom=MSDN">MSDN
   *     SCardEstablishContext function</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_SCOPE_SYSTEM = 2; // */

  // end   section with scopes for establishing a context
  // ===============================================================================================
  // begin section with share options

  /**
   * SCARD_SHARE_EXCLUSIVE.
   *
   * <p>This application is not willing to share the card with other applications.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardconnectw">MSDN
   *     SCardConnect function</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_SHARE_EXCLUSIVE = 1; // */

  /**
   * SCARD_SHARE_SHARED.
   *
   * <p>This application is willing to share the card with other applications.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardconnectw">MSDN
   *     SCardConnect function</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_SHARE_SHARED = 2; // */

  /**
   * SCARD_SHARE_DIRECT.
   *
   * <p>This application is allocating the reader for its private use, and will be controlling it
   * directly. No other applications are allowed access to it.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardconnectw">MSDN
   *     SCardConnect function</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_SHARE_DIRECT = 3; // */

  // end   section with share options
  // ===============================================================================================
  // begin section with reader states

  /**
   * App wants status.
   *
   * <p><b>dwCurrentState</b> The application is unaware of the current state, and would like to
   * know. The use of this value results in an immediate return from state transition monitoring
   * services. This is represented by all bits set to zero.
   *
   * <p><b>dwEventState</b> Not mentioned.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_UNAWARE = 0x0000; // */

  /**
   * Ignore this reader.
   *
   * <p><b>dwCurrentState</b> The application is not interested in this reader, and it should not be
   * considered during monitoring operations. If this bit value is set, all other bits are ignored.
   *
   * <p><b>dwEventState</b> This reader should be ignored.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_IGNORE = 0x0001; // */

  /**
   * State has changed.
   *
   * <p><b>dwCurrentState</b> Not mentioned.
   *
   * <p><b>dwEventState</b> There is a difference between the state believed by the application, and
   * the state known by the resource manager. When this bit is set, the application may assume a
   * significant state change has occurred on this reader.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_CHANGED = 0x0002; // */

  /**
   * Reader unknown.
   *
   * <p><b>dwCurrentState</b> Not mentioned.
   *
   * <p><b>dwEventState</b> The given reader name is not recognized by the resource manager. If this
   * bit is set, then {@link #SCARD_STATE_CHANGED} and {@link #SCARD_STATE_IGNORE} will also be set.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_UNKNOWN = 0x0004; // */

  /**
   * Status unavailable.
   *
   * <p><b>dwCurrentState</b> The application expects that this reader is not available for use. If
   * this bit is set, then all the following bits are ignored.
   *
   * <p><b>dwEventState</b> The actual state of this reader is not available. If this bit is set,
   * then all the following bits are clear.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_UNAVAILABLE = 0x0008; // */

  /**
   * Card removed.
   *
   * <p><b>dwCurrentState</b> The application expects that there is no card in the reader. If this
   * bit is set, all the following bits are ignored.
   *
   * <p><b>dwEventState</b> There is no card in the reader. If this bit is set, all the following
   * bits will be clear.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_EMPTY = 0x0010; // */

  /**
   * Card inserted.
   *
   * <p><b>dwCurrentState</b> The application expects that there is a card in the reader.
   *
   * <p><b>dwEventState</b> There is a card in the reader.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_PRESENT = 0x0020; // */

  /**
   * ATR matches card.
   *
   * <p><b>dwCurrentState</b> The application expects that there is a card in the reader with an ATR
   * that matches one of the target cards. If this bit is set, {@link #SCARD_STATE_PRESENT} is
   * assumed. This bit has no meaning to {@link #SCardGetStatusChange(ScardContext, Dword,
   * ScardReaderState[], Dword) SCardGetStatusChange} beyond {@link #SCARD_STATE_PRESENT}.
   *
   * <p><b>dwEventState</b> There is a card in the reader with an ATR matching one of the target
   * cards. If this bit is set, {@link #SCARD_STATE_PRESENT} will also be set. This bit is only
   * returned on the TODO SCardLocateCards function.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_ATRMATCH = 0x0040; // */

  /**
   * Exclusive Mode.
   *
   * <p><b>dwCurrentState</b> The application expects that the card in the reader is allocated for
   * exclusive use by another application. If this bit is set, {@link #SCARD_STATE_PRESENT} is
   * assumed.
   *
   * <p><b>dwEventState</b> The card in the reader is allocated for exclusive use by another
   * application. If this bit is set, {@link #SCARD_STATE_PRESENT} will also be set.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_EXCLUSIVE = 0x0080; // */

  /**
   * Shared Mode.
   *
   * <p><b>dwCurrentState</b> The application expects that the card in the reader is in use by one
   * or more other applications, but may be connected to in shared mode. If this bit is set, {@link
   * #SCARD_STATE_PRESENT} is assumed.
   *
   * <p><b>dwEventState</b> The card in the reader is in use by one or more other applications, but
   * may be connected to in shared mode. If this bit is set, {@link #SCARD_STATE_PRESENT} will also
   * be set.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_INUSE = 0x0100; // */

  /**
   * Unresponsive card.
   *
   * <p><b>dwCurrentState</b> The application expects that there is an unresponsive card in the
   * reader.
   *
   * <p><b>dwEventState</b> There is an unresponsive card in the reader.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_MUTE = 0x0200; // */

  /**
   * Unpowered card.
   *
   * <p><b>dwCurrentState</b> This implies that the card in the reader has not been powered up.
   *
   * <p><b>dwEventState</b> Identical meaning as for dwCurrentState.
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_STATE_UNPOWERED = 0x0400; // */

  // end   section with reader states
  // ===============================================================================================
  // begin section smart card states

  /**
   * SCARD_UNKNOWN.
   *
   * <p>The current state of the reader is unknown.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If <b>p</b> is defined as the value from <a
   *       href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a> and
   *       <b>m</b> is defined as the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> then it is: {@code p = 2^m}.</i>
   *   <li><i>Hereafter the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> is used, which is identical to the value from Sun's {@code
   *       PlatformPCSC.java}.</i>
   * </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *     Card/Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_UNKNOWN = 0; // */

  /**
   * SCARD_ABSENT.
   *
   * <p>There is no card in the reader.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If <b>p</b> is defined as the value from <a
   *       href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a> and
   *       <b>m</b> is defined as the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> then it is: {@code p = 2^m}.</i>
   *   <li><i>Hereafter the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> is used, which is identical to the value from Sun's {@code
   *       PlatformPCSC.java}.</i>
   * </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *     Card/Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_ABSENT = 1; // */

  /**
   * SCARD_PRESENT.
   *
   * <p>There is a card in the reader, but it has not been moved into position for use.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If <b>p</b> is defined as the value from <a
   *       href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a> and
   *       <b>m</b> is defined as the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> then it is: {@code p = 2^m}.</i>
   *   <li><i>Hereafter the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> is used, which is identical to the value from Sun's {@code
   *       PlatformPCSC.java}.</i>
   * </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *     Card/Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_PRESENT = 2; // */

  /**
   * SCARD_SWALLOWED.
   *
   * <p>There is a card in the reader in position for use. The card is not powered.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If <b>p</b> is defined as the value from <a
   *       href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a> and
   *       <b>m</b> is defined as the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> then it is: {@code p = 2^m}.</i>
   *   <li><i>Hereafter the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> is used, which is identical to the value from Sun's {@code
   *       PlatformPCSC.java}.</i>
   * </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *     Card/Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_SWALLOWED = 3; // */

  /**
   * SCARD_POWERED.
   *
   * <p>Power is being provided to the card, but the reader driver is unaware of the mode of the
   * card.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If <b>p</b> is defined as the value from <a
   *       href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a> and
   *       <b>m</b> is defined as the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> then it is: {@code p = 2^m}.</i>
   *   <li><i>Hereafter the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> is used, which is identical to the value from Sun's {@code
   *       PlatformPCSC.java}.</i>
   * </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *     Card/Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_POWERED = 4; // */

  /**
   * SCARD_NEGOTIABLE.
   *
   * <p>The card has been reset and is awaiting PTS negotiation.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If <b>p</b> is defined as the value from <a
   *       href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a> and
   *       <b>m</b> is defined as the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> then it is: {@code p = 2^m}.</i>
   *   <li><i>Hereafter the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> is used, which is identical to the value from Sun's {@code
   *       PlatformPCSC.java}.</i>
   * </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *     Card/Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_NEGOTIABLE = 5; // */

  /**
   * SCARD_SPECIFIC.
   *
   * <p>The card has been reset and specific communication protocols have been established.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>If <b>p</b> is defined as the value from <a
   *       href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a> and
   *       <b>m</b> is defined as the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> then it is: {@code p = 2^m}.</i>
   *   <li><i>Hereafter the value from <a
   *       href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *       Card/Reader State</a> is used, which is identical to the value from Sun's {@code
   *       PlatformPCSC.java}.</i>
   * </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/264bc504-1195-43ff-a057-3d86a02c5d9c">MSDN
   *     Card/Reader State</a>
   * @see <a href="https://github.com/LudovicRousseau/PCSC/tree/master/src">pcsclite.h.in</a>
   */
  int SCARD_SPECIFIC = 6; // */

  // end   section smart card states
  // ===============================================================================================
  // begin section with methods

  /**
   * Establishes a context.
   *
   * <p>The <b>SCardEstablishContext</b> function establishes the <a
   * href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#_security_resource_manager_context_gly">resource
   * manager context</a> (the scope) within which database operations are performed.
   *
   * <p><i><b>Remarks:</b></i>
   *
   * <ol>
   *   <li><i>The context handle returned by the <b>SCardEstablishContext</b> function can be used
   *       by database query and management functions. For more information, see <a
   *       href="https://docs.microsoft.com/en-us/windows/win32/secauthn/smart-card-database-query-functions">Smart
   *       Card Database Query Functions</a> and <a
   *       href="https://docs.microsoft.com/en-us/windows/win32/secauthn/smart-card-database-management-functions">Smart
   *       Card Database Management Functions</a>.</i>
   *   <li><i>To release an established resource manager context, use {@link #SCardReleaseContext}.
   *       </i>
   * </ol>
   *
   * @param dwScope <b>[in]</b> Scope of the <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#_security_resource_manager_context_gly">resource
   *     manager context</a>. This parameter can be one of the following values.
   *     <ul>
   *       <li>{@link #SCARD_SCOPE_USER} Database operations are performed within the domain of the
   *           user.
   *       <li>{@link #SCARD_SCOPE_SYSTEM} Database operations are performed within the domain of
   *           the system. The calling application must have appropriate access permissions for any
   *           database actions.
   *     </ul>
   *
   * @param pvReserved1 <b>[in]</b> Reserved for future use and must be {@code NULL}. This parameter
   *     will allow a suitably privileged management application to act on behalf of another user.
   * @param pvReserved2 <b>[in]</b> Reserved for future use and must be {@code NULL}.
   * @param phContext <b>[out]</b> A handle to the established <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#_security_resource_manager_context_gly">resource
   *     manager context</a>. This handle can now be supplied to other functions attempting to do
   *     work within this context.
   * @return If the function succeeds, the function returns {@link PcscStatus#SCARD_S_SUCCESS
   *     SCARD_S_SUCCESS}. If the function fails, it returns an error code. For more information,
   *     see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/secauthn/authentication-return-values">Smart
   *     Card Return Values</a> or {@link PcscStatus}.
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardestablishcontext">MSDN
   *     SCardEstablishContext function</a>
   */
  Dword SCardEstablishContext(
      Dword dwScope,
      @Nullable Pointer pvReserved1,
      @Nullable Pointer pvReserved2,
      ScardContextByReference phContext); // */

  /**
   * Provides a list of readers.
   *
   * <p>The <b>SCardListReaders</b> function provides the list of <a
   * href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#reader">readers</a> within
   * a set of named reader groups, eliminating duplicates.
   *
   * <p>The caller supplies a list of reader groups, and receives the list of readers within the
   * named groups. Unrecognized group names are ignored. This function only returns readers within
   * the named groups that are currently attached to the system and available for use.
   *
   * <p><i><b>Remarks:</b></i>
   *
   * <ol>
   *   <li><i>The <b>SCardListReaders</b> function is a database query function. For more
   *       information about other database query functions, see <a
   *       href="https://docs.microsoft.com/en-us/windows/win32/secauthn/smart-card-database-query-functions">Smart
   *       Card Database Query Functions</a>.</i>
   * </ol>
   *
   * @param hContext <b>[in]</b> Handle that identifies the <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#_security_resource_manager_context_gly">resource
   *     manager context</a> for the query. The resource manager context can be set by a previous
   *     call to {@link #SCardEstablishContext}. If this parameter is set to {@code NULL}, the
   *     search for readers is not limited to any context.
   * @param mszGroups <b>[in, optional]</b> Names of the reader groups defined to the system, as a
   *     multi-string. Use a {@code NULL} value to list all readers in the system (that is, the
   *     "SCard$AllReaders" group), see {@link #SCARD_ALL_READERS}, {@link #SCARD_DEFAULT_READERS},
   *     {@link #SCARD_LOCAL_READERS}, {@link #SCARD_SYSTEM_READERS}.
   * @param mszReaders <b>[out]</b> Multi-string that lists the card readers within the supplied
   *     reader groups. If this value is {@code NULL}, {@code SCardListReaders} ignores the buffer
   *     length supplied in {@code pcchReaders}, writes the length of the buffer that would have
   *     been returned if this parameter had not been {@code NULL } to {@code pcchReaders}, and
   *     returns a success code.
   * @param pcchReaders <b>[in, out]</b> Length of the {@code mszReaders} buffer in characters. This
   *     parameter receives the actual length of the multi-string structure, including all trailing
   *     {@code NULL} characters. If the buffer length is specified as {@link
   *     WinscardLibrary#SCARD_AUTOALLOCATE SCARD_AUTOALLOCATE}, then {@code mszReaders} is
   *     converted to a pointer to a byte pointer, and receives the address of a block of memory
   *     containing the multi-string structure. This block of memory must be deallocated with TODO
   *     #SCardFreeMemory.
   * @return This function returns different values depending on whether it succeeds or fails.
   *     <ol>
   *       <li><b>Success:</b> {@link PcscStatus#SCARD_S_SUCCESS SCARD_S_SUCCESS}
   *       <li><b>Group contains no readers:</b> {@link PcscStatus#SCARD_E_NO_READERS_AVAILABLE
   *           SCARD_E_NO_READERS_AVAILABLE}
   *       <li><b>Specified reader is not currently available for use:</b> {@link
   *           PcscStatus#SCARD_E_READER_UNAVAILABLE SCARD_E_READER_UNAVAILABLE}
   *       <li><b>Other:</b> An error code. For more information, see <a
   *           href="https://docs.microsoft.com/en-us/windows/win32/secauthn/authentication-return-values">Smart
   *           Card Return Values</a> or {@link PcscStatus}.
   *     </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardlistreadersa">MSDN
   *     SCardListReaders function</a>
   */
  Dword SCardListReaders(
      @Nullable ScardContext hContext,
      @Nullable ByteBuffer mszGroups,
      @Nullable ByteBuffer mszReaders,
      DwordByReference pcchReaders); // */

  /**
   * Provides a status for a specific set of readers.
   *
   * <p>The <b>SCardGetStatusChange</b> function blocks execution until the current availability of
   * the cards in a specific set of readers changes.
   *
   * <p>The caller supplies a list of <a
   * href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#reader">readers</a> to be
   * monitored by an {@link ScardReaderState} array and the maximum amount of time (in milliseconds)
   * that it is willing to wait for an action to occur on one of the listed readers. Note that
   * <b>SCardGetStatusChange</b> uses the user-supplied value in the {@link
   * ScardReaderState#dwCurrentState dwCurrentState} members of the {@code rgReaderStates} array as
   * the definition of the current state of the readers. The function returns when there is a change
   * in availability, having filled in the {@link ScardReaderState#dwEventState dwEventState}
   * members of {@code rgReaderStates} appropriately.
   *
   * <p><i><b>Remarks:</b></i>
   *
   * <ol>
   *   <li><i>The <b>SCardGetStatusChange</b> function is a smart card tracking function. For more
   *       information about other tracking functions, see <a
   *       href="https://docs.microsoft.com/en-us/windows/win32/secauthn/smart-card-tracking-functions">MSDN
   *       Smart Card Tracking Functions</a>.</i>
   * </ol>
   *
   * @param hContext <b>[in]</b> A handle that identifies the <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#_security_resource_manager_context_gly">resource
   *     manager context</a> The resource manager context is set by a previous call to {@link
   *     #SCardEstablishContext(Dword, Pointer, Pointer, ScardContextByReference)}.
   * @param dwTimeout <b>[in]</b> The maximum amount of time, in milliseconds, to wait for an
   *     action. A value of zero causes the function to return immediately. A value of {@link
   *     #INFINITE} causes this function never to time out.<br>
   * @param rgReaderStates <b>[in, out]</b> An array of {@link ScardReaderState} structures that
   *     specify the readers to watch, and that receives the result.<br>
   *     To be notified of the arrival of a new smart card reader, set the {@link
   *     ScardReaderState#szReader szReader} member of a {@link ScardReaderState} structure to
   *     {@link #PNP_READER_ID}, and set all of the other members of that structure to zero.<br>
   *     <b>Important:</b> Each member of each structure in this array must be initialized to zero
   *     and then set to specific values as necessary. If this is not done, the function will fail
   *     in situations that involve remote card readers.
   * @param cReaders <b>[in]</b> The number of elements in the {@code rgReaderStates} array.
   * @return This function returns different values depending on whether it succeeds or fails.
   *     <ol>
   *       <li><b>Success:</b> {@link PcscStatus#SCARD_S_SUCCESS SCARD_S_SUCCESS}
   *       <li><b>Failure:</b> An error code. For more information, see <a
   *           href="https://docs.microsoft.com/en-us/windows/win32/secauthn/authentication-return-values">Smart
   *           Card Return Values</a> or {@link PcscStatus}.
   *     </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardgetstatuschangea">MSDN
   *     SCardGetStatusChange function</a>
   */
  Dword SCardGetStatusChange(
      ScardContext hContext,
      Dword dwTimeout,
      ScardReaderState[] rgReaderStates,
      Dword cReaders); // */

  /**
   * Establishes a connection to a smart card.
   *
   * <p>The <b>SCardConnect</b> function establishes a connection (using a specific <a
   * href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#_security_resource_manager_context_gly">resource
   * manager context</a>) between the calling application and a smart card contained by a specific
   * reader. If no card exists in the specified reader, an error is returned.
   *
   * <p><i><b>Remarks:</b></i>
   *
   * <ol>
   *   <li><i>The <b>SCardConnect</b> function is a smart card and reader access function. For more
   *       information about other access functions, see <a
   *       href="https://docs.microsoft.com/en-us/windows/win32/secauthn/smart-card-and-reader-access-functions">Smart
   *       Card and Reader Access Functions</a>.</i>
   * </ol>
   *
   * @param hContext <b>[in]</b> A handle that identifies the <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#_security_resource_manager_context_gly">resource
   *     manager context</a> The resource manager context is set by a previous call to {@link
   *     #SCardEstablishContext(Dword, Pointer, Pointer, ScardContextByReference)}.
   * @param szReader <b>[in]</b> The name of the reader that contains the target card.
   * @param dwSharMode <b>[in]</b> A flag that indicates whether other applications may form
   *     connections to the card. See {@link #SCARD_SHARE_SHARED}, {@link #SCARD_SHARE_EXCLUSIVE},
   *     {@link #SCARD_SHARE_DIRECT}.
   * @param dwPreferredProtocols <b>[in]</b> A bitmask of acceptable protocols for the connection.
   *     Possible values may be combined with the <b>OR</b> operation.
   *     <ul>
   *       <li>{@link #SCARD_PROTOCOL_T0}: {@code T=0} is an acceptable protocol.
   *       <li>{@link #SCARD_PROTOCOL_T1}: {@code T=1} is an acceptable protocol.
   *       <li>{@link #SCARD_PROTOCOL_UNDEFINED}: This parameter may be zero only if {@code
   *           dwShareMode} is set to {@link #SCARD_SHARE_DIRECT}. In this case, no protocol
   *           negotiation will be performed by the drivers until an {@code
   *           IOCTL_SMARTCARD_SET_PROTOCOL} control directive is sent with TODO SCardControl.
   *     </ul>
   *
   * @param phCard <b>[out]</b> A handle that identifies the connection to the smart card in the
   *     designated reader.
   * @param pdwActiveProtocol <b>[out]</b> A flag that indicates the established active protocol.
   *     <ul>
   *       <li>{@link #SCARD_PROTOCOL_T0}: {@code T=0} is the active protocol.
   *       <li>{@link #SCARD_PROTOCOL_T1}: {@code T=1} is the active protocol.
   *       <li>{@link #SCARD_PROTOCOL_UNDEFINED}: {@link #SCARD_SHARE_DIRECT} has been specified, so
   *           that no protocol negotiation has occurred. It is possible that there is no card in
   *           the reader.
   *     </ul>
   *
   * @return This function returns different values depending on whether it succeeds or fails.
   *     <ol>
   *       <li><b>Success:</b> {@link PcscStatus#SCARD_S_SUCCESS SCARD_S_SUCCESS}
   *       <li><b>Failure:</b> An error code. For more information, see <a
   *           href="https://docs.microsoft.com/en-us/windows/win32/secauthn/authentication-return-values">Smart
   *           Card Return Values</a> or {@link PcscStatus}.
   *       <li>{@link PcscStatus#SCARD_E_NOT_READY SCARD_E_NOT_READY} The reader was unable to
   *           connect to the card.
   *     </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardconnectw">MSDN
   *     SCardConnect function</a>
   */
  Dword SCardConnect(
      ScardContext hContext,
      String szReader,
      Dword dwSharMode,
      Dword dwPreferredProtocols,
      ScardHandleByReference phCard,
      DwordByReference pdwActiveProtocol); // */

  /**
   * Retrieves the current status of a smart card in a reader.
   *
   * <p>The <b>SCardStatus</b> function provides the current status of a smart card in a reader. You
   * can call it any time after a successful call to {@link #SCardConnect} and before a successful
   * call to {@link #SCardDisconnect}. It does not affect the state of the reader or reader driver.
   *
   * <p><i><b>Remarks:</b></i>
   *
   * <ol>
   *   <li><i>The <b>SCardStatus</b> function is a smart card and reader access function. For
   *       information about other access functions, see <a
   *       href="https://docs.microsoft.com/en-us/windows/win32/secauthn/smart-card-and-reader-access-functions">Smart
   *       Card and Reader Access Functions</a>.</i>
   *   <li><i>For the T=0 protocol, the data received back are the SW1 and SW2 status codes,
   *       possibly preceded by response data.</i>
   * </ol>
   *
   * @param hCard <b>[in]</b> Reference value obtained from {@link #SCardConnect}.
   * @param mszReaderName <b>[out]</b> List of display names (multiple string) by which the
   *     currently connected reader is known.
   * @param pcchReaderLen <b>[in, out, optional]</b> On input, supplies the length of the {@code
   *     mszReaderName} buffer. On output, receives the actual length (in characters) of the reader
   *     name list, including the trailing {@code NULL} character. If this buffer length is
   *     specified as {@link #SCARD_AUTOALLOCATE}, then {@code mszReaderName} is converted to a
   *     pointer to a byte pointer, and it receives the address of a block of memory that contains
   *     the multiple-string structure.
   * @param pdwState <b>[out, optional]</b> Current state of the smart card in the reader. Upon
   *     success, it receives one of the following state indicators.
   *     <ul>
   *       <li>{@link #SCARD_ABSENT}
   *       <li>{@link #SCARD_PRESENT}
   *       <li>{@link #SCARD_SWALLOWED}
   *       <li>{@link #SCARD_POWERED}
   *       <li>{@link #SCARD_NEGOTIABLE}
   *       <li>{@link #SCARD_SPECIFIC}
   *     </ul>
   *
   * @param pdwProtocol <b>[out, optional]</b> Current protocol, if any. The returned value is
   *     meaningful only if the returned value of {@code pdwState} is {@link #SCARD_SPECIFIC}.
   *     <ul>
   *       <li>{@link #SCARD_PROTOCOL_RAW}: The Raw Transfer protocol is in use.
   *       <li>{@link #SCARD_PROTOCOL_T0}: The ISO/IEC 7816-3 T=0 protocol is in use.
   *       <li>{@link #SCARD_PROTOCOL_T1}: The ISO/IEC 7816-3 T=1 protocol is in use.
   *     </ul>
   *
   * @param pbAtr <b>[out]</b> Pointer to a byte buffer that receives the Answer-To-Reset from the
   *     currently inserted card, if available.
   * @param pcbAtrLen <b>[in, out, optional]</b> On input, supplies the length of the {@code pbAtr}
   *     buffer. On output, receives the number of bytes in the ATR string (32 bytes maximum). If
   *     this buffer length is specified as {@link #SCARD_AUTOALLOCATE}, then {@code pbAtr} is
   *     converted to a pointer to a byte pointer, and it receives the address of a block of memory
   *     that contains the multiple-string structure.<br>
   *     <i><b>Remark afi:</b> I am pretty sure that here thirty three bytes is the maximum. For
   *     explanation see {@link AnswerToReset#MAX_ATR_SIZE}.</i>
   * @return If the function successfully provides the current status of a smart card in a reader,
   *     the return value is {@link PcscStatus#SCARD_S_SUCCESS}. If the function fails, it returns
   *     an error code. For more information, see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/secauthn/authentication-return-values">Smart
   *     Card Return Values</a> or {@link PcscStatus}.
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardstatusa">MSDN
   *     SCardStatus function</a>
   */
  Dword SCardStatus(
      ScardHandle hCard,
      @Nullable ByteBuffer mszReaderName,
      DwordByReference pcchReaderLen,
      DwordByReference pdwState,
      DwordByReference pdwProtocol,
      ByteBuffer pbAtr,
      DwordByReference pcbAtrLen); // */

  /**
   * Transmits data to and from a connected smart card.
   *
   * <p>The <b>SCardTransmit</b> function sends a service request to the smart card and expects to
   * receive data back from the card.
   *
   * <p><i><b>Remarks:</b></i>
   *
   * <ol>
   *   <li><i>The <b>SCardTransmit</b> function is a smart card and reader access function. For
   *       information about other access functions, see <a
   *       href="https://docs.microsoft.com/en-us/windows/win32/secauthn/smart-card-and-reader-access-functions">Smart
   *       Card and Reader Access Functions</a>.</i>
   *   <li><i>For the T=0 protocol, the data received back are the SW1 and SW2 status codes,
   *       possibly preceded by response data.</i>
   * </ol>
   *
   * @param hCard <b>[in]</b> A reference value obtained from the {@link #SCardConnect} function.
   * @param pioSendPci <b>[in]</b> A pointer to the protocol header structure for the instruction.
   *     This buffer is in the format of an {@link ScardIoRequest} structure, followed by the
   *     specific protocol control information (PCI).<br>
   *     For the {@code T=0}, {@code T=1}, and {@code RAW} protocols, the PCI structure is constant.
   *     The smart card subsystem supplies a global {@code T=0}, {@code T=1}, or {@code Raw} PCI
   *     structure.
   * @param pbSendBuffer <b>[in]</b> A pointer to the actual data to be written to the card.<br>
   *     For T=0, the data parameters are placed into the address pointed to by {code pbSendBuffer}
   *     according to the following structure:<br>
   *     <i>struct{ BYTE CLA, INS, P1, P2, P3;} CmdBytes</i>.<br>
   *     The data sent to the card should immediately follow the send buffer. In the special case
   *     where no data is sent to the card and no data is expected in return (i.e. case 1 according
   *     to ISO/IEC 7816-3:2006 12.1.2), P3 is not sent.<br>
   *     <i><b>Remark afi:</b> I am pretty sure that P3 is sent to the card, see ISO/IEC 7816-3:2006
   *     12.2.2.</i>
   * @param cbSendLength <b>[in]</b> The length, in bytes, of the {@code pbSendBuffer} parameter.
   *     <br>
   *     For {@code T=0}, in the special case where no data is sent to the card and no data expected
   *     in return (i.e. case 1 according to ISO/IEC 7816-3:2006 12.1.2), this length must reflect
   *     that the P3 member is not being sent.<br>
   *     <i><b>Remark afi:</b> I am pretty sure that P3 is sent to the card, see ISO/IEC 7816-3:2006
   *     12.2.2.</i>
   * @param pioRecvPci <b>[in, out, optional]</b> Pointer to the protocol header structure for the
   *     instruction, followed by a buffer in which to receive any returned protocol control
   *     information (PCI) specific to the protocol in use. This parameter can be {@code NULL} if no
   *     PCI is returned.
   * @param pbRecvBuffer <b>[out]</b> Pointer to any data returned from the card.<br>
   *     For {@code T=0}, the data is immediately followed by the SW1 and SW2 status bytes. If no
   *     data is returned from the card, then this buffer will only contain the SW1 and SW2 status
   *     bytes.
   * @param pcbRecvLength <b>[in, out]</b> Supplies the length, in bytes, of the {@code
   *     pbRecvBuffer} parameter and receives the actual number of bytes received from the smart
   *     card. This value cannot be {@link #SCARD_AUTOALLOCATE} because {@code SCardTransmit} does
   *     not support {@link #SCARD_AUTOALLOCATE}.<br>
   *     For {@code T=0}, the receive buffer must be at least two bytes long to receive the SW1 and
   *     SW2 status bytes.
   * @return If the function successfully sends a service request to the smart card, the return
   *     value is {@link PcscStatus#SCARD_S_SUCCESS}. If the function fails, it returns an error
   *     code. For more information, see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/secauthn/authentication-return-values">Smart
   *     Card Return Values</a> or {@link PcscStatus}.
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardtransmit">MSDN
   *     SCardTransmit function</a>
   */
  Dword SCardTransmit(
      ScardHandle hCard,
      ScardIoRequest pioSendPci,
      ByteBuffer pbSendBuffer,
      Dword cbSendLength,
      @Nullable ScardIoRequest pioRecvPci,
      ByteBuffer pbRecvBuffer,
      DwordByReference pcbRecvLength); // */

  /**
   * Terminates a connection between an application and a smart card.
   *
   * <p>The <b>SCardDisconnect</b> function terminates a connection previously opened between the
   * calling application and a smart card in the target reader.
   *
   * <p><i><b>Remarks:</b></i>
   *
   * <ol>
   *   <li><i>If an application (which previously called {@link #SCardConnect}) exits without
   *       calling <b>SCardDisconnect</b>, the card is automatically reset.</i>
   *   <li><i>The <b>SCardDisconnect</b> function is a smart card and reader access function. For
   *       more information about other access functions, see <a
   *       href="https://docs.microsoft.com/en-us/windows/win32/secauthn/smart-card-and-reader-access-functions">Smart
   *       Card and Reader Access Functions</a>.</i>
   * </ol>
   *
   * @param hCard <b>[in]</b> Reference value obtained from a previous call to {@link
   *     #SCardConnect}.
   * @param dwDisposition <b>[in]</b> Action to take on the card in the connected reader on close.
   *     <ul>
   *       <li>{@link #SCARD_LEAVE_CARD}: Do not do anything special.
   *       <li>{@link #SCARD_RESET_CARD}: Reset the card.
   *       <li>{@link #SCARD_UNPOWER_CARD}: Power down the card.
   *       <li>{@link #SCARD_EJECT_CARD}: Eject the card.
   *     </ul>
   *
   * @return This function returns different values depending on whether it succeeds or fails.
   *     <ol>
   *       <li><b>Success:</b> {@link PcscStatus#SCARD_S_SUCCESS SCARD_S_SUCCESS}
   *       <li><b>Failure:</b> An error code. For more information, see <a
   *           href="https://docs.microsoft.com/en-us/windows/win32/secauthn/authentication-return-values">Smart
   *           Card Return Values</a> or {@link PcscStatus}.
   *     </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scarddisconnect">MSDN
   *     SCardDisconnect function</a>
   */
  Dword SCardDisconnect(ScardHandle hCard, Dword dwDisposition); // */

  /**
   * Releases a context.
   *
   * <p>The <b>SCardReleaseContext</b> function closes an established <a
   * href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#_security_resource_manager_context_gly">resource
   * manager context</a> freeing any resources allocated under that context, including {@code
   * SCARDHANDLE} objects and memory allocated using the {@code SCARD_AUTOALLOCATE} length
   * designator.
   *
   * @param hContext <b>[in]</b> Handle that identifies the <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/secgloss/r-gly#_security_resource_manager_context_gly">resource
   *     manager context</a> The resource manager context is set by a previous call to {@link
   *     #SCardEstablishContext}.
   * @return This function returns different values depending on whether it succeeds or fails.
   *     <ol>
   *       <li><b>Success:</b> {@link PcscStatus#SCARD_S_SUCCESS SCARD_S_SUCCESS}
   *       <li><b>Failure:</b> An error code. For more information, see <a
   *           href="https://docs.microsoft.com/en-us/windows/win32/secauthn/authentication-return-values">Smart
   *           Card Return Values</a> or {@link PcscStatus}.
   *     </ol>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/nf-winscard-scardreleasecontext">MSDN
   *     SCardReleaseContext function</a>
   */
  Dword SCardReleaseContext(ScardContext hContext); // */
} // end interface
