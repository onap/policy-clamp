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

import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.models.base.PfModelException;
import org.springframework.stereotype.Component;

/**
 * This class is used to send ParticipantControlLoopUpdate messages to participants on DMaaP.
 */
@Component
public class ParticipantControlLoopUpdatePublisher extends AbstractParticipantPublisher<ParticipantControlLoopUpdate> {

    private final CommissioningProvider commissioningProvider;

    /**
     * Constructor.
     *
     * @param commissioningProvider the CommissioningProvider
     */
    public ParticipantControlLoopUpdatePublisher(CommissioningProvider commissioningProvider) {
        this.commissioningProvider = commissioningProvider;
    }

    /**
     * Send ControlLoopUpdate to Participant.
     *
     * @param controlLoop the ControlLoop
     * @throws PfModelException on errors getting the Control Loop Definition
     */
    public void send(ControlLoop controlLoop) throws PfModelException {
        var pclu = new ParticipantControlLoopUpdate();
        pclu.setControlLoopId(controlLoop.getKey().asIdentifier());
        pclu.setControlLoop(controlLoop);
        // TODO: We should look up the correct TOSCA node template here for the control loop
        // Tiny hack implemented to return the tosca service template entry from the database and be passed onto dmaap
        pclu.setControlLoopDefinition(commissioningProvider.getToscaServiceTemplate(null, null));
        super.send(pclu);
    }
}
