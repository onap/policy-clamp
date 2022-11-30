/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.a1pms.utils;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * The Class MockRestEndpoint creates rest server endpoints for simulating Rest calls.
 */
@Path("/")
@Produces("application/json")
public class MockRestEndpoint {

    /**
     * Get dummy health endpoint.
     *
     * @return the response
     */
    @Path("/healthy")
    @GET
    public Response getApplicationHealthy() {
        return Response.status(200).entity("{}").build();
    }

    /**
     * Get dummy health endpoint.
     *
     * @return the response
     */
    @Path("/unhealthy")
    @GET
    public Response getApplicationUnHealthy() {
        return Response.status(500).entity("{}").build();
    }

    @Path("/services/success")
    @PUT
    public Response createServiceSuccess() {
        return Response.status(200).entity("{}").build();
    }

    @Path("/services/failure")
    @PUT
    public Response createServiceFailure() {
        return Response.status(500).entity("{}").build();
    }

    @Path("/service/success/{clientId}")
    @DELETE
    public Response deleteServiceSuccess(@PathParam("clientId") String clientId) {
        return Response.status(204).entity("{}").build();
    }

    @Path("/service/failure/{clientId}")
    @DELETE
    public Response deleteServiceFailure(@PathParam("clientId") String clientId) {
        return Response.status(500).entity("{}").build();
    }
}
