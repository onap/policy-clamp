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

import java.util.List;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to send Participant Status messages to clamp using TopicSinkClient.
 *
 */
public class ParticipantMessagePublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantMessagePublisher.class);

    private final TopicSinkClient topicSinkClient;

    /**
     * Constructor for instantiating ParticipantMessagePublisher.
     *
     * @param topicSinks the topic sinks
     */
    public ParticipantMessagePublisher(List<TopicSink> topicSinks) {
        if (topicSinks.size() != 1) {
            throw new IllegalArgumentException("Configuration unsupported, Topic sinks greater than 1");
        }
        this.topicSinkClient = new TopicSinkClient(topicSinks.get(0));
    }

    /**
     * Method to send Participant Status message to clamp on demand.
     *
     * @param participantStatus the Participant Status
     */
    public void sendParticipantStatus(final ParticipantStatus participantStatus) {
        topicSinkClient.send(participantStatus);
        LOGGER.debug("Sent Participant Status message to CLAMP - {}", participantStatus);
    }

    /**
     * Method to send Participant Status message to clamp on demand.
     *
     * @param participantRegister the Participant Status
     */
    public void sendParticipantRegister(final ParticipantRegister participantRegister) {
        topicSinkClient.send(participantRegister);
        LOGGER.debug("Sent Participant Register message to CLAMP - {}", participantRegister);
    }

    /**
     * Method to send Participant Status message to clamp on demand.
     *
     * @param participantDeregister the Participant Status
     */
    public void sendParticipantDeregister(final ParticipantDeregister participantDeregister) {
        topicSinkClient.send(participantDeregister);
        LOGGER.debug("Sent Participant Deregister message to CLAMP - {}", participantDeregister);
    }

    /**
     * Method to send Participant Update Ack message to runtime.
     *
     * @param participantUpdateAck the Participant Update Ack
     */
    public void sendParticipantUpdateAck(final ParticipantUpdateAck participantUpdateAck) {
        topicSinkClient.send(participantUpdateAck);
        LOGGER.debug("Sent Participant Update Ack message to CLAMP - {}", participantUpdateAck);
    }

    /**
     * Method to send ControlLoop Update/StateChange Ack message to runtime.
     *
     * @param controlLoopAck ControlLoop Update/StateChange Ack
     */
    public void sendControlLoopAck(final ControlLoopAck controlLoopAck) {
        topicSinkClient.send(controlLoopAck);
        LOGGER.debug("Sent ControlLoop Update/StateChange Ack to runtime - {}", controlLoopAck);
    }

    /**
     * Method to send Participant heartbeat to clamp on demand.
     *
     * @param participantStatus the Participant Status
     */
    public void sendHeartbeat(final ParticipantStatus participantStatus) {
        topicSinkClient.send(participantStatus);
        LOGGER.debug("Sent Participant heartbeat to CLAMP - {}", participantStatus);
    }
}
