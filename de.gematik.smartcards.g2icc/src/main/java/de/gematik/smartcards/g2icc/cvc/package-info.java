/*
 *  Copyright 2024 gematik GmbH
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

/**
 * This package provides functionality for handling card-verifiable certificates.
 *
 * <p>Actually this package supports CV-certificates according to gemSpec_PKI.
 *
 * <p><b>Concept:</b>
 *
 * <ol>
 *   <li>Card verifiable certificates (CVC) are handled by an immutable value-type class.
 *   <li>Public keys are cached. Initially that cache is filled by public keys imported from a
 *       file-system. All keys are imported.
 *   <li>CVC are cached. Initially that cache is filled by CVC imported from a file system. Only CVC
 *       with a valid signature are imported, other CVC are ignored.
 *   <li>A trustcenter uses the public key and CVC cache. After initialization the cache grow if
 *       further CVC are added. Only CVC with a valid signature are added to the cache.
 *   <li>This package relies on the following directory structure (by convention):
 *       <pre>
 *         {@code BaseDirectory}           : arbitrary directory (any name) in the file system
 *          +--- {@code input}             : CVC downloaded from CVC-Root-CA web site
 *          |     +--- {@code trust-anchor}: trusted public keys that come with the installation
 *          +--- {@code trusted}           : CVC, valid signature
 *          +--- {@code untrusted}         : CVC, missing public key for signature verification
 *          +--- {@code ...}               : arbitrary regular files or directories with CVC
 *       </pre>
 * </ol>
 *
 * <p><b>Concept card-verifiable certificate (CVC):</b>
 *
 * <p>{@link de.gematik.smartcards.g2icc.cvc.Cvc CvcG2} is an immutable value-type class and stores
 * properties of a card-verifiable certificate (CVC). That class also provides methods for checking
 * the content of the CVC.
 *
 * <p><b>Concept public key cache:</b>>
 *
 * <p>{@link de.gematik.smartcards.g2icc.cvc.CachePublicKey CachePublicKey} intentionally has a
 * visibility of "default" (i.e., package private). It is a mutable value-type class and caches a
 * {@link java.util.Map} with cardholder references (CHR) mapped to a public key. Apart from
 * artificial test scenarios, the relationship of CHR and public key is bijective, i.e., the
 * certification authority (CA) ensures that CHR are unique and the creation process of key-pairs
 * ensures that public keys are unique (with a very high probability).
 *
 * <p>The user of {@link de.gematik.smartcards.g2icc.cvc.CachePublicKey CachePublicKey} initializes
 * the public key cache by calling {@link
 * de.gematik.smartcards.g2icc.cvc.CachePublicKey#initialize(java.nio.file.Path) initialize(Path)}.
 * Within the given {@link java.nio.file.Path Path} (regular file or directory) and all regular
 * files in possibly existing subdirectories the initialisation method searches for regular files
 * with suffix {@link de.gematik.smartcards.g2icc.cvc.CachePublicKey#SUFFIX_PUK_DER
 * "_ELC-PublicKey.der"}.
 *
 * <p>The file name of those files is considered to have the format {@code "prefix.suffix"}. The
 * prefix is used as CHR of the public key enclosed in the file.
 *
 * <p>The octets in those files <b>SHALL</b> contain an elliptic curve public key in format {@link
 * de.gematik.smartcards.crypto.EafiElcPukFormat#ISOIEC7816 ISO/IEC 7816}, i.e.:
 *
 * <pre>7F49 - L
 * 06 - L - OID # identifying domain parameter
 * 86 - L - ... # public point in uncompressed format, see BST-TR-03111 v2.10 clause 3.2.1
 * </pre>
 *
 * <p>The user is able to add more keys to the public key cache.
 *
 * <p><b>Concept CVC cache:</b>>
 *
 * <p>{@link de.gematik.smartcards.g2icc.cvc.CacheCvc CacheCvc} intentionally has a visibility of
 * "default" (i.e., package private). It is a mutable value-type class and caches a {@link
 * java.util.Set Set} of arbitrary CVC. In particular: that {@link java.util.Set Set} holds
 *
 * <ol>
 *   <li>self-signed root certificates, i.e. a CVC where the flag list indicates "root" and CAR ==
 *       CHR,
 *   <li>link certificates, i.e. a CVC where the flag list indicates "root" and CAR differs from
 *       CHR,
 *   <li>sub-CA certificates, i.e. a CVC where the flag list indicates "sub-CA",
 *   <li>end entity certificates, i.e. a CVC where the flag list indicates "end entity".
 * </ol>
 *
 * <p>{@link de.gematik.smartcards.g2icc.cvc.CacheCvc CacheCvc} uses {@link
 * de.gematik.smartcards.g2icc.cvc.CachePublicKey CachePublicKey} for storing public keys.
 *
 * <p>The user of {@link de.gematik.smartcards.g2icc.cvc.CacheCvc CacheCvc} initializes the CVC
 * cache by calling {@link de.gematik.smartcards.g2icc.cvc.CacheCvc#initialize(java.nio.file.Path)
 * initialize(Path)}. First the public key cache is filled, then from all regular files in the given
 * directory a CVC is read. The content of the CVC is checked and the signature is verified. The
 * more CVC is successfully checked, the more public keys are known and trusted, and the more other
 * CVC can be imported into the cache. The name of files does not matter to importing CVC into the
 * cache. The octets in files <b>SHALL</b> encode a valid CVC. Files which do not match these
 * criteria are ignored. CVC with invalid signatures are ignored.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@DefaultAnnotation(NonNull.class) // help spotbugs
@ReturnValuesAreNonnullByDefault // help spotbugs
package de.gematik.smartcards.g2icc.cvc;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;
