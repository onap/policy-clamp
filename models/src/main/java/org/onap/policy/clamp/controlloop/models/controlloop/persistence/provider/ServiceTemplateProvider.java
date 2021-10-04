/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response.Status;
import lombok.RequiredArgsConstructor;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServiceTemplateProvider {

    private final PolicyModelsProvider modelsProvider;

    /**
     * Create service template.
     *
     * @param serviceTemplate the service template to be created
     * @return the created service template
     * @throws PfModelException on errors creating the service template
     */
    public ToscaServiceTemplate createServiceTemplate(final ToscaServiceTemplate serviceTemplate)
            throws PfModelException {
        return modelsProvider.createServiceTemplate(serviceTemplate);
    }

    /**
     * Delete service template.
     *
     * @param name the name of the service template to delete.
     * @param version the version of the service template to delete.
     * @return the TOSCA service template that was deleted
     * @throws PfModelException on errors deleting policy types
     */
    public ToscaServiceTemplate deleteServiceTemplate(final String name, final String version) throws PfModelException {
        return modelsProvider.deleteServiceTemplate(name, version);
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
        var serviceTemplates = modelsProvider.getServiceTemplateList(name, version);
        if (serviceTemplates.isEmpty()) {
            throw new PfModelException(Status.NOT_FOUND, "Control Loop definitions not found");
        }
        return serviceTemplates.get(0);
    }

    /**
     * Get service templates.
     *
     * @param name the name of the topology template to get, set to null to get all service templates
     * @param version the version of the service template to get, set to null to get all service templates
     * @return the topology templates found
     * @throws PfModelException on errors getting service templates
     */
    public List<ToscaServiceTemplate> getServiceTemplateList(final String name, final String version)
            throws PfModelException {
        return modelsProvider.getServiceTemplateList(name, version);
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
     * @param serviceTemplate the ToscaServiceTemplate
     * @return the node types with common or instance properties
     * @throws PfModelException on errors getting node type properties
     */
    public Map<String, ToscaNodeType> getCommonOrInstancePropertiesFromNodeTypes(boolean common,
            ToscaServiceTemplate serviceTemplate) throws PfModelException {
        var tempNodeTypesMap = this.getInitialNodeTypesMap(serviceTemplate.getNodeTypes(), common);

        return this.getFinalNodeTypesMap(serviceTemplate.getNodeTypes(), tempNodeTypesMap);

    }

    /**
     * Get node templates with appropriate common or instance properties added.
     *
     * @param initialNodeTemplates map of all the node templates in the specified template
     * @param nodeTypeProps map of all the node types that have common or instance properties including children
     * @return all node templates with appropriate common or instance properties added
     * @throws PfModelException on errors getting map of node templates with common or instance properties added
     */
    public Map<String, ToscaNodeTemplate> getDerivedCommonOrInstanceNodeTemplates(
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
}
