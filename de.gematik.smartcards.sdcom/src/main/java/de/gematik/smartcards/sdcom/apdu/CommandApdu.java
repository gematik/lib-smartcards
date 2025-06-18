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
package de.gematik.smartcards.sdcom.apdu;

import de.gematik.smartcards.sdcom.Message;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractBinary;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.io.Serial;
import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * A command APDU following the structure defined in ISO/IEC 7816-3:2006, clause 12.1.
 *
 * <p>Known subclasses: {@link AbstractAuthenticate Authenticate} {@link AbstractBinary Binary}.
 *
 * <p>The functionality provided here is a superset of {@code javax.smartcardio.CommandAPDU}. Thus,
 * there is no need to use the final class {@code javax.smartcardio.CommandAPDU}.
 *
 * <p>A {@link CommandApdu} consists of a four byte command header, an optional command data field
 * of variable length and an optional Le-field. This class does not verify if the APDU encodes a
 * semantically valid command.
 *
 * <p>If the expected length of the response APDU is specified in e.g. {@link
 * CommandApdu#CommandApdu(int, int, int, int, int)} constructors, the actual length (Ne)
 * <b>SHALL</b> be specified, not its encoded form (Le). Similarly, {@link #getNe()} returns the
 * actual value Ne. In other words, a value of 0 indicates "no data in the response APDU" rather
 * than "maximum length."
 *
 * <p>This class supports both the short and extended forms of length encoding for Ne and Nc.
 * However, note that not all terminals and Smart Cards are capable of accepting APDUs that use the
 * extended form.
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
 * <p>It follows that from the perspective of this class object sharing is possible without side
 * effects.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.GodClass", "PMD.TooManyMethods"})
public class CommandApdu implements Apdu, Command<CommandApdu>, Serializable {

  /** Automatically generated UID. */
  @Serial private static final long serialVersionUID = 326005894124392557L; // */

  /** Infimum of the number of octets in a command data field. */
  public static final int NC_INFIMUM = 1; // */

  /** Supremum of the number of octets in a command data field in case of short format. */
  public static final int NC_SUPREMUM_SHORT = 0xff; // */

  /** Supremum of the number of octets in a command data field. */
  public static final int NC_SUPREMUM = 0xffff; // */

  /** Infimum of the number of octets in an expected response data field. */
  public static final int NE_INFIMUM = 1; // */

  /**
   * Supremum, of the number of octets expected in response data field in case of short format.
   *
   * <p>Instead of 256 the value of this constant is intentionally set to 255.
   *
   * <p>An Le-field with value '00' (i.e., short format) indicates "up to 256" octet expected in
   * data field of the corresponding {@link ResponseApdu}. If less than those 256 octets are
   * available, then the trailer of the {@link ResponseApdu} typically contains '9000' = NoError.
   *
   * <p>An Le-field with value '0100' (i.e., extended format) indicates "exact 256" octet expected
   * in data field of the corresponding {@link ResponseApdu}. If less than those 256 octets are
   * available, then the trailer of the {@link ResponseApdu} typically contains '6282' =
   * EndOfFileWarning.
   *
   * <p>Thus, it makes a difference if the {@link CommandApdu} indicates "up to 256" octet on one
   * hand or "exact 256" octet on the other hand. Because of this difference the maximum (exact)
   * value a {@link CommandApdu} can encode in short format is 255.
   */
  public static final int NE_SUPREMUM_SHORT = 0xff; // */

  /**
   * Supremum of the number of octets in an expected response data field.
   *
   * <p>Instead of 65,536 = '1 0000' the value of this constant is intentionally set to 65,535 =
   * 'ffff'.
   *
   * <p>An Le-field with value '0000' (i.e., extended format) indicates "up to 65,536" octet
   * expected in data field of {@link ResponseApdu}. If less than those 65,536 octets are available,
   * then the trailer of the {@link ResponseApdu} typically contains '9000' = NoError.
   *
   * <p>An Le-field with value 'ffff' (i.e., extended format) indicates "exact 65,535" octet
   * expected in data field of {@link ResponseApdu}. If less than those 65,535 octets are available,
   * then the trailer of the {@link ResponseApdu} typically contains '6282' = EndOfFileWarning.
   *
   * <p>Thus, it makes a difference if the {@link CommandApdu} indicates "up to 65,536" octet on one
   * hand or "exact 65535" octet on the other hand. Because of this difference the maximum (exact)
   * value a {@link CommandApdu} can encode in extended format is 65,535 = 'ffff'.
   */
  public static final int NE_SUPREMUM = 0xffff; // */

  /**
   * Constant indicating up to 65,536 octets of data expected in a response APDU.
   *
   * <p><i><b>Note:</b> The value is chosen such that</i>
   *
   * <ol>
   *   <li><i>the last two octet are zero, it follows that an Le-field can be calculated via {@code
   *       getNe() & 0xffff}.</i>
   *   <li><i>the value is positive, it follows that {@link #getNe()} is never negative.</i>
   *   <li><i>the value is out of regular range for Ne, i.e. out of [1, 65535].</i>
   *   <li><i>the value is identical to the supremum of expected octet in a response APDU.</i>
   * </ol>
   */
  public static final int NE_EXTENDED_WILDCARD = 0x1_0000; // */

  /**
   * Constant indicating up to 256 octets of data expected in a response APDU.
   *
   * <p><i><b>Note:</b> The value is chosen such that</i>
   *
   * <ol>
   *   <li><i>the last two octet are zero, if follows that an Le-field can be calculated via {@code
   *       getNe() & 0xffff}.</i>
   *   <li><i>the value is positive, it follows that {@link #getNe()} is never negative.</i>
   *   <li><i>the value is out of regular range for Ne, i.e. out of [1, 65535].</i>
   * </ol>
   */
  public static final int NE_SHORT_WILDCARD = 0x2_0000;

  /** Constant indicating that Short File Identifier is absent in a command APDU. */
  public static final int SFI_ABSENT = 0; // */

  /** Infimum of valid Short File Identifier. */
  public static final int SFI_INFIMUM = 1; // */

  /** Supremum of Short File Identifier. */
  public static final int SFI_SUPREMUM = 30; // */

  /** CLA byte from 7816-3 APDU, range [0x00, 0xfe]. */
  private final int insCla; // */

  /** INS code from 7816-3 APDU, range [0x00, 0xff]. */
  private final int insIns; // */

  /** Parameter P1 from 7816-3 APDU, range [0x00, 0xff]. */
  private final int insP1; // */

  /** Parameter P2 from 7816-3 APDU, range [0x00, 0xff]. */
  private final int insP2; // */

  /**
   * Optional command data field from 7816-3 APDU.
   *
   * <p>Implementation assures that
   *
   * <pre>0 &le; m_Data.length &le; 65535</pre>
   */
  private final byte[] insData; // */

  /**
   * Number of expected response data bytes.
   *
   * <p>Implementation assures that Ne is from set {0, 1, 2, ..., 255, 256, 257, ..., 65535, {@link
   * #NE_SHORT_WILDCARD}, {@link #NE_EXTENDED_WILDCARD}}.
   */
  private final int insNe; // */

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
  protected volatile int insHashCode; // NOPMD volatile not recommended */

  /**
   * Constructs a {@link CommandApdu} from a byte array containing the complete APDU contents
   * (command header, optional data field and optional Le-field).
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param apdu the complete command APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA-byte has value 'ff'
   *       <li>{@code apdu} is invalid, e.g. Lc-field indicates {@code Nc=7} but command data field
   *           contains just one octet
   *     </ol>
   */
  public CommandApdu(final byte[] apdu) {
    this(ByteBuffer.wrap(apdu));
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from hexadecimal digits extracted from {@code apdu} containing
   * the complete APDU contents (command header, optional data field and optional Le-field).
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe, because input parameter is immutable.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable.</i>
   * </ol>
   *
   * @param apdu the complete command APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA-byte has value 'ff'
   *       <li>{@code apdu} is invalid, e.g. Lc-field indicates {@code Nc=7} but command data field
   *           contains just one octet
   *     </ol>
   */
  public CommandApdu(final String apdu) {
    this(Hex.toByteArray(apdu));
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from a byte array containing the complete APDU contents
   * (command header, optional data field and optional Le-field).
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param apdu the complete command APDU
   * @param offset offset to first byte of command APDU
   * @param length number of octets in commando APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA='ff'
   *       <li>{@code apdu} is invalid, e.g. Lc-field indicates {@code Nc=7} but command data field
   *           contains just one octet
   *     </ol>
   *
   * @throws IndexOutOfBoundsException if sub-range indicated by {@code offset} and {@code length}
   *     is out of bounds
   */
  public CommandApdu(final byte[] apdu, final int offset, final int length) {
    this(ByteBuffer.wrap(apdu).limit(offset + length).position(offset));

    // --- check parts of command APDU
    check(getCase());
  } // end constructor */

  /**
   * Creates a {@link CommandApdu} from the {@link ByteBuffer} containing the complete APDU contents
   * (command header, optional data field and optional Le-field).
   *
   * <p>The buffer's {@code position} <b>SHALL</b> be set to the start of the {@link CommandApdu},
   * its {@code limit} to the end of the {@link CommandApdu}. Upon return, the buffer's {@code
   * position} is equal to its limit. its limit remains unchanged.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the buffer while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param apdu the complete command APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA-byte has value 'ff'
   *       <li>{@code apdu} is invalid, e.g. Lc-field indicates {@code Nc=7} but command data field
   *           contains just one octet
   *     </ol>
   */
  public CommandApdu(final ByteBuffer apdu) {
    try {
      // --- extract command header
      insCla = apdu.get() & 0xff;
      insIns = apdu.get() & 0xff;
      insP1 = apdu.get() & 0xff;
      insP2 = apdu.get() & 0xff;

      // --- extract (optional) command data field and Ne
      final byte[][] result = zzzExtractDataNe(apdu);

      insData = result[0];
      insNe = ByteBuffer.wrap(result[1]).getInt();
    } catch (BufferUnderflowException e) {
      throw new IllegalArgumentException("APDU is too short", e);
    } // end Catch (...)

    if (apdu.hasRemaining()) {
      throw new IllegalArgumentException("extra octet at end of APDU");
    } // end fi

    // --- check parts of command APDU
    check(getCase());
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from the four header bytes.
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
   * @param p1 the parameter byte P1, only the eight least significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight least significant bit are taken into account
   * @throws IllegalArgumentException if CLA='ff'
   */
  public CommandApdu(final int cla, final int ins, final int p1, final int p2) {
    this(AfiUtils.EMPTY_OS, cla, ins, p1, p2, 0);

    // --- check parts of command APDU
    check(1);
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from the four header bytes and the expected response data
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
   * @param p1 the parameter byte P1, only the eight least significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight least significant bit are taken into account
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
  public CommandApdu(final int cla, final int ins, final int p1, final int p2, final int ne) {
    this(AfiUtils.EMPTY_OS, cla, ins, p1, p2, ne);

    // --- check parts of command APDU
    check(2);
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from the four header bytes and command data.
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
   * @param p1 the parameter byte P1, only the eight least significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight least significant bit are taken into account
   * @param data command data field
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA='ff'
   *       <li>1 > {@code data.length} > 65535
   *     </ol>
   */
  public CommandApdu(final int cla, final int ins, final int p1, final int p2, final byte[] data) {
    this(data.clone(), cla, ins, p1, p2, 0);

    // --- check parts of command APDU
    check(3);
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from the four header bytes and command data.
   *
   * <p>This is case 3 in ISO/IEC 7816-3, command data field present and Le-field absent.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe, because input parameter are immutable or primitive.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable or
   *       primitive.</i>
   * </ol>
   *
   * @param cla the class byte CLA, only the eight least significant bit are taken into account
   * @param ins the instruction byte INS, only the eight least significant bit are taken into
   *     account
   * @param p1 the parameter byte P1, only the eight least significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight least significant bit are taken into account
   * @param data command data field, only hex-digits are taken into account
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA='ff'
   *       <li>1 > {@code data.length} > 65535
   *     </ol>
   */
  public CommandApdu(final int cla, final int ins, final int p1, final int p2, final String data) {
    this(Hex.toByteArray(data), cla, ins, p1, p2, 0);

    // --- check parts of command APDU
    check(3);
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from the four header bytes and command data.
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
   * @param p1 the parameter byte P1, only the eight least significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight least significant bit are taken into account
   * @param data command data field
   * @param offset in the byte array at which the data bytes of the command body begin
   * @param length number of octets in the command body
   * @throws ArrayIndexOutOfBoundsException if
   *     <ol>
   *       <li>{@code 0 > offset > data.length}
   *       <li>{@code length < 0}
   *     </ol>
   *
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA='ff'
   *       <li>1 > {@code data.length} > 65535
   *       <li>{@code length = 0}
   *     </ol>
   *
   * @throws IndexOutOfBoundsException if sub-range indicated by {@code offset} and {@code length}
   *     is out of bounds
   */
  public CommandApdu(
      final int cla,
      final int ins,
      final int p1,
      final int p2,
      final byte[] data,
      final int offset,
      final int length) {
    this(Arrays.copyOfRange(data, offset, offset + length), cla, ins, p1, p2, 0);

    // --- check values of offset and length
    Objects.checkFromIndexSize(offset, length, data.length);

    check(3);
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from the four header bytes, the command data field and the
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
   * @param p1 the parameter byte P1, only the eight least significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight least significant bit are taken into account
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
  public CommandApdu(
      final int cla, final int ins, final int p1, final int p2, final byte[] data, final int ne) {
    this(data.clone(), cla, ins, p1, p2, ne);

    // --- check parts of command APDU
    check(4);
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from the four header bytes, the command data field and the
   * expected response data length.
   *
   * <p>This is case 4 in ISO/IEC 7816-3, command data field present and Le-field present.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe, because input parameter(s) are immutable or primitive</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable or
   *       primitive.</i>
   * </ol>
   *
   * @param cla the class byte CLA, only the eight least significant bit are taken into account
   * @param ins the instruction byte INS, only the eight least significant bit are taken into
   *     account
   * @param p1 the parameter byte P1, only the eight least significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight least significant bit are taken into account
   * @param data command data field
   * @param ne maximum number of expected data octet in a response APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA='ff'
   *       <li>{@code 1 > octet in command data field > 65535}
   *       <li>{@code Ne} not in set {1, 2, ..., 65535, {@link #NE_SHORT_WILDCARD}, {@link
   *           #NE_EXTENDED_WILDCARD}}.
   *     </ol>
   */
  public CommandApdu(
      final int cla, final int ins, final int p1, final int p2, final String data, final int ne) {
    this(Hex.toByteArray(data), cla, ins, p1, p2, ne);

    // --- check parts of command APDU
    check(4);
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from the four header bytes, the command data field and the
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
   * @param p1 the parameter byte P1, only the eight least significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight least significant bit are taken into account
   * @param data contains command data field
   * @param offset the offset in the byte array at which the data bytes of the command body begin
   * @param length the number of the data bytes in the command body
   * @param ne the maximum number of expected data bytes in a response APDU
   * @throws ArrayIndexOutOfBoundsException if {@code 0 > offset > data.length}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA-byte has value 'ff'
   *       <li>{@code 1 > octet in command data field > 65535}
   *       <li>{@code Ne} not in set {1, 2, ..., 65535, {@link #NE_SHORT_WILDCARD}, {@link
   *           #NE_EXTENDED_WILDCARD}}.
   *     </ol>
   *
   * @throws IndexOutOfBoundsException if sub-range indicated by {@code offset} and {@code length}
   *     is out of bounds
   */
  public CommandApdu(
      final int cla,
      final int ins,
      final int p1,
      final int p2,
      final byte[] data,
      final int offset,
      final int length,
      final int ne) {
    // Note 1: SonarQube claims the following major finding:
    //         "Constructor has 8 parameters, which is greater than 7 authorized."
    // Note 2: This will NOT be fixed.
    //         This constructor needs eight parameters.
    //         I will not combine parameters to just satisfy SonarQube.
    this(Arrays.copyOfRange(data, offset, offset + length), cla, ins, p1, p2, ne);

    // --- check values of offset and length
    Objects.checkFromIndexSize(offset, length, data.length);

    check(4);
  } // end constructor */

  /**
   * Constructs a {@link CommandApdu} from given parameters.
   *
   * <p>This constructor is able to create a command APDU of any ISO-case and is intended for
   * internal use by other constructors in this class giving specific values for instance
   * attributes.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because the byte-array for the command data field
   *       might change after this constructor is called.</i>
   *   <li><i>Object sharing is a problem here, because no defensive cloning is used for the command
   *       data field.</i>
   * </ol>
   *
   * @param cla the class byte CLA, only the eight least significant bit are taken into account
   * @param ins the instruction byte INS, only the eight least significant bit are taken into
   *     account
   * @param p1 the parameter byte P1, only the eight least significant bit are taken into account
   * @param p2 the parameter byte P2, only the eight least significant bit are taken into account
   * @param data command data field
   * @param ne maximum number of expected octets in the response data field, possibly zero
   *     indicating the absence of Le-field
   */
  /* package */
  @VisibleForTesting
  CommandApdu(
      final byte[] data, final int cla, final int ins, final int p1, final int p2, final int ne) {
    // Note 1: Because the visibility of this method is 'default' = package-private
    //         we do not check the input, but rely on assertions, which are tested
    //         during JUnit-tests.
    insCla = cla & 0xff;
    insIns = ins & 0xff;
    insP1 = p1 & 0xff;
    insP2 = p2 & 0xff;
    insData = data; // NOPMD array is stored directly
    insNe = ne;
  } // end constructor */

  /**
   * Checks this command APDU according to given ISO-case.
   *
   * @param isoCase gives the intended ISO-case, see ISO/IEC 7816-3:2006 clause 12.1.2
   *     <ol>
   *       <li>command data field absent, Le-field absent
   *       <li>command data field absent, Le-field present
   *       <li>command data field present, Le-field absent
   *       <li>command data field present, Le-field present
   *     </ol>
   *
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>CLA byte has value 'FF'
   *       <li>command data field is present for case 1 and case 2
   *       <li>command data field contains more than 65,535='ffff' octet for case 3 and case 4
   *       <li>{@code Ne} is not 0 for case 1 and case 3
   *       <li>{@code Ne} not from set {1, 2, ..., 65535, {@link #NE_SHORT_WILDCARD}, {@link
   *           #NE_EXTENDED_WILDCARD}} for case 2 and case 4
   *       <li>{@code isoCase} is not in range [1, 4]
   *     </ol>
   */
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
  /* package */ final void check(final int isoCase) {
    // --- check CLA
    // Note 1: CLA SHALL be in range [0, 0xfe], 0xff is invalid according to
    //         ISO/IEC 7816-4:2013 clause 5.4.1 §1 dash 2 (used for PPS)
    // Note 2: 0 <= CLA <= 255 = 'ff' is assured by constructors.
    if (0xff == insCla) { // NOPMD literal in if statement
      throw new IllegalArgumentException("invalid CLA byte: 'ff'");
    } // end fi

    // --- check INS
    // intentionally, no checks are performed here
    // Note: 0 <= INS <= 255 = 'ff' is assured by constructors.

    // --- check P1
    // intentionally, no checks are performed here
    // Note: 0 <= P1 <= 255 = 'ff' is assured by constructors.

    // --- check P2
    //  intentionally, no checks are performed here
    // Note: 0 <= P2 <= 255 = 'ff' is assured by constructors.

    switch (isoCase) {
      case 1 -> {
        checkCommandDataAbsent();
        checkLeFieldAbsent();
      }

      case 2 -> {
        checkCommandDataAbsent();
        checkLeFieldPresent();
      }

      case 3 -> {
        checkCommandDataPresent();
        checkLeFieldAbsent();
      }

      case 4 -> {
        checkCommandDataPresent();
        checkLeFieldPresent();
      }

      default -> throw new IllegalArgumentException("unimplemented case: " + isoCase);
    } // end Switch (isoCase)
  } // end method */

  private void checkCommandDataPresent() {
    if (insData.length < NC_INFIMUM) {
      throw new IllegalArgumentException("command data field absent, but SHALL be present");
    } // end fi
    if (insData.length > NC_SUPREMUM) {
      throw new IllegalArgumentException("command data field too long");
    } // end fi
  } // end method */

  private void checkCommandDataAbsent() {
    if (insData.length >= NC_INFIMUM) {
      throw new IllegalArgumentException("command data field present, but SHALL be absent");
    } // end fi
  } // end method */

  private void checkLeFieldAbsent() {
    if (insNe >= NE_INFIMUM) {
      throw new IllegalArgumentException("Le-field present, but SHALL be absent");
    } // end fi
  } // end method */

  private void checkLeFieldPresent() {
    if (0 == insNe) {
      throw new IllegalArgumentException("Le-field absent, but SHALL be present");
    } // end fi
    // ... Ne != 0

    if ((NE_SHORT_WILDCARD != insNe)
        && (NE_EXTENDED_WILDCARD != insNe)
        && ((NE_INFIMUM > insNe) || (insNe > NE_SUPREMUM))) {
      // ... Ne neither Wildcard nor in range [1, 65536]
      throw new IllegalArgumentException("invalid Ne: " + insNe);
    } // end fi
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * <p>Intentionally this method is not {@code final}, because it is possible (although not likely)
   * that subclasses needs to overwrite this method.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) of correct class are
   *       immutable and return value is primitive.</i>
   * </ol>
   *
   * @param obj object used for comparison, can be null
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
    // ... obj not same as this

    if (null == obj) {
      // ... this differs from null
      return false;
    } // end fi
    // ... obj not null

    // Note 2: Although this is possibly a superclass we check the classes of
    //         this and obj. Thus, this check isn't necessary in subclasses.
    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end fi
    // ... obj is instance of CommandApdu

    final CommandApdu other = (CommandApdu) obj;

    // --- compare instance attributes
    // ... assertion: instance attributes are never null
    // Note 3: Intentionally here we start with attributes which more likely differ.
    return (this.insIns == other.insIns)
        && (this.insP1 == other.insP1)
        && (this.insP2 == other.insP2)
        && (this.insNe == other.insNe)
        && (this.insCla == other.insCla)
        && Arrays.equals(this.insData, other.insData);
  } // end method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return hash-code of object
   * @see java.lang.Object#hashCode()
   */
  @Override
  public final int hashCode() {
    // Note 1: Because this class is a direct subclass of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because equals() takes into account CLA, INS, P1, P2, data and
    //         Le-field, here we can do the same.
    // Note 3: Instead of simple adding the hash-codes of instance attributes
    //         here, some calculation is done.
    // Note 4: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from the main memory into thread local memory
    if (0 == result) {
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion 1: CLA, INS, P1 and P2 are one byte octet
      // ... assertion 2: instance attributes are never null

      final int hashCodeMultiplier = 31; // small prime number

      // --- take into account primitive instance attributes
      // take into account command header
      result = (insCla << 24) | (insIns << 16) | (insP1 << 8) | insP2;

      // take into account Ne
      result = result * hashCodeMultiplier + insNe;

      // --- take into account reference types (currently only insData)
      // Note 5: Intentionally insData is not taken into account for
      //         performance reasons.

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Converts given trailer into an explanation {@link String}.
   *
   * <p>Although a trailer (SW1-SW2) is part of a {@link ResponseApdu}, intentionally this class
   * implements this method, because the meaning of a certain trailer-value possibly depends on the
   * type of command. For the same reason it is possible that subclasses override this method.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param trailer to be explained
   * @return explanation for certain trailers
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  public String explainTrailer(final int trailer) {
    return switch (trailer) {
      // --- ISO/IEC 7816-4 (non exhaustive list)
      case 0x6281 -> "CorruptDataWarning";
      case 0x6282 -> "EndOfFileWarning";
      case 0x6283 -> "FileDeactivated";
      case 0x6287 -> "RecordDeactivated";
      case 0x6581 -> "MemoryFailure";
      case 0x6981 -> "WrongFileType";
      case 0x6982 -> "SecurityStatusNotSatisfied";
      case 0x6986 -> "NoCurrentEf";
      case 0x6988 -> "IncorrectSmDo";
      case 0x6a82, 0x6a8a -> "FileNotFound";
      case 0x6a83 -> "RecordNotFound";
      case 0x6a87 -> "Nc-InconsistentWithParameters_P1-P2";
      case 0x6b00 -> "OffsetTooBig";
      case 0x6d00 -> "InstructionCodeNotSupported";
      case 0x6e00 -> "ClassNotSupported";
      case 0x9000 -> "NoError";

      default -> String.format("no explanation for '%04x' implemented", trailer);
    }; // end Switch
  } // end method */

  /**
   * Return octet string representation of {@link CommandApdu}.
   *
   * <p>If possible Lc- and Le-field are short (i.e. consists of one octet). Otherwise, Lc- and
   * Le-field are extended.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is never used again by this
   *       class.</i>
   * </ol>
   *
   * @return octet string representation of {@link CommandApdu} according to ISO/IEC 7816-3.
   * @see Message#getBytes()
   */
  @Override
  public final byte[] getBytes() {
    return isShort() ? getBytesShort() : getBytesExtended();
  } // end method */

  /**
   * Return octet string representation of {@link CommandApdu}.
   *
   * <p>This method always uses the extended format for Lc- and Le-fields (i.e. each Lc- and each
   * Le-field (if present) consists of two octets).
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is never used again by this
   *       class.</i>
   * </ol>
   *
   * @return octet string representation of {@link CommandApdu} according to ISO/IEC 7816-3:2006
   *     clause 12.1.3 extended Command-APDU
   */
  public final byte[] getBytesExtended() {
    final int nc = getNc();
    final int ne = getNe();
    final int iso = getCase();
    // ... assertion 1: 0 <= Nc <= 0xffff
    // ... assertion 2: 0 <= Ne <= 0xffff  OR  NeShortWildcard  OR  NeExtendedWildcard
    // ... assertion 3: NeShortWildCard    & 0xffff == 0
    // ... assertion 4: NeExtendedWildCard & 0xffff == 0
    // ... assertion 5: Ne == 0 indicates "Le-field absent"

    return AfiUtils.concatenate(
        // command header
        new byte[] {(byte) insCla, (byte) insIns, (byte) insP1, (byte) insP2},

        // if NOT case 1 extended-length indicator
        (1 == iso) ? AfiUtils.EMPTY_OS : new byte[1],

        // Lc-field
        (0 == nc) ? AfiUtils.EMPTY_OS : new byte[] {(byte) (nc >> 8), (byte) nc},

        // command data field
        insData,

        // Le-field
        (0 == ne) ? AfiUtils.EMPTY_OS : new byte[] {(byte) (ne >> 8), (byte) ne});
  } // end method */

  /**
   * Return octet string representation of {@link CommandApdu}.
   *
   * <p>This method always uses the short format for Lc- and Le-fields (i.e., each Lc- and each
   * Le-field (if present) consists of one octet).
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is never used again by this
   *       class.</i>
   * </ol>
   *
   * @return octet string representation of {@link CommandApdu} according to ISO/IEC 7816-3:2006
   *     clause 12.1.3 short Command-APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>command data field contains more than 255 octet
   *       <li>{@code Ne} indicates more than 256 expected octet
   *     </ol>
   */
  public final byte[] getBytesShort() {
    if (!isShort()) {
      throw new IllegalArgumentException(
          "according to ISO/IEC 7816-3:2006 clause 12.1.2 this='" + this + "' is not a short APDU");
    } // end fi

    final int nc = getNc();
    final int ne = getNe();
    // ... assertion 1: 0 <= Nc <= 0xff
    // ... assertion 2: 0 <= Ne <= 0xff  OR  NeWildCardShort

    return AfiUtils.concatenate(
        // command header
        new byte[] {(byte) insCla, (byte) insIns, (byte) insP1, (byte) insP2},

        // Lc-field
        (0 == nc) ? AfiUtils.EMPTY_OS : new byte[] {(byte) nc},

        // command data field
        insData,

        // Le-field
        (0 == ne) ? AfiUtils.EMPTY_OS : new byte[] {(byte) ne});
  } // end method */

  /**
   * Returns ISO-case of {@link CommandApdu}.
   *
   * <p>ISO-cases are defined in ISO/IEC 7186-3:2006, clause 12.1.2.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return ISO case of command APDU
   *     <ol>
   *       <li>1 if Lc- and Le-field are absent
   *       <li>2 if Lc-field is absent but Le-field is present
   *       <li>3 if Lc-field is present but Le-field is absent
   *       <li>4 if Lc- and Le-field are present
   *     </ol>
   */
  public final int getCase() {
    // ... assertion 1: Nc == 0 => Lc-field and command data field absent
    // ... assertion 2: Ne == 0 => Le-field absent
    // ... assertion 3: even if Le-field indicates NeWildcard (i.e. contains
    //                  only octet set to '00'), Ne != 0
    return ((getNc() == 0) ? 1 : 3) + ((getNe() == 0) ? 0 : 1);
  } // end method */

  /**
   * Returns number of the (logical) channel this command is associated with.
   *
   * <p>The number of the logical channel is in range [0, 19],
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return number of (logical) channel this command is associated with
   * @see Command#getChannelNumber()
   */
  @Override
  public final int getChannelNumber() {
    final int result;

    // see ISO/IEC 7816-4:2020 clause 5.4
    final int cla = getCla();

    if (0 == (cla & 0x40)) {
      // ... bit b7 = 0
      //     => first inter-industry values of CLA
      //     => channel number in bits b2 b1
      result = cla & 0x3;
    } else {
      // ... bit b7 = 1
      //     => further inter-industry values of CLA
      //     => channel number in lower nibble
      result = (cla & 0xf) + 4;
    } // end fi

    return result;
  } // end method */

  /**
   * Return CLA byte.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return CLA byte, a value from range [0, 254]
   */
  public final int getCla() {
    return insCla;
  } // end method */

  /**
   * Return content of command data field (possibly empty).
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because defensive cloning is used.</i>
   * </ol>
   *
   * @return command data field or an empty byte-array if command data field is absent
   */
  public final byte[] getData() {
    return insData.clone();
  } // end method */

  /**
   * Return INS byte.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return instance attribute INS code, a value from range [0, 255]
   */
  public final int getIns() {
    return insIns;
  } // end method */

  /**
   * Return supremum of response data field length.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return supremum of Nr indicated by this command APDU, i.e.
   *     <ol>
   *       <li>0 if Le-field is absent
   *       <li>256 if Ne is {@link #NE_SHORT_WILDCARD}
   *       <li>65536 if Ne is {@link #NE_EXTENDED_WILDCARD}
   *       <li>otherwise value of instance attribute Ne from range [1, 65536]
   *     </ol>
   */
  public final int getMaxNr() {
    // ... assertion 1: getNe() is never negative
    // ... assertion 2: NE_EXTENDED_WILDCARD == 65536
    final int nr = getNe();

    return (nr == NE_SHORT_WILDCARD) ? 0x100 : nr;
  } // end method */

  /**
   * Return number of octet in command data field.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return number of octets in command data field or zero if command data field is absent
   */
  public final int getNc() {
    return insData.length;
  } // end method */

  /**
   * Return number of octet expected in data field of corresponding {@link ResponseApdu}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return number of octet expected in data field of corresponding {@link ResponseApdu}d, i.e.
   *     <ol>
   *       <li>0 if Le-field is absent
   *       <li>{@link #NE_SHORT_WILDCARD} if up to 256 octet are expected
   *       <li>{@link #NE_EXTENDED_WILDCARD} if up to 65536 octet are expected
   *       <li>otherwise value of instance attribute Ne from range [1, 65536]
   *     </ol>
   */
  public final int getNe() {
    return insNe;
  } // end method */

  /**
   * Return parameter P1.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return parameter P1, a value from range [0, 255]
   */
  public final int getP1() {
    return insP1;
  } // end method */

  /**
   * Return parameter P2.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return parameter P2, a value from range [0, 255]
   */
  public final int getP2() {
    return insP2;
  } // end method */

  /**
   * Indicates if a command is protected by cryptographic means.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return {@code TRUE} if cryptographically protection of {@link CommandApdu} is indicated,
   *     {@code FALSE} otherwise
   * @see Command#isSecureMessagingIndicated()
   */
  @Override
  public final boolean isSecureMessagingIndicated() {
    // Note 1: References within this method are valid within ISO/IEC 7816-4:2020.
    final int cla = getCla();

    final boolean result;
    if (0 == (cla & 0x40)) {
      // ... bit b7 is not set, i.e., 0
      //     => first inter-industry values of CLA, see table 2
      result = (0 != (cla & 0xc)); // check if at least one of the bits b4, b3 is set
    } else {
      // ... bit b7 is set, i.e., 1
      //     => further inter-industry values of CLA, see table 3
      result = (0 != (cla & 0x20)); // check if bit b6 is set
    } // end fi

    return result;
  } // end method */

  /**
   * Estimates if a {@link CommandApdu} can be encoded in short format.
   *
   * <p>According to ISO/IEC 7816-3:2006 clause 12.1.3 a {@link CommandApdu} can be encoded in short
   * format (i.e. Lc-field and Le-field are encoded in one octet, if present), if the command data
   * field contains 255 octet or less and the Le-field encodes Ne with a value of 255 or the
   * Le-field encodes "up to 256" octet.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return true if and only if {@code (Nc <= 255) AND ((Ne < 256) OR (Ne == NeShortWildcard))}
   */
  public final boolean isShort() {
    // ... assertion Ne >= 0  OR  Ne from set "{ShortWildcard, ExtendedWildcard}"
    final int ne = getNe();
    final boolean ncShort = getNc() <= NC_SUPREMUM_SHORT;
    final boolean neShort = (ne <= NE_SUPREMUM_SHORT) || (ne == NE_SHORT_WILDCARD);

    return ncShort && neShort;
  } // end method */

  /**
   * Modifies command such that an association with a particular (logical) channel is removed.
   *
   * <p>In particular: The channel number in CLA-byte is set to the basic logical channel (i.e.,
   * zero), see {@link CommandApdu#setChannelNumber(int)}. As a side effect, indication of secure
   * messaging is also cleared. This side effect is intentional.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is never used again by this
   *       class.</i>
   * </ol>
   *
   * @return copy of {@code this} but associated with the basic logical channel and secure messaging
   *     not indicated
   * @see Command#removeChannelNumber()
   */
  @Override
  public CommandApdu removeChannelNumber() {
    final int actualChannelNumber = getChannelNumber();

    if ((0 == actualChannelNumber) && !isSecureMessagingIndicated()) {
      // ... already basic logical channel  AND  no secure messaging
      //     => no need to change anything
      return this;
    } // end fi
    // ... either not basic logical channel
    // ...  or secure messaging indicated

    final int claOld = getCla();
    final int claNew;
    if (actualChannelNumber < 4) { // NOPMD literal in if statement
      // ... first inter-industry values for CLA, see ISO/IEC 7816-4:2020 table 2

      // a. leave the following bits in the CLA byte as they are:
      //    - bit b8: distinguishing between ISO and proprietary behavior
      //    - bit b7: is always zero here
      //    - bit b6: better leave that bit alone
      //    - bit b5: command chaining
      // b. clear bits b4 b3, possibly indicating secure messaging
      // c. clear bits b2 b1, possibly indicating a non-basic logical channel
      claNew = claOld & 0xf0;
    } else {
      // ... second inter-industry values for CLA, see ISO/IEC 7816-4:2020 table 3

      // a. leave the following bits in the CLA byte as they are:
      //    - bit b8: distinguishing between ISO and proprietary behavior
      //    - bit b5: command chaining
      // b. clear bit b7 which previously indicated "further interindustry values"
      // c. clear bit b6 which previously indicated secure messaging (optionally)
      // d. clear bits b4 b3 b2 b1 indicating a non-basic logical channel
      claNew = claOld & 0x90;
    } // end else

    return new CommandApdu(insData, claNew, getIns(), getP1(), getP2(), getNe());
  } // end method */

  /**
   * Modifies command such that it is associated with the given channel.
   *
   * <p>Typically when secure messaging is active the command header is protected by a MAC. Thus, if
   * secure messaging is indicated in the CLA byte the command APDU is <b>NOT</b> changed by this
   * method.
   *
   * <p>Furthermore, the command APDU is <b>NOT</b> changed if the channel number in the CLA byte
   * differs from zero, i.e., someone already set the channel number.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Because indication of secure messaging differs between first and second inter-industry
   *       values for CLA it follows that the channel number has to be set before secure messaging
   *       is applied to avoid ambiguity.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is never used again by this
   *       class.</i>
   * </ol>
   *
   * @param channelNumber number of (logical) channel this command is associated with
   * @return copy of {@code this} but associated with the logical channel from parameter {@code
   *     channelNumber} and secure messaging not indicated
   * @throws IllegalArgumentException if {@code channelNumber} is not in range [0, 19]
   * @see Command#setChannelNumber(int)
   */
  @Override
  public CommandApdu setChannelNumber(final int channelNumber) {
    final int actualChannelNumber = getChannelNumber();
    if ((0 == channelNumber) || (0 != actualChannelNumber) || isSecureMessagingIndicated()) {
      // ... basicChannel_desired  OR  channelNumber already appropriate  OR SecureMessaging
      //     => no need to change anything
      return this;
    } // end fi
    // ... channelNumber != 0
    // ... actualChannelNumber == 0 => bits b7 b2 b1 = 0
    // ... no secure messaging indicated => bits b4 b3 are clear

    // --- check input parameter channelNumber
    if ((0 > channelNumber) || (channelNumber > 19)) {
      throw new IllegalArgumentException("out of range [0, 19]");
    } // end fi
    // ... channel number is valid

    // --- calculate new value for CLA
    final int claOld = getCla();
    final int claNew;
    if (channelNumber < 4) { // NOPMD literal in if statement
      // ... first inter-industry values for CLA, see ISO/IEC 7816-4:2020 table 2

      // a. leave the following bits in the CLA byte as they are:
      //    - bit b8: distinguishing between ISO and proprietary behavior
      //    - bit b7: is always zero here
      //    - bit b6: better leave that bit alone
      //    - bit b5: command chaining
      //    - bits b4 b3: secure messaging indication are already 0
      // b. set bits b2 b1 according to given channelNumber
      claNew = claOld + channelNumber;
    } else {
      // ... second inter-industry values for CLA, see ISO/IEC 7816-4:2020 table 3

      // a. leave the following bits in the CLA byte as they are:
      //    - bit b8: distinguishing between ISO and proprietary behavior
      //    - bit b5: command chaining
      // b. set bit b7 => second inter-industry values for CLA
      // c. clear bit b6 => no secure messaging indication
      // d. set bits b4 b3 b2 b1 according to given channelNumber
      claNew = (claOld & 0x90) + 0x40 + channelNumber - 4;
    } // end fi

    return new CommandApdu(insData, claNew, getIns(), getP1(), getP2(), getNe());
  } // end method */

  /**
   * Return {@link String} representation.
   *
   * <p>The return value consists of the following parts:
   *
   * <ol>
   *   <li>command header: CLA='xx' INS='xx' P1='xx' P2='xx'
   *   <li>command data field: either "Lc and Data absent", or "Lc='xx' Data='xx...yy'"
   *   <li>Le-field: either "Le absent" or "Le='xx'"
   * </ol>
   *
   * <p>Lc- and Le-field are encoded in one octet in case of short format, otherwise they are
   * encoded in two octet.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return {@link String} representation
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    // Note: Intentionally hereafter the getter getData() is NOT used,
    //       because that method clones the command data field, which
    //       is not necessary here.
    return toString(Hex.toHexDigits(insData));
  } // end method */

  /**
   * Return {@link String} representation.
   *
   * <p>The return value consists of the following parts:
   *
   * <ol>
   *   <li>command header: CLA='xx' INS='xx' P1='xx' P2='xx'
   *   <li>command data field: either "Lc and Data absent", or "Lc='xx' Data='xx...yy'"
   *   <li>Instead of the real octet string from the command data field the given {@code
   *       cmdDataField} is included in the return value. This way subclasses can blind sensitive
   *       information like PIN or password.
   *   <li>Le-field: either "Le absent" or "Le='xx'"
   * </ol>
   *
   * <p>Lc- and Le-field are encoded in one octet in case of short format, otherwise they are
   * encoded in two octets.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @param cmdDataField {@link String} representation of command data field
   * @return {@link String} representation
   */
  protected String toString(final String cmdDataField) {
    final int nc = getNc();
    final int ne = getNe();
    final int isoCase = getCase();
    final String format = isShort() ? "'%02x'" : "'%04x'";

    return String.format(
        "CLA='%02x'  INS='%02x'  P1='%02x'  P2='%02x'%s%s",
        insCla,
        insIns,
        insP1,
        insP2, // command header
        (isoCase < 3) // Lc-field and command data field
            ? "  Lc and Data absent" // ISO-case 1, 2
            : String.format(String.format("  Lc=%s  Data='%%s'", format), nc, cmdDataField),
        (1 == (isoCase & 1)) // Le-field
            ? "  Le absent" // ISO-case 1, 3
            : String.format(String.format("  Le=%s", format), ne & 0xffff));
  } // end method */

  private static byte[][] zzzExtractDataNe(final ByteBuffer apdu) {
    if (0 == apdu.remaining()) {
      // ... ISO-case 1
      return case1();
    } else {
      // ... APDU with at least 5 octets
      final int c5 = apdu.get() & 0xff;

      if (0 == apdu.remaining()) {
        // ... APDU with 5 octets
        //     => ISO-case 2S
        return case2S(c5);
      } else {
        // ... APDU with more than 5 octets
        if (0 == c5) {
          // ... expanded format
          return expandedApdu(apdu);
        } else {
          // ... ISO-case 3S or ISO-case 4S
          return shortApdu(apdu, c5);
        } // end else
      } // end else
    } // end else
  } // end method */

  /** ISO-case 1, extract command data field and Ne. */
  private static byte[][] case1() {
    return new byte[][] {AfiUtils.EMPTY_OS, new byte[4]};
  } // end method */

  /** ISO-case 2S, extract command data field and Ne. */
  private static byte[][] case2S(final int c5) {
    final var neOctet = new byte[4];
    final var neBuffer = ByteBuffer.wrap(neOctet);

    neBuffer.putInt((0 == c5) ? NE_SHORT_WILDCARD : c5);

    return new byte[][] {AfiUtils.EMPTY_OS, neOctet};
  } // end method */

  /** Expanded APDU format. */
  private static byte[][] expandedApdu(final ByteBuffer apdu) {
    final byte[][] result = new byte[2][];
    result[1] = new byte[4];
    final var ne = ByteBuffer.wrap(result[1]);
    final int c6c7 = apdu.getShort() & 0xffff;

    if (0 == apdu.remaining()) {
      // ... ISO-case 2E
      result[0] = AfiUtils.EMPTY_OS;
      ne.putInt((0 == c6c7) ? NE_EXTENDED_WILDCARD : c6c7);
    } else {
      // ... ISO-case 3E or ISO-case 4e
      result[0] = new byte[c6c7];
      apdu.get(result[0]);

      if (apdu.hasRemaining()) {
        // ... ISO-case 4E
        final var le = apdu.getShort() & 0xffff;
        ne.putInt((0 == le) ? NE_EXTENDED_WILDCARD : le);
      } else {
        // ... ISO-case 3S
        ne.putInt(0);
      } // end else
    } // end else

    return result;
  } // end method */

  /** Short APDU format for ISO-case 3 or ISO-case 4. */
  private static byte[][] shortApdu(final ByteBuffer apdu, final int c5) {
    final byte[][] result = new byte[2][];
    result[1] = new byte[4];
    final var ne = ByteBuffer.wrap(result[1]);

    result[0] = new byte[c5];
    apdu.get(result[0]);

    if (apdu.hasRemaining()) {
      // ... ISO-case 4S
      final var le = apdu.get() & 0xff;
      ne.putInt((0 == le) ? NE_SHORT_WILDCARD : le);
    } else {
      // ... ISO-case 3S
      ne.putInt(0);
    } // end else

    return result;
  } // end method */

  /**
   * Empty finalizer method prevents finalizer attacks.
   *
   * <p>For more information, see <a
   * href="https://wiki.sei.cmu.edu/confluence/display/java/OBJ11-J.+Be+wary+of+letting+constructors+throw+exceptions">SEI
   * Cert Rule Obj-11</a>.
   *
   * @deprecated in {@link java.lang.Object}
   */
  @Deprecated(forRemoval = true)
  @Override
  @SuppressWarnings({"PMD.EmptyFinalizer", "checkstyle:nofinalizer"})
  protected final void finalize() {
    // Note 1: SonarQube claims the following critical code smell on this method:
    //         "Do not override the Object.finalize() method."
    // Note 2: As stated in the Java-Doc comment to this method is overwritten
    //         to prevent finalizer attacks.
    // intentionally empty
  } // end method */
} // end class
