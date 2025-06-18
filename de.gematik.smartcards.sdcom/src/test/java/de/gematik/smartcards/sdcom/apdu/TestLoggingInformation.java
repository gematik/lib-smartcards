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
package de.gematik.smartcards.sdcom.apdu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerOctetString;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.tlv.DerPrintableString;
import de.gematik.smartcards.tlv.DerSet;
import de.gematik.smartcards.tlv.DerUtf8String;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

/**
 * Class for white-box testing of {@link Scenario7816.LoggingInformation}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims: "ES_COMPARING_STRINGS_WITH_EQ"
//         Explanation: This code compares java.lang.String objects for reference
//             equality using the == or != operators. Unless both strings
//             are either constants in a source file, or have been interned
//             using the String.intern() method, the same string value may
//             be represented by two different String objects.
//             Consider using the equals(Object) method instead.
//         Rational: The finding is correct. Intentionally, the code is NOT changed.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ES_COMPARING_STRINGS_WITH_EQ" // see note 1
}) // */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle.methodname"})
final class TestLoggingInformation {

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

  /** Test method for {@link Scenario7816.LoggingInformation#LoggingInformation(Level, String)}. */
  @Test
  void test_LoggingInformation__Level_String() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter

    // --- a. smoke test
    {
      final var expLevel = Level.INFO;
      final var expMessage = "4711";

      final var dut = new Scenario7816.LoggingInformation(expLevel, expMessage);

      assertSame(expLevel, dut.getLevel());
      assertSame(expMessage, dut.getMessage());
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    for (final var expLevel : Level.values()) {
      final var expMessage = RNG.nextUtf8(0, 4);

      final var dut = new Scenario7816.LoggingInformation(expLevel, expMessage); // NOPMD new in lo.

      assertSame(expLevel, dut.getLevel());
      assertSame(expMessage, dut.getMessage()); // ES_COMPARING_STRINGS_WITH_EQ
    } // end For (expLevel...)
    // end --- b.

    // --- c. empty message
    {
      final var message = "";
      for (final var expLevel : Level.values()) {
        final var dut = new Scenario7816.LoggingInformation(expLevel, message); // NOPMD new in loop

        assertSame(expLevel, dut.getLevel());
        assertTrue(dut.getMessage().isEmpty());
      } // end For (expLevel...)
    } // end --- c.
  } // end method */

  /** Test method fro {@link Scenario7816.LoggingInformation#LoggingInformation(DerSet)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_LoggingInformation__DerSet() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. the DO for the level is missing
    // --- d. the DO for the message is missing
    // --- e. more than one DO per instance attribute is present
    // --- f. arbitrary DO
    // --- g. empty message

    // --- a. smoke test
    {
      final var expLevel = Level.DEBUG;
      final var expMessage = "0815";
      final var input =
          new DerSet(
              List.of(
                  new DerUtf8String(expMessage),
                  new DerInteger(BigInteger.valueOf(expLevel.toInt()))));

      final var dut = new Scenario7816.LoggingInformation(input);

      assertSame(expLevel, dut.getLevel());
      assertEquals(expMessage, dut.getMessage());
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    for (final var expLevel : Level.values()) {
      final var expMessage = RNG.nextUtf8(0, 4);
      final var input =
          new DerSet(
              List.of(
                  new DerUtf8String(expMessage),
                  new DerInteger(BigInteger.valueOf(expLevel.toInt()))));

      final var dut = new Scenario7816.LoggingInformation(input);

      assertSame(expLevel, dut.getLevel());
      assertEquals(expMessage, dut.getMessage());
    } // end For (expLevel...)
    // end --- b.

    // --- c. the DO for the level is missing
    {
      final var expLevel = Level.TRACE;
      final var expMessage = RNG.nextUtf8(0, 4);
      final var input = new DerSet(List.of(new DerUtf8String(expMessage)));

      final var dut = new Scenario7816.LoggingInformation(input);

      assertSame(expLevel, dut.getLevel());
      assertEquals(expMessage, dut.getMessage());
    } // end --- c.

    // --- d. the DO for the message is missing
    {
      final var expLevel = Level.ERROR;
      final var expMessage = "";
      final var input = new DerSet(List.of(new DerInteger(BigInteger.valueOf(expLevel.toInt()))));

      final var dut = new Scenario7816.LoggingInformation(input);

      assertSame(expLevel, dut.getLevel());
      assertEquals(expMessage, dut.getMessage());
    } // end --- d.

    // --- e. more than one DO per instance attribute is present
    // Note: In a DerSet it is impossible to have the same tag more than once.

    // --- f. arbitrary DO
    {
      final var expLevel = Level.TRACE;
      final var expMessage = "0815 471f";
      final var input =
          new DerSet(
              List.of(
                  new DerOid(AfiOid.ansix9p521r1),
                  new DerUtf8String(expMessage),
                  new DerPrintableString(Hex.toHexDigits(RNG.nextBytes(5))),
                  new DerOctetString(RNG.nextBytes(5)),
                  new DerInteger(BigInteger.valueOf(expLevel.toInt()))));

      final var dut = new Scenario7816.LoggingInformation(input);

      assertSame(expLevel, dut.getLevel());
      assertEquals(expMessage, dut.getMessage());
    } // end --- f.

    // --- g. empty message
    {
      final var message = "";
      for (final var expLevel : Level.values()) {
        final var input =
            new DerSet(
                List.of(
                    new DerUtf8String(message),
                    new DerInteger(BigInteger.valueOf(expLevel.toInt()))));

        final var dut = new Scenario7816.LoggingInformation(input);

        assertSame(expLevel, dut.getLevel());
        assertTrue(dut.getMessage().isEmpty());
      } // end For (expLevel...)
    } // end --- g.
  } // end method */

