/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications copyright (c) 2019 Nokia
 * Modifications Copyright (c) 2019 Samsung
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.transaction.Transactional;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.policy.clamp.clds.Application;
import org.onap.policy.clamp.clds.exception.sdc.controller.BlueprintParserException;
import org.onap.policy.clamp.clds.exception.sdc.controller.CsarHandlerException;
import org.onap.policy.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.policy.clamp.clds.sdc.controller.installer.BlueprintArtifact;
import org.onap.policy.clamp.clds.sdc.controller.installer.CsarHandler;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;
import org.onap.policy.clamp.loop.cds.CdsDataInstaller;
import org.onap.policy.clamp.loop.service.ServicesRepository;
import org.onap.policy.clamp.loop.template.LoopTemplate;
import org.onap.policy.clamp.loop.template.LoopTemplateLoopElementModel;
import org.onap.policy.clamp.loop.template.LoopTemplatesRepository;
import org.onap.policy.clamp.loop.template.PolicyModelId;
import org.onap.policy.clamp.loop.template.PolicyModelsRepository;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.onap.sdc.tosca.parser.impl.SdcToscaParserFactory;
import org.onap.sdc.toscaparser.api.elements.Metadata;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@ActiveProfiles({"clamp-default", "clamp-default-user", "clamp-sdc-controller"})
public class CsarInstallerItTestCase {

    private static final String CSAR_ARTIFACT_NAME_CDS = "example/sdc/service_Vloadbalancerms_cds.csar";
    private static final String CSAR_ARTIFACT_NAME_NO_CDS = "example/sdc/service_Vloadbalancerms_no_cds.csar";
    private static final String INVARIANT_SERVICE_UUID = "4cc5b45a-1f63-4194-8100-cd8e14248c92";
    private static final String INVARIANT_RESOURCE1_UUID = "07e266fc-49ab-4cd7-8378-ca4676f1b9ec";
    private static final String INVARIANT_RESOURCE2_UUID = "023a3f0d-1161-45ff-b4cf-8918a8ccf3ad";
    private static final String RESOURCE_INSTANCE_NAME_RESOURCE1 = "ResourceInstanceName1";
    private static final String RESOURCE_INSTANCE_NAME_RESOURCE2 = "ResourceInstanceName2";

    @Autowired
    private LoopTemplatesRepository loopTemplatesRepo;

    @Autowired
    ServicesRepository serviceRepository;

    @Autowired
    PolicyModelsRepository policyModelsRepository;

    @Autowired
    @Qualifier("csarInstaller")
    private CsarInstaller csarInstaller;

    private BlueprintArtifact buildFakeBlueprintArtifact(String instanceName, String invariantResourceUuid,
                                                         String blueprintFilePath, String artifactName,
                                                         String invariantServiceUuid) throws IOException {
        IResourceInstance resource = Mockito.mock(IResourceInstance.class);
        Mockito.when(resource.getResourceInstanceName()).thenReturn(instanceName);
        Mockito.when(resource.getResourceInvariantUUID()).thenReturn(invariantResourceUuid);
        BlueprintArtifact blueprintArtifact = Mockito.mock(BlueprintArtifact.class);
        Mockito.when(blueprintArtifact.getDcaeBlueprint())
                .thenReturn(ResourceFileUtils.getResourceAsString(blueprintFilePath));
        Mockito.when(blueprintArtifact.getBlueprintArtifactName()).thenReturn(artifactName);
        Mockito.when(blueprintArtifact.getBlueprintInvariantServiceUuid()).thenReturn(invariantServiceUuid);
        Mockito.when(blueprintArtifact.getResourceAttached()).thenReturn(resource);
        return blueprintArtifact;
    }

