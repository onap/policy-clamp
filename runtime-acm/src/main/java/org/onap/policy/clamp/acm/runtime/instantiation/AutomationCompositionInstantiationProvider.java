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

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import org.onap.policy.clamp.acm.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.messages.rest.GenericNameVersion;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AutomationCompositionOrderStateResponse;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AutomationCompositionPrimed;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AutomationCompositionPrimedResponse;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstancePropertiesResponse;
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaNameVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is dedicated to the Instantiation of Commissioned automation composition.
 */
@Service
@Transactional
@AllArgsConstructor
public class AutomationCompositionInstantiationProvider {
    private static final String AUTOMATION_COMPOSITION_NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition";
    private static final String AUTOMATION_COMPOSITION_NODE_ELEMENT_TYPE = "AutomationCompositionElement";
    private static final String PARTICIPANT_ID_PROPERTY_KEY = "participant_id";
    private static final String PARTICIPANT_TYPE_PROPERTY_KEY = "participantType";
    private static final String AC_ELEMENT_NAME = "name";
    private static final String AC_ELEMENT_VERSION = "version";
    private static final String HYPHEN = "-";

    private static final Gson GSON = new Gson();

    private final AutomationCompositionProvider automationCompositionProvider;
    private final CommissioningProvider commissioningProvider;
    private final SupervisionHandler supervisionHandler;
    private final ParticipantProvider participantProvider;
    private static final String ENTRY = "entry ";

    /**
     * Creates Instance Properties and automation composition.
     *
     * @param serviceTemplate the service template
     * @return the result of the instantiation operation
     * @throws PfModelException on creation errors
     */
    public InstancePropertiesResponse createInstanceProperties(ToscaServiceTemplate serviceTemplate)
        throws PfModelException {

        String instanceName = serviceTemplate.getName();
        AutomationComposition automationComposition = new AutomationComposition();
        Map<UUID, AutomationCompositionElement> automationCompositionElements = new HashMap<>();

        ToscaServiceTemplate toscaServiceTemplate = commissioningProvider.getAllToscaServiceTemplate().get(0);

        Map<String, ToscaNodeTemplate> persistedNodeTemplateMap =
            toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();

        Map<String, ToscaNodeTemplate> nodeTemplates = deepCloneNodeTemplate(serviceTemplate);

        nodeTemplates.forEach((key, template) -> {
            ToscaNodeTemplate newNodeTemplate = new ToscaNodeTemplate();
            String name = key + "-" + instanceName;
            String version = template.getVersion();
            String description = template.getDescription() + " " + instanceName;
            newNodeTemplate.setName(name);
            newNodeTemplate.setVersion(version);
            newNodeTemplate.setDescription(description);
            newNodeTemplate.setProperties(new HashMap<>(template.getProperties()));
            newNodeTemplate.setType(template.getType());
            newNodeTemplate.setTypeVersion(template.getTypeVersion());
            newNodeTemplate.setMetadata(template.getMetadata());

            crateNewAutomationCompositionInstance(instanceName, automationComposition, automationCompositionElements,
                template, newNodeTemplate);

            persistedNodeTemplateMap.put(name, newNodeTemplate);
        });

        AutomationCompositions automationCompositions = new AutomationCompositions();

        serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().putAll(persistedNodeTemplateMap);

        automationComposition.setElements(automationCompositionElements);
        automationCompositions.getAutomationCompositionList().add(automationComposition);

        return saveInstancePropertiesAndAutomationComposition(serviceTemplate, automationCompositions);
    }

    /**
     * Deletes Instance Properties.
     *
     * @param name the name of the automation composition to delete
     * @param version the version of the automation composition to delete
     * @return the result of the deletion
     * @throws PfModelException on deletion errors
     */
    public InstantiationResponse deleteInstanceProperties(String name, String version) throws PfModelException {

        String instanceName = getInstancePropertyName(name, version);

        Map<String, ToscaNodeTemplate> filteredToscaNodeTemplateMap = new HashMap<>();

        ToscaServiceTemplate toscaServiceTemplate = commissioningProvider.getAllToscaServiceTemplate().get(0);

        toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().forEach((key, nodeTemplate) -> {
            if (!nodeTemplate.getName().contains(instanceName)) {
                filteredToscaNodeTemplateMap.put(key, nodeTemplate);
            }
        });

        List<ToscaNodeTemplate> filteredToscaNodeTemplateList =
            toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().values().stream()
                .filter(nodeTemplate -> nodeTemplate.getName().contains(instanceName)).collect(Collectors.toList());

        InstantiationResponse response = this.deleteAutomationComposition(name, version);

        automationCompositionProvider.deleteInstanceProperties(filteredToscaNodeTemplateMap,
            filteredToscaNodeTemplateList);

        return response;
    }

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

