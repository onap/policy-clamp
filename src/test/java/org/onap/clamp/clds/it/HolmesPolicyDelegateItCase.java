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

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.client.HolmesPolicyDelegate;
import org.onap.clamp.clds.model.properties.Holmes;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test Onap HolmesPolicyDelegate features.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class HolmesPolicyDelegateItCase {

    @Test
    public void testCreatePolicyJson() throws IOException {
        String modelBpmnProp = ResourceFileUtil
                .getResourceAsString("example/model-properties/holmes/modelBpmnProperties.json");
        String modelBpmn = ResourceFileUtil.getResourceAsString("example/model-properties/holmes/modelBpmn.json");
        ModelProperties prop = new ModelProperties("example-model-name",
                "ClosedLoop_FRWL_SIG_fad4dcae_e498_11e6_852e_0050568c4ccf", null, true, modelBpmn, modelBpmnProp);
        Holmes holmes = prop.getType(Holmes.class);
        String result = HolmesPolicyDelegate.formatHolmesConfigBody(prop, holmes);
        assertTrue("ClosedLoop_FRWL_SIG_fad4dcae_e498_11e6_852e_0050568c4ccf$$$blabla".equals(result));
    }
}
