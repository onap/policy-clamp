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
 * checks.js — reusable assertion helpers and polling utility.
 */

import { check, sleep } from 'k6';
import { get, queryPrometheus } from './acm-client.js';
import { clampAuth, participantAuth } from './auth.js';

// ---------------------------------------------------------------------------
// Polling utility
// ---------------------------------------------------------------------------

/**
 * Repeatedly calls fn() until it returns true or the timeout is reached.
 * Throws an error if the condition is never met, which fails the k6 iteration.
 *
 * @param {Function} fn           zero-argument function that returns boolean
 * @param {number}   timeoutMs    total wait budget in milliseconds (default 2 min)
 * @param {number}   intervalMs   sleep between attempts in milliseconds (default 5 s)
 */
export function pollUntil(fn, timeoutMs = 120000, intervalMs = 5000) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        if (fn()) return;
        sleep(intervalMs / 1000);
    }
    throw new Error(`pollUntil: condition not met within ${timeoutMs}ms`);
}

// ---------------------------------------------------------------------------
// ACM runtime — composition checks
// ---------------------------------------------------------------------------

/**
 * Asserts that at least expectedCount participants are registered.
 * @param {string} acmHost
 * @param {number} [expectedCount]  defaults to EXPECTED_PARTICIPANT_COUNT env var or 4
 * @returns {boolean}
 */
export function verifyParticipantsRegistered(acmHost, expectedCount) {
    const count = expectedCount || parseInt(__ENV.EXPECTED_PARTICIPANT_COUNT || '4', 10);
    const resp = get(acmHost, '/onap/policy/clamp/acm/v2/participants', clampAuth());
    if (resp.status !== 200) return false;
    return JSON.parse(resp.body).length === count;
}

export function assertParticipantsRegistered(acmHost, expectedCount) {
    const count = expectedCount || parseInt(__ENV.EXPECTED_PARTICIPANT_COUNT || '4', 10);
    const resp = get(acmHost, '/onap/policy/clamp/acm/v2/participants', clampAuth());
    return check(resp, {
        'participants: status 200':          r => r.status === 200,
        [`participants: count is ${count}`]: r => JSON.parse(r.body).length === count,
    });
}

/**
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} expectedState  e.g. "PRIMED" or "COMMISSIONED"
 * @returns {boolean}
 */
export function verifyPriming(acmHost, compositionId, expectedState) {
    const resp = get(acmHost, `/onap/policy/clamp/acm/v2/compositions/${compositionId}`, clampAuth());
    return check(resp, {
        [`priming: status 200`]:                    r => r.status === 200,
        [`priming: stateChangeResult is NO_ERROR`]: r => JSON.parse(r.body).stateChangeResult === 'NO_ERROR',
        [`priming: state is ${expectedState}`]:     r => JSON.parse(r.body).state === expectedState,
    });
}

/**
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} expectedResult  e.g. "NO_ERROR" or "FAILED"
 * @returns {boolean}
 */
export function verifyStateChangeResultPriming(acmHost, compositionId, expectedResult) {
    const resp = get(acmHost, `/onap/policy/clamp/acm/v2/compositions/${compositionId}`, clampAuth());
    return check(resp, {
        'priming stateChangeResult: status 200':                       r => r.status === 200,
        [`priming stateChangeResult: result is ${expectedResult}`]:    r => JSON.parse(r.body).stateChangeResult === expectedResult,
    });
}

// ---------------------------------------------------------------------------
// ACM runtime — instance checks
// ---------------------------------------------------------------------------

/**
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} instanceId
 * @param {string} expectedDeployState  e.g. "DEPLOYED", "UNDEPLOYED", "UNDEPLOYING"
 * @returns {boolean}
 */
