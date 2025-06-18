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
 * Artifacts from XMl-file "HBA_80276883110000220885_gema5.xml".
 *
 * <p><i><b>Note:</b> Currently the TrustCenter-database does not contain the CVC-Root key for the
 * CV-certificates in this class.</i>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.DataClass"})
public final class Hba80276883110000220885 {

  /** CV-certificate of Sub-CA from file "EF.C.CA_HPC.CS.E256". */
  public static final Cvc CVC_CA_E256 =
      new Cvc(
          Hex.toByteArray(
              """
                  7F2181D87F4E81915F290170420844454758588002207F494D06082A8648CE3D040302
                  8641047E273E2A297A70E9A86C80F81E82969E8D263212E02B4CF811217C2DA0391D82
                  8C4E69018141B866A42A7DA55962D4776750109D6A1D8D4E47052619CAD2225C5F2008
                  44454758581102207F4C1306082A8214004C048118530780FF7BDFE1FF0C5F25060200
                  000402035F24060208000402035F374043041DB4AEE312074CDF1B53D5A652277DFA8D
                  CE44818BE0E099EF97E985FF2883E6603B165E2EA52671873AFC449469647E4BCB931F
                  906C7069FA6CA99E7FBF
                  """)); // */

  /** End-Entity-CVC from file "EF.C.HPC.AUTD_SUK_CVC.E256". */
  public static final Cvc CVC_HPC_AUTD_SUK_CVC_E256 =
      new Cvc(
          Hex.toByteArray(
              """
                  7F2181DA7F4E81935F290170420844454758581102207F494B06062B24030503018641
                  043870C37249CA234D7BF81197E7DF27CE341AA575B22F4771F162483BE31745839ED4
                  B75816E10A4D41BFC3D1C03C1B81981773B692332373E17C82828377A0B25F200C0009
                  802768831100002208857F4C1306082A8214004C04811853070000000000000C5F2506
                  0204000300045F24060204000300085F37408DD2EA7850CE3314CC71092BB9C6C81C72
                  8F1FBEB8F5F68EE82E929BA02F9D40439830AB31E95FEC376B6B6F90A2FCE92133F1C5
                  343E7AC98151181E2C940250
                  """)); // */

  /** Private key "PrK.HPC.AUTD_SUK_CVC.E256". */
  public static final EcPrivateKeyImpl PRK_HPC_AUTD_SUK_CVC_E256 =
      new EcPrivateKeyImpl(
          new BigInteger("040D4148EA43CA215AF44B60B24151DC0C4B6858C9E8628F3175E78400A29E8B", 16),
          AfiElcParameterSpec.brainpoolP256r1); // */

  /** End-Entity-CVC from file "EF.C.HPC.AUTR_CVC.E256". */
  public static final Cvc CVC_HPC_AUTR_CVC_E256 =
      new Cvc(
          Hex.toByteArray(
              """
                  7F2181DA7F4E81935F290170420844454758581102207F494B06062B24030503018641
                  0411E6E8032E36DC3871B443372E33B75D6E727C92E4727FAF7ED5E4EDB41E04CC42C1
                  19633EBB870BA4AEC0BAF313341917700935466921448CD2DA2A1B42E4045F200C0006
                  802768831100002208857F4C1306082A8214004C0481185307005D20DAA083005F2506
                  0204000300045F24060204000300085F374041D90470DCAA89B95E9465341A6405052E
                  01E3124397582995CC73C63BE3BF5902D087D9430A057B857514CE6118B234A33CC792
                  729BD3A4FB01AAEA022E8010
                  """)); // */

  /** Private key "PrK.HPC.AUTR_CVC.E256". */
  public static final EcPrivateKeyImpl PRK_HPC_AUTR_CVC_E256 =
      new EcPrivateKeyImpl(
          new BigInteger("0AC9D3A22368381331ADDFEB263DEC9970141C8545AC0ED5196E676C02C8B3B7", 16),
          AfiElcParameterSpec.brainpoolP256r1); // */
} // end class
