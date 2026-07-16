/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.element.handler.MessageHandler;
import org.onap.policy.clamp.acm.element.main.concepts.ElementConfig;
import org.onap.policy.clamp.acm.element.main.concepts.ElementType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ConfigServiceTest {

    @Test
    void test() {
        var elementConfig = new ElementConfig();
        elementConfig.setElementType(ElementType.BRIDGE);
        elementConfig.setReceiverId(new ToscaConceptIdentifier("name", "1.0.0"));
        elementConfig.setTimerMs(100);

        var handler = mock(MessageHandler.class);
        var configService = new ConfigService(handler);
        configService.activateElement(elementConfig);

        verify(handler).active(elementConfig);

        assertThat(configService.getElementConfig()).isEqualTo(elementConfig);

        configService.deleteConfig();
        verify(handler).deactivateElement();

        assertThat(configService.getElementConfig()).isNotEqualTo(elementConfig);
    }
}
