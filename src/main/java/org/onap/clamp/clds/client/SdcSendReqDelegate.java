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

import java.util.List;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.onap.clamp.clds.client.req.SdcReq;
import org.onap.clamp.clds.model.CldsSdcServiceDetail;
import org.onap.clamp.clds.model.DcaeEvent;
import org.onap.clamp.clds.model.prop.Global;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.springframework.beans.factory.annotation.Autowired;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * Send control loop model to dcae proxy.
 */
public class SdcSendReqDelegate implements JavaDelegate {
    protected static final EELFLogger logger        = EELFManager.getInstance().getLogger(SdcSendReqDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Autowired
    private RefProp                 refProp;

    @Autowired
    private SdcCatalogServices      sdcCatalogServices;

    private String                  baseUrl;
    private String                  artifactType;
    private String                  locationArtifactType;
    private String                  artifactLabel;
    private String                  locationArtifactLabel;

    /**
     * Perform activity. Send to sdc proxy.
     *
     * @param execution
     */
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String userid = (String) execution.getVariable("userid");
        logger.info("userid=" + userid);
        String docText = new String((byte[]) execution.getVariable("docText"));
        String artifactName = (String) execution.getVariable("controlName") + DcaeEvent.ARTIFACT_NAME_SUFFIX;
        execution.setVariable("artifactName", artifactName);
        getSdcAttributes((String) execution.getVariable("controlName"));
        ModelProperties prop = ModelProperties.create(execution);
        String bluprintPayload = SdcReq.formatBlueprint(refProp, prop, docText);
        String formatttedSdcReq = SdcReq.formatSdcReq(bluprintPayload, artifactName, artifactLabel, artifactType);
        if (formatttedSdcReq != null) {
            execution.setVariable("formattedArtifactReq", formatttedSdcReq.getBytes());
        }
        List<String> sdcReqUrlsList = SdcReq.getSdcReqUrlsList(prop, baseUrl, sdcCatalogServices, execution);

        String sdcLocationsPayload = SdcReq.formatSdcLocationsReq(prop, artifactName);
        String locationArtifactName = (String) execution.getVariable("controlName") + "-location.json";
        String formattedSdcLocationReq = SdcReq.formatSdcReq(sdcLocationsPayload, locationArtifactName,
                locationArtifactLabel, locationArtifactType);
        if (formattedSdcLocationReq != null) {
            execution.setVariable("formattedLocationReq", formattedSdcLocationReq.getBytes());
        }
        String serviceInvariantUUID = getServiceInvariantUUIDFromProps(prop);
        uploadToSdc(prop, serviceInvariantUUID, userid, sdcReqUrlsList, formatttedSdcReq, formattedSdcLocationReq,
                artifactName, locationArtifactName);
    }

    private String getServiceInvariantUUIDFromProps(ModelProperties props) {
        String invariantUUID = "";
        Global globalProps = props.getGlobal();
        if (globalProps != null) {
            if (globalProps.getService() != null) {
                invariantUUID = globalProps.getService();
            }
        }
        return invariantUUID;
    }

    private void uploadToSdc(ModelProperties prop, String serviceInvariantUUID, String userid,
            List<String> sdcReqUrlsList, String formatttedSdcReq, String formattedSdcLocationReq, String artifactName,
            String locationArtifactName) throws Exception {
        logger.info("userid=" + userid);
        if (sdcReqUrlsList != null && sdcReqUrlsList.size() > 0) {
            for (String url : sdcReqUrlsList) {
                if (url != null) {
                    String originalServiceUUID = sdcCatalogServices
                            .getServiceUuidFromServiceInvariantId(serviceInvariantUUID);
                    logger.info("ServiceUUID used before upload in url:" + originalServiceUUID);
                    String sdcServicesInformation = sdcCatalogServices.getSdcServicesInformation(originalServiceUUID);
                    CldsSdcServiceDetail CldsSdcServiceDetail = sdcCatalogServices
                            .getCldsSdcServiceDetailFromJson(sdcServicesInformation);
                    String uploadedArtifactUUID = sdcCatalogServices
                            .getArtifactIdIfArtifactAlreadyExists(CldsSdcServiceDetail, artifactName);
                    // Upload artifacts to sdc
                    String updateUrl = uploadedArtifactUUID != null ? url + "/" + uploadedArtifactUUID : url;
                    String responseStr = sdcCatalogServices.uploadArtifactToSdc(prop, userid, updateUrl,
                            formatttedSdcReq);
                    logger.info("value of sdc Response of uploading to sdc :" + responseStr);
                    String updatedServiceUUID = sdcCatalogServices
                            .getServiceUuidFromServiceInvariantId(serviceInvariantUUID);
                    if (!originalServiceUUID.equalsIgnoreCase(updatedServiceUUID)) {
                        url = url.replace(originalServiceUUID, updatedServiceUUID);
                    }
                    logger.info("ServiceUUID used after upload in ulr:" + updatedServiceUUID);
                    sdcServicesInformation = sdcCatalogServices.getSdcServicesInformation(updatedServiceUUID);
                    CldsSdcServiceDetail = sdcCatalogServices.getCldsSdcServiceDetailFromJson(sdcServicesInformation);
                    uploadedArtifactUUID = sdcCatalogServices.getArtifactIdIfArtifactAlreadyExists(CldsSdcServiceDetail,
                            locationArtifactName);
                    // To send location information also to sdc
                    updateUrl = uploadedArtifactUUID != null ? url + "/" + uploadedArtifactUUID : url;
                    responseStr = sdcCatalogServices.uploadArtifactToSdc(prop, userid, updateUrl,
                            formattedSdcLocationReq);
                    logger.info("value of sdc Response of uploading location to sdc :" + responseStr);
                }
            }
        }
    }

    private void getSdcAttributes(String controlName) {
        baseUrl = refProp.getStringValue("sdc.serviceUrl");
        artifactLabel = SdcReq
                .normalizeResourceInstanceName(refProp.getStringValue("sdc.artifactLabel") + "-" + controlName);
        locationArtifactLabel = SdcReq
                .normalizeResourceInstanceName(refProp.getStringValue("sdc.locationArtifactLabel") + "-" + controlName);
        artifactType = refProp.getStringValue("sdc.artifactType");
        locationArtifactType = refProp.getStringValue("sdc.locationArtifactType");
    }
}
