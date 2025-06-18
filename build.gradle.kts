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

plugins { // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  id("com.diffplug.spotless")
  id("de.web.b8pacj.jacomon")
  id("de.web.b8pacj.publish") apply false
  id("eclipse")
  id("idea")
  `java-library`

  // Note 1: For more information on this plugin for dependency analysis, see
  //         https://github.com/autonomousapps/dependency-analysis-gradle-plugin
  // Note 2: Build-health can be investigated by:
  //         ./gradlew buildHealth
  id("com.autonomousapps.dependency-analysis")
} // end plugins _______________________________________________________________

subprojects { // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  apply(plugin = "com.autonomousapps.dependency-analysis")
  apply(plugin = "de.web.b8pacj.jacomon")
  apply(plugin = "de.web.b8pacj.publish")
  apply(plugin = "eclipse")
  apply(plugin = "idea")
  apply(plugin = "java-library")

  // set idea specific things, see
  // https://docs.gradle.org/current/dsl/org.gradle.plugins.ide.idea.model.IdeaModule.html
  idea { module { isDownloadJavadoc = true } } // end idea

  // set JavaVersion
  java { // here currently the latest LTS version
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
  } // end java

  tasks.register("afiCheck") {
    group = "other"
    description = "perform code quality checks"

    dependsOn(
      tasks.checkstyleMain,
      tasks.checkstyleTest,
      tasks.pmdMain,
      tasks.pmdTest,
      tasks.spotbugsMain,
      tasks.spotbugsTest,
    )
  } // */

  testing {
    suites {
      // Configure the built-in test suite
      val test by
        getting(JvmTestSuite::class) {
          // Use JUnit Jupiter test framework
          useJUnitJupiter("5.13.1")
        }
    }
  }
} // end subprojects ___________________________________________________________

spotless { //  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  // ratchetFrom("origin/main")

  format(
    "pacmod",
    {
      target("**/package-info.java", "**/module-info.java")
      trimTrailingWhitespace()

      // "delimiter" is the first java-doc-comment
      licenseHeaderFile(rootProject.file("COPYRIGHT"), "/\\*\\*\$")
    },
  )

  java {
    target("**/*.java")
    toggleOffOn()
    googleJavaFormat()
    importOrder()

    // Note: The following line improves java-source-code, but causes errors
    //       when Gradle's configuration-cache is enabled.
    //       Thus, the line is commented out and pmd will report unused imports, see
    //       https://github.com/diffplug/spotless/issues/1274#issuecomment-2149173245
    removeUnusedImports("cleanthat-javaparser-unnecessaryimport")
    licenseHeaderFile(rootProject.file("COPYRIGHT"))
  }

  kotlinGradle {
    target("**/*.gradle.kts")
    ktfmt().googleStyle().configure { it.setBlockIndent(2) }
    trimTrailingWhitespace()

    // "delimiter" is either
    // 1. [p]luginManagement in "settings.gradle.kts",
    // 2. [p]lugins          in "build.gradle.kts", or
    // 3. [i]mport           in "build.gradle.kts.
    licenseHeaderFile(rootProject.file("COPYRIGHT"), "[ip].+?\$")
  }

  pom {
    target("**/pom.xml")

    sortPom().expandEmptyElements(false)
  }
} // end spotless ______________________________________________________________
