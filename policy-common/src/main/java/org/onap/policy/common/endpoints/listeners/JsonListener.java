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
import lombok.NoArgsConstructor;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.message.bus.event.TopicListener;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for messages received on a topic, in JSON format, decodes them into a
 * {@link StandardCoderObject}, and then offers the objects to the subclass.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class JsonListener implements TopicListener {
    private static final Logger logger = LoggerFactory.getLogger(JsonListener.class);

    /**
     * Used to decode the event.
     */
    private static final Coder coder = new StandardCoder();

    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, String event) {
        // decode from JSON into a standard object
        StandardCoderObject sco;
        try {
            sco = coder.decode(event, StandardCoderObject.class);

        } catch (CoderException e) {
            logger.warn("unable to decode: {}", event, e);
            return;
        }

        onTopicEvent(infra, topic, sco);
    }

    /**
     * Indicates that a standard object was received.
     *
     * @param infra infrastructure with which the message was received
     * @param topic topic on which the message was received
     * @param sco the standard object that was received
     */
    public abstract void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco);
}
