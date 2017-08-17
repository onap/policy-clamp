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

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 *
 *
 */
public class DcaeDispatcherServices {
    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(DcaeDispatcherServices.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Autowired
    private RefProp                 refProp;

    /**
     *
     * @param deploymentId
     * @return
     * @throws Exception
     */
    public String deleteDeployment(String deploymentId) throws Exception {

        String statusUrl = null;
        InputStream in = null;
        try {
            String url = refProp.getStringValue("DCAE_DISPATCHER_URL") + "/dcae-deployments/" + deploymentId;
            logger.info("Dcae Dispatcher url - " + url);
            URL obj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
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
                StringBuffer response = new StringBuffer();
                while ((inputLine = bufferedReader.readLine()) != null) {
                    response.append(inputLine);
                }
                responseStr = response.toString();
            }

            if (responseStr != null) {
                if (requestFailed) {
                    logger.error("requestFailed - responseStr=" + responseStr);
                    throw new Exception(responseStr);
                }
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
            logger.error(e.getClass().getName() + " " + e.getMessage());
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return statusUrl;

    }

    /**
     *
     * @param statusUrl
     * @return
     * @throws Exception
     */
    public String getOperationStatus(String statusUrl) throws Exception {

        //Assigning processing status to monitor operation status further
        String opStatus = "processing";
        InputStream in = null;
        try {
            URL obj = new URL(statusUrl);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            logger.debug("Deployment operation status response code - " + responseCode);
            if(responseCode == 200){
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
            logger.debug(e.getClass().getName() + " " + e.getMessage());
            logger.debug(e.getMessage()
                    + " : got exception while retrieving status, trying again until we get 200 response code");
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return opStatus;
    }

    /**
     *
     * @throws Exception
     */
    public void getDeployments() throws Exception {
        InputStream in = null;
        try {
            String url = refProp.getStringValue("DCAE_DISPATCHER_URL") + "/dcae-deployments";
            logger.info("Dcae Dispatcher deployments url - " + url);
            URL obj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            logger.debug("response code " + responseCode);
            in = conn.getInputStream();
            String res = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
            logger.debug("res:" + res);
        } catch (Exception e) {
            logger.error("Exception occurred during DCAE communication", e);
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Returns status URL for deployment operation
     *
     * @param deploymentId
     * @param serviceTypeId
     * @return
     * @throws Exception
     */
    public String createNewDeployment(String deploymentId, String serviceTypeId) throws Exception {

        String statusUrl = null;
        InputStream inStream = null;
        BufferedReader in = null;
        try {
            String apiBodyString = "{\"serviceTypeId\": \"" + serviceTypeId + "\"}";
            logger.info("Dcae api Body String - " + apiBodyString);
            String url = refProp.getStringValue("DCAE_DISPATCHER_URL") + "/dcae-deployments/" + deploymentId;
            logger.info("Dcae Dispatcher Service url - " + url);
            URL obj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
            conn.setRequestMethod("PUT");
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

                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                responseStr = response.toString();
            }

            if (responseStr != null) {
                if (requestFailed) {
                    logger.error("requestFailed - responseStr=" + responseStr);
                    throw new Exception(responseStr);
                }
            }

            logger.debug("response code " + responseCode);
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(responseStr);
            JSONObject jsonObj = (JSONObject) obj0;
            JSONObject linksObj = (JSONObject) jsonObj.get("links");
            statusUrl = (String) linksObj.get("status");
            logger.debug("Status URL: " + statusUrl);
        } catch (Exception e) {
            logger.error("Exception occurred during the DCAE communication", e);
            throw e;
        } finally {
            if (inStream != null) {
                inStream.close();
            }
            if (in != null) {
                in.close();
            }
        }
        return statusUrl;
    }

    /**
     *
     * @param deploymentId
     * @param serviceTypeId
     * @return
     * @throws Exception
     */
    public String deleteExistingDeployment(String deploymentId, String serviceTypeId) throws Exception {

        String statusUrl = null;
        InputStream in = null;
        try {
            String apiBodyString = "{\"serviceTypeId\": \"" + serviceTypeId + "\"}";
            logger.debug(apiBodyString);
            String url = refProp.getStringValue("DCAE_DISPATCHER_URL") + "/dcae-deployments/" + deploymentId;
            logger.info("Dcae Dispatcher deployments url - " + url);
            URL obj = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) obj.openConnection();
            conn.setRequestMethod("DELETE");
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
            logger.error("Exception occurred during DCAE communication", e);
            throw e;
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return statusUrl;
    }

}