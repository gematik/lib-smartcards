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

import static de.gematik.smartcards.g2icc.cvc.CachePublicKey.SUFFIX_PUK_DER;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static java.nio.charset.StandardCharsets.UTF_8;

import de.gematik.smartcards.crypto.EafiElcPukFormat;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for all uses cases around trust-center activities.
 *
 * <p>The following use cases are implemented:
 *
 * <ol>
 *   <li>{@link #initialize()}
 *   <li>{@link #initializeCache(Path)}
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE".
//         Spotbugs message: The return value from a method is dereferenced without
//         a null check, and the return value of that method is one that should
//         generally be checked for null.
//         Rational: That finding is suppressed because getFileName()-method
//         cannot return null here.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE" // see note 1
}) // */
@SuppressWarnings({"PMD.LongVariable", "PMD.AvoidUsingVolatile"})
public final class TrustCenter {

  /** Logger. */
  @VisibleForTesting
  /* package */ static final Logger LOGGER = LoggerFactory.getLogger(TrustCenter.class); // */

  /** Path to the configuration file. */
  public static final Path PATH_CONFIGURATION =
      Paths.get(
          System.getProperty("user.home"),
          ".config",
          "de.gematik.smartcards.g2icc.cvc.TrustCenter.properties"); // */

  /** Name of property with path to {@link TrustCenter}. */
  /* package */ static final String PROPERTY_PATH_TRUST_CENTER = "pathTrustCenter"; // */

  /**
   * Relative path to a directory with the anchors of trust.
   *
   * <p>Together with {@link #getPath()} it points to
   *
   * <ol>
   *   <li>a directory (in which case all regular files in this directory and all subdirectories are
   *       taken into account) or
   *   <li>a regular file (in which case there is at most one anchor of trust).
   * </ol>
   */
  public static final Path PATH_TRUST_ANCHOR = Path.of("input", "trust-anchor"); // */

  /**
   * Relative path to a directory where trusted card-verifiable certificates are stored.
   *
   * <p>Together with {@link #getPath()} it points to a directory where all card-verifiable
   * certificates are stored which have been successfully verified.
   */
  public static final Path PATH_TRUSTED = Path.of("trusted"); // */

  /**
   * Relative path to a directory where untrusted card-verifiable certificates are stored.
   *
   * <p>Together with {@link #getPath()} it points to a directory where all card-verifiable
   * certificates are stored which have <b>NOT</b> been successfully verified. Typically, either the
   * public key for signature verification is missing or the signature verification failed.
   */
  public static final Path PATH_UNTRUSTED = Path.of("untrusted"); // */

  /**
   * Relative path to a file acting as a flag.
   *
   * <p>If this file is present (regardless of its content) while method {@link
   * #initializeCache(Path)} executes, then later on end-entity card-verifiable certificates added
   * by {@link #add(Cvc)} are also stored on the filesystem.
   *
   * <p>If this file is absent while method {@link #initializeCache(Path)} executes, then no
   * end-entity card-verifiable certificate is stored on the filesystem while {@link #add(Cvc)}
   * executes.
   */
  public static final Path PATH_FLAG_STORE_END_ENTITY = Path.of("storeEndEntityCvc"); // */

  /** File suffix used for storing CVC in human-readable format. */
  public static final String SUFFIX_CVC_ASCII = "_CV-Certificate.txt"; // */

  /**
   * File suffix used for storing CVC content as binary DER.
   *
   * <p><i><b>Note:</b> The suffix ".cvc" is also used by <a
   * href="https://pki.atos.net/egk">Atos</a> when publishing card-verifiable certificates.</i>
   */
  public static final String SUFFIX_CVC_DER = "_CV-Certificate.cvc"; // */

  /** File suffix used for storing CVC content as ASCII-DER. */
  public static final String SUFFIX_CVC_DER_ASCII = "_CV-Certificate_DER.txt"; // */

