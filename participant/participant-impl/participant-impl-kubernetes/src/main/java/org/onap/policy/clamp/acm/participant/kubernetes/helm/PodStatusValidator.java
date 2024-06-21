/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021-2024 Nordix Foundation. All rights reserved.
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

import jakarta.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PodStatusValidator {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final int statusCheckInterval;

    //Timeout for the thread to exit.
    private final int timeout;

    private ChartInfo chart;

    private HelmClient client = new HelmClient();

    /**
     * Constructor for PodStatusValidator.
     *
     * @param chart chartInfo
     * @param timeout timeout for the thread to exit
     * @param statusCheckInterval Interval to check pod status
     */
    public PodStatusValidator(ChartInfo chart, int timeout, int statusCheckInterval) {
        this.chart = chart;
        this.timeout = timeout;
        this.statusCheckInterval = statusCheckInterval;
    }

    /**
     * Run the execution.
     *
     * @throws InterruptedException in case of an exception
     * @throws ServiceException in case of an exception
     */
    public void run() throws InterruptedException, PfModelException {
        logger.info("Polling the status of deployed pods for the chart {}", chart.getChartId().getName());

        try {
            verifyPodStatus();
        } catch (IOException | ServiceException e) {
            throw new PfModelException(Response.Status.BAD_REQUEST, "Error verifying the status of the pod. Exiting");
        }
    }

    private void verifyPodStatus() throws ServiceException, IOException, InterruptedException, PfModelException {
        var isVerified = false;
        long endTime = System.currentTimeMillis() + (timeout * 1000L);

        while (!isVerified && System.currentTimeMillis() < endTime) {
            var output = client.executeCommand(verifyPodStatusCommand(chart));
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
            }
        }
        if (!isVerified) {
            throw new PfModelException(Response.Status.GATEWAY_TIMEOUT,
                    "Time out Exception verifying the status of the pod");
        }
    }

    private ProcessBuilder verifyPodStatusCommand(ChartInfo chart) {
        String cmd = HelmClient.COMMAND_KUBECTL
            + " get pods --namespace " + chart.getNamespace() + " | grep " + getPodName();
        return new ProcessBuilder(HelmClient.COMMAND_SH, "-c", cmd);
    }

    private String getPodName() {
        return StringUtils.isNotEmpty(chart.getPodName()) ? chart.getPodName() : chart.getChartId().getName();
    }

    private Map<String, String> mapPodStatus(String output) throws IOException {
        Map<String, String> podStatusMap = new HashMap<>();
        var podName = getPodName();
        try (var reader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(output,
            StandardCharsets.UTF_8)))) {
            var line = reader.readLine();
            while (line != null) {
                if (line.contains(podName)) {
                    var result = line.split("\\s+");
                    podStatusMap.put(result[0], result[2]);
                }
                line = reader.readLine();
            }
        }
        if (podStatusMap.isEmpty()) {
            logger.warn("Status of  Pod {} is empty", podName);
        }
        return podStatusMap;
    }
}
