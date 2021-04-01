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

package org.onap.policy.clamp.controlloop.participant.policy.main.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.common.ControlLoopConstants;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopUpdateListener;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.policy.main.startstop.Main;
import org.onap.policy.clamp.controlloop.participant.policy.main.startstop.ParticipantPolicyActivator;
import org.onap.policy.clamp.controlloop.participant.policy.main.utils.TestListenerUtils;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.utils.services.Registry;

/**
 * Class to perform unit test of {@link TestPolicyHandler}.
 */
public class TestPolicyHandler {

    private static ControlLoopUpdateListener clUpdateListener;
    private static ParticipantControlLoopUpdate participantControlLoopUpdateMsg;
    private static final String PARTICIPANTS_ENDPOINT = "participants";
    private static final String ELEMENTS_ENDPOINT = "elements";
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";
    static CommonTestData commonTestData = new CommonTestData();

    /**
     * Setup before class, instantiate Main.
     *
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Registry.newRegistry();
        final String[] configParameters = {"-c", "src/test/resources/parameters/TestParameters.json"};
        Main main = new Main(configParameters);
        assertTrue(main.isRunning());
        TestListenerUtils.initParticipantHandler();

        clUpdateListener = new ControlLoopUpdateListener(
                PolicyHandler.getInstance()
                .getPolicyProvider()
                .getIntermediaryApi()
                .getParticipantHandler());
        participantControlLoopUpdateMsg =
                TestListenerUtils.createControlLoopUpdateMsg("src/test/resources/utils/servicetemplates");
        participantControlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);
    }

    @Test
    public void testUpdatePolicies() throws Exception {
        // Add policy_types to the toscaServiceTemplate
        TestListenerUtils.addPolicyTypesToToscaServiceTemplate(
                        participantControlLoopUpdateMsg.getControlLoopDefinition());

        // Add policies to the toscaServiceTemplate
        TestListenerUtils.addPoliciesToToscaServiceTemplate(participantControlLoopUpdateMsg.getControlLoopDefinition());

        // Verify that the ToscaServicetemplate has policy_types
        assertNotNull(participantControlLoopUpdateMsg.getControlLoopDefinition().getPolicyTypes());

        // Verify that the ToscaServicetemplate has policies
        assertNotNull(participantControlLoopUpdateMsg.getControlLoopDefinition()
                .getToscaTopologyTemplate().getPolicies());

        clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);

        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy",
                TestListenerUtils.getParticipantHandler().getParticipantId().getName());
    }
}
