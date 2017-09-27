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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public void testHolmes() throws IOException {

        String modelBpmnProp = ResourceFileUtil.getResourceAsString("example/model-properties/modelBpmnProp.json");
        String modelBpmn = ResourceFileUtil.getResourceAsString("example/model-properties/modelBpmn.json");

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
        assertTrue(tca.isFound());
        assertEquals(1, tca.getTcaItems().size());
        assertEquals(0, tca.getTcaItems().get(0).getTcaThreshholds().size());

        Holmes holmes = prop.getType(Holmes.class);
        assertTrue(holmes.isFound());
        assertEquals("policy1", holmes.getOperationalPolicy());
        assertEquals("blabla", holmes.getCorrelationLogic());
    }

    @Test
    public void testGetVf() throws IOException {
        CldsModel cldsModel = new CldsModel();
        cldsModel.setPropText(
                ResourceFileUtil.getResourceAsString("example/model-properties/modelBpmnPropWithGlobal.json"));
        assertEquals("f5213e3a-9191-4362-93b5-b67f8d770e44", ModelProperties.getVf(cldsModel));
    }
}