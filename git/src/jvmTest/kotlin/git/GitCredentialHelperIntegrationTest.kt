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

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.MountableFile

/** The username configured in the Git HTTP server. */
private const val GIT_USERNAME = "testuser"

/** The password configured in the Git HTTP server. */
private const val GIT_PASSWORD = "testpassword"

/** The path where the credential helper binary is placed inside the container. */
private const val CREDENTIAL_HELPER_PATH = "/usr/local/bin/credentialhelper"

/** The URL of the test repository inside the container. */
private const val REPO_URL = "http://localhost/git/test-repo.git"

/**
 * An integration test to verify that the GIT credentials helper can actually collaborate with Git to clone a
 * remote repository.
 *
 * The test sets up a test container with nginx simulating a protected remote Git repository. (This is needed, since
 * Git only queries credentials for remote repositories.) In this container, the Git CLI is configured to use the
 * credentials helper binary. The test then tries to clone the repository and verifies the success status.
 */
class GitCredentialHelperIntegrationTest : WordSpec({
    val gitServer = GenericContainer(
        ImageFromDockerfile()
            .withFileFromClasspath("Dockerfile", "Dockerfile")
            .withFileFromClasspath("nginx.conf", "nginx.conf")
            .withFileFromClasspath("entrypoint.sh", "entrypoint.sh")
    ).withExposedPorts(80)
        .waitingFor(Wait.forListeningPort())

    beforeSpec {
        val binaryPath = requireNotNull(System.getProperty("credentialHelperBinary")) {
            "System property 'credentialHelperBinary' is not set. " +
                    "Make sure the 'linkReleaseExecutableLinuxX64' task ran before the tests."
        }

        gitServer.withCopyFileToContainer(
            MountableFile.forHostPath(binaryPath, 0b111_101_101 /* rwxr-xr-x */),
            CREDENTIAL_HELPER_PATH
        )

        gitServer.start()
    }

    afterSpec {
        gitServer.stop()
    }

    "git clone" should {
        "succeed when the credential helper provides correct credentials" {
            val result = gitServer.execInContainer(
                "sh", "-c",
                """
                mkdir -p /test-home
                printf 'http://$GIT_USERNAME:$GIT_PASSWORD@localhost/git/test-repo.git\n' > /test-home/.git-credentials
                git config --file /test-home/.gitconfig credential.helper $CREDENTIAL_HELPER_PATH
                git config --file /test-home/.gitconfig credential.useHttpPath true
                HOME=/test-home GIT_CONFIG_NOSYSTEM=1 \
                    git clone $REPO_URL /tmp/clone-success 2>&1
                """.trimIndent()
            )

            result.exitCode shouldBe 0
        }

        "select the correct credentials for the host" {
            val result = gitServer.execInContainer(
                "sh", "-c",
                """
                mkdir -p /test-home-path-match
                printf 'http://$GIT_USERNAME:wrong_pwd1@localhost/\n' > /test-home-path-match/.git-credentials
                printf 'http://$GIT_USERNAME:wrong_pwd2@localhost/git/\n' >> /test-home-path-match/.git-credentials
                printf 'http://$GIT_USERNAME:$GIT_PASSWORD@localhost/git/test-repo.git\n' >> /test-home-path-match/.git-credentials
                git config --file /test-home-path-match/.gitconfig credential.helper $CREDENTIAL_HELPER_PATH
                git config --file /test-home-path-match/.gitconfig credential.useHttpPath true
                HOME=/test-home-path-match GIT_CONFIG_NOSYSTEM=1 \
                    git clone $REPO_URL /tmp/clone-path-match 2>&1
                """.trimIndent()
            )

            withClue("stdout:\n${result.stdout}") {
                result.exitCode shouldBe 0
            }
        }

        "fail when no credentials are provided" {
            val result = gitServer.execInContainer(
                "sh", "-c",
                """
                mkdir -p /test-home-no-creds
                git config --file /test-home-no-creds/.gitconfig credential.helper ''
                HOME=/test-home-no-creds GIT_CONFIG_NOSYSTEM=1 GIT_TERMINAL_PROMPT=0 \
                    git clone $REPO_URL /tmp/clone-no-creds 2>&1
                """.trimIndent()
            )

            result.exitCode shouldNotBe 0
        }

        "fail when wrong credentials are provided" {
            val result = gitServer.execInContainer(
                "sh", "-c",
                """
                mkdir -p /test-home-wrong-creds
                printf 'http://$GIT_USERNAME:wrongpassword@localhost/git/test-repo.git\n' \
                    > /test-home-wrong-creds/.git-credentials
                git config --file /test-home-wrong-creds/.gitconfig \
                    credential.helper $CREDENTIAL_HELPER_PATH
                git config --file /test-home-wrong-creds/.gitconfig credential.useHttpPath true
                HOME=/test-home-wrong-creds GIT_CONFIG_NOSYSTEM=1 GIT_TERMINAL_PROMPT=0 \
                    git clone $REPO_URL /tmp/clone-wrong-creds 2>&1
                """.trimIndent()
            )

            result.exitCode shouldNotBe 0
            result.stdout shouldContain "Authentication failed"
        }
    }
})
