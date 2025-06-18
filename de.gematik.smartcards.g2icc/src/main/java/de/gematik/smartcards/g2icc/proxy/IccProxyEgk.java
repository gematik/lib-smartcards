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
 * Proxy for generation 2 Smart Cards of type eGK.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class IccProxyEgk extends IccUser {

  /** Name says it all. */
  public static final String AID_MF = "D276 0001 44  80 00"; // */

  /** Name says it all. */
  public static final String AID_DF_AMTS = "D276 0001 44  0C"; // */

  /** Name says it all. */
  public static final String AID_DF_HCA = "D276 0000 01  02"; // */

  /** Name says it all. */
  public static final String AID_DF_OSE = "D276 0001 44  0B"; // */

  /** CV-certificate from file "/ MF / EF.C.eGK.AUT_CVC.E256". */
  private final Cvc insEgkAutCvcE256; // */

  /**
   * Constructs an eGK proxy.
   *
   * @param apduLayer to which this proxy will connect
   */
  /* package */ IccProxyEgk(final ApduLayer apduLayer) {
    super(
        apduLayer,
        0x09, // PrK.eGK.AUT_CVC.E256
        0x09, // PrK.eGK.AUT_CVC.E256
        "01010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.CMS.AES128
        "07010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.VSD.AES128
        "05010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.CMS.AES256
        "0b010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.VSD.AES256
        "0f010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX); // Masterkey.PrK.RCA.AdminCMS.CS.E256

    // --- select MF with appropriate AID
    send(new CommandApdu(0x00, 0xA4, 0x04, 0x0C, AID_MF), 0x9000);

    // --- get C.eGK.AUT_CVC.E256
    insEgkAutCvcE256 =
        new Cvc(
            send(
                    new ReadBinary(6, 0, CommandApdu.NE_EXTENDED_WILDCARD),
                    0x9000 // expected trailer
                    )
                .getData());
  } // end constructor */

  /**
   * Getter.
   *
   * @return content of / MF / EF.C.eGK.AUT_CVC.E256
   */
  public Cvc getEgkAutCvcE256() {
    return insEgkAutCvcE256;
  } // end method */

  @Override
  public String toString() {
    return String.format("cardType            = G2-eGK%n") + super.toString();
  } // end method */

  /**
   * Retrieves CV-certificate used for role authentication.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_ROLE_AUTHENTICATION
   * elcRoleAuthentication}.
   *
   * @return here the CV-certificate from file "/ MF / EF.C.eGK.AUT_CVC.E256" is retrieved
   * @see IccUser#getCvcRoleAuthentication()
   */
  @Override
  public Cvc getCvcRoleAuthentication() {
    return getEgkAutCvcE256();
  } // end method */

  /**
   * Retrieves CV-certificate used for session key negotiation.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM
   * ELC_SESSIONKEY_4_SM}.
   *
   * @return here the CV-certificate from file "/ MF / EF.C.eGK.AUT_CVC.E256" is retrieved
   * @see IccProxy#getCvc4Sm()
   */
  @Override
  public Cvc getCvc4Sm() {
    return getEgkAutCvcE256();
  } // end method */
} // end class
