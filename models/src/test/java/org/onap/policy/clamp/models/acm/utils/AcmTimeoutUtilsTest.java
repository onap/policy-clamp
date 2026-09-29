/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.SubState;

class AcmTimeoutUtilsTest {

    @Test
    void testGetTimeout() {
        var result = AcmTimeoutUtils.getTimeout(Map.of(), AcmTimeoutUtils.DEPLOY_TIMEOUT, 1000);
        assertEquals(1000, result);

        result = AcmTimeoutUtils.getTimeout(Map.of(AcmTimeoutUtils.DEPLOY_TIMEOUT, 20000),
                AcmTimeoutUtils.DEPLOY_TIMEOUT, 1000);
        assertEquals(20000, result);
    }

    @Test
    void testGetOpName() {
        var result = AcmTimeoutUtils.getOpName(AcTypeState.PRIMING);
        assertEquals(AcmTimeoutUtils.PRIME_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(AcTypeState.DEPRIMING);
        assertEquals(AcmTimeoutUtils.DEPRIME_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(DeployState.DEPLOYING, SubState.NONE);
        assertEquals(AcmTimeoutUtils.DEPLOY_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(DeployState.UNDEPLOYING, SubState.NONE);
        assertEquals(AcmTimeoutUtils.UNDEPLOY_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(DeployState.UPDATING, SubState.NONE);
        assertEquals(AcmTimeoutUtils.UPDATE_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(DeployState.DELETING, SubState.NONE);
        assertEquals(AcmTimeoutUtils.DELETE_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(DeployState.MIGRATING, SubState.NONE);
        assertEquals(AcmTimeoutUtils.MIGRATE_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(DeployState.DEPLOYED, SubState.NONE);
        assertEquals(AcmTimeoutUtils.DEFAULT_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(DeployState.UNDEPLOYED, SubState.PREPARING);
        assertEquals(AcmTimeoutUtils.PREPARE_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(DeployState.DEPLOYED, SubState.REVIEWING);
        assertEquals(AcmTimeoutUtils.REVIEW_TIMEOUT, result);
        result = AcmTimeoutUtils.getOpName(DeployState.DEPLOYED, SubState.MIGRATION_PRECHECKING);
        assertEquals(AcmTimeoutUtils.MIGRATION_PRECHECK_TIMEOUT, result);
    }
}
