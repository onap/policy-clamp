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

package org.onap.policy.clamp.controlloop.participant.dcae.main.parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;
import org.onap.policy.common.parameters.ValidationResult;

/**
 * Class to perform unit test of {@link ParticipantParameterGroup}.
 *
 */
class TestParticipantDcaeParameters {
    CommonTestData commonTestData = new CommonTestData();

    @Test
    void testParticipantParameterGroup_Named() {
        final ParticipantDcaeParameters participantParameters = new ParticipantDcaeParameters("my-name");
        assertEquals("my-name", participantParameters.getName());
    }

    @Test
    void testParticipantParameterGroup() {
        final ParticipantDcaeParameters participantParameters = commonTestData.toObject(
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME),
                ParticipantDcaeParameters.class);
        final ParticipantIntermediaryParameters participantIntermediaryParameters = participantParameters
                .getIntermediaryParameters();
        final TopicParameterGroup topicParameterGroup  = participantParameters.getIntermediaryParameters()
                .getClampControlLoopTopics();
        final ValidationResult validationResult = participantParameters.validate();
        assertTrue(validationResult.isValid());
        assertEquals(CommonTestData.PARTICIPANT_GROUP_NAME, participantParameters.getName());
        assertEquals(CommonTestData.TIME_INTERVAL, participantIntermediaryParameters.getReportingTimeInterval());
        assertEquals(CommonTestData.DESCRIPTION, participantIntermediaryParameters.getDescription());
        assertEquals(CommonTestData.TOPIC_PARAMS, topicParameterGroup.getTopicSinks());
        assertEquals(CommonTestData.TOPIC_PARAMS, topicParameterGroup.getTopicSources());
    }

    @Test
    void testParticipantParameterGroup_EmptyParticipantIntermediaryParameters() {
        final Map<String, Object> map =
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME);
        map.replace("intermediaryParameters", commonTestData.getIntermediaryParametersMap(true));
        final ParticipantDcaeParameters participantParameters =
                commonTestData.toObject(map, ParticipantDcaeParameters.class);
        final ValidationResult validationResult = participantParameters.validate();
        assertNull(validationResult.getResult());
    }

    @Test
    void testParticipantParameterGroup_EmptyTopicParameters() {
        final Map<String, Object> map =
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME);
        final Map<String, Object> intermediaryParametersMap = commonTestData.getIntermediaryParametersMap(false);
        intermediaryParametersMap.put("clampControlLoopTopics", commonTestData.getTopicParametersMap(true));
        map.replace("intermediaryParameters", intermediaryParametersMap);

        final ParticipantDcaeParameters participantParameters =
                commonTestData.toObject(map, ParticipantDcaeParameters.class);
        final ValidationResult validationResult = participantParameters.validate();
        assertNull(validationResult.getResult());
    }
}
