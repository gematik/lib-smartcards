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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for white box testing {@link EafiIccProtocol}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestEafiIccProtocol {

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

  /** Test method for {@link EafiIccProtocol#getInstance(byte)}. */
  @Test
  void test_getInstance__byte() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all possible byte-values

    // --- a. smoke test
    assertEquals(EafiIccProtocol.T1, EafiIccProtocol.getInstance((byte) 1));

    // --- b. loop over all possible byte-values
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            value -> {
              final byte protocolNumber = (byte) value;
              // spotless:off
              final EafiIccProtocol expected = switch (value & 0xf) {
                case 0 -> EafiIccProtocol.T0;
                case 1 -> EafiIccProtocol.T1;
                case 2 -> EafiIccProtocol.T2;
                case 3 -> EafiIccProtocol.T3;
                case 4 -> EafiIccProtocol.T4;
                case 5 -> EafiIccProtocol.T5;
                case 6 -> EafiIccProtocol.T6;
                case 7 -> EafiIccProtocol.T7;
                case 8 -> EafiIccProtocol.T8;
                case 9 -> EafiIccProtocol.T9;
                case 10 -> EafiIccProtocol.T10;
                case 11 -> EafiIccProtocol.T11;
                case 12 -> EafiIccProtocol.T12;
                case 13 -> EafiIccProtocol.T13;
                case 14 -> EafiIccProtocol.T14;
                default -> EafiIccProtocol.T15;
              }; // end Switch (value)
              // spotless:on

              assertEquals(expected, EafiIccProtocol.getInstance(protocolNumber));
            }); // end forEach(value -> ...)
  } // end method

  /** Test method for {@link EafiIccProtocol#getInstance(int)}. */
  @Test
  void test_getInstance__int() {
    // Test strategy:
    // --- a. check all valid values
    // --- b. check some invalid values

    // --- a. check all valid values
    assertEquals(EafiIccProtocol.DIRECT, EafiIccProtocol.getInstance(0));
    assertEquals(EafiIccProtocol.T0, EafiIccProtocol.getInstance(1));
    assertEquals(EafiIccProtocol.T1, EafiIccProtocol.getInstance(2));
    assertEquals(EafiIccProtocol.T0_OR_T1, EafiIccProtocol.getInstance(3));

    // --- b. check some invalid values
    List.of(
            -1, // just below valid values
            5 // just above valid values
            )
        .forEach(
            input ->
                assertThrows(
                    IllegalArgumentException.class,
                    () -> EafiIccProtocol.getInstance(input))); // end forEach(input -> ...)
  } // end method */

  /** Test method for {@link EafiIccProtocol#getInstance(String)}. */
  @Test
  void test_getInstance__String() {
    // Test strategy:
    // --- a. valid values
    // --- b. some invalid values

    // --- a. valid values
    assertEquals(EafiIccProtocol.T0_OR_T1, EafiIccProtocol.getInstance("DONT_CARE"));
    for (final EafiIccProtocol i : EafiIccProtocol.values()) {
      assertEquals(i, EafiIccProtocol.getInstance(i.name()));
      assertEquals(i, EafiIccProtocol.getInstance(i.toString()));
    } // end For (i...)
    // end --- a.

    // --- b. some invalid values
    List.of("foo", "bar")
        .forEach(
            description ->
                assertThrows(
                    IllegalArgumentException.class,
                    () ->
                        EafiIccProtocol.getInstance(
                            description))); // end forEach(description -> ...)
  } // end method */

  /** Test method for {@link EafiIccProtocol#getCode()}. */
  @Test
  void test_getCode() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all enums

    // --- a. smoke test
    assertEquals(2, EafiIccProtocol.T1.getCode());

    // --- b. loop over all enums
    for (final EafiIccProtocol i : EafiIccProtocol.values()) {
      switch (i) {
        case DIRECT -> assertEquals(0, i.getCode());
        case T0 -> assertEquals(1, i.getCode());
        case T1 -> assertEquals(2, i.getCode());
        case T0_OR_T1 -> assertEquals(3, i.getCode());
        case RAW -> assertEquals(4, i.getCode());
        default -> assertThrows(IllegalArgumentException.class, i::getCode); // end default
      } // end Switch (i)
    } // end For (i...)
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
    assertEquals("*", EafiIccProtocol.T0_OR_T1.toString());
  } // end method */
} // end class
