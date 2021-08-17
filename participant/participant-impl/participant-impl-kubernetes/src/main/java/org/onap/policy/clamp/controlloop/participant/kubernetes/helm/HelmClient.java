/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021 Nordix Foundation. All rights reserved.
 * ======================================================================
 * Modifications Copyright (C) 2021 AT&T
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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        var processBuilder = prepareCreateNamespaceCommand(chart.getNamespace());
        try {
            executeCommand(processBuilder);
        } catch (ServiceException e) {
            logger.warn("Namespace not created", e);
        }
        processBuilder = prepareInstallCommand(chart);
        logger.info("Installing helm chart {} from the repository {} ", chart.getChartId().getName(),
            chart.getRepository());
        executeCommand(processBuilder);
        logger.info("Chart {} installed successfully", chart.getChartId().getName());
    }

    /**
     * Finds helm chart repository for the chart.
     *
     * @param chart ChartInfo.
     * @return the chart repository as a string
     * @throws ServiceException in case of error
     * @throws IOException in case of IO errors
     */
    public String findChartRepository(ChartInfo chart) throws ServiceException, IOException {
        updateHelmRepo();
        String repository = verifyConfiguredRepo(chart);
        if (repository != null) {
            return repository;
        }
        var localHelmChartDir = chartStore.getAppPath(chart.getChartId()).toString();
        logger.info("Chart not found in helm repositories, verifying local repo {} ", localHelmChartDir);
        if (verifyLocalHelmRepo(new File(localHelmChartDir + "/" + chart.getChartId().getName()))) {
            repository = localHelmChartDir;
        }

        return repository;
    }

    /**
     * Verify helm chart in configured repositories.
     * @param chart chartInfo
     * @return repo name
     * @throws IOException incase of error
     * @throws ServiceException incase of error
     */
    public String verifyConfiguredRepo(ChartInfo chart) throws IOException, ServiceException {
        logger.info("Looking for helm chart {} in all the configured helm repositories", chart.getChartId().getName());
        String repository = null;
        var builder = helmRepoVerifyCommand(chart.getChartId().getName());
        String output = executeCommand(builder);
        try (var reader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(output,
            StandardCharsets.UTF_8)))) {
            String line = reader.readLine();
            while (line != null) {
                if (line.contains(chart.getChartId().getName())) {
                    repository = line.split("/")[0];
                    logger.info("Helm chart located in the repository {} ", repository);
                    return repository;
                }
                line = reader.readLine();
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
        executeCommand(prepareUnInstallCommand(chart));
    }


    /**
     * Execute helm cli bash commands .
     * @param processBuilder processbuilder
     * @return string output
     * @throws ServiceException incase of error.
     */
    public static String executeCommand(ProcessBuilder processBuilder) throws ServiceException {
        var commandStr = toString(processBuilder);

        try {
            var process = processBuilder.start();
            process.waitFor();
            int exitValue = process.exitValue();

            if (exitValue != 0) {
                var error = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
                if (! error.isEmpty()) {
                    throw new ServiceException("Command execution failed: " + commandStr + " " + error);
                }
            }

            var output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            logger.debug("Command <{}> execution, output: {}", commandStr, output);
            return output;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Failed to execute the Command: " + commandStr + ", the command was interrupted",
                ie);
        } catch (Exception exc) {
            throw new ServiceException("Failed to execute the Command: " + commandStr, exc);
        }
    }

    private ProcessBuilder prepareInstallCommand(ChartInfo chart) {

        // @formatter:off
        List<String> helmArguments = new ArrayList<>(
            List.of(
                "helm",
                "install", chart.getReleaseName(), chart.getRepository() + "/" + chart.getChartId().getName(),
                "--version", chart.getChartId().getVersion(),
                "--namespace", chart.getNamespace()
            )
        );
        // @formatter:on

        // Verify if values.yaml/override parameters available for the chart
        var localOverrideYaml = chartStore.getOverrideFile(chart);

        if (verifyLocalHelmRepo(localOverrideYaml)) {
            logger.info("Override yaml available for the helm chart");
            helmArguments.addAll(List.of("--values", localOverrideYaml.getPath()));
        }

        if (chart.getOverrideParams() != null) {
            for (Map.Entry<String, String> entry : chart.getOverrideParams().entrySet()) {
                helmArguments.addAll(List.of("--set", entry.getKey() + "=" + entry.getValue()));
            }
        }
        return new ProcessBuilder().command(helmArguments);
    }

    private ProcessBuilder prepareUnInstallCommand(ChartInfo chart) {
        return new ProcessBuilder("helm", "delete", chart.getReleaseName(), "--namespace",
            chart.getNamespace());
    }

    private ProcessBuilder prepareCreateNamespaceCommand(String namespace) {
        return new ProcessBuilder().command("kubectl", "create", "namespace", namespace);
    }

    private ProcessBuilder helmRepoVerifyCommand(String chartName) {
        return new ProcessBuilder().command("sh", "-c", "helm search repo | grep " + chartName);
    }


    private void updateHelmRepo() throws ServiceException {
        logger.info("Updating local helm repositories before verifying the chart");
        executeCommand(new ProcessBuilder().command("helm", "repo", "update"));
        logger.debug("Helm repositories updated successfully");
    }

    private boolean verifyLocalHelmRepo(File localFile) {
        return localFile.exists();
    }

    protected static String toString(ProcessBuilder processBuilder) {
        return String.join(" ", processBuilder.command());
    }
}
