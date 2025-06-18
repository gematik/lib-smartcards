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
package de.gematik.smartcards.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.spec.ECFieldFp;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests on {@link AfiElcParameterSpec}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_LOAD_OF_KNOWN_NULL_VALUE", i.e.
//         Load of known null value.
//         That finding is correct, because intentionally the equals(...)-method is tested with
//         a parameter being null.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_LOAD_OF_KNOWN_NULL_VALUE", // see note 1
}) // */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestAfiElcParameterSpec {

  /** List with all curves predefined in {@link AfiElcParameterSpec}. */
  public static final List<AfiElcParameterSpec> CURVES = AfiElcParameterSpec.PREDEFINED;

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

  /** Test method for {@code AfiElcParameterSpec#AfiElcParameterSpec(BigInteger, ..., int)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_AfiElcParameterSpec__BigInteger6_int() {
    // Test strategy:
    // --- a. construct pre-defined curves
    // --- b. construct non-predefined curves

    // --- a. construct pre-defined curves
    for (final var curve : CURVES) {
      final BigInteger p = curve.getP();
      final BigInteger a = curve.getCurve().getA();
      final BigInteger b = curve.getCurve().getB();
      final BigInteger gx = curve.getGenerator().getAffineX();
      final BigInteger gy = curve.getGenerator().getAffineY();
      final BigInteger n = curve.getOrder();
      final int h = curve.getCofactor(); // NOPMD shore name

      final AfiElcParameterSpec dut = new AfiElcParameterSpec(p, a, b, gx, gy, n, h);
      assertEquals(curve.getL(), dut.getL());
      assertEquals(curve.getOid(), dut.getOid());
      assertEquals(curve.getTau(), dut.getTau());
    } // end For (curve...)

    // --- b. construct non-predefined curves
    // b.1 prime p differs
    // b.2 coefficient a differs
    // b.3 coefficient b differs
    // b.4 generator gx differs
    // b.5 generator gy differs
    // b.6 order n differs
    // b.7 co-factor h differs
    CURVES.forEach(
        curve -> {
          final BigInteger p = curve.getP();
          final BigInteger a = curve.getCurve().getA();
          final BigInteger b = curve.getCurve().getB();
          final BigInteger gx = curve.getGenerator().getAffineX();
          final BigInteger gy = curve.getGenerator().getAffineY();
          final BigInteger n = curve.getOrder();
          final int h = curve.getCofactor(); // NOPMD shore name

          // b.1 prime p differs
          assertFalse(new AfiElcParameterSpec(p.add(BigInteger.ONE), a, b, gx, gy, n, h).hasOid());

          // b.2 coefficient a differs
          assertFalse(new AfiElcParameterSpec(p, a.add(BigInteger.ONE), b, gx, gy, n, h).hasOid());

          // b.3 coefficient b differs
          assertFalse(new AfiElcParameterSpec(p, a, b.add(BigInteger.ONE), gx, gy, n, h).hasOid());

          // b.4 generator gx differs
          assertFalse(new AfiElcParameterSpec(p, a, b, gx.add(BigInteger.ONE), gy, n, h).hasOid());

          // b.5 generator gy differs
          assertFalse(new AfiElcParameterSpec(p, a, b, gx, gy.add(BigInteger.ONE), n, h).hasOid());

          // b.6 order n differs
          assertFalse(new AfiElcParameterSpec(p, a, b, gx, gy, n.add(BigInteger.ONE), h).hasOid());

          // b.7 co-factor h differs
          assertFalse(new AfiElcParameterSpec(p, a, b, gx, gy, n, h + 1).hasOid());
        }); // end forEach(curve -> ...)
  } // end method */

  /** Test method for {@link AfiElcParameterSpec#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. loop over predefined curves
    // --- e. predefined curve compared to non-predefined curve
    // --- f. compare non-predefined curves

    // --- create device under test (DUT)
    final AfiElcParameterSpec dut = AfiElcParameterSpec.ansix9p256r1;

    for (final Object obj :
        new Object[] {
          dut, // --- a. same reference
          null, // --- b. null input
          "afi" // --- c. difference in type
        }) {
      assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals to compare object references
    } // end For (obj...)

    // --- d. loop over predefined curves
    CURVES.forEach(
        curveOuter -> {
          CURVES.forEach(
              curveInner -> {
                if (curveOuter == curveInner) { // NOPMD use equals() to compare object references
                  assertSame(curveOuter, curveInner);
                  assertTrue(curveOuter.equals(curveInner)); // NOPMD use assertEquals(x,y)
                } else {
                  assertFalse(curveOuter.equals(curveInner)); // NOPMD use assertNotEquals(x,y)
                } // end else
              }); // end forEach(curveInner -> ...)
        }); // end forEach(curveOuter -> ...)

    // --- e. predefined curve compared to non-predefined curve
    // e.1 prime p differs
    // e.2 coefficient a differs
    // e.3 coefficient b differs
    // e.4 generator gx differs
    // e.5 generator gy differs
    // e.6 order n differs
    // e.7 co-factor h differs
    CURVES.forEach(
        curve -> {
          final BigInteger p = curve.getP();
          final BigInteger a = curve.getCurve().getA();
          final BigInteger b = curve.getCurve().getB();
          final BigInteger gx = curve.getGenerator().getAffineX();
          final BigInteger gy = curve.getGenerator().getAffineY();
          final BigInteger n = curve.getOrder();
          final int h = curve.getCofactor(); // NOPMD shore name

          AfiElcParameterSpec dutE;

          // e.1 prime p differs
          dutE = new AfiElcParameterSpec(p.add(BigInteger.ONE), a, b, gx, gy, n, h);
          assertFalse(curve.equals(dutE)); // NOPMD use assertNotEquals(x,y) instead
          assertFalse(dutE.equals(curve)); // NOPMD use assertNotEquals(x,y) instead

          // e.2 coefficient a differs
          dutE = new AfiElcParameterSpec(p, a.add(BigInteger.ONE), b, gx, gy, n, h);
          assertFalse(curve.equals(dutE)); // NOPMD use assertNotEquals(x,y) instead
          assertFalse(dutE.equals(curve)); // NOPMD use assertNotEquals(x,y) instead

          // e.3 coefficient b differs
          dutE = new AfiElcParameterSpec(p, a, b.add(BigInteger.ONE), gx, gy, n, h);
          assertFalse(curve.equals(dutE)); // NOPMD use assertNotEquals(x,y) instead
          assertFalse(dutE.equals(curve)); // NOPMD use assertNotEquals(x,y) instead

          // e.4 generator gx differs
          dutE = new AfiElcParameterSpec(p, a, b, gx.add(BigInteger.ONE), gy, n, h);
          assertFalse(curve.equals(dutE)); // NOPMD use assertNotEquals(x,y) instead
          assertFalse(dutE.equals(curve)); // NOPMD use assertNotEquals(x,y) instead

          // e.5 generator gy differs
          dutE = new AfiElcParameterSpec(p, a, b, gx, gy.add(BigInteger.ONE), n, h);
          assertFalse(curve.equals(dutE)); // NOPMD use assertNotEquals(x,y) instead
          assertFalse(dutE.equals(curve)); // NOPMD use assertNotEquals(x,y) instead

          // e.6 order n differs
          dutE = new AfiElcParameterSpec(p, a, b, gx, gy, n.add(BigInteger.ONE), h);
          assertFalse(curve.equals(dutE)); // NOPMD use assertNotEquals(x,y) instead
          assertFalse(dutE.equals(curve)); // NOPMD use assertNotEquals(x,y) instead

          // e.7 co-factor h differs
          dutE = new AfiElcParameterSpec(p, a, b, gx, gy, n, h + 1);
          assertFalse(curve.equals(dutE)); // NOPMD use assertNotEquals(x,y) instead
          assertFalse(dutE.equals(curve)); // NOPMD use assertNotEquals(x,y) instead
        }); // end forEach(curve -> ...)

    // --- f. compare non-predefined curves
    // f.1 prime p differs
    // f.2 coefficient a differs
    // f.3 coefficient b differs
    // f.4 generator gx differs
    // f.5 generator gy differs
    // f.6 order n differs
    // f.7 co-factor h differs
    CURVES.forEach(
        curve -> {
          final BigInteger p = curve.getP();
          final BigInteger a = curve.getCurve().getA();
          final BigInteger b = curve.getCurve().getB();
          final BigInteger gx = curve.getGenerator().getAffineX();
          final BigInteger gy = curve.getGenerator().getAffineY();
          final BigInteger n = curve.getOrder();
          final int h = curve.getCofactor(); // NOPMD shore name

          AfiElcParameterSpec dutF1;
          AfiElcParameterSpec dutF2;

          // f.1 prime p differs
          dutF1 = new AfiElcParameterSpec(p.add(BigInteger.ONE), a, b, gx, gy, n, h);
          dutF2 = new AfiElcParameterSpec(p.add(BigInteger.ONE), a, b, gx, gy, n, h);
          assertNotSame(dutF1, dutF2);
          assertTrue(dutF1.equals(dutF2)); // NOPMD use assertEquals(x,y) instead

          dutF2 = new AfiElcParameterSpec(p.add(BigInteger.TWO), a, b, gx, gy, n, h);
          assertNotSame(dutF1, dutF2);
          assertFalse(dutF1.equals(dutF2)); // NOPMD use assertNotEquals(x,y) instead

          // f.2 coefficient a differs
          dutF1 = new AfiElcParameterSpec(p, a.add(BigInteger.ONE), b, gx, gy, n, h);
          dutF2 = new AfiElcParameterSpec(p, a.add(BigInteger.ONE), b, gx, gy, n, h);
          assertNotSame(dutF1, dutF2);
          assertTrue(dutF1.equals(dutF2)); // NOPMD use assertEquals(x,y) instead

          dutF2 = new AfiElcParameterSpec(p, a.add(BigInteger.TWO), b, gx, gy, n, h);
          assertNotSame(dutF1, dutF2);
          assertFalse(dutF1.equals(dutF2)); // NOPMD use assertNotEquals(x,y) instead

          // f.3 coefficient b differs
          dutF1 = new AfiElcParameterSpec(p, a, b.add(BigInteger.ONE), gx, gy, n, h);
          dutF2 = new AfiElcParameterSpec(p, a, b.add(BigInteger.ONE), gx, gy, n, h);
          assertNotSame(dutF1, dutF2);
          assertTrue(dutF1.equals(dutF2)); // NOPMD use assertEquals(x,y) instead

          dutF2 = new AfiElcParameterSpec(p, a, b.add(BigInteger.TWO), gx, gy, n, h);
          assertNotSame(dutF1, dutF2);
          assertFalse(dutF1.equals(dutF2)); // NOPMD use assertNotEquals(x,y) instead

          // f.4 generator gx differs
          dutF1 = new AfiElcParameterSpec(p, a, b, gx.add(BigInteger.ONE), gy, n, h);
          dutF2 = new AfiElcParameterSpec(p, a, b, gx.add(BigInteger.ONE), gy, n, h);
          assertNotSame(dutF1, dutF2);
          assertTrue(dutF1.equals(dutF2)); // NOPMD use assertEquals(x,y) instead

          dutF2 = new AfiElcParameterSpec(p, a, b, gx.add(BigInteger.TWO), gy, n, h);
          assertNotSame(dutF1, dutF2);
          assertFalse(dutF1.equals(dutF2)); // NOPMD use assertNotEquals(x,y) instead

          // f.5 generator gy differs
          dutF1 = new AfiElcParameterSpec(p, a, b, gx, gy.add(BigInteger.ONE), n, h);
          dutF2 = new AfiElcParameterSpec(p, a, b, gx, gy.add(BigInteger.ONE), n, h);
          assertNotSame(dutF1, dutF2);
          assertTrue(dutF1.equals(dutF2)); // NOPMD use assertEquals(x,y) instead

          dutF2 = new AfiElcParameterSpec(p, a, b, gx, gy.add(BigInteger.TWO), n, h);
          assertNotSame(dutF1, dutF2);
          assertFalse(dutF1.equals(dutF2)); // NOPMD use assertNotEquals(x,y) instead

          // f.6 order n differs
          dutF1 = new AfiElcParameterSpec(p, a, b, gx, gy, n.add(BigInteger.ONE), h);
          dutF2 = new AfiElcParameterSpec(p, a, b, gx, gy, n.add(BigInteger.ONE), h);
          assertNotSame(dutF1, dutF2);
          assertTrue(dutF1.equals(dutF2)); // NOPMD use assertEquals(x,y) instead

          dutF2 = new AfiElcParameterSpec(p, a, b, gx, gy, n.add(BigInteger.TWO), h);
          assertNotSame(dutF1, dutF2);
          assertFalse(dutF1.equals(dutF2)); // NOPMD use assertNotEquals(x,y) instead

          // f.7 co-factor h differs
          dutF1 = new AfiElcParameterSpec(p, a, b, gx, gy, n, h + 1);
          dutF2 = new AfiElcParameterSpec(p, a, b, gx, gy, n, h + 1);
          assertNotSame(dutF1, dutF2);
          assertTrue(dutF1.equals(dutF2)); // NOPMD use assertEquals(x,y) instead

          dutF2 = new AfiElcParameterSpec(p, a, b, gx, gy, n, h + 2);
          assertNotSame(dutF1, dutF2);
          assertFalse(dutF1.equals(dutF2)); // NOPMD use assertNotEquals(x,y) instead
        }); // end forEach(curve -> ...)
  } // end method */

  /** Test method for {@link ECParameterSpec#getCofactor()}. */
  @Test
  void test_getCofactor() {
    // Test strategy:
    // --- a. smoke test for pre-defined curves
    for (final var curve : CURVES) {
      assertEquals(1, curve.getCofactor());
    } // end For (curve...)
  } // end method */

  /** Test method for {@link ECParameterSpec#getCurve()}, coefficient a. */
  @Test
  void test_getCurve_CoefficientA() {
    // Test strategy:
    // --- a. smoke test for pre-defined curves

    // Note: Attribute values in class AfiElcParameterSpec are taken from Ansi X9.62 and RFC 5639.
    final List<BigInteger> expected =
        List.of(
            // Note: For the expected value in ANSI curves see FIPS 186-4 clause D.1.2 footnote:
            //       a=-3 mod p
            BigInteger.valueOf(-3).mod(AfiElcParameterSpec.ansix9p256r1.getP()),
            BigInteger.valueOf(-3).mod(AfiElcParameterSpec.ansix9p384r1.getP()),
            BigInteger.valueOf(-3).mod(AfiElcParameterSpec.ansix9p521r1.getP()),
            /* RFC 5639 clause 3.4 */ new BigInteger(
                "7D5A0975FC2C3057EEF67530417AFFE7FB8055C126DC5C6CE94A4B44F330B5D9", 16),
            /* RFC 5639 clause 3.6 */ new BigInteger(
                "7BC382C63D8C150C3C72080ACE05AFA0C2BEA28E4FB22787139165EFBA91F9"
                    + "0F8AA5814A503AD4EB04A8C7DD22CE2826",
                16),
            /* RFC 5639 clause 3.7 */ new BigInteger(
                "7830A3318B603B89E2327145AC234CC594CBDD8D3DF91610A83441CAEA9863"
                    + "BC2DED5D5AA8253AA10A2EF1C98B9AC8B57F1117A72BF2C7B9E7C1AC4D77FC94CA",
                16));

    for (int i = CURVES.size(); i-- > 0; ) { // NOPMD assignment in operands
      final String exp = expected.get(i).toString(16);
      final String pre = CURVES.get(i).getCurve().getA().toString(16);
      assertEquals(exp, pre);
    } // end For (i...)
  } // end method */

  /** Test method for {@link ECParameterSpec#getCurve()}, coefficient b. */
  @Test
  void test_getCurve_CoefficientB() {
    // Test strategy:
    // --- a. smoke test for pre-defined curves

    // Note: Attribute values in class AfiElcParameterSpec are taken from Ansi X9.62 and RFC 5639.
    final List<BigInteger> expected =
        List.of(
            /* FIPS 186-4 clause D.1.2.3 */ new BigInteger(
                Hex.extractHexDigits(
                    "5ac635d8 aa3a93e7 b3ebbd55 769886bc 651d06b0 cc53b0f6" + "3bce3c3e 27d2604b"),
                16),
            /* FIPS 186-4 clause D.1.2.4 */ new BigInteger(
                Hex.extractHexDigits(
                    "b3312fa7 e23ee7e4 988e056b e3f82d19 181d9c6e fe814112"
                        + "0314088f 5013875a c656398d 8a2ed19d 2a85c8ed d3ec2aef"),
                16),
            /* FIPS 186-4 clause D.1.2.5 */ new BigInteger(
                Hex.extractHexDigits(
                    "051 953eb961 8e1c9a1f 929a21a0 b68540ee a2da725b"
                        + "99b315f3 b8b48991 8ef109e1 56193951 ec7e937b 1652c0bd"
                        + "3bb1bf07 3573df88 3d2c34f1 ef451fd4 6b503f00"),
                16),
            /* RFC 5639 clause 3.4 */ new BigInteger(
                "26DC5C6CE94A4B44F330B5D9BBD77CBF958416295CF7E1CE6BCCDC18FF8C07B6", 16),
            /* RFC 5639 clause 3.6 */ new BigInteger(
                "04A8C7DD22CE28268B39B55416F0447C2FB77DE107DCD2A62E880EA53EEB62"
                    + "D57CB4390295DBC9943AB78696FA504C11",
                16),
            /* RFC 5639 clause 3.7 */ new BigInteger(
                "3DF91610A83441CAEA9863BC2DED5D5AA8253AA10A2EF1C98B9AC8B57F1117"
                    + "A72BF2C7B9E7C1AC4D77FC94CADC083E67984050B75EBAE5DD2809BD638016F723",
                16));

    for (int i = CURVES.size(); i-- > 0; ) { // NOPMD assignment in operands
      final String exp = expected.get(i).toString(16);
      final String pre = CURVES.get(i).getCurve().getB().toString(16);
      assertEquals(exp, pre);
    } // end For (i...)
  } // end method */

  /** Test method for {@link ECParameterSpec#getGenerator()}, base point x-coordinate. */
  @Test
  void test_getGenerator_Gx() {
    // Test strategy:
    // --- a. smoke test for pre-defined curves

    // Note: Attribute values in class AfiElcParameterSpec are taken from Ansi X9.62 and RFC 5639.
    final List<BigInteger> expected =
        List.of(
            /* FIPS 186-4 clause D.1.2.3 */ new BigInteger(
                Hex.extractHexDigits(
                    "6b17d1f2 e12c4247 f8bce6e5 63a440f2 77037d81 2deb33a0" + "f4a13945 d898c296"),
                16),
            /* FIPS 186-4 clause D.1.2.4 */ new BigInteger(
                Hex.extractHexDigits(
                    "aa87ca22 be8b0537 8eb1c71e f320ad74 6e1d3b62 8ba79b98"
                        + "59f741e0 82542a38 5502f25d bf55296c 3a545e38 72760ab7"),
                16),
            /* FIPS 186-4 clause D.1.2.5 */ new BigInteger(
                Hex.extractHexDigits(
                    "c6 858e06b7 0404e9cd 9e3ecb66 2395b442 9c648139"
                        + "053fb521 f828af60 6b4d3dba a14b5e77 efe75928 fe1dc127"
                        + "a2ffa8de 3348b3c1 856a429b f97e7e31 c2e5bd66"),
                16),
            /* RFC 5639 clause 3.4 */ new BigInteger(
                "8BD2AEB9CB7E57CB2C4B482FFC81B7AFB9DE27E1E3BD23C23A4453BD9ACE3262", 16),
            /* RFC 5639 clause 3.6 */ new BigInteger(
                "1D1C64F068CF45FFA2A63A81B7C13F6B8847A3E77EF14FE3DB7FCAFE0CBD10"
                    + "E8E826E03436D646AAEF87B2E247D4AF1E",
                16),
            /* RFC 5639 clause 3.7 */ new BigInteger(
                "81AEE4BDD82ED9645A21322E9C4C6A9385ED9F70B5D916C1B43B62EEF4D009"
                    + "8EFF3B1F78E2D0D48D50D1687B93B97D5F7C6D5047406A5E688B352209BCB9F822",
                16));

    for (int i = CURVES.size(); i-- > 0; ) { // NOPMD assignment in operands
      final String exp = expected.get(i).toString(16);
      final String pre = CURVES.get(i).getGenerator().getAffineX().toString(16);
      assertEquals(exp, pre);
    } // end For (i...)
  } // end method */

  /** Test method for {@link ECParameterSpec#getGenerator()}, base point y-coordinate. */
  @Test
  void test_getGenerator_Gy() {
    // Test strategy:
    // --- a. smoke test for pre-defined curves

    // Note: Attribute values in class AfiElcParameterSpec are taken from Ansi X9.62 and RFC 5639.
    final List<BigInteger> expected =
        List.of(
            /* FIPS 186-4 clause D.1.2.3 */ new BigInteger(
                Hex.extractHexDigits(
                    "4fe342e2 fe1a7f9b 8ee7eb4a 7c0f9e16 2bce3357 6b315ece" + "cbb64068 37bf51f5"),
                16),
            /* FIPS 186-4 clause D.1.2.4 */ new BigInteger(
                Hex.extractHexDigits(
                    "3617de4a 96262c6f 5d9e98bf 9292dc29 f8f41dbd 289a147c"
                        + "e9da3113 b5f0b8c0 0a60b1ce 1d7e819d 7a431d7c 90ea0e5f"),
                16),
            /* FIPS 186-4 clause D.1.2.5 */ new BigInteger(
                Hex.extractHexDigits(
                    "118 39296a78 9a3bc004 5c8a5fb4 2c7d1bd9 98f54449"
                        + "579b4468 17afbd17 273e662c 97ee7299 5ef42640 c550b901"
                        + "3fad0761 353c7086 a272c240 88be9476 9fd16650"),
                16),
            /* RFC 5639 clause 3.4 */ new BigInteger(
                "547EF835C3DAC4FD97F8461A14611DC9C27745132DED8E545C1D54C72F046997", 16),
            /* RFC 5639 clause 3.6 */ new BigInteger(
                "8ABE1D7520F9C2A45CB1EB8E95CFD55262B70B29FEEC5864E19C054FF99129"
                    + "280E4646217791811142820341263C5315",
                16),
            /* RFC 5639 clause 3.7 */ new BigInteger(
                "7DDE385D566332ECC0EABFA9CF7822FDF209F70024A57B1AA000C55B881F81"
                    + "11B2DCDE494A5F485E5BCA4BD88A2763AED1CA2B2FA8F0540678CD1E0F3AD80892",
                16));

    for (int i = CURVES.size(); i-- > 0; ) { // NOPMD assignment in operands
      final String exp = expected.get(i).toString(16);
      final String pre = CURVES.get(i).getGenerator().getAffineY().toString(16);
      assertEquals(exp, pre);
    } // end For (i...)
  } // end method */

  /** Test method for {@link AfiElcParameterSpec#getInstance(AfiOid)}. */
  @Test
  void test_getInstance__AfiOid() {
    // Test strategy:
    // --- a. use OID from pre-defined curves for instantiation
    // --- b. use all OID from EafiOid for instantiation

    // --- a. use OID from pre-defined curves for instantiation
    for (final var exp : CURVES) {
      final AfiElcParameterSpec pre = AfiElcParameterSpec.getInstance(exp.getOid());
      assertSame(exp, pre);
    } // end For (exp...)

    // --- b. use all OID from EafiOid for instantiation
    AfiOid.PREDEFINED.forEach(
        oid -> {
          try {
            final AfiElcParameterSpec dut = AfiElcParameterSpec.getInstance(oid);
            assertTrue(CURVES.contains(dut));
          } catch (NoSuchElementException e) {
            assertTrue(
                AfiElcParameterSpec.PREDEFINED.stream()
                    .map(AfiElcParameterSpec::getOid)
                    .filter(curveOid -> curveOid.equals(oid))
                    .findAny()
                    .isEmpty());
          } // end Catch
        }); // end forEach(oid -> ...)
  } // end method */

  /** Test method for {@link AfiElcParameterSpec#getInstance(ECParameterSpec)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_getInstance__EcParameterSpec() {
    // Test strategy:
    // --- a. construct pre-defined curves
    // --- b. construct non-predefined curves

    // --- a. construct pre-defined curves
    for (final var curve : CURVES) {
      final BigInteger p = curve.getP();
      final BigInteger a = curve.getCurve().getA();
      final BigInteger b = curve.getCurve().getB();
      final BigInteger gx = curve.getGenerator().getAffineX();
      final BigInteger gy = curve.getGenerator().getAffineY();
      final BigInteger n = curve.getOrder();
      final int h = curve.getCofactor(); // NOPMD shore name

      final ECParameterSpec input =
          new ECParameterSpec(
              new EllipticCurve(new ECFieldFp(p), a, b), // curve
              new ECPoint(gx, gy), // generator
              n, // order
              h // co-factor
              );
      assertSame(curve, AfiElcParameterSpec.getInstance(input));
    } // end For (curve...)

    // --- b. construct non-predefined curves
    // b.1 prime p differs
    // b.2 coefficient a differs
    // b.3 coefficient b differs
    // b.4 generator gx differs
    // b.5 generator gy differs
    // b.6 order n differs
    // b.7 co-factor h differs
    CURVES.forEach(
        curve -> {
          final BigInteger p = curve.getP();
          final BigInteger a = curve.getCurve().getA();
          final BigInteger b = curve.getCurve().getB();
          final BigInteger gx = curve.getGenerator().getAffineX();
          final BigInteger gy = curve.getGenerator().getAffineY();
          final BigInteger n = curve.getOrder();
          final int h = curve.getCofactor(); // NOPMD shore name

          // b.1 prime p differs
          final var dutP =
              AfiElcParameterSpec.getInstance(
                  new ECParameterSpec(
                      new EllipticCurve(new ECFieldFp(p.add(BigInteger.ONE)), a, b),
                      new ECPoint(gx, gy),
                      n,
                      h));
          assertThrows(NoSuchElementException.class, dutP::getOid);

          // b.2 coefficient a differs
          final var dutA =
              AfiElcParameterSpec.getInstance(
                  new ECParameterSpec(
                      new EllipticCurve(new ECFieldFp(p), a.add(BigInteger.ONE), b),
                      new ECPoint(gx, gy),
                      n,
                      h));
          assertThrows(NoSuchElementException.class, dutA::getOid);

          // b.3 coefficient b differs
          final var dutB =
              AfiElcParameterSpec.getInstance(
                  new ECParameterSpec(
                      new EllipticCurve(new ECFieldFp(p), a, b.add(BigInteger.ONE)),
                      new ECPoint(gx, gy),
                      n,
                      h));
          assertThrows(NoSuchElementException.class, dutB::getOid);

          // b.4 generator gx differs
          final var dutX =
              AfiElcParameterSpec.getInstance(
                  new ECParameterSpec(
                      new EllipticCurve(new ECFieldFp(p), a, b),
                      new ECPoint(gx.add(BigInteger.ONE), gy),
                      n,
                      h));
          assertThrows(NoSuchElementException.class, dutX::getOid);

          // b.5 generator gy differs
          final var dutY =
              AfiElcParameterSpec.getInstance(
                  new ECParameterSpec(
                      new EllipticCurve(new ECFieldFp(p), a, b),
                      new ECPoint(gx, gy.add(BigInteger.ONE)),
                      n,
                      h));
          assertThrows(NoSuchElementException.class, dutY::getOid);

          // b.6 order n differs
          final var dutN =
              AfiElcParameterSpec.getInstance(
                  new ECParameterSpec(
                      new EllipticCurve(new ECFieldFp(p), a, b),
                      new ECPoint(gx, gy),
                      n.add(BigInteger.ONE),
                      h));
          assertThrows(NoSuchElementException.class, dutN::getOid);

          // b.7 co-factor h differs
          final var dutH =
              AfiElcParameterSpec.getInstance(
                  new ECParameterSpec(
                      new EllipticCurve(new ECFieldFp(p), a, b), new ECPoint(gx, gy), n, h + 1));
          assertThrows(NoSuchElementException.class, dutH::getOid);
        }); // end forEach(curve -> ...)
  } // end method */

  /** Test method for {@link AfiElcParameterSpec#getL()}. */
  @Test
  void test_getL() {
    // Test strategy:
    // --- a. smoke test for pre-defined curves

    // Note: Attribute values in class AfiElcParameterSpec are taken from Ansi X9.62 and RFC 5639.
    final List<Integer> expected =
        List.of(
            32,
            48,
            66, // ANSI curves
            32,
            48,
            64 // brainpool curves
            );

    for (int i = CURVES.size(); i-- > 0; ) { // NOPMD assignment in operands
      final int exp = expected.get(i);
      final int pre = CURVES.get(i).getL();
      assertEquals(exp, pre);
    } // end For (i...)
  } // end method */

  /** Test method for {@link AfiElcParameterSpec#getOid()}. */
  @Test
  void test_getOid() {
    // Note: This simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: OID absent

    // --- a. smoke test
    assertEquals(AfiOid.ansix9p256r1, AfiElcParameterSpec.ansix9p256r1.getOid());

    // --- b. ERROR: OID absent
    {
      final AfiElcParameterSpec dp = AfiElcParameterSpec.ansix9p256r1;
      final var dut =
          new AfiElcParameterSpec(
              dp.getP(),
              dp.getCurve().getA(),
              dp.getCurve().getB(),
              dp.getGenerator().getAffineX(),
              dp.getOrder().subtract(dp.getGenerator().getAffineY()),
              dp.getOrder(),
              dp.getCofactor());
      assertThrows(NoSuchElementException.class, dut::getOid);
    } // --- end b.
  } // end method */

  /** Test method for {@link ECParameterSpec#getOrder()}. */
  @Test
  void test_getOrder() {
    // Test strategy:
    // --- a. smoke test for pre-defined curves

    // Note: Attribute values in class AfiElcParameterSpec are taken from Ansi X9.62 and RFC 5639.
    final List<BigInteger> expected =
        List.of(
            /* FIPS 186-4 clause D.1.2.3 */ new BigInteger(
                "115792089210356248762697446949407573529996955224135760342"
                    + "422259061068512044369"),
            /* FIPS 186-4 clause D.1.2.4 */ new BigInteger(
                "3940200619639447921227904010014361380507973927046544666794"
                    + "6905279627659399113263569398956308152294913554433653942643"),
            /* FIPS 186-4 clause D.1.2.5 */ new BigInteger(
                "686479766013060971498190079908139321726943530014330540939"
                    + "446345918554318339765539424505774633321719753296399637136"
                    + "3321113864768612440380340372808892707005449"),
            /* RFC 5639 clause 3.4 */ new BigInteger(
                "A9FB57DBA1EEA9BC3E660A909D838D718C397AA3B561A6F7901E0E82974856A7", 16),
            /* RFC 5639 clause 3.6 */ new BigInteger(
                "8CB91E82A3386D280F5D6F7E50E641DF152F7109ED5456B31F166E6CAC0425"
                    + "A7CF3AB6AF6B7FC3103B883202E9046565",
                16),
            /* RFC 5639 clause 3.7 */ new BigInteger(
                "AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20ED6639CCA703308"
                    + "70553E5C414CA92619418661197FAC10471DB1D381085DDADDB58796829CA90069",
                16));

    for (int i = CURVES.size(); i-- > 0; ) { // NOPMD assignment in operands
      final String exp = expected.get(i).toString(16);
      final String pre = CURVES.get(i).getOrder().toString(16);
      assertEquals(exp, pre);
    } // end For (i...)
  } // end method */

  /** Test method for {@link AfiElcParameterSpec#getP()}}. */
  @Test
  void test_getP() {
    // Test strategy:
    // --- a. smoke test for pre-defined curves

    // Note: Attribute values in class AfiElcParameterSpec are taken from Ansi X9.62 and RFC 5639.
    final List<BigInteger> expected =
        List.of(
            /* FIPS 186-4 clause D.1.2.3 */ new BigInteger(
                "1157920892103562487626974469494075735300861434152903141955"
                    + "33631308867097853951"),
            /* FIPS 186-4 clause D.1.2.4 */ new BigInteger(
                "3940200619639447921227904010014361380507973927046544666794"
                    + "8293404245721771496870329047266088258938001861606973112319"),
            /* FIPS 186-4 clause D.1.2.5 */ new BigInteger(
                "686479766013060971498190079908139321726943530014330540939"
                    + "446345918554318339765605212255964066145455497729631139148"
                    + "0858037121987999716643812574028291115057151"),
            /* RFC 5639 clause 3.4 */ new BigInteger(
                "A9FB57DBA1EEA9BC3E660A909D838D726E3BF623D52620282013481D1F6E5377", 16),
            /* RFC 5639 clause 3.6 */ new BigInteger(
                "8CB91E82A3386D280F5D6F7E50E641DF152F7109ED5456B412B1DA197FB711"
                    + "23ACD3A729901D1A71874700133107EC53",
                16),
            /* RFC 5639 clause 3.7 */ new BigInteger(
                "AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20ED6639CCA703308"
                    + "717D4D9B009BC66842AECDA12AE6A380E62881FF2F2D82C68528AA6056583A48F3",
                16));

    for (int i = CURVES.size(); i-- > 0; ) { // NOPMD assignment in operands
      final String exp = expected.get(i).toString(16);
      final String pre = CURVES.get(i).getP().toString(16);
      assertEquals(exp, pre);
    } // end For (i...)
  } // end method */

  /** Test method for {@link AfiElcParameterSpec#getTau()}. */
  @Test
  void test_getTau() {
    // Test strategy:
    // --- a. smoke test for pre-defined curves

    // Note: Attribute values in class AfiElcParameterSpec are taken from Ansi X9.62 and RFC 5639.
    final List<Integer> expected =
        List.of(
            32,
            48,
            66, // ANSI curves
            32,
            48,
            64 // brainpool curves
            );

    for (int i = CURVES.size(); i-- > 0; ) { // NOPMD assingment in operands
      final int exp = expected.get(i);
      final int pre = CURVES.get(i).getTau();
      assertEquals(exp, pre);
    } // end For (i...)
  } // end method */

  /** Test method for {@link AfiElcParameterSpec#hasOid()}. */
  @Test
  void test_hasOid() {
    // Test strategy:
    // --- a. all predefined domain parameter have an OID
    // --- b. arbitrary domain parameter do not have an OID

    // --- a. all predefined domain parameter have an OID
    AfiElcParameterSpec.PREDEFINED.forEach(dp -> assertTrue(dp.hasOid()));

    // --- b. arbitrary domain parameter do not have an OID
    {
      final AfiElcParameterSpec dp = AfiElcParameterSpec.brainpoolP384r1;
      assertFalse(
          new AfiElcParameterSpec(
                  dp.getP(),
                  dp.getCurve().getA(),
                  dp.getCurve().getB(),
                  dp.getGenerator().getAffineX(),
                  dp.getOrder().subtract(dp.getGenerator().getAffineY()),
                  dp.getOrder(),
                  dp.getCofactor())
              .hasOid());
    }
  } // end method */

  /** Test method for {@link AfiElcParameterSpec#hashCode()}. */
  @Test
  void test_hashCode() {
    // Test strategy:
    // --- a. loop over predefined curves
    // --- b. call hashCode()-method again

    // --- a. loop over predefined curves
    CURVES.forEach(
        curve ->
            assertEquals(curve.getP().hashCode(), curve.hashCode())); // end forEach(curve -> ...)

    // --- b. call hashCode()-method again
    // Note: The main reason for this check is to get full code-coverage.
    //        a. The first time this method is called on a newly constructed BerTlv object
    //           insHashCode is zero.
    //        b. The second time this method is called the insHashCode isn't zero (with a
    //           high probability).
    final AfiElcParameterSpec dut = AfiElcParameterSpec.brainpoolP384r1;
    final int hash = dut.hashCode();
    assertEquals(hash, dut.hashCode());
  } // end method */

  /** Test serialization. */
  @SuppressWarnings({"PMD.NPathComplexity"})
  @Test
  void test_serialization() {
    // Test strategy:
    // --- a. intentionally empty
    // --- b. intentionally empty
    // --- c. predefined domain parameter
    // --- d. arbitrary curve (i.e. not a named curve)
    // --- e. small size of domain parameter

    AfiElcParameterSpec.PREDEFINED.forEach(
        dut -> {
          // --- c. predefined domain parameter
          {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
              oos.writeObject(dut);
            } catch (IOException e) {
              fail(AfiUtils.UNEXPECTED, e);
            } // end Catch (...)

            final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
              final Object obj = ois.readObject();
              assertSame(dut, obj);
            } catch (ClassNotFoundException | IOException e) {
              fail(AfiUtils.UNEXPECTED, e);
            } // end Catch (...)
          } // end --- c.

          // --- d. arbitrary curve (i.e. not a named curve), here use -G as generator
          {
            final AfiElcParameterSpec dp2 =
                AfiElcParameterSpec.getInstance(
                    new ECParameterSpec(
                        dut.getCurve(),
                        // with G as generator form predefined domain parameter here -G is used
                        new ECPoint(
                            dut.getGenerator().getAffineX(),
                            dut.getP().subtract(dut.getGenerator().getAffineY())),
                        dut.getOrder(),
                        dut.getCofactor()));
            assertTrue(AfiElcUtils.isPointOnEllipticCurve(dp2.getGenerator(), dut));

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
              oos.writeObject(dp2);
            } catch (IOException e) {
              fail(AfiUtils.UNEXPECTED, e);
            } // end Catch (...)

            final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
              final Object obj = ois.readObject();
              assertEquals(dp2, obj);
            } catch (ClassNotFoundException | IOException e) {
              fail(AfiUtils.UNEXPECTED, e);
            } // end Catch (...)
          } // end --- d.
        }); // end forEach(dp -> ...)

    // --- e. small size of domain parameter
    {
      // p=5, a=2, b=4, G=(4,1), n=7, h=1, (G) = {(4,1), (2,4), (0,3), (0,2), (2,1), (4,4), O}
      final AfiElcParameterSpec dut =
          AfiElcParameterSpec.getInstance(
              new ECParameterSpec(
                  new EllipticCurve(
                      new ECFieldFp(BigInteger.valueOf(5)), // p
                      BigInteger.valueOf(2), // a
                      BigInteger.valueOf(4) // b
                      ),
                  new ECPoint(
                      BigInteger.valueOf(4), // Gx
                      BigInteger.valueOf(1) // Gy
                      ),
                  BigInteger.valueOf(7), // order n
                  1 // cofactor h
                  ));
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(dut);
      } catch (IOException e) {
        fail(AfiUtils.UNEXPECTED, e);
      } // end Catch (...)

      final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      try (ObjectInputStream ois = new ObjectInputStream(bais)) {
        final Object obj = ois.readObject();
        assertEquals(dut, obj);
      } catch (ClassNotFoundException | IOException e) {
        fail(AfiUtils.UNEXPECTED, e);
      } // end Catch (...)
    } // end --- e.
  } // end method +/

  /** Test method for {@link AfiElcParameterSpec#toString()}. */
  @Test
  void test_toString() {
    // Test strategy:
    // --- a. smoke test
    assertEquals(
        String.format(
            "p  = 'a9fb57dba1eea9bc3e660a909d838d726e3bf623d52620282013481d1f6e5377'%n"
                + "a  = '7d5a0975fc2c3057eef67530417affe7fb8055c126dc5c6ce94a4b44f330b5d9'%n"
                + "b  = '26dc5c6ce94a4b44f330b5d9bbd77cbf958416295cf7e1ce6bccdc18ff8c07b6'%n"
                + "Gx = '8bd2aeb9cb7e57cb2c4b482ffc81b7afb9de27e1e3bd23c23a4453bd9ace3262'%n"
                + "Gy = '547ef835c3dac4fd97f8461a14611dc9c27745132ded8e545c1d54c72f046997'%n"
                + "n  = 'a9fb57dba1eea9bc3e660a909d838d718c397aa3b561a6f7901e0e82974856a7'%n"
                + "h  = 1"),
        AfiElcParameterSpec.brainpoolP256r1.toString());
  } // end method */
} // end class
