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

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;

/**
 * Class to perform unit test of {@link ParticipantDcaeParameters}.
 * It will be tested the "javax.validation.constraints"
 *
 */
class ParticipantDcaeParametersTest {
    private CommonTestData commonTestData = new CommonTestData();
    private ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

    @Test
    void testParticipantDcaeParameters() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isEmpty();
    }

    @Test
    void testZeroCheckCount() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.setCheckCount(0);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoClampClientEndPoints() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.setClampClientEndPoints(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoClampClientEndPointCreate() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.getClampClientEndPoints().setCreate(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoClampClientEndPointDelete() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.getClampClientEndPoints().setDelete(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoClampClientEndPointDeploy() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.getClampClientEndPoints().setDeploy(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoClampClientEndPointStatus() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.getClampClientEndPoints().setStatus(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoClampClientEndPointStop() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.getClampClientEndPoints().setStop(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoClampClientEndPointUndeploy() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.getClampClientEndPoints().setUndeploy(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoClampClientParameters() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.setClampClientParameters(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoHostname() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.getClampClientParameters().setHostname(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoTopicSinks() throws ControlLoopException {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();

        ParticipantIntermediaryParameters intermediaryParameters = participantParameters.getIntermediaryParameters();
        intermediaryParameters.getClampControlLoopTopics().setTopicSinks(null);

        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoTopicSources() throws ControlLoopException {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();

        ParticipantIntermediaryParameters intermediaryParameters = participantParameters.getIntermediaryParameters();
        intermediaryParameters.getClampControlLoopTopics().setTopicSources(null);

        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoConsulClientParameters() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.setConsulClientParameters(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoConsulHostname() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.getConsulClientParameters().setHostname(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoIntermediaryParameters() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.setIntermediaryParameters(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testNoParticipantId() {
        final ParticipantDcaeParameters participantParameters = commonTestData.getParticipantDcaeParameters();
        participantParameters.getIntermediaryParameters().setParticipantId(null);
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }
}
