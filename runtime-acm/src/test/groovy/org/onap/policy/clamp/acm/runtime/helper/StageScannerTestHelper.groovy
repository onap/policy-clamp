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
import org.onap.policy.clamp.models.acm.concepts.SubState
import org.onap.policy.clamp.models.acm.utils.TimestampHelper

class StageScannerTestHelper {

    static final AC_SMOKE_JSON = "src/test/resources/rest/acm/AutomationCompositionSmoke.json"

    static buildMigratingAc(UUID compositionId, UUID compositionTargetId) {
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_SMOKE_JSON, "Crud")
        ac.compositionId = compositionId
        ac.compositionTargetId = compositionTargetId
        CommonTestData.modifyAcState(ac, DeployState.MIGRATING)
        def element = ac.elements.values().iterator().next()
        element.deployState = DeployState.MIGRATING
        return ac
    }

    static buildMigrationRevertingAc(UUID compositionId, UUID compositionTargetId) {
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_SMOKE_JSON, "Crud")
        ac.compositionId = compositionId
        ac.compositionTargetId = compositionTargetId
        CommonTestData.modifyAcState(ac, DeployState.MIGRATION_REVERTING)
        def element = ac.elements.values().iterator().next()
        element.deployState = DeployState.MIGRATION_REVERTING
        return ac
    }

    static buildPreparingAc(UUID compositionId) {
        def ac = InstantiationUtils.getAutomationCompositionFromResource(AC_SMOKE_JSON, "Crud")
        ac.deployState = DeployState.UNDEPLOYED
        ac.subState = SubState.PREPARING
        ac.lockState = LockState.NONE
        ac.compositionId = compositionId
        ac.lastMsg = TimestampHelper.now()
        ac.phase = 0
        ac.elements.values().each {
            it.deployState = DeployState.UNDEPLOYED
            it.lockState = LockState.NONE
            it.subState = SubState.NONE
        }
        def element = ac.elements.values().iterator().next()
        element.subState = SubState.PREPARING
        return ac
    }

    static buildAcDefinition() {
        def serviceTemplate = InstantiationUtils.getToscaServiceTemplate(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
        def acDefinition = new AutomationCompositionDefinition()
        acDefinition.serviceTemplate = serviceTemplate
        return acDefinition
    }
}
