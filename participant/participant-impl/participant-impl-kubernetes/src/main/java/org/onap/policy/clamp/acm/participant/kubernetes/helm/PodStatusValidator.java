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

package org.onap.policy.clamp.acm.participant.kubernetes.helm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.handler.AutomationCompositionElementHandler;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PodStatusValidator implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final int statusCheckInterval;

    //Timeout for the thread to exit.
    private final int timeout;

    private ChartInfo chart;

    /**
     * Constructor for PodStatusValidator.
     * @param chart chartInfo
     * @param timeout timeout for the thread to exit
     * @param statusCheckInterval Interval to check pod status
     */
    public PodStatusValidator(ChartInfo chart, int timeout, int statusCheckInterval) {
        this.chart = chart;
        this.timeout = timeout;
        this.statusCheckInterval = statusCheckInterval;
    }


    @SneakyThrows
    @Override
    public void run() {
        logger.info("Polling the status of deployed pods for the chart {}", chart.getChartId().getName());

        try {
            verifyPodStatus();
        } catch (ServiceException | IOException e) {
            throw new ServiceException("Error verifying the status of the pod. Exiting", e);
        }
    }

    private void verifyPodStatus() throws ServiceException, IOException, InterruptedException {
        var isVerified = false;
        long endTime = System.currentTimeMillis() + (timeout * 1000L);

        while (!isVerified && System.currentTimeMillis() < endTime) {
            var output = HelmClient.executeCommand(verifyPodStatusCommand(chart));
            var podStatusMap = mapPodStatus(output);
            isVerified = !podStatusMap.isEmpty()
                    && podStatusMap.values().stream().allMatch("Running"::equals);
            if (!isVerified) {
                logger.info("Waiting for the pods to be active for the chart {}", chart.getChartId().getName());
                podStatusMap.forEach((key, value) -> logger.info("Pod: {} , state: {}", key, value));
                // Recheck status of pods in specific intervals.
                Thread.sleep(statusCheckInterval * 1000L);
            } else {
                logger.info("All pods are in running state for the helm chart {}", chart.getChartId().getName());
                AutomationCompositionElementHandler.getPodStatusMap().put(chart.getReleaseName(), podStatusMap);
            }
        }
        if (!isVerified) {
            throw new ServiceException("Time out Exception verifying the status of the pod");
        }
    }

    private ProcessBuilder verifyPodStatusCommand(ChartInfo chart) {
        String cmd = "kubectl get pods --namespace " +  chart.getNamespace() + " | grep "
                + chart.getReleaseName();
        return new ProcessBuilder("sh", "-c", cmd);
    }


    private Map<String, String> mapPodStatus(String output) throws IOException {
        Map<String, String> podStatusMap = new HashMap<>();
        try (var reader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(output,
            StandardCharsets.UTF_8)))) {
            var line = reader.readLine();
            while (line != null) {
                if (line.contains(chart.getReleaseName())) {
                    var result = line.split("\\s+");
                    podStatusMap.put(result[0], result[2]);
                }
                line = reader.readLine();
            }
        }
        if (podStatusMap.isEmpty()) {
            logger.warn("Status of  Pod {} is empty", chart.getReleaseName());
        }
        return podStatusMap;
    }
}
