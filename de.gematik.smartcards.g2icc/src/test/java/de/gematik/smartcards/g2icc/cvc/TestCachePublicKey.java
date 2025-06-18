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
package de.gematik.smartcards.g2icc.cvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.crypto.AfiElcParameterSpec;
import de.gematik.smartcards.crypto.EafiElcPukFormat;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Class for testing methods in class {@link CachePublicKey}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.
//         Spotbugs message: Non-null field insTempDirectory is not initialized
//         This finding is for an attribute which is initialized by JUnit.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 1
}) // */
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.MethodNamingConventions",
  "checkstyle.methodname"
})
final class TestCachePublicKey {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Temporary directory. */
  @TempDir
  private Path insTempDirectory; // NOPMD getter, NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR */

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

  /** Test method for {@link CachePublicKey#add(String, EcPublicKeyImpl)}. */
  @Test
  void test_add__String_EcPublicKeyImpl() {
    // Test strategy:
    // --- a. add a first key
    // --- b. add a bunch of other keys
    // --- c. add the same (key, value)-pair again
    // --- d. ERROR: add another key under the same CHR

    final CachePublicKey dut = new CachePublicKey();
    assertTrue(dut.insCache.isEmpty());

    // --- a. add a first key
    {
      final String chr = "Max.Mustermann";
      final EcPublicKeyImpl puk =
          new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1).getPublicKey();

      dut.add(chr, puk);

      assertEquals(1, dut.insCache.size());
      assertTrue(dut.insCache.containsKey(chr));
      assertSame(puk, dut.insCache.get(chr));
    } // end --- a.

    // --- b. add a bunch of other keys
    {
      final List<String> chrList = new ArrayList<>();
      final List<EcPrivateKeyImpl> prkList = new ArrayList<>();

      // spotless:off
      AfiElcParameterSpec.PREDEFINED.forEach(dp ->
          IntStream.rangeClosed(0, 20).forEach(i -> {
            String chr;
            do {
              chr = RNG.nextUtf8(1, 20);
            } while (dut.insCache.containsKey(chr));
            final EcPrivateKeyImpl prk = new EcPrivateKeyImpl(dp);

            chrList.add(chr);
            prkList.add(prk);
            dut.add(chr, prk.getPublicKey());
          }) // end forEach(i -> ...)
      ); // end forEach(dp -> ...)
      // spotless:on

      for (int i = chrList.size(); i-- > 0; ) { // NOPMD assignments in operands
        final String chr = chrList.get(i);
        final EcPublicKeyImpl puk = prkList.get(i).getPublicKey();

        assertTrue(dut.insCache.containsKey(chr));
        assertSame(puk, dut.insCache.get(chr));
      } // end For (i...)
    } // end --- b.

    // --- c. add the same (key, value)-pair again
    {
      final Map<String, EcPublicKeyImpl> clone = new ConcurrentHashMap<>(dut.insCache);

      dut.insCache.forEach(
          (key, value) -> {
            dut.add(key, value);

            assertEquals(clone, dut.insCache);
          }); // end forEach((key, value) -> ...)
    } // end --- c.

    // --- d. ERROR: add another key under the same CHR
    {
      EcPrivateKeyImpl prk;
      do {
        prk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1); // NOPMD new in loop
      } while (dut.insCache.containsValue(prk.getPublicKey()));
      final EcPublicKeyImpl puk = prk.getPublicKey();
      // ... public key from prk is NOT in cache

