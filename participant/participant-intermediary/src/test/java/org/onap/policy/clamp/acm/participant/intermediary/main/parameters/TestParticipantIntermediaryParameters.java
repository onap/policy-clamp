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

package org.onap.policy.clamp.acm.participant.intermediary.main.parameters;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantIntermediaryParameters;

/**
 * Class to perform unit test of {@link ParticipantParameterGroup}.
 */
class TestParticipantIntermediaryParameters {
    private CommonTestData commonTestData = new CommonTestData();
    private ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

    @Test
    void testParticipantIntermediaryParameterGroup() {
        final ParticipantIntermediaryParameters participantParameters =
                commonTestData.getParticipantIntermediaryParameters();
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isEmpty();
    }

    @Test
    void testParticipantIntermediaryParameterGroup_EmptyParameter() {
        final ParticipantIntermediaryParameters participantParameters =
                commonTestData.getParticipantIntermediaryParameters();
        participantParameters.setClampAutomationCompositionTopics(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testParticipantIntermediaryParameters_NullTopicSinks() {
        final ParticipantIntermediaryParameters participantParameters =
                commonTestData.getParticipantIntermediaryParameters();
        participantParameters.getClampAutomationCompositionTopics().setTopicSinks(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testParticipantIntermediaryParameters_NullTopicSources() {
        final ParticipantIntermediaryParameters participantParameters =
                commonTestData.getParticipantIntermediaryParameters();
        participantParameters.getClampAutomationCompositionTopics().setTopicSources(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }
}
