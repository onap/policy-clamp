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

package org.onap.policy.clamp.controlloop.participant.dcae.main.handler;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.participant.dcae.httpclient.ClampHttpClient;
import org.onap.policy.clamp.controlloop.participant.dcae.httpclient.ConsulDcaeHttpClient;
import org.onap.policy.clamp.controlloop.participant.intermediary.api.ControlLoopElementListener;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles implementation of controlLoopElement updates.
 */
public class ControlLoopElementHandler implements ControlLoopElementListener, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControlLoopElementHandler.class);
    private final ClampHttpClient clampClient;
    private final ConsulDcaeHttpClient consulClient;

    private static final String LOOP = "pmsh_loop";
    private static final String TEMPLATE = "LOOP_TEMPLATE_k8s_pmsh";

    private static final String BLUEPRINT_DEPLOYED = "BLUEPRINT_DEPLOYED";
    private static final String MICROSERVICE_INSTALLED_SUCCESSFULLY = "MICROSERVICE_INSTALLED_SUCCESSFULLY";

    private static final String BODY_CONSUL =
            "{ \"subscription\": { \"subscriptionName\": \"subscriptiona\", \"administrativeState\": \"UNLOCKED\", "
                    + "\"fileBasedGP\": 15, \"fileLocation\": \"/pm/pm.xml\", \"nfFilter\": "
                    + "{ \"nfNames\": [ \"^pnf1.*\" ], \"modelInvariantIDs\": "
                    + "[ \"5845y423-g654-6fju-po78-8n53154532k6\", \"7129e420-d396-4efb-af02-6b83499b12f8\" ], "
                    + "\"modelVersionIDs\": [ \"e80a6ae3-cafd-4d24-850d-e14c084a5ca9\" ] }, \"measurementGroups\": "
                    + "[ { \"measurementGroup\": { \"measurementTypes\": [ { \"measurementType\": \"countera\" }, "
                    + "{ \"measurementType\": \"counterb\" } ], \"managedObjectDNsBasic\": [ { \"DN\": \"dna\" }, "
                    + "{ \"DN\": \"dnb\" } ] } }, { \"measurementGroup\": { \"measurementTypes\": "
                    + "[ { \"measurementType\": \"counterc\" }, { \"measurementType\": \"counterd\" } ], "
                    + "\"managedObjectDNsBasic\": " + "[ { \"DN\": \"dnc\" }, { \"DN\": \"dnd\" } ] } } ] } }";

    /**
     * constructor.
     */
    public ControlLoopElementHandler(RestServerParameters clampParameters, RestServerParameters consulParameters) {
        clampClient = new ClampHttpClient(clampParameters);
        consulClient = new ConsulDcaeHttpClient(consulParameters);
    }

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
                JSONObject json = clampClient.getstatus(LOOP);
                if (json != null) {
                    clampClient.undeploy(LOOP);
                    DcaeHandler.getInstance().getDcaeProvider().getIntermediaryApi()
                        .updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.UNINITIALISED);
                }
                break;
            case PASSIVE:
                DcaeHandler.getInstance().getDcaeProvider().getIntermediaryApi()
                    .updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.PASSIVE);
                break;
            case RUNNING:
                DcaeHandler.getInstance().getDcaeProvider().getIntermediaryApi()
                    .updateControlLoopElementState(controlLoopElementId, newState, ControlLoopState.RUNNING);
                break;
            default:
                LOGGER.debug("Unknown orderedstate {}", newState);
                break;
        }
    }

    private JSONObject getStatus() throws PfModelException {
        JSONObject json = clampClient.getstatus(LOOP);
        if (json == null) {
            json = clampClient.create(LOOP, TEMPLATE);
        }
        if (json == null) {
            throw new PfModelException(null, "");
        }
        return json;
    }

    private void deploy() throws PfModelException {
        if (!consulClient.deploy(BODY_CONSUL)) {
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
     * @param controlLoopDefinition toscaServiceTemplate
     * @throws PfModelException in case of an exception
     */
    @Override
    public void controlLoopElementUpdate(ControlLoopElement element, ToscaServiceTemplate controlLoopDefinition)
            throws PfModelException {
        try {
            JSONObject json = getStatus();

            if (BLUEPRINT_DEPLOYED.equals(ClampHttpClient.getStatusCode(json))) {
                deploy();
                boolean deployedFlag = false;
                for (int i = 0; i < 10; i++) {
                    //sleep 10 seconds
                    TimeUnit.SECONDS.sleep(10);
                    json = getStatus();
                    String status = ClampHttpClient.getStatusCode(json);
                    if (MICROSERVICE_INSTALLED_SUCCESSFULLY.equals(status)) {
                        DcaeHandler.getInstance().getDcaeProvider().getIntermediaryApi()
                            .updateControlLoopElementState(element.getId(), element.getOrderedState(),
                                            ControlLoopState.PASSIVE);
                        deployedFlag = true;
                        break;
                    }
                }
                if (!deployedFlag) {
                    DcaeHandler.getInstance().getDcaeProvider().getIntermediaryApi()
                        .updateControlLoopElementState(element.getId(), element.getOrderedState(),
                                      ControlLoopState.UNINITIALISED2PASSIVE);
                }
            }
        } catch (PfModelException e) {
            throw e;
        } catch (Exception e) {
            throw new PfModelException(null, e.getMessage());
        }
    }

    /**
     * Get controlLoopElement statistics.
     *
     * @param controlLoopElementId controlloop element id
     */
    @Override
    public void getClElementStatistics(UUID controlLoopElementId) {
        ControlLoopElement clElement = DcaeHandler.getInstance().getDcaeProvider()
                .getIntermediaryApi().getControlLoopElement(controlLoopElementId);
        if (clElement != null) {
            ClElementStatistics clElementStatistics = new ClElementStatistics();
            clElementStatistics.setControlLoopState(clElement.getState());
            clElementStatistics.setTimeStamp(Instant.now());
            DcaeHandler.getInstance().getDcaeProvider().getIntermediaryApi()
                .updateControlLoopElementStatistics(controlLoopElementId, clElementStatistics);
        }
    }

    @Override
    public void close() throws IOException {
        clampClient.close();
        consulClient.close();
    }
}
