/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.it.sdc.controller.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.exception.policy.PolicyModelException;
import org.onap.clamp.clds.exception.sdc.controller.CsarHandlerException;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.sdc.controller.installer.BlueprintArtifact;
import org.onap.clamp.clds.sdc.controller.installer.CsarHandler;
import org.onap.clamp.clds.sdc.controller.installer.CsarInstaller;
import org.onap.clamp.clds.sdc.controller.installer.CsarInstallerImpl;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.onap.sdc.toscaparser.api.elements.Metadata;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = "clamp-default,clamp-default-user,clamp-sdc-controller")
public class CsarInstallerItCase {

    private static final String INVARIANT_SERVICE_UUID = "4cc5b45a-1f63-4194-8100-cd8e14248c92";
    private static final String INVARIANT_RESOURCE1_UUID = "07e266fc-49ab-4cd7-8378-ca4676f1b9ec";
    private static final String INVARIANT_RESOURCE2_UUID = "023a3f0d-1161-45ff-b4cf-8918a8ccf3ad";
    private static final String RESOURCE_INSTANCE_NAME_RESOURCE1 = "ResourceInstanceName1";
    private static final String RESOURCE_INSTANCE_NAME_RESOURCE2 = "ResourceInstanceName2";
    @Autowired
    private CsarInstaller csarInstaller;
    @Autowired
    private CldsDao cldsDao;

    @Test(expected = SdcArtifactInstallerException.class)
    public void testInstallTheCsarFail() throws SdcArtifactInstallerException, SdcToscaParserException,
        CsarHandlerException, IOException, InterruptedException, PolicyModelException {
        CsarHandler csarHandler = Mockito.mock(CsarHandler.class);
        BlueprintArtifact blueprintArtifact = Mockito.mock(BlueprintArtifact.class);
        Mockito.when(blueprintArtifact.getResourceAttached()).thenReturn(Mockito.mock(IResourceInstance.class));
        Map<String, BlueprintArtifact> blueprintMap = new HashMap<>();
        blueprintMap.put("resourceid", blueprintArtifact);
        Mockito.when(csarHandler.getMapOfBlueprints()).thenReturn(blueprintMap);
        Mockito.when(blueprintArtifact.getDcaeBlueprint()).thenReturn(
            IOUtils.toString(ResourceFileUtil.getResourceAsStream("example/sdc/blueprint-dcae/not-recognized.yaml"),
                StandardCharsets.UTF_8));
        csarInstaller.installTheCsar(csarHandler);
        fail("Should have raised an SdcArtifactInstallerException");
    }

    private BlueprintArtifact buildFakeBuildprintArtifact(String instanceName, String invariantResourceUuid,
        String blueprintFilePath, String artifactName, String invariantServiceUuid) throws IOException {
        IResourceInstance resource = Mockito.mock(IResourceInstance.class);
        Mockito.when(resource.getResourceInstanceName()).thenReturn(instanceName);
        Mockito.when(resource.getResourceInvariantUUID()).thenReturn(invariantResourceUuid);
        BlueprintArtifact blueprintArtifact = Mockito.mock(BlueprintArtifact.class);
        Mockito.when(blueprintArtifact.getDcaeBlueprint())
            .thenReturn(ResourceFileUtil.getResourceAsString(blueprintFilePath));
        Mockito.when(blueprintArtifact.getBlueprintArtifactName()).thenReturn(artifactName);
        Mockito.when(blueprintArtifact.getBlueprintInvariantServiceUuid()).thenReturn(invariantServiceUuid);
        Mockito.when(blueprintArtifact.getResourceAttached()).thenReturn(resource);
        return blueprintArtifact;
    }

    private CsarHandler buildFakeCsarHandler(String generatedName) throws IOException {
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
        BlueprintArtifact blueprintArtifact = buildFakeBuildprintArtifact(RESOURCE_INSTANCE_NAME_RESOURCE1,
            INVARIANT_RESOURCE1_UUID, "example/sdc/blueprint-dcae/tca.yaml", "tca.yaml", INVARIANT_SERVICE_UUID);
        listResources.add(blueprintArtifact.getResourceAttached());
        blueprintMap.put(blueprintArtifact.getBlueprintArtifactName(), blueprintArtifact);
        // Create fake blueprint artifact 2 on resource2
        blueprintArtifact = buildFakeBuildprintArtifact(RESOURCE_INSTANCE_NAME_RESOURCE2, INVARIANT_RESOURCE2_UUID,
            "example/sdc/blueprint-dcae/tca_2.yaml", "tca_2.yaml", INVARIANT_SERVICE_UUID);
        listResources.add(blueprintArtifact.getResourceAttached());
        blueprintMap.put(blueprintArtifact.getBlueprintArtifactName(), blueprintArtifact);

        // Create fake blueprint artifact 3 on resource 1 so that it's possible to
        // test multiple CL deployment per Service/vnf
        blueprintArtifact = buildFakeBuildprintArtifact(RESOURCE_INSTANCE_NAME_RESOURCE1, INVARIANT_RESOURCE1_UUID,
            "example/sdc/blueprint-dcae/tca_3.yaml", "tca_3.yaml", INVARIANT_SERVICE_UUID);
        blueprintMap.put(blueprintArtifact.getBlueprintArtifactName(), blueprintArtifact);

        // Build fake csarhandler
        Mockito.when(csarHandler.getSdcNotification()).thenReturn(notificationData);
        // Build fake csar Helper
        ISdcCsarHelper csarHelper = Mockito.mock(ISdcCsarHelper.class);
        Metadata data = Mockito.mock(Metadata.class);
        Mockito.when(data.getValue("name")).thenReturn(generatedName);
        Mockito.when(csarHelper.getServiceMetadata()).thenReturn(data);
        Mockito.when(csarHandler.getSdcCsarHelper()).thenReturn(csarHelper);
        return csarHandler;
    }

