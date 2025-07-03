/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementState;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
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
            if (element != null) {
                element.setSubState(SubState.MIGRATION_PRECHECKING);
            }
        }
        var acCopyMigrateTo = new AutomationComposition(automationComposition);
        var acElementCopyList = acCopyMigrateTo.getElements();
        for (var acElement : acElements) {
            var element = acElementCopyList.get(acElement.getId());
            if (element != null) {
                AcmUtils.recursiveMerge(element.getProperties(), acElement.getProperties());
                element.setDefinition(acElement.getDefinition());
            } else {
                element = CacheProvider.createAutomationCompositionElement(acElement);
                element.setSubState(SubState.MIGRATION_PRECHECKING);
                acElementCopyList.put(element.getId(), element);
            }
        }
        var toDelete = acElementCopyList.values().stream()
                .filter(el -> !SubState.MIGRATION_PRECHECKING.equals(el.getSubState()))
                .map(AutomationCompositionElement::getId)
                .toList();
        toDelete.forEach(acElementCopyList::remove);

        var compositionElementTargetMap = cacheProvider.getCompositionElementDtoMap(acCopyMigrateTo,
            compositionTargetId);
        var instanceElementMigrateMap = cacheProvider.getInstanceElementDtoMap(acCopyMigrateTo);

        for (var acElement : acElements) {
            var compositionElement = compositionElementMap.get(acElement.getId());
            var compositionElementTarget = compositionElementTargetMap.get(acElement.getId());
            var instanceElement = instanceElementMap.get(acElement.getId());
            var instanceElementMigrate = instanceElementMigrateMap.get(acElement.getId());

            if (instanceElement == null) {
                // new element scenario
                compositionElement = new CompositionElementDto(automationComposition.getCompositionId(),
                        acElement.getDefinition(), Map.of(), Map.of(), ElementState.NOT_PRESENT);
                instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), acElement.getId(),
                        Map.of(), Map.of(), ElementState.NOT_PRESENT);
                compositionElementTarget = CacheProvider.changeStateToNew(compositionElementTarget);
                instanceElementMigrate = CacheProvider.changeStateToNew(instanceElementMigrate);
            }

            listener.migratePrecheck(messageId, compositionElement, compositionElementTarget,
                    instanceElement, instanceElementMigrate);
        }

        for (var elementId : toDelete) {
            var compositionDtoTarget =
                    new CompositionElementDto(compositionTargetId,
                            automationComposition.getElements().get(elementId).getDefinition(),
                            Map.of(), Map.of(), ElementState.REMOVED);
            var instanceDtoTarget =
                    new InstanceElementDto(automationComposition.getInstanceId(), elementId,
                            Map.of(), Map.of(), ElementState.REMOVED);

            listener.migratePrecheck(messageId, compositionElementMap.get(elementId), compositionDtoTarget,
                    instanceElementMap.get(elementId), instanceDtoTarget);
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
                        SubState.PREPARING, acPrepareMsg.getRevisionIdInstance());
                    callParticipanPrepare(acPrepareMsg.getMessageId(), participantPrepare.getAcElementList(),
                        acPrepareMsg.getStage(), acPrepareMsg.getAutomationCompositionId());
                }
            }
        } else {
            var automationComposition =
                cacheProvider.getAutomationComposition(acPrepareMsg.getAutomationCompositionId());
            automationComposition.setSubState(SubState.REVIEWING);
            callParticipanReview(acPrepareMsg.getMessageId(), automationComposition);
        }
    }

    private void callParticipanPrepare(UUID messageId, List<AcElementDeploy> acElementList, Integer stageMsg,
            UUID instanceId) {
        var automationComposition = cacheProvider.getAutomationComposition(instanceId);
        for (var elementDeploy : acElementList) {
            var element = automationComposition.getElements().get(elementDeploy.getId());
            var compositionInProperties = cacheProvider
                .getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
            var compositionElement = cacheProvider.createCompositionElementDto(automationComposition.getCompositionId(),
                element);
            var stageSet = ParticipantUtils.findStageSetPrepare(compositionInProperties);
            if (stageSet.contains(stageMsg)) {
                var instanceElement =
                        new InstanceElementDto(instanceId, elementDeploy.getId(), elementDeploy.getProperties(),
                                element.getOutProperties());
                listener.prepare(messageId, compositionElement, instanceElement, stageMsg);
            }
        }
    }

    private void callParticipanReview(UUID messageId, AutomationComposition automationComposition) {
        for (var element : automationComposition.getElements().values()) {
            element.setSubState(SubState.REVIEWING);
            var compositionElement = cacheProvider.createCompositionElementDto(automationComposition.getCompositionId(),
                element);
            var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                element.getProperties(), element.getOutProperties());
            listener.review(messageId, compositionElement, instanceElement);
        }
    }
}
