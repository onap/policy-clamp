/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024,2026 OpenInfra Foundation Europe. All rights reserved.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.element.handler.messages.ElementMessage;
import org.onap.policy.clamp.acm.element.handler.messages.ElementMessageType;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.springframework.kafka.core.KafkaTemplate;

class MessagePublisherTest {

    @Test
    void testActiveEmpty() {
        var messagePublisher = new MessagePublisher(mock());
        var msg = new ElementMessage(ElementMessageType.ACK_MSG);
        assertThatThrownBy(() -> messagePublisher.publishMsg(msg))
                .isInstanceOf(AutomationCompositionRuntimeException.class);
    }

    @Test
    void testPublishMsg() {
        var kafkaTemplate = mock(KafkaTemplate.class);
        var messagePublisher = new MessagePublisher(kafkaTemplate);
        messagePublisher.active();
        var msg = new ElementMessage(ElementMessageType.ACK_MSG);
        messagePublisher.publishMsg(msg);
        verify(kafkaTemplate).send(null, msg);
        messagePublisher.stop();
        assertThatThrownBy(() -> messagePublisher.publishMsg(msg))
                .isInstanceOf(AutomationCompositionRuntimeException.class);
    }
}
