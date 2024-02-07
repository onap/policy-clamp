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

package org.onap.policy.clamp.acm.participant.intermediary.api.impl;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.handler.AutomationCompositionOutHandler;
import org.onap.policy.clamp.acm.participant.intermediary.handler.CacheProvider;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Component;

/**
 * This class is api implementation used by participant intermediary.
 */
@Component
@RequiredArgsConstructor
public class ParticipantIntermediaryApiImpl implements ParticipantIntermediaryApi {

    // The handler for the automationComposition intermediary
    private final AutomationCompositionOutHandler automationCompositionHandler;
    private final CacheProvider cacheProvider;

    @Override
    public void updateAutomationCompositionElementState(UUID automationCompositionId, UUID id, DeployState newState,
            LockState lockState, StateChangeResult stateChangeResult, String message) {
        automationCompositionHandler.updateAutomationCompositionElementState(automationCompositionId, id, newState,
                lockState, stateChangeResult, message);
    }

    @Override
    public void sendAcElementInfo(UUID automationCompositionId, UUID elementId, String useState,
            String operationalState, Map<String, Object> outProperties) {
        automationCompositionHandler.sendAcElementInfo(automationCompositionId, elementId, useState, operationalState,
                outProperties);
    }

    @Override
    public Map<UUID, AutomationComposition> getAutomationCompositions() {
        return PfUtils.mapMap(cacheProvider.getAutomationCompositions(), AutomationComposition::new);
    }

    @Override
    public void updateCompositionState(UUID compositionId, AcTypeState state, StateChangeResult stateChangeResult,
            String message) {
        automationCompositionHandler.updateCompositionState(compositionId, state, stateChangeResult, message);
    }

    @Override
    public AutomationCompositionElement getAutomationCompositionElement(UUID automationCompositionId, UUID elementId) {
        var automationComposition = cacheProvider.getAutomationCompositions().get(automationCompositionId);
        if (automationComposition == null) {
            return null;
        }
        var element = automationComposition.getElements().get(elementId);
        return element != null ? new AutomationCompositionElement(element) : null;
    }

    @Override
    public void sendAcDefinitionInfo(UUID compositionId, ToscaConceptIdentifier elementId,
            Map<String, Object> outProperties) {
        automationCompositionHandler.sendAcDefinitionInfo(compositionId, elementId, outProperties);
    }

    @Override
    public AutomationComposition getAutomationComposition(UUID automationCompositionId) {
        var automationComposition = cacheProvider.getAutomationCompositions().get(automationCompositionId);
        return automationComposition != null ? new AutomationComposition(automationComposition) : null;
    }

    @Override
    public Map<UUID, Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition>> getAcElementsDefinitions() {
        return PfUtils.mapMap(cacheProvider.getAcElementsDefinitions(),
                map -> PfUtils.mapMap(map, AutomationCompositionElementDefinition::new));
    }

    @Override
    public Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> getAcElementsDefinitions(
            UUID compositionId) {
        var acElementDefinitions = cacheProvider.getAcElementsDefinitions().get(compositionId);
        if (acElementDefinitions == null) {
            return null;
        }
        return PfUtils.mapMap(acElementDefinitions, AutomationCompositionElementDefinition::new);
    }

    @Override
    public AutomationCompositionElementDefinition getAcElementDefinition(UUID compositionId,
            ToscaConceptIdentifier elementId) {
        var acElementDefinitions = cacheProvider.getAcElementsDefinitions().get(compositionId);
        if (acElementDefinitions == null) {
            return null;
        }
        var acElementDefinition = acElementDefinitions.get(elementId);
        return acElementDefinition != null ? new AutomationCompositionElementDefinition(acElementDefinition) : null;
    }
}
