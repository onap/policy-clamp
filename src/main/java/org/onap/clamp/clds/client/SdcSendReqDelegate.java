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

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.commons.codec.DecoderException;
import org.onap.clamp.clds.client.req.sdc.SdcCatalogServices;
import org.onap.clamp.clds.client.req.sdc.SdcRequests;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.DcaeEvent;
import org.onap.clamp.clds.model.properties.Global;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Send control loop model to dcae proxy.
 */
@Component
public class SdcSendReqDelegate {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(SdcSendReqDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    @Autowired
    private SdcCatalogServices sdcCatalogServices;
    @Autowired
    private SdcRequests sdcReq;
    @Autowired
    private ClampProperties refProp;

    /**
     * Perform activity. Send to sdc proxy.
     *
     * @param camelExchange
     *            The camel object that contains all fields
     * @throws DecoderException
     *             In case of issues with password decryption
     * @throws GeneralSecurityException
     *             In case of issues with password decryption
     * @throws IOException
     *             In case of issues with file opening
     */
    @Handler
    public void execute(Exchange camelExchange) throws GeneralSecurityException, DecoderException, IOException {
        String controlName = (String) camelExchange.getProperty("controlName");
        String artifactLabel = sdcReq
                .normalizeResourceInstanceName(refProp.getStringValue("sdc.artifactLabel") + "-" + controlName);
        String locationArtifactLabel = sdcReq
                .normalizeResourceInstanceName(refProp.getStringValue("sdc.locationArtifactLabel") + "-" + controlName);
        String artifactType = refProp.getStringValue("sdc.artifactType");
        String locationArtifactType = refProp.getStringValue("sdc.locationArtifactType");
        String userid = (String) camelExchange.getProperty("userid");
        String docText = (String) camelExchange.getProperty("docText");
        String artifactName = (String) camelExchange.getProperty("controlName") + DcaeEvent.ARTIFACT_NAME_SUFFIX;
        camelExchange.setProperty("artifactName", artifactName);
        ModelProperties prop = ModelProperties.create(camelExchange);
        String bluprintPayload;
        bluprintPayload = sdcReq.formatBlueprint(prop, docText);
        // no need to upload blueprint for Holmes, thus blueprintPayload for
        // Holmes is empty
        if (!bluprintPayload.isEmpty()) {
            String formattedSdcReq = sdcReq.formatSdcReq(bluprintPayload, artifactName, artifactLabel, artifactType);
            if (formattedSdcReq != null) {
                camelExchange.setProperty("formattedArtifactReq", formattedSdcReq.getBytes());
            }
            Global globalProps = prop.getGlobal();
            if (globalProps != null && globalProps.getService() != null) {
                String serviceInvariantUUID = globalProps.getService();
                camelExchange.setProperty("serviceInvariantUUID", serviceInvariantUUID);
            }
            List<String> sdcReqUrlsList = sdcReq.getSdcReqUrlsList(prop);
            String sdcLocationsPayload = sdcReq.formatSdcLocationsReq(prop, artifactName);
            String locationArtifactName = (String) camelExchange.getProperty("controlName") + "-location.json";
            String formattedSdcLocationReq = sdcReq.formatSdcReq(sdcLocationsPayload, locationArtifactName,
                    locationArtifactLabel, locationArtifactType);
            if (formattedSdcLocationReq != null) {
                camelExchange.setProperty("formattedLocationReq", formattedSdcLocationReq.getBytes());
            }
            sdcCatalogServices.uploadToSdc(prop, userid, sdcReqUrlsList, formattedSdcReq, formattedSdcLocationReq,
                    artifactName, locationArtifactName);
        }
    }
}
