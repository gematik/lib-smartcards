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

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.g2icc.cvc.TrustCenter;
import de.gematik.smartcards.pcsc.Icc;
import de.gematik.smartcards.pcsc.IccChannel;
import de.gematik.smartcards.pcsc.Ifd;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import javax.smartcardio.CardException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Class performing white-box tests for {@link TestIccProxyGsmcKt}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@EnabledIf("de.gematik.smartcards.g2icc.proxy.PcscStatus#isSmartCardResourceManagerRunning")
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestIccProxyGsmcKt extends TestIccProxy {

  /** {@link Ifd} with device-under-test. */
  private static final Ifd IFD; // */

  /** {@link Icc} used for communication. */
  private static final Icc ICC; // */

  /** Basic logical channel of {@link #ICC}. */
  private static final IccChannel BLC; // */

  /*
   * static
   */
  static {
    // --- set path of trust-center to something useful
    assertDoesNotThrow(TrustCenter::initialize);

    IFD = getCardTerminal(IccProxyGsmcKt.class);

    try {
      ICC = (Icc) IFD.connect(T_1);
      BLC = (IccChannel) ICC.getBasicChannel();
    } catch (CardException e) {
      fail(UNEXPECTED, e);
      throw new AssertionError(UNEXPECTED, e); // NOPMD throwing raw exception
    } // end Catch (...)
  } // end static */

  /** Method executed after other tests. */
  @AfterAll
  static void tearDownAfterClass() {
    try {
      ICC.disconnect(true);
    } catch (CardException e) {
      LOGGER.atWarn().log(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Method executed before each test. */
  @BeforeEach
  void setUp() {
    // --- reset the application layer
    ICC.reset();
  } // end method */

  @org.junit.jupiter.api.Disabled("not yet implemented: test_getSmcAutdRpsCvcE256")
  @Test
  void test_getSmcAutdRpsCvcE256() {
    fail("not yet implemented: test_getSmcAutdRpsCvcE256");
  } // end method */

  @org.junit.jupiter.api.Disabled("not yet implemented: test_toString")
  @Test
  void test_toString() {
    fail("not yet implemented: test_toString");
  } // end method */

  @org.junit.jupiter.api.Disabled("not yet implemented: test_getCvcTrustedChannel")
  @Test
  void test_getCvcTrustedChannel() {
    fail("not yet implemented: test_getCvcTrustedChannel");
  } // end method */

  /** Tet method for {@code / DF.KT / PrK.SMKT.AUT.R2048}. */
  @Test
  void test_DfKt_PrkSmktAutR2048() {
    LOGGER.atDebug().log("start test_DfKT_PrkSmktAutR2048");

    // Assertions:
    // ... a. DF.KT is present
    // ... b. X.509 certificate is present in EF.C.SMKT.AUT.XXXX

    // Test strategy:
    // --- a. check that sending command APDU is possible
    // --- b. sign with signPKCS1_V1_5
    // --- c. sign with signPSS

    final int fileIdentifier = 0xc501;
    final int keyReference = 0x82;

    // --- a. check that sending command APDU is possible
    assertEquals(0x9000, BLC.send(new CommandApdu("00 a4 040c")).getSw());

    // --- b. sign with signPKCS1_V1_5
    zzzSignPkcs1V15(BLC, IccProxyGsmcKt.AID_DF_KT, fileIdentifier, keyReference);

    // --- c. sign with signPSS
    zzzSignPss(BLC, IccProxyGsmcKt.AID_DF_KT, fileIdentifier, keyReference);
  } // end method */

  /** Tet method for {@code / DF.KT / PrK.SMKT.AUT.E256}. */
  @Test
  void test_DfKt_PrkSmktAutE256() {
    LOGGER.atDebug().log("start test_DfKT_PrkSmktAutE256");

    // Assertions:
    // ... a. DF.KT is present
    // ... b. X.509 certificate is present in EF.C.SMKT.AUT2.XXXX

    // Test strategy:
    // --- a. check that sending command APDU is possible
    // --- b. sign with ECDSA

    final int fileIdentifier = 0xc504;
    final int keyReference = 0x86;

    // --- a. check that sending command APDU is possible
    assertEquals(0x9000, BLC.send(new CommandApdu("00 a4 040c")).getSw());

    // --- b. sign with ECDSA
    zzzSignEcdsa(BLC, IccProxyGsmcKt.AID_DF_KT, fileIdentifier, keyReference);
  } // end method */
} // end class
