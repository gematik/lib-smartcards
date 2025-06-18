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

import de.gematik.smartcards.crypto.EafiElcPukFormat;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.tlv.BerTlv;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class caches {@link EcPublicKeyImpl}.
 *
 * <p>This class is used by {@link CacheCvc}.
 *
 * <p>From the perspective of this class instances are entity-types. Thus, neither {@link
 * #equals(Object)} nor {@link #hashCode()} is overwritten.
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
/* package */ final class CachePublicKey {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(CachePublicKey.class); // */

  /**
   * File suffix used for storing an {@link EcPublicKeyImpl} as a binary DER-TLV structure according
   * to {@link EafiElcPukFormat#ISOIEC7816}.
   */
  /* package */ static final String SUFFIX_PUK_DER = "_ELC-PublicKey.der"; // */

  /** Length of {@link #SUFFIX_PUK_DER}. */
  private static final int LENGTH_PUK_DER = SUFFIX_PUK_DER.length(); // */

  /** Mapping from cardholder reference (CHR) to {@link EcPublicKeyImpl}. */
  /* package */ final Map<String, EcPublicKeyImpl> insCache = new ConcurrentHashMap<>(); // */

  /**
   * Adds given key to cache.
   *
   * @param chr cardholder reference, identifier of the key
   * @param puk {@link EcPublicKeyImpl} to be added to the cache
   * @throws IllegalArgumentException if cache already contains a public key for given {@code chr}
   *     and the public key in cache differs form given one
   */
  /* package */ void add(final String chr, final EcPublicKeyImpl puk) {
    final EcPublicKeyImpl currentKey = insCache.get(chr);

    if (null == currentKey) {
      // ... currently no mapping => just insert a mapping into cache
      insCache.put(chr, puk);
    } else if (!currentKey.equals(puk)) {
      // ... currently mapping available  AND  puk != currentKey
      //     => exception
      throw new IllegalArgumentException("key to be inserted differs from current one: " + chr);
    } // end fi
  } // end method */

  /**
   * Retrieves {@link EcPublicKeyImpl} identified by given {@code chr} form cache.
   *
   * @param chr given cardholder reference (i.e. CHR == key identifier)
   * @return appropriate corresponding {@link EcPublicKeyImpl}
   * @throws NoSuchElementException if cache does not contain a key with given {@code chr}
   */
  /* package */ EcPublicKeyImpl getPublicKey(final String chr) {
    if (insCache.containsKey(chr)) {
      // ... key present
      //     => return corresponding value
      return insCache.get(chr);
    } // end fi
    // ... cache does not contain mapping for given CHR

    throw new NoSuchElementException("no public key for CHR = '" + chr + '\'');
  } // end method */

  /**
   * Initializes cache content.
   *
   * <p>At first the content of the cache is cleared.
   *
   * <p>Afterward the given folder and its subfolders are checked for regular files with suffix
   * {@link #SUFFIX_PUK_DER}. The cardholder reference (i.e. CHR == key identifier) is estimated
   * from the file name prefix. The {@link EcPublicKeyImpl} is taken from the file content.
   *
   * <p>A user calling this method <b>SHALL</b> implicitly trust all public keys in the given
   * directory-tree. Thus, public keys in the given directory-tree are called trust anchors.
   *
   * <p>A {@code WARNING} is logged in case walking the given directory-tree throws {@link
   * IOException}.
   *
   * <p>A {@code INFO} is logged in case
   *
   * <ol>
   *   <li>a file access to public key throws an exception, or
   *   <li>constructing a {@link EcPublicKeyImpl} from file content throws an exception.
   * </ol>
   *
   * <p>For more information set logging level to {@code DEBUG} or {@code TRACE}.
   *
   * @param root start folder for filling the cache with {@link EcPublicKeyImpl}.
   */
  /* package */ void initialize(final Path root) {
    // --- log something
    LOGGER.atDebug().log("initialize: {}", root);
    LOGGER.atTrace().log("initialize: {}", root.toAbsolutePath().normalize());

    // --- clear cache content
    insCache.clear();

    // --- fill cache
    try (Stream<Path> stream = Files.walk(root.toAbsolutePath())) {
      stream
          .filter(
              path -> // RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE
              path.getFileName() // NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
                      .toString()
                      .endsWith(SUFFIX_PUK_DER))
          .forEach(
              path -> {
                try {
                  final byte[] fileContent = Files.readAllBytes(path);
                  final BerTlv derContent = BerTlv.getInstance(fileContent);
                  final EcPublicKeyImpl puk =
                      new EcPublicKeyImpl(derContent, EafiElcPukFormat.ISOIEC7816);
                  final String fileName =
                      path.getFileName() // NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
                          .toString();
                  final String chr = fileName.substring(0, fileName.length() - LENGTH_PUK_DER);

                  add(chr, puk);
                } catch (Exception e) { // NOPMD catching generic exception
                  LOGGER.atInfo().log("trouble with path \"{}\"", path);
                  LOGGER.atDebug().log(UNEXPECTED, e);
                } // end Catch (...)
              }); // end forEach(path -> ...)
    } catch (IOException e) {
      LOGGER.atWarn().log("trouble with root \"{}\"", root);
      LOGGER.atDebug().log(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
