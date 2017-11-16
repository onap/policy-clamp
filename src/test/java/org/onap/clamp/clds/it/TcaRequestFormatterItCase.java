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

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.AbstractItCase;
import org.onap.clamp.clds.client.req.tca.TcaRequestFormatter;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test Onap TcaRequestFormatter features.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-no-camunda.properties")
public class TcaRequestFormatterItCase extends AbstractItCase {

    String modelProp;
    String modelBpmn;
    String modelName;
    String controlName;
    String yamlInput;

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
    }

    @Test
    public void testCreatePolicyJson() throws IOException {
        ModelProperties prop = new ModelProperties(modelName, controlName, CldsEvent.ACTION_SUBMIT, false, modelBpmn,
                modelProp);
        String result = TcaRequestFormatter.createPolicyJson(refProp, prop);
        assertNotNull(result);
        JSONAssert.assertEquals(ResourceFileUtil.getResourceAsString("example/tca-policy-req/tca-policy-expected.json"),
                result, true);
    }

    @Test
    public void testUpdatedBlueprintWithConfiguration() throws IOException {
        ModelProperties prop = new ModelProperties(modelName, controlName, CldsEvent.ACTION_SUBMIT, false, modelBpmn,
                modelProp);
        String result = TcaRequestFormatter.updatedBlueprintWithConfiguration(refProp, prop, yamlInput);

        assertNotNull(result);
        assertEquals(ResourceFileUtil.getResourceAsString("example/tca-policy-req/blueprint-expected.yaml"), result);
    }
}
