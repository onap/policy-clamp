/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.service.rs;

import io.swagger.annotations.Api;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;


/**
 * Service to invoke example Camunda process.
 * <p>
 * Try testing by using:
 * http://[hostname]:[serverPort]/jaxrsservices/log/log-message/your-message-here
 */
@Api(value = "/log")
@Path("/log")
@Produces({MediaType.TEXT_PLAIN})
public interface JaxrsLogService {

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @param logMessageText
     * @return output from service - comment on what was done
     */
    @GET
    @Path("/log-message/{logMessageText}")
    @Produces(MediaType.TEXT_PLAIN)
    String logMessage(@PathParam("logMessageText") String logMessageText, @QueryParam("javamail") String javamail, @QueryParam("springmail") String springmail, @QueryParam("commonsmail") String commonsmail);

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @return output from service - comment on what was done
     */
    @POST
    @Path("/postLogHist")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    String postLogMessage(String histEventList);

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @param startTime
     * @param endTime
     * @param serviceName
     * @return output from service - comment on what was done
     */
    @GET
    @Path("/createLog/{startTime}/{endTime}/{serviceName}")
    @Produces(MediaType.TEXT_PLAIN)
    String createLogMessage(@PathParam("startTime") String startTime, @PathParam("endTime") String endTime, @PathParam("serviceName") String serviceName);

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @param procInstId
     * @param histEventList
     * @return output from service - comment on what was done
     */
    @GET
    @Path("/createLogHist/{procInstId}/{histEventList}")
    @Produces(MediaType.TEXT_PLAIN)
    String createLogMessageUsingHistory(@PathParam("procInstId") String procInstId, @PathParam("histEventList") String histEventList);

    /**
     * REST service that executes example camunda process to log input message.
     *
     * @param procInstId
     * @return output from service - comment on what was done
     */
    @GET
    @Path("/histLog/{procInstId}")
    @Produces(MediaType.TEXT_PLAIN)
    String CreateHistLog(@PathParam("procInstId") String procInstId);

}
