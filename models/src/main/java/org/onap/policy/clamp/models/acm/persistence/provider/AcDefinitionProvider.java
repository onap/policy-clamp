/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.document.base.ToscaServiceTemplateValidation;
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionDefinitionRepository;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.clamp.models.acm.utils.TimestampHelper;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AcDefinitionProvider {

    private static final String NAME = "AutomationCompositionDefinition";

    private final AutomationCompositionDefinitionRepository acmDefinitionRepository;

    /**
     * Create Automation Composition Definition.
     *
     * @param serviceTemplate the service template to be created
     * @return the created ACM Definition
     */
    public AutomationCompositionDefinition createAutomationCompositionDefinition(
            final ToscaServiceTemplate serviceTemplate, final String toscaElementName, String toscaCompositionName) {
        var acmDefinition = new AutomationCompositionDefinition();
        var compositionId = UUID.randomUUID();
        acmDefinition.setCompositionId(compositionId);
        acmDefinition.setState(AcTypeState.COMMISSIONED);
        if (serviceTemplate.getMetadata() == null) {
            serviceTemplate.setMetadata(new HashMap<>());
        }
        acmDefinition.setLastMsg(TimestampHelper.now());
        serviceTemplate.getMetadata().put("compositionId", compositionId);
        acmDefinition.setServiceTemplate(serviceTemplate);
        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate, toscaElementName);
        if (acElements.isEmpty()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    "NodeTemplate with element type " + toscaElementName + " must exist!");
        }
        acmDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, AcTypeState.COMMISSIONED));
        var jpaAcmDefinition = ProviderUtils.getJpaAndValidate(acmDefinition, JpaAutomationCompositionDefinition::new,
            acmDefinition.getClass().getSimpleName());
        var validationResult = new BeanValidationResult(acmDefinition.getClass().getSimpleName(), acmDefinition);
        ToscaServiceTemplateValidation.validate(validationResult, jpaAcmDefinition.getServiceTemplate(),
                toscaCompositionName);
        if (! validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        var result = acmDefinitionRepository.save(jpaAcmDefinition);

        return result.toAuthorative();
    }

    /**
     * Update a commissioned ServiceTemplate.
     *
     * @param compositionId The UUID of the automation composition definition to delete
     * @param serviceTemplate the service template to be created
     */
    public void updateServiceTemplate(UUID compositionId, ToscaServiceTemplate serviceTemplate, String toscaElementName,
                                      String toscaCompositionName) {
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(compositionId);
        acmDefinition.setState(AcTypeState.COMMISSIONED);
        acmDefinition.setLastMsg(TimestampHelper.now());
        acmDefinition.setServiceTemplate(serviceTemplate);
        var acElements =
                AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate, toscaElementName);
        if (acElements.isEmpty()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    "NodeTemplate with element type " + toscaElementName + " must exist!");
        }
        acmDefinition.setElementStateMap(AcmUtils.createElementStateMap(acElements, AcTypeState.COMMISSIONED));
        updateAcDefinition(acmDefinition, toscaCompositionName);
    }

    /**
     * Update the AutomationCompositionDefinition.
     *
     * @param acDefinition the AutomationCompositionDefinition to be updated
     */
    public void updateAcDefinition(AutomationCompositionDefinition acDefinition, String toscaCompositionName) {
        var jpaAcmDefinition = ProviderUtils.getJpaAndValidate(acDefinition, JpaAutomationCompositionDefinition::new,
                NAME);
        var validationResult = new BeanValidationResult(NAME, acDefinition);
        ToscaServiceTemplateValidation.validate(validationResult, jpaAcmDefinition.getServiceTemplate(),
                toscaCompositionName);
        if (! validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }
        acmDefinitionRepository.save(jpaAcmDefinition);
        acmDefinitionRepository.flush();
    }

    /**
     * Update Ac Definition with unchanged service template.
     *
     * @param acDefinition the AutomationCompositionDefinition to be updated
     */
    public void updateAcDefinitionState(AutomationCompositionDefinition acDefinition) {
        var jpaAcmDefinition = ProviderUtils.getJpaAndValidate(acDefinition, JpaAutomationCompositionDefinition::new,
                NAME);
        acmDefinitionRepository.save(jpaAcmDefinition);
        acmDefinitionRepository.flush();
    }

    /**
     * Delete Automation Composition Definition.
     *
     * @param compositionId The UUID of the automation composition definition to delete
     * @return the TOSCA service template that was deleted
     */
    public ToscaServiceTemplate deleteAcDefinition(UUID compositionId) {
        var jpaDelete = acmDefinitionRepository.findById(compositionId.toString());
        if (jpaDelete.isEmpty()) {
            String errorMessage = "delete of Automation Composition Definition \"" + compositionId
                    + "\" failed, Automation Composition Definition does not exist";
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, errorMessage);
        }

        var item = jpaDelete.get().getServiceTemplate();
        acmDefinitionRepository.deleteById(compositionId.toString());
        return item.toAuthorative();
    }

    /**
     * Get the requested automation composition definitions.
     *
     * @param compositionId The UUID of the automation composition definition to delete
     * @return the automation composition definition
     */
    @Transactional(readOnly = true)
    public AutomationCompositionDefinition getAcDefinition(UUID compositionId) {
        var jpaGet = acmDefinitionRepository.findById(compositionId.toString());
        if (jpaGet.isEmpty()) {
            String errorMessage =
                    "Get serviceTemplate \"" + compositionId + "\" failed, serviceTemplate does not exist";
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, errorMessage);
        }
        return jpaGet.get().toAuthorative();
    }

    /**
     * Get the requested automation composition definition.
     *
     * @param compositionId The UUID of the automation composition definition to delete
     * @return the automation composition definition
     */
    @Transactional(readOnly = true)
    public Optional<AutomationCompositionDefinition> findAcDefinition(UUID compositionId) {
        var jpaGet = acmDefinitionRepository.findById(compositionId.toString());
        return jpaGet.stream().map(JpaAutomationCompositionDefinition::toAuthorative).findFirst();
    }

    /**
     * Get Automation Composition Definitions in transition.
     *
     * @return the Automation Composition Definitions found
     */
    @Transactional(readOnly = true)
    public Set<UUID> getAllAcDefinitionsInTransition() {
        var jpaList = acmDefinitionRepository.findByStateIn(List.of(AcTypeState.PRIMING, AcTypeState.DEPRIMING));
        return jpaList.stream().map(JpaAutomationCompositionDefinition::getCompositionId)
                .map(UUID::fromString).collect(Collectors.toSet());
    }

    /**
     * Get service templates.
     *
     * @param name the name of the topology template to get, set to null to get all service templates
     * @param version the version of the service template to get, set to null to get all service templates
     * @param pageable the Pageable
     * @return the topology templates found
     */
    @Transactional(readOnly = true)
    public List<ToscaServiceTemplate> getServiceTemplateList(final String name, final String version,
            @NonNull Pageable pageable) {
        List<JpaAutomationCompositionDefinition> jpaList = null;
        if (name != null || version != null) {
            var entity = new JpaAutomationCompositionDefinition();
            entity.setName(name);
            entity.setVersion(version);
            var example = Example.of(entity);
            jpaList = acmDefinitionRepository.findAll(example, pageable).toList();
        } else {
            jpaList = acmDefinitionRepository.findAll(pageable).toList();
        }

        return jpaList.stream().map(JpaAutomationCompositionDefinition::getServiceTemplate)
                .map(DocToscaServiceTemplate::toAuthorative).toList();
    }
}
