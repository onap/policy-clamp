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
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.client.req.tca.TcaRequestFormatter;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test Onap TcaRequestFormatter features.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class TcaRequestFormatterItCase {

    @Autowired
    private ClampProperties refProp;
    private String modelProp;
    private String modelBpmn;
    private String modelName;
    private String controlName;
    private String yamlInput;
    private ModelProperties modelProperties;

    /**
     * Initialize Test.
     */
    @Before
    public void setUp() throws IOException {
        modelProp = ResourceFileUtil.getResourceAsString("example/model-properties/tca/modelBpmnProperties.json");
        modelBpmn = ResourceFileUtil.getResourceAsString("example/model-properties/tca/modelBpmn.json");
        yamlInput = ResourceFileUtil.getResourceAsString("example/tca-policy-req/blueprint-input.yaml");
        modelName = "example-model01";
        controlName = "ClosedLoop_FRWL_SIG_fad4dcae_e498_11e6_852e_0050568c4ccf";
        modelProperties = new ModelProperties(modelName, controlName, CldsEvent.ACTION_SUBMIT, false, modelBpmn,
                modelProp);
    }

    @Test
    public void testCreatePolicyJson() throws IOException, JSONException {
        String result = TcaRequestFormatter.createPolicyJson(refProp, modelProperties);
        assertNotNull(result);
        JSONAssert.assertEquals(ResourceFileUtil.getResourceAsString("example/tca-policy-req/tca-policy-expected.json"),
                result, true);
    }

    @Test
    public void testUpdatedBlueprintWithConfiguration() throws IOException {
        String result = TcaRequestFormatter.updatedBlueprintWithConfiguration(refProp, modelProperties, yamlInput);
        assertNotNull(result);
        assertEquals(ResourceFileUtil.getResourceAsString("example/tca-policy-req/blueprint-expected.yaml"), result);
    }
}
