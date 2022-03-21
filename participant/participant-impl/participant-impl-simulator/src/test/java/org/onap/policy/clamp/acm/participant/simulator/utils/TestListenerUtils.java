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

package org.onap.policy.clamp.acm.participant.simulator.utils;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUpdates;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionUpdate;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestListenerUtils {

    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();
    private static final Logger LOGGER = LoggerFactory.getLogger(TestListenerUtils.class);
    private static final String TOSCA_TEMPLATE = "clamp/acm/test/pm_simple_ac_tosca.yaml";

    /**
     * Method to create a automationComposition from a yaml file.
     *
     * @return AutomationComposition automation composition
     */
    public static AutomationComposition createAutomationComposition() {
        AutomationComposition automationComposition = new AutomationComposition();
        Map<UUID, AutomationCompositionElement> elements = new LinkedHashMap<>();
        ToscaServiceTemplate toscaServiceTemplate = testAutomationCompositionYamlSerialization();
        Map<String, ToscaNodeTemplate> nodeTemplatesMap =
            toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : nodeTemplatesMap.entrySet()) {
            AutomationCompositionElement acElement = new AutomationCompositionElement();
            acElement.setId(UUID.randomUUID());

            ToscaConceptIdentifier acElementParticipantId = new ToscaConceptIdentifier();
            acElementParticipantId.setName(toscaInputEntry.getKey());
            acElementParticipantId.setVersion(toscaInputEntry.getValue().getVersion());
            acElement.setParticipantId(acElementParticipantId);
            acElement.setParticipantType(acElementParticipantId);

            acElement.setDefinition(acElementParticipantId);
            acElement.setState(AutomationCompositionState.UNINITIALISED);
            acElement.setDescription(toscaInputEntry.getValue().getDescription());
            acElement.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
            elements.put(acElement.getId(), acElement);
        }
        automationComposition.setElements(elements);
        automationComposition.setName("PMSHInstance0");
        automationComposition.setVersion("1.0.0");

        ToscaConceptIdentifier definition = new ToscaConceptIdentifier();
        definition.setName("PMSHInstance0");
        definition.setVersion("1.0.0");
        automationComposition.setDefinition(definition);

        return automationComposition;
    }

    /**
     * Method to create AutomationCompositionUpdateMsg.
     *
     * @return AutomationCompositionUpdate message
     */
    public static AutomationCompositionUpdate createAutomationCompositionUpdateMsg() {
        final AutomationCompositionUpdate acUpdateMsg = new AutomationCompositionUpdate();
        ToscaConceptIdentifier automationCompositionId = new ToscaConceptIdentifier("PMSHInstance0", "1.0.0");
        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier("org.onap.PM_Policy", "0.0.0");

        acUpdateMsg.setAutomationCompositionId(automationCompositionId);
        acUpdateMsg.setParticipantId(participantId);
        acUpdateMsg.setMessageId(UUID.randomUUID());
        acUpdateMsg.setTimestamp(Instant.now());

        Map<UUID, AutomationCompositionElement> elements = new LinkedHashMap<>();
        ToscaServiceTemplate toscaServiceTemplate = testAutomationCompositionYamlSerialization();
        Map<String, ToscaNodeTemplate> nodeTemplatesMap =
            toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : nodeTemplatesMap.entrySet()) {
            if (ParticipantUtils.checkIfNodeTemplateIsAutomationCompositionElement(toscaInputEntry.getValue(),
                toscaServiceTemplate)) {
                AutomationCompositionElement acElement = new AutomationCompositionElement();
                acElement.setId(UUID.randomUUID());
                var acParticipantType =
                    ParticipantUtils.findParticipantType(toscaInputEntry.getValue().getProperties());

                acElement.setParticipantId(acParticipantType);
                acElement.setParticipantType(acParticipantType);

                acElement.setDefinition(
                    new ToscaConceptIdentifier(toscaInputEntry.getKey(), toscaInputEntry.getValue().getVersion()));
                acElement.setState(AutomationCompositionState.UNINITIALISED);
                acElement.setDescription(toscaInputEntry.getValue().getDescription());
                acElement.setOrderedState(AutomationCompositionOrderedState.PASSIVE);
                elements.put(acElement.getId(), acElement);
            }
        }

        List<ParticipantUpdates> participantUpdates = new ArrayList<>();
        for (AutomationCompositionElement element : elements.values()) {
            AcmUtils.prepareParticipantUpdate(element, participantUpdates);
        }
        acUpdateMsg.setParticipantUpdatesList(participantUpdates);
        return acUpdateMsg;
    }

    private static ToscaServiceTemplate testAutomationCompositionYamlSerialization() {
        try {
            String automationCompositionString = ResourceUtils.getResourceAsString(TestListenerUtils.TOSCA_TEMPLATE);
            if (automationCompositionString == null) {
                throw new FileNotFoundException(TestListenerUtils.TOSCA_TEMPLATE);
            }

            return yamlTranslator.fromYaml(automationCompositionString, ToscaServiceTemplate.class);
        } catch (FileNotFoundException e) {
            LOGGER.error("cannot find YAML file {}", TestListenerUtils.TOSCA_TEMPLATE);
            throw new IllegalArgumentException(e);
        }
    }
}
