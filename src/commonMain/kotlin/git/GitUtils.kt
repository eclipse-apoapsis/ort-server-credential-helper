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

import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer

/**
 * Return the expected [Path] to the git credentials file, which is platform-specific.
 */
internal expect fun getExpectedGitCredentialsFilePath(): Path

/**
 * Parse the input parameter lines received from stdin and constructs a [CredentialRequest] object.
 *
 * The input parameter lines are expected to be in the format:
 *
 * protocol=https
 * host=github.com
 * path=oss-review-toolkit/ort.git
 *
 * @param[stdinLines] The list of input parameter lines to parse.
 *
 * @return [CredentialRequest] object containing the parsed information.
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

/**
 * Read the content of the git credentials file located at the expected path.
 *
 * @return The content of the git credentials file as a string.
 */
internal fun readGitCredentialFileContent(): String =
    FileSystem.SYSTEM.source(getExpectedGitCredentialsFilePath()).buffer()
        .readUtf8()

/**
 * Parse the content of a git credentials file and returns a list of [Credentials] objects.
 *
 * The git credentials file is expected to have lines in the format:
 * `protocol://username:password@host/path`
 *
 * @param[fileContent] The content of the git credentials file to parse.
 *
 * @return A list of [Credentials] objects parsed from the file content.
 */
internal fun parseGitCredentialsFileContent(fileContent: String): List<Credentials> =
    fileContent.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { it.contains("@") } // Indicates, that the line contains credentials in the expected format
        .map { it.toCredentials() }

/**
 * Convert a string in the format `protocol://username:password@host/path` to a [Credentials] object.
 *
 * @receiver The string to convert, expected to be in the format `protocol://username:password@host/path`.
 *
 * @return A [Credentials] object containing the parsed information.
 */
private fun String.toCredentials(): Credentials {
    // Find position of last '@' symbol to separate credentials from host and path, as '@' can be present
    // in password as well
    val credentialsSplitPoint = this.lastIndexOf("@")

    val credentialsPart = this.substring(0, credentialsSplitPoint).substringAfter("://")
    val hostAndPathPart = this.substring(credentialsSplitPoint + 1)

    return Credentials(
        protocol = this.substringBefore("://").trim(),
        host = hostAndPathPart.substringBefore("/").trim(),
        path = hostAndPathPart.contains("/").let { if (it) hostAndPathPart.substringAfter("/").trim() else "" },
        username = credentialsPart.substringBefore(":").trim(),
        password = credentialsPart.substringAfter(":").trim()
    )
}

/**
 * Find the closest matching credentials for a given URL from a list of available credentials.
 *
 * The matching is based on the host and path of the URL.
 *
 * @param[requestedCredentials] The [Credentials] object containing the host and path to match against.
 * @param[availableCredentials] A list of available credentials to match against.
 *
 * @return The closest matching [Credentials] object, or an empty [Credentials] object with the URL as host if no match is found.
 */
internal fun findClosestMatchingCredential(
    requestedCredentials: CredentialRequest,
    availableCredentials: List<Credentials>
): Credentials {
    val credentialsForHost = availableCredentials.filter { it.host == requestedCredentials.host }

    val credentialsWithHostAndExactPath = credentialsForHost.filter { it.path == requestedCredentials.path }
    val credentialsWithHostAndContainingPath =
        requestedCredentials.path.isNotEmpty().let {
            credentialsForHost.filter { it.path.isNotEmpty() && requestedCredentials.path.startsWith(it.path + "/") }
        }

    val credentialsWithHostOnly = credentialsForHost.filter { it.path.isEmpty() }

    return when {
        credentialsWithHostAndExactPath.isNotEmpty() -> credentialsWithHostAndExactPath.first()

        credentialsWithHostAndContainingPath.isNotEmpty() -> credentialsForHost
            .associateWith { requestedCredentials.path.replace(it.path + "/", "").length }
            .minBy { it.value }.key

        credentialsWithHostOnly.isNotEmpty() -> credentialsWithHostOnly.first()

        else -> Credentials(
            protocol = requestedCredentials.protocol,
            host = requestedCredentials.host,
            path = requestedCredentials.path,
            username = "",
            password = ""
        )
    }
}
