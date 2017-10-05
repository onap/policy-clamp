/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.model.prop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.onap.clamp.clds.model.CldsModel;
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
        assertEquals("resourceid", policy.getPolicyChains().get(0).getPolicyItems().get(0).getTargetResourceId());
        assertEquals(180, policy.getPolicyChains().get(0).getPolicyItems().get(0).getRetryTimeLimit());
        assertEquals(3, policy.getPolicyChains().get(0).getPolicyItems().get(0).getMaxRetries());
        assertEquals("", policy.getPolicyChains().get(0).getPolicyItems().get(0).getParentPolicy());
        assertEquals(null, policy.getPolicyChains().get(0).getPolicyItems().get(0).getParentPolicyConditions());

        Tca tca = prop.getType(Tca.class);
        assertNotNull(tca);
        assertTrue(tca.isFound());
        assertEquals("vFirewallBroadcastPackets", tca.getTcaItem().getEventName());
        assertEquals("policy1", tca.getTcaItem().getPolicyId());
        assertEquals("f734f031-10aa-t8fb-330f-04dde2886325", tca.getTcaItem().getTcaUuId());
        assertEquals(2, tca.getTcaItem().getTcaThresholds().size());

        assertEquals("ABATED", tca.getTcaItem().getTcaThresholds().get(0).getClosedLoopEventStatus());
        assertEquals("VM", tca.getTcaItem().getTcaThresholds().get(0).getControlLoopSchema());
        assertEquals(
                "$.event.measurementsForVfScalingFields.vNicPerformanceArray[*].receivedBroadcastPacketsAccumulated",
                tca.getTcaItem().getTcaThresholds().get(0).getFieldPath());
        assertEquals("LESS_OR_EQUAL", tca.getTcaItem().getTcaThresholds().get(0).getOperator());
        assertEquals(Integer.valueOf(123), tca.getTcaItem().getTcaThresholds().get(0).getThreshold());

        assertEquals("ONSET", tca.getTcaItem().getTcaThresholds().get(1).getClosedLoopEventStatus());
        assertEquals("VNF", tca.getTcaItem().getTcaThresholds().get(1).getControlLoopSchema());
        assertEquals("$.event.measurementsForVfScalingFields.vNicPerformanceArray[*].receivedDiscardedPacketsDelta",
                tca.getTcaItem().getTcaThresholds().get(1).getFieldPath());
        assertEquals("GREATER_OR_EQUAL", tca.getTcaItem().getTcaThresholds().get(1).getOperator());
        assertEquals(Integer.valueOf(123), tca.getTcaItem().getTcaThresholds().get(1).getThreshold());
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
        assertEquals("resourceid", policy.getPolicyChains().get(0).getPolicyItems().get(0).getTargetResourceId());
        assertEquals(180, policy.getPolicyChains().get(0).getPolicyItems().get(0).getRetryTimeLimit());
        assertEquals(3, policy.getPolicyChains().get(0).getPolicyItems().get(0).getMaxRetries());
        assertEquals("", policy.getPolicyChains().get(0).getPolicyItems().get(0).getParentPolicy());
        assertEquals(null, policy.getPolicyChains().get(0).getPolicyItems().get(0).getParentPolicyConditions());

        Holmes holmes = prop.getType(Holmes.class);
        assertNotNull(holmes);
        assertTrue(holmes.isFound());
        assertEquals("configPolicy1", holmes.getConfigPolicyName());
        assertEquals("blabla", holmes.getCorrelationLogic());
    }

    @Test
    public void testGetVf() throws IOException {
        CldsModel cldsModel = new CldsModel();
        cldsModel.setPropText(
                ResourceFileUtil.getResourceAsString("example/model-properties/tca/modelBpmnProperties.json"));
        assertEquals("f5213e3a-9191-4362-93b5-b67f8d770e44", ModelProperties.getVf(cldsModel));
    }
}