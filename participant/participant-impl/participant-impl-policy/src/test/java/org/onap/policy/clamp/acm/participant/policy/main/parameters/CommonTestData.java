/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.policy.main.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.Topics;
import org.onap.policy.common.parameters.topic.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * Class to hold/create all parameters for test cases.
 */
public class CommonTestData {
    public static final String PARTICIPANT_GROUP_NAME = "AutomationCompositionParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> SINK_TOPIC_PARAMS = List.of(getSinkTopicParams());
    public static final List<TopicParameters> SOURCE_TOPIC_PARAMS = List.of(getSinkTopicParams(), getSyncTopicParams());

    public static final Coder CODER = new StandardCoder();

    /**
     * Get ParticipantPolicyParameters.
     *
     * @return ParticipantPolicyParameters
     */
    public ParticipantPolicyParameters getParticipantPolicyParameters() {
        try {
            return CODER.convert(getParticipantPolicyParametersMap(PARTICIPANT_GROUP_NAME),
                    ParticipantPolicyParameters.class);
        } catch (final CoderException e) {
            throw new RuntimeException("cannot create ParticipantPolicyParameters from map", e);
        }
    }

    /**
     * Returns a property map for a ParticipantPolicyParameters map for test cases.
     *
     * @param name name of the parameters
     *
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getParticipantPolicyParametersMap(final String name) {
        final Map<String, Object> map = new TreeMap<>();

        map.put("name", name);
        map.put("intermediaryParameters", getIntermediaryParametersMap(false));
        map.put("policyApiParameters", getPolicyApiParametersMap());
        map.put("policyPapParameters", getPolicyPapParametersMap());
        map.put("pdpGroup", "defaultGroup");
        map.put("pdpType", "apex");
        return map;
    }

    /**
     * Returns a property map for a policyPapParameters map for test cases.
     *
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getPolicyPapParametersMap() {
        final Map<String, Object> map = new TreeMap<>();
        map.put("clientName", "pap");
        map.put("hostname", "localhost");
        map.put("port", 6968);
        map.put("userName", "policyadmin");
        map.put("password", "zb!XztG34");
        map.put("https", false);
        map.put("allowSelfSignedCerts", true);
        return map;
    }

    /**
     * Returns a property map for a policyApiParameters map for test cases.
     *
     * @return a property map suitable for constructing an object
     */
    public Map<String, Object> getPolicyApiParametersMap() {
        final Map<String, Object> map = new TreeMap<>();
        map.put("clientName", "api");
        map.put("hostname", "localhost");
        map.put("port", 6969);
        map.put("userName", "policyadmin");
        map.put("password", "zb!XztG34");
        map.put("https", false);
        map.put("allowSelfSignedCerts", true);

        return map;
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
    public static TopicParameters getSinkTopicParams() {
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
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static UUID getParticipantId() {
        return UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c03");
    }
}
