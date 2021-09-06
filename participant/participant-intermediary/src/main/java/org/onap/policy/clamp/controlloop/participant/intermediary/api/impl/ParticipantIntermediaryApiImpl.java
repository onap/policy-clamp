/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.intermediary.api.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Component;

/**
 * This class is api implementation used by participant intermediary.
 */
@Component
public class ParticipantIntermediaryApiImpl implements ParticipantIntermediaryApi {

    // The handler for the participant intermediary
    private ParticipantHandler participantHandler;

    /**
     * Constructor.
     *
     * @param participantHandler ParticipantHandler
     */
    public ParticipantIntermediaryApiImpl(ParticipantHandler participantHandler) {
        this.participantHandler = participantHandler;
    }

    @Override
    public void registerControlLoopElementListener(ControlLoopElementListener controlLoopElementListener) {
        participantHandler.getControlLoopHandler().registerControlLoopElementListener(controlLoopElementListener);
    }

    @Override
    public void sendParticipantRegister() {
        participantHandler.sendParticipantRegister();
    }

    @Override
    public void sendParticipantDeregister() {
        participantHandler.sendParticipantDeregister();
    }

    @Override
    public List<Participant> getParticipants(String name, String version) {
        return List.of(participantHandler.getParticipant(name, version));
    }

    @Override
    public Participant updateParticipantState(ToscaConceptIdentifier definition, ParticipantState state) {
        return participantHandler.updateParticipantState(definition, state);
    }

    @Override
    public void updateParticipantStatistics(ParticipantStatistics participantStatistics) {
        // TODO Auto-generated method stub
    }

    @Override
    public ControlLoops getControlLoops(String name, String version) {
        return participantHandler.getControlLoopHandler().getControlLoops();
    }

    @Override
    public Map<UUID, ControlLoopElement> getControlLoopElements(String name, String version) {
        List<ControlLoop> controlLoops =
                participantHandler.getControlLoopHandler().getControlLoops().getControlLoopList();

        for (ControlLoop controlLoop : controlLoops) {
            if (name.equals(controlLoop.getDefinition().getName())) {
                return controlLoop.getElements();
            }
        }
        return new LinkedHashMap<>();
    }

    @Override
    public ControlLoopElement getControlLoopElement(UUID id) {
        List<ControlLoop> controlLoops =
                participantHandler.getControlLoopHandler().getControlLoops().getControlLoopList();

        for (ControlLoop controlLoop : controlLoops) {
            ControlLoopElement clElement = controlLoop.getElements().get(id);
            if (clElement != null) {
                return clElement;
            }
        }
        return null;
    }

    @Override
    public ControlLoopElement updateControlLoopElementState(ToscaConceptIdentifier controlLoopId,
            UUID id, ControlLoopOrderedState currentState,
            ControlLoopState newState, ParticipantMessageType messageType) {
        return participantHandler.getControlLoopHandler().updateControlLoopElementState(controlLoopId,
            id, currentState, newState);
    }

    @Override
    public void updateControlLoopElementStatistics(UUID id, ClElementStatistics elementStatistics) {
        participantHandler.getControlLoopHandler().updateControlLoopElementStatistics(id, elementStatistics);
    }
}
