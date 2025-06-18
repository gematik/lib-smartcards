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
import static de.gematik.smartcards.sdcom.apdu.CommandApdu.SFI_SUPREMUM;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractRecord.RECORD_NUMBER_ABSENT;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractRecord.RECORD_NUMBER_INFIMUM;
import static de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.AbstractRecord.RECORD_NUMBER_SUPREMUM;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestReadRecord {

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

  /** Test method for {@link ReadRecord#ReadRecord(int, int, int)}. */
  @Test
  void test_ReadRecord__int_int_int() {
    // Assertions:
    // ... a. constructor(s) from superclasses work as expected

    // Test strategy:
    // --- a. smoke test
    // --- b. border tests with relevant combinations of input parameter
    // --- c. ERROR: record number out of range

    // --- a. smoke test
    {
      assertEquals("00b2420c38", Hex.toHexDigits(new ReadRecord(66, 1, 56).getBytes()));
    } // end --- a.

    // --- b. border tests with relevant combinations of input parameter
    // spotless:off
    RNG.intsClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD, 5).forEach(ne ->
        RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5).forEach(sfi -> {
          RNG.intsClosed(RECORD_NUMBER_INFIMUM, RECORD_NUMBER_SUPREMUM, 5).forEach(recNo ->
              assertDoesNotThrow(() -> new ReadRecord(recNo, sfi, ne)));

          // --- c. ERROR: record number out of range
          List.of(RECORD_NUMBER_ABSENT, RECORD_NUMBER_SUPREMUM + 1).forEach(recNo ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> new ReadRecord(recNo, sfi, ne))
          ); // end forEach(recNo -> ...)
        }) // end forEach(sfi -> ...)
    ); // end forEach(ne -> ...)
    // spotless:on
    // end --- b, c
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
        new ReadRecord(
            30, // record number
            29, // SFI
            15); // Ne

    // --- a. smoke test
    {
      final var other = new ReadRecord(30, 29, 15);

      assertNotSame(dut, other);
      assertTrue(dut.equals(other)); // NOPMD simplify assertion
    } // end --- a.

    Map.ofEntries(
            // --- b. smoke test with    same   class,    same   content
            Map.entry(new ReadRecord(30, 29, 15), List.of(true, true)),
            // --- c. smoke test with    same   class, different content
            Map.entry(new ReadRecord(30, 28, 15), List.of(false, false)),
            // --- d. smoke test with different class,    same   content
            Map.entry(new CommandApdu(0x00, 0xb2, 0x1e, 0xec, 15), List.of(false, true)))
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

    // --- a. create a bunch of command APDU
    // spotless:off
    final List<ReadRecord> originals = new ArrayList<>();
    RNG.intsClosed(NE_INFIMUM, NE_EXTENDED_WILDCARD, 5).forEach(ne ->
        RNG.intsClosed(SFI_ABSENT, SFI_SUPREMUM, 5).forEach(sfi ->
            RNG.intsClosed(RECORD_NUMBER_INFIMUM, RECORD_NUMBER_SUPREMUM, 5).forEach(recNo ->
                originals.add(new ReadRecord(recNo, sfi, ne))
            ) // end forEach(recNo -> ...)
        ) // end forEach(sfi -> ...)
    ); // end forEach(ne -> ...)
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
} // end method */
