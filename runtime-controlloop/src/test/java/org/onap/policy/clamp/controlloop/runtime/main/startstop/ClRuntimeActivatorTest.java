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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterHandler;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.common.utils.services.Registry;

/**
 * Class to perform unit test of {@link ClRuntimeActivator}}.
 *
 */
class ClRuntimeActivatorTest {

    @Test
    void testStartAndStop() throws Exception {
        Registry.newRegistry();
        final String path = "src/test/resources/parameters/TestParameters.json";
        ClRuntimeParameterGroup parameterGroup = new ClRuntimeParameterHandler().getParameters(path);
        var supervisionHandler = Mockito.mock(SupervisionHandler.class);

        try (var activator = new ClRuntimeActivator(parameterGroup, supervisionHandler)) {

            assertFalse(activator.isAlive());
            activator.start();
            assertTrue(activator.isAlive());
            assertTrue(activator.getParameterGroup().isValid());

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
}
