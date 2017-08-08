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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
import org.springframework.beans.factory.annotation.Autowired;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.core.JsonProcessingException;

public class DcaeInventoryServices {
    protected static final EELFLogger       logger      = EELFManager.getInstance().getLogger(DcaeInventoryServices.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    @Autowired
    private RefProp                 refProp;

    @Autowired
    private CldsDao                 cldsDao;

    @Autowired
    private SdcCatalogServices      sdcCatalogServices;

    public void setEventInventory(CldsModel cldsModel, String userId) throws Exception {
        String artifactName = cldsModel.getControlName();
        DcaeEvent dcaeEvent = new DcaeEvent();
        String isDcaeInfoAvailable = null;
        if (artifactName != null) {
            artifactName = artifactName + ".yml";
        }
        try {
            /*
             * Below are the properties required for calling the dcae inventory
             * url call
             */
            ModelProperties prop = new ModelProperties(cldsModel.getName(), cldsModel.getControlName(), null, false, "{}",
                    cldsModel.getPropText());
            Global global = prop.getGlobal();
            String invariantServiceUuid = global.getService();
            List<String> resourceUuidList = global.getResourceVf();
            String serviceUuid = sdcCatalogServices.getServiceUuidFromServiceInvariantId(invariantServiceUuid);
            String resourceUuid = "";
            if (resourceUuidList != null && resourceUuidList.size() > 0) {
                resourceUuid = resourceUuidList.get(0).toString();
            }
            /* Invemtory service url is called in this method */
            isDcaeInfoAvailable = getDcaeInformation(artifactName, serviceUuid, resourceUuid);

            /* set dcae events */
            dcaeEvent.setArtifactName(artifactName);
            dcaeEvent.setEvent(DcaeEvent.EVENT_DISTRIBUTION);

        } catch (JsonProcessingException e) {
            // exception
            logger.error("JsonProcessingException" + e);
        } catch (IOException e) {

            // exception
            logger.error("IOException :" + e);
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
            if(oldTypeId == null || !oldTypeId.equalsIgnoreCase(newTypeId)){
            	CldsEvent.insEvent(cldsDao, dcaeEvent.getControlName(), userId, dcaeEvent.getCldsActionCd(),
                    CldsEvent.ACTION_STATE_RECEIVED, null);
            }
            cldsModel.save(cldsDao, userId);
        } else {
            logger.info(cldsModel.getName() + " Model is not present in Dcae Inventory Service.");
        }
    }

    public String getDcaeInformation(String artifactName, String serviceUUID, String resourceUUID)
            throws IOException, ParseException {
        String queryString = "?sdcResourceId=" + resourceUUID + "&sdcServiceId=" + serviceUUID + "&typeName="
                + artifactName;
        String fullUrl = refProp.getStringValue("DCAE_INVENTORY_URL") + "/dcae-service-types" + queryString;
        logger.info("Dcae Inventory Service full url - " + fullUrl);
        String daceInventoryResponse = null;
        URL inventoryUrl = new URL(fullUrl);

        HttpURLConnection conn = (HttpURLConnection) inventoryUrl.openConnection();
        conn.setRequestMethod("GET");
        boolean requestFailed = true;
        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            requestFailed = false;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine = null;
        StringBuffer response = new StringBuffer();
        String responseStr = null;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        responseStr = response.toString();
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
            logger.info(daceInventoryResponse.toString());
        }
        return daceInventoryResponse;
    }

}
