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
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcElementPropertiesPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;

class SupervisionAcHandlerTest {
    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";
    private static final UUID IDENTIFIER = UUID.randomUUID();

    @Test
    void testHandleAutomationCompositionStateChangeAckMessage() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setInstanceId(IDENTIFIER);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), null);

        var automationCompositionAckMessage =
                getAutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK,
                        automationComposition, DeployState.DEPLOYED, LockState.UNLOCKED);
        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    private AutomationCompositionDeployAck getAutomationCompositionDeployAck(ParticipantMessageType messageType,
            AutomationComposition automationComposition, DeployState deployState, LockState lockState) {
        var automationCompositionAckMessage = new AutomationCompositionDeployAck(messageType);
        for (var elementEntry : automationComposition.getElements().entrySet()) {
            var acElementDeployAck = new AcElementDeployAck(deployState, lockState, "", "", Map.of(), true, "");
            automationCompositionAckMessage.getAutomationCompositionResultMap().put(elementEntry.getKey(),
                    acElementDeployAck);
        }
        automationCompositionAckMessage.setAutomationCompositionId(automationComposition.getInstanceId());
        automationCompositionAckMessage.setParticipantId(CommonTestData.getParticipantId());
        return automationCompositionAckMessage;
    }

    @Test
    void testHandleAutomationCompositionUpdateAckMessage() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setInstanceId(IDENTIFIER);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));

        var automationCompositionAckMessage =
                getAutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK,
                        automationComposition, DeployState.DEPLOYED, LockState.LOCKED);
        automationCompositionAckMessage.setParticipantId(CommonTestData.getParticipantId());

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), null);

        handler.handleAutomationCompositionUpdateAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testHandleAcUpdateAckFailedMessage() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));

        var automationCompositionAckMessage =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK);
        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(DeployState.DEPLOYED);
        }
        var elementEntry = automationComposition.getElements().entrySet().iterator().next();
        elementEntry.getValue().setDeployState(DeployState.DEPLOYING);
        var acElementDeployAck =
                new AcElementDeployAck(DeployState.UNDEPLOYED, LockState.NONE, "", "", Map.of(), true, "Error");
        automationCompositionAckMessage
                .setAutomationCompositionResultMap(Map.of(elementEntry.getKey(), acElementDeployAck));

        automationCompositionAckMessage.setParticipantId(CommonTestData.getParticipantId());
        automationCompositionAckMessage.setAutomationCompositionId(IDENTIFIER);

        var automationCompositionStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), automationCompositionStateChangePublisher, null,
                null);

        handler.handleAutomationCompositionUpdateAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testDeployFailed() {
        var automationCompositionDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider, automationCompositionDeployPublisher,
                mock(AutomationCompositionStateChangePublisher.class), mock(AcElementPropertiesPublisher.class), null);

        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Deploy");
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        handler.deploy(automationComposition, acDefinition);
        verify(automationCompositionProvider).updateAutomationComposition(automationComposition);
        verify(automationCompositionDeployPublisher).send(automationComposition, acDefinition.getServiceTemplate(), 0,
                true);
    }

    @Test
    void testUndeploy() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), acStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), null);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Undeploy");
        handler.undeploy(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testUndeployFailed() {
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), acStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), null);

        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "UnDeploy");
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values()
                .forEach(element -> element.setDeployState(DeployState.UNDEPLOYING));
        handler.undeploy(automationComposition, acDefinition);
        verify(automationCompositionProvider).updateAutomationComposition(automationComposition);
        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testUnlock() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), acStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), null);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "UnLock");
        handler.unlock(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testUnlockFailed() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), acStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), null);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "UnLock");
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values().forEach(element -> element.setLockState(LockState.UNLOCKING));
        handler.unlock(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testLock() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), acStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), null);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Lock");
        handler.lock(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testLockFailed() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), acStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), null);
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Lock");
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values().forEach(element -> element.setLockState(LockState.LOCKING));
        handler.lock(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher).send(any(AutomationComposition.class), anyInt(), anyBoolean());
    }

    @Test
    void testDeleteAck() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setDeployState(DeployState.DELETING);
        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));

        var automationCompositionAckMessage =
                new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK);
        automationCompositionAckMessage
                .setParticipantId(automationComposition.getElements().values().iterator().next().getParticipantId());
        automationCompositionAckMessage.setAutomationCompositionId(IDENTIFIER);

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), null);

        handler.handleAutomationCompositionUpdateAckMessage(automationCompositionAckMessage);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
    }

    @Test
    void testUpdate() {
        var acElementPropertiesPublisher = mock(AcElementPropertiesPublisher.class);
        var handler = new SupervisionAcHandler(mock(AutomationCompositionProvider.class),
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                acElementPropertiesPublisher, null);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Lock");
        handler.update(automationComposition);
        verify(acElementPropertiesPublisher).send(any(AutomationComposition.class));
    }

    @Test
    void testMigrate() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acCompositionMigrationPublisher = mock(AutomationCompositionMigrationPublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider, null, null, null,
                acCompositionMigrationPublisher);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Migrate");
        handler.migrate(automationComposition, UUID.randomUUID());
        verify(acCompositionMigrationPublisher).send(any(AutomationComposition.class), any());
    }
}
