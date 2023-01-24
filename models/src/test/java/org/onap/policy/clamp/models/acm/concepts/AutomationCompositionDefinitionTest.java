/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.concepts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AutomationCompositionDefinitionTest {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase.yaml";

    @Test
    void testCopyContructor() {
        var acDefinition = new AutomationCompositionDefinition();
        acDefinition.setCompositionId(UUID.randomUUID());
        var nodeTemplateState = new NodeTemplateState();
        nodeTemplateState.setNodeTemplateId(new ToscaConceptIdentifier());
        acDefinition.setElementStateMap(Map.of("key", nodeTemplateState));
        acDefinition.setServiceTemplate(CommonTestData.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML));
        acDefinition.setState(AcTypeState.COMMISSIONED);
        var result = new AutomationCompositionDefinition(acDefinition);
        assertEquals(result, acDefinition);
    }
}
