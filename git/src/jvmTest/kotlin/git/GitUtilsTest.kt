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
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class GitUtilsTest : WordSpec({
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
})
