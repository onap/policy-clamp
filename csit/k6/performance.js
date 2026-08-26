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
 * performance.js
 *
 * SLA / staging performance test using performanceOptions (ramp-up VUs).
 * Exercises read-only ACM endpoints that are safe to call concurrently:
 *   - actuator health
 *   - participant list
 *   - composition list
 *
 * Run against the compose stack:
 *   k6 run performance.js
 *
 * Override hosts via env vars:
 *   k6 run performance.js \
 *     -e POLICY_RUNTIME_ACM_IP=localhost:30007 \
 *     -e HTTP_PARTICIPANT_SIM1_IP=localhost:30011
 */

import { check, group } from 'k6';
import { performanceOptions }        from './lib/options.js';
import { clampAuth, participantAuth } from './lib/auth.js';
import { get }                        from './lib/acm-client.js';

export const options = performanceOptions;

const ACM_HOST  = __ENV.POLICY_RUNTIME_ACM_IP   || 'localhost:30007';
const SIM1_HOST = __ENV.HTTP_PARTICIPANT_SIM1_IP || 'localhost:30011';

export default function () {
    group('HealthcheckAcm', () => {
        const resp = get(ACM_HOST, '/onap/policy/clamp/acm/actuator/health', clampAuth());
        check(resp, {
            'GET /actuator/health → 200': r => r.status === 200,
        });
    });

    group('HealthcheckParticipantSim', () => {
        const resp = get(SIM1_HOST, '/onap/policy/simparticipant/health', participantAuth());
        check(resp, {
            'GET /simparticipant/health → 200': r => r.status === 200,
        });
    });

    group('GetParticipants', () => {
        const resp = get(ACM_HOST, '/onap/policy/clamp/acm/v2/participants', clampAuth());
        check(resp, {
            'GET /v2/participants → 200':      r => r.status === 200,
            'GET /v2/participants → non-empty': r => JSON.parse(r.body).length > 0,
        });
    });

    group('GetCompositions', () => {
        const resp = get(ACM_HOST, '/onap/policy/clamp/acm/v2/compositions', clampAuth());
        check(resp, {
            'GET /v2/compositions → 200': r => r.status === 200,
        });
    });
}
