/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

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
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
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
    private final CommonTestData commonTestData = new CommonTestData();

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
        var automationCompositionElementId1 = UUID.randomUUID();
        var automationCompositionElementId2 = UUID.randomUUID();

        automationCompositionElementHandler.getChartMap().put(automationCompositionElementId1, charts.get(0));
        automationCompositionElementHandler.getChartMap().put(automationCompositionElementId2, charts.get(1));

        doNothing().when(chartService).uninstallChart(charts.get(0));

        automationCompositionElementHandler.undeploy(commonTestData.getAutomationCompositionId(),
                automationCompositionElementId1);

        doThrow(new ServiceException("Error uninstalling the chart")).when(chartService).uninstallChart(charts.get(0));

        assertDoesNotThrow(() -> automationCompositionElementHandler
                .undeploy(commonTestData.getAutomationCompositionId(), automationCompositionElementId1));
    }

    @Test
    void test_AutomationCompositionElementUpdate()
            throws PfModelException, IOException, ServiceException, ExecutionException, InterruptedException {
        doNothing().when(automationCompositionElementHandler).checkPodStatus(any(), any(), any(), anyInt(), anyInt());
        var element = CommonTestData.createAcElementDeploy();
        var nodeTemplatesMap = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();

        doReturn(false).when(chartService).installChart(any());
        assertDoesNotThrow(() -> automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(),
                element, nodeTemplatesMap.get(K8S_AUTOMATION_COMPOSITION_ELEMENT).getProperties()));

        doReturn(true).when(chartService).installChart(any());
        automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                nodeTemplatesMap.get(K8S_AUTOMATION_COMPOSITION_ELEMENT).getProperties());

        assertThat(automationCompositionElementHandler.getChartMap()).hasSize(1).containsKey(element.getId());

        doThrow(new ServiceException("Error installing the chart")).when(chartService).installChart(Mockito.any());

        var elementId2 = UUID.randomUUID();
        element.setId(elementId2);
        assertThrows(PfModelException.class,
                () -> automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                        nodeTemplatesMap.get(K8S_AUTOMATION_COMPOSITION_ELEMENT).getProperties()));

        assertThat(automationCompositionElementHandler.getChartMap().containsKey(elementId2)).isFalse();
    }

    @Test
    void test_checkPodStatus() throws ExecutionException, InterruptedException {
        var chartInfo = charts.get(0);
        var automationCompositionId = UUID.randomUUID();
        assertThrows(ServiceException.class, () -> automationCompositionElementHandler
                .checkPodStatus(automationCompositionId, UUID.randomUUID(), chartInfo, 1, 1));
    }

    @Test
    void testUpdate() throws PfModelException {
        var element = CommonTestData.createAcElementDeploy();
        var automationCompositionId = commonTestData.getAutomationCompositionId();
        assertDoesNotThrow(
                () -> automationCompositionElementHandler.update(automationCompositionId, element, Map.of()));
    }

    @Test
    void testLock() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.lock(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void testUnlock() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.unlock(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void testDelete() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.delete(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void testPrime() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.prime(UUID.randomUUID(), List.of()));
    }

    @Test
    void testDeprime() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.deprime(UUID.randomUUID()));
    }

    @Test
    void testHandleRestartComposition() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.handleRestartComposition(UUID.randomUUID(),
                List.of(), AcTypeState.PRIMED));
    }

    @Test
    void testHandleRestartInstanceDeploying()
            throws PfModelException, InterruptedException, ServiceException, IOException {
        doNothing().when(automationCompositionElementHandler).checkPodStatus(any(), any(), any(), anyInt(), anyInt());
        var element = CommonTestData.createAcElementDeploy();
        var nodeTemplatesMap = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();

        doReturn(true).when(chartService).installChart(any());
        assertDoesNotThrow(() -> automationCompositionElementHandler.handleRestartInstance(
                commonTestData.getAutomationCompositionId(), element,
                nodeTemplatesMap.get(K8S_AUTOMATION_COMPOSITION_ELEMENT).getProperties(), DeployState.DEPLOYING,
                LockState.NONE));

        assertThat(automationCompositionElementHandler.getChartMap()).containsKey(element.getId());
    }

    @Test
    void testHandleRestartInstanceDeployed()
            throws PfModelException, InterruptedException, ServiceException, IOException {
        doNothing().when(automationCompositionElementHandler).checkPodStatus(any(), any(), any(), anyInt(), anyInt());
        var element = CommonTestData.createAcElementDeploy();
        var nodeTemplatesMap = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();

        assertDoesNotThrow(() -> automationCompositionElementHandler.handleRestartInstance(
                commonTestData.getAutomationCompositionId(), element,
                nodeTemplatesMap.get(K8S_AUTOMATION_COMPOSITION_ELEMENT).getProperties(), DeployState.DEPLOYED,
                LockState.LOCKED));

        assertThat(automationCompositionElementHandler.getChartMap()).containsKey(element.getId());
    }

    @Test
    void testHandleRestartInstanceUndeploying()
            throws PfModelException, InterruptedException, ServiceException, IOException {
        doNothing().when(automationCompositionElementHandler).checkPodStatus(any(), any(), any(), anyInt(), anyInt());
        var element = CommonTestData.createAcElementDeploy();
        var nodeTemplatesMap = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();

        assertDoesNotThrow(() -> automationCompositionElementHandler.handleRestartInstance(
                commonTestData.getAutomationCompositionId(), element,
                nodeTemplatesMap.get(K8S_AUTOMATION_COMPOSITION_ELEMENT).getProperties(), DeployState.UNDEPLOYING,
                LockState.LOCKED));
    }
}
