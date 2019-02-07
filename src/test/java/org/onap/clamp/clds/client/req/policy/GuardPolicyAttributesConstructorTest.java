/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 Nokia Intellectual Property. All rights
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

package org.onap.clamp.clds.client.req.policy;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Policy;
import org.onap.clamp.clds.model.properties.PolicyChain;
import org.onap.clamp.clds.model.properties.PolicyItem;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.policy.api.AttributeType;
import org.onap.policy.controlloop.policy.builder.BuilderException;

public class GuardPolicyAttributesConstructorTest {

    private static final String CONTROL_NAME = "ClosedLoop-d4629aee-970f-11e8-86c9-02552dda865e";
    private ModelProperties modelProperties;
    private List<PolicyChain> policyChains;

    /**
     * @throws Exception thrown if resources not found.
     */
    @Before
    public void setUp() throws Exception {
        String modelProp = ResourceFileUtil
            .getResourceAsString("example/model-properties/tca_new/model-properties.json");
        String modelBpmnJson = ResourceFileUtil.getResourceAsString("example/model-properties/tca_new/model-bpmn.json");
        modelProperties = new ModelProperties("CLAMPDemoVFW_v1_0_3af8daec-6f10-4027-a3540", CONTROL_NAME, "PUT", false,
            modelBpmnJson, modelProp);

        policyChains = modelProperties.getType(Policy.class).getPolicyChains();
    }

    @Test
    public void testRequestAttributes() throws IOException, BuilderException {
        List<PolicyItem> policyItemsList = GuardPolicyAttributesConstructor
            .getAllPolicyGuardsFromPolicyChain(policyChains.get(0));

        Assertions.assertThat(policyItemsList.size()).isEqualTo(1);

        // Test first entry
        Map<AttributeType, Map<String, String>> requestAttributes = GuardPolicyAttributesConstructor
            .formatAttributes(modelProperties, policyItemsList.get(0));

        Assertions.assertThat(requestAttributes).containsKeys(AttributeType.MATCHING);
        Map<String, String> ruleParameters = requestAttributes.get(AttributeType.MATCHING);
        Assertions.assertThat(ruleParameters).contains(Assertions.entry(GuardPolicyAttributesConstructor.ACTOR, "APPC"),
            Assertions.entry(GuardPolicyAttributesConstructor.RECIPE, "restart"),
            Assertions.entry(GuardPolicyAttributesConstructor.TARGETS, ".*"),
            Assertions.entry(GuardPolicyAttributesConstructor.CLNAME,
                modelProperties.getControlNameAndPolicyUniqueId()),
            Assertions.entry(GuardPolicyAttributesConstructor.LIMIT, "1"),
            Assertions.entry(GuardPolicyAttributesConstructor.TIME_WINDOW, "10"),
            Assertions.entry(GuardPolicyAttributesConstructor.TIME_UNITS, "minute"),
            Assertions.entry(GuardPolicyAttributesConstructor.GUARD_ACTIVE_START, "00:00:01-05:00"),
            Assertions.entry(GuardPolicyAttributesConstructor.GUARD_ACTIVE_END, "00:00:00-05:00"));
    }
}
