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

import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.Serial;
import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Class storing an Answers-To-Reset as defined in ISO/IEC 7816-3.
 *
 * <p><i><b>Note:</b> Unless otherwise stated references are valid for ISO/IEC 7816-3:2006.</i>
 *
 * <p>The functionality provided here is a superset of {@code javax.smartcardio.ATR} which is a
 * final class.
 *
 * <p>From the perspective of this class
 *
 * <ol>
 *   <li>Instances are immutable value-types. Thus, {@link Object#equals(Object) equals()} and
 *       {@link Object#hashCode() hashCode()} are overwritten {@link Object#clone() clone()} is not
 *       overwritten.
 *   <li>Where data is passed in or out, defensive cloning is performed.
 *   <li>Methods are thread-safe.
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.CyclomaticComplexity",
  "PMD.GodClass",
  "PMD.TooManyMethods"
})
public final class AnswerToReset implements Serializable, Comparable<AnswerToReset> {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 810909513133011906L; // */

  /**
   * Maximum number of octets in an Answer-To-Reset.
   *
   * <p>According to ISO/IEC 7816-3 the initial character TS is NOT part of an Answer-To-Reset (ATR)
   * and ISO/IEC 7816-3 states that an ATR <b>SHALL</b> have no more than 32 octets. However,
   * traditionally engineers treat TS as part of an ATR. Thus, here the maximum number of octets in
   * an ATR is set to 33 rather than 32.
   */
  public static final int MAX_ATR_SIZE = 33; // */

  /**
   * Mapping for clock-stop-mode values.
   *
   * <p>Values in first TA for TA=15 are mapped to human-readable clock stop mode. For more
   * information, see ISO/IEC 7816-3:2006 table 9.
   */
  public static final Map<Integer, String> CLOCK_STOP_MODE =
      Map.ofEntries(
          Map.entry(0, "clock stop not supported"),
          Map.entry(1, "clock stop supported in state L"),
          Map.entry(2, "clock stop supported in state H"),
          Map.entry(3, "clock stop supported, no state preference")); // */

  /**
   * Mapping for voltage-class values.
   *
   * <p>Values in first TA for TA=15 are mapped to human-readable voltage classes. For more
   * information, see ISO/IEC 7816-3:2006 table 10.
   */
  public static final Map<Integer, String> VOLTAGE_CLASSES =
      Map.ofEntries(
          Map.entry(1, "class A"),
          Map.entry(2, "class B"),
          Map.entry(3, "class A and B"),
          Map.entry(4, "class C"),
          Map.entry(6, "class B and C"),
          Map.entry(7, "class A, B and C")); // */

  /** String constant. */
  private static final String F_02X = "'%02x' => "; // */

  /** Octet string of Answer-To-Reset as given by constructor including TS character. */
  private final byte[] insAnswerToReset; // NOPMD missing accessor */

  /** Flag indicating that Answer-To-Reset has more octet than expected. */
  /* package */ final boolean insHasExtraOctet; // NOPMD missing accessor */

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally the visibility of this instance attribute is "protected". Thus,
   *       subclasses are able to get and set it.</i>
   *   <li><i>Because only immutable instance attributes of this class and all subclasses are taken
   *       into account for this instance attribute lazy initialization is possible.</i>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  private volatile int insHashCode; // NOPMD volatile not recommended */

  /**
   * Historical bytes, copied from {@link #insAnswerToReset}.
   *
   * <p>If historical bytes are absent then the array is empty.
   */
  private final HistoricalBytes insHistoricalBytes; // */

  /** Set with protocols implicitly or explicitly indicated in ATR. */
  private final EnumSet<EafiIccProtocol> insProtocols; // NOPMD missing accessor */

  /**
   * Initial Character TS according to ISO/IEC 7816-3 clause 8.1.
   *
   * <p>Strictly speaking TS is not part of the Answer-To-Reset. Possible values are defined in
   * ISO/IEC 7816-3:2006 clause 8.1 $4:
   *
   * <ol>
   *   <li>'3b' = 0x3b, i.e. direct convention according to dash 2.
   *   <li>'3f' = 0x3f, i.e. inverse convention according to dash 1.
   * </ol>
   *
   * <p>This is a convenience attribute containing the first octet of {@link #insAnswerToReset}.
   */
  private final int insTs; // */

  /**
   * Format byte T0 according to ISO/IEC 7816-3 clause 8.2.2.
   *
   * <p>This is a convenience attribute containing the second octet of {@link #insAnswerToReset}.
   */
  private final int insT0; // */

  /**
   * Instance attribute containing the interface bytes, see ISO/IEC 7816-3:2006 table 6 and clause
   * 8.2.3.
   *
   * <p>List of {TAi, TBi, TCi, TDi} cluster. Each element contains one cluster.
   *
   * <ol>
   *   <li>bits b32..b25 store TAi
   *   <li>bits b24..b17 store TBi
   *   <li>bits b16..b09 store TCi
   *   <li>bits b08..b01 store TDi
   * </ol>
   *
   * <p>The first element with {@code index = 0} is artificial and contains Y1 (see ISO/IEC
   * 7816-3:2006 fig.13) at a position where usually TD0 would be stored. The four least significant
   * bit in that element are always zero.
   */
  private final List<Integer> insTabcd; // NOPMD missing accessor */

  /**
   * Check Character TCK according to ISO/IEC 7816-3:2006 clause 8.2.5.
   *
   * <p>From range [-1, 255], -1 indicates that this byte is absent.
   */
  private final int insTck; // */

  /**
   * Constructs an ATR from an octet string.
   *
   * <p>The first octet in the octet string <b>SHALL</b> be the Initial Character TS, although
   * strictly speaking that octet does not belong to the Answer-To-Reset, see ISO/IEC 7816-3:2006
   * clause 8.1.
   *
   * <p>This constructor accepts octet strings
   *
   * <ol>
   *   <li>if check byte TCK is absent although expected according to rules in ISO/IEC 7816-3:2006
   *   <li>if check byte TCK is present although expected to be absent according to the rules in
   *       ISO/IEC 7816-3:2006
   *   <li>if the octet string contains octets after check byte TCK
   * </ol>
   *
   * @param atr contains a byte-array from which the Answer-To-Reset is constructed
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>initial character TS is absent, see ISO/IEC 7816-3:2006 clause 8.1
   *       <li>format byte T0 is absent, see ISO/IEC 7816-3:2006 table 6
   *       <li>the Answer-To-Reset is shorter than indicated by format byte T0 (indicates length of
   *           historical bytes) or TDi
   *     </ol>
   */
  public AnswerToReset(final byte[] atr) {
    // Note 1: Use defensive cloning to prevent subsequent modifications.
    // Note 2: Clone as soon as possible to reduce the risk of problems
    //         with object sharing.
    insAnswerToReset = atr.clone();

    // Note 3: intentionally here it is not checked whether atr is longer
    //         than 32 bytes, see ISO/IEC 7816-3:2006 clause 8.2.1 §2.
    //         That check is done in the check()-method.

    // --- copy information from input parameter atr to instance attributes
    final ByteBuffer buffer = ByteBuffer.wrap(insAnswerToReset);
    try {
      insTs = buffer.get() & 0xff; // read TS
      insT0 = buffer.get() & 0xff; // read T0

      // read clusters of TAi, TBi, TCi, TDi (if those interface bytes are present)
      insTabcd = parseTabcd(insT0, buffer);
      // ... all clusters read

      // handle historical bytes
      final byte[] historicalBytes = new byte[insT0 & 0x0f];
      buffer.get(historicalBytes);
      insHistoricalBytes = new HistoricalBytes(historicalBytes);

      // handle check byte
      insTck = buffer.hasRemaining() ? (buffer.get() & 0xff) : -1;
    } catch (BufferUnderflowException e) {
      throw new IllegalArgumentException(
          String.format("ERROR, ATR too short: '%s'", Hex.toHexDigits(insAnswerToReset)), e);
    } // end Catch (... e)

    insHasExtraOctet = buffer.hasRemaining();
    // ... given Answer-To-Reset is completely pared without errors

    // --- initialize derived instance attributes
    insProtocols = initializeProtocols();
  } // end constructor */

