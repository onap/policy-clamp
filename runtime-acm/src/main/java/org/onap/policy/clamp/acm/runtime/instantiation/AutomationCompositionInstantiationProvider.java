/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2023 Nordix Foundation.
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
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.participants.AcmParticipantProvider;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.NodeTemplateState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is dedicated to the Instantiation of Commissioned automation composition.
 */
@Service
@Transactional
@AllArgsConstructor
public class AutomationCompositionInstantiationProvider {
    private static final String DO_NOT_MATCH = " do not match with ";

    private final AutomationCompositionProvider automationCompositionProvider;
    private final AcDefinitionProvider acDefinitionProvider;
    private final AcInstanceStateResolver acInstanceStateResolver;
    private final SupervisionAcHandler supervisionAcHandler;
    private final AcmParticipantProvider acmParticipantProvider;

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
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    automationComposition.getCompositionId() + DO_NOT_MATCH + compositionId);
        }
        var checkAutomationCompositionOpt =
                automationCompositionProvider.findAutomationComposition(automationComposition.getKey().asIdentifier());
        if (checkAutomationCompositionOpt.isPresent()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    automationComposition.getKey().asIdentifier() + " already defined");
        }

        var validationResult = validateAutomationComposition(automationComposition);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        automationComposition = automationCompositionProvider.createAutomationComposition(automationComposition);

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
        var response = new InstantiationResponse();
        var instanceId = automationComposition.getInstanceId();
        var acToUpdate = automationCompositionProvider.getAutomationComposition(instanceId);
        if (!compositionId.equals(acToUpdate.getCompositionId())) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
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
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
            }
            automationComposition = automationCompositionProvider.updateAutomationComposition(acToUpdate);
            response.setInstanceId(instanceId);
            response.setAffectedAutomationComposition(automationComposition.getKey().asIdentifier());
            return response;

        } else if ((DeployState.DEPLOYED.equals(acToUpdate.getDeployState())
                || DeployState.UPDATING.equals(acToUpdate.getDeployState()))
                && LockState.LOCKED.equals(acToUpdate.getLockState())) {
            return updateDeployedAutomationComposition(compositionId, automationComposition, acToUpdate);
        }
        throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                "Not allowed to update in the state " + acToUpdate.getDeployState());
    }

    /**
     * Update deployed AC Element properties.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param automationComposition the automation composition
     * @param acToBeUpdated the composition to be updated
     * @return the result of the update
     */
    public InstantiationResponse updateDeployedAutomationComposition(UUID compositionId,
            AutomationComposition automationComposition, AutomationComposition acToBeUpdated) {

        // Iterate and update the element property values
        for (var dbAcElement : acToBeUpdated.getElements().entrySet()) {
            var elementId = dbAcElement.getKey();
            if (automationComposition.getElements().containsKey(elementId)) {
                dbAcElement.getValue().getProperties()
                        .putAll(automationComposition.getElements().get(elementId).getProperties());
            }
        }
        if (automationComposition.getRestarting() != null) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, "There is a restarting process, Update not allowed");
        }
        var validationResult = validateAutomationComposition(acToBeUpdated);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }

        // Publish property update event to the participants
        supervisionAcHandler.update(acToBeUpdated);

        automationComposition = automationCompositionProvider.updateAutomationComposition(acToBeUpdated);
        var response = new InstantiationResponse();
        var instanceId = automationComposition.getInstanceId();
        response.setInstanceId(instanceId);
        response.setAffectedAutomationComposition(automationComposition.getKey().asIdentifier());
        return response;
    }

    /**
     * Validate AutomationComposition.
     *
     * @param automationComposition AutomationComposition to validate
     * @return the result of validation
     */
    private BeanValidationResult validateAutomationComposition(AutomationComposition automationComposition) {

        var result = new BeanValidationResult("AutomationComposition", automationComposition);
        var acDefinitionOpt = acDefinitionProvider.findAcDefinition(automationComposition.getCompositionId());
        if (acDefinitionOpt.isEmpty()) {
            result.addResult(new ObjectValidationResult("ServiceTemplate", "", ValidationStatus.INVALID,
                    "Commissioned automation composition definition not found"));
            return result;
        }
        if (!AcTypeState.PRIMED.equals(acDefinitionOpt.get().getState())) {
            result.addResult(new ObjectValidationResult("ServiceTemplate.state", acDefinitionOpt.get().getState(),
                    ValidationStatus.INVALID, "Commissioned automation composition definition not primed"));
            return result;
        }
        if (acDefinitionOpt.get().getRestarting() != null) {
            result.addResult(
                    new ObjectValidationResult("ServiceTemplate.restarting", acDefinitionOpt.get().getRestarting(),
                            ValidationStatus.INVALID, "There is a restarting process in composition"));
            return result;
        }
        var participantIds = acDefinitionOpt.get().getElementStateMap().values().stream()
                .map(NodeTemplateState::getParticipantId).collect(Collectors.toSet());

        acmParticipantProvider.verifyParticipantState(participantIds);

        result.addResult(AcmUtils.validateAutomationComposition(automationComposition,
                acDefinitionOpt.get().getServiceTemplate()));

        if (result.isValid()) {
            for (var element : automationComposition.getElements().values()) {
                var name = element.getDefinition().getName();
                var participantId = acDefinitionOpt.get().getElementStateMap().get(name).getParticipantId();
                element.setParticipantId(participantId);
            }
        }

        return result;
    }

    /**
     * Get Automation Composition.
     *
     * @param compositionId The UUID of the automation composition definition
     * @param instanceId The UUID of the automation composition instance
     * @return the Automation Composition
     */
    @Transactional(readOnly = true)
    public AutomationComposition getAutomationComposition(UUID compositionId, UUID instanceId) {
        var automationComposition = automationCompositionProvider.getAutomationComposition(instanceId);
        if (!automationComposition.getCompositionId().equals(compositionId)) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
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
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    automationComposition.getCompositionId() + DO_NOT_MATCH + compositionId);
        }
        if (!DeployState.UNDEPLOYED.equals(automationComposition.getDeployState())
                && !DeployState.DELETING.equals(automationComposition.getDeployState())) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    "Automation composition state is still " + automationComposition.getDeployState());
        }
        if (DeployState.DELETING.equals(automationComposition.getDeployState())
                && StateChangeResult.NO_ERROR.equals(automationComposition.getStateChangeResult())) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    "Automation composition state is still " + automationComposition.getDeployState());
        }
        if (automationComposition.getRestarting() != null) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, "There is a restarting process, Delete not allowed");
        }
        var acDefinition = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId());
        if (acDefinition != null) {
            var participantIds = acDefinition.getElementStateMap().values().stream()
                    .map(NodeTemplateState::getParticipantId).collect(Collectors.toSet());
            acmParticipantProvider.verifyParticipantState(participantIds);
        }
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
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    automationComposition.getCompositionId() + DO_NOT_MATCH + compositionId);
        }
        var acDefinition = acDefinitionProvider.getAcDefinition(automationComposition.getCompositionId());

        var participantIds = acDefinition.getElementStateMap().values().stream()
                .map(NodeTemplateState::getParticipantId).collect(Collectors.toSet());

        acmParticipantProvider.verifyParticipantState(participantIds);
        var result = acInstanceStateResolver.resolve(acInstanceStateUpdate.getDeployOrder(),
                acInstanceStateUpdate.getLockOrder(), automationComposition.getDeployState(),
                automationComposition.getLockState(), automationComposition.getStateChangeResult());
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

            default:
                throw new PfModelRuntimeException(Status.BAD_REQUEST, "Not valid " + acInstanceStateUpdate);
        }
    }
}
