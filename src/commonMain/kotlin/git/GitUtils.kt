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
 * Return the expected, platform-specific [Path] to the Git credentials file.
 */
internal expect fun getExpectedGitCredentialsFilePath(): Path

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

/**
 * Decode a URL-encoded [String] by replacing percent-encoded characters with their equivalents,
 * like in table listed below:
 *
 *    +     %20  %21 %23 %24 %26 %27 %28 %29 %2A %2B %2C %2F %3A %3B %3D %3F %40 %5B %5D
 *  space  space  !   #   $   &   '   (   )   *   +   ,   /   :   ;   =   ?   @   [   ]
 *
 * In case of invalid percent-encoding, the '%' character is kept as is in the output.
 *
 * Return the decoded [String] with special characters replaced.
 */
internal fun String.urlDecode(): String {
    val output = StringBuilder()
    var i = 0
    while (i < this.length) {
        when (val c = this[i]) {
            '+' -> output.append(' ')
            '%' -> {
                try {
                    val hex = this.substring(i + 1, i + 3)
                    val byteValue = hex.toInt(16).toByte()
                    output.append(byteValue.toInt().toChar())
                    i += 2
                } catch (_: RuntimeException) {
                    output.append('%')
                }
            }
            else -> output.append(c)
        }
        i++
    }
    return output.toString()
}

/**
 * Read the content of the Git credentials file located at the expected path.
 * Return the content of the Git credentials file as a [String].
 */
internal fun readGitCredentialFileContent(): String =
    FileSystem.SYSTEM.source(getExpectedGitCredentialsFilePath()).buffer()
        .readUtf8()

/**
 * Parse the content of a Git credentials file and returns a list of [Credentials] objects.
 *
 * The Git credentials file is expected to have lines in the format:
 * `protocol://username:password@host/path`
 *
 * Parse the string content of Git gretentials file passed in [fileContent].
 * Return a list of [Credentials] objects parsed from the file content.
 */
internal fun parseGitCredentialsFileContent(fileContent: String): List<Credentials> =
    fileContent.split("\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .filter { it.contains("@") } // Indicates, that the line contains credentials in the expected format
        .map { it.toCredentials() }

/**
 * Return a [Credentials] object containing information parsed from received [String] in
 * format `protocol://username:password@host/path`.
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
        path = hostAndPathPart.contains("/")
            .let { if (it) hostAndPathPart.substringAfter("/").substringBefore("?").trim() else "" },
        username = credentialsPart.substringBefore(":").trim().urlDecode(),
        password = credentialsPart.substringAfter(":").trim().urlDecode()
    )
}

/**
 * Find the closest matching [Credentials] for a given [requestedCredentials] from a list of [availableCredentials].
 * The matching is based on the host and path of the URL. in case of multiple matches, the one with the longest
 * matching path is returned. If no match is found, return [requestedCredentials].
 */
internal fun findClosestMatchingCredential(
    requestedCredentials: CredentialRequest,
    availableCredentials: List<Credentials>
): Credentials {
    val credentialsForHost = availableCredentials.filter { it.host == requestedCredentials.host }

    return credentialsForHost.find { it.path == requestedCredentials.path }
        ?: credentialsForHost.filter {
            it.path.isNotEmpty() && requestedCredentials.path.startsWith(it.path + "/")
        }.maxByOrNull { it.path.length }
        ?: credentialsForHost.find { it.path.isEmpty() }
        ?: Credentials(
            protocol = requestedCredentials.protocol,
            host = requestedCredentials.host,
            path = requestedCredentials.path,
            username = "",
            password = ""
        )
}
