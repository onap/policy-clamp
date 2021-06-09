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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import org.apache.commons.io.DirectoryWalker.CancelException;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.common.utils.coder.CoderException;

/**
 * Class to perform unit test of {@link ParticipantParameterHandler}.
 */
class TestParticipantSimulatorParameterHandler {

    @Test
    void testParameterHandlerNoParameterFile() throws ControlLoopException {
        final String path = "src/test/resources/parameters/NoParametersFile.json";

        assertThatThrownBy(() -> new ParticipantSimulatorParameterHandler().toParticipantSimulatorParameters(path))
            .hasCauseInstanceOf(CoderException.class)
            .hasRootCauseInstanceOf(FileNotFoundException.class);
    }

    @Test
    void testParameterHandlerInvalidParameters() throws ControlLoopException {
        final String path = "src/test/resources/parameters/InvalidParameters.json";

        assertThatThrownBy(() -> new ParticipantSimulatorParameterHandler().toParticipantSimulatorParameters(path))
            .hasMessageStartingWith("error reading parameters from")
            .hasCauseInstanceOf(CoderException.class);
    }

    @Test
    void testParameterHandlerNoParameters() throws CancelException, ControlLoopException {
        final String path = "src/test/resources/parameters/EmptyParameters.json";

        assertThatThrownBy(() -> new ParticipantSimulatorParameterHandler().toParticipantSimulatorParameters(path))
            .hasMessageContaining("no parameters found");
    }

    @Test
    void testParticipantParameterGroup() throws ControlLoopException {
        final String path = "src/test/resources/parameters/TestParameters.json";

        final ParticipantSimulatorParameters parGroup = new ParticipantSimulatorParameterHandler()
                .toParticipantSimulatorParameters(path);
        assertEquals(CommonTestData.PARTICIPANT_GROUP_NAME, parGroup.getName());
    }
}