    private CsarHandler buildBadFakeCsarHandler(String generatedName, String csarFileName) throws IOException,
            SdcToscaParserException {

        // Build a Bad csar because the blueprint contains a link to a microservice that does not exist in the emulator
        // Create fake notification
        INotificationData notificationData = Mockito.mock(INotificationData.class);
        Mockito.when(notificationData.getServiceVersion()).thenReturn("1.0");
        // Create fake resource in notification
        CsarHandler csarHandler = Mockito.mock(CsarHandler.class);
        List<IResourceInstance> listResources = new ArrayList<>();
        Mockito.when(notificationData.getResources()).thenReturn(listResources);
        Map<String, BlueprintArtifact> blueprintMap = new HashMap<>();
        Mockito.when(csarHandler.getMapOfBlueprints()).thenReturn(blueprintMap);
        // Create fake blueprint artifact 1 on resource1
        BlueprintArtifact blueprintArtifact = buildFakeBlueprintArtifact(RESOURCE_INSTANCE_NAME_RESOURCE1,
                INVARIANT_RESOURCE1_UUID, "example/sdc/blueprint-dcae/tca-bad-policy.yaml", "tca-bad-policy.yaml",
                INVARIANT_SERVICE_UUID);
        listResources.add(blueprintArtifact.getResourceAttached());
        blueprintMap.put(blueprintArtifact.getBlueprintArtifactName(), blueprintArtifact);

        // Build fake csarhandler
        Mockito.when(csarHandler.getSdcNotification()).thenReturn(notificationData);
        // Build fake csar Helper
        ISdcCsarHelper csarHelper = Mockito.mock(ISdcCsarHelper.class);
        Metadata data = Mockito.mock(Metadata.class);
        Mockito.when(data.getValue("name")).thenReturn(generatedName);
        Mockito.when(notificationData.getServiceName()).thenReturn(generatedName);
        Mockito.when(csarHelper.getServiceMetadata()).thenReturn(data);

        // Create helper based on real csar to test policy yaml and global properties
        // set
        SdcToscaParserFactory factory = SdcToscaParserFactory.getInstance();
        String path = Thread.currentThread().getContextClassLoader().getResource(csarFileName).getFile();
        ISdcCsarHelper sdcHelper = factory.getSdcCsarHelper(path);
        Mockito.when(csarHandler.getSdcCsarHelper()).thenReturn(sdcHelper);

        // Mockito.when(csarHandler.getSdcCsarHelper()).thenReturn(csarHelper);
        Mockito.when(csarHandler.getPolicyModelYaml())
                .thenReturn(Optional.ofNullable(ResourceFileUtils.getResourceAsString("tosca/tosca_example.yaml")));
        return csarHandler;
    }

    private CsarHandler buildFakeCsarHandler(String generatedName, String csarFileName) throws IOException,
            SdcToscaParserException {
        // Create fake notification
        INotificationData notificationData = Mockito.mock(INotificationData.class);
        Mockito.when(notificationData.getServiceVersion()).thenReturn("1.0");
        // Create fake resource in notification
        CsarHandler csarHandler = Mockito.mock(CsarHandler.class);
        List<IResourceInstance> listResources = new ArrayList<>();
        Mockito.when(notificationData.getResources()).thenReturn(listResources);
        Map<String, BlueprintArtifact> blueprintMap = new HashMap<>();
        Mockito.when(csarHandler.getMapOfBlueprints()).thenReturn(blueprintMap);
        // Create fake blueprint artifact 1 on resource1
        BlueprintArtifact blueprintArtifact = buildFakeBlueprintArtifact(RESOURCE_INSTANCE_NAME_RESOURCE1,
                INVARIANT_RESOURCE1_UUID, "example/sdc/blueprint-dcae/tca.yaml", "tca.yaml", INVARIANT_SERVICE_UUID);
        listResources.add(blueprintArtifact.getResourceAttached());
        blueprintMap.put(blueprintArtifact.getBlueprintArtifactName(), blueprintArtifact);
        // Create fake blueprint artifact 2 on resource2
        blueprintArtifact = buildFakeBlueprintArtifact(RESOURCE_INSTANCE_NAME_RESOURCE2, INVARIANT_RESOURCE2_UUID,
                "example/sdc/blueprint-dcae/tca_2.yaml", "tca_2.yaml", INVARIANT_SERVICE_UUID);
        listResources.add(blueprintArtifact.getResourceAttached());
        blueprintMap.put(blueprintArtifact.getBlueprintArtifactName(), blueprintArtifact);

        // Create fake blueprint artifact 3 on resource 1 so that it's possible to
        // test multiple CL deployment per Service/vnf
        blueprintArtifact = buildFakeBlueprintArtifact(RESOURCE_INSTANCE_NAME_RESOURCE1, INVARIANT_RESOURCE1_UUID,
                "example/sdc/blueprint-dcae/tca_3.yaml", "tca_3.yaml", INVARIANT_SERVICE_UUID);
        blueprintMap.put(blueprintArtifact.getBlueprintArtifactName(), blueprintArtifact);

        // Create fake blueprint artifact 3 on resource 1 so that it's possible to
        // test multiple CL deployment per Service/vnf
        blueprintArtifact = buildFakeBlueprintArtifact(RESOURCE_INSTANCE_NAME_RESOURCE1, INVARIANT_RESOURCE1_UUID,
                "example/sdc/blueprint-dcae/tca-guilin.yaml", "tca-guilin.yaml", INVARIANT_SERVICE_UUID);
        blueprintMap.put(blueprintArtifact.getBlueprintArtifactName(), blueprintArtifact);


        // Build fake csarhandler
        Mockito.when(csarHandler.getSdcNotification()).thenReturn(notificationData);
        // Build fake csar Helper
        ISdcCsarHelper csarHelper = Mockito.mock(ISdcCsarHelper.class);
        Metadata data = Mockito.mock(Metadata.class);
        Mockito.when(data.getValue("name")).thenReturn(generatedName);
        Mockito.when(notificationData.getServiceName()).thenReturn(generatedName);
        Mockito.when(csarHelper.getServiceMetadata()).thenReturn(data);

        // Create helper based on real csar to test policy yaml and global properties
        // set
        SdcToscaParserFactory factory = SdcToscaParserFactory.getInstance();
        String path = Thread.currentThread().getContextClassLoader().getResource(csarFileName).getFile();
        ISdcCsarHelper sdcHelper = factory.getSdcCsarHelper(path);
        Mockito.when(csarHandler.getSdcCsarHelper()).thenReturn(sdcHelper);

        // Mockito.when(csarHandler.getSdcCsarHelper()).thenReturn(csarHelper);
        Mockito.when(csarHandler.getPolicyModelYaml())
                .thenReturn(Optional.ofNullable(ResourceFileUtils.getResourceAsString("tosca/tosca_example.yaml")));
        return csarHandler;
    }

