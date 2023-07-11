/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import java.util.UUID;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.concepts.ParticipantState;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to scan the automation compositions in the database and check if they are in the correct state.
 */
@Component
public class SupervisionPartecipantScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionPartecipantScanner.class);

    private final TimeoutHandler<UUID> participantStatusTimeout = new TimeoutHandler<>();

    private final ParticipantProvider participantProvider;

    /**
     * Constructor for instantiating SupervisionPartecipantScanner.
     *
     * @param participantProvider the Participant Provider
     * @param acRuntimeParameterGroup the parameters for the automation composition runtime
     */
    public SupervisionPartecipantScanner(final ParticipantProvider participantProvider,
            final AcRuntimeParameterGroup acRuntimeParameterGroup) {
        this.participantProvider = participantProvider;

        participantStatusTimeout.setMaxWaitMs(acRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs());
    }

    /**
     * Run Scanning.
     */
    public void run() {
        LOGGER.debug("Scanning participans in the database . . .");

        for (var participant : participantProvider.getParticipants()) {
            scanParticipantStatus(participant);
        }

        LOGGER.debug("Participans scan complete . . .");
    }

    private void scanParticipantStatus(Participant participant) {
        var id = participant.getParticipantId();
        if (participantStatusTimeout.isTimeout(id)) {
            if (ParticipantState.ON_LINE.equals(participant.getParticipantState())) {
                // restart scenario
                LOGGER.debug("Participant is back ON_LINE {}", id);
                participantStatusTimeout.clear(id);
            } else {
                LOGGER.debug("report Participant is still OFF_LINE {}", id);
                return;
            }
        }
        if (participantStatusTimeout.getDuration(id) > participantStatusTimeout.getMaxWaitMs()) {
            LOGGER.debug("report Participant OFF_LINE {}", id);
            participantStatusTimeout.setTimeout(id);
            participant.setParticipantState(ParticipantState.OFF_LINE);
            participantProvider.updateParticipant(participant);
        }
    }

    /**
     * handle participant Status message.
     */
    public void handleParticipantStatus(UUID id) {
        LOGGER.debug("Participant is ON_LINE {}", id);
        participantStatusTimeout.clear(id);
    }
}
