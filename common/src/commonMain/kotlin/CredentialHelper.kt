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

/**
 * Handle an invocation of the credential helper with the given [command line arguments][args] and the given [request].
 * This function performs all steps to look up a specific set of credentials:
 * - It evaluates the command line.
 * - It processes the input from stdin to obtain the URL for which credentials are requested.
 * - It reads the correct file with credentials information.
 * - It finds the best-matching credentials.
 * - It triggers the generation of the output for the credential helper.
 * - It returns the correct exit code.
 * To achieve this, it delegates to some helper functions provided by the passed in [CredentialsRequest] object;
 * these need to be defined by concrete credential helper implementations to inject specific logic into the generic
 * framework.
 */
fun <C> credentialHelperMain(
    args: Array<String>,
    request: CredentialsRequest<C>,
    exit: (Int) -> Unit = ::exitProcess
) {
    val command = args.singleOrNull()
    when (command) {
        in request.ignoredCommands -> exit(0)
        request.credentialCommand -> handleCredentialRequest(request, exit)
        else -> exit(2)
    }
}

/**
 * Handle the given [request] to find credentials. Then exit the process with the given [exit] function.
 */
private fun <C> handleCredentialRequest(request: CredentialsRequest<C>, exit: (Int) -> Unit) {
    request.inputReader(stdinSource())
        .onSuccess { result -> findCredentials(request, result, exit) }
        .onFailure { error ->
            Logger.instance.log("Failed to read input: ${error.message}")
            exit(1)
        }
}

/**
 * Handle the given [request] to lookup credentials for the given [requestUrl]. Then exit the process with the
 * given [exit] function.
 */
private fun <C> findCredentials(request: CredentialsRequest<C>, requestUrl: RequestUrl<C>, exit: (Int) -> Unit) {
    Logger.instance.log("Request for URL '${requestUrl.url}'.")

    val authInfo = parseCredentialsFile(request.credentialsFile).findClosestMatch(requestUrl.url)
    val authInfoStr = authInfo?.let { "<host = ${it.host}, path = ${it.path}>" } ?: "<none>"
    Logger.instance.log("Found credentials: $authInfoStr.")

    val result = request.resultGenerator(requestUrl, authInfo)
    print(result.output)
    exit(result.exitCode)
}