  /**
   * File suffix used for storing an {@link EcPublicKeyImpl} key in human-readable form according to
   * {@link EcPublicKeyImpl#toString()}.
   */
  public static final String SUFFIX_PUK_ASCII = "_ELC-PublicKey.txt"; // */

  /**
   * File suffix used for storing an {@link EcPublicKeyImpl} as an ASCII DER-TLV structure according
   * to {@link EafiElcPukFormat#ISOIEC7816}.
   */
  public static final String SUFFIX_PUK_DER_ASCII = "_ELC-PublicKey_DER.txt"; // */

  /** {@link CacheCvc} used by this class. */
  @VisibleForTesting // otherwise = private
  /* package */ static final CacheCvc CACHE_CVC = new CacheCvc(); // */

  /** {@link CachePublicKey} used by this class. */
  @VisibleForTesting // otherwise = private
  /* package */ static final CachePublicKey CACHE_PUK = new CachePublicKey(); // */

  /**
   * Path to folder used as the Trust Center database.
   *
   * <p>The directory structure is described in {@code package-info.java}.
   *
   * <p>Value {@code NULL} indicates that this class attribute is not set and Thus, not usable.
   */
  private static @Nullable Path claPath; // */

  /**
   * Flag which determines if end-entity card-verifiable certificates are stored.
   *
   * <p>For more information, see {@link #PATH_FLAG_STORE_END_ENTITY}.
   */
  private static volatile boolean claFlagStoreEndEntity; // */

  /** Private default constructor. */
  private TrustCenter() {
    // intentionally empty
  } // end constructor */

  /**
   * Add a card-verifiable certificate to the database if it is "valid".
   *
   * <p>More formally: The given card-verifiable certificate (CVC) is checked. If the CVC contains
   * no critical errors and its signature is valid, then the given CVC is added to the cache of this
   * trust center.
   *
   * @param cvc card-verifiable certificate to be added to the cache of the trust-center
   * @return {@code TRUE} if CVC is added to the cache, {@code FALSE} otherwise
   */
  /* package */
  static boolean add(final Cvc cvc) {
    LOGGER
        .atTrace()
        .log(
            "add CVC with CAR={}, CHR={}",
            cvc.getCarObject().getHumanReadable(),
            cvc.getChrObject().getHumanReadable());

    boolean result = false;
    if (cvc.hasCriticalFindings()) {
      // ... critical findings
      //     => log
      LOGGER.atTrace().log("critical findings in CVC: {}", String.format("%n%s", cvc));
    } else {
      // ... no critical findings

      final Cvc.SignatureStatus signatureStatus = cvc.getSignatureStatus();
      if (Cvc.SignatureStatus.SIGNATURE_VALID.equals(signatureStatus)) {
        // ... valid signature
        //     => CVC is worthy to be added to cache
        result = true;
        LOGGER.atTrace().log("signature valid");

        CACHE_PUK.insCache.put(cvc.getChr(), cvc.getPublicKey());

        if (CACHE_CVC.add(cvc)
            && (null != claPath)
            && (isFlagStoreEndEntity() || !cvc.isEndEntity())) {
          // ...      cache did not contain the CVC before
          // ... AND  path properly set
          // ... AND  (storeAlsoEndEntity  OR  notEndEntity)
          //     => export it to the file system
          export(getPath().resolve(PATH_TRUSTED).resolve(CACHE_CVC.path(cvc)), cvc);
        } // end fi
      } else {
        // ... non-valid signature
        LOGGER.atTrace().log("{}", signatureStatus);
      } // end else
    } // end else (critical findings?)

    return result;
  } // end method */

  /**
   * Clear cache and unset path to file system.
   *
   * <p>This method unsets the path to the file system where card-verifiable certificates (CVC) are
   * persistently stored. Furthermore, all CVC and all public keys are removed from volatile cache.
   *
   * <p>This method never changes files or directories in persistent memory.
   *
   * <p>This is the inverse function of {@link #initializeCache(Path)}.
   */
  public static void clearCache() {
    claPath = null; // NOPMD assigning to null is a code smell

    CACHE_PUK.insCache.clear();
    CACHE_CVC.getCvc().clear();
  } // end method */

