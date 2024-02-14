/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.NonNull;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.clamp.models.acm.concepts.ParticipantRestartAc;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Component;

@Component
public class CacheProvider {

    @Getter
    private final UUID participantId;

    private final List<ParticipantSupportedElementType> supportedAcElementTypes;

    @Getter
    private final Map<UUID, AutomationComposition> automationCompositions = new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition>> acElementsDefinitions =
            new ConcurrentHashMap<>();

    @Getter
    private final Map<UUID, UUID> msgIdentification = new ConcurrentHashMap<>();

    /**
     * Constructor.
     *
     * @param parameters the parameters of the participant
     */
    public CacheProvider(ParticipantParameters parameters) {
        this.participantId = parameters.getIntermediaryParameters().getParticipantId();
        this.supportedAcElementTypes = parameters.getIntermediaryParameters().getParticipantSupportedElementTypes();
    }

    public List<ParticipantSupportedElementType> getSupportedAcElementTypes() {
        return PfUtils.mapList(supportedAcElementTypes, ParticipantSupportedElementType::new);
    }

    /**
     * Get AutomationComposition by id.
     *
     * @param automationCompositionId the AutomationComposition Id
     * @return the AutomationComposition
     */
    public AutomationComposition getAutomationComposition(@NonNull UUID automationCompositionId) {
        return automationCompositions.get(automationCompositionId);
    }

    /**
     * Remove AutomationComposition.
     *
     * @param automationCompositionId the AutomationComposition Id
     */
    public void removeAutomationComposition(@NonNull UUID automationCompositionId) {
        automationCompositions.remove(automationCompositionId);
    }

    /**
     * Add ElementDefinition.
     *
     * @param compositionId the composition Id
     * @param list the list of AutomationCompositionElementDefinition to add
     */
    public void addElementDefinition(@NonNull UUID compositionId, List<AutomationCompositionElementDefinition> list) {
        Map<ToscaConceptIdentifier, AutomationCompositionElementDefinition> map = new HashMap<>();
        for (var acElementDefinition : list) {
            map.put(acElementDefinition.getAcElementDefinitionId(), acElementDefinition);
        }
        acElementsDefinitions.put(compositionId, map);
    }

    public void removeElementDefinition(@NonNull UUID compositionId) {
        acElementsDefinitions.remove(compositionId);
    }

    /**
     * Get CommonProperties.
     *
     * @param instanceId the Automation Composition Id
     * @param acElementId the Automation Composition Element Id
     * @return the common Properties as Map
     */
    public Map<String, Object> getCommonProperties(@NonNull UUID instanceId, @NonNull UUID acElementId) {
        var automationComposition = automationCompositions.get(instanceId);
        var map = acElementsDefinitions.get(automationComposition.getCompositionId());
        var element = automationComposition.getElements().get(acElementId);
        return map.get(element.getDefinition()).getAutomationCompositionElementToscaNodeTemplate().getProperties();
    }

    /**
     * Get CommonProperties.
     *
     * @param compositionId the composition Id
     * @param definition the AutomationCompositionElementDefinition Id
     * @return the common Properties as Map
     */
    public Map<String, Object> getCommonProperties(@NonNull UUID compositionId,
        @NonNull ToscaConceptIdentifier definition) {
        return acElementsDefinitions.get(compositionId).get(definition)
            .getAutomationCompositionElementToscaNodeTemplate().getProperties();
    }

    /**
     * Initialize an AutomationComposition from a ParticipantDeploy.
     *
     * @param compositionId the composition Id
     * @param instanceId the Automation Composition Id
     * @param participantDeploy the ParticipantDeploy
     */
    public void initializeAutomationComposition(@NonNull UUID compositionId, @NonNull UUID instanceId,
            ParticipantDeploy participantDeploy) {
        var acLast = automationCompositions.get(instanceId);
        Map<UUID, AutomationCompositionElement> acElementMap = new LinkedHashMap<>();
        for (var element : participantDeploy.getAcElementList()) {
            var acElement = new AutomationCompositionElement();
            acElement.setId(element.getId());
            acElement.setParticipantId(getParticipantId());
            acElement.setDefinition(element.getDefinition());
            acElement.setDeployState(DeployState.DEPLOYING);
            acElement.setLockState(LockState.NONE);
            acElement.setProperties(element.getProperties());
            var acElementLast = acLast != null ? acLast.getElements().get(element.getId()) : null;
            if (acElementLast != null) {
                acElement.setOutProperties(acElementLast.getOutProperties());
                acElement.setOperationalState(acElementLast.getOperationalState());
                acElement.setUseState(acElementLast.getUseState());
            }
            acElementMap.put(element.getId(), acElement);
        }
        var automationComposition = new AutomationComposition();
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(instanceId);
        automationComposition.setElements(acElementMap);
        automationCompositions.put(instanceId, automationComposition);
    }

    /**
     * Initialize an AutomationComposition from a ParticipantRestartAc.
     *
     * @param compositionId the composition Id
     * @param participantRestartAc the ParticipantRestartAc
     */
    public void initializeAutomationComposition(@NonNull UUID compositionId,
            ParticipantRestartAc participantRestartAc) {
        Map<UUID, AutomationCompositionElement> acElementMap = new LinkedHashMap<>();
        for (var element : participantRestartAc.getAcElementList()) {
            var acElement = new AutomationCompositionElement();
            acElement.setId(element.getId());
            acElement.setParticipantId(getParticipantId());
            acElement.setDefinition(element.getDefinition());
            acElement.setDeployState(element.getDeployState());
            acElement.setLockState(element.getLockState());
            acElement.setOperationalState(element.getOperationalState());
            acElement.setUseState(element.getUseState());
            acElement.setProperties(element.getProperties());
            acElement.setOutProperties(element.getOutProperties());
            acElement.setRestarting(true);
            acElementMap.put(element.getId(), acElement);
        }

        var automationComposition = new AutomationComposition();
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(participantRestartAc.getAutomationCompositionId());
        automationComposition.setElements(acElementMap);
        automationCompositions.put(automationComposition.getInstanceId(), automationComposition);
    }
}
