/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.kubernetes.handler;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.controlloop.participant.kubernetes.helm.PodStatusValidator;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.controlloop.participant.kubernetes.service.ChartService;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of controlLoopElement updates.
 */
@Component
public class ControlLoopElementHandler implements ControlLoopElementListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // Map of helm installation and the status of corresponding pods
    @Getter
    private static Map<String, Map<String, String>> podStatusMap = new ConcurrentHashMap<>();
    private static final Coder CODER = new StandardCoder();

    @Autowired
    private ChartService chartService;

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    // Map of CLElement Id and installed Helm charts
    @Getter(AccessLevel.PACKAGE)
    private final Map<UUID, ChartInfo> chartMap = new HashMap<>();

    // Default thread config values
    private static class ThreadConfig {
        private int uninitializedToPassiveTimeout = 60;
        private int podStatusCheckInterval = 30;
    }

    /**
     * Callback method to handle a control loop element state change.
     *
     * @param controlLoopElementId the ID of the control loop element
     * @param currentState the current state of the control loop element
     * @param newState the state to which the control loop element is changing to
     */
    @Override
    public synchronized void controlLoopElementStateChange(ToscaConceptIdentifier controlLoopId,
            UUID controlLoopElementId, ControlLoopState currentState, ControlLoopOrderedState newState) {
        switch (newState) {
            case UNINITIALISED:
                ChartInfo chart = chartMap.get(controlLoopElementId);
                if (chart != null) {
                    LOGGER.info("Helm deployment to be deleted {} ", chart.getReleaseName());
                    try {
                        chartService.uninstallChart(chart);
                        intermediaryApi.updateControlLoopElementState(controlLoopId,
                            controlLoopElementId, newState, ControlLoopState.UNINITIALISED,
                            ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
                        chartMap.remove(controlLoopElementId);
                        podStatusMap.remove(chart.getReleaseName());
                    } catch (ServiceException se) {
                        LOGGER.warn("Deletion of Helm deployment failed", se);
                    }
                }
                break;
            case PASSIVE:
                intermediaryApi.updateControlLoopElementState(controlLoopId,
                    controlLoopElementId, newState, ControlLoopState.PASSIVE,
                    ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
                break;
            case RUNNING:
                intermediaryApi.updateControlLoopElementState(controlLoopId,
                    controlLoopElementId, newState, ControlLoopState.RUNNING,
                    ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
                break;
            default:
                LOGGER.warn("Cannot transition from state {} to state {}", currentState, newState);
                break;
        }
    }


    /**
     * Callback method to handle an update on a control loop element.
     *
     * @param element the information on the control loop element
     * @param nodeTemplate toscaNodeTemplate
     * @throws PfModelException in case of an exception
     */
    @Override
    public synchronized void controlLoopElementUpdate(ToscaConceptIdentifier controlLoopId,
            ControlLoopElement element, ToscaNodeTemplate nodeTemplate) throws PfModelException {
        @SuppressWarnings("unchecked")
        Map<String, Object> chartData =
            (Map<String, Object>) nodeTemplate.getProperties().get("chart");

        LOGGER.info("Installation request received for the Helm Chart {} ", chartData);
        try {
            var chartInfo =  CODER.convert(chartData, ChartInfo.class);
            chartService.installChart(chartInfo);
            chartMap.put(element.getId(), chartInfo);

            var config = CODER.convert(nodeTemplate.getProperties(), ThreadConfig.class);
            checkPodStatus(controlLoopId, element.getId(), chartInfo, config.uninitializedToPassiveTimeout,
                    config.podStatusCheckInterval);

        } catch (ServiceException | CoderException | IOException | ExecutionException
                | InterruptedException e) {
            LOGGER.warn("Installation of Helm chart failed", e);
        }
    }

    /**
     * Invoke a new thread to check the status of deployed pods.
     * @param chart ChartInfo
     */
    public void checkPodStatus(ToscaConceptIdentifier controlLoopId, UUID elementId,
            ChartInfo chart, int timeout, int podStatusCheckInterval) throws ExecutionException, InterruptedException {
        // Invoke runnable thread to check pod status
        Future<String> result = executor.submit(new PodStatusValidator(chart, timeout,
                podStatusCheckInterval), "Done");
        if (!result.get().isEmpty()) {
            LOGGER.info("Pod Status Validator Completed: {}", result.isDone());
            intermediaryApi.updateControlLoopElementState(controlLoopId, elementId,
                ControlLoopOrderedState.PASSIVE, ControlLoopState.PASSIVE,
                ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
        }
    }

    /**
     * Overridden method.
     *
     * @param controlLoopElementId controlLoopElement id
     * @throws PfModelException in case of error
     */
    @Override
    public synchronized void handleStatistics(UUID controlLoopElementId) throws PfModelException {
        var clElement = intermediaryApi.getControlLoopElement(controlLoopElementId);
        if (clElement != null) {
            var clElementStatistics = new ClElementStatistics();
            clElementStatistics.setControlLoopState(clElement.getState());
            clElementStatistics.setTimeStamp(Instant.now());
            intermediaryApi.updateControlLoopElementStatistics(controlLoopElementId, clElementStatistics);
        }
    }
}
