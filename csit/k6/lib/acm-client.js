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
 * acm-client.js — low-level HTTP helpers for ACM runtime and participants.
 *
 * All functions return the raw k6 Response object so callers can run
 * check() and extract body fields as needed.
 */

import http from 'k6/http';

const JSON_HEADERS = {
    'Accept':       'application/json',
    'Content-Type': 'application/json',
};

const YAML_HEADERS = {
    'Accept':       'application/yaml',
    'Content-Type': 'application/yaml',
};

/**
 * Builds the base URL from a host string.
 * Accepts either "host:port" or a full "http://host:port" string.
 * @param {string} host
 * @returns {string}
 */
function baseUrl(host) {
    return host.startsWith('http') ? host : `http://${host}`;
}

/**
 * Shared params builder — injects the Authorization header.
 * @param {string} authHeader  value returned by clampAuth() / participantAuth()
 * @param {Object} headers     additional headers (JSON_HEADERS or YAML_HEADERS)
 * @returns {Object}           k6 Params object
 */
function params(authHeader, headers) {
    return {
        headers: { Authorization: authHeader, ...headers },
    };
}

// ---------------------------------------------------------------------------
// GET
// ---------------------------------------------------------------------------

/**
 * @param {string} host        e.g. "policy-clamp-runtime-acm:6969"
 * @param {string} path        e.g. "/onap/policy/clamp/acm/v2/compositions"
 * @param {string} authHeader  from clampAuth() or participantAuth()
 * @param {Object} [queryParams]  optional query-string key/value pairs
 * @returns {Response}
 */
export function get(host, path, authHeader, queryParams = {}) {
    return http.get(
        `${baseUrl(host)}${path}`,
        { ...params(authHeader, JSON_HEADERS), params: queryParams }
    );
}

// ---------------------------------------------------------------------------
// POST
// ---------------------------------------------------------------------------

/**
 * @param {string} host
 * @param {string} path
 * @param {string|Object} body  string or object (will be JSON.stringify'd)
 * @param {string} authHeader
 * @returns {Response}
 */
export function postJson(host, path, body, authHeader) {
    const payload = typeof body === 'string' ? body : JSON.stringify(body);
    return http.post(`${baseUrl(host)}${path}`, payload, params(authHeader, JSON_HEADERS));
}

/**
 * @param {string} host
 * @param {string} path
 * @param {string} yamlBody  raw YAML string (loaded via open())
 * @param {string} authHeader
 * @returns {Response}
 */
export function postYaml(host, path, yamlBody, authHeader) {
    return http.post(`${baseUrl(host)}${path}`, yamlBody, params(authHeader, YAML_HEADERS));
}

/**
 * @param {string} host
 * @param {string} path
 * @param {string} authHeader
 * @returns {Response}
 */
export function post(host, path, authHeader) {
    return http.post(`${baseUrl(host)}${path}`, null, params(authHeader, JSON_HEADERS));
}

// ---------------------------------------------------------------------------
// PUT
// ---------------------------------------------------------------------------

/**
 * @param {string} host
 * @param {string} path
 * @param {string|Object} body
 * @param {string} authHeader
 * @returns {Response}
 */
export function putJson(host, path, body, authHeader) {
    const payload = typeof body === 'string' ? body : JSON.stringify(body);
    return http.put(`${baseUrl(host)}${path}`, payload, params(authHeader, JSON_HEADERS));
}

/**
 * @param {string} host
 * @param {string} path
 * @param {string} yamlBody
 * @param {string} authHeader
 * @returns {Response}
 */
export function putYaml(host, path, yamlBody, authHeader) {
    return http.put(`${baseUrl(host)}${path}`, yamlBody, params(authHeader, YAML_HEADERS));
}

// ---------------------------------------------------------------------------
// PATCH
// ---------------------------------------------------------------------------

/**
 * @param {string} host
 * @param {string} path
 * @param {string|Object} body
 * @param {string} authHeader
 * @param {Object} [queryParams]
 * @returns {Response}
 */
export function patch(host, path, body, authHeader, queryParams = {}) {
    const payload = typeof body === 'string' ? body : JSON.stringify(body);
    return http.patch(
        `${baseUrl(host)}${path}`,
        payload,
        { ...params(authHeader, JSON_HEADERS), params: queryParams }
    );
}

// ---------------------------------------------------------------------------
// DELETE
// ---------------------------------------------------------------------------

/**
 * @param {string} host
 * @param {string} path
 * @param {string} authHeader
 * @returns {Response}
 */
export function del(host, path, authHeader) {
    return http.del(`${baseUrl(host)}${path}`, null, params(authHeader, JSON_HEADERS));
}

// ---------------------------------------------------------------------------
// Observability helpers
// ---------------------------------------------------------------------------

/**
 * @param {string} host
 * @param {string} authHeader
 * @param {string} [contextPath]  defaults to "/onap/policy/clamp/acm/"
 * @returns {Response}
 */
export function getMetrics(host, authHeader, contextPath = '/onap/policy/clamp/acm/') {
    return http.get(
        `${baseUrl(host)}${contextPath}metrics`,
        params(authHeader, JSON_HEADERS)
    );
}

/**
 * @param {string} prometheusHost  e.g. "prometheus:9090"
 * @param {string} query           PromQL expression
 * @returns {Object}               parsed JSON body
 */
export function queryPrometheus(prometheusHost, query) {
    const resp = http.get(
        `${baseUrl(prometheusHost)}/api/v1/query`,
        { params: { query } }
    );
    return JSON.parse(resp.body);
}
