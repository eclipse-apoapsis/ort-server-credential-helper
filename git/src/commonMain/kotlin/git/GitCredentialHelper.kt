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

import okio.BufferedSource
import okio.Path

import org.eclipse.apoapsis.ortserver.credentialhelper.common.AuthenticationInfo
import org.eclipse.apoapsis.ortserver.credentialhelper.common.CredentialHelperResult
import org.eclipse.apoapsis.ortserver.credentialhelper.common.CredentialsRequest
import org.eclipse.apoapsis.ortserver.credentialhelper.common.RequestUrl
import org.eclipse.apoapsis.ortserver.credentialhelper.common.credentialHelperMain
import org.eclipse.apoapsis.ortserver.credentialhelper.common.getHomeDirectory

/** The name of the command that triggers a request for credentials. */
private const val GIT_GET_CREDENTIALS_COMMAND = "get"

/** A set with other valid commands that are not handled by this implementation. */
private val ignoredCommands = setOf("store", "erase")

/**
 * Generate credentials for Git based on the input provided by Git via stdin and the Git credentials file.
 * The expected input format from Git is a series of lines in the format `key=value`, i.e.:
 *
 * protocol=https
 * host=github.com
 * path=/eclipse-apoapsis/ort-server.git
 *
 * Standard input is then parsed to extract required parameters and the Git credentials file is read to find the best
 * matching credentials for the requested URL.
 *
 * The output is then generated in the format expected by Git, same as input, augmented with user and password, i.e.:
 *
 * protocol=https
 * host=github.com
 * path=/eclipse-apoapsis/ort-server.git
 * user=some-git-user
 * password=some-git-password
 *
 * See https://git-scm.com/docs/gitcredentials#_custom_helpers
 */
fun main(args: Array<String>) {
    val request = CredentialsRequest(
        inputReader = ::parseInputSource,
        resultGenerator = ::generateResult,
        credentialsFile = getExpectedGitCredentialsFilePath(),
        credentialCommand = GIT_GET_CREDENTIALS_COMMAND,
        ignoredCommands = ignoredCommands
    )

    credentialHelperMain(args, request)
}

/**
 * Data class representing a request for Git credentials, including the protocol, host, and path.
 */
internal data class GitCredentialRequest(
    val protocol: String?,
    val host: String?,
    val path: String?
)

/**
 * Return the expected, platform-specific [Path] to the Git credentials file.
 */
internal fun getExpectedGitCredentialsFilePath(): Path =
    getHomeDirectory().resolve(".git-credentials")

/**
 * Parse the given [inputSource] to extract the properties for a [GitCredentialRequest]. Return a [RequestUrl]
 * with this request or a failure result if mandatory properties are unspecified.
 */
private fun parseInputSource(inputSource: BufferedSource): Result<RequestUrl<GitCredentialRequest>> =
    runCatching {
        val stdinLines = generateSequence { inputSource.readUtf8Line() }
            .toCollection(mutableListOf())
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val request = parseRequestFromStdinLines(stdinLines)
        requireNotNull(request.host) { "Missing mandatory input property 'host'." }

        RequestUrl(request.toUrl(), request)
    }

/**
 * Parse the input parameter lines [stdinLines] received from stdin and return a [GitCredentialRequest] object.
 *
 * The input parameter lines are expected to be in the format:
 * protocol=https
 * host=github.com
 * path=oss-review-toolkit/ort.git
 */
internal fun parseRequestFromStdinLines(stdinLines: List<String>): GitCredentialRequest {
    val requestParams = stdinLines
        .filter { it.contains("=") }
        .associate { it.substringBefore("=").trim() to it.substringAfter("=").trim() }

    return GitCredentialRequest(
        protocol = requestParams["protocol"],
        host = requestParams["host"],
        path = requestParams["path"]
    )
}

/**
 * Generate the output for the given [request] and the found [authInfo]. The output is in the same format as the
 * input, plus additional attributes if an [AuthenticationInfo] is available.
 */
private fun generateOutput(request: GitCredentialRequest, authInfo: AuthenticationInfo?): String =
    buildList {
        addAttribute("protocol", request.protocol)
        addAttribute("host", request.host)
        addAttribute("path", request.path)
        addAttribute("username", authInfo?.username)
        addAttribute("password", authInfo?.password)
    }.joinToString(separator = "\n")

/**
 * Generate the result object for the current credential helper invocation based on the given [requestUrl] and
 * found [authInfo].
 */
private fun generateResult(
    requestUrl: RequestUrl<GitCredentialRequest>,
    authInfo: AuthenticationInfo?
): CredentialHelperResult =
    CredentialHelperResult(
        output = generateOutput(requestUrl.context, authInfo),
        // Status is always 0; if no credentials were found, the output will simply not contain user and
        // password attributes, which is the expected behavior for Git.
        exitCode = 0
    )

/**
 * Convert the properties of this request to a URL that is expected by the credential helper framework.
 */
private fun GitCredentialRequest.toUrl(): String =
    "${protocol ?: "https"}://${host}${path?.let { if (it.startsWith("/")) it else "/$it" }.orEmpty()}"

/**
 * Add the given [key]=[value] pair to this list if the [value] is not null.
 */
private fun MutableList<String>.addAttribute(key: String, value: String?) =
    value?.also { add("$key=$it") }
