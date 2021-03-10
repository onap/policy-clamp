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

package org.onap.policy.clamp.controlloop.participant.simulator.main.startstop;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.common.ControlLoopConstants;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.common.utils.resources.MessageConstants;

/**
 * Class to perform unit test of {@link Main}}.
 */
public class TestMain {

    @Test
    public void testMain_Help() {
        final String[] configParameters = {"-h"};
        Main main = new Main(configParameters);
        assertFalse(main.isRunning());
    }

    @Test
    public void testMain_Version() {
        final String[] configParameters = {"-v"};
        Main main = new Main(configParameters);
        assertFalse(main.isRunning());
    }

    @Test
    public void testMain_Valid() {
        final String[] configParameters = {"-c", "src/test/resources/parameters/TestParameters.json"};
        Main main = new Main(configParameters);
        assertTrue(main.isRunning());

        assertThatCode(() -> main.shutdown()).doesNotThrowAnyException();

        assertFalse(main.isRunning());
    }

    @Test
    public void testMain_NoParameter() {
        assertThatConfigParameterThrownException(new String[] {});
    }

    @Test
    public void testMain_FilePathNotDefined() {
        assertThatConfigParameterThrownException(new String[] {"-c"});
    }

    @Test
    public void testMain_TooManyCommand() {
        assertThatConfigParameterThrownException(new String[] {"-h", "d"});
    }

    @Test
    public void testMain_WrongParameter() {
        assertThatConfigParameterThrownException(new String[] {"-d"});
    }

    private void assertThatConfigParameterThrownException(final String[] configParameters) {
        assertThatThrownBy(() -> Main.main(configParameters)).isInstanceOf(ControlLoopRuntimeException.class)
                .hasMessage(String.format(MessageConstants.START_FAILURE_MSG, MessageConstants.POLICY_CLAMP));
    }

    @Test
    public void testParticipant_NoFileWithThisName() {
        assertThatConfigFileThrownException("src/test/resources/parameters/NoFileWithThisName.json");
    }

    @Test
    public void testParticipant_NotValidFile() {
        assertThatConfigFileThrownException("src/test/resources/parameters");
    }

    @Test
    public void testParticipant_FileEmpty() {
        assertThatConfigFileThrownException("src/test/resources/parameters/EmptyParameters.json");
    }

    @Test
    public void testParticipant_NoParameters() {
        assertThatConfigFileThrownException("src/test/resources/parameters/NoParameters.json");
    }

    @Test
    public void testParticipant_InvalidParameters() {
        assertThatConfigFileThrownException("src/test/resources/parameters/InvalidParameters.json");
    }

    private void assertThatConfigFileThrownException(final String configFilePath) {
        final String[] configParameters = new String[] {"-c", configFilePath};
        assertThatThrownBy(() -> new Main(configParameters)).isInstanceOf(ControlLoopRuntimeException.class)
                .hasMessage(String.format(MessageConstants.START_FAILURE_MSG, MessageConstants.POLICY_CLAMP));
    }
}
