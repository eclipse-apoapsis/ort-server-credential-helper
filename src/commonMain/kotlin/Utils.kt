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

import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer
import okio.use

const val CREDENTIAL_HELPER_LOG_FILE_NAME = "credential-helper.log"

/**
 * Return the expected path to the temporary directory based on the operating system.
 *
 * @return The expected [Path] to the temporary directory, which is platform-specific.
 */
expect fun getTmpDir(): Path

/**
 * Log a message to a log file in the temporary directory.
 * As logs can't be printed to the console when the credential helper is called, this function writes log messages to
 * a file named "git-credential-helper.log" in the temporary directory.
 * Each log message is prefixed with its log level (e.g., INFO, WARN, ERROR) for better readability.
 *
 * @param[message] The message to log.
 * @param[level] The log level of the message. Defaults to [LogLevel.INFO].
 */
fun log(message: String, level: LogLevel = LogLevel.INFO) {
    getTmpDir().resolve(CREDENTIAL_HELPER_LOG_FILE_NAME)
        .let { logFilePath ->
            FileSystem.SYSTEM.appendingSink(logFilePath, false)
                .buffer()
                .use { sink ->
                    sink.writeUtf8("${level.name}: $message\n")
                    sink.flush()
                }
        }
}

/**
 * Enum class representing the log levels for logging messages in the credential helper.
 */
enum class LogLevel {
    INFO,
    ERROR
}
