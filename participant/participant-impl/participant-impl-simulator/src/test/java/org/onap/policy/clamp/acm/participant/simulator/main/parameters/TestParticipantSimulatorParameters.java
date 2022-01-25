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

package org.onap.policy.clamp.acm.participant.simulator.main.parameters;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

/**
 * Class to perform unit test of {@link ParticipantParameterGroup}.
 */
class TestParticipantSimulatorParameters {
    private CommonTestData commonTestData = new CommonTestData();
    private ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

    @Test
    void testParticipantParameterGroup() {
        final ParticipantSimulatorParameters participantParameters = commonTestData.getParticipantSimulatorParameters();
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isEmpty();
    }

    @Test
    void testParticipantParameterGroup_EmptyParticipantIntermediaryParameters() {
        final ParticipantSimulatorParameters participantParameters = commonTestData.getParticipantSimulatorParameters();
        participantParameters.setIntermediaryParameters(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testParticipantPolicyParameters_NullTopicSinks() {
        final ParticipantSimulatorParameters participantParameters = commonTestData.getParticipantSimulatorParameters();
        participantParameters.getIntermediaryParameters().getClampAutomationCompositionTopics().setTopicSinks(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testParticipantPolicyParameters_NullTopicSources() {
        final ParticipantSimulatorParameters participantParameters = commonTestData.getParticipantSimulatorParameters();
        participantParameters.getIntermediaryParameters().getClampAutomationCompositionTopics().setTopicSources(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }
}
