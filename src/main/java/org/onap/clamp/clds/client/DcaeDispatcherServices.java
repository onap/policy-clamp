/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Date;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.onap.clamp.clds.config.CldsReferenceProperties;
import org.onap.clamp.clds.exception.DcaeDeploymentException;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class implements the communication with DCAE for the service
 * deployments.
 */
@Component
public class DcaeDispatcherServices {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(DcaeDispatcherServices.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    @Autowired
    private CldsReferenceProperties refProp;
    private static final String STATUS_URL_LOG = "Status URL extracted: ";
    private static final String DCAE_URL_PREFIX = "/dcae-deployments/";
    private static final String DCAE_URL_PROPERTY_NAME = "DCAE_DISPATCHER_URL";
    private static final String DCAE_REQUEST_FAILED_LOG = "RequestFailed - responseStr=";
    public static final String DCAE_REQUESTID_PROPERTY_NAME = "dcae.header.requestId";
    private static final String DCAE_LINK_FIELD = "links";
    private static final String DCAE_STATUS_FIELD = "status";

    /**
     * Delete the deployment on DCAE.
     * 
     * @param deploymentId
     *            The deployment ID
     * @return Return the URL Status
     */
    public String deleteDeployment(String deploymentId) {
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "deleteDeployment");
        try {
            String url = refProp.getStringValue(DCAE_URL_PROPERTY_NAME) + DCAE_URL_PREFIX + deploymentId;
            String responseStr = DcaeHttpConnectionManager.doDcaeHttpQuery(url, "DELETE", null, null);
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(responseStr);
            JSONObject jsonObj = (JSONObject) obj0;
            JSONObject linksObj = (JSONObject) jsonObj.get(DCAE_LINK_FIELD);
            String statusUrl = (String) linksObj.get(DCAE_STATUS_FIELD);
            logger.info(STATUS_URL_LOG + statusUrl);
            LoggingUtils.setResponseContext("0", "Delete deployments success", this.getClass().getName());
            return statusUrl;
        } catch (Exception e) {
            // Log StatusCode during exception in metrics log
            LoggingUtils.setResponseContext("900", "Delete deployments failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Delete deployments error");
            logger.error("Exception occurred during Delete Deployment Operation with DCAE", e);
            throw new DcaeDeploymentException("Exception occurred during Delete Deployment Operation with DCAE", e);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("deleteDeployment complete");
        }
    }

    /**
     * Get the Operation Status from a specified URL.
     * 
     * @param statusUrl
     *            The URL provided by a previous DCAE Query
     * @return The status
     */
    public String getOperationStatus(String statusUrl) {
        // Assigning processing status to monitor operation status further
        String opStatus = "processing";
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "getOperationStatus");
        try {
            String responseStr = DcaeHttpConnectionManager.doDcaeHttpQuery(statusUrl, "GET", null, null);
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(responseStr);
            JSONObject jsonObj = (JSONObject) obj0;
            String operationType = (String) jsonObj.get("operationType");
            String status = (String) jsonObj.get("status");
            logger.info("Operation Type - " + operationType + ", Status " + status);
            LoggingUtils.setResponseContext("0", "Get operation status success", this.getClass().getName());
            opStatus = status;
        } catch (Exception e) {
            // Log StatusCode during exception in metrics log
            LoggingUtils.setResponseContext("900", "Get operation status failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Get operation status error");
            logger.error("Exception occurred during getOperationStatus Operation with DCAE", e);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("getOperationStatus complete");
        }
        return opStatus;
    }

    /**
     * This method send a getDeployments operation to DCAE.
     */
    public void getDeployments() {
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "getDeployments");
        try {
            String url = refProp.getStringValue(DCAE_URL_PROPERTY_NAME) + DCAE_URL_PREFIX;
            DcaeHttpConnectionManager.doDcaeHttpQuery(url, "GET", null, null);
            LoggingUtils.setResponseContext("0", "Get deployments success", this.getClass().getName());
        } catch (Exception e) {
            // Log StatusCode during exception in metrics log
            LoggingUtils.setResponseContext("900", "Get deployments failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Get deployments error");
            logger.error("Exception occurred during getDeployments Operation with DCAE", e);
            throw new DcaeDeploymentException("Exception occurred during getDeployments Operation with DCAE", e);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("getDeployments complete");
        }
    }

    /**
     * Returns status URL for createNewDeployment operation.
     *
     * @param deploymentId
     *            The deployment ID
     * @param serviceTypeId
     *            Service type ID
     * @return The status URL
     */
    public String createNewDeployment(String deploymentId, String serviceTypeId) {
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "createNewDeployment");
        try {
            ObjectNode rootNode = (ObjectNode) refProp.getJsonTemplate("dcae.deployment.template");
            rootNode.put("serviceTypeId", serviceTypeId);
            String apiBodyString = rootNode.toString();
            logger.info("Dcae api Body String - " + apiBodyString);
            String url = refProp.getStringValue(DCAE_URL_PROPERTY_NAME) + DCAE_URL_PREFIX + deploymentId;
            String responseStr = DcaeHttpConnectionManager.doDcaeHttpQuery(url, "PUT", apiBodyString,
                    "application/json");
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(responseStr);
            JSONObject jsonObj = (JSONObject) obj0;
            JSONObject linksObj = (JSONObject) jsonObj.get(DCAE_LINK_FIELD);
            String statusUrl = (String) linksObj.get(DCAE_STATUS_FIELD);
            logger.info(STATUS_URL_LOG + statusUrl);
            LoggingUtils.setResponseContext("0", "Create new deployment failed", this.getClass().getName());
            return statusUrl;
        } catch (Exception e) {
            // Log StatusCode during exception in metrics log
            LoggingUtils.setResponseContext("900", "Create new deployment failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Create new deployment error");
            logger.error("Exception occurred during createNewDeployment Operation with DCAE", e);
            throw new DcaeDeploymentException("Exception occurred during createNewDeployment Operation with DCAE", e);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("createNewDeployment complete");
        }
    }

    /**
     * Returns status URL for deleteExistingDeployment operation.
     * 
     * @param deploymentId
     *            The deployment ID
     * @param serviceTypeId
     *            The service Type ID
     * @return The status URL
     */
    public String deleteExistingDeployment(String deploymentId, String serviceTypeId) {
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "deleteExistingDeployment");
        try {
            String apiBodyString = "{\"serviceTypeId\": \"" + serviceTypeId + "\"}";
            logger.info("Dcae api Body String - " + apiBodyString);
            String url = refProp.getStringValue(DCAE_URL_PROPERTY_NAME) + DCAE_URL_PREFIX + deploymentId;
            String responseStr = DcaeHttpConnectionManager.doDcaeHttpQuery(url, "DELETE", apiBodyString,
                    "application/json");
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(responseStr);
            JSONObject jsonObj = (JSONObject) obj0;
            JSONObject linksObj = (JSONObject) jsonObj.get(DCAE_LINK_FIELD);
            String statusUrl = (String) linksObj.get(DCAE_STATUS_FIELD);
            logger.info(STATUS_URL_LOG + statusUrl);
            LoggingUtils.setResponseContext("0", "Delete existing deployment success", this.getClass().getName());
            return statusUrl;
        } catch (Exception e) {
            // Log StatusCode during exception in metrics log
            LoggingUtils.setResponseContext("900", "Delete existing deployment failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Delete existing deployment error");
            logger.error("Exception occurred during deleteExistingDeployment Operation with DCAE", e);
            throw new DcaeDeploymentException("Exception occurred during deleteExistingDeployment Operation with DCAE",
                    e);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("deleteExistingDeployment complete");
        }
    }
}