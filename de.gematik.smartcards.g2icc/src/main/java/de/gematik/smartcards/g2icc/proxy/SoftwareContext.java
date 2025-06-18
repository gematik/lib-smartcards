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
package de.gematik.smartcards.g2icc.proxy;

import de.gematik.smartcards.crypto.AesKey;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.util.Arrays;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SessionkeyContext according to gemSpec_COS#(N029.900)d in software for securing and un-securing
 * of APDU.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
final class SoftwareContext {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(SoftwareContext.class); // */

  /** Message for no session keys available. */
  private static final String NO_SESSIONKEYS = "no session keys available"; // */

  /** Padding indicator. */
  private static final byte[] PADDING_INDICATOR = new byte[] {1}; // */

  /** See gemSpec_COS#(N029.900)d.2. */
  private final AesKey insKenc;

  /** See gemSpec_COS#(N029.900)d.4. */
  private final AesKey insKmac;

  /** See gemSpec_COS#(N029.900)d.5. */
  private final byte[] insSscMac;

  /** See gemSpec_COS#(N029.900)d.1. */
  private boolean insFlagSessionEnabled;

  /**
   * Comfort constructor.
   *
   * @param kd key derivation material
   * @param keyLength in bit, controls length of session keys, i.e., AES-128, AES-192, AES-256
   */
  /* package */ SoftwareContext(final byte[] kd, final int keyLength) {
    final byte[] kdEnc = AfiUtils.concatenate(kd, Hex.toByteArray("0000 0001"));
    final byte[] kdMac = AfiUtils.concatenate(kd, Hex.toByteArray("0000 0002"));

    insSscMac = new byte[16]; // see (N001.500)c, (N001.510)c, (N001.520)c
    switch (keyLength) {
      case 128 -> { // see (N031.500)a.1, (N001.500)
        insKenc = new AesKey(EafiHashAlgorithm.SHA_1.digest(kdEnc), 0, 16);
        insKmac = new AesKey(EafiHashAlgorithm.SHA_1.digest(kdMac), 0, 16);
      } // see (N031.500)a.2, (N001.510)
      case 192, 256 -> { // see (N031.500)a.3, (N001.520)
        insKenc = new AesKey(EafiHashAlgorithm.SHA_256.digest(kdEnc), 0, keyLength >> 3);
        insKmac = new AesKey(EafiHashAlgorithm.SHA_256.digest(kdMac), 0, keyLength >> 3);
      }
      default -> throw new IllegalArgumentException(keyLength + " not (yet) implemented");
    } // end Switch (keyLength)

    insFlagSessionEnabled = true;
  } // end constructor */

  /**
   * Return flag.
   *
   * @return unconditionally {@link #insFlagSessionEnabled}
   */
  /* package */ boolean isFlagSessionEnabled() {
    return insFlagSessionEnabled;
  } // end method */

  /**
   * Computes a CMAC.
   *
   * @param m message
   * @param flagSscMacIncrement flag
   * @return CMAC
   */
  /* package */ byte[] computeCryptographicChecksum(
      final byte[] m, final boolean flagSscMacIncrement) {
    if (!insFlagSessionEnabled) {
      throw new RejectedExecutionException(NO_SESSIONKEYS);
    } // end fi
    // ... session keys available

    final byte[] macInput = padIso(m);

    if (flagSscMacIncrement) {
      AfiUtils.incrementCounter(insSscMac);
    } // end fi

    LOGGER.atTrace().log("SSCmac  : {}", Hex.toHexDigits(insSscMac));
    LOGGER.atTrace().log("MACinput: {}", Hex.toHexDigits(macInput));
    final byte[] result = insKmac.calculateCmac(AfiUtils.concatenate(insSscMac, macInput), 8);
    LOGGER.atTrace().log("MAC     : {}", Hex.toHexDigits(result));

    return result;
  } // end method */

  /**
   * Decipher a cryptogram.
   *
   * @param data one octet padding indicator (<b>SHALL</b> be equal to '01') concatenated with a
   *     cryptogram
   * @return plain text
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>Padding indicator is not '01'
   *       <li>an error occurred during deciphering
   *     </ol>
   */
  /* package */ byte[] decipher(final byte[] data) {
    if (!insFlagSessionEnabled) {
      throw new RejectedExecutionException(NO_SESSIONKEYS);
    } // end fi
    // ... session keys available

    try {
      final int pi = data[0] & 0xff;
      if (1 == pi) { // NOPMD literal in if statement
        // ... padding indicator == 1

        final byte[] cipherText = new byte[data.length - 1];
        System.arraycopy(data, 1, cipherText, 0, cipherText.length);

        final byte[] plainText = insKenc.decipherCbc(cipherText, insKenc.encipherCbc(insSscMac));

        return insKenc.truncateIso(plainText);
      } // end fi
      // ... wrong padding indicator, intentionally no further action here
    } catch (IllegalArgumentException e) { // NOPMD empty catch block
      // ... padding error in deciphered data, intentionally no further action here
    } // end Catch (...)
    // ... wrong padding indicator or wrong padding

    insFlagSessionEnabled = false; // end SM session

    // Note: A wrong MAC only ends an SM session. Here intentionally an
    //       exception is thrown, because deciphering should only happen
    //       after MAC verification. So here we have correct MAC but
    //       wrong cryptogram. Thus, something is really strange here.
    throw new IllegalArgumentException("decipher error");
  } // end method */

  /**
   * Encipher a message.
   *
   * @param message to be enciphered
   * @return padding indicator '01' concatenated with corresponding cryptogram
   */
  /* package */ byte[] encipher(final byte[] message) {
    if (!insFlagSessionEnabled) {
      throw new RejectedExecutionException(NO_SESSIONKEYS);
    } // end fi
    // ... session keys available

    final byte[] cipher =
        insKenc.encipherCbc(
            padIso(message), insKenc.encipherCbc(AfiUtils.incrementCounter(insSscMac)));

    return AfiUtils.concatenate(
        PADDING_INDICATOR, // see gemSpec_COS#(N032.200)d
        cipher);
  } // end method */

  /* package */ byte[] padIso(final byte[] m) {
    return insKmac.padIso(m);
  } // end method */

  /**
   * Verify Cryptographic Checksum.
   *
   * <p><i><b>Note:</b> If this method returns {@code FALSE} (i.e. MAC is wrong, then as a
   * side-effect session keys are erased.</i>
   *
   * @param data which is MAC-protected
   * @param mac to be checked
   * @return {@code TRUE} if MAC matches expectation, {@code FALSE} otherwise
   */
  /* package */ boolean verifyCryptographicChecksum(final byte[] data, final byte[] mac) {
    if (!insFlagSessionEnabled) {
      throw new RejectedExecutionException(NO_SESSIONKEYS);
    } // end fi
    // ... session keys available

    AfiUtils.incrementCounter(insSscMac);
    final byte[] macInput = padIso(data);

    LOGGER.atTrace().log("SSCmac  : {}", Hex.toHexDigits(insSscMac));
    LOGGER.atTrace().log("MACinput: {}", Hex.toHexDigits(macInput));
    LOGGER.atTrace().log("MACexpec: {}", Hex.toHexDigits(mac));
    final byte[] macIs = insKmac.calculateCmac(AfiUtils.concatenate(insSscMac, macInput), 8);
    LOGGER.atTrace().log("MACis   : {}", Hex.toHexDigits(macIs));

    insFlagSessionEnabled = Arrays.equals(macIs, mac);
    LOGGER.atTrace().log("MACequal: {}", insFlagSessionEnabled);

    return insFlagSessionEnabled;
  } // end method */
} // end class
