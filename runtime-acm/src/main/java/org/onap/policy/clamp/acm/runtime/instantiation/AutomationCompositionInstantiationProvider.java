/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is dedicated to the Instantiation of Commissioned automation composition.
 */
@Service
@Transactional
@AllArgsConstructor
public class AutomationCompositionInstantiationProvider {
    private static final String AUTOMATION_COMPOSITION_NODE_ELEMENT_TYPE = "AutomationCompositionElement";

    private final AutomationCompositionProvider automationCompositionProvider;
    private final SupervisionHandler supervisionHandler;
    private final ParticipantProvider participantProvider;
    private final AcDefinitionProvider acDefinitionProvider;
    private static final String ENTRY = "entry ";

    /**
     * Create automation composition.
     *
     * @param automationComposition the automation composition
     * @return the result of the instantiation operation
     */
    public InstantiationResponse createAutomationComposition(AutomationComposition automationComposition) {

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
     * @param automationComposition the automation composition
     * @return the result of the instantiation operation
     */
    public InstantiationResponse updateAutomationComposition(AutomationComposition automationComposition) {
        var validationResult = validateAutomationComposition(automationComposition);
        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        automationCompositionProvider.updateAutomationComposition(automationComposition);

        var response = new InstantiationResponse();
        response.setInstanceId(automationComposition.getInstanceId());
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
        var serviceTemplate = acDefinitionProvider.findAcDefinition(automationComposition.getCompositionId());
        if (serviceTemplate.isEmpty()) {
            result.addResult(new ObjectValidationResult("ServiceTemplate", "", ValidationStatus.INVALID,
                    "Commissioned automation composition definition not found"));
        } else {
            result.addResult(AcmUtils.validateAutomationComposition(automationComposition, serviceTemplate.get()));
        }
        return result;
    }

    /**
     * Delete the automation composition with the given name and version.
     *
     * @param name the name of the automation composition to delete
     * @param version the version of the automation composition to delete
     * @return the result of the deletion
     */
    public InstantiationResponse deleteAutomationComposition(String name, String version) {
        var automationCompositionOpt =
                automationCompositionProvider.findAutomationComposition(new ToscaConceptIdentifier(name, version));
        if (automationCompositionOpt.isEmpty()) {
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, "Automation composition not found");
        }
        var automationComposition = automationCompositionOpt.get();
        if (!AutomationCompositionState.UNINITIALISED.equals(automationComposition.getState())) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    "Automation composition state is still " + automationComposition.getState());
        }
        var response = new InstantiationResponse();
        automationComposition =
                automationCompositionProvider.deleteAutomationComposition(automationComposition.getInstanceId());
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
    public AutomationCompositions getAutomationCompositions(String name, String version) {
        var automationCompositions = new AutomationCompositions();
        automationCompositions
                .setAutomationCompositionList(automationCompositionProvider.getAutomationCompositions(name, version));

        return automationCompositions;
    }

    /**
     * Issue a command to automation compositions, setting their ordered state.
     *
     * @param command the command to issue to automation compositions
     * @return the result of the initiation command
     * @throws AutomationCompositionException on ordered state invalid
     */
    public InstantiationResponse issueAutomationCompositionCommand(InstantiationCommand command)
            throws AutomationCompositionException {

        if (command.getOrderedState() == null) {
            throw new AutomationCompositionException(Status.BAD_REQUEST,
                    "ordered state invalid or not specified on command");
        }

        var participants = participantProvider.getParticipants();
        if (participants.isEmpty()) {
            throw new AutomationCompositionException(Status.BAD_REQUEST, "No participants registered");
        }
        var automationCompositionOpt =
                automationCompositionProvider.findAutomationComposition(command.getAutomationCompositionIdentifier());
        if (automationCompositionOpt.isEmpty()) {
            throw new AutomationCompositionException(Response.Status.BAD_REQUEST,
                    "AutomationComposition with id " + command.getAutomationCompositionIdentifier() + " not found");
        }

        var automationComposition = automationCompositionOpt.get();
        var validationResult = validateIssueAutomationComposition(automationComposition, participants);
        if (!validationResult.isValid()) {
            throw new AutomationCompositionException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }

        automationComposition.setCascadedOrderedState(command.getOrderedState());
        supervisionHandler.triggerAutomationCompositionSupervision(automationComposition);
        automationCompositionProvider.updateAutomationComposition(automationComposition);
        var response = new InstantiationResponse();
        response.setAffectedAutomationComposition(command.getAutomationCompositionIdentifier());

        return response;
    }

    private BeanValidationResult validateIssueAutomationComposition(AutomationComposition automationComposition,
            List<Participant> participants) {
        var result = new BeanValidationResult("AutomationComposition", automationComposition);

        var participantMap = participants.stream()
                .collect(Collectors.toMap(participant -> participant.getKey().asIdentifier(), Function.identity()));

        for (var element : automationComposition.getElements().values()) {

            var subResult = new BeanValidationResult(ENTRY + element.getDefinition().getName(), element);
            var p = participantMap.get(element.getParticipantId());
            if (p == null) {
                subResult.addResult(new ObjectValidationResult(AUTOMATION_COMPOSITION_NODE_ELEMENT_TYPE,
                        element.getDefinition().getName(), ValidationStatus.INVALID,
                        "Participant with ID " + element.getParticipantId() + " is not registered"));
            } else if (!p.getParticipantType().equals(element.getParticipantType())) {
                subResult.addResult(new ObjectValidationResult(AUTOMATION_COMPOSITION_NODE_ELEMENT_TYPE,
                        element.getDefinition().getName(), ValidationStatus.INVALID,
                        "Participant with ID " + element.getParticipantType() + " - " + element.getParticipantId()
                                + " is not registered"));
            }
            result.addResult(subResult);
        }

        return result;
    }
}
