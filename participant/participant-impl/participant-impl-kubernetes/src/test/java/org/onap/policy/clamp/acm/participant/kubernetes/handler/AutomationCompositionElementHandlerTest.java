/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024,2026 OpenInfra Foundation Europe. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.helm.PodStatusValidator;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.acm.participant.kubernetes.parameters.CommonTestData;
import org.onap.policy.clamp.acm.participant.kubernetes.service.ChartService;
import org.onap.policy.common.utils.coder.MapperFactory;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class AutomationCompositionElementHandlerTest {

    private static final String CHART_INFO = "src/test/resources/ChartList.json";
    private static List<ChartInfo> charts;
    private static ToscaServiceTemplate toscaServiceTemplate;
    private static final String K8S_AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.domain.database.PMSH_K8SMicroserviceAutomationCompositionElement";

    @BeforeAll
    static void init() {
        charts = CommonTestData.getObjectFromJsonFile(CHART_INFO, ChartList.class).getCharts();
        toscaServiceTemplate = CommonTestData.getToscaServiceTemplateFromYamlFile(CommonTestData.TOSCA_TEMPLATE_YAML);
    }

    @Test
    void test_AutomationCompositionElementStateChange() throws ServiceException, PfModelException {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), new ObjectMapper());

        var objectMapper = new ObjectMapper();
        Map<String, Object> inPropertiesMap = objectMapper.convertValue(charts.get(0), new TypeReference<>() {});

        acElementHandler.undeploy(CommonTestData.createCompositionElementDto(),
                CommonTestData.createInstanceElementDto(Map.of("chart", inPropertiesMap)));

        doThrow(new ServiceException("Error uninstalling the chart")).when(chartService).uninstallChart(charts.get(0));

        assertDoesNotThrow(() -> acElementHandler.undeploy(CommonTestData.createCompositionElementDto(),
                        CommonTestData.createInstanceElementDto(inPropertiesMap)));
    }

    @Test
    void test_AutomationCompositionElementUpdate()
            throws PfModelException, IOException, ServiceException {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), MapperFactory.createJsonMapper());

        var nodeTemplatesMap = toscaServiceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var instanceElementDto = CommonTestData.createInstanceElementDto(nodeTemplatesMap
                .get(K8S_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var compositionElementDto = CommonTestData.createCompositionElementDto();

        doReturn(false).when(chartService).installChart(any());
        assertThrows(PfModelException.class, () -> acElementHandler.deploy(compositionElementDto, instanceElementDto));

        doReturn(true).when(chartService).installChart(any());
        acElementHandler.deploy(compositionElementDto, instanceElementDto);

        doThrow(new ServiceException("Error installing the chart")).when(chartService).installChart(any());

        assertThrows(PfModelException.class, () -> acElementHandler.deploy(compositionElementDto, instanceElementDto));
    }

    @Test
    void testUpdate() {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), new ObjectMapper());
        assertDoesNotThrow(
                () -> acElementHandler.update(CommonTestData.createCompositionElementDto(),
                        CommonTestData.createInstanceElementDto(Map.of()),
                        CommonTestData.createInstanceElementDto(Map.of())));
    }

    @Test
    void testLock() {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), new ObjectMapper());

        assertDoesNotThrow(() -> acElementHandler.lock(CommonTestData.createCompositionElementDto(),
                CommonTestData.createInstanceElementDto(Map.of())));
    }

    @Test
    void testUnlock() {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), new ObjectMapper());

        assertDoesNotThrow(() -> acElementHandler.unlock(CommonTestData.createCompositionElementDto(),
                CommonTestData.createInstanceElementDto(Map.of())));
    }

    @Test
    void testDelete() {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), new ObjectMapper());

        assertDoesNotThrow(() -> acElementHandler.delete(CommonTestData.createCompositionElementDto(),
                CommonTestData.createInstanceElementDto(Map.of())));
    }

    @Test
    void testPrime() {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), new ObjectMapper());

        assertDoesNotThrow(() -> acElementHandler.prime(new CompositionDto(UUID.randomUUID(), Map.of(), Map.of())));
    }

    @Test
    void testDeprime() {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), new ObjectMapper());

        assertDoesNotThrow(() -> acElementHandler.deprime(new CompositionDto(UUID.randomUUID(), Map.of(), Map.of())));
    }

    @Test
    void testConvert() {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), new ObjectMapper());
        var obj = new WillNotSerialize();
        assertThrows(PfModelException.class, () -> acElementHandler.convert(obj, Object.class));
    }

    private static class WillNotSerialize {
        public WillNotSerialize getSelf() {
            return this;
        }
    }

    @Test
    void testMigrate() {
        var chartService = mock(ChartService.class);
        var acElementHandler = new AutomationCompositionElementHandler(mock(ParticipantIntermediaryApi.class),
                chartService, mock(PodStatusValidator.class), new ObjectMapper());

        assertDoesNotThrow(() -> acElementHandler.migrate(CommonTestData.createCompositionElementDto(),
                CommonTestData.createCompositionElementDto(), CommonTestData.createInstanceElementDto(Map.of()),
                CommonTestData.createInstanceElementDto(Map.of()), 0));
    }
}
