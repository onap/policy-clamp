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

package org.onap.policy.clamp.controlloop.participant.simulator.main.parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.policy.common.parameters.GroupValidationResult;

/**
 * Class to perform unit test of {@link ParticipantParameterGroup}.
 */
public class TestParticipantSimulatorParameters {
    CommonTestData commonTestData = new CommonTestData();

    @Test
    public void testParticipantParameterGroup_Named() {
        final ParticipantSimulatorParameters participantParameters = new ParticipantSimulatorParameters("my-name");
        assertEquals("my-name", participantParameters.getName());
    }

    @Test
    public void testParticipantParameterGroup() {
        final ParticipantSimulatorParameters participantParameters = commonTestData.toObject(
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME),
                ParticipantSimulatorParameters.class);
        assertThat(participantParameters.validate().isValid()).isTrue();
        assertEquals(CommonTestData.PARTICIPANT_GROUP_NAME, participantParameters.getName());
    }

    @Test
    public void testParticipantParameterGroup_NullName() {
        final ParticipantSimulatorParameters participantParameters = commonTestData
                .toObject(commonTestData.getParticipantParameterGroupMap(null),
                        ParticipantSimulatorParameters.class);
        final GroupValidationResult validationResult = participantParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals(null, participantParameters.getName());
        assertTrue(validationResult.getResult().contains("is null"));
    }

    @Test
    public void testParticipantParameterGroup_EmptyName() {
        final ParticipantSimulatorParameters participantParameters = commonTestData
                .toObject(commonTestData.getParticipantParameterGroupMap(""),
                                ParticipantSimulatorParameters.class);
        final GroupValidationResult validationResult = participantParameters.validate();
        assertFalse(validationResult.isValid());
        assertEquals("", participantParameters.getName());
        assertTrue(validationResult.getResult().contains(
                "field \"name\" type \"java.lang.String\" value \"\" INVALID, " + "must be a non-blank string"));
    }

    @Test
    public void testParticipantParameterGroup_SetName() {
        final ParticipantSimulatorParameters participantParameters = commonTestData.toObject(
                commonTestData.getParticipantParameterGroupMap(CommonTestData.PARTICIPANT_GROUP_NAME),
                ParticipantSimulatorParameters.class);
        participantParameters.setName("ParticipantNewGroup");
        assertThat(participantParameters.validate().isValid()).isTrue();
        assertEquals("ParticipantNewGroup", participantParameters.getName());
    }
}
