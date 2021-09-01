/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.kubernetes.parameters;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class CommonTestData {

    public static final String PARTICIPANT_GROUP_NAME = "ControlLoopParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> TOPIC_PARAMS = Arrays.asList(getTopicParams());
    public static final Coder CODER = new StandardCoder();


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
            map.put("participantType", getParticipantId());
            map.put("clampControlLoopTopics", getTopicParametersMap(false));
        }

        return map;
    }

    /**
     * Returns participantId for test cases.
     *
     * @return participant Id
     */
    public static ToscaConceptIdentifier getParticipantId() {
        final ToscaConceptIdentifier participantId = new ToscaConceptIdentifier();
        participantId.setName("K8sParticipant0");
        participantId.setVersion("1.0.0");
        return participantId;
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
        topicParams.setTopic("POLICY-CLRUNTIME-PARTICIPANT");
        topicParams.setTopicCommInfrastructure("dmaap");
        topicParams.setServers(Arrays.asList("localhost"));
        return topicParams;
    }
}
