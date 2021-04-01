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

package org.onap.policy.clamp.controlloop.participant.policy.main.parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import org.junit.Test;
import org.onap.policy.common.parameters.GroupValidationResult;

/**
 * Class to perform unit test of {@link ParticipantParameterGroup}.
 */
public class TestParticipantPolicyParameters {
    CommonTestData commonTestData = new CommonTestData();

    @Test
    public void testParticipantParameterGroup_Named() {
        final ParticipantPolicyParameters participantParameters = new ParticipantPolicyParameters("my-name");
        assertEquals("my-name", participantParameters.getName());
    }

    @Test
    public void testParticipantParameterGroup() {
        final ParticipantPolicyParameters participantParameters = commonTestData.toObject(
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME),
                ParticipantPolicyParameters.class);
        assertThat(participantParameters.validate().isValid()).isTrue();
    }

    @Test
    public void testParticipantParameterGroup_NullName() {
        final ParticipantPolicyParameters participantParameters = commonTestData
                .toObject(commonTestData.getParticipantParameterGroupMap(null),
                        ParticipantPolicyParameters.class);
        final GroupValidationResult validationResult = participantParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals(null, participantParameters.getName());
        assertThat(validationResult.getResult()).contains("is null");
    }

    @Test
    public void testParticipantParameterGroup_EmptyName() {
        final ParticipantPolicyParameters participantParameters = commonTestData
                .toObject(commonTestData.getParticipantParameterGroupMap(""),
                                ParticipantPolicyParameters.class);
        final GroupValidationResult validationResult = participantParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals("", participantParameters.getName());
        assertThat(validationResult.getResult()).contains(
                "field \"name\" type \"java.lang.String\" value \"\" INVALID, " + "must be a non-blank string");
    }

    @Test
    public void testParticipantParameterGroup_SetName() {
        final ParticipantPolicyParameters participantParameters = commonTestData.toObject(
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME),
                ParticipantPolicyParameters.class);
        participantParameters.setName("ParticipantNewGroup");
        assertThat(participantParameters.validate().isValid()).isTrue();
        assertEquals("ParticipantNewGroup", participantParameters.getName());
    }

    @Test
    public void testParticipantParameterGroup_EmptyParticipantIntermediaryParameters() {
        final Map<String, Object> map =
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME);
        map.replace("intermediaryParameters", commonTestData.getIntermediaryParametersMap(true));
        final ParticipantPolicyParameters participantParameters =
                commonTestData.toObject(map, ParticipantPolicyParameters.class);
        final GroupValidationResult validationResult = participantParameters.validate();
        assertFalse(validationResult.isValid());
        assertThat(validationResult.getResult()).contains(
                        "\"org.onap.policy.clamp.controlloop.participant.policy.main.parameters."
                        + "ParticipantPolicyParameters\""
                        + " INVALID, parameter group has status INVALID");
    }

    @Test
    public void testParticipantParameterGroupp_EmptyTopicParameters() {
        final Map<String, Object> map =
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME);
        final Map<String, Object> intermediaryParametersMap = commonTestData.getIntermediaryParametersMap(false);
        intermediaryParametersMap.put("clampControlLoopTopics", commonTestData.getTopicParametersMap(true));
        map.replace("intermediaryParameters", intermediaryParametersMap);

        final ParticipantPolicyParameters participantParameters =
                commonTestData.toObject(map, ParticipantPolicyParameters.class);
        final GroupValidationResult validationResult = participantParameters.validate();
        assertFalse(validationResult.isValid());
        assertThat(validationResult.getResult())
                .contains("\"org.onap.policy.common.endpoints.parameters.TopicParameterGroup\" INVALID, "
                        + "parameter group has status INVALID");
    }
}
