/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
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
    private final CommissioningProvider commissioningProvider;
    private final SupervisionHandler supervisionHandler;
    private final ParticipantProvider participantProvider;
    private static final String ENTRY = "entry ";

    /**
     * Create automation compositions.
     *
     * @param automationCompositions the automation composition
     * @return the result of the instantiation operation
     * @throws PfModelException on creation errors
     */
    public InstantiationResponse createAutomationCompositions(AutomationCompositions automationCompositions)
        throws PfModelException {
        for (AutomationComposition automationComposition : automationCompositions.getAutomationCompositionList()) {
            var checkAutomationCompositionOpt =
                automationCompositionProvider.findAutomationComposition(automationComposition.getKey().asIdentifier());
            if (checkAutomationCompositionOpt.isPresent()) {
                throw new PfModelException(Response.Status.BAD_REQUEST,
                    automationComposition.getKey().asIdentifier() + " already defined");
            }
        }
        BeanValidationResult validationResult = validateAutomationCompositions(automationCompositions);
        if (!validationResult.isValid()) {
            throw new PfModelException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        automationCompositionProvider.saveAutomationCompositions(automationCompositions.getAutomationCompositionList());

        var response = new InstantiationResponse();
        response.setAffectedAutomationCompositions(automationCompositions.getAutomationCompositionList().stream()
            .map(ac -> ac.getKey().asIdentifier()).collect(Collectors.toList()));

        return response;
    }

    /**
     * Update automation compositions.
     *
     * @param automationCompositions the automation composition
     * @return the result of the instantiation operation
     * @throws PfModelException on update errors
     */
    public InstantiationResponse updateAutomationCompositions(AutomationCompositions automationCompositions)
        throws PfModelException {
        BeanValidationResult validationResult = validateAutomationCompositions(automationCompositions);
        if (!validationResult.isValid()) {
            throw new PfModelException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        automationCompositionProvider.saveAutomationCompositions(automationCompositions.getAutomationCompositionList());

        var response = new InstantiationResponse();
        response.setAffectedAutomationCompositions(automationCompositions.getAutomationCompositionList().stream()
            .map(ac -> ac.getKey().asIdentifier()).collect(Collectors.toList()));

        return response;
    }

    /**
     * Validate AutomationCompositions.
     *
     * @param automationCompositions AutomationCompositions to validate
     * @return the result of validation
     * @throws PfModelException if automationCompositions is not valid
     */
    private BeanValidationResult validateAutomationCompositions(AutomationCompositions automationCompositions)
        throws PfModelException {

        var result = new BeanValidationResult("AutomationCompositions", automationCompositions);

        for (AutomationComposition automationComposition : automationCompositions.getAutomationCompositionList()) {
            var subResult = new BeanValidationResult(ENTRY + automationComposition.getDefinition().getName(),
                automationComposition);

            List<ToscaNodeTemplate> toscaNodeTemplates = commissioningProvider.getAutomationCompositionDefinitions(
                automationComposition.getDefinition().getName(), automationComposition.getDefinition().getVersion());

            if (toscaNodeTemplates.isEmpty()) {
                subResult.addResult(
                    new ObjectValidationResult("AutomationComposition", automationComposition.getDefinition().getName(),
                        ValidationStatus.INVALID, "Commissioned automation composition definition not found"));
            } else if (toscaNodeTemplates.size() > 1) {
                subResult.addResult(
                    new ObjectValidationResult("AutomationComposition", automationComposition.getDefinition().getName(),
                        ValidationStatus.INVALID, "Commissioned automation composition definition not valid"));
            } else {

                List<ToscaNodeTemplate> acElementDefinitions =
                    commissioningProvider.getAutomationCompositionElementDefinitions(toscaNodeTemplates.get(0));

                // @formatter:off
                Map<String, ToscaConceptIdentifier> definitions = acElementDefinitions
                        .stream()
                        .map(nodeTemplate -> nodeTemplate.getKey().asIdentifier())
                        .collect(Collectors.toMap(ToscaConceptIdentifier::getName, UnaryOperator.identity()));
                // @formatter:on

                for (AutomationCompositionElement element : automationComposition.getElements().values()) {
                    subResult.addResult(validateDefinition(definitions, element.getDefinition()));
                }
            }
            result.addResult(subResult);
        }
        return result;
    }

    /**
     * Validate ToscaConceptIdentifier, checking if exist in ToscaConceptIdentifiers map.
     *
     * @param definitions map of all ToscaConceptIdentifiers
     * @param definition ToscaConceptIdentifier to validate
     * @return the validation result
     */
    private ValidationResult validateDefinition(Map<String, ToscaConceptIdentifier> definitions,
        ToscaConceptIdentifier definition) {
        var result = new BeanValidationResult(ENTRY + definition.getName(), definition);
        ToscaConceptIdentifier identifier = definitions.get(definition.getName());
        if (identifier == null) {
            result.setResult(ValidationStatus.INVALID, "Not found");
        } else if (!identifier.equals(definition)) {
            result.setResult(ValidationStatus.INVALID, "Version not matching");
        }
        return (result.isClean() ? null : result);
    }

    /**
     * Delete the automation composition with the given name and version.
     *
     * @param name the name of the automation composition to delete
     * @param version the version of the automation composition to delete
     * @return the result of the deletion
     * @throws PfModelException on deletion errors
     */
    public InstantiationResponse deleteAutomationComposition(String name, String version) throws PfModelException {
        var automationCompositionOpt = automationCompositionProvider.findAutomationComposition(name, version);
        if (automationCompositionOpt.isEmpty()) {
            throw new PfModelException(Response.Status.NOT_FOUND, "Automation composition not found");
        }
        var automationComposition = automationCompositionOpt.get();
        if (!AutomationCompositionState.UNINITIALISED.equals(automationComposition.getState())) {
            throw new PfModelException(Response.Status.BAD_REQUEST,
                "Automation composition state is still " + automationComposition.getState());
        }
        var response = new InstantiationResponse();
        response.setAffectedAutomationCompositions(
            List.of(automationCompositionProvider.deleteAutomationComposition(name, version).getKey().asIdentifier()));
        return response;
    }

    /**
     * Get the requested automation compositions.
     *
     * @param name the name of the automation composition to get, null for all automation compositions
     * @param version the version of the automation composition to get, null for all automation compositions
     * @return the automation compositions
     * @throws PfModelException on errors getting automation compositions
     */
    @Transactional(readOnly = true)
    public AutomationCompositions getAutomationCompositions(String name, String version) throws PfModelException {
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
     * @throws PfModelException on errors setting the ordered state on the automation compositions
     * @throws AutomationCompositionException on ordered state invalid
     */
    public InstantiationResponse issueAutomationCompositionCommand(InstantiationCommand command)
        throws AutomationCompositionException, PfModelException {

        if (command.getOrderedState() == null) {
            throw new AutomationCompositionException(Status.BAD_REQUEST,
                "ordered state invalid or not specified on command");
        }

        var participants = participantProvider.getParticipants();
        if (participants.isEmpty()) {
            throw new AutomationCompositionException(Status.BAD_REQUEST, "No participants registered");
        }
        var validationResult = new BeanValidationResult("InstantiationCommand", command);
        List<AutomationComposition> automationCompositions =
            new ArrayList<>(command.getAutomationCompositionIdentifierList().size());
        for (ToscaConceptIdentifier id : command.getAutomationCompositionIdentifierList()) {
            var automationCompositionOpt = automationCompositionProvider.findAutomationComposition(id);
            if (automationCompositionOpt.isEmpty()) {
                validationResult.addResult("ToscaConceptIdentifier", id, ValidationStatus.INVALID,
                    "AutomationComposition with id " + id + " not found");
            } else {
                var automationComposition = automationCompositionOpt.get();
                automationComposition.setCascadedOrderedState(command.getOrderedState());
                automationCompositions.add(automationComposition);
            }
        }
        if (validationResult.isValid()) {
            validationResult = validateIssueAutomationCompositions(automationCompositions, participants);
        }
        if (!validationResult.isValid()) {
            throw new PfModelException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        automationCompositionProvider.saveAutomationCompositions(automationCompositions);

        supervisionHandler.triggerAutomationCompositionSupervision(command.getAutomationCompositionIdentifierList());
        var response = new InstantiationResponse();
        response.setAffectedAutomationCompositions(command.getAutomationCompositionIdentifierList());

        return response;
    }

    private BeanValidationResult validateIssueAutomationCompositions(List<AutomationComposition> automationCompositions,
        List<Participant> participants) {
        var result = new BeanValidationResult("AutomationCompositions", automationCompositions);

        Map<ToscaConceptIdentifier, Participant> participantMap = participants.stream()
            .collect(Collectors.toMap(participant -> participant.getKey().asIdentifier(), Function.identity()));

        for (AutomationComposition automationComposition : automationCompositions) {

            for (var element : automationComposition.getElements().values()) {

                var subResult = new BeanValidationResult(ENTRY + element.getDefinition().getName(), element);
                Participant p = participantMap.get(element.getParticipantId());
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

        }

        return result;
    }
}
