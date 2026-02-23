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

import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlinMultiplatform)
}

group = "org.eclipse.apoapsis.ortserver.credentialhelper"

repositories {
    mavenCentral()
}

kotlin {
    // Minimum number of platforms. To be extended in the future if needed.
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        binaries {
            executable {
                mainClass = "org.eclipse.apoapsis.ortserver.credentialhelper.CredentialHelper"
            }
        }
    }

    targets.withType<KotlinNativeTarget> {
        binaries {
            executable(setOf(NativeBuildType.RELEASE)) {
                entryPoint = "org.eclipse.apoapsis.ortserver.credentialhelper.main"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.clikt)
        }
    }
}

apply(plugin = "dev.detekt")

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
