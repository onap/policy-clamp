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

package org.onap.policy.clamp.controlloop.runtime.main.startstop;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterHandler;
import org.onap.policy.common.utils.services.Registry;

/**
 * Class to perform unit test of {@link ClRuntimeActivator}}.
 *
 */
public class ClRuntimeActivatorTest {

    @Test
    public void testStartAndStop() throws Exception {
        Registry.newRegistry();
        final String[] configParameters = {"-c", "src/test/resources/parameters/TestParameters.json"};
        final ClRuntimeCommandLineArguments arguments = new ClRuntimeCommandLineArguments();
        arguments.parse(configParameters);
        ClRuntimeParameterGroup parameterGroup = new ClRuntimeParameterHandler().getParameters(arguments);
        ClRuntimeActivator activator = new ClRuntimeActivator(parameterGroup);
        activator.isAlive();

        assertFalse(activator.isAlive());
        activator.start();
        assertTrue(activator.isAlive());
        assertTrue(activator.getParameterGroup().isValid());
        assertEquals(activator.getParameterGroup().getName(),
                activator.getParameterGroup().getRestServerParameters().getName());

        // repeat start - should throw an exception
        assertThatIllegalStateException().isThrownBy(() -> activator.start());
        assertTrue(activator.isAlive());
        assertTrue(activator.getParameterGroup().isValid());

        activator.stop();
        assertFalse(activator.isAlive());

        // repeat stop - should throw an exception
        assertThatIllegalStateException().isThrownBy(() -> activator.stop());
        assertFalse(activator.isAlive());
    }
}
