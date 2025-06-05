/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

import io.opentelemetry.context.Context;
import jakarta.ws.rs.core.Response.Status;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup;
import org.onap.policy.clamp.acm.runtime.supervision.AcmThreadFactory;
import org.onap.policy.clamp.acm.runtime.supervision.comm.ParticipantPrimePublisher;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AcTypeStateResolver;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides the creation, read and delete actions on Commissioning of automation composition concepts in the
 * database to the callers.
 */
@Service
@RequiredArgsConstructor
public class CommissioningProvider {

    private final AcDefinitionProvider acDefinitionProvider;
    private final AutomationCompositionProvider acProvider;
    private final ParticipantProvider participantProvider;
    private final AcTypeStateResolver acTypeStateResolver;
    private final ParticipantPrimePublisher participantPrimePublisher;
    private final AcRuntimeParameterGroup acRuntimeParameterGroup;

    private final ExecutorService executor =
            Context.taskWrapping(Executors.newFixedThreadPool(1, new AcmThreadFactory()));

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
                .toList());
        // @formatter:on

        return response;
    }

    /**
     * Create automation composition from a service template.
     *
     * @param serviceTemplate the service template
     * @return the result of the commissioning operation
     */
    @Transactional
    public CommissioningResponse createAutomationCompositionDefinition(ToscaServiceTemplate serviceTemplate) {

        var acmDefinition = acDefinitionProvider.createAutomationCompositionDefinition(serviceTemplate,
                acRuntimeParameterGroup.getAcmParameters().getToscaElementName(),
                acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName());
        serviceTemplate = acmDefinition.getServiceTemplate();
        return createCommissioningResponse(acmDefinition.getCompositionId(), serviceTemplate);
    }

    /**
     * Update Composition Definition.
     *
     * @param compositionId The UUID of the automation composition definition to update
     * @param serviceTemplate the service template
     * @return the result of the commissioning operation
     */
    @Transactional
    public CommissioningResponse updateCompositionDefinition(UUID compositionId, ToscaServiceTemplate serviceTemplate) {
        if (verifyIfInstanceExists(compositionId)) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    "There are ACM instances, Update of ACM Definition not allowed");
        }
        var acDefinition = acDefinitionProvider.getAcDefinition(compositionId);
        if (!AcTypeState.COMMISSIONED.equals(acDefinition.getState())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    "ACM not in COMMISSIONED state, Update of ACM Definition not allowed");
        }
        acDefinitionProvider.updateServiceTemplate(compositionId, serviceTemplate,
                acRuntimeParameterGroup.getAcmParameters().getToscaElementName(),
                acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName());

        return createCommissioningResponse(compositionId, serviceTemplate);
    }

    /**
     * Delete the automation composition definition with the given name and version.
     *
     * @param compositionId The UUID of the automation composition definition to delete
     * @return the result of the deletion
     */
    @Transactional
    public CommissioningResponse deleteAutomationCompositionDefinition(UUID compositionId) {
        if (verifyIfInstanceExists(compositionId)) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    "Delete instances, to commission automation composition definitions");
        }
        var acDefinition = acDefinitionProvider.getAcDefinition(compositionId);
        if (!AcTypeState.COMMISSIONED.equals(acDefinition.getState())) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST,
                    "ACM not in COMMISSIONED state, Delete of ACM Definition not allowed");
        }
        var serviceTemplate = acDefinitionProvider.deleteAcDefinition(compositionId);
        return createCommissioningResponse(compositionId, serviceTemplate);
    }

    /**
     * Get automation composition definition.
     *
     * @param acName the name of the automation composition, null for all
     * @param acVersion the version of the automation composition, null for all
     * @param pageable the Pageable
     * @return automation composition definition
     */
    @Transactional(readOnly = true)
    public ToscaServiceTemplates getAutomationCompositionDefinitions(String acName, String acVersion,
            @NonNull Pageable pageable) {
        var result = new ToscaServiceTemplates();
        result.setServiceTemplates(acDefinitionProvider.getServiceTemplateList(acName, acVersion, pageable));
        return result;
    }

    /**
     * Get automation composition definition.
     *
     * @param compositionId the compositionId
     * @return automation composition definition
     */
    @Transactional(readOnly = true)
    public AutomationCompositionDefinition getAutomationCompositionDefinition(UUID compositionId) {

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

    /**
     * Composition Definition Priming.
     *
     * @param compositionId the compositionId
     * @param acTypeStateUpdate the ACMTypeStateUpdate
     */
    public void compositionDefinitionPriming(UUID compositionId, AcTypeStateUpdate acTypeStateUpdate) {
        if (verifyIfInstanceExists(compositionId)) {
            throw new PfModelRuntimeException(Status.BAD_REQUEST, "There are instances, Priming/Depriming not allowed");
        }
        var acmDefinition = acDefinitionProvider.getAcDefinition(compositionId);
        var stateOrdered = acTypeStateResolver.resolve(acTypeStateUpdate.getPrimeOrder(), acmDefinition.getState(),
                acmDefinition.getStateChangeResult());
        switch (stateOrdered) {
            case PRIME:
                prime(acmDefinition);
                break;

            case DEPRIME:
                deprime(acmDefinition);
                break;

            default:
                throw new PfModelRuntimeException(Status.BAD_REQUEST, "Not valid " + acTypeStateUpdate.getPrimeOrder());
        }
    }

    private void prime(AutomationCompositionDefinition acmDefinition) {
        var preparation = participantPrimePublisher.prepareParticipantPriming(acmDefinition);
        acDefinitionProvider.updateAcDefinition(acmDefinition,
                acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName());

        executor.execute(
                () -> participantPrimePublisher.sendPriming(preparation, acmDefinition.getCompositionId(), null));
    }

    private void deprime(AutomationCompositionDefinition acmDefinition) {
        acmDefinition.setStateChangeResult(StateChangeResult.NO_ERROR);
        var participantIds = new HashSet<UUID>();
        for (var elementState : acmDefinition.getElementStateMap().values()) {
            var participantId = elementState.getParticipantId();
            if (participantId != null) {
                elementState.setState(AcTypeState.DEPRIMING);
                participantIds.add(participantId);
            }
        }
        if (!participantIds.isEmpty()) {
            participantProvider.verifyParticipantState(participantIds);
        }
        acmDefinition.setState(AcTypeState.DEPRIMING);
        acmDefinition.setLastMsg(TimestampHelper.now());
        acDefinitionProvider.updateAcDefinition(acmDefinition,
                acRuntimeParameterGroup.getAcmParameters().getToscaCompositionName());

        executor.execute(() -> participantPrimePublisher.sendDepriming(acmDefinition.getCompositionId()));
    }

}
