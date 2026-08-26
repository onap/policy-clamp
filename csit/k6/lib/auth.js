/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

/**
 * auth.js — credential helpers
 *
 * Returns a base64-encoded Basic Auth header value that can be passed
 * directly into k6 http params.headers.
 */

import encoding from 'k6/encoding';

/**
 * Builds a Basic Auth header string from username and password.
 * @param {string} user
 * @param {string} password
 * @returns {string}  e.g. "Basic cmFuZG9tVXNlcjpwYXNz"
 */
function basicAuth(user, password) {
    return `Basic ${encoding.b64encode(`${user}:${password}`)}`;
}

/**
 * Credentials for the ACM runtime service.
 * Source: compose.yaml RUNTIME_USER / RUNTIME_PASSWORD env vars.
 */
export function clampAuth() {
    const user     = __ENV.RUNTIME_USER     || 'runtimeUser';
    const password = __ENV.RUNTIME_PASSWORD || 'zb!XztG34';
    return basicAuth(user, password);
}

/**
 * Credentials for participant services (HTTP participant, sim participants).
 * Source: compose.yaml HTTP_USER / HTTP_PASSWORD env vars.
 */
export function participantAuth() {
    const user     = __ENV.HTTP_USER     || 'participantUser';
    const password = __ENV.HTTP_PASSWORD || 'zb!XztG34';
    return basicAuth(user, password);
}
