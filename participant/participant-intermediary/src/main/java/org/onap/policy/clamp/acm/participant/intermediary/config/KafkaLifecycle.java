/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.config;

import org.onap.policy.clamp.acm.participant.intermediary.comm.HeartbeatSender;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.common.acm.kafka.AbstractKafkaLifecycle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

/**
 * Manages the Kafka lifecycle for participant-intermediary.
 *
 * <p>On startup, waits for Kafka, starts listener containers, sends participant
 * registration, and starts the heartbeat scheduler.
 *
 * <p>On shutdown, sends deregistration (best-effort) and stops listener containers.
 */
@Component
public class KafkaLifecycle extends AbstractKafkaLifecycle {

    private final ParticipantHandler participantHandler;
    private final HeartbeatSender heartbeatSender;

    /**
     * Constructor.
     *
     * @param registry the Kafka listener endpoint registry
     * @param kafkaAdmin the ACM Kafka admin bean
     * @param participantHandler handler for registration/deregistration
     * @param heartbeatSender heartbeat message sender
     * @param participantParameters participant configuration parameters
     */
    public KafkaLifecycle(KafkaListenerEndpointRegistry registry,
            @Qualifier("acmKafkaAdmin") KafkaAdmin kafkaAdmin,
            ParticipantHandler participantHandler,
            HeartbeatSender heartbeatSender,
            ParticipantParameters participantParameters) {
        super(registry, kafkaAdmin,
                participantParameters.getIntermediaryParameters().getTopics().getOperationTopic(),
                participantParameters.getIntermediaryParameters().getTopics().getSyncTopic(),
                "acmOperationListener", "acmSyncListener");
        this.participantHandler = participantHandler;
        this.heartbeatSender = heartbeatSender;
    }

    @Override
    protected void onContainersStarted() {
        participantHandler.sendParticipantRegister();
        heartbeatSender.startScheduler();
    }

    @Override
    protected void onContainersStopping() {
        heartbeatSender.close();
        participantHandler.sendParticipantDeregister();
    }
}
