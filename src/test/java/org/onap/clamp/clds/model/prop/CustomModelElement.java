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

import com.google.gson.JsonObject;
import org.onap.clamp.clds.model.properties.AbstractModelElement;
import org.onap.clamp.clds.model.properties.ModelBpmn;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.util.JsonUtils;

/**
 * A CustomModelElement to test the capability to add new elements on the fly.
 */
public class CustomModelElement extends AbstractModelElement {

    private String test;
    private static final String CUSTOM_TYPE = "customType";

    /**
     * Main Constructor.
     */
    public CustomModelElement(ModelProperties modelProp, ModelBpmn modelBpmn, JsonObject modelJson) {
        super(CUSTOM_TYPE, modelProp, modelBpmn, modelJson);
        topicPublishes = JsonUtils.getStringValueByName(modelElementJsonNode, "topicPublishes");
        test = JsonUtils.getStringValueByName(modelElementJsonNode, "test");
    }

    public static final String getType() {
        return CUSTOM_TYPE;
    }

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}
