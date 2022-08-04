/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.service;

import org.onap.policy.clamp.acm.element.handler.MessagePublisher;
import org.onap.policy.clamp.acm.element.main.parameters.AcElement;
import org.onap.policy.clamp.models.acm.messages.dmaap.element.ElementMessage;
import org.onap.policy.clamp.models.acm.messages.dmaap.element.ElementStatus;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Service;

/**
 * Bridge Service.
 */
@Service
public class BridgeService extends AbstractElementService {

    private final MessagePublisher messagePublisher;
    private ToscaConceptIdentifier receiver;
    private ToscaConceptIdentifier elementId;

    public BridgeService(MessagePublisher messagePublisher, AcElement acElement) {
        this.messagePublisher = messagePublisher;
        this.elementId = acElement.getElementId();
    }

    @Override
    public ElementType getType() {
        return ElementType.BRIDGE;
    }

    @Override
    public void handleMessage(ElementMessage messageFrom) {
        var messageTo = new ElementStatus();
        messageTo.setElementId(receiver);
        // Add Tracking
        messageTo.setMessage(messageFrom.getMessage() + ", bridge: " + elementId);
        messagePublisher.publishMsg(messageTo);
    }

    @Override
    public void active(ElementConfig elementConfig) {
        receiver = elementConfig.getElementId();
    }
}
