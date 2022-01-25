/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021 Nordix Foundation. All rights reserved.
 * ======================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.parameters.ParticipantK8sParameters;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ChartStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final StandardCoder STANDARD_CODER = new StandardCoder();

    private final ParticipantK8sParameters participantK8sParameters;

    // ChartStore map contains chart name as key & ChartInfo as value.
    @Getter(AccessLevel.PACKAGE)
    private Map<String, ChartInfo> localChartMap = new ConcurrentHashMap<>();

    /**
     * Constructor method.
     */
    public ChartStore(ParticipantK8sParameters participantK8sParameters) {
        this.participantK8sParameters = participantK8sParameters;
        this.restoreFromLocalFileSystem();
    }

    /**
     * Get local helm chart file.
     *
     * @param chart ChartInfo
     * @return the chart file.
     */
    public File getHelmChartFile(ChartInfo chart) {
        var appPath = getAppPath(chart.getChartId());
        return new File(appPath.toFile(), chart.getChartId().getName());
    }

    /**
     * Get the override yaml file.
     *
     * @param chart ChartInfo
     * @return the override yaml file
     */
    public File getOverrideFile(ChartInfo chart) {
        var appPath = getAppPath(chart.getChartId());
        return new File(appPath.toFile(), "values.yaml");
    }


    /**
     * Saves the helm chart.
     *
     * @param chartInfo chartInfo
     * @param chartFile helm chart file.
     * @param overrideFile override file.
     * @return chart
     * @throws IOException incase of IO error
     * @throws ServiceException incase of error.
     */
    public synchronized ChartInfo saveChart(ChartInfo chartInfo, MultipartFile chartFile, MultipartFile overrideFile)
        throws IOException, ServiceException {
        if (localChartMap.containsKey(key(chartInfo))) {
            throw new ServiceException("Chart already exist");
        }
        var appPath = getAppPath(chartInfo.getChartId());
        Files.createDirectories(appPath);

        chartFile.transferTo(getHelmChartFile(chartInfo));
        if (overrideFile != null) {
            overrideFile.transferTo(getOverrideFile(chartInfo));
        }

        localChartMap.put(key(chartInfo), chartInfo);
        storeChartInFile(chartInfo);
        return chartInfo;
    }

    /**
     * Get the chart info.
     *
     * @param name name of the chart
     * @param version version of the chart
     * @return chart
     */
    public synchronized ChartInfo getChart(String name, String version) {
        return localChartMap.get(key(name, version));
    }

    /**
     * Get all the charts installed.
     *
     * @return list of charts.
     */
    public synchronized List<ChartInfo> getAllCharts() {
        return new ArrayList<>(localChartMap.values());
    }

    /**
     * Delete a chart.
     *
     * @param chart chart info
     */
    public synchronized void deleteChart(ChartInfo chart) {
        var appPath = getAppPath(chart.getChartId());
        try {
            FileSystemUtils.deleteRecursively(appPath);
        } catch (IOException exc) {
            LOGGER.warn("Could not delete chart from local file system : {}", appPath, exc);
        }

        localChartMap.remove(key(chart));
    }

    /**
     * Fetch the local chart directory of specific chart.
     *
     * @param chartId Id of the chart
     * @return path
     */
    public Path getAppPath(ToscaConceptIdentifier chartId) {
        return Path.of(participantK8sParameters.getLocalChartDirectory(), chartId.getName(), chartId.getVersion());
    }

    private void storeChartInFile(ChartInfo chart) {
        try (var out = new PrintStream(new FileOutputStream(getFile(chart)))) {
            out.print(STANDARD_CODER.encode(chart));
        } catch (Exception exc) {
            LOGGER.warn("Could not store chart: {}", chart.getChartId(), exc);
        }
    }

    private File getFile(ChartInfo chart) {
        var appPath = getAppPath(chart.getChartId()).toString();
        return Path.of(appPath, participantK8sParameters.getInfoFileName()).toFile();
    }

    private synchronized void restoreFromLocalFileSystem() {
        try {
            var localChartDirectoryPath = Paths.get(participantK8sParameters.getLocalChartDirectory());
            Files.createDirectories(localChartDirectoryPath);
            restoreFromLocalFileSystem(localChartDirectoryPath);
        } catch (Exception ioe) {
            LOGGER.warn("Could not restore charts from local file system", ioe);
        }
    }

    private synchronized void restoreFromLocalFileSystem(Path localChartDirectoryPath)
        throws IOException {

        Files.walkFileTree(localChartDirectoryPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path localChartFile, BasicFileAttributes attrs) throws IOException {
                try {
                    // Decode only the json file excluding the helm charts
                    if (localChartFile.endsWith(participantK8sParameters.getInfoFileName())) {
                        ChartInfo chart = STANDARD_CODER.decode(localChartFile.toFile(), ChartInfo.class);
                        localChartMap.put(key(chart), chart);
                    }
                    return FileVisitResult.CONTINUE;
                } catch (CoderException ce) {
                    throw new IOException("Error decoding chart file", ce);
                }
            }
        });
    }

    private String key(ChartInfo chart) {
        return key(chart.getChartId().getName(), chart.getChartId().getVersion());
    }

    private String key(String chartName, String chartVersion) {
        return chartName + "_" + chartVersion;
    }
}
