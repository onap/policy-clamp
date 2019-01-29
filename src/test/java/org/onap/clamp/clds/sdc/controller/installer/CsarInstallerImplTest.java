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
 * ===================================================================
 *
 */

package org.onap.clamp.clds.sdc.controller.installer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonObject;
import java.io.IOException;
import org.junit.Test;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.ResourceFileUtil;

public class CsarInstallerImplTest {

    @Test
    public void shouldReturnInputParametersFromBlueprint() throws IOException {
        //given
        String expectedBlueprintInputsText = "{\"aaiEnrichmentHost\":\"aai.onap.svc.cluster.local\""
            + ",\"aaiEnrichmentPort\":\"8443\""
            + ",\"enableAAIEnrichment\":true"
            + ",\"dmaap_host\":\"message-router\""
            + ",\"dmaap_port\":\"3904\""
            + ",\"enableRedisCaching\":false"
            + ",\"redisHosts\":\"dcae-redis:6379\""
            + ",\"tag_version\":\"nexus3.onap.org:10001/onap/org.onap.dcaegen2.deployments.tca-cdap-container:1.1.0\""
            + ",\"consul_host\":\"consul-server\""
            + ",\"consul_port\":\"8500\",\"cbs_host\":\"{\\\"test\\\":"
            + "{\\\"test\\\":\\\"test\\\"}}\",\"cbs_port\":\"10000\""
            + ",\"external_port\":\"32010\",\"policy_id\":\"AUTO_GENERATED_POLICY_ID_AT_SUBMIT\"}";

        JsonObject expectedBlueprintInputs = JsonUtils.GSON.fromJson(expectedBlueprintInputsText, JsonObject.class);
        String dceaBlueprint = ResourceFileUtil.getResourceAsString("tosca/dcea_blueprint.yml");
        BlueprintArtifact blueprintArtifact = mock(BlueprintArtifact.class);
        when(blueprintArtifact.getDcaeBlueprint()).thenReturn(dceaBlueprint);
        CsarInstallerImpl csarInstaller = new CsarInstallerImpl();

        //when
        String parametersInJson = csarInstaller.getAllBlueprintParametersInJson(blueprintArtifact);

        //then
        assertThat(JsonUtils.GSON.fromJson(parametersInJson, JsonObject.class)).isEqualTo(expectedBlueprintInputs);
    }
}