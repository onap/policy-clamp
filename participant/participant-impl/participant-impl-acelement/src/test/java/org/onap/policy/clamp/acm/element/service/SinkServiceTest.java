/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.element.handler.messages.ElementMessage;
import org.onap.policy.clamp.acm.element.handler.messages.ElementMessageType;
import org.onap.policy.clamp.acm.element.main.concepts.ElementConfig;
import org.onap.policy.clamp.acm.element.main.parameters.AcElement;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class SinkServiceTest {

    @Test
    void testNotThrow() {
        var acElement = new AcElement();
        acElement.setElementId(new ToscaConceptIdentifier("onap.policy.clamp.ac.element1", "1.0.0"));
        var sinkService = new SinkService();
        var elementConfig = new ElementConfig();
        assertDoesNotThrow(() -> sinkService.active(elementConfig));
        var message = new ElementMessage(ElementMessageType.STATUS);
        assertDoesNotThrow(() -> sinkService.handleMessage(message));
    }
}
