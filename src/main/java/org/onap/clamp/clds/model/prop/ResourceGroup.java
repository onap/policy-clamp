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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Parse Resource Group json properties.
 *
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
public class ResourceGroup {

    protected static final EELFLogger  logger      = EELFManager.getInstance().getLogger(ResourceGroup.class);
    protected static final EELFLogger  auditLogger = EELFManager.getInstance().getAuditLogger();

    private String                     groupNumber;
    private String                     policyId;
    private List<ServiceConfiguration> serviceConfigurations;

    /**
     * Parse String Match Resource Group given json node.
     *
     * @param modelBpmn
     * @param modelJson
     */
    public ResourceGroup(JsonNode node) {

        groupNumber = AbstractModelElement.getValueByName(node, "rgname");
        policyId = AbstractModelElement.getValueByName(node, "policyId");

        // process Server_Configurations
        JsonNode serviceConfigurationsNode = node.get(node.size() - 1).get("serviceConfigurations");
        Iterator<JsonNode> itr = serviceConfigurationsNode.elements();
        serviceConfigurations = new ArrayList<>();
        while (itr.hasNext()) {
            serviceConfigurations.add(new ServiceConfiguration(itr.next()));
        }
    }

    /**
     * @return the groupNumber
     */
    public String getGroupNumber() {
        return groupNumber;
    }

    /**
     * @return the policyId
     */
    public String getPolicyId() {
        return policyId;
    }

    /**
     * @return the serviceConfigurations
     */
    public List<ServiceConfiguration> getServiceConfigurations() {
        return serviceConfigurations;
    }

}
