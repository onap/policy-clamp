/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.supervision.comm;

import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.runtime.config.messaging.Listener;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Listener for ControlLoopStateChangeAck messages sent by participants.
 */
@Component
public class ControlLoopStateChangeAckListener extends ScoListener<ControlLoopAck> implements Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopStateChangeAckListener.class);

    private final SupervisionHandler supervisionHandler;

    /**
     * Constructs the object.
     */
    public ControlLoopStateChangeAckListener(SupervisionHandler supervisionHandler) {
        super(ControlLoopAck.class);
        this.supervisionHandler = supervisionHandler;
    }

    @Override
    public void onTopicEvent(final CommInfrastructure infra, final String topic, final StandardCoderObject sco,
            final ControlLoopAck controlLoopStateChangeAckMessage) {
        LOGGER.debug("ControlLoopStateChangeAck received from participant - {}", controlLoopStateChangeAckMessage);
        supervisionHandler.handleControlLoopStateChangeAckMessage(controlLoopStateChangeAckMessage);
    }

    @Override
    public ScoListener<ControlLoopAck> getScoListener() {
        return this;
    }

    @Override
    public String getType() {
        return ParticipantMessageType.CONTROLLOOP_STATECHANGE_ACK.name();
    }
}
