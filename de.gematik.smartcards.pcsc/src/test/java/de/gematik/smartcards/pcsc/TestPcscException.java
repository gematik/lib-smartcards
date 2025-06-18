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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import de.gematik.smartcards.utils.AfiRng;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link PcscException}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NM_CLASS_NOT_EXCEPTION".
//         Spotbugs message: This class is not derived from another exception,
//             but ends with 'Exception'.
//         Rational: That finding is suppressed because class name is
//             intentionally derived from the class-under-test.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NM_CLASS_NOT_EXCEPTION" // see note 1
}) // */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestPcscException {

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

  /** Test method for {@link PcscException#PcscException(int, String)}. */
  @Test
  void test_PcscExxception__int_String() {
    // Assertions:
    // ... a. constructor(int, String, Throwable) works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of valid input parameters

    // --- a. smoke test
    {
      final int code = 0;
      final String message = "foo";
      final PcscException dut = new PcscException(code, message);

      assertEquals(code, dut.getCode());
      assertSame(message, dut.getMessage());
      assertNull(dut.getCause());
    } // end --- a.

    // --- b. loop over a bunch of valid input parameters
    {
      for (final String message : new String[] {null, "", "barFoo"}) {
        final int code = RNG.nextInt();
        final PcscException dut = new PcscException(code, message); // NOPMD new in loop

        assertEquals(code, dut.getCode());
        assertEquals(message, dut.getMessage());
        assertNull(dut.getCause());
      } // end For (message...)
    } // end --- b.
  } // end method */

  /** Test method for {@link PcscException#PcscException(int, String, Throwable)}. */
  @Test
  void test_PcscException__int_String_Throwable() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of valid input parameter

    // --- a. smoke test
    {
      final int code = 0;
      final String message = "foo";
      final Throwable cause = new RuntimeException("bar");
      final PcscException dut = new PcscException(code, message, cause);

      assertEquals(code, dut.getCode());
      assertSame(message, dut.getMessage());
      assertSame(cause, dut.getCause());
    } // end --- a.

    // --- b. loop over a bunch of valid input parameter
    {
      final String[] messages = new String[] {null, "", "fooBar"};
      final Throwable[] causes =
          new Throwable[] {null, new RuntimeException("afi"), new ExceptionInInitializerError()};
      for (final String message : messages) {
        for (final Throwable cause : causes) {
          final int code = RNG.nextInt();
          final PcscException dut = new PcscException(code, message, cause); // NOPMD new in loop

          assertEquals(code, dut.getCode());
          assertEquals(message, dut.getMessage());
          assertEquals(cause, dut.getCause());
        } // end For (cause...)
      } // end For (message...)
    } // end --- b.
  } // end method */

  /** Test method for {@link PcscException#getCode()}. */
  @Test
  void test_getCode() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: This simple method does not need extensive testing, so,
    //       based on the assertion(s) we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final int code = RNG.nextInt();

    final PcscException dut = new PcscException(code, "");

    assertEquals(code, dut.getCode());
  } // end method */

  /** Test method for {@link PcscException#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. code, message, cause: present non-null non-null
    // --- b. code, message, cause: present non-null   null
    // --- c. code, message, cause: present   null   non-null
    // --- d. code, message, cause: present   null     null

    // --- a. code, message, cause: present non-null non-null
    {
      final int code = RNG.nextInt();
      final String message = "fooA";
      final Throwable cause = new IllegalArgumentException();

      final PcscException dut = new PcscException(code, message, cause);

      assertEquals(
          String.format("StatusCode = %d = 0x%x,", code, code)
              + " de.gematik.smartcards.pcsc.PcscException: fooA",
          dut.toString());
    } // end --- a.

    // --- b. code, message, cause: present non-null   null
    {
      final int code = 42;
      final String message = "fooB";

      final PcscException dut = new PcscException(code, message, null);

      assertEquals(
          "StatusCode = 42 = 0x2a," + " de.gematik.smartcards.pcsc.PcscException: fooB",
          dut.toString());
    } // end --- b.

    // --- c. code, message, cause: present   null   non-null
    {
      final int code = -42;
      final Throwable cause = new ArithmeticException();

      final PcscException dut = new PcscException(code, null, cause);

      assertEquals(
          "StatusCode = -42 = 0xffffffd6," + " de.gematik.smartcards.pcsc.PcscException",
          dut.toString());
    } // end --- c.

    // --- d. code, message, cause: present   null     null
    {
      final int code = RNG.nextInt();

      final PcscException dut = new PcscException(code, null, null);

      assertEquals(
          String.format("StatusCode = %d = 0x%x,", code, code)
              + " de.gematik.smartcards.pcsc.PcscException",
          dut.toString());
    } // end --- d.
  } // end method */
} // end class */
