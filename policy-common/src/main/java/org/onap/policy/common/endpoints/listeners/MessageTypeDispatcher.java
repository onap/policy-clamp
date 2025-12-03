/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.common.endpoints.listeners;

import java.util.concurrent.ConcurrentHashMap;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches standard objects to listeners, based on the message type extracted from the
 * message. Only one listener may be registered for a given type.
 */
public class MessageTypeDispatcher extends JsonListener {
    private static final Logger logger = LoggerFactory.getLogger(MessageTypeDispatcher.class);

    /**
     * Name of the message field, which may be hierarchical.
     */
    private final Object[] messageFieldNames;

    /**
     * Name of the message field, joined with "." - for logging.
     */
    private final String fullMessageFieldName;

    /**
     * Maps a message type to its listener.
     */
    private final ConcurrentHashMap<String, ScoListener<?>> type2listener = new ConcurrentHashMap<>();

    /**
     * Constructs the object.
     *
     * @param messageFieldNames name of the message field, which may be hierarchical
     */
    public MessageTypeDispatcher(String... messageFieldNames) {
        this.messageFieldNames = messageFieldNames;
        this.fullMessageFieldName = String.join(".", messageFieldNames);
    }

    /**
     * Registers a listener for a certain type of message.
     *
     * @param type type of message of interest to the listener
     * @param listener listener to register
     */
    public <T> void register(String type, ScoListener<T> listener) {
        type2listener.put(type, listener);
    }

    /**
     * Unregisters the listener associated with the specified message type.
     *
     * @param type type of message whose listener is to be unregistered
     */
    public void unregister(String type) {
        type2listener.remove(type);
    }

    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco) {
        // extract the message type
        final var type = sco.getString(messageFieldNames);
        if (type == null) {
            logger.warn("unable to extract {}: {}", fullMessageFieldName, sco);
            return;
        }

        // dispatch the message
        ScoListener<?> listener = type2listener.get(type);
        if (listener == null) {
            logger.info("discarding event of type {}", type);
            return;
        }

        listener.onTopicEvent(infra, topic, sco);
    }
}
