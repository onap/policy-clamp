/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionRollback;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.springframework.stereotype.Component;

/**
 * This class is used to send PropertiesUpdate messages to participants.
 */
@Component
@RequiredArgsConstructor
public class AcElementPropertiesPublisher {

    private final ParticipantPublisher participantPublisher;

    private final DtoMapperService dtoMapperService;

    /**
     * Send ACElementPropertiesUpdate to Participant.
     *
     * @param acPriorUpdate AutomationComposition prior update
     * @param automationComposition the AutomationComposition
     * @param acDefinition the AutomationCompositionDefinition
     */
    @Timed(value = "publisher.properties_update", description = "AC Element Properties Update published")
    public void send(AutomationCompositionRollback acPriorUpdate, AutomationComposition automationComposition,
                     AutomationCompositionDefinition acDefinition) {
        var propertiesUpdate = new PropertiesUpdate();
        propertiesUpdate.setCompositionId(automationComposition.getCompositionId());
        propertiesUpdate.setAutomationCompositionId(automationComposition.getInstanceId());
        propertiesUpdate.setMessageId(UUID.randomUUID());
        propertiesUpdate.setTimestamp(Instant.now());
        propertiesUpdate.setRevisionIdInstance(automationComposition.getRevisionId());
        propertiesUpdate.setRevisionIdComposition(acDefinition.getRevisionId());
        var rollback = DeployState.UPDATE_REVERTING.equals(automationComposition.getDeployState());
        propertiesUpdate.setRollback(rollback);
        var participantUpdatesList = AcmUtils.createParticipantDeployList(automationComposition, DeployOrder.UPDATE);
        // ParticipantUpdateList will be deprecated in future releases
        propertiesUpdate.setParticipantUpdatesList(participantUpdatesList);
        propertiesUpdate.setParticipantIdList(participantUpdatesList.stream()
                .map(ParticipantDeploy::getParticipantId).collect(Collectors.toSet()));

        propertiesUpdate.setParticipantDtoList(dtoMapperService.createDtoList(acPriorUpdate,
                automationComposition, acDefinition, null));
        participantPublisher.send(propertiesUpdate);
    }
}
