/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.main.parameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.handler.DummyParticipantParameters;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.Topics;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcElementRestart;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.dto.CompositionElementDto;
import org.onap.policy.clamp.models.acm.dto.ElementState;
import org.onap.policy.clamp.models.acm.dto.InstanceElementDto;
import org.onap.policy.clamp.models.acm.dto.ParticipantDto;
import org.onap.policy.clamp.models.acm.messages.kafka.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.common.utils.coder.MapperFactory;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.springframework.core.io.ClassPathResource;

/**
 * Class to hold/create all parameters for test cases.
 */
public class CommonTestData {
    public static final String PARTICIPANT_GROUP_NAME = "AutomationCompositionParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    private static final ObjectMapper MAPPER = MapperFactory.createJsonMapper();
    public static final UUID AC_ID_0 = UUID.randomUUID();
    public static final UUID AC_ID_1 = UUID.randomUUID();
    public static final UUID PARTICIPANT_ID = UUID.randomUUID();
    public static final UUID REPLICA_ID = UUID.randomUUID();

    /**
     * Get ParticipantIntermediaryParameters.
     *
     * @return ParticipantIntermediaryParameters
     */
    public static ParticipantIntermediaryParameters getParticipantIntermediaryParameters() {
        try {
            var json = MAPPER.writeValueAsString(getIntermediaryParametersMap(PARTICIPANT_GROUP_NAME));
            return MAPPER.readValue(json, ParticipantIntermediaryParameters.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get ParticipantParameters.
     *
     * @return ParticipantParameters
     */
    public static DummyParticipantParameters getParticipantParameters() {
        try {
            var json = MAPPER.writeValueAsString(getParametersMap(PARTICIPANT_GROUP_NAME));
            return MAPPER.readValue(json, DummyParticipantParameters.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a property map for a Parameters map for test cases.
     *
     * @param name name of the parameters
     * @return a property map suitable for constructing an object
     */
    public static Map<String, Object> getParametersMap(final String name) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("intermediaryParameters", getIntermediaryParametersMap(name));
        return map;
    }

    /**
     * Returns a property map for a intermediaryParameters map for test cases.
     *
     * @param name name of the parameters
     * @return a property map suitable for constructing an object
     */
    public static Map<String, Object> getIntermediaryParametersMap(final String name) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("name", name);
        map.put("participantId", getParticipantId());
        map.put("description", DESCRIPTION);
        map.put("reportingTimeIntervalMs", TIME_INTERVAL);
        map.put("kafka", getKafkaParametersMap());
        map.put("topics", getTopics());
        var supportedElementType = new ParticipantSupportedElementType();
        supportedElementType.setTypeName("org.onap.policy.clamp.acm.HttpAutomationCompositionElement");
        supportedElementType.setTypeVersion("1.0.0");
        map.put("participantSupportedElementTypes", List.of(supportedElementType));

        return map;
    }

    /**
     * Returns a property map for kafka parameters for test cases.
     *
     * @return a property map suitable for constructing an object
     */
    public static Map<String, Object> getKafkaParametersMap() {
        final Map<String, Object> map = new TreeMap<>();
        map.put("bootstrapServers", "localhost:9092");
        var consumerMap = new TreeMap<String, Object>();
        consumerMap.put("groupId", "test-intermediary");
        map.put("consumer", consumerMap);
        return map;
    }

    private static Topics getTopics() {
        return new Topics("policy-acruntime-participant", "acm-ppnt-sync");
    }

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static UUID getParticipantId() {
        return PARTICIPANT_ID;
    }

    public static UUID getReplicaId() {
        return REPLICA_ID;
    }

    public static ToscaConceptIdentifier getDefinition() {
        return new ToscaConceptIdentifier("org.onap.domain.pmsh.PMSH_DCAEMicroservice", "1.2.3");
    }

    /**
     * Returns a Map of ToscaConceptIdentifier and AutomationComposition for test cases.
     *
     * @return automationCompositionMap
     */
    public static Map<UUID, AutomationComposition> getTestAutomationCompositionMap() {
        var automationCompositions = getTestAutomationCompositions();
        var automationComposition = automationCompositions.getAutomationCompositionList().get(1);
        Map<UUID, AutomationComposition> automationCompositionMap = new LinkedHashMap<>();
        automationCompositionMap.put(automationComposition.getInstanceId(), automationComposition);
        return automationCompositionMap;
    }

    /**
     * Returns List of AutomationComposition for test cases.
     *
     * @return AutomationCompositions
     */
    public static AutomationCompositions getTestAutomationCompositions() {
        try {
            var automationCompositions =
                    MAPPER.readValue(new ClassPathResource("providers/TestAutomationCompositions.json")
                            .getInputStream(), AutomationCompositions.class);
            automationCompositions.getAutomationCompositionList().get(1).setInstanceId(AC_ID_0);
            automationCompositions.getAutomationCompositionList().get(1).setInstanceId(AC_ID_1);
            return automationCompositions;
        } catch (Exception e) {
            throw new RuntimeException("cannot read TestAutomationCompositions.json");
        }
    }

    /**
     * Return a AutomationCompositionStateChange.
     *
     * @param participantId the participantId
     * @param instanceId the AutomationComposition Id
     * @param deployOrder a DeployOrder
     * @param lockOrder a LockOrder
     * @return a AutomationCompositionStateChange
     */
    public static AutomationCompositionStateChange getStateChange(UUID participantId, UUID instanceId,
            DeployOrder deployOrder, LockOrder lockOrder) {
        var stateChange = new AutomationCompositionStateChange();
        stateChange.setStartPhase(0);
        stateChange.setAutomationCompositionId(instanceId);
        stateChange.setParticipantId(participantId);
        stateChange.setMessageId(UUID.randomUUID());
        stateChange.setDeployOrderedState(deployOrder);
        stateChange.setLockOrderedState(lockOrder);
        stateChange.setTimestamp(Instant.ofEpochMilli(3000));
        return stateChange;
    }

    /**
     * Create a ParticipantRestartAc.
     *
     * @return a ParticipantRestartAc
     */
    public static ParticipantRestartAc createParticipantRestartAc() {
        var participantRestartAc = new ParticipantRestartAc();
        participantRestartAc.setAutomationCompositionId(AC_ID_0);
        participantRestartAc.setDeployState(DeployState.DEPLOYED);
        participantRestartAc.setLockState(LockState.LOCKED);
        var acElementRestart = new AcElementRestart();
        acElementRestart.setDefinition(getDefinition());
        acElementRestart.setParticipantId(PARTICIPANT_ID);
        acElementRestart.setDeployState(DeployState.DEPLOYED);
        acElementRestart.setLockState(LockState.LOCKED);
        acElementRestart.setOperationalState("OperationalState");
        acElementRestart.setUseState("UseState");
        acElementRestart.setProperties(Map.of("key", "value"));
        acElementRestart.setOutProperties(Map.of("keyOut", "valueOut"));
        acElementRestart.setId(UUID.randomUUID());
        participantRestartAc.getAcElementList().add(acElementRestart);
        return participantRestartAc;
    }

    /**
     * Create a ParticipantDeploy from an AutomationComposition.
     *
     * @param participantId the participantId
     * @param automationComposition the AutomationComposition
     * @return the ParticipantDeploy
     */
    public static ParticipantDeploy createparticipantDeploy(UUID participantId,
            AutomationComposition automationComposition) {
        var participantDeploy = new ParticipantDeploy();
        participantDeploy.setParticipantId(participantId);
        for (var element : automationComposition.getElements().values()) {
            var acElement = new AcElementDeploy();
            acElement.setId(element.getId());
            acElement.setDefinition(element.getDefinition());
            acElement.setProperties(element.getProperties());
            acElement.setMigrationState(element.getMigrationState());
            participantDeploy.getAcElementList().add(acElement);
        }
        return participantDeploy;
    }

    /**
     * create a List of AutomationCompositionElementDefinition from an AutomationComposition.
     *
     * @param automationComposition the AutomationComposition
     * @return the List of AutomationCompositionElementDefinition
     */
    public static List<AutomationCompositionElementDefinition>
            createAutomationCompositionElementDefinitionList(AutomationComposition automationComposition) {
        List<AutomationCompositionElementDefinition> definitions = new ArrayList<>();
        for (var element : automationComposition.getElements().values()) {
            if (!MigrationState.REMOVED.equals(element.getMigrationState())) {
                definitions.add(createAutomationCompositionElementDefinition(element.getDefinition()));
            }
        }
        return definitions;
    }

    /**
     * create a new example of AutomationCompositionElementDefinition.
     *
     * @param definition the composition definition element id
     * @return the AutomationCompositionElementDefinition
     */
    public static AutomationCompositionElementDefinition createAutomationCompositionElementDefinition(
            ToscaConceptIdentifier definition) {
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(definition);
        var nodeTemplate = new ToscaNodeTemplate();
        nodeTemplate.setProperties(Map.of("key", "value"));
        acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(nodeTemplate);
        return acElementDefinition;
    }

    /**
     * Create a List of ParticipantDto for testing with pre-built DTOs.
     *
     * @param participantId the participantId
     * @param automationComposition the AutomationComposition
     * @return List of ParticipantDto
     */
    public static List<ParticipantDto> createParticipantDtoList(UUID participantId,
            AutomationComposition automationComposition) {
        var participantDto = new ParticipantDto();
        participantDto.setParticipantId(participantId);
        for (var element : automationComposition.getElements().values()) {
            var acElementDto = new AcElementDto();
            acElementDto.setCompositionElement(new CompositionElementDto(
                    automationComposition.getCompositionId(), element.getDefinition(),
                    Map.of("startPhase", 0), Map.of()));
            acElementDto.setInstanceElement(new InstanceElementDto(
                    automationComposition.getInstanceId(), element.getId(),
                    element.getProperties() != null ? element.getProperties() : Map.of(),
                    element.getOutProperties() != null ? element.getOutProperties() : Map.of()));
            acElementDto.setCompositionElementTarget(new CompositionElementDto(
                    automationComposition.getCompositionTargetId() != null
                            ? automationComposition.getCompositionTargetId()
                            : automationComposition.getCompositionId(),
                    element.getDefinition(), Map.of("startPhase", 0, "stage",
                            Map.of("migrate", List.of(0, 1, 2), "prepare", List.of(0, 1, 2))),
                    Map.of()));
            acElementDto.setInstanceElementTarget(new InstanceElementDto(
                    automationComposition.getInstanceId(), element.getId(),
                    element.getProperties() != null ? element.getProperties() : Map.of(),
                    element.getOutProperties() != null ? element.getOutProperties() : Map.of()));
            participantDto.getElementDtos().add(acElementDto);
        }
        return List.of(participantDto);
    }

    /**
     * Create a List of ParticipantDto for rollback testing with correct ElementState values.
     * NEW elements get NOT_PRESENT on compositionElement (being rolled back/removed).
     * REMOVED elements get NOT_PRESENT on compositionElementTarget (being restored).
     *
     * @param participantId the participantId
     * @param automationComposition the AutomationComposition
     * @return List of ParticipantDto
     */
    public static List<ParticipantDto> createRollbackParticipantDtoList(UUID participantId,
            AutomationComposition automationComposition) {
        var participantDto = new ParticipantDto();
        participantDto.setParticipantId(participantId);
        for (var element : automationComposition.getElements().values()) {
            var acElementDto = new AcElementDto();
            var compositionState = ElementState.PRESENT;
            var targetState = ElementState.PRESENT;
            if (MigrationState.NEW.equals(element.getMigrationState())) {
                targetState = ElementState.REMOVED;
            } else if (MigrationState.REMOVED.equals(element.getMigrationState())) {
                compositionState = ElementState.NOT_PRESENT;
                targetState = ElementState.NEW;
            }
            acElementDto.setCompositionElement(new CompositionElementDto(
                    automationComposition.getCompositionId(), element.getDefinition(),
                    Map.of("startPhase", 0, "stage",
                            Map.of("migrate", List.of(0, 1, 2), "prepare", List.of(0, 1, 2))),
                    Map.of(), compositionState));
            acElementDto.setInstanceElement(new InstanceElementDto(
                    automationComposition.getInstanceId(), element.getId(),
                    element.getProperties() != null ? element.getProperties() : Map.of(),
                    element.getOutProperties() != null ? element.getOutProperties() : Map.of()));
            acElementDto.setCompositionElementTarget(new CompositionElementDto(
                    automationComposition.getCompositionTargetId() != null
                            ? automationComposition.getCompositionTargetId()
                            : automationComposition.getCompositionId(),
                    element.getDefinition(), Map.of("startPhase", 0, "stage",
                            Map.of("migrate", List.of(0, 1, 2), "prepare", List.of(0, 1, 2))),
                    Map.of(), targetState));
            acElementDto.setInstanceElementTarget(new InstanceElementDto(
                    automationComposition.getInstanceId(), element.getId(),
                    element.getProperties() != null ? element.getProperties() : Map.of(),
                    element.getOutProperties() != null ? element.getOutProperties() : Map.of()));
            participantDto.getElementDtos().add(acElementDto);
        }
        return List.of(participantDto);
    }
}
