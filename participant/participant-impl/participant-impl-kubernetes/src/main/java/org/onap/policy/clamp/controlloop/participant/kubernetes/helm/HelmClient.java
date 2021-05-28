/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

package org.onap.policy.clamp.controlloop.participant.kubernetes.helm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.onap.policy.clamp.controlloop.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.controlloop.participant.kubernetes.service.ChartStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Client to talk with Helm cli. Supports helm3 + version
 */
@Component
public class HelmClient {

    @Autowired
    private ChartStore chartStore;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    /**
     * Install a chart.
     *
     * @param chart name and version.
     * @throws ServiceException incase of error
     */
    public void installChart(ChartInfo chart) throws ServiceException {
        ProcessBuilder builder = prepareCreateNamespaceCommand(chart.getNamespace());
        try {
            executeCommand(builder);
        } catch (ServiceException e) {
            logger.warn("Namespace not created {}", e.getMessage());
        }
        builder = prepareInstallCommand(chart);
        logger.info("Installing helm chart {} from the repository {} ", chart.getChartName(), chart.getRepository());
        executeCommand(builder);
        logger.info("Chart {} installed successfully", chart.getChartName());
    }

    /**
     * Finds helm chart repository for the chart.
     *
     * @param chart ChartInfo.
     * @throws ServiceException incase of error
     */
    public String findChartRepository(ChartInfo chart) throws ServiceException, IOException {
        updateHelmRepo();
        logger.info("Looking for helm chart {} in all the configured helm repositories", chart.getChartName());
        String repository = null;
        var processBuilder = helmRepoVerifyCommand(chart.getChartName());
        Process p = null;
        p = processBuilder.start();

        var reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String line = reader.readLine();
        while (line != null) {
            if (line.contains(chart.getChartName())) {
                repository = line.split("/")[0];
                logger.info("Helm chart located in the repository {} ", repository);
                break;
            }
            line = reader.readLine();
        }
        reader.close();

        if (repository == null) {
            var localHelmChartDir = chartStore.getAppPath(chart.getChartName(), chart.getVersion()).toString();
            logger.info("Chart not found in helm repositories, verifying local repo {} ", localHelmChartDir);
            if (verifyLocalHelmRepo(localHelmChartDir + "/" + chart.getChartName())) {
                repository = localHelmChartDir;
            }
        }
        return repository;
    }

    /**
     * Uninstall a chart.
     *
     * @param chart name and version.
     * @throws ServiceException incase of error
     */
    public void uninstallChart(ChartInfo chart) throws ServiceException {
        ProcessBuilder builder = prepareUnInstallCommand(chart);
        executeCommand(builder);
    }

    static String executeCommand(ProcessBuilder builder) throws ServiceException {
        var commandStr = toString(builder);

        try {
            var process = builder.start();
            process.waitFor();
            int exitValue = process.exitValue();

            if (exitValue != 0) {
                var error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                throw new ServiceException("Command execution failed: " + commandStr + " " + error);
            }
            var output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            logger.debug("Command <{}> execution, output:{}", commandStr, output);
            return output;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ServiceException(
                    "Failed to execute the Command: " + commandStr + "  the command was interrupted");
        } catch (Exception e) {
            throw new ServiceException("Failed to execute the Command: " + commandStr + "  " + e.getMessage());
        }
    }

    private ProcessBuilder prepareInstallCommand(ChartInfo chart) {

        List<String> helmArguments = new ArrayList<>();
        helmArguments.addAll(Arrays.asList(//
                "helm", //
                "install", chart.getReleaseName(), chart.getRepository() + "/" + chart.getChartName(), //
                "--version", chart.getVersion(), //
                "--namespace", chart.getNamespace()));

        // Verify if values.yaml available for the chart
        var overrideFile = chartStore.getOverrideFile(chart).getPath();
        if (verifyLocalHelmRepo(overrideFile)) {
            logger.info("Override yaml file available for the helm chart");
            helmArguments.addAll(Arrays.asList("--values", overrideFile));
        }

        var processBuilder = new ProcessBuilder();
        return processBuilder.command(helmArguments);
    }

    private ProcessBuilder prepareUnInstallCommand(ChartInfo chart) {
        return new ProcessBuilder("helm", "delete", chart.getReleaseName(), "--namespace", chart.getNamespace());
    }

    private ProcessBuilder prepareCreateNamespaceCommand(String namespace) {
        return new ProcessBuilder(Arrays.asList(//
                "kubectl", "create", "namespace", namespace));
    }

    private ProcessBuilder helmRepoVerifyCommand(String chartName) {
        return new ProcessBuilder().command(Arrays.asList(//
                "bash", //
                "-c", //
                "helm search repo | grep " + chartName));
    }

    private ProcessBuilder localRepoVerifyCommand(String localFile) {
        return new ProcessBuilder(Arrays.asList("bash", "-c", "ls " + localFile));
    }

    private void updateHelmRepo() throws ServiceException {
        logger.info("Updating local helm repositories before verifying the chart");
        List<String> helmArguments = Arrays.asList(//
                "helm", //
                "repo", //
                "update");

        var processBuilder = new ProcessBuilder();
        processBuilder.command(helmArguments);
        executeCommand(processBuilder);
        logger.debug("Helm repositories updated successfully");
    }

    private boolean verifyLocalHelmRepo(String localFile) {
        var isVerified = false;
        var processBuilder = localRepoVerifyCommand(localFile);
        try {
            executeCommand(processBuilder);
            isVerified = true;
        } catch (ServiceException e) {
            logger.error("Unable to verify file in local repository", e);
        }
        return isVerified;
    }

    protected static String toString(ProcessBuilder builder) {
        var str = new StringBuilder();
        builder.command().forEach(arg -> str.append(" " + arg));
        return str.toString().trim();

    }

}