export function verifyDeployStatus(acmHost, compositionId, instanceId, expectedDeployState) {
    const resp = get(
        acmHost,
        `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
        clampAuth()
    );
    return check(resp, {
        'deployStatus: status 200':                              r => r.status === 200,
        'deployStatus: stateChangeResult is NO_ERROR':          r => JSON.parse(r.body).stateChangeResult === 'NO_ERROR',
        [`deployStatus: deployState is ${expectedDeployState}`]: r => JSON.parse(r.body).deployState === expectedDeployState,
    });
}

/**
 * Asserts subState is "NONE".
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} instanceId
 * @returns {boolean}
 */
export function verifySubStatus(acmHost, compositionId, instanceId) {
    const resp = get(
        acmHost,
        `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
        clampAuth()
    );
    return check(resp, {
        'subStatus: status 200':                r => r.status === 200,
        'subStatus: stateChangeResult NO_ERROR': r => JSON.parse(r.body).stateChangeResult === 'NO_ERROR',
        'subStatus: subState is NONE':          r => JSON.parse(r.body).subState === 'NONE',
    });
}

/**
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} instanceId
 * @param {string} expectedResult  e.g. "FAILED", "NO_ERROR"
 * @returns {boolean}
 */
export function verifyStateChangeResult(acmHost, compositionId, instanceId, expectedResult) {
    const resp = get(
        acmHost,
        `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
        clampAuth()
    );
    return check(resp, {
        'stateChangeResult: status 200':                        r => r.status === 200,
        [`stateChangeResult: result is ${expectedResult}`]:     r => JSON.parse(r.body).stateChangeResult === expectedResult,
    });
}

/**
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} instanceId
 * @param {string} textToFind  substring to search for in the response body
 * @returns {boolean}
 */
export function verifyPropertiesUpdated(acmHost, compositionId, instanceId, textToFind) {
    const resp = get(
        acmHost,
        `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
        clampAuth()
    );
    return check(resp, {
        'propertiesUpdated: status 200':          r => r.status === 200,
        [`propertiesUpdated: contains ${textToFind}`]: r => r.body.includes(textToFind),
    });
}

/**
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} instanceId
 * @param {string} expectedDeployState
 * @param {string} elementId  UUID of the AC element
 * @returns {boolean}
 */
export function verifyInternalStateElementsRuntime(acmHost, compositionId, instanceId, expectedDeployState, elementId) {
    const resp = get(
        acmHost,
        `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
        clampAuth()
    );
    return check(resp, {
        'internalState: status 200':                                    r => r.status === 200,
        [`internalState: deployState is ${expectedDeployState}`]:       r => JSON.parse(r.body).deployState === expectedDeployState,
        [`internalState: element InternalState is ${expectedDeployState}`]: r => {
            const body = JSON.parse(r.body);
            return body.elements?.[elementId]?.outProperties?.InternalState === expectedDeployState;
        },
    });
}

/**
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} instanceId
 * @returns {boolean}
 */
export function verifyMigratedElementsRuntime(acmHost, compositionId, instanceId) {
    const resp = get(
        acmHost,
        `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
        clampAuth()
    );
    return check(resp, {
        'migratedElements: status 200':                         r => r.status === 200,
        'migratedElements: contains Sim_NewAutomationCompositionElement':  r => r.body.includes('Sim_NewAutomationCompositionElement'),
        'migratedElements: contains Sim_NewAutomationCompositionElement2': r => r.body.includes('Sim_NewAutomationCompositionElement2'),
        'migratedElements: no Sim_SinkAutomationCompositionElement':       r => !r.body.includes('Sim_SinkAutomationCompositionElement'),
        'migratedElements: element 34 stage [1,2]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c34']?.outProperties?.stage) === '[1,2]';
        },
        'migratedElements: element 35 stage [0,1]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c35']?.outProperties?.stage) === '[0,1]';
        },
        'migratedElements: element 37 stage [0,2]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c37']?.outProperties?.stage) === '[0,2]';
        },
        'migratedElements: element 40 stage [1,2]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c40']?.outProperties?.stage) === '[1,2]';
        },
    });
}

/**
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} instanceId
 * @returns {boolean}
 */
