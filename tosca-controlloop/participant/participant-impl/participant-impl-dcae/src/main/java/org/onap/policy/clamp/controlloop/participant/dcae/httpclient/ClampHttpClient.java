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

package org.onap.policy.clamp.controlloop.participant.dcae.httpclient;

import java.util.Map;
import org.apache.http.HttpStatus;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClampHttpClient extends AbstractHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClampHttpClient.class);

    private static final String STATUS = "/restservices/clds/v2/loop/getstatus/";
    private static final String CREATE = "/restservices/clds/v2/loop/create/%s?templateName=%s";
    private static final String UPDATE = "/restservices/clds/v2/loop/updateMicroservicePolicy/";
    private static final String DEPLOY = "/restservices/clds/v2/loop/deploy/";
    private static final String STOP = "/restservices/clds/v2/loop/stop/";
    private static final String DELETE = "/restservices/clds/v2/loop/delete/";
    private static final String UNDEPLOY = "/restservices/clds/v2/loop/undeploy/";

    /**
     * Constructor.
     */
    public ClampHttpClient(RestServerParameters restServerParameters) {
        super(restServerParameters);
    }

    /**
     * Create.
     *
     * @param loopName the loopName
     * @param templateName the templateName
     * @return the Map or null if error occurred
     */
    public Map<String, Object> create(String loopName, String templateName) {
        return executePost(String.format(CREATE, loopName, templateName), HttpStatus.SC_OK);
    }

    /**
     * Update.
     *
     * @param loopName the loopName
     * @param jsonEntity the Json entity
     * @return true
     */
    public boolean update(String loopName, String jsonEntity) {
        return executePost(UPDATE + loopName, HttpStatus.SC_OK) != null;
    }

    /**
     * Deploy.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean deploy(String loopName) { // DCAE
        return executePut(DEPLOY + loopName, HttpStatus.SC_ACCEPTED);
    }

    /**
     * Get Status.
     *
     * @param loopName the loopName
     * @return the Map or null if error occurred
     */
    public Map<String, Object> getstatus(String loopName) {
        return executeGet(STATUS + loopName, HttpStatus.SC_OK);
    }

    /**
     * Undeploy.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean undeploy(String loopName) {
        return executePut(UNDEPLOY + loopName, HttpStatus.SC_ACCEPTED);
    }

    /**
     * Stop.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean stop(String loopName) {
        return executePut(STOP + loopName, HttpStatus.SC_OK);
    }

    /**
     * Delete.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean delete(String loopName) {
        return executePut(DELETE + loopName, HttpStatus.SC_OK);
    }

    /**
     * return status from JSON mapped in a Map.
     *
     * @param map JSON mapped in a Map
     * @return status
     */
    public static String getStatusCode(Map<String, Object> map) {
        Map<String, Object> components = (Map<String, Object>) map.get("components");
        Map<String, Object> dcae = (Map<String, Object>) components.get("DCAE");
        Map<String, Object> componentState = (Map<String, Object>) dcae.get("componentState");
        return (String) componentState.get("stateName");
    }

    /**
     * Return policy id from JSON Object.
     *
     * @param map JSON mapped in a Map
     * @return policy id
     */
    public static String getPolicyId(Map<String, Object> map) {
        Map<String, Object> globalPropertiesJson = (Map<String, Object>) map.get("globalPropertiesJson");
        Map<String, Object> dcaeDeployParameters =
                (Map<String, Object>) globalPropertiesJson.get("dcaeDeployParameters");
        Map<String, Object> uniqueBlueprintParameters =
                (Map<String, Object>) dcaeDeployParameters.get("uniqueBlueprintParameters");
        return (String) uniqueBlueprintParameters.get("policy_id");
    }
}
