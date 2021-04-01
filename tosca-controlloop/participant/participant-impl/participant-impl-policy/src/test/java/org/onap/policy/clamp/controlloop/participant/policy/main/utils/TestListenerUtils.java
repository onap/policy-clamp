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

package org.onap.policy.clamp.controlloop.participant.policy.main.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantHealthCheck;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStateChange;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.controlloop.participant.policy.main.handler.PolicyProvider;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.ParticipantPolicyParameters;
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

public class TestListenerUtils {

    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();
    private static final Coder CODER = new StandardCoder();
    static CommonTestData commonTestData = new CommonTestData();
    private static final Logger LOGGER = LoggerFactory.getLogger(TestListenerUtils.class);

    @Getter
    private static ParticipantHandler participantHandler;

    private TestListenerUtils() {}

    /**
     * Method to initialize participantHandler.
     */
    public static void initParticipantHandler() {

        final ParticipantPolicyParameters participantParameters = commonTestData.toObject(
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME),
                ParticipantPolicyParameters.class);

        PolicyProvider policyProvider =
                new PolicyProvider(participantParameters.getIntermediaryParameters());

        participantHandler = policyProvider.getIntermediaryApi().getParticipantHandler();
    }

    /**
     * Method to create a controlLoop from a yaml file.
     *
     * @return ControlLoop controlloop
     */
    public static ControlLoop createControlLoop() {
        ControlLoop controlLoop = new ControlLoop();
        List<ControlLoopElement> elements = new ArrayList<>();
        ToscaServiceTemplate toscaServiceTemplate = testControlLoopRead("src/test/resources/utils/servicetemplates");
        Map<String, ToscaNodeTemplate> nodeTemplatesMap =
                toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : nodeTemplatesMap.entrySet()) {
            ControlLoopElement clElement = new ControlLoopElement();
            clElement.setId(UUID.randomUUID());

            ToscaConceptIdentifier clElementParticipantId = new ToscaConceptIdentifier();
            clElementParticipantId.setName(toscaInputEntry.getKey());
            clElementParticipantId.setVersion(toscaInputEntry.getValue().getVersion());
            clElement.setParticipantId(clElementParticipantId);

            clElement.setDefinition(clElementParticipantId);
            clElement.setState(ControlLoopState.UNINITIALISED);
            clElement.setDescription(toscaInputEntry.getValue().getDescription());
            clElement.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
            elements.add(clElement);
        }
        controlLoop.setElements(elements);
        controlLoop.setName("PMSHInstance0");
        controlLoop.setVersion("1.0.0");

        ToscaConceptIdentifier definition = new ToscaConceptIdentifier();
        definition.setName("PMSHInstance0");
        definition.setVersion("1.0.0");
        controlLoop.setDefinition(definition);

        return controlLoop;
    }

    /**
     * Method to create ParticipantStateChange message from the arguments passed.
     *
     * @param participantState participant State
     *
     * @return ParticipantStateChange message
     */
    public static ParticipantStateChange createParticipantStateChangeMsg(final ParticipantState participantState) {
        final ParticipantStateChange participantStateChangeMsg = new ParticipantStateChange();
        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier();
        participantId.setName("org.onap.PM_Policy");
        participantId.setVersion("0.0.0");

        participantStateChangeMsg.setParticipantId(participantId);
        participantStateChangeMsg.setTimestamp(Instant.now());
        participantStateChangeMsg.setState(participantState);

        return participantStateChangeMsg;
    }

    /**
     * Method to create ControlLoopStateChange message from the arguments passed.
     *
     * @param controlLoopOrderedState controlLoopOrderedState
     *
     * @return ParticipantControlLoopStateChange message
     */
    public static ParticipantControlLoopStateChange createControlLoopStateChangeMsg(
            final ControlLoopOrderedState controlLoopOrderedState) {
        final ParticipantControlLoopStateChange participantClStateChangeMsg = new ParticipantControlLoopStateChange();

        ToscaConceptIdentifier controlLoopId = new ToscaConceptIdentifier();
        controlLoopId.setName("PMSHInstance0");
        controlLoopId.setVersion("1.0.0");

        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier();
        participantId.setName("org.onap.PM_Policy");
        participantId.setVersion("0.0.0");

        participantClStateChangeMsg.setControlLoopId(controlLoopId);
        participantClStateChangeMsg.setParticipantId(participantId);
        participantClStateChangeMsg.setTimestamp(Instant.now());
        participantClStateChangeMsg.setOrderedState(controlLoopOrderedState);

        return participantClStateChangeMsg;
    }

    /**
     * Method to create ControlLoopUpdateMsg.
     *
     * @return ParticipantControlLoopUpdate message
     */
    public static ParticipantControlLoopUpdate createControlLoopUpdateMsg(final String inputDirPath) {
        final ParticipantControlLoopUpdate clUpdateMsg = new ParticipantControlLoopUpdate();
        ToscaConceptIdentifier controlLoopId = new ToscaConceptIdentifier();
        controlLoopId.setName("PMSHInstance0");
        controlLoopId.setVersion("1.0.0");

        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier();
        participantId.setName("org.onap.PM_Policy");
        participantId.setVersion("0.0.0");

        clUpdateMsg.setControlLoopId(controlLoopId);
        clUpdateMsg.setParticipantId(participantId);

        ControlLoop controlLoop = new ControlLoop();
        List<ControlLoopElement> elements = new ArrayList<>();
        ToscaServiceTemplate toscaServiceTemplate = testControlLoopRead(inputDirPath);
        Map<String, ToscaNodeTemplate> nodeTemplatesMap =
                toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : nodeTemplatesMap.entrySet()) {
            ControlLoopElement clElement = new ControlLoopElement();
            clElement.setId(UUID.randomUUID());

            ToscaConceptIdentifier clElementParticipantId = new ToscaConceptIdentifier();
            clElementParticipantId.setName(toscaInputEntry.getKey());
            clElementParticipantId.setVersion(toscaInputEntry.getValue().getVersion());
            clElement.setParticipantId(clElementParticipantId);

            clElement.setDefinition(clElementParticipantId);
            clElement.setState(ControlLoopState.UNINITIALISED);
            clElement.setDescription(toscaInputEntry.getValue().getDescription());
            clElement.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
            elements.add(clElement);
        }
        controlLoop.setElements(elements);
        controlLoop.setName("PMSHInstance0");
        controlLoop.setVersion("1.0.0");
        controlLoop.setDefinition(controlLoopId);
        clUpdateMsg.setControlLoop(controlLoop);
        clUpdateMsg.setControlLoopDefinition(toscaServiceTemplate);

        return clUpdateMsg;
    }

    /**
     * Method to create ParticipantHealthCheck message.
     *
     * @return ParticipantHealthCheck message
     */
    public static ParticipantHealthCheck createParticipantHealthCheckMsg() {
        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier();
        participantId.setName("org.onap.PM_Policy");
        participantId.setVersion("0.0.0");

        ToscaConceptIdentifier controlLoopId = new ToscaConceptIdentifier();
        controlLoopId.setName("PMSHInstance0");
        controlLoopId.setVersion("1.0.0");

        final ParticipantHealthCheck participantHealthCheckMsg = new ParticipantHealthCheck();
        participantHealthCheckMsg.setParticipantId(participantId);
        participantHealthCheckMsg.setControlLoopId(controlLoopId);
        participantHealthCheckMsg.setTimestamp(Instant.now());
        participantHealthCheckMsg.setState(ParticipantState.PASSIVE);

        return participantHealthCheckMsg;
    }

    /**
     * Method to create ParticipantControlLoopUpdate using the arguments passed.
     *
     * @param jsonFilePath the path of the controlloop content
     *
     * @return ParticipantControlLoopUpdate message
     * @throws CoderException exception while reading the file to object
     */
    public static ParticipantControlLoopUpdate createParticipantClUpdateMsgFromJson(String jsonFilePath)
            throws CoderException {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg =
                CODER.decode(new File(jsonFilePath), ParticipantControlLoopUpdate.class);
        return participantControlLoopUpdateMsg;
    }

    private static ToscaServiceTemplate testControlLoopRead(final String inputDirPath) {
        Set<String> controlLoopDirectoryContents =
                ResourceUtils.getDirectoryContents(inputDirPath);

        boolean atLeastOneControlLoopTested = false;
        ToscaServiceTemplate toscaServiceTemplate = null;

        for (String controlLoopFilePath : controlLoopDirectoryContents) {
            if (!controlLoopFilePath.endsWith(".yaml")) {
                continue;
            }
            atLeastOneControlLoopTested = true;
            toscaServiceTemplate = testControlLoopYamlSerialization(controlLoopFilePath);
        }

        assertTrue(atLeastOneControlLoopTested);
        return toscaServiceTemplate;
    }

    /**
     * Method to add polcies to the toscaServiceTemplate.
     *
     * @param toscaServiceTemplate to add policies
     */
    public static void addPolicyTypesToToscaServiceTemplate(
            ToscaServiceTemplate toscaServiceTemplate) {
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
            String policiesString = ResourceUtils.getResourceAsString(policiesFilePath);

            ToscaServiceTemplate foundPoliciesSt =
                yamlTranslator.fromYaml(policiesString, ToscaServiceTemplate.class);
            toscaServiceTemplate.getToscaTopologyTemplate().setPolicies(
                    foundPoliciesSt.getToscaTopologyTemplate().getPolicies());
        }
    }

    private static ToscaServiceTemplate testControlLoopYamlSerialization(String controlLoopFilePath) {
        try {
            String controlLoopString = ResourceUtils.getResourceAsString(controlLoopFilePath);
            if (controlLoopString == null) {
                throw new FileNotFoundException(controlLoopFilePath);
            }

            ToscaServiceTemplate serviceTemplate = yamlTranslator.fromYaml(
                        controlLoopString, ToscaServiceTemplate.class);
            return serviceTemplate;
        } catch (FileNotFoundException e) {
            LOGGER.error("cannot find YAML file", controlLoopFilePath);
            throw new IllegalArgumentException(e);
        }
    }
}
