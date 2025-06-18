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

import de.gematik.smartcards.g2icc.proxy.IccSmc;
import de.gematik.smartcards.g2icc.proxy.IccUser;
import de.gematik.smartcards.sdcom.apdu.ApduLayer;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.utils.AfiUtils;

/**
 * This class uses of an SMC for securing and un-securing of APDU.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public final class SecureMessagingLayerSmc extends SecureMessagingLayer {

  /**
   * Controls the SSCmac behavior for AES cipher, see gemSpec_COS#(N087.220)a.
   *
   * <p>Value is set to 1, i.e., increment SSCmac during PSO Compute CC in order to prepare the
   * first command response pair.
   */
  private final byte[] insFlagSscMacIncrement = new byte[] {1}; // */

  /** IFD with ICC where Trusted Channel begins. */
  private final ApduLayer insSmc; // */

  /**
   * Comfort constructor.
   *
   * @param ifd where the trusted channel ends and session keys are used for secure messaging
   * @param smc where the trusted channel starts and session keys are used for TC
   */
  public SecureMessagingLayerSmc(final IccUser ifd, final IccSmc smc) {
    super(ifd);
    insSmc = smc;
  } // end constructor */

  /**
   * Computes a MAC for the given message.
   *
   * @param message of arbitrary length (i.e., without padding)
   * @return MAC
   */
  @Override
  protected byte[] computeCryptographicChecksum(final byte[] message) {
    return insSmc
        .send(
            new CommandApdu(
                0x00,
                0x2a,
                0x8e,
                0x80, // command header of PSO Compute CC
                AfiUtils.concatenate(insFlagSscMacIncrement, message),
                CommandApdu.NE_SHORT_WILDCARD),
            0x9000)
        .getData();
  } // end method */

  /**
   * Deciphers given cryptogram, inverse operation to {@link #encipher(byte[])}.
   *
   * @param cipher padding indicator || cryptogram
   * @return plain text without any padding
   * @see SecureMessagingLayer#decipher(byte[])
   */
  @Override
  protected byte[] decipher(final byte[] cipher) {
    return insSmc
        .send(
            new CommandApdu(
                0x00,
                0x2a,
                0x80,
                0x86, // command header of PSO Decipher
                cipher,
                CommandApdu.NE_EXTENDED_WILDCARD),
            0x9000)
        .getData();
  } // end method */

  /**
   * Enciphers given message, inverse operation to {@link #decipher(byte[])}.
   *
   * @param message of arbitrary length (i.e., without any padding)
   * @return paddingIndicator || cipher text
   * @see SecureMessagingLayer#encipher(byte[])
   */
  @Override
  protected byte[] encipher(final byte[] message) {
    // SSCmac incremented here, so there is no need to increment
    // it a second time during PSO Compute CC
    insFlagSscMacIncrement[0] = 0;

    return insSmc
        .send(
            new CommandApdu(
                0x00,
                0x2a,
                0x86,
                0x80, // command header of PSO Encipher
                message,
                CommandApdu.NE_EXTENDED_WILDCARD),
            0x9000)
        .getData();
  } // end method */

  /**
   * Checks if data correspond to MAC.
   *
   * @param data protected by MAC, note: there is no ISO padding at the end of data
   * @param mac protecting data
   * @return true if MAC is correct, false otherwise
   */
  @Override
  protected boolean verifyCryptographicChecksum(final byte[] data, final byte[] mac) {
    insFlagSscMacIncrement[0] = 1; // prepare the flag for the next command-response pair

    final int trailer =
        insSmc
            .send(
                new CommandApdu(
                    0x00,
                    0x2a,
                    0x00,
                    0xa2, // command header of PSO Verify CC
                    AfiUtils.concatenate(
                        BerTlv.getInstance(0x80, data).getEncoded(),
                        BerTlv.getInstance(0x8e, mac).getEncoded())))
            .getTrailer();

    return 0x9000 == trailer;
  } // end method */
} // end class
