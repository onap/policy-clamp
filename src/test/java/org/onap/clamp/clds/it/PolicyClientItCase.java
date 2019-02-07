/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 *
 */

package org.onap.clamp.clds.it;

import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.client.req.policy.GuardPolicyAttributesConstructor;
import org.onap.clamp.clds.client.req.policy.OperationalPolicyAttributesConstructor;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.client.req.tca.TcaRequestFormatter;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsToscaModel;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Policy;
import org.onap.clamp.clds.model.properties.PolicyItem;
import org.onap.clamp.clds.model.properties.Tca;
import org.onap.clamp.clds.transform.XslTransformer;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.policy.api.AttributeType;
import org.onap.policy.api.PolicyConfigType;
import org.onap.policy.api.PolicyType;
import org.onap.policy.controlloop.policy.builder.BuilderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test Policy API, this uses the emulator written in python that is started
 * during the tests, It returns the payload sent in the policy queries so that
 * it can be validated here.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class PolicyClientItCase {

    private static final Type MAP_OF_STRING_TO_OBJECT_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    @Autowired
    private CldsDao cldsDao;
    @Autowired
    private ClampProperties refProp;
    @Autowired
    private PolicyClient policyClient;
    @Autowired
    XslTransformer cldsBpmnTransformer;

    String modelProp;
    String modelName;
    String controlName;
    String modelBpmnPropJson;
    ModelProperties prop;

    /**
     * Setup method.
     * @throws IOException thrown if resources not found
     * @throws TransformerException thrown if invalid xml given to transformation
     */
    @Before
    public void setUp() throws IOException, TransformerException {
        modelProp = ResourceFileUtil.getResourceAsString("example/model-properties/tca_new/model-properties.json");
        modelName = "example-model06";
        controlName = "ClosedLoop_FRWL_SIG_fad4dcae_e498_11e6_852e_0050568c4ccf";
        modelBpmnPropJson = cldsBpmnTransformer.doXslTransformToString(
            ResourceFileUtil.getResourceAsString("example/model-properties/tca_new/tca-template.xml"));
        prop = new ModelProperties(modelName, controlName, CldsEvent.ACTION_SUBMIT, false, modelBpmnPropJson,
            modelProp);
    }

    @Test
    public void testSendGuardPolicy() {
        // Normally there is only one Guard
        List<PolicyItem> policyItems = GuardPolicyAttributesConstructor
            .getAllPolicyGuardsFromPolicyChain(prop.getType(Policy.class).getPolicyChains().get(0));
        PolicyItem policyItem = policyItems.get(0);
        prop.setCurrentModelElementId(prop.getType(Policy.class).getId());
        prop.setPolicyUniqueId(prop.getType(Policy.class).getPolicyChains().get(0).getPolicyId());
        prop.setGuardUniqueId(policyItem.getId());
        String response = policyClient.sendGuardPolicy(
            GuardPolicyAttributesConstructor.formatAttributes(prop, policyItem), prop, LoggingUtils.getRequestId(),
            policyItem);
        Map<String, Object> mapNodes = JsonUtils.GSON.fromJson(response, MAP_OF_STRING_TO_OBJECT_TYPE);
        Assertions.assertThat(mapNodes).contains(Assertions.entry("policyClass", "Decision"),
            Assertions.entry("policyName",
                modelName.replace("-", "_") + "." + controlName + "_Policy_12lup3h_0_Guard_6TtHGPq"),
            Assertions.entry("policyDescription", "from clds"), Assertions.entry("onapName", "PDPD"),
            Assertions.entry("requestID", LoggingUtils.getRequestId()), Assertions.entry("ruleProvider", "GUARD_YAML"));

        // Check Guard attributes
        Assertions.assertThat((Map<String, Object>) mapNodes.get("attributes"))
            .containsKey(AttributeType.MATCHING.name());
        Assertions.assertThat(
            (Map<String, Object>) ((Map<String, Object>) mapNodes.get("attributes")).get(AttributeType.MATCHING.name()))
            .contains(Assertions.entry(GuardPolicyAttributesConstructor.ACTOR, "APPC"),
                Assertions.entry(GuardPolicyAttributesConstructor.CLNAME, controlName + "_0"),
                Assertions.entry(GuardPolicyAttributesConstructor.TIME_WINDOW, "10"));
    }

    @Test
    public void testSendBrmsPolicy() throws BuilderException, IllegalArgumentException, IOException {
        Map<AttributeType, Map<String, String>> attributes = OperationalPolicyAttributesConstructor.formatAttributes(
            refProp, prop, prop.getType(Policy.class).getId(), prop.getType(Policy.class).getPolicyChains().get(0));
        String response = policyClient.sendBrmsPolicy(attributes, prop, LoggingUtils.getRequestId());

        Map<String, Object> mapNodes = JsonUtils.GSON.fromJson(response, MAP_OF_STRING_TO_OBJECT_TYPE);

        Assertions.assertThat(mapNodes).contains(Assertions.entry("policyClass", "Config"),
            Assertions.entry("policyName", modelName.replace("-", "_") + "." + controlName + "_Policy_12lup3h_0"),
            Assertions.entry("policyConfigType", PolicyConfigType.BRMS_PARAM.name()),
            Assertions.entry("requestID", LoggingUtils.getRequestId()));

        // Check BRMS attributes present
        Assertions.assertThat((Map<String, Object>) mapNodes.get("attributes"))
            .containsKeys(AttributeType.MATCHING.name(), AttributeType.RULE.name());

    }

    @Test
    public void testSendMicroServiceInJson() throws IllegalArgumentException {
        prop.setCurrentModelElementId(prop.getType(Policy.class).getId());
        String jsonToSend = "{\"test\":\"test\"}";
        String response = policyClient.sendMicroServiceInJson(jsonToSend, prop, LoggingUtils.getRequestId());

        Map<String, Object> mapNodes = JsonUtils.GSON.fromJson(response, MAP_OF_STRING_TO_OBJECT_TYPE);

        Assertions.assertThat(mapNodes).contains(Assertions.entry("policyClass", "Config"),
            Assertions.entry("policyName", modelName.replace("-", "_") + "." + controlName + "_Policy_12lup3h"),
            Assertions.entry("policyConfigType", PolicyConfigType.MicroService.name()),
            Assertions.entry("requestID", LoggingUtils.getRequestId()),
            Assertions.entry("configBodyType", PolicyType.JSON.name()), Assertions.entry("onapName", "DCAE"),
            Assertions.entry("configBody", jsonToSend));

    }

    @Test
    public void testSendBasePolicyInOther() throws IllegalArgumentException, IOException {
        String body = "test";
        String response = policyClient.sendBasePolicyInOther(body, "myPolicy", prop, LoggingUtils.getRequestId());
        Map<String, Object> mapNodes = JsonUtils.GSON.fromJson(response, MAP_OF_STRING_TO_OBJECT_TYPE);

        Assertions.assertThat(mapNodes).contains(Assertions.entry("policyClass", "Config"),
            Assertions.entry("policyName", "myPolicy"),
            Assertions.entry("policyConfigType", PolicyConfigType.Base.name()),
            Assertions.entry("requestID", LoggingUtils.getRequestId()),
            Assertions.entry("configBodyType", PolicyType.OTHER.name()), Assertions.entry("onapName", "DCAE"),
            Assertions.entry("configBody", body));
    }

    @Test
    public void testSendMicroServiceInOther() throws IllegalArgumentException, IOException {
        Tca tca = prop.getType(Tca.class);
        String tcaJson = TcaRequestFormatter.createPolicyJson(refProp, prop);
        String response = policyClient.sendMicroServiceInOther(tcaJson, prop);

        Map<String, Object> mapNodes = JsonUtils.GSON.fromJson(response, MAP_OF_STRING_TO_OBJECT_TYPE);

        Assertions.assertThat(mapNodes).contains(Assertions.entry("policyClass", "Config"),
            Assertions.entry("policyName", modelName.replace("-", "_") + "." + controlName + "_TCA_1d13unw"),
            Assertions.entry("policyConfigType", PolicyConfigType.MicroService.name()),
            Assertions.entry("configBody", tcaJson), Assertions.entry("onapName", "DCAE"));
    }

    @Test
    public void testDeleteMicrosService() throws IllegalArgumentException, IOException {
        Tca tca = prop.getType(Tca.class);
        prop.setCurrentModelElementId(tca.getId());
        String[] responses = policyClient.deleteMicrosService(prop).split("\\}\\{");

        // There are 2 responses appended to the result, one for PDP one for PAP !
        Map<String, Object> mapNodesPdp = JsonUtils.GSON.fromJson(responses[0] + "}",
            MAP_OF_STRING_TO_OBJECT_TYPE);
        Map<String, Object> mapNodesPap = JsonUtils.GSON.fromJson("{" + responses[1],
            MAP_OF_STRING_TO_OBJECT_TYPE);

        Assertions.assertThat(mapNodesPdp).contains(
            Assertions.entry("policyName", modelName.replace("-", "_") + "." + controlName + "_TCA_1d13unw"),
            Assertions.entry("policyType", PolicyConfigType.MicroService.name()),
            Assertions.entry("policyComponent", "PDP"), Assertions.entry("deleteCondition", "ALL"));

        Assertions.assertThat(mapNodesPap).contains(
            Assertions.entry("policyName", modelName.replace("-", "_") + "." + controlName + "_TCA_1d13unw"),
            Assertions.entry("policyType", PolicyConfigType.MicroService.name()),
            Assertions.entry("policyComponent", "PAP"), Assertions.entry("deleteCondition", "ALL"));
    }

    @Test
    public void testDeleteGuard() throws IllegalArgumentException, IOException {
        List<PolicyItem> policyItems = GuardPolicyAttributesConstructor
            .getAllPolicyGuardsFromPolicyChain(prop.getType(Policy.class).getPolicyChains().get(0));
        prop.setCurrentModelElementId(prop.getType(Policy.class).getId());
        prop.setPolicyUniqueId(prop.getType(Policy.class).getPolicyChains().get(0).getPolicyId());
        prop.setGuardUniqueId(policyItems.get(0).getId());
        String[] responses = policyClient.deleteGuard(prop).split("\\}\\{");

        Map<String, Object> mapNodesPdp = JsonUtils.GSON.fromJson(responses[0] + "}",
            MAP_OF_STRING_TO_OBJECT_TYPE);
        Map<String, Object> mapNodesPap = JsonUtils.GSON.fromJson("{" + responses[1],
            MAP_OF_STRING_TO_OBJECT_TYPE);

        Assertions.assertThat(mapNodesPdp).contains(
            Assertions.entry("policyName",
                modelName.replace("-", "_") + "." + controlName + "_Policy_12lup3h_0_Guard_6TtHGPq"),
            Assertions.entry("policyType", "Decision"), Assertions.entry("policyComponent", "PDP"),
            Assertions.entry("deleteCondition", "ALL"));
        Assertions.assertThat(mapNodesPap).contains(
            Assertions.entry("policyName",
                modelName.replace("-", "_") + "." + controlName + "_Policy_12lup3h_0_Guard_6TtHGPq"),
            Assertions.entry("policyType", "Decision"), Assertions.entry("policyComponent", "PAP"),
            Assertions.entry("deleteCondition", "ALL"));
    }

    @Test
    public void testDeleteBrms() throws IllegalArgumentException, IOException {
        prop.setPolicyUniqueId(prop.getType(Policy.class).getPolicyChains().get(0).getPolicyId());
        prop.setCurrentModelElementId(prop.getType(Policy.class).getId());
        String[] responses = policyClient.deleteBrms(prop).split("\\}\\{");

        Map<String, Object> mapNodesPdp = JsonUtils.GSON.fromJson(responses[0] + "}",
            MAP_OF_STRING_TO_OBJECT_TYPE);
        Map<String, Object> mapNodesPap = JsonUtils.GSON.fromJson("{" + responses[1],
            MAP_OF_STRING_TO_OBJECT_TYPE);

        Assertions.assertThat(mapNodesPdp).contains(
            Assertions.entry("policyName", modelName.replace("-", "_") + "." + controlName + "_Policy_12lup3h_0"),
            Assertions.entry("policyType", "BRMS_Param"), Assertions.entry("policyComponent", "PDP"),
            Assertions.entry("deleteCondition", "ALL"));
        Assertions.assertThat(mapNodesPap).contains(
            Assertions.entry("policyName", modelName.replace("-", "_") + "." + controlName + "_Policy_12lup3h_0"),
            Assertions.entry("policyType", "BRMS_Param"), Assertions.entry("policyComponent", "PAP"),
            Assertions.entry("deleteCondition", "ALL"));
    }

    @Test
    public void testImportToscaModel() throws IOException {
        String toscaModelYaml = ResourceFileUtil.getResourceAsString("tosca/tca-policy-test.yaml");
        CldsToscaModel cldsToscaModel = new CldsToscaModel();
        cldsToscaModel.setToscaModelName("tca-policy-test");
        cldsToscaModel.setToscaModelYaml(toscaModelYaml);
        cldsToscaModel.setUserId("admin");
        cldsToscaModel.setPolicyType("tca");
        cldsToscaModel = cldsToscaModel.save(cldsDao, refProp, policyClient, "test");
        String tosca = policyClient.importToscaModel(cldsToscaModel);

        Assertions.assertThat(tosca).contains(
            "{\"serviceName\":\"tca-policy-test\",\"description\":\"tca-policy-test\","
                + "\"requestID\":null,\"filePath\":\"/tmp/tosca-models/tca-policy-test.yml\",");
        Assertions.assertThat(tosca).contains(toscaModelYaml);
    }
}
