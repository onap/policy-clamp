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

package org.onap.policy.clamp.acm.participant.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.onap.policy.clamp.acm.participant.kubernetes.configurations.HelmRepositoryConfig;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.helm.HelmClient;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.acm.participant.kubernetes.models.HelmRepository;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ChartServiceTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CHART_INFO_YAML = "src/test/resources/ChartList.json";
    private static List<ChartInfo> charts;

    @InjectMocks
    @Spy
    private ChartService chartService = new ChartService();

    @Mock
    private ChartStore chartStore;

    @Mock
    private HelmClient helmClient;

    @Mock
    private HelmRepositoryConfig helmRepositoryConfig;

    @BeforeAll
    static void init() throws CoderException {
        charts = CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
    }

    @Test
    void test_getAllCharts() {
        assertThat(chartService.getAllCharts()).isEmpty();

        doReturn(charts).when(chartStore).getAllCharts();
        Collection<ChartInfo> result = chartService.getAllCharts();
        assertNotNull(result);
        assertThat(result).containsAll(charts);
    }

    @Test
    void test_getChart() {
        assertNull(chartService.getChart("dummyName", "dummyversion"));

        doReturn(charts.get(0)).when(chartStore).getChart(any(), any());
        ChartInfo chart = chartService.getChart(charts.get(0).getChartId().getName(),
            charts.get(0).getChartId().getVersion());
        assertNotNull(chart);
        assertThat(chart.getNamespace()).isEqualTo(charts.get(0).getNamespace());
    }

    @Test
    void test_saveChart() throws IOException, ServiceException {
        doThrow(IOException.class).when(chartStore).saveChart(charts.get(0), null, null);
        assertThatThrownBy(() -> chartService.saveChart(charts.get(0), null, null))
            .isInstanceOf(IOException.class);

        MockMultipartFile mockChartFile = new MockMultipartFile("chart", "dummy".getBytes());
        MockMultipartFile mockOverrideFile = new MockMultipartFile("override", "dummy".getBytes());

        doReturn(charts.get(0)).when(chartStore).saveChart(any(), any(), any());

        ChartInfo chart = chartService.saveChart(charts.get(0), mockChartFile, mockOverrideFile);
        assertNotNull(chart);
        assertThat(chart.getChartId().getName()).isEqualTo(charts.get(0).getChartId().getName());

    }

    @Test
    void test_installChart() throws IOException, ServiceException {
        List<HelmRepository> helmRepositoryList = new ArrayList<>();
        helmRepositoryList.add(HelmRepository.builder().address("https://localhost:8080").build());
        doReturn(helmRepositoryList).when(helmRepositoryConfig).getRepos();
        doReturn(List.of("http", "https")).when(helmRepositoryConfig).getProtocols();
        assertDoesNotThrow(() -> chartService.installChart(charts.get(0)));
        doThrow(ServiceException.class).when(helmClient).installChart(any());
        assertThatThrownBy(() -> chartService.installChart(charts.get(0))).isInstanceOf(ServiceException.class);

        doReturn("dummyRepoName").when(chartService).findChartRepo(any());
        doNothing().when(helmClient).installChart(any());
        chartService.installChart(charts.get(1));
        assertEquals("dummyRepoName", charts.get(1).getRepository().getRepoName());

        ChartInfo testChart = charts.get(1);
        testChart.setRepository(null);
        doReturn(null).when(chartService).findChartRepo(any());
        chartService.installChart(charts.get(1));
    }

    @Test
    void test_UninstallChart() throws ServiceException {
        assertDoesNotThrow(() -> chartService.uninstallChart(charts.get(0)));
        doThrow(ServiceException.class).when(helmClient).uninstallChart(any());
        assertThatThrownBy(() -> chartService.uninstallChart(charts.get(0))).isInstanceOf(ServiceException.class);
    }

    @Test
    void test_findChartRepo() throws IOException, ServiceException {
        assertDoesNotThrow(() -> chartService.findChartRepo(charts.get(0)));
        doReturn("dummyRepoName").when(helmClient).findChartRepository(any());
        assertEquals("dummyRepoName", chartService.findChartRepo(charts.get(1)));

        doThrow(ServiceException.class).when(helmClient).findChartRepository(any());
        assertThatThrownBy(() -> chartService.findChartRepo(charts.get(0))).isInstanceOf(ServiceException.class);
    }
}
