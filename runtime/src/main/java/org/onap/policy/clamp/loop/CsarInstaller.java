/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.loop;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import org.json.simple.parser.ParseException;
import org.onap.policy.clamp.clds.client.DcaeInventoryServices;
import org.onap.policy.clamp.clds.exception.sdc.controller.BlueprintParserException;
import org.onap.policy.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.policy.clamp.clds.model.dcae.DcaeInventoryResponse;
import org.onap.policy.clamp.clds.sdc.controller.installer.BlueprintArtifact;
import org.onap.policy.clamp.clds.sdc.controller.installer.BlueprintMicroService;
import org.onap.policy.clamp.clds.sdc.controller.installer.BlueprintParser;
import org.onap.policy.clamp.clds.sdc.controller.installer.ChainGenerator;
import org.onap.policy.clamp.clds.sdc.controller.installer.CsarHandler;
import org.onap.policy.clamp.loop.cds.CdsDataInstaller;
import org.onap.policy.clamp.loop.service.CsarServiceInstaller;
import org.onap.policy.clamp.loop.service.Service;
import org.onap.policy.clamp.loop.template.LoopElementModel;
import org.onap.policy.clamp.loop.template.LoopTemplate;
import org.onap.policy.clamp.loop.template.LoopTemplatesRepository;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.clamp.loop.template.PolicyModelsRepository;
import org.onap.policy.clamp.policy.PolicyEngineServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * This class will be instantiated by spring config, and used by Sdc Controller.
 * There is no state kept by the bean. It's used to deploy the csar/notification
 * received from SDC in DB.
 */
