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

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.toKString

import okio.Buffer
import okio.BufferedSource
import okio.Path
import okio.Path.Companion.toPath
import okio.Source
import okio.Timeout
import okio.buffer

import platform.posix.STDIN_FILENO
import platform.posix.getenv
import platform.posix.getpwuid
import platform.posix.getuid
import platform.posix.read

@OptIn(ExperimentalForeignApi::class)
actual fun getTmpDir(): Path = requireNotNull(
    getEnv("TMPDIR")?.toPath() ?: "/tmp".toPath()
)

@OptIn(ExperimentalForeignApi::class)
actual fun getHomeDirectory(): Path = requireNotNull(
    getEnv("HOME")?.toPath() ?: getpwuid(getuid())?.pointed?.pw_dir?.toKString()?.toPath()
)

actual fun stdinSource(): BufferedSource = PosixStdinSource().buffer()

@OptIn(ExperimentalForeignApi::class)
private fun getEnv(name: String) = getenv(name)?.toKString()

/**
 * A [Source] implementation that reads from stdin via the POSIX [read] syscall on file descriptor 0.
 * This implementation is used as return value for the [stdinSource] function.
 */
@OptIn(ExperimentalForeignApi::class)
private class PosixStdinSource : Source {
    private companion object {
        const val BUFFER_SIZE = 8192L
    }

    override fun read(sink: Buffer, byteCount: Long): Long {
        val toRead = minOf(byteCount, BUFFER_SIZE)
        return memScoped {
            val buf = allocArray<ByteVar>(toRead)
            val bytesRead = read(STDIN_FILENO, buf, toRead.toULong())
            when {
                bytesRead > 0L -> {
                    sink.write(buf.readBytes(bytesRead.toInt()))
                    bytesRead
                }

                else -> -1L
            }
        }
    }

    override fun timeout(): Timeout = Timeout.NONE

    override fun close() { /* stdin is not closed. */ }
}
