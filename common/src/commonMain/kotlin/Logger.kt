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
import okio.use

private const val CREDENTIAL_HELPER_LOG_FILE_NAME = "credential-helper.log"

/**
 * Simple logger class for the credential helper. As the credential helper is called by Git and can't print
 * logs to the console, this logger writes log messages to a file in the temporary directory.
 */
class Logger private constructor() {
    companion object {
        private val logFilePath: Path by lazy {
            getTmpDir().resolve(CREDENTIAL_HELPER_LOG_FILE_NAME)
        }

        val instance: Logger by lazy {
            Logger()
        }
    }

    /**
     * Log a [message], prefixed with [logLevel].
     * [LogLevel.INFO] is used by default if no log level is provided.
     */
    fun log(message: String, logLevel: LogLevel = LogLevel.INFO) {
        FileSystem.SYSTEM.appendingSink(logFilePath, false)
            .buffer()
            .use { sink ->
                sink.writeUtf8("${logLevel.name}: $message\n")
                sink.flush()
            }
    }

    /**
     * Enum class representing the log levels for logging messages in the credential helper.
     */
    enum class LogLevel {
        INFO,
        ERROR
    }
}
