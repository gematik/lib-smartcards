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

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class caches {@link Cvc}.
 *
 * <p>This class is used by {@link TrustCenter}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE".
//         Spotbugs message: The return value from a method is dereferenced without
//         a null check, and the return value of that method is one that should
//         generally be checked for null.
//         Rational: That finding is suppressed because getFileName()-method
//         cannot return null here.
// Note 2: Spotbugs claims "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE".
//         Spotbugs message: A value is checked here to see whether it is null,
//         but this value cannot be null because it was previously dereferenced.
//         Rational: That finding is suppressed because getFileName()-method
//         cannot return null here.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", // see note 1
  "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE" // see note 2
}) // */
/* package */ final class CacheCvc {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(CacheCvc.class); // */

  /** Cache for CV-certificates. */
  private final Set<Cvc> insCvc = new HashSet<>(); // */

  /**
   * Add a card-verifiable certificate to the database.
   *
   * @param cvc card-verifiable certificate to be added to the cache
   * @return {@code TRUE} if this cache did not already contain the {@code cvc}, {@code FALSE}
   *     otherwise
   */
  /* package */ boolean add(final Cvc cvc) {
    return getCvc().add(cvc);
  } // end method */

  /**
   * Collect all {@link Cvc} from given directory and its children.
   *
   * <p>The given folder and its subfolders are scanned for regular files with file name suffix
   * {@link TrustCenter#SUFFIX_CVC_DER}.
   *
   * <p>A {@link Cvc} is added to the result, if
   *
   * <ol>
   *   <li>it is possible to extract a valid {@link ConstructedBerTlv} data object from the file
   *       content without exceptions, AND
   *   <li>it is possible to create a {@link Cvc} from that data object without exceptions.
   * </ol>
   *
   * @param root start folder for collecting {@link Cvc}
   * @return set with {@link Cvc} found in given directory
   */
  @VisibleForTesting // otherwise = private
  /* package */ static Set<Cvc> collectCvc(final Path root) {
    LOGGER.atTrace().log("collectCvc from root \"{}\"", root);

    final Set<Cvc> result = new HashSet<>();

    try (Stream<Path> stream = Files.walk(root.toAbsolutePath().normalize())) {
      stream
          .filter(
              path -> // RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE
              path.getFileName() // NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
                      .toString()
                      .endsWith(".cvc"))
          .forEach(
              path -> {
                try {
                  final byte[] fileContent = Files.readAllBytes(path);
                  final ConstructedBerTlv tlv = (ConstructedBerTlv) BerTlv.getInstance(fileContent);

                  result.add(new Cvc(tlv));
                  LOGGER.atTrace().log("CVC read from path \"{}\"", path);
                } catch (Exception e) { // NOPMD catching generic exception
                  LOGGER.atInfo().log("trouble with path \"{}\"", path);
                  LOGGER.atDebug().log(UNEXPECTED, e);
                } // end Catch (...)
              }); // end forEach(path -> ...)
    } catch (IOException e) {
      LOGGER.atWarn().log("trouble with root \"{}\"", root);
      LOGGER.atDebug().log(UNEXPECTED, e);
    } // end Catch (...)

    return result;
  } // end method */

  /**
   * Estimates a CV-certificate chain from given one up to but not including the self-signed
   * CV-certificate with given {@code car}.
   *
   * <p>If the given CV-certificate is a self-signed CVC-Root-CA, then the returned list is empty.
   *
   * <p>If the given CV-certificate is not self-signed or not a CVC-Root-CA, then
   *
   * <ol>
   *   <li>the first element is the given one
   *   <li>element i (if present) is the CV-certificate of the issuer who issued element {@code
   *       (i-1)}
   *   <li>The last element is a non-self-signed CV-certificate where {@code CAR} equals the given
   *       one.
   * </ol>
   *
   * @param cvc CV-certificate for which the CVC-chain is requested
   * @param car of CVC-Root-CA where the chain ends
   * @return list of CV-certificates (possibly empty)
   * @throws IllegalArgumentException if no path from given CV-Certificate to CVC-Root-CA with given
   *     CAR is found
   * @throws NoSuchElementException if no CVC-Root-CA with given CAR exists in the cache.
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  /* package */ List<Cvc> getChain(final Cvc cvc, final String car) {
    // Assumptions, for wording see
    // https://en.wikipedia.org/wiki/Graph_(discrete_mathematics):
    // ... a. Several CVC-Root-CA instances exist.
    // ... b. Each CVC-Root-CA issues CV-certificates for CVC-Sub-CA and End-Entity instances.
    // ... c. A CVC-Sub-CA instance issues CV-certificates for other CVC-Sub-CA and End-Entity
    //        instances.
    // ... d. CVC-Root-CA are linked together by Link-CV-certificates, i.e. a
    //        CVC-Root-CA issues a Link-CV-certificate for another CVC-Root-CA.
    //        This way a directed graph is established.
    // ... e. It is allowed that the graph has loops.
    // ... f. If nodes x and y are connected via one or more (directed) edges,
    //        then another connection from y to x via one or more (directed)
    //        edges SHALL exist.

    // Observations:
    // 1. The PKI of any CVC-Root-CA looks like a tree-structure with the
    //    CVC-Root-CA at the root.
    // 2. Zero, one or more CVC-Sub-CA are nodes in such a tree-structure.
    // 3. End-Entity instances are leaves in such a tree-structure.
    // 4. From assumption f it follows that it is possible to reach any CVC-Root-CA
    //    from any other CVC-Root-CA.

    // Example:
    // ss = self-signed (certificate)
    // lxy = link-certificate issued by x for root y
    // CVC-Root-CA_1 issues ...
    // |
    // CVC-Root-CA_2 issues ss2, l23, l21
    // |
    // CVC-Root-CA_3 issues ss3, l32, l34
    // |           \ CVC-Sub-CA_3a as 3a
    // |                         \ End-Entity_3ai has 3ai
    // |
    // CVC-Root-CA_4 issues ss4, l43, l45
    // |
    // CVC-Root-CA_5 issues ...
    //
    // Given cvc=3ai and car=CAR(CVC-Root-CA_4) this results in the following chain:
    // 3ai <- 3a <- l43

    // The algorithm hereafter works as follows (see example):
    // Step 1: Create 2 lists and a pointer:
    //         a. ListA contains CVC, initialize with given CVC
    //         b. ListB contains indices, initialize with invalid value, i.e. -1
    //         c. Let a pointer point to the first element in ListA
    // Step 2: Search in cache for CVC which
    //         a. have a CHR equal to CAR of CVC pointed to by pointer and
    //         b. are not yet in ListA
    // Step 3: Add all CVC from step 2 to ListA
    // Step 4: For each CVC added in step 3 add an integer equal to index of
    //         pointer to ListB. This way the two lists form a tree-structure
    //         with the nodes of the tree in ListA and the corresponding
    //         element in ListB telling where to find its parent.
    // Step 5: Stop iteration if self-signed CVC-Root-CA with CAR equal to
    //         given "car" is element in ListA (successful end of iteration)
    // Step 6: Increment pointer and stop iteration if pointer points beyond
    //         ListA (unsuccessful end of iteration).
    // Step 7: Continue with step 2.

    // Working example, based on
    // - cvc = 3ai
    // - car = CAR(CVC-Root-CA_4)
    // Step 1: ListA = {3ai}, pointer points to 3ai
    //         ListB = "{-1 }"
    // Iteration 1: pointer points to element 0
    // Step 2: relevant CVC-list = "{3a}"
    // Step 3: ListA = "{3ai, 3a }"
    // Step 4: ListB = "{-1 ,  0 }"
    // Step 6,7: do not stop iteration

    // Iteration 2: pointer points to element 1
    // Step 2: relevant CVC-list = "{l23, ss3, l43}"
    // Step 3: ListA = "{3ai, 3a , l23, ss3, l43}"
    // Step 4: ListB = "{-1 ,  0 ,  1 ,  1 ,  1 }"
    // Step 6,7: do not stop iteration

    // Iteration 3: pointer points to element 2
    // Step 2: relevant CVC-list = "{l12, ss2, l32}"
    // Step 3: ListA = "{3ai, 3a , l23, ss3, l43, l12, ss2, l32}"
    // Step 4: ListB = "{-1 ,  0 ,  1 ,  1 ,  1 ,  2 ,  2 ,  2 }"
    // Step 6,7: do not stop iteration

    // Iteration 4: pointer points to element 3
    // Step 2: relevant CVC-list = {} (all CVC with CHR=CVC-Root-CA_3 are already in ListA)
    // Step 3: ListA = "{3ai, 3a , l23, ss3, l43, l12, ss2, l32}"
    // Step 4: ListB = "{-1 ,  0 ,  1 ,  1 ,  1 ,  2 ,  2 ,  2 }"
    // Step 6,7: do not stop iteration

    // Iteration 5: pointer points to element 4
    // Step 2: relevant CVC-list = "{l34, ss4, l54}"
    // Step 3: ListA = "{3ai, 3a , l23, ss3, l43, l12, ss2, l32, l34, ss4, l54}"
    // Step 4: ListB = "{-1 ,  0 ,  1 ,  1 ,  1 ,  2 ,  2 ,  2 ,  4,   4,   4 }"
    // Step 6: stop iteration, because self-signed CVC-Root-CA with CAR=CVC-Root-CA_4 is in ListA

    // Create chain:
    // ss4 has corresponding index 4 => l43 is the last element of the chain
    // l43 has corresponding index 1 => 3a  is the previous element of the chain
    // 3a  has corresponding index 0 => 3ai is the previous element of the chain
    // 3ai has negativ corresponding index => chain is complete: "{3ai, 3a, l43}"

    final List<Cvc> result = new ArrayList<>();
    if (cvc.isRootCa() && cvc.getCar().equals(cvc.getChr())) {
      return result;
    } // end fi

    final Cvc selfSignedRootCvc;
    if (car.isEmpty()) {
      // ... no specific CVC-Root-CA requested
      //     => value of selfSignedRootCvc does not matter
      selfSignedRootCvc = cvc;
    } else {
      // ... specific CVC-Root-CA requested
      selfSignedRootCvc =
          insCvc.stream()
              .filter(Cvc::isRootCa)
              .filter(i -> car.equals(i.getCar()))
              .filter(i -> car.equals(i.getChr()))
              .findAny()
              .orElseThrow(() -> new NoSuchElementException("CAR = \"" + car + '\''));
    } // end fi

    // Step 1: Create two lists and a pointer
    int pointer = -1;
    final List<Cvc> listA = new ArrayList<>(List.of(cvc));
    final List<Integer> listB = new ArrayList<>(List.of(pointer));
    for (; ; ) {
      // Step 6: Increment pointer and stop iteration if pointer points beyond
      //         ListA (unsuccessful end of iteration).
      if (listA.size() == ++pointer) { // NOPMD assignment in operand
        // ... pointer points beyond listA
        throw new IllegalArgumentException("unsuccessful search");
      } // end fi
      // ... pointer points not beyond listA

      final String currentCar = listA.get(pointer).getCar();

      // Step 2: Search in cache for CVC
      final List<Cvc> parents =
          insCvc.stream()
              .filter(i -> currentCar.equals(i.getChr()))
              .filter(i -> !listA.contains(i))
              .toList();

      // Step 3: Add all CVC from step 2 to ListA
      // Step 4: For each CVC added in step 3 add an integer to ListB
      final int finalPointer = pointer;
      parents.forEach(
          i -> {
            listA.add(i);
            listB.add(finalPointer);
          }); // end forEach(i -> ...)

      // Step 5: Stop iteration if self-signed CVC-Root-CA is an element in ListA
      final var breakFlag =
          car.isEmpty()
              ? listA.stream().filter(Cvc::isRootCa).anyMatch(i -> i.getCar().equals(i.getChr()))
              : listA.contains(selfSignedRootCvc);
      if (breakFlag) {
        break;
      } // end fi
    } // end For (;;)

    // Create chain:
    do {
      result.addFirst(listA.get(pointer));
      pointer = listB.get(pointer);
    } while (pointer >= 0);

    return result;
  } // end method */

  /**
   * Initializes cache content.
   *
   * <p>First the content of the cache is cleared. Then the cache-content is filled by
   * card-verifiable certificates read from given directory.
   *
   * <p>A {@link Cvc} is added to the result, if
   *
   * <ol>
   *   <li>the {@link Cvc} has no critical findings, AND
   *   <li>signature verification is successful.
   * </ol>
   *
   * @param root start folder for filling the cache
   * @return {@link Set} with {@link Cvc} for which signature verification was not possible, because
   *     no trusted public key was available
   */
  /* package */ Set<Cvc> initialize(final Path root) {
    LOGGER.atDebug().log("initialize: {}", root);
    LOGGER.atTrace().log("initialize: {}", root.toAbsolutePath().normalize());

    // --- clear cache content
    getCvc().clear();

    // --- retrieve CVC from the file system and collect them in an inputSet
    final Set<Cvc> inputSet = collectCvc(root);

    // Note 1: The concept of the following implementation is:
    //         a. TrustCenter.add(cvc): I.e. present the trust center each
    //            CVC from inputSet.
    //         b. If the trust center accepts it, then delete it from inputSet.
    // Note 2: This implementation has the following drawback: Say, the inputSet
    //         contains a CVC with critical findings and a VERY long chain from
    //         CVC-Root-CA over numerous CVC-Sub-CA to a CVC-end-entity it may
    //         happen that within each round of the for-loop just one CVC is
    //         accepted by trust center and all other CVC are rejected.
    //         In this case, TrustCenter.add(cvc)-method logs a lot of CVC over
    //         and over again.
    // Note 3: Code-duplication could avoid the drawback mentioned in note 2.
    //         I.e., by duplicating code we could check here as well:
    //         a. Has the CVC critical findings?
    //         b. Is the public key known which verifies the signature?
    //         c. Is the signature valid?
    // Note 4: Intentionally hereafter code-duplication is avoided.

    // --- check CVC from inputSet and move those with valid signatures to cache
    for (; ; ) {
      final Set<Cvc> transfer =
          inputSet.stream()
              .filter(TrustCenter::add)
              // ... cvc and its public key are now part of the cache
              //     => remove it from inputSet (after the current round)
              .collect(Collectors.toSet());

      // --- remove transferred CVC from inputSet
      inputSet.removeAll(transfer);

      if (inputSet.isEmpty() || transfer.isEmpty()) {
        // ... no more CVC to validate  OR  could not validate any additional CVC
        //     => stop loop
        break;
      } // end fi
      // ... more CVC in inputSet  AND  some CVC where transferred
    } // end For (; ; )

    return inputSet;
  } // end method */

  /**
   * Estimates (relative) path of given CV-certificate in PKI hierarchy.
   *
   * @param cvc {@link Cvc} for which the path is estimated
   * @return (relative) {@link Path} of given CV-certificate in PKI hierarchy, in particular (all
   *     references in human-readable form):
   *     <ol>
   *       <li>the path starts with the CAR of the CVC-Root-CA element
   *       <li>the path continues with CAR of the CVC-Sub-CA elements (if any)
   *       <li>the path ends with the CHR of the given CV-certificate. For CVC-Sub-CA the CHR
   *           representation contains the CA-name. For End-Entity-CV-certificates the CHR
   *           representation contains the ICCSN.
   *     </ol>
   *
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>cache contains no parent for given or intermediate CV-certificate
   *       <li>more than one parent exists for given or intermediate CV-certificate but none of them
   *           are self-signed CVC-Root-CA certificates
   *     </ol>
   */
  /* package */ Path path(final Cvc cvc) {
    if (cvc.isRootCa() && cvc.getCar().equals(cvc.getChr())) {
      // ... self-signed CVC-Root-CA
      return Path.of(cvc.getCarObject().getHumanReadable());
    } // end fi

    // --- get chain of certificates
    final List<Cvc> chain = getChain(cvc, "");

    // --- extract CHR in correct order
    final String[] pathElements = new String[chain.size() + 1];
    pathElements[0] = chain.getLast().getCarObject().getHumanReadable();
    int index = 1;
    for (int i = chain.size(); i-- > 0; ) { // NOPMD assignment in operands
      pathElements[index++] = chain.get(i).getChrObject().getHumanReadable();
    } // end For (i...)

    // --- convert the array of pathElements to a Path instance
    return Path.of("", pathElements);
  } // end method */

  /**
   * Getter.
   *
   * @return {@link Set} with cached {@link Cvc}
   */
  /* package */ Set<Cvc> getCvc() {
    return insCvc;
  } // end method */
} // end class
