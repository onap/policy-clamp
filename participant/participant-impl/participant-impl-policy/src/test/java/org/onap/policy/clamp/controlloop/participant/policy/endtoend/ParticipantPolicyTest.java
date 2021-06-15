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

package org.onap.policy.clamp.controlloop.participant.policy.endtoend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopStateChangeListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopUpdateListener;
import org.onap.policy.clamp.controlloop.participant.policy.main.utils.TestListenerUtils;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = {"classpath:application_test.properties"})
class ParticipantPolicyTest {

    private static final Object lockit = new Object();
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";

    @Autowired
    private ParticipantIntermediaryApi participantIntermediaryApi;

    @Test
    void testUpdatePolicyTypes() {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        participantControlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);

        // Verify that the ToscaServicetemplate has policy_types
        assertNotNull(participantControlLoopUpdateMsg.getControlLoopDefinition().getPolicyTypes());

        synchronized (lockit) {
            ControlLoopUpdateListener clUpdateListener =
                    new ControlLoopUpdateListener(participantIntermediaryApi.getParticipantHandler());

            clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);
        }
        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy",
                participantIntermediaryApi.getParticipantHandler().getParticipantId().getName());
    }

    @Test
    void testUpdatePolicies() throws Exception {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        participantControlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);

        // Add policies to the toscaServiceTemplate
        TestListenerUtils.addPoliciesToToscaServiceTemplate(participantControlLoopUpdateMsg.getControlLoopDefinition());

        // Verify that the ToscaServicetemplate has policies
        assertNotNull(
                participantControlLoopUpdateMsg.getControlLoopDefinition().getToscaTopologyTemplate().getPolicies());

        synchronized (lockit) {
            ControlLoopUpdateListener clUpdateListener =
                    new ControlLoopUpdateListener(participantIntermediaryApi.getParticipantHandler());

            clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);
        }
        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy",
                participantIntermediaryApi.getParticipantHandler().getParticipantId().getName());
    }

    @Test
    void testDeletePoliciesAndPolicyTypes() throws Exception {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        participantControlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);

        // Add policies to the toscaServiceTemplate
        TestListenerUtils.addPoliciesToToscaServiceTemplate(participantControlLoopUpdateMsg.getControlLoopDefinition());

        // Verify that the ToscaServicetemplate has policies
        assertNotNull(
                participantControlLoopUpdateMsg.getControlLoopDefinition().getToscaTopologyTemplate().getPolicies());

        synchronized (lockit) {
            ControlLoopUpdateListener clUpdateListener =
                    new ControlLoopUpdateListener(participantIntermediaryApi.getParticipantHandler());

            clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);
        }
        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy",
                participantIntermediaryApi.getParticipantHandler().getParticipantId().getName());

        ControlLoopStateChangeListener clStateChangeListener =
                new ControlLoopStateChangeListener(participantIntermediaryApi.getParticipantHandler());
        ParticipantControlLoopStateChange participantControlLoopStateChangeMsg =
                TestListenerUtils.createControlLoopStateChangeMsg(ControlLoopOrderedState.UNINITIALISED);
        participantControlLoopStateChangeMsg.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        clStateChangeListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopStateChangeMsg);

        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy",
                participantIntermediaryApi.getParticipantHandler().getParticipantId().getName());
    }
}
