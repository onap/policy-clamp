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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
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
    public List<Participant> getParticipants(String name, String version) {
        return Arrays.asList(activator.getParticipantHandler().getParticipant(name, version));
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
    public List<ControlLoopElement> getControlLoopElements(String name, String version) {
        List<ControlLoop> controlLoops = activator.getParticipantHandler()
                .getControlLoopHandler().getControlLoops().getControlLoopList();

        for (ControlLoop controlLoop : controlLoops) {
            if (controlLoop.getDefinition().getName().equals(name)) {
                return controlLoop.getElements();
            }
        }
        return Collections.emptyList();
    }

    @Override
    public ControlLoop updateControlLoopState(ToscaConceptIdentifier definition, ControlLoopOrderedState state) {
        return activator.getParticipantHandler().getControlLoopHandler()
                .updateControlLoopState(definition, state);
    }

    @Override
    public ControlLoopElement updateControlLoopElementState(UUID id, ControlLoopOrderedState state) {
        return activator.getParticipantHandler().getControlLoopHandler()
                .updateControlLoopElementState(id, state);
    }

    @Override
    public void updateControlLoopElementStatistics(ClElementStatistics elementStatistics) {
        activator.getParticipantHandler().getControlLoopHandler()
        .updateControlLoopElementStatistics(elementStatistics);
    }

    @Override
    public ParticipantHandler getParticipantHandler() {
        return activator.getParticipantHandler();
    }
}
