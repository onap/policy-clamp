/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.AutomationCompositionMsg;
import org.onap.policy.clamp.acm.participant.intermediary.handler.cache.CacheProvider;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;

class MsgExecutorTest {

    @Test
    void testExecute() {
        var parameters = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameters);
        var publisher = mock(ParticipantMessagePublisher.class);
        var msgExecutor = new MsgExecutor(cacheProvider, publisher);
        var automationCompositionHandler = mock(AutomationCompositionHandler.class);
        var updateMsg = new AutomationCompositionDeploy();
        var acMsg = new AutomationCompositionMsg<>(
                automationCompositionHandler::handleAutomationCompositionDeploy, updateMsg);
        msgExecutor.execute(acMsg);
        verify(automationCompositionHandler).handleAutomationCompositionDeploy(updateMsg);
    }

    @Test
    void testExecuteCompositionOutdated() {
        var parameters = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameters);
        var publisher = mock(ParticipantMessagePublisher.class);
        var msgExecutor = new MsgExecutor(cacheProvider, publisher);
        var automationCompositionHandler = mock(AutomationCompositionHandler.class);
        var updateMsg = new AutomationCompositionDeploy();
        var acMsg = new AutomationCompositionMsg<>(
                automationCompositionHandler::handleAutomationCompositionDeploy, updateMsg);
        var compositionId = UUID.randomUUID();
        acMsg.setCompositionId(compositionId);
        var revisionIdComposition = UUID.randomUUID();
        acMsg.setRevisionIdComposition(revisionIdComposition);
        msgExecutor.execute(acMsg);
        verify(automationCompositionHandler, times(0)).handleAutomationCompositionDeploy(updateMsg);
        verify(publisher).sendParticipantReqSync(any());
        assertThat(cacheProvider.getMessagesOnHold()).hasSize(1);

        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setInstanceId(UUID.randomUUID());
        automationComposition.setCompositionId(compositionId);
        var definitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        cacheProvider.addElementDefinition(compositionId, definitions, revisionIdComposition);
        msgExecutor.check();
        verify(automationCompositionHandler, timeout(100)).handleAutomationCompositionDeploy(updateMsg);
        assertThat(cacheProvider.getMessagesOnHold()).isEmpty();
    }

    @Test
    void testCheckAndExecuteInstance() {
        var automationCompositionHandler = mock(AutomationCompositionHandler.class);
        var stateChangeMsg = new AutomationCompositionStateChange();
        var acMsg = new AutomationCompositionMsg<>(
                automationCompositionHandler::handleAutomationCompositionStateChange, stateChangeMsg);
        var compositionId = UUID.randomUUID();
        acMsg.setCompositionId(compositionId);
        var revisionIdComposition = UUID.randomUUID();
        acMsg.setRevisionIdComposition(revisionIdComposition);
        var instanceId = UUID.randomUUID();
        acMsg.setInstanceId(instanceId);
        acMsg.setRevisionIdInstance(UUID.randomUUID());

        var automationComposition =
                CommonTestData.getTestAutomationCompositions().getAutomationCompositionList().get(0);
        automationComposition.setInstanceId(instanceId);
        automationComposition.setCompositionId(compositionId);
        var definitions =
                CommonTestData.createAutomationCompositionElementDefinitionList(automationComposition);
        var parameters = CommonTestData.getParticipantParameters();
        var cacheProvider = new CacheProvider(parameters);
        cacheProvider.addElementDefinition(compositionId, definitions, revisionIdComposition);

        var publisher = mock(ParticipantMessagePublisher.class);
        var msgExecutor = new MsgExecutor(cacheProvider, publisher);
        msgExecutor.execute(acMsg);
        verify(automationCompositionHandler, times(0)).handleAutomationCompositionStateChange(stateChangeMsg);
        verify(publisher).sendParticipantReqSync(any());
        assertThat(cacheProvider.getMessagesOnHold()).hasSize(1);

        var participantDeploy =
                CommonTestData.createparticipantDeploy(cacheProvider.getParticipantId(), automationComposition);
        cacheProvider.initializeAutomationComposition(compositionId, automationComposition.getInstanceId(),
                participantDeploy, acMsg.getRevisionIdInstance());
        msgExecutor.check();
        verify(automationCompositionHandler, timeout(100)).handleAutomationCompositionStateChange(stateChangeMsg);
        assertThat(cacheProvider.getMessagesOnHold()).isEmpty();
    }
}
