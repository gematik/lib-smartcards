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

import de.gematik.smartcards.g2icc.cos.EafiCosAlgId;
import de.gematik.smartcards.g2icc.cvc.Cvc;
import de.gematik.smartcards.sdcom.apdu.ApduLayer;
import java.util.NoSuchElementException;

/**
 * Subset of {@link IccProxy} for G2 cards used as security module card.
 *
 * <p>Directly known subclasses: {@link IccProxyGsmcK}, {@link IccProxyGsmcKt}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
public abstract class IccSmc extends IccProxy {

  /**
   * Key reference of a private key used for authentication.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM
   * elcSessionkey4TC}.
   */
  private final int insPrk4Tc; // */

  /**
   * Comfort constructor.
   *
   * @param apduLayer used for smart card communication
   * @param prk4Sm key reference of the private key supporting {@link
   *     EafiCosAlgId#ELC_SESSIONKEY_4_SM ELC_SESSIONKEY_4_SM}
   * @param prk4Tc key reference of the private key supporting {@link
   *     EafiCosAlgId#ELC_SESSIONKEY_4_SM elcSessionkey4TC}
   * @param cmsMasterkey128 masterkey used for deriving card individual keys
   * @param cupMasterkey128 masterkey used for deriving card individual keys
   * @param cmsMasterkey256 masterkey used for deriving card individual keys
   * @param cupMasterkey256 masterkey used for deriving card individual keys
   * @param adminCmsMasterkeyElc256 masterkey used for deriving card individual keys
   */
  protected IccSmc(
      final ApduLayer apduLayer,
      final int prk4Sm,
      final int prk4Tc,
      final String cmsMasterkey128,
      final String cupMasterkey128,
      final String cmsMasterkey256,
      final String cupMasterkey256,
      final String adminCmsMasterkeyElc256) {
    // Note 1: SonarQube claims the following major finding:
    //         "Constructor has 8 parameters, which is greater than 7 authorized."
    // Note 2: This will NOT be fixed.
    //         This class needs eight parameters.
    //         I will not combine parameters to just satisfy SonarQube.
    super(
        apduLayer,
        prk4Sm,
        cmsMasterkey128,
        cupMasterkey128,
        cmsMasterkey256,
        cupMasterkey256,
        adminCmsMasterkeyElc256);

    insPrk4Tc = prk4Tc;
  } // end constructor */

  /**
   * Retrieves CV-certificate used for session key negotiation.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_TC}.
   *
   * @return CV-certificate used for role-authentication
   * @throws NoSuchElementException if an appropriate {@link Cvc} is absent
   */
  public abstract Cvc getCvc4Tc(); // */

  /**
   * Returns key reference of a private key.
   *
   * <p>The private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM elcSessionkey4TC}.
   *
   * @return key reference of the private key used for CV-certificate authentication
   */
  /* package */ int getPrk4Tc() {
    return insPrk4Tc;
  } // end method */
} // end class
