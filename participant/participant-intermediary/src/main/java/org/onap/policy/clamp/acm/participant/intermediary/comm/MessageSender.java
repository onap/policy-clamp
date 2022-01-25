/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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
 * This class sends messages from participants to CLAMP.
 */
@Component
public class MessageSender extends TimerTask implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSender.class);

    private final ParticipantHandler participantHandler;
    private ScheduledExecutorService timerPool;

    /**
     * Constructor, set the publisher.
     *
     * @param participantHandler the participant handler to use for gathering information
     * @param parameters the parameters of the participant
     */
    public MessageSender(ParticipantHandler participantHandler, ParticipantParameters parameters) {
        this.participantHandler = participantHandler;

        // Kick off the timer
        timerPool = makeTimerPool();
        var interval = parameters.getIntermediaryParameters().getReportingTimeIntervalMs();
        timerPool.scheduleAtFixedRate(this, interval, interval, TimeUnit.MILLISECONDS);
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
