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

package org.onap.policy.clamp.acm.runtime.commissioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.models.acm.concepts.Participant;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides the create, read and delete actions on Commissioning of automation composition concepts in the
 * database to the callers.
 */
@Service
@Transactional
public class CommissioningProvider {
    public static final String AUTOMATION_COMPOSITION_NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition";
    private static final String HYPHEN = "-";

    private final ServiceTemplateProvider serviceTemplateProvider;
    private final AutomationCompositionProvider acProvider;
    private static final Coder CODER = new StandardCoder();
    private final ParticipantProvider participantProvider;
    private final SupervisionHandler supervisionHandler;

    /**
     * Create a commissioning provider.
     *
     * @param serviceTemplateProvider the ServiceTemplate Provider
     * @param acProvider the AutomationComposition Provider
     * @param supervisionHandler the Supervision Handler
     * @param participantProvider the Participant Provider
     */
    public CommissioningProvider(ServiceTemplateProvider serviceTemplateProvider,
            AutomationCompositionProvider acProvider, SupervisionHandler supervisionHandler,
            ParticipantProvider participantProvider) {
        this.serviceTemplateProvider = serviceTemplateProvider;
        this.acProvider = acProvider;
        this.supervisionHandler = supervisionHandler;
        this.participantProvider = participantProvider;
    }

    /**
     * Create automation compositions from a service template.
     *
     * @param serviceTemplate the service template
     * @return the result of the commissioning operation
     * @throws PfModelException on creation errors
     */
    public CommissioningResponse createAutomationCompositionDefinitions(ToscaServiceTemplate serviceTemplate)
            throws PfModelException {

        if (verifyIfInstancePropertiesExists()) {
            throw new PfModelException(Status.BAD_REQUEST,
                    "Delete instances, to commission automation composition definitions");
        }
        serviceTemplate = serviceTemplateProvider.createServiceTemplate(serviceTemplate);
        List<Participant> participantList = participantProvider.getParticipants();
        if (!participantList.isEmpty()) {
            supervisionHandler.handleSendCommissionMessage(serviceTemplate.getName(), serviceTemplate.getVersion());
        }
        var response = new CommissioningResponse();
        // @formatter:off
        response.setAffectedAutomationCompositionDefinitions(
            serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .values()
                .stream()
                .map(template -> template.getKey().asIdentifier())
                .collect(Collectors.toList()));
        // @formatter:on

        return response;
    }

    /**
     * Delete the automation composition definition with the given name and version.
     *
     * @param name the name of the automation composition definition to delete
     * @param version the version of the automation composition to delete
     * @return the result of the deletion
     * @throws PfModelException on deletion errors
     */
    public CommissioningResponse deleteAutomationCompositionDefinition(String name, String version)
            throws PfModelException {

        if (verifyIfInstancePropertiesExists()) {
            throw new PfModelException(Status.BAD_REQUEST,
                    "Delete instances, to commission automation composition definitions");
        }
        List<Participant> participantList = participantProvider.getParticipants();
        if (!participantList.isEmpty()) {
            supervisionHandler.handleSendDeCommissionMessage();
        }
        serviceTemplateProvider.deleteServiceTemplate(name, version);
        var response = new CommissioningResponse();
        response.setAffectedAutomationCompositionDefinitions(List.of(new ToscaConceptIdentifier(name, version)));

        return response;
    }

    /**
     * Get automation composition node templates.
     *
     * @param acName the name of the automation composition, null for all
     * @param acVersion the version of the automation composition, null for all
     * @return list of automation composition node templates
     * @throws PfModelException on errors getting automation composition definitions
     */
    @Transactional(readOnly = true)
    public List<ToscaNodeTemplate> getAutomationCompositionDefinitions(String acName, String acVersion)
            throws PfModelException {

        // @formatter:off
        ToscaTypedEntityFilter<ToscaNodeTemplate> nodeTemplateFilter = ToscaTypedEntityFilter
                .<ToscaNodeTemplate>builder()
                .name(acName)
                .version(acVersion)
                .type(AUTOMATION_COMPOSITION_NODE_TYPE)
                .build();
        // @formatter:on

        return acProvider.getFilteredNodeTemplates(nodeTemplateFilter);
    }

    /**
     * Get the automation composition elements from a automation composition node template.
     *
     * @param automationCompositionNodeTemplate the automation composition node template
     * @return a list of the automation composition element node templates in a automation composition node template
     * @throws PfModelException on errors get automation composition element node templates
     */
    @Transactional(readOnly = true)
    public List<ToscaNodeTemplate> getAutomationCompositionElementDefinitions(
            ToscaNodeTemplate automationCompositionNodeTemplate) throws PfModelException {
        if (!AUTOMATION_COMPOSITION_NODE_TYPE.equals(automationCompositionNodeTemplate.getType())) {
            return Collections.emptyList();
        }

        if (MapUtils.isEmpty(automationCompositionNodeTemplate.getProperties())) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> automationCompositionElements =
                (List<Map<String, String>>) automationCompositionNodeTemplate.getProperties().get("elements");

        if (CollectionUtils.isEmpty(automationCompositionElements)) {
            return Collections.emptyList();
        }

        List<ToscaNodeTemplate> automationCompositionElementList = new ArrayList<>();
        // @formatter:off
        automationCompositionElementList.addAll(
                automationCompositionElements
                        .stream()
                        .map(elementMap -> acProvider.getNodeTemplates(elementMap.get("name"),
                                elementMap.get("version")))
                        .flatMap(List::stream)
                        .collect(Collectors.toList())
        );
        // @formatter:on

        return automationCompositionElementList;
    }

