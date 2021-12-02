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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityNotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.repository.ControlLoopRepository;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.repository.ToscaNodeTemplateRepository;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.repository.ToscaNodeTemplatesRepository;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaNodeTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaNodeTemplates;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides information on control loop concepts in the database to callers.
 */
@Component
@Transactional
@AllArgsConstructor
public class ControlLoopProvider {

    private final ControlLoopRepository controlLoopRepository;
    private final ToscaNodeTemplateRepository toscaNodeTemplateRepository;
    private final ToscaNodeTemplatesRepository toscaNodeTemplatesRepository;

    /**
     * Get Control Loop.
     *
     * @param controlLoopId the ID of the control loop to get
     * @return the control loop found
     * @throws PfModelException on errors getting the control loop
     */
    @Transactional(readOnly = true)
    public ControlLoop getControlLoop(final ToscaConceptIdentifier controlLoopId) throws PfModelException {
        try {
            return controlLoopRepository.getById(controlLoopId.asConceptKey()).toAuthorative();
        } catch (EntityNotFoundException e) {
            throw new PfModelException(Status.NOT_FOUND, "ControlLoop not found", e);
        }
    }

    /**
     * Find Control Loop by controlLoopId.
     *
     * @param name the name of the control loop to get, null to get all control loops
     * @param version the version of the control loop to get, null to get all control loops
     * @return the control loop found
     * @throws PfModelException on errors getting the control loop
     */
    @Transactional(readOnly = true)
    public Optional<ControlLoop> findControlLoop(@NonNull final String name, @NonNull final String version)
            throws PfModelException {
        return findControlLoop(new PfConceptKey(name, version));
    }

    /**
     * Find Control Loop by controlLoopId.
     *
     * @param controlLoopId the ID of the control loop to get
     * @return the control loop found
     * @throws PfModelException on errors getting the control loop
     */
    @Transactional(readOnly = true)
    public Optional<ControlLoop> findControlLoop(final ToscaConceptIdentifier controlLoopId) throws PfModelException {
        return findControlLoop(controlLoopId.asConceptKey());
    }

    private Optional<ControlLoop> findControlLoop(@NonNull final PfConceptKey key) throws PfModelException {
        try {
            return controlLoopRepository.findById(key).map(JpaControlLoop::toAuthorative);
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Not valid parameter", e);
        }
    }

    /**
     * Save Control Loop.
     *
     * @param controlLoop the control loop to update
     * @return the updated control loop
     * @throws PfModelException on errors updating the control loop
     */
    public ControlLoop saveControlLoop(final ControlLoop controlLoop) throws PfModelException {
        try {
            var result = controlLoopRepository
                    .save(ProviderUtils.getJpaAndValidate(controlLoop, JpaControlLoop::new, "control loop"));

            // Return the saved participant
            return result.toAuthorative();
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error in save controlLoop", e);
        }
    }

    /**
     * Get All Control Loops.
     *
     * @return all control loops found
     * @throws PfModelException on errors getting control loops
     */
    @Transactional(readOnly = true)
    public List<ControlLoop> getControlLoops() throws PfModelException {

        return ProviderUtils.asEntityList(controlLoopRepository.findAll());
    }

    /**
     * Get Control Loops.
     *
     * @param name the name of the control loop to get, null to get all control loops
     * @param version the version of the control loop to get, null to get all control loops
     * @return the control loops found
     * @throws PfModelException on errors getting control loops
     */
    @Transactional(readOnly = true)
    public List<ControlLoop> getControlLoops(final String name, final String version) throws PfModelException {

        return ProviderUtils.asEntityList(controlLoopRepository.getFiltered(JpaControlLoop.class, name, version));
    }

    /**
     * Saves control loops.
     *
     * @param controlLoops a specification of the control loops to create
     * @return the control loops created
     * @throws PfModelException on errors creating control loops
     */
    public List<ControlLoop> saveControlLoops(@NonNull final List<ControlLoop> controlLoops) throws PfModelException {
        try {
            var result = controlLoopRepository
                    .saveAll(ProviderUtils.getJpaAndValidateList(controlLoops, JpaControlLoop::new, "control loops"));

            // Return the saved participant
            return ProviderUtils.asEntityList(result);
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error in save ControlLoops", e);
        }
    }

    /**
     * Saves Instance Properties to the database.
     *
     * @param serviceTemplate the service template
     * @return a Map of tosca node templates
     */
    public Map<String, ToscaNodeTemplate> saveInstanceProperties(ToscaServiceTemplate serviceTemplate) {
        Map<String, ToscaNodeTemplate> savedNodeTemplates = new HashMap<>();

        var jpaToscaNodeTemplates = new JpaToscaNodeTemplates();
        jpaToscaNodeTemplates.fromAuthorative(List.of(serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()));

        toscaNodeTemplatesRepository.save(jpaToscaNodeTemplates);
        serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().forEach(savedNodeTemplates::put);

        return savedNodeTemplates;
    }

    /**
     * Delete a control loop.
     *
     * @param name the name of the control loop to delete
     * @param version the version of the control loop to delete
     * @return the control loop deleted
     * @throws PfModelException on errors deleting the control loop
     */
    public ControlLoop deleteControlLoop(@NonNull final String name, @NonNull final String version)
            throws PfModelException {

        var controlLoopKey = new PfConceptKey(name, version);
        var jpaDeleteControlLoop = controlLoopRepository.findById(controlLoopKey);

        if (jpaDeleteControlLoop.isEmpty()) {
            String errorMessage =
                    "delete of control loop \"" + controlLoopKey.getId() + "\" failed, control loop does not exist";
            throw new PfModelException(Response.Status.BAD_REQUEST, errorMessage);
        }

        controlLoopRepository.deleteById(controlLoopKey);

        return jpaDeleteControlLoop.get().toAuthorative();
    }

    /**
     * Deletes Instance Properties on the database.
     *
     * @param filteredToscaNodeTemplateMap filtered node templates map to delete
     * @param filteredToscaNodeTemplateList filtered node template list to delete
     */
    public void deleteInstanceProperties(Map<String, ToscaNodeTemplate> filteredToscaNodeTemplateMap,
            List<ToscaNodeTemplate> filteredToscaNodeTemplateList) {

        var jpaToscaNodeTemplates = new JpaToscaNodeTemplates();
        jpaToscaNodeTemplates.fromAuthorative(List.of(filteredToscaNodeTemplateMap));

        toscaNodeTemplatesRepository.save(jpaToscaNodeTemplates);

        filteredToscaNodeTemplateList.forEach(template -> {
            var jpaToscaNodeTemplate = new JpaToscaNodeTemplate(template);

            toscaNodeTemplateRepository.delete(jpaToscaNodeTemplate);
        });
    }

    /**
     * Get All Node Templates.
     *
     * @return the list of node templates found
     * @throws PfModelException on errors getting node templates
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
