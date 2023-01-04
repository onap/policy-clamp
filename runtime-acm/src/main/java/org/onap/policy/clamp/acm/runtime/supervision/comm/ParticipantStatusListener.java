/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.supervision.comm;

import org.onap.policy.clamp.acm.runtime.config.messaging.Listener;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionParticipantHandler;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Listener for ParticipantStatus messages sent by participants.
 */
@Component
public class ParticipantStatusListener extends ScoListener<ParticipantStatus> implements Listener<ParticipantStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantStatusListener.class);

    private final SupervisionParticipantHandler supervisionHandler;

    /**
     * Constructs the object.
     */
    public ParticipantStatusListener(SupervisionParticipantHandler supervisionHandler) {
        super(ParticipantStatus.class);
        this.supervisionHandler = supervisionHandler;
    }

    @Override
    public void onTopicEvent(final CommInfrastructure infra, final String topic, final StandardCoderObject sco,
            final ParticipantStatus participantStatusMessage) {
        LOGGER.debug("ParticipantStatus message received from participant - {}", participantStatusMessage);
        supervisionHandler.handleParticipantMessage(participantStatusMessage);
    }

    @Override
    public String getType() {
        return ParticipantMessageType.PARTICIPANT_STATUS.name();
    }

    @Override
    public ScoListener<ParticipantStatus> getScoListener() {
        return this;
    }
}
