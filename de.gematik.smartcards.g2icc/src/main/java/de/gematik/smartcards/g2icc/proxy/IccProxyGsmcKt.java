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
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ReadBinary;

/**
 * Proxy for generation 2 Smart Cards of type gSMC-KT.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class IccProxyGsmcKt extends IccSmc {

  /** Name says it all. */
  public static final String AID_MF = "D276 0001 4480 03";

  /** Name says it all. */
  public static final String AID_DF_KT = "D276 0001 4400";

  /** CV-certificate from file "/ MF / EF.C.SMC.AUTD_RPS_CVC.E256". */
  private final Cvc insSmcAutdRpsCvcE256;

  /**
   * Constructs a gSMC-KT proxy.
   *
   * <p><i>Note: it is assumed that a connection to an appropriate ICC is established.</i>
   *
   * @param apduLayer to which this proxy will connect
   */
  public IccProxyGsmcKt(final ApduLayer apduLayer) {
    super(
        apduLayer,
        0x0a, // PrK.SMC.AUTD_RPS_CVC.E256
        0x0a, // PrK.SMC.AUTD_RPS_CVC.E256
        "b1010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.CMS.AES128
        "b2010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.CUP.AES128
        "b5010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.CMS.AES256
        "b6010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.CMS.AES256
        "bf010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX); // Masterkey.PrK.RCA.AdminCMS.CS.E256

    // --- select MF with appropriate AID
    send(new CommandApdu(0x00, 0xA4, 0x04, 0x0C, AID_MF), 0x9000);

    // --- get C.SMC.AUTD_RPS_CVC.E256
    insSmcAutdRpsCvcE256 =
        new Cvc(send(new ReadBinary(10, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000).getData());
  } // end constructor */

  /**
   * Getter.
   *
   * @return CV-certificate from file "/ MF / EF.C.SMC.AUTD_RPS_CVC.E256"
   */
  public Cvc getSmcAutdRpsCvcE256() {
    return insSmcAutdRpsCvcE256;
  } // end method */

  @Override
  public String toString() {
    return String.format("cardType            = G2-gSMC-KT%n") + super.toString();
  } // end method */

  /**
   * Retrieves CV-certificate used for session key negotiation.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM
   * ELC_SESSIONKEY_4_SM}.
   *
   * @return CV-certificate used for session key negotiation
   */
  @Override
  public Cvc getCvc4Sm() {
    return getSmcAutdRpsCvcE256();
  } // end method */

  /**
   * Retrieves CV-certificate used for session key negotiation.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_TC}.
   *
   * @return CV-certificate used for role-authentication
   */
  @Override
  public Cvc getCvc4Tc() {
    return getSmcAutdRpsCvcE256();
  } // end method */
} // end class
