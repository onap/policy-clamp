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
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for Participant State Change messages sent by CLAMP.
 */
public class ControlLoopStateChangeListener extends ScoListener<ParticipantControlLoopStateChange>
        implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopStateChangeListener.class);

    private final ParticipantHandler participantHandler;

    /**
     * Constructs the object.
     *
     * @param participantHandler the handler for managing the state of the participant
     */
    public ControlLoopStateChangeListener(final ParticipantHandler participantHandler) {
        super(ParticipantControlLoopStateChange.class);
        this.participantHandler = participantHandler;
    }

    @Override
    public void onTopicEvent(final CommInfrastructure infra, final String topic, final StandardCoderObject sco,
            final ParticipantControlLoopStateChange controlLoopStateChangeMsg) {
        LOGGER.debug("Control Loop State Change received from CLAMP - {}", controlLoopStateChangeMsg);

        if (participantHandler.appliesTo(controlLoopStateChangeMsg)) {
            LOGGER.debug("Message for this participant - {}", controlLoopStateChangeMsg);
            participantHandler.getControlLoopHandler().handleControlLoopStateChange(controlLoopStateChangeMsg);
        } else {
            LOGGER.debug("Message not for this participant - {}", controlLoopStateChangeMsg);
        }
    }

    @Override
    public void close() {
        // No explicit action on this class
    }
}
