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
 * Artifacts from an unknown source, describing a CV-identity of a gSMC-KT.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class GsmcKt80276883110000107637 {

  /** CV-certificate of Sub-CA from file "EF.CA.E256". */
  public static final Cvc CVC_CA_E256 =
      new Cvc(
          Hex.toByteArray(
              """
                  7f21 81d8
                  |  7f4e 8191
                  |  |  5f29 01 70
                  |  |  42 08 4445475858850218
                  |  |  7f49 4d
                  |  |  |  06 08 2a8648ce3d040302
                  |  |  |  86 41 043cf7e1cb6a5dca9a1901b4d2f750386868b103f527fc31915
                                 4087dced604f2c541c140593be96a216ba763925e4c3c05b21a
                                 316745576ee2705ed61f1474ca93
                  |  |  5f20 08 4445475858110218
                  |  |  7f4c 13
                  |  |  |  06 08 2a8214004c048118
                  |  |  |  53 07 80000000000000
                  |  |  5f25 06 010800030008
                  |  |  5f24 06 020600030007
                  |  5f37 40 8edde30943db3adb81deb2720ddacd05af38538629609e4a3fc9f76
                             e7b85d816762f8ded10363de09147d22acffa2e25b0990fa3ef26db
                             44ded44080f004ef57
                  """)); // */

  /** End-Entity-CVC from file "EF.C...". */
  public static final Cvc CVC_SMC_AUTD_E256 =
      new Cvc(
          Hex.toByteArray(
              """
                  7f21 81da
                  |  7f4e 8193
                  |  |  5f29 01 70
                  |  |  42 08 4445475858110218
                  |  |  7f49 4b
                  |  |  |  06 06 2b2403050301
                  |  |  |  86 41 0403f54df417a6fd3e504cc7291d3a90f12087b50e85987070151
                                 2ca2f2269cfeea3c635950ea0e98048b7641a85d651de942620df
                                 3072173ac563e34ae8d52aa7
                  |  |  5f20 0c 000980276883110000107637
                  |  |  7f4c 13
                  |  |  |  06 08 2a8214004c048118
                  |  |  |  53 07 00000000000000
                  |  |  5f25 06 010900080005
                  |  |  5f24 06 020400080005
                  |  5f37 40 163d3e96bac224a595f073342e4170980c3545cdd065a2a90487525b7
                             cdfac482c3eb1b0666acb17eb19fd61fea59552af47ba09f26dcfd910
                             6c43962cec8871
                  """)); // */

  /** Private key "PrK.HPC.AUTD_SUK_CVC.E256". */
  public static final EcPrivateKeyImpl PRK_SMC_AUTD_RPS_CVC_E256 =
      new EcPrivateKeyImpl(
          new BigInteger("a94093f56fd172f81ad54d0551c7a870e07caa64a441e275230f3ebfa5779376", 16),
          AfiElcParameterSpec.brainpoolP256r1); // */
} // end class
