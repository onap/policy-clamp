/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.a1pms.utils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.a1pms.models.A1PolicyServiceEntity;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class CommonTestData {

    private static final String TEST_KEY_NAME = "org.onap.domain.database.A1PMSAutomationCompositionElement";
    private static final List<UUID> AC_ID_LIST = List.of(UUID.randomUUID(), UUID.randomUUID());

    /**
     * Get a new InstanceElement.
     *
     * @return InstanceElementDto object
     */
    public InstanceElementDto getAutomationCompositionElement() {
        return new InstanceElementDto(
                getAutomationCompositionId(), UUID.randomUUID(), null, Map.of(), Map.of());
    }

    /**
     * Get a new CompositionElement.
     *
     * @param properties common properties from service template
     * @return CompositionElementDto object
     */
    public CompositionElementDto getCompositionElement(Map<String, Object> properties) {
        return new CompositionElementDto(UUID.randomUUID(),
                new ToscaConceptIdentifier(TEST_KEY_NAME, "1.0.1"),
                properties, Map.of());
    }

    /**
     * Get automation composition id.
     *
     * @param instanceNo Identifier instance no
     * @return ToscaConceptIdentifier automationCompositionId
     */
    public ToscaConceptIdentifier getA1PolicyServiceId(int instanceNo) {
        return new ToscaConceptIdentifier("A1PMSInstance" + instanceNo, "1.0.0");
    }

    /**
     * Get automation composition id.
     *
     * @return UUID automationCompositionId
     */
    public UUID getAutomationCompositionId() {
        return getAutomationCompositionId(0);
    }

    /**
     * Get automation composition id.
     *
     * @param instanceNo Identifier instance no
     * @return UUID automationCompositionId
     */
    public UUID getAutomationCompositionId(int instanceNo) {
        return AC_ID_LIST.get(instanceNo);
    }

    /**
     * Get valid policy entities.
     *
     * @return List of policy entities
     */
    public List<A1PolicyServiceEntity> getValidPolicyEntities() {
        var a1PolicyServiceEntity1 = new A1PolicyServiceEntity(getA1PolicyServiceId(0),
                "testService1", "http://localhost", 0);
        var a1PolicyServiceEntity2 = new A1PolicyServiceEntity(getA1PolicyServiceId(1),
                "testService2", "http://127.0.0.1", 0);
        return List.of(a1PolicyServiceEntity1, a1PolicyServiceEntity2);
    }
}
