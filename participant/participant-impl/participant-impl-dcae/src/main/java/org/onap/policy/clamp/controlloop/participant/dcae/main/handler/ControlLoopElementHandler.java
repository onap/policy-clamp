/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.controlloop.participant.dcae.main.handler;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.Setter;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.messages.dmaap.participant.ParticipantMessageType;
import org.onap.policy.clamp.controlloop.participant.dcae.httpclient.ClampHttpClient;
import org.onap.policy.clamp.controlloop.participant.dcae.httpclient.ConsulDcaeHttpClient;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ParticipantDcaeParameters;
import org.onap.policy.clamp.controlloop.participant.dcae.model.Loop;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class handles implementation of controlLoopElement updates.
 */
@Component
public class ControlLoopElementHandler implements ControlLoopElementListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopElementHandler.class);
    private final ClampHttpClient clampClient;
    private final ConsulDcaeHttpClient consulClient;

    @Setter
    private ParticipantIntermediaryApi intermediaryApi;

    private static final String LOOP = "pmsh_loop";
    private static final String TEMPLATE = "LOOP_TEMPLATE_k8s_pmsh";
    private static final String POLICY = "policy";

    private static final String BLUEPRINT_DEPLOYED = "BLUEPRINT_DEPLOYED";
    private static final String MICROSERVICE_INSTALLED_SUCCESSFULLY = "MICROSERVICE_INSTALLED_SUCCESSFULLY";

    private int checkCount;
    private int secCount;

    private String bodyConsul;

    /**
     * Constructor.
     *
     * @param clampClient the CLAMP client
     * @param consulClient the Consul client
     */
    public ControlLoopElementHandler(ClampHttpClient clampClient, ConsulDcaeHttpClient consulClient,
            ParticipantDcaeParameters parameters) {
        this.clampClient = clampClient;
        this.consulClient = consulClient;
        this.checkCount = parameters.getCheckCount();
        this.secCount = parameters.getSecCount();
        bodyConsul = ResourceUtils.getResourceAsString(parameters.getJsonBodyConsulPath());
    }

    /**
     * Callback method to handle a control loop element state change.
     *
     * @param controlLoopElementId the ID of the control loop element
     * @param currentState the current state of the control loop element
     * @param newState the state to which the control loop element is changing to
     */
    @Override
    public void controlLoopElementStateChange(ToscaConceptIdentifier controlLoopId,
            UUID controlLoopElementId, ControlLoopState currentState,
            ControlLoopOrderedState newState) {
        switch (newState) {
            case UNINITIALISED:
                var loop = clampClient.getstatus(LOOP);
                if (loop != null) {
                    clampClient.undeploy(LOOP);
                    intermediaryApi.updateControlLoopElementState(controlLoopId,
                            controlLoopElementId, newState,
                            ControlLoopState.UNINITIALISED, ParticipantMessageType.CONTROL_LOOP_STATE_CHANGE);
                }
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
                LOGGER.debug("Unknown orderedstate {}", newState);
                break;
        }
    }

    private Loop getStatus() throws PfModelException {
        var loop = clampClient.getstatus(LOOP);
        if (loop == null) {
            loop = clampClient.create(LOOP, TEMPLATE);
        }
        if (loop == null) {
            throw new PfModelException(null, "");
        }
        return loop;
    }

    private void deploy() throws PfModelException {
        if (!consulClient.deploy(POLICY, bodyConsul)) {
            throw new PfModelException(null, "deploy to consul failed");
        }
        if (!clampClient.deploy(LOOP)) {
            throw new PfModelException(null, "deploy failed");
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
    public void controlLoopElementUpdate(ToscaConceptIdentifier controlLoopId,
            ControlLoopElement element, ToscaNodeTemplate nodeTemplate)
             throws PfModelException {
        try {
            var loop = getStatus();

            if (BLUEPRINT_DEPLOYED.equals(ClampHttpClient.getStatusCode(loop))) {
                deploy();
                var deployedFlag = false;
                for (var i = 0; i < checkCount; i++) {
                    // sleep 10 seconds
                    TimeUnit.SECONDS.sleep(secCount);
                    loop = getStatus();
                    String status = ClampHttpClient.getStatusCode(loop);
                    if (MICROSERVICE_INSTALLED_SUCCESSFULLY.equals(status)) {
                        intermediaryApi.updateControlLoopElementState(controlLoopId, element.getId(),
                            element.getOrderedState(), ControlLoopState.PASSIVE,
                            ParticipantMessageType.CONTROL_LOOP_UPDATE);
                        deployedFlag = true;
                        break;
                    }
                }
                if (!deployedFlag) {
                    LOGGER.warn("DCAE is not deployed properly, ClElement state will be UNINITIALISED2PASSIVE");
                    intermediaryApi.updateControlLoopElementState(controlLoopId, element.getId(),
                        element.getOrderedState(), ControlLoopState.UNINITIALISED2PASSIVE,
                        ParticipantMessageType.CONTROL_LOOP_UPDATE);
                }
            }
        } catch (PfModelException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PfModelException(null, e.getMessage(), e);
        } catch (Exception e) {
            throw new PfModelException(null, e.getMessage(), e);
        }
    }

    /**
     * Handle controlLoopElement statistics.
     *
     * @param controlLoopElementId controlloop element id
     */
    @Override
    public void handleStatistics(UUID controlLoopElementId) {
        var clElement = intermediaryApi.getControlLoopElement(controlLoopElementId);
        if (clElement != null) {
            var clElementStatistics = new ClElementStatistics();
            clElementStatistics.setControlLoopState(clElement.getState());
            clElementStatistics.setTimeStamp(Instant.now());
            intermediaryApi.updateControlLoopElementStatistics(controlLoopElementId, clElementStatistics);
        }
    }
}
