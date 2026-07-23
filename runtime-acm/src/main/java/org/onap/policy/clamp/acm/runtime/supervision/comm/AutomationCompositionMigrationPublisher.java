/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.utils.DtoMapperService;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionRollback;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutomationCompositionMigrationPublisher {

    private final ParticipantPublisher participantPublisher;

    private final DtoMapperService dtoMapperService;

    /**
     * Send AutomationCompositionMigration message to Participant.
     *
     * @param acPriorUpdate AutomationComposition prior update
     * @param automationComposition the AutomationComposition
     * @param stage the stage to execute
     * @param acDefinition AutomationCompositionDefinition
     * @param acDefinitionTarget target AutomationCompositionDefinition
     */
    @Timed(
            value = "publisher.automation_composition_migration",
            description = "AUTOMATION_COMPOSITION_MIGRATION messages published")
    public void send(AutomationCompositionRollback acPriorUpdate, AutomationComposition automationComposition,
                     int stage, AutomationCompositionDefinition acDefinition,
                     AutomationCompositionDefinition acDefinitionTarget, boolean firstStage) {
        var acMigration = new AutomationCompositionMigration();
        var rollback = DeployState.MIGRATION_REVERTING.equals(automationComposition.getDeployState());
        acMigration.setRollback(rollback);
        acMigration.setFirstStage(firstStage);
        acMigration.setPrecheck(Boolean.TRUE.equals(automationComposition.getPrecheck()));
        acMigration.setCompositionId(automationComposition.getCompositionId());
        acMigration.setAutomationCompositionId(automationComposition.getInstanceId());
        acMigration.setMessageId(UUID.randomUUID());
        acMigration.setCompositionTargetId(automationComposition.getCompositionTargetId());
        acMigration.setStage(stage);
        acMigration.setRevisionIdInstance(automationComposition.getRevisionId());
        acMigration.setRevisionIdComposition(acDefinition.getRevisionId());
        acMigration.setRevisionIdCompositionTarget(acDefinitionTarget.getRevisionId());
        var participantUpdatesList = AcmUtils.createParticipantDeployList(automationComposition,
                rollback ? DeployOrder.MIGRATION_REVERT : DeployOrder.MIGRATE);

        // participantUpdateList will be deprecated in future releases
        acMigration.setParticipantUpdatesList(participantUpdatesList);
        acMigration.setParticipantIdList(participantUpdatesList.stream()
                .map(ParticipantDeploy::getParticipantId).collect(Collectors.toSet()));

        var participantDtoList = dtoMapperService.createMigrationDto(acPriorUpdate, acDefinition,
                automationComposition, acDefinitionTarget);
        acMigration.setParticipantDtoList(participantDtoList);

        participantPublisher.send(acMigration);
    }
}
