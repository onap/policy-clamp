/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class CommonTestData {

    public static final String PARTICIPANT_GROUP_NAME = "AutomationCompositionParticipantGroup";
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> TOPIC_PARAMS = List.of(getTopicParams());
    public static final Coder CODER = new StandardCoder();
    private static final UUID AC_ID = UUID.randomUUID();

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
            map.put("participantType", getParticipantType());
            map.put("clampAutomationCompositionTopics", getTopicParametersMap(false));
            map.put("participantSupportedElementTypes", new ArrayList<>());
        }

        return map;
    }

    /**
     * Returns participantType for test cases.
     *
     * @return participant Type
     */
    public static ToscaConceptIdentifier getParticipantType() {
        final var participantId = new ToscaConceptIdentifier();
        participantId.setName("K8sParticipant0");
        participantId.setVersion("1.0.0");
        return participantId;
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
     * Get automation composition id.
     * @return UUID automationCompositionId
     */
    public UUID getAutomationCompositionId() {
        return AC_ID;
    }
}
