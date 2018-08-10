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

package org.onap.clamp.clds.client.req.sdc;

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
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.exception.sdc.SdcCommunicationException;
import org.onap.clamp.clds.model.CldsAlarmCondition;
import org.onap.clamp.clds.model.CldsServiceData;
import org.onap.clamp.clds.model.CldsVfData;
import org.onap.clamp.clds.model.CldsVfKPIData;
import org.onap.clamp.clds.model.CldsVfcData;
import org.onap.clamp.clds.model.properties.Global;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.sdc.SdcArtifact;
import org.onap.clamp.clds.model.sdc.SdcResource;
import org.onap.clamp.clds.model.sdc.SdcResourceBasicInfo;
import org.onap.clamp.clds.model.sdc.SdcServiceDetail;
import org.onap.clamp.clds.model.sdc.SdcServiceInfo;
import org.onap.clamp.clds.service.CldsService;
import org.onap.clamp.clds.util.CryptoUtils;
import org.onap.clamp.clds.util.JacksonUtils;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SdcCatalogServices {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(SdcCatalogServices.class);
    private static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    private static final String RESOURCE_VF_TYPE = "VF";
    private static final String RESOURCE_VFC_TYPE = "VFC";
    private static final String RESOURCE_CVFC_TYPE = "CVFC";
    private static final String SDC_REQUESTID_PROPERTY_NAME = "sdc.header.requestId";
    private static final String SDC_METADATA_URL_PREFIX = "/metadata";
    private static final String SDC_INSTANCE_ID_PROPERTY_NAME = "sdc.InstanceID";
    private static final String SDC_CATALOG_URL_PROPERTY_NAME = "sdc.catalog.url";
    private static final String SDC_SERVICE_URL_PROPERTY_NAME = "sdc.serviceUrl";
    private static final String SDC_INSTANCE_ID_CLAMP = "CLAMP-Tool";
    private static final String RESOURCE_URL_PREFIX = "resources";
    private static final LoggingUtils utils = new LoggingUtils (logger);
    @Autowired
    private ClampProperties refProp;

    /**
     * Return SDC id and pw as a HTTP Basic Auth string (for example: Basic
     * dGVzdDoxMjM0NTY=).
     *
     * @return The String with Basic Auth and password
     * @throws GeneralSecurityException
     *             In case of issue when decryting the SDC password
     * @throws DecoderException
     *             In case of issues with the decoding of the HexString message
     */
    private String getSdcBasicAuth() throws GeneralSecurityException, DecoderException {
        String sdcId = refProp.getStringValue("sdc.serviceUsername");
        String sdcPw = refProp.getStringValue("sdc.servicePassword");
        String password = CryptoUtils.decrypt(sdcPw);
        String idPw = Base64.getEncoder().encodeToString((sdcId + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + idPw;
    }

    /**
     * This method get the SDC services Information with the corresponding
     * Service UUID.
     * 
     * @param uuid
     *            The service UUID
     * @return A Json String with all the service list
     * @throws GeneralSecurityException
     *             In case of issue when decryting the SDC password
     * @throws DecoderException
     *             In case of issues with the decoding of the Hex String
     */
    public String getSdcServicesInformation(String uuid) throws GeneralSecurityException, DecoderException {
        Date startTime = new Date();
        String baseUrl = refProp.getStringValue(SDC_SERVICE_URL_PROPERTY_NAME);
        String basicAuth = getSdcBasicAuth();
        try {
            String url = baseUrl;
            if (uuid != null && !uuid.isEmpty()) {
                url = baseUrl + "/" + uuid + SDC_METADATA_URL_PREFIX;
            }
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn = utils.invoke(conn,"SDC", "getSdcServicesInformation");
            conn.setRequestProperty(refProp.getStringValue(SDC_INSTANCE_ID_PROPERTY_NAME), SDC_INSTANCE_ID_CLAMP);
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, basicAuth);
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
            conn.setRequestProperty(refProp.getStringValue(SDC_REQUESTID_PROPERTY_NAME), LoggingUtils.getRequestId());
            conn.setRequestMethod("GET");
            String resp = getResponse(conn);
            logger.debug("Services list received from SDC:" + resp);
            utils.invokeReturn();
            return resp;
        } catch (IOException e) {
            LoggingUtils.setResponseContext("900", "Get sdc services failed", this.getClass().getName());
            LoggingUtils.setErrorContext("900", "Get sdc services error");
            logger.error("not able to get any service information from sdc for uuid:" + uuid, e);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("getSdcServicesInformation complete");
        }
        utils.invokeReturn();
        return "";
    }

    /**
     * To remove duplicate serviceUUIDs from sdc services List.
     * 
     * @param rawCldsSdcServiceList
     *            A list of CldsSdcServiceInfo
     * @return A list of CldsSdcServiceInfo without duplicate service UUID
     */
    public List<SdcServiceInfo> removeDuplicateServices(List<SdcServiceInfo> rawCldsSdcServiceList) {
        List<SdcServiceInfo> cldsSdcServiceInfoList = null;
        if (rawCldsSdcServiceList != null && !rawCldsSdcServiceList.isEmpty()) {
            // sort list
            Collections.sort(rawCldsSdcServiceList);
            // and then take only the services with the max version (last in the
            // list with the same name)
            cldsSdcServiceInfoList = new ArrayList<>();
            for (int i = 1; i < rawCldsSdcServiceList.size(); i++) {
                // compare name with previous - if not equal, then keep the
                // previous (it's the last with that name)
                SdcServiceInfo prev = rawCldsSdcServiceList.get(i - 1);
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
     * @return List of CldsSdcResource
     */
    public List<SdcResource> removeDuplicateSdcResourceInstances(List<SdcResource> rawCldsSdcResourceList) {
        List<SdcResource> cldsSdcResourceList = null;
        if (rawCldsSdcResourceList != null && !rawCldsSdcResourceList.isEmpty()) {
            // sort list
            Collections.sort(rawCldsSdcResourceList);
            // and then take only the resources with the max version (last in
            // the list with the same name)
            cldsSdcResourceList = new ArrayList<>();
            for (int i = 1; i < rawCldsSdcResourceList.size(); i++) {
                // compare name with previous - if not equal, then keep the
                // previous (it's the last with that name)
                SdcResource prev = rawCldsSdcResourceList.get(i - 1);
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
     * @return List of CldsSdcResourceBasicInfo
     */
    public List<SdcResourceBasicInfo> removeDuplicateSdcResourceBasicInfo(
            List<SdcResourceBasicInfo> rawCldsSdcResourceListBasicList) {
        List<SdcResourceBasicInfo> cldsSdcResourceBasicInfoList = new ArrayList<>();
        if (rawCldsSdcResourceListBasicList != null && !rawCldsSdcResourceListBasicList.isEmpty()) {
            // sort list
            Collections.sort(rawCldsSdcResourceListBasicList);
            // and then take only the resources with the max version (last in
            // the list with the same name)
            for (int i = 1; i < rawCldsSdcResourceListBasicList.size(); i++) {
                // compare name with previous - if not equal, then keep the
                // previous (it's the last with that name)
                SdcResourceBasicInfo prev = rawCldsSdcResourceListBasicList.get(i - 1);
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
     * @throws GeneralSecurityException
     *             In case of issue when decryting the SDC password
     * @throws DecoderException
     *             In case of issues with the decoding of the Hex String
     */
    public String getServiceUuidFromServiceInvariantId(String invariantId)
            throws GeneralSecurityException, DecoderException {
        String serviceUuid = "";
        String responseStr = getSdcServicesInformation(null);
        List<SdcServiceInfo> rawCldsSdcServicesList = getCldsSdcServicesListFromJson(responseStr);
        List<SdcServiceInfo> cldsSdcServicesList = removeDuplicateServices(rawCldsSdcServicesList);
        if (cldsSdcServicesList != null && !cldsSdcServicesList.isEmpty()) {
            for (SdcServiceInfo currCldsSdcServiceInfo : cldsSdcServicesList) {
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
    private List<SdcServiceInfo> getCldsSdcServicesListFromJson(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return new ArrayList<>();
        }
        try {
            return JacksonUtils.getObjectMapperInstance().readValue(jsonStr, JacksonUtils.getObjectMapperInstance()
                    .getTypeFactory().constructCollectionType(List.class, SdcServiceInfo.class));
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
    private List<SdcResourceBasicInfo> getAllSdcResourcesListFromJson(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) {
            return new ArrayList<>();
        }
        try {
            return JacksonUtils.getObjectMapperInstance().readValue(jsonStr, JacksonUtils.getObjectMapperInstance()
                    .getTypeFactory().constructCollectionType(List.class, SdcResourceBasicInfo.class));
        } catch (IOException e) {
            logger.error("Exception occurred when attempting to decode the list of CldsSdcResourceBasicInfo JSON", e);
            return new ArrayList<>();
        }
    }

    /**
     * To get CldsSdcServiceDetail by parsing json string.
     * 
     * @param jsonStr
     * @return
     */
    public SdcServiceDetail decodeCldsSdcServiceDetailFromJson(String jsonStr) {
        try {
            return JacksonUtils.getObjectMapperInstance().readValue(jsonStr, SdcServiceDetail.class);
        } catch (IOException e) {
            logger.error("Exception when attempting to decode the CldsSdcServiceDetail JSON", e);
            return null;
        }
    }

    // upload artifact to sdc based on serviceUUID and resource name on url
    private String uploadArtifactToSdc(ModelProperties prop, String userid, String url, String formattedSdcReq) {
        // Verify whether it is triggered by Validation Test button from UI
        if (prop.isTestOnly()) {
            return "sdc artifact upload not executed for test action";
        }
        try {
            logger.info("userid=" + userid);
            byte[] postData = formattedSdcReq.getBytes(StandardCharsets.UTF_8);
            int postDataLength = postData.length;
            HttpURLConnection conn = getSdcHttpUrlConnection(userid, postDataLength, url, formattedSdcReq);
            conn = utils.invoke(conn,"SDC", "uploadArtifact");
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
                utils.invokeReturn();
                throw new BadRequestException(responseStr);
            }
            utils.invokeReturn();
            return responseStr;
        } catch (IOException e) {
            logger.error("Exception when attempting to communicate with SDC", e);
            utils.invokeReturn();
            throw new SdcCommunicationException("Exception when attempting to communicate with SDC", e);
        }
    }

    private HttpURLConnection getSdcHttpUrlConnection(String userid, int postDataLength, String url, String content) {
        try {
            logger.info("userid=" + userid);
            String basicAuth = getSdcBasicAuth();
            String sdcXonapInstanceId = refProp.getStringValue("sdc.sdcX-InstanceID");
            URL urlObj = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setDoOutput(true);
            conn.setRequestProperty(refProp.getStringValue(SDC_INSTANCE_ID_PROPERTY_NAME), sdcXonapInstanceId);
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, basicAuth);
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json");
            conn.setRequestProperty(HttpHeaders.CONTENT_MD5,
                    Base64.getEncoder().encodeToString(DigestUtils.md5Hex(content).getBytes("UTF-8")));
            conn.setRequestProperty("USER_ID", userid);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("charset", "utf-8");
            conn.setRequestProperty(HttpHeaders.CONTENT_LENGTH, Integer.toString(postDataLength));
            conn.setUseCaches(false);
            conn.setRequestProperty(refProp.getStringValue(SDC_REQUESTID_PROPERTY_NAME), LoggingUtils.getRequestId());
            return conn;
        } catch (IOException e) {
            logger.error("Exception when attempting to open connection with SDC", e);
            throw new SdcCommunicationException("Exception when attempting to open connection with SDC", e);
        } catch (DecoderException e) {
            logger.error("Exception when attempting to decode the Hex string", e);
            throw new SdcCommunicationException("Exception when attempting to decode the Hex string", e);
        } catch (GeneralSecurityException e) {
            logger.error("Exception when attempting to decrypt the encrypted password", e);
            throw new SdcCommunicationException("Exception when attempting to decrypt the encrypted password", e);
        }
    }

    private String getResponse(HttpURLConnection conn) {
        try (InputStream is = getInputStream(conn)) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                return IOUtils.toString(in);
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

    /**
     * Check if the SDC Info in cache has expired.
     * 
     * @param cldsServiceData
     *            The object representing the service data
     * @return boolean flag
     * @throws GeneralSecurityException
     *             In case of issues with the decryting the encrypted password
     * @throws DecoderException
     *             In case of issues with the decoding of the Hex String
     */
    public boolean isCldsSdcCacheDataExpired(CldsServiceData cldsServiceData)
            throws GeneralSecurityException, DecoderException {
        if (cldsServiceData != null && cldsServiceData.getServiceUUID() != null) {
            String cachedServiceUuid = cldsServiceData.getServiceUUID();
            String latestServiceUuid = getServiceUuidFromServiceInvariantId(cldsServiceData.getServiceInvariantUUID());
            String configuredMaxAge = refProp.getStringValue("clds.service.cache.invalidate.after.seconds");
            if (configuredMaxAge == null) {
                logger.warn(
                        "clds.service.cache.invalidate.after.seconds NOT set in clds-reference.properties file, taking 60s as default");
                configuredMaxAge = "60";
            }
            return (!cachedServiceUuid.equalsIgnoreCase(latestServiceUuid)) || (cldsServiceData.getAgeOfRecord() != null
                    && cldsServiceData.getAgeOfRecord() > Long.parseLong(configuredMaxAge));
        } else {
            return true;
        }
    }

    /**
     * Get the Service Data with Alarm Conditions for a given
     * invariantServiceUuid.
     * 
     * @param invariantServiceUuid
     * @return The CldsServiceData
     * @throws GeneralSecurityException
     *             In case of issues with the decryting the encrypted password
     * @throws DecoderException
     *             In case of issues with the decoding of the Hex String
     */
    public CldsServiceData getCldsServiceDataWithAlarmConditions(String invariantServiceUuid)
            throws GeneralSecurityException, DecoderException {
        String url = refProp.getStringValue(SDC_SERVICE_URL_PROPERTY_NAME);
        String catalogUrl = refProp.getStringValue(SDC_CATALOG_URL_PROPERTY_NAME);
        String serviceUuid = getServiceUuidFromServiceInvariantId(invariantServiceUuid);
        String serviceDetailUrl = url + "/" + serviceUuid + SDC_METADATA_URL_PREFIX;
        String responseStr = getCldsServicesOrResourcesBasedOnURL(serviceDetailUrl);
        CldsServiceData cldsServiceData = new CldsServiceData();
        if (responseStr != null) {
            SdcServiceDetail cldsSdcServiceDetail;
            try {
                cldsSdcServiceDetail = JacksonUtils.getObjectMapperInstance().readValue(responseStr,
                        SdcServiceDetail.class);
            } catch (IOException e) {
                logger.error("Exception when decoding the CldsServiceData JSON from SDC", e);
                throw new SdcCommunicationException("Exception when decoding the CldsServiceData JSON from SDC", e);
            }
            // To remove duplicate resources from serviceDetail and add valid
            // vfs to service
            if (cldsSdcServiceDetail != null && cldsSdcServiceDetail.getResources() != null) {
                cldsServiceData.setServiceUUID(cldsSdcServiceDetail.getUuid());
                cldsServiceData.setServiceInvariantUUID(cldsSdcServiceDetail.getInvariantUUID());
                List<SdcResource> cldsSdcResourceList = removeDuplicateSdcResourceInstances(
                        cldsSdcServiceDetail.getResources());
                if (cldsSdcResourceList != null && !cldsSdcResourceList.isEmpty()) {
                    List<CldsVfData> cldsVfDataList = new ArrayList<>();
                    for (SdcResource currCldsSdcResource : cldsSdcResourceList) {
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
                    logger.info("Invariant Service ID of cldsServiceData:" + cldsServiceData.getServiceInvariantUUID());
                }
            }
        }
        return cldsServiceData;
    }

    private void getAllVfcForVfList(List<CldsVfData> cldsVfDataList, String catalogUrl)
            throws GeneralSecurityException {
        // todo : refact this..
        if (cldsVfDataList != null && !cldsVfDataList.isEmpty()) {
            List<SdcResourceBasicInfo> allVfResources = getAllSdcVForVfcResourcesBasedOnResourceType(RESOURCE_VF_TYPE);
            List<SdcResourceBasicInfo> allVfcResources = getAllSdcVForVfcResourcesBasedOnResourceType(
                    RESOURCE_VFC_TYPE);
            allVfcResources.addAll(getAllSdcVForVfcResourcesBasedOnResourceType(RESOURCE_CVFC_TYPE));
            for (CldsVfData currCldsVfData : cldsVfDataList) {
                if (currCldsVfData != null && currCldsVfData.getVfInvariantResourceUUID() != null) {
                    String resourceUuid = getResourceUuidFromResourceInvariantUuid(
                            currCldsVfData.getVfInvariantResourceUUID(), allVfResources);
                    if (resourceUuid != null) {
                        String vfResourceUuidUrl = catalogUrl + RESOURCE_URL_PREFIX + "/" + resourceUuid
                                + SDC_METADATA_URL_PREFIX;
                        String vfResponse = getCldsServicesOrResourcesBasedOnURL(vfResourceUuidUrl);
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
                                                String vfcResourceUuidUrl = catalogUrl + RESOURCE_URL_PREFIX + "/"
                                                        + resourceVfcUuid + SDC_METADATA_URL_PREFIX;
                                                String vfcResponse = getCldsServicesOrResourcesBasedOnURL(
                                                        vfcResourceUuidUrl);
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
        ObjectNode vfResponseNode;
        try {
            vfResponseNode = (ObjectNode) JacksonUtils.getObjectMapperInstance().readTree(vfResponse);
        } catch (IOException e) {
            logger.error("Exception when decoding the JSON list of CldsVfcData", e);
            return new ArrayList<>();
        }
        ArrayNode vfcArrayNode = (ArrayNode) vfResponseNode.get("resources");
        List<CldsVfcData> cldsVfcDataList = new ArrayList<>();
        if (vfcArrayNode != null) {
            for (JsonNode vfcjsonNode : vfcArrayNode) {
                ObjectNode currVfcNode = (ObjectNode) vfcjsonNode;
                TextNode resourceTypeNode = (TextNode) currVfcNode.get("resoucreType");
                if (resourceTypeNode != null && "VFC".equalsIgnoreCase(resourceTypeNode.textValue())) {
                    handleVFCtypeNode(currVfcNode, cldsVfcDataList);
                } else if (resourceTypeNode != null && "CVFC".equalsIgnoreCase(resourceTypeNode.textValue())) {
                    handleCVFCtypeNode(currVfcNode, cldsVfcDataList);
                }
            }
        }
        return cldsVfcDataList;
    }

    private void handleVFCtypeNode(ObjectNode currVfcNode, List<CldsVfcData> cldsVfcDataList) {
        CldsVfcData currCldsVfcData = new CldsVfcData();
        TextNode vfcResourceName = (TextNode) currVfcNode.get("resourceInstanceName");
        TextNode vfcInvariantResourceUuid = (TextNode) currVfcNode.get("resourceInvariantUUID");
        currCldsVfcData.setVfcName(vfcResourceName.textValue());
        currCldsVfcData.setVfcInvariantResourceUUID(vfcInvariantResourceUuid.textValue());
        cldsVfcDataList.add(currCldsVfcData);
    }

    private void handleCVFCtypeNode(ObjectNode currVfcNode, List<CldsVfcData> cldsVfcDataList) {
        handleVFCtypeNode(currVfcNode, cldsVfcDataList);
        cldsVfcDataList.addAll(getVFCfromCVFC(currVfcNode.get("resourceUUID").textValue()));
    }

    private List<CldsVfcData> getVFCfromCVFC(String resourceUUID) {
        String catalogUrl = refProp.getStringValue(SDC_CATALOG_URL_PROPERTY_NAME);
        List<CldsVfcData> cldsVfcDataList = new ArrayList<>();
        if (resourceUUID != null) {
            String vfcResourceUUIDUrl = catalogUrl + RESOURCE_URL_PREFIX + "/" + resourceUUID + SDC_METADATA_URL_PREFIX;
            try {
                String vfcResponse = getCldsServicesOrResourcesBasedOnURL(vfcResourceUUIDUrl);
                ObjectNode vfResponseNode = (ObjectNode) JacksonUtils.getObjectMapperInstance().readTree(vfcResponse);
                ArrayNode vfcArrayNode = (ArrayNode) vfResponseNode.get("resources");
                if (vfcArrayNode != null) {
                    for (JsonNode vfcjsonNode : vfcArrayNode) {
                        ObjectNode currVfcNode = (ObjectNode) vfcjsonNode;
                        TextNode resourceTypeNode = (TextNode) currVfcNode.get("resoucreType");
                        if (resourceTypeNode != null && "VFC".equalsIgnoreCase(resourceTypeNode.textValue())) {
                            handleVFCtypeNode(currVfcNode, cldsVfcDataList);
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

    private List<CldsAlarmCondition> getAlarmCondtionsFromVfc(String vfcResponse) throws GeneralSecurityException {
        List<CldsAlarmCondition> cldsAlarmConditionList = new ArrayList<>();
        ObjectNode vfcResponseNode;
        try {
            vfcResponseNode = (ObjectNode) JacksonUtils.getObjectMapperInstance().readTree(vfcResponse);
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
    private List<CldsVfKPIData> getFieldPathFromVF(String vfResponse) throws GeneralSecurityException {
        List<CldsVfKPIData> cldsVfKPIDataList = new ArrayList<>();
        ObjectNode vfResponseNode;
        try {
            vfResponseNode = (ObjectNode) JacksonUtils.getObjectMapperInstance().readTree(vfResponse);
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
                if (artifactUrlNode != null && "csv".equalsIgnoreCase(artifactName)) {
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

    // Get the responses for the current artifact from the artifacts URL.
    private String getResponsesFromArtifactUrl(String artifactsUrl) {
        String hostUrl = refProp.getStringValue("sdc.hostUrl");
        String artifactsUrlReworked = artifactsUrl.replaceAll("\"", "");
        String artifactUrl = hostUrl + artifactsUrlReworked;
        logger.info("value of artifactURl:" + artifactUrl);
        String currArtifactResponse = getCldsServicesOrResourcesBasedOnURL(artifactUrl);
        logger.info("value of artifactResponse:" + currArtifactResponse);
        return currArtifactResponse;
    }

    /**
     * Service to services/resources/artifacts from sdc.Pass alarmConditions as
     * true to get alarm conditons from artifact url and else it is false
     * 
     * @param url
     *            The URL to trigger
     * @return The String containing the payload
     */
    public String getCldsServicesOrResourcesBasedOnURL(String url) {
        Date startTime = new Date();
        try {
            LoggingUtils.setTargetContext("SDC", "getCldsServicesOrResourcesBasedOnURL");
            String urlReworked = removeUnwantedBracesFromString(url);
            URL urlObj = new URL(urlReworked);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn = utils.invoke(conn,"SDC", "getSdcResources");
            String basicAuth = getSdcBasicAuth();
            conn.setRequestProperty(refProp.getStringValue(SDC_INSTANCE_ID_PROPERTY_NAME), SDC_INSTANCE_ID_CLAMP);
            conn.setRequestProperty(HttpHeaders.AUTHORIZATION, basicAuth);
            conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
            conn.setRequestProperty(refProp.getStringValue(SDC_REQUESTID_PROPERTY_NAME), LoggingUtils.getRequestId());
            conn.setRequestMethod("GET");
            int responseCode = conn.getResponseCode();
            logger.info("Sdc resource url - " + urlReworked + " , responseCode=" + responseCode);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = IOUtils.toString(in);
                return response;
            }
        } catch (IOException e) {
            LoggingUtils.setErrorContext("900", "Get sdc resources error");
            logger.error("Exception occurred during query to SDC", e);
            return "";
        } catch (DecoderException e) {
            LoggingUtils.setErrorContext("900", "Get sdc resources error");
            logger.error("Exception when attempting to decode the Hex string", e);
            throw new SdcCommunicationException("Exception when attempting to decode the Hex string", e);
        } catch (GeneralSecurityException e) {
            LoggingUtils.setErrorContext("900", "Get sdc resources error");
            logger.error("Exception when attempting to decrypt the encrypted password", e);
            throw new SdcCommunicationException("Exception when attempting to decrypt the encrypted password", e);
        } finally {
            LoggingUtils.setTimeContext(startTime, new Date());
            metricsLogger.info("getCldsServicesOrResourcesBasedOnURL completed");
            utils.invokeReturn();
        }
    }

    /**
     * To create properties object by using cldsServicedata.
     *
     * @param globalProps
     * @param cldsServiceData
     * @return
     * @throws IOException
     *             In case of issues during the parsing of the Global Properties
     */
    public String createPropertiesObjectByUUID(CldsServiceData cldsServiceData) throws IOException {
        String totalPropsStr;
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
        ObjectNode globalPropsJson = (ObjectNode) refProp.getJsonTemplate(CldsService.GLOBAL_PROPERTIES_KEY);
        if (cldsServiceData != null && cldsServiceData.getServiceUUID() != null) {
            // Objectnode to save all byservice, byvf , byvfc and byalarm nodes
            ObjectNode byIdObjectNode = mapper.createObjectNode();
            // To create vf ResourceUUID node with serviceInvariantUUID
            ObjectNode invariantUuidObjectNodeWithVf = createVfObjectNodeByServiceInvariantUuid(cldsServiceData);
            byIdObjectNode.putPOJO("byService", invariantUuidObjectNodeWithVf);
            // To create byVf and vfcResourceNode with vfResourceUUID
            ObjectNode vfcObjectNodeByVfUuid = createVfcObjectNodeByVfUuid(cldsServiceData.getCldsVfs());
            byIdObjectNode.putPOJO("byVf", vfcObjectNodeByVfUuid);
            // To create byKpi
            ObjectNode kpiObjectNode = mapper.createObjectNode();
            if (cldsServiceData.getCldsVfs() != null && !cldsServiceData.getCldsVfs().isEmpty()) {
                for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                    if (currCldsVfData != null) {
                        createKpiObjectNodeByVfUuid(kpiObjectNode, currCldsVfData.getCldsKPIList());
                    }
                }
            }
            byIdObjectNode.putPOJO("byKpi", kpiObjectNode);
            // To create byVfc and alarmCondition with vfcResourceUUID
            ObjectNode vfcResourceUuidObjectNode = mapper.createObjectNode();
            if (cldsServiceData.getCldsVfs() != null && !cldsServiceData.getCldsVfs().isEmpty()) {
                for (CldsVfData currCldsVfData : cldsServiceData.getCldsVfs()) {
                    if (currCldsVfData != null) {
                        createAlarmCondObjectNodeByVfcUuid(vfcResourceUuidObjectNode, currCldsVfData.getCldsVfcs());
                    }
                }
            }
            byIdObjectNode.putPOJO("byVfc", vfcResourceUuidObjectNode);
            // To create byAlarmCondition with alarmConditionKey
            List<CldsAlarmCondition> allAlarmConditions = getAllAlarmConditionsFromCldsServiceData(cldsServiceData,
                    "alarmCondition");
            ObjectNode alarmCondObjectNodeByAlarmKey = createAlarmCondObjectNodeByAlarmKey(allAlarmConditions);
            byIdObjectNode.putPOJO("byAlarmCondition", alarmCondObjectNodeByAlarmKey);
            // To create byAlertDescription with AlertDescription
            List<CldsAlarmCondition> allAlertDescriptions = getAllAlarmConditionsFromCldsServiceData(cldsServiceData,
                    "alertDescription");
            ObjectNode alertDescObjectNodeByAlert = createAlarmCondObjectNodeByAlarmKey(allAlertDescriptions);
            byIdObjectNode.putPOJO("byAlertDescription", alertDescObjectNodeByAlert);
            globalPropsJson.putPOJO("shared", byIdObjectNode);
            logger.info("Global properties JSON created with SDC info:" + globalPropsJson);
        }
        totalPropsStr = globalPropsJson.toString();
        return totalPropsStr;
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

    private ObjectNode createAlarmCondObjectNodeByAlarmKey(List<CldsAlarmCondition> cldsAlarmCondList) {
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
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

    private ObjectNode createVfObjectNodeByServiceInvariantUuid(CldsServiceData cldsServiceData) {
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
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

    private void createKpiObjectNodeByVfUuid(ObjectNode vfResourceUuidObjectNode,
            List<CldsVfKPIData> cldsVfKpiDataList) {
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
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

    private void createAlarmCondObjectNodeByVfcUuid(ObjectNode vfcResourceUuidObjectNode,
            List<CldsVfcData> cldsVfcDataList) {
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
        ObjectNode vfcObjectNode = mapper.createObjectNode();
        ObjectNode alarmCondNode = mapper.createObjectNode();
        ObjectNode alertDescNode = mapper.createObjectNode();
        if (cldsVfcDataList != null && !cldsVfcDataList.isEmpty()) {
            for (CldsVfcData currCldsVfcData : cldsVfcDataList) {
                if (currCldsVfcData != null) {
                    if (currCldsVfcData.getCldsAlarmConditions() != null
                            && !currCldsVfcData.getCldsAlarmConditions().isEmpty()) {
                        for (CldsAlarmCondition currCldsAlarmCondition : currCldsVfcData.getCldsAlarmConditions()) {
                            if ("alarmCondition".equalsIgnoreCase(currCldsAlarmCondition.getEventName())) {
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

    /**
     * Method to create vfc and kpi nodes inside vf node
     * 
     * @param mapper
     * @param cldsVfDataList
     * @return
     */
    private ObjectNode createVfcObjectNodeByVfUuid(List<CldsVfData> cldsVfDataList) {
        ObjectMapper mapper = JacksonUtils.getObjectMapperInstance();
        ObjectNode vfUuidObjectNode = mapper.createObjectNode();
        if (cldsVfDataList != null && !cldsVfDataList.isEmpty()) {
            for (CldsVfData currCldsVfData : cldsVfDataList) {
                if (currCldsVfData != null) {
                    ObjectNode vfObjectNode = mapper.createObjectNode();
                    ObjectNode vfcUuidNode = mapper.createObjectNode();
                    ObjectNode kpiObjectNode = mapper.createObjectNode();
                    if (currCldsVfData.getCldsVfcs() != null && !currCldsVfData.getCldsVfcs().isEmpty()) {
                        for (CldsVfcData currCldsVfcData : currCldsVfData.getCldsVfcs()) {
                            if (currCldsVfcData.getCldsAlarmConditions() != null
                                    && !currCldsVfcData.getCldsAlarmConditions().isEmpty()) {
                                vfcUuidNode.put(currCldsVfcData.getVfcInvariantResourceUUID(),
                                        currCldsVfcData.getVfcName());
                            }
                        }
                    } else {
                        vfcUuidNode.put("", "");
                    }
                    if (currCldsVfData.getCldsKPIList() != null && !currCldsVfData.getCldsKPIList().isEmpty()) {
                        for (CldsVfKPIData currCldsVfKPIData : currCldsVfData.getCldsKPIList()) {
                            kpiObjectNode.put(currCldsVfKPIData.getThresholdValue(),
                                    currCldsVfKPIData.getThresholdValue());
                        }
                    } else {
                        kpiObjectNode.put("", "");
                    }
                    vfObjectNode.putPOJO("vfc", vfcUuidNode);
                    vfObjectNode.putPOJO("kpi", kpiObjectNode);
                    vfUuidObjectNode.putPOJO(currCldsVfData.getVfInvariantResourceUUID(), vfObjectNode);
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
    public String getArtifactIdIfArtifactAlreadyExists(SdcServiceDetail cldsSdcServiceDetail, String artifactName) {
        String artifactUuid = null;
        boolean artifactExists = false;
        if (cldsSdcServiceDetail != null && cldsSdcServiceDetail.getResources() != null
                && !cldsSdcServiceDetail.getResources().isEmpty()) {
            for (SdcResource currCldsSdcResource : cldsSdcServiceDetail.getResources()) {
                if (artifactExists) {
                    break;
                }
                if (currCldsSdcResource != null && currCldsSdcResource.getArtifacts() != null
                        && !currCldsSdcResource.getArtifacts().isEmpty()) {
                    for (SdcArtifact currCldsSdcArtifact : currCldsSdcResource.getArtifacts()) {
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

    // To get all sdc VF/VFC Resources basic info.
    private List<SdcResourceBasicInfo> getAllSdcVForVfcResourcesBasedOnResourceType(String resourceType) {
        String catalogUrl = refProp.getStringValue(SDC_CATALOG_URL_PROPERTY_NAME);
        String resourceUrl = catalogUrl + "resources?resourceType=" + resourceType;
        String allSdcVfcResources = getCldsServicesOrResourcesBasedOnURL(resourceUrl);
        return removeDuplicateSdcResourceBasicInfo(getAllSdcResourcesListFromJson(allSdcVfcResources));
    }

    private String getResourceUuidFromResourceInvariantUuid(String resourceInvariantUuid,
            List<SdcResourceBasicInfo> resourceInfoList) {
        String resourceUuid = null;
        if (resourceInfoList != null && !resourceInfoList.isEmpty()) {
            for (SdcResourceBasicInfo currResource : resourceInfoList) {
                if (currResource != null && currResource.getInvariantUUID() != null && currResource.getUuid() != null
                        && currResource.getInvariantUUID().equalsIgnoreCase(resourceInvariantUuid)) {
                    resourceUuid = currResource.getUuid();
                    break;
                }
            }
        }
        return resourceUuid;
    }

    // Method to get service invariant uuid from model properties.
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
     * @throws GeneralSecurityException
     *             In case of issues with the decryting the encrypted password
     * @throws DecoderException
     *             In case of issues with the decoding of the Hex String
     */
    public void uploadToSdc(ModelProperties prop, String userid, List<String> sdcReqUrlsList, String formattedSdcReq,
            String formattedSdcLocationReq, String artifactName, String locationArtifactName)
            throws GeneralSecurityException, DecoderException {
        logger.info("userid=" + userid);
        String serviceInvariantUuid = getServiceInvariantUuidFromProps(prop);
        if (sdcReqUrlsList != null && !sdcReqUrlsList.isEmpty()) {
            for (String url : sdcReqUrlsList) {
                if (url != null) {
                    String originalServiceUuid = getServiceUuidFromServiceInvariantId(serviceInvariantUuid);
                    logger.info("ServiceUUID used before upload in url:" + originalServiceUuid);
                    String sdcServicesInformation = getSdcServicesInformation(originalServiceUuid);
                    SdcServiceDetail cldsSdcServiceDetail = decodeCldsSdcServiceDetailFromJson(sdcServicesInformation);
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
                    cldsSdcServiceDetail = decodeCldsSdcServiceDetailFromJson(sdcServicesInformation);
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

    /**
     * Method to delete blueprint and location json artifacts from sdc
     * 
     * @param prop
     * @param userid
     * @param sdcReqUrlsList
     * @param artifactName
     * @param locationArtifactName
     * @throws GeneralSecurityException
     * @throws DecoderException
     */
    public void deleteArtifactsFromSdc(ModelProperties prop, String userid, List<String> sdcReqUrlsList,
            String artifactName, String locationArtifactName) throws GeneralSecurityException, DecoderException {
        String serviceInvariantUuid = getServiceInvariantUuidFromProps(prop);
        for (String url : sdcReqUrlsList) {
            String originalServiceUuid = getServiceUuidFromServiceInvariantId(serviceInvariantUuid);
            logger.info("ServiceUUID used before deleting in url:" + originalServiceUuid);
            String sdcServicesInformation = getSdcServicesInformation(originalServiceUuid);
            SdcServiceDetail cldsSdcServiceDetail = decodeCldsSdcServiceDetailFromJson(sdcServicesInformation);
            String uploadedArtifactUuid = getArtifactIdIfArtifactAlreadyExists(cldsSdcServiceDetail, artifactName);
            String responseStr = deleteArtifact(userid, url, uploadedArtifactUuid);
            logger.info("value of sdc Response of deleting blueprint from sdc :" + responseStr);
            String updatedServiceUuid = getServiceUuidFromServiceInvariantId(serviceInvariantUuid);
            if (!originalServiceUuid.equalsIgnoreCase(updatedServiceUuid)) {
                url = url.replace(originalServiceUuid, updatedServiceUuid);
            }
            logger.info("ServiceUUID used after delete in ulr:" + updatedServiceUuid);
            sdcServicesInformation = getSdcServicesInformation(updatedServiceUuid);
            cldsSdcServiceDetail = decodeCldsSdcServiceDetailFromJson(sdcServicesInformation);
            uploadedArtifactUuid = getArtifactIdIfArtifactAlreadyExists(cldsSdcServiceDetail, locationArtifactName);
            responseStr = deleteArtifact(userid, url, uploadedArtifactUuid);
            logger.info("value of sdc Response of deleting location json from sdc :" + responseStr);
        }
    }

    private String deleteArtifact(String userid, String url, String uploadedArtifactUuid) {
        try {
            String responseStr = "";
            if (uploadedArtifactUuid != null && !uploadedArtifactUuid.isEmpty()) {
                logger.info("userid=" + userid);
                String basicAuth = getSdcBasicAuth();
                String sdcXonapInstanceId = refProp.getStringValue("sdc.sdcX-InstanceID");
                url = url + "/" + uploadedArtifactUuid;
                URL urlObj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn = utils.invoke(conn,"SDC", "deleteArtifact");
                conn.setDoOutput(true);
                conn.setRequestProperty(refProp.getStringValue(SDC_INSTANCE_ID_PROPERTY_NAME), sdcXonapInstanceId);
                conn.setRequestProperty(HttpHeaders.AUTHORIZATION, basicAuth);
                conn.setRequestProperty("USER_ID", userid);
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("charset", "utf-8");
                conn.setUseCaches(false);
                conn.setRequestProperty(refProp.getStringValue(SDC_REQUESTID_PROPERTY_NAME),
                        LoggingUtils.getRequestId());
                boolean requestFailed = true;
                int responseCode = conn.getResponseCode();
                logger.info("responseCode=" + responseCode);
                if (responseCode == 200) {
                    requestFailed = false;
                }
                responseStr = getResponse(conn);
                if (responseStr != null && requestFailed) {
                    logger.error("requestFailed - responseStr=" + responseStr);
                    utils.invokeReturn();
                    throw new BadRequestException(responseStr);
                }
            }
            utils.invokeReturn();
            return responseStr;
        } catch (IOException | DecoderException | GeneralSecurityException e) {
            logger.error("Exception when attempting to communicate with SDC", e);
            utils.invokeReturn();
            throw new SdcCommunicationException("Exception when attempting to communicate with SDC", e);
        }
    }
}
