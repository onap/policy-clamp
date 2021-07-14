/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.supervision.comm;

import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregister;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for ParticipantDeregister messages sent by participants.
 */
public class ParticipantDeregisterListener extends ScoListener<ParticipantDeregister> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantDeregisterListener.class);

    private final SupervisionHandler supervisionHandler;

    /**
     * Constructs the object.
     */
    public ParticipantDeregisterListener(SupervisionHandler supervisionHandler) {
        super(ParticipantDeregister.class);
        this.supervisionHandler = supervisionHandler;
    }

    @Override
    public void onTopicEvent(final CommInfrastructure infra, final String topic, final StandardCoderObject sco,
            final ParticipantDeregister participantDeregisterMessage) {
        LOGGER.debug("ParticipantDeregister message received from participant - {}", participantDeregisterMessage);
        supervisionHandler.handleParticipantMessage(participantDeregisterMessage);
    }
}
