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
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.util.NoSuchElementException;

/**
 * Proxy for generation 2 Smart Cards of type gSMC-K.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class IccProxyGsmcK extends IccSmc {

  /** Name says it all. */
  public static final String AID_DF_AK = "D276 0001 4402";

  /** Name says it all. */
  public static final String AID_DF_NK = "D276 0001 4403";

  /** Name says it all. */
  public static final String AID_DF_SAK = "D276 0001 4404";

  /** Name says it all. */
  public static final String AID_DF_SI_AN = "D276 0001 4405"; // */

  /** Name says it all. */
  public static final String AID_MF = "D276 0001 4480 01"; // */

  /** CV-certificate from file "/ MF / DF.SAK / EF.C.SAK.AUTD_CVC.E256". */
  private final Cvc insSakAutdSukCvcE256; // */

  /** Content from file "/ MF / EF.C.SMC.AUT_CVC.E256", possibly empty. */
  private final byte[] insSmcAutCvcE256; // */

  /**
   * Constructs a gSMC-K proxy.
   *
   * <p><i>Note: it is assumed that a connection to an appropriate ICC is established.</i>
   *
   * @param apduLayer to which this proxy will connect
   */
  public IccProxyGsmcK(final ApduLayer apduLayer) {
    super(
        apduLayer,
        0x05, // PrK.SMC.AUT_CVC.E256
        0x8a, // PrK.SAK.AUTD_CVC.E256
        "a1010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.CMS.AES128
        "a2010203 04050607 08090a0b 0c0d0e0f", // Masterkey.SK.CUP.AES128
        "a5010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.CMS.AES256
        "a6010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX, // Masterkey.SK.CMS.AES256
        "af010203 04050607 08090a0b 0c0d0e0f" + KEY_SUFFIX); // Masterkey.PrK.RCA.AdminCMS.CS.E256

    // --- select MF with appropriate AID
    send(new CommandApdu(0x00, 0xA4, 0x04, 0x0C, AID_MF), 0x9000);

    // --- get C.SMC.AUT_CVC.E256
    final var rsp =
        send(
            new ReadBinary(10, 0, CommandApdu.NE_EXTENDED_WILDCARD),
            0x9000, // NoError
            0x6b00); // OffsetTooBig, i.e. not personalized
    insSmcAutCvcE256 = (0x9000 == rsp.getTrailer()) ? rsp.getData() : AfiUtils.EMPTY_OS;

    // --- get C.SAK.AUTD_SUK_CVC.E256 from DF.SAK
    send(new CommandApdu(0x00, 0xA4, 0x04, 0x0C, AID_DF_SAK), 0x9000);

    // Note: "oldContent" and "newContent" both contain a CV-certificate for the
    //       same public key, but with different CED and CXD.
    final var oldContent =
        Hex.extractHexDigits(
            """
                7f21 81da
                |    7f4e 8193
                |    |    5f29 01 70
                |    |    42 08 4445415258110214
                |    |    7f49 4b
                |    |    |    06 06 2b2403050301
                |    |    |    86 41 04782bf795f3defc73abe5daf357cafcb181bfd4c155eee788aa33aace6267f1a1
                                       5858f21836b4edb144fe82e23732f6bf835dfd9a17056b43b5ca6edb6e991e55
                |    |    5f20 0c 000a80276883110000015234
                |    |    7f4c 13
                |    |    |    06 08 2a8214004c048118
                |    |    |    53 07 00000000000001
                |    |    5f25 06 010500070301
                |    |    5f24 06 020000070207
                |    5f37 40 a56cdcb5f575a34c1654d448932eba09923b7dffcc36cd0ef2260b739865ae5d
                             5f1891b5daaa8a03899500f2276858b94fc6afb5873d610f6b0513e2da90b946
                """);
    final var newContent =
        Hex.extractHexDigits(
            """
                7f21 81da
                |    7f4e 8193
                |    |    5f29 01 70
                |    |    42 08 4445475858120223
                |    |    7f49 4b
                |    |    |    06 06 2b2403050301
                |    |    |    86 41 04782bf795f3defc73abe5daf357cafcb181bfd4c155eee788aa33aace6267f1a1
                                       5858f21836b4edb144fe82e23732f6bf835dfd9a17056b43b5ca6edb6e991e55
                |    |    5f20 0c 000a80276883110000015234
                |    |    7f4c 13
                |    |    |    06 08 2a8214004c048118
                |    |    |    53 07 00000000000001
                |    |    5f25 06 020401000204
                |    |    5f24 06 020901000203
                |    5f37 40 98f8ef4d34de70fa2fc6761c7db7e5a06909d104cef237b0c081028867f665f3
                             a25e35c708c15e09891603c83f70f57dc38cda2e44a104d198ec9a7452d56c31
                """);
    final var presentContent =
        Hex.toHexDigits(
            send(new ReadBinary(10, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000).getData());

    insSakAutdSukCvcE256 =
        new Cvc(Hex.toByteArray(oldContent.equals(presentContent) ? newContent : presentContent));
  } // end constructor */

  /**
   * Getter.
   *
   * @return CV-certificate from file "/ MF / DF.SAK / EF.C.SAK.AUTD_CVC.E256"
   */
  public Cvc getSakAutdSukCvcE256() {
    return insSakAutdSukCvcE256;
  } // end method */

  /**
   * Getter.
   *
   * <p><i><b>Note:</b> It is possible that the corresponding file is empty.</i>
   *
   * @return Cv-certificate from file "/ MF / EF.C.SMC.AUT_CVC.E256"
   * @throws NoSuchElementException if the corresponding file does not contain a CV-certificate
   */
  public Cvc getSmcAutCvcE256() {
    if (0 == insSmcAutCvcE256.length) {
      throw new NoSuchElementException("no CV-certificate");
    } // end fi

    return new Cvc(insSmcAutCvcE256);
  } // end method */

  @Override
  public String toString() {
    return String.format("cardType            = G2-gSMC-K%n") + super.toString();
  } // end method */

  /**
   * Retrieves CV-certificate used for session key negotiation.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_SM
   * ELC_SESSIONKEY_4_SM}.
   *
   * @return CV-certificate used for session key negotiation
   * @throws NoSuchElementException if an appropriate {@link Cvc} is absent
   */
  @Override
  public Cvc getCvc4Sm() {
    return getSmcAutCvcE256();
  } // end method */

  /**
   * Retrieves CV-certificate used for session key negotiation.
   *
   * <p>The corresponding private key supports at least {@link EafiCosAlgId#ELC_SESSIONKEY_4_TC}.
   *
   * @return CV-certificate used for role-authentication
   * @throws NoSuchElementException if an appropriate {@link Cvc} is absent
   */
  @Override
  public Cvc getCvc4Tc() {
    return getSakAutdSukCvcE256();
  } // end method */
} // end class
