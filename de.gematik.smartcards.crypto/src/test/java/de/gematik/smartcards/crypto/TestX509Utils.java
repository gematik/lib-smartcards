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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.smartcards.utils.Hex;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link X509Utils}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestX509Utils {

  /** {@link X509Certificate} content of an SMC-B in encoded form. */
  private static final byte[] SMC_B_X509_ENCODED =
      Hex.toByteArray(
          """
              30 8203aa
              |  30 820350
              |  |  a0 03
              |  |  |  02 01 02
              |  |  02 07 0321ae752dcbb8
              |  |  30 0a
              |  |  |  06 08 2a8648ce3d040302
              |  |  30 819a
              |  |  |  31 0b
              |  |  |  |  30 09
              |  |  |  |  |  06 03 550406
              |  |  |  |  |  13 02 4445
              |  |  |  31 1f
              |  |  |  |  30 1d
              |  |  |  |  |  06 03 55040a
              |  |  |  |  |  0c 16 67656d6174696b20476d6248204e4f542d56414c4944
              |  |  |  31 48
              |  |  |  |  30 46
              |  |  |  |  |  06 03 55040b
              |  |  |  |  |  0c 3f 496e737469747574696f6e2064657320476573756e646865697473776573656
                                   e732d4341206465722054656c656d6174696b696e667261737472756b747572
              |  |  |  31 20
              |  |  |  |  30 1e
              |  |  |  |  |  06 03 550403
              |  |  |  |  |  0c 17 47454d2e534d43422d4341353120544553542d4f4e4c59
              |  |  30 1e
              |  |  |  17 0d 3233313032373030303030305a
              |  |  |  17 0d 3238313032363233353935395a
              |  |  30 8195
              |  |  |  31 0b
              |  |  |  |  30 09
              |  |  |  |  |  06 03 550406
              |  |  |  |  |  13 02 4445
              |  |  |  31 2b
              |  |  |  |  30 29
              |  |  |  |  |  06 03 55040a
              |  |  |  |  |  0c 22 322d534d432d422d546573746b617274652d2d383833313130303030313532373235
              |  |  |  31 20
              |  |  |  |  30 1e
              |  |  |  |  |  06 03 550405
              |  |  |  |  |  13 17 30302e3830323736383833313130303030313532373235
              |  |  |  31 37
              |  |  |  |  30 35
              |  |  |  |  |  06 03 550403
              |  |  |  |  |  0c 2e 5a61686e61727a74707261786973204e6f726265727420
                                   47726166204cc3bc6e6562757267544553542d4f4e4c59
              |  |  30 5a
              |  |  |  30 14
              |  |  |  |  06 07 2a8648ce3d0201
              |  |  |  |  06 09 2b2403030208010107
              |  |  |  03 42 00041665844f67d0b91ed2e6552a54ea77d5a11f06122630bed12ffb660facf82a7
                               75803c0285a1e2e4a45c8d474bb82e32c9a7a2852f9cea772d0f1910292d81bdf
              |  |  a3 820181
              |  |  |  30 82017d
              |  |  |  |  30 0c
              |  |  |  |  |  06 03 551d13
              |  |  |  |  |  01 01 ff
              |  |  |  |  |  04 02 3000
              |  |  |  |  30 13
              |  |  |  |  |  06 03 551d25
              |  |  |  |  |  04 0c 300a06082b06010505070302
              |  |  |  |  30 38
              |  |  |  |  |  06 08 2b06010505070101
              |  |  |  |  |  04 2c 302a302806082b06010505073001861c687474703a2f
                                   2f656863612e67656d6174696b2e64652f6f6373702f
              |  |  |  |  30 1f
              |  |  |  |  |  06 03 551d23
              |  |  |  |  |  04 18 301680140698e90255ffc99f5ca3650ef15de220f584fb93
              |  |  |  |  30 1d
              |  |  |  |  |  06 03 551d0e
              |  |  |  |  |  04 16 04148b2a63d76772652deb5abc8688f942e9fd77690b
              |  |  |  |  30 0e
              |  |  |  |  |  06 03 551d0f
              |  |  |  |  |  01 01 ff
              |  |  |  |  |  04 04 03020780
              |  |  |  |  30 20
              |  |  |  |  |  06 03 551d20
              |  |  |  |  |  04 19 3017300a06082a8214004c048123300906072a8214004c044d
              |  |  |  |  30 2c
              |  |  |  |  |  06 03 551d1f
              |  |  |  |  |  04 25 30233021a01fa01d861b687474703a2f2f656
                                   863612e67656d6174696b2e64652f63726c2f
              |  |  |  |  30 7e
              |  |  |  |  |  06 05 2b24080303
              |  |  |  |  |  04 75 3073a4283026310b300906035504061302444531173015060355040a0c0e67656d
                                   6174696b204265726c696e304730453043304130100c0e5a61686e61727a747072
                                   61786973300906072a8214004c04331322322d534d432d422d546573746b617274
                                   652d2d383833313130303030313532373235
              |  30 0a
              |  |  06 08 2a8648ce3d040302
              |  03 48 003045022100a355f4006c7a0d30c60a15615635fbacb95a90cff6f643a2f762a745ecbc5
                         648022004a56df6f2dd5a775d4821614f20622aa65fb73ab947656b02003cdb14717732
              """); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // intentionally empty
  } // end method */

  /** Method executed after other tests. */
  @AfterAll
  static void tearDownAfterClass() {
    // intentionally empty
  } // end method */

  /** Method executed before each test. */
  @BeforeEach
  void setUp() {
    // intentionally empty
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /** Test method for {@link X509Utils#getFactory(String)}. */
  @Test
  void getFactory() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. invalid type

    // --- a. smoke test
    assertDoesNotThrow(() -> X509Utils.getFactory("X.509"));

    // --- b. invalid type
    assertThrows(IllegalArgumentException.class, () -> X509Utils.getFactory("FooBar"));
  } // end method */

  /** Test method for {@link X509Utils#generateCertificate(byte[])}. */
  @Test
  void test_generateCertificate__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: invalid input

    // --- a. smoke test
    {
      assertDoesNotThrow(() -> X509Utils.generateCertificate(SMC_B_X509_ENCODED));
    } // end --- a.

    // --- b. ERROR: invalid input
    {
      final var input = Hex.toByteArray("30 00");

      assertThrows(IllegalArgumentException.class, () -> X509Utils.generateCertificate(input));
    } // end --- b.
  } // end method */
} // end class
