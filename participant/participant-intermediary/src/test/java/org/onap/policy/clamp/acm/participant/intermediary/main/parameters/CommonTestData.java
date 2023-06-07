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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.handler.DummyParticipantParameters;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.AutomationCompositionStateChange;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.LockOrder;
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
    public static final UUID AC_ID_0 = UUID.randomUUID();
    public static final UUID AC_ID_1 = UUID.randomUUID();
    public static final UUID PARTCICIPANT_ID = UUID.randomUUID();

    /**
     * Get ParticipantIntermediaryParameters.
     *
     * @return ParticipantIntermediaryParameters
     */
    public static ParticipantIntermediaryParameters getParticipantIntermediaryParameters() {
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
        map.put("reportingTimeIntervalMs", TIME_INTERVAL);
        map.put("clampAutomationCompositionTopics", getTopicParametersMap(false));
        var supportedElementType = new ParticipantSupportedElementType();
        supportedElementType.setTypeName("org.onap.policy.clamp.acm.HttpAutomationCompositionElement");
        supportedElementType.setTypeVersion("1.0.0");
        map.put("participantSupportedElementTypes", List.of(supportedElementType));

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
    public static UUID getParticipantId() {
        return PARTCICIPANT_ID;
    }

    public static UUID getRndParticipantId() {
        return UUID.randomUUID();
    }

    public static ToscaConceptIdentifier getDefinition() {
        return new ToscaConceptIdentifier("org.onap.domain.pmsh.PMSH_DCAEMicroservice", "1.2.3");
    }

    /**
     * Returns a Map of ToscaConceptIdentifier and AutomationComposition for test cases.
     *
     * @return automationCompositionMap
     *
     * @throws CoderException if there is an error with .json file.
     */
    public static Map<UUID, AutomationComposition> getTestAutomationCompositionMap() {
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
    public static AutomationCompositions getTestAutomationCompositions() {
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
     * Return a AutomationCompositionStateChange.
     *
     * @param participantId the participantId
     * @param instanceId th AutomationComposition Id
     * @param deployOrder a DeployOrder
     * @param lockOrder a LockOrder
     * @return a AutomationCompositionStateChange
     */
    public static AutomationCompositionStateChange getStateChange(UUID participantId, UUID instanceId,
            DeployOrder deployOrder, LockOrder lockOrder) {
        var stateChange = new AutomationCompositionStateChange();
        stateChange.setStartPhase(0);
        stateChange.setAutomationCompositionId(instanceId);
        stateChange.setParticipantId(participantId);
        stateChange.setMessageId(UUID.randomUUID());
        stateChange.setDeployOrderedState(deployOrder);
        stateChange.setLockOrderedState(lockOrder);
        stateChange.setTimestamp(Instant.ofEpochMilli(3000));
        return stateChange;
    }
}
