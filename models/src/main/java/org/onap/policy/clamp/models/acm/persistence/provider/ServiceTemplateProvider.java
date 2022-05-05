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

package org.onap.policy.clamp.models.acm.persistence.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.persistence.repository.ToscaServiceTemplateRepository;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ServiceTemplateProvider {

    private final ToscaServiceTemplateRepository serviceTemplateRepository;

    /**
     * Create service template.
     *
     * @param serviceTemplate the service template to be created
     * @return the created service template
     * @throws PfModelException on errors creating the service template
     */
    public ToscaServiceTemplate createServiceTemplate(final ToscaServiceTemplate serviceTemplate)
            throws PfModelException {
        try {
            var result = serviceTemplateRepository.save(ProviderUtils.getJpaAndValidate(serviceTemplate,
                    JpaToscaServiceTemplate::new, "toscaServiceTemplate"));
            return result.toAuthorative();
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error in save serviceTemplate", e);
        }
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
        var serviceTemplateKey = new PfConceptKey(name, version);
        var jpaDelete = serviceTemplateRepository.findById(serviceTemplateKey);
        if (jpaDelete.isEmpty()) {
            String errorMessage = "delete of serviceTemplate \"" + serviceTemplateKey.getId()
                    + "\" failed, serviceTemplate does not exist";
            throw new PfModelException(Response.Status.BAD_REQUEST, errorMessage);
        }
        serviceTemplateRepository.deleteById(serviceTemplateKey);
        return jpaDelete.get().toAuthorative();
    }

    /**
     * Get the requested automation composition definitions.
     *
     * @param name the name of the definition to get, null for all definitions
     * @param version the version of the definition to get, null for all definitions
     * @return the automation composition definitions
     * @throws PfModelException on errors getting automation composition definitions
     */
    @Transactional(readOnly = true)
    public ToscaServiceTemplate getToscaServiceTemplate(String name, String version) throws PfModelException {
        var serviceTemplateKey = new PfConceptKey(name, version);
        var jpaServiceTemplates = serviceTemplateRepository.findById(serviceTemplateKey);
        if (jpaServiceTemplates.isEmpty()) {
            throw new PfModelException(Status.NOT_FOUND, "Automation composition definitions not found");
        }
        return jpaServiceTemplates.get().toAuthorative();
    }

    /**
     * Get service templates.
     *
     * @return the topology templates found
     * @throws PfModelException on errors getting service templates
     */
    @Transactional(readOnly = true)
    public List<ToscaServiceTemplate> getAllServiceTemplates() throws PfModelException {
        var jpaList = serviceTemplateRepository.findAll();
        return ProviderUtils.asEntityList(jpaList);
    }

    /**
     * Get service templates.
     *
     * @param name the name of the topology template to get, set to null to get all service templates
     * @param version the version of the service template to get, set to null to get all service templates
     * @return the topology templates found
     * @throws PfModelException on errors getting service templates
     */
    @Transactional(readOnly = true)
    public List<ToscaServiceTemplate> getServiceTemplateList(final String name, final String version)
            throws PfModelException {
        var jpaList = serviceTemplateRepository.getFiltered(JpaToscaServiceTemplate.class, name, version);
        return ProviderUtils.asEntityList(jpaList);
    }

    /**
     * Get the initial node types with common or instance properties.
     *
     * @param fullNodeTypes map of all the node types in the specified template
     * @param common boolean to indicate whether common or instance properties are required
     * @return node types map that only has common properties
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
     * Get the node types derived from those that have been saved by instantiation.
     *
     * @param initialNodeTypes map of all the node types in the specified template
     * @param filteredNodeTypes map of all the node types that have common or instance properties
     * @param instanceName automation composition name
     * @return all node types that have common properties including their children
     */
    private Map<String, ToscaNodeType> getFinalSavedInstanceNodeTypesMap(
            Map<String, ToscaNodeType> initialNodeTypes,
            Map<String, ToscaNodeType> filteredNodeTypes, String instanceName) {

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
     * Get the requested node types by automation composition.
     *
     * @param instanceName automation composition name
     * @param serviceTemplate the ToscaServiceTemplate
     * @return the node types with common or instance properties
     */
    public Map<String, ToscaNodeType> getSavedInstanceInstancePropertiesFromNodeTypes(
            String instanceName, ToscaServiceTemplate serviceTemplate) {
        var tempNodeTypesMap =
                this.getInitialNodeTypesMap(serviceTemplate.getNodeTypes(), false);

        return this.getFinalSavedInstanceNodeTypesMap(serviceTemplate.getNodeTypes(), tempNodeTypesMap, instanceName);

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
