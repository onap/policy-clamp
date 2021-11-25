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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import lombok.NonNull;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaControlLoop;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.provider.impl.AbstractModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaEntity;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaNodeTemplate;
import org.onap.policy.models.tosca.simple.concepts.JpaToscaNodeTemplates;
import org.springframework.stereotype.Component;

/**
 * This class provides information on control loop concepts in the database to callers.
 */
@Component
public class ControlLoopProvider extends AbstractModelsProvider {

    /**
     * Create a provider for control loops.
     *
     * @param parameters the parameters for database access
     * @throws PfModelException on initiation errors
     */
    public ControlLoopProvider(@NonNull PolicyModelsProviderParameters parameters) throws PfModelException {
        super(parameters);
        this.init();
    }

    /**
     * Get Control Loop.
     *
     * @param controlLoopId the ID of the control loop to get
     * @return the control loop found
     * @throws PfModelException on errors getting the control loop
     */
    public ControlLoop getControlLoop(final ToscaConceptIdentifier controlLoopId) throws PfModelException {
        var jpaControlLoop = getPfDao().get(JpaControlLoop.class, controlLoopId.asConceptKey());

        return jpaControlLoop == null ? null : jpaControlLoop.toAuthorative();
    }

    /**
     * Update Control Loop.
     *
     * @param controlLoop the control loop to update
     * @return the updated control loop
     * @throws PfModelException on errors updating the control loop
     */
    public ControlLoop updateControlLoop(final ControlLoop controlLoop) throws PfModelException {
        return updateControlLoops(Collections.singletonList(controlLoop)).get(0);
    }

    /**
     * Get Control Loops.
     *
     * @param name the name of the control loop to get, null to get all control loops
     * @param version the version of the control loop to get, null to get all control loops
     * @return the control loops found
     * @throws PfModelException on errors getting control loops
     */
    public List<ControlLoop> getControlLoops(final String name, final String version) throws PfModelException {

        return asEntityList(getPfDao().getFiltered(JpaControlLoop.class, name, version));
    }

    /**
     * Get filtered control loops.
     *
     * @param filter the filter for the control loops to get
     * @return the control loops found
     * @throws PfModelException on errors getting control loops
     */
    public List<ControlLoop> getFilteredControlLoops(@NonNull final ToscaTypedEntityFilter<ControlLoop> filter) {

        return filter.filter(
                asEntityList(getPfDao().getFiltered(JpaControlLoop.class, filter.getName(), PfKey.NULL_KEY_VERSION)));
    }

    /**
     * Creates control loops.
     *
     * @param controlLoops a specification of the control loops to create
     * @return the control loops created
     * @throws PfModelException on errors creating control loops
     */
    public List<ControlLoop> createControlLoops(@NonNull final List<ControlLoop> controlLoops) throws PfModelException {

        List<JpaControlLoop> jpaControlLoopList =
                ProviderUtils.getJpaAndValidateList(controlLoops, JpaControlLoop::new, "control loop");

        jpaControlLoopList.forEach(jpaControlLoop -> getPfDao().create(jpaControlLoop));

        // Return the created control loops
        List<ControlLoop> returnControlLoops = new ArrayList<>(controlLoops.size());

        for (ControlLoop controlLoop : controlLoops) {
            var jpaControlLoop = getPfDao().get(JpaControlLoop.class,
                    new PfConceptKey(controlLoop.getName(), controlLoop.getVersion()));
            returnControlLoops.add(jpaControlLoop.toAuthorative());
        }

        return returnControlLoops;
    }

    /**
     * Updates control loops.
     *
     * @param controlLoops a specification of the control loops to update
     * @return the control loops updated
     * @throws PfModelException on errors updating control loops
     */
    public List<ControlLoop> updateControlLoops(@NonNull final List<ControlLoop> controlLoops) throws PfModelException {

        List<JpaControlLoop> jpaControlLoopList =
                ProviderUtils.getJpaAndValidateList(controlLoops, JpaControlLoop::new, "control loop");

        // Return the created control loops
        List<ControlLoop> returnControlLoops = new ArrayList<>(controlLoops.size());

        jpaControlLoopList.forEach(jpaControlLoop -> {
            var returnJpaControlLoop = getPfDao().update(jpaControlLoop);
            returnControlLoops.add(returnJpaControlLoop.toAuthorative());
        });

        return returnControlLoops;
    }

