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

import java.util.function.Consumer;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessage;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Listener for Participant messages sent by CLAMP.
 */
public abstract class ParticipantListener<T extends ParticipantMessage> extends ScoListener<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantListener.class);

    private final ParticipantHandler participantHandler;
    private final Consumer<T> consumer;

    /**
     * Constructs the object.
     *
     * @param clazz class of message this handles
     * @param participantHandler ParticipantHandler
     * @param consumer function that handles the message
     */
    protected ParticipantListener(Class<T> clazz, ParticipantHandler participantHandler, Consumer<T> consumer) {
        super(clazz);
        this.participantHandler = participantHandler;
        this.consumer = consumer;
    }

    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco, T message) {
        LOGGER.debug("Participant {} message received from CLAMP - {}", this.getClass().getName(), message);

        if (participantHandler.appliesTo(message)) {
            LOGGER.debug("Message for this participant - {}", message);
            consumer.accept(message);
        } else {
            LOGGER.debug("Message not for this participant - {}", message);
        }
    }
}