@Component
@Qualifier("csarInstaller")
public class CsarInstaller {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CsarInstaller.class);

    @Autowired
    private PolicyModelsRepository policyModelsRepository;

    @Autowired
    private LoopTemplatesRepository loopTemplatesRepository;

    @Autowired
    private ChainGenerator chainGenerator;

    @Autowired
    private DcaeInventoryServices dcaeInventoryService;

    @Autowired
    private CsarServiceInstaller csarServiceInstaller;

    @Autowired
    private CdsDataInstaller cdsDataInstaller;

    @Autowired
    private PolicyEngineServices policyEngineServices;

    /**
     * Verify whether Csar is deployed.
     *
     * @param csar The Csar Handler
     * @return The flag indicating whether Csar is deployed
     * @throws SdcArtifactInstallerException The SdcArtifactInstallerException
     */
    public boolean isCsarAlreadyDeployed(CsarHandler csar) throws SdcArtifactInstallerException {
        boolean alreadyInstalled = csarServiceInstaller.isServiceAlreadyDeployed(csar);

        for (Entry<String, BlueprintArtifact> blueprint : csar.getMapOfBlueprints().entrySet()) {
            alreadyInstalled = alreadyInstalled
                    && loopTemplatesRepository.existsById(LoopTemplate.generateLoopTemplateName(
                    csar.getSdcNotification().getServiceName(), csar.getSdcNotification().getServiceVersion(),
                    blueprint.getValue().getResourceAttached().getResourceInstanceName(),
                    blueprint.getValue().getBlueprintArtifactName()));
        }
        return alreadyInstalled;
    }

    /**
     * Install the service and loop templates from the csar.
     *
     * @param csar The Csar Handler
     * @throws SdcArtifactInstallerException The SdcArtifactInstallerException
     * @throws InterruptedException          The InterruptedException
     * @throws BlueprintParserException      In case of issues with the blueprint
     *                                       parsing
     */
    public void installTheCsar(CsarHandler csar)
            throws SdcArtifactInstallerException, InterruptedException, BlueprintParserException {
        logger.info("Installing the CSAR " + csar.getFilePath());
        Service associatedService = csarServiceInstaller.installTheService(csar);
        cdsDataInstaller.installCdsServiceProperties(csar, associatedService);

        installTheLoopTemplates(csar, associatedService);
        logger.info("Successfully installed the CSAR " + csar.getFilePath());
    }

    /**
     * Install the loop templates from the csar.
     *
     * @param csar    The Csar Handler
     * @param service The service object that is related to the loop
     * @throws SdcArtifactInstallerException The SdcArtifactInstallerException
     * @throws InterruptedException          The InterruptedException
     * @throws BlueprintParserException      In case of issues with the blueprint
     *                                       parsing
     */
    public void installTheLoopTemplates(CsarHandler csar, Service service)
            throws SdcArtifactInstallerException, InterruptedException, BlueprintParserException {
        try {
            logger.info("Installing the Loops");
            for (Entry<String, BlueprintArtifact> blueprint : csar.getMapOfBlueprints().entrySet()) {
                logger.info("Processing blueprint " + blueprint.getValue().getBlueprintArtifactName());
                loopTemplatesRepository.save(createLoopTemplateFromBlueprint(csar, blueprint.getValue(), service));
            }
            logger.info("Successfully installed the Loops ");
        } catch (IOException e) {
            throw new SdcArtifactInstallerException("Exception caught during the Loop installation in database", e);
        } catch (ParseException e) {
            throw new SdcArtifactInstallerException("Exception caught during the Dcae query to get ServiceTypeId", e);
        }
    }

    private LoopTemplate createLoopTemplateFromBlueprint(CsarHandler csar, BlueprintArtifact blueprintArtifact,
                                                         Service service)
            throws IOException, ParseException, InterruptedException, BlueprintParserException,
            SdcArtifactInstallerException {
        LoopTemplate newLoopTemplate = new LoopTemplate();
        newLoopTemplate.setBlueprint(blueprintArtifact.getDcaeBlueprint());
        newLoopTemplate.setName(LoopTemplate.generateLoopTemplateName(csar.getSdcNotification().getServiceName(),
                csar.getSdcNotification().getServiceVersion(),
                blueprintArtifact.getResourceAttached().getResourceInstanceName(),
                blueprintArtifact.getBlueprintArtifactName()));
        List<BlueprintMicroService> microServicesChain = chainGenerator
                .getChainOfMicroServices(BlueprintParser.getMicroServices(blueprintArtifact.getDcaeBlueprint()));
        if (microServicesChain.isEmpty()) {
            microServicesChain = BlueprintParser.fallbackToOneMicroService();
        }
        newLoopTemplate.setModelService(service);
        newLoopTemplate.addLoopElementModels(createMicroServiceModels(blueprintArtifact, microServicesChain));
        newLoopTemplate.setMaximumInstancesAllowed(0);
        DcaeInventoryResponse dcaeResponse = queryDcaeToGetServiceTypeId(blueprintArtifact);
        newLoopTemplate.setDcaeBlueprintId(dcaeResponse.getTypeId());
        return newLoopTemplate;
    }

    private HashSet<LoopElementModel> createMicroServiceModels(BlueprintArtifact blueprintArtifact,
                                                               List<BlueprintMicroService> microServicesChain)
            throws SdcArtifactInstallerException {
        HashSet<LoopElementModel> newSet = new HashSet<>();
        for (BlueprintMicroService microService : microServicesChain) {
            LoopElementModel loopElementModel =
                    new LoopElementModel(microService.getModelType(), LoopElementModel.MICRO_SERVICE_TYPE,
                            null);
            newSet.add(loopElementModel);
            PolicyModel newPolicyModel = policyEngineServices.createPolicyModelFromPolicyEngine(microService);
            if (newPolicyModel != null) {
                loopElementModel.addPolicyModel(newPolicyModel);
            } else {
                throw new SdcArtifactInstallerException(
                        "Unable to find the policy specified in the blueprint " + blueprintArtifact
                                .getBlueprintArtifactName() + ") on the Policy Engine:"
                                + microService.getModelType() + "/" + microService.getModelVersion());
            }
        }
        return newSet;
    }

    /**
     * Get the service blueprint Id in the Dcae inventory using the SDC UUID.
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