  /**
   * Export certificate content to given {@link Path}.
   *
   * <p>The following information is exported:
   *
   * <ol>
   *   <li>CVC content as TLV-structure in binary format
   *   <li>CVC content as TLV-structure in ASCII format
   *   <li>{@link Cvc#toString()}, i.e. with TLV-structure, explanation, report, etc.
   *   <li>public key from CVC
   *       <ul>
   *         <li>as TLV-structure in binary format
   *         <li>as TLV-structure in ASCII format
   *         <li>{@link EcPublicKeyImpl#toString()}
   *       </ul>
   * </ol>
   *
   * @param path directory to which information is exported
   * @param cvc certificate to be exported
   */
  @VisibleForTesting // otherwise = private
  /* package */ static void export(final Path path, final Cvc cvc) {
    // --- calculate file name prefix
    final String prefix = cvc.getChrObject().getHumanReadable();

    // --- calculate relevant CVC information
    final String cvcReport = cvc.toString();
    final ConstructedBerTlv tlvCvc = cvc.getCvc();

    // --- calculate relevant public key information
    final EcPublicKeyImpl puk = cvc.getPublicKey();
    final String pukReport = puk.toString();
    final ConstructedBerTlv tlvPuk = puk.getEncoded(EafiElcPukFormat.ISOIEC7816);

    Map.ofEntries(
            // export CVC as DER-TLV structure, binary format
            Map.entry(SUFFIX_CVC_DER, tlvCvc.getEncoded()),
            // export CVC as DER-TLV structure, ASCII format
            Map.entry(SUFFIX_CVC_DER_ASCII, tlvCvc.toString(" ", "   ").getBytes(UTF_8)),
            // export CVC with TLV-structure, explanation, report, etc.
            Map.entry(SUFFIX_CVC_ASCII, cvcReport.getBytes(UTF_8)),
            // export PuK as DER-TLV structure, binary format
            Map.entry(SUFFIX_PUK_DER, tlvPuk.getEncoded()),
            // export PuK as DER-TLV structure, ASCII format
            Map.entry(SUFFIX_PUK_DER_ASCII, tlvPuk.toString(" ", "   ").getBytes(UTF_8)),
            // export PuK as human-readable ASCII file
            Map.entry(SUFFIX_PUK_ASCII, pukReport.getBytes(UTF_8)))
        .forEach((suffix, content) -> fileWrite(path.resolve(prefix + suffix), content));
  } // end method */

  /**
   * Writes given {@code content} to given {@link Path}.
   *
   * @param path used to store content
   * @param content to be stored
   */
  @VisibleForTesting // otherwise = private
  /* package */ static void fileWrite(final Path path, final byte[] content) {
    try {
      final boolean writeFlag;
      if (Files.exists(path)) {
        // ... file exists (possibly a directory)
        //     => compare content

        if (Arrays.equals(
            content,
            Files.readAllBytes(path) // throws IOException in case path points to a directory
            )) {
          // ... file content equals byte[] from input parameter "content"
          //    => no need to write again
          writeFlag = false;
          LOGGER.atTrace().log("file already exists with appropriate content: \"{}\"", path);
        } else {
          // ... file content differs from input parameter "content"
          //     => overwrite
          writeFlag = true;
          LOGGER.atTrace().log("file content not appropriate: \"{}\"", path);
        } // end else
      } else {
        // ... file absent
        //    => create a file with appropriate content
        writeFlag = true;
        LOGGER.atTrace().log("file absent: \"{}\"", path);
      } // end else

      if (writeFlag) {
        // ... flag indicates that a write operation should occur
        //     => create parent directories (if necessary) and write
        Files.createDirectories(path.getParent()); // NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
        Files.write(path, content);
      } // end fi
    } catch (IOException e) {
      LOGGER.atWarn().log("trouble with path: \"{}\"", path);
      LOGGER.atDebug().log(UNEXPECTED, e);
    } // end Catch
  } // end method */

