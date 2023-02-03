/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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

import io.micrometer.core.annotation.Timed;
import java.util.List;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.acm.participant.intermediary.handler.Publisher;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to send Participant Status messages to clamp using TopicSinkClient.
 *
 */
@Component
public class ParticipantMessagePublisher implements Publisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantMessagePublisher.class);
    private static final String NOT_ACTIVE_TEXT = "Not Active!";

    private boolean active = false;
    private TopicSinkClient topicSinkClient;

    /**
     * Constructor for instantiating ParticipantMessagePublisher.
     *
     * @param topicSinks the topic sinks
     */
    @Override
    public void active(List<TopicSink> topicSinks) {
        if (topicSinks.size() != 1) {
            throw new IllegalArgumentException("Configuration unsupported, Topic sinks greater than 1");
        }
        this.topicSinkClient = new TopicSinkClient(topicSinks.get(0));
        active = true;
    }

    /**
     * Method to send Participant Status message to clamp on demand.
     *
     * @param participantStatus the Participant Status
     */
    @Timed(value = "publisher.participant_status", description = "PARTICIPANT_STATUS messages published")
    public void sendParticipantStatus(final ParticipantStatus participantStatus) {
        validate();
        topicSinkClient.send(participantStatus);
        LOGGER.info("Sent Participant Status message to CLAMP - {}", participantStatus);
    }

    /**
     * Method to send Participant Status message to clamp on demand.
     *
     * @param participantRegister the Participant Status
     */
    @Timed(value = "publisher.participant_register", description = "PARTICIPANT_REGISTER messages published")
    public void sendParticipantRegister(final ParticipantRegister participantRegister) {
        validate();
        topicSinkClient.send(participantRegister);
        LOGGER.info("Sent Participant Register message to CLAMP - {}", participantRegister);
    }

    /**
     * Method to send Participant Status message to clamp on demand.
     *
     * @param participantDeregister the Participant Status
     */
    @Timed(value = "publisher.participant_deregister", description = "PARTICIPANT_DEREGISTER messages published")
    public void sendParticipantDeregister(final ParticipantDeregister participantDeregister) {
        validate();
        topicSinkClient.send(participantDeregister);
        LOGGER.debug("Sent Participant Deregister message to CLAMP - {}", participantDeregister);
    }

    /**
     * Method to send Participant Prime Ack message to runtime.
     *
     * @param participantPrimeAck the Participant Prime Ack
     */
    @Timed(value = "publisher.participant_prime_ack", description = "PARTICIPANT_PRIME_ACK messages published")
    public void sendParticipantPrimeAck(final ParticipantPrimeAck participantPrimeAck) {
        validate();
        topicSinkClient.send(participantPrimeAck);
        LOGGER.debug("Sent Participant Prime Ack message to CLAMP - {}", participantPrimeAck);
    }

    /**
     * Method to send AutomationComposition Update/StateChange Ack message to runtime.
     *
     * @param automationCompositionAck AutomationComposition Update/StateChange Ack
     */
    @Timed(value = "publisher.automation_composition_update_ack",
            description = "AUTOMATION_COMPOSITION_UPDATE_ACK/AUTOMATION_COMPOSITION_STATECHANGE_ACK messages published")
    public void sendAutomationCompositionAck(final AutomationCompositionDeployAck automationCompositionAck) {
        validate();
        topicSinkClient.send(automationCompositionAck);
        LOGGER.debug("Sent AutomationComposition Update/StateChange Ack to runtime - {}", automationCompositionAck);
    }

    /**
     * Method to send Participant heartbeat to clamp on demand.
     *
     * @param participantStatus the Participant Status
     */
    @Timed(value = "publisher.participant_status", description = "PARTICIPANT_STATUS messages published")
    public void sendHeartbeat(final ParticipantStatus participantStatus) {
        validate();
        topicSinkClient.send(participantStatus);
        LOGGER.debug("Sent Participant heartbeat to CLAMP - {}", participantStatus);
    }

    private void validate() {
        if (!active) {
            throw new AutomationCompositionRuntimeException(Status.NOT_ACCEPTABLE, NOT_ACTIVE_TEXT);
        }
    }

    @Override
    public void stop() {
        active = false;
    }
}
