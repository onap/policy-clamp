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

package org.onap.policy.clamp.controlloop.participant.simulator.main.intermediary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantControlLoopUpdate;
import org.onap.policy.clamp.controlloop.participant.intermediary.comm.ControlLoopUpdateListener;
import org.onap.policy.clamp.controlloop.participant.simulator.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.simulator.main.rest.TestListenerUtils;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.CoderException;

/**
 * Class to perform unit test of {@link ControlLoopUpdateListener}.
 */
public class TestControlLoopUpdateListener {
    private static ControlLoopUpdateListener clUpdateListener;
    private static final CommInfrastructure INFRA = CommInfrastructure.NOOP;
    private static final String TOPIC = "my-topic";
    static CommonTestData commonTestData = new CommonTestData();

    /**
     * Method for setup.
     *
     * @throws ParticipantException if some error occurs while starting up the participant
     * @throws FileNotFoundException if the file is missing
     * @throws IOException if IO exception occurs
     */
    @BeforeClass
    public static void setUp() throws ControlLoopException, FileNotFoundException, IOException {
        TestListenerUtils.initParticipantHandler();
        clUpdateListener = new ControlLoopUpdateListener(TestListenerUtils.getParticipantHandler());
    }

    @Test
    public void testControlLoopUpdateListener_ParticipantIdNoMatch() throws CoderException {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg = prepareMsg("DummyName");
        clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);

        // Verify the content in participantHandler
        assertNotEquals(participantControlLoopUpdateMsg.getParticipantId().getName(),
                TestListenerUtils.getParticipantHandler().getParticipantId().getName());
    }

    @Test
    public void testControlLoopUpdateListener() throws CoderException {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg = prepareMsg("org.onap.PM_CDS_Blueprint");
        clUpdateListener.onTopicEvent(INFRA, TOPIC, null, participantControlLoopUpdateMsg);

        // Verify the content in participantHandler
        assertEquals(TestListenerUtils.getParticipantHandler().getParticipantId(),
                participantControlLoopUpdateMsg.getParticipantId());
        assertThat(TestListenerUtils.getParticipantHandler().getControlLoopHandler().getControlLoops()
                .getControlLoopList()).hasSize(1);
    }

    private ParticipantControlLoopUpdate prepareMsg(final String participantName) {
        ParticipantControlLoopUpdate participantControlLoopUpdateMsg;
        participantControlLoopUpdateMsg = TestListenerUtils.createControlLoopUpdateMsg();
        participantControlLoopUpdateMsg.getParticipantId().setName(participantName);
        participantControlLoopUpdateMsg.getControlLoop().setOrderedState(ControlLoopOrderedState.PASSIVE);
        return participantControlLoopUpdateMsg;
    }
}
