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
package de.gematik.smartcards.g2icc;

import de.gematik.smartcards.crypto.AfiElcParameterSpec;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.g2icc.cvc.Cvc;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;

/**
 * Artifacts for PoC PoPP 26.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class PocPopp26 {

  /** CV-certificate of Sub-CA. */
  public static final Cvc CVC_CA_E256 =
      new Cvc(
          Hex.toByteArray(
              """
                  7f21 81d8
                  |  7f4e 8191
                  |  |  5f29 01 70
                  |  |  42 08 4445475858870222
                  |  |  7f49 4d
                  |  |  |  06 08 2a8648ce3d040302
                  |  |  |  86 41 04962f87dfaca7bf6047df6008abe318fe8324fa1509fa5f97edb
                                 f309831c3615f772fff60bdd7e8c65c969f306743130c62476686
                                 7875800208882db7db3c835f
                  |  |  5f20 08 4445475858120223
                  |  |  7f4c 13
                  |  |  |  06 08 2a8214004c048118
                  |  |  |  53 07 80000000000003
                  |  |  5f25 06 020300080001
                  |  |  5f24 06 030100070301
                  |  5f37 40 492a399fd015ece67b9ad696509f66a11b899bafb296f3a93eec61c0c
                             8c195047c23b3ff3a8b8636e27bf0ef5078aa24b64c47e4a165d32037
                             d5b43b61884486
                  """)); // */

  /** End-Entity-CVC. */
  public static final Cvc CVC_EE_E256 =
      new Cvc(
          Hex.toByteArray(
              """
                  7f21 81da
                  |  7f4e 8193
                  |  |  5f29 01 70
                  |  |  42 08 4445475858120223
                  |  |  7f49 4b
                  |  |  |  06 06 2b2403050301
                  |  |  |  86 41 048f3eed7a9475ccc776cdc2d748d4b1a217fed1335132572202f
                                 bfa5d1f12351b443b32408b22461f369620102be4f922d1ac6b4d
                                 174ac12b0fb30276cbf0e24e
                  |  |  5f20 0c 000a80276001011699902101
                  |  |  7f4c 13
                  |  |  |  06 08 2a8214004c048118
                  |  |  |  53 07 00000000000000
                  |  |  5f25 06 020400020208
                  |  |  5f24 06 020900020207
                  |  5f37 40 66011bfecef9439194bd04438f61334ba2db58dc5e5a470a2f6cab58f
                             cb0f8dc21eae8b5e8daa30d82c88b85eb2c236dcc6db4812521be5bf0
                             e61ca4aa056778
                  """)); // */

  /** Private key. */
  public static final EcPrivateKeyImpl PRK_E256 =
      new EcPrivateKeyImpl(
          new BigInteger("6aeee72ee36b166b73e5f6ab027b91e7778949919249d4565df6489a6f587714", 16),
          AfiElcParameterSpec.brainpoolP256r1); // */
} // end class
