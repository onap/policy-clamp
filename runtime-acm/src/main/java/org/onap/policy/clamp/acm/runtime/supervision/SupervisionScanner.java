/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionUpdatePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to scan the automation compositions in the database and check if they are in the correct state.
 */
@Component
public class SupervisionScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionScanner.class);

    private final HandleCounter<ToscaConceptIdentifier> automationCompositionCounter = new HandleCounter<>();
    private final HandleCounter<ToscaConceptIdentifier> participantStatusCounter = new HandleCounter<>();
    private final HandleCounter<Pair<ToscaConceptIdentifier, ToscaConceptIdentifier>> participantUpdateCounter =
            new HandleCounter<>();

    private final Map<ToscaConceptIdentifier, Integer> phaseMap = new HashMap<>();

    private final AutomationCompositionProvider automationCompositionProvider;
    private final AcDefinitionProvider acDefinitionProvider;
    private final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher;
    private final AutomationCompositionUpdatePublisher automationCompositionUpdatePublisher;
    private final ParticipantProvider participantProvider;
    private final ParticipantStatusReqPublisher participantStatusReqPublisher;
    private final ParticipantUpdatePublisher participantUpdatePublisher;

    /**
     * Constructor for instantiating SupervisionScanner.
     *
     * @param automationCompositionProvider the provider to use to read automation compositions from the database
     * @param acDefinitionProvider the Policy Models Provider
     * @param automationCompositionStateChangePublisher the AutomationComposition StateChange Publisher
     * @param automationCompositionUpdatePublisher the AutomationCompositionUpdate Publisher
     * @param participantProvider the Participant Provider
     * @param participantStatusReqPublisher the Participant StatusReq Publisher
     * @param participantUpdatePublisher the Participant Update Publisher
     * @param acRuntimeParameterGroup the parameters for the automation composition runtime
     */
    public SupervisionScanner(final AutomationCompositionProvider automationCompositionProvider,
            AcDefinitionProvider acDefinitionProvider,
            final AutomationCompositionStateChangePublisher automationCompositionStateChangePublisher,
            AutomationCompositionUpdatePublisher automationCompositionUpdatePublisher,
            ParticipantProvider participantProvider, ParticipantStatusReqPublisher participantStatusReqPublisher,
            ParticipantUpdatePublisher participantUpdatePublisher,
            final AcRuntimeParameterGroup acRuntimeParameterGroup) {
        this.automationCompositionProvider = automationCompositionProvider;
        this.acDefinitionProvider = acDefinitionProvider;
        this.automationCompositionStateChangePublisher = automationCompositionStateChangePublisher;
        this.automationCompositionUpdatePublisher = automationCompositionUpdatePublisher;
        this.participantProvider = participantProvider;
        this.participantStatusReqPublisher = participantStatusReqPublisher;
        this.participantUpdatePublisher = participantUpdatePublisher;

        automationCompositionCounter.setMaxRetryCount(
                acRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().getMaxRetryCount());
        automationCompositionCounter
                .setMaxWaitMs(acRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs());

        participantUpdateCounter.setMaxRetryCount(
                acRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().getMaxRetryCount());
        participantUpdateCounter
                .setMaxWaitMs(acRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().getMaxWaitMs());

        participantStatusCounter.setMaxRetryCount(
                acRuntimeParameterGroup.getParticipantParameters().getUpdateParameters().getMaxRetryCount());
        participantStatusCounter.setMaxWaitMs(acRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs());
    }

    /**
     * Run Scanning.
     *
     * @param counterCheck if true activate counter and retry
     */
    public void run(boolean counterCheck) {
        LOGGER.debug("Scanning automation compositions in the database . . .");

        if (counterCheck) {
            try {
                for (var participant : participantProvider.getParticipants()) {
                    scanParticipantStatus(participant);
                }
            } catch (PfModelException pfme) {
                LOGGER.warn("error reading participant from database", pfme);
                return;
            }
        }

        try {
            var list = acDefinitionProvider.getAllAcDefinitions();
            for (var acDefinition : list) {
                var acList =
                        automationCompositionProvider.getAcInstancesByCompositionId(acDefinition.getCompositionId());
                for (var automationComposition : acList) {
                    scanAutomationComposition(automationComposition, acDefinition.getServiceTemplate(), counterCheck);
                }
            }
        } catch (PfModelException pfme) {
            LOGGER.warn("error reading automation compositions from database", pfme);
        }

        if (counterCheck) {
            scanParticipantUpdate();
        }

        LOGGER.debug("Automation composition scan complete . . .");
    }

    private void scanParticipantUpdate() {
        LOGGER.debug("Scanning participants to update . . .");

        for (var id : participantUpdateCounter.keySet()) {
            if (participantUpdateCounter.isFault(id)) {
                LOGGER.debug("report Participant Update fault");

            } else if (participantUpdateCounter.getDuration(id) > participantUpdateCounter.getMaxWaitMs()) {

                if (participantUpdateCounter.count(id)) {
                    LOGGER.debug("retry message ParticipantUpdate");
                    participantUpdatePublisher.sendCommissioning(null, null, id.getLeft(), id.getRight());
                } else {
                    LOGGER.debug("report Participant Update fault");
                    participantUpdateCounter.setFault(id);
                }
            }
        }

        LOGGER.debug("Participants to update scan complete . . .");
    }

    private void scanParticipantStatus(Participant participant) throws PfModelException {
        ToscaConceptIdentifier id = participant.getKey().asIdentifier();
        if (participantStatusCounter.isFault(id)) {
            LOGGER.debug("report Participant fault");
            return;
        }
        if (participantStatusCounter.getDuration(id) > participantStatusCounter.getMaxWaitMs()) {
            if (participantStatusCounter.count(id)) {
                LOGGER.debug("retry message ParticipantStatusReq");
                participantStatusReqPublisher.send(id);
                participant.setHealthStatus(ParticipantHealthStatus.NOT_HEALTHY);
            } else {
                LOGGER.debug("report Participant fault");
                participantStatusCounter.setFault(id);
                participant.setHealthStatus(ParticipantHealthStatus.OFF_LINE);
            }
            participantProvider.saveParticipant(participant);
        }
    }

    /**
     * handle participant Status message.
     */
    public void handleParticipantStatus(ToscaConceptIdentifier id) {
        participantStatusCounter.clear(id);
    }

    public void handleParticipantRegister(Pair<ToscaConceptIdentifier, ToscaConceptIdentifier> id) {
        participantUpdateCounter.clear(id);
    }

    public void handleParticipantUpdateAck(Pair<ToscaConceptIdentifier, ToscaConceptIdentifier> id) {
        participantUpdateCounter.remove(id);
    }

    private void scanAutomationComposition(final AutomationComposition automationComposition,
            ToscaServiceTemplate toscaServiceTemplate, boolean counterCheck) throws PfModelException {
        LOGGER.debug("scanning automation composition {} . . .", automationComposition.getKey().asIdentifier());

        if (automationComposition.getState().equals(automationComposition.getOrderedState().asState())) {
            LOGGER.debug("automation composition {} scanned, OK", automationComposition.getKey().asIdentifier());

            // Clear missed report counter on automation composition
            clearFaultAndCounter(automationComposition);
            return;
        }

        var completed = true;
        var minSpNotCompleted = 1000; // min startPhase not completed
        var maxSpNotCompleted = 0; // max startPhase not completed
        var defaultMin = 1000; // min startPhase
        var defaultMax = 0; // max startPhase
        for (AutomationCompositionElement element : automationComposition.getElements().values()) {
            ToscaNodeTemplate toscaNodeTemplate = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                    .get(element.getDefinition().getName());
            int startPhase = ParticipantUtils.findStartPhase(toscaNodeTemplate.getProperties());
            defaultMin = Math.min(defaultMin, startPhase);
            defaultMax = Math.max(defaultMax, startPhase);
            if (!element.getState().equals(element.getOrderedState().asState())) {
                completed = false;
                minSpNotCompleted = Math.min(minSpNotCompleted, startPhase);
                maxSpNotCompleted = Math.max(maxSpNotCompleted, startPhase);
            }
        }

        if (completed) {
            LOGGER.debug("automation composition scan: transition from state {} to {} completed",
                    automationComposition.getState(), automationComposition.getOrderedState());

            automationComposition.setState(automationComposition.getOrderedState().asState());
            automationCompositionProvider.saveAutomationComposition(automationComposition);

            // Clear missed report counter on automation composition
            clearFaultAndCounter(automationComposition);
        } else {
            LOGGER.debug("automation composition scan: transition from state {} to {} not completed",
                    automationComposition.getState(), automationComposition.getOrderedState());

            var nextSpNotCompleted =
                    AutomationCompositionState.UNINITIALISED2PASSIVE.equals(automationComposition.getState())
                            || AutomationCompositionState.PASSIVE2RUNNING.equals(automationComposition.getState())
                                    ? minSpNotCompleted
                                    : maxSpNotCompleted;

            var firstStartPhase =
                    AutomationCompositionState.UNINITIALISED2PASSIVE.equals(automationComposition.getState())
                            || AutomationCompositionState.PASSIVE2RUNNING.equals(automationComposition.getState())
                                    ? defaultMin
                                    : defaultMax;

            if (nextSpNotCompleted != phaseMap.getOrDefault(automationComposition.getKey().asIdentifier(),
                    firstStartPhase)) {
                phaseMap.put(automationComposition.getKey().asIdentifier(), nextSpNotCompleted);
                sendAutomationCompositionMsg(automationComposition, nextSpNotCompleted);
            } else if (counterCheck) {
                phaseMap.put(automationComposition.getKey().asIdentifier(), nextSpNotCompleted);
                handleCounter(automationComposition, nextSpNotCompleted);
            }
        }
    }

    private void clearFaultAndCounter(AutomationComposition automationComposition) {
        automationCompositionCounter.clear(automationComposition.getKey().asIdentifier());
        phaseMap.clear();
    }

    private void handleCounter(AutomationComposition automationComposition, int startPhase) {
        ToscaConceptIdentifier id = automationComposition.getKey().asIdentifier();
        if (automationCompositionCounter.isFault(id)) {
            LOGGER.debug("report AutomationComposition fault");
            return;
        }

        if (automationCompositionCounter.getDuration(id) > automationCompositionCounter.getMaxWaitMs()) {
            if (automationCompositionCounter.count(id)) {
                phaseMap.put(id, startPhase);
                sendAutomationCompositionMsg(automationComposition, startPhase);
            } else {
                LOGGER.debug("report AutomationComposition fault");
                automationCompositionCounter.setFault(id);
            }
        }
    }

    private void sendAutomationCompositionMsg(AutomationComposition automationComposition, int startPhase) {
        if (AutomationCompositionState.UNINITIALISED2PASSIVE.equals(automationComposition.getState())) {
            LOGGER.debug("retry message AutomationCompositionUpdate");
            automationCompositionUpdatePublisher.send(automationComposition, startPhase);
        } else {
            LOGGER.debug("retry message AutomationCompositionStateChange");
            automationCompositionStateChangePublisher.send(automationComposition, startPhase);
        }
    }
}
