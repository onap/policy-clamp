/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.utils.DtoMapperService;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcPreparePublisher {

    private final ParticipantPublisher participantPublisher;

    private final DtoMapperService dtoMapperService;

    /**
     * Send AutomationCompositionPrepare Prepare message to Participant.
     *
     * @param automationComposition the AutomationComposition
     * @param stage the stage
     * @param acDefinition the AutomationCompositionDefinition
     */
    @Timed(value = "publisher.prepare", description = "AC Prepare Pre Deploy published")
    public void sendPrepare(AutomationComposition automationComposition, int stage,
                            AutomationCompositionDefinition acDefinition) {
        var acPrepare = createAutomationCompositionPrepare(automationComposition.getCompositionId(),
            automationComposition.getInstanceId());
        acPrepare.setStage(stage);
        var participantUpdatesList = AcmUtils.createParticipantDeployList(automationComposition, DeployOrder.NONE);
        acPrepare.setParticipantList(participantUpdatesList);
        acPrepare.setParticipantIdList(participantUpdatesList.stream()
                .map(ParticipantDeploy::getParticipantId).collect(Collectors.toSet()));
        acPrepare.setRevisionIdInstance(automationComposition.getRevisionId());
        acPrepare.setRevisionIdComposition(acDefinition.getRevisionId());
        acPrepare.setParticipantDtoList(dtoMapperService.createDtoList(null, automationComposition,
                acDefinition, null));
        participantPublisher.send(acPrepare);
    }

    /**
     * Send AutomationCompositionPrepare Review message to Participant.
     *
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    @Timed(value = "publisher.review", description = "AC Review Post Deploy published")
    public void sendReview(AutomationComposition automationComposition, AutomationCompositionDefinition acDefinition) {
        var acPrepare = createAutomationCompositionPrepare(automationComposition.getCompositionId(),
            automationComposition.getInstanceId());
        acPrepare.setPreDeploy(false);
        acPrepare.setParticipantIdList(automationComposition.getElements().values().stream()
                .map(AutomationCompositionElement::getParticipantId).collect(Collectors.toSet()));
        acPrepare.setRevisionIdComposition(acDefinition.getRevisionId());
        acPrepare.setRevisionIdInstance(automationComposition.getRevisionId());
        acPrepare.setParticipantDtoList(dtoMapperService.createDtoList(null, automationComposition,
                acDefinition, null));
        participantPublisher.send(acPrepare);
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
