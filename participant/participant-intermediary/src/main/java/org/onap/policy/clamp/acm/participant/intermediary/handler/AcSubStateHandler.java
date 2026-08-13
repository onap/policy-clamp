/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare;
import org.onap.policy.clamp.models.acm.utils.AcmStageUtils;
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
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                migrationMsg.getParticipantDtoList(), cacheProvider.getParticipantId());
        if (elementDtoMap.isEmpty()) {
            return;
        }
        cacheProvider.fillCacheComposition(migrationMsg.getParticipantDtoList());

        var automationComposition = cacheProvider.getAutomationComposition(migrationMsg.getAutomationCompositionId());
        if (automationComposition == null) {
            automationComposition = cacheProvider.createAcInstance(migrationMsg.getCompositionId(),
                    migrationMsg.getCompositionTargetId(), migrationMsg.getAutomationCompositionId(),
                    elementDtoMap, DeployState.MIGRATING, SubState.MIGRATION_PRECHECKING,
                    migrationMsg.getRevisionIdInstance());
            LOGGER.info("New participant with new element type added for Migration Precheck");
        }
        callParticipantMigratePrecheck(migrationMsg.getMessageId(), automationComposition, elementDtoMap);
    }

    private void callParticipantMigratePrecheck(UUID messageId,
            AutomationComposition automationComposition, Map<UUID, AcElementDto> elementDtoMap) {

        for (var dto : elementDtoMap.values()) {
            var instanceElement = ParticipantDtoUtils.resolveInstanceElement(dto);
            var element = automationComposition.getElements()
                    .get(instanceElement.elementId());
            if (element != null) {
                element.setSubState(SubState.MIGRATION_PRECHECKING);
            }
            listener.migratePrecheck(messageId, dto.getCompositionElement(), dto.getCompositionElementTarget(),
                    dto.getInstanceElement(), dto.getInstanceElementTarget());
        }
    }

    /**
     * Handle AutomationComposition Prepare message.
     *
     * @param acPrepareMsg the AutomationCompositionPrepare message
     */
    public void handleAcPrepare(AutomationCompositionPrepare acPrepareMsg) {
        var elementDtoMap = ParticipantDtoUtils.getElementDtoMap(
                acPrepareMsg.getParticipantDtoList(), cacheProvider.getParticipantId());
        cacheProvider.fillCacheComposition(acPrepareMsg.getParticipantDtoList());

        if (acPrepareMsg.isPreDeploy()) {
            cacheProvider.createAcInstance(acPrepareMsg.getCompositionId(), null,
                    acPrepareMsg.getAutomationCompositionId(), elementDtoMap, DeployState.UNDEPLOYED,
                    SubState.PREPARING, acPrepareMsg.getRevisionIdInstance());
            callParticipantPrepare(acPrepareMsg.getMessageId(), acPrepareMsg.getStage(), elementDtoMap);
        } else {
            var automationComposition =
                cacheProvider.getAutomationComposition(acPrepareMsg.getAutomationCompositionId());
            automationComposition.setSubState(SubState.REVIEWING);
            callParticipantReview(acPrepareMsg.getMessageId(), automationComposition, elementDtoMap);
        }
    }

    private void callParticipantPrepare(UUID messageId, Integer stageMsg, Map<UUID, AcElementDto> elementDtoMap) {

        for (var dto : elementDtoMap.values()) {
            var stageSet = AcmStageUtils.findStageSetPrepare(dto.getCompositionElement().inProperties());
            if (stageSet.contains(stageMsg)) {
                listener.prepare(messageId, dto.getCompositionElement(),
                        ParticipantDtoUtils.resolveInstanceElement(dto), stageMsg);
            }
        }
    }

    private void callParticipantReview(UUID messageId, AutomationComposition automationComposition,
            Map<UUID, AcElementDto> elementDtoMap) {

        for (var dto : elementDtoMap.values()) {
            var instanceElement = ParticipantDtoUtils.resolveInstanceElement(dto);
            var element = automationComposition.getElements()
                    .get(instanceElement.elementId());
            element.setSubState(SubState.REVIEWING);
            listener.review(messageId, dto.getCompositionElement(), instanceElement);
        }
    }
}
