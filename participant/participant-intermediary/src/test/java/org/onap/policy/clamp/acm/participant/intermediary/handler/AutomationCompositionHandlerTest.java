/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeploy;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.PropertiesUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.models.base.PfModelException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AutomationCompositionHandlerTest {

    @Test
    void handleAutomationCompositionStateChangeNullTest() {
        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var cacheProvider = mock(CacheProvider.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher,
                mock(AutomationCompositionElementListener.class));

        var automationCompositionStateChange = new AutomationCompositionStateChange();
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(automationCompositionStateChange));

        automationCompositionStateChange.setAutomationCompositionId(UUID.randomUUID());
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(automationCompositionStateChange));
        verify(participantMessagePublisher).sendAutomationCompositionAck(any(AutomationCompositionDeployAck.class));

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        automationCompositionStateChange.setAutomationCompositionId(automationComposition.getInstanceId());
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        automationCompositionStateChange.setDeployOrderedState(DeployOrder.UPDATE);
        assertDoesNotThrow(() -> ach.handleAutomationCompositionStateChange(automationCompositionStateChange));
    }

    @Test
    void handleAutomationCompositionStateChangeUndeployTest() throws PfModelException {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
                automationComposition.getInstanceId(), DeployOrder.UNDEPLOY, LockOrder.NONE);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(AutomationCompositionElementListener.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(automationComposition.getElements().size())).undeploy(any(), any());
    }

    @Test
    void handleAutomationCompositionStateChangeLockTest() throws PfModelException {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
                automationComposition.getInstanceId(), DeployOrder.NONE, LockOrder.LOCK);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(AutomationCompositionElementListener.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(automationComposition.getElements().size())).lock(any(), any());
    }

    @Test
    void handleAutomationCompositionStateChangeUnlockTest() throws PfModelException {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
                automationComposition.getInstanceId(), DeployOrder.NONE, LockOrder.UNLOCK);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(AutomationCompositionElementListener.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(automationComposition.getElements().size())).unlock(any(), any());
    }

    @Test
    void handleAutomationCompositionStateChangeDeleteTest() throws PfModelException {
        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        var automationCompositionStateChange = CommonTestData.getStateChange(CommonTestData.getParticipantId(),
                automationComposition.getInstanceId(), DeployOrder.DELETE, LockOrder.NONE);
        var cacheProvider = mock(CacheProvider.class);
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        when(cacheProvider.getCommonProperties(any(UUID.class), any(UUID.class))).thenReturn(Map.of());

        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var listener = mock(AutomationCompositionElementListener.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);
        ach.handleAutomationCompositionStateChange(automationCompositionStateChange);
        verify(listener, times(automationComposition.getElements().size())).delete(any(), any());
    }

    @Test
    void handleAcPropertyUpdateTest() throws PfModelException {
        var cacheProvider = mock(CacheProvider.class);
        var listener = mock(AutomationCompositionElementListener.class);
        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);

        var updateMsg = new PropertiesUpdate();
        assertDoesNotThrow(() -> ach.handleAcPropertyUpdate(updateMsg));

        updateMsg.setParticipantId(CommonTestData.getParticipantId());
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(CommonTestData.getParticipantId());
        updateMsg.getParticipantUpdatesList().add(participantDeploy);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        updateMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        var acElementDeploy = new AcElementDeploy();
        acElementDeploy.setProperties(Map.of());
        acElementDeploy.setId(automationComposition.getElements().values().iterator().next().getId());
        participantDeploy.getAcElementList().add(acElementDeploy);

        ach.handleAcPropertyUpdate(updateMsg);
        verify(listener).update(any(), any(), any());
    }

    @Test
    void handleAutomationCompositionDeployTest() throws PfModelException {
        var cacheProvider = mock(CacheProvider.class);
        var listener = mock(AutomationCompositionElementListener.class);
        var participantMessagePublisher = mock(ParticipantMessagePublisher.class);
        var ach = new AutomationCompositionHandler(cacheProvider, participantMessagePublisher, listener);

        var deployMsg = new AutomationCompositionDeploy();
        assertDoesNotThrow(() -> ach.handleAutomationCompositionDeploy(deployMsg));

        deployMsg.setParticipantId(CommonTestData.getParticipantId());
        when(cacheProvider.getParticipantId()).thenReturn(CommonTestData.getParticipantId());
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(CommonTestData.getParticipantId());
        deployMsg.getParticipantUpdatesList().add(participantDeploy);

        var automationComposition = CommonTestData.getTestAutomationCompositionMap().values().iterator().next();
        deployMsg.setAutomationCompositionId(automationComposition.getInstanceId());
        when(cacheProvider.getAutomationComposition(automationComposition.getInstanceId()))
                .thenReturn(automationComposition);
        for (var element : automationComposition.getElements().values()) {
            var acElementDeploy = new AcElementDeploy();
            acElementDeploy.setProperties(Map.of());
            acElementDeploy.setId(element.getId());
            participantDeploy.getAcElementList().add(acElementDeploy);
        }
        ach.handleAutomationCompositionDeploy(deployMsg);
        verify(listener, times(automationComposition.getElements().size())).deploy(any(), any(), any());
    }
}
