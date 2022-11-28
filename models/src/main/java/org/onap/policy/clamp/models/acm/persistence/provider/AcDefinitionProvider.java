/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionDefinitionRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.ToscaServiceTemplateRepository;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaServiceTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AcDefinitionProvider {

    private final ToscaServiceTemplateRepository serviceTemplateRepository;
    private final AutomationCompositionDefinitionRepository acmDefinitionRepository;

    /**
     * Create Automation Composition Definition.
     *
     * @param serviceTemplate the service template to be created
     * @return the created ACM Definition
     */
    public AutomationCompositionDefinition createAutomationCompositionDefinition(
            final ToscaServiceTemplate serviceTemplate) {
        var acmDefinition = new AutomationCompositionDefinition();
        acmDefinition.setCompositionId(UUID.randomUUID());
        acmDefinition.setServiceTemplate(serviceTemplate);
        var jpaAcmDefinition = ProviderUtils.getJpaAndValidate(acmDefinition, JpaAutomationCompositionDefinition::new,
                "AutomationCompositionDefinition");
        var result = acmDefinitionRepository.save(jpaAcmDefinition);

        return result.toAuthorative();
    }

    /**
     * Update the ServiceTemplate.
     *
     * @param compositionId The UUID of the automation composition definition to delete
     * @param serviceTemplate the service template to be created
     */
    public void updateServiceTemplate(UUID compositionId, ToscaServiceTemplate serviceTemplate) {
        var jpaServiceTemplate =
                ProviderUtils.getJpaAndValidate(serviceTemplate, JpaToscaServiceTemplate::new, "toscaServiceTemplate");
        serviceTemplateRepository.save(jpaServiceTemplate);
    }

    /**
     * Delete Automation Composition Definition.
     *
     * @param compositionId The UUID of the automation composition definition to delete
     * @return the TOSCA service template that was deleted
     */
    public ToscaServiceTemplate deleteAcDefintion(UUID compositionId) {
        var jpaDelete = acmDefinitionRepository.findById(compositionId.toString());
        if (jpaDelete.isEmpty()) {
            String errorMessage = "delete of Automation Composition Definition \"" + compositionId
                    + "\" failed, Automation Composition Definition does not exist";
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, errorMessage);
        }

        var item = jpaDelete.get().getServiceTemplate();
        serviceTemplateRepository.deleteById(item.getKey());
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
    public ToscaServiceTemplate getAcDefinition(UUID compositionId) {
        var jpaGet = acmDefinitionRepository.findById(compositionId.toString());
        if (jpaGet.isEmpty()) {
            String errorMessage =
                    "Get serviceTemplate \"" + compositionId + "\" failed, serviceTemplate does not exist";
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, errorMessage);
        }
        return jpaGet.get().getServiceTemplate().toAuthorative();
    }

    /**
     * Get the requested automation composition definition.
     *
     * @param compositionId The UUID of the automation composition definition to delete
     * @return the automation composition definition
     */
    @Transactional(readOnly = true)
    public Optional<ToscaServiceTemplate> findAcDefinition(UUID compositionId) {
        var jpaGet = acmDefinitionRepository.findById(compositionId.toString());
        return jpaGet.stream().map(JpaAutomationCompositionDefinition::getServiceTemplate)
                .map(JpaToscaServiceTemplate::toAuthorative).findFirst();
    }

    /**
     * Get Automation Composition Definitions.
     *
     * @return the Automation Composition Definitions found
     */
    @Transactional(readOnly = true)
    public List<AutomationCompositionDefinition> getAllAcDefinitions() {
        var jpaList = acmDefinitionRepository.findAll();
        return ProviderUtils.asEntityList(jpaList);
    }

    /**
     * Get service templates.
     *
     * @param name the name of the topology template to get, set to null to get all service templates
     * @param version the version of the service template to get, set to null to get all service templates
     * @return the topology templates found
     */
    @Transactional(readOnly = true)
    public List<ToscaServiceTemplate> getServiceTemplateList(final String name, final String version) {
        var jpaList = serviceTemplateRepository.getFiltered(JpaToscaServiceTemplate.class, name, version);
        return ProviderUtils.asEntityList(jpaList);
    }
}
