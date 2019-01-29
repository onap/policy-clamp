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

package org.onap.clamp.clds.model.prop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.properties.Holmes;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Policy;
import org.onap.clamp.clds.model.properties.PolicyItem;
import org.onap.clamp.clds.model.properties.Tca;
import org.onap.clamp.clds.util.ResourceFileUtil;

/**
 * Test org.onap.clamp.ClampDesigner.model.prop package using ModelProperties.
 */
public class ModelPropertiesTest {

    @Before
    public void registerNewClasses() {
        ModelProperties.registerModelElement(Holmes.class, Holmes.getType());
    }

    @Test
    public void testTcaModelDecoding() throws IOException {
        String modelBpmnProp = ResourceFileUtil
            .getResourceAsString("example/model-properties/tca/modelBpmnProperties.json");
        String modelBpmn = ResourceFileUtil.getResourceAsString("example/model-properties/tca/modelBpmn.json");
        ModelProperties prop = new ModelProperties("example-model-name", "example-control-name", null, true, modelBpmn,
            modelBpmnProp);
        Policy policy = prop.getType(Policy.class);
        assertTrue(policy.isFound());
        assertEquals(1, policy.getPolicyChains().size());
        assertEquals("0", policy.getPolicyChains().get(0).getPolicyId());
        assertEquals(1, policy.getPolicyChains().get(0).getPolicyItems().size());
        PolicyItem firstPolicyItem = policy.getPolicyChains().get(0).getPolicyItems().get(0);
        assertEquals("resourceid", firstPolicyItem.getTargetResourceId());
        assertEquals(180, firstPolicyItem.getRetryTimeLimit());
        assertEquals(3, firstPolicyItem.getMaxRetries());
        assertEquals("", firstPolicyItem.getParentPolicy());
        assertThat(firstPolicyItem.getParentPolicyConditions()).isEmpty();
        Tca tca = prop.getType(Tca.class);
        assertNotNull(tca);
        assertTrue(tca.isFound());
        assertEquals("vFirewallBroadcastPackets", tca.getTcaItem().getEventName());
        assertEquals("VNF", tca.getTcaItem().getControlLoopSchemaType());
        assertEquals("policy1", tca.getTcaItem().getPolicyId());
        assertEquals("f734f031-10aa-t8fb-330f-04dde2886325", tca.getTcaItem().getTcaUuId());
        assertEquals(2, tca.getTcaItem().getTcaThresholds().size());
        assertEquals("ABATED", tca.getTcaItem().getTcaThresholds().get(0).getClosedLoopEventStatus());
        assertEquals("$.event.measurementsForVfScalingFields.additionalMeasurements[*].arrayOfFields[0].value",
            tca.getTcaItem().getTcaThresholds().get(0).getFieldPath());
        assertEquals("LESS_OR_EQUAL", tca.getTcaItem().getTcaThresholds().get(0).getOperator());
        assertEquals(Integer.valueOf(123), tca.getTcaItem().getTcaThresholds().get(0).getThreshold());
        assertEquals("ONSET", tca.getTcaItem().getTcaThresholds().get(1).getClosedLoopEventStatus());
        assertEquals("$.event.measurementsForVfScalingFields.additionalMeasurements[*].arrayOfFields[0].value",
            tca.getTcaItem().getTcaThresholds().get(1).getFieldPath());
        assertEquals("GREATER_OR_EQUAL", tca.getTcaItem().getTcaThresholds().get(1).getOperator());
        assertEquals(Integer.valueOf(123), tca.getTcaItem().getTcaThresholds().get(1).getThreshold());
        // Test global prop
        assertEquals("vnfRecipe", prop.getGlobal().getActionSet());
        assertEquals("4cc5b45a-1f63-4194-8100-cd8e14248c92", prop.getGlobal().getService());
        assertTrue(Arrays.equals(new String[] { "023a3f0d-1161-45ff-b4cf-8918a8ccf3ad" },
            prop.getGlobal().getResourceVf().toArray()));
        assertTrue(Arrays.equals(new String[] { "SNDGCA64", "ALPRGAED", "LSLEILAA", "MDTWNJC1" },
            prop.getGlobal().getLocation().toArray()));
        assertEquals("value1", prop.getGlobal().getDeployParameters().get("input1").getAsString());
        assertEquals("value2", prop.getGlobal().getDeployParameters().get("input2").getAsString());
    }

    @Test
    public void testHolmesModelDecoding() throws IOException {
        String modelBpmnProp = ResourceFileUtil
            .getResourceAsString("example/model-properties/holmes/modelBpmnProperties.json");
        String modelBpmn = ResourceFileUtil.getResourceAsString("example/model-properties/holmes/modelBpmn.json");
        ModelProperties prop = new ModelProperties("example-model-name", "example-control-name", null, true, modelBpmn,
            modelBpmnProp);
        Policy policy = prop.getType(Policy.class);
        assertTrue(policy.isFound());
        assertEquals(1, policy.getPolicyChains().size());
        assertEquals("0", policy.getPolicyChains().get(0).getPolicyId());
        assertEquals(1, policy.getPolicyChains().get(0).getPolicyItems().size());
        PolicyItem firstPolicyItem = policy.getPolicyChains().get(0).getPolicyItems().get(0);
        assertEquals("resourceid", firstPolicyItem.getTargetResourceId());
        assertEquals(180, firstPolicyItem.getRetryTimeLimit());
        assertEquals(3, firstPolicyItem.getMaxRetries());
        assertEquals("", firstPolicyItem.getParentPolicy());
        assertThat(firstPolicyItem.getParentPolicyConditions()).isEmpty();
        Holmes holmes = prop.getType(Holmes.class);
        assertNotNull(holmes);
        assertTrue(holmes.isFound());
        assertEquals("configPolicy1", holmes.getConfigPolicyName());
        assertEquals("blabla", holmes.getCorrelationLogic());
        // Test global prop
        assertEquals("vnfRecipe", prop.getGlobal().getActionSet());
        assertEquals("4cc5b45a-1f63-4194-8100-cd8e14248c92", prop.getGlobal().getService());
        assertTrue(Arrays.equals(new String[] { "f5213e3a-9191-4362-93b5-b67f8d770e44" },
            prop.getGlobal().getResourceVf().toArray()));
        assertTrue(Arrays.equals(new String[] { "SNDGCA64", "ALPRGAED", "LSLEILAA", "MDTWNJC1" },
            prop.getGlobal().getLocation().toArray()));
        assertEquals("value1", prop.getGlobal().getDeployParameters().get("input1").getAsString());
        assertEquals("value2", prop.getGlobal().getDeployParameters().get("input2").getAsString());
    }

    @Test
    public void testGetVf() throws IOException {
        CldsModel cldsModel = new CldsModel();
        cldsModel
        .setPropText(ResourceFileUtil.getResourceAsString("example/model-properties/tca/modelBpmnProperties.json"));
        assertEquals("023a3f0d-1161-45ff-b4cf-8918a8ccf3ad", ModelProperties.getVf(cldsModel));
    }
}