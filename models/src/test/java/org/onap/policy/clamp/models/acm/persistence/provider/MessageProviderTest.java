/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeployAck;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementInfo;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionInfo;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantMessageType;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaMessage;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaMessageJob;
import org.onap.policy.clamp.models.acm.persistence.repository.MessageJobRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.MessageRepository;
import org.onap.policy.clamp.models.acm.utils.CommonTestData;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class MessageProviderTest {

    @Test
    void testSaveParticipantPrimeAck() {
        var message = new ParticipantPrimeAck();
        message.setCompositionState(AcTypeState.PRIMED);
        message.setCompositionId(UUID.randomUUID());
        message.setParticipantId(UUID.randomUUID());
        message.setReplicaId(UUID.randomUUID());
        var messageRepository = mock(MessageRepository.class);
        var messageProvider = new MessageProvider(messageRepository, mock(MessageJobRepository.class));
        messageProvider.save(message);
        verify(messageRepository).save(any());
    }

    @Test
    void testSaveAutomationCompositionDeployAck() {
        var message = new AutomationCompositionDeployAck(ParticipantMessageType.AUTOMATION_COMPOSITION_STATECHANGE_ACK);
        message.setAutomationCompositionId(UUID.randomUUID());
        message.setCompositionId(UUID.randomUUID());
        message.setStateChangeResult(StateChangeResult.NO_ERROR);
        message.setParticipantId(UUID.randomUUID());
        message.setReplicaId(UUID.randomUUID());
        var element = new AcElementDeployAck(DeployState.DEPLOYED,
                LockState.LOCKED, null, null, Map.of(), true, "");
        message.setAutomationCompositionResultMap(Map.of(UUID.randomUUID(), element));
        var messageRepository = mock(MessageRepository.class);
        var messageProvider = new MessageProvider(messageRepository, mock(MessageJobRepository.class));
        messageProvider.save(message);
        verify(messageRepository).save(any());
    }

    @Test
    void testSaveParticipantStatusComposition() {
        var message = new ParticipantStatus();
        message.setCompositionId(UUID.randomUUID());
        message.setParticipantId(UUID.randomUUID());
        message.setReplicaId(UUID.randomUUID());
        var participantDefinition = new ParticipantDefinition();
        participantDefinition.setParticipantId(message.getParticipantId());
        var element = CommonTestData.getAcElementDefinition(new ToscaConceptIdentifier("name", "1.0.0"));
        element.setOutProperties(Map.of("compositionProperty", "value"));
        participantDefinition.setAutomationCompositionElementDefinitionList(List.of(element));
        message.setParticipantDefinitionUpdates(List.of(participantDefinition));
        var messageRepository = mock(MessageRepository.class);
        var messageProvider = new MessageProvider(messageRepository, mock(MessageJobRepository.class));
        messageProvider.save(message);
        verify(messageRepository).save(any());
    }

    @Test
    void testSaveParticipantStatusInstance() {
        var message = new ParticipantStatus();
        message.setCompositionId(UUID.randomUUID());
        message.setParticipantId(UUID.randomUUID());
        message.setReplicaId(UUID.randomUUID());
        var automationCompositionInfo = new AutomationCompositionInfo();
        automationCompositionInfo.setAutomationCompositionId(UUID.randomUUID());
        var element = new AutomationCompositionElementInfo();
        element.setAutomationCompositionElementId(UUID.randomUUID());
        element.setOutProperties(Map.of("instanceProperty", "value"));
        automationCompositionInfo.setElements(List.of(element));
        message.setAutomationCompositionInfoList(List.of(automationCompositionInfo));
        var messageRepository = mock(MessageRepository.class);
        var messageProvider = new MessageProvider(messageRepository, mock(MessageJobRepository.class));
        messageProvider.save(message);
        verify(messageRepository).save(any());
    }

    @Test
    void testGetAllMessages() {
        var messageRepository = mock(MessageRepository.class);
        var instanceId = UUID.randomUUID();
        var jpaMessage = new JpaMessage();
        when(messageRepository.findByIdentificationIdOrderByLastMsgDesc(instanceId.toString()))
                .thenReturn(List.of(jpaMessage));
        var messageProvider = new MessageProvider(messageRepository, mock(MessageJobRepository.class));
        var result = messageProvider.getAllMessages(instanceId);
        assertThat(result).hasSize(1);
        var doc = result.iterator().next();
        assertEquals(jpaMessage.getMessageId(), doc.getMessageId());
    }

    @Test
    void testFindCompositionMessages() {
        var jpa1 = createJpaCompositionMessage();
        var jpa2 = createJpaInstanceMessage();
        var messageRepository = mock(MessageRepository.class);
        when(messageRepository.findAll()).thenReturn(List.of(jpa1, jpa2));
        var messageProvider = new MessageProvider(messageRepository, mock(MessageJobRepository.class));
        var result = messageProvider.findCompositionMessages();
        assertThat(result).hasSize(1);
        var compositionId = result.iterator().next();
        assertEquals(jpa1.getDocMessage().getCompositionId(), compositionId);
    }

    private JpaMessage createJpaCompositionMessage() {
        var message = new DocMessage();
        message.setCompositionId(UUID.randomUUID());
        return new JpaMessage(message.getCompositionId().toString(), message);
    }

    private JpaMessage createJpaInstanceMessage() {
        var message = new DocMessage();
        message.setCompositionId(UUID.randomUUID());
        message.setInstanceId(UUID.randomUUID());
        return new JpaMessage(message.getInstanceId().toString(), message);
    }

    @Test
    void testFindInstanceMessages() {
        var jpa1 = createJpaCompositionMessage();
        var jpa2 = createJpaInstanceMessage();
        var messageRepository = mock(MessageRepository.class);
        when(messageRepository.findAll()).thenReturn(List.of(jpa1, jpa2));
        var messageProvider = new MessageProvider(messageRepository, mock(MessageJobRepository.class));
        var result = messageProvider.findInstanceMessages();
        assertThat(result).hasSize(1);
        var instanceId = result.iterator().next();
        assertEquals(jpa2.getDocMessage().getInstanceId(), instanceId);
    }

    @Test
    void testRemoveMessage() {
        var messageRepository = mock(MessageRepository.class);
        var messageProvider = new MessageProvider(messageRepository, mock(MessageJobRepository.class));
        var messageId = UUID.randomUUID();
        messageProvider.removeMessage(messageId.toString());
        verify(messageRepository).deleteById(messageId.toString());
    }

    @Test
    void testRemoveOldJobs() {
        var messageJobRepository = mock(MessageJobRepository.class);
        var jpaJob1 = new JpaMessageJob(UUID.randomUUID().toString());
        var jpaJob2 = new JpaMessageJob(UUID.randomUUID().toString());
        var old = Timestamp.from(Instant.now().minusSeconds(220));
        jpaJob2.setJobStarted(old);
        when(messageJobRepository.findAll()).thenReturn(List.of(jpaJob1, jpaJob2));
        var messageProvider = new MessageProvider(mock(MessageRepository.class), messageJobRepository);
        messageProvider.removeOldJobs();
        verify(messageJobRepository, times(0)).deleteById(jpaJob1.getJobId());
        verify(messageJobRepository).deleteById(jpaJob2.getJobId());
    }

    @Test
    void testCreateJob() {
        var messageJobRepository = mock(MessageJobRepository.class);
        var identificationId = UUID.randomUUID();
        var jpaJob = new JpaMessageJob(identificationId.toString());
        when(messageJobRepository.save(any())).thenReturn(jpaJob);
        var messageProvider = new MessageProvider(mock(MessageRepository.class), messageJobRepository);
        var opt = messageProvider.createJob(identificationId);
        assertThat(opt).isNotEmpty();
        assertEquals(jpaJob.getJobId(), opt.get());

        when(messageJobRepository.findByIdentificationId(identificationId.toString())).thenReturn(Optional.of(jpaJob));
        opt = messageProvider.createJob(identificationId);
        assertThat(opt).isEmpty();
    }

    @Test
    void testRemoveJob() {
        var messageJobRepository = mock(MessageJobRepository.class);
        var messageProvider = new MessageProvider(mock(MessageRepository.class), messageJobRepository);
        var jobId = UUID.randomUUID().toString();
        messageProvider.removeJob(jobId);
        verify(messageJobRepository).deleteById(jobId);
    }
}
