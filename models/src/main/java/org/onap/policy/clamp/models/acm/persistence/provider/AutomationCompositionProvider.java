/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationComposition;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRepository;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
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

    /**
     * Get automation composition.
     *
     * @param instanceId the ID of the automation composition to get
     * @return the automation composition found
     */
    @Transactional(readOnly = true)
    public AutomationComposition getAutomationComposition(final UUID instanceId) {
        var result = automationCompositionRepository.findByInstanceId(instanceId.toString());
        if (result.isEmpty()) {
            throw new PfModelRuntimeException(Status.NOT_FOUND, "AutomationComposition not found");
        }
        return result.get().toAuthorative();
    }

    /**
     * Get automation composition.
     *
     * @param automationCompositionId the ID of the automation composition to get
     * @return the automation composition found
     */
    @Transactional(readOnly = true)
    public AutomationComposition getAutomationComposition(final ToscaConceptIdentifier automationCompositionId) {
        try {
            return automationCompositionRepository.getById(automationCompositionId.asConceptKey()).toAuthorative();
        } catch (EntityNotFoundException e) {
            throw new PfModelRuntimeException(Status.NOT_FOUND, "AutomationComposition not found", e);
        }
    }

    /**
     * Find automation composition.
     *
     * @param instanceId the ID of the automation composition to get
     * @return the automation composition found
     */
    @Transactional(readOnly = true)
    public Optional<AutomationComposition> findAutomationComposition(final UUID instanceId) {
        var result = automationCompositionRepository.findByInstanceId(instanceId.toString());
        return result.stream().map(JpaAutomationComposition::toAuthorative).findFirst();
    }

    /**
     * Find automation composition by automationCompositionId.
     *
     * @param automationCompositionId the ID of the automation composition to get
     * @return the automation composition found
     */
    @Transactional(readOnly = true)
    public Optional<AutomationComposition> findAutomationComposition(
            final ToscaConceptIdentifier automationCompositionId) {
        return findAutomationComposition(automationCompositionId.asConceptKey());
    }

    private Optional<AutomationComposition> findAutomationComposition(@NonNull final PfConceptKey key) {
        return automationCompositionRepository.findById(key).map(JpaAutomationComposition::toAuthorative);
    }

    /**
     * Create automation composition.
     *
     * @param automationComposition the automation composition to create
     * @return the create automation composition
     */
    public AutomationComposition createAutomationComposition(final AutomationComposition automationComposition) {
        automationComposition.setInstanceId(UUID.randomUUID());
        var result = automationCompositionRepository.save(ProviderUtils.getJpaAndValidate(automationComposition,
                JpaAutomationComposition::new, "automation composition"));

        // Return the saved automation composition
        return result.toAuthorative();
    }

    /**
     * Update automation composition.
     *
     * @param automationComposition the automation composition to update
     * @return the updated automation composition
     */
    public AutomationComposition updateAutomationComposition(final AutomationComposition automationComposition) {
        var result = automationCompositionRepository.save(ProviderUtils.getJpaAndValidate(automationComposition,
                JpaAutomationComposition::new, "automation composition"));

        // Return the saved automation composition
        return result.toAuthorative();
    }

    /**
     * Get all automation compositions by compositionId.
     *
     * @param compositionId the compositionId of the automation composition definition
     * @return all automation compositions found
     */
    @Transactional(readOnly = true)
    public List<AutomationComposition> getAcInstancesByCompositionId(UUID compositionId) {
        return ProviderUtils
                .asEntityList(automationCompositionRepository.findByCompositionId(compositionId.toString()));
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

        return ProviderUtils.asEntityList(
                automationCompositionRepository.getFiltered(JpaAutomationComposition.class, name, version));
    }

    /**
     * Delete a automation composition.
     *
     * @param instanceId the ID of the automation composition to get
     * @return the automation composition deleted
     */
    public AutomationComposition deleteAutomationComposition(@NonNull final UUID instanceId) {
        var jpaDeleteAutomationComposition = automationCompositionRepository.findByInstanceId(instanceId.toString());
        if (jpaDeleteAutomationComposition.isEmpty()) {
            var errorMessage = "delete of automation composition \"" + instanceId
                    + "\" failed, automation composition does not exist";
            throw new PfModelRuntimeException(Response.Status.NOT_FOUND, errorMessage);
        }

        automationCompositionRepository.deleteById(jpaDeleteAutomationComposition.get().getKey());

        return jpaDeleteAutomationComposition.get().toAuthorative();
    }
}
