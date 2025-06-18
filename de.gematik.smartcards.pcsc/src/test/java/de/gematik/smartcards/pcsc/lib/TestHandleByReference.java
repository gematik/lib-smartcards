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
package de.gematik.smartcards.pcsc.lib;

import static de.gematik.smartcards.pcsc.lib.TestHandle.SIZE_BITS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import java.math.BigInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link HandleByReference}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestHandleByReference {

  /** Random number generator. */
  private static final AfiRng RNG = new AfiRng(); // */

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

  /** Test method for {@link HandleByReference#HandleByReference()}. */
  @Test
  void test_HandleByReference() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing,
    //       so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    assertDoesNotThrow(HandleByReference::new);
  } // end method */

  /** Test method for {@link HandleByReference#getLong()}. */
  @Test
  void test_getLong() {
    // Assertions:
    // ... a. setLong(long)-method works as expected

    // Note: More values are tested in test_setLong(long)-method, see there.

    // Test strategy:
    // --- a. smoke test
    final HandleByReference dut = new HandleByReference();
    final int input = 42;

    dut.setLong(input);

    assertEquals(input, dut.getLong());
  } // end method */

  /** Test method for {@link HandleByReference#setLong(long)}. */
  @Test
  void test_setLong() {
    // Assertions:
    // ... a. getLong()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. check with special long values
    // --- c. check with random values

    final HandleByReference dut = new HandleByReference();

    // --- a. smoke test
    dut.setLong(42);

    assertEquals(42L, dut.getLong());

    // --- b. check with special long values
    AfiUtils.SPECIAL_LONG.forEach(
        n -> {
          dut.setLong(n);

          assertEquals(n, dut.getLong());
        }); // end forEach(n -> ...)
    // end --- b.

    // --- c. check with random values
    IntStream.range(0, 255)
        .forEach(
            i -> {
              final long n = new BigInteger(SIZE_BITS, RNG).longValue();

              dut.setLong(n);

              assertEquals(n, dut.getLong());
            }); // end forEach(i -> ...)
  } // end method */
} // end class
