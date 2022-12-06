/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.kubernetes.helm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartList;
import org.onap.policy.clamp.acm.participant.kubernetes.models.HelmRepository;
import org.onap.policy.clamp.acm.participant.kubernetes.service.ChartStore;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileSystemUtils;


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

    @Mock
    HelmRepository repo;


    @BeforeAll
    static void init() throws CoderException {
        charts = CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
    }

    @AfterAll
    public static void close() throws IOException {
        FileSystemUtils.deleteRecursively(Path.of("target/tmp"));
    }

    @Test
    void test_installChart() throws ServiceException {
        doReturn("success").when(helmClient).executeCommand(any());
        doReturn(new File("/target/tmp/override.yaml")).when(chartStore)
            .getOverrideFile(any());
        var chartinfo = charts.get(0);

        assertDoesNotThrow(() -> helmClient.installChart(chartinfo));
        chartinfo.setNamespace("");
        assertDoesNotThrow(() -> helmClient.installChart(chartinfo));

        doReturn("").when(helmClient).executeCommand(any());
        assertDoesNotThrow(() -> helmClient.installChart(chartinfo));

    }

    @Test
    void test_addRepository() throws ServiceException {
        doReturn("").when(helmClient).executeCommand(any());
        when(repo.getRepoName()).thenReturn("RepoName");
        when(repo.getAddress()).thenReturn("http://localhost:8080");
        assertDoesNotThrow(() -> helmClient.addRepository(repo));

        doReturn("failed").when(helmClient).executeCommand(any());
        assertDoesNotThrow(() -> helmClient.addRepository(repo));
    }

    @Test
    void test_findChartRepository() throws IOException, ServiceException {
        String tmpPath = "target/tmp/dummyChart/1.0/";
        doReturn("nginx-stable/nginx-ingress\t0.9.3\t1.11.3"
                + " \tNGINX Ingress Controller").when(helmClient).executeCommand(any());

        String configuredRepo = helmClient.findChartRepository(charts.get(1));
        assertThat(configuredRepo).isEqualTo("nginx-stable");

        File tmpFile = new File(tmpPath + charts.get(1).getChartId().getName());
        if (!tmpFile.mkdirs()) {
            fail("Couldn't create dirs");
        }

        doReturn(Path.of(tmpPath)).when(chartStore).getAppPath(charts.get(1).getChartId());

        doReturn(null).when(helmClient).verifyConfiguredRepo(charts.get(1));

        String localRepoName = helmClient.findChartRepository(charts.get(1));
        assertNotNull(localRepoName);
        assertThat(localRepoName).endsWith(charts.get(0).getChartId().getVersion());
    }

    @Test
    void test_uninstallChart() throws ServiceException {
        doReturn("success").when(helmClient).executeCommand(any());
        helmClient.uninstallChart(charts.get(0));
        doThrow(ServiceException.class).when(helmClient).executeCommand(any());

        assertThatThrownBy(() -> helmClient.uninstallChart(charts.get(0)))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    void test_verifyConfiguredRepoForInvalidChart() throws IOException, ServiceException {
        doReturn("").when(helmClient).executeCommand(any());
        String configuredRepo = helmClient.verifyConfiguredRepo(charts.get(1));
        assertNull(configuredRepo);
    }

}
