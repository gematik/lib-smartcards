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
package de.gematik.smartcards.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link Hex}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestHex {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestHex.class); // */

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

  /** Test method for {@link Hex#extractHexDigits(CharSequence)}. */
  @Test
  void test_extractHexDigits__String() {
    // Test strategy:
    // --- a. manual smoke tests check the base functionality and corner cases
    // --- b. from a String containing all possible char-values hex-digits are extracted
    // --- c. from a String containing all possible code-points hex-digits are extracted

    // --- a. manual smoke tests check the base functionality and corner cases
    Map.ofEntries(
            Map.entry("", ""),
            Map.entry("kghKl \njQw<\tx$�!\r�#�~äöüÄÖÜßéèáàâê", ""),
            Map.entry("Q@ e/&", "e"),
            Map.entry("fTQ�\"1", "f1"),
            Map.entry("-3/ hH7* +9W|", "379"),
            Map.entry("KJI)04Pܴ 7 32RMaf87' +e59164", "04732af87e59164"),
            Map.entry("8\\04$ 732ALKPF87e59164\t", "804732af87e59164"))
        .forEach((input, output) -> assertEquals(output, Hex.extractHexDigits(input), input));

    // --- b. from a String containing all possible char-values hex-digits are extracted
    {
      final char[] chars = new char[0x10000];
      for (int i = chars.length; i-- > 0; ) { // NOPMD avoid assignment in operands
        chars[i] = (char) i;
      } // end For (i...)
      assertEquals(
          "0123456789abcdefabcdef",
          Hex.extractHexDigits(new String(chars)), // NOPMD String instantiation
          "20");
    } // end all possible char values

    // --- c. from a String containing all possible code-points hex-digits are extracted
    {
      // some manual checks (also for getting familiar with the underlying method from String-class
      assertEquals(
          "a5",
          Hex.extractHexDigits(
              new String(
                  new int[] {'0', 'A', '5'},
                  1, // offset
                  2 // length
                  )),
          "30");

      // check all code point values
      final int[] codePoints = new int[0x10ffff + 1];
      for (int i = codePoints.length; i-- > 0; ) { // NOPMD avoid assignment in operands
        codePoints[i] = i;
      } // end For (i...)
      assertEquals(
          "0123456789abcdefabcdef",
          Hex.extractHexDigits(new String(codePoints, 0, codePoints.length)),
          "39");
    } // end check code points
  } // end method */

  /** Test method for {@link Hex#toByteArray(CharSequence)} )}. */
  @Test
  void test_toByteArray__String() {
    // Test strategy:
    // --- a. all possible inputs (without extra characters) leading to one byte results are tested
    // --- b. some manual tests containing corner cases
    // --- c. odd length input

    // --- a. all possible inputs (without extra characters) leading to one byte results are tested
    IntStream.range(0, 256)
        .forEach(
            i -> {
              // lower case hex-digits
              assertArrayEquals(new byte[] {(byte) i}, Hex.toByteArray(String.format("%02x", i)));

              // upper case hex-digits
              assertArrayEquals(new byte[] {(byte) i}, Hex.toByteArray(String.format("%02X", i)));
            }); // end forEeach(i -> ...)

    // --- b. some manual tests containing corner cases
    assertEquals(0, Hex.toByteArray("").length, "20.empty");
    assertEquals(0, Hex.toByteArray("kghKl \njQw<\tx$�!\r�#�~").length, "22");

    // --- c. odd length input
    Arrays.stream(
            new String[] {
              "4", "a\t B�P9", "x3dwfg",
            })
        .forEach(
            i -> {
              final Exception exception =
                  assertThrows(IllegalArgumentException.class, () -> Hex.toByteArray(i), "30");
              assertEquals(
                  "Number of hex-digits in <" + i + "> is odd", exception.getMessage(), "32");
              assertNull(exception.getCause(), "34");
            }); // end forEach(i -> ...)

    // Note: More test are in test_toHexDigits__byteA
  } // end method */

  /** Test method for {@link Hex#toHexDigits(byte[])}. */
  @Test
  void test_toHexDigits__byteA() {
    // Test strategy:
    // --- a. corner cases
    // --- b. all possible input-arrays with one octet length are tested
    // --- c. manual tests for input arrays with more than one octet
    // --- d. some random arrays

    // --- a. corner cases
    assertEquals("", Hex.toHexDigits(new byte[0]), "{}");

    // --- b. all possible input-arrays with one octet length are tested
    IntStream.range(0, 256)
        .forEach(
            i ->
                assertEquals(
                    String.format("%02x", i),
                    Hex.toHexDigits(new byte[] {(byte) i}))); // end forEeach(i -> ...)

    // --- c. manual tests for input arrays with more than one octet
    assertEquals(
        "39087286f20032",
        Hex.toHexDigits(
            new byte[] {
              (byte) 0x39,
              (byte) 0x08,
              (byte) 0x72,
              (byte) 0x86,
              (byte) 0xf2,
              (byte) 0x00,
              (byte) 0x32,
            }),
        "20");

    // --- d. some random arrays
    IntStream.range(0, 256)
        .forEach(
            i -> {
              final byte[] byteA = RNG.nextBytes(i);
              final String string = Hex.toHexDigits(byteA);
              final byte[] byteB = Hex.toByteArray(string);
              assertNotSame(byteA, byteB, Integer.toString(i));
              assertArrayEquals(byteA, byteB, string);
            }); // end forEach(i -> ...)
  } // end method */

  /** Test method for {@link Hex#toHexDigits(byte[], int, int)}. */
  @Test
  void test_toHexDigits__byteA_int_int() {
    // Test strategy:
    // --- a. manual (smoke) tests
    // --- b. offset too big
    // --- c. length too big

    // --- a. manual (smoke) tests
    assertEquals("4711", Hex.toHexDigits(new byte[] {0x00, 0x47, 0x11, 0x02}, 1, 2), "00");

    Arrays.stream(
            new String[][] {
              {"01020304", "0", "4", "01020304"},
              {"02345678", "1", "0", ""},
              {"3abdfd78", "1", "2", "bdfd"},
            })
        .forEach(
            i -> {
              final byte[] input = Hex.toByteArray(i[0]);
              final int offset = Integer.parseInt(i[1]);
              final int length = Integer.parseInt(i[2]);
              final String expected = i[3];
              assertEquals(expected, Hex.toHexDigits(input, offset, length), "10");
            }); // end forEach(i -> ...)

    // --- b. offset too big
    IntStream.range(0, 32)
        .forEach(
            i -> {
              final byte[] input = RNG.nextBytes(1, 100);
              final int offsetOkay = input.length - 1; // offset just okay

              assertEquals(
                  String.format("%02x", input[offsetOkay] & 0xff),
                  Hex.toHexDigits(input, offsetOkay, 1));

              final int offsetNok = offsetOkay + 1; // offset just NOT okay
              final Throwable throwable =
                  assertThrows(
                      ArrayIndexOutOfBoundsException.class,
                      () -> Hex.toHexDigits(input, offsetNok, 1));
              final String message = throwable.getMessage();
              if (null == message) {
                // ... unexpected behavior, but happens sometimes
                // Note: This is probably a race incident.
                LOGGER.atWarn().log("afiUnexpected:", throwable);
              } else {
                // ... expected behavior
                assertEquals(
                    String.format("Index %d out of bounds for length %d", offsetNok, offsetNok),
                    throwable.getMessage());
                assertNull(throwable.getCause());
              } // end fi
            }); // end forEeach(i -> ...)

    // --- c. length too big
    final int numberOfRounds = 100;
    final double nulls =
        IntStream.range(0, numberOfRounds)
            .map(
                i -> { // count number of null-messages in exception
                  final byte[] input = RNG.nextBytes(1, 20);
                  final int offset = RNG.nextIntClosed(0, input.length - 1);
                  final int lengthOkay = input.length - offset; // length just okay

                  final StringBuilder expected = new StringBuilder();
                  for (int j = offset; j < input.length; j++) {
                    expected.append(String.format("%02x", input[j] & 0xff));
                  } // end For (j...)
                  assertEquals(expected.toString(), Hex.toHexDigits(input, offset, lengthOkay));

                  final int lengthNok = lengthOkay + 1; // length just NOT okay
                  final Throwable throwable =
                      assertThrows(
                          ArrayIndexOutOfBoundsException.class,
                          () -> Hex.toHexDigits(input, offset, lengthNok));

                  // Note: Because for performance boost hex-digits are calculated in parallel
                  // sometimes
                  //       the message in the exception is null. In that case either cause contains
                  // the
                  //       expected exception, or cause is also null.
                  int result = 0;
                  if (null == throwable.getMessage()) {
                    // ... message null => don't check anything
                    result++;
                  } else {
                    // ... message not null => check it
                    assertEquals(
                        String.format(
                            "Index %d out of bounds for length %d", input.length, input.length),
                        throwable.getMessage());
                    assertNull(throwable.getCause());
                  } // end fi
                  return result;
                })
            .sum();
    final double ratio = nulls / numberOfRounds * 100;
    LOGGER.atInfo().log(
        "test_toHexDigits_byteA_int_int: ratio = {} / {} = {}%.",
        Math.round(nulls), numberOfRounds, ratio);
  } // end method */
} // end class