  /**
   * Retrieve cluster of interface bytes from buffer with Answer-To-Reset.
   *
   * <p>The return value is used to set instance attribute {@link #insTabcd}.
   *
   * @param t0 format byte T0
   * @param buffer with Answer-To-Reset
   * @return list with clusters
   * @throws BufferUnderflowException if an expected interface byte is missing
   */
  @VisibleForTesting // otherwise = private
  /* package */ static List<Integer> parseTabcd(final int t0, final ByteBuffer buffer) {
    final List<Integer> result = new ArrayList<>();

    int yi = t0 & 0xf0;
    result.add(yi); // add first element to cluster list with index=0
    while (yi != 0) {
      final int ta = (0x10 == (yi & 0x10)) ? (buffer.get() & 0xff) : 0;
      final int tb = (0x20 == (yi & 0x20)) ? (buffer.get() & 0xff) : 0;
      final int tc = (0x40 == (yi & 0x40)) ? (buffer.get() & 0xff) : 0;
      final int td = (0x80 == (yi & 0x80)) ? (buffer.get() & 0xff) : 0;

      result.add((ta << 24) | (tb << 16) | (tc << 8) | td); // add interface bytes to cluster

      yi = td & 0xf0; // calculate y(i+1) from high nibble of TDi
    } // end While (more cluster available)
    // ... all cluster read

    return result;
  } // end method */

  /**
   * Retrieve a set of implicitly and explicitly indicated protocols.
   *
   * <p>The return value is used to initialize instance attribute {@link #insProtocols}.
   */
  @SuppressWarnings({"PMD.LooseCoupling"})
  private EnumSet<EafiIccProtocol> initializeProtocols() {
    final EnumSet<EafiIccProtocol> result = EnumSet.noneOf(EafiIccProtocol.class); // NOPMD use Set

    // --- fill set with those protocols which are explicitly indicated
    for (int i = 1; isPresentTd(i); i++) {
      result.add(EafiIccProtocol.getInstance((byte) getTd(i)));
    } // end For (i...)
    // ... all TDi scanned for offered protocols

    // --- adjust set of indicated protocols by implicitly indicated protocol
    if (result.isEmpty()) {
      // ... no protocol explicitly indicated
      // Note: The implicit default protocol is T=0 according to ISO/IEC 7816-3:2006
      //       clause 8.2.3, cite: "If TD1 is absent, then the only offer is T=0."
      result.add(EafiIccProtocol.T0);
    } // end fi (default protocol implicitly indicated)

    return result;
  } // end method */

  /**
   * Checks this object, whether it is in accordance to ISO/IEC 7816-3:2006.
   *
   * @return list with findings, an empty list indicates no findings
   */
  public List<String> check() {
    final List<String> result = new ArrayList<>();

    checkAtrLength(result);
    checkTs(result);
    checkT0();
    checkTdi(result);
    checkRfuProtocols(result);
    checkTa1(result);
    checkTb1(result);
    checkTc1();
    checkTa2(result);
    checkTb2(result);
    checkTc2(result);
    checkT0InterfaceBytes(result);
    checkT1InterfaceBytes(result);
    checkT15InterfaceBytes(result);
    checkHistoricalBytes(result);
    checkTck(result);

    return result;
  } // end method */

  /**
   * Check the length of ATR.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>ATR contains more than 32 octet, see ISO/IEC 7816-3:2006 clause 8.2.1 §2.
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkAtrLength(final List<String> listOfFindings) {
    if (insAnswerToReset.length > MAX_ATR_SIZE) {
      // ... atr too long
      listOfFindings.add(
          "ATR SHALL NOT be longer than 32 bytes, see ISO/IEC 7816-3 clause 8.2.1 §2.");
    } // end fi
  } // end method */

  /**
   * Check the values of TDi.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>TD1 indicates a protocol not from set {T=0, T=1}.
   *   <li>Indicated protocols are not in ascending order.
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkTdi(final List<String> listOfFindings) {
    // --- check TD1
    if (isPresentTd(1)) {
      final int firstProtocol = getTd(1) & 0x0f;

      if (15 == firstProtocol) { // NOPMD literal in if statement
        // ... T=15 invalid in TD1
        listOfFindings.add("T=15 is invalid in TD1, see ISO/IEC 7816-3:2006 clause 8.2.3 §5");
      } else if (firstProtocol > 1) { // NOPMD literal in if statement
        // ... neither T=0 nor T=1 in TD1
        listOfFindings.add(
            "invalid protocol in TD1, see ISO/IEC 7816-3:2006 clause 8.2.3 last sentence");
      } // end else if
    } // end fi (TD1 present?)

    // --- order of indicated protocols
    int indicated;
    int protocol = 0; // initialize with the lowest possible protocol
    for (int i = 1; isPresentTd(i); protocol = indicated) {
      indicated = getTd(i++) & 0x0f; // NOPMD reassignment
      if (indicated < protocol) {
        listOfFindings.add(
            "indicated protocols not in ascending order, see ISO/IEC 7816-3:2006 clause 8.2.3 §5");
        break;
      } // end fi
    } // end For (i...)
  } // end method */

  /**
   * Check for RFU protocols.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>TDi indicates a protocol which is RFU according to ISO/IEC 7816-3:2006 clause 8.2.3 §4.
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkRfuProtocols(final List<String> listOfFindings) {
    final var rfu =
        EnumSet.complementOf(
            EnumSet.of(
                EafiIccProtocol.T0, EafiIccProtocol.T1, EafiIccProtocol.T14, EafiIccProtocol.T15));
    rfu.retainAll(insProtocols);

    if (!rfu.isEmpty()) {
      listOfFindings.add(
          "TDi indicate protocols which are RFU, see ISO/IEC 7816-3 clause 8.2.3 §4.");
    } // end fi
  } // end method */

  /**
   * Compares this object with given object.
   *
   * <p>The comparison is done on {@link String} representations of {@link #getBytes()}.
   *
   * @param atr other ATR used for comparison
   * @return the value 0 if the argument string is equal to this string; a value less than 0 if this
   *     string is lexicographically less than the string argument; and a value greater than 0 if
   *     this string is lexicographically greater than the string argument.
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final AnswerToReset atr) {
    // Note: It is sufficient to just compare the instance attribute insAnswerToReset,
    //       because all other instance attributes are derived from that one.
    return Hex.toHexDigits(this.insAnswerToReset).compareTo(Hex.toHexDigits(atr.insAnswerToReset));
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * @return true if objects are equal, false otherwise
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final @CheckForNull Object obj) {
    // Note 1: Because this class is a direct subclass of Object calling
    //         super.equals(...) would be wrong. Instead, special checks are
    //         performed.

    // --- reflexive
    if (this == obj) {
      return true;
    } // end fi
    // ... obj is not the same as this

    if (null == obj) {
      // ... this differs from null
      return false;
    } // end fi
    // ... obj not null

    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end fi
    // ... obj is an instance of AnswerToReset

    final AnswerToReset other = (AnswerToReset) obj;

    // --- compare instance attributes
    // Note: It is sufficient to just compare the instance attribute insAnswerToReset,
    //       because all other instance attributes are derived from that one.
    return Arrays.equals(this.insAnswerToReset, other.insAnswerToReset);
  } // end method */

  /**
   * Returns Block Waiting Integer (BWI), see ISO/IEC 7816-3 clause 11.4.3.
   *
   * @return BWT as indicated in this {@link AnswerToReset}
   * @throws IllegalStateException if T=1 is not indicated in this {@link AnswerToReset}
   */
  public int getBwi() {
    if (!insProtocols.contains(EafiIccProtocol.T1)) {
      throw new IllegalStateException("T=1 not indicated in ATR => BWI is meaningless");
    } // end fi
    // ... T=1 indicated in ATR

    // Note: ISO/IEC 7816-3:2006 clause 11.4.3 defines:
    //       BWI is contained in the bits b8 to b5 in the first TB for T=1.
    return (getT1Tb1() & 0xf0) >> 4; // BWI
  } // end method */