export function verifyPrepareElementsRuntime(acmHost, compositionId, instanceId) {
    const resp = get(
        acmHost,
        `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
        clampAuth()
    );
    return check(resp, {
        'prepareElements: status 200':              r => r.status === 200,
        'prepareElements: element 34 prepareStage [1,2]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c34']?.outProperties?.prepareStage) === '[1,2]';
        },
        'prepareElements: element 35 prepareStage [0,1]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c35']?.outProperties?.prepareStage) === '[0,1]';
        },
        'prepareElements: element 36 prepareStage [0,2]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c36']?.outProperties?.prepareStage) === '[0,2]';
        },
    });
}

/**
 * @param {string} acmHost
 * @param {string} compositionId
 * @param {string} instanceId
 * @returns {boolean}
 */
export function verifyRollbackElementsRuntime(acmHost, compositionId, instanceId) {
    const resp = get(
        acmHost,
        `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
        clampAuth()
    );
    return check(resp, {
        'rollbackElements: status 200':                         r => r.status === 200,
        'rollbackElements: no Sim_NewAutomationCompositionElement': r => !r.body.includes('Sim_NewAutomationCompositionElement'),
        'rollbackElements: contains Sim_SinkAutomationCompositionElement': r => r.body.includes('Sim_SinkAutomationCompositionElement'),
        'rollbackElements: element 34 rollbackStage [2,1]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c34']?.outProperties?.rollbackStage) === '[2,1]';
        },
        'rollbackElements: element 35 rollbackStage [1,0]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c35']?.outProperties?.rollbackStage) === '[1,0]';
        },
        'rollbackElements: element 36 rollbackStage [2,0]': r => {
            const body = JSON.parse(r.body);
            return JSON.stringify(body.elements?.['709c62b3-8918-41b9-a747-d21eb79c6c36']?.outProperties?.rollbackStage) === '[2,0]';
        },
    });
}

/**
 * Asserts the composition has zero instances.
 * @param {string} acmHost
 * @param {string} compositionId
 * @returns {boolean}
 */
export function verifyUninstantiated(acmHost, compositionId) {
    const resp = get(
        acmHost,
        `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances`,
        clampAuth()
    );
    return check(resp, {
        'uninstantiated: status 200':       r => r.status === 200,
        'uninstantiated: instance list empty': r => JSON.parse(r.body).automationCompositionList?.length === 0,
    });
}

// ---------------------------------------------------------------------------
// Participant simulator checks
// ---------------------------------------------------------------------------

/**
 * @param {string} simHost
 * @param {string} instanceId
 * @returns {boolean}
 */
export function verifyMigratedElementsSim(simHost, instanceId) {
    const resp = get(simHost, `/onap/policy/simparticipant/v2/instances/${instanceId}`, participantAuth());
    return check(resp, {
        'migratedSim: status 200':                                      r => r.status === 200,
        'migratedSim: contains Sim_NewAutomationCompositionElement':    r => r.body.includes('Sim_NewAutomationCompositionElement'),
        'migratedSim: no Sim_SinkAutomationCompositionElement':         r => !r.body.includes('Sim_SinkAutomationCompositionElement'),
    });
}

/**
 * @param {string} simHost
 * @param {string} instanceId
 * @returns {boolean}
 */
export function verifyMigratedElementsSim3(simHost, instanceId) {
    const resp = get(simHost, `/onap/policy/simparticipant/v2/instances/${instanceId}`, participantAuth());
    return check(resp, {
        'migratedSim3: status 200':                                      r => r.status === 200,
        'migratedSim3: contains Sim_NewAutomationCompositionElement2':   r => r.body.includes('Sim_NewAutomationCompositionElement2'),
        'migratedSim3: no Sim_SinkAutomationCompositionElement':         r => !r.body.includes('Sim_SinkAutomationCompositionElement'),
    });
}

/**
 * @param {string} simHost
 * @param {string} instanceId
 * @returns {boolean}
 */
export function verifyRemovedElementsSim(simHost, instanceId) {
    const resp = get(simHost, `/onap/policy/simparticipant/v2/instances/${instanceId}`, participantAuth());
    return check(resp, {
        'removedSim: status 200':    r => r.status === 200,
        'removedSim: body is empty': r => r.body === '' || r.body === null,
    });
}

/**
 * @param {string} simHost
 * @param {string} instanceId
 * @returns {boolean}
 */
