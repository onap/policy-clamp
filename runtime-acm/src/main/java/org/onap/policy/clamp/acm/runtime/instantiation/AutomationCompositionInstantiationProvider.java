/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.instantiation;

import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.concepts.SubState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.SubOrder;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is dedicated to the Instantiation of Commissioned automation composition.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AutomationCompositionInstantiationProvider {
    private static final String DO_NOT_MATCH = " do not match with ";
    private static final String ELEMENT_ID_NOT_PRESENT = "Element id not present ";
    private static final String NOT_VALID_ORDER =
        "Not valid order %s; DeployState: %s; LockState: %s; SubState: %s; StateChangeResult: %s";

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationCompositionInstantiationProvider.class);

    private final AutomationCompositionProvider automationCompositionProvider;
    private final AcDefinitionProvider acDefinitionProvider;
    private final AcInstanceStateResolver acInstanceStateResolver;
    private final SupervisionAcHandler supervisionAcHandler;
    private final ParticipantProvider participantProvider;
    private final AcRuntimeParameterGroup acRuntimeParameterGroup;
    private final EncryptionUtils encryptionUtils;

    /**
     * Create automation composition.
     *
     * @param compositionId         The UUID of the automation composition definition
     * @param automationComposition the automation composition
     * @return the result of the instantiation operation
     */
    public InstantiationResponse createAutomationComposition(UUID compositionId,
                                                             AutomationComposition automationComposition) {
        AutomationCompositionProvider.validateInstanceEndpoint(compositionId, automationComposition);
        automationCompositionProvider.validateNameVersion(automationComposition.getKey().asIdentifier());

        var acDefinition = acDefinitionProvider.getAcDefinition(compositionId);
        AcDefinitionProvider.checkPrimedComposition(acDefinition);
        var validationResult = validateAutomationComposition(automationComposition, acDefinition);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        }
        encryptInstanceProperties(automationComposition, compositionId);
        automationComposition = automationCompositionProvider.createAutomationComposition(automationComposition);

        return createInstantiationResponse(automationComposition);
    }

    private InstantiationResponse createInstantiationResponse(AutomationComposition automationComposition) {
        var response = new InstantiationResponse();
        response.setInstanceId(automationComposition.getInstanceId());
        response.setAffectedAutomationComposition(automationComposition.getKey().asIdentifier());
        return response;
    }

    /**
     * Update automation composition.
     *
     * @param compositionId         The UUID of the automation composition definition
     * @param automationComposition the automation composition
     * @return the result of the update
     */
    public InstantiationResponse updateAutomationComposition(UUID compositionId,
                                                             AutomationComposition automationComposition) {
        var instanceId = automationComposition.getInstanceId();
        var acToUpdate = automationCompositionProvider.getAutomationComposition(instanceId);
        AutomationCompositionProvider.validateInstanceEndpoint(compositionId, acToUpdate);
        var acDefinition = acDefinitionProvider.getAcDefinition(compositionId);
        AcDefinitionProvider.checkPrimedComposition(acDefinition);
        if (DeployState.UNDEPLOYED.equals(acToUpdate.getDeployState())) {
            acToUpdate.setElements(automationComposition.getElements());
            acToUpdate.setName(automationComposition.getName());
            acToUpdate.setVersion(automationComposition.getVersion());
            acToUpdate.setDescription(automationComposition.getDescription());
            acToUpdate.setDerivedFrom(automationComposition.getDerivedFrom());
            var validationResult = validateAutomationComposition(acToUpdate, acDefinition);
            if (!validationResult.isValid()) {
                throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
            }
            encryptInstanceProperties(acToUpdate, compositionId);
            automationComposition = automationCompositionProvider.updateAutomationComposition(acToUpdate);
            return createInstantiationResponse(automationComposition);

        }

        var deployOrder = DeployOrder.UPDATE;
        var subOrder = SubOrder.NONE;

        if (automationComposition.getCompositionTargetId() != null) {

            if (Boolean.TRUE.equals(automationComposition.getPrecheck())) {
                subOrder = SubOrder.MIGRATE_PRECHECK;
                deployOrder = DeployOrder.NONE;
            } else {
                deployOrder = DeployOrder.MIGRATE;
            }
        }
        var result = acInstanceStateResolver.resolve(deployOrder, LockOrder.NONE, subOrder,
            acToUpdate.getDeployState(), acToUpdate.getLockState(), acToUpdate.getSubState(),
            acToUpdate.getStateChangeResult());
        return switch (result) {
            case "UPDATE" -> updateDeployedAutomationComposition(automationComposition, acToUpdate, acDefinition);

            case "MIGRATE" -> migrateAutomationComposition(automationComposition, acToUpdate, acDefinition);

            case "MIGRATE_PRECHECK" -> migratePrecheckAc(automationComposition, acToUpdate, acDefinition);

            default -> throw new PfModelRuntimeException(Status.BAD_REQUEST,
                "Not allowed to " + deployOrder + " in the state " + acToUpdate.getDeployState());
        };
    }

    /**
     * Update deployed AC Element properties.
     *
     * @param automationComposition the automation composition
     * @param acToBeUpdated         the composition to be updated
     * @return the result of the update
     */
    private InstantiationResponse updateDeployedAutomationComposition(
            AutomationComposition automationComposition, AutomationComposition acToBeUpdated,
            AutomationCompositionDefinition acDefinition) {
        // save copy in case of a rollback
        automationCompositionProvider.copyAcElementsBeforeUpdate(acToBeUpdated);

        // Iterate and update the element property values
        for (var element : automationComposition.getElements().entrySet()) {
            var elementId = element.getKey();
            var dbAcElement = acToBeUpdated.getElements().get(elementId);
            if (dbAcElement == null) {
                throw new PfModelRuntimeException(Status.BAD_REQUEST, ELEMENT_ID_NOT_PRESENT + elementId);
            }
            AcmUtils.recursiveMerge(dbAcElement.getProperties(), element.getValue().getProperties());
        }

        var validationResult = validateAutomationComposition(acToBeUpdated, acDefinition);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        }
        updateAcForProperties(acToBeUpdated);

        var acToPublish = new AutomationComposition(acToBeUpdated);
        encryptInstanceProperties(acToBeUpdated, acToBeUpdated.getCompositionId());

        automationComposition = automationCompositionProvider.updateAutomationComposition(acToBeUpdated);
        // Publish property update event to the participants
        supervisionAcHandler.update(acToPublish, acDefinition.getRevisionId());
        return createInstantiationResponse(automationComposition);
    }

    private InstantiationResponse migrateAutomationComposition(
            AutomationComposition automationComposition, AutomationComposition acToBeUpdated,
            AutomationCompositionDefinition acDefinition) {

        if (!DeployState.DEPLOYED.equals(acToBeUpdated.getDeployState())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                "Not allowed to migrate in the state " + acToBeUpdated.getDeployState());
        }
        // make copy for rollback
        automationCompositionProvider.copyAcElementsBeforeUpdate(acToBeUpdated);

        var acDefinitionTarget = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionTargetId());
        AcDefinitionProvider.checkPrimedComposition(acDefinitionTarget);
        // Iterate and update the element property values
        var elementsRemoved = updateElementsProperties(automationComposition, acToBeUpdated, acDefinitionTarget);

        updateAcForMigration(acToBeUpdated, acDefinitionTarget, DeployState.MIGRATING);

        var acToPublish = new AutomationComposition(acToBeUpdated);
        encryptInstanceProperties(acToBeUpdated, acToBeUpdated.getCompositionTargetId());

        var ac = automationCompositionProvider.updateAutomationComposition(acToBeUpdated);
        for (var element : elementsRemoved) {
            automationCompositionProvider.deleteAutomationCompositionElement(element.getId());
        }

        // Publish migrate event to the participants
        supervisionAcHandler.migrate(acToPublish, acDefinition.getRevisionId(), acDefinitionTarget.getRevisionId(),
                elementsRemoved);
        return createInstantiationResponse(ac);
    }

    private void updateAcForMigration(AutomationComposition acToBeUpdated,
                                      AutomationCompositionDefinition acDefinition, DeployState deployState) {
        AcmUtils.setCascadedState(acToBeUpdated, deployState, LockState.LOCKED);
        acToBeUpdated.setStateChangeResult(StateChangeResult.NO_ERROR);
        var stage = ParticipantUtils.getFirstStage(acToBeUpdated, acDefinition.getServiceTemplate());
        acToBeUpdated.setPhase(stage);
    }

    private void updateAcForProperties(AutomationComposition acToBeUpdated) {
        AcmUtils.setCascadedState(acToBeUpdated, DeployState.UPDATING, acToBeUpdated.getLockState());
        acToBeUpdated.setStateChangeResult(StateChangeResult.NO_ERROR);
    }

    private List<AutomationCompositionElement> getElementRemoved(AutomationComposition acFromDb,
                                                                 AutomationComposition acFromMigration) {
        return acFromDb.getElements().values().stream()
            .filter(element -> acFromMigration.getElements().get(element.getId()) == null).toList();
    }


    private InstantiationResponse migratePrecheckAc(
            AutomationComposition automationComposition, AutomationComposition acToBeUpdated,
            AutomationCompositionDefinition acDefinition) {

        acToBeUpdated.setPrecheck(true);
        var copyAc = new AutomationComposition(acToBeUpdated);
        var acDefinitionTarget = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionTargetId());
        AcDefinitionProvider.checkPrimedComposition(acDefinitionTarget);
        // Iterate and update the element property values
        var removedElements = updateElementsProperties(automationComposition, copyAc, acDefinitionTarget);

        // Publish migrate event to the participants
        supervisionAcHandler.migratePrecheck(copyAc, acDefinition.getRevisionId(), acDefinitionTarget.getRevisionId(),
                removedElements);

        AcmUtils.setCascadedState(acToBeUpdated, DeployState.DEPLOYED, LockState.LOCKED,
            SubState.MIGRATION_PRECHECKING);
        acToBeUpdated.setStateChangeResult(StateChangeResult.NO_ERROR);

        return createInstantiationResponse(automationCompositionProvider.updateAutomationComposition(acToBeUpdated));
    }

    /**
     * Validate AutomationComposition.
     *
     * @param automationComposition AutomationComposition to validate
     * @param acDefinition         the Composition Definition
     * @return the result of validation
     */
    private BeanValidationResult validateAutomationComposition(AutomationComposition automationComposition,
            AutomationCompositionDefinition acDefinition) {
        var result = new BeanValidationResult("AutomationComposition", automationComposition);
        participantProvider.checkRegisteredParticipant(acDefinition);
        result.addResult(AcmUtils.validateAutomationComposition(automationComposition,
                acDefinition.getServiceTemplate(),
            acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName()));

        result.addResult(automationCompositionProvider.validateElementIds(automationComposition));

        if (result.isValid()) {
            for (var element : automationComposition.getElements().values()) {
                var name = element.getDefinition().getName();
                var participantId = acDefinition.getElementStateMap().get(name).getParticipantId();
                element.setParticipantId(participantId);
            }
        }

        return result;
    }


    private void encryptInstanceProperties(AutomationComposition automationComposition, UUID compositionId) {
        if (encryptionUtils.encryptionEnabled()) {
            var acDefinitionOpt = acDefinitionProvider.findAcDefinition(compositionId);
            acDefinitionOpt.ifPresent(acDefinition
                -> encryptionUtils.findAndEncryptSensitiveData(acDefinition, automationComposition));
        }
    }

    /**
     * Get Automation Composition.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param instanceId    The UUID of the automation composition instance
     * @return the Automation Composition
     */
    @Transactional(readOnly = true)
    public AutomationComposition getAutomationComposition(@NonNull UUID compositionId, UUID instanceId) {
        var automationComposition = automationCompositionProvider.getAutomationComposition(instanceId);
        if (!compositionId.equals(automationComposition.getCompositionId())
            && !compositionId.equals(automationComposition.getCompositionTargetId())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                automationComposition.getCompositionId() + DO_NOT_MATCH + compositionId);
        }
        return automationComposition;
    }

    /**
     * Delete the automation composition with the given name and version.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param instanceId    The UUID of the automation composition instance
     * @return the result of the deletion
     */
    public InstantiationResponse deleteAutomationComposition(UUID compositionId, UUID instanceId) {
        var automationComposition = automationCompositionProvider.getAutomationComposition(instanceId);
        var acDefinition = getAcDefinition(compositionId, automationComposition);
        var result = acInstanceStateResolver.resolve(DeployOrder.DELETE,
            null, null,
            automationComposition.getDeployState(), automationComposition.getLockState(),
            automationComposition.getSubState(), automationComposition.getStateChangeResult());
        if (!DeployOrder.DELETE.name().equals(result)) {
            var msg = String.format(NOT_VALID_ORDER, DeployOrder.DELETE,
                automationComposition.getDeployState(), automationComposition.getLockState(),
                automationComposition.getSubState(), automationComposition.getStateChangeResult());
            throw new PfModelRuntimeException(Status.BAD_REQUEST, msg);
        }
        supervisionAcHandler.delete(automationComposition, acDefinition);
        return createInstantiationResponse(automationComposition);
    }

    /**
     * Get the requested automation compositions.
     *
     * @param name     the name of the automation composition to get, null for all automation compositions
     * @param version  the version of the automation composition to get, null for all automation compositions
     * @param pageable the Pageable
     * @return the automation compositions
     */
    @Transactional(readOnly = true)
    public AutomationCompositions getAutomationCompositions(@NonNull final UUID compositionId,
                                                            final String name, final String version,
                                                            @NonNull final Pageable pageable) {
        var automationCompositions = new AutomationCompositions();
        automationCompositions.setAutomationCompositionList(
            automationCompositionProvider.getAutomationCompositions(compositionId, name, version, pageable));

        return automationCompositions;
    }

    /**
     * Handle Composition Instance State.
     *
     * @param compositionId         the compositionId
     * @param instanceId            the instanceId
     * @param acInstanceStateUpdate the AcInstanceStateUpdate
     */
    public void compositionInstanceState(UUID compositionId, UUID instanceId,
                                         @Valid AcInstanceStateUpdate acInstanceStateUpdate) {
        var automationComposition = automationCompositionProvider.getAutomationComposition(instanceId);
        var acDefinition = getAcDefinition(compositionId, automationComposition);
        var result = acInstanceStateResolver.resolve(acInstanceStateUpdate.getDeployOrder(),
            acInstanceStateUpdate.getLockOrder(), acInstanceStateUpdate.getSubOrder(),
            automationComposition.getDeployState(), automationComposition.getLockState(),
            automationComposition.getSubState(), automationComposition.getStateChangeResult());
        switch (result) {
            case "DEPLOY":
                supervisionAcHandler.deploy(automationComposition, acDefinition);
                break;

            case "UNDEPLOY":
                supervisionAcHandler.undeploy(automationComposition, acDefinition);
                break;

            case "LOCK":
                supervisionAcHandler.lock(automationComposition, acDefinition);
                break;

            case "UNLOCK":
                supervisionAcHandler.unlock(automationComposition, acDefinition);
                break;

            case "PREPARE":
                supervisionAcHandler.prepare(automationComposition, acDefinition);
                break;

            case "REVIEW":
                supervisionAcHandler.review(automationComposition, acDefinition);
                break;

            default:
                var msg = String.format(NOT_VALID_ORDER, acInstanceStateUpdate,
                    automationComposition.getDeployState(), automationComposition.getLockState(),
                    automationComposition.getSubState(), automationComposition.getStateChangeResult());
                throw new PfModelRuntimeException(Status.BAD_REQUEST, msg);
        }
    }

    /**
     * Rollback AC Instance.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param instanceId    The UUID of the automation composition instance
     */
    public void rollback(UUID compositionId, UUID instanceId) {
        var automationComposition = automationCompositionProvider.getAutomationComposition(instanceId);
        AutomationCompositionProvider.validateInstanceEndpoint(compositionId, automationComposition);

        if (!DeployOrder.MIGRATION_REVERT.name().equals(acInstanceStateResolver.resolve(
                DeployOrder.MIGRATION_REVERT, LockOrder.NONE,
                SubOrder.NONE, automationComposition.getDeployState(), automationComposition.getLockState(),
                automationComposition.getSubState(), automationComposition.getStateChangeResult()))) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, "Invalid state for rollback");
        }

        var automationCompositionToRollback =
            automationCompositionProvider.getAutomationCompositionRollback(instanceId);
        var acToBeUpdated = new AutomationComposition(automationComposition);
        acToBeUpdated.setCompositionTargetId(automationCompositionToRollback.getCompositionId());
        acToBeUpdated.setElements(automationCompositionToRollback.getElements().values().stream()
                .collect(Collectors.toMap(AutomationCompositionElement::getId, AutomationCompositionElement::new)));

        var acDefinitionTarget = acDefinitionProvider.getAcDefinition(acToBeUpdated.getCompositionTargetId());
        var validationResult = validateAutomationComposition(acToBeUpdated, acDefinitionTarget);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        }

        updateAcForMigration(acToBeUpdated, acDefinitionTarget, DeployState.MIGRATION_REVERTING);
        var elementsRemoved = getElementRemoved(automationComposition, acToBeUpdated);
        automationCompositionProvider.updateAutomationComposition(acToBeUpdated);
        for (var element : elementsRemoved) {
            automationCompositionProvider.deleteAutomationCompositionElement(element.getId());
        }
        var acDefinition = acDefinitionProvider.getAcDefinition(acToBeUpdated.getCompositionId());
        supervisionAcHandler.migrate(acToBeUpdated, acDefinition.getRevisionId(), acDefinitionTarget.getRevisionId(),
                elementsRemoved);
    }

    private List<AutomationCompositionElement> updateElementsProperties(AutomationComposition automationComposition,
            AutomationComposition acToBeUpdated, AutomationCompositionDefinition acDefinitionTarget) {
        for (var element : automationComposition.getElements().entrySet()) {
            var elementId = element.getKey();
            var dbAcElement = acToBeUpdated.getElements().get(elementId);
            // Add additional elements if present for migration
            if (dbAcElement == null) {
                LOGGER.info("New Ac element {} added in Migration", elementId);
                acToBeUpdated.getElements().put(elementId, element.getValue());
            } else {
                AcmUtils.recursiveMerge(dbAcElement.getProperties(), element.getValue().getProperties());
                var newDefinition = element.getValue().getDefinition().asConceptKey();
                var dbElementDefinition = dbAcElement.getDefinition().asConceptKey();
                AutomationCompositionProvider.checkCompatibility(
                        newDefinition, dbElementDefinition, automationComposition.getInstanceId());
                dbAcElement.setDefinition(element.getValue().getDefinition());
            }
        }
        // Remove element which is not present in the new Ac instance
        var elementsRemoved = getElementRemoved(acToBeUpdated, automationComposition);
        elementsRemoved.forEach(element -> acToBeUpdated.getElements().remove(element.getId()));

        var validationResult = validateAutomationComposition(acToBeUpdated, acDefinitionTarget);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        }
        acToBeUpdated.setCompositionTargetId(automationComposition.getCompositionTargetId());
        return elementsRemoved;
    }

    private AutomationCompositionDefinition getAcDefinition(UUID compositionId,
                                                            AutomationComposition automationComposition) {
        AutomationCompositionProvider.validateInstanceEndpoint(compositionId, automationComposition);
        var acDefinition = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId());
        participantProvider.checkRegisteredParticipant(acDefinition);
        return acDefinition;
    }

    /**
     * Retrieves a list of AutomationComposition instances filtered by the specified state change results
     * and deployment states. The result can be paginated and sorted based on the provided parameters.
     *
     * @param stateChangeResults a list of StateChangeResult values to filter the AutomationComposition instances
     * @param deployStates a list of DeployState values to filter the AutomationComposition instances
     * @param pageable the pagination information including page size and page number
     * @return a list of AutomationComposition instances that match the specified filters
     */
    public AutomationCompositions getAcInstancesByStateResultDeployState(
        final String stateChangeResults, final String deployStates,
        final Pageable pageable) {

        List<StateChangeResult> stateChangeResultList = new ArrayList<>();
        if (stateChangeResults != null) {
            Arrays.stream(stateChangeResults.split(","))
                .forEach(stateChangeResult -> stateChangeResultList.add(StateChangeResult.valueOf(stateChangeResult)));
        }

        List<DeployState> deployStateList = new ArrayList<>();
        if (deployStates != null) {
            Arrays.stream(deployStates.split(","))
                .forEach(deployState -> deployStateList.add(DeployState.valueOf(deployState)));
        }

        var instances = automationCompositionProvider.getAcInstancesByStateResultDeployState(stateChangeResultList,
            deployStateList, pageable);
        return new AutomationCompositions(instances);
    }
}
