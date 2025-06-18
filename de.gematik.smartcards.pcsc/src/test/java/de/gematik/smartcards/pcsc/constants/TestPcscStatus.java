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
package de.gematik.smartcards.pcsc.constants;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.gematik.smartcards.pcsc.PcscException;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link PcscStatus}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestPcscStatus {

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

  /** Test method for {@link PcscStatus#check(String, int)}. */
  @Test
  void test_check__String_int() {
    // Assertions:
    // ... a. for each code key in NAME a mapping in DESCRIPTION exists
    // ... b. getExplanation(int)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all relevant errors
    // --- c. unknown error-code

    // --- a. smoke test
    {
      final String message = "fooA";
      final int code = 0x80100001;

      final Throwable throwable =
          assertThrows(PcscException.class, () -> PcscStatus.check(message, code));

      assertEquals(
          "fooA: SCARD_F_INTERNAL_ERROR -> An internal consistency check failed.",
          throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- a.

    // --- b. loop over all relevant errors
    Set.of(
            "", // empty message
            "fooB" // some message
            )
        .forEach(
            message -> {
              PcscStatus.NAME.forEach(
                  (code, name) -> {
                    if (0 == code) {
                      // ... SCARD_S_SUCCESS
                      assertDoesNotThrow(() -> PcscStatus.check(message, code));
                    } else {
                      // ... not SCARD_S_SUCCESS
                      final Throwable throwable =
                          assertThrows(PcscException.class, () -> PcscStatus.check(message, code));

                      assertEquals(
                          String.format("%s: %s", message, PcscStatus.getExplanation(code)),
                          throwable.getMessage());
                      assertNull(throwable.getCause());
                    } // end else
                  }); // end forEach((code, name) -> ...)
            }); // end forEach(message -> ...)

    // --- c. unknown error-code
    {
      final String message = "fooC";
      final int code = 1;

      final Throwable throwable =
          assertThrows(PcscException.class, () -> PcscStatus.check(message, code));

      assertEquals(
          String.format("%s: %s", message, PcscStatus.getExplanation(code)),
          throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- c.
  } // end method */

  /** Test method for {@link PcscStatus#getExplanation(int)}. */
  @Test
  void test_getExplanation__int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all constants
    // --- c. unknown constant

    // --- a. smoke test
    {
      assertEquals(
          "SCARD_F_INTERNAL_ERROR -> An internal consistency check failed.",
          PcscStatus.getExplanation(0x80100001));
    } // end --- a.

    // --- b. loop over all constants
    PcscStatus.NAME.forEach(
        (code, name) -> {
          final String description = PcscStatus.DESCRIPTION.get(code);

          assertEquals(name + " -> " + description, PcscStatus.getExplanation(code));
        }); // end forEach((code, name) -> ...)
    // end --- b.

    // --- c. unknown constant
    {
      assertEquals("no explanation for code = 0x2a = 42", PcscStatus.getExplanation(42));
    } // end --- c.
  } // end method */
} // end class
