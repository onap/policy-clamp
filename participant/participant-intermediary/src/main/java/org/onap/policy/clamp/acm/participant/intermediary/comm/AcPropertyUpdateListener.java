/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.springframework.stereotype.Component;

/**
 * Listener for AC Element property update messages sent by ACM.
 */
@Component
public class AcPropertyUpdateListener extends ParticipantListener<PropertiesUpdate> {

    /**
     * Constructs the object.
     *
     * @param participantHandler the handler for managing the state of the participant
     */
    public AcPropertyUpdateListener(final ParticipantHandler participantHandler) {
        super(PropertiesUpdate.class, participantHandler,
            participantHandler::handleAcPropertyUpdate);
    }

    @Override
    public String getType() {
        return ParticipantMessageType.PROPERTIES_UPDATE.name();
    }
}
