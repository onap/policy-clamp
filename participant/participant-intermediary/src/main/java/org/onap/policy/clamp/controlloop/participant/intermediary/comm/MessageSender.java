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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopAck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantRegister;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantUpdateAck;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class sends messages from participants to CLAMP.
 */
public class MessageSender extends TimerTask implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSender.class);

    private final ParticipantHandler participantHandler;
    private final ParticipantMessagePublisher publisher;
    private ScheduledExecutorService timerPool;

    /**
     * Constructor, set the publisher.
     *
     * @param participantHandler the participant handler to use for gathering information
     * @param publisher the publisher to use for sending messages
     * @param interval time interval to send Participant Status periodic messages
     */
    public MessageSender(ParticipantHandler participantHandler, ParticipantMessagePublisher publisher,
            long interval) {
        this.participantHandler = participantHandler;
        this.publisher = publisher;

        // Kick off the timer
        timerPool = makeTimerPool();
        timerPool.scheduleAtFixedRate(this, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        LOGGER.debug("Sent heartbeat to CLAMP");
        this.sendHeartbeat();
    }

    @Override
    public void close() {
        timerPool.shutdown();
    }

    /**
     * Send a response message for this participant.
     *
     * @param ackMessage the details to include in the response message
     */
    public void sendAckResponse(ControlLoopAck ackMessage) {
        sendAckResponse(null, ackMessage);
    }

    /**
     * Dispatch a response message for this participant.
     *
     * @param controlLoopId the control loop to which this message is a response
     * @param ackMessage the details to include in the response message
     */
    public void sendAckResponse(ToscaConceptIdentifier controlLoopId, ControlLoopAck ackMessage) {
        // Participant related fields
        ackMessage.setParticipantType(participantHandler.getParticipantType());
        ackMessage.setParticipantId(participantHandler.getParticipantId());
        publisher.sendControlLoopAck(ackMessage);
    }

    /**
     * Send a ParticipantRegister message for this participant.
     *
     * @param message the participantRegister message
     */
    public void sendParticipantRegister(ParticipantRegister message) {
        publisher.sendParticipantRegister(message);
    }

    /**
     * Send a ParticipantDeregister message for this participant.
     *
     * @param message the participantDeRegister message
     */
    public void sendParticipantDeregister(ParticipantDeregister message) {
        publisher.sendParticipantDeregister(message);
    }

    /**
     * Send a ParticipantUpdateAck message for this participant update.
     *
     * @param message the participantUpdateAck message
     */
    public void sendParticipantUpdateAck(ParticipantUpdateAck message) {
        publisher.sendParticipantUpdateAck(message);
    }

    /**
     * Send a ParticipantStatus message for this participant.
     *
     * @param participantStatus the ParticipantStatus message
     */
    public void sendParticipantStatus(ParticipantStatus participantStatus) {
        var controlLoops = participantHandler.getControlLoopHandler().getControlLoops();
        for (ControlLoopElementListener clElementListener :
            participantHandler.getControlLoopHandler().getListeners()) {
            updateClElementStatistics(controlLoops, clElementListener);
        }

        publisher.sendParticipantStatus(participantStatus);
    }

    /**
     * Dispatch a heartbeat for this participant.
     */
    public void sendHeartbeat() {
        publisher.sendHeartbeat(participantHandler.makeHeartbeat(false));
    }

    /**
     * Update ControlLoopElement statistics. The control loop elements listening will be
     * notified to retrieve statistics from respective controlloop elements, and controlloopelements
     * data on the handler will be updated.
     *
     * @param controlLoops the control loops
     * @param clElementListener control loop element listener
     */
    public void updateClElementStatistics(ControlLoops controlLoops, ControlLoopElementListener clElementListener) {
        for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {
            for (ControlLoopElement element : controlLoop.getElements().values()) {
                try {
                    clElementListener.handleStatistics(element.getId());
                } catch (PfModelException e) {
                    LOGGER.debug("Getting statistics for Control loop element failed");
                }
            }
        }
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
