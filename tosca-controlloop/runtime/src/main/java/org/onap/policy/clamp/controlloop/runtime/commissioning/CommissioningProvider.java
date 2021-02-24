/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.commissioning;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;

/**
 * This class provides the create, read and delete actions on Commissioning of Control Loop concepts in the database to
 * the callers.
 */
public class CommissioningProvider implements Closeable {
    public static final String CONTROL_LOOP_NODE_TYPE = "org.onap.policy.clamp.controlloop.ControlLoop";

    private final PolicyModelsProvider modelsProvider;
    private final ControlLoopProvider clProvider;

    private static final Object lockit = new Object();

    /**
     * Create a commissioning provider.
     *
     * @throws ControlLoopRuntimeException on errors creating the provider
     */
    public CommissioningProvider(PolicyModelsProviderParameters databaseProviderParameters)
            throws ControlLoopRuntimeException {
        try {
            modelsProvider = new PolicyModelsProviderFactory()
                    .createPolicyModelsProvider(databaseProviderParameters);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }

        try {
            clProvider = new ControlLoopProvider(databaseProviderParameters);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            modelsProvider.close();
        } catch (PfModelException e) {
            throw new IOException("error closing modelsProvider", e);
        }
    }

    /**
     * Create control loops from a service template.
     *
     * @param serviceTemplate the service template
     * @return the result of the commissioning operation
     * @throws PfModelException on creation errors
     */
    public CommissioningResponse createControlLoopDefinitions(ToscaServiceTemplate serviceTemplate)
            throws PfModelException {
        synchronized (lockit) {
            modelsProvider.createServiceTemplate(serviceTemplate);
        }

        CommissioningResponse response = new CommissioningResponse();
        // @formatter:off
        response.setAffectedControlLoopDefinitions(serviceTemplate.getToscaTopologyTemplate().getNodeTemplates()
                .values()
                .stream()
                .map(template -> template.getKey().asIdentifier())
                .collect(Collectors.toList()));
        // @formatter:on

        return response;
    }

    /**
     * Delete the control loop definition with the given name and version.
     *
     * @param name the name of the control loop definition to delete
     * @param version the version of the control loop to delete
     * @return the result of the deletion
     * @throws PfModelException on deletion errors
     */
    public CommissioningResponse deleteControlLoopDefinition(String name, String version) throws PfModelException {
        synchronized (lockit) {
            modelsProvider.deleteServiceTemplate(name, version);
        }

        CommissioningResponse response = new CommissioningResponse();
        response.setAffectedControlLoopDefinitions(
                Collections.singletonList(new ToscaConceptIdentifier(name, version)));

        return response;
    }

    /**
     * Get control loop node templates.
     *
     * @param clName the name of the control loop, null for all
     * @param clVersion the version of the control loop, null for all
     * @return list of control loop node templates
     * @throws PfModelException on errors getting control loop definitions
     */
    public List<ToscaNodeTemplate> getControlLoopDefinitions(String clName, String clVersion) throws PfModelException {

        // @formatter:off
        ToscaTypedEntityFilter<ToscaNodeTemplate> nodeTemplateFilter = ToscaTypedEntityFilter
                .<ToscaNodeTemplate>builder()
                .name(clName)
                .version(clVersion)
                .type(CONTROL_LOOP_NODE_TYPE)
                .build();
        // @formatter:on

        return clProvider.getFilteredNodeTemplates(nodeTemplateFilter);
    }

    /**
     * Get the control loop elements from a control loop node template.
     *
     * @param controlLoopNodeTemplate the control loop node template
     * @return a list of the control loop element node templates in a control loop node template
     * @throws PfModelException on errors get control loop element node templates
     */
    public List<ToscaNodeTemplate> getControlLoopElementDefinitions(ToscaNodeTemplate controlLoopNodeTemplate)
            throws PfModelException {
        if (!CONTROL_LOOP_NODE_TYPE.equals(controlLoopNodeTemplate.getType())) {
            return Collections.emptyList();
        }

        if (MapUtils.isEmpty(controlLoopNodeTemplate.getProperties())) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> controlLoopElements =
                (List<Map<String, String>>) controlLoopNodeTemplate.getProperties().get("elements");

        if (CollectionUtils.isEmpty(controlLoopElements)) {
            return Collections.emptyList();
        }

        List<ToscaNodeTemplate> controlLoopElementList = new ArrayList<>();
        // @formatter:off
        controlLoopElementList.addAll(
                controlLoopElements
                        .stream()
                        .map(elementMap -> clProvider.getNodeTemplates(elementMap.get("name"),
                                elementMap.get("version")))
                        .flatMap(List::stream)
                        .collect(Collectors.toList())
        );
        // @formatter:on

        return controlLoopElementList;
    }
}
