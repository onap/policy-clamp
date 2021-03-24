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

package org.onap.policy.clamp.controlloop.participant.intermediary.comm;

import java.io.Closeable;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantResponseDetails;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantResponseStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class sends messages from participants to CLAMP.
 */
public class MessageSender extends TimerTask implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSender.class);

    private final ParticipantHandler participantHandler;
    private final ParticipantStatusPublisher publisher;
    private ScheduledExecutorService timerPool;

    /**
     * Constructor, set the publisher.
     *
     * @param participantHandler the participant handler to use for gathering information
     * @param publisher the publisher to use for sending messages
     * @param interval time interval to send Participant Status periodic messages
     */
    public MessageSender(ParticipantHandler participantHandler, ParticipantStatusPublisher publisher,
            long interval) {
        this.participantHandler = participantHandler;
        this.publisher = publisher;

        // Kick off the timer
        timerPool = makeTimerPool();
        timerPool.scheduleAtFixedRate(this, 0, interval, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        LOGGER.debug("Sent heartbeat to CLAMP");

        ParticipantResponseDetails response = new ParticipantResponseDetails();

        response.setResponseTo(null);
        response.setResponseStatus(ParticipantResponseStatus.PERIODIC);
        response.setResponseMessage("Periodic response from participant");
    }

    @Override
    public void close() {
        timerPool.shutdown();
    }

    /**
     * Send a response message for this participant.
     *
     * @param response the details to include in the response message
     */
    public void sendResponse(ParticipantResponseDetails response) {
        sendResponse(null, response);
    }

    /**
     * Send a response message for this participant.
     *
     * @param controlLoopId the control loop to which this message is a response
     * @param response the details to include in the response message
     */
    public void sendResponse(ToscaConceptIdentifier controlLoopId, ParticipantResponseDetails response) {
        ParticipantStatus status = new ParticipantStatus();

        // Participant related fields
        status.setParticipantId(participantHandler.getParticipantId());
        status.setState(participantHandler.getState());
        status.setHealthStatus(participantHandler.getHealthStatus());

        // Control loop related fields
        status.setControlLoopId(controlLoopId);
        status.setControlLoops(participantHandler.getControlLoopHandler().getControlLoops());
        status.setResponse(response);

        publisher.send(status);
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
