/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021-2022, 2025-2026 OpenInfra Foundation Europe. All rights reserved.
 * ======================================================================
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
 * ========================LICENSE_END===================================
 */

package org.onap.policy.clamp.acm.participant.kubernetes.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.helm.HelmClient;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.HelmRepository;
import org.onap.policy.clamp.acm.participant.kubernetes.parameters.HelmRepositoryConfig;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ChartService {
    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ChartStore chartStore;

    private final HelmClient helmClient;

    private final HelmRepositoryConfig helmRepositoryConfig;

    private static final Coder coder = new StandardCoder();

    /**
     * Get all the installed charts.
     * @return list of charts.
     */
    public Collection<ChartInfo> getAllCharts() {
        return chartStore.getAllCharts();
    }

    /**
     * Get specific chart info.
     * @param name name of the app
     * @param version version of the app
     * @return chart
     */
    public ChartInfo getChart(String name, String version) {
        return chartStore.getChart(name, version);
    }

    /**
     * Save a helm chart.
     * @param chartInfo name and version of the app.
     * @param chartFile Helm chart file
     * @param overrideFile override file
     * @return chart details of the helm chart
     * @throws IOException in case of IO error
     * @throws ServiceException in case of error
     */
    public ChartInfo saveChart(ChartInfo chartInfo, MultipartFile chartFile, MultipartFile overrideFile)
        throws IOException, ServiceException {
        return chartStore.saveChart(chartInfo, chartFile, overrideFile);
    }

    /**
     * Delete a helm chart.
     * @param chart name and version of the chart.
     */
    public void deleteChart(ChartInfo chart) {
        chartStore.deleteChart(chart);
    }

    private static class Repositories extends ArrayList<HelmRepository> {}

    /**
     * Install a helm chart.
     * @param chart name and version.
     * @return boolean flag to indicate success or failure
     * @throws ServiceException in case of error
     * @throws IOException in case of IO errors
     */
    public boolean installChart(ChartInfo chart) throws ServiceException, IOException {
        boolean permittedRepo = false;
        if (chart.getRepository() == null) {
            var repoName = findChartRepo(chart);
            if (repoName == null) {
                logger.error("Chart repository could not be found. Skipping chart Installation "
                    + "for the chart {} ", chart.getChartId().getName());
                return false;
            } else {
                var repo = HelmRepository.builder().repoName(repoName).build();
                chart.setRepository(repo);
            }
        } else {
            // Add a remote repository if passed via TOSCA
            // and check whether the repo is permitted
            Repositories repos = null;
            try {
                repos = coder.decode(helmRepositoryConfig.getRepos(), Repositories.class);
            } catch (CoderException e) {
                throw new ServiceException(e.getMessage());
            }
            for (var repo : repos) {
                var protocols = Arrays.stream(helmRepositoryConfig.getProtocols().split(",")).toList();
                if (repo.getAddress().equals(chart.getRepository().getAddress())
                        && protocols.contains(chart.getRepository().getAddress().split(":")[0])) {
                    configureRepository(chart.getRepository());
                    permittedRepo = true;
                    break;
                }
            }
            if (!permittedRepo) {
                logger.error("Helm Repository/Protocol is not permitted for {}", chart.getRepository().getAddress());
                return false;
            }
        }
        helmClient.installChart(chart);
        return true;
    }


    /**
     * Configure remote repository.
     * @param repo HelmRepository
     * @throws ServiceException in case of error
     */
    public boolean configureRepository(HelmRepository repo) throws ServiceException {
        return helmClient.addRepository(repo);
    }

    /**
     * Finds helm chart repository for a given chart.
     * @param chart chartInfo.
     * @return the chart repo as a string
     * @throws ServiceException in case of error
     * @throws IOException in case of IO errors
     */
    public String findChartRepo(ChartInfo chart) throws ServiceException, IOException {
        logger.info("Fetching helm chart repository for the given chart {} ", chart.getChartId().getName());
        return helmClient.findChartRepository(chart);
    }

    /**
     * Uninstall a helm chart.
     * @param chart name and version
     * @throws ServiceException in case of error.
     */
    public void uninstallChart(ChartInfo chart) throws ServiceException {
        logger.info("Uninstalling helm deployment {}", chart.getReleaseName());
        helmClient.uninstallChart(chart);
    }
}
