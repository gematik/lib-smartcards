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

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Class performing white-box tests for {@link AfiOid}. */
// Note 1: Spotbugs claims "NP_LOAD_OF_KNOWN_NULL_VALUE", i.e.:
//         Load of known null value.
//         That finding is correct, because intentionally the equals(...)-method is tested with
//         a parameter being null.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_LOAD_OF_KNOWN_NULL_VALUE", // see note 1
}) // */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle:methodname"
})
final class TestAfiOid {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Method executed before other tests. */
  @BeforeAll
  static void setUpBeforeClass() {
    // Test strategy:
    // --- a. assure that elements in PREDEFINED are unique
    // --- b. assure that names of predefined OID are unique
    // --- c. assure that point-notations of predefined values are unique

    final int size = AfiOid.PREDEFINED.size();

    // --- a. assure that elements in PREDEFINED are unique
    final Set<AfiOid> set = new HashSet<>();
    AfiOid.PREDEFINED.forEach(
        oid -> {
          assertFalse(set.contains(oid), oid::getName);

          set.add(oid);
        }); // end forEach(oid -> ...)

    // --- b. assure that names of predefined OID are unique
    assertEquals(
        size, AfiOid.PREDEFINED.stream().map(AfiOid::getName).collect(Collectors.toSet()).size());

    // --- c. assure that point-notations of predefined values are unique
    assertEquals(
        size, AfiOid.PREDEFINED.stream().map(AfiOid::getPoint).collect(Collectors.toSet()).size());
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

  /** Test method for {@link AfiOid#AfiOid(byte[])}. */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  @Test
  void test_AfiOid__byteA() {
    // Assertions:
    // ... a. "os2Components(byte[])"-method works as expected

    // Note: This constructor is rather simple and underlying methods are tested elsewhere,
    //       so we can be lazy here and concentrate on good test-coverage.

    // Test strategy:
    // --- a. smoke test with pre-defined OID
    // --- b. smoke test with arbitrary OID
    // --- c. ERROR: IllegalArgumentException

    // --- a. smoke test with pre-defined OID
    {
      final var dut = AfiOid.ansix9p256r1;

      assertEquals(dut, new AfiOid(Hex.toByteArray(dut.getOctetString())));
    } // end --- a.

    // --- b. smoke test with arbitrary OID
    {
      final var dut = new AfiOid(Hex.toByteArray("2b400300"));

      assertEquals("1.3.64.3.0", dut.getPoint());
      assertEquals(dut.getPoint(), dut.getName());
    } // end --- b.

    // --- c. ERROR: IllegalArgumentException
    {
      for (final var input : List.of("", "ff", "2a80")) {
        final var octets = Hex.toByteArray(input);

        assertThrows(IllegalArgumentException.class, () -> new AfiOid(octets));
      } // end For (input...)
    } // end --- c.
  } // end method */

  /** Test method for {@link AfiOid#AfiOid(int...)}. */
  @Test
  void test_AfiOid__intA() {
    // Note: This constructor is rather simple and underlying methods are tested elsewhere,
    //       so we can be lazy here and concentrate on good test-coverage.

    // Test strategy:
    // --- a. smoke test with pre-defined OID
    // --- b. smoke test with arbitrary OID

    AfiOid dut;

    // --- a. smoke test with pre-defined OID
    dut = AfiOid.ansix9p384r1;
    assertEquals(
        dut, new AfiOid(AfiOid.asn2Components(dut.getAsn1()).stream().mapToInt(i -> i).toArray()));

    // --- b. smoke test with arbitrary OID
    dut = new AfiOid(1, 3, 64, 3, 1);
    assertEquals("1.3.64.3.1", dut.getPoint());
    assertEquals(dut.getPoint(), dut.getName());
  } // end method */

  /** Test method for {@link AfiOid#AfiOid(Collection)}. */
  @Test
  void test_AfiOid__ListI() {
    // Note: This constructor is quite simple and underlying methods are tested elsewhere,
    //       so we can be lazy here and concentrate on good test-coverage.

    // Test strategy:
    // --- a. smoke test with pre-defined OID
    // --- b. smoke test with arbitrary OID

    // --- a. smoke test with pre-defined OID
    AfiOid.PREDEFINED.forEach(
        oid -> {
          final AfiOid dut = new AfiOid(AfiOid.asn2Components(oid.getAsn1()));
          assertEquals(oid, dut);
          assertEquals(oid.getAsn1(), dut.getAsn1());
          assertEquals(oid.getOctetString(), dut.getOctetString());
          assertEquals(oid.getPoint(), dut.getPoint());
          assertEquals(oid.getName(), dut.getName());
        }); // end forEach(oid -> ...)

    // --- b. smoke test with arbitrary OID
    final AfiOid dut = new AfiOid(List.of(1, 3, 64, 3, 8));
    assertEquals("{1 3 64 3 8}", dut.getAsn1());
    assertEquals("2b400308", dut.getOctetString());
    assertEquals("1.3.64.3.8", dut.getPoint());
    assertEquals(dut.getPoint(), dut.getName());
  } // end method */

  /** Test method for {@link AfiOid#components2Asn1(Collection)}. */
  @Test
  void test_components2Asn1__ListI() {
    // Note: Simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    assertEquals("{}", AfiOid.components2Asn1(List.of()));
    assertEquals("{0}", AfiOid.components2Asn1(List.of(0)));
    assertEquals("{5 200}", AfiOid.components2Asn1(List.of(5, 200)));
  } // end method */

  /** Test method for {@link AfiOid#components2Os(int...)}. */
  @Test
  void test_components2Os_intA() {
    // Assertion:
    // ... a. components2Os(List) works as expected

    // Note: The simple method doesn't need extensive testing, so we can be lazy here
    //       and concentrate an good test-coverage.

    // Test strategy:
    // --- a. smoke tests
    assertEquals("00", AfiOid.components2Os(0, 0));
    assertEquals("813403", AfiOid.components2Os(2, 100, 3));
  } // end method */

  /** Test method for {@link AfiOid#components2Os(Collection)}. */
  @Test
  //
  void test_components2Os__ListI() {
    // Assertions:
    // ... a. writeSubId(ByteBuffer, long)-method works as expected
    // ... b. readSubId(ByteBuffer)-method works as expected

    // Test strategy:
    // --- a. less than two components
    // --- b. negative components
    // --- c. first  component out of range
    // --- d. second component out of range
    // --- e. some manually chosen cases
    // --- f. some random cases

    final List<Integer> input = new ArrayList<>();
    Throwable thrown;

    // --- a. less than two components
    // a.0 empty input
    // assertEquals(0, input.size()); // input.size() is always 0 here
    thrown = assertThrows(IllegalArgumentException.class, () -> AfiOid.components2Os(input));
    assertEquals("less than two object identifier components", thrown.getMessage());
    assertNull(thrown.getCause());

    // a.1 one element
    input.add(0);
    assertEquals(1, input.size());
    thrown = assertThrows(IllegalArgumentException.class, () -> AfiOid.components2Os(input));
    assertEquals("less than two object identifier components", thrown.getMessage());
    assertNull(thrown.getCause());

    // --- b. negative components
    input.add(-1);
    assertEquals(2, input.size());
    thrown = assertThrows(IllegalArgumentException.class, () -> AfiOid.components2Os(input));
    assertEquals("at least one component is negative", thrown.getMessage());
    assertNull(thrown.getCause());

    // --- c. first  component out of range
    final var inputC = List.of(3, 0);
    thrown = assertThrows(IllegalArgumentException.class, () -> AfiOid.components2Os(inputC));
    assertEquals("first component greater than 2", thrown.getMessage());
    assertNull(thrown.getCause());

    // --- d. second component out of range
    List.of(0, 1) // valid values for 1st component
        .forEach(
            component1 -> {
              final var in = List.of(component1, 40);
              final var e =
                  assertThrows(IllegalArgumentException.class, () -> AfiOid.components2Os(in));
              assertEquals(
                  "first component in range [0, 1], but second component greater than 39",
                  e.getMessage());
            }); // end forEach(component1 -> ...)

    // --- e. some manually chosen cases
    // e.1 example from ISO/IEC 8825-1:2015 clause 8.19.5
    assertEquals("813403", AfiOid.components2Os(List.of(2, 100, 3)));

    // --- f. some random cases
    // loop over various lengths of list
    RNG.intsClosed(2, 100, 50)
        .forEach(
            listLength -> {
              // fill list
              final List<Integer> inputF = new ArrayList<>();
              final int firstComponent = RNG.nextIntClosed(0, 2);
              final int secondComponent =
                  RNG.nextIntClosed(
                      0, // infimum of random value
                      (2 == firstComponent) ? 0x1_0000 : 39 // supremum
                      );
              inputF.add(firstComponent);
              inputF.add(secondComponent);

              while (inputF.size() < listLength) {
                inputF.add(RNG.nextIntClosed(0, 0x1_0000));
              } // end While (...)
              // ... input has desired length

              // convert output of method-under-test to a buffer and read subidentifier from that
              // buffer
              final String pre = AfiOid.components2Os(inputF);
              final ByteBuffer buffer = ByteBuffer.wrap(Hex.toByteArray(pre));
              final List<Long> subId = new ArrayList<>();
              while (buffer.hasRemaining()) {
                subId.add(AfiOid.readSubId(buffer));
              } // end While (...)

              // check
              assertEquals(subId.size() + 1, listLength);
              final long sub1 = subId.getFirst();
              final int xComponent = (sub1 >= 80) ? 2 : (int) (sub1 / 40);
              final int yComponent = (int) (sub1 - 40L * xComponent);
              assertEquals(xComponent, inputF.get(0));
              assertEquals(yComponent, inputF.get(1));
              for (int i = subId.size(); i-- > 1; ) { // NOPMD avoid assignments in operands
                assertEquals(subId.get(i), (long) inputF.get(i + 1));
              } // end For (i...)
            }); // end forEach(listLength -> ...)
  } // end method */

  /** Test method for {@link AfiOid#components2Point(Collection)}. */
  @Test
  void test_components2Point__ListI() {
    // Note: Simple method doesn't need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. smoke test
    assertEquals("", AfiOid.components2Point(List.of()));
    assertEquals("0", AfiOid.components2Point(List.of(0)));
    assertEquals("5.200", AfiOid.components2Point(List.of(5, 200)));
  } // end method */

  /** Test method for {@link AfiOid#asn2Components(String)}. */
  @Test
  void test_asn2Components__String() {
    // Assertions:
    // ... a. string2Components(String, char)-method works as expected

    // Note: Because of the assertion this method is rather simple, so
    //       we can be lazy here and concentrate on good test-coverage.

    // Test strategy:
    // --- a. smoke test
    // --- b. invalid prefix
    // --- c. invalid suffix

    // --- a. smoke test
    assertEquals(List.of(1, 2), AfiOid.asn2Components("{1 2}"));

    // --- b. invalid prefix
    final Throwable thrownB =
        assertThrows(IllegalArgumentException.class, () -> AfiOid.asn2Components(" 1 2}"));
    assertEquals("input doesn't start with '{'-character", thrownB.getMessage());
    assertNull(thrownB.getCause());

    // --- c. invalid suffix
    final Throwable thrownC =
        assertThrows(IllegalArgumentException.class, () -> AfiOid.asn2Components("{1 2"));
    assertEquals("input doesn't end with '}'-character", thrownC.getMessage());
    assertNull(thrownC.getCause());
  } // end method */

  /** Test method for {@link AfiOid#os2Components(byte[])}. */
  @Test
  void test_os2Components__byteA() {
    // Assertions:
    // ... a. components2Os(List)-method works as expected
    // ... b. readSubId()-method works as expected

    // Note: From the assertions it follows that we can concentrate on happy cases and concentrate
    //       whether method-under-test is the inverse function to components2Os(List).

    // Test strategy:
    // --- a. some manually chosen cases
    // --- b. some random cases
    // --- c. ERROR: IllegalArgumentException
    // --- d. ERROR: ArithmeticException

    // --- a. some manually chosen cases
    assertEquals(List.of(1, 2), AfiOid.os2Components(Hex.toByteArray("2a")));
    assertEquals(List.of(2, 100, 3), AfiOid.os2Components(Hex.toByteArray("813403")));

    // --- b. some random cases
    // loop over various lengths of list
    RNG.intsClosed(2, 100, 50)
        .forEach(
            listLength -> {
              // fill list
              final List<Integer> inputB = new ArrayList<>();
              final int firstComponent = RNG.nextIntClosed(0, 2);
              final int secondComponent =
                  RNG.nextIntClosed(
                      0, // infimum of random value
                      (2 == firstComponent) ? 0x1_0000 : 39 // supremum
                      );
              inputB.add(firstComponent);
              inputB.add(secondComponent);

              while (inputB.size() < listLength) {
                inputB.add(RNG.nextIntClosed(0, 0x1_0000));
              } // end While (...)
              // ... input has desired length

              assertEquals(
                  inputB, AfiOid.os2Components(Hex.toByteArray(AfiOid.components2Os(inputB))));
            }); // end forEach(listLength -> ...)

    // --- c. IllegalArgumentException
    // c.1 second component == Integer.MAX_VALUE
    assertEquals(
        List.of(2, Integer.MAX_VALUE, 3),
        AfiOid.os2Components(Hex.toByteArray("88 80 80 80 4f  03")));

    // c.2 second component > Integer.MAX_VALUE
    final var inputC2 = Hex.toByteArray("88 80 80 80 50  03");
    final Throwable thrownC2 =
        assertThrows(IllegalArgumentException.class, () -> AfiOid.os2Components(inputC2));
    assertEquals("secondComponent too big for this implementation", thrownC2.getMessage());
    assertNull(thrownC2.getCause());

    // c.3 third component == Integer.MAX_VALUE
    // 111 1111111 1111111 1111111 1111111
    assertEquals(
        List.of(0, 0, Integer.MAX_VALUE),
        AfiOid.os2Components(Hex.toByteArray("00   87 ff ff ff 7f")));

    // c.4 third component > Integer.MAX_VALUE
    final var inputC4 = Hex.toByteArray("00   88 80 80 80 00");
    final Throwable thrownC4 =
        assertThrows(IllegalArgumentException.class, () -> AfiOid.os2Components(inputC4));
    assertEquals("component too big for this implementation", thrownC4.getMessage());
    assertNull(thrownC4.getCause());

    // c.5 short input
    for (final var input : List.of("", "ff", "2a80")) {
      final var b = Hex.toByteArray(input);

      final var e = assertThrows(IllegalArgumentException.class, () -> AfiOid.os2Components(b));

      assertEquals(UNEXPECTED, e.getMessage());
      assertEquals(BufferUnderflowException.class, e.getCause().getClass());
    } // end For (input...)

    // --- d. ERROR: ArithmeticException
    // d.1 third component == Long.MAX_VALUE
    // d.2 third component > Long.MAX_VALUE
    {
      // d.1 third component == Long.MAX_VALUE
      // Long.MAX_VALUE = 1111111 1111111 1111111 1111111 1111111 1111111 1111111 1111111 1111111
      final var inputD1 = Hex.toByteArray("2a ffff ffff ffff ffff 7f");

      assertThrows(IllegalArgumentException.class, () -> AfiOid.os2Components(inputD1));

      // d.2 third component > Long.MAX_VALUE
      final var inputD2 = Hex.toByteArray("2a 8180 8080 8080 8080 8000");

      assertThrows(ArithmeticException.class, () -> AfiOid.os2Components(inputD2));
    } // end --- d.
  } // end method */

  /** Test method for {@link AfiOid#point2Components(String)}. */
  @Test
  void test_point2Components__String() {
    // Assertions:
    // ... a. string2Components(String, char)-method works as expected

    // Note: Because of the assertion this method is rather simple, so
    //       we can be lazy here and concentrate on good test-coverage.

    // Test strategy:
    // --- a. smoke test
    assertEquals(List.of(1, 2), AfiOid.point2Components("1.2"));
  } // end method */

  /** Test method for serialization and deserialization. */
  @Test
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.NPathComplexity"})
  void test_serialize() {
    // Test strategy:
    // --- a. predefined OID
    // --- b. arbitrary OID

    // --- a. predefined OID
    for (final var dut : AfiOid.PREDEFINED) {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
        oos.writeObject(dut);
      } catch (IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)

      final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
      try (ObjectInputStream ois = new ObjectInputStream(bais)) {
        final Object obj = ois.readObject();
        assertEquals(dut, obj);
      } catch (ClassNotFoundException | IOException e) {
        fail(UNEXPECTED, e);
      } // end Catch (...)
    } // end For (dut...)
    // end --- a.

    // --- b. arbitrary OID
    RNG.intsClosed(2, 100, 50)
        .forEach(
            listLength -> {
              // fill list
              final List<Integer> input = new ArrayList<>();
              final int firstComponent = RNG.nextIntClosed(0, 2);
              final int secondComponent =
                  RNG.nextIntClosed(
                      0, // infimum of random value
                      (2 == firstComponent) ? 0x1_0000 : 39 // supremum
                      );
              input.add(firstComponent);
              input.add(secondComponent);

              while (input.size() < listLength) {
                input.add(RNG.nextIntClosed(0, 0x1_0000));
              } // end While (...)
              // ... input has desired length

              final AfiOid dut = new AfiOid(input);

              final ByteArrayOutputStream baos = new ByteArrayOutputStream();
              try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(dut);
              } catch (IOException e) {
                fail(UNEXPECTED, e);
              } // end Catch (...)

              final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
              try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                final Object obj = ois.readObject();
                assertEquals(dut, obj);
              } catch (ClassNotFoundException | IOException e) {
                fail(UNEXPECTED, e);
              } // end Catch (...)
            }); // end forEach(listLength -> ...)
  } // end method */

  /** Test method for {@link AfiOid#string2Components(String, char)}. */
  @Test
  void test_string2Components__String_char() {
    // Test strategy:
    // --- a. smoke test for invalid values
    // --- b. smoke test for valid values
    // --- c. loop over various delimiter
    // --- d. loop over various list length

    // --- a. smoke test for invalid values
    // a.1 NumberFormatException
    List.of(
            "", // empty string
            "1..2", // a delimiter directly after another delimiter
            ".2.3", // delimiter at start
            "2.3.", // delimiter at end
            "1.12a34.3", // non-decimal characters
            "1.12 3" // two different values for delimiter
            )
        .forEach(
            input ->
                assertThrows(
                    NumberFormatException.class,
                    () -> AfiOid.string2Components(input, '.'))); // end forEach(input -> ...)

    // a.2 ArithmeticException
    List.of(
            "2." + (Integer.MIN_VALUE - 1L) + ".2", // supremum of invalid negative values
            "2." + (Integer.MAX_VALUE + 1L) + ".2" // infimum  of invalid negative values
            )
        .forEach(
            input ->
                assertThrows(
                    ArithmeticException.class,
                    () -> AfiOid.string2Components(input, '.'))); // end forEach(input -> ...)

    // a.3 IllegalArgumentException
    List.of(
            "-1.2", // first component negative
            "1.-2", // second component negative
            "1.2.-3", // third component negative
            "1.-2.3" // component in the middle negative
            )
        .forEach(
            input -> {
              final Throwable thrownA3 =
                  assertThrows(
                      IllegalArgumentException.class, () -> AfiOid.string2Components(input, '.'));
              assertEquals("at least one component is negative", thrownA3.getMessage());
              assertNull(thrownA3.getCause());
            }); // end forEach(input -> ...)

    // --- b. smoke test for valid values
    assertEquals(List.of(0, 0), AfiOid.string2Components("0.0", '.'));
    assertEquals(List.of(1, 2, 3), AfiOid.string2Components("1.2.3", '.'));
    assertEquals(List.of(1, 2, 3, 4, 5), AfiOid.string2Components("1.2.3.4.5", '.'));
    assertEquals(
        List.of(2, Integer.MAX_VALUE), AfiOid.string2Components("2." + Integer.MAX_VALUE, '.'));

    // --- c. loop over various delimiter
    Stream.of(
            '.', // delimiter of point-notation
            ' ', // delimiter of ASN.1-notation
            '-' // arbitrary delimiter
            )
        .forEach(
            delimiter -> {
              // --- d. loop over various list length
              RNG.intsClosed(1, 20, 7)
                  .forEach(
                      size -> {
                        final List<Integer> list = new ArrayList<>();

                        while (list.size() < size) {
                          list.add(RNG.nextIntClosed(0, 65_536));
                        } // end While

                        final String input =
                            list.stream()
                                .map(i -> Integer.toString(i)) // NOPMD use lambda expression
                                .collect(Collectors.joining(String.valueOf(delimiter)));
                        assertEquals(list, AfiOid.string2Components(input, delimiter));
                      }); // end forEach(size -> ...)
            }); // end forEach(delimiter -> ...)
  } // end method */

  /** Test method for {@link AfiOid#readSubId(ByteBuffer)}. */
  @Test
  @SuppressWarnings({"PMD.NcssCount"})
  void test_readSubId_ByteBuffer() {
    // Assertions:
    // ... a. writeSubId()-method works as expected => we can use writeSubId()-method
    //        to fill the buffer and read those values back during test

    // Test strategy:
    // --- a. smoke test with manually chosen values with one or more than one value in buffer
    // --- b. invalid buffer content
    // --- c. test range [0, 0x1_0000] (that range seems larger than any value from real world
    // --- d. test some random values greater then 0x1_0000

    // --- a. smoke test with manually chosen values with one or more than one value in buffer
    final byte[] byteA = new byte[10];
    final ByteBuffer bufA = ByteBuffer.wrap(byteA);

    // a.1 write a bunch of values to buffer and retrieve them later
    List.of(0L, 1L, 127L, 128L, 255L, 256L, 42L) // fill the buffer
        .forEach(i -> AfiOid.writeSubId(bufA, i));

    bufA.flip(); // flip the buffer, i.e. prepare it for reading
    assertEquals(0, bufA.position());
    assertEquals(10, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(0L, AfiOid.readSubId(bufA));
    assertEquals(1, bufA.position());
    assertEquals(9, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(1L, AfiOid.readSubId(bufA));
    assertEquals(2, bufA.position());
    assertEquals(8, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(127L, AfiOid.readSubId(bufA));
    assertEquals(3, bufA.position());
    assertEquals(7, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(128L, AfiOid.readSubId(bufA));
    assertEquals(5, bufA.position());
    assertEquals(5, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(255L, AfiOid.readSubId(bufA));
    assertEquals(7, bufA.position());
    assertEquals(3, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(256, AfiOid.readSubId(bufA));
    assertEquals(9, bufA.position());
    assertEquals(1, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(42L, AfiOid.readSubId(bufA));
    assertEquals(10, bufA.position());
    assertEquals(0, bufA.remaining());
    assertEquals(10, bufA.limit());

    // a.2 write more values to buffer and retrieve them later
    bufA.clear();
    List.of(
            0x3fffL, // supremum of two   byte encoding
            0x4000L, // infimum  of three byte encoding
            0x7_ffff_ffffL // supremum writeSubId(long) supports
            )
        .forEach(i -> AfiOid.writeSubId(bufA, i));

    bufA.flip(); // flip the buffer, i.e. prepare it for reading
    assertEquals(0, bufA.position());
    assertEquals(10, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(0x3fffL, AfiOid.readSubId(bufA));
    assertEquals(2, bufA.position());
    assertEquals(8, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(0x4000L, AfiOid.readSubId(bufA));
    assertEquals(5, bufA.position());
    assertEquals(5, bufA.remaining());
    assertEquals(10, bufA.limit());

    assertEquals(0x7_ffff_ffffL, AfiOid.readSubId(bufA));
    assertEquals(10, bufA.position());
    assertEquals(0, bufA.remaining());
    assertEquals(10, bufA.limit());

    // a.3 supremum readSubId()-method supports
    bufA.clear();
    bufA.put(Hex.toByteArray("ffffffffffffffff7f"));
    bufA.flip(); // flip the buffer, i.e. prepare it for reading
    assertEquals(0, bufA.position());
    assertEquals(9, bufA.remaining());
    assertEquals(9, bufA.limit());
    assertEquals(Long.MAX_VALUE, AfiOid.readSubId(bufA));

    // --- b. invalid buffer content
    // b.1 subidentifier in buffer is greater than Long.MAX_VALUE
    bufA.clear();
    bufA.put(Hex.toByteArray("81808080808080808000"));
    bufA.flip(); // flip the buffer, i.e. prepare it for reading
    assertEquals(0, bufA.position());
    assertEquals(10, bufA.remaining());
    assertEquals(10, bufA.limit());
    assertThrows(ArithmeticException.class, () -> AfiOid.readSubId(bufA));

    // b.2 end of subidentifier missing, buffer only partially filled
    bufA.clear();
    bufA.put(Hex.toByteArray("80"));
    bufA.flip(); // flip the buffer, i.e. prepare it for reading
    assertEquals(0, bufA.position());
    assertEquals(1, bufA.remaining());
    assertEquals(1, bufA.limit());
    assertThrows(BufferUnderflowException.class, () -> AfiOid.readSubId(bufA));

    // b.3 end of subidentifier missing, buffer completely filled
    bufA.clear();
    bufA.put(Hex.toByteArray("ffffffffffffffff7f   81"));
    bufA.flip(); // flip the buffer, i.e. prepare it for reading
    assertEquals(Long.MAX_VALUE, AfiOid.readSubId(bufA));
    assertEquals(9, bufA.position());
    assertEquals(1, bufA.remaining());
    assertEquals(10, bufA.limit());
    assertThrows(BufferUnderflowException.class, () -> AfiOid.readSubId(bufA));

    // --- c. test range [0, 0x1_0000] (that range seems larger than any value from real world
    final ByteBuffer bufC =
        ByteBuffer.allocate(
            10 // safety margin to prevent BufferOverflowException
                + 0x80 // = 1 * (0x80 - 0), range [0, 127] => one byte encodings
                + 2 * (0x4000 - 0x80) // range [128, 0x3fff] => two byte encodings
                + 3 * (0x1_0000 - 0x4000));
    final int infimum = 0;
    final int supremum = 0x1_0000;
    IntStream.rangeClosed(infimum, supremum) // fill buffer
        .forEach(i -> AfiOid.writeSubId(bufC, i));
    bufC.clear();
    IntStream.rangeClosed(infimum, supremum).forEach(i -> assertEquals(i, AfiOid.readSubId(bufC)));

    // --- d. test some random values greater then 0x1_0000
    bufC.clear();
    final List<Integer> listD = new ArrayList<>();

    // d.1 fill buffer with numbers on three octet
    RNG.intsClosed(0x1_0000, 0xff_ffff, 100)
        .forEach(
            i -> {
              AfiOid.writeSubId(bufC, i);
              listD.add(i);
            }); // end forEach(i -> ...)

    // d.2 fill buffer with numbers on four octet
    RNG.intsClosed(0x100_0000, Integer.MAX_VALUE, 100)
        .forEach(
            i -> {
              AfiOid.writeSubId(bufC, i);
              listD.add(i);
            }); // end forEach(i -> ...)

    // d.3 retrieve numbers from buffer
    bufC.flip();
    listD.forEach(i -> assertEquals((long) i, AfiOid.readSubId(bufC)));
  } // end method */

  /** Test method for {@link AfiOid#writeSubId(ByteBuffer, long)}. */
  @Test
  void test_writeSubId__ByteBuffer_long() {
    // Test strategy:
    // --- a. smoke test with manually chosen values to empty and partially filled buffer
    // --- b. invalid values
    // --- c. test all one byte codings
    // --- d. test all two byte codings (that range seems larger than any value from real world
    // --- e. test some 3 byte codings
    // --- f. test some 4 byte codings
    // --- g. test some 5 byte codings

    // --- a. smoke test with manually chosen values to empty and partially filled buffer
    final byte[] byteA = new byte[10];
    final ByteBuffer bufA = ByteBuffer.wrap(byteA);

    // a. 1 value   0 to empty buffer,            remaining after write is 9
    AfiOid.writeSubId(bufA, 0);
    assertEquals(1, bufA.position());
    assertEquals(9, bufA.remaining());
    assertEquals("00000000000000000000", Hex.toHexDigits(byteA));

    // a. 2 value   1 to partially filled buffer, remaining after write is 8
    AfiOid.writeSubId(bufA, 1);
    assertEquals(2, bufA.position());
    assertEquals(8, bufA.remaining());
    assertEquals("00010000000000000000", Hex.toHexDigits(byteA));

    // a. 3 value 127 to partially filled buffer, remaining after write is 7
    AfiOid.writeSubId(bufA, 0x7f);
    assertEquals(3, bufA.position());
    assertEquals(7, bufA.remaining());
    assertEquals("00017f00000000000000", Hex.toHexDigits(byteA));

    // a. 4 value 128 to partially filled buffer, remaining after write is 5
    AfiOid.writeSubId(bufA, 0x80);
    assertEquals(5, bufA.position());
    assertEquals(5, bufA.remaining());
    assertEquals("00017f81000000000000", Hex.toHexDigits(byteA));

    // a. 5 value 255 to partially filled buffer, remaining after write is 3
    AfiOid.writeSubId(bufA, 0xff);
    assertEquals(7, bufA.position());
    assertEquals(3, bufA.remaining());
    assertEquals("00017f8100817f000000", Hex.toHexDigits(byteA));

    // a. 6 value 256 to partially filled buffer, remaining after write is 1
    AfiOid.writeSubId(bufA, 0x100);
    assertEquals(9, bufA.position());
    assertEquals(1, bufA.remaining());
    assertEquals("00017f8100817f820000", Hex.toHexDigits(byteA));

    // a. 7 value  42 to partially filled buffer, remaining after write is 0
    AfiOid.writeSubId(bufA, 42);
    assertEquals(10, bufA.position());
    assertEquals(0, bufA.remaining());
    assertEquals("00017f8100817f82002a", Hex.toHexDigits(byteA));

    // a. 8 value  13 to partially filled buffer, exception expected
    assertThrows(BufferOverflowException.class, () -> AfiOid.writeSubId(bufA, 0));

    // clear backing array and buffer
    Arrays.fill(byteA, (byte) 0); // clear contents of backing array
    assertEquals("00000000000000000000", Hex.toHexDigits(byteA));
    bufA.rewind(); // clear buffer
    assertEquals(0, bufA.position());
    assertEquals(10, bufA.remaining());

    // a. 9 value 0x3fff to empty buffer,            remaining after write is 8
    AfiOid.writeSubId(bufA, 0x3fff);
    assertEquals(2, bufA.position());
    assertEquals(8, bufA.remaining());
    assertEquals("ff7f0000000000000000", Hex.toHexDigits(byteA));

    // a.10 value 0x4000 to partially filled buffer, remaining after write is 5
    AfiOid.writeSubId(bufA, 0x4000);
    assertEquals(5, bufA.position());
    assertEquals(5, bufA.remaining());
    assertEquals("ff7f8180000000000000", Hex.toHexDigits(byteA));

    // a.11 biggest value this implementation supports
    AfiOid.writeSubId(bufA, 0x7_ffff_ffffL);
    assertEquals(10, bufA.position());
    assertEquals(0, bufA.remaining());
    assertEquals("ff7f818000ffffffff7f", Hex.toHexDigits(byteA));

    // --- b. invalid values
    bufA.rewind();
    List.of(
            0x8_0000_0000L, // infimum  of invalid non-negative values
            Long.MAX_VALUE // supremum of invalid non-negative values
            )
        .forEach(
            subId ->
                assertThrows(
                    ArrayIndexOutOfBoundsException.class,
                    () -> AfiOid.writeSubId(bufA, subId))); // end forEach(subId -> ...)

    // clear backing array and buffer
    Arrays.fill(byteA, (byte) 0); // clear contents of backing array
    assertEquals("00000000000000000000", Hex.toHexDigits(byteA));

    // --- c. test all one byte codings
    IntStream.rangeClosed(0, 127)
        .forEach(
            subId -> {
              bufA.rewind();
              AfiOid.writeSubId(bufA, subId);
              assertEquals(1, bufA.position());
              assertEquals(9, bufA.remaining());
              assertEquals(String.format("%02x000000000000000000", subId), Hex.toHexDigits(byteA));
            }); // end forEach(subId -> ...)

    // --- d. test all two byte codings (that range seems larger than any value from real world
    IntStream.rangeClosed(1, 127)
        .forEach(
            msByte -> {
              IntStream.rangeClosed(0, 127)
                  .forEach(
                      lsByte -> {
                        bufA.rewind();
                        AfiOid.writeSubId(bufA, ((long) msByte << 7) + lsByte);
                        assertEquals(2, bufA.position());
                        assertEquals(8, bufA.remaining());
                        assertEquals(
                            String.format("%02x%02x0000000000000000", 0x80 | msByte, lsByte),
                            Hex.toHexDigits(byteA));
                      }); // end forEach(lsByte -> ...)
            }); // end forEach(msByte -> ...)

    // --- outer loop for longer codings
    RNG.intsClosed(1, 127, 12)
        .forEach(
            msByte -> {
              final byte[] byteE = new byte[5];
              final ByteBuffer bufE = ByteBuffer.wrap(byteE);

              // Note: With 12 rounds for msByte and 11 random values for secondByte it follows
              //       from 12 * 11 = 132 that with high probability all possible values for
              //       second byte will occur.
              RNG.intsClosed(1, 127, 15) // => 15 - 4 = 11 random values
                  .forEach(
                      secondByte -> {
                        final int num2 = (msByte << 7) + secondByte;
                        final String prefix2 =
                            String.format("%02x%02x", 0x80 | msByte, 0x80 | secondByte);

                        // Note: With 12 rounds for msByte and 15 rounds for secondByte it follows
                        //       from 12 * 15 > 128 that one random value for all other bytes all
                        //       possible values of those bytes will be tested (although not all
                        //       combinations of those bytes).
                        // --- e. test some 3 byte codings
                        List.of(0, RNG.nextIntClosed(1, 126), 127)
                            .forEach(
                                lsByte -> {
                                  Arrays.fill(byteE, (byte) 0);
                                  bufE.rewind();
                                  AfiOid.writeSubId(bufE, ((long) num2 << 7) + lsByte);
                                  assertEquals(3, bufE.position());
                                  assertEquals(2, bufE.remaining());
                                  assertEquals(
                                      String.format("%s%02x0000", prefix2, lsByte),
                                      Hex.toHexDigits(byteE));

                                  // --- f. test some 4 byte codings
                                  final String p02x = "%02x";
                                  final String format4 =
                                      prefix2 + p02x + String.format("%02x00", lsByte);
                                  List.of(0, RNG.nextIntClosed(1, 126), 127)
                                      .forEach(
                                          thirdByte -> {
                                            Arrays.fill(byteE, (byte) 0);
                                            bufE.rewind();
                                            AfiOid.writeSubId(
                                                bufE,
                                                ((long) num2 << 14) + (thirdByte << 7) + lsByte);
                                            assertEquals(4, bufE.position());
                                            assertEquals(1, bufE.remaining());
                                            assertEquals(
                                                String.format(format4, 0x80 | thirdByte),
                                                Hex.toHexDigits(byteE));

                                            // --- g. test some 5 byte codings
                                            final String format5 =
                                                prefix2
                                                    + String.format(p02x, 0x80 | thirdByte)
                                                    + p02x
                                                    + String.format(p02x, lsByte);
                                            List.of(0, RNG.nextIntClosed(1, 126), 127)
                                                .forEach(
                                                    fourthByte -> {
                                                      bufE.rewind();
                                                      final long subId =
                                                          (((long) num2) << 21)
                                                              + (thirdByte << 14)
                                                              + (fourthByte << 7)
                                                              + lsByte;
                                                      AfiOid.writeSubId(bufE, subId);
                                                      assertEquals(5, bufE.position());
                                                      assertEquals(0, bufE.remaining());
                                                      assertEquals(
                                                          String.format(format5, 0x80 | fourthByte),
                                                          Hex.toHexDigits(byteE));
                                                    }); // end forEach(fourthByte -> ...)
                                          }); // end forEach(thirdByte -> ...)
                                }); // end forEach(lsByte -> ...)
                      }); // end forEach(secondByte -> ...)
            }); // end forEach(msByte -> ...)
  } // end method */

  /** Test method for {@link AfiOid#equals(Object)}. */
  @Test
  void test_equals__Object() {
    // Test strategy:
    // --- a. same reference
    // --- b. null input
    // --- c. difference in type
    // --- d. loop over predefined OID
    // --- e. predefined OID compared to non-predefined OID

    // --- create device under test (DUT)
    final AfiOid dut = AfiOid.ansix9p256r1;

    for (final Object[] obj :
        new Object[][] {
          new Object[] {dut, true}, // --- a. same reference
          new Object[] {null, false}, // --- b. null input
          new Object[] {"afi", false}, // --- c. difference in type
        }) {
      assertEquals(obj[1], dut.equals(obj[0]));
    } // end For (obj...)

    // --- d. loop over predefined curves
    AfiOid.PREDEFINED.forEach(
        oidOuter -> {
          AfiOid.PREDEFINED.forEach(
              oidInner -> {
                final boolean identical = oidOuter == oidInner; // NOPMD use equals
                final boolean result = oidOuter.equals(oidInner);

                assertEquals(identical, result);

                if (identical) {
                  assertSame(oidOuter, oidInner);
                } else {
                  assertNotSame(oidOuter, oidInner);
                } // end else
              }); // end forEach(curveInner -> ...)
        }); // end forEach(curveOuter -> ...)

    // --- e. predefined OID compared to non-predefined OID
    // e.1 increment first component modulo 3
    // e.2 increment all other components
    AfiOid.PREDEFINED.forEach(
        oid -> {
          final List<Integer> components = AfiOid.asn2Components(oid.getAsn1());
          for (int i = components.size(); i-- >= 0; ) { // NOPMD assignment in operands
            final List<Integer> input = new ArrayList<>(components); // NOPMD new inside loop

            if (0 == i) {
              // ... increment first component
              //     => do this modulo 3
              input.set(0, (components.getFirst() + 1) % 3);
            } else if (i > 0) {
              // ... not first component
              input.set(i, components.get(i) + 1);
            } else {
              // ... i < 0 => add another component to input
              input.add(RNG.nextIntClosed(0, Integer.MAX_VALUE));
            } // end else

            final AfiOid other = new AfiOid(input); // NOPMD new inside loop
            boolean result = oid.equals(other);
            assertFalse(result);
            result = other.equals(oid);
            assertFalse(result);
          } // end For (i...)
        }); // end forEach(oid -> ...)
  } // end method */

  /** Test method for {@link AfiOid#getName()}. */
  @Test
  void test_getName() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. Smoke test for predefined OID
    // --- b. Smoke test for an arbitrary OID

    AfiOid dut;

    // --- a. Smoke test for predefined OID
    dut = AfiOid.rsaEncryption;
    assertEquals("rsaEncryption", dut.getName());

    // --- b. Smoke test for an arbitrary OID
    dut = new AfiOid(1, 2);
    assertEquals(dut.getPoint(), dut.getName());
  } // end method */

  /** Test method for {@link AfiOid#getAsn1()}. */
  @Test
  void test_getAsn1() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. Smoke test for predefined OID
    // --- b. Smoke test for an arbitrary OID

    // --- a. Smoke test for predefined OID
    assertEquals("{1 2 840 10045 3 1 7}", AfiOid.ansix9p256r1.getAsn1());

    // --- b. Smoke test for an arbitrary OID
    assertEquals("{1 2 840 10045 3 1 42}", new AfiOid(1, 2, 840, 10_045, 3, 1, 42).getAsn1());
  } // end method */

  /** Test method for {@link AfiOid#getOctetString()}. */
  @Test
  void test_getOctetString() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. Smoke test for predefined OID
    // --- b. Smoke test for an arbitrary OID

    // --- a. Smoke test for predefined OID
    assertEquals("2a8648ce3d030107", AfiOid.ansix9p256r1.getOctetString());

    // --- b. Smoke test for an arbitrary OID
    assertEquals("2a8648ce3d03012a", new AfiOid(1, 2, 840, 10_045, 3, 1, 42).getOctetString());
  } // end method */

  /** Test method for {@link AfiOid#getPoint()}. */
  @Test
  void test_getPoint() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. Smoke test for predefined OID
    // --- b. Smoke test for an arbitrary OID

    // --- a. Smoke test for predefined OID
    assertEquals("1.2.840.10045.3.1.7", AfiOid.ansix9p256r1.getPoint());

    // --- b. Smoke test for an arbitrary OID
    assertEquals("1.2.840.10045.3.1.42", new AfiOid(1, 2, 840, 10_045, 3, 1, 42).getPoint());
  } // end method */

  /** Test method for {@link AfiOid#hashCode()}. */
  @Test
  void test_hashCode() {
    // Test strategy:
    // --- a. loop over pre-defined OID
    // --- b. call hashCode()-method again

    // --- a. loop over pre-defined OID
    AfiOid.PREDEFINED.forEach(oid -> assertEquals(oid.getOctetString().hashCode(), oid.hashCode()));

    // --- b. call hashCode()-method again
    // Note: The main reason for this check is to get full code-coverage.
    //        a. The first time this method is called on a newly constructed BerTlv object
    //           insHashCode is zero.
    //        b. The second time this method is called the insHashCode isn't zero (with a
    //           high probability).
    final AfiOid dut = AfiOid.brainpoolP384r1;
    final int hash = dut.hashCode();
    assertEquals(hash, dut.hashCode());
  } // end method */

  /** Test method for {@link AfiOid#toString()}. */
  @Test
  void test_toString() {
    // Note: Simple method does not need extensive testing, so we can be lazy here.

    // Test strategy:
    // --- a. Smoke test for predefined OID
    // --- b. Smoke test for an arbitrary OID

    // --- a. Smoke test for predefined OID
    assertEquals("ansix9p256r1", AfiOid.ansix9p256r1.toString());

    // --- b. Smoke test for an arbitrary OID
    final AfiOid dut = new AfiOid(1, 2, 840, 10_045, 3, 1, 42);
    assertEquals(dut.getPoint(), dut.toString());
  } // end method */
} // end class
