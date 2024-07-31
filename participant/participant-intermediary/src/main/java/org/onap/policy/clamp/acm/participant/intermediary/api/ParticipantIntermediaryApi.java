/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.api;

import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * This interface is used by participant implementations to use the participant intermediary.
 */
public interface ParticipantIntermediaryApi {

    /**
     * Update the state of a AutomationComposition Instance Element.
     *
     * @param instance the ID of the AutomationComposition Instance to update the state on
     * @param elementId the ID of the AutomationComposition Instance element to update the state on
     * @param deployState the Deploy State of the AutomationComposition Instance element
     * @param lockState the Lock State of the AutomationComposition Instance element
     * @param stateChangeResult the indicator if error occurs
     * @param message the message
     */
    void updateAutomationCompositionElementState(UUID instance, UUID elementId, DeployState deployState,
            LockState lockState, StateChangeResult stateChangeResult, String message);

    /**
     * Update the stage of a AutomationComposition Instance Element.
     *
     * @param instance the ID of the AutomationComposition Instance to update the state on
     * @param elementId the ID of the AutomationComposition Instance Element to update the state on
     * @param stateChangeResult the indicator if error occurs
     * @param message the message
     */
    void updateAutomationCompositionElementStage(UUID instance, UUID elementId, StateChangeResult stateChangeResult,
            int stage, String message);

    /**
     * Get a copy of all AutomationComposition Instances.
     *
     * @return get all AutomationComposition Instances
     */
    Map<UUID, AutomationComposition> getAutomationCompositions();

    /**
     * Get a copy of the AutomationComposition Instance by AutomationComposition Instance Id.
     *
     * @param instanceId the ID of the AutomationComposition Instance to update the state on
     * @return get the AutomationComposition Instance
     */
    AutomationComposition getAutomationComposition(UUID instanceId);

    /**
     * Get a copy of the AutomationCompositionElement by AutomationComposition Instance Id and elementId.
     *
     * @param instanceId the ID of the AutomationComposition Instance to update the state on
     * @param elementId the ID of the AutomationComposition Instance Element to update the state on
     * @return get the AutomationCompositionElement
     */
    AutomationCompositionElement getAutomationCompositionElement(UUID instanceId, UUID elementId);

    /**
     * Get a copy of all AutomationCompositionElementDefinition from all primed compositions.
     *
     * @return a Map by compositionId of Maps of AutomationCompositionElement
     */
    Map<UUID, Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition>> getAcElementsDefinitions();

    /**
     * Get a copy of AutomationCompositionElementDefinitions of a composition.
     *
     * @param compositionId the composition id
     * @return a Map by element definition Id of AutomationCompositionElementDefinitions
     */
    Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> getAcElementsDefinitions(UUID compositionId);

    /**
     * Get a copy of the AutomationCompositionElementDefinition by compositionId and element definition Id.
     *
     * @param compositionId the composition id
     * @param elementId the element definition Id
     * @return the AutomationCompositionElementDefinition
     */
    AutomationCompositionElementDefinition getAcElementDefinition(UUID compositionId, ToscaConceptIdentifier elementId);

    /**
     * Send AutomationComposition Instance Element update Info to AC-runtime.
     *
     * @param instanceId the ID of the AutomationComposition Instance to update the states
     * @param elementId the ID of the AutomationComposition Instance Element to update the states
     * @param useState the use State
     * @param operationalState the operational State
     * @param outProperties the output Properties Map
     */
    void sendAcElementInfo(UUID instanceId, UUID elementId, String useState, String operationalState,
            Map<String, Object> outProperties);

    /**
     * Send Automation Composition Definition update Info to AC-runtime.
     *
     * @param compositionId the composition id
     * @param elementId the element definition Id
     * @param outProperties the output Properties Map
     */
    void sendAcDefinitionInfo(UUID compositionId, ToscaConceptIdentifier elementId, Map<String, Object> outProperties);

    /**
     * Update the state of a Automation Composition Definition.
     *
     * @param compositionId the composition id
     * @param state the state of Automation Composition Definition
     * @param stateChangeResult the indicator if error occurs
     * @param message the message
     */
    void updateCompositionState(UUID compositionId, AcTypeState state, StateChangeResult stateChangeResult,
            String message);
}
