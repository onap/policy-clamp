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
 * Parse Holmes bpmn parameters json properties.
 * <p>
 * Example json:
 * [{"name":"correlationalLogic","value":"vcwx"},{"name":"configPolicyName","value":"cccc"}]
 *
 */
public class Holmes extends AbstractModelElement {

    private static final String TYPE_HOLMES = "holmes";

    private String              correlationLogic;

    private String              configPolicyName;

    /**
     * Default constructor for Holmes Element
     *
     * @param modelProp
     *            The ModelProperties containing the all the info, like bpmn,
     *            bpmn params, etc ...
     * @param modelBpmn
     * @param modelJson
     */
    public Holmes(ModelProperties modelProp, ModelBpmn modelBpmn, JsonNode modelJson) {
        super(TYPE_HOLMES, modelProp, modelBpmn, modelJson);

        correlationLogic = this.getValueByName("correlationalLogic");
        configPolicyName = this.getValueByName("configPolicyName");
    }

    public static final String getType() {
        return TYPE_HOLMES;
    }

    public String getCorrelationLogic() {
        return correlationLogic;
    }

    public String getConfigPolicyName() {
        return configPolicyName;
    }

}
