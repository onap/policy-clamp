/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.handler;

import java.util.List;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.dmaap.element.ElementMessage;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.event.comm.client.TopicSinkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MessagePublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePublisher.class);
    private static final String NOT_ACTIVE_TEXT = "Not Active!";

    private boolean active = false;
    private TopicSinkClient topicSinkClient;

    /**
     * Constructor for instantiating MessagePublisher.
     *
     * @param topicSinks the topic sinks
     */
    public void active(List<TopicSink> topicSinks) {
        if (topicSinks.size() != 1) {
            throw new IllegalArgumentException("Configuration unsupported, Topic sinks greater than 1");
        }
        this.topicSinkClient = new TopicSinkClient(topicSinks.get(0));
        active = true;
    }

    /**
     * Method to send message.
     *
     * @param msg the acknowledgement message
     */
    public void publishMsg(final ElementMessage msg) {
        if (!active) {
            throw new AutomationCompositionRuntimeException(Response.Status.CONFLICT, NOT_ACTIVE_TEXT);
        }

        topicSinkClient.send(msg);
        LOGGER.debug("Sent message {}", msg);
    }

    public void stop() {
        active = false;
    }
}
