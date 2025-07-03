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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.AcDefinition;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionMigration;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionPrepare;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcSubStateHandlerTest {

    @Test
    void handleAcStateChangeNullTest() {
        var cacheProvider = mock(CacheProvider.class);
        var ach = new AcSubStateHandler(cacheProvider, mock(ThreadHandler.class));

        var acMigration = new AutomationCompositionMigration();
        acMigration.setPrecheck(true);
        assertDoesNotThrow(() -> ach.handleAcMigrationPrecheck(acMigration));

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        acMigration.setAutomationCompositionId(automationComposition.getInstanceId());
        acMigration.setCompositionTargetId(UUID.randomUUID());
        assertDoesNotThrow(() -> ach.handleAcMigrationPrecheck(acMigration));

        var acPrepare = new AutomationCompositionPrepare();
        assertDoesNotThrow(() -> ach.handleAcPrepare(acPrepare));

        acPrepare.setAutomationCompositionId(automationComposition.getInstanceId());
        assertDoesNotThrow(() -> ach.handleAcPrepare(acPrepare));
    }

    @Test
    void handleAcMigrationPrecheckTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setCompositionId(UUID.randomUUID());
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setCompositionTargetId(UUID.randomUUID());
        var cacheProvider = new CacheProvider(CommonTestData.getParticipantParameters());
        var definitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        cacheProvider.addElementDefinition(automationComposition.getCompositionId(), definitions, UUID.randomUUID());
        cacheProvider.addElementDefinition(
                automationComposition.getCompositionTargetId(), definitions, UUID.randomUUID());
        var participantDeploy =
                CommonTestData.createparticipantDeploy(cacheProvider.getParticipantId(), automationComposition);
        cacheProvider.initializeAutomationComposition(automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), participantDeploy, UUID.randomUUID());
        var migrationMsg = new AutomationCompositionMigration();
        migrationMsg.setStage(0);
        migrationMsg.setCompositionId(automationComposition.getCompositionId());
        migrationMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        migrationMsg.setCompositionTargetId(automationComposition.getCompositionTargetId());
        migrationMsg.setParticipantUpdatesList(List.of(participantDeploy));
        migrationMsg.setPrecheck(true);
        var listener = mock(ThreadHandler.class);
        var ach = new AcSubStateHandler(cacheProvider, listener);
        ach.handleAcMigrationPrecheck(migrationMsg);
        verify(listener, times(automationComposition.getElements().size()))
                .migratePrecheck(any(), any(), any(), any(), any());
    }

    @Test
    void handleAcMigrationPrecheckAddRemoveTest() {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationComposition.setCompositionId(UUID.randomUUID());
        automationComposition.setInstanceId(UUID.randomUUID());
        var cacheProvider = new CacheProvider(CommonTestData.getParticipantParameters());
        var definitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        cacheProvider.addElementDefinition(automationComposition.getCompositionId(), definitions, UUID.randomUUID());
        var participantDeploy =
                CommonTestData.createparticipantDeploy(cacheProvider.getParticipantId(), automationComposition);
        cacheProvider.initializeAutomationComposition(automationComposition.getCompositionId(),
                automationComposition.getInstanceId(), participantDeploy, UUID.randomUUID());

        var acMigrate = new AutomationComposition(automationComposition);
        acMigrate.setCompositionTargetId(UUID.randomUUID());

        // replacing first element with new one
        var element = acMigrate.getElements().values().iterator().next();
        element.setDefinition(new ToscaConceptIdentifier("policy.clamp.new.element", "1.0.0"));
        element.setId(UUID.randomUUID());

        var migrateDefinitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(acMigrate);
        cacheProvider.addElementDefinition(acMigrate.getCompositionTargetId(), migrateDefinitions, UUID.randomUUID());

        var migrationMsg = new AutomationCompositionMigration();
        migrationMsg.setStage(0);
        migrationMsg.setCompositionId(acMigrate.getCompositionId());
        migrationMsg.setAutomationCompositionId(acMigrate.getInstanceId());
        migrationMsg.setCompositionTargetId(acMigrate.getCompositionTargetId());
        var participantMigrate = CommonTestData.createparticipantDeploy(cacheProvider.getParticipantId(), acMigrate);
        migrationMsg.setParticipantUpdatesList(List.of(participantMigrate));
        var listener = mock(ThreadHandler.class);
        var ach = new AcSubStateHandler(cacheProvider, listener);
        ach.handleAcMigrationPrecheck(migrationMsg);
        verify(listener, times(acMigrate.getElements().size() + 1))
                .migratePrecheck(any(), any(), any(), any(), any());
    }

    @Test
    void handlePrepareTest() {
        var listener = mock(ThreadHandler.class);
        var cacheProvider = mock(CacheProvider.class);
        var ach = new AcSubStateHandler(cacheProvider, listener);

        var acPrepareMsg = new AutomationCompositionPrepare();
        acPrepareMsg.setPreDeploy(true);
        acPrepareMsg.setStage(0);
        assertDoesNotThrow(() -> ach.handleAcPrepare(acPrepareMsg));

        acPrepareMsg.setParticipantId(CommonTestData.getParticipantId());
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(CommonTestData.getParticipantId());
        acPrepareMsg.getParticipantList().add(participantDeploy);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        acPrepareMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(automationComposition.getCompositionId());
        for (var element : automationComposition.getElements().values()) {
            var acElementDeploy = new AcElementDeploy();
            acElementDeploy.setProperties(Map.of());
            acElementDeploy.setId(element.getId());
            participantDeploy.getAcElementList().add(acElementDeploy);
            acDefinition.getElements().put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), acDefinition));

        ach.handleAcPrepare(acPrepareMsg);
        verify(listener, times(automationComposition.getElements().size())).prepare(any(), any(), any(), anyInt());
    }

    @Test
    void handleReviewTest() {
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());

        var acPrepareMsg = new AutomationCompositionPrepare();
        acPrepareMsg.setPreDeploy(false);
        acPrepareMsg.setParticipantId(CommonTestData.getParticipantId());

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        acPrepareMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
            .thenReturn(automationComposition);
        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(automationComposition.getCompositionId());
        for (var element : automationComposition.getElements().values()) {
            var acElementDeploy = new AcElementDeploy();
            acElementDeploy.setProperties(Map.of());
            acElementDeploy.setId(element.getId());
            acDefinition.getElements().put(element.getDefinition(), new AutomationCompositionElementDefinition());
        }
        when(cacheProvider.getAcElementsDefinitions())
            .thenReturn(Map.of(automationComposition.getCompositionId(), acDefinition));

        var listener = mock(ThreadHandler.class);
        var ach = new AcSubStateHandler(cacheProvider, listener);
        ach.handleAcPrepare(acPrepareMsg);
        verify(listener, times(automationComposition.getElements().size())).review(any(), any(), any());
    }
}
