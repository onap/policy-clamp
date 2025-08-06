/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcElementPropertiesPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AcPreparePublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionDeployPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionMigrationPublisher;
import org.onap.policy.clamp.acm.runtime.supervision.comm.AutomationCompositionStateChangePublisher;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.MessageProvider;

class SupervisionAcHandlerTest {
    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";
    private static final String AC_INSTANTIATION_SIMPLE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionSimple.json";
    private static final UUID IDENTIFIER = UUID.randomUUID();

    @Test
    void testAutomationCompositionDeployAckValidation() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var messageProvider = mock(MessageProvider.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), messageProvider, mock(EncryptionUtils.class));

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setInstanceId(IDENTIFIER);
        var automationCompositionAckMessage =
                getAutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK,
                        automationComposition, DeployState.DEPLOYED, LockState.UNLOCKED);
        automationCompositionAckMessage.setStateChangeResult(null);
        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        automationCompositionAckMessage.setStateChangeResult(StateChangeResult.TIMEOUT);
        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        automationCompositionAckMessage.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationCompositionAckMessage.setAutomationCompositionId(null);
        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        automationCompositionAckMessage.setAutomationCompositionId(automationComposition.getInstanceId());
        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        automationCompositionAckMessage =
                getAutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK,
                        automationComposition, DeployState.DEPLOYING, LockState.UNLOCKED);
        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        verify(messageProvider, times(0)).save(any(AutomationCompositionDeployAck.class));

        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));
        automationCompositionAckMessage.setAutomationCompositionResultMap(null);
        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);
        verify(messageProvider).save(any(AutomationCompositionDeployAck.class));
    }

    @Test
    void testHandleAcMigrationWithStage() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setInstanceId(IDENTIFIER);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));
        var messageProvider = mock(MessageProvider.class);

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), messageProvider, mock(EncryptionUtils.class));

        var automationCompositionAckMessage =
                getAutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK,
                        automationComposition, DeployState.MIGRATING, LockState.LOCKED);
        automationCompositionAckMessage.setStage(1);
        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        verify(messageProvider).save(any(AutomationCompositionDeployAck.class));
    }

    @Test
    void testHandleAutomationCompositionStateChangeAckMessage() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Crud");
        automationComposition.setInstanceId(IDENTIFIER);
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        when(automationCompositionProvider.findAutomationComposition(IDENTIFIER))
                .thenReturn(Optional.of(automationComposition));
        var messageProvider = mock(MessageProvider.class);

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), messageProvider, mock(EncryptionUtils.class));

        var automationCompositionAckMessage =
                getAutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK,
                        automationComposition, DeployState.DEPLOYED, LockState.UNLOCKED);
        handler.handleAutomationCompositionStateChangeAckMessage(automationCompositionAckMessage);

        verify(messageProvider).save(any(AutomationCompositionDeployAck.class));
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
        automationCompositionAckMessage.setStateChangeResult(StateChangeResult.NO_ERROR);
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
        var messageProvider = mock(MessageProvider.class);

        var automationCompositionAckMessage =
                getAutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_DEPLOY_ACK,
                        automationComposition, DeployState.DEPLOYED, LockState.LOCKED);
        automationCompositionAckMessage.setParticipantId(CommonTestData.getParticipantId());

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), messageProvider, mock(EncryptionUtils.class));

        handler.handleAutomationCompositionUpdateAckMessage(automationCompositionAckMessage);

        verify(messageProvider).save(any(AutomationCompositionDeployAck.class));
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
        automationCompositionAckMessage.setStateChangeResult(StateChangeResult.NO_ERROR);
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
        var messageProvider = mock(MessageProvider.class);

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), automationCompositionStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), messageProvider, mock(EncryptionUtils.class));

        handler.handleAutomationCompositionUpdateAckMessage(automationCompositionAckMessage);

        verify(messageProvider).save(any(AutomationCompositionDeployAck.class));
    }

    @Test
    void testDeploy() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Deploy");
        automationComposition.setDeployState(DeployState.UNDEPLOYED);
        deploy(automationComposition);
    }

    @Test
    void testDeployFailed() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Deploy");
        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.getElements().values()
                .forEach(element -> element.setDeployState(DeployState.UNDEPLOYING));
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        deploy(automationComposition);

        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values()
                .forEach(element -> element.setDeployState(DeployState.DEPLOYING));
        automationComposition.getElements().values().iterator().next().setDeployState(DeployState.DEPLOYED);
        deploy(automationComposition);
    }

    @Test
    void testDeployFailedSimple() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_SIMPLE_JSON, "Deploy");
        automationComposition.setDeployState(DeployState.UNDEPLOYED);
        automationComposition.getElements().values().iterator().next().setDeployState(DeployState.UNDEPLOYED);
        deploy(automationComposition);

        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values().iterator().next().setDeployState(DeployState.DEPLOYED);
        deploy(automationComposition);

        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values().iterator().next().setDeployState(DeployState.UNDEPLOYED);
        deploy(automationComposition);
    }

    private void deploy(AutomationComposition automationComposition) {
        var acDeployPublisher = mock(AutomationCompositionDeployPublisher.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        var handler = new SupervisionAcHandler(acProvider, acDeployPublisher,
                mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), mock(MessageProvider.class), mock(EncryptionUtils.class));
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        handler.deploy(automationComposition, acDefinition);
        verify(acProvider).updateAutomationComposition(automationComposition);
        verify(acDeployPublisher, timeout(1000)).send(automationComposition, 0, true, acDefinition.getRevisionId());
    }

    @Test
    void testUndeploy() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Undeploy");
        automationComposition.setDeployState(DeployState.DEPLOYED);
        automationComposition.setStateChangeResult(StateChangeResult.NO_ERROR);
        automationComposition.getElements().values()
                .forEach(element -> element.setDeployState(DeployState.DEPLOYED));
        undeploy(automationComposition);
    }

    private void undeploy(AutomationComposition automationComposition) {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), acStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), mock(MessageProvider.class), mock(EncryptionUtils.class));
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        handler.undeploy(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher, timeout(1000))
                .send(any(AutomationComposition.class), anyInt(), anyBoolean(), any(UUID.class));
    }

    @Test
    void testUndeployFailed() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "UnDeploy");
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values()
                .forEach(element -> element.setDeployState(DeployState.UNDEPLOYED));
        undeploy(automationComposition);

        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values()
                .forEach(element -> element.setDeployState(DeployState.DEPLOYING));
        automationComposition.getElements().values().iterator().next().setDeployState(DeployState.UNDEPLOYED);
        undeploy(automationComposition);
    }

    @Test
    void testUndeployFailedSimple() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_SIMPLE_JSON, "UnDeploy");
        automationComposition.setDeployState(DeployState.DEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values().iterator().next().setDeployState(DeployState.UNDEPLOYED);
        undeploy(automationComposition);

        automationComposition.setDeployState(DeployState.UNDEPLOYING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values().iterator().next().setDeployState(DeployState.DEPLOYED);
        undeploy(automationComposition);
    }

    @Test
    void testUnlock() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "UnLock");
        automationComposition.setLockState(LockState.LOCKED);
        automationComposition.getElements().values()
                .forEach(element -> element.setLockState(LockState.LOCKED));
        unlock(automationComposition);
    }

    private void unlock(AutomationComposition automationComposition) {
        var acProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(acProvider,
                mock(AutomationCompositionDeployPublisher.class), acStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), mock(MessageProvider.class), mock(EncryptionUtils.class));
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        handler.unlock(automationComposition, acDefinition);

        verify(acProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher, timeout(1000))
                .send(any(AutomationComposition.class), anyInt(), anyBoolean(), any(UUID.class));
    }

    @Test
    void testUnlockFailed() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "UnLock");
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.setLockState(LockState.LOCKING);
        automationComposition.getElements().values().forEach(element -> element.setLockState(LockState.LOCKING));
        unlock(automationComposition);

        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.setLockState(LockState.UNLOCKING);
        automationComposition.getElements().values().forEach(element -> element.setLockState(LockState.UNLOCKING));
        automationComposition.getElements().values().iterator().next().setLockState(LockState.UNLOCKED);
        unlock(automationComposition);
    }

    @Test
    void testUnlockSimple() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_SIMPLE_JSON, "UnLock");
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.setLockState(LockState.LOCKING);
        automationComposition.getElements().values().iterator().next().setLockState(LockState.UNLOCKED);
        unlock(automationComposition);

        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.setLockState(LockState.UNLOCKING);
        automationComposition.getElements().values().iterator().next().setLockState(LockState.LOCKED);
        unlock(automationComposition);
    }

    @Test
    void testLock() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Lock");
        automationComposition.setLockState(LockState.UNLOCKED);
        automationComposition.getElements().values().forEach(element -> element.setLockState(LockState.UNLOCKED));
        lock(automationComposition);
    }

    private void lock(AutomationComposition automationComposition) {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acStateChangePublisher = mock(AutomationCompositionStateChangePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), acStateChangePublisher,
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), mock(MessageProvider.class), mock(EncryptionUtils.class));
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        handler.lock(automationComposition, acDefinition);

        verify(automationCompositionProvider).updateAutomationComposition(any(AutomationComposition.class));
        verify(acStateChangePublisher, timeout(1000))
                .send(any(AutomationComposition.class), anyInt(), anyBoolean(), any(UUID.class));
    }

    @Test
    void testLockFailed() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Lock");
        automationComposition.setLockState(LockState.UNLOCKING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values().forEach(element -> element.setLockState(LockState.UNLOCKING));
        lock(automationComposition);

        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.setLockState(LockState.LOCKING);
        automationComposition.getElements().values().forEach(element -> element.setLockState(LockState.LOCKING));
        automationComposition.getElements().values().iterator().next().setLockState(LockState.LOCKED);
        lock(automationComposition);
    }

    @Test
    void testLockSimple() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_SIMPLE_JSON, "Lock");
        automationComposition.setLockState(LockState.UNLOCKING);
        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.getElements().values().iterator().next().setLockState(LockState.LOCKED);
        lock(automationComposition);

        automationComposition.setStateChangeResult(StateChangeResult.FAILED);
        automationComposition.setLockState(LockState.LOCKING);
        automationComposition.getElements().values().iterator().next().setLockState(LockState.UNLOCKED);
        lock(automationComposition);
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
        automationCompositionAckMessage.setStateChangeResult(StateChangeResult.NO_ERROR);
        var messageProvider = mock(MessageProvider.class);

        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), messageProvider, mock(EncryptionUtils.class));

        handler.handleAutomationCompositionUpdateAckMessage(automationCompositionAckMessage);

        verify(messageProvider).save(any(AutomationCompositionDeployAck.class));
    }

    @Test
    void testUpdate() {
        var acElementPropertiesPublisher = mock(AcElementPropertiesPublisher.class);
        var handler = new SupervisionAcHandler(mock(AutomationCompositionProvider.class),
                mock(AutomationCompositionDeployPublisher.class),
                mock(AutomationCompositionStateChangePublisher.class), acElementPropertiesPublisher,
                mock(AutomationCompositionMigrationPublisher.class),
                mock(AcPreparePublisher.class), mock(MessageProvider.class), mock(EncryptionUtils.class));
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Lock");
        handler.update(automationComposition, UUID.randomUUID());
        verify(acElementPropertiesPublisher, timeout(1000)).send(any(AutomationComposition.class),
                any(UUID.class));
    }

    @Test
    void testMigrate() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acCompositionMigrationPublisher = mock(AutomationCompositionMigrationPublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), acCompositionMigrationPublisher,
                mock(AcPreparePublisher.class), mock(MessageProvider.class), mock(EncryptionUtils.class));
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Migrate");
        assert automationComposition != null;
        automationComposition.setPhase(0);
        handler.migrate(automationComposition, UUID.randomUUID(), UUID.randomUUID(), List.of());
        verify(acCompositionMigrationPublisher, timeout(1000))
                .send(any(AutomationComposition.class), anyInt(), any(UUID.class), any(UUID.class), any());
    }

    @Test
    void testMigratePrecheck() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acCompositionMigrationPublisher = mock(AutomationCompositionMigrationPublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), acCompositionMigrationPublisher,
                mock(AcPreparePublisher.class), mock(MessageProvider.class), mock(EncryptionUtils.class));
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Migrate");
        handler.migratePrecheck(automationComposition, UUID.randomUUID(), UUID.randomUUID(), List.of());
        verify(acCompositionMigrationPublisher, timeout(1000))
                .send(any(AutomationComposition.class), anyInt(), any(UUID.class), any(UUID.class), any());
    }

    @Test
    void testPrepare() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acPreparePublisher = mock(AcPreparePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                acPreparePublisher, mock(MessageProvider.class), mock(EncryptionUtils.class));
        var serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        var acDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Migrate");
        handler.prepare(automationComposition, acDefinition);
        verify(acPreparePublisher, timeout(1000)).sendPrepare(any(AutomationComposition.class), anyInt(),
                any(UUID.class));
    }

    @Test
    void testReview() {
        var automationCompositionProvider = mock(AutomationCompositionProvider.class);
        var acPreparePublisher = mock(AcPreparePublisher.class);
        var handler = new SupervisionAcHandler(automationCompositionProvider,
                mock(AutomationCompositionDeployPublisher.class), mock(AutomationCompositionStateChangePublisher.class),
                mock(AcElementPropertiesPublisher.class), mock(AutomationCompositionMigrationPublisher.class),
                acPreparePublisher, mock(MessageProvider.class), mock(EncryptionUtils.class));
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Migrate");
        handler.review(automationComposition, new AutomationCompositionDefinition());
        verify(acPreparePublisher, timeout(1000)).sendReview(any(AutomationComposition.class), any(UUID.class));
    }
}
