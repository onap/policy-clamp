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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.apache.commons.codec.DecoderException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.DcaeEvent;
import org.onap.clamp.clds.model.properties.Global;
import org.onap.clamp.clds.model.properties.ModelProperties;
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
    private static final String DCAE_INVENTORY_URL = "dcae.inventory.url";
    @Autowired
    private ClampProperties refProp;
    @Autowired
    private CldsDao cldsDao;

    /**
     * Set the event inventory.
     * 
     * @param cldsModel
     *            The CldsModel
     * @param userId
     *            The user ID
     * @throws GeneralSecurityException
     *             In case of issue when decryting the DCAE password
     * @throws ParseException
     *             In case of DCAE Json parse exception
     * @throws DecoderException
     *             In case of issues with HexString decoding
     */
    public void setEventInventory(CldsModel cldsModel, String userId)
            throws GeneralSecurityException, ParseException, DecoderException {
        String artifactName = cldsModel.getControlName();
        DcaeEvent dcaeEvent = new DcaeEvent();
        String isDcaeInfoAvailable = null;
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
            isDcaeInfoAvailable = getDcaeInformation(artifactName, invariantServiceUuid, resourceUuid);
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
            if (oldTypeId == null || !oldTypeId.equalsIgnoreCase(newTypeId)
                    || cldsModel.getEvent().getActionCd().equalsIgnoreCase(CldsEvent.ACTION_SUBMITDCAE)) {
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
        String queryString = "?asdcResourceId=" + resourceUuid + "&asdcServiceId=" + serviceUuid + "&typeName="
                + artifactName;
        String fullUrl = refProp.getStringValue(DCAE_INVENTORY_URL) + "/dcae-service-types" + queryString;
        logger.info("Dcae Inventory Service full url - " + fullUrl);
        String daceInventoryResponse = null;
        String responseStr = DcaeHttpConnectionManager.doDcaeHttpQuery(fullUrl, "GET", null, null);
        JSONParser parser = new JSONParser();
        Object obj0 = parser.parse(responseStr);
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
        LoggingUtils.setResponseContext("0", "Get Dcae Information success", this.getClass().getName());
        LoggingUtils.setTimeContext(startTime, new Date());
        metricsLogger.info("getDcaeInformation complete: number services returned=" + numServices);
        return daceInventoryResponse;
    }

    /**
     * Inserts a new DCAEServiceType or updates an existing instance. If the
     * typeName is same second time(already exists) then the
     * DCAEServiceTypeRequest is updated
     * 
     * @param blueprintTemplate
     *            blueprint content
     * @param owner
     *            owner of the data
     * @param typeName
     *            The type/artifact Name
     * @param typeVersion
     *            type version
     * @param asdcServiceId
     *            The service UUID
     * @param asdcResourceId
     *            The vf UUID
     * @return The DCAE inventory type id
     */
    public String createupdateDCAEServiceType(String blueprintTemplate, String owner, String typeName, int typeVersion,
            String asdcServiceId, String asdcResourceId) {
        Date startTime = new Date();
        LoggingUtils.setTargetContext("DCAE", "createDCAEServiceType");
        String typeId = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode dcaeServiceTypeRequest = mapper.createObjectNode();
            dcaeServiceTypeRequest.put("blueprintTemplate", blueprintTemplate);
            dcaeServiceTypeRequest.put("owner", owner);
            dcaeServiceTypeRequest.put("typeName", typeName);
            dcaeServiceTypeRequest.put("typeVersion", typeVersion);
            dcaeServiceTypeRequest.put("asdcServiceId", asdcServiceId);
            dcaeServiceTypeRequest.put("asdcResourceId", asdcResourceId);
            String apiBodyString = dcaeServiceTypeRequest.toString();
            logger.info("Dcae api Body String - " + apiBodyString);
            String url = refProp.getStringValue(DCAE_INVENTORY_URL) + "/dcae-service-types";
            String responseStr = DcaeHttpConnectionManager.doDcaeHttpQuery(url, "POST", apiBodyString,
                    "application/json");
            // If the DCAEServiceTypeRequest is created successfully it will
            // return a json object responce containing a node for newly created
            // "typeId"
            // The newly generated DCAEServiceTypeRequest can then be accessed
            // via URL: https://<DCAE_INVENTORY_URL>/dcae-service-types/<typeId>
            JSONParser parser = new JSONParser();
            Object obj0 = parser.parse(responseStr);
            JSONObject jsonObj = (JSONObject) obj0;
            typeId = (String) jsonObj.get("typeId"); // need to save this
                                                     // as
                                                     // service_type_id
                                                     // in model table
        } catch (IOException | ParseException e) {
            logger.error("Exception occurred during createupdateDCAEServiceType Operation with DCAE", e);
            throw new BadRequestException("Exception occurred during createupdateDCAEServiceType Operation with DCAE",
                    e);
        } finally {
            if (typeId != null) {
                LoggingUtils.setResponseContext("0", "Create update DCAE ServiceType success",
                        this.getClass().getName());
            } else {
                LoggingUtils.setResponseContext("900", "Create update DCAE ServiceType failed",
                        this.getClass().getName());
                LoggingUtils.setErrorContext("900", "Create update DCAE ServiceType error");
            }
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("createupdateDCAEServiceType complete");
        }
        return typeId;
    }
}
