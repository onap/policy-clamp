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

package org.onap.policy.clamp.acm.participant.http.utils;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * The Class MockRestEndpoint creates rest server endpoints for simulating Rest calls.
 */
@Path("/")
@Produces("application/json")
public class MockRestEndpoint {

    /**
     * Get dummy endpoint.
     *
     * @param name the name
     * @param version the version
     * @return the response
     */
    @Path("get")
    @GET
    public Response getMessages(@QueryParam("name") String name, @QueryParam("version") String version) {
        String createRequest = "dummy body";
        return Response.status(200).entity(List.of(createRequest)).build();
    }

    /**
     * Post dummy endpoint.
     *
     * @param name the name
     * @param version the version
     * @param jsonString the message
     * @return the response
     */
    @Path("post")
    @POST
    public Response policyMessage(@QueryParam("name") String name, @QueryParam("version") String version,
            final String jsonString) {
        return Response.status(200).build();
    }
}
