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

package org.onap.policy.clamp.acm.participant.policy.main.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUpdates;
import org.onap.policy.clamp.models.acm.concepts.ParticipantUtils;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionUpdate;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
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
    private static final Coder CODER = new StandardCoder();
    private static final Logger LOGGER = LoggerFactory.getLogger(TestListenerUtils.class);

    /**
     * Method to create a automationComposition from a yaml file.
     *
     * @return AutomationComposition automation composition
     */
    public static AutomationComposition createAutomationComposition() {
        AutomationComposition automationComposition = new AutomationComposition();
        Map<UUID, AutomationCompositionElement> elements = new LinkedHashMap<>();
        ToscaServiceTemplate toscaServiceTemplate = testAutomationCompositionRead();
        Map<String, ToscaNodeTemplate> nodeTemplatesMap =
            toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : nodeTemplatesMap.entrySet()) {
            AutomationCompositionElement acElement = new AutomationCompositionElement();
            acElement.setId(UUID.randomUUID());

            ToscaConceptIdentifier acElementParticipantId = new ToscaConceptIdentifier();
            acElementParticipantId.setName(toscaInputEntry.getKey());
            acElementParticipantId.setVersion(toscaInputEntry.getValue().getVersion());
            acElement.setParticipantId(acElementParticipantId);

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
     * Method to create AutomationCompositionStateChange message from the arguments passed.
     *
     * @param automationCompositionOrderedState automationCompositionOrderedState
     * @return AutomationCompositionStateChange message
     */
    public static AutomationCompositionStateChange createAutomationCompositionStateChangeMsg(
        final AutomationCompositionOrderedState automationCompositionOrderedState) {
        final AutomationCompositionStateChange acStateChangeMsg = new AutomationCompositionStateChange();

        ToscaConceptIdentifier automationCompositionId = new ToscaConceptIdentifier();
        automationCompositionId.setName("PMSHInstance0");
        automationCompositionId.setVersion("1.0.0");

        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier();
        participantId.setName("org.onap.PM_Policy");
        participantId.setVersion("0.0.0");

        acStateChangeMsg.setAutomationCompositionId(automationCompositionId);
        acStateChangeMsg.setParticipantId(participantId);
        acStateChangeMsg.setTimestamp(Instant.now());
        acStateChangeMsg.setOrderedState(automationCompositionOrderedState);

        return acStateChangeMsg;
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
        ToscaServiceTemplate toscaServiceTemplate = testAutomationCompositionRead();
        TestListenerUtils.addPoliciesToToscaServiceTemplate(toscaServiceTemplate);
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
            AcmUtils.setServiceTemplatePolicyInfo(element, toscaServiceTemplate);
            AcmUtils.prepareParticipantUpdate(element, participantUpdates);
        }
        acUpdateMsg.setParticipantUpdatesList(participantUpdates);
        return acUpdateMsg;
    }

    /**
     * Method to create participantUpdateMsg.
     *
     * @return ParticipantUpdate message
     */
    public static ParticipantUpdate createParticipantUpdateMsg() {
        final ParticipantUpdate participantUpdateMsg = new ParticipantUpdate();
        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier("org.onap.PM_Policy", "1.0.0");
        ToscaConceptIdentifier participantType =
            new ToscaConceptIdentifier("org.onap.policy.acm.PolicyAutomationCompositionParticipant", "2.3.1");

        participantUpdateMsg.setParticipantId(participantId);
        participantUpdateMsg.setTimestamp(Instant.now());
        participantUpdateMsg.setParticipantType(participantType);
        participantUpdateMsg.setTimestamp(Instant.ofEpochMilli(3000));
        participantUpdateMsg.setMessageId(UUID.randomUUID());

        ToscaServiceTemplate toscaServiceTemplate = testAutomationCompositionRead();
        // Add policies to the toscaServiceTemplate
        TestListenerUtils.addPoliciesToToscaServiceTemplate(toscaServiceTemplate);

        List<ParticipantDefinition> participantDefinitionUpdates = new ArrayList<>();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : toscaServiceTemplate.getToscaTopologyTemplate()
            .getNodeTemplates().entrySet()) {
            if (ParticipantUtils.checkIfNodeTemplateIsAutomationCompositionElement(toscaInputEntry.getValue(),
                toscaServiceTemplate)) {
                AcmUtils.prepareParticipantDefinitionUpdate(
                    ParticipantUtils.findParticipantType(toscaInputEntry.getValue().getProperties()),
                    toscaInputEntry.getKey(), toscaInputEntry.getValue(),
                    participantDefinitionUpdates, null);
            }
        }

        participantUpdateMsg.setParticipantDefinitionUpdates(participantDefinitionUpdates);
        return participantUpdateMsg;
    }

    /**
     * Method to create AutomationCompositionUpdate using the arguments passed.
     *
     * @param jsonFilePath the path of the automation composition content
     * @return AutomationCompositionUpdate message
     * @throws CoderException exception while reading the file to object
     */
    public static AutomationCompositionUpdate createParticipantAcUpdateMsgFromJson(String jsonFilePath)
        throws CoderException {
        AutomationCompositionUpdate automationCompositionUpdateMsg =
            CODER.decode(new File(jsonFilePath), AutomationCompositionUpdate.class);
        return automationCompositionUpdateMsg;
    }

    private static ToscaServiceTemplate testAutomationCompositionRead() {
        Set<String> automationCompositionDirectoryContents =
            ResourceUtils.getDirectoryContents("src/test/resources/utils/servicetemplates");

        boolean atLeastOneAutomationCompositionTested = false;
        ToscaServiceTemplate toscaServiceTemplate = null;

        for (String automationCompositionFilePath : automationCompositionDirectoryContents) {
            if (!automationCompositionFilePath.endsWith(".yaml")) {
                continue;
            }
            atLeastOneAutomationCompositionTested = true;
            toscaServiceTemplate = testAutomationCompositionYamlSerialization(automationCompositionFilePath);
        }

        // Add policy_types to the toscaServiceTemplate
        addPolicyTypesToToscaServiceTemplate(toscaServiceTemplate);

        assertTrue(atLeastOneAutomationCompositionTested);
        return toscaServiceTemplate;
    }

    private static void addPolicyTypesToToscaServiceTemplate(ToscaServiceTemplate toscaServiceTemplate) {
        Set<String> policyTypeDirectoryContents = ResourceUtils.getDirectoryContents("policytypes");

        for (String policyTypeFilePath : policyTypeDirectoryContents) {
            String policyTypeString = ResourceUtils.getResourceAsString(policyTypeFilePath);

            ToscaServiceTemplate foundPolicyTypeSt =
                yamlTranslator.fromYaml(policyTypeString, ToscaServiceTemplate.class);

            toscaServiceTemplate.setDerivedFrom(foundPolicyTypeSt.getDerivedFrom());
            toscaServiceTemplate.setDescription(foundPolicyTypeSt.getDescription());
            toscaServiceTemplate.setMetadata(foundPolicyTypeSt.getMetadata());
            toscaServiceTemplate.setName(foundPolicyTypeSt.getName());
            toscaServiceTemplate.setToscaDefinitionsVersion(foundPolicyTypeSt.getToscaDefinitionsVersion());
            toscaServiceTemplate.setVersion(foundPolicyTypeSt.getVersion());

            if (foundPolicyTypeSt.getDataTypes() != null) {
                if (toscaServiceTemplate.getDataTypes() == null) {
                    toscaServiceTemplate.setDataTypes(foundPolicyTypeSt.getDataTypes());
                } else {
                    toscaServiceTemplate.getDataTypes().putAll(foundPolicyTypeSt.getDataTypes());
                }
            }

            if (toscaServiceTemplate.getPolicyTypes() == null) {
                toscaServiceTemplate.setPolicyTypes(foundPolicyTypeSt.getPolicyTypes());
            } else {
                toscaServiceTemplate.getPolicyTypes().putAll(foundPolicyTypeSt.getPolicyTypes());
            }
        }
    }

    /**
     * Method to add polcies to the toscaServiceTemplate.
     *
     * @param toscaServiceTemplate to add policies
     */
    public static void addPoliciesToToscaServiceTemplate(ToscaServiceTemplate toscaServiceTemplate) {
        Set<String> policiesDirectoryContents = ResourceUtils.getDirectoryContents("policies");

        for (String policiesFilePath : policiesDirectoryContents) {
            if (!policiesFilePath.endsWith("yaml")) {
                continue;
            }

            String policiesString = ResourceUtils.getResourceAsString(policiesFilePath);

            ToscaServiceTemplate foundPoliciesSt =
                yamlTranslator.fromYaml(policiesString, ToscaServiceTemplate.class);
            toscaServiceTemplate.getToscaTopologyTemplate()
                .setPolicies(foundPoliciesSt.getToscaTopologyTemplate().getPolicies());
        }
    }

    private static ToscaServiceTemplate testAutomationCompositionYamlSerialization(
        String automationCompositionFilePath) {
        try {
            String automationCompositionString = ResourceUtils.getResourceAsString(automationCompositionFilePath);
            if (automationCompositionString == null) {
                throw new FileNotFoundException(automationCompositionFilePath);
            }

            ToscaServiceTemplate serviceTemplate =
                yamlTranslator.fromYaml(automationCompositionString, ToscaServiceTemplate.class);
            return serviceTemplate;
        } catch (FileNotFoundException e) {
            LOGGER.error("cannot find YAML file", automationCompositionFilePath);
            throw new IllegalArgumentException(e);
        }
    }
}
