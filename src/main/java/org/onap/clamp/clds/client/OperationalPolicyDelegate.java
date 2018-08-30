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
 * Modifications copyright (c) 2018 Nokia
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
import org.onap.clamp.clds.client.req.policy.OperationalPolicyAttributesConstructor;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Policy;
import org.onap.clamp.clds.model.properties.PolicyChain;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.policy.api.AttributeType;
import org.onap.policy.controlloop.policy.builder.BuilderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Send Operational Policy info to policy api. It uses the policy code to define
 * the model and communicate with it. See also the PolicyClient class.
 */
@Component
public class OperationalPolicyDelegate {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(OperationalPolicyDelegate.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();
    private final PolicyClient policyClient;
    private final ClampProperties refProp;
    private final OperationalPolicyAttributesConstructor attributesConstructor;

    @Autowired
    public OperationalPolicyDelegate(PolicyClient policyClient, ClampProperties refProp,
                                     OperationalPolicyAttributesConstructor attributesConstructor) {
        this.policyClient = policyClient;
        this.refProp = refProp;
        this.attributesConstructor = attributesConstructor;
    }

    /**
     * Perform activity. Send Operational Policy info to policy api.
     *
     * @param camelExchange
     *            The Camel Exchange object containing the properties
     * @throws BuilderException
     *             In case of issues with OperationalPolicyRequestAttributesConstructor
     * @throws UnsupportedEncodingException
     *             In case of issues with the Charset encoding
     */
    @Handler
    public void execute(Exchange camelExchange) throws BuilderException, UnsupportedEncodingException {
        String responseMessage = null;
        ModelProperties prop = ModelProperties.create(camelExchange);
        Policy policy = prop.getType(Policy.class);
        if (policy.isFound()) {
            for (PolicyChain policyChain : prop.getType(Policy.class).getPolicyChains()) {
                Map<AttributeType, Map<String, String>> attributes = attributesConstructor.formatAttributes(refProp,
                        prop, prop.getType(Policy.class).getId(), policyChain);
                responseMessage = policyClient.sendBrmsPolicy(attributes, prop, LoggingUtils.getRequestId());
            }
            if (responseMessage != null) {
                camelExchange.setProperty("operationalPolicyResponseMessage", responseMessage.getBytes());
            }
        }
    }
}
