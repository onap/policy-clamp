/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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
import org.onap.policy.clamp.acm.participant.sim.parameters.ParticipantSimParameters;
import org.onap.policy.common.endpoints.parameters.TopicParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

public class CommonTestData {
    public static final Coder CODER = new StandardCoder();
    public static final String DESCRIPTION = "Participant description";
    public static final long TIME_INTERVAL = 2000;
    public static final List<TopicParameters> TOPIC_PARAMS = List.of(getTopicParams());

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

        return map;
    }

    /**
     * Returns a property map for a TopicParameters map for test cases.
     *
     * @return a property map suitable for constructing an object
     */
    private static Map<String, Object> getTopicParametersMap() {
        final Map<String, Object> map = new TreeMap<>();
        map.put("topicSources", TOPIC_PARAMS);
        map.put("topicSinks", TOPIC_PARAMS);
        return map;
    }

    /**
     * Returns topic parameters for test cases.
     *
     * @return topic parameters
     */
    private static TopicParameters getTopicParams() {
        final TopicParameters topicParams = new TopicParameters();
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
        return UUID.fromString("101c62b3-8918-41b9-a747-d21eb79c6c01");
    }
}
