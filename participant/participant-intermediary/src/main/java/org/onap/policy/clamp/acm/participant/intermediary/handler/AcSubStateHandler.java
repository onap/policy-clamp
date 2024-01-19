/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AcSubStateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcSubStateHandler.class);

    private final CacheProvider cacheProvider;
    private final ThreadHandler listener;

    /**
     * Handles AutomationComposition Migration Precheck.
     *
     * @param migrationMsg the AutomationCompositionMigration
     */
    public void handleAcMigrationPrecheck(AutomationCompositionMigration migrationMsg) {
        if (migrationMsg.getAutomationCompositionId() == null || migrationMsg.getCompositionTargetId() == null) {
            return;
        }

        var automationComposition = cacheProvider.getAutomationComposition(migrationMsg.getAutomationCompositionId());
        if (automationComposition == null) {
            LOGGER.debug("Automation composition {} does not use this participant",
                    migrationMsg.getAutomationCompositionId());
            return;
        }
        automationComposition.setSubState(SubState.MIGRATION_PRECHECKING);
        for (var participantDeploy : migrationMsg.getParticipantUpdatesList()) {
            if (cacheProvider.getParticipantId().equals(participantDeploy.getParticipantId())) {

                callParticipantMigratePrecheck(migrationMsg.getMessageId(), participantDeploy.getAcElementList(),
                    automationComposition, migrationMsg.getCompositionTargetId());
            }
        }
    }

    private void callParticipantMigratePrecheck(UUID messageId, List<AcElementDeploy> acElements,
            AutomationComposition automationComposition, UUID compositionTargetId) {
        var compositionElementMap = cacheProvider.getCompositionElementDtoMap(automationComposition);
        var instanceElementMap = cacheProvider.getInstanceElementDtoMap(automationComposition);
        var acElementList = automationComposition.getElements();
        for (var acElement : acElements) {
            var element = acElementList.get(acElement.getId());
            element.setSubState(SubState.MIGRATION_PRECHECKING);
        }
        var acCopyMigrateTo = new AutomationComposition(automationComposition);
        acElementList = acCopyMigrateTo.getElements();
        for (var acElement : acElements) {
            var element = acElementList.get(acElement.getId());
            AcmUtils.recursiveMerge(element.getProperties(), acElement.getProperties());
            element.setDefinition(acElement.getDefinition());
        }

        var compositionElementTargetMap = cacheProvider.getCompositionElementDtoMap(acCopyMigrateTo,
            compositionTargetId);
        var instanceElementMigrateMap = cacheProvider.getInstanceElementDtoMap(acCopyMigrateTo);

        for (var acElement : acElements) {
            listener.migratePrecheck(messageId, compositionElementMap.get(acElement.getId()),
                compositionElementTargetMap.get(acElement.getId()),
                instanceElementMap.get(acElement.getId()), instanceElementMigrateMap.get(acElement.getId()));
        }
    }

    /**
     * Handle AutomationComposition Prepare message.
     *
     * @param acPrepareMsg the AutomationCompositionPrepare message
     */
    public void handleAcPrepare(AutomationCompositionPrepare acPrepareMsg) {
        if (acPrepareMsg.isPreDeploy()) {
            for (var participantPrepare : acPrepareMsg.getParticipantList()) {
                if (cacheProvider.getParticipantId().equals(participantPrepare.getParticipantId())) {
                    cacheProvider.initializeAutomationComposition(acPrepareMsg.getCompositionId(),
                        acPrepareMsg.getAutomationCompositionId(), participantPrepare, DeployState.UNDEPLOYED,
                        SubState.PREPARING);
                    callParticipanPrepare(acPrepareMsg.getMessageId(), participantPrepare.getAcElementList(),
                        acPrepareMsg.getAutomationCompositionId());
                }
            }
        } else {
            var automationComposition =
                cacheProvider.getAutomationComposition(acPrepareMsg.getAutomationCompositionId());
            automationComposition.setSubState(SubState.REVIEWING);
            callParticipanReview(acPrepareMsg.getMessageId(), automationComposition);
        }
    }

    private void callParticipanPrepare(UUID messageId, List<AcElementDeploy> acElementList, UUID instanceId) {
        var automationComposition = cacheProvider.getAutomationComposition(instanceId);
        for (var elementDeploy : acElementList) {
            var element = automationComposition.getElements().get(elementDeploy.getId());
            var compositionInProperties = cacheProvider
                .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            var compositionElement = cacheProvider.createCompositionElementDto(automationComposition.getCompositionId(),
                element, compositionInProperties);
            var instanceElement = new InstanceElementDto(instanceId, elementDeploy.getId(),
                elementDeploy.getToscaServiceTemplateFragment(),
                elementDeploy.getProperties(), element.getOutProperties());
            listener.prepare(messageId, compositionElement, instanceElement);
        }
    }

    private void callParticipanReview(UUID messageId, AutomationComposition automationComposition) {
        for (var element : automationComposition.getElements().values()) {
            var compositionInProperties = cacheProvider
                .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            element.setSubState(SubState.REVIEWING);
            var compositionElement = cacheProvider.createCompositionElementDto(automationComposition.getCompositionId(),
                element, compositionInProperties);
            var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                null, element.getProperties(), element.getOutProperties());
            listener.review(messageId, compositionElement, instanceElement);
        }
    }
}
