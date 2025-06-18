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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
 * Class performing white-box tests for {@link ScardHandle}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestScardHandle {

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

  /** Test method for {@link ScardHandle#ScardHandle()}. */
  @Test
  void test_ScardHandle() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    final ScardHandle dut = new ScardHandle();

    assertEquals(0, dut.longValue());
  } // end method */

  /** Test method for {@link ScardHandle#ScardHandle(long)}. */
  @Test
  void test_ScardHandle__long() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. check with special long values
    // --- c. check with random values

    // --- a. smoke test
    assertEquals(42L, new ScardHandle(42).longValue());

    // --- b. check with special long values
    AfiUtils.SPECIAL_LONG.forEach(
        n -> {
          final int bitLength = BigInteger.valueOf(n).bitLength();
          if ((bitLength < SIZE_BITS) || ((bitLength == SIZE_BITS) && (n > 0))) {
            // ... Dword has enough capacity for i
            assertEquals(n, new ScardHandle(n).longValue());
          } else {
            // ... in this environment i has too many bits for a Dword
            assertThrows(IllegalArgumentException.class, () -> new ScardHandle(n));
          } // end fi
        }); // end forEach(n -> ...)
    // end --- b.

    // --- c. check with random values
    IntStream.range(0, 255)
        .forEach(
            i -> {
              final long n = new BigInteger(SIZE_BITS, RNG).longValue();

              assertEquals(n, new ScardHandle(n).longValue());
            }); // end forEach(i -> ...)
  } // end method */

  /** Test method for {@link ScardHandle#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // - none -

    // Note: This simple method form superclass does not need extensive testing,
    //       so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. check with special long values
    // --- c. check with random values

    // --- a. smoke test
    assertEquals("ScardHandle{ffffffffffffffd6}", new ScardHandle(-42).toString());

    // --- b. check with special long values
    AfiUtils.SPECIAL_LONG.forEach(
        n -> {
          final int bitLength = BigInteger.valueOf(n).bitLength();

          if ((bitLength < SIZE_BITS) || ((bitLength == SIZE_BITS) && (n > 0))) {
            // ... Dword has enough capacity for i
            assertEquals(String.format("ScardHandle{%x}", n), new ScardHandle(n).toString());
          } // end fi
        }); // end forEach(n -> ...)

    // --- c. check with random values
    IntStream.range(0, 255)
        .forEach(
            i -> {
              final long n = new BigInteger(SIZE_BITS, RNG).longValue();

              assertEquals(String.format("ScardHandle{%x}", n), new ScardHandle(n).toString());
            }); // end forEach(i -> ...)
  } // end method */
} // end class
