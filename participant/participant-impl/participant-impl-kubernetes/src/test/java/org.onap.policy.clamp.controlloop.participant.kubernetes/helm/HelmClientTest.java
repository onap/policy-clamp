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

package org.onap.policy.clamp.controlloop.participant.kubernetes.helm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.onap.policy.clamp.controlloop.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.controlloop.participant.kubernetes.service.ChartStore;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
class HelmClientTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CHART_INFO_YAML = "src/test/resources/ChartList.json";
    private static List<ChartInfo> charts;

    @InjectMocks
    @Spy
    private HelmClient helmClient = new HelmClient();

    @Mock
    ChartStore chartStore;

    private static MockedStatic<HelmClient> mockedClient;

    @BeforeAll
    static void init() throws CoderException {
        charts = CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
        //Mock static method for bash command execution
        mockedClient = mockStatic(HelmClient.class);
    }

    @Test
    void test_installChart() throws IOException {
        mockedClient.when(() -> HelmClient.executeCommand(any()))
            .thenReturn("success");
        doReturn(new File("/target/tmp/override.yaml")).when(chartStore)
            .getOverrideFile(any());
        assertDoesNotThrow(() -> helmClient.installChart(charts.get(0)));
    }

    @Test
    void test_findChartRepository() throws IOException, ServiceException {
        mockedClient.when(() -> HelmClient.executeCommand(Mockito.any()))
            .thenReturn("nginx-stable/nginx-ingress\t0.9.3\t1.11.3"
                + " \tNGINX Ingress Controller");
        String configuredRepo = helmClient.findChartRepository(charts.get(1));

        assertThat(configuredRepo).isEqualTo("nginx-stable");

        doReturn(Path.of("/target/tmp/dummyChart/1.0")).when(chartStore).getAppPath(charts.get(1).getChartName(),
            charts.get(1).getVersion());

        doReturn(null).when(helmClient).verifyConfiguredRepo(charts.get(1));

        String localRepoName = helmClient.findChartRepository(charts.get(1));
        assertNotNull(localRepoName);
        assertThat(localRepoName).endsWith(charts.get(0).getVersion());
    }

    @Test
    void test_uninstallChart() throws ServiceException {
        helmClient.uninstallChart(charts.get(0));
        mockedClient.when(() -> HelmClient.executeCommand(any())).thenThrow(new ServiceException("error in execution"));

        assertThatThrownBy(() -> helmClient.uninstallChart(charts.get(0)))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    void test_verifyConfiguredRepoForInvalidChart() throws IOException, ServiceException {
        mockedClient.when(() -> HelmClient.executeCommand(Mockito.any()))
            .thenReturn("");
        String configuredRepo = helmClient.verifyConfiguredRepo(charts.get(1));
        assertNull(configuredRepo);
    }

}
