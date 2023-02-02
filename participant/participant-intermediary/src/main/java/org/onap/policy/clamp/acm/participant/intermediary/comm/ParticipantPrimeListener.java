/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantPrime;
import org.springframework.stereotype.Component;

/**
 * Listener for Participant Prime messages sent by runtime.
 */
@Component
public class ParticipantPrimeListener extends ParticipantListener<ParticipantPrime> {

    /**
     * Constructs the object.
     *
     * @param participantHandler the handler for managing the state of the participant
     */
    public ParticipantPrimeListener(final ParticipantHandler participantHandler) {
        super(ParticipantPrime.class, participantHandler, participantHandler::handleParticipantPrime);
    }

    @Override
    public String getType() {
        return ParticipantMessageType.PARTICIPANT_PRIME.name();
    }
}
