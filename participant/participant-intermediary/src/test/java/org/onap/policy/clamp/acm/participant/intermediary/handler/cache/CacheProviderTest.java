/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantDtoUtils;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementRestart;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.dto.CompositionDto;
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
    void testValidationOfSupportedAcElementTypes() {
        var parameter = CommonTestData.getParticipantParameters();
        var supportedElementType = new ParticipantSupportedElementType();
        supportedElementType.setTypeName("name.*");
        supportedElementType.setTypeVersion("1.0.*");
        parameter.getIntermediaryParameters().getParticipantSupportedElementTypes().add(supportedElementType);
        assertThatThrownBy(() -> new CacheProvider(parameter)).isInstanceOf(IllegalArgumentException.class);
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

        assertThatThrownBy(() -> cacheProvider.removeAutomationComposition(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> cacheProvider.removeElementDefinition(null)).isInstanceOf(NullPointerException.class);
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

    @Test
    void test_addElementDefinition() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var compositionId = UUID.randomUUID();
        var revisionId = UUID.randomUUID();

        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(new ToscaConceptIdentifier("name", "1.0.0"));
        var list = new ArrayList<AutomationCompositionElementDefinition>();
        list.add(acElementDefinition);

        cacheProvider.addElementDefinition(compositionId, list, revisionId);
        assertEquals(1, cacheProvider.getAcElementsDefinitions().size());
        var acDefinition = cacheProvider.getAcElementsDefinitions().get(compositionId);
        assertNotNull(acDefinition);
        assertEquals(compositionId, acDefinition.getCompositionId());
        assertEquals(revisionId, acDefinition.getRevisionId());

        var element = acDefinition.getElements().get(acElementDefinition.getAcElementDefinitionId());
        assertEquals(acElementDefinition, element);
        assertNotNull(element.getAutomationCompositionElementToscaNodeTemplate());
        assertNotNull(element.getAutomationCompositionElementToscaNodeTemplate().getProperties());
    }

    @Test
    void test_addElementDefinitionFromCompositionDto() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var compositionId = UUID.randomUUID();
        var revisionId = UUID.randomUUID();
        var elementId = new ToscaConceptIdentifier("name", "1.0.0");

        var inProperties = Map.of(elementId, Map.<String, Object>of("startPhase", 0));
        var outProperties = Map.of(elementId, Map.<String, Object>of("key", "value"));
        var compositionDto = new CompositionDto(compositionId, inProperties, outProperties);

        cacheProvider.addElementDefinition(compositionDto, revisionId);
        assertEquals(1, cacheProvider.getAcElementsDefinitions().size());
        var acDefinition = cacheProvider.getAcElementsDefinitions().get(compositionId);
        assertNotNull(acDefinition);
        assertEquals(compositionId, acDefinition.getCompositionId());
        assertEquals(revisionId, acDefinition.getRevisionId());

        var element = acDefinition.getElements().get(elementId);
        assertNotNull(element);
        assertEquals(elementId, element.getAcElementDefinitionId());
        assertNotNull(element.getAutomationCompositionElementToscaNodeTemplate());
        assertEquals(0, element.getAutomationCompositionElementToscaNodeTemplate().getProperties().get("startPhase"));
        assertEquals("value", element.getOutProperties().get("key"));
    }

    @Test
    void test_initializeAutomationComposition_NullValue() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        assertThatThrownBy(() -> cacheProvider.initializeAutomationComposition(null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void test_initializeAutomationComposition_ParticipantsDontMatch() {
        var element = new AcElementRestart();
        element.setId(UUID.randomUUID());
        element.setParticipantId(UUID.randomUUID());
        var participantRestartAc = CommonTestData.createParticipantRestartAc();
        participantRestartAc.setAcElementList(new ArrayList<>());
        participantRestartAc.getAcElementList().add(element);

        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        assertTrue(cacheProvider.getAutomationCompositions().isEmpty());
        var compositionId = UUID.randomUUID();
        cacheProvider.initializeAutomationComposition(compositionId, participantRestartAc);
        assertFalse(cacheProvider.getAutomationCompositions().isEmpty());
    }

    @Test
    void test_createAcInstance_NullValues() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);

        var randomID = UUID.randomUUID();
        var participantDeploy = new ParticipantDeploy();
        assertThrows(NullPointerException.class, () ->
            cacheProvider.createAcInstance(null, randomID, randomID, participantDeploy,
                DeployState.UNDEPLOYED, SubState.NONE, randomID));
        assertThrows(NullPointerException.class, () ->
            cacheProvider.createAcInstance(randomID, randomID, null, participantDeploy,
                DeployState.UNDEPLOYED, SubState.NONE, randomID));
    }

    @Test
    void test_createAcInstance_NotNullCompositionId() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var automationComposition = CommonTestData.getTestAutomationCompositions()
            .getAutomationCompositionList().get(0);
        var participantDeploy = CommonTestData.createparticipantDeploy(cacheProvider
            .getParticipantId(), automationComposition);

        var compositionId = UUID.randomUUID();
        var compositionTargetId = UUID.randomUUID();
        AtomicReference<UUID> instanceId = new AtomicReference<>(UUID.randomUUID());
        Optional.ofNullable(automationComposition.getInstanceId()).ifPresent(instanceId::set);

        var acInstance = cacheProvider.createAcInstance(compositionId, compositionTargetId, instanceId.get(),
            participantDeploy, DeployState.UNDEPLOYED, SubState.NONE, UUID.randomUUID());
        assertNotNull(acInstance);
        assertEquals(instanceId.get(), acInstance.getInstanceId());
        assertEquals(compositionId, acInstance.getCompositionId());
    }

    @Test
    void test_createAcInstanceFromDtoMap() {
        var parameter = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameter);
        var compositionId = UUID.randomUUID();
        var compositionTargetId = UUID.randomUUID();
        var automationComposition = CommonTestData.getTestAutomationCompositions()
                .getAutomationCompositionList().get(0);
        var instanceId = automationComposition.getInstanceId() != null
                ? automationComposition.getInstanceId() : UUID.randomUUID();

        var participantDtoList = CommonTestData.createParticipantDtoList(
                cacheProvider.getParticipantId(), automationComposition);
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                participantDtoList, cacheProvider.getParticipantId());

        var acInstance = cacheProvider.createAcInstance(compositionId, compositionTargetId, instanceId,
                elementDtoMap, DeployState.DEPLOYING, SubState.NONE, UUID.randomUUID());
        assertNotNull(acInstance);
        assertEquals(instanceId, acInstance.getInstanceId());
        assertEquals(compositionId, acInstance.getCompositionId());
        assertEquals(compositionTargetId, acInstance.getCompositionTargetId());
        assertFalse(acInstance.getElements().isEmpty());
    }
}
