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
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.DcaeEvent;
import org.onap.clamp.clds.model.prop.Global;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class implements the communication with DCAE for the service inventory.
 *
 */
public class DcaeInventoryServices {
    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(DcaeInventoryServices.class);
    protected static final EELFLogger auditLogger   = EELFManager.getInstance().getAuditLogger();
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Autowired
    private RefProp                   refProp;

    @Autowired
    private CldsDao                   cldsDao;

    @Autowired
    private SdcCatalogServices        sdcCatalogServices;

    /**
     * Set the event inventory.
     * 
     * @param cldsModel
     *            The CldsModel
     * @param userId
     *            The user ID
     * @throws ParseException
     *             In case of issues during the parsing of DCAE answer
     */
    public void setEventInventory(CldsModel cldsModel, String userId) throws ParseException {
        String artifactName = cldsModel.getControlName();
        DcaeEvent dcaeEvent = new DcaeEvent();
        String isDcaeInfoAvailable = null;
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "setEventInventory");
        if (artifactName != null) {
            artifactName = artifactName + ".yml";
        }
        try {
            /*
             * Below are the properties required for calling the dcae inventory
             * url call
             */
            ModelProperties prop = new ModelProperties(cldsModel.getName(), cldsModel.getControlName(), null, false,
                    "{}", cldsModel.getPropText());
            Global global = prop.getGlobal();
            String invariantServiceUuid = global.getService();
            List<String> resourceUuidList = global.getResourceVf();
            String serviceUuid = sdcCatalogServices.getServiceUuidFromServiceInvariantId(invariantServiceUuid);
            String resourceUuid = "";
            if (resourceUuidList != null && !resourceUuidList.isEmpty()) {
                resourceUuid = resourceUuidList.get(0);
            }
            /* Invemtory service url is called in this method */
            isDcaeInfoAvailable = getDcaeInformation(artifactName, serviceUuid, resourceUuid);

            /* set dcae events */
            dcaeEvent.setArtifactName(artifactName);
            dcaeEvent.setEvent(DcaeEvent.EVENT_DISTRIBUTION);

        } catch (JsonProcessingException e) {
            logger.error("Error during JSON decoding", e);
        } catch (IOException ex) {
            logger.error("Error during JSON decoding", ex);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("setEventInventory complete");
        }
        /* Null whether the DCAE has items lenght or not */
        if (isDcaeInfoAvailable != null) {
            /* Inserting Event in to DB */
            logger.info(isDcaeInfoAvailable);
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(isDcaeInfoAvailable);
            JSONObject jsonObj = (JSONObject) obj0;
            String oldTypeId = cldsModel.getTypeId();
            String newTypeId = "";
            if (jsonObj.get("typeId") != null) {
                newTypeId = jsonObj.get("typeId").toString();
                cldsModel.setTypeId(jsonObj.get("typeId").toString());
            }
            // cldsModel.setTypeName(cldsModel.getControlName().toString()+".yml");
            if (jsonObj.get("typeName") != null) {
                cldsModel.setTypeName(jsonObj.get("typeName").toString());
            }
            if (oldTypeId == null || !oldTypeId.equalsIgnoreCase(newTypeId)) {
                CldsEvent.insEvent(cldsDao, dcaeEvent.getControlName(), userId, dcaeEvent.getCldsActionCd(),
                        CldsEvent.ACTION_STATE_RECEIVED, null);
            }
            cldsModel.save(cldsDao, userId);
        } else {
            logger.info(cldsModel.getName() + " Model is not present in Dcae Inventory Service.");
        }
    }

    /**
     * DO a query to DCAE to get some Information.
     * 
     * @param artifactName
     *            The artifact Name
     * @param serviceUuid
     *            The service UUID
     * @param resourceUuid
     *            The resource UUID
     * @return The DCAE inventory for the artifact
     * @throws IOException
     *             In case of issues with the stream
     * @throws ParseException
     *             In case of issues with the Json parsing
     */
    public String getDcaeInformation(String artifactName, String serviceUuid, String resourceUuid)
            throws IOException, ParseException {
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "getDcaeInformation");
        String queryString = "?sdcResourceId=" + resourceUuid + "&sdcServiceId=" + serviceUuid + "&typeName="
                + artifactName;
        String fullUrl = refProp.getStringValue("DCAE_INVENTORY_URL") + "/dcae-service-types" + queryString;

        logger.info("Dcae Inventory Service full url - " + fullUrl);
        String daceInventoryResponse = null;
        URL inventoryUrl = new URL(fullUrl);

        HttpURLConnection conn = (HttpURLConnection) inventoryUrl.openConnection();
        conn.setRequestMethod("GET");
        String reqid = LoggingUtils.getRequestId();
        logger.info("reqid set to " + reqid);
        conn.setRequestProperty("X-ECOMP-RequestID", reqid);

        boolean requestFailed = true;
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            requestFailed = false;
        }

        StringBuilder response = new StringBuilder();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String inputLine = null;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        String responseStr = response.toString();
        if (responseStr != null) {
            if (requestFailed) {
                logger.error("requestFailed - responseStr=" + response);
                throw new BadRequestException(responseStr);
            }
        }
        String jsonResponseString = response.toString();
        JSONParser parser = new JSONParser();
        Object obj0 = parser.parse(jsonResponseString);

        JSONObject jsonObj = (JSONObject) obj0;

        Long totalCount = (Long) jsonObj.get("totalCount");

        int numServices = totalCount.intValue();
        if (numServices == 0) {
            daceInventoryResponse = null;
        } else if (numServices > 0) {
            JSONArray itemsArray = (JSONArray) jsonObj.get("items");
            JSONObject dcaeServiceType0 = (JSONObject) itemsArray.get(0);
            daceInventoryResponse = dcaeServiceType0.toString();
            logger.info(daceInventoryResponse);
        }
        LoggingUtils.setTimeContext(startTime, new Date());
        metricsLogger.info("getDcaeInformation complete: number services returned=" + numServices);
        return daceInventoryResponse;
    }

}
