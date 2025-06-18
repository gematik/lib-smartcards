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

import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_PROTOCOL_T1;

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import de.gematik.smartcards.pcsc.PcscException;
import de.gematik.smartcards.pcsc.constants.PcscStatus;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class providing a wrapper for native library.
 *
 * <p>The main purpose for this class is to provide logging at level {@code TRACE} for methods in
 * {@link WinscardLibrary}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public final class WinscardLibraryImpl {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(WinscardLibraryImpl.class); // */

  /** Charset used for {@link String} to {@code byte[]} conversion. */
  private static final Charset UTF8 = StandardCharsets.UTF_8; // */

  /** Native library implementing {@link WinscardLibrary}. */
  private final WinscardLibrary insLib; // */

  /**
   * Global protocol control information (PCI) structures.
   *
   * <p>The elements are used as follows:
   *
   * <ol>
   *   <li>protocol control information (PCI) for T=0 protocol
   *   <li>protocol control information (PCI) for T=1 protocol
   *   <li>protocol control information (PCI) for raw protocol
   * </ol>
   */
  private final List<ScardIoRequest> insScardPciList; // NOPMD no accessors */

  /**
   * Comfort constructor.
   *
   * @param lib native library implementing {@link WinscardLibrary}
   * @param scardPciList protocol control information (PCI) structures
   */
  private WinscardLibraryImpl(final WinscardLibrary lib, final List<ScardIoRequest> scardPciList) {
    insLib = lib;
    insScardPciList = Collections.unmodifiableList(scardPciList);
  } // end constructor */

  /**
   * Pseudo constructor.
   *
   * <p>Performs the following tasks:
   *
   * <ol>
   *   <li>loads the native platform specific PC/SC library
   *   <li>copies global PCI (protocol control information) structures for T=0, T=1 and raw out of
   *       native library into the corresponding {@link ScardIoRequest}.
   *   <li>constructs with this information the return value.
   * </ol>
   *
   * @return {@link WinscardLibraryImpl}
   */
  public static WinscardLibraryImpl openLib() {
    // --- define some platform specific constants
    final String windows = "WinSCard.dll";
    final String mac = "/System/Library/Frameworks/PCSC.framework/PCSC";
    final String pcsc = "libpcsclite.so.1";

    final String libName;
    final Map<String, Object> options;
    if (PcscStatus.OS_WINDOWS) {
      libName = windows;
      options = Map.of(Library.OPTION_FUNCTION_MAPPER, new WindowsFunctionMapper());
    } else if (PcscStatus.OS_MAC) {
      libName = mac;
      options = Map.of(Library.OPTION_FUNCTION_MAPPER, new MacFunctionMapper());
    } else {
      libName = pcsc;
      options = Collections.emptyMap();
    } // end else

    final WinscardLibrary lib = Native.load(libName, WinscardLibrary.class, options);
    final NativeLibrary nativeLibrary = NativeLibrary.getInstance(libName); // NOPMD close resource

    // Note 1: SCARD_PCI_* is #defined in pcsclite.h.in (both pcsclite and winscard).

    // #define SCARD_PCI_T0 (&g_rgSCardT0Pci),   i.e. protocol control information (PCI) for T=0
    // #define SCARD_PCI_T1 (&g_rgSCardT1Pci),   i.e. protocol control information (PCI) for T=1
    // #define SCARD_PCI_RAW (&g_rgSCardRawPci), i.e. PCI for RAW protocol
    final List<ScardIoRequest> list =
        List.of(
            new ScardIoRequest(nativeLibrary.getGlobalVariableAddress("g_rgSCardT0Pci")),
            new ScardIoRequest(nativeLibrary.getGlobalVariableAddress("g_rgSCardT1Pci")),
            new ScardIoRequest(nativeLibrary.getGlobalVariableAddress("g_rgSCardRawPci")));
    list.forEach(
        pci -> {
          pci.read();
          pci.setAutoSynch(false);
        }); // end forEach(pci -> ...)

    return new WinscardLibraryImpl(lib, list);
  } // end method */

  /**
   * Getter.
   *
   * @return library used for PC/SC activities
   */
  private WinscardLibrary getLib() {
    return insLib;
  } // end method */

  /**
   * FunctionMapper from identifier in winscard.h to the symbol in the PCSC shared library on OSX
   * that implements it.
   *
   * <p>The SCardControl identifier is implemented by the SCardControl132 symbol, since it appeared
   * in pcsc-lite 1.3.2 and replaced an old function with a different signature.
   */
  private static final class MacFunctionMapper implements FunctionMapper {

    /**
     * Returns function name.
     *
     * @return name of function
     * @see FunctionMapper#getFunctionName(NativeLibrary, Method)
     */
    @Override
    public String getFunctionName(final NativeLibrary library, final Method method) {
      String name = method.getName();

      if ("SCardControl".equals(name)) { // NOPMD literal in if statement
        name = "SCardControl132";
      } // end fi

      return name;
    } // end method */
  } // end inner class

  /**
   * FunctionMapper from identifier in WinSCard.h to the symbol in the WinSCard.dll shared library
   * on Windows that implements it.
   *
   * <p>Each function that takes a string has an implementation taking char and a different
   * implementation that takes wchar_t. We use the ASCII version, since it is unlikely for reader
   * names to contain non-ASCII.
   */
  private static final class WindowsFunctionMapper implements FunctionMapper {

    /** Set of names. */
    private static final Set<String> ASCII_SUFFIX_NAMES =
        Set.of(
            "SCardListReaderGroups",
            "SCardListReaders",
            "SCardGetStatusChange",
            "SCardConnect",
            "SCardStatus"); // */

    /**
     * Returns function name.
     *
     * @return name of function
     * @see FunctionMapper#getFunctionName(NativeLibrary, Method)
     */
    @Override
    public String getFunctionName(final NativeLibrary library, final Method method) {
      String name = method.getName();

      if (ASCII_SUFFIX_NAMES.contains(name)) {
        name = name + 'A';
      } // end fi

      return name;
    } // end method */
  } // end inner class

  /**
   * Converts octet string into a list of strings.
   *
   * <p>Assumes {@link StandardCharsets#UTF_8 UTF-8} encoding. The octet string <b>SHALL</b> be
   * encoded as follows:
   *
   * <ol>
   *   <li>Each string <b>SHALL</b> be {@code zero}-terminated, i.e. by an octet of value {@code 0 =
   *       0x00 = '00'}.
   *   <li>An empty string <b>SHALL</b>terminates the list, i.e. two consecutive octets each with a
   *       value of {@code 0 = 0x00 = '00'}.
   * </ol>
   *
   * @param multiString octet string containing a list of '0' terminated strings
   * @return fixed size list of strings (see {@link Arrays#asList(Object[])})
   * @throws IllegalArgumentException if multi-string is not terminated with a null-terminated empty
   *     string
   */
  // Note: For test purposes visibility is "public".
  public static List<String> multiString(final byte[] multiString) {
    if ((multiString.length > 0) && (0 == multiString[0])) {
      // ... multiString starts with delimiter, i.e. with an empty string
      return Collections.emptyList();
    } // end fi

    try {
      final String tmp = new String(multiString, StandardCharsets.UTF_8);

      return Arrays.asList(
          tmp
              // get first part of tmp up to empty 0-terminated string
              // Note: If such a termination is absent, then
              //       StringIndexOutOfBoundsException is thrown.
              .substring(0, tmp.indexOf("\0\0"))
              .split("\0") // split by '00'-character
          );
    } catch (StringIndexOutOfBoundsException e) {
      // ... no proper termination of the multi-string
      throw new IllegalArgumentException(
          "Multi-string must end with a null-terminated empty string.", e);
    } // end Catch (...)
    // end implementation 2 */
  } // end method */

  /**
   * Method wrapping {@link WinscardLibrary#SCardListReaders(ScardContext, ByteBuffer, ByteBuffer,
   * DwordByReference)}.
   *
   * <p><b>Observations:</b>
   *
   * <ol>
   *   <li>If no resource manager is running then {@link PcscStatus#SCARD_E_NO_SERVICE
   *       SCARD_E_NO_SERVICE} is to be expected.
   *   <li>If a resource manager is running then {@link PcscStatus#SCARD_S_SUCCESS SCARD_S_SUCCESS}
   *       is to be expected.
   *   <li><b>Linux</b> is able to use all scope-values defined in {@code pcsc-lite.h.in}, i.e. from
   *       set: [ {@link WinscardLibrary#SCARD_SCOPE_USER SCARD_SCOPE_USER}, {@link
   *       WinscardLibrary#SCARD_SCOPE_TERMINAL SCARD_SCOPE_TERMINAL}, {@link
   *       WinscardLibrary#SCARD_SCOPE_SYSTEM SCARD_SCOPE_SYSTEM} ].
   *   <li><b>Windows</b> is unable to use scope {@link WinscardLibrary#SCARD_SCOPE_TERMINAL
   *       SCARD_SCOPE_TERMINAL}.
   * </ol>
   *
   * <p><i><b>Implementation recommendations:</b></i>
   *
   * <ol>
   *   <li><i>Make sure (or assume) that resource manager is running.</i>
   *   <li><i>Use {@link WinscardLibrary#SCARD_SCOPE_SYSTEM SCARD_SCOPE_SYSTEM} in case TODO</i>
   *   <li><i>Use {@link WinscardLibrary#SCARD_SCOPE_USER SCARD_SCOPE_USER} in case TODO</i>
   * </ol>
   *
   * @param dwScope see {@code WinscardLibrary.SCardEstablishContext}}
   * @param pvReserved1 see {@code WinscardLibrary.SCardEstablishContext}}
   * @param pvReserved2 see {@code WinscardLibrary.SCardEstablishContext}}
   * @param phContext see {@code WinscardLibrary.SCardEstablishContext}}
   * @return see {@code WinscardLibrary.SCardEstablishContext}}
   */
  public int scardEstablishContext(
      final Dword dwScope,
      final @Nullable Pointer pvReserved1,
      final @Nullable Pointer pvReserved2,
      final ScardContextByReference phContext) {
    LOGGER
        .atTrace()
        .log(
            "SCardEstablishContext(scope={}, pvReserved1={}, pvReserved2={}, phContext={})",
            dwScope,
            pvReserved1,
            pvReserved2,
            phContext);

    final long startTime = System.nanoTime();
    final Dword code = getLib().SCardEstablishContext(dwScope, pvReserved1, pvReserved2, phContext);
    final long runTime = System.nanoTime() - startTime;
    final long result = code.longValue();

    LOGGER
        .atTrace()
        .log(
            "status={}={} phContext={}, runTime={}",
            String.format("0x%x", result),
            PcscStatus.getExplanation((int) result),
            phContext,
            AfiUtils.nanoSeconds2Time(runTime));

    return (int) result;
  } // end method */

  /**
   * Method wrapping {@link WinscardLibrary#SCardListReaders(ScardContext, ByteBuffer, ByteBuffer,
   * DwordByReference)}.
   *
   * <p><b>Observations:</b>
   *
   * <ol>
   *   <li>It does not matter how a {@link ScardContext} is established.
   *   <li><b>Linux</b> does not support {@code hContext = null}.
   *   <li><b>Windows</b>: {@code hContext = null} returns a list of all readers ever attached to
   *       the computer (obviously since OS-installation).
   *   <li>The observed behavior doesn't depend on the value of {@code mszGroups}. Thus, {@code
   *       mszGroups} could be any of {@code NULL}, {@link WinscardLibrary#SCARD_ALL_READERS
   *       SCARD_ALL_READERS}, {@link WinscardLibrary#SCARD_DEFAULT_READERS SCARD_DEFAULT_READERS}
   *       or a concatenation of {@link WinscardLibrary#SCARD_ALL_READERS SCARD_ALL_READERS} and
   *       {@link WinscardLibrary#SCARD_DEFAULT_READERS SCARD_DEFAULT_READERS}.
   *   <li>The array backing {@code mszReaders} can be larger than necessary.
   *   <li>If the size indicated by {@code pcchReaders} is smaller than the size of the to be
   *       returned {@code mszReaders} then {@link PcscStatus#SCARD_E_INSUFFICIENT_BUFFER
   *       SCARD_E_INSUFFICIENT_BUFFER} is returned.
   *   <li>Neither Windows nor Linux support {@link WinscardLibrary#SCARD_AUTOALLOCATE
   *       SCARD_AUTOALLOCATE}.
   * </ol>
   *
   * <p><i><b>Implementation recommendations:</b> In order to get a list of readers actually
   * connected:</i>
   *
   * <ol>
   *   <li>For {@code hContext} use a real context established with either with {@link
   *       WinscardLibrary#SCARD_SCOPE_SYSTEM SCARD_SCOPE_SYSTEM} or with {@link
   *       WinscardLibrary#SCARD_SCOPE_USER SCARD_SCOPE_USER} (presently no differences are known
   *       between these two values).
   *   <li>Set {@code mszGroups} to {@code NULL}.
   *   <li>Call the method in a {@code do .. while()}-loop. Within that loop first call this method
   *       with {@code mszReaders} set to {@code NULL} and {@code pcchReaders} containing 0 in order
   *       to retrieve the required buffer size. Then allocate an appropriate buffer for {@code
   *       mszReaders} and call the method again. Loop as long as the second call returns {@link
   *       PcscStatus#SCARD_E_INSUFFICIENT_BUFFER SCARD_E_INSUFFICIENT_BUFFER}. This return value
   *       occurs in case another reader is connected between the first and second call and because
   *       of the name of that reader a larger buffer is needed.
   * </ol>
   *
   * @param context see {@code WinscardLibrary.SCardListReaders}}
   * @param mszGroups see {@code WinscardLibrary.SCardListReaders}}
   * @param mszReaders see {@code WinscardLibrary.SCardListReaders}}
   * @param pcchReaders see {@code WinscardLibrary.SCardListReaders}}
   * @return see {@code WinscardLibrary.SCardListReaders}}
   */
  public int scardListReaders(
      final @Nullable ScardContext context,
      final @Nullable String mszGroups,
      final @Nullable byte[] mszReaders,
      final DwordByReference pcchReaders) {
    final String hexGroups;
    final ByteBuffer bufGroups;
    if (null == mszGroups) {
      hexGroups = "null";
      bufGroups = null;
    } else {
      final byte[] octets = mszGroups.getBytes(UTF8);
      hexGroups = Hex.toHexDigits(octets);
      bufGroups = ByteBuffer.wrap(octets);
    } // end else

    String hexReaders;
    String strReaders;
    final ByteBuffer bufReaders;
    if (null == mszReaders) {
      hexReaders = null; // NOPMD assigning to "null" is a code smell
      strReaders = null; // NOPMD assigning to "null" is a code smell
      bufReaders = null;
    } else {
      hexReaders = Hex.toHexDigits(mszReaders);
      strReaders = multiString(mszReaders).toString();
      bufReaders = ByteBuffer.wrap(mszReaders);
    } // end else

    LOGGER
        .atTrace()
        .log(
            "SCardListReaders(hContext={}, mszGroups='{}'={}, mszReaders='{}'={}, pcchReaders={})",
            context,
            hexGroups,
            mszGroups,
            hexReaders,
            strReaders,
            pcchReaders);

    final long startTime = System.nanoTime();
    final Dword code = getLib().SCardListReaders(context, bufGroups, bufReaders, pcchReaders);
    final long runTime = System.nanoTime() - startTime;
    final long result = code.longValue();

    if (null != mszReaders) {
      hexReaders = Hex.toHexDigits(mszReaders);
      strReaders = multiString(mszReaders).toString();
    } // end else
    LOGGER
        .atTrace()
        .log(
            "status={}={} mszReaders='{}'={}, pccsReaders={}, runTime={}",
            String.format("0x%x", result),
            PcscStatus.getExplanation((int) result),
            hexReaders,
            strReaders,
            pcchReaders,
            AfiUtils.nanoSeconds2Time(runTime));

    return (int) result;
  } // end method */

  /**
   * Method wrapping {@link WinscardLibrary#SCardGetStatusChange(ScardContext, Dword,
   * ScardReaderState[], Dword)}.
   *
   * <p><i><b>Note:</b> Compared to {@link WinscardLibrary#SCardGetStatusChange(ScardContext, Dword,
   * ScardReaderState[], Dword)} this method has no parameter {@code cReaders}, because this method
   * calculates that value from parameter {@code rgReaderStates}.</i>
   *
   * @param context see {@link WinscardLibrary#SCardGetStatusChange}}
   * @param dwTimeout see {@link WinscardLibrary#SCardGetStatusChange}}
   * @param rgReaderStates see {@link WinscardLibrary#SCardGetStatusChange}}
   * @return see {@link WinscardLibrary#SCardGetStatusChange}}
   * @throws IllegalArgumentException if {@code readerStates} has zero length
   * @throws NullPointerException if {@code rgReaderStates} is null
   */
  public int scardGetStatusChange(
      final ScardContext context, final long dwTimeout, final ScardReaderState... rgReaderStates) {
    final int cReaders = rgReaderStates.length;
    LOGGER
        .atTrace()
        .log(
            "SCardGetStatusChange(hContext={}, dwTimeout={}, cReaders={}, rgReaderStates={})",
            context,
            dwTimeout,
            cReaders,
            ScardReaderState.toString(rgReaderStates));

    final long startTime = System.nanoTime();
    final Dword code =
        getLib()
            .SCardGetStatusChange(
                context, new Dword(dwTimeout), rgReaderStates, new Dword(cReaders));
    final long runTime = System.nanoTime() - startTime;
    final long result = code.longValue();

    LOGGER
        .atTrace()
        .log(
            "status={}={} rgReaderStates={}, runTime={}",
            String.format("0x%x", result),
            PcscStatus.getExplanation((int) result),
            ScardReaderState.toString(rgReaderStates),
            AfiUtils.nanoSeconds2Time(runTime));

    return (int) result;
  } // end method */

  /**
   * Method wrapping {@link WinscardLibrary#SCardConnect}.
   *
   * @param context see {@link WinscardLibrary#SCardConnect}}
   * @param szReader see {@link WinscardLibrary#SCardConnect}}
   * @param dwShareMode see {@link WinscardLibrary#SCardConnect}}
   * @param dwPreferredProtocols see {@link WinscardLibrary#SCardConnect}}
   * @param phCard see {@link WinscardLibrary#SCardConnect}}
   * @param pdwActiveProtocol see {@link WinscardLibrary#SCardConnect}}
   * @return see {@link WinscardLibrary#SCardConnect}}
   */
  public int scardConnect(
      final ScardContext context,
      final String szReader,
      final int dwShareMode,
      final int dwPreferredProtocols,
      final ScardHandleByReference phCard,
      final DwordByReference pdwActiveProtocol) {
    LOGGER
        .atTrace()
        .log(
            "SCardConnect(hContext={}, szReader={}, dwSharedMode={},"
                + " dwPreferredProtocols={}, phCard={}, pdwActiveProtocol={})",
            context,
            szReader,
            dwShareMode,
            dwPreferredProtocols,
            phCard,
            pdwActiveProtocol);

    final long startTime = System.nanoTime();
    final Dword code =
        getLib()
            .SCardConnect(
                context,
                szReader,
                new Dword(dwShareMode),
                new Dword(dwPreferredProtocols),
                phCard,
                pdwActiveProtocol);
    final long runTime = System.nanoTime() - startTime;
    final long result = code.longValue();

    LOGGER
        .atTrace()
        .log(
            "status={}={} phCard={}, pdwActiveProtocol={}, runTime={}",
            String.format("0x%x", result),
            PcscStatus.getExplanation((int) result),
            phCard,
            pdwActiveProtocol,
            AfiUtils.nanoSeconds2Time(runTime));

    return (int) result;
  } // end method */

  /**
   * Method wrapping {@link WinscardLibrary#SCardStatus}.
   *
   * @param card see {@link WinscardLibrary#SCardStatus}
   * @param mszReaderName see {@link WinscardLibrary#SCardStatus}
   * @param pcchReaderLen see {@link WinscardLibrary#SCardStatus}
   * @param pdwState see {@link WinscardLibrary#SCardStatus}
   * @param pdwProtocol see {@link WinscardLibrary#SCardStatus}
   * @param pbAtr see {@link WinscardLibrary#SCardStatus}
   * @param pcbAtrLen see {@link WinscardLibrary#SCardStatus}
   * @return see {@link WinscardLibrary#SCardStatus}
   */
  public int scardStatus(
      final ScardHandle card,
      final @Nullable ByteBuffer mszReaderName,
      final DwordByReference pcchReaderLen,
      final DwordByReference pdwState,
      final DwordByReference pdwProtocol,
      final ByteBuffer pbAtr,
      final DwordByReference pcbAtrLen) {
    LOGGER
        .atTrace()
        .log(
            "SCardStatus(hCard={}, mszReaderName={}, pcchReaderLen={},"
                + " pdwState={}, pdwProtocol={}, pbAtr={}, pcbAtrlen={})",
            card,
            mszReaderName,
            pcchReaderLen,
            pdwState,
            pdwProtocol,
            Hex.toHexDigits(pbAtr.array()),
            pcbAtrLen);

    final long startTime = System.nanoTime();
    final Dword code =
        getLib()
            .SCardStatus(
                card, mszReaderName, pcchReaderLen, pdwState, pdwProtocol, pbAtr, pcbAtrLen);
    final long runTime = System.nanoTime() - startTime;
    final long result = code.longValue();

    LOGGER
        .atTrace()
        .log(
            "status={}={} mszReaderName={}, pcchReaderLen={},"
                + " pdwState={}, pdwProtocol={}, pbAtr={}, pcbAtrlen={}, runTime={}",
            String.format("0x%x", result),
            PcscStatus.getExplanation((int) result),
            mszReaderName,
            pcchReaderLen,
            pdwState,
            pdwProtocol,
            Hex.toHexDigits(pbAtr.array()),
            pcbAtrLen,
            AfiUtils.nanoSeconds2Time(runTime));

    return (int) result;
  } // end method */

  /**
   * Method wrapping {@link WinscardLibrary#SCardTransmit}.
   *
   * <p><i><b>Note:</b> Compared to {@link WinscardLibrary#SCardTransmit} this method has no
   * parameter {@code cbSendLength}, because this method calculates that value from parameter {@code
   * sendBuffer}.</i>
   *
   * @param card see {@link WinscardLibrary#SCardTransmit}}
   * @param pioSendPci see {@link WinscardLibrary#SCardTransmit}}
   * @param sendBuffer see {@link WinscardLibrary#SCardTransmit}}
   * @param pioRecvPci see {@link WinscardLibrary#SCardTransmit}}
   * @param pbRecvBuffer see {@link WinscardLibrary#SCardTransmit}}
   * @param pcbRecvLength see {@link WinscardLibrary#SCardTransmit}}
   * @param executionTime <b>[OUT]</b> parameter with at least one element which receives the
   *     execution time of the underlying library call in seconds
   * @return see {@link WinscardLibrary#SCardTransmit}}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int scardTransmit(
      final ScardHandle card,
      final ScardIoRequest pioSendPci,
      final byte[] sendBuffer,
      final @Nullable ScardIoRequest pioRecvPci,
      final ByteBuffer pbRecvBuffer,
      final DwordByReference pcbRecvLength,
      final double... executionTime) {
    LOGGER
        .atTrace()
        .log(
            "SCardTransmit(hCard={}, pioSendPci={}, pbSendBuffer={}, pioRecvPci={},"
                + " pbRecvbuffer='...', pcbRecvLength={})",
            card,
            pioSendPci,
            Hex.toHexDigits(sendBuffer),
            pioRecvPci,
            // Hex.toHexDigits(pbRecvBuffer.array()),
            pcbRecvLength);

    final long startTime = System.nanoTime();
    final Dword code =
        getLib()
            .SCardTransmit(
                card,
                pioSendPci,
                ByteBuffer.wrap(sendBuffer),
                new Dword(sendBuffer.length),
                pioRecvPci,
                pbRecvBuffer,
                pcbRecvLength);
    final long runTime = System.nanoTime() - startTime;
    executionTime[0] = runTime * 1e-9;
    final long result = code.longValue();

    LOGGER
        .atTrace()
        .log(
            "status={}={} pioRecvPci={}, pbRecvBuffer={}, pcbRecvLength={}, runTime={}",
            String.format("0x%x", result),
            PcscStatus.getExplanation((int) result),
            pioRecvPci,
            // Hex.toHexDigits(pbRecvBuffer.array()),
            Hex.toHexDigits(pbRecvBuffer.array(), 0, pcbRecvLength.getValue().intValue()),
            pcbRecvLength,
            AfiUtils.nanoSeconds2Time(runTime));

    return (int) result;
  } // end method */

  /**
   * Method simplifying {@link WinscardLibrary#SCardTransmit}.
   *
   * @param card see {@link WinscardLibrary#SCardTransmit}}
   * @param sendBuffer command message to be transmitted
   * @param executionTime <b>[OUT]</b> parameter with at least one element which receives the
   *     execution time of the underlying library call in seconds
   * @return corresponding response message
   * @throws PcscException if the card operation fails
   */
  public byte[] scardTransmit(
      final ScardHandle card, final byte[] sendBuffer, final double... executionTime)
      throws PcscException {
    final ScardIoRequest pioSendPci = new ScardIoRequest(SCARD_PROTOCOL_T1);
    final byte[] rsp = new byte[0x1_0002]; // maximum size of a Response APDU
    final ByteBuffer pbRecvBuffer = ByteBuffer.wrap(rsp);
    final DwordByReference pcbRecvLength = new DwordByReference(new Dword(rsp.length));

    PcscStatus.check(
        "scardTransmit",
        scardTransmit(
            card, pioSendPci, sendBuffer, null, pbRecvBuffer, pcbRecvLength, executionTime));

    return Arrays.copyOfRange(rsp, 0, pcbRecvLength.getValue().intValue());
  } // end method */

  /**
   * Method wrapping {@link WinscardLibrary#SCardDisconnect(ScardHandle, Dword)}.
   *
   * @param card see {@link WinscardLibrary#SCardDisconnect}}
   * @param dwPosition see {@link WinscardLibrary#SCardDisconnect}}
   * @return see {@link WinscardLibrary#SCardDisconnect}}
   */
  public int scardDisconnect(final ScardHandle card, final int dwPosition) {
    LOGGER.atTrace().log("SCardDisconnect(hCard={}, dwPosition={})", card, dwPosition);

    final long startTime = System.nanoTime();
    final Dword code = getLib().SCardDisconnect(card, new Dword(dwPosition));
    final long runTime = System.nanoTime() - startTime;
    final long result = code.longValue();

    LOGGER
        .atTrace()
        .log(
            "status={}={} runTime={}",
            String.format("0x%x", result),
            PcscStatus.getExplanation((int) result),
            AfiUtils.nanoSeconds2Time(runTime));

    return (int) result;
  } // end method */

  /**
   * Method wrapping {@link WinscardLibrary#SCardReleaseContext(ScardContext)}.
   *
   * <p><b>Observations:</b>
   *
   * <ol>
   *   <li>It does not matter if {@code SCardEstablishContext} was not successful. The context can
   *       be released successfully anyway.
   *   <li>A context can be released only once. Another call of {@code SCardReleaseContext} produces
   *       {@link PcscStatus#SCARD_E_INVALID_HANDLE SCARD_E_INVALID_HANDLE}.
   * </ol>
   *
   * <p><i><b>Implementation recommendations:</b></i>
   *
   * <ol>
   *   <li><i>Either assume that the used {@link ScardContext} is valid, or</i>
   *   <li><i>check validity with TODO #SCardIsValidContext(ScardContext).</i>
   * </ol>
   *
   * @param context see {@code WinscardLibrary.SCardReleaseContext}}
   * @return see {@code WinscardLibrary.SCardReleaseContext}}
   */
  public int scardReleaseContext(final ScardContext context) {
    LOGGER.atTrace().log("SCardReleaseContext(hContext={}", context);

    final long startTime = System.nanoTime();
    final Dword code = getLib().SCardReleaseContext(context);
    final long runTime = System.nanoTime() - startTime;
    final long result = code.longValue();

    LOGGER
        .atTrace()
        .log(
            "status={}={} runTime={}",
            String.format("0x%x", result),
            PcscStatus.getExplanation((int) result),
            AfiUtils.nanoSeconds2Time(runTime));

    return (int) result;
  } // end method */

  /**
   * Getter.
   *
   * @return global protocol control information (PCI) structures.
   */
  @VisibleForTesting
  /* package */ List<ScardIoRequest> getScardPciList() {
    return insScardPciList;
  } // end method */
} // end class
