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

import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NC_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NC_SUPREMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_EXTENDED_WILDCARD;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_INFIMUM;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box testing for {@link IoChallenge}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestIoChallenge {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestIoChallenge.class);

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

  /** Test method for {@link IoChallenge#calculateP1P2(long)}. */
  @Test
  void test_calculateP1P2__long() {
    // Assertions:
    // ... a. calculateDelay(int, int)-method works as expected

    // Test strategy:
    // --- a. minimum value
    // --- b. maximum value
    // --- c. below minimum
    // --- d. above maximum
    // --- e. manually chosen values
    // --- f. random values with exact conversion
    // --- g. random values which needed rounding

    final var maxInput = 4_396_972_769_280L;

    // --- a. minimum value
    {
      final var input = 0;
      final var expected = 0;

      final var actual = IoChallenge.calculateP1P2(input);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. maximum value
    {
      final var expected = (short) 0xffff;

      final var actual = IoChallenge.calculateP1P2(maxInput);

      assertEquals(expected, actual);
    } // end --- b.

    // --- c. below minimum
    Set.of(-1L, Long.MIN_VALUE)
        .forEach(delay -> assertEquals(0, IoChallenge.calculateP1P2(delay))); // end --- c.

    // --- d. above maximum
    Set.of(maxInput + 1, Long.MAX_VALUE)
        .forEach(
            delay -> assertEquals((short) 0xffff, IoChallenge.calculateP1P2(delay))); // end --- d.

    // --- e. manually chosen values
    //            delay, p1p2
    Map.ofEntries(
            Map.entry(0, 0x0000),
            Map.entry(1, 0x0020),
            Map.entry(2047, 0xffe0),
            Map.entry(2048, 0x0001),
            Map.entry(2049, 0x0021),
            Map.entry(4095, 0xffe1),
            Map.entry(4096, 0x0002),
            Map.entry(8191, 0xffe2),
            Map.entry(8192, 0x0003),
            Map.entry(8193, 0x0003),
            Map.entry(8194, 0x0023), // round up
            Map.entry(8195, 0x0023), // round up
            Map.entry(8196, 0x0023), // exact value
            Map.entry(8197, 0x0023), // round down
            Map.entry(8198, 0x0043) // round up
            )
        .forEach(
            (input, expected) -> {
              final short present = IoChallenge.calculateP1P2(input);

              assertEquals(expected.shortValue(), present);
            }); // end forEach((input, expected) -> ...)

    // --- f. random values with exact conversion
    RNG.intsClosed(0, 0xffff, 1024)
        .forEach(
            expected -> {
              final long delay = IoChallenge.calculateDelay(expected >> 8, expected & 0xff);

              final short actual = IoChallenge.calculateP1P2(delay);

              assertEquals((short) expected, actual);
            }); // end forEach(delay -> ...)
    // end --- f.

    // --- g. random values which needed rounding
    /*/ fixed manually chosen values
    {
      final long mantissa = 3;
      final int exponent = 0;
      final int p1p2Down = (int) (((mantissa - 1) << 5) + exponent);
      final short p1p2Exact = (short) ((mantissa << 5) + exponent);
      final int p1p2Up = (int) (((mantissa + 1) << 5) + exponent);
      final long delayDown = IoChallenge.calculateDelay(p1p2Down >> 8, p1p2Down & 0xff);
      final long delayExact = IoChallenge.calculateDelay(p1p2Exact >> 8, p1p2Exact & 0xff);
      final long delayUp = IoChallenge.calculateDelay(p1p2Up >> 8, p1p2Up & 0xff);

      System.out.printf(
          "delayDown = %d, delayUp = %d, delta = %d%n",
          delayDown, delayUp, delayUp - delayDown
      );

      for (long delay = delayDown; delay < delayUp; delay++) {
        final short present = IoChallenge.calculateP1P2(delay);

        if (p1p2Exact == present) {
          final long delta = Math.abs(delay - delayExact);
          assertTrue(delta <= (delay - delayDown));
          assertTrue(delta <= (delayUp - delay));
        } // end fi
      } // end For (delay...)
    } // */
    // spotless:off
    RNG.intsClosed(0, 31, 10).forEach(exponent -> {
      RNG.intsClosed(1, 2046, 20).forEach(mantissa -> {
        final int p1p2Down = ((mantissa - 1) << 5) + exponent;
        final int p1p2Exact = ((mantissa << 5) + exponent);
        final int p1p2Up = ((mantissa + 1) << 5) + exponent;
        final long delayDown = IoChallenge.calculateDelay(p1p2Down >> 8, p1p2Down & 0xff);
        final long delayExact = IoChallenge.calculateDelay(p1p2Exact >> 8, p1p2Exact & 0xff);
        final long delayUp = IoChallenge.calculateDelay(p1p2Up >> 8, p1p2Up & 0xff);

        if (delayUp < Integer.MAX_VALUE) {
          // ... all values in int-range
          RNG.intsClosed((int) delayDown, (int) delayUp, 1024).forEach(delay -> {
            final short present = IoChallenge.calculateP1P2(delay);

            if (p1p2Exact == present) {
              final long delta = Math.abs(delay - delayExact);

              assertTrue(
                  delta <= (delay - delayDown),
                  () -> String.format(
                      "m=%d, e=%d, dD=%d, dE=%d, dU=%d, d=%d, D=%d, Dd=%d",
                      mantissa,
                      exponent,
                      delayDown,
                      delayExact,
                      delayUp,
                      delay,
                      delta,
                      delay - delayDown));
              assertTrue(delta <= (delayUp - delay));
            } // end fi
          }); // end forEach(delay -> ...)
        } else {
          // ... at least delayUp out of int-range
          IntStream.range(0, 1024).forEach(i -> {
            final long delay = RNG.nextLong(delayDown, delayUp);
            final short present = IoChallenge.calculateP1P2(delay);

            if (p1p2Exact == present) {
              final long delta = Math.abs(delay - delayExact);

              assertTrue(delta <= (delay - delayDown));
              assertTrue(delta <= (delayUp - delay));
            } // end fi
          }); // end forEach(i -> ...)
        } // end else
      }); // end forEach(mantissa -> ...)
    }); // end forEach(exponent -> ...)
    // spotless:on
    // end --- g.
  } // end method */

  /*
   * Test method for {@link IoChallenge#calculateDelay(int, int)}.
   *
  @Test
  void test_calculateDelay__int_int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. minimum value
    // --- b. maximum value
    // --- c. random values
    // --- d. values from examples in JavaDoc

    // --- a. minimum value
    {
      assertEquals(0, IoChallenge.calculateDelay(0, 0));
    } // end --- a.

    // --- b. maximum value
    {
      assertEquals(4_396_972_769_280L, IoChallenge.calculateDelay(0xff, 0xff));
    } // end --- b.

    // --- c. random values
    RNG.intsClosed(0, 0xffff, 1024).forEach(delay -> {
      final long mantissa = (delay >> 5) & 0x7ff;
      final int exponent = (delay & 0x1f) - 1;
      final long expected = (exponent < 0)
          ? mantissa
          : (2048 + mantissa << exponent);

      assertEquals(expected, IoChallenge.calculateDelay(delay >> 8, delay & 0xff));
    }); // end forEach(delay -> ...)
    // end --- c.

    // --- d. values from examples in JavaDoc
    Map.ofEntries(
        Map.entry(0x0000, "0 ns"),
        Map.entry(0x0020, "1 ns"),
        Map.entry(0xffe0, String.format("%.3f us", 2.047)),
        Map.entry(0x0001, String.format("%.3f us", 2.048)),
        Map.entry(0x0021, String.format("%.3f us", 2.049)),
        Map.entry(0xffe1, String.format("%.3f us", 4.095)),
        Map.entry(0x001f, String.format("36' %.3f\"", 39.023)),
        Map.entry(0x003f, String.format("36' %.3f\"", 40.097)),
        Map.entry(0xffdf, String.format(" 1h 13' %.3f\"", 15.899)),
        Map.entry(0xffff, String.format(" 1h 13' %.3f\"", 16.973))
    ).forEach((delay, expected) ->
        assertEquals(
            expected,
            AfiUtils.nanoSeconds2Time(
                IoChallenge.calculateDelay(delay >> 8, delay & 0xff)
            )
        )
    ); // end forEach((delay, expected) -> ...)
  } // end method */

  /*
   * Test method for {@link IoChallenge#calculateDelay(int, int)}.
   *
   * <p><i><b>
   * Note:</b> Here the mantissa contains 12 bit and the exponent 4 bit.</i>
   *
  @Test
  void test_calculateDelay__int_int() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. minimum value
    // --- b. maximum value
    // --- c. random values
    // --- d. values from examples in JavaDoc

    // --- a. minimum value
    {
      assertEquals(0, IoChallenge.calculateDelay(0, 0));
    } // end --- a.

    // --- b. maximum value
    {
      // FIXME
    } // end --- b.

    // --- c. random values
    RNG.intsClosed(0, 0xffff, 1024).forEach(delay -> {
      final long mantissa = (delay >> 4) & 0xfff;
      final int exponent = (delay & 0xf) - 1;
      final long expected = (exponent < 0)
          ? mantissa
          : (4096 + mantissa << exponent);

      assertEquals(expected, IoChallenge.calculateDelay(delay >> 8, delay & 0xff));
    }); // end forEach(delay -> ...)
    // end --- c.

    // --- d. values from examples in JavaDoc
    Map.ofEntries(
        Map.entry(0x0000, "0 ns"),
        Map.entry(0x0010, String.format("%.3f us", 1.0)),
        Map.entry(0xfff0, String.format("%.6f ms", 4.095)),
        Map.entry(0x0001, String.format("%.6f ms", 4.096)),
        Map.entry(0xffff, String.format(" 2' %.3f\"", 14.201))
    ).forEach((delay, expected) ->
        assertEquals(
            expected,
            AfiUtils.nanoSeconds2Time(
                1000 * IoChallenge.calculateDelay(delay >> 8, delay & 0xff)
            )
        )
    ); // end forEach((delay, expected) -> ...)
  } // end method */

  /** Test method for {@link IoChallenge#IoChallenge(int, int, int)}. */
  @Test
  void test_IoChallenge__int_int_int() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int p1 = RNG.nextIntClosed(0, 0xff);
    final int p2 = RNG.nextIntClosed(0, 0xff);

    final IoChallenge dut = new IoChallenge(cla, ins, (p1 << 8) + p2);

    assertEquals(cla, dut.getCla());
    assertEquals(ins, dut.getIns());
    assertEquals(p1, dut.getP1());
    assertEquals(p2, dut.getP2());
    assertEquals(1, dut.getCase());
  } // end method */

  /** Test method for {@link IoChallenge#IoChallenge(int, int, int, int)}. */
  @Test
  void test_IoChallenge__int_int_int_int() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int p1 = RNG.nextIntClosed(0, 0xff);
    final int p2 = RNG.nextIntClosed(0, 0xff);
    final int ne = RNG.nextIntClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD);

    final IoChallenge dut = new IoChallenge(cla, ins, (p1 << 8) + p2, ne);

    assertEquals(cla, dut.getCla());
    assertEquals(ins, dut.getIns());
    assertEquals(p1, dut.getP1());
    assertEquals(p2, dut.getP2());
    assertEquals(ne, dut.getNe());
    assertEquals(2, dut.getCase());
  } // end method */

  /** Test method for {@link IoChallenge#IoChallenge(int, int, int, byte[])}. */
  @Test
  void test_IoChallenge__int_int_int_byteA() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int p1 = RNG.nextIntClosed(0, 0xff);
    final int p2 = RNG.nextIntClosed(0, 0xff);
    final byte[] data = RNG.nextBytes(NC_INFIMUM, NC_SUPREMUM);

    final IoChallenge dut = new IoChallenge(cla, ins, (p1 << 8) + p2, data);

    assertEquals(cla, dut.getCla());
    assertEquals(ins, dut.getIns());
    assertEquals(p1, dut.getP1());
    assertEquals(p2, dut.getP2());
    assertArrayEquals(data, dut.getData());
    assertEquals(3, dut.getCase());
  } // end method */

  /** Test method for {@link IoChallenge#IoChallenge(int, int, int, byte[], int)}. */
  @Test
  void test_IoChallenge__int_int_int_byteA_int() {
    // Assertions:
    // - none -

    // Note: This simple constructor does not need extensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int p1 = RNG.nextIntClosed(0, 0xff);
    final int p2 = RNG.nextIntClosed(0, 0xff);
    final byte[] data = RNG.nextBytes(NC_INFIMUM, NC_SUPREMUM);
    final int ne = RNG.nextIntClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD);

    final IoChallenge dut = new IoChallenge(cla, ins, (p1 << 8) + p2, data, ne);

    assertEquals(cla, dut.getCla());
    assertEquals(ins, dut.getIns());
    assertEquals(p1, dut.getP1());
    assertEquals(p2, dut.getP2());
    assertArrayEquals(data, dut.getData());
    assertEquals(ne, dut.getNe());
    assertEquals(4, dut.getCase());
  } // end method */

  /** Test method for {@link IoChallenge#getInstance(CommandApdu)}. */
  @Test
  void test_getInstance__CommandApdu() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. to

    // Test method:
    // --- a. smoke test with ISO case 1
    // --- b. smoke test with ISO case 2
    // --- c. smoke test with ISO case 3
    // --- d. smoke test with ISO case 4

    // --- a. smoke test with ISO case 1
    {
      final CommandApdu expected = new EraseBinary(3, 7);

      final CommandApdu present = IoChallenge.getInstance(expected);

      assertEquals(1, expected.getCase());
      assertArrayEquals(expected.getBytes(), present.getBytes());
      assertNotEquals(expected, present);
    } // end --- a.

    // --- b. smoke test with ISO case 2
    {
      final CommandApdu expected = new ReadBinary(7, 20, NE_EXTENDED_WILDCARD);

      final CommandApdu present = IoChallenge.getInstance(expected);

      assertEquals(2, expected.getCase());
      assertArrayEquals(expected.getBytes(), present.getBytes());
      assertNotEquals(expected, present);
    } // end --- b.

    // --- c. smoke test with ISO case 3
    {
      final CommandApdu expected = new UpdateBinary(5, 100, RNG.nextBytes(16, 32));

      final CommandApdu present = IoChallenge.getInstance(expected);

      assertEquals(3, expected.getCase());
      assertArrayEquals(expected.getBytes(), present.getBytes());
      assertNotEquals(expected, present);
    } // end --- c.

    // --- d. smoke test with ISO case 4
    {
      final CommandApdu expected =
          new CommandApdu(
              0x73,
              0x54,
              0x12,
              0x45,
              RNG.nextBytes(32, 64),
              RNG.nextIntClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD));

      final CommandApdu present = IoChallenge.getInstance(expected);

      assertEquals(4, expected.getCase());
      assertArrayEquals(expected.getBytes(), present.getBytes());
      assertNotEquals(expected, present);
    } // end --- d.
  } // end method */

  /** Test method for {@link IoChallenge#random(long, long, int, int)}. */
  @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity", "PMD.NcssCount"})
  @Test
  void test_random__long_long_int_int() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. test with ISO cases {1}
    // --- b. test with ISO cases {2}
    // --- c. test with ISO cases {3}
    // --- d. test with ISO cases {4}
    // --- e. limited delay range
    // --- f. empty delay range
    // --- g. ERROR: minDelay > maxDelay

    final List<Integer> p1p2List =
        new ArrayList<>(IntStream.rangeClosed(0x0000, 0xffff).boxed().toList());

    // --- a. test with ISO cases {1}
    {
      final long minDelay = 0;
      final long maxDelay = Long.MAX_VALUE;
      final int maxNc = 0;
      final int maxNe = 0;
      final List<Integer> claList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());
      final List<Integer> insList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());

      int counter;
      for (counter = 0; !claList.isEmpty() || !insList.isEmpty(); counter++) {
        final IoChallenge dut = IoChallenge.random(minDelay, maxDelay, maxNc, maxNe);

        assertEquals(1, dut.getCase());
        claList.remove(Integer.valueOf(dut.getCla()));
        insList.remove(Integer.valueOf(dut.getIns()));
        p1p2List.remove(Integer.valueOf((dut.getP1() << 8) + dut.getP2()));
      } // end For (counter...)
      LOGGER.atTrace().log("counter.a = {}", counter);
    } // end --- a.

    // --- b. test with ISO cases {2}
    {
      final long minDelay = 0;
      final long maxDelay = Long.MAX_VALUE;
      final int maxNc = 0;
      final Set<Integer> isoCases = Set.of(1, 2);
      final List<Integer> claList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());
      final List<Integer> insList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());

      int counter;
      for (counter = 0; !claList.isEmpty() || !insList.isEmpty(); counter++) {
        final IoChallenge dut = IoChallenge.random(minDelay, maxDelay, maxNc, NE_EXTENDED_WILDCARD);

        assertTrue(isoCases.contains(dut.getCase()));
        claList.remove(Integer.valueOf(dut.getCla()));
        insList.remove(Integer.valueOf(dut.getIns()));
        p1p2List.remove(Integer.valueOf((dut.getP1() << 8) + dut.getP2()));
      } // end For (counter...)
      LOGGER.atTrace().log("counter.b = {}", counter);
    } // end --- b.

    // --- c. test with ISO cases {3}
    {
      final long minDelay = 0;
      final long maxDelay = Long.MAX_VALUE;
      final int maxNe = 0;
      final Set<Integer> isoCases = Set.of(1, 3);
      final List<Integer> claList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());
      final List<Integer> insList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());

      int counter;
      for (counter = 0; !claList.isEmpty() || !insList.isEmpty(); counter++) {
        final IoChallenge dut = IoChallenge.random(minDelay, maxDelay, NC_SUPREMUM, maxNe);

        assertTrue(isoCases.contains(dut.getCase()));
        claList.remove(Integer.valueOf(dut.getCla()));
        insList.remove(Integer.valueOf(dut.getIns()));
        p1p2List.remove(Integer.valueOf((dut.getP1() << 8) + dut.getP2()));
      } // end For (counter...)
      LOGGER.atTrace().log("counter.c = {}", counter);
    } // end --- c.

    // --- d. test with ISO cases {4}
    {
      final long minDelay = 0;
      final long maxDelay = Long.MAX_VALUE;
      final List<Integer> claList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());
      final List<Integer> insList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());

      int counter;
      for (counter = 0; !claList.isEmpty() || !insList.isEmpty(); counter++) {
        final IoChallenge dut =
            IoChallenge.random(
                minDelay, maxDelay,
                NC_SUPREMUM, NE_EXTENDED_WILDCARD);

        claList.remove(Integer.valueOf(dut.getCla()));
        insList.remove(Integer.valueOf(dut.getIns()));
        p1p2List.remove(Integer.valueOf((dut.getP1() << 8) + dut.getP2()));
      } // end For (counter...)
      LOGGER.atTrace().log("counter.d = {}", counter);
    } // end --- d.

    // --- e. limited delay range
    {
      final long minDelay = 1024;
      final long maxDelay = 4096;
      final List<Integer> claList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());
      final List<Integer> insList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());

      int counter;
      for (counter = 0; !claList.isEmpty() || !insList.isEmpty(); counter++) {
        final IoChallenge dut =
            IoChallenge.random(
                minDelay, maxDelay,
                NC_SUPREMUM, NE_EXTENDED_WILDCARD);

        assertTrue(minDelay <= dut.getDelay());
        assertTrue(dut.getDelay() <= maxDelay);

        claList.remove(Integer.valueOf(dut.getCla()));
        insList.remove(Integer.valueOf(dut.getIns()));
        p1p2List.remove(Integer.valueOf((dut.getP1() << 8) + dut.getP2()));
      } // end For (counter...)
      LOGGER.atTrace().log("counter.e = {}", counter);
    } // end --- e.

    // --- f. empty delay range
    {
      final long minDelay = IoChallenge.calculateDelay(0x00, 0x1f) + 1;
      final long maxDelay = IoChallenge.calculateDelay(0x00, 0x3f) - 1;
      final int p1p2 = IoChallenge.calculateP1P2(maxDelay);
      final int p1 = p1p2 >> 8;
      final int p2 = p1p2 & 0xff;
      final List<Integer> claList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());
      final List<Integer> insList =
          new ArrayList<>(IntStream.rangeClosed(0x00, 0xfe).boxed().toList());

      int counter;
      for (counter = 0; !claList.isEmpty() || !insList.isEmpty(); counter++) {
        final IoChallenge dut =
            IoChallenge.random(
                minDelay, maxDelay,
                NC_SUPREMUM, NE_EXTENDED_WILDCARD);

        assertEquals(p1, dut.getP1());
        assertEquals(p2, dut.getP2());

        claList.remove(Integer.valueOf(dut.getCla()));
        insList.remove(Integer.valueOf(dut.getIns()));
        p1p2List.remove(Integer.valueOf((dut.getP1() << 8) + dut.getP2()));
      } // end For (counter...)
      LOGGER.atTrace().log("counter.f = {}", counter);
    } // end --- f.

    // --- g. ERROR: minDelay > maxDelay
    {
      assertThrows(IllegalArgumentException.class, () -> IoChallenge.random(8192, 8191, 1, 2));
    } // end --- g.

    LOGGER.atDebug().log("p1p2.size = {}", p1p2List.size());
  } // end method */

  /** Test method for {@link IoChallenge#getDelay()}. */
  @Test
  void test_getDelay() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. calculateDelay(int, int)-method works as expected

    // Test strategy:
    // --- a. minimum value
    // --- b. maximum value
    // --- d. values from examples in JavaDoc

    // --- a. minimum value
    {
      final IoChallenge dut = new IoChallenge(0, 0, 0x0000);

      assertEquals(0, dut.getDelay());
    } // end --- a.

    // --- b. maximum value
    {
      final IoChallenge dut = new IoChallenge(0, 0, 0xffff);

      assertEquals(4_396_972_769_280L, dut.getDelay());
    } // end --- b.

    // --- c. values from examples in JavaDoc
    Map.ofEntries(
            Map.entry(0x0000, "0 ns"),
            Map.entry(0x0020, "1 ns"),
            Map.entry(0xffe0, String.format("%.3f us", 2.047)),
            Map.entry(0x0001, String.format("%.3f us", 2.048)),
            Map.entry(0x0021, String.format("%.3f us", 2.049)),
            Map.entry(0xffe1, String.format("%.3f us", 4.095)),
            Map.entry(0x001f, String.format("36' %.3f\"", 39.023)),
            Map.entry(0x003f, String.format("36' %.3f\"", 40.097)),
            Map.entry(0xffdf, String.format(" 1h 13' %.3f\"", 15.899)),
            Map.entry(0xffff, String.format(" 1h 13' %.3f\"", 16.973)))
        .forEach(
            (delay, expected) -> {
              final IoChallenge dut = new IoChallenge(0, 0, delay);

              assertEquals(expected, AfiUtils.nanoSeconds2Time(dut.getDelay()));
            }); // end forEach((delay, expected) -> ...)
  } // end method */

  /** Test method for {@link IoChallenge#response()}. */
  @Test
  void test_response() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. case 1 smoke test
    // --- b. case 2 smoke test
    // --- c. case 3 smoke test
    // --- d. case 4 smoke test

    // --- a. case 1 smoke test
    {
      final IoChallenge dut = new IoChallenge(0, 1, 0x0102);

      assertEquals(new ResponseApdu("9003"), dut.response());
    } // end --- a.

    // --- b. case 2 smoke test
    {
      final IoChallenge dut = new IoChallenge(0x80, 0x02, 0x0304, 5);

      assertEquals(new ResponseApdu("8306830783 9083"), dut.response());
    } // end --- b.

    // --- c. case 3 smoke test
    {
      final IoChallenge dut = new IoChallenge(0x80, 0x03, 0x0410, Hex.toByteArray("112233"));

      assertEquals(new ResponseApdu("9079"), dut.response());
    } // end --- c.

    // --- d. case 4 smoke test
    {
      final IoChallenge dut = new IoChallenge(0x80, 0x04, 0x0510, Hex.toByteArray("4455"), 6);

      assertEquals(new ResponseApdu("85ad85ae85af 90b0"), dut.response());
    } // end --- d.
  } // end method */
} // end class
