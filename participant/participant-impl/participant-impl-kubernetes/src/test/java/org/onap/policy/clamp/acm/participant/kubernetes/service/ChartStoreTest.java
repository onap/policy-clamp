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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.acm.participant.kubernetes.parameters.ParticipantK8sParameters;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.FileSystemUtils;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChartStoreTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CHART_INFO_YAML = "src/test/resources/ChartList.json";
    private static List<ChartInfo> charts;

    @Mock
    private ParticipantK8sParameters parameters;

    private ChartStore chartStore;


    @BeforeAll
    static void init() throws CoderException {
        charts = CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
    }

    //Overriding the local chart dir parameter to a temp folder under target for testing java FILE IO operations.
    @BeforeEach
    void setup() {
        Mockito.doReturn("target/tmp/").when(parameters).getLocalChartDirectory();
        Mockito.doReturn("info.json").when(parameters).getInfoFileName();
        chartStore = new ChartStore(parameters);
    }

    //Clean up the 'tmp' dir after each test case.
    @AfterEach
    void cleanUp() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of(parameters.getLocalChartDirectory()));
        chartStore.getLocalChartMap().clear();
    }

    @Test
    void test_getHelmChartFile() {
        File file = chartStore.getHelmChartFile(charts.get(0));
        assertNotNull(file);
        assertThat(file.getPath()).endsWith(charts.get(0).getChartId().getName());
    }

    @Test
    void test_getOverrideFile() {
        File file = chartStore.getOverrideFile(charts.get(0));
        assertNotNull(file);
        assertThat(file.getPath()).endsWith("values.yaml");
    }

    @Test
    void test_saveChart() throws IOException, ServiceException {
        MockMultipartFile mockChartFile = new MockMultipartFile("chart", "dummy".getBytes());
        MockMultipartFile mockOverrideFile = new MockMultipartFile("override", "dummy".getBytes());
        ChartInfo testChart = charts.get(0);
        testChart.setChartId(new ToscaConceptIdentifier("testChart", "1.0.0"));
        ChartInfo result = chartStore.saveChart(charts.get(0), mockChartFile, mockOverrideFile);

        assertThat(result.getChartId().getName()).isEqualTo("testChart");
        assertThat(chartStore.getLocalChartMap()).hasSize(1);

        assertThatThrownBy(() -> chartStore.saveChart(charts.get(0), mockChartFile, mockOverrideFile))
            .isInstanceOf(ServiceException.class);
    }


    @Test
    void test_getChart() {
        assertNull(chartStore.getChart(charts.get(0).getChartId().getName(), charts.get(0).getChartId().getVersion()));
        chartStore.getLocalChartMap().put(charts.get(0).getChartId().getName() + "_" + charts.get(0).getChartId()
                .getVersion(), charts.get(0));
        ChartInfo chart = chartStore.getChart(charts.get(0).getChartId().getName(),
            charts.get(0).getChartId().getVersion());
        assertThat(chart.getChartId().getName()).isEqualTo(charts.get(0).getChartId().getName());
    }

    @Test
    void test_getAllChart() {
        // When the chart store is empty before adding any charts
        assertThat(chartStore.getAllCharts()).isEmpty();

        for (ChartInfo chart : charts) {
            chartStore.getLocalChartMap().put(chart.getChartId().getName() + "_" + chart.getChartId().getVersion(),
                chart);
        }
        List<ChartInfo> retrievedChartList = chartStore.getAllCharts();
        assertThat(retrievedChartList).isNotEmpty();
        assertThat(retrievedChartList.size()).isEqualTo(charts.size());
    }

    @Test
    void test_deleteChart() {
        chartStore.getLocalChartMap().put(charts.get(0).getChartId().getName() + "_" + charts.get(0).getChartId()
                .getVersion(), charts.get(0));
        assertThat(chartStore.getLocalChartMap()).hasSize(1);
        chartStore.deleteChart(charts.get(0));
        assertThat(chartStore.getLocalChartMap()).isEmpty();
    }

    @Test
    void test_getAppPath() {
        Path path = chartStore.getAppPath(charts.get(0).getChartId());
        assertNotNull(path);
        assertThat(path.toString()).endsWith(charts.get(0).getChartId().getVersion());
        assertThat(path.toString()).startsWith("target");
    }

    @Test
    void test_chartSoreInstantiationWithExistingChartFiles() throws IOException, ServiceException {
        MockMultipartFile mockChartFile = new MockMultipartFile("HelmChartFile", "dummyData".getBytes());
        MockMultipartFile mockOverrideFile = new MockMultipartFile("overrideFile.yaml", "dummyData".getBytes());
        ChartInfo testChart = charts.get(0);
        testChart.setChartId(new ToscaConceptIdentifier("dummyChart", "1.0.0"));

        //Creating a dummy chart in local dir.
        chartStore.saveChart(charts.get(0), mockChartFile, mockOverrideFile);

        //Instantiating a new chartStore object with pre available chart in local.
        ChartStore chartStore2 = new ChartStore(parameters);
        assertThat(chartStore2.getLocalChartMap()).hasSize(1).containsKey("dummyChart_" + charts.get(0).getChartId()
            .getVersion());
    }
}
