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

package org.onap.policy.clamp.controlloop.common.startstop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;

public class CommonCommandLineArgumentsTest {

    public CommonCommandLineArguments cli;

    @Before
    public void setup() {
        cli = new CommonCommandLineArguments(new Options());
    }

    @Test
    public void testConstructor() {
        assertThat(cli).isNotNull();
    }

    @Test
    public void testHelp() {
        assertThat(cli.help("DummyClass", new Options()))
                .contains("DummyClass [options...]\noptions");
    }

    @Test
    public void testVersion() { assertThatCode(() -> cli.version()).doesNotThrowAnyException(); }

    @Test
    public void testValidateEmptyFileName() {
        assertThatThrownBy(() -> cli.validate(""))
            .hasMessageContaining("file was not specified as an argument");
    }

    @Test
    public void testValidateFileUrlNull() {
        assertThatThrownBy(() -> cli.validate("abcd"))
                .hasMessageContaining("does not exist");
    }

    @Test
    public void testValidateFileFound() {
        String configFile = "demo/config/RuntimeConfig.json";
        assertThatCode(() -> cli.validate(configFile)).doesNotThrowAnyException();
    }

    @Test
    public void testValidateNotNormalFile() {
        String badFile = "demo/config";
        assertThatThrownBy(() -> cli.validate(badFile)).hasMessageContaining("is not a normal file");
    }

}
