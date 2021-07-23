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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class provides the create, read and delete actions on Commissioning of Control Loop concepts in the database to
 * the callers.
 */
@Component
public class CommissioningProvider {
    public static final String CONTROL_LOOP_NODE_TYPE = "org.onap.policy.clamp.controlloop.ControlLoop";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommissioningProvider.class);

    private final PolicyModelsProvider modelsProvider;
    private final ControlLoopProvider clProvider;

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
     * Correct constraints null arrays before sending back the tosca service template.
     *
     * @param template the tosca service template where null array constraints need to be removed
     * @return the template with null-constraint arrays corrected
     * @throws PfModelException on errors getting new template
     */
    private ToscaServiceTemplate correctToscaTemplateNullConstraint(ToscaServiceTemplate template) {
        Map<String, ToscaNodeType> nodeTypes = template.getNodeTypes();
        for (Map.Entry<String, ToscaNodeType> nodeType: nodeTypes.entrySet()) {
            Map<String, ToscaProperty> properties = nodeType.getValue().getProperties();
            Map<String, ToscaProperty> newProperties = this.parseProperties(properties);
            nodeType.getValue().setProperties(newProperties);
        }
        template.setNodeTypes(nodeTypes);

        return template;
    }

    /**
     * Correct constraints for a give set of properties from node templates.
     *
     * @param properties the properties from a given node template
     * @return the new set of properties with null-constraint arrays corrected
     * @throws PfModelException on errors correcting the properties
     */
    private Map<String, ToscaProperty> parseProperties(Map<String, ToscaProperty> properties) {
        Map<String, ToscaProperty> newProperties = new HashMap<String, ToscaProperty>();
        for (Map.Entry<String, ToscaProperty> propType: properties.entrySet()) {
            ToscaProperty newProp = this.removeNullArrayFromConstraintsIfPresent(propType.getValue());
            newProperties.put(propType.getKey(), newProp);
        }

        return newProperties;
    }

    /**
     * Remove the null from inside the constraints array.
     *
     * @param prop a single property from a given node template
     * @return corrected properties
     * @throws PfModelException on errors correcting the single property
     */
    private ToscaProperty removeNullArrayFromConstraintsIfPresent(ToscaProperty prop) {
        ToscaProperty newProp = prop;

        if (newProp.getConstraints() != null && newProp.getConstraints().size() == 1) {
            if (newProp.getConstraints().get(0) == null) {
                newProp.setConstraints(null);
            }

        }
        return newProp;
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
     * Get the initial node types with common properties.
     *
     * @param fullNodeTypes map of all the node types in the specified template
     * @param common boolean to indicate whether common or instance properties are required
     * @return node types map that only has common properties
     * @throws PfModelException on errors getting node type with common properties
     */
    private Map<String, ToscaNodeType> getInitialNodeTypesMap(
        Map<String, ToscaNodeType> fullNodeTypes, boolean common) {

        Map<String, ToscaNodeType> tempNodeTypesMap = new HashMap<String, ToscaNodeType>();

        fullNodeTypes.forEach((key, nodeType) -> {
            ToscaNodeType tempToscaNodeType = new ToscaNodeType();
            tempToscaNodeType.setName(key);
            Map<String, ToscaProperty> tempCommonPropertyMap = new HashMap<String, ToscaProperty>();
            Map<String, ToscaProperty> tempInstancePropertyMap = new HashMap<String, ToscaProperty>();
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
            if (!tempCommonPropertyMap.isEmpty() && common) {
                tempToscaNodeType.setProperties(tempCommonPropertyMap);
                tempNodeTypesMap.put(key, tempToscaNodeType);
            }

            if (!tempInstancePropertyMap.isEmpty() && !common) {
                tempToscaNodeType.setProperties(tempInstancePropertyMap);
                tempNodeTypesMap.put(key, tempToscaNodeType);
            }

        });

        return tempNodeTypesMap;
    }

    /**
     * Get the node types derived from those that have common properties.
     *
     * @param initialNodeTypes map of all the node types in the specified template
     * @param filteredNodeTypes map of all the node types that have common properties
     * @return all node types that have common properties including their children
     * @throws PfModelException on errors getting node type with common properties
     */
    private Map<String, ToscaNodeType> getFinalNodeTypesMap(
        Map<String, ToscaNodeType> initialNodeTypes,
        Map<String, ToscaNodeType> filteredNodeTypes) {

        initialNodeTypes.forEach((key, nodeType) -> {
            ToscaNodeType tempToscaNodeType = new ToscaNodeType();
            tempToscaNodeType.setName(key);

            if (filteredNodeTypes.get(nodeType.getDerivedFrom()) != null) {
                tempToscaNodeType.setName(key);
                Map<String, ToscaProperty> mergedProps = new HashMap<String, ToscaProperty>();
                Map<String, ToscaProperty> finalMergedProps = mergedProps;
                filteredNodeTypes.get(nodeType.getDerivedFrom()).getProperties().forEach((propKey, prop) -> {
                    finalMergedProps.putIfAbsent(propKey, prop);
                });
                tempToscaNodeType.setProperties(finalMergedProps);
            } else {
                return;
            }
            filteredNodeTypes.put(key, tempToscaNodeType);
        });
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
    private Map<String, ToscaNodeType> getCommonOrInstancePropertiesFromNodeTypes(
        boolean common, String name, String version)
        throws PfModelException {
        var serviceTemplates = new ToscaServiceTemplates();
        serviceTemplates.setServiceTemplates(modelsProvider.getServiceTemplateList(name, version));
        Map<String, ToscaNodeType> tempNodeTypesMap =
            this.getInitialNodeTypesMap(serviceTemplates.getServiceTemplates().get(0).getNodeTypes(), common);

        Map<String, ToscaNodeType> finalNodeTypesMap = this.getFinalNodeTypesMap(
            serviceTemplates.getServiceTemplates().get(0).getNodeTypes(), tempNodeTypesMap);

        return finalNodeTypesMap;
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
        Map<String, ToscaNodeTemplate> initialNodeTemplates,
        Map<String, ToscaNodeType> nodeTypeProps) {

        Map<String, ToscaNodeTemplate> finalNodeTemplatesMap = new HashMap<String, ToscaNodeTemplate>();

        initialNodeTemplates.forEach((templateKey, template) -> {
            if (nodeTypeProps.containsKey(template.getType())) {
                Map<String, Object> mergedProps = new HashMap<String, Object>();
                mergedProps = template.getProperties();
                Map<String, Object> finalMergedProps = new HashMap<String, Object>();

                nodeTypeProps.get(template.getType()).getProperties().forEach((propKey, prop) -> {
                    finalMergedProps.putIfAbsent(propKey, prop);
                });

                template.setProperties(null);
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
    public Map<String, ToscaNodeTemplate> getNodeTemplatesWithCommonOrInstanceProperties(
        boolean common, String name, String version) throws PfModelException {

        Map<String, ToscaNodeType> commonOrInstanceNodeTypeProps =
            this.getCommonOrInstancePropertiesFromNodeTypes(common, name, version);

        var serviceTemplates = new ToscaServiceTemplates();
        serviceTemplates.setServiceTemplates(modelsProvider.getServiceTemplateList(name, version));

        Map<String, ToscaNodeTemplate> finalNodeTemplatesMap = this.getDerivedCommonOrInstanceNodeTemplates(
            serviceTemplates.getServiceTemplates().get(0).getToscaTopologyTemplate().getNodeTemplates(),
            commonOrInstanceNodeTypeProps);

        return finalNodeTemplatesMap;
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
    public Map<String, Object> getToscaServiceTemplateReduced(String name, String version) throws PfModelException {
        var serviceTemplates = new ToscaServiceTemplates();
        serviceTemplates.setServiceTemplates(modelsProvider.getServiceTemplateList(name, version));

        // This is needed because the constraints in some cases have an array with a null in it
        // This is not acceptable for the commissioning endpoint
        // TODO This should be fixed in a more permanent way elsewhere
        ToscaServiceTemplate parsedToscaServiceTemplate = this.correctToscaTemplateNullConstraint(
            serviceTemplates.getServiceTemplates().get(0));


        Map<String, Object> template = new HashMap<String, Object>();
        template.put("tosca_definitions_version", parsedToscaServiceTemplate.getToscaDefinitionsVersion());
        template.put("data_types", parsedToscaServiceTemplate.getDataTypes());
        template.put("policy_types", parsedToscaServiceTemplate.getPolicyTypes());
        template.put("node_types", parsedToscaServiceTemplate.getNodeTypes());
        template.put("topology_template", parsedToscaServiceTemplate.getToscaTopologyTemplate());

        return template;
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
