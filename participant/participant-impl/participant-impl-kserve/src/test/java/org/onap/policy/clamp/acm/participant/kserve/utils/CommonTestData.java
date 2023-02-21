/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
 *  Modifications Copyright (C) 2022 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.kserve.utils;

import java.util.List;
import java.util.UUID;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class CommonTestData {

    private static final String TEST_KEY_NAME = "org.onap.domain.database.KserveAutomationCompositionElement";

    private static final List<UUID> AC_ID_LIST = List.of(UUID.randomUUID(), UUID.randomUUID());

    /**
     * Get a automationComposition Element.
     *
     * @return automationCompositionElement object
     */
    public AcElementDeploy getAutomationCompositionElement() {
        var element = new AcElementDeploy();
        element.setId(UUID.randomUUID());
        element.setDefinition(new ToscaConceptIdentifier(TEST_KEY_NAME, "1.0.1"));
        element.setOrderedState(DeployOrder.DEPLOY);
        return element;
    }

    /**
     * Get automation composition id.
     *
     * @return ToscaConceptIdentifier automationCompositionId
     */
    public UUID getAutomationCompositionId() {
        return AC_ID_LIST.get(0);
    }

}