  /**
   * Returns Block Waiting Time (BWT), see ISO/IEC 7816-3 clause 11.4.3.
   *
   * @return BWT as indicated in this {@link AnswerToReset}
   * @throws IllegalStateException if T=1 is not indicated in this {@link AnswerToReset}
   */
  /* package */ String getBwt() {
    // Note: ISO/IEC 7816-3:2006 clause 11.4.3 defines:
    //       BWT = 11 etu + 2^BWI * 960 * Fd/f
    final int bwi = getBwi();

    if (bwi < 0xa) { // NOPMD literal in if statement
      // ... BWI in range [0, 9]
      int i = 1 << getBwi(); // 2^BWI
      i *= 960;
      i *= 372; // Fd = F default

      return String.format("11 etu + %d clock cycles", i);
    } else {
      // ... BWI in range ['a', 'f']
      return "RFU";
    } // end else
  } // end method */

  /**
   * Returns octet string representing this Answer-To-Reset.
   *
   * @return a clone of the bytes in this Answer-To-Reset.
   */
  public byte[] getBytes() {
    return insAnswerToReset.clone();
  } // end method */

  /**
   * Returns Character Waiting Integer (CWI), see ISO/IEC 7816-3 clause 11.4.3.
   *
   * @return CWI as indicated in this {@link AnswerToReset}
   * @throws IllegalStateException if T=1 is not indicated in this {@link AnswerToReset}
   */
  public int getCwi() {
    if (!insProtocols.contains(EafiIccProtocol.T1)) {
      throw new IllegalStateException("T=1 not indicated in ATR => CWI is meaningless");
    } // end fi
    // ... T=1 indicated in ATR

    // Note: ISO/IEC 7816-3 clause 11.4.3 defines:
    //       CWI is contained in the bits b4 to b1 in the first TB for T=1.
    return getT1Tb1() & 0xf;
  } // end method */

  /**
   * Returns Character Waiting Time (CWT), see ISO/IEC 7816-3 clause 11.4.3.
   *
   * @return CWT as indicated in this {@link AnswerToReset} in etu
   * @throws IllegalStateException if T=1 is not indicated in this {@link AnswerToReset}
   */
  /* package */ int getCwt() {
    // Note: ISO/IEC 7816-3 clause 11.4.3 defines:
    //       CWT = (11 + 2^CWI) etu
    return 11 + (1 << getCwi());
  } // end method */

  /**
   * Returns class indicator (Y), see ISO/IEC 7816-3:2006 table 10.
   *
   * @return class indicator (Y) from this {@link AnswerToReset}, a value from range [0, 63]
   */
  public int getClassIndicator() {
    return getT15Ta1() & 0x3f;
  } // end method */

  /**
   * Returns clock stop indicator (X), see ISO/IEC 7816-3:2006 table 9.
   *
   * @return clock stop indicator (X) from this {@link AnswerToReset}, a value from range [0, 3].
   */
  public int getClockStopIndicator() {
    return getT15Ta1() >> 6;
  } // end method */

  /**
   * Returns baud rate adjustment integer (Di), see ISO/IEC 7816-3:2006 table 8.
   *
   * @return Di or -1 if indicated value is RFU
   */
  public int getDi() {
    return List.of(
            // Bits b4 to 1 of TA1
            -1, // 0 = 0000 => RFU
            1, // 1 = 0001
            2, // 2 = 0010
            4, // 3 = 0011
            8, // 4 = 0100
            16, // 5 = 0101
            32, // 6 = 0110
            64, // 7 = 0111
            12, // 8 = 1000
            20, // 9 = 1001
            -1, // a = 1010 => RFU
            -1, // b = 1011 => RFU
            -1, // c = 1100 => RFU
            -1, // d = 1101 => RFU
            -1, // e = 1110 => RFU
            -1 // f = 1111 => RFU
            )
        .get(getTa1() & 0x0f);
  } // end method */

  /**
   * Returns clock rate conversion integer (Fi), see ISO/IEC 7816-3 table 7.
   *
   * @return Fi or -1 if indicated value is RFU
   */
  public int getFi() {
    return List.of(
            // Bits b8 to 5 of TA1
            372, // 0 = 0000
            372, // 1 = 0001
            558, // 2 = 0010
            744, // 3 = 0011
            1116, // 4 = 0100
            1488, // 5 = 0101
            1860, // 6 = 0110
            -1, // 7 = 0111 => RFU
            -1, // 8 = 1000 => RFU
            512, // 9 = 1001
            768, // a = 1010
            1024, // b = 1011
            1536, // c = 1100
            2048, // d = 1101
            -1, // e = 1110 => RFU
            -1 // f = 1111 => RFU
            )
        .get(getTa1() >> 4);
  } // end method */

  /**
   * Returns maximum value of frequency supported by the card f(max), see ISO/IEC 7816-3 table 7.
   *
   * @return maximum clock frequency in MHz or -1 if indicated value is RFU
   */
  public double getFmax() {
    return List.of(
            4, // 0 = 0000
            5, // 1 = 0001
            6, // 2 = 0010
            8, // 3 = 0011
            12, // 4 = 0100
            16, // 5 = 0101
            20, // 6 = 0110
            -1, // 7 = 0111 => RFU
            -1, // 8 = 1000 => RFU
            5, // 9 = 1001
            7.5, // a = 1010
            10, // b = 1011
            15, // c = 1100
            20, // d = 1101
            -1, // e = 1110 => RFU
            -1 // f = 1111 => RFU
            )
        .get(getTa1() >> 4)
        .doubleValue();
  } // end method */

  /**
   * Returns a copy of the historical bytes in this Answer-To-Reset.
   *
   * <p>If this Answer-To-Reset does not contain historical bytes, an array of length zero is
   * returned.
   *
   * @return a clone of the historical bytes in this Answer-To-Reset.
   */
  public HistoricalBytes getHistoricalBytes() {
    return insHistoricalBytes;
  } // end method */

  /**
   * Returns set with supported protocols.
   *
   * <p>The returned value is a clone of the information available in the instantiated object.
   *
   * @return set of supported protocols in this Answer-To-Reset
   */
  @SuppressWarnings({"PMD.LooseCoupling"})
  public EnumSet<EafiIccProtocol> getSupportedProtocols() {
    return insProtocols.clone();
  } // end method */

  /**
   * Returns format byte T0 according to ISO/IEC 7816-3:2006 clause 8.2.2.
   *
   * @return value of T0
   */
  public int getT0() {
    return insT0;
  } // end method */

  /**
   * Check the value of T0.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>None, because all values from range ['00', 'ff'] are allowed
   * </ol>
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkT0() {
    // Note: Intentionally here are no checks for T0, because all values are allowed,
    //       see ISO/IEC 7816-3:2006 clause 8.2.2.
  } // end method */

  /**
   * Explain format byte T0.
   *
   * @return explanation for format byte T0
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainT0() {
    final int t0 = getT0();
    final StringBuilder result =
        new StringBuilder(128)
            .append(String.format("Format byte           T0 ='%02x' => ", t0))
            .append(appendY(t0, 1));
    final int lenHistoricalBytes = t0 & 0xf;

    if (0 == lenHistoricalBytes) {
      result.append("no historical bytes");
    } else if (1 == lenHistoricalBytes) { // NOPMD literal in if statement
      result.append("one historical byte");
    } else {
      result.append(String.format("%d historical bytes", lenHistoricalBytes));
    } // end else

    return result.toString();
  } // end method */

  /**
   * Initial Character TS according to ISO/IEC 7816-3:2006 clause 8.1.
   *
   * <p>Strictly speaking TS is not part of the Answer-To-Reset.
   *
   * @return value of TS
   */
  public int getTs() {
    return insTs;
  } // end method */

  /**
   * Check value of TS.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>TS is not from set {'3b', '3f'}, see ISO/IEC 7816-3:2006 clause 8.1 §4.
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkTs(final List<String> listOfFindings) {
    if (!(isDirectConvention() || isInverseConvention())) {
      // ... wrong TS value
      listOfFindings.add("TS invalid, see ISO/IEC 7816-3 clause 8.1 §4.");
    } // end fi
  } // end method */

