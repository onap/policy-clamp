/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.element.handler.messages.ElementMessage;
import org.onap.policy.clamp.acm.element.handler.messages.ElementMessageType;
import org.onap.policy.common.message.bus.event.TopicSink;


class MessagePublisherTest {

    @Test
    void testActiveEmpty() {
        var messagePublisher = new MessagePublisher();
        var list = List.<TopicSink>of();
        assertThatThrownBy(() -> messagePublisher.active(list))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testPublishMsg() {
        var topic = mock(TopicSink.class);
        var messagePublisher = new MessagePublisher();
        messagePublisher.active(List.of(topic));
        messagePublisher.publishMsg(new ElementMessage(ElementMessageType.STATUS));
        messagePublisher.stop();
        verify(topic).send(any());
    }
}
