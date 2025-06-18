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

import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_CHR;
import static de.gematik.smartcards.g2icc.cvc.Cvc.TAG_FLAG_LIST;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.gematik.smartcards.crypto.AfiElcParameterSpec;
import de.gematik.smartcards.crypto.AfiElcUtils;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.Hex;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  "PMD.ExcessiveImports",
  "PMD.GodClass",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestCacheCvc {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(TestCacheCvc.class); // */

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Standard charset used in this class. */
  private static final Charset UTF8 = StandardCharsets.UTF_8; // */

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
    TrustCenter.clearCache();
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    // intentionally empty
  } // end method */

  /** Test method for {@link CacheCvc#add(Cvc)}. */
  @Test
  void test_add__Cvc() {
    // Assertion:
    // ... a. Cvc.equals(Object)-method works as expected

    // Note: This simple method does not need extensive testing,
    //       so we can be lazy here.

    // Test strategy:
    // --- a. smoke test with one random CVC
    // --- b. add bunch of random CVC
    // --- c. add a CVC which is equal to a CVC already in the cache

    final Map<String, BerTlv> input = new ConcurrentHashMap<>();
    final EcPrivateKeyImpl prk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP512r1);
    final CacheCvc dut = new CacheCvc();
    final List<Cvc> cvcList = new ArrayList<>();

    assertTrue(dut.getCvc().isEmpty());

    // --- a. smoke test with one random CVC
    {
      final Cvc cvc = new Cvc(TestCvc.randomCvc(input, prk));
      cvcList.add(cvc);

      dut.add(cvc);

      assertEquals(1, dut.getCvc().size());
      assertEquals(Set.of(cvc), dut.getCvc());
    } // end --- a.

    // --- b. add bunch of random CVC
    IntStream.rangeClosed(0, 100)
        .forEach(
            i -> {
              final Cvc cvc = new Cvc(TestCvc.randomCvc(input, prk));
              cvcList.add(cvc);

              dut.add(cvc);

              assertTrue(dut.getCvc().contains(cvc));
            }); // end forEach(i -> ...)
    // end --- b.

    // --- c. add a CVC which is equal to a CVC already in the cache
    {
      final int size = dut.getCvc().size();

      cvcList.forEach(dut::add);

      assertEquals(size, dut.getCvc().size());
    } // end --- c.
  } // end method */

  /**
   * Test method for {@link CacheCvc#collectCvc(Path)}.
   *
   * @throws IOException if underlying methods do so
   */
  @Test
  void test_collectCvc__Path() throws IOException {
    // Assertions:
    // none

    // Test strategy:
    assertTrue(zzzTestCollectCvcPathA()); // --- a. point to a directory structure
    assertTrue(zzzTestCollectCvcPathB()); // --- b. point to a regular file rather than a directory
    assertTrue(zzzTestCollectCvcPathC()); // --- c. ERROR: path does not exist
    assertTrue(zzzTestCollectCvcPathD()); // --- d. ERROR: some files cause exceptions
  } // end method */

  /**
   * Test method for {@link CacheCvc#collectCvc(Path)}.
   *
   * <p>Performs test for strategy "--- a. point to a directory structure".
   *
   * @throws IOException if underlying methods do so
   */
  private boolean zzzTestCollectCvcPathA() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("setA");
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

    // --- create a private keys
    final List<EcPrivateKeyImpl> rootKeys =
        Stream.of(
                AfiElcParameterSpec.brainpoolP256r1,
                AfiElcParameterSpec.brainpoolP384r1,
                AfiElcParameterSpec.brainpoolP512r1)
            .map(EcPrivateKeyImpl::new)
            .toList();

    // --- create a bunch of CVC and store them in the directory structure
    final int noCvcPerRoot = 20;
    final Set<Cvc> expected = new HashSet<>();
    int fileNumber = 0;
    for (final EcPrivateKeyImpl prk : rootKeys) {
      for (int i = noCvcPerRoot; i-- > 0; ) { // NOPMD assignment in operands
        final Map<String, BerTlv> input = new ConcurrentHashMap<>(); // NOPMD new in loop
        final ConstructedBerTlv tlv = TestCvc.randomCvc(input, prk);
        final Path parent = paths.get(RNG.nextInt(0, paths.size()));
        final String namePrefix = String.format("%04d", fileNumber++);
        Files.write(parent.resolve(namePrefix + TrustCenter.SUFFIX_CVC_DER), tlv.getEncoded());
        expected.add(new Cvc(tlv));
      } // end For (i...)
    } // end For (prk...)

    assertEquals(noCvcPerRoot * rootKeys.size(), expected.size());
    assertEquals(expected, CacheCvc.collectCvc(root));

    return true;
  } // end method */

  /**
   * Test method for {@link CacheCvc#collectCvc(Path)}.
   *
   * <p>Performs test for strategy "--- b. point to a regular file rather than a directory".
   *
   * @throws IOException if underlying methods do so
   */
  private boolean zzzTestCollectCvcPathB() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("setB");
    final List<Path> paths = new ArrayList<>(List.of(root));
    for (int i = 1; i-- > 0; ) { // NOPMD assignment in operands
      final String dirName = String.format("dir.%03d", i);
      final Path parent = paths.get(RNG.nextInt(0, paths.size()));
      paths.add(parent.resolve(dirName));
    } // end For (i...)

    // --- create directories
    for (final Path p : paths) {
      Files.createDirectories(p);
    } // end For (p...)

    // --- create a private key
    final EcPrivateKeyImpl rootKey = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1);

    // --- create two CVC and store them in the directory structure
    final Map<String, BerTlv> input = new ConcurrentHashMap<>();
    final ConstructedBerTlv tlv1 = TestCvc.randomCvc(input, rootKey);
    final Cvc cvc1 = new Cvc(tlv1);
    final String fileNamePrefix1 = "1";
    final Path path1 = paths.getFirst().resolve(fileNamePrefix1 + TrustCenter.SUFFIX_CVC_DER);
    Files.write(path1, tlv1.getEncoded());

    input.clear();

    final ConstructedBerTlv tlv2 = TestCvc.randomCvc(input, rootKey);
    final Cvc cvc2 = new Cvc(tlv2);
    final String fileNamePrefix2 = "2";
    final Path path2 = paths.get(1).resolve(fileNamePrefix2 + TrustCenter.SUFFIX_CVC_DER);
    Files.write(path2, tlv2.getEncoded());

    assertEquals(Set.of(cvc1, cvc2), CacheCvc.collectCvc(root));
    assertEquals(Set.of(cvc1), CacheCvc.collectCvc(path1));
    assertEquals(Set.of(cvc2), CacheCvc.collectCvc(path2));

    return true;
  } // end method */

  /**
   * Test method for {@link CacheCvc#collectCvc(Path)}.
   *
   * <p>Performs test for strategy "--- c. ERROR: path does not exist".
   */
  private boolean zzzTestCollectCvcPathC() {
    final Path root = insTempDirectory.resolve("setC");

    assertTrue(CacheCvc.collectCvc(root).isEmpty());

    return true;
  } // end method */

  /**
   * Test method for {@link CacheCvc#collectCvc(Path)}.
   *
   * <p>Performs test for strategy "--- d. ERROR: some files cause exceptions".
   *
   * @throws IOException if underlying methods do so
   */
  private boolean zzzTestCollectCvcPathD() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("setD");
    final List<Path> paths = new ArrayList<>(List.of(root));
    for (int i = 1; i-- > 0; ) { // NOPMD assignment in operands
      final String dirName = String.format("dir.%03d", i);
      final Path parent = paths.get(RNG.nextInt(0, paths.size()));
      paths.add(parent.resolve(dirName));
    } // end For (i...)

    // --- create directories
    for (final Path p : paths) {
      Files.createDirectories(p);
    } // end For (p...)

    // --- create a private key
    final EcPrivateKeyImpl rootKey = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1);

    // --- create two CVC and store them in the directory structure
    final Map<String, BerTlv> input = new ConcurrentHashMap<>();
    final ConstructedBerTlv tlv1 = TestCvc.randomCvc(input, rootKey);
    final Cvc cvc1 = new Cvc(tlv1);
    final String fileNamePrefix1 = "1";
    final Path path1 = paths.getFirst().resolve(fileNamePrefix1 + TrustCenter.SUFFIX_CVC_DER);
    Files.write(path1, tlv1.getEncoded());

    input.clear();

    final ConstructedBerTlv tlv2 = TestCvc.randomCvc(input, rootKey);
    final Cvc cvc2 = new Cvc(tlv2);
    final String fileNamePrefix2 = "2";
    final Path path2 = paths.get(1).resolve(fileNamePrefix2 + TrustCenter.SUFFIX_CVC_DER);
    Files.write(path2, tlv2.getEncoded());

    final String fileNamePrefix3 = "random";
    final Path path3 = paths.getFirst().resolve(fileNamePrefix3 + TrustCenter.SUFFIX_CVC_DER);
    Files.write(path3, RNG.nextBytes(1, 100));

    assertEquals(Set.of(cvc1, cvc2), CacheCvc.collectCvc(root));

    return true;
  } // end method */

  /** Test method for {@link CacheCvc#getChain(Cvc, String)}. */
  @Test
  void test_getChain__Cvc_String() throws IOException {
    // Assertions:
    // ... a. initialize(Path)-method works as expected

    // Test strategy:
    assertTrue(testGetChainCvcStringA()); // --- a. straight line link-certificates
    assertTrue(testGetChainCvcStringB()); // --- b. tree-like link-certificates
    assertTrue(testGetChainCvcStringC()); // --- c. ERROR: invalid CAR
    // --- d. ERROR: no path to existing CVC-Root-CA, see --- c.
  } // end method */

  /**
   * Test method for {@link CacheCvc#getChain(Cvc, String)}.
   *
   * <p>Performs test for strategy "--- a. straight line link-certificates".
   *
   * <p><i><b>Note:</b> The only difference between this method and {@link
   * #testGetChainCvcStringB()} is thw way link-certificates are established between the
   * CVC-Root-CA.</i>
   *
   * @throws IOException if underlying methods do so
   */
  private boolean testGetChainCvcStringA() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("getChain.a");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // --- generate bunch of PKI-structure randomly
    final int noPkiStructures = 5;
    final List<CvcEntity> rootEntities =
        IntStream.rangeClosed(1, noPkiStructures).mapToObj(i -> generatePki(cvcEntities)).toList();

    // --- generate set of CV-certificates
    final Set<Cvc> expected =
        cvcEntities.values().stream().map(CvcEntity::getCvc).collect(Collectors.toSet());

    // --- generate link-certificates
    // strategy: link nextCvcRootCa to previousCvcRootCA and previousCvcRootCa to nextCvcRootCa
    for (int i = rootEntities.size() - 1; i > 0; ) {
      final CvcEntity b = rootEntities.get(i--); // NOPMD reassigning of loop control variable 'i'
      final CvcEntity a = rootEntities.get(i);

      expected.add(generateLinkCertificate(a, b));
      expected.add(generateLinkCertificate(b, a));
    } // end For (i...)

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("gc_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(root.resolve(fileName), i.getCvc().getEncoded());
    } // end For (i...)

    // --- initialize TrustCenter
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    final Cvc initialRoot = rootEntities.getFirst().getCvc();
    TrustCenter.CACHE_PUK.insCache.put(initialRoot.getChr(), initialRoot.getPublicKey());
    assertTrue(TrustCenter.CACHE_CVC.initialize(root).isEmpty());

    // --- perform test with non-specific CAR
    {
      // Note 1: Loop over all cvcEntities and check that the returned
      //         result indeed is a chain of certificates, i.e:
      //         a. If given CVC is self-signed CVC-Root-CA, then result is an empty list
      //         b. The given CVC is expected to be the first element in the returned list.
      //         c. CAR from one element is expected to be the CHR in the following element.
      //         d. The last element is a non-self-signed CV-certificate.
      //         e. No element is a CVC-Root-CA.
      cvcEntities.values().stream()
          .map(CvcEntity::getCvc)
          .forEach(
              cvc -> {
                LOGGER
                    .atTrace()
                    .log(
                        "test_getChain__Cvc_String_a.1, car=\"\", chr=\"{}\"",
                        cvc.getChrObject().getHumanReadable()); // */
                final List<Cvc> present = TrustCenter.CACHE_CVC.getChain(cvc, "");
                LOGGER
                    .atTrace()
                    .log(
                        "chain.a1:{}",
                        String.format(
                            "1%n%s",
                            present.stream()
                                .map(
                                    i ->
                                        "CAR.1="
                                            + i.getCarObject().getHumanReadable()
                                            + ", CHR.1="
                                            + i.getChrObject().getHumanReadable())
                                .collect(Collectors.joining(LINE_SEPARATOR))));

                if (cvc.isRootCa() && cvc.getCar().equals(cvc.getChr())) {
                  // a. If given CVC is self-signed CVC-Root-CA, then result is an empty list
                  assertTrue(present.isEmpty());
                } else {
                  // b. The given CVC is expected to be the first element in the returned list.
                  assertEquals(cvc, present.getFirst());

                  // c. CAR from one element is expected to be the CHR in the following element.
                  Cvc currentCvc = cvc;
                  for (int i = 1; i < present.size(); i++) {
                    final Cvc parentCvc = present.get(i);
                    assertEquals(currentCvc.getCar(), parentCvc.getChr());
                    currentCvc = parentCvc;
                  } // end For (i...)

                  // d. The last element is a non-self-signed CV-certificate.
                  assertNotEquals(currentCvc.getCar(), currentCvc.getChr());

                  // e. No element is a CVC-Root-CA.
                  assertFalse(present.stream().anyMatch(Cvc::isRootCa));
                } // end else
              }); // end forEach(cvc -> ...)
    } // end --- perform test with non-specific CAR

    // --- perform test with specific CAR
    {
      // Note 1: Outer loop: Loop over all CAR of CVC-Root-CA
      // Note 2: Inner loop: Loop over all cvcEntities and check that the returned
      //         result indeed is a chain of certificates, i.e:
      //         a. The given CVC is expected to be the first element in the returned list.
      //         b. CAR from one element is expected to be the CHR in the following element.
      //         c. The last element is a non-self-signed CV-certificate with expected CAR.
      rootEntities.forEach(
          cvcRootCa -> {
            final String car = cvcRootCa.getCvc().getCar();

            cvcEntities.values().stream()
                .map(CvcEntity::getCvc)
                .filter(i -> !i.isRootCa()) // ignore self-signed CVC and link CVC
                .forEach(
                    cvc -> {
                      LOGGER
                          .atTrace()
                          .log(
                              "test_getChain__Cvc_String_a.2, car=\"\", chr=\"{}\"",
                              cvc.getChrObject().getHumanReadable());
                      final List<Cvc> present = TrustCenter.CACHE_CVC.getChain(cvc, car);
                      LOGGER
                          .atTrace()
                          .log(
                              "chain.a2:{}",
                              String.format(
                                  "2%n%s",
                                  present.stream()
                                      .map(
                                          i ->
                                              "CAR.2="
                                                  + i.getCarObject().getHumanReadable()
                                                  + ", CHR.2="
                                                  + i.getChrObject().getHumanReadable())
                                      .collect(Collectors.joining(LINE_SEPARATOR))));

                      // a. The given CVC is expected to be the first element in the returned list.
                      assertEquals(cvc, present.getFirst());

                      // b. CAR from one element is expected to be the CHR in the following element.
                      Cvc currentCvc = cvc;
                      for (int i = 1; i < present.size(); i++) {
                        final Cvc parentCvc = present.get(i);
                        assertEquals(currentCvc.getCar(), parentCvc.getChr());
                        currentCvc = parentCvc;
                      } // end For (i...)

                      // c. The last element is a non-self-signed CV-certificate with expected CAR.
                      assertNotEquals(currentCvc.getCar(), currentCvc.getChr());
                      assertEquals(car, currentCvc.getCar());
                    }); // end forEach(cvc -> ...)
          }); // end forEach(cvcRootCa -> ...)
    } // end --- perform test with specific CAR

    return true;
  } // end method */

  /**
   * Test method for {@link CacheCvc#getChain(Cvc, String)}.
   *
   * <p>Performs test for strategy "--- b. tree-like link-certificates".
   *
   * <p><i><b>Note:</b> The only difference between this method and {@link
   * #testGetChainCvcStringA()} is thw way link-certificates are established between the
   * CVC-Root-CA.</i>
   *
   * @throws IOException if underlying methods do so
   */
  private boolean testGetChainCvcStringB() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("getChain.b");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // --- generate bunch of PKI-structure randomly
    final int noPkiStructures = 10;
    final List<CvcEntity> rootEntities =
        IntStream.rangeClosed(1, noPkiStructures).mapToObj(i -> generatePki(cvcEntities)).toList();

    // --- generate set of CV-certificates
    final Set<Cvc> expected =
        cvcEntities.values().stream().map(CvcEntity::getCvc).collect(Collectors.toSet());

    // --- generate link-certificates
    // strategy: a later generated CVC-Root-CA is double-linked to a randomly chosen
    //           previously generated CVC-Root-CA
    for (int i = 1; i < rootEntities.size(); i++) {
      final CvcEntity a = rootEntities.get(RNG.nextInt(0, i));
      final CvcEntity b = rootEntities.get(i);

      expected.add(generateLinkCertificate(a, b));
      expected.add(generateLinkCertificate(b, a));
    } // end For (i...)

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("gc_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(root.resolve(fileName), i.getCvc().getEncoded());
    } // end For (i...)

    // --- initialize TrustCenter
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    final Cvc initialRoot = rootEntities.getFirst().getCvc();
    TrustCenter.CACHE_PUK.insCache.put(initialRoot.getChr(), initialRoot.getPublicKey());
    assertTrue(TrustCenter.CACHE_CVC.initialize(root).isEmpty());

    // --- perform test with non-specific CAR
    {
      // Note 1: Loop over all cvcEntities and check that the returned
      //         result indeed is a chain of certificates, i.e:
      //         a. If given CVC is self-signed CVC-Root-CA, then result is an empty list
      //         b. The given CVC is expected to be the first element in the returned list.
      //         c. CAR from one element is expected to be the CHR in the following element.
      //         d. The last element is a non-self-signed CV-certificate.
      //         e. No element is a CVC-Root-CA.
      cvcEntities.values().stream()
          .map(CvcEntity::getCvc)
          .forEach(
              cvc -> {
                LOGGER
                    .atTrace()
                    .log(
                        "test_getChain__Cvc_String_b.1, car=\"\", chr=\"{}\"",
                        cvc.getChrObject().getHumanReadable());
                final List<Cvc> present = TrustCenter.CACHE_CVC.getChain(cvc, "");
                LOGGER
                    .atTrace()
                    .log(
                        "chain.b1:{}",
                        String.format(
                            "b.1%n%s",
                            present.stream()
                                .map(
                                    i ->
                                        "CARb.1="
                                            + i.getCarObject().getHumanReadable()
                                            + ", CHRb.1="
                                            + i.getChrObject().getHumanReadable())
                                .collect(Collectors.joining(LINE_SEPARATOR))));

                if (cvc.isRootCa() && cvc.getCar().equals(cvc.getChr())) {
                  // a. If given CVC is self-signed CVC-Root-CA, then result is an empty list
                  assertTrue(present.isEmpty());
                } else {
                  // b. The given CVC is expected to be the first element in the returned list.
                  assertEquals(cvc, present.getFirst());

                  // c. CAR from one element is expected to be the CHR in the following element.
                  Cvc currentCvc = cvc;
                  for (int i = 1; i < present.size(); i++) {
                    final Cvc parentCvc = present.get(i);
                    assertEquals(currentCvc.getCar(), parentCvc.getChr());
                    currentCvc = parentCvc;
                  } // end For (i...)

                  // d. The last element is a non-self-signed CV-certificate.
                  assertNotEquals(currentCvc.getCar(), currentCvc.getChr());

                  // e. No element is a CVC-Root-CA.
                  assertFalse(present.stream().anyMatch(Cvc::isRootCa));
                } // end else
              }); // end forEach(cvc -> ...)
    } // end --- perform test with non-specific CAR

    // --- perform test with specific CAR
    {
      // Note 1: Outer loop: Loop over all CAR of CVC-Root-CA
      // Note 2: Inner loop: Loop over all cvcEntities and check that the returned
      //         result indeed is a chain of certificates, i.e:
      //         a. The given CVC is expected to be the first element in the returned list.
      //         b. CAR from one element is expected to be the CHR in the following element.
      //         c. The last element is a non-self-signed CV-certificate with expected CAR.
      rootEntities.forEach(
          cvcRootCa -> {
            final String car = cvcRootCa.getCvc().getCar();

            cvcEntities.values().stream()
                .map(CvcEntity::getCvc)
                .filter(i -> !i.isRootCa()) // ignore self-signed CVC and link CVC
                .forEach(
                    cvc -> {
                      LOGGER
                          .atTrace()
                          .log(
                              "test_getChain__Cvc_String_b.2, car=\"\", chr=\"{}\"",
                              cvc.getChrObject().getHumanReadable());
                      final List<Cvc> present = TrustCenter.CACHE_CVC.getChain(cvc, car);
                      LOGGER
                          .atTrace()
                          .log(
                              "chain.b2:{}",
                              String.format(
                                  "b.2%n%s",
                                  present.stream()
                                      .map(
                                          i ->
                                              "CARb.2="
                                                  + i.getCarObject().getHumanReadable()
                                                  + ", CHRb.2="
                                                  + i.getChrObject().getHumanReadable())
                                      .collect(Collectors.joining(LINE_SEPARATOR))));

                      // a. The given CVC is expected to be the first element in the returned list.
                      assertEquals(cvc, present.getFirst());

                      // b. CAR from one element is expected to be the CHR in the following element.
                      Cvc currentCvc = cvc;
                      for (int i = 1; i < present.size(); i++) {
                        final Cvc parentCvc = present.get(i);
                        assertEquals(currentCvc.getCar(), parentCvc.getChr());
                        currentCvc = parentCvc;
                      } // end For (i...)

                      // c. The last element is a non-self-signed CV-certificate with expected CAR.
                      assertNotEquals(currentCvc.getCar(), currentCvc.getChr());
                      assertEquals(car, currentCvc.getCar());
                    }); // end forEach(cvc -> ...)
          }); // end forEach(cvcRootCa -> ...)
    } // end --- perform test with specific CAR

    return true;
  } // end method */

  /**
   * Test method for {@link CacheCvc#getChain(Cvc, String)}.
   *
   * <p>Performs test for strategies
   *
   * <ol>
   *   <li>"--- c. ERROR: invalid CAR"
   *   <li>"--- d. ERROR: no path to existing CVC-Root-CA"
   * </ol>
   */
  private boolean testGetChainCvcStringC() throws IOException {
    // Note 1: The following "room-of-trust" is used:
    //         ROOTa
    //         \-- SubCa.1
    //             \-- EE.1
    //
    //         ROOTb
    //         \-- EE.2

    // Note 2: At first no link-certificate is available. Thus,
    //         There is no path from EE.2 to ROOTa.
    // Note 3: After adding a link-certificate there will be a path from
    //         EE.2 to ROOTa.

    // --- preparation
    // create a directory structure
    final Path root = insTempDirectory.resolve("getChain.c");
    Files.createDirectories(root);

    // clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // create PKI.A-structure
    final CvcEntity rootCaA = generateCvcRootCa(cvcEntities);
    final String rootCaChrA = rootCaA.getCvc().getChr();
    cvcEntities.put(rootCaChrA, rootCaA);

    // create CVC-Sub-CA.1
    final CvcEntity subCa1 = generateCvcSubCa(cvcEntities, rootCaA);
    cvcEntities.put(subCa1.getCvc().getChr(), subCa1);

    // create End-Entity.1
    final CvcEntity ee1 = generateCvcEndEntity(cvcEntities, subCa1);
    cvcEntities.put(ee1.getCvc().getChr(), ee1);

    // create PKI.B-structure
    final CvcEntity rootCaB = generateCvcRootCa(cvcEntities);
    final String rootCaChrB = rootCaB.getCvc().getChr();
    cvcEntities.put(rootCaChrB, rootCaB);

    // create End-Entity.2
    final CvcEntity ee2 = generateCvcEndEntity(cvcEntities, rootCaB);
    cvcEntities.put(ee2.getCvc().getChr(), ee2);

    // generate set of CV-certificates
    final Set<Cvc> expected =
        cvcEntities.values().stream().map(CvcEntity::getCvc).collect(Collectors.toSet());

    // add self-signed CV-Certificate for SubCa.1

    // write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("ia_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(root.resolve(fileName), i.getCvc().getEncoded());
    } // end For (i...)

    // prepare TrustCenter
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    TrustCenter.CACHE_PUK.insCache.put(rootCaChrA, rootCaA.getPrivateKey().getPublicKey());
    TrustCenter.CACHE_PUK.insCache.put(rootCaChrB, rootCaB.getPrivateKey().getPublicKey());
    final Set<Cvc> residue = TrustCenter.CACHE_CVC.initialize(root);
    assertTrue(residue.isEmpty());
    assertEquals(expected, TrustCenter.CACHE_CVC.getCvc());

    // --- check all chains available so far
    // arbitrary CAR
    assertTrue(TrustCenter.CACHE_CVC.getChain(rootCaA.getCvc(), "").isEmpty());
    assertEquals(List.of(subCa1.getCvc()), TrustCenter.CACHE_CVC.getChain(subCa1.getCvc(), ""));
    assertEquals(
        List.of(ee1.getCvc(), subCa1.getCvc()), TrustCenter.CACHE_CVC.getChain(ee1.getCvc(), ""));
    assertTrue(TrustCenter.CACHE_CVC.getChain(rootCaB.getCvc(), "").isEmpty());
    assertEquals(List.of(ee2.getCvc()), TrustCenter.CACHE_CVC.getChain(ee2.getCvc(), ""));

    // specific CAR
    assertTrue(TrustCenter.CACHE_CVC.getChain(rootCaA.getCvc(), rootCaChrA).isEmpty());
    assertEquals(
        List.of(subCa1.getCvc()), TrustCenter.CACHE_CVC.getChain(subCa1.getCvc(), rootCaChrA));
    assertEquals(
        List.of(ee1.getCvc(), subCa1.getCvc()),
        TrustCenter.CACHE_CVC.getChain(ee1.getCvc(), rootCaChrA));
    assertTrue(TrustCenter.CACHE_CVC.getChain(rootCaB.getCvc(), rootCaChrB).isEmpty());
    assertEquals(List.of(ee2.getCvc()), TrustCenter.CACHE_CVC.getChain(ee2.getCvc(), rootCaChrB));

    // --- c. ERROR: invalid CAR
    {
      final var cvc = ee1.getCvc();

      assertThrows(
          NoSuchElementException.class, () -> TrustCenter.CACHE_CVC.getChain(cvc, "fooBar"));
    } // end --- c.

    // --- d. ERROR: no path to existing CVC-Root-CA
    {
      final var cvc = ee2.getCvc();
      assertThrows(
          IllegalArgumentException.class, () -> TrustCenter.CACHE_CVC.getChain(cvc, rootCaChrA));
      // add a path for EE.2 to ROOTa
      final Cvc linkCvc = generateLinkCertificate(rootCaB, rootCaA);
      assertTrue(TrustCenter.add(linkCvc));
      assertEquals(List.of(ee2.getCvc(), linkCvc), TrustCenter.CACHE_CVC.getChain(cvc, rootCaChrA));
      assertEquals(List.of(linkCvc), TrustCenter.CACHE_CVC.getChain(linkCvc, rootCaChrA));
    } // end --- d.

    return true;
  } // end method */

  /**
   * Test method for {@link CacheCvc#initialize(Path)}.
   *
   * @throws IOException if underlying methods do so
   */
  @Test
  void test_initialize__Path() throws IOException {
    // Assertions:
    // ... a. collectCvc(Path)-method works as expected

    // Test strategy:
    assertTrue(testInitializePathA()); // --- a. smoke test with manually chosen PKI structure
    assertTrue(testInitializePathB()); // --- b. random PKI structure
    assertTrue(testInitializePathC()); // --- c. two (small) PKI structures
  } // end method */

  /**
   * Test method for {@link CacheCvc#initialize(Path)}.
   *
   * <p>Performs test for strategy "--- a. smoke test with manually chosen PKI structure".
   *
   * @throws IOException if underlying methods do so
   */
  private boolean testInitializePathA() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("initialize.a");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // Note 1: The PKI structure for this test is as follows:
    //         ROOTa
    //         +-- EE-1
    //         +-- SubCa.1
    //         |   +-- EE-2
    //         |   \-- SubCa.2
    //         |       +-- EE-3
    //         \-- SubCa.3
    //             +-- EE-4
    //             \-- EE-5

    // --- create a new CVC-Root-CA
    final CvcEntity cvcRootCa = generateCvcRootCa(cvcEntities);
    final String cvcRootCaChr = cvcRootCa.getCvc().getChr();
    cvcEntities.put(cvcRootCaChr, cvcRootCa);

    // --- create CVC-Sub-CA.1
    final CvcEntity subCa1 = generateCvcSubCa(cvcEntities, cvcRootCa);
    cvcEntities.put(subCa1.getCvc().getChr(), subCa1);

    // --- create CVC-Sub-CA.2
    final CvcEntity subCa2 = generateCvcSubCa(cvcEntities, subCa1);
    cvcEntities.put(subCa2.getCvc().getChr(), subCa2);

    // --- create CVC-Sub-CA.3
    final CvcEntity subCa3 = generateCvcSubCa(cvcEntities, cvcRootCa);
    cvcEntities.put(subCa3.getCvc().getChr(), subCa3);

    // --- create End-Entities
    final CvcEntity ee1 = generateCvcEndEntity(cvcEntities, cvcRootCa);
    cvcEntities.put(ee1.getCvc().getChr(), ee1);
    final CvcEntity ee2 = generateCvcEndEntity(cvcEntities, subCa1);
    cvcEntities.put(ee2.getCvc().getChr(), ee2);
    final CvcEntity ee3 = generateCvcEndEntity(cvcEntities, subCa2);
    cvcEntities.put(ee3.getCvc().getChr(), ee3);
    final CvcEntity ee4 = generateCvcEndEntity(cvcEntities, subCa3);
    cvcEntities.put(ee4.getCvc().getChr(), ee4);
    final CvcEntity ee5 = generateCvcEndEntity(cvcEntities, subCa3);
    cvcEntities.put(ee5.getCvc().getChr(), ee5);

    // --- create set of CV-certificates
    final Set<Cvc> expected =
        cvcEntities.values().stream()
            .map(CvcEntity::getCvc)
            .filter(i -> !i.isRootCa()) // ignore self-signed CVC-Root-CA
            .collect(Collectors.toSet());

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("ia_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(root.resolve(fileName), i.getCvc().getEncoded());
    } // end For (i...)

    // --- perform test
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    TrustCenter.CACHE_PUK.insCache.put(cvcRootCaChr, cvcRootCa.getPrivateKey().getPublicKey());

    final Set<Cvc> residue = TrustCenter.CACHE_CVC.initialize(root);

    assertTrue(residue.isEmpty());
    assertEquals(expected, TrustCenter.CACHE_CVC.getCvc());

    return true;
  } // end method */

  /**
   * Test method for {@link CacheCvc#initialize(Path)}.
   *
   * <p>Performs test for strategy "--- b. random PKI structure".
   *
   * @throws IOException if underlying methods do so
   */
  private boolean testInitializePathB() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("initialize.b");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // Note 1: The PKI structure for this test is randomly generated.

    // --- create PKI-structure randomly
    generatePki(cvcEntities);
    final CvcEntity cvcRootCa =
        cvcEntities.values().stream()
            .filter(i -> "ROOTa".equals(i.getCaIdentifier()))
            .findAny()
            .orElseThrow();
    final String cvcRootCaChr = cvcRootCa.getCvc().getChr();

    // --- create set of CV-certificates
    final Set<Cvc> expected =
        cvcEntities.values().stream()
            .map(CvcEntity::getCvc)
            .filter(i -> !i.isRootCa()) // ignore self-signed CVC-Root-CA
            .collect(Collectors.toSet());

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("ib_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(root.resolve(fileName), i.getCvc().getEncoded());
    } // end For (i...)

    // --- perform test
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    TrustCenter.CACHE_PUK.insCache.put(cvcRootCaChr, cvcRootCa.getPrivateKey().getPublicKey());

    final Set<Cvc> residue = TrustCenter.CACHE_CVC.initialize(root);

    assertTrue(residue.isEmpty());
    assertEquals(expected, TrustCenter.CACHE_CVC.getCvc());

    return true;
  } // end method */

  /**
   * Test method for {@link CacheCvc#initialize(Path)}.
   *
   * <p>Performs test for strategy "--- c. two (small) PKI structures".
   *
   * @throws IOException if underlying methods do so
   */
  private boolean testInitializePathC() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("initialize.c");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // Note 1: The PKI structure for this test is as follows:
    //         ROOTa
    //         +-- EE-1
    //
    //         ROOTb
    //         +-- EE-2

    // --- create PKI-A structure
    final CvcEntity cvcRootCaA = generateCvcRootCa(cvcEntities);
    final String cvcRootCaChrA = cvcRootCaA.getCvc().getChr();
    cvcEntities.put(cvcRootCaChrA, cvcRootCaA);
    final CvcEntity ee1 = generateCvcEndEntity(cvcEntities, cvcRootCaA);
    cvcEntities.put(ee1.getCvc().getChr(), ee1);

    // --- create PKI-B structure
    final CvcEntity cvcRootCaB = generateCvcRootCa(cvcEntities);
    final String cvcRootCaChrB = cvcRootCaB.getCvc().getChr();
    cvcEntities.put(cvcRootCaChrB, cvcRootCaB);
    final CvcEntity ee2 = generateCvcEndEntity(cvcEntities, cvcRootCaB);
    cvcEntities.put(ee2.getCvc().getChr(), ee2);

    // --- create set of CV-certificates
    final Set<Cvc> expected =
        cvcEntities.values().stream()
            .map(CvcEntity::getCvc)
            .filter(i -> !i.isRootCa()) // ignore self-signed CVC-Root-CA
            .collect(Collectors.toSet());

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("ic_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(root.resolve(fileName), i.getCvc().getEncoded());
    } // end For (i...)

    // --- perform test
    // only public key of PKI-A in cache
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    TrustCenter.CACHE_PUK.add(cvcRootCaChrA, cvcRootCaA.getPrivateKey().getPublicKey());

    final Set<Cvc> residueA = TrustCenter.CACHE_CVC.initialize(root);

    assertEquals(Set.of(ee2.getCvc()), residueA);
    assertEquals(Set.of(ee1.getCvc()), TrustCenter.CACHE_CVC.getCvc());

    // only public key of PKI-B in cache
    TrustCenter.clearCache();
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    TrustCenter.CACHE_PUK.add(cvcRootCaChrB, cvcRootCaB.getPrivateKey().getPublicKey());

    final Set<Cvc> residueB = TrustCenter.CACHE_CVC.initialize(root);

    assertEquals(Set.of(ee1.getCvc()), residueB);
    assertEquals(Set.of(ee2.getCvc()), TrustCenter.CACHE_CVC.getCvc());

    // public keys of PKI-A and PKI-B in cache
    TrustCenter.clearCache();
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    TrustCenter.CACHE_PUK.add(cvcRootCaChrA, cvcRootCaA.getPrivateKey().getPublicKey());
    TrustCenter.CACHE_PUK.add(cvcRootCaChrB, cvcRootCaB.getPrivateKey().getPublicKey());

    final Set<Cvc> residue = TrustCenter.CACHE_CVC.initialize(root);

    assertTrue(residue.isEmpty());
    assertEquals(Set.of(ee1.getCvc(), ee2.getCvc()), TrustCenter.CACHE_CVC.getCvc());

    return true;
  } // end method */

  /*
   * Test method for {@link CacheCvc#rootChain(Cvc)}.
   *
   * @throws IOException if underlying methods do so
   *
  @Test
  void test_rootChain__Cvc() throws IOException {
    // Assertions:
    // none

    // Test strategy:
    test_rootChain__Cvc_a(); // --- a. single PKI-structure
    test_rootChain__Cvc_b(); // --- b. multiple PKI-structure with double-linked-certificates
    test_rootChain__Cvc_c(); // --- c. ERROR: chain stops immediately
    test_rootChain__Cvc_d(); // --- d. ERROR: chain stops at self-signed Sub-CA
    test_rootChain__Cvc_e(); // --- e. ERROR: chain stops, because an ancestor has no parent
    // --- f. ERROR: more than one parent, but no self-signed Root-CA, see --- d.
  } // end method */

  /*
   * Test method for {@link CacheCvc#rootChain(Cvc)}.
   *
   * <p>Performs test for strategy "--- a. single PKI-structure".
   *
   * @throws IOException if underlying methods do so
   *
  private void test_rootChain__Cvc_a() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("rootChain.a");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // --- generate one PKI-structure randomly
    generatePki(cvcEntities);

    // --- create set of CV-certificates
    final Set<Cvc> expected = cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .collect(Collectors.toSet());

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("ra_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(
          root.resolve(fileName),
          i.getCvc().getEncoded()
      );
    } // end For (i...)

    // --- initialize TrustCenter
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.insCacheCvc.isEmpty());
    cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .filter(Cvc::isRootCa)
        .forEach(cvc -> // loop over all CVC-Root-CA
            TrustCenter.CACHE_PUK.insCache.put(
                cvc.getChr(),
                cvc.getPublicKey()
            )
    ); // end forEach(cvc -> ...)
    assertTrue(
        TrustCenter.CACHE_CVC.initialize(root)
            .isEmpty()
    );

    // --- perform test
    // Note 1: Loop over all cvcEntities and check that the returned result indeed
    //         is a chain of certificates, i.e.:
    //         a. The given CVC is expected to be the first element in the returned list.
    //         b. CAR from one element is expected to be the CHR in the following element.
    //         c. The last element is a self-signed CVC-Root-CA.
    cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .forEach(cvc -> {
          final List<Cvc> present = TrustCenter.CACHE_CVC.rootChain(cvc);
          assertEquals(
              cvc,
              present.get(0)
          );

          Cvc currentCvc = cvc;
          for (int i = 1; i < present.size(); i++) {
            final Cvc parentCvc = present.get(i);
            assertEquals(
                currentCvc.getCar(),
                parentCvc.getChr()
            );
            currentCvc = parentCvc;
          } // end For (i...)

          assertTrue(
              currentCvc.isRootCa()
          );
          assertEquals(
              currentCvc.getCar(),
              currentCvc.getChr()
          );
        }); // end forEach(cvc -> ...)
  } // end method */

  /*
   * Test method for {@link CacheCvc#rootChain(Cvc)}.
   *
   * <p>Performs test for strategy "--- b. multiple PKI-structure with double-linked-certificates".
   *
   * @throws IOException if underlying methods do so
   *
  private void test_rootChain__Cvc_b() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("rootChain.b");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // --- generate bunch of PKI-structure randomly
    final int noPkiStructures = 5;
    final List <CvcEntity> rootEntities = IntStream.rangeClosed(1, noPkiStructures)
        .mapToObj(i -> generatePki(cvcEntities))
        .toList();

    // --- generate set of CV-certificates
    final Set<Cvc> expected = cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .collect(Collectors.toSet());

    // --- generate link-certificates
    // strategy: link nextCvcRootCa to previousCvcRootCA and previousCvcRootCa to nextCvcRootCa
    for (int i = rootEntities.size() - 1; i > 0;) {
      final CvcEntity b = rootEntities.get(i--); // NOPMD reassigning of loop control variable 'i'
      final CvcEntity a = rootEntities.get(i);

      expected.add(generateLinkCertificate(a, b));
      expected.add(generateLinkCertificate(b, a));
    } // end For (i...)

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("rb_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(
          root.resolve(fileName),
          i.getCvc().getEncoded()
      );
    } // end For (i...)

    // --- initialize TrustCenter
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.insCacheCvc.isEmpty());
    cvcEntities.values().stream()
        .filter(i -> "ROOTa".equals(i.getCaIdentifier())) // insert just one public key to cache
        .map(CvcEntity::getCvc)
        .filter(Cvc::isRootCa)
        .forEach(cvc ->
            TrustCenter.CACHE_PUK.insCache.put(
                cvc.getChr(),
                cvc.getPublicKey()
            )
    ); // end forEach(cvc -> ...)
    assertTrue(
        TrustCenter.CACHE_CVC.initialize(root)
            .isEmpty()
    );

    // --- perform test
    // Note 1: Loop over all cvcEntities and check that the returned result indeed
    //         is a chain of certificates, i.e.:
    //         a. The given CVC is expected to be the first element in the returned list.
    //         b. CAR from one element is expected to be the CHR in the following element.
    //         c. The last element is a self-signed CVC-Root-CA.
    cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .forEach(cvc -> {
          final List<Cvc> present = TrustCenter.CACHE_CVC.rootChain(cvc);
          assertEquals(
              cvc,
              present.get(0)
          );

          Cvc currentCvc = cvc;
          for (int i = 1; i < present.size(); i++) {
            final Cvc parentCvc = present.get(i);
            assertEquals(
                currentCvc.getCar(),
                parentCvc.getChr()
            );
            currentCvc = parentCvc;
          } // end For (i...)

          assertTrue(
              currentCvc.isRootCa()
          );
          assertEquals(
              currentCvc.getCar(),
              currentCvc.getChr()
          );
        }); // end forEach(cvc -> ...)
  } // end method */

  /*
   * Test method for {@link CacheCvc#rootChain(Cvc)}.
   *
   * <p>Performs test for strategy "--- c. ERROR: chain stops immediately".
   *
   * @throws IOException if underlying methods do so
   *
  private void test_rootChain__Cvc_c() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("rootChain.c");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // --- generate PKI.A-structure randomly
    generatePki(cvcEntities);

    // --- generate another PKI.B-structure
    final CvcEntity cvcRootCaB = generateCvcRootCa(cvcEntities);
    final CvcEntity endEntityLonely = generateCvcEndEntity(cvcEntities, cvcRootCaB);

    // --- create set of CV-certificates
    final Set<Cvc> expected = cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .collect(Collectors.toSet());
    expected.add(endEntityLonely.getCvc());

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("rc_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(
          root.resolve(fileName),
          i.getCvc().getEncoded()
      );
    } // end For (i...)

    // --- initialize TrustCenter
    cvcEntities.put(
        cvcRootCaB.getCvc().getCar(),
        cvcRootCaB
    );
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.insCacheCvc.isEmpty());
    cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .filter(Cvc::isRootCa)
        .forEach(cvc -> // loop over all CVC-Root-CA
            TrustCenter.CACHE_PUK.insCache.put(
                cvc.getChr(),
                cvc.getPublicKey()
            )
    ); // end forEach(cvc -> ...)
    assertTrue(
        TrustCenter.CACHE_CVC.initialize(root)
            .isEmpty()
    );
    final List<String> findings = endEntityLonely.getCvc().getReport();
    assertTrue(
        findings.isEmpty(),
        () -> findings.stream()
            .collect(Collectors.joining(LINE_SEPARATOR))
    );
    assertTrue(
        TrustCenter.CACHE_CVC.insCacheCvc.contains(endEntityLonely.getCvc())
    );

    // --- perform test for PKI.A-structure
    // Note 1: Loop over all cvcEntities and check that the returned result indeed
    //         is a chain of certificates, i.e.:
    //         a. The given CVC is expected to be the first element in the returned list.
    //         b. CAR from one element is expected to be the CHR in the following element.
    //         c. The last element is a self-signed CVC-Root-CA.
    assertFalse(cvcEntities.containsValue(endEntityLonely));
    cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .forEach(cvc -> {
          final List<Cvc> present = TrustCenter.CACHE_CVC.rootChain(cvc);
          assertEquals(
              cvc,
              present.get(0)
          );

          Cvc currentCvc = cvc;
          for (int i = 1; i < present.size(); i++) {
            final Cvc parentCvc = present.get(i);
            assertEquals(
                currentCvc.getCar(),
                parentCvc.getChr()
            );
            currentCvc = parentCvc;
          } // end For (i...)

          assertTrue(
              currentCvc.isRootCa()
          );
          assertEquals(
              currentCvc.getCar(),
              currentCvc.getChr()
          );
        }); // end forEach(cvc -> ...)

    // --- perform test for PKI.B-structure
    assertThrows(
        IllegalArgumentException.class,
        () -> TrustCenter.CACHE_CVC.rootChain(endEntityLonely.getCvc())
    );

    // add self-signed CVC-Root-CA
    TrustCenter.CACHE_CVC.insCacheCvc.add(cvcRootCaB.getCvc());
    assertEquals(
        List.of(
            endEntityLonely.getCvc(),
            cvcRootCaB.getCvc()
        ),
        TrustCenter.CACHE_CVC.rootChain(endEntityLonely.getCvc())
    );
  } // end method */

  /*
   * Test method for {@link CacheCvc#rootChain(Cvc)}.
   *
   * <p>Performs test for strategies
   * <ol>
   *   <li>"--- d. ERROR: chain stops at self-signed Sub-CA".
   *   <li>"--- f. ERROR: more than one parent, but no self-signed Root-CA"
   * </ol>
   *
   * @throws IOException if underlying methods do so
   *
  private void test_rootChain__Cvc_d() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("rootChain.d");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // --- generate PKI.A-structure randomly
    generatePki(cvcEntities);

    // --- generate another PKI.B-structure
    final CvcEntity cvcRootCaB = generateCvcRootCa(cvcEntities);
    final CvcEntity cvcSubCaB = generateCvcSubCa(cvcEntities, cvcRootCaB);
    final CvcEntity endEntityB = generateCvcEndEntity(cvcEntities, cvcSubCaB);

    // --- create set of CV-certificates
    final Set<Cvc> expected = cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .collect(Collectors.toSet());
    expected.add(
        generateLinkCertificate(cvcSubCaB, cvcSubCaB)
    );
    expected.add(endEntityB.getCvc());

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("rd_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(
          root.resolve(fileName),
          i.getCvc().getEncoded()
      );
    } // end For (i...)

    // --- initialize TrustCenter
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.insCacheCvc.isEmpty());
    cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .filter(Cvc::isRootCa)
        .forEach(cvc -> // loop over all CVC-Root-CA
            TrustCenter.CACHE_PUK.insCache.put(
                cvc.getChr(),
                cvc.getPublicKey()
            )
    ); // end forEach(cvc -> ...)
    TrustCenter.CACHE_PUK.insCache.put(
        cvcSubCaB.getCvc().getChr(),
        cvcSubCaB.getPrivateKey().getPublicKey()
    );
    assertTrue(
        TrustCenter.CACHE_CVC.initialize(root)
            .isEmpty()
    );
    final List<String> findings = endEntityB.getCvc().getReport();
    assertTrue(
        findings.isEmpty(),
        () -> findings.stream()
            .collect(Collectors.joining(LINE_SEPARATOR))
    );
    assertTrue(
        TrustCenter.CACHE_CVC.insCacheCvc.contains(endEntityB.getCvc())
    );

    // --- perform test for PKI.A-structure
    // Note 1: Loop over all cvcEntities and check that the returned result indeed
    //         is a chain of certificates, i.e.:
    //         a. The given CVC is expected to be the first element in the returned list.
    //         b. CAR from one element is expected to be the CHR in the following element.
    //         c. The last element is a self-signed CVC-Root-CA.
    assertFalse(cvcEntities.containsValue(endEntityB));
    cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .forEach(cvc -> {
          final List<Cvc> present = TrustCenter.CACHE_CVC.rootChain(cvc);
          assertEquals(
              cvc,
              present.get(0)
          );

          Cvc currentCvc = cvc;
          for (int i = 1; i < present.size(); i++) {
            final Cvc parentCvc = present.get(i);
            assertEquals(
                currentCvc.getCar(),
                parentCvc.getChr()
            );
            currentCvc = parentCvc;
          } // end For (i...)

          assertTrue(
              currentCvc.isRootCa()
          );
          assertEquals(
              currentCvc.getCar(),
              currentCvc.getChr()
          );
        }); // end forEach(cvc -> ...)

    // --- perform test for PKI.B-structure
    assertThrows(
        IllegalArgumentException.class,
        () -> TrustCenter.CACHE_CVC.rootChain(endEntityB.getCvc())
    );

    // add self-signed CVC-Root-CA
    TrustCenter.CACHE_CVC.insCacheCvc.add(cvcRootCaB.getCvc());
    TrustCenter.CACHE_CVC.insCacheCvc.add(cvcSubCaB.getCvc());
    assertThrows(
        IllegalArgumentException.class,
        () -> TrustCenter.CACHE_CVC.rootChain(endEntityB.getCvc())
    );
  } // end method */

  /*
   * Test method for {@link CacheCvc#rootChain(Cvc)}.
   *
   * <p>Performs test for strategy "--- e. ERROR: chain stops, because an ancestor has no parent".
   *
   * @throws IOException if underlying methods do so
   *
  private void test_rootChain__Cvc_e() throws IOException {
    // --- create a directory structure
    final Path root = insTempDirectory.resolve("rootChain.e");
    Files.createDirectories(root);

    // --- clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // --- generate PKI.A-structure randomly
    generatePki(cvcEntities);

    // --- generate another PKI.B-structure
    final CvcEntity cvcRootCaB = generateCvcRootCa(cvcEntities);
    final CvcEntity cvcSubCaB = generateCvcSubCa(cvcEntities, cvcRootCaB);
    final CvcEntity endEntityB = generateCvcEndEntity(cvcEntities, cvcSubCaB);

    // --- create set of CV-certificates
    final Set<Cvc> expected = cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .collect(Collectors.toSet());
    expected.add(cvcSubCaB.getCvc());
    expected.add(endEntityB.getCvc());

    // --- write CV-certificates to file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("re_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(
          root.resolve(fileName),
          i.getCvc().toByteArray()
      );
    } // end For (i...)

    // --- initialize TrustCenter
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.insCacheCvc.isEmpty());
    cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .filter(Cvc::isRootCa)
        .forEach(cvc -> // loop over all CVC-Root-CA
            TrustCenter.CACHE_PUK.insCache.put(
                cvc.getChr(),
                cvc.getPublicKey()
            )
    ); // end forEach(cvc -> ...)
    TrustCenter.CACHE_PUK.insCache.put(
        cvcRootCaB.getCvc().getChr(),
        cvcRootCaB.getPrivateKey().getPublicKey()
    );
    assertTrue(
        TrustCenter.CACHE_CVC.initialize(root)
            .isEmpty()
    );
    final List<String> findings = endEntityB.getCvc().getReport();
    assertTrue(
        findings.isEmpty(),
        () -> findings.stream()
            .collect(Collectors.joining(LINE_SEPARATOR))
    );
    assertTrue(
        TrustCenter.CACHE_CVC.insCacheCvc.contains(endEntityB.getCvc())
    );

    // --- perform test for PKI.A-structure
    // Note 1: Loop over all cvcEntities and check that the returned result indeed
    //         is a chain of certificates, i.e.:
    //         a. The given CVC is expected to be the first element in the returned list.
    //         b. CAR from one element is expected to be the CHR in the following element.
    //         c. The last element is a self-signed CVC-Root-CA.
    assertFalse(cvcEntities.containsValue(endEntityB));
    cvcEntities.values().stream()
        .map(CvcEntity::getCvc)
        .forEach(cvc -> {
          final List<Cvc> present = TrustCenter.CACHE_CVC.rootChain(cvc);
          assertEquals(
              cvc,
              present.get(0)
          );

          Cvc currentCvc = cvc;
          for (int i = 1; i < present.size(); i++) {
            final Cvc parentCvc = present.get(i);
            assertEquals(
                currentCvc.getCar(),
                parentCvc.getChr()
            );
            currentCvc = parentCvc;
          } // end For (i...)

          assertTrue(
              currentCvc.isRootCa()
          );
          assertEquals(
              currentCvc.getCar(),
              currentCvc.getChr()
          );
        }); // end forEach(cvc -> ...)

    // --- perform test for PKI.B-structure
    assertThrows(
        IllegalArgumentException.class,
        () -> TrustCenter.CACHE_CVC.rootChain(endEntityB.getCvc())
    );

    // add self-signed CVC-Root-CA
    TrustCenter.CACHE_CVC.insCacheCvc.add(cvcRootCaB.getCvc());
    assertEquals(
        List.of(
            endEntityB.getCvc(),
            cvcSubCaB.getCvc(),
            cvcRootCaB.getCvc()
        ),
        TrustCenter.CACHE_CVC.rootChain(endEntityB.getCvc())
    );
  } // end method */

  /**
   * Test method for {@link CacheCvc#path(Cvc)}.
   *
   * @throws IOException if underlying methods do so
   */
  @Test
  void test_path__Cvc() throws IOException {
    // Assertions:
    // ... a. getChain(Cvc, String)-method works as expected

    // Note 1: Because of the assertion, we can be lazy here.

    // Test strategy:
    // --- a. check method for all CV-certificate

    // Note 1: The "room-of-trust" for this test is as follows:
    //         ROOTa
    //         +-- SubCa.1
    //         |   \-- SubCa.2
    //         |       +-- EE-1
    //
    //         ROOTb
    //         \-- EE-2

    // Note 2: ROOTb is linked to ROOTa

    // --- preparation
    // create a directory structure
    final Path root = insTempDirectory.resolve("path");
    Files.createDirectories(root);

    // clear cache for a clean test bed
    TrustCenter.clearCache();
    final Map<String, CvcEntity> cvcEntities = new ConcurrentHashMap<>();

    // create PKI.A-structure
    final CvcEntity rootCaA = generateCvcRootCa(cvcEntities);
    final String rootCaChrHexA = rootCaA.getCvc().getChr();
    final String rootCaChrA = rootCaA.getCvc().getChrObject().getHumanReadable();
    cvcEntities.put(rootCaChrHexA, rootCaA);

    // create CVC-Sub-CA.1
    final CvcEntity subCa1 = generateCvcSubCa(cvcEntities, rootCaA);
    final String subCa1Chr = subCa1.getCvc().getChrObject().getHumanReadable();
    cvcEntities.put(subCa1.getCvc().getChr(), subCa1);

    // create CVC-Sub-CA.2
    final CvcEntity subCa2 = generateCvcSubCa(cvcEntities, subCa1);
    final String subCa2Chr = subCa2.getCvc().getChrObject().getHumanReadable();
    cvcEntities.put(subCa2.getCvc().getChr(), subCa2);

    // create End-Entity.1
    final CvcEntity ee1 = generateCvcEndEntity(cvcEntities, subCa2);
    final String ee1Chr = ee1.getCvc().getChrObject().getHumanReadable();
    cvcEntities.put(ee1.getCvc().getChr(), ee1);

    // create PKI.B-structure
    final CvcEntity rootCaB = generateCvcRootCa(cvcEntities);
    final String rootCaChrHexB = rootCaB.getCvc().getChr();
    final String rootCaChrB = rootCaB.getCvc().getChrObject().getHumanReadable();
    cvcEntities.put(rootCaChrHexB, rootCaB);

    // create End-Entity.2
    final CvcEntity ee2 = generateCvcEndEntity(cvcEntities, rootCaB);
    final String ee2Chr = ee2.getCvc().getChrObject().getHumanReadable();
    cvcEntities.put(ee2.getCvc().getChr(), ee2);

    // generate set of CV-certificates
    final Set<Cvc> expected =
        cvcEntities.values().stream().map(CvcEntity::getCvc).collect(Collectors.toSet());

    // generate one link-certificate
    final Cvc linkCertificate = generateLinkCertificate(rootCaB, rootCaA);
    expected.add(linkCertificate);

    // write CV-certificates to the file system
    int namePrefix = 0;
    for (final Cvc i : expected) {
      final String fileName = String.format("ia_%04d%s", namePrefix++, TrustCenter.SUFFIX_CVC_DER);
      Files.write(root.resolve(fileName), i.getCvc().getEncoded());
    } // end For (i...)

    // prepare TrustCenter
    assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
    assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    TrustCenter.CACHE_PUK.insCache.put(rootCaChrHexA, rootCaA.getPrivateKey().getPublicKey());
    final Set<Cvc> residue = TrustCenter.CACHE_CVC.initialize(root);
    assertTrue(residue.isEmpty());
    assertEquals(expected, TrustCenter.CACHE_CVC.getCvc());

    // --- a. check method for all CV-certificate in a straight path of for CV-certificates
    // a.1 check with CVC-Root-CA.A
    // a.2 check with CVC-Sub-CA.1
    // a.3 check with CVC-Sub-CA.2
    // a.4 check with End-Entity.1
    // a.5 check with CVC-Root-CA.B
    // a.6 check with End-Entity.2
    // a.7 check with link-certificate

    // a.1 check with CVC-Root-CA
    assertEquals(Path.of(rootCaChrA), TrustCenter.CACHE_CVC.path(rootCaA.getCvc()));

    // a.2 check with CVC-Sub-CA.1
    assertEquals(Path.of(rootCaChrA, subCa1Chr), TrustCenter.CACHE_CVC.path(subCa1.getCvc()));

    // a.3 check with CVC-Sub-CA.2
    assertEquals(
        Path.of(rootCaChrA, subCa1Chr, subCa2Chr), TrustCenter.CACHE_CVC.path(subCa2.getCvc()));

    // a.4 check with End-Entity.1
    assertEquals(
        Path.of(rootCaChrA, subCa1Chr, subCa2Chr, ee1Chr),
        TrustCenter.CACHE_CVC.path(ee1.getCvc()));

    // a.5 check with CVC-Root-CA.B
    assertEquals(Path.of(rootCaChrB), TrustCenter.CACHE_CVC.path(rootCaB.getCvc()));

    // a.6 check with End-Entity.2
    assertEquals(Path.of(rootCaChrB, ee2Chr), TrustCenter.CACHE_CVC.path(ee2.getCvc()));

    // a.7 check with link-certificate
    assertEquals(
        Path.of(rootCaChrA, linkCertificate.getChrObject().getHumanReadable()),
        TrustCenter.CACHE_CVC.path(linkCertificate));
  } // end method */

  private static String randomCar(final String caIdentifier, final LocalDate ced) {
    return Hex.toHexDigits(caIdentifier.getBytes(UTF8))
        + (RNG.nextBoolean() ? "1" : "8") // ServiceIndicator
        + String.format("%d", RNG.nextIntClosed(0, 9)) // discretionary data
        + "02" // algorithm reference
        + String.format("%02d", ced.getYear() - 2000); // year
  } // end method */

  /**
   * Chooses domain parameter randomly.
   *
   * @return randomly chosen brainpool domain parameter
   */
  private static AfiElcParameterSpec randomDomainParameter() {
    return List.of(
            AfiElcParameterSpec.brainpoolP256r1,
            AfiElcParameterSpec.brainpoolP384r1,
            AfiElcParameterSpec.brainpoolP512r1)
        .get(RNG.nextInt(0, 3));
  } // end method */

  /**
   * Generate one PKI-structure with arbitrary depth.
   *
   * <p>The given {@link Map} of {@link CvcEntity} is extended as follows:
   *
   * <ol>
   *   <li>A new CVC-Root-CA is created. Its self-signed CV-certificate is added to the {@link Map}.
   *   <li>The newly created CVC-Root-CA is the root of a new PKI-structure for which a bunch of
   *       Sub-CA are created.
   *   <li>The parent of a each new Sub-CA is randomly chosen from the CAs already present in this
   *       PKI-structure.
   *   <li>The CV-certificate of the newly created Sub-CA is added to the {@link Map}.
   *   <li>A bunch of end-entities is created.
   *   <li>The parent of each new end-entity is randomly chosen from the CAs already present.
   *   <li>The CV-certificate of the newly created end-entity is added to the {@link Map}.
   * </ol>
   *
   * @param cvcEntities already present in the "room-of-trust", this collection is enlarged by this
   *     method
   * @return CVC-Root-CA entity
   */
  private static CvcEntity generatePki(final Map<String, CvcEntity> cvcEntities) {
    // --- create a new CVC-Root-CA
    final CvcEntity cvcRootCa = generateCvcRootCa(cvcEntities);
    final String cvcRootCaChr = cvcRootCa.getCvc().getChr();
    cvcEntities.put(cvcRootCaChr, cvcRootCa);

    // --- create tree-like PKI structure
    // create the nodes
    final List<CvcEntity> nodes = new ArrayList<>(List.of(cvcRootCa));
    for (int i = RNG.nextIntClosed(4, 10); i-- > 0; ) { // NOPMD assignment in operand
      final CvcEntity parent = nodes.get(RNG.nextInt(0, nodes.size())); // randomly chose a parent
      final CvcEntity subCa = generateCvcSubCa(cvcEntities, parent);
      nodes.add(subCa);
      cvcEntities.put(subCa.getCvc().getChr(), subCa);
    } // end For (i...)

    // create leaves
    for (int i = 2 * nodes.size(); i-- > 0; ) { // NOPMD assignment in operand
      final CvcEntity parent = nodes.get(RNG.nextInt(0, nodes.size())); // randomly chose a parent
      final CvcEntity endEntity = generateCvcEndEntity(cvcEntities, parent);
      cvcEntities.put(endEntity.getCvc().getChr(), endEntity);
    } // end For (i...)

    return cvcRootCa;
  } // end method */

  /**
   * Generate a new CVC-Root-CA.
   *
   * <p>The principles are as follows:
   *
   * <ol>
   *   <li>All CVC-Root-CA have an identifier in the form {@code "ROOT?"} where the last character
   *       is a lower case letter in range [a, z]. The next available letter will be used. This way
   *       up to 26 CVC-Root-CA are possible.
   *   <li>The generation year of the first CVC-Root-CA is fixed by a constant.
   *   <li>The next CVC-Root-CA will be generated at a date randomly chosen in range one to three
   *       years after the previous CVC-Root-CA. This way the last (i.e. 26th CVC-Root-CA) will be
   *       generated at most 78 years after the first CVC-Root-CA.
   * </ol>
   *
   * @param cvcEntities already present in the "room-of-trust", this collection is NOT changed by
   *     this method
   * @return newly generated CVC-Root-CA
   */
  private static CvcEntity generateCvcRootCa(final Map<String, CvcEntity> cvcEntities) {
    // generation year of first CVC-Root-CA
    final int infimumYear = 2000;

    // prefix used by all CVC-Root-CA for CHR
    final String rootChrPrefix = "ROOT";
    final String rootChrHexPrefix = Hex.toHexDigits(rootChrPrefix.getBytes(UTF8));

    // --- estimate last letter of CHR for new CVC-Root-CA
    // number of CVC-Root-CA already present in cvcEntities
    final int noCvcRootCa =
        cvcEntities.keySet().stream().filter(i -> i.startsWith(rootChrHexPrefix)).toList().size();
    assertTrue(noCvcRootCa < 27);
    final String rcaIdentifier = rootChrPrefix + ((char) ('a' + noCvcRootCa));

    // --- estimate CED and CXD
    final LocalDate ced;
    if (0 == noCvcRootCa) {
      // ... first CVC-Root-CA
      ced = LocalDate.of(infimumYear, 1, 1).plusDays(RNG.nextIntClosed(0, 364));
    } else {
      // ... not first CVC-Root-CA
      final String lastRcaIdentifier = rootChrPrefix + ((char) ('a' + noCvcRootCa - 1));
      final CvcEntity lastRootCa =
          cvcEntities.values().stream()
              .filter(i -> lastRcaIdentifier.equals(i.getCaIdentifier()))
              .findAny()
              .orElseThrow();
      final LocalDate lastCed = lastRootCa.getCvc().getCed().getDate();
      ced = lastCed.plusDays(RNG.nextIntClosed(365, 3 * 365));
    } // end else
    final LocalDate cxd = ced.plusYears(10);
    assertTrue(cxd.getYear() < 2100);
    assertTrue(ced.isBefore(cxd));

    // --- generate key pair
    final EcPrivateKeyImpl privateKey = new EcPrivateKeyImpl(randomDomainParameter());
    final EcPublicKeyImpl publicKey = privateKey.getPublicKey();

    // --- generate self-signed CVC
    final String chr = randomCar(rcaIdentifier, ced);

    final Map<String, BerTlv> input = new ConcurrentHashMap<>();

    input.put(TestCvc.KEY_CPI, new CertificateProfileIndicator().getDataObject());
    input.put(TestCvc.KEY_CAR, new CertificationAuthorityReference(chr).getDataObject());
    input.put(
        TestCvc.KEY_PUK,
        BerTlv.getInstance(
            0x86, AfiElcUtils.p2osUncompressed(publicKey.getW(), publicKey.getParams())));
    input.put(TestCvc.KEY_OID_FLAG_LIST, new DerOid(AfiOid.CVC_FlagList_TI));
    input.put(TestCvc.KEY_FLAG_LIST, TestCvc.randomFlaglist(7, 7, (byte) 0xc0));
    input.put(TestCvc.KEY_CHR, new CardHolderReference(chr).getDataObject());
    input.put(TestCvc.KEY_CED, new CertificateDate(true, ced).getDataObject());
    input.put(TestCvc.KEY_CXD, new CertificateDate(false, cxd).getDataObject());

    final Cvc cvc = new Cvc(TestCvc.randomCvc(input, privateKey));
    assertFalse(cvc.hasCriticalFindings());
    final List<String> findings = cvc.getReport();
    assertEquals(
        List.of("verification key was not found => signature could not be checked"),
        findings,
        () -> findings.stream().collect(Collectors.joining(LINE_SEPARATOR)));

    // --- generate CvcEntity
    return new CvcEntity(privateKey, cvc);
  } // end method */

  /**
   * Generates a new CVC-Sub-CA.
   *
   * <p>The principles are as follows:
   *
   * <ol>
   *   <li>All CVC-Sub-CA have an identifier in the form {@code "?????"} where all letter are lower
   *       case in range [a, z].
   *   <li>All CVC-Sub-CA identifier are unique.
   *   <li>CED of the newly created CVC-Sub-CA will be randomly chosen in range [1, 15] after the
   *       CED of its parent.
   *   <li>CXD of the newly created CVC-Sub-CA will be randomly chosen in range [1, 15] before the
   *       CXD of its parent.
   *   <li>The building rule for CED and CXD together with the ten year life-time defined in {@link
   *       #generateCvcRootCa(Map)} assures that chains with more than 100 chain-members are
   *       possible and even then CED is less than CXD.
   * </ol>
   *
   * @param cvcEntities already present in the "room-of-trust", this collection is NOT changed by
   *     this method
   * @param parent issuing a {@link Cvc} to this CVC-Sub-CA
   * @return newly generated CVC-Sub-CA
   */
  private static CvcEntity generateCvcSubCa(
      final Map<String, CvcEntity> cvcEntities, final CvcEntity parent) {
    // --- estimate CHR
    final char[] chrPrefix = new char[5];
    for (; ; ) {
      for (int i = chrPrefix.length; i-- > 0; ) { // NOPMD assignment in operand
        chrPrefix[i] = (char) RNG.nextIntClosed('a', 'z');
      } // end For (i...)
      final String prefix = new String(chrPrefix); // NOPMD new in loop

      final boolean breakFlag =
          cvcEntities.values().stream()
              .filter(i -> prefix.equals(i.getCaIdentifier()))
              .findAny()
              .isEmpty();
      if (breakFlag) {
        // ... chrPrefix is unique
        break;
      } // end fi
    } // end For (;;)

    // --- estimate CED and CXD
    final LocalDate ced = parent.getCvc().getCed().getDate().plusDays(RNG.nextIntClosed(1, 15));
    final LocalDate cxd = parent.getCvc().getCxd().getDate().minusDays(RNG.nextIntClosed(1, 15));
    assertTrue(ced.isBefore(cxd));

    // --- generate key pair
    final EcPrivateKeyImpl privateKey = new EcPrivateKeyImpl(randomDomainParameter());
    final EcPublicKeyImpl publicKey = privateKey.getPublicKey();

    // --- estimate flagList
    final byte[] parentFlagList = parent.getCvc().insFlagList.clone();
    final byte[] flagList = new byte[parentFlagList.length];
    final byte[] maskRfu = Hex.toByteArray("00 ff 7b df e1 ff 1f");
    for (int i = flagList.length; i-- > 0; ) { // NOPMD assignment in operand
      flagList[i] = (byte) (parentFlagList[i] & maskRfu[i] & RNG.nextIntClosed(0, 0xff));
    } // end For (i...)
    flagList[0] &= 0x3f; // clear the two MSBit
    flagList[0] |= (byte) 0x80; // bits b0 b1 of flagList indicating "Sub-CA"

    // --- generate CV-certificate
    final String chr =
        randomCar(
            new String(chrPrefix), // NOPMD string initialization
            ced);
    final Map<String, BerTlv> input = new ConcurrentHashMap<>();

    input.put(TestCvc.KEY_CPI, new CertificateProfileIndicator().getDataObject());
    input.put(
        TestCvc.KEY_CAR,
        new CertificationAuthorityReference(parent.getCvc().getChr()).getDataObject());
    input.put(
        TestCvc.KEY_PUK,
        BerTlv.getInstance(
            0x86, AfiElcUtils.p2osUncompressed(publicKey.getW(), publicKey.getParams())));
    input.put(TestCvc.KEY_OID_FLAG_LIST, new DerOid(AfiOid.CVC_FlagList_TI));
    input.put(TestCvc.KEY_FLAG_LIST, BerTlv.getInstance(TAG_FLAG_LIST, flagList));
    input.put(TestCvc.KEY_CHR, new CardHolderReference(chr).getDataObject());
    input.put(TestCvc.KEY_CED, new CertificateDate(true, ced).getDataObject());
    input.put(TestCvc.KEY_CXD, new CertificateDate(false, cxd).getDataObject());

    final Cvc cvc = new Cvc(TestCvc.randomCvc(input, parent.getPrivateKey()));
    assertFalse(cvc.hasCriticalFindings());
    final List<String> findings = cvc.getReport();
    assertEquals(
        List.of("verification key was not found => signature could not be checked"),
        findings,
        () -> findings.stream().collect(Collectors.joining(LINE_SEPARATOR)));

    // --- generate CvcEntity
    return new CvcEntity(privateKey, cvc);
  } // end method */

  /**
   * Generates a link-certificate.
   *
   * @param requester entity for which a link-certificate is generated
   * @param signer entity signing the link-certificate
   */
  private static Cvc generateLinkCertificate(final CvcEntity requester, final CvcEntity signer) {
    final Cvc cvcRequest = requester.getCvc();
    final Cvc cvcSigner = signer.getCvc();
    final Map<String, BerTlv> input = new ConcurrentHashMap<>();

    input.put(TestCvc.KEY_CPI, cvcRequest.insCpi.getDataObject());
    input.put(
        TestCvc.KEY_CAR, new CertificationAuthorityReference(cvcSigner.getChr()).getDataObject());
    input.put(
        TestCvc.KEY_PUK,
        BerTlv.getInstance(
            0x86,
            AfiElcUtils.p2osUncompressed(
                cvcRequest.getPublicKey().getW(), cvcRequest.getPublicKey().getParams())));
    input.put(TestCvc.KEY_OID_FLAG_LIST, new DerOid(cvcRequest.insOidFlagList));
    input.put(TestCvc.KEY_FLAG_LIST, BerTlv.getInstance(TAG_FLAG_LIST, cvcRequest.insFlagList));
    input.put(TestCvc.KEY_CHR, cvcRequest.getChrObject().getDataObject());
    input.put(TestCvc.KEY_CED, cvcRequest.getCed().getDataObject());
    input.put(TestCvc.KEY_CXD, cvcRequest.getCxd().getDataObject());

    return new Cvc(TestCvc.randomCvc(input, signer.getPrivateKey()));
  } // end method */

  /**
   * Generates a new CVC-End-Entity.
   *
   * <p>The principles are as follows:
   *
   * <ol>
   *   <li>All CVC-End-Entity CHR are unique.
   *   <li>CED of the newly created CVC-End-Entity will be randomly chosen in range [1, 15] after
   *       the CED of its parent.
   *   <li>CXD of the newly created CVC-End-Entity will be randomly chosen in range [1, 15] before
   *       the CXD of its parent.
   *   <li>The building rule for CED and CXD together with the ten year life-time defined in {@link
   *       #generateCvcRootCa(Map)} assures that chains with more than 100 chain-members are
   *       possible and even then CED is less than CXD.
   * </ol>
   *
   * @param cvcEntities already present in the "room-of-trust", this collection is NOT changed by
   *     this method
   * @param parent issuing a {@link Cvc} to this CVC-End-Entity
   * @return newly generated CVC-End-Entity
   */
  private static CvcEntity generateCvcEndEntity(
      final Map<String, CvcEntity> cvcEntities, final CvcEntity parent) {
    // --- estimate CHR
    String chr;
    do {
      chr = Hex.toHexDigits(((BerTlv) TestCvc.randomChr(TAG_CHR, 12, 12).get(0)).getValueField());
    } while (cvcEntities.containsKey(chr));

    // --- estimate CED and CXD
    final LocalDate ced = parent.getCvc().getCed().getDate().plusDays(RNG.nextIntClosed(1, 15));
    final LocalDate cxd = parent.getCvc().getCxd().getDate().minusDays(RNG.nextIntClosed(1, 15));
    assertTrue(ced.isBefore(cxd));

    // --- generate key pair
    final EcPrivateKeyImpl privateKey = new EcPrivateKeyImpl(randomDomainParameter());
    final EcPublicKeyImpl publicKey = privateKey.getPublicKey();

    // --- estimate flagList
    final byte[] parentFlagList = parent.getCvc().insFlagList.clone();
    final byte[] flagList = new byte[parentFlagList.length];
    final byte[] maskRfu = Hex.toByteArray("00 ff 7b df e1 ff 1f");
    for (int i = flagList.length; i-- > 0; ) { // NOPMD assignment in operands
      flagList[i] = (byte) (parentFlagList[i] & maskRfu[i] & RNG.nextIntClosed(0, 0xff));
    } // end For (i...)
    flagList[0] &= 0x3f; // clear the two MSBit => flagList indicates "EndEntiy"

    // --- generate CV-certificate
    final Map<String, BerTlv> input = new ConcurrentHashMap<>();

    input.put(TestCvc.KEY_CPI, new CertificateProfileIndicator().getDataObject());
    input.put(
        TestCvc.KEY_CAR,
        new CertificationAuthorityReference(parent.getCvc().getChr()).getDataObject());
    input.put(
        TestCvc.KEY_PUK,
        BerTlv.getInstance(
            0x86, AfiElcUtils.p2osUncompressed(publicKey.getW(), publicKey.getParams())));
    input.put(TestCvc.KEY_OID_FLAG_LIST, new DerOid(AfiOid.CVC_FlagList_TI));
    input.put(TestCvc.KEY_FLAG_LIST, BerTlv.getInstance(TAG_FLAG_LIST, flagList));
    input.put(TestCvc.KEY_CHR, new CardHolderReference(chr).getDataObject());
    input.put(TestCvc.KEY_CED, new CertificateDate(true, ced).getDataObject());
    input.put(TestCvc.KEY_CXD, new CertificateDate(false, cxd).getDataObject());

    final Cvc cvc = new Cvc(TestCvc.randomCvc(input, parent.getPrivateKey()));
    assertFalse(cvc.hasCriticalFindings());
    final List<String> findings = cvc.getReport();
    assertEquals(
        List.of("verification key was not found => signature could not be checked"),
        findings,
        () -> findings.stream().collect(Collectors.joining(LINE_SEPARATOR)));

    // --- generate CvcEntity
    return new CvcEntity(privateKey, cvc);
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /**
   * Class collecting relevant information for an entity with a CV-certificate.
   *
   * <p>This is a value class which provides information.
   *
   * @author <a href="mailto:software-development@gematik.de">gematik</a>
   */
  private static final class CvcEntity {

    /** CA-identifier of entity. */
    private final String insCaIdentifier; // */

    /** CV-certificate of entity. */
    private final Cvc insCvc; // */

    /** Private key of entity. */
    private final EcPrivateKeyImpl insPrivateKey; // */

    /**
     * Comfort constructor.
     *
     * @param privateKey of entity
     * @param cvc of entity
     */
    private CvcEntity(final EcPrivateKeyImpl privateKey, final Cvc cvc) {
      insPrivateKey = privateKey;
      insCvc = cvc;
      insCaIdentifier =
          cvc.isEndEntity() ? "" : cvc.getChrObject().getHumanReadable().substring(0, 5);
    } // end constructor */

    /**
     * Returns CA-identifier.
     *
     * <p>In particular:
     *
     * <ol>
     *   <li>In case of an end-entity an empty {@link String} is returned.
     *   <li>If not an end-entity, then the string representation of the first five octet of CHR in
     *       human-readable form are returned.
     * </ol>
     *
     * @return first five letters of human-readable CHR in case of non-end-entity
     */
    private String getCaIdentifier() {
      return insCaIdentifier;
    } // end method */

    /**
     * Getter.
     *
     * @return CV-certificate of entity
     */
    private Cvc getCvc() {
      return insCvc;
    } // end method +/

    /**
     * Getter.
     *
     * @return private key of entity
     */
    private EcPrivateKeyImpl getPrivateKey() {
      return insPrivateKey;
    } // end method */
  } // end inner class
} // end class
