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

package org.onap.policy.clamp.controlloop.participant.http.main.handler;

import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.controlloop.participant.http.main.webclient.ClHttpClient;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of controlLoopElement updates.
 */
@Component
public class ControlLoopElementHandler implements ControlLoopElementListener, Closeable {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private Map<ToscaConceptIdentifier, Pair<Integer, String>> restResponseMap = new ConcurrentHashMap<>();

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    /**
     * Handle controlLoopElement statistics.
     *
     * @param controlLoopElementId controlloop element id
     */
    @Override
    public void handleStatistics(UUID controlLoopElementId) throws PfModelException {
        // Implementation not needed for http participant

    }

    /**
     * Handle a control loop element state change.
     *
     * @param controlLoopElementId the ID of the control loop element
     * @param currentState         the current state of the control loop element
     * @param newState             the state to which the control loop element is changing to
     * @throws PfModelException in case of a model exception
     */
    @Override
    public void controlLoopElementStateChange(ToscaConceptIdentifier controlLoopId, UUID controlLoopElementId,
            ControlLoopState currentState, ControlLoopOrderedState newState) throws PfModelException {
        switch (newState) {
            case UNINITIALISED:
                intermediaryApi.updateControlLoopElementState(controlLoopId,
                        controlLoopElementId, newState, ControlLoopState.UNINITIALISED,
                        ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
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
     */
    @Override
    public void controlLoopElementUpdate(ToscaConceptIdentifier controlLoopId, ControlLoopElement element,
            ToscaNodeTemplate nodeTemplate) {
        try {
            var configRequest = CODER.convert(nodeTemplate.getProperties(), ConfigRequest.class);
            Set<ConstraintViolation<ConfigRequest>> violations = Validation.buildDefaultValidatorFactory()
                .getValidator().validate(configRequest);
            if (violations.isEmpty()) {
                invokeHttpClient(configRequest);
                List<Pair<Integer, String>> failedResponseStatus = restResponseMap.values().stream()
                        .filter(response -> !HttpStatus.valueOf(response.getKey())
                        .is2xxSuccessful()).collect(Collectors.toList());
                if (failedResponseStatus.isEmpty()) {
                    intermediaryApi.updateControlLoopElementState(controlLoopId, element.getId(),
                            ControlLoopOrderedState.PASSIVE, ControlLoopState.PASSIVE,
                            ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
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
     * @param configRequest ConfigRequest
     */
    public void invokeHttpClient(ConfigRequest configRequest) throws ExecutionException, InterruptedException {
        // Invoke runnable thread to execute https requests of all config entities
        Future<Map> result = executor.submit(new ClHttpClient(configRequest, restResponseMap), restResponseMap);
        if (!result.get().isEmpty()) {
            LOGGER.debug("API Call Completed: {}", result.isDone());
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
