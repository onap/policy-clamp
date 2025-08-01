/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.participants;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionParticipantHandler;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AcmParticipantProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcmParticipantProvider.class);
    private final ParticipantProvider participantProvider;
    private final SupervisionParticipantHandler supervisionParticipantHandler;
    private final ParticipantStatusReqPublisher participantStatusReqPublisher;

    /**
     * Get all participants.
     *
     * @param pageable the Pageable
     * @return A list of available participants
     */
    public List<ParticipantInformation> getAllParticipants(final Pageable pageable) {
        var participants = this.participantProvider.getParticipants();
        return participants.stream().map(participant -> createParticipantInformation(participant, pageable)).toList();
    }

    private ParticipantInformation createParticipantInformation(Participant participant, Pageable pageable) {
        var participantInformation = new ParticipantInformation();
        participantInformation.setParticipant(participant);
        participantInformation.setAcElementInstanceMap(
            getAcElementsForParticipant(participant.getParticipantId(), pageable));
        participantInformation.setAcNodeTemplateStateDefinitionMap(
            getNodeTemplateStatesForParticipant(participant.getParticipantId(), pageable));
        return participantInformation;
    }

    /**
     * Get a participant.
     *
     * @param participantId The UUID of the participant to get
     * @param pageable the Pageable
     * @return The participant
     */
    public ParticipantInformation getParticipantById(final UUID participantId, final Pageable pageable) {
        var participant = this.participantProvider.getParticipantById(participantId);
        return createParticipantInformation(participant, pageable);
    }

    /**
     * Send a participant status request.
     *
     * @param participantId The UUID of the participant to send request to
     */
    public void sendParticipantStatusRequest(UUID participantId) {
        // check if participant is present
        this.participantProvider.getParticipantById(participantId);

        LOGGER.debug("Requesting Participant Status Now ParticipantStatusReq");
        participantStatusReqPublisher.send(participantId);
    }

    /**
     * Send status request to all participants.
     *
     */
    public void sendAllParticipantStatusRequest() {
        this.participantStatusReqPublisher.send((UUID) null);
    }

    private Map<UUID, AutomationCompositionElement> getAcElementsForParticipant(UUID participantId, Pageable pageable) {
        var automationCompositionElements =
            participantProvider.getAutomationCompositionElements(participantId, pageable);
        return automationCompositionElements
            .stream().collect(Collectors.toMap(AutomationCompositionElement::getId, Function.identity()));
    }

    private Map<UUID, NodeTemplateState> getNodeTemplateStatesForParticipant(UUID participantId, Pageable pageable) {
        var acNodeTemplateStates = participantProvider.getAcNodeTemplateStates(participantId, pageable);
        return acNodeTemplateStates
            .stream().collect(Collectors.toMap(NodeTemplateState::getNodeTemplateStateId, Function.identity()));
    }

    /**
     * Restart specific participant.
     *
     * @param participantId     ID of participant to restart
     */
    public void restartParticipant(UUID participantId) {
        if (participantProvider.findParticipant(participantId).isEmpty()) {
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND,
                    "Participant Not Found with ID: " + participantId);
        }
        supervisionParticipantHandler.handleRestart(participantId, null);
        LOGGER.debug("Restarting participant with ID: {}", participantId);
    }


    /**
     * Restart all participants.
     */
    public void restartAllParticipants() {
        supervisionParticipantHandler.handleRestartOfAllParticipants();
        LOGGER.debug("Restarting all participants");
    }
}
