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
 * Parse Policy json properties.
 * <p>
 * Example json:
 * "Policy_005sny1":[[{"name":"timeout","value":"5"}],{"policyConfigurations":[[
 * {"name":"recipe","value":["restart"]},{"name":"maxRetries","value":["3"]},{
 * "name":"retryTimeLimit","value":["180"]},{"name":"_id","value":["vf3RtPi"]},{
 * "name":"location","value":["san_diego"]},{"name":"resource","value":["vCTS"]}
 * ,{"name":"onMaxRetriesLimit","value":[""]},{"name":"onTimeLimit","value":[""]
 * },{"name":"onOtherFailure","value":[""]},{"name":"policy_parent","value":[""]
 * }],[{"name":"recipe","value":["rebuild"]},{"name":"maxRetries","value":["3"]}
 * ,{"name":"retryTimeLimit","value":["180"]},{"name":"_id","value":["89z8Ncl"]}
 * ,{"name":"location","value":["san_diego"]},{"name":"resource","value":["vCTS"
 * ]},{"name":"onMaxRetriesLimit","value":[""]},{"name":"onTimeLimit","value":[
 * ""]},{"name":"onOtherFailure","value":[""]},{"name":"policy_parent","value":[
 * "vf3RtPi"]}]]}]
 */
public class Policy extends AbstractModelElement {
    protected static final EELFLogger logger      = EELFManager.getInstance().getLogger(Policy.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private List<PolicyChain>         policyChains;

    private static final String       TYPE_POLICY = "policy";

    /**
     * Parse Policy given json node.
     *
     * @param modelProp
     * @param modelBpmn
     * @param modelJson
     */
    public Policy(ModelProperties modelProp, ModelBpmn modelBpmn, JsonNode modelJson) {
        super(TYPE_POLICY, modelProp, modelBpmn, modelJson);

        // process policies
        if (modelElementJsonNode != null) {
            Iterator<JsonNode> itr = modelElementJsonNode.elements();
            policyChains = new ArrayList<>();
            while (itr.hasNext()) {
                policyChains.add(new PolicyChain(itr.next()));
            }
        }
    }

    /**
     * @return the policyChains
     */
    public List<PolicyChain> getPolicyChains() {
        return policyChains;
    }

    public static final String getType() {
        return TYPE_POLICY;
    }

}
