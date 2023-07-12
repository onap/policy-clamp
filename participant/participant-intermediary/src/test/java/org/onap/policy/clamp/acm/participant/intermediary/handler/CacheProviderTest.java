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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;

class CacheProviderTest {

    @Test
    void testgetSupportedAcElementTypes() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        assertEquals(parameter.getIntermediaryParameters().getParticipantId(), cacheProvider.getParticipantId());
        assertEquals(parameter.getIntermediaryParameters().getParticipantSupportedElementTypes().get(0),
                cacheProvider.getSupportedAcElementTypes().get(0));
    }

    @Test
    void testNotNull() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var instanceId = UUID.randomUUID();
        assertThatThrownBy(() -> cacheProvider.initializeAutomationComposition(null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider.initializeAutomationComposition(instanceId, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider.initializeAutomationComposition(instanceId, instanceId, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.addElementDefinition(null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider.addElementDefinition(instanceId, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.getAutomationComposition(null)).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.getCommonProperties(null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider.getCommonProperties(instanceId, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.removeAutomationComposition(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.removeElementDefinition(null)).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.initializeAutomationComposition(null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testinitCommonProperties() throws AutomationCompositionException {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(cacheProvider.getParticipantId());

        var compositionId = UUID.randomUUID();

        List<AutomationCompositionElementDefinition> definitions = new ArrayList<>();
        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setCompositionId(compositionId);
        for (var element : automationComposition.getElements().values()) {
            var acElementDefinition = new AutomationCompositionElementDefinition();
            acElementDefinition.setAcElementDefinitionId(element.getDefinition());
            var nodeTemplate = new ToscaNodeTemplate();
            nodeTemplate.setProperties(Map.of("key", "value"));
            acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(nodeTemplate);
            definitions.add(acElementDefinition);

            var acElement = new AcElementDeploy();
            acElement.setId(element.getId());
            acElement.setDefinition(element.getDefinition());
            participantDeploy.getAcElementList().add(acElement);
        }
        cacheProvider.addElementDefinition(compositionId, definitions);

        cacheProvider.initializeAutomationComposition(compositionId, automationComposition.getInstanceId(),
                participantDeploy);

        for (var element : automationComposition.getElements().values()) {
            var commonProperties =
                    cacheProvider.getCommonProperties(automationComposition.getInstanceId(), element.getId());
            assertEquals("value", commonProperties.get("key"));
        }

        assertEquals(automationComposition.getInstanceId(),
                cacheProvider.getAutomationComposition(automationComposition.getInstanceId()).getInstanceId());

        assertThat(cacheProvider.getAutomationCompositions()).hasSize(1);
        cacheProvider.removeAutomationComposition(automationComposition.getInstanceId());
        assertThat(cacheProvider.getAutomationCompositions()).isEmpty();

        cacheProvider.removeElementDefinition(compositionId);
        assertThat(cacheProvider.getAcElementsDefinitions()).isEmpty();
    }

    @Test
    void testInitializeAutomationComposition() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);

        var participantRestartAc = CommonTestData.createParticipantRestartAc();
        var compositionId = UUID.randomUUID();
        cacheProvider.initializeAutomationComposition(compositionId, participantRestartAc);
        var result = cacheProvider.getAutomationComposition(participantRestartAc.getAutomationCompositionId());
        assertEquals(compositionId, result.getCompositionId());
        assertEquals(participantRestartAc.getAutomationCompositionId(), result.getInstanceId());
    }
}
