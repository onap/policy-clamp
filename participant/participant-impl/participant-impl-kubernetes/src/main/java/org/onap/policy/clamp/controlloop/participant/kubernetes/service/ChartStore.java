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

package org.onap.policy.clamp.controlloop.participant.kubernetes.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onap.policy.clamp.controlloop.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;


@Component
public class ChartStore {
    public static final String LOCAL_CHART_DIR = "/var/helm-manager/database";
    private static final String INFO_FILE_NAME = "CHART_INFO.json";
    private static Gson gson = new GsonBuilder().create();

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The chartStore map contains chart name as key & ChartInfo as value.
     */
    private Map<String, ChartInfo> localChartMap = new HashMap<>();

    /**
     * Constructor method.
     */
    public ChartStore() {
        this.restoreFromDatabase();
    }


    /**
     * Get local helm chart file.
     * @param chart ChartInfo
     * @return the chart file.
     */
    public File getHelmChartFile(ChartInfo chart) {
        var appPath = getAppPath(chart.getChartName(), chart.getVersion());
        return new File(appPath.toFile(), chart.getChartName());
    }

    /**
     * Get the override yaml file.
     * @param chart ChartInfo
     * @return the override yaml file
     */
    public File getOverrideFile(ChartInfo chart) {
        var appPath = getAppPath(chart.getChartName(), chart.getVersion());
        return new File(appPath.toFile(), "values.yaml");
    }


    /**
     * Saves the helm chart.
     * @param chartInfo chartInfo
     * @param chartFile helm chart file.
     * @return chart
     * @throws IOException incase of IO error
     * @throws ServiceException incase of error.
     */
    public synchronized ChartInfo saveChart(ChartInfo chartInfo, MultipartFile chartFile, MultipartFile overrideFile)
        throws IOException, ServiceException {
        if (localChartMap.containsKey(key(chartInfo.getChartName(), chartInfo.getVersion()))) {
            throw new ServiceException("Chart already exist");
        }
        var appPath = getAppPath(chartInfo.getChartName(), chartInfo.getVersion());
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
     * @param name name of the chart
     * @param version version of the chart
     * @return chart
     */
    public synchronized ChartInfo getChart(String name, String version)  {
        return localChartMap.get(key(name, version));
    }

    /**
     * Get all the charts installed.
     * @return list of charts.
     */
    public synchronized List<ChartInfo> getAllCharts() {
        return new ArrayList<>(localChartMap.values());
    }

    /**
     * Delete a chart.
     * @param chart chart info
     */
    public synchronized void deleteChart(ChartInfo chart) {
        try {
            var appPath = getAppPath(chart.getChartName(), chart.getVersion());
            FileSystemUtils.deleteRecursively(appPath);
        } catch (IOException e) {
            logger.warn("Could not delete chart from database : {}", e.getMessage());
        }

        localChartMap.remove(key(chart));
    }

    /**
     * Fetch the local chart directory of specific chart.
     * @param chartName name of the chart
     * @param chartVersion version of the chart
     * @return path
     */
    public Path getAppPath(String chartName, String chartVersion) {
        return Path.of(LOCAL_CHART_DIR, chartName, chartVersion);
    }

    private void storeChartInFile(ChartInfo chart) {
        try (var out = new PrintStream(new FileOutputStream(getFile(chart)))) {
            out.print(gson.toJson(chart));
        } catch (Exception e) {
            logger.warn("Could not store chart: {} {}", chart.getChartName(), e.getMessage());
        }
    }

    private File getFile(ChartInfo chart) {
        var appPath = getAppPath(chart.getChartName(), chart.getVersion()).toString();
        return Path.of(appPath, INFO_FILE_NAME).toFile();
    }

    private synchronized void restoreFromDatabase() {
        try {
            Files.createDirectories(Paths.get(LOCAL_CHART_DIR));
            restoreFromDatabase(new File(LOCAL_CHART_DIR));
        } catch (IOException e) {
            logger.warn("Could not restore charts from database: {}", e.getMessage());
        }
    }

    private synchronized void restoreFromDatabase(File file) throws IOException {
        if (file.isDirectory()) {
            for (File dirElement : file.listFiles()) {
                restoreFromDatabase(dirElement);
            }
        } else if (file.getName().equals(INFO_FILE_NAME)) {
            var json = Files.readString(file.toPath());
            ChartInfo chart = gson.fromJson(json, ChartInfo.class);
            localChartMap.put(key(chart), chart);
        }
    }

    private String key(ChartInfo chart) {
        return key(chart.getChartName(), chart.getVersion());
    }

    private String key(String chartName, String chartVersion) {
        return chartName + "_" + chartVersion;
    }

}
