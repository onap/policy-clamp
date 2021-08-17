/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T
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

package org.onap.policy.clamp.controlloop.participant.dcae.main.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.participant.dcae.httpclient.ClampHttpClient;
import org.onap.policy.clamp.controlloop.participant.dcae.httpclient.ConsulDcaeHttpClient;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.CommonTestData;
import org.onap.policy.clamp.controlloop.participant.dcae.model.Loop;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Class to perform unit test of {@link ControlLoopElementHandler}.
 *
 */
@ExtendWith(SpringExtension.class)
class ControlLoopElementHandlerTest {

    private static final String LOOP = "pmsh_loop";
    private static final String TEMPLATE = "LOOP_TEMPLATE_k8s_pmsh";
    private static final String BLUEPRINT_DEPLOYED = "BLUEPRINT_DEPLOYED";
    private static final String MICROSERVICE_INSTALLED_SUCCESSFULLY = "MICROSERVICE_INSTALLED_SUCCESSFULLY";

    public static final Coder CODER = new StandardCoder();
    private CommonTestData commonTestData = new CommonTestData();

    @Test
    void test_ControlLoopElementStateChange() {
        ClampHttpClient clampClient = spy(mock(ClampHttpClient.class));
        ConsulDcaeHttpClient consulClient = mock(ConsulDcaeHttpClient.class);
        ControlLoopElementHandler controlLoopElementHandler =
                new ControlLoopElementHandler(clampClient, consulClient, commonTestData.getParticipantDcaeParameters());

        when(clampClient.getstatus(LOOP)).thenReturn(new Loop());

        ParticipantIntermediaryApi intermediaryApi = mock(ParticipantIntermediaryApi.class);
        controlLoopElementHandler.setIntermediaryApi(intermediaryApi);

        UUID controlLoopElementId = UUID.randomUUID();
        controlLoopElementHandler.controlLoopElementStateChange(controlLoopElementId, ControlLoopState.PASSIVE,
                ControlLoopOrderedState.UNINITIALISED);

        verify(clampClient).undeploy(LOOP);
        controlLoopElementHandler.handleStatistics(controlLoopElementId);
        assertThat(intermediaryApi.getControlLoopElement(controlLoopElementId)).isNull();
    }

    @Test
    void testCreate_ControlLoopElementUpdate() throws PfModelException, JSONException, CoderException {
        ClampHttpClient clampClient = spy(mock(ClampHttpClient.class));
        Loop loopDeployed = CODER.convert(CommonTestData.createJsonStatus(BLUEPRINT_DEPLOYED), Loop.class);
        when(clampClient.create(LOOP, TEMPLATE)).thenReturn(loopDeployed);
        when(clampClient.deploy(LOOP)).thenReturn(true);

        Loop loopInstalled =
                CODER.convert(CommonTestData.createJsonStatus(MICROSERVICE_INSTALLED_SUCCESSFULLY), Loop.class);
        when(clampClient.getstatus(LOOP)).thenReturn(null, loopInstalled);

        ConsulDcaeHttpClient consulClient = spy(mock(ConsulDcaeHttpClient.class));
        when(consulClient.deploy(any(String.class), any(String.class))).thenReturn(true);

        ControlLoopElementHandler controlLoopElementHandler =
                new ControlLoopElementHandler(clampClient, consulClient, commonTestData.getParticipantDcaeParameters());

        ParticipantIntermediaryApi intermediaryApi = mock(ParticipantIntermediaryApi.class);
        controlLoopElementHandler.setIntermediaryApi(intermediaryApi);

        ControlLoopElement element = new ControlLoopElement();
        element.setId(UUID.randomUUID());
        element.setOrderedState(ControlLoopOrderedState.PASSIVE);

        final ToscaServiceTemplate controlLoopDefinition = new ToscaServiceTemplate();
        controlLoopElementHandler.controlLoopElementUpdate(element, controlLoopDefinition);

        verify(clampClient).create(LOOP, TEMPLATE);
        verify(consulClient).deploy(any(String.class), any(String.class));
        verify(clampClient).deploy(LOOP);
    }

    @Test
    void test_ControlLoopElementUpdate() throws PfModelException, JSONException, CoderException {
        ClampHttpClient clampClient = spy(mock(ClampHttpClient.class));
        Loop loopDeployed = CODER.convert(CommonTestData.createJsonStatus(BLUEPRINT_DEPLOYED), Loop.class);
        Loop loopInstalled =
                CODER.convert(CommonTestData.createJsonStatus(MICROSERVICE_INSTALLED_SUCCESSFULLY), Loop.class);
        when(clampClient.getstatus(LOOP)).thenReturn(loopDeployed, loopInstalled);
        when(clampClient.deploy(LOOP)).thenReturn(true);

        ConsulDcaeHttpClient consulClient = spy(mock(ConsulDcaeHttpClient.class));
        when(consulClient.deploy(any(String.class), any(String.class))).thenReturn(true);

        ControlLoopElementHandler controlLoopElementHandler =
                new ControlLoopElementHandler(clampClient, consulClient, commonTestData.getParticipantDcaeParameters());

        ParticipantIntermediaryApi intermediaryApi = mock(ParticipantIntermediaryApi.class);
        controlLoopElementHandler.setIntermediaryApi(intermediaryApi);

        ControlLoopElement element = new ControlLoopElement();
        element.setId(UUID.randomUUID());
        element.setOrderedState(ControlLoopOrderedState.PASSIVE);

        ToscaServiceTemplate controlLoopDefinition = new ToscaServiceTemplate();
        controlLoopElementHandler.controlLoopElementUpdate(element, controlLoopDefinition);

        verify(clampClient, times(0)).create(LOOP, TEMPLATE);
        verify(consulClient).deploy(any(String.class), any(String.class));
        verify(clampClient).deploy(LOOP);
    }
}
