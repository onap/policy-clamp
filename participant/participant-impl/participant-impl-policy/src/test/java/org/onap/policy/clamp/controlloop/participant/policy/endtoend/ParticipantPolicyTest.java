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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopStateChange;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ControlLoopUpdate;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopStateChangeListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopUpdateListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.handler.ParticipantHandler;
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
    private ParticipantHandler participantHandler;

    @Test
    void testUpdatePolicyTypes() {
        ControlLoopUpdate controlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        controlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);

        synchronized (lockit) {
            ControlLoopUpdateListener clUpdateListener = new ControlLoopUpdateListener(participantHandler);

            clUpdateListener.onTopicEvent(INFRA, TOPIC, null, controlLoopUpdateMsg);
        }
        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy", participantHandler.getParticipantId().getName());
    }

    @Test
    void testUpdatePolicies() throws Exception {
        ControlLoopUpdate controlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        controlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);

        synchronized (lockit) {
            ControlLoopUpdateListener clUpdateListener = new ControlLoopUpdateListener(participantHandler);

            clUpdateListener.onTopicEvent(INFRA, TOPIC, null, controlLoopUpdateMsg);
        }
        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy", participantHandler.getParticipantId().getName());
    }

    @Test
    void testDeletePoliciesAndPolicyTypes() throws Exception {
        ControlLoopUpdate controlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        controlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);

        synchronized (lockit) {
            ControlLoopUpdateListener clUpdateListener = new ControlLoopUpdateListener(participantHandler);

            clUpdateListener.onTopicEvent(INFRA, TOPIC, null, controlLoopUpdateMsg);
        }
        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy", participantHandler.getParticipantId().getName());

        ControlLoopStateChangeListener clStateChangeListener = new ControlLoopStateChangeListener(participantHandler);
        ControlLoopStateChange controlLoopStateChangeMsg =
                TestListenerUtils.createControlLoopStateChangeMsg(ControlLoopOrderedState.UNINITIALISED);
        controlLoopStateChangeMsg.setOrderedState(ControlLoopOrderedState.UNINITIALISED);
        clStateChangeListener.onTopicEvent(INFRA, TOPIC, null, controlLoopStateChangeMsg);

        // Verify the result of GET participants with what is stored
        assertEquals("org.onap.PM_Policy", participantHandler.getParticipantId().getName());
    }
}