    /**
     * Get node templates with common properties added.
     *
     * @param name the name of the definition to use, null for all definitions
     * @param version the version of the definition to use, null for all definitions
     * @param instanceName automation composition name
     * @param common boolean indicating common or instance properties to be used
     * @return the nodes templates with common or instance properties
     * @throws PfModelException on errors getting common or instance properties from node_templates
     */
    @Transactional(readOnly = true)
    public Map<String, ToscaNodeTemplate> getNodeTemplatesWithCommonOrInstanceProperties(
            final String name, final String version, final String instanceName, final boolean common)
            throws PfModelException {

        if (common && verifyIfInstancePropertiesExists()) {
            throw new PfModelException(Status.BAD_REQUEST,
                    "Cannot create or edit common properties, delete all the instantiations first");
        }

        var serviceTemplateList = serviceTemplateProvider.getServiceTemplateList(name, version);

        if (serviceTemplateList.isEmpty()) {
            throw new PfModelException(Status.BAD_REQUEST,
                    "Tosca service template has to be commissioned before saving instance properties");
        }

        var commonOrInstanceNodeTypeProps =
                serviceTemplateProvider.getCommonOrInstancePropertiesFromNodeTypes(common, serviceTemplateList.get(0));

        var serviceTemplates = new ToscaServiceTemplates();
        serviceTemplates.setServiceTemplates(filterToscaNodeTemplateInstance(serviceTemplateList, instanceName));

        return serviceTemplateProvider.getDerivedCommonOrInstanceNodeTemplates(
                serviceTemplates.getServiceTemplates().get(0).getToscaTopologyTemplate().getNodeTemplates(),
                commonOrInstanceNodeTypeProps);
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
        return serviceTemplateProvider.getToscaServiceTemplate(name, version);
    }

    /**
     * Get All the requested automation composition definitions.
     *
     * @return the automation composition definitions
     * @throws PfModelException on errors getting automation composition definitions
     */
    @Transactional(readOnly = true)
    public List<ToscaServiceTemplate> getAllToscaServiceTemplate() throws PfModelException {
        return serviceTemplateProvider.getAllServiceTemplates();
    }

    /**
     * Get the tosca service template with only required sections.
     *
     * @param name the name of the template to get, null for all definitions
     * @param version the version of the template to get, null for all definitions
     * @param instanceName automation composition name
     * @return the tosca service template
     * @throws PfModelException on errors getting tosca service template
     */
    @Transactional(readOnly = true)
    public String getToscaServiceTemplateReduced(
            final String name, final String version, final String instanceName)
            throws PfModelException {

        var serviceTemplateList = serviceTemplateProvider.getServiceTemplateList(name, version);

        List<ToscaServiceTemplate> filteredServiceTemplateList =
                filterToscaNodeTemplateInstance(serviceTemplateList, instanceName);

        if (filteredServiceTemplateList.isEmpty()) {
            throw new PfModelException(Status.BAD_REQUEST, "Invalid Service Template");
        }

        ToscaServiceTemplate fullTemplate = filteredServiceTemplateList.get(0);

        var template = new HashMap<String, Object>();
        template.put("tosca_definitions_version", fullTemplate.getToscaDefinitionsVersion());
        template.put("data_types", fullTemplate.getDataTypes());
        template.put("policy_types", fullTemplate.getPolicyTypes());
        template.put("node_types", fullTemplate.getNodeTypes());
        template.put("topology_template", fullTemplate.getToscaTopologyTemplate());

        try {
            return CODER.encode(template);

        } catch (CoderException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Converion to Json Schema failed", e);
        }
    }

    /**
     * Filters service templates if is not an instantiation type.
     *
     * @param serviceTemplates tosca service template
     * @param instanceName     automation composition name
     * @return List of tosca service templates
     */
    private List<ToscaServiceTemplate> filterToscaNodeTemplateInstance(
            List<ToscaServiceTemplate> serviceTemplates, String instanceName) {

        List<ToscaServiceTemplate> toscaServiceTemplates = new ArrayList<>();

        serviceTemplates.forEach(serviceTemplate -> {

            Map<String, ToscaNodeTemplate> toscaNodeTemplates = new HashMap<>();

            serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().forEach((key, nodeTemplate) -> {
                if (StringUtils.isNotEmpty(instanceName) && nodeTemplate.getName().contains(instanceName)) {
                    toscaNodeTemplates.put(key, nodeTemplate);
                } else if (!nodeTemplate.getName().contains(HYPHEN)) {
                    toscaNodeTemplates.put(key, nodeTemplate);
                }
            });

            serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().clear();
            serviceTemplate.getToscaTopologyTemplate().setNodeTemplates(toscaNodeTemplates);

            toscaServiceTemplates.add(serviceTemplate);
        });

        return toscaServiceTemplates;
    }

    /**
     * Validates to see if there is any instance properties saved.
     *
     * @return true if exists instance properties
     */
    private boolean verifyIfInstancePropertiesExists() {
        return acProvider.getAllNodeTemplates().stream()
                .anyMatch(nodeTemplate -> nodeTemplate.getKey().getName().contains(HYPHEN));

    }
}
