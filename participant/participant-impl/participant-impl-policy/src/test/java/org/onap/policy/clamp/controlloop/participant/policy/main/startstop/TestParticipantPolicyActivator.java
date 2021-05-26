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

package org.onap.policy.clamp.controlloop.participant.policy.main.startstop;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.ParticipantPolicyParameterHandler;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.common.utils.services.Registry;

public class TestParticipantPolicyActivator {

    private static ParticipantPolicyActivator activator;

    /**
     * Initializes an activator.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUp() throws Exception {
        Registry.newRegistry();
        final String[] participantConfigParameters = { "-c", "src/test/resources/parameters/TestParameters.json"};
        final ParticipantPolicyCommandLineArguments arguments =
                new ParticipantPolicyCommandLineArguments(participantConfigParameters);
        final ParticipantPolicyParameters parGroup =
                new ParticipantPolicyParameterHandler().getParameters(arguments);
        activator = new ParticipantPolicyActivator(parGroup);
    }

    /**
     * Method for cleanup after each test.
     *
     * @throws Exception if an error occurs
     */
    @AfterClass
    public static void teardown() throws Exception {
        // shut down activator
        if (activator != null && activator.isAlive()) {
            activator.shutdown();
        }
    }

    @Test
    public void testParticipantActivator() {
        activator.start();
        assertTrue(activator.isAlive());
        assertTrue(activator.getParameters().isValid());
        assertEquals(CommonTestData.PARTICIPANT_GROUP_NAME, activator.getParameters().getName());

        // repeat - should throw an exception
        assertThatIllegalStateException().isThrownBy(() -> activator.start());
        assertTrue(activator.isAlive());
        assertTrue(activator.getParameters().isValid());

        activator.shutdown();
        assertFalse(activator.isAlive());

        // repeat - should throw an exception
        assertThatIllegalStateException().isThrownBy(() -> activator.shutdown());
        assertFalse(activator.isAlive());
    }
}
