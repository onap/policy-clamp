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

package org.onap.policy.clamp.acm.participant.kubernetes.handler;

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
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.helm.PodStatusValidator;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.service.ChartService;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatistics;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
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
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener {
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

    // Map of acElement Id and installed Helm charts
    @Getter(AccessLevel.PACKAGE)
    private final Map<UUID, ChartInfo> chartMap = new HashMap<>();

    // Default thread config values
    private static class ThreadConfig {
        private int uninitializedToPassiveTimeout = 60;
        private int podStatusCheckInterval = 30;
    }

    /**
     * Callback method to handle a automation composition element state change.
     *
     * @param automationCompositionElementId the ID of the automation composition element
     * @param currentState the current state of the automation composition element
     * @param newState the state to which the automation composition element is changing to
     */
    @Override
    public synchronized void automationCompositionElementStateChange(ToscaConceptIdentifier automationCompositionId,
        UUID automationCompositionElementId, AutomationCompositionState currentState,
        AutomationCompositionOrderedState newState) {
        switch (newState) {
            case UNINITIALISED:
                ChartInfo chart = chartMap.get(automationCompositionElementId);
                if (chart != null) {
                    LOGGER.info("Helm deployment to be deleted {} ", chart.getReleaseName());
                    try {
                        chartService.uninstallChart(chart);
                        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                            automationCompositionElementId, newState, AutomationCompositionState.UNINITIALISED,
                            ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                        chartMap.remove(automationCompositionElementId);
                        podStatusMap.remove(chart.getReleaseName());
                    } catch (ServiceException se) {
                        LOGGER.warn("Deletion of Helm deployment failed", se);
                    }
                }
                break;
            case PASSIVE:
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, newState, AutomationCompositionState.PASSIVE,
                    ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                break;
            case RUNNING:
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, newState, AutomationCompositionState.RUNNING,
                    ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                break;
            default:
                LOGGER.warn("Cannot transition from state {} to state {}", currentState, newState);
                break;
        }
    }

    /**
     * Callback method to handle an update on a automation composition element.
     *
     * @param element the information on the automation composition element
     * @param nodeTemplate toscaNodeTemplate
     * @throws PfModelException in case of an exception
     */
    @Override
    public synchronized void automationCompositionElementUpdate(ToscaConceptIdentifier automationCompositionId,
        AutomationCompositionElement element, ToscaNodeTemplate nodeTemplate) throws PfModelException {
        @SuppressWarnings("unchecked")
        Map<String, Object> chartData = (Map<String, Object>) nodeTemplate.getProperties().get("chart");

        LOGGER.info("Installation request received for the Helm Chart {} ", chartData);
        try {
            var chartInfo = CODER.convert(chartData, ChartInfo.class);
            chartService.installChart(chartInfo);
            chartMap.put(element.getId(), chartInfo);

            var config = CODER.convert(nodeTemplate.getProperties(), ThreadConfig.class);
            checkPodStatus(automationCompositionId, element.getId(), chartInfo, config.uninitializedToPassiveTimeout,
                    config.podStatusCheckInterval);

        } catch (ServiceException | CoderException | IOException | ExecutionException
                | InterruptedException e) {
            LOGGER.warn("Installation of Helm chart failed", e);
        }
    }

    /**
     * Invoke a new thread to check the status of deployed pods.
     *
     * @param chart ChartInfo
     */
    public void checkPodStatus(ToscaConceptIdentifier controlLoopId, UUID elementId,
            ChartInfo chart, int timeout, int podStatusCheckInterval) throws ExecutionException, InterruptedException {
        // Invoke runnable thread to check pod status
        Future<String> result = executor.submit(new PodStatusValidator(chart, timeout,
                podStatusCheckInterval), "Done");
        if (!result.get().isEmpty()) {
            LOGGER.info("Pod Status Validator Completed: {}", result.isDone());
            intermediaryApi.updateAutomationCompositionElementState(controlLoopId, elementId,
                AutomationCompositionOrderedState.PASSIVE, AutomationCompositionState.PASSIVE,
                ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
        }
    }

    /**
     * Overridden method.
     *
     * @param automationCompositionElementId automationCompositionElement id
     * @throws PfModelException in case of error
     */
    @Override
    public synchronized void handleStatistics(UUID automationCompositionElementId) throws PfModelException {
        var acElement = intermediaryApi.getAutomationCompositionElement(automationCompositionElementId);
        if (acElement != null) {
            var acElementStatistics = new AcElementStatistics();
            acElementStatistics.setState(acElement.getState());
            acElementStatistics.setTimeStamp(Instant.now());
            intermediaryApi.updateAutomationCompositionElementStatistics(automationCompositionElementId,
                acElementStatistics);
        }
    }
}
