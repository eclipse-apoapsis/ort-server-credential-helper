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

import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.beEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

import kotlin.jvm.java

import okio.Path
import okio.Path.Companion.toPath

class CredentialsParserTest : WordSpec({
    "parseCredentialsFile()" should {
        "parse a simple credentials file correctly" {
            val credentials = parseCredentialsFile(resourcePath("git-credentials"))

            credentials.shouldContainExactlyInAnyOrder(
                listOf(
                    AuthenticationInfo(
                        host = "github.com",
                        path = "oss-review-toolkit/ort.git",
                        username = "token",
                        password = "abc123def456ghi789"
                    ),
                    AuthenticationInfo(
                        host = "github.com",
                        path = null,
                        username = "user1",
                        password = "user1pass"
                    )
                )
            )
        }

        "parse an empty credentials file correctly" {
            val credentials = parseCredentialsFile(resourcePath("git-credentials-empty"))

            credentials should beEmpty()
        }

        "ignore entries without credentials" {
            val credentials = parseCredentialsFile(resourcePath("git-credentials-no-creds"))

            credentials shouldContainExactlyInAnyOrder listOf(

                AuthenticationInfo(
                    host = "github.com",
                    path = null,
                    username = "user1",
                    password = "user1pass"
                ),
                AuthenticationInfo(
                    host = "github.com",
                    path = "eclipse-apoapsis/ort-server-credential-helper.git",
                    username = "user1",
                    password = "user1pass"
                )
            )
        }

        "decode special characters in username and password" {
            val credentials = parseCredentialsFile(resourcePath("git-credentials-special-chars"))

            credentials shouldContainExactlyInAnyOrder listOf(
                AuthenticationInfo(
                    host = "github.com",
                    path = "oss-review-toolkit/ort.git",
                    username = "usern@me",
                    password = "p@ssw:ord"
                )
            )
        }

        "remove query parameters" {
            val credentials = parseCredentialsFile(resourcePath("git-credentials-path-params"))

            credentials shouldContainExactlyInAnyOrder listOf(
                AuthenticationInfo(
                    host = "github.com",
                    path = "oss-review-toolkit/ort.git",
                    username = "username",
                    password = "pA3sW0Rd"
                )
            )
        }
    }

    "urlDecode" should {
        "decode plus signs to spaces" {
            "user+name".urlDecode() shouldBe "user name"
        }

        "decode percent-encoded characters" {
            "p%40ssw%3Ard".urlDecode() shouldBe "p@ssw:rd"
            "p%40ssw%20rd".urlDecode() shouldBe "p@ssw rd"
        }

        "handle invalid percent-encoding gracefully" {
            "invalid%2".urlDecode() shouldBe "invalid%2"
            "invalid%2G".urlDecode() shouldBe "invalid%2G"
        }

        "leave non-encoded characters unchanged" {
            "username".urlDecode() shouldBe "username"
        }
    }
})

/**
 * Return a [Path] to the resource file with the given [resourceName].
 */
private fun resourcePath(resourceName: String): Path {
    val resourceUrl = CredentialsParserTest::class.java.getResource("/$resourceName")
        ?: throw IllegalArgumentException("Resource file not found: $resourceName")
    return java.nio.file.Paths.get(resourceUrl.toURI()).toString().toPath()
}
