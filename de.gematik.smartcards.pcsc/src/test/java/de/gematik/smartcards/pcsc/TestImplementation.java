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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class comparing the implementation of this library to JDK's implementation.
 *
 * <p><b>Assertions:</b>
 *
 * <ol>
 *   <li>none, but for good code coverage at least one IFD with an ICC shall be present
 * </ol>
 *
 * <p><i><b>Note:</b> As of 2024-04-02 the tests in this class succeed if executed by gradle. They
 * fail, if executed by IntelliJ Idea.<br>
 * <b>Observation:</b> The list of card terminals is empty for JDK classes. I don't know why.</i>
 *
 * <pre>
 * ./gradlew --rerun-tasks :de.gematik.smartcards.pcsc:test \
 * --tests "de.gematik.smartcards.pcsc.TestImplementation"
 * </pre>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@EnabledIf("de.gematik.smartcards.pcsc.PcscStatus#isEverythingAvailable")
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestImplementation {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestImplementation.class); // */

  /**
   * Information collected with a provider from this library.
   *
   * <p>Intentionally gather information by this library first.
   */
  private static final InvestigateIfds PROVIDER_AFI; // */

  /** Information collected with a provider from the JDK. */
  private static final InvestigateIfds PROVIDER_JDK; // */

  /*
   * Code block used for setting final class attributes.
   */
  static {
    // --- investigate with provider from this library
    try {
      try (IfdCollection ifdCollection = new IfdCollection()) {
        PROVIDER_AFI = new InvestigateIfds(ifdCollection);
      } // end try-with-resources

      // --- investigate with provider from JDK
      PROVIDER_JDK = new InvestigateIfds(TerminalFactory.getDefault());
    } catch (CardException e) {
      throw new TransmitException(e);
    } // end Catch (...)
  } // end static */

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

  /**
   * Test method comparing the list of all connected terminals.
   *
   * <pre>
   * ./gradlew --rerun-tasks :de.gematik.smartcards.pcsc:test \
   * --tests "de.gematik.smartcards.pcsc.TestImplementation.test_listOfIfd"
   * </pre>
   */
  @Test
  void test_listOfIfd() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check that set of CardTerminal-names are equal
    final Set<String> jdk =
        PROVIDER_JDK.getCardTerminals().stream()
            .map(CardTerminal::getName)
            .collect(Collectors.toSet());
    final Set<String> afi =
        PROVIDER_AFI.getCardTerminals().stream()
            .map(CardTerminal::getName)
            .collect(Collectors.toSet());

    LOGGER.atTrace().log("JDK: {}", jdk);
    LOGGER.atTrace().log("afi: {}", afi);

    assertEquals(jdk, afi);
  } // end method */

  /** Test method comparing the lists of readers without a smart card. */
  @Test
  void test_listOfIfdEmpty() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check that set of CardTerminal-names are equal
    final Set<String> jdk =
        PROVIDER_JDK.getIfdEmpty().stream().map(CardTerminal::getName).collect(Collectors.toSet());
    final Set<String> afi =
        PROVIDER_AFI.getIfdEmpty().stream().map(CardTerminal::getName).collect(Collectors.toSet());

    LOGGER.atTrace().log("JDK: {}", jdk);
    LOGGER.atTrace().log("afi: {}", afi);

    assertEquals(jdk, afi);
  } // end method */

  /** Test method comparing mappings for readers with smart card. */
  @Test
  void test_listOfIfdWithSmartCard() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check that set of CardTerminal-names are equal
    final Set<String> jdk = PROVIDER_JDK.getIccResponses().keySet();
    final Set<String> afi = PROVIDER_AFI.getIccResponses().keySet();

    LOGGER.atTrace().log("JDK: {}", jdk);
    LOGGER.atTrace().log("afi: {}", afi);

    assertEquals(jdk, afi);
  } // end method */

  /** Test method comparing the responses from ICCs. */
  @Test
  void test_IccsResponses() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check that maps with ICCs and their responses are equal
    final Map<String, List<String>> jdk = PROVIDER_JDK.getIccResponses();
    final Map<String, List<String>> afi = PROVIDER_AFI.getIccResponses();

    assertEquals(jdk, afi);
  } // end method */
} // end class
