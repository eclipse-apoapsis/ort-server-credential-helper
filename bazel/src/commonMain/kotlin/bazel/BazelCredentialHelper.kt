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

package org.eclipse.apoapsis.ortserver.credentialhelper.bazel

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeFromBufferedSource

import okio.BufferedSource

import org.eclipse.apoapsis.ortserver.credentialhelper.common.AuthenticationInfo
import org.eclipse.apoapsis.ortserver.credentialhelper.common.CredentialHelperResult
import org.eclipse.apoapsis.ortserver.credentialhelper.common.CredentialsRequest
import org.eclipse.apoapsis.ortserver.credentialhelper.common.RequestUrl
import org.eclipse.apoapsis.ortserver.credentialhelper.common.credentialHelperMain
import org.eclipse.apoapsis.ortserver.credentialhelper.common.getHomeDirectory

/** The name of the command that triggers a request for credentials. */
private const val BAZEL_GET_CREDENTIALS_COMMAND = "get"

/** The name of the file containing the credentials for Bazel. */
private const val BAZEL_CREDENTIALS_FILE_NAME = ".bazel-credentials"

/** The JSON instance used to serialize and deserialize Bazel credential helper messages. */
internal val json = Json { ignoreUnknownKeys = true }

/**
 * Generate credentials for Bazel based on the input provided via stdin as JSON and the Bazel credentials file.
 *
 * The expected input format is a JSON object with a `uri` field:
 * ```json
 * { "uri": "https://registry.example.com/some/path" }
 * ```
 *
 * If credentials are found for the given URI, the output is a JSON object containing an `Authorization` header
 * with a Basic Auth value:
 * ```json
 * { "headers": { "Authorization": ["Basic <base64(user:password)>"] } }
 * ```
 *
 * If no credentials are found, an empty headers map is returned, but the exit code is still 0, since the request was
 * handled completely.
 *
 * See https://github.com/EngFlow/credential-helper-spec
 */
fun main(args: Array<String>) {
    val request = CredentialsRequest(
        inputReader = ::parseInputSource,
        resultGenerator = { _, authInfo -> generateResult(authInfo) },
        credentialsFile = getHomeDirectory().resolve(BAZEL_CREDENTIALS_FILE_NAME),
        credentialCommand = BAZEL_GET_CREDENTIALS_COMMAND
    )

    credentialHelperMain(args, request)
}

/**
 * Parse the given [inputSource] (JSON) to extract a [RequestUrl] with the URI from the [GetCredentialRequest].
 */
@OptIn(ExperimentalSerializationApi::class)
private fun parseInputSource(inputSource: BufferedSource): Result<RequestUrl<Unit>> =
    runCatching {
        val credentialRequest = json.decodeFromBufferedSource<GetCredentialRequest>(inputSource)
        RequestUrl(url = credentialRequest.uri, context = Unit)
    }

/**
 * Generate the result for the given [authInfo]. If credentials have been found for the current request, the response
 * contains a Basic Auth `Authorization` header. Otherwise, no headers are generated.
 */
private fun generateResult(authInfo: AuthenticationInfo?): CredentialHelperResult {
    val response = authInfo?.let { info ->
        val basicToken = encodeBasicAuth(info.username, info.password)
        GetCredentialResponse(headers = mapOf("Authorization" to listOf("Basic $basicToken")))
    } ?: GetCredentialResponse()

    return CredentialHelperResult(
        output = json.encodeToString(GetCredentialResponse.serializer(), response),
        exitCode = 0
    )
}

/**
 * Encode the given [username] and [password] as a Base64-encoded Basic Auth token.
 */
@OptIn(ExperimentalEncodingApi::class)
internal fun encodeBasicAuth(username: String, password: String): String =
    Base64.encode("$username:$password".encodeToByteArray())
