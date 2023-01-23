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

package org.onap.policy.clamp.acm.participant.policy.main.utils;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantUpdate;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestListenerUtils {

    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();
    private static final Logger LOGGER = LoggerFactory.getLogger(TestListenerUtils.class);

    /**
     * Method to create participantUpdateMsg.
     *
     * @return ParticipantUpdate message
     */
    public static ParticipantUpdate createParticipantUpdateMsg() {
        final var participantUpdateMsg = new ParticipantUpdate();
        var participantId = CommonTestData.getParticipantId();
        var participantType =
                new ToscaConceptIdentifier("org.onap.policy.acm.PolicyAutomationCompositionParticipant", "2.3.1");

        participantUpdateMsg.setParticipantId(participantId);
        participantUpdateMsg.setTimestamp(Instant.now());
        participantUpdateMsg.setParticipantType(participantType);
        participantUpdateMsg.setTimestamp(Instant.ofEpochMilli(3000));
        participantUpdateMsg.setMessageId(UUID.randomUUID());

        var toscaServiceTemplate = testAutomationCompositionRead();
        // Add policies to the toscaServiceTemplate
        TestListenerUtils.addPoliciesToToscaServiceTemplate(toscaServiceTemplate);

        participantUpdateMsg.setParticipantDefinitionUpdates(new ArrayList<>());
        return participantUpdateMsg;
    }

    private static ToscaServiceTemplate testAutomationCompositionRead() {
        var automationCompositionDirectoryContents = ResourceUtils.getDirectoryContents("clamp/acm/test");

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
        var policyTypeDirectoryContents = ResourceUtils.getDirectoryContents("policytypes");

        for (String policyTypeFilePath : policyTypeDirectoryContents) {
            String policyTypeString = ResourceUtils.getResourceAsString(policyTypeFilePath);

            var foundPolicyTypeSt =
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
        var policiesDirectoryContents = ResourceUtils.getDirectoryContents("policies");
        toscaServiceTemplate.getToscaTopologyTemplate().setPolicies(new ArrayList<>());

        for (var policiesFilePath : policiesDirectoryContents) {
            if (!policiesFilePath.endsWith("yaml")) {
                continue;
            }

            var policiesString = ResourceUtils.getResourceAsString(policiesFilePath);

            var foundPoliciesSt = yamlTranslator.fromYaml(policiesString, ToscaServiceTemplate.class);
            if (foundPoliciesSt.getToscaTopologyTemplate() != null
                    && foundPoliciesSt.getToscaTopologyTemplate().getPolicies() != null) {
                toscaServiceTemplate.getToscaTopologyTemplate().getPolicies()
                        .addAll(foundPoliciesSt.getToscaTopologyTemplate().getPolicies());
            }
        }
    }

    private static ToscaServiceTemplate testAutomationCompositionYamlSerialization(
            String automationCompositionFilePath) {
        try {
            String automationCompositionString = ResourceUtils.getResourceAsString(automationCompositionFilePath);
            if (automationCompositionString == null) {
                throw new FileNotFoundException(automationCompositionFilePath);
            }

            return yamlTranslator.fromYaml(automationCompositionString, ToscaServiceTemplate.class);
        } catch (FileNotFoundException e) {
            LOGGER.error("cannot find YAML file {}", automationCompositionFilePath);
            throw new IllegalArgumentException(e);
        }
    }
}
