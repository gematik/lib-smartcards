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

import de.gematik.smartcards.utils.Hex;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Format used during user verification.
 *
 * <p>This enumeration lists the formats used to convert a character-sequence (i.e., a user-secret)
 * into an octet string used in a command data field.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public enum EafiPasswordFormat {
  /** Decimal digits to and from BCD conversion. */
  BCD {
    /**
     * Converts secret to a pseudo-command-data-field.
     *
     * <p>Arbitrary decimal digits of any length are accepted by this format. Each decimal digit is
     * converted to a nibble. Non-decimal characters are ignored. If the number of decimal digits is
     * odd, then the result is right padded with 'f', e.g.
     *
     * <ol>
     *   <li>
     *       <pre>"1234" -> "****"</pre>
     *   <li>
     *       <pre>"Alfred: 123.45 Secret" -> "12345" -> "*****f"</pre>
     *       (ignoring non-decimal characters, padding necessary)
     * </ol>
     *
     * @param secret character-sequence (e.g., a PIN or a password)
     * @return corresponding command data field with '*'-characters blinding the secret
     * @see #blind(String)
     */
    @Override
    public String blind(final String secret) {
      final int length = countDecimalDigits(secret);
      final char[] result = new char[length];

      final String suffix;
      if (1 == (length & 1)) { // NOPMD literal in if statement
        // ... length is odd
        //     => pad with 'f'
        suffix = "f";
      } else {
        suffix = "";
      } // end fi

      Arrays.fill(result, '*');

      return new String(result) + suffix; // NOPMD String instantiation
    } // end method

    /**
     * Character-sequence to octet-string conversion.
     *
     * <p>Arbitrary decimal digits of any length are accepted by this format. Each decimal digit is
     * converted to a nibble. Non-decimal characters are ignored. If the number of decimal digits is
     * odd, then the result is right padded with 'f', e.g.
     *
     * <ol>
     *   <li>
     *       <pre>"1234" -> '1234'</pre>
     *   <li>
     *       <pre>"Alfred: 123.45 Secret" -> "12345" -> '12345f'</pre>
     *       (ignoring non-decimal characters, padding necessary)
     * </ol>
     *
     * @param secret character-sequence (i.e. a PIN)
     * @return corresponding octet-string used in a command data field
     * @see #octets(String)
     */
    @Override
    public byte[] octets(final String secret) {
      final int[] codePoints = extractDecimalDigits(secret);
      final int length = codePoints.length;

      final String suffix;
      if (1 == (length & 1)) { // NOPMD literal in if statement
        // ... length is odd
        //     => pad with 'f'
        suffix = "f";
      } else {
        suffix = "";
      } // end fi

      return Hex.toByteArray(new String(codePoints, 0, length) + suffix);
    } // end method */

    /**
     * Octet-string to character-string conversion.
     *
     * <p>Arbitrary octet strings of any length are accepted by this format. Only nibble in the
     * range {@code [0, 9]} are taken into account, e.g.
     *
     * <ol>
     *   <li>
     *       <pre>'1234'       -> "1234"</pre>
     *   <li>
     *       <pre>'12345f'     -> "12345"</pre>
     *       (ignoring non-decimal digits, here padding)
     *   <li>
     *       <pre>'12abcdef3a" -> "123"</pre>
     *       (ignoring non-decimal digits)
     * </ol>
     *
     * @param octets typically used in a command data field
     * @return corresponding character-sequence (i.e. a PIN)
     * @see #secret(byte[])
     */
    @Override
    public String secret(final byte[] octets) {
      final int[] digits = extractDecimalDigits(Hex.toHexDigits(octets));

      return new String(digits, 0, digits.length);
    } // end method */
  }, // end BCD

  /** Format-2-PIN-Block conversion. */
  FORMAT_2_PIN_BLOCK {
    /**
     * Converts secret to a pseudo-command-data-field.
     *
     * <p>Arbitrary characters (not only decimal digits) of any length are accepted by this format.
     * Each decimal digit is converted to a nibble. Non-decimal-digits are ignored. The number of
     * decimal digits may be zero but SHALL NOT exceed 14.
     *
     * <ol>
     *   <li>
     *       <pre>"123456"         -> "26******ffffffff"</pre>
     *   <li>
     *       <pre>"AbcDef"         -> "20ffffffffffffff"</pre>
     *   <li>
     *       <pre>"12345678901234" -> "2e**************"</pre>
     * </ol>
     *
     * @param secret character-sequence (i.e. PIN)
     * @return corresponding octet-string used in a command data field
     * @throws IllegalArgumentException if {@code secret} contains more than 14 decimal digits
     * @see #blind(String)
     */
    @Override
    public String blind(final String secret) {
      final int length = countDecimalDigits(secret);

      if (length > 14) { // NOPMD literal in if statement
        // ... secret too long
        //     => throw exception
        throw new IllegalArgumentException("secret too long");
      } // end fi
      // ... length is ok

      return String.format("2%x%sffffffffffffff", length, "**************".substring(0, length))
          .substring(0, 16);
    } // end method */

    /**
     * Character-sequence to octet-string conversion.
     *
     * <p>Arbitrary characters (not only decimal digits) of any length are accepted by this format.
     * Each decimal digit is converted to a nibble. Non-decimal-digits are ignored. The number of
     * decimal digits may be zero but SHALL NOT exceed 14.
     *
     * <ol>
     *   <li>
     *       <pre>"123456"         -> '26123456ffffffff'</pre>
     *   <li>
     *       <pre>"AbcDef"         -> '20ffffffffffffff'</pre>
     *   <li>
     *       <pre>"12345678901234" -> '2e12345678901234'</pre>
     * </ol>
     *
     * @param secret character-sequence (i.e. PIN)
     * @return corresponding octet-string used in a command data field
     * @throws IllegalArgumentException if {@code secret} contains more than 14 decimal digits
     * @see #octets(String)
     */
    @Override
    public byte[] octets(final String secret) {
      final int[] codePoints = extractDecimalDigits(secret);
      final int length = codePoints.length;

      if (length > 14) { // NOPMD literal in if statement
        // ... secret too long
        //     => throw exception
        throw new IllegalArgumentException("secret too long");
      } // end fi
      // ... length is ok

      return Hex.toByteArray(
          String.format("2%x%sffffffffffffff", length, new String(codePoints, 0, length))
              .substring(0, 16));
    } // end method */

    /**
     * Octet-string to character-string conversion.
     *
     * <p>Arbitrary octet strings with zero octet are accepted by this format. The first nibble
     * SHAll be two indicating Format-2-PIN-Block. The second nibble SHALL NOT be 'f', because the
     * remaining seven octet cannot contain 15 digits. From the third nibble on as many nibbles as
     * indicated by the second nibble are extracted regardless of their content, e.g.:
     *
     * <ol>
     *   <li>'26123456ffffffff' -> "123456"
     *   <li>'20ffffffffffffff' -> ""
     *   <li>'2e123456789abcde' -> "123456789abcde"
     * </ol>
     *
     * <p><i><b>Note:</b> This method doesn't ignore hex-digits from rang [a, f] but the inverse
     * method {@link #octets(String)} does.</i>
     *
     * @param octets typically used in a command data field
     * @return corresponding character-sequence (i.e. PIN)
     * @throws IllegalArgumentException if
     *     <ol>
     *       <li>number of octet in {@code octets} differ from eight
     *       <li>first nibble differs from '2'
     *       <li>the second nibble contains 'f'
     *     </ol>
     *
     * @see #secret(byte[])
     */
    @Override
    public String secret(final byte[] octets) {
      if (8 != octets.length) { // NOPMD literal in if statement
        throw new IllegalArgumentException("invalid input-length");
      } // end fi

      if (0x20 != (octets[0] & 0xf0)) { // NOPMD literal in if statement
        throw new IllegalArgumentException("type indicator != 2");
      } // end fi
      // ... parameter "octets" contains 8 octet and the first nibble is '2'

      final int length = octets[0] & 0x0f;

      if (15 == length) { // NOPMD literal in if statement
        // ... invalid length
        throw new IllegalArgumentException("invalid 2nd nibble");
      } // end fi

      // possibly throws ArrayIndexOutOfBoundsException if length = 15 = 0xf
      return Hex.toHexDigits(octets, 1, 7).substring(0, length);
    } // end method */
  }, // end FORMAT_2_PIN_BLOCK

  /**
   * Conversion to and from arbitrary character-sequence to {@link StandardCharsets#UTF_8 UTF.8}.
   */
  UTF8 {
    /**
     * Converts secret to a pseudo-command-data-field.
     *
     * <p>Arbitrary characters (not only decimal digits) of any length are accepted by this format.
     * Each character is replaced by '*', e.g.
     *
     * <ol>
     *   <li>
     *       <pre>"1234"   -> "****"</pre>
     *   <li>
     *       <pre>"Alfred" -> "******"</pre>
     * </ol>
     *
     * @param secret character-sequence (e.g. a PIN or a password)
     * @return corresponding command data field
     * @see #blind(String)
     */
    @Override
    public String blind(final String secret) {
      final char[] result = new char[secret.codePoints().toArray().length];

      Arrays.fill(result, '*');

      return new String(result); // NOPMD String instantiation
    } // end method

    /**
     * Character-sequence to octet-string conversion.
     *
     * <p>Arbitrary characters (not only decimal digits) of any length are accepted by this format.
     * Each character is converted to the corresponding {@link StandardCharsets#UTF_8} coding, e.g.
     *
     * <ol>
     *   <li>
     *       <pre>"1234"   -> '31323334'</pre>
     *   <li>
     *       <pre>"Alfred" -> '416c66726564'</pre>
     * </ol>
     *
     * @param secret character-sequence (e.g. a PIN or a password)
     * @return corresponding octet-string used in a command data field
     * @see #octets(String)
     */
    @Override
    public byte[] octets(final String secret) {
      return secret.getBytes(StandardCharsets.UTF_8);
    } // end method

    /**
     * Octet-string to character-string conversion.
     *
     * <p>Arbitrary octet strings of any length are accepted by this format. The octet string is
     * converted to a {@link String} assuming {@link StandardCharsets#UTF_8 UTF.8} encoding, e.g.
     *
     * <ol>
     *   <li>
     *       <pre>'31323334'     -> "1234"</pre>
     *   <li>
     *       <pre>'416c66726564' -> "Alfred"</pre>
     * </ol>
     *
     * @param octets typically used in a command data field
     * @return corresponding character-sequence (e.g. a PIN or a password)
     * @see #secret(byte[])
     */
    @Override
    public String secret(final byte[] octets) {
      return new String(octets, StandardCharsets.UTF_8);
    } // end method
  }, // end UTF8
  ;

  /**
   * Converts secret to a pseudo-command-data-field.
   *
   * <p>This method is similar to {@link #octets(String)} but instead of hexadecimal digits the
   * character '*' is used to blind {@code secret} information.
   *
   * @param secret character-sequence (e.g. a PIN or a password)
   * @return corresponding command data field with '*'-characters blinding the secret
   */
  public abstract String blind(String secret);

  /**
   * Character-sequence to octet-string conversion.
   *
   * <p>This method converts a character-sequence (i.e. a user-secret) into an octet-string used in
   * a command data field.
   *
   * <p>This is the inverse operation to {@link #secret(byte[])}.
   *
   * @param secret character-sequence (e.g. a PIN or a password)
   * @return corresponding octet-string used in a command data field
   */
  public abstract byte[] octets(String secret);

  /**
   * Octet-string to character-string conversion.
   *
   * <p>This method converts an octet string (possibly used in a command data field) into a
   * character-sequence (e.g. PIN or password).
   *
   * <p>This is the inverse operation to {@link #octets(String)}.
   *
   * @param octets typically used in a command data field
   * @return corresponding character-sequence (e.g. a PIN or a password)
   */
  public abstract String secret(byte[] octets);

  /**
   * Returns number of decimal digits in given {@link String}.
   *
   * @param secret {@link String} with zero or more decimal digits
   * @return number of decimal digits
   */
  private static int countDecimalDigits(final String secret) {
    return secret
        .codePoints()
        .map(codePoint -> ((0x30 <= codePoint) && (codePoint <= 0x39)) ? 1 : 0)
        .sum();
  } // end method */

  /**
   * Extracts decimal digits from given {@link String}.
   *
   * @param secret {@link String} with zero or more decimal digits
   * @return {@code int[]} with decimal digits
   */
  private static int[] extractDecimalDigits(final String secret) {
    return secret.codePoints().filter(c -> (0x30 <= c) && (c <= 0x39)).toArray();
  } // end method */
} // end enum
