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

import org.onap.clamp.clds.AbstractIT;
import org.onap.clamp.clds.client.req.DcaeReq;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.transform.TransformUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test DCAE API in org.onap.clamp.ClampDesigner.client package - replicate DCAE Delegates in test.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class DcaeIT extends AbstractIT {

    @Test
    public void testDcaeReq() throws Exception {
        String modelProp = TransformUtil.getResourceAsString("example/modelProp.json");
        String modelBpmnProp = TransformUtil.getResourceAsString("example/modelBpmnProp.json");
        String modelName = "example-model";
        String controlName = "ClosedLoop-FRWL-SIG-1582f840-2881-11e6-b4ec-005056a9d756";

        ModelProperties prop = new ModelProperties(modelName, controlName, CldsEvent.ACTION_SUBMIT, true, modelBpmnProp, modelProp);
        String dcaeReq = DcaeReq.format(refProp, prop);

        System.out.println("dcaeReq=" + dcaeReq);
        System.out.println("dcaeUrl=" + System.getProperty("CLDS_DCAE_URL") + "/" + controlName);
    }

}