  /**
   * Estimates a CV-certificate chain from given one up to but not including the self-signed
   * CV-certificate with given {@code car}.
   *
   * <p>The returned list has the following properties:
   *
   * <ol>
   *   <li>the first element is the given one
   *   <li>element i is the CV-certificate of the issuer who issues element {@code (i-1)}
   *   <li>The last element is a non-self-signed CV-certificate where {@code CAR} equals the given
   *       one.
   * </ol>
   *
   * @param cvc CV-certificate for which the CVC-chain is requested
   * @param car of CVC-Root-CA where the chain ends
   * @return list of CV-certificates
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>given CV-certificate belongs to a CVC-Root-CA, more specific: {@link Cvc#isRootCa()}
   *           return {@code TRUE}.
   *       <li>no path from given CV-Certificate to CVC-Root-CA with given CAR is found
   *     </ol>
   *
   * @throws NoSuchElementException if no CVC-Root-CA with given CAR exists in the cache
   */
  public static List<Cvc> getChain(final Cvc cvc, final String car) {
    if (cvc.isRootCa()) {
      throw new IllegalArgumentException("CV-certificate for CVC-Root-CA");
    } // end fi

    return CACHE_CVC.getChain(cvc, car);
  } // end method */

  /**
   * Return parent CV-certificate.
   *
   * <p>More precisely: Returns a set with CV-certificates where CHR equals CAR of given
   * certificate. The set is empty if no parent is found. The set may contain more than one element.
   * Typically, this happens for CVC-Root-CA when self-signed certificates and link-certificates are
   * present in the cache.
   *
   * @param child CV-certificate for which parent(s) are requested
   * @return set with all known parents of the given child, possibly empty
   */
  public static Set<Cvc> getParent(final Cvc child) {
    final var childCar = child.getCar();

    return CACHE_CVC.getCvc().stream()
        .filter(i -> childCar.equals(i.getChr()))
        .collect(Collectors.toSet());
  } // end method */

  /**
   * Get public key of entity with given cardholder reference (CHR).
   *
   * <p>Typically for
   *
   * <ol>
   *   <li>end-entity CVC the CHR contains the ICCSN of the Smart Card the CVC belongs to.
   *   <li>CVC-Sub-CA the CHR identifies the CVC-Sub-CA and the CVC-Sub-CA uses its CHR as CAR in
   *       the CVC it publishes.
   *   <li>CVC-Root-CA the CHR identifies the CVC-Root-CA and the CVC-Root-CA uses its CHR as CAR in
   *       the CVC it publishes. If the CVC-Root-CA publishes a self-signed CVC then in that CVC CAR
   *       and CHR are identical.
   * </ol>
   *
   * @param chr cardholder reference (CHR) for which the public key is requested
   * @return {@link EcPublicKeyImpl} identified by {@code chr}
   * @throws NoSuchElementException if cache does not contain a key with given {@code chr}
   */
  public static EcPublicKeyImpl getPublicKey(final String chr) {
    return CACHE_PUK.getPublicKey(chr);
  } // end method */

  /**
   * Returns the path to the database on filesystem.
   *
   * @return path to the database on filesystem
   * @throws IllegalStateException if the path is not set by {@link #initializeCache(Path)}
   */
  public static Path getPath() {
    if (null == claPath) {
      throw new IllegalStateException("path to trust center not (yet) set");
    } // end fi

    return claPath;
  } // end method */

  /**
   * Getter.
   *
   * @return {@code TRUE} if end-entity card-verifiable certificates are stored on filesystem,
   *     otherwise {@code FALSE}
   */
  @VisibleForTesting // otherwise = private
  /* package */ static boolean isFlagStoreEndEntity() {
    return claFlagStoreEndEntity;
  } // end method */

