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

import kotlinx.serialization.Serializable

/**
 * The request for the `get` command of Bazel Credential Helpers, passed via stdin as JSON.
 *
 * See https://github.com/EngFlow/credential-helper-spec/blob/main/schemas/get-credentials-request.schema.json
 */
@Serializable
data class GetCredentialRequest(
    /** The URI to get credentials for. */
    val uri: String
)

/**
 * The response for the `get` command of Bazel Credential Helpers, written to stdout as JSON.
 *
 * See https://github.com/EngFlow/credential-helper-spec/blob/main/schemas/get-credentials-response.schema.json
 */
@Serializable
data class GetCredentialResponse(
    /**
     * The headers containing credentials which tools should add to all requests to the URI. Each header name maps
     * to a list of header values.
     */
    val headers: Map<String, List<String>> = emptyMap(),

    /**
     * The time the credentials expire and stop being valid for new requests, formatted following RFC 3339.
     */
    val expires: String? = null
)
