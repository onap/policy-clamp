/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.comm;

import java.io.Closeable;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Periodically sends heartbeat messages from participants to the runtime.
 */
@Component
public class HeartbeatSender extends TimerTask implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeartbeatSender.class);

    private final ParticipantHandler participantHandler;
    private final ScheduledExecutorService timerPool;
    private final long interval;

    /**
     * Constructor, set the publisher.
     *
     * @param participantHandler the participant handler to use for gathering information
     * @param parameters the parameters of the participant
     */
    public HeartbeatSender(ParticipantHandler participantHandler, ParticipantParameters parameters) {
        this.participantHandler = participantHandler;
        this.timerPool = makeTimerPool();
        this.interval = parameters.getIntermediaryParameters().getReportingTimeIntervalMs();
    }

    /**
     * Start the heartbeat scheduler. Called by {@code KafkaLifecycle} after
     * Kafka is available and the participant has been registered.
     */
    public void startScheduler() {
        timerPool.scheduleAtFixedRate(this, interval, interval, TimeUnit.MILLISECONDS);
        LOGGER.info("Heartbeat scheduler started with interval {}ms", interval);
    }

    @Override
    public void run() {
        LOGGER.debug("Sent heartbeat to CLAMP");
        participantHandler.sendHeartbeat();
    }

    @Override
    public void close() {
        timerPool.shutdown();
    }

    /**
     * Makes a new timer pool.
     *
     * @return a new timer pool
     */
    protected ScheduledExecutorService makeTimerPool() {
        return Executors.newScheduledThreadPool(1);
    }
}
