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

import okio.BufferedSource
import okio.Path

/**
 * Return the expected path to the temporary directory based on the operating system.
 *
 * @return The expected [Path] to the temporary directory, which is platform-specific.
 */
expect fun getTmpDir(): Path

/**
 * Return the home directory of the current user based on the operating system.
 *
 * @return The [Path] to the home directory of the current user, which is platform-specific.
 */
expect fun getHomeDirectory(): Path

/**
 * Return a [BufferedSource] that reads from standard input (stdin).
 *
 * The returned source can be used for line-by-line reading as well as for structured parsing
 * (e.g. JSON via kotlinx.serialization with an okio-backed decoder), and is injectable in
 * tests by passing a pre-filled [okio.Buffer] instead of this default.
 */
expect fun stdinSource(): BufferedSource

/**
 * Terminate the current process with the given [exitCode].
 */
expect fun exitProcess(exitCode: Int): Nothing