    @Test
    @Transactional
    public void testGetPolicyModelYaml() throws IOException, SdcToscaParserException, CsarHandlerException {
        INotificationData notificationData = Mockito.mock(INotificationData.class);
        IArtifactInfo serviceArtifacts = Mockito.mock(IArtifactInfo.class);
        Mockito.when(serviceArtifacts.getArtifactType()).thenReturn("TOSCA_CSAR");
        List<IArtifactInfo> serviceArtifactsList = new ArrayList<>();
        serviceArtifactsList.add(serviceArtifacts);
        Mockito.when(notificationData.getServiceArtifacts()).thenReturn(serviceArtifactsList);

        CsarHandler csarHandler = new CsarHandler(notificationData, "", "");
        csarHandler.setFilePath(Thread.currentThread().getContextClassLoader().getResource(CSAR_ARTIFACT_NAME_CDS)
                .getFile());
        Assert.assertEquals(csarHandler.getPolicyModelYaml(), Optional
                .ofNullable(ResourceFileUtils.getResourceAsString("example/sdc/expected-result/policy-data.yaml")));
    }

    @Test
    @Transactional
    public void testIsCsarAlreadyDeployedTca() throws SdcArtifactInstallerException, SdcToscaParserException,
            CsarHandlerException, IOException, InterruptedException, BlueprintParserException {
        String generatedName = RandomStringUtils.randomAlphanumeric(5);
        CsarHandler csarHandler = buildFakeCsarHandler(generatedName, CSAR_ARTIFACT_NAME_CDS);
        assertThat(csarInstaller.isCsarAlreadyDeployed(csarHandler)).isFalse();
        csarInstaller.installTheCsar(csarHandler);
        assertThat(csarInstaller.isCsarAlreadyDeployed(csarHandler)).isTrue();
    }

    @Test
    @Transactional
    public void testWithoutCdsTca() throws SdcArtifactInstallerException, SdcToscaParserException,
            CsarHandlerException, IOException, InterruptedException, BlueprintParserException {
        String generatedName = RandomStringUtils.randomAlphanumeric(5);
        CsarHandler csarHandler = buildFakeCsarHandler(generatedName, CSAR_ARTIFACT_NAME_NO_CDS);

        assertThat(csarInstaller.isCsarAlreadyDeployed(csarHandler)).isFalse();
        csarInstaller.installTheCsar(csarHandler);
        assertThat(csarInstaller.isCsarAlreadyDeployed(csarHandler)).isTrue();
    }

    @Test(expected = SdcArtifactInstallerException.class)
    @Transactional
    public void testInstallTheBadCsarTca()
            throws IOException, SdcToscaParserException, InterruptedException, BlueprintParserException,
            SdcArtifactInstallerException {
        // This test validates that the blueprint is well rejected because the blueprint contains a link
        // to a policy that does not exist on the policy engine emulator.
        String generatedName = RandomStringUtils.randomAlphanumeric(5);
        csarInstaller.installTheCsar(buildBadFakeCsarHandler(generatedName, CSAR_ARTIFACT_NAME_NO_CDS));
    }

