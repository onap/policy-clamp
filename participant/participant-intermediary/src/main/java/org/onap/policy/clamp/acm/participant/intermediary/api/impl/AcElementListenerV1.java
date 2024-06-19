/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;

/**
 * Wrapper of AutomationCompositionElementListener.
 * Valid since 7.1.0 release.
 */
public abstract class AcElementListenerV1
        implements AutomationCompositionElementListener, AutomationCompositionElementListenerV1 {
    protected final ParticipantIntermediaryApi intermediaryApi;

    protected AcElementListenerV1(ParticipantIntermediaryApi intermediaryApi) {
        this.intermediaryApi = intermediaryApi;
    }

    @Override
    public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        var element = new AcElementDeploy();
        element.setId(instanceElement.elementId());
        element.setDefinition(compositionElement.elementDefinitionId());
        element.setToscaServiceTemplateFragment(instanceElement.toscaServiceTemplateFragment());
        element.setProperties(instanceElement.inProperties());
        Map<String, Object> properties = new HashMap<>(instanceElement.inProperties());
        properties.putAll(compositionElement.inProperties());
        deploy(instanceElement.instanceId(), element, properties);
    }

    @Override
    public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        undeploy(instanceElement.instanceId(), instanceElement.elementId());
    }

    @Override
    public void lock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        lock(instanceElement.instanceId(), instanceElement.elementId());
    }

    public void lock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
            StateChangeResult.NO_ERROR, "Locked");
    }

    @Override
    public void unlock(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        unlock(instanceElement.instanceId(), instanceElement.elementId());
    }

    public void unlock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
            StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Override
    public void delete(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
        throws PfModelException {
        delete(instanceElement.instanceId(), instanceElement.elementId());
    }

    public void delete(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, DeployState.DELETED, null,
            StateChangeResult.NO_ERROR, "Deleted");
    }

    @Override
    public void update(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
                       InstanceElementDto instanceElementUpdated) throws PfModelException {
        var element = new  AcElementDeploy();
        element.setId(instanceElementUpdated.elementId());
        element.setDefinition(compositionElement.elementDefinitionId());
        element.setProperties(instanceElementUpdated.inProperties());
        update(instanceElementUpdated.instanceId(), element, element.getProperties());
    }

    public void update(UUID instanceId, AcElementDeploy element, Map<String, Object> properties)
        throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(), DeployState.DEPLOYED, null,
            StateChangeResult.NO_ERROR, "Update not supported");
    }

    private List<AutomationCompositionElementDefinition> createAcElementDefinitionList(CompositionDto composition) {
        List<AutomationCompositionElementDefinition> elementDefinitionList = new ArrayList<>();
        for (var entry : composition.inPropertiesMap().entrySet()) {
            elementDefinitionList.add(createAcElementDefinition(entry.getKey(), entry.getValue(),
                composition.outPropertiesMap().get(entry.getKey())));
        }
        return elementDefinitionList;
    }

    private AutomationCompositionElementDefinition createAcElementDefinition(
        ToscaConceptIdentifier toscaConceptIdentifier, Map<String, Object> property,
        Map<String, Object> outProperties) {
        var acElementDefinition = new AutomationCompositionElementDefinition();
        acElementDefinition.setAcElementDefinitionId(toscaConceptIdentifier);
        var toscaNodeTemplate = new ToscaNodeTemplate();
        toscaNodeTemplate.setProperties(property);
        acElementDefinition.setAutomationCompositionElementToscaNodeTemplate(toscaNodeTemplate);
        acElementDefinition.setOutProperties(outProperties);
        return acElementDefinition;
    }

    @Override
    public void prime(CompositionDto composition) throws PfModelException {
        prime(composition.compositionId(), createAcElementDefinitionList(composition));
    }

    public void prime(UUID compositionId, List<AutomationCompositionElementDefinition> elementDefinitionList)
        throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "Primed");
    }

    @Override
    public void deprime(CompositionDto composition) throws PfModelException {
        deprime(composition.compositionId());
    }

    public void deprime(UUID compositionId) throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR,
            "Deprimed");
    }

    public void handleRestartComposition(CompositionDto composition, AcTypeState state) throws PfModelException {
        throw new PfModelException(Response.Status.BAD_REQUEST, "not supported!");
    }

    /**
     * Default implementation of handle Restart Composition.
     *
     * @param compositionId the composition Id
     * @param elementDefinitionList the list of AutomationCompositionElementDefinition
     * @param state the current AcTypeState
     * @throws PfModelException in case of a model exception
     */
    public void handleRestartComposition(UUID compositionId,
        List<AutomationCompositionElementDefinition> elementDefinitionList, AcTypeState state) throws PfModelException {
        throw new PfModelException(Response.Status.BAD_REQUEST, "not supported!");
    }

    public void handleRestartInstance(CompositionElementDto compositionElement, InstanceElementDto instanceElement,
        DeployState deployState, LockState lockState) throws PfModelException {
        throw new PfModelException(Response.Status.BAD_REQUEST, "not supported!");
    }

    /**
     * Default implementation of handle Restart Instance.
     *
     * @param instanceId the instance Id
     * @param element the AcElementDeploy
     * @param properties the in properties
     * @param deployState the current deployState
     * @param lockState the current lockState
     * @throws PfModelException in case of a model exception
     */
    public void handleRestartInstance(UUID instanceId, AcElementDeploy element,
        Map<String, Object> properties, DeployState deployState, LockState lockState) throws PfModelException {
        throw new PfModelException(Response.Status.BAD_REQUEST, "not supported!");

    }

    @Override
    public void migrate(CompositionElementDto compositionElement, CompositionElementDto compositionElementTarget,
        InstanceElementDto instanceElement, InstanceElementDto instanceElementMigrate) throws PfModelException {
        var element = new  AcElementDeploy();
        element.setId(instanceElementMigrate.elementId());
        element.setDefinition(compositionElementTarget.elementDefinitionId());
        element.setProperties(instanceElementMigrate.inProperties());
        migrate(instanceElementMigrate.instanceId(), element, compositionElementTarget.compositionId(),
            element.getProperties());
    }

    @Override
    public void migrate(UUID instanceId, AcElementDeploy element, UUID compositionTargetId,
                        Map<String, Object> properties) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(),
            DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Migrated");
    }
}
