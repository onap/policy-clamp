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
package org.onap.policy.clamp.acm.runtime.helper

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.concepts.DeployState
import org.onap.policy.clamp.models.acm.concepts.LockState
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult
import org.onap.policy.clamp.models.acm.utils.TimestampHelper

class PhaseScannerTestHelper {

    static final AC_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json"
    static final ELEMENT_NAME =
            "org.onap.domain.database.Http_PMSHMicroserviceAutomationCompositionElement"

    static buildAcDefinition() {
        def acDefinition = new AutomationCompositionDefinition()
        acDefinition.serviceTemplate = InstantiationUtils.getToscaServiceTemplate(
                CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
        return acDefinition
    }

    static buildAc(UUID compositionId, UUID instanceId, DeployState deployState,
                   LockState lockState) {
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_JSON, "Crud")
        ac.instanceId = instanceId
        ac.deployState = deployState
        ac.lockState = lockState
        ac.compositionId = compositionId
        return ac
    }

    static buildDeployingAc(UUID compositionId, UUID instanceId) {
        def ac = buildAc(compositionId, instanceId, DeployState.DEPLOYING, LockState.NONE)
        ac.phase = 0
        ac.elements.values().each { it.deployState = DeployState.DEPLOYING }
        ac.elements.values().iterator().next().deployState = DeployState.DEPLOYED
        return ac
    }

    static buildDeployingAcForStartPhase(UUID compositionId, UUID instanceId) {
        def ac = buildAc(compositionId, instanceId, DeployState.DEPLOYING, LockState.NONE)
        ac.phase = 0
        ac.elements.values().each { element ->
            if (ELEMENT_NAME == element.definition.name) {
                element.deployState = DeployState.DEPLOYING
                element.lockState = LockState.NONE
            } else {
                element.deployState = DeployState.DEPLOYED
                element.lockState = LockState.LOCKED
            }
        }
        return ac
    }

    static buildAcWithNullStartPhase(UUID compositionId, UUID instanceId) {
        def ac = buildAc(compositionId, instanceId, DeployState.DEPLOYING, LockState.NONE)
        ac.phase = 0
        ac.lastMsg = TimestampHelper.now()
        ac.elements.values().each { element ->
            if (ELEMENT_NAME == element.definition.name) {
                element.deployState = DeployState.DEPLOYING
                element.definition.name = "NotExistElement"
                element.lockState = LockState.NONE
            } else {
                element.deployState = DeployState.DEPLOYING
                element.definition.version = "0.0.0"
                element.lockState = LockState.NONE
            }
        }
        return ac
    }

    static buildUnlockingAc(UUID compositionId, UUID instanceId) {
        def ac = buildAc(compositionId, instanceId, DeployState.DEPLOYED, LockState.UNLOCKING)
        ac.phase = 0
        ac.elements.values().each { element ->
            if (ELEMENT_NAME == element.definition.name) {
                element.deployState = DeployState.DEPLOYED
                element.lockState = LockState.UNLOCKING
            } else {
                element.deployState = DeployState.DEPLOYED
                element.lockState = LockState.UNLOCKED
            }
        }
        return ac
    }
}
