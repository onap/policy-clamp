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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.onap.clamp.clds.transform.TransformUtil;

public class CustomModelElementTest {

    public CustomModelElementTest() {
    }

    @Test
    public void testNewElement() {

        try {
            String modelBpmnProp = TransformUtil.getResourceAsString("example/modelBpmnProp.json");
            String modelProp = TransformUtil.getResourceAsString("example/modelProp.json");
            String modName = "example-model-name";
            String controlName = "example-control-name";

            CustomModelElement customModelElement = null;

            // Instantiate first, we should not have our CustomModelElement yet
            ModelProperties prop = new ModelProperties(modName, controlName, null, true, modelBpmnProp, modelProp);

            Assert.assertNotNull(prop);

            customModelElement = prop.getType(CustomModelElement.class);

            Assert.assertNull(customModelElement);

            ModelProperties.registerModelElement(CustomModelElement.class, CustomModelElement.getType());

            customModelElement = prop.getType(CustomModelElement.class);

            Assert.assertNotNull(customModelElement);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
