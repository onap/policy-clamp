/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2024 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.comm;

import java.util.function.Consumer;
import org.onap.policy.clamp.acm.participant.intermediary.handler.Listener;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessage;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;

/**
 * Abstract Listener for Participant messages sent by CLAMP.
 */
public abstract class ParticipantListener<T extends ParticipantMessage> extends ScoListener<T> implements Listener<T> {

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
        if (participantHandler.appliesTo(message)) {
            consumer.accept(message);
        }
    }

    @Override
    public ScoListener<T> getScoListener() {
        return this;
    }
}
