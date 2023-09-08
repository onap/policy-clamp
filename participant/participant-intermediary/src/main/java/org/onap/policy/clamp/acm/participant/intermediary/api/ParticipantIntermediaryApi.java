/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
     * Update the state of a automation composition element.
     *
     * @param automationCompositionId the ID of the automation composition to update the state on
     * @param elementId the ID of the automation composition element to update the state on
     * @param deployState the Deploy State of the automation composition element
     * @param lockState the Lock State of the automation composition element
     * @param stateChangeResult the indicator if error occurs
     * @param message the message
     */
    void updateAutomationCompositionElementState(UUID automationCompositionId, UUID elementId, DeployState deployState,
            LockState lockState, StateChangeResult stateChangeResult, String message);

    /**
     * Get a copy of all AutomationCompositions.
     *
     * @return get all AutomationCompositions
     */
    Map<UUID, AutomationComposition> getAutomationCompositions();

    /**
     * Get a copy of the AutomationComposition by automationCompositionId.
     *
     * @param automationCompositionId the ID of the automation composition to update the state on
     * @return get the AutomationComposition
     */
    AutomationComposition getAutomationComposition(UUID automationCompositionId);

    /**
     * Get a copy of the AutomationCompositionElement by automationCompositionId and elementId.
     *
     * @param automationCompositionId the ID of the automation composition to update the state on
     * @param elementId the ID of the automation composition element to update the state on
     * @return get the AutomationCompositionElement
     */
    AutomationCompositionElement getAutomationCompositionElement(UUID automationCompositionId, UUID elementId);

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
     * Send Automation Composition Element update Info to AC-runtime.
     *
     * @param automationCompositionId the ID of the automation composition to update the states
     * @param elementId the ID of the automation composition element to update the states
     * @param useState the use State
     * @param operationalState the operational State
     * @param outProperties the output Properties Map
     */
    void sendAcElementInfo(UUID automationCompositionId, UUID elementId, String useState, String operationalState,
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
