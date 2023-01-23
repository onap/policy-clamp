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

package org.onap.policy.clamp.acm.participant.policy.main.parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
            map.put("participantType", getParticipantType());
            map.put("clampAutomationCompositionTopics", getTopicParametersMap(false));
            map.put("participantSupportedElementTypes", new ArrayList<>());
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
        final TopicParameters topicParams = new TopicParameters();
        topicParams.setTopic("POLICY-ACRUNTIME-PARTICIPANT");
        topicParams.setTopicCommInfrastructure("dmaap");
        topicParams.setServers(List.of("localhost"));
        return topicParams;
    }

    /**
     * Returns participantType for test cases.
     *
     * @return participant Type
     */
    public static ToscaConceptIdentifier getParticipantType() {
        return new ToscaConceptIdentifier("org.onap.PM_Policy", "0.0.0");
    }


    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static ToscaConceptIdentifier getParticipantId() {
        return new ToscaConceptIdentifier("org.onap.PM_Policy", "1.0.0");
    }
}
