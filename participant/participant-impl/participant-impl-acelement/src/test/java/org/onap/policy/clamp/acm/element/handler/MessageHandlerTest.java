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

package org.onap.policy.clamp.acm.element.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.element.main.parameters.AcElement;
import org.onap.policy.clamp.acm.element.service.ElementService;
import org.onap.policy.clamp.models.acm.messages.dmaap.element.ElementStatus;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class MessageHandlerTest {

    private static final String NAME = "name";
    private static final String VERSION = "1.0.0";

    @Test
    void testAppliesTo() {
        var messageHandler = createMessageHandler(List.of());

        assertThat(messageHandler.appliesTo(new ToscaConceptIdentifier(NAME, "0.0.2"))).isFalse();
        assertThat(messageHandler.appliesTo(new ToscaConceptIdentifier("different", VERSION))).isFalse();
        assertThat(messageHandler.appliesTo(new ToscaConceptIdentifier(NAME, VERSION))).isTrue();
    }

    @Test
    void testStarter() {
        var starter = createMockElementService(ElementType.STARTER);
        var bridge = createMockElementService(ElementType.BRIDGE);
        var sink = createMockElementService(ElementType.SINK);
        var messageHandler = createMessageHandler(List.of(starter, bridge, sink));

        var elementConfig = new ElementConfig();
        elementConfig.setElementType(ElementType.STARTER);

        messageHandler.active(elementConfig);
        verify(starter).active(elementConfig);
        assertThat(messageHandler.getActiveService()).isEqualTo(starter);
        messageHandler.deactivateElement();
    }

    @Test
    void testBridge() {
        var starter = createMockElementService(ElementType.STARTER);
        var bridge = createMockElementService(ElementType.BRIDGE);
        var sink = createMockElementService(ElementType.SINK);
        var messageHandler = createMessageHandler(List.of(starter, bridge, sink));

        var elementConfig = new ElementConfig();
        elementConfig.setElementType(ElementType.BRIDGE);
        messageHandler.active(elementConfig);
        verify(bridge).active(elementConfig);
        assertThat(messageHandler.getActiveService()).isEqualTo(bridge);

        messageHandler.update(elementConfig);
        verify(bridge).update(elementConfig);

        var message = new ElementStatus();
        messageHandler.handleMessage(message);
        verify(bridge).handleMessage(message);
        messageHandler.deactivateElement();
    }

    @Test
    void testSink() {
        var starter = createMockElementService(ElementType.STARTER);
        var bridge = createMockElementService(ElementType.BRIDGE);
        var sink = createMockElementService(ElementType.SINK);
        var messageHandler = createMessageHandler(List.of(starter, bridge, sink));

        var elementConfig = new ElementConfig();
        elementConfig.setElementType(ElementType.SINK);
        messageHandler.active(elementConfig);
        verify(sink).active(elementConfig);
        assertThat(messageHandler.getActiveService()).isEqualTo(sink);

        var message = new ElementStatus();
        messageHandler.handleMessage(message);
        verify(sink).handleMessage(message);
        messageHandler.deactivateElement();
    }

    private MessageHandler createMessageHandler(List<ElementService> elementServices) {
        var acElement = new AcElement();
        acElement.setElementId(new ToscaConceptIdentifier(NAME, VERSION));
        return new MessageHandler(acElement, elementServices);
    }

    private ElementService createMockElementService(ElementType elementType) {
        var starter = mock(ElementService.class);
        when(starter.getType()).thenReturn(elementType);
        return starter;
    }
}
