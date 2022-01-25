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

package org.onap.policy.clamp.models.acm.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUpdates;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

/**
 * Utility functions used in acm-runtime and participants.
 *
 */
public class AcmUtils {

    private AcmUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Prepare participant updates map.
     *
     * @param acElement automation composition element
     * @param participantUpdates list of participantUpdates
     */
    public static void prepareParticipantUpdate(AutomationCompositionElement acElement,
        List<ParticipantUpdates> participantUpdates) {
        if (participantUpdates.isEmpty()) {
            participantUpdates.add(getAutomationCompositionElementList(acElement));
            return;
        }

        var participantExists = false;
        for (ParticipantUpdates participantUpdate : participantUpdates) {
            if (participantUpdate.getParticipantId().equals(acElement.getParticipantId())) {
                participantUpdate.setAutomationCompositionElementList(List.of(acElement));
                participantExists = true;
            }
        }
        if (!participantExists) {
            participantUpdates.add(getAutomationCompositionElementList(acElement));
        }
    }

    private static ParticipantUpdates getAutomationCompositionElementList(AutomationCompositionElement acElement) {
        var participantUpdate = new ParticipantUpdates();
        participantUpdate.setParticipantId(acElement.getParticipantId());
        participantUpdate.setAutomationCompositionElementList(List.of(acElement));
        return participantUpdate;
    }

    /**
     * Set the Policy information in the service template for the automation composition element.
     *
     * @param acElement automation composition element
     * @param toscaServiceTemplate ToscaServiceTemplate
     */
    public static void setServiceTemplatePolicyInfo(AutomationCompositionElement acElement,
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
        acElement.setToscaServiceTemplateFragment(toscaServiceTemplateFragment);
    }

    /**
     * Prepare ParticipantDefinitionUpdate to set in the message.
     *
     * @param acParticipantType participant type
     * @param entryKey key for the entry
     * @param entryValue value relates to toscaNodeTemplate
     * @param participantDefinitionUpdates list of participantDefinitionUpdates
     * @param commonPropertiesMap common properties map
     */
    public static void prepareParticipantDefinitionUpdate(ToscaConceptIdentifier acParticipantType, String entryKey,
        ToscaNodeTemplate entryValue, List<ParticipantDefinition> participantDefinitionUpdates,
        Map<String, ToscaNodeType> commonPropertiesMap) {

        var acDefinition = new AutomationCompositionElementDefinition();
        acDefinition.setAcElementDefinitionId(new ToscaConceptIdentifier(entryKey, entryValue.getVersion()));
        acDefinition.setAutomationCompositionElementToscaNodeTemplate(entryValue);
        if (commonPropertiesMap != null) {
            ToscaNodeType nodeType = commonPropertiesMap.get(entryValue.getType());
            if (nodeType != null) {
                acDefinition.setCommonPropertiesMap(nodeType.getProperties());
            }
        }

        List<AutomationCompositionElementDefinition> automationCompositionElementDefinitionList = new ArrayList<>();

        if (participantDefinitionUpdates.isEmpty()) {
            participantDefinitionUpdates.add(
                getParticipantDefinition(acDefinition, acParticipantType, automationCompositionElementDefinitionList));
        } else {
            var participantExists = false;
            for (ParticipantDefinition participantDefinitionUpdate : participantDefinitionUpdates) {
                if (participantDefinitionUpdate.getParticipantType().equals(acParticipantType)) {
                    participantDefinitionUpdate.getAutomationCompositionElementDefinitionList().add(acDefinition);
                    participantExists = true;
                }
            }
            if (!participantExists) {
                participantDefinitionUpdates.add(getParticipantDefinition(acDefinition, acParticipantType,
                    automationCompositionElementDefinitionList));
            }
        }
    }

    private static ParticipantDefinition getParticipantDefinition(AutomationCompositionElementDefinition acDefinition,
        ToscaConceptIdentifier acParticipantType,
        List<AutomationCompositionElementDefinition> automationCompositionElementDefinitionList) {
        var participantDefinition = new ParticipantDefinition();
        participantDefinition.setParticipantType(acParticipantType);
        automationCompositionElementDefinitionList.add(acDefinition);
        participantDefinition.setAutomationCompositionElementDefinitionList(automationCompositionElementDefinitionList);
        return participantDefinition;
    }
}
