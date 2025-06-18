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
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.utils.AfiRng;
import java.io.Serial;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class provides a challenge for the input/output of command-response pairs.
 *
 * <p>The purpose of this command is to produce a predictable response APDU in combination with a
 * predictable runtime. More specific:
 *
 * <ol>
 *   <li>This command supports all four ISO/IEC 7816 cases.
 *   <li>This command supports arbitrary sizes for command- and response-data-field.
 *   <li>Parameters P1 and P2 indicate the runtime, i.e. the time-period between receiving this
 *       command APDU and sending the corresponding response APDU. For further information, see
 *       {@link #getDelay()}.
 *   <li>The response APDU is in accordance to {@link #response()}.
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class IoChallenge extends CommandApdu {

  /** Automatically generated on 2022-08-09. */
  @Serial private static final long serialVersionUID = 4370584307501159546L; // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /**
   * Constructs an {@link IoChallenge} from the four header bytes.
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
   * @param cla the class byte CLA, only the eight least significant bit are taken into account
   * @param ins the instruction byte INS, only the eight least significant bit are taken into
   *     account
   * @param p1p2 indicator for time-period between receiving this command APDU before a
   *     corresponding response APDU is sent, bits b16..b9 are used for P1 and bits b8..b1 are used
   *     for P2
   * @throws IllegalArgumentException if CLA='ff'
   */
  public IoChallenge(final int cla, final int ins, final int p1p2) {
    super(cla, ins, p1p2 >> 8, p1p2);
  } // end constructor */

  /**
   * Constructs a {@link IoChallenge} from the four header bytes and the expected response data
   * length.
   *
   * <p>This is case 2 in ISO/IEC 7816-3, command data field absent and Le-field present.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe, because input parameter(s) are primitive.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive.</i>
   * </ol>
   *
   * @param cla the class byte CLA, only the eight least significant bit are taken into account
   * @param ins the instruction byte INS, only the eight least significant bit are taken into
   *     account
   * @param p1p2 indicator for time-period between receiving this command APDU before a
   *     corresponding response APDU is sent, bits b16..b9 are used for P1 and bits b8..b1 are used
   *     for P2
   * @param ne the maximum number of expected data bytes in a response APDU, <b>SHALL</b> be
   *     <ul>
   *       <li>in range [{@link #NE_INFIMUM}, ..., {@link #NE_SUPREMUM}], or
   *       <li>{@link #NE_SHORT_WILDCARD}, or
   *       <li>{@link #NE_EXTENDED_WILDCARD}
   *     </ul>
   *
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA='ff'
   *       <li>{@code Ne} invalid
   *     </ol>
   */
  public IoChallenge(final int cla, final int ins, final int p1p2, final int ne) {
    super(cla, ins, p1p2 >> 8, p1p2, ne);
  } // end constructor */

  /**
   * Constructs an {@link IoChallenge} from the four header bytes and command data.
   *
   * <p>This is case 3 in ISO/IEC 7816-3, command data field present and Le-field absent.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because the byte-array for the command data field
   *       might change after this constructor is called.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
   *       defensive cloning is used.</i>
   * </ol>
   *
   * @param cla the class byte CLA, only the eight least significant bit are taken into account
   * @param ins the instruction byte INS, only the eight least significant bit are taken into
   *     account
   * @param p1p2 indicator for time-period between receiving this command APDU before a
   *     corresponding response APDU is sent, bits b16..b9 are used for P1 and bits b8..b1 are used
   *     for P2
   * @param data command data field
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA='ff'
   *       <li>1 > {@code data.length} > 65535
   *     </ol>
   */
  public IoChallenge(final int cla, final int ins, final int p1p2, final byte[] data) {
    super(cla, ins, p1p2 >> 8, p1p2, data);
  } // end constructor */

  /**
   * Constructs a {@link IoChallenge} from the four header bytes, the command data field and the
   * expected response data length.
   *
   * <p>This is case 4 in ISO/IEC 7816-3, command data field present and Le-field present.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because the byte-array for the command data field
   *       might change after this constructor is called.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
   *       defensive cloning is used.</i>
   * </ol>
   *
   * @param cla the class byte CLA, only the eight least significant bit are taken into account
   * @param ins the instruction byte INS, only the eight least significant bit are taken into
   *     account
   * @param p1p2 indicator for time-period between receiving this command APDU before a
   *     corresponding response APDU is sent, bits b16..b9 are used for P1 and bits b8..b1 are used
   *     for P2
   * @param data command data field
   * @param ne the maximum number of expected data bytes in a response APDU, <b>SHALL</b> be
   *     <ul>
   *       <li>in range [{@link #NE_INFIMUM}, ..., {@link #NE_SUPREMUM}], or
   *       <li>{@link #NE_SHORT_WILDCARD}, or
   *       <li>{@link #NE_EXTENDED_WILDCARD}
   *     </ul>
   *
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA='ff'
   *       <li>1 > {@code data.length} > 65535
   *       <li>{@code Ne} is invalid
   *     </ol>
   */
  public IoChallenge(
      final int cla, final int ins, final int p1p2, final byte[] data, final int ne) {
    super(cla, ins, p1p2 >> 8, p1p2, data, ne);
  } // end constructor */

  /**
   * Pseudo constructor.
   *
   * @param cmd from which an instance is created
   * @return corresponding {@link IoChallenge}
   */
  @SuppressWarnings({"PMD.SingletonClassReturningNewInstance"})
  public static IoChallenge getInstance(final CommandApdu cmd) {
    final int cla = cmd.getCla();
    final int ins = cmd.getIns();
    final int p1p2 = (cmd.getP1() << 8) + cmd.getP2();
    final int ne = cmd.getNe();

    return switch (cmd.getCase()) {
      case 1 -> new IoChallenge(cla, ins, p1p2);
      case 2 -> new IoChallenge(cla, ins, p1p2, ne);
      case 3 -> new IoChallenge(cla, ins, p1p2, cmd.getData());
      default -> new IoChallenge(cla, ins, p1p2, cmd.getData(), ne);
    };
  } // end method */

  /**
   * Constructs an {@link IoChallenge} randomly.
   *
   * <p>The ISO case is randomly chosen, in particular:
   *
   * <ol>
   *   <li>if {@code maxNc = 0} and {@code maxNe = 0} then only ISO case 1 occurs.
   *   <li>if {@code maxNc = 0} and {@code maxNe > 0} then ISO cases 1 and 2 occur.
   *   <li>if {@code maxNc > 0} and {@code maxNe = 0} then ISO cases 1 and 3 occur.
   *   <li>if {@code maxNc > 0} and {@code maxNe > 0} then ISO cases 1, 2, 3 and 4 occur.
   * </ol>
   *
   * <p>The runtime is randomly chosen from range {@code [minDelay, maxDelay]}.
   *
   * @param minDelay minimum runtime / [ns]
   * @param maxDelay maximum runtime / [ns]
   * @param maxNc for case 3 and 4 APDU Nc is randomly chosen from range [1, maxNc]
   * @param maxNe for case 2 and 4 APDU Ne is randomly chosen from range [1, maxNe]
   * @return randomly constructed {@link IoChallenge}
   * @throws IllegalArgumentException if {@code minDelay} &gt; {@code maxDelay}
   */
  public static IoChallenge random(
      final long minDelay, final long maxDelay, final int maxNc, final int maxNe) {
    if (minDelay > maxDelay) {
      throw new IllegalArgumentException("minDelay > maxDelay");
    } // end fi

    // --- estimate all combinations of P1, P2 which lead to a delay in range [minDelay, maxDelay]
    final int[] possibleValues =
        IntStream.rangeClosed(0, 0xffff)
            .filter(
                p1p2 -> {
                  final long delay = calculateDelay(p1p2 >> 8, p1p2 & 0xff);

                  return (minDelay <= delay) && (delay <= maxDelay);
                })
            .toArray();
    final int p1p2 =
        (possibleValues.length == 0)
            ? calculateP1P2(maxDelay) & 0xffff // no valid P1P2 combination => assume maxDelay
            : possibleValues[RNG.nextIntClosed(0, possibleValues.length - 1)];
    final int cla = RNG.nextIntClosed(0x00, 0xfe);
    final int ins = RNG.nextIntClosed(0x00, 0xff);
    final int isoCase =
        1 + (RNG.nextIntClosed(0, 3) & ((maxNc == 0) ? 1 : 3) & ((maxNe == 0) ? 2 : 3));

    return switch (isoCase) {
      case 1 -> new IoChallenge(cla, ins, p1p2);

      case 2 -> new IoChallenge(cla, ins, p1p2, RNG.nextIntClosed(NE_INFIMUM, maxNe));

      case 3 ->
          new IoChallenge(cla, ins, p1p2, RNG.nextBytes(RNG.nextIntClosed(NC_INFIMUM, maxNc)));

      default ->
          new IoChallenge(
              cla,
              ins,
              p1p2,
              RNG.nextBytes(RNG.nextIntClosed(NC_INFIMUM, maxNc)),
              RNG.nextIntClosed(NE_INFIMUM, maxNe));
    };
  } // end method */

  /**
   * Return execution time in nanoseconds.
   *
   * <p>The execution time is indicated by parameters P1 and P2 as follows:
   *
   * <ol>
   *   <li>Here, parameters P1 and P2 are seen as a bit string with parameter P1 holding the most
   *       significant bits and parameter P2 holding the least significant bits.
   *   <li>The eleven most significant bit are seen as a kind of a mantissa.
   *   <li>The five least significant bit are seen as a kind of exponent.
   *   <li>The conversion into nanoseconds is performed according to the following algorithm:
   *       <pre>
   *         final int p1p2 = (getP1() &lt;&lt; 8) + getP2();
   *         final long mantissa = (p1p2 &gt;&gt; 5) &amp; 0x7ff;
   *         final int exponent  = p1p2 &amp; 0x1f;
   *         final long delay    = (0 == exponent)
   *             ? mantissa
   *             : (2048 | mantissa) &lt;&lt; (exponent - 1));
   *       </pre>
   *   <li>The relation between P1 || P2 and the result of this function is bijective. This way the
   *       execution time can be specified with high precision.
   *   <li>Examples:
   *       <pre>
   *         P1 || P2 = '0000' =>    0 ns
   *         P1 || P2 = '0020' =>    1 ns
   *         P1 || P2 = 'ffe0' => 2047 ns
   *         P1 || P2 = '0001' => 2048 ns
   *         P1 || P2 = '0021' => 2049 ns
   *         P1 || P2 = 'ffe1' => 4095 ns
   *         P1 || P2 = '001f' => 36' 39.023"
   *         P1 || P2 = '003f' => 36' 40.097"
   *         P1 || P2 = 'ffdf' => 1h 13' 15.899"
   *         P1 || P2 = 'ffff' => 1h 13' 16.973"
   *       </pre>
   * </ol>
   *
   * @return execution time in nanoseconds
   */
  public long getDelay() {
    return calculateDelay(getP1(), getP2());
  } // end method */

  /**
   * Calculates delay based on parameters P1 and P2.
   *
   * <p>This is the inverse function to {@link #calculateP1P2(long)}.
   *
   * @param p1 parameter P1
   * @param p2 parameter P2
   * @return execution time in nanoseconds
   */
  @VisibleForTesting // otherwise = private
  /* package */ static long calculateDelay(final int p1, final int p2) {
    // Note: This implementation uses 11 bit for the mantissa and 5 for the exponent.
    final long mantissa = ((long) p1 << 3) + (p2 >> 5);
    final int exponent = p2 & 0x1f;

    return (0 == exponent) ? mantissa : (2048 | mantissa) << (exponent - 1);
  } // end method */

  /**
   * Calculates the "mantissa || exponent" representation for given delay.
   *
   * @param delay in nanoseconds
   * @return "mantissa || exponent" representation
   */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  public static short calculateP1P2(final long delay) {
    long mantissa;
    int exponent;
    if (delay < 0) {
      return 0;
    } else if (delay >= 4_396_972_769_280L) { // NOPMD literal in if statement
      // ... value too big
      //     => return maximum
      return -1;
    } else if (delay < 2048) { // NOPMD literal in if statement
      // ... value does not need manipulation
      mantissa = delay;
      exponent = 0;
    } else {
      // ... value manageable
      //     => calculate "mantissa || exponent" representation
      mantissa = delay;
      exponent = 1;

      while (mantissa >= 0x2000) {
        mantissa >>= 1;
        exponent++;
      } // end While (...)

      if (mantissa >= 0x1000) { // NOPMD literal in if statement
        if (exponent > 1) { // NOPMD literal in if statement
          // ... rounding
          mantissa += 1;
        } // end fi
        mantissa >>= 1;
        exponent++;
      } // end fi

      mantissa -= 2048;
    } // end else

    return (short) ((mantissa << 5) + exponent);
  } // end method */

  /**
   * Calculates corresponding {@link ResponseApdu}.
   *
   * <p>The corresponding response APDU is calculated as follows:
   *
   * <ol>
   *   <li>The response-data-field contains as many bytes as indicated by the Le-field.
   *   <li>CLA-byte and INS-byte are seen as a {@link Short} with CLA-byte as the most-significant
   *       byte and INS-byte as the least-significant-byte.
   *   <li>Parameters P1 and P2 are seen as a {@link Short} with parameter P1 as the
   *       most-significant byte and parameter P2 as the least-significant-byte.
   *   <li>The first two octet of the response APDU are the big-endian {@link Short}-sum of the two
   *       {@link Short} values represented by {@code CLA || INS} on one hand and {@code P1 || P2}
   *       on the other hand plus the sum of all octet in the command-data-field.
   *   <li>The next two octet of the response APDU (if present) are a big-endian {@link Short}
   *       resulting from incrementing the previous two octet (also seen as a big-endian {@link
   *       Short}).
   *   <li>The penultimate octet (i.e. SW1) is set to value 144 = 0x90 = '90'.
   * </ol>
   *
   * <p>Examples:
   *
   * <pre>
   *   Case 1 command APDU: '00 01 0102'            => '9003'
   *   Case 2 command APDU: '80 02 0304 05'         => '8306830783 9083'
   *   Case 3 command APDU: '80 03 0410 03 112233'  => '9079'
   *   Case 4 command APDU: '80 04 0510 02 4455 06' => '85ad85ae85af 90b0'
   * </pre>
   *
   * @return corresponding {@link ResponseApdu}
   */
  public ResponseApdu response() {
    short n = (short) (((getCla() + getP1()) << 8) + getIns() + getP2());

    for (final byte octet : getData()) {
      n += (short) (octet & 0xff);
    } // end For (octets in command data field)

    final int nr = getMaxNr(); // Nr calculated from Le-field
    final int noOctet = nr + 2; // response-data-field plus trailer
    final int noShort = (noOctet + 1) >> 1; // rounding up to number of short values
    final ByteBuffer buffer = ByteBuffer.allocate(noShort << 1);

    while (buffer.remaining() >= 2) {
      buffer.putShort(n++);
    } // end While (...)
    final byte[] octets = buffer.array();
    octets[nr] = (byte) 0x90; // set SW1 to ‘90’

    return new ResponseApdu(octets, 0, noOctet); // array, offset, length
  } // end method */
} // end class
