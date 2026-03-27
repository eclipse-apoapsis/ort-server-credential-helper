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
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class UtilsTest : WordSpec({
    "parseInputParamLines" should {
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
    }

    "parseGitCredentialsFileContent" should {
        "parse simple Git credential file content properly" {
            val credentialsFileContent = readResourceFileContent("git/git-credentials")
            val credentials = parseGitCredentialsFileContent(credentialsFileContent)

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
            val credentialsFileContent = readResourceFileContent("git/git-credentials-empty")
            val credentials = parseGitCredentialsFileContent(credentialsFileContent)

            credentials.shouldNotBeNull()
            credentials.shouldBeEmpty()
        }

        "omit lines that do not contain credentials" {
            val credentialsFileContent = readResourceFileContent("git/git-credentials-no-creds")
            val credentials = parseGitCredentialsFileContent(credentialsFileContent)

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

        "properly encode special characters in username and password" {
            val credentialsFileContent = readResourceFileContent("git/git-credentials-special-chars")
            val credentials = parseGitCredentialsFileContent(credentialsFileContent)

            credentials.shouldNotBeNull()
            credentials.shouldContainExactly(
                listOf(
                    Credentials(
                        protocol = "https",
                        host = "github.com",
                        path = "oss-review-toolkit/ort.git",
                        username = "usern@me",
                        password = "p@ssw:ord"
                    )
                )
            )
        }

        "remove trailing parameters from path" {
            val credentialsFileContent = readResourceFileContent("git/git-credentials-path-params")
            val credentials = parseGitCredentialsFileContent(credentialsFileContent)

            credentials.shouldNotBeNull()
            credentials.shouldContainExactly(
                listOf(
                    Credentials(
                        protocol = "https",
                        host = "github.com",
                        path = "oss-review-toolkit/ort.git",
                        username = "username",
                        password = "pA3sW0Rd"
                    )
                )
            )
        }
    }

    "findClosestMatchingCredential" should {
        "match credentials with same host and exact path" {
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
                    username shouldBe "user4"
                    password shouldBe "password444"
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
                    username shouldBe "user2"
                    password shouldBe "password222"
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
                    username shouldBe "user2"
                    password shouldBe "password222"
                }
        }

        "return input credentials if no matching host is available" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "nonexisting-host.com",
                path = ""
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "nonexisting-host.com"
                    path shouldBe ""
                    username shouldBe ""
                    password shouldBe ""
                }
        }

        "return input credentials if no host matched, but path of other host matches" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "nonexisting-host.com",
                path = "oss-review-toolkit/ort.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "nonexisting-host.com"
                    path shouldBe "oss-review-toolkit/ort.git"
                    username shouldBe ""
                    password shouldBe ""
                }
        }

        "return best matching path (up to 2nd level)" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "oss-review-toolkit/project1/gort.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe "oss-review-toolkit/project1"
                    username shouldBe "user5"
                    password shouldBe "password555"
                }
        }

        "return best matching path (up to 1st level)" {
            val credentialsToMatch = CredentialRequest(
                protocol = "https",
                host = "github.com",
                path = "oss-review-toolkit/project3/ort.git"
            )

            findClosestMatchingCredential(credentialsToMatch, credentialsList)
                .shouldNotBeNull {
                    protocol shouldBe "https"
                    host shouldBe "github.com"
                    path shouldBe "oss-review-toolkit"
                    username shouldBe "user3"
                    password shouldBe "password333"
                }
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

private fun readResourceFileContent(resourcePath: String) =
    ClassLoader.getSystemResource(resourcePath).readText()

private val credentialsList = listOf(
    Credentials(
        protocol = "https",
        host = "example.com",
        path = "",
        username = "user1",
        password = "password111"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "",
        username = "user2",
        password = "password222"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit",
        username = "user3",
        password = "password333"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit/ort.git",
        username = "user4",
        password = "password444"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit/project1",
        username = "user5",
        password = "password555"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit/project1/ort.git",
        username = "user6",
        password = "password666"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit/project2",
        username = "user7",
        password = "password777"
    ),
    Credentials(
        protocol = "https",
        host = "github.com",
        path = "oss-review-toolkit/project2/ort.git",
        username = "user8",
        password = "password888"
    )
)

