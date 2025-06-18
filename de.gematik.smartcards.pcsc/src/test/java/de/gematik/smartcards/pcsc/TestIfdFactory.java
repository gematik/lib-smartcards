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
package de.gematik.smartcards.pcsc;

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import javax.smartcardio.TerminalFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Class performing white-box tests for {@link IfdFactory}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestIfdFactory {

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // intentionally empty
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

  /** Test method for {@link IfdFactory#IfdFactory(Object)}. */
  @Test
  void test_IfdFactory__Object() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing,
    //       so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with different input
    for (final Object parameter : new Object[] {null, "foo", 1, BigInteger.ONE}) {
      assertDoesNotThrow(() -> new IfdFactory(parameter)); // NOPMD new in loop
    } // end For (parameter...)
  } // end method */

  /** Test method for {@link IfdFactory#engineTerminals()}. */
  @Test
  @EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isSmartCardResourceManagerRunning")
  void test_engineTerminals() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. check return type of method-under-test
    final IfdFactory dut = new IfdFactory(null);
    final var expected = IfdCollection.class;

    final var actual = dut.engineTerminals().getClass();

    assertEquals(expected, actual);
  } // end method */

  /** Tests for getting an appropriate {@link TerminalFactory}. */
  @EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isPcscProviderAvailable")
  @Test
  void test_getFactory_Default() {
    // Assertions:
    // ... a. The default provider is SunPCSC

    // Test strategy:
    // --- a. use default-provider
    {
      // Note 1: This test might fail on Linux installations, because the
      //         SunPCSC Provider looks at the wrong location for the library, see
      //         https://docs.oracle.com/en/java/javase/17/security/oracle-providers.html
      //         and search on that web-site for "SunPCSC".
      // Note 2: Workaround: Soft-link the PCSC-library from its real place to
      //         the default location the SunPCSC Provider is looking at, e.g.:
      //         root@host:/usr/lib64# ln -s ../lib/x86_64-linux-gnu/libpcsclite.so.1 libpcsclite.so
      final var sun = "SunPCSC"; // Sun's PC/SC provider
      final var expectedSet = Set.of(sun);

      final var dut = TerminalFactory.getDefault();

      final var nameProvider = dut.getProvider().getName();
      assertTrue(expectedSet.contains(nameProvider), nameProvider);
    } // end --- a.
  } // end method */

  /** Tests for getting an appropriate {@link TerminalFactory}. */
  @Test
  @EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isSmartCardResourceManagerRunning")
  void test_getFactory_AfiPcsc() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. use AfiPcsc-provider
    try {
      final TerminalFactory dut = TerminalFactory.getInstance(AfiPcsc.TYPE, null, new AfiPcsc());

      assertEquals(AfiPcsc.PROVIDER_NAME, dut.getProvider().getName());
      assertEquals(AfiPcsc.TYPE, dut.getType());
      assertEquals(IfdCollection.class, dut.terminals().getClass());
    } catch (NoSuchAlgorithmException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
