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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.sdc.controller.installer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.att.aft.dme2.internal.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.onap.clamp.clds.exception.sdc.controller.CsarHandlerException;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;
import org.onap.sdc.api.results.IDistributionClientDownloadResult;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;

public class CsarHandlerTest {

    private static final String SDC_FOLDER = "/tmp/csar-handler-tests";
    private static final String CSAR_ARTIFACT_NAME = "testArtifact.csar";
    private static final String SERVICE_UUID = "serviceUUID";
    private static final String RESOURCE1_UUID = "resource1UUID";
    private static final String RESOURCE1_INSTANCE_NAME = "sim-1802 0";
    private static final String RESOURCE1_INSTANCE_NAME_IN_CSAR = "sim18020";
    private static final String BLUEPRINT1_NAME = "FOI.Simfoimap223S0112.event_proc_bp.yaml";

    @Test
    public void testConstructor() throws CsarHandlerException {
        IArtifactInfo serviceArtifact = Mockito.mock(IArtifactInfo.class);
        Mockito.when(serviceArtifact.getArtifactType()).thenReturn(CsarHandler.CSAR_TYPE);
        Mockito.when(serviceArtifact.getArtifactName()).thenReturn(CSAR_ARTIFACT_NAME);
        List<IArtifactInfo> servicesList = new ArrayList<>();
        servicesList.add(serviceArtifact);
        INotificationData notifData = Mockito.mock(INotificationData.class);
        Mockito.when(notifData.getServiceArtifacts()).thenReturn(servicesList);
        CsarHandler csar = new CsarHandler(notifData, "test-controller", SDC_FOLDER);
        assertEquals(SDC_FOLDER + "/test-controller" + "/" + CSAR_ARTIFACT_NAME, csar.getFilePath());
    }

    @Test(expected = CsarHandlerException.class)
    public void testFailingConstructor() throws CsarHandlerException {
        INotificationData notifData = Mockito.mock(INotificationData.class);
        Mockito.when(notifData.getServiceArtifacts()).thenReturn(new ArrayList<>());
        new CsarHandler(notifData, "test-controller", "/tmp/csar-handler-tests");
        fail("Exception should have been raised");
    }

    private INotificationData buildFakeSdcNotification() {
        // BUild what is needed for CSAR
        IArtifactInfo serviceArtifact = Mockito.mock(IArtifactInfo.class);
        Mockito.when(serviceArtifact.getArtifactType()).thenReturn(CsarHandler.CSAR_TYPE);
        Mockito.when(serviceArtifact.getArtifactName()).thenReturn(CSAR_ARTIFACT_NAME);
        List<IArtifactInfo> servicesList = new ArrayList<>();
        servicesList.add(serviceArtifact);
        INotificationData notifData = Mockito.mock(INotificationData.class);
        Mockito.when(notifData.getServiceArtifacts()).thenReturn(servicesList);
        // Build what is needed for UUID
        Mockito.when(notifData.getServiceInvariantUUID()).thenReturn(SERVICE_UUID);
        // Build fake resource with one artifact BLUEPRINT
        IResourceInstance resource1 = Mockito.mock(IResourceInstance.class);
        Mockito.when(resource1.getResourceType()).thenReturn("VF");
        Mockito.when(resource1.getResourceInvariantUUID()).thenReturn(RESOURCE1_UUID);
        Mockito.when(resource1.getResourceInstanceName()).thenReturn(RESOURCE1_INSTANCE_NAME);
        // Create a fake artifact for resource
        IArtifactInfo blueprintArtifact = Mockito.mock(IArtifactInfo.class);
        Mockito.when(blueprintArtifact.getArtifactType()).thenReturn(CsarHandler.BLUEPRINT_TYPE);
        List<IArtifactInfo> artifactsListForResource = new ArrayList<>();
        artifactsListForResource.add(blueprintArtifact);
        Mockito.when(resource1.getArtifacts()).thenReturn(artifactsListForResource);
        List<IResourceInstance> resourcesList = new ArrayList<>();
        resourcesList.add(resource1);
        Mockito.when(notifData.getResources()).thenReturn(resourcesList);
        return notifData;
    }

