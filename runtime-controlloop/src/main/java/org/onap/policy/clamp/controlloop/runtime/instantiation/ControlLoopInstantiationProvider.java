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

package org.onap.policy.clamp.controlloop.runtime.instantiation;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.Participant;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.messages.rest.GenericNameVersion;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.ControlLoopOrderStateResponse;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.ControlLoopPrimed;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.ControlLoopPrimedResponse;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstancePropertiesResponse;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNameVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.stereotype.Component;

/**
 * This class is dedicated to the Instantiation of Commissioned control loop.
 */
@Component
@AllArgsConstructor
public class ControlLoopInstantiationProvider {
    private static final String CONTROL_LOOP_NODE_TYPE = "org.onap.policy.clamp.controlloop.ControlLoop";
    private static final String CONTROL_LOOP_NODE_ELEMENT_TYPE = "ControlLoopElement";
    private static final String PARTICIPANT_ID_PROPERTY_KEY = "participant_id";
    private static final String PARTICIPANT_TYPE_PROPERTY_KEY = "participantType";
    private static final String CL_ELEMENT_NAME = "name";
    private static final String CL_ELEMENT_VERSION = "version";
    private static final String INSTANCE_TEXT = "_Instance";

    private static final Gson GSON = new Gson();

    private final ControlLoopProvider controlLoopProvider;
    private final CommissioningProvider commissioningProvider;
    private final SupervisionHandler supervisionHandler;
    private final ParticipantProvider participantProvider;

    private static final Object lockit = new Object();

    /**
     * Creates Instance Properties and Control Loop.
     *
     * @param serviceTemplate the service template
     * @return the result of the instantiation operation
     * @throws PfModelException on creation errors
     */
    public InstancePropertiesResponse createInstanceProperties(ToscaServiceTemplate serviceTemplate)
            throws PfModelException {

        String instanceName = generateSequentialInstanceName();
        ControlLoop controlLoop = new ControlLoop();
        Map<UUID, ControlLoopElement> controlLoopElements = new HashMap<>();

        ToscaServiceTemplate toscaServiceTemplate = commissioningProvider.getToscaServiceTemplate(null, null);

        Map<String, ToscaNodeTemplate> persistedNodeTemplateMap =
                toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();

        Map<String, ToscaNodeTemplate> nodeTemplates = deepCloneNodeTemplate(serviceTemplate);

        nodeTemplates.forEach((key, template) -> {
            ToscaNodeTemplate newNodeTemplate = new ToscaNodeTemplate();
            String name = key + instanceName;
            String version = template.getVersion();
            String description = template.getDescription() + instanceName;
            newNodeTemplate.setName(name);
            newNodeTemplate.setVersion(version);
            newNodeTemplate.setDescription(description);
            newNodeTemplate.setProperties(new HashMap<>(template.getProperties()));
            newNodeTemplate.setType(template.getType());
            newNodeTemplate.setTypeVersion(template.getTypeVersion());
            newNodeTemplate.setMetadata(template.getMetadata());

            crateNewControlLoopInstance(instanceName, controlLoop, controlLoopElements, template, newNodeTemplate);

            persistedNodeTemplateMap.put(name, newNodeTemplate);
        });

        ControlLoops controlLoops = new ControlLoops();

        serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().putAll(persistedNodeTemplateMap);

        controlLoop.setElements(controlLoopElements);
        controlLoops.getControlLoopList().add(controlLoop);

        return saveInstancePropertiesAndControlLoop(serviceTemplate, controlLoops);
    }

    /**
     * Deletes Instance Properties.
     *
     * @param name the name of the control loop to delete
     * @param version the version of the control loop to delete
     * @return the result of the deletion
     * @throws PfModelException on deletion errors
     */
    public InstantiationResponse deleteInstanceProperties(String name, String version) throws PfModelException {

        String instanceName = getInstancePropertyName(name, version);

        Map<String, ToscaNodeTemplate> filteredToscaNodeTemplateMap = new HashMap<>();

        ToscaServiceTemplate toscaServiceTemplate = commissioningProvider.getToscaServiceTemplate(name, version);

        toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates().forEach((key, nodeTemplate) -> {
            if (!nodeTemplate.getName().contains(instanceName)) {
                filteredToscaNodeTemplateMap.put(key, nodeTemplate);
            }
        });

        List<ToscaNodeTemplate> filteredToscaNodeTemplateList = toscaServiceTemplate.getToscaTopologyTemplate()
                .getNodeTemplates().values().stream()
                .filter(nodeTemplate -> nodeTemplate.getName().contains(instanceName)).collect(Collectors.toList());

        InstantiationResponse response = this.deleteControlLoop(name, version);

        controlLoopProvider.deleteInstanceProperties(filteredToscaNodeTemplateMap, filteredToscaNodeTemplateList);

        return response;
    }

