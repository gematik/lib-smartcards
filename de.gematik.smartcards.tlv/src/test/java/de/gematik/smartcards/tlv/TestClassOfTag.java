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
package de.gematik.smartcards.tlv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests on {@link ClassOfTag}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle:methodname"})
final class TestClassOfTag {

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

  /** Test method for {@link ClassOfTag#getEncoding()}. */
  @Test
  void test_getEncoding() {
    // Simple method doesn't need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. check encoding for all possible values
    for (final var classOfTag : ClassOfTag.values()) {
      switch (classOfTag) { // NOPMD exhaustive even without "default"
        case UNIVERSAL:
          assertEquals(0x00, classOfTag.getEncoding());
          break;

        case APPLICATION:
          assertEquals(0x40, classOfTag.getEncoding());
          break;

        case CONTEXT_SPECIFIC:
          assertEquals(0x80, classOfTag.getEncoding());
          break;

        case PRIVATE:
          assertEquals(0xc0, classOfTag.getEncoding());
          break;

        default:
          fail("unexpected value: " + classOfTag);
          break;
      } // end Switch (classOfTag)
    } // end For (classOfTag...)
  } // end method */

  /** Test method for {@link ClassOfTag#getInstance(byte)}. */
  @Test
  void test_getInstance__byte() {
    // Simple method doesn't need much testing, so we can be lazy here.

    // Test strategy:
    // --- a. check encoding for all possible values
    for (int leadingOctet = 256; leadingOctet-- > 0; ) { // NOPMD assignment in operand
      // spotless:off
      final ClassOfTag expectd = switch (leadingOctet & 0xc0) {
        case 0x00 -> ClassOfTag.UNIVERSAL;
        case 0x40 -> ClassOfTag.APPLICATION;
        case 0x80 -> ClassOfTag.CONTEXT_SPECIFIC;
        default -> ClassOfTag.PRIVATE;
      }; // end Switch (...)
      // spotless:on

      final var actual = ClassOfTag.getInstance((byte) leadingOctet);

      assertSame(expectd, actual);
    } // end For (leadingOctet...)
  } // end method */
} // end class
