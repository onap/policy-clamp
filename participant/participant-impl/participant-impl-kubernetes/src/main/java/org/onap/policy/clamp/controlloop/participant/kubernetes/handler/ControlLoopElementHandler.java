/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
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
    public synchronized void controlLoopElementStateChange(UUID controlLoopElementId, ControlLoopState currentState,
                                                           ControlLoopOrderedState newState) {
        switch (newState) {
            case UNINITIALISED:
                ChartInfo chart = chartMap.get(controlLoopElementId);
                if (chart != null) {
                    LOGGER.info("Helm deployment to be deleted {} ", chart.getReleaseName());
                    try {
                        chartService.uninstallChart(chart);
                        intermediaryApi.updateControlLoopElementState(controlLoopElementId, newState,
                            ControlLoopState.UNINITIALISED);
                        chartMap.remove(controlLoopElementId);
                        podStatusMap.remove(chart.getReleaseName());
                    } catch (ServiceException se) {
                        LOGGER.warn("deletion of Helm deployment failed", se);
                    }
                }
                break;
            case PASSIVE:
                intermediaryApi.updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.PASSIVE);
                break;
            case RUNNING:
                intermediaryApi.updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.RUNNING);
                break;
            default:
                LOGGER.warn("cannot transition from state {} to state {}", currentState, newState);
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
    public synchronized void controlLoopElementUpdate(ControlLoopElement element,
            ToscaNodeTemplate nodeTemplate) throws PfModelException {
        @SuppressWarnings("unchecked")
        Map<String, Object> chartData =
            (Map<String, Object>) nodeTemplate.getProperties().get("chart");

        LOGGER.info("Installation request received for the Helm Chart {} ", chartData);
        try {
            var chartInfo =  CODER.convert(chartData, ChartInfo.class);
            var repositoryValue = chartData.get("repository");
            if (repositoryValue != null) {
                chartInfo.setRepository(repositoryValue.toString());
            }
            chartService.installChart(chartInfo);
            chartMap.put(element.getId(), chartInfo);

            var config = CODER.convert(nodeTemplate.getProperties(), ThreadConfig.class);
            checkPodStatus(chartInfo, config.uninitializedToPassiveTimeout, config.podStatusCheckInterval);

        } catch (ServiceException | CoderException | IOException e) {
            LOGGER.warn("Installation of Helm chart failed", e);
        }
    }

    /**
     * Invoke a new thread to check the status of deployed pods.
     * @param chart ChartInfo
     */
    public void checkPodStatus(ChartInfo chart, int timeout, int podStatusCheckInterval) {
        // Invoke runnable thread to check pod status
        var runnableThread = new Thread(new PodStatusValidator(chart, timeout, podStatusCheckInterval));
        runnableThread.start();
    }

    /**
     * Overridden method.
     *
     * @param controlLoopElementId controlLoopElement id
     * @throws PfModelException incase of error
     */
    @Override
    public synchronized void handleStatistics(UUID controlLoopElementId) throws PfModelException {
        // TODO Implement statistics functionality
    }
}
