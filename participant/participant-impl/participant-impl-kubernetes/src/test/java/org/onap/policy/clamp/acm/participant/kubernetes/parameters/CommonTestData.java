/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.kubernetes.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.Topics;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class CommonTestData {

    public static final String PARTICIPANT_GROUP_NAME = "AutomationCompositionParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> SINK_TOPIC_PARAMS = List.of(getTopicParams());
    public static final List<TopicParameters> SOURCE_TOPIC_PARAMS = List.of(getTopicParams(), getSyncTopicParams());
    public static final Coder CODER = new StandardCoder();
    private static final UUID AC_ID = UUID.randomUUID();
    private static final String KEY_NAME =
            "org.onap.domain.database.HelloWorld_K8SMicroserviceAutomationCompositionElement";

    /**
     * Get ParticipantK8sParameters.
     *
     * @return ParticipantK8sParameters
     */
    public ParticipantK8sParameters getParticipantK8sParameters() {
        try {
            return CODER.convert(getParticipantK8sParametersMap(PARTICIPANT_GROUP_NAME),
                ParticipantK8sParameters.class);
        } catch (final CoderException e) {
            throw new RuntimeException("cannot create ParticipantK8sParameters from map", e);
        }
    }

    /**
     * Returns a property map for a ParticipantK8sParameters map for test cases.
     *
     * @param name name of the parameters
     *
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getParticipantK8sParametersMap(final String name) {
        final Map<String, Object> map = new TreeMap<>();

        map.put("name", name);
        map.put("intermediaryParameters", getIntermediaryParametersMap(false));
        map.put("localChartDirectory", getLocalChartDir());
        map.put("infoFileName", getInfoFileName());
        return map;
    }


    /**
     * Returns string value of local chart Directory.
     * @return a string value
     */
    public String getLocalChartDir() {
        return "/var/helm-manager/local-charts";
    }

    /**
     * Returns string value of Info file name.
     * @return string value
     */
    public String getInfoFileName() {
        return "CHART-INFO.json";
    }



    /**
     * Returns a property map for a intermediaryParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getIntermediaryParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("name", "Participant parameters");
            map.put("reportingTimeIntervalMs", TIME_INTERVAL);
            map.put("description", DESCRIPTION);
            map.put("participantId", getParticipantId());
            map.put("clampAutomationCompositionTopics", getTopicParametersMap(false));
            map.put("participantSupportedElementTypes", new ArrayList<>());
            map.put("topics", new Topics("policy-acruntime-participant", "acm-ppnt-sync"));
        }

        return map;
    }

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static UUID getParticipantId() {
        return UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c02");
    }

    /**
     * Returns a property map for a TopicParameters map for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getTopicParametersMap(final boolean isEmpty) {
        final Map<String, Object> map = new TreeMap<>();
        if (!isEmpty) {
            map.put("topicSources", SOURCE_TOPIC_PARAMS);
            map.put("topicSinks", SINK_TOPIC_PARAMS);
        }
        return map;
    }

    /**
     * Returns topic parameters for test cases.
     *
     * @return topic parameters
     */
    public static TopicParameters getTopicParams() {
        final TopicParameters topicParams = new TopicParameters();
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
    public static TopicParameters getSyncTopicParams() {
        final TopicParameters topicParams = new TopicParameters();
        topicParams.setTopic("acm-ppnt-sync");
        topicParams.setTopicCommInfrastructure("NOOP");
        topicParams.setServers(List.of("localhost"));
        return topicParams;
    }

    /**
     * Get automation composition id.
     * @return UUID automationCompositionId
     */
    public UUID getAutomationCompositionId() {
        return AC_ID;
    }

    /**
     * Create an AcElementDeploy.
     *
     * @return an AcElementDeploy
     */
    public static AcElementDeploy createAcElementDeploy() {
        var element = new AcElementDeploy();
        element.setId(UUID.randomUUID());
        element.setDefinition(new ToscaConceptIdentifier(KEY_NAME, "1.0.1"));
        element.setOrderedState(DeployOrder.DEPLOY);
        return element;
    }

    /**
     * Create an InstanceElementDto.
     *
     * @return an InstanceElementDto
     */
    public InstanceElementDto createInstanceElementDto(Map<String, Object> inProperties) {
        return new InstanceElementDto(getAutomationCompositionId(), UUID.randomUUID(), inProperties, new HashMap<>());
    }

    /**
     * Create an compositionElementDto.
     *
     * @return an compositionElementDto
     */
    public CompositionElementDto createCompositionElementDto() {
        return new CompositionElementDto(getAutomationCompositionId(), null,
                Map.of("uninitializedToPassiveTimeout", 100, "podStatusCheckInterval", "30"), null);
    }
}
