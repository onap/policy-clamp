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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.onap.policy.common.endpoints.parameters.RestServerParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClampHttpClient extends AbstractHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClampHttpClient.class);

    private static final String STATUS = "/restservices/clds/v2/loop/getstatus/%s";
    private static final String CREATE = "/restservices/clds/v2/loop/create/%s?templateName=%s";
    private static final String UPDATE = "/restservices/clds/v2/loop/updateMicroservicePolicy/%s";
    private static final String DEPLOY = "/restservices/clds/v2/loop/deploy/%s";
    private static final String SUBMIT = "/restservices/clds/v2/loop/submit/%s";
    private static final String STOP = "/restservices/clds/v2/loop/stop/%s";
    private static final String DELETE = "/restservices/clds/v2/loop/delete/%s";
    private static final String UNDEPLOY = "/restservices/clds/v2/loop/undeploy/%s";

    /**
     * constructor.
     */
    public ClampHttpClient(RestServerParameters restServerParameters) {
        super(restServerParameters);
    }

    /**
     * Create.
     *
     * @param loopName the loopName
     * @param templateName th templateName
     * @return true
     */
    public JSONObject create(String loopName, String templateName) {
        try (CloseableHttpResponse response = execute(new HttpPost(String.format(CREATE, loopName, templateName)))) {
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            return new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Update.
     *
     * @param loopName the loopName
     * @param jsonEntity the Json entity
     * @return true
     */
    public boolean update(String loopName, String jsonEntity) {
        try {
            HttpPost post = new HttpPost(String.format(UPDATE, loopName));
            post.setEntity(new StringEntity(jsonEntity));
            try (CloseableHttpResponse response = execute(post)) {
                return response.getStatusLine().getStatusCode() == 200;
            }
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Submit.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean submit(String loopName) { // POLICY
        try (CloseableHttpResponse response = execute(new HttpPut(String.format(SUBMIT, loopName)))) {
            return response.getStatusLine().getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Deploy.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean deploy(String loopName) { // DCAE
        try (CloseableHttpResponse response = execute(new HttpPut(String.format(DEPLOY, loopName)))) {
            return response.getStatusLine().getStatusCode() == 202;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get Status.
     *
     * @param loopName the loopName
     * @return true
     * @throws Exception if
     */
    public JSONObject getstatus(String loopName) {
        try (CloseableHttpResponse response = execute(new HttpGet(String.format(STATUS, loopName)))) {
            if (response.getStatusLine().getStatusCode() != 200) {
                return null;
            }
            return new JSONObject(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Undeploy.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean undeploy(String loopName) {
        try (CloseableHttpResponse response = execute(new HttpPut(String.format(UNDEPLOY, loopName)))) {
            return response.getStatusLine().getStatusCode() == 202;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Stop.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean stop(String loopName) {
        try (CloseableHttpResponse response = execute(new HttpPut(String.format(STOP, loopName)))) {
            return response.getStatusLine().getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Delete.
     *
     * @param loopName the loopName
     * @return true
     */
    public boolean delete(String loopName) {
        try (CloseableHttpResponse response = execute(new HttpPut(String.format(DELETE, loopName)))) {
            return response.getStatusLine().getStatusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * return status from JSON Object.
     *
     * @param responseObj the JSON Object
     * @return status
     */
    public static String getStatusCode(JSONObject responseObj) {
        JSONObject components = responseObj.getJSONObject("components");
        JSONObject dcae = components.getJSONObject("DCAE");
        JSONObject componentState = dcae.getJSONObject("componentState");
        return componentState.getString("stateName");
    }

    /**
     * return policy id from JSON Object.
     *
     * @param responseObj JSONObject
     * @return policy id
     */
    public static String getPolicyId(JSONObject responseObj) {
        JSONObject globalPropertiesJson = responseObj.getJSONObject("globalPropertiesJson");
        JSONObject dcaeDeployParameters = globalPropertiesJson.getJSONObject("dcaeDeployParameters");
        JSONObject uniqueBlueprintParameters = dcaeDeployParameters.getJSONObject("uniqueBlueprintParameters");
        return uniqueBlueprintParameters.getString("policy_id");
    }

}
