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
package de.gematik.smartcards.crypto;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class provides utilities around {@link X509Certificate}s.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class X509Utils {

  /** Factory for creating {@link X509Certificate}s. */
  private static final CertificateFactory CERTIFICATE_FACTORY = getFactory("X.509"); // */

  /** Default constructor. */
  private X509Utils() {
    // intentionally empty
  } // end constructor */

  /**
   * Returns appropriate certificate factory.
   *
   * @param type of certificates
   * @return an appropriate factory
   * @throws IllegalArgumentException if no appropriate factory is available
   */
  @VisibleForTesting // otherwise = private
  /* package */ static CertificateFactory getFactory(final String type) {
    try {
      return CertificateFactory.getInstance(type);
    } catch (CertificateException e) {
      throw new IllegalArgumentException(e);
    } // end Catch (...)
  } // end method */

  /**
   * Generates an {@link X509Certificate} from given octet-string.
   *
   * @param rawX509 octet string
   * @return corresponding {@link X509Certificate}
   * @throws IllegalArgumentException if generating an {@link X509Certificate} failed
   */
  public static X509Certificate generateCertificate(final byte[] rawX509) {
    try {
      return (X509Certificate)
          CERTIFICATE_FACTORY.generateCertificate(new ByteArrayInputStream(rawX509));
    } catch (CertificateException e) {
      throw new IllegalArgumentException(e);
    } // end Catch (...)
  } // end method */
} // end class
