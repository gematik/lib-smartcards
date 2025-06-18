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
package de.gematik.smartcards.pcsc.lib;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.jna.Pointer;
import de.gematik.smartcards.sdcom.isoiec7816objects.AnswerToReset;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class performing white-box tests for {@link ScardIoRequest}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "ES_COMPARING_STRINGS_WITH_EQ"
//         Spotbugs message: This code compares java.lang.String objects for
//              reference equality using the == or != operators.
//         Rational: Intentionally assertSame(...) is used there.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "ES_COMPARING_STRINGS_WITH_EQ", // see note 1
}) // */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestScardReaderEafiState {

  /** Random number generator. */
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

  /** Test method for {@link ScardReaderState#ScardReaderState()}. */
  @SuppressWarnings({"PMD.NcssCount"})
  @Test
  void test_ScardReaderState() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing,
    //       so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. check that dwCurrentState can be changed
    // --- c. check that dwEventState can be changed
    // --- d. check that cbAtr can be changed
    // --- e. check that rgbAtr can be changed
    // --- f. check that szReader can be changed
    // --- g. check that pvUserData can be changed

    final ScardReaderState dut = new ScardReaderState();
    final Dword dwCurrentState = dut.dwCurrentState;
    final Dword dwEventState = dut.dwEventState;
    final Dword cbAtr = dut.cbAtr;
    final byte[] rgbAtr = dut.rgbAtr;

    assertSame(dwCurrentState, dwEventState);
    assertSame(dwCurrentState, cbAtr);
    assertArrayEquals(new byte[AnswerToReset.MAX_ATR_SIZE], dut.rgbAtr);

    // --- a. smoke test
    {
      assertNull(dut.szReader);
      assertNull(dut.pvUserData);
      assertEquals(0L, dut.dwCurrentState.longValue());
      assertSame(dwCurrentState, dut.dwCurrentState);
      assertSame(dwEventState, dut.dwEventState);
      assertSame(cbAtr, dut.cbAtr);
      assertSame(rgbAtr, dut.rgbAtr);
    } // end --- a.

    // --- b. check that dwCurrentState can be changed
    final long newCurrentState = RNG.nextInt() & 0x7fffffff;
    {
      dut.dwCurrentState = new Dword(newCurrentState);

      assertNull(dut.szReader);
      assertNull(dut.pvUserData);
      assertNotSame(dwCurrentState, dut.dwCurrentState);
      assertEquals(newCurrentState, dut.dwCurrentState.longValue());
      assertSame(dwEventState, dut.dwEventState);
      assertSame(cbAtr, dut.cbAtr);
      assertSame(rgbAtr, dut.rgbAtr);
    } // end --- b.

    // --- c. check that dwEventState can be changed
    final long newEventState = RNG.nextInt() & 0x7fffffff;
    {
      dut.dwEventState = new Dword(newEventState);

      assertNull(dut.szReader);
      assertNull(dut.pvUserData);
      assertNotSame(dwCurrentState, dut.dwCurrentState);
      assertEquals(newCurrentState, dut.dwCurrentState.longValue());
      assertNotSame(dwEventState, dut.dwEventState);
      assertEquals(newEventState, dut.dwEventState.longValue());
      assertSame(cbAtr, dut.cbAtr);
      assertSame(rgbAtr, dut.rgbAtr);
    } // end --- c.

    // --- d. check that cbAtr can be changed
    final long newCbAtr = RNG.nextInt() & 0x7fffffff;
    {
      dut.cbAtr = new Dword(newCbAtr);

      assertNull(dut.szReader);
      assertNull(dut.pvUserData);
      assertNotSame(dwCurrentState, dut.dwCurrentState);
      assertEquals(newCurrentState, dut.dwCurrentState.longValue());
      assertNotSame(dwEventState, dut.dwEventState);
      assertEquals(newEventState, dut.dwEventState.longValue());
      assertNotSame(cbAtr, dut.cbAtr);
      assertEquals(newCbAtr, dut.cbAtr.longValue());
      assertSame(rgbAtr, dut.rgbAtr);
    } // end --- c.

    // --- e. check that rgbAtr can be changed
    final byte[] newRgbAtr = RNG.nextBytes(1, 33);
    {
      dut.rgbAtr = newRgbAtr;

      assertNull(dut.szReader);
      assertNull(dut.pvUserData);
      assertNotSame(dwCurrentState, dut.dwCurrentState);
      assertEquals(newCurrentState, dut.dwCurrentState.longValue());
      assertNotSame(dwEventState, dut.dwEventState);
      assertEquals(newEventState, dut.dwEventState.longValue());
      assertNotSame(cbAtr, dut.cbAtr);
      assertEquals(newCbAtr, dut.cbAtr.longValue());
      assertNotSame(rgbAtr, dut.rgbAtr);
      assertSame(newRgbAtr, dut.rgbAtr);
    } // end --- e.

    // --- f. check that szReader can be changed
    final String newSzReader = RNG.nextUtf8(1, 10);
    {
      dut.szReader = newSzReader;

      assertSame(newSzReader, dut.szReader); // spotbugs: ES_COMPARING_STRINGS_WITH_EQ
      assertNull(dut.pvUserData);
      assertNotSame(dwCurrentState, dut.dwCurrentState);
      assertEquals(newCurrentState, dut.dwCurrentState.longValue());
      assertNotSame(dwEventState, dut.dwEventState);
      assertEquals(newEventState, dut.dwEventState.longValue());
      assertNotSame(cbAtr, dut.cbAtr);
      assertEquals(newCbAtr, dut.cbAtr.longValue());
      assertNotSame(rgbAtr, dut.rgbAtr);
      assertSame(newRgbAtr, dut.rgbAtr);
    } // end --- f.

    // --- g. check that pvUserData can be changed
    final Pointer newPvUserDate = Pointer.createConstant(RNG.nextInt() & 0x7fffffff);
    {
      dut.pvUserData = newPvUserDate;

      assertSame(newSzReader, dut.szReader); // spotbugs: ES_COMPARING_STRINGS_WITH_EQ
      assertSame(newPvUserDate, dut.pvUserData);
      assertNotSame(dwCurrentState, dut.dwCurrentState);
      assertEquals(newCurrentState, dut.dwCurrentState.longValue());
      assertNotSame(dwEventState, dut.dwEventState);
      assertEquals(newEventState, dut.dwEventState.longValue());
      assertNotSame(cbAtr, dut.cbAtr);
      assertEquals(newCbAtr, dut.cbAtr.longValue());
      assertNotSame(rgbAtr, dut.rgbAtr);
      assertSame(newRgbAtr, dut.rgbAtr);
    } // end --- f.
  } // end method */

  /** Test method for {@link ScardReaderState#createArray(List, boolean)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_createArray__List_boolean() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all possibilities of flag "withPnp"
    // --- c. loop over various list lengths

    final byte[] defaultAtr = new byte[AnswerToReset.MAX_ATR_SIZE];

    // --- a. smoke test
    {
      final List<String> input = List.of("foo", "afiBar");

      final ScardReaderState[] present = ScardReaderState.createArray(input, true);

      assertEquals(1 + input.size(), present.length);
      assertEquals(WinscardLibrary.PNP_READER_ID, present[0].szReader);
      assertEquals(input.get(0), present[1].szReader);
      assertEquals(input.get(1), present[2].szReader);
      for (int i = present.length; i-- > 0; ) { // NOPMD assignment in operand
        final ScardReaderState srs = present[i];
        assertNull(srs.pvUserData);
        assertEquals(0L, srs.dwCurrentState.longValue());
        assertEquals(0L, srs.dwEventState.longValue());
        assertArrayEquals(defaultAtr, srs.rgbAtr);
        assertEquals(0L, srs.cbAtr.longValue());

        if (0 == i) {
          assertSame(srs.dwCurrentState, srs.dwEventState, () -> srs.szReader);
          assertSame(srs.dwCurrentState, srs.cbAtr, () -> srs.szReader);
          assertSame(srs.dwEventState, srs.cbAtr, () -> srs.szReader);
        } else {
          assertNotSame(srs.dwCurrentState, srs.dwEventState, () -> srs.szReader);
          assertNotSame(srs.dwCurrentState, srs.cbAtr, () -> srs.szReader);
          assertNotSame(srs.dwEventState, srs.cbAtr, () -> srs.szReader);
        } // end else
      } // end For (i...)
    } // end --- a.

    // --- b. loop over all possibilities of flag "withPnp"
    // --- c. loop over various list lengths
    RNG.intsClosed(0, 20, 6)
        .forEach(
            noReaders -> {
              final List<String> readerNames =
                  IntStream.range(0, noReaders).mapToObj(i -> RNG.nextUtf8(1, 50)).toList();
              assertEquals(noReaders, readerNames.size());

              List.of(true, false)
                  .forEach(
                      withPnp -> {
                        if (!withPnp && readerNames.isEmpty()) {
                          assertThrows(
                              ArrayIndexOutOfBoundsException.class,
                              () -> ScardReaderState.createArray(readerNames, withPnp));
                        } else {
                          final ScardReaderState[] present =
                              ScardReaderState.createArray(readerNames, withPnp);

                          // check size of "present"
                          assertEquals((withPnp ? 1 : 0) + readerNames.size(), present.length);

                          // check instance attribute szReader
                          int i = 0;
                          if (withPnp) {
                            assertEquals(WinscardLibrary.PNP_READER_ID, present[i++].szReader);
                          } // end fi
                          for (final String name : readerNames) {
                            assertSame(name, present[i++].szReader); // ES_COMPARING_STRINGS_WITH_EQ
                          } // end For (name...)

                          // check other instance attributes
                          for (i = present.length; i-- > 0; ) { // NOPMD assignment in operand
                            final ScardReaderState srs = present[i];
                            assertNull(srs.pvUserData);
                            assertEquals(0L, srs.dwCurrentState.longValue());
                            assertEquals(0L, srs.dwEventState.longValue());
                            assertArrayEquals(defaultAtr, srs.rgbAtr);
                            assertEquals(0L, srs.cbAtr.longValue());

                            if (0 == i) {
                              assertSame(srs.dwCurrentState, srs.dwEventState, () -> srs.szReader);
                              assertSame(srs.dwCurrentState, srs.cbAtr, () -> srs.szReader);
                              assertSame(srs.dwEventState, srs.cbAtr, () -> srs.szReader);
                            } else {
                              assertNotSame(
                                  srs.dwCurrentState, srs.dwEventState, () -> srs.szReader);
                              assertNotSame(srs.dwCurrentState, srs.cbAtr, () -> srs.szReader);
                              assertNotSame(srs.dwEventState, srs.cbAtr, () -> srs.szReader);
                            } // end else
                          } // end For (i...)
                        } // end else
                      }); // end forEach(withPnp -> ...)
            }); // end forEach(noReaders -> ...)
  } // end method */

  /** Test method for {@link ScardReaderState#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. toString(long)-method works as expected

    // Note: Simple method does not need extensive testing,
    //       so we can be lazy here

    // Test strategy:
    // --- a. smoke test
    {
      final ScardReaderState dut = new ScardReaderState();
      dut.szReader = "fooA";
      dut.dwCurrentState = new Dword(0x42);
      dut.dwEventState = new Dword(42);
      dut.cbAtr = new Dword(3);
      dut.rgbAtr = Hex.toByteArray("3b00223344");

      assertEquals(
          "szReader=fooA, pvUserData=null,"
              + " cS=0x0000-0042=0-[changed, atrmatch],"
              + " eS=0x0000-002a=0-[changed, unavailable, present],"
              + " atrSize= 3, ATR='3b00223344'",
          dut.toString());
    } // end --- a.
  } // end method */

  /** Test method for {@link ScardReaderState#toString()}. */
  @Test
  void test_toString__String() {
    // Assertions:
    // ... a. toString(long)-method works as expected

    // Note: Simple method does not need extensive testing,
    //       so we can be lazy here

    // Test strategy:
    // --- a. smoke test
    // --- b. test with various lengths for readerName

    final ScardReaderState dut = new ScardReaderState();
    dut.szReader = "fooA";
    dut.dwCurrentState = new Dword(0x42);
    dut.dwEventState = new Dword(42);
    dut.cbAtr = new Dword(3);
    dut.rgbAtr = Hex.toByteArray("3b00223344");

    // --- a. smoke test
    {
      assertEquals(
          "szReader=fooA , pvUserData=null,"
              + " cS=0x0000-0042=0-[changed, atrmatch],"
              + " eS=0x0000-002a=0-[changed, unavailable, present],"
              + " atrSize= 3, ATR='3b00223344'",
          dut.toString("szReader=%-5s" + ScardReaderState.SUFFIX));
    } // end --- a.

    // --- b. test with various lengths for readerName
    RNG.intsClosed(1, 10, 5)
        .forEach(
            maxLengthName -> {
              final String formatter =
                  "szReader=%-" + maxLengthName + "s" + ScardReaderState.SUFFIX;

              assertEquals(
                  String.format(
                      "szReader=%s, pvUserData=null,"
                          + " cS=0x0000-0042=0-[changed, atrmatch],"
                          + " eS=0x0000-002a=0-[changed, unavailable, present],"
                          + " atrSize= 3, ATR='3b00223344'",
                      String.format("%-" + maxLengthName + "s", "fooA")),
                  dut.toString(formatter));
            }); // end forEach(maxLengthName -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link ScardReaderState#toString(ScardReaderState[])}. */
  @Test
  void test_toString__ScardReaderStateA() {
    // Assertions:
    // ... a. toString(String)-method works as expected
    // ... b. toString(int)-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. length = 0
    // --- c. name = null

    // --- a. smoke test
    {
      final String szReaderA = "foa";
      final Dword dwCurrentStateA = new Dword(42);
      final Dword dwEventStateA = new Dword(15);
      final String rgAtrA = "af0102030405060708090a0b0c0d0e0f";
      final Dword cbAtrA = new Dword(7);
      final ScardReaderState scardReaderStateA = new ScardReaderState();
      scardReaderStateA.szReader = szReaderA;
      scardReaderStateA.dwCurrentState = dwCurrentStateA;
      scardReaderStateA.dwEventState = dwEventStateA;
      scardReaderStateA.rgbAtr = Hex.toByteArray(rgAtrA);
      scardReaderStateA.cbAtr = cbAtrA;

      final String szReaderB = "afifooBar";
      final Dword dwCurrentStateB = new Dword(0x42);
      final Dword dwEventStateB = new Dword(0x15);
      final String rgAtrB = "fa1e1d1c1b1a191817161514131211";
      final Dword cbAtrB = new Dword(3);
      final ScardReaderState scardReaderStateB = new ScardReaderState();
      scardReaderStateB.szReader = szReaderB;
      scardReaderStateB.dwCurrentState = dwCurrentStateB;
      scardReaderStateB.dwEventState = dwEventStateB;
      scardReaderStateB.rgbAtr = Hex.toByteArray(rgAtrB);
      scardReaderStateB.cbAtr = cbAtrB;

      final String present = ScardReaderState.toString(scardReaderStateA, scardReaderStateB);

      final String formatter = "szReader=%-9s" + ScardReaderState.SUFFIX;
      assertEquals(
          String.format(
              "[%n  1.: %s%n  2.: %s%n]",
              scardReaderStateA.toString(formatter), scardReaderStateB.toString(formatter)),
          present);
    } // end --- a.

    // --- b. length = 0
    assertEquals(String.format("[%n%n]"), ScardReaderState.toString(new ScardReaderState[0]));

    // --- c. name = null
    {
      final String szReaderA = "foc";
      final Dword dwCurrentStateA = new Dword(42);
      final Dword dwEventStateA = new Dword(15);
      final String rgAtrA = "af0102030405060708090a0b0c0d0e0f";
      final Dword cbAtrA = new Dword(7);
      final ScardReaderState scardReaderStateA = new ScardReaderState();
      scardReaderStateA.szReader = szReaderA;
      scardReaderStateA.dwCurrentState = dwCurrentStateA;
      scardReaderStateA.dwEventState = dwEventStateA;
      scardReaderStateA.rgbAtr = Hex.toByteArray(rgAtrA);
      scardReaderStateA.cbAtr = cbAtrA;

      // final String szReaderB = "afifooBar";
      final Dword dwCurrentStateB = new Dword(0x42);
      final Dword dwEventStateB = new Dword(0x15);
      final String rgAtrB = "fa1e1d1c1b1a191817161514131211";
      final Dword cbAtrB = new Dword(3);
      final ScardReaderState scardReaderStateB = new ScardReaderState();
      // scardReaderStateB.szReader = szReaderB; // => szReader remains "null"
      scardReaderStateB.dwCurrentState = dwCurrentStateB;
      scardReaderStateB.dwEventState = dwEventStateB;
      scardReaderStateB.rgbAtr = Hex.toByteArray(rgAtrB);
      scardReaderStateB.cbAtr = cbAtrB;

      final String present = ScardReaderState.toString(scardReaderStateA, scardReaderStateB);

      final String formatter = "szReader=%-3s" + ScardReaderState.SUFFIX;
      assertEquals(
          String.format(
              "[%n  1.: %s%n  2.: %s%n]",
              scardReaderStateA.toString(formatter), scardReaderStateB.toString(formatter)),
          present);
    } // end --- c.
  } // end method */

  /** Test method for {@link ScardReaderState#toString(int)}. */
  @Test
  void test_toString__long() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. check for each value from WinscardLibrary
    // --- b. check for all values from WinscardLibrary
    // --- c. check in case other bits are set

    // --- a. check for each value from WinscardLibrary
    {
      assertTrue(ScardReaderState.toString(0).isEmpty());
      assertEquals(List.of("ignore"), ScardReaderState.toString(0x0001));
      assertEquals(List.of("changed"), ScardReaderState.toString(0x0002));
      assertEquals(List.of("unknown"), ScardReaderState.toString(0x0004));
      assertEquals(List.of("unavailable"), ScardReaderState.toString(0x0008));
      assertEquals(List.of("empty"), ScardReaderState.toString(0x0010));
      assertEquals(List.of("present"), ScardReaderState.toString(0x0020));
      assertEquals(List.of("atrmatch"), ScardReaderState.toString(0x0040));
      assertEquals(List.of("exclusive"), ScardReaderState.toString(0x0080));
      assertEquals(List.of("inuse"), ScardReaderState.toString(0x0100));
      assertEquals(List.of("mute"), ScardReaderState.toString(0x0200));
      assertEquals(List.of("unpowered"), ScardReaderState.toString(0x0400));
    } // end --- a.

    // --- b. check for all values from WinscardLibrary
    final int supremum = 0x7ff;
    {
      assertEquals(
          List.of(
              "ignore",
              "changed",
              "unknown",
              "unavailable",
              "empty",
              "present",
              "atrmatch",
              "exclusive",
              "inuse",
              "mute",
              "unpowered"),
          ScardReaderState.toString(supremum));
    } // end --- b.

    // --- c. check in case other bits are set
    List.of(supremum + 1, 0xfffff800)
        .forEach(input -> assertTrue(ScardReaderState.toString(input).isEmpty()));
    // end --- c.
  } // end method */
} // end class
