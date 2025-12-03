/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019, 2024 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.message.bus.event.client;

import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import org.onap.policy.common.message.bus.event.TopicEndpointManager;
import org.onap.policy.common.message.bus.event.TopicSink;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for sending messages to a Topic using TopicSink.
 */
@Getter
public class TopicSinkClient {
    private static final Logger logger = LoggerFactory.getLogger(TopicSinkClient.class);

    /**
     * Coder used to encode messages being sent to the topic.
     */
    private static final Coder CODER = new StandardCoder();

    /**
     * Where messages are published.
     */
    private final TopicSink sink;

    /**
     * Constructs the object.
     *
     * @param topic topic to which messages should be published
     * @throws TopicSinkClientException if the topic does not exist
     */
    public TopicSinkClient(final String topic) throws TopicSinkClientException {
        final List<TopicSink> lst = getTopicSinks(topic.toLowerCase());
        if (lst.isEmpty()) {
            throw new TopicSinkClientException("no sinks for topic: " + topic.toLowerCase());
        }

        this.sink = lst.get(0);
    }

    /**
     * Constructs the client from a sink object.
     *
     * @param sink topic sink publisher
     */
    public TopicSinkClient(@NonNull TopicSink sink) {
        this.sink = sink;
    }


    /**
     * Gets the canonical topic name.
     *
     * @return topic name
     */
    public String getTopic() {
        return this.sink.getTopic();
    }

    /**
     * Sends a message to the topic, after encoding the message as json.
     *
     * @param message message to be encoded and sent
     * @return {@code true} if the message was successfully sent/enqueued, {@code false} otherwise
     */
    public boolean send(final Object message) {
        try {
            final String json = CODER.encode(message);
            return sink.send(json);

        } catch (RuntimeException | CoderException e) {
            logger.warn("send to {} failed because of {}", sink.getTopic(), e.getMessage(), e);
            return false;
        }
    }

    // the remaining methods are wrappers that can be overridden by junit tests

    /**
     * Gets the sinks for a given topic.
     *
     * @param topic the topic of interest
     * @return the sinks for the topic
     */
    protected List<TopicSink> getTopicSinks(final String topic) {
        return TopicEndpointManager.getManager().getTopicSinks(topic);
    }
}
