/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler.cache;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantDtoUtils;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.dto.CompositionDto;
import org.onap.policy.clamp.models.acm.dto.ParticipantDto;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.utils.validation.Assertions;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CacheProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheProvider.class);

    @Getter
    private final UUID participantId;

    @Getter
    @Setter
    private boolean registered = false;

    @Getter
    private final UUID replicaId;

    private final List<ParticipantSupportedElementType> supportedAcElementTypes;

    @Getter
    private final Map<UUID, AutomationComposition> automationCompositions = new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, CompositionDto> compositionDtos = new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, UUID> msgIdentification = new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, AutomationCompositionMsg<?>> messagesOnHold = new HashMap<>();

    /**
     * Constructor.
     *
     * @param parameters the parameters of the participant
     */
    public CacheProvider(ParticipantParameters parameters) {
        this.participantId = parameters.getIntermediaryParameters().getParticipantId();
        this.supportedAcElementTypes = parameters.getIntermediaryParameters().getParticipantSupportedElementTypes();
        validateParticipantSupportedElementTypes();
        this.replicaId = UUID.randomUUID();
    }

    private void validateParticipantSupportedElementTypes() {
        for (var supElementType : supportedAcElementTypes) {
            Assertions.validateStringParameter("name", supElementType.getTypeName(), PfKey.NAME_REGEXP);
            Assertions.validateStringParameter("version", supElementType.getTypeVersion(), PfKey.VERSION_REGEXP);
        }
    }

    public List<ParticipantSupportedElementType> getSupportedAcElementTypes() {
        return PfUtils.mapList(supportedAcElementTypes, ParticipantSupportedElementType::new);
    }

    /**
     * Get AutomationComposition by id.
     *
     * @param automationCompositionId the AutomationComposition Id
     * @return the AutomationComposition
     */
    public AutomationComposition getAutomationComposition(@NonNull UUID automationCompositionId) {
        return automationCompositions.get(automationCompositionId);
    }

    /**
     * Remove AutomationComposition.
     *
     * @param automationCompositionId the AutomationComposition Id
     */
    public void removeAutomationComposition(@NonNull UUID automationCompositionId) {
        automationCompositions.remove(automationCompositionId);
    }

    /**
     * Add CompositionDto.
     *
     * @param compositionId the composition Id
     * @param list the list of AutomationCompositionElementDefinition to add
     */
    public void addCompositionDto(@NonNull UUID compositionId, List<AutomationCompositionElementDefinition> list) {
        compositionDtos.put(compositionId, AcmUtils.createCompositionDto(compositionId, list));
        LOGGER.info("Updated cache for the composition id {}", compositionId);
    }

    /**
     * Add CompositionDto.
     *
     * @param compositionDto the CompositionDto from the prime message
     */
    public void addCompositionDto(@NonNull CompositionDto compositionDto) {
        compositionDtos.put(compositionDto.compositionId(), compositionDto);
        LOGGER.info("Updated cache for the composition id {}", compositionDto.compositionId());
    }

    public void removeCompositionDto(@NonNull UUID compositionId) {
        compositionDtos.remove(compositionId);
    }

    /**
     * Initialize an AutomationComposition from a ParticipantDeploy.
     *
     * @param compositionId the composition Id
     * @param instanceId the Automation Composition Id
     * @param participantDeploy the ParticipantDeploy
     * @param revisionId the identification of the last update
     */
    public void initializeAutomationComposition(@NonNull UUID compositionId, @NonNull UUID instanceId,
            ParticipantDeploy participantDeploy, UUID revisionId) {
        initializeAutomationComposition(compositionId, null, instanceId, participantDeploy,
            DeployState.DEPLOYING, SubState.NONE, revisionId);

    }

    /**
     * Initialize an AutomationComposition from a ParticipantDeploy.
     *
     * @param compositionId the composition Id
     * @param instanceId the Automation Composition Id
     * @param participantDeploy the ParticipantDeploy
     * @param deployState the DeployState
     * @param subState the SubState
     * @param revisionId the identification of the last update
     */
    public void initializeAutomationComposition(@NonNull UUID compositionId, UUID compositionTargetId,
                                                @NonNull UUID instanceId,
            ParticipantDeploy participantDeploy, DeployState deployState, SubState subState, UUID revisionId) {

        var automationComposition = createAcInstance(compositionId, compositionTargetId, instanceId, participantDeploy,
                deployState, subState, revisionId);

        automationCompositions.put(instanceId, automationComposition);
        LOGGER.info("Initialized participant cache for the {} operation of the instance {}", deployState, instanceId);

    }

    /**
     * Initialize an AutomationComposition from a ParticipantRestartAc.
     *
     * @param compositionId the composition Id
     * @param participantRestartAc the ParticipantRestartAc
     */
    public void initializeAutomationComposition(@NonNull UUID compositionId,
            ParticipantRestartAc participantRestartAc) {
        Map<UUID, AutomationCompositionElement> acElementMap = new LinkedHashMap<>();
        for (var element : participantRestartAc.getAcElementList()) {
            if (!getParticipantId().equals(element.getParticipantId())) {
                continue;
            }
            var acElement = new AutomationCompositionElement();
            acElement.setId(element.getId());
            acElement.setParticipantId(getParticipantId());
            acElement.setDefinition(element.getDefinition());
            acElement.setDeployState(element.getDeployState());
            acElement.setLockState(element.getLockState());
            acElement.setSubState(SubState.NONE);
            acElement.setOperationalState(element.getOperationalState());
            acElement.setUseState(element.getUseState());
            acElement.setProperties(element.getProperties());
            acElement.setOutProperties(element.getOutProperties());
            acElement.setMigrationState(element.getMigrationState());
            acElementMap.put(element.getId(), acElement);
        }
        var automationComposition = new AutomationComposition();
        automationComposition.setCompositionId(compositionId);
        automationComposition.setCompositionTargetId(participantRestartAc.getCompositionTargetId());
        automationComposition.setDeployState(participantRestartAc.getDeployState());
        automationComposition.setLockState(participantRestartAc.getLockState());
        automationComposition.setInstanceId(participantRestartAc.getAutomationCompositionId());
        automationComposition.setElements(acElementMap);
        automationComposition.setStateChangeResult(participantRestartAc.getStateChangeResult());
        automationComposition.setRevisionId(participantRestartAc.getRevisionId());
        automationCompositions.put(automationComposition.getInstanceId(), automationComposition);
        LOGGER.info("Updated participant cache for the instance id {}",
                participantRestartAc.getAutomationCompositionId());
    }

    /**
     *  Create an AutomationComposition.
     * @param compositionId compositionId
     * @param compositionTargetId compositionTargetId
     * @param instanceId instanceId
     * @param participantDeploy participantDeploy
     * @param deployState deployState
     * @param subState subState
     * @param revisionId revisionId
     * @return AutomationComposition
     */
    public AutomationComposition createAcInstance(@NonNull UUID compositionId, UUID compositionTargetId,
                                                  @NonNull UUID instanceId, ParticipantDeploy participantDeploy,
                                                  DeployState deployState, SubState subState, UUID revisionId) {
        var acLast = automationCompositions.get(instanceId);
        Map<UUID, AutomationCompositionElement> acElementMap = new LinkedHashMap<>();
        for (var element : participantDeploy.getAcElementList()) {
            var acElement = createAutomationCompositionElement(element);
            acElement.setParticipantId(getParticipantId());
            acElement.setDeployState(deployState);
            acElement.setSubState(subState);
            var acElementLast = acLast != null ? acLast.getElements().get(element.getId()) : null;
            if (acElementLast != null) {
                acElement.setOutProperties(acElementLast.getOutProperties());
                acElement.setOperationalState(acElementLast.getOperationalState());
                acElement.setUseState(acElementLast.getUseState());
            }
            acElementMap.put(element.getId(), acElement);
        }
        var automationComposition = acLast != null ? acLast : new AutomationComposition();
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(instanceId);
        if (acLast != null) {
            automationComposition.getElements().putAll(acElementMap);
        } else {
            automationComposition.setElements(acElementMap);
        }
        automationComposition.setDeployState(deployState);
        automationComposition.setSubState(subState);
        automationComposition.setRevisionId(revisionId);
        if (compositionTargetId != null) {
            automationComposition.setCompositionTargetId(compositionTargetId);
        }

        return automationComposition;
    }

    /**
     * Create AutomationComposition instance from DTOs.
     *
     * @param compositionId the composition Id
     * @param instanceId the instance Id
     * @param elementDtoMap map of element Id to AcElementDto
     * @param deployState the DeployState
     * @param subState the SubState
     * @param revisionId the identification of the last update
     * @return the AutomationComposition
     */
    public AutomationComposition createAcInstance(@NonNull UUID compositionId, @NonNull UUID instanceId,
            Map<UUID, AcElementDto> elementDtoMap, DeployState deployState, SubState subState, UUID revisionId) {
        Map<UUID, AutomationCompositionElement> acElementMap = new LinkedHashMap<>();
        UUID compositionTargetId = null;
        for (var dto : elementDtoMap.values()) {
            var instanceElement = ParticipantDtoUtils.resolveInstanceElement(dto);
            var elementId = instanceElement.elementId();
            var acElement = new AutomationCompositionElement();
            acElement.setId(elementId);
            acElement.setDefinition(dto.getCompositionElement().elementDefinitionId());
            acElement.setProperties(new HashMap<>(instanceElement.inProperties()));
            acElement.setParticipantId(getParticipantId());
            acElement.setDeployState(dto.getDeployState());
            acElement.setSubState(dto.getSubState());
            acElement.setLockState(dto.getLockState());
            acElement.setOutProperties(new HashMap<>(instanceElement.outProperties()));
            acElement.setOperationalState(dto.getOperationalState());
            acElement.setUseState(dto.getUseState());
            acElementMap.put(elementId, acElement);
            if (dto.getCompositionElementTarget() != null) {
                compositionTargetId = dto.getCompositionElementTarget().compositionId();
            }
        }
        var automationComposition = new AutomationComposition();
        automationComposition.setElements(acElementMap);
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(instanceId);
        automationComposition.setDeployState(deployState);
        automationComposition.setSubState(subState);
        automationComposition.setRevisionId(revisionId);
        automationComposition.setCompositionTargetId(compositionTargetId);

        automationCompositions.put(instanceId, automationComposition);
        return automationComposition;
    }

    /**
     * Create AutomationCompositionElement to save in memory.
     *
     * @param element AcElementDeploy
     * @return a new AutomationCompositionElement
     */
    public static AutomationCompositionElement createAutomationCompositionElement(AcElementDeploy element) {
        var acElement = new AutomationCompositionElement();
        acElement.setId(element.getId());
        acElement.setDefinition(element.getDefinition());
        acElement.setProperties(element.getProperties());
        acElement.setSubState(SubState.NONE);
        acElement.setLockState(LockState.LOCKED);
        acElement.setMigrationState(element.getMigrationState());
        return acElement;
    }

    /**
     * Check instance is present and compare the last update.
     *
     * @param instanceId the instanceId
     * @param revisionId the last Update
     * @return true if the instance is updated
     */
    public boolean isInstanceUpdated(UUID instanceId, UUID revisionId) {
        if (revisionId == null) {
            // old ACM-r
            return true;
        }
        var automationComposition = automationCompositions.get(instanceId);
        if (automationComposition == null) {
            return false;
        }
        return revisionId.equals(automationComposition.getRevisionId());
    }

    /**
     * Fill Cache Composition and Composition target.
     *
     * @param participantDtoList the list of ParticipantDto from the Kafka message
     */
    public void fillCacheComposition(List<ParticipantDto> participantDtoList) {
        var compositionDto = ParticipantDtoUtils.getCompositionDto(participantDtoList, getParticipantId());
        addCompositionDto(compositionDto);
        var compositionTargetDto = ParticipantDtoUtils.getCompositionTargetDto(participantDtoList, getParticipantId());
        if (compositionTargetDto != null) {
            addCompositionDto(compositionTargetDto);
        }
    }
}
