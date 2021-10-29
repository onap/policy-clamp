/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.common.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantUpdates;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

/**
 * Utility functions used in controlloop-runtime and participants.
 *
 */
public class CommonUtils {
    private static final String POLICY_TYPE_ID = "policy_type_id";
    private static final String POLICY_ID = "policy_id";

    /**
     * Prepare participant updates map.
     *
     * @param clElement controlloop element
     * @param participantUpdates list of participantUpdates
     */
    public static void prepareParticipantUpdate(ControlLoopElement clElement,
        List<ParticipantUpdates> participantUpdates) {
        if (participantUpdates.isEmpty()) {
            participantUpdates.add(getControlLoopElementList(clElement));
            return;
        }

        var participantExists = false;
        for (ParticipantUpdates participantUpdate : participantUpdates) {
            if (participantUpdate.getParticipantId().equals(clElement.getParticipantId())) {
                participantUpdate.setControlLoopElementList(List.of(clElement));
                participantExists = true;
            }
        }
        if (!participantExists) {
            participantUpdates.add(getControlLoopElementList(clElement));
        }
    }

    private static ParticipantUpdates getControlLoopElementList(ControlLoopElement clElement) {
        var participantUpdate = new ParticipantUpdates();
        participantUpdate.setParticipantId(clElement.getParticipantId());
        participantUpdate.setControlLoopElementList(List.of(clElement));
        return participantUpdate;
    }

    /**
     * Set the Policy information in the service template for the controlloopelement.
     *
     * @param clElement controlloop element
     * @param toscaServiceTemplate ToscaServiceTemplate
     */
    public static void setServiceTemplatePolicyInfo(ControlLoopElement clElement,
            ToscaServiceTemplate toscaServiceTemplate) {
        // Pass respective PolicyTypes or Policies as part of toscaServiceTemplateFragment
        if (toscaServiceTemplate.getPolicyTypes() == null
            && toscaServiceTemplate.getToscaTopologyTemplate().getPolicies() == null) {
            return;
        }
        ToscaServiceTemplate toscaServiceTemplateFragment = new ToscaServiceTemplate();
        toscaServiceTemplateFragment.setPolicyTypes(toscaServiceTemplate.getPolicyTypes());
        ToscaTopologyTemplate toscaTopologyTemplate = new ToscaTopologyTemplate();
        toscaTopologyTemplate.setPolicies(toscaServiceTemplate.getToscaTopologyTemplate().getPolicies());
        toscaServiceTemplateFragment.setToscaTopologyTemplate(toscaTopologyTemplate);
        toscaServiceTemplateFragment.setDataTypes(toscaServiceTemplate.getDataTypes());
        clElement.setToscaServiceTemplateFragment(toscaServiceTemplateFragment);
    }

    /**
     * Prepare ParticipantDefinitionUpdate to set in the message.
     *
     * @param clParticipantType controlloop element
     * @param entryKey key for the entry
     * @param entryValue value relates to toscaNodeTemplate
     * @param participantDefinitionUpdates list of participantDefinitionUpdates
     * @param commonPropertiesMap common properties map
     */
    public static void prepareParticipantDefinitionUpdate(ToscaConceptIdentifier clParticipantType, String entryKey,
            ToscaNodeTemplate entryValue, List<ParticipantDefinition> participantDefinitionUpdates,
            Map<String, ToscaNodeType> commonPropertiesMap) {

        var clDefinition = new ControlLoopElementDefinition();
        clDefinition.setClElementDefinitionId(new ToscaConceptIdentifier(entryKey, entryValue.getVersion()));
        clDefinition.setControlLoopElementToscaNodeTemplate(entryValue);
        if (commonPropertiesMap != null) {
            ToscaNodeType nodeType = commonPropertiesMap.get(entryValue.getType());
            if (nodeType != null) {
                clDefinition.setCommonPropertiesMap(nodeType.getProperties());
            }
        }

        List<ControlLoopElementDefinition> controlLoopElementDefinitionList = new ArrayList<>();

        if (participantDefinitionUpdates.isEmpty()) {
            participantDefinitionUpdates
                    .add(getParticipantDefinition(clDefinition, clParticipantType, controlLoopElementDefinitionList));
        } else {
            var participantExists = false;
            for (ParticipantDefinition participantDefinitionUpdate : participantDefinitionUpdates) {
                if (participantDefinitionUpdate.getParticipantType().equals(clParticipantType)) {
                    participantDefinitionUpdate.getControlLoopElementDefinitionList().add(clDefinition);
                    participantExists = true;
                }
            }
            if (!participantExists) {
                participantDefinitionUpdates.add(
                        getParticipantDefinition(clDefinition, clParticipantType, controlLoopElementDefinitionList));
            }
        }
    }

    private static ParticipantDefinition getParticipantDefinition(ControlLoopElementDefinition clDefinition,
            ToscaConceptIdentifier clParticipantType,
            List<ControlLoopElementDefinition> controlLoopElementDefinitionList) {
        var participantDefinition = new ParticipantDefinition();
        participantDefinition.setParticipantType(clParticipantType);
        controlLoopElementDefinitionList.add(clDefinition);
        participantDefinition.setControlLoopElementDefinitionList(controlLoopElementDefinitionList);
        return participantDefinition;
    }
}
