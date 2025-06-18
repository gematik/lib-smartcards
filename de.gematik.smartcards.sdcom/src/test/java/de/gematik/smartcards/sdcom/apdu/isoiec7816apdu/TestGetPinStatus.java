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

import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.ID_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.ID_SUPREMUM;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
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
import java.util.stream.IntStream;
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
final class TestGetPinStatus {

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

  /** Test method for {@link GetPinStatus#GetPinStatus(AbstractAuthenticate.EafiLocation, int)}. */
  @Test
  void test_GetPinStatus__Location_int() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected
    // ... b. EafiLocation.referenced(int)-method works as expected

    // Note: Because of assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of valid input values

    // --- a. smoke test
    {
      assertEquals(
          "80200083",
          Hex.toHexDigits(
              new GetPinStatus(AbstractAuthenticate.EafiLocation.DF_SPECIFIC, 3).getBytes()));
    } // end --- a.

    // --- b. loop over a bunch of valid input values
    Arrays.stream(AbstractAuthenticate.EafiLocation.values())
        .forEach(
            location -> {
              IntStream.rangeClosed(ID_INFIMUM, ID_SUPREMUM)
                  .forEach(
                      id -> {
                        final GetPinStatus dut = new GetPinStatus(location, id);

                        assertEquals(0x80, dut.getCla());
                        assertEquals(GetPinStatus.INS, dut.getIns());
                        assertEquals(0x00, dut.getP1());
                        assertEquals(location.reference(id), dut.getP2());
                        assertEquals(1, dut.getCase());
                      }); // end forEach(id -> ...)
            }); // end forEach(location -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link CommandApdu#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. smoke test
    // --- b. smoke test with    same   class,    same   content
    // --- c. smoke test with    same   class, different content
    // --- d. smoke test with different class,    same   content

    final CommandApdu dut = new GetPinStatus(AbstractAuthenticate.EafiLocation.GLOBAL, ID_SUPREMUM);

    // --- a. smoke test
    {
      final CommandApdu other =
          new GetPinStatus(AbstractAuthenticate.EafiLocation.GLOBAL, ID_SUPREMUM);

      assertNotSame(dut, other);
      assertTrue(dut.equals(other)); // NOPMD simplify assertion
    } // end --- a.

    Map.ofEntries(
            // --- b. smoke test with    same   class,    same   content
            Map.entry(
                new GetPinStatus(AbstractAuthenticate.EafiLocation.GLOBAL, ID_SUPREMUM),
                List.of(true, true)),
            // --- c. smoke test with    same   class, different content
            Map.entry(
                new GetPinStatus(AbstractAuthenticate.EafiLocation.DF_SPECIFIC, ID_SUPREMUM),
                List.of(false, false)),
            // --- d. smoke test with different class,    same   content
            Map.entry(new CommandApdu(0x80, 0x20, 0x00, 0x1f), List.of(false, true)))
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
    // --- b. check RetryCounter
    // --- c. check that some trailers are explained by superclass

    final CommandApdu dut = new GetPinStatus(AbstractAuthenticate.EafiLocation.GLOBAL, ID_SUPREMUM);

    // --- a. explanation not empty
    List.of(0x62c1, 0x62c7, 0x62d0, 0x9000)
        .forEach(trailer -> assertFalse(dut.explainTrailer(trailer).isEmpty()));

    // --- b. check RetryCounter
    assertEquals("RetryCounter: at least 15 retries", dut.explainTrailer(0x63cf));
    IntStream.rangeClosed(0, 14)
        .forEach(
            retry ->
                assertEquals(
                    "RetryCounter: " + retry + " retries", dut.explainTrailer(0x63c0 + retry)));

    // --- c. check that some trailers are explained by superclass
    assertEquals("ObjectNotFound", dut.explainTrailer(0x6a88));
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
    Arrays.stream(AbstractAuthenticate.EafiLocation.values())
        .forEach(
            location -> {
              IntStream.rangeClosed(ID_INFIMUM, ID_SUPREMUM)
                  .forEach(
                      id ->
                          originals.add(new GetPinStatus(location, id))); // end forEach(id -> ...)
            }); // end forEach(location -> ...)
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
} // end class */
