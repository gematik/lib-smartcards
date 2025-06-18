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
package de.gematik.smartcards.crypto;

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;

import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiUtils;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Class for performing symmetric cryptography according to FIPS 197 (AES).
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize", "PMD.TooManyMethods"})
public final class AesKey {

  /** Block length, see FIPS-197:2023, clause 2.1, term "Block". */
  public static final int BLOCK_LENGTH = 16; // */

  /** Infimum length for a CMAC. */
  public static final int INFIMUM_CMAC_LENGTH = BLOCK_LENGTH >> 1; // */

  /** AES-CBC transformation without padding. */
  public static final String AES_CBC_NO_PADDING = "AES/CBC/NoPadding"; // */

  /** AES-GCM transformation. */
  public static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding"; // */

  /** Object with key material. */
  private final SecretKeySpec insSecretKeySpec;

  /** Sub-key used for CMAC calculation. */
  private final byte[] insK1;

  /** Sub-key used for CMAC calculation. */
  private final byte[] insK2;

  /**
   * Constructs a secret key from the given byte array, using the first len bytes of key, starting
   * at offset inclusive.
   *
   * <p>The bytes that constitute the secret key are those between key[offset] and key[offset+len-1]
   * inclusive.
   *
   * @param key the key material of the secret key
   * @param offset the offset in <code>key</code> where the key material starts
   * @param len the length of the key material
   * @throws IllegalArgumentException if
   *     <ul>
   *       <li>key is null
   *       <li>{@code offset + len} exceeds length of {@code key}
   *       <li>len is not in set {16, 24, 32}
   *     </ul>
   *
   * @throws ArrayIndexOutOfBoundsException is thrown if offset is negative
   */
  public AesKey(final byte[] key, final int offset, final int len) {
    if ((16 != len) && (24 != len) && (32 != len)) {
      throw new IllegalArgumentException("invalid len");
    } // end fi

    // --- initialize key
    insSecretKeySpec = new SecretKeySpec(key, offset, len, "AES");

    // --- initialize constants for CMAC calculation
    final var varRb = BigInteger.valueOf(0x87);
    final var varL = encipherCbc(new byte[BLOCK_LENGTH]);
    insK1 =
        AfiBigInteger.i2os(
            new BigInteger(1, varL).shiftLeft(1).xor((varL[0] >= 0) ? BigInteger.ZERO : varRb),
            BLOCK_LENGTH);
    insK2 =
        AfiBigInteger.i2os(
            new BigInteger(1, insK1).shiftLeft(1).xor((insK1[0] >= 0) ? BigInteger.ZERO : varRb),
            BLOCK_LENGTH);
  } // end constructor */

  /**
   * CMAC calculation.
   *
   * @param m message of arbitrary length for which a CMAC is calculated
   * @return CMAC consisting of {@link #BLOCK_LENGTH} octets
   */
  public byte[] calculateCmac(final byte[] m) {
    final byte[] macInput;
    if ((m.length > 0) && (0 == (m.length % BLOCK_LENGTH))) {
      // ... no padding
      macInput = m.clone();
      for (int j = BLOCK_LENGTH; j-- > 0; ) { // NOPMD assignment in operand
        macInput[macInput.length - BLOCK_LENGTH + j] ^= insK1[j];
      } // end For (j...)
    } else {
      // ... padding necessary
      macInput = padIso(m);
      for (int j = BLOCK_LENGTH; j-- > 0; ) { // NOPMD assignment in operand
        macInput[macInput.length - BLOCK_LENGTH + j] ^= insK2[j];
      } // end For (j...)
    } // end fi
    // ... macInput contains multiple blocks

    final byte[] c = encipherCbc(macInput);

    // --- extract len MSByte from the last block
    return Arrays.copyOfRange(c, c.length - BLOCK_LENGTH, c.length);
  } // end method */

  /**
   * CMAC calculation.
   *
   * @param m message of arbitrary length for which a CMAC is calculated
   * @param length arbitrary number of expected octets in CMAC; if less than {@link
   *     #INFIMUM_CMAC_LENGTH} then {@link #INFIMUM_CMAC_LENGTH} octets in the CMAC are returned; if
   *     greater than {@link #BLOCK_LENGTH} then all octets in the CMAC are returned
   * @return {@code length} most significant octets from full length CMAC; at least {@link
   *     #INFIMUM_CMAC_LENGTH}; at most {@link #BLOCK_LENGTH} octets
   */
  public byte[] calculateCmac(final byte[] m, final int length) {
    return Arrays.copyOfRange(
        calculateCmac(m), 0, Math.min(Math.max(INFIMUM_CMAC_LENGTH, length), BLOCK_LENGTH));
  } // end method */

