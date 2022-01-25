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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.comm.ParticipantStatusReqListener;
import org.onap.policy.clamp.acm.participant.intermediary.main.parameters.CommonTestData;
import org.onap.policy.clamp.acm.participant.intermediary.parameters.ParticipantParameters;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantStatusReq;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;

class IntermediaryActivatorTest {
    private static final Coder CODER = new StandardCoder();

    private static final String TOPIC_FIRST = "TOPIC1";
    private static final String TOPIC_SECOND = "TOPIC2";

    @Test
    void testStartAndStop() throws Exception {
        ParticipantParameters parameters = CommonTestData.getParticipantParameters();

        var publisherFirst = spy(mock(Publisher.class));
        var publisherSecond = spy(mock(Publisher.class));
        var publishers = List.of(publisherFirst, publisherSecond);

        var listenerFirst = spy(mock(ParticipantStatusReqListener.class));
        when(listenerFirst.getType()).thenReturn(TOPIC_FIRST);
        when(listenerFirst.getScoListener()).thenReturn(listenerFirst);

        var listenerSecond = spy(mock(ParticipantStatusReqListener.class));
        when(listenerSecond.getType()).thenReturn(TOPIC_SECOND);
        when(listenerSecond.getScoListener()).thenReturn(listenerSecond);

        List<Listener<ParticipantStatusReq>> listeners = List.of(listenerFirst, listenerSecond);

        ParticipantHandler handler = mock(ParticipantHandler.class);
        try (var activator = new IntermediaryActivator(parameters, handler, publishers, listeners)) {

            assertFalse(activator.isAlive());
            activator.start();
            assertTrue(activator.isAlive());

            // repeat start - should throw an exception
            assertThatIllegalStateException().isThrownBy(() -> activator.start());
            assertTrue(activator.isAlive());
            verify(publisherFirst, times(1)).active(anyList());
            verify(publisherSecond, times(1)).active(anyList());

            StandardCoderObject sco = CODER.decode("{messageType:" + TOPIC_FIRST + "}", StandardCoderObject.class);
            activator.getMsgDispatcher().onTopicEvent(null, "msg", sco);
            verify(listenerFirst, times(1)).onTopicEvent(any(), any(), any());

            sco = CODER.decode("{messageType:" + TOPIC_SECOND + "}", StandardCoderObject.class);
            activator.getMsgDispatcher().onTopicEvent(null, "msg", sco);
            verify(listenerSecond, times(1)).onTopicEvent(any(), any(), any());

            activator.close();
            assertFalse(activator.isAlive());

            // repeat stop - should throw an exception
            assertThatIllegalStateException().isThrownBy(() -> activator.stop());
            assertFalse(activator.isAlive());

            assertDoesNotThrow(() -> activator.handleContextRefreshEvent(mock(ContextRefreshedEvent.class)));
            assertDoesNotThrow(() -> activator.handleContextClosedEvent(mock(ContextClosedEvent.class)));
        }
    }
}
