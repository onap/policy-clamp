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

package org.onap.policy.clamp.acm.participant.policy.main.parameters;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.ParticipantPolicyParameters;

class ParticipantPolicyParametersTest {
    private final CommonTestData commonTestData = new CommonTestData();
    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

    @Test
    void testParticipantPolicyParameters() {
        final ParticipantPolicyParameters participantParameters = commonTestData.getParticipantPolicyParameters();
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isEmpty();
    }

    @Test
    void testParticipantPolicyParameters_NullTopicSinks() {
        final ParticipantPolicyParameters participantParameters = commonTestData.getParticipantPolicyParameters();
        participantParameters.getIntermediaryParameters().getClampAutomationCompositionTopics().setTopicSinks(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testParticipantPolicyParameters_NullTopicSources() {
        final ParticipantPolicyParameters participantParameters = commonTestData.getParticipantPolicyParameters();
        participantParameters.getIntermediaryParameters().getClampAutomationCompositionTopics().setTopicSources(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testParticipantPolicyParameters_NullPolicyApiParameters() {
        final ParticipantPolicyParameters participantParameters = commonTestData.getParticipantPolicyParameters();
        participantParameters.setPolicyApiParameters(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testParticipantPolicyParameters_NullHostname() {
        final ParticipantPolicyParameters participantParameters = commonTestData.getParticipantPolicyParameters();
        participantParameters.getPolicyApiParameters().setHostname(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }
}
