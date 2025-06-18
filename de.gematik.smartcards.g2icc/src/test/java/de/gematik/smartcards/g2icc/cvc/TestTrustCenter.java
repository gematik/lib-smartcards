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
import static de.gematik.smartcards.g2icc.cvc.TrustCenter.PATH_CONFIGURATION;
import static de.gematik.smartcards.g2icc.cvc.TrustCenter.PATH_FLAG_STORE_END_ENTITY;
import static de.gematik.smartcards.g2icc.cvc.TrustCenter.PATH_TRUSTED;
import static de.gematik.smartcards.g2icc.cvc.TrustCenter.PATH_TRUST_ANCHOR;
import static de.gematik.smartcards.g2icc.cvc.TrustCenter.SUFFIX_CVC_ASCII;
import static de.gematik.smartcards.g2icc.cvc.TrustCenter.SUFFIX_CVC_DER;
import static de.gematik.smartcards.g2icc.cvc.TrustCenter.SUFFIX_CVC_DER_ASCII;
import static de.gematik.smartcards.g2icc.cvc.TrustCenter.SUFFIX_PUK_ASCII;
import static de.gematik.smartcards.g2icc.cvc.TrustCenter.SUFFIX_PUK_DER_ASCII;
import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;
import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.gematik.smartcards.crypto.AfiElcParameterSpec;
import de.gematik.smartcards.crypto.EafiElcPukFormat;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Class performing white-box tests for {@link TrustCenter}.
 *
 * <p>Assertions:
 *
 * <ol>
 *   <li>the configuration file {@link TrustCenter#PATH_CONFIGURATION} is present
 *   <li>the configuration file contains a property {@link TrustCenter#PROPERTY_PATH_TRUST_CENTER}
 *   <li>property {@link TrustCenter#PROPERTY_PATH_TRUST_CENTER} points to a directory which can be
 *       successfully used in {@link TrustCenter#initializeCache(Path)}
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", i.e.
//         Spotbugs message: Non-null field insTempDirectory is not initialized
//         This finding is for an attribute which is initialized by JUnit.
// Note 2: Spotbugs claims "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE".
//         Spotbugs message: The return value from a method is dereferenced without
//         a null check, and the return value of that method is one that should
//         generally be checked for null.
//         Rational: That finding is suppressed because getFileName()-method
//         cannot return null here.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", // see note 1
  "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE" // see note 2
}) // */
// Note: This class is disabled for maven-builds, because maven claims:
//       java.lang.IllegalAccessError:
//       class de.gematik.smartcards.g2icc.cvc.TestTrustCenter
//       (in module de.gematik.smartcards.g2icc)
//       cannot access class ch.qos.logback.classic.Logger
//       (in module ch.qos.logback.classic)
//       because module de.gematik.smartcards.g2icc does not read module
//       ch.qos.logback.classic
@EnabledIfSystemProperty(named = "user.name", matches = "alfred")
@SuppressWarnings({
  "PMD.AvoidDuplicateLiterals",
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TooManyMethods",
  "PMD.TooManyStaticImports",
  "checkstyle.methodname"
})
final class TestTrustCenter {

  /** Random Number Generator. */
  private static final AfiRng RNG = new AfiRng(); // */

  /** Logger. */
  private final Logger insLogger = (Logger) TrustCenter.LOGGER; // */

  /** Appender used for logging. */
  private final ListAppender<ILoggingEvent> insAppender = new ListAppender<>(); // */

  /** Temporary directory. */
  @org.junit.jupiter.api.io.TempDir private Path insTempDirectory; // */

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
    // start the appender
    insAppender.start();

    // add the appender
    insLogger.addAppender(insAppender);
  } // end method */

  /** Method executed after each test. */
  @AfterEach
  void tearDown() {
    insAppender.stop();
    insLogger.detachAppender(insAppender);
  } // end method */

  /** Test method for {@link TrustCenter#add(Cvc)}. */
  @SuppressWarnings({"PMD.NcssCount"})
  @Test
  void test_add__Cvc() {
    // Assertions:
    // ... a. initialize(Path)-method works as expected
    // ... b. export(Path, Cvc)-method works as expected

    // Test strategy:
    // --- a. initialize one trust-anchor
    // --- b. add a self-signed CVC-Root-CA
    // --- c. add a link certificate
    // --- d. add self-signed CVC-Root-CA
    // --- e. add a CVC-Sub-CA
    // --- f. add a CVC-End-Entity (flagStoreEndEntity is false)
    // --- g. set "flagStoreEndEntity" to true
    // --- h. add a CVC-End-Entity (flagStoreEndEntity is true)
    // --- i. add CVC again
    // --- j. CVC with critical findings
    // --- k. CVC with an unknown signature verification key
    // --- l. CVC with invalid signature

    try {
      final var root = insTempDirectory.resolve("test_add__Cvc");
      final var trustAnchor = root.resolve(PATH_TRUST_ANCHOR);
      final var trusted = root.resolve(PATH_TRUSTED);
      Files.createDirectories(trustAnchor);
      Files.createDirectories(trusted);

      // --- a. initialize one trust-anchor
      final var rca1ChrString =
          Hex.toHexDigits("DEXXX".getBytes(UTF_8))
              + "8" // ServiceIndicator
              + "0" // discretionary data, level 0
              + "02" // algorithmReference of an elliptic curve
              + "24"; // year
      final var rca1Prk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP384r1);
      Files.write(
          trustAnchor.resolve(rca1ChrString + SUFFIX_PUK_DER),
          rca1Prk.getPublicKey().getEncoded(EafiElcPukFormat.ISOIEC7816).getEncoded());
      assertDoesNotThrow(() -> TrustCenter.initializeCache(root));
      assertFalse(TrustCenter.isFlagStoreEndEntity());

      // --- b. add a self-signed CVC-Root-CA
      Path tmp;
      final var rca1Car = new CertificationAuthorityReference(rca1ChrString);
      final var rca1Chr = new CardHolderReference(rca1ChrString);
      final var rca1OidPukUsage = AfiOid.ECDSA_with_SHA384;
      final var rca1OidFlagList = AfiOid.CVC_FlagList_TI;
      final var rca1FlagList = Hex.toByteArray("ff ffff ffff ffff");
      final var rca1Ced = new CertificateDate(true, LocalDate.parse("2024-04-29").minusDays(2));
      final var rca1Cxd = new CertificateDate(false, rca1Ced.getDate().plusYears(10));
      final var rca1Cvc =
          new Cvc(
              new CertificateProfileIndicator(),
              rca1Car,
              rca1OidPukUsage,
              rca1Prk.getPublicKey(),
              rca1Chr,
              rca1OidFlagList,
              rca1FlagList,
              rca1Ced,
              rca1Cxd,
              rca1Prk);

      assertTrue(TrustCenter.add(rca1Cvc));

      // check one file from first CVC-Root-CA
      assertTrue(
          Files.isRegularFile(
              tmp =
                  trusted
                      .resolve(rca1Chr.getHumanReadable())
                      .resolve(rca1Chr.getHumanReadable() + SUFFIX_CVC_DER_ASCII)));
      assertEquals(rca1Cvc.getCvc().toString(" ", "   "), Files.readString(tmp, UTF_8));
      // end --- b.

      // --- c. add a link certificate
      final var rca2ChrString =
          Hex.toHexDigits("DEXXX".getBytes(UTF_8))
              + "8" // ServiceIndicator
              + "0" // discretionary data, level 0
              + "02" // algorithmReference of an elliptic curve
              + "25"; // year
      final var rca2Prk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP512r1);
      final var rca2Car = new CertificationAuthorityReference(rca1ChrString);
      final var rca2Chr = new CardHolderReference(rca2ChrString);
      final var rca2OidPukUsage = AfiOid.ECDSA_with_SHA512;
      final var rca2OidFlagList = AfiOid.CVC_FlagList_TI;
      final var rca2FlagList = Hex.toByteArray("ff ffff ffff ffff");
      final var rca2Ced = new CertificateDate(true, LocalDate.parse("2024-04-29").plusYears(1));
      final var rca2Cxd = new CertificateDate(false, rca2Ced.getDate().plusYears(10));
      final var rca2Link =
          new Cvc(
              new CertificateProfileIndicator(),
              rca2Car,
              rca2OidPukUsage,
              rca2Prk.getPublicKey(),
              rca2Chr,
              rca2OidFlagList,
              rca2FlagList,
              rca2Ced,
              rca2Cxd,
              rca1Prk);

      assertTrue(TrustCenter.add(rca2Link));

      // check one file from link CVC-Root-CA
      assertTrue(
          Files.isRegularFile(
              tmp =
                  trusted
                      .resolve(
                          new CertificationAuthorityReference(rca1ChrString).getHumanReadable())
                      .resolve(rca2Chr.getHumanReadable())
                      .resolve(rca2Chr.getHumanReadable() + SUFFIX_CVC_DER_ASCII)));
      assertEquals(rca2Link.getCvc().toString(" ", "   "), Files.readString(tmp, UTF_8));
      // end --- c.

      // --- d. add self-signed CVC-Root-CA
      final var rca2Self =
          new Cvc(
              new CertificateProfileIndicator(),
              new CertificationAuthorityReference(rca2ChrString),
              rca2OidPukUsage,
              rca2Prk.getPublicKey(),
              rca2Chr,
              rca2OidFlagList,
              rca2FlagList,
              rca2Ced,
              rca2Cxd,
              rca2Prk);

      assertTrue(TrustCenter.add(rca2Self));

      // check one file from second CVC-Root-CA
      assertTrue(
          Files.isRegularFile(
              tmp =
                  trusted
                      .resolve(rca2Chr.getHumanReadable())
                      .resolve(rca2Chr.getHumanReadable() + SUFFIX_CVC_DER_ASCII)));
      assertEquals(rca2Self.getCvc().toString(" ", "   "), Files.readString(tmp, UTF_8));
      // end --- d.

      // --- e. add a CVC-Sub-CA
      final var subChrString =
          Hex.toHexDigits("DEXXX".getBytes(UTF_8))
              + "8" // ServiceIndicator
              + "1" // discretionary data, level 1
              + "02" // algorithmReference of an elliptic curve
              + "24"; // year
      final var subChr = new CardHolderReference(subChrString);
      final var subOidPukUsage = AfiOid.ECDSA_with_SHA384;
      final var subPrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP384r1);
      final var subOidFlagList = AfiOid.CVC_FlagList_TI;
      final var subFlagList = Hex.toByteArray("80 0000 0000 ffff");
      final var subCed = new CertificateDate(true, rca1Ced.getDate().plusDays(1));
      final var subCxd = new CertificateDate(false, subCed.getDate().plusYears(8));
      final var subCvc =
          new Cvc(
              new CertificateProfileIndicator(),
              rca1Car,
              subOidPukUsage,
              subPrk.getPublicKey(),
              subChr,
              subOidFlagList,
              subFlagList,
              subCed,
              subCxd,
              rca1Prk);

      assertTrue(TrustCenter.add(subCvc));

      assertTrue(
          Files.isRegularFile(
              tmp =
                  trusted
                      .resolve(rca1Chr.getHumanReadable())
                      .resolve(subChr.getHumanReadable())
                      .resolve(subChr.getHumanReadable() + SUFFIX_CVC_DER)));
      assertEquals(
          Hex.toHexDigits(subCvc.getCvc().getEncoded()), Hex.toHexDigits(Files.readAllBytes(tmp)));
      // end --- e.

      // --- f. add a CVC-End-Entity (flagStoreEndEntity is false)
      {
        final var eeChrString =
            "abcd" // discretionary data
                + "80" // Major Industrie Identifier
                + "276" // country code
                + "12345" // Issuer Identifier
                + "0102030405"; // Serial Number
        final var eeChr = new CardHolderReference(eeChrString);
        final var eeOidPukUsage = AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256;
        final var eePrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1);
        final var eeOidFlagList = AfiOid.CVC_FlagList_TI;
        final var eeFlagList = Hex.toByteArray("00 0000 0000 8421");
        final var eeCed = new CertificateDate(true, subCed.getDate().plusDays(1));
        final var eeCxd = new CertificateDate(false, subCed.getDate().plusYears(5));
        final var eeCvc =
            new Cvc(
                new CertificateProfileIndicator(),
                new CertificationAuthorityReference(subChrString),
                eeOidPukUsage,
                eePrk.getPublicKey(),
                eeChr,
                eeOidFlagList,
                eeFlagList,
                eeCed,
                eeCxd,
                subPrk);

        assertTrue(TrustCenter.add(eeCvc));

        assertFalse(
            Files.exists(
                trusted
                    .resolve(rca1Chr.getHumanReadable())
                    .resolve(subChr.getHumanReadable())
                    .resolve(eeChr.getHumanReadable())
                    .resolve(eeChr.getHumanReadable() + SUFFIX_PUK_ASCII)));
      } // end --- f.

      // --- g. set "flagStoreEndEntity" to true
      {
        final var flagStore = root.resolve(PATH_FLAG_STORE_END_ENTITY);
        Files.write(flagStore, AfiUtils.EMPTY_OS);

        assertDoesNotThrow(() -> TrustCenter.initializeCache(root));

        assertTrue(TrustCenter.isFlagStoreEndEntity());
      } // end --- g.

      // --- h. add a CVC-End-Entity (flagStoreEndEntity is true)
      {
        final var eeChr =
            new CardHolderReference(
                "abcd" // discretionary data
                    + "80" // Major Industrie Identifier
                    + "276" // country code
                    + "12345" // Issuer Identifier
                    + "5544332211" // Serial Number
                );
        final var eeOidPukUsage = AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256;
        final var eePrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1);
        final var eeOidFlagList = AfiOid.CVC_FlagList_TI;
        final var eeFlagList = Hex.toByteArray("00 0000 0000 8421");
        final var eeCed = new CertificateDate(true, subCed.getDate().plusDays(1));
        final var eeCxd = new CertificateDate(false, subCed.getDate().plusYears(5));
        final var eeCvc =
            new Cvc(
                new CertificateProfileIndicator(),
                new CertificationAuthorityReference(subChrString),
                eeOidPukUsage,
                eePrk.getPublicKey(),
                eeChr,
                eeOidFlagList,
                eeFlagList,
                eeCed,
                eeCxd,
                subPrk);

        assertTrue(TrustCenter.add(eeCvc));

        assertTrue(
            Files.isRegularFile(
                tmp =
                    trusted
                        .resolve(rca1Chr.getHumanReadable())
                        .resolve(subChr.getHumanReadable())
                        .resolve(eeChr.getHumanReadable())
                        .resolve(eeChr.getHumanReadable() + SUFFIX_PUK_ASCII)));
        assertEquals(eePrk.getPublicKey().toString(), Files.readString(tmp, UTF_8));

        // --- i. add CVC again
        // Note: Here CACHE_CVC.add(...) returns false, but the
        //       method-under-test still returns true.
        assertTrue(TrustCenter.add(eeCvc));
      } // end --- h, i.

      // --- j. CVC with critical findings
      {
        final var eeChr =
            new CardHolderReference(
                "abcdef" // discretionary data too long
                    + "80" // Major Industrie Identifier
                    + "276" // country code
                    + "12345" // Issuer Identifier
                    + "5544332211" // Serial Number
                );
        final var eeOidPukUsage = AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256;
        final var eePrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1);
        final var eeOidFlagList = AfiOid.CVC_FlagList_TI;
        final var eeFlagList = Hex.toByteArray("00 0000 0000 8421");
        final var eeCed = new CertificateDate(true, subCed.getDate().plusDays(1));
        final var eeCxd = new CertificateDate(false, subCed.getDate().plusYears(5));
        final var eeCvc =
            new Cvc(
                new CertificateProfileIndicator(),
                new CertificationAuthorityReference(subChrString),
                eeOidPukUsage,
                eePrk.getPublicKey(),
                eeChr,
                eeOidFlagList,
                eeFlagList,
                eeCed,
                eeCxd,
                subPrk);
        assertTrue(eeCvc.hasCriticalFindings());

        assertFalse(TrustCenter.add(eeCvc));
      } // end --- j.

      // --- k. CVC with an unknown signature verification key
      {
        final var eeChr =
            new CardHolderReference(
                "abcd" // discretionary data
                    + "80" // Major Industrie Identifier
                    + "276" // country code
                    + "12345" // Issuer Identifier
                    + "5544332211" // Serial Number
                );
        final var eeOidPukUsage = AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256;
        final var eePrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1);
        final var eeOidFlagList = AfiOid.CVC_FlagList_TI;
        final var eeFlagList = Hex.toByteArray("00 0000 0000 8421");
        final var eeCed = new CertificateDate(true, subCed.getDate().plusDays(1));
        final var eeCxd = new CertificateDate(false, subCed.getDate().plusYears(5));
        final var eeCvc =
            new Cvc(
                new CertificateProfileIndicator(),
                new CertificationAuthorityReference(Hex.extractHexDigits("4445414141 8 1 02 24")),
                eeOidPukUsage,
                eePrk.getPublicKey(),
                eeChr,
                eeOidFlagList,
                eeFlagList,
                eeCed,
                eeCxd,
                new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1));
        assertEquals(Cvc.SignatureStatus.SIGNATURE_NO_PUBLIC_KEY, eeCvc.getSignatureStatus());
        assertFalse(
            eeCvc.hasCriticalFindings(), () -> String.join(LINE_SEPARATOR, eeCvc.getReport()));

        assertFalse(TrustCenter.add(eeCvc));
      } // end --- k.

      // --- l. CVC with invalid signature
      {
        final var eeChr =
            new CardHolderReference(
                "abcd" // discretionary data
                    + "80" // Major Industrie Identifier
                    + "276" // country code
                    + "12345" // Issuer Identifier
                    + "0504030201" // Serial Number
                );
        final var eeOidPukUsage = AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256;
        final var eePrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1);
        final var eeOidFlagList = AfiOid.CVC_FlagList_TI;
        final var eeFlagList = Hex.toByteArray("00 0000 0000 8412");
        final var eeCed = new CertificateDate(true, subCed.getDate().plusDays(1));
        final var eeCxd = new CertificateDate(false, subCed.getDate().plusYears(5));
        final var eeCvc =
            new Cvc(
                new CertificateProfileIndicator(),
                new CertificationAuthorityReference(subChrString),
                eeOidPukUsage,
                eePrk.getPublicKey(),
                eeChr,
                eeOidFlagList,
                eeFlagList,
                eeCed,
                eeCxd,
                new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1));
        assertEquals(Cvc.SignatureStatus.SIGNATURE_INVALID, eeCvc.getSignatureStatus());

        assertFalse(TrustCenter.add(eeCvc));
      } // end --- l.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link TrustCenter#clearCache()}. */
  @Test
  void test_clearCache() {
    // Assertions:
    // ... a. initialize()-method works as expected

    // Test strategy:
    // --- a. fill cache with something useful
    // --- b. smoke test

    // --- a. fill cache with something useful
    {
      assertDoesNotThrow(TrustCenter::initialize);

      assertFalse(TrustCenter.CACHE_PUK.insCache.isEmpty());
      assertFalse(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    } // end --- a.

    // --- b. smoke test
    {
      assertDoesNotThrow(TrustCenter::clearCache);

      assertThrows(IllegalStateException.class, TrustCenter::getPath);
      assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
      assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
    } // end --- b.
  } // end method */

  /** Test method for {@link TrustCenter#export(Path, Cvc)}. */
  @Test
  void test_export__Path_Cvc() {
    // Assertions:
    // ... a. initialize(Path)-method works as expected

    // Test strategy:
    // --- a. smoke test

    try {
      final var root = insTempDirectory.resolve("test_export__Path_Cvc");
      final var trustAnchor = root.resolve(PATH_TRUST_ANCHOR);
      final var trusted = root.resolve(PATH_TRUSTED);
      final var rcaChrString =
          Hex.toHexDigits("DEXXX".getBytes(UTF_8))
              + "8" // ServiceIndicator
              + "0" // discretionary data, level 0
              + "02" // algorithmReference of an elliptic curve
              + "24"; // year
      final var rcaCar = new CertificationAuthorityReference(rcaChrString);
      final var rcaChr = new CardHolderReference(rcaChrString);
      final var rcaOidPukUsage = AfiOid.ECDSA_with_SHA512;
      final var rcaPrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP512r1);
      final var rcaOidFlagList = AfiOid.CVC_FlagList_TI;
      final var rcaFlagList = Hex.toByteArray("c0 0000 0000 ffff");
      final var rcaCed = new CertificateDate(true, LocalDate.parse("2024-04-29").minusDays(2));
      final var rcaCxd = new CertificateDate(false, rcaCed.getDate().plusYears(10));
      final var rcaCvc =
          new Cvc(
              new CertificateProfileIndicator(),
              rcaCar,
              rcaOidPukUsage,
              rcaPrk.getPublicKey(),
              rcaChr,
              rcaOidFlagList,
              rcaFlagList,
              rcaCed,
              rcaCxd,
              rcaPrk);
      Files.createDirectories(trusted);
      Files.createDirectories(trustAnchor);
      final var pathInput = trustAnchor.getParent();
      Files.write(pathInput.resolve(rcaChrString + SUFFIX_CVC_DER), rcaCvc.getCvc().getEncoded());
      Files.write(
          trustAnchor.resolve(rcaChrString + SUFFIX_PUK_DER),
          rcaPrk.getPublicKey().getEncoded(EafiElcPukFormat.ISOIEC7816).getEncoded());

      // --- a. smoke test
      {
        final var subChrString =
            Hex.toHexDigits("DEXXX".getBytes(UTF_8))
                + "8" // ServiceIndicator
                + "1" // discretionary data, level 1
                + "02" // algorithmReference of an elliptic curve
                + "24"; // year
        final var subChr = new CardHolderReference(subChrString);
        final var subOidPukUsage = AfiOid.ECDSA_with_SHA384;
        final var subPrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP384r1);
        final var subOidFlagList = AfiOid.CVC_FlagList_TI;
        final var subFlagList = Hex.toByteArray("80 0000 0000 8412");
        final var subCed = new CertificateDate(true, rcaCed.getDate().plusDays(1));
        final var subCxd = new CertificateDate(false, subCed.getDate().plusYears(8));
        final var subCvc =
            new Cvc(
                new CertificateProfileIndicator(),
                rcaCar,
                subOidPukUsage,
                subPrk.getPublicKey(),
                subChr,
                subOidFlagList,
                subFlagList,
                subCed,
                subCxd,
                rcaPrk);
        TrustCenter.initializeCache(root);

        TrustCenter.export(trusted, subCvc);

        Path tmp;
        assertTrue(
            Files.isRegularFile(tmp = trusted.resolve(subChr.getHumanReadable() + SUFFIX_CVC_DER)));
        assertEquals(subCvc.getCvc().toString(), Hex.toHexDigits(Files.readAllBytes(tmp)));
        assertTrue(
            Files.isRegularFile(
                tmp = trusted.resolve(subChr.getHumanReadable() + SUFFIX_CVC_DER_ASCII)));
        assertEquals(subCvc.getCvc().toString(" ", "   "), Files.readString(tmp, UTF_8));
        assertTrue(
            Files.isRegularFile(
                tmp = trusted.resolve(subChr.getHumanReadable() + SUFFIX_CVC_ASCII)));
        assertEquals(subCvc.toString(), Files.readString(tmp, UTF_8));
        assertTrue(
            Files.isRegularFile(tmp = trusted.resolve(subChr.getHumanReadable() + SUFFIX_PUK_DER)));
        assertEquals(
            subPrk.getPublicKey().getEncoded(EafiElcPukFormat.ISOIEC7816).toString(),
            Hex.toHexDigits(Files.readAllBytes(tmp)));
        assertTrue(
            Files.isRegularFile(
                tmp = trusted.resolve(subChr.getHumanReadable() + SUFFIX_PUK_DER_ASCII)));
        assertEquals(
            subPrk.getPublicKey().getEncoded(EafiElcPukFormat.ISOIEC7816).toString(" ", "   "),
            Files.readString(tmp, UTF_8));
        assertTrue(
            Files.isRegularFile(
                tmp = trusted.resolve(subChr.getHumanReadable() + SUFFIX_PUK_ASCII)));
        assertEquals(subPrk.getPublicKey().toString(), Files.readString(tmp, UTF_8));
      } // end --- a.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link TrustCenter#fileWrite(Path, byte[])}. */
  @Test
  void test_fileWrite__Path_byteA() {
    // Assertions:
    // ... a. The log level in "logback-test.xml" for package "de.gematik.smartcards" is
    //        set to "TRACE"

    // Test strategy:
    // --- a. write to a non-existent file
    // --- b. write to the same file, same content
    // --- c. write to the same file, different content
    // --- d. write with the path pointing to a directory

    final byte[] contentA = RNG.nextBytes(32, 64);
    final byte[] contentB = RNG.nextBytes(72, 96);
    final Path path = insTempDirectory.resolve("foo").resolve("bar").resolve("afi.bin");
    ILoggingEvent logEvent;

    try {
      // --- a. write to a non-existent file
      Files.deleteIfExists(path);
      assertFalse(Files.exists(path));
      TrustCenter.fileWrite(path, contentA);
      assertTrue(Files.isRegularFile(path));
      assertEquals(Hex.toHexDigits(contentA), Hex.toHexDigits(Files.readAllBytes(path)));
      assertEquals(1, insAppender.list.size()); // INFO: see assertion a.
      logEvent = insAppender.list.getFirst();
      assertEquals(Level.TRACE, logEvent.getLevel());
      assertEquals(String.format("file absent: \"%s\"", path), logEvent.getFormattedMessage());

      // --- b. write to the same file, same content
      TrustCenter.fileWrite(path, contentA);
      assertEquals(2, insAppender.list.size());
      logEvent = insAppender.list.get(1);
      assertEquals(Level.TRACE, logEvent.getLevel());
      assertEquals(
          String.format("file already exists with appropriate content: \"%s\"", path),
          logEvent.getFormattedMessage());

      // --- c. write to the same file, different content
      TrustCenter.fileWrite(path, contentB);
      assertEquals(3, insAppender.list.size());
      logEvent = insAppender.list.get(2);
      assertEquals(Level.TRACE, logEvent.getLevel());
      assertEquals(
          String.format("file content not appropriate: \"%s\"", path),
          logEvent.getFormattedMessage());

      // --- d. write with the path pointing to a directory
      TrustCenter.fileWrite(path.getParent(), contentA); // NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE
      assertEquals(5, insAppender.list.size());
      logEvent = insAppender.list.get(3);
      assertEquals(Level.WARN, logEvent.getLevel());
      assertEquals(
          String.format("trouble with path: \"%s\"", path.getParent()),
          logEvent.getFormattedMessage());
      logEvent = insAppender.list.get(4);
      assertEquals(Level.DEBUG, logEvent.getLevel());
      assertEquals(UNEXPECTED, logEvent.getMessage());
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link TrustCenter#getChain(Cvc, String)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: getChain")
  @Test
  void test_getChain__Cvc_String() {
    fail("not (yet) implemented: getChain"); // TODO
  } // end method */

  /** Test method for {@link TrustCenter#getParent(Cvc)}. */
  @org.junit.jupiter.api.Disabled("not (yet) implemented: getParent")
  @Test
  void test_getParent__Cvc() {
    fail("not (yet) implemented: getParent"); // TODO
  } // end method */

  /** Test method for {@link TrustCenter#getPublicKey(String)}. */
  @Test
  void test_getPublicKey__String() {
    // Assertions:
    // ... a. initialize()-method works as expected

    // Note: This simple method does not need extensive testing,
    //       so we can be lazy here and concentrate on code-coverage.

    // Test strategy:
    // --- a. use CHR of key in cache
    // --- b. ERROR: CHR is unknown in cache

    assertDoesNotThrow(TrustCenter::initialize);

    // --- a. use CHR of key in cache
    {
      final var chr = "44455a4757850222"; // DEZGW_8-5-02-22
      final var expected =
          new EcPublicKeyImpl(
              BerTlv.getInstance(
                  """
                      7f49 4e
                         06 09 2b2403030208010107
                         86 41 0462df8742130fed6b8258aef76627aba1bc8f0d35122d7a09992518cd05f6318e
                                 9d7d89bbcc60e5b4608292c1b3fbda89ee8a3406069f038c6722c8d5aad5f2d3
                      """),
              EafiElcPukFormat.ISOIEC7816);

      final var present = TrustCenter.getPublicKey(chr);

      assertEquals(expected, present);
    } // end --- a.

    // --- b. ERROR: CHR is unknown in cache
    {
      assertThrows(NoSuchElementException.class, () -> TrustCenter.getPublicKey("foo"));
    } // end --- b.
  } // end method */

  /** Test method for {@link TrustCenter#getPath()}. */
  @Test
  void test_getPath() {
    // Assertions:
    // ... a. initialize()-method works as expected

    // Test strategy:
    // --- a. smoke test with no path set
    // --- b. smoke test with path set

    assertDoesNotThrow(TrustCenter::initialize);
    final var root = TrustCenter.getPath();
    assertDoesNotThrow(TrustCenter::clearCache);

    // --- a. smoke test with no path set
    {
      assertThrows(IllegalStateException.class, TrustCenter::getPath);
    } // end --- a.

    // --- b. smoke test with path set
    {
      assertDoesNotThrow(TrustCenter::initialize);

      assertEquals(root, TrustCenter.getPath());
    } // end --- b.
  } // end method */

  /** Test method for {@link TrustCenter#isFlagStoreEndEntity()}. */
  @Test
  void test_isFlagStoreEndEntity() {
    // Assertions:
    // ... a. initialize(Path)-method works as expected

    // Test strategy:
    // --- a. empty directories
    // --- b. just "flagStoreEndEntity" present

    try {
      final var root = insTempDirectory.resolve("test_isFlagStoreEndEntity");
      final var trustAnchor = root.resolve(PATH_TRUST_ANCHOR);
      final var trusted = root.resolve(PATH_TRUSTED);
      final var flagStore = root.resolve(PATH_FLAG_STORE_END_ENTITY);

      Files.createDirectories(trusted);
      Files.createDirectories(trustAnchor);

      // --- a. empty directories
      {
        assertTrue(Files.isDirectory(trustAnchor));
        assertTrue(Files.isDirectory(trusted));
        assertFalse(Files.exists(flagStore));

        assertDoesNotThrow(() -> TrustCenter.initializeCache(root));

        assertFalse(TrustCenter.isFlagStoreEndEntity());
      } // end --- a.

      Files.write(flagStore, AfiUtils.EMPTY_OS);

      // --- b. just "flagStoreEndEntity" present
      {
        assertTrue(Files.isDirectory(trustAnchor));
        assertTrue(Files.isDirectory(trusted));
        assertTrue(Files.isRegularFile(flagStore));

        assertDoesNotThrow(() -> TrustCenter.initializeCache(root));

        assertTrue(TrustCenter.isFlagStoreEndEntity());
      } // end --- b.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link TrustCenter#initialize()}. */
  @Test
  void test_initialize() {
    // Assertions:
    // ... a. initializeCache(Path)-method works as expected

    // Test strategy:
    // --- a. backup existing file
    // --- b. ERROR: configuration file absent
    // --- c. ERROR: configuration file is not a properties-file
    // --- d. ERROR: configuration file does not contain property "pathTrustCenter"
    // --- e. ERROR: property "pathTrustCenter" does not point to an existing directory
    // --- f. valid property "pathTrustCenter" while path is unset
    // --- g. restore
    // --- h. valid property "pathTrustCenter" while path is set to another directory
    // --- i. valid property "pathTrustCenter" while path is set to same directory

    final var pathBackup = Paths.get(PATH_CONFIGURATION + ".bak");

    assertTrue(Files.exists(PATH_CONFIGURATION));
    assertFalse(Files.exists(pathBackup));

    try {
      // --- a. backup existing file
      {
        Files.move(PATH_CONFIGURATION, pathBackup);
      } // end --- a.

      // --- b. ERROR: configuration file absent
      {
        assertFalse(Files.exists(PATH_CONFIGURATION));

        assertThrows(NoSuchFileException.class, TrustCenter::initialize);
      } // end --- b.

      // --- c. ERROR: configuration file is not a properties-file
      {
        Files.write(PATH_CONFIGURATION, RNG.nextBytes(128));

        assertThrows(NullPointerException.class, TrustCenter::initialize);
      } // end --- c.

      // --- d. ERROR: configuration file does not contain property "pathTrustCenter"
      {
        Files.writeString(
            PATH_CONFIGURATION,
            """
            key = value
            """);

        assertThrows(NullPointerException.class, TrustCenter::initialize);
      } // end --- d.

      // --- e. ERROR: property "pathTrustCenter" does not point to an existing directory
      {
        Files.writeString(
            PATH_CONFIGURATION,
            """
                pathTrustCenter = foo.bar
                """);

        assertThrows(IllegalArgumentException.class, TrustCenter::initialize);
      } // end --- e.

      // --- f. valid property "pathTrustCenter" while path is unset
      {
        final var root = insTempDirectory.resolve("test_initialize");
        final var trustAnchor = root.resolve(PATH_TRUST_ANCHOR);
        final var trusted = root.resolve(PATH_TRUSTED);
        Files.createDirectories(trustAnchor);
        Files.createDirectories(trusted);
        final var rcaChrString =
            Hex.toHexDigits("DEXXX".getBytes(UTF_8))
                + "8" // ServiceIndicator
                + "0" // discretionary data, level 0
                + "02" // algorithmReference of an elliptic curve
                + "25"; // year
        final var rcaPuk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP512r1).getPublicKey();
        Files.write(
            trustAnchor.resolve(rcaChrString + SUFFIX_PUK_DER),
            rcaPuk.getEncoded(EafiElcPukFormat.ISOIEC7816).getEncoded());
        Files.writeString(
            PATH_CONFIGURATION,
            String.format(
                "pathTrustCenter = %s%n",
                root.toAbsolutePath().normalize().toString().replace('\\', '/')));
        assertDoesNotThrow(TrustCenter::clearCache);
        assertThrows(IllegalStateException.class, TrustCenter::getPath);
        final var expected = Map.ofEntries(Map.entry(rcaChrString, rcaPuk));

        TrustCenter.initialize();
        assertEquals(expected, TrustCenter.CACHE_PUK.insCache);
      } // end --- f.

      // --- g. restore
      {
        Files.deleteIfExists(PATH_CONFIGURATION);
        assertFalse(Files.exists(PATH_CONFIGURATION));
        assertTrue(Files.exists(pathBackup));
        Files.move(pathBackup, PATH_CONFIGURATION);
        assertTrue(Files.isRegularFile(PATH_CONFIGURATION));
      } // end --- g.

      // --- h. valid property "pathTrustCenter" while path is set to another directory
      {
        TrustCenter.initialize();
        assertFalse(TrustCenter.CACHE_PUK.insCache.isEmpty());
      } // end --- h.

      // --- i. valid property "pathTrustCenter" while path is set to same directory
      {
        final var expected = TrustCenter.getPath();

        assertDoesNotThrow(TrustCenter::initialize);

        assertEquals(expected, TrustCenter.getPath());
      } // end --- i.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** Test method for {@link TrustCenter#initializeCache(Path)}. */
  @SuppressWarnings({"PMD.NcssCount"})
  @Test
  void test_initializeCache__Path() {
    // Assertions:
    // ... a. add(Cvc)-method works as expected
    // ... b. exportCvc(Path, Cvc)-method works as expected

    // Test strategy:
    // --- a. ERROR: directory root absent
    // --- b. ERROR: directory trustAnchor absent
    // --- c. ERROR: directory trusted absent
    // --- d. empty directories
    // --- e. just "flagStoreEndEntity" present
    // --- f. one chain present
    // --- g. one untrusted CVC present

    try {
      final var root = insTempDirectory.resolve("test_initializeCache__Path");
      // --- a. ERROR: directory root absent
      {
        assertFalse(Files.exists(root));

        assertThrows(IllegalArgumentException.class, () -> TrustCenter.initializeCache(root));
      } // end --- a.

      final var trustAnchor = root.resolve(PATH_TRUST_ANCHOR);
      final var trusted = root.resolve(PATH_TRUSTED);
      Files.createDirectories(trustAnchor.getParent());
      Files.createDirectories(trusted);

      // --- b. ERROR: directory trustAnchor absent
      {
        assertTrue(Files.isDirectory(trusted));
        assertFalse(Files.exists(trustAnchor));

        assertThrows(IllegalArgumentException.class, () -> TrustCenter.initializeCache(root));
      } // end --- b.

      Files.createDirectories(trustAnchor);

      // --- c. ERROR: directory trusted absent
      {
        assertTrue(Files.deleteIfExists(trusted));
        assertTrue(Files.isDirectory(trustAnchor));
        assertFalse(Files.exists(trusted));

        assertThrows(IllegalArgumentException.class, () -> TrustCenter.initializeCache(root));
      } // end --- c.

      Files.createDirectories(trusted);
      final var flagStore = root.resolve(PATH_FLAG_STORE_END_ENTITY);

      // --- d. empty directories
      {
        assertTrue(Files.isDirectory(trustAnchor));
        assertTrue(Files.isDirectory(trusted));
        assertFalse(Files.exists(flagStore));

        assertDoesNotThrow(() -> TrustCenter.initializeCache(root));

        assertSame(root, TrustCenter.getPath());
        assertFalse(TrustCenter.isFlagStoreEndEntity());
        assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
        assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
      } // end --- d.

      Files.write(flagStore, AfiUtils.EMPTY_OS);

      // --- e. just "flagStoreEndEntity" present
      {
        assertTrue(Files.isDirectory(trustAnchor));
        assertTrue(Files.isDirectory(trusted));
        assertTrue(Files.isRegularFile(flagStore));

        assertDoesNotThrow(() -> TrustCenter.initializeCache(root));

        assertSame(root, TrustCenter.getPath());
        assertTrue(TrustCenter.isFlagStoreEndEntity());
        assertTrue(TrustCenter.CACHE_PUK.insCache.isEmpty());
        assertTrue(TrustCenter.CACHE_CVC.getCvc().isEmpty());
      } // end --- e.

      final var pathInput = trustAnchor.getParent();

      // --- f. one chain present
      // --- create CVC-Root-CA
      final var rcaChrString =
          Hex.toHexDigits("DEXXX".getBytes(UTF_8))
              + "8" // ServiceIndicator
              + "0" // discretionary data, level 0
              + "02" // algorithmReference of an elliptic curve
              + "24"; // year
      final var rcaCar = new CertificationAuthorityReference(rcaChrString);
      final var rcaChr = new CardHolderReference(rcaChrString);
      final var rcaOidPukUsage = AfiOid.ECDSA_with_SHA512;
      final var rcaPrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP512r1);
      final var rcaOidFlagList = AfiOid.CVC_FlagList_TI;
      final var rcaFlagList = Hex.toByteArray("c0 0000 0000 ffff");
      final var rcaCed = new CertificateDate(true, LocalDate.parse("2024-04-29").minusDays(2));
      final var rcaCxd = new CertificateDate(false, rcaCed.getDate().plusYears(10));
      final var rcaCvc =
          new Cvc(
              new CertificateProfileIndicator(),
              rcaCar,
              rcaOidPukUsage,
              rcaPrk.getPublicKey(),
              rcaChr,
              rcaOidFlagList,
              rcaFlagList,
              rcaCed,
              rcaCxd,
              rcaPrk);
      Files.write(pathInput.resolve(rcaChrString + SUFFIX_CVC_DER), rcaCvc.getCvc().getEncoded());
      Files.write(
          trustAnchor.resolve(rcaChrString + SUFFIX_PUK_DER),
          rcaPrk.getPublicKey().getEncoded(EafiElcPukFormat.ISOIEC7816).getEncoded());

      // --- create CVC-Sub-CA
      final var subChrString =
          Hex.toHexDigits("DEXXX".getBytes(UTF_8))
              + "8" // ServiceIndicator
              + "1" // discretionary data, level 1
              + "02" // algorithmReference of an elliptic curve
              + "24"; // year
      final var subChr = new CardHolderReference(subChrString);
      final var subOidPukUsage = AfiOid.ECDSA_with_SHA384;
      final var subPrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP384r1);
      final var subOidFlagList = AfiOid.CVC_FlagList_TI;
      final var subFlagList = Hex.toByteArray("80 0000 0000 ffff");
      final var subCed = new CertificateDate(true, rcaCed.getDate().plusDays(1));
      final var subCxd = new CertificateDate(false, subCed.getDate().plusYears(8));
      final var subCvc =
          new Cvc(
              new CertificateProfileIndicator(),
              rcaCar,
              subOidPukUsage,
              subPrk.getPublicKey(),
              subChr,
              subOidFlagList,
              subFlagList,
              subCed,
              subCxd,
              rcaPrk);
      Files.write(pathInput.resolve(subChrString + SUFFIX_CVC_DER), subCvc.getCvc().getEncoded());

      // create CVC-End-Entity
      final var eeChrString =
          "abcd" // discretionary data
              + "80" // Major Industrie Identifier
              + "276" // country code
              + "12345" // Issuer Identifier
              + "1122334455"; // Serial Number
      final var eeChr = new CardHolderReference(eeChrString);
      final var eeOidPukUsage = AfiOid.authS_gemSpec_COS_G2_ecc_with_sha256;
      final var eePrk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1);
      final var eeOidFlagList = AfiOid.CVC_FlagList_TI;
      final var eeFlagList = Hex.toByteArray("00 0000 0000 8421");
      final var eeCed = new CertificateDate(true, subCed.getDate().plusDays(1));
      final var eeCxd = new CertificateDate(false, subCed.getDate().plusYears(5));
      final var eeCvc =
          new Cvc(
              new CertificateProfileIndicator(),
              new CertificationAuthorityReference(subChrString),
              eeOidPukUsage,
              eePrk.getPublicKey(),
              eeChr,
              eeOidFlagList,
              eeFlagList,
              eeCed,
              eeCxd,
              subPrk);
      Files.write(pathInput.resolve(eeChrString + SUFFIX_CVC_DER), eeCvc.getCvc().getEncoded());

      // --- define expected values
      final var expPublicKeys =
          Map.ofEntries(
              Map.entry(rcaChrString, rcaPrk.getPublicKey()),
              Map.entry(subChrString, subPrk.getPublicKey()),
              Map.entry(eeChrString, eePrk.getPublicKey()));
      final var expCvc = Set.of(rcaCvc, subCvc, eeCvc);

      // --- run method-under-test
      assertDoesNotThrow(() -> TrustCenter.initializeCache(root));

      // --- checks
      // instance attributes
      assertEquals(expPublicKeys, TrustCenter.CACHE_PUK.insCache);
      assertEquals(expCvc, TrustCenter.CACHE_CVC.getCvc());

      Path tmp;

      // check one file from CVC-Root-CA
      assertTrue(
          Files.isRegularFile(
              tmp =
                  trusted
                      .resolve(rcaChr.getHumanReadable())
                      .resolve(rcaChr.getHumanReadable() + SUFFIX_CVC_DER_ASCII)));
      assertEquals(rcaCvc.getCvc().toString(" ", "   "), Files.readString(tmp, UTF_8));

      // check one file from CVC-Sub-CA
      assertTrue(
          Files.isRegularFile(
              tmp =
                  trusted
                      .resolve(rcaChr.getHumanReadable())
                      .resolve(subChr.getHumanReadable())
                      .resolve(subChr.getHumanReadable() + SUFFIX_CVC_DER)));
      assertEquals(
          Hex.toHexDigits(subCvc.getCvc().getEncoded()), Hex.toHexDigits(Files.readAllBytes(tmp)));

      // check one file from CVC-End-Entity
      assertTrue(
          Files.isRegularFile(
              tmp =
                  trusted
                      .resolve(rcaChr.getHumanReadable())
                      .resolve(subChr.getHumanReadable())
                      .resolve(eeChr.getHumanReadable())
                      .resolve(eeChr.getHumanReadable() + SUFFIX_PUK_ASCII)));
      assertEquals(eePrk.getPublicKey().toString(), Files.readString(tmp, UTF_8));
      // end --- f.

      // --- g. one untrusted CVC present
      // --- create another CVC-Root-CA
      final var rca2ChrString =
          Hex.toHexDigits("DEXXX".getBytes(UTF_8))
              + "8" // ServiceIndicator
              + "0" // discretionary data, level 0
              + "02" // algorithmReference of an elliptic curve
              + "25"; // year
      final var rca2Car = new CertificationAuthorityReference(rcaChrString);
      final var rca2Chr = new CardHolderReference(rcaChrString);
      final var rca2OidPukUsage = AfiOid.ECDSA_with_SHA256;
      final var rca2Prk = new EcPrivateKeyImpl(AfiElcParameterSpec.brainpoolP256r1);
      final var rca2OidFlagList = AfiOid.CVC_FlagList_TI;
      final var rca2FlagList = Hex.toByteArray("ff ffff ffff ffff");
      final var rca2Ced = new CertificateDate(true, LocalDate.parse("2024-04-30").minusDays(2));
      final var rca2Cxd = new CertificateDate(false, rcaCed.getDate().plusYears(10));
      final var rca2Cvc =
          new Cvc(
              new CertificateProfileIndicator(),
              rca2Car,
              rca2OidPukUsage,
              rca2Prk.getPublicKey(),
              rca2Chr,
              rca2OidFlagList,
              rca2FlagList,
              rca2Ced,
              rca2Cxd,
              rca2Prk);
      Files.write(pathInput.resolve(rca2ChrString + SUFFIX_CVC_DER), rca2Cvc.getCvc().getEncoded());

      // --- run method-under-test
      assertDoesNotThrow(() -> TrustCenter.initializeCache(root));

      // --- checks
      // instance attributes
      assertEquals(expPublicKeys, TrustCenter.CACHE_PUK.insCache);
      assertEquals(expCvc, TrustCenter.CACHE_CVC.getCvc());

      // --- check one file from second CVC-Root-CA
      assertTrue(
          Files.isRegularFile(
              tmp =
                  root.resolve("untrusted")
                      .resolve(rca2Chr.getHumanReadable())
                      .resolve(rca2Chr.getHumanReadable() + SUFFIX_CVC_DER_ASCII)));
      assertEquals(rca2Cvc.getCvc().toString(" ", "   "), Files.readString(tmp, UTF_8));
      // end --- g.
    } catch (IOException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /** This test method is used for experiments. */
  @Test
  void test_zz_misc() {
    // --- set path of trust-center to something useful
    assertDoesNotThrow(TrustCenter::initialize);

    /*/ --- create End-Entity-CVC
    {
      final String raw = "a94093f56fd172f81ad54d0551c7a870e07caa64a441e275230f3ebfa5779376";
      final BigInteger d = new BigInteger(raw, 16);
      final EcPrivateKeyImpl prk = new EcPrivateKeyImpl(d, AfiElcParameterSpec.brainpoolP256r1);
      final Cvc cvc = new Cvc((ConstructedBerTlv) BerTlv.getInstance(
          """
              7f21 81da
              |  7f4e 8193
              |  |  5f29 01 70
              |  |  42 08 4445475858110218
              |  |  7f49 4b
              |  |  |  06 06 2b2403050301
              |  |  |  86 41 0403f54df417a6fd3e504cc7291d3a90f12087b50e85987070151
                             2ca2f2269cfeea3c635950ea0e98048b7641a85d651de942620df
                             3072173ac563e34ae8d52aa7
              |  |  5f20 0c 000980276883110000107637
              |  |  7f4c 13
              |  |  |  06 08 2a8214004c048118
              |  |  |  53 07 00000000000000
              |  |  5f25 06 010900080005
              |  |  5f24 06 020400080005
              |  5f37 40 163d3e96bac224a595f073342e4170980c3545cdd065a2a90487525b7
                         cdfac482c3eb1b0666acb17eb19fd61fea59552af47ba09f26dcfd910
                         6c43962cec8871
              """
      ));
      assertEquals(
          cvc.getPublicKey(),
          prk.getPublicKey()
      );

      assertTrue(TrustCenter.add(cvc));
    } // end "create End-Entity-CVC */

    /*/ --- import CV-certificates from arbitrary file
    try {
      LOGGER.atInfo().log("import CV-certificates from arbitrary file");

      // define path and read CVC
      final var inFolder = Paths.get("/home/dev/tmp/80276001011699902101-cvc-flag0");
      assertTrue(Files.isDirectory(inFolder));
      final var inPath = inFolder.resolve("80276001011699902101-cvc-flag0.crt");
      assertTrue(Files.isRegularFile(inPath));
      final var inCvc = Files.readAllBytes(inPath);
      final var cvc = new Cvc(inCvc);
      LOGGER.atInfo().log("CVC: {}", String.format("%n%s", cvc));

      // path to private key
      final var inPrk = inFolder.resolve("80276001011699902101-cvc-flag0.prv");
      final var prkOctet = Files.readAllBytes(inPrk);
      final var prkTlv = BerTlv.getInstance(prkOctet);
      LOGGER.atTrace().log(
          "content of 80276001011699902101-cvc-flag0.prv: {}",
          String.format("%n%s", prkTlv.toStringTree())
      );
      assertEquals(DerSequence.class, prkTlv.getClass());
      final var os1 = ((DerSequence) prkTlv).getPrimitive(DerOctetString.TAG).orElseThrow();
      final var seq = (DerSequence) BerTlv.getInstance(os1.getValueField());
      final var os2 = (DerOctetString) seq.getPrimitive(DerOctetString.TAG).orElseThrow();
      final var doOid = seq.getConstructed(0xa0).orElseThrow()
          .getPrimitive(DerOid.TAG).orElseThrow();
      final var oid = ((DerOid) doOid).getDecoded();
      final var prk = new EcPrivateKeyImpl(
          new BigInteger(1, os2.getDecoded()),
          AfiElcParameterSpec.getInstance(oid)
      );
      assertEquals(cvc.getPublicKey(), prk.getPublicKey());
      LOGGER.atInfo().log("PrK: {}", prk.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    } // end Catch (...)
    // end "import CV-certificates from arbitrary file" */
  } // end method */
} // end class
