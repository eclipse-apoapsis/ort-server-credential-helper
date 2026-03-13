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

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.mordant.platform.MultiplatformSystem

import org.eclipse.apoapsis.ortserver.credentialhelper.LogLevel
import org.eclipse.apoapsis.ortserver.credentialhelper.log

const val COMMAND_PARAM_NAME = "git"

/**
 * Generate credentials for git based on the input provided by git via stdin and the git credentials file.
 * The expected input format from git is a series of lines in the format `key=value`, i.e.:
 *
 * protocol=https
 * host=github.com
 * path=/eclipse-apoapsis/ort-server.git
 *
 * Standard input is then parsed to extract reqired parameters and the git credentials file is read to find the best
 * matching credentials for the requested URL.
 *
 * The output is then generated in the format expected by git, same as input, augmented with user and password, i.e.:
 *
 * protocol=https
 * host=github.com
 * path=/eclipse-apoapsis/ort-server.git
 * user=some-git-user
 * password=some-git-password
 */
internal class GitCredentialHelper : CliktCommand(COMMAND_PARAM_NAME) {
    val commandLineArguments: List<String> by argument().multiple()

    /**
     * Generate credentials for git based on the provided command line arguments and the input from stdin.
     *
     * @param[commandLineArguments] The command line arguments parsed by Clikt. First argument is expected to be "get".
     */
    override fun run() {
        log("GIT Credential Helper called with arguments: $commandLineArguments")

        // Only "get" git action is supported and should be passed by git as first arg.
        if (commandLineArguments.isEmpty() || commandLineArguments[0] != "get") {
            log("No git action provided. Expected 'get' as first argument.", LogLevel.ERROR)
            MultiplatformSystem.exitProcess(1)
        }

        val stdinLines = generateSequence { readlnOrNull() }
            .toCollection(mutableListOf())
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val request = parseRequestFromStdinLines(stdinLines)
        log("Parsed requested credentials: $request")

        if (request.host.isEmpty() || request.protocol.isEmpty()) {
            log("Missing required parameters. Host and protocol must be provided.", LogLevel.ERROR)
            MultiplatformSystem.exitProcess(1)
        }

        val matchedCredentials = findCredentialsForUrl(request)
        log("Found matching credentials: $matchedCredentials")

        generateOutput(matchedCredentials)

        log("Git credential helper execution completed successfully.")
    }

    private fun findCredentialsForUrl(credentialsToMatch: CredentialRequest): Credentials {
        val credentialsFileContent = readGitCredentialFileContent()
        val availableCredentials = parseGitCredentialsFileContent(credentialsFileContent)
        return findClosestMatchingCredential(credentialsToMatch, availableCredentials)
    }

    private fun generateOutput(credentials: Credentials) {
        println("protocol=${credentials.protocol}")
        println("host=${credentials.host}")
        println("user=${credentials.username}")
        println("password=${credentials.password}")
    }
}

/**
 * Data class representing a request for git credentials, including the protocol, host, and path.
 */
internal data class CredentialRequest(
    val protocol: String,
    val host: String,
    val path: String = ""
)

/**
 * Data class representing the credentials for a git repository,
 * including the protocol, host, path, username, and password.
 */
internal data class Credentials(
    val protocol: String,
    val host: String,
    val path: String,
    val username: String,
    val password: String
) {
    override fun toString(): String {
        return "protocol='$protocol', " +
            "host='$host', " +
            "path='$path', " +
            "username=$username, " +
            "password=${if (password.isEmpty()) "" else password.replace(Regex("^.*"), "************")}"
    }
}
