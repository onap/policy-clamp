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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Parse serviceConfigurations from StringMatch json properties.
 * <p>
 * Example json: "StringMatch_0c2cy0c":[[{"name":"topicPublishes","value":"DCAE-CL-EVENT"}],{"serviceConfigurations":[[{"name":"aaiMatchingFields","value":["VMID"]},{"name":"aaiSendFields","value":["VNFNAME","LOCID"]},{"name":"vnf","value":["aSBG"]},{"name":"timeWindow","value":["0"]},{"name":"ageLimit","value":["1600"]},{"name":"createClosedLoopEventId","value":["Initial"]},{"name":"outputEventName","value":["OnSet"]},{"stringSet":[{"name":"alarmCondition","value":["authenticationFailure"]},{"name":"eventSeverity","value":["NORMAL"]},{"name":"eventSourceType","value":["f5BigIP"]}]}],[{"name":"aaiMatchingFields","value":["VMID"]},{"name":"aaiSendFields","value":["VMID","Identiy","VNFNAME"]},{"name":"vnf","value":["aSBG"]},{"name":"timeWindow","value":["0"]},{"name":"ageLimit","value":["1600"]},{"name":"createClosedLoopEventId","value":["Close"]},{"name":"outputEventName","value":["Abatement"]},{"stringSet":[{"name":"alarmCondition","value":["authenticationFailure"]},{"name":"eventSeverity","value":["NORMAL"]},{"name":"eventSourceType","value":["f5BigIP"]}]}]]}]
 */
public class ServiceConfiguration {

    private static final Logger logger = Logger.getLogger(ServiceConfiguration.class.getName());

    private final List<String> aaiMatchingFields;
    private final List<String> aaiSendFields;
    private final String groupNumber;
    private final List<String> resourceVf;
    private final List<String> resourceVfc;
    private final String timeWindow;
    private final String ageLimit;
    private final String createClosedLoopEventId;
    private final String outputEventName;
    private final Map<String, String> stringSet;

    /**
     * Parse serviceConfigurations given json node.
     *
     * @param node
     */
    public ServiceConfiguration(JsonNode node) {
        aaiMatchingFields = ModelElement.getValuesByName(node, "aaiMatchingFields");
        aaiSendFields = ModelElement.getValuesByName(node, "aaiSendFields");
        groupNumber = ModelElement.getValueByName(node, "groupNumber");
        resourceVf = ModelElement.getValuesByName(node, "vf");
        resourceVfc = ModelElement.getValuesByName(node, "vfc");
        timeWindow = ModelElement.getValueByName(node, "timeWindow");
        ageLimit = ModelElement.getValueByName(node, "ageLimit");
        createClosedLoopEventId = ModelElement.getValueByName(node, "createClosedLoopEventId");
        outputEventName = ModelElement.getValueByName(node, "outputEventName");

        // process the stringSet fields
        JsonNode ssNodes = node.findPath("stringSet");
        Iterator<JsonNode> itr = ssNodes.elements();
        stringSet = new HashMap<>();
        while (itr.hasNext()) {
            JsonNode ssNode = itr.next();
            String key = ssNode.path("name").asText();
            String value = ssNode.path("value").path(0).asText();
            if (key.length() != 0 && value.length() != 0) {
                // only add string set field if not null
                logger.fine("stringSet: " + key + "=" + value);
                stringSet.put(key, value);
            }
        }
    }

    /**
     * @return the aaiMatchingFields
     */
    public List<String> getaaiMatchingFields() {
        return aaiMatchingFields;
    }

    /**
     * @return the aaiSendFields
     */
    public List<String> getaaiSendFields() {
        return aaiSendFields;
    }

    /**
     * @return the groupNumber
     */
    public String getGroupNumber() {
        return groupNumber;
    }

    /**
     * @return the resourceVf
     */
    public List<String> getResourceVf() {
        return resourceVf;
    }

    /**
     * @return the resourceVfc
     */
    public List<String> getResourceVfc() {
        return resourceVfc;
    }

    /**
     * @return the timeWindow
     */
    public String getTimeWindow() {
        return timeWindow;
    }

    /**
     * @return the ageLimit
     */
    public String getAgeLimit() {
        return ageLimit;
    }

    /**
     * @return the createClosedLoopEventId
     */
    public String getCreateClosedLoopEventId() {
        return createClosedLoopEventId;
    }

    /**
     * @return the outputEventName
     */
    public String getOutputEventName() {
        return outputEventName;
    }

    /**
     * @return the stringSet
     */
    public Map<String, String> getStringSet() {
        return stringSet;
    }

}
