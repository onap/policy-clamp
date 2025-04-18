/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022,2024 Nordix Foundation.
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

import org.onap.policy.clamp.acm.element.handler.messages.ElementStatus;
import org.onap.policy.common.endpoints.listeners.ScoListener;
import org.onap.policy.common.message.bus.event.Topic.CommInfrastructure;
import org.onap.policy.common.utils.coder.StandardCoderObject;
import org.springframework.stereotype.Component;

@Component
public class MessageListener extends ScoListener<ElementStatus> {

    private final MessageHandler handler;

    public MessageListener(MessageHandler handler) {
        super(ElementStatus.class);
        this.handler = handler;
    }

    @Override
    public void onTopicEvent(CommInfrastructure infra, String topic, StandardCoderObject sco, ElementStatus message) {
        if (handler.appliesTo(message.getElementId())) {
            handler.handleMessage(message);
        }
    }
}
