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
package de.gematik.smartcards.sdcom.isoiec7816objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for white-box testing of {@link AnswerToReset}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.CyclomaticComplexity",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestAnswerToReset {

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

  /** Test method for {@link AnswerToReset#appendY(int, int)}. */
  @Test
  void test_appendY__int_int() {
    // Test strategy:
    // --- a. smoke test
    // --- b. test with all relevant combinations

    // --- a. smoke test
    {
      final int tdi = 0x62;
      final int iPlus1 = 3;

      final String result = AnswerToReset.appendY(tdi, iPlus1);

      assertEquals("TB3 TC3 follow, ", result);
    } // end --- a.

    // --- b. test with all relevant combinations
    // spotless:off
    RNG.intsClosed(1, 15, 8).forEach(i ->
        Map.ofEntries(
            Map.entry(
                0x00, //  -   -   -   -
                "no interface bytes follow, "),
            Map.entry(
                0x10, // TA1  -   -   -
                String.format("TA%1$d follow, ", i)),
            Map.entry(
                0x20, //  -  TB1  -  -
                String.format("TB%1$d follow, ", i)),
            Map.entry(
                0x30, // TA1 TB1  -   -
                String.format("TA%1$d TB%1$d follow, ", i)),
            Map.entry(
                0x40, //  -   -  TC1  -
                String.format("TC%1$d follow, ", i)),
            Map.entry(
                0x50, // TA1  -  TC1  -
                String.format("TA%1$d TC%1$d follow, ", i)),
            Map.entry(
                0x60, //  -  TB1 TC1  -
                String.format("TB%1$d TC%1$d follow, ", i)),
            Map.entry(
                0x70, // TA1 TB1 TC1  -
                String.format("TA%1$d TB%1$d TC%1$d follow, ", i)),
            Map.entry(
                0x80, //  -   -   -  TD1
                String.format("TD%1$d follow, ", i)),
            Map.entry(
                0x90, // TA1  -   -  TD1
                String.format("TA%1$d TD%1$d follow, ", i)),
            Map.entry(
                0xa0, //  -  TB1  -  TD1
                String.format("TB%1$d TD%1$d follow, ", i)),
            Map.entry(
                0xb0, // TA1 TB1  -  TD1
                String.format("TA%1$d TB%1$d TD%1$d follow, ", i)),
            Map.entry(
                0xc0, //  -   -  TC1 TD1
                String.format("TC%1$d TD%1$d follow, ", i)),
            Map.entry(
                0xd0, // TA1  -  TC1 TD1
                String.format("TA%1$d TC%1$d TD%1$d follow, ", i)),
            Map.entry(
                0xe0, //  -  TB1 TC1 TD1
                String.format("TB%1$d TC%1$d TD%1$d follow, ", i)),
            Map.entry(
                0xf0, // TA1 TB1 TC1 TD1
                String.format("TA%1$d TB%1$d TC%1$d TD%1$d follow, ", i))
        ).forEach((highNibble, expected) ->
            IntStream.rangeClosed(0, 15).forEach(lowNibble ->
                assertEquals(
                    expected,
                    AnswerToReset.appendY(highNibble + lowNibble, i)
                )
            ) // end forEach(lowNibble -> ...)
        ) // end forEach((highNibble, expected) -> ...)
    ); // end forEach(iPlus1 -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link AnswerToReset#parseTabcd(int, ByteBuffer)}. */
  @Test
  void test_parseTabcd() {
    // Test strategy:
    // --- a. manual smoke test
    // --- b. smoke test with zero, one and two clusters
    // --- c. ERROR: too few octet in buffer

    // --- a. manual smoke test
    assertEquals(
        List.of(0x10, 0x43000000),
        AnswerToReset.parseTabcd(0x10, ByteBuffer.wrap(Hex.toByteArray("43"))));

    // loop over a bunch of protocols indicated in format byte T0
    RNG.intsClosed(0, 15, 5)
        .forEach(
            protocol -> {
              // --- b. smoke test with zero, one and two clusters
              // loop over a bunch of suffixes:
              RNG.intsClosed(0, 20, 5)
                  .forEach(
                      suffixLength -> {
                        final byte[] suffix = RNG.nextBytes(suffixLength);

                        List.of(
                                List.of(0x00, "", Collections.emptyList()), //  -   -   -
                                List.of(0x10, "27", List.of(0x27000000)), // TA1  -   -
                                List.of(0x20, "53", List.of(0x00530000)), //  -  TB1  -
                                List.of(0x30, "0815", List.of(0x08150000)), // TA1 TB1  -
                                List.of(0x40, "81", List.of(0x00008100)), //  -   -  TC1
                                List.of(0x50, "3782", List.of(0x37008200)), // TA1  -  TC1
                                List.of(0x60, "bcde", List.of(0x00bcde00)), //  -  TB1 TC1
                                List.of(0x70, "f74328", List.of(0xf7432800)), // TA1 TB1 TC1
                                List.of(
                                    0xf0, "a1743802", List.of(0xa1743802)), // A B C D  -   -   -
                                List.of(
                                    0xd0,
                                    "b7381f42",
                                    List.of(0xb700381f, 0x42000000)), // A - C D TA2  -   -
                                List.of(
                                    0xc0,
                                    "5a2e7d",
                                    List.of(0x00005a2e, 0x007d0000)), // - - C D  -  TB2  -
                                List.of(
                                    0x90,
                                    "1730435b",
                                    List.of(0x17000030, 0x435b0000)), // A - - D TA2 TB2  -
                                List.of(
                                    0xe0,
                                    "c21041cf",
                                    List.of(0x00c21041, 0x0000cf00)), // - B C D  -   -  TC2
                                List.of(
                                    0xb0,
                                    "23b152affa",
                                    List.of(0x23b10052, 0xaf00fa00)), // A B - D TA2  -  TC2
                                List.of(
                                    0x80,
                                    "63f485",
                                    List.of(0x00000063, 0x00f48500)), // - - - D  -  TB2 TC2
                                List.of(
                                    0xa0,
                                    "b474a2b2c2",
                                    List.of(0x00b40074, 0xa2b2c200)), // - B - D TA2 TB2 TC2
                                List.of(
                                    0x80,
                                    "ff4308420e",
                                    List.of(0x000000ff, 0x4308420e)) // TD1 TA2 TB2 TC2 TD2
                                )
                            .forEach(
                                data -> {
                                  final int y0 = (int) data.get(0);
                                  final int t0 = y0 | protocol;
                                  final String input = (String) data.get(1);
                                  final ByteBuffer buffer =
                                      ByteBuffer.wrap(
                                          AfiUtils.concatenate(Hex.toByteArray(input), suffix));
                                  final List<Integer> expected =
                                      new ArrayList<>(
                                          ((List<?>) data.get(2))
                                              .stream().map(i -> (Integer) i).toList());
                                  expected.addFirst(y0);

                                  final List<Integer> present =
                                      AnswerToReset.parseTabcd(t0, buffer);

                                  assertEquals(expected, present, input);
                                }); // end forEach(data -> ...)
                      }); // end forEach(suffixLength -> ...)
              // end --- b.

              // --- c. ERROR: too few octet in buffer
              List.of(
                      List.of("10", ""), // TA1 expected
                      List.of("20", ""), // TB1 expected
                      List.of("40", ""), // TC1 expected
                      List.of("80", ""), // TD1 expected
                      List.of("30", "47"), // TA1 TB1 expected
                      List.of("80", "43") // TD1 TC2 expected
                      )
                  .forEach(
                      input -> {
                        final int y0 = Integer.parseInt(input.get(0), 16);
                        final int t0 = y0 | protocol;
                        final String hex = input.get(1);
                        final ByteBuffer buffer = ByteBuffer.wrap(Hex.toByteArray(hex));

                        assertThrows(
                            BufferUnderflowException.class,
                            () -> AnswerToReset.parseTabcd(t0, buffer));
                      }); // end forEach(input -> ...)
              // end --- c.
            }); // end forEach(protocol -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#AnswerToReset(byte[])}. */
  @Test
  void test_AnswerToReset__byteA() {
    // Assertions:
    // ... a. parseTabcd(int, Buffer)-method works as expected

    // Test strategy:
    // --- a. shortest possible ATR
    // --- b. loop over various lengths for interface bytes
    // --- c. various lengths for historical bytes
    // --- d. ERROR: too few octet in input byte[]
    // --- e. TCK absent
    // --- f. TCK present with some random values
    // --- g. loop over various lengths for extra bytes
    // --- h. check if input byte[] is defensively cloned

    // --- a. shortest possible ATR
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      assertEquals(0x3f, dut.getTs());
      assertEquals(0x00, dut.getT0());
      assertFalse(dut.isPresentTa(1));
      assertFalse(dut.isPresentTb(1));
      assertFalse(dut.isPresentTc(1));
      assertFalse(dut.isPresentTd(1));
      assertEquals(0, dut.getHistoricalBytes().getHistoricalBytes().length);
      assertEquals(-1, dut.getTck());
      assertFalse(dut.insHasExtraOctet);
      assertEquals(EnumSet.of(EafiIccProtocol.T0), dut.getSupportedProtocols());
    } // end --- a.

    // --- b. loop over various lengths for interface bytes
    IntStream.rangeClosed(1, 30)
        .forEach(
            noCluster -> {
              final var randomAtr = new RandomAtr(noCluster);
              final var hex = randomAtr.getAtr();
              final var indicatedProtocols = EnumSet.noneOf(EafiIccProtocol.class);
              indicatedProtocols.addAll(
                  randomAtr.getList().stream()
                      .mapToInt(i -> i.get("D"))
                      .filter(i -> i >= 0)
                      .mapToObj(i -> EafiIccProtocol.getInstance((byte) i))
                      .collect(Collectors.toSet()));
              if (indicatedProtocols.isEmpty()) {
                indicatedProtocols.add(EafiIccProtocol.T0);
              } // end fi

              // --- c. various lengths for historical bytes
              IntStream.rangeClosed(0, 15)
                  .forEach(
                      hbLength -> {
                        final var hb = RNG.nextBytes(hbLength);
                        final var prefix = AfiUtils.concatenate(Hex.toByteArray(hex), hb);
                        prefix[1] += (byte) hbLength;

                        // --- d. ERROR: too few octets in input byte[]
                        assertThrows(
                            IllegalArgumentException.class,
                            () ->
                                new AnswerToReset(
                                    Hex.toByteArray(
                                        // snip away last octet
                                        Hex.toHexDigits(prefix, 0, prefix.length - 1))));
                        // end --- d.

                        // --- e. TCK absent
                        {
                          final var dut = new AnswerToReset(prefix);

                          assertEquals(prefix[0] & 0xff, dut.getTs());
                          assertEquals(prefix[1] & 0xff, dut.getT0());
                          assertEquals(
                              hbLength, dut.getHistoricalBytes().getHistoricalBytes().length);
                          assertEquals(-1, dut.getTck());
                          assertFalse(dut.insHasExtraOctet);
                          assertEquals(indicatedProtocols, dut.getSupportedProtocols());
                        } // end --- e.

                        IntStream.rangeClosed(0, 0xff)
                            .forEach(
                                tck -> {
                                  // --- f. TCK present with some random values
                                  final byte[] atrInput =
                                      AfiUtils.concatenate(prefix, new byte[] {(byte) tck});

                                  {
                                    final var dut = new AnswerToReset(atrInput);

                                    assertEquals(prefix[0] & 0xff, dut.getTs());
                                    assertEquals(prefix[1] & 0xff, dut.getT0());
                                    assertEquals(
                                        hbLength,
                                        dut.getHistoricalBytes().getHistoricalBytes().length);
                                    assertEquals(tck, dut.getTck());
                                    assertFalse(dut.insHasExtraOctet);
                                    assertEquals(indicatedProtocols, dut.getSupportedProtocols());
                                  }

                                  // --- g. loop over various lengths for extra bytes
                                  RNG.intsClosed(1, 10, 5)
                                      .forEach(
                                          suffixLength -> {
                                            final var dut =
                                                new AnswerToReset(
                                                    AfiUtils.concatenate(
                                                        atrInput, RNG.nextBytes(suffixLength)));

                                            assertEquals(prefix[0] & 0xff, dut.getTs());
                                            assertEquals(prefix[1] & 0xff, dut.getT0());
                                            assertEquals(
                                                hbLength,
                                                dut.getHistoricalBytes()
                                                    .getHistoricalBytes()
                                                    .length);
                                            assertEquals(tck, dut.getTck());
                                            assertTrue(dut.insHasExtraOctet);
                                            assertEquals(
                                                indicatedProtocols, dut.getSupportedProtocols());
                                          }); // end forEach(suffixLength -> ...)
                                  // end --- g.
                                }); // end forEach(tck -> ...)
                        // end --- f.
                      }); // end forEach(hbLength -> ...)
              // end --- c.
            }); // end forEach(noCluster -> ...)
    // end --- b.

    // --- h. check if input byte[] is defensively cloned
    {
      final byte[] atrInput = Hex.toByteArray("3f 00");
      final var dut = new AnswerToReset(atrInput);
      atrInput[0] = 0x3b;

      assertEquals("3f00", Hex.toHexDigits(dut.getBytes()));
    } // end --- h.
  } // end method */

  /** Test method for {@link AnswerToReset#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. same type, equal content
    // --- e. same type, different content

    final var dut = new AnswerToReset(Hex.toByteArray("3f 00"));

    for (final Object obj :
        new Object[] {
          dut, // --- a. same reference
          null, // --- b. null input
          "afi" // --- c. difference in type
        }) {
      assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals() to compare object references
    } // end For (obj...)

    Map.ofEntries(
            Map.entry(Hex.toHexDigits(dut.getBytes()), true), // --- d. same type, equal content
            Map.entry("3b 00", false) // --- e. same type, different content
            )
        .forEach(
            (atr, expected) ->
                assertEquals(
                    expected,
                    dut.equals(
                        new AnswerToReset(
                            Hex.toByteArray(atr))))); // end forEach((atr, expected) -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#hashCode()}. */
  @Test
  void test_hashCode() {
    // Test strategy:
    // --- a. loop over bunch of random ATR
    // --- b. call hashCode()-method again

    // --- a. loop over bunch of random ATR
    IntStream.rangeClosed(0, 15)
        .forEach(
            noCluster -> {
              final RandomAtr randomAtr = new RandomAtr(noCluster);
              final byte[] atrInput = Hex.toByteArray(randomAtr.getAtr());

              final var dut = new AnswerToReset(atrInput);
              assertEquals(Arrays.hashCode(atrInput), dut.hashCode());
            }); // end forEach(noCluster -> ...)

    // --- b. call hashCode()-method again
    final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

    final int hash = dut.hashCode();

    assertEquals(hash, dut.hashCode());
  } // end method */

  /** Test method for {@link AnswerToReset#compareTo(AnswerToReset)}. */
  @Test
  void test_compareTo_AnswerToReset() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));
    final var equal = new AnswerToReset(dut.getBytes());
    final var other = new AnswerToReset(Hex.toByteArray("3f 00"));

    assertTrue(dut.compareTo(other) < 0);
    assertEquals(0, dut.compareTo(equal));
    assertTrue(other.compareTo(dut) > 0);
  } // end method */

  /** Test method for {@link AnswerToReset#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. minimal ATR
    // --- b. manually chosen variations

    // --- a. minimal ATR
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      assertEquals("3f 00", dut.toString());
    } // end --- a.

    // --- b. manually chosen variations
    // spotless:off
    Set.of(//                  clu HiB TCK
        "3b 00",            //  -   -   -
        "3f 00 47",         //  -   -   x
        "3b 01 66",         //  -   x   -
        "3b 01 47 cc",      //  -   x   x
        "3b 10 15",         //  x   -   -
        "3f 10 15 cc",      //  x   -   x
        "3b 11 97 66",      //  x   x   -
        "3b 51 97ff 66 cc", //  x   x   x
        "3b 80 91 9501 fe"  // TD1, TD2 present
    ).forEach((atr -> {
      final var dut = new AnswerToReset(Hex.toByteArray(atr));

      assertEquals(atr, dut.toString());
    })); // end forEach(atr -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link AnswerToReset#getSupportedProtocols()}. */
  @Test
  void test_getSupportedProtocols() {
    // Assertions:
    // ... a. constructor(s) works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. no TDi
    // --- b. arbitrary number of TDi
    // --- c. check for defensive cloning

    // --- a. no TDi
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 00"));
      final var expected = Set.of(EafiIccProtocol.T0);

      final var actual = dut.getSupportedProtocols();

      assertEquals(expected, actual);
      assertNotSame(actual, dut.getSupportedProtocols());
    } // end --- a.

    // --- b. arbitrary number of TDi
    IntStream.rangeClosed(1, 10)
        .forEach(
            noCluster -> {
              final var randomAtr = new RandomAtr(noCluster);
              final var hex = randomAtr.getAtr();
              final var expected = EnumSet.noneOf(EafiIccProtocol.class);
              expected.addAll(
                  randomAtr.getList().stream()
                      .mapToInt(i -> i.get("D"))
                      .filter(i -> i >= 0)
                      .mapToObj(i -> EafiIccProtocol.getInstance((byte) i))
                      .collect(Collectors.toSet()));
              if (expected.isEmpty()) {
                expected.add(EafiIccProtocol.T0);
              } // end fi
              final var dut = new AnswerToReset(Hex.toByteArray(hex));

              final var actual = dut.getSupportedProtocols();

              assertEquals(expected, actual);
              assertNotSame(actual, dut.getSupportedProtocols());
            }); // end forEach(noCluster -> ...)
    // end --- b.

    // --- c. check for defensive cloning
    // Note: Intentionally this kind of test is (also) done in
    //       the previous steps, see there.
  } // end method */

  /** Test method for {@link AnswerToReset#isPresent(int, String)}. */
  @Test
  void test_isPresent__int_String() {
    // Assertions:
    // ... a. attribute insTabcd is properly filled, i.e. parseTabcd(...) works as expected

    // Test strategy:
    // --- a. smoke test with manually chosen values
    // --- b. arbitrary interface bytes
    // --- c. ERROR: unsupported "position"

    // --- a. smoke test with manually chosen values
    {
      AnswerToReset dut =
          new AnswerToReset(
              Hex.toByteArray(
                  "3b 20 47" // TS T0 TB1
                  ));
      assertFalse(dut.isPresent(1, "A"));
      assertTrue(dut.isPresent(1, "B"));
      assertFalse(dut.isPresent(1, "C"));
      assertFalse(dut.isPresent(1, "D"));

      dut =
          new AnswerToReset(
              Hex.toByteArray(
                  "3f 90 1180 82 47 c4" // TS T0   TA1-TD1  TD2  TD3  TC4
                  ));

      assertTrue(dut.isPresent(1, "A"));
      assertFalse(dut.isPresent(1, "B"));
      assertFalse(dut.isPresent(1, "C"));
      assertTrue(dut.isPresent(1, "D"));

      assertFalse(dut.isPresent(2, "A"));
      assertFalse(dut.isPresent(2, "B"));
      assertFalse(dut.isPresent(2, "C"));
      assertTrue(dut.isPresent(2, "D"));

      assertFalse(dut.isPresent(3, "A"));
      assertFalse(dut.isPresent(3, "B"));
      assertFalse(dut.isPresent(3, "C"));
      assertTrue(dut.isPresent(3, "D"));

      assertFalse(dut.isPresent(4, "A"));
      assertFalse(dut.isPresent(4, "B"));
      assertTrue(dut.isPresent(4, "C"));
      assertFalse(dut.isPresent(4, "D"));

      assertFalse(dut.isPresent(5, "A"));
      assertFalse(dut.isPresent(5, "B"));
      assertFalse(dut.isPresent(5, "C"));
      assertFalse(dut.isPresent(5, "D"));

      assertFalse(dut.isPresent(6, "A"));
      assertFalse(dut.isPresent(6, "B"));
      assertFalse(dut.isPresent(6, "C"));
      assertFalse(dut.isPresent(6, "D"));
    }
    // end --- a.

    // --- b. arbitrary interface bytes
    IntStream.rangeClosed(0, 2)
        .forEach(
            i -> { // loop a couple of times
              // start with just one cluster and with each loop-iteration increment number of
              // cluster
              IntStream.rangeClosed(1, 30)
                  .forEach(
                      noCluster -> {
                        final RandomAtr randomAtr = new RandomAtr(noCluster);
                        final String hex = randomAtr.getAtr();
                        final List<Map<String, Integer>> expected = randomAtr.getList();
                        final var dut = new AnswerToReset(Hex.toByteArray(hex));

                        for (int j = 0; j < noCluster + 3; j++) {
                          Map<String, Integer> exp; // NOPMD could be final (false positive)
                          try {
                            exp = expected.get(j);
                          } catch (IndexOutOfBoundsException e) {
                            exp =
                                Map.of(
                                    "A", -1,
                                    "B", -1,
                                    "C", -1,
                                    "D", -1);
                          } // end Catch (...)

                          final int index = j + 1;
                          exp.forEach(
                              (position, value) ->
                                  assertEquals(
                                      -1 != value,
                                      dut.isPresent(index, position),
                                      () -> String.format("%s: T%s%d", hex, position, index)));
                        } // end For (j...)
                      }); // end forEach(noCluster -> ...)
            }); // end forEach(i -> ...)
    // end --- b.

    // --- c. ERROR: unsupported "position"
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 10 11"));

      assertTrue(dut.isPresent(1, "A"));
      assertFalse(dut.isPresent(1, "B"));
      assertFalse(dut.isPresent(1, "C"));
      assertFalse(dut.isPresent(1, "D"));
      assertThrows(
          IllegalArgumentException.class,
          () -> dut.isPresent(1, "X")); // "X" not in set {"A", "B", "C", "D"}
    } // end --- c.
  } // end method */

  /** Test method for {@link AnswerToReset#isPresentTa(int)}. */
  @Test
  void test_isPresentTa__int() {
    // Assertions:
    // ... a. constructor(s) works as expected
    // ... b. underlying isPresent(int, String)-method works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new AnswerToReset(Hex.toByteArray("3b 10 11"));

    // --- a. smoke test
    assertTrue(dut.isPresentTa(1));
    assertFalse(dut.isPresentTa(2));
  } // end method */

  /** Test method for {@link AnswerToReset#isPresentTb(int)}. */
  @Test
  void test_isPresentTb__int() {
    // Assertions:
    // ... a. constructor works as expected
    // ... b. underlying isPresent(int, String)-method works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new AnswerToReset(Hex.toByteArray("3b 20 42"));

    // --- a. smoke test
    assertTrue(dut.isPresentTb(1));
    assertFalse(dut.isPresentTb(2));
  } // end method */

  /** Test method for {@link AnswerToReset#isPresentTc(int)}. */
  @Test
  void test_isPresentTc__int() {
    // Assertions:
    // ... a. constructor works as expected
    // ... b. underlying isPresent(int, String)-method works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new AnswerToReset(Hex.toByteArray("3b 40 fe"));

    // --- a. smoke test
    assertTrue(dut.isPresentTc(1));
    assertFalse(dut.isPresentTc(2));
  } // end method */

  /** Test method for {@link AnswerToReset#isPresentTd(int)}. */
  @Test
  void test_isPresentTd__int() {
    // Assertions:
    // ... a. constructor works as expected
    // ... b. underlying isPresent(int, String)-method works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new AnswerToReset(Hex.toByteArray("3b 80 01"));

    // --- a. smoke test
    assertTrue(dut.isPresentTd(1));
    assertFalse(dut.isPresentTd(2));
  } // end method */

  /** Test method for {@link AnswerToReset#getInterfaceByte(int, String)}. */
  @Test
  void test_getInterfaceByte__int_String() {
    // Assertions:
    // ... a. attribute insTabcd is properly filled, i.e. parseTabcd(...) works as expected
    // ... b. isPresent(int, String)-method works as expected

    // Test strategy:
    // --- a. smoke test with manually chosen values
    // --- b. arbitrary interface bytes
    // --- c. ERROR: interface byte absent
    // --- d. ERROR: unsupported "position"

    // --- a. smoke test with manually chosen values
    {
      AnswerToReset dut =
          new AnswerToReset(
              Hex.toByteArray(
                  "3f 20 47" // TS T0 TB1
                  ));
      assertEquals(0x47, dut.getInterfaceByte(1, "B"));

      dut =
          new AnswerToReset(
              Hex.toByteArray(
                  "3b 90 1180 82 47 c4" // TS T0   TA1-TD1  TD2  TD3  TC4
                  ));

      assertEquals(0x11, dut.getInterfaceByte(1, "A"));
      assertEquals(0x80, dut.getInterfaceByte(1, "D"));
      assertEquals(0x82, dut.getInterfaceByte(2, "D"));
      assertEquals(0x47, dut.getInterfaceByte(3, "D"));
      assertEquals(0xc4, dut.getInterfaceByte(4, "C"));
    }
    // end --- a.

    // --- b. arbitrary interface bytes
    // --- c. ERROR: interface byte absent
    IntStream.rangeClosed(0, 2)
        .forEach(
            i -> { // loop a couple of times
              // start with just one cluster and with each loop-iteration increment number of
              // cluster
              IntStream.rangeClosed(1, 30)
                  .forEach(
                      noCluster -> {
                        final RandomAtr randomAtr = new RandomAtr(noCluster);
                        final String hex = randomAtr.getAtr();
                        final List<Map<String, Integer>> expected = randomAtr.getList();
                        final var dut = new AnswerToReset(Hex.toByteArray(hex));

                        for (int j = 0; j < noCluster; j++) {
                          final Map<String, Integer> exp = expected.get(j);

                          final int index = j + 1;
                          exp.forEach(
                              (position, value) -> {
                                if (dut.isPresent(index, position)) {
                                  // ... interface byte is present
                                  //     => check it
                                  assertEquals(
                                      value,
                                      dut.getInterfaceByte(index, position),
                                      () -> String.format("%s: T%s%d", hex, position, index));
                                } else {
                                  // ... interface byte is absent
                                  assertThrows(
                                      IllegalArgumentException.class,
                                      () -> dut.getInterfaceByte(index, position));
                                } // end else
                              }); // end forEach((position, value) -> ...)
                        } // end For (j...)
                      }); // end forEach(noCluster -> ...)
            }); // end forEach(i -> ...)
    // end --- b, c

    // --- d. ERROR: unsupported "position"
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 10 11"));

      assertEquals(0x11, dut.getInterfaceByte(1, "A"));
      assertThrows(
          IllegalArgumentException.class,
          () -> dut.getInterfaceByte(1, "X")); // "X" not in set {"A", "B", "C", "D"}
    } // end --- d.
  } // end method */

  /** Test method for {@link AnswerToReset#getTa(int)}. */
  @Test
  void test_getTa__int() {
    // Assertions:
    // ... a. constructor works as expected
    // ... b. underlying getInterfaceByte(int, String)-method works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new AnswerToReset(Hex.toByteArray("3f 10 11"));

    // --- a. smoke test
    assertEquals(0x11, dut.getTa(1));
    assertThrows(IllegalArgumentException.class, () -> dut.getTa(2));
  } // end method */

  /** Test method for {@link AnswerToReset#getTb(int)}. */
  @Test
  void test_getTb__int() {
    // Assertions:
    // ... a. constructor works as expected
    // ... b. underlying getInterfaceByte(int, String)-method works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new AnswerToReset(Hex.toByteArray("3f 20 42"));

    // --- a. smoke test
    assertEquals(0x42, dut.getTb(1));
    assertThrows(IllegalArgumentException.class, () -> dut.getTb(2));
  } // end method */

  /** Test method for {@link AnswerToReset#getTc(int)}. */
  @Test
  void test_getTc__int() {
    // Assertions:
    // ... a. constructor works as expected
    // ... b. underlying getInterfaceByte(int, String)-method works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new AnswerToReset(Hex.toByteArray("3f 40 fe"));

    // --- a. smoke test
    assertEquals(0xfe, dut.getTc(1));
    assertThrows(IllegalArgumentException.class, () -> dut.getTc(2));
  } // end method */

  /** Test method for {@link AnswerToReset#getTd(int)}. */
  @Test
  void test_getTd__int() {
    // Assertions:
    // ... a. constructor works as expected
    // ... b. underlying getInterfaceByte(int, String)-method works as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new AnswerToReset(Hex.toByteArray("3f 80 01"));

    // --- a. smoke test
    assertEquals(0x01, dut.getTd(1));
    assertThrows(IllegalArgumentException.class, () -> dut.getTd(2));
  } // end method */

  /** Test method for {@link AnswerToReset#checkAtrLength(List)}. */
  @Test
  void test_checkAtrLength__List() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. length of ATR is (just) okay
    // --- b. length of ATR is (just) not okay

    final List<String> findings = new ArrayList<>();

    // --- a. length of ATR is (just) okay
    {
      final var dut =
          new AnswerToReset(
              Hex.toByteArray(
                  //   1  2 3 4 5  6 7 8 9  a b c d  e f g        historical bytes        TCK
                  //  T0 A1B1C1D1 A2B2C2D2 A3B3C3D3 A4B4C4  1 2 3 4 5 6 7 8 9 a b c d e f
                  "3b ff 950045f0 11b200f1 fe45187f 0100c4 0102030405060708090a0b0c0d0e0f cc"));

      dut.checkAtrLength(findings);

      assertEquals(AnswerToReset.MAX_ATR_SIZE, dut.getBytes().length);
      assertTrue(findings.isEmpty());
    } // end --- a.

    // --- b. length of ATR is (just) not okay
    {
      final var dut =
          new AnswerToReset(
              Hex.toByteArray(
                  //   1  2 3 4 5  6 7 8 9  a b c d  e f g h        historical bytes        TCK
                  //  T0 A1B1C1D1 A2B2C2D2 A3B3C3D3 A4B4C4D4  1 2 3 4 5 6 7 8 9 a b c d e f
                  "3b ff 950045f0 11b200f1 fe4518ff 0100c402 0102030405060708090a0b0c0d0e0f cc"));

      dut.checkAtrLength(findings);

      assertEquals(AnswerToReset.MAX_ATR_SIZE + 1, dut.getBytes().length);
      assertEquals(
          List.of("ATR SHALL NOT be longer than 32 bytes, see ISO/IEC 7816-3 clause 8.2.1 §2."),
          findings);
    } // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#getTs()}. */
  @Test
  void test_getTs() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. check with all possible values

    // --- a. smoke test
    assertEquals(0x3f, new AnswerToReset(Hex.toByteArray("3f 00")).getTs());

    // --- b. check with all possible values
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ts -> {
              final var dut =
                  new AnswerToReset(
                      Hex.toByteArray(
                          String.format(
                              "%02x 00", // just TS and T0
                              ts)));

              assertEquals(ts, dut.getTs());
            }); // end forEach(ts -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#checkTs(List)}. */
  @Test
  void test_checkTs__List() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. direct convention
    // --- b. invers convention
    // --- c. unknown convention

    final List<String> findings = new ArrayList<>();

    // --- a. direct convention
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      dut.checkTs(findings);

      assertTrue(dut.isDirectConvention());
      assertTrue(findings.isEmpty());
    } // end --- a.

    // --- b. invers convention
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      dut.checkTs(findings);

      assertTrue(dut.isInverseConvention());
      assertTrue(findings.isEmpty());
    } // end --- b.

    // --- c. unknown convention
    IntStream.rangeClosed(0, 0xff)
        .mapToObj(ts -> new AnswerToReset(Hex.toByteArray(String.format("%02x 00", ts))))
        .forEach(
            dut -> {
              findings.clear();

              dut.checkTs(findings);

              if (dut.isDirectConvention()) {
                assertTrue(findings.isEmpty());
              } else if (dut.isInverseConvention()) {
                assertTrue(findings.isEmpty());
              } else {
                assertEquals(List.of("TS invalid, see ISO/IEC 7816-3 clause 8.1 §4."), findings);
              } // end else
            }); // end forEach(atr -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#explainTs()}. */
  @Test
  void test_explainTs() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getTs()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. check with all values defined in ISO/IEC 7816-3
    // --- c. check with other values

    // --- a. smoke test
    assertEquals(
        "Initial Character     TS ='3b' => direct convention",
        new AnswerToReset(Hex.toByteArray("3b 00")).explainTs());

    // --- b. check with all values defined in ISO/IEC 7816-3
    // --- c. check with other values
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ts -> {
              final var dut =
                  new AnswerToReset(
                      Hex.toByteArray(
                          String.format(
                              "%02x 00", // just TS and T0
                              ts)));

              final String pre = dut.explainTs();

              final String expected; // see  ISO/IEC 7816-3:2006 clause 8.1 §4
              if (0x3b == ts) { // NOPMD literal in if statement
                expected = "Initial Character     TS ='3b' => direct convention";
              } else if (0x3f == ts) { // NOPMD literal in if statement
                expected = "Initial Character     TS ='3f' => inverse convention";
              } else {
                expected =
                    String.format("Initial Character     TS ='%02x' => unknown convention", ts);
              } // end else

              assertEquals(expected, pre);
            }); // end forEach(ts -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#getT0()}. */
  @Test
  void test_getT0() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. check with all possible values

    final Map<Integer, String> interfaceBytes =
        Map.ofEntries(
            Map.entry(0x00, "           "), //  -   -   -   -
            Map.entry(0x10, "11         "), // TA1  -   -   -
            Map.entry(0x20, "   42      "), //  -  TB1  -  -
            Map.entry(0x30, "11 42      "), // TA1 TB1  -   -
            Map.entry(0x40, "      fe   "), //  -   -  TC1  -
            Map.entry(0x50, "11    fe   "), // TA1  -  TC1  -
            Map.entry(0x60, "   42 fe   "), //  -  TB1 TC1  -
            Map.entry(0x70, "11 42 fe   "), // TA1 TB1 TC1  -
            Map.entry(0x80, "         00"), //  -   -   -  TD1
            Map.entry(0x90, "11       00"), // TA1  -   -  TD1
            Map.entry(0xa0, "   42    00"), //  -  TB1  -  TD1
            Map.entry(0xb0, "11 42    00"), // TA1 TB1  -  TD1
            Map.entry(0xc0, "      fe 00"), //  -   -  TC1 TD1
            Map.entry(0xd0, "11    fe 00"), // TA1  -  TC1 TD1
            Map.entry(0xe0, "   42 fe 00"), //  -  TB1 TC1 TD1
            Map.entry(0xf0, "11 42 fe 00") // TA1 TB1 TC1 TD1
            );
    final List<String> historicalBytes =
        List.of(
            "", // no historical bytes
            "af", //  1 historical byte
            "af02", //  2 historical byte
            "af0203", //  3 historical byte
            "af020304", //  4 historical byte
            "af02030405", //  5 historical byte
            "af0203040506", //  6 historical byte
            "af020304050607", //  7 historical byte
            "af02030405060708", //  8 historical byte
            "af0203040506070809", //  9 historical byte
            "af02030405060708090a", // 10 historical byte
            "af02030405060708090a0b", // 11 historical byte
            "af02030405060708090a0b0c", // 12 historical byte
            "af02030405060708090a0b0c0d", // 13 historical byte
            "af02030405060708090a0b0c0d0e", // 14 historical byte
            "af02030405060708090a0b0c0d0e0f" // 15 historical byte
            );

    // --- a. smoke test
    assertEquals(0x00, new AnswerToReset(Hex.toByteArray("3b 00")).getT0());

    // --- b. check with all possible values
    IntStream.rangeClosed(0, 255)
        .forEach(
            t0 -> {
              final var dut =
                  new AnswerToReset(
                      Hex.toByteArray(
                          String.format(
                              "3f %02x %s %s",
                              t0, interfaceBytes.get(t0 & 0xf0), historicalBytes.get(t0 & 0x0f))));

              assertEquals(t0, dut.getT0());
            }); // end forEach(t0 -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#explainT0()}. */
  @Test
  void test_explainT0() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getT0()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. check with all possible values

    final Map<Integer, List<String>> interfaceBytes =
        Map.ofEntries(
            Map.entry(
                0x00, //  -   -   -   -
                List.of(
                    "           ",
                    "Format byte           T0 ='%02x' =>" + " no interface bytes follow, %s")),
            Map.entry(
                0x10, // TA1  -   -   -
                List.of("11         ", "Format byte           T0 ='%02x' =>" + " TA1 follow, %s")),
            Map.entry(
                0x20, //  -  TB1  -  -
                List.of("   42      ", "Format byte           T0 ='%02x' =>" + " TB1 follow, %s")),
            Map.entry(
                0x30, // TA1 TB1  -   -
                List.of(
                    "11 42      ", "Format byte           T0 ='%02x' =>" + " TA1 TB1 follow, %s")),
            Map.entry(
                0x40, //  -   -  TC1  -
                List.of("      fe   ", "Format byte           T0 ='%02x' =>" + " TC1 follow, %s")),
            Map.entry(
                0x50, // TA1  -  TC1  -
                List.of(
                    "11    fe   ", "Format byte           T0 ='%02x' =>" + " TA1 TC1 follow, %s")),
            Map.entry(
                0x60, //  -  TB1 TC1  -
                List.of(
                    "   42 fe   ", "Format byte           T0 ='%02x' =>" + " TB1 TC1 follow, %s")),
            Map.entry(
                0x70, // TA1 TB1 TC1  -
                List.of(
                    "11 42 fe   ",
                    "Format byte           T0 ='%02x' =>" + " TA1 TB1 TC1 follow, %s")),
            Map.entry(
                0x80, //  -   -   -  TD1
                List.of("         00", "Format byte           T0 ='%02x' =>" + " TD1 follow, %s")),
            Map.entry(
                0x90, // TA1  -   -  TD1
                List.of(
                    "11       00", "Format byte           T0 ='%02x' =>" + " TA1 TD1 follow, %s")),
            Map.entry(
                0xa0, //  -  TB1  -  TD1
                List.of(
                    "   42    00", "Format byte           T0 ='%02x' =>" + " TB1 TD1 follow, %s")),
            Map.entry(
                0xb0, // TA1 TB1  -  TD1
                List.of(
                    "11 42    00",
                    "Format byte           T0 ='%02x' =>" + " TA1 TB1 TD1 follow, %s")),
            Map.entry(
                0xc0, //  -   -  TC1 TD1
                List.of(
                    "      fe 00", "Format byte           T0 ='%02x' =>" + " TC1 TD1 follow, %s")),
            Map.entry(
                0xd0, // TA1  -  TC1 TD1
                List.of(
                    "11    fe 00",
                    "Format byte           T0 ='%02x' =>" + " TA1 TC1 TD1 follow, %s")),
            Map.entry(
                0xe0, //  -  TB1 TC1 TD1
                List.of(
                    "   42 fe 00",
                    "Format byte           T0 ='%02x' =>" + " TB1 TC1 TD1 follow, %s")),
            Map.entry(
                0xf0, // TA1 TB1 TC1 TD1
                List.of(
                    "11 42 fe 00",
                    "Format byte           T0 ='%02x' =>" + " TA1 TB1 TC1 TD1 follow, %s")));

    final Map<Integer, List<String>> historicalBytes =
        Map.ofEntries(
            Map.entry(0x0, List.of("", "no historical bytes")),
            Map.entry(0x1, List.of("af", "one historical byte")),
            Map.entry(0x2, List.of("af02", "2 historical bytes")),
            Map.entry(0x3, List.of("af0203", "3 historical bytes")),
            Map.entry(0x4, List.of("af020304", "4 historical bytes")),
            Map.entry(0x5, List.of("af02030405", "5 historical bytes")),
            Map.entry(0x6, List.of("af0203040506", "6 historical bytes")),
            Map.entry(0x7, List.of("af020304050607", "7 historical bytes")),
            Map.entry(0x8, List.of("af02030405060708", "8 historical bytes")),
            Map.entry(0x9, List.of("af0203040506070809", "9 historical bytes")),
            Map.entry(0xa, List.of("af02030405060708090a", "10 historical bytes")),
            Map.entry(0xb, List.of("af02030405060708090a0b", "11 historical bytes")),
            Map.entry(0xc, List.of("af02030405060708090a0b0c", "12 historical bytes")),
            Map.entry(0xd, List.of("af02030405060708090a0b0c0d", "13 historical bytes")),
            Map.entry(0xe, List.of("af02030405060708090a0b0c0d0e", "14 historical bytes")),
            Map.entry(0xf, List.of("af02030405060708090a0b0c0d0e0f", "15 historical bytes")));

    // --- a. smoke test
    assertEquals(0x00, new AnswerToReset(Hex.toByteArray("3b 00")).getT0());

    // --- b. check with all possible values
    IntStream.rangeClosed(0, 255)
        .forEach(
            t0 -> {
              final List<String> listInterBytes = interfaceBytes.get(t0 & 0xf0);
              final List<String> listHistoBytes = historicalBytes.get(t0 & 0x0f);
              final var dut =
                  new AnswerToReset(
                      Hex.toByteArray(
                          String.format(
                              "3f %02x %s %s", t0, listInterBytes.get(0), listHistoBytes.get(0))));

              assertEquals(
                  String.format(listInterBytes.get(1), t0, listHistoBytes.get(1)), dut.explainT0());
            }); // end forEach(t0 -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#getTa1()}. */
  @Test
  void test_getTa1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTa(int)-method works as expected
    // ... c. getTa(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test with TA1 absent
    // --- b. test with TA1 present

    // --- a. smoke test with TA1 absent
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b e0 b1c101"));

      final int ta1 = dut.getTa1();

      assertEquals(0x11, ta1); // default value
    } // end --- a.

    // --- b. test with TA1 present
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ta1Expected -> {
              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 10 %02x", ta1Expected)));

              final int ta1Present = dut.getTa1();

              assertEquals(ta1Expected, ta1Present);
            }); // end forEach(ta1Expected -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#getFi()}. */
  @Test
  void test_getFi() {
    // Note: The result of this simple method is best checked manually against
    //       ISO/IEC 7816-3. Hereafter this is just a copy of the
    //       method-under-test for preventing unintended changes

    // Test strategy:
    // --- a. smoke test with TA1 absent for testing the default value
    // --- b. test with all possible values for TA1

    final List<Integer> expected =
        // see ISO/IEC 7816-3:2006 table 8
        List.of(
            // Bits b8 to 5 of TA1
            372, // 0 = 0000
            372, // 1 = 0001
            558, // 2 = 0010
            744, // 3 = 0011
            1116, // 4 = 0100
            1488, // 5 = 0101
            1860, // 6 = 0110
            -1, // 7 = 0111 => RFU
            -1, // 8 = 1000 => RFU
            512, // 9 = 1001
            768, // a = 1010
            1024, // b = 1011
            1536, // c = 1100
            2048, // d = 1101
            -1, // e = 1110 => RFU
            -1 // f = 1111 => RFU
            );

    // --- a. smoke test with TA1 absent for testing the default value
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      assertEquals(372, dut.getFi());
    } // end --- a.

    // --- b. test with all possible values for TA1
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ta1 -> {
              final var dut = new AnswerToReset(Hex.toByteArray(String.format("3b 10 %02x", ta1)));

              final int fi = dut.getFi();

              assertEquals(expected.get(ta1 >> 4), fi);
            }); // end forEach(ta1 -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#getDi()}. */
  @Test
  void test_getDi() {
    // Note: The result of this simple method is best checked manually against
    //       ISO/IEC 7816-3. Hereafter this is just a copy of the
    //       method-under-test for preventing unintended changes

    // Test strategy:
    // --- a. smoke test with TA1 absent for testing the default value
    // --- b. test with all possible values for TA1

    final List<Integer> expected =
        // see ISO/IEC 7816-3:2006 table 8
        List.of(
            // Bits b4 to 1 of TA1
            -1, // 0 = 0000 => RFU
            1, // 1 = 0001
            2, // 2 = 0010
            4, // 3 = 0011
            8, // 4 = 0100
            16, // 5 = 0101
            32, // 6 = 0110
            64, // 7 = 0111
            12, // 8 = 1000
            20, // 9 = 1001
            -1, // a = 1010 => RFU
            -1, // b = 1011 => RFU
            -1, // c = 1100 => RFU
            -1, // d = 1101 => RFU
            -1, // e = 1110 => RFU
            -1 // f = 1111 => RFU
            );

    // --- a. smoke test with TA1 absent for testing the default value
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      assertEquals(1, dut.getDi());
    } // end --- a.

    // --- b. test with all possible values for TA1
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ta1 -> {
              final var dut = new AnswerToReset(Hex.toByteArray(String.format("3b 10 %02x", ta1)));

              final int di = dut.getDi();

              assertEquals(expected.get(ta1 & 0xf), di);
            }); // end forEach(ta1 -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#getFmax()}. */
  @Test
  void test_getFmax() {
    // Note: The result of this simple method is best checked manually against
    //       ISO/IEC 7816-3. Hereafter this is just a copy of the
    //       method-under-test for preventing unintended changes

    // Test strategy:
    // --- a. smoke test with TA1 absent for testing the default value
    // --- b. test with all possible values for TA1

    final List<Double> expected =
        // see ISO/IEC 7816-3:2006 table 8
        List.of(
            // Bits b8 to 5 of TA1
            4.0, // 0 = 0000
            5.0, // 1 = 0001
            6.0, // 2 = 0010
            8.0, // 3 = 0011
            12.0, // 4 = 0100
            16.0, // 5 = 0101
            20.0, // 6 = 0110
            -1.0, // 7 = 0111 => RFU
            -1.0, // 8 = 1000 => RFU
            5.0, // 9 = 1001
            7.5, // a = 1010
            10.0, // b = 1011
            15.0, // c = 1100
            20.0, // d = 1101
            -1.0, // e = 1110 => RFU
            -1.0 // f = 1111 => RFU
            );

    // --- a. smoke test with TA1 absent for testing the default value
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      assertEquals(5.0, dut.getFmax(), 0.001);
    } // end --- a.

    // --- b. test with all possible values for TA1
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ta1 -> {
              final var dut = new AnswerToReset(Hex.toByteArray(String.format("3b 10 %02x", ta1)));

              final double fmax = dut.getFmax();

              assertEquals(expected.get(ta1 >> 4), fmax, 0.001);
            }); // end forEach(ta1 -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#explainTa1()}. */
  @Test
  void test_explainTa1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. underlying methods work as expected, i.e.
    //        - isPresentTa(int)
    //        - getTa1()
    //        - getFi()
    //        - getDi()
    //        - getFmax()

    // Note: Because of the assertions, we can be lazy here and
    //       concentrate on code coverage.

    // Test strategy:
    // --- a. TA1 absent
    // --- b. TA1 present

    // --- a. TA1 absent
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      assertEquals(
          String.format(
              "Clock rate, baud rate TA1=---- => default values, Fi=372, Di=1, fmax=%.1f MHz", 5.0),
          dut.explainTa1());
    } // end --- a.

    // --- b. TA1 present
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 10 97"));

      assertEquals(
          String.format("Clock rate, baud rate TA1='97' => Fi=512, Di=64, fmax=%.1f MHz", 5.0),
          dut.explainTa1());
    } // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#explainTb12(List, boolean)}. */
  @Test
  void test_explainTb12__List_boolean() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTb(int)-method works as expected
    // ... c. getTb(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. TB1 absent  and TB2 absent,  explain TB[12]
    // --- b. TB1 absent  and TB2 present, explain TB[12]
    // --- c. TB1 present and TB2 absent,  explain TB[12]
    // --- d. TB1 present and TB2 present, explain TB[12] for all possible values

    final List<String> explanation = new ArrayList<>();

    // --- a. TB1 absent  and TB2 absent,  explain TB[12]
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 90 95 10 11"));

      dut.explainTb12(explanation, true);

      assertTrue(explanation.isEmpty());

      dut.explainTb12(explanation, false);

      assertTrue(explanation.isEmpty());
    } // end --- a.

    // --- b. TB1 absent  and TB2 present, explain TB[12]
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 90 95 20 11"));

      dut.explainTb12(explanation, true);

      assertTrue(explanation.isEmpty());

      dut.explainTb12(explanation, false);

      assertEquals(1, explanation.size());
      assertEquals("Deprecated            TB2='11'", explanation.getFirst());

      explanation.clear();
    } // end --- b.

    // --- c. TB1 present and TB2 absent,  explain TB[12]
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f a0 95 10 11"));

      dut.explainTb12(explanation, false);

      assertTrue(explanation.isEmpty());

      dut.explainTb12(explanation, true);

      assertEquals(1, explanation.size());
      assertEquals("Deprecated            TB1='95'", explanation.getFirst());
    } // end --- b.

    // --- d. TB1 present and TB2 present, explain TB[12] for all possible values
    // spotless:off
    IntStream.rangeClosed(0, 0xff).forEach(tb1 ->
        IntStream.rangeClosed(0, 0xff).forEach(tb2 -> {
          final var dut =
              new AnswerToReset(Hex.toByteArray(String.format("3b a0 %02x 20 %02x", tb1, tb2)));
          explanation.clear();

          dut.explainTb12(explanation, true);
          dut.explainTb12(explanation, false);

          assertEquals(2, explanation.size());
          assertEquals(
              String.format("Deprecated            TB1='%02x'", tb1),
              explanation.get(0));
          assertEquals(
              String.format("Deprecated            TB2='%02x'", tb2),
              explanation.get(1));
        }) // end forEach(tb2 -> ...)
    ); // end forEach(tb1 -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link AnswerToReset#getTc1()}. */
  @Test
  void test_getTc1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTc(int)-method works as expected
    // ... c. getTc(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test with TC1 absent
    // --- b. smoke test with TC1 present

    // --- a. smoke test with TC1 absent
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b b0 a1b101"));

      final int tc1 = dut.getTc1();

      assertEquals(0x00, tc1); // default value
    } // end --- a.

    // --- b. smoke test with TC1 present
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            tc1Expected -> {
              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 40 %02x", tc1Expected)));

              final int tc1Present = dut.getTc1();

              assertEquals(tc1Expected, tc1Present);
            }); // end forEach(tc1Expected -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#explainTc1()}. */
  @Test
  void test_explainTc1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTc(int)-method works as expected
    // ... c. getTc(int)-method works as expected
    // ... d. getFi()-method works as expected
    // ... e. getDi()-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. TC1 absent
    // --- b. TC1 present with value 'ff'=254
    // --- c. loop over all relevant values of TC1, i.e. {0, 1, ..., 254}

    // --- a. TC1 absent
    Map.ofEntries(
            Map.entry("3b 80 80 01", false), // TC1 absent AND T=15 absent
            Map.entry("3b 80 80 81 0f", true) // TC1 absent AND T=15 present
            )
        .forEach(
            (atr, isT15Present) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              final String explanation = dut.explainTc1();

              assertEquals(isT15Present, dut.getSupportedProtocols().contains(EafiIccProtocol.T15));
              assertEquals("Extra guard time      TC1=---- => default value, 12 etu", explanation);
            }); // end forEach(atr -> ...)

    // --- b. TC1 present with value 'ff'=254
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 40 ff"));

      final String explanation = dut.explainTc1();

      assertEquals("Extra guard time      TC1='ff' => 12 etu for T=0, 11 etu for T=1", explanation);
    } // end --- b.

    // --- c. loop over all relevant values of TC1, i.e. {0, 1, ..., 254}
    IntStream.rangeClosed(0, 254)
        .forEach(
            tc1 -> {
              // T=15 absent
              AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 40 %02x", tc1)));

              String explanation = dut.explainTc1();

              assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T15));
              assertEquals(
                  String.format("Extra guard time      TC1='%02x' => %d etu", tc1, 12 + tc1),
                  explanation);

              // T=15 present
              dut = new AnswerToReset(Hex.toByteArray(String.format("3f c0 %02x 80 81 0f", tc1)));

              explanation = dut.explainTc1();

              assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T15));
              assertEquals(
                  String.format(
                      "Extra guard time      TC1='%02x' => 12 etu + %d clock cycles",
                      tc1, tc1 * dut.getFi() / dut.getDi()),
                  explanation);
            }); // end forEach(tc1 -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#explainTd1()}. */
  @Test
  void test_explainTd1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTd(int)-method works as expected
    // ... c. getTd(int)-method works as expected
    // ... d. appendY(int, int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. TD1 absent
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      assertEquals("Protocol indicator    TD1=---- => implicit offer is T=0", dut.explainTd1());
    } // end --- a.

    // --- b. TD1 present indicating an arbitrary protocol, other protocols absent
    {
      IntStream.rangeClosed(0, 15)
          .forEach(
              i -> {
                final var dut = new AnswerToReset(Hex.toByteArray(String.format("3f 80 %02x", i)));

                assertEquals(
                    String.format(
                        "Protocol indicator    TD1='%02x' =>"
                            + " no interface bytes follow, only offer is T=%d",
                        i, i),
                    dut.explainTd1());
              }); // end forEach(i -> ...)
    } // end --- b.

    // --- c. TD1 present indicating an arbitrary protocol, other protocols present
    {
      IntStream.rangeClosed(0, 15)
          .forEach(
              i -> {
                final var dut =
                    new AnswerToReset(
                        Hex.toByteArray(String.format("3f 80 %02x %02x", 0x80 | i, (i + 1) % 15)));

                assertEquals(
                    String.format(
                        "Protocol indicator    TD1='%02x' => TD2 follow, 1st offer is T=%d",
                        0x80 | i, i),
                    dut.explainTd1());
              }); // end forEach(i -> ...)
    } // end --- c.
  } // end method */

  /** Test method for {@link AnswerToReset#getTa2()}. */
  @Test
  void test_getTa2() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTa(int)-method works as expected
    // ... c. getTa(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test with TA2 absent
    // --- b. smoke test with TA2 present

    // --- a. smoke test with TA2 absent
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 e0 b2c201"));

      final int ta2 = dut.getTa2();

      assertEquals(0x00, ta2); // default value
    } // end --- a.

    // --- b. smoke test with TA2 present
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ta2Expected -> {
              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 80 10 %02x", ta2Expected)));

              final int ta2Present = dut.getTa2();

              assertEquals(ta2Expected, ta2Present);
            }); // end forEach(ta2Expected -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#explainTa2()}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_explainTa2() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. underlying methods work as expected, i.e.
    //        - isPresentTa(int)
    //        - getTa2()

    // Note: Because of the assertions we can be lazy here and
    //       concentrate on code coverage.

    // Test strategy:
    // --- a. TA2 absent
    // --- b. TA2 present

    // --- a. TA2 absent
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      assertFalse(dut.isPresentTa(2));
      assertEquals("Specific mode byte    TA2=---- => negotiable mode", dut.explainTa2());
    } // end --- a.

    // --- b. TA2 present
    // spotless:off
    List.of(false, true).forEach(b8 -> {
      final String b8Explanation =
          (b8 ? "unable" : "capable") + " to change to negotiable mode, ";

      List.of(false, true).forEach(b5 -> {
        final String b5Explanation =
            "Fi, Di, fmax " + (b5 ? "implicitly known" : "taken from TA1");

        IntStream.rangeClosed(0, 3).forEach(b7b6 ->
            IntStream.rangeClosed(0, 15).forEach(protocol -> {
              final int ta2 =
                  (b8 ? 0x80 : 0x00)
                      + (b7b6 << 5)
                      + (b5 ? 0x10 : 0x00)
                      + protocol;
              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 80 10 %02x", ta2)));

              final String present = dut.explainTa2();

              assertTrue(dut.isPresentTa(2));
              assertEquals(
                  String.format(
                      "Specific mode byte    TA2='%02x' => specific mode offering T=%d, %s%s",
                      ta2, protocol, b8Explanation, b5Explanation),
                  present);
            }) // end forEach(protocol -> ...)
        ); // end forEach(b7b6 -> ...)
      }); // end forEach(b5 -> ...)
    }); // end forEach(b8 -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link AnswerToReset#getTc2()}. */
  @Test
  void test_getTc2() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTc(int)-method works as expected
    // ... c. getTc(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test with TC2 absent
    // --- b. smoke test with TC2 present

    // --- a. smoke test with TC2 absent
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 b0 a2b201"));

      final int tc2 = dut.getTc2();

      assertEquals(10, tc2); // default value
    } // end --- a.

    // --- b. smoke test with TC2 present
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            tc2Expected -> {
              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 80 40 %02x", tc2Expected)));

              final int tc2Present = dut.getTc2();

              assertEquals(tc2Expected, tc2Present);
            }); // end forEach(tc2Expected -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#explainTc2(List)}. */
  @Test
  void test_explainTc2__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTc(int)-method works as expected
    // ... c. getTc2()-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. TC2 absent, T=0 not indicated
    // --- b. TC2 absent, T=0 implicitly indicated
    // --- c. TC2 absent, T=0 explicitly indicated
    // --- d. TC2 present, T=0 not indicated
    // --- e. TC2 present, T=0 indicated (has to be explicitly)

    // --- a. TC2 absent, T=0 not indicated
    {
      final List<String> list = new ArrayList<>();
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 01"));
      dut.explainTc2(list);

      assertFalse(dut.isPresentTc(2));
      assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
      assertTrue(list.isEmpty());
    } // end --- a.

    testExplainTc2ListB();
    testExplainTc2ListC();
    testExplainTc2ListD();
    testExplainTc2ListE();
  } // end method */

  /** Test method {@link AnswerToReset#explainTc2(List)} partially. */
  private void testExplainTc2ListB() {
    // --- b. TC2 absent, T=0 implicitly indicated
    final List<String> list = new ArrayList<>();
    final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));
    dut.explainTc2(list);

    assertFalse(dut.isPresentTc(2));
    assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
    assertEquals(1, list.size());
    assertEquals("T=0 Waiting time      TC2=---- => default value, WI=10", list.getFirst());
  } // end method */

  /** Test method {@link AnswerToReset#explainTc2(List)} partially. */
  private void testExplainTc2ListC() {
    // --- c. TC2 absent, T=0 explicitly indicated
    final List<String> list = new ArrayList<>();
    final var dut = new AnswerToReset(Hex.toByteArray("3b 80 00"));
    dut.explainTc2(list);

    assertFalse(dut.isPresentTc(2));
    assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
    assertEquals(1, list.size());
    assertEquals("T=0 Waiting time      TC2=---- => default value, WI=10", list.getFirst());
  } // end method */

  /** Test method {@link AnswerToReset#explainTc2(List)} partially. */
  private void testExplainTc2ListD() {
    // --- d. TC2 present, T=0 not indicated
    final List<String> list = new ArrayList<>();

    IntStream.rangeClosed(0, 0xff)
        .forEach(
            tc2 -> {
              list.clear();

              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 80 41 %02x", tc2)));
              dut.explainTc2(list);

              assertTrue(dut.isPresentTc(2));
              assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
              assertEquals(1, list.size());
              assertEquals(
                  String.format(
                      "T=0 Waiting time      TC2='%02x' present, although T=0 not indicated in ATR",
                      tc2),
                  list.getFirst());
            }); // end forEach(tc2 -> ...)
  } // end method */

  /** Test method {@link AnswerToReset#explainTc2(List)} partially. */
  private void testExplainTc2ListE() {
    // --- e. TC2 present, T=0 indicated (has to be explicitly)
    // e.1 TC2=0
    // e.2 TC2 from range [1, 255]

    final List<String> list = new ArrayList<>();

    // e.1 TC2=0
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 40 00"));
      dut.explainTc2(list);

      assertTrue(dut.isPresentTc(2));
      assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
      assertEquals(1, list.size());
      assertEquals("T=0 Waiting time      TC2='00' => WI=RFU", list.getFirst());
    } // end e.2

    // e.2 TC2 from range [1, 255]
    IntStream.rangeClosed(1, 255)
        .forEach(
            tc2 -> {
              list.clear();

              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 80 40 %02x", tc2)));
              dut.explainTc2(list);

              assertTrue(dut.isPresentTc(2));
              assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
              assertEquals(1, list.size());
              assertEquals(
                  String.format("T=0 Waiting time      TC2='%02x' => WI=%d", tc2, tc2),
                  list.getFirst());
            }); // end forEach(tc2 -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#getBwi()}. */
  @Test
  void test_getBwi() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getT1Tb1()-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. T=1 indicated
    // --- c. ERROR: T=1 not indicated

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 21 4d"));

      final int bwi = dut.getBwi();

      assertEquals(4, bwi);
    } // end --- a.

    // --- b. T=1 indicated
    // b.1 T=1 indicated, no T=1 specific cluster
    // b.2 T=1 indicated,    T=1 specific cluster, TBi absent
    // b.3 T=1 indicated,    T=1 specific cluster, TBi present, not RFU
    // b.4 T=1 indicated,    T=1 specific cluster, TBi present, RFU
    Map.ofEntries(
            // b.1 T=1 indicated, no T=1 specific cluster
            Map.entry("3f 80 01", 0x4),
            // b.2 T=1 indicated,    T=1 specific cluster, TBi absent
            Map.entry("3b 80 81 d1 4d 08 02", 0x4),
            // b.3 T=1 indicated,    T=1 specific cluster, TBi present, not RFU
            Map.entry("3f 80 81 21 10", 1),
            // b.4 T=1 indicated,    T=1 specific cluster, TBi present, RFU
            Map.entry("3f 80 81 21 a0", 10))
        .forEach(
            (atr, expected) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(expected, dut.getBwi(), atr);
            }); // end forEach((atr, expected) -> ...)
    // end --- b.

    // --- c. ERROR: T=1 not indicated
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T1));
      assertThrows(IllegalStateException.class, dut::getBwi);
    } // end --- c.
  } // end method */

  /** Test method for {@link AnswerToReset#getBwt()}. */
  @Test
  void test_getBwt() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getBwi()-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. T=1 indicated, TBi absent
    // --- c. T=1 indicated, TBi present
    // --- d. ERROR: T=1 not indicated

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 21 0f"));

      final String bwt = dut.getBwt();

      assertEquals("11 etu + 357120 clock cycles", bwt);
    } // end --- a.

    // --- b. T=1 indicated, TBi absent
    List.of(
            "3f 80 01", // b.1 T=1 indicated, no T=1 specific cluster
            "3b 80 81 d1 4d 08 02" // b.2 T=1 indicated,    T=1 specific cluster, TBi absent
            )
        .forEach(
            atr -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals("11 etu + 5713920 clock cycles", dut.getBwt(), atr);
            }); // end forEach((atr, expected) -> ...)
    // end --- b.

    // --- c. T=1 indicated, TBi present
    IntStream.rangeClosed(0, 15)
        .forEach(
            bwi -> {
              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 80 81 21 %02x", bwi << 4)));

              final String bwt = dut.getBwt();

              assertEquals(
                  (bwi >= 0xa)
                      ? "RFU"
                      : String.format(
                          "11 etu + %d clock cycles", Math.round(Math.pow(2, bwi)) * 960 * 372),
                  bwt);
            }); // end forEach(bwi -> ...)

    // --- d. ERROR: T=1 not indicated
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T1));
      assertThrows(IllegalStateException.class, dut::getBwi);
    } // end --- d.
  } // end method */

  /** Test method for {@link AnswerToReset#getCwi()}. */
  @Test
  void test_getCwi() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getT1Tb1()-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. T=1 indicated
    // --- c. ERROR: T=1 not indicated

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 21 4d"));

      final int cwi = dut.getCwi();

      assertEquals(0xd, cwi);
    } // end --- a.

    // --- b. T=1 indicated
    // b.1 T=1 indicated, no T=1 specific cluster
    // b.2 T=1 indicated,    T=1 specific cluster, TCi absent
    // b.3 T=1 indicated,    T=1 specific cluster, TCi present, not RFU
    Map.ofEntries(
            // b.1 T=1 indicated, no T=1 specific cluster
            Map.entry("3f 80 01", 0xd),
            // b.2 T=1 indicated,    T=1 specific cluster, TCi absent
            Map.entry("3b 80 81 d1 4d 08 02", 0xd),
            // b.3 T=1 indicated,    T=1 specific cluster, TCi present
            Map.entry("3f 80 81 21 17", 7))
        .forEach(
            (atr, expected) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(expected, dut.getCwi(), atr);
            }); // end forEach((atr, expected) -> ...)
    // end --- b.

    // --- c. ERROR: T=1 not indicated
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T1));
      assertThrows(IllegalStateException.class, dut::getCwi);
    } // end --- c.
  } // end method */

  /** Test method for {@link AnswerToReset#getCwt()}. */
  @Test
  void test_getCwt() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getCwi()-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. T=1 indicated, TBi absent
    // --- c. T=1 indicated, TBi present
    // --- d. ERROR: T=1 not indicated

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 21 03"));

      final int cwt = dut.getCwt();

      assertEquals(19, cwt);
    } // end --- a.

    // --- b. T=1 indicated, TBi absent
    List.of(
            "3f 80 01", // b.1 T=1 indicated, no T=1 specific cluster
            "3b 80 81 d1 4d 08 02" // b.2 T=1 indicated,    T=1 specific cluster, TBi absent
            )
        .forEach(
            atr -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(8203, dut.getCwt(), atr);
            }); // end forEach((atr, expected) -> ...)
    // end --- b.

    // --- c. T=1 indicated, TBi present
    IntStream.rangeClosed(0, 15)
        .forEach(
            cwi -> {
              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 80 81 21 %02x", cwi)));

              final int cwt = dut.getCwt();

              assertEquals(Math.round(Math.pow(2, cwi)) + 11, cwt);
            }); // end forEach(bwi -> ...)

    // --- d. ERROR: T=1 not indicated
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T1));
      assertThrows(IllegalStateException.class, dut::getCwt);
    } // end --- d.
  } // end method */

  /** Test method for {@link AnswerToReset#getT1Ta1()}. */
  @Test
  void test_getT1Ta1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected
    // ... c. getTa(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 11 2a"));

      final int ifsc = dut.getT1Ta1();

      assertEquals(0x2a, ifsc);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    Map.ofEntries(
            Map.entry("3f 00", 32), // T=1 not indicated
            Map.entry("3f 80 01", 32), // T=1 indicated but no T=1 specific cluster
            Map.entry("3b 80 81 e1 4d 08 02", 32), // T=1 indicated, T=1 specific cluster, but no TA
            Map.entry("3f 80 81 11 10", 16), // T=1 indicated, T=1 specific cluster, TA3
            Map.entry("3b 80 81 82 83 84 11 42", 0x42) // TA6, other protocols in between
            )
        .forEach(
            (atr, ifsc) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(ifsc, dut.getT1Ta1());
            }); // end forEach((atr, ifsc) -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#explainT1Ta1(int)}. */
  @Test
  void test_explainT1Ta1__int() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTa(int)-method works as expected
    // ... c. getT1Ta1(int)-method works as expected
    // ... d. firstTxCluster(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 11 2a"));

      final String present = dut.explainT1Ta1(3);

      assertEquals("T=1 IFSC              TA3='2a' => 42 bytes", present);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    // b.1 T=1 not indicated
    // b.2 T=1 indicated, no T=1 specific cluster
    // b.3 T=1 indicated,    T=1 specific cluster, TAi absent
    // b.4 T=1 indicated,    T=1 specific cluster, TAi present, not RFU
    // b.5 T=1 indicated,    T=1 specific cluster, TAi present, RFU
    Map.ofEntries(
            // b.1 T=1 not indicated
            Map.entry("3f 00", "T=1 IFSC              TA =---- => default value, 32 bytes"),
            // b.2 T=1 indicated, no T=1 specific cluster
            Map.entry("3f 80 01", "T=1 IFSC              TA =---- => default value, 32 bytes"),
            // b.3 T=1 indicated,    T=1 specific cluster, TAi absent
            Map.entry(
                "3b 80 81 e1 4d 08 02",
                "T=1 IFSC              TA3=---- => default value, 32 bytes"),
            // b.4 T=1 indicated,    T=1 specific cluster, TAi present, not RFU
            Map.entry("3f 80 81 11 10", "T=1 IFSC              TA3='10' => 16 bytes"),
            // b.5 T=1 indicated,    T=1 specific cluster, TAi present, RFU
            Map.entry("3f 80 81 11 00", "T=1 IFSC              TA3='00' => RFU"))
        .forEach(
            (atr, expected) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(expected, dut.explainT1Ta1(dut.firstTxCluster(1)), atr);
            }); // end forEach((atr, expected) -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#getT1Tb1()}. */
  @Test
  void test_getT1Tb1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected
    // ... c. getTb(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 21 2a"));

      final int tbx = dut.getT1Tb1();

      assertEquals(0x2a, tbx);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    Map.ofEntries(
            Map.entry("3f 00", 0x4d), // T=1 not indicated
            Map.entry("3f 80 01", 0x4d), // T=1 indicated but no T=1 specific cluster
            Map.entry(
                "3b 80 81 d1 4d 08 02", 0x4d), // T=1 indicated, T=1 specific cluster, but no TB
            Map.entry("3f 80 81 21 10", 16), // T=1 indicated, T=1 specific cluster, TB3
            Map.entry("3b 80 81 82 83 84 21 42", 0x42) // TB6, other protocols in between
            )
        .forEach(
            (atr, tbx) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(tbx, dut.getT1Tb1());
            }); // end forEach((atr, tbx) -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#explainT1Tb1(int)}. */
  @Test
  void test_explainT1Tb1__int() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTb(int)-method works as expected
    // ... c. getTb(int)-method works as expected
    // ... d. getCwt()-method works as expected
    // ... e. getBtw()-method works as expected
    // ... f. firstTxCluster(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests
    // --- c. ERROR: T=1 not indicated

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 21 2a"));

      final String present = dut.explainT1Tb1(3);

      assertEquals(
          "T=1 Waiting Times     TB3='2a' => BWT=11 etu + 1428480 clock cycles, CWT=1035 etu",
          present);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    // b.1 T=1 indicated, no T=1 specific cluster
    // b.2 T=1 indicated,    T=1 specific cluster, TBi absent
    // b.3 T=1 indicated,    T=1 specific cluster, TBi present
    Map.ofEntries(
            // b.1 T=1 indicated, no T=1 specific cluster
            Map.entry(
                "3f 80 01",
                "T=1 Waiting Times     TB =---- => default value,"
                    + " BWT=11 etu + 5713920 clock cycles, CWT=8203 etu"),
            // b.2 T=1 indicated,    T=1 specific cluster, TBi absent
            Map.entry(
                "3b 80 81 d1 4d 08 02",
                "T=1 Waiting Times     TB3=---- => default value,"
                    + " BWT=11 etu + 5713920 clock cycles, CWT=8203 etu"),
            // b.3 T=1 indicated,    T=1 specific cluster, TBi present
            Map.entry(
                "3f 80 81 21 00",
                "T=1 Waiting Times     TB3='00' => BWT=11 etu + 357120 clock cycles, CWT=12 etu"))
        .forEach(
            (atr, expected) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(expected, dut.explainT1Tb1(dut.firstTxCluster(1)), atr);
            }); // end forEach((atr, expected) -> ...)

    // --- c. ERROR: T=1 not indicated
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T1));
      assertThrows(IllegalStateException.class, () -> dut.explainT1Tb1(dut.firstTxCluster(1)));
    } // end --- c.
  } // end method */

  /** Test method for {@link AnswerToReset#getT1Tc1()}. */
  @Test
  void test_getT1Tc1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected
    // ... c. getTa(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 41 2a"));

      final int tcx = dut.getT1Tc1();

      assertEquals(0x2a, tcx);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    Map.ofEntries(
            Map.entry("3f 00", 0), // T=1 not indicated
            Map.entry("3f 80 01", 0), // T=1 indicated but no T=1 specific cluster
            Map.entry("3b 80 81 b1 4d 08 02", 0), // T=1 indicated, T=1 specific cluster, but no TC
            Map.entry("3f 80 81 41 10", 16), // T=1 indicated, T=1 specific cluster, TA3
            Map.entry("3b 80 81 82 83 84 41 42", 0x42) // TC6, other protocols in between
            )
        .forEach(
            (atr, tcx) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(tcx, dut.getT1Tc1());
            }); // end forEach((atr, tcx) -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#explainT1Tc1(int)}. */
  @Test
  void test_explainT1Tc1__Int() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTc(int)-method works as expected
    // ... c. getT1Tc1(int)-method works as expected
    // ... d. firstTxCluster(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 41 2a"));

      final String present = dut.explainT1Tc1(3);

      assertEquals("T=1 Redundancy Code   TC3='2a' => LRC", present);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    // b.1 T=1 not indicated
    // b.2 T=1 indicated, no T=1 specific cluster
    // b.3 T=1 indicated,    T=1 specific cluster, TCi absent
    // b.4 T=1 indicated,    T=1 specific cluster, TCi present, LRC
    // b.5 T=1 indicated,    T=1 specific cluster, TCi present, CRC
    Map.ofEntries(
            // b.1 T=1 not indicated
            Map.entry("3f 00", "T=1 Redundancy Code   TC =---- => default value, LRC"),
            // b.2 T=1 indicated, no T=1 specific cluster
            Map.entry("3f 80 01", "T=1 Redundancy Code   TC =---- => default value, LRC"),
            // b.3 T=1 indicated,    T=1 specific cluster, TCi absent
            Map.entry(
                "3b 80 81 b1 4d 08 01", "T=1 Redundancy Code   TC3=---- => default value, LRC"),
            // b.4 T=1 indicated,    T=1 specific cluster, TCi present, LRC
            Map.entry("3f 80 81 41 00", "T=1 Redundancy Code   TC3='00' => LRC"),
            // b.5 T=1 indicated,    T=1 specific cluster, TAi present, CRC
            Map.entry("3f 80 81 41 01", "T=1 Redundancy Code   TC3='01' => CRC"))
        .forEach(
            (atr, expected) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(expected, dut.explainT1Tc1(dut.firstTxCluster(1)), atr);
            }); // end forEach((atr, expected) -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#getT15Ta1()}. */
  @Test
  void test_getT15Ta1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected
    // ... c. getTa(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 1f 2a"));

      final int tax = dut.getT15Ta1();

      assertEquals(0x2a, tax);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    Map.ofEntries(
            Map.entry("3f 00", 1), // T=15 not indicated
            Map.entry("3f 80 01", 1), // T=15 indicated but no T=1 specific cluster
            Map.entry(
                "3b 80 81 ef 4d 08 02", 1), // T=15 indicated, T=15 specific cluster, but no TA
            Map.entry("3f 80 81 1f 10", 16), // T=15 indicated, T=15 specific cluster, TA3
            Map.entry("3b 80 81 82 83 84 1f 42", 66) // TA6, other protocols in between
            )
        .forEach(
            (atr, tax) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(tax, dut.getT15Ta1());
            }); // end forEach((atr, tax) -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#explainT15Ta1(int)}. */
  @Test
  void test_explainT15Ta1__int() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTa(int)-method works as expected
    // ... c. firstTxCluster(int)-method works as expected
    // ... d. getT15Ta1(int)-method works as expected
    // ... e. getClassIndicator()-method works as expected
    // ... f. getClockStopIndicator()-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 1f 04"));

      final String present = dut.explainT15Ta1(3);

      assertEquals("ClockStop, Class      TA3='04' => clock stop not supported, class C", present);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    // b.1 T=15 not indicated
    // b.2 T=15 indicated, no T=15 specific cluster
    // b.3 T=15 indicated,    T=15 specific cluster, TAi absent
    // b.4 T=15 indicated,    T=15 specific cluster, TAi present, not RFU
    // b.5 T=15 indicated,    T=15 specific cluster, TAi present, RFU
    // spotless:off
    Map.ofEntries(
        // b.1 T=15 not indicated
        Map.entry(
            "3f 00",
            "ClockStop, Class      TA =---- => default value, clock stop not supported, class A"),
        // b.2 T=15 indicated, no T=15 specific cluster
        Map.entry(
            "3f 80 81 02",
            "ClockStop, Class      TA =---- => default value, clock stop not supported, class A"),
        // b.3 T=15 indicated,    T=15 specific cluster, TAi absent
        Map.entry(
            "3b 80 81 ef 4d 08 02",
            "ClockStop, Class      TA3=---- => default value, clock stop not supported, class A"),
        // b.4 T=15 indicated,    T=15 specific cluster, TAi present, not RFU
        Map.entry(
            "3f 80 81 1f 42",
            "ClockStop, Class      TA3='42' => clock stop supported in state L, class B"),
        // b.5 T=1 indicated,    T=1 specific cluster, TAi present, RFU
        Map.entry(
            "3f 80 81 1f 80",
            "ClockStop, Class      TA3='80' => clock stop supported in state H, class RFU")
    ).forEach((atr, expected) -> {
      final var dut = new AnswerToReset(Hex.toByteArray(atr));

      assertEquals(expected, dut.explainT15Ta1(dut.firstTxCluster(15)), atr);
    }); // end forEach((atr, expected) -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link AnswerToReset#getT15Tb1()}. */
  @Test
  void test_getT15Tb1() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected
    // ... c. getTb(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 2f 2a"));

      final int tbx = dut.getT15Tb1();

      assertEquals(0x2a, tbx);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    Map.ofEntries(
            Map.entry("3f 00", 0), // T=15 not indicated
            Map.entry("3f 80 0f", 0), // T=15 indicated but no T=15 specific cluster
            Map.entry(
                "3b 80 81 df 4d 08 02", 0), // T=15 indicated, T=15 specific cluster, but no TB
            Map.entry("3f 80 81 2f 10", 16), // T=15 indicated, T=15 specific cluster, TB3
            Map.entry("3b 80 81 82 83 84 2f 42", 66) // TB6, other protocols in between
            )
        .forEach(
            (atr, tbx) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(tbx, dut.getT15Tb1());
            }); // end forEach((atr, tbx) -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#explainT15Tb1(int)}. */
  @Test
  void test_explainT15Tb1__int() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTb(int)-method works as expected
    // ... c. getTb(int)-method works as expected
    // ... d. firstTxCluster(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 80 80 2f 00"));

      final String present = dut.explainT15Tb1(3);

      assertEquals("Standard/proprietary  TB3='00' => contact C6 not used", present);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    // b.1 T=15 not indicated
    // b.2 T=15 indicated, no T=15 specific cluster
    // b.3 T=15 indicated,    T=15 specific cluster, TBi absent
    // b.4 T=15 indicated,    T=15 specific cluster, TBi present, '00'
    // b.5 T=15 indicated,    T=15 specific cluster, TBi present, < '80'
    // b.6 T=15 indicated,    T=15 specific cluster, TBi present, >= '80'
    Map.ofEntries(
            // b.1 T=15 not indicated
            Map.entry(
                "3f 80 01", "Standard/proprietary  TB =---- => default value, contact C6 not used"),
            // b.2 T=15 indicated, no T=15 specific cluster
            Map.entry(
                "3f 80 0f", "Standard/proprietary  TB =---- => default value, contact C6 not used"),
            // b.3 T=15 indicated,    T=15 specific cluster, TBi absent
            Map.entry(
                "3b 80 81 df 4d 08 02",
                "Standard/proprietary  TB3=---- => default value, contact C6 not used"),
            // b.4 T=15 indicated,    T=15 specific cluster, TBi present, '00'
            Map.entry("3f 80 81 2f 00", "Standard/proprietary  TB3='00' => contact C6 not used"),
            // b.4 T=15 indicated,    T=15 specific cluster, TBi present, < '80'
            Map.entry(
                "3f 80 81 2f 7f", "Standard/proprietary  TB3='7f' => contact C6 usage is RFU"),
            // b.4 T=15 indicated,    T=15 specific cluster, TBi present, >= '80'
            Map.entry(
                "3f 80 81 2f 80",
                "Standard/proprietary  TB3='80' => proprietary usage of contact C6"))
        .forEach(
            (atr, expected) -> {
              final var dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(expected, dut.explainT15Tb1(dut.firstTxCluster(15)), atr);
            }); // end forEach((atr, expected) -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#checkTdi(List)}. */
  @Test
  void test_checkTdi__List() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. TDi absent
    // --- b. TD1 indicates any possible protocol
    // --- c. protocols not in ascending order

    final List<String> findings = new ArrayList<>();

    // --- a. TDi absent
    {
      final var dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      dut.checkTdi(findings);

      assertFalse(dut.isPresentTd(1));
      assertTrue(findings.isEmpty());
    } // end --- a.

    // --- b. TD1 indicates any possible protocol
    IntStream.rangeClosed(0, 15)
        .forEach(
            protocol -> {
              final var dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 80  %02x", protocol)));
              findings.clear();

              dut.checkTdi(findings);

              switch (protocol) {
                case 0, 1 -> assertTrue(findings.isEmpty());
                case 15 ->
                    assertEquals(
                        List.of("T=15 is invalid in TD1, see ISO/IEC 7816-3:2006 clause 8.2.3 §5"),
                        findings);
                default ->
                    assertEquals(
                        List.of(
                            "invalid protocol in TD1,"
                                + " see ISO/IEC 7816-3:2006 clause 8.2.3 last sentence"),
                        findings);
              } // end Switch (protocol)
            }); // end forEach(protocol -> ...)
    // end --- b.

    // --- c. protocols not in ascending order
    Set.of(
            //  T0 TD1 TD2 TD3 TD4
            "3b 80  81  00", // T=1 before T=0
            "3b 80  80  8f  01", // T=15 before T=1
            "3b 80  80  81  00", // T=0 is indicated once more
            "3b 80  80  82  01" // T=2 before T=1
            )
        .forEach(
            atr -> {
              final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));
              findings.clear();

              dut.checkTdi(findings);

              assertEquals(
                  List.of(
                      "indicated protocols not in ascending order,"
                          + " see ISO/IEC 7816-3:2006 clause 8.2.3 §5"),
                  findings);
            }); // end forEach(atr -> ...)
    // end --- c.
  } // end method */

  /** Test method for {@link AnswerToReset#checkRfuProtocols(List)}. */
  @Test
  void test_checkRfuProtocols__List() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. all non-RFU protocols
    // --- b. loop over all RFU protocols6

    final String format = "3b 80 80 %02x 8e 0f";
    final List<String> findings = new ArrayList<>();

    // --- a. all non-RFU protocols
    {
      final int protocol = 1;
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(String.format(format, protocol)));

      dut.checkRfuProtocols(findings);

      assertTrue(findings.isEmpty());
    } // end --- a.

    // --- b. loop over all RFU protocols
    IntStream.rangeClosed(2, 13)
        .forEach(
            protocol -> {
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format(format, protocol)));
              findings.clear();

              dut.checkRfuProtocols(findings);

              assertEquals(
                  List.of(
                      "TDi indicate protocols which are RFU, see ISO/IEC 7816-3 clause 8.2.3 §4."),
                  findings);
            }); // end forEach(protocol -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#checkTa1(List)}. */
  @Test
  void test_checkTa1__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getFi()-method works as expected
    // ... c. getDi()-method works as expected

    // Test strategy:
    // --- a. TA1 absent
    // --- b. loop over all possible values for TA1

    final List<String> findings = new ArrayList<>();

    // --- a. TA1 absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      dut.checkTa1(findings);

      assertFalse(dut.isPresentTa(1));
      assertTrue(findings.isEmpty());
    } // end --- a.

    // --- b. loop over all possible values for TA1
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ta1 -> {
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 10 %02x", ta1)));
              findings.clear();
              final List<String> expected = new ArrayList<>();
              if (-1 == dut.getFi()) {
                expected.add("RFU value in high nibble of TA1, see ISO/IEC 7816-3:2006 table 7");
              } // end fi
              if (-1 == dut.getDi()) {
                expected.add("RFU value in low nibble of TA1, see ISO/IEC 7816-3:2006 table 8");
              } // end fi

              dut.checkTa1(findings);

              assertTrue(dut.isPresentTa(1));
              assertEquals(expected, findings);
            }); // end forEach(ta1 -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#checkTb1(List)}. */
  @Test
  void test_checkTb1__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTb(int)-method works as expected

    // Test strategy:
    // --- a. TB1 absent
    // --- b. loop over all possible values for TB1

    final List<String> findings = new ArrayList<>();

    // --- a. TB1 absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      dut.checkTb1(findings);

      assertFalse(dut.isPresentTb(1));
      assertTrue(findings.isEmpty());
    } // end --- a.

    // --- b. loop over all possible values for TB1
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            tb1 -> {
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 20 %02x", tb1)));
              findings.clear();

              dut.checkTb1(findings);

              assertTrue(dut.isPresentTb(1));
              assertEquals(
                  List.of(
                      "Deprecated indication of external programming voltage in TB1,"
                          + " see ISO/IEC 7816-3 table 6"),
                  findings);
            }); // end forEach(tb1 -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#checkTa2(List)}. */
  @Test
  void test_checkTa2__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTa(int)-method works as expected
    // ... c. getTa(int)-method works as expected

    // Test strategy:
    // --- a. TA2 absent
    // --- b. loop over all possible values for TA2

    final List<String> findings = new ArrayList<>();

    // --- a. TA2 absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      dut.checkTa2(findings);

      assertFalse(dut.isPresentTa(2));
      assertTrue(findings.isEmpty());
    } // end --- a.

    // --- b. loop over all possible values for TA2
    // spotless:off
    IntStream.rangeClosed(0, 0xff).forEach(ta2 -> {
      final AnswerToReset dut =
          new AnswerToReset(Hex.toByteArray(String.format("3b 80 10 %02x", ta2)));
      findings.clear();
      final List<String> expected = new ArrayList<>();
      if ((0 != (ta2 & 0x40)) || (0 != (ta2 & 0x20))) {
        expected.add("RFU value in b7 or b6 of TA2, see ISO/IEC 7816-3:2006 clause 8.3");
      } // end fi
      switch (ta2 & 0xf) {
        case 0:
        case 1:
        case 14:
          break;

        case 15:
          expected.add("T=15 is not a real protocol and thus forbidden to be indicated in TA2");
          break;

        default:
          expected.add(
              "TA2 indicates a protocol which is RFU, see ISO/IEC 7816-3:2006 clause 8.2.3 §4");
          break;
      } // end Switch (protocol)

      dut.checkTa2(findings);

      assertTrue(dut.isPresentTa(2));
      assertEquals(expected, findings);
    }); // end forEach(ta2 -> ...)
    // spotless:on
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#checkTb2(List)}. */
  @Test
  void test_checkTb2__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTb(int)-method works as expected

    // Test strategy:
    // --- a. TB2 absent
    // --- b. loop over all possible values for TB2

    final List<String> findings = new ArrayList<>();

    // --- a. TB2 absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      dut.checkTb2(findings);

      assertFalse(dut.isPresentTb(2));
      assertTrue(findings.isEmpty());
    } // end --- a.

    // --- b. loop over all possible values for TB2
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            tb2 -> {
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 80 20 %02x", tb2)));
              findings.clear();

              dut.checkTb2(findings);

              assertTrue(dut.isPresentTb(2));
              assertEquals(
                  List.of(
                      "Deprecated indication of external programming voltage in TB2,"
                          + " see ISO/IEC 7816-3 table 6"),
                  findings,
                  dut::toString);
            }); // end forEach(tb2 -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#checkTc2(List)}. */
  @Test
  void test_checkTc2__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTc(int)-method works as expected
    // ... c. getTc2()-method works as expected

    // Test strategy:
    // --- a. TC2 absent
    // --- b. loop over all possible values for TC2

    final List<String> expected = new ArrayList<>();
    final List<String> findings = new ArrayList<>();

    // --- a. TC2 absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      dut.checkTc2(findings);

      assertFalse(dut.isPresentTc(2));
      assertTrue(findings.isEmpty());
    } // end --- a.

    // --- b. loop over all possible values for TC2
    // b.1 T=0 indicated
    // b.2 T=0 not indicated
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            tc2 -> {
              // b.1 T=0 indicated
              expected.clear();
              findings.clear();
              if (0 == tc2) {
                expected.add("TC2=0 is RFU, see ISO/IEC 7816-3 clause 10.2 §3");
              } // end fi
              AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 80 40 %02x", tc2)));

              dut.checkTc2(findings);

              assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
              assertTrue(dut.isPresentTc(2));
              assertEquals(expected, findings);

              // b.2 T=0 not indicated
              dut = new AnswerToReset(Hex.toByteArray(String.format("3b 80 41 %02x", tc2)));
              expected.clear();
              findings.clear();
              expected.add("T=0 is not indicated, but TC2 is present");

              dut.checkTc2(findings);

              assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
              assertTrue(dut.isPresentTc(2));
              assertEquals(expected, findings);
            }); // end forEach(ta1 -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#checkT0InterfaceBytes(List)}. */
  @Test
  void test_checkT0InterfaceBytes__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected

    // Test strategy:
    // --- a. T=0 interface bytes absent
    // --- b. T=0 interface bytes present

    final List<String> findings = new ArrayList<>();

    // --- a. T=0 interface bytes absent
    for (final String atr :
        new String[] {
          //  T0 TD1 TD2
          "3f 00", "3b 80  00",
        }) {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr)); // NOPMD new in loop

      dut.checkT0InterfaceBytes(findings);

      assertTrue(findings.isEmpty(), () -> atr + ", " + findings);
    } // end For (atr...)
    // end --- a.

    // --- b. T=0 interface bytes present
    for (final String atr :
        new String[] {
          //  T0 TD1 TD2 TD3
          "3b 80  80  00", "3b 80  80  80  00",
        }) {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr)); // NOPMD new in loop
      findings.clear();

      dut.checkT0InterfaceBytes(findings);

      assertEquals(
          List.of(
              "protocol specific interface bytes for T=0 are not defined in ISO/IEC 7816-3:2006"),
          findings,
          atr::toString);
    } // end For (atr...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#checkT1InterfaceBytes(List)}. */
  @Test
  void test_checkT1InterfaceBytes__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected
    // ... c. isPresentTd(int)-method works as expected
    // ... d. getT1Ta1()-method works as expected
    // ... e. getT1Tb1()-method works as expected
    // ... f. getT1Tc1()-method works as expected

    // Test strategy:
    // --- a. T=1 interface bytes present at most once
    // --- b. T=1 interface bytes present more than once
    // --- c. loop over all values for TA for T=1
    // --- d. loop over all values for TB for T=1
    // --- e. loop over all values for TC for T=1

    final List<String> findings = new ArrayList<>();

    // --- a. T=1 interface bytes present at most once
    for (final String atr :
        new String[] {
          //  T0 TD1 TD2 TD3
          "3f 00", "3b 80  01", "3b 80  81  01",
        }) {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr)); // NOPMD new in loop

      dut.checkT1InterfaceBytes(findings);

      assertTrue(findings.isEmpty(), () -> atr + ", " + findings);
    } // end For (atr...)
    // end --- a.

    // --- b. T=1 interface bytes present more than once
    for (final String atr :
        new String[] {
          //  T0 TD1 TD2 TD3 TD4
          "3b 80  80  81  01", "3b 80  80  82  81  01", "3b 80  80  81  8e  01",
        }) {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr)); // NOPMD new in loop
      findings.clear();

      dut.checkT1InterfaceBytes(findings);

      assertEquals(
          List.of(
              "protocol specific interface bytes for T=1 are not allowed more than once,"
                  + " see ISO/IEC 7816-3:2006 clause 11.4.1 §1"),
          findings,
          atr::toString);
    } // end For (atr...)
    // end --- b.

    // --- c. loop over all values for TA for T=1
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ta -> {
              findings.clear();
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 80 80 11 %02x", ta)));

              dut.checkT1InterfaceBytes(findings);

              switch (ta) { // NOPMD few branches
                case 0, 0xff ->
                    assertEquals(
                        List.of(
                            "information field size coded in TA3 is RFU,"
                                + " see ISO/IEC 7816-3 clause 11.4.2 §3 dash 1"),
                        findings);

                default -> assertTrue(findings.isEmpty());
              } // end Switch (ta)
            }); // end forEach(ta -> ...)
    // end --- c.

    // --- d. loop over all values for TB for T=1
    // spotless:off
    IntStream.rangeClosed(0, 0xff).forEach(tb -> {
      findings.clear();
      final AnswerToReset dut =
          new AnswerToReset(Hex.toByteArray(String.format("3b 80 80 21 %02x", tb)));

      dut.checkT1InterfaceBytes(findings);

      if ((tb >> 4) < 0xa) { // NOPMD literal in if statement
        assertTrue(findings.isEmpty());
      } else {
        assertEquals(
            List.of("block waiting integer in TB3 is RFU, see ISO/IEC 7816-3:2006 clause 11.4.3"),
            findings);
      } // end else
    }); // end forEach(tb -> ...)
    // end --- d.

    // --- e. loop over all values for TC for T=1
    IntStream.rangeClosed(0, 0xff).forEach(tc -> {
      findings.clear();
      final AnswerToReset dut =
          new AnswerToReset(Hex.toByteArray(String.format("3b 80 80 41 %02x", tc)));

      dut.checkT1InterfaceBytes(findings);

      switch (tc) { // NOPMD few branches
        case 0, 1 -> assertTrue(findings.isEmpty());

        default -> assertEquals(
            List.of("redundancy code in TC3 is RFU, see ISO/IEC 7816-3:2006 clause 11.4.4 §2"),
            findings);
      } // end Switch (tc)
    }); // end forEach(tc -> ...)
    // spotless:on
    // end --- e.
  } // end method */

  /** Test method for {@link AnswerToReset#checkT15InterfaceBytes(List)}. */
  @Test
  void test_checkT15InterfaceBytes__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected
    // ... c. isPresentTd(int)-method works as expected
    // ... d. getT15Ta1()-method works as expected
    // ... e. getT15Tb1()-method works as expected
    // ... f. getTc(int)-method works as expected

    // Test strategy:
    // --- a. T=15 interface bytes present at most once
    // --- b. T=15 interface bytes present more than once
    // --- c. loop over all values for TA for T=15
    // --- d. loop over all values for TB for T=15
    // --- e. loop over all values for TC for T=15

    final List<String> findings = new ArrayList<>();

    // --- a. T=15 interface bytes present at most once
    for (final String atr :
        new String[] {
          //  T0 TD1 TD2 TD3
          "3f 00", "3b 80  0f", "3b 80  8f  0f",
        }) {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr)); // NOPMD new in loop

      dut.checkT1InterfaceBytes(findings);

      assertTrue(findings.isEmpty(), () -> atr + ", " + findings);
    } // end For (atr...)
    // end --- a.

    // --- b. T=15 interface bytes present more than once
    for (final String atr :
        new String[] {
          //  T0 TD1 TD2 TD3 TD4
          "3b 80  80  8f  0f", "3b 80  80  82  8f  0f", "3b 80  80  8f  8e  0f",
        }) {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr)); // NOPMD new in loop
      findings.clear();

      dut.checkT15InterfaceBytes(findings);

      assertEquals(
          List.of(
              "global interface bytes for T=15 are not allowed more than once,"
                  + " see ISO/IEC 7816-3:2006 clause 8.3"),
          findings,
          atr::toString);
    } // end For (atr...)
    // end --- b.

    // --- c. loop over all values for TA for T=15
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            ta -> {
              findings.clear();
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 80 80 1f %02x", ta)));

              dut.checkT15InterfaceBytes(findings);

              switch (ta & 0x3f) { // NOPMD few branches
                case 1, 2, 3, 4, 6, 7 -> assertTrue(findings.isEmpty());

                default ->
                    assertEquals(
                        List.of("class indication in TA3 is RFU, see ISO/IEC 7816-3:2006 table 10"),
                        findings,
                        dut::toString);
              } // end Switch (ta)
            }); // end forEach(ta -> ...)
    // end --- c.

    // --- d. loop over all values for TB for T=15
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            tb -> {
              findings.clear();
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 80 80 2f %02x", tb)));

              dut.checkT15InterfaceBytes(findings);

              if (tb < 128) { // NOPMD literal in if statement
                if (0 == tb) {
                  assertTrue(findings.isEmpty());
                } else {
                  assertEquals(
                      List.of(
                          "indication of usage of C6 in TB3 is RFU, see ISO/IEC 7816-3 clause 8.3"),
                      findings,
                      dut::toString);
                } // end else
              } else {
                assertEquals(
                    List.of("TB3 for T=15 is proprietary, see ISO/IEC 7816-3:2006 clause 8.3"),
                    findings);
              } // end else
            }); // end forEach(tb -> ...)
    // end --- d.

    // --- e. loop over all values for TC for T=15
    IntStream.rangeClosed(0, 0xff)
        .forEach(
            tc -> {
              findings.clear();
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3b 80 80 4f %02x", tc)));

              dut.checkT15InterfaceBytes(findings);

              assertEquals(
                  List.of(
                      "TC for T=15 is not defined, thus TC3 is RFU,"
                          + " see ISO/IEC 7816-3 clause 8.3 §1 and §2"),
                  findings);
            }); // end forEach(ta -> ...)
    // end --- e.
  } // end method */

  /** Test method for {@link AnswerToReset#explainTd2plus(List)}. */
  @Test
  void test_explainTd2plus__List_int() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTd(int)-method works as expected
    // ... c. getTd(int)-method works as expected
    // ... d. appendY(int, int)-method works as expected
    // ... e. firstTxCluster(int)-method works as expected
    // ... f. explainT1specificInterfaceBytes(int)-method works as expected
    // ... g. explainT15globalInterfaceBytes(int)-method works as expected
    // ... h. getInterfaceByte(int, String)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. TD1 absent
    // --- b. TD2 absent
    // --- c. bunch of manually chosen tests

    final List<String> explanation = new ArrayList<>();

    // --- a. TD1 absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      dut.explainTd2plus(explanation);

      assertTrue(explanation.isEmpty());
    } // end --- a.

    // --- b. TD2 absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3f 80 00"));

      dut.explainTd2plus(explanation);

      assertTrue(explanation.isEmpty());
    } // end --- b.

    // --- c. bunch of manually chosen tests
    Map.ofEntries(
            // b.1 TD2 offers same protocol as TD1
            Map.entry(
                //  T0 TD1 TD2
                "3b 80  80  00",
                List.of(
                    "Protocol indicator    TD2='00' => no interface bytes follow,"
                        + " T=0 is offered once more")),
            // b.2 protocol offer in TD2 differs from TD1
            Map.entry(
                //  T0 A1D1 D2
                "3b 90 9780 01",
                List.of(
                    "Protocol indicator    TD2='01' => no interface bytes follow, 2nd offer is T=1",
                    "T=1 IFSC              TA3=---- => default value, 32 bytes",
                    "T=1 Waiting Times     TB3=---- => default value,"
                        + " BWT=11 etu + 5713920 clock cycles, CWT=8203 etu",
                    "T=1 Redundancy Code   TC3=---- => default value, LRC")),
            // b.3 one cluster with T=1 information
            Map.entry(
                //  T0 A1D1 D2 A3
                "3b 90 9781 11 fe",
                List.of(
                    "Protocol indicator    TD2='11' => TA3 follow, T=1 is offered once more",
                    "T=1 IFSC              TA3='fe' => 254 bytes",
                    "T=1 Waiting Times     TB3=---- => default value,"
                        + " BWT=11 etu + 5713920 clock cycles, CWT=8203 etu",
                    "T=1 Redundancy Code   TC3=---- => default value, LRC")),
            // b.4 one cluster with T=15 information
            Map.entry(
                //  T0 A1D1 D2 A3B3
                "3b 90 9680 3f 0200",
                List.of(
                    "Protocol indicator    TD2='3f' => TA3 TB3 follow, 2nd offer is T=15",
                    "ClockStop, Class      TA3='02' => clock stop not supported, class B",
                    "Standard/proprietary  TB3='00' => contact C6 not used")),
            // b.5 several cluster with T=1 and T=15 information plus various other
            Map.entry(
                // protocols
                //                 T=1  T=1  T=2  T=1  T=15 T=4  T=15 T=5
                //  T0 A1C1D1 C2D2 A3D3 B4D4 A5D5 B6D6 A7D7 C8D8 B9D9 A B C
                "3b d0 18ffc0 0191 fea1 4292 37a1 159f 84c4 08af 0075 a1b2c3",
                List.of(
                    "Protocol indicator    TD2='91' => TA3 TD3 follow, 2nd offer is T=1",
                    "T=1 IFSC              TA3='fe' => 254 bytes",
                    "T=1 Waiting Times     TB3=---- => default value,"
                        + " BWT=11 etu + 5713920 clock cycles, CWT=8203 etu",
                    "T=1 Redundancy Code   TC3=---- => default value, LRC",
                    "Protocol indicator    TD3='a1' => TB4 TD4 follow, T=1 is offered once more",
                    "RFU                   TB4='42'",
                    "Protocol indicator    TD4='92' => TA5 TD5 follow, 3rd offer is T=2",
                    "RFU                   TA5='37'",
                    "Protocol indicator    TD5='a1' => TB6 TD6 follow, 4th offer is T=1",
                    "RFU                   TB6='15'",
                    "Protocol indicator    TD6='9f' => TA7 TD7 follow, 5th offer is T=15",
                    "ClockStop, Class      TA7='84' => clock stop supported in state H, class C",
                    "Standard/proprietary  TB7=---- => default value, contact C6 not used",
                    "Protocol indicator    TD7='c4' => TC8 TD8 follow, 6th offer is T=4",
                    "RFU                   TC8='08'",
                    "Protocol indicator    TD8='af' => TB9 TD9 follow, 7th offer is T=15",
                    "RFU                   TB9='00'",
                    "Protocol indicator    TD9='75' => TA10 TB10 TC10 follow, 8th offer is T=5",
                    "RFU                   TA10='a1'",
                    "RFU                   TB10='b2'",
                    "RFU                   TC10='c3'")))
        .forEach(
            (atr, expected) -> {
              final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));

              explanation.clear();
              dut.explainTd2plus(explanation);

              assertEquals(expected, explanation, atr);
            }); // end forEach((atr, expected) -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#firstTxCluster(int)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_firstTxCluster__int() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isPresentTd(int)-method works as expected
    // ... c. getTd(int)-method works as expected

    // Note 1: Because of the assertions we can be lazy here
    //         and concentrate on code coverage and correctness.
    // Note 2: The following code uses ATR with the following structure:
    //         TS T0 TD1 ... TDi ... TDn.
    // Note 3: Each TDi indicates a protocol. Either the protocol-under-test
    //         or any other protocol.
    // Note 4: Either the protocol-under-test is indicated or it is not indicated.
    // Note 5: If the protocol-under-test is indicated it is indicated after
    //         prefix-number of other protocols.
    // Note 6: After prefix-number of other protocols the protocol-under-test
    //         is or is not indicated.
    // Note 7: Afterwards postfix-number of TDi follow.
    // Note 8: If protocol-under-test is indicated then it may or may not be
    //         indicated in postfix-number of TDi.

    // Test strategy:
    // --- a. smoke test for protocol T=1
    // --- b. loop over relevant combinations of
    //     - protocol-under-test present or absent
    //     - other protocols indicated before or after protocol-under-test

    // --- a. smoke test for protocol T=1
    {
      //                                                           TS T0 TA1 TD1 TD2 TA3
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3f 90  97  80  11  40"));

      assertEquals(3, dut.firstTxCluster(1));
    } // end --- a.

    // --- b. loop over relevant combinations of
    //     - protocol-under-test present or absent
    //     - other protocols indicated before or after protocol-under-test
    // spotless:off
    IntStream.rangeClosed(0, 15).forEach(td1Protocol -> {
      //                                      TS T0 TD1
      final String atrPrefix = String.format("3b 80 %02x", td1Protocol);

      IntStream.rangeClosed(0, 15).forEach(protocolUnderTest ->
          List.of(false, true).forEach(isIndicated ->
              IntStream.rangeClosed(0, 10).forEach(prefixNumber -> {
                final StringBuilder prefix = new StringBuilder(atrPrefix);

                for (int i = 0; i < prefixNumber; i++) {
                  int tdi;
                  do {
                    tdi = RNG.nextIntClosed(0, 15);
                  } while (protocolUnderTest == tdi);

                  prefix.append(String.format("%02x", tdi));
                } // end For (i...)

                if (isIndicated) {
                  prefix.append(String.format("%02x", protocolUnderTest));
                } // end fi

                IntStream.rangeClosed(0, 10).forEach(postfixNumber -> {
                  final StringBuilder atrString = new StringBuilder(prefix.toString());

                  for (int i = 0; i < postfixNumber; i++) {
                    int tdi;
                    do {
                      tdi = RNG.nextIntClosed(0, 15);
                    } while (!isIndicated && (protocolUnderTest == tdi));

                    atrString.append(String.format("%02x", tdi));
                  } // end For (i...)

                  // convert String to byte[] and adjust bit
                  // b8 in non-final TDi
                  final byte[] atrInput =
                      Hex.toByteArray(atrString.toString());
                  for (int i = atrInput.length - 1;
                      i-- > 1; ) { // NOPMD assignments in
                    // operands
                    atrInput[i] |= (byte) 0x80;
                  } // end For (i...)
                  final AnswerToReset dut =
                      new AnswerToReset(atrInput);

                  assertEquals(
                      isIndicated ? (prefixNumber + 3) : -1,
                      dut.firstTxCluster(protocolUnderTest),
                      () -> String.format(
                          "ATR='%s': isIndicated=%s, T=%d, preNo=%d, posNo=%d, prefix=%s",
                          dut,
                          isIndicated,
                          protocolUnderTest,
                          prefixNumber,
                          postfixNumber,
                          prefix
                      )
                  );
                }); // end forEach(postfixNumber -> ...)
              }) // end forEach(prefixNumber -> ...)
          ) // end forEach(isIndicated -> ...)
      ); // end forEach(protocolUnderTest -> ...)
    }); // end forEach(td1Protocol -> ...)
    // spotless:on
  } // end method */

  /** Test method for {@link AnswerToReset#explainT1specificInterfaceBytes()}. */
  @Test
  void test_explainT1specificInterfaceBytes() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected
    // ... c. explainT1Ta1(int)-method works as expected
    // ... d. explainT1Tb1(int)-method works as expected
    // ... e. explainT1Tc1(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 80 80 11 2a"));

      final List<String> present = dut.explainT1specificInterfaceBytes();

      assertEquals(
          List.of(
              "T=1 IFSC              TA3='2a' => 42 bytes",
              "T=1 Waiting Times     TB3=---- => default value,"
                  + " BWT=11 etu + 5713920 clock cycles, CWT=8203 etu",
              "T=1 Redundancy Code   TC3=---- => default value, LRC"),
          present);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    // spotless:off
    Map.ofEntries(
        Map.entry(
            "3f 80 80 01", // T=1 specific cluster, empty
            List.of(
                "T=1 IFSC              TA3=---- => default value, 32 bytes",
                "T=1 Waiting Times     TB3=---- => default value,"
                    + " BWT=11 etu + 5713920 clock cycles, CWT=8203 etu",
                "T=1 Redundancy Code   TC3=---- => default value, LRC")),
        Map.entry(
            "3b 80 80 82 71 2b0201", // T=1 specific cluster, TA4, TB4, TC4 present
            List.of(
                "T=1 IFSC              TA4='2b' => 43 bytes",
                "T=1 Waiting Times     TB4='02' => BWT=11 etu + 357120 clock cycles, CWT=15 etu",
                "T=1 Redundancy Code   TC4='01' => CRC"))
    ).forEach((atr, expected) -> {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));

      assertEquals(expected, dut.explainT1specificInterfaceBytes());
    }); // end forEach((atr, expected) -> ...)
    // spotless:on
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#explainT15globalInterfaceBytes()}. */
  @Test
  void test_explainT15globalInterfaceBytes() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected
    // ... c. explainT15Ta1(int)-method works as expected
    // ... d. explainT15Tb1(int)-method works as expected
    // ... e. explainT1Tc1(int)-method works as expected

    // Note: Because of the assertions we can be lazy here
    //       and concentrate on code coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. bunch of manually chosen tests

    // --- a. smoke test
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 80 80 1f c3"));

      final List<String> present = dut.explainT15globalInterfaceBytes();

      assertEquals(
          List.of(
              "ClockStop, Class      TA3='c3' =>"
                  + " clock stop supported, no state preference, class A and B",
              "Standard/proprietary  TB3=---- => default value, contact C6 not used"),
          present);
    } // end --- a.

    // --- b. bunch of manually chosen tests
    Map.ofEntries(
            Map.entry(
                "3f 80 80 0f", // T=15 specific cluster, empty
                List.of(
                    "ClockStop, Class      TA3=---- =>"
                        + " default value, clock stop not supported, class A",
                    "Standard/proprietary  TB3=---- => default value, contact C6 not used")),
            Map.entry(
                "3b 80 80 82 7f 070042", // T=15 specific cluster, TA4, TB4, TC4 present
                List.of(
                    "ClockStop, Class      TA4='07' =>"
                        + " clock stop not supported, class A, B and C",
                    "Standard/proprietary  TB4='00' => contact C6 not used",
                    "RFU                   TC4='42'")))
        .forEach(
            (atr, expected) -> {
              final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(expected, dut.explainT15globalInterfaceBytes(), atr);
            }); // end forEach((atr, expected) -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#isT1DefaultParameter()}. */
  @Test
  void test_isT1DefaultParameter() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected

    // Test strategy:
    // --- a. T=1 not indicated
    // --- b. T=1 indicated but no T=1 specific cluster
    // --- c. T=1 indicated and T=1 specific cluster

    // --- a. T=1 not indicated
    {
      final AnswerToReset dut =
          new AnswerToReset(
              Hex.toByteArray(
                  //  T0 A1D1 C2D2 A3B3
                  "3f 90 15c0 013f 8100"));

      assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T1));
      assertFalse(dut.isT1DefaultParameter());
    } // end --- a.

    // --- b. T=1 indicated but no T=1 specific cluster
    {
      final AnswerToReset dut =
          new AnswerToReset(
              Hex.toByteArray(
                  //  T0 A1D1 A2D2 A3B3
                  "3f 90 1591 013f 8100"));

      assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T1));
      assertTrue(dut.isT1DefaultParameter());
    } // end --- b.

    // --- c. T=1 indicated and T=1 specific cluster
    final AnswerToReset dut =
        new AnswerToReset(
            Hex.toByteArray(
                //  T0 A1D1 A2D2 A3B3D3 A4B4
                "3f 90 1591 01b1 fe453f 8100"));

    assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T1));
    assertFalse(dut.isT1DefaultParameter());
  } // end method */

  /** Test method for {@link AnswerToReset#isT15DefaultParameter()}. */
  @Test
  void test_isT15DefaultParameter() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. firstTxCluster(int)-method works as expected

    // Test strategy:
    // --- a. T=15 not indicated
    // --- b. T=15 indicated but no T=15 specific cluster
    // --- c. T=15 indicated and T=15 specific cluster

    // --- a. T=15 not indicated
    {
      final AnswerToReset dut =
          new AnswerToReset(
              Hex.toByteArray(
                  //  T0 A1D1 C2D2 A3B3
                  "3f 90 15c0 0131 8100"));

      assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T15));
      assertTrue(dut.isT15DefaultParameter());
    } // end --- a.

    // --- b. T=15 indicated but no T=15 specific cluster
    {
      final AnswerToReset dut =
          new AnswerToReset(
              Hex.toByteArray(
                  //  T0 A1D1 A2D2 A3B3
                  "3f 90 159f 0131 8100"));

      assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T15));
      assertTrue(dut.isT15DefaultParameter());
    } // end --- b.

    // --- c. T=15 indicated and T=15 specific cluster
    final AnswerToReset dut =
        new AnswerToReset(
            Hex.toByteArray(
                //  T0 A1D1 A2D2 A3B3D3 A4B4
                "3f 90 1591 01b1 fe453f 8100"));

    assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T15));
    assertFalse(dut.isT15DefaultParameter());
  } // end method */

  /** Test method for {@link AnswerToReset#getTck()}. */
  @Test
  void test_getTck() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. TCK absent
    // --- b. TCK present

    // --- a. TCK absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      assertEquals(-1, dut.getTck());
    } // end --- a.

    // --- b. TCK present
    RNG.intsClosed(0, 0xff, 10)
        .forEach(
            tck -> {
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 00 %02x", tck)));

              assertEquals(tck, dut.getTck());
            }); // end forEach(tck -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link AnswerToReset#checkTck(List)}. */
  @Test
  void test_checkTck__List() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. isChecksumRequired()-method works as expected
    // ... c. getTck()-method works as expected

    // Test strategy:
    // --- a. TCK not required, absent
    // --- b. TCK   required,   present, correct
    // --- c. TCK not required, present
    // --- d. TCK   required,   absent
    // --- e. TCK   required,   present, wrong

    final List<String> findings = new ArrayList<>();

    // --- a. TCK not required, absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 80 00"));

      dut.checkTck(findings);

      assertTrue(findings.isEmpty(), findings::toString);
    } // end --- a.

    // --- b. TCK   required,   present, correct
    Set.of(
            //  T0 TD1 TD2 HiB TCK
            "3b 80  80  0f     0f",
            "3f 80  80  01     01",
            "3b 80  01         81",
            "3f 81  01     81  01")
        .forEach(
            atr -> {
              final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));

              dut.checkTck(findings);

              assertTrue(findings.isEmpty(), findings::toString);
            }); // end forEach(atr -> ...)
    // end --- b.

    // --- c. TCK not required, present
    Set.of(
            //  T0 TD1 TD2 HiB TCK
            "3b 00              0f", "3f 80  00          80", "3f 01          81  80")
        .forEach(
            atr -> {
              findings.clear();
              final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));

              dut.checkTck(findings);

              assertEquals(
                  List.of("checksum forbidden, but available, see ISO/IEC 7816-3 clause 8.2.5"),
                  findings,
                  atr);
            }); // end forEach(atr -> ...)
    // end --- c.

    // --- d. TCK   required,   absent
    Map.ofEntries(
            //         TS T0 TD1 TD2 HiB  TCK
            Map.entry("3b 80  80  0f", "0f"),
            Map.entry("3f 80  80  01", "01"),
            Map.entry("3b 80  01", "81"),
            Map.entry("3f 81  01     81", "01"))
        .forEach(
            (atr, tck) -> {
              findings.clear();
              final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));

              dut.checkTck(findings);

              assertEquals(
                  List.of(
                      String.format(
                          "checksum required, but absent, should be TCK='%s',"
                              + " see ISO/IEC 7816-3 clause 8.2.5",
                          tck)),
                  findings,
                  atr);
            }); // end forEach(atr -> ...)
    // end --- d.

    // --- e. TCK   required,   present, wrong
    Map.ofEntries(
            //         TS T0 TD1 TD2 HiB  TCK
            Map.entry("3b 80  80  0f       0e", "0f"),
            Map.entry("3f 80  80  01       fe", "01"),
            Map.entry("3b 80  01           80", "81"),
            Map.entry("3f 81  01     81    00", "01"))
        .forEach(
            (atr, tck) -> {
              findings.clear();
              final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));

              dut.checkTck(findings);

              assertEquals(
                  List.of(
                      String.format(
                          "wrong checksum, found TCK='%02x', should be TCK='%s',"
                              + " see ISO/IEC 7816-3 clause 8.2.5",
                          dut.getTck(), tck)),
                  findings,
                  atr);
            }); // end forEach(atr -> ...)
    // end --- e.
  } // end method */

  /** Test method for {@link AnswerToReset#explainTck()}. */
  @Test
  void test_explainTck() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. getTck()-method works as expected

    // Test strategy:
    // --- a. TCK absent
    // --- b. TCK present

    // --- a. TCK absent
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 00"));

      assertEquals("Checksum              TCK=----", dut.explainTck());
    } // end --- a.

    // --- b. TCK present
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3b 80 80 01 42"));

      assertEquals("Checksum              TCK='42'", dut.explainTck());
    } // end --- a.
  } // end method */

  /** Test method for {@link AnswerToReset#isChecksumRequired()}. */
  @Test
  void test_isChecksumRequired() {
    // Assertions:
    // ... a. constructor(s) work as expected

    // Test strategy:
    // --- a. minimal ATR, i.e. no protocol explicitly indicated
    // --- b. only T=0 indicated
    // --- c. just one protocol != T=0 indicated
    // --- d. T=0 and any other protocol indicated

    // --- a. minimal ATR, i.e. no protocol explicitly indicated
    {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray("3f 00"));

      assertEquals(1, dut.getSupportedProtocols().size());
      assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
      assertFalse(dut.isChecksumRequired());
    } // end --- a.

    // --- b. only T=0 indicated
    Set.of(
            //  T0 D1 C2D2 A3
            "3f 80 00", "3b 80 c0 0310 42")
        .forEach(
            atr -> {
              final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));

              assertEquals(1, dut.getSupportedProtocols().size());
              assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
              assertFalse(dut.isChecksumRequired());
            }); // end forEach(atr -> ...)
    // end --- b.

    // --- c. just one protocol != T=0 indicated
    IntStream.rangeClosed(1, 15)
        .forEach(
            protocol -> {
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 80 %02x", protocol)));

              assertEquals(1, dut.getSupportedProtocols().size());
              assertFalse(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
              assertTrue(dut.isChecksumRequired());
            }); // end forEach(protocol -> ...)

    // --- d. T=0 and any other protocol indicated
    IntStream.rangeClosed(1, 15)
        .forEach(
            protocol -> {
              final AnswerToReset dut =
                  new AnswerToReset(Hex.toByteArray(String.format("3f 80 80 %02x", protocol)));

              assertEquals(2, dut.getSupportedProtocols().size());
              assertTrue(dut.getSupportedProtocols().contains(EafiIccProtocol.T0));
              assertTrue(dut.isChecksumRequired());
            }); // end forEach(protocol -> ...)
  } // end method */

  /** Test method for {@link AnswerToReset#check()}. */
  @Test
  void test_check() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. underlying methods work as expected

    // Note: Because of the assertions we can be lazy here.

    // Test strategy:
    // --- a. smoke test with no findings
    // --- b. FIXME bunch of ATR with one finding each

    // --- a. smoke test with no findings
    for (final String atr : new String[] {"3b 00"}) {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr)); // NOPMD new in loop

      final List<String> findings = dut.check();

      assertTrue(findings.isEmpty(), dut::toString);
    } // end For (atr...)
    // end --- a.

    // --- b. FIXME bunch of ATR with one finding each
  } // end method */

  /** Test method for {@link AnswerToReset#explain()}. */
  @Test
  void test_explain() {
    // Assertions:
    // ... a. constructor(s) work as expected
    // ... b. underlying methods work as expected

    // Note: Because of the assertions, we can be lazy here.

    // Test strategy:
    // --- a. smoke test with minimal ATR
    // --- b. bunch of ATR

    final String ta1Explanation =
        String.format(
            "Clock rate, baud rate TA1=---- => default values, Fi=372, Di=1, fmax=%.1f MHz", 5.0);

    // --- a. smoke test with minimal ATR
    for (final String atr : new String[] {"3b 00"}) {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr)); // NOPMD new in loop

      final List<String> explanation = dut.explain();

      assertEquals(
          List.of(
              "Initial Character     TS ='3b' => direct convention",
              "Format byte           T0 ='00' => no interface bytes follow, no historical bytes",
              ta1Explanation,
              "Extra guard time      TC1=---- => default value, 12 etu",
              "Protocol indicator    TD1=---- => implicit offer is T=0",
              "Specific mode byte    TA2=---- => negotiable mode",
              "T=0 Waiting time      TC2=---- => default value, WI=10",
              "ClockStop, Class      TA =---- => default value, clock stop not supported, class A",
              "Standard/proprietary  TB =---- => default value, contact C6 not used",
              "Checksum              TCK=----"),
          explanation,
          dut::toString);
    } // end For (atr...)
    // end --- a.

    // --- b. bunch of ATR
    // spotless:off
    Map.ofEntries(
        Map.entry(
            "3b 80 01 81", // default T=1 parameter
            List.of(
                "Initial Character     TS ='3b' => direct convention",
                "Format byte           T0 ='80' => TD1 follow, no historical bytes",
                ta1Explanation,
                "Extra guard time      TC1=---- => default value, 12 etu",
                "Protocol indicator    TD1='01' => no interface bytes follow, only offer is T=1",
                "Specific mode byte    TA2=---- => negotiable mode",
                "T=1 IFSC              TA =---- => default value, 32 bytes",
                "T=1 Waiting Times     TB =---- => default value,"
                    + " BWT=11 etu + 5713920 clock cycles, CWT=8203 etu",
                "T=1 Redundancy Code   TC =---- => default value, LRC",
                "ClockStop, Class      TA =---- => default value, clock stop not supported,"
                    + " class A",
                "Standard/proprietary  TB =---- => default value, contact C6 not used",
                "Checksum              TCK='81'")),
        Map.entry(
            "3b 80 81 11 fe ee", // non default T=1 parameter
            List.of(
                "Initial Character     TS ='3b' => direct convention",
                "Format byte           T0 ='80' => TD1 follow, no historical bytes",
                ta1Explanation,
                "Extra guard time      TC1=---- => default value, 12 etu",
                "Protocol indicator    TD1='81' => TD2 follow, only offer is T=1",
                "Specific mode byte    TA2=---- => negotiable mode",
                "Protocol indicator    TD2='11' => TA3 follow, T=1 is offered once more",
                "T=1 IFSC              TA3='fe' => 254 bytes",
                "T=1 Waiting Times     TB3=---- => default value,"
                    + " BWT=11 etu + 5713920 clock cycles, CWT=8203 etu",
                "T=1 Redundancy Code   TC3=---- => default value, LRC",
                "ClockStop, Class      TA =---- => default value, clock stop not supported,"
                    + " class A",
                "Standard/proprietary  TB =---- => default value, contact C6 not used",
                "Checksum              TCK='ee'")),
        Map.entry(
            "3b 80 80 1f c1 de", // non default T=15 parameter
            List.of(
                "Initial Character     TS ='3b' => direct convention",
                "Format byte           T0 ='80' => TD1 follow, no historical bytes",
                ta1Explanation,
                "Extra guard time      TC1=---- => default value, 12 etu",
                "Protocol indicator    TD1='80' => TD2 follow, 1st offer is T=0",
                "Specific mode byte    TA2=---- => negotiable mode",
                "T=0 Waiting time      TC2=---- => default value, WI=10",
                "Protocol indicator    TD2='1f' => TA3 follow, 2nd offer is T=15",
                "ClockStop, Class      TA3='c1' => clock stop supported, no state preference,"
                    + " class A",
                "Standard/proprietary  TB3=---- => default value, contact C6 not used",
                "Checksum              TCK='de'"))
    ).forEach((atr, expected) -> {
      final AnswerToReset dut = new AnswerToReset(Hex.toByteArray(atr));

      final List<String> present = dut.explain();

      assertEquals(expected, present, atr);
    }); // end forEach((atr, expected) -> ...)
    // spotless:on
    // end --- b.
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /**
   * This class creates a random ATR without historical bytes and without check byte.
   *
   * <p>In particular:
   *
   * <ol>
   *   <li>Initial character TS has a random value in range [0, 255].
   *   <li>Format byte T0: High nibble correctly indicates presence and absence of TA1, TB1, TC1 and
   *       TD1, low nibble is zero (i.e. no historical bytes).
   *   <li>Interface bytes TA1, TB1, ..., TAi, TBi, ..., TAn, TBn, TCn, TDn are randomly chosen:
   *       <ol>
   *         <li>TAi, TBi, TCi are randomly present or absent.
   *         <li>If TAi, TBi or TCi are present then their values is randomly chosen from range
   *             [0,255].
   *         <li>The high nibble of TDi correctly indicates presence and absence of TA(i+1),
   *             TB(i+1), TC(i+1) and TD(i+1) with {@code i &ge; 1}.
   *         <li>The low nibble of TDi is randomly chosen (random protocol).
   *       </ol>
   *   <li>Historical bytes are absent.
   *   <li>Check byte TCK is absent.
   * </ol>
   */
  private static final class RandomAtr {

    /** Answer-To-Reset as hex-digits. */
    private final String insAtr; // */

    /**
     * List of expected interface bytes.
     *
     * <p>In particular:
     *
     * <ol>
     *   <li>The first list element (i.e. index = 0) contains information about TA1, TB1, TC1 and
     *       TD1.
     *   <li>The second list element (i.e. index = 1) (if present) contains information about TA2,
     *       TB2, TC2 and TD2.
     *   <li>etc.
     *   <li>Each map uses {@link String} as key mapping to {@link Integer}.
     *   <li>Each map has the keys "A", "B", "C" and "D".
     *   <li>The corresponding values are {@link Integer} from range [-1, 255]. The value -1
     *       indicates that the corresponding interface byte is absent.
     * </ol>
     */
    private final List<Map<String, Integer>> insList; // */

    /**
     * Constructor.
     *
     * @param numberOfCluster indicates the number of clusters for interface bytes, <b>SHALL</b> be
     *     {@code &ge; 1},
     */
    @SuppressWarnings({
      "PMD.CognitiveComplexity",
      "PMD.CyclomaticComplexity",
      "PMD.NPathComplexity"
    })
    private RandomAtr(final int numberOfCluster) {
      final StringBuilder atr = new StringBuilder();
      final List<Map<String, Integer>> list = new ArrayList<>();

      // estimate last cluster
      final boolean isTdPresent = RNG.nextBoolean(); // presence or absence of TD in last cluster
      boolean isTcPresent = RNG.nextBoolean();
      boolean isTbPresent = RNG.nextBoolean();
      boolean isTaPresent = RNG.nextBoolean();

      int td = isTdPresent ? RNG.nextIntClosed(0, 15) : -1; // no more interface bytes, rnd protocol
      int tc = isTcPresent ? RNG.nextIntClosed(0, 255) : -1;
      int tb = isTbPresent ? RNG.nextIntClosed(0, 255) : -1;
      int ta = isTaPresent ? RNG.nextIntClosed(0, 255) : -1;

      for (final int tx : new int[] {td, tc, tb, ta}) {
        if (tx >= 0) {
          atr.insert(0, String.format("%02x", tx));
        } // end fi
      } // end For (tx...)

      list.add(
          Map.of(
              "A", ta,
              "B", tb,
              "C", tc,
              "D", td));

      td =
          RNG.nextIntClosed(0, 15) // arbitrary protocol
              + (isTaPresent ? 0x10 : 0)
              + (isTbPresent ? 0x20 : 0)
              + (isTcPresent ? 0x40 : 0)
              + (isTdPresent ? 0x80 : 0);

      while (list.size() < numberOfCluster) {
        isTcPresent = RNG.nextBoolean();
        isTbPresent = RNG.nextBoolean();
        isTaPresent = RNG.nextBoolean();

        tc = isTcPresent ? RNG.nextIntClosed(0, 255) : -1;
        tb = isTbPresent ? RNG.nextIntClosed(0, 255) : -1;
        ta = isTaPresent ? RNG.nextIntClosed(0, 255) : -1;

        for (final int tx : new int[] {td, tc, tb, ta}) { // NOPMD new in loop
          if (tx >= 0) {
            atr.insert(0, String.format("%02x", tx));
          } // end fi
        } // end For (tx...)

        list.addFirst(
            Map.of(
                "A", ta,
                "B", tb,
                "C", tc,
                "D", td));

        // arbitrary protocol
        td =
            RNG.nextIntClosed(0, 15) // arbitrary protocol
                + (isTaPresent ? 0x10 : 0)
                + (isTbPresent ? 0x20 : 0)
                + (isTcPresent ? 0x40 : 0)
                + 0x80; // TDi always present in non-last cluster
      } // end While (not enough clusters)
      // ... enough cluster

      atr.insert(0, String.format("%02x", td & 0xf0)); // add format byte T0, no historical bytes
      atr.insert(0, String.format("%02x", RNG.nextIntClosed(0, 255))); // add initial character TS

      insAtr = atr.toString();
      insList = list;
    } // end constructor */

    /**
     * Retrieve attribute Answer-To-Reset.
     *
     * @return {@link String} representation of octets in ATR
     */
    /* package */ String getAtr() {
      return insAtr;
    } // end method */

    /**
     * Retrieve list with information about interface bytes.
     *
     * @return list with interface byte information.
     */
    /* package */ List<Map<String, Integer>> getList() {
      return insList;
    } // end method */
  } // end inner class
} // end class
