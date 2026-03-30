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

import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer

/**
 * Parse the credentials file at the given [path] and return a [List] of [AuthenticationInfo] objects representing the
 * credentials and the services they apply to stored in the file.
 */
fun parseCredentialsFile(path: Path): List<AuthenticationInfo> {
    Logger.instance.log("Parsing credentials file at path: '$path'.")

    val content = readCredentialFileContent(path)
    return parseCredentialsFileContent(content)
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
 * Read the content of the credentials file at the given [path] and return it as a [String].
 */
internal fun readCredentialFileContent(path: Path): String =
    FileSystem.SYSTEM.source(path).buffer().readUtf8()

/**
 * Regex to parse a URL of the form `protocol://username:password@host/path`.
 * The password may contain '@' characters; the last '@' before the host is used as the separator.
 * The credentials part (`username:password@`) as well as the path and query-string components are optional.
 * Any query parameters (starting with '?') are matched but not captured, effectively stripping them.
 *
 * Named groups:
 * - `username`: URL-encoded username (optional)
 * - `password`: URL-encoded password, may contain '@' and ':' (optional)
 * - `host`:     hostname (and optional port)
 * - `path`:     optional path after the host, without leading slash and without query parameters
 */
internal val URL_REGEX =
    Regex("""^[^:]+://(?:(?<username>[^:]+):(?<password>.+)@)?(?<host>[^/?]+)(?:/(?<path>[^?]*))?(?:\?.*)?$""")

/**
 * Parse the content of a credentials file and return a list of [AuthenticationInfo] objects.
 *
 * The Git credentials file is expected to have lines in the format:
 * `protocol://username:password@host/path`.
 * See https://git-scm.com/docs/git-credential-store.
 */
private fun parseCredentialsFileContent(fileContent: String): List<AuthenticationInfo> =
    fileContent.split("\n")
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { URL_REGEX.matchEntire(it) }
        .filter { match -> match.groups["username"] != null && match.groups["password"] != null }
        .map { it.toAuthenticationInfo() }
        .toList()

/**
 * Return an [AuthenticationInfo] object from a [MatchResult] of [URL_REGEX].
 */
private fun MatchResult.toAuthenticationInfo(): AuthenticationInfo =
    AuthenticationInfo(
        host = requireNotNull(groups["host"]).value,
        path = groups["path"]?.value?.takeIf { it.isNotEmpty() },
        username = groups["username"]?.value.orEmpty().urlDecode(),
        password = groups["password"]?.value.orEmpty().urlDecode()
    )
