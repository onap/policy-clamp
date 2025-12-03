/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2019 Samsung Electronics Co., Ltd.
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

package org.onap.policy.common.message.bus.event.noop;

import java.util.List;
import org.onap.policy.common.message.bus.event.base.TopicBase;
import org.onap.policy.common.message.bus.utils.NetLoggerUtil;
import org.onap.policy.common.message.bus.utils.NetLoggerUtil.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No Operation topic endpoint.
 */
public abstract class NoopTopicEndpoint extends TopicBase {

    /**
     * Logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(NoopTopicEndpoint.class);

    /**
     * Constructs the object.
     */
    protected NoopTopicEndpoint(List<String> servers, String topic) {
        super(servers, topic);
    }

    /**
     *  I/O.
     *
     * @param type "IN" or "OUT".
     * @param message message.
     * @return true if successful.
     */
    protected boolean io(EventType type, String message) {

        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message is empty");
        }

        if (!this.alive) {
            throw new IllegalStateException(this + " is stopped");
        }

        try {
            synchronized (this) {
                this.recentEvents.add(message);
            }

            NetLoggerUtil.log(type, this.getTopicCommInfrastructure(), this.topic, message);

            broadcast(message);
        } catch (Exception e) {
            logger.warn("{}: cannot send because of {}", this, e.getMessage(), e);
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public CommInfrastructure getTopicCommInfrastructure() {
        return CommInfrastructure.NOOP;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean start() {
        logger.info("{}: starting", this);

        synchronized (this) {
            if (!this.alive) {
                if (locked) {
                    throw new IllegalStateException(this + " is locked.");
                }

                this.alive = true;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public boolean stop() {
        logger.info("{}: stopping", this);

        synchronized (this) {
            this.alive = false;
        }
        return true;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public void shutdown() {
        logger.info("{}: shutdown", this);

        this.stop();
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public String toString() {
        return "NoopTopicEndpoint[" + super.toString() + "]";
    }
}