  /**
   * Explain initial character TS.
   *
   * @return explanation for initial character TS
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainTs() {
    final String convention;
    if (isDirectConvention()) {
      // ... direct convention
      convention = "direct";
    } else {
      // ... not direct convention
      convention = (isInverseConvention() ? "inverse" : "unknown");
    } // end else

    return String.format("Initial Character     TS ='%02x' => %s convention", getTs(), convention);
  } // end method */

  /**
   * Retrieve global interface byte TA1 according to ISO/IEC 7816-3:2006 clause 8.3.
   *
   * <p>If present in the ATR this is a copy from there, otherwise default from ISO/IEC 7816-3:2006
   * is used.
   *
   * @return global interface byte TA1
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getTa1() {
    return isPresentTa(1) ? getTa(1) : 0x11;
  } // end method */

  /**
   * Check value of TA1.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>RFU value in high nibble of TA1
   *   <li>RFU value in low nibble of TA1
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkTa1(final List<String> listOfFindings) {
    if (-1 == getFi()) {
      listOfFindings.add("RFU value in high nibble of TA1, see ISO/IEC 7816-3:2006 table 7");
    } // end fi

    if (-1 == getDi()) {
      listOfFindings.add("RFU value in low nibble of TA1, see ISO/IEC 7816-3:2006 table 8");
    } // end fi
  } // end method */

  /**
   * Explain global interface byte TA1 according to ISO/IEC 7816-3:2006 clause 8.3.
   *
   * @return explanation for global interface byte TA1
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainTa1() {
    return String.format(
        "Clock rate, baud rate TA1=%s %s",
        isPresentTa(1) ? String.format("'%02x' =>", getTa1()) : "---- => default values,",
        String.format("Fi=%d, Di=%d, fmax=%.1f MHz", getFi(), getDi(), getFmax()));
  } // end method */

  /**
   * Check value of TB1.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>deprecated TB1 is present
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkTb1(final List<String> listOfFindings) {
    if (isPresentTb(1)) {
      listOfFindings.add(
          "Deprecated indication of external programming voltage in TB1,"
              + " see ISO/IEC 7816-3 table 6");
    } // end fi
  } // end method */

  /**
   * Retrieve global interface byte TC1 according to ISO/IEC 7816-3:2006 clause 8.3.
   *
   * <p>If present in the ATR this is a copy from there, otherwise default from ISO/IEC 7816-3:2006
   * is used.
   *
   * @return global interface byte TC1
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getTc1() {
    return isPresentTc(1) ? getTc(1) : 0x00;
  } // end method */

  /**
   * Check value of TC1.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>None, because all values from range ['00', 'ff'] are allowed
   * </ol>
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkTc1() {
    // Note: Intentionally here are no checks for TC1, because all values are allowed,
    //       see ISO/IEC 7816-3:2006 clause 8.3.
  } // end method */

  /**
   * Explain global interface byte TC1 according to ISO/IEC 7816-3:2006 clause 8.3.
   *
   * @return explanation for global interface byte TC1
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainTc1() {
    final StringBuilder result = new StringBuilder(128).append("Extra guard time      TC1=");

    if (isPresentTc(1)) {
      // ... TC1 is present
      final int tc1 = getTc1();

      result.append(String.format(F_02X, tc1));

      if (0xff == tc1) { // NOPMD literal in if statement
        result.append("12 etu for T=0, 11 etu for T=1");
      } else {
        if (insProtocols.contains(EafiIccProtocol.T15)) {
          // ... TC1 present AND TC1 != 255 AND T=15 present
          result.append(String.format("12 etu + %d clock cycles", tc1 * getFi() / getDi()));
        } else {
          // ... TC1 present AND TC1 != 255 AND T=15 absent
          result.append(String.format("%d etu", 12 + tc1));
        } // end fi
      } // end fi
    } else {
      // ... TC1 absent
      result.append("---- => default value, 12 etu");
    } // end else

    return result.toString();
  } // end method */

  /**
   * Retrieve global interface byte TA2 according to ISO/IEC 7816-3:2006 clause 8.3.
   *
   * <p>If present in the ATR this is a copy from there, otherwise default from ISO/IEC 7816-3:2006
   * is used.
   *
   * @return global interface byte TA1
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getTa2() {
    return isPresentTa(2) ? getTa(2) : 0x00;
  } // end method */

  /**
   * Check value of TA2.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>bit b7 in TA2 is set
   *   <li>bit b6 in TA2 is set
   *   <li>protocol indicated in the low nibble of TA2 is not in set {T=0, T=1, T=14}
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkTa2(final List<String> listOfFindings) {
    if (isPresentTa(2)) {
      final int ta2 = getTa2();

      if (0 != (ta2 & 0x60)) {
        listOfFindings.add("RFU value in b7 or b6 of TA2, see ISO/IEC 7816-3:2006 clause 8.3");
      } // end fi

      final int protocol = ta2 & 0xf;
      if (15 == protocol) { // NOPMD literal in if statement
        listOfFindings.add("T=15 is not a real protocol and thus forbidden to be indicated in TA2");
      } else if (!Set.of(
              0, // T=0  is allowed in TA2
              1, // T=1  is allowed in TA2
              14 // T=14 is allowed in TA2
              )
          .contains(protocol)) {
        listOfFindings.add(
            "TA2 indicates a protocol which is RFU, see ISO/IEC 7816-3:2006 clause 8.2.3 §4");
      } // end else if
    } // end fi
  } // end method */

  /**
   * Explain global interface byte TA2 according to ISO/IEC 7816-3:2006 fig. 15.
   *
   * @return explanation for global interface byte TA2
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainTa2() {
    final StringBuilder result = new StringBuilder(128).append("Specific mode byte    TA2=");

    if (isPresentTa(2)) {
      // ... TA2 is present
      final int ta2 = getTa2();

      result
          .append(String.format("'%02x' => specific mode offering T=%d, ", ta2, ta2 & 0xf))
          .append((0x00 == (ta2 & 0x80)) ? "capable" : "unable") // NOPMD use single append
          .append(" to change to negotiable mode, ") // NOPMD use single append
          .append("Fi, Di, fmax ")
          .append((0 == (ta2 & 0x10)) ? "taken from TA1" : "implicitly known");
    } else {
      // ... TA2 is absent
      result.append("---- => negotiable mode");
    } // end else

    return result.toString();
  } // end method */

  /**
   * Check value of TB2.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>deprecated TB2 is present
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkTb2(final List<String> listOfFindings) {
    if (isPresentTb(2)) {
      listOfFindings.add(
          "Deprecated indication of external programming voltage in TB2,"
              + " see ISO/IEC 7816-3 table 6");
    } // end fi
  } // end method */

  /**
   * Retrieves protocol specific interface byte TC2 according to ISO/IEC 7816-3:2006 clause 8.2.3.
   *
   * <p>If present in the ATR this is a copy from there, otherwise default from ISO/IEC 7816-3:2006
   * is used.
   *
   * @return protocol specific interface byte TC2
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getTc2() {
    return isPresentTc(2) ? getTc(2) : 10;
  } // end method */

  /**
   * Check value of TC2.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>T=0 is indicated but value of TC2 is RFU
   *   <li>T=0 is not indicated but TC2 is present
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkTc2(final List<String> listOfFindings) {
    if (insProtocols.contains(EafiIccProtocol.T0)) {
      // ... T=0 indicated in ATR, possibly implicitly
      // RFU in TC2?
      if (0 == getTc2()) {
        listOfFindings.add("TC2=0 is RFU, see ISO/IEC 7816-3 clause 10.2 §3");
      } // end fi
    } else if (isPresentTc(2)) {
      // ... T=0 NOT indicated, even not implicitly, but TC2 is present
      listOfFindings.add("T=0 is not indicated, but TC2 is present");
    } // end fi
  } // end method */

