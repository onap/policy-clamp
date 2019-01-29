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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 *
 */

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.DcaeEvent;
import org.onap.clamp.clds.model.dcae.DcaeInventoryResponse;
import org.onap.clamp.clds.model.properties.Global;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class implements the communication with DCAE for the service inventory.
 */
@Component
public class DcaeInventoryServices {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(DcaeInventoryServices.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    public static final String DCAE_INVENTORY_URL = "dcae.inventory.url";
    public static final String DCAE_INVENTORY_RETRY_INTERVAL = "dcae.intentory.retry.interval";
    public static final String DCAE_INVENTORY_RETRY_LIMIT = "dcae.intentory.retry.limit";
    private final ClampProperties refProp;
    private final CldsDao cldsDao;
    private final DcaeHttpConnectionManager dcaeHttpConnectionManager;

    @Autowired
    public DcaeInventoryServices(ClampProperties refProp, CldsDao cldsDao,
        DcaeHttpConnectionManager dcaeHttpConnectionManager) {
        this.refProp = refProp;
        this.cldsDao = cldsDao;
        this.dcaeHttpConnectionManager = dcaeHttpConnectionManager;
    }

    /**
     * Set the event inventory.
     *
     * @param cldsModel
     *        The CldsModel
     * @param userId
     *        The user ID
     * @throws ParseException
     *         In case of DCAE Json parse exception
     */
    public void setEventInventory(CldsModel cldsModel, String userId) throws ParseException, InterruptedException {
        String artifactName = cldsModel.getControlName();
        DcaeEvent dcaeEvent = new DcaeEvent();
        DcaeInventoryResponse dcaeResponse = null;
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "setEventInventory");
        if (artifactName != null) {
            artifactName = artifactName + ".yml";
        }
        try {
            // Below are the properties required for calling the dcae inventory
            ModelProperties prop = new ModelProperties(cldsModel.getName(), cldsModel.getControlName(), null, false,
                "{}", cldsModel.getPropText());
            Global global = prop.getGlobal();
            String invariantServiceUuid = global.getService();
            List<String> resourceUuidList = global.getResourceVf();
            String resourceUuid = "";
            if (resourceUuidList != null && !resourceUuidList.isEmpty()) {
                resourceUuid = resourceUuidList.get(0);
            }
            /* Inventory service url is called in this method */
            dcaeResponse = getDcaeInformation(artifactName, invariantServiceUuid, resourceUuid);
            /* set dcae events */
            dcaeEvent.setArtifactName(artifactName);
            dcaeEvent.setEvent(DcaeEvent.EVENT_DISTRIBUTION);
            LoggingUtils.setResponseContext("0", "Set inventory success", this.getClass().getName());
        } catch (JsonProcessingException e) {
            LoggingUtils.setResponseContext("900", "Set inventory failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Set inventory error");
            logger.error("Error during JSON decoding", e);
        } catch (IOException ex) {
            LoggingUtils.setResponseContext("900", "Set inventory failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Set inventory error");
            logger.error("Error during DCAE communication", ex);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("setEventInventory complete");
        }
        this.analyzeAndSaveDcaeResponse(dcaeResponse, cldsModel, dcaeEvent, userId);
    }

    private void analyzeAndSaveDcaeResponse(DcaeInventoryResponse dcaeResponse, CldsModel cldsModel,
        DcaeEvent dcaeEvent, String userId) {
        if (dcaeResponse != null) {
            logger.info("Dcae Response for query on inventory: " + dcaeResponse);
            String oldTypeId = cldsModel.getTypeId();
            if (dcaeResponse.getTypeId() != null) {
                cldsModel.setTypeId(dcaeResponse.getTypeId());
            }
            if (dcaeResponse.getTypeName() != null) {
                cldsModel.setTypeName(dcaeResponse.getTypeName());
            }
            if (oldTypeId == null || !cldsModel.getEvent().getActionCd().equalsIgnoreCase(CldsEvent.ACTION_DISTRIBUTE)
                || cldsModel.getEvent().getActionCd().equalsIgnoreCase(CldsEvent.ACTION_SUBMITDCAE)) {
                CldsEvent.insEvent(cldsDao, dcaeEvent.getControlName(), userId, dcaeEvent.getCldsActionCd(),
                    CldsEvent.ACTION_STATE_RECEIVED, null);
            }
            cldsModel.save(cldsDao, userId);
        } else {
            logger.info(cldsModel.getName() + " Model is not present in Dcae Inventory Service.");
        }
    }

