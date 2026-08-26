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
 * health-check.js
 *
 * Run against the compose stack:
 *   k6 run health-check.js
 *
 * Override hosts via env vars (mirrors Robot -v variables):
 *   k6 run health-check.js \
 *     -e POLICY_RUNTIME_ACM_IP=localhost:30007 \
 *     -e POLICY_HTTP_PARTICIPANT=localhost:30009 \
 *     -e HTTP_PARTICIPANT_SIM1_IP=localhost:30011
 */

import { check, group } from 'k6';
import { functionalOptions }          from './lib/options.js';
import { clampAuth, participantAuth } from './lib/auth.js';
import { get, putJson }               from './lib/acm-client.js';
import { pollUntil, verifyParticipantsRegistered, assertParticipantsRegistered } from './lib/checks.js';

export const options = functionalOptions;

// Hosts — match defaults from csit/resources/scripts/run-test.sh
const ACM_HOST  = __ENV.POLICY_RUNTIME_ACM_IP   || 'localhost:30007';
const SIM1_HOST = __ENV.HTTP_PARTICIPANT_SIM1_IP || 'localhost:30011';
const HTTP_PPNT = __ENV.POLICY_HTTP_PARTICIPANT || null;

export default function () {
    group('HealthcheckAcm', () => {
        const resp = get(ACM_HOST, '/onap/policy/clamp/acm/actuator/health', clampAuth());
        check(resp, {
            'status is 200': r => r.status === 200,
        });
    });

    if (HTTP_PPNT) {
        group('HealthcheckParticipantHttp', () => {
            const resp = get(HTTP_PPNT, '/onap/policy/clamp/acm/httpparticipant/health', participantAuth());
            check(resp, {
                'status is 200': r => r.status === 200,
            });
        });
    }

    group('HealthcheckParticipantSim', () => {
        const resp = get(SIM1_HOST, '/onap/policy/simparticipant/health', participantAuth());
        check(resp, {
            'status is 200': r => r.status === 200,
        });
    });

    group('RegisterParticipants', () => {
        const resp = putJson(ACM_HOST, '/onap/policy/clamp/acm/v2/participants', null, clampAuth());
        check(resp, {
            'status is 202': r => r.status === 202,
        });

        pollUntil(
            () => verifyParticipantsRegistered(ACM_HOST),
            60000,   // 60 sec — k6 runs on host, participants need more time to register
            2000     // 2 sec
        );
        assertParticipantsRegistered(ACM_HOST);
    });
}