  /**
   * Explain T=0 specific interface byte TC2 according to ISO/IEC 7816-3:2006 clause 10.2.
   *
   * <p>If protocol {@code T=0} is
   *
   * <ol>
   *   <li>indicated then an appropriate explanation is added to the given list.
   *   <li>not indicated (not even implicitly) then the given list is not changed.
   * </ol>
   *
   * @param explanation list with explanations
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void explainTc2(final List<String> explanation) {
    final StringBuilder result = new StringBuilder(128);
    final int tc2 = getTc2(); // possibly default value

    if (insProtocols.contains(EafiIccProtocol.T0)) {
      // ... T=0 (possibly implicitly) indicated in ATR

      result
          .append("T=0 Waiting time      TC2=")
          .append(isPresentTc(2) ? String.format(F_02X, tc2) : "---- => default value, ")
          .append("WI=") // NOPMD use single append
          .append((0 == tc2) ? "RFU" : Integer.toString(tc2));
    } else if (isPresentTc(2)) {
      // ... T=0 not indicated in ATR, not even implicitly, but TC2 is present
      result.append(
          String.format(
              "T=0 Waiting time      TC2='%02x' present, although T=0 not indicated in ATR", tc2));
    } // end else

    if (!result.isEmpty()) {
      // ... explanation available
      explanation.add(result.toString());
    } // end fi
  } // end method */

  /**
   * Check T=0 interface bytes.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>T=0 specific interface bytes are present
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkT0InterfaceBytes(final List<String> listOfFindings) {
    final int firstCluster = firstTxCluster(0);

    if (firstCluster > 0) {
      listOfFindings.add(
          "protocol specific interface bytes for T=0 are not defined in ISO/IEC 7816-3:2006");
    } // end fi
  } // end method */

  /**
   * Check T=1 interface bytes.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>T=1 specific interface bytes are present more than once
   *   <li>TA for T=1 indicates RFU values
   *   <li>TB for T=1 indicates RFU values
   *   <li>TC for T=1 indicates RFU values
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkT1InterfaceBytes(final List<String> listOfFindings) {
    // --- check for second T=1 specific cluster
    final int protocol = 1;
    final int firstCluster = firstTxCluster(protocol);
    for (int i = firstCluster; isPresentTd(i); i++) {
      if (protocol == (getTd(i) & 0xf)) {
        listOfFindings.add(
            "protocol specific interface bytes for T=1 are not allowed more than once,"
                + " see ISO/IEC 7816-3:2006 clause 11.4.1 §1");
      } // end fi
    } // end For (i...)

    // --- check TA for T=1
    final int ta = getT1Ta1();
    if ((0 == ta) || (0xff == ta)) {
      listOfFindings.add(
          String.format(
              "information field size coded in TA%d is RFU,"
                  + " see ISO/IEC 7816-3 clause 11.4.2 §3 dash 1",
              firstCluster));
    } // end fi

    // --- check TB for T=1
    if (0xa0 <= getT1Tb1()) { // NOPMD literal in if statement
      listOfFindings.add(
          String.format(
              "block waiting integer in TB%d is RFU, see ISO/IEC 7816-3:2006 clause 11.4.3",
              firstCluster));
    } // end fi

    // --- check TC for T=1
    if (0 != (getT1Tc1() & 0xfe)) {
      listOfFindings.add(
          String.format(
              "redundancy code in TC%d is RFU, see ISO/IEC 7816-3:2006 clause 11.4.4 §2",
              firstCluster));
    } // end fi
  } // end method */

  /**
   * Check T=15 interface bytes.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>T=15 specific interface bytes are present more than once
   *   <li>TA for T=15 indicates RFU values
   *   <li>TB for T=15 indicates RFU values
   *   <li>TB for T=15 indicates proprietary values
   *   <li>TC for T=15 is present
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkT15InterfaceBytes(final List<String> listOfFindings) {
    // --- check for second T=15 specific cluster
    final int protocol = 15;
    final int firstCluster = firstTxCluster(protocol);
    for (int i = firstCluster; isPresentTd(i); i++) {
      if (protocol == (getTd(i) & 0xf)) {
        listOfFindings.add(
            "global interface bytes for T=15 are not allowed more than once,"
                + " see ISO/IEC 7816-3:2006 clause 8.3");
      } // end fi
    } // end For (i...)

    // --- check TA for T=15
    final int ta = getT15Ta1();
    if (!VOLTAGE_CLASSES.containsKey(ta & 0x3f)) {
      listOfFindings.add(
          String.format(
              "class indication in TA%d is RFU, see ISO/IEC 7816-3:2006 table 10", firstCluster));
    } // end fi

    // --- check TB for T=15
    if (isPresentTb(firstCluster)) {
      final int tb = getT15Tb1();

      if (tb < 128) { // NOPMD literal in if statement
        // ... bit b8 in TB for T=15 is not set
        if (0 != tb) {
          listOfFindings.add(
              String.format(
                  "indication of usage of C6 in TB%d is RFU, see ISO/IEC 7816-3 clause 8.3",
                  firstCluster));
        } // end fi
      } else {
        // ... bit b8 in TB for T=15 is set
        listOfFindings.add(
            String.format(
                "TB%d for T=15 is proprietary," + " see ISO/IEC 7816-3:2006 clause 8.3",
                firstCluster));
      } // end else
    } // end fi (TB for T=15 present?)

    // --- check TC for T=15
    if (isPresentTc(firstCluster)) {
      listOfFindings.add(
          String.format(
              "TC for T=15 is not defined, thus TC%d is RFU,"
                  + " see ISO/IEC 7816-3 clause 8.3 §1 and §2",
              firstCluster));
    } // end fi (TC for T=15 present?)
  } // end method */

  /**
   * Check historical bytes.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>Checksum is required but absent.
   *   <li>Checksum is forbidden but present.
   *   <li>Checksum is required, present, but wrong.
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkHistoricalBytes(final List<String> listOfFindings) {
    listOfFindings.addAll(getHistoricalBytes().check());
  } // end method */

  /**
   * Returns TCK.
   *
   * @return value of TCK, value -1 indicates that this byte is absent
   */
  public int getTck() {
    return insTck;
  } // end method */

  /**
   * Check TCK.
   *
   * <p>If findings are present, then they are added to {@code listOfFindings}. Possible findings:
   *
   * <ol>
   *   <li>Checksum is required but absent.
   *   <li>Checksum is forbidden but present.
   *   <li>Checksum is required, present, but wrong.
   * </ol>
   *
   * @param listOfFindings name says it all
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void checkTck(final List<String> listOfFindings) {
    if (isChecksumRequired()) {
      // ... checksum is required
      checkTckPresence(listOfFindings);
    } else {
      // ... checksum not required
      if (-1 != getTck()) {
        // ... checksum not required but present
        listOfFindings.add("checksum forbidden, but available, see ISO/IEC 7816-3 clause 8.2.5");
      } // end fi (TCK present)
    } // end else (Checksum required?)
  } // end method */

  private void checkTckPresence(final List<String> listOfFindings) {
    final int present = getTck();
    int tck = 0x00; // initialize

    if (-1 == present) {
      // ... checksum is required, but TCK is absent in ATR
      //     => inform user

      // Note: TS is not taken into account for checksum calculation.
      for (int i = insAnswerToReset.length; i-- > 1; ) { // NOPMD assignment in operand
        tck ^= insAnswerToReset[i] & 0xff;
      } // end For (i...)

      listOfFindings.add(
          String.format(
              "checksum required, but absent, should be TCK='%02x',"
                  + " see ISO/IEC 7816-3 clause 8.2.5",
              tck));
    } else {
      // ... checksum is required AND TCK is present in ATR
      //     => check for correctness

      // Note: Neither the present TCK nor TS are taken into account for checksum calculation.
      for (int i = insAnswerToReset.length - 1; i-- > 1; ) { // NOPMD assignment in operand
        tck ^= insAnswerToReset[i] & 0xff;
      } // end For (i...)

      if (present != tck) {
        listOfFindings.add(
            String.format(
                "wrong checksum, found TCK='%02x', should be TCK='%02x',"
                    + " see ISO/IEC 7816-3 clause 8.2.5",
                present, tck));
      } // end fi
    } // end else (TCK present?)
  } // end method */

