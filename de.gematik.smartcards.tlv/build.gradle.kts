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

import de.web.b8pacj.plugin.publish.PublishMavenLocal

// set information used for publication
project.version = "0.7.6" // for history of releases see CHANGELOG.md

group = "de.gematik.smartcards"

val libraryDescription = "BER-TLV object"
val developerName = "gematik"
val developerEmail = "software-development@gematik.de"

// begin Manifest  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
// Note 1: The plugin for publication will add/replace the following
//         mappings as shown: will add/replace the following mappings as shown:
//         1. "Build-version" to project.getVersion().toString()
//         2. "Build-system"  to "Gradle " + project.getGradle().getGradleVersion()
//         3. "Build-date"    to "reproducible-build"
// Note 2: No information depending on time or environment is used, because
//         builds should be reproducible, see:
//         https://en.wikipedia.org/wiki/Reproducible_builds
// Note 3: The following attributes will be added to the manifest-files used in
//         binaries, Java-Doc and Java-Sources.
tasks.withType<Jar>().configureEach {
  manifest {
    attributes(
      "Implementation-Title" to libraryDescription,
      "Author-Name" to developerName,
      "Author-Email" to developerEmail,
    )
  }
} // end Manifest ______________________________________________________________

// begin POM configuration . . . . . . . . . . . . . . . . . . . . . . . . . . .
tasks.withType<GenerateMavenPom>().configureEach {
  doFirst {
    // For a complete list of properties for MavenPom see
    // https://docs.gradle.org/current/javadoc/org/gradle/api/publish/maven/MavenPom.html
    pom.name.set("ISO/IEC 8825 BER-TLV objects")

    val repoPrefix = "gematik/lib-smartcards"
    val gitPath = "$repoPrefix.git"
    val scmConnection = "scm:git:git://github.com/$gitPath"
    val scmDeveloper = "scm:git:ssh://git@github.com:$gitPath"
    val scmUrl = "https://github.com/$repoPrefix"
    val urlCompany = "https://www.gematik.de"

    pom.description.set(libraryDescription)
    pom.url.set(scmUrl)
    pom.licenses {
      license {
        name.set("The Apache License, Version 2.0")
        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
      }
    }
    pom.developers {
      developer {
        id.set("afi")
        name.set(developerName)
        email.set(developerEmail)
        url.set(urlCompany)
      }
    }
    pom.scm { // refer to http://maven.apache.org/pom.html#SCM
      connection.set(scmConnection) // connection with (at least) read access
      developerConnection.set(scmDeveloper) // connection with write access
      url.set(scmUrl) // publicly browsable repository
    }
  } // end doFirst
} // end POM configuration _____________________________________________________

// begin plugin configuration  . . . . . . . . . . . . . . . . . . . . . . . . .
gradle.taskGraph.whenReady {
  // set flag indicating whether a release is build
  val flagRelease = project.plugins.getPlugin(PublishMavenLocal::class).isRelease
  logger.debug("release?    = $flagRelease")

  // begin jacoco configuration
  tasks {
    jacocoTestCoverageVerification {
      // For defining rules see e.g.: https://reflectoring.io/jacoco/
      violationRules {
        rule {
          // class missed count
          limit {
            counter = "CLASS"
            value = "MISSEDCOUNT"
            maximum = (if (flagRelease) "0" else "0").toBigDecimal()
          }

          // method missed count
          limit {
            counter = "METHOD"
            value = "MISSEDCOUNT"
            maximum = (if (flagRelease) "0" else "0").toBigDecimal()
          }

          // instruction covered ratio
          limit {
            counter = "INSTRUCTION"
            value = "COVEREDRATIO"
            minimum = (if (flagRelease) "1.00" else "1.00").toBigDecimal()
          }

          // branch covered ratio
          limit {
            counter = "BRANCH"
            value = "COVEREDRATIO"
            minimum = (if (flagRelease) "1.00" else "1.00").toBigDecimal()
          }
        } // end rule */
      } // end violationRules
    } // end jacocoTestCoverageVerification
  } // end tasks
  // end   jacoco configuration
} // end gradle.taskGraph.whenReady

// end plugin configuration ____________________________________________________

// subproject specific dependencies  . . . . . . . . . . . . . . . . . . . . . .
dependencies {
  // Note: Project property "afiED" is described in file "gradle.properties".
  if ("true".equals(project.property("afiED"))) {
    api(libs.de.gematik.smartcards.utils)
  } else {
    api(project(":de.gematik.smartcards.utils"))
  } // end else

  implementation(libs.spotbugs.annotations) // e.g. in package-info.java

  // Note 1: Plugin "dependency-analysis complains:
  //         Advice for :de.gematik.smartcards.utils
  //         Unused dependencies which should be removed:
  //           testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  // Note 2: Intentionally, this advice is ignored, because the following
  //         dependency is necessary to run tests.
  testImplementation(Testing.junit.jupiter)

  testImplementation(libs.jupiter.api)
  testImplementation(libs.slf4j.api) // logging in test-classes

  testRuntimeOnly(libs.logback.classic) // logging in test-classes
} // end dependencies __________________________________________________________

// section configuring test tasks
tasks.withType(Test::class) {
  testLogging { events("PASSED") }

  // Note 1: Speed up tests by parallel execution, see
  //         https://docs.gradle.org/current/userguide/performance.html#execute_tests_in_parallel
  // Note 2: Project is build with command "./gradlew --rerun-tasks
  // de.gematik.smartcards.tlv:puToMaLo".

  // no statement about parallel test execution                      arthur => 16" .. 26"
  // maxParallelForks = 2                                         // arthur => 16" .. 17"
  val noProcessors = Runtime.getRuntime().availableProcessors()
  maxParallelForks = (noProcessors - 1).takeIf { it > 0 } ?: 1 // arthur => 23" .. 24"
} // end test task configuration _______________________________________________
