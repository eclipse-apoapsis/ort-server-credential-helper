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

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe

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

class BazelCredentialHelperTest : WordSpec({
    afterEach {
        unmockkAll()
    }

    "main" should {
        "specify the correct command to look up credentials" {
            testBazelCredentialHelper { request ->
                request.credentialCommand shouldBe "get"
            }
        }

        "specify no ignored commands" {
            testBazelCredentialHelper { request ->
                request.ignoredCommands shouldBe emptySet()
            }
        }

        "specify the correct path to the credentials file" {
            testBazelCredentialHelper { request ->
                val credentialsFile = request.credentialsFile
                credentialsFile.name shouldBe ".bazel-credentials"
                credentialsFile.parent shouldBe getHomeDirectory()
            }
        }

        "parse a valid JSON input correctly" {
            testBazelCredentialHelper { request ->
                val inputSource = createSource("""{"uri": "https://registry.example.com/some/path"}""")

                val result = request.inputReader(inputSource)

                result shouldBeSuccess RequestUrl(
                    url = "https://registry.example.com/some/path",
                    context = Unit
                )
            }
        }

        "fail to parse invalid JSON input" {
            testBazelCredentialHelper { request ->
                val inputSource = createSource("this is not valid json")

                val result = request.inputReader(inputSource)

                result.shouldBeFailure()
            }
        }

        "produce a Basic Auth Authorization header if credentials were found" {
            val uri = "https://registry.example.com/some/path"
            val authInfo = AuthenticationInfo(
                host = "registry.example.com",
                path = "/some/path",
                username = "alice",
                password = "secret"
            )

            testBazelCredentialHelper { request ->
                val result = request.resultGenerator(RequestUrl(uri, Unit), authInfo)

                result.exitCode shouldBe 0

                val expectedResponse = GetCredentialResponse(
                    headers = mapOf("Authorization" to listOf("Basic ${encodeBasicAuth("alice", "secret")}"))
                )
                val response = json.decodeFromString<GetCredentialResponse>(result.output)
                response shouldBe expectedResponse
            }
        }

        "produce an empty headers map if no credentials were found" {
            val uri = "https://registry.example.com/some/path"

            testBazelCredentialHelper { request ->
                val result = request.resultGenerator(RequestUrl(uri, Unit), null)

                result.exitCode shouldBe 0

                val response = json.decodeFromString<GetCredentialResponse>(result.output)
                response.headers shouldBe emptyMap()
            }
        }
    }
})

/**
 * Execute a test of the main function of the Bazel credential helper application. Use the given [block] to test the
 * request passed to the credential helper framework.
 */
private fun testBazelCredentialHelper(block: (CredentialsRequest<Unit>) -> Unit) {
    mockkStatic("org.eclipse.apoapsis.ortserver.credentialhelper.common.CredentialHelperKt")

    val args = arrayOf("get")
    val slotRequest = slot<CredentialsRequest<Unit>>()
    every { credentialHelperMain(args, capture(slotRequest)) } just runs

    main(args)

    block(slotRequest.captured)
}

/**
 * Create a [BufferedSource] that provides the given [content] as its content.
 */
private fun createSource(content: String): BufferedSource =
    Buffer().also { it.writeUtf8(content) }
