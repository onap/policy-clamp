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
import java.util.List;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantInformation;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AcmParticipantProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcmParticipantProvider.class);
    private final ParticipantProvider participantProvider;
    private final ParticipantStatusReqPublisher participantStatusReqPublisher;

    public AcmParticipantProvider(ParticipantProvider participantProvider,
                                  ParticipantStatusReqPublisher participantStatusReqPublisher) {
        this.participantProvider = participantProvider;
        this.participantStatusReqPublisher = participantStatusReqPublisher;
    }

    /**
     * Get all participants.
     *
     * @return A list of available participants
     */
    public List<ParticipantInformation> getAllParticipants() {
        List<Participant> participants = this.participantProvider.getParticipants();

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
    public ParticipantInformation getParticipantById(String participantId) {
        Participant participant = this.participantProvider.getParticipantById(participantId);
        ParticipantInformation participantInformation = new ParticipantInformation();
        participantInformation.setParticipant(participant);
        return participantInformation;
    }

    /**
     * Send a participant status request.
     *
     * @param participantId The UUID of the participant to send request to
     */
    public void sendParticipantStatusRequest(String participantId) {
        Participant participant = this.participantProvider.getParticipantById(participantId);
        ToscaConceptIdentifier id = participant.getKey().asIdentifier();

        LOGGER.debug("Requesting Participant Status Now ParticipantStatusReq");
        participantStatusReqPublisher.send(id);
        participant.setParticipantState(ParticipantState.OFF_LINE);
        participantProvider.updateParticipant(participant);
    }

    /**
     * Send status request to all participants.
     *
     */
    public void sendAllParticipantStatusRequest() {
        this.participantStatusReqPublisher.send((ToscaConceptIdentifier) null);
    }
}
