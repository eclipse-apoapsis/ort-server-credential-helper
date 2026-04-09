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

package org.eclipse.apoapsis.ortserver.credentialhelper.git

import okio.Path

import org.eclipse.apoapsis.ortserver.credentialhelper.common.getHomeDirectory

/**
 * Return the expected, platform-specific [Path] to the Git credentials file.
 */
internal fun getExpectedGitCredentialsFilePath(): Path =
    getHomeDirectory().resolve(".git-credentials")

/**
 * Parse the input parameter lines [stdinLines] received from stdin and return a [CredentialRequest] object.
 *
 * The input parameter lines are expected to be in the format:
 * protocol=https
 * host=github.com
 * path=oss-review-toolkit/ort.git
 */
internal fun parseRequestFromStdinLines(stdinLines: List<String>): CredentialRequest {
    val requestParams = stdinLines
        .filter { it.contains("=") }
        .associate { it.substringBefore("=").trim() to it.substringAfter("=").trim() }

    return CredentialRequest(
        protocol = requestParams["protocol"].orEmpty(),
        host = requestParams["host"].orEmpty(),
        path = requestParams["path"].orEmpty()
    )
}
