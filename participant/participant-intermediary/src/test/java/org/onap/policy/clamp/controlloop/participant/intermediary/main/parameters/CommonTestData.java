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

package org.onap.policy.clamp.controlloop.participant.intermediary.main.parameters;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantDeregisterAck;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ParticipantMessagePublisher;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ControlLoopHandler;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.DummyParticipantParameters;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
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
    public static final String PARTICIPANT_GROUP_NAME = "ControlLoopParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> TOPIC_PARAMS = Arrays.asList(getTopicParams());
    public static final Coder CODER = new StandardCoder();
    private static final Object lockit = new Object();

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
            return CODER.convert(getParametersMap(PARTICIPANT_GROUP_NAME),
                    DummyParticipantParameters.class);
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
        map.put("participantType", getParticipantId());
        map.put("reportingTimeIntervalMs", TIME_INTERVAL);
        map.put("clampControlLoopTopics", getTopicParametersMap(false));

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
        topicParams.setTopic("POLICY-CLRUNTIME-PARTICIPANT");
        topicParams.setTopicCommInfrastructure("dmaap");
        topicParams.setServers(Arrays.asList("localhost"));
        return topicParams;
    }

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static ToscaConceptIdentifier getParticipantId() {
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
     * Returns a mocked ControlLoopHandler for test cases.
     *
     * @return ControlLoopHandler
     */
    public ControlLoopHandler getMockControlLoopHandler() {
        return new ControlLoopHandler(
                getParticipantParameters(),
                getParticipantMessagePublisher());
    }

    /**
     * Returns a mocked ParticipantHandler for test cases.
     *
     * @return participant Handler
     */
    public ParticipantHandler getMockParticipantHandler() {
        var parameters = getParticipantParameters();
        var controlLoopHandler = getMockControlLoopHandler();
        var publisher = new ParticipantMessagePublisher();
        publisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
        var participantHandler = new ParticipantHandler(parameters, publisher, controlLoopHandler);
        return participantHandler;
    }

    /**
     * Returns a mocked ParticipantHandler for test cases.
     *
     * @return participant Handler
     *
     * @throws CoderException if there is an error with .json file.
     */
    public ParticipantHandler getParticipantHandlerControlLoops() throws CoderException {
        var controlLoopHandler = Mockito.mock(ControlLoopHandler.class);
        Mockito.doReturn(getTestControlLoops()).when(controlLoopHandler).getControlLoops();
        Mockito.doReturn(getTestControlLoopMap()).when(controlLoopHandler).getControlLoopMap();
        var publisher = new ParticipantMessagePublisher();
        publisher.active(Collections.singletonList(Mockito.mock(TopicSink.class)));
        var parameters = getParticipantParameters();
        var participantHandler = new ParticipantHandler(parameters, publisher, controlLoopHandler);
        participantHandler.sendParticipantRegister();
        participantHandler.handleParticipantStatusReq(null);
        participantHandler.sendParticipantDeregister();
        var participantDeregisterAckMsg = new ParticipantDeregisterAck();
        participantDeregisterAckMsg.setResponseTo(UUID.randomUUID());
        participantHandler.handleParticipantDeregisterAck(participantDeregisterAckMsg);
        return participantHandler;
    }

    /**
     * Returns a Map of ToscaConceptIdentifier and ControlLoop for test cases.
     *
     * @return controlLoopMap
     *
     * @throws CoderException if there is an error with .json file.
     */
    public Map<ToscaConceptIdentifier, ControlLoop> getTestControlLoopMap() throws CoderException {
        var controlLoops = getTestControlLoops();
        var controlLoop = controlLoops.getControlLoopList().get(1);
        var id = getParticipantId();
        Map<ToscaConceptIdentifier, ControlLoop> controlLoopMap = new LinkedHashMap<>();
        controlLoopMap.put(id, controlLoop);
        return controlLoopMap;
    }

    /**
     * Returns List of ControlLoop for test cases.
     *
     * @return ControlLoops
     *
     * @throws CoderException if there is an error with .json file.
     */
    public ControlLoops getTestControlLoops() throws CoderException {
        return new StandardCoder()
                .decode(new File("src/test/resources/providers/TestControlLoops.json"), ControlLoops.class);
    }

    /**
     * Returns a map for a elementsOnThisParticipant for test cases.
     *
     * @param uuid UUID and id ToscaConceptIdentifier
     * @return a map suitable for elementsOnThisParticipant
     */
    public Map<UUID, ControlLoopElement> setControlLoopElementTest(UUID uuid, ToscaConceptIdentifier id) {
        var clElement = new ControlLoopElement();
        clElement.setId(uuid);
        clElement.setParticipantId(id);
        clElement.setDefinition(id);
        clElement.setOrderedState(ControlLoopOrderedState.UNINITIALISED);

        var clElementStatistics = new ClElementStatistics();
        clElementStatistics.setParticipantId(id);
        clElementStatistics.setControlLoopState(ControlLoopState.UNINITIALISED);
        clElementStatistics.setTimeStamp(Instant.now());

        clElement.setClElementStatistics(clElementStatistics);

        Map<UUID, ControlLoopElement> elementsOnThisParticipant = new LinkedHashMap<>();
        elementsOnThisParticipant.put(uuid, clElement);
        return elementsOnThisParticipant;
    }

}
