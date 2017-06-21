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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parse StringMatch json properties.
 * <p>
 * Example json: "StringMatch_0c2cy0c":[[{"name":"topicPublishes","value":"DCAE-CL-EVENT"}],{"serviceConfigurations":[[{"name":"aaiMatchingFields","value":["VMID"]},{"name":"aaiSendFields","value":["VNFNAME","LOCID"]},{"name":"vnf","value":["aSBG"]},{"name":"timeWindow","value":["0"]},{"name":"ageLimit","value":["1600"]},{"name":"createClosedLoopEventId","value":["Initial"]},{"name":"outputEventName","value":["OnSet"]},{"stringSet":[{"name":"alarmCondition","value":["authenticationFailure"]},{"name":"eventSeverity","value":["NORMAL"]},{"name":"eventSourceType","value":["f5BigIP"]}]}],[{"name":"aaiMatchingFields","value":["VMID"]},{"name":"aaiSendFields","value":["VMID","Identiy","VNFNAME"]},{"name":"vnf","value":["aSBG"]},{"name":"timeWindow","value":["0"]},{"name":"ageLimit","value":["1600"]},{"name":"createClosedLoopEventId","value":["Close"]},{"name":"outputEventName","value":["Abatement"]},{"stringSet":[{"name":"alarmCondition","value":["authenticationFailure"]},{"name":"eventSeverity","value":["NORMAL"]},{"name":"eventSourceType","value":["f5BigIP"]}]}]]}]
 */
public class StringMatch extends ModelElement {
    private static final Logger logger = Logger.getLogger(StringMatch.class.getName());

    private final List<ServiceConfiguration> serviceConfigurations;

    /**
     * Parse StringMatch given json node.
     *
     * @param modelBpmn
     * @param modelJson
     */
    public StringMatch(ModelProperties modelProp, ModelBpmn modelBpmn, JsonNode modelJson) {
        super(ModelElement.TYPE_STRING_MATCH, modelProp, modelBpmn, modelJson);

        topicPublishes = getValueByName(meNode.get(0), "topicPublishes");

        // process Server_Configurations
        JsonNode serviceConfigurationsNode = meNode.get(1).get("serviceConfigurations");
        Iterator<JsonNode> itr = serviceConfigurationsNode.elements();
        serviceConfigurations = new ArrayList<>();
        while (itr.hasNext()) {
            serviceConfigurations.add(new ServiceConfiguration(itr.next()));
        }
    }

    /**
     * @return the serviceConfigurations
     */
    public List<ServiceConfiguration> getServiceConfigurations() {
        return serviceConfigurations;
    }

}
