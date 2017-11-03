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

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.BadRequestException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.onap.clamp.clds.exception.DcaeDeploymentException;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class implements the communication with DCAE for the service
 * deployments.
 *
 */
public class DcaeDispatcherServices {
    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(DcaeDispatcherServices.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Autowired
    private RefProp                   refProp;

    /**
     * Delete the deployment on DCAE.
     * 
     * @param deploymentId
     *            The deployment ID
     * @return Return the URL Status
     * @throws IOException
     *             In case of issues with the Stream
     */
    public String deleteDeployment(String deploymentId) throws IOException {

        String statusUrl = null;
        InputStream in = null;
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "deleteDeployment");
        try {
            String url = refProp.getStringValue("DCAE_DISPATCHER_URL") + "/dcae-deployments/" + deploymentId;
            logger.info("Dcae Dispatcher url - " + url);
            URL obj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
            conn.setRequestProperty("X-ECOMP-RequestID", LoggingUtils.getRequestId());
            conn.setRequestMethod("DELETE");
            int responseCode = conn.getResponseCode();

            boolean requestFailed = true;
            logger.info("responseCode=" + responseCode);
            if (responseCode == 200 || responseCode == 202) {
                requestFailed = false;
            }

            InputStream inStream = conn.getErrorStream();
            if (inStream == null) {
                inStream = conn.getInputStream();
            }

            String responseStr = null;
            if (inStream != null) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inStream));
                String inputLine = null;
                StringBuilder response = new StringBuilder();
                while ((inputLine = bufferedReader.readLine()) != null) {
                    response.append(inputLine);
                }
                responseStr = response.toString();
            }

            if (responseStr != null && requestFailed) {
                logger.error("requestFailed - responseStr=" + responseStr);
                throw new BadRequestException(responseStr);
            }

            logger.debug("response code " + responseCode);
            in = conn.getInputStream();
            logger.debug("res:" + responseStr);
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(responseStr);
            JSONObject jsonObj = (JSONObject) obj0;
            JSONObject linksObj = (JSONObject) jsonObj.get("links");
            statusUrl = (String) linksObj.get("status");
            logger.debug("Status URL: " + statusUrl);

        } catch (Exception e) {
            logger.error("Exception occurred during Delete Deployment Operation with DCAE", e);
            throw new DcaeDeploymentException("Exception occurred during Delete Deployment Operation with DCAE", e);
        } finally {
            if (in != null) {
                in.close();
            }
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("deleteDeployment complete");
        }

        return statusUrl;

    }

    /**
     * Get the Operation Status from a specified URL.
     * 
     * @param statusUrl
     *            The URL provided by a previous DCAE Query
     * @return The status
     * @throws IOException
     *             In case of issues with the Stream
     * 
     */
    public String getOperationStatus(String statusUrl) throws IOException {

        // Assigning processing status to monitor operation status further
        String opStatus = "processing";
        InputStream in = null;
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "getOperationStatus");
        try {
            URL obj = new URL(statusUrl);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-ECOMP-RequestID", LoggingUtils.getRequestId());
            int responseCode = conn.getResponseCode();
            logger.debug("Deployment operation status response code - " + responseCode);
            if (responseCode == 200) {
                in = conn.getInputStream();
                String res = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
                JSONParser parser = new JSONParser();
                Object obj0 = parser.parse(res);
                JSONObject jsonObj = (JSONObject) obj0;
                String operationType = (String) jsonObj.get("operationType");
                String status = (String) jsonObj.get("status");
                logger.debug("Operation Type - " + operationType + ", Status " + status);
                opStatus = status;
            }
        } catch (Exception e) {
            logger.error("Exception occurred during getOperationStatus Operation with DCAE", e);
            logger.debug(e.getMessage()
                    + " : got exception while retrieving status, trying again until we get 200 response code");
        } finally {
            if (in != null) {
                in.close();
            }
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("getOperationStatus complete");
        }
        return opStatus;
    }

    /**
     * This method send a getDeployments operation to DCAE.
     * 
     * @throws IOException
     *             In case of issues with the Stream
     */
    public void getDeployments() throws IOException {
        InputStream in = null;
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "getDeployments");
        try {
            String url = refProp.getStringValue("DCAE_DISPATCHER_URL") + "/dcae-deployments";
            logger.info("Dcae Dispatcher deployments url - " + url);
            URL obj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-ECOMP-RequestID", LoggingUtils.getRequestId());
            int responseCode = conn.getResponseCode();
            logger.debug("response code " + responseCode);
            in = conn.getInputStream();
            String res = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
            logger.debug("res:" + res);
        } catch (Exception e) {
            logger.error("Exception occurred during getDeployments Operation with DCAE", e);
            throw new DcaeDeploymentException("Exception occurred during getDeployments Operation with DCAE", e);
        } finally {
            if (in != null) {
                in.close();
            }
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
     * @throws IOException
     *             In case of issues with the Stream
     */
    public String createNewDeployment(String deploymentId, String serviceTypeId) throws IOException {

        String statusUrl = null;
        InputStream inStream = null;
        BufferedReader in = null;
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "createNewDeployment");
        try {
            ObjectNode rootNode = (ObjectNode) refProp.getJsonTemplate("dcae.deployment.template");
            ((ObjectNode) rootNode).put("serviceTypeId", serviceTypeId);
            String apiBodyString = rootNode.toString();

            logger.info("Dcae api Body String - " + apiBodyString);
            String url = refProp.getStringValue("DCAE_DISPATCHER_URL") + "/dcae-deployments/" + deploymentId;
            logger.info("Dcae Dispatcher Service url - " + url);
            URL obj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("X-ECOMP-RequestID", LoggingUtils.getRequestId());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.writeBytes(apiBodyString);
                wr.flush();
            }

            boolean requestFailed = true;
            int responseCode = conn.getResponseCode();
            logger.info("responseCode=" + responseCode);
            if (responseCode == 200 || responseCode == 202) {
                requestFailed = false;
            }

            inStream = conn.getErrorStream();
            if (inStream == null) {
                inStream = conn.getInputStream();
            }

            String responseStr = null;
            if (inStream != null) {
                in = new BufferedReader(new InputStreamReader(inStream));

                String inputLine = null;

                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                responseStr = response.toString();
            }

            if (responseStr != null && requestFailed) {
                logger.error("requestFailed - responseStr=" + responseStr);
                throw new BadRequestException(responseStr);
            }

            logger.debug("response code " + responseCode);
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(responseStr);
            JSONObject jsonObj = (JSONObject) obj0;
            JSONObject linksObj = (JSONObject) jsonObj.get("links");
            statusUrl = (String) linksObj.get("status");
            logger.debug("Status URL: " + statusUrl);
        } catch (Exception e) {
            logger.error("Exception occurred during createNewDeployment Operation with DCAE", e);
            throw new DcaeDeploymentException("Exception occurred during createNewDeployment Operation with DCAE", e);
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (in != null) {
                in.close();
            }
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("createNewDeployment complete");
        }
        return statusUrl;
    }

    /**
     * Returns status URL for deleteExistingDeployment operation.
     * 
     * @param deploymentId
     *            The deployment ID
     * @param serviceTypeId
     *            The service Type ID
     * @return The status URL
     * @throws IOException
     *             In case of issues with the Stream
     */
    public String deleteExistingDeployment(String deploymentId, String serviceTypeId) throws IOException {

        String statusUrl = null;
        InputStream in = null;
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "deleteExistingDeployment");
        try {
            String apiBodyString = "{\"serviceTypeId\": \"" + serviceTypeId + "\"}";
            logger.debug(apiBodyString);
            String url = refProp.getStringValue("DCAE_DISPATCHER_URL") + "/dcae-deployments/" + deploymentId;
            logger.info("Dcae Dispatcher deployments url - " + url);
            URL obj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
            conn.setRequestMethod("DELETE");
            conn.setRequestProperty("X-ECOMP-RequestID", LoggingUtils.getRequestId());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(apiBodyString);
            wr.flush();

            int responseCode = conn.getResponseCode();
            logger.debug("response code " + responseCode);
            in = conn.getInputStream();
            String res = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
            logger.debug("res:" + res);
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(res);
            JSONObject jsonObj = (JSONObject) obj0;
            JSONObject linksObj = (JSONObject) jsonObj.get("links");
            statusUrl = (String) linksObj.get("status");
            logger.debug("Status URL: " + statusUrl);
        } catch (Exception e) {
            logger.error("Exception occurred during deleteExistingDeployment Operation with DCAE", e);
            throw new DcaeDeploymentException("Exception occurred during deleteExistingDeployment Operation with DCAE",
                    e);
        } finally {
            if (in != null) {
                in.close();
            }
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("deleteExistingDeployment complete");
        }
        return statusUrl;
    }

}