    /**
     * Create control loops.
     *
     * @param controlLoops the control loop
     * @return the result of the instantiation operation
     * @throws PfModelException on creation errors
     */
    public InstantiationResponse createControlLoops(ControlLoops controlLoops) throws PfModelException {

        synchronized (lockit) {
            for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {
                var checkControlLoop = controlLoopProvider.getControlLoop(controlLoop.getKey().asIdentifier());
                if (checkControlLoop != null) {
                    throw new PfModelException(Response.Status.BAD_REQUEST,
                            controlLoop.getKey().asIdentifier() + " already defined");
                }
            }
            BeanValidationResult validationResult = validateControlLoops(controlLoops);
            if (!validationResult.isValid()) {
                throw new PfModelException(Response.Status.BAD_REQUEST, validationResult.getResult());
            }
            controlLoopProvider.createControlLoops(controlLoops.getControlLoopList());
        }

        var response = new InstantiationResponse();
        response.setAffectedControlLoops(controlLoops.getControlLoopList().stream()
                .map(cl -> cl.getKey().asIdentifier()).collect(Collectors.toList()));

        return response;
    }

    /**
     * Update control loops.
     *
     * @param controlLoops the control loop
     * @return the result of the instantiation operation
     * @throws PfModelException on update errors
     */
    public InstantiationResponse updateControlLoops(ControlLoops controlLoops) throws PfModelException {
        synchronized (lockit) {
            BeanValidationResult validationResult = validateControlLoops(controlLoops);
            if (!validationResult.isValid()) {
                throw new PfModelException(Response.Status.BAD_REQUEST, validationResult.getResult());
            }
            controlLoopProvider.updateControlLoops(controlLoops.getControlLoopList());
        }

        var response = new InstantiationResponse();
        response.setAffectedControlLoops(controlLoops.getControlLoopList().stream()
                .map(cl -> cl.getKey().asIdentifier()).collect(Collectors.toList()));

        return response;
    }