  /**
   * Explain check byte TCK.
   *
   * @return explanation for check byte TCK
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainTck() {
    return String.format(
        "Checksum              TCK=%s",
        (-1 == getTck()) ? "----" : String.format("'%02x'", getTck()));
  } // end method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * @return hash-code of object
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    // Note 1: Because this class is a direct subclass of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because equals() takes into all octet of the ATR
    //         here we can do the same.
    // Note 3: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from main memory into thread local memory
    if (0 == result) {
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion 1: instance attributes are never null

      result = Arrays.hashCode(insAnswerToReset);

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result; // return hashCode from thread local memory
  } // end method */

  /**
   * Examines ATR and checks whether a checksum is required.
   *
   * <p>See to ISO/IEC 7816-3:2006 clause 8.2.5.
   *
   * @return true if TCK shall be present, false otherwise.
   */
  public boolean isChecksumRequired() {
    // Note: ISO/IEC 7816-3:2006 clause 8.2.5 states:
    //       If only T=0 is indicated, possibly by default, then TCK shall be absent.
    //       If T=0 and T=15 are present and in all the other cases, TCK shall be present.

    // From the 2nd sentence in 7816-3 clause 8.2.5 it follows that TCK shall be present if
    // more than one protocol is indicated.
    // From the 1st sentence it follows that TCK shall be absent if T=0 is the
    // only indicated protocol.
    return (insProtocols.size() > 1) || !insProtocols.contains(EafiIccProtocol.T0);
  } // end method */

  /**
   * Method tells if "direct convention" is indicated in this ATR.
   *
   * @return true if and only if this ATR indicates Direct Convention (TS == 0x3b)
   */
  public boolean isDirectConvention() {
    return 0x3b == getTs(); // see ISO/IEC 7816-3:2006 clause 8.1 §4 dash 2
  } // end method */

  /**
   * Method tells if "invers convention" is indicated in this ATR.
   *
   * @return true if and only if this ATR indicates Inverse Convention (TS == 0x3f)
   */
  public boolean isInverseConvention() {
    return 0x3f == getTs(); // see ISO/IEC 7816-3:2006 clause 8.1 §4 dash 1
  } // end method */

  /**
   * Indicates if a certain interface byte is present.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @param position indicating which part of cluster i is asked for, <b>SHALL</b> be in set {A, B,
   *     C, D}
   * @return true if TX(i) is present, false otherwise
   * @throws IllegalArgumentException if {@code position} is not in set {A, B, C, D}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ boolean isPresent(final int i, final String position) {
    try {
      // Note: The presence or absence is indicated in the high nibble of
      //       TD(i-1) i.e. in bits b7..b4.
      // spotless:off
      final int mask = switch (position) {
        case "A" -> 0x10;
        case "B" -> 0x20;
        case "C" -> 0x40;
        case "D" -> 0x80;
        default -> throw new IllegalArgumentException("invalid position");
      }; // end Switch (position)
      // spotless:on

      return 0 != (insTabcd.get(i - 1) & mask);
    } catch (IndexOutOfBoundsException e) {
      // ... i is an invalid index into insTabcd
      //     => requested interface byte is absent
      return false;
    } // end Catch (
  } // end method */

  /**
   * Indicates if TAi is present in this ATR.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @return true if TAi is present, false otherwise
   */
  public boolean isPresentTa(final int i) {
    return isPresent(i, "A");
  } // end method */

  /**
   * Indicates if TBi is present in this ATR.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @return true if TBi is present, false otherwise
   */
  public boolean isPresentTb(final int i) {
    return isPresent(i, "B");
  } // end method */

  /**
   * Indicates if TCi is present in this ATR.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @return true if TCi is present, false otherwise
   */
  public boolean isPresentTc(final int i) {
    return isPresent(i, "C");
  } // end method */

  /**
   * Returns TDi form this ATR.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @return true if TD(i) is present, false otherwise
   */
  public boolean isPresentTd(final int i) {
    return isPresent(i, "D");
  } // end method */

  /**
   * Returns a certain interface byte.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @param position indicating which part of cluster i is asked for, <b>SHALL</b> be in set {A, B,
   *     C, D}
   * @return requested interface byte
   * @throws IllegalArgumentException if requested interface byte is absent
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getInterfaceByte(final int i, final String position) {
    if (!isPresent(i, position)) {
      throw new IllegalArgumentException(String.format("T%s%d is absent", position, i));
    } // end fi
    // ... value of parameter "position" is valid hereafter, because
    //     isPresent(int, String) throws an exception for an invalid "position"

    // spotless:off
    final int shifter = switch (position) {
      case "A" -> 24;
      case "B" -> 16;
      case "C" -> 8;
      default -> 0;
    }; // end Switch (position)
    // spotless:on

    return (insTabcd.get(i) >> shifter) & 0xff;
  } // end method */

  /**
   * Returns TAi form this ATR.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @return value of TA at given index
   * @throws IllegalArgumentException if requested interface byte is absent
   */
  public int getTa(final int i) {
    return getInterfaceByte(i, "A");
  } // end method */

  /**
   * Returns TBi form this ATR.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @return value of TB at given index
   * @throws IllegalArgumentException if requested interface byte is absent
   */
  public int getTb(final int i) {
    return getInterfaceByte(i, "B");
  } // end method */

  /**
   * Returns TCi form this ATR.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @return value of TC at given index
   * @throws IllegalArgumentException if requested interface byte is absent
   */
  public int getTc(final int i) {
    return getInterfaceByte(i, "C");
  } // end method */

  /**
   * Returns TDi form this ATR.
   *
   * @param i given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @return value of TD at given index
   * @throws IllegalArgumentException if requested interface byte is absent
   */
  public int getTd(final int i) {
    return getInterfaceByte(i, "D");
  } // end method */

  /**
   * Retrieves first TAi with {@code i &gt; 2} after {@code T=1} indication.
   *
   * <p>If present in the ATR this is a copy from there, otherwise default from ISO/IEC 7816-3:2006
   * clause 11.4.2 §1 is used.
   *
   * @return first TA for protocol {@code T=1}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getT1Ta1() {
    try {
      return getTa(firstTxCluster(1));
    } catch (IllegalArgumentException e) {
      // ... either no cluster with T=1 specific information OR
      //     such a cluster has no TA interface byte
      //     => return default value
      return 32;
    } // end Catch (...)
  } // end method */

  /**
   * Returns value of requested interface byte.
   *
   * @param clusterIndex given index {@code i} as defined in ISO/IEC 7816-3:2006 clause 8.2.1
   * @param position indicating which part of cluster {@code clusterIndex} is asked for,
   *     <b>SHALL</b> be in set {A, B, C, D}
   * @return if present value of requested interface byte, otherwise "default value"
   */
  private String txValue(final int clusterIndex, final String position) {
    try {
      return String.format("%d='%02x' =>", clusterIndex, getInterfaceByte(clusterIndex, position));
    } catch (IllegalArgumentException e) {
      // ... requested interface byte absent
      return ((clusterIndex > 0) ? Integer.toString(clusterIndex) : " ")
          + "=---- => default value,";
    } // end Catch (...)
  } // end method */

  /**
   * Explains first TA for {@code T=1}, see ISO/IEC 7816-3 clause 11.4.1, 11.4.2.
   *
   * @param clusterIndex index of cluster where T=1 specific interface bytes are found. If the
   *     corresponding cluster is not present then default values are explained.
   * @return explanation of first TA for protocol {@code T=1}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainT1Ta1(final int clusterIndex) {
    final int ta = getT1Ta1();
    final String ifsc = Set.of(0x00, 0xff).contains(ta) ? "RFU" : String.format("%d bytes", ta);

    return String.format("T=1 IFSC              TA%s %s", txValue(clusterIndex, "A"), ifsc);
  } // end method */

  /**
   * Retrieves first TBi with {@code i &gt; 2} after {@code T=1} indication.
   *
   * <p>If present in the ATR this is a copy from there, otherwise default from ISO/IEC 7816-3:2006
   * clause 11.4.3 is used.
   *
   * @return first TB for protocol {@code T=1}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getT1Tb1() {
    try {
      return getTb(firstTxCluster(1));
    } catch (IllegalArgumentException e) {
      // ... either no cluster with T=1 specific information OR
      //     such a cluster has no TB interface byte
      //     => return default value
      return 0x4d;
    } // end Catch (...)
  } // end method */

