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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Class performing white-box tests on {@link Base45}. */
// Note 1: Spotbugs claims "DMI_RANDOM_USED_ONLY_ONCE",
//         Short message: Random object created and used only once
//         That finding is suppressed because using the test-object once is
//         intentional.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "DMI_RANDOM_USED_ONLY_ONCE" // see note 1
}) // */
@SuppressWarnings({"PMD.MethodNamingConventions", "checkstyle:methodname"})
final class TestBase45 {

  /** Pseudo random number generator, used for random test vectors. */
  private static final RandomGenerator RNG = new Random(System.nanoTime()); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Test strategy:
    // --- a. check characters used for encoding
    // Note: See
    //       a. https://en.wikipedia.org/wiki/QR_code#Storage
    //       b. https://www.thonky.com/qr-code-tutorial/alphanumeric-mode-encoding
    assertEquals('0', Base45.CHARS[0]);
    assertEquals('1', Base45.CHARS[1]);
    assertEquals('2', Base45.CHARS[2]);
    assertEquals('3', Base45.CHARS[3]);
    assertEquals('4', Base45.CHARS[4]);
    assertEquals('5', Base45.CHARS[5]);
    assertEquals('6', Base45.CHARS[6]);
    assertEquals('7', Base45.CHARS[7]);
    assertEquals('8', Base45.CHARS[8]);
    assertEquals('9', Base45.CHARS[9]);
    assertEquals('A', Base45.CHARS[10]);
    assertEquals('B', Base45.CHARS[11]);
    assertEquals('C', Base45.CHARS[12]);
    assertEquals('D', Base45.CHARS[13]);
    assertEquals('E', Base45.CHARS[14]);
    assertEquals('F', Base45.CHARS[15]);
    assertEquals('G', Base45.CHARS[16]);
    assertEquals('H', Base45.CHARS[17]);
    assertEquals('I', Base45.CHARS[18]);
    assertEquals('J', Base45.CHARS[19]);
    assertEquals('K', Base45.CHARS[20]);
    assertEquals('L', Base45.CHARS[21]);
    assertEquals('M', Base45.CHARS[22]);
    assertEquals('N', Base45.CHARS[23]);
    assertEquals('O', Base45.CHARS[24]);
    assertEquals('P', Base45.CHARS[25]);
    assertEquals('Q', Base45.CHARS[26]);
    assertEquals('R', Base45.CHARS[27]);
    assertEquals('S', Base45.CHARS[28]);
    assertEquals('T', Base45.CHARS[29]);
    assertEquals('U', Base45.CHARS[30]);
    assertEquals('V', Base45.CHARS[31]);
    assertEquals('W', Base45.CHARS[32]);
    assertEquals('X', Base45.CHARS[33]);
    assertEquals('Y', Base45.CHARS[34]);
    assertEquals('Z', Base45.CHARS[35]);
    assertEquals(' ', Base45.CHARS[36]);
    assertEquals('$', Base45.CHARS[37]);
    assertEquals('%', Base45.CHARS[38]);
    assertEquals('*', Base45.CHARS[39]);
    assertEquals('+', Base45.CHARS[40]);
    assertEquals('-', Base45.CHARS[41]);
    assertEquals('.', Base45.CHARS[42]);
    assertEquals('/', Base45.CHARS[43]);
    assertEquals(':', Base45.CHARS[44]);
    assertEquals(45, Base45.CHARS.length);
    assertEquals(45, Base45.MODUL);
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

