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
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_ABSENT;
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_SUPREMUM;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for testing abstract class {@link AppendRecord}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestAppendRecord {

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

  /** Test method for {@link AppendRecord#AppendRecord(int, byte[])}. */
  @Test
  void test_AppendRecord__int_byteA() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. border tests with relevant combinations of input parameter
    // --- c. ERROR: SFI out of range

    // --- a. smoke test
    {
      assertEquals(
          "00e2001803112233",
          Hex.toHexDigits(new AppendRecord(3, Hex.toByteArray("112233")).getBytes()));
    } // end --- a.

    // --- b. border tests with relevant combinations of input parameter
    RNG.intsClosed(NC_INFIMUM, NC_SUPREMUM, 5)
        .forEach(
            nc -> {
              final byte[] data = RNG.nextBytes(nc);

              RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5)
                  .forEach(
                      sfi -> {
                        final AppendRecord dut = new AppendRecord(sfi, data);

                        assertEquals(0x00, dut.getCla());
                        assertEquals(0xe2, dut.getIns());
                        assertEquals(0x00, dut.getP1());
                        assertEquals(sfi << 3, dut.getP2());
                        assertEquals(3, dut.getCase());
                        assertEquals(nc, dut.getNc());
                        assertArrayEquals(data, dut.getData());
                      }); // end forEach(sfi -> ...)

              // --- c. ERROR: SFI out of range
              List.of(
                      SFI_ABSENT - 1, // just below infimum
                      SFI_SUPREMUM + 1 // just above supremum
                      )
                  .forEach(
                      sfi ->
                          assertThrows(
                              IllegalArgumentException.class,
                              () -> new AppendRecord(sfi, data))); // end forEach(sfi -> ...)
            }); // end forEach(nc -> ...)
    // end --- b, c
  } // end method */

  /** Test method for {@link AppendRecord#AppendRecord(int, String)}. */
  @Test
  void test_AppendRecord__int_String() {
    // Assertions:
    // ... a. underlying constructor works as expected

    // Note: Because of the assertion we can be lazy here and concentrate
    //       on code coverage.

    // Test strategy:
    // --- a. smoke test
    {
      assertEquals(
          "00e200180411223344", Hex.toHexDigits(new AppendRecord(3, "11223344").getBytes()));
    } // end --- a.
  } // end method */

  /** Test method for {@link CommandApdu#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. smoke test
    // --- b. smoke test with    same   class,    same   content
    // --- c. smoke test with    same   class, different content
    // --- d. smoke test with different class,    same   content

    final var cmdDataField = "135642";
    final CommandApdu dut = new AppendRecord(30, cmdDataField);

    // --- a. smoke test
    {
      final CommandApdu other = new AppendRecord(30, cmdDataField);

      assertNotSame(dut, other);
      assertTrue(dut.equals(other)); // NOPMD simplify assertion
    } // end --- a.

    Map.ofEntries(
            // --- b. smoke test with    same   class,    same   content
            Map.entry(new AppendRecord(30, cmdDataField), List.of(true, true)),
            // --- c. smoke test with    same   class, different content
            Map.entry(new AppendRecord(30, "754321"), List.of(false, false)),
            // --- d. smoke test with different class,    same   content
            Map.entry(new CommandApdu(0x00, 0xe2, 0x00, 0xf0, cmdDataField), List.of(false, true)))
        .forEach(
            (obj, result) -> {
              assertNotSame(dut, obj);
              assertEquals(result.get(0), dut.equals(obj));
              if (result.get(1)) {
                assertEquals(Hex.toHexDigits(dut.getBytes()), Hex.toHexDigits(obj.getBytes()));
              } // end fi
            }); // end forEach((obj, result) -> ...)
  } // end method */

  /** Test method for {@link AppendRecord#explainTrailer(int)}. */
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

    final CommandApdu dut = new AppendRecord(0, "12");

    // --- a. explanation not empty
    List.of(0x6a84).forEach(trailer -> assertFalse(dut.explainTrailer(trailer).isEmpty()));

    // --- b. check that some trailers are explained by superclass
    assertEquals("NoError", dut.explainTrailer(0x9000));
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
    final List<AppendRecord> originals = new ArrayList<>();
    RNG.intsClosed(NC_INFIMUM, NC_SUPREMUM, 5)
        .forEach(
            nc -> {
              final byte[] data = RNG.nextBytes(nc);

              RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5)
                  .forEach(
                      sfi -> originals.add(new AppendRecord(sfi, data))); // end forEach(sfi -> ...)
            }); // end forEach(nc -> ...)
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
