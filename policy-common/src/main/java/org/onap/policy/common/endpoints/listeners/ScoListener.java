/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for receipt of a {@link StandardCoderObject}, translating it into an object of
 * the appropriate type, and then passing it to the subclass.
 *
 * @param <T> type of message/POJO this handles
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ScoListener<T> {

    private static final Logger logger = LoggerFactory.getLogger(ScoListener.class);

    /**
     * Used to translate the standard object to an object of type "T".
     */
    private static final Coder coder = new StandardCoder();

    /**
     * Class of message this handles.
     */
    private final Class<T> clazz;

    /**
     * Receives an event, translates it into the desired type of object, and passes it to
     * the subclass.
     *
     * @param infra infrastructure with which the message was received
     * @param topic topic on which the message was received
     * @param sco event that was received
     */
    public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco) {
        // translate the event to the desired object type
        final T msg;
        try {
            msg = coder.fromStandard(sco, clazz);

        } catch (CoderException e) {
            logger.warn("unable to decode {}: {}", clazz.getName(), sco, e);
            return;
        }

        onTopicEvent(infra, topic, sco, msg);
    }

    /**
     * Indicates that a message was received.
     *
     * @param infra infrastructure with which the message was received
     * @param topic topic on which the message was received
     * @param sco event that was received
     * @param message message that was received
     */
    public abstract void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco, T message);
}
