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
package de.gematik.smartcards.tlv;

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class performing white-box test on {@link DerUtcTime}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.,
//         Non-null field is not initialized
//         This finding is for a class attribute which is initialized by JUnit.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" // see note 1
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestDerUtcTime {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Temporary Directory. */
  @TempDir
  /* package */ static Path claTempDir; // NOPMD, NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR */

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

  /** Test method for {@link DerUtcTime#DerUtcTime(ZonedDateTime, DerUtcTime.UtcTimeFormat)}. */
  @Test
  void test_DerUtcTime__ZoneDateTime_UtcTimeFormat() {
    // Assertions:
    // ... a. fromDateTime(...)-method works as expected

    // Note: Because of assertion_a and the simplicity of the
    //       constructor under test, we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    final ZonedDateTime input =
        ZonedDateTime.of(2021, 2, 19, 18, 43, 27, 12_345, DerUtcTime.UTC_TIME_ZONE);
    final DerUtcTime dut = new DerUtcTime(input, DerUtcTime.UtcTimeFormat.HH_MM_Z);
    assertEquals("17 0b 323130323139313834335a", dut.toString(" "));
    assertTrue(dut.isValid());
    assertTrue(dut.insFindings.isEmpty());
    assertNotSame(input, dut.insDecoded);
    assertEquals("2021-02-19T18:43Z", dut.insDecoded.toString());
  } // end method */

  /** Test method for {@link DerUtcTime#DerUtcTime(ByteBuffer)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerUtcTime__ByteBuffer() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with all formats
    // --- b. FINDING: wrong format
    // --- c: ERROR: ArithmeticException
    // --- d. ERROR: BufferUnderflowException

    final var tagField = "17 ";

    // --- a. smoke test with all formats
    // a.1 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.1
    // a.2 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.1
    // a.3 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.2
    // a.4 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.2
    {
      // a.1 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.1
      var input = "0b 323130323139313834345a";
      var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      var dut = new DerUtcTime(buffer);

      assertEquals(tagField + input, dut.toString(" "));
      assertEquals(
          ZonedDateTime.of(2021, 2, 19, 18, 44, 0, 0, DerUtcTime.UTC_TIME_ZONE), dut.insDecoded);

      // a.2 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.1
      input = "0d 3231303231393138343432375a";
      buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      dut = new DerUtcTime(buffer);

      assertEquals(tagField + input, dut.toString(" "));
      assertEquals(
          ZonedDateTime.of(2021, 2, 19, 18, 44, 27, 0, DerUtcTime.UTC_TIME_ZONE), dut.insDecoded);

      // a.3 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.2
      input = "0f 323130323139313834352b30343330";
      buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      dut = new DerUtcTime(buffer);

      assertEquals(tagField + input, dut.toString(" "));
      assertEquals(
          ZonedDateTime.of(2021, 2, 19, 18, 45, 0, 0, ZoneOffset.ofHoursMinutes(4, 30)),
          dut.insDecoded);

      // a.4 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.2
      input = "11 3231303231393138343530392d31303235";
      buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      dut = new DerUtcTime(buffer);

      assertEquals(tagField + input, dut.toString(" "));
      assertEquals(
          ZonedDateTime.of(2021, 2, 19, 18, 45, 9, 0, ZoneOffset.ofHoursMinutes(-10, -25)),
          dut.insDecoded);
    } // end --- a.

    // --- b. FINDING: wrong format
    {
      final var set =
          Set.of(
              "20210213233842Z", // yyyy instead of yy
              "21-02-13 23:38:43Z"); // delimiter
      for (final var input : set) {
        final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
        final String octets =
            BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
        final var buffer = ByteBuffer.wrap(Hex.toByteArray(octets));

        final var dut = new DerUtcTime(buffer);

        assertEquals("17" + octets, dut.toString());
        assertFalse(dut.isValid());
        assertEquals(1, dut.insFindings.size());
        assertEquals(List.of("wrong format"), dut.insFindings);
      } // end For (input...)
    } // end --- b.

    // --- c: ERROR: ArithmeticException
    {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerUtcTime(buffer));

      assertNull(e.getCause());
    } // end --- c.

    // --- d. ERROR: BufferUnderflowException
    for (final var input : Set.of("-00", "-8100", "01-23", "-8102-4567")) {
      final var buffer = ByteBuffer.wrap(Hex.toByteArray(input));

      assertDoesNotThrow(() -> new DerUtcTime(buffer));

      buffer.clear().limit(buffer.limit() - 1);

      assertThrows(BufferUnderflowException.class, () -> new DerUtcTime(buffer));
    } // end For (input...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerUtcTime#DerUtcTime(InputStream)}. */
  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  @Test
  void test_DerUtcTime__InputStream() {
    // Assertions:
    // ... a. constructors from superclasses work as expected
    // ... b. toString()-method works as expected

    // Test strategy:
    // --- a. smoke test with all formats
    // --- b. FINDING: wrong format
    // --- c: ERROR: ArithmeticException
    // --- d. ERROR: IOException

    final var tagField = "17 ";

    try {
      // --- a. smoke test with all formats
      // a.1 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.1
      // a.2 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.1
      // a.3 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.2
      // a.4 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.2
      {
        // a.1 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.1
        var input = "0b 323130323139313834345a";
        var inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        var dut = new DerUtcTime(inputStream);

        assertEquals(tagField + input, dut.toString(" "));
        assertEquals(
            ZonedDateTime.of(2021, 2, 19, 18, 44, 0, 0, DerUtcTime.UTC_TIME_ZONE), dut.insDecoded);

        // a.2 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.1
        input = "0d 3231303231393138343432375a";
        inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        dut = new DerUtcTime(inputStream);

        assertEquals(tagField + input, dut.toString(" "));
        assertEquals(
            ZonedDateTime.of(2021, 2, 19, 18, 44, 27, 0, DerUtcTime.UTC_TIME_ZONE), dut.insDecoded);

        // a.3 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.2
        input = "0f 323130323139313834352b30343330";
        inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        dut = new DerUtcTime(inputStream);

        assertEquals(tagField + input, dut.toString(" "));
        assertEquals(
            ZonedDateTime.of(2021, 2, 19, 18, 45, 0, 0, ZoneOffset.ofHoursMinutes(4, 30)),
            dut.insDecoded);

        // a.4 ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.2
        input = "11 3231303231393138343530392d31303235";
        inputStream = new ByteArrayInputStream(Hex.toByteArray(input));

        dut = new DerUtcTime(inputStream);

        assertEquals(tagField + input, dut.toString(" "));
        assertEquals(
            ZonedDateTime.of(2021, 2, 19, 18, 45, 9, 0, ZoneOffset.ofHoursMinutes(-10, -25)),
            dut.insDecoded);
      } // end --- a.

      // --- b. FINDING: wrong format
      {
        final var set =
            Set.of(
                "20210213233842Z", // yyyy instead of yy
                "21-02-13 23:38:43Z"); // delimiter
        for (final var input : set) {
          final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
          final String octets =
              BerTlv.getLengthField(valueField.length) + Hex.toHexDigits(valueField);
          final var inputStream = new ByteArrayInputStream(Hex.toByteArray(octets));

          final var dut = new DerUtcTime(inputStream);

          assertEquals("17" + octets, dut.toString());
          assertFalse(dut.isValid());
          assertEquals(1, dut.insFindings.size());
          assertEquals(List.of("wrong format"), dut.insFindings);
        } // end For (input...)
      } // end --- b.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)

    // --- c: ERROR: ArithmeticException
    {
      final var inputStream =
          new ByteArrayInputStream(Hex.toByteArray("---89 8000 0000 0000 0000---0102..."));

      final var e = assertThrows(ArithmeticException.class, () -> new DerUtcTime(inputStream));

      assertNull(e.getCause());
    } // end --- c.

    // --- d. ERROR: IOException
    try {
      final var path = claTempDir.resolve("test_readInstance__InputStream-c.bin");
      Files.write(path, Hex.toByteArray("-01-00"));

      try (var is = Files.newInputStream(path)) {
        is.close();

        assertThrows(IOException.class, () -> new DerUtcTime(is));
      } // end try-with-resources
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
    // end --- d.
  } // end method */

  /** Test method for {@link DerUtcTime#fromDateTime(ZonedDateTime, DerUtcTime.UtcTimeFormat)}. */
  @Test
  @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity"})
  void test_fromDateTime__ZoneDateTime_UtcTimeFormat() {
    // Test strategy:
    // --- a. smoke test with manual values
    // --- b. manually chosen values with problems due to switching to daylight-saving time
    // --- c. loop over all known time-zones and some randomly chosen instant values

    // --- a. smoke test with manual values
    for (final var input :
        List.of(
            List.of(
                ZonedDateTime.of(
                    2021, // year
                    2, // month
                    18, // dayOfMonth
                    17, // hour
                    53, // minute
                    49, // second
                    653, // nanoOfSecond
                    ZoneId.of("Europe/Berlin")),
                DerUtcTime.UtcTimeFormat.HH_MM_DIFF,
                "2102181753+0100"),
            List.of(
                ZonedDateTime.of(
                    2021, // year
                    2, // month
                    18, // dayOfMonth
                    17, // hour
                    53, // minute
                    49, // second
                    653, // nanoOfSecond
                    ZoneId.of("Europe/Paris")),
                DerUtcTime.UtcTimeFormat.HH_MM_SS_DIFF,
                "210218175349+0100"),
            List.of(
                ZonedDateTime.of(
                    2021, // year
                    2, // month
                    18, // dayOfMonth
                    17, // hour
                    53, // minute
                    49, // second
                    653, // nanoOfSecond
                    ZoneId.of("Europe/Rome")),
                DerUtcTime.UtcTimeFormat.HH_MM_Z,
                "2102181653Z"),
            List.of(
                ZonedDateTime.of(
                    2021, // year
                    2, // month
                    18, // dayOfMonth
                    17, // hour
                    53, // minute
                    49, // second
                    653, // nanoOfSecond
                    ZoneId.of("Europe/Berlin")),
                DerUtcTime.UtcTimeFormat.HH_MM_SS_Z,
                "210218165349Z"))) {
      final ZonedDateTime dateTime = (ZonedDateTime) input.get(0);
      final DerUtcTime.UtcTimeFormat format = (DerUtcTime.UtcTimeFormat) input.get(1);
      final String expected = (String) input.get(2);
      assertEquals(
          expected,
          DerUtcTime.fromDateTime(dateTime, format),
          () -> dateTime.toString() + ", hour=" + dateTime.getHour());
    } // end For (input...)

    // --- b. manually chosen values with problems due to switching to daylight-saving time
    // Note 1: The hour differs by one in the following tests between input and expected.
    //         Probably the reason is a change in timezone-offset due to switch to
    //         daylight-saving time. In that case (typically) the hour 2 am is skipped.
    Map.ofEntries(
            //                year  MM DD hh  deltaMinutes
            Map.entry(List.of(1989, 3, 25, 22, 60), "America/Danmarkshavn"),
            Map.entry(List.of(1990, 3, 25, 2, 60), "Europe/Bratislava"),
            Map.entry(List.of(1995, 3, 26, 2, 60), "Asia/Oral"),
            Map.entry(List.of(2005, 3, 27, 0, 60), "Atlantic/Azores"),
            Map.entry(List.of(2008, 9, 28, 2, 60), "Antarctica/McMurdo"),
            Map.entry(List.of(2014, 10, 5, 2, 60), "Australia/Adelaide"),
            Map.entry(List.of(2022, 3, 27, 2, 60), "Europe/Ljubljana"),
            Map.entry(List.of(2032, 10, 3, 2, 60), "Australia/Sydney"),
            Map.entry(List.of(2041, 10, 6, 2, 30), "Australia/LHI"),
            Map.entry(List.of(2067, 3, 27, 2, 60), "Europe/Bratislava"),
            Map.entry(List.of(2073, 3, 26, 2, 60), "Europe/Sarajevo"),
            Map.entry(List.of(2076, 10, 4, 2, 60), "Australia/Yancowinna"))
        .forEach(
            (input, zone) -> {
              final ZoneId zoneId = ZoneId.of(zone);
              final int year = input.get(0);
              final int month = input.get(1);
              final int dayOfMonth = input.get(2);
              final int hour = input.get(3);
              final int deltaMinutes = input.get(4);

              if (60 == deltaMinutes) { // NOPMD literal in if statement
                // ... step size is 1 hour when switching to daylight saving time
                // b.1 check date and time values in the hour before skipping
                {
                  final int day;
                  final int hourBefore;
                  if (0 == hour) {
                    day = dayOfMonth - 1;
                    hourBefore = 23;
                  } else {
                    day = dayOfMonth;
                    hourBefore = hour - 1;
                  } // end else
                  RNG.intsClosed(0, 59, 5)
                      .forEach(
                          minute -> {
                            final int second = RNG.nextIntClosed(0, 59);
                            final int nanos = RNG.nextIntClosed(0, 999_999);

                            final ZonedDateTime dateTime =
                                ZonedDateTime.of(
                                    year, month, day, hourBefore, minute, second, nanos, zoneId);
                            final String zoneOffset = dateTime.getOffset().toString();
                            final String offset;
                            if ("Z".equals(zoneOffset)) { // NOPMD literals in if statement
                              offset = "+0000";
                            } else {
                              final String offsetHour = zoneOffset.substring(0, 3);
                              final String offsetMinute = zoneOffset.substring(4, 6);
                              offset = offsetHour + offsetMinute;
                            } // end else

                            final String expected =
                                String.format(
                                    "%02d%02d%02d%02d%02d%s",
                                    year % 100, month, day, hourBefore, minute, offset);

                            final String present =
                                DerUtcTime.fromDateTime(
                                    dateTime, DerUtcTime.UtcTimeFormat.HH_MM_DIFF);

                            assertEquals(
                                expected, present, () -> dateTime + ", hour=" + dateTime.getHour());
                          }); // end forEach(minute -> ...)
                } // end b.1

                // b.2 check date and time values which are skipped
                RNG.intsClosed(0, 59, 5)
                    .forEach(
                        minute -> {
                          final int second = RNG.nextIntClosed(0, 59);
                          final int nanos = RNG.nextIntClosed(0, 999_999);

                          final ZonedDateTime dateTime =
                              ZonedDateTime.of(
                                  year, month, dayOfMonth, hour, minute, second, nanos, zoneId);
                          final String zoneOffset = dateTime.getOffset().toString();
                          final String offset;
                          if ("Z".equals(zoneOffset)) { // NOPMD literals in if statement
                            offset = "+0000";
                          } else {
                            final String offsetHour = zoneOffset.substring(0, 3);
                            final String offsetMinute = zoneOffset.substring(4, 6);
                            offset = offsetHour + offsetMinute;
                          } // end else

                          final String expected =
                              String.format(
                                  "%02d%02d%02d%02d%02d%s",
                                  year % 100, month, dayOfMonth, hour + 1, minute, offset);

                          final String present =
                              DerUtcTime.fromDateTime(
                                  dateTime, DerUtcTime.UtcTimeFormat.HH_MM_DIFF);

                          assertEquals(
                              expected, present, () -> dateTime + ", hour=" + dateTime.getHour());
                        }); // end forEach(minute -> ...)

                // b.3 check date and time values in the hour after skipping
                RNG.intsClosed(0, 59, 5)
                    .forEach(
                        minute -> {
                          final int second = RNG.nextIntClosed(0, 59);
                          final int nanos = RNG.nextIntClosed(0, 999_999);

                          final ZonedDateTime dateTime =
                              ZonedDateTime.of(
                                  year, month, dayOfMonth, hour + 1, minute, second, nanos, zoneId);
                          final String zoneOffset = dateTime.getOffset().toString();
                          final String offset;
                          if ("Z".equals(zoneOffset)) { // NOPMD literals in if statement
                            offset = "+0000";
                          } else {
                            final String offsetHour = zoneOffset.substring(0, 3);
                            final String offsetMinute = zoneOffset.substring(4, 6);
                            offset = offsetHour + offsetMinute;
                          } // end else

                          final String expected =
                              String.format(
                                  "%02d%02d%02d%02d%02d%s",
                                  year % 100, month, dayOfMonth, hour + 1, minute, offset);

                          final String present =
                              DerUtcTime.fromDateTime(
                                  dateTime, DerUtcTime.UtcTimeFormat.HH_MM_DIFF);

                          assertEquals(
                              expected, present, () -> dateTime + ", hour=" + dateTime.getHour());
                        }); // end forEach(minute -> ...)
              } else if (30 == deltaMinutes) { // NOPMD literal in if statement
                // ... step size is 30 minutes when switching to daylight saving time
                // b.1 check date and time values in the hour before skipping
                RNG.intsClosed(0, 59, 5)
                    .forEach(
                        minute -> {
                          final int second = RNG.nextIntClosed(0, 59);
                          final int nanos = RNG.nextIntClosed(0, 999_999);

                          final ZonedDateTime dateTime =
                              ZonedDateTime.of(
                                  year, month, dayOfMonth, hour - 1, minute, second, nanos, zoneId);
                          final String zoneOffset = dateTime.getOffset().toString();
                          final String offset;
                          if ("Z".equals(zoneOffset)) { // NOPMD literals in if statement
                            offset = "+0000";
                          } else {
                            final String offsetHour = zoneOffset.substring(0, 3);
                            final String offsetMinute = zoneOffset.substring(4, 6);
                            offset = offsetHour + offsetMinute;
                          } // end else

                          final String expected =
                              String.format(
                                  "%02d%02d%02d%02d%02d%s",
                                  year % 100, month, dayOfMonth, hour - 1, minute, offset);

                          final String present =
                              DerUtcTime.fromDateTime(
                                  dateTime, DerUtcTime.UtcTimeFormat.HH_MM_DIFF);

                          assertEquals(
                              expected, present, () -> dateTime + ", hour=" + dateTime.getHour());
                        }); // end forEach(minute -> ...)

                // b.2 check date and time values which are skipped
                RNG.intsClosed(0, 29, 5)
                    .forEach(
                        minute -> {
                          final int second = RNG.nextIntClosed(0, 59);
                          final int nanos = RNG.nextIntClosed(0, 999_999);

                          final ZonedDateTime dateTime =
                              ZonedDateTime.of(
                                  year, month, dayOfMonth, hour, minute, second, nanos, zoneId);
                          final String zoneOffset = dateTime.getOffset().toString();
                          final String offset;
                          if ("Z".equals(zoneOffset)) { // NOPMD literals in if statement
                            offset = "+0000";
                          } else {
                            final String offsetHour = zoneOffset.substring(0, 3);
                            final String offsetMinute = zoneOffset.substring(4, 6);
                            offset = offsetHour + offsetMinute;
                          } // end else

                          final String expected =
                              String.format(
                                  "%02d%02d%02d%02d%02d%s",
                                  year % 100, month, dayOfMonth, hour, minute + 30, offset);

                          final String present =
                              DerUtcTime.fromDateTime(
                                  dateTime, DerUtcTime.UtcTimeFormat.HH_MM_DIFF);

                          assertEquals(
                              expected, present, () -> dateTime + ", hour=" + dateTime.getHour());
                        }); // end forEach(minute -> ...)

                // b.3 check date and time values in the hour after skipping
                RNG.intsClosed(0, 59, 5)
                    .forEach(
                        minute -> {
                          final int second = RNG.nextIntClosed(0, 59);
                          final int nanos = RNG.nextIntClosed(0, 999_999);

                          final ZonedDateTime dateTime =
                              ZonedDateTime.of(
                                  year, month, dayOfMonth, hour + 1, minute, second, nanos, zoneId);
                          final String zoneOffset = dateTime.getOffset().toString();
                          final String offset;
                          if ("Z".equals(zoneOffset)) { // NOPMD literals in if statement
                            offset = "+0000";
                          } else {
                            final String offsetHour = zoneOffset.substring(0, 3);
                            final String offsetMinute = zoneOffset.substring(4, 6);
                            offset = offsetHour + offsetMinute;
                          } // end else

                          final String expected =
                              String.format(
                                  "%02d%02d%02d%02d%02d%s",
                                  year % 100, month, dayOfMonth, hour + 1, minute, offset);

                          final String present =
                              DerUtcTime.fromDateTime(
                                  dateTime, DerUtcTime.UtcTimeFormat.HH_MM_DIFF);

                          assertEquals(
                              expected, present, () -> dateTime + ", hour=" + dateTime.getHour());
                        }); // end forEach(minute -> ...)

                // b.4 check date and time values in the 30 minutes after skipping
                RNG.intsClosed(30, 59, 5)
                    .forEach(
                        minute -> {
                          final int second = RNG.nextIntClosed(0, 59);
                          final int nanos = RNG.nextIntClosed(0, 999_999);

                          final ZonedDateTime dateTime =
                              ZonedDateTime.of(
                                  year, month, dayOfMonth, hour, minute, second, nanos, zoneId);
                          final String zoneOffset = dateTime.getOffset().toString();
                          final String offset;
                          if ("Z".equals(zoneOffset)) { // NOPMD literals in if statement
                            offset = "+0000";
                          } else {
                            final String offsetHour = zoneOffset.substring(0, 3);
                            final String offsetMinute = zoneOffset.substring(4, 6);
                            offset = offsetHour + offsetMinute;
                          } // end else

                          final String expected =
                              String.format(
                                  "%02d%02d%02d%02d%02d%s",
                                  year % 100, month, dayOfMonth, hour, minute, offset);

                          final String present =
                              DerUtcTime.fromDateTime(
                                  dateTime, DerUtcTime.UtcTimeFormat.HH_MM_DIFF);

                          assertEquals(
                              expected, present, () -> dateTime + ", hour=" + dateTime.getHour());
                        }); // end forEach(minute -> ...)

              } else {
                fail("unexpected program flow");
              } // end else(deltaMinutes...)
            }); // end forEach(input -> ...)

    /*/ --- c. loop over all known time-zones and some randomly chosen instant values
    // Note: Date and time here are chosen randomly. Sometimes tests fail.
    //       Typically, this is the case when daylight saving time changes, i.e.,
    //       End of March in Europe or in October in Australia.
    //       If such a test fails: Don't worry and
    //       a. add that particular random value to --- b. above.
    //       b. hope that during the following test-run not again problematic
    //          date and time combination occurs.
    final Set<String> zoneIds = ZoneId.getAvailableZoneIds();
    zoneIds.add("Z");
    zoneIds.forEach(
        zoneId -> {
          final ZoneId zone = ZoneId.of(zoneId);
          RNG.intsClosed(1970, 2100, 20)
              .forEach(
                  year -> {
                    final int month = RNG.nextIntClosed(1, 12);
                    final int dayOfMonth = RNG.nextIntClosed(1, 28);
                    final int hour = RNG.nextIntClosed(0, 23);
                    final int minute = RNG.nextIntClosed(0, 59);
                    final int second = RNG.nextIntClosed(0, 59);
                    final int nanos = RNG.nextIntClosed(0, 999_999_999);

                    final ZonedDateTime input =
                        ZonedDateTime.of(
                            year, month, dayOfMonth, hour, minute, second, nanos, zone);
                    final ZonedDateTime inUtc =
                        ZonedDateTime.of(
                            year,
                            month,
                            dayOfMonth,
                            hour,
                            minute,
                            second,
                            nanos,
                            DerUtcTime.UTC_TIME_ZONE);

                    // ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.1
                    assertEquals(
                        String.format(
                            "%02d%02d%02d%02d%02dZ", year % 100, month, dayOfMonth, hour, minute),
                        DerUtcTime.fromDateTime(inUtc, DerUtcTime.UtcTimeFormat.HH_MM_Z));

                    // ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.1
                    assertEquals(
                        String.format(
                            "%02d%02d%02d%02d%02d%02dZ",
                            year % 100, month, dayOfMonth, hour, minute, second),
                        DerUtcTime.fromDateTime(inUtc, DerUtcTime.UtcTimeFormat.HH_MM_SS_Z));

                    // ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.1, c.2
                    final String zoneOffset = input.getOffset().toString();
                    final String offset;
                    if ("Z".equals(zoneOffset)) { // NOPMD literals in if statement
                      offset = "+0000";
                    } else {
                      final String offsetHour = zoneOffset.substring(0, 3);
                      final String offsetMinute = zoneOffset.substring(4, 6);
                      offset = offsetHour + offsetMinute;
                    } // end fi

                    // Note: Calculate offset due to switching to daylight saving time,
                    //       where no 2nd hour exists, see test section --- b above.
                    final int dstOffset =
                        ((hour <= 2) && (hour != input.getHour())) ? (input.getHour() - hour) : 0;
                    assertEquals(
                        String.format(
                            "%02d%02d%02d%02d%02d%s",
                            year % 100, month, dayOfMonth, hour + dstOffset, minute, offset),
                        DerUtcTime.fromDateTime(input, DerUtcTime.UtcTimeFormat.HH_MM_DIFF),
                        () -> input + ", hour=" + input.getHour());

                    // ISO/IEC 8824-1:2021, clause 47.3, §1 a, b.2, c.2
                    assertEquals(
                        String.format(
                            "%02d%02d%02d%02d%02d%02d%s",
                            year % 100,
                            month,
                            dayOfMonth,
                            hour + dstOffset,
                            minute,
                            second,
                            offset),
                        DerUtcTime.fromDateTime(input, DerUtcTime.UtcTimeFormat.HH_MM_SS_DIFF),
                        input::toString);
                  }); // end forEach(year -> ...)
        }); // end forEach(zoneId -> ...) */
  } // end method */

  /** Test method for {@link DerUtcTime#getComment()}. */
  @Test
  void test_getComment() {
    // Test strategy:
    // --- a. smoke test without findings
    // --- b. smoke test with findings

    // --- a. smoke test without findings
    // --- b. smoke test with findings
    for (final var entry :
        Map.ofEntries(
                Map.entry(
                    "2102132338Z", // no findings, Zulu, no seconds
                    " # UTCTime := 2021-02-13T23:38Z"),
                Map.entry(
                    "210213233843Z", // no findings, Zulu, with seconds
                    " # UTCTime := 2021-02-13T23:38:43Z"),
                Map.entry(
                    "2102132338+0315", // no findings, positive offset, no seconds
                    " # UTCTime := 2021-02-13T23:38+03:15"),
                Map.entry(
                    "210213233843-1045", // no findings, negative offset, with seconds
                    " # UTCTime := 2021-02-13T23:38:43-10:45"),
                Map.entry(
                    "20210213233843Z", // with findings
                    " # UTCTime, findings: wrong format, value-field as UTF-8: 20210213233843Z"))
            .entrySet()) {
      final var input = entry.getKey();
      final var expected = entry.getValue();
      final byte[] valueField = input.getBytes(StandardCharsets.US_ASCII);
      final String octets =
          String.format("%02x", DerUtcTime.TAG)
              + BerTlv.getLengthField(valueField.length)
              + Hex.toHexDigits(valueField);
      final BerTlv dutGen = BerTlv.getInstance(octets);
      assertEquals(DerUtcTime.class, dutGen.getClass());
      final DerUtcTime dut = (DerUtcTime) dutGen;
      assertEquals(octets, dut.toString());

      final var actual = dut.getComment();

      assertEquals(expected, actual);
    } // end For (entry...)
  } // end method */

  /** Test method for {@link DerUtcTime#getDecoded()}. */
  @Test
  void test_getDecoded() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with value constructor
    // --- b. smoke test with InputStream constructor

    // --- a. smoke test with value constructor
    {
      final ZonedDateTime input =
          ZonedDateTime.of(2021, 2, 19, 18, 48, 27, 12_345, DerUtcTime.UTC_TIME_ZONE);
      final DerUtcTime dut = new DerUtcTime(input, DerUtcTime.UtcTimeFormat.HH_MM_Z);
      assertEquals("17 0b 323130323139313834385a", dut.toString(" "));
      assertTrue(dut.isValid());
      assertTrue(dut.insFindings.isEmpty());
      assertNotSame(input, dut.insDecoded);
      assertEquals("2021-02-19T18:48Z", dut.getDecoded().toString());
    }

    // --- b. smoke test with InputStream constructor
    {
      final BerTlv dutGen =
          BerTlv.getInstance(
              new ByteArrayInputStream(Hex.toByteArray("17 0b 323130323139313834315a")));
      assertEquals(DerUtcTime.class, dutGen.getClass());
      final DerUtcTime dut = (DerUtcTime) dutGen;
      assertEquals(
          ZonedDateTime.of(2021, 2, 19, 18, 41, 0, 0, DerUtcTime.UTC_TIME_ZONE), dut.getDecoded());
    }
  } // end method */
} // end class
