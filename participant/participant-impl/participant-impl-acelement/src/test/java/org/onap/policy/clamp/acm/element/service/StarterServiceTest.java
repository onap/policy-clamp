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
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.element.handler.MessagePublisher;
import org.onap.policy.clamp.acm.element.main.parameters.AcElement;
import org.onap.policy.clamp.models.acm.messages.dmaap.element.ElementMessage;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class StarterServiceTest {

    @Test
    void testActive() throws Exception {
        var acElement = new AcElement();
        acElement.setElementId(new ToscaConceptIdentifier("onap.policy.clamp.ac.element1", "1.0.0"));

        var messagePublisher = mock(MessagePublisher.class);
        try (var starterService = new StarterService(messagePublisher, acElement)) {

            assertThat(starterService.getType()).isEqualTo(ElementType.STARTER);

            var elementConfig = new ElementConfig();
            elementConfig.setTimerSec(100);
            elementConfig.setElementId(new ToscaConceptIdentifier("onap.policy.clamp.ac.element2", "1.0.0"));
            starterService.active(elementConfig);
            verify(messagePublisher, timeout(200).atLeastOnce()).publishMsg(any(ElementMessage.class));
            starterService.deactivate();
        }
    }

}