  /**
   * Explains first TB for {@code T=1}, see ISO/IEC 7816-3 clause 11.4.1, 11.4.3.
   *
   * @param clusterIndex index of cluster where T=1 specific interface bytes are found. If the
   *     corresponding cluster is not present then default values are explained.
   * @return explanation of first TB for protocol {@code T=1}
   * @throws IllegalStateException if T=1 is not indicated in this {@link AnswerToReset}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainT1Tb1(final int clusterIndex) {
    return String.format(
        "T=1 Waiting Times     TB%s BWT=%s, CWT=%d etu",
        txValue(clusterIndex, "B"),
        getBwt(), // throws IllegalArgumentException if T=1 is not indicated
        getCwt() // throws IllegalArgumentException if T=1 is not indicated
        );
  } // end method */

  /**
   * Retrieves first TCi with {@code i &gt; 2} after {@code T=1} indication.
   *
   * <p>If present in the ATR this is a copy from there, otherwise default from ISO/IEC 7816-3:2006
   * clause 11.4.4 is used.
   *
   * @return first TC for protocol {@code T=1}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getT1Tc1() {
    try {
      return getTc(firstTxCluster(1));
    } catch (IllegalArgumentException e) {
      // ... either no cluster with T=1 specific information OR
      //     such a cluster has no TC interface byte
      //     => return default value
      return 0;
    } // end Catch (...)
  } // end method */

  /**
   * Explains first TC for {@code T=1}, see ISO/IEC 7816-3 clause 11.4.1, 11.4.4.
   *
   * @param clusterIndex index of cluster where T=1 specific interface bytes are found. If the
   *     corresponding cluster is not present then default values are explained.
   * @return explanation of first TC for protocol {@code T=1}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainT1Tc1(final int clusterIndex) {
    return String.format(
        "T=1 Redundancy Code   TC%s %s",
        txValue(clusterIndex, "C"), (0 == (getT1Tc1() & 1)) ? "LRC" : "CRC");
  } // end method */

  /**
   * Retrieves first TAi with {@code i &gt; 2} after {@code T=15} indication.
   *
   * <p>If present in the ATR this is a copy from there, otherwise default value from ISO/IEC
   * 7816-3:2006 clause 8.3 and tables 9 and 10 is used.
   *
   * @return first TA for protocol {@code T=15}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getT15Ta1() {
    try {
      return getTa(firstTxCluster(15));
    } catch (IllegalArgumentException e) {
      // ... either no cluster with T=15 specific information OR
      //     such a cluster has no TA interface byte
      //     => return default value
      return 1;
    } // end Catch (...)
  } // end method */

  /**
   * Explains first TA for {@code T=15}, see ISO/IEC 7816-3 clause 8.3, tables 9 and 10.
   *
   * @param clusterIndex index of cluster where T=15 specific interface bytes are found. If the
   *     corresponding cluster is not present then default values are explained.
   * @return explanation of first TA for protocol {@code T=15}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainT15Ta1(final int clusterIndex) {
    return String.format(
        "ClockStop, Class      TA%s %s, %s",
        txValue(clusterIndex, "A"),
        CLOCK_STOP_MODE.get(getClockStopIndicator()),
        VOLTAGE_CLASSES.getOrDefault(getClassIndicator(), "class RFU"));
  } // end method */

  /**
   * Retrieves first TBi with {@code i &gt; 2} after {@code T=15} indication.
   *
   * <p>If present in the ATR this is a copy from there, otherwise default value from ISO/IEC
   * 7816-3:2006 clauses 5.2.4 and 8.3 is used.
   *
   * @return first TB for protocol {@code T=15}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int getT15Tb1() {
    try {
      return getTb(firstTxCluster(15));
    } catch (IllegalArgumentException e) {
      // ... either no cluster with T=1 specific information OR
      //     such a cluster has no TB interface byte
      //     => return default value
      return 0;
    } // end Catch (...)
  } // end method */

  /**
   * Explains first TB for {@code T=15}, see ISO/IEC 7816-3 clause 8.3.
   *
   * @param clusterIndex index of cluster where T=1 specific interface bytes are found. If the
   *     corresponding cluster is not present then default values are explained.
   * @return explanation of first TB for protocol {@code T=1}
   * @throws IllegalStateException if T=1 is not indicated in this {@link AnswerToReset}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainT15Tb1(final int clusterIndex) {
    final int tb = getT15Tb1();
    final String explanation;

    if (0 == tb) {
      explanation = "contact C6 not used";
    } else if (tb >= 128) { // NOPMD literal in if statement
      explanation = "proprietary usage of contact C6";
    } else {
      explanation = "contact C6 usage is RFU";
    } // end else

    return String.format("Standard/proprietary  TB%s %s", txValue(clusterIndex, "B"), explanation);
  } // end method */

  /**
   * Explains this Answer-To-Reset.
   *
   * @return list of string which explain this Answer-To-Reset
   */
  public List<String> explain() {
    final List<String> result = new ArrayList<>();

    result.add(explainTs()); // TS, see ISO/IEC 7816-3:2006 clause 8.1 §4
    result.add(explainT0()); // T0, see ISO/IEC 7816-3:2006 clause 8.2.2

    // --- explain first cluster with global interface bytes TA1, TB1, TC1
    result.add(explainTa1());
    explainTb12(result, true);
    result.add(explainTc1());

    // --- explain TD1 and receive that protocol which is first indicated
    result.add(explainTd1());

    // --- explain second cluster with global interface bytes TA2, TB2 and TC2 (T=0 specific)
    result.add(explainTa2());
    explainTb12(result, false);
    explainTc2(result);

    // --- explain TD2 and the following interface bytes (if present)
    explainTd2plus(result);

    // ---
    if (isT1DefaultParameter()) {
      // ... no cluster with T=1 specific interface bytes but T=1 indicated
      //     => explain default values as soon as possible

      result.addAll(explainT1specificInterfaceBytes());
    } // end fi

    // ---
    if (isT15DefaultParameter()) {
      // ... no cluster with T=15 global interface bytes
      //     => explain default values just before historical bytes.

      result.addAll(explainT15globalInterfaceBytes());
    } // end fi

    // --- explain historical bytes
    // FIXME

    // --- explain check byte TCK
    result.add(explainTck());

    // --- add findings from check
    result.addAll(check());

    return result;
  } // end method */

  /**
   * Estimates first T=x specific cluster.
   *
   * @param x protocol indicator
   * @return index of first cluster with T=x specific information, or {@code -1} if such a cluster
   *     is absent
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ int firstTxCluster(final int x) {
    for (int index = 2; isPresentTd(index); ) {
      final int ti = getTd(index++) & 0x0f; // NOPMD reassign, estimate protocol indicated in TDi

      if (x == ti) {
        // ... first cluster with T=x specific interface bytes present
        return index;
      } // end fi
    } // end For (index...)
    // ... no cluster with T=x specific information

    return -1;
  } // end method */

  /**
   * Estimates if T=1 is indicated but T=1 specific interface bytes are absent.
   *
   * @return {@code TRUE} if protocol T=1 is indicated, but cluster with T=1 specific interface
   *     bytes is absent, {@code FALSE} otherwise
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ boolean isT1DefaultParameter() {
    return insProtocols.contains(EafiIccProtocol.T1) && (firstTxCluster(1) < 0);
  } // end method */

  /**
   * Estimates if T=15 global interface bytes are absent.
   *
   * @return {@code TRUE} if cluster with T=15 global interface bytes is absent, {@code FALSE}
   *     otherwise
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ boolean isT15DefaultParameter() {
    return firstTxCluster(15) < 0;
  } // end method */

