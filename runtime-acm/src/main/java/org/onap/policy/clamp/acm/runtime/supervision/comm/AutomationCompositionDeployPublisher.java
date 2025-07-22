/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021,2023-2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class is used to send AutomationCompositionDeploy messages to participants on Kafka.
 */
@Component
@AllArgsConstructor
public class AutomationCompositionDeployPublisher extends AbstractParticipantPublisher<AutomationCompositionDeploy> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionDeployPublisher.class);

    /**
     * Send AutomationCompositionDeploy to Participant.
     *
     * @param automationComposition the AutomationComposition
     * @param startPhase the Start Phase
     * @param firstStartPhase true if the first StartPhase
     * @param revisionIdComposition the last Update from Composition
     */
    @Timed(value = "publisher.automation_composition_deploy",
            description = "AUTOMATION_COMPOSITION_DEPLOY messages published")
    public void send(AutomationComposition automationComposition, int startPhase, boolean firstStartPhase,
            UUID revisionIdComposition) {
        Map<UUID, List<AcElementDeploy>> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            var acElementDeploy = AcmUtils.createAcElementDeploy(element, DeployOrder.DEPLOY);
            map.putIfAbsent(element.getParticipantId(), new ArrayList<>());
            map.get(element.getParticipantId()).add(acElementDeploy);
        }
        List<ParticipantDeploy> participantDeploys = new ArrayList<>();
        var acDeployMsg = new AutomationCompositionDeploy();
        for (var entry : map.entrySet()) {
            var participantDeploy = new ParticipantDeploy();
            participantDeploy.setParticipantId(entry.getKey());
            acDeployMsg.getParticipantIdList().add(entry.getKey());
            participantDeploy.setAcElementList(entry.getValue());
            participantDeploys.add(participantDeploy);
        }

        acDeployMsg.setCompositionId(automationComposition.getCompositionId());
        acDeployMsg.setStartPhase(startPhase);
        acDeployMsg.setFirstStartPhase(firstStartPhase);
        acDeployMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        acDeployMsg.setMessageId(UUID.randomUUID());
        acDeployMsg.setTimestamp(Instant.now());
        acDeployMsg.setRevisionIdInstance(automationComposition.getRevisionId());
        acDeployMsg.setRevisionIdComposition(revisionIdComposition);
        acDeployMsg.setParticipantUpdatesList(participantDeploys);

        LOGGER.debug("AutomationCompositionDeploy message sent {}", acDeployMsg.getMessageId());
        super.send(acDeployMsg);
    }
}
