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

package org.eclipse.apoapsis.ortserver.credentialhelper.common

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

import okio.Path
import okio.Path.Companion.toPath

import platform.posix.getenv

actual fun getTmpDir(): Path = requireNotNull(
    getEnv("LOCALAPPDATA")?.toPath()?.resolve("Temp")
)

actual fun getHomeDirectory(): Path = requireNotNull(
    getEnv("XDG_CONFIG_HOME")?.toPath()
)

@OptIn(ExperimentalForeignApi::class)
private fun getEnv(name: String) = getenv(name)?.toKString()
