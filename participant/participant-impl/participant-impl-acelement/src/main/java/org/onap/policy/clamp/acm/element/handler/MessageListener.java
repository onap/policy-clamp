/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022,2024,2026 OpenInfra Foundation Europe. All rights reserved.
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.acm.element.handler.messages.ElementMessage;
import org.onap.policy.clamp.common.acm.utils.NetLoggerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@KafkaListener(topics = "${element.listenerTopic}")
@RequiredArgsConstructor
public class MessageListener {

    private final MessageHandler handler;

    @Value("${element.listenerTopic}")
    private String topic;

    /**
     * Handle ParticipantRegister messages.
     *
     * @param message the message
     */
    @KafkaHandler
    public void onTopicEvent(final ElementMessage message) {
        if (handler.appliesTo(message.getElementId())) {
            NetLoggerUtil.log(NetLoggerUtil.EventType.IN, "KAFKA", topic, message.toString());
            handler.handleMessage(message);
        }
    }
}
