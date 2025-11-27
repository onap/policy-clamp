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
import org.onap.policy.clamp.models.acm.concepts.MigrationState;
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
import org.onap.policy.clamp.models.acm.utils.AcmStageUtils;
import org.onap.policy.clamp.models.acm.utils.AcmStateUtils;
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
        LOGGER.info("Create instance request received for compositionId {}", compositionId);
        AutomationCompositionProvider.validateInstanceEndpoint(compositionId, automationComposition);
        automationCompositionProvider.validateNameVersion(automationComposition.getKey().asIdentifier());

        var acDefinition = acDefinitionProvider.getAcDefinition(compositionId);
        AcDefinitionProvider.checkPrimedComposition(acDefinition);
        var validationResult = validateAutomationComposition(automationComposition, acDefinition, 0);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        } else {
            associateParticipantId(automationComposition, acDefinition, null);
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
        var acFromDb = automationCompositionProvider.getAutomationComposition(instanceId);
        AutomationCompositionProvider.validateInstanceEndpoint(compositionId, acFromDb);
        var acDefinition = acDefinitionProvider.getAcDefinition(compositionId);
        AcDefinitionProvider.checkPrimedComposition(acDefinition);
        AcmUtils.checkMigrationState(acFromDb);
        if (DeployState.UNDEPLOYED.equals(acFromDb.getDeployState())) {
            LOGGER.info("Updating undeployed instance with id {}", instanceId);
            acFromDb.setElements(automationComposition.getElements());
            acFromDb.setName(automationComposition.getName());
            acFromDb.setVersion(automationComposition.getVersion());
            acFromDb.setDescription(automationComposition.getDescription());
            acFromDb.setDerivedFrom(automationComposition.getDerivedFrom());
            var validationResult = validateAutomationComposition(acFromDb, acDefinition, 0);
            if (!validationResult.isValid()) {
                throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
            } else {
                associateParticipantId(acFromDb, acDefinition, null);
            }
            encryptInstanceProperties(acFromDb, compositionId);
            automationComposition = automationCompositionProvider.updateAutomationComposition(acFromDb);
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
            acFromDb.getDeployState(), acFromDb.getLockState(), acFromDb.getSubState(),
            acFromDb.getStateChangeResult());
        return switch (result) {
            case "UPDATE" -> updateDeployedAutomationComposition(automationComposition, acFromDb, acDefinition);

            case "MIGRATE" -> migrateAutomationComposition(automationComposition, acFromDb, acDefinition);

            case "MIGRATE_PRECHECK" -> migratePrecheckAc(automationComposition, acFromDb, acDefinition);

            default -> throw new PfModelRuntimeException(Status.BAD_REQUEST,
                "Not allowed to " + deployOrder + " in the state " + acFromDb.getDeployState());
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
        LOGGER.info("Updating deployed instance with id {}", automationComposition.getInstanceId());

        // Iterate and update the element property values
        for (var element : automationComposition.getElements().entrySet()) {
            var elementId = element.getKey();
            var dbAcElement = acToBeUpdated.getElements().get(elementId);
            if (dbAcElement == null) {
                throw new PfModelRuntimeException(Status.BAD_REQUEST, ELEMENT_ID_NOT_PRESENT + elementId);
            }
            AcmUtils.recursiveMerge(dbAcElement.getProperties(), element.getValue().getProperties());
        }

        var validationResult = validateAutomationComposition(acToBeUpdated, acDefinition, 0);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        } else {
            associateParticipantId(acToBeUpdated, acDefinition, null);
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
            AutomationComposition automationComposition, AutomationComposition acFromDb,
            AutomationCompositionDefinition acDefinition) {
        LOGGER.info("Migrating instance with id {}", automationComposition.getInstanceId());
        if (!DeployState.DEPLOYED.equals(acFromDb.getDeployState())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                "Not allowed to migrate in the state " + acFromDb.getDeployState());
        }
        // make copy for rollback
        automationCompositionProvider.copyAcElementsBeforeUpdate(acFromDb);

        var acDefinitionTarget = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionTargetId());
        AcDefinitionProvider.checkPrimedComposition(acDefinitionTarget);
        // Iterate and update the element property values
        updateElementsProperties(automationComposition, acFromDb, acDefinitionTarget, acDefinition);

        updateAcForMigration(acFromDb, acDefinitionTarget, DeployState.MIGRATING);

        var acToPublish = new AutomationComposition(acFromDb);
        encryptInstanceProperties(acFromDb, acFromDb.getCompositionTargetId());

        var ac = automationCompositionProvider.updateAutomationComposition(acFromDb);

        // Publish migrate event to the participants
        supervisionAcHandler.migrate(acToPublish, acDefinition.getRevisionId(), acDefinitionTarget.getRevisionId());
        return createInstantiationResponse(ac);
    }

    private void updateAcForMigration(AutomationComposition acFromDb,
                                      AutomationCompositionDefinition acDefinition, DeployState deployState) {
        AcmStateUtils.setCascadedState(acFromDb, deployState, LockState.LOCKED);
        acFromDb.setStateChangeResult(StateChangeResult.NO_ERROR);
        var stage = DeployState.MIGRATION_REVERTING.equals(deployState)
                ?  AcmStageUtils.getLastStage(acFromDb, acDefinition.getServiceTemplate())
                : AcmStageUtils.getFirstStage(acFromDb, acDefinition.getServiceTemplate());
        acFromDb.setPhase(stage);
    }

    private void updateAcForProperties(AutomationComposition acToBeUpdated) {
        AcmStateUtils.setCascadedState(acToBeUpdated, DeployState.UPDATING, acToBeUpdated.getLockState());
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
        LOGGER.info("Running migrate precheck for id: {}", automationComposition.getInstanceId());
        var copyAc = new AutomationComposition(acToBeUpdated);
        var acDefinitionTarget = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionTargetId());
        AcDefinitionProvider.checkPrimedComposition(acDefinitionTarget);
        // Iterate and update the element property values
        updateElementsProperties(automationComposition, copyAc, acDefinitionTarget, acDefinition);

        // Publish migrate event to the participants
        supervisionAcHandler.migratePrecheck(copyAc, acDefinition.getRevisionId(), acDefinitionTarget.getRevisionId());

        AcmStateUtils.setCascadedState(acToBeUpdated, DeployState.DEPLOYED, LockState.LOCKED,
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
            AutomationCompositionDefinition acDefinition, int removeElement) {
        var result = new BeanValidationResult("AutomationComposition", automationComposition);
        participantProvider.checkRegisteredParticipant(acDefinition);
        result.addResult(AcmUtils.validateAutomationComposition(automationComposition,
                acDefinition.getServiceTemplate(),
            acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName(), removeElement));

        result.addResult(automationCompositionProvider.validateElementIds(automationComposition));

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
        LOGGER.info("Delete automation composition request received for name: {} and version: {}",
                automationComposition.getName(), automationComposition.getVersion());
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
        LOGGER.info("Rollback automation composition request received for CompositionID: {} and InstanceID: {}",
                compositionId, instanceId);
        var acFromDb = automationCompositionProvider.getAutomationComposition(instanceId);
        AutomationCompositionProvider.validateInstanceEndpoint(compositionId, acFromDb);

        if (!DeployOrder.MIGRATION_REVERT.name().equals(acInstanceStateResolver.resolve(
                DeployOrder.MIGRATION_REVERT, LockOrder.NONE,
                SubOrder.NONE, acFromDb.getDeployState(), acFromDb.getLockState(),
                acFromDb.getSubState(), acFromDb.getStateChangeResult()))) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, "Invalid state for rollback");
        }

        var automationCompositionToRollback =
            automationCompositionProvider.getAutomationCompositionRollback(instanceId);
        var acFromDbCopy = new AutomationComposition(acFromDb);
        acFromDbCopy.setElements(automationCompositionToRollback.getElements().values().stream()
                .collect(Collectors.toMap(AutomationCompositionElement::getId, AutomationCompositionElement::new)));

        var acDefinition = acDefinitionProvider.getAcDefinition(acFromDbCopy.getCompositionId());
        var validationResult = validateAutomationComposition(acFromDbCopy, acDefinition, 0);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        }
        // Include new elements from migration for the participant undeploy
        for (var element : acFromDb.getElements().values()) {
            if (MigrationState.NEW.equals(element.getMigrationState())) {
                acFromDbCopy.getElements().put(element.getId(), element);
            }
            if (MigrationState.REMOVED.equals(element.getMigrationState())) {
                acFromDbCopy.getElements().get(element.getId()).setMigrationState(MigrationState.REMOVED);
            }
        }

        updateAcForMigration(acFromDbCopy, acDefinition, DeployState.MIGRATION_REVERTING);
        automationCompositionProvider.updateAutomationComposition(acFromDbCopy);
        var acDefinitionTarget = acDefinitionProvider.getAcDefinition(acFromDbCopy.getCompositionTargetId());
        supervisionAcHandler.migrate(acFromDbCopy, acDefinition.getRevisionId(), acDefinitionTarget.getRevisionId());
    }

    private void updateElementsProperties(AutomationComposition automationComposition,
            AutomationComposition acFromDb, AutomationCompositionDefinition acDefinitionTarget,
                                          AutomationCompositionDefinition acDefinition) {
        for (var element : automationComposition.getElements().entrySet()) {
            var elementId = element.getKey();
            var dbAcElement = acFromDb.getElements().get(elementId);
            if (dbAcElement == null) {
                LOGGER.info("New Ac element {} added in Migration", elementId);
                element.getValue().setMigrationState(MigrationState.NEW);
                acFromDb.getElements().put(elementId, element.getValue());
            } else {
                AcmUtils.recursiveMerge(dbAcElement.getProperties(), element.getValue().getProperties());
                var newDefinition = element.getValue().getDefinition().asConceptKey();
                var dbElementDefinition = dbAcElement.getDefinition().asConceptKey();
                AutomationCompositionProvider.checkCompatibility(
                        newDefinition, dbElementDefinition, automationComposition.getInstanceId());
                dbAcElement.setDefinition(element.getValue().getDefinition());
            }
        }
        // Update migrationState for the removed elements
        var elementsRemoved = getElementRemoved(acFromDb, automationComposition);
        elementsRemoved.forEach(element -> acFromDb.getElements().get(element.getId())
                .setMigrationState(MigrationState.REMOVED));

        var validationResult = validateAutomationComposition(acFromDb, acDefinitionTarget, elementsRemoved.size());
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        } else {
            associateParticipantId(acFromDb, acDefinitionTarget, acDefinition);
        }
        acFromDb.setCompositionTargetId(automationComposition.getCompositionTargetId());
    }

    private void associateParticipantId(AutomationComposition acFromDb,
                                        AutomationCompositionDefinition acDefinitionTarget,
                                        AutomationCompositionDefinition oldAcDefinition) {
        for (var element : acFromDb.getElements().values()) {
            var name = element.getDefinition().getName();
            var migrationState = element.getMigrationState();
            if (MigrationState.DEFAULT.equals(migrationState) || MigrationState.NEW.equals(migrationState)) {
                var participantId = acDefinitionTarget.getElementStateMap().get(name).getParticipantId();
                element.setParticipantId(participantId);
            } else if (MigrationState.REMOVED.equals(migrationState) && oldAcDefinition != null) {
                var participantId = oldAcDefinition.getElementStateMap().get(name).getParticipantId();
                element.setParticipantId(participantId);
            }
        }
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
     * @param instanceIds a list of instance UUIDs
     * @param stateChangeResults a list of StateChangeResult values to filter the AutomationComposition instances
     * @param deployStates a list of DeployState values to filter the AutomationComposition instances
     * @param pageable the pagination information including page size and page number
     * @return a list of AutomationComposition instances that match the specified filters
     */
    public AutomationCompositions getAcInstancesByFilter(
        final String instanceIds, final String stateChangeResults, final String deployStates,
        final Pageable pageable) {

        LOGGER.info("Get automation compositions request received with filters");
        List<String> acIds = new ArrayList<>();
        if (instanceIds != null) {
            Arrays.stream(instanceIds.split(",")).forEach(acId -> acIds.add(acId.trim()));
        }

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

        var instances = automationCompositionProvider.getAcInstancesByFilter(acIds, stateChangeResultList,
            deployStateList, pageable);
        return new AutomationCompositions(instances);
    }
}
