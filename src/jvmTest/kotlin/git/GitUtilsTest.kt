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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class UtilsTest : WordSpec({
    "parseInputParamLines" should ({
        "properly parse credentials request lines" {
            val requestLines = listOf(
                "protocol=https",
                "host=github.com",
                "path=oss-review-toolkit/ort.git"
            )

            parseRequestFromStdinLines(requestLines)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe "oss-review-toolkit/ort.git"
                }
        }

        "omit lines that do not contain an equals sign" {
            val requestLines = listOf(
                "protocol=https",
                "host=github.com",
                "invalid-line-without-equals-sign",
                "path=oss-review-toolkit/ort.git"
            )

            parseRequestFromStdinLines(requestLines)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe "oss-review-toolkit/ort.git"
                }
        }

        "omit empty lines" {
            val requestLines = listOf(
                "protocol=https",
                "",
                "host=github.com",
                "   ",
                "path=oss-review-toolkit/ort.git"
            )

            parseRequestFromStdinLines(requestLines)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe "oss-review-toolkit/ort.git"
                }
        }
    })

    "parseGitCredentialsFileContent" should ({
        "parse simple git credential file content properly" {
            val credentials = readResourceContent("git/git-credentials")

            credentials.shouldNotBeNull()
            credentials.shouldContainExactlyInAnyOrder(
                listOf(
                    Credentials(
                        protocol = "https",
                        host = "github.com",
                        path = "oss-review-toolkit/ort.git",
                        username = "token",
                        password = "abc123def456ghi789"
                    ),
                    Credentials(
                        protocol = "https",
                        host = "github.com",
                        path = "",
                        username = "user1",
                        password = "user1pass"
                    )
                )
            )
        }

        "return empty list if file is empty" {
            val credentials = readResourceContent("git/git-credentials-empty")

            credentials.shouldNotBeNull()
            credentials.shouldBeEmpty()
        }

        "omit lines that does not contain credentials" {
            val credentials = readResourceContent("git/git-credentials-no-creds")
            credentials.shouldNotBeNull()
            credentials.shouldContainExactlyInAnyOrder(
                listOf(
                    Credentials(
                        protocol = "https",
                        host = "github.com",
                        path = "",
                        username = "user1",
                        password = "user1pass"
                    ),
                    Credentials(
                        protocol = "https",
                        host = "github.com",
                        path = "eclipse-apoapsis/ort-server-credential-helper.git",
                        username = "user1",
                        password = "user1pass"
                    )
                )
            )
        }
    })

    "findClosestMatchingCredential" should ({
        "match credentials with same host and path" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "oss-review-toolkit/ort.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe "oss-review-toolkit/ort.git"
                    username shouldBe "user2"
                    password shouldBe "passwordABC"
                }
        }

        "match credentials with same host and empty path" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "github.com",
                path = ""
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe ""
                    username shouldBe "user1"
                    password shouldBe "passwordXYZ"
                }
        }

        "match credentials with empty path if no credentials with matching path are available" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "some-non-existing/path.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe ""
                    username shouldBe "user1"
                    password shouldBe "passwordXYZ"
                }
        }

        "return input credentials if no matching credentials are available" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "example.com",
                path = ""
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "example.com"
                    path shouldBe ""
                    username shouldBe ""
                    password shouldBe ""
                }
        }

        "return input credentials if no matching credentials are available for empty path" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "example.com",
                path = "some-path/path.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "example.com"
                    path shouldBe "some-path/path.git"
                    username shouldBe ""
                    password shouldBe ""
                }
        }

        "return input credentials if there is no matching host, but matching path" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "other.github.com",
                path = "oss-review-toolkit/blabla.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "other.github.com"
                    path shouldBe "oss-review-toolkit/blabla.git"
                    username shouldBe ""
                    password shouldBe ""
                }
        }

        "return full path match" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "oss-review-toolkit/project1/ort.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsListForPatchMatchingTests)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe "oss-review-toolkit/project1/ort.git"
                    username shouldBe "user3"
                    password shouldBe "password333"
                }
        }

        "return best matching path (up to 2nd level)" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "oss-review-toolkit/project1/gort.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsListForPatchMatchingTests)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe "oss-review-toolkit/project1"
                    username shouldBe "user4"
                    password shouldBe "password444"
                }
        }

        "return best mathing path (up to 1st level)" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "oss-review-toolkit/project2/ort.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsListForPatchMatchingTests)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe "oss-review-toolkit"
                    username shouldBe "user5"
                    password shouldBe "password555"
                }
        }
    })
})

private fun readResourceContent(resourcePath: String) =
    parseGitCredentialsFileContent(ClassLoader.getSystemResource(resourcePath).readText())

private val credentialsList = listOf(
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "",
        username = "user1",
        password = "passwordXYZ"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit/ort.git",
        username = "user2",
        password = "passwordABC"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "apoapsis/ort-server.git",
        username = "user3",
        password = "password123"
    ),
    Credentials(
        protocol = "https",
        host = "example.com",
        path = "apoapsis/ort-server.git",
        username = "user4",
        password = "password987"
    ),
    Credentials(
        protocol = "https",
        host = "example.com",
        path = "oss-review-toolkit/ort.git",
        username = "user4",
        password = "password987"
    ),
    Credentials(
        protocol = "https",
        host = "example.com",
        path = "oss-review-toolkit/blabla.git",
        username = "user4",
        password = "password987"
    )
)

private val credentialsListForPatchMatchingTests = listOf(
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "",
        username = "user1",
        password = "password111"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit/ort.git",
        username = "user2",
        password = "password222"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit/project1/ort.git",
        username = "user3",
        password = "password333"
    ),
    Credentials(
        protocol = "https",
        host = "example.com",
        path = "oss-review-toolkit/project1/ort.git",
        username = "userA",
        password = "passwordAAA"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit/project1",
        username = "user4",
        password = "password444"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit",
        username = "user5",
        password = "password555"
    )
)
