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

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A CustomModelElement to test the capability to add new elements on the fly.
 */
public class CustomModelElement extends ModelElement {

    private static final String CUSTOM_TYPE = "customType";

    /**
     * 
     */
    public CustomModelElement(ModelProperties modelProp, ModelBpmn modelBpmn, JsonNode modelJson) {
        super(CUSTOM_TYPE, modelProp, modelBpmn, modelJson);
        topicPublishes = getValueByName("topicPublishes");
    }

    public static final String getType() {
        return CUSTOM_TYPE;
    }

}
