/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.http.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.participant.http.main.handler.ControlLoopElementHandler;
import org.onap.policy.clamp.controlloop.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.controlloop.participant.http.utils.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.http.utils.ToscaUtils;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
class ClElementHandlerTest {

    @InjectMocks
    @Spy
    private ControlLoopElementHandler controlLoopElementHandler = new ControlLoopElementHandler();

    @Mock
    private ParticipantIntermediaryApi participantIntermediaryApi;

    private CommonTestData commonTestData = new CommonTestData();

    private static ToscaServiceTemplate serviceTemplate;
    private static final String HTTP_CONTROL_LOOP_ELEMENT =
            "org.onap.domain.database.Http_PMSHMicroserviceControlLoopElement";

    @BeforeAll
    static void init() throws CoderException {
        serviceTemplate = ToscaUtils.readControlLoopFromTosca();
    }

    @Test
    void test_controlLoopElementeStateChange() throws IOException {
        var controlLoopId = commonTestData.getControlLoopId();
        var element = commonTestData.getControlLoopElement();
        var controlLoopElementId = element.getId();

        var config = Mockito.mock(ConfigRequest.class);
        assertDoesNotThrow(() -> controlLoopElementHandler.invokeHttpClient(config));

        assertDoesNotThrow(() -> controlLoopElementHandler
                .controlLoopElementStateChange(controlLoopId,
                        controlLoopElementId, ControlLoopState.PASSIVE, ControlLoopOrderedState.PASSIVE));

        assertDoesNotThrow(() -> controlLoopElementHandler
                .controlLoopElementStateChange(controlLoopId,
                        controlLoopElementId, ControlLoopState.PASSIVE, ControlLoopOrderedState.UNINITIALISED));

        assertDoesNotThrow(() -> controlLoopElementHandler
                .controlLoopElementStateChange(controlLoopId,
                        controlLoopElementId, ControlLoopState.PASSIVE, ControlLoopOrderedState.RUNNING));

        controlLoopElementHandler.close();
    }

    @Test
    void test_ControlLoopElementUpdate() throws ExecutionException, InterruptedException {
        doNothing().when(controlLoopElementHandler).invokeHttpClient(any());
        ControlLoopElement element = commonTestData.getControlLoopElement();

        Map<String, ToscaNodeTemplate> nodeTemplatesMap =
            serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();

        assertDoesNotThrow(() -> controlLoopElementHandler
            .controlLoopElementUpdate(commonTestData.getControlLoopId(), element,
                nodeTemplatesMap.get(HTTP_CONTROL_LOOP_ELEMENT)));
    }
}
