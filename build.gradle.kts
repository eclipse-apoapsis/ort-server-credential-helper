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

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinMultiplatform) apply false
}

apply(plugin = "dev.detekt")

subprojects {
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
