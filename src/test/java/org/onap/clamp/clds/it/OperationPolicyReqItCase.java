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
 * ===================================================================
 * 
 */

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.client.req.policy.OperationalPolicyReq;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Policy;
import org.onap.clamp.clds.model.properties.PolicyChain;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.policy.api.AttributeType;
import org.onap.policy.controlloop.policy.builder.BuilderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OperationPolicyReqItCase {

    @Autowired
    private ClampProperties refProp;

    @Test
    public void formatAttributesTest() throws IOException, BuilderException {
        String modelBpmnProp = ResourceFileUtil
                .getResourceAsString("example/model-properties/policy/modelBpmnProperties.json");
        String modelBpmn = ResourceFileUtil.getResourceAsString("example/model-properties/policy/modelBpmn.json");
        ModelProperties modelProperties = new ModelProperties("testModel", "controlNameTest", CldsEvent.ACTION_SUBMIT,
                true, modelBpmn, modelBpmnProp);
        List<Map<AttributeType, Map<String, String>>> attributes = new ArrayList<>();
        if (modelProperties.getType(Policy.class).isFound()) {
            for (PolicyChain policyChain : modelProperties.getType(Policy.class).getPolicyChains()) {
                attributes.add(OperationalPolicyReq.formatAttributes(refProp, modelProperties,
                        modelProperties.getType(Policy.class).getId(), policyChain));
            }
        }
        assertFalse(attributes.isEmpty());
        assertTrue(attributes.size() == 2);
        // now validate the Yaml, to do so we replace the dynamic ID by a known
        // key so that we can compare it
        String yaml = URLDecoder.decode(attributes.get(0).get(AttributeType.RULE).get("controlLoopYaml"), "UTF-8");
        yaml = replaceGeneratedValues(yaml);
        assertEquals(ResourceFileUtil.getResourceAsString("example/operational-policy/yaml-policy-chain-1.yaml"), yaml);
        yaml = URLDecoder.decode(attributes.get(1).get(AttributeType.RULE).get("controlLoopYaml"), "UTF-8");
        yaml = replaceGeneratedValues(yaml);
        assertEquals(ResourceFileUtil.getResourceAsString("example/operational-policy/yaml-policy-chain-2.yaml"), yaml);
    }

    private String replaceGeneratedValues(String yaml) {
        yaml = yaml.replaceAll("Policy - created" + System.lineSeparator() + "    by CLDS", "Policy - created by CLDS");
        yaml = yaml.replaceAll("trigger_policy: (.*)", "trigger_policy: <generatedId>");
        yaml = yaml.replaceAll("id: (.*)", "id: <generatedId>");
        yaml = yaml.replaceAll("success: (.*)", "success: <generatedId>");
        // Remove this field as not always present (depends of policy api)
        yaml = yaml.replaceAll("  pnf: null" + System.lineSeparator(), "");
        yaml = yaml.replaceAll("failure: (.*)", "failure: <generatedId>");
        yaml = yaml.replaceAll("failure_exception: (.*)", "failure_exception: <generatedId>");
        yaml = yaml.replaceAll("failure_retries: (.*)", "failure_retries: <generatedId>");
        yaml = yaml.replaceAll("failure_timeout: (.*)", "failure_timeout: <generatedId>");
        yaml = yaml.substring(yaml.indexOf("controlLoop:"), yaml.length());
        return yaml;
    }
}
