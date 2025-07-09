/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.utils;

import jakarta.ws.rs.core.Response;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcElementRestart;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.persistence.concepts.StringToMapConverter;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility functions used in acm-runtime and participants.
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AcmUtils {
    public static final String ENTRY = "entry ";
    private static final StringToMapConverter MAP_CONVERTER = new StringToMapConverter();

    private static final Logger LOGGER = LoggerFactory.getLogger(AcmUtils.class);

    /**
     * Checks if a NodeTemplate is an AutomationCompositionElement.
     *
     * @param nodeTemplate the ToscaNodeTemplate
     * @param toscaServiceTemplate the ToscaServiceTemplate
     * @return true if the NodeTemplate is an AutomationCompositionElement
     */
    public static boolean checkIfNodeTemplateIsAutomationCompositionElement(ToscaNodeTemplate nodeTemplate,
            ToscaServiceTemplate toscaServiceTemplate, String toscaElementName) {
        if (nodeTemplate.getType().contains(toscaElementName)) {
            return true;
        } else {
            var nodeType = toscaServiceTemplate.getNodeTypes().get(nodeTemplate.getType());
            if (nodeType != null) {
                var derivedFrom = nodeType.getDerivedFrom();
                if (derivedFrom != null) {
                    return derivedFrom.contains(toscaElementName);
                }
            }
        }
        return false;
    }

    public static ToscaConceptIdentifier getType(ToscaNodeTemplate nodeTemplate) {
        return new ToscaConceptIdentifier(nodeTemplate.getType(), nodeTemplate.getTypeVersion());
    }

    /**
     * Prepare list of ParticipantDefinition for the Priming message.
     *
     * @param acElements the extracted AcElements from ServiceTemplate
     * @param supportedElementMap supported Element Map
     */
    public static List<ParticipantDefinition> prepareParticipantPriming(
            List<Entry<String, ToscaNodeTemplate>> acElements, Map<ToscaConceptIdentifier, UUID> supportedElementMap) {

        Map<UUID, List<AutomationCompositionElementDefinition>> map = new HashMap<>();
        for (var elementEntry : acElements) {
            var type = getType(elementEntry.getValue());
            var participantId = supportedElementMap.get(type);
            if (participantId == null) {
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                        "Element Type " + type + " not supported");
            }
            var acElementDefinition = new AutomationCompositionElementDefinition();
            acElementDefinition.setAcElementDefinitionId(
                    new ToscaConceptIdentifier(elementEntry.getKey(), elementEntry.getValue().getVersion()));
            acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(elementEntry.getValue());
            map.putIfAbsent(participantId, new ArrayList<>());
            map.get(participantId).add(acElementDefinition);
        }
        return prepareParticipantPriming(map);
    }

    /**
     * Prepare ParticipantPriming.
     *
     * @param map of AutomationCompositionElementDefinition with participantId as key
     * @return list of ParticipantDefinition
     */
    public static List<ParticipantDefinition> prepareParticipantPriming(
            Map<UUID, List<AutomationCompositionElementDefinition>> map) {
        List<ParticipantDefinition> result = new ArrayList<>();
        for (var entry : map.entrySet()) {
            var participantDefinition = new ParticipantDefinition();
            participantDefinition.setParticipantId(entry.getKey());
            participantDefinition.setAutomationCompositionElementDefinitionList(entry.getValue());
            result.add(participantDefinition);
        }
        return result;
    }

    /**
     * Extract AcElements from ServiceTemplate.
     *
     * @param serviceTemplate the ToscaServiceTemplate
     * @return the list of Entry of AutomationCompositionElement
     */
    public static List<Entry<String, ToscaNodeTemplate>> extractAcElementsFromServiceTemplate(
            ToscaServiceTemplate serviceTemplate, String toscaElementName) {
        return serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().entrySet().stream().filter(
                nodeTemplateEntry -> checkIfNodeTemplateIsAutomationCompositionElement(nodeTemplateEntry.getValue(),
                        serviceTemplate, toscaElementName))
                .toList();
    }

    /**
     * Create NodeTemplateState Map.
     *
     * @param acElements extracted AcElements from ServiceTemplate.
     * @param state the AcTypeState
     * @return the NodeTemplateState Map
     */
    public static Map<String, NodeTemplateState> createElementStateMap(
            List<Entry<String, ToscaNodeTemplate>> acElements, AcTypeState state) {
        Map<String, NodeTemplateState> result = new HashMap<>(acElements.size());
        for (var entry : acElements) {
            var nodeTemplateState = new NodeTemplateState();
            nodeTemplateState.setNodeTemplateStateId(UUID.randomUUID());
            nodeTemplateState.setState(state);
            nodeTemplateState
                    .setNodeTemplateId(new ToscaConceptIdentifier(entry.getKey(), entry.getValue().getVersion()));
            result.put(entry.getKey(), nodeTemplateState);
        }
        return result;
    }

    /**
     * Validate AutomationComposition.
     *
     * @param automationComposition AutomationComposition to validate
     * @param serviceTemplate the service template
     * @return the result of validation
     */
    public static BeanValidationResult validateAutomationComposition(AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate, String toscaCompositionName) {
        var result = new BeanValidationResult(ENTRY + automationComposition.getName(), automationComposition);

        var map = getMapToscaNodeTemplates(serviceTemplate);

        var nodeTemplateGet = map.values().stream()
                .filter(nodeTemplate -> toscaCompositionName.equals(nodeTemplate.getType())).findFirst();

        if (nodeTemplateGet.isEmpty()) {
            result.addResult(new ObjectValidationResult("ToscaServiceTemplate", serviceTemplate.getName(),
                    ValidationStatus.INVALID, "Commissioned automation composition definition not consistent"));
        } else {

            var toscaNodeTemplate = nodeTemplateGet.get();
            var acElementDefinitions = getAutomationCompositionElementDefinitions(map, toscaNodeTemplate);

            // @formatter:off
            var definitions = acElementDefinitions
                    .stream()
                    .map(nodeTemplate -> nodeTemplate.getKey().asIdentifier())
                    .collect(Collectors.toMap(ToscaConceptIdentifier::getName, UnaryOperator.identity()));
            // @formatter:on

            if (definitions.size() != automationComposition.getElements().size()) {
                result.setResult(ValidationStatus.INVALID,
                        "Elements of the instance not matching with the elements of the composition");
            }

            for (var element : automationComposition.getElements().values()) {
                result.addResult(validateDefinition(definitions, element.getDefinition()));
            }
        }

        return result;

    }

    private static ValidationResult validateDefinition(Map<String, ToscaConceptIdentifier> definitions,
            ToscaConceptIdentifier definition) {
        var result = new BeanValidationResult(ENTRY + definition.getName(), definition);
        var identifier = definitions.get(definition.getName());
        if (identifier == null) {
            result.setResult(ValidationStatus.INVALID, "Not found");
        } else if (!identifier.equals(definition)) {
            result.setResult(ValidationStatus.INVALID, "Version not matching");
        }
        return (result.isClean() ? null : result);
    }

    private static Map<ToscaConceptIdentifier, ToscaNodeTemplate> getMapToscaNodeTemplates(
            ToscaServiceTemplate serviceTemplate) {
        if (serviceTemplate.getToscaTopologyTemplate() == null
                || MapUtils.isEmpty(serviceTemplate.getToscaTopologyTemplate().getNodeTemplates())) {
            return Map.of();
        }
        var list = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().values();
        return list.stream().collect(Collectors
                .toMap(node -> new ToscaConceptIdentifier(node.getName(), node.getVersion()), Function.identity()));
    }

    private static List<ToscaNodeTemplate> getAutomationCompositionElementDefinitions(
            Map<ToscaConceptIdentifier, ToscaNodeTemplate> map, ToscaNodeTemplate automationCompositionNodeTemplate) {

        if (MapUtils.isEmpty(automationCompositionNodeTemplate.getProperties())) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        var automationCompositionElements =
                (List<Map<String, String>>) automationCompositionNodeTemplate.getProperties().get("elements");

        if (CollectionUtils.isEmpty(automationCompositionElements)) {
            return Collections.emptyList();
        }

        // @formatter:off
        return automationCompositionElements
                .stream()
                .map(elementMap ->
                    map.get(new ToscaConceptIdentifier(elementMap.get("name"), elementMap.get("version"))))
            .toList();
        // @formatter:on
    }


    /**
     * Return true if DeployState, LockState and SubState are in a Transitional State.
     *
     * @param deployState the DeployState
     * @param lockState the LockState
     * @param subState the SubState
     * @return true if there is a state in a Transitional State
     */
    public static boolean isInTransitionalState(DeployState deployState, LockState lockState, SubState subState) {
        return DeployState.DEPLOYING.equals(deployState) || DeployState.UNDEPLOYING.equals(deployState)
                || LockState.LOCKING.equals(lockState) || LockState.UNLOCKING.equals(lockState)
                || DeployState.DELETING.equals(deployState) || DeployState.UPDATING.equals(deployState)
                || DeployState.MIGRATING.equals(deployState) || DeployState.MIGRATION_REVERTING.equals(deployState)
                || !SubState.NONE.equals(subState);
    }

    /**
     * Get DeployOrder from transitional DeployState.
     *
     * @param deployState the Deploy State
     * @return the DeployOrder
     */
    public static DeployOrder stateDeployToOrder(DeployState deployState) {
        return switch (deployState) {
            case DEPLOYING -> DeployOrder.DEPLOY;
            case UNDEPLOYING -> DeployOrder.UNDEPLOY;
            case DELETING -> DeployOrder.DELETE;
            default -> DeployOrder.NONE;
        };
    }

    /**
     * Get LockOrder from transitional LockState.
     *
     * @param lockState the Lock State
     * @return the LockOrder
     */
    public static LockOrder stateLockToOrder(LockState lockState) {
        if (LockState.LOCKING.equals(lockState)) {
            return LockOrder.LOCK;
        } else if (LockState.UNLOCKING.equals(lockState)) {
            return LockOrder.UNLOCK;
        }
        return LockOrder.NONE;
    }

    /**
     * Get final DeployState from transitional DeployState.
     *
     * @param deployState the DeployState
     * @return the DeployState
     */
    public static DeployState deployCompleted(DeployState deployState) {
        return switch (deployState) {
            case MIGRATING, MIGRATION_REVERTING, UPDATING, DEPLOYING -> DeployState.DEPLOYED;
            case UNDEPLOYING -> DeployState.UNDEPLOYED;
            case DELETING -> DeployState.DELETED;
            default -> deployState;
        };
    }

    /**
     * Get final LockState from transitional LockState.
     *
     * @param lockState the LockState
     * @return the LockState
     */
    public static LockState lockCompleted(DeployState deployState, LockState lockState) {
        if (LockState.LOCKING.equals(lockState) || DeployState.DEPLOYING.equals(deployState)) {
            return LockState.LOCKED;
        } else if (LockState.UNLOCKING.equals(lockState)) {
            return LockState.UNLOCKED;
        } else if (DeployState.UNDEPLOYING.equals(deployState)) {
            return LockState.NONE;
        }
        return lockState;
    }

    /**
     * Return true if transition states is Forward.
     *
     * @param deployState the DeployState
     * @param lockState the LockState
     * @return true if transition if Forward
     */
    public static boolean isForward(DeployState deployState, LockState lockState) {
        return DeployState.DEPLOYING.equals(deployState) || LockState.UNLOCKING.equals(lockState);
    }

    /**
     * Set the states on the automation composition and on all its automation composition elements.
     *
     * @param deployState the DeployState we want the automation composition to transition to
     * @param lockState the LockState we want the automation composition to transition to
     */
    public static void setCascadedState(final AutomationComposition automationComposition,
            final DeployState deployState, final LockState lockState) {
        setCascadedState(automationComposition, deployState, lockState, SubState.NONE);
    }

    /**
     /**
     * Set the states on the automation composition and on all its automation composition elements.
     *
     * @param deployState the DeployState we want the automation composition to transition to
     * @param lockState the LockState we want the automation composition to transition to
     * @param subState the SubState we want the automation composition to transition to
     */
    public static void setCascadedState(final AutomationComposition automationComposition,
        final DeployState deployState, final LockState lockState, final SubState subState) {
        automationComposition.setDeployState(deployState);
        automationComposition.setLockState(lockState);
        automationComposition.setLastMsg(TimestampHelper.now());
        automationComposition.setSubState(subState);

        if (MapUtils.isEmpty(automationComposition.getElements())) {
            return;
        }

        for (var element : automationComposition.getElements().values()) {
            element.setDeployState(deployState);
            element.setLockState(lockState);
            element.setSubState(subState);
            element.setMessage(null);
            element.setStage(null);
        }
    }

    /**
     * Create a new AcElementDeploy from an AutomationCompositionElement.
     *
     * @param element the AutomationCompositionElement
     * @param deployOrder the DeployOrder
     * @return the AcElementDeploy
     */
    public static AcElementDeploy createAcElementDeploy(AutomationCompositionElement element, DeployOrder deployOrder) {
        var acElementDeploy = new AcElementDeploy();
        acElementDeploy.setId(element.getId());
        acElementDeploy.setDefinition(new ToscaConceptIdentifier(element.getDefinition()));
        acElementDeploy.setOrderedState(deployOrder);
        acElementDeploy.setProperties(PfUtils.mapMap(element.getProperties(), UnaryOperator.identity()));
        return acElementDeploy;
    }

    /**
     * Create a list of AcElementDeploy for update/migrate message.
     *
     * @param automationComposition the AutomationComposition
     * @param deployOrder the DeployOrder
     */
    public static List<ParticipantDeploy> createParticipantDeployList(AutomationComposition automationComposition,
            DeployOrder deployOrder) {
        Map<UUID, List<AcElementDeploy>> map = new HashMap<>();
        for (var element : automationComposition.getElements().values()) {
            var acElementDeploy = createAcElementDeploy(element, deployOrder);
            map.putIfAbsent(element.getParticipantId(), new ArrayList<>());
            map.get(element.getParticipantId()).add(acElementDeploy);
        }
        List<ParticipantDeploy> participantDeploys = new ArrayList<>();
        for (var entry : map.entrySet()) {
            var participantDeploy = new ParticipantDeploy();
            participantDeploy.setParticipantId(entry.getKey());
            participantDeploy.setAcElementList(entry.getValue());
            participantDeploys.add(participantDeploy);
        }
        return participantDeploys;
    }

    /**
     * Create a new ParticipantRestartAc for restarting scenario.
     *
     * @param automationComposition the AutomationComposition
     * @param participantId the participantId of the participant restarted
     * @return the ParticipantRestartAc
     */
    public static ParticipantRestartAc createAcRestart(AutomationComposition automationComposition,
            UUID participantId) {
        var syncAc = new ParticipantRestartAc();
        syncAc.setDeployState(automationComposition.getDeployState());
        syncAc.setLockState(automationComposition.getLockState());
        syncAc.setAutomationCompositionId(automationComposition.getInstanceId());
        for (var element : automationComposition.getElements().values()) {
            if (participantId.equals(element.getParticipantId())) {
                var acElementSync = createAcElementRestart(element);
                syncAc.getAcElementList().add(acElementSync);
            }
        }
        return syncAc;
    }

    /**
     * Create a new AcElementRestart from an AutomationCompositionElement.
     *
     * @param element the AutomationCompositionElement
     * @return the AcElementRestart
     */
    public static AcElementRestart createAcElementRestart(AutomationCompositionElement element) {
        var acElementRestart = new AcElementRestart();
        acElementRestart.setId(element.getId());
        acElementRestart.setDefinition(new ToscaConceptIdentifier(element.getDefinition()));
        acElementRestart.setParticipantId(element.getParticipantId());
        acElementRestart.setDeployState(element.getDeployState());
        acElementRestart.setLockState(element.getLockState());
        acElementRestart.setOperationalState(element.getOperationalState());
        acElementRestart.setUseState(element.getUseState());
        acElementRestart.setProperties(PfUtils.mapMap(element.getProperties(), UnaryOperator.identity()));
        acElementRestart.setOutProperties(PfUtils.mapMap(element.getOutProperties(), UnaryOperator.identity()));
        return acElementRestart;
    }

    /**
     * Prepare the list of ParticipantDefinition for Participant Restarting/Sync msg.
     *
     * @param participantId the participantId
     * @param acmDefinition the AutomationCompositionDefinition
     * @param toscaElementName the ElementName
     * @return List of ParticipantDefinition
     */
    public static List<ParticipantDefinition> prepareParticipantRestarting(UUID participantId,
            AutomationCompositionDefinition acmDefinition, String toscaElementName) {
        var acElements = extractAcElementsFromServiceTemplate(acmDefinition.getServiceTemplate(),
                toscaElementName);

        // list of entry filtered by participantId
        List<Entry<String, ToscaNodeTemplate>> elementList = new ArrayList<>();
        Map<ToscaConceptIdentifier, UUID> supportedElementMap = new HashMap<>();
        for (var elementEntry : acElements) {
            var elementState = acmDefinition.getElementStateMap().get(elementEntry.getKey());
            if (participantId == null || participantId.equals(elementState.getParticipantId())) {
                supportedElementMap.put(getType(elementEntry.getValue()), elementState.getParticipantId());
                elementList.add(elementEntry);
            }
        }
        var list = prepareParticipantPriming(elementList, supportedElementMap);
        for (var participantDefinition : list) {
            for (var elementDe : participantDefinition.getAutomationCompositionElementDefinitionList()) {
                var state = acmDefinition.getElementStateMap().get(elementDe.getAcElementDefinitionId().getName());
                if (state != null) {
                    elementDe.setOutProperties(state.getOutProperties());
                }
            }
        }
        return list;
    }

    /**
     * Validated the Message field.
     *
     * @param message the message
     * @return a validated message
     */
    public static String validatedMessage(String message) {
        if (message != null && message.length() > 255) {
            LOGGER.warn("message too long {}", message);
            return message.substring(0, 255);
        }
        return message;
    }

    /**
     * Recursive Merge - checks if keys in map2 should update the values in map1.
     *
     * @param map1 Map where to merge
     * @param map2 Map with new values
     */
    @SuppressWarnings("unchecked")
    public static void recursiveMerge(Map<String, Object> map1, Map<String, Object> map2) {
        Deque<Pair<Map<String, Object>, Map<String, Object>>> stack = new ArrayDeque<>();
        stack.push(Pair.of(map1, map2));
        while (!stack.isEmpty()) {
            var pair = stack.pop();
            var mapLeft = pair.getLeft();
            var mapRight = pair.getRight();
            for (var entryRight : mapRight.entrySet()) {
                var valueLeft = mapLeft.get(entryRight.getKey());
                var valueRight = entryRight.getValue();
                if (valueLeft instanceof Map && valueRight instanceof Map) {
                    stack.push(Pair.of((Map<String, Object>) valueLeft, (Map<String, Object>) valueRight));
                } else if ((valueLeft instanceof List && valueRight instanceof List)) {
                    recursiveMerge((List<Object>) valueLeft, (List<Object>) valueRight);
                } else {
                    mapLeft.put(entryRight.getKey(), valueRight);
                }
            }
        }
    }

    /**
     * Recursive merge - checks if list2 has new values to be added to list1.
     *
     * @param list1 where to merge
     * @param list2 new values to merge
     */
    @SuppressWarnings("unchecked")
    private static void recursiveMerge(List<Object> list1, List<Object> list2) {
        var minsize = Math.min(list1.size(), list2.size());
        for (var i = 0; i < minsize; i++) {
            var value1 = list1.get(i);
            var value2 = list2.get(i);

            if (value1 instanceof Map && value2 instanceof Map) {
                recursiveMerge((Map<String, Object>) value1, (Map<String, Object>) value2);
            } else if (value1 instanceof List && value2 instanceof List) {
                recursiveMerge((List<Object>) value1, (List<Object>) value2);
            } else {
                list1.set(i, value2);
            }
        }

        for (int i = minsize; i < list2.size(); i++) {
            list1.add(list2.get(i));
        }
    }

    public static Map<String, Object> cloneMap(Map<String, Object> map) {
        var str = MAP_CONVERTER.convertToDatabaseColumn(map);
        return MAP_CONVERTER.convertToEntityAttribute(str);
    }
}
