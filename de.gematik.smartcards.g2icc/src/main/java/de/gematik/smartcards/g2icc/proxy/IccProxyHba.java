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
 * Proxy for generation 2 Smart Cards of type HBA.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class IccProxyHba extends IccUser {

  /** Name says it all. */
  public static final String AID_MF = "D276 0001 46  01";

  /** Name says it all. */
  public static final String AID_DF_AUTO = "D276 0001 46  03";

  /** Name says it all. */
  public static final String AID_DF_HPA = "D276 0001 46  02";

  /** Name says it all. */
  public static final String AID_DF_QES = "D276 0000 66  01";

  /** CV-certificate from file "/ MF / EF.C.HPC.AUTD_SUK_CVC.E256". */
  private final Cvc insHpcAutdSukCvcE256;

  /** CV-certificate from file "/ MF / EF.C.HPC.AUTR_CVC.E256". */
  private final Cvc insHpcAutrCvcE256;

  /**
   * Constructs an HBA proxy.
   *
   * @param apduLayer to which this proxy will connect
   */
  public IccProxyHba(final ApduLayer apduLayer) {
    super(
        apduLayer,
        0x06, // PrK.HPC.AUTR_CVC.E256
        0x09, // PrK.HPC.AUTD_SUK_CVC.E256
        "81010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.CMS.AES128
        "82010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.CUP.AES128
        "85010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.CMS.AES256
        "86010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.CMS.AES256
        "8f010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX); // Masterkey.PrK.RCA.AdminCMS.CS.E256

    // --- select MF with appropriate AID
    send(new CommandApdu(0x00, 0xA4, 0x04, 0x0C, AID_MF), 0x9000);

    // --- get C.HPC.AUTR_CVC.E256
    insHpcAutrCvcE256 =
        new Cvc(send(new ReadBinary(6, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000).getData());

    // --- get C.HPC.AUTD_SUK_CVC.E256
    insHpcAutdSukCvcE256 =
        new Cvc(send(new ReadBinary(9, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000).getData());
  } // end constructor */

  @Override
  public String toString() {
    return String.format("cardType            = G2-HBA%n") + super.toString();
  } // end method */

  /**
   * Getter.
   *
   * @return CV-certificate from file "/ MF / EF.C.HPC.AUTD_SUK_CVC.E256"
   */
  public Cvc getHpcAutdSukCvcE256() {
    return insHpcAutdSukCvcE256;
  } // end method */

  /**
   * Getter.
   *
   * @return CV-certificate from file "/ MF / EF.C.HPC.AUTR_CVC.E256"
   */
  public Cvc getHpcAutrCvcE256() {
    return insHpcAutrCvcE256;
  } // end method */

  /**
   * Retrieves CV-certificate used for role authentication.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_ROLE_AUTHENTICATION
   * elcRoleAuthentication}.
   *
   * @return CV-certificate from file "/ MF / EF.C.HPC.AUTR_CVC.E256"
   * @see IccUser#getCvcRoleAuthentication()
   */
  @Override
  public Cvc getCvcRoleAuthentication() {
    return getHpcAutrCvcE256();
  } // end method */

  /**
   * Retrieves CV-certificate used for session key negotiation.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM
   * ELC_SESSIONKEY_4_SM}.
   *
   * @return CV-certificate from file "/ MF / EF.C.HPC.AUTD_SUK_CVC.E256"
   * @see IccProxy#getCvc4Sm()
   */
  @Override
  public Cvc getCvc4Sm() {
    return getHpcAutdSukCvcE256();
  } // end method */
} // end class