  /** Test method for {@link Base45#decode(CharSequence)}. */
  @Test
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  void test_decode__CharSequence() {
    // Test strategy:
    // --- a. test vectors from specification
    // --- b. manually chosen corner cases
    // --- c. all one byte inputs
    // --- d. all two byte inputs
    // --- e. ERROR: Invalid number of input characters
    // --- f. ERROR: Invalid characters in input string
    // --- g. ERROR: Invalid character-triple

    // --- a. test vectors from specification
    for (final String[] i :
        new String[][] {
          new String[] {"AB", "BB8"}, // base45, clause 4.1, example 1
          new String[] {"Hello!!", "%69 VD92EX0"}, // base45, clause 4.1, example 2
          new String[] {"base-45", "UJCLQE7W581"}, // base45, clause 4.1, example 3
          new String[] {"ietf!", "QED8WEX0"}, // base45, clause 4.2, example 1
        }) {
      final String octets = i[0];
      final String base45 = i[1];
      assertEquals(octets, new String(Base45.decode(base45), StandardCharsets.UTF_8)); // NOPMD new
    } // end For (i...)

    // --- b. manually chosen corner cases
    assertArrayEquals(new byte[0], Base45.decode(""));
    assertArrayEquals(new byte[1], Base45.decode("00"));
    assertArrayEquals(new byte[2], Base45.decode("000"));
    assertArrayEquals(new byte[3], Base45.decode("00000"));

    // --- c. all one byte inputs
    {
      final byte[] octets = new byte[1]; // start with octets = ´00´
      int cint = 0;
      int dint = 0;
      final var chars = new char[2];
      do {
        chars[0] = Base45.CHARS[cint];
        chars[1] = Base45.CHARS[dint];
        final var input = new String(chars); // NOPMD new in loop
        assertArrayEquals(octets, Base45.decode(input));

        if (++cint == Base45.MODUL) { // NOPMD assignment in operand
          // ... cInt increased beyond maximum value
          //     => reset indexC and increment indexD
          cint = 0;
          dint++;
        } // end fi

        octets[0]++; // increment content of octets[0]
      } while (0 != octets[0]);
    } // end --- c.

    // --- d. all two byte inputs
    {
      final byte[] octets = new byte[2];
      int cint = 0;
      int dint = 0;
      int eint = 0;
      final var chars = new char[3];
      for (; ; ) {
        chars[0] = Base45.CHARS[cint];
        chars[1] = Base45.CHARS[dint];
        chars[2] = Base45.CHARS[eint];
        final String input = new String(chars); // NOPMD new in loop
        assertArrayEquals(octets, Base45.decode(input), input);

        if (++cint == Base45.MODUL) { // NOPMD assignment in operand
          // ... cInt increased beyond maximum value
          //     => reset indexC and increment indexD
          cint = 0;

          if (++dint == Base45.MODUL) { // NOPMD assignment in operand
            // ... dInt increased beyond maximum value
            //     => reset dInt and increment eInt
            dint = 0;
            eint++;
          } // end fi
        } // end fi

        octets[1] = (byte) (octets[1] + 1);

        if (0 == octets[1]) {
          // ... overflow in least significant byte
          //     => increment next byte
          octets[0] = (byte) (octets[0] + 1);

          if (0 == octets[0]) {
            // ... overflow in most significant byte, all possible values tested
            //     => end of loop reached
            break;
          } // end fi
        } // end fi
      } // end For (...)
    }

    // --- e. ERROR: Invalid number of input characters
    Arrays.stream(new String[] {"1", "1234", "1234567"})
        .forEach(
            input -> {
              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> Base45.decode(input));
              assertEquals("invalid number of input character", throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(input -> ...)

    // --- f. ERROR: Invalid characters in input string
    Arrays.stream(
            new String[] {
              "_a", // 1st character invalid
              "a#", // 2nd character invalid
              "_23",
              "1(3456",
              "12=456789",
            })
        .forEach(
            input -> {
              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> Base45.decode(input));
              assertEquals("invalid character(s)", throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(input -> ...)

    // --- g. ERROR: Invalid character-triple
    {
      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> Base45.decode(":::"));
      assertEquals("invalid code", throwable.getMessage());
      assertNull(throwable.getCause());
    }
  } // end method */

  /** Test method for {@link Base45#decode(char, char, char)}. */
  @Test
  void test_decode__char3() {
    // Test strategy:
    // --- a. loop over all possible input values
    // --- b. ERROR: Invalid code
    // --- c. ERROR: invalid characters

    // --- a. loop over all possible input values
    // --- b. ERROR: Invalid code
    {
      final char[] chars = Base45.ALPHABET.toCharArray();
      for (final char charC : chars) {
        final int intC = Base45.ALPHABET.indexOf(charC);

        for (final char charD : chars) {
          final int intD = Base45.ALPHABET.indexOf(charD);

          for (final char charE : chars) {
            final int intE = Base45.ALPHABET.indexOf(charE);
            final int expected = intC + Base45.MODUL * (intD + Base45.MODUL * intE);

            if (expected < 0x1_0000) { // NOPMD literal in if statement
              // ... character-triple is okay
              assertEquals(expected, Base45.decode(charC, charD, charE));
            } else {
              // ... character-triple NOT okay, output exceeds two-byte range
              final Throwable throwable =
                  assertThrows(
                      IllegalArgumentException.class, () -> Base45.decode(charC, charD, charE));
              assertEquals("invalid code", throwable.getMessage());
              assertNull(throwable.getCause());
            } // end else
          } // end For (charE...)
        } // end For (charD...)
      } // end For (charC...)
    }

    // --- c. ERROR: invalid characters
    for (final char[] i :
        new char[][] {
          new char[] {'!', 'A', ' '}, // 1st character invalid
          new char[] {'2', '_', '$'}, // 2nd character invalid
          new char[] {'3', 'C', '('}, // 3rd character invalid
        }) {
      final char charC = i[0];
      final char charD = i[1];
      final char charE = i[2];

      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> Base45.decode(charC, charD, charE));
      assertEquals("invalid character(s)", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end For (i...)
  } // end method */

  /** Test method for {@link Base45#encode(byte[])}. */
  @Test
  void test_encode__byteA() {
    // Test strategy:
    // --- a. test vectors from specification
    // --- b. manually chosen corner cases
    // --- c. all one byte inputs
    // --- d. all two byte inputs
    // --- e. some 3 byte inputs
    // --- f. some 4 byte inputs
    // --- g. some 5 byte inputs

    // --- a. test vectors from specification
    for (final String[] i :
        new String[][] {
          new String[] {"AB", "BB8"}, // base45, clause 4.1, example 1
          new String[] {"Hello!!", "%69 VD92EX0"}, // base45, clause 4.1, example 2
          new String[] {"base-45", "UJCLQE7W581"}, // base45, clause 4.1, example 3
          new String[] {"ietf!", "QED8WEX0"}, // base45, clause 4.2, example 1
        }) {
      final String octets = i[0];
      final String base45 = i[1];
      assertEquals(base45, Base45.encode(octets.getBytes(StandardCharsets.UTF_8)));
    } // end For (i...)

    // --- b. manually chosen corner cases
    assertEquals(0, Base45.encode(new byte[0]).length()); // empty input
    assertEquals("00", Base45.encode(new byte[1])); // ´00´
    assertEquals("000", Base45.encode(new byte[2])); // ´0000´
    assertEquals("00000", Base45.encode(new byte[3])); // ´0000 00´

    // --- c. all one byte inputs
    {
      final var inputC = new byte[1];
      final var chars = new char[2];
      for (int i = 0; i < 0x100; i++) {
        inputC[0] = (byte) i;
        chars[0] = Base45.CHARS[i % Base45.MODUL];
        chars[1] = Base45.CHARS[i / Base45.MODUL];
        assertEquals(
            new String(chars), // NOPMD new in loop
            Base45.encode(inputC));
      } // end For (i...)
    } // end --- c.

    // --- d. all two byte inputs
    {
      final var inputD = new byte[2];
      final var chars = new char[3];
      for (int a = 0; a < 0x100; a++) {
        inputD[0] = (byte) a;
        for (int b = 0; b < 0x100; b++) {
          inputD[1] = (byte) b;

          final int word = (a << 8) + b;
          chars[0] = Base45.CHARS[word % Base45.MODUL];
          chars[1] = Base45.CHARS[(word / Base45.MODUL) % Base45.MODUL];
          chars[2] = Base45.CHARS[word / Base45.MODUL / Base45.MODUL];

          assertEquals(
              new String(chars), // NOPMD new in loop
              Base45.encode(inputD));
        } // end For (b...)
      } // end For (a...)
    } // end --- d.

    // --- e. some 3 byte inputs
    // Note: Hereafter is assumed that 1 and 2 byte inputs are encoded correctly.
    IntStream.rangeClosed(0, 1000)
        .forEach(
            i -> {
              final byte[] input = new byte[3];
              RNG.nextBytes(input); // DMI_RANDOM_USED_ONLY_ONCE
              assertEquals(
                  Base45.encode(Arrays.copyOfRange(input, 0, 2))
                      + Base45.encode(Arrays.copyOfRange(input, 2, 3)),
                  Base45.encode(input));
            });

    // --- f. some 4 byte inputs
    // Note: Hereafter is assumed that 2 byte inputs are encoded correctly.
    IntStream.rangeClosed(0, 1000)
        .forEach(
            i -> {
              final byte[] input = new byte[4];
              RNG.nextBytes(input); // DMI_RANDOM_USED_ONLY_ONCE
              assertEquals(
                  Base45.encode(Arrays.copyOfRange(input, 0, 2))
                      + Base45.encode(Arrays.copyOfRange(input, 2, 4)),
                  Base45.encode(input));
            });

    // --- g. some 5 byte inputs
    // Note: Hereafter is assumed that 1 and 4 byte inputs are encoded correctly.
    IntStream.rangeClosed(0, 1000)
        .forEach(
            i -> {
              final byte[] input = new byte[5];
              RNG.nextBytes(input); // DMI_RANDOM_USED_ONLY_ONCE
              assertEquals(
                  Base45.encode(Arrays.copyOfRange(input, 0, 4))
                      + Base45.encode(Arrays.copyOfRange(input, 4, 5)),
                  Base45.encode(input));
            });
  } // end method */

  /** Test method for {@link Base45#encode(int[], char)}. */
  @Test
  void test_encode__char__intA() {
    // Test strategy:
    // --- a. Loop over all possible input values
    final int[] encoded = new int[3];
    for (int input = 0; input < 0x1_0000; input++) {
      final int intC = input % Base45.MODUL;
      final int intD = (input / Base45.MODUL) % Base45.MODUL;
      final int intE = input / Base45.MODUL / Base45.MODUL;

      Base45.encode(encoded, (char) input);

      assertEquals(intC, encoded[0]);
      assertEquals(intD, encoded[1]);
      assertEquals(intE, encoded[2]);
    } // end For (input...)
  } // end method */
} // end class
