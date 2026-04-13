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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.beNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class CredentialsMatcherTest : StringSpec({
    "Result should be null for an empty list" {
        emptyList<AuthenticationInfo>().findClosestMatch(testUrl()) should beNull()
    }

    "Result should be null if there is no matching host" {
        val infos = listOf(
            AuthenticationInfo(
                host = "other.$TEST_HOST",
                path = TEST_PATH,
                username = USERNAME,
                password = PASSWORD
            )
        )

        infos.findClosestMatch(testUrl()) should beNull()
    }

    "Result should be null for an invalid URL" {
        val infos = listOf(
            AuthenticationInfo(
                host = TEST_HOST,
                path = TEST_PATH,
                username = USERNAME,
                password = PASSWORD
            )
        )

        infos.findClosestMatch("not a valid URL") should beNull()
    }

    "An exact match should be found" {
        val match = AuthenticationInfo(
            host = TEST_HOST,
            path = TEST_PATH,
            username = USERNAME,
            password = PASSWORD
        )
        val infos = listOf(
            AuthenticationInfo(
                host = TEST_HOST,
                path = null,
                username = "",
                password = ""
            ),
            match,
            AuthenticationInfo(
                host = TEST_HOST,
                path = TEST_ORG,
                username = "",
                password = ""
            )
        )

        infos.findClosestMatch(testUrl()) shouldBe match
    }

    "A match for the host should be found if there is no matching path" {
        val match = AuthenticationInfo(
            host = TEST_HOST,
            path = null,
            username = USERNAME,
            password = PASSWORD
        )
        val infos = listOf(
            AuthenticationInfo(
                host = TEST_HOST,
                path = TEST_PATH,
                username = "",
                password = ""
            ),
            AuthenticationInfo(
                host = "other-host",
                path = TEST_PATH,
                username = "",
                password = ""
            ),
            match
        )

        infos.findClosestMatch(testUrl(org = "/another-org")) shouldBe match
    }

    "A match for the prefix path should be found it if there is no matching path" {
        val match = AuthenticationInfo(
            host = TEST_HOST,
            path = TEST_ORG,
            username = USERNAME,
            password = PASSWORD
        )
        val infos = listOf(
            AuthenticationInfo(
                host = TEST_HOST,
                path = null,
                username = "",
                password = ""
            ),
            match,
            AuthenticationInfo(
                host = TEST_HOST,
                path = "/other-path",
                username = "",
                password = ""
            )
        )

        infos.findClosestMatch(testUrl(repo = "/another-repo")) shouldBe match
    }

    "A host with a port should be matched" {
        val match = AuthenticationInfo(
            host = "$TEST_HOST:443",
            path = TEST_PATH,
            username = USERNAME,
            password = PASSWORD
        )
        val infos = listOf(match)

        infos.findClosestMatch(testUrl(port = 443)) shouldBe match
    }

    "A host with a different port should not be matched" {
        val match = AuthenticationInfo(
            host = "$TEST_HOST:443",
            path = TEST_PATH,
            username = USERNAME,
            password = PASSWORD
        )
        val infos = listOf(match)

        infos.findClosestMatch(testUrl(port = 442)) should beNull()
    }

    "Query parameters should be stripped from the URL" {
        val match = AuthenticationInfo(
            host = TEST_HOST,
            path = TEST_PATH,
            username = USERNAME,
            password = PASSWORD
        )
        val infos = listOf(
            AuthenticationInfo(
                host = TEST_HOST,
                path = null,
                username = "",
                password = ""
            ),
            match,
            AuthenticationInfo(
                host = TEST_HOST,
                path = TEST_ORG,
                username = "",
                password = ""
            )
        )

        infos.findClosestMatch("${testUrl()}?foo=bar,x=y") shouldBe match
    }
})

private const val TEST_HOST = "test.example.com"
private const val TEST_ORG = "/test-org"
private const val TEST_REPO = "/test-repo.git"
private const val TEST_PATH = "$TEST_ORG$TEST_REPO"
private const val USERNAME = "correctUser"
private const val PASSWORD = "correctPassword"

/**
 * Generate a URL for testing based on the given components.
 */
private fun testUrl(
    host: String = TEST_HOST,
    org: String = TEST_ORG,
    repo: String = TEST_REPO,
    port: Int? = null
): String =
    "https://$host${if (port != null) ":$port" else ""}$org$repo"
