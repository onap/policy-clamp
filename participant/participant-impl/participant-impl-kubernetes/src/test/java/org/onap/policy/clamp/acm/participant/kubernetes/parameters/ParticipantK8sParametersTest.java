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

package org.onap.policy.clamp.acm.participant.kubernetes.parameters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

class ParticipantK8sParametersTest {

    private CommonTestData commonTestData = new CommonTestData();
    private ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

    @Test
    void testParticipantPolicyParameters() {
        final ParticipantK8sParameters participantParameters = commonTestData.getParticipantK8sParameters();
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNullOrEmpty();
    }

    @Test
    void testParticipantK8sParameters_NullTopicSinks() {
        final ParticipantK8sParameters participantParameters = commonTestData.getParticipantK8sParameters();
        participantParameters.getIntermediaryParameters().getClampAutomationCompositionTopics().setTopicSinks(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testParticipantK8sParameters_NullTopicSources() {
        final ParticipantK8sParameters participantParameters = commonTestData.getParticipantK8sParameters();
        participantParameters.getIntermediaryParameters().getClampAutomationCompositionTopics().setTopicSources(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testParticipantK8sParameters_BlankLocalChartDirParameter() {
        final ParticipantK8sParameters participantParameters = commonTestData.getParticipantK8sParameters();
        participantParameters.setLocalChartDirectory(" ");
        Set<ConstraintViolation<ParticipantK8sParameters>> violations = validatorFactory.getValidator()
            .validate(participantParameters);
        assertThat(violations.size()).isEqualTo(1);
    }

    @Test
    void testParticipantK8sParameters_BlankInfoFileParameter() {
        final ParticipantK8sParameters participantParameters = commonTestData.getParticipantK8sParameters();
        participantParameters.setInfoFileName("");
        Set<ConstraintViolation<ParticipantK8sParameters>> violations = validatorFactory.getValidator()
            .validate(participantParameters);
        assertThat(violations.size()).isEqualTo(1);
    }

    @Test
    void testNoIntermediaryParameters() {
        final ParticipantK8sParameters participantParameters = commonTestData.getParticipantK8sParameters();
        participantParameters.setIntermediaryParameters(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoParticipantId() {
        final ParticipantK8sParameters participantParameters = commonTestData.getParticipantK8sParameters();
        participantParameters.getIntermediaryParameters().setParticipantId(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

}
