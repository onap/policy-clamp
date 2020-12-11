/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
 *                             reserved.
 * Modifications Copyright (C) 2019 Huawei Technologies Co., Ltd.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.Set;
import javax.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.policy.clamp.clds.Application;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.loop.service.Service;
import org.onap.policy.clamp.loop.template.LoopTemplate;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.clamp.loop.template.PolicyModelsService;
import org.onap.policy.clamp.policy.microservice.MicroServicePolicy;
import org.onap.policy.clamp.policy.microservice.MicroServicePolicyService;
import org.onap.policy.clamp.policy.operational.OperationalPolicy;
import org.onap.policy.clamp.policy.operational.OperationalPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class LoopControllerTestItCase {

    private static final String EXAMPLE_LOOP_NAME = "ClosedLoopTest";
    private static final String EXAMPLE_JSON = "{\"testName\":\"testValue\"}";

    @Autowired
    LoopService loopService;

    @Autowired
    LoopsRepository loopsRepository;

    @Autowired
    MicroServicePolicyService microServicePolicyService;

    @Autowired
    OperationalPolicyService operationalPolicyService;

    @Autowired
    PolicyModelsService policyModelsService;

    @Autowired
    LoopController loopController;

    private void saveTestLoopToDb() {
        Loop testLoop = createTestLoop(EXAMPLE_LOOP_NAME, "blueprint", "representation");
        testLoop.setGlobalPropertiesJson(JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class));
        LoopTemplate template = new LoopTemplate();
        template.setName("testTemplate");
        testLoop.setLoopTemplate(template);
        Service modelService = new Service("{\"name\":\"serviceName\",\"UUID\":\"uuid\"}", "{}");
        testLoop.setModelService(modelService);
        loopService.saveOrUpdateLoop(testLoop);
    }

    private Loop createTestLoop(String loopName, String loopBlueprint, String loopSvg) {
        return new Loop(loopName);
    }

    @Test
    @Transactional
    public void testUpdateOperationalPolicies() {
        saveTestLoopToDb();
        String policy = "[{\"name\":\"OPERATIONAL_CLholmes31_v1_0_vFW_PG_T10_k8s-holmes-rules\","
                + "\"configurationsJson\":{"
                + "\"operational_policy\":{\"controlLoop\":{\"trigger_policy\":\"unique-policy-id-1-modifyConfig\","
                + "\"timeout\":\"3600\",\"abatement\":\"false\","
                + "\"controlLoopName\":\"LOOP_CLholmes31_v1_0_vFW_PG_T10_k8s-holmes-rules\"},"
                + "\"policies\":[{\"id\":\"unique-policy-id-1-modifyConfig\",\"recipe\":\"ModifyConfig\","
                + "\"retry\":\"2\",\"timeout\":\"1200\",\"actor\":\"APPC\",\"payload\":\"{\\\"active-streams\\\":5}\","
                + "\"success\":\"\",\"failure\":\"\",\"failure_timeout\":\"\",\"failure_retries\":\"\","
                + "\"failure_exception\":\"\",\"failure_guard\":\"\",\"target\":{\"type\":\"VNF\","
                + "\"resourceID\":\"vFW_PG_T1\"}}]}}}]";
        JsonParser parser = new JsonParser();
        JsonElement ele = parser.parse(policy);
        JsonArray arr = ele.getAsJsonArray();
        Loop loop = loopController.updateOperationalPolicies(EXAMPLE_LOOP_NAME, arr);
        assertThat(loop.getOperationalPolicies()).hasSize(1);
        Set<OperationalPolicy> opSet = loop.getOperationalPolicies();
        OperationalPolicy op = opSet.iterator().next();
        assertThat(op.getName()).isEqualTo("OPERATIONAL_CLholmes31_v1_0_vFW_PG_T10_k8s-holmes-rules");
    }

    @Test
    @Transactional
    public void testUpdateGlobalProperties() {
        saveTestLoopToDb();
        String policy = "{\"dcaeDeployParameters\":{\"aaiEnrichmentHost\":\"aai.onap.svc.cluster.local\","
                + "\"aaiEnrichmentPort\":\"8443\",\"enableAAIEnrichment\":\"false\",\"dmaap_host\":\"message-router"
                + ".onap\",\"dmaap_port\":\"3904\",\"enableRedisCaching\":\"false\",\"redisHosts\":\"dcae-redis.onap"
                + ".svc.cluster.local:6379\",\"tag_version\":\"nexus3.onap.org:10001/onap/org.onap.dcaegen2.deployments"
                + ".tca-cdap-container:1.1.1\",\"consul_host\":\"consul-server.onap\",\"consul_port\":\"8500\","
                + "\"cbs_host\":\"config-binding-service\",\"cbs_port\":\"10000\",\"external_port\":\"32012\","
                + "\"policy_model_id\":\"onap.policies.monitoring.cdap.tca.hi.lo.app\","
                + "\"policy_id\":\"tca_k8s_CLTCA_v1_0_vFW_PG_T10_k8s-tca-clamp-policy-05162019\"}}";
        JsonParser parser = new JsonParser();
        JsonElement ele = parser.parse(policy);
        JsonObject obj = ele.getAsJsonObject();
        loopController.updateGlobalPropertiesJson(EXAMPLE_LOOP_NAME, obj);
        Loop loop = loopController.getLoop(EXAMPLE_LOOP_NAME);
        JsonObject globalPropertiesJson = loop.getGlobalPropertiesJson();
        JsonObject prop = globalPropertiesJson.getAsJsonObject("dcaeDeployParameters");
        assertThat(prop.get("aaiEnrichmentHost").getAsString()).isEqualTo("aai.onap.svc.cluster.local");
    }

    @Test
    @Transactional
    public void testUpdateMicroservicePolicy() {
        saveTestLoopToDb();
        PolicyModel policyModel = new PolicyModel("testPolicyModel",
                "tosca_definitions_version: tosca_simple_yaml_1_0_0", "1.0.0");
        policyModelsService.saveOrUpdatePolicyModel(policyModel);
        MicroServicePolicy policy = new MicroServicePolicy("policyName", policyModel, false,
                JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null);
        loopController.updateMicroservicePolicy(EXAMPLE_LOOP_NAME, policy);
        assertThat(microServicePolicyService.isExisting("policyName")).isTrue();
    }

    @Test
    @Transactional
    public void testAddAndRemoveOperationalPolicies() throws IOException {
        saveTestLoopToDb();
        PolicyModel policyModel = new PolicyModel("testPolicyModel",
                null, "1.0.0");
        policyModelsService.saveOrUpdatePolicyModel(policyModel);

        loopController.addOperationalPolicy(EXAMPLE_LOOP_NAME, "testPolicyModel", "1.0.0");

        Loop newLoop = loopController.getLoop(EXAMPLE_LOOP_NAME);
        Set<OperationalPolicy> opPolicyList = newLoop.getOperationalPolicies();
        assertThat(opPolicyList.size()).isEqualTo(1);
        for (OperationalPolicy policy : opPolicyList) {
            assertThat(policy.getName().contains("OPERATIONAL_serviceName")).isTrue();
            Assertions.assertThat(policy.getPolicyModel().getPolicyModelType()).isEqualTo("testPolicyModel");
            Assertions.assertThat(policy.getPolicyModel().getVersion()).isEqualTo("1.0.0");
        }

        loopController.removeOperationalPolicy(EXAMPLE_LOOP_NAME, "testPolicyModel", "1.0.0");
        Loop newLoop2 = loopController.getLoop(EXAMPLE_LOOP_NAME);
        assertThat(newLoop2.getOperationalPolicies().size()).isEqualTo(0);
    }
}