    private IDistributionClientDownloadResult buildFakeSdcResut() throws IOException {
        IDistributionClientDownloadResult resultArtifact = Mockito.mock(IDistributionClientDownloadResult.class);
        Mockito.when(resultArtifact.getArtifactPayload()).thenReturn(
                IOUtils.toByteArray(ResourceFileUtil.getResourceAsStream("example/sdc/service-Simsfoimap0112.csar")));
        return resultArtifact;
    }

    @Test
    public void testSave()
            throws SdcArtifactInstallerException, SdcToscaParserException, CsarHandlerException, IOException {
        CsarHandler csar = new CsarHandler(buildFakeSdcNotification(), "test-controller", "/tmp/csar-handler-tests");
        // Test the save
        csar.save(buildFakeSdcResut());
        assertTrue((new File(SDC_FOLDER + "/test-controller/" + CSAR_ARTIFACT_NAME)).exists());
        assertEquals(CSAR_ARTIFACT_NAME, csar.getArtifactElement().getArtifactName());
        assertNotNull(csar.getSdcCsarHelper());
        // Test dcaeBlueprint
        String blueprint = csar.getMapOfBlueprints().get(RESOURCE1_INSTANCE_NAME).getDcaeBlueprint();
        assertNotNull(blueprint);
        assertTrue(!blueprint.isEmpty());
        assertTrue(blueprint.contains("DCAE-VES-PM-EVENT-v1"));
        // Test additional properties from Sdc notif
        assertEquals(BLUEPRINT1_NAME,
                csar.getMapOfBlueprints().get(RESOURCE1_INSTANCE_NAME).getBlueprintArtifactName());
        assertEquals(RESOURCE1_UUID,
                csar.getMapOfBlueprints().get(RESOURCE1_INSTANCE_NAME).getBlueprintInvariantResourceUuid());
        assertEquals(SERVICE_UUID,
                csar.getMapOfBlueprints().get(RESOURCE1_INSTANCE_NAME).getBlueprintInvariantServiceUuid());
        // Do some cleanup
        Path path = Paths.get(SDC_FOLDER + "/test-controller/" + CSAR_ARTIFACT_NAME);
        Files.deleteIfExists(path);
    }

    @Test
    public void testDoubleSave()
            throws SdcArtifactInstallerException, SdcToscaParserException, CsarHandlerException, IOException {
        CsarHandler csar = new CsarHandler(buildFakeSdcNotification(), "test-controller", "/tmp/csar-handler-tests");
        // Test the save
        csar.save(buildFakeSdcResut());
        assertTrue((new File(SDC_FOLDER + "/test-controller/" + CSAR_ARTIFACT_NAME)).exists());
        assertEquals(CSAR_ARTIFACT_NAME, csar.getArtifactElement().getArtifactName());
        assertNotNull(csar.getSdcCsarHelper());
        // Test dcaeBlueprint
        String blueprint = csar.getMapOfBlueprints().get(RESOURCE1_INSTANCE_NAME).getDcaeBlueprint();
        assertNotNull(blueprint);
        assertTrue(!blueprint.isEmpty());
        assertTrue(blueprint.contains("DCAE-VES-PM-EVENT-v1"));
        // Test additional properties from Sdc notif
        assertEquals(BLUEPRINT1_NAME,
                csar.getMapOfBlueprints().get(RESOURCE1_INSTANCE_NAME).getBlueprintArtifactName());
        assertEquals(RESOURCE1_UUID,
                csar.getMapOfBlueprints().get(RESOURCE1_INSTANCE_NAME).getBlueprintInvariantResourceUuid());
        assertEquals(SERVICE_UUID,
                csar.getMapOfBlueprints().get(RESOURCE1_INSTANCE_NAME).getBlueprintInvariantServiceUuid());
        Path path = Paths.get(SDC_FOLDER + "/test-controller/" + CSAR_ARTIFACT_NAME);
        // A double save should simply overwrite the existing
        csar.save(buildFakeSdcResut());
        // Do some cleanup
        Files.deleteIfExists(path);
    }
}
