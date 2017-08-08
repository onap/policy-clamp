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

import org.onap.clamp.clds.transform.TransformUtil;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import java.io.IOException;

/**
 * Test org.onap.clamp.ClampDesigner.model.prop package using ModelProperties.
 */
public class ModelPropertiesTest {

    @Test
    public void testJsonParse() throws IOException {
        String modelBpmnProp = TransformUtil.getResourceAsString("example/modelBpmnProp.json");
        String modelProp = TransformUtil.getResourceAsString("example/modelProp.json");
        String modName = "example-model-name";
        String controlName = "example-control-name";

        ModelProperties prop = new ModelProperties(modName, controlName, null, true, modelBpmnProp, modelProp);
        Assert.assertEquals(modName, prop.getModelName());
        Assert.assertEquals(controlName, prop.getControlName());
        Assert.assertEquals(null, prop.getActionCd());

        Global g = prop.getGlobal();
        Assert.assertEquals("0f983e18-4603-4bb4-a98c-e29691fb16a1", g.getService());
        Assert.assertEquals("[SNDGCA64]", g.getLocation().toString());
        Assert.assertEquals("[6c7aaec2-59eb-41d9-8681-b7f976ab668d]", g.getResourceVf().toString());

        StringMatch sm = prop.getType(StringMatch.class);
        Assert.assertEquals("StringMatch_", sm.getId());

        Policy p = prop.getType(Policy.class);
        Assert.assertEquals("Policy_", p.getId());
        Assert.assertEquals(null, p.getTopicPublishes());
        Assert.assertEquals(null, p.getTopicSubscribes());

        Tca t = prop.getType(Tca.class);
        Assert.assertEquals("Narra", t.getTcaItems().get(0).getTcaName());
        Assert.assertEquals(Integer.valueOf(4), t.getTcaItems().get(0).getTcaThreshholds().get(0).getThreshhold());
    }

    @Test
	public void testPolicy() throws IOException {

		String modelBpmnProp = TransformUtil.getResourceAsString("example/modelBpmnPropForPolicy.json");
		System.out.println(modelBpmnProp);

		String modelProp = TransformUtil.getResourceAsString("example/modelPropForPolicy.json");
		System.out.println(modelProp);

		ModelProperties prop = new ModelProperties("example-model-name", "example-control-name", null, true, modelBpmnProp, modelProp);
		System.out.println("attempting prop.getGlobal()...");
		Global g = prop.getGlobal();
		System.out.println("attempting prop.getStringMatch()...");
		StringMatch stringMatch = prop.getType(StringMatch.class);
		if(stringMatch.isFound()){
			System.out.println("stringMatch json object is present...");
			assertEquals("1", stringMatch.getResourceGroups().get(0).getPolicyId());
		}
		System.out.println("attempting prop.getPolicy()...");
		Policy policy = prop.getType(Policy.class);
		if(policy.isFound()){
			System.out.println("policy json object is present...");
			assertEquals("1", policy.getPolicyChains().get(0).getPolicyId());
		}
	}


}