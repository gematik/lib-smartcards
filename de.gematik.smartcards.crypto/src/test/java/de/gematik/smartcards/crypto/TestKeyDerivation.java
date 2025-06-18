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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiBigInteger;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/**
 * Class testing key derivation from gemSpec_TK_FD.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.AvoidInstantiatingObjectsInLoops",
  "PMD.MethodNamingConventions",
  "checkstyle.methodname"
})
final class TestKeyDerivation {

  /** MK.CMS.AES128.ENC. */
  public static final String MK_CMS_AES128_ENC =
      Hex.extractHexDigits("01010203 04050607 08090a0b 0c0d0e0f"); // */

  /** MK.CMS.AES128.MAC. */
  public static final String MK_CMS_AES128_MAC =
      Hex.extractHexDigits("02010203 04050607 08090a0b 0c0d0e0f"); // */

  /** MK.CMS.AES128.ENC. */
  public static final String MK_CMS_AES256_ENC =
      Hex.extractHexDigits(
          "01010203 04050607 08090a0b 0c0d0e0f 10111213 14151617 18191a1b 1c1d1e1f"); // */

  /** MK.CMS.AES128.MAC. */
  public static final String MK_CMS_AES256_MAC =
      Hex.extractHexDigits(
          "02010203 04050607 08090a0b 0c0d0e0f 10111213 14151617 18191a1b 1c1d1e1f"); // */

  /** MK.VSD.AES128.ENC. */
  public static final String MK_VSD_AES128_ENC =
      Hex.extractHexDigits("03010203 04050607 08090a0b 0c0d0e0f"); // */

  /** MK.VSD.AES128.MAC. */
  public static final String MK_VSD_AES128_MAC =
      Hex.extractHexDigits("04010203 04050607 08090a0b 0c0d0e0f"); // */

  /** MK.VSD.AES128.ENC. */
  public static final String MK_VSD_AES256_ENC =
      Hex.extractHexDigits(
          "03010203 04050607 08090a0b 0c0d0e0f 10111213 14151617 18191a1b 1c1d1e1f"); // */

  /** MK.VSD.AES128.MAC. */
  public static final String MK_VSD_AES256_MAC =
      Hex.extractHexDigits(
          "04010203 04050607 08090a0b 0c0d0e0f 10111213 14151617 18191a1b 1c1d1e1f"); // */

  /** All masterkeys. */
  public static final List<String> MASTER_KEYS =
      List.of(
          MK_CMS_AES128_ENC, MK_CMS_AES128_MAC,
          MK_CMS_AES256_ENC, MK_CMS_AES256_MAC,
          MK_VSD_AES128_ENC, MK_VSD_AES128_MAC,
          MK_VSD_AES256_ENC, MK_VSD_AES256_MAC); // */

  /**
   * Test method for ATOS algorithm.
   *
   * <p>Key derivation and key values here are taken from gemSpec_TK_FD clause 4.3.3.1.
   */
  @Test
  void test_Atos() {
    final var ff = Hex.extractHexDigits("ffffffff ffffffff ffffffff ffffffff");

    final var map =
        Map.ofEntries(
            Map.entry(
                "80276001040000000001",
                List.of(
                    "83B71CA85A0F940FD154409AC67AE0DB",
                    "FA65036CC682E440903A9BA7F90E0F2C",
                    "FA9E833E3584F7B2F27F08E2E9C4B72D4112B78A4236AF799ADF6A25584A1848",
                    "FF9690C39521DD9BC7DD8D8B33B741A8888BDE8FA8DEF8DCA840079FF646AAE8",
                    "47E68B915481A7A6B772D58AB55CC48C",
                    "F602F5C2F838B8230F0B623131B9A35B",
                    "C82DE2D3878F8257C452F0E355A1212E2D5A3F4F96CBC4503885D3CF593C9018",
                    "1F3901DD3274FC85822853276D369BA408B5AFEE6FA2804FE42115EF0C314804")));
    for (final var entry : map.entrySet()) {
      final var iccsn = entry.getKey();
      final var keys = entry.getValue();

      assertEquals(10 * 2, iccsn.length());

      final var y = AfiBigInteger.i2os(new BigInteger(iccsn, 16).shiftLeft(8), 16);
      final var yStar = AfiBigInteger.i2os(new BigInteger(1, y).xor(new BigInteger(ff, 16)), 16);
      assertEquals("0000000000" + iccsn + "00", Hex.toHexDigits(y));
      final var plaintext = AfiUtils.concatenate(y, yStar);

      try {
        for (int i = 0; i < keys.size(); i++) {
          final var key = Hex.toByteArray(MASTER_KEYS.get(i));
          final var keySpec = new SecretKeySpec(key, 0, key.length, "AES"); // NOPMD new in loop
          final var cipher = Cipher.getInstance("AES/ECB/NoPadding");
          cipher.init(Cipher.ENCRYPT_MODE, keySpec);

          final var sk =
              Hex.toHexDigits(Arrays.copyOfRange(cipher.doFinal(plaintext), 0, key.length));

          assertEquals(keys.get(i).toLowerCase(Locale.ENGLISH), sk);
        } // end For (i...)
      } catch (NoSuchPaddingException
          | IllegalBlockSizeException
          | NoSuchAlgorithmException
          | BadPaddingException
          | InvalidKeyException e) {
        fail(UNEXPECTED, e); // NOPMD raw exception thrown
      } // end Catch (...)
    } // end For (entry...)
  } // end method */
} // end class
