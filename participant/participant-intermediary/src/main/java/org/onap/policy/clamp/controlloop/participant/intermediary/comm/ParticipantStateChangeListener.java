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

package org.onap.policy.clamp.controlloop.participant.intermediary.comm;

import java.io.Closeable;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStateChange;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for Participant State Change messages sent by CLAMP.
 *
 */
public class ParticipantStateChangeListener extends ScoListener<ParticipantStateChange> implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantStateChangeListener.class);

    private final ParticipantHandler participantHandler;

    /**
     * Constructs the object.
     *
     * @param participantHandler the handler for managing the state of the participant
     */
    public ParticipantStateChangeListener(final ParticipantHandler participantHandler) {
        super(ParticipantStateChange.class);
        this.participantHandler = participantHandler;
    }

    @Override
    public void onTopicEvent(final CommInfrastructure infra, final String topic, final StandardCoderObject sco,
            final ParticipantStateChange participantStateChangeMsg) {
        LOGGER.debug("Participant State Change received from CLAMP - {}", participantStateChangeMsg);

        if (participantHandler.canHandle(participantStateChangeMsg)) {
            LOGGER.debug("Message for this participant");
            participantHandler.handleParticipantStateChange(participantStateChangeMsg);
        } else {
            LOGGER.debug("Message not for this participant");
        }
    }

    @Override
    public void close() {
        // No explicit action on this class
    }
}