    /**
     * Gets a list of automation compositions with it's ordered state.
     *
     * @param name the name of the automation composition to get, null for all automation compositions
     * @param version the version of the automation composition to get, null for all automation compositions
     * @return a list of Instantiation Command
     * @throws PfModelException on errors getting automation compositions
     */
    @Transactional(readOnly = true)
    public AutomationCompositionOrderStateResponse getInstantiationOrderState(String name, String version)
        throws PfModelException {

        List<AutomationComposition> automationCompositions =
            automationCompositionProvider.getAutomationCompositions(name, version);

        var response = new AutomationCompositionOrderStateResponse();

        automationCompositions.forEach(automationComposition -> {
            var genericNameVersion = new GenericNameVersion();
            genericNameVersion.setName(automationComposition.getName());
            genericNameVersion.setVersion(automationComposition.getVersion());
            response.getAutomationCompositionIdentifierList().add(genericNameVersion);
        });

        return response;
    }

    /**
     * Saves Instance Properties and automation composition.
     * Gets a list of automation compositions which are primed or de-primed.
     *
     * @param name the name of the automation composition to get, null for all automation compositions
     * @param version the version of the automation composition to get, null for all automation compositions
     * @return a list of Instantiation Command
     * @throws PfModelException on errors getting automation compositions
     */
    @Transactional(readOnly = true)
    public AutomationCompositionPrimedResponse getAutomationCompositionPriming(String name, String version)
        throws PfModelException {

        List<AutomationComposition> automationCompositions =
            automationCompositionProvider.getAutomationCompositions(name, version);

        var response = new AutomationCompositionPrimedResponse();

        automationCompositions.forEach(automationComposition -> {
            var primed = new AutomationCompositionPrimed();
            primed.setName(automationComposition.getName());
            primed.setVersion(automationComposition.getVersion());
            primed.setPrimed(automationComposition.getPrimed());
            response.getPrimedAutomationCompositionsList().add(primed);
        });

        return response;
    }

    /**
     * Creates instance element name.
     *
     * @param serviceTemplate the service template
     * @param automationCompositions a list of automation compositions
     * @return the result of the instance properties and instantiation operation
     * @throws PfModelException on creation errors
     */
    private InstancePropertiesResponse saveInstancePropertiesAndAutomationComposition(
        ToscaServiceTemplate serviceTemplate, AutomationCompositions automationCompositions) throws PfModelException {

        for (var automationComposition : automationCompositions.getAutomationCompositionList()) {
            var checkAutomationCompositionOpt =
                automationCompositionProvider.findAutomationComposition(automationComposition.getKey().asIdentifier());
            if (checkAutomationCompositionOpt.isPresent()) {
                throw new PfModelException(Response.Status.BAD_REQUEST, "Automation composition with id "
                    + automationComposition.getKey().asIdentifier() + " already defined");
            }
        }
        Map<String, ToscaNodeTemplate> toscaSavedNodeTemplate =
            automationCompositionProvider.saveInstanceProperties(serviceTemplate);
        automationCompositionProvider.saveAutomationCompositions(automationCompositions.getAutomationCompositionList());
        List<ToscaConceptIdentifier> affectedAutomationCompositions = automationCompositions
            .getAutomationCompositionList().stream().map(ac -> ac.getKey().asIdentifier()).collect(Collectors.toList());

        List<ToscaConceptIdentifier> toscaAffectedProperties = toscaSavedNodeTemplate.values().stream()
            .map(template -> template.getKey().asIdentifier()).collect(Collectors.toList());

        var response = new InstancePropertiesResponse();
        response.setAffectedInstanceProperties(Stream.of(affectedAutomationCompositions, toscaAffectedProperties)
            .flatMap(Collection::stream).collect(Collectors.toList()));

        return response;
    }

