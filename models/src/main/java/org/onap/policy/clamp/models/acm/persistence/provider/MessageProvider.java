/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionDeployAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantAckMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantPrimeAck;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.ParticipantStatus;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaMessage;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaMessageJob;
import org.onap.policy.clamp.models.acm.persistence.repository.MessageJobRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.MessageRepository;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class MessageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProvider.class);

    private final MessageRepository messageRepository;
    private final MessageJobRepository messageJobRepository;

    /**
     * Save ParticipantPrimeAck message.
     *
     * @param message the ParticipantPrimeAck message
     */
    public void save(ParticipantPrimeAck message) {
        var doc = from(message);
        doc.setCompositionState(message.getCompositionState());
        doc.setMessage(AcmUtils.validatedMessage(message.getMessage()));
        var jpa = new JpaMessage(message.getCompositionId().toString(), doc);
        ProviderUtils.validate(doc, jpa, "ParticipantPrimeAck message");
        messageRepository.save(jpa);
    }

    /**
     * Save AutomationCompositionDeployAck message.
     *
     * @param message the AutomationCompositionDeployAck message
     */
    public void save(AutomationCompositionDeployAck message) {
        for (var entry : message.getAutomationCompositionResultMap().entrySet()) {
            var doc = from(message);
            doc.setStage(message.getStage());
            doc.setInstanceElementId(entry.getKey());
            doc.setInstanceId(message.getAutomationCompositionId());
            doc.setMessage(AcmUtils.validatedMessage(entry.getValue().getMessage()));
            doc.setDeployState(entry.getValue().getDeployState());
            doc.setLockState(entry.getValue().getLockState());
            var jpa = new JpaMessage(message.getAutomationCompositionId().toString(), doc);
            ProviderUtils.validate(doc, jpa, "AutomationCompositionDeployAck message");
            messageRepository.save(jpa);
        }
    }

    /**
     * Save instance OutProperties.
     *
     * @param message the ParticipantStatus message
     */
    public void saveInstanceOutProperties(ParticipantStatus message) {
        for (var instance : message.getAutomationCompositionInfoList()) {
            for (var element : instance.getElements()) {
                var jpa = new JpaMessage();
                jpa.setIdentificationId(instance.getAutomationCompositionId().toString());
                jpa.setLastMsg(Timestamp.from(message.getTimestamp()));
                var doc = from(message);
                doc.setInstanceId(instance.getAutomationCompositionId());
                doc.setUseState(element.getUseState());
                doc.setOperationalState(element.getOperationalState());
                doc.setOutProperties(element.getOutProperties());
                doc.setInstanceElementId(element.getAutomationCompositionElementId());
                jpa.fromAuthorative(doc);
                ProviderUtils.validate(doc, jpa, "ParticipantStatus instance message");
                messageRepository.save(jpa);
            }
        }
    }

    /**
     * Save composition OutProperties.
     *
     * @param message the ParticipantStatus message
     * @param elementStateMap the NodeTemplateState map
     */
    public void saveCompositionOutProperties(ParticipantStatus message,
            Map<ToscaConceptIdentifier, NodeTemplateState> elementStateMap) {
        for (var acDefinition : message.getParticipantDefinitionUpdates()) {
            for (var element : acDefinition.getAutomationCompositionElementDefinitionList()) {
                var elementState = elementStateMap.get(element.getAcElementDefinitionId());
                if (elementState != null && elementState.getParticipantId().equals(message.getParticipantId())) {
                    var jpa = new JpaMessage();
                    jpa.setIdentificationId(message.getCompositionId().toString());
                    jpa.setLastMsg(Timestamp.from(message.getTimestamp()));
                    var doc = from(message);
                    doc.setOutProperties(element.getOutProperties());
                    doc.setAcElementDefinitionId(element.getAcElementDefinitionId());
                    jpa.fromAuthorative(doc);
                    ProviderUtils.validate(doc, jpa, "ParticipantStatus composition message");
                    messageRepository.save(jpa);
                }
            }
        }
    }

    private DocMessage from(ParticipantStatus message) {
        var doc = new DocMessage();
        doc.setMessageType(message.getMessageType());
        doc.setCompositionId(message.getCompositionId());
        doc.setParticipantId(message.getParticipantId());
        doc.setReplicaId(message.getReplicaId());
        doc.setMessageType(message.getMessageType());
        return doc;
    }

    private DocMessage from(ParticipantAckMessage message) {
        var doc = new DocMessage();
        doc.setMessageType(message.getMessageType());
        doc.setCompositionId(message.getCompositionId());
        doc.setStateChangeResult(message.getStateChangeResult());
        doc.setParticipantId(message.getParticipantId());
        doc.setReplicaId(message.getReplicaId());
        return doc;
    }

    @Transactional(readOnly = true)
    public List<DocMessage> getAllMessages(UUID identificationId) {
        var result = messageRepository.findByIdentificationIdOrderByLastMsgAsc(identificationId.toString());
        return result.stream().map(JpaMessage::toAuthorative).toList();
    }

    /**
     * Find all Composition ids from Messages.
     *
     * @return set of Composition ids
     */
    @Transactional(readOnly = true)
    public Set<UUID> findCompositionMessages() {
        var result = messageRepository.findAll();
        return result.stream()
                .map(JpaMessage::toAuthorative)
                .filter(doc -> doc.getInstanceId() == null)
                .map(DocMessage::getCompositionId)
                .collect(Collectors.toSet());
    }

    /**
     * Find all Instance ids from Messages.
     *
     * @return set of Instance ids
     */
    @Transactional(readOnly = true)
    public Set<UUID> findInstanceMessages() {
        var result = messageRepository.findAll();
        return result.stream()
                .map(JpaMessage::toAuthorative)
                .map(DocMessage::getInstanceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Remove the message.
     *
     * @param messageId the messageId
     */
    public void removeMessage(String messageId) {
        messageRepository.deleteById(messageId);
    }

    /**
     * Remove old jobs.
     */
    public void removeOldJobs() {
        var list = messageJobRepository.findAll();
        var old = Timestamp.from(Instant.now().minusSeconds(200));
        for (var job : list) {
            if (job.getJobStarted().before(old)) {
                messageJobRepository.deleteById(job.getJobId());
            }
        }
    }

    /**
     * Create new Job related to the identificationId.
     *
     * @param identificationId the instanceId or compositionId
     *
     * @return the jobId if the job has been created
     */
    public Optional<String> createJob(UUID identificationId) {
        var opt = messageJobRepository.findByIdentificationId(identificationId.toString());
        if (opt.isPresent()) {
            // already exist a job with this identificationId
            return Optional.empty();
        }
        var job = new JpaMessageJob(identificationId.toString());
        try {
            var result = messageJobRepository.save(job);
            return Optional.of(result.getJobId());
        } catch (ConstraintViolationException ex) {
            // already exist a job with this identificationId
            LOGGER.warn(ex.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Remove the job by jobId.
     *
     * @param jobId the jobId
     */
    public void removeJob(String jobId) {
        messageJobRepository.deleteById(jobId);
    }
}
