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
 * Module information.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
module de.gematik.smartcards.pcsc {
  // provides java.security.Provider with de.gematik.smartcards.pcsc.AfiPcsc;

  // --- exports of this module
  // Note: As stated in the Oracle documentation, it is strongly recommended to
  //       NOT export the package containing the service provider.
  //       Thus, nothing is exported here, see JavaDoc for class
  //       "java.util.ServiceLoader<S>" section "Deploying service providers as modules"
  exports de.gematik.smartcards.pcsc; // FIXME check how this could be commented out
  exports de.gematik.smartcards.pcsc.lib to
      com.sun.jna;

  // --- requirements of this module
  requires com.github.spotbugs.annotations; // null-annotations, e.g. package-info.java
  requires com.sun.jna;
  requires transitive de.gematik.smartcards.sdcom;
  requires transitive java.smartcardio; // communication with smart-cards
  requires transitive org.slf4j; // used for logging
} // end module