  /** Test method for {@link Scenario7816.LoggingInformation#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. difference in message
    // --- e. difference in level
    // --- f. different objects same content

    final var defaultLevel = Level.INFO;
    final var defaultMessage = "0816";
    final var dut = new Scenario7816.LoggingInformation(defaultLevel, defaultMessage);

    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    for (final Object[] obj :
        new Object[][] {
          new Object[] {dut, true}, // --- a. same reference
          new Object[] {null, false}, // --- b. null input
          new Object[] {"afi", false}, // --- c. difference in type
        }) {
      assertEquals(obj[1], dut.equals(obj[0]));
    } // end For (obj...)
    // end --- a, b, c.

    // --- d. difference in message
    // --- e. difference in level
    for (final var message : List.of(defaultMessage, "0817", "")) {
      for (final var level : Level.values()) {
        final Object obj = new Scenario7816.LoggingInformation(level, message); // NOPMD new in loop

        assertEquals(defaultLevel.equals(level) && defaultMessage.equals(message), dut.equals(obj));
      } // end For (level...)
    } // end For (message...)
    // end --- d, e.

    // --- f. different objects same content
    {
      final var other = new Scenario7816.LoggingInformation(defaultLevel, defaultMessage);

      assertNotSame(dut, other);
      assertEquals(dut, other);
    } // end --- f.
  } // end method */

  /** Test method for {@link Scenario7816.LoggingInformation#hashCode()}. */
  @Test
  void test_hashCode() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. call method-under-test again
    // --- c. loop over relevant range of input parameter

    // --- a. smoke test
    {
      final var level = Level.INFO;
      final var message = "foo bar";
      final var dut = new Scenario7816.LoggingInformation(level, message);
      final var expected = 31 * level.toInt() + message.hashCode();

      final var actual = dut.hashCode();

      assertEquals(expected, actual);

      // --- b. call method-under-test again
      assertEquals(actual, dut.hashCode());
    } // end --- a, b.

    // --- c. loop over relevant range of input parameter
    for (final var level : Level.values()) {
      final var message = RNG.nextUtf8(0, 4);
      final var dut = new Scenario7816.LoggingInformation(level, message); // NOPMD new in loop

      final var expected = 31 * level.toInt() + message.hashCode();

      final var actual = dut.hashCode();

      assertEquals(expected, actual);
      assertEquals(actual, dut.hashCode());
    } // end For (expLevel...)
    // end --- c.
  } // end method */

  /** Test method for {@link Scenario7816.LoggingInformation#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. empty message

    // --- a. smoke test
    {
      final var level = Level.INFO;
      final var message = "47a1";
      final var dut = new Scenario7816.LoggingInformation(level, message);
      final var expected = String.format("INFO, \"%s\"", message);

      final var actual = dut.toString();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    for (final var level : Level.values()) {
      final var message = RNG.nextUtf8(0, 4);
      final var dut = new Scenario7816.LoggingInformation(level, message); // NOPMD new in loop
      final var expected = String.format("%s, \"%s\"", level, message);

      final var actual = dut.toString();

      assertEquals(expected, actual);
    } // end For (expLevel...)
    // end --- b.

    // --- c. empty message
    {
      final var message = "";
      for (final var level : Level.values()) {
        final var dut = new Scenario7816.LoggingInformation(level, message); // NOPMD new in loop
        final var expected = String.format("%s, \"\"", level);

        final var actual = dut.toString();

        assertEquals(expected, actual);
      } // end For (expLevel...)
    } // end --- c.
  } // end method */

  /** Test method for {@link Scenario7816.LoggingInformation#toTlv()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_toTlv() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. empty message

    // --- a. smoke test
    {
      final var level = Level.ERROR;
      final var message = "48a1";
      final var dut = new Scenario7816.LoggingInformation(level, message);
      final var expected =
          new DerSet(
              List.of(
                  new DerInteger(BigInteger.valueOf(level.toInt())), new DerUtf8String(message)));

      final var actual = dut.toTlv();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    for (final var level : Level.values()) {
      final var message = RNG.nextUtf8(0, 4);
      final var dut = new Scenario7816.LoggingInformation(level, message);
      final var expected =
          new DerSet(
              List.of(
                  new DerInteger(BigInteger.valueOf(level.toInt())), new DerUtf8String(message)));

      final var actual = dut.toTlv();

      assertEquals(expected, actual);
    } // end For (expLevel...)
    // end --- b.

    // --- c. empty message
    {
      final var message = "";
      for (final var level : Level.values()) {
        final var dut = new Scenario7816.LoggingInformation(level, message);
        final var expected =
            new DerSet(
                List.of(
                    new DerInteger(BigInteger.valueOf(level.toInt())), new DerUtf8String(message)));

        final var actual = dut.toTlv();

        assertEquals(expected, actual);
      } // end For (expLevel...)
    } // end --- c.
  } // end method */
} // end class
