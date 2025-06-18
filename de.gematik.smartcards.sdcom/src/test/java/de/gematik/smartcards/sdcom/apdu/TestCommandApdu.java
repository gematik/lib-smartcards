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

import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NC_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NC_SUPREMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NC_SUPREMUM_SHORT;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_EXTENDED_WILDCARD;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_SHORT_WILDCARD;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_SUPREMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_SUPREMUM_SHORT;
import static de.gematik.smartcards.utils.AfiUtils.EMPTY_OS;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for white-box testing of {@link CommandApdu}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_LOAD_OF_KNOWN_NULL_VALUE".
//         Spotbugs message: The variable referenced at this point is known to be
//             null due to an earlier check against null. Although this is valid,
//             it might be a mistake (perhaps you intended to refer to a different
//             variable, or perhaps the earlier check to see if the variable is
//             null should have been a check to see if it was non-null).
//         Rational: That finding is suppressed because intentionally a null-value is used.
// Note 2: Spotbugs claims "RV_RETURN_VALUE_IGNORED_INFERRED".
//         Spotbugs message: This code calls a method and ignores the return value.
//         Rational: Intentionally the return value is ignored.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_LOAD_OF_KNOWN_NULL_VALUE", // see note 1
  "RV_RETURN_VALUE_IGNORED_INFERRED" // see note 2
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestCommandApdu {

  /** Message in case command data field absent. */
  private static final String CMD_DATA_ABSENT = "command data field absent, but SHALL be present";

  /** Message in case command data field is too long. */
  private static final String CMD_DATA_TOO_LONG = "command data field too long"; // */

  /** Message in case of invalid CLA byte value. */
  private static final String INVALID_CLA = "invalid CLA byte: 'ff'"; // */

  /** Message in case Le-field is absent. */
  private static final String LE_ABSENT = "Le-field absent, but SHALL be present"; // */

  /** Message prefix in case of invalid Le. */
  private static final String LE_INVALID = "invalid Ne: "; // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /**
   * {@link Set} with valid Ne-values.
   *
   * <p>Includes
   *
   * <ul>
   *   <li>{@link CommandApdu#NE_INFIMUM},
   *   <li>2,
   *   <li>256,
   *   <li>{@link CommandApdu#NE_SUPREMUM} - 1,
   *   <li>{@link CommandApdu#NE_SUPREMUM},
   *   <li>{@link CommandApdu#NE_SHORT_WILDCARD},
   *   <li>{@link CommandApdu#NE_EXTENDED_WILDCARD}
   * </ul>
   */
  private static final Set<Integer> NE_VALUES = getNeValues(); // */

  /**
   * Creates a {@link Set} of valid Ne-values.
   *
   * @return {@link Set} with valid Ne-values
   */
  private static Set<Integer> getNeValues() {
    final Set<Integer> result = new HashSet<>(20);

    RNG.intsClosed(NE_INFIMUM, NE_SUPREMUM, 10).forEach(result::add);

    result.add(NE_SUPREMUM_SHORT - 1);
    result.add(NE_SUPREMUM_SHORT);
    result.add(NE_SUPREMUM_SHORT + 1);
    result.add(NE_SHORT_WILDCARD);
    result.add(NE_EXTENDED_WILDCARD);

    return result;
  } // end method */

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

  /** Test method for {@link CommandApdu#CommandApdu(byte[])}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_CommandApdu__byteA() {
    // Assertions:
    // ... a. underlying CommandApdu(Bytebuffer)-constructor works as expected
    // ... b. underlying check(...) method works as expected
    // ... c. getters work as expected

    // Note: Because of the assertions, the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy.
    // --- a. smoke test
    // --- b. check with length <= 3 => invalid
    // --- c. check with length == 4 => ISO-case 1
    // --- d. other cases
    // --- e. ERROR: invalid APDU

    // --- a. smoke test
    {
      var input =
          new byte[] {
            12, 42, -98, -102, 64, -121, 49, 1, -79, -14, 44, 2, 80, -101, 69, 19, 124, 82, -35,
            110, 92, -107, 32, 56, 127, 55, -121, 31, -3, -4, 103, -26, 44, -82, -6, -105, -66, 14,
            -117, -67, -70, -20, 9, -1, -77, -68, 91, 91, 19, -65, -53, -119, 3, -9, 1, -40, -105,
            1, 0, -114, 8, 34, -24, 38, 5, -39, 100, 2, -40, 0
          };
      var dut = new CommandApdu(input);
      assertTrue(dut.isShort());

      input =
          new byte[] {
            12, 42, -98, -102, 0, 0, 64, -121, 49, 1, -79, -14, 44, 2, 80, -101, 69, 19, 124, 82,
            -35, 110, 92, -107, 32, 56, 127, 55, -121, 31, -3, -4, 103, -26, 44, -82, -6, -105, -66,
            14, -117, -67, -70, -20, 9, -1, -77, -68, 91, 91, 19, -65, -53, -119, 3, -9, 1, -40,
            -105, 1, 0, -114, 8, 34, -24, 38, 5, -39, 100, 2, -40, 0, 0
          };
      dut = new CommandApdu(input);
      assertFalse(dut.isShort());
    } // end --- a.

    // --- b. check with length <= 3 => invalid
    IntStream.rangeClosed(0, 3)
        .forEach(
            length -> {
              final byte[] input = RNG.nextBytes(length);
              assertThrows(IllegalArgumentException.class, () -> new CommandApdu(input));
            }); // end forEach(i -> ...)

    // --- c. check with length == 4 => ISO-case 1
    {
      final int length = 4;
      final byte[] input = RNG.nextBytes(length);
      RNG.intsClosed(0, 0xff, 5)
          .forEach(
              cla -> {
                input[0] = (byte) cla;

                if (0xff == cla) { // NOPMD literal in if statement
                  // ... CLA == 'ff'
                  //     => invalid CLA-byte
                  final Throwable throwable =
                      assertThrows(IllegalArgumentException.class, () -> new CommandApdu(input));
                  assertEquals(INVALID_CLA, throwable.getMessage());
                  assertNull(throwable.getCause());
                } else {
                  // ... CLA != 'ff'
                  //     => valid CLA-byte
                  final CommandApdu dut = new CommandApdu(input);
                  assertEquals(cla, dut.getCla());
                  assertEquals(input[1] & 0xff, dut.getIns());
                  assertEquals(input[2] & 0xff, dut.getP1());
                  assertEquals(input[3] & 0xff, dut.getP2());
                  assertEquals(0, dut.getNc()); // Nc
                  assertArrayEquals(EMPTY_OS, dut.getData()); // data
                  assertEquals(0, dut.getNe());
                  assertEquals(1, dut.getCase());
                } // end else
              }); // end forEach(cla -> ...)
    } // end --- c.

    // --- d. other cases
    Map.ofEntries(
            // Case 2S
            Map.entry("20 02 a571 05", List.of("", 5, 2)),
            // Case 2S, Wildcard
            Map.entry("21 02 a571 00", List.of("", NE_SHORT_WILDCARD, 2)),
            // Case 2E
            Map.entry("22 02 a571 00 1234", List.of("", 0x1234, 2)),
            // Case 2E, Wildcard
            Map.entry("23 02 a571 00 0000", List.of("", NE_EXTENDED_WILDCARD, 2)),
            // Case 3S
            Map.entry("30 04 a571 05 3002a5325b", List.of("3002a5325b", 0, 3)),
            // Case 3E, small Nc
            Map.entry("38 04 a571 00 0005 3802a5325b", List.of("3802a5325b", 0, 3)),
            // Case 4S
            Map.entry("4b 04 a571 05 4b02a5325b ff", List.of("4b02a5325b", 0xff, 4)),
            // Case 4S, Wildcard
            Map.entry("4c 04 a571 05 4c02a5325b 00", List.of("4c02a5325b", NE_SHORT_WILDCARD, 4)),
            // Case 4E, small Ne
            Map.entry("4d 04 a571 00 0005 4d02a5325b 0001", List.of("4d02a5325b", 1, 4)),
            // Case 4E, large Ne
            Map.entry("4e 04 a571 00 0005 4e02a5325b 5432", List.of("4e02a5325b", 0x5432, 4)),
            // Case 4E, Wildcard
            Map.entry(
                "4f 04 a571 00 0005 4f02a5325b 0000",
                List.of("4f02a5325b", NE_EXTENDED_WILDCARD, 4)))
        .forEach(
            (input, exp) -> {
              final byte[] cmd = Hex.toByteArray(input);
              final int cla = cmd[0] & 0xff;
              final int ins = cmd[1] & 0xff;
              final int p1 = cmd[2] & 0xff;
              final int p2 = cmd[3] & 0xff;
              final byte[] data = Hex.toByteArray((String) exp.get(0));
              final int ne = (Integer) exp.get(1);
              final int ic = (Integer) exp.get(2);
              final CommandApdu dut = new CommandApdu(cmd);

              assertEquals(cla, dut.getCla());
              assertEquals(ins, dut.getIns());
              assertEquals(p1, dut.getP1());
              assertEquals(p2, dut.getP2());
              assertArrayEquals(data, dut.getData());
              assertEquals(ne, dut.getNe());
              assertEquals(ic, dut.getCase());
            }); // end forEach((inputString, exp) -> ...)

    // --- e. ERROR: invalid APDU
    {
      final var list =
          List.of(
              "00 a4 0102 03 010203 0405", // extra octet at end of ISO-case 4S
              "00 a4 0102 00 0003 010203 0000 0a", // extra octet at end of ISO-case 4E
              "00 a4 0102 0056" // expanded format but only 6 octets
              );
      for (final var input : list) {
        final var apdu = Hex.toByteArray(input);

        assertThrows(IllegalArgumentException.class, () -> new CommandApdu(apdu));
      } // end For (input...)
    } // end --- e.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(String)}. */
  @Test
  void test_CommandApdu__String() {
    // Assertions:
    // ... a. underlying CommandApdu(byte[])-constructor works as expected
    // ... b. underlying check(...) method works as expected
    // ... c. getters work as expected

    // Note: Because of the assertions the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy.
    // --- a. smoke test
    {
      final var cla = RNG.nextIntClosed(0, 0xfe);
      final var ins = RNG.nextIntClosed(0, 0xff);
      final var p1 = RNG.nextIntClosed(0, 0xff);
      final var p2 = RNG.nextIntClosed(0, 0xff);
      final var ne = RNG.nextIntClosed(1, 255);
      final var octets = String.format("%02x%02x%02x%02x%02x", cla, ins, p1, p2, ne);

      final var dut = new CommandApdu(octets);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(p1, dut.getP1());
      assertEquals(p2, dut.getP2());
      assertEquals(0, dut.getNc());
      assertEquals(ne, dut.getNe());
    } // end --- a.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(byte[], int, int)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_CommandApdu__byteA_int_int() {
    // Assertions:
    // ... a. underlying parse(...) method works as expected
    // ... b. underlying CommandApdu(Object...)-constructor works as expected
    // ... c. underlying check(...) method works as expected
    // ... d. getters work as expected

    // Note: Because of the assertions, the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy.
    // --- a. loop over various lengths for prefix- and postfix-octet strings
    // --- b. check with length <= 3 => invalid
    // --- c. check with length == 4 => ISO-case 1
    // --- d. other cases
    // --- e. ERROR: invalid APDU
    // --- f. ERROR: invalid offset and/or invalid length

    // --- a. loop over various lengths for prefix- and postfix-octet strings
    RNG.intsClosed(0, 20, 5)
        .forEach(
            offset -> {
              final byte[] prefix = RNG.nextBytes(offset);

              RNG.intsClosed(0, 20, 5)
                  .forEach(
                      postfixLength -> {
                        final byte[] postfix = RNG.nextBytes(postfixLength);

                        // --- b. check with length <= 3 => invalid
                        IntStream.rangeClosed(0, 3)
                            .forEach(
                                length -> {
                                  final byte[] input = RNG.nextBytes(length);
                                  final byte[] apdu = AfiUtils.concatenate(prefix, input, postfix);
                                  assertThrows(
                                      IllegalArgumentException.class,
                                      () -> new CommandApdu(apdu, offset, length));
                                }); // end forEach(i -> ...)

                        // --- c. check with length == 4 => ISO-case 1
                        {
                          final int length = 4;
                          final byte[] input = RNG.nextBytes(length);
                          final byte[] apdu = AfiUtils.concatenate(prefix, input, postfix);
                          RNG.intsClosed(0, 0xff, 5)
                              .forEach(
                                  cla -> {
                                    apdu[offset] = (byte) cla;

                                    if (0xff == cla) { // NOPMD literal in if statement
                                      // ... CLA == 'ff'
                                      //     => invalid CLA-byte
                                      final Throwable throwable =
                                          assertThrows(
                                              IllegalArgumentException.class,
                                              () -> new CommandApdu(apdu, offset, length));
                                      assertEquals(INVALID_CLA, throwable.getMessage());
                                      assertNull(throwable.getCause());
                                    } else {
                                      // ... CLA != 'ff'
                                      //     => valid CLA-byte
                                      final CommandApdu dut = new CommandApdu(apdu, offset, length);
                                      assertEquals(cla, dut.getCla());
                                      assertEquals(input[1] & 0xff, dut.getIns());
                                      assertEquals(input[2] & 0xff, dut.getP1());
                                      assertEquals(input[3] & 0xff, dut.getP2());
                                      assertEquals(0, dut.getNc()); // Nc
                                      assertArrayEquals(EMPTY_OS, dut.getData()); // data
                                      assertEquals(0, dut.getNe());
                                      assertEquals(1, dut.getCase());
                                    } // end else
                                  }); // end forEach(cla -> ...)
                        } // end --- c.

                        // --- d. other cases
                        Map.ofEntries(
                                // Case 2S
                                Map.entry("20 02 f140 05", List.of("", 5, 2)),
                                // Case 2S, Wildcard
                                Map.entry("21 02 f140 00", List.of("", NE_SHORT_WILDCARD, 2)),
                                // Case 2E
                                Map.entry("22 02 f140 00 1234", List.of("", 0x1234, 2)),
                                // Case 2E, Wildcard
                                Map.entry(
                                    "23 02 f140 00 0000", List.of("", NE_EXTENDED_WILDCARD, 2)),
                                // Case 3S
                                Map.entry("30 04 f140 05 3002f14005", List.of("3002f14005", 0, 3)),
                                // Case 3E, small Nc
                                Map.entry(
                                    "38 04 f140 00 0005 3802f14005", List.of("3802f14005", 0, 3)),
                                // Case 4S
                                Map.entry(
                                    "4b 04 f140 05 4b02f14005 ff", List.of("4b02f14005", 0xff, 4)),
                                // Case 4S, Wildcard
                                Map.entry(
                                    "4c 04 f140 05 4c02f14005 00",
                                    List.of("4c02f14005", NE_SHORT_WILDCARD, 4)),
                                // Case 4E, small Ne
                                Map.entry(
                                    "4d 04 f140 00 0005 4d02f14005 0001",
                                    List.of("4d02f14005", 1, 4)),
                                // Case 4E, large Ne
                                Map.entry(
                                    "4e 04 f140 00 0005 4e02f14005 5432",
                                    List.of("4e02f14005", 0x5432, 4)),
                                // Case 4E, Wildcard
                                Map.entry(
                                    "4f 04 f140 00 0005 4f02f14005 0000",
                                    List.of("4f02f14005", NE_EXTENDED_WILDCARD, 4)))
                            .forEach(
                                (input, exp) -> {
                                  final byte[] cmd = Hex.toByteArray(input);
                                  final int cla = cmd[0] & 0xff;
                                  final int ins = cmd[1] & 0xff;
                                  final int p1 = cmd[2] & 0xff;
                                  final int p2 = cmd[3] & 0xff;
                                  final byte[] data = Hex.toByteArray((String) exp.get(0));
                                  final int ne = (Integer) exp.get(1);
                                  final int ic = (Integer) exp.get(2);
                                  final byte[] apdu = AfiUtils.concatenate(prefix, cmd, postfix);
                                  final CommandApdu dut = new CommandApdu(apdu, offset, cmd.length);

                                  assertEquals(cla, dut.getCla());
                                  assertEquals(ins, dut.getIns());
                                  assertEquals(p1, dut.getP1());
                                  assertEquals(p2, dut.getP2());
                                  assertArrayEquals(data, dut.getData());
                                  assertEquals(ne, dut.getNe());
                                  assertEquals(ic, dut.getCase());
                                }); // end forEach((inputString, exp) -> ...)

                        // --- e. ERROR: invalid APDU
                        List.of(
                                // extra octet at end of ISO-case 4S
                                "00 a4 0102 03 010203 0405",
                                // extra octet at end of ISO-case 4E
                                "00 a4 0102 00 0003 010203 0000 0a",
                                // expanded format but only 6 octets
                                "00 a4 0102 0056")
                            .forEach(
                                input -> {
                                  final var cmd = Hex.toByteArray(input);
                                  final byte[] apdu = AfiUtils.concatenate(prefix, cmd, postfix);

                                  assertThrows(
                                      IllegalArgumentException.class,
                                      () -> new CommandApdu(apdu, offset, cmd.length));
                                }); // end forEach(input -> ...)
                      }); // end forEach(postfixLength -> ...)
            }); // end forEach(offset -> ...)

    // --- f. ERROR: invalid offset and/or invalid length
    {
      // Note: Keys in the following map are lists with the following elements:
      //       - index = 0: apdu.length
      //       - index = 1: offset
      //       - index = 2: length
      final var list =
          List.of(
              List.of(8, -1, 2), // offset < 0
              List.of(7, 1, -3), // length < 0
              List.of(5, 2, 4) // offset + length > apduLength
              );
      for (final var input : list) {
        final int apduLength = input.get(0);
        final int offset = input.get(1);
        final int length = input.get(2);
        final var apdu = RNG.nextBytes(apduLength);

        assertThrows(IllegalArgumentException.class, () -> new CommandApdu(apdu, offset, length));
      } // end For (input...)
    } // end --- f.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(ByteBuffer)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_CommandApdu__ByteBuffer() {
    // Assertions:
    // ... a. underlying CommandApdu(byte[])-constructor works as expected

    // Test strategy.
    // --- a. loop over various lengths for prefix- and postfix-octet strings
    // --- b. check with length <= 3 => invalid
    // --- c. check with length == 4 => ISO-case 1
    // --- d. other cases
    // --- e. ERROR: invalid APDU

    // --- a. loop over various lengths for prefix- and postfix-octet strings
    RNG.intsClosed(0, 20, 5)
        .forEach(
            offset -> {
              final byte[] prefix = RNG.nextBytes(offset);

              RNG.intsClosed(0, 20, 5)
                  .forEach(
                      postfixLength -> {
                        final byte[] postfix = RNG.nextBytes(postfixLength);

                        // --- b. check with length <= 3 => invalid
                        IntStream.rangeClosed(0, 3)
                            .forEach(
                                length -> {
                                  final byte[] input = RNG.nextBytes(length);
                                  final byte[] apdu = AfiUtils.concatenate(prefix, input, postfix);
                                  final ByteBuffer buffer = ByteBuffer.wrap(apdu);
                                  buffer.limit(offset + length);
                                  buffer.position(offset);
                                  assertThrows(
                                      IllegalArgumentException.class,
                                      () -> new CommandApdu(buffer));
                                }); // end forEach(i -> ...)

                        // --- c. check with length == 4 => ISO-case 1
                        {
                          final int length = 4;
                          final byte[] input = RNG.nextBytes(length);
                          final byte[] apdu = AfiUtils.concatenate(prefix, input, postfix);
                          final ByteBuffer buffer = ByteBuffer.wrap(apdu);
                          buffer.limit(offset + length);
                          buffer.position(offset);

                          RNG.intsClosed(0, 0xff, 5)
                              .forEach(
                                  cla -> {
                                    apdu[offset] = (byte) cla;
                                    buffer.limit(offset + length);
                                    buffer.position(offset);

                                    if (0xff == cla) { // NOPMD literal in if statement
                                      // ... CLA == 'ff'
                                      //     => invalid CLA-byte
                                      final Throwable throwable =
                                          assertThrows(
                                              IllegalArgumentException.class,
                                              () -> new CommandApdu(buffer));
                                      assertEquals(INVALID_CLA, throwable.getMessage());
                                      assertNull(throwable.getCause());
                                    } else {
                                      // ... CLA != 'ff'
                                      //     => valid CLA-byte
                                      final CommandApdu dut = new CommandApdu(buffer);
                                      assertEquals(cla, dut.getCla());
                                      assertEquals(input[1] & 0xff, dut.getIns());
                                      assertEquals(input[2] & 0xff, dut.getP1());
                                      assertEquals(input[3] & 0xff, dut.getP2());
                                      assertEquals(0, dut.getNc()); // Nc
                                      assertArrayEquals(EMPTY_OS, dut.getData()); // data
                                      assertEquals(0, dut.getNe());
                                      assertEquals(1, dut.getCase());
                                    } // end else
                                  }); // end forEach(cla -> ...)
                        } // end --- c.

                        // --- d. other cases
                        Map.ofEntries(
                                // Case 2S
                                Map.entry("20 02 7342 05", List.of("", 5, 2)),
                                // Case 2S, Wildcard
                                Map.entry("21 02 7342 00", List.of("", NE_SHORT_WILDCARD, 2)),
                                // Case 2E
                                Map.entry("22 02 7342 00 1234", List.of("", 0x1234, 2)),
                                // Case 2E, Wildcard
                                Map.entry(
                                    "23 02 7342 00 0000", List.of("", NE_EXTENDED_WILDCARD, 2)),
                                // Case 3S
                                Map.entry("30 04 7342 05 30028ab075", List.of("30028ab075", 0, 3)),
                                // Case 3E, small Nc
                                Map.entry(
                                    "38 04 7342 00 0005 38028ab075", List.of("38028ab075", 0, 3)),
                                // Case 4S
                                Map.entry(
                                    "4b 04 7342 05 4b028ab075 ff", List.of("4b028ab075", 0xff, 4)),
                                // Case 4S, Wildcard
                                Map.entry(
                                    "4c 04 7342 05 4c028ab075 00",
                                    List.of("4c028ab075", NE_SHORT_WILDCARD, 4)),
                                // Case 4E, small Ne
                                Map.entry(
                                    "4d 04 7342 00 0005 4d028ab075 0001",
                                    List.of("4d028ab075", 1, 4)),
                                // Case 4E, large Ne
                                Map.entry(
                                    "4e 04 7342 00 0005 4e028ab075 5432",
                                    List.of("4e028ab075", 0x5432, 4)),
                                // Case 4E, Wildcard
                                Map.entry(
                                    "4f 04 7342 00 0005 4f028ab075 0000",
                                    List.of("4f028ab075", NE_EXTENDED_WILDCARD, 4)))
                            .forEach(
                                (input, exp) -> {
                                  final byte[] cmd = Hex.toByteArray(input);
                                  final int cla = cmd[0] & 0xff;
                                  final int ins = cmd[1] & 0xff;
                                  final int p1 = cmd[2] & 0xff;
                                  final int p2 = cmd[3] & 0xff;
                                  final byte[] data = Hex.toByteArray((String) exp.get(0));
                                  final int ne = (Integer) exp.get(1);
                                  final int ic = (Integer) exp.get(2);
                                  final byte[] apdu = AfiUtils.concatenate(prefix, cmd, postfix);
                                  final ByteBuffer buffer = ByteBuffer.wrap(apdu);
                                  buffer.limit(offset + cmd.length);
                                  buffer.position(offset);
                                  final CommandApdu dut = new CommandApdu(buffer);

                                  assertEquals(cla, dut.getCla());
                                  assertEquals(ins, dut.getIns());
                                  assertEquals(p1, dut.getP1());
                                  assertEquals(p2, dut.getP2());
                                  assertArrayEquals(data, dut.getData());
                                  assertEquals(ne, dut.getNe());
                                  assertEquals(ic, dut.getCase());
                                }); // end forEach((inputString, exp) -> ...)
                      }); // end forEach(postfixLength -> ...)
            }); // end forEach(offset -> ...)

    // --- e. ERROR: invalid APDU
    {
      final var list =
          List.of(
              // extra octet at end of ISO-case 4S
              "00 a4 0102 03 010203 0405",
              // extra octet at end of ISO-case 4E
              "00 a4 0102 00 0003 010203 0000 0a",
              // expanded format but only 6 octets
              "00 a4 0102 0056");
      for (final var input : list) {
        final var apdu = ByteBuffer.wrap(Hex.toByteArray(input));

        assertThrows(IllegalArgumentException.class, () -> new CommandApdu(apdu));
      } // end For (input...)
    } // end --- a.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(int, int, int, int)}. */
  @Test
  void test_CommandApdu__int_int_int_int() {
    // Assertions:
    // ... a. underlying constructor new CommandApdu(Object ...) works as expected
    // ... b. underlying check(...)-method works as expected
    // ... c. getters work as expected

    // Note: Because of the assertions the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy:
    // --- a. create a case 1 command APDU
    // --- b. check if CLA='ff' is detected

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int p1 = RNG.nextIntClosed(0, 0xff);
    final int p2 = RNG.nextIntClosed(0, 0xff);

    // --- a. create a case 1 command APDU
    final CommandApdu dut = new CommandApdu(cla, ins, p1, p2);
    assertEquals(cla, dut.getCla());
    assertEquals(ins, dut.getIns());
    assertEquals(p1, dut.getP1());
    assertEquals(p2, dut.getP2());
    assertEquals(0, dut.getNc());
    assertEquals("", Hex.toHexDigits(dut.getData()));
    assertEquals(0, dut.getNe());
    assertEquals(1, dut.getCase());

    // --- b. check if CLA='ff' is detected
    {
      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(0xff, ins, p1, p2));
      assertEquals(INVALID_CLA, throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- b.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(int, int, int, int, int)}. */
  @Test
  void test_CommandApdu__int_int_int_int_int() {
    // Assertions:
    // ... a. underlying constructor new CommandApdu(Object ...) works as expected
    // ... b. underlying check(...)-method works as expected
    // ... c. getters work as expected

    // Note: Because of the assertions, the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy:
    // --- a. create a case 2 command APDU
    // --- b. check if CLA='ff' is detected
    // --- c. check that invalid Ne-values are detected
    // --- d. ERROR: Le-field absent

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int p1 = RNG.nextIntClosed(0, 0xff);
    final int p2 = RNG.nextIntClosed(0, 0xff);

    // --- a. create a case 2 command APDU
    NE_VALUES.forEach(
        ne -> {
          final CommandApdu dut = new CommandApdu(cla, ins, p1, p2, ne);
          assertEquals(cla, dut.getCla());
          assertEquals(ins, dut.getIns());
          assertEquals(p1, dut.getP1());
          assertEquals(p2, dut.getP2());
          assertEquals(0, dut.getNc());
          assertEquals("", Hex.toHexDigits(dut.getData()));
          assertEquals(ne, dut.getNe());
          assertEquals(2, dut.getCase());
        }); // end forEach(ne -> ...)

    // --- b. check if CLA='ff' is detected
    {
      final Throwable throwable =
          assertThrows(
              IllegalArgumentException.class, () -> new CommandApdu(0xff, ins, p1, p2, NE_INFIMUM));
      assertEquals(INVALID_CLA, throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- b.

    // --- c. check that invalid Ne-values are detected
    List.of(
            NE_INFIMUM - 2,
            NE_INFIMUM - 1,
            NE_SUPREMUM + 2, // Note: NE_SUPREMUM=0xffff => NE_SUPREMUM+1=NeExtendedWildcard
            NE_SHORT_WILDCARD - 1,
            NE_SHORT_WILDCARD + 1,
            // Note: NE_EXTENDED_WILDCARD - 1 is not tested, because NE_EXTENDED_WILDCARD ==
            // NE_SUPREMUM
            NE_EXTENDED_WILDCARD + 1)
        .forEach(
            ne -> {
              final Throwable throwable =
                  assertThrows(
                      IllegalArgumentException.class, () -> new CommandApdu(cla, ins, p1, p2, ne));
              assertEquals((0 == ne) ? LE_ABSENT : LE_INVALID + ne, throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(ne -> ...)

    // --- d. ERROR: Le-field absent
    {
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, 0));

      assertEquals(LE_ABSENT, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- d.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(int, int, int, int, byte[])}. */
  @Test
  void test_CommandApdu__int_int_int_int_byteA() {
    // Assertions:
    // ... a. underlying constructor new CommandApdu(...) works as expected
    // ... b. getters work as expected

    // Note: Because of the assertions, the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. check for defensive cloning
    // --- c. ERROR: command data field absent
    // --- d. ERROR: command data field too long

    // --- a. smoke test
    {
      final var cla = RNG.nextIntClosed(0, 0xfe);
      final var ins = RNG.nextIntClosed(0, 0xff);
      final var p1 = RNG.nextIntClosed(0, 0xff);
      final var p2 = RNG.nextIntClosed(0, 0xff);
      final var data = RNG.nextBytes(1, 8);

      final var dut = new CommandApdu(cla, ins, p1, p2, data);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(p1, dut.getP1());
      assertEquals(p2, dut.getP2());
      assertEquals(data.length, dut.getNc());
      assertArrayEquals(data, dut.getData());
      assertEquals(3, dut.getCase());
      assertEquals(0, dut.getNe());
    } // end --- a.

    // --- b. check for defensive cloning
    {
      final var dataIn = RNG.nextBytes(1, 8);

      final var dut = new CommandApdu(1, 2, 3, 4, dataIn);

      final var data1 = dut.getData();
      assertNotSame(dataIn, data1);
      assertArrayEquals(dataIn, data1);
      dataIn[0]++;
      assertFalse(Arrays.equals(dataIn, data1));
      final var data2 = dut.getData();
      assertArrayEquals(data1, data2);
    } // end --- b.

    // --- c. ERROR: command data field absent
    {
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, EMPTY_OS));

      assertEquals(CMD_DATA_ABSENT, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- c.

    // --- d. ERROR: command data field too long
    {
      final var data = RNG.nextBytes(NC_SUPREMUM + 1);
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, data));

      assertEquals(CMD_DATA_TOO_LONG, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- d.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(int, int, int, int, String)}. */
  @Test
  void test_CommandApdu__int_int_int_int_String() {
    // Assertions:
    // ... a. underlying constructor new CommandApdu(...) works as expected
    // ... b. getters work as expected

    // Note: Because of the assertions, the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: command data field absent
    // --- c. ERROR: command data field too long

    // --- a. smoke test
    {
      final var cla = RNG.nextIntClosed(0, 0xfe);
      final var ins = RNG.nextIntClosed(0, 0xff);
      final var p1 = RNG.nextIntClosed(0, 0xff);
      final var p2 = RNG.nextIntClosed(0, 0xff);
      final var data = RNG.nextBytes(1, 2);
      final var octets = Hex.toHexDigits(data);

      final var dut = new CommandApdu(cla, ins, p1, p2, octets);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(p1, dut.getP1());
      assertEquals(p2, dut.getP2());
      assertEquals(data.length, dut.getNc());
      assertArrayEquals(data, dut.getData());
      assertEquals(3, dut.getCase());
      assertEquals(0, dut.getNe());
    } // end --- a.

    // --- b. ERROR: command data field absent
    {
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, ""));

      assertEquals(CMD_DATA_ABSENT, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- b.

    // --- c. ERROR: command data field too long
    {
      final var data = Hex.toHexDigits(RNG.nextBytes(NC_SUPREMUM + 1));
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, data));

      assertEquals(CMD_DATA_TOO_LONG, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- c.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(int, int, int, int, byte[], int, int)}. */
  @Test
  void test_CommandApdu__int_int_int_int_byteA_int_int() {
    // Assertions:
    // ... a. underlying constructor new CommandApdu(...) works as expected
    // ... b. underlying check(...)-method works as expected
    // ... c. getters work as expected

    // Note: Because of the assertions, the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage
    //       and Arrays.copyOfRange(...) functionality.

    // Test strategy:
    // --- a. smoke test
    // --- b. various prefix and postfix octets
    // --- c. check for defensive cloning of command data field upon construction
    // --- d. ERROR: command data field too long
    // --- e. ERROR: command data field absent

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int p1 = RNG.nextIntClosed(0, 0xff);
    final int p2 = RNG.nextIntClosed(0, 0xff);

    // --- a. smoke test
    {
      final var lenPrefix = RNG.nextIntClosed(1, 8);
      final var prefix = RNG.nextBytes(lenPrefix);
      final var lenPostfix = RNG.nextIntClosed(1, 8);
      final var postfix = RNG.nextBytes(lenPostfix);
      final var lenData = RNG.nextIntClosed(1, 8);
      final var data = RNG.nextBytes(lenData);

      final var dut =
          new CommandApdu(
              cla, ins, p1, p2, AfiUtils.concatenate(prefix, data, postfix), lenPrefix, lenData);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(p1, dut.getP1());
      assertEquals(p2, dut.getP2());
      assertEquals(3, dut.getCase());
      assertEquals(lenData, dut.getNc());
      assertArrayEquals(data, dut.getData());
      assertEquals(0, dut.getNe());
    } // end --- a.

    // --- b. various prefix and postfix octets
    RNG.intsClosed(0, 10, 5)
        .forEach(
            lenPrefix -> {
              final var prefix = RNG.nextBytes(lenPrefix);

              RNG.intsClosed(0, 10, 5)
                  .forEach(
                      lenPostfix -> {
                        final var postfix = RNG.nextBytes(lenPostfix);
                        final var lenData = RNG.nextIntClosed(1, 8);
                        final var data = RNG.nextBytes(lenData);

                        final var dut =
                            new CommandApdu(
                                cla,
                                ins,
                                p1,
                                p2,
                                AfiUtils.concatenate(prefix, data, postfix),
                                lenPrefix,
                                lenData);

                        assertEquals(3, dut.getCase());
                        assertEquals(lenData, dut.getNc());
                        assertEquals(0, dut.getNe());

                        // --- c. check for defensive cloning of command data field upon
                        // construction
                        final var data1 = dut.getData();
                        assertArrayEquals(data, data1);
                        data[0]++;
                        assertFalse(Arrays.equals(data, data1));
                        final var data2 = dut.getData();
                        assertNotSame(data1, data2);
                        assertArrayEquals(data1, data2);

                        // --- d. ERROR: command data field too long
                        {
                          final var dataD = RNG.nextBytes(NC_SUPREMUM + 1);
                          final var tmp = AfiUtils.concatenate(prefix, dataD, postfix);
                          final var thrown =
                              assertThrows(
                                  IllegalArgumentException.class,
                                  () -> new CommandApdu(1, 2, 3, 4, tmp, lenPrefix, dataD.length));

                          assertEquals(CMD_DATA_TOO_LONG, thrown.getMessage());
                          assertNull(thrown.getCause());
                        } // end --- d.
                      }); // end forEach(lenPostfix -> ...)
            }); // end forEach(lenPrefix -> ...)

    // --- e. ERROR: command data field absent
    {
      final var thrown =
          assertThrows(
              IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, EMPTY_OS, 0, 0));

      assertEquals(CMD_DATA_ABSENT, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- e.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(int, int, int, int, byte[], int)}. */
  @Test
  void test_CommandApdu__int_int_int_int_byteA_int() {
    // Assertions:
    // ... a. underlying constructor new CommandApdu(...) works as expected
    // ... b. getters work as expected

    // Note: Because of the assertions, the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. check for defensive cloning
    // --- c. ERROR: command data field absent
    // --- d. ERROR: command data field too long
    // --- e. ERROR: Le-field absent

    // --- a. smoke test
    {
      final var cla = RNG.nextIntClosed(0, 0xfe);
      final var ins = RNG.nextIntClosed(0, 0xff);
      final var p1 = RNG.nextIntClosed(0, 0xff);
      final var p2 = RNG.nextIntClosed(0, 0xff);
      final var data = RNG.nextBytes(1, 8);
      final var ne = RNG.nextIntClosed(1, 255);

      final var dut = new CommandApdu(cla, ins, p1, p2, data, ne);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(p1, dut.getP1());
      assertEquals(p2, dut.getP2());
      assertEquals(4, dut.getCase());
      assertEquals(data.length, dut.getNc());
      assertArrayEquals(data, dut.getData());
      assertEquals(ne, dut.getNe());
    } // end --- a.

    // --- b. check for defensive cloning
    {
      final var dataIn = RNG.nextBytes(1, 8);

      final var dut = new CommandApdu(1, 2, 3, 4, dataIn, 5);

      final var data1 = dut.getData();
      assertNotSame(dataIn, data1);
      assertArrayEquals(dataIn, data1);
      dataIn[0]++;
      assertFalse(Arrays.equals(dataIn, data1));
      final var data2 = dut.getData();
      assertArrayEquals(data1, data2);
    } // end --- b.

    // --- c. ERROR: command data field absent
    {
      final var thrown =
          assertThrows(
              IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, EMPTY_OS, 2));

      assertEquals(CMD_DATA_ABSENT, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- c.

    // --- d. ERROR: command data field too long
    {
      final var data = RNG.nextBytes(NC_SUPREMUM + 1);
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, data, 5));

      assertEquals(CMD_DATA_TOO_LONG, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- d.

    // --- e. ERROR: Le-field absent
    {
      final var data = RNG.nextBytes(1, 8);
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, data, 0));

      assertEquals(LE_ABSENT, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- e.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(int, int, int, int, String, int)}. */
  @Test
  void test_CommandApdu__int_int_int_int_String_int() {
    // Assertions:
    // ... a. underlying constructor new CommandApdu(...) works as expected
    // ... b. getters work as expected

    // Note: Because of the assertions, the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: command data field absent
    // --- c. ERROR: Le-field absent

    // --- a. smoke test
    {
      final var cla = RNG.nextIntClosed(0, 0xfe);
      final var ins = RNG.nextIntClosed(0, 0xff);
      final var p1 = RNG.nextIntClosed(0, 0xff);
      final var p2 = RNG.nextIntClosed(0, 0xff);
      final var data = RNG.nextBytes(1, 2);
      final var ne = RNG.nextIntClosed(1, 255);
      final var octets = Hex.toHexDigits(data);

      final var dut = new CommandApdu(cla, ins, p1, p2, octets, ne);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(p1, dut.getP1());
      assertEquals(p2, dut.getP2());
      assertEquals(4, dut.getCase());
      assertEquals(data.length, dut.getNc());
      assertArrayEquals(data, dut.getData());
      assertEquals(ne, dut.getNe());
    } // end --- a.

    // --- b. ERROR: command data field absent
    {
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, "", 2));

      assertEquals(CMD_DATA_ABSENT, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- c.

    // --- c. ERROR: Le-field absent
    {
      final var data = Hex.toHexDigits(RNG.nextBytes(1, 8));
      final var thrown =
          assertThrows(IllegalArgumentException.class, () -> new CommandApdu(1, 2, 3, 4, data, 0));

      assertEquals(LE_ABSENT, thrown.getMessage());
      assertNull(thrown.getCause());
    } // end --- c.
  } // end method */

  /** Test method for {@link CommandApdu#CommandApdu(int, int, int, int, byte[], int, int, int)}. */
  @Test
  void test_CommandApdu__int_int_int_int_byteA_int_int_int() {
    // Assertions:
    // ... a. underlying constructor new CommandApdu(...) works as expected
    // ... b. underlying check(...)-method works as expected
    // ... c. getters work as expected

    // Note: Because of the assertions, the constructor-under-test is rather
    //       simple. Thus, we can be lazy here and concentrate on code-coverage
    //       and Arrays.copyOfRange(...) functionality.

    // Test strategy:
    // --- a. smoke test
    // --- b. various prefix and postfix octets
    // --- c. check for defensive cloning of command data field upon construction
    // --- d. ERROR: command data field absent
    // --- e. ERROR: Le-field absent

    final int cla = RNG.nextIntClosed(0, 0xfe);
    final int ins = RNG.nextIntClosed(0, 0xff);
    final int p1 = RNG.nextIntClosed(0, 0xff);
    final int p2 = RNG.nextIntClosed(0, 0xff);
    final int ne = RNG.nextIntClosed(1, 255);

    // --- a. smoke test
    {
      final var lenPrefix = RNG.nextIntClosed(1, 8);
      final var prefix = RNG.nextBytes(lenPrefix);
      final var lenPostfix = RNG.nextIntClosed(1, 8);
      final var postfix = RNG.nextBytes(lenPostfix);
      final var lenData = RNG.nextIntClosed(1, 8);
      final var data = RNG.nextBytes(lenData);

      final var dut =
          new CommandApdu(
              cla,
              ins,
              p1,
              p2,
              AfiUtils.concatenate(prefix, data, postfix),
              lenPrefix,
              lenData,
              ne);

      assertEquals(cla, dut.getCla());
      assertEquals(ins, dut.getIns());
      assertEquals(p1, dut.getP1());
      assertEquals(p2, dut.getP2());
      assertEquals(4, dut.getCase());
      assertEquals(lenData, dut.getNc());
      assertArrayEquals(data, dut.getData());
      assertEquals(ne, dut.getNe());
    } // end --- a.

    // --- b. various prefix and postfix octets
    RNG.intsClosed(0, 10, 5)
        .forEach(
            lenPrefix -> {
              final var prefix = RNG.nextBytes(lenPrefix);

              RNG.intsClosed(0, 10, 5)
                  .forEach(
                      lenPostfix -> {
                        final var postfix = RNG.nextBytes(lenPostfix);
                        final var lenData = RNG.nextIntClosed(1, 8);
                        final var data = RNG.nextBytes(lenData);

                        final var dut =
                            new CommandApdu(
                                cla,
                                ins,
                                p1,
                                p2,
                                AfiUtils.concatenate(prefix, data, postfix),
                                lenPrefix,
                                lenData,
                                ne);

                        assertEquals(4, dut.getCase());
                        assertEquals(lenData, dut.getNc());
                        assertEquals(ne, dut.getNe());

                        // --- c. check for defensive cloning of command data field upon
                        // construction
                        final var data1 = dut.getData();
                        assertArrayEquals(data, data1);
                        data[0]++;
                        assertFalse(Arrays.equals(data, data1));
                        final var data2 = dut.getData();
                        assertNotSame(data1, data2);
                        assertArrayEquals(data1, data2);

                        // --- d. ERROR: command data field absent
                        {
                          final var dataD = AfiUtils.concatenate(prefix, postfix);
                          final var thrown =
                              assertThrows(
                                  IllegalArgumentException.class,
                                  () -> new CommandApdu(cla, ins, p1, p2, dataD, lenPrefix, 0, ne));

                          assertEquals(CMD_DATA_ABSENT, thrown.getMessage());
                          assertNull(thrown.getCause());
                        } // end --- d.

                        // --- e. ERROR: Le-field absent
                        {
                          final var dataE = AfiUtils.concatenate(prefix, data, postfix);
                          final var thrown =
                              assertThrows(
                                  IllegalArgumentException.class,
                                  () ->
                                      new CommandApdu(
                                          cla, ins, p1, p2, dataE, lenPrefix, lenData, 0));

                          assertEquals(LE_ABSENT, thrown.getMessage());
                          assertNull(thrown.getCause());
                        } // end --- e.
                      }); // end forEach(lenPostfix -> ...)
            }); // end forEach(lenPrefix -> ...)
  } // end method */

  /** Test method for {@link CommandApdu#check(int)}. */
  @Test
  void test_check__int() {
    // Test strategy:
    // --- a. smoke test, valid, all ISO-cases
    // --- b. ERROR: invalid input parameter isoCase
    // --- c. valid ISO-Case 1
    // --- d. valid ISO-Case 2
    // --- e. valid ISO-Case 3
    // --- f. valid ISO-Case 4

    // --- a. smoke test
    for (final var entry :
        Map.ofEntries(
                // ISO-case 1
                Map.entry(new CommandApdu(EMPTY_OS, 1, 2, 3, 4, 0), 1),
                // ISO-case 2S
                Map.entry(new CommandApdu(EMPTY_OS, 1, 2, 3, 4, 1), 2),
                // ISO-case 2E
                Map.entry(new CommandApdu(EMPTY_OS, 1, 2, 3, 4, 256), 2),
                // ISO-case 3S
                Map.entry(new CommandApdu(RNG.nextBytes(2), 1, 2, 3, 4, 0), 3),
                // ISO-case 3E
                Map.entry(new CommandApdu(RNG.nextBytes(256), 1, 2, 3, 4, 0), 3),
                // ISO-case 4S
                Map.entry(new CommandApdu(RNG.nextBytes(2), 1, 2, 3, 4, 1), 4),
                // ISO-case 4E
                Map.entry(new CommandApdu(RNG.nextBytes(2), 1, 2, 3, 4, 256), 4))
            .entrySet()) {
      final var dut = entry.getKey();
      final var isoCase = entry.getValue();

      assertDoesNotThrow(() -> dut.check(isoCase));

      // --- b. ERROR: invalid input parameter isoCase
      List.of(
              0, // just below valid value
              5 // just above valid value
              )
          .forEach(
              invalidIsoCase -> {
                final Throwable throwable =
                    assertThrows(IllegalArgumentException.class, () -> dut.check(invalidIsoCase));

                assertEquals("unimplemented case: " + invalidIsoCase, throwable.getMessage());
                assertNull(throwable.getCause());
              }); // end forEach(invalidIsoCase -> ...)
    } // end For (entry...)

    zzzCheckCase1(); // --- c. valid ISO-Case 1
    zzzCheckCase2(); // --- d. valid ISO-Case 2
    zzzCheckCase3(); // --- e. valid ISO-Case 3
    zzzCheckCase4(); // --- f. valid ISO-Case 4
  } // end method */

  private static void zzzCheckCase1() {
    final int isoCase = 1;

    // a.1 valid case 1 APDU
    // a.2 invalid CLA byte
    // a.3 command data field present
    // a.4 Le-field present

    // a.1 valid case 1 APDU
    IntStream.range(0, 0xff)
        .forEach(
            cla -> {
              final CommandApdu dut = new CommandApdu(EMPTY_OS, cla, 1, 2, 3, 0);

              dut.check(isoCase);

              assertEquals(isoCase, dut.getCase());
            }); // end forEach(cla -> ...)

    // a.2 invalid CLA byte
    {
      final var dut = new CommandApdu(EMPTY_OS, 0xff, 1, 2, 3, 0);

      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

      assertEquals(INVALID_CLA, throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // a.3 command data field present
    RNG.intsClosed(NC_INFIMUM, NC_SUPREMUM, 10)
        .forEach(
            nc -> {
              final var dut = new CommandApdu(RNG.nextBytes(nc), 0xfe, 1, 2, 3, 0);

              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

              assertEquals(3, dut.getCase());
              assertEquals(
                  "command data field present, but SHALL be absent", throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(nc -> ...)

    // a.4 Le-field present
    NE_VALUES.forEach(
        ne -> {
          final var dut = new CommandApdu(EMPTY_OS, 0, 1, 2, 3, ne);

          final Throwable throwable =
              assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

          assertEquals(2, dut.getCase());
          assertEquals("Le-field present, but SHALL be absent", throwable.getMessage());
          assertNull(throwable.getCause());
        }); // end forEach(ne -> ...)
  } // end method */

  private static void zzzCheckCase2() {
    final int isoCase = 2;

    // b.1 valid case 2 APDU
    // b.2 invalid CLA byte
    // b.3 command data field present
    // b.4 Le-field absent
    // b.5 invalid Le-value

    // b.1 valid case 2 APDU
    IntStream.range(0, 0xff)
        .forEach(
            cla -> {
              NE_VALUES.forEach(
                  ne -> {
                    final CommandApdu dut = new CommandApdu(EMPTY_OS, cla, 1, 2, 3, ne);

                    dut.check(isoCase);

                    assertEquals(isoCase, dut.getCase());
                  }); // end forEach(ne -> ...)
            }); // end forEach(cla -> ...)

    // b.2 invalid CLA byte
    {
      final var dut = new CommandApdu(EMPTY_OS, 0xff, 1, 2, 3, 0);
      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));
      assertEquals(INVALID_CLA, throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // b.3 command data field present
    RNG.intsClosed(NC_INFIMUM, NC_SUPREMUM, 10)
        .forEach(
            nc -> {
              final var dut = new CommandApdu(RNG.nextBytes(nc), 0, 1, 2, 3, 3);

              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

              assertEquals(4, dut.getCase());
              assertEquals(
                  "command data field present, but SHALL be absent", throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(nc -> ...)

    // b.4 Le-field absent
    {
      final var dut = new CommandApdu(EMPTY_OS, 1, 2, 3, 4, 0);

      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

      assertEquals(1, dut.getCase());
      assertEquals(LE_ABSENT, throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // b.5 invalid Le-value
    List.of(
            Integer.MIN_VALUE, // infimum
            // Note: NE_INFIMUM - 1 => Le-field absent rather than "invalid Le-value"
            NE_INFIMUM - 2, // just below valid value
            NE_SUPREMUM + 2, // Note: NE_SUPREMUM=0xffff => NE_SUPREMUM+1=NeExtendedWildcard
            Integer.MAX_VALUE // supremum
            )
        .forEach(
            ne -> {
              final var dut = new CommandApdu(EMPTY_OS, 0, 1, 2, 3, ne);

              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

              assertEquals(2, dut.getCase());
              assertEquals(LE_INVALID + ne, throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(ne -> ...)
  } // end method */

  private static void zzzCheckCase3() {
    final int isoCase = 3;

    // c.1 valid case 3 APDU
    // c.2 invalid CLA byte
    // c.3 command data field absent
    // c.4 Le-field present
    // c.5 invalid Lc-value

    // c.1 valid case 3 APDU
    RNG.intsClosed(NC_INFIMUM, NC_SUPREMUM, 10)
        .forEach(
            nc -> {
              IntStream.range(0, 0xff)
                  .forEach(
                      cla -> {
                        final CommandApdu dut = new CommandApdu(RNG.nextBytes(nc), cla, 1, 2, 3, 0);

                        dut.check(isoCase);

                        assertEquals(isoCase, dut.getCase());
                      }); // end forEach(cla -> ...)
            }); // end forEach(nc -> ...)

    // c.2 invalid CLA byte
    {
      final var dut = new CommandApdu(RNG.nextBytes(NC_INFIMUM, NC_SUPREMUM), 0xff, 1, 2, 3, 0);

      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

      assertEquals(3, dut.getCase());
      assertEquals(INVALID_CLA, throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // c.3 command data field absent
    {
      final var dut = new CommandApdu(EMPTY_OS, 0, 1, 2, 3, 0);

      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

      assertEquals(1, dut.getCase());
      assertEquals(CMD_DATA_ABSENT, throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // c.4 Le-field present
    NE_VALUES.forEach(
        ne -> {
          final var dut = new CommandApdu(RNG.nextBytes(NC_INFIMUM, NC_SUPREMUM), 0, 1, 2, 3, ne);

          final Throwable throwable =
              assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

          assertEquals(4, dut.getCase());
          assertEquals("Le-field present, but SHALL be absent", throwable.getMessage());
          assertNull(throwable.getCause());
        });

    // c.5 invalid Lc-value
    {
      final var dut = new CommandApdu(RNG.nextBytes(NC_SUPREMUM + 1), 0, 1, 2, 3, 0);

      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

      assertEquals(3, dut.getCase());
      assertEquals(CMD_DATA_TOO_LONG, throwable.getMessage());
      assertNull(throwable.getCause());
    }
  } // end method */

  private static void zzzCheckCase4() {
    final int isoCase = 4;

    // d.1 valid case 4 APDU
    // d.2 invalid CLA byte
    // d.3 command data field absent
    // d.4 Le-field absent
    // d.5 invalid Lc-value
    // d.6 invalid Le-value

    // d.1 valid case 4 APDU
    IntStream.range(0, 0xff)
        .forEach(
            cla -> {
              RNG.intsClosed(NC_INFIMUM, NC_SUPREMUM, 10)
                  .forEach(
                      nc -> {
                        final byte[] data = RNG.nextBytes(nc);

                        NE_VALUES.forEach(
                            ne -> {
                              final CommandApdu dut = new CommandApdu(data, 0, 1, 2, 3, ne);

                              dut.check(isoCase);

                              assertEquals(isoCase, dut.getCase());
                            }); // end forEach(ne -> ...)
                      }); // end forEach(nc -> ...)
            }); // end forEach(cla -> ...)

    // d.2 invalid CLA byte
    {
      final var dut = new CommandApdu(RNG.nextBytes(NC_INFIMUM, NC_SUPREMUM), 0xff, 1, 2, 3, 2);

      final Throwable throwable =
          assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

      assertEquals(INVALID_CLA, throwable.getMessage());
      assertNull(throwable.getCause());
    }

    // d.3 command data field absent
    NE_VALUES.forEach(
        ne -> {
          final var dut = new CommandApdu(EMPTY_OS, 0, 1, 2, 3, ne);

          final Throwable throwable =
              assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

          assertEquals(2, dut.getCase());
          assertEquals(CMD_DATA_ABSENT, throwable.getMessage());
          assertNull(throwable.getCause());
        });

    // d.4 Le-field absent
    RNG.intsClosed(NC_INFIMUM, NC_SUPREMUM, 10)
        .forEach(
            nc -> {
              final var dut = new CommandApdu(RNG.nextBytes(nc), 0, 1, 2, 3, 0);

              final Throwable throwable =
                  assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

              assertEquals(3, dut.getCase());
              assertEquals(LE_ABSENT, throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(nc -> ...)

    // d.5 invalid Lc-value
    {
      final byte[] data = RNG.nextBytes(NC_SUPREMUM + 1);
      NE_VALUES.forEach(
          ne -> {
            final var dut = new CommandApdu(data, 0, 1, 2, 3, ne);

            final Throwable throwable =
                assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

            assertEquals(4, dut.getCase());
            assertEquals(CMD_DATA_TOO_LONG, throwable.getMessage());
            assertNull(throwable.getCause());
          }); // end forEach(ne -> ...)
    }

    // d.6 invalid Le-value
    RNG.intsClosed(NC_INFIMUM, NC_SUPREMUM, 10)
        .forEach(
            nc -> {
              final byte[] data = RNG.nextBytes(nc);

              List.of(
                      Integer.MIN_VALUE, // infimum
                      // Note: NE_INFIMUM - 1 => Le-field absent rather than "invalid Le-value"
                      NE_INFIMUM - 2, // just below valid value
                      NE_SUPREMUM
                          + 2, // Note: NE_SUPREMUM=0xffff => NE_SUPREMUM+1=NeExtendedWildcard
                      Integer.MAX_VALUE // supremum
                      )
                  .forEach(
                      ne -> {
                        final var dut = new CommandApdu(data, 0, 1, 2, 3, ne);

                        final Throwable throwable =
                            assertThrows(IllegalArgumentException.class, () -> dut.check(isoCase));

                        assertEquals(4, dut.getCase());
                        assertEquals(LE_INVALID + ne, throwable.getMessage());
                        assertNull(throwable.getCause());
                      }); // end forEach(ne -> ...)
            }); // end forEach(nc -> ...)
  } // end method */

  /** Test method for {@link CommandApdu#equals(Object)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. difference in CLA
    // --- e. difference in INS
    // --- f. difference in P1
    // --- g. difference in P2
    // --- h. difference in command data field
    // --- i. difference in Ne
    // --- e. different object, but equal content

    for (final var ne : NE_VALUES) {
      final int cla = RNG.nextIntClosed(0, 0xfe);
      final int ins = RNG.nextIntClosed(0, 0xff);
      final int p1 = RNG.nextIntClosed(0, 0xff);
      final int p2 = RNG.nextIntClosed(0, 0xff);
      final byte[] data = RNG.nextBytes(NC_INFIMUM, 10);

      final CommandApdu dut1 = new CommandApdu(cla, ins, p1, p2);
      final CommandApdu dut2 = new CommandApdu(cla, ins, p1, p2, ne);
      final CommandApdu dut3 = new CommandApdu(cla, ins, p1, p2, data);
      final CommandApdu dut4 = new CommandApdu(cla, ins, p1, p2, data, ne);

      for (final var dut : List.of(dut1, dut2, dut3, dut4)) {
        for (final Object obj :
            new Object[] {
              dut, // --- a. same reference
              null, // --- b. null input
              "afi" // --- c. difference in type
            }) {
          assertEquals(obj == dut, dut.equals(obj)); // NOPMD use equals()
        } // end For (obj...)
      } // end For (dut...)

      // --- d. difference in CLA
      IntStream.rangeClosed(0, 0xfe)
          .forEach(
              i -> {
                assertEquals(cla == i, dut1.equals(new CommandApdu(i, ins, p1, p2)));
                assertEquals(cla == i, dut2.equals(new CommandApdu(i, ins, p1, p2, ne)));
                assertEquals(cla == i, dut3.equals(new CommandApdu(i, ins, p1, p2, data)));
                assertEquals(cla == i, dut4.equals(new CommandApdu(i, ins, p1, p2, data, ne)));
              }); // end forEach(i -> ...)

      // --- e. difference in INS
      IntStream.rangeClosed(0, 0xff)
          .forEach(
              i -> {
                assertEquals(ins == i, dut1.equals(new CommandApdu(cla, i, p1, p2)));
                assertEquals(ins == i, dut2.equals(new CommandApdu(cla, i, p1, p2, ne)));
                assertEquals(ins == i, dut3.equals(new CommandApdu(cla, i, p1, p2, data)));
                assertEquals(ins == i, dut4.equals(new CommandApdu(cla, i, p1, p2, data, ne)));
              }); // end forEach(i -> ...)

      // --- f. difference in P1
      IntStream.rangeClosed(0, 0xff)
          .forEach(
              i -> {
                assertEquals(p1 == i, dut1.equals(new CommandApdu(cla, ins, i, p2)));
                assertEquals(p1 == i, dut2.equals(new CommandApdu(cla, ins, i, p2, ne)));
                assertEquals(p1 == i, dut3.equals(new CommandApdu(cla, ins, i, p2, data)));
                assertEquals(p1 == i, dut4.equals(new CommandApdu(cla, ins, i, p2, data, ne)));
              }); // end forEach(i -> ...)

      // --- g. difference in P2
      IntStream.rangeClosed(0, 0xff)
          .forEach(
              i -> {
                assertEquals(p2 == i, dut1.equals(new CommandApdu(cla, ins, p1, i)));
                assertEquals(p2 == i, dut2.equals(new CommandApdu(cla, ins, p1, i, ne)));
                assertEquals(p2 == i, dut3.equals(new CommandApdu(cla, ins, p1, i, data)));
                assertEquals(p2 == i, dut4.equals(new CommandApdu(cla, ins, p1, i, data, ne)));
              }); // end forEach(i -> ...)

      // --- h. difference in command data field
      // h.1 difference in ISO-case
      // h.2 same Nc, but different content
      // h.3 data field subset
      // h.4 data field superset

      // h.1 difference in ISO-case
      boolean result;
      result = dut1.equals(dut3);
      assertFalse(result, "ISO-case 1 vs. 3");
      result = dut2.equals(dut4);
      assertFalse(result, "ISO-case 2 vs. 4");
      result = dut3.equals(dut1);
      assertFalse(result, "ISO-case 3 vs. 1");
      result = dut4.equals(dut2);
      assertFalse(result, "ISO-case 4 vs. 2");

      // h.2 same Nc, but different content
      byte[] data2 = data.clone();
      data2[0]++;
      result = dut3.equals(new CommandApdu(cla, ins, p1, p2, data2));
      assertFalse(result);
      result = dut4.equals(new CommandApdu(cla, ins, p1, p2, data2, ne));
      assertFalse(result);

      // h.3 data field subset
      if (data.length > 1) { // NOPMD literal in if statement
        // ... NC > 1
        //     => it is possible to build a subset
        data2 = Arrays.copyOfRange(data, 0, data.length - 1);
        result = dut3.equals(new CommandApdu(cla, ins, p1, p2, data2));
        assertFalse(result);
        result = dut4.equals(new CommandApdu(cla, ins, p1, p2, data2, ne));
        assertFalse(result);
      } // end fi

      // h.4 data field superset
      data2 = Arrays.copyOfRange(data, 0, data.length + 1);
      for (int i = 0xff; i-- > 0; ) { // NOPMD assignment in operands
        data2[data.length] = (byte) i;
        result = dut3.equals(new CommandApdu(cla, ins, p1, p2, data2)); // NOPMD new in loop
        assertFalse(result);
        result = dut4.equals(new CommandApdu(cla, ins, p1, p2, data2, ne)); // NOPMD new in loop
        assertFalse(result);
      } // end For (i...)

      // --- i. difference in Ne
      NE_VALUES.forEach(
          ne2 -> {
            // Note: Intentionally here no test with ISO-case 1 and 3 APDU,
            //       because there the Le-field is absent.
            assertEquals(ne.equals(ne2), dut2.equals(new CommandApdu(cla, ins, p1, p2, ne2)));
            assertEquals(ne.equals(ne2), dut4.equals(new CommandApdu(cla, ins, p1, p2, data, ne2)));
          }); // end forEach(ne2 -> ...)

      // --- e. different object, but equal content
      Map.ofEntries(
              Map.entry(dut1, new CommandApdu(cla, ins, p1, p2)),
              Map.entry(dut2, new CommandApdu(cla, ins, p1, p2, ne)),
              Map.entry(dut3, new CommandApdu(cla, ins, p1, p2, data)),
              Map.entry(dut4, new CommandApdu(cla, ins, p1, p2, data, ne)))
          .forEach(
              (cmd1, cmd2) -> {
                final boolean res = cmd1.equals(cmd2);
                assertTrue(res);
              });
    } // end For (ne...)
  } // end method */

  /** Test method for {@link CommandApdu#hashCode()}. */
  @Test
  void test_hashCode() {
    // Note: Because data-field is not taken into account, here we just use
    //       ISO-case 2 command APDU for testing.

    // Test strategy:
    // --- a. loop over bunch of random ISO-case 2 APDU
    // --- b. call hashCode()-method again

    // --- a. loop over bunch of random ISO-case 2 APDU
    NE_VALUES.forEach(
        ne -> {
          final int cla = RNG.nextIntClosed(0, 0xfe);
          final int ins = RNG.nextIntClosed(0, 0xff);
          final int p1 = RNG.nextIntClosed(0, 0xff);
          final int p2 = RNG.nextIntClosed(0, 0xff);
          final CommandApdu dut = new CommandApdu(cla, ins, p1, p2, ne);
          final int exp =
              (int) Long.parseLong(String.format("%02x%02x%02x%02x", cla, ins, p1, p2), 16) * 31
                  + ne;

          assertEquals(exp, dut.hashCode());
        }); // end forEach(ne -> ...)

    // --- b. call hashCode()-method again
    final CommandApdu dut = new CommandApdu(5, 4, 3, 2, RNG.nextBytes(3), 8);

    final int hash = dut.hashCode();

    assertEquals(hash, dut.hashCode());
  } // end method */

  /** Test method for {@link CommandApdu#explainTrailer(int)}. */
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
    // --- b. strange trailer have no explanation

    final CommandApdu dut = new CommandApdu(EMPTY_OS, 1, 2, 3, 4, 0);

    // --- a. explanation not null
    List.of(
            0x6281, 0x6282, 0x6283, 0x6287, 0x6581, 0x6881, 0x6981, 0x6982, 0x6986, 0x6988, 0x6a82,
            0x6a83, 0x6a87, 0x6a8a, 0x6b00, 0x6d00, 0x6e00, 0x9000)
        .forEach(trailer -> assertFalse(dut.explainTrailer(trailer).isEmpty()));

    // --- b. strange trailer have no explanation
    final int trailer = 0x0123;
    assertEquals(
        String.format("no explanation for '%04x' implemented", trailer),
        dut.explainTrailer(trailer));
  } // end method */

  /** Test method for {@link CommandApdu#getBytes()}. */
  @Test
  void test_getBytes() {
    // Assertions:
    // ... a. underlying getBytesShort()-method works as expected
    // ... b. underlying getBytesExtended()-method works as expected
    // ... c. CommandApdu(String)-constructor works as expected

    // Note: Because of the assertions, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    {
      // ISO-case 1
      final var case1 = Map.entry(new CommandApdu(EMPTY_OS, 1, 2, 3, 4, 0), "01 02 0304");
      assertDoesNotThrow(() -> case1.getKey().check(1));

      // ISO-case 2S
      final var case2s = Map.entry(new CommandApdu(EMPTY_OS, 5, 6, 7, 8, 9), "05 06 0708 09");
      assertDoesNotThrow(() -> case2s.getKey().check(2));

      // ISO-case 2E
      final var case2e =
          Map.entry(new CommandApdu(EMPTY_OS, 10, 11, 12, 13, 256), "0a 0b 0c0d 00 0100");
      assertDoesNotThrow(() -> case2e.getKey().check(2));

      // ISO-case 3s
      final var data3s = RNG.nextBytes(1, 255);
      final var case3s =
          Map.entry(
              new CommandApdu(data3s, 14, 15, 16, 17, 0),
              String.format("0e 0f 1011 %02x %s", data3s.length, Hex.toHexDigits(data3s)));
      assertDoesNotThrow(() -> case3s.getKey().check(3));

      // ISO-case 3e
      final var data3e = RNG.nextBytes(256, 1024);
      final var case3e =
          Map.entry(
              new CommandApdu(data3e, 18, 19, 20, 21, 0),
              String.format("12 13 1415 00 %04x %s", data3e.length, Hex.toHexDigits(data3e)));
      assertDoesNotThrow(() -> case3e.getKey().check(3));

      // ISO-case 4s
      final var data4s = RNG.nextBytes(1, 255);
      final var case4s =
          Map.entry(
              new CommandApdu(data4s, 22, 23, 24, 25, NE_SHORT_WILDCARD),
              String.format("16 17 1819 %02x %s 00", data4s.length, Hex.toHexDigits(data4s)));
      assertDoesNotThrow(() -> case4s.getKey().check(4));

      // ISO-case 4e
      final var data4e = RNG.nextBytes(1, 255);
      final var case4e =
          Map.entry(
              new CommandApdu(data4e, 26, 27, 28, 29, 0x4321),
              String.format("1a 1b 1c1d 00 %04x %s 4321", data4e.length, Hex.toHexDigits(data4e)));
      assertDoesNotThrow(() -> case4e.getKey().check(4));

      for (final var entry :
          Map.ofEntries(case1, case2s, case2e, case3s, case3e, case4s, case4e).entrySet()) {
        final var dut = entry.getKey();
        final var expected = Hex.extractHexDigits(entry.getValue());

        final var actual = Hex.toHexDigits(dut.getBytes());

        assertEquals(expected, actual);
      } // end For (entry...)
    } // end --- a.
  } // end method */

  /** Test method for {@link CommandApdu#getBytesExtended()}. */
  @Test
  void test_getBytesExtended() {
    // Assertions:
    // ... a. getCase()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. check all ISO-cases

    // --- a. smoke test
    {
      final var dut = new CommandApdu(EMPTY_OS, 0x42, 0x38, 0x39, 0xaf, 256);
      final var expected = Hex.extractHexDigits("42 38 39af 00 0100");

      final var actual = Hex.toHexDigits(dut.getBytesExtended());

      assertEquals(expected, actual);
    } // end ---a .

    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. check all ISO-cases
    List.of(
            NC_INFIMUM, // infimum
            NC_SUPREMUM_SHORT, // supremum for short format
            NC_SUPREMUM_SHORT + 1, // infimum for extended format
            NC_SUPREMUM)
        .forEach(
            nc -> {
              NE_VALUES.forEach(
                  ne -> {
                    final int cla = RNG.nextIntClosed(0, 0xfe);
                    final int ins = RNG.nextIntClosed(0, 0xff);
                    final int p1 = RNG.nextIntClosed(0, 0xff);
                    final int p2 = RNG.nextIntClosed(0, 0xff);
                    final byte[] data = RNG.nextBytes(nc);

                    List.of(
                            new CommandApdu(EMPTY_OS, cla, ins, p1, p2, 0),
                            new CommandApdu(EMPTY_OS, cla, ins, p1, p2, ne),
                            new CommandApdu(data, cla, ins, p1, p2, 0),
                            new CommandApdu(data, cla, ins, p1, p2, ne))
                        .forEach(
                            dut -> {
                              switch (dut.getCase()) {
                                // ISO-case 1
                                case 1 ->
                                    assertEquals(
                                        String.format("%02x%02x%02x%02x", cla, ins, p1, p2),
                                        Hex.toHexDigits(dut.getBytesExtended()));

                                // ISO-case 2
                                case 2 ->
                                    assertEquals(
                                        String.format(
                                            "%02x%02x%02x%02x%06x", cla, ins, p1, p2, ne & 0xffff),
                                        Hex.toHexDigits(dut.getBytesExtended()));

                                // ISO-case 3
                                case 3 ->
                                    assertEquals(
                                        String.format(
                                            "%02x%02x%02x%02x%06x%s",
                                            cla, ins, p1, p2, nc, Hex.toHexDigits(data)),
                                        Hex.toHexDigits(dut.getBytesExtended()));

                                // ISO-case 4
                                default ->
                                    assertEquals(
                                        String.format(
                                            "%02x%02x%02x%02x%06x%s%04x",
                                            cla,
                                            ins,
                                            p1,
                                            p2,
                                            nc,
                                            Hex.toHexDigits(data),
                                            ne & 0xffff),
                                        Hex.toHexDigits(dut.getBytesExtended()));
                              } // end Switch (case)
                            }); // end forEach(dut -> ...)
                  }); // end forEach(ne -> ...)
            }); // end forEach(nc -> ...)
  } // end method */

  /** Test method for {@link CommandApdu#getBytesShort()}. */
  @Test
  void test_getBytesShort() {
    // Assertions:
    // ... a. isShort()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. check all ISO-cases

    // --- a. smoke test
    {
      final var dut = new CommandApdu(EMPTY_OS, 0x42, 0x38, 0x39, 0xad, NE_SHORT_WILDCARD);
      final var expected = Hex.extractHexDigits("42 38 39ad 00");

      final var actual = Hex.toHexDigits(dut.getBytesShort());

      assertEquals(expected, actual);
    } // end ---a .

    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. check all ISO-cases
    List.of(
            NC_INFIMUM, // infimum
            NC_SUPREMUM_SHORT, // supremum for short format
            NC_SUPREMUM_SHORT + 1, // infimum for extended format
            NC_SUPREMUM)
        .forEach(
            nc -> {
              NE_VALUES.forEach(
                  ne -> {
                    final int cla = RNG.nextIntClosed(0, 0xfe);
                    final int ins = RNG.nextIntClosed(0, 0xff);
                    final int p1 = RNG.nextIntClosed(0, 0xff);
                    final int p2 = RNG.nextIntClosed(0, 0xff);
                    final byte[] data = RNG.nextBytes(nc);

                    List.of(
                            new CommandApdu(EMPTY_OS, cla, ins, p1, p2, 0),
                            new CommandApdu(EMPTY_OS, cla, ins, p1, p2, ne),
                            new CommandApdu(data, cla, ins, p1, p2, 0),
                            new CommandApdu(data, cla, ins, p1, p2, ne))
                        .forEach(
                            dut -> {
                              if (dut.isShort()) {
                                // ... command APDU dut can be encoded in short format
                                switch (dut.getCase()) {
                                  // ISO-case 1
                                  case 1 ->
                                      assertEquals(
                                          String.format("%02x%02x%02x%02x", cla, ins, p1, p2),
                                          Hex.toHexDigits(dut.getBytesShort()));

                                  // ISO-case 2
                                  case 2 ->
                                      assertEquals(
                                          String.format(
                                              "%02x%02x%02x%02x%02x", cla, ins, p1, p2, ne & 0xff),
                                          Hex.toHexDigits(dut.getBytesShort()));

                                  // ISO-case 3
                                  case 3 ->
                                      assertEquals(
                                          String.format(
                                              "%02x%02x%02x%02x%02x%s",
                                              cla, ins, p1, p2, nc, Hex.toHexDigits(data)),
                                          Hex.toHexDigits(dut.getBytesShort()));

                                  // ISO-case 4
                                  default ->
                                      assertEquals(
                                          String.format(
                                              "%02x%02x%02x%02x%02x%s%02x",
                                              cla,
                                              ins,
                                              p1,
                                              p2,
                                              nc,
                                              Hex.toHexDigits(data),
                                              ne & 0xff),
                                          Hex.toHexDigits(dut.getBytesShort()));
                                } // end Switch (case)
                              } else {
                                // ... command APDU dut cannot be encoded in short format
                                //     => exception expected
                                final Throwable throwable =
                                    assertThrows(
                                        IllegalArgumentException.class, dut::getBytesShort);
                                assertEquals(
                                    "according to ISO/IEC 7816-3:2006 clause 12.1.2 this='"
                                        + dut
                                        + "' is not a short APDU",
                                    throwable.getMessage());
                                assertNull(throwable.getCause());
                              } // end else
                            }); // end forEach(dut -> ...)
                  }); // end forEach(ne -> ...)
            }); // end forEach(nc -> ...)
  } // end method */

  /** Test method for {@link CommandApdu#getCase()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_getCase() {
    // Assertions:
    // ... a. constructor work as expected
    // ... b. getNc()- and getNe()-method work as expected

    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // --- a. loop over relevant values for Nc
    // --- b. loop over relevant values for Ne
    // --- c. check all ISO-cases
    final var listNc =
        List.of(
            NC_INFIMUM, // infimum
            NC_SUPREMUM_SHORT, // supremum for short format
            NC_SUPREMUM_SHORT + 1, // infimum for extended format
            NC_SUPREMUM);
    for (final var nc : listNc) {
      for (final var ne : NE_VALUES) {
        final int cla = RNG.nextIntClosed(0, 0xfe);
        final int ins = RNG.nextIntClosed(0, 0xff);
        final int p1 = RNG.nextIntClosed(0, 0xff);
        final int p2 = RNG.nextIntClosed(0, 0xff);
        final byte[] data = RNG.nextBytes(nc);

        assertEquals(1, new CommandApdu(EMPTY_OS, cla, ins, p1, p2, 0).getCase());
        assertEquals(2, new CommandApdu(EMPTY_OS, cla, ins, p1, p2, ne).getCase());
        assertEquals(3, new CommandApdu(data, cla, ins, p1, p2, 0).getCase());
        assertEquals(4, new CommandApdu(data, cla, ins, p1, p2, ne).getCase());
      } // end For (ne...)
    } // end For (nc...)
  } // end method */

  /** Test method for {@link CommandApdu#getChannelNumber()}. */
  @Test
  void test_getChannelNumber() {
    // Assertions:
    // ... a. constructor work as expected
    // ... b. getCla()-method works as expected

    // Note: The simple method relies on CLA byte only. Together with the
    //       assertions, it follows, that it is sufficient to test the
    //       method-under-test for just one ISO-case (here case 1).

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all possible values for CLA byte

    // --- a. smoke test
    {
      final var dut = new CommandApdu(EMPTY_OS, 1, 2, 3, 4, 5);

      assertEquals(1, dut.getChannelNumber());
    } // end --- a.

    // --- b. loop over all possible values for CLA byte
    IntStream.rangeClosed(0, 0xfe)
        .forEach(
            cla ->
                assertEquals(
                    (0 == (cla & 0x40)) ? cla & 0x3 : (cla & 0xf) + 4,
                    new CommandApdu(EMPTY_OS, cla, 0, 0, 0, 0).getChannelNumber()));
  } // end method */

  /** Test method for {@link CommandApdu#getCla()}. */
  @Test
  void test_getCla() {
    // Assertions:
    // ... a. constructor work as expected

    // Note: The simple method relies on CLA byte only. Together with the
    //       assertions, it follows that it is sufficient to test the
    //       method-under-test for just one ISO-case (here case 1).

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all possible values for CLA byte

    // --- a. smoke test
    {
      final var cla = RNG.nextInt() & 0xfffffffe;
      final var dut = new CommandApdu(EMPTY_OS, cla, 0, 0, 0, 0);

      assertEquals(cla & 0xff, dut.getCla());
    } // end --- a.

    // --- b. loop over all possible values for CLA byte
    IntStream.rangeClosed(0, 0xfe)
        .forEach(cla -> assertEquals(cla, new CommandApdu(EMPTY_OS, cla, 0, 0, 0, 0).getCla()));
  } // end method */

  /** Test method for {@link CommandApdu#getData()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_getData() {
    // Assertions:
    // ... a. CommandApdu(Object ...)-constructor works as expected

    // Note: Intentionally here CommandApdu(Object ...)-constructor is used,
    //       because there the command data field is  NOT cloned upon construction.

    // Test strategy:
    // --- a. check for expected content
    // --- b. check for defensive cloning
    for (final var nc : RNG.intsClosed(0, NC_SUPREMUM, 5).boxed().toList()) {
      final byte[] dataIn = RNG.nextBytes(nc);
      final CommandApdu dut =
          new CommandApdu(
              dataIn,
              RNG.nextIntClosed(0, 0xfe), // CLA
              RNG.nextIntClosed(0, 0xfe), // INS
              RNG.nextIntClosed(0, 0xfe), // P1
              RNG.nextIntClosed(0, 0xfe), // P2
              RNG.nextIntClosed(0, 4));

      final byte[] data1 = dut.getData();
      final byte[] data2 = dut.getData();

      assertArrayEquals(dataIn, data1);
      assertArrayEquals(dataIn, data2);
      assertNotSame(dataIn, data1);
      assertNotSame(dataIn, data2);
      assertNotSame(data1, data2);
    } // end For (nc...)
  } // end method */

  /** Test method for {@link CommandApdu#getIns()}. */
  @Test
  void test_getIns() {
    // Assertions:
    // ... a. constructor work as expected

    // Note: The simple method relies on INS byte only. Together with the
    //       assertions, it follows that it is sufficient to test the
    //       method-under-test for just one ISO-case (here case 1).

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all possible values for INS byte

    // --- a. smoke test
    {
      final var ins = RNG.nextInt();
      final var dut = new CommandApdu(EMPTY_OS, 0, ins, 0, 0, 0);

      assertEquals(ins & 0xff, dut.getIns());
    } // end --- a.

    // --- b. loop over all possible values for INS byte
    IntStream.rangeClosed(0, 0xff)
        .forEach(ins -> assertEquals(ins, new CommandApdu(EMPTY_OS, 0, ins, 0, 0, 0).getIns()));
  } // end method */

  /** Test method for {@link CommandApdu#getMaxNr()}. */
  @Test
  void test_getMaxNr() {
    // Assertions:
    // ... a. constructor work as expected
    // ... b. getNe()-method works as expected

    // Note: The simple method relies on the Le-field only. Together with the
    //       assertions, it follows that it is sufficient to test the
    //       method-under-test for just one ISO-case (here mainly case 2).

    // Test strategy:
    // --- a. manual tested corner cases
    // --- b. random values

    // --- a. manual tested corner cases
    assertEquals(0, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, 0).getMaxNr()); // ISO-case 1
    assertEquals(
        NE_SUPREMUM_SHORT, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_SUPREMUM_SHORT).getMaxNr());
    assertEquals(256, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_SHORT_WILDCARD).getMaxNr());
    assertEquals(NE_SUPREMUM, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_SUPREMUM).getMaxNr());
    assertEquals(0x1_0000, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_EXTENDED_WILDCARD).getMaxNr());

    // --- b. random values
    RNG.intsClosed(NE_INFIMUM, NE_SUPREMUM, 20)
        .forEach(ne -> assertEquals(ne, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, ne).getMaxNr()));
  } // end method */

  /** Test method for {@link CommandApdu#getNc()}. */
  @Test
  void test_getNc() {
    // Assertions:
    // ... a. constructor work as expected

    // Note: The simple method relies on data field only. Together with the
    //        assertions, it follows that it is sufficient to test the
    //       method-under-test for just one ISO-case (here mainly case 3).

    // Test strategy:
    // --- a. manual tested corner cases
    // --- b. random values

    // --- a. manual tested corner cases
    assertEquals(0, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, 0).getNc());

    // --- b. random values
    RNG.intsClosed(1, NC_SUPREMUM, 10)
        .forEach(
            nc ->
                assertEquals(
                    nc,
                    new CommandApdu(RNG.nextBytes(nc), 1, 2, 3, 4, 0)
                        .getNc())); // end forEach(nc -> ...)
  } // end method */

  /** Test method for {@link CommandApdu#getNe()}. */
  @Test
  void test_getNe() {
    // Assertions:
    // ... a. constructor work as expected

    // Note: The simple method relies on Le-field only. Together with the
    //        assertions, it follows that it is sufficient to test the
    //       method-under-test for just one ISO-case (here mainly case 2).

    // Test strategy:
    // --- a. smoke test
    // --- b. test with relevant values

    // --- a. smoke test
    {
      assertEquals(0, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, 0).getNe()); // ISO-case 1
      assertEquals(
          NE_SUPREMUM_SHORT, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_SUPREMUM_SHORT).getNe());
      assertEquals(
          NE_SHORT_WILDCARD, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_SHORT_WILDCARD).getNe());
      assertEquals(NE_SUPREMUM, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_SUPREMUM).getNe());
      assertEquals(0x1_0000, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_EXTENDED_WILDCARD).getNe());
    } // end --- a.

    // --- b. test with relevant values
    NE_VALUES.forEach(ne -> assertEquals(ne, new CommandApdu(EMPTY_OS, 0, 1, 2, 3, ne).getNe()));
  } // end method */

  /** Test method for {@link CommandApdu#getP1()}. */
  @Test
  void test_getP1() {
    // Assertions:
    // - none -

    // Note: The simple method relies on parameter P1 only. Together with the
    //        assertions, it follows that it is sufficient to test the
    //       method-under-test for just one ISO-case (here case 1).

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all possible values for P1 byte

    // --- a. smoke test
    {
      final var p1 = RNG.nextInt();
      final var dut = new CommandApdu(EMPTY_OS, 0, 0, p1, 0, 0);

      assertEquals(p1 & 0xff, dut.getP1());
    } // end --- a.
    // --- b. loop over all possible values for P1 byte
    IntStream.rangeClosed(0, 0xff)
        .forEach(p1 -> assertEquals(p1, new CommandApdu(EMPTY_OS, 0, 0, p1, 0, 0).getP1()));
  } // end method */

  /** Test method for {@link CommandApdu#getP2()}. */
  @Test
  void test_getP2() {
    // Assertions:
    // - none -

    // Note: The simple method relies on parameter P1 only. Together with the
    //        assertions, it follows that it is sufficient to test the
    //       method-under-test for just one ISO-case (here case 1).

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all possible values for P2 byte

    // --- a. smoke test
    {
      final var p2 = RNG.nextInt();
      final var dut = new CommandApdu(EMPTY_OS, 0, 0, 0, p2, 0);

      assertEquals(p2 & 0xff, dut.getP2());
    } // end --- a.
    // --- b. loop over all possible values for P2 byte
    IntStream.rangeClosed(0, 0xff)
        .forEach(p2 -> assertEquals(p2, new CommandApdu(EMPTY_OS, 0, 0, 0, p2, 0).getP2()));
  } // end method */

  /** Test method for {@link CommandApdu#isSecureMessagingIndicated()}. */
  @Test
  void test_isSecureMessagingIndicated() {
    // Assertions:
    // ... a. constructor work as expected

    // Note: The simple method relies on CLA byte only. Together with the
    //        assertions, it follows that it is sufficient to test the
    //       method-under-test for just one ISO-case (here case 1).

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over all possible values for CLA byte

    // --- a. smoke test
    {
      assertFalse(new CommandApdu(EMPTY_OS, 0x00, 1, 2, 3, 0).isSecureMessagingIndicated());
      assertTrue(new CommandApdu(EMPTY_OS, 0x04, 1, 2, 3, 0).isSecureMessagingIndicated());
      assertTrue(new CommandApdu(EMPTY_OS, 0x08, 1, 2, 3, 0).isSecureMessagingIndicated());
      assertTrue(new CommandApdu(EMPTY_OS, 0x0c, 1, 2, 3, 0).isSecureMessagingIndicated());
      assertTrue(new CommandApdu(EMPTY_OS, 0x60, 1, 2, 3, 0).isSecureMessagingIndicated());
    } // end --- a.

    // --- b. loop over all possible values for CLA byte
    IntStream.rangeClosed(0, 0xfe)
        .forEach(
            cla ->
                assertEquals(
                    0 != (cla & (0 == (cla & 0x40) ? 0x0c : 0x20)),
                    new CommandApdu(EMPTY_OS, cla, 2, 3, 4, 0).isSecureMessagingIndicated()));
  } // end method */

  /** Test method for {@link CommandApdu#isShort()}. */
  @Test
  void test_isShort() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test for corner cases
    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. check all ISO-cases

    // --- a. smoke test for corner cases
    {
      assertTrue(new CommandApdu(EMPTY_OS, 0, 1, 2, 3, 0).isShort()); // ISO-case 1
      assertTrue(new CommandApdu(EMPTY_OS, 0, 1, 2, 3, 1).isShort()); // ISO-case 2S
      assertTrue(new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_SUPREMUM_SHORT).isShort()); // ISO-case 2S
      assertTrue(new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_SHORT_WILDCARD).isShort()); // ISO-case 2S
      assertFalse(new CommandApdu(EMPTY_OS, 0, 1, 2, 3, 256).isShort()); // ISO-case 2E
      assertFalse(new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_SUPREMUM).isShort()); // ISO-case 2E
      assertFalse(new CommandApdu(EMPTY_OS, 0, 1, 2, 3, NE_EXTENDED_WILDCARD).isShort()); // case 2E
      assertTrue(new CommandApdu(RNG.nextBytes(1), 0, 1, 2, 3, 0).isShort()); // ISO-case 3S
      assertTrue(new CommandApdu(RNG.nextBytes(NC_SUPREMUM_SHORT), 0, 1, 2, 3, 0).isShort()); // 3S
      assertFalse(new CommandApdu(RNG.nextBytes(256), 0, 1, 2, 3, 0).isShort()); // ISO-case 3E
      assertFalse(new CommandApdu(RNG.nextBytes(NC_SUPREMUM), 0, 1, 2, 3, 0).isShort()); // case 3E
      assertTrue(new CommandApdu(RNG.nextBytes(1), 0, 1, 2, 3, 1).isShort()); // ISO-case 4S
      assertTrue(
          new CommandApdu(RNG.nextBytes(NC_SUPREMUM_SHORT), 0, 1, 2, 3, NE_SUPREMUM_SHORT)
              .isShort()); // 4S
      assertTrue(
          new CommandApdu(RNG.nextBytes(NC_SUPREMUM_SHORT), 0, 1, 2, 3, NE_SHORT_WILDCARD)
              .isShort()); // 4S
      assertFalse(new CommandApdu(RNG.nextBytes(256), 0, 1, 2, 3, 1).isShort()); // ISO-case 4E
      assertFalse(new CommandApdu(RNG.nextBytes(NC_SUPREMUM), 0, 1, 2, 3, 1).isShort()); // case 4E
      assertFalse(new CommandApdu(RNG.nextBytes(1), 0, 1, 2, 3, 256).isShort()); // ISO-case 4E
      assertFalse(
          new CommandApdu(RNG.nextBytes(NC_SUPREMUM), 0, 1, 2, 3, NE_SUPREMUM)
              .isShort()); // case 4E
    } // end --- a.

    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. check all ISO-cases
    List.of(
            NC_INFIMUM, // infimum
            NC_SUPREMUM_SHORT, // supremum for short format
            NC_SUPREMUM_SHORT + 1, // infimum for extended format
            NC_SUPREMUM)
        .forEach(
            nc -> {
              NE_VALUES.forEach(
                  ne -> {
                    final int cla = RNG.nextIntClosed(0, 0xfe);
                    final int ins = RNG.nextIntClosed(0, 0xff);
                    final int p1 = RNG.nextIntClosed(0, 0xff);
                    final int p2 = RNG.nextIntClosed(0, 0xff);
                    final byte[] data = RNG.nextBytes(nc);

                    List.of(
                            new CommandApdu(EMPTY_OS, cla, ins, p1, p2, 0),
                            new CommandApdu(EMPTY_OS, cla, ins, p1, p2, ne),
                            new CommandApdu(data, cla, ins, p1, p2, 0),
                            new CommandApdu(data, cla, ins, p1, p2, ne))
                        .forEach(
                            dut -> {
                              final int ncIs = dut.getNc();
                              final int neIs = dut.getNe();
                              final boolean ncShort = ncIs <= NC_SUPREMUM_SHORT;
                              final boolean neShort =
                                  (neIs <= NE_SUPREMUM_SHORT) || (NE_SHORT_WILDCARD == neIs);

                              assertEquals(ncShort && neShort, dut.isShort());
                            }); // end forEach(dut -> ...)
                  }); // end forEach(ne -> ...)
            }); // end forEach(nc -> ...)
  } // end method */

  /** Test method for {@link CommandApdu#removeChannelNumber()}. */
  @Test
  void test_removeChannelNumber() {
    // Assertion:
    // ... a. getChannelNumber()-method works as expected
    // ... b. isSecureMessagingIndicated()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. loop over all possible values for CLA byte
    // --- e. check all ISO-cases

    // --- a. smoke test
    {
      final var input = new CommandApdu(EMPTY_OS, 0x1f, 0xa4, 0x04, 0x0c, 0);
      final var expected = new CommandApdu(EMPTY_OS, 0x10, 0xa4, 0x04, 0x0c, 0);

      final var actual = input.removeChannelNumber();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. loop over all possible values for CLA byte
    // --- e. check all ISO-cases
    List.of(
            NC_INFIMUM, // infimum
            NC_SUPREMUM_SHORT, // supremum for short format
            NC_SUPREMUM_SHORT + 1, // infimum for extended format
            NC_SUPREMUM)
        .forEach(
            nc -> {
              NE_VALUES.forEach(
                  ne -> {
                    IntStream.rangeClosed(0, 0xfe)
                        .forEach(
                            cla -> {
                              final int ins = RNG.nextIntClosed(0, 0xff);
                              final int p1 = RNG.nextIntClosed(0, 0xff);
                              final int p2 = RNG.nextIntClosed(0, 0xff);
                              final byte[] data = RNG.nextBytes(nc);

                              List.of(
                                      new CommandApdu(EMPTY_OS, cla, ins, p1, p2, 0),
                                      new CommandApdu(EMPTY_OS, cla, ins, p1, p2, ne),
                                      new CommandApdu(data, cla, ins, p1, p2, 0),
                                      new CommandApdu(data, cla, ins, p1, p2, ne))
                                  .forEach(
                                      dut -> {
                                        final int chaNo = dut.getChannelNumber();
                                        final var plain = !dut.isSecureMessagingIndicated();

                                        final CommandApdu actual = dut.removeChannelNumber();

                                        if ((0 == chaNo) && plain) {
                                          // ... no changes expected
                                          assertSame(dut, actual);
                                        } else {
                                          final int expCla =
                                              (chaNo < 4)
                                                  ? dut.getCla() & 0xf0
                                                  : dut.getCla() & 0x90;

                                          assertEquals(0, actual.getChannelNumber());
                                          assertFalse(actual.isSecureMessagingIndicated());
                                          assertEquals(expCla, actual.getCla());
                                          assertEquals(ins, actual.getIns());
                                          assertEquals(p1, actual.getP1());
                                          assertEquals(p2, actual.getP2());
                                          assertArrayEquals(dut.getData(), actual.getData());
                                          assertEquals(dut.getNe(), actual.getNe());
                                        } // end else
                                      }); // end forEach(dut -> ...)
                            }); // end forEach(cla -> ...)
                  }); // end forEach(ne -> ...)
            }); // end forEach(nc -> ...)
    // end --- b, c, d, e
  } // end method */

  /** Test method for {@link CommandApdu#setChannelNumber(int)}. */
  @SuppressWarnings({"PMD.CognitiveComplexity"})
  @Test
  void test_setChannelNumber() {
    // Assertions:
    // ... a. getChannelNumber()-method works as expected
    // ... b. isSecureMessagingIndicated()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. loop over all possible values for CLA byte
    // --- e. check all ISO-cases
    // --- f. invalid channelNumber

    // --- a. smoke test
    {
      final var input = new CommandApdu(EMPTY_OS, 0x00, 0xa4, 0x04, 0x0c, 0);
      final var chaNo = 5;
      final var expected = new CommandApdu(EMPTY_OS, 0x41, 0xa4, 0x04, 0x0c, 0);

      final var actual = input.setChannelNumber(chaNo);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. loop over all possible values for CLA byte
    // --- e. check all ISO-cases
    // spotless:off
    List.of(
        NC_INFIMUM, // infimum
        NC_SUPREMUM_SHORT, // supremum for short format
        NC_SUPREMUM_SHORT + 1, // infimum for extended format
        NC_SUPREMUM
    ).forEach(nc -> {
      NE_VALUES.forEach(ne -> {
        IntStream.rangeClosed(0, 0xfe).forEach(cla -> {
          final int ins = RNG.nextIntClosed(0, 0xff);
          final int p1 = RNG.nextIntClosed(0, 0xff);
          final int p2 = RNG.nextIntClosed(0, 0xff);
          final byte[] data = RNG.nextBytes(nc);

          List.of(
              new CommandApdu(EMPTY_OS, cla, ins, p1, p2, 0),
              new CommandApdu(EMPTY_OS, cla, ins, p1, p2, ne),
              new CommandApdu(data, cla, ins, p1, p2, 0),
              new CommandApdu(data, cla, ins, p1, p2, ne)
          ).forEach(dut -> {
            IntStream.rangeClosed(-1, 20).forEach(channelNumber -> {
              final int chaNo = dut.getChannelNumber();
              final var secMe = dut.isSecureMessagingIndicated();

              if ((0 == channelNumber) || (0 != chaNo) || secMe) {
                // ... no changes expected
                assertSame(dut, dut.setChannelNumber(channelNumber));
              } else if ((0 <= channelNumber) && (channelNumber <= 19)) {
                final CommandApdu actual = dut.setChannelNumber(channelNumber);

                final int expCla = (channelNumber < 4)
                    ? dut.getCla() + channelNumber
                    : ((dut.getCla() & 0x90)
                        + 0x40
                        + channelNumber
                        - 4
                    );
                assertEquals(channelNumber, actual.getChannelNumber());
                assertFalse(actual.isSecureMessagingIndicated());
                assertEquals(
                    expCla,
                    actual.getCla(),
                    () -> String.format("chNo=%d%ndut=%s%nact=%s", channelNumber, dut, actual)
                );
                assertEquals(ins, actual.getIns());
                assertEquals(p1, actual.getP1());
                assertEquals(p2, actual.getP2());
                assertArrayEquals(dut.getData(), actual.getData());
                assertEquals(dut.getNe(), actual.getNe());
              } else {
                // --- f. invalid channelNumber
                final Throwable throwable = assertThrows(
                    IllegalArgumentException.class,
                    () -> dut.setChannelNumber(channelNumber), // RV_RETURN_VALUE_IGNORED_INFERRED
                    () -> "chNo=" + channelNumber + ": " + dut
                );
                assertEquals("out of range [0, 19]", throwable.getMessage());
              } // end else
            }); // end forEach(channelNumber -> ...)
          }); // end forEach(dut -> ...)
        }); // end forEach(cla -> ...)
      }); // end forEach(ne -> ...)
    }); // end forEach(nc -> ...)
    // spotless:on
    // end --- b, c, d, e, f.
  } // end method */

  /** Test method for serialisation. */
  @Test
  void test_serialize() {
    // Assertions:
    // ... a. equals()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. create a bunch of command APDU
    // --- c. serialize each command APDU
    // --- d. deserialize each command APDU
    // --- e. check if deserialized APDU equals original APDU

    // --- a. smoke test
    {
      final var dut =
          new CommandApdu(
              RNG.nextBytes(3), // command data field
              RNG.nextIntClosed(0x00, 0xfe), // CLA
              RNG.nextInt(), // INS
              RNG.nextInt(), // P1
              RNG.nextInt(), // P2
              RNG.nextIntClosed(1, NE_SUPREMUM) // Ne
              );

      byte[] transfer = EMPTY_OS;
      final String string = dut.toString();
      final int length = string.length();

      // --- b. serialize each command APDU
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(string);
        oos.writeObject(dut);
        oos.writeInt(length);
        oos.flush();
        transfer = baos.toByteArray();
      } catch (IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)

      try (ByteArrayInputStream bais = new ByteArrayInputStream(transfer);
          ObjectInputStream ois = new ObjectInputStream(bais)) {
        // --- d. deserialize each command APDU
        final Object outA = ois.readObject();
        final Object outB = ois.readObject();
        final int outC = ois.readInt();

        // --- e. check if deserialized APDU equals original APDU
        assertEquals(string, outA);
        assertEquals(outB, dut);
        assertEquals(outC, length);
      } catch (ClassNotFoundException | IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end --- a.

    // --- b. create a bunch of command APDU
    // b.1 loop over all relevant values for Nc
    // b.2 loop over all relevant values for Ne
    // b.3 check all ISO-cases
    final List<CommandApdu> originals = new ArrayList<>();
    List.of(
            NC_INFIMUM, // infimum
            NC_SUPREMUM_SHORT, // supremum for short format
            NC_SUPREMUM_SHORT + 1, // infimum for extended format
            NC_SUPREMUM)
        .forEach(
            nc -> {
              NE_VALUES.forEach(
                  ne -> {
                    final int cla = RNG.nextIntClosed(0, 0xfe);
                    final int ins = RNG.nextIntClosed(0, 0xff);
                    final int p1 = RNG.nextIntClosed(0, 0xff);
                    final int p2 = RNG.nextIntClosed(0, 0xff);
                    final byte[] data = RNG.nextBytes(nc);

                    originals.add(new CommandApdu(EMPTY_OS, cla, ins, p1, p2, 0));
                    originals.add(new CommandApdu(EMPTY_OS, cla, ins, p1, p2, ne));
                    originals.add(new CommandApdu(data, cla, ins, p1, p2, 0));
                    originals.add(new CommandApdu(data, cla, ins, p1, p2, ne));
                  }); // end forEach(ne -> ...)
            }); // end forEach(nc -> ...)

    originals.forEach(
        dut -> {
          byte[] transfer = EMPTY_OS;
          final String string = dut.toString();
          final int length = string.length();

          // --- b. serialize each command APDU
          try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
              ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(string);
            oos.writeObject(dut);
            oos.writeInt(length);
            oos.flush();
            transfer = baos.toByteArray();
          } catch (IOException e) {
            fail(UNEXPECTED, e);
          } // end Catch (...)

          try (ByteArrayInputStream bais = new ByteArrayInputStream(transfer);
              ObjectInputStream ois = new ObjectInputStream(bais)) {
            // --- d. deserialize each command APDU
            final Object outA = ois.readObject();
            final Object outB = ois.readObject();
            final int outC = ois.readInt();

            // --- e. check if deserialized APDU equals original APDU
            assertEquals(string, outA);
            assertEquals(outB, dut);
            assertEquals(outC, length);
          } catch (ClassNotFoundException | IOException e) {
            fail(UNEXPECTED, e);
          } // end Catch (...)
        }); // end forEach(cmdApdu -> ...)
  } // end method */

  /** Test method {@link CommandApdu#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // ... a. isShort()-method works as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. check all ISO-cases

    // --- a. smoke test
    {
      final var dut = new CommandApdu(EMPTY_OS, 0x00, 0xa4, 0x04, 0x04, NE_SHORT_WILDCARD);
      final var expected = "CLA='00'  INS='a4'  P1='04'  P2='04'  Lc and Data absent  Le='00'";

      final var actual = dut.toString();

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. loop over relevant values for Nc
    // --- c. loop over relevant values for Ne
    // --- d. check all ISO-cases
    List.of(
            NC_INFIMUM, // infimum
            NC_SUPREMUM_SHORT, // supremum for short format
            NC_SUPREMUM_SHORT + 1, // infimum for extended format
            NC_SUPREMUM)
        .forEach(
            nc -> {
              NE_VALUES.forEach(
                  ne -> {
                    final int cla = RNG.nextIntClosed(0, 0xfe);
                    final int ins = RNG.nextIntClosed(0, 0xff);
                    final int p1 = RNG.nextIntClosed(0, 0xff);
                    final int p2 = RNG.nextIntClosed(0, 0xff);
                    final byte[] data = RNG.nextBytes(nc);
                    final String datS = Hex.toHexDigits(data);
                    final String cmdHeader =
                        String.format(
                            "CLA='%02x'  INS='%02x'  P1='%02x'  P2='%02x'", cla, ins, p1, p2);

                    List.of(
                            new CommandApdu(EMPTY_OS, cla, ins, p1, p2, 0),
                            new CommandApdu(EMPTY_OS, cla, ins, p1, p2, ne),
                            new CommandApdu(data, cla, ins, p1, p2, 0),
                            new CommandApdu(data, cla, ins, p1, p2, ne))
                        .forEach(
                            dut -> {
                              final String lcAndData;
                              final String leField;
                              if (dut.isShort()) {
                                // ... command APDU dut can be encoded in short format
                                lcAndData = String.format("  Lc='%02x'  Data='%s'", nc, datS);
                                leField = String.format("  Le='%02x'", ne & 0xff);
                              } else {
                                // ... command APDU dut cannot be encoded in short format
                                lcAndData = String.format("  Lc='%04x'  Data='%s'", nc, datS);
                                leField = String.format("  Le='%04x'", ne & 0xffff);
                              } // end else

                              switch (dut.getCase()) { // NOPMD default missing (false positive)
                                // ISO-case 1
                                case 1 ->
                                    assertEquals(
                                        cmdHeader + "  Lc and Data absent  Le absent",
                                        dut.toString());

                                // ISO-case 2
                                case 2 ->
                                    assertEquals(
                                        cmdHeader + "  Lc and Data absent" + leField,
                                        dut.toString());

                                // ISO-case 3
                                case 3 ->
                                    assertEquals(
                                        cmdHeader + lcAndData + "  Le absent", dut.toString());

                                // ISO-case 4
                                default ->
                                    assertEquals(cmdHeader + lcAndData + leField, dut.toString());
                              } // end Switch (case)
                            }); // end forEach(dut -> ...)
                  }); // end forEach(ne -> ...)
            }); // end forEach(nc -> ...)
  } // end method */
} // end class
