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
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import org.apache.commons.io.DirectoryWalker.CancelException;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.participant.simulator.main.startstop.ParticipantSimulatorCommandLineArguments;
import org.onap.policy.common.utils.coder.CoderException;

/**
 * Class to perform unit test of {@link ParticipantParameterHandler}.
 */
public class TestParticipantSimulatorParameterHandler {

    @Test
    public void testParameterHandlerNoParameterFile() throws ControlLoopException {
        final String[] emptyArgumentString = { "-c", "src/test/resources/parameters/NoParametersFile.json" };

        final ParticipantSimulatorCommandLineArguments emptyArguments = new ParticipantSimulatorCommandLineArguments();
        emptyArguments.parse(emptyArgumentString);

        assertThatThrownBy(() -> new ParticipantSimulatorParameterHandler().getParameters(emptyArguments))
            .hasCauseInstanceOf(CoderException.class)
            .hasRootCauseInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void testParameterHandlerInvalidParameters() throws ControlLoopException {
        final String[] invalidArgumentString = { "-c", "src/test/resources/parameters/InvalidParameters.json" };

        final ParticipantSimulatorCommandLineArguments invalidArguments =
                new ParticipantSimulatorCommandLineArguments();
        invalidArguments.parse(invalidArgumentString);

        assertThatThrownBy(() -> new ParticipantSimulatorParameterHandler().getParameters(invalidArguments))
            .hasMessageStartingWith("error reading parameters from")
            .hasCauseInstanceOf(CoderException.class);
    }

    @Test
    public void testParameterHandlerNoParameters() throws CancelException, ControlLoopException {
        final String[] noArgumentString = { "-c", "src/test/resources/parameters/EmptyParameters.json" };

        final ParticipantSimulatorCommandLineArguments noArguments = new ParticipantSimulatorCommandLineArguments();
        noArguments.parse(noArgumentString);

        assertThatThrownBy(() -> new ParticipantSimulatorParameterHandler().getParameters(noArguments))
            .hasMessageContaining("no parameters found");
    }

    @Test
    public void testParticipantParameterGroup() throws ControlLoopException {
        final String[] participantConfigParameters = { "-c", "src/test/resources/parameters/TestParameters.json" };

        final ParticipantSimulatorCommandLineArguments arguments = new ParticipantSimulatorCommandLineArguments();
        arguments.parse(participantConfigParameters);

        final ParticipantSimulatorParameters parGroup = new ParticipantSimulatorParameterHandler()
                .getParameters(arguments);
        assertTrue(arguments.checkSetConfigurationFilePath());
        assertEquals(CommonTestData.PARTICIPANT_GROUP_NAME, parGroup.getName());
    }

    @Test
    public void testParticipantVersion() throws ControlLoopException {
        final String[] participantConfigParameters = { "-v" };
        final ParticipantSimulatorCommandLineArguments arguments = new ParticipantSimulatorCommandLineArguments();
        final String version = arguments.parse(participantConfigParameters);
        assertTrue(version.startsWith("ONAP Tosca defined control loop Participant"));
    }

    @Test
    public void testParticipantHelp() throws ControlLoopException {
        final String[] participantConfigParameters = { "-h" };
        final ParticipantSimulatorCommandLineArguments arguments = new ParticipantSimulatorCommandLineArguments();
        final String help = arguments.parse(participantConfigParameters);
        assertTrue(help.startsWith("usage:"));
    }

    @Test
    public void testParticipantInvalidOption() throws ControlLoopException {
        final String[] participantConfigParameters = { "-d" };
        final ParticipantSimulatorCommandLineArguments arguments = new ParticipantSimulatorCommandLineArguments();
        assertThatThrownBy(() -> arguments.parse(participantConfigParameters))
            .hasMessageStartingWith("invalid command line arguments specified");
    }
}
