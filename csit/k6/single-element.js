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
 * single-element.js
 *
 * Run against the compose stack:
 *   k6 run single-element.js
 *
 * Override hosts via env vars (mirrors Robot -v variables):
 *   k6 run single-element.js \
 *     -e POLICY_RUNTIME_ACM_IP=localhost:30007 \
 *     -e HTTP_PARTICIPANT_SIM1_IP=localhost:30011
 */

import { check, group } from 'k6';
import { functionalOptions }          from './lib/options.js';
import { clampAuth, participantAuth } from './lib/auth.js';
import { get, postYaml, putJson, del } from './lib/acm-client.js';
import {
    pollUntil,
    verifyPriming,
    verifyStateChangeResult,
    verifyDeployStatus,
    verifyInternalStateElementsRuntime,
    verifyUninstantiated,
    verifyCompositionParticipantSim,
} from './lib/checks.js';

export const options = functionalOptions;

// Hosts — match defaults from csit/resources/scripts/run-test.sh
const ACM_HOST  = __ENV.POLICY_RUNTIME_ACM_IP   || 'localhost:30007';
const SIM1_HOST = __ENV.HTTP_PARTICIPANT_SIM1_IP || 'localhost:30011';

// AC element UUID — matches ac-instance-simple.yaml
const ELEMENT_ID = '709c62b3-8918-41b9-a747-d21eb80c6c34';

// Data files — loaded at init time (k6 open() runs in init context)
const AC_DEFINITION = open('../resources/tests/data/ac-definition-simple.yaml');
const AC_INSTANCE   = open('../resources/tests/data/ac-instance-simple.yaml');
const AC_PRIMING    = open('../resources/tests/data/ACPriming.json');
const AC_DEPRIMING  = open('../resources/tests/data/ACDepriming.json');
const DEPLOY        = open('../resources/tests/data/DeployAC.json');
const UNDEPLOY      = open('../resources/tests/data/UndeployAC.json');
const SIM_FAIL      = open('../resources/tests/data/SettingSimPropertiesFail.json');
const SIM_SUCCESS   = open('../resources/tests/data/SettingSimPropertiesSuccess.json');
const SIM_DELAY     = open('../resources/tests/data/SettingSimPropertiesDelay.json');

export default function () {
    let compositionId;
    let instanceId;

    group('CommissionAutomationCompositionSimple', () => {
        const resp = postYaml(ACM_HOST, '/onap/policy/clamp/acm/v2/compositions', AC_DEFINITION, clampAuth());
        check(resp, {
            'commission: status is 201': r => r.status === 201,
        });
        compositionId = resp.json('compositionId');
        check(compositionId, {
            'commission: compositionId returned': id => id !== undefined && id !== null,
        });
    });

    group('PrimeACDefinitionsSimple', () => {
        const resp = putJson(
            ACM_HOST,
            `/onap/policy/clamp/acm/v2/compositions/${compositionId}`,
            AC_PRIMING,
            clampAuth()
        );
        check(resp, {
            'prime: status is 202': r => r.status === 202,
        });

        pollUntil(
            () => verifyPriming(ACM_HOST, compositionId, 'PRIMED'),
            120000,  // 2 min — matches Robot "2 min"
            5000     // 5 sec — matches Robot "5 sec"
        );
    });

    group('InstantiateAutomationCompositionSimple', () => {
        const instanceYaml = AC_INSTANCE.replace('COMPOSITIONIDPLACEHOLDER', compositionId);
        const resp = postYaml(
            ACM_HOST,
            `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances`,
            instanceYaml,
            clampAuth()
        );
        check(resp, {
            'instantiate: status is 201': r => r.status === 201,
        });
        instanceId = resp.json('instanceId');
        check(instanceId, {
            'instantiate: instanceId returned': id => id !== undefined && id !== null,
        });
    });

    group('FailDeployAutomationCompositionSimple', () => {
        const simResp = putJson(
            SIM1_HOST,
            '/onap/policy/simparticipant/v2/parameters',
            SIM_FAIL,
            participantAuth()
        );
        check(simResp, {
            'set sim fail: status is 200': r => r.status === 200,
        });

        const deployResp = putJson(
            ACM_HOST,
            `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
            DEPLOY,
            clampAuth()
        );
        check(deployResp, {
            'deploy: status is 202': r => r.status === 202,
        });

        pollUntil(
            () => verifyStateChangeResult(ACM_HOST, compositionId, instanceId, 'FAILED'),
            120000,  // 2 min
            5000     // 5 sec
        );

        verifyCompositionParticipantSim(SIM1_HOST, 'Sim_AutomationCompositionElement');
    });

    group('UnDeployAutomationCompositionSimple', () => {
        const simResp = putJson(
            SIM1_HOST,
            '/onap/policy/simparticipant/v2/parameters',
            SIM_DELAY,
            participantAuth()
        );
        check(simResp, {
            'set sim delay: status is 200': r => r.status === 200,
        });

        const undeployResp = putJson(
            ACM_HOST,
            `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
            UNDEPLOY,
            clampAuth()
        );
        check(undeployResp, {
            'undeploy: status is 202': r => r.status === 202,
        });

        pollUntil(
            () => verifyDeployStatus(ACM_HOST, compositionId, instanceId, 'UNDEPLOYING'),
            60000,   // 1 min
            5000
        );

        pollUntil(
            () => verifyInternalStateElementsRuntime(ACM_HOST, compositionId, instanceId, 'UNDEPLOYING', ELEMENT_ID),
            60000,   // 1 min
            5000
        );

        pollUntil(
            () => verifyDeployStatus(ACM_HOST, compositionId, instanceId, 'UNDEPLOYED'),
            180000,  // 3 min
            5000
        );

        verifyInternalStateElementsRuntime(ACM_HOST, compositionId, instanceId, 'UNDEPLOYED', ELEMENT_ID);

        verifyCompositionParticipantSim(SIM1_HOST, 'Sim_AutomationCompositionElement');
    });

    group('UnInstantiateAutomationCompositionSimple', () => {
        const simResp = putJson(
            SIM1_HOST,
            '/onap/policy/simparticipant/v2/parameters',
            SIM_SUCCESS,
            participantAuth()
        );
        check(simResp, {
            'set sim success: status is 200': r => r.status === 200,
        });

        const deleteResp = del(
            ACM_HOST,
            `/onap/policy/clamp/acm/v2/compositions/${compositionId}/instances/${instanceId}`,
            clampAuth()
        );
        check(deleteResp, {
            'delete instance: status is 202': r => r.status === 202,
        });

        pollUntil(
            () => verifyUninstantiated(ACM_HOST, compositionId),
            60000,   // 1 min
            5000
        );

        verifyCompositionParticipantSim(SIM1_HOST, 'Sim_AutomationCompositionElement');
    });

    group('DeleteACDefinitionSimple', () => {
        const deprimeResp = putJson(
            ACM_HOST,
            `/onap/policy/clamp/acm/v2/compositions/${compositionId}`,
            AC_DEPRIMING,
            clampAuth()
        );
        check(deprimeResp, {
            'deprime: status is 202': r => r.status === 202,
        });

        pollUntil(
            () => verifyPriming(ACM_HOST, compositionId, 'COMMISSIONED'),
            120000,  // 2 min
            5000
        );

        const deleteResp = del(
            ACM_HOST,
            `/onap/policy/clamp/acm/v2/compositions/${compositionId}`,
            clampAuth()
        );
        check(deleteResp, {
            'delete definition: status is 200': r => r.status === 200,
        });
    });
}
