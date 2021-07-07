/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.kubernetes.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
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
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.controlloop.participant.kubernetes.service.ChartService;
import org.onap.policy.clamp.controlloop.participant.kubernetes.utils.TestUtils;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
class ControlLoopElementHandlerTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CHART_INFO_YAML = "src/test/resources/ChartList.json";
    private static final String KEY_NAME = "org.onap.domain.database.HelloWorld_K8SMicroserviceControlLoopElement";
    private static List<ChartInfo> charts;
    private static ToscaServiceTemplate toscaServiceTemplate;


    @InjectMocks
    @Spy
    private ControlLoopElementHandler controlLoopElementHandler = new ControlLoopElementHandler();

    @Mock
    private ChartService chartService;

    @Mock
    private ParticipantIntermediaryApi participantIntermediaryApi;

    @BeforeAll
    static void init() throws CoderException {
        charts = CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
        toscaServiceTemplate = TestUtils.testControlLoopRead();
    }


    @Test
    void test_ControlLoopElementStateChange() throws ServiceException {
        UUID controlLoopElementId1 = UUID.randomUUID();
        UUID controlLoopElementId2 = UUID.randomUUID();

        controlLoopElementHandler.getChartMap().put(controlLoopElementId1, charts.get(0));
        controlLoopElementHandler.getChartMap().put(controlLoopElementId2, charts.get(1));

        doNothing().when(chartService).uninstallChart(charts.get(0));

        controlLoopElementHandler.controlLoopElementStateChange(controlLoopElementId1, ControlLoopState.PASSIVE,
            ControlLoopOrderedState.UNINITIALISED);

        doThrow(new ServiceException("Error uninstalling the chart")).when(chartService)
            .uninstallChart(charts.get(0));

        assertDoesNotThrow(() -> controlLoopElementHandler
            .controlLoopElementStateChange(controlLoopElementId1, ControlLoopState.PASSIVE,
                ControlLoopOrderedState.UNINITIALISED));

        assertDoesNotThrow(() -> controlLoopElementHandler
            .controlLoopElementStateChange(controlLoopElementId1, ControlLoopState.PASSIVE,
                ControlLoopOrderedState.RUNNING));

    }

    @Test
    void test_ControlLoopElementUpdate() throws PfModelException, IOException, ServiceException {
        doNothing().when(controlLoopElementHandler).checkPodStatus(any());
        UUID elementId1 = UUID.randomUUID();
        ControlLoopElement element = new ControlLoopElement();
        element.setId(elementId1);
        element.setDefinition(new ToscaConceptIdentifier(KEY_NAME, "1.0.1"));
        element.setOrderedState(ControlLoopOrderedState.PASSIVE);

        controlLoopElementHandler.controlLoopElementUpdate(element, toscaServiceTemplate);

        assertThat(controlLoopElementHandler.getChartMap()).hasSize(1).containsKey(elementId1);

        doThrow(new ServiceException("Error installing the chart")).when(chartService)
            .installChart(Mockito.any());

        UUID elementId2 = UUID.randomUUID();
        element.setId(elementId2);
        controlLoopElementHandler.controlLoopElementUpdate(element, toscaServiceTemplate);

        assertThat(controlLoopElementHandler.getChartMap().containsKey(elementId2)).isFalse();
    }
}
