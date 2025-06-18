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

import static de.gematik.smartcards.utils.AfiUtils.KIBI;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.event.Level.INFO;
import static org.slf4j.event.Level.TRACE;

import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.IoChallenge;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerOctetString;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.utils.AfiRng;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Class for white-box testing of {@link Scenario7816}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestScenario7816 {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestScenario7816.class);

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

  /** Test method for {@link Scenario7816#Scenario7816(List)}. */
  @Test
  void test_Scenario7816__List() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. empty list
    // --- c. extra objects in the list
    // --- d. extra objects in a Collection

    // --- a. smoke test
    {
      final var expScenario =
          List.of(
              new Scenario7816.LoggingInformation(INFO, "foo 1"),
              List.of(0x9000, 0x6A83),
              new CommandApdu("00 a4 04 04 00"));

      final var dut = new Scenario7816(expScenario);

      assertEquals(Scenario7816.VERSION, dut.getVersion());
      assertEquals(expScenario, dut.getScenario());
    } // end --- a.

    // --- b. empty list
    {
      final var expScenario = Collections.emptyList();

      final var dut = new Scenario7816(expScenario);

      assertEquals(Scenario7816.VERSION, dut.getVersion());
      assertEquals(expScenario, dut.getScenario());
      assertTrue(dut.getScenario().isEmpty());
    } // end --- b.

    // --- c. extra objects in the list
    {
      final var expScenario =
          List.of(
              new Scenario7816.LoggingInformation(INFO, "bar"),
              List.of(0x9000, 0x6A83),
              new CommandApdu("00 a4 04 04 80"));
      final var input = new ArrayList<>(expScenario);
      input.add("to be ignored");

      final var dut = new Scenario7816(input);

      assertEquals(Scenario7816.VERSION, dut.getVersion());
      assertEquals(expScenario, dut.getScenario());
    } // end --- c.

    // --- d. extra objects in a Collection
    {
      final var logi = new Scenario7816.LoggingInformation(TRACE, "foo bar");
      final List<Object> expEsw = List.of(0x7243, 0x6400);
      final List<Object> input = new ArrayList<>(expEsw);
      input.add("ignore me");
      final var cmd = new CommandApdu("80 a4 04 04 00");
      final var expScenario = List.of(logi, expEsw, cmd);

      final var dut = new Scenario7816(List.of(logi, input, cmd));

      assertEquals(Scenario7816.VERSION, dut.getVersion());
      assertEquals(expScenario, dut.getScenario());
    } // end --- d.
  } // end method */

  /** Test method for {@link Scenario7816#Scenario7816(DerSequence)}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_Scenario7816__DerSequence() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of input parameter
    // --- c. extra DOs
    // --- d. ERROR: version number out of range
    // --- e. corner cases

    final var infimumVersion = BigInteger.valueOf(Integer.MIN_VALUE);
    final var supremumVersion = BigInteger.valueOf(Integer.MAX_VALUE);
    final var levels = Level.values();
    final var maxLevelIndex = levels.length - 1;

    // --- a. smoke test
    {
      final var expVersion = 42;
      final var expEsw = List.of(0x9000, 0x6982);
      final var expLog = new Scenario7816.LoggingInformation(TRACE, "Foo Bar");
      final var expCmd = new CommandApdu("80 42 1234 04 11223344 13");
      final var expScenario = List.of(expEsw, expLog, expCmd);
      final var input =
          new DerSequence(
              List.of(
                  new DerInteger(BigInteger.valueOf(expVersion)),
                  new DerSequence(
                      List.of(
                          new DerSequence(
                              expEsw.stream()
                                  .map(i -> new DerInteger(BigInteger.valueOf(i)))
                                  .toList()),
                          expLog.toTlv(),
                          new DerOctetString(expCmd.getBytes())))));

      final var dut = new Scenario7816(input);

      assertEquals(expVersion, dut.getVersion());
      assertEquals(expScenario, dut.getScenario());
    } // end --- a.

    // --- b. loop over relevant range of input parameter
    IntStream.range(0, 100)
        .forEach(
            i -> {
              final var input = new ArrayList<BerTlv>();
              final var expVersion = RNG.nextInt();
              input.add(new DerInteger(BigInteger.valueOf(expVersion)));
              final var expScenario = new ArrayList<>();
              final var inList = new ArrayList<BerTlv>();

              while (expScenario.size() < i) {
                final var rnd = RNG.nextDouble();
                if (rnd < 0.5) { // NOPMD literal in if statement
                  // ... 50% command APDU
                  final var cmd = IoChallenge.random(0, Long.MAX_VALUE, KIBI, KIBI).getBytes();
                  inList.add(new DerOctetString(cmd));
                  expScenario.add(new CommandApdu(cmd));
                } else if (rnd < 0.7) { // NOPMD literal in if statement
                  // ... 20% expected status words
                  final var esw =
                      IntStream.range(0, RNG.nextIntClosed(0, 10))
                          .map(j -> RNG.nextIntClosed(0, 0xffff))
                          .boxed()
                          .toList();
                  inList.add(
                      new DerSequence(
                          esw.stream().map(j -> new DerInteger(BigInteger.valueOf(j))).toList()));
                  expScenario.add(esw);
                } else {
                  // ... 30% logging information
                  final var logi =
                      new Scenario7816.LoggingInformation(
                          levels[RNG.nextIntClosed(0, maxLevelIndex)], RNG.nextUtf8(0, 32));
                  inList.add(logi.toTlv());
                  expScenario.add(logi);
                } // end else
              } // end While (not enough objects in scenario)
              input.add(new DerSequence(inList));

              final var dut = new Scenario7816(new DerSequence(input));

              assertEquals(expVersion, dut.getVersion());
              assertEquals(expScenario, dut.getScenario());
            }); // end forEach(i -> ...)
    // end ---b.

    // --- c. extra DOs
    Map.ofEntries(
            // extra DO at level 0
            Map.entry(
                """
                    30-14
                       02 01 ff
                       82 00
                       02 01 00
                       02 01 47
                       30-07
                          04-05-00 0a 0203 a2
                    30-00
                    """,
                List.of(-1, List.of(new CommandApdu("00 0a 0203 a2")))),
            // extra DO at level 1
            Map.entry(
                """
                    30-1f
                       02 01 01
                       02 01 42
                       30-17
                          a2-00
                          30-09
                             02-03-009000
                             02-02-6a42
                          04-04-80 20 0123
                          82-00
                          30-00
                    """,
                List.of(
                    1,
                    List.of(
                        List.of(0x9000, 0x6a42),
                        new CommandApdu("80 20 0123"),
                        Collections.emptyList()))),
            // extra DO at level 2
            Map.entry(
                """
                    30-2f
                       02 01 31
                       30-2a
                          04-0a-83 34 4321 04 aabbccdd 42
                          30-10
                             02-02-6400
                             04-04-01020304
                             02-02-6200
                             82-00
                          04-0a-83 34 4321 04 aabbccdd 43
                    """,
                List.of(
                    49,
                    List.of(
                        new CommandApdu("83 34 4321 04 aabbccdd 42"),
                        List.of(0x6400, 0x6200),
                        new CommandApdu("83 34 4321 04 aabbccdd 43")))))
        .forEach(
            (input, expected) -> {
              final var expVersion = expected.getFirst();
              final var expScenario = expected.getLast();

              final var dut = new Scenario7816((DerSequence) BerTlv.getInstance(input));

              assertEquals(expVersion, dut.getVersion());
              assertEquals(expScenario, dut.getScenario());
            }); // end forEach((input, expected) -> ...)
    // end --- c.

    // --- d. ERROR: version number out of range
    Map.ofEntries(
            Map.entry(infimumVersion.subtract(BigInteger.ONE), Boolean.TRUE),
            Map.entry(infimumVersion, Boolean.FALSE),
            Map.entry(supremumVersion, Boolean.FALSE),
            Map.entry(supremumVersion.add(BigInteger.ONE), Boolean.TRUE))
        .forEach(
            (version, flagException) -> {
              final var input =
                  new DerSequence(
                      List.of(new DerInteger(version), new DerSequence(Collections.emptyList())));
              if (flagException) {
                assertThrows(ArithmeticException.class, () -> new Scenario7816(input));
              } else {
                final var dut = new Scenario7816(input);

                assertEquals(version.intValueExact(), dut.getVersion());
                assertTrue(dut.getScenario().isEmpty());
              } // end else
            }); // end forEach((version, flagException) -> ...)
    // end --- d.

    // --- e. corner cases
    // e.1 empty object list
    // e.2 empty list with expected status words

    // e.1 empty object list
    {
      final var version = RNG.nextInt();

      final var dut =
          new Scenario7816(
              new DerSequence(
                  List.of(
                      new DerInteger(BigInteger.valueOf(version)), BerTlv.getInstance("3000"))));

      assertEquals(version, dut.getVersion());
      assertTrue(dut.getScenario().isEmpty());
    } // end e.1

    // e.2 empty list with expected status words
    {
      final var version = RNG.nextInt();

      final var dut =
          new Scenario7816(
              new DerSequence(
                  List.of(
                      new DerInteger(BigInteger.valueOf(version)),
                      new DerSequence(List.of(new DerSequence(Collections.emptyList()))))));

      assertEquals(version, dut.getVersion());
      assertEquals(List.of(Collections.emptyList()), dut.getScenario());
    } // end e.2
    // end --- e.
  } // end method */

  /** Test method for {@link Scenario7816#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. difference in version
    // --- e. difference in scenario
    // --- f. different objects same content

    final var scenario =
        List.of(new Scenario7816.LoggingInformation(INFO, "do it"), new CommandApdu("80 79 1234"));
    final var dut = new Scenario7816(scenario);

    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    for (final Object[] obj :
        new Object[][] {
          new Object[] {dut, true}, // --- a. same reference
          new Object[] {null, false}, // --- b. null input
          new Object[] {"afi", false}, // --- c. difference in type
        }) {
      assertEquals(obj[1], dut.equals(obj[0]));
    } // end For (obj...)
    // end --- a, b, c.

    // --- d. difference in version
    {
      final var other =
          new Scenario7816(
              new DerSequence(
                  List.of(
                      new DerInteger(BigInteger.valueOf(Scenario7816.VERSION + 1)),
                      new DerSequence(
                          List.of(
                              ((Scenario7816.LoggingInformation) scenario.getFirst()).toTlv(),
                              new DerOctetString(
                                  ((CommandApdu) scenario.getLast()).getBytes()))))));
      assertEquals(dut.getScenario(), other.getScenario());

      assertNotEquals(dut, other);
    } // end --- d.

    // --- e. difference in scenario
    {
      final var other =
          new Scenario7816(
              new DerSequence(
                  List.of(
                      new DerInteger(BigInteger.valueOf(Scenario7816.VERSION)),
                      new DerSequence(
                          List.of(
                              new DerOctetString(
                                  ((CommandApdu) scenario.getLast()).getBytes()))))));
      assertEquals(dut.getVersion(), other.getVersion());

      assertNotEquals(dut, other);
    } // end --- e.

    // --- f. different objects same content
    {
      final var other = new Scenario7816(scenario);

      assertNotSame(dut, other);
      assertEquals(dut, other);
    } // end --- f.
  } // end method */

  /** Test method for {@link Scenario7816#hashCode()}. */
  @Test
  void test_hashCode() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. calculate again

    final var version = RNG.nextInt();
    final var scenario =
        List.of(
            List.of(RNG.nextIntClosed(0, 0xffff), RNG.nextIntClosed(0, 0xffff)),
            new CommandApdu("00 47 11 0815"));
    final var dut =
        new Scenario7816(
            new DerSequence(
                List.of(
                    new DerInteger(BigInteger.valueOf(version)),
                    new DerSequence(
                        List.of(
                            new DerSequence(
                                ((List<?>) scenario.getFirst())
                                    .stream()
                                        .filter(i -> i instanceof Integer)
                                        .map(i -> new DerInteger(BigInteger.valueOf((int) i)))
                                        .toList()),
                            new DerOctetString(((CommandApdu) scenario.getLast()).getBytes()))))));

    // --- a. smoke test
    int expected = version;
    expected = expected * 31 + scenario.hashCode();

    final var actual = dut.hashCode();

    assertEquals(expected, actual);
    // end --- a.

    // --- b. calculate again
    assertEquals(actual, dut.hashCode());
  } // end method */

  /** Test method for {@link Scenario7816#run(ApduLayer, Logger)}. */
  @Test
  void test_run__ApduLayer_Logger() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. ERROR: unexpected status word

    final var myApduLayer = new MyApduLayer();

    // --- a. smoke test
    {
      final var dut =
          new Scenario7816(
              List.of(
                  List.of(0x9000, 0x6400),
                  new Scenario7816.LoggingInformation(INFO, "foo 2"),
                  new CommandApdu("80 fe 8765 03 112233")));
      final var expected = List.of(new ResponseApdu("af 9000"));
      myApduLayer.insResponseApdus.addAll(expected);

      final var actual = dut.run(myApduLayer, LOGGER);

      assertEquals(expected, actual);
    } // end --- a.

    // --- b. ERROR: unexpected status word
    {
      final var dut =
          new Scenario7816(
              List.of(
                  List.of(0x9000, 0x6400),
                  new Scenario7816.LoggingInformation(INFO, "bar"),
                  new CommandApdu("00 a4 040c"),
                  new CommandApdu("12 34 5678 00"),
                  new CommandApdu("80 fe 8765 03 112233")));
      final var expected = List.of(new ResponseApdu("af 9000"), new ResponseApdu("6700"));
      myApduLayer.insResponseApdus.addAll(expected);

      final var actual = dut.run(myApduLayer, LOGGER);

      assertEquals(expected, actual);
    } // end --- b.
  } // end method */

  /** Test method for {@link Scenario7816#toString()}. */
  @Test
  void test_toString() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    {
      final var dut =
          new Scenario7816(
              new DerSequence(
                  List.of(
                      new DerInteger(BigInteger.valueOf(17)),
                      new DerSequence(
                          List.of(
                              new Scenario7816.LoggingInformation(INFO, "Foo Bar").toTlv(),
                              new DerSequence(List.of(new DerInteger(BigInteger.valueOf(0x9000)))),
                              new DerOctetString(new CommandApdu("00 a4 0404 00 0000").getBytes()),
                              new DerSequence(
                                  List.of(
                                      new DerInteger(BigInteger.valueOf(0x9000)),
                                      new DerInteger(BigInteger.valueOf(0x6A82)))),
                              new DerOctetString(
                                  new CommandApdu("00 a4 020c 02 2Fa3").getBytes()))))));
      final var expected =
          String.join(
              LINE_SEPARATOR,
              List.of(
                  "version = 17",
                  "INFO, \"Foo Bar\"",
                  "esw = {9000}",
                  "CLA='00'  INS='a4'  P1='04'  P2='04'  Lc and Data absent  Le='0000'",
                  "esw = {9000, 6a82}",
                  "CLA='00'  INS='a4'  P1='02'  P2='0c'  Lc='02'  Data='2fa3'  Le absent"));

      final var actual = dut.toString();

      assertEquals(expected, actual);
    } // end --- a.
  } // end method */

  /** Test method for {@link Scenario7816#toTlv()}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_toTlv() {
    // Assertions:
    // - none -

    // Test strategy:
    // --- a. smoke test
    // --- b. loop over relevant range of instance attribute values
    // --- c. empty scenario
    // --- d. empty expected status words

    final var levels = Level.values();
    final var maxLevelIndex = levels.length - 1;

    // --- a. smoke test
    {
      final var dut =
          new Scenario7816(
              List.of(
                  new Scenario7816.LoggingInformation(INFO, "foo 3"),
                  List.of(0x9000, 0x6A83),
                  new CommandApdu("00 a4 04 04 00")));
      final var expected =
          BerTlv.getInstance(
              """
                  30 23
                     02 01 00
                     30 1e
                        31 0a
                           02 01 14
                           0c 05 666f6f2033
                        30 09
                           02 03 009000
                           02 02 6a83
                        04 05 00a4040400
                  """);

      final var actual = dut.toTlv();

      assertEquals(expected.toString(" "), actual.toString(" "));
    } // end --- a.

    // --- b. loop over relevant range of instance attribute values
    IntStream.range(0, 100)
        .forEach(
            i -> {
              final var input = new ArrayList<BerTlv>();
              final var expVersion = RNG.nextInt();
              input.add(new DerInteger(BigInteger.valueOf(expVersion)));
              final var expScenario = new ArrayList<>();
              final var inList = new ArrayList<BerTlv>();

              while (expScenario.size() < i) {
                final var rnd = RNG.nextDouble();
                if (rnd < 0.5) { // NOPMD literal in if statement
                  // ... 50% command APDU
                  final var cmd = IoChallenge.random(0, Long.MAX_VALUE, KIBI, KIBI).getBytes();
                  inList.add(new DerOctetString(cmd));
                  expScenario.add(new CommandApdu(cmd));
                } else if (rnd < 0.7) { // NOPMD literal in if statement
                  // ... 20% expected status words
                  final var esw =
                      IntStream.range(0, RNG.nextIntClosed(0, 10))
                          .map(j -> RNG.nextIntClosed(0, 0xffff))
                          .boxed()
                          .toList();
                  inList.add(
                      new DerSequence(
                          esw.stream().map(j -> new DerInteger(BigInteger.valueOf(j))).toList()));
                  expScenario.add(esw);
                } else {
                  // ... 30% logging information
                  final var logi =
                      new Scenario7816.LoggingInformation(
                          levels[RNG.nextIntClosed(0, maxLevelIndex)], RNG.nextUtf8(0, 32));
                  inList.add(logi.toTlv());
                  expScenario.add(logi);
                } // end else
              } // end While (not enough objects in scenario)
              input.add(new DerSequence(inList));
              final var expected = new DerSequence(input);
              final var dut = new Scenario7816(expected);

              final var actual = dut.toTlv();

              assertEquals(expected.toString(" ", "   "), actual.toString(" ", "   "));
            }); // end forEach(i -> ...)
    // end ---b.

    // --- c. empty scenario
    {
      final var dut = new Scenario7816(Collections.emptyList());
      final var expected = BerTlv.getInstance("30 05   02 01 00   30 00");

      final var actual = dut.toTlv();

      assertEquals(expected.toString(" "), actual.toString(" "));
    } // end --- c.

    // --- d. empty expected status words
    {
      final var dut =
          new Scenario7816(
              List.of(
                  new Scenario7816.LoggingInformation(INFO, "foo 4"),
                  Collections.emptyList(),
                  new CommandApdu("00 a4 04 04 00")));
      final var expected =
          BerTlv.getInstance(
              """
                  30 11
                     02 01 00
                     30 15
                        31 0a
                           02 01 14
                           0c 05 666f6f2034
                        30 00
                        04 05 00a4040400
                  """);

      final var actual = dut.toTlv();

      assertEquals(expected.toString(" "), actual.toString(" "));
    } // end --- d.
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /** Pseudo-APDU layer used for testing. */
  private static final class MyApduLayer implements ApduLayer {

    /** Queue for {@link ResponseApdu}. */
    private final BlockingQueue<ResponseApdu> insResponseApdus = new LinkedBlockingQueue<>(); // */

    /**
     * Returns execution time of previous command-response pair.
     *
     * @return execution time in seconds of previous command-response pair
     */
    @Override
    public double getTime() {
      return 0;
    } // end method */

    /**
     * Sends given message.
     *
     * @param message to be sent, typically a command
     * @return corresponding response message
     */
    @Override
    public byte[] send(final byte[] message) {
      try {
        return insResponseApdus.take().getBytes();
      } catch (InterruptedException e) {
        throw new NoSuchElementException(e);
      } // end Catch (...)
    } // end method */
  } // end inner class
} // end class
