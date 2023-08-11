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

package org.onap.policy.clamp.acm.participant.sim.parameters;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.sim.comm.CommonTestData;

class ParticipantSimParametersTest {

    private final ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();

    @Test
    void testNotValidParticipantSimParameters() {
        var participantParameters = new ParticipantSimParameters();
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isNotEmpty();
    }

    @Test
    void testValidParticipantSimParameters() {
        var participantParameters = CommonTestData.getParticipantSimParameters();
        assertThat(validatorFactory.getValidator().validate(participantParameters)).isEmpty();
    }
}
