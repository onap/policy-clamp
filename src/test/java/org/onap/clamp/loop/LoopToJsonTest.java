/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
 * ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
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
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.Random;
import org.junit.Test;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.ResourceFileUtils;
import org.onap.clamp.loop.log.LogType;
import org.onap.clamp.loop.log.LoopLog;
import org.onap.clamp.loop.service.Service;
import org.onap.clamp.loop.template.LoopElementModel;
import org.onap.clamp.loop.template.LoopTemplate;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;

public class LoopToJsonTest {

    private Gson gson = new Gson();

    private OperationalPolicy getOperationalPolicy(String configJson, String name) {
        return new OperationalPolicy(name, null, gson.fromJson(configJson, JsonObject.class),
                getPolicyModel("org.onap.policy.drools.legacy", "yaml", "1.0.0", "Drools", "type1"), null, null, null);
    }

    private Loop getLoop(String name, String blueprint, String globalPropertiesJson,
                         String dcaeId, String dcaeUrl, String dcaeBlueprintId)
            throws JsonSyntaxException, IOException {
        Loop loop = new Loop(name);
        loop.setGlobalPropertiesJson(new Gson().fromJson(globalPropertiesJson, JsonObject.class));
        loop.setLastComputedState(LoopState.DESIGN);
        loop.setDcaeDeploymentId(dcaeId);
        loop.setDcaeDeploymentStatusUrl(dcaeUrl);
        return loop;
    }

    private MicroServicePolicy getMicroServicePolicy(String name, String modelType, String jsonRepresentation,
                                                     String policyTosca, String jsonProperties, boolean shared) {
        MicroServicePolicy microService = new MicroServicePolicy(name, new PolicyModel(modelType, policyTosca, "1.0.0"),
                shared,
                gson.fromJson(jsonRepresentation, JsonObject.class), null, null, null);
        microService.setConfigurationsJson(new Gson().fromJson(jsonProperties, JsonObject.class));
        return microService;
    }

    private LoopElementModel getLoopElementModel(String yaml, String name, PolicyModel policyModel) {
        LoopElementModel model = new LoopElementModel();
        model.setBlueprint(yaml);
        model.setName(name);
        model.addPolicyModel(policyModel);
        model.setLoopElementType("OPERATIONAL_POLICY");
        return model;
    }

    private PolicyModel getPolicyModel(String policyType, String policyModelTosca, String version, String policyAcronym,
                                       String policyVariant) {
        return new PolicyModel(policyType, policyModelTosca, version, policyAcronym);
    }

    private LoopTemplate getLoopTemplate(String name, String blueprint, Integer maxInstancesAllowed) {
        LoopTemplate template = new LoopTemplate(name, blueprint, maxInstancesAllowed, null);
        template.addLoopElementModel(getLoopElementModel("yaml", "microService1",
                getPolicyModel("org.onap.policy.drools", "yaml", "1.0.0", "Drools", "type1")));
        return template;
    }

    private LoopLog getLoopLog(LogType type, String message, Loop loop) {
        LoopLog log = new LoopLog(message, type, "CLAMP", loop);
        log.setId(Long.valueOf(new Random().nextInt()));
        return log;
    }

    /**
     * This tests a GSON encode/decode.
     *
     * @throws IOException In case of failure
     */
    @Test
    public void loopGsonTest() throws IOException {
        Loop loopTest = getLoop("ControlLoopTest", "yamlcontent", "{\"testname\":\"testvalue\"}",
                "123456789", "https://dcaetest.org", "UUID-blueprint");
        OperationalPolicy opPolicy = this.getOperationalPolicy(
                ResourceFileUtils.getResourceAsString("tosca/operational-policy-properties.json"), "GuardOpPolicyTest");
        loopTest.addOperationalPolicy(opPolicy);
        MicroServicePolicy microServicePolicy = getMicroServicePolicy("configPolicyTest", "",
                "{\"configtype\":\"json\"}", "tosca_definitions_version: tosca_simple_yaml_1_0_0",
                "{\"param1\":\"value1\"}", true);
        loopTest.addMicroServicePolicy(microServicePolicy);
        LoopLog loopLog = getLoopLog(LogType.INFO, "test message", loopTest);
        loopTest.addLog(loopLog);
        LoopTemplate loopTemplate = getLoopTemplate("templateName", "yaml", 1);
        loopTest.setLoopTemplate(loopTemplate);

        String jsonSerialized = JsonUtils.GSON_JPA_MODEL.toJson(loopTest);
        assertThat(jsonSerialized).isNotNull().isNotEmpty();
        System.out.println(jsonSerialized);
        Loop loopTestDeserialized = JsonUtils.GSON_JPA_MODEL.fromJson(jsonSerialized, Loop.class);
        assertNotNull(loopTestDeserialized);
        assertThat(loopTestDeserialized).isEqualToIgnoringGivenFields(loopTest, "svgRepresentation", "blueprint",
                "components");
        assertThat(loopTestDeserialized.getComponent("DCAE").getState())
                .isEqualToComparingFieldByField(loopTest.getComponent("DCAE").getState());
        assertThat(loopTestDeserialized.getComponent("POLICY").getState()).isEqualToComparingOnlyGivenFields(
                loopTest.getComponent("POLICY").getState(), "stateName", "description");
        // blueprint not exposed so wont be deserialized

        assertThat(loopTestDeserialized.getOperationalPolicies()).containsExactly(opPolicy);
        assertThat(loopTestDeserialized.getMicroServicePolicies()).containsExactly(microServicePolicy);
        assertThat(loopTestDeserialized.getLoopLogs()).containsExactly(loopLog);
        assertThat((LoopLog) loopTestDeserialized.getLoopLogs().toArray()[0]).isEqualToIgnoringGivenFields(loopLog,
                "loop");

        // Verify the loop template
        assertThat(loopTestDeserialized.getLoopTemplate()).isEqualTo(loopTemplate);
    }

    /**
     * This tests the service object GSON encode/decode.
     *
     * @throws IOException In case of issues
     */
    @Test
    public void loopServiceTest() throws IOException {
        Loop loopTest2 = getLoop("ControlLoopTest", "yamlcontent", "{\"testname\":\"testvalue\"}",
                "123456789", "https://dcaetest.org", "UUID-blueprint");

        JsonObject jsonModel = new GsonBuilder().create()
                .fromJson(ResourceFileUtils.getResourceAsString("tosca/model-properties.json"), JsonObject.class);
        Service service = new Service(jsonModel.get("serviceDetails").getAsJsonObject(),
                jsonModel.get("resourceDetails").getAsJsonObject(), "1.0");
        loopTest2.setModelService(service);
        String jsonSerialized = JsonUtils.GSON_JPA_MODEL.toJson(loopTest2);
        assertThat(jsonSerialized).isNotNull().isNotEmpty();
        System.out.println(jsonSerialized);

        Loop loopTestDeserialized = JsonUtils.GSON_JPA_MODEL.fromJson(jsonSerialized, Loop.class);
        assertNotNull(loopTestDeserialized);
        assertThat(loopTestDeserialized).isEqualToIgnoringGivenFields(loopTest2, "modelService", "svgRepresentation",
                "blueprint", "components");
    }
}
