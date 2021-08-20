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
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaCapabilityType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
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
public class CommissioningProvider {
    public static final String CONTROL_LOOP_NODE_TYPE = "org.onap.policy.clamp.controlloop.ControlLoop";

    private final PolicyModelsProvider modelsProvider;
    private final ControlLoopProvider clProvider;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Object lockit = new Object();

    /**
     * Create a commissioning provider.
     *
     * @param modelsProvider the PolicyModelsProvider
     * @param clProvider the ControlLoopProvider
     */
    public CommissioningProvider(PolicyModelsProvider modelsProvider, ControlLoopProvider clProvider) {
        this.modelsProvider = modelsProvider;
        this.clProvider = clProvider;
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    /**
     * Create control loops from a service template.
     *
     * @param serviceTemplate the service template
     * @return the result of the commissioning operation
     * @throws PfModelException on creation errors
     */
    public CommissioningResponse createControlLoopDefinitions(ToscaServiceTemplate serviceTemplate)
        throws PfModelException, ControlLoopException {

        if (verifyIfInstancePropertiesExists()) {
            throw new ControlLoopException(Status.BAD_REQUEST,
                "Delete instances, to commission control loop definitions");
        }

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
    public CommissioningResponse deleteControlLoopDefinition(String name, String version)
        throws PfModelException, ControlLoopException {

        if (verifyIfInstancePropertiesExists()) {
            throw new ControlLoopException(Status.BAD_REQUEST,
                "Delete instances, to commission control loop definitions");
        }

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
     * Get the initial node types with common or instance properties.
     *
     * @param fullNodeTypes map of all the node types in the specified template
     * @param common boolean to indicate whether common or instance properties are required
     * @return node types map that only has common properties
     * @throws PfModelException on errors getting node type with common properties
     */
    private Map<String, ToscaNodeType> getInitialNodeTypesMap(Map<String, ToscaNodeType> fullNodeTypes,
            boolean common) {

        var tempNodeTypesMap = new HashMap<String, ToscaNodeType>();

        fullNodeTypes.forEach((key, nodeType) -> {
            var tempToscaNodeType = new ToscaNodeType();
            tempToscaNodeType.setName(key);

            var resultantPropertyMap = findCommonOrInstancePropsInNodeTypes(nodeType, common);

            if (!resultantPropertyMap.isEmpty()) {
                tempToscaNodeType.setProperties(resultantPropertyMap);
                tempNodeTypesMap.put(key, tempToscaNodeType);
            }
        });
        return tempNodeTypesMap;
    }

    private Map<String, ToscaProperty> findCommonOrInstancePropsInNodeTypes(ToscaNodeType nodeType, boolean common) {

        var tempCommonPropertyMap = new HashMap<String, ToscaProperty>();
        var tempInstancePropertyMap = new HashMap<String, ToscaProperty>();

        nodeType.getProperties().forEach((propKey, prop) -> {

            if (prop.getMetadata() != null) {
                prop.getMetadata().forEach((k, v) -> {
                    if (k.equals("common") && v.equals("true") && common) {
                        tempCommonPropertyMap.put(propKey, prop);
                    } else if (k.equals("common") && v.equals("false") && !common) {
                        tempInstancePropertyMap.put(propKey, prop);
                    }

                });
            } else {
                tempInstancePropertyMap.put(propKey, prop);
            }
        });

        if (tempCommonPropertyMap.isEmpty() && !common) {
            return tempInstancePropertyMap;
        } else {
            return tempCommonPropertyMap;
        }
    }

    /**
     * Get the node types derived from those that have common properties.
     *
     * @param initialNodeTypes map of all the node types in the specified template
     * @param filteredNodeTypes map of all the node types that have common or instance properties
     * @return all node types that have common properties including their children
     * @throws PfModelException on errors getting node type with common properties
     */
    private Map<String, ToscaNodeType> getFinalNodeTypesMap(Map<String, ToscaNodeType> initialNodeTypes,
            Map<String, ToscaNodeType> filteredNodeTypes) {
        for (var i = 0; i < initialNodeTypes.size(); i++) {
            initialNodeTypes.forEach((key, nodeType) -> {
                var tempToscaNodeType = new ToscaNodeType();
                tempToscaNodeType.setName(key);

                if (filteredNodeTypes.get(nodeType.getDerivedFrom()) != null) {
                    tempToscaNodeType.setName(key);

                    var finalProps = new HashMap<String, ToscaProperty>(
                            filteredNodeTypes.get(nodeType.getDerivedFrom()).getProperties());

                    tempToscaNodeType.setProperties(finalProps);
                } else {
                    return;
                }
                filteredNodeTypes.putIfAbsent(key, tempToscaNodeType);

            });
        }
        return filteredNodeTypes;
    }

    /**
     * Get the requested node types with common or instance properties.
     *
     * @param common boolean indicating common or instance properties
     * @param name the name of the definition to get, null for all definitions
     * @param version the version of the definition to get, null for all definitions
     * @return the node types with common or instance properties
     * @throws PfModelException on errors getting node type properties
     */
    private Map<String, ToscaNodeType> getCommonOrInstancePropertiesFromNodeTypes(boolean common, String name,
            String version) throws PfModelException {
        var serviceTemplates = new ToscaServiceTemplates();
        serviceTemplates.setServiceTemplates(modelsProvider.getServiceTemplateList(name, version));
        var tempNodeTypesMap =
                this.getInitialNodeTypesMap(serviceTemplates.getServiceTemplates().get(0).getNodeTypes(), common);

        return this.getFinalNodeTypesMap(serviceTemplates.getServiceTemplates().get(0).getNodeTypes(),
                tempNodeTypesMap);

    }

    /**
     * Get node templates with appropriate common or instance properties added.
     *
     * @param initialNodeTemplates map of all the node templates in the specified template
     * @param nodeTypeProps map of all the node types that have common or instance properties including children
     * @return all node templates with appropriate common or instance properties added
     * @throws PfModelException on errors getting map of node templates with common or instance properties added
     */
    private Map<String, ToscaNodeTemplate> getDerivedCommonOrInstanceNodeTemplates(
            Map<String, ToscaNodeTemplate> initialNodeTemplates, Map<String, ToscaNodeType> nodeTypeProps) {

        var finalNodeTemplatesMap = new HashMap<String, ToscaNodeTemplate>();

        initialNodeTemplates.forEach((templateKey, template) -> {
            if (nodeTypeProps.containsKey(template.getType())) {
                var finalMergedProps = new HashMap<String, Object>();

                nodeTypeProps.get(template.getType()).getProperties().forEach(finalMergedProps::putIfAbsent);

                template.setProperties(finalMergedProps);

                finalNodeTemplatesMap.put(templateKey, template);
            } else {
                return;
            }
        });
        return finalNodeTemplatesMap;
    }

    /**
     * Get node templates with common properties added.
     *
     * @param common boolean indicating common or instance properties to be used
     * @param name the name of the definition to use, null for all definitions
     * @param version the version of the definition to use, null for all definitions
     * @return the nodes templates with common or instance properties
     * @throws PfModelException on errors getting common or instance properties from node_templates
     */
    public Map<String, ToscaNodeTemplate> getNodeTemplatesWithCommonOrInstanceProperties(boolean common, String name,
            String version) throws PfModelException {

        var commonOrInstanceNodeTypeProps = this.getCommonOrInstancePropertiesFromNodeTypes(common, name, version);

        var serviceTemplates = new ToscaServiceTemplates();
        serviceTemplates.setServiceTemplates(modelsProvider.getServiceTemplateList(name, version));

        return this.getDerivedCommonOrInstanceNodeTemplates(
                serviceTemplates.getServiceTemplates().get(0).getToscaTopologyTemplate().getNodeTemplates(),
                commonOrInstanceNodeTypeProps);
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
     * Get the tosca service template with only required sections.
     *
     * @param name the name of the template to get, null for all definitions
     * @param version the version of the template to get, null for all definitions
     * @return the tosca service template
     * @throws PfModelException on errors getting tosca service template
     */
    public String getToscaServiceTemplateReduced(String name, String version)
        throws PfModelException {

        var serviceTemplates = new ToscaServiceTemplates();
        serviceTemplates.setServiceTemplates(modelsProvider.getServiceTemplateList(name, version));

        ToscaServiceTemplate fullTemplate = serviceTemplates.getServiceTemplates().get(0);

        var template = new HashMap<String, Object>();
        template.put("tosca_definitions_version", fullTemplate.getToscaDefinitionsVersion());
        template.put("data_types", fullTemplate.getDataTypes());
        template.put("policy_types", fullTemplate.getPolicyTypes());
        template.put("node_types", fullTemplate.getNodeTypes());
        template.put("topology_template", fullTemplate.getToscaTopologyTemplate());

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(template);

        } catch (JsonProcessingException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Converion to Json Schema failed", e);
        }
    }

    /**
     * Get the requested json schema.
     *
     * @param section section of the tosca service template to get schema for
     * @return the specified tosca service template or section Json Schema
     * @throws PfModelException on errors with retrieving the classes
     */
    public String getToscaServiceTemplateSchema(String section) throws PfModelException {
        var visitor = new SchemaFactoryWrapper();

        try {
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
                    mapper.acceptJsonFormatVisitor(
                            mapper.getTypeFactory().constructCollectionType(List.class, ToscaNodeTemplate.class),
                            visitor);
                    break;
                default:
                    mapper.acceptJsonFormatVisitor(mapper.constructType(ToscaServiceTemplate.class), visitor);
            }

            var jsonSchema = visitor.finalSchema();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema);
        } catch (JsonProcessingException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Converion to Json Schema failed", e);
        }
    }

    /**
     * Validates to see if there is any instance properties saved.
     *
     * @return true if exists instance properties
     */
    private Boolean verifyIfInstancePropertiesExists() {
        return clProvider.getNodeTemplates(null, null).stream()
            .anyMatch(nodeTemplate -> nodeTemplate.getKey().getName().contains("_Instance"));

    }
}
