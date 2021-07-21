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

package org.onap.policy.clamp.controlloop.participant.simulator.main.rest;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantStateChange;
import org.onap.policy.clamp.controlloop.participant.simulator.main.parameters.CommonTestData;
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

    private TestListenerUtils() {}

    /**
     * Method to create a controlLoop from a yaml file.
     *
     * @return ControlLoop controlloop
     */
    public static ControlLoop createControlLoop() {
        ControlLoop controlLoop = new ControlLoop();
        Map<UUID, ControlLoopElement> elements = new LinkedHashMap<>();
        ToscaServiceTemplate toscaServiceTemplate = testControlLoopRead();
        Map<String, ToscaNodeTemplate> nodeTemplatesMap =
                toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : nodeTemplatesMap.entrySet()) {
            ControlLoopElement clElement = new ControlLoopElement();
            clElement.setId(UUID.randomUUID());

            ToscaConceptIdentifier clElementParticipantId = new ToscaConceptIdentifier();
            clElementParticipantId.setName(toscaInputEntry.getKey());
            clElementParticipantId.setVersion(toscaInputEntry.getValue().getVersion());
            clElement.setParticipantId(clElementParticipantId);
            clElement.setParticipantType(clElementParticipantId);

            clElement.setDefinition(clElementParticipantId);
            clElement.setState(ControlLoopState.UNINITIALISED);
            clElement.setDescription(toscaInputEntry.getValue().getDescription());
            clElement.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
            elements.put(clElement.getId(), clElement);
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
        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier("org.onap.PM_CDS_Blueprint", "1.0.0");

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

        ToscaConceptIdentifier controlLoopId = new ToscaConceptIdentifier("PMSHInstance0", "1.0.0");
        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier("org.onap.PM_CDS_Blueprint", "1.0.0");

        participantClStateChangeMsg.setControlLoopId(controlLoopId);
        participantClStateChangeMsg.setParticipantId(participantId);
        participantClStateChangeMsg.setTimestamp(Instant.now());
        participantClStateChangeMsg.setOrderedState(controlLoopOrderedState);

        return participantClStateChangeMsg;
    }

    /**
     * Method to create ControlLoopUpdateMsg.
     *
     * @return ControlLoopUpdate message
     */
    public static ControlLoopUpdate createControlLoopUpdateMsg() {
        final ControlLoopUpdate clUpdateMsg = new ControlLoopUpdate();
        ToscaConceptIdentifier controlLoopId = new ToscaConceptIdentifier("PMSHInstance0", "1.0.0");
        ToscaConceptIdentifier participantId = new ToscaConceptIdentifier("org.onap.PM_CDS_Blueprint", "1.0.0");

        clUpdateMsg.setControlLoopId(controlLoopId);
        clUpdateMsg.setParticipantId(participantId);
        clUpdateMsg.setParticipantType(participantId);

        ControlLoop controlLoop = new ControlLoop();
        Map<UUID, ControlLoopElement> elements = new LinkedHashMap<>();
        ToscaServiceTemplate toscaServiceTemplate = testControlLoopRead();
        Map<String, ToscaNodeTemplate> nodeTemplatesMap =
                toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        for (Map.Entry<String, ToscaNodeTemplate> toscaInputEntry : nodeTemplatesMap.entrySet()) {
            ControlLoopElement clElement = new ControlLoopElement();
            clElement.setId(UUID.randomUUID());

            ToscaConceptIdentifier clElementParticipantId = new ToscaConceptIdentifier();
            clElementParticipantId.setName(toscaInputEntry.getKey());
            clElementParticipantId.setVersion(toscaInputEntry.getValue().getVersion());
            clElement.setParticipantId(clElementParticipantId);
            clElement.setParticipantType(clElementParticipantId);

            clElement.setDefinition(clElementParticipantId);
            clElement.setState(ControlLoopState.UNINITIALISED);
            clElement.setDescription(toscaInputEntry.getValue().getDescription());
            clElement.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
            elements.put(clElement.getId(), clElement);
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
     * Method to create ControlLoopUpdate using the arguments passed.
     *
     * @param jsonFilePath the path of the controlloop content
     *
     * @return ControlLoopUpdate message
     * @throws CoderException exception while reading the file to object
     */
    public static ControlLoopUpdate createParticipantClUpdateMsgFromJson(String jsonFilePath)
            throws CoderException {
        ControlLoopUpdate controlLoopUpdateMsg =
                CODER.decode(new File(jsonFilePath), ControlLoopUpdate.class);
        return controlLoopUpdateMsg;
    }

    private static ToscaServiceTemplate testControlLoopRead() {
        Set<String> controlLoopDirectoryContents =
                ResourceUtils.getDirectoryContents("src/test/resources/rest/servicetemplates");

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
