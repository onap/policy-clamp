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

import org.onap.clamp.clds.client.req.SdcReq;
import org.onap.clamp.clds.model.CldsAsdcServiceDetail;
import org.onap.clamp.clds.model.DcaeEvent;
import org.onap.clamp.clds.model.prop.Global;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Send control loop model to dcae proxy.
 */
public class SdcSendReqDelegate implements JavaDelegate {
    // currently uses the java.util.logging.Logger like the Camunda engine
    private static final Logger logger = LoggerFactory.getLogger(SdcSendReqDelegate.class);

    @Autowired
    private RefProp refProp;

    @Autowired
    private SdcCatalogServices asdcCatalogServices;

    private String baseUrl;
    private String artifactType;
    private String locationArtifactType;
    private String artifactLabel;
    private String locationArtifactLabel;

    /**
     * Perform activity.  Send to asdc proxy.
     *
     * @param execution
     */
    public void execute(DelegateExecution execution) throws Exception {
        String userid = (String) execution.getVariable("userid");
        logger.info("userid=" + userid);
        String docText = (String) execution.getVariable("docText");
        String artifactName = execution.getVariable("controlName") + DcaeEvent.ARTIFACT_NAME_SUFFIX;
        execution.setVariable("artifactName", artifactName);
        getAsdcAttributes();
        ModelProperties prop = ModelProperties.create(execution);
        String bluprintPayload = SdcReq.formatBlueprint(refProp, prop, docText);
        String formatttedAsdcReq = SdcReq.formatAsdcReq(bluprintPayload, artifactName, artifactLabel, artifactType);
        if (formatttedAsdcReq != null) {
            execution.setVariable("formattedArtifactReq", formatttedAsdcReq.getBytes());
        }
        List<String> asdcReqUrlsList = SdcReq.getAsdcReqUrlsList(prop, baseUrl, asdcCatalogServices, execution);

        String asdcLocationsPayload = SdcReq.formatAsdcLocationsReq(prop, artifactName);
        String locationArtifactName = execution.getVariable("controlName") + "-location.json";
        String formattedAsdcLocationReq = SdcReq.formatAsdcReq(asdcLocationsPayload, locationArtifactName, locationArtifactLabel, locationArtifactType);
        if (formattedAsdcLocationReq != null) {
            execution.setVariable("formattedLocationReq", formattedAsdcLocationReq.getBytes());
        }
        String serviceInvariantUUID = getServiceInvariantUUIDFromProps(prop);
        uploadToAsdc(prop, serviceInvariantUUID, userid, asdcReqUrlsList, formatttedAsdcReq, formattedAsdcLocationReq, artifactName, locationArtifactName);
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

    private void uploadToAsdc(ModelProperties prop, String serviceInvariantUUID, String userid, List<String> asdcReqUrlsList, String formatttedAsdcReq, String formattedAsdcLocationReq, String artifactName, String locationArtifactName) throws Exception {
        logger.info("userid=" + userid);
        if (asdcReqUrlsList != null && asdcReqUrlsList.size() > 0) {
            for (String url : asdcReqUrlsList) {
                if (url != null) {
                    String originalServiceUUID = asdcCatalogServices.getServiceUUIDFromServiceInvariantID(serviceInvariantUUID);
                    logger.info("ServiceUUID used before upload in url:" + originalServiceUUID);
                    String asdcServicesInformation = asdcCatalogServices.getAsdcServicesInformation(originalServiceUUID);
                    CldsAsdcServiceDetail cldsAsdcServiceDetail = asdcCatalogServices.getCldsAsdcServiceDetailFromJson(asdcServicesInformation);
                    String uploadedArtifactUUID = asdcCatalogServices.getArtifactIdIfArtifactAlreadyExists(cldsAsdcServiceDetail, artifactName);
                    // Upload artifacts to asdc
                    String updateUrl = uploadedArtifactUUID != null ? url + "/" + uploadedArtifactUUID : url;
                    String responseStr = asdcCatalogServices.uploadArtifactToAsdc(prop, userid, updateUrl, formatttedAsdcReq);
                    logger.info("value of asdc Response of uploading to asdc :" + responseStr);
                    String updatedServiceUUID = asdcCatalogServices.getServiceUUIDFromServiceInvariantID(serviceInvariantUUID);
                    if (!originalServiceUUID.equalsIgnoreCase(updatedServiceUUID)) {
                        url = url.replace(originalServiceUUID, updatedServiceUUID);
                    }
                    logger.info("ServiceUUID used after upload in ulr:" + updatedServiceUUID);
                    asdcServicesInformation = asdcCatalogServices.getAsdcServicesInformation(updatedServiceUUID);
                    cldsAsdcServiceDetail = asdcCatalogServices.getCldsAsdcServiceDetailFromJson(asdcServicesInformation);
                    uploadedArtifactUUID = asdcCatalogServices.getArtifactIdIfArtifactAlreadyExists(cldsAsdcServiceDetail, locationArtifactName);
                    //  To send location information also to asdc
                    updateUrl = uploadedArtifactUUID != null ? url + "/" + uploadedArtifactUUID : url;
                    responseStr = asdcCatalogServices.uploadArtifactToAsdc(prop, userid, updateUrl, formattedAsdcLocationReq);
                    logger.info("value of asdc Response of uploading location to asdc :" + responseStr);
                }
            }
        }
    }

    private void getAsdcAttributes() {
        baseUrl = refProp.getStringValue("asdc.serviceUrl");
        artifactLabel = refProp.getStringValue("asdc.artifactLabel");
        locationArtifactLabel = refProp.getStringValue("asdc.locationArtifactLabel");
        artifactType = refProp.getStringValue("asdc.artifactType");
        locationArtifactType = refProp.getStringValue("asdc.locationArtifactType");
    }
}
