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

package org.onap.policy.clamp.controlloop.participant.policy.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.springframework.stereotype.Component;

@Component
public class PolicyPapHttpClient extends AbstractHttpClient {

    private static final String PAP_URI = "/policy/pap/v1/";

    /**
     * Constructor.
     *
     * @param parameters the policy participant parameters
     */
    public PolicyPapHttpClient(ParticipantPolicyParameters parameters) {
        super(parameters.getPolicyPapParameters());
    }

    /**
     * Deploy Policies.
     *
     * @param policyName the name of the policy to be deployed
     * @param policyVersion the version of the policy to be deployed
     * @return Response
     */
    public Response deployPolicy(final String policyName, final String policyVersion) {
        JsonObject body = new JsonObject();
        body.addProperty("policy-id", policyName);
        body.addProperty("policy-version", policyVersion);

        JsonArray arr = new JsonArray();
        arr.add(body);

        JsonObject data = new JsonObject();
        data.add("policies",  arr);
        return executePost(PAP_URI + "pdps/policies/", Entity.entity(data, MediaType.APPLICATION_JSON));
    }

    /**
     * Undeploy Policies.
     *
     * @param policyName the name of the policy to be undeployed
     * @return Response
     */
    public Response undeployPolicy(final String policyName) {
        return executeDelete(PAP_URI + "pdps/policies/" + policyName);
    }
}