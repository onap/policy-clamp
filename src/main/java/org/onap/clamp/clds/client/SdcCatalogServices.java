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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

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

import javax.ws.rs.BadRequestException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.onap.clamp.clds.client.req.SdcReq;
import org.onap.clamp.clds.exception.SdcCommunicationException;
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
import org.onap.clamp.clds.model.prop.Global;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class SdcCatalogServices {
    protected static final EELFLogger logger            = EELFManager.getInstance().getLogger(SdcCatalogServices.class);
    protected static final EELFLogger metricsLogger     = EELFManager.getInstance().getMetricsLogger();

    private static final String       RESOURCE_VF_TYPE  = "VF";
    private static final String       RESOURCE_VFC_TYPE = "VFC";

    @Autowired
    private RefProp                   refProp;

    /**
     * This method get the SDC services Information with the corresponding
     * Service UUID.
     * 
     * @param uuid
     *            The service UUID
     * @return A Json String with all the service list
     */
    public String getSdcServicesInformation(String uuid) {
        Date startTime = new Date();
        String baseUrl = refProp.getStringValue("sdc.serviceUrl");
        String basicAuth = SdcReq.getSdcBasicAuth(refProp);
        LoggingUtils.setTargetContext("SDC", "getSdcServicesInformation");

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
            conn.setRequestProperty("X-ECOMP-RequestID", LoggingUtils.getRequestId());
            conn.setRequestMethod("GET");

            String resp = getResponse(conn);
            if (resp != null) {
                logger.info(resp.toString());
                // metrics log
                LoggingUtils.setResponseContext("0", "Get sdc services success", this.getClass().getName());
                return resp;
            }
        } catch (IOException e) {
            LoggingUtils.setResponseContext("900", "Get sdc services failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Get sdc services error");
            logger.error("not able to get any service information from sdc for uuid:" + uuid, e);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("getSdcServicesInformation complete");
        }

        return "";
    }

    /**
     * To remove duplicate serviceUUIDs from sdc services List.
     *
     * @param rawCldsSdcServiceList
     *            A list of CldsSdcServiceInfo
     * @return A list of CldsSdcServiceInfo without duplicate service UUID
     */
    public List<CldsSdcServiceInfo> removeDuplicateServices(List<CldsSdcServiceInfo> rawCldsSdcServiceList) {
        List<CldsSdcServiceInfo> cldsSdcServiceInfoList = null;
        if (rawCldsSdcServiceList != null && !rawCldsSdcServiceList.isEmpty()) {
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
     * To remove duplicate serviceUUIDs from sdc resources List.
     *
     * @param rawCldsSdcResourceList
     * @return
     */
    public List<CldsSdcResource> removeDuplicateSdcResourceInstances(List<CldsSdcResource> rawCldsSdcResourceList) {
        List<CldsSdcResource> cldsSdcResourceList = null;
        if (rawCldsSdcResourceList != null && !rawCldsSdcResourceList.isEmpty()) {
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
     * To remove duplicate basic resources with same resourceUUIDs.
     *
     * @param rawCldsSdcResourceListBasicList
     * @return
     */
    public List<CldsSdcResourceBasicInfo> removeDuplicateSdcResourceBasicInfo(
            List<CldsSdcResourceBasicInfo> rawCldsSdcResourceListBasicList) {
        List<CldsSdcResourceBasicInfo> cldsSdcResourceBasicInfoList = null;
        if (rawCldsSdcResourceListBasicList != null && !rawCldsSdcResourceListBasicList.isEmpty()) {
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
     * To get ServiceUUID by using serviceInvariantUUID.
     *
     * @param invariantId
     *            The invariant ID
     * @return The service UUID
     */
    public String getServiceUuidFromServiceInvariantId(String invariantId) {
        String serviceUuid = "";
        String responseStr = getSdcServicesInformation(null);
        List<CldsSdcServiceInfo> rawCldsSdcServicesList = getCldsSdcServicesListFromJson(responseStr);
        List<CldsSdcServiceInfo> cldsSdcServicesList = removeDuplicateServices(rawCldsSdcServicesList);
        if (cldsSdcServicesList != null && !cldsSdcServicesList.isEmpty()) {
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
     * To get CldsAsdsServiceInfo class by parsing json string.
     *
     * @param jsonStr
     *            The Json string that must be decoded
     * @return The list of CldsSdcServiceInfo, if there is a failure it return
     *         an empty list
     */
    public List<CldsSdcServiceInfo> getCldsSdcServicesListFromJson(String jsonStr) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (StringUtils.isBlank(jsonStr)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(jsonStr,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CldsSdcServiceInfo.class));
        } catch (IOException e) {
            logger.error("Error when attempting to decode the JSON containing CldsSdcServiceInfo", e);
            return new ArrayList<>();
        }
    }

    /**
     * To get List of CldsSdcResourceBasicInfo class by parsing json string.
     *
     * @param jsonStr
     *            The JSOn string that must be decoded
     * @return The list of CldsSdcResourceBasicInfo, an empty list in case of
     *         issues
     */
    public List<CldsSdcResourceBasicInfo> getAllSdcResourcesListFromJson(String jsonStr) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (StringUtils.isBlank(jsonStr)) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(jsonStr,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, CldsSdcResourceBasicInfo.class));
        } catch (IOException e) {
            logger.error("Exception occurred when attempting to decode the list of CldsSdcResourceBasicInfo JSON", e);
            return new ArrayList<>();
        }
    }

    /**
     * To get CldsAsdsResource class by parsing json string.
     *
     * @param jsonStr
     * @return
     * @throws IOException
     */
    public CldsSdcResource getCldsSdcResourceFromJson(String jsonStr) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(jsonStr, CldsSdcResource.class);
    }

    /**
     * To get CldsSdcServiceDetail by parsing json string.
     *
     * @param jsonStr
     * @return
     */
    public CldsSdcServiceDetail getCldsSdcServiceDetailFromJson(String jsonStr) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(jsonStr, CldsSdcServiceDetail.class);
        } catch (IOException e) {
            logger.error("Exception when attempting to decode the CldsSdcServiceDetail JSON", e);
            return null;
        }
    }

    /**
     * To upload artifact to sdc based on serviceUUID and resource name on url.
     *
     * @param prop
     * @param userid
     * @param url
     * @param formattedSdcReq
     * @return
     */
    public String uploadArtifactToSdc(ModelProperties prop, String userid, String url, String formatttedSdcReq) {
        // Verify whether it is triggered by Validation Test button from UI
        if (prop.isTest()) {
            return "sdc artifact upload not executed for test action";
        }
        try {
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
            if (responseStr != null && requestFailed) {
                logger.error("requestFailed - responseStr=" + responseStr);
                throw new BadRequestException(responseStr);
            }
            return responseStr;
        } catch (IOException e) {
            logger.error("Exception when attempting to communicate with SDC", e);
            throw new SdcCommunicationException("Exception when attempting to communicate with SDC", e);
        }

    }

    private HttpURLConnection getSdcHttpUrlConnection(String userid, int postDataLength, String url, String md5Text) {
        try {
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
            conn.setRequestProperty("X-ECOMP-RequestID", LoggingUtils.getRequestId());
            return conn;
        } catch (IOException e) {
            logger.error("Exception when attempting to open connection with SDC", e);
            throw new SdcCommunicationException("Exception when attempting to open connection with SDC", e);
        }
    }

    private String getResponse(HttpURLConnection conn) {
        try (InputStream is = getInputStream(conn)) {
            if (is != null) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    return response.toString();
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            logger.error("Exception when attempting to open SDC response", e);
            throw new SdcCommunicationException("Exception when attempting to open SDC response", e);
        }
    }

    private InputStream getInputStream(HttpURLConnection conn) {
        try {
            InputStream inStream = conn.getErrorStream();
            if (inStream == null) {
                inStream = conn.getInputStream();
            }
            return inStream;
        } catch (IOException e) {
            logger.error("Exception when attempting to open SDC error stream", e);
            throw new SdcCommunicationException("Exception when attempting to open SDC error stream", e);
        }
    }

    public CldsDBServiceCache getCldsDbServiceCacheUsingCldsServiceData(CldsServiceData cldsServiceData) {
        try {
            CldsDBServiceCache cldsDbServiceCache = new CldsDBServiceCache();
            cldsDbServiceCache.setCldsDataInstream(cldsServiceData);
            cldsDbServiceCache.setInvariantId(cldsServiceData.getServiceInvariantUUID());
            cldsDbServiceCache.setServiceId(cldsServiceData.getServiceUUID());
            return cldsDbServiceCache;
        } catch (IOException e) {
            logger.error("Exception when getting service in cache", e);
            throw new SdcCommunicationException("Exception when getting service in cache", e);
        }
    }

    public boolean isCldsSdcCacheDataExpired(CldsServiceData cldsServiceData) {
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

    public CldsServiceData getCldsServiceDataWithAlarmConditions(String invariantServiceUuid) {
        String url = refProp.getStringValue("sdc.serviceUrl");
        String catalogUrl = refProp.getStringValue("sdc.catalog.url");
        String serviceUuid = getServiceUuidFromServiceInvariantId(invariantServiceUuid);
        String serviceDetailUrl = url + "/" + serviceUuid + "/metadata";
        String responseStr = getCldsServicesOrResourcesBasedOnURL(serviceDetailUrl, false);
        ObjectMapper objectMapper = new ObjectMapper();
        CldsServiceData cldsServiceData = new CldsServiceData();
        if (responseStr != null) {
            CldsSdcServiceDetail cldsSdcServiceDetail;
            try {
                cldsSdcServiceDetail = objectMapper.readValue(responseStr, CldsSdcServiceDetail.class);
            } catch (IOException e) {
                logger.error("Exception when decoding the CldsServiceData JSON from SDC", e);
                throw new SdcCommunicationException("Exception when decoding the CldsServiceData JSON from SDC", e);
            }
            cldsServiceData.setServiceUUID(cldsSdcServiceDetail.getUuid());
            cldsServiceData.setServiceInvariantUUID(cldsSdcServiceDetail.getInvariantUUID());

            // To remove duplicate resources from serviceDetail and add valid
            // vfs to service
            if (cldsSdcServiceDetail != null && cldsSdcServiceDetail.getResources() != null) {
                List<CldsSdcResource> cldsSdcResourceList = removeDuplicateSdcResourceInstances(
                        cldsSdcServiceDetail.getResources());
                if (cldsSdcResourceList != null && !cldsSdcResourceList.isEmpty()) {
                    List<CldsVfData> cldsVfDataList = new ArrayList<>();
                    for (CldsSdcResource currCldsSdcResource : cldsSdcResourceList) {
                        if (currCldsSdcResource != null && currCldsSdcResource.getResoucreType() != null
                                && "VF".equalsIgnoreCase(currCldsSdcResource.getResoucreType())) {
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

    private void getAllVfcForVfList(List<CldsVfData> cldsVfDataList, String catalogUrl) {
        // todo : refact this..
        if (cldsVfDataList != null && !cldsVfDataList.isEmpty()) {
            List<CldsSdcResourceBasicInfo> allVfResources = getAllSdcVForVfcResourcesBasedOnResourceType(
                    RESOURCE_VF_TYPE);
            List<CldsSdcResourceBasicInfo> allVfcResources = getAllSdcVForVfcResourcesBasedOnResourceType(
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
                                if (!vfcDataListFromVfResponse.isEmpty()) {
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

    private List<CldsVfcData> getVfcDataListFromVfResponse(String vfResponse) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode vfResponseNode;
        try {
            vfResponseNode = (ObjectNode) mapper.readTree(vfResponse);
        } catch (IOException e) {
            logger.error("Exception when decoding the JSON list of CldsVfcData", e);
            return new ArrayList<>();
        }
        ArrayNode vfcArrayNode = (ArrayNode) vfResponseNode.get("resources");
        List<CldsVfcData> cldsVfcDataList = new ArrayList<>();
        if (vfcArrayNode != null) {
            for (JsonNode vfcjsonNode : vfcArrayNode) {
                CldsVfcData currCldsVfcData = new CldsVfcData();
                ObjectNode currVfcNode = (ObjectNode) vfcjsonNode;
                TextNode resourceTypeNode = (TextNode) currVfcNode.get("resoucreType");
                if (resourceTypeNode != null && "VFC".equalsIgnoreCase(resourceTypeNode.textValue())) {
                    TextNode vfcResourceName = (TextNode) currVfcNode.get("resourceInstanceName");
                    TextNode vfcInvariantResourceUuid = (TextNode) currVfcNode.get("resourceInvariantUUID");
                    currCldsVfcData.setVfcName(vfcResourceName.textValue());
                    currCldsVfcData.setVfcInvariantResourceUUID(vfcInvariantResourceUuid.textValue());
                    cldsVfcDataList.add(currCldsVfcData);
                } else if (resourceTypeNode != null && "CVFC".equalsIgnoreCase(resourceTypeNode.textValue())) {
                    cldsVfcDataList.addAll(getVFCfromCVFC(currVfcNode.get("resourceUUID").textValue()));
                }
            }
        }
        return cldsVfcDataList;
    }

    private List<CldsVfcData> getVFCfromCVFC(String resourceUUID) {
        String catalogUrl = refProp.getStringValue("sdc.catalog.url");
        List<CldsVfcData> cldsVfcDataList = new ArrayList<>();

        if (resourceUUID != null) {
            String vfcResourceUUIDUrl = catalogUrl + "resources" + "/" + resourceUUID + "/metadata";
            try {
                String vfcResponse = getCldsServicesOrResourcesBasedOnURL(vfcResourceUUIDUrl, false);
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode vfResponseNode = (ObjectNode) mapper.readTree(vfcResponse);
                ArrayNode vfcArrayNode = (ArrayNode) vfResponseNode.get("resources");

                if (vfcArrayNode != null) {
                    for (JsonNode vfcjsonNode : vfcArrayNode) {
                        CldsVfcData currCldsVfcData = new CldsVfcData();
                        ObjectNode currVfcNode = (ObjectNode) vfcjsonNode;
                        TextNode resourceTypeNode = (TextNode) currVfcNode.get("resoucreType");
                        if (resourceTypeNode != null && "VFC".equalsIgnoreCase(resourceTypeNode.textValue())) {
                            TextNode vfcResourceName = (TextNode) currVfcNode.get("resourceInstanceName");
                            TextNode vfcInvariantResourceUUID = (TextNode) currVfcNode.get("resourceInvariantUUID");
                            currCldsVfcData.setVfcName(vfcResourceName.textValue());
                            currCldsVfcData.setVfcInvariantResourceUUID(vfcInvariantResourceUUID.textValue());
                            cldsVfcDataList.add(currCldsVfcData);
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Exception during JSON analyzis", e);
            }
        }
        return cldsVfcDataList;
    }

    private String removeUnwantedBracesFromString(String id) {
        return (id != null) ? id.replaceAll("\"", "") : "";
    }

    private List<CldsAlarmCondition> getAlarmCondtionsFromVfc(String vfcResponse) {
        List<CldsAlarmCondition> cldsAlarmConditionList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode vfcResponseNode;
        try {
            vfcResponseNode = (ObjectNode) mapper.readTree(vfcResponse);
        } catch (IOException e) {
            logger.error("Exception when decoding the JSON list of CldsAlarmCondition", e);
            return cldsAlarmConditionList;
        }
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

    private List<CldsAlarmCondition> parseCsvToGetAlarmConditions(String allAlarmCondsValues) {
        try {
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
        } catch (IOException e) {
            logger.error("Exception when attempting to parse the CSV containing the alarm", e);
            return new ArrayList<>();
        }
    }

    // Method to get the artifact for any particular VF
    private List<CldsVfKPIData> getFieldPathFromVF(String vfResponse) {
        List<CldsVfKPIData> cldsVfKPIDataList = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode vfResponseNode;
        try {
            vfResponseNode = (ObjectNode) mapper.readTree(vfResponse);
        } catch (IOException e) {
            logger.error("Exception when decoding the JSON list of CldsVfKPIData", e);
            return cldsVfKPIDataList;
        }
        ArrayNode artifactsArrayNode = (ArrayNode) vfResponseNode.get("artifacts");

        if (artifactsArrayNode != null && artifactsArrayNode.size() > 0) {
            for (int index = 0; index < artifactsArrayNode.size(); index++) {
                ObjectNode currArtifactNode = (ObjectNode) artifactsArrayNode.get(index);
                TextNode artifactUrlNode = (TextNode) currArtifactNode.get("artifactURL");
                TextNode artifactNameNode = (TextNode) currArtifactNode.get("artifactName");
                String artifactName = "";
                if (artifactNameNode != null) {
                    artifactName = artifactNameNode.textValue();
                    artifactName = artifactName.substring(artifactName.lastIndexOf('.') + 1);
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
    private List<CldsVfKPIData> parseCsvToGetFieldPath(String allFieldPathValues) {
        try {
            List<CldsVfKPIData> cldsVfKPIDataList = new ArrayList<>();
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
        } catch (IOException e) {
            logger.error("Exception when attempting to parse the CSV containing the alarm kpi data", e);
            return new ArrayList<>();
        }
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
        cldsAlarmCondition.setEventName(record.get(2));
        cldsAlarmCondition.setAlarmConditionKey(record.get(3));
        cldsAlarmCondition.setSeverity(record.get(4));
        cldsAlarmConditionList.add(cldsAlarmCondition);
    }

    public String getResponsesFromArtifactUrl(String artifactsUrl) {
        String hostUrl = refProp.getStringValue("sdc.hostUrl");
        String artifactsUrlReworked = artifactsUrl.replaceAll("\"", "");
        String artifactUrl = hostUrl + artifactsUrlReworked;
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
     */
    public String getCldsServicesOrResourcesBasedOnURL(String url, boolean alarmConditions) {
        try {
            String urlReworked = removeUnwantedBracesFromString(url);
            URL urlObj = new URL(urlReworked);

            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            String basicAuth = SdcReq.getSdcBasicAuth(refProp);
            conn.setRequestProperty(refProp.getStringValue("sdc.InstanceID"), "CLAMP-Tool");
            conn.setRequestProperty("Authorization", basicAuth);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("X-ECOMP-RequestID", LoggingUtils.getRequestId());
            conn.setRequestMethod("GET");

            int responseCode = conn.getResponseCode();
            logger.info("responseCode=" + responseCode);
            StringBuilder response;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (!inputLine.isEmpty()) {
                        response.append(inputLine);
                    }
                    if (alarmConditions) {
                        response.append("\n");
                    }
                }
            }
            return response.toString();
        } catch (IOException e) {
            logger.error("Exception occurred during query to SDC", e);
            return "";
        }

    }

    /**
     * To create properties object by using cldsServicedata.
     *
     * @param globalProps
     * @param cldsServiceData
     * @return
     */
    public String createPropertiesObjectByUUID(String globalProps, CldsServiceData cldsServiceData) {
        String totalPropsStr;
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode globalPropsJson;
        if (cldsServiceData != null && cldsServiceData.getServiceUUID() != null) {

            // Objectnode to save all byservice, byvf , byvfc and byalarm nodes
            ObjectNode byIdObjectNode = mapper.createObjectNode();

            // To create vf ResourceUUID node with serviceInvariantUUID
            ObjectNode invariantUuidObjectNodeWithVf = createVfObjectNodeByServiceInvariantUuid(mapper,
                    cldsServiceData);
            byIdObjectNode.putPOJO("byService", invariantUuidObjectNodeWithVf);

            // To create byVf and vfcResourceNode with vfResourceUUID
            ObjectNode vfcObjectNodeByVfUuid = createVfcObjectNodeByVfUuid(mapper, cldsServiceData.getCldsVfs());
            byIdObjectNode.putPOJO("byVf", vfcObjectNodeByVfUuid);

            // To create byKpi
            ObjectNode kpiObjectNode = mapper.createObjectNode();
            if (cldsServiceData.getCldsVfs() != null && !cldsServiceData.getCldsVfs().isEmpty()) {
                for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                    if (currCldsVfData != null) {
                        createKpiObjectNodeByVfUuid(mapper, kpiObjectNode, currCldsVfData.getCldsKPIList());
                    }
                }
            }
            byIdObjectNode.putPOJO("byKpi", kpiObjectNode);

            // To create byVfc and alarmCondition with vfcResourceUUID
            ObjectNode vfcResourceUuidObjectNode = mapper.createObjectNode();
            if (cldsServiceData.getCldsVfs() != null && !cldsServiceData.getCldsVfs().isEmpty()) {
                for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                    if (currCldsVfData != null) {
                        createAlarmCondObjectNodeByVfcUuid(mapper, vfcResourceUuidObjectNode,
                                currCldsVfData.getCldsVfcs());
                    }
                }
            }
            byIdObjectNode.putPOJO("byVfc", vfcResourceUuidObjectNode);

            // To create byAlarmCondition with alarmConditionKey
            List<CldsAlarmCondition> allAlarmConditions = getAllAlarmConditionsFromCldsServiceData(cldsServiceData,
                    "alarmCondition");
            ObjectNode alarmCondObjectNodeByAlarmKey = createAlarmCondObjectNodeByAlarmKey(mapper, allAlarmConditions);

            byIdObjectNode.putPOJO("byAlarmCondition", alarmCondObjectNodeByAlarmKey);

            // To create byAlertDescription with AlertDescription
            List<CldsAlarmCondition> allAlertDescriptions = getAllAlarmConditionsFromCldsServiceData(cldsServiceData,
                    "alertDescription");
            ObjectNode alertDescObjectNodeByAlert = createAlarmCondObjectNodeByAlarmKey(mapper, allAlertDescriptions);

            byIdObjectNode.putPOJO("byAlertDescription", alertDescObjectNodeByAlert);

            globalPropsJson = decodeGlobalProp(globalProps, mapper);

            globalPropsJson.putPOJO("shared", byIdObjectNode);
            logger.info("value of objNode:" + globalPropsJson);
        } else {
            /**
             * to create json with total properties when no serviceUUID passed
             */
            globalPropsJson = decodeGlobalProp(globalProps, mapper);
        }
        totalPropsStr = globalPropsJson.toString();
        return totalPropsStr;
    }

    private ObjectNode decodeGlobalProp(String globalProps, ObjectMapper mapper) {
        try {
            return (ObjectNode) mapper.readValue(globalProps, JsonNode.class);
        } catch (IOException e) {
            logger.error("Exception occurred during decoding of the global props, returning an empty objectNode", e);
            return mapper.createObjectNode();
        }
    }

    /**
     * Method to get alarm conditions/alert description from Service Data.
     * 
     * @param cldsServiceData
     *            CldsServiceData the Service Data to analyze
     * @param eventName
     *            The String event name that will be used to filter the alarm
     *            list
     * @return The list of CldsAlarmCondition for the event name specified
     */
    public List<CldsAlarmCondition> getAllAlarmConditionsFromCldsServiceData(CldsServiceData cldsServiceData,
            String eventName) {
        List<CldsAlarmCondition> alarmCondList = new ArrayList<>();
        if (cldsServiceData != null && cldsServiceData.getCldsVfs() != null
                && !cldsServiceData.getCldsVfs().isEmpty()) {
            for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                alarmCondList.addAll(getAllAlarmConditionsFromCldsVfData(currCldsVfData, eventName));
            }
        }
        return alarmCondList;
    }

    /**
     * Method to get alarm conditions/alert description from VF Data.
     * 
     * @param currCldsVfData
     *            The Vf Data to analyze
     * @param eventName
     *            The String event name that will be used to filter the alarm
     *            list
     * @return The list of CldsAlarmCondition for the event name specified
     */
    private List<CldsAlarmCondition> getAllAlarmConditionsFromCldsVfData(CldsVfData currCldsVfData, String eventName) {
        List<CldsAlarmCondition> alarmCondList = new ArrayList<>();

        if (currCldsVfData != null && currCldsVfData.getCldsVfcs() != null && !currCldsVfData.getCldsVfcs().isEmpty()) {
            for (CldsVfcData currCldsVfcData : currCldsVfData.getCldsVfcs()) {
                alarmCondList.addAll(getAllAlarmConditionsFromCldsVfcData(currCldsVfcData, eventName));
            }
        }
        return alarmCondList;
    }

    /**
     * Method to get alarm conditions/alert description from VFC Data.
     * 
     * @param currCldsVfcData
     *            The VfC Data to analyze
     * @param eventName
     *            The String event name that will be used to filter the alarm
     *            list
     * @return The list of CldsAlarmCondition for the event name specified
     */
    private List<CldsAlarmCondition> getAllAlarmConditionsFromCldsVfcData(CldsVfcData currCldsVfcData,
            String eventName) {
        List<CldsAlarmCondition> alarmCondList = new ArrayList<>();

        if (currCldsVfcData != null && currCldsVfcData.getCldsAlarmConditions() != null
                && !currCldsVfcData.getCldsAlarmConditions().isEmpty()) {
            for (CldsAlarmCondition currCldsAlarmCondition : currCldsVfcData.getCldsAlarmConditions()) {
                if (currCldsAlarmCondition != null
                        && currCldsAlarmCondition.getEventName().equalsIgnoreCase(eventName)) {
                    alarmCondList.add(currCldsAlarmCondition);
                }
            }
        }
        return alarmCondList;
    }

    private ObjectNode createAlarmCondObjectNodeByAlarmKey(ObjectMapper mapper,
            List<CldsAlarmCondition> cldsAlarmCondList) {
        ObjectNode alarmCondKeyNode = mapper.createObjectNode();

        if (cldsAlarmCondList != null && !cldsAlarmCondList.isEmpty()) {
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

    private ObjectNode createVfObjectNodeByServiceInvariantUuid(ObjectMapper mapper, CldsServiceData cldsServiceData) {
        ObjectNode invariantUuidObjectNode = mapper.createObjectNode();
        ObjectNode vfObjectNode = mapper.createObjectNode();
        ObjectNode vfUuidNode = mapper.createObjectNode();
        List<CldsVfData> cldsVfsList = cldsServiceData.getCldsVfs();
        if (cldsVfsList != null && !cldsVfsList.isEmpty()) {
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

    private void createKpiObjectNodeByVfUuid(ObjectMapper mapper, ObjectNode vfResourceUuidObjectNode,
            List<CldsVfKPIData> cldsVfKpiDataList) {
        if (cldsVfKpiDataList != null && !cldsVfKpiDataList.isEmpty()) {
            for (CldsVfKPIData currCldsVfKpiData : cldsVfKpiDataList) {
                if (currCldsVfKpiData != null) {
                    ObjectNode thresholdNameObjectNode = mapper.createObjectNode();

                    ObjectNode fieldPathObjectNode = mapper.createObjectNode();
                    ObjectNode nfNamingCodeNode = mapper.createObjectNode();

                    fieldPathObjectNode.put(currCldsVfKpiData.getFieldPathValue(),
                            currCldsVfKpiData.getFieldPathValue());
                    nfNamingCodeNode.put(currCldsVfKpiData.getNfNamingValue(), currCldsVfKpiData.getNfNamingValue());

                    thresholdNameObjectNode.putPOJO("fieldPath", fieldPathObjectNode);
                    thresholdNameObjectNode.putPOJO("nfNamingCode", nfNamingCodeNode);

                    vfResourceUuidObjectNode.putPOJO(currCldsVfKpiData.getThresholdValue(), thresholdNameObjectNode);
                }
            }
        }
    }

    private void createAlarmCondObjectNodeByVfcUuid(ObjectMapper mapper, ObjectNode vfcResourceUuidObjectNode,
            List<CldsVfcData> cldsVfcDataList) {
        ObjectNode vfcObjectNode = mapper.createObjectNode();
        ObjectNode alarmCondNode = mapper.createObjectNode();
        ObjectNode alertDescNode = mapper.createObjectNode();
        if (cldsVfcDataList != null && !cldsVfcDataList.isEmpty()) {
            for (CldsVfcData currCldsVfcData : cldsVfcDataList) {
                if (currCldsVfcData != null) {
                    if (currCldsVfcData.getCldsAlarmConditions() != null
                            && !currCldsVfcData.getCldsAlarmConditions().isEmpty()) {
                        for (CldsAlarmCondition currCldsAlarmCondition : currCldsVfcData.getCldsAlarmConditions()) {
                            alarmCondNode.put(currCldsAlarmCondition.getAlarmConditionKey(),
                                    currCldsAlarmCondition.getAlarmConditionKey());
                            if (currCldsAlarmCondition.getEventName().equalsIgnoreCase("alarmCondition")) {
                                alarmCondNode.put(currCldsAlarmCondition.getAlarmConditionKey(),
                                        currCldsAlarmCondition.getAlarmConditionKey());
                            } else {
                                alertDescNode.put(currCldsAlarmCondition.getAlarmConditionKey(),
                                        currCldsAlarmCondition.getAlarmConditionKey());
                            }
                        }
                    }

                    vfcObjectNode.putPOJO("alarmCondition", alarmCondNode);
                    vfcObjectNode.putPOJO("alertDescription", alertDescNode);
                    vfcResourceUuidObjectNode.putPOJO(currCldsVfcData.getVfcInvariantResourceUUID(), vfcObjectNode);
                }
            }
        } else {
            alarmCondNode.put("", "");
            vfcObjectNode.putPOJO("alarmCondition", alarmCondNode);
            alertDescNode.put("", "");
            vfcObjectNode.putPOJO("alertDescription", alarmCondNode);
            vfcResourceUuidObjectNode.putPOJO("", vfcObjectNode);
        }
    }

    private ObjectNode createVfcObjectNodeByVfUuid(ObjectMapper mapper, List<CldsVfData> cldsVfDataList) {
        ObjectNode vfUuidObjectNode = mapper.createObjectNode();

        if (cldsVfDataList != null && !cldsVfDataList.isEmpty()) {
            for (CldsVfData currCldsVfData : cldsVfDataList) {
                if (currCldsVfData != null) {
                    ObjectNode vfcObjectNode = mapper.createObjectNode();
                    ObjectNode vfcUuidNode = mapper.createObjectNode();
                    if (currCldsVfData.getCldsVfcs() != null && !currCldsVfData.getCldsVfcs().isEmpty()) {
                        for (CldsVfcData currCldsVfcData : currCldsVfData.getCldsVfcs()) {
                            vfcUuidNode.put(currCldsVfcData.getVfcInvariantResourceUUID(),
                                    currCldsVfcData.getVfcName());
                        }
                    } else {
                        vfcUuidNode.put("", "");
                    }
                    vfcObjectNode.putPOJO("vfc", vfcUuidNode);
                    vfUuidObjectNode.putPOJO(currCldsVfData.getVfInvariantResourceUUID(), vfcObjectNode);
                }
            }
        } else {
            ObjectNode vfcUuidNode = mapper.createObjectNode();
            vfcUuidNode.put("", "");
            ObjectNode vfcObjectNode = mapper.createObjectNode();
            vfcObjectNode.putPOJO("vfc", vfcUuidNode);
            vfUuidObjectNode.putPOJO("", vfcObjectNode);
        }
        return vfUuidObjectNode;
    }

    /**
     * This method searches the equivalent artifact UUID for a specific
     * artifactName in a SdcServiceDetail.
     * 
     * @param cldsSdcServiceDetail
     *            The SdcServiceDetail that will be analyzed
     * @param artifactName
     *            The artifact name that will be searched
     * @return The artifact UUID found
     */
    public String getArtifactIdIfArtifactAlreadyExists(CldsSdcServiceDetail cldsSdcServiceDetail, String artifactName) {
        String artifactUuid = null;
        boolean artifactExists = false;
        if (cldsSdcServiceDetail != null && cldsSdcServiceDetail.getResources() != null
                && !cldsSdcServiceDetail.getResources().isEmpty()) {
            for (CldsSdcResource currCldsSdcResource : cldsSdcServiceDetail.getResources()) {
                if (artifactExists) {
                    break;
                }
                if (currCldsSdcResource != null && currCldsSdcResource.getArtifacts() != null
                        && !currCldsSdcResource.getArtifacts().isEmpty()) {
                    for (CldsSdcArtifact currCldsSdcArtifact : currCldsSdcResource.getArtifacts()) {
                        if (currCldsSdcArtifact != null && currCldsSdcArtifact.getArtifactName() != null
                                && currCldsSdcArtifact.getArtifactName().equalsIgnoreCase(artifactName)) {
                            artifactUuid = currCldsSdcArtifact.getArtifactUUID();
                            artifactExists = true;
                            break;
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
            conn.setRequestProperty("X-ECOMP-RequestID", LoggingUtils.getRequestId());
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
        } catch (IOException e) {
            logger.error("Not able to get any service information from sdc for uuid:" + invariantServiceUuid, e);
        }
        return "";
    }

    /**
     * To get all sdc VF/VFC Resources basic info.
     * 
     * @param resourceType
     *            The resourceType
     * @return The list of CldsSdcResourceBasicInfo
     */
    private List<CldsSdcResourceBasicInfo> getAllSdcVForVfcResourcesBasedOnResourceType(String resourceType) {
        String catalogUrl = refProp.getStringValue("sdc.catalog.url");
        String resourceUrl = catalogUrl + "resources?resourceType=" + resourceType;
        String allSdcVfcResources = getCldsServicesOrResourcesBasedOnURL(resourceUrl, false);
        return removeDuplicateSdcResourceBasicInfo(getAllSdcResourcesListFromJson(allSdcVfcResources));
    }

    private String getResourceUuidFromResourceInvariantUuid(String resourceInvariantUuid,
            List<CldsSdcResourceBasicInfo> resourceInfoList) {
        String resourceUuid = null;
        if (resourceInfoList != null && !resourceInfoList.isEmpty()) {
            for (CldsSdcResourceBasicInfo currResource : resourceInfoList) {
                if (currResource != null && currResource.getInvariantUUID() != null && currResource.getUuid() != null
                        && currResource.getInvariantUUID().equalsIgnoreCase(resourceInvariantUuid)) {
                    resourceUuid = currResource.getUuid();
                    break;
                }
            }
        }
        return resourceUuid;
    }

    /**
     * Method to get service invariant uuid from model properties.
     * 
     * @param props
     *            The Clds model properties
     * @return The Service Id
     */
    private String getServiceInvariantUuidFromProps(ModelProperties props) {
        String invariantUuid = "";
        Global globalProps = props.getGlobal();
        if (globalProps != null && globalProps.getService() != null) {
            invariantUuid = globalProps.getService();
        }
        return invariantUuid;
    }

    /**
     * This method upload the BluePrint to SDC.
     * 
     * @param prop
     *            The Clds model Properties
     * @param userid
     *            The user id for SDC
     * @param sdcReqUrlsList
     *            The list of SDC URL to try
     * @param formattedSdcReq
     *            The blueprint to upload
     * @param formattedSdcLocationReq
     *            THe location Blueprint to upload
     * @param artifactName
     *            The artifact name from where we can get the Artifact UUID
     * @param locationArtifactName
     *            The location artifact name from where we can get the Artifact
     *            UUID
     * 
     */
    public void uploadToSdc(ModelProperties prop, String userid, List<String> sdcReqUrlsList, String formattedSdcReq,
            String formattedSdcLocationReq, String artifactName, String locationArtifactName) {
        logger.info("userid=" + userid);
        String serviceInvariantUuid = getServiceInvariantUuidFromProps(prop);
        if (sdcReqUrlsList != null && !sdcReqUrlsList.isEmpty()) {
            for (String url : sdcReqUrlsList) {
                if (url != null) {
                    String originalServiceUuid = getServiceUuidFromServiceInvariantId(serviceInvariantUuid);
                    logger.info("ServiceUUID used before upload in url:" + originalServiceUuid);
                    String sdcServicesInformation = getSdcServicesInformation(originalServiceUuid);
                    CldsSdcServiceDetail cldsSdcServiceDetail = getCldsSdcServiceDetailFromJson(sdcServicesInformation);
                    String uploadedArtifactUuid = getArtifactIdIfArtifactAlreadyExists(cldsSdcServiceDetail,
                            artifactName);
                    // Upload artifacts to sdc
                    String updateUrl = uploadedArtifactUuid != null ? url + "/" + uploadedArtifactUuid : url;
                    String responseStr = uploadArtifactToSdc(prop, userid, updateUrl, formattedSdcReq);
                    logger.info("value of sdc Response of uploading to sdc :" + responseStr);
                    String updatedServiceUuid = getServiceUuidFromServiceInvariantId(serviceInvariantUuid);
                    if (!originalServiceUuid.equalsIgnoreCase(updatedServiceUuid)) {
                        url = url.replace(originalServiceUuid, updatedServiceUuid);
                    }
                    logger.info("ServiceUUID used after upload in ulr:" + updatedServiceUuid);
                    sdcServicesInformation = getSdcServicesInformation(updatedServiceUuid);
                    cldsSdcServiceDetail = getCldsSdcServiceDetailFromJson(sdcServicesInformation);
                    uploadedArtifactUuid = getArtifactIdIfArtifactAlreadyExists(cldsSdcServiceDetail,
                            locationArtifactName);
                    // To send location information also to sdc
                    updateUrl = uploadedArtifactUuid != null ? url + "/" + uploadedArtifactUuid : url;
                    responseStr = uploadArtifactToSdc(prop, userid, updateUrl, formattedSdcLocationReq);
                    logger.info("value of sdc Response of uploading location to sdc :" + responseStr);
                }
            }
        }
    }
}
