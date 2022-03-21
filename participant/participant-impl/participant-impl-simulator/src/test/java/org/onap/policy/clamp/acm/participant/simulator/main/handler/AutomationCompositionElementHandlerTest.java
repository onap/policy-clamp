/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.simulator.main.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class AutomationCompositionElementHandlerTest {

    private static final String ID_NAME = "org.onap.PM_CDS_Blueprint";
    private static final String ID_VERSION = "1.0.1";
    private static final UUID automationCompositionElementId = UUID.randomUUID();
    private static final ToscaConceptIdentifier automationCompositionId =
        new ToscaConceptIdentifier(ID_NAME, ID_VERSION);

    @Test
    void testSimulatorHandlerExceptions() {
        AutomationCompositionElementHandler handler = getTestingHandler();

        assertDoesNotThrow(() -> handler.automationCompositionElementStateChange(automationCompositionId,
            automationCompositionElementId, AutomationCompositionState.UNINITIALISED,
            AutomationCompositionOrderedState.PASSIVE));

        assertDoesNotThrow(() -> handler.automationCompositionElementStateChange(automationCompositionId,
            automationCompositionElementId, AutomationCompositionState.RUNNING,
            AutomationCompositionOrderedState.UNINITIALISED));

        assertDoesNotThrow(() -> handler.automationCompositionElementStateChange(automationCompositionId,
            automationCompositionElementId, AutomationCompositionState.PASSIVE,
            AutomationCompositionOrderedState.RUNNING));
        var element = getTestingAcElement();
        var acElementDefinition = Mockito.mock(ToscaNodeTemplate.class);

        assertDoesNotThrow(
            () -> handler.automationCompositionElementUpdate(automationCompositionId, element, acElementDefinition));

        assertDoesNotThrow(() -> handler.handleStatistics(automationCompositionElementId));
    }

    AutomationCompositionElementHandler getTestingHandler() {
        var handler = new AutomationCompositionElementHandler();
        var intermediaryApi = Mockito.mock(ParticipantIntermediaryApi.class);
        var element = getTestingAcElement();
        when(intermediaryApi.getAutomationCompositionElement(automationCompositionElementId)).thenReturn(element);
        handler.setIntermediaryApi(intermediaryApi);
        return handler;
    }

    AutomationCompositionElement getTestingAcElement() {
        var element = new AutomationCompositionElement();
        element.setDefinition(automationCompositionId);
        element.setDescription("Description");
        element.setId(automationCompositionElementId);
        element.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        element.setParticipantId(automationCompositionId);
        element.setState(AutomationCompositionState.UNINITIALISED);
        var template = Mockito.mock(ToscaServiceTemplate.class);
        element.setToscaServiceTemplateFragment(template);
        return element;
    }
}
