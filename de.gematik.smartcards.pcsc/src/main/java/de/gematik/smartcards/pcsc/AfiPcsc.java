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
package de.gematik.smartcards.pcsc;

import java.io.Serial;
import java.security.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider object for PC/SC.
 *
 * <p><i><b>Note:</b> Implementation guidelines for a provider are given by <a
 * href="https://docs.oracle.com/pls/topic/lookup?ctx=javase17&id=security_guide_impl_provider">Oracle</a>.
 * </i>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.DataClass"})
public final class AfiPcsc extends Provider {

  /** Automatically generated constant. */
  @Serial private static final long serialVersionUID = -8143321426135869174L; // */

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(AfiPcsc.class); // */

  /** Information. */
  public static final String INFO = "PC/SC provider using JNA"; // */

  /** Name of provider. */
  public static final String PROVIDER_NAME = AfiPcsc.class.getName(); // */

  /** Type. */
  public static final String TYPE = "PC/SC"; // */

  /**
   * Version {@link String}.
   *
   * <p>The version string contains a version number optionally followed by other information
   * separated by one of the characters of '+', '-'.
   *
   * <p>The format for the version number is:
   *
   * <blockquote>
   *
   * <pre>
   *     ^[0-9]+(\.[0-9]+)*
   * </pre>
   *
   * </blockquote>
   *
   * <p><i><b>Note:</b> The intention is that this version number equals the version number of the
   * published library, see {@code "project.version"} in gradle's build-script.</i>
   */
  public static final String VERSION = "0.4.5"; // */

  /**
   * Default constructor.
   *
   * <p>This constructor calls {@code super(...)} with {@link #PROVIDER_NAME}, {@link #VERSION} and
   * {@link #INFO}.
   */
  public AfiPcsc() {
    super(PROVIDER_NAME, VERSION, INFO);
    LOGGER.atTrace().log("Provider constructed: {}", LOGGER.getName());
    putService(new AfiProviderService(this, "TerminalFactory", TYPE, IfdFactory.class.getName()));
  } // end constructor */

  /**
   * A provider service.
   *
   * @author <a href="mailto:software-development@gematik.de">gematik</a>
   */
  private static final class AfiProviderService extends Provider.Service {

    /**
     * Construct a new service.
     *
     * @param provider the provider that offers this service
     * @param type the type of this service
     * @param algorithm the algorithm name
     * @param className the name of the class implementing this service
     * @throws NullPointerException if provider, type, algorithm, or className is null
     */
    public AfiProviderService(
        final Provider provider,
        final String type,
        final String algorithm,
        final String className) {
      super(provider, type, algorithm, className, /* aliases */ null, /* attributes */ null);
    } // end constructor */

    /**
     * Return a new instance of the implementation described by this service.
     *
     * <p>The security provider framework uses this method to construct implementations.
     * Applications will typically not need to call it. The default implementation uses reflection
     * to invoke the standard constructor for this type of service. Security providers can override
     * this method to implement instantiation in a different way. For details and the values of
     * constructorParameter that are valid for the various types of services see the @extLink
     * security_guide_jca Java Cryptography Architecture (JCA) Reference Guide.
     *
     * @param constructorParameter the value to pass to the constructor, or {@code null} if this
     *     type of service does not use a {@code constructorParameter}
     * @return a new implementation of this service
     * @see Provider.Service#newInstance(Object)
     */
    @Override
    public Object newInstance(final Object constructorParameter) {
      return new IfdFactory(null);
    } // end method */
  } // end inner class
} // end class
