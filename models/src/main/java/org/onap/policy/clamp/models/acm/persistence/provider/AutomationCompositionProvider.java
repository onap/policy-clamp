/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
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

package org.onap.policy.clamp.models.acm.persistence.provider;

import java.util.List;
import java.util.Optional;
import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRepository;
import org.onap.policy.clamp.models.acm.persistence.repository.ToscaNodeTemplateRepository;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaNodeTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides information on automation composition concepts in the database to callers.
 */
@Service
@Transactional
@AllArgsConstructor
public class AutomationCompositionProvider {

    private final AutomationCompositionRepository automationCompositionRepository;
    private final ToscaNodeTemplateRepository toscaNodeTemplateRepository;

    /**
     * Get automation composition.
     *
     * @param automationCompositionId the ID of the automation composition to get
     * @return the automation composition found
     * @throws PfModelException on errors getting the automation composition
     */
    @Transactional(readOnly = true)
    public AutomationComposition getAutomationComposition(final ToscaConceptIdentifier automationCompositionId)
        throws PfModelException {
        try {
            return automationCompositionRepository.getById(automationCompositionId.asConceptKey()).toAuthorative();
        } catch (EntityNotFoundException e) {
            throw new PfModelException(Status.NOT_FOUND, "AutomationComposition not found", e);
        }
    }

    /**
     * Find automation composition by automationCompositionId.
     *
     * @param name the name of the automation composition to get, null to get all automation compositions
     * @param version the version of the automation composition to get, null to get all automation compositions
     * @return the automation composition found
     * @throws PfModelException on errors getting the automation composition
     */
    @Transactional(readOnly = true)
    public Optional<AutomationComposition> findAutomationComposition(@NonNull final String name,
        @NonNull final String version) throws PfModelException {
        return findAutomationComposition(new PfConceptKey(name, version));
    }

    /**
     * Find automation composition by automationCompositionId.
     *
     * @param automationCompositionId the ID of the automation composition to get
     * @return the automation composition found
     * @throws PfModelException on errors getting the automation composition
     */
    @Transactional(readOnly = true)
    public Optional<AutomationComposition> findAutomationComposition(
        final ToscaConceptIdentifier automationCompositionId) throws PfModelException {
        return findAutomationComposition(automationCompositionId.asConceptKey());
    }

    private Optional<AutomationComposition> findAutomationComposition(@NonNull final PfConceptKey key)
        throws PfModelException {
        try {
            return automationCompositionRepository.findById(key).map(JpaAutomationComposition::toAuthorative);
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Not valid parameter", e);
        }
    }

    /**
     * Save automation composition.
     *
     * @param automationComposition the automation composition to update
     * @return the updated automation composition
     * @throws PfModelException on errors updating the automation composition
     */
    public AutomationComposition saveAutomationComposition(final AutomationComposition automationComposition)
        throws PfModelException {
        try {
            var result = automationCompositionRepository.save(ProviderUtils.getJpaAndValidate(automationComposition,
                JpaAutomationComposition::new, "automation composition"));

            // Return the saved participant
            return result.toAuthorative();
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error in save automationComposition", e);
        }
    }

    /**
     * Get all automation compositions.
     *
     * @return all automation compositions found
     */
    @Transactional(readOnly = true)
    public List<AutomationComposition> getAutomationCompositions() {

        return ProviderUtils.asEntityList(automationCompositionRepository.findAll());
    }

    /**
     * Get automation compositions.
     *
     * @param name the name of the automation composition to get, null to get all automation compositions
     * @param version the version of the automation composition to get, null to get all automation compositions
     * @return the automation compositions found
     */
    @Transactional(readOnly = true)
    public List<AutomationComposition> getAutomationCompositions(final String name, final String version) {

        return ProviderUtils
            .asEntityList(automationCompositionRepository.getFiltered(JpaAutomationComposition.class, name, version));
    }

    /**
     * Saves automation compositions.
     *
     * @param automationCompositions a specification of the automation compositions to create
     * @return the automation compositions created
     * @throws PfModelException on errors creating automation compositions
     */
    public List<AutomationComposition> saveAutomationCompositions(
        @NonNull final List<AutomationComposition> automationCompositions) throws PfModelException {
        try {
            var result =
                automationCompositionRepository.saveAll(ProviderUtils.getJpaAndValidateList(automationCompositions,
                    JpaAutomationComposition::new, "automation compositions"));

            // Return the saved participant
            return ProviderUtils.asEntityList(result);
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error in save AutomationCompositions", e);
        }
    }

    /**
     * Delete a automation composition.
     *
     * @param name the name of the automation composition to delete
     * @param version the version of the automation composition to delete
     * @return the automation composition deleted
     * @throws PfModelException on errors deleting the automation composition
     */
    public AutomationComposition deleteAutomationComposition(@NonNull final String name, @NonNull final String version)
        throws PfModelException {

        var automationCompositionKey = new PfConceptKey(name, version);
        var jpaDeleteAutomationComposition = automationCompositionRepository.findById(automationCompositionKey);

        if (jpaDeleteAutomationComposition.isEmpty()) {
            String errorMessage = "delete of automation composition \"" + automationCompositionKey.getId()
                + "\" failed, automation composition does not exist";
            throw new PfModelException(Response.Status.BAD_REQUEST, errorMessage);
        }

        automationCompositionRepository.deleteById(automationCompositionKey);

        return jpaDeleteAutomationComposition.get().toAuthorative();
    }

    /**
     * Get All Node Templates.
     *
     * @return the list of node templates found
     */
    @Transactional(readOnly = true)
    public List<ToscaNodeTemplate> getAllNodeTemplates() {
        return ProviderUtils.asEntityList(toscaNodeTemplateRepository.findAll());
    }

    /**
     * Get Node Templates.
     *
     * @param name the name of the node template to get, null to get all node templates
     * @param version the version of the node template to get, null to get all node templates
     * @return the node templates found
     * @throws PfModelException on errors getting node templates
     */
    @Transactional(readOnly = true)
    public List<ToscaNodeTemplate> getNodeTemplates(final String name, final String version) {
        return ProviderUtils
            .asEntityList(toscaNodeTemplateRepository.getFiltered(JpaToscaNodeTemplate.class, name, version));
    }

    /**
     * Get filtered node templates.
     *
     * @param filter the filter for the node templates to get
     * @return the node templates found
     * @throws PfModelException on errors getting node templates
     */
    @Transactional(readOnly = true)
    public List<ToscaNodeTemplate> getFilteredNodeTemplates(
        @NonNull final ToscaTypedEntityFilter<ToscaNodeTemplate> filter) {

        return filter.filter(ProviderUtils.asEntityList(toscaNodeTemplateRepository
            .getFiltered(JpaToscaNodeTemplate.class, filter.getName(), filter.getVersion())));
    }
}
