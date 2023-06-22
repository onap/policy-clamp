/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import lombok.AccessLevel;
import lombok.Getter;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.helm.PodStatusValidator;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.service.ChartService;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElementDefinition;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
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
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Map of helm installation and the status of corresponding pods
    @Getter
    private static Map<String, Map<String, String>> podStatusMap = new ConcurrentHashMap<>();
    private static final Coder CODER = new StandardCoder();

    @Autowired
    private ChartService chartService;

    @Autowired
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
     */
    @Override
    public synchronized void undeploy(UUID automationCompositionId, UUID automationCompositionElementId) {
        var chart = chartMap.get(automationCompositionElementId);
        if (chart != null) {
            LOGGER.info("Helm deployment to be deleted {} ", chart.getReleaseName());
            try {
                chartService.uninstallChart(chart);
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                        automationCompositionElementId, DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                        "Undeployed");
                chartMap.remove(automationCompositionElementId);
                podStatusMap.remove(chart.getReleaseName());
            } catch (ServiceException se) {
                LOGGER.warn("Deletion of Helm deployment failed", se);
            }
        }
    }

    /**
     * Callback method to handle an update on a automation composition element.
     *
     * @param automationCompositionId the automationComposition Id
     * @param element the information on the automation composition element
     * @param properties properties Map
     * @throws PfModelException in case of an exception
     */
    @Override
    public synchronized void deploy(UUID automationCompositionId, AcElementDeploy element,
            Map<String, Object> properties) throws PfModelException {

        try {
            var chartInfo = getChartInfo(properties);
            if (chartService.installChart(chartInfo)) {
                chartMap.put(element.getId(), chartInfo);

                var config = getThreadConfig(properties);
                checkPodStatus(automationCompositionId, element.getId(), chartInfo,
                        config.uninitializedToPassiveTimeout, config.podStatusCheckInterval);
            } else {
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                        DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, "Chart not installed");
            }
        } catch (ServiceException | IOException e) {
            throw new PfModelException(Response.Status.BAD_REQUEST, "Installation of Helm chart failed ", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PfModelException(Response.Status.BAD_REQUEST, "Error invoking ExecutorService ", e);
        } catch (AutomationCompositionException e) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, e.getMessage());
        }
    }

    private ThreadConfig getThreadConfig(Map<String, Object> properties) throws AutomationCompositionException {
        try {
            return CODER.convert(properties, ThreadConfig.class);
        } catch (CoderException e) {
            throw new AutomationCompositionException(Status.BAD_REQUEST, "Error extracting ThreadConfig ", e);
        }
    }

    private ChartInfo getChartInfo(Map<String, Object> properties) throws AutomationCompositionException {
        @SuppressWarnings("unchecked")
        var chartData = (Map<String, Object>) properties.get("chart");

        LOGGER.info("Installation request received for the Helm Chart {} ", chartData);
        try {
            return CODER.convert(chartData, ChartInfo.class);
        } catch (CoderException e) {
            throw new AutomationCompositionException(Status.BAD_REQUEST, "Error extracting ChartInfo ", e);
        }
    }

    /**
     * Invoke a new thread to check the status of deployed pods.
     *
     * @param chart ChartInfo
     * @throws ServiceException in case of an exception
     */
    public void checkPodStatus(UUID automationCompositionId, UUID elementId, ChartInfo chart, int timeout,
            int podStatusCheckInterval) throws InterruptedException, ServiceException {

        var result = new PodStatusValidator(chart, timeout, podStatusCheckInterval);
        result.run();
        LOGGER.info("Pod Status Validator Completed");
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, elementId,
                DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");

    }

    @Override
    public void lock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.LOCKED,
                StateChangeResult.NO_ERROR, "Locked");
    }

    @Override
    public void unlock(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, null, LockState.UNLOCKED,
                StateChangeResult.NO_ERROR, "Unlocked");
    }

    @Override
    public void delete(UUID instanceId, UUID elementId) throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, elementId, DeployState.DELETED, null,
                StateChangeResult.NO_ERROR, "Deleted");
    }

    @Override
    public void update(UUID instanceId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        intermediaryApi.updateAutomationCompositionElementState(instanceId, element.getId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Update not supported");
    }

    @Override
    public void prime(UUID compositionId, List<AutomationCompositionElementDefinition> elementDefinitionList)
            throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.PRIMED, StateChangeResult.NO_ERROR, "Primed");
    }

    @Override
    public void deprime(UUID compositionId) throws PfModelException {
        intermediaryApi.updateCompositionState(compositionId, AcTypeState.COMMISSIONED, StateChangeResult.NO_ERROR,
                "Deprimed");
    }
}
