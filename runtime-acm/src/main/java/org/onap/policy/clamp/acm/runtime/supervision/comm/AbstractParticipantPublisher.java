/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2024 Nordix Foundation.
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

import jakarta.ws.rs.core.Response.Status;
import org.onap.policy.clamp.acm.runtime.config.messaging.Publisher;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessage;
import org.onap.policy.common.message.bus.event.TopicSink;
import org.onap.policy.common.message.bus.event.client.TopicSinkClient;


public abstract class AbstractParticipantPublisher<E extends ParticipantMessage> implements Publisher {

    private TopicSinkClient topicSinkClient;
    private boolean active = false;

    /**
     * Method to send Participant message to participants on demand.
     *
     * @param participantMessage the Participant message
     */
    public void send(final E participantMessage) {
        if (!active) {
            throw new AutomationCompositionRuntimeException(Status.NOT_ACCEPTABLE, "Not Active!");
        }
        topicSinkClient.send(participantMessage);
    }


    @Override
    public void active(TopicSink topicSink) {
        this.topicSinkClient = new TopicSinkClient(topicSink);
        active = true;
    }

    @Override
    public void stop() {
        active = false;
    }

    /**
     * Is default topic.
     * @return true if default
     */
    @Override
    public boolean isDefaultTopic() {
        return true;
    }
}
