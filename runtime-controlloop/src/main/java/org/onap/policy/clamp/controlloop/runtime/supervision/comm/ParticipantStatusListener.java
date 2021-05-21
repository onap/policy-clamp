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

import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStatus;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.onap.policy.common.utils.services.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for ParticipantStatus messages sent by participants.
 */
public class ParticipantStatusListener extends ScoListener<ParticipantStatus> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantStatusListener.class);

    private final SupervisionHandler supervisionHandler = Registry.get(SupervisionHandler.class.getName());

    /**
     * Constructs the object.
     */
    public ParticipantStatusListener() {
        super(ParticipantStatus.class);
    }

    @Override
    public void onTopicEvent(final CommInfrastructure infra, final String topic, final StandardCoderObject sco,
            final ParticipantStatus participantStatusMessage) {
        LOGGER.debug("ParticipantStatus message received from participant - {}", participantStatusMessage);
        supervisionHandler.handleParticipantStatusMessage(participantStatusMessage);
    }
}
