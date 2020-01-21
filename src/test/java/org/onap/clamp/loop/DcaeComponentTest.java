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

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.clamp.clds.model.dcae.DcaeOperationStatusResponse;
import org.onap.clamp.loop.components.external.DcaeComponent;
import org.onap.clamp.loop.components.external.ExternalComponentState;
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
        microServicePolicy.setConfigurationsJson(new Gson().fromJson("{\"param1\":\"value1\"}", JsonObject.class));

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

    @Test
    public void computeStateTest() throws IOException {
        Exchange exchange = Mockito.mock(Exchange.class);
        Message message = Mockito.mock(Message.class);
        Exchange exchange2 = Mockito.mock(Exchange.class);
        Mockito.when(exchange.getIn()).thenReturn(message);
        Mockito.when(message.getExchange()).thenReturn(exchange2);
        Mockito.when(exchange2.getProperty("dcaeResponse")).thenReturn(null);

        DcaeComponent dcae = new DcaeComponent();

        // initial state
        ExternalComponentState state = dcae.computeState(exchange);
        assertThat(state.getStateName()).isEqualTo("BLUEPRINT_DEPLOYED");

        // OperationalType = install
        DcaeOperationStatusResponse dcaeResponse = Mockito.mock(DcaeOperationStatusResponse.class); 
        Mockito.when(dcaeResponse.getOperationType()).thenReturn("install");

        Mockito.when(dcaeResponse.getStatus()).thenReturn("succeeded");
        Mockito.when(exchange2.getProperty("dcaeResponse")).thenReturn(dcaeResponse);
        ExternalComponentState state2 = dcae.computeState(exchange);
        assertThat(state2.getStateName()).isEqualTo("MICROSERVICE_INSTALLED_SUCCESSFULLY");
        Mockito.when(dcaeResponse.getStatus()).thenReturn("processing");
        ExternalComponentState state3 = dcae.computeState(exchange);
        assertThat(state3.getStateName()).isEqualTo("PROCESSING_MICROSERVICE_INSTALLATION");

        Mockito.when(dcaeResponse.getStatus()).thenReturn("failed");
        ExternalComponentState state4 = dcae.computeState(exchange);
        assertThat(state4.getStateName()).isEqualTo("MICROSERVICE_INSTALLATION_FAILED");

        // OperationalType = uninstall
        Mockito.when(dcaeResponse.getOperationType()).thenReturn("uninstall");

        Mockito.when(dcaeResponse.getStatus()).thenReturn("succeeded");
        Mockito.when(exchange2.getProperty("dcaeResponse")).thenReturn(dcaeResponse);
        ExternalComponentState state5 = dcae.computeState(exchange);
        assertThat(state5.getStateName()).isEqualTo("MICROSERVICE_UNINSTALLED_SUCCESSFULLY");

        Mockito.when(dcaeResponse.getStatus()).thenReturn("processing");
        ExternalComponentState state6 = dcae.computeState(exchange);
        assertThat(state6.getStateName()).isEqualTo("PROCESSING_MICROSERVICE_UNINSTALLATION");

        Mockito.when(dcaeResponse.getStatus()).thenReturn("failed");
        ExternalComponentState state7 = dcae.computeState(exchange);
        assertThat(state7.getStateName()).isEqualTo("MICROSERVICE_UNINSTALLATION_FAILED");

        // error cases
        Mockito.when(dcaeResponse.getOperationType()).thenReturn("whatever");
        ExternalComponentState state8 = dcae.computeState(exchange);
        assertThat(state8.getStateName()).isEqualTo("IN_ERROR");

        Mockito.when(dcaeResponse.getOperationType()).thenReturn("install");
        Mockito.when(dcaeResponse.getStatus()).thenReturn("anythingelse");
        ExternalComponentState state9 = dcae.computeState(exchange);
        assertThat(state9.getStateName()).isEqualTo("IN_ERROR");
    }
}
