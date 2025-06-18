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
project.version = "0.3.7" // for history of releases see CHANGELOG.md

group = "de.gematik.smartcards"

val libraryDescription = "Generation 2 functionality"
val developerName = "gematik"
val developerEmail = "software-development@gematik.de"

// begin Manifest  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
// Note 1: The plugin for publication will add/replace the following
//         mappings as shown: will add/replace the following mappings as shown:
//         a. "Build-version" to project.getVersion().toString()
//         b. "Build-system"  to "Gradle " + project.getGradle().getGradleVersion()
//         c. "Build-date"    to "reproducible-build"
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
    pom.name.set("Generation 2 functionality")

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
            maximum = (if (flagRelease) "1" else "1").toBigDecimal()
          }

          // method missed count
          limit {
            counter = "METHOD"
            value = "MISSEDCOUNT"
            maximum = (if (flagRelease) "30" else "30").toBigDecimal()
          }

          // branch covered ratio
          limit {
            counter = "BRANCH"
            value = "COVEREDRATIO"
            minimum = (if (flagRelease) "0.81" else "0.81").toBigDecimal()
          }

          // instruction covered ratio
          limit {
            counter = "INSTRUCTION"
            value = "COVEREDRATIO"
            minimum = (if (flagRelease) "0.92" else "0.92").toBigDecimal()
          }
        }
      } // end violationRules
    } // end jacocoTestCoverageVerification
  } // end tasks
  // end   jacoco configuration
} // end gradle.taskGraph.whenReady

// end plugin configuration ____________________________________________________

// sub-project specific dependencies . . . . . . . . . . . . . . . . . . . . . .
dependencies { // sub-project specific dependencies
  // Note: Project property "afiED" is described in file "gradle.properties".
  if ("true".equals(project.property("afiED"))) {
    api(libs.de.gematik.smartcards.crypto)
    api(libs.de.gematik.smartcards.sdcom)
    api(libs.de.gematik.smartcards.tlv)
    api(libs.de.gematik.smartcards.utils)

    testImplementation(libs.de.gematik.smartcards.pcsc)
  } else {
    api(project(":de.gematik.smartcards.crypto"))
    api(project(":de.gematik.smartcards.sdcom"))
    api(project(":de.gematik.smartcards.tlv"))
    api(project(":de.gematik.smartcards.utils"))

    testImplementation(project(":de.gematik.smartcards.pcsc"))
  } // end else

  api(libs.slf4j.api) // logging

  implementation(libs.spotbugs.annotations) // e.g. in package-info.java

  // Note 1: Plugin "dependency-analysis complains:
  //         Advice for :de.gematik.smartcards.utils
  //         Unused dependencies which should be removed:
  //           testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
  // Note 2: Intentionally, this advice is ignored, because the following
  //         dependency is necessary to run tests.
  testImplementation(Testing.junit.jupiter)

  testImplementation(libs.jna)
  testImplementation(libs.jupiter.api)
  testImplementation(libs.logback.classic)
  testImplementation(libs.logback.core)
} // end dependencies __________________________________________________________

// section configuring test tasks
tasks.withType(Test::class) {
  systemProperty(
    "junit.jupiter.testclass.order.default",
    "org.junit.jupiter.api.ClassOrderer\$ClassName",
  )

  testLogging { events("PASSED") }

  // set heap size for the test JVM(s)
  minHeapSize = "128m"
  maxHeapSize = "256m"

  // Note: Disable parallel test execution, because some tests connect to a
  //       real world IFD and that component does not permit more than one
  //       user (more than one test execution) at a time.
  maxParallelForks = 1
}