    /**
     * Crates a new automation composition instance.
     *
     * @param instanceName automation composition Instance name
     * @param automationComposition empty automation composition
     * @param automationCompositionElements new automation composition Element map
     * @param template original Cloned Tosca Node Template
     * @param newNodeTemplate new Tosca Node Template
     */
    private void crateNewAutomationCompositionInstance(String instanceName, AutomationComposition automationComposition,
        Map<UUID, AutomationCompositionElement> automationCompositionElements, ToscaNodeTemplate template,
        ToscaNodeTemplate newNodeTemplate) {
        if (template.getType().equals(AUTOMATION_COMPOSITION_NODE_TYPE)) {
            automationComposition.setDefinition(getAutomationCompositionDefinition(newNodeTemplate));
        }

        if (template.getType().contains(AUTOMATION_COMPOSITION_NODE_ELEMENT_TYPE)) {
            AutomationCompositionElement automationCompositionElement =
                getAutomationCompositionElement(newNodeTemplate);
            automationCompositionElements.put(automationCompositionElement.getId(), automationCompositionElement);
        }

        automationComposition.setName(instanceName);
        automationComposition.setVersion(template.getVersion());
        automationComposition.setDescription("Automation composition " + instanceName);
        automationComposition.setState(AutomationCompositionState.UNINITIALISED);
        automationComposition.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
    }

    /**
     * Get's the instance property name of the automation composition.
     *
     * @param name the name of the automation composition to get, null for all automation compositions
     * @param version the version of the automation composition to get, null for all automation compositions
     * @return the instance name of the automation composition instance properties
     * @throws PfModelException on errors getting automation compositions
     */
    private String getInstancePropertyName(String name, String version) throws PfModelException {
        List<String> toscaDefinitionsNames =
            automationCompositionProvider.getAutomationCompositions(name, version).stream()
                .map(AutomationComposition::getDefinition).map(ToscaNameVersion::getName).collect(Collectors.toList());

        return toscaDefinitionsNames.stream().reduce("", (s1, s2) -> {

            if (s2.contains(HYPHEN)) {
                String[] instances = s2.split(HYPHEN);

                return HYPHEN + instances[1];
            }

            return s1;
        });
    }

    /**
     * Retrieves automation composition Definition.
     *
     * @param template tosca node template
     * @return automation composition definition
     */
    private ToscaConceptIdentifier getAutomationCompositionDefinition(ToscaNodeTemplate template) {
        ToscaConceptIdentifier definition = new ToscaConceptIdentifier();
        definition.setName(template.getName());
        definition.setVersion(template.getVersion());
        return definition;
    }

    /**
     * Retrieves automation composition Element.
     *
     * @param template tosca node template
     * @return a automation composition element
     */
    @SuppressWarnings("unchecked")
    private AutomationCompositionElement getAutomationCompositionElement(ToscaNodeTemplate template) {
        AutomationCompositionElement automationCompositionElement = new AutomationCompositionElement();
        ToscaConceptIdentifier definition = new ToscaConceptIdentifier();
        definition.setName(template.getName());
        definition.setVersion(template.getVersion());
        automationCompositionElement.setDefinition(definition);
        LinkedTreeMap<String, Object> participantId =
            (LinkedTreeMap<String, Object>) template.getProperties().get(PARTICIPANT_ID_PROPERTY_KEY);
        if (participantId != null) {
            ToscaConceptIdentifier participantIdProperty = new ToscaConceptIdentifier();
            participantIdProperty.setName(String.valueOf(participantId.get(AC_ELEMENT_NAME)));
            participantIdProperty.setVersion(String.valueOf(participantId.get(AC_ELEMENT_VERSION)));
            automationCompositionElement.setParticipantId(participantIdProperty);
        }
        LinkedTreeMap<String, Object> participantType =
            (LinkedTreeMap<String, Object>) template.getProperties().get(PARTICIPANT_TYPE_PROPERTY_KEY);
        if (participantType != null) {
            ToscaConceptIdentifier participantTypeProperty = new ToscaConceptIdentifier();
            participantTypeProperty.setName(String.valueOf(participantType.get(AC_ELEMENT_NAME)));
            participantTypeProperty.setVersion(participantType.get(AC_ELEMENT_VERSION).toString());
            automationCompositionElement.setParticipantType(participantTypeProperty);
        }
        return automationCompositionElement;
    }

    /**
     * Deep clones ToscaNodeTemplate.
     *
     * @param serviceTemplate ToscaServiceTemplate
     * @return a cloned Hash Map of ToscaNodeTemplate
     */
    private Map<String, ToscaNodeTemplate> deepCloneNodeTemplate(ToscaServiceTemplate serviceTemplate) {
        String jsonString = GSON.toJson(serviceTemplate.getToscaTopologyTemplate().getNodeTemplates());
        Type type = new TypeToken<HashMap<String, ToscaNodeTemplate>>() {}.getType();
        return GSON.fromJson(jsonString, type);
    }
}
