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

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidatorFactory;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
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
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of controlLoopElement updates.
 */
@Component
public class ControlLoopElementHandler implements ControlLoopElementListener {

    private static final Coder CODER = new StandardCoder();

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Getter
    private static Map<ToscaConceptIdentifier, Pair<Integer, String>> restResponseMap = new ConcurrentHashMap<>();

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
    public void controlLoopElementStateChange(UUID controlLoopElementId, ControlLoopState currentState,
                                              ControlLoopOrderedState newState) throws PfModelException {
        // Implementation not needed for http participant
    }

    /**
     * Callback method to handle an update on a control loop element.
     *
     * @param element the information on the control loop element
     * @param controlLoopDefinition toscaServiceTemplate
     */
    @Override
    public void controlLoopElementUpdate(ControlLoopElement element, ToscaServiceTemplate controlLoopDefinition) {

        for (Map.Entry<String, ToscaNodeTemplate> nodeTemplate : controlLoopDefinition.getToscaTopologyTemplate()
            .getNodeTemplates().entrySet()) {
            // Fetching the node template of corresponding CL element
            if (element.getDefinition().getName().equals(nodeTemplate.getKey())) {
                try {
                    var configRequest = CODER.convert(nodeTemplate.getValue().getProperties(), ConfigRequest.class);
                    Set<ConstraintViolation<ConfigRequest>> violations = Validation.buildDefaultValidatorFactory()
                        .getValidator().validate(configRequest);
                    if (violations.isEmpty()) {
                        invokeHttpClient(configRequest);
                    } else {
                        LOGGER.error("Violations found in the config request parameters: {}", violations);
                        throw new ValidationException("Constraint violations in the config request");
                    }
                } catch (CoderException | ValidationException e) {
                    LOGGER.error("Error invoking the http request for the config ", e);
                }
            }
        }
    }

    /**
     * Invoke a runnable thread to execute http requests.
     * @param configRequest ConfigRequest
     */
    public void invokeHttpClient(ConfigRequest configRequest) {
        // Invoke runnable thread to execute https requests of all config entities
        var runnableThread = new Thread(new ClHttpClient(configRequest));
        runnableThread.start();
    }
}
