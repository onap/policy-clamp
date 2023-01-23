/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.intermediary.main.parameters;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.acm.participant.intermediary.handler.AutomationCompositionHandler;
import org.onap.policy.clamp.acm.participant.intermediary.handler.DummyParticipantParameters;
import org.onap.policy.clamp.acm.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to hold/create all parameters for test cases.
 */
public class CommonTestData {
    public static final String PARTICIPANT_GROUP_NAME = "AutomationCompositionParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> TOPIC_PARAMS = List.of(getTopicParams());
    public static final Coder CODER = new StandardCoder();
    private static final Object lockit = new Object();
    public static final UUID AC_ID_0 = UUID.randomUUID();
    public static final UUID AC_ID_1 = UUID.randomUUID();
    public static final ToscaConceptIdentifier PARTCICIPANT_ID =
            new ToscaConceptIdentifier("org.onap.PM_Policy", "0.0.0");

    /**
     * Get ParticipantIntermediaryParameters.
     *
     * @return ParticipantIntermediaryParameters
     */
    public ParticipantIntermediaryParameters getParticipantIntermediaryParameters() {
        try {
            return CODER.convert(getIntermediaryParametersMap(PARTICIPANT_GROUP_NAME),
                    ParticipantIntermediaryParameters.class);
        } catch (final CoderException e) {
            throw new RuntimeException("cannot create ParticipantSimulatorParameters from map", e);
        }
    }

    /**
     * Get ParticipantParameters.
     *
     * @return ParticipantParameters
     */
    public static DummyParticipantParameters getParticipantParameters() {
        try {
            return CODER.convert(getParametersMap(PARTICIPANT_GROUP_NAME), DummyParticipantParameters.class);
        } catch (final CoderException e) {
            throw new RuntimeException("cannot create ParticipantSimulatorParameters from map", e);
        }
    }

