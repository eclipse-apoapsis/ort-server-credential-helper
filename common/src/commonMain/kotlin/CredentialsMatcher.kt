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

/**
 * Find the closest matching [AuthenticationInfo] for the given [url] from this list.
 * The matching is based on the host and path of the URL. In case of multiple matches, the one with the longest
 * matching path is returned. If no match is found, return *null*.
 */
fun List<AuthenticationInfo>.findClosestMatch(url: String): AuthenticationInfo? {
    val match = URL_REGEX.matchEntire(url) ?: return null
    val path = match.groups["path"]?.value

    val credentialsForHost = filter { it.host == match.groups["host"]?.value }
    return credentialsForHost.find { it.path?.removePrefix("/") == path }
        ?: credentialsForHost.filter {
            it.path != null && path.orEmpty().startsWith(it.path.removePrefix("/") + "/")
        }.maxByOrNull { it.path?.length ?: 0 }
        ?: credentialsForHost.find { it.path == null }
}
