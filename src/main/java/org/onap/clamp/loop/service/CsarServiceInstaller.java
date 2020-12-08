/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
 *                             reserved.
 * Modifications Copyright (C) 2020 Huawei Technologies Co., Ltd.
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

package org.onap.clamp.loop.service;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonObject;
import java.util.Map.Entry;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.clamp.clds.sdc.controller.installer.CsarHandler;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.sdc.tosca.parser.api.IEntityDetails;
import org.onap.sdc.tosca.parser.elements.queries.EntityQuery;
import org.onap.sdc.tosca.parser.elements.queries.TopologyTemplateQuery;
import org.onap.sdc.tosca.parser.enums.EntityTemplateType;
import org.onap.sdc.tosca.parser.enums.SdcTypes;
import org.onap.sdc.toscaparser.api.NodeTemplate;
import org.onap.sdc.toscaparser.api.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Qualifier("csarInstaller")
public class CsarServiceInstaller {
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CsarServiceInstaller.class);

    @Autowired
    ServicesRepository serviceRepository;

    /**
     * Install the Service from the csar.
     *
     * @param csar The Csar Handler
     * @return The service object
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Service installTheService(CsarHandler csar) {
        logger.info("Start to install the Service from csar");
        JsonObject serviceDetails = JsonUtils.GSON.fromJson(
                JsonUtils.GSON.toJson(csar.getSdcCsarHelper().getServiceMetadataAllProperties()), JsonObject.class);

        // Add properties details for each type, VfModule, VF, VFC, ....
        JsonObject resourcesProp = createServicePropertiesByType(csar);
        resourcesProp.add("VFModule", createVfModuleProperties(csar));

        Service modelService = new Service(serviceDetails, resourcesProp,
                csar.getSdcNotification().getServiceVersion());

        serviceRepository.save(modelService);
        logger.info("Successfully installed the Service");
        return modelService;
    }

    private JsonObject createServicePropertiesByType(CsarHandler csar) {
        JsonObject resourcesProp = new JsonObject();
        // Iterate on all types defined in the tosca lib
        for (SdcTypes type : SdcTypes.values()) {
            JsonObject resourcesPropByType = new JsonObject();
            // For each type, get the metadata of each nodetemplate
            for (NodeTemplate nodeTemplate : csar.getSdcCsarHelper().getServiceNodeTemplateBySdcType(type)) {
                resourcesPropByType.add(nodeTemplate.getName(),
                        JsonUtils.GSON.toJsonTree(nodeTemplate.getMetaData().getAllProperties()));
            }
            resourcesProp.add(type.getValue(), resourcesPropByType);
        }
        return resourcesProp;
    }

    private static JsonObject createVfModuleProperties(CsarHandler csar) {
        JsonObject vfModuleProps = new JsonObject();
        // Loop on all Groups defined in the service (VFModule entries type:
        // org.openecomp.groups.VfModule)
        for (IEntityDetails entity : csar.getSdcCsarHelper().getEntity(
                EntityQuery.newBuilder(EntityTemplateType.GROUP).build(),
                TopologyTemplateQuery.newBuilder(SdcTypes.SERVICE).build(), false)) {
            // Get all metadata info
            JsonObject allVfProps = (JsonObject) JsonUtils.GSON.toJsonTree(entity.getMetadata().getAllProperties());
            vfModuleProps.add(entity.getMetadata().getAllProperties().get("vfModuleModelName"), allVfProps);
            // now append the properties section so that we can also have isBase,
            // volume_group, etc ... fields under the VFmodule name
            for (Entry<String, Property> additionalProp : entity.getProperties().entrySet()) {
                allVfProps.add(additionalProp.getValue().getName(),
                        JsonUtils.GSON.toJsonTree(additionalProp.getValue().getValue()));
            }
        }
        return vfModuleProps;
    }

    /**
     * Verify whether Service in Csar is deployed.
     *
     * @param csar The Csar Handler
     * @return The flag indicating whether Service is deployed
     * @throws SdcArtifactInstallerException The SdcArtifactInstallerException
     */
    public boolean isServiceAlreadyDeployed(CsarHandler csar) throws SdcArtifactInstallerException {
        boolean alreadyInstalled = true;
        JsonObject serviceDetails = JsonUtils.GSON.fromJson(
                JsonUtils.GSON.toJson(csar.getSdcCsarHelper().getServiceMetadataAllProperties()), JsonObject.class);
        alreadyInstalled = serviceRepository.existsById(serviceDetails.get("UUID").getAsString());

        return alreadyInstalled;
    }
}