    /**
     * Returns a property map for a Parameters map for test cases.
     *
     * @param name name of the parameters
     * @return a property map suitable for constructing an object
     */
    public static Map<String, Object> getParametersMap(final String name) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("intermediaryParameters", getIntermediaryParametersMap(name));
        return map;
    }

    /**
     * Returns a property map for a intermediaryParameters map for test cases.
     *
     * @param name name of the parameters
     * @return a property map suitable for constructing an object
     */
    public static Map<String, Object> getIntermediaryParametersMap(final String name) {
        final Map<String, Object> map = new TreeMap<>();
        map.put("name", name);
        map.put("participantId", getParticipantId());
        map.put("description", DESCRIPTION);
        map.put("participantType", getDefinition());
        map.put("reportingTimeIntervalMs", TIME_INTERVAL);
        map.put("clampAutomationCompositionTopics", getTopicParametersMap(false));
        map.put("participantSupportedElementTypes", new ArrayList<>());

        return map;
    }

    /**
     * Returns a property map for a TopicParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public static Map<String, Object> getTopicParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("topicSources", TOPIC_PARAMS);
            map.put("topicSinks", TOPIC_PARAMS);
        }
        return map;
    }

    /**
     * Returns topic parameters for test cases.
     *
     * @return topic parameters
     */
    public static TopicParameters getTopicParams() {
        final var topicParams = new TopicParameters();
        topicParams.setTopic("POLICY-ACRUNTIME-PARTICIPANT");
        topicParams.setTopicCommInfrastructure("dmaap");
        topicParams.setServers(List.of("localhost"));
        return topicParams;
    }

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static ToscaConceptIdentifier getParticipantId() {
        return PARTCICIPANT_ID;
    }

    public static ToscaConceptIdentifier getRndParticipantId() {
        return new ToscaConceptIdentifier("diff", "0.0.0");
    }

    public static ToscaConceptIdentifier getDefinition() {
        return new ToscaConceptIdentifier("org.onap.PM_CDS_Blueprint", "1.0.1");
    }

    /**
     * Returns a participantMessagePublisher for MessageSender.
     *
     * @return participant Message Publisher
     */
    private ParticipantMessagePublisher getParticipantMessagePublisher() {
        synchronized (lockit) {
            var participantMessagePublisher = new ParticipantMessagePublisher();
            participantMessagePublisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
            return participantMessagePublisher;
        }
    }

    /**
     * Returns a mocked AutomationCompositionHandler for test cases.
     *
     * @return AutomationCompositionHandler
     */
    public AutomationCompositionHandler getMockAutomationCompositionHandler() {
        return new AutomationCompositionHandler(getParticipantParameters(), getParticipantMessagePublisher());
    }

    /**
     * Returns a mocked ParticipantHandler for test cases.
     *
     * @return participant Handler
     */
    public ParticipantHandler getMockParticipantHandler() {
        var parameters = getParticipantParameters();
        var automationCompositionHandler = getMockAutomationCompositionHandler();
        var publisher = new ParticipantMessagePublisher();
        publisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
        return new ParticipantHandler(parameters, publisher, automationCompositionHandler);
    }

    /**
     * Returns a mocked ParticipantHandler for test cases.
     *
     * @return participant Handler
     *
     * @throws CoderException if there is an error with .json file.
     */
    public ParticipantHandler getParticipantHandlerAutomationCompositions() throws CoderException {
        var automationCompositionHandler = Mockito.mock(AutomationCompositionHandler.class);
        Mockito.doReturn(getTestAutomationCompositionMap()).when(automationCompositionHandler)
                .getAutomationCompositionMap();
        var publisher = new ParticipantMessagePublisher();
        publisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
        var parameters = getParticipantParameters();
        var participantHandler = new ParticipantHandler(parameters, publisher, automationCompositionHandler);
        participantHandler.sendParticipantRegister();
        participantHandler.handleParticipantStatusReq(null);
        participantHandler.sendParticipantDeregister();
        var participantDeregisterAckMsg = new ParticipantDeregisterAck();
        participantDeregisterAckMsg.setResponseTo(UUID.randomUUID());
        participantHandler.handleParticipantDeregisterAck(participantDeregisterAckMsg);
        return participantHandler;
    }

    /**
     * Returns a Map of ToscaConceptIdentifier and AutomationComposition for test cases.
     *
     * @return automationCompositionMap
     *
     * @throws CoderException if there is an error with .json file.
     */
    public Map<UUID, AutomationComposition> getTestAutomationCompositionMap() {
        var automationCompositions = getTestAutomationCompositions();
        var automationComposition = automationCompositions.getAutomationCompositionList().get(1);
        Map<UUID, AutomationComposition> automationCompositionMap = new LinkedHashMap<>();
        automationCompositionMap.put(automationComposition.getInstanceId(), automationComposition);
        return automationCompositionMap;
    }

    /**
     * Returns List of AutomationComposition for test cases.
     *
     * @return AutomationCompositions
     *
     * @throws CoderException if there is an error with .json file.
     */
    public AutomationCompositions getTestAutomationCompositions() {
        try {
            var automationCompositions =
                    new StandardCoder().decode(new File("src/test/resources/providers/TestAutomationCompositions.json"),
                            AutomationCompositions.class);
            automationCompositions.getAutomationCompositionList().get(1).setInstanceId(AC_ID_0);
            automationCompositions.getAutomationCompositionList().get(1).setInstanceId(AC_ID_1);
            return automationCompositions;
        } catch (Exception e) {
            throw new RuntimeException("cannot read TestAutomationCompositions.json");
        }
    }

    /**
     * Returns a map for a elementsOnThisParticipant for test cases.
     *
     * @param uuid UUID
     * @param definition ToscaConceptIdentifier
     * @return a map suitable for elementsOnThisParticipant
     */
    public Map<UUID, AutomationCompositionElement> setAutomationCompositionElementTest(UUID uuid,
            ToscaConceptIdentifier definition, ToscaConceptIdentifier participantId) {
        var acElement = new AutomationCompositionElement();
        acElement.setId(uuid);
        acElement.setParticipantId(participantId);
        acElement.setDefinition(definition);
        acElement.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);

        Map<UUID, AutomationCompositionElement> elementsOnThisParticipant = new LinkedHashMap<>();
        elementsOnThisParticipant.put(uuid, acElement);
        return elementsOnThisParticipant;
    }

    /**
     * Returns a AutomationCompositionHandler with elements on the definition,uuid.
     *
     * @param definition ToscaConceptIdentifier
     * @param  uuid UUID
     * @return a AutomationCompositionHander with elements
     */
    public AutomationCompositionHandler setTestAutomationCompositionHandler(ToscaConceptIdentifier definition,
            UUID uuid, ToscaConceptIdentifier participantId) {
        var ach = getMockAutomationCompositionHandler();

        var key = getTestAutomationCompositionMap().keySet().iterator().next();
        var value = getTestAutomationCompositionMap().get(key);
        ach.getAutomationCompositionMap().put(key, value);

        var keyElem = setAutomationCompositionElementTest(uuid, definition, participantId).keySet().iterator().next();
        var valueElem = setAutomationCompositionElementTest(uuid, definition, participantId).get(keyElem);
        ach.getElementsOnThisParticipant().put(keyElem, valueElem);

        return ach;
    }

    /**
     * Return a AutomationCompositionStateChange.
     *
     * @param participantId the participantId
     * @param  uuid UUID
     * @param state a AutomationCompositionOrderedState
     * @return a AutomationCompositionStateChange
     */
    public AutomationCompositionStateChange getStateChange(ToscaConceptIdentifier participantId, UUID uuid,
            AutomationCompositionOrderedState state) {
        var stateChange = new AutomationCompositionStateChange();
        stateChange.setAutomationCompositionId(UUID.randomUUID());
        stateChange.setParticipantId(participantId);
        stateChange.setMessageId(uuid);
        stateChange.setOrderedState(state);
        stateChange.setCurrentState(AutomationCompositionState.UNINITIALISED);
        stateChange.setTimestamp(Instant.ofEpochMilli(3000));
        return stateChange;
    }
}
