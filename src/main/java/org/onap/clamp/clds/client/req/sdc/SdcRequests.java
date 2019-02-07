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
 * 
 */

package org.onap.clamp.clds.client.req.sdc;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.codec.DecoderException;
import org.onap.clamp.clds.client.req.tca.TcaRequestFormatter;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.properties.Global;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Tca;
import org.onap.clamp.clds.model.sdc.SdcResource;
import org.onap.clamp.clds.model.sdc.SdcServiceDetail;
import org.onap.clamp.clds.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Construct a Sdc request given CLDS objects.
 */
@Component
@Primary
public class SdcRequests {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(SdcRequests.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    @Autowired
    private SdcCatalogServices sdcCatalogServices;
    @Autowired
    protected ClampProperties refProp;

    /**
     * Format the Blueprint from a Yaml
     *
     * @param prop
     *            The ModelProperties describing the clds model
     * @param docText
     *            The Yaml file that must be converted
     * @return A String containing the BluePrint
     * @throws IOException
     *             In case of issues
     */
    public String formatBlueprint(ModelProperties prop, String docText) throws IOException {
        String yamlvalue = getYamlvalue(docText);
        String updatedBlueprint = "";
        Tca tca = prop.getType(Tca.class);
        if (tca.isFound()) {
            updatedBlueprint = TcaRequestFormatter.updatedBlueprintWithConfiguration(refProp, prop, yamlvalue);
        }
        logger.info("value of blueprint:" + updatedBlueprint);
        return updatedBlueprint;
    }

    /**
     * Format the SDC Locations Request in the JSON Format
     *
     * @param prop
     *            The ModelProperties describing the clds model
     * @param artifactName
     *            The name of the artifact
     * @return SDC Locations request in the JSON Format
     */
    public String formatSdcLocationsReq(ModelProperties prop, String artifactName) {
        Global global = prop.getGlobal();
        List<String> locationsList = global.getLocation();
        JsonArray locationsArrayNode = new JsonArray();
        JsonObject locationObject = new JsonObject();
        for (String currLocation : locationsList) {
            locationsArrayNode.add(currLocation);
        }
        locationObject.addProperty("artifactName", artifactName);
        locationObject.add("locations", locationsArrayNode);
        String locationJsonFormat = locationObject.toString();
        logger.info("Value of location Json Artifact:" + locationsArrayNode);
        return locationJsonFormat;
    }

    /**
     * Format the SDC Request
     *
     * @param payloadData
     *            The ModelProperties describing the clds model
     * @param artifactName
     *            The name of the artifact
     * @param artifactLabel
     *            The Label of the artifact
     * @param artifactType
     *            The type of the artifact
     * @return formatted SDC Request
     */
    public String formatSdcReq(String payloadData, String artifactName, String artifactLabel, String artifactType) {
        logger.info("artifact=" + payloadData);
        String base64Artifact = Base64.getEncoder().encodeToString(payloadData.getBytes(StandardCharsets.UTF_8));
        return "{ \n" + "\"payloadData\" : \"" + base64Artifact + "\",\n" + "\"artifactLabel\" : \"" + artifactLabel
                + "\",\n" + "\"artifactName\" :\"" + artifactName + "\",\n" + "\"artifactType\" : \"" + artifactType
                + "\",\n" + "\"artifactGroupType\" : \"DEPLOYMENT\",\n" + "\"description\" : \"from CLAMP Cockpit\"\n"
                + "} \n";
    }

    private List<String> filterVfResourceList(String serviceUuid, List<SdcResource> sdcResourcesList,
            List<String> cldsResourceVfList) {
        List<String> urlList = new ArrayList<>();
        for (SdcResource cldsSdcResource : sdcResourcesList) {
            if (cldsSdcResource != null && cldsSdcResource.getResoucreType() != null
                    && cldsSdcResource.getResoucreType().equalsIgnoreCase("VF")
                    && cldsResourceVfList.contains(cldsSdcResource.getResourceInvariantUUID())) {
                String normalizedResourceInstanceName = normalizeResourceInstanceName(
                        cldsSdcResource.getResourceInstanceName());
                String svcUrl = createUrlForResource(normalizedResourceInstanceName, serviceUuid);
                urlList.add(svcUrl);
            }
        }
        return urlList;
    }

    private String createUrlForResource(String normalizedResourceInstanceName, String serviceUuid) {
        return refProp.getStringValue("sdc.serviceUrl") + "/" + serviceUuid + "/resourceInstances/"
                + normalizedResourceInstanceName + "/artifacts";
    }

    /**
     * To get List of urls for all vfresources
     *
     * @param prop
     *            The model properties JSON describing the closed loop flow
     * @return A list of Service URL
     * @throws GeneralSecurityException
     *             In case of issues when decrypting the password
     * @throws DecoderException
     *             In case of issues when decoding the Hex String
     */
    public List<String> getSdcReqUrlsList(ModelProperties prop) throws GeneralSecurityException, DecoderException {
        List<String> urlList = new ArrayList<>();
        Global globalProps = prop.getGlobal();
        if (globalProps != null && globalProps.getService() != null && globalProps.getResourceVf() != null) {
            String serviceUuid = sdcCatalogServices.getServiceUuidFromServiceInvariantId(globalProps.getService());
            SdcServiceDetail sdcServiceDetail = sdcCatalogServices
                    .decodeCldsSdcServiceDetailFromJson(sdcCatalogServices.getSdcServicesInformation(serviceUuid));
            if (sdcServiceDetail != null) {
                urlList = filterVfResourceList(serviceUuid, sdcServiceDetail.getResources(),
                        globalProps.getResourceVf());
            }
        } else {
            logger.warn("GlobalProperties json is empty, skipping getSdcReqUrlsList and returning empty list");
        }
        return urlList;
    }

    /**
     * "Normalize" the resource instance name: - Remove spaces, underscores,
     * dashes, and periods. - make lower case This is required by SDC when using
     * the resource instance name to upload an artifact.
     *
     * @param inText
     * @return
     */
    public String normalizeResourceInstanceName(String inText) {
        return inText.replace(" ", "").replace("-", "").replace(".", "").toLowerCase();
    }

    /**
     * Method to get yaml/template properties value from json.
     *
     * @param jsonGlobal
     *            The Json containing a Yaml file
     * @return The yaml extracted from the JSON
     * @throws IOException
     *             In case of issues with the Json parser
     */
    protected String getYamlvalue(String jsonGlobal) throws IOException {
        String yamlFileValue = "";
        JsonObject root = JsonUtils.GSON.fromJson(jsonGlobal, JsonObject.class);
        Iterator<Entry<String, JsonElement>> entryItr = root.entrySet().iterator();
        while (entryItr.hasNext()) {
            Entry<String, JsonElement> entry = entryItr.next();
            String key = entry.getKey();
            if (key != null && key.equalsIgnoreCase("global")) {
                JsonArray arrayNode = entry.getValue().getAsJsonArray();
                for (JsonElement anArrayNode : arrayNode) {
                    JsonObject node = anArrayNode.getAsJsonObject();
                    JsonArray arrayValueNode = node.get("value").getAsJsonArray();
                    JsonElement jsonNode = arrayValueNode.get(0);
                    yamlFileValue = jsonNode.getAsString();
                    logger.info("value:" + yamlFileValue);
                }
                break;
            }
        }
        return yamlFileValue;
    }
}