  /**
   * Explain global interface byte TB1 and TB2 according to ISO/IEC 7816-3:2006 clause 8.3.
   *
   * <p><i><b>Note:</b>According to ISO/IEC 7816-3:2006 clause 8.3 the global interface bytes TB1
   * and TB2 are deprecated.</i>
   *
   * <p>If TB1 (TB2) is
   *
   * <ol>
   *   <li>present then an appropriate explanation is added to the given list.
   *   <li>absent then the given list is not changed.
   * </ol>
   *
   * @param explanation list with explanations
   * @param isTb1Inspected flag indicating if TB1 ({@code TRUE}) or TB2 ({@code FALSE}) is explained
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void explainTb12(final List<String> explanation, final boolean isTb1Inspected) {
    final int clusterIndex = isTb1Inspected ? 1 : 2;

    if (isPresentTb(clusterIndex)) {
      // ... TB1 is present
      explanation.add(
          String.format("Deprecated            TB%d='%02x'", clusterIndex, getTb(clusterIndex)));
    } // end fi
  } // end method */

  /**
   * Explain TD1 (possibly absent).
   *
   * @return explanation for TD1
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ String explainTd1() {
    final StringBuilder result = new StringBuilder(128).append("Protocol indicator    TD1=");

    if (isPresentTd(1)) {
      // ... TD1 is present
      final int td1 = getTd(1);
      final int firstProtocol = td1 & 0x0f;

      result
          .append(String.format(F_02X, td1))
          .append(appendY(td1, 2))
          .append(
              String.format(
                  "%s offer is T=%d", (1 == insProtocols.size()) ? "only" : "1st", firstProtocol));
    } else {
      // ... TD1 is absent
      result.append("---- => implicit offer is T=0");
    } // end else

    return result.toString();
  } // end method */

  /**
   * Explain TD2 and all following interface bytes (if present).
   *
   * <p>If TD2 or more interface bytes are
   *
   * <ol>
   *   <li>present then an appropriate explanation is added to the given list.
   *   <li>absent then the given list is not changed.
   * </ol>
   *
   * @param explanation list with explanations
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ void explainTd2plus(final List<String> explanation) {
    final String[] orders = {
      "1st", "2nd", "3rd", "4th", "5th", "6th", "7th",
      "8th", "9th", "10th", "11th", "12th", "13th", "14th"
    };
    final String[] indices = new String[] {"A", "B", "C"};
    final StringBuilder tdiExplanation = new StringBuilder(128);

    int actualProtocol = isPresentTd(1) ? (getTd(1) & 0xf) : 0;
    int indexOrders = 0;
    for (int i = 2; isPresentTd(i); ) {
      // ... TDi is present
      //     => show it
      final int tdi = getTd(i);
      final int nextProtocol = tdi & 0x0f;
      tdiExplanation.setLength(0);
      tdiExplanation.append(
          String.format("Protocol indicator    TD%d='%02x' => %s", i, tdi, appendY(tdi, i + 1)));
      final String protocol = "T=" + nextProtocol;

      if (nextProtocol == actualProtocol) {
        // ... this TDi offers the same protocol as previous TD(i-1)
        tdiExplanation.append(protocol).append(" is offered once more");
      } else {
        // ... this TDi indicates a protocol different from protocol indicated in previous TDi
        actualProtocol = nextProtocol;
        tdiExplanation.append(orders[++indexOrders]).append(" offer is ").append(protocol);
      } // end else (same protocol?)
      explanation.add(tdiExplanation.toString());
      i++; // NOPMD reassign loop control variable, i now points into next cluster

      // --- check for protocol specific interface bytes
      if (firstTxCluster(1) == i) {
        // ... actual cluster is first with T=1 specific interface bytes
        explanation.addAll(explainT1specificInterfaceBytes());
      } else if (firstTxCluster(15) == i) {
        // ... actual cluster is first with T=15 global interface bytes
        explanation.addAll(explainT15globalInterfaceBytes());
      } else {
        // ... cluster with information not standardized in ISO/IEC 7816-3:2006
        //     => just show them
        explainTd2plusShow(explanation, indices, i);
      } // end else
    } // end For (i...)
  } // end method */

  private void explainTd2plusShow(
      final List<String> explanation, final String[] indices, final int i) {
    for (final String position : indices) {
      try {
        explanation.add(
            String.format(
                "RFU                   T%s%d='%02x'", position, i, getInterfaceByte(i, position)));
      } catch (IllegalArgumentException e) { // NOPMD empty catch block
        // ... requested interface byte absent
        //     => intentionally no action
      } // end Catch (...)
    } // end For (position -> ...)
  } // end method */

  /**
   * Method explains T=1 specific interface bytes.
   *
   * @return explanation of T=1 specific interface bytes
   * @throws IllegalStateException if T=1 is not indicated in this {@link AnswerToReset}
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ List<String> explainT1specificInterfaceBytes() {
    final int clusterIndex = firstTxCluster(1);

    return List.of(
        explainT1Ta1(clusterIndex), explainT1Tb1(clusterIndex), explainT1Tc1(clusterIndex));
  } // end method */

  /**
   * Method explains global interface bytes from T=15.
   *
   * @return explanation of T=15 specific interface bytes
   */
  @VisibleForTesting
  // otherwise = private
  /* package */ List<String> explainT15globalInterfaceBytes() {
    final int clusterIndex = firstTxCluster(15);

    final List<String> result = new ArrayList<>();

    result.add(explainT15Ta1(clusterIndex));
    result.add(explainT15Tb1(clusterIndex));

    // first TC for T=15, not defined in ISO/IEC 7816-3:2006
    if (isPresentTc(clusterIndex)) {
      // ... first TC with T=15 specific information is present
      result.add(
          String.format("RFU                   TC%d='%02x'", clusterIndex, getTc(clusterIndex)));
    } // end fi

    return result;
  } // end method */

  /**
   * Compiles a {@link String} representation of this Answer-To-Reset.
   *
   * <p>The {@link String} representation differs from the {@link #getBytes()} result in the
   * following way: Between each part of the Answer-To-Reset a space is included for better
   * readability. More specific: the Answer-To-Reset contains the following parts:
   *
   * <ol>
   *   <li>mandatory: initial character TS
   *   <li>mandatory: format byte T0
   *   <li>optional: zero, one or more cluster with TAi, TBi, TCi, TDi. Each cluster is considered
   *       as a separate part of the Answer-To-Reset.
   *   <li>optional: historical bytes
   *   <li>conditional: checksum byte TCK
   * </ol>
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder(1024);

    result.append(String.format("%02x ", getTs())).append(String.format("%02x", getT0()));

    if (0 != (getT0() & 0xf0)) {
      boolean flag = true; // enter the for-loop
      for (int i = 1; flag; i++) {
        result.append(' ');

        final int index = i;
        List.of("A", "B", "C", "D")
            .forEach(
                position -> {
                  if (isPresent(index, position)) {
                    result.append(String.format("%02x", getInterfaceByte(index, position)));
                  } // end fi
                }); // end forEach(position -> ...)

        // loop until either TDi is absent or TDi is present but high nibble
        // indicates no further interface bytes
        flag = isPresentTd(i) && (0 != (getTd(i) & 0xf0));
      } // end For (i...)
    } // end fi (Tx1 available)

    final byte[] hb = getHistoricalBytes().getHistoricalBytes();
    if (hb.length > 0) {
      result.append(' ').append(Hex.toHexDigits(hb));
    } // end fi

    if (-1 != getTck()) {
      result.append(String.format(" %02x", getTck()));
    } // end fi

    return result.toString();
  } // end method */

  /**
   * Explains which interface byte follow (if any).
   *
   * @param tdi interface byte TDi where the high-nibble indicates presence and absence of the
   *     following interface bytes
   * @param index of cluster to be explained
   * @return explanation which TXi follow
   */
  @VisibleForTesting // otherwise = private
  /* package */ static String appendY(final int tdi, final int index) {
    final int highNibble = tdi & 0xf0;
    final StringBuilder result = new StringBuilder(128);

    if (0 != (highNibble & 0x10)) {
      result.append(String.format("TA%d ", index));
    } // end fi

    if (0 != (highNibble & 0x20)) {
      result.append(String.format("TB%d ", index));
    } // end fi

    if (0 != (highNibble & 0x40)) {
      result.append(String.format("TC%d ", index));
    } // end fi

    if (0 != (highNibble & 0x80)) {
      result.append(String.format("TD%d ", index));
    } // end fi

    if (result.isEmpty()) {
      result.append("no interface bytes ");
    } // end fi

    result.append("follow, ");

    return result.toString();
  } // end method */
} // end class
