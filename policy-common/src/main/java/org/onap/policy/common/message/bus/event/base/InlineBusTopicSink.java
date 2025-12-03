/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2017-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2018-2019 Samsung Electronics Co., Ltd.
 * Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

package org.onap.policy.common.message.bus.event.base;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.common.message.bus.utils.NetLoggerUtil;
import org.onap.policy.common.message.bus.utils.NetLoggerUtil.EventType;
import org.onap.policy.common.parameters.topic.BusTopicParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport Agnostic Bus Topic Sink to carry out the core functionality to interact with a sink.
 *
 */
public abstract class InlineBusTopicSink extends BusTopicBase implements BusTopicSink {

    /**
     * Loggers.
     */
    private static final Logger logger = LoggerFactory.getLogger(InlineBusTopicSink.class);

    /**
     * The partition key to publish to.
     */
    @Getter
    @Setter
    protected String partitionKey;

    /**
     * Message bus publisher.
     */
    protected BusPublisher publisher;

    /**
     * Constructor for abstract sink.
     * @param busTopicParams contains below listed attributes
     *     servers: servers
     *     topic: topic
     *     apiKey: api secret
     *     apiSecret: api secret
     *     partitionId: partition id
     *     useHttps: does connection use HTTPS?
     *     allowTracing: is tracing allowed?
     *     allowSelfSignedCerts: are self-signed certificates allow     *
     * @throws IllegalArgumentException if invalid parameters are passed in
     */
    protected InlineBusTopicSink(BusTopicParams busTopicParams) {

        super(busTopicParams);

        if (busTopicParams.isPartitionIdInvalid()) {
            this.partitionKey = UUID.randomUUID().toString();
        } else {
            this.partitionKey = busTopicParams.getPartitionId();
        }
    }

    /**
     * Initialize the Bus publisher.
     */
    public abstract void init();

    @Override
    public boolean start() {
        logger.info("{}: starting", this);

        synchronized (this) {
            if (!this.alive) {
                if (locked) {
                    throw new IllegalStateException(this + " is locked.");
                }

                this.init();
                this.alive = true;
            }
        }

        return true;
    }

    @Override
    public boolean stop() {

        BusPublisher publisherCopy;
        synchronized (this) {
            this.alive = false;
            publisherCopy = this.publisher;
            this.publisher = null;
        }

        if (publisherCopy != null) {
            try {
                publisherCopy.close();
            } catch (Exception e) {
                logger.warn("{}: cannot stop publisher because of {}", this, e.getMessage(), e);
            }
        } else {
            logger.warn("{}: there is no publisher", this);
            return false;
        }

        return true;
    }

    @Override
    public boolean send(String message) {

        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Message to send is empty");
        }

        if (!this.alive) {
            throw new IllegalStateException(this + " is stopped");
        }

        try {
            synchronized (this) {
                this.recentEvents.add(message);
            }

            NetLoggerUtil.log(EventType.OUT, this.getTopicCommInfrastructure(), this.topic, message);

            publisher.send(this.partitionKey, message);
            broadcast(message);
        } catch (Exception e) {
            logger.warn("{}: cannot send because of {}", this, e.getMessage(), e);
            return false;
        }

        return true;
    }

    @Override
    public void shutdown() {
        this.stop();
    }

    @Override
    public String toString() {
        return "InlineBusTopicSink [partitionId=" + partitionKey + ", alive=" + alive + ", publisher=" + publisher
                        + "]";
    }
}
