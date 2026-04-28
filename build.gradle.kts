/*
 * Copyright (C) 2026 The ORT Server Authors (See <https://github.com/eclipse-apoapsis/ort-server/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

import dev.detekt.gradle.Detekt

import git.semver.plugin.gradle.PrintTask

import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.gitSemver)
    alias(libs.plugins.kotlinMultiplatform) apply false
}

semver {
    // Do not create an empty release commit when running the "releaseVersion" task.
    createReleaseCommit = false

    // Use "RC" instead of "SNAPSHOT" as the default pre-release identifier.
    defaultPreRelease = "RC"

    // Do not let untracked files bump the version or add a "-SNAPSHOT" suffix.
    noDirtyCheck = true
}

// Only override a default version (which usually is "unspecified"), but not a custom version.
if (version == Project.DEFAULT_VERSION) {
    val semVersion = semver.semVersion

    // Set the version based on the following rules:
    // - If the current commit is tagged as a release (e.g., 1.2.3) or pre-release (e.g., 1.2.3-RC1), the version
    //   equals the tag.
    // - If the current commit is ahead of the last release or pre-release tag, the version is the tag plus the
    //   pre-release identifier, the commit count, and the SHA. For example, "1.2.3-RC1.001.sha.0123456".
    val shaLength = if (semVersion.commitCount > 0) 7 else 0
    version = semVersion.toInfoVersionString(shaLength = shaLength).replace('+', '.')
}

logger.lifecycle("Building ORT Server Credential Helper version $version.")

open class CredentialHelperPrintTask : PrintTask({ "" }, "Prints the current project version", "") {
    private val projectVersion = project.version.toString()

    @TaskAction
    fun printVersion() = println(projectVersion)
}

tasks.replace("printVersion", CredentialHelperPrintTask::class.java)

apply(plugin = "dev.detekt")

subprojects {
    apply(plugin = "dev.detekt")

    dependencies {
        "detektPlugins"("dev.detekt:detekt-rules-ktlint-wrapper:${rootProject.libs.versions.detektPlugin.get()}")
        "detektPlugins"("org.ossreviewtoolkit:detekt-rules:${rootProject.libs.versions.ort.get()}")
    }

    pluginManager.withPlugin("dev.detekt") {
        (extensions.getByName("detekt") as dev.detekt.gradle.extensions.DetektExtension).apply {
            buildUponDefaultConfig = true
            config.from(rootProject.files(".detekt.yml"))
            basePath = rootDir

            source.from(
                fileTree(".") { include("*.gradle.kts") },
                "src/commonMain/kotlin",
                "src/commonTest/kotlin",
                "src/jvmMain/kotlin",
                "src/jvmTest/kotlin",
                "src/linuxMain/kotlin",
                "src/macosMain/kotlin",
                "src/mingwMain/kotlin"
            )
        }
    }

    tasks.withType<Detekt>().configureEach {
        exclude {
            "/build/generated/" in it.file.absoluteFile.invariantSeparatorsPath
        }
    }

    afterEvaluate {
        tasks.register("detektAll") {
            group = "Verification"
            description = "Run all detekt tasks."

            dependsOn(tasks.withType<Detekt>().filterNot { it.name == "detekt" })
        }
    }

    tasks.whenTaskAdded {
        if (name == "allTests") {
            dependsOn(tasks.named("jvmTest"))
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        testLogging {
            events = setOf(
                org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
            )
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showExceptions = true
            showStandardStreams = true
        }
    }
}

dependencies {
    detektPlugins("dev.detekt:detekt-rules-ktlint-wrapper:${rootProject.libs.versions.detektPlugin.get()}")
    detektPlugins("org.ossreviewtoolkit:detekt-rules:${rootProject.libs.versions.ort.get()}")
}

detekt {
    // Only configure differences to the default.
    buildUponDefaultConfig = true
    config.from(files("$rootDir/.detekt.yml"))
    basePath = rootDir
    source.from(fileTree(".") { include("*.gradle.kts") }, "src/testFixtures/kotlin")
}