  /**
   * Sets the database of trust-center to a directory with a path read from configuration.
   *
   * <p>Caches are filled with information from the directory the path from configuration points to.
   * In case the caches are already filled with information from that directory, then the caches are
   * NOT refilled (this method "does nothing").
   *
   * @throws NoSuchFileException if the configuration file is absent
   * @throws NullPointerException if the configuration file does not contain a property {@link
   *     #PROPERTY_PATH_TRUST_CENTER}
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>the configuration file does not contain a value {@link #PROPERTY_PATH_TRUST_CENTER}
   *       <li>{@code root} does not point to an existing directory
   *       <li>directory with trust anchor is absent
   *       <li>directory for trusted certificates is absent
   *     </ol>
   */
  public static void initialize() throws IOException {
    final var content = Files.readAllBytes(PATH_CONFIGURATION);
    final var properties = new Properties();
    properties.load(new ByteArrayInputStream(content));
    final var propertyPathTrustCenter = properties.getProperty(PROPERTY_PATH_TRUST_CENTER);
    final var root = Paths.get(propertyPathTrustCenter);
    final var abno = root.toAbsolutePath().normalize();

    if ((null == claPath) || (!abno.equals(claPath.toAbsolutePath().normalize()))) {
      initializeCache(root);
    } // end fi
  } // end method */

  /**
   * Sets the database of trust-center to given {@code root} directory.
   *
   * <p>In particular: Removes all public keys and certificates from caches and refills the caches
   * with information read from the given directory. Those actions are performed on each invocation,
   * even if this method is called twice with the same {@code root} parameter value.
   *
   * <p>This is the inverse function of {@link #clearCache()}.
   *
   * <p>The presence or absence of a regular file {@link #PATH_FLAG_STORE_END_ENTITY} controls if
   * end-entity card-verifiable certificates are stored on the file system when method {@link
   * #add(Cvc)} executes.
   *
   * @param root new directory used as the path
   * @throws IllegalArgumentException if
   *     <ol>
   *       <li>{@code root} does not point to an existing directory
   *       <li>directory with trust anchor is absent
   *       <li>directory for trusted certificates is absent
   *     </ol>
   */
  public static void initializeCache(final Path root) {
    final var trustAnchor = root.resolve(PATH_TRUST_ANCHOR);
    final var trusted = root.resolve(PATH_TRUSTED);

    if (Files.isDirectory(root) && Files.isDirectory(trustAnchor) && Files.isDirectory(trusted)) {
      // ... given path points to a directory
      //     => use that directory

      // --- set the class attribute such that no export occurs in method TrustCenter.add(Cvc)
      claPath = null; // NOPMD assigning to null is a code smell

      // --- initialize the cache with public keys with information from given directory
      CACHE_PUK.initialize(trustAnchor);

      // --- initialize cache with information from given directory
      final Set<Cvc> untrusted = CACHE_CVC.initialize(root);

      // --- export all untrusted CVC to the file system
      final var pu = root.resolve(PATH_UNTRUSTED);
      untrusted.forEach(cvc -> export(pu.resolve(CACHE_CVC.path(cvc)), cvc));

      // --- export all trusted CVC to the file system
      final var pt = root.resolve(PATH_TRUSTED);
      CACHE_CVC.getCvc().forEach(cvc -> export(pt.resolve(CACHE_CVC.path(cvc)), cvc));

      // --- set class attribute properly
      // Note: This class attribute is intentionally set after other actions in
      //       this method. Thus, valid CVC will be exported only after the cache
      //       is properly filled. Otherwise, method CacheCvc.path(Cvc) does not
      //       give proper return values.
      claPath = root;

      // --- set class attribute flagStoreEndEntity
      claFlagStoreEndEntity = Files.isRegularFile(root.resolve(PATH_FLAG_STORE_END_ENTITY));
      LOGGER.atInfo().log("FlagStoreEndEntity = {}", claFlagStoreEndEntity);
    } else {
      // ... path does not point to a directory
      //     => throw appropriate exception
      throw new IllegalArgumentException(
          String.format(
              "wrong path: <%s>, absolute: <%s>", root, root.toAbsolutePath().normalize()));
    } // end fi
  } // end method */
} // end class
