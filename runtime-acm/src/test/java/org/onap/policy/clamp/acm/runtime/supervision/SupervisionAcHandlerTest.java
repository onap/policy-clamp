/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.supervision;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;

class SupervisionAcHandlerTest {
    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";
    private static final UUID IDENTIFIER = UUID.randomUUID();

    @Test
    void testHandleAutomationCompositionStateChangeAckMessage() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class),
                mock(AutomationCompositionStateChangePublisher.class));

        var automationCompositionAckMessage =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        for (var elementEntry : automationComposition.getElements().entrySet()) {
            var acElementDeployAck =
                    new AcElementDeployAck(DeployState.DEPLOYED, LockState.UNLOCKED, "", "", Map.of(), true, "");
            automationCompositionAckMessage.getAutomationCompositionResultMap().put(elementEntry.getKey(),
                    acElementDeployAck);
        }
        automationCompositionAckMessage.setAutomationCompositionId(IDENTIFIER);

        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testHandleAutomationCompositionUpdateAckMessage() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));

        var automationCompositionAckMessage =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK);
        for (var elementEntry : automationComposition.getElements().entrySet()) {
            var acElementDeployAck =
                    new AcElementDeployAck(DeployState.DEPLOYED, LockState.LOCKED, "", "", Map.of(), true, "");
            automationCompositionAckMessage
                    .setAutomationCompositionResultMap(Map.of(elementEntry.getKey(), acElementDeployAck));
        }
        automationCompositionAckMessage.setParticipantId(CommonTestData.getParticipantId());
        automationCompositionAckMessage.setAutomationCompositionId(IDENTIFIER);

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class),
                mock(AutomationCompositionStateChangePublisher.class));

        handler.handleAutomationCompositionUpdateAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testUndeploy() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class),
                acStateChangePublisher);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Undeploy");
        handler.undeploy(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testUnlock() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class),
                acStateChangePublisher);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "UnLock");
        handler.unlock(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testLock() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class),
                acStateChangePublisher);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Lock");
        handler.lock(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }
}
