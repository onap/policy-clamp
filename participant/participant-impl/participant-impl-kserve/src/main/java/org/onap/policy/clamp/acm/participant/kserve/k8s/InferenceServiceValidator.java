/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2023 Nordix Foundation. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.kserve.k8s;

import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import lombok.SneakyThrows;
import org.onap.policy.clamp.acm.participant.kserve.exception.KserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InferenceServiceValidator implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final KserveClient kserveClient;

    private final int statusCheckInterval;

    //Timeout for the thread to exit.
    private final int timeout;

    private final String inferenceServiceName;

    private final String namespace;

    /**
     * Constructor for PodStatusValidator.
     *
     * @param inferenceServiceName name of the inference service
     * @param namespace            kubernetes namespace
     * @param timeout              timeout for the thread to exit
     * @param statusCheckInterval  Interval to check pod status
     */
    public InferenceServiceValidator(String inferenceServiceName, String namespace, int timeout,
            int statusCheckInterval, KserveClient kserveClient) {
        this.inferenceServiceName = inferenceServiceName;
        this.namespace = namespace;
        this.timeout = timeout;
        this.statusCheckInterval = statusCheckInterval;
        this.kserveClient = kserveClient;
    }


    @SneakyThrows
    @Override
    public void run() {
        logger.info("Polling the status of deployed Inference Service {} in namespace {}", inferenceServiceName,
                namespace);
        try {
            verifyInferenceServiceStatus();
        } catch (KserveException | IOException e) {
            throw new KserveException("Error verifying the status of the inference service. Exiting", e);
        }
    }

    /**
     * Verify inference service status.
     * @throws KserveException exception
     * @throws IOException exception
     * @throws InterruptedException exception
     * @throws ApiException exception
     */
    private void verifyInferenceServiceStatus()
            throws KserveException, IOException, InterruptedException, ApiException {
        var isVerified = false;
        String isvcStatus = null;
        long endTime = System.currentTimeMillis() + (timeout * 1000L);

        while (!isVerified && System.currentTimeMillis() < endTime) {
            isvcStatus = kserveClient.getInferenceServiceStatus(namespace, inferenceServiceName);
            isVerified = isvcStatus.equalsIgnoreCase(Boolean.TRUE.toString());
            if (!isVerified) {
                logger.info("Waiting for the inference service {} to be active ", inferenceServiceName);
                // Recheck status of pods in specific intervals.
                Thread.sleep(statusCheckInterval * 1000L);
            } else {
                logger.info("Inference Service {} is Ready to use ", inferenceServiceName);
            }
        }
        if (!isVerified) {
            if (isvcStatus != null && isvcStatus.isEmpty()) {
                throw new KserveException("Kserve setup is unavailable for inference service to be deployed");
            } else {
                throw new KserveException("Time out Exception verifying the status of the inference service");
            }
        }
    }
}
