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

import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_ALL_READERS;
import static de.gematik.smartcards.pcsc.lib.WinscardLibrary.SCARD_DEFAULT_READERS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link WinscardLibrary}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions"})
final class TestWinscardLibrary {

  /** Test method for {@link WinscardLibrary#INFINITE}. */
  @Test
  void test_inifinte() {
    assertEquals("ffffffff", String.format("%x", WinscardLibrary.INFINITE));
    assertEquals(0xffffffff, WinscardLibrary.INFINITE);
  } // end method */

  /** Test method for {@link WinscardLibrary#SCARD_ALL_READERS}. */
  @Test
  void test_scardAllReaders() {
    assertEquals(
        Hex.toHexDigits(
            AfiUtils.concatenate("SCard$AllReaders".getBytes(StandardCharsets.UTF_8), new byte[1])),
        Hex.toHexDigits(SCARD_ALL_READERS.getBytes(StandardCharsets.UTF_8)));
  } // end method */

  /** Test method for {@link WinscardLibrary#SCARD_ALL_READERS}. */
  @Test
  void test_scardDefaultReaders() {
    assertEquals(
        Hex.toHexDigits(
            AfiUtils.concatenate(
                "SCard$DefaultReaders".getBytes(StandardCharsets.UTF_8), new byte[1])),
        Hex.toHexDigits(SCARD_DEFAULT_READERS.getBytes(StandardCharsets.UTF_8)));
  } // end method */

  /** Test method for {@link WinscardLibrary#SCARD_AUTOALLOCATE}. */
  @Test
  void test_scardAutoAllocate() {
    // Note: Microsoft defines SCARD_AUTOALLOCATE as
    //       pub const SCARD_AUTOALLOCATE: c_ulong = 18446744073709551615
    //       see
    // https://docs.rs/smartcard/0.1.0/smartcard/scard/winscard/constant.SCARD_AUTOALLOCATE.html

    assertEquals(
        new BigInteger("18446744073709551615").toString(16),
        String.format("%x", WinscardLibrary.SCARD_AUTOALLOCATE.longValue()));
  } // end method */
} // end class