    /**
     * Validate ControlLoops.
     *
     * @param controlLoops ControlLoops to validate
     * @return the result of validation
     * @throws PfModelException if controlLoops is not valid
     */
    private BeanValidationResult validateControlLoops(ControlLoops controlLoops) throws PfModelException {

        var result = new BeanValidationResult("ControlLoops", controlLoops);

        for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {
            var subResult = new BeanValidationResult("entry " + controlLoop.getDefinition().getName(), controlLoop);

            List<ToscaNodeTemplate> toscaNodeTemplates = commissioningProvider.getControlLoopDefinitions(
                    controlLoop.getDefinition().getName(), controlLoop.getDefinition().getVersion());

            if (toscaNodeTemplates.isEmpty()) {
                subResult.addResult(new ObjectValidationResult("ControlLoop", controlLoop.getDefinition().getName(),
                        ValidationStatus.INVALID, "Commissioned control loop definition not FOUND"));
            } else if (toscaNodeTemplates.size() > 1) {
                subResult.addResult(new ObjectValidationResult("ControlLoop", controlLoop.getDefinition().getName(),
                        ValidationStatus.INVALID, "Commissioned control loop definition not VALID"));
            } else {

                List<ToscaNodeTemplate> clElementDefinitions =
                        commissioningProvider.getControlLoopElementDefinitions(toscaNodeTemplates.get(0));

                // @formatter:off
                Map<String, ToscaConceptIdentifier> definitions = clElementDefinitions
                        .stream()
                        .map(nodeTemplate -> nodeTemplate.getKey().asIdentifier())
                        .collect(Collectors.toMap(ToscaConceptIdentifier::getName, UnaryOperator.identity()));
                // @formatter:on

                for (ControlLoopElement element : controlLoop.getElements().values()) {
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
        var result = new BeanValidationResult("entry " + definition.getName(), definition);
        ToscaConceptIdentifier identifier = definitions.get(definition.getName());
        if (identifier == null) {
            result.setResult(ValidationStatus.INVALID, "Not FOUND");
        } else if (!identifier.equals(definition)) {
            result.setResult(ValidationStatus.INVALID, "Version not matching");
        }
        return (result.isClean() ? null : result);
    }

    /**
     * Delete the control loop with the given name and version.
     *
     * @param name the name of the control loop to delete
     * @param version the version of the control loop to delete
     * @return the result of the deletion
     * @throws PfModelException on deletion errors
     */
    public InstantiationResponse deleteControlLoop(String name, String version) throws PfModelException {
        var response = new InstantiationResponse();
        synchronized (lockit) {
            List<ControlLoop> controlLoops = controlLoopProvider.getControlLoops(name, version);
            if (controlLoops.isEmpty()) {
                throw new PfModelException(Response.Status.NOT_FOUND, "Control Loop not found");
            }
            for (ControlLoop controlLoop : controlLoops) {
                if (!ControlLoopState.UNINITIALISED.equals(controlLoop.getState())) {
                    throw new PfModelException(Response.Status.BAD_REQUEST,
                            "Control Loop State is still " + controlLoop.getState());
                }
            }

            response.setAffectedControlLoops(Collections
                    .singletonList(controlLoopProvider.deleteControlLoop(name, version).getKey().asIdentifier()));
        }
        return response;
    }

    /**
     * Get the requested control loops.
     *
     * @param name the name of the control loop to get, null for all control loops
     * @param version the version of the control loop to get, null for all control loops
     * @return the control loops
     * @throws PfModelException on errors getting control loops
     */
    public ControlLoops getControlLoops(String name, String version) throws PfModelException {
        var controlLoops = new ControlLoops();
        controlLoops.setControlLoopList(controlLoopProvider.getControlLoops(name, version));

        return controlLoops;
    }

    /**
     * Issue a command to control loops, setting their ordered state.
     *
     * @param command the command to issue to control loops
     * @return the result of the initiation command
     * @throws PfModelException on errors setting the ordered state on the control loops
     * @throws ControlLoopException on ordered state invalid
     */
    public InstantiationResponse issueControlLoopCommand(InstantiationCommand command)
            throws ControlLoopException, PfModelException {

        if (command.getOrderedState() == null) {
            throw new ControlLoopException(Status.BAD_REQUEST, "ordered state invalid or not specified on command");
        }

        synchronized (lockit) {
            var participants = participantProvider.getParticipants(null, null);
            if (participants.isEmpty()) {
                throw new ControlLoopException(Status.BAD_REQUEST, "No participants registered");
            }
            List<ControlLoop> controlLoops = new ArrayList<>(command.getControlLoopIdentifierList().size());
            for (ToscaConceptIdentifier id : command.getControlLoopIdentifierList()) {
                var controlLoop = controlLoopProvider.getControlLoop(id);
                controlLoop.setCascadedOrderedState(command.getOrderedState());
                controlLoops.add(controlLoop);
            }
            BeanValidationResult validationResult = validateIssueControlLoops(controlLoops, participants);
            if (!validationResult.isValid()) {
                throw new PfModelException(Response.Status.BAD_REQUEST, validationResult.getResult());
            }
            controlLoopProvider.updateControlLoops(controlLoops);
        }

        supervisionHandler.triggerControlLoopSupervision(command.getControlLoopIdentifierList());
        var response = new InstantiationResponse();
        response.setAffectedControlLoops(command.getControlLoopIdentifierList());

        return response;
    }

    private BeanValidationResult validateIssueControlLoops(List<ControlLoop> controlLoops,
            List<Participant> participants) {
        var result = new BeanValidationResult("ControlLoops", controlLoops);

        Map<ToscaConceptIdentifier, Participant> participantMap = participants.stream()
                .collect(Collectors.toMap(participant -> participant.getKey().asIdentifier(), Function.identity()));

        for (ControlLoop controlLoop : controlLoops) {

            for (var element : controlLoop.getElements().values()) {
                var subResult = new BeanValidationResult("entry " + element.getDefinition().getName(), element);

                Participant p = participantMap.get(element.getParticipantId());
                if (p == null) {
                    subResult.addResult(new ObjectValidationResult(CONTROL_LOOP_NODE_ELEMENT_TYPE,
                            element.getDefinition().getName(), ValidationStatus.INVALID,
                            "Participant with ID " + element.getParticipantId() + " is not registered"));
                } else if (!p.getParticipantType().equals(element.getParticipantType())) {
                    subResult.addResult(new ObjectValidationResult(CONTROL_LOOP_NODE_ELEMENT_TYPE,
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
     * Gets a list of control loops with it's ordered state.
     *
     * @param name the name of the control loop to get, null for all control loops
     * @param version the version of the control loop to get, null for all control loops
     * @return a list of Instantiation Command
     * @throws PfModelException on errors getting control loops
     */
    public ControlLoopOrderStateResponse getInstantiationOrderState(String name, String version)
            throws PfModelException {

        List<ControlLoop> controlLoops = controlLoopProvider.getControlLoops(name, version);

        var response = new ControlLoopOrderStateResponse();

        controlLoops.forEach(controlLoop -> {
            var genericNameVersion = new GenericNameVersion();
            genericNameVersion.setName(controlLoop.getName());
            genericNameVersion.setVersion(controlLoop.getVersion());
            response.getControlLoopIdentifierList().add(genericNameVersion);
        });

        return response;
    }

    /**
     * Saves Instance Properties and Control Loop.
     * Gets a list of control loops which are primed or de-primed.
     *
     * @param name the name of the control loop to get, null for all control loops
     * @param version the version of the control loop to get, null for all control loops
     * @return a list of Instantiation Command
     * @throws PfModelException on errors getting control loops
     */
    public ControlLoopPrimedResponse getControlLoopPriming(String name, String version) throws PfModelException {

        List<ControlLoop> controlLoops = controlLoopProvider.getControlLoops(name, version);

        var response = new ControlLoopPrimedResponse();

        controlLoops.forEach(controlLoop -> {
            var primed = new ControlLoopPrimed();
            primed.setName(controlLoop.getName());
            primed.setVersion(controlLoop.getVersion());
            primed.setPrimed(controlLoop.getPrimed());
            response.getPrimedControlLoopsList().add(primed);
        });

        return response;
    }

    /**
     * Creates instance element name.
     *
     * @param serviceTemplate the service template
     * @param controlLoops a list of control loops
     * @return the result of the instance properties and instantiation operation
     * @throws PfModelException on creation errors
     */
    private InstancePropertiesResponse saveInstancePropertiesAndControlLoop(ToscaServiceTemplate serviceTemplate,
            ControlLoops controlLoops) throws PfModelException {

        var response = new InstancePropertiesResponse();

        Map<String, ToscaNodeTemplate> toscaSavedNodeTemplate;

        synchronized (lockit) {
            for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {
                var checkControlLoop = controlLoopProvider.getControlLoop(controlLoop.getKey().asIdentifier());
                if (checkControlLoop != null) {
                    throw new PfModelException(Response.Status.BAD_REQUEST,
                            "Control loop with id " + controlLoop.getKey().asIdentifier() + " already defined");
                }
            }

            toscaSavedNodeTemplate = controlLoopProvider.saveInstanceProperties(serviceTemplate);

            controlLoopProvider.createControlLoops(controlLoops.getControlLoopList());

        }

        List<ToscaConceptIdentifier> affectedControlLoops = controlLoops.getControlLoopList().stream()
                .map(cl -> cl.getKey().asIdentifier()).collect(Collectors.toList());

        List<ToscaConceptIdentifier> toscaAffectedProperties = toscaSavedNodeTemplate.values().stream()
                .map(template -> template.getKey().asIdentifier()).collect(Collectors.toList());

        response.setAffectedInstanceProperties(Stream.of(affectedControlLoops, toscaAffectedProperties)
                .flatMap(Collection::stream).collect(Collectors.toList()));

        return response;
    }

    /**
     * Crates a new Control Loop instance.
     *
     * @param instanceName Control Loop Instance name
     * @param controlLoop empty Control Loop
     * @param controlLoopElements new Control Loop Element map
     * @param template original Cloned Tosca Node Template
     * @param newNodeTemplate new Tosca Node Template
     */
    private void crateNewControlLoopInstance(String instanceName, ControlLoop controlLoop,
            Map<UUID, ControlLoopElement> controlLoopElements, ToscaNodeTemplate template,
            ToscaNodeTemplate newNodeTemplate) {
        if (template.getType().equals(CONTROL_LOOP_NODE_TYPE)) {
            controlLoop.setDefinition(getControlLoopDefinition(newNodeTemplate));
        }

        if (template.getType().contains(CONTROL_LOOP_NODE_ELEMENT_TYPE)) {
            ControlLoopElement controlLoopElement = getControlLoopElement(newNodeTemplate);
            controlLoopElements.put(controlLoopElement.getId(), controlLoopElement);
        }

        controlLoop.setName("PMSH" + instanceName);
        controlLoop.setVersion(template.getVersion());
        controlLoop.setDescription("PMSH control loop " + instanceName);
        controlLoop.setState(ControlLoopState.UNINITIALISED);
        controlLoop.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
    }

    /**
     * Get's the instance property name of the control loop.
     *
     * @param name the name of the control loop to get, null for all control loops
     * @param version the version of the control loop to get, null for all control loops
     * @return the instance name of the control loop instance properties
     * @throws PfModelException on errors getting control loops
     */
    private String getInstancePropertyName(String name, String version) throws PfModelException {
        List<String> toscaDefinitionsNames = controlLoopProvider.getControlLoops(name, version).stream()
                .map(ControlLoop::getDefinition).map(ToscaNameVersion::getName).collect(Collectors.toList());

        return toscaDefinitionsNames.stream().reduce("", (s1, s2) -> {

            if (s2.contains(INSTANCE_TEXT)) {
                String[] instances = s2.split(INSTANCE_TEXT);

                return INSTANCE_TEXT + instances[1];
            }

            return s1;
        });
    }

    /**
     * Generates Instance Name in sequential order and return it to append to the Node Template Name.
     *
     * @return instanceName
     */
    private String generateSequentialInstanceName() {
        List<ToscaNodeTemplate> nodeTemplates = controlLoopProvider.getNodeTemplates(null, null);

        int instanceNumber = nodeTemplates.stream().map(ToscaNodeTemplate::getName)
                .filter(name -> name.contains(INSTANCE_TEXT)).map(n -> {
                    String[] defNameArr = n.split(INSTANCE_TEXT);

                    return Integer.parseInt(defNameArr[1]);
                }).reduce(0, Math::max);

        return INSTANCE_TEXT + (instanceNumber + 1);
    }

    /**
     * Retrieves Control Loop Definition.
     *
     * @param template tosca node template
     * @return control loop definition
     */
    private ToscaConceptIdentifier getControlLoopDefinition(ToscaNodeTemplate template) {
        ToscaConceptIdentifier definition = new ToscaConceptIdentifier();
        definition.setName(template.getName());
        definition.setVersion(template.getVersion());

        return definition;
    }

    /**
     * Retrieves Control Loop Element.
     *
     * @param template tosca node template
     * @return a control loop element
     */
    @SuppressWarnings("unchecked")
    private ControlLoopElement getControlLoopElement(ToscaNodeTemplate template) {
        ControlLoopElement controlLoopElement = new ControlLoopElement();
        ToscaConceptIdentifier definition = new ToscaConceptIdentifier();
        definition.setName(template.getName());
        definition.setVersion(template.getVersion());
        controlLoopElement.setDefinition(definition);

        LinkedTreeMap<String, Object> participantId =
                (LinkedTreeMap<String, Object>) template.getProperties().get(PARTICIPANT_ID_PROPERTY_KEY);

        if (participantId != null) {
            ToscaConceptIdentifier participantIdProperty = new ToscaConceptIdentifier();
            participantIdProperty.setName(String.valueOf(participantId.get(CL_ELEMENT_NAME)));
            participantIdProperty.setVersion(String.valueOf(participantId.get(CL_ELEMENT_VERSION)));
            controlLoopElement.setParticipantId(participantIdProperty);
        }

        LinkedTreeMap<String, Object> participantType =
                (LinkedTreeMap<String, Object>) template.getProperties().get(PARTICIPANT_TYPE_PROPERTY_KEY);

        if (participantType != null) {
            ToscaConceptIdentifier participantTypeProperty = new ToscaConceptIdentifier();
            participantTypeProperty.setName(String.valueOf(participantType.get(CL_ELEMENT_NAME)));
            participantTypeProperty.setVersion(participantType.get(CL_ELEMENT_VERSION).toString());
            controlLoopElement.setParticipantType(participantTypeProperty);
        }

        return controlLoopElement;
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
