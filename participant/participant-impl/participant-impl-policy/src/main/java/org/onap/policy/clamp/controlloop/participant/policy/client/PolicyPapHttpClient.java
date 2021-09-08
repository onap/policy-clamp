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
    private final String pdpGroup;
    private final String pdpType;

    /**
     * Constructor.
     *
     * @param parameters the policy participant parameters
     */
    public PolicyPapHttpClient(ParticipantPolicyParameters parameters) {
        super(parameters.getPolicyPapParameters());
        this.pdpGroup = parameters.getPdpGroup();
        this.pdpType = parameters.getPdpType();
    }

    /**
     * Deploy or undeploy Policies.
     *
     * @param policyName the name of the policy to be deployed/undeployed
     * @param policyVersion the version of the policy to be deployed/undeployed
     * @param action the action to deploy/undeploy policy
     * @return Response
     */
    public Response handlePolicyDeployOrUndeploy(final String policyName, final String policyVersion,
                                                 final String action) {
        // policies
        JsonObject policyArrayBody = new JsonObject();
        policyArrayBody.addProperty("name", policyName);
        policyArrayBody.addProperty("version", policyVersion);
        JsonArray policyArr = new JsonArray();
        policyArr.add(policyArrayBody);

        // deploymentSubgroups
        JsonObject deploymentSubGrpBody = new JsonObject();
        deploymentSubGrpBody.addProperty("pdpType", pdpType);
        deploymentSubGrpBody.addProperty("action", action);
        deploymentSubGrpBody.add("policies", policyArr);
        JsonArray deployArr = new JsonArray();
        deployArr.add(deploymentSubGrpBody);

        // groups
        JsonObject groupArrayBody = new JsonObject();
        groupArrayBody.addProperty("name", pdpGroup);
        groupArrayBody.add("deploymentSubgroups", deployArr);
        JsonArray groupArr = new JsonArray();
        groupArr.add(groupArrayBody);

        // main json
        JsonObject mainJson = new JsonObject();
        mainJson.add("groups", groupArr);

        return executePost(PAP_URI + "pdps/deployments/batch", Entity.entity(mainJson, MediaType.APPLICATION_JSON));
    }
}