    private int getTotalCountFromDcaeInventoryResponse(String responseStr) throws ParseException {
        JSONParser parser = new JSONParser();
        Object obj0 = parser.parse(responseStr);
        JSONObject jsonObj = (JSONObject) obj0;
        Long totalCount = (Long) jsonObj.get("totalCount");
        return totalCount.intValue();
    }

    private DcaeInventoryResponse getItemsFromDcaeInventoryResponse(String responseStr)
        throws ParseException {
        JSONParser parser = new JSONParser();
        Object obj0 = parser.parse(responseStr);
        JSONObject jsonObj = (JSONObject) obj0;
        JSONArray itemsArray = (JSONArray) jsonObj.get("items");
        JSONObject dcaeServiceType0 = (JSONObject) itemsArray.get(0);
        return JsonUtils.GSON.fromJson(dcaeServiceType0.toString(), DcaeInventoryResponse.class);
    }

    /**
     * DO a query to DCAE to get some Information.
     *
     * @param artifactName
     *        The artifact Name
     * @param serviceUuid
     *        The service UUID
     * @param resourceUuid
     *        The resource UUID
     * @return The DCAE inventory for the artifact in DcaeInventoryResponse
     * @throws IOException
     *         In case of issues with the stream
     * @throws ParseException
     *         In case of issues with the Json parsing
     */
    public DcaeInventoryResponse getDcaeInformation(String artifactName, String serviceUuid, String resourceUuid)
        throws IOException, ParseException, InterruptedException {
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "getDcaeInformation");
        String queryString = "?asdcResourceId=" + resourceUuid + "&asdcServiceId=" + serviceUuid + "&typeName="
            + artifactName;
        String fullUrl = refProp.getStringValue(DCAE_INVENTORY_URL) + "/dcae-service-types" + queryString;
        logger.info("Dcae Inventory Service full url - " + fullUrl);
        DcaeInventoryResponse response = queryDcaeInventory(fullUrl);
        LoggingUtils.setResponseContext("0", "Get Dcae Information success", this.getClass().getName());
        LoggingUtils.setTimeContext(startTime, new Date());
        return response;
    }

    private DcaeInventoryResponse queryDcaeInventory(String fullUrl)
        throws IOException, InterruptedException, ParseException {
        int retryInterval = 0;
        int retryLimit = 1;
        if (refProp.getStringValue(DCAE_INVENTORY_RETRY_LIMIT) != null) {
            retryLimit = Integer.valueOf(refProp.getStringValue(DCAE_INVENTORY_RETRY_LIMIT));
        }
        if (refProp.getStringValue(DCAE_INVENTORY_RETRY_INTERVAL) != null) {
            retryInterval = Integer.valueOf(refProp.getStringValue(DCAE_INVENTORY_RETRY_INTERVAL));
        }
        for (int i = 0; i < retryLimit; i++) {
            metricsLogger.info("Attempt n°" + i + " to contact DCAE inventory");
            String response = dcaeHttpConnectionManager.doDcaeHttpQuery(fullUrl, "GET", null, null);
            int totalCount = getTotalCountFromDcaeInventoryResponse(response);
            metricsLogger.info("getDcaeInformation complete: totalCount returned=" + totalCount);
            if (totalCount > 0) {
                logger.info("getDcaeInformation, answer from DCAE inventory:" + response);
                return getItemsFromDcaeInventoryResponse(response);
            }
            logger.info(
                "Dcae inventory totalCount returned is 0, so waiting " + retryInterval + "ms before retrying ...");
            // wait for a while and try to connect to DCAE again
            Thread.sleep(retryInterval);
        }
        logger.warn("Dcae inventory totalCount returned is still 0, after " + retryLimit + " attempts, returning NULL");
        return null;
    }
}
