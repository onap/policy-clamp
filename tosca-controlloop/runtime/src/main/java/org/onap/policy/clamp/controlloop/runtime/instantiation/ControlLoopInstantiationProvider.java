/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;

/**
 * This class is dedicated to the Instantiation of Commissioned control loop.
 */
public class ControlLoopInstantiationProvider implements Closeable {
    private final ControlLoopProvider controlLoopProvider;
    private final CommissioningProvider commissioningProvider;

    private static final Object lockit = new Object();

    /**
     * Create a instantiation provider.
     *
     * @param databaseProviderParameters the parameters for database access
     */
    public ControlLoopInstantiationProvider(PolicyModelsProviderParameters databaseProviderParameters) {
        try {
            controlLoopProvider = new ControlLoopProvider(databaseProviderParameters);
            commissioningProvider = new CommissioningProvider(databaseProviderParameters);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        controlLoopProvider.close();
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
                ControlLoop checkControlLoop = controlLoopProvider.getControlLoop(controlLoop.getKey().asIdentifier());
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

        InstantiationResponse response = new InstantiationResponse();
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

        InstantiationResponse response = new InstantiationResponse();
        response.setAffectedControlLoops(controlLoops.getControlLoopList().stream()
                .map(cl -> cl.getKey().asIdentifier()).collect(Collectors.toList()));

        return response;
    }

    /**
     * Validate ControlLoops.
     *
     * @param controlLoops ControlLoops to validate
     * @result the result of validation
     * @throws PfModelException if controlLoops is not valid
     */
    private BeanValidationResult validateControlLoops(ControlLoops controlLoops) throws PfModelException {

        BeanValidationResult validationResult = new BeanValidationResult("ControlLoops", controlLoops);

        for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {

            List<ToscaNodeTemplate> toscaNodeTemplates = commissioningProvider.getControlLoopDefinitions(
                    controlLoop.getDefinition().getName(), controlLoop.getDefinition().getVersion());

            if (toscaNodeTemplates.isEmpty()) {
                validationResult.setResult(ValidationStatus.INVALID, "Commissioned control loop definition not FOUND");
            } else if (toscaNodeTemplates.size() > 1) {
                validationResult.setResult(ValidationStatus.INVALID, "Commissioned control loop definition not VALID");
            } else {

                List<ToscaNodeTemplate> clElementDefinitions =
                        commissioningProvider.getControlLoopElementDefinitions(toscaNodeTemplates.get(0));

                // @formatter:off
                Map<String, ToscaConceptIdentifier> definitions = clElementDefinitions
                        .stream()
                        .map(nodeTemplate -> nodeTemplate.getKey().asIdentifier())
                        .collect(Collectors.toMap(ToscaConceptIdentifier::getName, UnaryOperator.identity()));
                // @formatter:on

                for (ControlLoopElement element : controlLoop.getElements()) {
                    validateDefinition(definitions, element.getDefinition(), validationResult);
                }
            }
        }
        return validationResult;
    }

    /**
     * Validate ToscaConceptIdentifier, checking if exist in ToscaConceptIdentifiers map.
     *
     * @param definitions map of all ToscaConceptIdentifiers
     * @param definition ToscaConceptIdentifier to validate
     * @param result where to add the results
     */
    private void validateDefinition(Map<String, ToscaConceptIdentifier> definitions, ToscaConceptIdentifier definition,
            final BeanValidationResult result) {
        ToscaConceptIdentifier identifier = definitions.get(definition.getName());
        if (identifier == null) {
            result.addResult(new ObjectValidationResult("Control Loop Element", definition, ValidationStatus.INVALID,
                    definition.getName() + " not FOUND"));
        } else if (!identifier.equals(definition)) {
            result.addResult(new ObjectValidationResult("Control Loop Element", definition, ValidationStatus.INVALID,
                    definition.getName() + " version not matching"));
        }
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
        InstantiationResponse response = new InstantiationResponse();
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
        ControlLoops controlLoops = new ControlLoops();
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
            List<ControlLoop> controlLoops = new ArrayList<>(command.getControlLoopIdentifierList().size());
            for (ToscaConceptIdentifier id : command.getControlLoopIdentifierList()) {
                ControlLoop controlLoop = controlLoopProvider.getControlLoop(id);
                controlLoop.setCascadedOrderedState(command.getOrderedState());
                controlLoops.add(controlLoop);
            }
            controlLoopProvider.updateControlLoops(controlLoops);
        }

        InstantiationResponse response = new InstantiationResponse();
        response.setAffectedControlLoops(command.getControlLoopIdentifierList());

        return response;
    }
}
