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

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll

import okio.Buffer
import okio.BufferedSource

import org.eclipse.apoapsis.ortserver.credentialhelper.common.AuthenticationInfo
import org.eclipse.apoapsis.ortserver.credentialhelper.common.CredentialsRequest
import org.eclipse.apoapsis.ortserver.credentialhelper.common.RequestUrl
import org.eclipse.apoapsis.ortserver.credentialhelper.common.credentialHelperMain
import org.eclipse.apoapsis.ortserver.credentialhelper.common.getHomeDirectory

class GitCredentialHelperTest : WordSpec({
    afterEach {
        unmockkAll()
    }

    "parseInputParamLines" should {
        "properly parse credentials request lines" {
            val requestLines = listOf(
                "protocol=https",
                "host=github.com",
                "path=/oss-review-toolkit/ort.git"
            )

            parseRequestFromStdinLines(requestLines) shouldBe GitCredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "/oss-review-toolkit/ort.git"
            )
        }

        "omit lines that do not contain an equals sign" {
            val requestLines = listOf(
                "protocol=https",
                "host=github.com",
                "invalid-line-without-equals-sign",
                "path=/oss-review-toolkit/ort.git"
            )

            parseRequestFromStdinLines(requestLines) shouldBe GitCredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "/oss-review-toolkit/ort.git"
            )
        }

        "omit empty lines" {
            val requestLines = listOf(
                "protocol=https",
                "",
                "host=github.com",
                "   ",
                "path=/oss-review-toolkit/ort.git"
            )

            parseRequestFromStdinLines(requestLines) shouldBe GitCredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "/oss-review-toolkit/ort.git"
            )
        }

        "set null values for undefined attributes" {
            parseRequestFromStdinLines(emptyList()) shouldBe GitCredentialRequest(
                protocol = null,
                host = null,
                path = null
            )
        }
    }

    "main" should {
        "specify the correct command to look up credentials" {
            testGitCredentialHelper { request ->
                request.credentialCommand shouldBe "get"
            }
        }

        "specify the correct commands to ignore" {
            testGitCredentialHelper { request ->
                request.ignoredCommands shouldContainExactlyInAnyOrder setOf("store", "erase")
            }
        }

        "specify the correct path to the credentials file" {
            testGitCredentialHelper { request ->
                val credentialsFile = request.credentialsFile
                credentialsFile.name shouldBe ".git-credentials"
                credentialsFile.parent shouldBe getHomeDirectory()
            }
        }

        "parse input correctly" {
            testGitCredentialHelper { request ->
                val inputLines = listOf(
                    "protocol=https",
                    "host=github.com",
                    "path=oss-review-toolkit/ort.git"
                )
                val inputSource = createSource(inputLines)

                val result = request.inputReader(inputSource)

                result shouldBeSuccess RequestUrl(
                    url = "https://github.com/oss-review-toolkit/ort.git",
                    context = GitCredentialRequest(
                        protocol = "https",
                        host = "github.com",
                        path = "oss-review-toolkit/ort.git"
                    )
                )
            }
        }

        "parse input correctly if no path is provided" {
            testGitCredentialHelper { request ->
                val inputLines = listOf(
                    "protocol=https",
                    "host=github.com",
                    "suffix=oss-review-toolkit/ort.git"
                )
                val inputSource = createSource(inputLines)

                val result = request.inputReader(inputSource)

                result shouldBeSuccess RequestUrl(
                    url = "https://github.com",
                    context = GitCredentialRequest(
                        protocol = "https",
                        host = "github.com",
                        path = null
                    )
                )
            }
        }

        "parse input correctly if no protocol is provided" {
            testGitCredentialHelper { request ->
                val inputLines = listOf(
                    "spec=https",
                    "host=github.com",
                    "path=/oss-review-toolkit/ort.git"
                )
                val inputSource = createSource(inputLines)

                val result = request.inputReader(inputSource)

                result shouldBeSuccess RequestUrl(
                    url = "https://github.com/oss-review-toolkit/ort.git",
                    context = GitCredentialRequest(
                        protocol = null,
                        host = "github.com",
                        path = "/oss-review-toolkit/ort.git"
                    )
                )
            }
        }

        "fail to parse input if no host is provided" {
            testGitCredentialHelper { request ->
                val inputLines = listOf(
                    "protocol=https",
                    "path=/oss-review-toolkit/ort.git"
                )
                val inputSource = createSource(inputLines)

                val result = request.inputReader(inputSource)

                result.shouldBeFailure { exception ->
                    exception.shouldBeInstanceOf<IllegalArgumentException>()
                    exception.message shouldContain "host"
                }
            }
        }

        "produce correct output if credentials were found" {
            val gitRequest = GitCredentialRequest(
                protocol = "ftp",
                host = "example.com",
                path = "/some/repo.git"
            )
            val authInfo = AuthenticationInfo(
                host = "example.com",
                path = gitRequest.path,
                username = "scot",
                password = "tiger"
            )
            val expectedLines = listOf(
                "protocol=ftp",
                "host=example.com",
                "path=/some/repo.git",
                "username=scot",
                "password=tiger"
            )

            testGitCredentialHelperOutput(gitRequest, authInfo) { outputLines ->
                outputLines shouldContainExactlyInAnyOrder expectedLines
            }
        }

        "produce correct output if credentials were not found" {
            val gitRequest = GitCredentialRequest(
                protocol = "ftp",
                host = "example.com",
                path = "/some/repo.git"
            )
            val expectedLines = listOf(
                "protocol=ftp",
                "host=example.com",
                "path=/some/repo.git"
            )

            testGitCredentialHelperOutput(gitRequest, null) { outputLines ->
                outputLines shouldContainExactlyInAnyOrder expectedLines
            }
        }

        "skip undefined properties in the generated output" {
            val gitRequest = GitCredentialRequest(
                protocol = null,
                host = "example.com",
                path = null
            )
            val expectedLines = listOf("host=example.com")

            testGitCredentialHelperOutput(gitRequest, null) { outputLines ->
                outputLines shouldContainExactlyInAnyOrder expectedLines
            }
        }
    }
})

/**
 * Execute a test of the main function of the Git credential helper application. Use the given [block] to test the
 * request passed to the credential helper framework.
 */
private fun testGitCredentialHelper(block: (CredentialsRequest<GitCredentialRequest>) -> Unit) {
    mockkStatic("org.eclipse.apoapsis.ortserver.credentialhelper.common.CredentialHelperKt")

    val args = arrayOf("get")
    val slotRequest = slot<CredentialsRequest<GitCredentialRequest>>()
    every { credentialHelperMain(args, capture(slotRequest)) } just runs

    main(args)

    block(slotRequest.captured)
}

/**
 * Run a test for the output generation of the Git credential helper for the given [gitRequest] and the found
 * [authInfo]. Invoke the given verification [block] with the lines that have been produced.
 */
private fun testGitCredentialHelperOutput(
    gitRequest: GitCredentialRequest,
    authInfo: AuthenticationInfo?,
    block: (List<String>) -> Unit
) {
    testGitCredentialHelper { request ->
        val result = request.resultGenerator(RequestUrl("someUrl", gitRequest), authInfo)

        result.exitCode shouldBe 0
        block(result.output.split("\n").map { it.trim() })
    }
}

/**
 * Create a [BufferedSource] that provides the given [lines] as its content.
 */
private fun createSource(lines: List<String>): BufferedSource =
    Buffer().also { buffer ->
        lines.forEach { line -> buffer.writeUtf8(line).writeUtf8("\n") }
    }
