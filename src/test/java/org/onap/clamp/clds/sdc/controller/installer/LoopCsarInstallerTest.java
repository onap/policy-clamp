/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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
 *  * Modifications copyright (c) 2019 Nokia
 * ===================================================================
 *
 */

package org.onap.clamp.clds.sdc.controller.installer;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.clds.client.DcaeInventoryServices;
import org.onap.clamp.clds.config.sdc.BlueprintParserFilesConfiguration;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.clamp.clds.service.CldsService;
import org.onap.clamp.clds.service.CldsTemplateService;
import org.onap.clamp.clds.transform.XslTransformer;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.toscaparser.api.elements.Metadata;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

@RunWith(MockitoJUnitRunner.class)
public class LoopCsarInstallerTest {

    @Mock
    private CsarHandler csarHandler;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private DcaeInventoryServices dcaeInventoryServices;

    @Mock
    private IResourceInstance resourceInstance;

    @Mock
    private CldsService cldsService;

    @Mock
    private INotificationData notificationData;

    @Mock
    private Metadata metadata;

    @Mock
    private ISdcCsarHelper sdcCsarHelper;

    private CsarInstallerImpl csarInstaller;
    private BlueprintArtifact artifact;

    /**
     * Set up method. throws: Exception
     */
    @Before
    public void setUp() throws Exception {
        String dceaBlueprint = ResourceFileUtil.getResourceAsString("tosca/dcea_blueprint.yml");
        artifact = prepareBlueprintArtifact(dceaBlueprint);
        csarInstaller = new CsarInstallerImpl(applicationContext, null, new CldsTemplateService(), cldsService,
            dcaeInventoryServices, new XslTransformer());
    }

    @Test
    public void shouldReturnInputParametersFromBlueprint() {
        // given
        String expectedBlueprintInputsText = "{\"aaiEnrichmentHost\":\"aai.onap.svc.cluster.local\""
            + ",\"aaiEnrichmentPort\":\"8443\"" + ",\"enableAAIEnrichment\":true" + ",\"dmaap_host\":\"message-router\""
            + ",\"dmaap_port\":\"3904\"" + ",\"enableRedisCaching\":false" + ",\"redisHosts\":\"dcae-redis:6379\""
            + ",\"tag_version\":"
            + "\"nexus3.onap.org:10001/onap/org.onap.dcaegen2.deployments.tca-cdap-container:1.1.0\""
            + ",\"consul_host\":\"consul-server\"" + ",\"consul_port\":\"8500\",\"cbs_host\":\"{\\\"test\\\":"
            + "{\\\"test\\\":\\\"test\\\"}}\",\"cbs_port\":\"10000\""
            + ",\"external_port\":\"32010\",\"policy_id\":\"AUTO_GENERATED_POLICY_ID_AT_SUBMIT\"}";

        JsonObject expectedBlueprintInputs = JsonUtils.GSON.fromJson(expectedBlueprintInputsText, JsonObject.class);
        // when
        String parametersInJson = csarInstaller.getAllBlueprintParametersInJson(artifact);
        // then
        Assertions.assertThat(JsonUtils.GSON.fromJson(parametersInJson, JsonObject.class))
            .isEqualTo(expectedBlueprintInputs);
    }

    @Test
    public void shouldReturnBuildModelName() throws SdcArtifactInstallerException {
        // given
        String expectedModelName = "CLAMP_test_name_" + "vtest_service_version_" + "test_resource_instance_name_"
            + "test_artifact_name";
        prepareMockCsarHandler("name", "test_name", "test_service_version");
        Mockito.when(resourceInstance.getResourceInstanceName()).thenReturn("test_resource_instance_name");
        // when
        String actualModelName = CsarInstallerImpl.buildModelName(csarHandler, artifact);
        // then
        Assertions.assertThat(actualModelName).isEqualTo(expectedModelName);
    }

    @Test
    public void shouldReturnRightMapping() throws SdcArtifactInstallerException, IOException {
        // given
        String input = "[{\"blueprintKey\":\"tca_k8s\"," + "\"dcaeDeployable\":false,"
            + "\"files\":{\"svgXmlFilePath\":\"samplePath\",\"bpmnXmlFilePath\":\"samplePath\"}}]";
        BlueprintParserFilesConfiguration filesConfiguration = new BlueprintParserFilesConfiguration();
        filesConfiguration.setBpmnXmlFilePath("samplePath");
        filesConfiguration.setSvgXmlFilePath("samplePath");
        Resource resource = Mockito.mock(Resource.class);
        InputStream inputStream = IOUtils.toInputStream(input, "UTF-8");
        Mockito.when(applicationContext.getResource(Mockito.any(String.class))).thenReturn(resource);
        Mockito.when(resource.getInputStream()).thenReturn(inputStream);
        csarInstaller.loadConfiguration();
        // when
        BlueprintParserFilesConfiguration configuration = csarInstaller.searchForRightMapping(artifact);

        // then
        Assertions.assertThat(configuration.getBpmnXmlFilePath()).isEqualTo("samplePath");
        Assertions.assertThat(configuration.getSvgXmlFilePath()).isEqualTo("samplePath");
    }

    private BlueprintArtifact prepareBlueprintArtifact(String dceaBlueprint) {
        artifact = new BlueprintArtifact();
        artifact.setBlueprintArtifactName("test_artifact_name");
        artifact.setBlueprintInvariantServiceUuid("test_inv_uuid");
        artifact.setResourceAttached(resourceInstance);
        artifact.setDcaeBlueprint(dceaBlueprint);
        return artifact;
    }

    private void prepareMockCsarHandler(String metadataNameMockInput, String metadataNameMockOutput,
        String serviceVersion) {
        Mockito.when(csarHandler.getSdcCsarHelper()).thenReturn(sdcCsarHelper);
        Mockito.when(sdcCsarHelper.getServiceMetadata()).thenReturn(metadata);
        Mockito.when(metadata.getValue(metadataNameMockInput)).thenReturn(metadataNameMockOutput);
        Mockito.when(csarHandler.getSdcNotification()).thenReturn(notificationData);
        Mockito.when(notificationData.getServiceVersion()).thenReturn(serviceVersion);
    }
}