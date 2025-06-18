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
package de.gematik.smartcards.g2icc.cos;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.AfiUtils;
import java.util.Optional;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Secure messaging transformer.
 *
 * <p>Directly known subclasses: {@link SecureMessagingConverterSoftware}, {@link
 * SecureMessagingLayer}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.TooManyMethods"})
public abstract class SecureMessagingConverter {

  /** Logger. */
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SecureMessagingConverter.class); // */

  /** Padding indicator. */
  @VisibleForTesting // otherwise = private
  /* package */ static final byte[] PADDING_INDICATOR = new byte[] {1}; // */

  /**
   * Controls whether command data is enciphered or not.
   *
   * <p>If set to {@code TRUE}, then the command data field of the following command APDU(s) (if
   * present) will be enciphered.
   *
   * <p>If set to {@code FALSE}, then the command data field of the following command APDU(s) (if
   * present) will be sent in plain-text.
   */
  private boolean insSmCmdEnc = true; // */

  /**
   * Shows whether response data were enciphered or not.
   *
   * <p>{@code TRUE} indicates that the response data field of the last received response APDU (if
   * present) was enciphered.
   *
   * <p>{@code FALSE} indicates that the response data field of the last received response APDU (if
   * present) was sent as plain-text.
   */
  private boolean insSmRspEnc; // */

  /** Default constructor. */
  /* package */ SecureMessagingConverter() {
    // intentionally empty
  } // end constructor */

  /**
   * Secures a command APDU according to gemSpec_COS#13.2.
   *
   * <p><i><b>Note:</b> Use {@link #setSmCmdEnc(boolean)} to control whether command data is
   * enciphered or not.</i>
   *
   * @param cmdApdu APDU to be secured by secure messaging
   * @return secured command APDU
   */
  public final CommandApdu secureCommand(final CommandApdu cmdApdu) {
    LOGGER.atDebug().log("unprotected APDU: {}", cmdApdu);

    final byte[] plainCmdData = cmdApdu.getData();

    // --- protectedData
    final byte[] protectedData;
    if (0 == plainCmdData.length) {
      // ... cmdData absent, see gemSpec_COS#(N032.000) => protectedDate empty
      protectedData = new byte[0];
    } else if (isSmCmdEnc()) {
      // ... cmdData present and to be enciphered
      // Note: Padding indicator is included in cipher
      final byte[] cipher = encipher(plainCmdData);
      // see gemSpec_COS#(N032.200)d, (N032.300)d
      protectedData = BerTlv.getInstance(0x87, cipher).getEncoded();
    } else {
      // ... cmdData present and to be sent as plain text
      // see gemSpec_COS#(N032.100)d
      protectedData = BerTlv.getInstance(0x81, plainCmdData).getEncoded();
    } // end fi

    // --- LeDO, see gemSpec_COS#(N032.400)
    final int ne = cmdApdu.getNe();
    final int newLe = (0 == ne) ? CommandApdu.NE_SHORT_WILDCARD : CommandApdu.NE_EXTENDED_WILDCARD;
    final byte[] leDo = zzzCreateLeDo(ne);

    // --- head, see gemSpec_COS#(032.600)
    final boolean isChannelNumber4 = (cmdApdu.getChannelNumber() < 4);
    final byte[] head;
    final byte claDash;
    final byte ins = (byte) cmdApdu.getIns();
    final byte p1 = (byte) cmdApdu.getP1();
    final byte p2 = (byte) cmdApdu.getP2();
    if (isChannelNumber4) {
      // ... channel number in range [0..3], see gemSpec_COS#(N032.600)a
      claDash = (byte) (cmdApdu.getCla() | 0x0c); // see gemSpec_COS#(N032.500)a
      head =
          new byte[] {
            claDash, ins, p1, p2,
          };
    } else {
      // ... channel number beyond 3, see gemSpec_COS#(N032.600)b
      claDash = (byte) (cmdApdu.getCla() | 0x20); // see gemSpec_COS#(N032.500)b
      head = new byte[] {(byte) 0x89, (byte) 0x04, claDash, ins, p1, p2};
    } // end fi

    // --- tmpData
    // see gemSpec_COS#(N032.800)
    final byte[] tmpData = AfiUtils.concatenate(protectedData, leDo);

    // --- MACin, attention: SSCmac will be taken into account by subclasses
    final byte[] macIn;
    if (!isChannelNumber4 || (0 == tmpData.length)) {
      // ... see gemSpec_COS#(N032.800)[a,b].2.i
      macIn = AfiUtils.concatenate(head, tmpData);
    } else {
      // ... see gemSpec_COS#(N032.800)[a,b].2.ii
      macIn = AfiUtils.concatenate(padIso(head), tmpData);
    } // end fi
    final byte[] mac = computeCryptographicChecksum(macIn);
    final byte[] macDo = BerTlv.getInstance(0x8e, mac).getEncoded();
    final byte[] newD = AfiUtils.concatenate(isChannelNumber4 ? new byte[0] : head, tmpData, macDo);

    final var result = new CommandApdu(claDash, ins, p1, p2, newD, newLe);
    LOGGER.atTrace().log("  protected APDU: {}", result);

    return result;
  } // end method */

  private static byte[] zzzCreateLeDo(final int ne) {
    final byte[] leDo;
    if (0 == ne) {
      // ... Le field absent, see gemSpec_COS#(N032.400)a
      leDo = new byte[0];
    } else {
      // ... Le field present, see gemSpec_COS#(N032.400)b
      if (CommandApdu.NE_SHORT_WILDCARD == ne) {
        leDo = BerTlv.getInstance(0x97, "00").getEncoded();
      } else if (CommandApdu.NE_EXTENDED_WILDCARD == ne) {
        leDo = BerTlv.getInstance(0x97, "0000").getEncoded();
      } else {
        leDo =
            BerTlv.getInstance(0x97, String.format((ne < 256) ? "%02x" : "%04x", ne)).getEncoded();
      } // end fi
    } // end fi
    return leDo;
  } // end method */

  /**
   * Un-secures a response APDU according to gemSpec_COS#13.3.
   *
   * @param proRsp response APDU to be unsecured
   * @return un-secured response APDU
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>response data-field is not BER-TLV encoded
   *       <li>MAC-DO absent but odd tags in response data-field
   *       <li>odd tags in response data-field but no MAC-DO
   *       <li>MAC is wrong
   *       <li>enciphered data, but incorrect ISO padding
   *     </ol>
   */
  public final ResponseApdu unsecureResponse(final ResponseApdu proRsp) {
    LOGGER.atTrace().log("  protected APDU: {}", proRsp);

    // --- initialize flagSmRspEnc to a secure value
    insSmRspEnc = false; // NOPMD never used (false positive)

    final byte[] rspData = proRsp.getData();
    final ResponseApdu result;
    if (rspData.length > 0) {
      // ... response data available => do unsecure

      // --- separate data field in TLV-objects
      final ConstructedBerTlv rspTlvData = (ConstructedBerTlv) BerTlv.getInstance(0x20, rspData);

      // --- check MAC
      zzzCheckMac(rspTlvData);

      // --- if necessary, work on dataDO
      final Optional<BerTlv> dataDo =
          rspTlvData.getTemplate().parallelStream()
              .filter(
                  i -> {
                    final var tag = i.getTag() | 1; // ignore least significant bit
                    return (0x81 == tag) // plain text DO
                        || (0x87 == tag); // enciphered DO
                  })
              .findAny();
      final byte[] rspD;
      if (dataDo.isPresent()) {
        // ... response data available
        final BerTlv dataTlv = dataDo.get();
        final var tagData = dataTlv.getTag();

        if (0x81 == (tagData | 1)) { // NOPMD literal in an if statement
          // ... plain data-field
          rspD = dataTlv.getValueField();
        } else {
          // ... enciphered data-field
          rspD = decipher(dataTlv.getValueField());
          insSmRspEnc = true;
        } // end else
      } else {
        // ... no response data available
        rspD = AfiUtils.EMPTY_OS;
      } // end fi

      // --- estimate trailer
      final int sw = zzzGetSw(proRsp, rspTlvData);

      result = new ResponseApdu(rspD, sw);
    } else {
      // ... no data available
      result = proRsp;
    } // end fi

    LOGGER.atDebug().log("unprotected APDU: {}", result);
    return result;
  } // end method */

  private void zzzCheckMac(final ConstructedBerTlv rspTlvData) {
    var data = AfiUtils.EMPTY_OS;
    boolean oddTags = false;
    boolean isPreviousTagOdd = false;
    for (final BerTlv o : rspTlvData.getTemplate()) {
      if (0x8e == o.getTag()) { // NOPMD literal in if statement
        // ... macDO
        //     => ignore
        continue;
      } // end fi

      if (0 == (o.getTag() & 0x1)) {
        // ... even tag => not included into MAC-calculation
        if (isPreviousTagOdd) {
          data = padIso(data);
        } // end fi
        isPreviousTagOdd = false;
      } else {
        // ... odd tag
        oddTags = true;
        data = AfiUtils.concatenate(data, o.getEncoded());
        isPreviousTagOdd = true;
      } // end fi (even or odd tag)
    } // end For (o...)

    zzzCheckMacInner(rspTlvData, oddTags, data);
  } // end method */

  private void zzzCheckMacInner(
      final ConstructedBerTlv rspTlvData, final boolean oddTags, final byte[] data) {
    final Optional<PrimitiveBerTlv> macDo = rspTlvData.getPrimitive(0x8e);
    if (oddTags && macDo.isEmpty()) {
      throw new IllegalArgumentException("odd tags, but no MAC-DO");
    } // end fi

    if (macDo.isPresent()) {
      if (!oddTags) {
        throw new IllegalArgumentException("MAC-data object, but only even tags");
      } // end fi

      // verify cryptographic checksum
      if (!verifyCryptographicChecksum(data, macDo.get().getValueField())) {
        throw new IllegalArgumentException("Wrong MAC");
      } // end fi
    } // end fi (macDO available)
  } // end method */

  private static int zzzGetSw(final ResponseApdu proRsp, final ConstructedBerTlv rspTlvData) {
    final int result;

    final Optional<PrimitiveBerTlv> swDo = rspTlvData.getPrimitive(0x99);
    if (swDo.isPresent()) {
      // check length of status DO
      final var data = swDo.get().getValueField();
      if (2 != data.length) { // NOPMD literal in if statement
        throw new IllegalArgumentException("Status-DO has wrong length");
      } // end fi
      result = ((data[0] & 0xff) << 8) + (data[1] & 0xff);
    } else {
      result = proRsp.getTrailer();
    } // end fi
    return result;
  } // end method */

  /**
   * Computes a MAC for the given message.
   *
   * @param message of arbitrary length (i.e., without padding)
   * @return MAC
   */
  protected abstract byte[] computeCryptographicChecksum(byte[] message); // */

  /**
   * Deciphers given cryptogram, inverse operation to {@link #encipher(byte[])}.
   *
   * @param data one octet padding indicator (<b>SHALL</b> be equal to '01') concatenated with a
   *     cryptogram
   * @return plain text without any padding
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>Padding indicator is not '01'
   *       <li>an error occurred during deciphering
   *       <li>{@code plainText} is not correctly padded
   *     </ol>
   */
  protected abstract byte[] decipher(byte[] data); // */

  /**
   * Enciphers given message, inverse operation to {@link #decipher(byte[])}.
   *
   * @param message of arbitrary length (i.e., without any padding)
   * @return padding indicator '01' concatenated with corresponding cryptogram
   */
  protected abstract byte[] encipher(byte[] message); // */

  /**
   * Checks if data correspond to MAC.
   *
   * @param data protected by MAC, note: there is no ISO padding at the end of data
   * @param mac protecting data
   * @return {@code TRUE} if MAC matches expectation, {@code FALSE} otherwise
   */
  protected abstract boolean verifyCryptographicChecksum(byte[] data, byte[] mac); // */

  /**
   * Performs an ISO padding on given message.
   *
   * @param message to be padded
   * @return {@code message || 80 (00 ... 00)}, length is a multiple of 16
   */
  protected abstract byte[] padIso(byte[] message); // */

  /**
   * Getter.
   *
   * @return {@code TRUE}, then (if present) command data will be enciphered {@code FALSE}, then
   *     command data will be transferred as plain text
   */
  public final boolean isSmCmdEnc() {
    return insSmCmdEnc;
  } // end method */

  /**
   * Getter.
   *
   * @return {@code TRUE}, then response data in the last received {@link ResponseApdu} where
   *     available and enciphered, {@code FALSE} otherwise
   */
  public final boolean isSmRspEnc() {
    return insSmRspEnc;
  } // end method */

  /**
   * Setter.
   *
   * @param flag {@code TRUE}, then (if present) command data will be enciphered {@code FALSE}, then
   *     command data is transferred as plain text
   */
  public final void setSmCmdEnc(final boolean flag) {
    insSmCmdEnc = flag;
  } // end method */
} // end class
