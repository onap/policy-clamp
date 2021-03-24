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
import java.util.List;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to send Participant Status messages to clamp using TopicSinkClient.
 */
public class ParticipantStatusPublisher implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantStatusPublisher.class);

    private final TopicSinkClient topicSinkClient;

    /**
     * Constructor for instantiating ParticipantStatusPublisher.
     *
     * @param topicSinks the topic sinks
     */
    public ParticipantStatusPublisher(List<TopicSink> topicSinks) {
        this.topicSinkClient = new TopicSinkClient(topicSinks.get(0));
    }

    /**
     * Method to send Participant Status message to clamp on demand.
     *
     * @param participantStatus the Participant Status
     */
    public void send(final ParticipantStatus participantStatus) {
        topicSinkClient.send(participantStatus);
        LOGGER.debug("Sent Participant Status message to CLAMP - {}", participantStatus);
    }

    @Override
    public void close() {
        // No explicit action on this class
    }
}
