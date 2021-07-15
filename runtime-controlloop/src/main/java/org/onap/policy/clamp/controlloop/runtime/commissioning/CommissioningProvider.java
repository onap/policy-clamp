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

package org.onap.policy.clamp.controlloop.runtime.commissioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;
import org.onap.policy.models.tosca.authorative.concepts.ToscaCapabilityType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaRelationshipType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;
import org.springframework.stereotype.Component;

/**
 * This class provides the create, read and delete actions on Commissioning of Control Loop concepts in the database to
 * the callers.
 */
@Component
public class CommissioningProvider implements Closeable {
    public static final String CONTROL_LOOP_NODE_TYPE = "org.onap.policy.clamp.controlloop.ControlLoop";

    private final PolicyModelsProvider modelsProvider;
    private final ControlLoopProvider clProvider;

    private static final Object lockit = new Object();

    /**
     * Create a commissioning provider.
     *
     * @param controlLoopParameters the parameters for access to the database
     * @throws PfModelRuntimeException on errors creating the database provider
     */
    public CommissioningProvider(ClRuntimeParameterGroup controlLoopParameters) {
        try {
            modelsProvider = new PolicyModelsProviderFactory()
                    .createPolicyModelsProvider(controlLoopParameters.getDatabaseProviderParameters());
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }

        try {
            clProvider = new ControlLoopProvider(controlLoopParameters.getDatabaseProviderParameters());
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            modelsProvider.close();
            clProvider.close();
        } catch (PfModelException e) {
            throw new IOException("error closing modelsProvider", e);
        }
    }

    /**
     * Create control loops from a service template.
     *
     * @param serviceTemplate the service template
     * @return the result of the commissioning operation
     * @throws PfModelException on creation errors
     */
    public CommissioningResponse createControlLoopDefinitions(ToscaServiceTemplate serviceTemplate)
            throws PfModelException {
        synchronized (lockit) {
            modelsProvider.createServiceTemplate(serviceTemplate);
        }

        var response = new CommissioningResponse();
        // @formatter:off
        response.setAffectedControlLoopDefinitions(serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .values()
                .stream()
                .map(template -> template.getKey().asIdentifier())
                .collect(Collectors.toList()));
        // @formatter:on

        return response;
    }

    /**
     * Delete the control loop definition with the given name and version.
     *
     * @param name the name of the control loop definition to delete
     * @param version the version of the control loop to delete
     * @return the result of the deletion
     * @throws PfModelException on deletion errors
     */
    public CommissioningResponse deleteControlLoopDefinition(String name, String version) throws PfModelException {
        synchronized (lockit) {
            modelsProvider.deleteServiceTemplate(name, version);
        }

        var response = new CommissioningResponse();
        response.setAffectedControlLoopDefinitions(
                Collections.singletonList(new ToscaConceptIdentifier(name, version)));

        return response;
    }

    /**
     * Get control loop node templates.
     *
     * @param clName the name of the control loop, null for all
     * @param clVersion the version of the control loop, null for all
     * @return list of control loop node templates
     * @throws PfModelException on errors getting control loop definitions
     */
    public List<ToscaNodeTemplate> getControlLoopDefinitions(String clName, String clVersion) throws PfModelException {

        // @formatter:off
        ToscaTypedEntityFilter<ToscaNodeTemplate> nodeTemplateFilter = ToscaTypedEntityFilter
                .<ToscaNodeTemplate>builder()
                .name(clName)
                .version(clVersion)
                .type(CONTROL_LOOP_NODE_TYPE)
                .build();
        // @formatter:on

        return clProvider.getFilteredNodeTemplates(nodeTemplateFilter);
    }

    /**
     * Get the control loop elements from a control loop node template.
     *
     * @param controlLoopNodeTemplate the control loop node template
     * @return a list of the control loop element node templates in a control loop node template
     * @throws PfModelException on errors get control loop element node templates
     */
    public List<ToscaNodeTemplate> getControlLoopElementDefinitions(ToscaNodeTemplate controlLoopNodeTemplate)
            throws PfModelException {
        if (!CONTROL_LOOP_NODE_TYPE.equals(controlLoopNodeTemplate.getType())) {
            return Collections.emptyList();
        }

        if (MapUtils.isEmpty(controlLoopNodeTemplate.getProperties())) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> controlLoopElements =
                (List<Map<String, String>>) controlLoopNodeTemplate.getProperties().get("elements");

        if (CollectionUtils.isEmpty(controlLoopElements)) {
            return Collections.emptyList();
        }

        List<ToscaNodeTemplate> controlLoopElementList = new ArrayList<>();
        // @formatter:off
        controlLoopElementList.addAll(
                controlLoopElements
                        .stream()
                        .map(elementMap -> clProvider.getNodeTemplates(elementMap.get("name"),
                                elementMap.get("version")))
                        .flatMap(List::stream)
                        .collect(Collectors.toList())
        );
        // @formatter:on

        return controlLoopElementList;
    }

    /**
     * Get the requested control loop definitions.
     *
     * @param name the name of the definition to get, null for all definitions
     * @param version the version of the definition to get, null for all definitions
     * @return the control loop definitions
     * @throws PfModelException on errors getting control loop definitions
     */
    public ToscaServiceTemplate getToscaServiceTemplate(String name, String version) throws PfModelException {
        var serviceTemplates = new ToscaServiceTemplates();
        serviceTemplates.setServiceTemplates(modelsProvider.getServiceTemplateList(name, version));
        return serviceTemplates.getServiceTemplates().get(0);
    }

    /**
     * Get the requested json schema.
     *
     * @param section section of the tosca service template to get schema for
     * @return the specified tosca service template or section Json Schema
     * @throws PfModelException on errors with retrieving the classes
     * @throws JsonProcessingException on errors generating the schema
     */
    public String getToscaServiceTemplateSchema(String section) throws PfModelException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();

        switch (section) {
            case "data_types":
                mapper.acceptJsonFormatVisitor(mapper.constructType(ToscaDataType.class), visitor);
                break;
            case "capability_types":
                mapper.acceptJsonFormatVisitor(mapper.constructType(ToscaCapabilityType.class), visitor);
                break;
            case "node_types":
                mapper.acceptJsonFormatVisitor(mapper.constructType(ToscaNodeType.class), visitor);
                break;
            case "relationship_types":
                mapper.acceptJsonFormatVisitor(mapper.constructType(ToscaRelationshipType.class), visitor);
                break;
            case "policy_types":
                mapper.acceptJsonFormatVisitor(mapper.constructType(ToscaPolicyType.class), visitor);
                break;
            case "topology_template":
                mapper.acceptJsonFormatVisitor(mapper.constructType(ToscaTopologyTemplate.class), visitor);
                break;
            case "node_templates":
                mapper.acceptJsonFormatVisitor(mapper.getTypeFactory()
                    .constructCollectionType(List.class, ToscaNodeTemplate.class), visitor);
                break;
            default:
                mapper.acceptJsonFormatVisitor(mapper.constructType(ToscaServiceTemplate.class), visitor);
        }

        JsonSchema jsonSchema = visitor.finalSchema();
        String response = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);

        return response;
    }
}
