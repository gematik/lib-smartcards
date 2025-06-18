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
package de.gematik.smartcards.sdcom.isoiec7816objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import de.gematik.smartcards.utils.AfiUtils;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for white box testing {@link LifeCycleStatus}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestLifeCycleStatus {

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

  @Test
  void test_getInstance__int() {
    // Test strategy:
    // --- a. smoke test
    // --- b. check all one byte values
    // --- c. check that only the 8 LSBit are considered

    // --- a. smoke test
    assertEquals(LifeCycleStatus.RFU, LifeCycleStatus.getInstance(2));

    // --- b. check all one byte values
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            lcsByte -> {
              final LifeCycleStatus lcs = LifeCycleStatus.getInstance(lcsByte);

              if (lcsByte > 0xf) { // NOPMD literal in if statement
                // ... lcsByte with bits b8 to b5 not all zero
                assertEquals(LifeCycleStatus.PROPRIETARY, lcs);
              } else {
                // ... lcsByte with bits b8 to b5 all zero
                // spotless:off
                final LifeCycleStatus expected = switch (lcsByte) {
                  case 0 -> LifeCycleStatus.NO_INFORMATION_GIVEN;
                  case 1 -> LifeCycleStatus.CREATION;
                  case 3 -> LifeCycleStatus.INITIALISATION;
                  case 4, 6 -> LifeCycleStatus.DEACTIVATED;
                  case 5, 7 -> LifeCycleStatus.ACTIVATED;
                  case 12, 13, 14, 15 -> LifeCycleStatus.TERMINATED;
                  default -> LifeCycleStatus.RFU;
                };
                // spotless:on

                assertEquals(expected, lcs);
              } // end else
            }); // end forEach(lcs -> ...)
    // end --- a.

    // --- b. check that only the 8 LSBit are considered
    AfiUtils.SPECIAL_INT.forEach(
        lcsByte ->
            assertSame(
                LifeCycleStatus.getInstance(lcsByte & 0xff),
                LifeCycleStatus.getInstance(lcsByte),
                () -> Integer.toString(lcsByte, 16)));
  } // end method */

  /** Test method for {@link EafiIccProtocol#toString()}. */
  @Test
  void test_toString() {
    // Note 1: Simple method does not need extensive testing, so we can be lazy
    //         here.
    // Note 2: Expected result is better manually checked. Thus, here are just
    //         some smoke test.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all values

    // --- a. smoke test
    assertEquals("proprietary", LifeCycleStatus.getInstance(0x10).toString());

    // --- b. loop over all values
    Arrays.stream(LifeCycleStatus.values())
        .forEach(lcs -> assertFalse(lcs.toString().isEmpty(), lcs::name));
  } // end method */
} // end class
