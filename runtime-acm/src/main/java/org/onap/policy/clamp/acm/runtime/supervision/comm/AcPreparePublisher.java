/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.supervision.comm;

import io.micrometer.core.annotation.Timed;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AcPreparePublisher extends AbstractParticipantPublisher<AutomationCompositionPrepare> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcPreparePublisher.class);

    /**
     * Send AutomationCompositionPrepare Prepare message to Participant.
     *
     * @param automationComposition the AutomationComposition
     */
    @Timed(value = "publisher.prepare", description = "AC Prepare Pre Deploy published")
    public void sendPrepare(AutomationComposition automationComposition) {
        var acPrepare = createAutomationCompositionPrepare(automationComposition.getCompositionId(),
            automationComposition.getInstanceId());
        acPrepare.setParticipantList(
            AcmUtils.createParticipantDeployList(automationComposition, DeployOrder.NONE));
        LOGGER.debug("AC Prepare sent {}", acPrepare);
        super.send(acPrepare);
    }

    /**
     * Send AutomationCompositionPrepare Review message to Participant.
     *
     * @param automationComposition the AutomationComposition
     */
    @Timed(value = "publisher.review", description = "AC Review Post Deploy published")
    public void sendRevew(AutomationComposition automationComposition) {
        var acPrepare = createAutomationCompositionPrepare(automationComposition.getCompositionId(),
            automationComposition.getInstanceId());
        acPrepare.setPreDeploy(false);
        LOGGER.debug("AC Review sent {}", acPrepare);
        super.send(acPrepare);
    }

    private AutomationCompositionPrepare createAutomationCompositionPrepare(UUID compositionId, UUID instanceId) {
        var acPrepare = new AutomationCompositionPrepare();
        acPrepare.setCompositionId(compositionId);
        acPrepare.setAutomationCompositionId(instanceId);
        acPrepare.setMessageId(UUID.randomUUID());
        acPrepare.setTimestamp(Instant.now());
        return acPrepare;
    }
}
