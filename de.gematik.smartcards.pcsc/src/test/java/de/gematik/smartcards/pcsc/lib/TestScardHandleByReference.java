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
import static org.junit.jupiter.api.Assertions.assertNotSame;

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
 * Class performing white-box tests for {@link ScardHandleByReference}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestScardHandleByReference {

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

  /** Test method for {@link ScardHandleByReference#ScardHandleByReference()}. */
  @Test
  void test_ScardHandleByReference() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing,
    //       so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    assertDoesNotThrow(ScardHandleByReference::new);
  } // end method */

  /** Test method for {@link ScardHandleByReference#getValue()}. */
  @Test
  void test_getValue() {
    // Assertions:
    // ... a. setValue(ScardHandle)-method works as expected

    // Note: More values are tested in test_setLong(ScardHandle)-method, see there.

    // Test strategy:
    // --- a. smoke test
    final ScardHandleByReference dut = new ScardHandleByReference();
    final int input = 42;
    final ScardHandle inp = new ScardHandle(input);
    dut.setValue(inp);

    final ScardHandle pre = dut.getValue();

    assertNotSame(inp, pre);
    assertEquals(input, pre.longValue());
  } // end method */

  /** Test method for {@link ScardHandleByReference#setValue(ScardHandle)}. */
  @Test
  void test_setValue__ScardHandle() {
    // Assertions:
    // ... a. getValue()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. check with special long values
    // --- c. check with random values

    final ScardHandleByReference dut = new ScardHandleByReference();

    // --- a. smoke test
    {
      final int input = -42;
      final ScardHandle inp = new ScardHandle(input);
      dut.setValue(inp);

      final ScardHandle pre = dut.getValue();

      assertNotSame(inp, pre);
      assertEquals(input, pre.longValue());
    } // end --- a.

    // --- b. check with special long values
    AfiUtils.SPECIAL_LONG.forEach(
        n -> {
          final int bitLength = BigInteger.valueOf(n).bitLength();
          if ((bitLength < SIZE_BITS) || ((bitLength == SIZE_BITS) && (n > 0))) {
            // ... ScardHandle has enough capacity for i
            final ScardHandle input = new ScardHandle(n);

            dut.setValue(input);

            final ScardHandle present = dut.getValue();

            assertNotSame(input, present);
            assertEquals(input.longValue(), present.longValue());
          } // end fi
        }); // end forEach(n -> ...)
    // end --- b.

    // --- c. check with random values
    IntStream.range(0, 255)
        .forEach(
            i -> {
              final long n = new BigInteger(SIZE_BITS, RNG).longValue();

              final ScardHandle input = new ScardHandle(n);

              dut.setValue(input);

              final ScardHandle present = dut.getValue();

              assertNotSame(input, present);
              assertEquals(input.longValue(), present.longValue());
            }); // end forEach(i -> ...)
  } // end method */
} // end class
