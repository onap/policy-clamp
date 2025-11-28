/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.policy.main.utils;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.onap.policy.clamp.acm.participant.policy.concepts.DeploymentGroups;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Mock rest endpoints for api and pap servers.
 */
@Path("/")
@Produces("application/json")
public class MockRestEndpoint {

    /**
     * Dummy endpoint for create policy types.
     *
     * @param body tosca service template
     * @return the response
     */
    @Path("policy/api/v1/policytypes")
    @POST
    public Response createPolicyType(@RequestBody ToscaServiceTemplate body) {
        return Response.status(200).build();
    }

    /**
     * Dummy endpoint for create policies.
     *
     * @param body tosca service template
     * @return the response
     */
    @Path("policy/api/v1/policies")
    @POST
    public Response createPolicy(@RequestBody ToscaServiceTemplate body) {
        return Response.status(200).build();
    }

    /**
     * Dummy endpoint for delete policy types.
     *
     * @return the response
     */
    @Path("policy/api/v1/policytypes/{policyTypeId}/versions/{versionId}")
    @DELETE
    public Response deletePolicyType() {
        return Response.status(200).build();
    }

    /**
     * Dummy endpoint for delete policy.
     *
     * @return the response
     */
    @Path("policy/api/v1/policies/{policyId}/versions/{versionId}")
    @DELETE
    public Response deletePolicy() {
        return Response.status(200).build();
    }

    /**
     * Dummy endpoint for deploy/undeploy policy in pap.
     *
     * @param body pdp deployment group
     * @return the response
     */
    @Path("policy/pap/v1/pdps/deployments/batch")
    @POST
    public Response handlePolicyDeployOrUndeploy(@RequestBody DeploymentGroups body) {
        return Response.status(200).build();
    }

}
