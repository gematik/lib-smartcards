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
 * Proxy for generation 2 Smart Cards of type SMC-B.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class IccProxySmcB extends IccUser {

  /** Name says it all. */
  public static final String AID_MF = "D276 0001 4606";

  /** CV-certificate from file "/ MF / EF.C.SMC.AUTD_RPE_CVC.E256". */
  private final Cvc insSmcAutdRpeCvcE256; // */

  /** CV-certificate from file "/ MF / EF.C.SMC.AUTR_CVC.E256". */
  private final Cvc insSmcAurtCvcE256; // */

  /**
   * Constructs an SMC-B proxy.
   *
   * @param ac to which this proxy will connect
   */
  public IccProxySmcB(final ApduLayer ac) {
    super(
        ac,
        0x06, // PrK.SMC.AUTR_CVC.E256
        0x09, // PrK.SMC.AUTD_RPE_CVC.E256
        "91010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.CMS.AES128
        "92010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.CUP.AES128
        "95010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.CMS.AES256
        "96010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.CMS.AES256
        "9f010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX); // Masterkey.PrK.RCA.AdminCMS.CS.E256

    // --- select MF with appropriate AID
    send(new CommandApdu(0x00, 0xA4, 0x04, 0x0C, AID_MF), 0x9000);

    // --- get C.SMC.AUTR_CVC.E256
    insSmcAurtCvcE256 =
        new Cvc(send(new ReadBinary(6, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000).getData());

    // --- get C.SMC.AUTD_RPE_CVC.E256
    insSmcAutdRpeCvcE256 =
        new Cvc(send(new ReadBinary(9, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000).getData());
  } // end constructor */

  @Override
  public String toString() {
    return String.format("cardType            = G2-SMC-B%n") + super.toString();
  } // end method */

  /**
   * Getter.
   *
   * @return CV-certificate from file "/ MF / EF.C.SMC.AUTD_RPE_CVC.E256"
   */
  public Cvc getSmcAutdRpeCvcE256() {
    return insSmcAutdRpeCvcE256;
  } // end method */

  /**
   * Getter.
   *
   * @return CV-certificate from file "/ MF / EF.C.SMC.AUTR_CVC.E256"
   */
  public Cvc getSmcAurtCvcE256() {
    return insSmcAurtCvcE256;
  } // end method */

  /**
   * Retrieves CV-certificate used for role authentication.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_ROLE_AUTHENTICATION
   * elcRoleAuthentication}.
   *
   * @return CV-certificate from file "/ MF / EF.C.SMC.AUTR_CVC.E256"
   * @see IccUser#getCvcRoleAuthentication()
   */
  @Override
  public Cvc getCvcRoleAuthentication() {
    return getSmcAurtCvcE256();
  } // end method */

  /**
   * Retrieves CV-certificate used for session key negotiation.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM
   * ELC_SESSIONKEY_4_SM}.
   *
   * @return CV-certificate from file "/ MF / EF.C.SMC.AUTD_RPE_CVC.E256"
   * @see IccProxy#getCvc4Sm()
   */
  @Override
  public Cvc getCvc4Sm() {
    return getSmcAutdRpeCvcE256();
  } // end method */
} // end class
