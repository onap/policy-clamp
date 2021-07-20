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

import lombok.AllArgsConstructor;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to send ParticipantControlLoopUpdate messages to participants on DMaaP.
 */
@Component
@AllArgsConstructor
public class ParticipantControlLoopUpdatePublisher extends AbstractParticipantPublisher<ParticipantControlLoopUpdate> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticipantControlLoopUpdatePublisher.class);

    private final CommissioningProvider commissioningProvider;

    /**
     * Send ControlLoopUpdate to Participant.
     *
     * @param controlLoop the ControlLoop
     * @throws PfModelException on errors getting the Control Loop Definition
     */
    public void send(ControlLoop controlLoop) {
        var pclu = new ParticipantControlLoopUpdate();
        pclu.setControlLoopId(controlLoop.getKey().asIdentifier());
        pclu.setControlLoop(controlLoop);
        // TODO: We should look up the correct TOSCA node template here for the control loop
        // Tiny hack implemented to return the tosca service template entry from the database and be passed onto dmaap
        try {
            pclu.setControlLoopDefinition(commissioningProvider.getToscaServiceTemplate(null, null));
        } catch (PfModelException pfme) {
            LOGGER.warn("Get of tosca service template failed, cannot send ParticipantControlLoopUpdate", pfme);
            return;
        }
        super.send(pclu);
    }
}