  /**
   * CMAC verification.
   *
   * @param m message of arbitrary length for which a CMAC is calculated
   * @param cmac CMAC of arbitrary length and content
   * @return {@code TRUE} if and only if length of {@code cmac} is at least half of {@link
   *     #BLOCK_LENGTH} and the most significant octet of the full length CMAC for message {@code m}
   *     are identical to the given value {@code cmac}, {@code FALSE} otherwise
   */
  public boolean verifyCmac(final byte[] m, final byte[] cmac) {
    return Arrays.equals(calculateCmac(m, cmac.length), cmac);
  } // end method */

  /**
   * Deciphers given {@code ciphertext} making use ICV=´00..00'.
   *
   * <p>This is the inverse operation to {@link #encipherCbc(byte[])}.
   *
   * @param ciphertext message to be deciphered, <b>SHALL</b> have a length which is a multiple of
   *     {@link #BLOCK_LENGTH}
   * @return plaintext
   * @throws IllegalArgumentException if length of {@code ciphertext} is not a multiple of {@link
   *     #BLOCK_LENGTH}
   */
  public byte[] decipherCbc(final byte[] ciphertext) {
    return decipherCbc(ciphertext, new byte[BLOCK_LENGTH]);
  } // end method */

  /**
   * Deciphers given {@code ciphertext} making use of given ICV.
   *
   * <p>This is the inverse operation to {@link #encipherCbc(byte[], byte[])}.
   *
   * @param ciphertext message to be deciphered, <b>SHALL</b> have a length which is a multiple of
   *     {@link #BLOCK_LENGTH}
   * @param icv the Initial Chaining Value, <b>SHALL</b> consists of {@link #BLOCK_LENGTH} octets
   * @return plaintext
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>length of {@code ciphertext} is not a multiple of {@link #BLOCK_LENGTH}
   *       <li>length of {@code icv} differs from {@link #BLOCK_LENGTH}
   *     </ol>
   */
  public byte[] decipherCbc(final byte[] ciphertext, final byte[] icv) {
    try {
      return cryptoCbc(Cipher.DECRYPT_MODE, ciphertext, icv);
    } catch (IllegalBlockSizeException e) {
      throw new IllegalArgumentException("invalid ciphertext.length", e);
    } // end Catch (...)
  } // end method */

  /**
   * Enciphers given {@code plaintext} and makes use of ICV='00...00'.
   *
   * <p>This is the inverse operation to {@link #decipherCbc(byte[])}.
   *
   * @param plaintext message to be enciphered, <b>SHALL</b> have a length which is a multiple of
   *     {@link #BLOCK_LENGTH}
   * @return ciphertext
   * @throws IllegalArgumentException if length of {@code plaintext} is not a multiple of {@link
   *     #BLOCK_LENGTH}
   */
  public byte[] encipherCbc(final byte[] plaintext) {
    return encipherCbc(plaintext, new byte[BLOCK_LENGTH]);
  } // end method */

  /**
   * Enciphers given {@code plaintext} and makes use of given ICV.
   *
   * <p>This is the inverse operation to {@link #decipherCbc(byte[], byte[])}.
   *
   * @param plaintext message to be enciphered, <b>SHALL</b> have a length which is a multiple of
   *     {@link #BLOCK_LENGTH}
   * @param icv the Initial Chaining Value, <b>SHALL</b> consists of {@link #BLOCK_LENGTH} octets
   * @return ciphertext
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>length of {@code plaintext} is not a multiple of {@link #BLOCK_LENGTH}
   *       <li>length of {@code icv} differs from {@link #BLOCK_LENGTH}
   *     </ol>
   */
  public byte[] encipherCbc(final byte[] plaintext, final byte[] icv) {
    try {
      return cryptoCbc(Cipher.ENCRYPT_MODE, plaintext, icv);
    } catch (IllegalBlockSizeException e) {
      throw new IllegalArgumentException("invalid plaintext.length", e);
    } // end Catch (...)
  } // end method */

