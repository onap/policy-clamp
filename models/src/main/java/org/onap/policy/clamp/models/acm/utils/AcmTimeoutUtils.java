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

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AcmTimeoutUtils {
    public static final String DEFAULT_TIMEOUT = "maxOperationWaitMs";
    public static final String PRIME_TIMEOUT = "primeTimeoutMs";
    public static final String DEPRIME_TIMEOUT = "deprimeTimeoutMs";
    public static final String DEPLOY_TIMEOUT = "deployTimeoutMs";
    public static final String UNDEPLOY_TIMEOUT = "undeployTimeoutMs";
    public static final String UPDATE_TIMEOUT = "updateTimeoutMs";
    public static final String MIGRATE_TIMEOUT = "migrateTimeoutMs";
    public static final String DELETE_TIMEOUT = "deleteTimeoutMs";

    public static final Map<DeployState, String> MAP_TIMEOUT = Map.of(DeployState.DEPLOYING, DEPLOY_TIMEOUT,
            DeployState.UNDEPLOYING, UNDEPLOY_TIMEOUT,
            DeployState.UPDATING, UPDATE_TIMEOUT,
            DeployState.MIGRATING, MIGRATE_TIMEOUT,
            DeployState.DELETING, DELETE_TIMEOUT);

    /**
     * Get timeout value from properties by name operation, return default value if not present.
     *
     * @param properties instance properties
     * @param name the operation name
     * @param defaultValue the default value
     * @return the timeout value
     */
    public static long getTimeout(Map<String, Object> properties, String name, long defaultValue) {
        var objTimeout = properties.get(name);
        if (objTimeout != null) {
            return Long.parseLong(objTimeout.toString());
        }
        return defaultValue;
    }

    /**
     * Get operation name of a composition definition.
     *
     * @param state the state of the composition definition
     * @return the operation name
     */
    public static String getOpName(AcTypeState state) {
        return AcTypeState.PRIMING.equals(state) ? PRIME_TIMEOUT : DEPRIME_TIMEOUT;
    }

    /**
     * Get operation name of a AutomationComposition.
     *
     * @param deployState the state of the AutomationComposition
     * @return the operation name
     */
    public static String getOpName(DeployState deployState) {
        return MAP_TIMEOUT.getOrDefault(deployState, DEFAULT_TIMEOUT);
    }
}
