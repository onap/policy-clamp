/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.handler.AutomationCompositionHandler;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.springframework.stereotype.Component;

/**
 * This class is api implementation used by participant intermediary.
 */
@Component
public class ParticipantIntermediaryApiImpl implements ParticipantIntermediaryApi {

    // The handler for the participant intermediary
    private final ParticipantHandler participantHandler;

    // The handler for the automationComposition intermediary
    private final AutomationCompositionHandler automationCompositionHandler;

    /**
     * Constructor.
     *
     * @param participantHandler ParticipantHandler
     * @param automationCompositionHandler AutomationCompositionHandler
     */
    public ParticipantIntermediaryApiImpl(ParticipantHandler participantHandler,
        AutomationCompositionHandler automationCompositionHandler) {
        this.participantHandler = participantHandler;
        this.automationCompositionHandler = automationCompositionHandler;
    }

    @Override
    public void registerAutomationCompositionElementListener(
        AutomationCompositionElementListener automationCompositionElementListener) {
        automationCompositionHandler.registerAutomationCompositionElementListener(automationCompositionElementListener);
    }

    @Override
    public List<Participant> getParticipants(String name, String version) {
        return List.of(participantHandler.getParticipant(name, version));
    }

    @Override
    public Map<String, ToscaProperty> getAcElementDefinitionCommonProperties(ToscaConceptIdentifier acElementDef) {
        return participantHandler.getAcElementDefinitionCommonProperties(acElementDef);
    }

    @Override
    public Participant updateParticipantState(ToscaConceptIdentifier definition, ParticipantState state) {
        return participantHandler.updateParticipantState(definition, state);
    }

    @Override
    public AutomationCompositions getAutomationCompositions(String name, String version) {
        return automationCompositionHandler.getAutomationCompositions();
    }

    @Override
    public Map<UUID, AutomationCompositionElement> getAutomationCompositionElements(String name, String version) {
        List<AutomationComposition> automationCompositions =
            automationCompositionHandler.getAutomationCompositions().getAutomationCompositionList();

        for (AutomationComposition automationComposition : automationCompositions) {
            if (name.equals(automationComposition.getDefinition().getName())) {
                return automationComposition.getElements();
            }
        }
        return new LinkedHashMap<>();
    }

    @Override
    public AutomationCompositionElement getAutomationCompositionElement(UUID id) {
        List<AutomationComposition> automationCompositions =
            automationCompositionHandler.getAutomationCompositions().getAutomationCompositionList();

        for (AutomationComposition automationComposition : automationCompositions) {
            AutomationCompositionElement acElement = automationComposition.getElements().get(id);
            if (acElement != null) {
                return acElement;
            }
        }
        return null;
    }

    @Override
    public AutomationCompositionElement updateAutomationCompositionElementState(
        ToscaConceptIdentifier automationCompositionId, UUID id, AutomationCompositionOrderedState currentState,
        AutomationCompositionState newState, ParticipantMessageType messageType) {
        return automationCompositionHandler.updateAutomationCompositionElementState(automationCompositionId, id,
            currentState, newState);
    }
}
