/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 *  * Modifications Copyright (C) 2020 Huawei Technologies Co., Ltd.
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

package org.onap.clamp.loop.cds;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonObject;
import org.onap.clamp.clds.client.CdsServices;
import org.onap.clamp.clds.model.cds.CdsBpWorkFlowListResponse;
import org.onap.clamp.clds.sdc.controller.installer.CsarHandler;
import org.onap.clamp.loop.service.Service;
import org.onap.clamp.loop.service.ServicesRepository;
import org.onap.sdc.tosca.parser.enums.SdcTypes;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class installs the cds data in the service model properties.
 * This can be refreshed later on by clicking on the button refresh, when recomputing the json schema.
 */
@Component
public class CdsDataInstaller {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CdsDataInstaller.class);

    @Autowired
    CdsServices cdsServices;

    @Autowired
    ServicesRepository serviceRepository;

    public static final String CONTROLLER_PROPERTIES = "controllerProperties";
    public static final String SDNC_MODEL_NAME = "sdnc_model_name";
    public static final String SDNC_MODEL_VERSION = "sdnc_model_version";

    /**
     * This method installs the service model properties for CDS in the service object given in input.
     *
     * @param csar    The csar from sdc
     * @param service the service object already provisioned with csar data
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Service installCdsServiceProperties(CsarHandler csar, Service service) {
        // Iterate on all types defined in the tosca lib
        for (SdcTypes type : SdcTypes.values()) {
            JsonObject resourcesPropByType = service.getResourceByType(type.getValue());
            // For each type, get the metadata of each nodetemplate
            for (NodeTemplate nodeTemplate : csar.getSdcCsarHelper().getServiceNodeTemplateBySdcType(type)) {
                // get cds artifact information and save in resources Prop
                if (SdcTypes.PNF == type || SdcTypes.VF == type) {
                    JsonObject controllerProperties = createCdsArtifactProperties(
                            String.valueOf(nodeTemplate.getPropertyValue(SDNC_MODEL_NAME)),
                            String.valueOf(nodeTemplate.getPropertyValue(SDNC_MODEL_VERSION)));
                    if (controllerProperties != null) {
                        resourcesPropByType.getAsJsonObject(nodeTemplate.getName())
                                .add(CONTROLLER_PROPERTIES, controllerProperties);
                        logger.info("Successfully installed the CDS data in Service");
                    }
                    else {
                        logger.warn("Skipping CDS data installation in Service, as sdnc_model_name and "
                                + "sdnc_model_version are not provided in the CSAR");
                    }
                }
            }
        }
        serviceRepository.save(service);

        return service;
    }

    /**
     * This method updates the service model properties for CDS in the service object given in input.
     *
     * @param service the service object already provisioned with csar data
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Service updateCdsServiceProperties(Service service) {
        // Iterate on all types defined in the tosca lib
        for (SdcTypes type : SdcTypes.values()) {
            JsonObject resourcesPropByType = service.getResourceByType(type.getValue());
            for (String resourceName : resourcesPropByType.keySet()) {
                // get cds artifact information and save in resources Prop
                if ((SdcTypes.PNF == type || SdcTypes.VF == type) && resourcesPropByType.getAsJsonObject(resourceName)
                        .getAsJsonObject(CONTROLLER_PROPERTIES) != null) {
                    JsonObject controllerProperties =
                            createCdsArtifactProperties(resourcesPropByType.getAsJsonObject(resourceName)
                                            .getAsJsonObject(CONTROLLER_PROPERTIES).get(SDNC_MODEL_NAME)
                                            .getAsString(),
                                    resourcesPropByType.getAsJsonObject(resourceName)
                                            .getAsJsonObject(CONTROLLER_PROPERTIES).get(SDNC_MODEL_VERSION)
                                            .getAsString());
                    if (controllerProperties != null) {
                        resourcesPropByType.getAsJsonObject(resourceName)
                                .add(CONTROLLER_PROPERTIES, controllerProperties);
                    }
                }
            }
        }
        serviceRepository.save(service);
        logger.info("Successfully updated the CDS data in Service");
        return service;
    }

    /**
     * Retrieve CDS artifacts information from node template and save in resource object.
     *
     * @param sdncModelName    sdnc model name
     * @param sdncModelVersion sdnc model version
     * @return Returns CDS artifacts information
     */
    private JsonObject createCdsArtifactProperties(String sdncModelName, String sdncModelVersion) {
        if (sdncModelName != null && !"null".equals(sdncModelName)
                && sdncModelVersion != null && !"null".equals(sdncModelVersion)) {
            JsonObject controllerProperties = new JsonObject();
            controllerProperties.addProperty(SDNC_MODEL_NAME, sdncModelName);
            controllerProperties.addProperty(SDNC_MODEL_VERSION, sdncModelVersion);

            CdsBpWorkFlowListResponse response =
                    queryCdsToGetWorkFlowList(sdncModelName, sdncModelVersion);
            if (response == null) {
                return controllerProperties;
            }

            JsonObject workFlowProps = new JsonObject();
            for (String workFlow : response.getWorkflows()) {
                logger.info("Found CDS workflow " + workFlow + " for model name " + sdncModelName + " and version "
                        + sdncModelVersion);
                JsonObject inputs = queryCdsToGetWorkFlowInputProperties(response.getBlueprintName(),
                        response.getVersion(), workFlow);
                workFlowProps.add(workFlow, inputs);
            }

            controllerProperties.add("workflows", workFlowProps);
            return controllerProperties;
        }
        return null;
    }


    private CdsBpWorkFlowListResponse queryCdsToGetWorkFlowList(String artifactName, String artifactVersion) {
        return cdsServices.getBlueprintWorkflowList(artifactName, artifactVersion);
    }

    private JsonObject queryCdsToGetWorkFlowInputProperties(String artifactName, String artifactVersion,
                                                            String workFlow) {
        return cdsServices.getWorkflowInputProperties(artifactName, artifactVersion, workFlow);
    }
}
