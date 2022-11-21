/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.a1pms.utils;

import java.util.List;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.a1pms.models.A1PolicyServiceEntity;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class CommonTestData {

    private static final String TEST_KEY_NAME = "org.onap.domain.database.A1PMSAutomationCompositionElement";

    /**
     * Get a automationComposition Element.
     *
     * @return automationCompositionElement object
     */
    public AutomationCompositionElement getAutomationCompositionElement() {
        AutomationCompositionElement element = new AutomationCompositionElement();
        element.setId(UUID.randomUUID());
        element.setDefinition(new ToscaConceptIdentifier(TEST_KEY_NAME, "1.0.1"));
        element.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
        return element;
    }

    /**
     * Get automation composition id.
     *
     * @return ToscaConceptIdentifier automationCompositionId
     */
    public ToscaConceptIdentifier getAutomationCompositionId() {
        return getAutomationCompositionId(0);
    }

    /**
     * Get automation composition id.
     * @param instanceNo Identifier instance no
     * @return ToscaConceptIdentifier automationCompositionId
     */
    public ToscaConceptIdentifier getAutomationCompositionId(int instanceNo) {
        return new ToscaConceptIdentifier("A1PMSInstance" + instanceNo, "1.0.0");
    }


    /**
     * Get valid policy entities.
     * @return List of policy entities
     */
    public List<A1PolicyServiceEntity> getValidPolicyEntities() {
        A1PolicyServiceEntity a1PolicyServiceEntity1 = new A1PolicyServiceEntity(getAutomationCompositionId(0),
                "testService1", "http://localhost", 0);
        A1PolicyServiceEntity a1PolicyServiceEntity2 = new A1PolicyServiceEntity(getAutomationCompositionId(1),
                "testService2", "http://127.0.0.1", 0);
        return List.of(a1PolicyServiceEntity1, a1PolicyServiceEntity2);
    }
}
