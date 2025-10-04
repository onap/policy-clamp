/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementState;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class CacheProviderTest {

    @Test
    void testGetSupportedAcElementTypes() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        assertEquals(parameter.getIntermediaryParameters().getParticipantId(), cacheProvider.getParticipantId());
        assertEquals(parameter.getIntermediaryParameters().getParticipantSupportedElementTypes().get(0),
                cacheProvider.getSupportedAcElementTypes().get(0));
    }

    @Test
    void testInitializeAutomationCompositionNotNull() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var instanceId = UUID.randomUUID();
        var participantDeploy = new ParticipantDeploy();

        assertThatThrownBy(() -> cacheProvider
                .initializeAutomationComposition(null, instanceId, participantDeploy, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider
                .initializeAutomationComposition(instanceId, null, participantDeploy, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider
                .initializeAutomationComposition(instanceId, instanceId, null, null))
                .isInstanceOf(NullPointerException.class);

        var deployState = DeployState.DEPLOYED;
        var subState = SubState.NONE;

        assertThatThrownBy(() -> cacheProvider.initializeAutomationComposition(null, instanceId, null,
                participantDeploy, deployState, subState, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider.initializeAutomationComposition(instanceId, null, null,
                participantDeploy, deployState, subState, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testNotNull() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var instanceId = UUID.randomUUID();

        assertThatThrownBy(() -> cacheProvider.addElementDefinition(null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider.addElementDefinition(instanceId, null, null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.getAutomationComposition(null)).isInstanceOf(NullPointerException.class);

        var definition = new ToscaConceptIdentifier();
        assertThatThrownBy(() -> cacheProvider.getCommonProperties(null, definition))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider.getCommonProperties(instanceId, (ToscaConceptIdentifier) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider.getCommonProperties(instanceId, (UUID) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cacheProvider.getCommonProperties(null, instanceId))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.removeAutomationComposition(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.removeElementDefinition(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testInitCommonProperties() {
        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setInstanceId(UUID.randomUUID());
        var compositionId = UUID.randomUUID();
        automationComposition.setCompositionId(compositionId);
        var definitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        var cacheProvider = new CacheProvider(CommonTestData.getParticipantParameters());
        cacheProvider.addElementDefinition(compositionId, definitions, UUID.randomUUID());

        var participantDeploy =
                CommonTestData.createparticipantDeploy(cacheProvider.getParticipantId(), automationComposition);
        cacheProvider.initializeAutomationComposition(compositionId, automationComposition.getInstanceId(),
                participantDeploy, UUID.randomUUID());

        for (var element : automationComposition.getElements().values()) {
            var commonProperties =
                    cacheProvider.getCommonProperties(automationComposition.getInstanceId(), element.getId());
            assertEquals("value", commonProperties.get("key"));

            commonProperties = cacheProvider
                    .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
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
    void testDeploy() {
        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setInstanceId(UUID.randomUUID());
        var compositionId = UUID.randomUUID();
        automationComposition.setCompositionId(compositionId);
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);

        var participantDeploy =
                CommonTestData.createparticipantDeploy(cacheProvider.getParticipantId(), automationComposition);
        cacheProvider.initializeAutomationComposition(compositionId, automationComposition.getInstanceId(),
                participantDeploy, UUID.randomUUID());

        var ac = cacheProvider.getAutomationComposition(automationComposition.getInstanceId());
        for (var element : ac.getElements().values()) {
            element.setOperationalState("OperationalState");
            element.setUseState("UseState");
            element.setOutProperties(Map.of("key", "value"));
        }

        // deploy again
        cacheProvider.initializeAutomationComposition(compositionId, automationComposition.getInstanceId(),
                participantDeploy, UUID.randomUUID());

        // check UseState, OperationalState and OutProperties have not changed
        ac = cacheProvider.getAutomationComposition(automationComposition.getInstanceId());
        for (var element : ac.getElements().values()) {
            assertEquals("OperationalState", element.getOperationalState());
            assertEquals("UseState", element.getUseState());
            assertEquals("value", element.getOutProperties().get("key"));
        }
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
        for (var acElementRestart : participantRestartAc.getAcElementList()) {
            var element = result.getElements().get(acElementRestart.getId());
            assertEquals(element.getOperationalState(), acElementRestart.getOperationalState());
            assertEquals(element.getUseState(), acElementRestart.getUseState());
            assertEquals(element.getLockState(), acElementRestart.getLockState());
            assertEquals(element.getDeployState(), acElementRestart.getDeployState());
            assertEquals(element.getProperties(), acElementRestart.getProperties());
            assertEquals(element.getOutProperties(), acElementRestart.getOutProperties());
        }
    }

    @Test
    void testCreateCompositionElementDto() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var compositionId = UUID.randomUUID();
        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setCompositionId(compositionId);
        cacheProvider.addElementDefinition(compositionId,
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition),
                UUID.randomUUID());
        for (var element : automationComposition.getElements().values()) {
            var result = cacheProvider.createCompositionElementDto(compositionId, element);
            assertEquals(compositionId, result.compositionId());
            assertEquals(element.getDefinition(), result.elementDefinitionId());
        }
    }

    @Test
    void testGetCompositionElementDtoMap() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var compositionId = UUID.randomUUID();
        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setCompositionId(compositionId);
        cacheProvider.addElementDefinition(compositionId,
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition),
                UUID.randomUUID());
        var result = cacheProvider.getCompositionElementDtoMap(automationComposition);
        for (var element : automationComposition.getElements().values()) {
            var compositionElementDto = result.get(element.getId());
            assertEquals(element.getDefinition(), compositionElementDto.elementDefinitionId());
            assertEquals(ElementState.PRESENT, result.get(element.getId()).state());
        }
        var element = automationComposition.getElements().values().iterator().next();
        element.setDefinition(new ToscaConceptIdentifier("NotExist", "0.0.0"));
        result = cacheProvider.getCompositionElementDtoMap(automationComposition);
        assertEquals(ElementState.NOT_PRESENT, result.get(element.getId()).state());
    }

    @Test
    void testGetInstanceElementDtoMap() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var compositionId = UUID.randomUUID();
        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setCompositionId(compositionId);
        var result = cacheProvider.getInstanceElementDtoMap(automationComposition);
        for (var element : automationComposition.getElements().values()) {
            var compositionElementDto = result.get(element.getId());
            assertEquals(element.getId(), compositionElementDto.elementId());
        }
    }

    @Test
    void testIsCompositionDefinitionUpdated() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var compositionId = UUID.randomUUID();
        assertTrue(cacheProvider.isCompositionDefinitionUpdated(compositionId, null));

        var revisionId = UUID.randomUUID();
        assertFalse(cacheProvider.isCompositionDefinitionUpdated(compositionId, revisionId));

        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setCompositionId(compositionId);
        cacheProvider.addElementDefinition(compositionId,
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition),
                revisionId);
        assertTrue(cacheProvider.isCompositionDefinitionUpdated(compositionId, revisionId));

        revisionId = UUID.randomUUID();
        assertFalse(cacheProvider.isCompositionDefinitionUpdated(compositionId, revisionId));
    }

    @Test
    void testIsInstanceUpdated() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var instanceId = UUID.randomUUID();
        assertTrue(cacheProvider.isInstanceUpdated(instanceId, null));
        var revisionId = UUID.randomUUID();
        assertFalse(cacheProvider.isInstanceUpdated(instanceId, revisionId));

        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setInstanceId(instanceId);

        var participantDeploy =
                CommonTestData.createparticipantDeploy(cacheProvider.getParticipantId(), automationComposition);
        cacheProvider.initializeAutomationComposition(UUID.randomUUID(), automationComposition.getInstanceId(),
                participantDeploy, revisionId);
        assertTrue(cacheProvider.isInstanceUpdated(instanceId, revisionId));

        revisionId = UUID.randomUUID();
        assertFalse(cacheProvider.isInstanceUpdated(instanceId, revisionId));
    }
}
