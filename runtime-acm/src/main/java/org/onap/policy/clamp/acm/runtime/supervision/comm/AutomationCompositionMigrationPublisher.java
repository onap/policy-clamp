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
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.springframework.stereotype.Component;

@Component
public class AutomationCompositionMigrationPublisher
        extends AbstractParticipantPublisher<AutomationCompositionMigration> {

    /**
     * Send AutomationCompositionMigration message to Participant.
     *
     * @param automationComposition the AutomationComposition
     * @param stage the stage to execute
     * @param revisionIdComposition the last Update from Composition
     * @param revisionIdCompositionTarget the last Update from Composition Target
     */
    @Timed(
            value = "publisher.automation_composition_migration",
            description = "AUTOMATION_COMPOSITION_MIGRATION messages published")
    public void send(AutomationComposition automationComposition, int stage, UUID revisionIdComposition,
                     UUID revisionIdCompositionTarget, boolean fisrtStage) {
        var acMigration = new AutomationCompositionMigration();
        var rollback = DeployState.MIGRATION_REVERTING.equals(automationComposition.getDeployState());
        acMigration.setRollback(rollback);
        acMigration.setFirstStage(fisrtStage);
        acMigration.setPrecheck(Boolean.TRUE.equals(automationComposition.getPrecheck()));
        acMigration.setCompositionId(automationComposition.getCompositionId());
        acMigration.setAutomationCompositionId(automationComposition.getInstanceId());
        acMigration.setMessageId(UUID.randomUUID());
        acMigration.setCompositionTargetId(automationComposition.getCompositionTargetId());
        acMigration.setStage(stage);
        acMigration.setRevisionIdInstance(automationComposition.getRevisionId());
        acMigration.setRevisionIdComposition(revisionIdComposition);
        acMigration.setRevisionIdCompositionTarget(revisionIdCompositionTarget);
        var participantUpdatesList = AcmUtils.createParticipantDeployList(automationComposition,
                rollback ? DeployOrder.MIGRATION_REVERT : DeployOrder.MIGRATE);
        acMigration.setParticipantUpdatesList(participantUpdatesList);
        acMigration.setParticipantIdList(participantUpdatesList.stream()
                .map(ParticipantDeploy::getParticipantId).collect(Collectors.toSet()));

        super.send(acMigration);
    }
}
