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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link AfiPcsc}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestAfiPcsc {

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

  /** Test method for {@link AfiPcsc#AfiPcsc()}. */
  @Test
  void test_AfiPcsc() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing,
    //       so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final Provider dut = new AfiPcsc();
    final Set<Provider.Service> services = dut.getServices();
    final Provider.Service afiService = dut.getService("TerminalFactory", "PC/SC");

    assertEquals("de.gematik.smartcards.pcsc.AfiPcsc", AfiPcsc.PROVIDER_NAME);
    assertEquals(AfiPcsc.PROVIDER_NAME, dut.getName());
    assertEquals(AfiPcsc.VERSION, dut.getVersionStr());
    assertEquals(AfiPcsc.INFO, dut.getInfo());
    assertEquals(1, services.size());
    assertNotNull(afiService);
    assertEquals("de.gematik.smartcards.pcsc.IfdFactory", afiService.getClassName());
  } // end method */

  /**
   * Check that version numbers match.
   *
   * <p>The following version numbers are considered:
   *
   * <ol>
   *   <li>{@link AfiPcsc#VERSION}
   *   <li>{@code project.version} from Gradle's build-script
   * </ol>
   *
   * <p>To run just this test-method, use the following shell-command:<br>
   *
   * <pre>
   * ./gradlew --rerun-tasks :de.gematik.smartcards.pcsc:test \
   * --tests "de.gematik.smartcards.pcsc.TestAfiPcsc.test_versionNumbers"
   * </pre>
   */
  @Test
  void test_versionNumbers() {
    // Assertions:
    // ... a. build-script is named "build.gradle.kts"
    // ... b. version number in build-script is on a line starting with "project.version"
    // ... c. version number in build-script is enclosed in quotes

    // Test strategy:
    // --- a. get the version number from build-script
    // --- b. compare it to version number from class AfiPcsc

    final Path base = Path.of("").toAbsolutePath().normalize();

    // --- a. get the version number from build-script
    final Path buildScript = base.resolve("build.gradle.kts");

    assertTrue(base.toString().endsWith("de.gematik.smartcards.pcsc"), base::toString);
    assertTrue(Files.isRegularFile(buildScript), buildScript::toString);

    // --- b. compare it to version number from class AfiPcsc
    try {
      final String line =
          Files.readAllLines(buildScript, StandardCharsets.UTF_8).stream()
              .filter(l -> l.trim().startsWith("project.version"))
              .findFirst()
              .orElseThrow();
      final int firstIndex = line.indexOf('"') + 1;
      final int secondIndex = line.indexOf('"', firstIndex);
      final String buildScriptVersionNumber = line.substring(firstIndex, secondIndex);

      assertTrue(firstIndex > 0, "start quote");
      assertTrue(firstIndex < secondIndex, "end quote");
      assertEquals(AfiPcsc.VERSION, buildScriptVersionNumber, "compare version numbers");
    } catch (IOException | NoSuchElementException e) {
      fail(AfiUtils.UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
