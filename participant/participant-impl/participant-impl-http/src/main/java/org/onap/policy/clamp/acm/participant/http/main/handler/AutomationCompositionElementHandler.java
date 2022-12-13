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
import javax.validation.ValidationException;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.clamp.acm.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.acm.participant.http.main.webclient.AcHttpClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.AutomationCompositionElementListener;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.clamp.models.acm.messages.dmaap.participant.ParticipantMessageType;
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
public class AutomationCompositionElementHandler implements AutomationCompositionElementListener, Closeable {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Map<ToscaConceptIdentifier, Pair<Integer, String>> restResponseMap = new ConcurrentHashMap<>();

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    /**
     * Handle a automation composition element state change.
     *
     * @param automationCompositionElementId the ID of the automation composition element
     * @param currentState the current state of the automation composition element
     * @param newState the state to which the automation composition element is changing to
     * @throws PfModelException in case of a model exception
     */
    @Override
    public void automationCompositionElementStateChange(UUID automationCompositionId,
        UUID automationCompositionElementId, AutomationCompositionState currentState,
        AutomationCompositionOrderedState newState) {
        switch (newState) {
            case UNINITIALISED:
                intermediaryApi.updateAutomationCompositionElementState(automationCompositionId,
                    automationCompositionElementId, newState, AutomationCompositionState.UNINITIALISED,
                    ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
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
     * @param automationCompositionId the automationComposition Id
     * @param element the information on the automation composition element
     * @param properties properties Map
     */
    @Override
    public void automationCompositionElementUpdate(UUID automationCompositionId,
        AutomationCompositionElement element, Map<String, Object> properties) {
        try {
            var configRequest = CODER.convert(properties, ConfigRequest.class);
            var violations =
                Validation.buildDefaultValidatorFactory().getValidator().validate(configRequest);
            if (violations.isEmpty()) {
                invokeHttpClient(configRequest);
                var failedResponseStatus = restResponseMap.values().stream()
                        .filter(response -> !HttpStatus.valueOf(response.getKey())
                        .is2xxSuccessful()).collect(Collectors.toList());
                if (failedResponseStatus.isEmpty()) {
                    intermediaryApi.updateAutomationCompositionElementState(automationCompositionId, element.getId(),
                            AutomationCompositionOrderedState.PASSIVE, AutomationCompositionState.PASSIVE,
                            ParticipantMessageType.AUTOMATION_COMPOSITION_STATE_CHANGE);
                } else {
                    LOGGER.error("Error on Invoking the http request: {}", failedResponseStatus);
                }
            } else {
                LOGGER.error("Violations found in the config request parameters: {}", violations);
                throw new ValidationException("Constraint violations in the config request");
            }
        } catch (CoderException | ValidationException | InterruptedException | ExecutionException e) {
            LOGGER.error("Error invoking the http request for the config ", e);
        }
    }

    /**
     * Invoke a runnable thread to execute http requests.
     *
     * @param configRequest ConfigRequest
     */
    public void invokeHttpClient(ConfigRequest configRequest) throws ExecutionException, InterruptedException {
        // Invoke runnable thread to execute https requests of all config entities
        var result = executor.submit(new AcHttpClient(configRequest, restResponseMap), restResponseMap);
        if (!result.get().isEmpty()) {
            LOGGER.debug("Http Request Completed: {}", result.isDone());
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
