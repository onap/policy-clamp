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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.element.handler.MessageActivator;
import org.onap.policy.clamp.acm.element.handler.MessageHandler;
import org.onap.policy.clamp.models.acm.messages.rest.element.DmaapConfig;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.common.endpoints.parameters.TopicParameterGroup;

class ConfigServiceTest {

    @Test
    void test() {
        var elementConfig = new ElementConfig();
        elementConfig.setTopicParameterGroup(new DmaapConfig());
        elementConfig.getTopicParameterGroup().setTopicCommInfrastructure("dmaap");
        elementConfig.getTopicParameterGroup().setTopic("topic");
        elementConfig.getTopicParameterGroup().setServer("localhost");
        elementConfig.getTopicParameterGroup().setFetchTimeout(1000);

        var handler = mock(MessageHandler.class);
        var messageActivator = mock(MessageActivator.class);
        var configService = new ConfigService(handler, messageActivator);
        configService.activateElement(elementConfig);

        verify(messageActivator).activate(any(TopicParameterGroup.class));
        verify(handler).active(elementConfig);

        assertThat(configService.getElementConfig()).isEqualTo(elementConfig);

        configService.deleteConfig();
        verify(messageActivator).deactivate();
        verify(handler).deactivateElement();

        assertThat(configService.getElementConfig()).isNotEqualTo(elementConfig);
    }
}