export function verifyRollbackElementsSim(simHost, instanceId) {
    const resp = get(simHost, `/onap/policy/simparticipant/v2/instances/${instanceId}`, participantAuth());
    return check(resp, {
        'rollbackSim: status 200':                                      r => r.status === 200,
        'rollbackSim: no Sim_NewAutomationCompositionElement':          r => !r.body.includes('Sim_NewAutomationCompositionElement'),
        'rollbackSim: contains Sim_SinkAutomationCompositionElement':   r => r.body.includes('Sim_SinkAutomationCompositionElement'),
    });
}

/**
 * @param {string} simHost
 * @param {string} textToFind
 * @returns {boolean}
 */
export function verifyCompositionParticipantSim(simHost, textToFind) {
    const resp = get(simHost, '/onap/policy/simparticipant/v2/compositiondatas', participantAuth());
    return check(resp, {
        'compositionSim: status 200':                   r => r.status === 200,
        [`compositionSim: contains ${textToFind}`]:     r => r.body.includes(textToFind),
    });
}

/**
 * @param {string} simHost
 * @param {string} instanceId
 * @param {string} textToFind
 * @returns {boolean}
 */
export function verifyParticipantSim(simHost, instanceId, textToFind) {
    const resp = get(simHost, `/onap/policy/simparticipant/v2/instances/${instanceId}`, participantAuth());
    return check(resp, {
        'participantSim: status 200':               r => r.status === 200,
        [`participantSim: contains ${textToFind}`]: r => r.body.includes(textToFind),
    });
}

// ---------------------------------------------------------------------------
// Observability checks
// ---------------------------------------------------------------------------

/**
 * @param {string} jaegerHost  e.g. "jaeger:16686"
 * @param {string} service     Jaeger service name
 * @returns {boolean}
 */
export function verifyTracingWorks(jaegerHost, service) {
    const resp = get(jaegerHost, '/api/traces', '', { service });
    return check(resp, {
        'tracing: status 200': r => r.status === 200,
    });
}

/**
 * @param {string} jaegerHost
 * @param {string} service
 * @returns {boolean}
 */
export function verifyKafkaInTraces(jaegerHost, service) {
    const tags = JSON.stringify({
        'messaging.system': 'kafka',
        'messaging.destination.name': 'policy-acruntime-participant',
    });
    const resp = get(jaegerHost, '/api/traces', '', {
        service,
        tags,
        lookback: '1h',
        limit: 10,
    });
    return check(resp, {
        'kafkaTraces: status 200':          r => r.status === 200,
        'kafkaTraces: has trace data':      r => JSON.parse(r.body).data?.length > 0,
    });
}

/**
 * @param {string} jaegerHost
 * @param {string} service
 * @returns {boolean}
 */
export function verifyHttpInTraces(jaegerHost, service) {
    const tags = JSON.stringify({ 'uri': '/v2/compositions/{compositionId}' });
    const resp = get(jaegerHost, '/api/traces', '', {
        service,
        tags,
        operation: 'http put /v2/compositions/{compositionId}',
        lookback: '1h',
        limit: 10,
    });
    return check(resp, {
        'httpTraces: status 200':       r => r.status === 200,
        'httpTraces: has trace data':   r => JSON.parse(r.body).data?.length > 0,
    });
}

/**
 * Queries Prometheus and asserts the average response time for a URI is under the limit.
 * @param {string} prometheusHost
 * @param {string} job        Prometheus job label
 * @param {string} uri        URI label value
 * @param {string} method     HTTP method label value
 * @param {number} timeLimitMs  maximum allowed average response time in milliseconds
 * @returns {boolean}
 */
export function validateResponseTime(prometheusHost, job, uri, method, timeLimitMs) {
    const query = `http_server_requests_seconds_sum{uri="${uri}",method="${method}",job="${job}"}/http_server_requests_seconds_count{uri="${uri}",method="${method}",job="${job}"}`;
    const result = queryPrometheus(prometheusHost, query);
    const rawSeconds = parseFloat(result?.data?.result?.[0]?.value?.[1] ?? '0');
    const actualMs = rawSeconds * 1000;
    return check({ actualMs }, {
        [`responseTime: ${method} ${uri} under ${timeLimitMs}ms`]: d => d.actualMs <= timeLimitMs,
    });
}
