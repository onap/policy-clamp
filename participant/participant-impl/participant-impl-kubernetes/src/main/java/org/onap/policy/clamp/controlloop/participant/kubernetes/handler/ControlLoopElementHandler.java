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


import com.google.gson.internal.LinkedTreeMap;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.controlloop.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.controlloop.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.controlloop.participant.kubernetes.service.ChartService;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of controlLoopElement updates.
 */
@Component
public class ControlLoopElementHandler implements ControlLoopElementListener {

    @Autowired
    private ChartService chartService;

    @Autowired
    private ParticipantIntermediaryApi intermediaryApi;

    private final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Map of CLElement Id and installed Helm charts
    private static final Map<String, ChartInfo> chartMap = new HashMap<>();

    /**
     * Callback method to handle a control loop element state change.
     *
     * @param controlLoopElementId the ID of the control loop element
     * @param currentState the current state of the control loop element
     * @param newState the state to which the control loop element is changing to
     */
    @Override
    public void controlLoopElementStateChange(UUID controlLoopElementId, ControlLoopState currentState,
            ControlLoopOrderedState newState) {
        switch (newState) {
            case UNINITIALISED:
                if (chartMap.containsKey(controlLoopElementId.toString())) {
                    ChartInfo chart = chartMap.get(controlLoopElementId.toString());
                    logger.info("Helm deployment to be deleted {} ", chart.getReleaseName());
                    try {
                        chartService.uninstallChart(chart);
                        intermediaryApi.updateControlLoopElementState(controlLoopElementId, newState,
                                ControlLoopState.UNINITIALISED);
                    } catch (ServiceException e) {
                        e.printStackTrace();
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
                logger.debug("Unknown orderedstate {}", newState);
                break;
        }
    }


    /**
     * Callback method to handle an update on a control loop element.
     *
     * @param element the information on the control loop element
     * @param controlLoopDefinition toscaServiceTemplate
     * @throws PfModelException in case of an exception
     */
    @Override
    public void controlLoopElementUpdate(ControlLoopElement element, ToscaServiceTemplate controlLoopDefinition)
            throws PfModelException {

        for (Map.Entry<String, ToscaNodeTemplate> nodeTemplate : controlLoopDefinition.getToscaTopologyTemplate()
                .getNodeTemplates().entrySet()) {

            // Fetching the node template of corresponding CL element
            if (element.getDefinition().getName().equals(nodeTemplate.getKey())
                    && nodeTemplate.getValue().getProperties().containsKey("chart")) {
                @SuppressWarnings("unchecked")
                LinkedTreeMap<String, Object> chartData =
                        (LinkedTreeMap<String, Object>) nodeTemplate.getValue().getProperties().get("chart");

                logger.info("Installation request received for the Helm Chart {} ", chartData);
                var chart = new ChartInfo(String.valueOf(chartData.get("release_name")),
                        String.valueOf(chartData.get("chart_name")), String.valueOf(chartData.get("version")),
                        String.valueOf(chartData.get("namespace")));
                try {
                    if (chartData.containsKey("repository")) {
                        chart.setRepository(String.valueOf(chartData.get("repository")));
                    }
                    chartService.installChart(chart);
                    chartMap.put(element.getId().toString(), chart);
                } catch (IOException | ServiceException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Overridden method.
     *
     * @param controlLoopElementId controlLoopElement id
     * @throws PfModelException incase of error
     */
    @Override
    public void handleStatistics(UUID controlLoopElementId) throws PfModelException {
        // TODO Implement statistics functionality
    }
}
