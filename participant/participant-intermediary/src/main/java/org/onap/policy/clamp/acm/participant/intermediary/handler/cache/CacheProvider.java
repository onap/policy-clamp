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
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ElementState;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
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
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.springframework.stereotype.Component;

@Component
public class CacheProvider {

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
    private final Map<UUID, AcDefinition> acElementsDefinitions = new ConcurrentHashMap<>();

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
        this.replicaId = UUID.randomUUID();
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
     * Add ElementDefinition.
     *
     * @param compositionId the composition Id
     * @param list the list of AutomationCompositionElementDefinition to add
     * @param revisionId the last Update
     */
    public void addElementDefinition(@NonNull UUID compositionId, List<AutomationCompositionElementDefinition> list,
            UUID revisionId) {
        var acDefinition = new AcDefinition();
        acDefinition.setCompositionId(compositionId);
        acDefinition.setRevisionId(revisionId);
        for (var acElementDefinition : list) {
            if (acElementDefinition.getAutomationCompositionElementToscaNodeTemplate() == null) {
                acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(new ToscaNodeTemplate());
            }
            if (acElementDefinition.getAutomationCompositionElementToscaNodeTemplate().getProperties() == null) {
                acElementDefinition.getAutomationCompositionElementToscaNodeTemplate().setProperties(new HashMap<>());
            }
            acDefinition.getElements().put(acElementDefinition.getAcElementDefinitionId(), acElementDefinition);
        }
        acElementsDefinitions.put(compositionId, acDefinition);
    }

    public void removeElementDefinition(@NonNull UUID compositionId) {
        acElementsDefinitions.remove(compositionId);
    }

    /**
     * Get CommonProperties.
     *
     * @param instanceId the Automation Composition Id
     * @param acElementId the Automation Composition Element Id
     * @return the common Properties as Map
     */
    public Map<String, Object> getCommonProperties(@NonNull UUID instanceId, @NonNull UUID acElementId) {
        var automationComposition = automationCompositions.get(instanceId);
        var element = automationComposition.getElements().get(acElementId);
        return getCommonProperties(automationComposition.getCompositionId(), element.getDefinition());
    }

    /**
     * Get CommonProperties.
     *
     * @param compositionId the composition Id
     * @param definition the AutomationCompositionElementDefinition Id
     * @return the common Properties as Map
     */
    public Map<String, Object> getCommonProperties(@NonNull UUID compositionId,
        @NonNull ToscaConceptIdentifier definition) {
        var acDefinition = acElementsDefinitions.get(compositionId);
        if (acDefinition == null) {
            return new HashMap<>();
        }
        var map = acDefinition.getElements().get(definition);
        return map != null ? map.getAutomationCompositionElementToscaNodeTemplate().getProperties() : new HashMap<>();
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
        initializeAutomationComposition(compositionId, instanceId, participantDeploy,
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
    public void initializeAutomationComposition(@NonNull UUID compositionId, @NonNull UUID instanceId,
            ParticipantDeploy participantDeploy, DeployState deployState, SubState subState, UUID revisionId) {
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
        var automationComposition = new AutomationComposition();
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(instanceId);
        automationComposition.setElements(acElementMap);
        automationComposition.setDeployState(deployState);
        automationComposition.setSubState(subState);
        automationComposition.setRevisionId(revisionId);
        automationCompositions.put(instanceId, automationComposition);
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
        return acElement;
    }

    /**
     * Create CompositionElementDto.
     *
     * @param compositionId the composition Id
     * @param element AutomationComposition Element
     * @return the CompositionElementDto
     */
    public CompositionElementDto createCompositionElementDto(UUID compositionId, AutomationCompositionElement element) {
        var acDefinition = acElementsDefinitions.get(compositionId);
        var acDefinitionElement = acDefinition != null ? acDefinition.getElements().get(element.getDefinition()) : null;

        return (acDefinitionElement != null) ? new CompositionElementDto(compositionId, element.getDefinition(),
                acDefinitionElement.getAutomationCompositionElementToscaNodeTemplate().getProperties(),
                acDefinitionElement.getOutProperties()) :
            new CompositionElementDto(compositionId, element.getDefinition(),
                Map.of(), Map.of(), ElementState.NOT_PRESENT);
    }

    /**
     * Get a Map of CompositionElementDto by elementId from the elements of an AutomationComposition.
     *
     * @param automationComposition the AutomationComposition
     * @param compositionId the compositionId
     * @return the Map of CompositionElementDto
     */
    public Map<UUID, CompositionElementDto> getCompositionElementDtoMap(AutomationComposition automationComposition,
            UUID compositionId) {
        var acDefinition = acElementsDefinitions.get(compositionId);
        Map<UUID, CompositionElementDto> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            var acDefinitionElement = acDefinition.getElements().get(element.getDefinition());
            var compositionElement = (acDefinitionElement != null)
                    ? new CompositionElementDto(compositionId, element.getDefinition(),
                    acDefinitionElement.getAutomationCompositionElementToscaNodeTemplate().getProperties(),
                    acDefinitionElement.getOutProperties()) :
                    new CompositionElementDto(compositionId, element.getDefinition(),
                            Map.of(), Map.of(), ElementState.NOT_PRESENT);
            map.put(element.getId(), compositionElement);
        }
        return map;
    }

    public Map<UUID, CompositionElementDto> getCompositionElementDtoMap(AutomationComposition automationComposition) {
        return getCompositionElementDtoMap(automationComposition, automationComposition.getCompositionId());
    }

    /**
     * Get a Map of InstanceElementDto by elementId from the elements of an AutomationComposition.
     *
     * @param automationComposition the AutomationComposition
     * @return the Map of InstanceElementDto
     */
    public Map<UUID, InstanceElementDto> getInstanceElementDtoMap(AutomationComposition automationComposition) {
        Map<UUID, InstanceElementDto> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            var instanceElement = new InstanceElementDto(automationComposition.getInstanceId(), element.getId(),
                    element.getProperties(), element.getOutProperties());
            map.put(element.getId(), instanceElement);
        }
        return map;
    }

    /**
     * Create a new InstanceElementDto record with state New.
     *
     * @param instanceElement the InstanceElementDto
     * @return a new InstanceElementDto
     */
    public static InstanceElementDto changeStateToNew(InstanceElementDto instanceElement) {
        return new InstanceElementDto(instanceElement.instanceId(), instanceElement.elementId(),
                instanceElement.inProperties(), instanceElement.outProperties(), ElementState.NEW);
    }

    /**
     * Create a new CompositionElementDto record with state New.
     *
     * @param compositionElement the CompositionElementDto
     * @return a new CompositionElementDto
     */
    public static CompositionElementDto changeStateToNew(CompositionElementDto compositionElement) {
        return new CompositionElementDto(compositionElement.compositionId(), compositionElement.elementDefinitionId(),
                compositionElement.inProperties(), compositionElement.outProperties(), ElementState.NEW);
    }

    /**
     * Check composition is present and compare the last update.
     *
     * @param compositionId the instanceId
     * @param revisionId the last Update
     * @return true if the composition is updated
     */
    public boolean isCompositionDefinitionUpdated(UUID compositionId, UUID revisionId) {
        if (revisionId == null) {
            // old ACM-r
            return true;
        }
        var acDefinition = acElementsDefinitions.get(compositionId);
        if (acDefinition == null) {
            return false;
        }
        return revisionId.equals(acDefinition.getRevisionId());
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
}
