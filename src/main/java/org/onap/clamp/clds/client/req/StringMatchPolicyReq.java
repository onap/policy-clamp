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

package org.onap.clamp.clds.client.req;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onap.clamp.clds.model.prop.Global;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.ServiceConfiguration;
import org.onap.clamp.clds.model.prop.StringMatch;
import org.onap.clamp.clds.model.refprop.RefProp;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;


/**
 * Construct a Policy for String Match Micro Service request given CLDS objects.
 */
public class StringMatchPolicyReq {
    // currently uses the java.util.logging.Logger like the Camunda engine
    private static final Logger logger = Logger.getLogger(StringMatchPolicyReq.class.getName());

    /**
     * Format Policy String Match request.
     *
     * @param refProp
     * @param prop
     * @return
     * @throws IOException
     */
    public static String format(RefProp refProp, ModelProperties prop) throws IOException {
        Global global = prop.getGlobal();
        String service = global.getService();

        StringMatch sm = prop.getStringMatch();
        prop.setCurrentModelElementId(sm.getId());
        ObjectNode rootNode = (ObjectNode) refProp.getJsonTemplate("sm.template", service);

        // "policyName":
        rootNode.put("policyName", prop.getCurrentPolicyScopeAndPolicyName());

        // "content":{
        ObjectNode content = rootNode.with("content");

        // "closedLoopControlName":
        content.put("closedLoopControlName", prop.getControlName());

        // 		"serviceConfigurations":[
        appendServiceConfigurations(refProp, service, content, sm);

        String stringMatchPolicyReq = rootNode.toString();
        logger.info("stringMatchPolicyReq=" + stringMatchPolicyReq);
        return stringMatchPolicyReq;
    }

    /**
     * Add serviceConfigurations to json
     *
     * @param appendToNode
     * @param sm
     * @throws IOException
     */
    public static void appendServiceConfigurations(RefProp refProp, String service, ObjectNode appendToNode, StringMatch sm) throws IOException {
        // 	"serviceConfigurations":{
        ObjectNode scNodes = appendToNode.with("serviceConfigurations");

        Iterator<ServiceConfiguration> scItr = sm.getServiceConfigurations().iterator();
        int index = 0;
        while (scItr.hasNext()) {
            ServiceConfiguration sc = scItr.next();

            //"ItemX":{
            index++;
            String keyValue = "Item" + index;
            ObjectNode scNode = (ObjectNode) refProp.getJsonTemplate("sm.sc.template", service);
            scNodes.set(keyValue, scNode);

            // "rulegroup":"abc",
            String rulegroupInd = refProp.getStringValue("sm.rulegroup", service);
            String groupNumber = sc.getGroupNumber();
            if (rulegroupInd != null && rulegroupInd.equalsIgnoreCase("true") && groupNumber != null && groupNumber.length() > 0) {

                //String rulegroup = (sc.getResourceVf() == null ? "" : String.join(" ", sc.getResourceVf())) + " - " + (sc.getResourceVfc() == null ? "" : String.join(" ", sc.getResourceVfc()));
                scNode.put("rulegroup", groupNumber);
            }

            // "aaiMatchingFields" : ["VM_NAME"],
            JsonUtil.addArrayField(scNode, "aaiMatchingFields", sc.getaaiMatchingFields());
            // "aaiSendFields" : ["VMID", "TenantID"],
            JsonUtil.addArrayField(scNode, "aaiSendFields", sc.getaaiSendFields());

            // "stringSet": [
            ArrayNode ssNode = scNode.putArray("stringSet");
            //	ObjectNode ssNode = scNode.with("stringSet");
            for (Entry<String, String> entry : sc.getStringSet().entrySet()) {
                // exclude eventSourceType
                if (!entry.getKey().equals("eventSourceType")) {
                    ssNode.add(entry.getKey());
                    ssNode.add(entry.getValue());
                }
            }

            // timeWindow": "0",
            scNode.put("timeWindow", sc.getTimeWindow());
            // "ageLimit": "3600",
            scNode.put("ageLimit", sc.getAgeLimit());
            // "createClosedLoopEventId" : "Initial",
            scNode.put("createClosedLoopEventId", sc.getCreateClosedLoopEventId());
            // "outputEventName": "OnSet"
            scNode.put("outputEventName", sc.getOutputEventName());
        }
    }

}
