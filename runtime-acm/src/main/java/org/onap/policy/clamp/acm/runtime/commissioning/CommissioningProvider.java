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

import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates;
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

    private final AcDefinitionProvider acDefinitionProvider;
    private final AutomationCompositionProvider acProvider;
    private final ParticipantProvider participantProvider;
    private final SupervisionHandler supervisionHandler;

    /**
     * Create a commissioning provider.
     *
     * @param acDefinitionProvider the ServiceTemplate Provider
     * @param acProvider the AutomationComposition Provider
     * @param supervisionHandler the Supervision Handler
     * @param participantProvider the Participant Provider
     */
    public CommissioningProvider(AcDefinitionProvider acDefinitionProvider, AutomationCompositionProvider acProvider,
            SupervisionHandler supervisionHandler, ParticipantProvider participantProvider) {
        this.acDefinitionProvider = acDefinitionProvider;
        this.acProvider = acProvider;
        this.supervisionHandler = supervisionHandler;
        this.participantProvider = participantProvider;
    }

    private CommissioningResponse createCommissioningResponse(UUID compositionId,
            ToscaServiceTemplate serviceTemplate) {
        var response = new CommissioningResponse();
        response.setCompositionId(compositionId);
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
     * Create automation compositions from a service template.
     *
     * @param serviceTemplate the service template
     * @return the result of the commissioning operation
     */
    public CommissioningResponse createAutomationCompositionDefinitions(ToscaServiceTemplate serviceTemplate) {

        var acmDefinition = acDefinitionProvider.createAutomationCompositionDefinition(serviceTemplate);
        serviceTemplate = acmDefinition.getServiceTemplate();
        var participantList = participantProvider.getParticipants();
        if (!participantList.isEmpty()) {
            supervisionHandler.handleSendCommissionMessage(acmDefinition);
        }
        return createCommissioningResponse(acmDefinition.getCompositionId(), serviceTemplate);
    }

    /**
     * Update Composition Definition.
     *
     * @param compositionId The UUID of the automation composition definition to update
     * @param serviceTemplate the service template
     * @return the result of the commissioning operation
     */
    public CommissioningResponse updateCompositionDefinition(UUID compositionId, ToscaServiceTemplate serviceTemplate) {

        var automationCompositions = acProvider.getAcInstancesByCompositionId(compositionId);
        if (!automationCompositions.isEmpty()) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    "There are ACM instances, Update of ACM Definition not allowed");
        }
        acDefinitionProvider.updateServiceTemplate(compositionId, serviceTemplate);

        return createCommissioningResponse(compositionId, serviceTemplate);
    }

    /**
     * Delete the automation composition definition with the given name and version.
     *
     * @param compositionId The UUID of the automation composition definition to delete
     * @return the result of the deletion
     */
    public CommissioningResponse deleteAutomationCompositionDefinition(UUID compositionId) {

        if (verifyIfInstanceExists(compositionId)) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    "Delete instances, to commission automation composition definitions");
        }
        var participantList = participantProvider.getParticipants();
        if (!participantList.isEmpty()) {
            supervisionHandler.handleSendDeCommissionMessage(compositionId);
        }
        var serviceTemplate = acDefinitionProvider.deleteAcDefintion(compositionId);
        return createCommissioningResponse(compositionId, serviceTemplate);
    }

    /**
     * Get automation composition definition.
     *
     * @param acName the name of the automation composition, null for all
     * @param acVersion the version of the automation composition, null for all
     * @return automation composition definition
     * @throws PfModelException on errors getting automation composition definitions
     */
    @Transactional(readOnly = true)
    public ToscaServiceTemplates getAutomationCompositionDefinitions(String acName, String acVersion) {

        var result = new ToscaServiceTemplates();
        result.setServiceTemplates(acDefinitionProvider.getServiceTemplateList(acName, acVersion));
        return result;
    }

    /**
     * Get automation composition definition.
     *
     * @param compositionId the compositionId
     * @return automation composition definition
     */
    @Transactional(readOnly = true)
    public ToscaServiceTemplate getAutomationCompositionDefinitions(UUID compositionId) {

        return acDefinitionProvider.getAcDefinition(compositionId);
    }

    /**
     * Validates to see if there is any instance saved.
     *
     * @return true if exists instance
     */
    private boolean verifyIfInstanceExists(UUID compositionId) {
        return !acProvider.getAcInstancesByCompositionId(compositionId).isEmpty();
    }
}
