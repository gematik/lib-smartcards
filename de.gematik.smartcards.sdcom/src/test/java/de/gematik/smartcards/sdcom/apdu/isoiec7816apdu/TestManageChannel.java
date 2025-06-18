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
package de.gematik.smartcards.sdcom.apdu.isoiec7816apdu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class comparing the implementation of this library to JDK's implementation.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.MethodNamingConventions"})
final class TestManageChannel {

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

  /** Test method for static constants. */
  @Test
  void test_staticConstants() {
    // Assertions:
    // ... a. constructor(s) work as expected (also from superclasses)
    // ... b. toString()-method works as expected

    // Test method:
    // --- a. smoke test
    assertEquals(
        "CLA='00'  INS='70'  P1='00'  P2='00'  Lc and Data absent  Le='01'",
        ManageChannel.OPEN.toString());
    assertEquals(
        "CLA='00'  INS='70'  P1='80'  P2='00'  Lc and Data absent  Le absent",
        ManageChannel.CLOSE.toString());
    assertEquals(
        "CLA='00'  INS='70'  P1='40'  P2='00'  Lc and Data absent  Le absent",
        ManageChannel.RESET_CHANNEL.toString());
    assertEquals(
        "CLA='00'  INS='70'  P1='40'  P2='01'  Lc and Data absent  Le absent",
        ManageChannel.RESET_APPLICATION.toString());
  } // end method */
} // end class
