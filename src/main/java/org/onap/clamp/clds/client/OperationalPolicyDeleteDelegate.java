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

package org.onap.clamp.clds.client;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Policy;
import org.onap.clamp.clds.model.properties.PolicyChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Delete Operational Policy via policy api.
 */
@Component
public class OperationalPolicyDeleteDelegate {

    protected static final EELFLogger logger = EELFManager.getInstance()
        .getLogger(OperationalPolicyDeleteDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    @Autowired
    private PolicyClient policyClient;

    /**
     * Perform activity. Delete Operational Policy via policy api.
     *
     * @param camelExchange
     *        The Camel Exchange object containing the properties
     */
    @Handler
    public void execute(Exchange camelExchange) {
        ModelProperties prop = ModelProperties.create(camelExchange);
        Policy policy = prop.getType(Policy.class);
        prop.setCurrentModelElementId(policy.getId());
        String eventAction = (String) camelExchange.getProperty("eventAction");
        if (policy.getPolicyChains() != null && !policy.getPolicyChains().isEmpty()
            && !eventAction.equalsIgnoreCase(CldsEvent.ACTION_CREATE) && policy.isFound()) {
            for (PolicyChain policyChain : policy.getPolicyChains()) {
                prop.setPolicyUniqueId(policyChain.getPolicyId());
                logger.info("Policy Delete response: " + policyClient.deleteBrms(prop));
            }
        }
    }
}
