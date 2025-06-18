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

import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.EafiLocation;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.ID_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.ID_SUPREMUM;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box test for {@link AbstractAuthenticate}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "RV_RETURN_VALUE_IGNORED_INFERRED".
//         Spotbugs message: This code calls a method and ignores the return value.
//         Rational: Intentionally the return value is ignored.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "RV_RETURN_VALUE_IGNORED_INFERRED" // see note 1
}) // */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestAbstractAuthenticate {

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
   * Test method for {@link AbstractAuthenticate#AbstractAuthenticate(int, int, int,
   * AbstractAuthenticate.EafiLocation, int)}.
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity"})
  @Test
  void test_Authenticate__int_int_int_Location_int() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected
    // ... b. EafiLocation.referenced(int)-method works as expected

    // Note: Because of assertions we can be lazy here.

    // Test strategy:
    // --- a. loop over a bunch of valid input values
    for (final var location : EafiLocation.values()) {
      for (final var cla : RNG.intsClosed(0x00, 0xfe, 5).boxed().toList()) {
        for (final var ins : RNG.intsClosed(0x00, 0xff, 5).boxed().toList()) {
          for (final var p1 : RNG.intsClosed(0x00, 0xff, 5).boxed().toList()) {
            for (final var id : RNG.intsClosed(ID_INFIMUM, ID_SUPREMUM, 5).boxed().toList()) {
              final AbstractAuthenticate dut = new AbstractAuthenticate(cla, ins, p1, location, id);

              assertEquals(cla, dut.getCla());
              assertEquals(ins, dut.getIns());
              assertEquals(p1, dut.getP1());
              assertEquals(location.reference(id), dut.getP2());
              assertEquals(1, dut.getCase());
            } // end For (id...)
          } // end For (p1...)
        } // end For (ins...)
      } // end For (cla...)
    } // end For (location...)
  } // end method */

  /**
   * Test method for {@link AbstractAuthenticate#AbstractAuthenticate(int, int, int,
   * AbstractAuthenticate.EafiLocation, int, byte[])}.
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity"})
  @Test
  void test_Authenticate__int_int_int_Location_int_byteA() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected
    // ... b. LOCATION.referenced(int)-method works as expected

    // Note: Because of assertions we can be lazy here.

    // Test strategy:
    // --- a. loop over a bunch of valid input values
    for (final var location : EafiLocation.values()) {
      for (final var cla : RNG.intsClosed(0x00, 0xfe, 5).boxed().toList()) {
        for (final var ins : RNG.intsClosed(0x00, 0xff, 5).boxed().toList()) {
          for (final var p1 : RNG.intsClosed(0x00, 0xff, 5).boxed().toList()) {
            for (final var id : RNG.intsClosed(ID_INFIMUM, ID_SUPREMUM, 5).boxed().toList()) {
              for (final var nc : RNG.intsClosed(1, 20, 5).boxed().toList()) {
                final byte[] cmdData = RNG.nextBytes(nc);
                final AbstractAuthenticate dut =
                    new AbstractAuthenticate(cla, ins, p1, location, id, cmdData);

                assertEquals(cla, dut.getCla());
                assertEquals(ins, dut.getIns());
                assertEquals(p1, dut.getP1());
                assertEquals(location.reference(id), dut.getP2());
                assertArrayEquals(cmdData, dut.getData());
                assertEquals(3, dut.getCase());
              } // end For (nc...)
            } // end For (id...)
          } // end For (p1...)
        } // end For (ins...)
      } // end For (cla...)
    } // end For (location...)
  } // end method */

  /** Test method for {@link CommandApdu#equals(Object)}. */
  @SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. smoke test with    same   class,    same   content
    // --- b. smoke test with    same   class, different content
    // --- c. smoke test with different class,    same   content

    final CommandApdu dut = new AbstractAuthenticate(1, 2, 3, EafiLocation.DF_SPECIFIC, 4);

    Map.ofEntries(
            // --- a. smoke test with    same   class,    same   content
            Map.entry(
                new AbstractAuthenticate(1, 2, 3, EafiLocation.DF_SPECIFIC, 4),
                List.of(true, true)),
            // --- b. smoke test with    same   class, different content
            Map.entry(
                new AbstractAuthenticate(1, 2, 3, AbstractAuthenticate.EafiLocation.DF_SPECIFIC, 5),
                List.of(false, false)),
            // --- c. smoke test with different class,    same   content
            Map.entry(new CommandApdu(1, 2, 3, 0x84), List.of(false, true)))
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
    // --- b. check WrongSecretWarning
    // --- c. check that some trailers are explained by superclass

    final CommandApdu dut = new AbstractAuthenticate(1, 2, 3, EafiLocation.GLOBAL, 5);

    // --- a. explanation not empty
    List.of(0x6983, 0x6a88).forEach(trailer -> assertFalse(dut.explainTrailer(trailer).isEmpty()));

    // --- b. check WrongSecretWarning
    assertEquals("WrongSecretWarning: at least 15 retries", dut.explainTrailer(0x63cf));
    IntStream.rangeClosed(0, 14)
        .forEach(
            retry ->
                assertEquals(
                    "WrongSecretWarning: " + retry + " retries",
                    dut.explainTrailer(0x63c0 + retry)));

    // --- c. check that some trailers are explained by superclass
    assertEquals("NoError", dut.explainTrailer(0x9000));
  } // end method */

  /** Test method for {@link EafiLocation#reference(int)}. */
  @Test
  void test_reference__int() {
    // Test strategy:
    // --- a. test for all valid input values
    // --- b. ERROR: invalid identifier

    // --- a. test for all valid input values
    for (final var location : EafiLocation.values()) {
      for (final var id : IntStream.rangeClosed(ID_INFIMUM, ID_SUPREMUM).boxed().toList()) {
        final var expected = (EafiLocation.GLOBAL.equals(location) ? 0x00 : 0x80) | id;

        final var actual = location.reference(id);

        assertEquals(expected, actual);
      } // end For (id...)
    } // end For (location...)
    // end --- a.

    // --- b. ERROR: invalid identifier
    // spotless:off
    Arrays.stream(EafiLocation.values()).forEach(location ->
        List.of(
            ID_INFIMUM - 1, // just below infimum
            ID_SUPREMUM + 1 // just above supremum
        ).forEach(id -> {
          final Throwable throwable = assertThrows(
              IllegalArgumentException.class,
              // RV_RETURN_VALUE_IGNORED_INFERRED
              () -> location.reference(id));

          assertEquals("Invalid identifier: " + id, throwable.getMessage());
          assertNull(throwable.getCause());
        }) // end forEach(id -> ...)
    ); // end forEach(location -> ...)
    // spotless:on
    // end --- b.
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
    // a.1 case 1
    // a.2 case 3
    final List<AbstractAuthenticate> originals = new ArrayList<>();
    IntStream.range(0, 10)
        .forEach(
            i ->
                originals.add(
                    new AbstractAuthenticate(
                        RNG.nextIntClosed(0, 0xfe), // CLA
                        RNG.nextIntClosed(0, 0xff), // INS
                        RNG.nextIntClosed(0, 0xff), // P1
                        RNG.nextBoolean() ? EafiLocation.GLOBAL : EafiLocation.DF_SPECIFIC,
                        RNG.nextIntClosed(ID_INFIMUM, ID_SUPREMUM) // identifier
                        )));
    IntStream.range(0, 10)
        .forEach(
            i ->
                originals.add(
                    new AbstractAuthenticate(
                        RNG.nextIntClosed(0, 0xfe), // CLA
                        RNG.nextIntClosed(0, 0xff), // INS
                        RNG.nextIntClosed(0, 0xff), // P1
                        RNG.nextBoolean() ? EafiLocation.GLOBAL : EafiLocation.DF_SPECIFIC,
                        RNG.nextIntClosed(ID_INFIMUM, ID_SUPREMUM), // identifier
                        RNG.nextBytes(1, 20) // command data field
                        )));
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
} // end class
