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
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.concepts.DeployState
import org.onap.policy.clamp.models.acm.concepts.LockState
import org.onap.policy.clamp.models.acm.concepts.MigrationState
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult
import org.onap.policy.clamp.models.acm.utils.TimestampHelper

class SupervisionScannerTestHelper {

    static final AC_SMOKE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionSmoke.json"

    static buildAcDefinition(UUID compositionId, AcTypeState state) {
        def serviceTemplate = InstantiationUtils.getToscaServiceTemplate(
                CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
        serviceTemplate.metadata = [compositionId: compositionId.toString()]
        def acDefinition = new AutomationCompositionDefinition()
        acDefinition.state = state
        acDefinition.stateChangeResult = StateChangeResult.NO_ERROR
        acDefinition.compositionId = compositionId
        acDefinition.lastMsg = TimestampHelper.now()
        acDefinition.serviceTemplate = serviceTemplate
        def node = new NodeTemplateState()
        node.state = AcTypeState.PRIMING
        node.nodeTemplateStateId = UUID.randomUUID()
        acDefinition.elementStateMap = [(node.nodeTemplateStateId.toString()): node]
        return acDefinition
    }

    static buildMigratingAcFromResource(UUID compositionId, UUID instanceId,
                                         UUID compositionTargetId) {
        def ac = InstantiationUtils.getAutomationCompositionFromResource(
                AC_SMOKE_JSON, "Crud")
        ac.deployState = DeployState.MIGRATING
        ac.instanceId = instanceId
        ac.compositionId = compositionId
        ac.compositionTargetId = compositionTargetId
        ac.lockState = LockState.LOCKED
        ac.lastMsg = TimestampHelper.now()
        ac.phase = 0
        ac.elements.values().each {
            it.deployState = DeployState.DEPLOYED
            it.lockState = LockState.LOCKED
        }
        return ac
    }

    static buildMigratingAcWithMigrationStates(UUID compositionId, UUID instanceId,
                                                UUID compositionTargetId) {
        def ac = buildMigratingAcFromResource(compositionId, instanceId, compositionTargetId)
        def elementIds = new ArrayList<>(ac.elements.keySet())
        def states = [MigrationState.REMOVED, MigrationState.NEW, MigrationState.DEFAULT]

        elementIds.eachWithIndex { id, i ->
            ac.elements[id].migrationState = states[i]
        }

        ac.elements.each { id, element ->
            if (MigrationState.REMOVED == element.migrationState) {
                element.deployState = DeployState.DELETED
                element.lockState = LockState.LOCKED
            } else {
                element.deployState = DeployState.DEPLOYED
                element.lockState = LockState.LOCKED
            }
        }
        return ac
    }
}
