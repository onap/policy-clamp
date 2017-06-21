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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.onap.clamp.clds.client.req.SdcReq;
import org.onap.clamp.clds.model.*;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SdcCatalogServices {
    private static final Logger logger = LoggerFactory.getLogger(SdcSendReqDelegate.class);

    @Autowired
    private RefProp refProp;

    public String getAsdcServicesInformation(String uuid) throws Exception {
        String baseUrl = refProp.getStringValue("asdc.serviceUrl");
        String basicAuth = SdcReq.getAsdcBasicAuth(refProp);
        try {
            String url = baseUrl;
            if (uuid != null) {
                url = baseUrl + "/" + uuid + "/metadata";
            }
            URL urlObj = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

            conn.setRequestProperty("X-ONAP-InstanceID", "CLAMP-Tool");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestMethod("GET");

            String resp = getResponse(conn);
            if (resp != null) {
                logger.info(resp.toString());
                return resp;
            }
        } catch (Exception e) {
            logger.error("not able to ger any service information from asdc for uuid:" + uuid);
        }
        return "";
    }

    /**
     * To remove duplicate serviceUUIDs from asdc services List
     *
     * @param rawCldsAsdcServiceList
     * @return
     */
    public List<CldsAsdcServiceInfo> removeDuplicateServices(List<CldsAsdcServiceInfo> rawCldsAsdcServiceList) {
        List<CldsAsdcServiceInfo> cldsAsdcServiceInfoList = null;
        if (rawCldsAsdcServiceList != null && rawCldsAsdcServiceList.size() > 0) {
            // sort list
            Collections.sort(rawCldsAsdcServiceList);
            // and then take only the services with the max version (last in the list with the same name)
            cldsAsdcServiceInfoList = new ArrayList<>();
            for (int i = 1; i < rawCldsAsdcServiceList.size(); i++) {
                // compare name with previous - if not equal, then keep the previous (it's the last with that name)
                CldsAsdcServiceInfo prev = rawCldsAsdcServiceList.get(i - 1);
                if (!rawCldsAsdcServiceList.get(i).getName().equals(prev.getName())) {
                    cldsAsdcServiceInfoList.add(prev);
                }
            }
            // add the last in the list
            cldsAsdcServiceInfoList.add(rawCldsAsdcServiceList.get(rawCldsAsdcServiceList.size() - 1));
        }
        return cldsAsdcServiceInfoList;
    }

    /**
     * To remove duplicate serviceUUIDs from asdc resources List
     *
     * @param rawCldsAsdcResourceList
     * @return
     */
    public List<CldsAsdcResource> removeDuplicateAsdcResourceInstances(List<CldsAsdcResource> rawCldsAsdcResourceList) {
        List<CldsAsdcResource> cldsAsdcResourceList = null;
        if (rawCldsAsdcResourceList != null && rawCldsAsdcResourceList.size() > 0) {
            // sort list
            Collections.sort(rawCldsAsdcResourceList);
            // and then take only the resources with the max version (last in the list with the same name)
            cldsAsdcResourceList = new ArrayList<>();
            for (int i = 1; i < rawCldsAsdcResourceList.size(); i++) {
                // compare name with previous - if not equal, then keep the previous (it's the last with that name)
                CldsAsdcResource prev = rawCldsAsdcResourceList.get(i - 1);
                if (!rawCldsAsdcResourceList.get(i).getResourceInstanceName().equals(prev.getResourceInstanceName())) {
                    cldsAsdcResourceList.add(prev);
                }
            }
            // add the last in the list
            cldsAsdcResourceList.add(rawCldsAsdcResourceList.get(rawCldsAsdcResourceList.size() - 1));
        }
        return cldsAsdcResourceList;
    }


    /**
     * To remove duplicate basic resources with same resourceUUIDs
     *
     * @param rawCldsAsdcResourceListBasicList
     * @return
     */
    public List<CldsAsdcResourceBasicInfo> removeDuplicateAsdcResourceBasicInfo(List<CldsAsdcResourceBasicInfo> rawCldsAsdcResourceListBasicList) {
        List<CldsAsdcResourceBasicInfo> cldsAsdcResourceBasicInfoList = null;
        if (rawCldsAsdcResourceListBasicList != null && rawCldsAsdcResourceListBasicList.size() > 0) {
            // sort list
            Collections.sort(rawCldsAsdcResourceListBasicList);
            // and then take only the resources with the max version (last in the list with the same name)
            cldsAsdcResourceBasicInfoList = new ArrayList<>();
            for (int i = 1; i < rawCldsAsdcResourceListBasicList.size(); i++) {
                // compare name with previous - if not equal, then keep the previous (it's the last with that name)
                CldsAsdcResourceBasicInfo prev = rawCldsAsdcResourceListBasicList.get(i - 1);
                if (!rawCldsAsdcResourceListBasicList.get(i).getName().equals(prev.getName())) {
                    cldsAsdcResourceBasicInfoList.add(prev);
                }
            }
            // add the last in the list
            cldsAsdcResourceBasicInfoList.add(rawCldsAsdcResourceListBasicList.get(rawCldsAsdcResourceListBasicList.size() - 1));
        }
        return cldsAsdcResourceBasicInfoList;
    }

    /**
     * To get ServiceUUID by using serviceInvariantUUID
     *
     * @param invariantID
     * @return
     * @throws Exception
     */
    public String getServiceUUIDFromServiceInvariantID(String invariantID) throws Exception {
        String serviceUUID = "";
        String responseStr = getAsdcServicesInformation(null);
        List<CldsAsdcServiceInfo> rawCldsAsdcServicesList = getCldsAsdcServicesListFromJson(responseStr);
        List<CldsAsdcServiceInfo> cldsAsdcServicesList = removeDuplicateServices(rawCldsAsdcServicesList);
        if (cldsAsdcServicesList != null && cldsAsdcServicesList.size() > 0) {
            for (CldsAsdcServiceInfo currCldsAsdcServiceInfo : cldsAsdcServicesList) {
                if (currCldsAsdcServiceInfo != null && currCldsAsdcServiceInfo.getInvariantUUID() != null
                        && currCldsAsdcServiceInfo.getInvariantUUID().equalsIgnoreCase(invariantID)) {
                    serviceUUID = currCldsAsdcServiceInfo.getUuid();
                    break;
                }
            }
        }
        return serviceUUID;
    }

    /**
     * To get CldsAsdsServiceInfo class by parsing json string
     *
     * @param jsonStr
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public List<CldsAsdcServiceInfo> getCldsAsdcServicesListFromJson(String jsonStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        return objectMapper.readValue(jsonStr, objectMapper.getTypeFactory().constructCollectionType(List.class, CldsAsdcServiceInfo.class));
    }

    /**
     * To get List<CldsAsdcResourceBasicInfo> class by parsing json string
     *
     * @param jsonStr
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public List<CldsAsdcResourceBasicInfo> getAllAsdcResourcesListFromJson(String jsonStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        return objectMapper.readValue(jsonStr, objectMapper.getTypeFactory().constructCollectionType(List.class, CldsAsdcResourceBasicInfo.class));
    }

    /**
     * To get CldsAsdsResource class by parsing json string
     *
     * @param jsonStr
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public CldsAsdcResource getCldsAsdcResourceFromJson(String jsonStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonStr, CldsAsdcResource.class);
    }

    /**
     * To get CldsAsdcServiceDetail by parsing json string
     *
     * @param jsonStr
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public CldsAsdcServiceDetail getCldsAsdcServiceDetailFromJson(String jsonStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonStr, CldsAsdcServiceDetail.class);
    }

    /**
     * To upload artifact to asdc based on serviceUUID and resourcename on url
     * @param prop
     * @param userid
     * @param url
     * @param formatttedAsdcReq
     * @return
     * @throws Exception
     */
    public String uploadArtifactToAsdc(ModelProperties prop, String userid, String url, String formatttedAsdcReq) throws Exception {
        logger.info("userid=" + userid);
        String md5Text = SdcReq.calculateMD5ByString(formatttedAsdcReq);
        byte[] postData = SdcReq.stringToByteArray(formatttedAsdcReq);
        int postDataLength = postData.length;
        HttpURLConnection conn = getAsdcHttpUrlConnection(userid, postDataLength, url, md5Text);
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }
        boolean requestFailed = true;
        int responseCode = conn.getResponseCode();
        logger.info("responseCode=" + responseCode);
        if (responseCode == 200) {
            requestFailed = false;
        }

        String responseStr = getResponse(conn);
        if (responseStr != null) {
            if (requestFailed) {
                logger.error("requestFailed - responseStr=" + responseStr);
                throw new Exception(responseStr);
            }
        }
        return responseStr;
    }

    private HttpURLConnection getAsdcHttpUrlConnection(String userid, int postDataLength, String url, String md5Text) throws IOException {
        logger.info("userid=" + userid);
        String basicAuth = SdcReq.getAsdcBasicAuth(refProp);
        String asdcXONAPInstanceID = refProp.getStringValue("asdc.asdcX-ONAP-InstanceID");
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty("X-ONAP-InstanceID", asdcXONAPInstanceID);
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-MD5", md5Text);
        conn.setRequestProperty("HTTP_CSP_USERID", userid);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);
        return conn;
    }

    private String getResponse(HttpURLConnection conn) throws IOException {
        try (InputStream is = getInputStream(conn)) {
            if (is != null) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                    StringBuffer response = new StringBuffer();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    return response.toString();
                }
            }
        }
        return null;
    }

    private InputStream getInputStream(HttpURLConnection conn) throws IOException {
        InputStream inStream = conn.getErrorStream();
        if (inStream == null) {
            inStream = conn.getInputStream();
        }
        return inStream;
    }


    public CldsDBServiceCache getCldsDBServiceCacheUsingCldsServiceData(CldsServiceData cldsServiceData) throws IOException {
        CldsDBServiceCache cldsDbServiceCache = new CldsDBServiceCache();
        cldsDbServiceCache.setCldsDataInstream(cldsServiceData);
        cldsDbServiceCache.setInvariantId(cldsServiceData.getServiceInvariantUUID());
        cldsDbServiceCache.setServiceId(cldsServiceData.getServiceUUID());
        return cldsDbServiceCache;
    }

    public boolean isCldsAsdcCacheDataExpired(CldsServiceData cldsServiceData) throws Exception {
        boolean expired = false;
        if (cldsServiceData != null && cldsServiceData.getServiceUUID() != null) {
            String cachedServiceUUID = cldsServiceData.getServiceUUID();
            String latestServiceUUID = getServiceUUIDFromServiceInvariantID(cldsServiceData.getServiceInvariantUUID());
            String defaultRecordAge = refProp.getStringValue("CLDS_SERVICE_CACHE_MAX_SECONDS");
            if ((!cachedServiceUUID.equalsIgnoreCase(latestServiceUUID)) ||
                    (cldsServiceData.getAgeOfRecord() != null && cldsServiceData.getAgeOfRecord() > Long.parseLong(defaultRecordAge))) {
                expired = true;
            }
        } else {
            expired = true;
        }
        return expired;
    }

    public CldsServiceData getCldsServiceDataWithAlarmConditions(String invariantServiceUUID) throws Exception {
        String url = refProp.getStringValue("asdc.serviceUrl");
        String catalogUrl = refProp.getStringValue("asdc.catalog.url");
        String serviceUUID = getServiceUUIDFromServiceInvariantID(invariantServiceUUID);
        String serviceDetailUrl = url + "/" + serviceUUID + "/metadata";
        String responseStr = getCldsServicesOrResourcesBasedOnURL(serviceDetailUrl, false);
        ObjectMapper objectMapper = new ObjectMapper();
        CldsServiceData cldsServiceData = new CldsServiceData();
        if (responseStr != null) {
            CldsAsdcServiceDetail cldsAsdcServiceDetail = objectMapper.readValue(responseStr, CldsAsdcServiceDetail.class);
            cldsServiceData.setServiceUUID(cldsAsdcServiceDetail.getUuid());
            cldsServiceData.setServiceInvariantUUID(cldsAsdcServiceDetail.getInvariantUUID());

            // To remove  duplicate  resources from serviceDetail and add valid vfs to service
            if (cldsAsdcServiceDetail != null && cldsAsdcServiceDetail.getResources() != null) {
                List<CldsAsdcResource> cldsAsdcResourceList = removeDuplicateAsdcResourceInstances(cldsAsdcServiceDetail.getResources());
                if (cldsAsdcResourceList != null && cldsAsdcResourceList.size() > 0) {
                    List<CldsVfData> cldsVfDataList = new ArrayList<>();
                    for (CldsAsdcResource currCldsAsdcResource : cldsAsdcResourceList) {
                        if (currCldsAsdcResource != null && currCldsAsdcResource.getResoucreType() != null && currCldsAsdcResource.getResoucreType().equalsIgnoreCase("VF")) {
                            CldsVfData currCldsVfData = new CldsVfData();
                            currCldsVfData.setVfName(currCldsAsdcResource.getResourceInstanceName());
                            currCldsVfData.setVfInvariantResourceUUID(currCldsAsdcResource.getResourceInvariantUUID());
                            cldsVfDataList.add(currCldsVfData);
                        }
                    }
                    cldsServiceData.setCldsVfs(cldsVfDataList);
                    // For each vf in the list , add all vfc's
                    getAllVfcForVfList(cldsVfDataList, catalogUrl);
                    logger.info("value of cldsServiceData:" + cldsServiceData);
                    logger.info("value of cldsServiceData:" + cldsServiceData.getServiceInvariantUUID());
                }
            }
        }
        return cldsServiceData;
    }

    /**
     * @param cldsVfDataList
     * @throws IOException
     */
    private void getAllVfcForVfList(List<CldsVfData> cldsVfDataList, String catalogUrl) throws IOException {
        // todo : refact this..
        if (cldsVfDataList != null && cldsVfDataList.size() > 0) {
            List<CldsAsdcResourceBasicInfo> allAsdcResources = getAllAsdcResources();
            String resourceVFType = "VF";
            List<CldsAsdcResourceBasicInfo> allVfResources = getAllAsdcVForVFCResourcesBasedOnResourceType(resourceVFType, allAsdcResources);
            String resourceVFCType = "VFC";
            List<CldsAsdcResourceBasicInfo> allVfcResources = getAllAsdcVForVFCResourcesBasedOnResourceType(resourceVFCType, allAsdcResources);
            for (CldsVfData currCldsVfData : cldsVfDataList) {
                if (currCldsVfData != null && currCldsVfData.getVfInvariantResourceUUID() != null) {
                    String resourceUUID = getResourceUUIDFromResourceInvariantUUID(currCldsVfData.getVfInvariantResourceUUID(), allVfResources);
                    if (resourceUUID != null) {
                        String vfResourceUUIDUrl = catalogUrl + "resources" + "/" + resourceUUID + "/metadata";
                        String vfResponse = getCldsServicesOrResourcesBasedOnURL(vfResourceUUIDUrl, false);
                        if (vfResponse != null) {
                            List<CldsVfcData> vfcDataListFromVfResponse = getVFCDataListFromVfResponse(vfResponse);
                            if (vfcDataListFromVfResponse != null) {
                                currCldsVfData.setCldsVfcs(vfcDataListFromVfResponse);
                                if (vfcDataListFromVfResponse.size() > 0) {
                                    // To get artifacts for every VFC and get alarm conditions from artifact
                                    for (CldsVfcData currCldsVfcData : vfcDataListFromVfResponse) {
                                        if (currCldsVfcData != null && currCldsVfcData.getVfcInvariantResourceUUID() != null) {
                                            String resourceVFCUUID = getResourceUUIDFromResourceInvariantUUID(currCldsVfcData.getVfcInvariantResourceUUID(), allVfcResources);
                                            if (resourceVFCUUID != null) {
                                                String vfcResourceUUIDUrl = catalogUrl + "resources" + "/" + resourceVFCUUID + "/metadata";
                                                String vfcResponse = getCldsServicesOrResourcesBasedOnURL(vfcResourceUUIDUrl, false);
                                                if (vfcResponse != null) {
                                                    List<CldsAlarmCondition> alarmCondtionsFromVfc = getAlarmCondtionsFromVfc(vfcResponse);
                                                    currCldsVfcData.setCldsAlarmConditions(alarmCondtionsFromVfc);
                                                }
                                            } else {
                                                logger.info("No resourceVFC UUID found for given invariantID:" + currCldsVfcData.getVfcInvariantResourceUUID());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        logger.info("No resourceUUID found for given invariantREsourceUUID:" + currCldsVfData.getVfInvariantResourceUUID());
                    }
                }
            }
        }
    }

    private List<CldsVfcData> getVFCDataListFromVfResponse(String vfResponse) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode vfResponseNode = (ObjectNode) mapper.readTree(vfResponse);
        ArrayNode vfcArrayNode = (ArrayNode) vfResponseNode.get("resources");
        List<CldsVfcData> cldsVfcDataList = new ArrayList<>();
        if (vfcArrayNode != null && vfcArrayNode.size() > 0) {
            for (int index = 0; index < vfcArrayNode.size(); index++) {
                CldsVfcData currCldsVfcData = new CldsVfcData();
                ObjectNode currVfcNode = (ObjectNode) vfcArrayNode.get(index);
                TextNode resourceTypeNode = (TextNode) currVfcNode.get("resoucreType");
                if (resourceTypeNode != null && resourceTypeNode.textValue().equalsIgnoreCase("VFC")) {
                    TextNode vfcResourceName = (TextNode) currVfcNode.get("resourceInstanceName");
                    TextNode vfcInvariantResourceUUID = (TextNode) currVfcNode.get("resourceInvariantUUID");
                    currCldsVfcData.setVfcName(vfcResourceName.textValue());
                    currCldsVfcData.setVfcInvariantResourceUUID(vfcInvariantResourceUUID.textValue());
                    cldsVfcDataList.add(currCldsVfcData);
                }
            }
        }
        return cldsVfcDataList;
    }

    private String removeUnwantedBracesFromString(String id) {
        if (id != null && id.contains("\"")) {
            id = id.replaceAll("\"", "");
        }
        return id;
    }

    private List<CldsAlarmCondition> getAlarmCondtionsFromVfc(String vfcResponse) throws IOException {
        List<CldsAlarmCondition> cldsAlarmConditionList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode vfcResponseNode = (ObjectNode) mapper.readTree(vfcResponse);
        ArrayNode artifactsArrayNode = (ArrayNode) vfcResponseNode.get("artifacts");

        if (artifactsArrayNode != null && artifactsArrayNode.size() > 0) {
            for (int index = 0; index < artifactsArrayNode.size(); index++) {
                ObjectNode currArtifactNode = (ObjectNode) artifactsArrayNode.get(index);
                TextNode artifactUrlNode = (TextNode) currArtifactNode.get("artifactURL");
                if (artifactUrlNode != null) {
                    String responsesFromArtifactUrl = getResponsesFromArtifactUrl(artifactUrlNode.textValue());
                    cldsAlarmConditionList.addAll(parseCsvToGetAlarmConditions(responsesFromArtifactUrl));
                    logger.info(responsesFromArtifactUrl);
                }
            }
        }
        return cldsAlarmConditionList;
    }

    private List<CldsAlarmCondition> parseCsvToGetAlarmConditions(String allAlarmCondsValues) throws IOException {
        List<CldsAlarmCondition> cldsAlarmConditionList = new ArrayList<>();
        Reader alarmReader = new StringReader(allAlarmCondsValues);
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(alarmReader);
        if (records != null) {
            Iterator<CSVRecord> it = records.iterator();
            if (it.hasNext()) {
                it.next();
            }
            it.forEachRemaining(record -> processRecord(cldsAlarmConditionList, record));
        }
        return cldsAlarmConditionList;
    }

    private void processRecord(List<CldsAlarmCondition> cldsAlarmConditionList, CSVRecord record) {
        if (record == null) {
            return;
        }
        if (record.size() < 5) {
            logger.debug("invalid csv alarm Record,total columns less than 5: " + record);
            return;
        }
        if (StringUtils.isBlank(record.get(1)) || StringUtils.isBlank(record.get(3)) || StringUtils.isBlank(record.get(4))) {
            logger.debug("invalid csv alarm Record,one of column is having blank value : " + record);
            return;
        }
        CldsAlarmCondition cldsAlarmCondition = new CldsAlarmCondition();
        cldsAlarmCondition.setEventSourceType(record.get(1));
        cldsAlarmCondition.setAlarmConditionKey(record.get(3));
        cldsAlarmCondition.setSeverity(record.get(4));
        cldsAlarmConditionList.add(cldsAlarmCondition);
    }

    private String getResponsesFromArtifactUrl(String artifactsUrl) throws IOException {
        String hostUrl = refProp.getStringValue("asdc.hostUrl");
        artifactsUrl = artifactsUrl.replaceAll("\"", "");
        String artifactUrl = hostUrl + artifactsUrl;
        logger.info("value of artifactURl:" + artifactUrl);
        String currArtifactResponse = getCldsServicesOrResourcesBasedOnURL(artifactUrl, true);
        logger.info("value of artifactResponse:" + currArtifactResponse);
        return currArtifactResponse;
    }

    /**
     * Service to services/resources/artifacts from asdc.Pass alarmConditions as true to get alarmconditons from artifact url and else it is false
     *
     * @param url
     * @param alarmConditions
     * @return
     * @throws IOException
     */
    private String getCldsServicesOrResourcesBasedOnURL(String url, boolean alarmConditions) throws IOException {
        String responseStr;
        try {
            url = removeUnwantedBracesFromString(url);
            URL urlObj = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            String basicAuth = SdcReq.getAsdcBasicAuth(refProp);
            conn.setRequestProperty("X-ONAP-InstanceID", "CLAMP-Tool");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            logger.info("responseCode=" + responseCode);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer response = new StringBuffer();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
                if (alarmConditions) {
                    response.append("\n");
                }
            }
            responseStr = response.toString();
            in.close();
        } catch (Exception e) {
            logger.error("Exception occured :" + e.getMessage());
            throw e;
        }
        return responseStr;
    }

    /**
     * To create properties object by using cldsServicedata
     *
     * @param globalProps
     * @param cldsServiceData
     * @return
     * @throws IOException
     */
    public String createPropertiesObjectByUUID(String globalProps, CldsServiceData cldsServiceData) throws IOException {
        String totalPropsStr;
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode globalPropsJson;
        if (cldsServiceData != null && cldsServiceData.getServiceUUID() != null) {

            /**
             *  Objectnode to save all byservice, byvf , byvfc and byalarm nodes
             */
            ObjectNode byIdObjectNode = mapper.createObjectNode();
            /**
             * To create vf ResourceUUID node with serviceInvariantUUID
             *
             */
            ObjectNode invariantUUIDObjectNodeWithVF = createVFObjectNodeByServiceInvariantUUID(mapper, cldsServiceData);
            byIdObjectNode.putPOJO("byService", invariantUUIDObjectNodeWithVF);

            /**
             *  To create byVf and vfcResourceNode with vfResourceUUID
             */
            ObjectNode vfcObjectNodeByVfUUID = createVFCObjectNodeByVfUUID(mapper, cldsServiceData.getCldsVfs());
            byIdObjectNode.putPOJO("byVf", vfcObjectNodeByVfUUID);


            /**
             *  To create byVfc and alarmCondition with vfcResourceUUID
             */
            ObjectNode vfcResourceUUIDObjectNode = mapper.createObjectNode();
            if (cldsServiceData.getCldsVfs() != null && cldsServiceData.getCldsVfs().size() > 0) {
                for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                    if (currCldsVfData != null) {
                        createAlarmCondObjectNodeByVfcUUID(mapper, vfcResourceUUIDObjectNode, currCldsVfData.getCldsVfcs());
                    }
                }
            }
            byIdObjectNode.putPOJO("byVfc", vfcResourceUUIDObjectNode);

            /**
             *  To create byAlarmCondition  with alarmConditionKey
             */
            List<CldsAlarmCondition> allAlarmConditions = getAllAlarmConditionsFromCldsServiceData(cldsServiceData);
            ObjectNode alarmCondObjectNodeByAlarmKey = createAlarmCondObjectNodeByAlarmKey(mapper, allAlarmConditions);

            byIdObjectNode.putPOJO("byAlarmCondition", alarmCondObjectNodeByAlarmKey);

            globalPropsJson = (ObjectNode) mapper.readValue(globalProps, JsonNode.class);

            globalPropsJson.putPOJO("shared", byIdObjectNode);
            logger.info("valuie of objNode:" + globalPropsJson);
        } else {
            /**
             *  to create json with total properties when no serviceUUID passed
             */
            globalPropsJson = (ObjectNode) mapper.readValue(globalProps, JsonNode.class);
        }
        totalPropsStr = globalPropsJson.toString();
        return totalPropsStr;
    }

    private List<CldsAlarmCondition> getAllAlarmConditionsFromCldsServiceData(CldsServiceData cldsServiceData) {
        List<CldsAlarmCondition> alarmCondList = new ArrayList<>();
        if (cldsServiceData != null && cldsServiceData.getCldsVfs() != null && cldsServiceData.getCldsVfs().size() > 0) {
            for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                if (currCldsVfData != null && currCldsVfData.getCldsVfcs() != null && currCldsVfData.getCldsVfcs().size() > 0) {
                    for (CldsVfcData currCldsVfcData : currCldsVfData.getCldsVfcs()) {
                        if (currCldsVfcData != null && currCldsVfcData.getCldsAlarmConditions() != null && currCldsVfcData.getCldsAlarmConditions().size() > 0) {
                            for (CldsAlarmCondition currCldsAlarmCondition : currCldsVfcData.getCldsAlarmConditions()) {
                                if (currCldsAlarmCondition != null) {
                                    alarmCondList.add(currCldsAlarmCondition);
                                }
                            }
                        }
                    }
                }
            }
        }
        return alarmCondList;
    }

    private ObjectNode createAlarmCondObjectNodeByAlarmKey(ObjectMapper mapper, List<CldsAlarmCondition> cldsAlarmCondList) {
        ObjectNode alarmCondKeyNode = mapper.createObjectNode();

        if (cldsAlarmCondList != null && cldsAlarmCondList.size() > 0) {
            for (CldsAlarmCondition currCldsAlarmCondition : cldsAlarmCondList) {
                if (currCldsAlarmCondition != null) {
                    ObjectNode alarmCondNode = mapper.createObjectNode();
                    alarmCondNode.put("eventSourceType", currCldsAlarmCondition.getEventSourceType());
                    alarmCondNode.put("eventSeverity", currCldsAlarmCondition.getSeverity());
                    alarmCondKeyNode.putPOJO(currCldsAlarmCondition.getAlarmConditionKey(), alarmCondNode);
                }
            }
        } else {
            ObjectNode alarmCondNode = mapper.createObjectNode();
            alarmCondNode.put("eventSourceType", "");
            alarmCondNode.put("eventSeverity", "");
            alarmCondKeyNode.putPOJO("", alarmCondNode);
        }
        return alarmCondKeyNode;
    }

    private ObjectNode createVFObjectNodeByServiceInvariantUUID(ObjectMapper mapper, CldsServiceData cldsServiceData) {
        ObjectNode invariantUUIDObjectNode = mapper.createObjectNode();
        ObjectNode vfObjectNode = mapper.createObjectNode();
        ObjectNode vfUUIDNode = mapper.createObjectNode();
        List<CldsVfData> cldsVfsList = cldsServiceData.getCldsVfs();
        if (cldsVfsList != null && cldsVfsList.size() > 0) {
            for (CldsVfData currCldsVfData : cldsVfsList) {
                if (currCldsVfData != null) {
                    vfUUIDNode.put(currCldsVfData.getVfInvariantResourceUUID(), currCldsVfData.getVfName());
                }
            }
        } else {
            vfUUIDNode.put("", "");
        }
        vfObjectNode.putPOJO("vf", vfUUIDNode);
        invariantUUIDObjectNode.putPOJO(cldsServiceData.getServiceInvariantUUID(), vfObjectNode);
        return invariantUUIDObjectNode;
    }

    private void createAlarmCondObjectNodeByVfcUUID(ObjectMapper mapper, ObjectNode vfcResourceUUIDObjectNode, List<CldsVfcData> cldsVfcDataList) {
        ObjectNode alarmCondContsObjectNode = mapper.createObjectNode();
        ObjectNode alarmCondNode = mapper.createObjectNode();
        //	alarmCondNode.put("", "");
        if (cldsVfcDataList != null && cldsVfcDataList.size() > 0) {
            for (CldsVfcData currCldsVfcData : cldsVfcDataList) {
                if (currCldsVfcData != null) {
                    if (currCldsVfcData.getCldsAlarmConditions() != null && currCldsVfcData.getCldsAlarmConditions().size() > 0) {
                        for (CldsAlarmCondition currCldsAlarmCondition : currCldsVfcData.getCldsAlarmConditions()) {
                            alarmCondNode.put(currCldsAlarmCondition.getAlarmConditionKey(), currCldsAlarmCondition.getAlarmConditionKey());
                        }
                        alarmCondContsObjectNode.putPOJO("alarmCondition", alarmCondNode);
                    }
                    alarmCondContsObjectNode.putPOJO("alarmCondition", alarmCondNode);
                    vfcResourceUUIDObjectNode.putPOJO(currCldsVfcData.getVfcInvariantResourceUUID(), alarmCondContsObjectNode);
                }
            }
        } else {
            alarmCondNode.put("", "");
            alarmCondContsObjectNode.putPOJO("alarmCondition", alarmCondNode);
            vfcResourceUUIDObjectNode.putPOJO("", alarmCondContsObjectNode);
        }
    }

    private ObjectNode createVFCObjectNodeByVfUUID(ObjectMapper mapper, List<CldsVfData> cldsVfDataList) {
        ObjectNode vfUUIDObjectNode = mapper.createObjectNode();

        if (cldsVfDataList != null && cldsVfDataList.size() > 0) {
            for (CldsVfData currCldsVfData : cldsVfDataList) {
                if (currCldsVfData != null) {
                    ObjectNode vfcObjectNode = mapper.createObjectNode();
                    ObjectNode vfcUUIDNode = mapper.createObjectNode();
                    if (currCldsVfData.getCldsVfcs() != null && currCldsVfData.getCldsVfcs().size() > 0) {
                        for (CldsVfcData currCldsVfcData : currCldsVfData.getCldsVfcs()) {
                            vfcUUIDNode.put(currCldsVfcData.getVfcInvariantResourceUUID(), currCldsVfcData.getVfcName());
                        }
                    } else {
                        vfcUUIDNode.put("", "");
                    }
                    vfcObjectNode.putPOJO("vfc", vfcUUIDNode);
                    vfUUIDObjectNode.putPOJO(currCldsVfData.getVfInvariantResourceUUID(), vfcObjectNode);
                }
            }
        } else {
            ObjectNode vfcUUIDNode = mapper.createObjectNode();
            vfcUUIDNode.put("", "");
            ObjectNode vfcObjectNode = mapper.createObjectNode();
            vfcObjectNode.putPOJO("vfc", vfcUUIDNode);
            vfUUIDObjectNode.putPOJO("", vfcObjectNode);
        }
        return vfUUIDObjectNode;
    }

    public String getArtifactIdIfArtifactAlreadyExists(CldsAsdcServiceDetail cldsAsdcServiceDetail, String artifactName) {
        String artifactUUId = null;
        boolean artifactxists = false;
        if (cldsAsdcServiceDetail != null && cldsAsdcServiceDetail.getResources() != null && cldsAsdcServiceDetail.getResources().size() > 0) {
            for (CldsAsdcResource currCldsAsdcResource : cldsAsdcServiceDetail.getResources()) {
                if (artifactxists) {
                    break;
                }
                if (currCldsAsdcResource != null && currCldsAsdcResource.getArtifacts() != null && currCldsAsdcResource.getArtifacts().size() > 0) {
                    for (CldsAsdcArtifact currCldsAsdcArtifact : currCldsAsdcResource.getArtifacts()) {
                        if (currCldsAsdcArtifact != null && currCldsAsdcArtifact.getArtifactName() != null) {
                            if (currCldsAsdcArtifact.getArtifactName().equalsIgnoreCase(artifactName)) {
                                artifactUUId = currCldsAsdcArtifact.getArtifactUUID();
                                artifactxists = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return artifactUUId;
    }

    public String updateControlLoopStatusToDCAE(String dcaeUrl, String invariantResourceUUID, String invariantServiceUUID, String artifactName) {
        String baseUrl = refProp.getStringValue("asdc.serviceUrl");
        String basicAuth = SdcReq.getAsdcBasicAuth(refProp);
        String postStatusData = "{ \n" +
                "\"event\" : \"" + "Created" + "\",\n" +
                "\"serviceUUID\" : \"" + invariantServiceUUID + "\",\n" +
                "\"resourceUUID\" :\"" + invariantResourceUUID + "\",\n" +
                "\"artifactName\" : \"" + artifactName + "\",\n" +
                "} \n";
        try {
            String url = baseUrl;
            if (invariantServiceUUID != null) {
                url = dcaeUrl + "/closed-loops";
            }
            URL urlObj = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestProperty("X-ONAP-InstanceID", "CLAMP-Tool");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestMethod("POST");

            byte[] postData = SdcReq.stringToByteArray(postStatusData);
            try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
            }

            int responseCode = conn.getResponseCode();
            logger.info("responseCode=" + responseCode);

            String resp = getResponse(conn);
            if (resp != null) {
                return resp;
            }
        } catch (Exception e) {
            logger.error("not able to ger any service information from asdc for uuid:" + invariantServiceUUID);
        }
        return "";
    }

    /**
     * To get all asdc VF/VFC Resources basic info
     *
     * @return
     * @throws IOException
     */
    private List<CldsAsdcResourceBasicInfo> getAllAsdcVForVFCResourcesBasedOnResourceType(String resourceType, List<CldsAsdcResourceBasicInfo> allAsdcResources) throws IOException {
        List<CldsAsdcResourceBasicInfo> allAsdcVFResources = new ArrayList<>();
        if (allAsdcResources != null && allAsdcResources.size() > 0) {
            for (CldsAsdcResourceBasicInfo currResource : allAsdcResources) {
                if (currResource != null && currResource.getResourceType() != null && currResource.getResourceType().equalsIgnoreCase(resourceType)) {
                    allAsdcVFResources.add(currResource);
                }
            }
        }
        return allAsdcVFResources;
    }

    private String getResourceUUIDFromResourceInvariantUUID(String resourceInvariantUUID, List<CldsAsdcResourceBasicInfo> resourceInfoList) throws IOException {
        String resourceUUID = null;
        if (resourceInfoList != null && resourceInfoList.size() > 0) {
            for (CldsAsdcResourceBasicInfo currResource : resourceInfoList) {
                if (currResource != null && currResource.getInvariantUUID() != null && currResource.getUuid() != null
                        && currResource.getInvariantUUID().equalsIgnoreCase(resourceInvariantUUID)) {
                    resourceUUID = currResource.getUuid();
                    break;
                }
            }
        }
        return resourceUUID;
    }

    /**
     * To get all asdc Resources basic info
     *
     * @return
     * @throws IOException
     */
    private List<CldsAsdcResourceBasicInfo> getAllAsdcResources() throws IOException {
        String catalogUrl = refProp.getStringValue("asdc.catalog.url");
        String resourceUrl = catalogUrl + "resources";
        String allAsdcResources = getCldsServicesOrResourcesBasedOnURL(resourceUrl, false);
        List<CldsAsdcResourceBasicInfo> allAsdcResourceBasicInfo = getAllAsdcResourcesListFromJson(allAsdcResources);
        return removeDuplicateAsdcResourceBasicInfo(allAsdcResourceBasicInfo);
    }
}
