/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation.
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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV3;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.helm.PodStatusValidator;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.service.ChartService;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
public class AutomationCompositionElementHandler extends AcElementListenerV3 {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Coder CODER = new StandardCoder();

    private final ChartService chartService;

    public AutomationCompositionElementHandler(ParticipantIntermediaryApi intermediaryApi, ChartService chartService) {
        super(intermediaryApi);
        this.chartService = chartService;
    }


    // Default thread config values
    private static class ThreadConfig {
        private int uninitializedToPassiveTimeout = 60;
        private int podStatusCheckInterval = 30;
    }

    /**
     * Handle an undeploy on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement    the information of the Automation Composition Instance Element
     * @throws PfModelException in case of a model exception
     */
    @Override
    public void undeploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
            throws PfModelException {

        var chart = getChartInfo(instanceElement.inProperties());
        if (chart != null) {
            LOGGER.info("Helm deployment to be deleted {} ", chart.getReleaseName());
            try {
                chartService.uninstallChart(chart);
                intermediaryApi.updateAutomationCompositionElementState(instanceElement.instanceId(),
                        instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                        "Undeployed");
                instanceElement.outProperties().remove(chart.getReleaseName());
                intermediaryApi.sendAcElementInfo(instanceElement.instanceId(), instanceElement.elementId(),
                        null, null, instanceElement.outProperties());
            } catch (ServiceException se) {
                throw new PfModelException(Status.EXPECTATION_FAILED, "Deletion of Helm deployment failed", se);
            }
        }

    }

    /**
     * Handle a deploy on a automation composition element.
     *
     * @param compositionElement the information of the Automation Composition Definition Element
     * @param instanceElement    the information of the Automation Composition Instance Element
     * @throws PfModelException from Policy framework
     */
    @Override
    public void deploy(CompositionElementDto compositionElement, InstanceElementDto instanceElement)
            throws PfModelException {
        try {
            var chartInfo = getChartInfo(instanceElement.inProperties());
            if (chartService.installChart(chartInfo)) {
                var config = getThreadConfig(compositionElement.inProperties());
                checkPodStatus(instanceElement.instanceId(), instanceElement.elementId(), chartInfo,
                        config.uninitializedToPassiveTimeout, config.podStatusCheckInterval, instanceElement);
            } else {
                throw new PfModelException(Response.Status.BAD_REQUEST, "Installation of Helm chart failed ");
            }
        } catch (ServiceException | IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PfModelException(Response.Status.BAD_REQUEST, "Installation of Helm chart failed ", e);
        }

    }

    private ThreadConfig getThreadConfig(Map<String, Object> properties) throws PfModelException {
        try {
            return CODER.convert(properties, ThreadConfig.class);
        } catch (CoderException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error extracting ThreadConfig ", e);
        }
    }

    private ChartInfo getChartInfo(Map<String, Object> properties) throws PfModelException {
        @SuppressWarnings("unchecked")
        var chartData = (Map<String, Object>) properties.get("chart");
        LOGGER.info("Installation request received for the Helm Chart {} ", chartData);
        try {
            return CODER.convert(chartData, ChartInfo.class);
        } catch (CoderException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error extracting ChartInfo", e);
        }

    }

    /**
     * Invoke a new thread to check the status of deployed pods.
     *
     * @param chart ChartInfo
     * @throws PfModelException in case of an exception
     */
    public void checkPodStatus(UUID automationCompositionId, UUID elementId, ChartInfo chart, int timeout,
            int podStatusCheckInterval, InstanceElementDto instanceElement) throws InterruptedException,
            PfModelException {

        var result = new PodStatusValidator(chart, timeout, podStatusCheckInterval);
        result.run();
        LOGGER.info("Pod Status Validator Completed");
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
        instanceElement.outProperties().put(chart.getReleaseName(), "Running");

        intermediaryApi.sendAcElementInfo(automationCompositionId, elementId, null, null,
                instanceElement.outProperties());

    }
}