    @Test
    @Transactional
    @Commit
    public void testInstallTheCsarTca() throws SdcArtifactInstallerException, SdcToscaParserException,
            CsarHandlerException, IOException, JSONException, InterruptedException, BlueprintParserException {
        String generatedName = RandomStringUtils.randomAlphanumeric(5);
        csarInstaller.installTheCsar(buildFakeCsarHandler(generatedName, CSAR_ARTIFACT_NAME_CDS));

        assertThat(serviceRepository.existsById("63cac700-ab9a-4115-a74f-7eac85e3fce0")).isTrue();
        // We should have CDS info
        assertThat(serviceRepository.findById("63cac700-ab9a-4115-a74f-7eac85e3fce0").get().getResourceByType("VF")
                .getAsJsonObject("vLoadBalancerMS 0").getAsJsonObject(
                        CdsDataInstaller.CONTROLLER_PROPERTIES)).isNotNull();
        assertThat(loopTemplatesRepo.existsById(LoopTemplate.generateLoopTemplateName(generatedName, "1.0",
                RESOURCE_INSTANCE_NAME_RESOURCE1, "tca.yaml"))).isTrue();
        assertThat(loopTemplatesRepo.existsById(LoopTemplate.generateLoopTemplateName(generatedName, "1.0",
                RESOURCE_INSTANCE_NAME_RESOURCE1, "tca_3.yaml"))).isTrue();
        assertThat(loopTemplatesRepo.existsById(LoopTemplate.generateLoopTemplateName(generatedName, "1.0",
                RESOURCE_INSTANCE_NAME_RESOURCE2, "tca_2.yaml"))).isTrue();
        assertThat(loopTemplatesRepo.existsById(LoopTemplate.generateLoopTemplateName(generatedName, "1.0",
                RESOURCE_INSTANCE_NAME_RESOURCE1, "tca-guilin.yaml"))).isTrue();
        // Verify now that policy and json representation, global properties are well
        // set
        LoopTemplate loopTemplate = loopTemplatesRepo.findById(LoopTemplate.generateLoopTemplateName(generatedName,
                "1.0", RESOURCE_INSTANCE_NAME_RESOURCE1, "tca.yaml")).get();
        assertThat(loopTemplate.getLoopElementModelsUsed()).hasSize(1);
        Assertions.assertThat(loopTemplate.getModelService().getServiceUuid())
                .isEqualTo("63cac700-ab9a-4115-a74f-7eac85e3fce0");
        JSONAssert.assertEquals(ResourceFileUtils.getResourceAsString("tosca/model-properties.json"),
                JsonUtils.GSON_JPA_MODEL.toJson(loopTemplate.getModelService()), true);
        JSONAssert.assertEquals(ResourceFileUtils.getResourceAsString("tosca/service-details.json"),
                JsonUtils.GSON_JPA_MODEL.toJson(loopTemplate.getModelService().getServiceDetails()), true);
        JSONAssert.assertEquals(ResourceFileUtils.getResourceAsString("tosca/resource-details.json"),
                JsonUtils.GSON_JPA_MODEL.toJson(loopTemplate.getModelService().getResourceDetails()), true);
        Assertions.assertThat(((LoopTemplateLoopElementModel) (loopTemplate.getLoopElementModelsUsed().toArray()[0]))
                .getLoopElementModel().getName()).isNotEmpty();

        loopTemplate = loopTemplatesRepo.findById(LoopTemplate.generateLoopTemplateName(generatedName, "1.0",
                RESOURCE_INSTANCE_NAME_RESOURCE1, "tca_3.yaml")).get();
        assertThat(((LoopTemplateLoopElementModel) (loopTemplate.getLoopElementModelsUsed().toArray()[0]))
                .getLoopElementModel().getName()).isNotEmpty();
        assertThat(((LoopTemplateLoopElementModel) (loopTemplate.getLoopElementModelsUsed().toArray()[0]))
                .getLoopElementModel().getName()).isNotEmpty();
        assertThat(loopTemplate.getMaximumInstancesAllowed()).isEqualByComparingTo(Integer.valueOf(0));
        loopTemplate = loopTemplatesRepo.findById(LoopTemplate.generateLoopTemplateName(generatedName, "1.0",
                RESOURCE_INSTANCE_NAME_RESOURCE2, "tca_2.yaml")).get();
        assertThat(((LoopTemplateLoopElementModel) (loopTemplate.getLoopElementModelsUsed().toArray()[0]))
                .getLoopElementModel().getName()).isNotEmpty();

        assertThat(policyModelsRepository.findAll().size()).isPositive();
        assertThat(policyModelsRepository
                .existsById(new PolicyModelId("onap.policies.monitoring.cdap.tca.hi.lo.app", "1.0.0"))).isTrue();
        assertThat(policyModelsRepository
                .getOne((new PolicyModelId("onap.policies.monitoring.cdap.tca.hi.lo.app", "1.0.0")))
                .getPolicyModelTosca()).isNotBlank();
    }
}
