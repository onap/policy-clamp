/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.common.message.bus.event;

/**
 * Marks a given Topic Endpoint as able to send messages over a topic.
 */
public interface TopicSink extends Topic {

    /**
     * Sends a string message over this Topic Endpoint.
     *
     * @param message message to send
     * @return true if the send operation succeeded, false otherwise
     * @throws IllegalArgumentException an invalid message has been provided
     * @throws IllegalStateException    the entity is in a state that prevents
     *         it from sending messages, for example, locked or stopped.
     */
    boolean send(String message);

}
