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

package org.onap.policy.clamp.acm.runtime.supervision;

import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.models.acm.concepts.ParticipantReplica;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to scan the automation compositions in the database and check if they are in the correct state.
 */
@Component
public class SupervisionParticipantScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger(SupervisionParticipantScanner.class);

    private final long maxWaitMs;

    private final ParticipantProvider participantProvider;

    /**
     * Constructor for instantiating SupervisionParticipantScanner.
     *
     * @param participantProvider the Participant Provider
     * @param acRuntimeParameterGroup the parameters for the automation composition runtime
     */
    public SupervisionParticipantScanner(final ParticipantProvider participantProvider,
            final AcRuntimeParameterGroup acRuntimeParameterGroup) {
        this.participantProvider = participantProvider;
        this.maxWaitMs = acRuntimeParameterGroup.getParticipantParameters().getMaxStatusWaitMs();
    }

    /**
     * Run Scanning.
     */
    public void run() {
        LOGGER.debug("Scanning participants in the database . . .");
        participantProvider.findReplicasOnLine().forEach(this::scanParticipantReplicaStatus);
        LOGGER.debug("Participants scan complete . . .");
    }

    private void scanParticipantReplicaStatus(ParticipantReplica replica) {
        var now = TimestampHelper.nowEpochMilli();
        var lastMsg = TimestampHelper.toEpochMilli(replica.getLastMsg());
        if ((now - lastMsg) > maxWaitMs) {
            LOGGER.debug("Participant OFF_LINE {}", replica.getReplicaId());
            participantProvider.deleteParticipantReplica(replica.getReplicaId());
        }
    }
}
