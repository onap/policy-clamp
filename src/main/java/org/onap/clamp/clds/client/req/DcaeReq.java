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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onap.clamp.clds.model.prop.Global;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.StringMatch;
import org.onap.clamp.clds.model.refprop.RefProp;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;


/**
 * Construct a DCAE request given CLDS objects.
 */
public class DcaeReq {
    // currently uses the java.util.logging.Logger like the Camunda engine
    private static final Logger logger = Logger.getLogger(DcaeReq.class.getName());

    /**
     * Format DCAE request.
     *
     * @param refProp
     * @param prop
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    public static String format(RefProp refProp, ModelProperties prop) throws IOException {
        Global globalProp = prop.getGlobal();
        String service = globalProp.getService();

        StringMatch smProp = prop.getStringMatch();
        prop.setCurrentModelElementId(smProp.getId());

        ObjectNode rootNode = (ObjectNode) refProp.getJsonTemplate("dcae.template");

        //   "properties":{
        ObjectNode properties = rootNode.with("properties");
        //     "service_name":
        properties.put("service_name", globalProp.getService());
        //     "service_ids":[
        List<String> service_ids = refProp.decodeToList("dcae.decode.service_ids", globalProp.getService());
        JsonUtil.addArrayField(properties, "service_ids", service_ids);
        //     "vnf_ids":[
        JsonUtil.addArrayField(properties, "vnf_ids", globalProp.getResourceVf());
        //     "location_ids":[
        JsonUtil.addArrayField(properties, "location_ids", globalProp.getLocation());

        //   "template":{
        ObjectNode template = rootNode.with("template");
        //     "string_matching":{
        ObjectNode string_matching = template.with("string_matching");
        //       "dcae":{
        ObjectNode dcae = string_matching.with("dcae");

        dcae.put("inputTopic", smProp.getTopicSubscribes());
        dcae.put("outputTopic", smProp.getTopicPublishes());
        dcae.put("closedLoopControlName", prop.getControlName());
        dcae.put("policyName", prop.getCurrentPolicyScopeAndPolicyName());

        //         "serviceConfigurations":[
        StringMatchPolicyReq.appendServiceConfigurations(refProp, service, dcae, smProp);

        String dcaeReq = rootNode.toString();
        logger.info("dcaeReq=" + dcaeReq);
        return dcaeReq;
    }

}
