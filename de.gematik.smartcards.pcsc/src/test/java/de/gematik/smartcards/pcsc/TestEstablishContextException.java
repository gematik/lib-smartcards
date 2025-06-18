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

import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link EstablishContextException}.
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
final class TestEstablishContextException { // NM_CLASS_NOT_EXCEPTION

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

  /** Test method for {@link EstablishContextException#EstablishContextException(PcscException)}. */
  @Test
  void test_EstablishContextException__PcscException() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing,
    //       so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final PcscException cause = new PcscException(0, "foo");

    final EstablishContextException dut = new EstablishContextException(cause);

    assertSame(cause, dut.getCause());
  } // end method */
} // end class
