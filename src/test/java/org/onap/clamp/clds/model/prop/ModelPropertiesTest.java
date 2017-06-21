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

        ModelProperties prop = new ModelProperties(modName, controlName, null, modelBpmnProp, modelProp);
        Assert.assertEquals(modName, prop.getModelName());
        Assert.assertEquals(controlName, prop.getControlName());
        Assert.assertEquals(null, prop.getActionCd());

        Global g = prop.getGlobal();
        Assert.assertEquals("df6fcd2b-1932-429e-bb13-0cd0d32113cb", g.getService());
        Assert.assertEquals("[SNDGCA64, ALPRGAED]", g.getLocation().toString());
        Assert.assertEquals("[4b49acee-cf70-4b20-b956-a4fe0c1a8239]", g.getResourceVf().toString());

        Collector c = prop.getCollector();
        Assert.assertEquals("Collector_", c.getId());
        Assert.assertEquals("DCAE-COLLECTOR-UCSNMP", c.getTopicPublishes());

        StringMatch sm = prop.getStringMatch();
        Assert.assertEquals("StringMatch_", sm.getId());
        Assert.assertEquals("DCAE-CL-EVENT", sm.getTopicPublishes());

        Policy p = prop.getPolicy();
        Assert.assertEquals("Policy_", p.getId());
        Assert.assertEquals(null, p.getTopicPublishes());
        Assert.assertEquals("DCAE-CL-EVENT", p.getTopicSubscribes());
        Assert.assertEquals(500, p.getTimeout().intValue());
        
        Tca t = prop.getTca();
        Assert.assertEquals("Narra", t.getTcaItems().get(0).getTcaName());
        Assert.assertEquals(Integer.valueOf(4), t.getTcaItems().get(0).getTcaThreshholds().get(0).getThreshhold());
    }

}