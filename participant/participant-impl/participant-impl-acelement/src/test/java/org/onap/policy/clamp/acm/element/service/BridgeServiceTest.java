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

package org.onap.policy.clamp.acm.element.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.element.handler.MessagePublisher;
import org.onap.policy.clamp.acm.element.main.parameters.AcElement;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.kafka.element.ElementMessage;
import org.onap.policy.clamp.models.acm.messages.kafka.element.ElementStatus;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class BridgeServiceTest {

    @Test
    void testHandleMessage() {
        var acElement = new AcElement();
        acElement.setElementId(new ToscaConceptIdentifier("onap.policy.clamp.ac.element1", "1.0.0"));

        var messagePublisher = mock(MessagePublisher.class);
        var bridgeService = new BridgeService(messagePublisher, acElement);

        assertThat(bridgeService.getType()).isEqualTo(ElementType.BRIDGE);

        var elementConfig = new ElementConfig();
        elementConfig.setReceiverId(new ToscaConceptIdentifier("onap.policy.clamp.ac.element2", "1.0.0"));
        bridgeService.active(elementConfig);

        bridgeService.handleMessage(new ElementStatus());
        verify(messagePublisher).publishMsg(any(ElementMessage.class));
    }

    @Test
    void testWrongConf() {
        var acElement = new AcElement();
        acElement.setElementId(new ToscaConceptIdentifier("onap.policy.clamp.ac.element1", "1.0.0"));

        var messagePublisher = new MessagePublisher();
        var bridgeService = new BridgeService(messagePublisher, acElement);
        var elementStatus = new ElementStatus();
        assertThatThrownBy(() -> bridgeService.handleMessage(elementStatus))
                .isInstanceOf(AutomationCompositionRuntimeException.class);
    }
}