  /**
   * Deciphers given {@code ciphertext}.
   *
   * @param ciphertext message to be deciphered
   * @param nonce the unique nonce, typically twelve octets
   * @param associatedData which is authenticated, but not encrypted
   * @param tagLength length of tag, <b>SHALL</b>> be in set {128, 120, 112, 104, 96}
   * @return plaintext
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code tagLength} is not in set {128, 120, 112, 104, 96}
   *       <li>tag mismatch
   *     </ol>
   */
  public byte[] decipherGcm(
      final byte[] ciphertext,
      final byte[] nonce,
      final byte[] associatedData,
      final int tagLength) {
    try {
      final Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
      final var nc = AfiUtils.concatenate(nonce, ciphertext);
      final var gcmParameterSpec = new GCMParameterSpec(tagLength, nonce, 0, nonce.length);
      cipher.init(Cipher.DECRYPT_MODE, insSecretKeySpec, gcmParameterSpec);
      cipher.updateAAD(associatedData);

      return cipher.doFinal(nc, nonce.length, ciphertext.length);
    } catch (AEADBadTagException e) {
      throw new IllegalArgumentException("tag mismatch"); // NOPMD no stack trace
    } catch (BadPaddingException // thrown in case of "Tag mismatch"
        | IllegalBlockSizeException // impossible for GCM-mode
        | InvalidAlgorithmParameterException // it tagLength is invalid
        | InvalidKeyException // impossible if constructor works as expected
        | NoSuchAlgorithmException // unlikely
        | NoSuchPaddingException e // unlikely
    ) {
      throw new IllegalArgumentException(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Enciphers given {@code plaintext}.
   *
   * @param plaintext message to be enciphered; any length and content are valid
   * @param nonce the unique nonce, typically twelve octets
   * @param associatedData which is authenticated, but not encrypted
   * @param tagLength length of tag, <b>SHALL</b>> be in set {128, 120, 112, 104, 96}
   * @return ciphertext
   * @throws IllegalArgumentException if {@code tagLength} is not in set {128, 120, 112, 104, 96}
   */
  public byte[] encipherGcm(
      final byte[] plaintext,
      final byte[] nonce,
      final byte[] associatedData,
      final int tagLength) {
    try {
      final Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
      final var gcmParameterSpec = new GCMParameterSpec(tagLength, nonce, 0, nonce.length);
      cipher.init(Cipher.ENCRYPT_MODE, insSecretKeySpec, gcmParameterSpec);
      cipher.updateAAD(associatedData);

      return cipher.doFinal(plaintext);
    } catch (BadPaddingException // impossible, because of "NoPadding"
        | IllegalBlockSizeException // impossible for GCM-mode
        | InvalidAlgorithmParameterException // it tagLength is invalid
        | InvalidKeyException // impossible if constructor works as expected
        | NoSuchAlgorithmException // unlikely
        | NoSuchPaddingException e // unlikely
    ) {
      throw new IllegalArgumentException(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Extends a given {@code input} according to ISO/IEC 7816-4 padding.
   *
   * <p>This is the inverse operation to {@link #truncateIso(byte[])}.
   *
   * @param input to be padded
   * @return extended {@code input}
   */
  public byte[] padIso(final byte[] input) {
    final var newLength = ((input.length / BLOCK_LENGTH) + 1) * BLOCK_LENGTH;
    final var result = Arrays.copyOfRange(input, 0, newLength);

    // --- set delimiter according to ISO/IEC 7816-4
    result[input.length] = (byte) 0x80;

    return result;
  } // end method */

  /**
   * Truncates a given {@code input} according to ISO/IEC 7816-4 padding.
   *
   * <p>This is the inverse operation to {@link #padIso(byte[])}.
   *
   * @param input to be truncated
   * @return truncated {@code input}
   * @throws IllegalArgumentException {@code input} is not correctly padded
   */
  public byte[] truncateIso(final byte[] input) {
    try {
      int blockLength = BLOCK_LENGTH;
      int i = input.length;

      // truncate the rest of padding bytes, at most block length - 1
      while ((input[--i] == 0x00) && (--blockLength > 0)) { // NOPMD empty while
        // intentionally empty
      } // end While (...)

      // test and truncate delimiter
      if (-128 == input[i]) {
        return Arrays.copyOfRange(input, 0, i);
      } // end fi
    } catch (ArrayIndexOutOfBoundsException e) { // NOPMD empty catch block
      // intentionally empty
    } // end Catch (...)
    // ... invalid padding

    throw new IllegalArgumentException("padding not in accordance to ISO/IEC 7816-4");
  } // end method */

  /**
   * Enciphers or deciphers given {@code blob} and makes use of given ICV.
   *
   * @param mode indicates requested operation
   * @param blob message to be enciphered/deciphered, <b>SHALL</b> have a length which is a multiple
   *     of {@link #BLOCK_LENGTH}
   * @param icv the Initial Chaining Value, <b>SHALL</b> consists of {@link #BLOCK_LENGTH} octets
   * @return ciphertext
   * @throws IllegalArgumentException if length of {@code blob} is not a multiple of {@link
   *     #BLOCK_LENGTH}
   * @throws IllegalBlockSizeException if length of {@code icv} differs from {@link #BLOCK_LENGTH}
   */
  private byte[] cryptoCbc(final int mode, final byte[] blob, final byte[] icv)
      throws IllegalBlockSizeException {
    try {
      final Cipher cipher = Cipher.getInstance(AES_CBC_NO_PADDING);
      cipher.init(mode, insSecretKeySpec, new IvParameterSpec(icv));

      return cipher.doFinal(blob);
    } catch (BadPaddingException // impossible, because of "NoPadding"
        | InvalidAlgorithmParameterException
        | InvalidKeyException // impossible if constructor works as expected
        | NoSuchAlgorithmException // unlikely
        | NoSuchPaddingException e // unlikely
    ) {
      throw new IllegalArgumentException("invalid ICV.length", e);
    } // end Catch (...)
  } // end method */
} // end class
