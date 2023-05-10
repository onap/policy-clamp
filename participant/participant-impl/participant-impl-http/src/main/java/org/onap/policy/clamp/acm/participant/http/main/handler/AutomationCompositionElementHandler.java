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

package org.onap.policy.clamp.acm.participant.http.main.handler;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.validation.Validation;
import javax.ws.rs.core.Response.Status;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.clamp.acm.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.acm.participant.http.main.webclient.AcHttpClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of automationCompositionElement updates.
 */
@Component
@RequiredArgsConstructor
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener, Closeable {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    private final AcHttpClient acHttpClient;

    /**
     * Handle a automation composition element state change.
     *
     * @param automationCompositionElementId the ID of the automation composition element
     */
    @Override
    public void undeploy(UUID automationCompositionId, UUID automationCompositionElementId) {
        intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, automationCompositionElementId,
                DeployState.UNDEPLOYED, null, "");
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
        var configRequest = getConfigRequest(properties);
        var restResponseMap = invokeHttpClient(configRequest);
        var failedResponseStatus = restResponseMap.values().stream()
                .filter(response -> !HttpStatus.valueOf(response.getKey()).is2xxSuccessful())
                .collect(Collectors.toList());
        if (failedResponseStatus.isEmpty()) {
            intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                    DeployState.DEPLOYED, null, "Deployed");
        } else {
            throw new PfModelException(Status.BAD_REQUEST, "Error on Invoking the http request: {}",
                    failedResponseStatus);
        }
    }

    private ConfigRequest getConfigRequest(Map<String, Object> properties) throws PfModelException {
        try {
            var configRequest = CODER.convert(properties, ConfigRequest.class);
            var violations = Validation.buildDefaultValidatorFactory().getValidator().validate(configRequest);
            if (!violations.isEmpty()) {
                LOGGER.error("Violations found in the config request parameters: {}", violations);
                throw new PfModelException(Status.BAD_REQUEST, "Constraint violations in the config request");
            }
            return configRequest;
        } catch (CoderException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error extracting ConfigRequest ", e);
        }
    }

    /**
     * Invoke a runnable thread to execute http requests.
     *
     * @param configRequest ConfigRequest
     */
    private Map<ToscaConceptIdentifier, Pair<Integer, String>> invokeHttpClient(ConfigRequest configRequest)
            throws PfModelException {
        try {
            Map<ToscaConceptIdentifier, Pair<Integer, String>> restResponseMap = new ConcurrentHashMap<>();
            // Invoke runnable thread to execute https requests of all config entities
            var result = executor.submit(() -> acHttpClient.run(configRequest, restResponseMap), restResponseMap);
            if (!result.get().isEmpty()) {
                LOGGER.debug("Http Request Completed: {}", result.isDone());
            }
            return restResponseMap;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PfModelException(Status.BAD_REQUEST, "Error invoking ExecutorService ", e);
        } catch (ExecutionException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error invoking the http request for the config ", e);
        }
    }

    /**
     * Closes this stream and releases any system resources associated
     * with it. If the stream is already closed then invoking this
     * method has no effect.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        executor.shutdown();
    }
}
