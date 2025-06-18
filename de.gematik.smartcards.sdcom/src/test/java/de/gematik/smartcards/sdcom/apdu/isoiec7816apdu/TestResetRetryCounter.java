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
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.EafiLocation.GLOBAL;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.ID_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractAuthenticate.ID_SUPREMUM;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestResetRetryCounter {

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
   * Test method for {@link ResetRetryCounter#ResetRetryCounter(AbstractAuthenticate.EafiLocation,
   * int)}.
   */
  @Test
  void test_ResetRetryCounter__Location_int() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected
    // ... b. EafiLocation.referenced(int)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all relevant combinations of input parameter

    // --- a. smoke test
    {
      assertEquals(
          "002c011e0365432f",
          Hex.toHexDigits(
              new ResetRetryCounter(
                      GLOBAL,
                      30, // id
                      EafiPasswordFormat.BCD, // format
                      "65432", // PUK
                      "" // PIN
                      )
                  .getBytes()));
    } // end --- a.

    // --- b. loop over all relevant combinations of input parameter
    Arrays.stream(AbstractAuthenticate.EafiLocation.values())
        .forEach(
            location -> {
              IntStream.rangeClosed(ID_INFIMUM, ID_SUPREMUM)
                  .forEach(
                      id -> {
                        final ResetRetryCounter dut = new ResetRetryCounter(location, id);

                        assertEquals(0x00, dut.getCla());
                        assertEquals(0x2c, dut.getIns());
                        assertEquals(3, dut.getP1());
                        assertEquals(location.reference(id), dut.getP2());
                        assertEquals(1, dut.getCase());
                      }); // end forEach(id -> ...)
            }); // end forEach(location -> ...)
    // end --- b.
  } // end method */

  /**
   * Test method for {@link ResetRetryCounter#ResetRetryCounter(AbstractAuthenticate.EafiLocation,
   * int, EafiPasswordFormat, String, String)}.
   */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_ResetRetryCounter__Location_int_Format_String_String() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected
    // ... b. EafiLocation.referenced(int)-method works as expected
    // ... c. EafiPasswordFormat.octets(...)-method works as expected

    // Note: Because of assertions, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of valid input values

    // --- a. smoke test
    {
      final var dut =
          new ResetRetryCounter(
              DF_SPECIFIC, 5, EafiPasswordFormat.FORMAT_2_PIN_BLOCK, "1465327", "864205");

      assertEquals(0x00, dut.getCla());
      assertEquals(0x2c, dut.getIns());
      assertEquals(0x00, dut.getP1());
      assertEquals(0x85, dut.getP2());
      assertEquals(
          Hex.extractHexDigits("27-1465327-fffffff   26-864205-ffffffff"),
          Hex.toHexDigits(dut.getData()));
      assertEquals(3, dut.getCase());
    } // end --- a.

    // --- b. loop over a bunch of valid input values
    // spotless:off
    Arrays.stream(EafiPasswordFormat.values()).forEach(format -> {
      RNG.intsClosed(0, 14, 5).forEach(lengthPuk -> {
        final boolean isPukEmpty = 0 == lengthPuk;
        final String puk = (format.equals(EafiPasswordFormat.UTF8))
            ? TestEafiPasswordFormat.randomUtf8Secret(lengthPuk)
            : TestEafiPasswordFormat.randomDigitSecret(lengthPuk);

        RNG.intsClosed(0, 14, 5).forEach(lengthPin -> {
          final boolean isPinEmpty = 0 == lengthPin;
          final String pin = (format.equals(EafiPasswordFormat.UTF8))
              ? TestEafiPasswordFormat.randomUtf8Secret(lengthPin)
              : TestEafiPasswordFormat.randomDigitSecret(lengthPin);

          final int expectedP1;
          if (isPukEmpty) {
            if (isPinEmpty) {
              // ... PUK  and  PIN absent
              expectedP1 = 3;
            } else {
              // ... PUK absent, PIN present
              expectedP1 = 2;
            } // end else
          } else {
            if (isPinEmpty) {
              // ... PUK  present, PIN absent
              expectedP1 = 1;
            } else {
              // ... PUK  and  PIN present
              expectedP1 = 0;
            } // end else
          } // end else (PUK absent?)

          Arrays.stream(AbstractAuthenticate.EafiLocation.values()).forEach(location -> {
            RNG.intsClosed(ID_INFIMUM, ID_SUPREMUM, 5).forEach(id -> {
              if (3 == expectedP1) { // NOPMD literal in if
                // statement
                // ... command data field absent
                assertThrows(
                    IllegalArgumentException.class,
                    () -> new ResetRetryCounter(location, id, format, puk, pin));
              } else {
                // ... command data field present
                final ResetRetryCounter dut = new ResetRetryCounter(location, id, format, puk, pin);

                assertEquals(0x00, dut.getCla());
                assertEquals(ResetRetryCounter.INS, dut.getIns());
                assertEquals(expectedP1, dut.getP1());
                assertEquals(location.reference(id), dut.getP2());
                assertEquals(3, dut.getCase());
                assertEquals(
                    (isPukEmpty ? "" : Hex.toHexDigits(format.octets(puk)))
                        + (isPinEmpty ? "" : Hex.toHexDigits(format.octets(pin))),
                    Hex.toHexDigits(dut.getData())
                );
              } // end else
            }); // end forEach(id -> ...)
          }); // end forEach(location -> ...)
        }); // end forEach(lengthPuk -> ...)
      }); // end forEach(lengthPuk -> ...)
    }); // end forEach(format -> ...)
    // spotless:on
    // end --- a.
  } // end method */

  /** Test method for {@link CommandApdu#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. smoke test
    // --- b. smoke test with    same   class,    same   content
    // --- c. smoke test with    same   class, different content
    // --- d. smoke test with different class,    same   content

    final CommandApdu dut =
        new ResetRetryCounter(DF_SPECIFIC, 31, EafiPasswordFormat.BCD, "12345", "654321");

    // --- a. smoke test
    {
      final var other =
          new ResetRetryCounter(DF_SPECIFIC, 31, EafiPasswordFormat.BCD, "12345", "654321");

      assertNotSame(dut, other);
      assertTrue(dut.equals(other)); // NOPMD simplify assertion
    } // end --- a.

    Map.ofEntries(
            // --- b. smoke test with    same   class,    same   content
            Map.entry(
                new ResetRetryCounter(DF_SPECIFIC, 31, EafiPasswordFormat.BCD, "12345", "654321"),
                List.of(true, true)),
            // --- c. smoke test with    same   class, different content
            Map.entry(
                new ResetRetryCounter(DF_SPECIFIC, 31, EafiPasswordFormat.BCD, "012345", "7654321"),
                List.of(false, false)),
            // --- d. smoke test with different class,    same   content
            Map.entry(
                new CommandApdu(0x00, ResetRetryCounter.INS, 0x00, 0x9f, "12345f 654321"),
                List.of(false, true)))
        .forEach(
            (obj, result) -> {
              assertNotSame(dut, obj);
              assertEquals(result.get(0), dut.equals(obj));
              if (result.get(1)) {
                assertEquals(Hex.toHexDigits(dut.getBytes()), Hex.toHexDigits(obj.getBytes()));
              } // end fi
            }); // end forEach((obj, result) -> ...)
  } // end method */

  /** Test method for {@link ResetRetryCounter#explainTrailer(int)}. */
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

    final CommandApdu dut =
        new ResetRetryCounter(DF_SPECIFIC, 0, EafiPasswordFormat.BCD, "01234", "543210");

    // --- a. explanation not empty
    List.of(0x6983, 0x6985).forEach(trailer -> assertFalse(dut.explainTrailer(trailer).isEmpty()));

    // --- b. check that some trailers are explained by superclass
    assertEquals("NoError", dut.explainTrailer(0x9000));
  } // end method */

  /** Test method for serialisation. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.CognitiveComplexity"})
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
    // spotless:off
    final List<AbstractAuthenticate> originals = new ArrayList<>();
    Arrays.stream(EafiPasswordFormat.values()).forEach(format -> {
      RNG.intsClosed(0, 14, 5).forEach(lengthPuk -> {
        final boolean isPukEmpty = 0 == lengthPuk;
        final String puk = (format.equals(EafiPasswordFormat.UTF8))
            ? TestEafiPasswordFormat.randomUtf8Secret(lengthPuk)
            : TestEafiPasswordFormat.randomDigitSecret(lengthPuk);

        RNG.intsClosed(0, 14, 5).forEach(lengthPin -> {
          final boolean isPinEmpty = 0 == lengthPin;
          final String pin = (format.equals(EafiPasswordFormat.UTF8))
              ? TestEafiPasswordFormat.randomUtf8Secret(lengthPin)
              : TestEafiPasswordFormat.randomDigitSecret(lengthPin);

          Arrays.stream(AbstractAuthenticate.EafiLocation.values()).forEach(location -> {
            RNG.intsClosed(ID_INFIMUM, ID_SUPREMUM, 5).forEach(id -> {
              final ResetRetryCounter dut;
              if (isPukEmpty && isPinEmpty) {
                // ... command data field absent
                dut = new ResetRetryCounter(location, id);
              } else {
                // ... command data field present
                dut = new ResetRetryCounter(location, id, format, puk, pin);
              } // end else

              originals.add(dut);
            }); // end forEach(id -> ...)
          }); // end forEach(location -> ...)
        }); // end forEach(lengthPuk -> ...)
      }); // end forEach(lengthPuk -> ...)
    }); // end forEach(format -> ...)
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

  /** Test method for {@link ResetRetryCounter#toString()}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_toString() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected
    // ... b. EafiLocation.referenced(int)-method works as expected
    // ... c. EafiPasswordFormat.blind(...)-method works as expected

    // Note: Because of assertions, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over a bunch of valid input values

    // --- a. smoke test
    {
      final var expected =
          "CLA='00'  INS='2c'  P1='00'  P2='83'  Lc='10'  "
              + "Data='26******ffffffff26******ffffffff'  Le absent";
      final var dut =
          new ResetRetryCounter(
              DF_SPECIFIC, 3, EafiPasswordFormat.FORMAT_2_PIN_BLOCK, "123456", "135246");

      final var present = dut.toString();

      assertEquals(expected, present);
    } // end --- a.

    // --- b. loop over a bunch of valid input values
    // spotless:off
    Arrays.stream(EafiPasswordFormat.values()).forEach(format -> {
      RNG.intsClosed(0, 14, 5).forEach(lengthPuk -> {
        final boolean isPukEmpty = 0 == lengthPuk;
        final String puk = (format.equals(EafiPasswordFormat.UTF8))
            ? TestEafiPasswordFormat.randomUtf8Secret(lengthPuk)
            : TestEafiPasswordFormat.randomDigitSecret(lengthPuk);

        RNG.intsClosed(0, 14, 5).forEach(lengthPin -> {
          final boolean isPinEmpty = 0 == lengthPin;
          final String pin = (format.equals(EafiPasswordFormat.UTF8))
              ? TestEafiPasswordFormat.randomUtf8Secret(lengthPin)
              : TestEafiPasswordFormat.randomDigitSecret(lengthPin);

          final int expectedP1;
          if (isPukEmpty) {
            if (isPinEmpty) {
              // ... PUK  and  PIN absent
              expectedP1 = 3;
            } else {
              // ... PUK absent, PIN present
              expectedP1 = 2;
            } // end else
          } else {
            if (isPinEmpty) {
              // ... PUK  present, PIN absent
              expectedP1 = 1;
            } else {
              // ... PUK  and  PIN present
              expectedP1 = 0;
            } // end else
          } // end else (PUK absent?)

          Arrays.stream(AbstractAuthenticate.EafiLocation.values()).forEach(location -> {
            RNG.intsClosed(ID_INFIMUM, ID_SUPREMUM, 5).forEach(id -> {
              final ResetRetryCounter dut;
              final String expected;
              if (isPukEmpty && isPinEmpty) {
                // ... command data field absent
                dut = new ResetRetryCounter(location, id);
                expected = String.format(
                    "CLA='00'  INS='2c'  P1='03'  P2='%02x'  Lc and Data absent  Le absent",
                    location.reference(id));
              } else {
                // ... command data field present
                dut = new ResetRetryCounter(location, id, format, puk, pin);
                expected = String.format(
                    "CLA='00'  INS='2c'  P1='%02x'  P2='%02x'  Lc='%02x'  Data='%s'  Le absent",
                    expectedP1,
                    location.reference(id),
                    dut.getNc(),
                    (isPukEmpty ? "" : format.blind(puk)) + (isPinEmpty ? "" : format.blind(pin))
                );
              } // end else

              assertEquals(expected, dut.toString());
            }); // end forEach(id -> ...)
          }); // end forEach(location -> ...)
        }); // end forEach(lengthPuk -> ...)
      }); // end forEach(lengthPuk -> ...)
    }); // end forEach(format -> ...)
    // spotless:on
    // end --- b.
  } // end method */
} // end class
