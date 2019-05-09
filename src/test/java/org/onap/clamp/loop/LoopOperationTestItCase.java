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

package org.onap.clamp.loop;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.HashSet;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.clamp.clds.Application;
import org.onap.clamp.loop.LoopOperation.TempLoopState;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class LoopOperationTestItCase {

    private Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    @Autowired
    LoopService loopService;

    private Loop createTestLoop() {
        String yaml = "imports:\n"
            + "  - \"http://www.getcloudify.org/spec/cloudify/3.4/types.yaml\"\n"
            + "node_templates:\n"
            + "  docker_service_host:\n"
            + "    type: dcae.nodes.SelectedDockerHost";

        Loop loopTest = new Loop("ControlLoopTest", yaml, "<xml></xml>");
        loopTest.setGlobalPropertiesJson(new Gson().fromJson("{\"dcaeDeployParameters\":"
                + "{\"policy_id\": \"name\"}}", JsonObject.class));
        loopTest.setLastComputedState(LoopState.DESIGN);
        loopTest.setDcaeDeploymentId("123456789");
        loopTest.setDcaeDeploymentStatusUrl("http4://localhost:8085");
        loopTest.setDcaeBlueprintId("UUID-blueprint");

        MicroServicePolicy microServicePolicy = new MicroServicePolicy("configPolicyTest", "",
            "tosca_definitions_version: tosca_simple_yaml_1_0_0", true,
             gson.fromJson("{\"configtype\":\"json\"}", JsonObject.class), new HashSet<>());
        microServicePolicy.setProperties(new Gson().fromJson("{\"param1\":\"value1\"}", JsonObject.class));

        loopTest.addMicroServicePolicy(microServicePolicy);
        return loopTest;
    }


    @Test
    public void testAnalysePolicyResponse() {
        LoopOperation loopOp = new LoopOperation(loopService);
        String status1 = loopOp.analysePolicyResponse(200);
        String status2 = loopOp.analysePolicyResponse(404);
        String status3 = loopOp.analysePolicyResponse(500);
        String status4 = loopOp.analysePolicyResponse(503);

        // then
        assertThat(status1).isEqualTo("SUBMITTED");
        assertThat(status2).isEqualTo("NOT_SUBMITTED");
        assertThat(status3).isEqualTo("IN_ERROR");
        assertThat(status4).isEqualTo("IN_ERROR");
    }

    @Test
    public void testGetOperationalPolicyName() {
        LoopOperation loopOp = new LoopOperation(loopService);
        Loop loop = this.createTestLoop();
        String opName1 = loopOp.getOperationalPolicyName(loop);
        assertThat(opName1).isNull();

        OperationalPolicy opPolicy1 = new OperationalPolicy("OperationalPolicyTest1", null,
                gson.fromJson("{\"type\":\"Operational\"}", JsonObject.class));
        loop.addOperationalPolicy(opPolicy1);
        String opName2 = loopOp.getOperationalPolicyName(loop);
        assertThat(opName2).isEqualTo("OperationalPolicyTest1");
    }

    @Test
    public void testAnalyseDcaeResponse() throws ParseException {
        LoopOperation loopOp = new LoopOperation(loopService);
        String dcaeStatus1 = loopOp.analyseDcaeResponse(null, null);
        assertThat(dcaeStatus1).isEqualTo("NOT_DEPLOYED");

        String dcaeStatus2 = loopOp.analyseDcaeResponse(null, 500);
        assertThat(dcaeStatus2).isEqualTo("IN_ERROR");

        String dcaeStatus3 = loopOp.analyseDcaeResponse(null, 404);
        assertThat(dcaeStatus3).isEqualTo("NOT_DEPLOYED");

        Exchange camelExchange = Mockito.mock(Exchange.class);
        Message mockMessage = Mockito.mock(Message.class);
        Mockito.when(camelExchange.getIn()).thenReturn(mockMessage);
        Mockito.when(mockMessage.getBody(String.class))
            .thenReturn("{\"operationType\":\"install\",\"status\":\"succeeded\"}");
        String dcaeStatus4 = loopOp.analyseDcaeResponse(camelExchange, 200);
        assertThat(dcaeStatus4).isEqualTo("DEPLOYED");

        Mockito.when(mockMessage.getBody(String.class))
            .thenReturn("{\"operationType\":\"install\",\"status\":\"processing\"}");
        String dcaeStatus5 = loopOp.analyseDcaeResponse(camelExchange, 200);
        assertThat(dcaeStatus5).isEqualTo("PROCESSING");

        Mockito.when(mockMessage.getBody(String.class))
            .thenReturn("{\"operationType\":\"install\",\"status\":\"failed\"}");
        String dcaeStatus6 = loopOp.analyseDcaeResponse(camelExchange, 200);
        assertThat(dcaeStatus6).isEqualTo("IN_ERROR");

        Mockito.when(mockMessage.getBody(String.class))
            .thenReturn("{\"operationType\":\"uninstall\",\"status\":\"succeeded\"}");
        String dcaeStatus7 = loopOp.analyseDcaeResponse(camelExchange, 200);
        assertThat(dcaeStatus7).isEqualTo("NOT_DEPLOYED");

        Mockito.when(mockMessage.getBody(String.class))
            .thenReturn("{\"operationType\":\"uninstall\",\"status\":\"processing\"}");
        String dcaeStatus8 = loopOp.analyseDcaeResponse(camelExchange, 200);
        assertThat(dcaeStatus8).isEqualTo("PROCESSING");

        Mockito.when(mockMessage.getBody(String.class))
            .thenReturn("{\"operationType\":\"uninstall\",\"status\":\"failed\"}");
        String dcaeStatus9 = loopOp.analyseDcaeResponse(camelExchange, 200);
        assertThat(dcaeStatus9).isEqualTo("IN_ERROR");
    }

    @Test
    public void testUpdateLoopStatus() {
        LoopOperation loopOp = new LoopOperation(loopService);
        Loop loop = this.createTestLoop();
        loopService.saveOrUpdateLoop(loop);
        LoopState newState1 = loopOp.updateLoopStatus(loop, TempLoopState.SUBMITTED, TempLoopState.DEPLOYED);
        LoopState dbState1 = loopService.getLoop(loop.getName()).getLastComputedState();
        assertThat(newState1).isEqualTo(LoopState.DEPLOYED);
        assertThat(dbState1).isEqualTo(LoopState.DEPLOYED);

        LoopState newState2 = loopOp.updateLoopStatus(loop, TempLoopState.SUBMITTED, TempLoopState.NOT_DEPLOYED);
        LoopState dbState2 = loopService.getLoop(loop.getName()).getLastComputedState();
        assertThat(newState2).isEqualTo(LoopState.SUBMITTED);
        assertThat(dbState2).isEqualTo(LoopState.SUBMITTED);

        LoopState newState3 = loopOp.updateLoopStatus(loop, TempLoopState.SUBMITTED, TempLoopState.PROCESSING);
        assertThat(newState3).isEqualTo(LoopState.WAITING);

        LoopState newState4 = loopOp.updateLoopStatus(loop, TempLoopState.SUBMITTED, TempLoopState.IN_ERROR);
        assertThat(newState4).isEqualTo(LoopState.IN_ERROR);

        LoopState newState5 = loopOp.updateLoopStatus(loop, TempLoopState.NOT_SUBMITTED, TempLoopState.DEPLOYED);
        assertThat(newState5).isEqualTo(LoopState.IN_ERROR);

        LoopState newState6 = loopOp.updateLoopStatus(loop, TempLoopState.NOT_SUBMITTED, TempLoopState.PROCESSING);
        assertThat(newState6).isEqualTo(LoopState.IN_ERROR);

        LoopState newState7 = loopOp.updateLoopStatus(loop, TempLoopState.NOT_SUBMITTED, TempLoopState.NOT_DEPLOYED);
        assertThat(newState7).isEqualTo(LoopState.DESIGN);

        LoopState newState8 = loopOp.updateLoopStatus(loop, TempLoopState.IN_ERROR, TempLoopState.DEPLOYED);
        assertThat(newState8).isEqualTo(LoopState.IN_ERROR);

        LoopState newState9 = loopOp.updateLoopStatus(loop, TempLoopState.IN_ERROR, TempLoopState.NOT_DEPLOYED);
        assertThat(newState9).isEqualTo(LoopState.IN_ERROR);

        LoopState newState10 = loopOp.updateLoopStatus(loop, TempLoopState.IN_ERROR, TempLoopState.PROCESSING);
        assertThat(newState10).isEqualTo(LoopState.IN_ERROR);

        LoopState newState11 = loopOp.updateLoopStatus(loop, TempLoopState.IN_ERROR, TempLoopState.IN_ERROR);
        assertThat(newState11).isEqualTo(LoopState.IN_ERROR);
    }

    @Test
    public void testUpdateLoopInfo() throws ParseException {
        Loop loop = this.createTestLoop();
        loopService.saveOrUpdateLoop(loop);

        Exchange camelExchange = Mockito.mock(Exchange.class);
        Message mockMessage = Mockito.mock(Message.class);
        Mockito.when(camelExchange.getIn()).thenReturn(mockMessage);
        Mockito.when(mockMessage.getBody(String.class))
            .thenReturn("{\"links\":{\"status\":\"http://testhost/dcae-operationstatus\",\"test2\":\"test2\"}}");

        LoopOperation loopOp = new LoopOperation(loopService);
        loopOp.updateLoopInfo(camelExchange, loop, "testNewId");

        Loop newLoop = loopService.getLoop(loop.getName());
        String newDeployId =  newLoop.getDcaeDeploymentId();
        String newDeploymentStatusUrl = newLoop.getDcaeDeploymentStatusUrl();

        assertThat(newDeployId).isEqualTo("testNewId");
        assertThat(newDeploymentStatusUrl).isEqualTo("http4://testhost/dcae-operationstatus");
    }

    @Test
    public void testGetDeploymentId() {
        Loop loop = this.createTestLoop();
        LoopOperation loopOp = new LoopOperation(loopService);
        String deploymentId1 = loopOp.getDeploymentId(loop);
        assertThat(deploymentId1).isEqualTo("123456789");

        loop.setDcaeDeploymentId(null);
        String deploymentId2 = loopOp.getDeploymentId(loop);
        assertThat(deploymentId2).isEqualTo("closedLoop_ControlLoopTest_deploymentId");

        loop.setDcaeDeploymentId("");
        String deploymentId3 = loopOp.getDeploymentId(loop);
        assertThat(deploymentId3).isEqualTo("closedLoop_ControlLoopTest_deploymentId");
    }

    @Test
    public void testGetDeployPayload() throws IOException {
        Loop loop = this.createTestLoop();
        LoopOperation loopOp = new LoopOperation(loopService);
        String deploymentPayload = loopOp.getDeployPayload(loop);

        String expectedPayload = "{\"serviceTypeId\":\"UUID-blueprint\",\"inputs\":{\"policy_id\":\"name\"}}";
        assertThat(deploymentPayload).isEqualTo(expectedPayload);
    }
}