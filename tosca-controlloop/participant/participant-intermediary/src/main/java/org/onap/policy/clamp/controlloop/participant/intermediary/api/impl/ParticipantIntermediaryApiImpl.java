/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.IntermediaryActivator;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * This class is api implementation used by participant intermediary.
 */
public class ParticipantIntermediaryApiImpl implements ParticipantIntermediaryApi {

    // The activator for the participant intermediary
    private IntermediaryActivator activator;

    @Override
    public void init(ParticipantIntermediaryParameters parameters) {
        activator = new IntermediaryActivator(parameters);

        activator.start();
    }

    @Override
    public void close() {
        activator.shutdown();
    }

    @Override
    public void registerControlLoopElementListener(ControlLoopElementListener controlLoopElementListener) {
        activator.getParticipantHandler().getControlLoopHandler()
                .registerControlLoopElementListener(controlLoopElementListener);
    }

    @Override
    public List<Participant> getParticipants(String name, String version) {
        return List.of(activator.getParticipantHandler().getParticipant(name, version));
    }

    @Override
    public Participant updateParticipantState(ToscaConceptIdentifier definition, ParticipantState state) {
        return activator.getParticipantHandler().updateParticipantState(definition, state);
    }

    @Override
    public void updateParticipantStatistics(ParticipantStatistics participantStatistics) {
        // TODO Auto-generated method stub
    }

    @Override
    public ControlLoops getControlLoops(String name, String version) {
        return activator.getParticipantHandler().getControlLoopHandler().getControlLoops();
    }

    @Override
    public Map<UUID, ControlLoopElement> getControlLoopElements(String name, String version) {
        List<ControlLoop> controlLoops = activator.getParticipantHandler()
                .getControlLoopHandler().getControlLoops().getControlLoopList();

        for (ControlLoop controlLoop : controlLoops) {
            if (name.equals(controlLoop.getDefinition().getName())) {
                return controlLoop.getElements();
            }
        }
        return new LinkedHashMap<>();
    }
    
    @Override
    public ControlLoopElement getControlLoopElement(UUID id) {
        List<ControlLoop> controlLoops = activator.getParticipantHandler()
                .getControlLoopHandler().getControlLoops().getControlLoopList();

        for (ControlLoop controlLoop : controlLoops) {
            if (controlLoop.getElements().get(id) != null) {
                return controlLoop.getElements().get(id);
            }
        }
        return null;
    }

    @Override
    public ControlLoopElement updateControlLoopElementState(UUID id, ControlLoopOrderedState currentState,
            ControlLoopState newState) {
        return activator.getParticipantHandler().getControlLoopHandler()
                .updateControlLoopElementState(id, currentState, newState);
    }

    @Override
    public void updateControlLoopElementStatistics(UUID id, ClElementStatistics elementStatistics) {
        activator.getParticipantHandler().getControlLoopHandler()
        .updateControlLoopElementStatistics(id, elementStatistics);
    }

    @Override
    public ParticipantHandler getParticipantHandler() {
        return activator.getParticipantHandler();
    }
}
