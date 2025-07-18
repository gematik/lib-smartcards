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

import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.EafiLocation.DF_SPECIFIC;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.ID_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.ID_SUPREMUM;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box testing of {@link EafiPasswordFormat}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestVerify {

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
    // intentionally empty
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /**
   * Test method for {@link Verify#Verify(AbstractAuthenticate.EafiLocation, int,
   * EafiPasswordFormat, String)}.
   */
  @SuppressWarnings("PMD.CognitiveComplexity")
  @Test
  void test_Verify__Location_int_EafiPasswordFormat_String() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected
    // ... b. EafiLocation.referenced(int)-method works as expected
    // ... c. EafiPasswordFormat.octets(...)-method works as expected

    // Note: Because of the assertions, we can be lazy here.

    // Test strategy:
    // --- a. loop over a bunch of valid input values
    for (final var location : AbstractAuthenticate.EafiLocation.values()) {
      for (final var format : EafiPasswordFormat.values()) {
        for (final var id : RNG.intsClosed(ID_INFIMUM, ID_SUPREMUM, 5).boxed().toList()) {
          for (final var length : RNG.intsClosed(1, 14, 5).boxed().toList()) {
            final String secret =
                (format.equals(EafiPasswordFormat.UTF8))
                    ? TestEafiPasswordFormat.randomUtf8Secret(length)
                    : TestEafiPasswordFormat.randomDigitSecret(length);

            final Verify dut = new Verify(location, id, format, secret); // NOPMD new in loop

            assertEquals(0x00, dut.getCla());
            assertEquals(Verify.INS, dut.getIns());
            assertEquals(0, dut.getP1());
            assertEquals(location.reference(id), dut.getP2());
            assertEquals(3, dut.getCase());
            assertArrayEquals(format.octets(secret), dut.getData());
          } // end For (length...)
        } // end For (id...)
      } // end For (format...)
    } // end For (location...)
  } // end method */

  /** Test method for {@link CommandApdu#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. smoke test
    // --- b. smoke test with    same   class,    same   content
    // --- c. smoke test with    same   class, different content
    // --- d. smoke test with different class,    same   content

    final CommandApdu dut = new Verify(DF_SPECIFIC, 31, EafiPasswordFormat.BCD, "12345");

    // --- a. smoke test
    {
      final var other = new Verify(DF_SPECIFIC, 31, EafiPasswordFormat.BCD, "12345");

      assertNotSame(dut, other);
      assertTrue(dut.equals(other)); // NOPMD simplify assertion
    } // end --- a.

    Map.ofEntries(
            // --- b. smoke test with    same   class,    same   content
            Map.entry(
                new Verify(DF_SPECIFIC, 31, EafiPasswordFormat.BCD, "12345"), List.of(true, true)),
            // --- c. smoke test with    same   class, different content
            Map.entry(
                new Verify(DF_SPECIFIC, 31, EafiPasswordFormat.BCD, "123456"),
                List.of(false, false)),
            // --- d. smoke test with different class,    same   content
            Map.entry(new CommandApdu(0, Verify.INS, 0, 0x9f, "12345f"), List.of(false, true)))
        .forEach(
            (obj, result) -> {
              assertNotSame(dut, obj);
              assertEquals(result.get(0), dut.equals(obj));
              if (result.get(1)) {
                assertEquals(Hex.toHexDigits(dut.getBytes()), Hex.toHexDigits(obj.getBytes()));
              } // end fi
            }); // end forEach((obj, result) -> ...)
  } // end method */

  /** Test method for {@link AbstractAuthenticate#explainTrailer(int)}. */
  @Test
  void test_explainTrailer__int() {
    // Note 1: The meaning of a certain trailer-value is typically standardised
    //         in an ISO-standard or specified in a specification.
    //         Thus, the correct explanation has to be ensured by manual reviews
    //         rather than unit test. It follows that the JUnit-test here
    //         concentrates on code-coverage.
    // Note 2: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. explanation not empty
    // --- b. check that some trailers are explained by superclass

    final CommandApdu dut = new Verify(DF_SPECIFIC, 0, EafiPasswordFormat.BCD, "01234");

    // --- a. explanation not empty
    List.of(0x6985).forEach(trailer -> assertFalse(dut.explainTrailer(trailer).isEmpty()));

    // --- b. check that some trailers are explained by superclass
    assertEquals("NoError", dut.explainTrailer(0x9000));
  } // end method */

  /** Test method for serialisation. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_serialize() {
    // Assertions:
    // ... a. equals()-method works as expected

    // Test strategy:
    // --- a. create a bunch of command APDU
    // --- b. serialize each command APDU
    // --- c. deserialize each command APDU
    // --- d. check if deserialized APDU equals original APDU

    // --- a. create a bunch of command APDU
    final List<AbstractAuthenticate> originals = new ArrayList<>();
    // spotless:off
    Arrays.stream(AbstractAuthenticate.EafiLocation.values()).forEach(location -> {
      Arrays.stream(EafiPasswordFormat.values()).forEach(format -> {
        RNG.intsClosed(ID_INFIMUM, ID_SUPREMUM, 5).forEach(id -> {
          RNG.intsClosed(1, 14, 5).forEach(length -> {
            final String secret = (format.equals(EafiPasswordFormat.UTF8))
                ? TestEafiPasswordFormat.randomUtf8Secret(length)
                : TestEafiPasswordFormat.randomDigitSecret(length);

            originals.add(new Verify(location, id, format, secret));
          }); // end forEach(lengthOld -> ...)
        }); // end forEach(id -> ...)
      }); // end forEach(format -> ...)
    }); // end forEach(location -> ...)
    // spotless:on
    // end --- a.

    for (final var cmdApdu : originals) {
      byte[] transfer = AfiUtils.EMPTY_OS;
      final String string = cmdApdu.toString();
      final int length = string.length();

      // --- b. serialize each command APDU
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(string);
        oos.writeObject(cmdApdu);
        oos.writeInt(length);
        oos.flush();
        transfer = baos.toByteArray();
      } catch (IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
      // end --- b.

      try (ByteArrayInputStream bais = new ByteArrayInputStream(transfer);
          ObjectInputStream ois = new ObjectInputStream(bais)) {
        // --- c. deserialize each command APDU
        final Object outA = ois.readObject();
        final Object outB = ois.readObject();
        final int outC = ois.readInt();

        // --- d. check if deserialized APDU equals original APDU
        assertEquals(string, outA);
        assertEquals(outB, cmdApdu);
        assertEquals(outC, length);
      } catch (ClassNotFoundException | IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (cmdApdu...)
  } // end method */

  /** Test method for {@link Verify#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected
    // ... b. EafiLocation.referenced(int)-method works as expected
    // ... c. EafiPasswordFormat.blind(...)-method works as expected

    // Note: Because of assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of valid input values

    // --- a. smoke test
    {
      assertEquals(
          "CLA='00'  INS='20'  P1='00'  P2='92'  Lc='05'  Data='*****'  Le absent",
          new Verify(
                  DF_SPECIFIC,
                  18, // id
                  EafiPasswordFormat.UTF8, // format
                  "53142")
              .toString());
    } // end --- a.

    // --- b. loop over a bunch of valid input values
    // spotless:off
    Arrays.stream(AbstractAuthenticate.EafiLocation.values()).forEach(location -> {
      Arrays.stream(EafiPasswordFormat.values()).forEach(format -> {
        RNG.intsClosed(ID_INFIMUM, ID_SUPREMUM, 5).forEach(id -> {
          RNG.intsClosed(1, 14, 5).forEach(length -> {
            final String secret = (format.equals(EafiPasswordFormat.UTF8))
                ? TestEafiPasswordFormat.randomUtf8Secret(length)
                : TestEafiPasswordFormat.randomDigitSecret(length);
            final Verify dut = new Verify(location, id, format, secret);

            assertEquals(
                String.format(
                    "CLA='00'  INS='20'  P1='00'  P2='%02x'  Lc='%02x'  Data='%s'  Le absent",
                    location.reference(id),
                    dut.getNc(),
                    format.blind(secret)
                ),
                dut.toString()
            );
          }); // end forEach(length -> ...)
        }); // end forEach(id -> ...)
      }); // end forEach(format -> ...)
    }); // end forEach(location -> ...)
    // spotless:on
    // end --- b.
  } // end method */
} // end class
