/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_ELEMENT_NAME
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML

import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition
import org.onap.policy.clamp.models.acm.utils.AcmUtils

class SupervisionMessagesTestHelper {

    static final AC_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionUpdate.json"

    static buildAcmDefinition(AcTypeState state = AcTypeState.PRIMED) {
        def serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML)
        def acmDefinition = new AutomationCompositionDefinition(
                compositionId: UUID.randomUUID(),
                state: state,
                serviceTemplate: serviceTemplate)
        def acElements = AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate, TOSCA_ELEMENT_NAME)
        acmDefinition.elementStateMap = AcmUtils.createElementStateMap(acElements, state)
        acmDefinition.elementStateMap.values().each { it.participantId = UUID.randomUUID() }
        return acmDefinition
    }

    static loadAcFromResource() {
        return InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Crud")
    }
}
