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

import io.micrometer.core.annotation.Timed;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.acm.element.handler.messages.ElementMessage;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.common.acm.utils.NetLoggerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePublisher {
    private static final String NOT_ACTIVE_TEXT = "Not Active!";

    @Value("${element.publisherTopic}")
    private String topic;

    private boolean active = false;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Active MessagePublisher.
     */
    public void active() {
        active = true;
    }

    /**
     * Method to send message.
     *
     * @param message the acknowledgement message
     */
    @Timed(value = "publisher.status", description = "STATUS messages published")
    public void publishMsg(final ElementMessage message) {
        if (!active) {
            throw new AutomationCompositionRuntimeException(Response.Status.CONFLICT, NOT_ACTIVE_TEXT);
        }
        NetLoggerUtil.log(NetLoggerUtil.EventType.OUT, "KAFKA", topic, message.toString());
        try {
            kafkaTemplate.send(topic, message).join();
        } catch (final Exception e) {
            log.warn("send to {} failed because of {}", topic, e.getMessage(), e);
        }
    }

    public void stop() {
        active = false;
    }
}
