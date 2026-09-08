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

package org.onap.policy.clamp.acm.runtime.config;

import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantStatusReqPublisher;
import org.onap.policy.clamp.common.acm.kafka.AbstractKafkaLifecycle;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

/**
 * Manages the Kafka lifecycle for runtime-acm.
 *
 * <p>On startup, waits for Kafka, starts listener containers, and broadcasts
 * a participant status request so that participants re-register.
 *
 * <p>On shutdown, stops listener containers.
 */
@Component
public class KafkaLifecycle extends AbstractKafkaLifecycle {

    private final ParticipantStatusReqPublisher participantStatusReqPublisher;

    /**
     * Constructor.
     *
     * @param registry the Kafka listener endpoint registry
     * @param kafkaAdmin the Spring Kafka Admin
     * @param participantStatusReqPublisher publisher for participant status requests
     * @param parameterGroup the runtime parameters
     */
    public KafkaLifecycle(KafkaListenerEndpointRegistry registry, KafkaAdmin kafkaAdmin,
            ParticipantStatusReqPublisher participantStatusReqPublisher,
            AcRuntimeParameterGroup parameterGroup) {
        super(registry, kafkaAdmin,
                parameterGroup.getTopics().getOperationTopic(),
                parameterGroup.getTopics().getSyncTopic(),
                "participantMessageListener");
        this.participantStatusReqPublisher = participantStatusReqPublisher;
    }

    @Override
    protected void onContainersStarted() {
        participantStatusReqPublisher.broadcast();
    }
}
