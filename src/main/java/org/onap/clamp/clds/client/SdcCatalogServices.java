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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.onap.clamp.clds.client.req.SdcReq;
import org.onap.clamp.clds.model.CldsAlarmCondition;
import org.onap.clamp.clds.model.CldsDBServiceCache;
import org.onap.clamp.clds.model.CldsSdcArtifact;
import org.onap.clamp.clds.model.CldsSdcResource;
import org.onap.clamp.clds.model.CldsSdcResourceBasicInfo;
import org.onap.clamp.clds.model.CldsSdcServiceDetail;
import org.onap.clamp.clds.model.CldsSdcServiceInfo;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsVfData;
import org.onap.clamp.clds.model.CldsVfKPIData;
import org.onap.clamp.clds.model.CldsVfcData;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class SdcCatalogServices {
    protected static final EELFLogger logger            = EELFManager.getInstance().getLogger(SdcCatalogServices.class);
    protected static final EELFLogger metricsLogger     = EELFManager.getInstance().getMetricsLogger();

    private static final String       RESOURCE_VF_TYPE  = "VF";
    private static final String       RESOURCE_VFC_TYPE = "VFC";

    @Autowired
    private RefProp                   refProp;

    public String getSdcServicesInformation(String uuid) throws Exception {
        Date startTime = new Date();
        String baseUrl = refProp.getStringValue("sdc.serviceUrl");
        String basicAuth = SdcReq.getSdcBasicAuth(refProp);
        try {
            String url = baseUrl;
            if (uuid != null) {
                url = baseUrl + "/" + uuid + "/metadata";
            }
            URL urlObj = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

            conn.setRequestProperty(refProp.getStringValue("sdc.InstanceID"), "CLAMP-Tool");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestMethod("GET");

            String resp = getResponse(conn);
            if (resp != null) {
                logger.info(resp.toString());
                return resp;
            }
            // metrics log
            LoggingUtils.setResponseContext("0", "Get sdc services success", this.getClass().getName());

        } catch (Exception e) {
            LoggingUtils.setResponseContext("900", "Get sdc services failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Get sdc services error");
            logger.error("not able to get any service information from sdc for uuid:" + uuid);
        }
        LoggingUtils.setTimeContext(startTime, new Date());
        LoggingUtils.setTargetContext("SDC", "Get Services");
        metricsLogger.info("Get sdc services information");

        return "";
    }

    /**
     * To remove duplicate serviceUUIDs from sdc services List
     *
     * @param rawCldsSdcServiceList
     * @return
     */
    public List<CldsSdcServiceInfo> removeDuplicateServices(List<CldsSdcServiceInfo> rawCldsSdcServiceList) {
        List<CldsSdcServiceInfo> cldsSdcServiceInfoList = null;
        if (rawCldsSdcServiceList != null && rawCldsSdcServiceList.size() > 0) {
            // sort list
            Collections.sort(rawCldsSdcServiceList);
            // and then take only the services with the max version (last in the
            // list with the same name)
            cldsSdcServiceInfoList = new ArrayList<>();
            for (int i = 1; i < rawCldsSdcServiceList.size(); i++) {
                // compare name with previous - if not equal, then keep the
                // previous (it's the last with that name)
                CldsSdcServiceInfo prev = rawCldsSdcServiceList.get(i - 1);
                if (!rawCldsSdcServiceList.get(i).getName().equals(prev.getName())) {
                    cldsSdcServiceInfoList.add(prev);
                }
            }
            // add the last in the list
            cldsSdcServiceInfoList.add(rawCldsSdcServiceList.get(rawCldsSdcServiceList.size() - 1));
        }
        return cldsSdcServiceInfoList;
    }

    /**
     * To remove duplicate serviceUUIDs from sdc resources List
     *
     * @param rawCldsSdcResourceList
     * @return
     */
    public List<CldsSdcResource> removeDuplicateSdcResourceInstances(List<CldsSdcResource> rawCldsSdcResourceList) {
        List<CldsSdcResource> cldsSdcResourceList = null;
        if (rawCldsSdcResourceList != null && rawCldsSdcResourceList.size() > 0) {
            // sort list
            Collections.sort(rawCldsSdcResourceList);
            // and then take only the resources with the max version (last in
            // the list with the same name)
            cldsSdcResourceList = new ArrayList<>();
            for (int i = 1; i < rawCldsSdcResourceList.size(); i++) {
                // compare name with previous - if not equal, then keep the
                // previous (it's the last with that name)
                CldsSdcResource prev = rawCldsSdcResourceList.get(i - 1);
                if (!rawCldsSdcResourceList.get(i).getResourceInstanceName().equals(prev.getResourceInstanceName())) {
                    cldsSdcResourceList.add(prev);
                }
            }
            // add the last in the list
            cldsSdcResourceList.add(rawCldsSdcResourceList.get(rawCldsSdcResourceList.size() - 1));
        }
        return cldsSdcResourceList;
    }

    /**
     * To remove duplicate basic resources with same resourceUUIDs
     *
     * @param rawCldsSdcResourceListBasicList
     * @return
     */
    public List<CldsSdcResourceBasicInfo> removeDuplicateSdcResourceBasicInfo(
            List<CldsSdcResourceBasicInfo> rawCldsSdcResourceListBasicList) {
        List<CldsSdcResourceBasicInfo> cldsSdcResourceBasicInfoList = null;
        if (rawCldsSdcResourceListBasicList != null && rawCldsSdcResourceListBasicList.size() > 0) {
            // sort list
            Collections.sort(rawCldsSdcResourceListBasicList);
            // and then take only the resources with the max version (last in
            // the list with the same name)
            cldsSdcResourceBasicInfoList = new ArrayList<>();
            for (int i = 1; i < rawCldsSdcResourceListBasicList.size(); i++) {
                // compare name with previous - if not equal, then keep the
                // previous (it's the last with that name)
                CldsSdcResourceBasicInfo prev = rawCldsSdcResourceListBasicList.get(i - 1);
                if (!rawCldsSdcResourceListBasicList.get(i).getName().equals(prev.getName())) {
                    cldsSdcResourceBasicInfoList.add(prev);
                }
            }
            // add the last in the list
            cldsSdcResourceBasicInfoList
                    .add(rawCldsSdcResourceListBasicList.get(rawCldsSdcResourceListBasicList.size() - 1));
        }
        return cldsSdcResourceBasicInfoList;
    }

    /**
     * To get ServiceUUID by using serviceInvariantUUID
     *
     * @param invariantId
     * @return
     * @throws Exception
     */
    public String getServiceUuidFromServiceInvariantId(String invariantId) throws Exception {
        String serviceUuid = "";
        String responseStr = getSdcServicesInformation(null);
        List<CldsSdcServiceInfo> rawCldsSdcServicesList = getCldsSdcServicesListFromJson(responseStr);
        List<CldsSdcServiceInfo> cldsSdcServicesList = removeDuplicateServices(rawCldsSdcServicesList);
        if (cldsSdcServicesList != null && cldsSdcServicesList.size() > 0) {
            for (CldsSdcServiceInfo currCldsSdcServiceInfo : cldsSdcServicesList) {
                if (currCldsSdcServiceInfo != null && currCldsSdcServiceInfo.getInvariantUUID() != null
                        && currCldsSdcServiceInfo.getInvariantUUID().equalsIgnoreCase(invariantId)) {
                    serviceUuid = currCldsSdcServiceInfo.getUuid();
                    break;
                }
            }
        }
        return serviceUuid;
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
    public List<CldsSdcServiceInfo> getCldsSdcServicesListFromJson(String jsonStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        return objectMapper.readValue(jsonStr,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CldsSdcServiceInfo.class));
    }

    /**
     * To get List<CldsSdcResourceBasicInfo> class by parsing json string
     *
     * @param jsonStr
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public List<CldsSdcResourceBasicInfo> getAllSdcResourcesListFromJson(String jsonStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        if (StringUtils.isBlank(jsonStr)) {
            return null;
        }
        return objectMapper.readValue(jsonStr,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CldsSdcResourceBasicInfo.class));
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
    public CldsSdcResource getCldsSdcResourceFromJson(String jsonStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonStr, CldsSdcResource.class);
    }

    /**
     * To get CldsSdcServiceDetail by parsing json string
     *
     * @param jsonStr
     * @return
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    public CldsSdcServiceDetail getCldsSdcServiceDetailFromJson(String jsonStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonStr, CldsSdcServiceDetail.class);
    }

    /**
     * To upload artifact to sdc based on serviceUUID and resourcename on url
     *
     * @param prop
     * @param userid
     * @param url
     * @param formatttedSdcReq
     * @return
     * @throws Exception
     */
    public String uploadArtifactToSdc(ModelProperties prop, String userid, String url, String formatttedSdcReq)
            throws Exception {
        // Verify whether it is triggered by Validation Test button from UI
        if (prop.isTest()) {
            return "sdc artifact upload not executed for test action";
        }
        logger.info("userid=" + userid);
        String md5Text = SdcReq.calculateMD5ByString(formatttedSdcReq);
        byte[] postData = SdcReq.stringToByteArray(formatttedSdcReq);
        int postDataLength = postData.length;
        HttpURLConnection conn = getSdcHttpUrlConnection(userid, postDataLength, url, md5Text);
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

    private HttpURLConnection getSdcHttpUrlConnection(String userid, int postDataLength, String url, String md5Text)
            throws IOException {
        logger.info("userid=" + userid);
        String basicAuth = SdcReq.getSdcBasicAuth(refProp);
        String sdcXonapInstanceId = refProp.getStringValue("sdc.sdcX-InstanceID");
        URL urlObj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
        conn.setDoOutput(true);
        conn.setRequestProperty(refProp.getStringValue("sdc.InstanceID"), sdcXonapInstanceId);
        conn.setRequestProperty("Authorization", basicAuth);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Content-MD5", md5Text);
        conn.setRequestProperty("USER_ID", userid);
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

    public CldsDBServiceCache getCldsDbServiceCacheUsingCldsServiceData(CldsServiceData cldsServiceData)
            throws IOException {
        CldsDBServiceCache cldsDbServiceCache = new CldsDBServiceCache();
        cldsDbServiceCache.setCldsDataInstream(cldsServiceData);
        cldsDbServiceCache.setInvariantId(cldsServiceData.getServiceInvariantUUID());
        cldsDbServiceCache.setServiceId(cldsServiceData.getServiceUUID());
        return cldsDbServiceCache;
    }

    public boolean isCldsSdcCacheDataExpired(CldsServiceData cldsServiceData) throws Exception {
        boolean expired = false;
        if (cldsServiceData != null && cldsServiceData.getServiceUUID() != null) {
            String cachedServiceUuid = cldsServiceData.getServiceUUID();
            String latestServiceUuid = getServiceUuidFromServiceInvariantId(cldsServiceData.getServiceInvariantUUID());
            String defaultRecordAge = refProp.getStringValue("CLDS_SERVICE_CACHE_MAX_SECONDS");
            if ((!cachedServiceUuid.equalsIgnoreCase(latestServiceUuid)) || (cldsServiceData.getAgeOfRecord() != null
                    && cldsServiceData.getAgeOfRecord() > Long.parseLong(defaultRecordAge))) {
                expired = true;
            }
        } else {
            expired = true;
        }
        return expired;
    }

    public CldsServiceData getCldsServiceDataWithAlarmConditions(String invariantServiceUuid) throws Exception {
        String url = refProp.getStringValue("sdc.serviceUrl");
        String catalogUrl = refProp.getStringValue("sdc.catalog.url");
        String serviceUuid = getServiceUuidFromServiceInvariantId(invariantServiceUuid);
        String serviceDetailUrl = url + "/" + serviceUuid + "/metadata";
        String responseStr = getCldsServicesOrResourcesBasedOnURL(serviceDetailUrl, false);
        ObjectMapper objectMapper = new ObjectMapper();
        CldsServiceData cldsServiceData = new CldsServiceData();
        if (responseStr != null) {
            CldsSdcServiceDetail cldsSdcServiceDetail = objectMapper.readValue(responseStr, CldsSdcServiceDetail.class);
            cldsServiceData.setServiceUUID(cldsSdcServiceDetail.getUuid());
            cldsServiceData.setServiceInvariantUUID(cldsSdcServiceDetail.getInvariantUUID());

            // To remove duplicate resources from serviceDetail and add valid
            // vfs to service
            if (cldsSdcServiceDetail != null && cldsSdcServiceDetail.getResources() != null) {
                List<CldsSdcResource> cldsSdcResourceList = removeDuplicateSdcResourceInstances(
                        cldsSdcServiceDetail.getResources());
                if (cldsSdcResourceList != null && cldsSdcResourceList.size() > 0) {
                    List<CldsVfData> cldsVfDataList = new ArrayList<>();
                    for (CldsSdcResource currCldsSdcResource : cldsSdcResourceList) {
                        if (currCldsSdcResource != null && currCldsSdcResource.getResoucreType() != null
                                && currCldsSdcResource.getResoucreType().equalsIgnoreCase("VF")) {
                            CldsVfData currCldsVfData = new CldsVfData();
                            currCldsVfData.setVfName(currCldsSdcResource.getResourceInstanceName());
                            currCldsVfData.setVfInvariantResourceUUID(currCldsSdcResource.getResourceInvariantUUID());
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
            List<CldsSdcResourceBasicInfo> allVfResources = getAllSdcVForVFCResourcesBasedOnResourceType(
                    RESOURCE_VF_TYPE);
            List<CldsSdcResourceBasicInfo> allVfcResources = getAllSdcVForVFCResourcesBasedOnResourceType(
                    RESOURCE_VFC_TYPE);
            for (CldsVfData currCldsVfData : cldsVfDataList) {
                if (currCldsVfData != null && currCldsVfData.getVfInvariantResourceUUID() != null) {
                    String resourceUuid = getResourceUuidFromResourceInvariantUuid(
                            currCldsVfData.getVfInvariantResourceUUID(), allVfResources);
                    if (resourceUuid != null) {
                        String vfResourceUuidUrl = catalogUrl + "resources" + "/" + resourceUuid + "/metadata";
                        String vfResponse = getCldsServicesOrResourcesBasedOnURL(vfResourceUuidUrl, false);
                        if (vfResponse != null) {
                            // Below 2 line are to get the KPI(field path) data
                            // associated with the VF's
                            List<CldsVfKPIData> cldsVfKPIDataList = getFieldPathFromVF(vfResponse);
                            currCldsVfData.setCldsKPIList(cldsVfKPIDataList);

                            List<CldsVfcData> vfcDataListFromVfResponse = getVfcDataListFromVfResponse(vfResponse);
                            if (vfcDataListFromVfResponse != null) {
                                currCldsVfData.setCldsVfcs(vfcDataListFromVfResponse);
                                if (vfcDataListFromVfResponse.size() > 0) {
                                    // To get artifacts for every VFC and get
                                    // alarm conditions from artifact
                                    for (CldsVfcData currCldsVfcData : vfcDataListFromVfResponse) {
                                        if (currCldsVfcData != null
                                                && currCldsVfcData.getVfcInvariantResourceUUID() != null) {
                                            String resourceVfcUuid = getResourceUuidFromResourceInvariantUuid(
                                                    currCldsVfcData.getVfcInvariantResourceUUID(), allVfcResources);
                                            if (resourceVfcUuid != null) {
                                                String vfcResourceUuidUrl = catalogUrl + "resources" + "/"
                                                        + resourceVfcUuid + "/metadata";
                                                String vfcResponse = getCldsServicesOrResourcesBasedOnURL(
                                                        vfcResourceUuidUrl, false);
                                                if (vfcResponse != null) {
                                                    List<CldsAlarmCondition> alarmCondtionsFromVfc = getAlarmCondtionsFromVfc(
                                                            vfcResponse);
                                                    currCldsVfcData.setCldsAlarmConditions(alarmCondtionsFromVfc);
                                                }
                                            } else {
                                                logger.info("No resourceVFC UUID found for given invariantID:"
                                                        + currCldsVfcData.getVfcInvariantResourceUUID());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        logger.info("No resourceUUID found for given invariantREsourceUUID:"
                                + currCldsVfData.getVfInvariantResourceUUID());
                    }
                }
            }
        }
    }

    private List<CldsVfcData> getVfcDataListFromVfResponse(String vfResponse) throws IOException {
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
                    TextNode vfcInvariantResourceUuid = (TextNode) currVfcNode.get("resourceInvariantUUID");
                    currCldsVfcData.setVfcName(vfcResourceName.textValue());
                    currCldsVfcData.setVfcInvariantResourceUUID(vfcInvariantResourceUuid.textValue());
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

    // Method to get the artifact for any particular VF
    private List<CldsVfKPIData> getFieldPathFromVF(String vfResponse) throws JsonProcessingException, IOException {
        List<CldsVfKPIData> cldsVfKPIDataList = new ArrayList<CldsVfKPIData>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode vfResponseNode = (ObjectNode) mapper.readTree(vfResponse);
        ArrayNode artifactsArrayNode = (ArrayNode) vfResponseNode.get("artifacts");

        if (artifactsArrayNode != null && artifactsArrayNode.size() > 0) {
            for (int index = 0; index < artifactsArrayNode.size(); index++) {
                ObjectNode currArtifactNode = (ObjectNode) artifactsArrayNode.get(index);
                TextNode artifactUrlNode = (TextNode) currArtifactNode.get("artifactURL");
                TextNode artifactNameNode = (TextNode) currArtifactNode.get("artifactName");
                String artifactName = "";
                if (artifactNameNode != null) {
                    artifactName = artifactNameNode.textValue();
                    artifactName = artifactName.substring(artifactName.lastIndexOf(".") + 1);
                }
                if (artifactUrlNode != null && artifactName != null && !artifactName.isEmpty()
                        && artifactName.equalsIgnoreCase("csv")) {
                    String responsesFromArtifactUrl = getResponsesFromArtifactUrl(artifactUrlNode.textValue());
                    cldsVfKPIDataList.addAll(parseCsvToGetFieldPath(responsesFromArtifactUrl));
                    logger.info(responsesFromArtifactUrl);
                }
            }
        }
        return cldsVfKPIDataList;
    }

    private CldsVfKPIData convertCsvRecordToKpiData(CSVRecord record) {
        if (record.size() < 6) {
            logger.debug("invalid csv field path Record,total columns less than 6: " + record);
            return null;
        }

        if (StringUtils.isBlank(record.get(1)) || StringUtils.isBlank(record.get(3))
                || StringUtils.isBlank(record.get(5))) {
            logger.debug("Invalid csv field path Record,one of column is having blank value : " + record);
            return null;
        }

        CldsVfKPIData cldsVfKPIData = new CldsVfKPIData();
        cldsVfKPIData.setNfNamingCode(record.get(0).trim());
        cldsVfKPIData.setNfNamingValue(record.get(1).trim());

        cldsVfKPIData.setFieldPath(record.get(2).trim());
        cldsVfKPIData.setFieldPathValue(record.get(3).trim());

        cldsVfKPIData.setThresholdName(record.get(4).trim());
        cldsVfKPIData.setThresholdValue(record.get(5).trim());
        return cldsVfKPIData;

    }

    // Method to get the artifactURL Data and set the CldsVfKPIData node
    private List<CldsVfKPIData> parseCsvToGetFieldPath(String allFieldPathValues) throws IOException {
        List<CldsVfKPIData> cldsVfKPIDataList = new ArrayList<CldsVfKPIData>();
        Reader alarmReader = new StringReader(allFieldPathValues);
        Iterable<CSVRecord> records = CSVFormat.RFC4180.parse(alarmReader);
        if (records != null) {
            for (CSVRecord record : records) {
                CldsVfKPIData kpiData = this.convertCsvRecordToKpiData(record);
                if (kpiData != null) {
                    cldsVfKPIDataList.add(kpiData);
                }
            }
        }
        return cldsVfKPIDataList;
    }

    private void processRecord(List<CldsAlarmCondition> cldsAlarmConditionList, CSVRecord record) {
        if (record == null) {
            return;
        }
        if (record.size() < 5) {
            logger.debug("invalid csv alarm Record,total columns less than 5: " + record);
            return;
        }
        if (StringUtils.isBlank(record.get(1)) || StringUtils.isBlank(record.get(3))
                || StringUtils.isBlank(record.get(4))) {
            logger.debug("invalid csv alarm Record,one of column is having blank value : " + record);
            return;
        }
        CldsAlarmCondition cldsAlarmCondition = new CldsAlarmCondition();
        cldsAlarmCondition.setEventSourceType(record.get(1));
        cldsAlarmCondition.setAlarmConditionKey(record.get(3));
        cldsAlarmCondition.setSeverity(record.get(4));
        cldsAlarmConditionList.add(cldsAlarmCondition);
    }

    public String getResponsesFromArtifactUrl(String artifactsUrl) throws IOException {
        String hostUrl = refProp.getStringValue("sdc.hostUrl");
        artifactsUrl = artifactsUrl.replaceAll("\"", "");
        String artifactUrl = hostUrl + artifactsUrl;
        logger.info("value of artifactURl:" + artifactUrl);
        String currArtifactResponse = getCldsServicesOrResourcesBasedOnURL(artifactUrl, true);
        logger.info("value of artifactResponse:" + currArtifactResponse);
        return currArtifactResponse;
    }

    /**
     * Service to services/resources/artifacts from sdc.Pass alarmConditions as
     * true to get alarmconditons from artifact url and else it is false
     *
     * @param url
     * @param alarmConditions
     * @return
     * @throws IOException
     */
    public String getCldsServicesOrResourcesBasedOnURL(String url, boolean alarmConditions) {
        String responseStr;
        try {
            url = removeUnwantedBracesFromString(url);
            URL urlObj = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            String basicAuth = SdcReq.getSdcBasicAuth(refProp);
            conn.setRequestProperty(refProp.getStringValue("sdc.InstanceID"), "CLAMP-Tool");
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
            return responseStr;
        } catch (Exception e) {
            logger.error("Exception occurred :", e);
            return "";
        }

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

            // Objectnode to save all byservice, byvf , byvfc and byalarm nodes
            ObjectNode byIdObjectNode = mapper.createObjectNode();

            // To create vf ResourceUUID node with serviceInvariantUUID
            ObjectNode invariantUuidObjectNodeWithVF = createVFObjectNodeByServiceInvariantUUID(mapper,
                    cldsServiceData);
            byIdObjectNode.putPOJO("byService", invariantUuidObjectNodeWithVF);

            // To create byVf and vfcResourceNode with vfResourceUUID
            ObjectNode vfcObjectNodeByVfUuid = createVFCObjectNodeByVfUuid(mapper, cldsServiceData.getCldsVfs());
            byIdObjectNode.putPOJO("byVf", vfcObjectNodeByVfUuid);

            // To create byKpi
            ObjectNode kpiObjectNode = mapper.createObjectNode();
            if (cldsServiceData.getCldsVfs() != null && cldsServiceData.getCldsVfs().size() > 0) {
                for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                    if (currCldsVfData != null) {
                        createKPIObjectNodeByVfUUID(mapper, kpiObjectNode, currCldsVfData.getCldsKPIList());
                    }
                }
            }
            byIdObjectNode.putPOJO("byKpi", kpiObjectNode);

            // To create byVfc and alarmCondition with vfcResourceUUID
            ObjectNode vfcResourceUuidObjectNode = mapper.createObjectNode();
            if (cldsServiceData.getCldsVfs() != null && cldsServiceData.getCldsVfs().size() > 0) {
                for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                    if (currCldsVfData != null) {
                        createAlarmCondObjectNodeByVfcUuid(mapper, vfcResourceUuidObjectNode,
                                currCldsVfData.getCldsVfcs());
                    }
                }
            }
            byIdObjectNode.putPOJO("byVfc", vfcResourceUuidObjectNode);

            // To create byAlarmCondition with alarmConditionKey
            List<CldsAlarmCondition> allAlarmConditions = getAllAlarmConditionsFromCldsServiceData(cldsServiceData);
            ObjectNode alarmCondObjectNodeByAlarmKey = createAlarmCondObjectNodeByAlarmKey(mapper, allAlarmConditions);

            byIdObjectNode.putPOJO("byAlarmCondition", alarmCondObjectNodeByAlarmKey);

            globalPropsJson = (ObjectNode) mapper.readValue(globalProps, JsonNode.class);

            globalPropsJson.putPOJO("shared", byIdObjectNode);
            logger.info("valuie of objNode:" + globalPropsJson);
        } else {
            /**
             * to create json with total properties when no serviceUUID passed
             */
            globalPropsJson = (ObjectNode) mapper.readValue(globalProps, JsonNode.class);
        }
        totalPropsStr = globalPropsJson.toString();
        return totalPropsStr;
    }

    public List<CldsAlarmCondition> getAllAlarmConditionsFromCldsServiceData(CldsServiceData cldsServiceData) {
        List<CldsAlarmCondition> alarmCondList = new ArrayList<>();
        if (cldsServiceData != null && cldsServiceData.getCldsVfs() != null
                && cldsServiceData.getCldsVfs().size() > 0) {
            for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                if (currCldsVfData != null && currCldsVfData.getCldsVfcs() != null
                        && currCldsVfData.getCldsVfcs().size() > 0) {
                    for (CldsVfcData currCldsVfcData : currCldsVfData.getCldsVfcs()) {
                        if (currCldsVfcData != null && currCldsVfcData.getCldsAlarmConditions() != null
                                && currCldsVfcData.getCldsAlarmConditions().size() > 0) {
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

    private ObjectNode createAlarmCondObjectNodeByAlarmKey(ObjectMapper mapper,
            List<CldsAlarmCondition> cldsAlarmCondList) {
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
        ObjectNode invariantUuidObjectNode = mapper.createObjectNode();
        ObjectNode vfObjectNode = mapper.createObjectNode();
        ObjectNode vfUuidNode = mapper.createObjectNode();
        List<CldsVfData> cldsVfsList = cldsServiceData.getCldsVfs();
        if (cldsVfsList != null && cldsVfsList.size() > 0) {
            for (CldsVfData currCldsVfData : cldsVfsList) {
                if (currCldsVfData != null) {
                    vfUuidNode.put(currCldsVfData.getVfInvariantResourceUUID(), currCldsVfData.getVfName());
                }
            }
        } else {
            vfUuidNode.put("", "");
        }
        vfObjectNode.putPOJO("vf", vfUuidNode);
        invariantUuidObjectNode.putPOJO(cldsServiceData.getServiceInvariantUUID(), vfObjectNode);
        return invariantUuidObjectNode;
    }

    private void createKPIObjectNodeByVfUUID(ObjectMapper mapper, ObjectNode vfResourceUUIDObjectNode,
            List<CldsVfKPIData> cldsVfKPIDataList) {
        if (cldsVfKPIDataList != null && cldsVfKPIDataList.size() > 0) {
            for (CldsVfKPIData currCldsVfKPIData : cldsVfKPIDataList) {
                if (currCldsVfKPIData != null) {
                    ObjectNode thresholdNameObjectNode = mapper.createObjectNode();

                    ObjectNode fieldPathObjectNode = mapper.createObjectNode();
                    ObjectNode nfNamingCodeNode = mapper.createObjectNode();

                    fieldPathObjectNode.put(currCldsVfKPIData.getFieldPathValue(),
                            currCldsVfKPIData.getFieldPathValue());
                    nfNamingCodeNode.put(currCldsVfKPIData.getNfNamingValue(), currCldsVfKPIData.getNfNamingValue());

                    thresholdNameObjectNode.putPOJO("fieldPath", fieldPathObjectNode);
                    thresholdNameObjectNode.putPOJO("nfNamingCode", nfNamingCodeNode);

                    vfResourceUUIDObjectNode.putPOJO(currCldsVfKPIData.getThresholdValue(), thresholdNameObjectNode);
                }
            }
        }
    }

    private void createAlarmCondObjectNodeByVfcUuid(ObjectMapper mapper, ObjectNode vfcResourceUUIDObjectNode,
            List<CldsVfcData> cldsVfcDataList) {
        ObjectNode alarmCondContsObjectNode = mapper.createObjectNode();
        ObjectNode alarmCondNode = mapper.createObjectNode();
        // alarmCondNode.put("", "");
        if (cldsVfcDataList != null && cldsVfcDataList.size() > 0) {
            for (CldsVfcData currCldsVfcData : cldsVfcDataList) {
                if (currCldsVfcData != null) {
                    if (currCldsVfcData.getCldsAlarmConditions() != null
                            && currCldsVfcData.getCldsAlarmConditions().size() > 0) {
                        for (CldsAlarmCondition currCldsAlarmCondition : currCldsVfcData.getCldsAlarmConditions()) {
                            alarmCondNode.put(currCldsAlarmCondition.getAlarmConditionKey(),
                                    currCldsAlarmCondition.getAlarmConditionKey());
                        }
                        alarmCondContsObjectNode.putPOJO("alarmCondition", alarmCondNode);
                    }
                    alarmCondContsObjectNode.putPOJO("alarmCondition", alarmCondNode);
                    vfcResourceUUIDObjectNode.putPOJO(currCldsVfcData.getVfcInvariantResourceUUID(),
                            alarmCondContsObjectNode);
                }
            }
        } else {
            alarmCondNode.put("", "");
            alarmCondContsObjectNode.putPOJO("alarmCondition", alarmCondNode);
            vfcResourceUUIDObjectNode.putPOJO("", alarmCondContsObjectNode);
        }
    }

    private ObjectNode createVFCObjectNodeByVfUuid(ObjectMapper mapper, List<CldsVfData> cldsVfDataList) {
        ObjectNode vfUUIDObjectNode = mapper.createObjectNode();

        if (cldsVfDataList != null && cldsVfDataList.size() > 0) {
            for (CldsVfData currCldsVfData : cldsVfDataList) {
                if (currCldsVfData != null) {
                    ObjectNode vfcObjectNode = mapper.createObjectNode();
                    ObjectNode vfcUuidNode = mapper.createObjectNode();
                    if (currCldsVfData.getCldsVfcs() != null && currCldsVfData.getCldsVfcs().size() > 0) {
                        for (CldsVfcData currCldsVfcData : currCldsVfData.getCldsVfcs()) {
                            vfcUuidNode.put(currCldsVfcData.getVfcInvariantResourceUUID(),
                                    currCldsVfcData.getVfcName());
                        }
                    } else {
                        vfcUuidNode.put("", "");
                    }
                    vfcObjectNode.putPOJO("vfc", vfcUuidNode);
                    vfUUIDObjectNode.putPOJO(currCldsVfData.getVfInvariantResourceUUID(), vfcObjectNode);
                }
            }
        } else {
            ObjectNode vfcUuidNode = mapper.createObjectNode();
            vfcUuidNode.put("", "");
            ObjectNode vfcObjectNode = mapper.createObjectNode();
            vfcObjectNode.putPOJO("vfc", vfcUuidNode);
            vfUUIDObjectNode.putPOJO("", vfcObjectNode);
        }
        return vfUUIDObjectNode;
    }

    public String getArtifactIdIfArtifactAlreadyExists(CldsSdcServiceDetail CldsSdcServiceDetail, String artifactName) {
        String artifactUuid = null;
        boolean artifactxists = false;
        if (CldsSdcServiceDetail != null && CldsSdcServiceDetail.getResources() != null
                && CldsSdcServiceDetail.getResources().size() > 0) {
            for (CldsSdcResource currCldsSdcResource : CldsSdcServiceDetail.getResources()) {
                if (artifactxists) {
                    break;
                }
                if (currCldsSdcResource != null && currCldsSdcResource.getArtifacts() != null
                        && currCldsSdcResource.getArtifacts().size() > 0) {
                    for (CldsSdcArtifact currCldsSdcArtifact : currCldsSdcResource.getArtifacts()) {
                        if (currCldsSdcArtifact != null && currCldsSdcArtifact.getArtifactName() != null) {
                            if (currCldsSdcArtifact.getArtifactName().equalsIgnoreCase(artifactName)) {
                                artifactUuid = currCldsSdcArtifact.getArtifactUUID();
                                artifactxists = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return artifactUuid;
    }

    public String updateControlLoopStatusToDcae(String dcaeUrl, String invariantResourceUuid,
            String invariantServiceUuid, String artifactName) {
        String baseUrl = refProp.getStringValue("sdc.serviceUrl");
        String basicAuth = SdcReq.getSdcBasicAuth(refProp);
        String postStatusData = "{ \n" + "\"event\" : \"" + "Created" + "\",\n" + "\"serviceUUID\" : \""
                + invariantServiceUuid + "\",\n" + "\"resourceUUID\" :\"" + invariantResourceUuid + "\",\n"
                + "\"artifactName\" : \"" + artifactName + "\",\n" + "} \n";
        try {
            String url = baseUrl;
            if (invariantServiceUuid != null) {
                url = dcaeUrl + "/closed-loops";
            }
            URL urlObj = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestProperty(refProp.getStringValue("sdc.InstanceID"), "CLAMP-Tool");
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
            logger.error("not able to ger any service information from sdc for uuid:" + invariantServiceUuid);
        }
        return "";
    }

    /**
     * To get all sdc VF/VFC Resources basic info
     *
     * @return
     * @throws IOException
     */
    private List<CldsSdcResourceBasicInfo> getAllSdcVForVFCResourcesBasedOnResourceType(String resourceType)
            throws IOException {
        List<CldsSdcResourceBasicInfo> allSdcResourceVFCBasicInfo = new ArrayList<CldsSdcResourceBasicInfo>();
        String catalogUrl = refProp.getStringValue("sdc.catalog.url");
        String resourceUrl = catalogUrl + "resources?resourceType=" + resourceType;
        String allSdcVFCResources = getCldsServicesOrResourcesBasedOnURL(resourceUrl, false);

        allSdcResourceVFCBasicInfo = getAllSdcResourcesListFromJson(allSdcVFCResources);
        return removeDuplicateSdcResourceBasicInfo(allSdcResourceVFCBasicInfo);
    }

    private String getResourceUuidFromResourceInvariantUuid(String resourceInvariantUUID,
            List<CldsSdcResourceBasicInfo> resourceInfoList) throws IOException {
        String resourceUuid = null;
        if (resourceInfoList != null && resourceInfoList.size() > 0) {
            for (CldsSdcResourceBasicInfo currResource : resourceInfoList) {
                if (currResource != null && currResource.getInvariantUUID() != null && currResource.getUuid() != null
                        && currResource.getInvariantUUID().equalsIgnoreCase(resourceInvariantUUID)) {
                    resourceUuid = currResource.getUuid();
                    break;
                }
            }
        }
        return resourceUuid;
    }
}
