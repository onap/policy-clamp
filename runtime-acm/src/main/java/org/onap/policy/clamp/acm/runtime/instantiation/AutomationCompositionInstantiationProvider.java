/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 Nordix Foundation.
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
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
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
     * @param compositionId The UUID of the automation composition definition
     * @param automationComposition the automation composition
     * @return the result of the instantiation operation
     */
    public InstantiationResponse createAutomationComposition(UUID compositionId,
            AutomationComposition automationComposition) {
        if (!compositionId.equals(automationComposition.getCompositionId())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    automationComposition.getCompositionId() + DO_NOT_MATCH + compositionId);
        }
        var checkAutomationCompositionOpt =
                automationCompositionProvider.findAutomationComposition(automationComposition.getKey().asIdentifier());
        if (checkAutomationCompositionOpt.isPresent()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    automationComposition.getKey().asIdentifier() + " already defined");
        }

        var validationResult = validateAutomationComposition(automationComposition);
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
     * @param compositionId The UUID of the automation composition definition
     * @param automationComposition the automation composition
     * @return the result of the update
     */
    public InstantiationResponse updateAutomationComposition(UUID compositionId,
            AutomationComposition automationComposition) {
        var instanceId = automationComposition.getInstanceId();
        var acToUpdate = automationCompositionProvider.getAutomationComposition(instanceId);
        if (!compositionId.equals(acToUpdate.getCompositionId())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    automationComposition.getCompositionId() + DO_NOT_MATCH + compositionId);
        }
        if (DeployState.UNDEPLOYED.equals(acToUpdate.getDeployState())) {
            acToUpdate.setElements(automationComposition.getElements());
            acToUpdate.setName(automationComposition.getName());
            acToUpdate.setVersion(automationComposition.getVersion());
            acToUpdate.setDescription(automationComposition.getDescription());
            acToUpdate.setDerivedFrom(automationComposition.getDerivedFrom());
            var validationResult = validateAutomationComposition(acToUpdate);
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
            case "UPDATE" -> updateDeployedAutomationComposition(automationComposition, acToUpdate);

            case "MIGRATE" -> migrateAutomationComposition(automationComposition, acToUpdate);

            case "MIGRATE_PRECHECK" -> migratePrecheckAc(automationComposition, acToUpdate);

            default -> throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    "Not allowed to " + deployOrder + " in the state " + acToUpdate.getDeployState());
        };
    }

    /**
     * Update deployed AC Element properties.
     *
     * @param automationComposition the automation composition
     * @param acToBeUpdated the composition to be updated
     * @return the result of the update
     */
    private InstantiationResponse updateDeployedAutomationComposition(
            AutomationComposition automationComposition, AutomationComposition acToBeUpdated) {

        // Iterate and update the element property values
        for (var element : automationComposition.getElements().entrySet()) {
            var elementId = element.getKey();
            var dbAcElement = acToBeUpdated.getElements().get(elementId);
            if (dbAcElement == null) {
                throw new PfModelRuntimeException(Status.BAD_REQUEST, ELEMENT_ID_NOT_PRESENT + elementId);
            }
            AcmUtils.recursiveMerge(dbAcElement.getProperties(), element.getValue().getProperties());
        }

        var validationResult = validateAutomationComposition(acToBeUpdated);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        }
        // Publish property update event to the participants
        supervisionAcHandler.update(acToBeUpdated);

        encryptInstanceProperties(acToBeUpdated, acToBeUpdated.getCompositionId());

        automationComposition = automationCompositionProvider.updateAutomationComposition(acToBeUpdated);
        return createInstantiationResponse(automationComposition);
    }

    private InstantiationResponse migrateAutomationComposition(
        AutomationComposition automationComposition, AutomationComposition acToBeUpdated) {

        if (!DeployState.DEPLOYED.equals(acToBeUpdated.getDeployState())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                "Not allowed to migrate in the state " + acToBeUpdated.getDeployState());
        }

        // Iterate and update the element property values
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
                checkCompatibility(newDefinition, dbElementDefinition, automationComposition.getInstanceId());
                dbAcElement.setDefinition(element.getValue().getDefinition());
            }
        }
        // Remove element which is not present in the new Ac instance
        var elementsRemoved = getElementRemoved(acToBeUpdated, automationComposition);
        elementsRemoved.forEach(uuid -> acToBeUpdated.getElements().remove(uuid));

        var validationResult =
                validateAutomationComposition(acToBeUpdated, automationComposition.getCompositionTargetId());
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        }
        acToBeUpdated.setCompositionTargetId(automationComposition.getCompositionTargetId());
        var acDefinition = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionTargetId());
        // Publish migrate event to the participants
        supervisionAcHandler.migrate(acToBeUpdated, acDefinition.getServiceTemplate());

        encryptInstanceProperties(acToBeUpdated, acToBeUpdated.getCompositionTargetId());

        var ac = automationCompositionProvider.updateAutomationComposition(acToBeUpdated);
        elementsRemoved.forEach(automationCompositionProvider::deleteAutomationCompositionElement);
        return createInstantiationResponse(ac);
    }

    private List<UUID> getElementRemoved(AutomationComposition acFromDb, AutomationComposition acFromMigration) {
        return acFromDb.getElements().keySet().stream()
                .filter(id -> acFromMigration.getElements().get(id) == null).toList();
    }

    void checkCompatibility(PfConceptKey newDefinition, PfConceptKey dbElementDefinition,
                            UUID instanceId) {
        var compatibility = newDefinition.getCompatibility(dbElementDefinition);
        if (PfKey.Compatibility.DIFFERENT.equals(compatibility)) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    dbElementDefinition + " is not compatible with " + newDefinition);
        }
        if (PfKey.Compatibility.MAJOR.equals(compatibility) || PfKey.Compatibility.MINOR
                .equals(compatibility)) {
            LOGGER.warn("Migrate {}: Version {} has {} compatibility with {} ", instanceId, newDefinition,
                    compatibility, dbElementDefinition);
        }
    }

    private InstantiationResponse migratePrecheckAc(
            AutomationComposition automationComposition, AutomationComposition acToBeUpdated) {

        acToBeUpdated.setPrecheck(true);
        var copyAc = new AutomationComposition(acToBeUpdated);
        // Iterate and update the element property values
        for (var element : automationComposition.getElements().entrySet()) {
            var elementId = element.getKey();
            var copyElement = copyAc.getElements().get(elementId);
            // Add additional elements if present for migration
            if (copyElement == null) {
                LOGGER.info("New Ac element {} added in Migration", elementId);
                copyAc.getElements().put(elementId, element.getValue());
            } else {
                AcmUtils.recursiveMerge(copyElement.getProperties(), element.getValue().getProperties());
                var newDefinition = element.getValue().getDefinition().asConceptKey();
                var copyElementDefinition = copyElement.getDefinition().asConceptKey();
                checkCompatibility(newDefinition, copyElementDefinition, automationComposition.getInstanceId());
                copyElement.setDefinition(element.getValue().getDefinition());
            }
        }
        // Remove element which is not present in the new Ac instance
        var elementsRemoved = getElementRemoved(copyAc, automationComposition);
        elementsRemoved.forEach(uuid -> copyAc.getElements().remove(uuid));

        var validationResult =
                validateAutomationComposition(copyAc, automationComposition.getCompositionTargetId());
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, validationResult.getResult());
        }
        copyAc.setCompositionTargetId(automationComposition.getCompositionTargetId());

        // Publish migrate event to the participants
        supervisionAcHandler.migratePrecheck(copyAc);

        AcmUtils.setCascadedState(acToBeUpdated, DeployState.DEPLOYED, LockState.LOCKED,
            SubState.MIGRATION_PRECHECKING);
        acToBeUpdated.setStateChangeResult(StateChangeResult.NO_ERROR);

        return createInstantiationResponse(automationCompositionProvider.updateAutomationComposition(acToBeUpdated));
    }

    private BeanValidationResult validateAutomationComposition(AutomationComposition automationComposition) {
        return validateAutomationComposition(automationComposition, automationComposition.getCompositionId());
    }

    /**
     * Validate AutomationComposition.
     *
     * @param automationComposition AutomationComposition to validate
     * @param compositionId the composition id
     * @return the result of validation
     */
    private BeanValidationResult validateAutomationComposition(AutomationComposition automationComposition,
            UUID compositionId) {

        var result = new BeanValidationResult("AutomationComposition", automationComposition);
        var acDefinitionOpt = acDefinitionProvider.findAcDefinition(compositionId);
        if (acDefinitionOpt.isEmpty()) {
            result.addResult(new ObjectValidationResult("ServiceTemplate", compositionId, ValidationStatus.INVALID,
                    "Commissioned automation composition definition not found"));
            return result;
        }
        if (!AcTypeState.PRIMED.equals(acDefinitionOpt.get().getState())) {
            result.addResult(new ObjectValidationResult("ServiceTemplate.state", acDefinitionOpt.get().getState(),
                    ValidationStatus.INVALID, "Commissioned automation composition definition not primed"));
            return result;
        }
        var participantIds = acDefinitionOpt.get().getElementStateMap().values().stream()
                .map(NodeTemplateState::getParticipantId).collect(Collectors.toSet());

        participantProvider.verifyParticipantState(participantIds);

        result.addResult(AcmUtils.validateAutomationComposition(automationComposition,
                acDefinitionOpt.get().getServiceTemplate(),
                acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName()));

        result.addResult(automationCompositionProvider.validateElementIds(automationComposition));

        if (result.isValid()) {
            for (var element : automationComposition.getElements().values()) {
                var name = element.getDefinition().getName();
                var participantId = acDefinitionOpt.get().getElementStateMap().get(name).getParticipantId();
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
     * @param instanceId The UUID of the automation composition instance
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
     * @param instanceId The UUID of the automation composition instance
     * @return the result of the deletion
     */
    public InstantiationResponse deleteAutomationComposition(UUID compositionId, UUID instanceId) {
        var automationComposition = automationCompositionProvider.getAutomationComposition(instanceId);
        if (!compositionId.equals(automationComposition.getCompositionId())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    automationComposition.getCompositionId() + DO_NOT_MATCH + compositionId);
        }
        if (!DeployState.UNDEPLOYED.equals(automationComposition.getDeployState())
                && !DeployState.DELETING.equals(automationComposition.getDeployState())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    "Automation composition state is still " + automationComposition.getDeployState());
        }
        if (DeployState.DELETING.equals(automationComposition.getDeployState())
                && StateChangeResult.NO_ERROR.equals(automationComposition.getStateChangeResult())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    "Automation composition state is still " + automationComposition.getDeployState());
        }
        var acDefinition = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId());
        var participantIds = acDefinition.getElementStateMap().values().stream()
            .map(NodeTemplateState::getParticipantId).collect(Collectors.toSet());
        participantProvider.verifyParticipantState(participantIds);
        supervisionAcHandler.delete(automationComposition, acDefinition);
        var response = new InstantiationResponse();
        response.setInstanceId(automationComposition.getInstanceId());
        response.setAffectedAutomationComposition(automationComposition.getKey().asIdentifier());
        return response;
    }

    /**
     * Get the requested automation compositions.
     *
     * @param name the name of the automation composition to get, null for all automation compositions
     * @param version the version of the automation composition to get, null for all automation compositions
     * @return the automation compositions
     */
    @Transactional(readOnly = true)
    public AutomationCompositions getAutomationCompositions(UUID compositionId, String name, String version) {
        var automationCompositions = new AutomationCompositions();
        automationCompositions.setAutomationCompositionList(
                automationCompositionProvider.getAutomationCompositions(compositionId, name, version));

        return automationCompositions;
    }

    /**
     * Handle Composition Instance State.
     *
     * @param compositionId the compositionId
     * @param instanceId the instanceId
     * @param acInstanceStateUpdate the AcInstanceStateUpdate
     */
    public void compositionInstanceState(UUID compositionId, UUID instanceId,
            @Valid AcInstanceStateUpdate acInstanceStateUpdate) {
        var automationComposition = automationCompositionProvider.getAutomationComposition(instanceId);
        if (!compositionId.equals(automationComposition.getCompositionId())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    automationComposition.getCompositionId() + DO_NOT_MATCH + compositionId);
        }
        var acDefinition = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId());

        var participantIds = acDefinition.getElementStateMap().values().stream()
                .map(NodeTemplateState::getParticipantId).collect(Collectors.toSet());

        participantProvider.verifyParticipantState(participantIds);
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
                supervisionAcHandler.prepare(automationComposition);
                break;

            case "REVIEW":
                supervisionAcHandler.review(automationComposition);
                break;

            default:
                var msg = String.format(NOT_VALID_ORDER, acInstanceStateUpdate,
                        automationComposition.getDeployState(), automationComposition.getLockState(),
                        automationComposition.getSubState(), automationComposition.getStateChangeResult());
                throw new PfModelRuntimeException(Status.BAD_REQUEST, msg);
        }
    }
}
