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

package org.onap.policy.clamp.controlloop.participant.policy.main.handler;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.Response;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.handler.ControlLoopHandler;
import org.onap.policy.clamp.controlloop.participant.intermediary.parameters.ParticipantIntermediaryParameters;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.common.utils.services.Registry;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;

/**
 * This class handles policy participant and control loop elements.
 *
 * <p/>It is effectively a singleton that is started at system start.
 */
public class PolicyHandler extends ControlLoopHandler {

    private final ParticipantIntermediaryParameters participantParameters;
    private final ParticipantPolicyParameters policyParameters;

    @Getter
    private PolicyProvider policyProvider;
    @Getter
    private PolicyModelsProvider databaseProvider;

    /**
     * Create a handler.
     *
     * @param parameters the parameters for access to the database
     * @throws PfModelException in case of an exception
     */
    public PolicyHandler(ParticipantPolicyParameters parameters) throws PfModelException {
        super(parameters.getDatabaseProviderParameters());
        participantParameters = parameters.getIntermediaryParameters();
        policyParameters = parameters;
    }

    public static PolicyHandler getInstance() {
        return Registry.get(PolicyHandler.class.getName());
    }

    @Override
    public Set<Class<?>> getProviderClasses() {
        return null;
    }

    @Override
    public void startProviders() {
        try {
            policyProvider = new PolicyProvider(participantParameters);
            databaseProvider = new PolicyModelsProviderFactory().createPolicyModelsProvider(
                                         policyParameters.getDatabaseProviderParameters());
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, "Start providers failed ", e);
        }
    }

    @Override
    public void stopProviders() {
        try {
            policyProvider.close();
        } catch (IOException e) {
            throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, "Stop providers failed ", e);
        }

        try {
            databaseProvider.close();
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, "Stop providers failed ", e);
        }
    }
}
