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

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiRng;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class for testing methods in interface {@link ApduLayer}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestApduLayer {

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

  /** Test method for {@link ApduLayer#getTime()}. */
  @Test
  void test_getTime() {
    // Assertions:
    // ... a. send()-method works as expected

    // Test strategy:
    // --- a. smoke test

    final ApduLayer dut = new MyApduLayer();

    // --- a. smoke test
    final int cla = RNG.nextIntClosed(0, 0x7f);
    final int ins = RNG.nextIntClosed(0, 0xff);
    dut.send(new CommandApdu(cla, ins, 1, 2));

    assertEquals(
        (cla * 256 + ins) / 1000.0, // expected
        dut.getTime(), // actual
        0.001); // epsilon
  } // end method */

  /** Test method for {@link ApduLayer#send(CommandApdu)}. */
  @Test
  void test_send__CommandApdu() {
    // Test strategy:
    // --- a. check for IllegalArgumentException
    // --- b. smoke test

    final ApduLayer dut = new MyApduLayer();

    // --- a. check for IllegalArgumentException
    {
      final var cmd = new CommandApdu(0x80, 1, 2, 3);

      final var throwable = assertThrows(IllegalArgumentException.class, () -> dut.send(cmd));

      assertEquals("CLA='80'", throwable.getMessage());
      assertNull(throwable.getCause());
    } // end --- a.

    // --- b. smoke test
    Map.ofEntries(
            Map.entry(
                "00 01 0001", // case 1
                "SW1SW2='0001'  Data absent"),
            Map.entry(
                "00 01 ffff 03", // case 2, more data available than needed for response data field
                "SW1SW2='ffff'  Nr='0003'  Data='0001ff'"),
            Map.entry(
                "12 34 5678 02 9abc", // case 3
                "SW1SW2='5678'  Data absent"),
            Map.entry(
                "12 34 5678 02 9abc 0a", // case 3
                "SW1SW2='5678'  Nr='000a'  Data='12345678029abc0a0000'"))
        .forEach(
            (cmd, rsp) ->
                assertEquals(
                    rsp,
                    dut.send(new CommandApdu(cmd)).toString())); // end forEach((cmd, rsp) -> ...)
  } // end method */

  /** Test method for {@link ApduLayer#send(CommandApdu, int...)}. */
  @Test
  void test_send__CommandApdu_intA() {
    // Test strategy:
    // --- a. smoke tests with matching trailer
    // --- b. empty trailer array
    // --- c. some trailer given, but none match

    final ApduLayer dut = new MyApduLayer();

    // --- a. smoke tests with matching trailer
    try {
      // 1 expected trailer
      assertEquals(0x0000, dut.send(new CommandApdu("00 01 0000"), 0).getTrailer());

      // 2 expected trailers
      assertEquals(0x0001, dut.send(new CommandApdu("00 01 0001"), 0, 1).getTrailer());

      // 3 expected trailers
      assertEquals(0x1001, dut.send(new CommandApdu("00 01 1001"), 0, 0x1001, 1).getTrailer());
    } catch (IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.

    // --- b. empty trailer array
    {
      final var cmd = new CommandApdu("00 01 9000");

      final var t = assertThrows(IllegalArgumentException.class, () -> dut.send(cmd, new int[0]));

      assertEquals(
          "unexpected trailer: NoError, rspApdu = SW1SW2='9000'  Data absent", t.getMessage());
      assertNull(t.getCause());
    } // end --- b.

    // --- c. some trailer given, but none match
    {
      final CommandApdu cmd = new CommandApdu("00 02 9000");
      List.of(
              new int[] {0x6982}, // 1 expected trailer
              new int[] {0x6982, 0x6a88}, // 2 expected trailers
              new int[] {0x6982, 0x6a88, 0x6b00} // 3 expected trailers
              )
          .forEach(
              trailers -> {
                final Throwable throwable =
                    assertThrows(IllegalArgumentException.class, () -> dut.send(cmd, trailers));
                assertEquals(
                    "unexpected trailer: NoError, rspApdu = SW1SW2='9000'  Data absent",
                    throwable.getMessage());
                assertNull(throwable.getCause());
              }); // end forEach(trailers -> ...)
    } // end --- c.
  } // end method */

  /** Test method for {@link ApduLayer#send(CommandApdu, ResponseApdu)}. */
  @Test
  void test_send__CommandApdu_ResponseApdu() {
    // Assertions:
    // ... a. send(CommandApdu)-method works as expected
    // ... b. difference(ResponseApdu)-method works as expected

    // Note: Because of the assertions we can be lazy here and
    //       concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test with no difference
    // --- b. smoke test with a difference

    final ApduLayer dut = new MyApduLayer();

    // --- a. smoke test with no difference
    try {
      final CommandApdu cmd = new CommandApdu("00 01 0203");
      final ResponseApdu rsp = new ResponseApdu("0203");

      assertEquals(rsp, dut.send(cmd, rsp));
    } catch (IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.

    // --- b. smoke test with a difference
    {
      final var cmd = new CommandApdu("00 03 9000");
      final var rsp = new ResponseApdu("6700");

      final var t = assertThrows(IllegalArgumentException.class, () -> dut.send(cmd, rsp));

      assertEquals(
          "NoError: [TRAILER], "
              + "expectedResponse SW1SW2='6700'  Data absent !="
              + " SW1SW2='9000'  Data absent receivedResponse",
          t.getMessage());
      assertNull(t.getCause());
    } // end --- b.
  } // end method */

  /** Test method for {@link ApduLayer#send(CommandApdu, ResponseApdu, ResponseApdu)}. */
  @Test
  void test_send__CommandApdu_ResponseApdu_ResponseApdu() {
    // Assertions:
    // ... a. send(CommandApdu)-method works as expected
    // ... b. difference(ResponseApdu, ResponseApdu)-method works as expected

    // Note: Because of the assertions we can be lazy here and
    //       concentrate on code-coverage.

    // Test strategy:
    // --- a. smoke test with no difference
    // --- b. smoke test with a difference

    final ApduLayer dut = new MyApduLayer();

    // --- a. smoke test with no difference
    try {
      final CommandApdu cmd = new CommandApdu("00 01 0203");
      final ResponseApdu rsp = new ResponseApdu("0202");

      assertEquals(new ResponseApdu("0203"), dut.send(cmd, rsp, new ResponseApdu("fffe")));
    } catch (IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- a.

    // --- b. smoke test with a difference
    {
      final var cmd = new CommandApdu("00 04 9000");
      final var rsp = new ResponseApdu("6700");
      final var mask = new ResponseApdu("ffff");

      final var t = assertThrows(IllegalArgumentException.class, () -> dut.send(cmd, rsp, mask));

      assertEquals(
          "NoError: [TRAILER], "
              + "expectedResponse SW1SW2='6700'  Data absent !="
              + " SW1SW2='9000'  Data absent receivedResponse",
          t.getMessage());
      assertNull(t.getCause());
    } // end --- b.
  } // end method */

  /**
   * Class used for testing methods in interface {@link ApduLayer}.
   *
   * <p>This class provides an {@link ApduLayer} with the following properties:
   *
   * <ol>
   *   <li>If bit b8 in CLA byte is set, then {@link IllegalArgumentException} is thrown.
   *   <li>CLA and INS byte are used to set the execution time as follows: {@code executionTime =
   *       (CLA * 256 + INS) / 1000.0}.
   *   <li>Parameter P1 is used as SW1 in trailer and parameter P2 is used as SW2 in trailer.
   *   <li>The response data field contains as many octet as indicated by {@code Ne} copied from the
   *       octet string representation of the {@link CommandApdu}.
   * </ol>
   *
   * @author <a href="mailto:software-development@gematik.de">gematik</a>
   */
  /* package */ static class MyApduLayer implements ApduLayer {

    /** Execution time in seconds. */
    private double insTime; // NOPMD missing accessor (false positive) */

    /**
     * Return execution time of the previous command-response pair.
     *
     * @return execution time in seconds of previous command-response pair
     */
    @Override
    public double getTime() {
      return insTime;
    } // end method */

    /**
     * Sends given message.
     *
     * @param message to be sent, typically a command
     * @return corresponding response message
     */
    @Override
    public byte[] send(final byte[] message) {
      final CommandApdu commandApdu = new CommandApdu(message);

      // --- check CLA byte
      final int cla = commandApdu.getCla();
      if (cla >= 0x80) { // NOPMD literal in if statement
        // ... bit b8 in CLA is set
        //     => throw CardException
        throw new IllegalArgumentException(String.format("CLA='%02x'", cla));
      } // end fi

      // --- set execution time
      final int ins = commandApdu.getIns();
      insTime = ((cla << 8) + ins) / 1000.0;

      // --- estimate response data field
      final int ne = commandApdu.getNe();
      final byte[] data = Arrays.copyOfRange(commandApdu.getBytes(), 0, ne);
      final int trailer = (commandApdu.getP1() << 8) + commandApdu.getP2();

      return new ResponseApdu(data, trailer).getBytes();
    } // end method */
  } // end inner class
} // end class
