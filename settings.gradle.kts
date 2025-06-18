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

pluginManagement {
  // Note: This section is necessary for finding "de.web.b8pacj.jacomon".

  repositories {
    gradlePluginPortal() // repository necessary to find plugin "de.fayard.refreshVersions".

    maven { // repository necessary to find plugin "de.web.b8pacj.jacomon".
      url = uri("https://v2202005121345117714.megasrv.de/maven.repository")
    }
  } // end repositories
} // end pluginManagement section ______________________________________________

plugins {
  // For setting up refreshVersions plugin see
  // https://jmfayard.github.io/refreshVersions/
  id("de.fayard.refreshVersions") version "0.60.5"

  // Apply the foojay-resolver plugin to allow automatic download of JDKs
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
} // ___________________________________________________________________________

// begin dependencyResolutionManagement  . . . . . . . . . . . . . . . . . . . .
dependencyResolutionManagement {
  repositories {
    mavenCentral() // for all the public libraries

    maven { url = uri("https://v2202005121345117714.megasrv.de/maven.repository") }

    /* Note: For proper dependency resolution, local repositories are not
    //       appropriate. A local repository is useful only during the development
    //       phase.
    mavenLocal() // */
  }
} // end dependencyResolutionManagement ________________________________________

rootProject.name = "lib-smartcards"

include(
  // "AppServer", // application with a server used for testing module "net"
  "de.gematik.smartcards.crypto",
  "de.gematik.smartcards.g2icc",
  // "de.gematik.smartcards.net",
  "de.gematik.smartcards.pcsc",
  "de.gematik.smartcards.sdcom",
  // "de.gematik.smartcards.sicct",
  "de.gematik.smartcards.tlv",
  "de.gematik.smartcards.utils",
)
