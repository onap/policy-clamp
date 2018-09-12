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

import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.onap.clamp.clds.client.req.policy.GuardPolicyAttributesConstructor;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Policy;
import org.onap.clamp.clds.model.properties.PolicyChain;
import org.onap.clamp.clds.model.properties.PolicyItem;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.policy.api.AttributeType;
import org.onap.policy.controlloop.policy.builder.BuilderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Send Guard Policy info to policy API. It uses the policy code to define
 * the model and communicate with it. See also the PolicyClient class.
 */
@Component
public class GuardPolicyDelegate {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(GuardPolicyDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    private final PolicyClient policyClient;
    private final ClampProperties refProp;

    @Autowired
    public GuardPolicyDelegate(PolicyClient policyClient, ClampProperties refProp) {
        this.policyClient = policyClient;
        this.refProp = refProp;
    }

    /**
     * Perform activity. Send Guard Policies info to policy api.
     *
     * @param camelExchange
     *        The Camel Exchange object containing the properties
     * @throws BuilderException
     *         In case of issues with OperationalPolicyRequestAttributesConstructor
     * @throws UnsupportedEncodingException
     *         In case of issues with the Charset encoding
     */
    @Handler
    public void execute(Exchange camelExchange) throws BuilderException, UnsupportedEncodingException {
        String responseMessageGuard = null;
        ModelProperties prop = ModelProperties.create(camelExchange);
        Policy policy = prop.getType(Policy.class);
        if (policy.isFound()) {
            for (PolicyChain policyChain : prop.getType(Policy.class).getPolicyChains()) {
                for(PolicyItem policyItem:policyChain.getPolicyItems()) {
                    if ("on".equals(policyItem.getEnableGuardPolicy()))
                        responseMessageGuard = createGuardPolicy(prop, policyItem);
                }
            }
            if (responseMessageGuard != null) {
                camelExchange.setProperty("guardPolicyResponseMessage", responseMessageGuard.getBytes());
            }
        }
    }

    private String createGuardPolicy(ModelProperties prop, PolicyItem policyItem) {
        Map<AttributeType, Map<String, String>> attributes = GuardPolicyAttributesConstructor
            .formatAttributes(refProp, prop, prop.getType(Policy.class).getId(), policyItem);
        return policyClient.sendGuardPolicy(attributes, prop, LoggingUtils.getRequestId());
    }
}
