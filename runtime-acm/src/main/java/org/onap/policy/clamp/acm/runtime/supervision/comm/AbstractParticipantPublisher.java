/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.supervision.comm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.policy.clamp.common.acm.utils.NetLoggerUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractParticipantPublisher<T> {

    @Value("${runtime.topics.operationTopic}")
    private String operationTopic;

    @Value("${runtime.topics.syncTopic}")
    private String syncTopic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendToSyncTopic(String key, final T message) {
        this.send(syncTopic, key, message);
    }

    public void sendToSyncTopic(final T message) {
        this.send(syncTopic, null, message);
    }

    public void send(final T message) {
        this.send(operationTopic, null, message);
    }

    private void send(final String topic, String key, final T message) {
        NetLoggerUtil.log(NetLoggerUtil.EventType.OUT, "KAFKA", topic, message.toString());
        try {
            if (key == null) {
                kafkaTemplate.send(topic, message).join();
            } else {
                kafkaTemplate.send(topic, key, message).join();
            }
        } catch (final Exception e) {
            log.warn("send to {} failed because of {}", topic, e.getMessage(), e);
        }
    }
}
