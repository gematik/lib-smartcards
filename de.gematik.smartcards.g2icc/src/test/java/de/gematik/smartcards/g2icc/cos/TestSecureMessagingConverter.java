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
package de.gematik.smartcards.g2icc.cos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import de.gematik.smartcards.utils.Hex;
import java.util.Arrays;
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
 * Class performing white-box tests for {@link SecureMessagingConverter}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestSecureMessagingConverter {

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

  /** Test method for {@link SecureMessagingConverter#secureCommand(CommandApdu)}. */
  @Test
  void test_secureCommand__CommandApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test, case 1, plain
    // --- b. smoke test, case 2, plain
    // --- c. smoke test, case 3, plain
    // --- d. smoke test, case 4, plain
    // --- e. smoke test, case 1, cipher
    // --- f. smoke test, case 2, cipher
    // --- g. smoke test, case 3, cipher
    // --- h. smoke test, case 4, cipher
    // --- i. smoke test, case 1, logical channel 4
    // --- j. smoke test, case 4, logical channel 19
    // --- k. loop over valid range of input parameter

    final var dut = new MySecureMessagingConverter();
    dut.setSmCmdEnc(false);

    // --- a. smoke test, case 1
    {
      final var input = new CommandApdu("01 a4 8101");
      final var expected =
          new CommandApdu(
              0x0d,
              0xa4,
              0x81,
              0x01,
              "8e-14-c3eef0eb17776ee261b0694007216442dcddb305",
              CommandApdu.NE_SHORT_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. smoke test, case 2, plain
    {
      final var input = new CommandApdu("02 a4 8102 00");
      final var expected =
          new CommandApdu(
              0x0e,
              0xa4,
              0x81,
              0x02,
              "97-01-00   8e-14-7cd310fb0dc5fb28ccc58f8fec785cbe3f56e49f",
              CommandApdu.NE_EXTENDED_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- b.

    // --- c. smoke test, case 3, plain
    {
      final var input = new CommandApdu("03 a4 8103 04 abc42def");
      final var expected =
          new CommandApdu(
              0x0f,
              0xa4,
              0x81,
              0x03,
              "81-04-abc42def   8e-14-65883dfe7f2559666810db52e1b209b4b7149f4c",
              CommandApdu.NE_SHORT_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- c.

    // --- d. smoke test, case 4, plain
    {
      final var input = new CommandApdu("00 a4 8104 00 0004 abc42def 0000");
      final var expected =
          new CommandApdu(
              0x0c,
              0xa4,
              0x81,
              0x04,
              "81-04-abc42def   97-02-0000   8e-14-db87481ad8231551a5bbbfeb2cce131eff36edbd",
              CommandApdu.NE_EXTENDED_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- d.

    dut.setSmCmdEnc(true);

    // --- e. smoke test, case 1, cipher
    {
      final var input = new CommandApdu("01 a4 8701");
      final var expected =
          new CommandApdu(
              0x0d,
              0xa4,
              0x87,
              0x01,
              "8e-14-0c8e33da842d8232e2fb15aba696c39190433365",
              CommandApdu.NE_SHORT_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- e.

    // --- f. smoke test, case 2, cipher
    {
      final var input = new CommandApdu("02 a4 8702 00 1234");
      final var expected =
          new CommandApdu(
              0x0e,
              0xa4,
              0x87,
              0x02,
              "97-02-1234   8e-14-7285ee57e610279259a0b8b76eafa86dd68f6173",
              CommandApdu.NE_EXTENDED_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- b.

    // --- g. smoke test, case 3, cipher
    {
      final var input = new CommandApdu("03 a4 8703 06 43ff007f80b2");
      final var expected =
          new CommandApdu(
              0x0f,
              0xa4,
              0x87,
              0x03,
              "87-11-014400018081b381010101010101010101"
                  + "8e-14-91e8c931314954ceb810ef324308f54d7448f8d6",
              CommandApdu.NE_SHORT_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- g.

    // --- h. smoke test, case 4, cipher
    {
      final var input = new CommandApdu("03 a4 8704 06 43ff007f80b2 18");
      final var expected =
          new CommandApdu(
              0x0f,
              0xa4,
              0x87,
              0x04,
              "87-11-014400018081b381010101010101010101"
                  + "97-01-18"
                  + "8e-14-b741479e5845c2bb1b5ff48cf85fe8858e4b2bcd",
              CommandApdu.NE_EXTENDED_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- h.

    // --- i. smoke test, case 1, logical channel 4
    {
      final var input = new CommandApdu("40 a4 8701");
      final var expected =
          new CommandApdu(
              0x60,
              0xa4,
              0x87,
              0x01,
              "89-04-60a48701   8e-14-8f340cddf3087b5c1139517688b3865c256e8f7c",
              CommandApdu.NE_SHORT_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- i.

    // --- j. smoke test, case 4, logical channel 19
    {
      final var input = new CommandApdu("4f a4 8704 00 0004 abc42def ffff");
      final var expected =
          new CommandApdu(
              0x6f,
              0xa4,
              0x87,
              0x04,
              "89-04-6fa48704"
                  + "87-11-01acc52ef0810101010101010101010101"
                  + "97-02-ffff"
                  + "8e-14-78a361c96410a176424a36c31e3f3ccf58b53bf4",
              CommandApdu.NE_EXTENDED_WILDCARD);

      final var actual = dut.secureCommand(input);

      assertEquals(expected, actual);
    } // end --- j.

    // --- k. loop over valid range of input parameter
    // TODO
  } // end method */

  /** Test method for {@link SecureMessagingConverter#unsecureResponse(ResponseApdu)}. */
  @Test
  void test_unsecureResponse__ResponseApdu() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test, no data
    // --- b. smoke test, data, plain
    // --- c. smoke test, data, cipher
    // --- d. smoke test, nothing to unsecure
    // --- e. ERROR: invalid input
    // --- f. ERROR: status DO with wrong length

    final var dut = new MySecureMessagingConverter();

    // --- a. smoke test, no data
    {
      final var expected = new ResponseApdu("912a");
      final var tdo = BerTlv.getInstance(0x99, expected.getBytes());
      final var mac = EafiHashAlgorithm.SHA_1.digest(tdo.getEncoded());
      final var input =
          new ResponseApdu(
              AfiUtils.concatenate(tdo.getEncoded(), BerTlv.getInstance(0x8e, mac).getEncoded()),
              expected.getSw());

      final var actual = dut.unsecureResponse(input);

      assertEquals(expected, actual);
      assertEquals(
          new ResponseApdu(
              Hex.toByteArray("99-02-912a   8e-14-53060908f0369b8c343f126f8386f5fe71e8e386"),
              expected.getSw()),
          input);
    } // end --- a.

    // --- b. smoke test, data, plain
    {
      final var expected = new ResponseApdu("ab00ff7f8017 912b");
      final var pdo = BerTlv.getInstance(0x81, expected.getData());
      final var tdo = BerTlv.getInstance(0x99, String.format("%04x", expected.getSw()));
      final var mac =
          EafiHashAlgorithm.SHA_1.digest(AfiUtils.concatenate(pdo.getEncoded(), tdo.getEncoded()));
      final var input =
          new ResponseApdu(
              AfiUtils.concatenate(
                  pdo.getEncoded(), tdo.getEncoded(), BerTlv.getInstance(0x8e, mac).getEncoded()),
              expected.getSw());

      final var actual = dut.unsecureResponse(input);

      assertEquals(expected, actual);
      assertEquals(
          new ResponseApdu(
              Hex.toByteArray(
                  "81-06-ab00ff7f8017"
                      + "99-02-912b"
                      + "8e-14-afe91de553190f41d838217da39b0df618894da2"),
              expected.getSw()),
          input);
    } // end --- b.

    // --- c. smoke test, data, cipher
    // c.1 only odd tags
    // c.2 an even tag in between

    // c.1 only odd tags
    {
      final var expected = new ResponseApdu("ab00ff7f8017 912c");
      final var pdo = BerTlv.getInstance(0x87, dut.encipher(expected.getData()));
      final var tdo = BerTlv.getInstance(0x99, String.format("%04x", expected.getSw()));
      final var mac =
          EafiHashAlgorithm.SHA_1.digest(AfiUtils.concatenate(pdo.getEncoded(), tdo.getEncoded()));
      final var input =
          new ResponseApdu(
              AfiUtils.concatenate(
                  pdo.getEncoded(), tdo.getEncoded(), BerTlv.getInstance(0x8e, mac).getEncoded()),
              expected.getSw());

      final var actual = dut.unsecureResponse(input);

      assertEquals(expected, actual);
      assertEquals(
          new ResponseApdu(
              Hex.toByteArray(
                  "87-11-01ac010080811881010101010101010101"
                      + "99-02-912c"
                      + "8e-14-eec32a414f5dbf9df54151c6a5ee756864c81d67"),
              expected.getSw()),
          input);
    } // end c.1

    // c.2 an even tag in between
    {
      final var expected = new ResponseApdu("ab00ff7f8017 912c");
      final var pdo = BerTlv.getInstance(0x87, dut.encipher(expected.getData()));
      final var tdo = BerTlv.getInstance(0x99, String.format("%04x", expected.getSw()));
      final var mac =
          EafiHashAlgorithm.SHA_1.digest(
              AfiUtils.concatenate(dut.padIso(pdo.getEncoded()), tdo.getEncoded()));
      final var input =
          new ResponseApdu(
              AfiUtils.concatenate(
                  pdo.getEncoded(),
                  BerTlv.getInstance(0x82, "abcdef").getEncoded(),
                  tdo.getEncoded(),
                  BerTlv.getInstance(0x8e, mac).getEncoded()),
              expected.getSw());

      final var actual = dut.unsecureResponse(input);

      assertEquals(expected, actual);
      assertEquals(
          new ResponseApdu(
              Hex.toByteArray(
                  "87-11-01ac010080811881010101010101010101"
                      + "82-03-abcdef"
                      + "99-02-912c"
                      + "8e-14-"
                      + Hex.toHexDigits(mac)),
              expected.getSw()),
          input);
    } // end c.2
    // end --- c.

    // --- d. smoke test, nothing to unsecure
    Map.ofEntries(
            Map.entry("98-02-3456 9000", "9000"), // neither MAC-DO nor odd tags
            Map.entry("7234", "7234") // response data-field absent
            )
        .forEach(
            (input, expected) -> {
              final var actual = dut.unsecureResponse(new ResponseApdu(input));

              assertEquals(new ResponseApdu(expected), actual);
            }); // end forEach(input -> ...)
    // end --- d.

    // --- e. ERROR: invalid input
    List.of(
            "99-02-6700  8e-01-27 9000", // wrong MAC
            "99-026-700 9000", // odd tags but no MAC-DO
            "8e-02-4711 9000", // MAC-DO but no odd tags
            "1234 9000" // response data-field not BER-TLV encoded
            )
        .forEach(
            input ->
                assertThrows(
                    IllegalArgumentException.class,
                    () ->
                        dut.unsecureResponse(
                            new ResponseApdu(input)))); // end forEach(input -> ...)
    // end --- e.

    // --- f. ERROR: status DO with wrong length
    List.of(
            "90", // just one octet
            "904711" // three octet
            )
        .forEach(
            input -> {
              final var tdo = BerTlv.getInstance(0x99, input);
              final var mac = EafiHashAlgorithm.SHA_1.digest(tdo.getEncoded());
              final var in =
                  new ResponseApdu(
                      AfiUtils.concatenate(
                          tdo.getEncoded(), BerTlv.getInstance(0x8e, mac).getEncoded()),
                      0x9000);

              final var throwable =
                  assertThrows(IllegalArgumentException.class, () -> dut.unsecureResponse(in));
              assertEquals("Status-DO has wrong length", throwable.getMessage());
            }); // end forEach(input -> ...)
    // end --- f.
  } // end method */

  /** Test method for {@link SecureMessagingConverter#padIso(byte[])}. */
  @Test
  void test_padIso__byteA() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant of input parameter

    final var blockLength = 16;
    final var dut = new MySecureMessagingConverter();
    final var separator = new byte[] {-128};
    final var zero = new byte[1];

    // --- a. smoke test
    {
      final var input = Hex.toByteArray("0102 0304 0506 0708 09");
      final var expected = Hex.toByteArray("0102 0304 0506 0708 09  80 0000 0000 0000");

      final var actual = dut.padIso(input);

      assertArrayEquals(expected, actual);
    } // end --- a.

    // --- b. loop over relevant of input parameter
    Set.of(
            AfiUtils.EMPTY_OS,
            Hex.toByteArray("80"),
            Hex.toByteArray("af8000"),
            RNG.nextBytes(15),
            RNG.nextBytes(16),
            RNG.nextBytes(17),
            RNG.nextBytes(31),
            RNG.nextBytes(32),
            RNG.nextBytes(33))
        .forEach(
            input -> {
              var expected = AfiUtils.concatenate(input, separator);
              while (0 != (expected.length % blockLength)) {
                expected = AfiUtils.concatenate(expected, zero);
              } // end While (...)

              final var actual = dut.padIso(input);

              assertArrayEquals(expected, actual);
            }); // end forEach(input -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link SecureMessagingConverter#computeCryptographicChecksum(byte[])}. */
  @Test
  void test_computeCryptographicChecksum__byteA() {
    // Assertions:
    // - none -

    // Note: Here we test an abstract method. Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new MySecureMessagingConverter();

    // --- a. smoke test
    {
      final var input = RNG.nextBytes(1, 64);
      final var expected = EafiHashAlgorithm.SHA_1.digest(input);

      final var actual = dut.computeCryptographicChecksum(input);

      assertArrayEquals(expected, actual);
    } // end --- a.
  } // end method */

  /** Test method for {@link SecureMessagingConverter#decipher(byte[])}. */
  @Test
  void test_decipher__byteA() {
    // Assertions:
    // - none -

    // Note: Here we test an abstract method. Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. ERROR: wrong padding

    final var dut = new MySecureMessagingConverter();

    // --- a. smoke test
    {
      final var expected = "afcb0578ff";
      final var cipher = Hex.extractHexDigits("01 b0cc067900 8101010101010101010101");

      final var actual = Hex.toHexDigits(dut.decipher(Hex.toByteArray(cipher)));

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    IntStream.range(0, 40)
        .forEach(
            length -> {
              final var expected = RNG.nextBytes(length);
              final var cipher = dut.encipher(expected);

              final var actual = dut.decipher(cipher);

              assertArrayEquals(expected, actual);
            }); // end --- b.

    // --- c. ERROR: wrong padding
    {
      final var input = Hex.toByteArray("01 01 b0cc067900 81010101010101010101ff");

      assertThrows(IllegalArgumentException.class, () -> dut.decipher(input));
    } // end --- c.
  } // end method */

  /** Test method for {@link SecureMessagingConverter#encipher(byte[])}. */
  @Test
  void test_encipher__byteA() {
    // Assertions:
    // - none -

    // Note: Here we test an abstract method. Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test

    final var dut = new MySecureMessagingConverter();

    // --- a. smoke test
    {
      final var input = Hex.toByteArray("afcb0578ff");
      final var expected = Hex.extractHexDigits("01 b0cc067900 8101010101010101010101");

      final var actual = dut.encipher(input);

      assertEquals(expected, Hex.toHexDigits(actual));
    } // end --- a.
  } // end method */

  /**
   * Test method for {@link SecureMessagingConverter#verifyCryptographicChecksum(byte[], byte[])}.
   */
  @Test
  void test_verifyCryptographicChecksum__byteA_byteA() {
    // Assertions:
    // ... a. computeCryptographicChecksum(byte[])-method works as expected

    // Note: Here we test an abstract method. Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test, MAC okay
    // --- b. smoke test, MAC not okay

    final var dut = new MySecureMessagingConverter();

    // --- a. smoke test, MAC okay
    {
      final var input = RNG.nextBytes(1, 64);
      final var mac = dut.computeCryptographicChecksum(input);
      final var expected = true;

      final var actual = dut.verifyCryptographicChecksum(input, mac);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. smoke test, MAC not okay
    {
      final var input = RNG.nextBytes(1, 64);
      final var mac = dut.computeCryptographicChecksum(input);
      mac[0]++;
      final var expected = false;

      final var actual = dut.verifyCryptographicChecksum(input, mac);

      assertEquals(expected, actual);
    } // end --- b.
  } // end method */

  /** Test method for getter and setter of {@link SecureMessagingConverter} {@code SmCmdEnc}. */
  @Test
  void test_get_set_SmCmdEnc() {
    // Assertions:
    // - none -

    // Note: This simple method does not need intensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test for freshly initialized objects
    // --- b. loop over relevant range of input parameter

    final var dut = new MySecureMessagingConverter();

    // --- a. smoke test for freshly initialized objects
    {
      assertTrue(dut.isSmCmdEnc());
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    List.of(true, false, false, true, true)
        .forEach(
            expected -> {
              dut.setSmCmdEnc(expected);

              final var actual = dut.isSmCmdEnc();

              assertEquals(expected, actual);
            }); // end forEach(expected -> ...)
    // end --- b.
  } // end method */

  /** Test method for {@link SecureMessagingConverter#isSmRspEnc()}. */
  @Test
  void test_isSmRspEnc() {
    // Assertions:
    // - none -

    // Note: This simple method does not need intensive testing.
    //       Thus, we can be lazy here.

    // Test strategy:
    // --- a. smoke test for freshly initialized objects
    // --- b. loop over relevant range of input parameter

    final var dut = new MySecureMessagingConverter();

    // --- a. smoke test for freshly initialized objects
    {
      assertFalse(dut.isSmRspEnc());
    } // end --- a.
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /**
   * Class used for testing {@link SecureMessagingConverter}.
   *
   * @author <a href="mailto:software-development@gematik.de">gematik</a>
   */
  private static final class MySecureMessagingConverter extends SecureMessagingConverter {

    /** Hash algorithm. */
    private static final EafiHashAlgorithm HASH = EafiHashAlgorithm.SHA_1; // */

    /**
     * Computes a MAC for the given message.
     *
     * @param message of arbitrary length (i.e., without padding)
     * @return MAC, here: {@link EafiHashAlgorithm#SHA_1} hash value
     */
    @Override
    protected byte[] computeCryptographicChecksum(final byte[] message) {
      return HASH.digest(message);
    } // end method */

    /**
     * Deciphers given cryptogram, inverse operation to {@link #encipher(byte[])}.
     *
     * @param data padding indicator || cryptogram
     * @return plain text without any padding
     * @throws IllegalArgumentException {@code plainText} is not correctly padded
     */
    @Override
    protected byte[] decipher(final byte[] data) {
      final var plain = Arrays.copyOfRange(data, 1, data.length);

      // Caesar's code with delta = 1;
      for (int i = plain.length; i-- > 0; ) { // NOPMD reassignment of loop control variable
        plain[i]--;
      } // end For (i...)

      return truncateIso(plain);
    } // end method */

    /**
     * Enciphers given message, inverse operation to {@link #decipher(byte[])}.
     *
     * @param message of arbitrary length (i.e., without any padding)
     * @return here: paddingIndicator || chipher
     */
    @Override
    protected byte[] encipher(final byte[] message) {
      final var cipher = padIso(message);

      // Caesar's code with delta = 1
      for (int i = cipher.length; i-- > 0; ) { // NOPMD reassignment of loop control variable
        cipher[i]++;
      } // end For (i...)

      return AfiUtils.concatenate(PADDING_INDICATOR, cipher);
    } // end method */

    /**
     * Checks if data correspond to MAC.
     *
     * @param data protected by MAC, note: there is no ISO padding at the end of data
     * @param mac protecting data
     * @return true if MAC is correct, false otherwise
     */
    @Override
    protected boolean verifyCryptographicChecksum(final byte[] data, final byte[] mac) {
      return Arrays.equals(mac, HASH.digest(data));
    } // end method */

    /**
     * Performs an ISO padding on given message.
     *
     * @param message to be padded
     * @return {@code message || 80 (00 ... 00)}, length is a multiple of 16
     */
    @Override
    protected byte[] padIso(final byte[] message) {
      final int newLength = ((message.length >> 4) + 1) << 4;
      final byte[] result = Arrays.copyOf(message, newLength);

      // --- set delimiter according to ISO/IEC 7816-4
      result[message.length] = (byte) 0x80;

      return result;
    } // end method */

    /**
     * Truncates a given {@code input} according to ISO/IEC 7816-4 padding.
     *
     * <p>This is the inverse operation to {@link #padIso(byte[])}.
     *
     * @param input to be truncated
     * @return truncated {@code input}
     * @throws IllegalArgumentException {@code input} is not correctly padded
     */
    private byte[] truncateIso(final byte[] input) {
      try {
        int blockLength = 16;
        int i = input.length;

        // truncate the rest of padding bytes, at most block length - 1
        while ((input[--i] == 0x00) && (--blockLength > 0)) { // NOPMD empty while
          // intentionally empty
        } // end While (...)

        // test and truncate delimiter
        if (-128 == input[i]) {
          return Arrays.copyOfRange(input, 0, i);
        } // end fi
      } catch (ArrayIndexOutOfBoundsException e) { // NOPMD empty catch block
        // intentionally empty
      } // end Catch (...)
      // ... invalid padding

      throw new IllegalArgumentException("padding not in accordance to ISO/IEC 7816-4");
    } // end method */
  } // end inner class
} // end class
