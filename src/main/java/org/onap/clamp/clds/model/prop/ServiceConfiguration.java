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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parse serviceConfigurations from StringMatch json properties.
 * <p>
 * Example json:
 * {"StringMatch_0aji7go":{"Group1":[{"name":"rgname","value":"1493749598520"},{
 * "name":"rgfriendlyname","value":"Group1"},{"name":"policyName","value":
 * "Policy1"},{"name":"policyId","value":"1"},{"serviceConfigurations":[[{"name"
 * :"aaiMatchingFields","value":["complex.city","vserver.vserver-name"]},{"name"
 * :"aaiSendFields","value":["complex.city","vserver.vserver-name"]},{"name":
 * "eventSeverity","value":["OK"]},{"name":"eventSourceType","value":[""]},{
 * "name":"timeWindow","value":["100"]},{"name":"ageLimit","value":["100"]},{
 * "name":"createClosedLoopEventId","value":["Initial"]},{"name":
 * "outputEventName","value":["ONSET"]}]]}],"Group2":[{"name":"rgname","value":
 * "1493749665149"},{"name":"rgfriendlyname","value":"Group2"},{"name":
 * "policyName","value":"Policy2"},{"name":"policyId","value":"2"},{
 * "serviceConfigurations":[[{"name":"aaiMatchingFields","value":[
 * "cloud-region.identity-url","vserver.vserver-name"]},{"name":"aaiSendFields",
 * "value":["cloud-region.identity-url","vserver.vserver-name"]},{"name":
 * "eventSeverity","value":["NORMAL"]},{"name":"eventSourceType","value":[""]},{
 * "name":"timeWindow","value":["1000"]},{"name":"ageLimit","value":["1000"]},{
 * "name":"createClosedLoopEventId","value":["Initial"]},{"name":
 * "outputEventName","value":["ONSET"]}],[{"name":"aaiMatchingFields","value":[
 * "generic-vnf.vnf-name","vserver.vserver-name"]},{"name":"aaiSendFields",
 * "value":["generic-vnf.vnf-name","vserver.vserver-name"]},{"name":
 * "eventSeverity","value":["CRITICAL"]},{"name":"eventSourceType","value":[""]}
 * ,{"name":"timeWindow","value":["3000"]},{"name":"ageLimit","value":["3000"]},
 * {"name":"createClosedLoopEventId","value":["Initial"]},{"name":
 * "outputEventName","value":["ABATED"]}]]}]}}
 *
 */
public class ServiceConfiguration {

    protected static final EELFLogger         logger      = EELFManager.getInstance().getLogger(ServiceConfiguration.class);
    protected static final EELFLogger   auditLogger = EELFManager.getInstance().getAuditLogger();

    private final List<String>        aaiMatchingFields;
    private final List<String>        aaiSendFields;
    // private final String groupNumber;
    private final List<String>        resourceVf;
    private final List<String>        resourceVfc;
    private final String              timeWindow;
    private final String              ageLimit;
    private final String              createClosedLoopEventId;
    private final String              outputEventName;
    private final Map<String, String> stringSet;

    /**
     * Parse serviceConfigurations given json node.
     *
     * @param node
     */
    public ServiceConfiguration(JsonNode node) {
        aaiMatchingFields = ModelElement.getValuesByName(node, "aaiMatchingFields");
        aaiSendFields = ModelElement.getValuesByName(node, "aaiSendFields");
        // groupNumber = ModelElement.getValueByName(node, "groupNumber");
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
                logger.debug("stringSet: " + key + "=" + value);
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
     */ /*
        * public String getGroupNumber() { return groupNumber; }
        */
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
