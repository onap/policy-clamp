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

package org.onap.policy.clamp.controlloop.participant.policy.main.parameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import org.apache.commons.io.DirectoryWalker.CancelException;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.participant.policy.main.startstop.ParticipantPolicyCommandLineArguments;
import org.onap.policy.common.utils.coder.CoderException;

public class TestParticipantPolicyParameterHandler {

    @Test
    public void testParameterHandlerNoParameterFile() throws ControlLoopException {
        final String[] emptyArgumentString = { "-c", "src/test/resources/parameters/NoParametersFile.json" };

        final ParticipantPolicyCommandLineArguments emptyArguments = new ParticipantPolicyCommandLineArguments();
        emptyArguments.parse(emptyArgumentString);

        assertThatThrownBy(() -> new ParticipantPolicyParameterHandler().getParameters(emptyArguments))
            .hasCauseInstanceOf(CoderException.class)
            .hasRootCauseInstanceOf(FileNotFoundException.class);
    }

    @Test
    public void testParameterHandlerInvalidParameters() throws ControlLoopException {
        final String[] invalidArgumentString = { "-c", "src/test/resources/parameters/InvalidParameters.json" };

        final ParticipantPolicyCommandLineArguments invalidArguments =
                new ParticipantPolicyCommandLineArguments();
        invalidArguments.parse(invalidArgumentString);

        assertThatThrownBy(() -> new ParticipantPolicyParameterHandler().getParameters(invalidArguments))
            .hasMessageStartingWith("error reading parameters from")
            .hasCauseInstanceOf(CoderException.class);
    }

    @Test
    public void testParticipantPolicyParameters() throws ControlLoopException {
        final String[] participantConfigParameters = { "-c", "src/test/resources/parameters/TestParameters.json" };

        final ParticipantPolicyCommandLineArguments arguments = new ParticipantPolicyCommandLineArguments();
        arguments.parse(participantConfigParameters);

        final ParticipantPolicyParameters parGroup = new ParticipantPolicyParameterHandler()
                .getParameters(arguments);
        assertTrue(arguments.checkSetConfigurationFilePath());
        assertEquals(CommonTestData.PARTICIPANT_GROUP_NAME, parGroup.getName());
    }

    @Test
    public void testParticipantVersion() throws ControlLoopException {
        final String[] participantConfigParameters = { "-v" };
        final ParticipantPolicyCommandLineArguments arguments = new ParticipantPolicyCommandLineArguments();
        final String version = arguments.parse(participantConfigParameters);
        assertThat(arguments.parse(participantConfigParameters)).startsWith(
                        "ONAP Tosca defined control loop Participant");
    }

    @Test
    public void testParticipantHelp() throws ControlLoopException {
        final String[] participantConfigParameters = { "-h" };
        final ParticipantPolicyCommandLineArguments arguments = new ParticipantPolicyCommandLineArguments();
        assertThat(arguments.parse(participantConfigParameters)).startsWith("usage:");
    }

    @Test
    public void testParticipantInvalidOption() throws ControlLoopException {
        final String[] participantConfigParameters = { "-d" };
        final ParticipantPolicyCommandLineArguments arguments = new ParticipantPolicyCommandLineArguments();
        assertThatThrownBy(() -> arguments.parse(participantConfigParameters))
            .hasMessageStartingWith("invalid command line arguments specified");
    }
}
