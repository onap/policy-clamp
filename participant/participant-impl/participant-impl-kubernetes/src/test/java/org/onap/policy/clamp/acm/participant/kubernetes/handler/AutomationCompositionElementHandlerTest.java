/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.kubernetes.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.acm.participant.kubernetes.parameters.CommonTestData;
import org.onap.policy.clamp.acm.participant.kubernetes.service.ChartService;
import org.onap.policy.clamp.acm.participant.kubernetes.utils.TestUtils;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AutomationCompositionElementHandlerTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CHART_INFO_YAML = "src/test/resources/ChartList.json";
    private static final String KEY_NAME =
        "org.onap.domain.database.HelloWorld_K8SMicroserviceAutomationCompositionElement";
    private static List<ChartInfo> charts;
    private static ToscaServiceTemplate toscaServiceTemplate;
    private static final String K8S_AUTOMATION_COMPOSITION_ELEMENT =
        "org.onap.domain.database.PMSH_K8SMicroserviceAutomationCompositionElement";
    private CommonTestData commonTestData = new CommonTestData();

    @InjectMocks
    @Spy
    private AutomationCompositionElementHandler automationCompositionElementHandler =
        new AutomationCompositionElementHandler();

    @Mock
    private ChartService chartService;

    @Mock
    private ParticipantIntermediaryApi participantIntermediaryApi;

    @Mock
    private ExecutorService executor;
    @Mock
    private Future<String> result;

    @BeforeAll
    static void init() throws CoderException {
        charts = CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
        toscaServiceTemplate = TestUtils.testAutomationCompositionRead();
    }

    @Test
    void test_AutomationCompositionElementStateChange() throws ServiceException {
        UUID automationCompositionElementId1 = UUID.randomUUID();
        UUID automationCompositionElementId2 = UUID.randomUUID();

        automationCompositionElementHandler.getChartMap().put(automationCompositionElementId1, charts.get(0));
        automationCompositionElementHandler.getChartMap().put(automationCompositionElementId2, charts.get(1));

        doNothing().when(chartService).uninstallChart(charts.get(0));

        automationCompositionElementHandler.automationCompositionElementStateChange(
            commonTestData.getAutomationCompositionId(), automationCompositionElementId1,
            AutomationCompositionState.PASSIVE, AutomationCompositionOrderedState.UNINITIALISED);

        doThrow(new ServiceException("Error uninstalling the chart")).when(chartService).uninstallChart(charts.get(0));

        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementStateChange(
            commonTestData.getAutomationCompositionId(), automationCompositionElementId1,
            AutomationCompositionState.PASSIVE, AutomationCompositionOrderedState.PASSIVE));

        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementStateChange(
            commonTestData.getAutomationCompositionId(), automationCompositionElementId1,
            AutomationCompositionState.PASSIVE, AutomationCompositionOrderedState.UNINITIALISED));

        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementStateChange(
            commonTestData.getAutomationCompositionId(), automationCompositionElementId1,
            AutomationCompositionState.PASSIVE, AutomationCompositionOrderedState.RUNNING));

    }

    @Test
    void test_AutomationCompositionElementUpdate() throws PfModelException, IOException, ServiceException,
        ExecutionException, InterruptedException {
        doReturn(true).when(chartService).installChart(any());
        doNothing().when(automationCompositionElementHandler).checkPodStatus(any(), any(), any(), anyInt(), anyInt());
        UUID elementId1 = UUID.randomUUID();
        AutomationCompositionElement element = new AutomationCompositionElement();
        element.setId(elementId1);
        element.setDefinition(new ToscaConceptIdentifier(KEY_NAME, "1.0.1"));
        element.setOrderedState(AutomationCompositionOrderedState.PASSIVE);

        Map<String, ToscaNodeTemplate> nodeTemplatesMap =
            toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler.automationCompositionElementUpdate(
            commonTestData.getAutomationCompositionId(), element,
            nodeTemplatesMap.get(K8S_AUTOMATION_COMPOSITION_ELEMENT));

        assertThat(automationCompositionElementHandler.getChartMap()).hasSize(1).containsKey(elementId1);

        doThrow(new ServiceException("Error installing the chart")).when(chartService).installChart(Mockito.any());

        UUID elementId2 = UUID.randomUUID();
        element.setId(elementId2);
        automationCompositionElementHandler.automationCompositionElementUpdate(
            commonTestData.getAutomationCompositionId(), element,
            nodeTemplatesMap.get(K8S_AUTOMATION_COMPOSITION_ELEMENT));

        assertThat(automationCompositionElementHandler.getChartMap().containsKey(elementId2)).isFalse();
    }

    @Test
    void test_handleStatistics() throws PfModelException {
        UUID elementId1 = UUID.randomUUID();
        automationCompositionElementHandler.getChartMap().put(elementId1, charts.get(0));
        when(participantIntermediaryApi.getAutomationCompositionElement(elementId1))
            .thenReturn(new AutomationCompositionElement());
        assertDoesNotThrow(() -> automationCompositionElementHandler.handleStatistics(elementId1));
    }

    @Test
    void test_checkPodStatus() throws ExecutionException, InterruptedException {
        doReturn(result).when(executor).submit(any(Runnable.class), any());
        doReturn("Done").when(result).get();
        doReturn(true).when(result).isDone();
        var chartInfo = charts.get(0);
        ToscaConceptIdentifier controlLoopId = new ToscaConceptIdentifier();
        AutomationCompositionElement element = new AutomationCompositionElement();
        assertDoesNotThrow(
            () -> automationCompositionElementHandler.checkPodStatus(controlLoopId, element.getId(), chartInfo,
                1, 1));
    }
}
