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

package org.eclipse.apoapsis.ortserver.credentialhelper

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.platform.MultiplatformSystem.exitProcess

const val COMMAND_NAME = "credentialhelper"

fun main(args: Array<String>) {
    CredentialHelper().main(args)
    exitProcess(0)
}

/**
 * The main class for the credentials helper.
 * This class is responsible for parsing the command line arguments and executing the appropriate actions.
 */
class CredentialHelper : CliktCommand(COMMAND_NAME) {
    override fun run() {
    }

    private val help by option(
        "--help",
        help = "Placeholder for help message."
    )
}
