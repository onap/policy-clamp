/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.handler.AutomationCompositionOutHandler;
import org.onap.policy.clamp.acm.participant.intermediary.handler.CacheProvider;
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

    @Test
    void testUpdateAutomationCompositionElementState() {
        var automationComposiitonHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationComposiitonHandler, mock(CacheProvider.class));
        apiImpl.updateAutomationCompositionElementState(AUTOMATION_COMPOSITION_ID, ELEMENT_ID, DeployState.UNDEPLOYED,
                LockState.NONE, StateChangeResult.NO_ERROR, null);
        verify(automationComposiitonHandler).updateAutomationCompositionElementState(AUTOMATION_COMPOSITION_ID,
                ELEMENT_ID, DeployState.UNDEPLOYED, LockState.NONE, StateChangeResult.NO_ERROR, null);
    }

    @Test
    void testUpdateCompositionState() {
        var automationComposiitonHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationComposiitonHandler, mock(CacheProvider.class));
        apiImpl.updateCompositionState(COMPOSITION_ID, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "");
        verify(automationComposiitonHandler).updateCompositionState(COMPOSITION_ID, AcTypeState.PRIMED,
                StateChangeResult.NO_ERROR, "");
    }

    @Test
    void testSendAcElementInfo() {
        var automationComposiitonHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationComposiitonHandler, mock(CacheProvider.class));
        apiImpl.sendAcElementInfo(AUTOMATION_COMPOSITION_ID, ELEMENT_ID, USE_STATE, OPERATIONAL_STATE, MAP);
        verify(automationComposiitonHandler).sendAcElementInfo(AUTOMATION_COMPOSITION_ID, ELEMENT_ID, USE_STATE,
                OPERATIONAL_STATE, MAP);
    }

    @Test
    void testSendAcDefinitionInfo() {
        var automationComposiitonHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationComposiitonHandler, mock(CacheProvider.class));
        apiImpl.sendAcDefinitionInfo(COMPOSITION_ID, DEFINITION_ELEMENT_ID, MAP);
        verify(automationComposiitonHandler).sendAcDefinitionInfo(COMPOSITION_ID, DEFINITION_ELEMENT_ID, MAP);
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

        var automationComposiitonHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationComposiitonHandler, cacheProvider);
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
        var cacheProvider = mock(CacheProvider.class);
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(DEFINITION_ELEMENT_ID);
        acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(new ToscaNodeTemplate());
        var elementsDefinitions = Map.of(DEFINITION_ELEMENT_ID, acElementDefinition);
        var map = Map.of(COMPOSITION_ID, elementsDefinitions);
        when(cacheProvider.getAcElementsDefinitions()).thenReturn(map);
        var automationComposiitonHandler = mock(AutomationCompositionOutHandler.class);
        var apiImpl = new ParticipantIntermediaryApiImpl(automationComposiitonHandler, cacheProvider);
        var mapResult = apiImpl.getAcElementsDefinitions();
        assertEquals(map, mapResult);

        var result = apiImpl.getAcElementsDefinitions(UUID.randomUUID());
        assertThat(result).isEmpty();

        result = apiImpl.getAcElementsDefinitions(COMPOSITION_ID);
        assertEquals(elementsDefinitions, result);

        var element = apiImpl.getAcElementDefinition(UUID.randomUUID(), new ToscaConceptIdentifier("wrong", "0.0.1"));
        assertThat(element).isNull();

        element = apiImpl.getAcElementDefinition(COMPOSITION_ID, new ToscaConceptIdentifier("wrong", "0.0.1"));
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
}
