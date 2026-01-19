/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022,2026 OpenInfra Foundation Europe. All rights reserved.
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.helm.HelmClient;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.acm.participant.kubernetes.models.HelmRepository;
import org.onap.policy.clamp.acm.participant.kubernetes.parameters.HelmRepositoryConfig;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.mock.web.MockMultipartFile;

class ChartServiceTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CHART_INFO_YAML = "src/test/resources/ChartList.json";
    private static List<ChartInfo> charts;

    @BeforeAll
    static void init() throws CoderException {
        charts = CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
    }

    @Test
    void test_getAllCharts() {
        var chartStore = mock(ChartStore.class);
        var helmClient = mock(HelmClient.class);
        var chartService = new ChartService(chartStore, helmClient, new HelmRepositoryConfig());

        assertThat(chartService.getAllCharts()).isEmpty();
        doReturn(charts).when(chartStore).getAllCharts();
        var result = chartService.getAllCharts();
        assertNotNull(result);
        assertThat(result).containsAll(charts);
    }

    @Test
    void test_getChart() {
        var chartStore = mock(ChartStore.class);
        var helmClient = mock(HelmClient.class);
        var chartService = new ChartService(chartStore, helmClient, new HelmRepositoryConfig());
        assertNull(chartService.getChart("dummyName", "dummyversion"));

        var chart0 = charts.get(0);
        doReturn(chart0).when(chartStore).getChart(any(), any());
        var chart = chartService.getChart(chart0.getChartId().getName(), chart0.getChartId().getVersion());
        assertNotNull(chart);
        assertThat(chart.getNamespace()).isEqualTo(chart0.getNamespace());
    }

    @Test
    void test_saveChart() throws IOException, ServiceException {
        var chart0 = charts.get(0);
        var chartStore = mock(ChartStore.class);
        doThrow(IOException.class).when(chartStore).saveChart(chart0, null, null);
        var helmClient = mock(HelmClient.class);
        var chartService = new ChartService(chartStore, helmClient, new HelmRepositoryConfig());
        assertThatThrownBy(() -> chartService.saveChart(chart0, null, null))
            .isInstanceOf(IOException.class);

        var mockChartFile = new MockMultipartFile("chart", "dummy".getBytes());
        var mockOverrideFile = new MockMultipartFile("override", "dummy".getBytes());

        doReturn(chart0).when(chartStore).saveChart(any(), any(), any());

        var chart = chartService.saveChart(chart0, mockChartFile, mockOverrideFile);
        assertNotNull(chart);
        assertThat(chart.getChartId().getName()).isEqualTo(chart0.getChartId().getName());
    }

    @Test
    void test_FailInstallChart() throws IOException, ServiceException {
        var helmRepositoryConfig = new HelmRepositoryConfig();
        helmRepositoryConfig.setRepos("[]");
        helmRepositoryConfig.setProtocols("http,https");
        var chartStore = mock(ChartStore.class);
        var helmClient = mock(HelmClient.class);
        var chartService = new ChartService(chartStore, helmClient, helmRepositoryConfig);
        doReturn("dummyRepoName").when(helmClient).findChartRepository(any());
        var testChart = charts.get(1);
        var result = chartService.installChart(testChart);
        assertTrue(result);
    }

    @Test
    void test_FailEncodeInstallChart() {
        var helmRepositoryConfig = new HelmRepositoryConfig();
        helmRepositoryConfig.setRepos("[");
        helmRepositoryConfig.setProtocols("http,https");
        var chartStore = mock(ChartStore.class);
        var helmClient = mock(HelmClient.class);
        var chartService = new ChartService(chartStore, helmClient, helmRepositoryConfig);
        var chart0 = charts.get(0);
        assertThatThrownBy(() -> chartService.installChart(chart0)).isInstanceOf(ServiceException.class);
    }

    @Test
    void test_installChart() throws IOException, ServiceException, CoderException {
        var helmRepositoryConfig = new HelmRepositoryConfig();
        var repos = List.of(HelmRepository.builder().address("https://localhost:8080").build());
        helmRepositoryConfig.setRepos(CODER.encode(repos));
        helmRepositoryConfig.setProtocols("http,https");
        var chart0 = charts.get(0);
        var chartStore = mock(ChartStore.class);
        var helmClient = mock(HelmClient.class);
        var chartService = new ChartService(chartStore, helmClient, helmRepositoryConfig);
        assertDoesNotThrow(() -> chartService.installChart(chart0));

        doThrow(ServiceException.class).when(helmClient).installChart(any());
        assertThatThrownBy(() -> chartService.installChart(chart0)).isInstanceOf(ServiceException.class);

        doReturn("dummyRepoName").when(helmClient).findChartRepository(any());
        var testChart = charts.get(1);
        chartService.installChart(testChart);
        assertEquals("dummyRepoName", testChart.getRepository().getRepoName());

        testChart.setRepository(null);
        doReturn(null).when(helmClient).findChartRepository(any());
        chartService.installChart(testChart);

        chartService.deleteChart(testChart);
        verify(chartStore).deleteChart(testChart);
    }

    @Test
    void test_UninstallChart() throws ServiceException {
        var chart0 = charts.get(0);
        var chartStore = mock(ChartStore.class);
        var helmClient = mock(HelmClient.class);
        var chartService = new ChartService(chartStore, helmClient, new HelmRepositoryConfig());
        assertDoesNotThrow(() -> chartService.uninstallChart(chart0));
        doThrow(ServiceException.class).when(helmClient).uninstallChart(any());
        assertThatThrownBy(() -> chartService.uninstallChart(chart0)).isInstanceOf(ServiceException.class);
    }

    @Test
    void test_findChartRepo() throws IOException, ServiceException {
        var chart0 = charts.get(0);
        var chartStore = mock(ChartStore.class);
        var helmClient = mock(HelmClient.class);
        var chartService = new ChartService(chartStore, helmClient, new HelmRepositoryConfig());
        assertDoesNotThrow(() -> chartService.findChartRepo(chart0));
        doReturn("dummyRepoName").when(helmClient).findChartRepository(any());
        assertEquals("dummyRepoName", chartService.findChartRepo(charts.get(1)));

        doThrow(ServiceException.class).when(helmClient).findChartRepository(any());
        assertThatThrownBy(() -> chartService.findChartRepo(chart0)).isInstanceOf(ServiceException.class);
    }
}
