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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.onap.policy.clamp.controlloop.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.controlloop.participant.kubernetes.handler.ControlLoopElementHandler;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PodStatusValidator implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    //3 minutes Timeout for the thread to exit.
    private static final long TIME_OUT = 180000;

    private ChartInfo chart;

    public PodStatusValidator(ChartInfo chart) {
        this.chart = chart;
    }


    @SneakyThrows
    @Override
    public void run() {
        logger.info("Polling the status of deployed pods for the chart {}", chart.getChartId().getName());
        Map<String, String> podStatusMap;
        String output = null;
        var isVerified = false;
        long endTime = System.currentTimeMillis() + TIME_OUT;

        while (!isVerified && System.currentTimeMillis() < endTime) {
            try {
                output = HelmClient.executeCommand(verifyPodStatusCommand(chart));
                podStatusMap = mapPodStatus(output);
                isVerified = podStatusMap.values()
                    .stream()
                    .allMatch(s -> s.equals("Running"));
                if (! isVerified) {
                    logger.info("Waiting for the pods to be active for the chart {}", chart.getChartId().getName());
                    Iterator<Map.Entry<String, String>> it = podStatusMap.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, String> entry = it.next();
                        logger.info("Pod: {} , state: {}", entry.getKey(), entry.getValue());
                        it.remove();
                    }
                    ControlLoopElementHandler.getPodStatusMap().put(chart.getReleaseName(), podStatusMap);
                    // Recheck status of pods with 30 secs delay.
                    Thread.sleep(30000);
                } else {
                    logger.info("All pods are in running state for the helm chart {}", chart.getChartId().getName());
                    ControlLoopElementHandler.getPodStatusMap().put(chart.getReleaseName(), podStatusMap);
                }
            } catch (ServiceException | IOException  e) {
                throw new ServiceException("Error verifying the status of the pod.Exiting", e);
            }
        }
    }

    private ProcessBuilder verifyPodStatusCommand(ChartInfo chart) {
        String podName = chart.getReleaseName() + "-" + chart.getChartId().getName();
        String cmd = "kubectl get pods --namespace " +  chart.getNamespace() + " | grep " + podName;
        return new ProcessBuilder("bash", "-c", cmd);
    }


    private Map<String, String> mapPodStatus(String output) throws IOException, ServiceException {
        Map<String, String> podStatusMap = new HashMap<>();
        try (var reader = new BufferedReader(new InputStreamReader(IOUtils.toInputStream(output,
            StandardCharsets.UTF_8)))) {
            var line = reader.readLine();
            while (line != null) {
                if (line.contains(chart.getChartId().getName())) {
                    var result = line.split("\\s+|\\t+");
                    podStatusMap.put(result[0], result[2]);
                }
                line = reader.readLine();
            }
        }
        if (!podStatusMap.isEmpty()) {
            return podStatusMap;
        } else {
            throw new ServiceException("Status of Pod is empty");
        }
    }
}
