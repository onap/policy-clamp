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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ParticipantDcaeParameterHandler;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ParticipantDcaeParameters;
import org.onap.policy.clamp.controlloop.participant.dcae.main.startstop.ParticipantDcaeCommandLineArguments;
import org.onap.policy.common.utils.coder.CoderException;

/**
 * Class to perform unit test of {@link ParticipantParameterHandler}.
 *
 */
public class TestParticipantDcaeParameterHandler {

    @Test
    public void testParameterHandlerNoParameterFile() throws ControlLoopException {
        final String[] emptyArgumentString = { "-c", "src/test/resources/parameters/NoParametersFile.json" };

        final ParticipantDcaeCommandLineArguments emptyArguments = new ParticipantDcaeCommandLineArguments();
        emptyArguments.parse(emptyArgumentString);

        assertThatThrownBy(() -> new ParticipantDcaeParameterHandler().getParameters(emptyArguments))
            .hasCauseInstanceOf(CoderException.class)
            .hasRootCauseInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void testParameterHandlerInvalidParameters() throws ControlLoopException {
        final String[] invalidArgumentString = { "-c", "src/test/resources/parameters/InvalidParameters.json" };

        final ParticipantDcaeCommandLineArguments invalidArguments =
                new ParticipantDcaeCommandLineArguments();
        invalidArguments.parse(invalidArgumentString);

        assertThatThrownBy(() -> new ParticipantDcaeParameterHandler().getParameters(invalidArguments))
            .hasMessageStartingWith("error reading parameters from")
            .hasCauseInstanceOf(CoderException.class);
    }

    @Test
    public void testParticipantParameterGroup() throws ControlLoopException {
        final String[] participantConfigParameters = { "-c", "src/test/resources/parameters/TestParameters.json" };

        final ParticipantDcaeCommandLineArguments arguments = new ParticipantDcaeCommandLineArguments();
        arguments.parse(participantConfigParameters);

        final ParticipantDcaeParameters parGroup = new ParticipantDcaeParameterHandler()
                .getParameters(arguments);
        assertTrue(arguments.checkSetConfigurationFilePath());
        assertEquals(CommonTestData.PARTICIPANT_GROUP_NAME, parGroup.getName());
    }

    @Test
    public void testParticipantVersion() throws ControlLoopException {
        final String[] participantConfigParameters = { "-v" };
        final ParticipantDcaeCommandLineArguments arguments = new ParticipantDcaeCommandLineArguments();
        assertThat(arguments.parse(participantConfigParameters)).startsWith(
                "ONAP Tosca defined control loop Participant");
    }

    @Test
    public void testParticipantHelp() throws ControlLoopException {
        final String[] participantConfigParameters = { "-h" };
        final ParticipantDcaeCommandLineArguments arguments = new ParticipantDcaeCommandLineArguments();
        assertThat(arguments.parse(participantConfigParameters)).startsWith("usage:");
    }

    @Test
    public void testParticipantInvalidOption() throws ControlLoopException {
        final String[] participantConfigParameters = { "-d" };
        final ParticipantDcaeCommandLineArguments arguments = new ParticipantDcaeCommandLineArguments();
        assertThatThrownBy(() -> arguments.parse(participantConfigParameters))
            .hasMessageStartingWith("invalid command line arguments specified");
    }
}
