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

package org.onap.policy.clamp.acm.participant.policy.client;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.stereotype.Component;

@Component
public class PolicyApiHttpClient extends AbstractHttpClient {

    private static final String POLICY_URI = "/policy/api/v1/";

    /**
     * Constructor.
     *
     * @param parameters the policy participant parameters
     */
    public PolicyApiHttpClient(ParticipantPolicyParameters parameters) {
        super(parameters.getPolicyApiParameters());
    }

    /**
     * Create Policy Types.
     *
     * @param toscaServiceTemplate the whole ToscaServiceTemplate
     * @return Response
     * @throws PfModelException on errors creating the policy type
     */
    public Response createPolicyType(ToscaServiceTemplate toscaServiceTemplate) throws PfModelException {
        return executePost(POLICY_URI + "policytypes", Entity.entity(toscaServiceTemplate, MediaType.APPLICATION_JSON));
    }

    /**
     * Create Policies.
     *
     * @param toscaServiceTemplate the whole ToscaServiceTemplate
     * @return Response
     */
    public Response createPolicy(final ToscaServiceTemplate toscaServiceTemplate) {
        return executePost(POLICY_URI + "policies", Entity.entity(toscaServiceTemplate, MediaType.APPLICATION_JSON));
    }

    /**
     * Delete Policies.
     *
     * @param policyName the name of the policy to be deleted
     * @param policyVersion the version of the policy to be deleted
     * @return Response
     */
    public Response deletePolicy(final String policyName, final String policyVersion) {
        return executeDelete(POLICY_URI + "policies/" + policyName + "/versions/" + policyVersion);
    }

    /**
     * Delete Policy types.
     *
     * @param policyTypeName the name of the policy to be deleted
     * @param policyTypeVersion the version of the policy to be deleted
     * @return Response
     */
    public Response deletePolicyType(final String policyTypeName, final String policyTypeVersion) {
        return executeDelete(POLICY_URI + "policytypes/" + policyTypeName + "/versions/" + policyTypeVersion);
    }
}
