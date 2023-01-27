/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AcmParticipantProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcmParticipantProvider.class);
    private final ParticipantProvider participantProvider;
    private final ParticipantStatusReqPublisher participantStatusReqPublisher;

    /**
     * Get all participants.
     *
     * @return A list of available participants
     */
    public List<ParticipantInformation> getAllParticipants() {
        var participants = this.participantProvider.getParticipants();

        List<ParticipantInformation> participantInformationList = new ArrayList<>();
        participants.forEach(participant -> {
            ParticipantInformation participantInformation = new ParticipantInformation();
            participantInformation.setParticipant(participant);
            participantInformationList.add(participantInformation);
        });
        return participantInformationList;
    }

    /**
     * Get a participant.
     *
     * @param participantId The UUID of the participant to get
     * @return The participant
     */
    public ParticipantInformation getParticipantById(UUID participantId) {
        var participant = this.participantProvider.getParticipantById(participantId);
        var participantInformation = new ParticipantInformation();
        participantInformation.setParticipant(participant);

        participantInformation.setAcElementInstanceMap(getAutomationCompositionElementsForParticipant(participantId));
        participantInformation.setAcNodeTemplateStateDefinitionMap(getNodeTemplateStatesForParticipant(participantId));

        return participantInformation;
    }

    /**
     * Send a participant status request.
     *
     * @param participantId The UUID of the participant to send request to
     */
    public void sendParticipantStatusRequest(UUID participantId) {
        var participant = this.participantProvider.getParticipantById(participantId);

        LOGGER.debug("Requesting Participant Status Now ParticipantStatusReq");
        participantStatusReqPublisher.send(participantId);
        participant.setParticipantState(ParticipantState.OFF_LINE);
        participantProvider.updateParticipant(participant);
    }

    /**
     * Send status request to all participants.
     *
     */
    public void sendAllParticipantStatusRequest() {
        this.participantStatusReqPublisher.send((UUID) null);
    }

    private Map<UUID, AutomationCompositionElement> getAutomationCompositionElementsForParticipant(UUID participantId) {
        var automationCompositionElements = participantProvider
            .getAutomationCompositionElements(participantId);
        Map<UUID, AutomationCompositionElement> map = new HashMap<>();
        MapUtils.populateMap(map, automationCompositionElements, AutomationCompositionElement::getId);

        return map;
    }

    private Map<UUID, NodeTemplateState> getNodeTemplateStatesForParticipant(UUID participantId) {
        var acNodeTemplateStates = participantProvider.getAcNodeTemplateStates(participantId);
        Map<UUID, NodeTemplateState> map = new HashMap<>();
        MapUtils.populateMap(map, acNodeTemplateStates, NodeTemplateState::getNodeTemplateStateId);

        return map;
    }
}
