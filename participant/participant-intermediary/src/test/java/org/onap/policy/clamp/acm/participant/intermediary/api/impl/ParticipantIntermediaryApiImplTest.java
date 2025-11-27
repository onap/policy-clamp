/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementState;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.AutomationCompositionOutHandler;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.AcDefinition;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class ParticipantIntermediaryApiImplTest {

    private static final String USE_STATE = "useState";
    private static final String OPERATIONAL_STATE = "operationState";
    private static final Map<String, Object> MAP = Map.of("key", 1);
    private static final UUID AUTOMATION_COMPOSITION_ID = UUID.randomUUID();
    private static final UUID ELEMENT_ID = UUID.randomUUID();
    private static final UUID COMPOSITION_ID = UUID.randomUUID();
    private static final ToscaConceptIdentifier DEFINITION_ELEMENT_ID = new ToscaConceptIdentifier("code", "0.0.1");
    private static final ToscaConceptIdentifier WRONG_DEF_ELEMENT_ID = new ToscaConceptIdentifier("wrong", "0.0.1");

    @Test
    void testUpdateAutomationCompositionElementState() {
        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, mock(CacheProvider.class));
        apiImpl.updateAutomationCompositionElementState(AUTOMATION_COMPOSITION_ID, ELEMENT_ID, DeployState.UNDEPLOYED,
                LockState.NONE, StateChangeResult.NO_ERROR, null);
        verify(automationCompositionHandler).updateAutomationCompositionElementState(AUTOMATION_COMPOSITION_ID,
                ELEMENT_ID, DeployState.UNDEPLOYED, LockState.NONE, StateChangeResult.NO_ERROR, null);
    }

    @Test
    void testUpdateCompositionState() {
        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, mock(CacheProvider.class));
        apiImpl.updateCompositionState(COMPOSITION_ID, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "");
        verify(automationCompositionHandler).updateCompositionState(COMPOSITION_ID, AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "");
    }

    @Test
    void testSendAcElementInfo() {
        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, mock(CacheProvider.class));
        apiImpl.sendAcElementInfo(AUTOMATION_COMPOSITION_ID, ELEMENT_ID, USE_STATE, OPERATIONAL_STATE, MAP);
        verify(automationCompositionHandler).sendAcElementInfo(AUTOMATION_COMPOSITION_ID, ELEMENT_ID, USE_STATE,
                OPERATIONAL_STATE, MAP);
    }

    @Test
    void testSendAcDefinitionInfo() {
        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, mock(CacheProvider.class));
        apiImpl.sendAcDefinitionInfo(COMPOSITION_ID, DEFINITION_ELEMENT_ID, MAP);
        verify(automationCompositionHandler).sendAcDefinitionInfo(COMPOSITION_ID, DEFINITION_ELEMENT_ID, MAP);
    }

    @Test
    void testGetAutomationCompositionElement() {
        var automationComposition = new AutomationComposition();
        automationComposition.setInstanceId(AUTOMATION_COMPOSITION_ID);
        var map = Map.of(AUTOMATION_COMPOSITION_ID, automationComposition);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationCompositions()).thenReturn(map);
        var acElement = new AutomationCompositionElement();
        acElement.setId(ELEMENT_ID);
        automationComposition.setElements(Map.of(ELEMENT_ID, acElement));

        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, cacheProvider);
        var mapResult = apiImpl.getAutomationCompositions();
        assertEquals(map, mapResult);

        var result = apiImpl.getAutomationComposition(UUID.randomUUID());
        assertThat(result).isNull();

        result = apiImpl.getAutomationComposition(AUTOMATION_COMPOSITION_ID);
        assertEquals(automationComposition, result);

        var element = apiImpl.getAutomationCompositionElement(UUID.randomUUID(), UUID.randomUUID());
        assertThat(element).isNull();

        element = apiImpl.getAutomationCompositionElement(AUTOMATION_COMPOSITION_ID, UUID.randomUUID());
        assertThat(element).isNull();

        element = apiImpl.getAutomationCompositionElement(AUTOMATION_COMPOSITION_ID, ELEMENT_ID);
        assertEquals(acElement, element);
    }

    @Test
    void testGetAcElementsDefinitions() {
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(DEFINITION_ELEMENT_ID);
        acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(new ToscaNodeTemplate());
        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(COMPOSITION_ID);
        acDefinition.getElements().put(DEFINITION_ELEMENT_ID, acElementDefinition);
        var map = Map.of(COMPOSITION_ID, acDefinition);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAcElementsDefinitions()).thenReturn(map);
        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, cacheProvider);
        var mapResult = apiImpl.getAcElementsDefinitions();
        assertThat(map).hasSameSizeAs(mapResult);
        assertThat(mapResult.get(COMPOSITION_ID)).isNotEmpty();
        assertEquals(mapResult.get(COMPOSITION_ID), acDefinition.getElements());

        var result = apiImpl.getAcElementsDefinitions(UUID.randomUUID());
        assertThat(result).isEmpty();

        result = apiImpl.getAcElementsDefinitions(COMPOSITION_ID);
        assertEquals(acDefinition.getElements(), result);

        var element = apiImpl.getAcElementDefinition(UUID.randomUUID(), WRONG_DEF_ELEMENT_ID);
        assertThat(element).isNull();

        element = apiImpl.getAcElementDefinition(COMPOSITION_ID, WRONG_DEF_ELEMENT_ID);
        assertThat(element).isNull();

        element = apiImpl.getAcElementDefinition(COMPOSITION_ID, DEFINITION_ELEMENT_ID);
        assertEquals(acElementDefinition, element);
    }

    @Test
    void testInstanceElementDto() {
        // test InstanceElementDto with toscaServiceTemplateFragment
        var instanceElementDto = new InstanceElementDto(COMPOSITION_ID, ELEMENT_ID, new ToscaServiceTemplate(),
                Map.of(), Map.of());
        assertEquals(COMPOSITION_ID, instanceElementDto.instanceId());
        assertEquals(ELEMENT_ID, instanceElementDto.elementId());
    }

    @Test
    void testGetInstanceElementDto() {
        var automationComposition = new AutomationComposition();
        automationComposition.setInstanceId(AUTOMATION_COMPOSITION_ID);
        var map = Map.of(AUTOMATION_COMPOSITION_ID, automationComposition);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationCompositions()).thenReturn(map);
        var acElement = new AutomationCompositionElement();
        acElement.setId(ELEMENT_ID);
        acElement.setProperties(MAP);
        acElement.setOutProperties(MAP);
        automationComposition.setElements(Map.of(ELEMENT_ID, acElement));

        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, cacheProvider);
        var mapResult = apiImpl.getAutomationCompositions();
        assertEquals(map, mapResult);

        var rndInstance = UUID.randomUUID();
        var rndElementId = UUID.randomUUID();
        var element = apiImpl.getInstanceElementDto(rndInstance, rndElementId);
        assertThat(element).isNotNull();
        assertEquals(rndInstance, element.instanceId());
        assertEquals(rndElementId, element.elementId());
        assertEquals(ElementState.NOT_PRESENT, element.state());

        element = apiImpl.getInstanceElementDto(AUTOMATION_COMPOSITION_ID, rndElementId);
        assertThat(element).isNotNull();
        assertEquals(AUTOMATION_COMPOSITION_ID, element.instanceId());
        assertEquals(rndElementId, element.elementId());
        assertEquals(ElementState.NOT_PRESENT, element.state());

        element = apiImpl.getInstanceElementDto(AUTOMATION_COMPOSITION_ID, ELEMENT_ID);
        assertThat(element).isNotNull();
        assertEquals(AUTOMATION_COMPOSITION_ID, element.instanceId());
        assertEquals(ELEMENT_ID, element.elementId());
        assertEquals(acElement.getProperties(), element.inProperties());
        assertEquals(acElement.getOutProperties(), element.outProperties());
        assertEquals(ElementState.PRESENT, element.state());
    }

    @Test
    void testGetCompositionElementDto() {
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(DEFINITION_ELEMENT_ID);
        acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(new ToscaNodeTemplate());
        acElementDefinition.getAutomationCompositionElementToscaNodeTemplate().setProperties(MAP);
        acElementDefinition.setOutProperties(MAP);
        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(COMPOSITION_ID);
        acDefinition.getElements().put(DEFINITION_ELEMENT_ID, acElementDefinition);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAcElementsDefinitions()).thenReturn(Map.of(COMPOSITION_ID, acDefinition));
        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, cacheProvider);

        var rndCompositionId = UUID.randomUUID();
        var element = apiImpl.getCompositionElementDto(rndCompositionId, WRONG_DEF_ELEMENT_ID);
        assertThat(element).isNotNull();
        assertEquals(rndCompositionId, element.compositionId());
        assertEquals(WRONG_DEF_ELEMENT_ID, element.elementDefinitionId());
        assertEquals(ElementState.NOT_PRESENT, element.state());

        element = apiImpl.getCompositionElementDto(COMPOSITION_ID, WRONG_DEF_ELEMENT_ID);
        assertThat(element).isNotNull();
        assertEquals(COMPOSITION_ID, element.compositionId());
        assertEquals(WRONG_DEF_ELEMENT_ID, element.elementDefinitionId());
        assertEquals(ElementState.NOT_PRESENT, element.state());

        element = apiImpl.getCompositionElementDto(COMPOSITION_ID, DEFINITION_ELEMENT_ID);
        assertThat(element).isNotNull();
        assertEquals(COMPOSITION_ID, element.compositionId());
        assertEquals(DEFINITION_ELEMENT_ID, element.elementDefinitionId());
        assertEquals(acElementDefinition.getAutomationCompositionElementToscaNodeTemplate().getProperties(),
                element.inProperties());
        assertEquals(acElementDefinition.getOutProperties(), element.outProperties());
        assertEquals(ElementState.PRESENT, element.state());
    }

    @Test
    void testGetMigrateNextStage() {
        var cacheProvider = mock(CacheProvider.class);
        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, cacheProvider);
        var migrate = Map.of("migrate", List.of(0, 2));
        Map<String, Object> stageSet = Map.of("stage", migrate);
        var compositionElementTarget = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                stageSet, Map.of());
        var result = apiImpl.getMigrateNextStage(compositionElementTarget, 0);
        assertEquals(2, result);
        result = apiImpl.getMigrateNextStage(compositionElementTarget, 2);
        assertEquals(2, result);
    }

    @Test
    void testGetRollbackNextStage() {
        var cacheProvider = mock(CacheProvider.class);
        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, cacheProvider);
        var migrate = Map.of("migrate", List.of(0, 2));
        Map<String, Object> stageSet = Map.of("stage", migrate);
        var compositionElementTarget = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                stageSet, Map.of());
        var result = apiImpl.getRollbackNextStage(compositionElementTarget, 2);
        assertEquals(0, result);
        result = apiImpl.getRollbackNextStage(compositionElementTarget, 0);
        assertEquals(0, result);
    }

    @Test
    void testUpdateAutomationCompositionElementStage() {
        var instanceId = UUID.randomUUID();
        var mockCacheProvider = mock(CacheProvider.class);
        when(mockCacheProvider.getAutomationComposition(instanceId)).thenReturn(null);
        var mockAutomationCompositionHandler = mock(AutomationCompositionOutHandler.class,
            withSettings().useConstructor(mock(ParticipantMessagePublisher.class), mockCacheProvider));
        var elementId = UUID.randomUUID();
        doCallRealMethod().when(mockAutomationCompositionHandler)
            .updateAutomationCompositionElementStage(instanceId, elementId, StateChangeResult.NO_ERROR, 1, "message");
        var api = new ParticipantIntermediaryApiImpl(mockAutomationCompositionHandler, mockCacheProvider);

        assertDoesNotThrow(() -> api.updateAutomationCompositionElementStage(instanceId, elementId,
            StateChangeResult.NO_ERROR, 1, "message"));
        verify(mockAutomationCompositionHandler).updateAutomationCompositionElementStage(instanceId, elementId,
            StateChangeResult.NO_ERROR, 1, "message");
    }

    @Test
    void testGetPrepareNextStage() {
        var cacheProvider = mock(CacheProvider.class);
        var automationCompositionHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationCompositionHandler, cacheProvider);
        var prepare = Map.of("prepare", List.of(0, 2));
        Map<String, Object> stageSet = Map.of("stage", prepare);
        var compositionElementTarget = new CompositionElementDto(UUID.randomUUID(), new ToscaConceptIdentifier(),
                stageSet, Map.of());
        var result = apiImpl.getPrepareNextStage(compositionElementTarget, 0);
        assertEquals(2, result);
        result = apiImpl.getPrepareNextStage(compositionElementTarget, 2);
        assertEquals(2, result);
    }
}
