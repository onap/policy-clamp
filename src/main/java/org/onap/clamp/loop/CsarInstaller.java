/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.loop;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import org.json.simple.parser.ParseException;
import org.onap.clamp.clds.client.DcaeInventoryServices;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.clamp.clds.model.dcae.DcaeInventoryResponse;
import org.onap.clamp.clds.sdc.controller.installer.BlueprintArtifact;
import org.onap.clamp.clds.sdc.controller.installer.BlueprintParser;
import org.onap.clamp.clds.sdc.controller.installer.ChainGenerator;
import org.onap.clamp.clds.sdc.controller.installer.CsarHandler;
import org.onap.clamp.clds.sdc.controller.installer.MicroService;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.drawing.SvgFacade;
import org.onap.clamp.loop.deploy.DcaeDeployParameters;
import org.onap.clamp.loop.service.Service;
import org.onap.clamp.loop.service.ServiceRepository;
import org.onap.clamp.policy.Policy;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;
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
import org.yaml.snakeyaml.Yaml;

/**
 * This class will be instantiated by spring config, and used by Sdc Controller.
 * There is no state kept by the bean. It's used to deploy the csar/notification
 * received from SDC in DB.
 */
@Component
@Qualifier("csarInstaller")
public class CsarInstaller {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CsarInstaller.class);
    public static final String CONTROL_NAME_PREFIX = "ClosedLoop-";
    public static final String GET_INPUT_BLUEPRINT_PARAM = "get_input";
    // This will be used later as the policy scope
    public static final String MODEL_NAME_PREFIX = "Loop_";

    @Autowired
    LoopsRepository loopRepository;

    @Autowired
    ServiceRepository serviceRepository;

    @Autowired
    BlueprintParser blueprintParser;

    @Autowired
    ChainGenerator chainGenerator;

    @Autowired
    DcaeInventoryServices dcaeInventoryService;

    @Autowired
    private SvgFacade svgFacade;

    /**
    * Verify whether Csar is deployed.
    * 
    * @param csar The Csar Handler
    * @return The flag indicating whether Csar is deployed
    * @throws SdcArtifactInstallerException The SdcArtifactInstallerException
    */
    public boolean isCsarAlreadyDeployed(CsarHandler csar) throws SdcArtifactInstallerException {
        boolean alreadyInstalled = true;
        JsonObject serviceDetails = JsonUtils.GSON.fromJson(
                JsonUtils.GSON.toJson(csar.getSdcCsarHelper().getServiceMetadataAllProperties()), JsonObject.class);
        alreadyInstalled = alreadyInstalled
                && serviceRepository.existsById(serviceDetails.get("UUID").getAsString());

        for (Entry<String, BlueprintArtifact> blueprint : csar.getMapOfBlueprints().entrySet()) {
            alreadyInstalled = alreadyInstalled
                    && loopRepository.existsById(Loop.generateLoopName(csar.getSdcNotification().getServiceName(),
                            csar.getSdcNotification().getServiceVersion(),
                            blueprint.getValue().getResourceAttached().getResourceInstanceName(),
                            blueprint.getValue().getBlueprintArtifactName()));
        }
        
        return alreadyInstalled;
    }

    /**
     * Install the service and loops from the csar.
     * 
     * @param csar The Csar Handler
     * @throws SdcArtifactInstallerException The SdcArtifactInstallerException
     * @throws InterruptedException The InterruptedException
     */
    public void installTheCsar(CsarHandler csar) throws SdcArtifactInstallerException, InterruptedException {
        logger.info("Installing the CSAR " + csar.getFilePath());
        installTheLoop(csar, installTheService(csar));
        logger.info("Successfully installed the CSAR " + csar.getFilePath());
    }

    /**
     * Install the Loop from the csar.
     * 
     * @param csar The Csar Handler
     * @param service The service object that is related to the loop
     * @throws SdcArtifactInstallerException The SdcArtifactInstallerException
     * @throws InterruptedException The InterruptedException
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void installTheLoop(CsarHandler csar, Service service) 
            throws SdcArtifactInstallerException, InterruptedException {
        try {
            logger.info("Installing the Loops");
            for (Entry<String, BlueprintArtifact> blueprint : csar.getMapOfBlueprints().entrySet()) {
                logger.info("Processing blueprint " + blueprint.getValue().getBlueprintArtifactName());
                loopRepository.save(createLoopFromBlueprint(csar, blueprint.getValue(), service));
            }
            logger.info("Successfully installed the Loops ");
        } catch (IOException e) {
            throw new SdcArtifactInstallerException("Exception caught during the Loop installation in database", e);
        } catch (ParseException e) {
            throw new SdcArtifactInstallerException("Exception caught during the Dcae query to get ServiceTypeId", e);
        }
    }

    /**
     * Install the Service from the csar.
     * 
     * @param csar The Csar Handler
     * @return The service object
     */
    @Transactional
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

    private Loop createLoopFromBlueprint(CsarHandler csar, BlueprintArtifact blueprintArtifact, Service service)
            throws IOException, ParseException, InterruptedException {
        Loop newLoop = new Loop();
        newLoop.setBlueprint(blueprintArtifact.getDcaeBlueprint());
        newLoop.setName(Loop.generateLoopName(csar.getSdcNotification().getServiceName(),
                csar.getSdcNotification().getServiceVersion(),
                blueprintArtifact.getResourceAttached().getResourceInstanceName(),
                blueprintArtifact.getBlueprintArtifactName()));
        newLoop.setLastComputedState(LoopState.DESIGN);

        List<MicroService> microServicesChain = chainGenerator
                .getChainOfMicroServices(blueprintParser.getMicroServices(blueprintArtifact.getDcaeBlueprint()));
        if (microServicesChain.isEmpty()) {
            microServicesChain = blueprintParser.fallbackToOneMicroService(blueprintArtifact.getDcaeBlueprint());
        }
        newLoop.setModelService(service);
        newLoop.setMicroServicePolicies(
                createMicroServicePolicies(microServicesChain, csar, blueprintArtifact, newLoop));
        newLoop.setOperationalPolicies(createOperationalPolicies(csar, blueprintArtifact, newLoop));

        newLoop.setSvgRepresentation(svgFacade.getSvgImage(microServicesChain));
        newLoop.setGlobalPropertiesJson(createGlobalPropertiesJson(blueprintArtifact, newLoop));

        DcaeInventoryResponse dcaeResponse = queryDcaeToGetServiceTypeId(blueprintArtifact);
        newLoop.setDcaeBlueprintId(dcaeResponse.getTypeId());
        return newLoop;
    }

    private HashSet<OperationalPolicy> createOperationalPolicies(CsarHandler csar, BlueprintArtifact blueprintArtifact,
            Loop newLoop) {
        return new HashSet<>(Arrays.asList(new OperationalPolicy(Policy.generatePolicyName("OPERATIONAL",
                csar.getSdcNotification().getServiceName(), csar.getSdcNotification().getServiceVersion(),
                blueprintArtifact.getResourceAttached().getResourceInstanceName(),
                blueprintArtifact.getBlueprintArtifactName()), newLoop, new JsonObject())));
    }

    private HashSet<MicroServicePolicy> createMicroServicePolicies(List<MicroService> microServicesChain,
            CsarHandler csar, BlueprintArtifact blueprintArtifact, Loop newLoop) throws IOException {
        HashSet<MicroServicePolicy> newSet = new HashSet<>();

        for (MicroService microService : microServicesChain) {
            MicroServicePolicy microServicePolicy = new MicroServicePolicy(
                    Policy.generatePolicyName(microService.getName(), csar.getSdcNotification().getServiceName(),
                            csar.getSdcNotification().getServiceVersion(),
                            blueprintArtifact.getResourceAttached().getResourceInstanceName(),
                            blueprintArtifact.getBlueprintArtifactName()),
                    microService.getModelType(), csar.getPolicyModelYaml().orElse(""), false,
                    new HashSet<>(Arrays.asList(newLoop)));

            newSet.add(microServicePolicy);
            microService.setMappedNameJpa(microServicePolicy.getName());
        }
        return newSet;
    }

    private JsonObject createGlobalPropertiesJson(BlueprintArtifact blueprintArtifact, Loop newLoop) {
        return DcaeDeployParameters.getDcaeDeploymentParametersInJson(blueprintArtifact, newLoop);
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

    private static JsonObject createServicePropertiesByType(CsarHandler csar) {
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

    /**
     * ll get the latest version of the artifact (version can be specified to DCAE
     * call).
     *
     * @return The DcaeInventoryResponse object containing the dcae values
     */
    private DcaeInventoryResponse queryDcaeToGetServiceTypeId(BlueprintArtifact blueprintArtifact)
            throws IOException, ParseException, InterruptedException {
        return dcaeInventoryService.getDcaeInformation(blueprintArtifact.getBlueprintArtifactName(),
                blueprintArtifact.getBlueprintInvariantServiceUuid(),
                blueprintArtifact.getResourceAttached().getResourceInvariantUUID());
    }

}
