/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUpdates;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.common.parameters.ObjectValidationResult;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.parameters.ValidationStatus;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

/**
 * Utility functions used in acm-runtime and participants.
 *
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AcmUtils {

    public static final String AUTOMATION_COMPOSITION_NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition";
    public static final String ENTRY = "entry ";

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
                participantUpdate.getAutomationCompositionElementList().add(acElement);
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
        participantUpdate.getAutomationCompositionElementList().add(acElement);
        return participantUpdate;
    }

    /**
     * Set the Policy information in the service template for the automation composition element.
     *
     * @param acElement automation composition element
     * @param toscaServiceTemplate ToscaServiceTemplate
     */
    public static void setAcPolicyInfo(AutomationCompositionElement acElement,
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
     */
    public static void prepareParticipantDefinitionUpdate(ToscaConceptIdentifier acParticipantType, String entryKey,
            ToscaNodeTemplate entryValue, List<ParticipantDefinition> participantDefinitionUpdates) {

        var acDefinition = new AutomationCompositionElementDefinition();
        acDefinition.setAcElementDefinitionId(new ToscaConceptIdentifier(entryKey, entryValue.getVersion()));
        acDefinition.setAutomationCompositionElementToscaNodeTemplate(entryValue);

        List<AutomationCompositionElementDefinition> automationCompositionElementDefinitionList = new ArrayList<>();

        if (participantDefinitionUpdates.isEmpty()) {
            participantDefinitionUpdates.add(getParticipantDefinition(acDefinition, acParticipantType,
                    automationCompositionElementDefinitionList));
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

    /**
     * Validate AutomationComposition.
     *
     * @param automationComposition AutomationComposition to validate
     * @param serviceTemplate the service template
     * @return the result of validation
     */
    public static BeanValidationResult validateAutomationComposition(AutomationComposition automationComposition,
            ToscaServiceTemplate serviceTemplate) {
        var result = new BeanValidationResult(ENTRY + automationComposition.getName(), automationComposition);

        var map = getMapToscaNodeTemplates(serviceTemplate);

        var nodeTemplateGet = map.values().stream()
                .filter(nodeTemplate -> AUTOMATION_COMPOSITION_NODE_TYPE.equals(nodeTemplate.getType())).findFirst();

        if (nodeTemplateGet.isEmpty()) {
            result.addResult(new ObjectValidationResult("ToscaServiceTemplate", serviceTemplate.getName(),
                    ValidationStatus.INVALID, "Commissioned automation composition definition not consistent"));
        } else {

            var toscaNodeTemplate = nodeTemplateGet.get();
            var acElementDefinitions = getAutomationCompositionElementDefinitions(map, toscaNodeTemplate);

            // @formatter:off
            var definitions = acElementDefinitions
                    .stream()
                    .map(nodeTemplate -> nodeTemplate.getKey().asIdentifier())
                    .collect(Collectors.toMap(ToscaConceptIdentifier::getName, UnaryOperator.identity()));
            // @formatter:on

            for (var element : automationComposition.getElements().values()) {
                result.addResult(validateDefinition(definitions, element.getDefinition()));
            }
        }

        return result;

    }

    private static ValidationResult validateDefinition(Map<String, ToscaConceptIdentifier> definitions,
            ToscaConceptIdentifier definition) {
        var result = new BeanValidationResult(ENTRY + definition.getName(), definition);
        var identifier = definitions.get(definition.getName());
        if (identifier == null) {
            result.setResult(ValidationStatus.INVALID, "Not found");
        } else if (!identifier.equals(definition)) {
            result.setResult(ValidationStatus.INVALID, "Version not matching");
        }
        return (result.isClean() ? null : result);
    }

    private static Map<ToscaConceptIdentifier, ToscaNodeTemplate> getMapToscaNodeTemplates(
            ToscaServiceTemplate serviceTemplate) {
        if (serviceTemplate.getToscaTopologyTemplate() == null
                || MapUtils.isEmpty(serviceTemplate.getToscaTopologyTemplate().getNodeTemplates())) {
            return Map.of();
        }
        var list = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().values();
        return list.stream().collect(Collectors
                .toMap(node -> new ToscaConceptIdentifier(node.getName(), node.getVersion()), Function.identity()));
    }

    private static List<ToscaNodeTemplate> getAutomationCompositionElementDefinitions(
            Map<ToscaConceptIdentifier, ToscaNodeTemplate> map, ToscaNodeTemplate automationCompositionNodeTemplate) {

        if (MapUtils.isEmpty(automationCompositionNodeTemplate.getProperties())) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        var automationCompositionElements =
                (List<Map<String, String>>) automationCompositionNodeTemplate.getProperties().get("elements");

        if (CollectionUtils.isEmpty(automationCompositionElements)) {
            return Collections.emptyList();
        }

        // @formatter:off
        return automationCompositionElements
                .stream()
                .map(elementMap ->
                    map.get(new ToscaConceptIdentifier(elementMap.get("name"), elementMap.get("version"))))
                .collect(Collectors.toList());
        // @formatter:on
    }
}
