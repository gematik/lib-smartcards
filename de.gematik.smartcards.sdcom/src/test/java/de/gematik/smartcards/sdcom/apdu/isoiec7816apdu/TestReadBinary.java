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

import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_EXTENDED_WILDCARD;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.NE_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_ABSENT;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_SUPREMUM;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for testing {@link ReadBinary}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestReadBinary {

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

  /** Test method for {@link ReadBinary#ReadBinary(int, int, int)}. */
  @Test
  void test_ReadBinary__int_int_int() {
    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant valid input values
    // --- c. invalid SFI
    // --- d. offset < 0
    // --- e. offset >= 0, but invalid for SFI present
    // --- f. offset >= 0, but invalid for SFI absent
    // --- g. invalid Ne

    final int offsetInfimum = 0;
    final int offsetSfiSupremum = 255;
    final int offsetSupremum = 0x7fff;

    // --- a. smoke test
    {
      assertEquals("00b0810203", Hex.toHexDigits(new ReadBinary(1, 2, 3).getBytes()));
    } // end --- a.

    // --- b. loop over relevant valid input values
    RNG.intsClosed(SFI_INFIMUM, SFI_SUPREMUM, 5)
        .forEach(
            sfi -> {
              RNG.intsClosed(offsetInfimum, offsetSfiSupremum, 5)
                  .forEach(
                      offset -> {
                        RNG.intsClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD, 5)
                            .forEach(
                                ne ->
                                    assertDoesNotThrow(
                                        () ->
                                            new ReadBinary(
                                                sfi, offset, ne))); // end forEach(ne -> ...)
                      }); // end forEach(offset -> ...)
            }); // end forEach(sfi -> ...)
    // end --- b.

    // --- c. invalid SFI
    List.of(SFI_ABSENT - 1, SFI_SUPREMUM + 1)
        .forEach(
            sfi -> {
              RNG.intsClosed(offsetInfimum, offsetSupremum, 5)
                  .forEach(
                      offset -> {
                        RNG.intsClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD, 5)
                            .forEach(
                                ne -> {
                                  final Throwable throwable =
                                      assertThrows(
                                          IllegalArgumentException.class,
                                          () -> new ReadBinary(sfi, offset, ne));
                                  assertEquals("invalid SFI: " + sfi, throwable.getMessage());
                                  assertNull(throwable.getCause());
                                }); // end forEach(ne -> ...)
                      }); // end forEach(offset -> ...)
            }); // end forEach(sfi -> ...)
    // end --- c.

    // --- d. offset < 0
    RNG.intsClosed(SFI_INFIMUM, SFI_SUPREMUM, 5)
        .forEach(
            sfi -> {
              RNG.intsClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD, 5)
                  .forEach(
                      ne -> {
                        final Throwable throwable =
                            assertThrows(
                                IllegalArgumentException.class,
                                () -> new ReadBinary(sfi, offsetInfimum - 1, ne));
                        assertEquals("invalid offset: -1", throwable.getMessage());
                        assertNull(throwable.getCause());
                      }); // end forEach(ne -> ...)
            }); // end forEach(sfi -> ...)
    // end --- d.

    // --- e. offset >= 0, but invalid for SFI present
    RNG.intsClosed(SFI_INFIMUM, SFI_SUPREMUM, 5)
        .forEach(
            sfi -> {
              RNG.intsClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD, 5)
                  .forEach(
                      ne -> {
                        final Throwable throwable =
                            assertThrows(
                                IllegalArgumentException.class,
                                () -> new ReadBinary(sfi, offsetSfiSupremum + 1, ne));
                        assertEquals(
                            "invalid offset for SFI present: " + 256, throwable.getMessage());
                        assertNull(throwable.getCause());
                      }); // end forEach(ne -> ...)
            }); // end forEach(sfi -> ...)
    // end --- e.

    // --- f. offset >= 0, but invalid for SFI absent
    RNG.intsClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD, 5)
        .forEach(
            ne -> {
              final Throwable throwable =
                  assertThrows(
                      IllegalArgumentException.class,
                      () -> new ReadBinary(SFI_ABSENT, offsetSupremum + 1, ne));
              assertEquals("invalid offset: " + 32_768, throwable.getMessage());
              assertNull(throwable.getCause());
            }); // end forEach(ne -> ...)
    // end --- f.

    // --- g. invalid Ne
    RNG.intsClosed(SFI_INFIMUM, SFI_SUPREMUM, 5)
        .forEach(
            sfi -> {
              RNG.intsClosed(offsetInfimum, offsetSfiSupremum, 5)
                  .forEach(
                      offset -> {
                        List.of(NE_INFIMUM - 1, NE_EXTENDED_WILDCARD + 1)
                            .forEach(
                                ne -> {
                                  final Throwable throwable =
                                      assertThrows(
                                          IllegalArgumentException.class,
                                          () -> new ReadBinary(SFI_ABSENT, offset, ne));
                                  assertEquals(
                                      (0 == ne)
                                          ? "Le-field absent, but SHALL be present"
                                          : "invalid Ne: " + ne,
                                      throwable.getMessage());
                                  assertNull(throwable.getCause());
                                }); // end forEach(ne -> ...)
                      }); // end forEach(offset -> ...)
            }); // end forEach(sfi -> ...)
    // end --- g.
  } // end method */

  /** Test method for {@link CommandApdu#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. smoke test
    // --- b. smoke test with    same   class,    same   content
    // --- c. smoke test with    same   class, different content
    // --- d. smoke test with different class,    same   content

    final CommandApdu dut = new ReadBinary(1, 2, 3);

    // --- a. smoke test
    {
      final var other = new ReadBinary(1, 2, 3);

      assertNotSame(dut, other);
      assertTrue(dut.equals(other)); // NOPMD simplify assertion
    } // end --- a.

    Map.ofEntries(
            // --- b. smoke test with    same   class,    same   content
            Map.entry(new ReadBinary(1, 2, 3), List.of(true, true)),
            // --- c. smoke test with    same   class, different content
            Map.entry(new ReadBinary(1, 2, 4), List.of(false, false)),
            // --- d. smoke test with different class,    same   content
            Map.entry(new CommandApdu(0, 0xb0, 0x81, 2, 3), List.of(false, true)))
        .forEach(
            (obj, result) -> {
              assertNotSame(dut, obj);
              assertEquals(result.get(0), dut.equals(obj));
              if (result.get(1)) {
                assertEquals(Hex.toHexDigits(dut.getBytes()), Hex.toHexDigits(obj.getBytes()));
              } // end fi
            }); // end forEach((obj, result) -> ...)
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

    final int offsetInfimum = 0;
    final int offsetSfiSupremum = 255;

    // --- a. create a bunch of command APDU
    // spotless:off
    final List<ReadBinary> originals = new ArrayList<>();
    RNG.intsClosed(SFI_INFIMUM, SFI_SUPREMUM, 5).forEach(sfi -> {
      RNG.intsClosed(offsetInfimum, offsetSfiSupremum, 5).forEach(offset -> {
        RNG.intsClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD, 5).forEach(ne ->
            originals.add(new ReadBinary(sfi, offset, ne))
        ); // end forEach(ne -> ...)
      }); // end forEach(offset -> ...)
    }); // end forEach(sfi -> ...)
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
} // end class
