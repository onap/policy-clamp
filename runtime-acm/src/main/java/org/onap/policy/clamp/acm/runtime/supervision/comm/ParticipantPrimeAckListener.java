/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.policy.clamp.acm.runtime.main.utils.NetLoggerUtil;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.springframework.stereotype.Component;

/**
 * Listener for ParticipantPrimeAck messages sent by participants.
 */
@Component
public class ParticipantPrimeAckListener extends ScoListener<ParticipantPrimeAck>
                implements Listener<ParticipantPrimeAck> {

    private final SupervisionHandler supervisionHandler;

    /**
     * Constructs the object.
     */
    public ParticipantPrimeAckListener(SupervisionHandler supervisionHandler) {
        super(ParticipantPrimeAck.class);
        this.supervisionHandler = supervisionHandler;
    }

    @Override
    public void onTopicEvent(final CommInfrastructure infra, final String topic, final StandardCoderObject sco,
            final ParticipantPrimeAck participantPrimeAckMessage) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.IN, infra, topic, participantPrimeAckMessage.toString());
        supervisionHandler.handleParticipantMessage(participantPrimeAckMessage);
    }

    @Override
    public String getType() {
        return ParticipantMessageType.PARTICIPANT_PRIME_ACK.name();
    }

    @Override
    public ScoListener<ParticipantPrimeAck> getScoListener() {
        return this;
    }
}
