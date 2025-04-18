/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.sim.comm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.Topics;
import org.onap.policy.clamp.acm.participant.sim.model.SimConfig;
import org.onap.policy.clamp.acm.participant.sim.parameters.ParticipantSimParameters;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

public class CommonTestData {
    public static final Coder CODER = new StandardCoder();
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> SINK_TOPIC_PARAMS = List.of(getSinkTopicParams());
    public static final List<TopicParameters> SOURCE_TOPIC_PARAMS = List.of(getSinkTopicParams(), getSyncTopicParams());

    /**
     * Get ParticipantSimParameters.
     *
     * @return ParticipantSimParameters
     */
    public static ParticipantSimParameters getParticipantSimParameters() {
        try {
            return CODER.convert(getParticipantSimParametersMap(), ParticipantSimParameters.class);
        } catch (final CoderException e) {
            throw new RuntimeException("cannot create ParticipantSimParameters from map", e);
        }
    }

    /**
     * Returns a property map for a ParticipantSimParameters map for test cases.
     *
     * @return a property map suitable for constructing an object
     */
    private static Map<String, Object> getParticipantSimParametersMap() {
        final Map<String, Object> map = new TreeMap<>();

        map.put("intermediaryParameters", getIntermediaryParametersMap());
        return map;
    }

    /**
     * Returns a property map for a intermediaryParameters map for test cases.
     *
     * @return a property map suitable for constructing an object
     */
    private static Map<String, Object> getIntermediaryParametersMap() {
        final Map<String, Object> map = new TreeMap<>();
        map.put("name", "Participant parameters");
        map.put("reportingTimeIntervalMs", TIME_INTERVAL);
        map.put("description", DESCRIPTION);
        map.put("participantId", getParticipantId());
        map.put("clampAutomationCompositionTopics", getTopicParametersMap());
        map.put("participantSupportedElementTypes", new ArrayList<>());
        map.put("topics", new Topics("policy-acruntime-participant", "acm-ppnt-sync"));

        return map;
    }

    /**
     * Returns a property map for a TopicParameters map for test cases.
     *
     * @return a property map suitable for constructing an object
     */
    private static Map<String, Object> getTopicParametersMap() {
        final Map<String, Object> map = new TreeMap<>();
        map.put("topicSources", SOURCE_TOPIC_PARAMS);
        map.put("topicSinks", SINK_TOPIC_PARAMS);
        return map;
    }

    /**
     * Returns topic parameters for test cases.
     *
     * @return topic parameters
     */
    private static TopicParameters getSinkTopicParams() {
        final var topicParams = new TopicParameters();
        topicParams.setTopic("policy-acruntime-participant");
        topicParams.setTopicCommInfrastructure("NOOP");
        topicParams.setServers(List.of("localhost"));
        return topicParams;
    }

    /**
     * Returns sync topic parameters for test cases.
     *
     * @return topic parameters
     */
    private static TopicParameters getSyncTopicParams() {
        final var topicParams = new TopicParameters();
        topicParams.setTopic("acm-ppnt-sync");
        topicParams.setTopicCommInfrastructure("NOOP");
        topicParams.setServers(List.of("localhost"));
        return topicParams;
    }

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static UUID getParticipantId() {
        return UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c01");
    }

    /**
     * Returns a Map of ToscaConceptIdentifier and AutomationComposition for test cases.
     *
     * @return automationCompositionMap
     */
    public static Map<UUID, AutomationComposition> getTestAutomationCompositionMap() {
        var automationComposition = getTestAutomationComposition();
        return Map.of(automationComposition.getInstanceId(), automationComposition);
    }

    /**
     * Returns List of AutomationComposition for test cases.
     *
     * @return AutomationCompositions
     */
    public static AutomationComposition getTestAutomationComposition() {
        var automationComposition = new AutomationComposition();
        automationComposition.setInstanceId(UUID.randomUUID());
        var element = new AutomationCompositionElement();
        element.setId(UUID.randomUUID());
        automationComposition.setElements(Map.of(element.getId(), element));
        return automationComposition;
    }

    /**
     * Create a new SimConfig.
     *
     * @return a new SimConfig
     */
    public static SimConfig createSimConfig() {
        var config = new SimConfig();
        config.setPrepareTimerMs(1);
        config.setDeployTimerMs(1);
        config.setReviewTimerMs(1);
        config.setUndeployTimerMs(1);
        config.setLockTimerMs(1);
        config.setUnlockTimerMs(1);
        config.setUpdateTimerMs(1);
        config.setDeleteTimerMs(1);
        config.setPrimeTimerMs(1);
        config.setDeprimeTimerMs(1);
        config.setMigrateTimerMs(1);
        config.setMigratePrecheckTimerMs(1);
        return config;
    }
}