    /**
     * Delete a control loop.
     *
     * @param name the name of the control loop to delete
     * @param version the version of the control loop to delete
     * @return the control loop deleted
     * @throws PfModelRuntimeException on errors deleting the control loop
     */
    public ControlLoop deleteControlLoop(@NonNull final String name, @NonNull final String version) {

        var controlLoopKey = new PfConceptKey(name, version);

        var jpaDeleteControlLoop = getPfDao().get(JpaControlLoop.class, controlLoopKey);

        if (jpaDeleteControlLoop == null) {
            String errorMessage =
                    "delete of control loop \"" + controlLoopKey.getId() + "\" failed, control loop does not exist";
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, errorMessage);
        }

        getPfDao().delete(jpaDeleteControlLoop);

        return jpaDeleteControlLoop.toAuthorative();
    }

    /**
     * Saves Instance Properties to the database.
     * @param serviceTemplate the service template
     * @return a Map of tosca node templates
     */
    public Map<String, ToscaNodeTemplate> saveInstanceProperties(ToscaServiceTemplate serviceTemplate) {

        Map<String, ToscaNodeTemplate> savedNodeTemplates = new HashMap<>();

        var jpaToscaNodeTemplates = new JpaToscaNodeTemplates();
        jpaToscaNodeTemplates.fromAuthorative(Collections.singletonList(serviceTemplate.getToscaTopologyTemplate()
            .getNodeTemplates()));

        getPfDao().create(jpaToscaNodeTemplates);
        serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().forEach(savedNodeTemplates::put);

        return savedNodeTemplates;
    }

    /**
     * Deletes Instance Properties on the database.
     *
     * @param filteredToscaNodeTemplateMap filtered node templates map to delete
     * @param filteredToscaNodeTemplateList filtered node template list to delete
     */
    public void deleteInstanceProperties(
        Map<String, ToscaNodeTemplate> filteredToscaNodeTemplateMap,
        List<ToscaNodeTemplate> filteredToscaNodeTemplateList) {

        var jpaToscaNodeTemplates = new JpaToscaNodeTemplates();
        jpaToscaNodeTemplates.fromAuthorative(Collections.singletonList(filteredToscaNodeTemplateMap));

        getPfDao().create(jpaToscaNodeTemplates);

        filteredToscaNodeTemplateList.forEach(template -> {
            var jpaToscaNodeTemplate = new JpaToscaNodeTemplate(template);

            getPfDao().delete(jpaToscaNodeTemplate);
        });
    }

    /**
     * Get Node Templates.
     *
     * @param name the name of the node template to get, null to get all node templates
     * @param version the version of the node template to get, null to get all node templates
     * @return the node templates found
     * @throws PfModelException on errors getting node templates
     */
    public List<ToscaNodeTemplate> getNodeTemplates(final String name, final String version) {
        return asEntityList(getPfDao().getFiltered(JpaToscaNodeTemplate.class, name, version));
    }

    /**
     * Get filtered node templates.
     *
     * @param filter the filter for the node templates to get
     * @return the node templates found
     * @throws PfModelException on errors getting node templates
     */
    public List<ToscaNodeTemplate> getFilteredNodeTemplates(
            @NonNull final ToscaTypedEntityFilter<ToscaNodeTemplate> filter) {

        return filter.filter(asEntityList(
                getPfDao().getFiltered(JpaToscaNodeTemplate.class, filter.getName(), filter.getVersion())));
    }

    /**
     * Convert JPA control loop list to an authorative control loop list.
     *
     * @param <T> the type of TOSCA entity
     * @param <J> the type of JPA TOSCA entity
     * @param jpaEntityList the list to convert
     * @return the authorative list
     */
    private <T extends ToscaEntity, J extends PfAuthorative<T>> List<T> asEntityList(List<J> jpaEntityList) {
        return jpaEntityList.stream().map(J::toAuthorative).collect(Collectors.toList());
    }

}
