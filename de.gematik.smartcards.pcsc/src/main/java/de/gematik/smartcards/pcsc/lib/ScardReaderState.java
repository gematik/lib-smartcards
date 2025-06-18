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

import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_ATRMATCH;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_CHANGED;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_EMPTY;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_EXCLUSIVE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_IGNORE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_INUSE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_MUTE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_PRESENT;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_UNAVAILABLE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_UNAWARE;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_UNKNOWN;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_STATE_UNPOWERED;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset;
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * The {@code SCARD_READERSTATE} struct used by {@link WinscardLibrary#SCardGetStatusChange
 * SCardGetStatusChange}, see pcsclite.h.
 *
 * <p>On each platform, the sizeof and alignment is different. On Windows, {@link ScardReaderState}
 * is explicitly aligned to word boundaries.
 *
 * <ul>
 *   <li><b>Windows</b> has extra padding after {@link ScardReaderState#rgbAtr rgbAtr}, so that the
 *       structure is aligned at word boundaries even when it is in an array {@link
 *       ScardReaderState}[]
 *       <pre>
 *         sizeof(SCARD_READERSTATE_A):
 *         windows x86: 4+4+4+4+4+36 = 56
 *         windows x64: 8+8+4+4+4+36 = 64
 *         structure alignment: not sure (but it doesn't matter)
 *       </pre>
 *   <li><b>OSX</b> has no extra padding around {@link ScardReaderState#rgbAtr rgbAtr}, and
 *       pcsclite.h contains "#pragma pack(1)", so it is not word-aligned.
 *       <pre>
 *         sizeof(SCARD_READERSTATE_A):
 *         osx x86: 4+4+4+4+4+33 = 53
 *         osx x64: 8+8+4+4+4+33 = 61
 *         structure alignment: packed
 *       </pre>
 *   <li><b>Linux</b> pcsclite.h has no extra padding around {@link #rgbAtr rgbAtr}, but it is
 *       aligned by default. In addition, DWORD is typedef'd to long instead of int.
 *       <pre>
 *         sizeof(SCARD_READERSTATE_A):
 *         linux x86: 4+4+4+4+4+33 = 53
 *         linux x64: 8+8+8+8+8+33 = 73
 *         structure alignment: default
 *       </pre>
 * </ul>
 *
 * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
 * visibility of this class has to be "public".</i>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a
 *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
 *     SCARD_READERSTATE</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 */
// Note 1: Spotbugs claims "PA_PUBLIC_PRIMITIVE_ATTRIBUTE".
//         Short message: Primitive field is public and set from inside the class,
//             which makes it too exposed.
//         Rational: That primitive field has to be public.
// Note 2: Spotbugs claims "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD".
//         Spotbugs message: No writes were seen to this public/protected field.
//         Rational: The structure cannot be changed.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", // see note 1
  "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD" // see note 2
}) // */
// Note 2: According to the class comment of "com.sun.jna.Structure" it is
//         preferable to have a class annotation with the field order. If so,
//         then overriding getFieldOrder()-method is not necessary.
@Structure.FieldOrder({
  "szReader",
  "pvUserData",
  "dwCurrentState",
  "dwEventState",
  "cbAtr",
  "rgbAtr"
})
@SuppressWarnings({"PMD.TooManyStaticImports"})
public final class ScardReaderState extends Structure {

  /** Alignment. */
  private static final int ALIGN = Platform.isMac() ? ALIGN_NONE : ALIGN_DEFAULT; // */

  /** Part of formatting string used for attributes other than {@link #szReader}. */
  /* package */ static final String SUFFIX =
      ", pvUserData=%s, cS=0x%04x-%04x=%d-%s, eS=0x%04x-%04x=%d-%s, atrSize=%2d, ATR='%s'"; // */

  /**
   * Zero terminated string with name of card reader.
   *
   * <p>Set the value of this member to "\\?PnP?\Notification" and the values of all other instance
   * attributes to zero to be notified of the arrival of a new smart card reader.
   *
   * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
   * visibility of this instance attribute has to be "public".</i>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   */
  public @Nullable String szReader; // NOPMD no accessor, PA_PUBLIC_PRIMITIVE_ATTRIBUTE */

  /**
   * Pointer to user defined data.
   *
   * <p>Not used by the smart card subsystem. This member is used by the application.
   *
   * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
   * visibility of this instance attribute has to be "public".</i>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   */
  public @Nullable Pointer pvUserData; // NOPMD no accessor */

  /**
   * Current state of reader.
   *
   * <p>Current state of the reader, as seen by the application. This field can take on any of the
   * following values, in combination, as a bitmask.
   *
   * <ol>
   *   <li>{@link WinscardLibrary#SCARD_STATE_UNAWARE SCARD_STATE_UNAWARE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_IGNORE SCARD_STATE_IGNORE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_UNAVAILABLE SCARD_STATE_UNAVAILABLE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_EMPTY SCARD_STATE_EMPTY}
   *   <li>{@link WinscardLibrary#SCARD_STATE_PRESENT SCARD_STATE_PRESENT}
   *   <li>{@link WinscardLibrary#SCARD_STATE_ATRMATCH SCARD_STATE_ATRMATCH}
   *   <li>{@link WinscardLibrary#SCARD_STATE_EXCLUSIVE SCARD_STATE_EXCLUSIVE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_INUSE SCARD_STATE_INUSE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_MUTE SCARD_STATE_MUTE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_UNPOWERED SCARD_STATE_UNPOWERED}
   * </ol>
   *
   * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
   * visibility of this instance attribute has to be "public".</i>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
   */
  public Dword dwCurrentState = new Dword(); // NOPMD no accessor */

  /**
   * Reader state ofter a state change.
   *
   * <p>Current state of the reader, as known by the smart card resource manager. This field can
   * take on any of the following values, in combination, as a bitmask.
   *
   * <ol>
   *   <li>{@link WinscardLibrary#SCARD_STATE_IGNORE SCARD_STATE_IGNORE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_CHANGED SCARD_STATE_CHANGED}
   *   <li>{@link WinscardLibrary#SCARD_STATE_UNKNOWN SCARD_STATE_UNKNOWN}
   *   <li>{@link WinscardLibrary#SCARD_STATE_UNAVAILABLE SCARD_STATE_UNAVAILABLE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_EMPTY SCARD_STATE_EMPTY}
   *   <li>{@link WinscardLibrary#SCARD_STATE_PRESENT SCARD_STATE_PRESENT}
   *   <li>{@link WinscardLibrary#SCARD_STATE_ATRMATCH SCARD_STATE_ATRMATCH}
   *   <li>{@link WinscardLibrary#SCARD_STATE_EXCLUSIVE SCARD_STATE_EXCLUSIVE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_INUSE SCARD_STATE_INUSE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_MUTE SCARD_STATE_MUTE}
   *   <li>{@link WinscardLibrary#SCARD_STATE_UNPOWERED SCARD_STATE_UNPOWERED}
   * </ol>
   *
   * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
   * visibility of this instance attribute has to be "public".</i>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   * @see <a
   *     href="https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-rdpesc/3a235960-2fec-446b-8ed1-50bcc70e3c5f">MSDN
   *     Reader State</a>
   * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
   */
  public Dword dwEventState = dwCurrentState; // NOPMD no accessor */

  /**
   * Number of octets in ATR.
   *
   * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
   * visibility of this instance attribute has to be "public".</i>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   */
  public Dword cbAtr = dwCurrentState; // NOPMD no accessor */

  /**
   * Answer-To-Reset (ATR).
   *
   * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
   * visibility of this instance attribute has to be "public".</i>
   *
   * @see <a
   *     href="https://docs.microsoft.com/en-us/windows/win32/api/winscard/ns-winscard-scard_readerstatea">MSDN
   *     SCARD_READERSTATE</a>
   */
  public byte[] rgbAtr = new byte[AnswerToReset.MAX_ATR_SIZE]; // NOPMD no accessor */

  /** Default constructor. */
  public ScardReaderState() {
    super(null, ALIGN);

    // Note 1: Instance attributes are already set, see their definitions.
  } // end constructor */

  /**
   * Creates and initializes an array of {@link ScardReaderState}.
   *
   * <p>First an array is created and memory is allocated for the elements of that array. Then
   * {@link ScardReaderState#szReader} is set to names from {@code readerNames}.
   *
   * <p>If {@code withPnp} is {@code TRUE}, then an additional array element with {@link
   * ScardReaderState#szReader} set to {@link WinscardLibrary#PNP_READER_ID} is added as the first
   * element.
   *
   * @param readerNames names of readers, possibly empty
   * @param withPnp flag indicating if a special array element for Plug-And-Play readers shall be
   *     added to the result
   * @return appropriate allocated array
   */
  public static ScardReaderState[] createArray(
      final List<String> readerNames, final boolean withPnp) {
    // --- allocate an array for the result, i.e. PNP plus all entries in readerNames
    final ScardReaderState[] result = new ScardReaderState[(withPnp ? 1 : 0) + readerNames.size()];

    // --- allocate and fill memory for elements in the result
    // Note: First a new ScardReaderState()-object is created. It becomes the
    //       first element in "result". Thus, it is necessary to create a new
    //       object each time this method is called.
    //       So all ScardReaderState-objects returned by this method are
    //       independent of each other.
    new ScardReaderState().toArray(result);

    // --- set readerNames
    int i = 0;
    if (withPnp) {
      result[i++].szReader = WinscardLibrary.PNP_READER_ID;
    } // end fi

    for (final String name : readerNames) {
      result[i++].szReader = name;
    } // end For (name...)
    // end implementation 2 */

    return result;
  } // end method */

  /**
   * Return {@link String} representation.
   *
   * <p>The following information is concatenated:
   *
   * <ol>
   *   <li>"szReader={@link #szReader}"
   *   <li>"pvUserData={@link #pvUserData}"
   *   <li>"cS={@link #dwCurrentState} in six hex-digits prefixed with "0x"
   *   <li>"cS={@link #dwEventState} in six hex-digits prefixed with "0x"
   *   <li>"atrSize={@link #cbAtr} as two decimal digits (no leading zero)
   *   <li>"ATR='{@link #rgbAtr}' as hex-digits
   * </ol>
   *
   * @return {@link String} representation
   * @see com.sun.jna.Structure#toString()
   */
  @Override
  public String toString() {
    return toString("szReader=%s" + SUFFIX);
  } // end method */

  /**
   * Return {@link String} representation.
   *
   * <p>The following information is concatenated:
   *
   * <ol>
   *   <li>"szReader={@link #szReader}"
   *   <li>"pvUserData={@link #pvUserData}"
   *   <li>"cS={@link #dwCurrentState} in six hex-digits prefixed with "0x"
   *   <li>"cS={@link #dwEventState} in six hex-digits prefixed with "0x"
   *   <li>"atrSize={@link #cbAtr} as two decimal digits (no leading zero)
   *   <li>"ATR='{@link #rgbAtr}' as hex-digits
   * </ol>
   *
   * @param formatter used to format the result
   * @return {@link String} representation
   * @see com.sun.jna.Structure#toString()
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String toString(final String formatter) {
    final long currentState = dwCurrentState.longValue();
    final int csCounter = (int) (currentState >> 16);
    final int csState = (int) (currentState & 0xffff);
    final long eventState = dwEventState.longValue();
    final int esCounter = (int) (eventState >> 16);
    final int esState = (int) (eventState & 0xffff);

    // UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD
    return String.format(
        formatter,
        szReader,
        pvUserData,
        csCounter,
        csState,
        csCounter,
        toString(csState),
        esCounter,
        esState,
        esCounter,
        toString(esState),
        cbAtr.longValue(),
        Hex.toHexDigits(rgbAtr));
  } // end method */

  /**
   * Return a {@link String} representation of given array.
   *
   * @param readerStates array of states to be converted to {@link String}
   * @return {@link String} representation of parameter {@code readerStates}
   */
  public static String toString(final ScardReaderState... readerStates) {
    final int maxLengthName =
        Arrays.stream(readerStates)
            .mapToInt(state -> (null == state.szReader) ? 0 : state.szReader.length())
            .max()
            .orElse(0);
    final String formatter = "szReader=%-" + maxLengthName + "s" + SUFFIX;
    final AtomicInteger counter = new AtomicInteger();

    return String.format(
        "[%n%s%n]",
        Arrays.stream(readerStates)
            .map(
                state ->
                    String.format("%3d.: %s", counter.incrementAndGet(), state.toString(formatter)))
            .collect(Collectors.joining(LINE_SEPARATOR)));
  } // end method */

  /**
   * Return unmodifiable list with status.
   *
   * @param state to be explained
   * @return unmodifiable list with bits in {@code state} explained
   */
  @VisibleForTesting // otherwise = private
  /* package */ static List<String> toString(final int state) {
    if (SCARD_STATE_UNAWARE == (state & 0x7fff)) {
      return Collections.emptyList();
    } // end fi

    return Stream.of(
            Map.entry(SCARD_STATE_IGNORE, "ignore"),
            Map.entry(SCARD_STATE_CHANGED, "changed"),
            Map.entry(SCARD_STATE_UNKNOWN, "unknown"),
            Map.entry(SCARD_STATE_UNAVAILABLE, "unavailable"),
            Map.entry(SCARD_STATE_EMPTY, "empty"),
            Map.entry(SCARD_STATE_PRESENT, "present"),
            Map.entry(SCARD_STATE_ATRMATCH, "atrmatch"),
            Map.entry(SCARD_STATE_EXCLUSIVE, "exclusive"),
            Map.entry(SCARD_STATE_INUSE, "inuse"),
            Map.entry(SCARD_STATE_MUTE, "mute"),
            Map.entry(SCARD_STATE_UNPOWERED, "unpowered"))
        .filter(entry -> (0 != (state & entry.getKey())))
        .map(Map.Entry::getValue)
        .toList();
  } // end method */
} // end class