      dut.insCache.forEach(
          (key, value) ->
              assertThrows(
                  IllegalArgumentException.class,
                  () -> dut.add(key, puk))); // end forEach((key, value) -> ...)
    } // end --- d.
  } // end method */

  /** Test method for {@link CachePublicKey#getPublicKey(String)}. */
  @Test
  void test_getPublicKey__String() {
    // Assertions:
    // - none -

    // Note: This simple method does not need extensive testing,
    //       so we can be lazy here and concentrate on code-coverage.

    // Test strategy:
    // --- a. use CHR of key in cache
    // --- b. ERROR: CHR is unknown in cache

    final CachePublicKey dut = new CachePublicKey();
    final String chr = "Mustermann.Max";
    final EcPublicKeyImpl puk =
        new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1).getPublicKey();
    dut.add(chr, puk);

    // --- a. use CHR of key in cache
    assertSame(puk, dut.getPublicKey(chr));

    // --- b. ERROR: CHR is unknown in cache
    assertThrows(NoSuchElementException.class, () -> dut.getPublicKey("foo"));
  } // end method */

  /**
   * Test method for {@link CachePublicKey#initialize(Path)}.
   *
   * @throws IOException if underlying methods do so
   */
  @Test
  void test_initialize__Path() throws IOException {
    // Assertions:
    // ... a. add(String, EcPublicKeyImpl)-method works as expected

    // Note 1: Set A contains a bunch of public keys but nothing else.
    //         The directory structure is flat.
    // Note 2: Set B contains a bunch of public keys plus some extra files
    //         A multi-level directory structure is used.

    final CachePublicKey dut = new CachePublicKey();

    // Test strategy:
    assertTrue(testInitializePathA(dut)); // --- a. point to directory of set A
    assertTrue(testInitializePathB(dut)); // --- b. point to directory of set B
    assertTrue(testInitializePathC(dut)); // --- c. point to a regular file rather than a directory
    assertTrue(testInitializePathD(dut)); // --- d. ERROR: path does not exist
    assertTrue(testInitializePathE(dut)); // --- e. ERROR: some files cause exceptions
  } // end method */

  /**
   * Test method for {@link CachePublicKey#initialize(Path)}.
   *
   * <p>Performs test for strategy "--- a. point to directory of set A".
   *
   * @param dut device under test
   * @throws IOException if underlying methods do so
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  private boolean testInitializePathA(final CachePublicKey dut) throws IOException {
    // --- create set A
    final Path root = insTempDirectory.resolve("setA");

    Files.createDirectories(root);

    final Set<String> chrSet = new HashSet<>();
    while (chrSet.size() < 30) {
      chrSet.add(Hex.toHexDigits(RNG.nextBytes(1, 10)));
    } // end While (enough CHR?)
    final Map<String, EcPublicKeyImpl> map = new ConcurrentHashMap<>();
    for (final String chr : chrSet) {
      final EcPublicKeyImpl puk =
          new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1).getPublicKey();
      map.put(chr, puk);
      Files.write(
          root.resolve(chr + CachePublicKey.SUFFIX_PUK_DER),
          puk.getEncoded(EafiElcPukFormat.ISOIEC7816).getEncoded());
    } // end For (chr...)

    // --- fill the cache a bit
    dut.insCache.clear();
    dut.add("Alf", new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP384r1).getPublicKey());
    assertEquals(1, dut.insCache.size());

    dut.initialize(root); // Note: This method clears all previous content

    assertEquals(map, dut.insCache);

    return true;
  } // end method */

  /**
   * Test method for {@link CachePublicKey#initialize(Path)}.
   *
   * <p>Performs test for strategy "--- b. point to directory of set B"
   *
   * @param dut device under test
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  private boolean testInitializePathB(final CachePublicKey dut) throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("setB");
    final List<Path> paths = new ArrayList<>(List.of(root));
    for (int i = 10; i-- > 0; ) { // NOPMD assignment in operands
      final String dirName = String.format("dir.%03d", i);
      final Path parent = paths.get(RNG.nextInt(0, paths.size()));
      paths.add(parent.resolve(dirName));
    } // end For (i...)

    // --- create directories
    for (final Path p : paths) {
      Files.createDirectories(p);
    } // end For (p...)

    // --- create set B
    final Set<String> chrSet = new HashSet<>();
    while (chrSet.size() < 30) {
      chrSet.add(Hex.toHexDigits(RNG.nextBytes(1, 10)));
    } // end While (enough CHR?)

    final Map<String, EcPublicKeyImpl> map = new ConcurrentHashMap<>();
    for (final String chr : chrSet) {
      final EcPublicKeyImpl puk =
          new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1).getPublicKey();
      map.put(chr, puk);
      final Path parent = paths.get(RNG.nextInt(0, paths.size()));
      Files.write(
          parent.resolve(chr + CachePublicKey.SUFFIX_PUK_DER),
          puk.getEncoded(EafiElcPukFormat.ISOIEC7816).getEncoded());
    } // end For (chr...)

    dut.initialize(root); // Note: This method clears all previous content

    assertEquals(map, dut.insCache);

    return true;
  } // end method */

  /**
   * Test method for {@link CachePublicKey#initialize(Path)}.
   *
   * <p>Performs test for strategy "--- c. point to a regular file rather than a directory"
   *
   * @param dut device under test
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  private boolean testInitializePathC(final CachePublicKey dut) throws IOException {
    // --- create set C
    final Path root = insTempDirectory.resolve("setC");
    Files.createDirectories(root);
    final String chr1 = Hex.toHexDigits(RNG.nextBytes(1, 5));
    final String chr2 = Hex.toHexDigits(RNG.nextBytes(6, 10));
    final Set<String> chrSet = Set.of(chr1, chr2);

    final Map<String, EcPublicKeyImpl> map = new ConcurrentHashMap<>();
    for (final String chr : chrSet) {
      final EcPublicKeyImpl puk =
          new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1).getPublicKey();
      map.put(chr, puk);
      Files.write(
          root.resolve(chr + CachePublicKey.SUFFIX_PUK_DER),
          puk.getEncoded(EafiElcPukFormat.ISOIEC7816).getEncoded());
    } // end For (chr...)

    dut.initialize(root); // assure that directory "setC" contains more than one public key
    assertEquals(map, dut.insCache);

    // perform intended test
    dut.initialize(root.resolve(chr1 + CachePublicKey.SUFFIX_PUK_DER));
    assertEquals(Map.of(chr1, map.get(chr1)), dut.insCache);

    return true;
  } // end method */

  /**
   * Test method for {@link CachePublicKey#initialize(Path)}.
   *
   * <p>Performs test for strategy "--- d. ERROR: path does not exist"
   *
   * @param dut device under test
   */
  private boolean testInitializePathD(final CachePublicKey dut) {
    final Path root = insTempDirectory.resolve("setD");

    // --- fill the cache a bit
    dut.insCache.clear();
    dut.add("Alfred", new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP384r1).getPublicKey());
    assertEquals(1, dut.insCache.size());

    dut.initialize(root); // Note: This method clears all previous content

    assertTrue(dut.insCache.isEmpty());

    return true;
  } // end method */

  /**
   * Test method for {@link CachePublicKey#initialize(Path)}.
   *
   * <p>Performs test for strategy "--- e. ERROR: some files cause exceptions"
   *
   * @param dut device under test
   */
  @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
  private boolean testInitializePathE(final CachePublicKey dut) throws IOException {
    // --- create set E
    final Path root = insTempDirectory.resolve("setE");
    Files.createDirectories(root);
    final String chr1 = Hex.toHexDigits(RNG.nextBytes(1, 5));
    final String chr2 = Hex.toHexDigits(RNG.nextBytes(6, 10));
    final Set<String> chrSet = Set.of(chr1, chr2);

    final Map<String, EcPublicKeyImpl> map = new ConcurrentHashMap<>();
    for (final String chr : chrSet) {
      final EcPublicKeyImpl puk =
          new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1).getPublicKey();
      map.put(chr, puk);
      Files.write(
          root.resolve(chr + CachePublicKey.SUFFIX_PUK_DER),
          puk.getEncoded(EafiElcPukFormat.ISOIEC7816).getEncoded());
    } // end For (chr...)
    Files.write(root.resolve("Arbitrary" + CachePublicKey.SUFFIX_PUK_DER), RNG.nextBytes(1, 100));

    dut.initialize(root);

    assertEquals(map, dut.insCache);

    return true;
  } // end method */
} // end class
