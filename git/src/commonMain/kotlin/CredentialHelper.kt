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
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.platform.MultiplatformSystem.exitProcess

import org.eclipse.apoapsis.ortserver.credentialhelper.common.Logger

fun main(args: Array<String>) {
    CredentialHelper()
        .subcommands(GitCredentialHelper())
        .main(args)
    exitProcess(0)
}

/**
 * The main class for the credentials helper.
 * Responsible for parsing the command line arguments and delegating the credential generation
 * to the appropriate helper based on the first argument.
 */
class CredentialHelper : CliktCommand() {
    override fun help(context: Context) = """
        ORT Server Credential Helper

        The tool to generate credentials for various CVS-es.

        Usage:
          credentialhelper <type> [options]

        Supported helpers:
          git     Generates a Git-style credential helper output.
    """.trimIndent()

    override fun run() {
        Logger.instance.log("Credentials helper started.")
    }
}
