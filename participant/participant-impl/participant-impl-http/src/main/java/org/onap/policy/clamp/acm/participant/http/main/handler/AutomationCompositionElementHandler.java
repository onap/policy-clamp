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

package org.onap.policy.clamp.acm.participant.http.main.handler;

import jakarta.validation.Validation;
import jakarta.ws.rs.core.Response.Status;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.UUID;
import org.onap.policy.clamp.acm.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.acm.participant.http.main.webclient.AcHttpClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.intermediary.api.impl.AcElementListenerV1;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionException;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
public class AutomationCompositionElementHandler extends AcElementListenerV1 {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AcHttpClient acHttpClient;

    public AutomationCompositionElementHandler(ParticipantIntermediaryApi intermediaryApi, AcHttpClient acHttpClient) {
        super(intermediaryApi);
        this.acHttpClient = acHttpClient;
    }

    /**
     * Handle a automation composition element state change.
     *
     * @param automationCompositionElementId the ID of the automation composition element
     */
    @Override
    public void undeploy(UUID automationCompositionId, UUID automationCompositionElementId) {
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, automationCompositionElementId,
                DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "");
    }

    /**
     * Callback method to handle an update on a automation composition element.
     *
     * @param automationCompositionId the automationComposition Id
     * @param element the information on the automation composition element
     * @param properties properties Map
     * @throws PfModelException in case of a exception
     */
    @Override
    public void deploy(UUID automationCompositionId, AcElementDeploy element, Map<String, Object> properties)
            throws PfModelException {
        try {
            var configRequest = getConfigRequest(properties);
            var restResponseMap = acHttpClient.run(configRequest);
            var failedResponseStatus = restResponseMap.values().stream()
                    .filter(response -> !HttpStatus.valueOf(response.getKey()).is2xxSuccessful())
                    .toList();
            if (failedResponseStatus.isEmpty()) {
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                        DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");
            } else {
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                        DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                        "Error on Invoking the http request: " + failedResponseStatus);
            }
        } catch (AutomationCompositionException e) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.UNDEPLOYED, null, StateChangeResult.FAILED, e.getMessage());
        }
    }

    private ConfigRequest getConfigRequest(Map<String, Object> properties) throws AutomationCompositionException {
        try {
            var configRequest = CODER.convert(properties, ConfigRequest.class);
            var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(configRequest);
            if (!violations.isEmpty()) {
                LOGGER.error("Violations found in the config request parameters: {}", violations);
                throw new AutomationCompositionException(Status.BAD_REQUEST,
                        "Constraint violations in the config request");
            }
            return configRequest;
        } catch (CoderException e) {
            throw new AutomationCompositionException(Status.BAD_REQUEST, "Error extracting ConfigRequest ", e);
        }
    }
}
