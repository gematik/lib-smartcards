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
package de.gematik.smartcards.g2icc.cvc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import de.gematik.smartcards.utils.AfiRng;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link CvcComponent}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestCvcComponent {

  /** Random Number Generator. */
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
    // --- clear cache
    TrustCenter.clearCache();
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /** Test method for {@link CvcComponent#CvcComponent(PrimitiveBerTlv)}. */
  @Test
  void test_CvcComponent__PrimitiveBerTlv() {
    // Test strategy:
    // --- a. check for defensive cloning
    final byte[] input = RNG.nextBytes(1, 10);
    final CvcComponent dut = new MyCvcComponentA((PrimitiveBerTlv) BerTlv.getInstance(0x8a, input));
    assertNotSame(input, dut.getValue());
    assertArrayEquals(input, dut.getValue());
  } // end method */

  @Test
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  void test_equals__Object() {
    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. different object, but same type

    final PrimitiveBerTlv input = (PrimitiveBerTlv) BerTlv.getInstance(0x8a, RNG.nextBytes(1, 10));
    final CvcComponent dut = new MyCvcComponentA(input);

    for (final Object obj :
        new Object[] {
          dut, // --- a. same reference
          null, // --- b. null input
          "afi", // --- c. difference in type
          new MyCvcComponentB(input) // --- c. difference in type
        }) {
      assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals() to compare object references
    } // end For (obj...)

    // --- d. different object, but same type
    // d.1 same length, but different content
    // d.2 different length
    // d.3 same content

    // d.1 same length, but different content
    Object object;
    do {
      object =
          new MyCvcComponentA(
              (PrimitiveBerTlv) BerTlv.getInstance(0x8a, RNG.nextBytes(dut.getValue().length)));
    } while (Arrays.equals(dut.getValue(), ((CvcComponent) object).getValue()));
    assertFalse(dut.equals(object)); // NOPMD use assertNotEquals(x, y) instead

    // d.2 different length
    object =
        new MyCvcComponentA(
            (PrimitiveBerTlv) BerTlv.getInstance(0x8a, RNG.nextBytes(dut.getValue().length + 1)));
    assertFalse(dut.equals(object)); // NOPMD use assertNotEquals(x, y) instead

    // d.3 same content
    object = new MyCvcComponentA(input);
    assertNotSame(dut, object);
    assertTrue(dut.equals(object)); // NOPMD use assertEquals(x, y) instead
  } // end method */

  /** Test method for {@link CvcComponent#getReport()}. */
  @Test
  void test_getReport() {
    // Note: Simple default implementation does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final CvcComponent dut =
        new MyCvcComponentA((PrimitiveBerTlv) BerTlv.getInstance(0x80, RNG.nextBytes(0, 10)));

    assertTrue(dut.getReport().isEmpty());
  } // end method */

  /** Test method for {@link CvcComponent#getValue()}. */
  @Test
  void test_getValue() {
    // Test strategy:
    // --- a. check for defensive cloning
    final byte[] input = RNG.nextBytes(1, 10);
    final CvcComponent dut = new MyCvcComponentA((PrimitiveBerTlv) BerTlv.getInstance(0x8a, input));
    final byte[] value = dut.getValue();
    assertNotSame(value, dut.getValue());
    assertArrayEquals(value, dut.getValue());
  } // end method */

  /** Test method for {@link CvcComponent#hasCriticalFindings()}. */
  @Test
  void test_hasCriticalFindings() {
    // Note: Simple default implementation does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final CvcComponent dut =
        new MyCvcComponentA((PrimitiveBerTlv) BerTlv.getInstance(0x80, RNG.nextBytes(0, 10)));

    assertFalse(dut.hasCriticalFindings());
  } // end method */

  /** Test method for {@link CvcComponent#hashCode()}. */
  @Test
  void test_hashCode() {
    // Test strategy:
    // --- a. smoke test
    // --- b. call hashCode()-method again

    // --- a. smoke test
    {
      final BerTlv input = BerTlv.getInstance(0x8a, RNG.nextBytes(1, 10));
      final CvcComponent dut = new MyCvcComponentA((PrimitiveBerTlv) input);
      assertEquals(input.hashCode(), dut.hashCode());
    } // end --- a.

    // --- b. call hashCode()-method again
    {
      final CvcComponent dut =
          new MyCvcComponentA((PrimitiveBerTlv) BerTlv.getInstance(0x8b, RNG.nextBytes(0, 20)));
      final int hash = dut.hashCode();
      assertEquals(hash, dut.hashCode());
    } // end --- b.
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /** Test class A. */
  private static final class MyCvcComponentA extends CvcComponent {

    /**
     * Comfort constructor.
     *
     * @param tlv primitive data
     */
    private MyCvcComponentA(final PrimitiveBerTlv tlv) {
      super(tlv);
    } // end constructor */

    /**
     * Explanation of the content of this component.
     *
     * @return {@link List} of {@link String} with explanation
     */
    /* package */
    @Override
    List<String> getExplanation() {
      return Collections.emptyList();
    } // end method */
  } // end inner class MyCvcComponentA

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /** Test class B. */
  private static final class MyCvcComponentB extends CvcComponent {

    /**
     * Comfort constructor.
     *
     * @param tlv primitive data
     */
    private MyCvcComponentB(final PrimitiveBerTlv tlv) {
      super(tlv);
    } // end constructor */

    /**
     * Explanation of the content of this component.
     *
     * @return {@link List} of {@link String} with explanation
     */
    /* package */
    @Override
    List<String> getExplanation() {
      return Collections.emptyList();
    } // end method */
  } // end inner class MyCvcComponentB
} // end class
