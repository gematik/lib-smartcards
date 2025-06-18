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
package de.gematik.smartcards.g2icc.cos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link EafiCosAlgId}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions"})
final class TestEafiCosAlgId {

  /** Test method for {@link EafiCosAlgId#getAlgId()}. */
  @Test
  void test_getAlgId() {
    // Note: Simple class and simple method does not need extensive testing.
    //       So we can be lazy here and concentrate on code coverage.

    // Test strategy:
    // --- a. check all values against [gemSpec_COS#16.1]
    // a.1 [gemSpec_COS#Tab.268], authentication
    // a.3 [gemSpec_COS#Tab.268], encipher / decipher
    // a.2 [gemSpec_COS#Tab.268], signature

    // a.1 [gemSpec_COS#Tab.268], authentication
    assertEquals("54", EafiCosAlgId.AES_SESSIONKEY_4_SM.getAlgId());
    assertEquals("74", EafiCosAlgId.AES_SESSIONKEY_4_TC.getAlgId());
    assertEquals("F4", EafiCosAlgId.ELC_ASYNCHRON_ADMIN.getAlgId());
    assertEquals("00", EafiCosAlgId.ELC_ROLE_AUTHENTICATION.getAlgId());
    assertEquals("00", EafiCosAlgId.ELC_ROLE_CHECK.getAlgId());
    assertEquals("54", EafiCosAlgId.ELC_SESSIONKEY_4_SM.getAlgId());
    assertEquals("D4", EafiCosAlgId.ELC_SESSIONKEY_4_TC.getAlgId());
    assertEquals("05", EafiCosAlgId.RSA_CLIENT_AUTHENTICATION.getAlgId());

    // a.3 [gemSpec_COS#Tab.268], encipher / decipher
    assertEquals("85", EafiCosAlgId.RSA_DECIPHER_OAEP.getAlgId());
    assertEquals("05", EafiCosAlgId.RSA_ENCIPHER_OAEP.getAlgId());
    assertEquals("0B", EafiCosAlgId.ELC_SHARED_SECRET_CALCULATION.getAlgId());

    // a.2 [gemSpec_COS#Tab.268], signature
    assertEquals("07", EafiCosAlgId.SIGN_9796_2_DS_2.getAlgId());
    assertEquals("02", EafiCosAlgId.SIGN_PKCS_1_V_1_5.getAlgId());
    assertEquals("05", EafiCosAlgId.SIGN_PSS.getAlgId());
    assertEquals("00", EafiCosAlgId.SIGN_ECDSA.getAlgId());
  } // end method */
} // end class
