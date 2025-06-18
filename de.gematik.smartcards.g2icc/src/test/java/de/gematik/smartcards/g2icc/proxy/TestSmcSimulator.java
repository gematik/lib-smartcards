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

import static de.gematik.smartcards.g2icc.cvc.Cvc.SignatureStatus.SIGNATURE_VALID;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.crypto.AfiElcParameterSpec;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.g2icc.cvc.Cvc;
import de.gematik.smartcards.g2icc.cvc.TrustCenter;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for class {@link SmcSimulator}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"
//         Spotbugs message: This instance method writes to a static field.
//         Rational: This is intentional to disable manual tests.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", // see note 1
}) // */
@EnabledIfSystemProperty(named = "user.name", matches = "alfred")
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
@TestMethodOrder(MethodOrderer.MethodName.class)
final class TestSmcSimulator {
  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestSmcSimulator.class); // */

  /**
   * Flag indicating manual started test methods.
   *
   * <p>The idea behind is as follows:
   *
   * <ol>
   *   <li>If a certain test method is started manually, then this method runs without changing the
   *       code in this class.
   *   <li>If all tests in this class are started, e.g., during a test-suite, then the first
   *       test-method disables all other tests in this class.
   * </ol>
   */
  private static boolean claManualTest = true; // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // --- set path of trust-center to something useful
    assertDoesNotThrow(TrustCenter::initialize);
  } // end method */

  /** Method executed after other tests. */
  @AfterAll
  static void tearDownAfterClass() {
    // intentionally empty
  } // end method */

  /** Method executed before each test. */
  @BeforeEach
  void setUp() {
    // intentionally empty
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /**
   * Returns flag indicating if tests run manually.
   *
   * @return {@code TRUE} if tests are started manually, {@code FALSE} otherwise
   */
  private static boolean isManualTest() {
    return claManualTest;
  } // end method */

  /** Disable test methods when called automatically. */
  @Test
  void test_000_DisableWhenAutomatic() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check the value of the flag
    // --- b. set flag to false
    // --- c. check the value of the flag

    // --- a. check the value of the flag
    assertTrue(isManualTest());

    // --- b. disable manual tests
    claManualTest = false; // ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD

    // --- c. check the value of the flag
    assertFalse(isManualTest());
  } // end method */

  /** Test method for {@link SmcSimulator#performCommand(byte[])}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_performCommand__byteA")
  @Test
  void test_performCommand__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_performCommand__byteA");
  } // end method */

  /** Test method for {@link SmcSimulator#mainLoop(CommandApdu)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_mainLoop__CommandApdu")
  @Test
  void test_mainLoop__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_mainLoop__CommandApdu");
  } // end method */

  /** Test method for {@link SmcSimulator#generalAuthenticateStep1(CommandApdu)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_generalAuthenticateStep1__cmdApdu")
  @Test
  void test_generalAuthenticateStep1__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_generalAuthenticateStep1__CommandApdu");
  } // end method */

  /** Test method for {@link SmcSimulator#generalAuthenticateStep2(CommandApdu)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_generalAuthenticateStep2__cmdApdu")
  @Test
  void test_generalAuthenticateStep2__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_generalAuthenticateStep2__CommandApdu");
  } // end method */

  /** Test method for {@link SmcSimulator#performSecurityOperation(CommandApdu)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_performSecurityOperation__cmdApdu")
  @Test
  void test_performSecurityOperation__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_performSecurityOperation__CommandApdu");
  } // end method */

  /** Test method for {@link SmcSimulator#psoComputeCryptographicChecksum(CommandApdu)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_psoComputeCC__cmdApdu")
  @Test
  void test_psoComputeCryptographicChecksum__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_psoComputeCryptographicChecksum__CommandApdu");
  } // end method */

  /** Test method for {@link SmcSimulator#psoDecipher(CommandApdu)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_psoDecipher__CommandApdu")
  @Test
  void test_psoDecipher__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_psoDecipher__CommandApdu");
  } // end method */

  /** Test method for {@link SmcSimulator#psoEncipher(CommandApdu)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_psoEncipher__CommandApdu")
  @Test
  void test_psoEncipher__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_psoEncipher__CommandApdu");
  } // end method */

  /** Test method for {@link SmcSimulator#psoVerifyCryptographicChecksum(CommandApdu)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_psoVerifyCC__cmdApdu")
  @Test
  void test_psoVerifyCryptographicChecksum__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_psoVerifyCryptographicChecksum__CommandApdu");
  } // end method */

  /** Test method for {@link SmcSimulator#getTime()}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_getTime")
  @Test
  void test_getTime() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_getTime");
  } // end method */

  /** Test method for {@link SmcSimulator#setTime(long, long)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_setTime__long_long")
  @Test
  void test_setTime__long_long() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_setTime__long_long");
  } // end method */

  /** Test method for {@link SmcSimulator#send(CommandApdu)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_send__CommandApdu")
  @Test
  void test_send__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_send__CommandApdu");
  } // end method */

  /** Test method for {@link SmcSimulator#send(byte[])}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: test_send__byteA")
  @Test
  void test_send__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    fail("not (yet) implemented: test_send__byteA");
  } // end method */

  /**
   * Prepare key and certificates.
   *
   * <p>This is not a real test method. Here, we read CV-certificates provided by someone and check
   * and convert the information such, that this information can be used in class {@link
   * SmcSimulator}.
   */
  @EnabledIf("de.gematik.smartcards.g2icc.proxy.TestSmcSimulator#isManualTest")
  @Test
  void test_zm_GetAndCheckCryptoMaterial() {
    // Assertions:
    // - none -

    try {
      // --- define paths
      final var pathDir = Paths.get(System.getProperty("user.home"), "Downloads", "renew");
      final var pathSubCa = pathDir.resolve("DEGXX120223.crt");
      final var pathEeCvc =
          pathDir.resolve("0009_80-276-88311-0000107637_CV-Certificate_renew_resign.der");

      assertTrue(Files.isDirectory(pathDir));
      assertTrue(Files.isRegularFile(pathSubCa));
      assertTrue(Files.isRegularFile(pathEeCvc));

      // --- read Sub-CA-CVC
      final var cvcSubCa = new Cvc(Files.readAllBytes(pathSubCa));
      LOGGER.atInfo().log("CVC-Sub-CA: {}", cvcSubCa);
      assertEquals(SIGNATURE_VALID, cvcSubCa.getSignatureStatus());

      // --- read EE-CVC
      final var cvcEe = new Cvc(Files.readAllBytes(pathEeCvc));
      LOGGER.atInfo().log("CVC-Sub-CA: {}", cvcEe);
      assertEquals(SIGNATURE_VALID, cvcEe.getSignatureStatus());

      // --- check chain
      final var prk =
          new EcPrivateKeyImpl(
              new BigInteger(
                  "a94093f56fd172f81ad54d0551c7a870e07caa64a441e275230f3ebfa5779376", 16),
              AfiElcParameterSpec.brainpoolP256r1);
      assertEquals(prk.getPublicKey(), cvcEe.getPublicKey());
      assertEquals(cvcEe.getCar(), cvcSubCa.getChr());
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