    @Test
    public void testIsCsarAlreadyDeployedTca() throws SdcArtifactInstallerException, SdcToscaParserException,
        CsarHandlerException, IOException, InterruptedException, PolicyModelException {
        String generatedName = RandomStringUtils.randomAlphanumeric(5);
        CsarHandler csarHandler = buildFakeCsarHandler(generatedName);
        assertFalse(csarInstaller.isCsarAlreadyDeployed(csarHandler));
        csarInstaller.installTheCsar(csarHandler);
        assertTrue(csarInstaller.isCsarAlreadyDeployed(csarHandler));
    }

    @Test
    public void testInstallTheCsarTca() throws SdcArtifactInstallerException, SdcToscaParserException,
        CsarHandlerException, IOException, JSONException, InterruptedException, PolicyModelException {
        String generatedName = RandomStringUtils.randomAlphanumeric(5);
        CsarHandler csar = buildFakeCsarHandler(generatedName);
        csarInstaller.installTheCsar(csar);
        CldsModel cldsModel1 = verifyClosedLoopModelLoadedInDb(csar, "tca.yaml");
        JSONAssert.assertEquals(
            IOUtils.toString(ResourceFileUtil.getResourceAsStream("example/sdc/blueprint-dcae/prop-text-for-tca.json"),
                StandardCharsets.UTF_8),
            cldsModel1.getPropText(), true);
        CldsModel cldsModel2 = verifyClosedLoopModelLoadedInDb(csar, "tca_2.yaml");
        JSONAssert.assertEquals(IOUtils.toString(
            ResourceFileUtil.getResourceAsStream("example/sdc/blueprint-dcae/prop-text-for-tca-2.json"),
            StandardCharsets.UTF_8), cldsModel2.getPropText(), true);
        CldsModel cldsModel3 = verifyClosedLoopModelLoadedInDb(csar, "tca_3.yaml");
        JSONAssert.assertEquals(
            IOUtils.toString(ResourceFileUtil.getResourceAsStream("example/sdc/blueprint-dcae/prop-text-for-tca.json"),
                StandardCharsets.UTF_8),
            cldsModel3.getPropText(), true);
    }

    private CldsModel verifyClosedLoopModelLoadedInDb(CsarHandler csar, String artifactName)
        throws SdcArtifactInstallerException {

        // Get the template back from DB
        CldsTemplate templateFromDb = CldsTemplate.retrieve(cldsDao, CsarInstallerImpl.TEMPLATE_NAME_PREFIX
            + CsarInstallerImpl.buildModelName(csar, csar.getMapOfBlueprints().get(artifactName)), false);
        assertNotNull(templateFromDb);
        assertNotNull(templateFromDb.getBpmnText());
        assertNotNull(templateFromDb.getImageText());
        assertNotNull(templateFromDb.getPropText());
        assertTrue(templateFromDb.getPropText().contains("global")
            && templateFromDb.getPropText().contains("node_templates:"));
        assertEquals(templateFromDb.getName(), CsarInstallerImpl.TEMPLATE_NAME_PREFIX
            + CsarInstallerImpl.buildModelName(csar, csar.getMapOfBlueprints().get(artifactName)));
        // Get the Model back from DB
        CldsModel modelFromDb = CldsModel.retrieve(cldsDao,
            CsarInstallerImpl.buildModelName(csar, csar.getMapOfBlueprints().get(artifactName)), true);
        assertNotNull(modelFromDb);
        assertNotNull(modelFromDb.getBpmnText());
        assertNotNull(modelFromDb.getImageText());
        assertNotNull(modelFromDb.getPropText());
        assertTrue(modelFromDb.getPropText().contains("policy_id"));
        assertEquals(CsarInstallerImpl.buildModelName(csar, csar.getMapOfBlueprints().get(artifactName)),
            modelFromDb.getName());
        assertEquals(CsarInstallerImpl.CONTROL_NAME_PREFIX, modelFromDb.getControlNamePrefix());
        return modelFromDb;
    }
}
