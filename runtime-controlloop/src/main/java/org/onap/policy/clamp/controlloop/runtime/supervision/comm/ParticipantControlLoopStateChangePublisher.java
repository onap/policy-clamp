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

import java.util.List;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to send ParticipantControlLoopStateChangePublisher messages to participants on DMaaP.
 */
public class ParticipantControlLoopStateChangePublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantControlLoopStateChangePublisher.class);

    private TopicSinkClient topicSinkClient;

    /**
     * Constructor for instantiating ParticipantControlLoopStateChangePublisherPublisher.
     *
     * @param topicSinks the topic sinks
     * @param interval time interval to send ParticipantControlLoopStateChangePublisher messages
     */
    public ParticipantControlLoopStateChangePublisher(final List<TopicSink> topicSinks, final long interval) {
        // TODO: Should not be dependent on the order of topic sinks in the config
        this.topicSinkClient = new TopicSinkClient(topicSinks.get(0));
    }

    /**
     * Terminates the current timer.
     */
    public void terminate() {
        // This is a user initiated message and doesn't need a timer.
    }

    /**
     * Get the current time interval used by the timer task.
     *
     * @return interval the current time interval
     */
    public long getInterval() {
        // This is a user initiated message and doesn't need a timer.
        return -1;
    }

    /**
     * Method to send ParticipantControlLoopStateChangePublisher status message to participants on demand.
     *
     * @param controlLoopStateChange the ParticipantControlLoopStateChangePublisher message
     */
    public void send(final ParticipantControlLoopStateChange controlLoopStateChange) {
        topicSinkClient.send(controlLoopStateChange);
        LOGGER.debug("Sent ParticipantControlLoopStateChange to Participants - {}", controlLoopStateChange);
    }
}
