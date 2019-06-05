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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashSet;

import org.junit.Test;
import org.onap.clamp.clds.model.dcae.DcaeOperationStatusResponse;
import org.onap.clamp.loop.components.external.DcaeComponent;
import org.onap.clamp.policy.microservice.MicroServicePolicy;

public class DcaeComponentTest {

    private Loop createTestLoop() {
        String yaml = "imports:\n" + "  - \"http://www.getcloudify.org/spec/cloudify/3.4/types.yaml\"\n"
            + "node_templates:\n" + "  docker_service_host:\n" + "    type: dcae.nodes.SelectedDockerHost";

        Loop loopTest = new Loop("ControlLoopTest", yaml, "<xml></xml>");
        loopTest.setGlobalPropertiesJson(
            new Gson().fromJson("{\"dcaeDeployParameters\":" + "{\"policy_id\": \"name\"}}", JsonObject.class));
        loopTest.setLastComputedState(LoopState.DESIGN);
        loopTest.setDcaeDeploymentId("123456789");
        loopTest.setDcaeDeploymentStatusUrl("http4://localhost:8085");
        loopTest.setDcaeBlueprintId("UUID-blueprint");

        MicroServicePolicy microServicePolicy = new MicroServicePolicy("configPolicyTest", "",
            "tosca_definitions_version: tosca_simple_yaml_1_0_0", true,
            new Gson().fromJson("{\"configtype\":\"json\"}", JsonObject.class), new HashSet<>());
        microServicePolicy.setProperties(new Gson().fromJson("{\"param1\":\"value1\"}", JsonObject.class));

        loopTest.addMicroServicePolicy(microServicePolicy);
        return loopTest;
    }

    @Test
    public void convertDcaeResponseTest() throws IOException {
        String dcaeFakeResponse = "{'requestId':'testId','operationType':'install','status':'state','error':'errorMessage', 'links':{'self':'selfUrl','uninstall':'uninstallUrl'}}";
        DcaeOperationStatusResponse responseObject = DcaeComponent.convertDcaeResponse(dcaeFakeResponse);
        assertThat(responseObject.getRequestId()).isEqualTo("testId");
        assertThat(responseObject.getOperationType()).isEqualTo("install");
        assertThat(responseObject.getStatus()).isEqualTo("state");
        assertThat(responseObject.getError()).isEqualTo("errorMessage");
        assertThat(responseObject.getLinks()).isNotNull();
        assertThat(responseObject.getLinks().getSelf()).isEqualTo("selfUrl");
        assertThat(responseObject.getLinks().getUninstall()).isEqualTo("uninstallUrl");

        assertThat(responseObject.getLinks().getStatus()).isNull();
    }

    @Test
    public void testGetDeployPayload() throws IOException {
        Loop loop = this.createTestLoop();
        String deploymentPayload = DcaeComponent.getDeployPayload(loop);
        String expectedPayload = "{\"serviceTypeId\":\"UUID-blueprint\",\"inputs\":{\"policy_id\":\"name\"}}";
        assertThat(deploymentPayload).isEqualTo(expectedPayload);
    }

    @Test
    public void testGetUndeployPayload() throws IOException {
        Loop loop = this.createTestLoop();
        String unDeploymentPayload = DcaeComponent.getUndeployPayload(loop);
        String expectedPayload = "{\"serviceTypeId\":\"UUID-blueprint\"}";
        assertThat(unDeploymentPayload).isEqualTo(expectedPayload);
    }

}
